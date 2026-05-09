package com.ccnu.military.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 动态AHP集结结果实体
 * 存储专家对动态指标体系AHP打分的集结结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dynamic_ahp_aggregation_result")
public class DynamicAhpAggregationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 专家组ID（8位随机码，用于唯一标识）
     */
    @Column(name = "group_id", length = 16, nullable = false, unique = true)
    private String groupId;

    /**
     * 指标模板ID
     */
    @Column(name = "template_id", nullable = false)
    private Long templateId;

    /**
     * 模板名称
     */
    @Column(name = "template_name", length = 200)
    private String templateName;

    /**
     * 参与专家ID列表（逗号分隔）
     */
    @Column(name = "expert_ids", columnDefinition = "TEXT", nullable = false)
    private String expertIds;

    /**
     * 专家人数
     */
    @Column(name = "expert_count", nullable = false)
    private Integer expertCount;

    /**
     * 层级权重JSON
     * 格式: {"战术级": 0.6, "战役级": 0.4}
     */
    @Column(name = "level_weights_json", columnDefinition = "TEXT")
    private String levelWeightsJson;

    /**
     * 一级维度权重JSON（按层级分组）
     * 格式: {"战术级": {"通信质量": 0.35, "抗干扰": 0.25}, "战役级": {...}}
     */
    @Column(name = "dimension_weights_json", columnDefinition = "TEXT")
    private String dimensionWeightsJson;

    /**
     * 二级指标综合权重JSON（最终结果）
     * 格式: [{"levelName": "战术级", "primaryName": "通信质量", "secondaryName": "时延", "combinedWeight": 0.05}, ...]
     */
    @Column(name = "combined_weights_json", columnDefinition = "TEXT")
    private String combinedWeightsJson;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
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
