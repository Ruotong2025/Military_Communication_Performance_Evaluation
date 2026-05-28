package com.ccnu.military.dto;

import com.ccnu.military.enums.SourceDataSelectionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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

    @Schema(description = "匹配到的指标名称")
    private String matchedIndicatorName;

    @Schema(description = "匹配相似度")
    private Double matchSimilarity;

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

    @Schema(description = "数据源字段映射列表")
    private List<SourceDataMapping> sourceDataMappings;

    /**
     * 数据源字段映射
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceDataMapping {

        @Schema(description = "数据源名称")
        private String sourceDataName;

        @Schema(description = "公式符号（如：Ps, Pn）")
        private String formulaSymbol;

        @Schema(description = "单位")
        private String unit;

        @Schema(description = "数据类型: NUMERIC / PERCENTAGE")
        private String dataType;

        @Schema(description = "选择类型: EXISTING_DATABASE / API_RECOMMENDED")
        @Builder.Default
        private SourceDataSelectionType selectionType = SourceDataSelectionType.API_RECOMMENDED;

        @Schema(description = "关联的已有数据源ID（selectionType=EXISTING_DATABASE时使用）")
        private Long relatedSourceDataId;

        @Schema(description = "选中的数据库字段名（selectionType=EXISTING_DATABASE时使用）")
        private String selectedField;

        @Schema(description = "测量方法说明")
        private String measurementMethod;
    }
}
