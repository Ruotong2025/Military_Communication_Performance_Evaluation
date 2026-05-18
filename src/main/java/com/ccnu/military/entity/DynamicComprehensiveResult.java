package com.ccnu.military.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 动态综合评分结果实体
 * 存储每次综合评分计算的结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dynamic_comprehensive_result")
public class DynamicComprehensiveResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 批次ID
     */
    @Column(name = "batch_id", nullable = false, length = 64)
    private String batchId;

    /**
     * 作战ID
     */
    @Column(name = "operation_id", nullable = false, length = 64)
    private String operationId;

    /**
     * 模板ID
     */
    @Column(name = "template_id")
    private Long templateId;

    /**
     * 模板名称
     */
    @Column(name = "template_name", length = 255)
    private String templateName;

    /**
     * 综合得分（总得分）
     */
    @Column(name = "total_score", precision = 10, scale = 4)
    private BigDecimal totalScore;

    /**
     * 定性加权分
     */
    @Column(name = "qualitative_weighted_score", precision = 10, scale = 4)
    private BigDecimal qualitativeWeightedScore;

    /**
     * 定量加权分
     */
    @Column(name = "quantitative_weighted_score", precision = 10, scale = 4)
    private BigDecimal quantitativeWeightedScore;

    /**
     * 计算时间
     */
    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    /**
     * 层级得分JSON
     */
    @Column(name = "level_scores_json", columnDefinition = "TEXT")
    private String levelScoresJson;

    /**
     * 权重配置JSON
     */
    @Column(name = "weights_config_json", columnDefinition = "TEXT")
    private String weightsConfigJson;
}
