package com.ccnu.military.entity;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * AI识别历史记录实体
 */
@Data
@Entity
@Table(name = "indicator_ai_log")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndicatorAiLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "indicator_names", columnDefinition = "text", nullable = false)
    private String indicatorNames;

    @Column(name = "request_payload", columnDefinition = "text")
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "text")
    private String responsePayload;

    @Column(name = "identified_results", columnDefinition = "text")
    private String identifiedResults;

    @Column(name = "is_success")
    private Boolean isSuccess = true;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "processing_time_ms")
    private Integer processingTimeMs;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
