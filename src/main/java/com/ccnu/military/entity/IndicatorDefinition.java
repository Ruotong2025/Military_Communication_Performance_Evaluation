package com.ccnu.military.entity;

import lombok.*;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 指标定义实体
 */
@Data
@Entity
@Table(name = "indicator_definition",
       uniqueConstraints = @UniqueConstraint(columnNames = "indicator_name"))
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndicatorDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "indicator_name", nullable = false, length = 200)
    private String indicatorName;

    @Column(name = "indicator_name_en", length = 200)
    private String indicatorNameEn;

    @Column(name = "indicator_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private IndicatorType indicatorType;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "formula", columnDefinition = "text")
    private String formula;

    @Column(name = "formula_description", columnDefinition = "text")
    private String formulaDescription;

    @Column(name = "calculation_method", length = 500)
    private String calculationMethod;

    @Column(name = "source_data_hint", columnDefinition = "text")
    private String sourceDataHint;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_from_ai")
    private Boolean isFromAi = false;

    @Column(name = "ai_confidence", precision = 5, scale = 2)
    private BigDecimal aiConfidence;

    @Column(name = "related_indicators", columnDefinition = "json")
    private String relatedIndicators;

    @Column(name = "related_source_data", columnDefinition = "json")
    private String relatedSourceData;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private List<IndicatorSourceData> sourceDataList;

    public enum IndicatorType {
        QUALITATIVE,
        QUANTITATIVE
    }

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
}
