package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 蒙特卡洛模拟结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonteCarloResultDTO {

    /**
     * 模拟批次号
     */
    private String simulationBatch;

    /**
     * 模拟次数
     */
    private Integer simulationTimes;

    /**
     * 效能得分
     */
    private Double effectivenessScore;

    /**
     * 所有模拟记录
     */
    private List<SimulationRecord> records;

    /**
     * 统计数据
     */
    private Statistics statistics;

    /**
     * 分布直方图数据
     */
    private List<HistogramBin> histogram;

    /**
     * 累积分布函数数据
     */
    private List<CdfPoint> cdf;

    /**
     * 敏感性分析结果
     */
    private List<SensitivityResult> sensitivityAnalysis;

    /**
     * 单条模拟记录
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimulationRecord {
        private Integer iteration;
        private Map<String, Double> costValues;
        private Double costScore;
        private Double costEffectivenessRatio;
    }

    /**
     * 统计数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Statistics {
        private Double mean;
        private Double stdDev;
        private Double min;
        private Double max;
        private Double median;
        private Double[] confidenceInterval95;
        private Double variance;
    }

    /**
     * 直方图区间
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistogramBin {
        private Double start;
        private Double end;
        private Integer count;
        private Double frequency;
    }

    /**
     * 累积分布点
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CdfPoint {
        private Double value;
        private Double probability;
    }

    /**
     * 敏感性分析结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SensitivityResult {
        private String indicatorCode;
        private String indicatorName;
        private Double correlationCoefficient;
        private Double elasticity;
        private String impactDirection;
    }
}
