package com.ccnu.military.service;

import com.ccnu.military.entity.AhpCommunicationScoringResult;
import com.ccnu.military.entity.EquipmentQtEvaluationRecord;
import com.ccnu.military.entity.ExpertAhpGroupWeights;
import com.ccnu.military.repository.AhpCommunicationScoringResultRepository;
import com.ccnu.military.repository.EquipmentQtEvaluationRecordRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 综合评分服务
 *
 * 综合评分计算逻辑：
 * - 效能指标：来自 score_military_comm_effect 表 (evaluation_batch_id 关联)，字段为归一化得分
 * - 装备定量指标：来自 equipment_qt_evaluation_record 表的 normalized_scores 字段
 * - 装备定性指标：来自 equipment_ql_aggregation_result 表的 x_star 质心值
 * - 权重数据：来自 expert_ahp_group_weights 表
 *
 * 综合评分 = 效能综合分 + 装备综合分
 * 效能综合分 = Σ(效能叶子全局权重 × score_military_comm_effect中的归一化得分)
 * 装备综合分 = Σ(装备叶子全局权重 × (定量归一化得分 | 定性质心分))
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComprehensiveScoringService {

    private final JdbcTemplate jdbcTemplate;
    private final AhpCommunicationScoringResultRepository scoringResultRepository;
    private final EquipmentQtEvaluationRecordRepository qtRecordRepository;
    private final ObjectMapper objectMapper;

    /**
     * 装备指标名称映射：
     * key = AHP权重中的中文指标名称
     * value = normalized_scores/aggregation_result 中的英文键名
     */
    private static final Map<String, String[]> EQUIPMENT_INDICATOR_MAPPING = new LinkedHashMap<>();

    static {
        // 定量指标映射 (equipment_qt_evaluation_record.normalized_scores)
        EQUIPMENT_INDICATOR_MAPPING.put("连通率", new String[]{"eqt_connectivity_rate"});
        EQUIPMENT_INDICATOR_MAPPING.put("应急处理", new String[]{"eqt_emergency_handling"});
        EQUIPMENT_INDICATOR_MAPPING.put("组网时长", new String[]{"eqt_network_setup_time"});
        EQUIPMENT_INDICATOR_MAPPING.put("业务开通", new String[]{"eqt_service_activation"});
        EQUIPMENT_INDICATOR_MAPPING.put("干扰目标锁定时间", new String[]{"eqt_jamming_target_time"});
        EQUIPMENT_INDICATOR_MAPPING.put("干扰目标截获概率", new String[]{"eqt_jamming_target_time"});
        EQUIPMENT_INDICATOR_MAPPING.put("干扰效能达成率", new String[]{"eqt_jamming_effectiveness", "eql_jamming_effectiveness"});
        EQUIPMENT_INDICATOR_MAPPING.put("任务可靠度", new String[]{"eqt_mission_reliability"});
        EQUIPMENT_INDICATOR_MAPPING.put("应急抢通", new String[]{"eqt_emergency_restoration"});
        EQUIPMENT_INDICATOR_MAPPING.put("欺骗信号生成", new String[]{"eqt_deception_success_rate", "eql_deception_signal"});
        EQUIPMENT_INDICATOR_MAPPING.put("抗截获通信", new String[]{"eql_signal_interception"});
        EQUIPMENT_INDICATOR_MAPPING.put("抗毁路由", new String[]{"eqt_link_maintenance"});
        EQUIPMENT_INDICATOR_MAPPING.put("返修率", new String[]{"eqt_rework_rate"});

        // 定性指标映射 (equipment_ql_aggregation_result.x_star)
        EQUIPMENT_INDICATOR_MAPPING.put("抗干扰操作", new String[]{"eql_anti_jamming"});
        EQUIPMENT_INDICATOR_MAPPING.put("防骗反骗", new String[]{"eql_anti_deception"});
        EQUIPMENT_INDICATOR_MAPPING.put("欺骗信号", new String[]{"eql_deception_signal"});
        EQUIPMENT_INDICATOR_MAPPING.put("维修能力", new String[]{"eql_maintenance_skill"});
        EQUIPMENT_INDICATOR_MAPPING.put("信号截获感知", new String[]{"eql_signal_interception"});
        EQUIPMENT_INDICATOR_MAPPING.put("干扰效能", new String[]{"eql_jamming_effectiveness"});
        EQUIPMENT_INDICATOR_MAPPING.put("装备反馈", new String[]{"eql_equipment_feedback"});
    }

    /**
     * 执行综合评分计算
     */
    @Transactional
    public Map<String, Object> calculateComprehensiveScores(String evaluationBatchId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("evaluationBatchId", evaluationBatchId);

        try {
            // 1. 获取集结权重
            ExpertAhpGroupWeights groupWeights = getLatestGroupWeights();
            if (groupWeights == null) {
                result.put("success", false);
                result.put("message", "未找到集结权重，请先执行集结计算");
                return result;
            }

            // 2. 解析统一快照（使用 Map 结构）
            Map<String, Object> unified = parseUnifiedSnapshot(groupWeights);
            if (unified == null) {
                result.put("success", false);
                result.put("message", "集结权重快照数据为空，请重新执行集结计算");
                return result;
            }

            // 3. 获取该批次下的所有作战
            List<String> operationIds = getOperationIdsForBatch(evaluationBatchId);
            if (operationIds.isEmpty()) {
                result.put("success", false);
                result.put("message", "该批次下没有作战记录");
                return result;
            }

            // 4. 获取各部分得分
            // 效能归一化得分 (来自 score_military_comm_effect 表)
            Map<String, Map<String, Double>> effScores = getEffectivenessNormalizedScores(evaluationBatchId);
            // 装备定量归一化得分 (来自 equipment_qt_evaluation_record.normalized_scores)
            Map<String, Map<String, Double>> eqQtScores = getEquipmentQtNormalizedScores(evaluationBatchId);
            // 装备定性质心得分 (来自 equipment_ql_aggregation_result.x_star)
            Map<String, Map<String, Double>> eqQlScores = getEquipmentQlAggregationScores(evaluationBatchId);

            // 5. 获取域间权重
            Map<String, Double> crossDomain = parseCrossDomain(unified);
            double wEff = crossDomain.getOrDefault("effWeight", 0.5);
            double wEq = crossDomain.getOrDefault("eqWeight", 0.5);

            // 6. 计算每个作战的综合得分
            List<Map<String, Object>> operationResults = new ArrayList<>();
            for (String opId : operationIds) {
                Map<String, Object> opResult = calculateOperationScore(
                        opId, unified, effScores.get(opId), eqQtScores.get(opId), eqQlScores.get(opId), wEff, wEq);
                operationResults.add(opResult);
            }

            // 7. 保存结果
            saveScoringResults(evaluationBatchId, operationResults, groupWeights, wEff, wEq);

            // 8. 提取维度权重（从第一个结果中获取）
            Map<String, Object> dimWeights = null;
            if (!operationResults.isEmpty()) {
                dimWeights = (Map<String, Object>) operationResults.get(0).get("dimensionWeights");
            }

            result.put("success", true);
            result.put("message", String.format("综合评分计算完成，共 %d 个作战", operationIds.size()));
            result.put("operationCount", operationIds.size());
            result.put("results", operationResults);
            result.put("weightSnapshot", Map.of("effDomainWeight", round6(wEff), "eqDomainWeight", round6(wEq)));
            result.put("dimensionWeights", dimWeights);

        } catch (Exception e) {
            log.error("综合评分计算失败", e);
            result.put("success", false);
            result.put("message", "综合评分计算失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取最新的集结权重记录
     */
    private ExpertAhpGroupWeights getLatestGroupWeights() {
        try {
            List<ExpertAhpGroupWeights> list = jdbcTemplate.query(
                    "SELECT * FROM expert_ahp_group_weights ORDER BY updated_at DESC LIMIT 1",
                    (rs, rowNum) -> mapGroupWeights(rs));
            return list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            log.error("获取集结权重失败", e);
            return null;
        }
    }

    private ExpertAhpGroupWeights mapGroupWeights(java.sql.ResultSet rs) throws java.sql.SQLException {
        ExpertAhpGroupWeights gw = new ExpertAhpGroupWeights();
        gw.setId(rs.getLong("id"));
        gw.setGroupId(rs.getString("group_id"));
        gw.setExpertIds(rs.getString("expert_ids"));
        gw.setExpertCount(rs.getObject("expert_count") != null ? rs.getInt("expert_count") : 0);
        gw.setAggregatedUnifiedJson(rs.getString("aggregated_unified_json"));
        gw.setEffDomainWeight(getBigDecimal(rs, "eff_domain_weight"));
        gw.setEqDomainWeight(getBigDecimal(rs, "eq_domain_weight"));
        gw.setEffLeafWeightsJson(rs.getString("eff_leaf_weights_json"));
        gw.setEqLeafWeightsJson(rs.getString("eq_leaf_weights_json"));
        gw.setEffDimWeightsJson(rs.getString("eff_dim_weights_json"));
        gw.setEqDimWeightsJson(rs.getString("eq_dim_weights_json"));
        gw.setExpertWeightsJson(rs.getString("expert_weights_json"));
        return gw;
    }

    private BigDecimal getBigDecimal(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        BigDecimal val = rs.getBigDecimal(column);
        return rs.wasNull() ? null : val;
    }

    /**
     * 解析统一快照 JSON（使用 Map 结构）
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseUnifiedSnapshot(ExpertAhpGroupWeights gw) {
        if (gw == null || gw.getAggregatedUnifiedJson() == null || gw.getAggregatedUnifiedJson().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(gw.getAggregatedUnifiedJson(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("解析统一快照失败，尝试从叶子权重 JSON 解析", e);
            return buildFallbackSnapshot(gw);
        }
    }

    /**
     * 当 unified JSON 解析失败时，从叶子权重 JSON 构建快照
     *
     * 权重结构：
     * - dimensionWeights: 维度在域内的权重（维度权重，0~1，相加为1）
     * - indicators: 指标在维度内的局部权重（指标权重，0~1，每个维度内相加为1）
     *
     * 综合分计算：Σ(维度权重 × Σ(指标局部权重 × score))
     * 维度得分计算：Σ(指标局部权重 × score)
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> buildFallbackSnapshot(ExpertAhpGroupWeights gw) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        // 域间权重
        Map<String, Object> crossDomain = new LinkedHashMap<>();
        crossDomain.put("effWeight", gw.getEffDomainWeight() != null ? gw.getEffDomainWeight().doubleValue() : 0.5);
        crossDomain.put("eqWeight", gw.getEqDomainWeight() != null ? gw.getEqDomainWeight().doubleValue() : 0.5);
        snapshot.put("crossDomain", crossDomain);

        // 效能叶子权重
        if (gw.getEffLeafWeightsJson() != null && !gw.getEffLeafWeightsJson().isBlank()) {
            try {
                List<Map<String, Object>> leaves = objectMapper.readValue(
                        gw.getEffLeafWeightsJson(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

                // 1. 按维度分组收集原始权重
                Map<String, Double> dimGlobalWeights = new LinkedHashMap<>(); // 维度全局权重（用于计算维度权重）
                Map<String, Map<String, Double>> dimIndicatorGlobals = new LinkedHashMap<>(); // 维度内指标的全局权重

                for (Map<String, Object> leaf : leaves) {
                    String dim = (String) leaf.get("dim");
                    String indicator = (String) leaf.get("indicator");
                    Number weightNum = (Number) leaf.get("globalWeight");
                    double weight = weightNum != null ? weightNum.doubleValue() : 0.0;

                    dimGlobalWeights.merge(dim, weight, Double::sum);
                    dimIndicatorGlobals.computeIfAbsent(dim, k -> new LinkedHashMap<>()).put(indicator, weight);
                }

                // 2. 计算维度权重（归一化到0~1）
                double totalDimWeight = dimGlobalWeights.values().stream().mapToDouble(Double::doubleValue).sum();
                Map<String, Double> dimWeights = new LinkedHashMap<>();
                for (Map.Entry<String, Double> entry : dimGlobalWeights.entrySet()) {
                    dimWeights.put(entry.getKey(), totalDimWeight > 0 ? entry.getValue() / totalDimWeight : 0.0);
                }

                // 3. 计算指标局部权重（每个维度内归一化到0~1）
                Map<String, Map<String, Double>> indicators = new LinkedHashMap<>();
                for (Map.Entry<String, Map<String, Double>> dimEntry : dimIndicatorGlobals.entrySet()) {
                    String dim = dimEntry.getKey();
                    double dimTotal = dimEntry.getValue().values().stream().mapToDouble(Double::doubleValue).sum();
                    Map<String, Double> localWeights = new LinkedHashMap<>();
                    for (Map.Entry<String, Double> indEntry : dimEntry.getValue().entrySet()) {
                        localWeights.put(indEntry.getKey(), dimTotal > 0 ? indEntry.getValue() / dimTotal : 0.0);
                    }
                    indicators.put(dim, localWeights);
                }

                Map<String, Object> effectiveness = new LinkedHashMap<>();
                effectiveness.put("dimensionWeights", dimWeights);
                effectiveness.put("indicators", indicators);
                snapshot.put("effectiveness", effectiveness);

                log.debug("构建效能快照 - 维度权重: {}, 指标: {}", dimWeights, indicators);
            } catch (Exception e) {
                log.warn("解析效能叶子权重失败", e);
            }
        }

        // 装备叶子权重
        if (gw.getEqLeafWeightsJson() != null && !gw.getEqLeafWeightsJson().isBlank()) {
            try {
                List<Map<String, Object>> leaves = objectMapper.readValue(
                        gw.getEqLeafWeightsJson(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

                // 1. 按维度分组收集原始权重
                Map<String, Double> dimGlobalWeights = new LinkedHashMap<>();
                Map<String, Map<String, Double>> dimIndicatorGlobals = new LinkedHashMap<>();

                for (Map<String, Object> leaf : leaves) {
                    String dim = (String) leaf.get("dim");
                    String indicator = (String) leaf.get("indicator");
                    Number weightNum = (Number) leaf.get("globalWeight");
                    double weight = weightNum != null ? weightNum.doubleValue() : 0.0;

                    dimGlobalWeights.merge(dim, weight, Double::sum);
                    dimIndicatorGlobals.computeIfAbsent(dim, k -> new LinkedHashMap<>()).put(indicator, weight);
                }

                // 2. 计算维度权重
                double totalDimWeight = dimGlobalWeights.values().stream().mapToDouble(Double::doubleValue).sum();
                Map<String, Double> dimWeights = new LinkedHashMap<>();
                for (Map.Entry<String, Double> entry : dimGlobalWeights.entrySet()) {
                    dimWeights.put(entry.getKey(), totalDimWeight > 0 ? entry.getValue() / totalDimWeight : 0.0);
                }

                // 3. 计算指标局部权重
                Map<String, Map<String, Double>> indicators = new LinkedHashMap<>();
                for (Map.Entry<String, Map<String, Double>> dimEntry : dimIndicatorGlobals.entrySet()) {
                    String dim = dimEntry.getKey();
                    double dimTotal = dimEntry.getValue().values().stream().mapToDouble(Double::doubleValue).sum();
                    Map<String, Double> localWeights = new LinkedHashMap<>();
                    for (Map.Entry<String, Double> indEntry : dimEntry.getValue().entrySet()) {
                        localWeights.put(indEntry.getKey(), dimTotal > 0 ? indEntry.getValue() / dimTotal : 0.0);
                    }
                    indicators.put(dim, localWeights);
                }

                Map<String, Object> equipment = new LinkedHashMap<>();
                equipment.put("dimensionWeights", dimWeights);
                equipment.put("indicators", indicators);
                snapshot.put("equipment", equipment);

                log.debug("构建装备快照 - 维度权重: {}, 指标: {}", dimWeights, indicators);
            } catch (Exception e) {
                log.warn("解析装备叶子权重失败", e);
            }
        }

        return snapshot;
    }

    /**
     * 解析域间权重
     */
    @SuppressWarnings("unchecked")
    private Map<String, Double> parseCrossDomain(Map<String, Object> unified) {
        Map<String, Double> result = new LinkedHashMap<>();
        result.put("effWeight", 0.5);
        result.put("eqWeight", 0.5);

        if (unified != null && unified.containsKey("crossDomain")) {
            Map<String, Object> cd = (Map<String, Object>) unified.get("crossDomain");
            if (cd != null) {
                if (cd.containsKey("effWeight")) {
                    result.put("effWeight", ((Number) cd.get("effWeight")).doubleValue());
                }
                if (cd.containsKey("eqWeight")) {
                    result.put("eqWeight", ((Number) cd.get("eqWeight")).doubleValue());
                }
            }
        }
        return result;
    }

    /**
     * 获取该批次下的所有作战ID
     * 优先从 score_military_comm_effect (evaluation_batch_id) 获取
     */
    private List<String> getOperationIdsForBatch(String evaluationBatchId) {
        try {
            // 优先从 score_military_comm_effect 获取 (使用 evaluation_batch_id)
            List<String> fromScore = jdbcTemplate.queryForList(
                    "SELECT DISTINCT operation_id FROM score_military_comm_effect WHERE evaluation_batch_id = ? ORDER BY operation_id",
                    String.class, evaluationBatchId);
            if (!fromScore.isEmpty()) {
                return fromScore;
            }
            // 回退：从 equipment_qt_evaluation_record 获取
            return qtRecordRepository.findByEvaluationBatchIdOrderByOperationId(evaluationBatchId)
                    .stream()
                    .map(EquipmentQtEvaluationRecord::getOperationId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        } catch (Exception e) {
            log.warn("获取作战ID失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取效能归一化得分 (来自 score_military_comm_effect 表)
     * 字段映射：score 表字段名 -> 指标名
     */
    private Map<String, Map<String, Double>> getEffectivenessNormalizedScores(String evaluationBatchId) {
        Map<String, Map<String, Double>> result = new LinkedHashMap<>();

        // 字段映射：score 表字段名 -> 指标名
        Map<String, String> fieldToIndicator = new LinkedHashMap<>();
        fieldToIndicator.put("security_key_leakage_qt", "密钥泄露得分");
        fieldToIndicator.put("security_detected_probability_qt", "被侦察得分");
        fieldToIndicator.put("security_interception_resistance_ql", "抗拦截得分");
        fieldToIndicator.put("reliability_crash_rate_qt", "崩溃比例得分");
        fieldToIndicator.put("reliability_recovery_capability_qt", "恢复能力得分");
        fieldToIndicator.put("reliability_communication_availability_qt", "通信可用得分");
        fieldToIndicator.put("transmission_bandwidth_qt", "带宽得分");
        fieldToIndicator.put("transmission_call_setup_time_qt", "呼叫建立得分");
        fieldToIndicator.put("transmission_transmission_delay_qt", "传输时延得分");
        fieldToIndicator.put("transmission_bit_error_rate_qt", "误码率得分");
        fieldToIndicator.put("transmission_throughput_qt", "吞吐量得分");
        fieldToIndicator.put("transmission_spectral_efficiency_qt", "频谱效率得分");
        fieldToIndicator.put("anti_jamming_sinr_qt", "信干噪比得分");
        fieldToIndicator.put("anti_jamming_anti_jamming_margin_qt", "抗干扰余量得分");
        fieldToIndicator.put("anti_jamming_communication_distance_qt", "通信距离得分");
        fieldToIndicator.put("effect_damage_rate_qt", "战损率得分");
        fieldToIndicator.put("effect_mission_completion_rate_qt", "任务完成率得分");
        fieldToIndicator.put("effect_blind_rate_qt", "致盲率得分");

        try {
            List<String> fields = new ArrayList<>(fieldToIndicator.keySet());

            // 使用 evaluation_batch_id 查询
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT operation_id, " + String.join(", ", fields) + " FROM score_military_comm_effect WHERE evaluation_batch_id = ?",
                    evaluationBatchId);

            for (Map<String, Object> row : rows) {
                String opId = String.valueOf(row.get("operation_id"));
                Map<String, Double> scores = new LinkedHashMap<>();
                for (Map.Entry<String, String> e : fieldToIndicator.entrySet()) {
                    Object val = row.get(e.getKey());
                    scores.put(e.getValue(), val != null ? ((Number) val).doubleValue() : 0.0);
                }
                result.put(opId, scores);
            }
        } catch (Exception e) {
            log.warn("获取效能归一化得分失败", e);
        }
        return result;
    }

    /**
     * 获取装备定量归一化得分 (来自 equipment_qt_evaluation_record.normalized_scores)
     */
    private Map<String, Map<String, Double>> getEquipmentQtNormalizedScores(String evaluationBatchId) {
        Map<String, Map<String, Double>> result = new LinkedHashMap<>();
        try {
            log.info("获取装备定量归一化得分, evaluationBatchId={}", evaluationBatchId);
            List<EquipmentQtEvaluationRecord> records =
                    qtRecordRepository.findByEvaluationBatchIdOrderByOperationId(evaluationBatchId);
            log.info("找到 {} 条装备定量记录", records.size());

            for (EquipmentQtEvaluationRecord rec : records) {
                String opId = rec.getOperationId();
                Map<String, Object> normalized = parseJsonToMap(rec.getNormalizedScores());
                Map<String, Double> scores = new LinkedHashMap<>();
                for (Map.Entry<String, Object> e : normalized.entrySet()) {
                    if (e.getValue() instanceof Number) {
                        scores.put(e.getKey(), ((Number) e.getValue()).doubleValue());
                    }
                }
                log.debug("装备定量数据 - operationId={}, scores={}", opId, scores.keySet());
                result.put(opId, scores);
            }
        } catch (Exception e) {
            log.error("获取装备定量归一化得分失败", e);
        }
        return result;
    }

    /**
     * 获取装备定性集结结果 (来自 equipment_ql_aggregation_result 表的 x_star 质心值)
     * x_star 已经是质心分，直接使用
     */
    private Map<String, Map<String, Double>> getEquipmentQlAggregationScores(String evaluationBatchId) {
        Map<String, Map<String, Double>> result = new LinkedHashMap<>();
        try {
            log.info("获取装备定性集结结果, evaluationBatchId={}", evaluationBatchId);
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT operation_id, indicator_key, x_star FROM equipment_ql_aggregation_result WHERE evaluation_batch_id = ?",
                    evaluationBatchId);
            log.info("找到 {} 条装备定性集结记录", rows.size());

            for (Map<String, Object> row : rows) {
                String opId = String.valueOf(row.get("operation_id"));
                String indicatorKey = String.valueOf(row.get("indicator_key"));
                Object xStarObj = row.get("x_star");
                // x_star 已经是质心分，直接使用（假设 x_star 范围是 0-100）
                double xStar = xStarObj != null ? new BigDecimal(xStarObj.toString()).doubleValue() : 0.0;
                // 归一化到 0~1
                double normalized = Math.max(0, Math.min(1, xStar / 100.0));
                log.debug("定性数据 - operationId={}, indicatorKey={}, x_star={}, normalized={}",
                        opId, indicatorKey, xStar, normalized);
                result.computeIfAbsent(opId, k -> new LinkedHashMap<>()).put(indicatorKey, normalized);
            }
        } catch (Exception e) {
            log.error("获取装备定性集结结果(x_star质心)失败", e);
        }
        return result;
    }

    /**
     * 计算单个作战的综合得分
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> calculateOperationScore(
            String operationId,
            Map<String, Object> unified,
            Map<String, Double> effScores,
            Map<String, Double> eqQtScores,
            Map<String, Double> eqQlScores,
            double wEff,
            double wEq) {

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("operationId", operationId);
        result.put("effDomainWeight", round6(wEff));
        result.put("eqDomainWeight", round6(wEq));

        // 1. 计算效能综合分
        double effScore = calculateEffectivenessScore(unified, effScores);
        result.put("effectivenessScore", round6(effScore));

        // 2. 计算装备综合分 (定量 + 定性质心)
        double eqScore = calculateEquipmentScore(unified, eqQtScores, eqQlScores);
        result.put("equipmentScore", round6(eqScore));

        // 3. 计算总分
        double totalScore = wEff * effScore + wEq * eqScore;
        result.put("totalScore", round6(totalScore));

        // 4. 维度得分
        Map<String, Double> dimScores = calculateDimensionScores(unified, effScores, eqQtScores);
        result.put("dimensionScores", dimScores);

        // 5. 指标得分（用于细分图表）- effScores/eqQtScores/eqQlScores 已经是当前作战的得分
        Map<String, Map<String, Double>> indicatorScores = calculateIndicatorScores(unified, effScores, eqQtScores, eqQlScores);
        result.put("indicatorScores", indicatorScores);

        // 6. 维度权重（用于图表标注）
        Map<String, Double> dimWeights = calculateDimensionWeights(unified);
        result.put("dimensionWeights", dimWeights);

        return result;
    }

    /**
     * 计算维度权重（用于图表标注）
     */
    @SuppressWarnings("unchecked")
    private Map<String, Double> calculateDimensionWeights(Map<String, Object> unified) {
        Map<String, Double> result = new LinkedHashMap<>();

        Map<String, Object> effectiveness = (Map<String, Object>) unified.get("effectiveness");
        if (effectiveness != null) {
            Map<String, Double> dimWeights = (Map<String, Double>) effectiveness.get("dimensionWeights");
            if (dimWeights != null) {
                for (Map.Entry<String, Double> entry : dimWeights.entrySet()) {
                    result.put("效能_" + entry.getKey(), round6(entry.getValue()));
                }
            }
        }

        Map<String, Object> equipment = (Map<String, Object>) unified.get("equipment");
        if (equipment != null) {
            Map<String, Double> dimWeights = (Map<String, Double>) equipment.get("dimensionWeights");
            if (dimWeights != null) {
                for (Map.Entry<String, Double> entry : dimWeights.entrySet()) {
                    result.put("装备_" + entry.getKey(), round6(entry.getValue()));
                }
            }
        }

        return result;
    }

    /**
     * 计算指标得分（用于细分图表）
     */
    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Double>> calculateIndicatorScores(
            Map<String, Object> unified,
            Map<String, Double> effScores,
            Map<String, Double> eqQtScores,
            Map<String, Double> eqQlScores) {

        Map<String, Map<String, Double>> result = new LinkedHashMap<>();

        // 添加调试日志
        log.info("=== 开始计算指标得分 ===");
        log.info("unified.effectiveness 存在: {}", unified.get("effectiveness") != null);
        log.info("effScores 记录数: {}", effScores != null ? effScores.size() : 0);
        if (effScores != null && !effScores.isEmpty()) {
            log.info("effScores 的 operationIds: {}", effScores.keySet());
        }

        // 效能指标
        Map<String, Object> effectiveness = (Map<String, Object>) unified.get("effectiveness");
        if (effectiveness != null) {
            Map<String, Map<String, Double>> indicators = (Map<String, Map<String, Double>>) effectiveness.get("indicators");
            log.info("indicators 存在: {}, 维度数量: {}", indicators != null, indicators != null ? indicators.size() : 0);
            if (indicators != null) {
                for (Map.Entry<String, Map<String, Double>> dimEntry : indicators.entrySet()) {
                    String dimName = "效能_" + dimEntry.getKey();
                    log.info("处理维度: {}, 指标数量: {}", dimName, dimEntry.getValue().size());
                    Map<String, Double> indScores = new LinkedHashMap<>();
                    for (Map.Entry<String, Double> indEntry : dimEntry.getValue().entrySet()) {
                        double score = findScore(effScores, indEntry.getKey());
                        log.debug("指标 {} 得分: {}", indEntry.getKey(), score);
                        indScores.put(indEntry.getKey(), round6(score));
                    }
                    result.put(dimName, indScores);
                }
            }
        } else {
            log.warn("unified.effectiveness 为 null!");
        }

        log.info("=== 指标得分计算完成: {} 个维度 ===", result.size());

        // 装备指标
        Map<String, Object> equipment = (Map<String, Object>) unified.get("equipment");
        if (equipment != null) {
            Map<String, Map<String, Double>> indicators = (Map<String, Map<String, Double>>) equipment.get("indicators");
            if (indicators != null) {
                log.debug("装备指标 - 维度数量: {}, 维度: {}", indicators.size(), indicators.keySet());
                for (Map.Entry<String, Map<String, Double>> dimEntry : indicators.entrySet()) {
                    String dimName = "装备_" + dimEntry.getKey();
                    Map<String, Double> indScores = new LinkedHashMap<>();
                    for (Map.Entry<String, Double> indEntry : dimEntry.getValue().entrySet()) {
                        // 优先定量，其次定性
                        double score = findScore(eqQtScores, indEntry.getKey());
                        if (score == 0.0 && eqQlScores != null) {
                            score = findScore(eqQlScores, indEntry.getKey());
                        }
                        indScores.put(indEntry.getKey(), round6(score));
                    }
                    result.put(dimName, indScores);
                }
            }
        }

        log.info("指标得分计算完成，共 {} 个维度", result.size());
        return result;
    }

    /**
     * 计算效能综合分
     * 综合分 = Σ(维度权重 × Σ(指标局部权重 × score))
     */
    @SuppressWarnings("unchecked")
    private double calculateEffectivenessScore(Map<String, Object> unified, Map<String, Double> scores) {
        if (scores == null || scores.isEmpty()) {
            log.warn("效能得分为空");
            return 0.0;
        }

        Map<String, Object> effectiveness = (Map<String, Object>) unified.get("effectiveness");
        if (effectiveness == null) {
            log.warn("unified 中没有 effectiveness 数据");
            return 0.0;
        }

        Map<String, Double> dimWeights = (Map<String, Double>) effectiveness.get("dimensionWeights");
        Map<String, Map<String, Double>> indicators = (Map<String, Map<String, Double>>) effectiveness.get("indicators");

        if (dimWeights == null || indicators == null) {
            log.warn("效能维度权重或指标权重为空: dimWeights={}, indicators={}", dimWeights, indicators);
            return 0.0;
        }

        log.debug("计算效能综合分 - 维度权重: {}", dimWeights);
        log.debug("计算效能综合分 - 得分: {}", scores);

        double totalScore = 0.0;
        for (Map.Entry<String, Map<String, Double>> dimEntry : indicators.entrySet()) {
            String dimName = dimEntry.getKey();
            double wDim = dimWeights.getOrDefault(dimName, 0.0);
            if (wDim == 0.0) continue;

            Map<String, Double> indWeights = dimEntry.getValue();
            if (indWeights == null || indWeights.isEmpty()) continue;

            // 计算该维度的得分（指标局部权重 × score 的加权平均）
            double dimScore = 0.0;
            for (Map.Entry<String, Double> indEntry : indWeights.entrySet()) {
                String indicatorName = indEntry.getKey();
                double localWeight = indEntry.getValue();
                double score = findScore(scores, indicatorName);
                dimScore += localWeight * score;
            }

            // 乘以维度权重
            totalScore += wDim * dimScore;
            log.debug("效能维度 {}: wDim={}, dimScore={}, contribution={}", dimName, wDim, dimScore, wDim * dimScore);
        }

        log.info("计算效能综合分完成: totalScore={}", totalScore);
        return totalScore;
    }

    /**
     * 计算装备综合分 (定量归一化得分 + 定性质心分)
     * 综合分 = Σ(维度权重 × Σ(指标局部权重 × score))
     */
    @SuppressWarnings("unchecked")
    private double calculateEquipmentScore(Map<String, Object> unified,
                                          Map<String, Double> qtScores,
                                          Map<String, Double> qlScores) {

        Map<String, Object> equipment = (Map<String, Object>) unified.get("equipment");
        if (equipment == null) {
            log.warn("unified 中没有 equipment 数据");
            return 0.0;
        }

        Map<String, Double> dimWeights = (Map<String, Double>) equipment.get("dimensionWeights");
        Map<String, Map<String, Double>> indicators = (Map<String, Map<String, Double>>) equipment.get("indicators");

        if (dimWeights == null || indicators == null) {
            log.warn("装备维度权重或指标权重为空: dimWeights={}, indicators={}", dimWeights, indicators);
            return 0.0;
        }

        log.debug("计算装备综合分 - 维度权重: {}", dimWeights);
        log.debug("计算装备综合分 - 定量得分: {}", qtScores);
        log.debug("计算装备综合分 - 定性得分: {}", qlScores);

        double totalScore = 0.0;
        for (Map.Entry<String, Map<String, Double>> dimEntry : indicators.entrySet()) {
            String dimName = dimEntry.getKey();
            double wDim = dimWeights.getOrDefault(dimName, 0.0);
            if (wDim == 0.0) continue;

            Map<String, Double> indWeights = dimEntry.getValue();
            if (indWeights == null || indWeights.isEmpty()) continue;

            // 计算该维度的得分
            double dimScore = 0.0;
            for (Map.Entry<String, Double> indEntry : indWeights.entrySet()) {
                String indicatorName = indEntry.getKey();
                double localWeight = indEntry.getValue();

                // 优先定量，其次定性 (x_star 质心)
                double score = 0.0;
                if (qtScores != null && !qtScores.isEmpty()) {
                    score = findScore(qtScores, indicatorName);
                }
                if (score == 0.0 && qlScores != null && !qlScores.isEmpty()) {
                    score = findScore(qlScores, indicatorName);
                }

                dimScore += localWeight * score;
                log.debug("装备指标 {}: score={}, localWeight={}, contribution={}",
                        indicatorName, score, localWeight, localWeight * score);
            }

            // 乘以维度权重
            totalScore += wDim * dimScore;
            log.debug("装备维度 {}: wDim={}, dimScore={}, contribution={}", dimName, wDim, dimScore, wDim * dimScore);
        }

        log.info("计算装备综合分完成: totalScore={}", totalScore);
        return totalScore;
    }

    /**
     * 根据指标名称匹配得分
     * 支持中文指标名称映射到英文键名
     */
    private double findScore(Map<String, Double> scores, String indicatorName) {
        if (scores == null || scores.isEmpty()) {
            log.debug("findScore: scores 为空或 null");
            return 0.0;
        }
        if (indicatorName == null) {
            log.debug("findScore: indicatorName 为 null");
            return 0.0;
        }

        log.debug("findScore: 尝试匹配 indicatorName='{}', 可用keys={}", indicatorName, scores.keySet());

        // 1. 直接匹配
        if (scores.containsKey(indicatorName)) {
            log.debug("findScore: 直接匹配成功 '{}' -> {}", indicatorName, scores.get(indicatorName));
            return scores.get(indicatorName);
        }

        // 2. 移除 "_得分" 后缀匹配
        String normalized = indicatorName.replace("_得分", "").replace("得分", "");
        for (Map.Entry<String, Double> e : scores.entrySet()) {
            String key = e.getKey().replace("_得分", "").replace("得分", "");
            if (key.equalsIgnoreCase(normalized)) {
                log.debug("findScore: 移除后缀匹配 '{}' -> {} (原始key='{}')", indicatorName, e.getValue(), e.getKey());
                return e.getValue();
            }
        }

        // 3. 指标名称映射匹配（中文 -> 英文）
        String[] mappedKeys = EQUIPMENT_INDICATOR_MAPPING.get(indicatorName);
        if (mappedKeys != null) {
            for (String mappedKey : mappedKeys) {
                if (scores.containsKey(mappedKey)) {
                    log.debug("findScore: 映射匹配 '{}' -> {} (mappedKey='{}')", indicatorName, scores.get(mappedKey), mappedKey);
                    return scores.get(mappedKey);
                }
            }
        }

        // 4. 部分匹配
        for (Map.Entry<String, Double> e : scores.entrySet()) {
            String key = e.getKey().toLowerCase().replace("_", "").replace("得分", "");
            String target = normalized.toLowerCase().replace("_", "");
            if (key.contains(target) || target.contains(key)) {
                log.debug("findScore: 部分匹配 '{}' -> {} (原始key='{}')", indicatorName, e.getValue(), e.getKey());
                return e.getValue();
            }
        }

        // 5. 反向匹配（得分 key 包含指标名）
        for (Map.Entry<String, Double> e : scores.entrySet()) {
            String key = e.getKey().toLowerCase();
            String target = indicatorName.toLowerCase().replace("_得分", "");
            if (key.contains(target)) {
                log.debug("findScore: 反向匹配 '{}' -> {} (原始key='{}')", indicatorName, e.getValue(), e.getKey());
                return e.getValue();
            }
        }

        log.debug("findScore: 未找到匹配 '{}'", indicatorName);
        return 0.0;
    }

    /**
     * 计算维度得分（使用局部权重加权平均，得分范围0~1）
     * 维度得分 = Σ(指标局部权重 × score)
     */
    @SuppressWarnings("unchecked")
    private Map<String, Double> calculateDimensionScores(
            Map<String, Object> unified,
            Map<String, Double> effScores,
            Map<String, Double> eqScores) {

        Map<String, Double> result = new LinkedHashMap<>();

        // 效能维度
        Map<String, Object> effectiveness = (Map<String, Object>) unified.get("effectiveness");
        if (effectiveness != null) {
            Map<String, Double> dimWeights = (Map<String, Double>) effectiveness.get("dimensionWeights");
            Map<String, Map<String, Double>> indicators = (Map<String, Map<String, Double>>) effectiveness.get("indicators");

            if (dimWeights != null && indicators != null) {
                for (Map.Entry<String, Double> dimEntry : dimWeights.entrySet()) {
                    String dimName = dimEntry.getKey();
                    Map<String, Double> indWeights = indicators.get(dimName);

                    if (indWeights != null && !indWeights.isEmpty()) {
                        double dimScore = 0.0;
                        double matchedCount = 0;
                        for (Map.Entry<String, Double> indEntry : indWeights.entrySet()) {
                            double localWeight = indEntry.getValue();
                            double score = findScore(effScores, indEntry.getKey());
                            dimScore += localWeight * score;
                            if (score > 0) matchedCount++;
                        }
                        log.debug("效能维度 {}: matched={}/{}, dimScore={}", dimName, (int)matchedCount, indWeights.size(), dimScore);
                        result.put("效能_" + dimName, round6(Math.max(0, Math.min(1, dimScore))));
                    }
                }
            }
        }

        // 装备维度
        Map<String, Object> equipment = (Map<String, Object>) unified.get("equipment");
        if (equipment != null) {
            Map<String, Double> dimWeights = (Map<String, Double>) equipment.get("dimensionWeights");
            Map<String, Map<String, Double>> indicators = (Map<String, Map<String, Double>>) equipment.get("indicators");

            if (dimWeights != null && indicators != null) {
                for (Map.Entry<String, Double> dimEntry : dimWeights.entrySet()) {
                    String dimName = dimEntry.getKey();
                    Map<String, Double> indWeights = indicators.get(dimName);

                    if (indWeights != null && !indWeights.isEmpty()) {
                        double dimScore = 0.0;
                        double matchedCount = 0;
                        for (Map.Entry<String, Double> indEntry : indWeights.entrySet()) {
                            double localWeight = indEntry.getValue();
                            double score = findScore(eqScores, indEntry.getKey());
                            dimScore += localWeight * score;
                            if (score > 0) matchedCount++;
                        }
                        log.debug("装备维度 {}: matched={}/{}, dimScore={}", dimName, (int)matchedCount, indWeights.size(), dimScore);
                        result.put("装备_" + dimName, round6(Math.max(0, Math.min(1, dimScore))));
                    }
                }
            }
        }

        return result;
    }

    /**
     * 保存评分结果
     */
    private void saveScoringResults(String evaluationBatchId, List<Map<String, Object>> operationResults,
                                    ExpertAhpGroupWeights gw, double wEff, double wEq) {
        for (Map<String, Object> op : operationResults) {
            String opId = String.valueOf(op.get("operationId"));

            AhpCommunicationScoringResult entity =
                    scoringResultRepository.findByEvaluationBatchIdAndOperationId(evaluationBatchId, opId)
                            .orElse(new AhpCommunicationScoringResult());

            entity.setEvaluationBatchId(evaluationBatchId);
            entity.setOperationId(opId);
            entity.setEffDomainWeight(BigDecimal.valueOf(wEff).setScale(6, RoundingMode.HALF_UP));
            entity.setEqDomainWeight(BigDecimal.valueOf(wEq).setScale(6, RoundingMode.HALF_UP));
            entity.setEffectivenessScore(toBd(op.get("effectivenessScore")));
            entity.setEquipmentScore(toBd(op.get("equipmentScore")));
            entity.setTotalScore(toBd(op.get("totalScore")));
            entity.setDimensionScoresJson(toJson(op.get("dimensionScores")));
            entity.setIndicatorScoresJson(toJson(op.get("indicatorScores")));
            entity.setExpertInfoJson(gw.getExpertWeightsJson());
            // 保存维度权重快照
            entity.setWeightSnapshotJson(toJson(op.get("dimensionWeights")));

            scoringResultRepository.save(entity);
        }
    }

    /**
     * 获取已保存的评分结果
     */
    public Map<String, Object> getSavedResults(String evaluationBatchId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("evaluationBatchId", evaluationBatchId);

        try {
            List<AhpCommunicationScoringResult> saved =
                    scoringResultRepository.findByEvaluationBatchIdOrderByOperationIdAsc(evaluationBatchId);

            if (saved.isEmpty()) {
                result.put("success", false);
                result.put("message", "未找到该批次的综合评分结果");
                return result;
            }

            List<Map<String, Object>> operationResults = new ArrayList<>();
            for (AhpCommunicationScoringResult r : saved) {
                Map<String, Object> op = new LinkedHashMap<>();
                op.put("operationId", r.getOperationId());
                op.put("effectivenessScore", r.getEffectivenessScore());
                op.put("equipmentScore", r.getEquipmentScore());
                op.put("totalScore", r.getTotalScore());
                op.put("dimensionScores", parseJsonToMap(r.getDimensionScoresJson()));
                op.put("indicatorScores", parseJsonToMap(r.getIndicatorScoresJson()));
                operationResults.add(op);
            }

            result.put("success", true);
            result.put("results", operationResults);

            AhpCommunicationScoringResult first = saved.get(0);
            // 构建权重快照（包括域间权重）
            Map<String, Object> weightSnapshot = new LinkedHashMap<>();
            weightSnapshot.put("effDomainWeight", first.getEffDomainWeight());
            weightSnapshot.put("eqDomainWeight", first.getEqDomainWeight());
            result.put("weightSnapshot", weightSnapshot);

            // 从 weightSnapshotJson 解析维度权重
            Map<String, Object> dimWeights = parseJsonToMap(first.getWeightSnapshotJson());
            if (dimWeights != null) {
                result.put("dimensionWeights", dimWeights);
            }

        } catch (Exception e) {
            log.error("获取已保存结果失败", e);
            result.put("success", false);
            result.put("message", "获取结果失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 删除评分结果
     */
    @Transactional
    public void deleteResults(String evaluationBatchId) {
        scoringResultRepository.deleteByEvaluationBatchId(evaluationBatchId);
    }

    /**
     * 获取批次列表
     */
    public List<String> getEvaluationBatches() {
        try {
            // 优先从 score_military_comm_effect 获取 (使用 evaluation_batch_id)
            List<String> fromScore = jdbcTemplate.queryForList(
                    "SELECT DISTINCT evaluation_batch_id FROM score_military_comm_effect ORDER BY evaluation_batch_id DESC",
                    String.class);
            if (!fromScore.isEmpty()) {
                return fromScore;
            }
            return scoringResultRepository.findAll().stream()
                    .map(AhpCommunicationScoringResult::getEvaluationBatchId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        } catch (Exception e) {
            log.warn("获取批次列表失败", e);
            return new ArrayList<>();
        }
    }

    // ==================== 辅助方法 ====================

    private Map<String, Object> parseJsonToMap(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("解析JSON失败: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("序列化JSON失败", e);
            return null;
        }
    }

    private BigDecimal toBd(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
        try {
            return new BigDecimal(val.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private double round6(double v) {
        if (!Double.isFinite(v)) return 0.0;
        return BigDecimal.valueOf(v).setScale(6, RoundingMode.HALF_UP).doubleValue();
    }

    // ==================== 数据查询接口 ====================

    /**
     * 获取效能指标原始数据（metrics_military_comm_effect）
     */
    public Map<String, Object> getMetricsRaw(String evaluationBatchId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("evaluationBatchId", evaluationBatchId);

        List<String> fieldNames = Arrays.asList(
                "security_key_leakage_count", "security_detected_probability",
                "security_interception_resistance_probability", "reliability_crash_count",
                "reliability_recovery_time", "reliability_communication_availability",
                "transmission_bandwidth", "transmission_call_setup_time", "transmission_delay",
                "transmission_bit_error_rate", "transmission_throughput", "transmission_spectral_efficiency",
                "anti_jamming_sinr", "anti_jamming_margin", "anti_jamming_communication_distance",
                "effect_damage_rate", "effect_mission_completion_rate", "effect_blind_rate"
        );

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT operation_id, " + String.join(", ", fieldNames) +
                            " FROM metrics_military_comm_effect WHERE evaluation_batch_id = ? ORDER BY operation_id",
                    evaluationBatchId);
            result.put("success", true);
            result.put("data", rows);
            result.put("fields", fieldNames);
        } catch (Exception e) {
            log.error("获取效能原始数据失败", e);
            result.put("success", false);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取效能指标归一化得分（score_military_comm_effect）
     * 使用 evaluation_batch_id 关联
     */
    public Map<String, Object> getMetricsScore(String evaluationBatchId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("evaluationBatchId", evaluationBatchId);

        List<String> scoreFields = Arrays.asList(
                "security_key_leakage_qt", "security_detected_probability_qt", "security_interception_resistance_ql",
                "reliability_crash_rate_qt", "reliability_recovery_capability_qt", "reliability_communication_availability_qt",
                "transmission_bandwidth_qt", "transmission_call_setup_time_qt", "transmission_transmission_delay_qt",
                "transmission_bit_error_rate_qt", "transmission_throughput_qt", "transmission_spectral_efficiency_qt",
                "anti_jamming_sinr_qt", "anti_jamming_anti_jamming_margin_qt", "anti_jamming_communication_distance_qt",
                "effect_damage_rate_qt", "effect_mission_completion_rate_qt", "effect_blind_rate_qt"
        );

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT operation_id, " + String.join(", ", scoreFields) +
                            " FROM score_military_comm_effect WHERE evaluation_batch_id = ? ORDER BY operation_id",
                    evaluationBatchId);
            result.put("success", true);
            result.put("data", rows);
            result.put("fields", scoreFields);
        } catch (Exception e) {
            log.error("获取效能归一化得分失败", e);
            result.put("success", false);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取装备操作原始数据（equipment_qt_evaluation_record.raw_data）
     */
    public Map<String, Object> getEquipmentRaw(String evaluationBatchId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("evaluationBatchId", evaluationBatchId);

        try {
            List<EquipmentQtEvaluationRecord> records =
                    qtRecordRepository.findByEvaluationBatchIdOrderByOperationId(evaluationBatchId);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (EquipmentQtEvaluationRecord rec : records) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("operation_id", rec.getOperationId());
                row.put("raw_data", parseJsonToMap(rec.getRawData()));
                rows.add(row);
            }
            result.put("success", true);
            result.put("data", rows);
        } catch (Exception e) {
            log.error("获取装备原始数据失败", e);
            result.put("success", false);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取装备操作归一化得分（equipment_qt_evaluation_record.normalized_scores）
     */
    public Map<String, Object> getEquipmentScore(String evaluationBatchId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("evaluationBatchId", evaluationBatchId);

        try {
            List<EquipmentQtEvaluationRecord> records =
                    qtRecordRepository.findByEvaluationBatchIdOrderByOperationId(evaluationBatchId);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (EquipmentQtEvaluationRecord rec : records) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("operation_id", rec.getOperationId());
                row.put("normalized_scores", parseJsonToMap(rec.getNormalizedScores()));
                row.put("composite_score", rec.getCompositeScore());
                rows.add(row);
            }
            result.put("success", true);
            result.put("data", rows);
        } catch (Exception e) {
            log.error("获取装备归一化得分失败", e);
            result.put("success", false);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取装备定性集结结果（equipment_ql_aggregation_result.x_star 质心）
     */
    public Map<String, Object> getEquipmentQlAggregation(String evaluationBatchId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("evaluationBatchId", evaluationBatchId);

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT operation_id, indicator_key, indicator_name, x_star, mapped_grade " +
                            "FROM equipment_ql_aggregation_result WHERE evaluation_batch_id = ? ORDER BY operation_id, indicator_key",
                    evaluationBatchId);
            result.put("success", true);
            result.put("data", rows);
        } catch (Exception e) {
            log.error("获取装备定性集结结果失败", e);
            result.put("success", false);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取所有批次信息（用于调试批次不统一问题）
     */
    public Map<String, Object> getBatchInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        try {
            // 效能 score 表批次 (evaluation_batch_id)
            List<String> effBatches = jdbcTemplate.queryForList(
                    "SELECT DISTINCT evaluation_batch_id FROM score_military_comm_effect ORDER BY evaluation_batch_id DESC LIMIT 20",
                    String.class);
            info.put("effectivenessScoreBatches", effBatches);

            // 装备 QT 批次
            List<String> eqQtBatches = qtRecordRepository.findAll().stream()
                    .map(EquipmentQtEvaluationRecord::getEvaluationBatchId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted(Comparator.reverseOrder())
                    .limit(20)
                    .toList();
            info.put("equipmentQtBatches", eqQtBatches);

            // 装备 QL 批次
            List<String> eqQlBatches = jdbcTemplate.queryForList(
                    "SELECT DISTINCT evaluation_batch_id FROM equipment_ql_aggregation_result ORDER BY evaluation_batch_id DESC LIMIT 20",
                    String.class);
            info.put("equipmentQlBatches", eqQlBatches);

            // 各表记录数
            Long effCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM score_military_comm_effect", Long.class);
            Long eqQtCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM equipment_qt_evaluation_record", Long.class);
            Long eqQlCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM equipment_ql_aggregation_result", Long.class);

            info.put("effectivenessScoreCount", effCount);
            info.put("equipmentQtCount", eqQtCount);
            info.put("equipmentQlCount", eqQlCount);

            info.put("success", true);
        } catch (Exception e) {
            log.error("获取批次信息失败", e);
            info.put("success", false);
            info.put("message", "获取失败: " + e.getMessage());
        }
        return info;
    }

    /**
     * 获取该批次下的所有作战ID
     */
    public Map<String, Object> getOperationsByBatch(String evaluationBatchId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("evaluationBatchId", evaluationBatchId);

        try {
            // 从效能表获取作战ID (evaluation_batch_id)
            List<String> effOps = jdbcTemplate.queryForList(
                    "SELECT DISTINCT operation_id FROM score_military_comm_effect WHERE evaluation_batch_id = ? ORDER BY operation_id",
                    String.class, evaluationBatchId);
            // 从装备QT表获取作战ID
            List<String> eqOps = qtRecordRepository.findByEvaluationBatchIdOrderByOperationId(evaluationBatchId)
                    .stream()
                    .map(EquipmentQtEvaluationRecord::getOperationId)
                    .filter(Objects::nonNull)
                    .toList();
            // 从装备QL表获取作战ID
            List<String> qlOps = jdbcTemplate.queryForList(
                    "SELECT DISTINCT operation_id FROM equipment_ql_aggregation_result WHERE evaluation_batch_id = ? ORDER BY operation_id",
                    String.class, evaluationBatchId);

            result.put("effectivenessOperations", effOps);
            result.put("equipmentQtOperations", eqOps);
            result.put("equipmentQlOperations", qlOps);
            result.put("success", true);
        } catch (Exception e) {
            log.error("获取作战ID失败", e);
            result.put("success", false);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取指定作战的完整评估数据（统一视图）
     */
    public Map<String, Object> getOperationData(String evaluationBatchId, String operationId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("evaluationBatchId", evaluationBatchId);
        result.put("operationId", operationId);

        try {
            // 1. 效能原始数据
            List<Map<String, Object>> effRaw = jdbcTemplate.queryForList(
                    "SELECT * FROM metrics_military_comm_effect WHERE evaluation_batch_id = ? AND operation_id = ?",
                    evaluationBatchId, operationId);
            result.put("effectivenessRaw", effRaw.isEmpty() ? null : effRaw.get(0));

            // 2. 效能归一化得分 (evaluation_batch_id)
            List<Map<String, Object>> effScore = jdbcTemplate.queryForList(
                    "SELECT * FROM score_military_comm_effect WHERE evaluation_batch_id = ? AND operation_id = ?",
                    evaluationBatchId, operationId);
            result.put("effectivenessScore", effScore.isEmpty() ? null : effScore.get(0));

            // 3. 装备原始数据
            EquipmentQtEvaluationRecord eqRecord = qtRecordRepository
                    .findByEvaluationBatchIdAndOperationId(evaluationBatchId, operationId)
                    .orElse(null);
            result.put("equipmentRaw", eqRecord != null ? parseJsonToMap(eqRecord.getRawData()) : null);

            // 4. 装备归一化得分
            result.put("equipmentScore", eqRecord != null ? parseJsonToMap(eqRecord.getNormalizedScores()) : null);
            result.put("equipmentCompositeScore", eqRecord != null ? eqRecord.getCompositeScore() : null);

            // 5. 装备定性数据 (x_star 质心)
            List<Map<String, Object>> eqQl = jdbcTemplate.queryForList(
                    "SELECT * FROM equipment_ql_aggregation_result WHERE evaluation_batch_id = ? AND operation_id = ?",
                    evaluationBatchId, operationId);
            result.put("equipmentQualitative", eqQl);

            result.put("success", true);
        } catch (Exception e) {
            log.error("获取作战数据失败", e);
            result.put("success", false);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取等级定义数据
     */
    public Map<String, Object> getGradeDefinitions() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> grades = jdbcTemplate.queryForList(
                    "SELECT grade_code, grade_name, category, min_score, max_score, grade_level " +
                            "FROM evaluation_grade_definition ORDER BY grade_level");
            result.put("success", true);
            result.put("data", grades);
        } catch (Exception e) {
            log.error("获取等级定义失败", e);
            result.put("success", false);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }
}
