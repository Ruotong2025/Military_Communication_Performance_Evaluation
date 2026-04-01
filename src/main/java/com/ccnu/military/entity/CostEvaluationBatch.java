package com.ccnu.military.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成本评估批次配置实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cost_evaluation_batch")
public class CostEvaluationBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 评估批次ID */
    @Column(name = "evaluation_id", nullable = false, length = 50, unique = true)
    private String evaluationId;

    /** 批次名称 */
    @Column(name = "batch_name", length = 200)
    private String batchName;

    /** 评估时间 */
    @Column(name = "evaluation_time", nullable = false)
    private LocalDateTime evaluationTime;

    /** 权重确定方法 */
    @Column(name = "weight_method", length = 20)
    @Enumerated(EnumType.STRING)
    private WeightMethod weightMethod = WeightMethod.equal;

    /** 归一化方法 */
    @Column(name = "normalization_method", length = 20)
    @Enumerated(EnumType.STRING)
    private NormalizationMethod normalizationMethod = NormalizationMethod.minmax;

    /** 启用的指标列表 JSON */
    @Column(name = "active_indicators", columnDefinition = "json")
    private String activeIndicators;

    /** 全局最小值快照 JSON */
    @Column(name = "global_min_values", columnDefinition = "json")
    private String globalMinValues;

    /** 全局最大值快照 JSON */
    @Column(name = "global_max_values", columnDefinition = "json")
    private String globalMaxValues;

    /** 批次状态 */
    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private BatchStatus status = BatchStatus.pending;

    /** 作战任务数量 */
    @Column(name = "operation_count")
    private Integer operationCount = 0;

    /** 已完成数量 */
    @Column(name = "completed_count")
    private Integer completedCount = 0;

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

    public enum WeightMethod {
        equal,  // 等权
        ahp,    // AHP
        manual  // 手动
    }

    public enum NormalizationMethod {
        minmax,  // 极差归一化
        zscore,  // Z-Score
        manual   // 手动
    }

    public enum BatchStatus {
        pending,    // 待处理
        computing,  // 计算中
        completed,  // 已完成
        failed      // 失败
    }
}
