package com.ccnu.military.entity;

import lombok.Data;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 专家 AHP 统一层次权重快照：一张表同时存效能与装备，
 * 所有叶子指标的全局权重之和为 1。
 * <p>
 * 层次结构：
 * 根 → 域间一级（效能 vs 装备）→ 效能维度（5维）/ 装备维度 → 叶子指标
 */
@Data
@Entity
@Table(name = "expert_ahp_individual_weights",
       uniqueConstraints = @UniqueConstraint(name = "uk_expert_id", columnNames = {"expert_id"}))
@EntityListeners(AuditingEntityListener.class)
public class ExpertAhpIndividualWeights {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expert_id", nullable = false)
    private Long expertId;

    @Column(name = "expert_name", length = 100)
    private String expertName;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── 域间一级（效能 vs 装备）────────────────────────────────────────
    @Column(name = "eff_domain_weight", precision = 10, scale = 6)
    private BigDecimal effDomainWeight;

    @Column(name = "eq_domain_weight", precision = 10, scale = 6)
    private BigDecimal eqDomainWeight;

    @Column(name = "cross_domain_score", precision = 10, scale = 6)
    private BigDecimal crossDomainScore;

    @Column(name = "cross_domain_confidence", precision = 3, scale = 2)
    private BigDecimal crossDomainConfidence;

    // ── 效能维度层（数量不固定，存 JSON）──────────────────────────────
    @Column(name = "eff_dim_weights_json", columnDefinition = "LONGTEXT")
    private String effDimWeightsJson;

    @Column(name = "eff_dim_count")
    private Integer effDimCount;

    // ── 效能叶子指标全局权重（数量不固定，存 JSON）──────────────────
    @Column(name = "eff_leaf_weights_json", columnDefinition = "LONGTEXT")
    private String effLeafWeightsJson;

    @Column(name = "eff_leaf_count")
    private Integer effLeafCount;

    // ── 效能 CR─────────────────────────────────────────────────────
    @Column(name = "eff_cr", precision = 8, scale = 6)
    private BigDecimal effCr;

    // ── 装备维度层（数量不固定，存 JSON）─────────────────────────────────
    @Column(name = "eq_dim_weights_json", columnDefinition = "LONGTEXT")
    private String eqDimWeightsJson;

    @Column(name = "eq_dim_count")
    private Integer eqDimCount;

    // ── 装备叶子指标全局权重（数量不固定，存 JSON）──────────────────────
    @Column(name = "eq_leaf_weights_json", columnDefinition = "LONGTEXT")
    private String eqLeafWeightsJson;

    @Column(name = "eq_leaf_count")
    private Integer eqLeafCount;

    // ── 装备一致性比率─────────────────────────────────────────────────
    @Column(name = "eq_cr_json", columnDefinition = "LONGTEXT")
    private String eqCrJson;

    // ── 完整结果 JSON（供前后端完整渲染）────────────────────────────────
    @Column(name = "ahp_result_json", columnDefinition = "LONGTEXT")
    private String ahpResultJson;
}
