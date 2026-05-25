package com.ccnu.military.service;

import com.ccnu.military.entity.IndicatorDefinition;
import com.ccnu.military.entity.IndicatorSourceData;
import com.ccnu.military.entity.MatchResult;
import com.ccnu.military.entity.MatchResult.SimilarityCandidate;
import com.ccnu.military.repository.IndicatorDefinitionRepository;
import com.ccnu.military.repository.IndicatorSourceDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 语义相似度服务
 * 实现三级匹配流程：精确匹配 -> 语义相似度(Chroma加速) -> 无匹配
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticSimilarityService {

    private final IndicatorDefinitionRepository indicatorRepository;
    private final IndicatorSourceDataRepository sourceDataRepository;
    private final IndicatorVectorService vectorService;
    private final ChromaVectorService chromaVectorService;

    /**
     * 三级匹配入口
     *
     * @param query 用户输入的查询词
     * @return 匹配结果
     */
    public MatchResult matchIndicator(String query) {
        if (query == null || query.trim().isEmpty()) {
            return MatchResult.noMatch();
        }

        String trimmedQuery = query.trim();
        log.info("开始三级匹配: query='{}'", trimmedQuery);

        // 第一级：精确匹配
        MatchResult exactResult = tryExactMatch(trimmedQuery);
        if (exactResult != null) {
            return exactResult;
        }

        // 第二级：语义相似度（优先使用 FAISS）
        MatchResult similarResult = trySemanticSimilarity(trimmedQuery);
        if (similarResult != null) {
            return similarResult;
        }

        // 第三级：无匹配结果
        return MatchResult.noMatch();
    }

    /**
     * 第一级：精确匹配
     * 根据 indicator_name 完全相同进行匹配
     */
    private MatchResult tryExactMatch(String query) {
        Optional<IndicatorDefinition> exactMatch = indicatorRepository.findByIndicatorName(query);

        if (exactMatch.isPresent()) {
            log.info("第一级命中(精确匹配): '{}'", query);
            return MatchResult.exact(exactMatch.get());
        }

        log.debug("精确匹配未命中: '{}'", query);
        return null;
    }

    /**
     * 第二级：语义相似度
     * 使用 Chroma 向量数据库加速搜索
     */
    private MatchResult trySemanticSimilarity(String query) {
        try {
            // 尝试使用 Chroma 搜索
            int chromaCount = chromaVectorService.getIndicatorCount();
            if (chromaCount > 0) {
                log.debug("使用 Chroma 索引搜索相似指标，当前索引数量: {}", chromaCount);
                return trySemanticSimilarityWithChroma(query);
            }
        } catch (Exception e) {
            log.error("Chroma 搜索失败: {}", e.getMessage());
        }

        // Chroma 不可用时，返回 null（不再降级到 MySQL）
        log.warn("Chroma 索引为空或不可用，无法进行语义相似度搜索");
        return null;
    }

    /**
     * 使用 Chroma 进行语义相似度搜索
     */
    private MatchResult trySemanticSimilarityWithChroma(String query) {
        // 计算查询向量
        List<Double> queryVector = vectorService.encodeText(query);
        if (queryVector.isEmpty()) {
            log.error("无法计算查询词的向量: '{}'", query);
            return null;
        }

        // 使用 Chroma 搜索
        List<ChromaVectorService.SearchResult> results = chromaVectorService.searchIndicators(queryVector, 5);

        if (results.isEmpty()) {
            log.debug("Chroma 未找到相似指标");
            return null;
        }

        log.debug("Chroma 找到 {} 个相似指标", results.size());

        // 获取指标详情
        List<Long> ids = results.stream()
                .map(ChromaVectorService.SearchResult::getId)
                .collect(Collectors.toList());

        List<IndicatorDefinition> indicators = indicatorRepository.findAllById(ids);

        // 按 Chroma 返回顺序构建结果（分数已经是降序）
        Map<Long, Double> scoreMap = results.stream()
                .collect(Collectors.toMap(
                        ChromaVectorService.SearchResult::getId,
                        ChromaVectorService.SearchResult::getScore
                ));

        List<SimilarityCandidate> candidates = indicators.stream()
                .map(ind -> SimilarityCandidate.builder()
                        .id(ind.getId())
                        .name(ind.getIndicatorName())
                        .similarity(scoreMap.getOrDefault(ind.getId(), 0.0))
                        .build())
                .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
                .limit(3)
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            return null;
        }

        log.info("第二级命中(Chroma语义相似度): query='{}', Top-3: {}", query, candidates);
        return MatchResult.similar(candidates);
    }

    /**
     * 计算查询词与候选词的语义相似度（保留原有方法，兼容旧代码）
     *
     * @param query      查询词
     * @param candidates 候选词列表
     * @return 按相似度降序排列的结果列表
     */
    public List<SimilarityResult> calculateSimilarity(String query, List<String> candidates) {
        if (query == null || query.trim().isEmpty() || candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // 计算 query 向量
        List<Double> queryVector = vectorService.encodeText(query.trim());
        if (queryVector.isEmpty()) {
            return Collections.emptyList();
        }

        // 计算候选词向量
        List<List<Double>> candidateVectors = vectorService.encodeTexts(candidates);
        if (candidateVectors.isEmpty()) {
            return Collections.emptyList();
        }

        // 计算相似度
        List<SimilarityResult> results = new ArrayList<>();
        for (int i = 0; i < candidates.size() && i < candidateVectors.size(); i++) {
            double similarity = vectorService.computeCosineSimilarity(queryVector, candidateVectors.get(i));
            results.add(new SimilarityResult(null, candidates.get(i), similarity));
        }

        // 按相似度降序排列
        results.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));

        return results;
    }

    /**
     * 计算相似度并返回与数据库ID的映射
     */
    public List<SimilarityResult> calculateSimilarityWithIndicators(
            String query,
            List<IndicatorDefinition> indicators) {

        if (indicators == null || indicators.isEmpty()) {
            return Collections.emptyList();
        }

        // 提取指标名称列表，同时记录索引
        List<String> names = new ArrayList<>();
        Map<String, IndicatorDefinition> nameToIndicator = new HashMap<>();

        for (IndicatorDefinition ind : indicators) {
            String name = ind.getIndicatorName();
            names.add(name);
            nameToIndicator.put(name, ind);
        }

        // 计算相似度
        List<SimilarityResult> similarityResults = calculateSimilarity(query, names);

        // 设置指标ID
        for (SimilarityResult result : similarityResults) {
            IndicatorDefinition ind = nameToIndicator.get(result.getName());
            if (ind != null) {
                result.setId(ind.getId());
            }
        }

        return similarityResults;
    }

    /**
     * 搜索相似数据源（使用 Chroma）
     *
     * @param query 查询词
     * @param topK 返回数量
     * @return 相似数据源列表（包含关联的指标信息）
     */
    public List<SourceDataSimilarityResult> searchSimilarSourceData(String query, int topK) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // 计算查询向量
            List<Double> queryVector = vectorService.encodeText(query);
            if (queryVector.isEmpty()) {
                log.error("无法计算查询词的向量: '{}'", query);
                return Collections.emptyList();
            }

            // 使用 Chroma 搜索
            List<ChromaVectorService.SearchResult> chromaResults = chromaVectorService.searchSourceData(queryVector, topK);

            if (chromaResults.isEmpty()) {
                log.debug("未找到相似数据源");
                return Collections.emptyList();
            }

            // 获取数据源详情
            List<Long> sourceDataIds = chromaResults.stream()
                    .map(ChromaVectorService.SearchResult::getId)
                    .collect(Collectors.toList());

            List<IndicatorSourceData> sourceDataList = sourceDataRepository.findAllById(sourceDataIds);

            // 获取关联的指标信息
            Set<Long> indicatorIds = sourceDataList.stream()
                    .map(IndicatorSourceData::getIndicatorId)
                    .collect(Collectors.toSet());

            Map<Long, IndicatorDefinition> indicatorMap = indicatorRepository.findAllById(indicatorIds)
                    .stream()
                    .collect(Collectors.toMap(IndicatorDefinition::getId, i -> i));

            // 构建结果
            Map<Long, Double> scoreMap = chromaResults.stream()
                    .collect(Collectors.toMap(
                            ChromaVectorService.SearchResult::getId,
                            ChromaVectorService.SearchResult::getScore
                    ));

            List<SourceDataSimilarityResult> results = new ArrayList<>();
            for (IndicatorSourceData sourceData : sourceDataList) {
                IndicatorDefinition indicator = indicatorMap.get(sourceData.getIndicatorId());

                SourceDataSimilarityResult result = new SourceDataSimilarityResult();
                result.setSourceDataId(sourceData.getId());
                result.setSourceDataName(sourceData.getSourceDataName());
                result.setIndicatorId(sourceData.getIndicatorId());
                result.setIndicatorName(indicator != null ? indicator.getIndicatorName() : null);
                result.setSimilarity(scoreMap.getOrDefault(sourceData.getId(), 0.0));
                result.setFormulaSymbol(sourceData.getFormulaSymbol());
                result.setUnit(sourceData.getUnit());

                results.add(result);
            }

            // 按相似度降序
            results.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));

            return results;

        } catch (Exception e) {
            log.error("搜索相似数据源失败: {}", query, e);
            return Collections.emptyList();
        }
    }

    /**
     * 相似度结果（内部类）
     */
    @lombok.Data
    public static class SimilarityResult {
        private Long id;
        private String name;
        private Double similarity;

        public SimilarityResult() {}

        public SimilarityResult(Long id, String name, Double similarity) {
            this.id = id;
            this.name = name;
            this.similarity = similarity;
        }
    }

    /**
     * 数据源相似度结果
     */
    @lombok.Data
    public static class SourceDataSimilarityResult {
        private Long sourceDataId;
        private String sourceDataName;
        private Long indicatorId;
        private String indicatorName;
        private Double similarity;
        private String formulaSymbol;
        private String unit;
    }
}
