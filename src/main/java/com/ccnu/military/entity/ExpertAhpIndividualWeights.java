package com.ccnu.military.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 专家 AHP 计算结果快照：与库表分列字段对齐，便于在 Navicat 中直接查看；
 * ahp_result_json 仍保存完整 MatrixCalculationResult 供前后端使用。
 */
@Data
@Entity
@Table(name = "expert_ahp_individual_weights", uniqueConstraints = {
        @UniqueConstraint(name = "uk_expert_id", columnNames = {"expert_id"})
})
public class ExpertAhpIndividualWeights {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expert_id", nullable = false)
    private Long expertId;

    @Column(name = "expert_name", length = 100)
    private String expertName;

    /** 完整计算结果 JSON（维度层、指标层、综合权重、CR 等） */
    @Column(name = "ahp_result_json", columnDefinition = "LONGTEXT")
    private String resultJson;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ---------- 一级维度权重（与维度层 AHP 一致）----------
    @Column(name = "security_weight", precision = 10, scale = 6)
    private BigDecimal securityWeight;

    @Column(name = "reliability_weight", precision = 10, scale = 6)
    private BigDecimal reliabilityWeight;

    @Column(name = "transmission_weight", precision = 10, scale = 6)
    private BigDecimal transmissionWeight;

    @Column(name = "anti_jamming_weight", precision = 10, scale = 6)
    private BigDecimal antiJammingWeight;

    @Column(name = "effect_weight", precision = 10, scale = 6)
    private BigDecimal effectWeight;

    // ---------- 二级指标对总目标的综合权重（维度权重 × 指标层权重）----------
    @Column(name = "security_key_leakage_weight", precision = 10, scale = 6)
    private BigDecimal securityKeyLeakageWeight;

    @Column(name = "security_detected_probability_weight", precision = 10, scale = 6)
    private BigDecimal securityDetectedProbabilityWeight;

    @Column(name = "security_interception_resistance_weight", precision = 10, scale = 6)
    private BigDecimal securityInterceptionResistanceWeight;

    @Column(name = "reliability_crash_rate_weight", precision = 10, scale = 6)
    private BigDecimal reliabilityCrashRateWeight;

    @Column(name = "reliability_recovery_capability_weight", precision = 10, scale = 6)
    private BigDecimal reliabilityRecoveryCapabilityWeight;

    @Column(name = "reliability_communication_availability_weight", precision = 10, scale = 6)
    private BigDecimal reliabilityCommunicationAvailabilityWeight;

    @Column(name = "transmission_bandwidth_weight", precision = 10, scale = 6)
    private BigDecimal transmissionBandwidthWeight;

    @Column(name = "transmission_call_setup_time_weight", precision = 10, scale = 6)
    private BigDecimal transmissionCallSetupTimeWeight;

    @Column(name = "transmission_transmission_delay_weight", precision = 10, scale = 6)
    private BigDecimal transmissionTransmissionDelayWeight;

    @Column(name = "transmission_bit_error_rate_weight", precision = 10, scale = 6)
    private BigDecimal transmissionBitErrorRateWeight;

    @Column(name = "transmission_throughput_weight", precision = 10, scale = 6)
    private BigDecimal transmissionThroughputWeight;

    @Column(name = "transmission_spectral_efficiency_weight", precision = 10, scale = 6)
    private BigDecimal transmissionSpectralEfficiencyWeight;

    @Column(name = "anti_jamming_sinr_weight", precision = 10, scale = 6)
    private BigDecimal antiJammingSinrWeight;

    @Column(name = "anti_jamming_anti_jamming_margin_weight", precision = 10, scale = 6)
    private BigDecimal antiJammingAntiJammingMarginWeight;

    @Column(name = "anti_jamming_communication_distance_weight", precision = 10, scale = 6)
    private BigDecimal antiJammingCommunicationDistanceWeight;

    @Column(name = "effect_damage_rate_weight", precision = 10, scale = 6)
    private BigDecimal effectDamageRateWeight;

    @Column(name = "effect_mission_completion_rate_weight", precision = 10, scale = 6)
    private BigDecimal effectMissionCompletionRateWeight;

    @Column(name = "effect_blind_rate_weight", precision = 10, scale = 6)
    private BigDecimal effectBlindRateWeight;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = LocalDateTime.now();
    }
}
