package com.ccnu.military.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 定量指标配置模板实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "equipment_qt_indicator_def")
public class EquipmentQtIndicatorDef {

    @Id
    @Column(name = "indicator_key", length = 50)
    private String indicatorKey;

    @Column(name = "indicator_name", nullable = false, length = 100)
    private String indicatorName;

    @Column(name = "indicator_name_en", length = 100)
    private String indicatorNameEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false)
    private Phase phase;

    @Column(name = "dimension", nullable = false, length = 50)
    private String dimension;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "source_table", length = 100)
    private String sourceTable;

    @Column(name = "source_field", length = 100)
    private String sourceField;

    @Enumerated(EnumType.STRING)
    @Column(name = "aggregation_method", nullable = false)
    private AggregationMethod aggregationMethod = AggregationMethod.direct;

    @Column(name = "custom_sql_template", columnDefinition = "TEXT")
    private String customSqlTemplate;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "score_direction", nullable = false)
    private ScoreDirection scoreDirection;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

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

    public enum Phase {
        pre_war, mid_war, post_war
    }

    public enum AggregationMethod {
        direct, avg, sum, count, percentage, custom, avg_conditional
    }

    public enum ScoreDirection {
        positive, negative
    }
}
