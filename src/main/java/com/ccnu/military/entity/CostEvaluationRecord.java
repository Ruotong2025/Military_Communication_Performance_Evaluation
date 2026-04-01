package com.ccnu.military.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成本评估记录实体
 * <p>存储每次评估的成本数据。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cost_evaluation_record", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"evaluation_id", "operation_id"})
})
public class CostEvaluationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 评估批次ID */
    @Column(name = "evaluation_id", nullable = false, length = 50)
    private String evaluationId;

    /** 作战任务ID */
    @Column(name = "operation_id", nullable = false, length = 50)
    private String operationId;

    /** 综合成本指数 C（归一化加权求和后，0~1） */
    @Column(name = "cost_index", precision = 8, scale = 6)
    private BigDecimal costIndex;

    /** 原始成本指数（加权求和前，未归一化） */
    @Column(name = "cost_index_raw", precision = 15, scale = 4)
    private BigDecimal costIndexRaw;

    /** 效能得分 E（来自惩罚模型最终得分） */
    @Column(name = "effectiveness_score", precision = 10, scale = 6)
    private BigDecimal effectivenessScore;

    /** 效费比 R = E/C */
    @Column(name = "cost_effectiveness_ratio", precision = 12, scale = 6)
    private BigDecimal costEffectivenessRatio;

    /** 各类成本汇总 JSON */
    @Column(name = "cost_by_category", columnDefinition = "json")
    private String costByCategory;

    /** 原始指标数据 JSON */
    @Column(name = "raw_indicators", columnDefinition = "json")
    private String rawIndicators;

    /** 归一化后指标数据 JSON */
    @Column(name = "normalized_indicators", columnDefinition = "json")
    private String normalizedIndicators;

    /** 权重快照 JSON */
    @Column(name = "weights_snapshot", columnDefinition = "json")
    private String weightsSnapshot;

    /** 归一化边界快照 JSON */
    @Column(name = "normalization_bounds", columnDefinition = "json")
    private String normalizationBounds;

    /** 备注 */
    @Column(name = "remarks", columnDefinition = "text")
    private String remarks;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
