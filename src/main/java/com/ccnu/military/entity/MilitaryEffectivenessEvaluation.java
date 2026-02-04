package com.ccnu.military.entity;

import javax.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 军事效能评估实体类
 * 对应表: military_effectiveness_evaluation
 */
@Data
@Entity
@Table(name = "military_effectiveness_evaluation")
public class MilitaryEffectivenessEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evaluation_id")
    private Long evaluationId;

    @Column(name = "scenario_id", nullable = false)
    private Integer scenarioId;

    @Column(name = "test_id", nullable = false, unique = true, length = 50)
    private String testId;

    // ========== 1. 响应能力指标 (RS - 2个) ==========
    @Column(name = "RS_avg_call_setup_duration_ms", precision = 12, scale = 3)
    private BigDecimal avgCallSetupDurationMs;

    @Column(name = "RS_avg_transmission_delay_ms", precision = 12, scale = 3)
    private BigDecimal avgTransmissionDelayMs;

    // ========== 2. 处理能力指标 (PO - 2个) ==========
    @Column(name = "PO_effective_throughput", precision = 15, scale = 2)
    private BigDecimal effectiveThroughput;

    @Column(name = "PO_spectral_efficiency", precision = 8, scale = 4)
    private BigDecimal spectralEfficiency;

    // ========== 3. 有效性指标 (EF - 4个) ==========
    @Column(name = "EF_avg_communication_distance", precision = 10, scale = 2)
    private BigDecimal avgCommunicationDistance;

    @Column(name = "EF_avg_ber", precision = 15, scale = 10)
    private BigDecimal avgBer;

    @Column(name = "EF_avg_plr", precision = 8, scale = 6)
    private BigDecimal avgPlr;

    @Column(name = "EF_task_success_rate", precision = 8, scale = 6)
    private BigDecimal taskSuccessRate;

    // ========== 4. 可靠性指标 (RL - 4个) ==========
    @Column(name = "RL_communication_availability_rate", precision = 8, scale = 6)
    private BigDecimal communicationAvailabilityRate;

    @Column(name = "RL_communication_success_rate", precision = 8, scale = 6)
    private BigDecimal communicationSuccessRate;

    @Column(name = "RL_recovery_duration_ms", precision = 12, scale = 3)
    private BigDecimal recoveryDurationMs;

    @Column(name = "RL_crash_rate", precision = 8, scale = 6)
    private BigDecimal crashRate;

    // ========== 5. 抗干扰性指标 (AJ - 2个) ==========
    @Column(name = "AJ_avg_sinr", precision = 8, scale = 2)
    private BigDecimal avgSinr;

    @Column(name = "AJ_avg_jamming_margin", precision = 8, scale = 2)
    private BigDecimal avgJammingMargin;

    // ========== 6. 人为操作能力指标 (HO - 2个) ==========
    @Column(name = "HO_avg_operator_reaction_time_ms", precision = 10, scale = 2)
    private BigDecimal avgOperatorReactionTimeMs;

    @Column(name = "HO_operation_success_rate", precision = 8, scale = 6)
    private BigDecimal operationSuccessRate;

    // ========== 7. 组网能力指标 (NC - 2个) ==========
    @Column(name = "NC_avg_network_setup_duration_ms", precision = 12, scale = 3)
    private BigDecimal avgNetworkSetupDurationMs;

    @Column(name = "NC_avg_connectivity_rate", precision = 8, scale = 6)
    private BigDecimal avgConnectivityRate;

    // ========== 8. 安全性指标 (SC - 3个) ==========
    @Column(name = "SC_key_compromise_frequency", precision = 10, scale = 4)
    private BigDecimal keyCompromiseFrequency;

    @Column(name = "SC_detection_probability", precision = 5, scale = 4)
    private BigDecimal detectionProbability;

    @Column(name = "SC_interception_resistance", precision = 5, scale = 2)
    private BigDecimal interceptionResistance;

    // ========== 统计信息 ==========
    @Column(name = "total_communications")
    private Integer totalCommunications;

    @Column(name = "total_lifecycles")
    private Integer totalLifecycles;

    @Column(name = "total_communication_duration_ms")
    private Long totalCommunicationDurationMs;

    @Column(name = "total_interruption_time_ms")
    private Long totalInterruptionTimeMs;

    // ========== 元数据 ==========
    @Column(name = "created_at", updatable = false)
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
