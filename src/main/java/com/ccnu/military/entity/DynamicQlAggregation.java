package com.ccnu.military.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 动态定性评估集结结果实体
 * 存储多位专家评分的集结结果
 */
@Data
@Entity
@Table(name = "dynamic_ql_aggregation",
       uniqueConstraints = @UniqueConstraint(columnNames = {"batch_id", "operation_id", "secondary_code"}))
public class DynamicQlAggregation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== 批次与模板 ====================
    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Column(name = "template_id")
    private Long templateId;

    // ==================== 作战与指标 ====================
    @Column(name = "operation_id", nullable = false)
    private String operationId;

    @Column(name = "secondary_code", nullable = false)
    private String secondaryCode;

    @Column(name = "secondary_name")
    private String secondaryName;

    // ==================== 集结结果 ====================
    @Column(name = "aggregated_value", precision = 10, scale = 6)
    private BigDecimal aggregatedValue;

    @Column(name = "aggregation_method")
    @Enumerated(EnumType.STRING)
    private AggregationMethod aggregationMethod = AggregationMethod.WEIGHTED_CENTROID;

    // ==================== 参与专家信息（JSON字符串） ====================
    @Column(name = "participating_experts", columnDefinition = "TEXT")
    private String participatingExpertsJson;

    @Column(name = "expert_count")
    private Integer expertCount = 0;

    // ==================== 权重参数 ====================
    @Column(name = "weight_alpha", precision = 5, scale = 4)
    private BigDecimal weightAlpha = BigDecimal.valueOf(0.5);

    @Column(name = "weight_lambda", precision = 5, scale = 4)
    private BigDecimal weightLambda = BigDecimal.valueOf(0.5);

    // ==================== 统计信息 ====================
    @Column(name = "min_score", precision = 10, scale = 6)
    private BigDecimal minScore;

    @Column(name = "max_score", precision = 10, scale = 6)
    private BigDecimal maxScore;

    @Column(name = "avg_score", precision = 10, scale = 6)
    private BigDecimal avgScore;

    @Column(name = "std_dev", precision = 10, scale = 6)
    private BigDecimal stdDev;

    // ==================== 时间戳 ====================
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ==================== 枚举定义 ====================

    public enum AggregationMethod {
        WEIGHTED_CENTROID,  // 质心式加权平均（推荐）
        WEIGHTED_AVG,       // 简单加权平均
        MEAN,               // 算术平均
        GEOMETRIC_MEAN      // 几何平均
    }
}
