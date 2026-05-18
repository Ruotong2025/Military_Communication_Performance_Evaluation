package com.ccnu.military.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class DynamicCostBatchDetailVO {
    
    private Long batchId;
    
    private String batchCode;
    
    private String batchName;
    
    private Long templateId;
    
    private String templateName;
    
    private Integer simulationCount;
    
    private Long randomSeed;
    
    private BigDecimal effectivenessScore;
    
    private BigDecimal costScore;
    
    private BigDecimal effectivenessCostRatio;
    
    private Map<String, DynamicCostSimulationResult.DistributionStats> distribution;
    
    private LocalDateTime createdTime;
    
    private List<SimulationDetail> detailList;
    
    @Data
    public static class SimulationDetail {
        private Integer iterationNumber;
        private String indicatorName;
        private String indicatorCode;
        private String level1Name;
        private String level2Name;
        private BigDecimal simulatedValue;
        private BigDecimal normalizedValue;
    }
}
