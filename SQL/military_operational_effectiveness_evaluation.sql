/*
 Navicat Premium Dump SQL

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 90600 (9.6.0)
 Source Host           : localhost:3306
 Source Schema         : military_operational_effectiveness_evaluation

 Target Server Type    : MySQL
 Target Server Version : 90600 (9.6.0)
 File Encoding         : 65001

 Date: 31/03/2026 09:59:51
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ahp_equipment_operation_weights
-- ----------------------------
DROP TABLE IF EXISTS `ahp_equipment_operation_weights`;
CREATE TABLE `ahp_equipment_operation_weights`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `batch_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评估批次ID（例如：AHP-2025-01），用于区分不同次权重计算',
  `evaluation_time` datetime NOT NULL COMMENT '评估时间（关联评估表）',
  `personnel_dim_weight` decimal(5, 0) NULL DEFAULT NULL,
  `personnel_dim_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '人员能力维度权重置信度',
  `personnel_personnel_count_qt_weight` decimal(5, 0) NULL DEFAULT NULL,
  `personnel_personnel_count_qt_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '人员数量指标权重置信度',
  `personnel_work_experience_qt_weight` decimal(5, 0) NULL DEFAULT NULL,
  `personnel_work_experience_qt_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '工作经验指标权重置信度',
  `personnel_training_experience_qt_weight` decimal(5, 0) NULL DEFAULT NULL,
  `personnel_training_experience_qt_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '演训经历指标权重置信度',
  `system_setup_dim_weight` decimal(5, 0) NULL DEFAULT NULL,
  `system_setup_dim_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '系统开设维度权重置信度',
  `system_setup_network_setup_time_qt_weight` decimal(5, 0) NULL DEFAULT NULL,
  `system_setup_network_setup_time_qt_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '组网时长指标权重置信度',
  `maintenance_dim_weight` decimal(5, 0) NULL DEFAULT NULL,
  `maintenance_dim_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '维修保障维度权重置信度',
  `maintenance_maintenance_skill_ql_weight` decimal(5, 0) NULL DEFAULT NULL,
  `maintenance_maintenance_skill_ql_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '维修能力指标权重置信度',
  `maintenance_spare_parts_availability_qt_weight` decimal(5, 0) NULL DEFAULT NULL,
  `maintenance_spare_parts_availability_qt_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '备件充足指标权重置信度',
  `response_dim_weight` decimal(5, 0) NULL DEFAULT NULL,
  `response_dim_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '操作响应维度权重置信度',
  `response_emergency_handling_qt_weight` decimal(5, 0) NULL DEFAULT NULL,
  `response_emergency_handling_qt_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '应急处理指标权重置信度',
  `comm_support_dim_weight` decimal(5, 0) NULL DEFAULT NULL,
  `comm_support_dim_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '通信保障操作维度权重置信度',
  `comm_support_link_maintenance_qt_weight` decimal(5, 0) NULL DEFAULT NULL,
  `comm_support_link_maintenance_qt_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '链路维持指标权重置信度',
  `comm_support_service_activation_qt_weight` decimal(5, 0) NULL DEFAULT NULL,
  `comm_support_service_activation_qt_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '业务开通指标权重置信度',
  `comm_support_emergency_restoration_qt_weight` decimal(5, 0) NULL DEFAULT NULL,
  `comm_support_emergency_restoration_qt_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '应急抢通指标权重置信度',
  `comm_attack_dim_weight` decimal(5, 0) NULL DEFAULT NULL,
  `comm_attack_dim_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '通信进攻操作维度权重置信度',
  `comm_attack_target_acquisition_qt_weight` decimal(5, 0) NULL DEFAULT NULL,
  `comm_attack_target_acquisition_qt_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '干扰目标锁定指标权重置信度',
  `comm_attack_jamming_effectiveness_ql_weight` decimal(5, 0) NULL DEFAULT NULL,
  `comm_attack_jamming_effectiveness_ql_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '干扰效能达成指标权重置信度',
  `comm_attack_deception_signal_generation_ql_weight` decimal(5, 0) NULL DEFAULT NULL,
  `comm_attack_deception_signal_generation_ql_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '欺骗信号生成指标权重置信度',
  `comm_defense_dim_weight` decimal(5, 0) NULL DEFAULT NULL,
  `comm_defense_dim_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '通信防御操作维度权重置信度',
  `comm_defense_signal_interception_awareness_ql_weight` decimal(5, 0) NULL DEFAULT NULL,
  `comm_defense_signal_interception_awareness_ql_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '信号截获感知指标权重置信度',
  `comm_defense_anti_jamming_operation_ql_weight` decimal(5, 0) NULL DEFAULT NULL,
  `comm_defense_anti_jamming_operation_ql_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '抗干扰操作指标权重置信度',
  `comm_defense_anti_deception_awareness_ql_weight` decimal(5, 0) NULL DEFAULT NULL,
  `comm_defense_anti_deception_awareness_ql_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '防骗反骗指标权重置信度',
  `system_performance_dim_weight` decimal(5, 0) NULL DEFAULT NULL,
  `system_performance_dim_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '系统性能维度权重置信度',
  `system_performance_connectivity_rate_qt_weight` decimal(5, 0) NULL DEFAULT NULL,
  `system_performance_connectivity_rate_qt_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '连通率指标权重置信度',
  `system_performance_mission_reliability_qt_weight` decimal(5, 0) NULL DEFAULT NULL,
  `system_performance_mission_reliability_qt_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '任务可靠度指标权重置信度',
  `maintenance_feedback_dim_weight` decimal(5, 0) NULL DEFAULT NULL,
  `maintenance_feedback_dim_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '维修与反馈维度权重置信度',
  `maintenance_feedback_field_repair_qt_weight` decimal(5, 0) NULL DEFAULT NULL,
  `maintenance_feedback_field_repair_qt_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '抢修能力指标权重置信度',
  `maintenance_feedback_rework_rate_qt_weight` decimal(5, 0) NULL DEFAULT NULL,
  `maintenance_feedback_rework_rate_qt_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '返修率指标权重置信度',
  `maintenance_feedback_equipment_feedback_ql_weight` decimal(5, 0) NULL DEFAULT NULL,
  `maintenance_feedback_equipment_feedback_ql_confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '装备反馈指标权重置信度',
  `remarks` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_operation`(`batch_id` ASC) USING BTREE,
  INDEX `idx_evaluation_time`(`evaluation_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '装备操作层级AHP权重表（含维度权重和指标权重及置信度）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ahp_expert_aggregation_weights
-- ----------------------------
DROP TABLE IF EXISTS `ahp_expert_aggregation_weights`;
CREATE TABLE `ahp_expert_aggregation_weights`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `evaluation_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '评估批次ID',
  `expert_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '参与集结的专家数量',
  `security_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '安全性指标权重（集结后）',
  `reliability_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '可靠性指标权重（集结后）',
  `transmission_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '传输性能指标权重（集结后）',
  `anti_jamming_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '抗干扰指标权重（集结后）',
  `effect_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '作战效能指标权重（集结后）',
  `security_key_leakage_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '密钥泄露权重（集结后）',
  `security_detected_probability_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '被侦察概率权重（集结后）',
  `security_interception_resistance_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '抗拦截能力权重（集结后）',
  `reliability_crash_rate_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '崩溃比例权重（集结后）',
  `reliability_recovery_capability_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '恢复能力权重（集结后）',
  `reliability_communication_availability_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '通信可用性权重（集结后）',
  `transmission_bandwidth_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '带宽权重（集结后）',
  `transmission_call_setup_time_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '呼叫建立时间权重（集结后）',
  `transmission_transmission_delay_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '传输时延权重（集结后）',
  `transmission_bit_error_rate_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '误码率权重（集结后）',
  `transmission_throughput_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '吞吐量权重（集结后）',
  `transmission_spectral_efficiency_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '频谱效率权重（集结后）',
  `anti_jamming_sinr_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '信干噪比权重（集结后）',
  `anti_jamming_anti_jamming_margin_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '抗干扰余量权重（集结后）',
  `anti_jamming_communication_distance_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '通信距离权重（集结后）',
  `effect_damage_rate_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '战损率权重（集结后）',
  `effect_mission_completion_rate_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '任务完成率权重（集结后）',
  `effect_blind_rate_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '致盲率权重（集结后）',
  `security_std_dev` decimal(5, 4) NULL DEFAULT NULL COMMENT '安全性权重标准差（反映专家分歧度）',
  `reliability_std_dev` decimal(5, 4) NULL DEFAULT NULL COMMENT '可靠性权重标准差',
  `transmission_std_dev` decimal(5, 4) NULL DEFAULT NULL COMMENT '传输性能权重标准差',
  `anti_jamming_std_dev` decimal(5, 4) NULL DEFAULT NULL COMMENT '抗干扰权重标准差',
  `effect_std_dev` decimal(5, 4) NULL DEFAULT NULL COMMENT '作战效能权重标准差',
  `consistency_check_pass_rate` decimal(5, 2) NULL DEFAULT NULL COMMENT '一致性检验通过率（%）',
  `aggregation_details` json NULL COMMENT '集结详细信息（JSON格式，存储各专家原始权重等）',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '备注信息',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_evaluation_id`(`evaluation_id` ASC) USING BTREE,
  INDEX `idx_created`(`created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = 'AHP专家集结表（存储同一评估批次下多专家权重的集结结果）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for ahp_expert_military_operation_effect_weights
-- ----------------------------
DROP TABLE IF EXISTS `ahp_expert_military_operation_effect_weights`;
CREATE TABLE `ahp_expert_military_operation_effect_weights`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `batch_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评估批次ID（例如：AHP-2025-01），用于区分不同次权重计算',
  `expert_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '专家姓名或编号',
  `security_key_leakage_weight` decimal(5, 2) NULL DEFAULT NULL COMMENT '密钥泄露权重（1-9分）',
  `security_key_leakage_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '密钥泄露把握度（如0-100或1-5等级）',
  `security_detected_probability_weight` decimal(5, 2) NULL DEFAULT NULL COMMENT '被侦察权重',
  `security_detected_probability_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '被侦察把握度',
  `security_interception_resistance_weight` decimal(5, 2) NULL DEFAULT NULL COMMENT '抗拦截权重',
  `security_interception_resistance_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '抗拦截把握度',
  `reliability_crash_rate_weight` decimal(5, 2) NULL DEFAULT NULL COMMENT '崩溃比例权重',
  `reliability_crash_rate_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '崩溃比例把握度',
  `reliability_recovery_capability_weight` decimal(5, 2) NULL DEFAULT NULL COMMENT '恢复能力权重',
  `reliability_recovery_capability_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '恢复能力把握度',
  `reliability_communication_availability_weight` decimal(5, 2) NULL DEFAULT NULL COMMENT '通信可用权重',
  `reliability_communication_availability_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '通信可用把握度',
  `transmission_bandwidth_weight` decimal(5, 2) NULL DEFAULT NULL COMMENT '带宽权重',
  `transmission_bandwidth_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '带宽把握度',
  `transmission_call_setup_time_weight` decimal(5, 2) NULL DEFAULT NULL COMMENT '呼叫建立权重',
  `transmission_call_setup_time_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '呼叫建立把握度',
  `transmission_transmission_delay_weight` decimal(5, 2) NULL DEFAULT NULL COMMENT '传输时延权重',
  `transmission_transmission_delay_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '传输时延把握度',
  `transmission_bit_error_rate_weight` decimal(5, 2) NULL DEFAULT NULL COMMENT '误码率权重',
  `transmission_bit_error_rate_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '误码率把握度',
  `transmission_throughput_weight` decimal(5, 2) NULL DEFAULT NULL COMMENT '吞吐量权重',
  `transmission_throughput_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '吞吐量把握度',
  `transmission_spectral_efficiency_weight` decimal(5, 2) NULL DEFAULT NULL COMMENT '频谱效率权重',
  `transmission_spectral_efficiency_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '频谱效率把握度',
  `anti_jamming_sinr_weight` decimal(5, 2) NULL DEFAULT NULL COMMENT '信干噪比权重',
  `anti_jamming_sinr_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '信干噪比把握度',
  `anti_jamming_anti_jamming_margin_weight` decimal(5, 2) NULL DEFAULT NULL COMMENT '抗干扰余量权重',
  `anti_jamming_anti_jamming_margin_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '抗干扰余量把握度',
  `anti_jamming_communication_distance_weight` decimal(5, 2) NULL DEFAULT NULL COMMENT '通信距离权重',
  `anti_jamming_communication_distance_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '通信距离把握度',
  `resource_power_consumption_weight` decimal(5, 2) NULL DEFAULT NULL COMMENT '功耗权重',
  `resource_power_consumption_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '功耗把握度',
  `resource_manpower_requirement_weight` decimal(5, 2) NULL DEFAULT NULL COMMENT '人力需求权重',
  `resource_manpower_requirement_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '人力需求把握度',
  `effect_damage_rate_weight` decimal(5, 2) NULL DEFAULT NULL COMMENT '战损率权重',
  `effect_damage_rate_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '战损率把握度',
  `effect_mission_completion_rate_weight` decimal(5, 2) NULL DEFAULT NULL COMMENT '任务完成率权重',
  `effect_mission_completion_rate_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '任务完成率把握度',
  `effect_cost_effectiveness_weight` decimal(5, 2) NULL DEFAULT NULL COMMENT '效费比权重',
  `effect_cost_effectiveness_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '效费比把握度',
  `remarks` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '备注（可选）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_batch`(`batch_id` ASC) USING BTREE,
  INDEX `idx_expert`(`expert_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 131 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AHP专家权重打分表（直接评分法，含把握度）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ahp_final_weights
-- ----------------------------
DROP TABLE IF EXISTS `ahp_final_weights`;
CREATE TABLE `ahp_final_weights`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `batch_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评估批次ID，如 AHP-2026-001',
  `evaluation_date` date NOT NULL COMMENT '评估日期',
  `indicator_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '指标键名，如 security_key_leakage',
  `indicator_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '指标中文名，如 密钥泄露',
  `indicator_name_en` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '指标英文名，如 Key Leakage',
  `category` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '一级类别 (security/reliability/transmission/anti_jamming/resource/effect)',
  `final_weight` decimal(8, 6) NULL DEFAULT NULL COMMENT '最终权重 (归一化后，总和=1)，二级权重×一级类别权重',
  `second_level_weight` decimal(8, 6) NULL DEFAULT NULL COMMENT '二级权重 (未乘一级权重)，基于AHP几何平均法计算',
  `original_weight` decimal(8, 6) NULL DEFAULT NULL COMMENT '原始平均权重，专家评分的简单算术平均',
  `lambda_max` decimal(8, 4) NULL DEFAULT NULL COMMENT 'AHP最大特征值 λmax，用于一致性检验',
  `CI` decimal(8, 6) NULL DEFAULT NULL COMMENT '一致性指标 CI = (λmax-n)/(n-1)',
  `CR` decimal(8, 6) NULL DEFAULT NULL COMMENT '一致性比率 CR = CI/RI，CR≤0.1表示通过一致性检验',
  `is_consistent` tinyint(1) NULL DEFAULT NULL COMMENT '是否通过一致性检验 (1=通过, 0=未通过)',
  `valid_experts` int NULL DEFAULT NULL COMMENT '有效专家数量，即把握度≥阈值的专家数量',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_batch`(`batch_id` ASC) USING BTREE COMMENT '按批次查询',
  INDEX `idx_category`(`category` ASC) USING BTREE COMMENT '按一级类别查询',
  INDEX `idx_weight`(`final_weight` DESC) USING BTREE COMMENT '按最终权重降序排列'
) ENGINE = InnoDB AUTO_INCREMENT = 161 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '最终AHP权重结果表 - 存储修正后的指标权重及一致性信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ahp_individual_expert_weights
-- ----------------------------
DROP TABLE IF EXISTS `ahp_individual_expert_weights`;
CREATE TABLE `ahp_individual_expert_weights`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `expert_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '专家ID',
  `expert_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '专家姓名',
  `evaluation_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评估批次ID',
  `security_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '安全性指标权重',
  `reliability_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '可靠性指标权重',
  `transmission_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '传输性能指标权重',
  `anti_jamming_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '抗干扰指标权重',
  `effect_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '作战效能指标权重',
  `security_key_leakage_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '密钥泄露权重',
  `security_detected_probability_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '被侦察概率权重',
  `security_interception_resistance_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '抗拦截能力权重',
  `reliability_crash_rate_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '崩溃比例权重',
  `reliability_recovery_capability_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '恢复能力权重',
  `reliability_communication_availability_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '通信可用性权重',
  `transmission_bandwidth_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '带宽权重',
  `transmission_call_setup_time_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '呼叫建立时间权重',
  `transmission_transmission_delay_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '传输时延权重',
  `transmission_bit_error_rate_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '误码率权重',
  `transmission_throughput_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '吞吐量权重',
  `transmission_spectral_efficiency_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '频谱效率权重',
  `anti_jamming_sinr_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '信干噪比权重',
  `anti_jamming_anti_jamming_margin_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '抗干扰余量权重',
  `anti_jamming_communication_distance_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '通信距离权重',
  `effect_damage_rate_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '战损率权重',
  `effect_mission_completion_rate_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '任务完成率权重',
  `effect_blind_rate_weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '致盲率权重',
  `consistency_ratio` decimal(5, 4) NULL DEFAULT NULL COMMENT '一致性比率CR值（CR<0.1表示通过一致性检验）',
  `consistency_index` decimal(5, 4) NULL DEFAULT NULL COMMENT '一致性指标CI值',
  `is_consistent` tinyint(1) NULL DEFAULT 0 COMMENT '是否通过一致性检验（0:未通过, 1:通过）',
  `ahp_result_json` json NULL COMMENT 'AHP完整计算结果（JSON格式存储）',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注信息',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_expert_id`(`expert_id` ASC) USING BTREE,
  INDEX `idx_evaluation_id`(`evaluation_id` ASC) USING BTREE,
  INDEX `idx_created`(`created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AHP专家权重表（存储专家信息及AHP层次分析法计算的各级指标权重）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for cost_evaluation
-- ----------------------------
DROP TABLE IF EXISTS `cost_evaluation`;
CREATE TABLE `cost_evaluation`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `operation_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '作战任务ID',
  `evaluation_time` datetime NOT NULL COMMENT '评估时间',
  `cost_personnel_strategic_command_staff_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '战略指挥人员编制 (人)',
  `cost_personnel_campaign_command_staff_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '战役指挥人员编制 (人)',
  `cost_personnel_tactical_staff_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '战术级人员编制 (人)',
  `cost_personnel_equipment_operators_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '装备操作人员编制 (人)',
  `cost_personnel_campaign_maintenance_hours_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '战役级装备维修工时 (小时)',
  `cost_personnel_tactical_maintenance_hours_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '战术级装备维护工时 (小时)',
  `cost_personnel_unit_maintenance_hours_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '单装日常维护工时 (小时)',
  `cost_equipment_procurement_total_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '装备采购总费用 (万元)',
  `cost_equipment_depreciation_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '单装折旧成本 (元/小时)',
  `cost_equipment_campaign_support_maintenance_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '战役级保障装备维护费 (万元)',
  `cost_energy_campaign_fuel_electricity_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '战役级油料/电力保障费 (万元)',
  `cost_energy_tactical_fuel_battery_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '战术行动油料/电池消耗 (升/块)',
  `cost_energy_unit_direct_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '单装直接能源消耗 (升/小时 或 度/小时)',
  `cost_logistics_spare_parts_availability_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '备件充足率 (%)',
  `cost_logistics_campaign_storage_transport_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '战役级备件仓储与运输费 (万元)',
  `cost_logistics_tactical_forward_delivery_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '战术级备件前送费用 (元/次)',
  `cost_training_total_budget_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '训练经费总额 (万元)',
  `cost_training_tactical_consumption_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '战术级训练消耗 (万元)',
  `cost_training_per_soldier_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '单兵培训费用 (元/人)',
  `cost_infrastructure_base_construction_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '保障基地建设费 (万元)',
  `cost_infrastructure_spectrum_fee_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '频谱资源占用费 (万元/年)',
  `remarks` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_operation`(`operation_id` ASC) USING BTREE,
  INDEX `idx_evaluation_time`(`evaluation_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 31 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '成本评估表（全成本指标扁平版）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for equipment_operation_qualitative_score
-- ----------------------------
DROP TABLE IF EXISTS `equipment_operation_qualitative_score`;
CREATE TABLE `equipment_operation_qualitative_score`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `operation_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '作战任务ID',
  `evaluation_time` datetime NOT NULL COMMENT '评估时间',
  `expert_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '专家姓名（用于区分不同专家）',
  `expert_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '专家ID（可选，关联专家可信度表）',
  `maintenance_maintenance_skill_ql` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '维修能力得分（等级：A+ ~ E-）',
  `maintenance_maintenance_skill_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '维修能力得分把握度（如0.9表示90%）',
  `comm_attack_jamming_effectiveness_ql` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '干扰效能达成得分（等级：A+ ~ E-）',
  `comm_attack_jamming_effectiveness_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '干扰效能达成得分把握度',
  `comm_attack_deception_signal_generation_ql` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '欺骗信号生成得分（等级：A+ ~ E-）',
  `comm_attack_deception_signal_generation_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '欺骗信号生成得分把握度',
  `comm_defense_signal_interception_awareness_ql` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '信号截获感知得分（等级：A+ ~ E-）',
  `comm_defense_signal_interception_awareness_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '信号截获感知得分把握度',
  `comm_defense_anti_jamming_operation_ql` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '抗干扰操作得分（等级：A+ ~ E-）',
  `comm_defense_anti_jamming_operation_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '抗干扰操作得分把握度',
  `comm_defense_anti_deception_awareness_ql` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '防骗反骗得分（等级：A+ ~ E-）',
  `comm_defense_anti_deception_awareness_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '防骗反骗得分把握度',
  `maintenance_feedback_equipment_feedback_ql` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '装备反馈得分（等级：A+ ~ E-）',
  `maintenance_feedback_equipment_feedback_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '装备反馈得分把握度',
  `remarks` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_operation`(`operation_id` ASC) USING BTREE,
  INDEX `idx_evaluation_time`(`evaluation_time` ASC) USING BTREE,
  INDEX `idx_expert`(`expert_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '装备操作定性指标专家打分表（每行一位专家对本次评估所有定性指标的评分）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for equipment_operation_score
-- ----------------------------
DROP TABLE IF EXISTS `equipment_operation_score`;
CREATE TABLE `equipment_operation_score`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `operation_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '作战任务ID',
  `evaluation_time` datetime NOT NULL COMMENT '评估时间',
  `personnel_personnel_count_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '人员数量得分 (原定量, %)',
  `personnel_work_experience_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '工作经验得分 (原定量, 年)',
  `personnel_training_experience_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '演训经历得分 (原定量, 次)',
  `system_setup_network_setup_time_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '组网时长得分 (原定量, 分钟)',
  `maintenance_maintenance_skill_ql` decimal(5, 2) NULL DEFAULT NULL COMMENT '维修能力得分 (原定性)',
  `maintenance_spare_parts_availability_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '备件充足得分 (原定量, %)',
  `response_emergency_handling_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '应急处理得分 (原定量, 秒)',
  `comm_support_link_maintenance_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '链路维持得分 (原定量, 中断时间占比 %)',
  `comm_support_service_activation_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '业务开通得分 (原定量, 分钟)',
  `comm_support_emergency_restoration_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '应急抢通得分 (原定量, 秒)',
  `comm_attack_target_acquisition_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '干扰目标锁定得分 (原定量, 秒)',
  `comm_attack_jamming_effectiveness_ql` decimal(5, 2) NULL DEFAULT NULL COMMENT '干扰效能达成得分 (原定性)',
  `comm_attack_deception_signal_generation_ql` decimal(5, 2) NULL DEFAULT NULL COMMENT '欺骗信号生成得分 (原定性)',
  `comm_defense_signal_interception_awareness_ql` decimal(5, 2) NULL DEFAULT NULL COMMENT '信号截获感知得分 (原定性)',
  `comm_defense_anti_jamming_operation_ql` decimal(5, 2) NULL DEFAULT NULL COMMENT '抗干扰操作得分 (原定性)',
  `comm_defense_anti_deception_awareness_ql` decimal(5, 2) NULL DEFAULT NULL COMMENT '防骗反骗得分 (原定性)',
  `system_performance_connectivity_rate_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '连通率得分 (原定量, %)',
  `system_performance_mission_reliability_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '任务可靠度得分 (原定量, %)',
  `maintenance_feedback_field_repair_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '抢修能力得分 (原定量, 分钟)',
  `maintenance_feedback_rework_rate_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '返修率得分 (原定量, %)',
  `maintenance_feedback_equipment_feedback_ql` decimal(5, 2) NULL DEFAULT NULL COMMENT '装备反馈得分 (原定性)',
  `remarks` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_operation`(`operation_id` ASC) USING BTREE,
  INDEX `idx_evaluation_time`(`evaluation_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 41 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '装备操作层级评估表（带一级前缀，全量化得分版）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for evaluation_grade_definition
-- ----------------------------
DROP TABLE IF EXISTS `evaluation_grade_definition`;
CREATE TABLE `evaluation_grade_definition`  (
  `grade_id` int NOT NULL AUTO_INCREMENT COMMENT '等级ID',
  `grade_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '等级代码：A+, A, A-, B+, B, B-, C+, C, C-, D+, D, D-, E+, E, E-',
  `grade_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '等级名称：优+, 优, 优-, 良+, 良, 良-, 合格+, 合格, 合格-, 差+, 差, 差-, 极差+, 极差, 极差-',
  `category` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '大类：优/良/合格/差/极差',
  `min_score` decimal(5, 2) NOT NULL COMMENT '最低分（包含）',
  `max_score` decimal(5, 2) NOT NULL COMMENT '最高分（不包含，最高等级除外）',
  `score_range` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分数区间描述：[95,100], [90,95)等',
  `grade_level` int NOT NULL COMMENT '等级排序：1-15，数字越小等级越高',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '等级描述',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`grade_id`) USING BTREE,
  UNIQUE INDEX `grade_code`(`grade_code` ASC) USING BTREE,
  INDEX `idx_grade_code`(`grade_code` ASC) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_grade_level`(`grade_level` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 31 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '评分等级定义表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for expert_ahp_comparison_score
-- ----------------------------
DROP TABLE IF EXISTS `expert_ahp_comparison_score`;
CREATE TABLE `expert_ahp_comparison_score`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `expert_id` int NOT NULL COMMENT '专家id\r\n',
  `expert_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '专家名称',
  `comparison_key` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '比较对标识，如:安全性_可靠性',
  `score` decimal(5, 2) NOT NULL COMMENT '打分值(1-9标度)',
  `confidence` decimal(3, 2) NULL DEFAULT 0.80 COMMENT '把握度(0-1)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2480 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '专家AHP原始打分表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for expert_ahp_group_weights
-- ----------------------------
DROP TABLE IF EXISTS `expert_ahp_group_weights`;
CREATE TABLE `expert_ahp_group_weights`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `group_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '专家组ID（UUID）',
  `expert_ids` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '专家ID集合（按ID排序，逗号分隔），作为唯一标识',
  `expert_count` int NOT NULL COMMENT '参与专家数量',
  `group_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '专家组名称（可选描述）',
  `cr_dim` decimal(8, 6) NULL DEFAULT NULL COMMENT '维度层CR',
  `cr_security` decimal(8, 6) NULL DEFAULT NULL COMMENT '安全性指标层CR',
  `cr_reliability` decimal(8, 6) NULL DEFAULT NULL COMMENT '可靠性指标层CR',
  `cr_transmission` decimal(8, 6) NULL DEFAULT NULL COMMENT '传输能力指标层CR',
  `cr_anti_jamming` decimal(8, 6) NULL DEFAULT NULL COMMENT '抗干扰能力指标层CR',
  `cr_effect` decimal(8, 6) NULL DEFAULT NULL COMMENT '效能影响指标层CR',
  `dim_weight_security` decimal(10, 6) NULL DEFAULT NULL COMMENT '安全性维度权重',
  `dim_weight_reliability` decimal(10, 6) NULL DEFAULT NULL COMMENT '可靠性维度权重',
  `dim_weight_transmission` decimal(10, 6) NULL DEFAULT NULL COMMENT '传输能力维度权重',
  `dim_weight_anti_jamming` decimal(10, 6) NULL DEFAULT NULL COMMENT '抗干扰能力维度权重',
  `dim_weight_effect` decimal(10, 6) NULL DEFAULT NULL COMMENT '效能影响维度权重',
  `ind_weight_key_leakage` decimal(10, 6) NULL DEFAULT NULL COMMENT '密钥泄露得分权重',
  `ind_weight_detected_probability` decimal(10, 6) NULL DEFAULT NULL COMMENT '被侦察得分权重',
  `ind_weight_interception_resistance` decimal(10, 6) NULL DEFAULT NULL COMMENT '抗拦截得分权重',
  `ind_weight_crash_rate` decimal(10, 6) NULL DEFAULT NULL COMMENT '崩溃比例得分权重',
  `ind_weight_recovery_capability` decimal(10, 6) NULL DEFAULT NULL COMMENT '恢复能力得分权重',
  `ind_weight_communication_availability` decimal(10, 6) NULL DEFAULT NULL COMMENT '通信可用得分权重',
  `ind_weight_bandwidth` decimal(10, 6) NULL DEFAULT NULL COMMENT '带宽得分权重',
  `ind_weight_call_setup_time` decimal(10, 6) NULL DEFAULT NULL COMMENT '呼叫建立得分权重',
  `ind_weight_transmission_delay` decimal(10, 6) NULL DEFAULT NULL COMMENT '传输时延得分权重',
  `ind_weight_bit_error_rate` decimal(10, 6) NULL DEFAULT NULL COMMENT '误码率得分权重',
  `ind_weight_throughput` decimal(10, 6) NULL DEFAULT NULL COMMENT '吞吐量得分权重',
  `ind_weight_spectral_efficiency` decimal(10, 6) NULL DEFAULT NULL COMMENT '频谱效率得分权重',
  `ind_weight_sinr` decimal(10, 6) NULL DEFAULT NULL COMMENT '信干噪比得分权重',
  `ind_weight_anti_jamming_margin` decimal(10, 6) NULL DEFAULT NULL COMMENT '抗干扰余量得分权重',
  `ind_weight_communication_distance` decimal(10, 6) NULL DEFAULT NULL COMMENT '通信距离得分权重',
  `ind_weight_damage_rate` decimal(10, 6) NULL DEFAULT NULL COMMENT '战损率得分权重',
  `ind_weight_mission_completion_rate` decimal(10, 6) NULL DEFAULT NULL COMMENT '任务完成率得分权重',
  `ind_weight_blind_rate` decimal(10, 6) NULL DEFAULT NULL COMMENT '致盲率得分权重',
  `expert_weights_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '专家权重明细（JSON格式）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_expert_ids`(`expert_ids` ASC) USING BTREE,
  INDEX `idx_group_id`(`group_id` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '专家集结权重快照' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for expert_ahp_individual_weights
-- ----------------------------
DROP TABLE IF EXISTS `expert_ahp_individual_weights`;
CREATE TABLE `expert_ahp_individual_weights`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `expert_id` bigint NOT NULL,
  `expert_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `ahp_result_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'MatrixCalculationResult JSON（维度层+指标层+综合权重+CR等）',
  `updated_at` datetime NULL DEFAULT NULL,
  `security_weight` decimal(10, 6) NULL DEFAULT NULL COMMENT '一级：安全性',
  `reliability_weight` decimal(10, 6) NULL DEFAULT NULL COMMENT '一级：可靠性',
  `transmission_weight` decimal(10, 6) NULL DEFAULT NULL COMMENT '一级：传输能力',
  `anti_jamming_weight` decimal(10, 6) NULL DEFAULT NULL COMMENT '一级：抗干扰能力',
  `effect_weight` decimal(10, 6) NULL DEFAULT NULL COMMENT '一级：效能影响',
  `security_key_leakage_weight` decimal(10, 6) NULL DEFAULT NULL COMMENT '综合：密钥泄露得分',
  `security_detected_probability_weight` decimal(10, 6) NULL DEFAULT NULL COMMENT '综合：被侦察得分',
  `security_interception_resistance_weight` decimal(10, 6) NULL DEFAULT NULL COMMENT '综合：抗拦截得分',
  `reliability_crash_rate_weight` decimal(10, 6) NULL DEFAULT NULL COMMENT '综合：崩溃比例得分',
  `reliability_recovery_capability_weight` decimal(10, 6) NULL DEFAULT NULL COMMENT '综合：恢复能力得分',
  `reliability_communication_availability_weight` decimal(10, 6) NULL DEFAULT NULL COMMENT '综合：通信可用得分',
  `transmission_bandwidth_weight` decimal(10, 6) NULL DEFAULT NULL COMMENT '综合：带宽得分',
  `transmission_call_setup_time_weight` decimal(10, 6) NULL DEFAULT NULL COMMENT '综合：呼叫建立得分',
  `transmission_transmission_delay_weight` decimal(10, 6) NULL DEFAULT NULL COMMENT '综合：传输时延得分',
  `transmission_bit_error_rate_weight` decimal(10, 6) NULL DEFAULT NULL COMMENT '综合：误码率得分',
  `transmission_throughput_weight` decimal(10, 6) NULL DEFAULT NULL COMMENT '综合：吞吐量得分',
  `transmission_spectral_efficiency_weight` decimal(10, 6) NULL DEFAULT NULL COMMENT '综合：频谱效率得分',
  `anti_jamming_sinr_weight` decimal(10, 6) NULL DEFAULT NULL COMMENT '综合：信干噪比得分',
  `anti_jamming_anti_jamming_margin_weight` decimal(10, 6) NULL DEFAULT NULL COMMENT '综合：抗干扰余量得分',
  `anti_jamming_communication_distance_weight` decimal(10, 6) NULL DEFAULT NULL COMMENT '综合：通信距离得分',
  `effect_damage_rate_weight` decimal(10, 6) NULL DEFAULT NULL COMMENT '综合：战损率得分',
  `effect_mission_completion_rate_weight` decimal(10, 6) NULL DEFAULT NULL COMMENT '综合：任务完成率得分',
  `effect_blind_rate_weight` decimal(10, 6) NULL DEFAULT NULL COMMENT '综合：致盲率得分',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_expert_id`(`expert_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '专家AHP层次总排序权重快照' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for expert_base_info
-- ----------------------------
DROP TABLE IF EXISTS `expert_base_info`;
CREATE TABLE `expert_base_info`  (
  `expert_id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `expert_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '专家姓名',
  `gender` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '性别',
  `birth_date` date NULL DEFAULT NULL COMMENT '出生日期',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '电子邮箱',
  `work_unit` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工作单位',
  `department` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '所在部门',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '职称(教授/研究员/高工等)',
  `title_level` int NULL DEFAULT NULL COMMENT '职称等级(1-初级 2-中级 3-副高 4-正高)',
  `is_academician` tinyint(1) NULL DEFAULT 0 COMMENT '是否为院士',
  `is_yangtze_scholar` tinyint(1) NULL DEFAULT 0 COMMENT '是否为长江学者',
  `is_excellent_youth` tinyint(1) NULL DEFAULT 0 COMMENT '是否为杰青',
  `is_doctoral_supervisor` tinyint(1) NULL DEFAULT 0 COMMENT '是否为博导',
  `is_master_supervisor` tinyint(1) NULL DEFAULT 0 COMMENT '是否为硕导',
  `position` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '职务(主任/院长/所长等)',
  `position_level` int NULL DEFAULT NULL COMMENT '职务等级(1-一般 2-中层 3-高层)',
  `education` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最高学历(本科/硕士/博士)',
  `education_level` int NULL DEFAULT NULL COMMENT '学历等级(1-本科 2-硕士 3-博士)',
  `graduated_school` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '毕业院校',
  `school_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '学校层次(985/211/普通)',
  `major` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '所学专业',
  `work_years` int NULL DEFAULT NULL COMMENT '工作年限',
  `professional_years` int NULL DEFAULT NULL COMMENT '专业工作年限',
  `exercise_experience` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '演习训练经历(JSON格式)',
  `academic_count` int NULL DEFAULT 0 COMMENT '学术论文数量',
  `academic_sci_ei_count` int NULL DEFAULT 0 COMMENT 'SCI/EI论文数量',
  `academic_core_count` int NULL DEFAULT 0 COMMENT '核心期刊论文数量',
  `research_count` int NULL DEFAULT 0 COMMENT '科研项目数量(主持)',
  `research_participate_count` int NULL DEFAULT 0 COMMENT '科研项目数量(参与)',
  `patent_count` int NULL DEFAULT 0 COMMENT '专利数量(授权)',
  `software_copyright_count` int NULL DEFAULT 0 COMMENT '软件著作权数量',
  `monograph_count` int NULL DEFAULT 0 COMMENT '专著/教材数量',
  `award_count` int NULL DEFAULT 0 COMMENT '获奖数量',
  `expertise_area` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '专业领域(JSON数组)',
  `has_military_training` tinyint(1) NULL DEFAULT 0 COMMENT '是否有军事训练学知识',
  `has_system_simulation` tinyint(1) NULL DEFAULT 0 COMMENT '是否有系统仿真知识',
  `has_statistics` tinyint(1) NULL DEFAULT 0 COMMENT '是否有数理统计学知识',
  `military_training_score` int NULL DEFAULT NULL COMMENT '军事训练学考核成绩(0-100)',
  `system_simulation_score` int NULL DEFAULT NULL COMMENT '系统仿真考核成绩(0-100)',
  `statistics_score` int NULL DEFAULT NULL COMMENT '数理统计考核成绩(0-100)',
  `national_exercise_count` int NULL DEFAULT 0 COMMENT '国家级演习次数(主持)',
  `national_exercise_participate_count` int NULL DEFAULT 0 COMMENT '国家级演习次数(参与)',
  `regional_exercise_count` int NULL DEFAULT 0 COMMENT '省级/战区级演习次数(主持)',
  `regional_exercise_participate_count` int NULL DEFAULT 0 COMMENT '省级/战区级演习次数(参与)',
  `military_practice_years` int NULL DEFAULT 0 COMMENT '部队实践/挂职年限',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '状态(1-启用 0-禁用)',
  `remarks` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`expert_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 102 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '专家基础信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for expert_credibility_evaluation_score
-- ----------------------------
DROP TABLE IF EXISTS `expert_credibility_evaluation_score`;
CREATE TABLE `expert_credibility_evaluation_score`  (
  `expert_id` bigint UNSIGNED NOT NULL COMMENT '专家ID(关联expert_base_info)',
  `expert_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '专家姓名',
  `title_ql` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '职称得分',
  `position_ql` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '职务得分',
  `education_experience_ql` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '学习经历得分',
  `academic_achievements_ql` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '学术成果得分',
  `research_achievements_ql` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '科研成果得分',
  `exercise_experience_ql` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '演习训练经历得分',
  `military_training_knowledge_ql` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '军事训练学知识得分',
  `system_simulation_knowledge_ql` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '系统仿真知识得分',
  `statistics_knowledge_ql` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '数理统计学知识得分',
  `professional_years_qt` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '专业年限得分',
  `total_score` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '综合可信度得分',
  `credibility_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '可信度等级(A/B/C/D)',
  `weight_title` decimal(5, 2) NULL DEFAULT 0.10 COMMENT '职称权重',
  `weight_position` decimal(5, 2) NULL DEFAULT 0.08 COMMENT '职务权重',
  `weight_education` decimal(5, 2) NULL DEFAULT 0.08 COMMENT '学习经历权重',
  `weight_academic` decimal(5, 2) NULL DEFAULT 0.10 COMMENT '学术成果权重',
  `weight_research` decimal(5, 2) NULL DEFAULT 0.10 COMMENT '科研成果权重',
  `weight_exercise` decimal(5, 2) NULL DEFAULT 0.10 COMMENT '演习训练权重',
  `weight_military_training` decimal(5, 2) NULL DEFAULT 0.12 COMMENT '军事训练学权重',
  `weight_system_simulation` decimal(5, 2) NULL DEFAULT 0.12 COMMENT '系统仿真权重',
  `weight_statistics` decimal(5, 2) NULL DEFAULT 0.10 COMMENT '数理统计权重',
  `weight_professional_years` decimal(5, 2) NULL DEFAULT 0.10 COMMENT '专业年限权重',
  `evaluation_date` date NULL DEFAULT NULL COMMENT '评估日期',
  `evaluator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '评估人',
  `remarks` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE INDEX `uk_expert_id`(`expert_id` ASC) USING BTREE,
  INDEX `idx_expert_name`(`expert_name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '专家可信度评估得分表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for expert_weighted_evaluation_result
-- ----------------------------
DROP TABLE IF EXISTS `expert_weighted_evaluation_result`;
CREATE TABLE `expert_weighted_evaluation_result`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `evaluation_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评估批次ID',
  `operation_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '作战任务ID',
  `expert_count` int NOT NULL COMMENT '参与专家数量',
  `expert_ids` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '参与专家ID，逗号分隔',
  `cr_dim` decimal(8, 6) NULL DEFAULT NULL COMMENT '维度层CR',
  `cr_security` decimal(8, 6) NULL DEFAULT NULL COMMENT '安全性指标层CR',
  `cr_reliability` decimal(8, 6) NULL DEFAULT NULL COMMENT '可靠性指标层CR',
  `cr_transmission` decimal(8, 6) NULL DEFAULT NULL COMMENT '传输能力指标层CR',
  `cr_anti_jamming` decimal(8, 6) NULL DEFAULT NULL COMMENT '抗干扰能力指标层CR',
  `cr_effect` decimal(8, 6) NULL DEFAULT NULL COMMENT '效能影响指标层CR',
  `dim_weight_security` decimal(10, 6) NULL DEFAULT NULL COMMENT '安全性维度权重',
  `dim_weight_reliability` decimal(10, 6) NULL DEFAULT NULL COMMENT '可靠性维度权重',
  `dim_weight_transmission` decimal(10, 6) NULL DEFAULT NULL COMMENT '传输能力维度权重',
  `dim_weight_anti_jamming` decimal(10, 6) NULL DEFAULT NULL COMMENT '抗干扰能力维度权重',
  `dim_weight_effect` decimal(10, 6) NULL DEFAULT NULL COMMENT '效能影响维度权重',
  `ind_weight_key_leakage` decimal(10, 6) NULL DEFAULT NULL COMMENT '密钥泄露得分权重',
  `ind_weight_detected_probability` decimal(10, 6) NULL DEFAULT NULL COMMENT '被侦察得分权重',
  `ind_weight_interception_resistance` decimal(10, 6) NULL DEFAULT NULL COMMENT '抗拦截得分权重',
  `ind_weight_crash_rate` decimal(10, 6) NULL DEFAULT NULL COMMENT '崩溃比例得分权重',
  `ind_weight_recovery_capability` decimal(10, 6) NULL DEFAULT NULL COMMENT '恢复能力得分权重',
  `ind_weight_communication_availability` decimal(10, 6) NULL DEFAULT NULL COMMENT '通信可用得分权重',
  `ind_weight_bandwidth` decimal(10, 6) NULL DEFAULT NULL COMMENT '带宽得分权重',
  `ind_weight_call_setup_time` decimal(10, 6) NULL DEFAULT NULL COMMENT '呼叫建立得分权重',
  `ind_weight_transmission_delay` decimal(10, 6) NULL DEFAULT NULL COMMENT '传输时延得分权重',
  `ind_weight_bit_error_rate` decimal(10, 6) NULL DEFAULT NULL COMMENT '误码率得分权重',
  `ind_weight_throughput` decimal(10, 6) NULL DEFAULT NULL COMMENT '吞吐量得分权重',
  `ind_weight_spectral_efficiency` decimal(10, 6) NULL DEFAULT NULL COMMENT '频谱效率得分权重',
  `ind_weight_sinr` decimal(10, 6) NULL DEFAULT NULL COMMENT '信干噪比得分权重',
  `ind_weight_anti_jamming_margin` decimal(10, 6) NULL DEFAULT NULL COMMENT '抗干扰余量得分权重',
  `ind_weight_communication_distance` decimal(10, 6) NULL DEFAULT NULL COMMENT '通信距离得分权重',
  `ind_weight_damage_rate` decimal(10, 6) NULL DEFAULT NULL COMMENT '战损率得分权重',
  `ind_weight_mission_completion_rate` decimal(10, 6) NULL DEFAULT NULL COMMENT '任务完成率得分权重',
  `ind_weight_blind_rate` decimal(10, 6) NULL DEFAULT NULL COMMENT '致盲率得分权重',
  `score_key_leakage` decimal(8, 6) NULL DEFAULT NULL COMMENT '密钥泄露加权得分',
  `score_detected_probability` decimal(8, 6) NULL DEFAULT NULL COMMENT '被侦察加权得分',
  `score_interception_resistance` decimal(8, 6) NULL DEFAULT NULL COMMENT '抗拦截加权得分',
  `score_crash_rate` decimal(8, 6) NULL DEFAULT NULL COMMENT '崩溃比例加权得分',
  `score_recovery_capability` decimal(8, 6) NULL DEFAULT NULL COMMENT '恢复能力加权得分',
  `score_communication_availability` decimal(8, 6) NULL DEFAULT NULL COMMENT '通信可用加权得分',
  `score_bandwidth` decimal(8, 6) NULL DEFAULT NULL COMMENT '带宽加权得分',
  `score_call_setup_time` decimal(8, 6) NULL DEFAULT NULL COMMENT '呼叫建立加权得分',
  `score_transmission_delay` decimal(8, 6) NULL DEFAULT NULL COMMENT '传输时延加权得分',
  `score_bit_error_rate` decimal(8, 6) NULL DEFAULT NULL COMMENT '误码率加权得分',
  `score_throughput` decimal(8, 6) NULL DEFAULT NULL COMMENT '吞吐量加权得分',
  `score_spectral_efficiency` decimal(8, 6) NULL DEFAULT NULL COMMENT '频谱效率加权得分',
  `score_sinr` decimal(8, 6) NULL DEFAULT NULL COMMENT '信干噪比加权得分',
  `score_anti_jamming_margin` decimal(8, 6) NULL DEFAULT NULL COMMENT '抗干扰余量加权得分',
  `score_communication_distance` decimal(8, 6) NULL DEFAULT NULL COMMENT '通信距离加权得分',
  `score_damage_rate` decimal(8, 6) NULL DEFAULT NULL COMMENT '战损率加权得分',
  `score_mission_completion_rate` decimal(8, 6) NULL DEFAULT NULL COMMENT '任务完成率加权得分',
  `score_blind_rate` decimal(8, 6) NULL DEFAULT NULL COMMENT '致盲率加权得分',
  `score_security` decimal(8, 6) NULL DEFAULT NULL COMMENT '安全性维度加权得分',
  `score_reliability` decimal(8, 6) NULL DEFAULT NULL COMMENT '可靠性维度加权得分',
  `score_transmission` decimal(8, 6) NULL DEFAULT NULL COMMENT '传输能力维度加权得分',
  `score_anti_jamming` decimal(8, 6) NULL DEFAULT NULL COMMENT '抗干扰能力维度加权得分',
  `score_effect` decimal(8, 6) NULL DEFAULT NULL COMMENT '效能影响维度加权得分',
  `total_score` decimal(10, 6) NULL DEFAULT NULL COMMENT '综合加权得分',
  `expert_weights_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '专家权重明细（JSON格式）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_eval_operation`(`evaluation_id` ASC, `operation_id` ASC) USING BTREE,
  INDEX `idx_evaluation_id`(`evaluation_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 30 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '专家集结加权评估结果' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for metrics_military_comm_effect
-- ----------------------------
DROP TABLE IF EXISTS `metrics_military_comm_effect`;
CREATE TABLE `metrics_military_comm_effect`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `evaluation_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评估批次ID',
  `operation_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '作战任务ID',
  `security_key_leakage_count` decimal(10, 2) NULL DEFAULT NULL COMMENT '密钥泄露次数（次/单位时间）',
  `security_detected_probability` decimal(5, 2) NULL DEFAULT NULL COMMENT '被侦察概率（%）',
  `security_interception_resistance_probability` decimal(5, 2) NULL DEFAULT NULL COMMENT '抗拦截概率（%）',
  `reliability_crash_count` int NULL DEFAULT NULL COMMENT '系统崩溃次数',
  `reliability_recovery_time` decimal(10, 2) NULL DEFAULT NULL COMMENT '恢复时间（ms）',
  `reliability_communication_availability` decimal(5, 2) NULL DEFAULT NULL COMMENT '通信可用率（%）',
  `transmission_bandwidth` decimal(10, 2) NULL DEFAULT NULL COMMENT '带宽（Mbps）',
  `transmission_call_setup_time` decimal(10, 2) NULL DEFAULT NULL COMMENT '呼叫建立时间（ms）',
  `transmission_delay` decimal(10, 2) NULL DEFAULT NULL COMMENT '传输时延（ms）',
  `transmission_bit_error_rate` decimal(10, 6) NULL DEFAULT NULL COMMENT '误码率（%）',
  `transmission_throughput` decimal(10, 2) NULL DEFAULT NULL COMMENT '吞吐量（Mbps）',
  `transmission_spectral_efficiency` decimal(10, 2) NULL DEFAULT NULL COMMENT '频谱效率（bit/Hz）',
  `anti_jamming_sinr` decimal(10, 2) NULL DEFAULT NULL COMMENT '信干噪比（dB）',
  `anti_jamming_margin` decimal(10, 2) NULL DEFAULT NULL COMMENT '抗干扰余量（dB）',
  `anti_jamming_communication_distance` decimal(10, 2) NULL DEFAULT NULL COMMENT '通信距离（km）',
  `effect_damage_rate` decimal(5, 2) NULL DEFAULT NULL COMMENT '战损率（%）',
  `effect_mission_completion_rate` decimal(5, 2) NULL DEFAULT NULL COMMENT '任务完成率（%）',
  `effect_blind_rate` decimal(5, 2) NULL DEFAULT NULL COMMENT '致盲率（%）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_evaluation`(`evaluation_id` ASC) USING BTREE,
  INDEX `idx_operation`(`operation_id` ASC) USING BTREE,
  INDEX `idx_created`(`created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 111 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '军事通信效果指标表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for records_link_maintenance_events
-- ----------------------------
DROP TABLE IF EXISTS `records_link_maintenance_events`;
CREATE TABLE `records_link_maintenance_events`  (
  `event_id` bigint NOT NULL AUTO_INCREMENT COMMENT '事件ID，主键自增',
  `operation_id` int NOT NULL COMMENT '作战ID，关联作战主表',
  `source_node` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '源节点标识（链路中断事件填写）',
  `target_node` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '目标节点标识（链路中断事件填写）',
  `equipment_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '装备/节点标识（维修事件填写）',
  `is_critical_link` tinyint(1) NULL DEFAULT 0 COMMENT '是否为关键链路（1是0否），默认0',
  `interruption_start_ms` bigint NULL DEFAULT NULL COMMENT '中断开始时间（毫秒时间戳）',
  `interruption_end_ms` bigint NULL DEFAULT NULL COMMENT '中断结束时间（毫秒时间戳）',
  `interruption_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '中断原因（如强干扰、设备故障、节点损毁等）',
  `interruption_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '中断类型（自然衰减/干扰/故障/损毁）',
  `recovery_start_ms` bigint NULL DEFAULT NULL COMMENT '应急抢通开始时间（毫秒时间戳）',
  `recovery_end_ms` bigint NULL DEFAULT NULL COMMENT '应急抢通完成时间（毫秒时间戳）',
  `recovery_duration_ms` bigint NULL DEFAULT NULL COMMENT '抢通耗时（毫秒）',
  `recovery_method` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备用手段（如跳频切换、卫星备用、有线备用等）',
  `recovery_success` tinyint(1) NULL DEFAULT NULL COMMENT '是否成功恢复（1成功，0失败）',
  `maintenance_required` tinyint(1) NULL DEFAULT 0 COMMENT '是否需要维修（1是，0否），默认0',
  `maintenance_start_ms` bigint NULL DEFAULT NULL COMMENT '维修开始时间（毫秒时间戳）',
  `maintenance_end_ms` bigint NULL DEFAULT NULL COMMENT '维修结束时间（毫秒时间戳）',
  `maintenance_duration_ms` bigint NULL DEFAULT NULL COMMENT '维修耗时（毫秒）',
  `maintenance_success` tinyint(1) NULL DEFAULT NULL COMMENT '是否成功修复（1成功，0失败）',
  `failure_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '故障原因（维修类型填写）',
  `repair_method` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '维修措施（维修类型填写）',
  `operator_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作员/维修人员ID',
  `feedback_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '反馈内容（可选）',
  `feedback_submitted` tinyint(1) NULL DEFAULT NULL COMMENT '是否已按规定渠道上报（1是0否）',
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间，默认当前时间戳',
  PRIMARY KEY (`event_id`) USING BTREE,
  INDEX `idx_operation_id`(`operation_id` ASC) USING BTREE,
  INDEX `idx_source_target_node`(`source_node` ASC, `target_node` ASC) USING BTREE,
  INDEX `idx_equipment_id`(`equipment_id` ASC) USING BTREE,
  INDEX `idx_interruption_time`(`interruption_start_ms` ASC, `interruption_end_ms` ASC) USING BTREE,
  INDEX `idx_maintenance_time`(`maintenance_start_ms` ASC, `maintenance_end_ms` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 413 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '链路中断与维修事件表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for records_military_communication_info
-- ----------------------------
DROP TABLE IF EXISTS `records_military_communication_info`;
CREATE TABLE `records_military_communication_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `operation_id` int NOT NULL COMMENT '作战ID',
  `src_node_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '源节点标识',
  `dst_node_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '目标节点标识',
  `comm_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通信类型（无线电/卫星/微波/扩频/移动通信）',
  `start_time_ms` bigint NULL DEFAULT NULL COMMENT '本次通信开始时间（毫秒时间戳）',
  `end_time_ms` bigint NULL DEFAULT NULL COMMENT '本次通信结束时间（毫秒时间戳）',
  `call_req_ms` bigint NULL DEFAULT NULL COMMENT '呼叫请求时间（毫秒时间戳）',
  `call_resp_ms` bigint NULL DEFAULT NULL COMMENT '呼叫响应时间（毫秒时间戳）',
  `call_setup_ms` decimal(12, 3) NULL DEFAULT NULL COMMENT '呼叫建立时长（毫秒）',
  `call_success` tinyint(1) NULL DEFAULT NULL COMMENT '呼叫是否成功（1成功，0失败）',
  `msg_bytes` int NULL DEFAULT NULL COMMENT '电文大小（字节）',
  `trans_delay_ms` decimal(12, 3) NULL DEFAULT NULL COMMENT '传输时延（毫秒）',
  `bandwidth_hz` decimal(15, 2) NULL DEFAULT NULL COMMENT '信道带宽（Hz）',
  `snr_db` decimal(8, 2) NULL DEFAULT NULL COMMENT '瞬时信噪比（dB）',
  `throughput_bps` decimal(15, 2) NULL DEFAULT NULL COMMENT '瞬时吞吐量（bps）',
  `tx_power_dbm` decimal(8, 2) NULL DEFAULT NULL COMMENT '发射功率（dBm）',
  `rx_power_dbm` decimal(10, 2) NULL DEFAULT NULL COMMENT '接收信号功率（dBm）',
  `distance_km` decimal(10, 2) NULL DEFAULT NULL COMMENT '通信距离（km）',
  `total_bits` bigint NULL DEFAULT NULL COMMENT '本次传输比特数',
  `error_bits` bigint NULL DEFAULT NULL COMMENT '本次错误比特数',
  `packets_sent` int NULL DEFAULT NULL COMMENT '本次发送包数',
  `packets_lost` int NULL DEFAULT NULL COMMENT '本次丢包数',
  `fail_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '失败原因',
  `retry_cnt` int NULL DEFAULT 0 COMMENT '重试次数（应急处置），默认0',
  `noise_power_dbm` decimal(10, 2) NULL DEFAULT NULL COMMENT '瞬时噪声功率（dBm）',
  `jamming_power_dbm` decimal(10, 2) NULL DEFAULT NULL COMMENT '瞬时干扰功率（dBm）',
  `sinr_db` decimal(8, 2) NULL DEFAULT NULL COMMENT '瞬时SINR（dB）',
  `jamming_margin_db` decimal(8, 2) NULL DEFAULT NULL COMMENT '抗干扰余量（dB）',
  `detected` tinyint(1) NULL DEFAULT 0 COMMENT '是否被侦察（1是，0否），默认0',
  `intercepted` tinyint(1) NULL DEFAULT 0 COMMENT '是否被拦截（1是，0否），默认0',
  `operator_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作员ID',
  `operator_reaction_ms` decimal(10, 2) NULL DEFAULT NULL COMMENT '操作员反应时间（毫秒）',
  `op_success` tinyint(1) NULL DEFAULT NULL COMMENT '操作是否成功（1成功，0失败）',
  `comm_success` tinyint(1) NULL DEFAULT NULL COMMENT '本次通信是否成功（1成功，0失败）',
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `recorded_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间，默认当前时间戳',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_operation_id`(`operation_id` ASC) USING BTREE,
  INDEX `idx_comm_type`(`comm_type` ASC) USING BTREE,
  INDEX `idx_start_time`(`start_time_ms` ASC) USING BTREE,
  INDEX `idx_src_node`(`src_node_id` ASC) USING BTREE,
  INDEX `idx_dst_node`(`dst_node_id` ASC) USING BTREE,
  INDEX `idx_comm_success`(`comm_success` ASC) USING BTREE,
  INDEX `idx_recorded_at`(`recorded_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1489 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '通信基础信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for records_military_operation_info
-- ----------------------------
DROP TABLE IF EXISTS `records_military_operation_info`;
CREATE TABLE `records_military_operation_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `operation_id` int NOT NULL COMMENT '作战ID',
  `avg_network_setup_time_ms` bigint NULL DEFAULT NULL COMMENT '平均组网时长（毫秒）',
  `total_node_count` int NOT NULL DEFAULT 0 COMMENT '总节点数量（组网规模）',
  `isolated_node_count` int NOT NULL DEFAULT 0 COMMENT '孤立节点数量（无法通信）',
  `actual_connections` int NULL DEFAULT NULL COMMENT '实际连接数',
  `command_personnel_count` int NULL DEFAULT NULL COMMENT '指挥人员数量',
  `operator_personnel_count` int NULL DEFAULT NULL COMMENT '操作人员数量',
  `maintenance_personnel_count` int NULL DEFAULT NULL COMMENT '维修人员数量',
  `avg_experience_years` decimal(4, 1) NULL DEFAULT NULL COMMENT '操作人员平均工作经验（年）',
  `annual_maintenance_hours` bigint NULL DEFAULT NULL COMMENT '战役级装备维修工时（年总工时）',
  `avg_training_frequency_per_year` decimal(5, 2) NULL DEFAULT NULL COMMENT '人均培训频次（每年参加重大演训次数）',
  `total_equipment_count` int NOT NULL DEFAULT 0 COMMENT '总装备数量',
  `damaged_equipment_count` int NOT NULL DEFAULT 0 COMMENT '受损装备数量（未恢复）',
  `new_equipment_ratio` decimal(5, 2) NULL DEFAULT NULL COMMENT '新装备占比（%）',
  `total_power_consumption_kw` decimal(10, 2) NULL DEFAULT NULL COMMENT '整体功耗（千瓦）',
  `annual_electricity_consumption_kwh` decimal(12, 2) NULL DEFAULT NULL COMMENT '总耗电量（kWh）',
  `annual_fuel_consumption_liters` decimal(12, 2) NULL DEFAULT NULL COMMENT '总耗油量（升）',
  `spectrum_reserve_mhz` decimal(10, 2) NULL DEFAULT NULL COMMENT '频谱储备（MHz）',
  `spare_parts_satisfaction_rate` decimal(5, 2) NULL DEFAULT NULL COMMENT '备件满足率（%）',
  `total_transport_distance_km` decimal(10, 2) NULL DEFAULT NULL COMMENT '总运输路程（公里）',
  `avg_altitude_m` decimal(7, 2) NULL DEFAULT NULL COMMENT '平均海拔（米）',
  `weather_condition` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '天气条件',
  `temperature_celsius` decimal(4, 1) NULL DEFAULT NULL COMMENT '温度（摄氏度）',
  `electromagnetic_interference_level` tinyint NULL DEFAULT NULL COMMENT '电磁干扰等级（1-5级）',
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `recorded_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_operation_id`(`operation_id` ASC) USING BTREE,
  INDEX `idx_recorded_at`(`recorded_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 156 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '作战信息基础表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for records_security_events
-- ----------------------------
DROP TABLE IF EXISTS `records_security_events`;
CREATE TABLE `records_security_events`  (
  `event_id` bigint NOT NULL AUTO_INCREMENT COMMENT '事件ID，主键自增',
  `operation_id` int NOT NULL COMMENT '作战ID，关联作战主表',
  `event_type` enum('key_leak','detected_by_enemy','intercepted','interception_attempt') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '事件类型（密钥泄露、被侦察、被拦截、拦截尝试）',
  `event_time_ms` bigint NOT NULL COMMENT '事件发生时间（毫秒时间戳）',
  `node_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '涉及节点标识',
  `key_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '密钥标识（如有）',
  `key_age_ms` bigint NULL DEFAULT NULL COMMENT '密钥使用时长（毫秒）',
  `detected_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '检测方（如蓝军报告、系统自检等）',
  `intercepted_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '被拦截内容摘要（可选）',
  `intercept_method` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '拦截方式（如侧信道、破译等）',
  `interception_attempt_source` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '尝试拦截的来源（如蓝军设备ID）',
  `impact_level` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '影响等级（如高、中、低）',
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  PRIMARY KEY (`event_id`) USING BTREE,
  INDEX `idx_operation_id`(`operation_id` ASC) USING BTREE,
  INDEX `idx_event_type`(`event_type` ASC) USING BTREE,
  INDEX `idx_event_time_ms`(`event_time_ms` ASC) USING BTREE,
  INDEX `idx_node_id`(`node_id` ASC) USING BTREE,
  INDEX `idx_key_id`(`key_id` ASC) USING BTREE,
  INDEX `idx_operation_time`(`operation_id` ASC, `event_time_ms` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 494 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '安全事件表，记录密钥泄露、被拦截等安全事件' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for score_military_comm_effect
-- ----------------------------
DROP TABLE IF EXISTS `score_military_comm_effect`;
CREATE TABLE `score_military_comm_effect`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `evaluation_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评估批次ID',
  `operation_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '作战任务ID（与各层级表关联）',
  `security_key_leakage_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '密钥泄露得分',
  `security_detected_probability_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '被侦察得分',
  `security_interception_resistance_ql` decimal(5, 2) NULL DEFAULT NULL COMMENT '抗拦截得分',
  `reliability_crash_rate_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '崩溃比例得分',
  `reliability_recovery_capability_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '恢复能力得分',
  `reliability_communication_availability_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '通信可用得分',
  `transmission_bandwidth_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '带宽得分',
  `transmission_call_setup_time_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '呼叫建立得分',
  `transmission_transmission_delay_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '传输时延得分',
  `transmission_bit_error_rate_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '误码率得分',
  `transmission_throughput_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '吞吐量得分',
  `transmission_spectral_efficiency_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '频谱效率得分',
  `anti_jamming_sinr_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '信干噪比得分',
  `anti_jamming_anti_jamming_margin_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '抗干扰余量得分',
  `anti_jamming_communication_distance_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '通信距离得分',
  `effect_damage_rate_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '战损率得分',
  `effect_mission_completion_rate_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '任务完成率得分',
  `effect_blind_rate_qt` decimal(5, 2) NULL DEFAULT NULL COMMENT '致盲率（%）得分',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_operation`(`operation_id` ASC) USING BTREE,
  INDEX `idx_created`(`created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 100 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '军事作战效果指标表（带一级指标前缀，全量化得分版）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- View structure for v_evaluation_object_summary
-- ----------------------------
DROP VIEW IF EXISTS `v_evaluation_object_summary`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY DEFINER VIEW `v_evaluation_object_summary` AS select `e`.`evaluation_object_id` AS `evaluation_object_id`,`e`.`evaluation_object_type` AS `evaluation_object_type`,`e`.`evaluation_dimension` AS `evaluation_dimension`,`e`.`evaluation_batch` AS `evaluation_batch`,count(distinct `e`.`expert_id`) AS `expert_count`,avg(`e`.`score_value`) AS `avg_score`,min(`e`.`score_value`) AS `min_score`,max(`e`.`score_value`) AS `max_score`,std(`e`.`score_value`) AS `score_stddev`,(sum((`e`.`score_value` * `ec`.`综合得分`)) / sum(`ec`.`综合得分`)) AS `weighted_avg_score`,group_concat(distinct `e`.`grade_code` order by `e`.`grade_code` ASC separator ',') AS `grade_distribution`,max(`e`.`evaluation_date`) AS `evaluation_date` from (`expert_evaluation_scores` `e` left join (select `expert_credibility_evaluation_score`.`id` AS `id`,`expert_credibility_evaluation_score`.`expert_name` AS `expert_name`,((((((((((`expert_credibility_evaluation_score`.`title_ql` + `expert_credibility_evaluation_score`.`position_ql`) + `expert_credibility_evaluation_score`.`education_experience_ql`) + `expert_credibility_evaluation_score`.`academic_achievements_ql`) + `expert_credibility_evaluation_score`.`research_achievements_ql`) + `expert_credibility_evaluation_score`.`exercise_experience_ql`) + `expert_credibility_evaluation_score`.`military_training_knowledge_ql`) + `expert_credibility_evaluation_score`.`system_simulation_knowledge_ql`) + `expert_credibility_evaluation_score`.`statistics_knowledge_ql`) + `expert_credibility_evaluation_score`.`professional_years_qt`) / 10) AS `综合得分` from `expert_credibility_evaluation_score`) `ec` on((`e`.`expert_id` = `ec`.`id`))) group by `e`.`evaluation_object_id`,`e`.`evaluation_object_type`,`e`.`evaluation_dimension`,`e`.`evaluation_batch`;

-- ----------------------------
-- View structure for v_expert_evaluation_statistics
-- ----------------------------
DROP VIEW IF EXISTS `v_expert_evaluation_statistics`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY DEFINER VIEW `v_expert_evaluation_statistics` AS select `e`.`expert_id` AS `expert_id`,`e`.`expert_name` AS `expert_name`,count(0) AS `total_evaluations`,avg(`e`.`score_value`) AS `avg_score`,min(`e`.`score_value`) AS `min_score`,max(`e`.`score_value`) AS `max_score`,std(`e`.`score_value`) AS `score_stddev`,sum((case when (`e`.`category` = '优') then 1 else 0 end)) AS `count_excellent`,sum((case when (`e`.`category` = '良') then 1 else 0 end)) AS `count_good`,sum((case when (`e`.`category` = '合格') then 1 else 0 end)) AS `count_pass`,sum((case when (`e`.`category` = '差') then 1 else 0 end)) AS `count_poor`,sum((case when (`e`.`category` = '极差') then 1 else 0 end)) AS `count_very_poor`,avg(`e`.`confidence_level`) AS `avg_confidence`,max(`e`.`evaluation_date`) AS `last_evaluation_date` from `expert_evaluation_scores` `e` group by `e`.`expert_id`,`e`.`expert_name`;

SET FOREIGN_KEY_CHECKS = 1;
