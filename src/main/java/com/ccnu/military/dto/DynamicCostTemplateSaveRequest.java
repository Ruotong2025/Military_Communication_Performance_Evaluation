package com.ccnu.military.dto;

import lombok.Data;
import java.util.List;

@Data
public class DynamicCostTemplateSaveRequest {
    
    private Long id;
    
    private String templateName;
    
    private List<IndicatorConfig> costIndicators;
    
    @Data
    public static class IndicatorConfig {
        private String level1Name;
        private String level2Name;
        private String indicatorCode;
        private Double weight;
        private Double minValue;
        private Double maxValue;
        private String unit;
        private String distributionType;
    }
}
