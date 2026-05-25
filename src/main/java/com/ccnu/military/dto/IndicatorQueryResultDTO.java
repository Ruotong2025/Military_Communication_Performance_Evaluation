package com.ccnu.military.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 指标查询结果DTO
 * 包含三种情况：精确匹配、数据库语义相似度匹配、无匹配（需要调用API）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "指标查询结果")
public class IndicatorQueryResultDTO {

    @Schema(description = "用户输入的指标名称")
    private String indicatorName;

    // ============ 匹配类型 ============
    @Schema(description = "匹配类型: EXACT-精确匹配, SIMILAR-语义相似度, NO_MATCH-无匹配")
    private String matchType;

    // ============ 精确匹配（第一级命中）===========
    @Schema(description = "精确匹配的指标详情")
    private ExactMatchDTO exactMatch;

    // ============ 语义相似度匹配（第二级命中）===========
    @Schema(description = "数据库语义相似度匹配候选结果（按相似度降序）")
    private List<DatabaseCandidate> databaseCandidates;

    @Schema(description = "默认选中的数据库记录ID（相似度最高）")
    private Long defaultSelectedDbId;

    // ============ 全部指标列表（下拉框用）===========
    @Schema(description = "数据库全部指标列表（用于下拉选择）")
    private List<IndicatorOption> allDatabaseIndicators;

    // ============ 统计信息 ============
    @Schema(description = "统计信息")
    private Statistics statistics;

    /**
     * 精确匹配详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "精确匹配详情")
    public static class ExactMatchDTO {

        @Schema(description = "数据库记录ID")
        private Long id;

        @Schema(description = "指标名称")
        private String indicatorName;

        @Schema(description = "指标类型: QUANTITATIVE / QUALITATIVE")
        private String indicatorType;

        @Schema(description = "指标类型描述")
        private String indicatorTypeDesc;

        @Schema(description = "计算公式")
        private String formula;

        @Schema(description = "公式文字说明")
        private String formulaDescription;

        @Schema(description = "计算方法")
        private String calculationMethod;

        @Schema(description = "单位")
        private String unit;

        @Schema(description = "描述")
        private String description;

        @Schema(description = "公式相关数据源")
        private List<FormulaSourceData> sourceDataList;
    }

    /**
     * 数据库候选结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "数据库候选结果")
    public static class DatabaseCandidate {

        @Schema(description = "数据库记录ID")
        private Long id;

        @Schema(description = "数据库中的指标名称")
        private String indicatorName;

        @Schema(description = "语义相似度 (0.0 - 1.0)")
        private Double similarity;

        @Schema(description = "指标类型: QUANTITATIVE / QUALITATIVE")
        private String indicatorType;

        @Schema(description = "指标类型描述")
        private String indicatorTypeDesc;

        @Schema(description = "计算公式")
        private String formula;

        @Schema(description = "公式文字说明")
        private String formulaDescription;

        @Schema(description = "计算方法")
        private String calculationMethod;

        @Schema(description = "单位")
        private String unit;

        @Schema(description = "描述")
        private String description;

        @Schema(description = "公式相关数据源")
        private List<FormulaSourceData> sourceDataList;
    }

    /**
     * 公式数据源
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "公式数据源")
    public static class FormulaSourceData {

        @Schema(description = "数据名称")
        private String dataName;

        @Schema(description = "公式符号")
        private String formulaSymbol;

        @Schema(description = "数据类型")
        private String dataType;

        @Schema(description = "单位")
        private String unit;

        @Schema(description = "测量方法")
        private String measurementMethod;

        @Schema(description = "是否必需")
        private Boolean isEssential;
    }

    /**
     * 指标下拉选项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "指标下拉选项")
    public static class IndicatorOption {

        @Schema(description = "指标ID")
        private Long id;

        @Schema(description = "指标名称")
        private String indicatorName;

        @Schema(description = "指标大类")
        private String category;

        @Schema(description = "指标类型")
        private String indicatorType;
    }

    /**
     * 统计信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "统计信息")
    public static class Statistics {

        @Schema(description = "相似度匹配结果数量")
        private Integer matchedCount;

        @Schema(description = "数据库指标总数")
        private Integer totalDatabaseCount;
    }
}
