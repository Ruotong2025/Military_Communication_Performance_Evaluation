package com.ccnu.military.service;

import com.ccnu.military.entity.DynamicDimension;
import com.ccnu.military.entity.DynamicDimension.DimensionLevel;
import com.ccnu.military.entity.DynamicTemplate;
import com.ccnu.military.repository.DynamicDimensionRepository;
import com.ccnu.military.repository.DynamicTemplateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 动态定量评估服务
 * 核心概念：
 * - 模板 (Template): 定义指标结构
 * - 批次 (Batch): 关联模板，包含若干作战的原始数据
 * - 归一化结果: 基于批次原始数据计算的结果，属于该批次
 */
@Slf4j
@Service
public class DynamicQtService {

    private final JdbcTemplate jdbcTemplate;
    private final DynamicTemplateRepository templateRepository;
    private final DynamicDimensionRepository dimensionRepository;

    public DynamicQtService(
            JdbcTemplate jdbcTemplate,
            DynamicTemplateRepository templateRepository,
            DynamicDimensionRepository dimensionRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.templateRepository = templateRepository;
        this.dimensionRepository = dimensionRepository;
    }

    // ==================== 批次管理 ====================

    /**
     * 获取所有评估批次列表（带统计信息）
     */
    public List<Map<String, Object>> getEvaluationBatches() {
        String sql = "SELECT DISTINCT batch_id, MAX(template_id) as template_id, MAX(template_name) as template_name, " +
                "MAX(created_at) as created_at " +
                "FROM dynamic_qt_record WHERE batch_id IS NOT NULL GROUP BY batch_id ORDER BY created_at DESC";

        List<Map<String, Object>> batches = jdbcTemplate.queryForList(sql);

        for (Map<String, Object> batch : batches) {
            String batchId = (String) batch.get("batch_id");

            String opCountSql = "SELECT COUNT(DISTINCT operation_id) FROM dynamic_qt_record WHERE batch_id = ?";
            int opCount = jdbcTemplate.queryForObject(opCountSql, Integer.class, batchId);
            batch.put("operationCount", opCount);
        }

        return batches;
    }

    /**
     * 根据模板ID获取批次列表
     */
    public List<Map<String, Object>> getBatchesByTemplate(Long templateId) {
        String sql = "SELECT DISTINCT batch_id, MAX(template_id) as template_id, MAX(template_name) as template_name, " +
                "MAX(created_at) as created_at " +
                "FROM dynamic_qt_record WHERE template_id = ? GROUP BY batch_id ORDER BY created_at DESC";

        List<Map<String, Object>> batches = jdbcTemplate.queryForList(sql, templateId);

        for (Map<String, Object> batch : batches) {
            String batchId = (String) batch.get("batch_id");

            String opCountSql = "SELECT COUNT(DISTINCT operation_id) FROM dynamic_qt_record WHERE batch_id = ? AND normalization_name IS NULL";
            int opCount = jdbcTemplate.queryForObject(opCountSql, Integer.class, batchId);
            batch.put("operationCount", opCount);
        }

        return batches;
    }

