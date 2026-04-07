package com.ccnu.military.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 定量评估记录实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "equipment_qt_evaluation_record")
public class EquipmentQtEvaluationRecord {

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

    @Column(name = "raw_data", nullable = false, columnDefinition = "JSON")
    private String rawData;

    @Column(name = "normalized_scores", nullable = false, columnDefinition = "JSON")
    private String normalizedScores;

    @Column(name = "composite_score", precision = 10, scale = 4)
    private BigDecimal compositeScore;

    @Column(name = "dimension_scores", columnDefinition = "JSON")
    private String dimensionScores;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

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
