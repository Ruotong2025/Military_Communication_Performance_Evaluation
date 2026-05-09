package com.ccnu.military.service;

import com.ccnu.military.dto.DynamicAhpAggregationRequest;
import com.ccnu.military.dto.DynamicAhpAggregationResponse;
import com.ccnu.military.dto.DynamicIndicatorTreeDTO;
import com.ccnu.military.dto.DynamicAhpAggregationResponse.CollectiveMatrix;
import com.ccnu.military.dto.DynamicAhpAggregationResponse.CombinedWeightItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ccnu.military.dto.DynamicAhpAggregationResponse.CrResult;
import com.ccnu.military.dto.DynamicAhpAggregationResponse.ExpertWeightDetail;
import com.ccnu.military.dto.DynamicAhpAggregationResponse.AggregationDetail;
import com.ccnu.military.dto.DynamicAhpAggregationResponse.SunburstData;
import com.ccnu.military.dto.DynamicAhpAggregationResponse.SunburstLevel;
import com.ccnu.military.entity.DynamicAhpAggregationResult;
import com.ccnu.military.entity.DynamicAhpMatrix;
import com.ccnu.military.entity.DynamicDimension;
import com.ccnu.military.entity.DynamicTemplate;
import com.ccnu.military.entity.ExpertBaseInfo;
import com.ccnu.military.entity.ExpertCredibilityScore;
import com.ccnu.military.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 动态AHP专家集结服务
 * 将多个专家对动态指标体系的AHP打分进行集结
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicAhpAggregationService {

    private final DynamicAhpMatrixRepository matrixRepository;
    private final DynamicAhpAggregationResultRepository aggregationResultRepository;
    private final DynamicTemplateRepository templateRepository;
    private final DynamicDimensionRepository dimensionRepository;
    private final ExpertBaseInfoRepository expertBaseInfoRepository;
    private final ExpertCredibilityScoreRepository credibilityRepository;
    private final ObjectMapper objectMapper;

    // AHP一致性指标RI值
    private static final Map<Integer, Double> RI_VALUES = Map.ofEntries(
            Map.entry(1, 0.0), Map.entry(2, 0.0), Map.entry(3, 0.58),
            Map.entry(4, 0.90), Map.entry(5, 1.12), Map.entry(6, 1.24),
            Map.entry(7, 1.32), Map.entry(8, 1.41), Map.entry(9, 1.45),
            Map.entry(10, 1.49), Map.entry(11, 1.51), Map.entry(12, 1.54)
    );

    // ==================== 公开接口 ====================

    /**
     * 获取可用模板列表
     */
    public List<Map<String, Object>> getAvailableTemplates() {
        List<DynamicTemplate> templates = templateRepository.findAllByOrderByCreatedAtDesc();
        return templates.stream().map(tpl -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", tpl.getId());
            map.put("templateName", tpl.getTemplateName());
            map.put("templateCode", tpl.getTemplateCode());
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 获取指定模板下有AHP打分的专家列表
     */
    public List<Map<String, Object>> getAvailableExperts(Long templateId) {
        // 查询有该模板AHP打分的专家
        List<DynamicAhpMatrix> matrices = matrixRepository.findByTemplateId(templateId);
        Set<Long> expertIds = matrices.stream()
                .map(DynamicAhpMatrix::getExpertId)
                .collect(Collectors.toSet());

        if (expertIds.isEmpty()) {
            return Collections.emptyList();
        }

        return expertIds.stream().sorted().map(id -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("expertId", id);
            // 获取专家名称
            String name = expertBaseInfoRepository.findById(id)
                    .map(ExpertBaseInfo::getExpertName)
                    .orElse("专家" + id);
            map.put("expertName", name);
            // 获取可信度
            int cred = credibilityRepository.findByExpertId(id)
                    .map(ExpertCredibilityScore::getTotalScore)
                    .map(BigDecimal::intValue)
                    .orElse(50);
            map.put("credibility", cred);
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 预览集结结果（不保存）
     */
    public DynamicAhpAggregationResponse previewAggregation(DynamicAhpAggregationRequest request) {
        // 1. 解析专家列表
        List<Long> expertIds = resolveExpertIds(request);

        // 2. 获取模板信息
        DynamicTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + request.getTemplateId()));

        // 3. 获取指标树结构
        DynamicIndicatorTreeDTO tree = getIndicatorTree(request.getTemplateId());

        // 4. 加载专家可信度
        Map<Long, Double> credMap = loadExpertCredibilities(expertIds);

        // 5. 加载所有打分数据
        List<DynamicAhpMatrix> allMatrices = matrixRepository.findByTemplateIdAndExpertIdIn(
                request.getTemplateId(), expertIds);

        // 6. 按 comparisonType 分组
        Map<DynamicAhpMatrix.ComparisonType, List<DynamicAhpMatrix>> byType = allMatrices.stream()
                .collect(Collectors.groupingBy(DynamicAhpMatrix::getComparisonType));

        // 7. 执行集结
        return buildAggregationResult(template, tree, expertIds, credMap, byType, null);
    }

    /**
     * 执行集结计算并保存
     */
    @Transactional
    public DynamicAhpAggregationResponse executeAggregation(DynamicAhpAggregationRequest request) {
        // 1. 解析专家列表
        List<Long> expertIds = resolveExpertIds(request);
        String expertIdsStr = buildExpertIdsString(expertIds);

        // 2. 获取模板信息
        DynamicTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + request.getTemplateId()));

        // 3. 获取指标树结构
        DynamicIndicatorTreeDTO tree = getIndicatorTree(request.getTemplateId());

        // 4. 加载专家可信度
        Map<Long, Double> credMap = loadExpertCredibilities(expertIds);

        // 5. 加载所有打分数据
        List<DynamicAhpMatrix> allMatrices = matrixRepository.findByTemplateIdAndExpertIdIn(
                request.getTemplateId(), expertIds);

        // 6. 按 comparisonType 分组
        Map<DynamicAhpMatrix.ComparisonType, List<DynamicAhpMatrix>> byType = allMatrices.stream()
                .collect(Collectors.groupingBy(DynamicAhpMatrix::getComparisonType));

        // 7. 执行集结
        DynamicAhpAggregationResponse response = buildAggregationResult(template, tree, expertIds, credMap, byType, null);

        // 8. 保存结果
        saveAggregationResult(template, expertIdsStr, response);

        return response;
    }

    /**
     * 获取集结结果列表
     */
    public List<DynamicAhpAggregationResult> getAggregationResults(Long templateId) {
        return aggregationResultRepository.findByTemplateIdOrderByUpdatedAtDesc(templateId);
    }

    /**
     * 获取单个集结结果
     */
    public DynamicAhpAggregationResponse getAggregationResult(String groupId) {
        DynamicAhpAggregationResult entity = aggregationResultRepository.findByGroupId(groupId)
                .orElseThrow(() -> new IllegalArgumentException("集结结果不存在: " + groupId));

        return parseAggregationResult(entity);
    }

    /**
     * 删除集结结果
     */
    @Transactional
    public void deleteAggregationResult(String groupId) {
        aggregationResultRepository.findByGroupId(groupId)
                .ifPresent(aggregationResultRepository::delete);
    }

    // ==================== 核心集结逻辑 ====================

    /**
     * 解析专家列表
     */
    private List<Long> resolveExpertIds(DynamicAhpAggregationRequest request) {
        if (Boolean.TRUE.equals(request.getUseAllExperts())) {
            List<DynamicAhpMatrix> matrices = matrixRepository.findByTemplateId(request.getTemplateId());
            return matrices.stream()
                    .map(DynamicAhpMatrix::getExpertId)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        } else if (request.getExpertIds() != null && !request.getExpertIds().isEmpty()) {
            return request.getExpertIds().stream().sorted().collect(Collectors.toList());
        } else {
            List<DynamicAhpMatrix> matrices = matrixRepository.findByTemplateId(request.getTemplateId());
            return matrices.stream()
                    .map(DynamicAhpMatrix::getExpertId)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    /**
     * 加载专家可信度
     */
    private Map<Long, Double> loadExpertCredibilities(List<Long> expertIds) {
        Map<Long, Double> credMap = new LinkedHashMap<>();
        for (Long expertId : expertIds) {
            int cred = credibilityRepository.findByExpertId(expertId)
                    .map(ExpertCredibilityScore::getTotalScore)
                    .map(BigDecimal::intValue)
                    .orElse(50);
            credMap.put(expertId, cred / 100.0); // 归一化到 0~1
        }
        return credMap;
    }

    /**
     * 构建集结结果
     */
    private DynamicAhpAggregationResponse buildAggregationResult(
            DynamicTemplate template,
            DynamicIndicatorTreeDTO tree,
            List<Long> expertIds,
            Map<Long, Double> credMap,
            Map<DynamicAhpMatrix.ComparisonType, List<DynamicAhpMatrix>> byType,
            String groupId) {

        // 调试日志
        log.info("========== 集结调试信息 ==========");
        log.info("模板: {}, 专家: {}", template.getTemplateName(), expertIds);
        log.info("树层级: {}", tree.getLevels().stream().map(l -> l.getName()).collect(Collectors.toList()));
        for (DynamicAhpMatrix.ComparisonType type : byType.keySet()) {
            log.info("  {} 类型数据条数: {}", type, byType.get(type).size());
            // 打印前几条数据
            for (int i = 0; i < Math.min(3, byType.get(type).size()); i++) {
                DynamicAhpMatrix m = byType.get(type).get(i);
                log.info("    样本{}: rowName={}, colName={}, rowCode={}, colCode={}, parentCode={}, levelName={}",
                        i, m.getRowName(), m.getColName(), m.getRowCode(), m.getColCode(), m.getParentCode(), m.getLevelName());
            }
        }
        log.info("==================================");
        log.info("树结构信息:");
        for (DynamicIndicatorTreeDTO.LevelNode ln : tree.getLevels()) {
            log.info("  层级: {} (id={}), 一级维度数: {}", ln.getName(), ln.getId(), ln.getPrimaryDimensions().size());
            for (DynamicIndicatorTreeDTO.PrimaryNode pn : ln.getPrimaryDimensions()) {
                log.info("    一级维度: name={}, code={}, 二级指标数: {}", pn.getName(), pn.getCode(), pn.getSecondaryDimensions().size());
            }
        }
        log.info("==================================");

        // 收集所有 comparisonKey 下的专家打分详情
        Map<String, List<AggregationDetail>> allAggregationDetails = new LinkedHashMap<>();

        // 构造集体判断矩阵
        Map<String, Double> collectiveScores = new LinkedHashMap<>();

        // 处理层级间矩阵
        List<DynamicAhpMatrix> levelBetween = byType.get(DynamicAhpMatrix.ComparisonType.LEVEL_BETWEEN);
        Map<String, CollectiveMatrix> levelMatrixResult = new LinkedHashMap<>();
        if (levelBetween != null && !levelBetween.isEmpty()) {
            List<String> levels = tree.getLevels().stream()
                    .map(DynamicIndicatorTreeDTO.LevelNode::getName)
                    .collect(Collectors.toList());
            if (levels.size() > 1) {
                // 构造 comparison_key → 集体打分
                Map<String, List<DynamicAhpMatrix>> levelScores = levelBetween.stream()
                        .collect(Collectors.groupingBy(m -> m.getRowName() + "|" + m.getColName()));

                // 集结每个层级对的打分
                for (Map.Entry<String, List<DynamicAhpMatrix>> entry : levelScores.entrySet()) {
                    List<AggregationDetail> details = aggregateScores(entry.getKey(), entry.getValue(), credMap);
                    allAggregationDetails.put(entry.getKey(), details);
                    double collectiveScore = details.stream()
                            .mapToDouble(d -> d.getNormalizedWeight().doubleValue() * d.getScore().doubleValue())
                            .sum();
                    collectiveScore = Math.max(1.0 / 9.0, Math.min(9.0, collectiveScore));
                    collectiveScores.put(entry.getKey(), collectiveScore);
                }

                // 构造矩阵并计算
                double[][] matrix = buildMatrix(levels, collectiveScores);
                double[] weights = calculateWeights(matrix);
                double cr = calculateCr(matrix, weights);

                levelMatrixResult.put("LEVEL_BETWEEN", CollectiveMatrix.builder()
                        .headers(levels)
                        .matrix(matrixToList(matrix))
                        .weights(doubleArrayToList(weights))
                        .cr(BigDecimal.valueOf(cr).setScale(4, RoundingMode.HALF_UP))
                        .ci(BigDecimal.valueOf((calculateLambdaMax(matrix, weights) - levels.size()) / (levels.size() - 1.0)).setScale(4, RoundingMode.HALF_UP))
                        .lambdaMax(BigDecimal.valueOf(calculateLambdaMax(matrix, weights)).setScale(4, RoundingMode.HALF_UP))
                        .build());
            }
        }

        // 处理一级维度间矩阵
        Map<String, CollectiveMatrix> primaryMatrices = new LinkedHashMap<>();
        Map<String, BigDecimal> primaryWeights = new LinkedHashMap<>();
        List<DynamicAhpMatrix> primaryBetween = byType.get(DynamicAhpMatrix.ComparisonType.PRIMARY_BETWEEN);

        if (primaryBetween != null && !primaryBetween.isEmpty()) {
            // 正常流程：处理PRIMARY_BETWEEN数据
            Map<String, List<DynamicAhpMatrix>> byLevel = primaryBetween.stream()
                    .collect(Collectors.groupingBy(DynamicAhpMatrix::getLevelName));

            for (Map.Entry<String, List<DynamicAhpMatrix>> levelEntry : byLevel.entrySet()) {
                String levelName = levelEntry.getKey();
                List<DynamicAhpMatrix> matrices = levelEntry.getValue();

                DynamicIndicatorTreeDTO.LevelNode levelNode = tree.getLevels().stream()
                        .filter(l -> l.getName().equals(levelName))
                        .findFirst().orElse(null);
                if (levelNode == null) continue;

                List<String> primaries = levelNode.getPrimaryDimensions().stream()
                        .map(DynamicIndicatorTreeDTO.PrimaryNode::getName)
                        .collect(Collectors.toList());
                if (primaries.size() < 2) continue;

                processPrimaryMatrix(levelName, matrices, primaries, credMap, primaryMatrices, primaryWeights,
                        allAggregationDetails, collectiveScores);
            }
        } else {
            // 没有PRIMARY_BETWEEN数据时，尝试从LEVEL_BETWEEN数据中解析
            if (levelBetween != null && !levelBetween.isEmpty()) {
                log.info("没有找到PRIMARY_BETWEEN数据，尝试从LEVEL_BETWEEN数据中解析一级维度矩阵");

                // 获取所有一级维度名称
                Set<String> allPrimaryNames = new HashSet<>();
                Set<String> allLevelNames = new HashSet<>();
                for (DynamicIndicatorTreeDTO.LevelNode ln : tree.getLevels()) {
                    allLevelNames.add(ln.getName());
                    log.info("树层级: {}, 一级维度数量: {}", ln.getName(), ln.getPrimaryDimensions().size());
                    for (DynamicIndicatorTreeDTO.PrimaryNode pn : ln.getPrimaryDimensions()) {
                        allPrimaryNames.add(pn.getName());
                        log.info("  一级维度: name={}, code={}", pn.getName(), pn.getCode());
                    }
                }
                log.info("所有一级维度名称: {}", allPrimaryNames);
                log.info("数据库LEVEL_BETWEEN记录数: {}", levelBetween.size());
                // 打印前3条的rowName/colName
                for (int i = 0; i < Math.min(3, levelBetween.size()); i++) {
                    DynamicAhpMatrix m = levelBetween.get(i);
                    log.info("  DB样本{}: levelName={}, rowName={}, colName={}, rowCode={}, colCode={}",
                            i, m.getLevelName(), m.getRowName(), m.getColName(), m.getRowCode(), m.getColCode());
                }

                // 过滤出是一级维度比较的记录
                List<DynamicAhpMatrix> primaryComparisons = levelBetween.stream()
                        .filter(m -> allPrimaryNames.contains(m.getRowName()) && allPrimaryNames.contains(m.getColName()))
                        .collect(Collectors.toList());

                log.info("从LEVEL_BETWEEN中筛选出一级维度比较记录: {}条", primaryComparisons.size());

                if (!primaryComparisons.isEmpty()) {
                    // 按层级分组处理
                    for (DynamicIndicatorTreeDTO.LevelNode levelNode : tree.getLevels()) {
                        String levelName = levelNode.getName();
                        List<String> primaries = levelNode.getPrimaryDimensions().stream()
                                .map(DynamicIndicatorTreeDTO.PrimaryNode::getName)
                                .collect(Collectors.toList());
                        if (primaries.size() < 2) continue;

                        // 筛选属于该层级一级维度的记录
                        Set<String> primarySet = new HashSet<>(primaries);
                        List<DynamicAhpMatrix> levelPrimaries = primaryComparisons.stream()
                                .filter(m -> primarySet.contains(m.getRowName()) && primarySet.contains(m.getColName()))
                                .collect(Collectors.toList());

                        if (!levelPrimaries.isEmpty()) {
                            processPrimaryMatrix(levelName, levelPrimaries, primaries, credMap, primaryMatrices,
                                    primaryWeights, allAggregationDetails, collectiveScores);
                        }
                    }
                }
            }
        }

        // 处理二级指标间矩阵
        Map<String, Map<String, CollectiveMatrix>> secondaryMatrices = new LinkedHashMap<>();
        Map<String, double[]> secondaryWeightsMap = new LinkedHashMap<>();
        List<DynamicAhpMatrix> secondaryBetween = byType.get(DynamicAhpMatrix.ComparisonType.SECONDARY_BETWEEN);

        if (secondaryBetween != null && !secondaryBetween.isEmpty()) {
            // 按 levelName + parentCode 分组
            Map<String, List<DynamicAhpMatrix>> byLevelAndParent = secondaryBetween.stream()
                    .collect(Collectors.groupingBy(m -> m.getLevelName() + "|" + m.getParentCode()));

            for (Map.Entry<String, List<DynamicAhpMatrix>> entry : byLevelAndParent.entrySet()) {
                String[] parts = entry.getKey().split("\\|", 2);
                String levelName = parts[0];
                String parentCode = parts[1];
                List<DynamicAhpMatrix> matrices = entry.getValue();

                // 获取二级指标名称列表
                DynamicIndicatorTreeDTO.LevelNode levelNode = tree.getLevels().stream()
                        .filter(l -> l.getName().equals(levelName))
                        .findFirst()
                        .orElse(null);

                if (levelNode == null) continue;

                DynamicIndicatorTreeDTO.PrimaryNode primaryNode = levelNode.getPrimaryDimensions().stream()
                        .filter(p -> p.getCode().equals(parentCode))
                        .findFirst()
                        .orElse(null);

                if (primaryNode == null) continue;

                List<String> secondaries = primaryNode.getSecondaryDimensions().stream()
                        .map(DynamicIndicatorTreeDTO.SecondaryNode::getName)
                        .collect(Collectors.toList());

                if (secondaries.size() < 2) continue;

                // 为当前一级维度创建独立的scores map
                Map<String, Double> secLevelScores = new LinkedHashMap<>();

                // 按 comparisonKey 分组
                Map<String, List<DynamicAhpMatrix>> secScores = matrices.stream()
                        .collect(Collectors.groupingBy(m -> m.getRowName() + "|" + m.getColName()));

                // 集结
                for (Map.Entry<String, List<DynamicAhpMatrix>> secEntry : secScores.entrySet()) {
                    String compKey = levelName + "|" + parentCode + "|" + secEntry.getKey();
                    List<AggregationDetail> details = aggregateScores(compKey, secEntry.getValue(), credMap);
                    allAggregationDetails.put(compKey, details);
                    double collectiveScore = details.stream()
                            .mapToDouble(d -> d.getNormalizedWeight().doubleValue() * d.getScore().doubleValue())
                            .sum();
                    collectiveScore = Math.max(1.0 / 9.0, Math.min(9.0, collectiveScore));
                    secLevelScores.put(secEntry.getKey(), collectiveScore);
                    collectiveScores.put(compKey, collectiveScore);
                }

                // 构造矩阵并计算
                double[][] matrix = buildMatrix(secondaries, secLevelScores);
                double[] weights = calculateWeights(matrix);
                double cr = calculateCr(matrix, weights);

                // 保存二级指标权重数组（使用 parentCode 作为 key）
                secondaryWeightsMap.put(entry.getKey(), weights);

                // 使用 primaryName 作为 key（与前端 getPrimariesForLevel 保持一致）
                String primaryName = primaryNode.getName();
                secondaryMatrices.computeIfAbsent(levelName, k -> new LinkedHashMap<>())
                        .put(primaryName, CollectiveMatrix.builder()
                                .headers(secondaries)
                                .matrix(matrixToList(matrix))
                                .weights(doubleArrayToList(weights))
                                .cr(BigDecimal.valueOf(cr).setScale(4, RoundingMode.HALF_UP))
                                .ci(BigDecimal.valueOf((calculateLambdaMax(matrix, weights) - secondaries.size()) / (secondaries.size() - 1.0)).setScale(4, RoundingMode.HALF_UP))
                                .lambdaMax(BigDecimal.valueOf(calculateLambdaMax(matrix, weights)).setScale(4, RoundingMode.HALF_UP))
                                .build());
            }
        }

        // 计算综合权重
        List<CombinedWeightItem> combinedWeights = calculateCombinedWeights(tree, primaryWeights, secondaryWeightsMap);

        // 构造专家权重明细
        List<ExpertWeightDetail> expertDetails = buildExpertWeightDetails(expertIds, credMap, allAggregationDetails);

        // 构造 CR 结果
        Map<String, CrResult> crResults = new LinkedHashMap<>();
        if (levelMatrixResult.containsKey("LEVEL_BETWEEN")) {
            CollectiveMatrix lm = levelMatrixResult.get("LEVEL_BETWEEN");
            crResults.put("LEVEL_BETWEEN", CrResult.builder()
                    .cr(lm.getCr())
                    .ci(lm.getCi())
                    .lambdaMax(lm.getLambdaMax())
                    .consistent(lm.getCr().compareTo(BigDecimal.valueOf(0.1)) <= 0)
                    .build());
        }
        for (Map.Entry<String, CollectiveMatrix> entry : primaryMatrices.entrySet()) {
            CollectiveMatrix m = entry.getValue();
            crResults.put(entry.getKey() + ":PRIMARY_BETWEEN", CrResult.builder()
                    .cr(m.getCr())
                    .ci(m.getCi())
                    .lambdaMax(m.getLambdaMax())
                    .consistent(m.getCr().compareTo(BigDecimal.valueOf(0.1)) <= 0)
                    .build());
        }
        for (Map.Entry<String, Map<String, CollectiveMatrix>> levelEntry : secondaryMatrices.entrySet()) {
            for (Map.Entry<String, CollectiveMatrix> entry : levelEntry.getValue().entrySet()) {
                CollectiveMatrix m = entry.getValue();
                crResults.put(levelEntry.getKey() + ":" + entry.getKey() + ":SECONDARY_BETWEEN", CrResult.builder()
                        .cr(m.getCr())
                        .ci(m.getCi())
                        .lambdaMax(m.getLambdaMax())
                        .consistent(m.getCr().compareTo(BigDecimal.valueOf(0.1)) <= 0)
                        .build());
            }
        }

        // 构造旭日图数据
        SunburstData sunburstData = buildSunburstData(tree, combinedWeights);

        // 收集层级权重
        Map<String, BigDecimal> levelWeightsMap = new LinkedHashMap<>();
        for (CombinedWeightItem item : combinedWeights) {
            levelWeightsMap.putIfAbsent(item.getLevelName(), item.getLevelWeight());
        }

        return DynamicAhpAggregationResponse.builder()
                .groupId(groupId)
                .templateId(template.getId())
                .templateName(template.getTemplateName())
                .expertCount(expertIds.size())
                .expertIds(expertIds)
                .expertWeights(expertDetails)
                .crResults(crResults)
                .levelBetweenMatrix(levelMatrixResult.get("LEVEL_BETWEEN"))
                .primaryMatrices(primaryMatrices)
                .secondaryMatrices(secondaryMatrices)
                .levelWeights(levelWeightsMap)
                .combinedWeights(combinedWeights)
                .sunburstData(sunburstData)
                .aggregationDetails(allAggregationDetails)
                .build();
    }

    /**
     * 集结打分
     */
    private List<AggregationDetail> aggregateScores(String compKey, List<DynamicAhpMatrix> scores, Map<Long, Double> credMap) {
        List<AggregationDetail> details = new ArrayList<>();

        // 计算 rawWeight
        Map<Long, Double> rawWeights = new LinkedHashMap<>();
        for (DynamicAhpMatrix s : scores) {
            Double cred = credMap.getOrDefault(s.getExpertId(), 0.5);
            Double conf = s.getConfidence() != null ? s.getConfidence().doubleValue() : 0.5;
            double raw = cred * 0.5 + conf * 0.5;
            rawWeights.put(s.getExpertId(), raw);
        }

        // 归一化
        double total = rawWeights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total <= 0) total = 1.0;

        Map<Long, Double> normalizedWeights = new LinkedHashMap<>();
        for (Map.Entry<Long, Double> entry : rawWeights.entrySet()) {
            normalizedWeights.put(entry.getKey(), entry.getValue() / total);
        }

        // 构造详情
        for (DynamicAhpMatrix s : scores) {
            Long expertId = s.getExpertId();
            Double cred = credMap.getOrDefault(expertId, 0.5);
            Double conf = s.getConfidence() != null ? s.getConfidence().doubleValue() : 0.5;
            Double raw = rawWeights.get(expertId);
            Double norm = normalizedWeights.get(expertId);
            Double score = s.getScore() != null ? s.getScore().doubleValue() : 1.0;

            String expertName = expertBaseInfoRepository.findById(expertId)
                    .map(ExpertBaseInfo::getExpertName)
                    .orElse("专家" + expertId);

            details.add(AggregationDetail.builder()
                    .expertId(expertId)
                    .expertName(expertName)
                    .credibility(BigDecimal.valueOf(cred).setScale(2, RoundingMode.HALF_UP))
                    .confidence(BigDecimal.valueOf(conf).setScale(2, RoundingMode.HALF_UP))
                    .rawWeight(BigDecimal.valueOf(raw).setScale(4, RoundingMode.HALF_UP))
                    .normalizedWeight(BigDecimal.valueOf(norm).setScale(4, RoundingMode.HALF_UP))
                    .score(BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP))
                    .weightedScore(BigDecimal.valueOf(norm * score).setScale(4, RoundingMode.HALF_UP))
                    .build());
        }

        return details;
    }

    /**
     * 处理一级维度矩阵的集结计算
     */
    private void processPrimaryMatrix(
            String levelName,
            List<DynamicAhpMatrix> matrices,
            List<String> primaries,
            Map<Long, Double> credMap,
            Map<String, CollectiveMatrix> primaryMatrices,
            Map<String, BigDecimal> primaryWeights,
            Map<String, List<AggregationDetail>> allAggregationDetails,
            Map<String, Double> collectiveScores) {

        // 为当前层级创建独立的scores map
        Map<String, Double> levelScores = new LinkedHashMap<>();

        // 按 comparisonKey 分组
        Map<String, List<DynamicAhpMatrix>> primaryScores = matrices.stream()
                .collect(Collectors.groupingBy(m -> m.getRowName() + "|" + m.getColName()));

        // 集结
        for (Map.Entry<String, List<DynamicAhpMatrix>> entry : primaryScores.entrySet()) {
            String compKey = levelName + "|" + entry.getKey();
            List<AggregationDetail> details = aggregateScores(compKey, entry.getValue(), credMap);
            allAggregationDetails.put(compKey, details);
            double collectiveScore = details.stream()
                    .mapToDouble(d -> d.getNormalizedWeight().doubleValue() * d.getScore().doubleValue())
                    .sum();
            collectiveScore = Math.max(1.0 / 9.0, Math.min(9.0, collectiveScore));
            levelScores.put(entry.getKey(), collectiveScore);
            collectiveScores.put(compKey, collectiveScore);
        }

        // 构造矩阵并计算
        double[][] matrix = buildMatrix(primaries, levelScores);
        double[] weights = calculateWeights(matrix);
        double cr = calculateCr(matrix, weights);

        // 保存一级维度权重
        for (int i = 0; i < primaries.size(); i++) {
            primaryWeights.put(primaries.get(i), BigDecimal.valueOf(weights[i]).setScale(6, RoundingMode.HALF_UP));
        }

        primaryMatrices.put(levelName, CollectiveMatrix.builder()
                .headers(primaries)
                .matrix(matrixToList(matrix))
                .weights(doubleArrayToList(weights))
                .cr(BigDecimal.valueOf(cr).setScale(4, RoundingMode.HALF_UP))
                .ci(BigDecimal.valueOf((calculateLambdaMax(matrix, weights) - primaries.size()) / (primaries.size() - 1.0)).setScale(4, RoundingMode.HALF_UP))
                .lambdaMax(BigDecimal.valueOf(calculateLambdaMax(matrix, weights)).setScale(4, RoundingMode.HALF_UP))
                .build());

        log.info("一级维度矩阵处理完成: levelName={}, primaries={}", levelName, primaries);
    }

    /**
     * 构造判断矩阵
     */
    private double[][] buildMatrix(List<String> elements, Map<String, Double> collectiveScores) {
        int n = elements.size();
        double[][] matrix = new double[n][n];

        // 建立名称到索引的映射
        Map<String, Integer> nameToIdx = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            nameToIdx.put(elements.get(i), i);
        }

        // 初始化单位矩阵
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = (i == j) ? 1.0 : 0.0;
            }
        }

        // 填充矩阵
        for (Map.Entry<String, Double> entry : collectiveScores.entrySet()) {
            String key = entry.getKey();
            Double score = entry.getValue();
            if (score == null || !Double.isFinite(score)) continue;

            String[] parts = key.split("\\|");
            if (parts.length < 2) continue;

            String nameA = parts[parts.length - 2];
            String nameB = parts[parts.length - 1];

            Integer idxA = nameToIdx.get(nameA);
            Integer idxB = nameToIdx.get(nameB);
            if (idxA == null || idxB == null) continue;

            matrix[idxA][idxB] = score;
            matrix[idxB][idxA] = 1.0 / score;
        }

        return matrix;
    }

    /**
     * 计算AHP权重（列和归一化法）
     */
    private double[] calculateWeights(double[][] matrix) {
        int n = matrix.length;

        // 计算每列的和
        double[] colSums = new double[n];
        for (int j = 0; j < n; j++) {
            double sum = 0;
            for (int i = 0; i < n; i++) {
                sum += matrix[i][j];
            }
            colSums[j] = sum;
        }

        // 归一化并计算权重
        double[] weights = new double[n];
        for (int i = 0; i < n; i++) {
            double rowSum = 0;
            for (int j = 0; j < n; j++) {
                rowSum += matrix[i][j] / colSums[j];
            }
            weights[i] = rowSum / n;
        }

        return weights;
    }

    /**
     * 计算最大特征值
     */
    private double calculateLambdaMax(double[][] matrix, double[] weights) {
        int n = matrix.length;
        double lambdaSum = 0;
        for (int i = 0; i < n; i++) {
            double sum = 0;
            for (int j = 0; j < n; j++) {
                sum += matrix[i][j] * weights[j];
            }
            if (weights[i] > 0) {
                lambdaSum += sum / weights[i];
            }
        }
        return lambdaSum / n;
    }

    /**
     * 计算CR
     */
    private double calculateCr(double[][] matrix, double[] weights) {
        int n = matrix.length;
        double lambdaMax = calculateLambdaMax(matrix, weights);
        double ci = (lambdaMax - n) / (n - 1);
        double ri = RI_VALUES.getOrDefault(n, 1.54);
        return ri > 0 ? ci / ri : 0;
    }

    /**
     * 计算综合权重
     */
    private List<CombinedWeightItem> calculateCombinedWeights(
            DynamicIndicatorTreeDTO tree,
            Map<String, BigDecimal> primaryWeights,
            Map<String, double[]> secondaryWeightsMap) {

        List<CombinedWeightItem> result = new ArrayList<>();
        BigDecimal totalWeight = BigDecimal.ZERO;

        // 计算层级权重（暂时平均分配，后续可从levelMatrixResult获取）
        List<String> levelNames = tree.getLevels().stream()
                .map(DynamicIndicatorTreeDTO.LevelNode::getName)
                .collect(Collectors.toList());
        BigDecimal levelWeightAvg = BigDecimal.ONE.divide(
                BigDecimal.valueOf(levelNames.size()), 6, RoundingMode.HALF_UP);

        Map<String, BigDecimal> levelWeights = new LinkedHashMap<>();
        for (String level : levelNames) {
            levelWeights.put(level, levelWeightAvg);
        }

        // 遍历树结构
        for (DynamicIndicatorTreeDTO.LevelNode levelNode : tree.getLevels()) {
            String levelName = levelNode.getName();
            BigDecimal levelWeight = levelWeights.getOrDefault(levelName, levelWeightAvg);

            for (DynamicIndicatorTreeDTO.PrimaryNode primaryNode : levelNode.getPrimaryDimensions()) {
                String primaryName = primaryNode.getName();
                BigDecimal primaryWeight = primaryWeights.getOrDefault(primaryName, BigDecimal.ZERO);

                // 获取二级指标权重数组
                double[] secWeights = secondaryWeightsMap.get(levelName + "|" + primaryNode.getCode());
                List<DynamicIndicatorTreeDTO.SecondaryNode> secondaries = primaryNode.getSecondaryDimensions();
                int secCount = secondaries.size();

                // 计算二级指标权重（如果没有AHP数据则均匀分配）
                List<BigDecimal> secondaryWeightList;
                if (secWeights != null && secWeights.length == secCount) {
                    // 有AHP数据，直接使用
                    secondaryWeightList = Arrays.stream(secWeights)
                            .mapToObj(w -> BigDecimal.valueOf(w).setScale(6, RoundingMode.HALF_UP))
                            .collect(Collectors.toList());
                } else if (secCount > 0) {
                    // 没有AHP数据，均匀分配
                    BigDecimal uniformWeight = BigDecimal.ONE.divide(
                            BigDecimal.valueOf(secCount), 6, RoundingMode.HALF_UP);
                    secondaryWeightList = Collections.nCopies(secCount, uniformWeight);
                } else {
                    secondaryWeightList = Collections.emptyList();
                }

                for (int i = 0; i < secondaries.size(); i++) {
                    DynamicIndicatorTreeDTO.SecondaryNode secondaryNode = secondaries.get(i);
                    String secondaryName = secondaryNode.getName();
                    BigDecimal secondaryWeight = secondaryWeightList.size() > i ? secondaryWeightList.get(i) : BigDecimal.ZERO;

                    BigDecimal combinedWeight = levelWeight.multiply(primaryWeight).multiply(secondaryWeight);
                    totalWeight = totalWeight.add(combinedWeight);

                    result.add(CombinedWeightItem.builder()
                            .levelName(levelName)
                            .primaryCode(primaryNode.getCode())
                            .primaryName(primaryName)
                            .secondaryCode(secondaryNode.getCode())
                            .secondaryName(secondaryName)
                            .levelWeight(levelWeight)
                            .primaryWeight(primaryWeight)
                            .secondaryWeight(secondaryWeight)
                            .combinedWeight(combinedWeight)
                            .build());
                }
            }
        }

        // 归一化
        if (totalWeight.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal normFactor = BigDecimal.ONE.divide(totalWeight, 10, RoundingMode.HALF_UP);
            for (CombinedWeightItem item : result) {
                item.setCombinedWeight(item.getCombinedWeight().multiply(normFactor).setScale(6, RoundingMode.HALF_UP));
                item.setLevelWeight(item.getLevelWeight().multiply(normFactor).setScale(6, RoundingMode.HALF_UP));
            }
        }

        return result;
    }

    /**
     * 构造专家权重明细
     */
    private List<ExpertWeightDetail> buildExpertWeightDetails(
            List<Long> expertIds,
            Map<Long, Double> credMap,
            Map<String, List<AggregationDetail>> allDetails) {

        List<ExpertWeightDetail> result = new ArrayList<>();

        for (Long expertId : expertIds) {
            int participationCount = 0;
            double totalNormWeight = 0;
            int detailCount = 0;

            for (List<AggregationDetail> details : allDetails.values()) {
                for (AggregationDetail detail : details) {
                    if (detail.getExpertId().equals(expertId)) {
                        participationCount++;
                        totalNormWeight += detail.getNormalizedWeight().doubleValue();
                        detailCount++;
                    }
                }
            }

            String expertName = expertBaseInfoRepository.findById(expertId)
                    .map(ExpertBaseInfo::getExpertName)
                    .orElse("专家" + expertId);

            BigDecimal avgNormWeight = detailCount > 0
                    ? BigDecimal.valueOf(totalNormWeight / detailCount).setScale(4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            result.add(ExpertWeightDetail.builder()
                    .expertId(expertId)
                    .expertName(expertName)
                    .credibility(BigDecimal.valueOf(credMap.getOrDefault(expertId, 0.5) * 100).setScale(1, RoundingMode.HALF_UP))
                    .participationCount(participationCount)
                    .avgNormalizedWeight(avgNormWeight)
                    .build());
        }

        return result;
    }

    /**
     * 构造旭日图数据
     */
    private SunburstData buildSunburstData(DynamicIndicatorTreeDTO tree, List<CombinedWeightItem> combinedWeights) {
        List<SunburstLevel> levelNodes = new ArrayList<>();

        // 按层级分组
        Map<String, List<CombinedWeightItem>> byLevel = combinedWeights.stream()
                .collect(Collectors.groupingBy(CombinedWeightItem::getLevelName));

        for (Map.Entry<String, List<CombinedWeightItem>> levelEntry : byLevel.entrySet()) {
            String levelName = levelEntry.getKey();
            List<CombinedWeightItem> levelItems = levelEntry.getValue();

            // 按一级维度分组
            Map<String, List<CombinedWeightItem>> byPrimary = levelItems.stream()
                    .collect(Collectors.groupingBy(CombinedWeightItem::getPrimaryName));

            List<SunburstLevel> primaryNodes = new ArrayList<>();
            BigDecimal levelTotal = levelItems.stream()
                    .map(CombinedWeightItem::getCombinedWeight)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            for (Map.Entry<String, List<CombinedWeightItem>> primaryEntry : byPrimary.entrySet()) {
                String primaryName = primaryEntry.getKey();
                List<CombinedWeightItem> primaryItems = primaryEntry.getValue();

                List<SunburstLevel> secondaryNodes = primaryItems.stream()
                        .map(item -> SunburstLevel.builder()
                                .name(item.getSecondaryName())
                                .value(item.getCombinedWeight().multiply(BigDecimal.valueOf(100)))
                                .build())
                        .collect(Collectors.toList());

                BigDecimal primaryTotal = primaryItems.stream()
                        .map(CombinedWeightItem::getCombinedWeight)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                primaryNodes.add(SunburstLevel.builder()
                        .name(primaryName)
                        .value(primaryTotal.multiply(BigDecimal.valueOf(100)))
                        .children(secondaryNodes)
                        .build());
            }

            levelNodes.add(SunburstLevel.builder()
                    .name(levelName)
                    .value(levelTotal.multiply(BigDecimal.valueOf(100)))
                    .children(primaryNodes)
                    .build());
        }

        return SunburstData.builder()
                .name("综合权重")
                .children(levelNodes)
                .build();
    }

    // ==================== 辅助方法 ====================

    private DynamicIndicatorTreeDTO getIndicatorTree(Long templateId) {
        DynamicTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在"));

        DynamicIndicatorTreeDTO dto = new DynamicIndicatorTreeDTO();
        dto.setId(template.getId());
        dto.setTemplateName(template.getTemplateName());

        // 从数据库获取所有维度数据
        List<DynamicDimension> dimensions = dimensionRepository.findByTemplateIdOrderBySortOrder(templateId);

        // 分离 LEVEL、PRIMARY、SECONDARY 类型的维度
        List<DynamicDimension> levelDims = dimensions.stream()
                .filter(d -> d.getDimensionLevel() == DynamicDimension.DimensionLevel.LEVEL)
                .collect(Collectors.toList());
        List<DynamicDimension> primaryDims = dimensions.stream()
                .filter(d -> d.getDimensionLevel() == DynamicDimension.DimensionLevel.PRIMARY)
                .collect(Collectors.toList());
        List<DynamicDimension> secondaryDims = dimensions.stream()
                .filter(d -> d.getDimensionLevel() == DynamicDimension.DimensionLevel.SECONDARY)
                .collect(Collectors.toList());

        // 检查是否有真正的层级数据
        boolean hasRealLevels = levelDims.stream()
                .anyMatch(l -> primaryDims.stream()
                        .anyMatch(p -> p.getParentId() != null && p.getParentId().equals(l.getId())));

        List<DynamicIndicatorTreeDTO.LevelNode> levelNodes;

        if (hasRealLevels) {
            // 有真正的层级结构，使用原有逻辑
            levelNodes = buildLevelNodesFromDimensions(levelDims, primaryDims, secondaryDims);
        } else {
            // 没有真正的层级结构，从 AHP 数据获取层级信息
            levelNodes = buildLevelNodesFromAhpData(templateId, primaryDims, secondaryDims);
        }

        dto.setLevels(levelNodes);
        return dto;
    }

    /**
     * 从维度数据构建层级节点（有真正的层级时使用）
     */
    private List<DynamicIndicatorTreeDTO.LevelNode> buildLevelNodesFromDimensions(
            List<DynamicDimension> levelDims,
            List<DynamicDimension> primaryDims,
            List<DynamicDimension> secondaryDims) {

        List<DynamicIndicatorTreeDTO.LevelNode> levelNodes = new ArrayList<>();

        // 按 ID 分组一级维度
        Map<Long, List<DynamicDimension>> primaryByParent = primaryDims.stream()
                .collect(Collectors.groupingBy(d -> d.getParentId() != null ? d.getParentId() : 0L));

        // 按 ID 分组二级维度
        Map<Long, List<DynamicDimension>> secondaryByParent = secondaryDims.stream()
                .collect(Collectors.groupingBy(d -> d.getParentId() != null ? d.getParentId() : 0L));

        for (DynamicDimension levelDim : levelDims) {
            DynamicIndicatorTreeDTO.LevelNode levelNode = new DynamicIndicatorTreeDTO.LevelNode();
            levelNode.setId(levelDim.getId());
            levelNode.setName(levelDim.getName());

            List<DynamicIndicatorTreeDTO.PrimaryNode> primaryNodes = new ArrayList<>();
            List<DynamicDimension> primaries = primaryByParent.getOrDefault(levelDim.getId(), Collections.emptyList());

            for (DynamicDimension primary : primaries) {
                DynamicIndicatorTreeDTO.PrimaryNode primaryNode = new DynamicIndicatorTreeDTO.PrimaryNode();
                primaryNode.setId(primary.getId());
                primaryNode.setName(primary.getName());
                primaryNode.setCode(primary.getCode());

                List<DynamicIndicatorTreeDTO.SecondaryNode> secondaryNodes = new ArrayList<>();
                List<DynamicDimension> secondaries = secondaryByParent.getOrDefault(primary.getId(), Collections.emptyList());

                for (DynamicDimension sec : secondaries) {
                    DynamicIndicatorTreeDTO.SecondaryNode secondaryNode = new DynamicIndicatorTreeDTO.SecondaryNode();
                    secondaryNode.setId(sec.getId());
                    secondaryNode.setName(sec.getName());
                    secondaryNode.setCode(sec.getCode());
                    secondaryNode.setMetricType(sec.getMetricType() != null ? sec.getMetricType().name() : null);
                    secondaryNodes.add(secondaryNode);
                }

                primaryNode.setSecondaryDimensions(secondaryNodes);
                primaryNodes.add(primaryNode);
            }

            levelNode.setPrimaryDimensions(primaryNodes);
            levelNodes.add(levelNode);
        }

        return levelNodes;
    }

    /**
     * 从 AHP 矩阵数据构建层级节点（没有真正的层级时使用）
     */
    private List<DynamicIndicatorTreeDTO.LevelNode> buildLevelNodesFromAhpData(
            Long templateId,
            List<DynamicDimension> primaryDims,
            List<DynamicDimension> secondaryDims) {

        List<DynamicIndicatorTreeDTO.LevelNode> levelNodes = new ArrayList<>();

        // 从 SECONDARY_BETWEEN 数据获取唯一的 levelName
        List<DynamicAhpMatrix> secondaryMatrices = matrixRepository
                .findByTemplateIdAndComparisonTypeIn(templateId,
                        List.of(DynamicAhpMatrix.ComparisonType.SECONDARY_BETWEEN));

        Set<String> levelNames = secondaryMatrices.stream()
                .map(DynamicAhpMatrix::getLevelName)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));

        // 从 PRIMARY_BETWEEN 数据获取层级
        List<DynamicAhpMatrix> primaryMatrices = matrixRepository
                .findByTemplateIdAndComparisonTypeIn(templateId,
                        List.of(DynamicAhpMatrix.ComparisonType.PRIMARY_BETWEEN));

        levelNames.addAll(primaryMatrices.stream()
                .map(DynamicAhpMatrix::getLevelName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        // 如果仍然没有层级，使用 "默认层级"
        if (levelNames.isEmpty()) {
            levelNames.add("默认层级");
        }

        // 按 ID 分组一级维度
        Map<Long, List<DynamicDimension>> primaryByParent = primaryDims.stream()
                .collect(Collectors.groupingBy(d -> d.getParentId() != null ? d.getParentId() : 0L));

        // 按 ID 分组二级维度
        Map<Long, List<DynamicDimension>> secondaryByParent = secondaryDims.stream()
                .collect(Collectors.groupingBy(d -> d.getParentId() != null ? d.getParentId() : 0L));

        // 为每个层级创建节点
        for (String levelName : levelNames) {
            DynamicIndicatorTreeDTO.LevelNode levelNode = new DynamicIndicatorTreeDTO.LevelNode();
            levelNode.setId(0L); // 虚拟 ID
            levelNode.setName(levelName);

            // 该层级下的所有一级维度
            Set<String> primaryNamesInLevel = primaryMatrices.stream()
                    .filter(m -> levelName.equals(m.getLevelName()))
                    .flatMap(m -> Stream.of(m.getRowName(), m.getColName()))
                    .collect(Collectors.toCollection(TreeSet::new));

            List<DynamicIndicatorTreeDTO.PrimaryNode> primaryNodes = new ArrayList<>();

            // 为该层级下的一级维度创建节点
            for (String primaryName : primaryNamesInLevel) {
                // 查找匹配的一级维度
                DynamicDimension primaryDim = primaryDims.stream()
                        .filter(p -> p.getName().equals(primaryName))
                        .findFirst()
                        .orElse(null);

                if (primaryDim == null) {
                    // 如果数据库中没有，创建一个虚拟的
                    primaryDim = new DynamicDimension();
                    primaryDim.setId(0L);
                    primaryDim.setName(primaryName);
                    primaryDim.setCode(primaryName);
                }

                DynamicIndicatorTreeDTO.PrimaryNode primaryNode = new DynamicIndicatorTreeDTO.PrimaryNode();
                primaryNode.setId(primaryDim.getId());
                primaryNode.setName(primaryDim.getName());
                primaryNode.setCode(primaryDim.getCode());

                // 该一级维度下的二级指标
                List<DynamicIndicatorTreeDTO.SecondaryNode> secondaryNodes = new ArrayList<>();
                List<DynamicDimension> secondaries = secondaryByParent.getOrDefault(primaryDim.getId(), Collections.emptyList());

                for (DynamicDimension sec : secondaries) {
                    DynamicIndicatorTreeDTO.SecondaryNode secondaryNode = new DynamicIndicatorTreeDTO.SecondaryNode();
                    secondaryNode.setId(sec.getId());
                    secondaryNode.setName(sec.getName());
                    secondaryNode.setCode(sec.getCode());
                    secondaryNode.setMetricType(sec.getMetricType() != null ? sec.getMetricType().name() : null);
                    secondaryNodes.add(secondaryNode);
                }

                primaryNode.setSecondaryDimensions(secondaryNodes);
                primaryNodes.add(primaryNode);
            }

            levelNode.setPrimaryDimensions(primaryNodes);
            levelNodes.add(levelNode);
        }

        return levelNodes;
    }

    private String buildExpertIdsString(List<Long> expertIds) {
        return expertIds.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private List<List<BigDecimal>> matrixToList(double[][] matrix) {
        List<List<BigDecimal>> result = new ArrayList<>();
        for (double[] row : matrix) {
            List<BigDecimal> rowList = new ArrayList<>();
            for (double v : row) {
                rowList.add(BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP));
            }
            result.add(rowList);
        }
        return result;
    }

    private List<BigDecimal> doubleArrayToList(double[] array) {
        List<BigDecimal> result = new ArrayList<>();
        for (double v : array) {
            result.add(BigDecimal.valueOf(v).setScale(6, RoundingMode.HALF_UP));
        }
        return result;
    }

    /**
     * 保存集结结果
     */
    private void saveAggregationResult(DynamicTemplate template, String expertIdsStr, DynamicAhpAggregationResponse response) {
        String groupId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        response.setGroupId(groupId);

        DynamicAhpAggregationResult entity = DynamicAhpAggregationResult.builder()
                .groupId(groupId)
                .templateId(template.getId())
                .templateName(template.getTemplateName())
                .expertIds(expertIdsStr)
                .expertCount(response.getExpertCount())
                .levelWeightsJson(toJson(response.getLevelWeights()))
                .dimensionWeightsJson(toJson(response.getPrimaryMatrices()))
                .combinedWeightsJson(toJson(response.getCombinedWeights()))
                .build();

        aggregationResultRepository.save(entity);
        log.info("集结结果已保存: groupId={}, templateId={}, expertCount={}", groupId, template.getId(), response.getExpertCount());
    }

    private DynamicAhpAggregationResponse parseAggregationResult(DynamicAhpAggregationResult entity) {
        try {
            DynamicAhpAggregationResponse response = new DynamicAhpAggregationResponse();
            response.setGroupId(entity.getGroupId());
            response.setTemplateId(entity.getTemplateId());
            response.setTemplateName(entity.getTemplateName());
            response.setExpertCount(entity.getExpertCount());

            if (entity.getLevelWeightsJson() != null) {
                Map<String, BigDecimal> levelWeights = objectMapper.readValue(entity.getLevelWeightsJson(),
                        new TypeReference<Map<String, BigDecimal>>() {});
                response.setLevelWeights(levelWeights);
            }
            if (entity.getCombinedWeightsJson() != null) {
                List<CombinedWeightItem> combinedWeights = objectMapper.readValue(entity.getCombinedWeightsJson(),
                        new TypeReference<List<CombinedWeightItem>>() {});
                response.setCombinedWeights(combinedWeights);
            }
            return response;
        } catch (JsonProcessingException e) {
            log.error("解析集结结果失败", e);
            throw new RuntimeException("解析集结结果失败: " + e.getMessage());
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("JSON序列化失败: {}", e.getMessage());
            return null;
        }
    }
}
