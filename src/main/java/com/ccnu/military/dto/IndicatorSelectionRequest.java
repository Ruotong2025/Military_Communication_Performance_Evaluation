package com.ccnu.military.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户选择保存请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户选择保存请求")
public class IndicatorSelectionRequest {

    @Schema(description = "用户输入的原始指标名称")
    private String originalName;

    @Schema(description = "选择的来源: DATABASE / API / MANUAL")
    private String selectedSource;

    @Schema(description = "选中的数据库记录ID（来源为DATABASE时使用）")
    private Long selectedDbId;

    @Schema(description = "指标类型: QUANTITATIVE / QUALITATIVE")
    private String indicatorType;

    @Schema(description = "计算公式")
    private String formula;

    @Schema(description = "公式说明")
    private String formulaDescription;

    @Schema(description = "计算方法")
    private String calculationMethod;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "指标大类")
    private String category;

    // 匹配关系字段
    @Schema(description = "匹配到的指标名称")
    private String matchedIndicatorName;

    @Schema(description = "匹配相似度")
    private Double matchSimilarity;
}
