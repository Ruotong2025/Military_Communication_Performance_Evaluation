package com.ccnu.military.service;

import com.ccnu.military.dto.*;
import com.ccnu.military.entity.IndicatorDefinition;
import com.ccnu.military.entity.IndicatorSourceData;
import com.ccnu.military.entity.MatchResult;
import com.ccnu.military.repository.IndicatorDefinitionRepository;
import com.ccnu.military.repository.IndicatorSourceDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 指标智能识别服务
 * <p>实现三种模式：数据库语义匹配 / API分析 / 手动选择
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorIdentificationService {

    private final DeepSeekApiClient deepSeekApiClient;
    private final SemanticSimilarityService semanticSimilarityService;
    private final IndicatorVectorService indicatorVectorService;
    private final ChromaVectorService chromaVectorService;
    private final IndicatorDefinitionRepository indicatorRepository;
    private final IndicatorSourceDataRepository sourceDataRepository;

    // ============================================
    // 新增方法：查询指标（情况一 + 情况三）
    // ============================================

    /**
     * 智能查询（新接口）
     * 返回 Top3 相似度候选 + 全量指标下拉列表
     */
    public IntelligentQueryResult intelligentQuery(IndicatorQueryRequest request) {
        String indicatorName = request.getIndicatorName().trim();
        log.info("智能查询指标: {}", indicatorName);

        // 执行语义匹配
        MatchResult matchResult = semanticSimilarityService.matchIndicator(indicatorName);

        // 转换候选列表
        List<IntelligentQueryResult.CandidateOption> candidates = new ArrayList<>();
        List<Long> candidateIds = new ArrayList<>();

        // 精确匹配：把精确匹配项加入候选（相似度100%）
        if (matchResult.isExactMatch()) {
            IndicatorDefinition exact = matchResult.getExactMatch();
            if (exact != null) {
                candidateIds.add(exact.getId());
                candidates.add(buildCandidateOption(exact, 1.0));
                log.info("精确匹配命中: {}", exact.getIndicatorName());
            }
        }

        // 语义相似候选
        if (matchResult.hasSimilarCandidates()) {
            List<Long> similarIds = matchResult.getCandidates().stream()
                    .map(MatchResult.SimilarityCandidate::getId)
                    .collect(Collectors.toList());
            candidateIds.addAll(similarIds);

            Map<Long, IndicatorDefinition> indicatorMap = indicatorRepository.findAllById(similarIds)
                    .stream()
                    .collect(Collectors.toMap(IndicatorDefinition::getId, ind -> ind));

            for (MatchResult.SimilarityCandidate candidate : matchResult.getCandidates()) {
                IndicatorDefinition ind = indicatorMap.get(candidate.getId());
                if (ind != null) {
                    candidates.add(buildCandidateOption(ind, candidate.getSimilarity()));
                }
            }
        }

        // 按相似度降序排列
        candidates.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));

        // 查询候选的数据源
        Map<Long, List<SourceDataDTO>> sourceDataMap = getSourceDataMap(candidateIds);
        for (IntelligentQueryResult.CandidateOption candidate : candidates) {
            candidate.setSourceDataList(sourceDataMap.getOrDefault(candidate.getId(), Collections.emptyList()));
        }

        // 获取全量指标列表
        List<IndicatorDefinition> allIndicators = indicatorRepository.findByIsActiveTrueOrderByCategoryAscIndicatorNameAsc();
        List<IntelligentQueryResult.IndicatorOption> allOptions = allIndicators.stream()
                .map(ind -> IntelligentQueryResult.IndicatorOption.builder()
                        .id(ind.getId())
                        .name(ind.getIndicatorName())
                        .indicatorType(ind.getIndicatorType() != null ? ind.getIndicatorType().name() : null)
                        .build())
                .collect(Collectors.toList());

        // 检查是否有精确匹配
        boolean hasExactMatch = matchResult.isExactMatch();

        return IntelligentQueryResult.builder()
                .originalName(indicatorName)
                .candidates(candidates)
                .allIndicators(allOptions)
                .hasExactMatch(hasExactMatch)
                .totalDatabaseCount(allIndicators.size())
                .build();
    }

    /**
     * 构建候选选项
     */
    private IntelligentQueryResult.CandidateOption buildCandidateOption(IndicatorDefinition ind, Double similarity) {
        return IntelligentQueryResult.CandidateOption.builder()
                .id(ind.getId())
                .name(ind.getIndicatorName())
                .similarity(similarity)
                .indicatorType(ind.getIndicatorType() != null ? ind.getIndicatorType().name() : null)
                .indicatorTypeDesc(ind.getIndicatorType() == IndicatorDefinition.IndicatorType.QUALITATIVE ?
                        "定性指标" : "定量指标")
                .unit(ind.getUnit())
                .description(ind.getDescription())
                .formula(ind.getFormula())
                .formulaDescription(ind.getFormulaDescription())
                .calculationMethod(ind.getCalculationMethod())
                .build();
    }

    /**
     * 获取指标ID到数据源的映射
     */
    private Map<Long, List<SourceDataDTO>> getSourceDataMap(List<Long> indicatorIds) {
        if (indicatorIds == null || indicatorIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<IndicatorSourceData> sourceDataList = sourceDataRepository.findByIndicatorIdIn(indicatorIds);

        Map<Long, List<SourceDataDTO>> result = new HashMap<>();
        for (IndicatorSourceData source : sourceDataList) {
            SourceDataDTO dto = SourceDataDTO.builder()
                    .sourceDataName(source.getSourceDataName())
                    .formulaSymbol(source.getFormulaSymbol())
                    .measurementMethod(source.getMeasurementMethod())
                    .dataType(source.getDataType() != null ? source.getDataType().name() : null)
                    .unit(source.getUnit())
                    .priority(source.getPriority())
                    .confidence(source.getConfidence() != null ? source.getConfidence().doubleValue() : null)
                    .isFormulaRelated(source.getIsFormulaRelated())
                    .isEssential(source.getIsEssential())
                    .build();
            result.computeIfAbsent(source.getIndicatorId(), k -> new ArrayList<>()).add(dto);
        }

        return result;
    }

    /**
     * 查询指标，返回三级匹配结果
     * 第一级：精确匹配
     * 第二级：语义相似度
     * 第三级：无匹配（需要调用 DeepSeek API）
     */
    public IndicatorQueryResultDTO queryIndicator(IndicatorQueryRequest request) {
        String indicatorName = request.getIndicatorName().trim();
        log.info("查询指标: {}", indicatorName);

        // 执行三级匹配
        MatchResult matchResult = semanticSimilarityService.matchIndicator(indicatorName);

        // 第一级：精确匹配命中
        if (matchResult.isExactMatch()) {
            IndicatorDefinition exactMatch = matchResult.getExactMatch();
            log.info("精确匹配命中: {}", exactMatch.getIndicatorName());

            List<IndicatorSourceData> sourceDataList =
                    sourceDataRepository.findByIndicatorIdAndIsFormulaRelatedTrue(exactMatch.getId());

            List<IndicatorQueryResultDTO.FormulaSourceData> sourceDataDTOs = sourceDataList.stream()
                    .map(data -> IndicatorQueryResultDTO.FormulaSourceData.builder()
                            .dataName(data.getSourceDataName())
                            .formulaSymbol(data.getFormulaSymbol())
                            .dataType(data.getDataType() != null ? data.getDataType().name() : null)
                            .unit(data.getUnit())
                            .measurementMethod(data.getMeasurementMethod())
                            .isEssential(data.getIsEssential())
                            .build())
                    .collect(Collectors.toList());

            return IndicatorQueryResultDTO.builder()
                    .indicatorName(indicatorName)
                    .matchType("EXACT")
                    .exactMatch(IndicatorQueryResultDTO.ExactMatchDTO.builder()
                            .id(exactMatch.getId())
                            .indicatorName(exactMatch.getIndicatorName())
                            .indicatorType(exactMatch.getIndicatorType() != null ? exactMatch.getIndicatorType().name() : null)
                            .indicatorTypeDesc(exactMatch.getIndicatorType() == IndicatorDefinition.IndicatorType.QUALITATIVE ?
                                    "定性指标" : "定量指标")
                            .formula(exactMatch.getFormula())
                            .formulaDescription(exactMatch.getFormulaDescription())
                            .calculationMethod(exactMatch.getCalculationMethod())
                            .unit(exactMatch.getUnit())
                            .description(exactMatch.getDescription())
                            .sourceDataList(sourceDataDTOs)
                            .build())
                    .statistics(IndicatorQueryResultDTO.Statistics.builder()
                            .matchedCount(1)
                            .totalDatabaseCount((int) indicatorRepository.count())
                            .build())
                    .build();
        }

        // 第二级：语义相似度命中
        if (matchResult.hasSimilarCandidates()) {
            List<MatchResult.SimilarityCandidate> candidates = matchResult.getCandidates();

            // 获取所有候选指标的详细信息
            List<Long> candidateIds = candidates.stream()
                    .map(MatchResult.SimilarityCandidate::getId)
                    .collect(Collectors.toList());

            Map<Long, IndicatorDefinition> indicatorMap = indicatorRepository.findAllById(candidateIds)
                    .stream()
                    .collect(Collectors.toMap(IndicatorDefinition::getId, ind -> ind));

            List<IndicatorQueryResultDTO.DatabaseCandidate> candidateDTOs = new ArrayList<>();

            for (MatchResult.SimilarityCandidate candidate : candidates) {
                IndicatorDefinition ind = indicatorMap.get(candidate.getId());
                if (ind == null) continue;

                List<IndicatorSourceData> sourceDataList =
                        sourceDataRepository.findByIndicatorIdAndIsFormulaRelatedTrue(ind.getId());

                List<IndicatorQueryResultDTO.FormulaSourceData> sourceDataDTOs = sourceDataList.stream()
                        .map(data -> IndicatorQueryResultDTO.FormulaSourceData.builder()
                                .dataName(data.getSourceDataName())
                                .formulaSymbol(data.getFormulaSymbol())
                                .dataType(data.getDataType() != null ? data.getDataType().name() : null)
                                .unit(data.getUnit())
                                .measurementMethod(data.getMeasurementMethod())
                                .isEssential(data.getIsEssential())
                                .build())
                        .collect(Collectors.toList());

                candidateDTOs.add(IndicatorQueryResultDTO.DatabaseCandidate.builder()
                        .id(ind.getId())
                        .indicatorName(ind.getIndicatorName())
                        .similarity(candidate.getSimilarity())
                        .indicatorType(ind.getIndicatorType() != null ? ind.getIndicatorType().name() : null)
                        .indicatorTypeDesc(ind.getIndicatorType() == IndicatorDefinition.IndicatorType.QUALITATIVE ?
                                "定性指标" : "定量指标")
                        .formula(ind.getFormula())
                        .formulaDescription(ind.getFormulaDescription())
                        .calculationMethod(ind.getCalculationMethod())
                        .unit(ind.getUnit())
                        .description(ind.getDescription())
                        .sourceDataList(sourceDataDTOs)
                        .build());
            }

            // 获取所有指标列表用于下拉选择
            List<IndicatorDefinition> allIndicators = indicatorRepository.findByIsActiveTrueOrderByCategoryAscIndicatorNameAsc();
            List<IndicatorQueryResultDTO.IndicatorOption> allOptions = allIndicators.stream()
                    .map(ind -> IndicatorQueryResultDTO.IndicatorOption.builder()
                            .id(ind.getId())
                            .indicatorName(ind.getIndicatorName())
                            .category(ind.getCategory())
                            .indicatorType(ind.getIndicatorType() != null ? ind.getIndicatorType().name() : null)
                            .build())
                    .collect(Collectors.toList());

            return IndicatorQueryResultDTO.builder()
                    .indicatorName(indicatorName)
                    .matchType("SIMILAR")
                    .databaseCandidates(candidateDTOs)
                    .defaultSelectedDbId(candidateDTOs.isEmpty() ? null : candidateDTOs.get(0).getId())
                    .allDatabaseIndicators(allOptions)
                    .statistics(IndicatorQueryResultDTO.Statistics.builder()
                            .matchedCount(candidateDTOs.size())
                            .totalDatabaseCount(allIndicators.size())
                            .build())
                    .build();
        }

        // 第三级：无匹配结果
        List<IndicatorDefinition> allIndicators = indicatorRepository.findByIsActiveTrueOrderByCategoryAscIndicatorNameAsc();
        List<IndicatorQueryResultDTO.IndicatorOption> allOptions = allIndicators.stream()
                .map(ind -> IndicatorQueryResultDTO.IndicatorOption.builder()
                        .id(ind.getId())
                        .indicatorName(ind.getIndicatorName())
                        .category(ind.getCategory())
                        .indicatorType(ind.getIndicatorType() != null ? ind.getIndicatorType().name() : null)
                        .build())
                .collect(Collectors.toList());

        return IndicatorQueryResultDTO.builder()
                .indicatorName(indicatorName)
                .matchType("NO_MATCH")
                .databaseCandidates(Collections.emptyList())
                .allDatabaseIndicators(allOptions)
                .statistics(IndicatorQueryResultDTO.Statistics.builder()
                        .matchedCount(0)
                        .totalDatabaseCount(allIndicators.size())
                        .build())
                .build();
    }

    /**
     * API分析单个指标（情况二）- 仅返回结果，不写库
     * 用户确认后通过 saveSelection 方法保存
     */
    public IndicatorAnalysisResult analyzeSingleIndicatorApi(IndicatorQueryRequest request) {
        String indicatorName = request.getIndicatorName().trim();
        log.info("API分析指标（仅返回结果，不写库）: {}", indicatorName);
        try {
            // 只调用API，返回结果（不写库）
            IndicatorAnalysisResult analysis = deepSeekApiClient.analyzeSingleIndicator(indicatorName, request.getDomain());
            return analysis;
        } catch (Exception e) {
            log.error("API分析失败: {}", indicatorName, e);
            throw new RuntimeException("API分析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 保存用户选择
     */
    @Transactional
    public IndicatorDefinition saveSelection(IndicatorSelectionRequest request) {
        log.info("保存用户选择: {}, 来源: {}", request.getOriginalName(), request.getSelectedSource());

        // 选择 DATABASE 来源时，也需要创建/更新记录（复制匹配指标的信息）
        Optional<IndicatorDefinition> existing =
                indicatorRepository.findByIndicatorName(request.getOriginalName());

        IndicatorDefinition entity;
        boolean isNew = existing.isEmpty();

        // 如果是从数据库相似度候选选择，获取匹配指标的详细信息
        IndicatorDefinition matchedIndicator = null;
        if ("DATABASE".equals(request.getSelectedSource()) && request.getSelectedDbId() != null) {
            matchedIndicator = indicatorRepository.findById(request.getSelectedDbId())
                    .orElseThrow(() -> new RuntimeException("指标不存在: " + request.getSelectedDbId()));
        }

        if (existing.isPresent()) {
            entity = existing.get();
            // 如果有匹配指标，更新详细信息
            if (matchedIndicator != null) {
                entity.setIndicatorType(matchedIndicator.getIndicatorType());
                entity.setFormula(matchedIndicator.getFormula());
                entity.setFormulaDescription(matchedIndicator.getFormulaDescription());
                entity.setCalculationMethod(matchedIndicator.getCalculationMethod());
                entity.setUnit(matchedIndicator.getUnit());
                entity.setDescription(matchedIndicator.getDescription());
                entity.setCategory(matchedIndicator.getCategory());
            } else if (request.getIndicatorType() != null) {
                entity.setIndicatorType(IndicatorDefinition.IndicatorType.valueOf(request.getIndicatorType()));
            }
            entity.setFormula(request.getFormula() != null ? request.getFormula() : entity.getFormula());
            entity.setFormulaDescription(request.getFormulaDescription() != null ? request.getFormulaDescription() : entity.getFormulaDescription());
            entity.setCalculationMethod(request.getCalculationMethod() != null ? request.getCalculationMethod() : entity.getCalculationMethod());
            entity.setUnit(request.getUnit() != null ? request.getUnit() : entity.getUnit());
            entity.setDescription(request.getDescription() != null ? request.getDescription() : entity.getDescription());
            entity.setIsFromAi("API".equals(request.getSelectedSource()));
        } else {
            // 构建新记录
            IndicatorDefinition.IndicatorType type = null;
            if (matchedIndicator != null) {
                type = matchedIndicator.getIndicatorType();
            } else if (request.getIndicatorType() != null) {
                type = IndicatorDefinition.IndicatorType.valueOf(request.getIndicatorType());
            }

            entity = IndicatorDefinition.builder()
                    .indicatorName(request.getOriginalName())
                    .indicatorType(type)
                    .formula(matchedIndicator != null ? matchedIndicator.getFormula() : request.getFormula())
                    .formulaDescription(matchedIndicator != null ? matchedIndicator.getFormulaDescription() : request.getFormulaDescription())
                    .calculationMethod(matchedIndicator != null ? matchedIndicator.getCalculationMethod() : request.getCalculationMethod())
                    .unit(matchedIndicator != null ? matchedIndicator.getUnit() : request.getUnit())
                    .description(matchedIndicator != null ? matchedIndicator.getDescription() : request.getDescription())
                    .category(matchedIndicator != null ? matchedIndicator.getCategory() : request.getCategory())
                    .isFromAi("API".equals(request.getSelectedSource()))
                    .isActive(true)
                    .build();
        }

        // 保存基本信息
        entity = indicatorRepository.save(entity);
        log.info("指标保存成功: {}", entity.getIndicatorName());

        // 如果是从数据库候选选择，复制数据源
        if (matchedIndicator != null) {
            copySourceData(matchedIndicator.getId(), entity.getId());
        }

        // 如果是新指标，计算向量并同步到 Chroma
        if (isNew) {
            try {
                List<Double> vector = indicatorVectorService.encodeText(entity.getIndicatorName());
                if (!vector.isEmpty()) {
                    // 添加到 Chroma 向量索引
                    chromaVectorService.addIndicators(
                            Collections.singletonList(entity.getId()),
                            Collections.singletonList(entity.getIndicatorName()),
                            Collections.singletonList(vector)
                    );
                    log.info("指标向量已添加到 Chroma: {}", entity.getIndicatorName());
                }
            } catch (Exception e) {
                log.error("指标向量计算或同步到 Chroma 失败: {}", entity.getIndicatorName(), e);
            }
        }

        return entity;
    }

    /**
     * 复制数据源
     */
    private void copySourceData(Long sourceIndicatorId, Long targetIndicatorId) {
        List<IndicatorSourceData> sourceList = sourceDataRepository.findByIndicatorId(sourceIndicatorId);
        for (IndicatorSourceData source : sourceList) {
            IndicatorSourceData newSource = IndicatorSourceData.builder()
                    .indicatorId(targetIndicatorId)
                    .sourceDataName(source.getSourceDataName())
                    .sourceDataCode(source.getSourceDataCode())
                    .measurementMethod(source.getMeasurementMethod())
                    .formulaSymbol(source.getFormulaSymbol())
                    .unit(source.getUnit())
                    .isEssential(source.getIsEssential())
                    .priority(source.getPriority())
                    .isFormulaRelated(source.getIsFormulaRelated())
                    .build();
            sourceDataRepository.save(newSource);
        }
        log.info("数据源复制完成: 从指标 {} 到指标 {}", sourceIndicatorId, targetIndicatorId);
    }

    // ============================================
    // 原有批量识别方法
    // ============================================

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

        // 现在 indicator_name 是全局唯一的，直接查询
        List<IndicatorDefinition> existingIndicators =
                indicatorRepository.findByIndicatorNameIn(request.getIndicators());

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
                indicatorRepository.findByIndicatorName(name);

        IndicatorDefinition entity;
        boolean isNew = existing.isEmpty();

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

        // 保存基本信息
        entity = indicatorRepository.save(entity);

        // 如果是新指标，计算向量并同步到 Chroma
        if (isNew) {
            try {
                List<Double> vector = indicatorVectorService.encodeText(name);
                if (!vector.isEmpty()) {
                    chromaVectorService.addIndicators(
                            Collections.singletonList(entity.getId()),
                            Collections.singletonList(entity.getIndicatorName()),
                            Collections.singletonList(vector)
                    );
                    log.info("指标向量已添加到 Chroma: {}", name);
                }
            } catch (Exception e) {
                log.error("指标向量同步到 Chroma 失败: {}", name, e);
            }
        }

        return entity;
    }

    private List<IndicatorSourceData> saveSourceData(Long indicatorId, IndicatorAnalysisResult analysis) {
        List<IndicatorSourceData> savedList = new ArrayList<>();

        sourceDataRepository.deleteByIndicatorId(indicatorId);

        if (analysis.getSourceDataList() != null && !analysis.getSourceDataList().isEmpty()) {
            for (SourceDataDTO data : analysis.getSourceDataList()) {
                IndicatorSourceData entity = IndicatorSourceData.builder()
                        .indicatorId(indicatorId)
                        .sourceDataName(data.getSourceDataName())
                        .sourceDataCode(generateDataCode(data.getSourceDataName()))
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

                IndicatorSourceData saved = sourceDataRepository.save(entity);
                savedList.add(saved);

                // 计算向量并同步到 Chroma
                try {
                    List<Double> vector = indicatorVectorService.encodeText(data.getSourceDataName());
                    if (!vector.isEmpty()) {
                        chromaVectorService.addSourceData(
                            Collections.singletonList(saved.getId()),
                            Collections.singletonList(saved.getSourceDataName()),
                            Collections.singletonList(vector)
                        );
                        log.debug("数据源向量已添加到 Chroma: {}", saved.getSourceDataName());
                    }
                } catch (Exception e) {
                    log.warn("数据源向量同步到 Chroma 失败: {}", saved.getSourceDataName(), e);
                }
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
