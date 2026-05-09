package com.ccnu.military.entity;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 动态AHP权重计算结果实体
 * 存储专家对动态指标体系的AHP层次分析权重结果
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "dynamic_ahp_weights")
public class DynamicAhpWeights {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 专家ID
     */
    @Column(name = "expert_id", nullable = false)
    private Long expertId;

    /**
     * 专家名称
     */
    @Column(name = "expert_name", length = 100)
    private String expertName;

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
     * 层级名称
     */
    @Column(name = "level_name", length = 100)
    private String levelName;

    /**
     * 维度类型：LEVEL、PRIMARY、SECONDARY
     */
    @Column(name = "dimension_type", length = 50)
    @Enumerated(EnumType.STRING)
    private DimensionType dimensionType;

    /**
     * 维度编码
     */
    @Column(name = "dimension_code", length = 100)
    private String dimensionCode;

    /**
     * 维度名称
     */
    @Column(name = "dimension_name", length = 200)
    private String dimensionName;

    /**
     * 维度描述
     */
    @Column(name = "dimension_description", length = 500)
    private String dimensionDescription;

    /**
     * 权重值
     */
    @Column(name = "weight", precision = 10, scale = 6)
    private BigDecimal weight;

    /**
     * 层级内排序
     */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /**
     * 最大特征值
     */
    @Column(name = "lambda_max", precision = 10, scale = 6)
    private BigDecimal lambdaMax;

    /**
     * 一致性指标CI
     */
    @Column(name = "ci", precision = 10, scale = 6)
    private BigDecimal ci;

    /**
     * 随机一致性指标RI
     */
    @Column(name = "ri", precision = 10, scale = 4)
    private BigDecimal ri;

    /**
     * 一致性比率CR
     */
    @Column(name = "cr", precision = 10, scale = 6)
    private BigDecimal cr;

    /**
     * 是否通过一致性检验
     */
    @Column(name = "is_consistent")
    private Boolean isConsistent;

    /**
     * 综合权重（跨层级累积）
     */
    @Column(name = "combined_weight", precision = 10, scale = 6)
    private BigDecimal combinedWeight;

    /**
     * 父级维度编码（用于构建层级树）
     */
    @Column(name = "parent_code", length = 100)
    private String parentCode;

    /**
     * 路径（用于前端展示，如：战术级/通信质量/时延）
     */
    @Column(name = "weight_path", length = 500)
    private String weightPath;

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

    public enum DimensionType {
        /**
         * 层级
         */
        LEVEL,

        /**
         * 一级维度
         */
        PRIMARY,

        /**
         * 二级指标
         */
        SECONDARY
    }
}
