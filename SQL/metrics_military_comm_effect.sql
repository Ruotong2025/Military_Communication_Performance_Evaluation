-- ================================================================================
-- 军事通信效能指标计算SQL (修订版 v5)
-- 从以下四张 records 表计算指标：
--   - records_military_communication_info   (通信记录)
--   - records_link_maintenance_events      (链路维护事件)
--   - records_military_operation_info      (作战基础信息)
--   - records_security_events             (安全事件 - 包含密钥泄露等)
-- 目标表: metrics_military_comm_effect
-- ================================================================================

-- ============================================
-- 完整插入SQL（基于 operation_id）
-- ============================================

-- 先删除可能存在的旧数据
DELETE FROM metrics_military_comm_effect;

-- 插入计算后的数据
INSERT INTO metrics_military_comm_effect (
    evaluation_id,
    operation_id,

    -- ========== 安全性指标 ==========
    security_key_leakage_count,
    security_detected_probability,
    security_interception_resistance_probability,

    reliability_crash_count,
    reliability_recovery_time,
    reliability_communication_availability,

    -- ========== 传输指标 ==========
    transmission_bandwidth,
    transmission_call_setup_time,
    transmission_delay,
    transmission_bit_error_rate,
    transmission_throughput,
    transmission_spectral_efficiency,

    -- ========== 抗干扰指标 ==========
    anti_jamming_sinr,
    anti_jamming_margin,
    anti_jamming_communication_distance,

    -- ========== 效能指标 ==========
    effect_damage_rate,
    effect_mission_completion_rate,
    effect_blind_rate,

    -- ========== 元数据 ==========
    created_at,
    updated_at
)
SELECT
    -- evaluation_id
    CONCAT('EVAL-', op.operation_id) AS evaluation_id,
    op.operation_id AS operation_id,

    -- ========== 安全性指标 ==========
    -- 1. security_key_leakage_count: 密钥泄露次数（从 records_security_events 获取）
    COALESCE((
        SELECT COUNT(*)
        FROM records_security_events sec
        WHERE sec.operation_id = op.operation_id
          AND sec.event_type = 'key_leak'
    ), 0) AS security_key_leakage_count,

    -- 2. security_detected_probability: 被侦察概率 = 被检测次数 / 总通信次数 * 100
    COALESCE((
        SELECT
            CASE
                WHEN total_comm = 0 OR total_comm IS NULL THEN 0
                ELSE (detected_count * 100.0 / total_comm)
            END
        FROM (
            SELECT
                SUM(CASE WHEN detected = 1 THEN 1 ELSE 0 END) AS detected_count,
                COUNT(*) AS total_comm
            FROM records_military_communication_info
            WHERE operation_id = op.operation_id
        ) AS comm_stats
    ), 0) AS security_detected_probability,

    -- 3. security_interception_resistance_probability: 抗拦截能力 = intercepted=1 条数 ÷ 通信总条数 × 100
    COALESCE((
        SELECT
            CASE
                WHEN total_comm = 0 OR total_comm IS NULL THEN 0
                ELSE (intercepted_count * 100.0 / total_comm)
            END
        FROM (
            SELECT
                (SELECT COUNT(*) FROM records_military_communication_info WHERE operation_id = op.operation_id) AS total_comm,
                SUM(CASE WHEN intercepted = 1 THEN 1 ELSE 0 END) AS intercepted_count
            FROM records_military_communication_info
            WHERE operation_id = op.operation_id
        ) AS intercept_stats
    ), 0) AS security_interception_resistance_probability,

    -- ========== 可靠性指标 ==========
    -- 4. reliability_crash_count: 网络崩溃次数
    COALESCE((
        SELECT COUNT(*)
        FROM records_link_maintenance_events
        WHERE operation_id = op.operation_id
          AND interruption_type = 'crash'
    ), 0) AS reliability_crash_count,

    -- 5. reliability_recovery_time: 平均恢复时间（毫秒）
    COALESCE((
        SELECT AVG(recovery_duration_ms)
        FROM records_link_maintenance_events
        WHERE operation_id = op.operation_id
          AND recovery_success = 1
          AND recovery_duration_ms IS NOT NULL
    ), 0) AS reliability_recovery_time,

    -- 6. reliability_communication_availability: 通信可用性 = (总时长 - 中断时长) / 总时长 * 100
    COALESCE((
        SELECT
            CASE
                WHEN total_duration = 0 OR total_duration IS NULL THEN 100
                ELSE ((total_duration - COALESCE(interruption_duration, 0)) * 100.0 / total_duration)
            END
        FROM (
            SELECT
                (SELECT MAX(end_time_ms) - MIN(start_time_ms)
                 FROM records_military_communication_info
                 WHERE operation_id = op.operation_id) AS total_duration
        ) AS total_stats
        LEFT JOIN (
            SELECT
                SUM(interruption_end_ms - interruption_start_ms) AS interruption_duration
            FROM records_link_maintenance_events
            WHERE operation_id = op.operation_id
        ) AS interrupt_stats ON 1=1
    ), 100) AS reliability_communication_availability,

    -- ========== 传输指标 ==========
    -- 7. transmission_bandwidth: 平均带宽（Mbps）
    COALESCE((
        SELECT AVG(bandwidth_hz) / 1000000
        FROM records_military_communication_info
        WHERE operation_id = op.operation_id
          AND bandwidth_hz IS NOT NULL
    ), 0) AS transmission_bandwidth,

    -- 8. transmission_call_setup_time: 平均呼叫建立时间（毫秒）
    COALESCE((
        SELECT AVG(call_setup_ms)
        FROM records_military_communication_info
        WHERE operation_id = op.operation_id
          AND call_setup_ms IS NOT NULL
    ), 0) AS transmission_call_setup_time,

    -- 9. transmission_delay: 平均传输时延（毫秒）
    COALESCE((
        SELECT AVG(trans_delay_ms)
        FROM records_military_communication_info
        WHERE operation_id = op.operation_id
          AND trans_delay_ms IS NOT NULL
    ), 0) AS transmission_delay,

    -- 10. transmission_bit_error_rate: 平均误码率（%）
    COALESCE((
        SELECT AVG(error_bits * 1.0 / NULLIF(total_bits, 0)) * 100
        FROM records_military_communication_info
        WHERE operation_id = op.operation_id
          AND total_bits > 0
    ), 0) AS transmission_bit_error_rate,

    -- 11. transmission_throughput: 平均吞吐量（Mbps）
    COALESCE((
        SELECT AVG(throughput_bps) / 1000000
        FROM records_military_communication_info
        WHERE operation_id = op.operation_id
          AND throughput_bps IS NOT NULL
    ), 0) AS transmission_throughput,

    -- 12. transmission_spectral_efficiency: 频谱效率（bit/Hz）
    COALESCE((
        SELECT AVG(throughput_bps / NULLIF(bandwidth_hz, 0))
        FROM records_military_communication_info
        WHERE operation_id = op.operation_id
          AND bandwidth_hz > 0
          AND throughput_bps IS NOT NULL
    ), 0) AS transmission_spectral_efficiency,

    -- ========== 抗干扰指标 ==========
    -- 13. anti_jamming_sinr: 平均信干噪比（dB）
    COALESCE((
        SELECT AVG(sinr_db)
        FROM records_military_communication_info
        WHERE operation_id = op.operation_id
          AND sinr_db IS NOT NULL
    ), 0) AS anti_jamming_sinr,

    -- 14. anti_jamming_margin: 平均抗干扰余量（dB）
    COALESCE((
        SELECT AVG(jamming_margin_db)
        FROM records_military_communication_info
        WHERE operation_id = op.operation_id
          AND jamming_margin_db IS NOT NULL
    ), 0) AS anti_jamming_margin,

    -- 15. anti_jamming_communication_distance: 平均通信距离（km）
    COALESCE((
        SELECT AVG(distance_km)
        FROM records_military_communication_info
        WHERE operation_id = op.operation_id
          AND distance_km IS NOT NULL
    ), 0) AS anti_jamming_communication_distance,

    -- ========== 效能指标 ==========
    -- 16. effect_damage_rate: 毁伤率（%）= 受损装备数 / 总装备数 * 100
    COALESCE((
        SELECT
            CASE
                WHEN COALESCE(total_equipment_count, 0) = 0 THEN 0
                ELSE (COALESCE(damaged_equipment_count, 0) * 100.0 / total_equipment_count)
            END
        FROM records_military_operation_info
        WHERE operation_id = op.operation_id
    ), 0) AS effect_damage_rate,

    -- 17. effect_mission_completion_rate: 任务完成率 = 成功通信次数 / 总通信次数 * 100
    COALESCE((
        SELECT
            CASE
                WHEN total_comm = 0 OR total_comm IS NULL THEN 0
                ELSE (success_count * 100.0 / total_comm)
            END
        FROM (
            SELECT
                SUM(CASE WHEN comm_success = 1 THEN 1 ELSE 0 END) AS success_count,
                COUNT(*) AS total_comm
            FROM records_military_communication_info
            WHERE operation_id = op.operation_id
        ) AS mission_stats
    ), 0) AS effect_mission_completion_rate,

    -- 18. effect_blind_rate: 盲区率（%）= 孤立节点数 / 总节点数 * 100
    COALESCE((
        SELECT
            CASE
                WHEN COALESCE(total_node_count, 0) = 0 THEN 0
                ELSE (COALESCE(isolated_node_count, 0) * 100.0 / total_node_count)
            END
        FROM records_military_operation_info
        WHERE operation_id = op.operation_id
    ), 0) AS effect_blind_rate,

    -- ========== 元数据 ==========
    NOW() AS created_at,
    NOW() AS updated_at

