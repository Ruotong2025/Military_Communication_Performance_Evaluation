package com.ccnu.military.service;

import com.ccnu.military.dto.MetricsCalculationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 军事通信效能指标计算服务
 */
@Slf4j
@Service
public class MetricsCalculationService {

    private static final String SCORE_TABLE = "score_military_comm_effect";

    private final JdbcTemplate jdbcTemplate;

    public MetricsCalculationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 计算指标数据
     * @param request 包含要计算的作战ID列表
     * @return 计算结果
     */
    public Map<String, Object> calculateMetrics(MetricsCalculationRequest request) {
        try {
            // 确定要计算的作战ID
            List<Integer> targetIds;
            if (Boolean.TRUE.equals(request.getSpecificOnly()) && request.getOperationIds() != null && !request.getOperationIds().isEmpty()) {
                targetIds = request.getOperationIds();
                log.info("计算指定作战ID: {}", targetIds);
            } else {
                // 获取所有作战ID
                targetIds = jdbcTemplate.queryForList(
                        "SELECT DISTINCT operation_id FROM records_military_operation_info ORDER BY operation_id",
                        Integer.class
                );
                log.info("计算全部作战ID: {}", targetIds);
            }

            if (targetIds.isEmpty()) {
                return Map.of(
                        "success", true,
                        "message", "没有找到作战数据",
                        "count", 0,
                        "data", Collections.emptyList()
                );
            }

            String evaluationBatchId = newEvaluationBatchId();
            int insertedCount = 0;
            List<Map<String, Object>> resultList = new ArrayList<>();

            for (Integer operationId : targetIds) {
                Map<String, Object> metrics = calculateMetricsForOperation(evaluationBatchId, operationId);
                if (metrics != null && !metrics.isEmpty()) {
                    resultList.add(metrics);
                    insertedCount++;
                }
            }

            // 注意：归一化 score 计算已拆分为独立接口 POST /metrics/generate-score
            log.info("评估批次 {} 成功计算并插入 {} 条指标数据", evaluationBatchId, insertedCount);

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("success", true);
            out.put("message", String.format("成功计算 %d 条指标数据（批次 %s）", insertedCount, evaluationBatchId));
            out.put("count", insertedCount);
            out.put("data", resultList);
            out.put("evaluationBatchId", evaluationBatchId);
            return out;

        } catch (Exception e) {
            log.error("指标计算失败", e);
            return Map.of(
                    "success", false,
                    "message", "指标计算失败: " + e.getMessage()
            );
        }
    }

