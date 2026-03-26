package com.ccnu.military.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 全局权重调整请求：十项权重将应用到所有已评估记录并重算综合分与等级
 */
@Data
public class GlobalWeightsRequest {

    private BigDecimal weightTitle;
    private BigDecimal weightPosition;
    private BigDecimal weightEducation;
    private BigDecimal weightAcademic;
    private BigDecimal weightResearch;
    private BigDecimal weightExercise;
    private BigDecimal weightMilitaryTraining;
    private BigDecimal weightSystemSimulation;
    private BigDecimal weightStatistics;
    private BigDecimal weightProfessionalYears;
}
