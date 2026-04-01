package com.ccnu.military.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成本指标配置实体
 * <p>定义成本指标的元数据，支持动态扩展。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cost_indicator_config")
public class CostIndicatorConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 指标唯一标识 */
    @Column(name = "indicator_key", nullable = false, length = 80, unique = true)
    private String indicatorKey;

    /** 指标中文名称 */
    @Column(name = "indicator_name", nullable = false, length = 200)
    private String indicatorName;

    /** 指标英文名称 */
    @Column(name = "indicator_name_en", length = 200)
    private String indicatorNameEn;

    /** 指标大类 */
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    /** 指标子类 */
    @Column(name = "sub_category", length = 50)
    private String subCategory;

    /** 指标类型: cost=成本型, benefit=效益型 */
    @Column(name = "indicator_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private IndicatorType indicatorType;

    /** 数据来源表名 */
    @Column(name = "source_table", length = 100)
    private String sourceTable;

    /** 数据来源字段名 */
    @Column(name = "source_field", length = 100)
    private String sourceField;

    /** 数据来源表达式 */
    @Column(name = "source_expression", length = 500)
    private String sourceExpression;

    /** 归一化下限 */
    @Column(name = "normalization_min", precision = 15, scale = 4)
    private BigDecimal normalizationMin;

    /** 归一化上限 */
    @Column(name = "normalization_max", precision = 15, scale = 4)
    private BigDecimal normalizationMax;

    /** 是否使用实际数据范围 */
    @Column(name = "use_actual_range")
    private Boolean useActualRange = true;

    /** 权重 */
    @Column(name = "weight", precision = 8, scale = 6)
    private BigDecimal weight;

    /** 是否启用 */
    @Column(name = "is_active")
    private Boolean isActive = true;

    /** 排序序号 */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /** 计量单位 */
    @Column(name = "unit", length = 30)
    private String unit;

    /** 单位简称 */
    @Column(name = "unit_abbrev", length = 10)
    private String unitAbbrev;

    /** 指标说明 */
    @Column(name = "description", columnDefinition = "text")
    private String description;

    /** 备注 */
    @Column(name = "remarks", columnDefinition = "text")
    private String remarks;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum IndicatorType {
        cost,   // 成本型: 数值越大成本越高
        benefit // 效益型: 数值越大成本越低
    }
}
