package com.ccnu.military.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 防御操作记录实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "records_comm_defense_operation")
public class RecordsCommDefenseOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "operation_id", nullable = false)
    private Integer operationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private OperationType operationType;

    @Column(name = "start_time_ms", nullable = false)
    private Long startTimeMs;

    @Column(name = "end_time_ms")
    private Long endTimeMs;

    @Column(name = "operator_id", length = 50)
    private String operatorId;

    @Column(name = "detection_time_ms")
    private Long detectionTimeMs;

    @Column(name = "detection_method", length = 100)
    private String detectionMethod;

    @Column(name = "jamming_detected")
    private Integer jammingDetected;

    @Column(name = "anti_jamming_actions", length = 200)
    private String antiJammingActions;

    @Column(name = "anti_jamming_success")
    private Integer antiJammingSuccess;

    @Column(name = "suspicious_signal_detected")
    private Integer suspiciousSignalDetected;

    @Column(name = "verification_method", length = 100)
    private String verificationMethod;

    @Column(name = "verification_result", length = 100)
    private String verificationResult;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum OperationType {
        detection_awareness, anti_jamming, anti_deception
    }
}
