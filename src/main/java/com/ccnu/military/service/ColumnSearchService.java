package com.ccnu.military.service;

import com.ccnu.military.dto.ColumnSimilarityResult;
import com.ccnu.military.entity.IndicatorSourceData;
import com.ccnu.military.repository.IndicatorSourceDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 字段相似度搜索服务
 * 从 indicator_source_data 表搜索已有数据源（基于 Chroma 向量搜索）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ColumnSearchService {

    private final IndicatorVectorService indicatorVectorService;
    private final ChromaVectorService chromaVectorService;
    private final IndicatorSourceDataRepository sourceDataRepository;

    /**
     * 搜索相似的数据源（来自 indicator_source_data 表）
     * @param dataName 数据源名称（如"成功传输的数据量"）
     * @param limit 返回数量
     * @return 相似数据源列表
     */
    public List<ColumnSimilarityResult> searchSimilarColumns(String dataName, int limit) {
        if (dataName == null || dataName.trim().isEmpty()) {
            log.warn("[调试] searchSimilarColumns 收到空查询: dataName={}", dataName);
            return Collections.emptyList();
        }

        String query = dataName.trim();
        log.info("========================================");
        log.info("[调试] searchSimilarColumns 收到查询请求");
        log.info("[调试] - dataName: {}", query);
        log.info("[调试] - limit: {}", limit);
        log.info("========================================");

        return searchFromChroma(query, limit);
    }

    /**
     * 从 Chroma source_data 集合搜索
     */
    private List<ColumnSimilarityResult> searchFromChroma(String query, int limit) {
        log.info("[调试] ===== 进入 searchFromChroma =====");
        log.info("[调试] - query: {}", query);

        // 1. 对查询词进行向量编码
        List<Double> queryVector = indicatorVectorService.encodeText(query);
        log.info("[调试] 向量编码完成，向量维度: {}", queryVector.size());

        if (queryVector.isEmpty() || queryVector.stream().allMatch(v -> v == 0.0)) {
            log.warn("[调试] 向量编码返回空或全零，返回空结果");
            return Collections.emptyList();
        }

        // 2. 从 Chroma source_data 集合搜索
        List<ChromaVectorService.SearchResult> chromaResults =
                chromaVectorService.searchSourceData(queryVector, limit);

        log.info("[调试] Chroma 搜索返回 {} 条结果", chromaResults.size());

        if (chromaResults.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 获取对应的 IndicatorSourceData 详情
        List<Long> ids = chromaResults.stream()
                .map(ChromaVectorService.SearchResult::getId)
                .collect(Collectors.toList());

        Map<Long, IndicatorSourceData> dataMap = sourceDataRepository.findAllById(ids)
                .stream()
                .collect(Collectors.toMap(IndicatorSourceData::getId, d -> d));

        // 4. 构建返回结果
        List<ColumnSimilarityResult> results = new ArrayList<>();
        for (ChromaVectorService.SearchResult cr : chromaResults) {
            IndicatorSourceData sd = dataMap.get(cr.getId());
            if (sd != null) {
                results.add(ColumnSimilarityResult.builder()
                        .columnName(sd.getSourceDataCode() != null ? sd.getSourceDataCode() : sd.getSourceDataName())
                        .columnLabel(sd.getSourceDataName())
                        .tableName("indicator_source_data")
                        .tableLabel("已有数据源")
                        .similarity(cr.getScore())
                        .dataType(sd.getDataType() != null ? sd.getDataType().name() : "NUMERIC")
                        .unit(sd.getUnit())
                        .description(sd.getMeasurementMethod())
                        .relatedSourceDataId(sd.getId())  // 添加数据源ID关联
                        .build());
            }
        }

        // 打印 top 结果
        if (!results.isEmpty()) {
            log.info("[调试] Top 结果详情:");
            for (int i = 0; i < Math.min(3, results.size()); i++) {
                ColumnSimilarityResult r = results.get(i);
                log.info("[调试]   {}. {} 相似度={}", i + 1, r.getColumnLabel(), r.getSimilarity());
            }
        }

        return results;
    }

    /**
     * 从 API 分析结果中提取建议的字段
     * @param apiResult API 分析结果
     * @return 建议的字段列表
     */
    public List<ColumnSimilarityResult> extractFromApiResult(com.ccnu.military.dto.IndicatorAnalysisResult apiResult) {
        List<ColumnSimilarityResult> suggestions = new ArrayList<>();

        if (apiResult == null || apiResult.getSourceDataList() == null) {
            return suggestions;
        }

        for (com.ccnu.military.dto.SourceDataDTO sourceData : apiResult.getSourceDataList()) {
            String dataName = sourceData.getSourceDataName();
            String measurementMethod = sourceData.getMeasurementMethod();
            String dataType = sourceData.getDataType();

            // 搜索相似数据源
            List<ColumnSimilarityResult> similarData = searchSimilarColumns(dataName, 3);

            // 如果找到相似数据源，使用相似度最高的
            if (!similarData.isEmpty()) {
                ColumnSimilarityResult best = similarData.get(0);
                best.setSimilarity(0.95); // API 建议默认高置信度
                suggestions.add(best);
            } else {
                // 没有找到匹配，创建占位建议
                suggestions.add(ColumnSimilarityResult.builder()
                        .columnName(dataName)
                        .columnLabel(dataName)
                        .tableName("indicator_source_data")
                        .tableLabel("已有数据源")
                        .similarity(0.8)
                        .dataType(dataType != null ? dataType : "NUMERIC")
                        .description(measurementMethod)
                        .build());
            }
        }

        return suggestions;
    }
}
