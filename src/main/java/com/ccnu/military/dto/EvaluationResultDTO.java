package com.ccnu.military.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 评估结果DTO
 */
@Data
public class EvaluationResultDTO {
    private String testId;
    private Integer scenarioId;
    
    // 维度得分
    private BigDecimal responseScore;      // 响应能力
    private BigDecimal processingScore;    // 处理能力
    private BigDecimal effectivenessScore; // 有效性
    private BigDecimal reliabilityScore;   // 可靠性
    private BigDecimal antiJammingScore;   // 抗干扰性
    private BigDecimal operationScore;     // 人为操作
    private BigDecimal networkingScore;    // 组网能力
    private BigDecimal securityScore;      // 安全性
    
    // 综合得分
    private BigDecimal totalScore;
    private String grade;  // 优秀/良好/中等/及格/较差
    private Integer rank;
    
    // 关键指标
    private BigDecimal taskSuccessRate;
    private BigDecimal communicationAvailabilityRate;
    private Integer totalNetworkCrashes;
    private Integer totalCommunications;
}
