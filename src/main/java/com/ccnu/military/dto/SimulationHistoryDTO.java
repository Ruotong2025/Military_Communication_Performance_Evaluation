package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模拟历史DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationHistoryDTO {

    private Long id;

    private Long configId;

    private String simulationBatch;

    private Integer simulationTimes;

    private String simulationType;

    private Double effectivenessScore;

    private Double mean;

    private Double stdDev;

    private Double min;

    private Double max;

    private String createdTime;
}
