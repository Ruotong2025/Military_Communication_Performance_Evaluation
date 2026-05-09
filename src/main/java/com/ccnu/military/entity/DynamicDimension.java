package com.ccnu.military.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "dynamic_dimension",
       uniqueConstraints = @UniqueConstraint(columnNames = {"template_id", "dimension_level", "code"}))
public class DynamicDimension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "dimension_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private DimensionLevel dimensionLevel;

    @Column(name = "parent_id")
    private Long parentId;  // 父节点ID（用于树形结构）

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "weight", precision = 10, scale = 6)
    private BigDecimal weight = BigDecimal.ZERO;

    // 以下字段仅对SECONDARY维度有效
    @Column(name = "metric_type")
    @Enumerated(EnumType.STRING)
    private MetricType metricType;

    @Column(name = "aggregation_method")
    private String aggregationMethod;

    @Column(name = "score_direction")
    @Enumerated(EnumType.STRING)
    private ScoreDirection scoreDirection;

    @Column(name = "unit")
    private String unit;

    @Column(name = "baseline_value", precision = 20, scale = 6)
    private BigDecimal baselineValue;

    @Column(name = "target_value", precision = 20, scale = 6)
    private BigDecimal targetValue;

    @Column(name = "average_value", precision = 20, scale = 6)
    private BigDecimal averageValue;  // 平均数（用于归一化参考）

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum DimensionLevel {
        LEVEL,      // 层级（如战术级、战役级）
        PRIMARY,    // 一级维度
        SECONDARY   // 二级维度（最小粒度）
    }

    public enum MetricType {
        QUANTITATIVE,  // 定量
        QUALITATIVE    // 定性
    }

    public enum ScoreDirection {
        POSITIVE,   // 正向（值越大越好）
        NEGATIVE    // 负向（值越小越好）
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
