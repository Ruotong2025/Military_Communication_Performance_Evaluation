package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 动态AHP集结响应DTO
 * 包含集结后的所有结果数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicAhpAggregationResponse {

    // ==================== 基本信息 ====================

    /**
     * 专家组ID（保存后有值，预览时为null）
     */
    private String groupId;

    /**
     * 指标模板ID
     */
    private Long templateId;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 参与专家人数
     */
    private Integer expertCount;

    /**
     * 专家ID列表
     */
    private List<Long> expertIds;

    // ==================== 专家权重明细 ====================

    /**
     * 专家权重明细列表
     */
    private List<ExpertWeightDetail> expertWeights;

    // ==================== 一致性检验 ====================

    /**
     * 一致性检验结果（key为矩阵标识，value为CR等）
     */
    private Map<String, CrResult> crResults;

    // ==================== 集体判断矩阵 ====================

    /**
     * 层级间矩阵
     */
    private CollectiveMatrix levelBetweenMatrix;

    /**
     * 一级维度间矩阵（按层级分组）
     * key: levelName
     */
    private Map<String, CollectiveMatrix> primaryMatrices;

    /**
     * 二级指标间矩阵（按层级+一级维度分组）
     * key: levelName
     * inner key: primaryCode
     */
    private Map<String, Map<String, CollectiveMatrix>> secondaryMatrices;

    // ==================== 综合权重 ====================

    /**
     * 层级权重
     */
    private Map<String, BigDecimal> levelWeights;

    /**
     * 综合权重列表（所有二级指标）
     */
    private List<CombinedWeightItem> combinedWeights;

    /**
     * 旭日图数据
     */
    private SunburstData sunburstData;

    // ==================== 集结过程详情 ====================

    /**
     * 集结过程详情（用于展示每个比较项的详细计算）
     * key: comparisonKey
     * value: 各专家的集结详情
     */
    private Map<String, List<AggregationDetail>> aggregationDetails;

    // ==================== DTO内部类 ====================

    /**
     * 专家权重明细
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpertWeightDetail {
        private Long expertId;
        private String expertName;
        private BigDecimal credibility;          // 可信度 0~100
        private Integer participationCount;     // 参与的比较项数
        private BigDecimal avgNormalizedWeight; // 平均归一化权重
    }

    /**
     * 一致性检验结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrResult {
        private BigDecimal cr;
        private BigDecimal ci;
        private BigDecimal lambdaMax;
        private Boolean consistent;
    }

    /**
     * 集体判断矩阵
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollectiveMatrix {
        /**
         * 矩阵维度名称列表（表头/行名）
         */
        private List<String> headers;

        /**
         * 矩阵数据（二维数组，按headers顺序）
         */
        private List<List<BigDecimal>> matrix;

        /**
         * 权重向量
         */
        private List<BigDecimal> weights;

        /**
         * CR值
         */
        private BigDecimal cr;

        /**
         * CI值
         */
        private BigDecimal ci;

        /**
         * lambdaMax值
         */
        private BigDecimal lambdaMax;
    }

    /**
     * 综合权重项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CombinedWeightItem {
        private String levelName;           // 层级名称
        private String primaryCode;        // 一级维度编码
        private String primaryName;        // 一级维度名称
        private String secondaryCode;      // 二级指标编码
        private String secondaryName;      // 二级指标名称
        private BigDecimal levelWeight;    // 层级权重
        private BigDecimal primaryWeight;   // 维度内权重
        private BigDecimal secondaryWeight; // 指标权重
        private BigDecimal combinedWeight; // 综合权重
    }

    /**
     * 旭日图数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SunburstData {
        private String name;
        private List<SunburstLevel> children;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SunburstLevel {
        private String name;
        private BigDecimal value;
        private List<SunburstLevel> children;
    }

    /**
     * 集结过程详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AggregationDetail {
        private Long expertId;
        private String expertName;
        private BigDecimal credibility;         // 可信度 0~1
        private BigDecimal confidence;         // 把握度 0~1
        private BigDecimal rawWeight;         // RawWeight = Cred×0.5 + Conf×0.5
        private BigDecimal normalizedWeight;   // 归一化权重
        private BigDecimal score;             // 原始标度
        private BigDecimal weightedScore;     // 加权得分
    }
}
