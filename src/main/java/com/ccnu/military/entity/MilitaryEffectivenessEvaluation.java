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

    // ========== 1. 响应能力指标 (2个) ==========
    @Column(name = "avg_call_setup_duration_ms", precision = 12, scale = 3)
    private BigDecimal avgCallSetupDurationMs;

    @Column(name = "avg_transmission_delay_ms", precision = 12, scale = 3)
    private BigDecimal avgTransmissionDelayMs;

    // ========== 2. 处理能力指标 (4个) ==========
    @Column(name = "effective_throughput", precision = 15, scale = 2)
    private BigDecimal effectiveThroughput;

    @Column(name = "spectral_efficiency", precision = 8, scale = 4)
    private BigDecimal spectralEfficiency;

    @Column(name = "channel_utilization", precision = 5, scale = 2)
    private BigDecimal channelUtilization;

    @Column(name = "avg_concurrent_links", precision = 8, scale = 2)
    private BigDecimal avgConcurrentLinks;

    // ========== 3. 有效性指标 (3个) ==========
    @Column(name = "avg_communication_distance", precision = 10, scale = 2)
    private BigDecimal avgCommunicationDistance;

    @Column(name = "avg_ber", precision = 15, scale = 10)
    private BigDecimal avgBer;

    @Column(name = "avg_plr", precision = 8, scale = 6)
    private BigDecimal avgPlr;

    // ========== 4. 可靠性指标 (5个) ==========
    @Column(name = "task_success_rate", precision = 8, scale = 6)
    private BigDecimal taskSuccessRate;

    @Column(name = "communication_availability_rate", precision = 8, scale = 6)
    private BigDecimal communicationAvailabilityRate;

    @Column(name = "total_network_crashes")
    private Integer totalNetworkCrashes;

    @Column(name = "avg_response_time_ms")
    private Long avgResponseTimeMs;

    @Column(name = "avg_handling_duration_ms")
    private Long avgHandlingDurationMs;

    // ========== 5. 抗干扰性指标 (3个) ==========
    @Column(name = "avg_snr", precision = 8, scale = 2)
    private BigDecimal avgSnr;

    @Column(name = "avg_sinr", precision = 8, scale = 2)
    private BigDecimal avgSinr;

    @Column(name = "avg_jamming_margin", precision = 8, scale = 2)
    private BigDecimal avgJammingMargin;

    // ========== 6. 人为操作能力指标 (2个) ==========
    @Column(name = "avg_operator_reaction_time_ms", precision = 10, scale = 2)
    private BigDecimal avgOperatorReactionTimeMs;

    @Column(name = "operation_success_rate", precision = 8, scale = 6)
    private BigDecimal operationSuccessRate;

    // ========== 7. 组网能力指标 (3个) ==========
    @Column(name = "avg_network_setup_duration_ms")
    private Long avgNetworkSetupDurationMs;

    @Column(name = "avg_network_setup_speed", precision = 8, scale = 2)
    private BigDecimal avgNetworkSetupSpeed;

    @Column(name = "avg_connectivity_rate", precision = 8, scale = 6)
    private BigDecimal avgConnectivityRate;

    // ========== 8. 机动性指标 (2个) ==========
    @Column(name = "deployment_speed", precision = 8, scale = 2)
    private BigDecimal deploymentSpeed;

    @Column(name = "personnel_avg_deployment_speed", precision = 8, scale = 2)
    private BigDecimal personnelAvgDeploymentSpeed;

    // ========== 9. 安全性指标 (7个) ==========
    @Column(name = "avg_key_age_hours", precision = 10, scale = 2)
    private BigDecimal avgKeyAgeHours;

    @Column(name = "key_compromise_frequency", precision = 10, scale = 4)
    private BigDecimal keyCompromiseFrequency;

    @Column(name = "avg_key_leak_response_time_ms", precision = 10, scale = 2)
    private BigDecimal avgKeyLeakResponseTimeMs;

    @Column(name = "key_security_index", precision = 8, scale = 6)
    private BigDecimal keySecurityIndex;

    @Column(name = "key_response_efficiency", precision = 8, scale = 6)
    private BigDecimal keyResponseEfficiency;

    @Column(name = "detection_probability", precision = 5, scale = 4)
    private BigDecimal detectionProbability;

    @Column(name = "interception_resistance", precision = 5, scale = 2)
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
