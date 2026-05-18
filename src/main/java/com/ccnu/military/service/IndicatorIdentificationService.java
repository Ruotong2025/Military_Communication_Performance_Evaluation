package com.ccnu.military.service;

import com.ccnu.military.dto.*;
import com.ccnu.military.entity.IndicatorDefinition;
import com.ccnu.military.entity.IndicatorSourceData;
import com.ccnu.military.repository.IndicatorDefinitionRepository;
import com.ccnu.military.repository.IndicatorSourceDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 指标智能识别服务
 * <p>实现MySQL预匹配 + DeepSeek API批量识别的优化逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorIdentificationService {

    private final DeepSeekApiClient deepSeekApiClient;
    private final IndicatorDefinitionRepository indicatorRepository;
    private final IndicatorSourceDataRepository sourceDataRepository;

    /**
     * 批量解析并识别指标（带预匹配的优化版本）
     */
    @Transactional
    public IndicatorBatchParseResultDTO batchParseAndIdentify(IndicatorBatchParseRequestDTO request) {
        long startTime = System.currentTimeMillis();
        List<IndicatorBatchParseResultDTO.IndicatorResult> results = new ArrayList<>();

        int totalCount = request.getIndicators().size();
        int hitFromDb = 0;
        int newFromApi = 0;

        // ============================================
        // Step 1: MySQL 预匹配（关键优化！）
        // ============================================
        log.info("Step 1: MySQL预匹配开始，共 {} 个指标...", totalCount);

        List<IndicatorDefinition> existingIndicators =
                indicatorRepository.findByIndicatorNameInAndCategory(
                        request.getIndicators(),
                        request.getCategory());

        Map<String, IndicatorDefinition> existingMap = existingIndicators.stream()
                .collect(Collectors.toMap(
                        ind -> ind.getIndicatorName().trim().toLowerCase(),
                        ind -> ind,
                        (a, b) -> a));

        List<String> needApiCallIndicators = new ArrayList<>();
        Map<String, IndicatorBatchParseResultDTO.IndicatorResult> cachedResults = new LinkedHashMap<>();

        for (String indicatorName : request.getIndicators()) {
            String name = indicatorName.trim();
            String key = name.toLowerCase();

            IndicatorDefinition existing = existingMap.get(key);

            if (existing != null) {
                List<IndicatorSourceData> sourceDataList =
                        sourceDataRepository.findByIndicatorIdAndIsFormulaRelatedTrue(existing.getId());

                cachedResults.put(name, buildResultFromEntity(existing, sourceDataList, false, "MySQL命中"));
                hitFromDb++;
                log.debug("  ✅ 命中: {}", name);
            } else {
                needApiCallIndicators.add(name);
                log.debug("  ❌ 未命中: {}", name);
            }
        }

        // ============================================
        // Step 2: API 批量识别（只处理未命中的）
        // ============================================
        Map<String, IndicatorBatchParseResultDTO.IndicatorResult> apiResults = new HashMap<>();

        if (!needApiCallIndicators.isEmpty()) {
            log.info("Step 2: API批量识别开始，共 {} 个指标需要识别...", needApiCallIndicators.size());

            try {
                Map<String, IndicatorAnalysisResult> analysisResults =
                        deepSeekApiClient.batchAnalyzeIndicators(
                                needApiCallIndicators,
                                request.getDomain(),
                                request.getCategory());

                for (String indicatorName : needApiCallIndicators) {
                    IndicatorAnalysisResult analysis = analysisResults.get(indicatorName);

                    if (analysis != null) {
                        try {
                            IndicatorDefinition saved = saveIndicatorDefinition(indicatorName, analysis, request.getCategory());
                            List<IndicatorSourceData> sourceDataList = saveSourceData(saved.getId(), analysis);

                            apiResults.put(indicatorName,
                                    buildResultFromAnalysis(analysis, sourceDataList, true, "API新识别"));
                            newFromApi++;

                        } catch (Exception e) {
                            log.error("保存指标失败: {}", indicatorName, e);
                            apiResults.put(indicatorName,
                                    IndicatorBatchParseResultDTO.IndicatorResult.builder()
                                            .indicatorName(indicatorName)
                                            .message("保存失败: " + e.getMessage())
                                            .build());
                        }
                    } else {
                        apiResults.put(indicatorName,
                                IndicatorBatchParseResultDTO.IndicatorResult.builder()
                                        .indicatorName(indicatorName)
                                        .message("API未返回结果")
                                        .build());
                    }
                }

            } catch (Exception e) {
                log.error("API批量识别失败，尝试逐个识别...", e);
                for (String indicatorName : needApiCallIndicators) {
                    try {
                        IndicatorAnalysisResult analysis =
                                deepSeekApiClient.analyzeSingleIndicator(indicatorName, request.getDomain());

                        IndicatorDefinition saved = saveIndicatorDefinition(indicatorName, analysis, request.getCategory());
                        List<IndicatorSourceData> sourceDataList = saveSourceData(saved.getId(), analysis);

                        apiResults.put(indicatorName,
                                buildResultFromAnalysis(analysis, sourceDataList, true, "API新识别"));
                        newFromApi++;

                    } catch (Exception ex) {
                        log.error("单个指标识别失败: {}", indicatorName, ex);
                        apiResults.put(indicatorName,
                                IndicatorBatchParseResultDTO.IndicatorResult.builder()
                                        .indicatorName(indicatorName)
                                        .message("API识别失败: " + ex.getMessage())
                                        .build());
                    }
                }
            }
        }

        // ============================================
        // Step 3: 合并结果（保持原顺序）
        // ============================================
        log.info("Step 3: 合并结果...");

        for (String indicatorName : request.getIndicators()) {
            String name = indicatorName.trim();

            if (cachedResults.containsKey(name)) {
                results.add(cachedResults.get(name));
            } else if (apiResults.containsKey(name)) {
                results.add(apiResults.get(name));
            }
        }

        // ============================================
        // Step 4: 构建统计信息
        // ============================================
        long qualitativeCount = results.stream()
                .filter(r -> "QUALITATIVE".equals(r.getIndicatorType()))
                .count();
        long quantitativeCount = results.stream()
                .filter(r -> "QUANTITATIVE".equals(r.getIndicatorType()))
                .count();

        int estimatedTokens = estimateTokens(needApiCallIndicators);

        IndicatorBatchParseResultDTO.Statistics statistics =
                IndicatorBatchParseResultDTO.Statistics.builder()
                        .qualitativeCount((int) qualitativeCount)
                        .quantitativeCount((int) quantitativeCount)
                        .qualitativeRatio(totalCount > 0 ? (double) qualitativeCount / totalCount : 0)
                        .quantitativeRatio(totalCount > 0 ? (double) quantitativeCount / totalCount : 0)
                        .hitFromDb(hitFromDb)
                        .newFromApi(newFromApi)
                        .build();

        long processingTime = System.currentTimeMillis() - startTime;

        IndicatorBatchParseResultDTO result = IndicatorBatchParseResultDTO.builder()
                .success(true)
                .message(String.format("处理完成！MySQL命中%d个，API新识别%d个", hitFromDb, newFromApi))
                .totalCount(totalCount)
                .successCount(newFromApi)
                .skippedCount(hitFromDb)
                .processingTimeMs(processingTime)
                .estimatedTokens(estimatedTokens)
                .results(results)
                .statistics(statistics)
                .build();

        log.info("✅ 处理完成！总数={}, MySQL命中={}, API新识别={}, Token消耗≈{}",
                totalCount, hitFromDb, newFromApi, estimatedTokens);

        return result;
    }

    /**
     * 获取所有指标
     */
    public List<IndicatorDefinition> getIndicators(String category, String type) {
        List<IndicatorDefinition> indicators;

        if (category != null && !category.isEmpty() && type != null && !type.isEmpty()) {
            indicators = indicatorRepository.findByCategoryAndIndicatorType(
                    category, IndicatorDefinition.IndicatorType.valueOf(type));
        } else if (category != null && !category.isEmpty()) {
            indicators = indicatorRepository.findByCategory(category);
        } else if (type != null && !type.isEmpty()) {
            indicators = indicatorRepository.findByIndicatorType(IndicatorDefinition.IndicatorType.valueOf(type));
        } else {
            indicators = indicatorRepository.findByIsActiveTrueOrderByCategoryAscIndicatorNameAsc();
        }

        for (IndicatorDefinition indicator : indicators) {
            List<IndicatorSourceData> sourceDataList =
                    sourceDataRepository.findByIndicatorIdAndIsFormulaRelatedTrue(indicator.getId());
            indicator.setSourceDataList(sourceDataList);
        }

        return indicators;
    }

    /**
     * 获取指标详情
     */
    public IndicatorDefinition getIndicatorById(Long id) {
        IndicatorDefinition indicator = indicatorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("指标不存在: " + id));

        List<IndicatorSourceData> sourceDataList =
                sourceDataRepository.findByIndicatorIdAndIsFormulaRelatedTrue(id);
        indicator.setSourceDataList(sourceDataList);

        return indicator;
    }

    /**
     * 删除指标
     */
    @Transactional
    public void deleteIndicator(Long id) {
        sourceDataRepository.deleteByIndicatorId(id);
        indicatorRepository.deleteById(id);
    }

    // ============================================
    // 私有方法
    // ============================================

    private IndicatorDefinition saveIndicatorDefinition(
            String name,
            IndicatorAnalysisResult analysis,
            String category) {

        Optional<IndicatorDefinition> existing =
                indicatorRepository.findByIndicatorNameAndCategory(name, category);

        IndicatorDefinition entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setIndicatorType(IndicatorDefinition.IndicatorType.valueOf(analysis.getIndicatorType()));
            entity.setDescription(analysis.getDescription());
            entity.setUnit(analysis.getUnit());
            entity.setFormula(analysis.getFormula());
            entity.setFormulaDescription(analysis.getFormulaDescription());
            entity.setCalculationMethod(analysis.getCalculationMethod());
            entity.setSourceDataHint(analysis.getSourceDataHint());
            entity.setIsFromAi(true);
            entity.setAiConfidence(BigDecimal.valueOf(analysis.getConfidence()));
        } else {
            entity = IndicatorDefinition.builder()
                    .indicatorName(name)
                    .indicatorType(IndicatorDefinition.IndicatorType.valueOf(analysis.getIndicatorType()))
                    .description(analysis.getDescription())
                    .unit(analysis.getUnit())
                    .formula(analysis.getFormula())
                    .formulaDescription(analysis.getFormulaDescription())
                    .calculationMethod(analysis.getCalculationMethod())
                    .sourceDataHint(analysis.getSourceDataHint())
                    .isFromAi(true)
                    .aiConfidence(BigDecimal.valueOf(analysis.getConfidence()))
                    .category(category)
                    .isActive(true)
                    .build();
        }

        return indicatorRepository.save(entity);
    }

    private List<IndicatorSourceData> saveSourceData(Long indicatorId, IndicatorAnalysisResult analysis) {
        List<IndicatorSourceData> savedList = new ArrayList<>();

        sourceDataRepository.deleteByIndicatorId(indicatorId);

        if (analysis.getSourceDataList() != null && !analysis.getSourceDataList().isEmpty()) {
            for (IndicatorAnalysisResult.SourceData data : analysis.getSourceDataList()) {
                IndicatorSourceData entity = IndicatorSourceData.builder()
                        .indicatorId(indicatorId)
                        .sourceDataName(data.getDataName())
                        .sourceDataCode(generateDataCode(data.getDataName()))
                        .measurementMethod(data.getMeasurementMethod())
                        .dataType(data.getDataType() != null ?
                                IndicatorSourceData.DataType.valueOf(data.getDataType()) : null)
                        .unit(data.getUnit())
                        .priority(data.getPriority() != null ? data.getPriority() : 1)
                        .confidence(BigDecimal.valueOf(data.getConfidence() != null ? data.getConfidence() : 0.0))
                        .isFormulaRelated(data.getIsFormulaRelated() != null ? data.getIsFormulaRelated() : true)
                        .formulaSymbol(data.getFormulaSymbol())
                        .isEssential(data.getIsEssential() != null ? data.getIsEssential() : true)
                        .build();

                savedList.add(sourceDataRepository.save(entity));
            }
        }

        return savedList;
    }

    private String generateDataCode(String dataName) {
        return "SRC_" + dataName.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_").toUpperCase();
    }

    private IndicatorBatchParseResultDTO.IndicatorResult buildResultFromEntity(
            IndicatorDefinition entity,
            List<IndicatorSourceData> sourceDataList,
            Boolean isNew,
            String message) {

        List<IndicatorBatchParseResultDTO.FormulaSourceData> formulaDataList = sourceDataList.stream()
                .filter(data -> Boolean.TRUE.equals(data.getIsFormulaRelated()))
                .map(data -> IndicatorBatchParseResultDTO.FormulaSourceData.builder()
                        .dataName(data.getSourceDataName())
                        .formulaSymbol(data.getFormulaSymbol())
                        .dataType(data.getDataType() != null ? data.getDataType().name() : null)
                        .unit(data.getUnit())
                        .measurementMethod(data.getMeasurementMethod())
                        .isEssential(data.getIsEssential())
                        .build())
                .collect(Collectors.toList());

        return IndicatorBatchParseResultDTO.IndicatorResult.builder()
                .indicatorName(entity.getIndicatorName())
                .indicatorType(entity.getIndicatorType().name())
                .indicatorTypeDesc(entity.getIndicatorType() == IndicatorDefinition.IndicatorType.QUALITATIVE ?
                        "定性指标" : "定量指标")
                .confidence(entity.getAiConfidence() != null ? entity.getAiConfidence().doubleValue() : null)
                .description(entity.getDescription())
                .unit(entity.getUnit())
                .formula(entity.getFormula())
                .formulaDescription(entity.getFormulaDescription())
                .calculationMethod(entity.getCalculationMethod())
                .sourceDataHint(entity.getSourceDataHint())
                .formulaRelatedData(formulaDataList)
                .isNew(isNew)
                .isFromCache(!isNew)
                .message(message)
                .build();
    }

    private IndicatorBatchParseResultDTO.IndicatorResult buildResultFromAnalysis(
            IndicatorAnalysisResult analysis,
            List<IndicatorSourceData> sourceDataList,
            Boolean isNew,
            String message) {

        List<IndicatorBatchParseResultDTO.FormulaSourceData> formulaDataList = sourceDataList.stream()
                .filter(data -> Boolean.TRUE.equals(data.getIsFormulaRelated()))
                .map(data -> IndicatorBatchParseResultDTO.FormulaSourceData.builder()
                        .dataName(data.getSourceDataName())
                        .formulaSymbol(data.getFormulaSymbol())
                        .dataType(data.getDataType() != null ? data.getDataType().name() : null)
                        .unit(data.getUnit())
                        .measurementMethod(data.getMeasurementMethod())
                        .isEssential(data.getIsEssential())
                        .build())
                .collect(Collectors.toList());

        return IndicatorBatchParseResultDTO.IndicatorResult.builder()
                .indicatorName(analysis.getIndicatorName())
                .indicatorType(analysis.getIndicatorType())
                .indicatorTypeDesc(analysis.getIndicatorTypeDesc())
                .confidence(analysis.getConfidence())
                .description(analysis.getDescription())
                .unit(analysis.getUnit())
                .formula(analysis.getFormula())
                .formulaDescription(analysis.getFormulaDescription())
                .calculationMethod(analysis.getCalculationMethod())
                .sourceDataHint(analysis.getSourceDataHint())
                .formulaRelatedData(formulaDataList)
                .isNew(isNew)
                .isFromCache(!isNew)
                .message(message)
                .build();
    }

    private int estimateTokens(List<String> indicators) {
        if (indicators == null || indicators.isEmpty()) {
            return 0;
        }
        return 400 + indicators.size() * 10 + 200 + indicators.size() * 50;
    }
}
