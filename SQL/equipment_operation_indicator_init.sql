-- ============================================================
-- 装备操作评估 - 指标配置数据初始化
-- 创建时间: 2026-04-03
-- 说明: 定量指标10条 + 定性指标6条
-- ============================================================

SET NAMES utf8mb4;

DELETE FROM `equipment_qt_indicator_def`;
DELETE FROM `equipment_ql_indicator_def`;

-- ============================================================
-- 定量指标配置（10条）
-- ============================================================
INSERT INTO `equipment_qt_indicator_def` (
  `indicator_key`, `indicator_name`, `indicator_name_en`, `phase`, `dimension`,
  `description`, `source_table`, `source_field`, `aggregation_method`,
  `custom_sql_template`, `unit`, `score_direction`, `display_order`, `enabled`
) VALUES
-- Q1 组网时长
('eqt_network_setup_time', '组网时长', 'Network Setup Time', 'pre_war', '系统性能',
 '战前系统开设阶段，从指令发出到所有节点就绪的平均时长',
 'records_military_operation_info', 'avg_network_setup_time_ms', 'avg',
 NULL, 'ms', 'negative', 1, 1),

-- Q2 应急处理
('eqt_emergency_handling', '应急处理', 'Emergency Response', 'mid_war', '操作响应',
 '作战过程中，操作员感知到异常并做出反应的平均时间',
 'records_military_communication_info', 'operator_reaction_ms', 'avg',
 NULL, 'ms', 'negative', 2, 1),

-- Q3 链路维持
('eqt_link_maintenance', '链路维持', 'Link Maintenance', 'mid_war', '通信保障操作',
 '通信链路维持能力 = (总通信时长 - 总中断时长) / 总通信时长 × 100%',
 NULL, NULL, 'custom',
 'SELECT ((SELECT MAX(end_time_ms)-MIN(start_time_ms) FROM records_military_communication_info WHERE operation_id = {operation_id}) - COALESCE((SELECT SUM(interruption_end_ms-interruption_start_ms) FROM records_link_maintenance_events WHERE operation_id = {operation_id}), 0)) * 100.0 / NULLIF((SELECT MAX(end_time_ms)-MIN(start_time_ms) FROM records_military_communication_info WHERE operation_id = {operation_id}), 0)',
 '%', 'positive', 3, 1),

-- Q4 业务开通
('eqt_service_activation', '业务开通', 'Service Activation', 'mid_war', '通信保障操作',
 '从发起通信请求到通信建立完成的平均时长',
 'records_military_communication_info', 'call_setup_ms', 'avg',
 NULL, 'ms', 'negative', 4, 1),

-- Q5 应急抢通
('eqt_emergency_restoration', '应急抢通', 'Emergency Restoration', 'mid_war', '通信保障操作',
 '链路中断后启动应急手段并恢复通信的平均耗时（仅统计成功的抢通）',
 'records_link_maintenance_events', 'recovery_duration_ms', 'avg_conditional',
 'recovery_success = 1 AND recovery_duration_ms IS NOT NULL', 'ms', 'negative', 5, 1),

-- Q6 连通率
('eqt_connectivity_rate', '连通率', 'Connectivity Rate', 'mid_war', '系统性能',
 '实际连通节点数 / 总节点数 × 100%',
 'records_military_operation_info', NULL, 'custom',
 'SELECT (total_node_count - isolated_node_count) * 100.0 / NULLIF(total_node_count, 0) FROM records_military_operation_info WHERE operation_id = {operation_id}',
 '%', 'positive', 6, 1),

-- Q7 任务可靠度
('eqt_mission_reliability', '任务可靠度', 'Mission Reliability', 'mid_war', '系统性能',
 '通信成功次数 / 总通信次数 × 100%',
 'records_military_communication_info', 'comm_success', 'percentage',
 NULL, '%', 'positive', 7, 1),

-- Q8 返修率
('eqt_rework_rate', '返修率', 'Rework Rate', 'post_war', '维修与反馈',
 '维修失败次数 / 需要维修的总次数 × 100%',
 'records_link_maintenance_events', NULL, 'custom',
 'SELECT COUNT(CASE WHEN maintenance_success = 0 AND maintenance_required = 1 THEN 1 END) * 100.0 / NULLIF(COUNT(CASE WHEN maintenance_required = 1 THEN 1 END), 0) FROM records_link_maintenance_events WHERE operation_id = {operation_id}',
 '%', 'negative', 8, 1),

