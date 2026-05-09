package com.ccnu.military.entity;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 动态AHP判断矩阵记录实体
 * 存储专家对动态指标体系的AHP判断矩阵打分
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "dynamic_ahp_matrix")
public class DynamicAhpMatrix {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 专家ID（关联专家可信度库）
     */
    @Column(name = "expert_id", nullable = false)
    private Long expertId;

    /**
     * 专家名称
     */
    @Column(name = "expert_name", length = 100)
    private String expertName;

    /**
     * 指标模板ID
     */
    @Column(name = "template_id", nullable = false)
    private Long templateId;

    /**
     * 模板名称
     */
    @Column(name = "template_name", length = 200)
    private String templateName;

    /**
     * 层级名称（如"战术级"、"战役级"等）
     */
    @Column(name = "level_name", length = 100)
    private String levelName;

    /**
     * 比较类型：LEVEL间比较、PRIMARY间比较（一级维度间）、SECONDARY间比较（二级维度间）
     */
    @Column(name = "comparison_type", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private ComparisonType comparisonType;

    /**
     * 父级维度编码（用于指标层矩阵）
     * - 层级间比较时：父级为一级维度
     * - 一级维度间比较时：父级为一级维度
     * - 二级维度间比较时：父级为一级维度
     */
    @Column(name = "parent_code", length = 100)
    private String parentCode;

    /**
     * 比较的维度编码（逗号分隔的多个编码）
     * 如："D1,D2" 表示D1与D2的比较
     */
    @Column(name = "dimension_codes", length = 500)
    private String dimensionCodes;

    /**
     * 比较的维度名称（逗号分隔的多个名称）
     */
    @Column(name = "dimension_names", length = 1000)
    private String dimensionNames;

    /**
     * 比较的维度数量
     */
    @Column(name = "dimension_count")
    private Integer dimensionCount;

    /**
     * 比较标度值（行/列格式）
     * JSON格式：[[1,3,5],[0.33,1,2],[0.2,0.5,1]]
     */
    @Column(name = "matrix_values", columnDefinition = "TEXT")
    private String matrixValues;

    /**
     * 判断标度值（上三角区域的标度）
     */
    @Column(name = "score", precision = 10, scale = 4)
    private BigDecimal score;

    /**
     * 判断可信度（把握度）0-1
     */
    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    /**
     * 行维度编码
     */
    @Column(name = "row_code", length = 100)
    private String rowCode;

    /**
     * 行维度名称
     */
    @Column(name = "row_name", length = 200)
    private String rowName;

    /**
     * 列维度编码
     */
    @Column(name = "col_code", length = 100)
    private String colCode;

    /**
     * 列维度名称
     */
    @Column(name = "col_name", length = 200)
    private String colName;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
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

    public enum ComparisonType {
        /**
         * 层级间比较（如：战术级 vs 战役级）
         */
        LEVEL_BETWEEN,

        /**
         * 同一层级内一级维度间比较
         */
        PRIMARY_BETWEEN,

        /**
         * 同一一级维度下二级指标间比较
         */
        SECONDARY_BETWEEN,

        /**
         * 一级维度与二级指标的跨层比较
         */
        CROSS_LEVEL
    }
}
