package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 专家集结权重预览DTO（仅返回权重，不含评估得分）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectiveWeightPreview {

    /** 评估批次ID */
    private String evaluationId;

    /** 参与专家数量 */
    private Integer expertCount;

    /** 参与专家ID列表 */
    private List<Long> expertIds;

    // ==================== 一致性（6个CR）====================
    /** 各矩阵CR值 */
    private Map<String, BigDecimal> crResults;

    // ==================== 集结权重 ====================
    /** 5个维度集结权重 */
    private Map<String, BigDecimal> dimensionWeights;

    /** 18个二级指标综合权重（对总目标） */
    private Map<String, BigDecimal> indicatorWeights;

    /** 5个维度的指标层权重（用于计算维度得分） */
    private Map<String, Map<String, BigDecimal>> indicatorWeightsByDimension;

    /** 专家权重明细 */
    private List<CollectiveCalculateResponse.ExpertWeightDetail> expertWeights;

    /** 与 {@link CollectiveCalculateResponse#getAggregatedUnified()} 相同 */
    private com.ccnu.military.dto.AhpIndividualResult aggregatedUnified;

    // ==================== 集体判断矩阵 ====================
    /**
     * 集体比较打分
     * key: "A_B"格式的比较对
     * value: 加权平均后的集体打分
     */
    private Map<String, Double> collectiveScores;

    /**
     * 每个 comparison_key 下各专家的归一化权重
     * key: comparison_key（如 "安全性_可靠性"）
     * value: Map<expertId, weight>，在 key 内归一化后，各 expertId 权重之和 = 1
     */
    private Map<String, Map<String, BigDecimal>> perKeyExpertWeights;
}
