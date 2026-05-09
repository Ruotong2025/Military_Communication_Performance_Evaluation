package com.ccnu.military.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DynamicIndicatorTreeDTO {
    private Long id;
    private String templateName;
    private String templateCode;
    private String status;
    private Statistics statistics;
    private List<LevelNode> levels;

    @Data
    public static class LevelNode {
        private Long id;
        private String name;
        private String description;
        private Integer sortOrder;
        private List<PrimaryNode> primaryDimensions;
    }

    @Data
    public static class PrimaryNode {
        private Long id;
        private String name;
        private String code;
        private Integer sortOrder;
        private Double weight;
        private List<SecondaryNode> secondaryDimensions;
    }

    @Data
    public static class SecondaryNode {
        private Long id;
        private String name;
        private String code;
        private Integer sortOrder;
        private String metricType;        // 指标性质：QUANTITATIVE/QUALITATIVE
        private String aggregationMethod;  // 聚合方式
        private String scoreDirection;    // 得分方向：POSITIVE/NEGATIVE
        private String unit;               // 单位
        private Double weight;
        private Double baselineValue;      // 基线值
        private Double targetValue;        // 目标值
        private Double averageValue;       // 平均数（用于归一化参考）
    }

    @Data
    public static class Statistics {
        private Integer levelCount;
        private Integer totalPrimaryDimensions;
        private Integer totalSecondaryDimensions;
        private Integer quantitativeCount;  // 定量指标数量
        private Integer qualitativeCount;   // 定性指标数量
        private List<String> levelNames;
        private Map<String, Integer> primaryDimensionCountPerLevel;
    }
}
