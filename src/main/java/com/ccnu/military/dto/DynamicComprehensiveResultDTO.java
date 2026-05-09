package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 动态综合评分结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicComprehensiveResultDTO {

    /**
     * 批次ID
     */
    private String batchId;

    /**
     * 作战ID
     */
    private String operationId;

    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 综合得分
     */
    private Double totalScore;

    /**
     * 定性加权分
     */
    private Double qualitativeWeightedScore;

    /**
     * 定量加权分
     */
    private Double quantitativeWeightedScore;

    /**
     * 计算时间
     */
    private String calculatedAt;

    /**
     * 层级得分列表
     */
    private List<LevelScore> levelScores;

    /**
     * 综合权重列表（用于前端展示权重分布）
     */
    private List<DynamicAhpCombinedWeightDTO> combinedWeights;

    /**
     * 层级得分
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelScore {
        /**
         * 层级名称，如"战术级"
         */
        private String levelName;
        /**
         * 层级权重，如0.6
         */
        private Double weight;
        /**
         * 定性加权分
         */
        private Double qualitativeScore;
        /**
         * 定量加权分
         */
        private Double quantitativeScore;
        /**
         * 综合得分
         */
        private Double comprehensiveScore;
        /**
         * 一级维度得分列表
         */
        private List<PrimaryDimensionScore> primaryDimensions;
    }

    /**
     * 一级维度得分
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrimaryDimensionScore {
        /**
         * 一级维度编码
         */
        private String dimensionCode;
        /**
         * 一级维度名称，如"通信质量"
         */
        private String dimensionName;
        /**
         * 一级维度权重（在一级维度组内的权重）
         */
        private Double weight;
        /**
         * 定性加权分
         */
        private Double qualitativeScore;
        /**
         * 定量加权分
         */
        private Double quantitativeScore;
        /**
         * 综合得分
         */
        private Double comprehensiveScore;
    }
}
