package com.ccnu.military.service;

import com.ccnu.military.entity.*;
import com.ccnu.military.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 动态定量评估服务
 * 完全动态化的定量评估系统
 */
@Slf4j
@Service
public class DynamicQtService {

    private final JdbcTemplate jdbcTemplate;
    private final IndicatorTemplateRepository templateRepository;
    private final LevelDefinitionRepository levelRepository;
    private final PrimaryDimensionRepository primaryRepository;
    private final SecondaryDimensionRepository secondaryRepository;

    public DynamicQtService(
            JdbcTemplate jdbcTemplate,
            IndicatorTemplateRepository templateRepository,
            LevelDefinitionRepository levelRepository,
            PrimaryDimensionRepository primaryRepository,
            SecondaryDimensionRepository secondaryRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.templateRepository = templateRepository;
        this.levelRepository = levelRepository;
        this.primaryRepository = primaryRepository;
        this.secondaryRepository = secondaryRepository;
    }

    /**
     * 获取所有批次列表
     */
    public List<Map<String, Object>> getBatches() {
        String sql = "SELECT batch_id, template_id, template_name, description, operation_ids, " +
                "status, created_at FROM dynamic_qt_evaluation_batch ORDER BY created_at DESC";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * 创建新批次（不创建任何记录，只是创建批次）
     */
    @Transactional
    public Map<String, Object> createBatch(Long templateId, String description) {
        IndicatorTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));

        String batchId = "DYN_QT_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        String sql = "INSERT INTO dynamic_qt_evaluation_batch " +
                "(batch_id, template_id, template_name, description, status) " +
                "VALUES (?, ?, ?, ?, 'active')";
        jdbcTemplate.update(sql, batchId, templateId, template.getTemplateName(), description);

