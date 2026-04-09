#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
插入装备操作指标配置数据（修订版）
创建时间: 2026-04-04
说明: 定量指标14条 + 定性指标5条，按阶段-二级维度分层组织

【维度体系】
  战前 → 系统性能（组网时长）
  战中 → 系统性能（应急处理、连通率、任务可靠度；原独立维度「操作响应」已取消并入）
  战中 → 通信保障操作（链路维持、业务开通、应急抢通）
  战后 → 维修与反馈（返修率、抢修能力）
  战中 → 通信进攻操作（干扰目标锁定、干扰效能达成、欺骗信号生成）
  战中 → 通信防御操作（信号截获感知、抗干扰操作、防骗反骗）
"""
import mysql.connector
import os

DB_CONFIG = {
    'host': os.environ.get('DB_HOST', 'localhost'),
    'port': int(os.environ.get('DB_PORT', '3306')),
    'database': os.environ.get('DB_NAME', 'military_operational_effectiveness_evaluation'),
    'user': os.environ.get('DB_USER', 'root'),
    'password': os.environ.get('DB_PASS', 'root')
}

conn = mysql.connector.connect(**DB_CONFIG)
cursor = conn.cursor()

print("清空旧数据...")
cursor.execute("DELETE FROM equipment_qt_indicator_def")
cursor.execute("DELETE FROM equipment_ql_indicator_def")
conn.commit()

# ==================== 定量指标配置（14条）====================
# Q1~Q8:  战前/战中/战后常规维度
# Q9~Q11: 通信进攻维度
# Q12~Q14: 通信防御维度
qt_indicators = [
    # === 战前 → 系统性能 ===
    # Q1 组网时长
    ("eqt_network_setup_time", "组网时长", "Network Setup Time", "pre_war", "系统性能",
     "战前系统开设阶段，从指令发出到所有节点就绪的平均时长",
     "records_military_operation_info", "avg_network_setup_time_ms", None,
     "avg", "ms", "negative", 1, 1),

    # === 战中 → 系统性能（应急处理，原「操作响应」维度已并入） ===
    # Q2 应急处理
    ("eqt_emergency_handling", "应急处理", "Emergency Response", "mid_war", "系统性能",
     "作战过程中，操作员感知到异常并做出反应的平均时间",
     "records_military_communication_info", "operator_reaction_ms", None,
     "avg", "ms", "negative", 2, 1),

    # === 战中 → 通信保障操作 ===
    # Q3 链路维持
    ("eqt_link_maintenance", "链路维持", "Link Maintenance", "mid_war", "通信保障操作",
     "通信链路维持能力 = (总通信时长 - 总中断时长) / 总通信时长 × 100%",
     None, None,
     "SELECT ((SELECT MAX(end_time_ms)-MIN(start_time_ms) FROM records_military_communication_info WHERE operation_id = {operation_id}) - COALESCE((SELECT SUM(interruption_end_ms-interruption_start_ms) FROM records_link_maintenance_events WHERE operation_id = {operation_id}), 0)) * 100.0 / NULLIF((SELECT MAX(end_time_ms)-MIN(start_time_ms) FROM records_military_communication_info WHERE operation_id = {operation_id}), 0)",
     "custom", "%", "positive", 3, 1),

    # Q4 业务开通
    ("eqt_service_activation", "业务开通", "Service Activation", "mid_war", "通信保障操作",
     "从发起通信请求到通信建立完成的平均时长",
     "records_military_communication_info", "call_setup_ms", None,
     "avg", "ms", "negative", 4, 1),

    # Q5 应急抢通
    ("eqt_emergency_restoration", "应急抢通", "Emergency Restoration", "mid_war", "通信保障操作",
     "链路中断后启动应急手段并恢复通信的平均耗时（仅统计成功的抢通）",
     "records_link_maintenance_events", "recovery_duration_ms",
     "recovery_success = 1 AND recovery_duration_ms IS NOT NULL",
     "avg_conditional", "ms", "negative", 5, 1),

    # === 战中 → 系统性能 ===
    # Q6 连通率
    ("eqt_connectivity_rate", "连通率", "Connectivity Rate", "mid_war", "系统性能",
     "实际连通节点数 / 总节点数 × 100%",
     "records_military_operation_info", None,
     "SELECT (total_node_count - isolated_node_count) * 100.0 / NULLIF(total_node_count, 0) FROM records_military_operation_info WHERE operation_id = {operation_id}",
     "custom", "%", "positive", 6, 1),

    # Q7 任务可靠度
    ("eqt_mission_reliability", "任务可靠度", "Mission Reliability", "mid_war", "系统性能",
     "通信成功次数 / 总通信次数 × 100%",
     "records_military_communication_info", "comm_success", None,
     "percentage", "%", "positive", 7, 1),

    # === 战后 → 维修与反馈 ===
    # Q8 返修率
    ("eqt_rework_rate", "返修率", "Rework Rate", "post_war", "维修与反馈",
     "维修失败次数 / 需要维修的总次数 × 100%",
     "records_link_maintenance_events", None,
     "SELECT COUNT(CASE WHEN maintenance_success = 0 AND maintenance_required = 1 THEN 1 END) * 100.0 / NULLIF(COUNT(CASE WHEN maintenance_required = 1 THEN 1 END), 0) FROM records_link_maintenance_events WHERE operation_id = {operation_id}",
     "custom", "%", "negative", 8, 1),

    # Q9 抢修能力
    ("eqt_emergency_repair_capacity", "抢修能力", "Emergency Repair Capacity", "post_war", "维修与反馈",
     "链路故障后成功恢复的次数 / 总中断次数 × 100%，反映应急抢修能力",
     "records_link_maintenance_events", "recovery_success", None,
     "percentage", "%", "positive", 9, 1),

    # === 战中 → 通信进攻操作（定量） ===
    # Q10 干扰目标锁定耗时
    ("eqt_jamming_target_time", "干扰目标锁定耗时", "Jamming Target Lock Time", "mid_war", "通信进攻操作",
     "从发起干扰锁定到锁定完成的平均耗时",
     "records_comm_attack_operation", None,
     "SELECT AVG(end_time_ms - start_time_ms) FROM records_comm_attack_operation WHERE operation_id = {operation_id} AND operation_type = 'jamming_target_lock' AND end_time_ms IS NOT NULL",
     "custom", "ms", "negative", 10, 1),

    # Q11 干扰效能达成（定量）
    ("eqt_jamming_effectiveness_rate", "干扰效能达成", "Jamming Effectiveness Rate", "mid_war", "通信进攻操作",
     "干扰后通信质量降级或中断的次数 / 总干扰次数 × 100%",
     "records_comm_attack_operation", None,
     "SELECT COUNT(CASE WHEN effect_assessment IS NOT NULL AND effect_assessment != 'none' THEN 1 END) * 100.0 / NULLIF(COUNT(*), 0) FROM records_comm_attack_operation WHERE operation_id = {operation_id} AND operation_type = 'jamming_effect'",
     "custom", "%", "positive", 11, 1),

    # === 战中 → 通信防御操作（定量） ===
    # Q12 信号截获感知
    ("eqt_signal_interception_time", "信号截获感知", "Signal Interception Time", "mid_war", "通信防御操作",
     "从通信开始到检测到可疑信号的响应时间均值",
     "records_comm_defense_operation", "detection_time_ms", None,
     "avg", "ms", "negative", 12, 1),

    # Q13 抗干扰操作（定量）
    ("eqt_anti_jamming_success_rate", "抗干扰操作", "Anti-Jamming Success Rate", "mid_war", "通信防御操作",
     "执行抗干扰操作后成功恢复通信的次数 / 总抗干扰次数 × 100%",
     "records_comm_defense_operation", "anti_jamming_success",
     "SELECT COUNT(CASE WHEN anti_jamming_success = 1 THEN 1 END) * 100.0 / NULLIF(COUNT(*), 0) FROM records_comm_defense_operation WHERE operation_id = {operation_id} AND operation_type = 'anti_jamming' AND anti_jamming_success IS NOT NULL",
     "custom", "%", "positive", 13, 1),

    # Q14 防骗反骗（定量）
    ("eqt_anti_deception_rate", "防骗反骗", "Anti-Deception Detection Rate", "mid_war", "通信防御操作",
     "发现并识别可疑欺骗信号的次数 / 可疑信号出现总次数 × 100%",
     "records_comm_defense_operation", None,
     "SELECT COUNT(CASE WHEN verification_result = 'confirmed' THEN 1 END) * 100.0 / NULLIF(COUNT(*), 0) FROM records_comm_defense_operation WHERE operation_id = {operation_id} AND operation_type = 'anti_deception'",
     "custom", "%", "positive", 14, 1),
]

print("插入定量指标...")
cursor.executemany("""
    INSERT INTO equipment_qt_indicator_def (
        indicator_key, indicator_name, indicator_name_en, phase, dimension,
        description, source_table, source_field, custom_sql_template,
        aggregation_method, unit, score_direction, display_order, enabled
    ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
""", qt_indicators)
print(f"  插入 {len(qt_indicators)} 条定量指标")

# ==================== 定性指标配置（5条）====================
# L1~L2: 通信进攻维度
# L3~L5: 通信防御维度
ql_indicators = [
    # === 通信进攻操作（定性） ===
    # L1 干扰效能达成（定性）
    ("eql_jamming_effectiveness", "干扰效能达成", "Jamming Effectiveness", "mid_war", "通信进攻操作",
     "基于 effect_assessment 文本评估干扰效能（降级程度/完全中断等），由专家综合判断",
     "records_comm_attack_operation", "effect_assessment",
     "SELECT effect_assessment FROM records_comm_attack_operation WHERE operation_id = {operation_id} AND operation_type = 'jamming_effect'",
     "grade",
     '["A_plus","A","A_minus","B_plus","B","B_minus","C_plus","C","C_minus","D"]',
     1, "percentage", 1,
     "根据干扰前后通信质量下降幅度、敌方反应、持续时间综合打分",
     1, 1, 1),

    # L2 欺骗信号生成（定性）
    ("eql_deception_signal", "欺骗信号生成", "Deception Signal Generation", "mid_war", "通信进攻操作",
     "基于欺骗信号类型和专家判断评估欺骗信号生成质量与敌方上当程度",
     "records_comm_attack_operation", "spoofing_signal_type",
     "SELECT spoofing_signal_type, spoofing_success FROM records_comm_attack_operation WHERE operation_id = {operation_id} AND operation_type = 'spoofing_signal'",
     "grade",
     '["A_plus","A","A_minus","B_plus","B","B_minus","C_plus","C","C_minus","D"]',
     1, "percentage", 1,
     "根据欺骗信号的逼真度、敌方上当程度、策略有效性综合打分",
     2, 1, 1),

    # === 通信防御操作（定性） ===
    # L3 信号截获感知（定性）
    ("eql_signal_interception", "信号截获感知", "Signal Interception Awareness", "mid_war", "通信防御操作",
     "基于感知耗时和发现手段评估信号截获感知能力，由专家综合评判",
     "records_comm_defense_operation", "detection_time_ms",
     "SELECT detection_time_ms, detection_method FROM records_comm_defense_operation WHERE operation_id = {operation_id} AND operation_type = 'detection_awareness'",
     "grade",
     '["A_plus","A","A_minus","B_plus","B","B_minus","C_plus","C","C_minus","D"]',
     1, "percentage", 1,
     "根据感知准确性、判断及时性、威胁识别质量综合打分",
     3, 1, 1),

    # L4 抗干扰操作（定性）
    ("eql_anti_jamming", "抗干扰操作", "Anti-Jamming Operation", "mid_war", "通信防御操作",
     "基于抗干扰措施有效性和成功率评估，由专家综合判断",
     "records_comm_defense_operation", "anti_jamming_success",
     "SELECT anti_jamming_actions, anti_jamming_success FROM records_comm_defense_operation WHERE operation_id = {operation_id} AND operation_type = 'anti_jamming'",
     "grade",
     '["A_plus","A","A_minus","B_plus","B","B_minus","C_plus","C","C_minus","D"]',
     1, "percentage", 1,
     "根据抗干扰措施合理性、切换方案有效性、恢复速度综合打分",
     4, 1, 1),

    # L5 防骗反骗（定性）
    ("eql_anti_deception", "防骗反骗", "Anti-Deception", "mid_war", "通信防御操作",
     "基于可疑信号识别和核实结果评估防骗反骗能力，由专家综合评判",
     "records_comm_defense_operation", "verification_result",
     "SELECT suspicious_signal_detected, verification_result FROM records_comm_defense_operation WHERE operation_id = {operation_id} AND operation_type = 'anti_deception'",
     "grade",
     '["A_plus","A","A_minus","B_plus","B","B_minus","C_plus","C","C_minus","D"]',
     1, "percentage", 1,
     "根据识破率、核实准确性、反制策略有效性综合打分",
     5, 1, 1),
]

print("插入定性指标...")
cursor.executemany("""
    INSERT INTO equipment_ql_indicator_def (
        indicator_key, indicator_name, indicator_name_en, phase, dimension,
        description, reference_table, reference_field, reference_sql_template,
        evaluation_method, grade_keys, confidence_required, confidence_method,
        allow_comment, scoring_help, display_order, enabled
    ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
""", ql_indicators)
print(f"  插入 {len(ql_indicators)} 条定性指标")

conn.commit()
cursor.close()
conn.close()
print("\n完成！定量 14 条 + 定性 5 条指标已插入。")