FROM (
    -- 获取所有 operation_id（从四个表中合并）
    SELECT DISTINCT operation_id FROM records_military_communication_info
    UNION
    SELECT DISTINCT operation_id FROM records_link_maintenance_events
    UNION
    SELECT DISTINCT operation_id FROM records_military_operation_info
    UNION
    SELECT DISTINCT operation_id FROM records_security_events
) AS op;


-- ============================================
-- 查询验证结果
-- ============================================
SELECT
    id,
    evaluation_id,
    operation_id,

    -- 安全性
    security_key_leakage_count,
    security_detected_probability,
    security_interception_resistance_probability,

    -- 可靠性
    reliability_crash_count,
    reliability_recovery_time,
    reliability_communication_availability,

    -- 传输
    transmission_bandwidth,
    transmission_call_setup_time,
    transmission_delay,
    transmission_bit_error_rate,
    transmission_throughput,
    transmission_spectral_efficiency,

    -- 抗干扰
    anti_jamming_sinr,
    anti_jamming_margin,
    anti_jamming_communication_distance,

    -- 效能
    effect_damage_rate,
    effect_mission_completion_rate,
    effect_blind_rate,

    created_at,
    updated_at
FROM metrics_military_comm_effect
ORDER BY operation_id;


-- ============================================
-- 汇总统计（可选）
-- ============================================
SELECT
    '=== 指标汇总统计 ===' AS info,
    COUNT(*) AS total_operations,
    SUM(security_key_leakage_count) AS total_key_leaks,
    AVG(security_detected_probability) AS avg_detected_prob,
    AVG(security_interception_resistance_probability) AS avg_interception_resistance,
    AVG(reliability_crash_count) AS avg_crash_count,
    AVG(reliability_communication_availability) AS avg_availability,
    AVG(transmission_throughput) AS avg_throughput,
    AVG(anti_jamming_sinr) AS avg_sinr,
    AVG(effect_mission_completion_rate) AS avg_mission_completion
FROM metrics_military_comm_effect;