        Map<String, Object> result = new HashMap<>();
        result.put("batchId", batchId);
        result.put("templateId", templateId);
        result.put("templateName", template.getTemplateName());
        return result;
    }

    /**
     * 获取模板的所有定量指标（层级 -> 一级 -> 二级）
     */
    private List<SecondaryDimension> getQuantitativeIndicators(Long templateId) {
        List<LevelDefinition> levels = levelRepository.findByTemplateIdOrderBySortOrder(templateId);
        List<SecondaryDimension> allQtIndicators = new ArrayList<>();

        for (LevelDefinition level : levels) {
            List<PrimaryDimension> primaries = primaryRepository.findByLevelIdOrderBySortOrder(level.getId());
            for (PrimaryDimension primary : primaries) {
                List<SecondaryDimension> secondaries =
                        secondaryRepository.findByPrimaryDimensionIdOrderBySortOrder(primary.getId());
                for (SecondaryDimension sec : secondaries) {
                    if (sec.getMetricType() == SecondaryDimension.MetricType.QUANTITATIVE) {
                        allQtIndicators.add(sec);
                    }
                }
            }
        }
        return allQtIndicators;
    }

    /**
     * 获取批次的指标列表（按层级分组）
     * 返回结构:
     * {
     *   "batchId": "xxx",
     *   "levels": [
     *     {
     *       "levelName": "战略态势感知",
     *       "primaries": [
     *         {
     *           "primaryName": "情报获取",
     *           "secondaries": [
     *             { "code": "SEC001", "name": "指标1", "direction": "POSITIVE" }
     *           ]
     *         }
     *       ]
     *     }
     *   ]
     * }
     */
    public Map<String, Object> getIndicatorsByBatch(String batchId) {
        String batchSql = "SELECT template_id FROM dynamic_qt_evaluation_batch WHERE batch_id = ?";
        Map<String, Object> batchInfo = jdbcTemplate.queryForMap(batchSql, batchId);
        Long templateId = ((Number) batchInfo.get("template_id")).longValue();

        List<SecondaryDimension> qtIndicators = getQuantitativeIndicators(templateId);

        // 按层级分组，再按一级维度分组
        Map<String, Map<String, List<Map<String, Object>>>> levelGroups = new LinkedHashMap<>();
        int totalCount = 0;

        for (SecondaryDimension indicator : qtIndicators) {
            String levelName = indicator.getPrimaryDimension().getLevel().getLevelName();
            String primaryName = indicator.getPrimaryDimension().getDimensionName();

            levelGroups.computeIfAbsent(levelName, k -> new LinkedHashMap<>());
            levelGroups.get(levelName).computeIfAbsent(primaryName, k -> new ArrayList<>());

            Map<String, Object> indicatorInfo = new LinkedHashMap<>();
            indicatorInfo.put("code", indicator.getDimensionCode());
            indicatorInfo.put("name", indicator.getDimensionName());
            indicatorInfo.put("unit", indicator.getUnit());
            indicatorInfo.put("direction", indicator.getScoreDirection() != null ?
                    indicator.getScoreDirection().name() : "POSITIVE");

            levelGroups.get(levelName).get(primaryName).add(indicatorInfo);
            totalCount++;
        }

        // 转换为前端需要的格式
        List<Map<String, Object>> levels = new ArrayList<>();
        int levelCount = 0;
        int primaryCount = 0;

        for (Map.Entry<String, Map<String, List<Map<String, Object>>>> levelEntry : levelGroups.entrySet()) {
            Map<String, Object> levelData = new LinkedHashMap<>();
            levelData.put("levelName", levelEntry.getKey());

            List<Map<String, Object>> primaries = new ArrayList<>();
            for (Map.Entry<String, List<Map<String, Object>>> primaryEntry : levelEntry.getValue().entrySet()) {
                Map<String, Object> primaryData = new LinkedHashMap<>();
                primaryData.put("primaryName", primaryEntry.getKey());
                primaryData.put("secondaries", primaryEntry.getValue());
                primaries.add(primaryData);
                primaryCount++;
            }

            levelData.put("primaries", primaries);
            levels.add(levelData);
            levelCount++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("batchId", batchId);
        result.put("templateId", templateId);
        result.put("levels", levels);
        result.put("levelCount", levelCount);
        result.put("primaryCount", primaryCount);
        result.put("indicatorCount", totalCount);
        return result;
    }

    /**
     * 获取评估记录
     * 返回: { operationIds: [], tableData: [{ operationId, values: { code: value } }] }
     */
    public Map<String, Object> getRecords(String batchId) {
        String opsSql = "SELECT DISTINCT operation_id FROM dynamic_qt_evaluation_record WHERE batch_id = ? ORDER BY operation_id";
        List<String> operationIds = jdbcTemplate.queryForList(opsSql, String.class, batchId);

        String dataSql = "SELECT operation_id, secondary_code, raw_value, normalized_value, is_simulated " +
                "FROM dynamic_qt_evaluation_record WHERE batch_id = ?";
        List<Map<String, Object>> allData = jdbcTemplate.queryForList(dataSql, batchId);

        Map<String, Map<String, Object>> dataByOp = new LinkedHashMap<>();
        for (String opId : operationIds) {
            dataByOp.put(opId, new LinkedHashMap<>());
        }

        for (Map<String, Object> row : allData) {
            String opId = (String) row.get("operation_id");
            String code = (String) row.get("secondary_code");
            BigDecimal rawValue = (BigDecimal) row.get("raw_value");
            Boolean isSimulated = (Boolean) row.get("is_simulated");

            Map<String, Object> opData = dataByOp.get(opId);
            if (opData != null) {
                opData.put(code, rawValue != null ? rawValue.doubleValue() : null);
                opData.put(code + "_simulated", isSimulated != null ? isSimulated : false);
            }
        }

        List<Map<String, Object>> tableData = new ArrayList<>();
        for (String opId : operationIds) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("operationId", opId);
            row.put("values", dataByOp.get(opId));
            tableData.add(row);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("batchId", batchId);
        result.put("operationIds", operationIds);
        result.put("tableData", tableData);
        return result;
    }

    /**
     * 全局模拟：根据指定次数生成作战数据
     */
    @Transactional
    public Map<String, Object> globalSimulate(String batchId, int count) {
        // 1. 先获取模板ID和指标列表
        String batchSql = "SELECT template_id FROM dynamic_qt_evaluation_batch WHERE batch_id = ?";
        Map<String, Object> batchInfo = jdbcTemplate.queryForMap(batchSql, batchId);
        Long templateId = ((Number) batchInfo.get("template_id")).longValue();

        List<SecondaryDimension> qtIndicators = getQuantitativeIndicators(templateId);

        // 2. 生成指定数量的作战ID
        List<String> operationIds = new ArrayList<>();
        // 获取当前已有数量，避免ID重复
        String countSql = "SELECT COUNT(DISTINCT operation_id) FROM dynamic_qt_evaluation_record WHERE batch_id = ?";
        int existingCount = jdbcTemplate.queryForObject(countSql, Integer.class, batchId);

        for (int i = 1; i <= count; i++) {
            String opId = "OP_" + String.format("%03d", existingCount + i);
            operationIds.add(opId);
        }

        // 3. 为生成的作战ID创建空记录
        for (String opId : operationIds) {
            createEmptyRecords(batchId, templateId, opId, qtIndicators);
        }

        // 4. 更新批次表的operation_ids
        String existingOps = "";
        try {
            String selectOps = "SELECT operation_ids FROM dynamic_qt_evaluation_batch WHERE batch_id = ?";
            existingOps = jdbcTemplate.queryForObject(selectOps, String.class, batchId);
        } catch (Exception e) {
            existingOps = "";
        }

        Set<String> allOps = new LinkedHashSet<>();
        if (existingOps != null && !existingOps.isEmpty()) {
            String[] parts = existingOps.split(",");
            for (String p : parts) {
                if (!p.trim().isEmpty()) {
                    allOps.add(p.trim());
                }
            }
        }
        allOps.addAll(operationIds);

        String newOpsStr = String.join(",", allOps);
        String updateBatch = "UPDATE dynamic_qt_evaluation_batch SET operation_ids = ? WHERE batch_id = ?";
        jdbcTemplate.update(updateBatch, newOpsStr, batchId);

        // 5. 模拟所有单元格的随机值
        int simulatedCount = 0;
        Map<String, Double> results = new LinkedHashMap<>();

        for (String opId : operationIds) {
            for (SecondaryDimension indicator : qtIndicators) {
                String code = indicator.getDimensionCode();
                double value = Math.round(Math.random() * 1000) / 1000.0;

                String updateSql = "UPDATE dynamic_qt_evaluation_record " +
                        "SET raw_value = ?, is_simulated = TRUE, updated_at = NOW() " +
                        "WHERE batch_id = ? AND operation_id = ? AND secondary_code = ?";
                jdbcTemplate.update(updateSql, value, batchId, opId, code);

                results.put(opId + "_" + code, value);
                simulatedCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("simulatedCount", simulatedCount);
        result.put("operationCount", operationIds.size());
        result.put("operationIds", operationIds);
        result.put("results", results);
        return result;
    }

    /**
     * 为批次创建空记录
     */
    private void createEmptyRecords(String batchId, Long templateId, String operationId,
                                   List<SecondaryDimension> indicators) {
        for (SecondaryDimension indicator : indicators) {
            PrimaryDimension primary = indicator.getPrimaryDimension();
            LevelDefinition level = primary.getLevel();

            String sql = "INSERT IGNORE INTO dynamic_qt_evaluation_record " +
                    "(batch_id, template_id, operation_id, level_id, level_name, " +
                    "primary_dimension_id, primary_dimension, secondary_dimension_id, secondary_dimension, secondary_code, " +
                    "metric_type, unit, is_simulated) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE)";

            jdbcTemplate.update(sql,
                    batchId, templateId, operationId,
                    level.getId(), level.getLevelName(),
                    primary.getId(), primary.getDimensionName(),
                    indicator.getId(), indicator.getDimensionName(), indicator.getDimensionCode(),
                    "QUANTITATIVE", indicator.getUnit());
        }
    }

    /**
     * 模拟单个单元格
     */
    @Transactional
    public Double simulateCell(String batchId, String operationId, String secondaryCode) {
        double value = Math.round(Math.random() * 1000) / 1000.0;

        String sql = "UPDATE dynamic_qt_evaluation_record " +
                "SET raw_value = ?, is_simulated = TRUE, updated_at = NOW() " +
                "WHERE batch_id = ? AND operation_id = ? AND secondary_code = ?";
        jdbcTemplate.update(sql, value, batchId, operationId, secondaryCode);

        return value;
    }

    /**
     * 批量模拟（按单元格列表）
     */
    @Transactional
    public Map<String, Double> batchSimulate(String batchId, List<Map<String, String>> cells) {
        Map<String, Double> results = new LinkedHashMap<>();

        String sql = "UPDATE dynamic_qt_evaluation_record " +
                "SET raw_value = ?, is_simulated = TRUE, updated_at = NOW() " +
                "WHERE batch_id = ? AND operation_id = ? AND secondary_code = ?";

        for (Map<String, String> cell : cells) {
            String operationId = cell.get("operationId");
            String secondaryCode = cell.get("secondaryCode");
            double value = Math.round(Math.random() * 1000) / 1000.0;

            jdbcTemplate.update(sql, value, batchId, operationId, secondaryCode);
            results.put(operationId + "_" + secondaryCode, value);
        }

        return results;
    }

    /**
     * 保存单条记录
     */
    @Transactional
    public void saveRecord(Map<String, Object> request) {
        String batchId = (String) request.get("batchId");
        String operationId = (String) request.get("operationId");
        String secondaryCode = (String) request.get("secondaryCode");
        Double value = ((Number) request.get("value")).doubleValue();

        String sql = "UPDATE dynamic_qt_evaluation_record " +
                "SET raw_value = ?, is_simulated = FALSE, updated_at = NOW() " +
                "WHERE batch_id = ? AND operation_id = ? AND secondary_code = ?";
        jdbcTemplate.update(sql, value, batchId, operationId, secondaryCode);
    }

    /**
     * 批量保存记录
     */
    @Transactional
    public int saveRecords(List<Map<String, Object>> records) {
        int count = 0;
        for (Map<String, Object> record : records) {
            saveRecord(record);
            count++;
        }
        return count;
    }

    /**
     * 归一化处理
     */
    @Transactional
    public Map<String, Object> normalize(String batchId) {
        // 获取所有指标的方向
        Map<String, Object> indicatorsInfo = getIndicatorsByBatch(batchId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> levels = (List<Map<String, Object>>) indicatorsInfo.get("levels");

        Map<String, String> directionMap = new HashMap<>();
        for (Map<String, Object> level : levels) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> primaries = (List<Map<String, Object>>) level.get("primaries");
            for (Map<String, Object> primary : primaries) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> secondaries = (List<Map<String, Object>>) primary.get("secondaries");
                for (Map<String, Object> sec : secondaries) {
                    directionMap.put((String) sec.get("code"), (String) sec.get("direction"));
                }
            }
        }

        // 获取所有有值的指标
        String indicatorsSql = "SELECT DISTINCT secondary_code FROM dynamic_qt_evaluation_record " +
                "WHERE batch_id = ? AND raw_value IS NOT NULL";
        List<String> indicators = jdbcTemplate.queryForList(indicatorsSql, String.class, batchId);

        int normalizedCount = 0;
        for (String code : indicators) {
            String minSql = "SELECT MIN(raw_value) FROM dynamic_qt_evaluation_record WHERE batch_id = ? AND secondary_code = ? AND raw_value IS NOT NULL";
            String maxSql = "SELECT MAX(raw_value) FROM dynamic_qt_evaluation_record WHERE batch_id = ? AND secondary_code = ? AND raw_value IS NOT NULL";

            Double min = jdbcTemplate.queryForObject(minSql, Double.class, batchId, code);
            Double max = jdbcTemplate.queryForObject(maxSql, Double.class, batchId, code);

            if (min == null || max == null || min.equals(max)) {
                continue;
            }

            String direction = directionMap.getOrDefault(code, "POSITIVE");

            // 保存归一化配置
            String configSql = "INSERT INTO dynamic_qt_normalization_config (batch_id, secondary_code, min_value, max_value, direction) " +
                    "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE min_value = VALUES(min_value), max_value = VALUES(max_value), direction = VALUES(direction)";
            jdbcTemplate.update(configSql, batchId, code, min, max, direction);

            // 执行归一化
            String updateSql = "UPDATE dynamic_qt_evaluation_record SET normalized_value = " +
                    "(CASE WHEN ? = 'POSITIVE' THEN (raw_value - ?) / (? - ?) ELSE (? - raw_value) / (? - ?) END) " +
                    "WHERE batch_id = ? AND secondary_code = ? AND raw_value IS NOT NULL";

            if ("POSITIVE".equals(direction)) {
                normalizedCount += jdbcTemplate.update(updateSql, direction, min, max, min, min, max, batchId, code);
            } else {
                normalizedCount += jdbcTemplate.update(updateSql, direction, max, max, min, max, min, batchId, code);
            }
        }

        String updateBatchSql = "UPDATE dynamic_qt_evaluation_batch SET status = 'normalized' WHERE batch_id = ?";
        jdbcTemplate.update(updateBatchSql, batchId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("normalizedCount", normalizedCount);
        result.put("indicatorCount", indicators.size());
        return result;
    }

    /**
     * 获取归一化后的记录
     */
    public Map<String, Object> getNormalizedRecords(String batchId) {
        String opsSql = "SELECT DISTINCT operation_id FROM dynamic_qt_evaluation_record WHERE batch_id = ? ORDER BY operation_id";
        List<String> operationIds = jdbcTemplate.queryForList(opsSql, String.class, batchId);

        String dataSql = "SELECT operation_id, secondary_code, normalized_value, is_simulated " +
                "FROM dynamic_qt_evaluation_record WHERE batch_id = ? AND normalized_value IS NOT NULL";
        List<Map<String, Object>> allData = jdbcTemplate.queryForList(dataSql, batchId);

        Map<String, Map<String, Object>> dataByOp = new LinkedHashMap<>();
        for (String opId : operationIds) {
            dataByOp.put(opId, new LinkedHashMap<>());
        }

        for (Map<String, Object> row : allData) {
            String opId = (String) row.get("operation_id");
            String code = (String) row.get("secondary_code");
            BigDecimal normalizedValue = (BigDecimal) row.get("normalized_value");

            Map<String, Object> opData = dataByOp.get(opId);
            if (opData != null && normalizedValue != null) {
                opData.put(code, normalizedValue.doubleValue());
            }
        }

        List<Map<String, Object>> tableData = new ArrayList<>();
        for (String opId : operationIds) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("operationId", opId);
            row.put("values", dataByOp.get(opId));
            tableData.add(row);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("batchId", batchId);
        result.put("operationIds", operationIds);
        result.put("tableData", tableData);
        return result;
    }

    /**
     * 删除批次
     */
    @Transactional
    public int deleteBatch(String batchId) {
        jdbcTemplate.update("DELETE FROM dynamic_qt_evaluation_record WHERE batch_id = ?", batchId);
        jdbcTemplate.update("DELETE FROM dynamic_qt_normalization_config WHERE batch_id = ?", batchId);
        int deleted = jdbcTemplate.update("DELETE FROM dynamic_qt_evaluation_batch WHERE batch_id = ?", batchId);
        return deleted;
    }

    /**
     * 获取可用的作战ID列表
     */
    public List<Map<String, Object>> getAvailableOperations() {
        String sql = "SELECT DISTINCT operation_id FROM records_military_operation_info ORDER BY operation_id";
        try {
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.warn("获取作战ID失败，可能表不存在: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 获取批次已有的作战ID列表
     */
    public List<String> getBatchOperations(String batchId) {
        String opsSql = "SELECT DISTINCT operation_id FROM dynamic_qt_evaluation_record WHERE batch_id = ? ORDER BY operation_id";
        return jdbcTemplate.queryForList(opsSql, String.class, batchId);
    }
}
