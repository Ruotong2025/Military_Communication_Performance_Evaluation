package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 成本评估计算结果 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostEffectivenessResultDTO {

    /** 主键ID */
    private Long id;

    /** 评估批次ID */
    private String evaluationId;

    /** 作战任务ID */
    private String operationId;

    /** 综合成本指数 C（0~1） */
    private BigDecimal costIndex;

    /** 原始成本指数 */
    private BigDecimal costIndexRaw;

    /** 效能得分 E */
    private BigDecimal effectivenessScore;

    /** 效费比 R = E/C */
    private BigDecimal costEffectivenessRatio;

    /** 效费等级 */
    private String efficiencyGrade;

    /** 各类成本汇总 */
    private Map<String, BigDecimal> costByCategory;

    /** 原始指标数据 */
    private Map<String, BigDecimal> rawIndicators;

    /** 归一化后指标数据 */
    private Map<String, BigDecimal> normalizedIndicators;

    /** 权重快照 */
    private Map<String, BigDecimal> weightsSnapshot;

    /** 归一化边界（min/max per indicator） */
    private Map<String, Map<String, BigDecimal>> normalizationBounds;

    /** 方向调整后的成本分量（归一化后，效益型已反向，0~1） */
    private Map<String, BigDecimal> effectiveNormalized;

    /** 创建时间 */
    private String createdAt;

    /** 更新时间 */
    private String updatedAt;
}
