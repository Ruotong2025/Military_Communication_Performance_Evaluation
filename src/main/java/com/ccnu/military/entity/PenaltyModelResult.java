package com.ccnu.military.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 惩罚模型计算结果实体
 * <p>存储每个作战任务经过惩罚计算后的最终得分，
 * 不依赖具体指标名称，以 indicator_key 通用标识各指标。
 * <p>注意: 惩罚模型参数在前端代码中默认写死，不存储到数据库。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "penalty_model_result", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"evaluation_id", "operation_id"})
})
public class PenaltyModelResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 评估批次ID */
    @Column(name = "evaluation_id", nullable = false, length = 50)
    private String evaluationId;

    /** 作战任务ID */
    @Column(name = "operation_id", nullable = false, length = 50)
    private String operationId;

    /** 集结综合得分（原始，Sstage） */
    @Column(name = "original_score", nullable = false, precision = 10, scale = 6)
    private BigDecimal originalScore;

    /** 综合惩罚因子 P = min(F1..Fn) */
    @Column(name = "overall_penalty", nullable = false, precision = 8, scale = 6)
    private BigDecimal overallPenalty;

    /** 最终效能得分 Sfinal = Sstage × P */
    @Column(name = "final_score", nullable = false, precision = 10, scale = 6)
    private BigDecimal finalScore;

    /** 惩罚幅度 (Sstage - Sfinal) / Sstage */
    @Column(name = "penalty_amplitude", nullable = false, precision = 8, scale = 6)
    private BigDecimal penaltyAmplitude;

    /**
     * 各指标惩罚因子详情（JSON 数组）
     * <p>示例：
     * <pre>
     * [
     *   { "indicator_key": "reliability_crash_rate_qt",
     *     "indicator_score": 45.2,   -- 该作战该指标得分（百分制）
     *     "threshold": 70,
     *     "m": 0.8,
     *     "fi": 0.5171 },
     *   { "indicator_key": "effect_mission_completion_rate_qt",
     *     "indicator_score": 72.0,
     *     "threshold": 70,
     *     "m": 0.8,
     *     "fi": 1.0000 }
     * ]
     * </pre>
     */
    @Column(name = "penalty_details", nullable = false, columnDefinition = "json")
    private String penaltyDetails;

    /**
     * 各指标批次算术平均分（便于前端配置区回显，不参与计算）
     * JSON: { "reliability_crash_rate_qt": 0.65, "effect_mission_completion_rate_qt": 0.72 }
     */
    @Column(name = "batch_avg_score_map", columnDefinition = "json")
    private String batchAvgScoreMap;

    /**
     * 各指标批次最低分（便于前端配置区回显，不参与计算）
     * JSON: { "reliability_crash_rate_qt": 0.45, "effect_mission_completion_rate_qt": 0.68 }
     */
    @Column(name = "batch_min_score_map", columnDefinition = "json")
    private String batchMinScoreMap;

    /**
     * 各指标 Fi（均值口径，便于前端配置区回显）
     * JSON: { "reliability_crash_rate_qt": 0.7429, "effect_mission_completion_rate_qt": 1.0000 }
     */
    @Column(name = "batch_avg_fi_map", columnDefinition = "json")
    private String batchAvgFiMap;

    /**
     * 各指标 Fi（批次最低分口径，便于前端配置区回显）
     * JSON: { "reliability_crash_rate_qt": 0.5143, "effect_mission_completion_rate_qt": 1.0000 }
     */
    @Column(name = "batch_min_fi_map", columnDefinition = "json")
    private String batchMinFiMap;

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
