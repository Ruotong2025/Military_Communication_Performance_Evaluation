package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 专家集结计算响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectiveCalculateResponse {

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
    private List<ExpertWeightDetail> expertWeights;

    /**
     * 集结后的完整层次（域间一级 + 效能体系 + 装备体系 + 叶子全局权重），
     * 与个体快照 {@link com.ccnu.military.dto.AhpIndividualResult} 结构一致，供前端一级/二级/三级与旭日图展示。
     */
    private com.ccnu.military.dto.AhpIndividualResult aggregatedUnified;

    // ==================== 集体判断矩阵 ====================
    /** 集体比较打分（用于展示矩阵） */
    private Map<String, Double> collectiveScores;

    /**
     * 每个 comparison_key 下各专家的归一化权重
     * key: comparison_key（如 "安全性_可靠性"）
     * value: Map<expertId, weight>，在 key 内归一化后，各 expertId 权重之和 = 1
     */
    private Map<String, Map<String, BigDecimal>> perKeyExpertWeights;

    // ==================== 评估结果 ====================
    /** 各作战任务的评估结果 */
    private List<OperationResult> results;

    // ==================== 嵌套类 ====================

    /**
     * 专家权重明细
     * 注意：归一化权重为该专家在所有 comparison_key 上权重均值，
     *       实际集结时每对的权重由该对的 (可信度, 把握度) 共同决定。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpertWeightDetail {
        /** 专家ID */
        private Long expertId;
        /** 专家姓名 */
        private String expertName;
        /** 可信度得分（0~100） */
        private BigDecimal credibility;
        /** 在所有 comparison_key 上归一化权重均值（仅供参考） */
        private BigDecimal normalizedWeight;
    }

    /**
     * 作战任务评估结果
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationResult {
        /** 作战任务ID */
        private String operationId;
        /** 综合得分 */
        private BigDecimal totalScore;
        /** 5个维度加权得分 */
        private Map<String, BigDecimal> dimensionScores;
        /** 18个指标加权得分 */
        private Map<String, BigDecimal> indicatorScores;
    }
}
