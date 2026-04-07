package com.ccnu.military.service;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.entity.EquipmentQtEvaluationRecord;
import com.ccnu.military.entity.EquipmentQtIndicatorDef;
import com.ccnu.military.repository.EquipmentQtEvaluationRecordRepository;
import com.ccnu.military.repository.EquipmentQtIndicatorDefRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 装备操作定量指标计算引擎
 * 从 equipment_qt_indicator_def 读取配置，动态执行 SQL 计算原始值，归一化后保存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentQtCalculationService {

    private final JdbcTemplate jdbcTemplate;
    private final EquipmentQtIndicatorDefRepository qtDefRepository;
    private final EquipmentQtEvaluationRecordRepository qtRecordRepository;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter BATCH_ID_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /**
     * 为指定作战ID列表执行定量指标计算
     */
    @Transactional
    public Map<String, Object> calculateForOperations(List<Long> operationIds, String batchId) {
        try {
            List<EquipmentQtIndicatorDef> indicators = qtDefRepository.findByEnabledTrueOrderByDisplayOrderAsc();
            if (indicators.isEmpty()) {
                return buildResult(false, "未找到启用的定量指标配置", 0, null, null);
            }

            if (batchId == null || batchId.isBlank()) {
                batchId = newBatchId();
            }

            List<Map<String, Object>> savedRecords = new ArrayList<>();
            int count = 0;

            for (Long opId : operationIds) {
                Map<String, BigDecimal> rawData = new LinkedHashMap<>();
                for (EquipmentQtIndicatorDef ind : indicators) {
                    BigDecimal value = calculateSingleIndicator(ind, opId);
                    rawData.put(ind.getIndicatorKey(), value);
                }

                // 计算归一化（归一化稍后统一做，此处先存原始值）
                EquipmentQtEvaluationRecord record = new EquipmentQtEvaluationRecord();
                record.setOperationId(String.valueOf(opId));
                record.setEvaluationTime(LocalDateTime.now());
                record.setEvaluationBatchId(batchId);
                record.setRawData(toJson(rawData));
                record.setNormalizedScores(toJson(new LinkedHashMap<String, BigDecimal>()));
                record.setCompositeScore(null);

                qtRecordRepository.save(record);
                savedRecords.add(convertRecordToMap(record, indicators));
                count++;
            }

            return buildResult(true, String.format("成功计算 %d 条作战的定量指标（批次 %s）", count, batchId), count, batchId, savedRecords);

        } catch (Exception e) {
            log.error("定量指标计算失败", e);
            return buildResult(false, "定量指标计算失败: " + e.getMessage(), 0, null, null);
        }
    }

    /**
     * 为指定批次生成归一化得分
     */
    @Transactional
    public Map<String, Object> generateNormalizedScores(String batchId) {
        try {
            List<EquipmentQtEvaluationRecord> records = qtRecordRepository.findByEvaluationBatchIdOrderByOperationId(batchId);
            if (records.isEmpty()) {
                return buildResult(false, "该批次无定量评估记录", 0, batchId, null);
            }

            List<EquipmentQtIndicatorDef> indicators = qtDefRepository.findByEnabledTrueOrderByDisplayOrderAsc();
            Map<String, List<Double>> allValues = new HashMap<>();
            for (EquipmentQtIndicatorDef ind : indicators) {
                allValues.put(ind.getIndicatorKey(), new ArrayList<>());
            }

            // 收集所有值（raw_data JSON 中数字可能是 Integer/Double，需统一为 BigDecimal）
            for (EquipmentQtEvaluationRecord rec : records) {
                Map<String, BigDecimal> raw = parseRawDataJson(rec.getRawData());
                for (EquipmentQtIndicatorDef ind : indicators) {
                    BigDecimal val = raw.get(ind.getIndicatorKey());
                    if (val != null) {
                        allValues.get(ind.getIndicatorKey()).add(val.doubleValue());
                    }
                }
            }

            // 计算每指标的 min/max
            Map<String, Double> minMap = new HashMap<>();
            Map<String, Double> maxMap = new HashMap<>();
            for (EquipmentQtIndicatorDef ind : indicators) {
                List<Double> vals = allValues.get(ind.getIndicatorKey());
                if (vals == null || vals.isEmpty()) continue;
                double min = vals.stream().min(Double::compare).orElse(0.0);
                double max = vals.stream().max(Double::compare).orElse(0.0);
                minMap.put(ind.getIndicatorKey(), min);
                maxMap.put(ind.getIndicatorKey(), max);
            }

            int updated = 0;
            for (EquipmentQtEvaluationRecord rec : records) {
                Map<String, BigDecimal> raw = parseRawDataJson(rec.getRawData());
                Map<String, BigDecimal> normalized = new LinkedHashMap<>();
                double compositeSum = 0;
                int compositeCount = 0;

                for (EquipmentQtIndicatorDef ind : indicators) {
                    BigDecimal val = raw.get(ind.getIndicatorKey());
                    if (val == null) {
                        normalized.put(ind.getIndicatorKey(), BigDecimal.ZERO);
                        continue;
                    }

                    double min = minMap.getOrDefault(ind.getIndicatorKey(), 0.0);
                    double max = maxMap.getOrDefault(ind.getIndicatorKey(), 0.0);
                    double normScore;

                    if (Math.abs(max - min) < 1e-10) {
                        normScore = 1.0;
                    } else {
                        if (ind.getScoreDirection() == EquipmentQtIndicatorDef.ScoreDirection.negative) {
                            // 越小越好：翻转后归一化
                            normScore = (max - val.doubleValue()) / (max - min);
                        } else {
                            normScore = (val.doubleValue() - min) / (max - min);
                        }
                    }

                    normScore = Math.max(0, Math.min(1, normScore));
                    normalized.put(ind.getIndicatorKey(), BigDecimal.valueOf(normScore).setScale(4, RoundingMode.HALF_UP));
                    compositeSum += normScore;
                    compositeCount++;
                }

                double composite = compositeCount > 0 ? compositeSum / compositeCount : 0;
                rec.setNormalizedScores(toJson(normalized));
                rec.setCompositeScore(BigDecimal.valueOf(composite).setScale(4, RoundingMode.HALF_UP));
                qtRecordRepository.save(rec);
                updated++;
            }

            return buildResult(true, String.format("成功生成 %d 条归一化得分", updated), updated, batchId, null);

        } catch (Exception e) {
            log.error("生成归一化得分失败", e);
            return buildResult(false, "生成归一化得分失败: " + e.getMessage(), 0, batchId, null);
        }
    }

    /**
     * 获取所有定量指标配置
     */
    public List<Map<String, Object>> getIndicatorDefs() {
        return qtDefRepository.findByEnabledTrueOrderByDisplayOrderAsc().stream()
                .map(this::convertDefToMap)
                .toList();
    }

    /**
     * 获取评估批次列表
     */
    public List<Map<String, Object>> listBatches() {
        List<EquipmentQtEvaluationRecord> records = qtRecordRepository.findAll();
        Map<String, List<EquipmentQtEvaluationRecord>> grouped = new LinkedHashMap<>();
        for (EquipmentQtEvaluationRecord r : records) {
            grouped.computeIfAbsent(r.getEvaluationBatchId(), k -> new ArrayList<>()).add(r);
        }

        List<Map<String, Object>> batches = new ArrayList<>();
        grouped.forEach((batchId, recs) -> {
            Map<String, Object> batch = new LinkedHashMap<>();
            batch.put("evaluation_batch_id", batchId);
            batch.put("row_count", recs.size());
            batch.put("created_at", recs.stream().map(EquipmentQtEvaluationRecord::getCreatedAt).filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(null));
            batches.add(batch);
        });
        batches.sort((a, b) -> {
            Object ca = a.get("created_at");
            Object cb = b.get("created_at");
            if (ca == null && cb == null) return 0;
            if (ca == null) return 1;
            if (cb == null) return -1;
            return ((LocalDateTime) cb).compareTo((LocalDateTime) ca);
        });
        return batches;
    }

    /**
     * 按批次获取定量评估记录
     */
    public List<Map<String, Object>> getRecordsByBatch(String batchId) {
        if (batchId == null || batchId.isBlank()) return Collections.emptyList();
        List<EquipmentQtIndicatorDef> indicators = qtDefRepository.findByEnabledTrueOrderByDisplayOrderAsc();
        return qtRecordRepository.findByEvaluationBatchIdOrderByOperationId(batchId).stream()
                .map(r -> convertRecordToMap(r, indicators))
                .toList();
    }

    /**
     * 按批次删除
     */
    @Transactional
    public int deleteByBatch(String batchId) {
        if (batchId == null || batchId.isBlank()) return 0;
        qtRecordRepository.deleteByEvaluationBatchId(batchId);
        return 1;
    }

    // ==================== 私有方法 ====================

    private BigDecimal calculateSingleIndicator(EquipmentQtIndicatorDef ind, Long operationId) {
        try {
            EquipmentQtIndicatorDef.AggregationMethod method = ind.getAggregationMethod();
            String table = ind.getSourceTable();
            String field = ind.getSourceField();
            int op = operationId.intValue();

            switch (method) {
                case direct:
                    if (field == null || field.isBlank() || table == null || table.isBlank()) break;
                    return queryNullableDecimal(
                            String.format("(SELECT %s FROM %s WHERE operation_id = ? LIMIT 1)", field, table),
                            op);

                case avg:
                    if (field == null || field.isBlank() || table == null || table.isBlank()) break;
                    return queryNullableDecimal(
                            String.format("SELECT AVG(%s) FROM %s WHERE operation_id = ? AND %s IS NOT NULL", field, table, field),
                            op);

                case sum:
                    if (field == null || field.isBlank() || table == null || table.isBlank()) break;
                    return queryNullableDecimal(
                            String.format("SELECT COALESCE(SUM(%s), 0) FROM %s WHERE operation_id = ?", field, table),
                            op);

                case count:
                    if (table == null || table.isBlank()) break;
                    return queryNullableDecimal(
                            String.format("SELECT COUNT(*) FROM %s WHERE operation_id = ?", table),
                            op);

                case percentage:
                    if (field == null || field.isBlank() || table == null || table.isBlank()) break;
                    String extraPct = extraPredicateFromTemplate(ind.getCustomSqlTemplate());
                    String[] parts = field.split("/");
                    String colA = parts[0].trim();
                    String sqlPct;
                    if (parts.length >= 2 && !parts[1].trim().isEmpty()) {
                        String colB = parts[1].trim();
                        sqlPct = String.format(
                                "SELECT SUM(%s) * 100.0 / NULLIF(SUM(%s), 0) FROM %s WHERE operation_id = ?%s",
                                colA, colB, table, extraPct);
                    } else {
                        sqlPct = String.format(
                                "SELECT SUM(CASE WHEN %s = 1 THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(*), 0) FROM %s WHERE operation_id = ?%s",
                                colA, table, extraPct);
                    }
                    return queryNullableDecimal(sqlPct, op);

                case avg_conditional:
                    if (field == null || field.isBlank() || table == null || table.isBlank()) break;
                    String cond = ind.getCustomSqlTemplate();
                    if (cond == null || cond.isBlank()) break;
                    return queryNullableDecimal(
                            String.format("SELECT AVG(%s) FROM %s WHERE operation_id = ? AND (%s)", field, table, cond),
                            op);

                case custom:
                    String tpl = ind.getCustomSqlTemplate();
                    if (tpl == null || tpl.isBlank()) break;
                    String sql = tpl.replace("{operation_id}", String.valueOf(op));
                    List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
                    if (!result.isEmpty() && result.get(0).values().iterator().next() != null) {
                        Object val = result.get(0).values().iterator().next();
                        return new BigDecimal(val.toString()).setScale(4, RoundingMode.HALF_UP);
                    }
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            log.warn("计算指标 {} 失败 (opId={}): {}", ind.getIndicatorKey(), operationId, e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    /**
     * custom_sql_template 中若配置为 "WHERE col = 'x'"，拼到已有 WHERE operation_id = ? 后改为 AND (...)
     */
    private static String extraPredicateFromTemplate(String customSqlTemplate) {
        if (customSqlTemplate == null || customSqlTemplate.isBlank()) {
            return "";
        }
        String t = customSqlTemplate.trim();
        if (t.length() >= 6 && t.regionMatches(true, 0, "WHERE ", 0, 6)) {
            return " AND (" + t.substring(6).trim() + ")";
        }
        return " AND (" + t + ")";
    }

    /**
     * 聚合查询可能返回 NULL（无行或全 NULL），避免 queryForObject 抛错导致整指标变 0
     */
    private BigDecimal queryNullableDecimal(String sql, Object... args) {
        List<BigDecimal> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            BigDecimal bd = rs.getBigDecimal(1);
            return rs.wasNull() ? null : bd;
        }, args);
        if (list.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal v = list.get(0);
        if (v == null) {
            return BigDecimal.ZERO;
        }
        return v.setScale(4, RoundingMode.HALF_UP);
    }

    private Map<String, Object> buildResult(boolean success, String message, int count, String batchId, List<Map<String, Object>> data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("message", message);
        result.put("count", count);
        if (batchId != null) result.put("evaluationBatchId", batchId);
        if (data != null) result.put("data", data);
        return result;
    }

    private Map<String, Object> convertDefToMap(EquipmentQtIndicatorDef def) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("indicator_key", def.getIndicatorKey());
        m.put("indicator_name", def.getIndicatorName());
        m.put("indicator_name_en", def.getIndicatorNameEn());
        m.put("phase", def.getPhase());
        m.put("dimension", def.getDimension());
        m.put("description", def.getDescription());
        m.put("source_table", def.getSourceTable());
        m.put("source_field", def.getSourceField());
        m.put("aggregation_method", def.getAggregationMethod());
        m.put("custom_sql_template", def.getCustomSqlTemplate());
        m.put("unit", def.getUnit());
        m.put("score_direction", def.getScoreDirection());
        m.put("display_order", def.getDisplayOrder());
        m.put("enabled", def.getEnabled());
        return m;
    }

    private Map<String, Object> convertRecordToMap(EquipmentQtEvaluationRecord rec, List<EquipmentQtIndicatorDef> indicators) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rec.getId());
        m.put("operation_id", rec.getOperationId());
        m.put("evaluation_batch_id", rec.getEvaluationBatchId());
        m.put("evaluation_time", rec.getEvaluationTime());
        m.put("raw_data", parseJsonToMap(rec.getRawData(), Object.class));
        m.put("normalized_scores", parseJsonToMap(rec.getNormalizedScores(), Object.class));
        m.put("composite_score", rec.getCompositeScore());
        m.put("created_at", rec.getCreatedAt());
        return m;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private <T> Map<String, T> parseJsonToMap(String json, Class<T> valueType) {
        try {
            if (json == null || json.isBlank()) return new HashMap<>();
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * 解析 raw_data JSON：Jackson 会把整数反序列化为 Integer、小数为 Double，不能直接当作 BigDecimal。
     */
    private Map<String, BigDecimal> parseRawDataJson(String json) {
        Map<String, Object> loose = parseJsonToMap(json, Object.class);
        if (loose.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, BigDecimal> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : loose.entrySet()) {
            BigDecimal bd = coerceNumberToBigDecimal(e.getValue());
            if (bd != null) {
                out.put(e.getKey(), bd);
            }
        }
        return out;
    }

    private static BigDecimal coerceNumberToBigDecimal(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof BigDecimal) {
            return (BigDecimal) o;
        }
        if (o instanceof Number) {
            return new BigDecimal(((Number) o).toString());
        }
        if (o instanceof Boolean) {
            return (Boolean) o ? BigDecimal.ONE : BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(o.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String newBatchId() {
        String ts = LocalDateTime.now().format(BATCH_ID_FORMAT);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        return "EQT-BATCH-" + ts + "-" + suffix;
    }
}
