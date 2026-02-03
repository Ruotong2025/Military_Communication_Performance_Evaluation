CREATE TABLE IF NOT EXISTS military_effectiveness_evaluation (
    -- 主键
    evaluation_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评估ID',
    scenario_id INT NOT NULL COMMENT '场景ID',
    test_id VARCHAR(50) NOT NULL UNIQUE COMMENT '测试批次ID',

    -- ========== 1. 响应能力指标 (2个) ==========
    avg_call_setup_duration_ms DECIMAL(12,3) COMMENT '平均呼叫建立时长（毫秒）',
    avg_transmission_delay_ms DECIMAL(12,3) COMMENT '平均传输时延（毫秒）',

    -- ========== 2. 处理能力指标 (4个) ==========
    effective_throughput DECIMAL(15,2) COMMENT '有效吞吐量（bps，剔除重传与协议开销）:系统在观测时间内统计得到的实际传输速率',
    spectral_efficiency DECIMAL(8,4) COMMENT '频谱效率（bps/Hz）:同样 1 Hz 的频谱，谁能传更多数据，谁的频谱效率就高',
    channel_utilization DECIMAL(5,2) COMMENT '信道利用率 = 实际吞吐/理论容量（%）',
    avg_concurrent_links DECIMAL(8,2) COMMENT '平均并发链路数',

    -- ========== 3. 有效性指标 (5个) ==========
    avg_communication_distance DECIMAL(10,2) COMMENT '平均通信距离（km）',
    avg_ber DECIMAL(15,10) COMMENT '平均误码率（0-1）',
    avg_plr DECIMAL(8,6) COMMENT '平均丢包率（0-1）',

    -- ========== 4. 可靠性指标 (5个) ==========
    -- 从每次通信的角度考虑
    task_success_rate DECIMAL(8,6) COMMENT '任务成功率（0-1）',
    communication_availability_rate DECIMAL(8,6) COMMENT '通信可用性（0-1）MTBF/(MTBF+MTTR)',
    -- 从整个网络奔溃的角度考虑
    total_network_crashes INT COMMENT '网络崩溃次数',
    avg_response_time_ms BIGINT COMMENT '平均响应时间（毫秒，从崩溃发生到开始处理）',
    avg_handling_duration_ms BIGINT COMMENT '平均处理时长（毫秒，从响应开始到恢复成功）',

    -- ========== 5. 抗干扰性指标 (3个) ==========
    avg_snr DECIMAL(8,2) COMMENT '平均信噪比（dB）',
    avg_sinr DECIMAL(8,2) COMMENT '平均信干噪比（dB）',
    avg_jamming_margin DECIMAL(8,2) COMMENT '平均抗干扰余量（dB）',

    -- ========== 6. 人为操作能力指标 (2个) ==========
    avg_operator_reaction_time_ms DECIMAL(10,2) COMMENT '平均操作员反应时间（毫秒）',
    operation_success_rate DECIMAL(8,6) COMMENT '操作成功率（0-1）',

    -- ========== 7. 组网能力指标 (3个) ==========
    avg_network_setup_duration_ms BIGINT COMMENT '平均组网时长（毫秒）',
    avg_network_setup_speed DECIMAL(8,2) COMMENT '平均组网速度（节点/秒）',
    avg_connectivity_rate DECIMAL(8,6) COMMENT '平均连通率（0-1）',

    -- ========== 8. 机动性指标 (2个) ==========
    deployment_speed DECIMAL(8,2) COMMENT '部署速度 = 节点数/部署时间（节点/分钟）',
    personnel_avg_deployment_speed DECIMAL(8,2) COMMENT '人均部署速度 = 节点数/(部署时间×人数)（节点/人/分钟）',

    -- ========== 9. 安全性指标 (5个) ==========
    avg_key_age_hours DECIMAL(10,2) COMMENT '平均密钥使用时长（小时）',
    key_compromise_frequency DECIMAL(10,4) COMMENT '密钥泄露频率（事件/小时或事件/天，越低越安全）',
    avg_key_leak_response_time_ms DECIMAL(10,2) COMMENT '当密钥被检测泄露或可能被破解时，系统从发现到采取有效响应（如撤销或更新密钥）的平均时间',
    key_security_index DECIMAL(8,6) COMMENT '密钥安全指数（0-1，综合考虑使用时长、泄露频率和响应时间，越大越安全）',
    key_response_efficiency DECIMAL(8,6) COMMENT '密钥响应效率归一化（0-1，越大越快）',
    detection_probability DECIMAL(5,4) COMMENT '被侦察概率 = 被侦察次数/总任务次数',
    interception_resistance DECIMAL(5,2) COMMENT '抗拦截能力 = (1 - 成功拦截/拦截尝试) × 100（%）',

    -- ========== 统计信息 ==========
    total_communications INT COMMENT '总通信次数',
    total_lifecycles INT COMMENT '总生命周期数',
    total_communication_duration_ms BIGINT COMMENT '总通信时长（毫秒）',
    total_interruption_time_ms BIGINT COMMENT '总中断时长（毫秒）',

    -- ========== 元数据 ==========
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- ========== 索引 ==========
    INDEX idx_scenario (scenario_id),
    INDEX idx_test_id (test_id)
) COMMENT='军事效能评估基础指标表';
