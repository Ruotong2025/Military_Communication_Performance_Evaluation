package com.ccnu.military.service;

import com.ccnu.military.dto.CostEffectivenessCalculateRequest;
import com.ccnu.military.dto.CostEffectivenessCalculateResponse;
import com.ccnu.military.dto.CostEffectivenessResultDTO;
import com.ccnu.military.entity.AhpCommunicationScoringResult;
import com.ccnu.military.entity.CostEvaluationBatch;
import com.ccnu.military.entity.CostEvaluationRecord;
import com.ccnu.military.entity.CostIndicatorConfig;
import com.ccnu.military.entity.ExpertWeightedEvaluationResult;
import com.ccnu.military.entity.PenaltyModelResult;
import com.ccnu.military.repository.AhpCommunicationScoringResultRepository;
import com.ccnu.military.repository.CostEvaluationBatchRepository;
import com.ccnu.military.repository.CostEvaluationRecordRepository;
import com.ccnu.military.repository.CostIndicatorConfigRepository;
import com.ccnu.military.repository.ExpertWeightedEvaluationResultRepository;
import com.ccnu.military.repository.PenaltyModelResultRepository;
import com.ccnu.military.repository.RecordsMilitaryOperationInfoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 效费分析服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CostEffectivenessService {

    private final CostIndicatorConfigRepository indicatorConfigRepository;
    private final CostEvaluationRecordRepository recordRepository;
    private final CostEvaluationBatchRepository batchRepository;
    private final PenaltyModelResultRepository penaltyResultRepository;
    private final ExpertWeightedEvaluationResultRepository weightedResultRepository;
    private final RecordsMilitaryOperationInfoRepository operationInfoRepository;
    private final AhpCommunicationScoringResultRepository scoringResultRepository;
    private final ObjectMapper objectMapper;

    /**
     * 获取所有启用的成本指标配置
     */
    public List<CostIndicatorConfig> getActiveIndicators() {
        return indicatorConfigRepository.findByIsActiveTrueOrderBySortOrderAsc();
    }

    /**
     * 获取所有指标类别
     */
    public List<String> getAllCategories() {
        return indicatorConfigRepository.findAllCategories();
    }

    /**
     * 按类别获取指标
     */
    public List<CostIndicatorConfig> getIndicatorsByCategory(String category) {
        return indicatorConfigRepository.findByIsActiveTrueAndCategoryOrderBySortOrderAsc(category);
    }

    /**
     * 获取成本评估结果
     */
    public List<CostEffectivenessResultDTO> getResults(String evaluationId) {
        List<CostEvaluationRecord> records = recordRepository.findByEvaluationId(evaluationId);
        return records.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * 获取作战任务原始成本数据预览
     */
    public List<Map<String, Object>> getRawDataPreview(String evaluationId, List<String> operationIds) {
        List<CostIndicatorConfig> indicators = indicatorConfigRepository.findByIsActiveTrueOrderBySortOrderAsc();
        Map<String, Map<String, Object>> rawDataMap = fetchOperationData(operationIds);

        // 计算每个指标的 min/max 边界
        Map<String, BigDecimal> globalMinValues = new HashMap<>();
        Map<String, BigDecimal> globalMaxValues = new HashMap<>();
        calculateNormalizationBounds(indicators, rawDataMap, globalMinValues, globalMaxValues);

        List<Map<String, Object>> result = new ArrayList<>();
        for (String opId : operationIds) {
            Map<String, Object> rawData = rawDataMap.get(opId);
            if (rawData == null) continue;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("operationId", opId);
            row.put("evaluationId", evaluationId);

            // 只取指标配置中定义了的字段
            for (CostIndicatorConfig ind : indicators) {
                String field = ind.getSourceField();
                if (field != null && rawData.containsKey(field)) {
                    row.put(ind.getIndicatorKey(), rawData.get(field));
                }
            }

            // 添加每个指标的 min/max 边界
            List<Map<String, Object>> indicatorBounds = new ArrayList<>();
            for (CostIndicatorConfig ind : indicators) {
                Map<String, Object> bound = new LinkedHashMap<>();
                bound.put("indicatorKey", ind.getIndicatorKey());
                bound.put("min", globalMinValues.getOrDefault(ind.getIndicatorKey(), BigDecimal.ZERO));
                bound.put("max", globalMaxValues.getOrDefault(ind.getIndicatorKey(), BigDecimal.ONE));
                indicatorBounds.add(bound);
            }
            row.put("_indicatorBounds", indicatorBounds);

            result.add(row);
        }
        return result;
    }

    /**
     * 计算效费分析
     */
    @Transactional
    public CostEffectivenessCalculateResponse calculate(CostEffectivenessCalculateRequest request) {
        String evaluationId = request.getEvaluationId();
        List<String> operationIds = request.getOperationIds();

        // 获取启用的指标配置
        List<CostIndicatorConfig> indicators = indicatorConfigRepository.findByIsActiveTrueOrderBySortOrderAsc();
        if (indicators.isEmpty()) {
            throw new RuntimeException("没有启用的成本指标配置");
        }

        // 获取作战基础信息数据
        Map<String, Map<String, Object>> operationDataMap = fetchOperationData(operationIds);

        // 获取惩罚模型最终得分
        Map<String, BigDecimal> effectivenessScores = fetchEffectivenessScores(evaluationId, operationIds);

        // 计算归一化边界（使用实际数据范围）
        Map<String, BigDecimal> globalMinValues = new HashMap<>();
        Map<String, BigDecimal> globalMaxValues = new HashMap<>();
        calculateNormalizationBounds(indicators, operationDataMap, globalMinValues, globalMaxValues);

        // 计算权重配置
        Map<String, BigDecimal> weightsConfig = calculateWeights(indicators, request);

        // 计算各作战的成本指数和效费比
        List<CostEffectivenessResultDTO> results = new ArrayList<>();
        List<CostEvaluationRecord> recordsToSave = new ArrayList<>();

        for (String operationId : operationIds) {
            Map<String, Object> rawData = operationDataMap.get(operationId);
            if (rawData == null) continue;

            try {
                CostEffectivenessResultDTO result = calculateForOperation(
                        operationId, evaluationId, indicators, rawData,
                        globalMinValues, globalMaxValues, weightsConfig,
                        effectivenessScores.get(operationId)
                );
                results.add(result);

                // 转换为实体保存
                CostEvaluationRecord record = convertToEntity(result, evaluationId, operationId);
                record.setNormalizationBounds(serializeToJson(Map.of("min", globalMinValues, "max", globalMaxValues)));
                recordsToSave.add(record);
            } catch (Exception e) {
                log.error("计算作战 {} 成本失败: {}", operationId, e.getMessage());
            }
        }

        // 保存结果
        for (CostEvaluationRecord record : recordsToSave) {
            // 如果已存在则更新
            Optional<CostEvaluationRecord> existing = recordRepository
                    .findByEvaluationIdAndOperationId(record.getEvaluationId(), record.getOperationId());
            if (existing.isPresent()) {
                record.setId(existing.get().getId());
            }
            recordRepository.save(record);
        }

        // 汇总计算结果写入批次表
        CostEffectivenessCalculateResponse.Statistics stats = computeStatistics(results);
        updateBatchStatus(evaluationId, results.size(), operationIds,
                stats.getAvgCostIndex(), stats.getAvgEffectivenessScore(),
                stats.getAvgCostEffectivenessRatio(),
                stats.getMinCostIndex(), stats.getMaxCostIndex(),
                stats.getMinCostEffectivenessRatio(), stats.getMaxCostEffectivenessRatio());

        // 构建响应
        return buildResponse(evaluationId, results, weightsConfig, globalMinValues, globalMaxValues, stats);
    }

    /**
     * 删除评估结果
     */
    @Transactional
    public void deleteResults(String evaluationId) {
        recordRepository.deleteByEvaluationId(evaluationId);
        batchRepository.deleteByEvaluationId(evaluationId);
    }

    /**
     * 保存指标权重配置（用户手动修改后调用）
     */
    @Transactional
    public void saveIndicatorWeights(Map<String, BigDecimal> weights) {
        if (weights == null || weights.isEmpty()) return;
        for (Map.Entry<String, BigDecimal> entry : weights.entrySet()) {
            String indicatorKey = entry.getKey();
            BigDecimal weight = entry.getValue();
            Optional<CostIndicatorConfig> configOpt = indicatorConfigRepository.findByIndicatorKey(indicatorKey);
            if (configOpt.isPresent()) {
                CostIndicatorConfig config = configOpt.get();
                config.setWeight(weight);
                indicatorConfigRepository.save(config);
            }
        }
    }

    /**
     * 重置为两层等权权重（根据当前指标配置重新计算并保存）
     */
    @Transactional
    public Map<String, BigDecimal> resetToEqualWeights() {
        List<CostIndicatorConfig> indicators = indicatorConfigRepository.findByIsActiveTrueOrderBySortOrderAsc();
        Map<String, BigDecimal> weights = calculateEqualWeights(indicators);
        // 保存回数据库
        for (Map.Entry<String, BigDecimal> entry : weights.entrySet()) {
            String indicatorKey = entry.getKey();
            BigDecimal weight = entry.getValue();
            Optional<CostIndicatorConfig> configOpt = indicatorConfigRepository.findByIndicatorKey(indicatorKey);
            if (configOpt.isPresent()) {
                CostIndicatorConfig config = configOpt.get();
                config.setWeight(weight);
                indicatorConfigRepository.save(config);
            }
        }
        return weights;
    }

    /**
     * 计算两层等权权重（纯计算，不保存数据库）
     */
    private Map<String, BigDecimal> calculateEqualWeights(List<CostIndicatorConfig> indicators) {
        Map<String, BigDecimal> weights = new HashMap<>();

        // 按类别分组
        Map<String, List<CostIndicatorConfig>> categoryGroups = new LinkedHashMap<>();
        for (String cat : COST_CATEGORIES) {
            categoryGroups.put(cat, new ArrayList<>());
        }
        categoryGroups.put("_other", new ArrayList<>());

        for (CostIndicatorConfig indicator : indicators) {
            String category = indicator.getCategory();
            if (categoryGroups.containsKey(category)) {
                categoryGroups.get(category).add(indicator);
            } else {
                categoryGroups.get("_other").add(indicator);
            }
        }

        // 计算一级维度权重（每类 25%）
        BigDecimal baseCategoryWeight = new BigDecimal("0.25");
        int nonEmptyCategories = 0;
        for (String cat : COST_CATEGORIES) {
            if (!categoryGroups.get(cat).isEmpty()) {
                nonEmptyCategories++;
            }
        }

        // 空类别权重按比例分配
        int emptyCategories = COST_CATEGORIES.size() - nonEmptyCategories;
        BigDecimal extraWeight = BigDecimal.ZERO;
        if (emptyCategories > 0 && nonEmptyCategories > 0) {
            extraWeight = baseCategoryWeight.multiply(BigDecimal.valueOf(emptyCategories))
                    .divide(BigDecimal.valueOf(nonEmptyCategories), 6, RoundingMode.HALF_UP);
        }

        // 计算每个指标的权重
        for (String cat : COST_CATEGORIES) {
            List<CostIndicatorConfig> catIndicators = categoryGroups.get(cat);
            if (catIndicators.isEmpty()) continue;

            BigDecimal catWeight = baseCategoryWeight.add(extraWeight);
            BigDecimal perIndicatorWeight = catWeight.divide(
                    BigDecimal.valueOf(catIndicators.size()), 6, RoundingMode.HALF_UP);

            for (CostIndicatorConfig indicator : catIndicators) {
                weights.put(indicator.getIndicatorKey(), perIndicatorWeight);
            }
        }

        // 处理其他类别
        List<CostIndicatorConfig> otherIndicators = categoryGroups.get("_other");
        if (!otherIndicators.isEmpty() && extraWeight.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal perIndicatorWeight = extraWeight.divide(
                    BigDecimal.valueOf(otherIndicators.size()), 6, RoundingMode.HALF_UP);
            for (CostIndicatorConfig indicator : otherIndicators) {
                weights.put(indicator.getIndicatorKey(), perIndicatorWeight);
            }
        }

        return weights;
    }

    // ==================== 私有方法 ====================

    /**
     * 获取作战基础数据（从 records_military_operation_info 表，按 operation_id 查）
     */
    private Map<String, Map<String, Object>> fetchOperationData(List<String> operationIds) {
        Map<String, Map<String, Object>> result = new HashMap<>();

        if (operationIds == null || operationIds.isEmpty()) {
            return result;
        }

        // 将 String 列表转为 Integer 列表
        List<Integer> intIds = new ArrayList<>();
        for (String opIdStr : operationIds) {
            try {
                intIds.add(Integer.parseInt(opIdStr.trim()));
            } catch (NumberFormatException e) {
                log.warn("无效的作战ID（无法转为整数）: {}", opIdStr);
            }
        }

        if (intIds.isEmpty()) {
            return result;
        }

        // 批量查询（使用 operation_id 字段）
        Map<Integer, Map<String, Object>> batchData = operationInfoRepository.findByOperationIds(intIds);

        for (Integer opIdInt : batchData.keySet()) {
            result.put(String.valueOf(opIdInt), batchData.get(opIdInt));
        }

        log.info("从 records_military_operation_info 加载了 {} 条作战基础数据", result.size());
        return result;
    }

    /**
     * 获取效能得分（优先级：综合评分 > 专家集结加权得分 > 惩罚模型最终得分）
     * - ahp_communication_scoring_result.total_score：综合评分结果
     * - expert_weighted_evaluation_result.total_score：专家集结后的综合得分（覆盖所有批次）
     * - penalty_model_result.final_score：惩罚模型处理后的最终得分（部分批次可能有）
     */
    private Map<String, BigDecimal> fetchEffectivenessScores(String evaluationId, List<String> operationIds) {
        Map<String, BigDecimal> scores = new HashMap<>();

        // Step 1: 优先从综合评分结果获取（ahp_communication_scoring_result.total_score）
        List<AhpCommunicationScoringResult> scoringResults =
                scoringResultRepository.findByEvaluationBatchIdOrderByOperationIdAsc(evaluationId);
        if (!scoringResults.isEmpty()) {
            for (AhpCommunicationScoringResult sr : scoringResults) {
                if (operationIds.contains(sr.getOperationId()) && sr.getTotalScore() != null) {
                    scores.put(sr.getOperationId(), sr.getTotalScore());
                }
            }
            log.info("从综合评分结果(ahp_communication_scoring_result)加载了 {} 条效能得分", scores.size());
            if (!scores.isEmpty()) {
                return scores;
            }
        }

        // Step 2: 尝试从专家集结加权结果获取（total_score，覆盖所有批次）
        List<ExpertWeightedEvaluationResult> weightedResults =
                weightedResultRepository.findByEvaluationIdOrderByOperationIdAsc(evaluationId);
        if (!weightedResults.isEmpty()) {
            for (ExpertWeightedEvaluationResult wr : weightedResults) {
                if (operationIds.contains(wr.getOperationId())) {
                    scores.put(wr.getOperationId(), wr.getTotalScore());
                }
            }
            log.info("从专家集结结果加载了 {} 条效能得分", scores.size());
            return scores;
        }

        // Step 3: 尝试从惩罚模型结果获取（final_score，仅部分批次）
        if (scores.isEmpty()) {
            List<PenaltyModelResult> penaltyResults =
                    penaltyResultRepository.findByEvaluationId(evaluationId);
            for (PenaltyModelResult pr : penaltyResults) {
                if (operationIds.contains(pr.getOperationId())) {
                    scores.put(pr.getOperationId(), pr.getFinalScore());
                }
            }
            if (!scores.isEmpty()) {
                log.info("从惩罚模型结果加载了 {} 条效能得分", scores.size());
            }
        }

        if (scores.isEmpty()) {
            log.warn("评估批次 [{}] 找不到任何效能得分数据（综合评分/专家集结/惩罚模型均无数据）", evaluationId);
        }

        return scores;
    }

    /**
     * 计算归一化边界
     */
    private void calculateNormalizationBounds(List<CostIndicatorConfig> indicators,
                                            Map<String, Map<String, Object>> operationDataMap,
                                            Map<String, BigDecimal> globalMinValues,
                                            Map<String, BigDecimal> globalMaxValues) {
        for (CostIndicatorConfig indicator : indicators) {
            String field = indicator.getSourceField();
            List<BigDecimal> values = new ArrayList<>();

            for (Map<String, Object> data : operationDataMap.values()) {
                Object value = data.get(field);
                if (value != null) {
                    BigDecimal bdValue = toBigDecimal(value);
                    if (bdValue != null) {
                        values.add(bdValue);
                    }
                }
            }

            if (!values.isEmpty()) {
                globalMinValues.put(indicator.getIndicatorKey(), Collections.min(values));
                globalMaxValues.put(indicator.getIndicatorKey(), Collections.max(values));
            } else {
                // 使用配置的默认值
                globalMinValues.put(indicator.getIndicatorKey(),
                        indicator.getNormalizationMin() != null ? indicator.getNormalizationMin() : BigDecimal.ZERO);
                globalMaxValues.put(indicator.getIndicatorKey(),
                        indicator.getNormalizationMax() != null ? indicator.getNormalizationMax() : BigDecimal.valueOf(100));
            }
        }
    }

    // 四大成本类别（一级维度）
    private static final List<String> COST_CATEGORIES = Arrays.asList(
            "人力成本", "装备成本", "能源成本", "备件物流"
    );

    /**
     * 计算权重配置（两层等权）
     * 一级维度（4类）: 各 25% (0.25)
     *   ├─ 人力成本 25%
     *   │   └─ 类内各指标均分权重
     *   ├─ 装备成本 25%
     *   │   └─ 类内各指标均分权重
     *   ├─ 能源成本 25%
     *   │   └─ 类内各指标均分权重
     *   └─ 备件物流 25%
     *       └─ 类内各指标均分权重
     * 确保 Σ权重 = 1.0 (100%)
     * 空类别的权重按比例分配给其他类别
     */
    private Map<String, BigDecimal> calculateWeights(List<CostIndicatorConfig> indicators,
                                                     CostEffectivenessCalculateRequest request) {
        Map<String, BigDecimal> weights = new HashMap<>();

        if ("manual".equals(request.getWeightMethod()) && request.getManualWeights() != null) {
            // 使用手动设置的权重
            weights.putAll(request.getManualWeights());
        } else {
            // 两层等权计算
            // Step 1: 按类别分组指标
            Map<String, List<CostIndicatorConfig>> categoryGroups = new LinkedHashMap<>();
            for (String cat : COST_CATEGORIES) {
                categoryGroups.put(cat, new ArrayList<>());
            }
            // 兜底未知类别
            categoryGroups.put("_other", new ArrayList<>());

            for (CostIndicatorConfig indicator : indicators) {
                String category = indicator.getCategory();
                if (categoryGroups.containsKey(category)) {
                    categoryGroups.get(category).add(indicator);
                } else {
                    categoryGroups.get("_other").add(indicator);
                }
            }

            // Step 2: 计算一级维度权重（每类 25%，空类别权重待分配）
            BigDecimal baseCategoryWeight = new BigDecimal("0.25"); // 0.25 = 25%
            Map<String, BigDecimal> categoryWeights = new HashMap<>();
            int nonEmptyCategories = 0;

            for (String cat : COST_CATEGORIES) {
                if (!categoryGroups.get(cat).isEmpty()) {
                    categoryWeights.put(cat, baseCategoryWeight);
                    nonEmptyCategories++;
                }
            }

            // Step 3: 计算二级指标权重（类内均分）
            // 空类别的权重按比例分配给其他类别
            int emptyCategories = COST_CATEGORIES.size() - nonEmptyCategories;
            BigDecimal extraWeight = BigDecimal.ZERO;
            if (emptyCategories > 0 && nonEmptyCategories > 0) {
                extraWeight = baseCategoryWeight.multiply(BigDecimal.valueOf(emptyCategories))
                        .divide(BigDecimal.valueOf(nonEmptyCategories), 6, RoundingMode.HALF_UP);
            }

            for (String cat : COST_CATEGORIES) {
                List<CostIndicatorConfig> catIndicators = categoryGroups.get(cat);
                if (catIndicators.isEmpty()) continue;

                BigDecimal catWeight = categoryWeights.getOrDefault(cat, BigDecimal.ZERO).add(extraWeight);
                BigDecimal perIndicatorWeight = catWeight.divide(
                        BigDecimal.valueOf(catIndicators.size()), 6, RoundingMode.HALF_UP);

                for (CostIndicatorConfig indicator : catIndicators) {
                    weights.put(indicator.getIndicatorKey(), perIndicatorWeight);
                }
            }

            // 处理未知类别（放在 _other，权重为 0 或按数量均分空类别权重）
            List<CostIndicatorConfig> otherIndicators = categoryGroups.get("_other");
            if (!otherIndicators.isEmpty()) {
                BigDecimal otherWeight = extraWeight;
                if (otherWeight.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal perIndicatorWeight = otherWeight.divide(
                            BigDecimal.valueOf(otherIndicators.size()), 6, RoundingMode.HALF_UP);
                    for (CostIndicatorConfig indicator : otherIndicators) {
                        weights.put(indicator.getIndicatorKey(), perIndicatorWeight);
                    }
                }
            }
        }

        return weights;
    }

    /**
     * 计算单个作战的成本指数和效费比
     */
    private CostEffectivenessResultDTO calculateForOperation(
            String operationId,
            String evaluationId,
            List<CostIndicatorConfig> indicators,
            Map<String, Object> rawData,
            Map<String, BigDecimal> globalMinValues,
            Map<String, BigDecimal> globalMaxValues,
            Map<String, BigDecimal> weightsConfig,
            BigDecimal effectivenessScore) {

        // 原始指标值
        Map<String, BigDecimal> rawIndicators = new HashMap<>();
        Map<String, BigDecimal> normalizedIndicators = new HashMap<>();
        Map<String, BigDecimal> costByCategory = new HashMap<>();
        Map<String, BigDecimal> effectiveNormalized = new HashMap<>();

        // 计算归一化值和加权成本
        BigDecimal totalWeightedCost = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (CostIndicatorConfig indicator : indicators) {
            String field = indicator.getSourceField();
            Object rawValue = rawData.get(field);
            BigDecimal value = toBigDecimal(rawValue);

            rawIndicators.put(indicator.getIndicatorKey(), value != null ? value : BigDecimal.ZERO);

            // 归一化处理
            BigDecimal normalized = normalize(value, indicator, globalMinValues, globalMaxValues);
            normalizedIndicators.put(indicator.getIndicatorKey(), normalized);

            // 统一按成本型合成：min-max 归一化值直接作为可比成本分量（数值越大负担越高）
            BigDecimal effNorm = normalized;
            effectiveNormalized.put(indicator.getIndicatorKey(), effNorm);

            // 累加加权成本
            BigDecimal weight = weightsConfig.getOrDefault(indicator.getIndicatorKey(), BigDecimal.ZERO);
            BigDecimal weightedCost = effNorm.multiply(weight);
            totalWeightedCost = totalWeightedCost.add(weightedCost);
            totalWeight = totalWeight.add(weight);

            // 按类别汇总
            BigDecimal categoryCost = costByCategory.getOrDefault(indicator.getCategory(), BigDecimal.ZERO);
            costByCategory.put(indicator.getCategory(), categoryCost.add(weightedCost));
        }

        // 成本指数 C（归一化到0~1）
        BigDecimal costIndex = BigDecimal.ZERO;
        if (totalWeight.compareTo(BigDecimal.ZERO) > 0) {
            costIndex = totalWeightedCost.divide(totalWeight, 6, RoundingMode.HALF_UP);
        }

        // 效费比 R = E / C
        BigDecimal costEffectivenessRatio = BigDecimal.ZERO;
        if (costIndex.compareTo(BigDecimal.ZERO) > 0) {
            costEffectivenessRatio = effectivenessScore != null
                    ? effectivenessScore.divide(costIndex, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        }

        // 效费等级
        String efficiencyGrade = calculateEfficiencyGrade(costEffectivenessRatio);

        // 构建归一化边界
        Map<String, Map<String, BigDecimal>> normBounds = new HashMap<>();
        for (CostIndicatorConfig ind : indicators) {
            Map<String, BigDecimal> bounds = new HashMap<>();
            bounds.put("min", globalMinValues.getOrDefault(ind.getIndicatorKey(), BigDecimal.ZERO));
            bounds.put("max", globalMaxValues.getOrDefault(ind.getIndicatorKey(), BigDecimal.ONE));
            normBounds.put(ind.getIndicatorKey(), bounds);
        }

        return CostEffectivenessResultDTO.builder()
                .evaluationId(evaluationId)
                .operationId(operationId)
                .costIndex(costIndex)
                .costIndexRaw(totalWeightedCost)
                .effectivenessScore(effectivenessScore)
                .costEffectivenessRatio(costEffectivenessRatio)
                .efficiencyGrade(efficiencyGrade)
                .costByCategory(costByCategory)
                .rawIndicators(rawIndicators)
                .normalizedIndicators(normalizedIndicators)
                .effectiveNormalized(effectiveNormalized)
                .weightsSnapshot(weightsConfig)
                .normalizationBounds(normBounds)
                .build();
    }

    /**
     * 归一化处理（min-max变换）
     */
    private BigDecimal normalize(BigDecimal value, CostIndicatorConfig indicator,
                                 Map<String, BigDecimal> globalMinValues,
                                 Map<String, BigDecimal> globalMaxValues) {
        if (value == null) return BigDecimal.ZERO;

        BigDecimal min = globalMinValues.getOrDefault(indicator.getIndicatorKey(), BigDecimal.ZERO);
        BigDecimal max = globalMaxValues.getOrDefault(indicator.getIndicatorKey(), BigDecimal.ONE);

        BigDecimal range = max.subtract(min);
        if (range.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal normalized = value.subtract(min).divide(range, 6, RoundingMode.HALF_UP);
        // 确保在0~1范围内
        return normalized.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }

    /**
     * 计算效费等级
     */
    private String calculateEfficiencyGrade(BigDecimal ratio) {
        if (ratio == null) return "无数据";
        // 根据效费比划分等级（可根据实际情况调整阈值）
        if (ratio.compareTo(BigDecimal.valueOf(1.5)) >= 0) return "优秀";
        if (ratio.compareTo(BigDecimal.valueOf(1.2)) >= 0) return "良好";
        if (ratio.compareTo(BigDecimal.valueOf(1.0)) >= 0) return "合格";
        if (ratio.compareTo(BigDecimal.valueOf(0.8)) >= 0) return "较差";
        return "差";
    }

    /**
     * 更新批次状态
     */
    private void updateBatchStatus(String evaluationId, int successCount) {
        updateBatchStatus(evaluationId, successCount, null);
    }

    /**
     * 更新批次状态（带作战ID列表快照）
     */
    private void updateBatchStatus(String evaluationId, int successCount, List<String> operationIds) {
        updateBatchStatus(evaluationId, successCount, operationIds, null, null, null, null, null, null, null);
    }

    /**
     * 更新批次状态（带完整汇总结果）
     */
    private void updateBatchStatus(String evaluationId, int successCount, List<String> operationIds,
                                   BigDecimal avgCostIndex, BigDecimal avgEffectScore,
                                   BigDecimal avgRatio, BigDecimal minCostIndex, BigDecimal maxCostIndex,
                                   BigDecimal minRatio, BigDecimal maxRatio) {
        Optional<CostEvaluationBatch> batchOpt = batchRepository.findByEvaluationId(evaluationId);
        CostEvaluationBatch batch;
        if (batchOpt.isPresent()) {
            batch = batchOpt.get();
        } else {
            batch = new CostEvaluationBatch();
            batch.setEvaluationId(evaluationId);
            batch.setEvaluationTime(LocalDateTime.now());
        }
        batch.setStatus(CostEvaluationBatch.BatchStatus.completed);
        batch.setCompletedCount(successCount);
        batch.setOperationCount(operationIds != null ? operationIds.size() : successCount);
        if (operationIds != null) {
            batch.setOperationIds(serializeToJson(operationIds));
        }
        // 汇总计算结果
        batch.setAvgCostIndex(avgCostIndex);
        batch.setAvgEffectivenessScore(avgEffectScore);
        batch.setAvgCostEffectivenessRatio(avgRatio);
        batch.setMinCostIndex(minCostIndex);
        batch.setMaxCostIndex(maxCostIndex);
        batch.setMinCostEffectivenessRatio(minRatio);
        batch.setMaxCostEffectivenessRatio(maxRatio);
        batchRepository.save(batch);
    }

    /**
     * 计算统计信息（抽取为独立方法，供 batch 表写入和响应构建共用）
     */
    private CostEffectivenessCalculateResponse.Statistics computeStatistics(List<CostEffectivenessResultDTO> results) {
        if (results == null || results.isEmpty()) {
            return CostEffectivenessCalculateResponse.Statistics.builder().build();
        }
        int n = results.size();
        BigDecimal avgCostIndex = results.stream()
                .map(CostEffectivenessResultDTO::getCostIndex).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(n), 6, RoundingMode.HALF_UP);
        BigDecimal minCostIndex = results.stream()
                .map(CostEffectivenessResultDTO::getCostIndex).filter(Objects::nonNull)
                .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal maxCostIndex = results.stream()
                .map(CostEffectivenessResultDTO::getCostIndex).filter(Objects::nonNull)
                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal avgRatio = results.stream()
                .map(CostEffectivenessResultDTO::getCostEffectivenessRatio).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(n), 6, RoundingMode.HALF_UP);
        BigDecimal minRatio = results.stream()
                .map(CostEffectivenessResultDTO::getCostEffectivenessRatio).filter(Objects::nonNull)
                .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal maxRatio = results.stream()
                .map(CostEffectivenessResultDTO::getCostEffectivenessRatio).filter(Objects::nonNull)
                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal avgEffect = results.stream()
                .map(CostEffectivenessResultDTO::getEffectivenessScore).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(n), 6, RoundingMode.HALF_UP);
        BigDecimal minEffect = results.stream()
                .map(CostEffectivenessResultDTO::getEffectivenessScore).filter(Objects::nonNull)
                .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal maxEffect = results.stream()
                .map(CostEffectivenessResultDTO::getEffectivenessScore).filter(Objects::nonNull)
                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        return CostEffectivenessCalculateResponse.Statistics.builder()
                .avgCostIndex(avgCostIndex).minCostIndex(minCostIndex).maxCostIndex(maxCostIndex)
                .avgCostEffectivenessRatio(avgRatio)
                .minCostEffectivenessRatio(minRatio).maxCostEffectivenessRatio(maxRatio)
                .avgEffectivenessScore(avgEffect)
                .minEffectivenessScore(minEffect).maxEffectivenessScore(maxEffect)
                .build();
    }

    /**
     * 构建响应
     */
    private CostEffectivenessCalculateResponse buildResponse(
            String evaluationId,
            List<CostEffectivenessResultDTO> results,
            Map<String, BigDecimal> weightsConfig,
            Map<String, BigDecimal> globalMinValues,
            Map<String, BigDecimal> globalMaxValues,
            CostEffectivenessCalculateResponse.Statistics statistics) {

        return CostEffectivenessCalculateResponse.builder()
                .evaluationId(evaluationId)
                .successCount(results.size())
                .failCount(0)
                .results(results)
                .normalizationBounds(CostEffectivenessCalculateResponse.NormalizationBounds.builder()
                        .minValues(globalMinValues)
                        .maxValues(globalMaxValues)
                        .build())
                .weightsConfig(weightsConfig)
                .statistics(statistics)
                .build();
    }

    /**
     * 实体转DTO
     */
    private CostEffectivenessResultDTO convertToDTO(CostEvaluationRecord record) {
        Map<String, BigDecimal> costByCategory = parseJson(record.getCostByCategory());
        Map<String, BigDecimal> rawIndicators = parseJson(record.getRawIndicators());
        Map<String, BigDecimal> normalizedIndicators = parseJson(record.getNormalizedIndicators());
        Map<String, BigDecimal> effectiveNormalized = parseJson(record.getEffectiveNormalized());
        Map<String, BigDecimal> weightsSnapshot = parseJson(record.getWeightsSnapshot());

        // 解析归一化边界
        Map<String, Map<String, BigDecimal>> normalizationBounds = parseNormalizationBounds(record.getNormalizationBounds());

        return CostEffectivenessResultDTO.builder()
                .id(record.getId())
                .evaluationId(record.getEvaluationId())
                .operationId(record.getOperationId())
                .costIndex(record.getCostIndex())
                .costIndexRaw(record.getCostIndexRaw())
                .effectivenessScore(record.getEffectivenessScore())
                .costEffectivenessRatio(record.getCostEffectivenessRatio())
                .efficiencyGrade(calculateEfficiencyGrade(record.getCostEffectivenessRatio()))
                .costByCategory(costByCategory)
                .rawIndicators(rawIndicators)
                .normalizedIndicators(normalizedIndicators)
                .effectiveNormalized(effectiveNormalized)
                .weightsSnapshot(weightsSnapshot)
                .normalizationBounds(normalizationBounds)
                .createdAt(record.getCreatedAt() != null ? record.getCreatedAt().toString() : null)
                .updatedAt(record.getUpdatedAt() != null ? record.getUpdatedAt().toString() : null)
                .build();
    }

    /**
     * DTO转实体
     */
    private CostEvaluationRecord convertToEntity(CostEffectivenessResultDTO dto, String evaluationId, String operationId) {
        CostEvaluationRecord record = new CostEvaluationRecord();
        record.setEvaluationId(evaluationId);
        record.setOperationId(operationId);
        record.setCostIndex(dto.getCostIndex());
        record.setCostIndexRaw(dto.getCostIndexRaw());
        record.setEffectivenessScore(dto.getEffectivenessScore());
        record.setCostEffectivenessRatio(dto.getCostEffectivenessRatio());
        record.setCostByCategory(serializeToJson(dto.getCostByCategory()));
        record.setRawIndicators(serializeToJson(dto.getRawIndicators()));
        record.setNormalizedIndicators(serializeToJson(dto.getNormalizedIndicators()));
        record.setWeightsSnapshot(serializeToJson(dto.getWeightsSnapshot()));
        // 保存 effectiveNormalized（方向调整后的分量）
        if (dto.getEffectiveNormalized() != null) {
            record.setEffectiveNormalized(serializeToJson(dto.getEffectiveNormalized()));
        }
        return record;
    }

    /**
     * JSON解析
     */
    @SuppressWarnings("unchecked")
    private Map<String, BigDecimal> parseJson(String json) {
        if (json == null || json.isEmpty()) return new HashMap<>();
        try {
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            Map<String, BigDecimal> result = new HashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                result.put(entry.getKey(), toBigDecimal(entry.getValue()));
            }
            return result;
        } catch (Exception e) {
            log.warn("JSON解析失败: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * JSON解析归一化边界
     * 格式: {"min": {...}, "max": {...}} -> Map<indicatorKey, Map<"min"|"max", BigDecimal>>
     */
    @SuppressWarnings("unchecked")
    private Map<String, Map<String, BigDecimal>> parseNormalizationBounds(String json) {
        Map<String, Map<String, BigDecimal>> result = new HashMap<>();
        if (json == null || json.isEmpty()) return result;
        try {
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            Map<String, BigDecimal> minValues = new HashMap<>();
            Map<String, BigDecimal> maxValues = new HashMap<>();

            Object minObj = map.get("min");
            Object maxObj = map.get("max");

            if (minObj instanceof Map) {
                for (Map.Entry<String, Object> entry : ((Map<String, Object>) minObj).entrySet()) {
                    minValues.put(entry.getKey(), toBigDecimal(entry.getValue()));
                }
            }
            if (maxObj instanceof Map) {
                for (Map.Entry<String, Object> entry : ((Map<String, Object>) maxObj).entrySet()) {
                    maxValues.put(entry.getKey(), toBigDecimal(entry.getValue()));
                }
            }

            // 合并为统一结构
            Set<String> allKeys = new HashSet<>();
            allKeys.addAll(minValues.keySet());
            allKeys.addAll(maxValues.keySet());
            for (String key : allKeys) {
                Map<String, BigDecimal> bounds = new HashMap<>();
                bounds.put("min", minValues.getOrDefault(key, BigDecimal.ZERO));
                bounds.put("max", maxValues.getOrDefault(key, BigDecimal.ONE));
                result.put(key, bounds);
            }
        } catch (Exception e) {
            log.warn("归一化边界JSON解析失败: {}", e.getMessage());
        }
        return result;
    }

    /**
     * JSON序列化
     */
    private String serializeToJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("JSON序列化失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 转换为BigDecimal
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
