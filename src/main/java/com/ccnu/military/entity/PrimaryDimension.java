package com.ccnu.military.entity;

import javax.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "mtl_primary_dimension")
public class PrimaryDimension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level_id", nullable = false)
    private LevelDefinition level;

    @Column(name = "dimension_name", nullable = false)
    private String dimensionName;

    @Column(name = "dimension_code", nullable = false)
    private String dimensionCode;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "weight", precision = 10, scale = 6)
    private BigDecimal weight = BigDecimal.ZERO;

    @OneToMany(mappedBy = "primaryDimension", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SecondaryDimension> secondaryDimensions = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
