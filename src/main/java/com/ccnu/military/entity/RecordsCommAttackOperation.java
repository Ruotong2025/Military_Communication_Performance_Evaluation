package com.ccnu.military.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 进攻操作记录实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "records_comm_attack_operation")
public class RecordsCommAttackOperation {

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

    @Column(name = "target_node", length = 50)
    private String targetNode;

    @Column(name = "target_communication_id")
    private Long targetCommunicationId;

    @Column(name = "jamming_power_dbm", precision = 8, scale = 2)
    private BigDecimal jammingPowerDbm;

    @Column(name = "jamming_frequency_hz", precision = 15, scale = 2)
    private BigDecimal jammingFrequencyHz;

    @Column(name = "effect_assessment", length = 200)
    private String effectAssessment;

    @Column(name = "spoofing_signal_type", length = 50)
    private String spoofingSignalType;

    @Column(name = "spoofing_success")
    private Integer spoofingSuccess;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum OperationType {
        jamming_target_lock, jamming_effect, spoofing_signal
    }
}
