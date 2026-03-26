package com.ccnu.military.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 专家基础信息实体
 */
@Data
@Entity
@Table(name = "expert_base_info")
public class ExpertBaseInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expert_id")
    private Long expertId;

    @Column(name = "expert_name", nullable = false, unique = true, length = 100)
    private String expertName;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "work_unit", length = 200)
    private String workUnit;

    @Column(name = "department", length = 100)
    private String department;

    // 职称信息
    @Column(name = "title", length = 50)
    private String title;

    @Column(name = "title_level")
    private Integer titleLevel;

    @Column(name = "is_academician")
    private Boolean isAcademician = false;

    @Column(name = "is_yangtze_scholar")
    private Boolean isYangtzeScholar = false;

    @Column(name = "is_excellent_youth")
    private Boolean isExcellentYouth = false;

    @Column(name = "is_doctoral_supervisor")
    private Boolean isDoctoralSupervisor = false;

    @Column(name = "is_master_supervisor")
    private Boolean isMasterSupervisor = false;

    // 职务信息
    @Column(name = "position", length = 50)
    private String position;

    @Column(name = "position_level")
    private Integer positionLevel;

    // 教育背景
    @Column(name = "education", length = 50)
    private String education;

    @Column(name = "education_level")
    private Integer educationLevel;

    @Column(name = "graduated_school", length = 200)
    private String graduatedSchool;

    @Column(name = "school_level", length = 20)
    private String schoolLevel;

    @Column(name = "major", length = 100)
    private String major;

    // 工作经历
    @Column(name = "work_years")
    private Integer workYears;

    @Column(name = "professional_years")
    private Integer professionalYears;

    @Column(name = "exercise_experience", columnDefinition = "TEXT")
    private String exerciseExperience;

    // 科研成果
    @Column(name = "academic_count")
    private Integer academicCount = 0;

    @Column(name = "academic_sci_ei_count")
    private Integer academicSciEiCount = 0;

    @Column(name = "academic_core_count")
    private Integer academicCoreCount = 0;

    @Column(name = "research_count")
    private Integer researchCount = 0;

    @Column(name = "research_participate_count")
    private Integer researchParticipateCount = 0;

    @Column(name = "patent_count")
    private Integer patentCount = 0;

    @Column(name = "software_copyright_count")
    private Integer softwareCopyrightCount = 0;

    @Column(name = "monograph_count")
    private Integer monographCount = 0;

    @Column(name = "award_count")
    private Integer awardCount = 0;

    // 专业领域
    @Column(name = "expertise_area", length = 500)
    private String expertiseArea;

    @Column(name = "has_military_training")
    private Boolean hasMilitaryTraining = false;

    @Column(name = "has_system_simulation")
    private Boolean hasSystemSimulation = false;

    @Column(name = "has_statistics")
    private Boolean hasStatistics = false;

    // 专业知识考核成绩
    @Column(name = "military_training_score")
    private Integer militaryTrainingScore;

    @Column(name = "system_simulation_score")
    private Integer systemSimulationScore;

    @Column(name = "statistics_score")
    private Integer statisticsScore;

    // 演习训练
    @Column(name = "national_exercise_count")
    private Integer nationalExerciseCount = 0;

    @Column(name = "national_exercise_participate_count")
    private Integer nationalExerciseParticipateCount = 0;

    @Column(name = "regional_exercise_count")
    private Integer regionalExerciseCount = 0;

    @Column(name = "regional_exercise_participate_count")
    private Integer regionalExerciseParticipateCount = 0;

    @Column(name = "military_practice_years")
    private Integer militaryPracticeYears = 0;

    // 其他
    @Column(name = "status")
    private Integer status = 1;

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
