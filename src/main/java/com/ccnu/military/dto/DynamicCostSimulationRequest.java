package com.ccnu.military.dto;

import lombok.Data;

@Data
public class DynamicCostSimulationRequest {
    
    private Long templateId;
    
    private String batchName;
    
    private Integer simulationCount = 1000;
    
    private Long randomSeed;
}
