package com.ccnu.military.entity;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "mtl_indicator_template")
public class IndicatorTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_name", nullable = false)
    private String templateName;

    @Column(name = "template_code", unique = true, nullable = false)
    private String templateCode;

    @Column(name = "description")
    private String description;

    @Column(name = "level_count")
    private Integer levelCount = 0;

    @Column(name = "primary_count")
    private Integer primaryCount = 0;

    @Column(name = "secondary_count")
    private Integer secondaryCount = 0;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status = Status.DRAFT;

    @Column(name = "source_file")
    private String sourceFile;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LevelDefinition> levels = new ArrayList<>();

    public enum Status {
        DRAFT, ACTIVE, ARCHIVED
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
