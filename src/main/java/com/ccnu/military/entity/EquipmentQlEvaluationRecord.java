package com.ccnu.military.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 定性评估记录实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "equipment_ql_evaluation_record")
public class EquipmentQlEvaluationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "operation_id", nullable = false, length = 50)
    private String operationId;

    @Column(name = "evaluation_time", nullable = false)
    private LocalDateTime evaluationTime;

    @Column(name = "evaluation_batch_id", nullable = false, length = 50)
    private String evaluationBatchId;

    @Column(name = "expert_id", nullable = false)
    private Long expertId;

    @Column(name = "expert_name", nullable = false, length = 100)
    private String expertName;

    @Column(name = "scores", nullable = false, columnDefinition = "JSON")
    private String scores;

    @Column(name = "aggregated_grade", length = 10)
    private String aggregatedGrade;

    @Column(name = "aggregated_score", precision = 10, scale = 4)
    private BigDecimal aggregatedScore;

    @Column(name = "overall_confidence", precision = 5, scale = 2)
    private BigDecimal overallConfidence;

    @Column(name = "overall_comment", columnDefinition = "TEXT")
    private String overallComment;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

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
