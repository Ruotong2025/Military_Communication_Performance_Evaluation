package com.ccnu.military.service;

import com.ccnu.military.dto.ExpertAhpScoresPersistRequest;
import com.ccnu.military.dto.ExpertAhpSimulateRequest;
import com.ccnu.military.dto.MatrixCalculationRequest;
import com.ccnu.military.dto.MatrixCalculationResult;
import com.ccnu.military.entity.ExpertAhpComparisonScore;
import com.ccnu.military.entity.ExpertBaseInfo;
import com.ccnu.military.repository.ExpertAhpComparisonScoreRepository;
import com.ccnu.military.repository.ExpertBaseInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 装备操作 AHP 比较打分持久化与模拟生成
 * 域隔离：通过 comparison_key 前缀 "装备操作_" 与 "效能指标_" 分离两套数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentAhpScoreService {

    private static final String PREFIX = "装备操作_";
    private static final double CR_LIMIT = 0.1;
    private static final int WEIGHT_PAYLOAD_MAX_TRIES = 8;

    private final ExpertAhpComparisonScoreRepository scoreRepository;
    private final ExpertBaseInfoRepository expertBaseInfoRepository;
    private final EquipmentAhpService equipmentAhpService;
    private final ExpertAhpIndividualWeightsService individualWeightsService;
    @org.springframework.context.annotation.Lazy
    private final AhpIndividualService ahpIndividualService;

    /**
     * 查询某专家在装备操作域的比较打分
     */
    public List<ExpertAhpComparisonScore> listByExpert(Long expertId) {
        if (expertId == null) {
            return Collections.emptyList();
        }
        return scoreRepository.findByExpertIdAndComparisonKeyStartingWith(expertId, PREFIX);
    }

    /** 存在装备操作比较打分的专家 ID（用于离散度分析等） */
    public List<Long> listExpertIdsWithEquipmentScores() {
        List<Long> ids = scoreRepository.findDistinctExpertIdsByComparisonKeyStartingWith(PREFIX);
        return ids != null ? ids : Collections.emptyList();
    }

    /**
     * 从库中装备操作打分构造矩阵计算请求（comparison_key 含前缀，与保存/前端一致）
     */
    public MatrixCalculationRequest buildMatrixRequestFromStoredRows(Long expertId) {
        if (expertId == null) {
            return null;
        }
        List<ExpertAhpComparisonScore> rows = scoreRepository.findByExpertIdAndComparisonKeyStartingWith(expertId, PREFIX);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, ExpertAhpComparisonScore> byKey = new HashMap<>();
        for (ExpertAhpComparisonScore r : rows) {
            if (r.getComparisonKey() != null) {
                byKey.put(r.getComparisonKey(), r);
            }
        }
        List<String> dims = equipmentAhpService.getDimensions();
        List<MatrixCalculationRequest.MatrixEntry> dimEntries = new ArrayList<>();
        for (int i = 0; i < dims.size(); i++) {
            for (int j = i + 1; j < dims.size(); j++) {
                String key = PREFIX + dims.get(i) + "_" + dims.get(j);
                ExpertAhpComparisonScore s = byKey.get(key);
                if (s != null && s.getScore() != null) {
                    Double conf = s.getConfidence() != null ? s.getConfidence().doubleValue() : null;
                    dimEntries.add(new MatrixCalculationRequest.MatrixEntry(
                            key, s.getScore().doubleValue(), conf));
                }
            }
        }
        Map<String, List<Map<String, Object>>> dimInds = equipmentAhpService.getDimensionIndicators(null);
        Map<String, List<MatrixCalculationRequest.MatrixEntry>> indMap = new LinkedHashMap<>();
        for (String dim : dims) {
            List<Map<String, Object>> inds = dimInds.getOrDefault(dim, Collections.emptyList());
            List<MatrixCalculationRequest.MatrixEntry> entries = new ArrayList<>();
            for (int i = 0; i < inds.size(); i++) {
                for (int j = i + 1; j < inds.size(); j++) {
                    String n1 = (String) inds.get(i).get("name");
                    String n2 = (String) inds.get(j).get("name");
                    if (n1 == null || n2 == null) {
                        continue;
                    }
                    String key = PREFIX + dim + "_" + n1 + "_" + n2;
                    ExpertAhpComparisonScore s = byKey.get(key);
                    if (s != null && s.getScore() != null) {
                        Double conf = s.getConfidence() != null ? s.getConfidence().doubleValue() : null;
                        entries.add(new MatrixCalculationRequest.MatrixEntry(
                                key, s.getScore().doubleValue(), conf));
                    }
                }
            }
            indMap.put(dim, entries);
        }
        if (dimEntries.isEmpty()) {
            return null;
        }
        MatrixCalculationRequest req = new MatrixCalculationRequest();
        req.setDimensionMatrix(dimEntries);
        req.setIndicatorMatrices(indMap);
        return req;
    }

    /**
     * 根据库中装备操作打分计算 AHP 权重（用于离散度分析等，不写库）
     */
    public MatrixCalculationResult calculateFromStoredScores(Long expertId) {
        MatrixCalculationRequest req = buildMatrixRequestFromStoredRows(expertId);
        if (req == null) {
            return null;
        }
        return equipmentAhpService.calculate(req);
    }

    /**
     * 保存专家装备操作 AHP 打分（只删/写装备操作前缀，不影响效能指标数据）
     */
    @Transactional
    public int persist(ExpertAhpScoresPersistRequest req) {
        if (req.getExpertId() == null) {
            throw new IllegalArgumentException("expertId 不能为空");
        }
        ExpertBaseInfo expert = expertBaseInfoRepository.findById(req.getExpertId())
                .orElseThrow(() -> new IllegalArgumentException("专家不存在: " + req.getExpertId()));
        String name = req.getExpertName() != null && !req.getExpertName().isBlank()
                ? req.getExpertName()
                : expert.getExpertName();

        List<ExpertAhpComparisonScore> rows = buildRows(req.getExpertId(), name, req.getDimensionMatrix(), req.getIndicatorMatrices());
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("没有可保存的比较项");
        }

        // 仅删除装备操作前缀，不触碰效能指标数据
        scoreRepository.deleteByExpertIdAndComparisonKeyStartingWith(req.getExpertId(), PREFIX);
        scoreRepository.saveAll(rows);

        log.info("已保存装备操作 AHP 打分 expertId={} 条数={}", req.getExpertId(), rows.size());
        ahpIndividualService.persistUnified(req.getExpertId(), name);
        return rows.size();
    }

    /**
     * 批量模拟装备操作 AHP 打分（仅模拟装备操作前缀）
     */
    @Transactional
    public Map<String, Object> simulate(ExpertAhpSimulateRequest req) {
        if (req.getExpertIds() == null || req.getExpertIds().isEmpty()) {
            throw new IllegalArgumentException("请至少选择一名专家");
        }
        List<Long> inserted = new ArrayList<>();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        log.info("开始批量模拟装备操作 AHP 打分，专家数={}", req.getExpertIds().size());

        for (Long expertId : req.getExpertIds()) {
            if (expertId == null) continue;
            ExpertBaseInfo expert = expertBaseInfoRepository.findById(expertId)
                    .orElseThrow(() -> new IllegalArgumentException("专家不存在: " + expertId));
            MatrixCalculationRequest matrix = randomConsistentMatrixPayload(rnd);
            List<ExpertAhpComparisonScore> rows = buildRows(
                    expertId, expert.getExpertName(),
                    matrix.getDimensionMatrix(), matrix.getIndicatorMatrices());
            scoreRepository.deleteByExpertIdAndComparisonKeyStartingWith(expertId, PREFIX);
            scoreRepository.saveAll(rows);
            inserted.add(expertId);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("insertedExpertIds", inserted);
        out.put("insertedCount", inserted.size());
        log.info("批量模拟装备操作 AHP 打分结束，写入={}", inserted.size());
        return out;
    }

    // ─── 模拟数据生成（参考 ExpertAhpComparisonScoreService 模式） ───

    private MatrixCalculationRequest randomConsistentMatrixPayload(ThreadLocalRandom rnd) {
        for (int t = 0; t < WEIGHT_PAYLOAD_MAX_TRIES; t++) {
            MatrixCalculationRequest candidate = buildWeightProportionalPayload(rnd);
            if (candidate != null && allMatricesPassCr(candidate)) {
                return candidate;
            }
        }
        return buildAllOnesPayload(rnd);
    }

    private MatrixCalculationRequest buildWeightProportionalPayload(ThreadLocalRandom rnd) {
        List<String> dims = equipmentAhpService.getDimensions();
        List<MatrixCalculationRequest.MatrixEntry> dimEntries = buildEntriesFromRandomPositiveWeights(
                dims.toArray(new String[0]), rnd, PREFIX, null);
        Map<String, List<MatrixCalculationRequest.MatrixEntry>> indMap = new LinkedHashMap<>();
        Map<String, List<Map<String, Object>>> dimInds = equipmentAhpService.getDimensionIndicators(null);
        for (Map.Entry<String, List<Map<String, Object>>> e : dimInds.entrySet()) {
            String[] names = e.getValue().stream()
                    .map(m -> (String) m.get("name"))
                    .toArray(String[]::new);
            // 指标层 key 必须与前端/persist 一致：装备操作_{维度名}_{指标i}_{指标j}
            indMap.put(e.getKey(), buildEntriesFromRandomPositiveWeights(names, rnd, PREFIX, e.getKey()));
        }
        MatrixCalculationRequest req = new MatrixCalculationRequest();
        req.setDimensionMatrix(dimEntries);
        req.setIndicatorMatrices(indMap);
        return req;
    }

    /**
     * 生成高波动随机权重：
     * - 20% 专家：Dirichlet 风格（指数分布），产生极端比值（如 8:1）
     * - 40% 专家：均匀分布 [1, 9]，中等波动
     * - 40% 专家：先随机挑选 1～2 个"优势元素"放大 3～6 倍，其余均匀，产生局部极端
     */
    /**
     * @param indicatorDimensionName 非空时表示指标层，comparison_key 为 prefix + 维度名 + "_" + 指标i + "_" + 指标j；为空时表示维度层 prefix + 维度i + "_" + 维度j
     */
    private List<MatrixCalculationRequest.MatrixEntry> buildEntriesFromRandomPositiveWeights(
            String[] elements, ThreadLocalRandom rnd, String prefix, String indicatorDimensionName) {
        int n = elements.length;
        double[] a = new double[n];

        double roll = rnd.nextDouble();
        if (roll < 0.20) {
            // Dirichlet 指数风格：少数极大、多数极小，波动最剧烈
            double[] raw = new double[n];
            double sum = 0;
            for (int i = 0; i < n; i++) {
                raw[i] = -Math.log(rnd.nextDouble(1e-6, 1.0));
                sum += raw[i];
            }
            // 映射到 [1, 9] 范围，同时保留指数级的相对差异
            double base = rnd.nextDouble(0.5, 3.0);
            for (int i = 0; i < n; i++) {
                a[i] = base * (raw[i] / sum) * n * 3.5;
                a[i] = Math.max(0.11, Math.min(9.0, a[i]));
            }
        } else if (roll < 0.60) {
            // 均匀分布 [1, 9]，中等波动（原有逻辑）
            for (int i = 0; i < n; i++) {
                a[i] = rnd.nextDouble(1.0, 9.0);
            }
        } else {
            // 局部放大风格：挑选 1～2 个优势元素乘以 3～6 倍，其余 [1, 3]
            for (int i = 0; i < n; i++) {
                a[i] = rnd.nextDouble(1.0, 3.0);
            }
            int advantageCount = rnd.nextInt(1, Math.min(3, n));
            Set<Integer> chosen = new HashSet<>();
            while (chosen.size() < advantageCount) {
                chosen.add(rnd.nextInt(n));
            }
            for (int idx : chosen) {
                a[idx] = rnd.nextDouble(3.5, 7.0);
            }
        }

        List<MatrixCalculationRequest.MatrixEntry> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double score = a[i] / a[j];
                score = round2(score);
                score = Math.max(1.0 / 9.0, Math.min(9.0, score));
                String key = indicatorDimensionName != null && !indicatorDimensionName.isBlank()
                        ? prefix + indicatorDimensionName + "_" + elements[i] + "_" + elements[j]
                        : prefix + elements[i] + "_" + elements[j];
                list.add(new MatrixCalculationRequest.MatrixEntry(key, score, randomConfidence(rnd)));
            }
        }
        return list;
    }

    private boolean allMatricesPassCr(MatrixCalculationRequest req) {
        // 维度层 CR 校验
        List<String> dims = equipmentAhpService.getDimensions();
        MatrixCalculationResult.MatrixResult dr =
                buildMatrixResult(dims.toArray(new String[0]), req.getDimensionMatrix(), null);
        if (dr.getCr() >= CR_LIMIT) {
            return false;
        }
        // 指标层 CR 校验
        Map<String, List<Map<String, Object>>> dimInds = equipmentAhpService.getDimensionIndicators(null);
        Map<String, List<MatrixCalculationRequest.MatrixEntry>> ind = req.getIndicatorMatrices();
        if (ind == null) {
            return false;
        }
        for (Map.Entry<String, List<Map<String, Object>>> e : dimInds.entrySet()) {
            String[] names = e.getValue().stream()
                    .map(m -> (String) m.get("name"))
                    .toArray(String[]::new);
            List<MatrixCalculationRequest.MatrixEntry> entries = ind.get(e.getKey());
            if (entries == null || entries.isEmpty()) {
                continue;
            }
            MatrixCalculationResult.MatrixResult ir = buildMatrixResult(names, entries, e.getKey());
            if (ir.getCr() >= CR_LIMIT) {
                return false;
            }
        }
        return true;
    }

    /**
     * 简化版 CR 计算（不使用 ExpertAHPService.calculateSingleMatrix，保持一致性）
     */
    /**
     * @param indicatorDimensionName 指标层传入维度名，用于解析 装备操作_维度_指标1_指标2；维度层传 null
     */
    private MatrixCalculationResult.MatrixResult buildMatrixResult(
            String[] elements,
            List<MatrixCalculationRequest.MatrixEntry> entries,
            String indicatorDimensionName) {
        int n = elements.length;
        double[][] matrix = new double[n][n];
        for (int i = 0; i < n; i++) matrix[i][i] = 1.0;
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(elements[i], i);
        for (MatrixCalculationRequest.MatrixEntry e : entries) {
            if (e == null || e.getKey() == null) continue;
            Integer ia = null;
            Integer ib = null;
            if (indicatorDimensionName != null && !indicatorDimensionName.isBlank()) {
                for (int i = 0; i < n; i++) {
                    for (int j = i + 1; j < n; j++) {
                        String expected = PREFIX + indicatorDimensionName + "_" + elements[i] + "_" + elements[j];
                        if (expected.equals(e.getKey())) {
                            ia = i;
                            ib = j;
                            break;
                        }
                    }
                    if (ia != null) break;
                }
                // 兼容历史错误 key（仅 装备操作_指标1_指标2，无维度名）：按首尾名匹配
                if (ia == null) {
                    String key = e.getKey().replace(PREFIX, "");
                    String[] parts = key.split("_");
                    if (parts.length >= 2) {
                        String a = parts[0];
                        String b = parts[parts.length - 1];
                        ia = idx.get(a);
                        ib = idx.get(b);
                    }
                }
            } else {
                String key = e.getKey().replace(PREFIX, "");
                String[] parts = key.split("_");
                if (parts.length >= 2) {
                    String a = parts[0];
                    String b = parts[parts.length - 1];
                    ia = idx.get(a);
                    ib = idx.get(b);
                }
            }
            if (ia != null && ib != null) {
                matrix[ia][ib] = e.getScore();
                matrix[ib][ia] = 1.0 / e.getScore();
            }
        }
        double[] w = new double[n];
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            double colProd = 1.0;
            for (int j = 0; j < n; j++) colProd *= matrix[i][j];
            w[i] = Math.pow(colProd, 1.0 / n);
            sum += w[i];
        }
        for (int i = 0; i < n; i++) w[i] /= sum;
        double lambdaMax = 0.0;
        for (int i = 0; i < n; i++) {
            double s = 0.0;
            for (int j = 0; j < n; j++) s += matrix[i][j] * w[j];
            s /= w[i];
            lambdaMax += s;
        }
        lambdaMax /= n;
        double ci = (lambdaMax - n) / (n - 1);
        double[] riTable = {0, 0, 0.58, 0.90, 1.12, 1.24, 1.32, 1.41, 1.45, 1.49};
        double ri = n < riTable.length ? riTable[n] : 1.45;
        double cr = ci / ri;
        MatrixCalculationResult.MatrixResult r = new MatrixCalculationResult.MatrixResult();
        r.setWeights(w);
        r.setLambdaMax(round2(lambdaMax));
        r.setCr(round2(cr));
        return r;
    }

    private MatrixCalculationRequest buildAllOnesPayload(ThreadLocalRandom rnd) {
        List<String> dims = equipmentAhpService.getDimensions();
        List<MatrixCalculationRequest.MatrixEntry> dimEntries = new ArrayList<>();
        for (int i = 0; i < dims.size(); i++) {
            for (int j = i + 1; j < dims.size(); j++) {
                dimEntries.add(new MatrixCalculationRequest.MatrixEntry(
                        PREFIX + dims.get(i) + "_" + dims.get(j), 1.0, randomConfidence(rnd)));
            }
        }
        Map<String, List<Map<String, Object>>> dimInds = equipmentAhpService.getDimensionIndicators(null);
        Map<String, List<MatrixCalculationRequest.MatrixEntry>> indMap = new LinkedHashMap<>();
        for (Map.Entry<String, List<Map<String, Object>>> e : dimInds.entrySet()) {
            List<Map<String, Object>> inds = e.getValue();
            List<MatrixCalculationRequest.MatrixEntry> list = new ArrayList<>();
            for (int i = 0; i < inds.size(); i++) {
                for (int j = i + 1; j < inds.size(); j++) {
                    list.add(new MatrixCalculationRequest.MatrixEntry(
                            PREFIX + e.getKey() + "_" + inds.get(i).get("name") + "_" + inds.get(j).get("name"),
                            1.0, randomConfidence(rnd)));
                }
            }
            indMap.put(e.getKey(), list);
        }
        MatrixCalculationRequest req = new MatrixCalculationRequest();
        req.setDimensionMatrix(dimEntries);
        req.setIndicatorMatrices(indMap);
        return req;
    }

    private double randomConfidence(ThreadLocalRandom rnd) {
        if (rnd.nextDouble() < 0.20) {
            return round2(rnd.nextDouble(0.35, 0.59));
        }
        return round2(rnd.nextDouble(0.62, 0.96));
    }

    private static double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private List<ExpertAhpComparisonScore> buildRows(
            Long expertId,
            String expertName,
            List<MatrixCalculationRequest.MatrixEntry> dimensionMatrix,
            Map<String, List<MatrixCalculationRequest.MatrixEntry>> indicatorMatrices) {

        List<ExpertAhpComparisonScore> rows = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        if (dimensionMatrix != null) {
            for (MatrixCalculationRequest.MatrixEntry e : dimensionMatrix) {
                if (e == null || e.getKey() == null || e.getScore() == null) continue;
                rows.add(row(expertId, expertName, e.getKey(), e.getScore(), e.getConfidence(), now));
            }
        }
        if (indicatorMatrices != null) {
            for (List<MatrixCalculationRequest.MatrixEntry> list : indicatorMatrices.values()) {
                if (list == null) continue;
                for (MatrixCalculationRequest.MatrixEntry e : list) {
                    if (e == null || e.getKey() == null || e.getScore() == null) continue;
                    rows.add(row(expertId, expertName, e.getKey(), e.getScore(), e.getConfidence(), now));
                }
            }
        }
        return rows;
    }

    private ExpertAhpComparisonScore row(
            Long expertId,
            String expertName,
            String key,
            double score,
            Double confidence,
            LocalDateTime now) {

        ExpertAhpComparisonScore s = new ExpertAhpComparisonScore();
        s.setExpertId(expertId);
        s.setExpertName(expertName);
        s.setComparisonKey(key);
        double sc = score;
        if (!Double.isFinite(sc)) sc = 1.0;
        sc = Math.max(1.0 / 9.0, Math.min(9.0, sc));
        s.setScore(BigDecimal.valueOf(sc).setScale(2, RoundingMode.HALF_UP));
        double c = confidence != null ? confidence : 0.55;
        c = Math.min(1.0, Math.max(0.0, c));
        s.setConfidence(BigDecimal.valueOf(c).setScale(2, RoundingMode.HALF_UP));
        s.setCreateTime(now);
        return s;
    }
}
