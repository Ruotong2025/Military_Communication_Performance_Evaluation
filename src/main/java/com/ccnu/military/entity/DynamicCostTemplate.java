package com.ccnu.military.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "dynamic_cost_template")
public class DynamicCostTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_name", nullable = false)
    private String templateName;

    @Column(name = "template_code", nullable = false, unique = true)
    private String templateCode;

    @Column(name = "cost_config", columnDefinition = "JSON")
    private String costConfig;

    @Column(name = "total_cost_count")
    private Integer totalCostCount = 0;

    @Column(name = "status")
    private Integer status = 1;

    @Column(name = "created_time")
    private LocalDateTime createdTime;

    @Column(name = "updated_time")
    private LocalDateTime updatedTime;

    @PrePersist
    public void prePersist() {
        this.createdTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedTime = LocalDateTime.now();
    }
}
