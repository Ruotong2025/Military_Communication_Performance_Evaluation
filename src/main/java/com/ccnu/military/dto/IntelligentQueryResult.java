package com.ccnu.military.dto;

import com.ccnu.military.entity.IndicatorDefinition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 智能查询结果DTO
 * 用于新的四选一UI模式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "智能查询结果")
public class IntelligentQueryResult {

    @Schema(description = "原始查询名称")
    private String originalName;

    @Schema(description = "Top3相似度候选列表")
    private List<CandidateOption> candidates;

    @Schema(description = "全部指标下拉列表")
    private List<IndicatorOption> allIndicators;

    @Schema(description = "是否命中精确匹配")
    private Boolean hasExactMatch;

    @Schema(description = "数据库指标总数")
    private Integer totalDatabaseCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "候选选项")
    public static class CandidateOption {

        @Schema(description = "指标ID")
        private Long id;

        @Schema(description = "指标名称")
        private String name;

        @Schema(description = "相似度 (0-1)")
        private Double similarity;

        @Schema(description = "指标类型")
        private String indicatorType;

        @Schema(description = "指标类型描述")
        private String indicatorTypeDesc;

        @Schema(description = "单位")
        private String unit;

        @Schema(description = "描述")
        private String description;

        @Schema(description = "计算公式")
        private String formula;

        @Schema(description = "公式说明")
        private String formulaDescription;

        @Schema(description = "计算方法")
        private String calculationMethod;

        @Schema(description = "数据源列表")
        private List<SourceDataDTO> sourceDataList;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "指标下拉选项")
    public static class IndicatorOption {

        @Schema(description = "指标ID")
        private Long id;

        @Schema(description = "指标名称")
        private String name;

        @Schema(description = "指标类型")
        private String indicatorType;
    }
}
