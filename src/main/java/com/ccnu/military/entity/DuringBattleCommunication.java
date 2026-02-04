package com.ccnu.military.entity;

import javax.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 战中通信记录实体类
 * 对应表: during_battle_communications
 */
@Data
@Entity
@Table(name = "during_battle_communications")
public class DuringBattleCommunication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "communication_id")
    private Long communicationId;

    @Column(name = "scenario_id", nullable = false)
    private Integer scenarioId;

    @Column(name = "test_id", nullable = false, length = 50)
    private String testId;

    @Column(name = "source_node_id", length = 50)
    private String sourceNodeId;

    @Column(name = "target_node_id", length = 50)
    private String targetNodeId;

    @Column(name = "communication_type", length = 20)
    private String communicationType;

    @Column(name = "communication_start_time_ms")
    private Long communicationStartTimeMs;

    @Column(name = "communication_end_time_ms")
    private Long communicationEndTimeMs;

    @Column(name = "communication_duration_ms")
    private Long communicationDurationMs;

    @Column(name = "call_setup_duration_ms", precision = 12, scale = 3)
    private BigDecimal callSetupDurationMs;

    @Column(name = "call_setup_success")
    private Boolean callSetupSuccess;

    @Column(name = "transmission_delay_ms", precision = 12, scale = 3)
    private BigDecimal transmissionDelayMs;

    @Column(name = "channel_bandwidth", precision = 15, scale = 2)
    private BigDecimal channelBandwidth;

    @Column(name = "instant_snr", precision = 8, scale = 2)
    private BigDecimal instantSnr;

    @Column(name = "instant_throughput", precision = 15, scale = 2)
    private BigDecimal instantThroughput;

    @Column(name = "concurrent_links_count")
    private Integer concurrentLinksCount;

    @Column(name = "communication_distance", precision = 10, scale = 2)
    private BigDecimal communicationDistance;

    @Column(name = "instant_ber", precision = 15, scale = 10)
    private BigDecimal instantBer;

    @Column(name = "instant_plr", precision = 8, scale = 5)
    private BigDecimal instantPlr;

    @Column(name = "communication_success")
    private Boolean communicationSuccess;

    @Column(name = "failure_reason", length = 200)
    private String failureReason;

    @Column(name = "instant_sinr", precision = 8, scale = 2)
    private BigDecimal instantSinr;

    @Column(name = "jamming_margin", precision = 8, scale = 2)
    private BigDecimal jammingMargin;

    @Column(name = "operator_reaction_time_ms", precision = 10, scale = 2)
    private BigDecimal operatorReactionTimeMs;

    @Column(name = "operation_success")
    private Boolean operationSuccess;

    @Column(name = "operation_error")
    private Boolean operationError;

    @Column(name = "encryption_used")
    private Boolean encryptionUsed;

    @Column(name = "encryption_algorithm", length = 50)
    private String encryptionAlgorithm;

    @Column(name = "key_age_ms")
    private Long keyAgeMs;

    @Column(name = "key_updated")
    private Boolean keyUpdated;

    @Column(name = "detected")
    private Boolean detected;

    @Column(name = "intercepted")
    private Boolean intercepted;

    @Column(name = "interception_attempted")
    private Boolean interceptionAttempted;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
