package com.ccnu.military.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AHP通信效能综合评分结果实体
 * 存储每次综合评分计算的结果，包括效能分、装备分、总分等
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ahp_communication_scoring_result")
@EntityListeners(AuditingEntityListener.class)
public class AhpCommunicationScoringResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "evaluation_batch_id", nullable = false, length = 50)
    private String evaluationBatchId;

    @Column(name = "operation_id", nullable = false, length = 50)
    private String operationId;

    // 域间权重快照
    @Column(name = "eff_domain_weight", precision = 10, scale = 6)
    private BigDecimal effDomainWeight;

    @Column(name = "eq_domain_weight", precision = 10, scale = 6)
    private BigDecimal eqDomainWeight;

    // 效能综合分（0~1）
    @Column(name = "effectiveness_score", precision = 10, scale = 6)
    private BigDecimal effectivenessScore;

    // 装备综合分（0~1）
    @Column(name = "equipment_score", precision = 10, scale = 6)
    private BigDecimal equipmentScore;

    // 综合总分
    @Column(name = "total_score", precision = 10, scale = 6)
    private BigDecimal totalScore;

    // 各维度得分明细 JSON
    @Column(name = "dimension_scores_json", columnDefinition = "LONGTEXT")
    private String dimensionScoresJson;

    // 指标得分明细 JSON
    @Column(name = "indicator_scores_json", columnDefinition = "LONGTEXT")
    private String indicatorScoresJson;

    // 参与专家信息 JSON
    @Column(name = "expert_info_json", columnDefinition = "LONGTEXT")
    private String expertInfoJson;

    // 权重快照 JSON
    @Column(name = "weight_snapshot_json", columnDefinition = "LONGTEXT")
    private String weightSnapshotJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}