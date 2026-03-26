-- ================================================================================
-- 军事通信效能指标计算SQL
-- 从 records_link_maintenance_events, records_military_communication_info,
--    records_military_operation_info 计算 metrics_military_comm_effect
-- ================================================================================

-- ============================================
-- 完整插入/更新SQL（基于 operation_id）
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
    -- ========== 可靠性指标 ==========
    reliability_crash_rate,
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
    created_at,
    updated_at
)
SELECT
    -- evaluation_id: 使用 operation_id 作为评估ID
    CONCAT('EVAL-', op.operation_id) AS evaluation_id,
    op.operation_id AS operation_id,

    -- ========== 安全性指标 ==========
    -- 1. security_key_leakage_count: 密钥泄露次数（从被检测/拦截记录统计）
    COALESCE((
        SELECT COUNT(*)
        FROM records_military_communication_info comm
        WHERE comm.operation_id = op.operation_id
        AND (comm.detected = 1 OR comm.intercepted = 1)
    ), 0) AS security_key_leakage_count,

    -- 2. security_detected_probability: 被侦察概率 = 被检测次数 / 总通信次数
    COALESCE((
        SELECT COUNT(*) / NULLIF(COUNT(*), 0)
        FROM records_military_communication_info comm
        WHERE comm.operation_id = op.operation_id
        AND comm.detected = 1
    ), 0) AS security_detected_probability,

    -- 3. security_interception_resistance_probability: 抗拦截能力 = 1 - 拦截成功率
    COALESCE((
        SELECT 1 - (
            SUM(CASE WHEN intercepted = 1 THEN 1 ELSE 0 END) * 1.0 /
            NULLIF(SUM(CASE WHEN intercepted IS NOT NULL THEN 1 ELSE 0 END), 0)
        )
        FROM records_military_communication_info comm
        WHERE comm.operation_id = op.operation_id
    ), 1.0) AS security_interception_resistance_probability,

    -- ========== 可靠性指标 ==========
    -- 4. reliability_crash_rate: 网络崩溃率 = 崩溃次数 / 总通信次数
    COALESCE((
        SELECT COUNT(*) * 1.0 /
               NULLIF((SELECT COUNT(*) FROM records_military_communication_info WHERE operation_id = op.operation_id), 0)
        FROM records_link_maintenance_events evt
        WHERE evt.operation_id = op.operation_id
        AND evt.interruption_type = 'crash'
    ), 0) AS reliability_crash_rate,

    -- 5. reliability_recovery_time: 平均恢复时间（毫秒）
    COALESCE((
        SELECT AVG(recovery_duration_ms)
        FROM records_link_maintenance_events
        WHERE operation_id = op.operation_id
        AND recovery_success = 1
    ), 0) AS reliability_recovery_time,

    -- 6. reliability_communication_availability: 通信可用性 = 1 - 中断时长占比
    COALESCE((
        SELECT
            1 - (
                SUM(interruption_end_ms - interruption_start_ms) * 1.0 /
                NULLIF((
                    SELECT MAX(end_time_ms) - MIN(start_time_ms)
                    FROM records_military_communication_info
                    WHERE operation_id = op.operation_id
                ), 0)
            )
        FROM records_link_maintenance_events
        WHERE operation_id = op.operation_id
    ), 1.0) AS reliability_communication_availability,

    -- ========== 传输指标 ==========
    -- 7. transmission_bandwidth: 平均带宽
    COALESCE((
        SELECT AVG(bandwidth_hz)
        FROM records_military_communication_info
        WHERE operation_id = op.operation_id
    ), 0) AS transmission_bandwidth,

    -- 8. transmission_call_setup_time: 平均呼叫建立时间
    COALESCE((
        SELECT AVG(call_setup_ms)
        FROM records_military_communication_info
        WHERE operation_id = op.operation_id
    ), 0) AS transmission_call_setup_time,

    -- 9. transmission_delay: 平均传输时延
    COALESCE((
        SELECT AVG(trans_delay_ms)
        FROM records_military_communication_info
        WHERE operation_id = op.operation_id
    ), 0) AS transmission_delay,

    -- 10. transmission_bit_error_rate: 平均误码率
    COALESCE((
        SELECT AVG(error_bits * 1.0 / NULLIF(total_bits, 0))
        FROM records_military_communication_info
        WHERE operation_id = op.operation_id
    ), 0) AS transmission_bit_error_rate,

    -- 11. transmission_throughput: 平均吞吐量
    COALESCE((
        SELECT AVG(throughput_bps)
        FROM records_military_communication_info
        WHERE operation_id = op.operation_id
    ), 0) AS transmission_throughput,

    -- 12. transmission_spectral_efficiency: 频谱效率 = 吞吐量 / 带宽
    COALESCE((
        SELECT AVG(throughput_bps / NULLIF(bandwidth_hz, 0))
        FROM records_military_communication_info
        WHERE operation_id = op.operation_id
        AND bandwidth_hz > 0
    ), 0) AS transmission_spectral_efficiency,

    -- ========== 抗干扰指标 ==========
    -- 13. anti_jamming_sinr: 平均信干噪比
    COALESCE((
        SELECT AVG(sinr_db)
        FROM records_military_communication_info
        WHERE operation_id = op.operation_id
    ), 0) AS anti_jamming_sinr,

    -- 14. anti_jamming_margin: 平均抗干扰余量
    COALESCE((
        SELECT AVG(jamming_margin_db)
        FROM records_military_communication_info
        WHERE operation_id = op.operation_id
    ), 0) AS anti_jamming_margin,

    -- 15. anti_jamming_communication_distance: 平均通信距离
    COALESCE((
        SELECT AVG(distance_km)
        FROM records_military_communication_info
        WHERE operation_id = op.operation_id
    ), 0) AS anti_jamming_communication_distance,

    -- ========== 效能指标 ==========
    -- 16. effect_damage_rate: 毁伤率（从作战信息获取，或从通信拦截成功率反推）
    -- 假设毁伤率与拦截抗性相关
    COALESCE((
        SELECT 1 - (SELECT COALESCE(security_interception_resistance_probability, 1))
    ), 0.1) AS effect_damage_rate,

    -- 17. effect_mission_completion_rate: 任务完成率 = 成功通信次数 / 总通信次数
    COALESCE((
        SELECT SUM(CASE WHEN comm_success = 1 THEN 1 ELSE 0 END) * 1.0 /
               NULLIF(COUNT(*), 0)
        FROM records_military_communication_info
        WHERE operation_id = op.operation_id
    ), 0) AS effect_mission_completion_rate,

    -- 18. effect_blind_rate: 盲区率（通信盲区比例）
    COALESCE((
        SELECT 1 - (
            SELECT COUNT(DISTINCT src_node_id, dst_node_id) * 1.0 /
            NULLIF((SELECT theoretical_connections FROM records_military_operation_info WHERE operation_id = op.operation_id LIMIT 1), 0)
        )
        FROM records_military_communication_info
        WHERE operation_id = op.operation_id
    ), 0) AS effect_blind_rate,

    -- 时间戳
    NOW() AS created_at,
    NOW() AS updated_at

FROM (
    -- 获取所有 operation_id（从三个表中合并）
    SELECT DISTINCT operation_id FROM records_military_communication_info
    UNION
    SELECT DISTINCT operation_id FROM records_link_maintenance_events
    UNION
    SELECT DISTINCT operation_id FROM records_military_operation_info
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
    reliability_crash_rate,
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
    created_at
FROM metrics_military_comm_effect;