    /**
     * 生成新的评估批次 ID（每次点击「计算指标」唯一；同一次计算内各作战共用同一 ID）
     * 时间戳到毫秒 + 随机后缀，避免同一秒内多次计算冲突、且便于按时间排序辨认。
     */
    private static String newEvaluationBatchId() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        return "EVAL-BATCH-" + ts + "-" + suffix;
    }

    /**
     * 根据评估批次与作战ID计算各项指标
     */
    private Map<String, Object> calculateMetricsForOperation(String evaluationBatchId, Integer operationId) {
        try {
            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("operationId", operationId);
            metrics.put("evaluationId", evaluationBatchId);

            // ========== 安全性指标 ==========
            // 1. security_key_leakage_count: 密钥泄露次数
            Integer keyLeakCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM records_security_events WHERE operation_id = ? AND event_type = 'key_leak'",
                    Integer.class, operationId
            );
            metrics.put("securityKeyLeakageCount", keyLeakCount != null ? keyLeakCount : 0);

            // 2. security_detected_probability: 被侦察概率
            Double detectedProb = jdbcTemplate.queryForObject(
                    "SELECT CASE WHEN COUNT(*) = 0 THEN 0 " +
                            "ELSE (SUM(CASE WHEN detected = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) END " +
                            "FROM records_military_communication_info WHERE operation_id = ?",
                    Double.class, operationId
            );
            metrics.put("securityDetectedProbability", round(detectedProb, 4));

            // 3. security_interception_resistance_probability: 抗拦截能力 = intercepted=1 条数 ÷ 通信总条数 × 100（值越低越安全）
            Integer totalComm = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM records_military_communication_info WHERE operation_id = ?",
                    Integer.class, operationId
            );
            Integer interceptedCommCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM records_military_communication_info WHERE operation_id = ? AND intercepted = 1",
                    Integer.class, operationId
            );
            double interceptionResistance = 0.0;
            if (totalComm != null && totalComm > 0 && interceptedCommCount != null) {
                interceptionResistance = interceptedCommCount * 100.0 / totalComm;
            }
            metrics.put("securityInterceptionResistanceProbability", round(interceptionResistance, 4));

            // ========== 可靠性指标 ==========
            // 4. reliability_crash_count: 网络崩溃次数（值越低越好）
            Integer crashCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM records_link_maintenance_events WHERE operation_id = ? AND interruption_type = 'crash'",
                    Integer.class, operationId
            );
            metrics.put("reliabilityCrashCount", crashCount != null ? crashCount : 0);

            // 5. reliability_recovery_time: 平均恢复时间（毫秒）
            Double avgRecoveryTime = jdbcTemplate.queryForObject(
                    "SELECT AVG(recovery_duration_ms) FROM records_link_maintenance_events " +
                            "WHERE operation_id = ? AND recovery_success = 1 AND recovery_duration_ms IS NOT NULL",
                    Double.class, operationId
            );
            metrics.put("reliabilityRecoveryTime", round(avgRecoveryTime, 2));

            // 6. reliability_communication_availability: 通信可用性
            Long totalDuration = jdbcTemplate.queryForObject(
                    "SELECT MAX(end_time_ms) - MIN(start_time_ms) FROM records_military_communication_info WHERE operation_id = ?",
                    Long.class, operationId
            );
            Long interruptionDuration = jdbcTemplate.queryForObject(
                    "SELECT SUM(interruption_end_ms - interruption_start_ms) FROM records_link_maintenance_events WHERE operation_id = ?",
                    Long.class, operationId
            );
            double availability = 100.0;
            if (totalDuration != null && totalDuration > 0) {
                availability = (totalDuration - (interruptionDuration != null ? interruptionDuration : 0)) * 100.0 / totalDuration;
            }
            metrics.put("reliabilityCommunicationAvailability", round(availability, 4));

            // ========== 传输指标 ==========
            // 7. transmission_bandwidth: 平均带宽（Mbps）
            Double avgBandwidth = jdbcTemplate.queryForObject(
                    "SELECT AVG(bandwidth_hz) / 1000000 FROM records_military_communication_info " +
                            "WHERE operation_id = ? AND bandwidth_hz IS NOT NULL",
                    Double.class, operationId
            );
            metrics.put("transmissionBandwidth", round(avgBandwidth, 4));

            // 8. transmission_call_setup_time: 平均呼叫建立时间（毫秒）
            Double avgCallSetupTime = jdbcTemplate.queryForObject(
                    "SELECT AVG(call_setup_ms) FROM records_military_communication_info " +
                            "WHERE operation_id = ? AND call_setup_ms IS NOT NULL",
                    Double.class, operationId
            );
            metrics.put("transmissionCallSetupTime", round(avgCallSetupTime, 2));

            // 9. transmission_delay: 平均传输时延（毫秒）
            Double avgDelay = jdbcTemplate.queryForObject(
                    "SELECT AVG(trans_delay_ms) FROM records_military_communication_info " +
                            "WHERE operation_id = ? AND trans_delay_ms IS NOT NULL",
                    Double.class, operationId
            );
            metrics.put("transmissionDelay", round(avgDelay, 4));

            // 10. transmission_bit_error_rate: 平均误码率（%）
            Double avgBer = jdbcTemplate.queryForObject(
                    "SELECT AVG(error_bits * 1.0 / NULLIF(total_bits, 0)) * 100 " +
                            "FROM records_military_communication_info WHERE operation_id = ? AND total_bits > 0",
                    Double.class, operationId
            );
            metrics.put("transmissionBitErrorRate", round(avgBer, 4));

            // 11. transmission_throughput: 平均吞吐量（Mbps）
            Double avgThroughput = jdbcTemplate.queryForObject(
                    "SELECT AVG(throughput_bps) / 1000000 FROM records_military_communication_info " +
                            "WHERE operation_id = ? AND throughput_bps IS NOT NULL",
                    Double.class, operationId
            );
            metrics.put("transmissionThroughput", round(avgThroughput, 4));

            // 12. transmission_spectral_efficiency: 频谱效率（bit/Hz）
            Double avgSpectralEfficiency = jdbcTemplate.queryForObject(
                    "SELECT AVG(throughput_bps / NULLIF(bandwidth_hz, 0)) " +
                            "FROM records_military_communication_info WHERE operation_id = ? AND bandwidth_hz > 0 AND throughput_bps IS NOT NULL",
                    Double.class, operationId
            );
            metrics.put("transmissionSpectralEfficiency", round(avgSpectralEfficiency, 4));

            // ========== 抗干扰指标 ==========
            // 13. anti_jamming_sinr: 平均信干噪比（dB）
            Double avgSinr = jdbcTemplate.queryForObject(
                    "SELECT AVG(sinr_db) FROM records_military_communication_info " +
                            "WHERE operation_id = ? AND sinr_db IS NOT NULL",
                    Double.class, operationId
            );
            metrics.put("antiJammingSinr", round(avgSinr, 2));

            // 14. anti_jamming_margin: 平均抗干扰余量（dB）
            Double avgJammingMargin = jdbcTemplate.queryForObject(
                    "SELECT AVG(jamming_margin_db) FROM records_military_communication_info " +
                            "WHERE operation_id = ? AND jamming_margin_db IS NOT NULL",
                    Double.class, operationId
            );
            metrics.put("antiJammingMargin", round(avgJammingMargin, 2));

            // 15. anti_jamming_communication_distance: 平均通信距离（km）
            Double avgDistance = jdbcTemplate.queryForObject(
                    "SELECT AVG(distance_km) FROM records_military_communication_info " +
                            "WHERE operation_id = ? AND distance_km IS NOT NULL",
                    Double.class, operationId
            );
            metrics.put("antiJammingCommunicationDistance", round(avgDistance, 2));

            // ========== 效能指标 ==========
            // 16. effect_damage_rate: 毁伤率（%）
            Map<String, Object> opInfo = jdbcTemplate.queryForMap(
                    "SELECT total_equipment_count, damaged_equipment_count FROM records_military_operation_info WHERE operation_id = ?",
                    operationId
            );
            Integer totalEquipment = (Integer) opInfo.get("total_equipment_count");
            Integer damagedEquipment = (Integer) opInfo.get("damaged_equipment_count");
            double damageRate = 0.0;
            if (totalEquipment != null && totalEquipment > 0 && damagedEquipment != null) {
                damageRate = damagedEquipment * 100.0 / totalEquipment;
            }
            metrics.put("effectDamageRate", round(damageRate, 4));

            // 17. effect_mission_completion_rate: 任务完成率
            Integer successComm = jdbcTemplate.queryForObject(
                    "SELECT SUM(CASE WHEN comm_success = 1 THEN 1 ELSE 0 END) FROM records_military_communication_info WHERE operation_id = ?",
                    Integer.class, operationId
            );
            double missionCompletionRate = 0.0;
            if (totalComm != null && totalComm > 0 && successComm != null) {
                missionCompletionRate = successComm * 100.0 / totalComm;
            }
            metrics.put("effectMissionCompletionRate", round(missionCompletionRate, 4));

            // 18. effect_blind_rate: 盲区率（%）
            Integer totalNode = jdbcTemplate.queryForObject(
                    "SELECT total_node_count FROM records_military_operation_info WHERE operation_id = ?",
                    Integer.class, operationId
            );
            Integer isolatedNode = jdbcTemplate.queryForObject(
                    "SELECT isolated_node_count FROM records_military_operation_info WHERE operation_id = ?",
                    Integer.class, operationId
            );
            double blindRate = 0.0;
            if (totalNode != null && totalNode > 0 && isolatedNode != null) {
                blindRate = isolatedNode * 100.0 / totalNode;
            }
            metrics.put("effectBlindRate", round(blindRate, 4));

            // 插入数据库
            insertMetrics(metrics);

            return metrics;

        } catch (Exception e) {
            log.error("计算作战 {} 的指标失败", operationId, e);
            return null;
        }
    }

    /**
     * 将指标数据插入数据库
     */
    private void insertMetrics(Map<String, Object> metrics) {
        String sql = "INSERT INTO metrics_military_comm_effect (" +
                "evaluation_id, operation_id, " +
                "security_key_leakage_count, security_detected_probability, security_interception_resistance_probability, " +
                "reliability_crash_count, reliability_recovery_time, reliability_communication_availability, " +
                "transmission_bandwidth, transmission_call_setup_time, transmission_delay, transmission_bit_error_rate, " +
                "transmission_throughput, transmission_spectral_efficiency, " +
                "anti_jamming_sinr, anti_jamming_margin, anti_jamming_communication_distance, " +
                "effect_damage_rate, effect_mission_completion_rate, effect_blind_rate, " +
                "created_at, updated_at" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

        jdbcTemplate.update(sql,
                metrics.get("evaluationId"),
                metrics.get("operationId"),
                metrics.get("securityKeyLeakageCount"),
                metrics.get("securityDetectedProbability"),
                metrics.get("securityInterceptionResistanceProbability"),
                metrics.get("reliabilityCrashCount"),
                metrics.get("reliabilityRecoveryTime"),
                metrics.get("reliabilityCommunicationAvailability"),
                metrics.get("transmissionBandwidth"),
                metrics.get("transmissionCallSetupTime"),
                metrics.get("transmissionDelay"),
                metrics.get("transmissionBitErrorRate"),
                metrics.get("transmissionThroughput"),
                metrics.get("transmissionSpectralEfficiency"),
                metrics.get("antiJammingSinr"),
                metrics.get("antiJammingMargin"),
                metrics.get("antiJammingCommunicationDistance"),
                metrics.get("effectDamageRate"),
                metrics.get("effectMissionCompletionRate"),
                metrics.get("effectBlindRate")
        );
    }

    /**
     * 获取作战ID列表（用于前端选择）
     */
    public List<Map<String, Object>> getAvailableOperations() {
        String sql = "SELECT operation_id, avg_network_setup_time_ms, total_node_count, isolated_node_count, " +
                "command_personnel_count, weather_condition, notes " +
                "FROM records_military_operation_info ORDER BY operation_id DESC";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * 列出所有评估批次（按最近创建时间倒序）
     */
    public List<Map<String, Object>> listEvaluationBatches() {
        String sql = "SELECT evaluation_id AS evaluation_batch_id, COUNT(*) AS row_count, " +
                "MAX(created_at) AS created_at " +
                "FROM metrics_military_comm_effect GROUP BY evaluation_id " +
                "ORDER BY MAX(created_at) DESC";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * 按评估批次获取已计算的指标数据
     */
    public List<Map<String, Object>> getCalculatedMetrics(String evaluationBatchId) {
        if (!StringUtils.hasText(evaluationBatchId)) {
            return Collections.emptyList();
        }
        String sql = "SELECT evaluation_id, operation_id, " +
                "security_key_leakage_count, security_detected_probability, security_interception_resistance_probability, " +
                "reliability_crash_count, reliability_recovery_time, reliability_communication_availability, " +
                "transmission_bandwidth, transmission_call_setup_time, transmission_delay, transmission_bit_error_rate, " +
                "transmission_throughput, transmission_spectral_efficiency, " +
                "anti_jamming_sinr, anti_jamming_margin, anti_jamming_communication_distance, " +
                "effect_damage_rate, effect_mission_completion_rate, effect_blind_rate, " +
                "created_at, updated_at " +
                "FROM metrics_military_comm_effect WHERE evaluation_id = ? ORDER BY operation_id";
        return jdbcTemplate.queryForList(sql, evaluationBatchId.trim());
    }

    /**
     * 删除指定评估批次的全部指标行
     */
    public int deleteEvaluationBatch(String evaluationBatchId) {
        if (!StringUtils.hasText(evaluationBatchId)) {
            return 0;
        }
        jdbcTemplate.update("DELETE FROM " + SCORE_TABLE + " WHERE evaluation_id = ?", evaluationBatchId.trim());
        return jdbcTemplate.update("DELETE FROM metrics_military_comm_effect WHERE evaluation_id = ?", evaluationBatchId.trim());
    }

    // ==================== Score 归一化相关方法 ====================

    /**
     * 根据评估批次生成归一化 score 数据并写入 score 表（返回操作结果）
     */
    public Map<String, Object> generateScore(String evaluationBatchId) {
        if (!StringUtils.hasText(evaluationBatchId)) {
            return Map.of("success", false, "message", "evaluationBatchId 不能为空", "count", 0);
        }
        log.info("开始为评估批次 {} 生成归一化 score 数据", evaluationBatchId);

        try {
            // 1. 先删除该批次的旧 score 数据
            jdbcTemplate.update("DELETE FROM " + SCORE_TABLE + " WHERE evaluation_id = ?", evaluationBatchId.trim());

            // 2. 查询原始指标数据
            List<Map<String, Object>> rawData = getCalculatedMetrics(evaluationBatchId);
            if (rawData == null || rawData.isEmpty()) {
                log.warn("评估批次 {} 无原始指标数据，跳过 score 生成", evaluationBatchId);
                return Map.of("success", false, "message", "该批次无原始指标数据，请先执行「计算指标」", "count", 0);
            }

            // 3. 计算每个指标的 min/max（在同一批次内）
            List<String> allFields = MetricsDirection.getAllFields();
            Map<String, Double> minMap = new HashMap<>();
            Map<String, Double> maxMap = new HashMap<>();
            for (String field : allFields) {
                Double minVal = null;
                Double maxVal = null;
                for (Map<String, Object> row : rawData) {
                    Object val = row.get(field);
                    if (val == null) continue;
                    double d = ((Number) val).doubleValue();
                    if (minVal == null || d < minVal) minVal = d;
                    if (maxVal == null || d > maxVal) maxVal = d;
                }
                minMap.put(field, minVal);
                maxMap.put(field, maxVal);
            }

            // 4. 对每行数据计算 score 并写入
            int count = 0;
            for (Map<String, Object> row : rawData) {
                String evalId = String.valueOf(row.get("evaluation_id"));
                Object opIdRaw = row.get("operation_id");
                // score 表 operation_id 为 varchar
                String opId = String.valueOf(opIdRaw);

                List<Object> scoreValues = new ArrayList<>();
                for (String field : allFields) {
                    Object rawVal = row.get(field);
                    Double val = rawVal != null ? ((Number) rawVal).doubleValue() : null;
                    Double score = MetricsDirection.normalize(val, minMap.get(field), maxMap.get(field), field);
                    scoreValues.add(score);
                }

                insertScore(evalId, opId, scoreValues);
                count++;
            }

            log.info("评估批次 {} 完成 score 生成，共 {} 条记录", evaluationBatchId, count);
            return Map.of(
                    "success", true,
                    "message", String.format("成功生成 %d 条归一化 score 数据（批次 %s）", count, evaluationBatchId),
                    "count", count,
                    "evaluationBatchId", evaluationBatchId
            );
        } catch (Exception e) {
            log.error("生成 score 数据失败", e);
            return Map.of("success", false, "message", "生成 score 数据失败: " + e.getMessage(), "count", 0);
        }
    }

    /**
     * 将 score 数据插入数据库
     * 使用 score_military_comm_effect 表的实际列名
     */
    private void insertScore(String evaluationId, String operationId, List<Object> scoreValues) {
        List<String> scoreFields = MetricsDirection.getScoreFields();

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("INSERT INTO ").append(SCORE_TABLE).append(" (evaluation_id, operation_id, ");
        for (int i = 0; i < scoreFields.size(); i++) {
            sqlBuilder.append(scoreFields.get(i));
            if (i < scoreFields.size() - 1) sqlBuilder.append(", ");
        }
        sqlBuilder.append(", created_at, updated_at) VALUES (?, ?, ");
        for (int i = 0; i < scoreFields.size(); i++) {
            sqlBuilder.append("?");
            if (i < scoreFields.size() - 1) sqlBuilder.append(", ");
        }
        sqlBuilder.append(", NOW(), NOW())");

        List<Object> params = new ArrayList<>();
        params.add(evaluationId);
        params.add(operationId);
        params.addAll(scoreValues);

        jdbcTemplate.update(sqlBuilder.toString(), params.toArray());
    }

    /**
     * 按评估批次获取归一化 score 数据
     */
    public List<Map<String, Object>> getScoreData(String evaluationBatchId) {
        if (!StringUtils.hasText(evaluationBatchId)) {
            return Collections.emptyList();
        }
        List<String> scoreFields = MetricsDirection.getScoreFields();
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT evaluation_id, operation_id, ");
        sqlBuilder.append(String.join(", ", scoreFields));
        sqlBuilder.append(", created_at FROM ").append(SCORE_TABLE);
        sqlBuilder.append(" WHERE evaluation_id = ? ORDER BY operation_id");
        return jdbcTemplate.queryForList(sqlBuilder.toString(), evaluationBatchId.trim());
    }

    /**
     * 获取 ECharts 图表数据（多实验对比）
     */
    public Map<String, Object> getScoreChartData(String evaluationBatchId) {
        List<Map<String, Object>> scoreData = getScoreData(evaluationBatchId);
        if (scoreData.isEmpty()) {
            return Collections.emptyMap();
        }

        // score 表列名列表
        List<String> scoreFields = MetricsDirection.getScoreFields();
        Map<String, String> fieldNames = MetricsDirection.getFieldNameMap();
        Map<String, String> fieldCategories = MetricsDirection.getFieldCategoryMap();
        // score 列名 → 原始字段名的反向映射
        Map<String, String> scoreColToField = MetricsDirection.getFieldToScoreColumnMap();
        Map<String, String> fieldToScoreCol = new HashMap<>();
        for (Map.Entry<String, String> entry : scoreColToField.entrySet()) {
            fieldToScoreCol.put(entry.getValue(), entry.getKey());
        }

        // 构建 indicators 元数据（使用 score 表列名作为 code）
        List<Map<String, String>> indicators = new ArrayList<>();
        for (String sf : scoreFields) {
            String originalField = fieldToScoreCol.getOrDefault(sf, sf);
            Map<String, String> ind = new LinkedHashMap<>();
            ind.put("code", sf);
            ind.put("name", fieldNames.getOrDefault(originalField, sf));
            ind.put("category", fieldCategories.getOrDefault(originalField, ""));
            indicators.add(ind);
        }

        // 构建 operations 列表
        List<Map<String, Object>> operations = new ArrayList<>();
        for (Map<String, Object> row : scoreData) {
            Map<String, Object> op = new LinkedHashMap<>();
            op.put("operationId", row.get("operation_id"));
            op.put("label", "作战 " + row.get("operation_id"));
            operations.add(op);
        }

        // 构建 series（每行一个 series，对应一个作战）
        List<Map<String, Object>> series = new ArrayList<>();
        for (Map<String, Object> row : scoreData) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("operationId", row.get("operation_id"));
            s.put("name", "作战 " + row.get("operation_id"));
            List<Double> data = new ArrayList<>();
            for (String sf : scoreFields) {
                Object val = row.get(sf);
                data.add(val != null ? ((Number) val).doubleValue() : null);
            }
            s.put("data", data);
            series.add(s);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("evaluationId", evaluationBatchId);
        result.put("indicators", indicators);
        result.put("operations", operations);
        result.put("series", series);
        return result;
    }

    private Double round(Double value, int places) {
        if (value == null) return 0.0;
        double multiplier = Math.pow(10, places);
        return Math.round(value * multiplier) / multiplier;
    }
}
