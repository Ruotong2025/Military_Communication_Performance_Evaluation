package com.ccnu.military.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 专家集结 AHP 统一层次权重快照：一张表同时存效能与装备，
 * 所有叶子指标的全局权重之和为 1。
 * <p>
 * 参照 ExpertAhpIndividualWeights 的设计，使用 JSON 存储动态数量的维度与指标权重。
 * <p>
 * 层次结构：
 * 根 → 域间一级（效能 vs 装备）→ 效能维度（数量不固定）/ 装备维度 → 叶子指标
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "expert_ahp_group_weights",
       uniqueConstraints = @UniqueConstraint(name = "uk_expert_ids", columnNames = {"expert_ids"}))
@EntityListeners(AuditingEntityListener.class)
public class ExpertAhpGroupWeights {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── 专家组基本信息 ──────────────────────────────────────────────
    @Column(name = "group_id", nullable = false, length = 50)
    private String groupId;

    @Column(name = "expert_ids", nullable = false, length = 500)
    private String expertIds;

    @Column(name = "expert_count")
    private Integer expertCount;

    @Column(name = "group_name", length = 200)
    private String groupName;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── 域间一级（效能 vs 装备）────────────────────────────────────
    @Column(name = "cross_domain_score", precision = 10, scale = 6)
    private BigDecimal crossDomainScore;

    @Column(name = "cross_domain_confidence", precision = 3, scale = 2)
    private BigDecimal crossDomainConfidence;

    @Column(name = "eff_domain_weight", precision = 10, scale = 6)
    private BigDecimal effDomainWeight;

    @Column(name = "eq_domain_weight", precision = 10, scale = 6)
    private BigDecimal eqDomainWeight;

    // ── 效能维度层（JSON）──────────────────────────────────────────
    @Column(name = "eff_dim_weights_json", columnDefinition = "LONGTEXT")
    private String effDimWeightsJson;

    @Column(name = "eff_dim_count")
    private Integer effDimCount;

    // ── 效能叶子指标全局权重（JSON）────────────────────────────────
    @Column(name = "eff_leaf_weights_json", columnDefinition = "LONGTEXT")
    private String effLeafWeightsJson;

    @Column(name = "eff_leaf_count")
    private Integer effLeafCount;

    @Column(name = "eff_cr", precision = 8, scale = 6)
    private BigDecimal effCr;

    // ── 装备维度层（JSON）──────────────────────────────────────────
    @Column(name = "eq_dim_weights_json", columnDefinition = "LONGTEXT")
    private String eqDimWeightsJson;

    @Column(name = "eq_dim_count")
    private Integer eqDimCount;

    // ── 装备叶子指标全局权重（JSON）────────────────────────────────
    @Column(name = "eq_leaf_weights_json", columnDefinition = "LONGTEXT")
    private String eqLeafWeightsJson;

    @Column(name = "eq_leaf_count")
    private Integer eqLeafCount;

    @Column(name = "eq_cr_json", columnDefinition = "LONGTEXT")
    private String eqCrJson;

    // ── 专家权重明细 JSON（可信度信息）────────────────────────────
    @Column(name = "expert_weights_json", columnDefinition = "LONGTEXT")
    private String expertWeightsJson;

    // ── 完整集结统一快照 JSON（参照 AhpIndividualResult）───────────
    @Column(name = "aggregated_unified_json", columnDefinition = "LONGTEXT")
    private String aggregatedUnifiedJson;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}