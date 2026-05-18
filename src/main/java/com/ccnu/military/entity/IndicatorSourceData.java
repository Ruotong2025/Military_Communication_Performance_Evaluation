package com.ccnu.military.entity;

import lombok.*;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 指标可测得数据关联实体
 */
@Data
@Entity
@Table(name = "indicator_source_data")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndicatorSourceData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "indicator_id", nullable = false)
    private Long indicatorId;

    @Column(name = "source_data_name", nullable = false, length = 200)
    private String sourceDataName;

    @Column(name = "source_data_code", length = 100)
    private String sourceDataCode;

    @Column(name = "measurement_method", length = 500)
    private String measurementMethod;

    @Column(name = "data_type", length = 50)
    @Enumerated(EnumType.STRING)
    private DataType dataType;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "priority")
    private Integer priority = 0;

    @Column(name = "confidence", precision = 5, scale = 2)
    private BigDecimal confidence;

    @Column(name = "is_formula_related")
    private Boolean isFormulaRelated = false;

    @Column(name = "formula_symbol", length = 50)
    private String formulaSymbol;

    @Column(name = "is_essential")
    private Boolean isEssential = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum DataType {
        NUMERIC,
        PERCENTAGE
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
