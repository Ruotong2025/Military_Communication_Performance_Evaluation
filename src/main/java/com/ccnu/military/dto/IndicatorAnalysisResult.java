package com.ccnu.military.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.List;

/**
 * AI指标分析结果DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "AI指标分析结果")
public class IndicatorAnalysisResult {

    @Schema(description = "指标名称")
    private String indicatorName;
    
    @Schema(description = "指标类型 QUANTITATIVE/QUALITATIVE")
    private String indicatorType;
    
    @Schema(description = "指标类型描述")
    private String indicatorTypeDesc;
    
    @Schema(description = "置信度")
    private Double confidence;
    
    @Schema(description = "指标描述")
    private String description;
    
    @Schema(description = "单位")
    private String unit;
    
    @Schema(description = "计算公式")
    private String formula;
    
    @Schema(description = "公式描述")
    private String formulaDescription;
    
    @Schema(description = "计算方法")
    private String calculationMethod;
    
    @Schema(description = "数据源提示")
    private String sourceDataHint;
    
    @Schema(description = "数据源列表")
    private List<SourceDataDTO> sourceDataList;
}
