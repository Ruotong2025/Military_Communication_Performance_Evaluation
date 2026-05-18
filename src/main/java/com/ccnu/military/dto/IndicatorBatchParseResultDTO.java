package com.ccnu.military.dto;

import lombok.*;
import java.util.List;

/**
 * 指标批量解析结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorBatchParseResultDTO {

    private Boolean success;
    private String message;
    private Integer totalCount;
    private Integer successCount;
    private Integer skippedCount;
    private Long processingTimeMs;
    private Integer estimatedTokens;
    private List<IndicatorResult> results;
    private Statistics statistics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndicatorResult {
        private String indicatorName;
        private String indicatorType;
        private String indicatorTypeDesc;
        private Double confidence;
        private String description;
        private String unit;
        private String formula;
        private String formulaDescription;
        private String calculationMethod;
        private String sourceDataHint;
        private List<FormulaSourceData> formulaRelatedData;
        private Boolean isNew;
        private Boolean isFromCache;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormulaSourceData {
        private String dataName;
        private String formulaSymbol;
        private String dataType;
        private String unit;
        private String measurementMethod;
        private Boolean isEssential;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Statistics {
        private Integer qualitativeCount;
        private Integer quantitativeCount;
        private Double qualitativeRatio;
        private Double quantitativeRatio;
        private Integer hitFromDb;
        private Integer newFromApi;
    }
}
