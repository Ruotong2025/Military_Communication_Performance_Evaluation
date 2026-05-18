package com.ccnu.military.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "dynamic_cost_batch")
public class DynamicCostBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "batch_code", nullable = false)
    private String batchCode;

    @Column(name = "batch_name")
    private String batchName;

    @Column(name = "simulation_count")
    private Integer simulationCount = 1000;

    @Column(name = "random_seed")
    private Long randomSeed;

    @Column(name = "effectiveness_score", precision = 10, scale = 4)
    private BigDecimal effectivenessScore;

    @Column(name = "cost_score", precision = 10, scale = 4)
    private BigDecimal costScore;

    @Column(name = "effectiveness_cost_ratio", precision = 10, scale = 4)
    private BigDecimal effectivenessCostRatio;

    @Column(name = "result_summary", columnDefinition = "JSON")
    private String resultSummary;

    @Column(name = "status")
    private Integer status = 1;

    @Column(name = "created_time")
    private LocalDateTime createdTime;

    @PrePersist
    public void prePersist() {
        this.createdTime = LocalDateTime.now();
    }
}
