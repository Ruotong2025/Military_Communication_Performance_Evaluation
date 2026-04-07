package com.ccnu.military.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 定性指标配置模板实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "equipment_ql_indicator_def")
public class EquipmentQlIndicatorDef {

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

    @Column(name = "reference_table", length = 100)
    private String referenceTable;

    @Column(name = "reference_field", length = 100)
    private String referenceField;

    @Column(name = "reference_sql_template", columnDefinition = "TEXT")
    private String referenceSqlTemplate;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_method", nullable = false)
    private EvaluationMethod evaluationMethod = EvaluationMethod.grade;

    @Column(name = "grade_keys", columnDefinition = "JSON")
    private String gradeKeys;

    @Column(name = "confidence_required", nullable = false)
    private Boolean confidenceRequired = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_method", nullable = false)
    private ConfidenceMethod confidenceMethod = ConfidenceMethod.percentage;

    @Column(name = "allow_comment", nullable = false)
    private Boolean allowComment = true;

    @Column(name = "scoring_help", columnDefinition = "TEXT")
    private String scoringHelp;

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

    public enum EvaluationMethod {
        grade, likert5, likert10, raw_score
    }

    public enum ConfidenceMethod {
        percentage, level5
    }
}
