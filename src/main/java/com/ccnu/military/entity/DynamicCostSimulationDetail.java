package com.ccnu.military.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "dynamic_cost_simulation_detail")
public class DynamicCostSimulationDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "iteration_number", nullable = false)
    private Integer iterationNumber;

    @Column(name = "indicator_name", nullable = false)
    private String indicatorName;

    @Column(name = "indicator_code")
    private String indicatorCode;

    @Column(name = "level1_name")
    private String level1Name;

    @Column(name = "level2_name")
    private String level2Name;

    @Column(name = "simulated_value", precision = 15, scale = 4)
    private BigDecimal simulatedValue;

    @Column(name = "normalized_value", precision = 10, scale = 4)
    private BigDecimal normalizedValue;

    @Column(name = "weight", precision = 8, scale = 4)
    private BigDecimal weight;
}