-- Q9 干扰目标锁定耗时
('eqt_jamming_target_time', '干扰目标锁定耗时', 'Jamming Target Lock Time', 'mid_war', '通信进攻操作',
 '从发起干扰锁定到锁定完成的平均耗时',
 'records_comm_attack_operation', NULL, 'custom',
 'SELECT AVG(end_time_ms - start_time_ms) FROM records_comm_attack_operation WHERE operation_id = {operation_id} AND operation_type = ''jamming_target_lock'' AND end_time_ms IS NOT NULL',
 'ms', 'negative', 9, 1),

-- Q10 欺骗信号成功率
('eqt_deception_success_rate', '欺骗信号成功率', 'Deception Success Rate', 'mid_war', '通信进攻操作',
 '欺骗成功次数 / 欺骗总次数 × 100%',
 'records_comm_attack_operation', 'spoofing_success', 'percentage',
 'WHERE operation_type = ''spoofing_signal''', '%', 'positive', 10, 1);

-- ============================================================
-- 定性指标配置（6条）
-- ============================================================
INSERT INTO `equipment_ql_indicator_def` (
    `indicator_key`, `indicator_name`, `indicator_name_en`, `phase`, `dimension`,
    `description`, `reference_table`, `reference_field`, `reference_sql_template`,
    `evaluation_method`, `grade_keys`, `confidence_required`, `confidence_method`,
    `allow_comment`, `scoring_help`, `display_order`, `enabled`
) VALUES
-- L1 维修能力
('eql_maintenance_skill', '维修能力', 'Maintenance Skill', 'post_war', '维修与反馈',
 '综合战役/战术级维修工时、维修成功率、维修人员数量等因素评估维修能力',
 NULL, NULL, NULL,
 'grade',
 JSON_ARRAY('A_plus','A','A_minus','B_plus','B','B_minus','C_plus','C','C_minus','D'),
 1, 'percentage', 1,
 '根据维修质量、定位速度、维修方案合理性综合打分',
 1, 1),

-- L2 干扰效能达成
('eql_jamming_effectiveness', '干扰效能达成', 'Jamming Effectiveness', 'mid_war', '通信进攻操作',
 '基于effect_assessment文本评估干扰效能（降级程度/完全中断等）',
 'records_comm_attack_operation', 'effect_assessment',
 'custom',
 'grade',
 JSON_ARRAY('A_plus','A','A_minus','B_plus','B','B_minus','C_plus','C','C_minus','D'),
 1, 'percentage', 1,
 '根据干扰前后通信质量下降幅度、敌方反应综合打分',
 2, 1),

-- L3 欺骗信号生成
('eql_deception_signal', '欺骗信号生成', 'Deception Signal Generation', 'mid_war', '通信进攻操作',
 '基于欺骗信号类型和专家判断评估欺骗信号生成质量',
 'records_comm_attack_operation', 'spoofing_signal_type',
 'custom',
 'grade',
 JSON_ARRAY('A_plus','A','A_minus','B_plus','B','B_minus','C_plus','C','C_minus','D'),
 1, 'percentage', 1,
 '根据欺骗信号的逼真度、敌方上当程度、策略有效性综合打分',
 3, 1),

-- L4 信号截获感知
('eql_signal_interception', '信号截获感知', 'Signal Interception Awareness', 'mid_war', '通信防御操作',
 '基于感知耗时和发现手段评估信号截获感知能力',
 'records_comm_defense_operation', 'detection_time_ms',
 'custom',
 'grade',
 JSON_ARRAY('A_plus','A','A_minus','B_plus','B','B_minus','C_plus','C','C_minus','D'),
 1, 'percentage', 1,
 '根据感知准确性、判断及时性、威胁识别质量综合打分',
 4, 1),

-- L5 抗干扰操作
('eql_anti_jamming', '抗干扰操作', 'Anti-Jamming Operation', 'mid_war', '通信防御操作',
 '基于抗干扰措施有效性和成功率评估',
 'records_comm_defense_operation', 'anti_jamming_success',
 'custom',
 'grade',
 JSON_ARRAY('A_plus','A','A_minus','B_plus','B','B_minus','C_plus','C','C_minus','D'),
 1, 'percentage', 1,
 '根据抗干扰措施合理性、切换方案有效性、恢复速度综合打分',
 5, 1),

-- L6 防骗反骗
('eql_anti_deception', '防骗反骗', 'Anti-Deception', 'mid_war', '通信防御操作',
 '基于可疑信号识别和核实结果评估防骗反骗能力',
 'records_comm_defense_operation', 'verification_result',
 'custom',
 'grade',
 JSON_ARRAY('A_plus','A','A_minus','B_plus','B','B_minus','C_plus','C','C_minus','D'),
 1, 'percentage', 1,
 '根据识破率、核实准确性、反制策略有效性综合打分',
 6, 1);
