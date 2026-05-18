package com.ccnu.military.dto;

import lombok.*;
import java.util.List;

/**
 * AI指标分析结果DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndicatorAnalysisResult {

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
    private List<SourceData> sourceDataList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SourceData {
        private String dataName;
        private String measurementMethod;
        private String dataType;
        private String unit;
        private Integer priority;
        private Double confidence;
        private Boolean isFormulaRelated;
        private String formulaSymbol;
        private Boolean isEssential;
    }
}
