package com.ccnu.military.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class IndicatorTreeDTO {
    private List<LevelNode> levels;
    private Statistics statistics;

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
        private String metricType;
        private String aggregationMethod;
        private String scoreDirection;
        private String unit;
        private Double weight;
    }

    @Data
    public static class Statistics {
        private Integer levelCount;
        private Integer totalPrimaryDimensions;
        private Integer totalSecondaryDimensions;
        private List<String> levelNames;
        private Map<String, Integer> primaryDimensionCountPerLevel;
    }
}
