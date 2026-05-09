package com.ccnu.military.service;

import com.ccnu.military.entity.*;
import com.ccnu.military.entity.DynamicQlRecord.ScoreLevel;
import com.ccnu.military.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 动态定性评估服务
 * 核心功能：
 * 1. 获取定性指标结构（动态配置）
 * 2. 管理评估记录
 * 3. 执行集结计算
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicQlService {

    private final JdbcTemplate jdbcTemplate;
    private final DynamicQlRecordRepository qlRecordRepository;
    private final DynamicQlSessionRepository qlSessionRepository;
    private final DynamicQlAggregationRepository qlAggregationRepository;
    private final DynamicDimensionRepository dimensionRepository;
    private final DynamicTemplateRepository templateRepository;
    private final ExpertBaseInfoRepository expertRepository;
    private final ObjectMapper objectMapper;

    // ==================== 指标结构获取（动态化） ====================

    /**
     * 获取定性指标列表（按层级分组）
     * 完全动态化：从 dynamic_dimension 表查询
     */
    public Map<String, Object> getQualitativeIndicators(Long templateId) {
        // 获取模板信息
        DynamicTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));

        // 获取层级信息
        List<DynamicDimension> levelDims = dimensionRepository.findByTemplateIdAndDimensionLevelOrderBySortOrder(
                templateId, DynamicDimension.DimensionLevel.LEVEL);
        Map<Long, DynamicDimension> levelDimMap = levelDims.stream()
                .collect(Collectors.toMap(DynamicDimension::getId, d -> d));

        // 获取一级维度
        List<DynamicDimension> primaryDims = dimensionRepository.findByTemplateIdAndDimensionLevelOrderBySortOrder(
                templateId, DynamicDimension.DimensionLevel.PRIMARY);
        Map<Long, DynamicDimension> primaryDimMap = primaryDims.stream()
                .collect(Collectors.toMap(DynamicDimension::getId, d -> d));

        // 获取定性指标（二级维度，metric_type = QUALITATIVE）
        List<DynamicDimension> qlIndicators = dimensionRepository.findQualitativeIndicators(templateId);

        // 按层级分组
        Map<String, Map<String, List<Map<String, Object>>>> levelPrimarySecondaries = new LinkedHashMap<>();
        int totalIndicators = 0;

        for (DynamicDimension indicator : qlIndicators) {
            // 获取一级维度和层级信息
            Long parentId = indicator.getParentId();
            DynamicDimension primary = primaryDimMap.get(parentId);
            DynamicDimension level = primary != null ? levelDimMap.get(primary.getParentId()) : null;

            String levelName = level != null ? level.getName() : "未分类";
            String primaryName = primary != null ? primary.getName() : "未分类";

            levelPrimarySecondaries.computeIfAbsent(levelName, k -> new LinkedHashMap<>());
            levelPrimarySecondaries.get(levelName).computeIfAbsent(primaryName, k -> new ArrayList<>());

            Map<String, Object> indicatorInfo = new LinkedHashMap<>();
            indicatorInfo.put("code", indicator.getCode());
            indicatorInfo.put("name", indicator.getName());
            indicatorInfo.put("description", indicator.getDescription());
            indicatorInfo.put("direction", indicator.getScoreDirection() != null ?
                    indicator.getScoreDirection().name() : "POSITIVE");
            indicatorInfo.put("weight", indicator.getWeight());
            indicatorInfo.put("primaryId", primary != null ? primary.getId() : null);
            indicatorInfo.put("primaryName", primaryName);
            indicatorInfo.put("levelName", levelName);

            levelPrimarySecondaries.get(levelName).get(primaryName).add(indicatorInfo);
            totalIndicators++;
        }

        // 构建层级列表（只包含有定性指标的层级）
        List<Map<String, Object>> levels = new ArrayList<>();
        int levelCount = 0;
        int primaryCount = 0;

        for (DynamicDimension levelDim : levelDims) {
            String levelName = levelDim.getName();
            
            // 检查该层级是否有定性指标
            Map<String, List<Map<String, Object>>> primaryMap = levelPrimarySecondaries.getOrDefault(levelName, new LinkedHashMap<>());
            
            // 如果该层级没有任何定性指标，跳过
            boolean hasIndicators = primaryMap.values().stream()
                    .anyMatch(list -> list != null && !list.isEmpty());
            if (!hasIndicators) {
                continue;
            }

            Map<String, Object> levelData = new LinkedHashMap<>();
            levelData.put("levelName", levelName);
            levelData.put("levelId", levelDim.getId());
            levelData.put("levelCode", levelDim.getCode());

            List<Map<String, Object>> primaries = new ArrayList<>();
            
            for (Map.Entry<String, List<Map<String, Object>>> entry : primaryMap.entrySet()) {
                // 跳过没有指标的维度
                if (entry.getValue() == null || entry.getValue().isEmpty()) {
                    continue;
                }
                
                Map<String, Object> primaryData = new LinkedHashMap<>();
                primaryData.put("primaryName", entry.getKey());
                primaryData.put("secondaries", entry.getValue());
                primaries.add(primaryData);
                primaryCount++;
            }

            // 只有当该层级有一级维度时才添加
            if (!primaries.isEmpty()) {
                levelData.put("primaries", primaries);
                levels.add(levelData);
                levelCount++;
            }
        }

        // 构建结果
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("templateId", templateId);
        result.put("templateName", template.getTemplateName());
        result.put("levels", levels);
        result.put("levelCount", levelCount);
        result.put("primaryCount", primaryCount);
        result.put("indicatorCount", totalIndicators);

        return result;
    }

    /**
     * 获取所有层级名称列表
     */
    public List<String> getLevelNames(Long templateId) {
        List<DynamicDimension> levelDims = dimensionRepository.findByTemplateIdAndDimensionLevelOrderBySortOrder(
                templateId, DynamicDimension.DimensionLevel.LEVEL);
        return levelDims.stream().map(DynamicDimension::getName).collect(Collectors.toList());
    }

    /**
     * 获取某层级的指标列表
     */
    public List<DynamicDimension> getIndicatorsByLevel(Long templateId, String levelName) {
        // 获取该层级下的一级维度
        List<DynamicDimension> levelDims = dimensionRepository.findByTemplateIdAndDimensionLevelOrderBySortOrder(
                templateId, DynamicDimension.DimensionLevel.LEVEL);
        DynamicDimension levelDim = levelDims.stream()
                .filter(d -> d.getName().equals(levelName))
                .findFirst()
                .orElse(null);

        if (levelDim == null) {
            return Collections.emptyList();
        }

        // 获取该层级下的一级维度ID列表
        List<DynamicDimension> primaryDims = dimensionRepository.findByTemplateIdAndDimensionLevelOrderBySortOrder(
                templateId, DynamicDimension.DimensionLevel.PRIMARY);
        List<Long> primaryIds = primaryDims.stream()
                .filter(p -> p.getParentId() != null && p.getParentId().equals(levelDim.getId()))
                .map(DynamicDimension::getId)
                .collect(Collectors.toList());

        // 获取定性指标
        List<DynamicDimension> allQlIndicators = dimensionRepository.findQualitativeIndicators(templateId);
        return allQlIndicators.stream()
                .filter(ind -> {
                    Long parentId = ind.getParentId();
                    return parentId != null && primaryIds.contains(parentId);
                })
                .collect(Collectors.toList());
    }

    // ==================== 批次和作战管理 ====================

    /**
     * 获取可用的定量评估批次列表
     */
    public List<Map<String, Object>> getAvailableBatches(Long templateId) {
        String sql = "SELECT DISTINCT batch_id, MAX(template_id) as template_id, MAX(template_name) as template_name, " +
                "MAX(created_at) as created_at " +
                "FROM dynamic_qt_record WHERE batch_id IS NOT NULL ";

        if (templateId != null) {
            sql += "AND template_id = ? ";
            sql += "GROUP BY batch_id ORDER BY created_at DESC";
            return jdbcTemplate.queryForList(sql, templateId);
        } else {
            sql += "GROUP BY batch_id ORDER BY created_at DESC";
            return jdbcTemplate.queryForList(sql);
        }
    }

    /**
     * 获取批次下的作战列表
     */
    public List<String> getOperationsByBatch(String batchId) {
        // 优先从定量记录表获取
        String sqlQt = "SELECT DISTINCT operation_id FROM dynamic_qt_record " +
                "WHERE batch_id = ? ORDER BY operation_id";
        List<String> operations = jdbcTemplate.queryForList(sqlQt, String.class, batchId);

        // 如果定量表没有，从定性记录表获取
        if (operations.isEmpty()) {
            String sqlQl = "SELECT DISTINCT operation_id FROM dynamic_ql_record " +
                    "WHERE batch_id = ? ORDER BY operation_id";
            operations = jdbcTemplate.queryForList(sqlQl, String.class, batchId);
        }

        return operations;
    }

    /**
     * 获取作战的定量参考数据
     */
    public Map<String, Object> getReferenceData(String batchId, String operationId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("operationId", operationId);

        // 获取原始数据
        String rawSql = "SELECT secondary_code, secondary_name, raw_value, direction " +
                "FROM dynamic_qt_record WHERE batch_id = ? AND operation_id = ? AND normalization_name IS NULL";
        List<Map<String, Object>> rawData = jdbcTemplate.queryForList(rawSql, batchId, operationId);

        // 获取归一化数据
        String normSql = "SELECT secondary_code, normalized_value " +
                "FROM dynamic_qt_record WHERE batch_id = ? AND operation_id = ? AND normalization_name IS NOT NULL";
        List<Map<String, Object>> normData = jdbcTemplate.queryForList(normSql, batchId, operationId);
        Map<String, Double> normMap = new HashMap<>();
        for (Map<String, Object> row : normData) {
            String code = (String) row.get("secondary_code");
            BigDecimal val = (BigDecimal) row.get("normalized_value");
            if (val != null) {
                normMap.put(code, val.doubleValue());
            }
        }

        // 合并数据
        List<Map<String, Object>> quantitativeData = new ArrayList<>();
        for (Map<String, Object> row : rawData) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", row.get("secondary_code"));
            item.put("name", row.get("secondary_name"));
            BigDecimal rawVal = (BigDecimal) row.get("raw_value");
            item.put("rawValue", rawVal != null ? rawVal.doubleValue() : null);
            item.put("normalizedValue", normMap.get(row.get("secondary_code")));
            item.put("direction", row.get("direction"));
            quantitativeData.add(item);
        }

        result.put("quantitativeData", quantitativeData);
        return result;
    }

    // ==================== 专家管理 ====================

    /**
     * 获取可选专家列表
     * 从 expert_base_info 和 expert_credibility_evaluation_score 表获取
     */
    public List<Map<String, Object>> getAvailableExperts() {
        String sql = "SELECT e.expert_id, e.expert_name, e.work_unit, e.title, e.department, " +
                "cs.total_score as credibility, cs.credibility_level " +
                "FROM expert_base_info e " +
                "LEFT JOIN expert_credibility_evaluation_score cs ON e.expert_id = cs.expert_id " +
                "WHERE e.status = 1 " +
                "ORDER BY cs.total_score DESC";

        List<Map<String, Object>> experts = jdbcTemplate.queryForList(sql);

        // 补充计算可信度（如果没有可信度数据，使用默认值70）
        for (Map<String, Object> expert : experts) {
            if (expert.get("credibility") == null) {
                expert.put("credibility", 70.0);
            } else {
                // 确保是 Double 类型
                Object cred = expert.get("credibility");
                if (cred instanceof BigDecimal) {
                    expert.put("credibility", ((BigDecimal) cred).doubleValue());
                }
            }
        }

        return experts;
    }

    /**
     * 批量获取专家可信度
     */
    public Map<Long, Double> getExpertsCredibility(List<Long> expertIds) {
        if (expertIds == null || expertIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String placeholders = expertIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT cs.expert_id, COALESCE(cs.total_score, 70) as credibility " +
                "FROM expert_credibility_evaluation_score cs " +
                "WHERE cs.expert_id IN (" + placeholders + ")";

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, expertIds.toArray());
        Map<Long, Double> credibilityMap = new HashMap<>();
        for (Map<String, Object> row : results) {
            Long expertId = ((Number) row.get("expert_id")).longValue();
            Object cred = row.get("credibility");
            Double credibility;
            if (cred instanceof BigDecimal) {
                credibility = ((BigDecimal) cred).doubleValue();
            } else {
                credibility = cred != null ? ((Number) cred).doubleValue() : 70.0;
            }
            credibilityMap.put(expertId, credibility);
        }

        return credibilityMap;
    }

    // ==================== 评估记录管理 ====================

    /**
     * 保存评估记录
     * @param batchId 批次ID
     * @param templateId 模板ID
     * @param expertId 专家ID
     * @param operationId 作战ID
     * @param scores 评分列表 [{code, level(等级代码如A+, B), confidence(把握度0-100), remark}]
     */
    @Transactional
    public void saveEvaluation(String batchId, Long templateId, Long expertId, String operationId,
                               List<Map<String, Object>> scores) {
        // 获取专家信息
        String expertName = null;
        Double credibility = 70.0;
        String sql = "SELECT e.expert_name, cs.total_score as credibility " +
                "FROM expert_base_info e " +
                "LEFT JOIN expert_credibility_evaluation_score cs ON e.expert_id = cs.expert_id " +
                "WHERE e.expert_id = ?";
        try {
            Map<String, Object> expertInfo = jdbcTemplate.queryForMap(sql, expertId);
            expertName = (String) expertInfo.get("expert_name");
            Object cred = expertInfo.get("credibility");
            if (cred != null) {
                if (cred instanceof BigDecimal) {
                    credibility = ((BigDecimal) cred).doubleValue();
                } else {
                    credibility = ((Number) cred).doubleValue();
                }
            }
        } catch (Exception e) {
            log.warn("获取专家信息失败: {}", expertId, e);
        }

        for (Map<String, Object> score : scores) {
            String code = (String) score.get("code");
            String levelCode = (String) score.get("level"); // 等级代码: A+, A, A-, B+, B...

            // 解析等级（15级）
            if (levelCode == null || !DynamicQlRecord.ScoreLevel.getAllCodes().contains(levelCode)) {
                log.warn("无效的评分等级: {}", levelCode);
                continue;
            }

            // 获取等级分值
            double levelValue = DynamicQlRecord.ScoreLevel.getValue(levelCode);

            // 把握度（0-100的数值）
            Object confidenceObj = score.get("confidence");
            double confidenceValue = 80.0; // 默认值
            if (confidenceObj != null) {
                if (confidenceObj instanceof Number) {
                    confidenceValue = ((Number) confidenceObj).doubleValue();
                } else {
                    try {
                        confidenceValue = Double.parseDouble(confidenceObj.toString());
                    } catch (NumberFormatException e) {
                        log.warn("把握度格式错误: {}", confidenceObj);
                    }
                }
            }

            String remark = (String) score.getOrDefault("remark", "");

            // 获取指标信息
            List<DynamicDimension> indicators = dimensionRepository.findByTemplateIdAndDimensionLevel(
                    templateId, DynamicDimension.DimensionLevel.SECONDARY);
            DynamicDimension indicator = indicators.stream()
                    .filter(i -> i.getCode().equals(code))
                    .findFirst()
                    .orElse(null);

            String levelName = "未分类";
            String primaryName = "未分类";
            if (indicator != null) {
                levelName = getLevelNameForIndicator(templateId, indicator);
                primaryName = getPrimaryNameForIndicator(templateId, indicator);
            }

            // 检查是否已存在
            Optional<DynamicQlRecord> existing =
                    qlRecordRepository.findByBatchIdAndExpertIdAndOperationIdAndSecondaryCode(
                            batchId, expertId, operationId, code);

            DynamicQlRecord record;
            if (existing.isPresent()) {
                record = existing.get();
            } else {
                record = new DynamicQlRecord();
                record.setBatchId(batchId);
                record.setTemplateId(templateId);
                record.setExpertId(expertId);
                record.setExpertName(expertName);
                record.setExpertCredibility(BigDecimal.valueOf(credibility));
                record.setOperationId(operationId);
                record.setSecondaryCode(code);
                record.setLevelName(levelName);
                record.setPrimaryDimension(primaryName);
            }

            // 设置评分等级（15级）
            record.setScoreLevel(levelCode);
            record.setScoreValue(BigDecimal.valueOf(levelValue)); // 使用等级中间分值

            // 设置把握度（数值0-100）
            record.setConfidenceValue(BigDecimal.valueOf(confidenceValue));

            record.setEvaluationRemark(remark);
            record.setEvaluatedAt(java.time.LocalDateTime.now());

            qlRecordRepository.save(record);
        }
    }

    /**
     * 获取指标所属的层级名称
     */
    private String getLevelNameForIndicator(Long templateId, DynamicDimension indicator) {
        if (indicator.getParentId() == null) {
            return "未分类";
        }

        DynamicDimension primary = dimensionRepository.findById(indicator.getParentId()).orElse(null);
        if (primary == null || primary.getParentId() == null) {
            return "未分类";
        }

        DynamicDimension level = dimensionRepository.findById(primary.getParentId()).orElse(null);
        return level != null ? level.getName() : "未分类";
    }

    /**
     * 获取指标所属的一级维度名称
     */
    private String getPrimaryNameForIndicator(Long templateId, DynamicDimension indicator) {
        if (indicator.getParentId() == null) {
            return "未分类";
        }

        DynamicDimension primary = dimensionRepository.findById(indicator.getParentId()).orElse(null);
        return primary != null ? primary.getName() : "未分类";
    }

    /**
     * 获取已保存的评估记录
     */
    public Map<String, Object> getEvaluationRecords(String batchId, Long expertId, String operationId) {
        List<DynamicQlRecord> records;

        if (batchId != null && expertId != null && operationId != null) {
            records = qlRecordRepository.findByBatchIdAndExpertIdAndOperationId(batchId, expertId, operationId);
        } else if (batchId != null && expertId != null) {
            records = qlRecordRepository.findByBatchIdAndExpertId(batchId, expertId);
        } else if (batchId != null && operationId != null) {
            records = qlRecordRepository.findByBatchIdAndOperationId(batchId, operationId);
        } else if (batchId != null) {
            records = qlRecordRepository.findByBatchId(batchId);
        } else {
            records = Collections.emptyList();
        }

        // 构建转置表格数据
        Map<String, Map<String, Map<String, Object>>> tableData = new LinkedHashMap<>();

        for (DynamicQlRecord record : records) {
            String opId = record.getOperationId();
            String expId = String.valueOf(record.getExpertId());
            String code = record.getSecondaryCode();

            tableData.computeIfAbsent(opId, k -> new LinkedHashMap<>());
            tableData.get(opId).computeIfAbsent(expId, k -> new LinkedHashMap<>());

            Map<String, Object> cellData = new LinkedHashMap<>();
            String levelCode = record.getScoreLevel();
            cellData.put("level", levelCode);
            cellData.put("levelLabel", DynamicQlRecord.ScoreLevel.getName(levelCode));
            cellData.put("value", record.getScoreValue() != null ? record.getScoreValue().doubleValue() : null);
            cellData.put("confidenceValue", record.getConfidenceValue() != null ? record.getConfidenceValue().doubleValue() : null);
            cellData.put("remark", record.getEvaluationRemark());

            tableData.get(opId).get(expId).put(code, cellData);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batchId", batchId);
        result.put("records", tableData);
        result.put("totalCount", records.size());

        return result;
    }

    /**
     * 获取表格数据（完整结构，用于前端渲染）
     */
    public Map<String, Object> getTableData(String batchId, Long templateId, String levelName,
                                            List<Long> expertIds) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 获取定性指标结构（完整的，不进行层级过滤）
        Map<String, Object> indicatorInfo = getQualitativeIndicators(templateId);
        result.put("templateId", templateId);
        result.put("templateName", indicatorInfo.get("templateName"));

        // 返回完整的层级结构（不过滤），让前端根据 activeLevel 显示
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> allLevels = (List<Map<String, Object>>) indicatorInfo.get("levels");
        result.put("levels", allLevels);

        // 2. 获取作战列表
        List<String> operationIds = batchId != null ? getOperationsByBatch(batchId) : Collections.emptyList();
        // 转换为对象数组格式
        List<Map<String, String>> operations = operationIds.stream()
                .map(id -> {
                    Map<String, String> op = new LinkedHashMap<>();
                    op.put("operationId", id);
                    op.put("operationName", id);
                    return op;
                })
                .collect(Collectors.toList());
        result.put("operations", operations);

        // 3. 获取专家信息
        List<Map<String, Object>> expertList = new ArrayList<>();
        if (expertIds != null && !expertIds.isEmpty()) {
            String placeholders = expertIds.stream().map(id -> "?").collect(Collectors.joining(","));
            String sql = "SELECT e.expert_id, e.expert_name, cs.total_score as credibility " +
                    "FROM expert_base_info e " +
                    "LEFT JOIN expert_credibility_evaluation_score cs ON e.expert_id = cs.expert_id " +
                    "WHERE e.expert_id IN (" + placeholders + ")";
            expertList = jdbcTemplate.queryForList(sql, expertIds.toArray());
        }
        result.put("experts", expertList);

        // 4. 获取已保存的评估记录
        Map<String, Map<String, Map<String, Object>>> tableData = new LinkedHashMap<>();
        if (batchId != null) {
            List<DynamicQlRecord> records = qlRecordRepository.findByBatchId(batchId);
            for (DynamicQlRecord record : records) {
                // 按层级过滤
                if (levelName != null && !levelName.equals(record.getLevelName())) {
                    continue;
                }
                // 按专家过滤
                if (expertIds != null && !expertIds.isEmpty() && !expertIds.contains(record.getExpertId())) {
                    continue;
                }

                String opId = record.getOperationId();
                String expId = String.valueOf(record.getExpertId());
                String code = record.getSecondaryCode();

                tableData.computeIfAbsent(opId, k -> new LinkedHashMap<>());
                tableData.get(opId).computeIfAbsent(expId, k -> new LinkedHashMap<>());

                Map<String, Object> cellData = new LinkedHashMap<>();
                String levelCode = record.getScoreLevel();
                cellData.put("level", levelCode);
                cellData.put("levelLabel", DynamicQlRecord.ScoreLevel.getName(levelCode));
                cellData.put("value", record.getScoreValue() != null ? record.getScoreValue().doubleValue() : null);
                cellData.put("confidenceValue", record.getConfidenceValue() != null ? record.getConfidenceValue().doubleValue() : null);
                cellData.put("remark", record.getEvaluationRemark());

                tableData.get(opId).get(expId).put(code, cellData);
            }
        }
        result.put("tableData", tableData);

        // 5. 计算进度
        // 如果没有指定专家，使用 availableExperts 的数量
        int expertCount = 0;
        if (expertIds != null && !expertIds.isEmpty()) {
            expertCount = expertIds.size();
        } else {
            // 没有指定专家时，使用该批次下所有评估记录中涉及的不同专家数量
            expertCount = countDistinctExperts(batchId, levelName);
        }
        int totalItems = operations.size() * getIndicatorCountForLevel(templateId, levelName) * expertCount;
        int completedItems = countCompletedItems(batchId, levelName, expertIds);
        result.put("progress", Map.of(
                "total", totalItems,
                "completed", completedItems,
                "percentage", totalItems > 0 ? (completedItems * 100.0 / totalItems) : 0
        ));

        return result;
    }

    /**
     * 获取某层级的指标数量
     */
    private int getIndicatorCountForLevel(Long templateId, String levelName) {
        if (levelName == null) {
            return dimensionRepository.findQualitativeIndicators(templateId).size();
        }
        return getIndicatorsByLevel(templateId, levelName).size();
    }

    /**
     * 统计已完成项数
     */
    private int countCompletedItems(String batchId, String levelName, List<Long> expertIds) {
        if (batchId == null) return 0;

        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM dynamic_ql_record WHERE 1=1");
        List<Object> params = new ArrayList<>();

        sql.append(" AND batch_id = ?");
        params.add(batchId);

        if (levelName != null) {
            sql.append(" AND level_name = ?");
            params.add(levelName);
        }

        if (expertIds != null && !expertIds.isEmpty()) {
            String placeholders = expertIds.stream().map(id -> "?").collect(Collectors.joining(","));
            sql.append(" AND expert_id IN (").append(placeholders).append(")");
            params.addAll(expertIds);
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count.intValue() : 0;
    }

    /**
     * 统计不同专家数量（用于进度计算）
     */
    private int countDistinctExperts(String batchId, String levelName) {
        if (batchId == null) return 0;

        StringBuilder sql = new StringBuilder("SELECT COUNT(DISTINCT expert_id) FROM dynamic_ql_record WHERE batch_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(batchId);

        if (levelName != null) {
            sql.append(" AND level_name = ?");
            params.add(levelName);
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count.intValue() : 0;
    }

    // ==================== 集结计算（质心式加权平均，按作战ID单独集结） ====================

    /**
     * 执行集结计算（质心式加权平均）
     * 核心逻辑：按每个作战ID单独进行集结计算
     * 公式：
     *   α_norm = α / 100（权威度归一化）
     *   λ = 把握度 / 100
     *   γ = wα·α_norm + wλ·λ
     *   x* = Σγ·L·中点 / Σγ·L
     *   其中 L = (a2 - a1) 为等级区间长度
     */
    @Transactional
    public Map<String, Object> aggregateScores(String batchId, String operationId, String levelName,
                                               double weightAlpha, double weightLambda) {
        try {
            // 1. 获取等级区间定义
            Map<String, double[]> gradeIntervalMap = buildGradeIntervalMap();

            // 2. 获取所有作战ID（遍历每个作战单独进行集结）
            List<String> allOperations = getOperationsByBatch(batchId);

            if (allOperations.isEmpty()) {
                return Map.of("success", false, "message", "该批次下没有作战数据");
            }

            // 3. 归一化权重
            double[] nw = normalizeAlphaLambdaWeights(weightAlpha, weightLambda);
            double wAlphaNorm = nw[0];
            double wLambdaNorm = nw[1];

            // 4. 存储所有集结结果
            Map<String, List<Map<String, Object>>> resultsByOperation = new LinkedHashMap<>();
            List<Map<String, Object>> allAggregationResults = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            // 5. 对每个作战ID单独进行集结计算
            for (String opId : allOperations) {
                // 获取该作战的所有评估记录
                List<DynamicQlRecord> opRecords = qlRecordRepository.findByBatchIdAndOperationId(batchId, opId);

                if (opRecords.isEmpty()) {
                    continue;
                }

                // 拉取该作战涉及的专家权威度
                Set<Long> opExpertIds = opRecords.stream()
                        .map(DynamicQlRecord::getExpertId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                Map<Long, Double> alphaMap = buildAlphaMap(opExpertIds);

                // 按指标分组计算
                Map<String, List<DynamicQlRecord>> recordsByIndicator = opRecords.stream()
                        .collect(Collectors.groupingBy(r -> r.getSecondaryCode() != null ? r.getSecondaryCode() : "UNKNOWN"));

                List<Map<String, Object>> opResults = new ArrayList<>();

                for (Map.Entry<String, List<DynamicQlRecord>> entry : recordsByIndicator.entrySet()) {
                    String indicatorCode = entry.getKey();
                    List<DynamicQlRecord> indicatorRecords = entry.getValue();

                    // 计算该指标的集结结果
                    Map<String, Object> indicatorResult = computeIndicatorAggregation(
                            indicatorCode, indicatorRecords, alphaMap, gradeIntervalMap, wAlphaNorm, wLambdaNorm, warnings);

                    if (indicatorResult != null) {
                        // 添加作战ID到结果中
                        indicatorResult.put("operationId", opId);
                        opResults.add(indicatorResult);
                        allAggregationResults.add(indicatorResult);
                    }
                }

                if (!opResults.isEmpty()) {
                    resultsByOperation.put(opId, opResults);
                }
            }

            if (allAggregationResults.isEmpty()) {
                return Map.of("success", false, "message", "没有可集结的评估记录");
            }

            // 6. 批量保存所有集结结果到数据库
            for (Map<String, Object> indicatorResult : allAggregationResults) {
                String indicatorCode = (String) indicatorResult.get("indicatorCode");
                String opId = (String) indicatorResult.get("operationId");
                saveAggregationWithNewTransaction(batchId, opId, indicatorCode, indicatorResult, wAlphaNorm, wLambdaNorm);
            }

            // 7. 按层级分组结果
            Map<String, List<Map<String, Object>>> resultsByLevel = new LinkedHashMap<>();
            for (Map<String, Object> indicatorResult : allAggregationResults) {
                String lvlName = (String) indicatorResult.getOrDefault("levelName", "未分类");
                resultsByLevel.computeIfAbsent(lvlName, k -> new ArrayList<>()).add(indicatorResult);
            }

            // 8. 构建层级信息列表
            List<Map<String, Object>> levelInfos = new ArrayList<>();
            for (Map.Entry<String, List<Map<String, Object>>> entry : resultsByLevel.entrySet()) {
                Map<String, Object> levelInfo = new LinkedHashMap<>();
                levelInfo.put("levelName", entry.getKey());
                levelInfo.put("indicatorCount", entry.getValue().size());
                levelInfos.add(levelInfo);
            }

            // 9. 构建操作ID列表
            List<String> operationIds = new ArrayList<>(resultsByOperation.keySet());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("batchId", batchId);
            result.put("operationIds", operationIds);
            result.put("resultsByOperation", resultsByOperation);
            result.put("aggregationResults", allAggregationResults);
            result.put("resultsByLevel", resultsByLevel);
            result.put("levelInfos", levelInfos);
            result.put("weights", Map.of("wAlpha", wAlphaNorm, "wLambda", wLambdaNorm));
            result.put("weightsInput", Map.of("wAlpha", weightAlpha, "wLambda", weightLambda));
            result.put("warnings", warnings);

            return result;
        } catch (Exception e) {
            log.error("集结计算过程发生异常: batchId={}", batchId, e);
            throw new RuntimeException("集结计算失败: " + e.getMessage(), e);
        }
    }

    /**
     * 在独立事务中保存集结结果
     * 使用 REQUIRES_NEW 确保保存操作独立于外部事务
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAggregationWithNewTransaction(String batchId, String operationId, String indicatorCode,
                                                  Map<String, Object> indicatorResult,
                                                  double wAlpha, double wLambda) {
        saveAggregationToDatabase(batchId, operationId, indicatorCode, indicatorResult, wAlpha, wLambda);
    }

    /**
     * 计算单个指标的集结结果（质心式）
     */
    private Map<String, Object> computeIndicatorAggregation(
            String indicatorCode,
            List<DynamicQlRecord> records,
            Map<Long, Double> alphaMap,
            Map<String, double[]> gradeIntervalMap,
            double wAlpha,
            double wLambda,
            List<String> warnings) {

        if (records.isEmpty()) {
            return null;
        }

        // 获取指标名称 - 优先从 dynamic_dimension 表获取
        String indicatorName = getIndicatorNameFromDimension(indicatorCode);
        if (indicatorName == null || indicatorName.isEmpty()) {
            indicatorName = records.get(0).getSecondaryName();
        }
        if (indicatorName == null || indicatorName.isEmpty()) {
            indicatorName = indicatorCode;
        }
        
        String levelName = records.get(0).getLevelName();
        String primaryName = records.get(0).getPrimaryDimension();

        // 计算每个专家的明细数据
        List<Map<String, Object>> expertDetails = new ArrayList<>();
        boolean allLambdaZero = true;

        for (DynamicQlRecord record : records) {
            Long expertId = record.getExpertId();
            String gradeCode = record.getScoreLevel();

            // 获取等级区间
            double[] interval = gradeIntervalMap.getOrDefault(gradeCode, new double[]{70.0, 75.0});
            double a1 = interval[0];
            double a2 = interval[1];
            double midpoint = (a1 + a2) / 2.0;
            double intervalLength = a2 - a1;

            // 获取权威度
            double alphaRaw = alphaMap.getOrDefault(expertId, 70.0);
            double alphaNorm = alphaRaw / 100.0;

            // 获取把握度
            double confidenceValue = record.getConfidenceValue() != null ?
                    record.getConfidenceValue().doubleValue() : 75.0;
            double lambda01 = confidenceValue / 100.0; // 转换为 0-1 范围

            if (lambda01 > 0) {
                allLambdaZero = false;
            }

            // 计算综合可信度 γ = wα·α_norm + wλ·λ
            double gamma = wAlpha * alphaNorm + wLambda * lambda01;

            // 计算 γ·中点（质心分子项）
            double gammaTimesMidpoint = gamma * midpoint;

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("expertId", expertId);
            detail.put("expertName", record.getExpertName());
            detail.put("alphaRaw", Math.round(alphaRaw * 100.0) / 100.0);
            detail.put("alphaNorm", Math.round(alphaNorm * 1e6) / 1e6);
            detail.put("lambdaRaw", confidenceValue);
            detail.put("lambda01", Math.round(lambda01 * 1e6) / 1e6);
            detail.put("gamma", Math.round(gamma * 1e6) / 1e6);
            detail.put("gradeCode", gradeCode);
            detail.put("a1", a1);
            detail.put("a2", a2);
            detail.put("midpoint", Math.round(midpoint * 100.0) / 100.0);
            detail.put("gammaTimesMidpoint", Math.round(gammaTimesMidpoint * 1e6) / 1e6);

            expertDetails.add(detail);
        }

        // 如果所有把握度都为0，跳过
        if (allLambdaZero) {
            warnings.add("指标【" + indicatorName + "】所有专家把握度均为0，已跳过");
            return null;
        }

        // 计算质心 x*
        double numerator = expertDetails.stream()
                .mapToDouble(d -> ((Number) d.get("gammaTimesMidpoint")).doubleValue())
                .sum();
        double denominator = expertDetails.stream()
                .mapToDouble(d -> ((Number) d.get("gamma")).doubleValue())
                .sum();

        Double xStar = null;
        String mappedGrade = null;
        if (denominator > 0) {
            xStar = Math.round(numerator / denominator * 10000.0) / 10000.0;
            mappedGrade = resolveGradeByNumericValue(xStar);
        } else {
            warnings.add("指标【" + indicatorName + "】集结分母为0，已跳过");
            return null;
        }

            // 构建结果
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("indicatorCode", indicatorCode);
            result.put("indicatorName", indicatorName);
            result.put("levelName", levelName);
            result.put("primaryName", primaryName);
            result.put("xStar", xStar);
            result.put("mappedGrade", mappedGrade);
            // 返回映射后等级的区间
            result.put("gradeRange", gradeIntervalMap.getOrDefault(mappedGrade, new double[]{70.0, 75.0}));
            result.put("denominator", Math.round(denominator * 1e6) / 1e6);
            result.put("expertCount", expertDetails.size());
            result.put("details", expertDetails);

            return result;
    }

    /**
     * 从 dynamic_dimension 表获取指标名称
     */
    private String getIndicatorNameFromDimension(String indicatorCode) {
        try {
            String sql = "SELECT name FROM dynamic_dimension WHERE code = ? AND dimension_level = 'SECONDARY'";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, indicatorCode);
            if (!rows.isEmpty()) {
                return (String) rows.get(0).get("name");
            }
        } catch (Exception e) {
            log.warn("查询指标名称失败: code={}, error={}", indicatorCode, e.getMessage());
        }
        return null;
    }

    /**
     * 从 evaluation_grade_definition 构建 gradeCode → [min, max] 映射
     */
    private Map<String, double[]> buildGradeIntervalMap() {
        Map<String, double[]> map = new LinkedHashMap<>();
        try {
            String sql = "SELECT grade_code, min_score, max_score FROM evaluation_grade_definition";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            for (Map<String, Object> row : rows) {
                String code = String.valueOf(row.get("grade_code"));
                BigDecimal minBd = (BigDecimal) row.get("min_score");
                BigDecimal maxBd = (BigDecimal) row.get("max_score");
                if (minBd != null && maxBd != null) {
                    map.put(code, new double[]{minBd.doubleValue(), maxBd.doubleValue()});
                }
            }
        } catch (Exception e) {
            log.warn("查等级区间定义失败: {}", e.getMessage());
        }
        if (map.isEmpty()) {
            // fallback：15档
            map.put("A+", new double[]{95.0, 100.0});
            map.put("A",  new double[]{90.0, 95.0});
            map.put("A-", new double[]{85.0, 90.0});
            map.put("B+", new double[]{80.0, 85.0});
            map.put("B",  new double[]{75.0, 80.0});
            map.put("B-", new double[]{70.0, 75.0});
            map.put("C+", new double[]{65.0, 70.0});
            map.put("C",  new double[]{60.0, 65.0});
            map.put("C-", new double[]{55.0, 60.0});
            map.put("D+", new double[]{47.0, 55.0});
            map.put("D",  new double[]{39.0, 47.0});
            map.put("D-", new double[]{30.0, 39.0});
            map.put("E+", new double[]{20.0, 30.0});
            map.put("E",  new double[]{10.0, 20.0});
            map.put("E-", new double[]{0.0,  10.0});
        }
        return map;
    }

    /**
     * 批量查询专家权威度（alpha）
     */
    private Map<Long, Double> buildAlphaMap(Set<Long> expertIds) {
        Map<Long, Double> map = new LinkedHashMap<>();
        if (expertIds == null || expertIds.isEmpty()) return map;

        List<String> placeholders = Collections.nCopies(expertIds.size(), "?");
        String sql = "SELECT expert_id, total_score FROM expert_credibility_evaluation_score " +
                "WHERE expert_id IN (" + String.join(",", placeholders) + ")";
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, expertIds.toArray());
            for (Map<String, Object> row : rows) {
                Object idObj = row.get("expert_id");
                Long id = idObj instanceof Number ? ((Number) idObj).longValue() : Long.valueOf(String.valueOf(idObj));
                BigDecimal bd = (BigDecimal) row.get("total_score");
                double score = bd != null ? bd.doubleValue() : 0.0;
                map.put(id, score);
            }
        } catch (Exception e) {
            log.warn("查专家权威度失败: {}", e.getMessage());
        }
        // 未查到权威度的专家默认 70
        for (Long id : expertIds) {
            map.putIfAbsent(id, 70.0);
        }
        return map;
    }

    /**
     * 归一化 wα、wλ 权重到非负且和为1
     */
    private double[] normalizeAlphaLambdaWeights(double wAlpha, double wLambda) {
        double a = Math.max(0.0, wAlpha);
        double l = Math.max(0.0, wLambda);
        double s = a + l;
        if (s <= 1e-12) {
            return new double[]{0.5, 0.5};
        }
        return new double[]{a / s, l / s};
    }

    /**
     * 按分数区间判定综合等级
     */
    private String resolveGradeByNumericValue(double numericValue) {
        Map<String, double[]> map = buildGradeIntervalMap();
        for (Map.Entry<String, double[]> entry : map.entrySet()) {
            double[] interval = entry.getValue();
            double min = interval[0];
            double max = interval[1];
            if (numericValue >= min && numericValue < max) {
                return entry.getKey();
            }
        }
        // fallback
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
     * 保存集结结果到数据库
     * 注意：此方法本身不开启事务，由调用者控制事务
     */
    private void saveAggregationToDatabase(String batchId, String operationId, String indicatorCode,
                                           Map<String, Object> indicatorResult,
                                           double wAlpha, double wLambda) {
        try {
            // 处理 null operationId，使用占位符存储以满足数据库 NOT NULL 约束
            String effectiveOperationId = (operationId != null && !operationId.trim().isEmpty())
                    ? operationId : "DEFAULT";

            // 先查询是否已存在记录（存在则更新，不存在则新增）
            Optional<DynamicQlAggregation> existingOpt =
                    qlAggregationRepository.findByBatchIdAndOperationIdAndSecondaryCode(
                            batchId, effectiveOperationId, indicatorCode);

            DynamicQlAggregation aggregation;
            if (existingOpt.isPresent()) {
                aggregation = existingOpt.get();
            } else {
                aggregation = new DynamicQlAggregation();
                aggregation.setBatchId(batchId);
                aggregation.setOperationId(effectiveOperationId);
                aggregation.setSecondaryCode(indicatorCode);
            }

            aggregation.setSecondaryName((String) indicatorResult.get("indicatorName"));

            Object xStarObj = indicatorResult.get("xStar");
            if (xStarObj != null) {
                aggregation.setAggregatedValue(BigDecimal.valueOf(((Number) xStarObj).doubleValue()));
            }

            aggregation.setAggregationMethod(DynamicQlAggregation.AggregationMethod.WEIGHTED_CENTROID);
            aggregation.setExpertCount((Integer) indicatorResult.get("expertCount"));

            // 设置权重参数
            aggregation.setWeightAlpha(BigDecimal.valueOf(wAlpha));
            aggregation.setWeightLambda(BigDecimal.valueOf(wLambda));

            // 保存专家明细
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> details = (List<Map<String, Object>>) indicatorResult.get("details");
            if (details != null) {
                aggregation.setParticipatingExpertsJson(objectMapper.writeValueAsString(details));
            }

            qlAggregationRepository.save(aggregation);
        } catch (Exception e) {
            log.error("保存集结结果失败: batchId={}, operationId={}, indicatorCode={}, error={}",
                    batchId, operationId, indicatorCode, e.getMessage(), e);
            // 抛出异常让事务回滚
            throw new RuntimeException("保存集结结果失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取集结结果
     */
    public List<DynamicQlAggregation> getAggregationResults(String batchId, String operationId) {
        if (operationId != null) {
            return qlAggregationRepository.findByBatchIdAndOperationId(batchId, operationId);
        } else {
            return qlAggregationRepository.findByBatchId(batchId);
        }
    }

    // ==================== 会话管理 ====================

    /**
     * 创建评估会话
     */
    @Transactional
    public Map<String, Object> createSession(Long templateId, String batchId, List<Long> expertIds) {
        String sessionId = "QL_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        DynamicQlSession session = new DynamicQlSession();
        session.setSessionId(sessionId);
        session.setTemplateId(templateId);
        session.setBatchId(batchId);
        session.setStatus(DynamicQlSession.SessionStatus.DRAFT);

        if (expertIds != null && !expertIds.isEmpty()) {
            try {
                List<Map<String, Object>> experts = new ArrayList<>();
                for (Long expertId : expertIds) {
                    Map<String, Object> expert = new LinkedHashMap<>();
                    expert.put("expertId", expertId);
                    // 后续从数据库获取详细信息
                    experts.add(expert);
                }
                session.setSelectedExpertsJson(objectMapper.writeValueAsString(experts));
            } catch (JsonProcessingException e) {
                log.error("序列化专家列表失败", e);
            }
        }

        qlSessionRepository.save(session);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);
        result.put("templateId", templateId);
        result.put("batchId", batchId);
        result.put("expertCount", expertIds != null ? expertIds.size() : 0);

        return result;
    }

    /**
     * 获取会话详情
     */
    public Map<String, Object> getSessionDetail(String sessionId) {
        DynamicQlSession session = qlSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("会话不存在: " + sessionId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", session.getSessionId());
        result.put("templateId", session.getTemplateId());
        result.put("batchId", session.getBatchId());
        result.put("status", session.getStatus().name());
        result.put("description", session.getDescription());

        // 解析专家列表
        if (session.getSelectedExpertsJson() != null) {
            try {
                List<Map<String, Object>> experts =
                        objectMapper.readValue(session.getSelectedExpertsJson(),
                                new TypeReference<List<Map<String, Object>>>() {});
                result.put("selectedExperts", experts);
            } catch (JsonProcessingException e) {
                log.error("解析专家列表失败", e);
            }
        }

        return result;
    }
}
