package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 原始数据集结DTO
 * 用于展示定性数据和定量数据的集结结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawDataAggregationDTO {

    /**
     * 批次ID
     */
    private String batchId;

    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 作战ID列表
     */
    private List<String> operationIds;

    /**
     * 一级维度列表
     */
    private List<PrimaryDimensionData> primaryDimensions;

    /**
     * 原始数据表格
     */
    private List<OperationRawData> operationData;

    /**
     * 一级维度数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrimaryDimensionData {
        private String code;
        private String name;
        private String levelName;
        private List<IndicatorData> indicators;
    }

    /**
     * 指标数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndicatorData {
        private String code;
        private String name;
        private String metricType; // QUANTITATIVE or QUALITATIVE
        private Double weight;
    }

    /**
     * 作战原始数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationRawData {
        private String operationId;
        private List<IndicatorScore> scores;
        private Double comprehensiveScore;
    }

    /**
     * 指标得分
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndicatorScore {
        private String indicatorCode;
        private String indicatorName;
        private Double qualitativeScore; // 定性得分（0-100）
        private Double quantitativeScore; // 定量得分（0-100 归一化后）
        private Double combinedScore; // 综合得分
    }
}
