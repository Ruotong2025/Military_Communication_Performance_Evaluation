package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 蒙特卡洛模拟请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonteCarloRequestDTO {

    /**
     * 配置ID
     */
    private Long configId;

    /**
     * 效能得分（如果为空则从配置的批次获取）
     */
    private Double effectivenessScore;

    /**
     * 模拟次数
     */
    private Integer simulationTimes;

    /**
     * 分布类型：uniform-均匀分布，normal-正态分布
     */
    private String distributionType;

    /**
     * 成本指标列表（如果为空则从配置获取）
     */
    private List<CostIndicatorDTO> costIndicators;
}
