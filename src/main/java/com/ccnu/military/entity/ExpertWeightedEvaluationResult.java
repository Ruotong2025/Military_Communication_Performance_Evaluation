package com.ccnu.military.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 专家集结加权评估结果实体
 * 保存集结后的权重与评估得分
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "expert_weighted_evaluation_result")
public class ExpertWeightedEvaluationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evaluation_id", nullable = false, length = 50)
    private String evaluationId;

    @Column(name = "operation_id", nullable = false, length = 50)
    private String operationId;

    @Column(name = "expert_count")
    private Integer expertCount;

    @Column(name = "expert_ids", length = 500)
    private String expertIds;

    // ==================== 一致性（6个CR）====================
    @Column(name = "cr_dim", precision = 8, scale = 6)
    private BigDecimal crDim;

    @Column(name = "cr_security", precision = 8, scale = 6)
    private BigDecimal crSecurity;

    @Column(name = "cr_reliability", precision = 8, scale = 6)
    private BigDecimal crReliability;

    @Column(name = "cr_transmission", precision = 8, scale = 6)
    private BigDecimal crTransmission;

    @Column(name = "cr_anti_jamming", precision = 8, scale = 6)
    private BigDecimal crAntiJamming;

    @Column(name = "cr_effect", precision = 8, scale = 6)
    private BigDecimal crEffect;

    // ==================== 5个维度集结权重 ====================
    @Column(name = "dim_weight_security", precision = 10, scale = 6)
    private BigDecimal dimWeightSecurity;

    @Column(name = "dim_weight_reliability", precision = 10, scale = 6)
    private BigDecimal dimWeightReliability;

    @Column(name = "dim_weight_transmission", precision = 10, scale = 6)
    private BigDecimal dimWeightTransmission;

    @Column(name = "dim_weight_anti_jamming", precision = 10, scale = 6)
    private BigDecimal dimWeightAntiJamming;

    @Column(name = "dim_weight_effect", precision = 10, scale = 6)
    private BigDecimal dimWeightEffect;

    // ==================== 18个二级指标综合权重 ====================
    @Column(name = "ind_weight_key_leakage", precision = 10, scale = 6)
    private BigDecimal indWeightKeyLeakage;

    @Column(name = "ind_weight_detected_probability", precision = 10, scale = 6)
    private BigDecimal indWeightDetectedProbability;

    @Column(name = "ind_weight_interception_resistance", precision = 10, scale = 6)
    private BigDecimal indWeightInterceptionResistance;

    @Column(name = "ind_weight_crash_rate", precision = 10, scale = 6)
    private BigDecimal indWeightCrashRate;

    @Column(name = "ind_weight_recovery_capability", precision = 10, scale = 6)
    private BigDecimal indWeightRecoveryCapability;

    @Column(name = "ind_weight_communication_availability", precision = 10, scale = 6)
    private BigDecimal indWeightCommunicationAvailability;

    @Column(name = "ind_weight_bandwidth", precision = 10, scale = 6)
    private BigDecimal indWeightBandwidth;

    @Column(name = "ind_weight_call_setup_time", precision = 10, scale = 6)
    private BigDecimal indWeightCallSetupTime;

    @Column(name = "ind_weight_transmission_delay", precision = 10, scale = 6)
    private BigDecimal indWeightTransmissionDelay;

    @Column(name = "ind_weight_bit_error_rate", precision = 10, scale = 6)
    private BigDecimal indWeightBitErrorRate;

    @Column(name = "ind_weight_throughput", precision = 10, scale = 6)
    private BigDecimal indWeightThroughput;

    @Column(name = "ind_weight_spectral_efficiency", precision = 10, scale = 6)
    private BigDecimal indWeightSpectralEfficiency;

    @Column(name = "ind_weight_sinr", precision = 10, scale = 6)
    private BigDecimal indWeightSinr;

    @Column(name = "ind_weight_anti_jamming_margin", precision = 10, scale = 6)
    private BigDecimal indWeightAntiJammingMargin;

    @Column(name = "ind_weight_communication_distance", precision = 10, scale = 6)
    private BigDecimal indWeightCommunicationDistance;

    @Column(name = "ind_weight_damage_rate", precision = 10, scale = 6)
    private BigDecimal indWeightDamageRate;

    @Column(name = "ind_weight_mission_completion_rate", precision = 10, scale = 6)
    private BigDecimal indWeightMissionCompletionRate;

    @Column(name = "ind_weight_blind_rate", precision = 10, scale = 6)
    private BigDecimal indWeightBlindRate;

    // ==================== 18个指标加权得分 ====================
    @Column(name = "score_key_leakage", precision = 8, scale = 6)
    private BigDecimal scoreKeyLeakage;

    @Column(name = "score_detected_probability", precision = 8, scale = 6)
    private BigDecimal scoreDetectedProbability;

    @Column(name = "score_interception_resistance", precision = 8, scale = 6)
    private BigDecimal scoreInterceptionResistance;

    @Column(name = "score_crash_rate", precision = 8, scale = 6)
    private BigDecimal scoreCrashRate;

    @Column(name = "score_recovery_capability", precision = 8, scale = 6)
    private BigDecimal scoreRecoveryCapability;

    @Column(name = "score_communication_availability", precision = 8, scale = 6)
    private BigDecimal scoreCommunicationAvailability;

    @Column(name = "score_bandwidth", precision = 8, scale = 6)
    private BigDecimal scoreBandwidth;

    @Column(name = "score_call_setup_time", precision = 8, scale = 6)
    private BigDecimal scoreCallSetupTime;

    @Column(name = "score_transmission_delay", precision = 8, scale = 6)
    private BigDecimal scoreTransmissionDelay;

    @Column(name = "score_bit_error_rate", precision = 8, scale = 6)
    private BigDecimal scoreBitErrorRate;

    @Column(name = "score_throughput", precision = 8, scale = 6)
    private BigDecimal scoreThroughput;

    @Column(name = "score_spectral_efficiency", precision = 8, scale = 6)
    private BigDecimal scoreSpectralEfficiency;

    @Column(name = "score_sinr", precision = 8, scale = 6)
    private BigDecimal scoreSinr;

    @Column(name = "score_anti_jamming_margin", precision = 8, scale = 6)
    private BigDecimal scoreAntiJammingMargin;

    @Column(name = "score_communication_distance", precision = 8, scale = 6)
    private BigDecimal scoreCommunicationDistance;

    @Column(name = "score_damage_rate", precision = 8, scale = 6)
    private BigDecimal scoreDamageRate;

    @Column(name = "score_mission_completion_rate", precision = 8, scale = 6)
    private BigDecimal scoreMissionCompletionRate;

    @Column(name = "score_blind_rate", precision = 8, scale = 6)
    private BigDecimal scoreBlindRate;

    // ==================== 5个维度加权得分 ====================
    @Column(name = "score_security", precision = 8, scale = 6)
    private BigDecimal scoreSecurity;

    @Column(name = "score_reliability", precision = 8, scale = 6)
    private BigDecimal scoreReliability;

    @Column(name = "score_transmission", precision = 8, scale = 6)
    private BigDecimal scoreTransmission;

    @Column(name = "score_anti_jamming", precision = 8, scale = 6)
    private BigDecimal scoreAntiJamming;

    @Column(name = "score_effect", precision = 8, scale = 6)
    private BigDecimal scoreEffect;

    // ==================== 综合得分 ====================
    @Column(name = "total_score", precision = 10, scale = 6)
    private BigDecimal totalScore;

    @Column(name = "expert_weights_json", columnDefinition = "LONGTEXT")
    private String expertWeightsJson;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
