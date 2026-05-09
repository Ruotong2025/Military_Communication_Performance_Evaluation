package com.ccnu.military.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 动态定性评估会话实体
 * 用于管理评估会话状态和配置
 */
@Data
@Entity
@Table(name = "dynamic_ql_session")
public class DynamicQlSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true)
    private String sessionId;

    // ==================== 关联信息 ====================
    @Column(name = "batch_id")
    private String batchId;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "template_name")
    private String templateName;

    // ==================== 会话状态 ====================
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private SessionStatus status = SessionStatus.DRAFT;

    // ==================== 评估配置（JSON字符串存储） ====================
    @Column(name = "selected_experts", columnDefinition = "TEXT")
    private String selectedExpertsJson;

    @Column(name = "evaluation_mode")
    @Enumerated(EnumType.STRING)
    private EvaluationMode evaluationMode = EvaluationMode.SINGLE_EXPERT;

    @Column(name = "show_reference_data")
    private Boolean showReferenceData = true;

    // ==================== 进度跟踪 ====================
    @Column(name = "total_count")
    private Integer totalCount = 0;

    @Column(name = "completed_count")
    private Integer completedCount = 0;

    // ==================== 元数据 ====================
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== 枚举定义 ====================

    public enum SessionStatus {
        DRAFT,       // 草稿
        IN_PROGRESS, // 进行中
        COMPLETED    // 已完成
    }

    public enum EvaluationMode {
        SINGLE_EXPERT,  // 单专家模式
        MULTI_EXPERT    // 多专家模式
    }

    // ==================== 生命周期回调 ====================

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
