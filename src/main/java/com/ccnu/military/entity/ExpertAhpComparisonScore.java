package com.ccnu.military.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 专家 AHP 两两比较原始打分（持久化）
 */
@Data
@Entity
@Table(name = "expert_ahp_comparison_score")
public class ExpertAhpComparisonScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expert_id", nullable = false)
    private Long expertId;

    @Column(name = "expert_name", length = 100)
    private String expertName;

    @Column(name = "comparison_key", nullable = false, length = 200)
    private String comparisonKey;

    @Column(name = "score", nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "confidence", precision = 3, scale = 2)
    private BigDecimal confidence;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "create_time")
    private LocalDateTime createTime;
}
