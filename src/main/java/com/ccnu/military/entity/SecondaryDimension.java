package com.ccnu.military.entity;

import javax.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "mtl_secondary_dimension")
public class SecondaryDimension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_dimension_id", nullable = false)
    private PrimaryDimension primaryDimension;

    @Column(name = "dimension_name", nullable = false)
    private String dimensionName;

    @Column(name = "dimension_code", nullable = false)
    private String dimensionCode;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "metric_type")
    @Enumerated(EnumType.STRING)
    private MetricType metricType = MetricType.QUANTITATIVE;

    @Column(name = "aggregation_method")
    private String aggregationMethod = "avg";

    @Column(name = "score_direction")
    @Enumerated(EnumType.STRING)
    private ScoreDirection scoreDirection = ScoreDirection.POSITIVE;

    @Column(name = "unit")
    private String unit;

    @Column(name = "baseline_value", precision = 20, scale = 6)
    private BigDecimal baselineValue;

    @Column(name = "target_value", precision = 20, scale = 6)
    private BigDecimal targetValue;

    @Column(name = "weight", precision = 10, scale = 6)
    private BigDecimal weight = BigDecimal.ZERO;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum MetricType {
        QUANTITATIVE,
        QUALITATIVE
    }

    public enum ScoreDirection {
        POSITIVE,
        NEGATIVE
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
