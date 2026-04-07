package com.ccnu.military.service;

import com.ccnu.military.entity.EquipmentQlEvaluationRecord;
import com.ccnu.military.entity.EquipmentQlIndicatorDef;
import com.ccnu.military.entity.ExpertBaseInfo;
import com.ccnu.military.repository.EquipmentQlEvaluationRecordRepository;
import com.ccnu.military.repository.EquipmentQlIndicatorDefRepository;
import com.ccnu.military.repository.ExpertBaseInfoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 装备操作定性指标评估服务
 * 专家打分录入 + 集结
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentQlEvaluationService {

    private final JdbcTemplate jdbcTemplate;
    private final EquipmentQlIndicatorDefRepository qlDefRepository;
    private final EquipmentQlEvaluationRecordRepository qlRecordRepository;
    private final ExpertBaseInfoRepository expertRepository;
    private final ObjectMapper objectMapper;

    /**
     * 获取所有定性指标配置
     */
    public List<Map<String, Object>> getIndicatorDefs() {
        return qlDefRepository.findByEnabledTrueOrderByDisplayOrderAsc().stream()
                .map(this::convertDefToMap)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定指标的参考数据（供专家打分前参考）
     */
    public List<Map<String, Object>> getReferenceData(Long operationId, String indicatorKey) {
        try {
            Optional<EquipmentQlIndicatorDef> defOpt = qlDefRepository.findById(indicatorKey);
            if (defOpt.isEmpty()) return Collections.emptyList();

            EquipmentQlIndicatorDef def = defOpt.get();
            String sqlTemplate = def.getReferenceSqlTemplate();

            if (sqlTemplate == null || sqlTemplate.isBlank()) {
                // 无自定义SQL，返回空
                return Collections.emptyList();
            }

            String sql = sqlTemplate.replace("{operation_id}", String.valueOf(operationId));
            return jdbcTemplate.queryForList(sql);

        } catch (Exception e) {
            log.warn("获取参考数据失败 (opId={}, indicatorKey={}): {}", operationId, indicatorKey, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 专家提交定性评分
     */
    @Transactional
    public Map<String, Object> submitEvaluation(Map<String, Object> payload) {
        try {
            String batchId = (String) payload.get("evaluationBatchId");
            Long operationId = Long.valueOf(payload.get("operationId").toString());
            Long expertId = Long.valueOf(payload.get("expertId").toString());
            Map<String, Map<String, Object>> scores = parseScores(payload.get("scores"));

            // ====== 非空校验 ======
            if (operationId == null) {
                return Map.of("success", false, "message", "请选择作战ID");
            }
            if (expertId == null) {
                return Map.of("success", false, "message", "请选择专家");
            }

            // 获取所有启用指标定义
            List<EquipmentQlIndicatorDef> enabledIndicators = qlDefRepository.findByEnabledTrueOrderByDisplayOrderAsc();
            List<String> missingIndicators = new ArrayList<>();
            for (EquipmentQlIndicatorDef ind : enabledIndicators) {
                String key = ind.getIndicatorKey();
                Map<String, Object> scoreEntry = scores.get(key);
                if (scoreEntry == null || scoreEntry.get("gradeCode") == null || String.valueOf(scoreEntry.get("gradeCode")).isBlank()) {
                    missingIndicators.add(ind.getIndicatorName());
                }
            }
            if (!missingIndicators.isEmpty()) {
                return Map.of("success", false, "message", "以下指标未打分: " + String.join("、", missingIndicators));
            }

            if (batchId == null || batchId.isBlank()) {
                return Map.of("success", false, "message", "请选择评估批次（应与军事基础数据/指标计算生成的 evaluation_id 一致）");
            }

            return persistQlEvaluation(batchId, operationId, expertId, scores);

        } catch (Exception e) {
            log.error("提交定性评分失败", e);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", "提交失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 模拟生成：当前批次下「全部已可信度评估专家」对指定作战（或全部作战）的定性评分，覆盖已有记录。
     * 请求体可传 {@code allOperations: true}，此时对该批次在 {@link #listOperationsForEvaluationBatch} 中的全部作战各生成一遍。
     * 按指标在 [scoreLow, scoreHigh] 上采样区间中点意向分，离散度越大高斯噪声越大；再映射为允许等级 + 随机把握度。
     */
    @Transactional
    public Map<String, Object> simulateQlBatch(Map<String, Object> request) {
        Map<String, Object> err = new LinkedHashMap<>();
        try {
            String batchId = request.get("evaluationBatchId") != null
                    ? String.valueOf(request.get("evaluationBatchId")).trim() : "";
            if (batchId.isEmpty()) {
                err.put("success", false);
                err.put("message", "请选择评估批次");
                return err;
            }

            List<Long> operationIds = resolveSimulationOperationIds(batchId, request);
            if (operationIds.isEmpty()) {
                err.put("success", false);
                err.put("message", resolveSimulationOperationIdsEmptyMessage(request));
                return err;
            }

            double defLow = clampScore(coalesceDouble(request.get("defaultScoreLow"), 72.0));
            double defHigh = clampScore(coalesceDouble(request.get("defaultScoreHigh"), 86.0));
            if (defLow > defHigh) {
                double t = defLow;
                defLow = defHigh;
                defHigh = t;
            }
            double defDisp = clampDispersion(coalesceDouble(request.get("defaultDispersion"), 40.0));

            @SuppressWarnings("unchecked")
            Map<String, Object> indicatorParamsRaw =
                    request.get("indicatorParams") instanceof Map ? (Map<String, Object>) request.get("indicatorParams") : Collections.emptyMap();

            String sqlExperts = "SELECT e.expert_id AS expert_id, e.expert_name AS expert_name FROM expert_base_info e "
                    + "INNER JOIN expert_credibility_evaluation_score c ON e.expert_id = c.expert_id ORDER BY e.expert_id";
            List<Map<String, Object>> expertRows = jdbcTemplate.queryForList(sqlExperts);
            if (expertRows.isEmpty()) {
                err.put("success", false);
                err.put("message", "没有已完成可信度评估的专家，无法模拟");
                return err;
            }

            List<EquipmentQlIndicatorDef> indicators = qlDefRepository.findByEnabledTrueOrderByDisplayOrderAsc();
            if (indicators.isEmpty()) {
                err.put("success", false);
                err.put("message", "未配置启用的定性指标");
                return err;
            }

            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            int totalSaved = 0;
            for (Long operationId : operationIds) {
                totalSaved += simulateQlBatchOneOperation(batchId, operationId, defLow, defHigh, defDisp,
                        indicatorParamsRaw, expertRows, indicators, rnd);
            }

            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("success", true);
            if (operationIds.size() == 1) {
                ok.put("message", String.format("已为 %d 名专家生成/覆盖定性评分（作战 %s）", totalSaved, operationIds.get(0)));
            } else {
                ok.put("message", String.format("已为 %d 个作战、共 %d 条专家评分生成/覆盖定性数据（每作战每名专家一条）",
                        operationIds.size(), totalSaved));
            }
            ok.put("expertCount", expertRows.size());
            ok.put("operationCount", operationIds.size());
            ok.put("savedCount", totalSaved);
            ok.put("evaluationBatchId", batchId);
            ok.put("operationIds", operationIds);
            if (operationIds.size() == 1) {
                ok.put("operationId", operationIds.get(0));
            }
            return ok;
        } catch (Exception e) {
            log.error("定性模拟批量生成失败", e);
            err.put("success", false);
            err.put("message", "模拟失败: " + e.getMessage());
            return err;
        }
    }

    private static String simulationOperationIdToken(Object o) {
        if (o == null) {
            return "";
        }
        return String.valueOf(o).trim();
    }

    private static boolean isAllOperationsSimulationRequest(Map<String, Object> request) {
        Object flag = request.get("allOperations");
        if (Boolean.TRUE.equals(flag) || "true".equalsIgnoreCase(String.valueOf(flag))) {
            return true;
        }
        String token = simulationOperationIdToken(request.get("operationId"));
        return "ALL".equalsIgnoreCase(token) || "__ALL__".equals(token);
    }

    private List<Long> resolveSimulationOperationIds(String batchId, Map<String, Object> request) {
        if (isAllOperationsSimulationRequest(request)) {
            List<Map<String, Object>> rows = listOperationsForEvaluationBatch(batchId);
            List<Long> ids = new ArrayList<>();
            for (Map<String, Object> r : rows) {
                Object o = r.get("operation_id");
                if (o != null) {
                    ids.add(toLongId(o));
                }
            }
            return ids;
        }
        if (request.get("operationId") == null) {
            return Collections.emptyList();
        }
        String token = simulationOperationIdToken(request.get("operationId"));
        if (token.isEmpty() || "ALL".equalsIgnoreCase(token) || "__ALL__".equals(token)) {
            return Collections.emptyList();
        }
        return Collections.singletonList(toLongId(request.get("operationId")));
    }

    private static String resolveSimulationOperationIdsEmptyMessage(Map<String, Object> request) {
        if (isAllOperationsSimulationRequest(request)) {
            return "该评估批次下没有可选作战（请确认已执行指标计算且 metrics 表中有该批次作战）";
        }
        return "请选择作战ID";
    }

    private static long toLongId(Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        return Long.parseLong(String.valueOf(o).trim());
    }

    private int simulateQlBatchOneOperation(String batchId, long operationId,
                                            double defLow, double defHigh, double defDisp,
                                            Map<String, Object> indicatorParamsRaw,
                                            List<Map<String, Object>> expertRows,
                                            List<EquipmentQlIndicatorDef> indicators,
                                            ThreadLocalRandom rnd) {
        int saved = 0;
        for (Map<String, Object> row : expertRows) {
            Object exObj = row.get("expert_id");
            long expertId = exObj instanceof Number ? ((Number) exObj).longValue() : Long.parseLong(String.valueOf(exObj));
            Map<String, Map<String, Object>> scores = new LinkedHashMap<>();
            for (EquipmentQlIndicatorDef ind : indicators) {
                String key = ind.getIndicatorKey();
                Object po = indicatorParamsRaw != null ? indicatorParamsRaw.get(key) : null;
                @SuppressWarnings("unchecked")
                Map<String, Object> p = po instanceof Map ? (Map<String, Object>) po : null;
                double low = defLow;
                double high = defHigh;
                double disp = defDisp;
                if (p != null) {
                    if (p.get("scoreLow") != null) low = clampScore(coalesceDouble(p.get("scoreLow"), low));
                    if (p.get("scoreHigh") != null) high = clampScore(coalesceDouble(p.get("scoreHigh"), high));
                    if (p.get("dispersion") != null) disp = clampDispersion(coalesceDouble(p.get("dispersion"), disp));
                }
                if (low > high) {
                    double swap = low;
                    low = high;
                    high = swap;
                }
                List<String> allowed = allowedGradeCodesForIndicator(ind);
                double mean = (low + high) / 2.0;
                double span = Math.max(high - low, 1e-6);
                double sigma = Math.max(0.8, span / 4.0) * (0.15 + 0.85 * (disp / 100.0));
                double sample = mean + rnd.nextGaussian() * sigma;
                sample = clampScore(sample);
                String grade = snapToAllowedGrade(sample, allowed);
                int confidence = sampleConfidence(rnd, disp);
                Map<String, Object> one = new LinkedHashMap<>();
                one.put("gradeCode", grade);
                one.put("confidence", confidence);
                scores.put(key, one);
            }
            Map<String, Object> oneRes = persistQlEvaluation(batchId, operationId, expertId, scores);
            if (Boolean.TRUE.equals(oneRes.get("success"))) {
                saved++;
            }
        }
        return saved;
    }

    private Map<String, Object> persistQlEvaluation(String batchId, Long operationId, Long expertId,
                                                    Map<String, Map<String, Object>> scores) {
        String expertName = expertRepository.findById(expertId)
                .map(ExpertBaseInfo::getExpertName)
                .orElse("未知专家");

        for (Map.Entry<String, Map<String, Object>> entry : scores.entrySet()) {
            Map<String, Object> scoreEntry = entry.getValue();
            Object gradeCodeObj = scoreEntry.get("gradeCode");
            if (gradeCodeObj != null) {
                String gradeCode = String.valueOf(gradeCodeObj);
                Double numericValue = resolveNumericValueByGrade(gradeCode);
                scoreEntry.put("numericValue", numericValue);
            }
        }

        double totalConfidence = 0;
        double totalScore = 0;
        int count = 0;
        for (Map<String, Object> scoreEntry : scores.values()) {
            Object confObj = scoreEntry.get("confidence");
            Object numObj = scoreEntry.get("numericValue");
            if (confObj != null && numObj != null) {
                totalConfidence += ((Number) confObj).doubleValue();
                totalScore += ((Number) numObj).doubleValue();
                count++;
            }
        }
        double overallConfidence = count > 0 ? totalConfidence / count : 0;
        double avgScore = count > 0 ? totalScore / count : 0;
        String aggregatedGrade = resolveGradeByNumericValue(avgScore);

        Optional<EquipmentQlEvaluationRecord> existingOpt =
                qlRecordRepository.findByEvaluationBatchIdAndOperationIdAndExpertId(
                        batchId, String.valueOf(operationId), expertId);

        boolean isUpdate = existingOpt.isPresent();
        EquipmentQlEvaluationRecord record;
        if (isUpdate) {
            record = existingOpt.get();
        } else {
            record = new EquipmentQlEvaluationRecord();
            record.setOperationId(String.valueOf(operationId));
            record.setEvaluationBatchId(batchId);
            record.setExpertId(expertId);
            record.setExpertName(expertName);
            record.setEvaluationTime(LocalDateTime.now());
        }

        record.setScores(toJson(scores));
        record.setOverallConfidence(BigDecimal.valueOf(overallConfidence).setScale(2, RoundingMode.HALF_UP));
        record.setAggregatedScore(BigDecimal.valueOf(avgScore).setScale(4, RoundingMode.HALF_UP));
        record.setAggregatedGrade(aggregatedGrade);
        record.setSubmittedAt(LocalDateTime.now());
        qlRecordRepository.save(record);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", isUpdate ? "评分已更新（同一批次·作战·专家仅保留一条记录）" : "评分提交成功");
        result.put("updated", isUpdate);
        result.put("evaluationBatchId", batchId);
        result.put("recordId", record.getId());
        return result;
    }

    private static double coalesceDouble(Object o, double d) {
        if (o == null) return d;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return d;
        }
    }

    private static double clampScore(double v) {
        return Math.min(100.0, Math.max(0.0, v));
    }

    private static double clampDispersion(double v) {
        return Math.min(100.0, Math.max(0.0, v));
    }

    private static int sampleConfidence(ThreadLocalRandom rnd, double dispersion) {
        double confMean = 80.0;
        double confSigma = 4.0 + 18.0 * (dispersion / 100.0);
        double c = confMean + rnd.nextGaussian() * confSigma;
        c = Math.min(100.0, Math.max(55.0, c));
        int rounded = (int) Math.round(c / 5.0) * 5;
        return Math.min(100, Math.max(55, rounded));
    }

    /** 指标 JSON 中 A_plus / B_minus 等转为 A+ / B- */
    private static String normalizeQlGradeKey(String raw) {
        if (raw == null || raw.isBlank()) return raw;
        return raw.replace("_plus", "+").replace("_minus", "-");
    }

    private List<String> allowedGradeCodesForIndicator(EquipmentQlIndicatorDef def) {
        List<String> raw = parseJsonList(def.getGradeKeys());
        List<String> out = new ArrayList<>();
        for (String k : raw) {
            String n = normalizeQlGradeKey(k);
            if (n != null && !n.isBlank()) {
                out.add(n);
            }
        }
        if (!out.isEmpty()) {
            return out;
        }
        try {
            List<String> codes = jdbcTemplate.queryForList(
                    "SELECT grade_code FROM evaluation_grade_definition ORDER BY grade_level ASC", String.class);
            return codes != null ? codes : Collections.emptyList();
        } catch (Exception e) {
            return Arrays.asList("A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D+", "D", "D-", "E+", "E", "E-");
        }
    }

    private String snapToAllowedGrade(double numericSample, List<String> allowed) {
        if (allowed == null || allowed.isEmpty()) {
            return resolveGradeByNumericValue(numericSample);
        }
        String direct = resolveGradeByNumericValue(numericSample);
        if (allowed.contains(direct)) {
            return direct;
        }
        String best = allowed.get(0);
        double bestDist = Double.MAX_VALUE;
        for (String code : allowed) {
            Double mid = resolveNumericValueByGrade(code);
            if (mid == null) continue;
            double d = Math.abs(mid - numericSample);
            if (d < bestDist) {
                bestDist = d;
                best = code;
            }
        }
        return best;
    }

    /**
     * 获取评估批次列表：优先来自军事通信指标表 metrics_military_comm_effect（与「指标计算」/基础数据生成批次一致），
     * 并合并仅存在于定性评估记录中的历史批次。
     */
    public List<Map<String, Object>> listBatches() {
        String sqlMetrics = "SELECT evaluation_id AS evaluation_batch_id, COUNT(*) AS row_count, "
                + "MAX(created_at) AS created_at "
                + "FROM metrics_military_comm_effect GROUP BY evaluation_id "
                + "ORDER BY MAX(created_at) DESC";
        List<Map<String, Object>> metricRows = jdbcTemplate.queryForList(sqlMetrics);

        Map<String, Map<String, Object>> ordered = new LinkedHashMap<>();
        for (Map<String, Object> row : metricRows) {
            String id = String.valueOf(row.get("evaluation_batch_id"));
            Map<String, Object> batch = new LinkedHashMap<>();
            batch.put("evaluation_batch_id", id);
            batch.put("row_count", row.get("row_count"));
            batch.put("created_at", row.get("created_at"));
            batch.put("expert_count", 0L);
            ordered.put(id, batch);
        }

        List<EquipmentQlEvaluationRecord> records = qlRecordRepository.findAll();
        Map<String, List<EquipmentQlEvaluationRecord>> grouped = new LinkedHashMap<>();
        for (EquipmentQlEvaluationRecord r : records) {
            grouped.computeIfAbsent(r.getEvaluationBatchId(), k -> new ArrayList<>()).add(r);
        }

        List<Map<String, Object>> qlOnlyTail = new ArrayList<>();
        grouped.forEach((batchId, recs) -> {
            long expertCount = recs.stream().map(EquipmentQlEvaluationRecord::getExpertId).distinct().count();
            if (ordered.containsKey(batchId)) {
                ordered.get(batchId).put("expert_count", expertCount);
            } else {
                Map<String, Object> batch = new LinkedHashMap<>();
                batch.put("evaluation_batch_id", batchId);
                batch.put("row_count", recs.size());
                batch.put("expert_count", expertCount);
                batch.put("created_at", recs.stream().map(EquipmentQlEvaluationRecord::getCreatedAt)
                        .filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(null));
                qlOnlyTail.add(batch);
            }
        });
        qlOnlyTail.sort((a, b) -> {
            Object ca = a.get("created_at");
            Object cb = b.get("created_at");
            if (ca == null && cb == null) return 0;
            if (ca == null) return 1;
            if (cb == null) return -1;
            return ((LocalDateTime) cb).compareTo((LocalDateTime) ca);
        });

        List<Map<String, Object>> result = new ArrayList<>(ordered.values());
        result.addAll(qlOnlyTail);
        return result;
    }

    /**
     * 某评估批次下可选作战：与批次内指标行一致（metrics_military_comm_effect）；若无则回落为该批次下已有定性记录的作战。
     */
    public List<Map<String, Object>> listOperationsForEvaluationBatch(String evaluationBatchId) {
        if (evaluationBatchId == null || evaluationBatchId.trim().isEmpty()) {
            return listAllOperationsForSelect();
        }
        String bid = evaluationBatchId.trim();
        String sql = "SELECT o.operation_id, o.avg_network_setup_time_ms, o.total_node_count, o.isolated_node_count, "
                + "o.command_personnel_count, o.weather_condition, o.notes "
                + "FROM records_military_operation_info o "
                + "INNER JOIN (SELECT DISTINCT operation_id FROM metrics_military_comm_effect WHERE evaluation_id = ?) m "
                + "ON o.operation_id = m.operation_id "
                + "ORDER BY o.operation_id DESC";
        List<Map<String, Object>> fromMetrics = jdbcTemplate.queryForList(sql, bid);
        if (!fromMetrics.isEmpty()) {
            return fromMetrics;
        }
        List<String> opIds = qlRecordRepository.findByEvaluationBatchIdOrderByOperationId(bid).stream()
                .map(EquipmentQlEvaluationRecord::getOperationId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (opIds.isEmpty()) {
            return Collections.emptyList();
        }
        String placeholders = String.join(",", Collections.nCopies(opIds.size(), "?"));
        String sqlFallback = "SELECT operation_id, avg_network_setup_time_ms, total_node_count, isolated_node_count, "
                + "command_personnel_count, weather_condition, notes "
                + "FROM records_military_operation_info WHERE operation_id IN (" + placeholders + ") ORDER BY operation_id DESC";
        return jdbcTemplate.queryForList(sqlFallback, opIds.toArray());
    }

    private List<Map<String, Object>> listAllOperationsForSelect() {
        String sql = "SELECT operation_id, avg_network_setup_time_ms, total_node_count, isolated_node_count, "
                + "command_personnel_count, weather_condition, notes "
                + "FROM records_military_operation_info ORDER BY operation_id DESC";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * 获取指定专家在指定批次的定性评估记录（用于二次打开回显）
     */
    public Map<String, Object> getRecordForEdit(String batchId, String operationId, Long expertId) {
        if (batchId == null || operationId == null || expertId == null) {
            return null;
        }
        Optional<EquipmentQlEvaluationRecord> opt = qlRecordRepository
                .findByEvaluationBatchIdAndOperationIdAndExpertId(batchId, operationId, expertId);
        if (opt.isEmpty()) return null;
        return convertRecordToMap(opt.get());
    }

    /**
     * 按批次+作战获取定性评估记录
     */
    public List<Map<String, Object>> getRecordsByBatchAndOperation(String batchId, String operationId) {
        if (batchId == null || batchId.isBlank()) return Collections.emptyList();
        List<EquipmentQlEvaluationRecord> records;
        if (operationId != null && !operationId.isBlank()) {
            records = qlRecordRepository.findByEvaluationBatchIdAndOperationIdOrderByExpertId(batchId, operationId);
        } else {
            records = qlRecordRepository.findByEvaluationBatchIdOrderByOperationId(batchId);
        }
        return records.stream().map(this::convertRecordToMap).collect(Collectors.toList());
    }

    /**
     * 按批次删除
     */
    @Transactional
    public int deleteByBatch(String batchId) {
        if (batchId == null || batchId.isBlank()) return 0;
        qlRecordRepository.deleteByEvaluationBatchId(batchId);
        return 1;
    }

    // ==================== 私有方法 ====================

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> parseScores(Object scoresObj) {
        try {
            if (scoresObj instanceof String) {
                return objectMapper.readValue((String) scoresObj, new TypeReference<Map<String, Map<String, Object>>>() {});
            } else if (scoresObj instanceof Map) {
                return (Map<String, Map<String, Object>>) scoresObj;
            }
        } catch (Exception e) {
            log.warn("解析 scores 失败: {}", e.getMessage());
        }
        return new HashMap<>();
    }

    /**
     * 按分数区间判定综合等级：落在 [min_score, max_score) 内；grade_level=1（最高档）右端闭区间。
     */
    private String resolveGradeByNumericValue(double numericValue) {
        try {
            String sql = "SELECT grade_code, min_score, max_score, grade_level FROM evaluation_grade_definition ORDER BY grade_level ASC";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            for (Map<String, Object> row : rows) {
                BigDecimal minBd = (BigDecimal) row.get("min_score");
                BigDecimal maxBd = (BigDecimal) row.get("max_score");
                if (minBd == null || maxBd == null) continue;
                double mn = minBd.doubleValue();
                double mx = maxBd.doubleValue();
                int level = ((Number) row.get("grade_level")).intValue();
                boolean in = (level == 1)
                        ? (numericValue >= mn && numericValue <= mx)
                        : (numericValue >= mn && numericValue < mx);
                if (in) {
                    return String.valueOf(row.get("grade_code"));
                }
            }
        } catch (Exception e) {
            log.warn("按区间解析综合等级失败，使用近似规则: {}", e.getMessage());
        }
        if (numericValue >= 90) return "A+";
        if (numericValue >= 85) return "A";
        if (numericValue >= 80) return "A-";
        if (numericValue >= 75) return "B+";
        if (numericValue >= 70) return "B";
        if (numericValue >= 65) return "B-";
        if (numericValue >= 60) return "C+";
        if (numericValue >= 55) return "C";
        if (numericValue >= 50) return "C-";
        return "D";
    }

    /**
     * 等级对应分值：优先取区间内点 (min_score+max_score)/2，不再使用代表分值列。
     */
    private Double resolveNumericValueByGrade(String gradeCode) {
        if (gradeCode == null || gradeCode.isBlank()) return null;
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT min_score, max_score FROM evaluation_grade_definition WHERE grade_code = ?", gradeCode);
            BigDecimal minBd = (BigDecimal) row.get("min_score");
            BigDecimal maxBd = (BigDecimal) row.get("max_score");
            if (minBd != null && maxBd != null) {
                return (minBd.doubleValue() + maxBd.doubleValue()) / 2.0;
            }
        } catch (Exception e) {
            log.debug("按区间中点解析等级分值失败 gradeCode={}: {}", gradeCode, e.getMessage());
        }
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT numeric_value FROM evaluation_grade_definition WHERE grade_code = ?", Double.class, gradeCode);
        } catch (Exception e2) {
            return parseGradeFallback(gradeCode);
        }
    }

    private Double parseGradeFallback(String gradeCode) {
        switch (gradeCode) {
            case "A+": return 95.0;
            case "A": return 90.0;
            case "A-": return 85.0;
            case "B+": return 80.0;
            case "B": return 75.0;
            case "B-": return 70.0;
            case "C+": return 65.0;
            case "C": return 60.0;
            case "C-": return 55.0;
            case "D+": return 47.0;
            case "D": return 39.0;
            case "D-": return 30.0;
            case "E+": return 20.0;
            case "E": return 10.0;
            case "E-": return 0.0;
            default: return 70.0;
        }
    }

    private Map<String, Object> convertDefToMap(EquipmentQlIndicatorDef def) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("indicator_key", def.getIndicatorKey());
        m.put("indicator_name", def.getIndicatorName());
        m.put("indicator_name_en", def.getIndicatorNameEn());
        m.put("phase", def.getPhase());
        m.put("dimension", def.getDimension());
        m.put("description", def.getDescription());
        m.put("reference_table", def.getReferenceTable());
        m.put("reference_field", def.getReferenceField());
        m.put("reference_sql_template", def.getReferenceSqlTemplate());
        m.put("evaluation_method", def.getEvaluationMethod());
        m.put("grade_keys", parseJsonList(def.getGradeKeys()));
        m.put("confidence_required", def.getConfidenceRequired());
        m.put("confidence_method", def.getConfidenceMethod());
        m.put("allow_comment", def.getAllowComment());
        m.put("scoring_help", def.getScoringHelp());
        m.put("display_order", def.getDisplayOrder());
        m.put("enabled", def.getEnabled());
        return m;
    }

    private Map<String, Object> convertRecordToMap(EquipmentQlEvaluationRecord rec) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rec.getId());
        m.put("operation_id", rec.getOperationId());
        m.put("evaluation_batch_id", rec.getEvaluationBatchId());
        m.put("evaluation_time", rec.getEvaluationTime());
        m.put("expert_id", rec.getExpertId());
        m.put("expert_name", rec.getExpertName());
        m.put("scores", parseScores(rec.getScores()));
        m.put("aggregated_grade", rec.getAggregatedGrade());
        m.put("aggregated_score", rec.getAggregatedScore());
        m.put("overall_confidence", rec.getOverallConfidence());
        m.put("submitted_at", rec.getSubmittedAt());
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

    private List<String> parseJsonList(String json) {
        try {
            if (json == null || json.isBlank()) return new ArrayList<>();
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
