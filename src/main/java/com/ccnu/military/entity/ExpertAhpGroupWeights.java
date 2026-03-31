package com.ccnu.military.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 专家集结权重快照（专家组维度）
 * 存储每次集结计算的权重结果，以专家ID集合作为唯一标识
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "expert_ahp_group_weights")
public class ExpertAhpGroupWeights {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 专家组ID（UUID），每执行一次集结计算生成一个
     */
    @Column(name = "group_id", nullable = false, length = 50)
    private String groupId;

    /**
     * 专家ID集合（按ID排序后用逗号分隔），作为唯一标识
     * 如: "1,3,5" 或 "2,4"
     */
    @Column(name = "expert_ids", nullable = false, length = 500)
    private String expertIds;

    /**
     * 专家数量
     */
    @Column(name = "expert_count")
    private Integer expertCount;

    /**
     * 专家组名称（可选描述）
     */
    @Column(name = "group_name", length = 200)
    private String groupName;

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

    // ==================== 专家权重明细JSON ====================
    @Column(name = "expert_weights_json", columnDefinition = "LONGTEXT")
    private String expertWeightsJson;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
