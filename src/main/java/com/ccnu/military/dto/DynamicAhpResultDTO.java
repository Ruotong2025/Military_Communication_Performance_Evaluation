package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 动态AHP权重计算结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicAhpResultDTO {

    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 层级名称
     */
    private String levelName;

    /**
     * 矩阵类型
     */
    private String matrixType;

    /**
     * 父级维度编码
     */
    private String parentCode;

    /**
     * 维度数量
     */
    private Integer dimensionCount;

    /**
     * 参与比较的维度名称
     */
    private List<String> dimensionNames;

    /**
     * 维度权重列表
     */
    private List<DimensionWeight> weights;

    /**
     * 最大特征值
     */
    private BigDecimal lambdaMax;

    /**
     * 一致性指标CI
     */
    private BigDecimal ci;

    /**
     * 随机一致性指标RI
     */
    private BigDecimal ri;

    /**
     * 一致性比率CR
     */
    private BigDecimal cr;

    /**
     * 是否通过一致性检验
     */
    private Boolean consistent;

    /**
     * 维度权重
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionWeight {
        /**
         * 维度编码
         */
        private String code;

        /**
         * 维度名称
         */
        private String name;

        /**
         * 维度描述
         */
        private String description;

        /**
         * 权重值
         */
        private BigDecimal weight;

        /**
         * 排序
         */
        private Integer sortOrder;
    }

    /**
     * 跨层级综合权重
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CombinedWeight {
        /**
         * 层级名称
         */
        private String levelName;

        /**
         * 层级权重
         */
        private BigDecimal levelWeight;

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
        private BigDecimal primaryCombinedWeight;

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
         * 二级指标权重（在一级维度内）
         */
        private BigDecimal secondaryWeight;

        /**
         * 综合权重（层级权重 × 一级维度权重 × 二级指标权重）
         */
        private BigDecimal combinedWeight;

        /**
         * 完整路径
         */
        private String path;
    }
}
