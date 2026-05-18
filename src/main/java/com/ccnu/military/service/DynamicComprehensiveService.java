package com.ccnu.military.service;

import com.ccnu.military.dto.DynamicAhpCombinedWeightDTO;
import com.ccnu.military.dto.DynamicComprehensiveResultDTO;
import com.ccnu.military.dto.RawDataAggregationDTO;
import com.ccnu.military.entity.DynamicAhpAggregationResult;
import com.ccnu.military.entity.DynamicComprehensiveResult;
import com.ccnu.military.entity.DynamicDimension;
import com.ccnu.military.entity.DynamicQlAggregation;
import com.ccnu.military.entity.DynamicTemplate;
import com.ccnu.military.repository.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 动态综合评分服务
 * 整合动态AHP权重、动态定性评估、动态定量评估进行综合评分
 *
 * 综合评分公式：
 * 综合得分 = 定性加权分 + 定量加权分
 * 其中：
 * - 定性加权分 = Σ(定性得分 × 综合权重)
 * - 定量加权分 = Σ(定量得分 × 综合权重)
 * - 综合权重 = 层级权重 × 一级维度权重 × 二级指标权重，相加为1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicComprehensiveService {

    private final JdbcTemplate jdbcTemplate;
    private final DynamicAhpAggregationResultRepository ahpRepository;
    private final DynamicQlAggregationRepository qlRepository;
    private final DynamicDimensionRepository dimensionRepository;
    private final DynamicTemplateRepository templateRepository;
    private final DynamicComprehensiveResultRepository comprehensiveResultRepository;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取批次列表
     */
    public List<Map<String, Object>> getBatches(Long templateId) {
        Set<String> batchIdSet = new LinkedHashSet<>();
        List<Map<String, Object>> result = new ArrayList<>();

        String qtSql = "SELECT batch_id, template_id, template_name, MAX(created_at) as created_at " +
                "FROM dynamic_qt_record WHERE batch_id IS NOT NULL ";
        if (templateId != null) {
            qtSql += "AND template_id = ? ";
        }
        qtSql += "GROUP BY batch_id, template_id, template_name ORDER BY MAX(created_at) DESC";
        List<Map<String, Object>> qtBatches = templateId != null
                ? jdbcTemplate.queryForList(qtSql, templateId)
                : jdbcTemplate.queryForList(qtSql);

        for (Map<String, Object> batch : qtBatches) {
            String batchId = (String) batch.get("batch_id");
            if (batchId != null && batchIdSet.add(batchId)) {
                result.add(batch);
            }
        }

        String qlSql = "SELECT r.batch_id, r.template_id, t.template_name, MAX(r.created_at) as created_at " +
                "FROM dynamic_ql_record r " +
                "LEFT JOIN dynamic_template t ON r.template_id = t.id " +
                "WHERE r.batch_id IS NOT NULL ";
        if (templateId != null) {
            qlSql += "AND r.template_id = ? ";
        }
        qlSql += "GROUP BY r.batch_id, r.template_id, t.template_name ORDER BY MAX(r.created_at) DESC";
        List<Map<String, Object>> qlBatches = templateId != null
                ? jdbcTemplate.queryForList(qlSql, templateId)
                : jdbcTemplate.queryForList(qlSql);

        for (Map<String, Object> batch : qlBatches) {
            String batchId = (String) batch.get("batch_id");
            if (batchId != null && batchIdSet.add(batchId)) {
                result.add(batch);
            }
        }

        // 计算每个批次的作战数量
        for (Map<String, Object> batch : result) {
            String batchId = (String) batch.get("batch_id");
            int opCount = 0;
            try {
                String qtCountSql = "SELECT COUNT(DISTINCT operation_id) FROM dynamic_qt_record WHERE batch_id = ? AND normalization_name IS NULL";
                Object count = jdbcTemplate.queryForObject(qtCountSql, new Object[]{batchId}, Integer.class);
                if (count != null) {
                    opCount += ((Number) count).intValue();
                }
            } catch (Exception e) {
                log.debug("定量表统计失败: {}", e.getMessage());
            }
            try {
                String qlCountSql = "SELECT COUNT(DISTINCT operation_id) FROM dynamic_ql_record WHERE batch_id = ?";
                Object count = jdbcTemplate.queryForObject(qlCountSql, new Object[]{batchId}, Integer.class);
                if (count != null) {
                    opCount += ((Number) count).intValue();
                }
            } catch (Exception e) {
                log.debug("定性表统计失败: {}", e.getMessage());
            }
            batch.put("operationCount", opCount);
        }

        return result;
    }

    /**
     * 获取原始数据集结表
     */
    public RawDataAggregationDTO getRawDataAggregation(String batchId) {
        RawDataAggregationDTO result = RawDataAggregationDTO.builder().build();

        String batchInfoSql = "SELECT DISTINCT template_id, template_name FROM dynamic_qt_record WHERE batch_id = ? LIMIT 1";
        try {
            Map<String, Object> batchInfo = jdbcTemplate.queryForMap(batchInfoSql, batchId);
            result.setBatchId(batchId);
            result.setTemplateId(batchInfo.get("template_id") != null ?
                    ((Number) batchInfo.get("template_id")).longValue() : null);
            result.setTemplateName((String) batchInfo.get("template_name"));
        } catch (Exception e) {
            log.error("获取批次信息失败: {}", e.getMessage());
            return result;
        }

        String opsSql = "SELECT DISTINCT operation_id FROM dynamic_qt_record WHERE batch_id = ? AND normalization_name IS NULL ORDER BY operation_id";
        List<String> operationIds = jdbcTemplate.queryForList(opsSql, String.class, batchId);
        result.setOperationIds(operationIds);

        List<RawDataAggregationDTO.PrimaryDimensionData> primaryDimensions = getIndicatorStructure(result.getTemplateId());
        result.setPrimaryDimensions(primaryDimensions);

        Map<String, Map<String, Double>> qlScores = getQualitativeScores(batchId);
        Map<String, Map<String, Double>> qtScores = getQuantitativeRawScores(batchId);
        Map<String, Double> weights = getWeightMap(result.getTemplateId());

        List<RawDataAggregationDTO.OperationRawData> operationData = new ArrayList<>();

        for (String opId : operationIds) {
            RawDataAggregationDTO.OperationRawData opData = RawDataAggregationDTO.OperationRawData.builder()
                    .operationId(opId)
                    .scores(new ArrayList<>())
                    .build();

            double totalQual = 0;
            double totalQt = 0;

            for (RawDataAggregationDTO.PrimaryDimensionData primary : primaryDimensions) {
                for (RawDataAggregationDTO.IndicatorData indicator : primary.getIndicators()) {
                    String code = indicator.getCode();
                    Double qualScore = qlScores.getOrDefault(opId, Collections.emptyMap()).get(code);
                    Double qtScore = qtScores.getOrDefault(opId, Collections.emptyMap()).get(code);

                    Double combinedScore = null;
                    if (qualScore != null && qtScore != null) {
                        combinedScore = qualScore + qtScore;
                        totalQual += qualScore * (weights.getOrDefault(code, 0.0));
                        totalQt += qtScore * (weights.getOrDefault(code, 0.0));
                    } else if (qualScore != null) {
                        combinedScore = qualScore;
                        totalQual += qualScore * (weights.getOrDefault(code, 0.0));
                    } else if (qtScore != null) {
                        combinedScore = qtScore;
                        totalQt += qtScore * (weights.getOrDefault(code, 0.0));
                    }

                    if (qualScore != null || qtScore != null) {
                        opData.getScores().add(RawDataAggregationDTO.IndicatorScore.builder()
                                .indicatorCode(code)
                                .indicatorName(indicator.getName())
                                .qualitativeScore(qualScore)
                                .quantitativeScore(qtScore)
                                .combinedScore(combinedScore)
                                .build());
                    }
                }
            }

            if (!opData.getScores().isEmpty()) {
                opData.setComprehensiveScore(totalQual + totalQt);
            }
            operationData.add(opData);
        }

        result.setOperationData(operationData);
        return result;
    }

    /**
     * 获取综合评分结果（优先从数据库查询，无则重新计算并保存）
     */
    public List<DynamicComprehensiveResultDTO> getComprehensiveScores(String batchId) {
        log.info("【综合评分Service】getComprehensiveScores 开始, batchId={}", batchId);
        // 1. 优先从数据库查询
        List<DynamicComprehensiveResultDTO> savedResults = getSavedComprehensiveScores(batchId);
        if (!savedResults.isEmpty()) {
            log.info("【综合评分Service】从数据库加载综合评分结果成功, batchId={}, count={}", batchId, savedResults.size());
            return savedResults;
        }

        // 2. 数据库无记录，重新计算并保存
        log.info("【综合评分Service】数据库无综合评分结果，重新计算, batchId={}", batchId);
        List<DynamicComprehensiveResultDTO> results = calculateAndSaveComprehensiveScores(batchId);
        log.info("【综合评分Service】重新计算完成, batchId={}, count={}", batchId, results.size());
        return results;
    }

    /**
     * 从数据库获取已保存的综合评分结果
     */
    public List<DynamicComprehensiveResultDTO> getSavedComprehensiveScores(String batchId) {
        log.info("【综合评分Service】getSavedComprehensiveScores 开始查询, batchId={}", batchId);
        List<DynamicComprehensiveResultDTO> results = new ArrayList<>();

        try {
            List<DynamicComprehensiveResult> entities = comprehensiveResultRepository.findByBatchIdOrderByOperationId(batchId);
            log.info("【综合评分Service】数据库查询完成, 找到 {} 条记录", entities.size());

            for (DynamicComprehensiveResult entity : entities) {
                try {
                    DynamicComprehensiveResultDTO dto = DynamicComprehensiveResultDTO.builder()
                            .batchId(entity.getBatchId())
                            .operationId(entity.getOperationId())
                            .templateId(entity.getTemplateId())
                            .templateName(entity.getTemplateName())
                            .totalScore(entity.getTotalScore() != null ? entity.getTotalScore().doubleValue() : null)
                            .qualitativeWeightedScore(entity.getQualitativeWeightedScore() != null ? entity.getQualitativeWeightedScore().doubleValue() : null)
                            .quantitativeWeightedScore(entity.getQuantitativeWeightedScore() != null ? entity.getQuantitativeWeightedScore().doubleValue() : null)
                            .calculatedAt(entity.getCalculatedAt() != null ? entity.getCalculatedAt().toString() : null)
                            .build();

                    // 解析JSON字段
                    if (entity.getLevelScoresJson() != null && !entity.getLevelScoresJson().isEmpty()) {
                        try {
                            dto.setLevelScores(objectMapper.readValue(entity.getLevelScoresJson(),
                                    objectMapper.getTypeFactory().constructCollectionType(List.class, DynamicComprehensiveResultDTO.LevelScore.class)));
                        } catch (Exception e) {
                            log.warn("解析levelScoresJson失败: {}", e.getMessage());
                        }
                    }
                    if (entity.getWeightsConfigJson() != null && !entity.getWeightsConfigJson().isEmpty()) {
                        try {
                            dto.setCombinedWeights(objectMapper.readValue(entity.getWeightsConfigJson(),
                                    objectMapper.getTypeFactory().constructCollectionType(List.class, DynamicAhpCombinedWeightDTO.class)));
                        } catch (Exception e) {
                            log.warn("解析weightsConfigJson失败: {}", e.getMessage());
                        }
                    }

                    results.add(dto);
                } catch (Exception e) {
                    log.error("【综合评分Service】转换实体失败, operationId={}, error={}", entity.getOperationId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("【综合评分Service】从数据库获取综合评分结果失败: {}", e.getMessage(), e);
        }

        log.info("【综合评分Service】getSavedComprehensiveScores 完成, 返回 {} 条记录", results.size());
        return results;
    }

    /**
     * 计算综合评分并保存到数据库
     */
    public List<DynamicComprehensiveResultDTO> calculateAndSaveComprehensiveScores(String batchId) {
        List<DynamicComprehensiveResultDTO> results = new ArrayList<>();

        String batchInfoSql = "SELECT DISTINCT template_id, template_name FROM dynamic_qt_record WHERE batch_id = ? LIMIT 1";
        Long templateId = null;
        String templateName = null;
        try {
            Map<String, Object> batchInfo = jdbcTemplate.queryForMap(batchInfoSql, batchId);
            templateId = batchInfo.get("template_id") != null ?
                    ((Number) batchInfo.get("template_id")).longValue() : null;
            templateName = (String) batchInfo.get("template_name");
        } catch (Exception e) {
            log.error("获取批次信息失败: {}", e.getMessage());
            return results;
        }

        String opsSql = "SELECT DISTINCT operation_id FROM dynamic_qt_record WHERE batch_id = ? AND normalization_name IS NULL ORDER BY operation_id";
        List<String> operationIds = jdbcTemplate.queryForList(opsSql, String.class, batchId);

        // 获取综合权重数据（完整层级结构）
        List<DynamicAhpCombinedWeightDTO> combinedWeights = getCombinedWeights(templateId);
        // 获取定性集结数据
        Map<String, Map<String, Double>> qlScores = getQualitativeScores(batchId);
        // 获取定量原始数据
        Map<String, Map<String, Double>> qtScores = getQuantitativeRawScores(batchId);
        // 获取指标结构
        Map<String, String> indicatorNameToCode = getIndicatorNameToCodeMap(templateId);

        // 计算每个作战的综合得分并保存
        for (String opId : operationIds) {
            DynamicComprehensiveResultDTO dto = calculateOperationScore(
                    batchId, opId, templateId, templateName,
                    combinedWeights,
                    qlScores.get(opId),
                    qtScores.get(opId),
                    indicatorNameToCode);
            results.add(dto);

            // 保存到数据库
            saveComprehensiveResult(dto, combinedWeights);
        }

        log.info("综合评分计算并保存完成: batchId={}, count={}", batchId, results.size());
        return results;
    }

    /**
     * 保存单个综合评分结果到数据库
     */
    private void saveComprehensiveResult(DynamicComprehensiveResultDTO dto, List<DynamicAhpCombinedWeightDTO> combinedWeights) {
        try {
            // 序列化JSON字段
            String levelScoresJson = dto.getLevelScores() != null ?
                    objectMapper.writeValueAsString(dto.getLevelScores()) : null;
            String weightsConfigJson = combinedWeights != null ?
                    objectMapper.writeValueAsString(combinedWeights) : null;

            DynamicComprehensiveResult entity = DynamicComprehensiveResult.builder()
                    .batchId(dto.getBatchId())
                    .operationId(dto.getOperationId())
                    .templateId(dto.getTemplateId())
                    .templateName(dto.getTemplateName())
                    .totalScore(dto.getTotalScore() != null ? BigDecimal.valueOf(dto.getTotalScore()) : null)
                    .qualitativeWeightedScore(dto.getQualitativeWeightedScore() != null ? BigDecimal.valueOf(dto.getQualitativeWeightedScore()) : null)
                    .quantitativeWeightedScore(dto.getQuantitativeWeightedScore() != null ? BigDecimal.valueOf(dto.getQuantitativeWeightedScore()) : null)
                    .calculatedAt(LocalDateTime.now())
                    .levelScoresJson(levelScoresJson)
                    .weightsConfigJson(weightsConfigJson)
                    .build();

            // 使用 upsert 逻辑：存在则更新，不存在则插入
            DynamicComprehensiveResult existing = comprehensiveResultRepository
                    .findByBatchIdAndOperationId(dto.getBatchId(), dto.getOperationId())
                    .orElse(null);

            if (existing != null) {
                entity.setId(existing.getId());
            }
            comprehensiveResultRepository.save(entity);

            log.debug("保存综合评分结果: batchId={}, operationId={}, totalScore={}",
                    dto.getBatchId(), dto.getOperationId(), dto.getTotalScore());
        } catch (Exception e) {
            log.error("保存综合评分结果失败: batchId={}, operationId={}, error={}",
                    dto.getBatchId(), dto.getOperationId(), e.getMessage());
        }
    }

    /**
     * 计算单个作战的综合得分
     */
    private DynamicComprehensiveResultDTO calculateOperationScore(
            String batchId, String operationId, Long templateId, String templateName,
            List<DynamicAhpCombinedWeightDTO> combinedWeights,
            Map<String, Double> qlScores,
            Map<String, Double> qtScores,
            Map<String, String> indicatorNameToCode) {

        double totalQualWeighted = 0;
        double totalQtWeighted = 0;

        // 按层级分组
        Map<String, List<DynamicAhpCombinedWeightDTO>> byLevel = combinedWeights.stream()
                .collect(Collectors.groupingBy(DynamicAhpCombinedWeightDTO::getLevelName));

        List<DynamicComprehensiveResultDTO.LevelScore> levelScores = new ArrayList<>();

        for (Map.Entry<String, List<DynamicAhpCombinedWeightDTO>> levelEntry : byLevel.entrySet()) {
            String levelName = levelEntry.getKey();
            List<DynamicAhpCombinedWeightDTO> levelItems = levelEntry.getValue();

            double levelQual = 0, levelQt = 0, levelWeight = 0;
            List<DynamicComprehensiveResultDTO.PrimaryDimensionScore> primaryScores = new ArrayList<>();

            // 按一级维度分组
            Map<String, List<DynamicAhpCombinedWeightDTO>> byPrimary = levelItems.stream()
                    .collect(Collectors.groupingBy(DynamicAhpCombinedWeightDTO::getPrimaryName));

            for (Map.Entry<String, List<DynamicAhpCombinedWeightDTO>> primaryEntry : byPrimary.entrySet()) {
                String primaryName = primaryEntry.getKey();
                List<DynamicAhpCombinedWeightDTO> primaryItems = primaryEntry.getValue();

                double primaryQual = 0, primaryQt = 0, primaryWeightSum = 0;

                for (DynamicAhpCombinedWeightDTO item : primaryItems) {
                    String secName = item.getSecondaryName();
                    Double w = item.getCombinedWeight();

                    // 通过指标名称查找得分
                    String code = indicatorNameToCode.get(secName);
                    Double qualScore = qlScores != null ? qlScores.get(code) : null;
                    Double qtScore = qtScores != null ? qtScores.get(code) : null;

                    if (qualScore != null) {
                        primaryQual += qualScore * w;
                        levelQual += qualScore * w;
                        totalQualWeighted += qualScore * w;
                    }
                    if (qtScore != null) {
                        primaryQt += qtScore * w;
                        levelQt += qtScore * w;
                        totalQtWeighted += qtScore * w;
                    }
                    primaryWeightSum += w;
                }

                levelWeight += primaryWeightSum;

                // 获取一级维度的 code
                String dimCode = getDimensionCodeByName(primaryName, templateId);

                primaryScores.add(DynamicComprehensiveResultDTO.PrimaryDimensionScore.builder()
                        .dimensionCode(dimCode)
                        .dimensionName(primaryName)
                        .weight(round(primaryWeightSum, 4))
                        .qualitativeScore(round(primaryQual, 3))
                        .quantitativeScore(round(primaryQt, 3))
                        .comprehensiveScore(round(primaryQual + primaryQt, 3))
                        .build());
            }

            levelScores.add(DynamicComprehensiveResultDTO.LevelScore.builder()
                    .levelName(levelName)
                    .weight(round(levelWeight, 4))
                    .qualitativeScore(round(levelQual, 3))
                    .quantitativeScore(round(levelQt, 3))
                    .comprehensiveScore(round(levelQual + levelQt, 3))
                    .primaryDimensions(primaryScores)
                    .build());
        }

        double totalScore = totalQualWeighted + totalQtWeighted;

        return DynamicComprehensiveResultDTO.builder()
                .batchId(batchId)
                .operationId(operationId)
                .templateId(templateId)
                .templateName(templateName)
                .totalScore(round(totalScore, 3))
                .qualitativeWeightedScore(round(totalQualWeighted, 3))
                .quantitativeWeightedScore(round(totalQtWeighted, 3))
                .levelScores(levelScores)
                .combinedWeights(combinedWeights)
                .calculatedAt(LocalDateTime.now().format(DATE_FORMATTER))
                .build();
    }

    /**
     * 获取综合权重数据（解析 combined_weights_json）
     */
    private List<DynamicAhpCombinedWeightDTO> getCombinedWeights(Long templateId) {
        List<DynamicAhpCombinedWeightDTO> result = new ArrayList<>();

        if (templateId == null) {
            return result;
        }

        try {
            List<DynamicAhpAggregationResult> aggregations = ahpRepository.findByTemplateIdOrderByUpdatedAtDesc(templateId);
            if (!aggregations.isEmpty()) {
                DynamicAhpAggregationResult agg = aggregations.get(0);
                String combinedWeightsJson = agg.getCombinedWeightsJson();
                String levelWeightsJson = agg.getLevelWeightsJson();
                String dimensionWeightsJson = agg.getDimensionWeightsJson();

                // 解析层级权重
                Map<String, Double> levelWeights = parseJsonToMap(levelWeightsJson);
                // 解析一级维度权重
                Map<String, Map<String, Double>> dimensionWeights = parseDimensionWeights(dimensionWeightsJson);

                if (combinedWeightsJson != null && !combinedWeightsJson.isBlank()) {
                    List<Map<String, Object>> weights = objectMapper.readValue(
                            combinedWeightsJson,
                            new TypeReference<List<Map<String, Object>>>() {});

                    for (Map<String, Object> w : weights) {
                        String levelName = (String) w.get("levelName");
                        String primaryName = (String) w.get("primaryName");
                        String secondaryName = (String) w.get("secondaryName");
                        Number weight = (Number) w.get("combinedWeight");

                        if (secondaryName != null && weight != null) {
                            // 获取层级权重和一级维度权重
                            Double lw = levelWeights.getOrDefault(levelName, 1.0);
                            Double pw = dimensionWeights.getOrDefault(levelName, new HashMap<>())
                                    .getOrDefault(primaryName, 1.0);

                            result.add(DynamicAhpCombinedWeightDTO.builder()
                                    .levelName(levelName)
                                    .primaryName(primaryName)
                                    .secondaryName(secondaryName)
                                    .combinedWeight(weight.doubleValue())
                                    .levelWeight(lw)
                                    .primaryWeight(pw)
                                    .build());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取综合权重数据失败: {}", e.getMessage());
        }

        // 验证权重总和
        if (!result.isEmpty()) {
            double totalWeight = result.stream()
                    .mapToDouble(DynamicAhpCombinedWeightDTO::getCombinedWeight).sum();
            log.info("综合权重总和验证: {} (应为1.0)", round(totalWeight, 4));
        }

        return result;
    }

    /**
     * 获取权重Map（指标名称 -> 综合权重）
     */
    private Map<String, Double> getWeightMap(Long templateId) {
        Map<String, Double> result = new LinkedHashMap<>();

        if (templateId == null) {
            return result;
        }

        try {
            List<DynamicAhpAggregationResult> aggregations = ahpRepository.findByTemplateIdOrderByUpdatedAtDesc(templateId);
            if (!aggregations.isEmpty()) {
                DynamicAhpAggregationResult agg = aggregations.get(0);
                String combinedWeightsJson = agg.getCombinedWeightsJson();

                if (combinedWeightsJson != null && !combinedWeightsJson.isBlank()) {
                    List<Map<String, Object>> weights = objectMapper.readValue(
                            combinedWeightsJson,
                            new TypeReference<List<Map<String, Object>>>() {});

                    for (Map<String, Object> w : weights) {
                        String name = (String) w.get("secondaryName");
                        Number weight = (Number) w.get("combinedWeight");
                        if (name != null && weight != null) {
                            result.put(name, weight.doubleValue());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取权重Map失败: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 获取指标名称到编码的映射
     */
    private Map<String, String> getIndicatorNameToCodeMap(Long templateId) {
        Map<String, String> result = new LinkedHashMap<>();

        if (templateId == null) {
            return result;
        }

        try {
            List<DynamicDimension> secondaries = dimensionRepository.findByTemplateIdAndDimensionLevelOrderBySortOrder(
                    templateId, DynamicDimension.DimensionLevel.SECONDARY);
            for (DynamicDimension d : secondaries) {
                result.put(d.getName(), d.getCode());
            }
        } catch (Exception e) {
            log.error("获取指标名称映射失败: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 根据一级维度名称获取编码
     */
    private String getDimensionCodeByName(String name, Long templateId) {
        if (templateId == null) {
            return name;
        }

        try {
            List<DynamicDimension> primaries = dimensionRepository.findByTemplateIdAndDimensionLevelOrderBySortOrder(
                    templateId, DynamicDimension.DimensionLevel.PRIMARY);
            for (DynamicDimension d : primaries) {
                if (d.getName().equals(name)) {
                    return d.getCode();
                }
            }
        } catch (Exception e) {
            log.error("获取维度编码失败: {}", e.getMessage());
        }

        return name;
    }

    /**
     * 解析 JSON 到 Map
     */
    private Map<String, Double> parseJsonToMap(String json) {
        Map<String, Double> result = new HashMap<>();
        if (json == null || json.isBlank()) {
            return result;
        }
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    result.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
                }
            }
        } catch (Exception e) {
            log.error("解析JSON到Map失败: {}", e.getMessage());
        }
        return result;
    }

    /**
     * 解析一级维度权重 JSON
     * 格式: {"战术级": {"通信质量": 0.35, "抗干扰": 0.25}, "战役级": {...}}
     */
    private Map<String, Map<String, Double>> parseDimensionWeights(String json) {
        Map<String, Map<String, Double>> result = new HashMap<>();
        if (json == null || json.isBlank()) {
            return result;
        }
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            for (Map.Entry<String, Object> levelEntry : map.entrySet()) {
                String levelName = levelEntry.getKey();
                if (levelEntry.getValue() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dimMap = (Map<String, Object>) levelEntry.getValue();
                    Map<String, Double> dimWeights = new HashMap<>();
                    for (Map.Entry<String, Object> dimEntry : dimMap.entrySet()) {
                        if (dimEntry.getValue() instanceof Number) {
                            dimWeights.put(dimEntry.getKey(), ((Number) dimEntry.getValue()).doubleValue());
                        }
                    }
                    result.put(levelName, dimWeights);
                }
            }
        } catch (Exception e) {
            log.error("解析一级维度权重失败: {}", e.getMessage());
        }
        return result;
    }

    /**
     * 获取定性集结数据
     */
    private Map<String, Map<String, Double>> getQualitativeScores(String batchId) {
        Map<String, Map<String, Double>> result = new LinkedHashMap<>();

        try {
            List<DynamicQlAggregation> aggregations = qlRepository.findByBatchId(batchId);
            for (DynamicQlAggregation agg : aggregations) {
                String opId = agg.getOperationId();
                String code = agg.getSecondaryCode();
                Double value = agg.getAggregatedValue() != null ? agg.getAggregatedValue().doubleValue() : null;
                result.computeIfAbsent(opId, k -> new LinkedHashMap<>()).put(code, value);
            }
        } catch (Exception e) {
            log.error("获取定性集结数据失败: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 获取定量原始数据
     */
    private Map<String, Map<String, Double>> getQuantitativeRawScores(String batchId) {
        Map<String, Map<String, Double>> result = new LinkedHashMap<>();

        try {
            String sql = "SELECT operation_id, secondary_code, raw_value FROM dynamic_qt_record " +
                    "WHERE batch_id = ? AND normalization_name IS NULL";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, batchId);

            for (Map<String, Object> row : rows) {
                String opId = (String) row.get("operation_id");
                String code = (String) row.get("secondary_code");
                BigDecimal val = (BigDecimal) row.get("raw_value");

                if (val != null) {
                    result.computeIfAbsent(opId, k -> new LinkedHashMap<>()).put(code, val.doubleValue());
                }
            }
        } catch (Exception e) {
            log.error("获取定量原始数据失败: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 获取指标结构（一级维度 + 二级指标）
     */
    private List<RawDataAggregationDTO.PrimaryDimensionData> getIndicatorStructure(Long templateId) {
        if (templateId == null) {
            return new ArrayList<>();
        }

        List<RawDataAggregationDTO.PrimaryDimensionData> result = new ArrayList<>();

        List<DynamicDimension> secondaries = dimensionRepository.findByTemplateIdAndDimensionLevelOrderBySortOrder(
                templateId, DynamicDimension.DimensionLevel.SECONDARY);

        List<DynamicDimension> primaries = dimensionRepository.findByTemplateIdAndDimensionLevelOrderBySortOrder(
                templateId, DynamicDimension.DimensionLevel.PRIMARY);
        Map<Long, DynamicDimension> primaryMap = primaries.stream()
                .collect(Collectors.toMap(DynamicDimension::getId, d -> d));

        List<DynamicDimension> levels = dimensionRepository.findByTemplateIdAndDimensionLevelOrderBySortOrder(
                templateId, DynamicDimension.DimensionLevel.LEVEL);
        Map<Long, DynamicDimension> levelMap = levels.stream()
                .collect(Collectors.toMap(DynamicDimension::getId, d -> d));

        Map<Long, List<DynamicDimension>> byPrimary = secondaries.stream()
                .filter(d -> d.getParentId() != null && primaryMap.containsKey(d.getParentId()))
                .collect(Collectors.groupingBy(DynamicDimension::getParentId));

        for (Map.Entry<Long, List<DynamicDimension>> entry : byPrimary.entrySet()) {
            DynamicDimension primary = primaryMap.get(entry.getKey());
            if (primary == null) continue;

            DynamicDimension level = levelMap.get(primary.getParentId());

            List<RawDataAggregationDTO.IndicatorData> indicators = entry.getValue().stream()
                    .map(d -> RawDataAggregationDTO.IndicatorData.builder()
                            .code(d.getCode())
                            .name(d.getName())
                            .metricType(d.getMetricType() != null ? d.getMetricType().name() : "QUANTITATIVE")
                            .build())
                    .collect(Collectors.toList());

            result.add(RawDataAggregationDTO.PrimaryDimensionData.builder()
                    .code(primary.getCode())
                    .name(primary.getName())
                    .levelName(level != null ? level.getName() : null)
                    .indicators(indicators)
                    .build());
        }

        return result;
    }

    /**
     * 获取指标树结构
     */
    public Map<String, Object> getIndicatorTree(Long templateId) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (templateId == null) {
            return result;
        }

        try {
            DynamicTemplate template = templateRepository.findById(templateId).orElse(null);
            if (template != null) {
                result.put("templateId", templateId);
                result.put("templateName", template.getTemplateName());
            }

            List<DynamicDimension> levels = dimensionRepository.findByTemplateIdOrderBySortOrder(templateId);
            Map<String, List<DynamicDimension>> byLevel = levels.stream()
                    .collect(Collectors.groupingBy(
                            d -> d.getDimensionLevel() != null ? d.getDimensionLevel().name() : "UNKNOWN"));

            result.put("levels", byLevel);
            result.put("dimensions", levels);
        } catch (Exception e) {
            log.error("获取指标树失败: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 四舍五入
     */
    private double round(double value, int places) {
        if (!Double.isFinite(value)) return 0.0;
        return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }
}
