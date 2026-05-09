package com.ccnu.military.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "dynamic_template")
public class DynamicTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_name", nullable = false)
    private String templateName;

    @Column(name = "template_code", nullable = false, unique = true)
    private String templateCode;

    @Column(name = "description", columnDefinition = "TEXT")
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

    public enum Status {
        DRAFT,      // 草稿
        ACTIVE,     // 激活
        ARCHIVED    // 归档
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
