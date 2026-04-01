package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 成本评估计算响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostEffectivenessCalculateResponse {

    /** 评估批次ID */
    private String evaluationId;

    /** 计算成功的作战任务数量 */
    private Integer successCount;

    /** 计算失败的数量 */
    private Integer failCount;

    /** 各作战任务的计算结果 */
    private List<CostEffectivenessResultDTO> results;

    /** 全局归一化边界 */
    private NormalizationBounds normalizationBounds;

    /** 使用的权重配置 */
    private Map<String, BigDecimal> weightsConfig;

    /** 数据统计 */
    private Statistics statistics;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NormalizationBounds {
        /** 各指标的最小值 { indicator_key: min_value } */
        private Map<String, BigDecimal> minValues;

        /** 各指标的最大值 { indicator_key: max_value } */
        private Map<String, BigDecimal> maxValues;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Statistics {
        /** 成本指数统计 */
        private BigDecimal avgCostIndex;
        private BigDecimal minCostIndex;
        private BigDecimal maxCostIndex;

        /** 效费比统计 */
        private BigDecimal avgCostEffectivenessRatio;
        private BigDecimal minCostEffectivenessRatio;
        private BigDecimal maxCostEffectivenessRatio;

        /** 效能得分统计 */
        private BigDecimal avgEffectivenessScore;
        private BigDecimal minEffectivenessScore;
        private BigDecimal maxEffectivenessScore;
    }
}
