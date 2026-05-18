package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 效能得分DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EffectivenessScoreDTO {

    private Long templateId;

    private String templateName;

    private String batchId;

    private Integer operationCount;

    /**
     * 综合效能得分
     */
    private Double totalScore;

    /**
     * 定性加权分
     */
    private Double qualitativeScore;

    /**
     * 定量加权分
     */
    private Double quantitativeScore;

    /**
     * 层级得分列表
     */
    private List<LevelScoreItem> levelScores;

    /**
     * 一级维度得分列表（用于雷达图）
     */
    private List<PrimaryDimensionItem> primaryDimensions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelScoreItem {
        private String levelName;
        private Double weight;
        private Double comprehensiveScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrimaryDimensionItem {
        private String dimensionCode;
        private String dimensionName;
        private Double score;
    }
}
