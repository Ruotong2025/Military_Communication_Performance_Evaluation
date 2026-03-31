package com.ccnu.military.service;

import com.ccnu.military.dto.CollectiveCalculateRequest;
import com.ccnu.military.dto.CollectiveCalculateResponse;
import com.ccnu.military.dto.CollectiveWeightPreview;
import com.ccnu.military.entity.ExpertAhpComparisonScore;
import com.ccnu.military.entity.ExpertAhpGroupWeights;
import com.ccnu.military.entity.ExpertCredibilityScore;
import com.ccnu.military.entity.ExpertWeightedEvaluationResult;
import com.ccnu.military.repository.ExpertAhpComparisonScoreRepository;
import com.ccnu.military.repository.ExpertAhpGroupWeightsRepository;
import com.ccnu.military.repository.ExpertCredibilityScoreRepository;
import com.ccnu.military.repository.ExpertWeightedEvaluationResultRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 专家集结服务（对比打分层集结）
 *
 * 核心思想：不是从 expert_ahp_individual_weights 取权重结果，
 * 而是从 expert_ahp_comparison_score 取所有专家的原始比较打分，
 * 用"可信度 + 把握度"加权平均构造集体判断矩阵，再做 AHP 层次单排序。
 *
 * 计算步骤（修正版）：
 * ① 选定参与专家（可指定或全部）
 * ② 加载每位专家的可信度得分（Cred_i ∈ [0,100]），作为该专家的基础权重
 * ③ 对每个 comparison_key，分别计算每位专家在该 key 上的综合权重：
 *    W_{i,k} = (Cred_i/100) × 0.5 + confidence_{i,k} × 0.5
 *    w_{i,k} = W_{i,k} / Σ W_{j,k}    （在 key 内归一化）
 * ④ 对每个 comparison_key，用 w_{i,k} 对 score_{i,k} 加权算术平均：
 *    collective_score_k = Σ_i ( w_{i,k} × score_{i,k} )
 * ⑤ 构造 6 个集体判断矩阵（维度层 + 5个指标层）
 * ⑥ 对 6 个矩阵分别做 AHP，计算权重向量和 CR
 * ⑦ 计算 18 个二级指标综合权重（维度权重 × 指标层权重，归一化）
 * ⑧ 存储集结权重到 expert_ahp_group_weights 表（按专家集合 upsert）
 *
 * 综合得分（权重×归一化 score）：在「评估结果计算」页点击「加载综合结果」时执行并写入 expert_weighted_evaluation_result。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpertAggregationCollectiveService {

    private final ExpertAhpComparisonScoreRepository scoreRepository;
    private final ExpertCredibilityScoreRepository credibilityRepository;
    private final ExpertAhpGroupWeightsRepository groupWeightsRepository;
    private final ExpertWeightedEvaluationResultRepository weightedEvaluationResultRepository;
    private final MetricsCalculationService metricsCalculationService;
    private final ExpertAHPService ahpService;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    // RI值表
    private static final double[] RI = {0, 0, 0, 0.58, 0.90, 1.12, 1.24, 1.32, 1.41, 1.45, 1.49, 1.51, 1.54, 1.56, 1.57, 1.58};

    // 维度名称（5个）
    private static final String[] DIMENSIONS = {"安全性", "可靠性", "传输能力", "抗干扰能力", "效能影响"};

    // 各维度的指标名称（18个）
    private static final Map<String, String[]> DIMENSION_INDICATORS = new LinkedHashMap<>();

    // 指标中文名 → score表列名
    private static final Map<String, String> INDICATOR_TO_SCORE_COL = new LinkedHashMap<>();

    // 维度中文名 → 英文字段名（用于score表列名解析）
    private static final Map<String, String> DIM_TO_SCORE_PREFIX = new LinkedHashMap<>();

    static {
        DIMENSION_INDICATORS.put("安全性", new String[]{"密钥泄露得分", "被侦察得分", "抗拦截得分"});
        DIMENSION_INDICATORS.put("可靠性", new String[]{"崩溃比例得分", "恢复能力得分", "通信可用得分"});
        DIMENSION_INDICATORS.put("传输能力", new String[]{"带宽得分", "呼叫建立得分", "传输时延得分", "误码率得分", "吞吐量得分", "频谱效率得分"});
        DIMENSION_INDICATORS.put("抗干扰能力", new String[]{"信干噪比得分", "抗干扰余量得分", "通信距离得分"});
        DIMENSION_INDICATORS.put("效能影响", new String[]{"战损率得分", "任务完成率得分", "致盲率得分"});

        // 从 MetricsDirection 获取指标中文名 → score表列名的映射
        Map<String, String> fieldToScore = MetricsDirection.getFieldToScoreColumnMap();
        for (Map.Entry<String, String> e : fieldToScore.entrySet()) {
            String indicator = translateFieldToIndicator(e.getKey());
            INDICATOR_TO_SCORE_COL.put(indicator, e.getValue());
        }

        DIM_TO_SCORE_PREFIX.put("安全性", "security");
        DIM_TO_SCORE_PREFIX.put("可靠性", "reliability");
        DIM_TO_SCORE_PREFIX.put("传输能力", "transmission");
        DIM_TO_SCORE_PREFIX.put("抗干扰能力", "anti_jamming");
        DIM_TO_SCORE_PREFIX.put("效能影响", "effect");
    }

    // ==================== 公开接口 ====================

    /**
     * 获取所有评估批次ID（从前端计算指标得分时使用）
     */
    public List<String> getEvaluationIds() {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT evaluation_id FROM score_military_comm_effect ORDER BY evaluation_id DESC",
                String.class
        );
    }

    /**
     * 权重预览（仅计算并返回集结权重，不保存）
     */
    public CollectiveWeightPreview previewWeights(CollectiveCalculateRequest request) {
        List<Long> expertIds = resolveExpertIds(request);

        // Step ②：加载专家可信度（全局基础权重）
        Map<Long, Double> credMap = loadExpertCredibilities(expertIds);

        // Step ③：计算每个 comparison_key 下各专家的归一化权重
        Map<String, List<ExpertAhpComparisonScore>> grouped = groupScoresByKey(expertIds);
        Map<String, Map<Long, Double>> perKeyWeights = calculatePerKeyExpertWeights(grouped, credMap);

        // Step ④：构造集体比较打分
        Map<String, Double> collectiveScores = buildCollectiveScores(grouped, perKeyWeights);

        Map<String, MatrixContext> matrixResults = buildAndCalculateMatrices(collectiveScores);
        Map<String, Double> dimWeights = matrixResults.get("__dim__").weights;
        Map<String, Double> combinedWeights = calculateCombinedWeights(dimWeights, matrixResults);
        Map<String, Map<String, Double>> indWeightsByDim = buildIndicatorWeightsByDimension(matrixResults);

        CollectiveWeightPreview preview = new CollectiveWeightPreview();
        preview.setEvaluationId(request.getEvaluationId());
        preview.setExpertCount(expertIds.size());
        preview.setExpertIds(expertIds);

        // CR结果
        Map<String, BigDecimal> crResults = new LinkedHashMap<>();
        MatrixContext dimCtx = matrixResults.get("__dim__");
        crResults.put("dim", toBd(dimCtx != null ? dimCtx.cr : 0.0));
        for (String dim : DIMENSIONS) {
            MatrixContext ctx = matrixResults.get(dim);
            crResults.put(dim, toBd(ctx != null ? ctx.cr : 0.0));
        }
        preview.setCrResults(crResults);

        // 维度权重
        preview.setDimensionWeights(toBdMap(dimWeights));

        // 指标综合权重（归一化到对总目标）
        preview.setIndicatorWeights(toBdMap(combinedWeights));

        // 指标权重按维度分组
        preview.setIndicatorWeightsByDimension(toBdMap2(indWeightsByDim));

        // 专家权重明细（基于可信度，展示每位专家的整体可信度水平）
        preview.setExpertWeights(buildExpertWeightDetails(expertIds, credMap, perKeyWeights));

        // 集体比较打分（用于展示矩阵）
        preview.setCollectiveScores(collectiveScores);

        // 每个 key 下各专家的归一化权重（供前端展示计算过程）
        preview.setPerKeyExpertWeights(toBdMapPerKey(perKeyWeights));

        return preview;
    }

    /**
     * 执行集结计算并保存（upsert：专家集合相同则更新，不同则新增）
     * 注意：使用 noRollbackFor 避免内部方法异常导致主事务回滚
     */
    @Transactional(noRollbackFor = Exception.class)
    public CollectiveCalculateResponse calculateAndSave(CollectiveCalculateRequest request) {
        List<Long> expertIds = resolveExpertIds(request);
        String expertIdsStr = buildExpertIdsString(expertIds);

        // ① 加载专家可信度（全局基础权重）
        Map<Long, Double> credMap = loadExpertCredibilities(expertIds);

        // ② 计算每个 comparison_key 下各专家的归一化权重
        Map<String, List<ExpertAhpComparisonScore>> grouped = groupScoresByKey(expertIds);
        Map<String, Map<Long, Double>> perKeyWeights = calculatePerKeyExpertWeights(grouped, credMap);

        // ③ 构造集体比较打分
        Map<String, Double> collectiveScores = buildCollectiveScores(grouped, perKeyWeights);

        // ④ 构造并计算 6 个矩阵
        Map<String, MatrixContext> matrixResults = buildAndCalculateMatrices(collectiveScores);
        Map<String, Double> dimWeights = matrixResults.get("__dim__").weights;

        // ⑤ 计算 18 个指标综合权重（归一化到对总目标）
        Map<String, Double> combinedWeights = calculateCombinedWeights(dimWeights, matrixResults);
        Map<String, Map<String, Double>> indWeightsByDim = buildIndicatorWeightsByDimension(matrixResults);

        // ⑥ Upsert 集结权重到 expert_ahp_group_weights 表
        String groupId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ExpertAhpGroupWeights entity = buildGroupWeightsEntity(
                groupId, expertIdsStr, expertIds, matrixResults,
                dimWeights, combinedWeights, indWeightsByDim,
                credMap, perKeyWeights);

        // 查询是否存在相同专家集合的记录
        Optional<ExpertAhpGroupWeights> existingOpt = groupWeightsRepository.findByExpertIds(expertIdsStr);
        if (existingOpt.isPresent()) {
            // 更新：保留原 groupId，更新其他字段
            ExpertAhpGroupWeights existing = existingOpt.get();
            entity.setId(existing.getId());
            entity.setGroupId(existing.getGroupId());
            groupWeightsRepository.save(entity);
            log.info("更新专家集结权重，groupId={}, expertIds={}", existing.getGroupId(), expertIdsStr);
        } else {
            // 新增
            groupWeightsRepository.save(entity);
            log.info("新增专家集结权重，groupId={}, expertIds={}", groupId, expertIdsStr);
        }

        // ⑦ 构建响应
        CollectiveCalculateResponse response = new CollectiveCalculateResponse();
        response.setEvaluationId(request.getEvaluationId());
        response.setExpertCount(expertIds.size());
        response.setExpertIds(expertIds);

        // CR结果
        Map<String, BigDecimal> crResults = new LinkedHashMap<>();
        MatrixContext dimCtxR = matrixResults.get("__dim__");
        crResults.put("dim", toBd(dimCtxR != null ? dimCtxR.cr : 0.0));
        for (String dim : DIMENSIONS) {
            MatrixContext ctx = matrixResults.get(dim);
            crResults.put(dim, toBd(ctx != null ? ctx.cr : 0.0));
        }
        response.setCrResults(crResults);

        response.setDimensionWeights(toBdMap(dimWeights));
        response.setIndicatorWeights(toBdMap(combinedWeights));
        response.setIndicatorWeightsByDimension(toBdMap2(indWeightsByDim));
        response.setExpertWeights(buildExpertWeightDetails(expertIds, credMap, perKeyWeights));
        response.setCollectiveScores(collectiveScores);
        response.setPerKeyExpertWeights(toBdMapPerKey(perKeyWeights));

        log.info("专家集结计算完成（仅权重），expertCount={}, expertIds={}", expertIds.size(), expertIdsStr);
        return response;
    }

    /**
     * 「加载综合结果」：取最近一次集结权重 × 该批次 score 表归一化得分，写入 expert_weighted_evaluation_result 并返回（展示保留两位小数由 toBdScore2 控制）。
     */
    @Transactional
    public CollectiveCalculateResponse computeAndPersistWeightedResults(String evaluationId) {
        if (!StringUtils.hasText(evaluationId)) {
            throw new IllegalArgumentException("评估批次不能为空");
        }
        String eid = evaluationId.trim();
        ExpertAhpGroupWeights gw = groupWeightsRepository.findFirstByOrderByUpdatedAtDesc()
                .orElseThrow(() -> new IllegalStateException(
                        "请先在「权重集结打分」中执行「执行集结计算」生成集结权重。"));
        Map<String, Double> combined = combinedWeightsDoubleFromEntity(gw);
        if (combined.isEmpty()) {
            throw new IllegalStateException("集结权重记录中二级指标权重为空，请重新执行集结计算。");
        }
        CollectiveCalculateResponse response = buildResponseShellFromGroupWeights(gw, eid);
        List<CollectiveCalculateResponse.OperationResult> results = buildWeightedEvaluationResults(eid, combined);
        if (results.isEmpty()) {
            throw new IllegalStateException(
                    "该评估批次在 score_military_comm_effect 中无归一化得分数据，请先生成归一化 score。");
        }
        response.setResults(results);
        try {
            persistWeightedEvaluationResults(response);
        } catch (Exception ex) {
            throw new IllegalStateException("保存综合评估结果失败: " + ex.getMessage(), ex);
        }
        log.info("综合结果已计算并保存，evaluationId={}, operations={}", eid, results.size());
        return response;
    }

    /**
     * 按评估批次加载已保存的加权评估结果（供「综合打分」页展示）
     */
    public CollectiveCalculateResponse loadWeightedResults(String evaluationId) {
        if (!StringUtils.hasText(evaluationId)) {
            throw new IllegalArgumentException("评估批次不能为空");
        }
        List<ExpertWeightedEvaluationResult> rows = weightedEvaluationResultRepository
                .findByEvaluationIdOrderByOperationIdAsc(evaluationId.trim());
        if (rows.isEmpty()) {
            throw new IllegalStateException(
                    "暂无该批次的已保存综合结果。请先在「评估结果计算」页选择批次并点击「加载综合结果」（权重×归一化得分）。");
        }
        CollectiveCalculateResponse out = ExpertWeightedEvaluationResultMapper.toResponse(rows);
        try {
            String json = rows.get(0).getExpertWeightsJson();
            if (StringUtils.hasText(json) && out != null) {
                List<CollectiveCalculateResponse.ExpertWeightDetail> list = objectMapper.readValue(
                        json,
                        objectMapper.getTypeFactory().constructCollectionType(
                                List.class, CollectiveCalculateResponse.ExpertWeightDetail.class));
                out.setExpertWeights(list);
            }
        } catch (Exception e) {
            log.debug("解析 expert_weights_json 失败: {}", e.getMessage());
        }
        return out;
    }

    /**
     * 删除指定批次的已保存加权评估结果
     */
    @Transactional
    public void deleteWeightedResults(String evaluationId) {
        if (StringUtils.hasText(evaluationId)) {
            weightedEvaluationResultRepository.deleteByEvaluationId(evaluationId.trim());
        }
    }

    private Map<String, Double> combinedWeightsDoubleFromEntity(ExpertAhpGroupWeights gw) {
        Map<String, Double> m = new LinkedHashMap<>();
        m.put("密钥泄露得分", bdToDouble(gw.getIndWeightKeyLeakage()));
        m.put("被侦察得分", bdToDouble(gw.getIndWeightDetectedProbability()));
        m.put("抗拦截得分", bdToDouble(gw.getIndWeightInterceptionResistance()));
        m.put("崩溃比例得分", bdToDouble(gw.getIndWeightCrashRate()));
        m.put("恢复能力得分", bdToDouble(gw.getIndWeightRecoveryCapability()));
        m.put("通信可用得分", bdToDouble(gw.getIndWeightCommunicationAvailability()));
        m.put("带宽得分", bdToDouble(gw.getIndWeightBandwidth()));
        m.put("呼叫建立得分", bdToDouble(gw.getIndWeightCallSetupTime()));
        m.put("传输时延得分", bdToDouble(gw.getIndWeightTransmissionDelay()));
        m.put("误码率得分", bdToDouble(gw.getIndWeightBitErrorRate()));
        m.put("吞吐量得分", bdToDouble(gw.getIndWeightThroughput()));
        m.put("频谱效率得分", bdToDouble(gw.getIndWeightSpectralEfficiency()));
        m.put("信干噪比得分", bdToDouble(gw.getIndWeightSinr()));
        m.put("抗干扰余量得分", bdToDouble(gw.getIndWeightAntiJammingMargin()));
        m.put("通信距离得分", bdToDouble(gw.getIndWeightCommunicationDistance()));
        m.put("战损率得分", bdToDouble(gw.getIndWeightDamageRate()));
        m.put("任务完成率得分", bdToDouble(gw.getIndWeightMissionCompletionRate()));
        m.put("致盲率得分", bdToDouble(gw.getIndWeightBlindRate()));
        return m;
    }

    private static double bdToDouble(BigDecimal b) {
        return b != null ? b.doubleValue() : 0.0;
    }

    private CollectiveCalculateResponse buildResponseShellFromGroupWeights(ExpertAhpGroupWeights gw, String evaluationId) {
        CollectiveCalculateResponse r = new CollectiveCalculateResponse();
        r.setEvaluationId(evaluationId);
        r.setExpertCount(gw.getExpertCount());
        r.setExpertIds(parseExpertIds(gw.getExpertIds()));

        Map<String, BigDecimal> cr = new LinkedHashMap<>();
        cr.put("dim", gw.getCrDim());
        cr.put("安全性", gw.getCrSecurity());
        cr.put("可靠性", gw.getCrReliability());
        cr.put("传输能力", gw.getCrTransmission());
        cr.put("抗干扰能力", gw.getCrAntiJamming());
        cr.put("效能影响", gw.getCrEffect());
        r.setCrResults(cr);

        Map<String, BigDecimal> dw = new LinkedHashMap<>();
        dw.put("安全性", gw.getDimWeightSecurity());
        dw.put("可靠性", gw.getDimWeightReliability());
        dw.put("传输能力", gw.getDimWeightTransmission());
        dw.put("抗干扰能力", gw.getDimWeightAntiJamming());
        dw.put("效能影响", gw.getDimWeightEffect());
        r.setDimensionWeights(dw);

        Map<String, BigDecimal> iw = new LinkedHashMap<>();
        iw.put("密钥泄露得分", gw.getIndWeightKeyLeakage());
        iw.put("被侦察得分", gw.getIndWeightDetectedProbability());
        iw.put("抗拦截得分", gw.getIndWeightInterceptionResistance());
        iw.put("崩溃比例得分", gw.getIndWeightCrashRate());
        iw.put("恢复能力得分", gw.getIndWeightRecoveryCapability());
        iw.put("通信可用得分", gw.getIndWeightCommunicationAvailability());
        iw.put("带宽得分", gw.getIndWeightBandwidth());
        iw.put("呼叫建立得分", gw.getIndWeightCallSetupTime());
        iw.put("传输时延得分", gw.getIndWeightTransmissionDelay());
        iw.put("误码率得分", gw.getIndWeightBitErrorRate());
        iw.put("吞吐量得分", gw.getIndWeightThroughput());
        iw.put("频谱效率得分", gw.getIndWeightSpectralEfficiency());
        iw.put("信干噪比得分", gw.getIndWeightSinr());
        iw.put("抗干扰余量得分", gw.getIndWeightAntiJammingMargin());
        iw.put("通信距离得分", gw.getIndWeightCommunicationDistance());
        iw.put("战损率得分", gw.getIndWeightDamageRate());
        iw.put("任务完成率得分", gw.getIndWeightMissionCompletionRate());
        iw.put("致盲率得分", gw.getIndWeightBlindRate());
        r.setIndicatorWeights(iw);

        r.setIndicatorWeightsByDimension(new LinkedHashMap<>());
        r.setCollectiveScores(new LinkedHashMap<>());
        r.setPerKeyExpertWeights(new LinkedHashMap<>());
        r.setExpertWeights(parseExpertWeightDetails(gw.getExpertWeightsJson()));
        return r;
    }

    private List<Long> parseExpertIds(String expertIdsStr) {
        if (!StringUtils.hasText(expertIdsStr)) {
            return Collections.emptyList();
        }
        List<Long> out = new ArrayList<>();
        for (String p : expertIdsStr.split(",")) {
            String s = p.trim();
            if (s.isEmpty()) {
                continue;
            }
            try {
                out.add(Long.valueOf(s));
            } catch (NumberFormatException ignored) {
                // skip
            }
        }
        return out;
    }

    private List<CollectiveCalculateResponse.ExpertWeightDetail> parseExpertWeightDetails(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() { });
            List<CollectiveCalculateResponse.ExpertWeightDetail> out = new ArrayList<>();
            for (Map<String, Object> m : raw) {
                CollectiveCalculateResponse.ExpertWeightDetail d = new CollectiveCalculateResponse.ExpertWeightDetail();
                Object eid = m.get("expertId");
                if (eid != null) {
                    d.setExpertId(Long.valueOf(eid.toString()));
                }
                Object name = m.get("expertName");
                d.setExpertName(name != null ? name.toString() : "未知");
                Object cred = m.get("credibility");
                if (cred instanceof Number) {
                    d.setCredibility(BigDecimal.valueOf(((Number) cred).doubleValue()).setScale(2, RoundingMode.HALF_UP));
                } else if (cred != null) {
                    d.setCredibility(new BigDecimal(cred.toString()).setScale(2, RoundingMode.HALF_UP));
                } else {
                    d.setCredibility(BigDecimal.ZERO);
                }
                Object nw = m.get("avgNormalizedWeight");
                if (nw instanceof Number) {
                    d.setNormalizedWeight(BigDecimal.valueOf(((Number) nw).doubleValue()).setScale(2, RoundingMode.HALF_UP));
                } else {
                    d.setNormalizedWeight(BigDecimal.ZERO);
                }
                out.add(d);
            }
            return out;
        } catch (Exception e) {
            log.warn("解析 expert_weights_json 失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private void persistWeightedEvaluationResults(CollectiveCalculateResponse response) throws Exception {
        if (response == null || !StringUtils.hasText(response.getEvaluationId())) {
            return;
        }
        if (response.getResults() == null || response.getResults().isEmpty()) {
            return;
        }
        String eid = response.getEvaluationId().trim();
        weightedEvaluationResultRepository.deleteByEvaluationId(eid);
        String expertWeightsJson = objectMapper.writeValueAsString(
                response.getExpertWeights() != null ? response.getExpertWeights() : Collections.emptyList());
        List<ExpertWeightedEvaluationResult> batch = new ArrayList<>();
        for (CollectiveCalculateResponse.OperationResult op : response.getResults()) {
            batch.add(ExpertWeightedEvaluationResultMapper.toRow(response, op, expertWeightsJson));
        }
        weightedEvaluationResultRepository.saveAll(batch);
    }

    /**
     * 按当前集结的综合权重，对选定评估批次下各作战的归一化 score 加权求综合分与维度分。
     */
    private List<CollectiveCalculateResponse.OperationResult> buildWeightedEvaluationResults(
            String evaluationId, Map<String, Double> combinedWeights) {
        if (!org.springframework.util.StringUtils.hasText(evaluationId) || combinedWeights == null || combinedWeights.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> rows = metricsCalculationService.getScoreData(evaluationId.trim());
        if (rows == null || rows.isEmpty()) {
            log.warn("评估批次 {} 在 score_military_comm_effect 中无数据，请先对该批次执行「生成归一化 score」", evaluationId);
            return Collections.emptyList();
        }

        List<CollectiveCalculateResponse.OperationResult> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String opId = String.valueOf(row.get("operation_id"));

            Map<String, BigDecimal> indicatorScoresBd = new LinkedHashMap<>();
            double total = 0.0;
            Map<String, Double> dimTotals = new LinkedHashMap<>();
            for (String dim : DIMENSIONS) {
                dimTotals.put(dim, 0.0);
            }

            for (Map.Entry<String, Double> we : combinedWeights.entrySet()) {
                String indName = we.getKey();
                double w = we.getValue();
                String scoreCol = INDICATOR_TO_SCORE_COL.get(indName);
                if (scoreCol == null) {
                    continue;
                }
                Object raw = row.get(scoreCol);
                double s = raw != null ? ((Number) raw).doubleValue() : 0.0;
                indicatorScoresBd.put(indName, toBdScore2(s));
                total += w * s;
                String dim = dimensionOfIndicator(indName);
                if (dim != null) {
                    dimTotals.merge(dim, w * s, Double::sum);
                }
            }

            CollectiveCalculateResponse.OperationResult r = new CollectiveCalculateResponse.OperationResult();
            r.setOperationId(opId);
            r.setTotalScore(toBdScore2(total));
            Map<String, BigDecimal> ds = new LinkedHashMap<>();
            for (String dim : DIMENSIONS) {
                ds.put(dim, toBdScore2(dimTotals.getOrDefault(dim, 0.0)));
            }
            r.setDimensionScores(ds);
            r.setIndicatorScores(indicatorScoresBd);
            out.add(r);
        }

        out.sort(Comparator.comparing(o -> {
            try {
                return Integer.parseInt(o.getOperationId());
            } catch (Exception e) {
                return 0;
            }
        }));
        return out;
    }

    private static String dimensionOfIndicator(String indicatorName) {
        for (Map.Entry<String, String[]> e : DIMENSION_INDICATORS.entrySet()) {
            for (String ind : e.getValue()) {
                if (ind.equals(indicatorName)) {
                    return e.getKey();
                }
            }
        }
        return null;
    }

    /**
     * 查询所有已保存的专家组集结权重
     */
    public List<ExpertAhpGroupWeights> getAllGroupWeights() {
        return groupWeightsRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * 根据专家组ID查询集结权重
     */
    public Optional<ExpertAhpGroupWeights> getGroupWeightsByGroupId(String groupId) {
        return groupWeightsRepository.findByGroupId(groupId);
    }

    /**
     * 根据专家集合查询集结权重
     */
    public Optional<ExpertAhpGroupWeights> getGroupWeightsByExpertIds(List<Long> expertIds) {
        String expertIdsStr = buildExpertIdsString(expertIds);
        return groupWeightsRepository.findByExpertIds(expertIdsStr);
    }

    /**
     * 删除指定的专家组
     */
    @Transactional
    public void deleteGroupWeights(String groupId) {
        groupWeightsRepository.deleteByGroupId(groupId);
    }

    // ==================== 核心计算逻辑 ====================

    /**
     * Step ①：解析专家列表
     */
    private List<Long> resolveExpertIds(CollectiveCalculateRequest request) {
        if (Boolean.TRUE.equals(request.getUseAllExperts())) {
            return scoreRepository.findAll().stream()
                    .map(ExpertAhpComparisonScore::getExpertId)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        } else if (request.getExpertIds() != null && !request.getExpertIds().isEmpty()) {
            return request.getExpertIds().stream().sorted().collect(Collectors.toList());
        } else {
            return scoreRepository.findAll().stream()
                    .map(ExpertAhpComparisonScore::getExpertId)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    /**
     * 构建专家ID字符串（排序后逗号分隔）
     */
    private String buildExpertIdsString(List<Long> expertIds) {
        return expertIds.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    /**
     * Step ②-③（合并）：计算每位专家的可信度得分（全局基础权重，仅用于展示，不参与集结计算）
     * 返回：expertId → Cred_i ∈ [0,1]（可信度归一化到 0~1）
     */
    private Map<Long, Double> loadExpertCredibilities(List<Long> expertIds) {
        Map<Long, Double> credMap = new LinkedHashMap<>();
        for (Long expertId : expertIds) {
            int credInt = credibilityRepository.findByExpertId(expertId)
                    .map(ExpertCredibilityScore::getTotalScore)
                    .map(BigDecimal::doubleValue)
                    .orElse(50.0)
                    .intValue();
            // 归一化到 [0, 1]
            credMap.put(expertId, credInt / 100.0);
        }
        return credMap;
    }

    /**
     * Step ③（核心）：对每个 comparison_key，计算该 key 下各位专家的归一化权重
     * W_{i,k} = Cred_i × 0.5 + confidence_{i,k} × 0.5
     * w_{i,k} = W_{i,k} / Σ W_{j,k}
     * 返回：Map<comparisonKey, Map<expertId, w_ik>>
     */
    private Map<String, Map<Long, Double>> calculatePerKeyExpertWeights(
            Map<String, List<ExpertAhpComparisonScore>> grouped,
            Map<Long, Double> credMap) {

        // 对每个 comparison_key 计算 rawWeight 并归一化
        Map<String, Map<Long, Double>> perKeyWeights = new LinkedHashMap<>();
        for (Map.Entry<String, List<ExpertAhpComparisonScore>> e : grouped.entrySet()) {
            String key = e.getKey();
            List<ExpertAhpComparisonScore> list = e.getValue();

            // 计算每个专家在该 key 上的 rawWeight
            Map<Long, Double> rawMap = new LinkedHashMap<>();
            for (ExpertAhpComparisonScore s : list) {
                Long expId = s.getExpertId();
                Double cred = credMap.getOrDefault(expId, 0.5);
                Double conf = s.getConfidence() != null ? s.getConfidence().doubleValue() : 0.5;
                double raw = cred * 0.5 + conf * 0.5;
                rawMap.put(expId, raw);
            }

            // 归一化到该 key 内
            double total = rawMap.values().stream().mapToDouble(Double::doubleValue).sum();
            if (total <= 0) total = 1.0;
            Map<Long, Double> normMap = new LinkedHashMap<>();
            for (Map.Entry<Long, Double> re : rawMap.entrySet()) {
                normMap.put(re.getKey(), re.getValue() / total);
            }
            perKeyWeights.put(key, normMap);
        }

        return perKeyWeights;
    }

    /**
     * Step ④：构造集体比较打分
     * 对每个 comparison_key：collective_score_k = Σ_i ( w_{i,k} × score_{i,k} )
     */
    private Map<String, Double> buildCollectiveScores(
            Map<String, List<ExpertAhpComparisonScore>> grouped,
            Map<String, Map<Long, Double>> perKeyWeights) {

        Map<String, Double> collectiveScores = new LinkedHashMap<>();
        for (Map.Entry<String, List<ExpertAhpComparisonScore>> e : grouped.entrySet()) {
            String key = e.getKey();
            List<ExpertAhpComparisonScore> list = e.getValue();
            Map<Long, Double> weights = perKeyWeights.get(key);
            if (weights == null) continue;

            double sum = 0.0;
            for (ExpertAhpComparisonScore s : list) {
                Double w = weights.get(s.getExpertId());
                if (w == null) continue;
                Double score = s.getScore() != null ? s.getScore().doubleValue() : 1.0;
                sum += w * score;
            }
            // 限制在 [1/9, 9] 范围内
            sum = Math.max(1.0 / 9.0, Math.min(9.0, sum));
            collectiveScores.put(key, sum);
        }
        return collectiveScores;
    }

    /**
     * 构造按 comparison_key 分组的比较打分列表（供 buildCollectiveScores 使用）
     */
    private Map<String, List<ExpertAhpComparisonScore>> groupScoresByKey(List<Long> expertIds) {
        Map<String, List<ExpertAhpComparisonScore>> grouped = new LinkedHashMap<>();
        for (Long expertId : expertIds) {
            List<ExpertAhpComparisonScore> scores = scoreRepository.findByExpertIdOrderByIdAsc(expertId);
            for (ExpertAhpComparisonScore s : scores) {
                String key = s.getComparisonKey();
                if (key == null || key.isBlank()) continue;
                grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
            }
        }
        return grouped;
    }

    /**
     * Step ④-⑤：构造 6 个集体矩阵并做 AHP 计算
     * 返回 map：key="__dim__"表示维度层，key=dimName表示该维度的指标层
     */
    private Map<String, MatrixContext> buildAndCalculateMatrices(Map<String, Double> collectiveScores) {
        Map<String, MatrixContext> results = new LinkedHashMap<>();

        // 维度层矩阵
        MatrixContext dimCtx = buildAndCalculateMatrix(DIMENSIONS, collectiveScores);
        results.put("__dim__", dimCtx);

        // 各维度指标层矩阵
        for (Map.Entry<String, String[]> e : DIMENSION_INDICATORS.entrySet()) {
            String dimName = e.getKey();
            String[] indicators = e.getValue();
            MatrixContext ctx = buildAndCalculateMatrix(indicators, collectiveScores);
            results.put(dimName, ctx);
        }

        return results;
    }

    /**
     * 构造并计算单个矩阵
     */
    private MatrixContext buildAndCalculateMatrix(String[] elements, Map<String, Double> collectiveScores) {
        int n = elements.length;
        double[][] matrix = new double[n][n];

        // 初始化单位矩阵
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = (i == j) ? 1.0 : 0.0;
            }
        }

        // 构建元素名→索引的映射
        Map<String, Integer> nameToIdx = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            nameToIdx.put(elements[i], i);
        }

        // 填充矩阵（上三角）
        for (Map.Entry<String, Double> e : collectiveScores.entrySet()) {
            String key = e.getKey();
            Double score = e.getValue();
            if (score == null || !Double.isFinite(score)) continue;

            // 解析 key，格式 "A_B"
            if (!key.contains("_")) continue;
            String[] parts = key.split("_", 2);
            String nameA = parts[0];
            String nameB = parts[1];

            Integer idxA = nameToIdx.get(nameA);
            Integer idxB = nameToIdx.get(nameB);
            if (idxA == null || idxB == null) continue;

            matrix[idxA][idxB] = score;
            matrix[idxB][idxA] = 1.0 / score;
        }

        // AHP 计算权重
        double[] weights = ahpService.calculateWeights(matrix);
        double lambdaMax = ahpService.calculateLambdaMax(matrix, weights);
        double ci = (lambdaMax - n) / (n - 1);
        double ri = n < RI.length ? RI[n] : 1.59;
        double cr = ri > 0 ? ci / ri : 0;

        // 构建权重映射
        Map<String, Double> weightMap = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            weightMap.put(elements[i], weights[i]);
        }

        MatrixContext ctx = new MatrixContext();
        ctx.weights = weightMap;
        ctx.cr = cr;
        ctx.lambdaMax = lambdaMax;
        ctx.matrix = matrix;
        ctx.elementNames = Arrays.asList(elements);
        return ctx;
    }

    /**
     * Step ⑥：计算 18 个二级指标综合权重（归一化到对总目标，和为1）
     */
    private Map<String, Double> calculateCombinedWeights(Map<String, Double> dimWeights,
                                                          Map<String, MatrixContext> matrixResults) {
        Map<String, Double> combined = new LinkedHashMap<>();

        for (Map.Entry<String, String[]> e : DIMENSION_INDICATORS.entrySet()) {
            String dimName = e.getKey();
            String[] indicators = e.getValue();
            MatrixContext indCtx = matrixResults.get(dimName);
            if (indCtx == null) continue;
            Map<String, Double> indWeights = indCtx.weights;
            double dimWeight = dimWeights.getOrDefault(dimName, 0.0);

            for (String indicator : indicators) {
                Double indWeight = indWeights.getOrDefault(indicator, 0.0);
                combined.put(indicator, dimWeight * indWeight);
            }
        }

        // 归一化，使 18 个指标综合权重和为1
        double total = combined.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total > 0) {
            for (String key : combined.keySet()) {
                combined.put(key, combined.get(key) / total);
            }
        }
        return combined;
    }

    /**
     * 构建各维度内的指标权重（局部权重，用于计算维度得分）
     * key=dimName, value=Map<indicator, weight>
     */
    private Map<String, Map<String, Double>> buildIndicatorWeightsByDimension(
            Map<String, MatrixContext> matrixResults) {
        Map<String, Map<String, Double>> result = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> e : DIMENSION_INDICATORS.entrySet()) {
            String dimName = e.getKey();
            MatrixContext ctx = matrixResults.get(dimName);
            if (ctx != null) {
                result.put(dimName, new LinkedHashMap<>(ctx.weights));
            }
        }
        return result;
    }

    // ==================== 实体构建 ====================

    private ExpertAhpGroupWeights buildGroupWeightsEntity(
            String groupId,
            String expertIdsStr,
            List<Long> expertIds,
            Map<String, MatrixContext> matrixResults,
            Map<String, Double> dimWeights,
            Map<String, Double> combinedWeights,
            Map<String, Map<String, Double>> indWeightsByDim,
            Map<Long, Double> credMap,
            Map<String, Map<Long, Double>> perKeyWeights) {

        ExpertAhpGroupWeights entity = new ExpertAhpGroupWeights();
        entity.setGroupId(groupId);
        entity.setExpertIds(expertIdsStr);
        entity.setExpertCount(expertIds.size());

        // CR
        MatrixContext dimCtx = matrixResults.get("__dim__");
        entity.setCrDim(toBd(dimCtx != null ? dimCtx.cr : 0.0));
        for (String dim : DIMENSIONS) {
            MatrixContext ctx = matrixResults.get(dim);
            if (ctx != null) setCr(entity, dim, ctx.cr);
        }

        // 维度权重
        entity.setDimWeightSecurity(toBd(dimWeights.get("安全性")));
        entity.setDimWeightReliability(toBd(dimWeights.get("可靠性")));
        entity.setDimWeightTransmission(toBd(dimWeights.get("传输能力")));
        entity.setDimWeightAntiJamming(toBd(dimWeights.get("抗干扰能力")));
        entity.setDimWeightEffect(toBd(dimWeights.get("效能影响")));

        // 指标综合权重
        setIndWeight(entity, combinedWeights);

        // 专家权重JSON（可信度 + 各 key 下的归一化权重均值）
        try {
            List<Map<String, Object>> details = new ArrayList<>();
            for (Long expertId : expertIds) {
                Map<String, Object> d = new LinkedHashMap<>();
                d.put("expertId", expertId);
                ExpertCredibilityScore cred = credibilityRepository.findByExpertId(expertId).orElse(null);
                d.put("expertName", cred != null ? cred.getExpertName() : "未知");
                d.put("credibility", cred != null ? cred.getTotalScore() : BigDecimal.ZERO);
                // 该专家在各 key 上归一化权重的均值（作为整体参考）
                double avgW = perKeyWeights.values().stream()
                        .filter(w -> w.containsKey(expertId))
                        .mapToDouble(w -> w.get(expertId))
                        .average().orElse(0.5);
                d.put("avgNormalizedWeight", toBd(avgW));
                details.add(d);
            }
            entity.setExpertWeightsJson(objectMapper.writeValueAsString(details));
        } catch (Exception e) {
            log.warn("序列化专家权重JSON失败", e);
        }

        return entity;
    }

    private void setCr(ExpertAhpGroupWeights entity, String dim, double cr) {
        switch (dim) {
            case "安全性": entity.setCrSecurity(toBd(cr)); break;
            case "可靠性": entity.setCrReliability(toBd(cr)); break;
            case "传输能力": entity.setCrTransmission(toBd(cr)); break;
            case "抗干扰能力": entity.setCrAntiJamming(toBd(cr)); break;
            case "效能影响": entity.setCrEffect(toBd(cr)); break;
        }
    }

    private void setIndWeight(ExpertAhpGroupWeights entity, Map<String, Double> weights) {
        entity.setIndWeightKeyLeakage(toBd(weights.get("密钥泄露得分")));
        entity.setIndWeightDetectedProbability(toBd(weights.get("被侦察得分")));
        entity.setIndWeightInterceptionResistance(toBd(weights.get("抗拦截得分")));
        entity.setIndWeightCrashRate(toBd(weights.get("崩溃比例得分")));
        entity.setIndWeightRecoveryCapability(toBd(weights.get("恢复能力得分")));
        entity.setIndWeightCommunicationAvailability(toBd(weights.get("通信可用得分")));
        entity.setIndWeightBandwidth(toBd(weights.get("带宽得分")));
        entity.setIndWeightCallSetupTime(toBd(weights.get("呼叫建立得分")));
        entity.setIndWeightTransmissionDelay(toBd(weights.get("传输时延得分")));
        entity.setIndWeightBitErrorRate(toBd(weights.get("误码率得分")));
        entity.setIndWeightThroughput(toBd(weights.get("吞吐量得分")));
        entity.setIndWeightSpectralEfficiency(toBd(weights.get("频谱效率得分")));
        entity.setIndWeightSinr(toBd(weights.get("信干噪比得分")));
        entity.setIndWeightAntiJammingMargin(toBd(weights.get("抗干扰余量得分")));
        entity.setIndWeightCommunicationDistance(toBd(weights.get("通信距离得分")));
        entity.setIndWeightDamageRate(toBd(weights.get("战损率得分")));
        entity.setIndWeightMissionCompletionRate(toBd(weights.get("任务完成率得分")));
        entity.setIndWeightBlindRate(toBd(weights.get("致盲率得分")));
    }

    private List<CollectiveCalculateResponse.ExpertWeightDetail> buildExpertWeightDetails(
            List<Long> expertIds,
            Map<Long, Double> credMap,
            Map<String, Map<Long, Double>> perKeyWeights) {
        List<CollectiveCalculateResponse.ExpertWeightDetail> details = new ArrayList<>();
        for (Long expertId : expertIds) {
            CollectiveCalculateResponse.ExpertWeightDetail d = new CollectiveCalculateResponse.ExpertWeightDetail();
            d.setExpertId(expertId);

            ExpertCredibilityScore cred = credibilityRepository.findByExpertId(expertId).orElse(null);
            d.setExpertName(cred != null ? cred.getExpertName() : "未知");
            // 可信度得分（原始 0~100）
            BigDecimal credScore = cred != null ? cred.getTotalScore() : BigDecimal.ZERO;
            d.setCredibility(credScore);
            // 该专家在各 comparison_key 上归一化权重的均值
            double avgNormW = perKeyWeights.values().stream()
                    .filter(w -> w.containsKey(expertId))
                    .mapToDouble(w -> w.get(expertId))
                    .average().orElse(0.5);
            d.setNormalizedWeight(toBd(avgNormW));
            details.add(d);
        }
        return details;
    }

    // ==================== 辅助工具 ====================

    /**
     * 将原始指标字段名转为指标中文名
     */
    private static String translateFieldToIndicator(String field) {
        switch (field) {
            // 安全性
            case "security_key_leakage_count": return "密钥泄露得分";
            case "security_detected_probability": return "被侦察得分";
            case "security_interception_resistance_probability": return "抗拦截得分";
            // 可靠性
            case "reliability_crash_count": return "崩溃比例得分";
            case "reliability_recovery_time": return "恢复能力得分";
            case "reliability_communication_availability": return "通信可用得分";
            // 传输能力
            case "transmission_bandwidth": return "带宽得分";
            case "transmission_call_setup_time": return "呼叫建立得分";
            case "transmission_delay": return "传输时延得分";
            case "transmission_bit_error_rate": return "误码率得分";
            case "transmission_throughput": return "吞吐量得分";
            case "transmission_spectral_efficiency": return "频谱效率得分";
            // 抗干扰
            case "anti_jamming_sinr": return "信干噪比得分";
            case "anti_jamming_margin": return "抗干扰余量得分";
            case "anti_jamming_communication_distance": return "通信距离得分";
            // 效能
            case "effect_damage_rate": return "战损率得分";
            case "effect_mission_completion_rate": return "任务完成率得分";
            case "effect_blind_rate": return "致盲率得分";
            default: return field;
        }
    }

    /** 综合得分、维度分、指标归一化得分等对外展示：保留两位小数 */
    private static BigDecimal toBdScore2(Double v) {
        if (v == null || !Double.isFinite(v)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal toBd(Double v) {
        if (v == null || !Double.isFinite(v)) return BigDecimal.ZERO;
        return BigDecimal.valueOf(v).setScale(6, RoundingMode.HALF_UP);
    }

    private static BigDecimal toBd(BigDecimal v) {
        if (v == null) return BigDecimal.ZERO;
        return v.setScale(6, RoundingMode.HALF_UP);
    }

    private static Map<String, BigDecimal> toBdMap(Map<String, Double> map) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Map.Entry<String, Double> e : map.entrySet()) {
            result.put(e.getKey(), toBd(e.getValue()));
        }
        return result;
    }

    private static Map<String, Map<String, BigDecimal>> toBdMap2(Map<String, Map<String, Double>> map) {
        Map<String, Map<String, BigDecimal>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Double>> e : map.entrySet()) {
            result.put(e.getKey(), toBdMap(e.getValue()));
        }
        return result;
    }

    /**
     * 将 Map<comparisonKey, Map<expertId, weight>> 转为 BigDecimal 版本
     */
    private static Map<String, Map<String, BigDecimal>> toBdMapPerKey(
            Map<String, Map<Long, Double>> perKey) {
        Map<String, Map<String, BigDecimal>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Map<Long, Double>> e : perKey.entrySet()) {
            Map<String, BigDecimal> inner = new LinkedHashMap<>();
            for (Map.Entry<Long, Double> ie : e.getValue().entrySet()) {
                inner.put(String.valueOf(ie.getKey()), toBd(ie.getValue()));
            }
            result.put(e.getKey(), inner);
        }
        return result;
    }

    // ==================== 内部数据结构 ====================

    /**
     * 单个矩阵的计算结果上下文
     */
    private static class MatrixContext {
        /** 元素权重映射 */
        Map<String, Double> weights;
        /** 一致性比率 CR */
        double cr;
        /** 最大特征值 */
        double lambdaMax;
        /** 原始矩阵 */
        double[][] matrix;
        /** 元素名称列表 */
        List<String> elementNames;
    }
}
