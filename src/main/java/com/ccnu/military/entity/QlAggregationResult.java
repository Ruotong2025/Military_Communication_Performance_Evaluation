package com.ccnu.military.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 专家定性指标集结结果实体
 * 存储每次集结计算后的群体质心分 x* 及映射等级
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ql_aggregation_result")
public class QlAggregationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "evaluation_batch_id", nullable = false, length = 50)
    private String evaluationBatchId;

    @Column(name = "operation_id", nullable = false, length = 50)
    private String operationId;

    @Column(name = "indicator_key", nullable = false, length = 50)
    private String indicatorKey;

    @Column(name = "indicator_name", nullable = false, length = 100)
    private String indicatorName;

    @Column(name = "x_star", precision = 10, scale = 4)
    private BigDecimal xStar;

    @Column(name = "mapped_grade", length = 10)
    private String mappedGrade;

    @Column(name = "denominator", precision = 14, scale = 6)
    private BigDecimal denominator;

    @Column(name = "expert_count", nullable = false)
    private Integer expertCount;

    @Column(name = "expert_ids", columnDefinition = "JSON")
    private String expertIds;

    @Column(name = "weight_snapshot", columnDefinition = "JSON")
    private String weightSnapshot;

    @Column(name = "warnings", columnDefinition = "JSON")
    private String warnings;

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
}
