package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 动态AHP完整结果（跨层级综合权重）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicAhpCompleteResultDTO {

    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 专家ID
     */
    private Long expertId;

    /**
     * 专家名称
     */
    private String expertName;

    /**
     * 所有层级列表
     */
    private List<LevelResult> levelResults;

    /**
     * 层级间权重结果
     */
    private List<DynamicAhpResultDTO.DimensionWeight> levelWeights;

    /**
     * 完整的叶子指标综合权重列表
     */
    private List<DynamicAhpResultDTO.CombinedWeight> allLeaves;

    /**
     * 综合权重总和（应为100%）
     */
    private BigDecimal totalWeight;

    /**
     * 是否所有矩阵都通过一致性检验
     */
    private Boolean allConsistent;

    /**
     * 未通过一致性检验的矩阵列表
     */
    private List<String> inconsistentMatrices;

    /**
     * 层级结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelResult {
        /**
         * 层级名称
         */
        private String levelName;

        /**
         * 一级维度权重列表
         */
        private List<DynamicAhpResultDTO.DimensionWeight> primaryWeights;

        /**
         * 二级指标权重映射（key：一级维度编码）
         */
        private List<PrimaryWithSecondaries> primaries;
    }

    /**
     * 一级维度及其二级指标
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrimaryWithSecondaries {
        /**
         * 层级名称
         */
        private String levelName;

        /**
         * 一级维度编码
         */
        private String primaryCode;

        /**
         * 一级维度名称
         */
        private String primaryName;

        /**
         * 一级维度描述
         */
        private String primaryDescription;

        /**
         * 一级维度权重（层级内）
         */
        private BigDecimal primaryWeight;

        /**
         * 一级维度综合权重 = 层级权重 × 一级权重
         */
        private BigDecimal combinedWeight;

        /**
         * 一级维度权重是否通过一致性检验
         */
        private Boolean primaryConsistent;

        /**
         * 一级维度CR
         */
        private BigDecimal primaryCr;

        /**
         * 二级指标权重列表
         */
        private List<SecondaryWeight> secondaries;
    }

    /**
     * 二级指标权重
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecondaryWeight {
        /**
         * 二级指标编码
         */
        private String secondaryCode;

        /**
         * 二级指标名称
         */
        private String secondaryName;

        /**
         * 二级指标描述
         */
        private String secondaryDescription;

        /**
         * 指标类型（QUANTITATIVE/QUALITATIVE）
         */
        private String metricType;

        /**
         * 指标权重（一级维度内）
         */
        private BigDecimal secondaryWeight;

        /**
         * 综合权重（层级×一级维度×二级指标）
         */
        private BigDecimal combinedWeight;

        /**
         * 是否通过一致性检验
         */
        private Boolean consistent;

        /**
         * CR值
         */
        private BigDecimal cr;
    }
}
