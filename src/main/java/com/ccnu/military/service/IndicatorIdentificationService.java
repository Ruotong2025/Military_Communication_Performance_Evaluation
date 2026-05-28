package com.ccnu.military.service;

import com.ccnu.military.dto.*;
import com.ccnu.military.entity.IndicatorDefinition;
import com.ccnu.military.entity.IndicatorSourceData;
import com.ccnu.military.entity.MatchResult;
import com.ccnu.military.enums.SourceDataSelectionType;
import com.ccnu.military.repository.IndicatorDefinitionRepository;
import com.ccnu.military.repository.IndicatorSourceDataRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Optional;

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
    private final ObjectMapper objectMapper = new ObjectMapper();

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
     * 场景1: 选择已有指标 → 记录 related_indicators 映射
     * 场景2: 选择API分析 → 新建指标 + 处理数据源
     * 场景3: 手动输入 → 新建指标
     */
    @Transactional
    public IndicatorDefinition saveSelection(IndicatorSelectionRequest request) {
        log.info("保存用户选择: {}, 来源: {}", request.getOriginalName(), request.getSelectedSource());

        IndicatorDefinition entity;

        if ("DATABASE".equals(request.getSelectedSource())) {
            // 场景1: 选择已有指标 → 记录关联关系
            entity = handleDatabaseSelection(request);
        } else if ("API".equals(request.getSelectedSource())) {
            // 场景2: API分析 → 新建指标 + 处理数据源
            entity = handleApiSelection(request);
        } else {
            // 场景3: 手动输入 → 新建指标
            entity = handleManualInput(request);
        }

        return entity;
    }

    /**
     * 场景1: 选择已有指标 - 仅记录关联关系
     */
    private IndicatorDefinition handleDatabaseSelection(IndicatorSelectionRequest request) {
        IndicatorDefinition indicator = indicatorRepository.findById(request.getSelectedDbId())
                .orElseThrow(() -> new RuntimeException("指标不存在: " + request.getSelectedDbId()));

        // 构建关联映射
        Map<String, Object> association = new LinkedHashMap<>();
        association.put("originalName", request.getOriginalName());
        association.put("matchedIndicatorId", indicator.getId());
        association.put("matchedIndicatorName", indicator.getIndicatorName());
        association.put("matchedAt", LocalDateTime.now().toString());
        association.put("similarity", request.getMatchSimilarity() != null ? request.getMatchSimilarity() : 1.0);

        // 读取现有 related_indicators，追加新映射
        Map<String, Object> relatedIndicators = parseJson(indicator.getRelatedIndicators());
        List<Map<String, Object>> associations = (List<Map<String, Object>>) relatedIndicators.getOrDefault("associations", new ArrayList<>());
        associations.add(association);
        relatedIndicators.put("associations", associations);

        indicator.setRelatedIndicators(toJson(relatedIndicators));
        IndicatorDefinition saved = indicatorRepository.save(indicator);
        log.info("已记录指标关联: 输入 '{}' → 已有指标 '{}'", request.getOriginalName(), indicator.getIndicatorName());
        return saved;
    }

    /**
     * 场景2: API分析 - 新建指标 + 处理数据源
     */
    private IndicatorDefinition handleApiSelection(IndicatorSelectionRequest request) {
        // 1. 创建新指标
        IndicatorDefinition.IndicatorType type = null;
        if (request.getIndicatorType() != null) {
            type = IndicatorDefinition.IndicatorType.valueOf(request.getIndicatorType());
        }

        IndicatorDefinition indicator = IndicatorDefinition.builder()
                .indicatorName(request.getOriginalName())
                .indicatorType(type)
                .formula(request.getFormula())
                .formulaDescription(request.getFormulaDescription())
                .calculationMethod(request.getCalculationMethod())
                .description(request.getDescription())
                .unit(request.getUnit())
                .category(request.getCategory())
                .isFromAi(true)
                .aiConfidence(BigDecimal.valueOf(85.0))
                .isActive(true)
                .build();
        indicator = indicatorRepository.save(indicator);
        log.info("新指标创建成功: {}", indicator.getIndicatorName());

        // 2. 处理每个数据源
        List<Map<String, Object>> sourceDataMappings = new ArrayList<>();

        if (request.getSourceDataMappings() != null) {
            for (IndicatorSelectionRequest.SourceDataMapping mapping : request.getSourceDataMappings()) {
                Map<String, Object> sourceMapping = new LinkedHashMap<>();
                sourceMapping.put("apiSourceDataName", mapping.getSourceDataName());
                sourceMapping.put("selectedField", mapping.getSelectedField());
                sourceMapping.put("selectionType", mapping.getSelectionType() != null ? mapping.getSelectionType().name() : "API_RECOMMENDED");
                sourceMapping.put("selectedAt", LocalDateTime.now().toString());

                if (mapping.getSelectionType() == SourceDataSelectionType.EXISTING_DATABASE) {
                    // 选择已有数据源 → 记录关联关系
                    sourceMapping.put("relatedSourceDataId", mapping.getRelatedSourceDataId());
                    sourceDataMappings.add(sourceMapping);
                    log.info("数据源 '{}' 关联到已有数据源 ID: {}", mapping.getSourceDataName(), mapping.getRelatedSourceDataId());
                } else {
                    // API推荐 → 新增 source_data 记录
                    IndicatorSourceData sourceData = IndicatorSourceData.builder()
                            .indicatorId(indicator.getId())
                            .sourceDataName(mapping.getSourceDataName())
                            .formulaSymbol(mapping.getFormulaSymbol())
                            .unit(mapping.getUnit())
                            .measurementMethod(mapping.getMeasurementMethod())
                            .dataType(mapping.getDataType() != null ?
                                    IndicatorSourceData.DataType.valueOf(mapping.getDataType()) : null)
                            .isFormulaRelated(true)
                            .isEssential(true)
                            .priority(1)
                            .build();
                    sourceData = sourceDataRepository.save(sourceData);
                    log.info("新增API数据源: {}, ID: {}", mapping.getSourceDataName(), sourceData.getId());

                    // 将新建的数据源ID也添加到关联关系中
                    sourceMapping.put("relatedSourceDataId", sourceData.getId());
                    sourceDataMappings.add(sourceMapping);

                    // 同步向量到 Chroma
                    try {
                        List<Double> vector = indicatorVectorService.encodeText(sourceData.getSourceDataName());
                        if (!vector.isEmpty()) {
                            chromaVectorService.addSourceData(
                                    Collections.singletonList(sourceData.getId()),
                                    Collections.singletonList(sourceData.getSourceDataName()),
                                    Collections.singletonList(vector)
                            );
                            log.debug("数据源向量已添加到 Chroma: {}", sourceData.getSourceDataName());
                        }
                    } catch (Exception e) {
                        log.warn("数据源向量同步到 Chroma 失败: {}", sourceData.getSourceDataName(), e);
                    }
                }
            }
        }

        // 3. 保存数据源关联关系
        if (!sourceDataMappings.isEmpty()) {
            Map<String, Object> relatedSourceData = new LinkedHashMap<>();
            relatedSourceData.put("mappings", sourceDataMappings);
            indicator.setRelatedSourceData(toJson(relatedSourceData));
            indicatorRepository.save(indicator);
        }

        // 4. 同步向量到 Chroma
        try {
            List<Double> vector = indicatorVectorService.encodeText(indicator.getIndicatorName());
            if (!vector.isEmpty()) {
                chromaVectorService.addIndicators(
                        Collections.singletonList(indicator.getId()),
                        Collections.singletonList(indicator.getIndicatorName()),
                        Collections.singletonList(vector)
                );
                log.info("指标向量已添加到 Chroma: {}", indicator.getIndicatorName());
            }
        } catch (Exception e) {
            log.error("指标向量同步到 Chroma 失败: {}", indicator.getIndicatorName(), e);
        }

        return indicator;
    }

    /**
     * 场景3: 手动输入 - 新建指标
     */
    private IndicatorDefinition handleManualInput(IndicatorSelectionRequest request) {
        IndicatorDefinition.IndicatorType type = null;
        if (request.getIndicatorType() != null) {
            type = IndicatorDefinition.IndicatorType.valueOf(request.getIndicatorType());
        }

        IndicatorDefinition indicator = IndicatorDefinition.builder()
                .indicatorName(request.getOriginalName())
                .indicatorType(type)
                .formula(request.getFormula())
                .formulaDescription(request.getFormulaDescription())
                .calculationMethod(request.getCalculationMethod())
                .description(request.getDescription())
                .unit(request.getUnit())
                .category(request.getCategory())
                .isFromAi(false)
                .isActive(true)
                .build();

        indicator = indicatorRepository.save(indicator);
        log.info("手动输入指标创建成功: {}", indicator.getIndicatorName());

        // 同步向量到 Chroma
        try {
            List<Double> vector = indicatorVectorService.encodeText(indicator.getIndicatorName());
            if (!vector.isEmpty()) {
                chromaVectorService.addIndicators(
                        Collections.singletonList(indicator.getId()),
                        Collections.singletonList(indicator.getIndicatorName()),
                        Collections.singletonList(vector)
                );
            }
        } catch (Exception e) {
            log.error("指标向量同步到 Chroma 失败: {}", indicator.getIndicatorName(), e);
        }

        return indicator;
    }

    /**
     * 解析 JSON 字符串为 Map
     */
    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isEmpty()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, LinkedHashMap.class);
        } catch (JsonProcessingException e) {
            log.warn("JSON解析失败: {}", json, e);
            return new LinkedHashMap<>();
        }
    }

    /**
     * 将 Map 转换为 JSON 字符串
     */
    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败", e);
            return "{}";
        }
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
