-- ========================================
-- 军事效能评估数据插入SQL
-- 从 communication_network_lifecycle 和 during_battle_communications 表中分析数据
-- 插入到 military_effectiveness_evaluation 表
-- ========================================

INSERT INTO military_effectiveness_evaluation (
    scenario_id,
    test_id,
    
    -- ========== 1. 响应能力指标 (2个) ==========
    avg_call_setup_duration_ms,
    avg_transmission_delay_ms,
    
    -- ========== 2. 处理能力指标 (4个) ==========
    effective_throughput,
    spectral_efficiency,
    channel_utilization,
    avg_concurrent_links,
    
    -- ========== 3. 有效性指标 (3个) ==========
    avg_communication_distance,
    avg_ber,
    avg_plr,
    
    -- ========== 4. 可靠性指标 (5个) ==========
    task_success_rate,
    communication_success_rate,
    communication_availability_rate,
    total_network_crashes,
    avg_response_time_ms,
    avg_handling_duration_ms,
    
    -- ========== 5. 抗干扰性指标 (3个) ==========
    avg_snr,
    avg_sinr,
    avg_jamming_margin,
    
    -- ========== 6. 人为操作能力指标 (2个) ==========
    avg_operator_reaction_time_ms,
    operation_success_rate,
    
    -- ========== 7. 组网能力指标 (3个) ==========
    avg_network_setup_duration_ms,
    avg_network_setup_speed,
    avg_connectivity_rate,
    
    -- ========== 8. 机动性指标 (2个) ==========
    deployment_speed,
    personnel_avg_deployment_speed,
    
    -- ========== 9. 安全性指标 (5个) ==========
    avg_key_age_hours,
    key_compromise_frequency,
    avg_key_leak_response_time_ms,
    key_security_index,
    key_response_efficiency,
    detection_probability,
    interception_resistance,
    
    -- ========== 统计信息 ==========
    total_communications,
    total_lifecycles,
    total_communication_duration_ms,
    total_interruption_time_ms
)
SELECT 
    -- 基础信息
    cnl.scenario_id,
    cnl.test_id,
    
    -- ========== 1. 响应能力指标 ==========
    -- 平均呼叫建立时长（毫秒）
    (SELECT AVG(call_setup_duration_ms) 
     FROM during_battle_communications dbc 
     WHERE dbc.test_id = cnl.test_id 
       AND dbc.communication_type = 'call' 
       AND dbc.call_setup_success = 1
    ) AS avg_call_setup_duration_ms,
    
    -- 平均传输时延（毫秒）
    (SELECT AVG(transmission_delay_ms) 
     FROM during_battle_communications dbc 
     WHERE dbc.test_id = cnl.test_id 
       AND dbc.transmission_delay_ms IS NOT NULL
    ) AS avg_transmission_delay_ms,
    
    -- ========== 2. 处理能力指标 ==========
    -- 有效吞吐量（bps）- 只统计成功通信的平均瞬时吞吐量
    (SELECT AVG(instant_throughput) 
     FROM during_battle_communications dbc 
     WHERE dbc.test_id = cnl.test_id 
       AND dbc.communication_success = 1
       AND dbc.instant_throughput > 0
    ) AS effective_throughput,
    
    -- 频谱效率（bps/Hz）= 平均吞吐量 / 平均带宽
    (SELECT AVG(instant_throughput / NULLIF(channel_bandwidth, 0))
     FROM during_battle_communications dbc 
     WHERE dbc.test_id = cnl.test_id 
       AND dbc.communication_success = 1
       AND dbc.channel_bandwidth > 0
       AND dbc.instant_throughput > 0
    ) AS spectral_efficiency,
    
    -- 信道利用率（%）= (实际吞吐量 / 理论容量) * 100
    -- 理论容量可以用香农公式估算：C = B * log2(1 + SNR)
    (SELECT AVG(
        (instant_throughput / NULLIF(channel_bandwidth * LOG2(1 + POWER(10, instant_snr/10)), 0)) * 100
     )
     FROM during_battle_communications dbc 
     WHERE dbc.test_id = cnl.test_id 
       AND dbc.communication_success = 1
       AND dbc.channel_bandwidth > 0
       AND dbc.instant_throughput > 0
       AND dbc.instant_snr IS NOT NULL
    ) AS channel_utilization,
    
    -- 平均并发链路数
    (SELECT AVG(concurrent_links_count) 
     FROM during_battle_communications dbc 
     WHERE dbc.test_id = cnl.test_id
       AND dbc.concurrent_links_count IS NOT NULL
    ) AS avg_concurrent_links,
    
    -- ========== 3. 有效性指标 ==========
    -- 平均通信距离（km）
    (SELECT AVG(communication_distance) 
     FROM during_battle_communications dbc 
     WHERE dbc.test_id = cnl.test_id
       AND dbc.communication_distance IS NOT NULL
    ) AS avg_communication_distance,
    
    -- 平均误码率（0-1）
    (SELECT AVG(instant_ber) 
     FROM during_battle_communications dbc 
     WHERE dbc.test_id = cnl.test_id
       AND dbc.instant_ber IS NOT NULL
    ) AS avg_ber,
    
    -- 平均丢包率（0-1）- 转换为0-1范围
    (SELECT AVG(instant_plr / 100.0) 
     FROM during_battle_communications dbc 
     WHERE dbc.test_id = cnl.test_id
       AND dbc.instant_plr IS NOT NULL
    ) AS avg_plr,
    
    -- ========== 4. 可靠性指标 ==========
    -- 任务成功率 = 成功的生命周期数 / 总生命周期数
    (SELECT SUM(CASE WHEN lifecycle_success = 1 THEN 1 ELSE 0 END) / COUNT(*)
     FROM communication_network_lifecycle cnl2
     WHERE cnl2.test_id = cnl.test_id
    ) AS task_success_rate,
    
    -- 通信成功率 = 成功通信次数 / 总通信次数
    (SELECT SUM(CASE WHEN communication_success = 1 THEN 1 ELSE 0 END) / COUNT(*)
     FROM during_battle_communications dbc 
     WHERE dbc.test_id = cnl.test_id
    ) AS communication_success_rate,
    
    -- 通信可用性 = MTBF / (MTBF + MTTR)
    -- MTBF = 总正常通信时长 / 崩溃次数
    -- MTTR = 总中断时长 / 崩溃次数
    (SELECT 
        CASE 
            WHEN SUM(CASE WHEN network_crash_occurred = 1 THEN 1 ELSE 0 END) = 0 THEN 1.0
            ELSE 
                (SUM(communication_duration_ms) / NULLIF(SUM(CASE WHEN network_crash_occurred = 1 THEN 1 ELSE 0 END), 0)) /
                (
                    (SUM(communication_duration_ms) / NULLIF(SUM(CASE WHEN network_crash_occurred = 1 THEN 1 ELSE 0 END), 0)) +
                    (SUM(COALESCE(total_interruption_duration_ms, 0)) / NULLIF(SUM(CASE WHEN network_crash_occurred = 1 THEN 1 ELSE 0 END), 0))
                )
        END
     FROM communication_network_lifecycle cnl2
     WHERE cnl2.test_id = cnl.test_id
    ) AS communication_availability_rate,
    
    -- 网络崩溃次数
    (SELECT SUM(CASE WHEN network_crash_occurred = 1 THEN 1 ELSE 0 END)
     FROM communication_network_lifecycle cnl2
     WHERE cnl2.test_id = cnl.test_id
    ) AS total_network_crashes,
    
    -- 平均响应时间（毫秒）= 应急处置开始时间 - 崩溃时间
    (SELECT AVG(emergency_start_ms - crash_time_ms)
     FROM communication_network_lifecycle cnl2
     WHERE cnl2.test_id = cnl.test_id
       AND cnl2.network_crash_occurred = 1
       AND cnl2.emergency_start_ms IS NOT NULL
       AND cnl2.crash_time_ms IS NOT NULL
    ) AS avg_response_time_ms,
    
    -- 平均处理时长（毫秒）= 应急处置时长
    (SELECT AVG(emergency_handling_duration_ms)
     FROM communication_network_lifecycle cnl2
     WHERE cnl2.test_id = cnl.test_id
       AND cnl2.network_crash_occurred = 1
       AND cnl2.emergency_handling_duration_ms IS NOT NULL
    ) AS avg_handling_duration_ms,
    
    -- ========== 5. 抗干扰性指标 ==========
    -- 平均信噪比（dB）
    (SELECT AVG(instant_snr) 
     FROM during_battle_communications dbc 
     WHERE dbc.test_id = cnl.test_id
       AND dbc.instant_snr IS NOT NULL
    ) AS avg_snr,
    
    -- 平均信干噪比（dB）
    (SELECT AVG(instant_sinr) 
     FROM during_battle_communications dbc 
     WHERE dbc.test_id = cnl.test_id
       AND dbc.instant_sinr IS NOT NULL
    ) AS avg_sinr,
    
    -- 平均抗干扰余量（dB）
    (SELECT AVG(jamming_margin) 
     FROM during_battle_communications dbc 
     WHERE dbc.test_id = cnl.test_id
       AND dbc.jamming_margin IS NOT NULL
    ) AS avg_jamming_margin,
    
    -- ========== 6. 人为操作能力指标 ==========
    -- 平均操作员反应时间（毫秒）
    (SELECT AVG(operator_reaction_time_ms) 
     FROM during_battle_communications dbc 
     WHERE dbc.test_id = cnl.test_id
       AND dbc.operator_reaction_time_ms IS NOT NULL
    ) AS avg_operator_reaction_time_ms,
    
    -- 操作成功率 = 成功操作次数 / 总操作次数
    (SELECT SUM(CASE WHEN operation_success = 1 THEN 1 ELSE 0 END) / COUNT(*)
     FROM during_battle_communications dbc 
     WHERE dbc.test_id = cnl.test_id
       AND dbc.operation_success IS NOT NULL
    ) AS operation_success_rate,
    
    -- ========== 7. 组网能力指标 ==========
    -- 平均组网时长（毫秒）
    (SELECT AVG(network_setup_duration_ms)
     FROM communication_network_lifecycle cnl2
     WHERE cnl2.test_id = cnl.test_id
       AND cnl2.network_setup_duration_ms IS NOT NULL
    ) AS avg_network_setup_duration_ms,
    
    -- 平均组网速度（节点/秒）
    (SELECT AVG(network_setup_speed)
     FROM communication_network_lifecycle cnl2
     WHERE cnl2.test_id = cnl.test_id
       AND cnl2.network_setup_speed IS NOT NULL
    ) AS avg_network_setup_speed,
    
    -- 平均连通率（0-1）- 转换为0-1范围
    (SELECT AVG(connectivity_rate / 100.0)
     FROM communication_network_lifecycle cnl2
     WHERE cnl2.test_id = cnl.test_id
       AND cnl2.connectivity_rate IS NOT NULL
    ) AS avg_connectivity_rate,
    
    -- ========== 8. 机动性指标 ==========
    -- 部署速度 = 节点数 / 部署时间（节点/分钟）
    -- 假设组网时长即为部署时间
    (SELECT AVG(network_scale / NULLIF(network_setup_duration_ms / 60000.0, 0))
     FROM communication_network_lifecycle cnl2
     WHERE cnl2.test_id = cnl.test_id
       AND cnl2.network_setup_duration_ms > 0
       AND cnl2.network_scale IS NOT NULL
    ) AS deployment_speed,
    
    -- 人均部署速度 = 节点数 / (部署时间 × 人数)（节点/人/分钟）
    -- 假设每个测试有固定的操作员数量，这里用不同操作员数量来计算
    (SELECT 
        AVG(cnl2.network_scale / NULLIF(cnl2.network_setup_duration_ms / 60000.0, 0)) / 
        NULLIF(COUNT(DISTINCT dbc.operator_id), 0)
     FROM communication_network_lifecycle cnl2
     LEFT JOIN during_battle_communications dbc ON dbc.test_id = cnl2.test_id
     WHERE cnl2.test_id = cnl.test_id
       AND cnl2.network_setup_duration_ms > 0
       AND cnl2.network_scale IS NOT NULL
     GROUP BY cnl2.test_id
    ) AS personnel_avg_deployment_speed,
    
    -- ========== 9. 安全性指标 ==========
    -- 平均密钥使用时长（小时）
    (SELECT AVG(key_age_ms / 3600000.0)
     FROM during_battle_communications dbc 
     WHERE dbc.test_id = cnl.test_id
       AND dbc.key_age_ms IS NOT NULL
    ) AS avg_key_age_hours,
    
    -- 密钥泄露频率（事件/小时）
    -- 假设 key_valid = 0 或 key_updated = 1 表示密钥泄露事件
    (SELECT 
        SUM(CASE WHEN key_valid = 0 OR key_updated = 1 THEN 1 ELSE 0 END) / 
        NULLIF(SUM(communication_duration_ms) / 3600000.0, 0)
     FROM during_battle_communications dbc 
     WHERE dbc.test_id = cnl.test_id
    ) AS key_compromise_frequency,
    
    -- 平均密钥泄露响应时间（毫秒）
    -- 这里用加密开销作为响应时间的近似
    (SELECT AVG(encryption_overhead_ms)
     FROM during_battle_communications dbc 
     WHERE dbc.test_id = cnl.test_id
       AND dbc.encryption_overhead_ms IS NOT NULL
       AND (dbc.key_valid = 0 OR dbc.key_updated = 1)
    ) AS avg_key_leak_response_time_ms,
    
    -- 密钥安全指数（0-1）
    -- 综合考虑：密钥有效性、更新频率、使用时长
    (SELECT 
        (
            SUM(CASE WHEN key_valid = 1 THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0) * 0.5 +
            (1 - LEAST(AVG(key_age_ms) / 86400000.0, 1)) * 0.3 +
            (SUM(CASE WHEN key_updated = 1 THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0)) * 0.2
        )
     FROM during_battle_communications dbc 
     WHERE dbc.test_id = cnl.test_id
       AND dbc.encryption_used = 1
    ) AS key_security_index,
    
    -- 密钥响应效率（0-1）- 响应时间越短效率越高
    (SELECT 
        1 - LEAST(AVG(encryption_overhead_ms) / 10000.0, 1)
     FROM during_battle_communications dbc 
     WHERE dbc.test_id = cnl.test_id
       AND dbc.encryption_overhead_ms IS NOT NULL
    ) AS key_response_efficiency,
    
    -- 被侦察概率 = 被侦察次数 / 总任务次数
    (SELECT SUM(CASE WHEN detected = 1 THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0)
     FROM during_battle_communications dbc 
     WHERE dbc.test_id = cnl.test_id
    ) AS detection_probability,
    
    -- 抗拦截能力（%）= (1 - 成功拦截/拦截尝试) × 100
    (SELECT 
        (1 - 
            NULLIF(SUM(CASE WHEN intercepted = 1 THEN 1 ELSE 0 END), 0) / 
            NULLIF(SUM(CASE WHEN interception_attempted = 1 THEN 1 ELSE 0 END), 0)
        ) * 100
     FROM during_battle_communications dbc 
     WHERE dbc.test_id = cnl.test_id
       AND dbc.interception_attempted = 1
    ) AS interception_resistance,
    
    -- ========== 统计信息 ==========
    -- 总通信次数
    (SELECT COUNT(*)
     FROM during_battle_communications dbc 
     WHERE dbc.test_id = cnl.test_id
    ) AS total_communications,
    
    -- 总生命周期数
    (SELECT COUNT(*)
     FROM communication_network_lifecycle cnl2
     WHERE cnl2.test_id = cnl.test_id
    ) AS total_lifecycles,
    
    -- 总通信时长（毫秒）
    (SELECT SUM(communication_duration_ms)
     FROM communication_network_lifecycle cnl2
     WHERE cnl2.test_id = cnl.test_id
       AND cnl2.communication_duration_ms IS NOT NULL
    ) AS total_communication_duration_ms,
    
    -- 总中断时长（毫秒）
    (SELECT SUM(COALESCE(total_interruption_duration_ms, 0))
     FROM communication_network_lifecycle cnl2
     WHERE cnl2.test_id = cnl.test_id
    ) AS total_interruption_time_ms

FROM (
    -- 获取每个 test_id 的唯一记录
    SELECT DISTINCT scenario_id, test_id
    FROM communication_network_lifecycle
) AS cnl

ORDER BY cnl.test_id;