    /**
     * 创建评估批次
     */
    @Transactional
    public Map<String, Object> createEvaluationBatch(Long templateId, String description) {
        DynamicTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));

        String batchId = "DYN_QT_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        Map<String, Object> result = new HashMap<>();
        result.put("batchId", batchId);
        result.put("templateId", templateId);
        result.put("templateName", template.getTemplateName());
        result.put("operationCount", 0);
        return result;
    }

    /**
     * 删除评估批次（删除该批次的所有数据）
     */
    @Transactional
    public void deleteEvaluationBatch(String batchId) {
        jdbcTemplate.update("DELETE FROM dynamic_qt_record WHERE batch_id = ?", batchId);
    }

    /**
     * 检查批次是否存在
     */
    public boolean batchExists(String batchId) {
        String sql = "SELECT COUNT(*) FROM dynamic_qt_record WHERE batch_id = ? LIMIT 1";
        int count = jdbcTemplate.queryForObject(sql, Integer.class, batchId);
        return count > 0;
    }

    /**
     * 获取批次的作战数量
     */
    public int getOperationCount(String batchId) {
        String sql = "SELECT COUNT(DISTINCT operation_id) FROM dynamic_qt_record WHERE batch_id = ? AND normalization_name IS NULL";
        return jdbcTemplate.queryForObject(sql, Integer.class, batchId);
    }

    // ==================== 原始数据管理 ====================

    /**
     * 获取模板的所有二级维度指标（仅定量）
     */
    private List<DynamicDimension> getAllIndicators(Long templateId) {
        return dimensionRepository.findQuantitativeIndicators(templateId);
    }

    /**
     * 获取批次的指标列表（按层级分组）
     */
    public Map<String, Object> getIndicatorsByBatch(String batchId, Long templateId) {
        String batchSql = "SELECT template_id FROM dynamic_qt_record WHERE batch_id = ? LIMIT 1";
        Map<String, Object> firstRecord = null;
        try {
            firstRecord = jdbcTemplate.queryForMap(batchSql, batchId);
        } catch (Exception e) {
            // 如果没有记录，使用传入的 templateId
        }

        Long actualTemplateId;
        if (firstRecord != null) {
            actualTemplateId = ((Number) firstRecord.get("template_id")).longValue();
        } else if (templateId != null) {
            actualTemplateId = templateId;
        } else {
            return new HashMap<>();
        }

        List<DynamicDimension> qtIndicators = getAllIndicators(actualTemplateId);

        // 获取一级维度信息用于分组
        List<DynamicDimension> primaryDims = dimensionRepository.findByTemplateIdAndDimensionLevelOrderBySortOrder(
                actualTemplateId, DimensionLevel.PRIMARY);
        Map<Long, DynamicDimension> primaryDimMap = primaryDims.stream()
                .collect(Collectors.toMap(DynamicDimension::getId, d -> d));

        // 获取层级信息用于分组
        List<DynamicDimension> levelDims = dimensionRepository.findByTemplateIdAndDimensionLevelOrderBySortOrder(
                actualTemplateId, DimensionLevel.LEVEL);
        Map<Long, DynamicDimension> levelDimMap = levelDims.stream()
                .collect(Collectors.toMap(DynamicDimension::getId, d -> d));

        Map<String, Map<String, List<Map<String, Object>>>> levelGroups = new LinkedHashMap<>();
        int totalCount = 0;

        for (DynamicDimension indicator : qtIndicators) {
            // 通过parentId获取一级维度和层级信息
            Long parentId = indicator.getParentId();
            DynamicDimension primary = primaryDimMap.get(parentId);
            DynamicDimension level = primary != null ? levelDimMap.get(primary.getParentId()) : null;

            String levelName = level != null ? level.getName() : "未知层级";
            String primaryName = primary != null ? primary.getName() : "未知维度";

            levelGroups.computeIfAbsent(levelName, k -> new LinkedHashMap<>());
            levelGroups.get(levelName).computeIfAbsent(primaryName, k -> new ArrayList<>());

            Map<String, Object> indicatorInfo = new LinkedHashMap<>();
            indicatorInfo.put("id", indicator.getId());
            indicatorInfo.put("code", indicator.getCode());
            indicatorInfo.put("name", indicator.getName());
            indicatorInfo.put("unit", indicator.getUnit());
            indicatorInfo.put("direction", indicator.getScoreDirection() != null ?
                    indicator.getScoreDirection().name() : "POSITIVE");
            indicatorInfo.put("metricType", indicator.getMetricType() != null ?
                    indicator.getMetricType().name() : "QUANTITATIVE");
            // 添加平均数
            indicatorInfo.put("averageValue", indicator.getAverageValue() != null ?
                    indicator.getAverageValue().doubleValue() : null);

            levelGroups.get(levelName).get(primaryName).add(indicatorInfo);
            totalCount++;
        }

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
     * 全局模拟：生成指定数量的作战数据
     * @param batchId 批次ID
     * @param templateId 模板ID
     * @param templateName 模板名称
     * @param count 生成数量
     * @param mode 模式: COVER-覆盖当前批次, APPEND-追加到当前批次
     * @param dispersion 离散度（0-1），表示数据围绕平均值的波动范围
     *                   0表示所有值都等于平均值
     *                   0.2表示值在[平均值-20%, 平均值+20%]范围内波动
     */
    @Transactional
    public Map<String, Object> globalSimulate(String batchId, Long templateId, String templateName,
                                               int count, String mode, double dispersion) {
        // 限制离散度范围 [0, 1]
        if (dispersion < 0) dispersion = 0;
        if (dispersion > 1) dispersion = 1;

        List<DynamicDimension> qtIndicators = getAllIndicators(templateId);

        // 获取一级维度信息
        List<DynamicDimension> primaryDims = dimensionRepository.findByTemplateIdAndDimensionLevelOrderBySortOrder(
                templateId, DimensionLevel.PRIMARY);
        Map<Long, DynamicDimension> primaryDimMap = primaryDims.stream()
                .collect(Collectors.toMap(DynamicDimension::getId, d -> d));

        // 获取层级信息
        List<DynamicDimension> levelDims = dimensionRepository.findByTemplateIdAndDimensionLevelOrderBySortOrder(
                templateId, DimensionLevel.LEVEL);
        Map<Long, DynamicDimension> levelDimMap = levelDims.stream()
                .collect(Collectors.toMap(DynamicDimension::getId, d -> d));

        // 根据模式处理已有数据
        if ("COVER".equals(mode)) {
            // 覆盖模式：删除该批次的原始数据（保留归一化结果）
            jdbcTemplate.update("DELETE FROM dynamic_qt_record WHERE batch_id = ? AND normalization_name IS NULL", batchId);
        }
        // APPEND模式：不删除已有数据，直接追加

        // 获取当前已有作战数量
        String countSql = "SELECT COUNT(DISTINCT operation_id) FROM dynamic_qt_record WHERE batch_id = ?";
        int existingCount = jdbcTemplate.queryForObject(countSql, Integer.class, batchId);

        // 生成指定数量的作战ID
        List<String> operationIds = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String opId = "OP_" + String.format("%03d", existingCount + i);
            operationIds.add(opId);
        }

        // 为新作战创建记录结构（不存储 average_value，模拟时从内存获取）
        String insertSql = "INSERT INTO dynamic_qt_record " +
                "(batch_id, template_id, template_name, operation_id, secondary_code, secondary_name, " +
                "level_name, primary_dimension, direction, metric_type) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        for (String opId : operationIds) {
            for (DynamicDimension indicator : qtIndicators) {
                // 获取一级维度和层级信息
                Long parentId = indicator.getParentId();
                DynamicDimension primary = primaryDimMap.get(parentId);
                DynamicDimension level = primary != null ? levelDimMap.get(primary.getParentId()) : null;

                String metricType = indicator.getMetricType() != null ?
                        indicator.getMetricType().name() : "QUANTITATIVE";
                String direction = indicator.getScoreDirection() != null ?
                        indicator.getScoreDirection().name() : "POSITIVE";

                jdbcTemplate.update(insertSql,
                        batchId, templateId, templateName, opId,
                        indicator.getCode(), indicator.getName(),
                        level != null ? level.getName() : "", primary != null ? primary.getName() : "",
                        direction, metricType);
            }
        }

        // 模拟所有单元格的随机值（根据平均值和离散度）
        int simulatedCount = 0;
        for (String opId : operationIds) {
            for (DynamicDimension indicator : qtIndicators) {
                String code = indicator.getCode();
                double value = generateSimulatedValue(indicator, dispersion);

                String updateSql = "UPDATE dynamic_qt_record " +
                        "SET raw_value = ?, is_simulated = TRUE, updated_at = NOW() " +
                        "WHERE batch_id = ? AND operation_id = ? AND secondary_code = ? AND normalization_name IS NULL";
                jdbcTemplate.update(updateSql, value, batchId, opId, code);
                simulatedCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("mode", mode);
        result.put("dispersion", dispersion);
        result.put("simulatedCount", simulatedCount);
        result.put("operationCount", operationIds.size());
        result.put("operationIds", operationIds);
        return result;
    }

    /**
     * 根据平均值和离散度生成模拟值
     * @param indicator 指标信息
     * @param dispersion 离散度（0-1），表示数据围绕平均值的波动比例
     * @return 模拟值（在原始尺度上围绕平均值波动，使用均匀分布确保真正的随机性）
     */
    private double generateSimulatedValue(DynamicDimension indicator, double dispersion) {
        BigDecimal averageValue = indicator.getAverageValue();

        // 如果没有平均值，设置默认平均值为50（用于归一化范围）
        double avg = (averageValue != null) ? averageValue.doubleValue() : 50.0;

        if (dispersion <= 0) {
            // 如果离散度为0，返回平均值
            return Math.round(avg * 1000) / 1000.0;
        }

        // 有平均值的情况：根据离散度生成围绕平均值波动的值
        // 计算波动范围 = 平均值 * 离散度
        double range = Math.abs(avg) * dispersion;
        if (range < 1) range = 1; // 最小波动1

        // 使用均匀分布生成 [avg - range, avg + range] 范围内的随机值
        double minVal = avg - range;
        double maxVal = avg + range;
        if (minVal < 0) minVal = 0; // 确保不为负

        // 真正的随机数：均匀分布
        double random = minVal + Math.random() * (maxVal - minVal);

        return Math.round(random * 1000) / 1000.0;
    }

    /**
     * 全局模拟（兼容旧调用，无离散度参数）
     */
    public Map<String, Object> globalSimulate(String batchId, Long templateId, String templateName,
                                               int count, String mode) {
        // 默认20%离散度
        return globalSimulate(batchId, templateId, templateName, count, mode, 0.2);
    }
    
    /**
     * 获取定量指标统计信息（用于模拟配置）
     * 返回各指标的名称、平均值等
     */
    public Map<String, Object> getIndicatorStats(Long templateId) {
        List<DynamicDimension> qtIndicators = dimensionRepository.findQuantitativeIndicators(templateId);
        
        List<Map<String, Object>> indicators = new ArrayList<>();
        for (DynamicDimension indicator : qtIndicators) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", indicator.getCode());
            item.put("name", indicator.getName());
            
            // 获取平均值，如果为null则返回50（默认归一化范围）
            BigDecimal avg = indicator.getAverageValue();
            double averageValue = (avg != null) ? avg.doubleValue() : 50.0;
            item.put("averageValue", averageValue);
            
            // 获取一级维度和层级信息
            if (indicator.getParentId() != null) {
                DynamicDimension primary = dimensionRepository.findById(indicator.getParentId()).orElse(null);
                if (primary != null) {
                    item.put("primaryName", primary.getName());
                    if (primary.getParentId() != null) {
                        DynamicDimension level = dimensionRepository.findById(primary.getParentId()).orElse(null);
                        if (level != null) {
                            item.put("levelName", level.getName());
                        }
                    }
                }
            }
            
            item.put("direction", indicator.getScoreDirection() != null ? indicator.getScoreDirection().name() : "POSITIVE");
            
            indicators.add(item);
        }
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("templateId", templateId);
        result.put("indicators", indicators);
        result.put("indicatorCount", indicators.size());
        
        return result;
    }
    
    /**
     * 全局模拟（使用独立离散度）
     * @param dispersions 各指标的独立离散度，格式: {指标code: 离散度}
     */
    @Transactional
    public Map<String, Object> globalSimulateWithDispersions(String batchId, Long templateId, String templateName,
                                                            int count, String mode, Map<String, Double> dispersions) {
        // 验证所有离散度范围
        for (Map.Entry<String, Double> entry : dispersions.entrySet()) {
            double d = entry.getValue();
            if (d < 0) entry.setValue(0.0);
            if (d > 1) entry.setValue(1.0);
        }

        List<DynamicDimension> qtIndicators = getAllIndicators(templateId);

        // 获取一级维度信息
        List<DynamicDimension> primaryDims = dimensionRepository.findByTemplateIdAndDimensionLevelOrderBySortOrder(
                templateId, DimensionLevel.PRIMARY);
        Map<Long, DynamicDimension> primaryDimMap = primaryDims.stream()
                .collect(Collectors.toMap(DynamicDimension::getId, d -> d));

        // 获取层级信息
        List<DynamicDimension> levelDims = dimensionRepository.findByTemplateIdAndDimensionLevelOrderBySortOrder(
                templateId, DimensionLevel.LEVEL);
        Map<Long, DynamicDimension> levelDimMap = levelDims.stream()
                .collect(Collectors.toMap(DynamicDimension::getId, d -> d));

        // 根据模式处理已有数据
        if ("COVER".equals(mode)) {
            jdbcTemplate.update("DELETE FROM dynamic_qt_record WHERE batch_id = ? AND normalization_name IS NULL", batchId);
        }

        // 获取当前已有作战数量
        String countSql = "SELECT COUNT(DISTINCT operation_id) FROM dynamic_qt_record WHERE batch_id = ?";
        int existingCount = jdbcTemplate.queryForObject(countSql, Integer.class, batchId);

        // 生成指定数量的作战ID
        List<String> operationIds = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String opId = "OP_" + String.format("%03d", existingCount + i);
            operationIds.add(opId);
        }

        // 为新作战创建记录结构
        String insertSql = "INSERT INTO dynamic_qt_record " +
                "(batch_id, template_id, template_name, operation_id, secondary_code, secondary_name, " +
                "level_name, primary_dimension, direction, metric_type) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        for (String opId : operationIds) {
            for (DynamicDimension indicator : qtIndicators) {
                Long parentId = indicator.getParentId();
                DynamicDimension primary = primaryDimMap.get(parentId);
                DynamicDimension level = primary != null ? levelDimMap.get(primary.getParentId()) : null;

                String metricType = indicator.getMetricType() != null ?
                        indicator.getMetricType().name() : "QUANTITATIVE";
                String direction = indicator.getScoreDirection() != null ?
                        indicator.getScoreDirection().name() : "POSITIVE";

                jdbcTemplate.update(insertSql,
                        batchId, templateId, templateName, opId,
                        indicator.getCode(), indicator.getName(),
                        level != null ? level.getName() : "", primary != null ? primary.getName() : "",
                        direction, metricType);
            }
        }

        // 模拟所有单元格的随机值（根据各指标独立离散度）
        int simulatedCount = 0;
        for (String opId : operationIds) {
            for (DynamicDimension indicator : qtIndicators) {
                String code = indicator.getCode();
                // 获取该指标的独立离散度，如果不存在则使用默认0.2
                double dispersion = dispersions.getOrDefault(code, 0.2);
                double value = generateSimulatedValue(indicator, dispersion);

                String updateSql = "UPDATE dynamic_qt_record SET raw_value = ?, is_simulated = 1 " +
                        "WHERE batch_id = ? AND operation_id = ? AND secondary_code = ?";
                int updated = jdbcTemplate.update(updateSql, value, batchId, opId, code);
                if (updated > 0) simulatedCount++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("batchId", batchId);
        result.put("operationCount", count);
        result.put("simulatedCount", simulatedCount);
        result.put("mode", mode);
        
        return result;
    }

    /**
     * 获取原始评估数据（转置表格）
     */
    public Map<String, Object> getEvaluationRecords(String batchId) {
        String opsSql = "SELECT DISTINCT operation_id FROM dynamic_qt_record " +
                "WHERE batch_id = ? AND normalization_name IS NULL ORDER BY operation_id";
        List<String> operationIds = jdbcTemplate.queryForList(opsSql, String.class, batchId);

        String dataSql = "SELECT operation_id, secondary_code, raw_value, is_simulated, " +
                "metric_type, qualitative_level, direction " +
                "FROM dynamic_qt_record WHERE batch_id = ? AND normalization_name IS NULL";
        List<Map<String, Object>> allData = jdbcTemplate.queryForList(dataSql, batchId);

        return buildTableData(batchId, operationIds, allData, "raw");
    }

    /**
     * 获取归一化结果数据
     */
    public Map<String, Object> getNormalizationRecords(String batchId) {
        // 获取最新的归一化结果
        String latestNormSql = "SELECT normalization_name, MAX(updated_at) as max_updated " +
                "FROM dynamic_qt_record " +
                "WHERE batch_id = ? AND normalization_name IS NOT NULL " +
                "GROUP BY normalization_name ORDER BY max_updated DESC LIMIT 1";
        String normalizationName = null;
        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(latestNormSql, batchId);
            if (results != null && !results.isEmpty()) {
                normalizationName = (String) results.get(0).get("normalization_name");
            }
        } catch (Exception e) {
            // 没有归一化结果
        }

        if (normalizationName == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("batchId", batchId);
            result.put("operationIds", new ArrayList<>());
            result.put("tableData", new ArrayList<>());
            return result;
        }

        String opsSql = "SELECT DISTINCT operation_id FROM dynamic_qt_record " +
                "WHERE batch_id = ? AND normalization_name = ? ORDER BY operation_id";
        List<String> operationIds = jdbcTemplate.queryForList(opsSql, String.class, batchId, normalizationName);

        String dataSql = "SELECT operation_id, secondary_code, normalized_value, direction " +
                "FROM dynamic_qt_record WHERE batch_id = ? AND normalization_name = ?";
        List<Map<String, Object>> allData = jdbcTemplate.queryForList(dataSql, batchId, normalizationName);

        Map<String, Object> result = buildTableData(batchId, operationIds, allData, "normalized");
        result.put("normalizationName", normalizationName);
        return result;
    }

    /**
     * 构建表格数据
     */
    private Map<String, Object> buildTableData(String batchId, List<String> operationIds,
                                                List<Map<String, Object>> allData, String dataType) {
        Map<String, Map<String, Object>> dataByOp = new LinkedHashMap<>();
        Map<String, String> metricTypeByCode = new HashMap<>();

        for (String opId : operationIds) {
            dataByOp.put(opId, new LinkedHashMap<>());
        }

        for (Map<String, Object> row : allData) {
            String opId = (String) row.get("operation_id");
            String code = (String) row.get("secondary_code");

            if ("normalized".equals(dataType)) {
                BigDecimal normalizedValue = (BigDecimal) row.get("normalized_value");
                if (dataByOp.get(opId) != null && normalizedValue != null) {
                    dataByOp.get(opId).put(code, normalizedValue.doubleValue());
                }
            } else {
                BigDecimal rawValue = (BigDecimal) row.get("raw_value");
                Boolean isSimulated = (Boolean) row.get("is_simulated");
                String metricType = (String) row.get("metric_type");
                String qualitativeLevel = (String) row.get("qualitative_level");

                if (metricType != null) {
                    metricTypeByCode.put(code, metricType);
                }

                if (dataByOp.get(opId) != null) {
                    dataByOp.get(opId).put(code, rawValue != null ? rawValue.doubleValue() : null);
                    dataByOp.get(opId).put(code + "_simulated", isSimulated != null ? isSimulated : false);
                    if (qualitativeLevel != null) {
                        dataByOp.get(opId).put(code + "_level", qualitativeLevel);
                    }
                }
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
        result.put("metricTypes", metricTypeByCode);

        if ("normalized".equals(dataType)) {
            Map<String, Object> indicators = getIndicatorsByBatch(batchId, null);
            result.put("levels", indicators.get("levels"));
        }

        return result;
    }

    /**
     * 模拟单个单元格
     */
    @Transactional
    public Double simulateCell(String batchId, String operationId, String secondaryCode) {
        double value = Math.round(Math.random() * 1000) / 1000.0;

        String sql = "UPDATE dynamic_qt_record " +
                "SET raw_value = ?, is_simulated = TRUE, updated_at = NOW() " +
                "WHERE batch_id = ? AND operation_id = ? AND secondary_code = ? AND normalization_name IS NULL";
        jdbcTemplate.update(sql, value, batchId, operationId, secondaryCode);

        return value;
    }

    /**
     * 保存单条记录
     */
    @Transactional
    public void saveRecord(Map<String, Object> request) {
        String batchId = (String) request.get("batchId");
        String operationId = (String) request.get("operationId");
        String secondaryCode = (String) request.get("secondaryCode");
        String level = (String) request.get("level");

        Double value;
        String sql;

        if (level != null && !level.isEmpty()) {
            value = convertQualitativeToValue(level);
            sql = "UPDATE dynamic_qt_record " +
                    "SET raw_value = ?, qualitative_level = ?, is_simulated = FALSE, updated_at = NOW() " +
                    "WHERE batch_id = ? AND operation_id = ? AND secondary_code = ? AND normalization_name IS NULL";
            jdbcTemplate.update(sql, value, level, batchId, operationId, secondaryCode);
        } else {
            value = ((Number) request.get("value")).doubleValue();
            sql = "UPDATE dynamic_qt_record " +
                    "SET raw_value = ?, is_simulated = FALSE, updated_at = NOW() " +
                    "WHERE batch_id = ? AND operation_id = ? AND secondary_code = ? AND normalization_name IS NULL";
            jdbcTemplate.update(sql, value, batchId, operationId, secondaryCode);
        }
    }

    private Double convertQualitativeToValue(String level) {
        if (level == null) return null;
        switch (level) {
            case "EXCELLENT": return 1.0;
            case "GOOD": return 0.75;
            case "FAIR": return 0.5;
            case "POOR": return 0.25;
            default: return null;
        }
    }

    // ==================== 归一化管理 ====================

    /**
     * 检查是否存在归一化结果
     */
    public boolean hasNormalization(String batchId) {
        String sql = "SELECT COUNT(*) FROM dynamic_qt_record WHERE batch_id = ? AND normalization_name IS NOT NULL LIMIT 1";
        int count = jdbcTemplate.queryForObject(sql, Integer.class, batchId);
        return count > 0;
    }

    /**
     * 获取归一化名称（最新的）
     */
    public String getNormalizationName(String batchId) {
        String sql = "SELECT normalization_name FROM dynamic_qt_record " +
                "WHERE batch_id = ? AND normalization_name IS NOT NULL " +
                "ORDER BY MAX(updated_at) DESC LIMIT 1";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, batchId);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 执行归一化（直接覆盖当前批次的归一化结果）
     * 不再保留多次归一化历史，每次执行都是覆盖
     */
    @Transactional
    public Map<String, Object> createNormalization(String batchId) {
        // 获取所有原始数据（从原始记录中获取元数据，去重）
        String rawDataSql = "SELECT DISTINCT operation_id, secondary_code, secondary_name, raw_value, direction, " +
                "template_id, template_name, level_name, primary_dimension " +
                "FROM dynamic_qt_record " +
                "WHERE batch_id = ? AND normalization_name IS NULL AND raw_value IS NOT NULL " +
                "ORDER BY operation_id, secondary_code";
        List<Map<String, Object>> rawData = jdbcTemplate.queryForList(rawDataSql, batchId);

        if (rawData.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "没有可归一化的原始数据");
            return result;
        }

        // 按指标分组，计算 min/max
        Map<String, List<Double>> valuesByCode = new HashMap<>();
        for (Map<String, Object> row : rawData) {
            String code = (String) row.get("secondary_code");
            BigDecimal val = (BigDecimal) row.get("raw_value");
            if (val != null) {
                valuesByCode.computeIfAbsent(code, k -> new ArrayList<>()).add(val.doubleValue());
            }
        }

        Map<String, Double> minMap = new HashMap<>();
        Map<String, Double> maxMap = new HashMap<>();
        for (Map.Entry<String, List<Double>> entry : valuesByCode.entrySet()) {
            List<Double> vals = entry.getValue();
            minMap.put(entry.getKey(), Collections.min(vals));
            maxMap.put(entry.getKey(), Collections.max(vals));
        }

        // 生成归一化名称（基于时间戳）
        String normalizationName = "归一化_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        // 删除旧的归一化结果
        jdbcTemplate.update("DELETE FROM dynamic_qt_record WHERE batch_id = ? AND normalization_name IS NOT NULL", batchId);

        // 使用独立的 INSERT 语句
        String insertSql = "INSERT INTO dynamic_qt_record " +
                "(batch_id, template_id, template_name, operation_id, secondary_code, secondary_name, " +
                "level_name, primary_dimension, raw_value, direction, normalization_name, normalized_value) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int normalizedCount = 0;

        for (Map<String, Object> row : rawData) {
            String opId = (String) row.get("operation_id");
            String code = (String) row.get("secondary_code");
            String secondaryName = (String) row.get("secondary_name");
            Double rawValue = ((BigDecimal) row.get("raw_value")).doubleValue();
            String direction = (String) row.get("direction");
            if (direction == null) {
                direction = "POSITIVE";
            }

            // 获取元数据
            Long templateId = row.get("template_id") != null ?
                    ((Number) row.get("template_id")).longValue() : null;
            String templateName = (String) row.get("template_name");
            String levelName = (String) row.get("level_name");
            String primaryDimension = (String) row.get("primary_dimension");

            Double normalizedValue = null;
            Double min = minMap.get(code);
            Double max = maxMap.get(code);

            if (min != null && max != null && !min.equals(max)) {
                if ("POSITIVE".equals(direction)) {
                    normalizedValue = (rawValue - min) / (max - min);
                } else {
                    normalizedValue = (max - rawValue) / (max - min);
                }
                normalizedValue = Math.max(0, Math.min(1, normalizedValue));
                normalizedValue = Math.round(normalizedValue * 1000.0) / 1000.0;
            }

            if (normalizedValue != null) {
                // 检查是否已存在（防止重复插入）
                String checkSql = "SELECT COUNT(*) FROM dynamic_qt_record " +
                        "WHERE batch_id = ? AND operation_id = ? AND secondary_code = ? AND normalization_name = ?";
                int exists = jdbcTemplate.queryForObject(checkSql, Integer.class,
                        batchId, opId, code, normalizationName);
                if (exists > 0) {
                    // 更新已有记录
                    String updateSql = "UPDATE dynamic_qt_record SET normalized_value = ? " +
                            "WHERE batch_id = ? AND operation_id = ? AND secondary_code = ? AND normalization_name = ?";
                    jdbcTemplate.update(updateSql, normalizedValue, batchId, opId, code, normalizationName);
                } else {
                    // 直接插入
                    jdbcTemplate.update(insertSql,
                            batchId, templateId, templateName, opId,
                            code, secondaryName,
                            levelName, primaryDimension,
                            rawValue, direction, normalizationName, normalizedValue);
                }
                normalizedCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("normalizationName", normalizationName);
        result.put("normalizedCount", normalizedCount);

        int operationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT operation_id) FROM dynamic_qt_record WHERE batch_id = ? AND normalization_name IS NOT NULL",
                Integer.class, batchId);
        result.put("operationCount", operationCount);

        return result;
    }

    /**
     * 删除归一化结果
     */
    @Transactional
    public void deleteNormalization(String batchId) {
        jdbcTemplate.update("DELETE FROM dynamic_qt_record WHERE batch_id = ? AND normalization_name IS NOT NULL", batchId);
    }

    // ==================== 兼容旧API ====================

    @Transactional
    public Map<String, Object> globalSimulate(String batchId, Long templateId, String templateName, int count) {
        return globalSimulate(batchId, templateId, templateName, count, "APPEND");
    }

    public Map<String, Object> getNormalizedRecords(String batchId) {
        return getNormalizationRecords(batchId);
    }

    public int deleteBatch(String batchId) {
        deleteEvaluationBatch(batchId);
        return 1;
    }

    /**
     * 归一化（兼容旧API）
     */
    @Transactional
    public Map<String, Object> normalize(String batchId) {
        return createNormalization(batchId);
    }

    public List<String> getNormalizationNames(String batchId) {
        String sql = "SELECT normalization_name FROM dynamic_qt_record " +
                "WHERE batch_id = ? AND normalization_name IS NOT NULL " +
                "GROUP BY normalization_name ORDER BY MAX(created_at) DESC";
        return jdbcTemplate.queryForList(sql, String.class, batchId);
    }

    /**
     * 获取归一化批次列表（带详细信息）
     */
    public List<Map<String, Object>> getNormalizationBatches(String batchId) {
        String sql = "SELECT normalization_name, COUNT(DISTINCT operation_id) as operation_count, " +
                "COUNT(DISTINCT secondary_code) as indicator_count, MAX(created_at) as created_at " +
                "FROM dynamic_qt_record WHERE batch_id = ? AND normalization_name IS NOT NULL " +
                "GROUP BY normalization_name ORDER BY created_at DESC";

        return jdbcTemplate.queryForList(sql, batchId);
    }

    /**
     * 创建归一化（兼容旧API，支持自定义名称）
     */
    @Transactional
    public Map<String, Object> createNormalization(String batchId, String normalizationName, String description) {
        return createNormalization(batchId);
    }
}
