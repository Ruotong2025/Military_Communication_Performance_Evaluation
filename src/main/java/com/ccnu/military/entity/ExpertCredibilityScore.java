package com.ccnu.military.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 专家可信度评估得分实体
 * 主键为 expert_id，与 expert_base_info 表关联
 */
@Data
@Entity
@Table(name = "expert_credibility_evaluation_score")
public class ExpertCredibilityScore {

    @Id
    @Column(name = "expert_id", nullable = false)
    private Long expertId;

    @Column(name = "expert_name", nullable = false, length = 100)
    private String expertName;

    // 10个维度得分
    @Column(name = "title_ql", precision = 5, scale = 2)
    private BigDecimal titleQl = BigDecimal.ZERO;

    @Column(name = "position_ql", precision = 5, scale = 2)
    private BigDecimal positionQl = BigDecimal.ZERO;

    @Column(name = "education_experience_ql", precision = 5, scale = 2)
    private BigDecimal educationExperienceQl = BigDecimal.ZERO;

    @Column(name = "academic_achievements_ql", precision = 5, scale = 2)
    private BigDecimal academicAchievementsQl = BigDecimal.ZERO;

    @Column(name = "research_achievements_ql", precision = 5, scale = 2)
    private BigDecimal researchAchievementsQl = BigDecimal.ZERO;

    @Column(name = "exercise_experience_ql", precision = 5, scale = 2)
    private BigDecimal exerciseExperienceQl = BigDecimal.ZERO;

    @Column(name = "military_training_knowledge_ql", precision = 5, scale = 2)
    private BigDecimal militaryTrainingKnowledgeQl = BigDecimal.ZERO;

    @Column(name = "system_simulation_knowledge_ql", precision = 5, scale = 2)
    private BigDecimal systemSimulationKnowledgeQl = BigDecimal.ZERO;

    @Column(name = "statistics_knowledge_ql", precision = 5, scale = 2)
    private BigDecimal statisticsKnowledgeQl = BigDecimal.ZERO;

    @Column(name = "professional_years_qt", precision = 5, scale = 2)
    private BigDecimal professionalYearsQt = BigDecimal.ZERO;

    // 综合得分
    @Column(name = "total_score", precision = 5, scale = 2)
    private BigDecimal totalScore = BigDecimal.ZERO;

    @Column(name = "credibility_level", length = 20)
    private String credibilityLevel;

    // 权重配置
    @Column(name = "weight_title", precision = 5, scale = 2)
    private BigDecimal weightTitle = new BigDecimal("0.10");

    @Column(name = "weight_position", precision = 5, scale = 2)
    private BigDecimal weightPosition = new BigDecimal("0.08");

    @Column(name = "weight_education", precision = 5, scale = 2)
    private BigDecimal weightEducation = new BigDecimal("0.08");

    @Column(name = "weight_academic", precision = 5, scale = 2)
    private BigDecimal weightAcademic = new BigDecimal("0.10");

    @Column(name = "weight_research", precision = 5, scale = 2)
    private BigDecimal weightResearch = new BigDecimal("0.10");

    @Column(name = "weight_exercise", precision = 5, scale = 2)
    private BigDecimal weightExercise = new BigDecimal("0.10");

    @Column(name = "weight_military_training", precision = 5, scale = 2)
    private BigDecimal weightMilitaryTraining = new BigDecimal("0.12");

    @Column(name = "weight_system_simulation", precision = 5, scale = 2)
    private BigDecimal weightSystemSimulation = new BigDecimal("0.12");

    @Column(name = "weight_statistics", precision = 5, scale = 2)
    private BigDecimal weightStatistics = new BigDecimal("0.10");

    @Column(name = "weight_professional_years", precision = 5, scale = 2)
    private BigDecimal weightProfessionalYears = new BigDecimal("0.10");

    // 评估信息
    @Column(name = "evaluation_date")
    private LocalDate evaluationDate;

    @Column(name = "evaluator", length = 100)
    private String evaluator;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (evaluationDate == null) {
            evaluationDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
