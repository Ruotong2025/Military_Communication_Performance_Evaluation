package com.ccnu.military.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class DynamicCostSimulationResult {
    
    private Long batchId;
    
    private String batchCode;
    
    private BigDecimal costScore;
    
    private BigDecimal effectivenessCostRatio;
    
    private Map<String, DistributionStats> distribution;
    
    @Data
    public static class DistributionStats {
        private Double min;
        private Double max;
        private Double mean;
        private Double std;
        private Double percentile25;
        private Double percentile50;
        private Double percentile75;
        private Double percentile90;
        private Double percentile95;
    }
}
