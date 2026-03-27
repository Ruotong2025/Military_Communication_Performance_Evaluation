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

 Date: 26/03/2026 19:53:38
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ahp_equipment_operation_weights
-- ----------------------------
DROP TABLE IF EXISTS `ahp_equipment_operation_weights`;
CREATE TABLE `ahp_equipment_operation_weights`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `batch_id` varchar(50) CHARACTER SET utf8mb4  NOT NULL COMMENT '评估批次ID（例如：AHP-2025-01），用于区分不同次权重计算',
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
  `remarks` text CHARACTER SET utf8mb4  NULL COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_operation`(`batch_id` ASC) USING BTREE,
  INDEX `idx_evaluation_time`(`evaluation_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4  COMMENT = '装备操作层级AHP权重表（含维度权重和指标权重及置信度）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ahp_equipment_operation_weights
-- ----------------------------
INSERT INTO `ahp_equipment_operation_weights` VALUES (1, 'AHP-2026-002', '2026-03-12 16:29:24', 7, 0.7714, 3, 0.7640, 6, 0.8796, 5, 0.8438, 2, 0.9003, 3, 0.8403, 9, 0.9430, 3, 0.8934, 8, 0.7938, 1, 0.8870, 9, 0.7245, 2, 0.8612, 8, 0.8087, 5, 0.8520, 6, 0.6837, 6, 0.7272, 2, 0.8045, 10, 0.6616, 4, 0.7352, 3, 0.7500, 3, 0.7386, 8, 0.8370, 6, 0.7768, 5, 0.6477, 6, 0.8119, 3, 0.7240, 7, 0.6231, 9, 0.6089, 7, 0.7171, 7, 0.8681, '专家张明远对设备操作效能评估的AHP权重打分，各指标权重已归一化处理。', '2026-03-12 16:29:23', '2026-03-12 16:42:42');
INSERT INTO `ahp_equipment_operation_weights` VALUES (2, 'AHP-2026-002', '2026-03-12 16:29:24', 3, 0.9436, 10, 0.6967, 7, 0.6589, 2, 0.7664, 7, 0.7341, 3, 0.6659, 6, 0.7525, 8, 0.7916, 1, 0.7740, 3, 0.9074, 4, 0.7257, 6, 0.7574, 4, 0.8675, 4, 0.6792, 10, 0.8120, 3, 0.9027, 1, 0.7530, 7, 0.7159, 3, 0.7833, 2, 0.6547, 2, 0.7693, 5, 0.7348, 8, 0.7499, 1, 0.6142, 1, 0.8009, 3, 0.6725, 10, 0.8545, 8, 0.6682, 3, 0.7137, 6, 0.8763, '专家李建伟对设备操作效能评估的AHP权重打分，各指标权重已归一化处理。', '2026-03-12 16:29:23', '2026-03-12 16:42:42');
INSERT INTO `ahp_equipment_operation_weights` VALUES (3, 'AHP-2026-002', '2026-03-12 16:29:24', 2, 0.8092, 2, 0.8680, 10, 0.8209, 5, 0.8991, 10, 0.8267, 1, 0.6299, 6, 0.8971, 6, 0.7031, 5, 0.8808, 6, 0.7882, 3, 0.8443, 6, 0.6889, 6, 0.6202, 9, 0.8232, 10, 0.7852, 2, 0.6390, 8, 0.6924, 6, 0.6653, 5, 0.8647, 7, 0.6976, 7, 0.7171, 8, 0.8168, 10, 0.8248, 2, 0.6834, 10, 0.6407, 9, 0.8851, 8, 0.9068, 4, 0.7911, 4, 0.6084, 5, 0.7521, '专家王建国对设备操作效能评估的AHP权重打分，各指标权重已归一化处理。', '2026-03-12 16:29:23', '2026-03-12 16:42:42');
INSERT INTO `ahp_equipment_operation_weights` VALUES (4, 'AHP-2026-002', '2026-03-12 16:29:24', 9, 0.9211, 8, 0.6523, 2, 0.6856, 6, 0.8197, 10, 0.8417, 3, 0.7342, 2, 0.7406, 5, 0.7965, 6, 0.6899, 9, 0.8511, 4, 0.8675, 4, 0.8196, 1, 0.6958, 7, 0.7722, 4, 0.8548, 5, 0.7134, 8, 0.7317, 4, 0.7623, 3, 0.6070, 9, 0.8400, 7, 0.7755, 4, 0.8397, 6, 0.7061, 2, 0.9255, 6, 0.8489, 7, 0.8069, 7, 0.9402, 10, 0.6664, 3, 0.6490, 10, 0.8700, '专家刘志华对设备操作效能评估的AHP权重打分，各指标权重已归一化处理。', '2026-03-12 16:29:23', '2026-03-13 12:01:48');
INSERT INTO `ahp_equipment_operation_weights` VALUES (5, 'AHP-2026-002', '2026-03-12 16:29:24', 10, 0.6664, 9, 0.7431, 7, 0.7908, 9, 0.8061, 10, 0.9302, 4, 0.7879, 8, 0.7911, 1, 0.6221, 9, 0.6112, 5, 0.9057, 6, 0.8219, 8, 0.7396, 1, 0.7795, 5, 0.7920, 8, 0.6462, 5, 0.6825, 2, 0.7088, 10, 0.6296, 1, 0.6080, 9, 0.6372, 10, 0.6817, 2, 0.7371, 7, 0.8780, 1, 0.7265, 8, 0.8222, 5, 0.6489, 6, 0.8250, 8, 0.8604, 2, 0.8411, 7, 0.8504, '专家陈国强对设备操作效能评估的AHP权重打分，各指标权重已归一化处理。', '2026-03-12 16:29:23', '2026-03-12 16:42:43');
INSERT INTO `ahp_equipment_operation_weights` VALUES (6, 'AHP-2026-002', '2026-03-12 16:29:24', 8, 0.7130, 10, 0.6648, 8, 0.8567, 4, 0.6677, 4, 0.7399, 8, 0.8367, 7, 0.8690, 2, 0.8840, 4, 0.8213, 2, 0.9051, 3, 0.7733, 2, 0.8815, 2, 0.6867, 6, 0.8235, 8, 0.6454, 7, 0.6188, 2, 0.6865, 7, 0.6915, 9, 0.8060, 6, 0.7279, 7, 0.7842, 9, 0.7898, 10, 0.8957, 1, 0.6463, 8, 0.7141, 5, 0.8346, 2, 0.8379, 8, 0.6859, 8, 0.7288, 7, 0.7582, '专家赵文军对设备操作效能评估的AHP权重打分，各指标权重已归一化处理。', '2026-03-12 16:29:23', '2026-03-12 16:42:43');
INSERT INTO `ahp_equipment_operation_weights` VALUES (7, 'AHP-2026-002', '2026-03-12 16:29:24', 1, 0.6242, 1, 0.8115, 1, 0.8441, 10, 0.6576, 4, 0.7264, 2, 0.8516, 5, 0.7678, 1, 0.8850, 10, 0.8125, 3, 0.8956, 5, 0.8910, 2, 0.8903, 4, 0.8741, 5, 0.7437, 8, 0.7981, 7, 0.9249, 6, 0.8626, 8, 0.8160, 10, 0.7670, 5, 0.6570, 2, 0.6400, 7, 0.6280, 1, 0.7936, 9, 0.8311, 7, 0.6304, 2, 0.7617, 8, 0.6146, 7, 0.8645, 10, 0.8892, 1, 0.8727, '专家黄旭东对设备操作效能评估的AHP权重打分，各指标权重已归一化处理。', '2026-03-12 16:29:23', '2026-03-12 16:42:43');
INSERT INTO `ahp_equipment_operation_weights` VALUES (8, 'AHP-2026-002', '2026-03-12 16:29:24', 7, 0.7999, 1, 0.7683, 2, 0.8454, 3, 0.8138, 10, 0.6022, 4, 0.7003, 9, 0.9484, 6, 0.8163, 1, 0.7902, 2, 0.8676, 10, 0.6694, 2, 0.9069, 5, 0.6456, 9, 0.7112, 1, 0.6428, 2, 0.7279, 8, 0.7794, 10, 0.7160, 2, 0.8899, 5, 0.9187, 10, 0.6316, 10, 0.6710, 4, 0.7296, 7, 0.7712, 1, 0.6230, 7, 0.6674, 8, 0.8644, 8, 0.7799, 2, 0.6270, 3, 0.6267, '专家周明辉对设备操作效能评估的AHP权重打分，各指标权重已归一化处理。', '2026-03-12 16:29:23', '2026-03-12 16:42:43');
INSERT INTO `ahp_equipment_operation_weights` VALUES (9, 'AHP-2026-002', '2026-03-12 16:29:24', 9, 0.8221, 8, 0.8119, 6, 0.7537, 1, 0.8239, 6, 0.9413, 5, 0.8722, 8, 0.8032, 8, 0.8698, 1, 0.7861, 1, 0.6598, 7, 0.8177, 10, 0.6770, 1, 0.6357, 5, 0.6962, 5, 0.6307, 10, 0.7600, 8, 0.6201, 2, 0.6440, 8, 0.6615, 3, 0.8615, 10, 0.6036, 3, 0.6134, 3, 0.6096, 5, 0.8122, 6, 0.8849, 8, 0.7532, 10, 0.7355, 5, 0.7028, 8, 0.7713, 6, 0.8114, '专家吴建平对设备操作效能评估的AHP权重打分，各指标权重已归一化处理。', '2026-03-12 16:29:23', '2026-03-12 16:42:43');
INSERT INTO `ahp_equipment_operation_weights` VALUES (10, 'AHP-2026-002', '2026-03-12 16:29:24', 7, 0.8232, 6, 0.6641, 8, 0.7106, 7, 0.8959, 3, 0.7669, 8, 0.7634, 5, 0.6986, 6, 0.6920, 2, 0.8816, 4, 0.7187, 4, 0.6116, 2, 0.7136, 7, 0.6520, 9, 0.8396, 9, 0.8514, 6, 0.6686, 6, 0.7478, 8, 0.7769, 10, 0.7188, 8, 0.7083, 6, 0.6033, 8, 0.6541, 2, 0.6405, 4, 0.6330, 3, 0.8915, 8, 0.7920, 7, 0.7068, 7, 0.8047, 1, 0.7003, 1, 0.8352, '专家郑晓峰对设备操作效能评估的AHP权重打分，各指标权重已归一化处理。', '2026-03-12 16:29:23', '2026-03-12 16:42:43');

-- ----------------------------
-- Table structure for ahp_expert_military_operation_effect_weights
-- ----------------------------
DROP TABLE IF EXISTS `ahp_expert_military_operation_effect_weights`;
CREATE TABLE `ahp_expert_military_operation_effect_weights`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `batch_id` varchar(50) CHARACTER SET utf8mb4  NOT NULL COMMENT '评估批次ID（例如：AHP-2025-01），用于区分不同次权重计算',
  `expert_name` varchar(100) CHARACTER SET utf8mb4  NOT NULL COMMENT '专家姓名或编号',
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
  `remarks` text CHARACTER SET utf8mb4  NULL COMMENT '备注（可选）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_batch`(`batch_id` ASC) USING BTREE,
  INDEX `idx_expert`(`expert_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 131 CHARACTER SET = utf8mb4  COMMENT = 'AHP专家权重打分表（直接评分法，含把握度）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ahp_expert_military_operation_effect_weights
-- ----------------------------
INSERT INTO `ahp_expert_military_operation_effect_weights` VALUES (91, 'AHP-2026-002', '张军', 7.00, 0.73, 5.00, 0.79, 6.00, 0.75, 9.00, 0.70, 6.00, 0.75, 9.00, 0.68, 6.00, 0.76, 3.00, 0.82, 5.00, 0.66, 5.00, 0.73, 6.00, 0.77, 5.00, 0.80, 8.00, 0.72, 9.00, 0.68, 6.00, 0.84, 5.00, 0.66, 2.00, 0.65, 8.00, 0.72, 10.00, 0.81, 5.00, 0.77, '批次AHP-2026-002，专家：张军，平均权重：6.25，专家CV：15.25%（正确方法）', '2026-03-06 19:45:57', '2026-03-06 19:48:52');
INSERT INTO `ahp_expert_military_operation_effect_weights` VALUES (92, 'AHP-2026-002', '李建国', 4.00, 0.67, 6.00, 0.76, 8.00, 0.66, 9.00, 0.68, 7.00, 0.73, 8.00, 0.72, 4.00, 0.79, 4.00, 0.68, 5.00, 0.66, 7.00, 0.70, 5.00, 0.76, 5.00, 0.67, 7.00, 0.85, 8.00, 0.85, 8.00, 0.68, 5.00, 0.71, 4.00, 0.66, 8.00, 0.80, 7.00, 0.72, 9.00, 0.76, '批次AHP-2026-002，专家：李建国，平均权重：5.95，专家CV：22.79%（正确方法）', '2026-03-06 19:45:57', '2026-03-07 17:01:33');
INSERT INTO `ahp_expert_military_operation_effect_weights` VALUES (93, 'AHP-2026-002', '王海峰', 8.00, 0.77, 5.00, 0.77, 8.00, 0.79, 8.00, 0.66, 7.00, 0.68, 9.00, 0.68, 4.00, 0.66, 3.00, 0.79, 7.00, 0.84, 7.00, 0.78, 4.00, 0.75, 6.00, 0.79, 9.00, 0.68, 7.00, 0.83, 6.00, 0.82, 3.00, 0.77, 3.00, 0.66, 9.00, 0.76, 8.00, 0.78, 4.00, 0.72, '批次AHP-2026-002，专家：王海峰，平均权重：6.25，专家CV：15.43%（正确方法）', '2026-03-06 19:45:57', '2026-03-06 19:48:52');
INSERT INTO `ahp_expert_military_operation_effect_weights` VALUES (94, 'AHP-2026-002', '刘芳', 7.00, 0.67, 4.00, 0.74, 8.00, 0.75, 9.00, 0.66, 7.00, 0.71, 8.00, 0.81, 5.00, 0.73, 2.00, 0.83, 6.00, 0.70, 7.00, 0.83, 5.00, 0.69, 6.00, 0.76, 6.00, 0.72, 7.00, 0.84, 8.00, 0.75, 1.00, 0.75, 4.00, 0.71, 8.00, 0.81, 8.00, 0.66, 4.00, 0.73, '批次AHP-2026-002，专家：刘芳，平均权重：6.00，专家CV：17.31%（正确方法）', '2026-03-06 19:45:57', '2026-03-06 19:48:52');
INSERT INTO `ahp_expert_military_operation_effect_weights` VALUES (95, 'AHP-2026-002', '陈伟', 6.00, 0.82, 7.00, 0.83, 8.00, 0.68, 9.00, 0.72, 6.00, 0.66, 9.00, 0.65, 5.00, 0.71, 5.00, 0.72, 5.00, 0.81, 6.00, 0.72, 5.00, 0.75, 4.00, 0.71, 9.00, 0.74, 6.00, 0.75, 7.00, 0.73, 2.00, 0.79, 3.00, 0.83, 7.00, 0.71, 9.00, 0.68, 5.00, 0.77, '批次AHP-2026-002，专家：陈伟，平均权重：6.15，专家CV：13.89%（正确方法）', '2026-03-06 19:45:57', '2026-03-06 19:48:52');
INSERT INTO `ahp_expert_military_operation_effect_weights` VALUES (96, 'AHP-2026-002', '赵敏', 6.00, 0.81, 6.00, 0.69, 7.00, 0.74, 9.00, 0.71, 7.00, 0.67, 8.00, 0.78, 6.00, 0.84, 5.00, 0.69, 6.00, 0.68, 6.00, 0.75, 7.00, 0.73, 5.00, 0.75, 8.00, 0.73, 6.00, 0.83, 5.00, 0.84, 4.00, 0.74, 2.00, 0.83, 9.00, 0.80, 9.00, 0.75, 6.00, 0.72, '批次AHP-2026-002，专家：赵敏，平均权重：6.35，专家CV：14.03%（正确方法）', '2026-03-06 19:45:57', '2026-03-06 19:48:52');
INSERT INTO `ahp_expert_military_operation_effect_weights` VALUES (97, 'AHP-2026-002', '孙强', 5.00, 0.76, 6.00, 0.77, 6.00, 0.68, 8.00, 0.72, 7.00, 0.77, 8.00, 0.72, 8.00, 0.70, 3.00, 0.75, 7.00, 0.70, 6.00, 0.73, 6.00, 0.70, 4.00, 0.80, 7.00, 0.75, 7.00, 0.81, 8.00, 0.84, 3.00, 0.68, 2.00, 0.71, 8.00, 0.74, 9.00, 0.82, 4.00, 0.84, '批次AHP-2026-002，专家：孙强，平均权重：6.10，专家CV：15.20%（正确方法）', '2026-03-06 19:45:57', '2026-03-06 19:48:52');
INSERT INTO `ahp_expert_military_operation_effect_weights` VALUES (98, 'AHP-2026-002', '周婷', 6.00, 0.78, 6.00, 0.70, 6.00, 0.74, 8.00, 0.84, 7.00, 0.80, 8.00, 0.67, 6.00, 0.82, 5.00, 0.70, 4.00, 0.81, 5.00, 0.72, 5.00, 0.76, 6.00, 0.76, 8.00, 0.72, 6.00, 0.74, 6.00, 0.83, 3.00, 0.73, 5.00, 0.79, 10.00, 0.84, 10.00, 0.82, 5.00, 0.80, '批次AHP-2026-002，专家：周婷，平均权重：6.25，专家CV：16.22%（正确方法）', '2026-03-06 19:45:57', '2026-03-06 19:48:52');
INSERT INTO `ahp_expert_military_operation_effect_weights` VALUES (99, 'AHP-2026-002', '吴磊', 7.00, 0.76, 5.00, 0.78, 8.00, 0.68, 8.00, 0.69, 6.00, 0.66, 8.00, 0.74, 7.00, 0.84, 5.00, 0.79, 6.00, 0.76, 5.00, 0.65, 4.00, 0.66, 2.00, 0.76, 8.00, 0.79, 8.00, 0.73, 6.00, 0.77, 4.00, 0.79, 3.00, 0.77, 8.00, 0.83, 9.00, 0.76, 4.00, 0.78, '批次AHP-2026-002，专家：吴磊，平均权重：6.05，专家CV：15.83%（正确方法）', '2026-03-06 19:45:57', '2026-03-06 19:48:52');
INSERT INTO `ahp_expert_military_operation_effect_weights` VALUES (100, 'AHP-2026-002', '郑雪', 6.00, 0.78, 5.00, 0.92, 7.00, 0.86, 9.00, 0.80, 7.00, 0.95, 8.00, 0.87, 6.00, 0.90, 4.00, 0.79, 6.00, 0.78, 6.00, 0.80, 5.00, 0.85, 6.00, 0.94, 8.00, 0.92, 7.00, 0.91, 7.00, 0.81, 3.00, 0.79, 3.00, 0.81, 8.00, 0.91, 9.00, 0.76, 5.00, 0.86, '批次AHP-2026-002，专家：郑雪，平均权重：6.25，专家CV：6.41%（正确方法）', '2026-03-06 19:45:57', '2026-03-06 19:48:52');
INSERT INTO `ahp_expert_military_operation_effect_weights` VALUES (121, 'AHP-2026-001', '张军', 7.00, 6.00, 6.00, 10.00, 6.00, 8.00, 5.00, 4.00, 7.00, 7.00, 6.00, 4.00, 8.00, 7.00, 6.00, 2.00, 4.00, 8.00, 10.00, 4.00, 0.84, 0.75, 0.56, 0.67, 0.78, 0.84, 0.65, 0.74, 0.81, 0.85, 0.58, 0.85, 0.64, 0.87, 0.86, 0.72, 0.87, 0.73, 0.87, 0.72, '批次AHP-2026-001，专家：张军，平均权重：6.25，平均把握度：0.76', '2026-03-12 17:19:48', '2026-03-12 17:19:48');
INSERT INTO `ahp_expert_military_operation_effect_weights` VALUES (122, 'AHP-2026-001', '李建国', 5.00, 5.00, 8.00, 8.00, 7.00, 7.00, 5.00, 4.00, 6.00, 6.00, 6.00, 5.00, 7.00, 6.00, 6.00, 3.00, 1.00, 7.00, 9.00, 6.00, 0.66, 0.67, 0.64, 0.77, 0.80, 0.86, 0.63, 0.89, 0.61, 0.64, 0.72, 0.77, 0.62, 0.74, 0.62, 0.63, 0.83, 0.83, 0.74, 0.81, '批次AHP-2026-001，专家：李建国，平均权重：5.85，平均把握度：0.72', '2026-03-12 17:19:48', '2026-03-12 17:19:48');
INSERT INTO `ahp_expert_military_operation_effect_weights` VALUES (123, 'AHP-2026-001', '王海峰', 7.00, 5.00, 8.00, 8.00, 6.00, 7.00, 5.00, 3.00, 5.00, 7.00, 5.00, 4.00, 8.00, 8.00, 7.00, 2.00, 3.00, 9.00, 8.00, 5.00, 0.55, 0.69, 0.69, 0.72, 0.66, 0.69, 0.56, 0.64, 0.68, 0.61, 0.88, 0.66, 0.86, 0.81, 0.83, 0.74, 0.87, 0.57, 0.59, 0.56, '批次AHP-2026-001，专家：王海峰，平均权重：6.00，平均把握度：0.69', '2026-03-12 17:19:48', '2026-03-12 17:19:48');
INSERT INTO `ahp_expert_military_operation_effect_weights` VALUES (124, 'AHP-2026-001', '刘芳', 7.00, 6.00, 6.00, 8.00, 6.00, 9.00, 7.00, 4.00, 5.00, 7.00, 5.00, 6.00, 8.00, 8.00, 8.00, 2.00, 2.00, 9.00, 8.00, 5.00, 0.86, 0.87, 0.67, 0.56, 0.75, 0.89, 0.61, 0.76, 0.71, 0.90, 0.76, 0.63, 0.86, 0.81, 0.60, 0.75, 0.58, 0.58, 0.67, 0.67, '批次AHP-2026-001，专家：刘芳，平均权重：6.30，平均把握度：0.72', '2026-03-12 17:19:48', '2026-03-12 17:19:48');
INSERT INTO `ahp_expert_military_operation_effect_weights` VALUES (125, 'AHP-2026-001', '陈伟', 7.00, 6.00, 7.00, 8.00, 6.00, 9.00, 5.00, 5.00, 6.00, 7.00, 6.00, 4.00, 9.00, 6.00, 7.00, 3.00, 2.00, 7.00, 9.00, 4.00, 0.83, 0.82, 0.74, 0.80, 0.60, 0.79, 0.56, 0.56, 0.90, 0.81, 0.80, 0.78, 0.86, 0.89, 0.82, 0.81, 0.55, 0.73, 0.87, 0.68, '批次AHP-2026-001，专家：陈伟，平均权重：6.15，平均把握度：0.76', '2026-03-12 17:19:48', '2026-03-12 17:19:48');
INSERT INTO `ahp_expert_military_operation_effect_weights` VALUES (126, 'AHP-2026-001', '赵敏', 7.00, 4.00, 6.00, 8.00, 8.00, 7.00, 7.00, 3.00, 7.00, 5.00, 5.00, 5.00, 9.00, 6.00, 7.00, 3.00, 4.00, 9.00, 9.00, 5.00, 0.82, 0.62, 0.76, 0.75, 0.85, 0.57, 0.81, 0.81, 0.57, 0.72, 0.61, 0.78, 0.73, 0.83, 0.65, 0.84, 0.81, 0.88, 0.74, 0.72, '批次AHP-2026-001，专家：赵敏，平均权重：6.20，平均把握度：0.74', '2026-03-12 17:19:48', '2026-03-12 17:19:48');
INSERT INTO `ahp_expert_military_operation_effect_weights` VALUES (127, 'AHP-2026-001', '孙强', 6.00, 5.00, 6.00, 8.00, 7.00, 9.00, 5.00, 4.00, 6.00, 7.00, 6.00, 4.00, 8.00, 7.00, 6.00, 4.00, 2.00, 7.00, 9.00, 6.00, 0.63, 0.89, 0.73, 0.67, 0.66, 0.76, 0.62, 0.84, 0.76, 0.80, 0.66, 0.82, 0.80, 0.78, 0.68, 0.73, 0.66, 0.82, 0.63, 0.73, '批次AHP-2026-001，专家：孙强，平均权重：6.10，平均把握度：0.73', '2026-03-12 17:19:48', '2026-03-12 17:19:48');
INSERT INTO `ahp_expert_military_operation_effect_weights` VALUES (128, 'AHP-2026-001', '周婷', 7.00, 4.00, 6.00, 8.00, 8.00, 9.00, 7.00, 4.00, 7.00, 6.00, 5.00, 6.00, 8.00, 6.00, 8.00, 2.00, 2.00, 8.00, 10.00, 5.00, 0.64, 0.62, 0.87, 0.68, 0.75, 0.72, 0.58, 0.83, 0.57, 0.72, 0.59, 0.74, 0.60, 0.80, 0.62, 0.68, 0.65, 0.89, 0.75, 0.73, '批次AHP-2026-001，专家：周婷，平均权重：6.30，平均把握度：0.70', '2026-03-12 17:19:48', '2026-03-12 17:19:48');
INSERT INTO `ahp_expert_military_operation_effect_weights` VALUES (129, 'AHP-2026-001', '吴磊', 5.00, 4.00, 7.00, 9.00, 6.00, 9.00, 6.00, 3.00, 6.00, 6.00, 5.00, 4.00, 7.00, 7.00, 6.00, 2.00, 3.00, 9.00, 10.00, 4.00, 0.86, 0.68, 0.87, 0.74, 0.88, 0.71, 0.81, 0.66, 0.79, 0.74, 0.86, 0.64, 0.85, 0.63, 0.76, 0.74, 0.78, 0.58, 0.71, 0.66, '批次AHP-2026-001，专家：吴磊，平均权重：5.90，平均把握度：0.75', '2026-03-12 17:19:48', '2026-03-12 17:19:48');
INSERT INTO `ahp_expert_military_operation_effect_weights` VALUES (130, 'AHP-2026-001', '郑雪', 6.00, 5.00, 6.00, 8.00, 7.00, 9.00, 5.00, 4.00, 7.00, 6.00, 4.00, 5.00, 8.00, 6.00, 7.00, 2.00, 2.00, 7.00, 8.00, 4.00, 0.57, 0.66, 0.88, 0.88, 0.59, 0.73, 0.66, 0.85, 0.78, 0.60, 0.88, 0.67, 0.59, 0.90, 0.67, 0.79, 0.87, 0.76, 0.76, 0.58, '批次AHP-2026-001，专家：郑雪，平均权重：5.80，平均把握度：0.73', '2026-03-12 17:19:48', '2026-03-12 17:19:48');

-- ----------------------------
-- Table structure for ahp_final_weights
-- ----------------------------
DROP TABLE IF EXISTS `ahp_final_weights`;
CREATE TABLE `ahp_final_weights`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `batch_id` varchar(50) CHARACTER SET utf8mb4  NOT NULL COMMENT '评估批次ID，如 AHP-2026-001',
  `evaluation_date` date NOT NULL COMMENT '评估日期',
  `indicator_key` varchar(50) CHARACTER SET utf8mb4  NOT NULL COMMENT '指标键名，如 security_key_leakage',
  `indicator_name` varchar(100) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '指标中文名，如 密钥泄露',
  `indicator_name_en` varchar(100) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '指标英文名，如 Key Leakage',
  `category` varchar(30) CHARACTER SET utf8mb4  NOT NULL COMMENT '一级类别 (security/reliability/transmission/anti_jamming/resource/effect)',
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
) ENGINE = InnoDB AUTO_INCREMENT = 161 CHARACTER SET = utf8mb4  COMMENT = '最终AHP权重结果表 - 存储修正后的指标权重及一致性信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ahp_final_weights
-- ----------------------------
INSERT INTO `ahp_final_weights` VALUES (141, 'AHP-2026-001', '2026-03-13', 'security_key_leakage', '密钥泄露', 'security_key_leakage_weight', 'security', 0.085258, 0.341034, 0.082011, 3.0003, 0.000158, 0.000272, 1, 10);
INSERT INTO `ahp_final_weights` VALUES (142, 'AHP-2026-001', '2026-03-13', 'security_detected_probability', '被侦察概率', 'security_detected_probability_weight', 'security', 0.073594, 0.294375, 0.072751, 3.0003, 0.000158, 0.000272, 1, 10);
INSERT INTO `ahp_final_weights` VALUES (143, 'AHP-2026-001', '2026-03-13', 'security_interception_resistance', '抗拦截能力', 'security_interception_resistance_weight', 'security', 0.091148, 0.364592, 0.095238, 3.0003, 0.000158, 0.000272, 1, 10);
INSERT INTO `ahp_final_weights` VALUES (144, 'AHP-2026-001', '2026-03-13', 'reliability_crash_rate', '崩溃率', 'reliability_crash_rate_weight', 'reliability', 0.072363, 0.361814, 0.072881, 3.0000, 0.000021, 0.000036, 1, 10);
INSERT INTO `ahp_final_weights` VALUES (145, 'AHP-2026-001', '2026-03-13', 'reliability_recovery_capability', '恢复能力', 'reliability_recovery_capability_weight', 'reliability', 0.057946, 0.289732, 0.056780, 3.0000, 0.000021, 0.000036, 1, 10);
INSERT INTO `ahp_final_weights` VALUES (146, 'AHP-2026-001', '2026-03-13', 'reliability_communication_availability', '通信可用性', 'reliability_communication_availability_weight', 'reliability', 0.069691, 0.348453, 0.070339, 3.0000, 0.000021, 0.000036, 1, 10);
INSERT INTO `ahp_final_weights` VALUES (147, 'AHP-2026-001', '2026-03-13', 'transmission_bandwidth', '带宽', 'transmission_bandwidth_weight', 'transmission', 0.036734, 0.183671, 0.036306, 5.7892, -0.042166, -0.034005, 1, 10);
INSERT INTO `ahp_final_weights` VALUES (148, 'AHP-2026-001', '2026-03-13', 'transmission_call_setup_time', '呼叫建立时间', 'transmission_call_setup_time_weight', 'transmission', 0.023804, 0.119022, 0.024841, 5.7892, -0.042166, -0.034005, 1, 10);
INSERT INTO `ahp_final_weights` VALUES (149, 'AHP-2026-001', '2026-03-13', 'transmission_transmission_delay', '传输时延', 'transmission_transmission_delay_weight', 'transmission', 0.035580, 0.177899, 0.036306, 5.7892, -0.042166, -0.034005, 1, 10);
INSERT INTO `ahp_final_weights` VALUES (150, 'AHP-2026-001', '2026-03-13', 'transmission_bit_error_rate', '误码率', 'transmission_bit_error_rate_weight', 'transmission', 0.037873, 0.189365, 0.038217, 5.7892, -0.042166, -0.034005, 1, 10);
INSERT INTO `ahp_final_weights` VALUES (151, 'AHP-2026-001', '2026-03-13', 'transmission_throughput', '吞吐量', 'transmission_throughput_weight', 'transmission', 0.034179, 0.170896, 0.033121, 5.7892, -0.042166, -0.034005, 1, 10);
INSERT INTO `ahp_final_weights` VALUES (152, 'AHP-2026-001', '2026-03-13', 'transmission_spectral_efficiency', '频谱效率', 'transmission_spectral_efficiency_weight', 'transmission', 0.031829, 0.159147, 0.031210, 5.7892, -0.042166, -0.034005, 1, 10);
INSERT INTO `ahp_final_weights` VALUES (153, 'AHP-2026-001', '2026-03-13', 'anti_jamming_sinr', 'SINR', 'anti_jamming_sinr_weight', 'anti_jamming', 0.054286, 0.361907, 0.054167, 2.9780, -0.010994, -0.018955, 1, 10);
INSERT INTO `ahp_final_weights` VALUES (154, 'AHP-2026-001', '2026-03-13', 'anti_jamming_anti_jamming_margin', '抗干扰余量', 'anti_jamming_anti_jamming_margin_weight', 'anti_jamming', 0.049112, 0.327414, 0.049306, 2.9780, -0.010994, -0.018955, 1, 10);
INSERT INTO `ahp_final_weights` VALUES (155, 'AHP-2026-001', '2026-03-13', 'anti_jamming_communication_distance', '通信距离', 'anti_jamming_communication_distance_weight', 'anti_jamming', 0.046602, 0.310679, 0.046528, 2.9780, -0.010994, -0.018955, 1, 10);
INSERT INTO `ahp_final_weights` VALUES (156, 'AHP-2026-001', '2026-03-13', 'resource_power_consumption', '功耗', 'resource_power_consumption_weight', 'resource', 0.051231, 0.512311, 0.051562, 1.8808, -0.119182, 0.000000, 1, 10);
INSERT INTO `ahp_final_weights` VALUES (157, 'AHP-2026-001', '2026-03-13', 'resource_manpower_requirement', '人力需求', 'resource_manpower_requirement_weight', 'resource', 0.048769, 0.487689, 0.048438, 1.8808, -0.119182, 0.000000, 1, 10);
INSERT INTO `ahp_final_weights` VALUES (158, 'AHP-2026-001', '2026-03-13', 'effect_damage_rate', '毁伤率', 'effect_damage_rate_weight', 'effect', 0.038529, 0.385288, 0.037387, 2.9832, -0.008379, -0.014446, 1, 10);
INSERT INTO `ahp_final_weights` VALUES (159, 'AHP-2026-001', '2026-03-13', 'effect_mission_completion_rate', '任务完成率', 'effect_mission_completion_rate_weight', 'effect', 0.040196, 0.401958, 0.039640, 2.9832, -0.008379, -0.014446, 1, 10);
INSERT INTO `ahp_final_weights` VALUES (160, 'AHP-2026-001', '2026-03-13', 'effect_cost_effectiveness', '效费比', 'effect_cost_effectiveness_weight', 'effect', 0.021275, 0.212754, 0.022973, 2.9832, -0.008379, -0.014446, 1, 10);

-- ----------------------------
-- Table structure for cost_evaluation
-- ----------------------------
DROP TABLE IF EXISTS `cost_evaluation`;
CREATE TABLE `cost_evaluation`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `operation_id` varchar(50) CHARACTER SET utf8mb4  NOT NULL COMMENT '作战任务ID',
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
  `remarks` text CHARACTER SET utf8mb4  NULL COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_operation`(`operation_id` ASC) USING BTREE,
  INDEX `idx_evaluation_time`(`evaluation_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 31 CHARACTER SET = utf8mb4  COMMENT = '成本评估表（全成本指标扁平版）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of cost_evaluation
-- ----------------------------
INSERT INTO `cost_evaluation` VALUES (21, 'OP-2026-001', '2026-03-01 10:00:00', 137.77, 65.02, 71.41, 94.34, 33.54, 47.67, 35.15, 332.00, 45.63, 98.43, 66.06, 54.38, 39.23, 34.54, 50.98, 40.80, 98.33, 50.10, 6.03, 172.45, 14.25, '军事通信装备效能评估成本数据 - 实验批次 OP-2026-001', '2026-03-12 17:19:48', '2026-03-12 17:19:48');
INSERT INTO `cost_evaluation` VALUES (22, 'OP-2026-002', '2026-03-01 11:00:00', 120.47, 88.28, 50.84, 58.92, 46.12, 39.98, 31.53, 432.18, 62.03, 59.37, 57.53, 64.54, 34.63, 62.26, 50.06, 43.49, 55.75, 41.65, 6.12, 200.18, 16.62, '军事通信装备效能评估成本数据 - 实验批次 OP-2026-002', '2026-03-12 17:19:48', '2026-03-12 17:19:48');
INSERT INTO `cost_evaluation` VALUES (23, 'OP-2026-003', '2026-03-01 12:00:00', 120.81, 86.10, 71.88, 75.94, 46.00, 22.58, 24.87, 372.66, 54.09, 65.15, 63.71, 48.05, 50.06, 71.87, 60.44, 19.15, 62.71, 46.03, 2.67, 119.52, 22.70, '军事通信装备效能评估成本数据 - 实验批次 OP-2026-003', '2026-03-12 17:19:48', '2026-03-12 17:19:48');
INSERT INTO `cost_evaluation` VALUES (24, 'OP-2026-004', '2026-03-01 13:00:00', 117.48, 124.57, 68.53, 89.45, 48.47, 28.03, 37.75, 493.48, 59.69, 107.54, 90.02, 64.60, 48.27, 80.26, 40.78, 36.70, 95.24, 31.73, 2.56, 114.41, 28.73, '军事通信装备效能评估成本数据 - 实验批次 OP-2026-004', '2026-03-12 17:19:48', '2026-03-12 17:19:48');
INSERT INTO `cost_evaluation` VALUES (25, 'OP-2026-005', '2026-03-01 14:00:00', 123.82, 121.63, 66.50, 99.81, 51.19, 52.84, 26.44, 448.52, 66.75, 105.08, 69.43, 69.79, 49.26, 73.04, 50.71, 21.54, 101.35, 41.11, 3.07, 127.86, 18.40, '军事通信装备效能评估成本数据 - 实验批次 OP-2026-005', '2026-03-12 17:19:48', '2026-03-12 17:19:48');
INSERT INTO `cost_evaluation` VALUES (26, 'OP-2026-006', '2026-03-01 15:00:00', 116.26, 82.72, 46.50, 103.74, 44.48, 23.62, 23.24, 415.39, 73.23, 89.63, 61.07, 41.82, 52.18, 57.37, 39.91, 55.43, 102.31, 59.76, 7.01, 108.81, 28.67, '军事通信装备效能评估成本数据 - 实验批次 OP-2026-006', '2026-03-12 17:19:48', '2026-03-12 17:19:48');
INSERT INTO `cost_evaluation` VALUES (27, 'OP-2026-007', '2026-03-01 16:00:00', 153.92, 118.53, 70.75, 102.75, 57.85, 48.49, 24.12, 562.85, 91.96, 110.06, 80.78, 51.83, 54.06, 45.63, 47.95, 28.60, 116.50, 66.55, 3.43, 218.09, 26.17, '军事通信装备效能评估成本数据 - 实验批次 OP-2026-007', '2026-03-12 17:19:48', '2026-03-12 17:19:48');
INSERT INTO `cost_evaluation` VALUES (28, 'OP-2026-008', '2026-03-01 17:00:00', 132.39, 129.50, 61.61, 95.01, 64.16, 26.31, 35.53, 528.13, 98.94, 115.39, 62.76, 55.79, 28.34, 85.55, 45.11, 30.78, 72.81, 33.63, 3.66, 124.42, 18.60, '军事通信装备效能评估成本数据 - 实验批次 OP-2026-008', '2026-03-12 17:19:48', '2026-03-12 17:19:48');
INSERT INTO `cost_evaluation` VALUES (29, 'OP-2026-009', '2026-03-01 18:00:00', 110.61, 102.74, 52.10, 86.93, 64.79, 51.69, 32.07, 534.81, 96.58, 91.98, 63.28, 51.63, 46.51, 81.69, 39.93, 50.13, 101.24, 52.05, 5.96, 132.12, 26.41, '军事通信装备效能评估成本数据 - 实验批次 OP-2026-009', '2026-03-12 17:19:48', '2026-03-12 17:19:48');
INSERT INTO `cost_evaluation` VALUES (30, 'OP-2026-010', '2026-03-01 19:00:00', 146.97, 126.39, 59.88, 66.16, 51.03, 30.93, 30.92, 518.65, 62.46, 138.38, 90.59, 65.22, 59.90, 78.78, 40.85, 41.71, 116.23, 49.73, 3.46, 183.30, 26.04, '军事通信装备效能评估成本数据 - 实验批次 OP-2026-010', '2026-03-12 17:19:48', '2026-03-12 17:19:48');

-- ----------------------------
-- Table structure for equipment_operation_qualitative_score
-- ----------------------------
DROP TABLE IF EXISTS `equipment_operation_qualitative_score`;
CREATE TABLE `equipment_operation_qualitative_score`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `operation_id` varchar(50) CHARACTER SET utf8mb4  NOT NULL COMMENT '作战任务ID',
  `evaluation_time` datetime NOT NULL COMMENT '评估时间',
  `expert_name` varchar(100) CHARACTER SET utf8mb4  NOT NULL COMMENT '专家姓名（用于区分不同专家）',
  `expert_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '专家ID（可选，关联专家可信度表）',
  `maintenance_maintenance_skill_ql` varchar(10) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '维修能力得分（等级：A+ ~ E-）',
  `maintenance_maintenance_skill_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '维修能力得分把握度（如0.9表示90%）',
  `comm_attack_jamming_effectiveness_ql` varchar(10) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '干扰效能达成得分（等级：A+ ~ E-）',
  `comm_attack_jamming_effectiveness_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '干扰效能达成得分把握度',
  `comm_attack_deception_signal_generation_ql` varchar(10) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '欺骗信号生成得分（等级：A+ ~ E-）',
  `comm_attack_deception_signal_generation_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '欺骗信号生成得分把握度',
  `comm_defense_signal_interception_awareness_ql` varchar(10) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '信号截获感知得分（等级：A+ ~ E-）',
  `comm_defense_signal_interception_awareness_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '信号截获感知得分把握度',
  `comm_defense_anti_jamming_operation_ql` varchar(10) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '抗干扰操作得分（等级：A+ ~ E-）',
  `comm_defense_anti_jamming_operation_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '抗干扰操作得分把握度',
  `comm_defense_anti_deception_awareness_ql` varchar(10) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '防骗反骗得分（等级：A+ ~ E-）',
  `comm_defense_anti_deception_awareness_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '防骗反骗得分把握度',
  `maintenance_feedback_equipment_feedback_ql` varchar(10) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '装备反馈得分（等级：A+ ~ E-）',
  `maintenance_feedback_equipment_feedback_confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '装备反馈得分把握度',
  `remarks` text CHARACTER SET utf8mb4  NULL COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_operation`(`operation_id` ASC) USING BTREE,
  INDEX `idx_evaluation_time`(`evaluation_time` ASC) USING BTREE,
  INDEX `idx_expert`(`expert_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4  COMMENT = '装备操作定性指标专家打分表（每行一位专家对本次评估所有定性指标的评分）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of equipment_operation_qualitative_score
-- ----------------------------
INSERT INTO `equipment_operation_qualitative_score` VALUES (1, 'OP-2026-001', '2026-03-06 09:15:41', '张军', 11, 'B+', 0.71, 'A-', 0.97, 'B+', 0.84, 'B+', 0.78, 'A-', 0.66, 'A', 0.88, 'A', 0.94, '专家：张军，平均置信度：0.83，模拟平均分：86.42', '2026-03-06 09:15:40', '2026-03-06 09:15:40');
INSERT INTO `equipment_operation_qualitative_score` VALUES (2, 'OP-2026-001', '2026-03-06 09:15:41', '李建国', 12, 'A+', 1.00, 'A+', 0.50, 'B', 0.91, 'A', 0.98, 'A', 0.88, 'A+', 1.00, 'A', 0.85, '专家：李建国，平均置信度：0.95，模拟平均分：92.55', '2026-03-06 09:15:40', '2026-03-09 16:05:04');
INSERT INTO `equipment_operation_qualitative_score` VALUES (3, 'OP-2026-001', '2026-03-06 09:15:41', '王海峰', 13, 'B', 0.60, 'B-', 0.76, 'A-', 0.94, 'B+', 0.73, 'B-', 0.65, 'A-', 0.84, 'B+', 0.85, '专家：王海峰，平均置信度：0.77，模拟平均分：80.52', '2026-03-06 09:15:40', '2026-03-06 09:15:40');
INSERT INTO `equipment_operation_qualitative_score` VALUES (4, 'OP-2026-001', '2026-03-06 09:15:41', '刘芳', 14, 'B+', 0.89, 'A-', 0.94, 'A-', 0.96, 'B+', 0.93, 'A', 0.50, 'A-', 0.84, 'B+', 0.92, '专家：刘芳，平均置信度：0.91，模拟平均分：86.21', '2026-03-06 09:15:40', '2026-03-09 16:05:13');
INSERT INTO `equipment_operation_qualitative_score` VALUES (5, 'OP-2026-001', '2026-03-06 09:15:41', '陈伟', 15, 'C', 0.73, 'B-', 0.80, 'B', 0.99, 'B+', 0.83, 'B-', 0.62, 'B-', 0.77, 'C+', 0.69, '专家：陈伟，平均置信度：0.78，模拟平均分：72.86', '2026-03-06 09:15:40', '2026-03-06 09:15:40');
INSERT INTO `equipment_operation_qualitative_score` VALUES (6, 'OP-2026-001', '2026-03-06 09:15:41', '赵敏', 16, 'A', 0.83, 'A', 0.96, 'B-', 0.60, 'B+', 0.86, 'A', 0.98, 'A-', 0.70, 'A-', 0.88, '专家：赵敏，平均置信度：0.83，模拟平均分：87.39', '2026-03-06 09:15:40', '2026-03-06 09:15:40');
INSERT INTO `equipment_operation_qualitative_score` VALUES (7, 'OP-2026-001', '2026-03-06 09:15:41', '孙强', 17, 'C', 0.60, 'C+', 0.76, 'B-', 0.80, 'C+', 0.60, 'C+', 0.60, 'C+', 0.75, 'B', 0.78, '专家：孙强，平均置信度：0.70，模拟平均分：68.61', '2026-03-06 09:15:40', '2026-03-06 09:15:40');
INSERT INTO `equipment_operation_qualitative_score` VALUES (8, 'OP-2026-001', '2026-03-06 09:15:41', '周婷', 18, 'C', 0.50, 'C', 0.85, 'B+', 1.00, 'B', 0.68, 'C', 0.60, 'C+', 0.72, 'B-', 0.84, '专家：周婷，平均置信度：0.76，模拟平均分：69.75', '2026-03-06 09:15:40', '2026-03-09 16:04:56');
INSERT INTO `equipment_operation_qualitative_score` VALUES (9, 'OP-2026-001', '2026-03-06 09:15:41', '吴磊', 19, 'B-', 0.81, 'B', 0.72, 'C-', 0.62, 'C+', 0.50, 'B-', 0.80, 'C-', 0.60, 'C+', 0.60, '专家：吴磊，平均置信度：0.69，模拟平均分：68.13', '2026-03-06 09:15:40', '2026-03-09 16:05:09');
INSERT INTO `equipment_operation_qualitative_score` VALUES (10, 'OP-2026-001', '2026-03-06 09:15:41', '郑雪', 20, 'D+', 0.60, 'C', 0.60, 'B-', 0.78, 'C', 0.71, 'C-', 0.60, 'C', 0.72, 'D+', 0.60, '专家：郑雪，平均置信度：0.66，模拟平均分：59.92', '2026-03-06 09:15:40', '2026-03-06 09:15:40');

-- ----------------------------
-- Table structure for equipment_operation_score
-- ----------------------------
DROP TABLE IF EXISTS `equipment_operation_score`;
CREATE TABLE `equipment_operation_score`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `operation_id` varchar(50) CHARACTER SET utf8mb4  NOT NULL COMMENT '作战任务ID',
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
  `remarks` text CHARACTER SET utf8mb4  NULL COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_operation`(`operation_id` ASC) USING BTREE,
  INDEX `idx_evaluation_time`(`evaluation_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 41 CHARACTER SET = utf8mb4  COMMENT = '装备操作层级评估表（带一级前缀，全量化得分版）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of equipment_operation_score
-- ----------------------------
INSERT INTO `equipment_operation_score` VALUES (31, 'OP-2026-001', '2026-03-13 10:46:45', 71.29, 69.75, 70.39, 69.01, 75.16, 88.16, 78.07, 91.18, 88.77, 68.54, 88.19, 76.53, 77.64, 79.60, 76.11, 78.80, 67.25, 82.32, 83.50, 66.23, 76.94, '实验1：张军，平均分：75.55', '2026-03-12 17:19:48', '2026-03-13 10:46:45');
INSERT INTO `equipment_operation_score` VALUES (32, 'OP-2026-002', '2026-02-13 17:19:49', 75.53, 89.38, 64.41, 67.38, 61.30, 60.50, 80.11, 72.26, 76.61, 80.94, 93.84, 92.52, 86.49, 89.53, 75.86, 60.97, 74.43, 88.70, 88.23, 80.45, 76.12, '实验2：李建国，平均分：77.88', '2026-03-12 17:19:48', '2026-02-13 17:19:49');
INSERT INTO `equipment_operation_score` VALUES (33, 'OP-2026-003', '2026-03-08 17:19:49', 61.33, 81.42, 80.13, 76.51, 63.69, 81.96, 79.75, 78.48, 94.21, 64.81, 71.06, 60.49, 94.68, 71.32, 74.75, 90.84, 70.08, 65.57, 68.57, 76.67, 83.02, '实验3：王海峰，平均分：75.68', '2026-03-12 17:19:48', '2026-03-08 17:19:49');
INSERT INTO `equipment_operation_score` VALUES (34, 'OP-2026-004', '2026-03-08 17:19:49', 78.76, 86.88, 69.15, 80.71, 71.94, 78.16, 94.19, 64.85, 60.51, 88.13, 76.30, 75.00, 64.19, 89.54, 76.06, 77.34, 82.96, 76.85, 93.93, 71.97, 70.23, '实验4：刘芳，平均分：77.51', '2026-03-12 17:19:48', '2026-03-08 17:19:49');
INSERT INTO `equipment_operation_score` VALUES (35, 'OP-2026-005', '2026-02-15 17:19:49', 91.48, 66.70, 89.61, 87.28, 68.17, 68.13, 69.24, 73.93, 90.03, 93.78, 64.98, 71.31, 68.54, 64.04, 82.55, 62.92, 64.17, 60.17, 66.91, 81.93, 76.39, '实验5：陈伟，平均分：74.39', '2026-03-12 17:19:48', '2026-02-15 17:19:49');
INSERT INTO `equipment_operation_score` VALUES (36, 'OP-2026-006', '2026-02-24 17:19:49', 78.34, 91.78, 90.94, 62.89, 72.23, 91.52, 69.34, 94.46, 71.93, 79.55, 94.28, 62.23, 84.65, 78.54, 74.91, 65.94, 75.07, 71.82, 64.30, 81.64, 80.20, '实验6：赵敏，平均分：77.93', '2026-03-12 17:19:48', '2026-02-24 17:19:49');
INSERT INTO `equipment_operation_score` VALUES (37, 'OP-2026-007', '2026-03-08 17:19:49', 86.00, 92.71, 94.70, 65.43, 72.36, 86.62, 62.20, 75.66, 62.91, 72.78, 88.51, 87.67, 72.36, 64.44, 62.03, 91.12, 63.65, 70.87, 78.30, 92.68, 63.96, '实验7：孙强，平均分：76.52', '2026-03-12 17:19:48', '2026-03-08 17:19:49');
INSERT INTO `equipment_operation_score` VALUES (38, 'OP-2026-008', '2026-02-24 17:19:49', 72.53, 69.81, 61.20, 63.81, 70.55, 75.86, 86.49, 68.86, 73.62, 92.02, 92.20, 91.85, 77.77, 90.05, 78.66, 90.39, 62.66, 70.83, 69.71, 63.30, 92.81, '实验8：周婷，平均分：76.90', '2026-03-12 17:19:48', '2026-02-24 17:19:49');
INSERT INTO `equipment_operation_score` VALUES (39, 'OP-2026-009', '2026-03-05 17:19:49', 88.16, 83.30, 63.28, 84.56, 63.89, 82.76, 93.01, 67.51, 76.38, 74.87, 88.89, 84.55, 75.27, 62.23, 93.69, 81.49, 72.06, 88.24, 90.42, 71.00, 70.00, '实验9：吴磊，平均分：78.84', '2026-03-12 17:19:48', '2026-03-05 17:19:49');
INSERT INTO `equipment_operation_score` VALUES (40, 'OP-2026-010', '2026-03-03 17:19:49', 93.58, 64.82, 68.84, 68.00, 64.91, 77.44, 83.67, 72.80, 94.29, 87.77, 76.20, 76.70, 93.92, 86.75, 77.04, 75.58, 61.12, 70.49, 70.71, 92.99, 67.24, '实验10：郑雪，平均分：77.37', '2026-03-12 17:19:48', '2026-03-03 17:19:49');

-- ----------------------------
-- Table structure for evaluation_grade_definition
-- ----------------------------
DROP TABLE IF EXISTS `evaluation_grade_definition`;
CREATE TABLE `evaluation_grade_definition`  (
  `grade_id` int NOT NULL AUTO_INCREMENT COMMENT '等级ID',
  `grade_code` varchar(10) CHARACTER SET utf8mb4  NOT NULL COMMENT '等级代码：A+, A, A-, B+, B, B-, C+, C, C-, D+, D, D-, E+, E, E-',
  `grade_name` varchar(50) CHARACTER SET utf8mb4  NOT NULL COMMENT '等级名称：优+, 优, 优-, 良+, 良, 良-, 合格+, 合格, 合格-, 差+, 差, 差-, 极差+, 极差, 极差-',
  `category` varchar(20) CHARACTER SET utf8mb4  NOT NULL COMMENT '大类：优/良/合格/差/极差',
  `min_score` decimal(5, 2) NOT NULL COMMENT '最低分（包含）',
  `max_score` decimal(5, 2) NOT NULL COMMENT '最高分（不包含，最高等级除外）',
  `score_range` varchar(20) CHARACTER SET utf8mb4  NOT NULL COMMENT '分数区间描述：[95,100], [90,95)等',
  `grade_level` int NOT NULL COMMENT '等级排序：1-15，数字越小等级越高',
  `description` text CHARACTER SET utf8mb4  NULL COMMENT '等级描述',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`grade_id`) USING BTREE,
  UNIQUE INDEX `grade_code`(`grade_code` ASC) USING BTREE,
  INDEX `idx_grade_code`(`grade_code` ASC) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_grade_level`(`grade_level` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 31 CHARACTER SET = utf8mb4  COMMENT = '评分等级定义表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of evaluation_grade_definition
-- ----------------------------
INSERT INTO `evaluation_grade_definition` VALUES (16, 'A+', '优+', '优', 95.00, 100.00, '[95,100]', 1, '优秀+：表现极其出色，超出预期', '2026-03-05 23:13:46');
INSERT INTO `evaluation_grade_definition` VALUES (17, 'A', '优', '优', 90.00, 95.00, '[90,95)', 2, '优秀：表现出色，达到优秀标准', '2026-03-05 23:13:46');
INSERT INTO `evaluation_grade_definition` VALUES (18, 'A-', '优-', '优', 85.00, 90.00, '[85,90)', 3, '优秀-：表现良好，接近优秀标准', '2026-03-05 23:13:46');
INSERT INTO `evaluation_grade_definition` VALUES (19, 'B+', '良+', '良', 80.00, 85.00, '[80,85)', 4, '良好+：表现较好，超出良好标准', '2026-03-05 23:13:46');
INSERT INTO `evaluation_grade_definition` VALUES (20, 'B', '良', '良', 75.00, 80.00, '[75,80)', 5, '良好：表现良好，达到良好标准', '2026-03-05 23:13:46');
INSERT INTO `evaluation_grade_definition` VALUES (21, 'B-', '良-', '良', 70.00, 75.00, '[70,75)', 6, '良好-：表现尚可，接近良好标准', '2026-03-05 23:13:46');
INSERT INTO `evaluation_grade_definition` VALUES (22, 'C+', '合格+', '合格', 65.00, 70.00, '[65,70)', 7, '合格+：基本达标，略高于合格线', '2026-03-05 23:13:46');
INSERT INTO `evaluation_grade_definition` VALUES (23, 'C', '合格', '合格', 60.00, 65.00, '[60,65)', 8, '合格：达到基本要求，刚好合格', '2026-03-05 23:13:46');
INSERT INTO `evaluation_grade_definition` VALUES (24, 'C-', '合格-', '合格', 55.00, 60.00, '[55,60)', 9, '合格-：勉强合格，需要改进', '2026-03-05 23:13:46');
INSERT INTO `evaluation_grade_definition` VALUES (25, 'D+', '差+', '差', 47.00, 55.00, '[47,55)', 10, '差+：表现较差，明显低于标准', '2026-03-05 23:13:46');
INSERT INTO `evaluation_grade_definition` VALUES (26, 'D', '差', '差', 39.00, 47.00, '[39,47)', 11, '差：表现差，远低于标准', '2026-03-05 23:13:46');
INSERT INTO `evaluation_grade_definition` VALUES (27, 'D-', '差-', '差', 30.00, 39.00, '[30,39)', 12, '差-：表现很差，严重不达标', '2026-03-05 23:13:46');
INSERT INTO `evaluation_grade_definition` VALUES (28, 'E+', '极差+', '极差', 20.00, 30.00, '[20,30)', 13, '极差+：表现极差，几乎无法接受', '2026-03-05 23:13:46');
INSERT INTO `evaluation_grade_definition` VALUES (29, 'E', '极差', '极差', 10.00, 20.00, '[10,20)', 14, '极差：表现极其糟糕，完全不可接受', '2026-03-05 23:13:46');
INSERT INTO `evaluation_grade_definition` VALUES (30, 'E-', '极差-', '极差', 0.00, 10.00, '[0,10)', 15, '极差-：表现最差，毫无价值', '2026-03-05 23:13:46');

-- ----------------------------
-- Table structure for expert_ahp_comparison_score
-- ----------------------------
DROP TABLE IF EXISTS `expert_ahp_comparison_score`;
CREATE TABLE `expert_ahp_comparison_score`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `expert_name` varchar(100) CHARACTER SET utf8mb4  NOT NULL COMMENT '专家姓名',
  `round` int NULL DEFAULT 1 COMMENT '评估轮次',
  `comparison_key` varchar(200) CHARACTER SET utf8mb4  NOT NULL COMMENT '比较对标识，如:安全性_可靠性',
  `score` decimal(5, 2) NOT NULL COMMENT '打分值(1-9标度)',
  `confidence` decimal(3, 2) NULL DEFAULT 0.80 COMMENT '把握度(0-1)',
  `remark` varchar(500) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_expert_round`(`expert_name` ASC, `round` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4  COMMENT = '专家AHP原始打分表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of expert_ahp_comparison_score
-- ----------------------------

-- ----------------------------
-- Table structure for expert_base_info
-- ----------------------------
DROP TABLE IF EXISTS `expert_base_info`;
CREATE TABLE `expert_base_info`  (
  `expert_id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `expert_name` varchar(100) CHARACTER SET utf8mb4  NOT NULL COMMENT '专家姓名',
  `gender` varchar(10) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '性别',
  `birth_date` date NULL DEFAULT NULL COMMENT '出生日期',
  `phone` varchar(20) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '电子邮箱',
  `work_unit` varchar(200) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '工作单位',
  `department` varchar(100) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '所在部门',
  `title` varchar(50) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '职称(教授/研究员/高工等)',
  `title_level` int NULL DEFAULT NULL COMMENT '职称等级(1-初级 2-中级 3-副高 4-正高)',
  `is_academician` tinyint(1) NULL DEFAULT 0 COMMENT '是否为院士',
  `is_yangtze_scholar` tinyint(1) NULL DEFAULT 0 COMMENT '是否为长江学者',
  `is_excellent_youth` tinyint(1) NULL DEFAULT 0 COMMENT '是否为杰青',
  `is_doctoral_supervisor` tinyint(1) NULL DEFAULT 0 COMMENT '是否为博导',
  `is_master_supervisor` tinyint(1) NULL DEFAULT 0 COMMENT '是否为硕导',
  `position` varchar(50) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '职务(主任/院长/所长等)',
  `position_level` int NULL DEFAULT NULL COMMENT '职务等级(1-一般 2-中层 3-高层)',
  `education` varchar(50) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '最高学历(本科/硕士/博士)',
  `education_level` int NULL DEFAULT NULL COMMENT '学历等级(1-本科 2-硕士 3-博士)',
  `graduated_school` varchar(200) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '毕业院校',
  `school_level` varchar(20) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '学校层次(985/211/普通)',
  `major` varchar(100) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '所学专业',
  `work_years` int NULL DEFAULT NULL COMMENT '工作年限',
  `professional_years` int NULL DEFAULT NULL COMMENT '专业工作年限',
  `exercise_experience` text CHARACTER SET utf8mb4  NULL COMMENT '演习训练经历(JSON格式)',
  `academic_count` int NULL DEFAULT 0 COMMENT '学术论文数量',
  `academic_sci_ei_count` int NULL DEFAULT 0 COMMENT 'SCI/EI论文数量',
  `academic_core_count` int NULL DEFAULT 0 COMMENT '核心期刊论文数量',
  `research_count` int NULL DEFAULT 0 COMMENT '科研项目数量(主持)',
  `research_participate_count` int NULL DEFAULT 0 COMMENT '科研项目数量(参与)',
  `patent_count` int NULL DEFAULT 0 COMMENT '专利数量(授权)',
  `software_copyright_count` int NULL DEFAULT 0 COMMENT '软件著作权数量',
  `monograph_count` int NULL DEFAULT 0 COMMENT '专著/教材数量',
  `award_count` int NULL DEFAULT 0 COMMENT '获奖数量',
  `expertise_area` varchar(500) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '专业领域(JSON数组)',
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
  `remarks` text CHARACTER SET utf8mb4  NULL COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`expert_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 72 CHARACTER SET = utf8mb4  COMMENT = '专家基础信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of expert_base_info
-- ----------------------------
INSERT INTO `expert_base_info` VALUES (62, '高志刚', '男', '1969-03-17', '17918356341', '38875643@edu.cn', '北部战区海军某舰艇部队', '效能评估室', '工程师', 2, 0, 0, 0, 0, 1, '处长', 2, '硕士', 2, '西安电子科技大学', '211', '数据科学与大数据技术', 12, 8, NULL, 8, 4, 3, 1, 2, 1, 1, 0, 0, '军事仿真,抗干扰通信,指挥控制系统', 1, 1, 1, 71, 64, 69, 0, 1, 2, 2, 1, 1, NULL, '2026-03-26 17:12:24', '2026-03-26 17:12:24');
INSERT INTO `expert_base_info` VALUES (63, '周秀英', '女', '1975-02-15', '14326351880', '21765017@mil.cn', '联合参谋部信息通信局', '训练考核中心', '教授', 4, 0, 0, 0, 1, 1, '部长', 3, '博士', 3, '南京大学', '985', '人工智能', 28, 22, NULL, 35, 26, 7, 4, 5, 5, 4, 2, 2, '电磁频谱管理,指挥控制系统,数据融合', 1, 1, 1, 97, 91, 94, 2, 4, 4, 4, 3, 1, NULL, '2026-03-26 17:12:24', '2026-03-26 17:12:24');
INSERT INTO `expert_base_info` VALUES (64, '何平', '女', '1981-08-04', '14212143114', '653758@edu.cn', '某军区司令部', '效能评估室', '副研究员', 3, 0, 0, 0, 1, 1, '院长', 3, '博士', 3, '海军工程大学', '211', '人工智能', 18, 14, NULL, 18, 12, 5, 3, 4, 3, 2, 1, 1, '网络攻防,密码学', 1, 1, 1, 79, 76, 80, 1, 3, 3, 3, 2, 1, NULL, '2026-03-26 17:12:24', '2026-03-26 17:12:24');
INSERT INTO `expert_base_info` VALUES (65, '宋建华', '男', '1979-04-27', '17909833026', '23296095@mil.cn', '某集团军通信部', '作战实验中心', '工程师', 2, 0, 0, 0, 0, 0, '处长', 2, '硕士', 2, '西安电子科技大学', '211', '软件工程', 12, 8, NULL, 8, 4, 3, 1, 2, 1, 1, 0, 0, '效能评估,数据融合,军事通信系统', 1, 1, 1, 70, 67, 72, 0, 1, 2, 2, 1, 1, NULL, '2026-03-26 17:12:24', '2026-03-26 17:12:24');
INSERT INTO `expert_base_info` VALUES (66, '林勇', '男', '1968-05-15', '14932133926', '843280@mil.cn', '空军预警学院', '训练考核中心', '助理工程师', 1, 0, 0, 0, 0, 0, '研究员', 1, '本科', 1, '桂林电子科技大学', '普通', '信息工程', 5, 0, NULL, 0, 0, 0, 0, 0, 0, 0, 0, 0, '指挥控制系统,数据融合', 0, 0, 0, NULL, NULL, NULL, 0, 0, 0, 0, 0, 1, NULL, '2026-03-26 17:12:24', '2026-03-26 17:12:24');
INSERT INTO `expert_base_info` VALUES (67, '黄志强', '男', '1981-11-23', '19410182014', '39843847@edu.cn', '联合参谋部信息通信局', '作战实验中心', '工程师', 2, 0, 0, 0, 0, 0, '室主任', 2, '硕士', 2, '哈尔滨工程大学', '211', '软件工程', 12, 8, NULL, 8, 4, 3, 1, 2, 1, 1, 0, 0, '效能评估,人工智能军事应用', 1, 1, 1, 67, 62, 63, 0, 1, 2, 2, 1, 1, NULL, '2026-03-26 17:12:24', '2026-03-26 17:12:24');
INSERT INTO `expert_base_info` VALUES (68, '谢志刚', '男', '1977-05-20', '14075632680', '35256517@mil.cn', '某集团军通信部', '作战实验中心', '研究员', 4, 0, 0, 0, 1, 1, '所长', 3, '博士', 3, '电子科技大学', '985', '系统工程', 28, 22, NULL, 35, 26, 7, 4, 5, 5, 4, 2, 2, '密码学,军事仿真', 1, 1, 1, 98, 93, 97, 2, 4, 4, 4, 3, 1, NULL, '2026-03-26 17:12:24', '2026-03-26 17:12:24');
INSERT INTO `expert_base_info` VALUES (69, '谢丽', '男', '1982-05-27', '16952978245', '1132123@edu.cn', '第二炮兵工程学院', '效能评估室', '副教授', 3, 0, 0, 0, 1, 1, '部长', 3, '博士', 3, '南京航空航天大学', '211', '系统工程', 18, 14, NULL, 18, 12, 5, 3, 4, 3, 2, 1, 1, '卫星通信,军事仿真', 1, 1, 1, 78, 82, 77, 1, 3, 3, 3, 2, 1, NULL, '2026-03-26 17:12:24', '2026-03-26 17:12:24');
INSERT INTO `expert_base_info` VALUES (70, '萧杰', '女', '1990-04-20', '13376136282', '1075241@mil.cn', '军事科学院', '效能评估室', '讲师', 2, 0, 0, 0, 0, 0, '处长', 2, '硕士', 2, '空军工程大学', '211', '电子信息工程', 12, 8, NULL, 8, 4, 3, 1, 2, 1, 1, 0, 0, '数据链技术,效能评估,抗干扰通信', 1, 1, 1, 63, 70, 62, 0, 1, 2, 2, 1, 1, NULL, '2026-03-26 17:12:24', '2026-03-26 17:12:24');
INSERT INTO `expert_base_info` VALUES (71, '郑伟', '男', '1967-04-13', '18162279019', '1169518@mil.cn', '国防大学', '网络信息中心', '副研究员', 3, 0, 0, 0, 1, 1, '部长', 3, '博士', 3, '西安电子科技大学', '211', '信息工程', 18, 14, NULL, 18, 12, 5, 3, 4, 3, 2, 1, 1, '效能评估,网络攻防,卫星通信', 1, 1, 1, 87, 76, 77, 1, 3, 3, 3, 2, 1, NULL, '2026-03-26 17:12:24', '2026-03-26 17:12:24');

-- ----------------------------
-- Table structure for expert_credibility_evaluation_score
-- ----------------------------
DROP TABLE IF EXISTS `expert_credibility_evaluation_score`;
CREATE TABLE `expert_credibility_evaluation_score`  (
  `expert_id` bigint UNSIGNED NOT NULL COMMENT '专家ID(关联expert_base_info)',
  `expert_name` varchar(100) CHARACTER SET utf8mb4  NOT NULL COMMENT '专家姓名',
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
  `credibility_level` varchar(20) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '可信度等级(A/B/C/D)',
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
  `evaluator` varchar(100) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '评估人',
  `remarks` text CHARACTER SET utf8mb4  NULL COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE INDEX `uk_expert_id`(`expert_id` ASC) USING BTREE,
  INDEX `idx_expert_name`(`expert_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4  COMMENT = '专家可信度评估得分表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of expert_credibility_evaluation_score
-- ----------------------------
INSERT INTO `expert_credibility_evaluation_score` VALUES (62, '高志刚', 60.00, 75.00, 74.00, 55.50, 32.00, 61.00, 75.20, 70.30, 73.80, 59.00, 63.51, 'C', 0.10, 0.08, 0.08, 0.10, 0.10, 0.10, 0.12, 0.12, 0.10, 0.10, '2026-03-26', NULL, NULL, '2026-03-26 17:12:24', '2026-03-26 17:12:24');
INSERT INTO `expert_credibility_evaluation_score` VALUES (63, '周秀英', 90.00, 90.00, 95.00, 100.00, 100.00, 100.00, 93.40, 89.20, 91.30, 87.00, 93.54, 'A', 0.10, 0.08, 0.08, 0.10, 0.10, 0.10, 0.12, 0.12, 0.10, 0.10, '2026-03-26', NULL, NULL, '2026-03-26 17:12:24', '2026-03-26 17:12:24');
INSERT INTO `expert_credibility_evaluation_score` VALUES (64, '何平', 83.00, 90.00, 90.00, 79.50, 94.00, 100.00, 80.80, 78.70, 81.50, 73.00, 84.64, 'B', 0.10, 0.08, 0.08, 0.10, 0.10, 0.10, 0.12, 0.12, 0.10, 0.10, '2026-03-26', NULL, NULL, '2026-03-26 17:12:24', '2026-03-26 17:12:24');
INSERT INTO `expert_credibility_evaluation_score` VALUES (65, '宋建华', 60.00, 75.00, 74.00, 55.50, 32.00, 61.00, 74.50, 72.40, 75.90, 59.00, 63.89, 'C', 0.10, 0.08, 0.08, 0.10, 0.10, 0.10, 0.12, 0.12, 0.10, 0.10, '2026-03-26', NULL, NULL, '2026-03-26 17:12:24', '2026-03-26 17:12:24');
INSERT INTO `expert_credibility_evaluation_score` VALUES (66, '林勇', 45.00, 60.00, 55.00, 20.00, 0.00, 0.00, 50.00, 50.00, 50.00, 30.00, 35.70, 'D', 0.10, 0.08, 0.08, 0.10, 0.10, 0.10, 0.12, 0.12, 0.10, 0.10, '2026-03-26', NULL, NULL, '2026-03-26 17:12:24', '2026-03-26 17:12:24');
INSERT INTO `expert_credibility_evaluation_score` VALUES (67, '黄志强', 60.00, 75.00, 74.00, 55.50, 32.00, 61.00, 72.40, 68.90, 69.60, 59.00, 62.59, 'C', 0.10, 0.08, 0.08, 0.10, 0.10, 0.10, 0.12, 0.12, 0.10, 0.10, '2026-03-26', NULL, NULL, '2026-03-26 17:12:24', '2026-03-26 17:12:24');
INSERT INTO `expert_credibility_evaluation_score` VALUES (68, '谢志刚', 90.00, 90.00, 95.00, 100.00, 100.00, 100.00, 94.10, 90.60, 93.40, 87.00, 94.00, 'A', 0.10, 0.08, 0.08, 0.10, 0.10, 0.10, 0.12, 0.12, 0.10, 0.10, '2026-03-26', NULL, NULL, '2026-03-26 17:12:24', '2026-03-26 17:12:24');
INSERT INTO `expert_credibility_evaluation_score` VALUES (69, '谢丽', 83.00, 90.00, 90.00, 79.50, 94.00, 100.00, 80.10, 82.90, 79.40, 73.00, 84.85, 'B', 0.10, 0.08, 0.08, 0.10, 0.10, 0.10, 0.12, 0.12, 0.10, 0.10, '2026-03-26', NULL, NULL, '2026-03-26 17:12:24', '2026-03-26 17:12:24');
INSERT INTO `expert_credibility_evaluation_score` VALUES (70, '萧杰', 60.00, 75.00, 74.00, 55.50, 32.00, 61.00, 69.60, 74.50, 68.90, 59.00, 62.85, 'C', 0.10, 0.08, 0.08, 0.10, 0.10, 0.10, 0.12, 0.12, 0.10, 0.10, '2026-03-26', NULL, NULL, '2026-03-26 17:12:24', '2026-03-26 17:12:24');
INSERT INTO `expert_credibility_evaluation_score` VALUES (71, '郑伟', 83.00, 90.00, 90.00, 79.50, 94.00, 100.00, 86.40, 78.70, 79.40, 73.00, 85.10, 'B', 0.10, 0.08, 0.08, 0.10, 0.10, 0.10, 0.12, 0.12, 0.10, 0.10, '2026-03-26', NULL, NULL, '2026-03-26 17:12:24', '2026-03-26 17:12:24');

-- ----------------------------
-- Table structure for expert_credibility_results
-- ----------------------------
DROP TABLE IF EXISTS `expert_credibility_results`;
CREATE TABLE `expert_credibility_results`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `batch_id` varchar(50) CHARACTER SET utf8mb4  NOT NULL COMMENT '评估批次ID，如 AHP-2026-001',
  `evaluation_date` date NOT NULL COMMENT '评估日期',
  `expert_name` varchar(100) CHARACTER SET utf8mb4  NOT NULL COMMENT '专家姓名',
  `influence_score` decimal(6, 2) NULL DEFAULT NULL COMMENT '影响力得分 (0-100)，包含职称、职务、学历、学术、科研、演习等因素',
  `knowledge_score` decimal(6, 2) NULL DEFAULT NULL COMMENT '专业知识得分 (0-100)，包含军事训练、系统仿真、统计学知识和专业年限',
  `subjective_total_score` decimal(6, 2) NULL DEFAULT NULL COMMENT '主观综合得分 (0-100)，影响力与专业知识得分的加权平均',
  `subjective_credibility` decimal(5, 4) NULL DEFAULT NULL COMMENT '主观可信度 αi，综合得分除以100后的标准化值 (0-1)',
  `dispersion` decimal(8, 2) NULL DEFAULT NULL COMMENT '离散度 CV(%)，变异系数，反映专家评分与其他专家的偏离程度',
  `consistency_score` decimal(5, 4) NULL DEFAULT NULL COMMENT '一致性得分 (0-1)，基于离散度计算的一致性水平',
  `objective_credibility` decimal(5, 4) NULL DEFAULT NULL COMMENT '客观可信度 βi，基于一致性得分的分段衰减结果',
  `comprehensive_credibility` decimal(5, 4) NULL DEFAULT NULL COMMENT '综合可信度 = λ·α + (1-λ)·β，主客观可信度的加权融合',
  `subjective_weight` decimal(3, 2) NULL DEFAULT 0.60 COMMENT '主观可信度权重 λ，默认值0.6',
  `objective_weight` decimal(3, 2) NULL DEFAULT 0.40 COMMENT '客观可信度权重 (1-λ)，默认值0.4',
  `remarks` text CHARACTER SET utf8mb4  NULL COMMENT '备注信息',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_batch_expert`(`batch_id` ASC, `expert_name` ASC) USING BTREE COMMENT '同一批次内专家姓名唯一',
  INDEX `idx_batch`(`batch_id` ASC) USING BTREE COMMENT '按批次查询',
  INDEX `idx_credibility`(`comprehensive_credibility` DESC) USING BTREE COMMENT '按综合可信度降序排列'
) ENGINE = InnoDB AUTO_INCREMENT = 81 CHARACTER SET = utf8mb4  COMMENT = '专家可信度评估结果表 - 存储主客观及综合可信度' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of expert_credibility_results
-- ----------------------------
INSERT INTO `expert_credibility_results` VALUES (71, 'AHP-2026-001', '2026-03-13', '张军', 93.20, 89.49, 91.35, 0.9135, 14.90, 1.0000, 1.0000, 0.9481, 0.60, 0.40, NULL);
INSERT INTO `expert_credibility_results` VALUES (72, 'AHP-2026-001', '2026-03-13', '李建国', 84.40, 84.35, 84.37, 0.8437, 21.91, 0.6543, 0.6543, 0.7680, 0.60, 0.40, NULL);
INSERT INTO `expert_credibility_results` VALUES (73, 'AHP-2026-001', '2026-03-13', '王海峰', 90.27, 86.53, 88.40, 0.8840, 15.88, 0.9560, 0.9560, 0.9128, 0.60, 0.40, NULL);
INSERT INTO `expert_credibility_results` VALUES (74, 'AHP-2026-001', '2026-03-13', '刘芳', 92.68, 90.52, 91.60, 0.9160, 17.52, 0.8741, 0.8741, 0.8992, 0.60, 0.40, NULL);
INSERT INTO `expert_credibility_results` VALUES (75, 'AHP-2026-001', '2026-03-13', '陈伟', 83.02, 80.21, 81.61, 0.8161, 13.45, 1.0000, 1.0000, 0.8897, 0.60, 0.40, NULL);
INSERT INTO `expert_credibility_results` VALUES (76, 'AHP-2026-001', '2026-03-13', '赵敏', 88.83, 80.15, 84.49, 0.8449, 12.84, 1.0000, 1.0000, 0.9069, 0.60, 0.40, NULL);
INSERT INTO `expert_credibility_results` VALUES (77, 'AHP-2026-001', '2026-03-13', '孙强', 87.51, 89.48, 88.50, 0.8850, 15.54, 0.9729, 0.9729, 0.9202, 0.60, 0.40, NULL);
INSERT INTO `expert_credibility_results` VALUES (78, 'AHP-2026-001', '2026-03-13', '周婷', 78.93, 79.53, 79.23, 0.7923, 15.88, 0.9560, 0.9560, 0.8578, 0.60, 0.40, NULL);
INSERT INTO `expert_credibility_results` VALUES (79, 'AHP-2026-001', '2026-03-13', '吴磊', 65.17, 82.50, 73.83, 0.7383, 16.12, 0.9440, 0.9440, 0.8206, 0.60, 0.40, NULL);
INSERT INTO `expert_credibility_results` VALUES (80, 'AHP-2026-001', '2026-03-13', '郑雪', 64.31, 75.29, 69.80, 0.6980, 5.79, 1.0000, 1.0000, 0.8188, 0.60, 0.40, NULL);

-- ----------------------------
-- Table structure for metrics_military_comm_effect
-- ----------------------------
DROP TABLE IF EXISTS `metrics_military_comm_effect`;
CREATE TABLE `metrics_military_comm_effect`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `evaluation_id` varchar(50) CHARACTER SET utf8mb4  NOT NULL COMMENT '评估批次ID',
  `operation_id` varchar(50) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '作战任务ID',
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
) ENGINE = InnoDB AUTO_INCREMENT = 104 CHARACTER SET = utf8mb4  COMMENT = '军事通信效果指标表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of metrics_military_comm_effect
-- ----------------------------
INSERT INTO `metrics_military_comm_effect` VALUES (84, 'EVAL-BATCH-20260325163853228-706F025F', '20260010', 1.00, 8.33, 8.33, 0, 6217.00, 96.96, 5.05, 297.17, 140.71, 14.399800, 3.02, 0.62, 6.84, 11.66, 24.53, 6.67, 58.33, 30.30, '2026-03-25 16:38:53', '2026-03-25 16:38:53');
INSERT INTO `metrics_military_comm_effect` VALUES (85, 'EVAL-BATCH-20260325163853228-706F025F', '20260009', 1.00, 7.69, 7.69, 1, 5696.67, 97.38, 5.13, 347.41, 138.39, 6.143900, 2.83, 0.56, 6.87, 10.02, 27.68, 1.55, 84.62, 36.00, '2026-03-25 16:38:53', '2026-03-25 16:38:53');
INSERT INTO `metrics_military_comm_effect` VALUES (86, 'EVAL-BATCH-20260325163853228-706F025F', '20260008', 1.00, 14.29, 14.29, 1, 4790.00, 98.42, 4.66, 298.40, 130.75, 4.095400, 2.14, 0.47, 6.14, 10.83, 13.96, 3.55, 85.71, 19.23, '2026-03-25 16:38:53', '2026-03-25 16:38:53');
INSERT INTO `metrics_military_comm_effect` VALUES (87, 'EVAL-BATCH-20260325163853228-706F025F', '20260007', 3.00, 0.00, 0.00, 0, 4200.20, 98.22, 5.05, 299.14, 116.47, 12.211600, 3.06, 0.65, 6.76, 9.92, 22.76, 6.03, 77.78, 30.30, '2026-03-25 16:38:53', '2026-03-25 16:38:53');
INSERT INTO `metrics_military_comm_effect` VALUES (88, 'EVAL-BATCH-20260325163853228-706F025F', '20260006', 1.00, 0.00, 16.67, 2, 5299.75, 97.74, 4.36, 256.85, 90.84, 4.829800, 2.97, 0.70, 9.69, 12.17, 28.75, 10.61, 91.67, 23.81, '2026-03-25 16:38:53', '2026-03-25 16:38:53');
INSERT INTO `metrics_military_comm_effect` VALUES (89, 'EVAL-BATCH-20260325163853228-706F025F', '20260005', 1.00, 12.50, 12.50, 1, 5582.25, 96.38, 3.43, 306.73, 70.90, 2.305000, 2.66, 0.76, 9.04, 14.40, 30.24, 4.00, 100.00, 24.49, '2026-03-25 16:38:53', '2026-03-25 16:38:53');
INSERT INTO `metrics_military_comm_effect` VALUES (90, 'EVAL-BATCH-20260325163853228-706F025F', '20260004', 3.00, 27.27, 0.00, 1, 4834.25, 96.96, 5.72, 240.85, 110.89, 9.946100, 3.08, 0.55, 10.31, 14.49, 22.53, 2.02, 72.73, 25.00, '2026-03-25 16:38:53', '2026-03-25 16:38:53');
INSERT INTO `metrics_military_comm_effect` VALUES (91, 'EVAL-BATCH-20260325163853228-706F025F', '20260003', 1.00, 33.33, 16.67, 1, 5954.57, 95.01, 3.20, 274.57, 164.98, 8.040800, 1.94, 0.61, 3.39, 11.60, 19.44, 3.51, 66.67, 54.55, '2026-03-25 16:38:53', '2026-03-25 16:38:53');
INSERT INTO `metrics_military_comm_effect` VALUES (92, 'EVAL-BATCH-20260325163853228-706F025F', '20260002', 0.00, 14.29, 14.29, 1, 5808.67, 97.37, 4.70, 235.84, 89.51, 2.596400, 2.66, 0.58, 8.36, 13.09, 19.47, 10.00, 100.00, 37.50, '2026-03-25 16:38:53', '2026-03-25 16:38:53');
INSERT INTO `metrics_military_comm_effect` VALUES (93, 'EVAL-BATCH-20260325163853228-706F025F', '20260001', 0.00, 9.09, 9.09, 1, 4271.80, 96.73, 5.08, 264.85, 122.61, 14.387700, 2.88, 0.56, 5.99, 13.64, 19.24, 1.27, 63.64, 17.24, '2026-03-25 16:38:53', '2026-03-25 16:38:53');
INSERT INTO `metrics_military_comm_effect` VALUES (94, 'EVAL-BATCH-20260325194702753-6A9AB557', '20260010', 0.00, 9.09, 18.18, 0, 2012.00, 98.90, 12.88, 339.03, 26.13, 4.916900, 8.16, 0.65, 19.74, 12.20, 26.78, 0.50, 90.91, 10.87, '2026-03-25 19:47:02', '2026-03-25 19:47:02');
INSERT INTO `metrics_military_comm_effect` VALUES (95, 'EVAL-BATCH-20260325194702753-6A9AB557', '20260009', 0.00, 0.00, 0.00, 1, 757.00, 99.41, 11.53, 289.70, 26.90, 2.606400, 6.58, 0.55, 19.52, 11.81, 25.22, 1.05, 100.00, 18.75, '2026-03-25 19:47:02', '2026-03-25 19:47:02');
INSERT INTO `metrics_military_comm_effect` VALUES (96, 'EVAL-BATCH-20260325194702753-6A9AB557', '20260008', 1.00, 0.00, 30.00, 0, 2503.00, 99.54, 11.82, 269.24, 38.08, 4.599300, 6.55, 0.56, 21.08, 11.71, 16.14, 1.89, 90.00, 13.64, '2026-03-25 19:47:02', '2026-03-25 19:47:02');
INSERT INTO `metrics_military_comm_effect` VALUES (97, 'EVAL-BATCH-20260325194702753-6A9AB557', '20260007', 0.00, 0.00, 7.69, 1, 529.00, 99.77, 11.86, 235.69, 42.78, 4.887000, 6.28, 0.55, 19.66, 8.10, 25.27, 0.00, 92.31, 13.33, '2026-03-25 19:47:02', '2026-03-25 19:47:02');
INSERT INTO `metrics_military_comm_effect` VALUES (98, 'EVAL-BATCH-20260325194702753-6A9AB557', '20260006', 0.00, 0.00, 27.27, 0, 2434.00, 99.40, 11.90, 255.78, 24.66, 2.721900, 7.04, 0.58, 19.55, 9.70, 28.02, 0.00, 100.00, 24.00, '2026-03-25 19:47:02', '2026-03-25 19:47:02');
INSERT INTO `metrics_military_comm_effect` VALUES (99, 'EVAL-BATCH-20260325194702753-6A9AB557', '20260005', 2.00, 0.00, 0.00, 1, 1871.00, 98.59, 12.52, 288.33, 43.72, 6.351800, 7.55, 0.61, 19.96, 11.55, 17.02, 0.51, 84.62, 20.83, '2026-03-25 19:47:02', '2026-03-25 19:47:02');
INSERT INTO `metrics_military_comm_effect` VALUES (100, 'EVAL-BATCH-20260325194702753-6A9AB557', '20260004', 0.00, 0.00, 8.33, 0, 2110.00, 99.54, 13.31, 260.55, 27.78, 5.836700, 7.63, 0.59, 16.79, 9.31, 28.39, 0.00, 91.67, 9.43, '2026-03-25 19:47:02', '2026-03-25 19:47:02');
INSERT INTO `metrics_military_comm_effect` VALUES (101, 'EVAL-BATCH-20260325194702753-6A9AB557', '20260003', 0.00, 9.09, 18.18, 1, 745.00, 99.12, 12.29, 291.02, 27.50, 3.729100, 7.59, 0.62, 18.99, 10.83, 26.69, 0.00, 90.91, 8.62, '2026-03-25 19:47:02', '2026-03-25 19:47:02');
INSERT INTO `metrics_military_comm_effect` VALUES (102, 'EVAL-BATCH-20260325194702753-6A9AB557', '20260001', 0.00, 0.00, 7.69, 0, 1672.00, 98.99, 10.64, 338.35, 27.93, 4.593000, 6.08, 0.59, 20.12, 13.34, 32.45, 0.00, 92.31, 8.62, '2026-03-25 19:47:02', '2026-03-25 19:47:02');
INSERT INTO `metrics_military_comm_effect` VALUES (103, 'EVAL-BATCH-20260325194702753-6A9AB557', '20260002', 0.00, 14.29, 21.43, 0, 2471.33, 98.70, 13.04, 319.31, 30.17, 2.489200, 7.58, 0.57, 20.00, 11.14, 30.79, 0.74, 100.00, 9.80, '2026-03-25 19:47:02', '2026-03-25 19:47:02');

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
-- Records of records_link_maintenance_events
-- ----------------------------
INSERT INTO `records_link_maintenance_events` VALUES (1, 20260001, 'NODE-02', 'NODE-06', 'EQ-030', 0, 3068418, 3078452, '光纤断裂', 'hardware_fault', 3080358, 3081537, 1179, '人工抢修', 1, 0, NULL, NULL, NULL, 0, NULL, '光纤熔接', 'OP-05', NULL, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_link_maintenance_events` VALUES (2, 20260001, 'NODE-10', 'NODE-20', 'EQ-021', 0, 313305, 321351, '电磁干扰', 'maintenance', 326043, 328455, 2412, '自动切换', 1, 0, NULL, NULL, NULL, 0, NULL, '重启设备', 'OP-11', NULL, 0, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_link_maintenance_events` VALUES (3, 20260001, 'NODE-08', 'NODE-04', 'EQ-013', 0, 1260743, 1278171, '链路拥塞', 'hardware_fault', 1281804, 1283229, 1425, '重启恢复', 1, 0, NULL, NULL, NULL, 0, NULL, '重启设备', 'OP-11', NULL, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_link_maintenance_events` VALUES (4, 20260002, 'NODE-05', 'NODE-08', 'EQ-027', 0, 1733957, 1749315, '配置错误', 'external_attack', 1752350, 1754700, 2350, '重启恢复', 1, 0, NULL, NULL, NULL, 0, NULL, '电源修复', 'OP-06', NULL, 0, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_link_maintenance_events` VALUES (5, 20260002, 'NODE-17', 'NODE-16', 'EQ-041', 1, 1339921, 1361489, '电源故障', 'hardware_fault', 1364802, 1367242, 2440, '人工抢修', 1, 1, 1369823, NULL, NULL, 1, NULL, NULL, 'OP-07', NULL, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_link_maintenance_events` VALUES (6, 20260002, 'NODE-04', 'NODE-02', 'EQ-012', 0, 2573051, 2581243, '节点宕机', 'external_attack', 2581455, 2584079, 2624, '重启恢复', 1, 0, NULL, NULL, NULL, 0, NULL, '重新配置', 'OP-04', NULL, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_link_maintenance_events` VALUES (7, 20260003, 'NODE-15', 'NODE-08', 'EQ-044', 0, 132116, 153791, '节点宕机', 'crash', 155573, 156318, 745, '自动切换', 1, 0, NULL, NULL, NULL, 0, NULL, '更换模块', 'OP-05', NULL, 0, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_link_maintenance_events` VALUES (8, 20260003, 'NODE-15', 'NODE-08', 'EQ-012', 0, 99741, 106761, '链路拥塞', 'software_fault', 111454, 113281, 1827, '备用链路启用', 0, 1, 118046, NULL, NULL, 1, '元器件老化', NULL, 'OP-02', NULL, 0, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_link_maintenance_events` VALUES (9, 20260004, 'NODE-05', 'NODE-09', 'EQ-049', 0, 2940124, 2943615, '链路拥塞', 'external_attack', 2947404, 2949514, 2110, '自动切换', 1, 1, 2952220, NULL, NULL, 0, NULL, '光纤熔接', 'OP-13', NULL, 0, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_link_maintenance_events` VALUES (10, 20260004, 'NODE-09', 'NODE-03', 'EQ-017', 0, 1185377, 1197500, '配置错误', 'software_fault', 1198286, 1200239, 1953, '重启恢复', 0, 0, NULL, NULL, NULL, 0, '外力破坏', '更换模块', 'OP-08', NULL, 0, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_link_maintenance_events` VALUES (11, 20260005, 'NODE-09', 'NODE-04', 'EQ-040', 0, 1506089, 1518097, '电源故障', 'crash', 1521469, 1524363, 2894, '人工抢修', 1, 0, NULL, NULL, NULL, 0, NULL, '电源修复', 'OP-20', NULL, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_link_maintenance_events` VALUES (12, 20260005, 'NODE-07', 'NODE-03', 'EQ-043', 1, 3128322, 3135894, '光纤断裂', 'hardware_fault', 3136505, 3138718, 2213, '重启恢复', 1, 1, 3139268, NULL, NULL, 1, NULL, NULL, 'OP-12', NULL, 0, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_link_maintenance_events` VALUES (13, 20260005, 'NODE-13', 'NODE-14', 'EQ-001', 0, 373894, 396281, '电源故障', 'hardware_fault', 397871, 398377, 506, '备用链路启用', 1, 1, 405662, NULL, NULL, 1, NULL, NULL, 'OP-17', NULL, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_link_maintenance_events` VALUES (14, 20260006, 'NODE-11', 'NODE-16', 'EQ-039', 1, 180320, 198542, '光纤断裂', 'external_attack', 201506, 203940, 2434, '备用链路启用', 1, 0, NULL, NULL, NULL, 0, NULL, '电源修复', 'OP-07', NULL, 0, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_link_maintenance_events` VALUES (15, 20260007, 'NODE-07', 'NODE-06', 'EQ-020', 1, 140985, 149097, '光纤断裂', 'crash', 151092, 151621, 529, '重启恢复', 1, 0, NULL, NULL, NULL, 0, NULL, '重新配置', 'OP-02', NULL, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_link_maintenance_events` VALUES (16, 20260008, 'NODE-09', 'NODE-15', 'EQ-042', 0, 1672758, 1687721, '电源故障', 'external_attack', 1688835, 1691338, 2503, '人工抢修', 1, 1, 1698650, NULL, NULL, 1, NULL, NULL, 'OP-04', NULL, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_link_maintenance_events` VALUES (17, 20260009, 'NODE-13', 'NODE-03', 'EQ-035', 0, 3017688, 3035530, '电源故障', 'crash', 3037244, 3038001, 757, '重启恢复', 1, 0, NULL, NULL, NULL, 0, NULL, '重启设备', 'OP-18', NULL, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_link_maintenance_events` VALUES (18, 20260010, 'NODE-20', 'NODE-18', 'EQ-034', 0, 937174, 966080, '光纤断裂', 'external_attack', 968116, 970128, 2012, '重启恢复', 1, 0, NULL, NULL, NULL, 0, NULL, '光纤熔接', 'OP-17', NULL, 0, NULL, '2026-03-25 19:46:46');

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
) ENGINE = InnoDB AUTO_INCREMENT = 1085 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '通信基础信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of records_military_communication_info
-- ----------------------------
INSERT INTO `records_military_communication_info` VALUES (967, 20260001, 'NODE-04', 'NODE-18', '视频', 106840, 120484, 133, 59, 316.536, 1, 322617, 28.783, 7228310.32, 39.30, 5498856.62, 23.07, -37.13, 30.77, 495769, 9493, 3348, 96, NULL, 2, -98.64, -120.00, 27.49, 11.03, 0, 0, 'OP-5', 1009.25, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (968, 20260001, 'NODE-10', 'NODE-12', '数据', 617211, 619214, 36, 69, 366.227, 0, 481161, 134.740, 7009966.14, 35.47, 5480792.68, 32.54, -36.91, 39.43, 806992, 240425, 3672, 476, '信道忙', 1, -90.42, -86.26, 20.97, 19.47, 0, 0, 'OP-5', 937.47, 1, 0, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (969, 20260001, 'NODE-10', 'NODE-01', '视频', 1014706, 1028326, 55, 117, 100.284, 1, 226478, 14.439, 12409137.94, 39.55, 6381068.86, 30.39, -84.92, 21.55, 188638, 5604, 457, 5, NULL, 1, -87.78, -120.00, 30.35, 10.46, 0, 0, 'OP-4', 1936.79, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (970, 20260001, 'NODE-01', 'NODE-19', '短消息', 3595249, 3623462, 96, 184, 437.075, 1, 249237, 20.255, 13274373.85, 29.55, 7712286.57, 29.54, -74.64, 46.46, 678724, 233, 3836, 40, NULL, 0, -92.55, -60.50, 22.72, 9.98, 0, 0, 'OP-6', 1038.46, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (971, 20260001, 'NODE-01', 'NODE-16', '短消息', 323681, 340013, 104, 51, 477.578, 1, 30790, 14.275, 9558240.56, 25.54, 4094805.34, 28.37, -65.16, 30.73, 974814, 48338, 3880, 97, NULL, 1, -84.17, -120.00, 19.94, 16.65, 0, 0, 'OP-1', 934.68, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (972, 20260001, 'NODE-18', 'NODE-19', '数据', 184989, 199816, 96, 122, 474.123, 1, 499957, 12.612, 6224926.58, 32.16, 3377119.70, 38.82, -35.81, 36.17, 730863, 17819, 4697, 94, NULL, 1, -91.34, -89.26, 20.45, 18.77, 0, 0, 'OP-4', 1308.63, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (973, 20260001, 'NODE-05', 'NODE-13', '短消息', 845344, 846635, 191, 130, 245.914, 1, 464242, 39.605, 13389740.60, 22.76, 4356309.75, 24.78, -85.62, 47.19, 416337, 14728, 2306, 13, NULL, 1, -99.28, -80.09, 11.78, 8.89, 0, 1, 'OP-1', 148.57, 0, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (974, 20260001, 'NODE-08', 'NODE-04', '数据', 176256, 185911, 15, 16, 330.374, 1, 251182, 13.748, 17792390.77, 27.16, 7560299.71, 21.16, -58.94, 41.94, 782125, 34036, 3989, 74, NULL, 1, -90.02, -66.61, 13.59, 14.48, 0, 0, 'OP-1', 386.11, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (975, 20260001, 'NODE-03', 'NODE-13', '数据', 1765791, 1779540, 65, 160, 249.795, 1, 416384, 10.123, 10992096.29, 23.34, 6922014.61, 30.87, -71.47, 20.58, 266089, 8506, 3689, 8, NULL, 2, -88.64, -77.50, 15.22, 8.46, 0, 0, 'OP-4', 312.81, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (976, 20260001, 'NODE-07', 'NODE-12', '视频', 1513445, 1528498, 165, 72, 273.533, 1, 146084, 11.211, 9170133.88, 27.21, 4941702.20, 36.14, -80.88, 34.72, 602307, 394, 2912, 6, NULL, 2, -96.74, -120.00, 17.10, 11.83, 0, 0, 'OP-3', 658.01, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (977, 20260001, 'NODE-07', 'NODE-10', '短消息', 446522, 472554, 116, 26, 250.783, 1, 465916, 11.315, 13600291.93, 32.45, 10965886.22, 33.65, -31.14, 16.30, 952542, 16301, 2160, 45, NULL, 1, -81.68, -78.45, 25.16, 17.60, 0, 0, 'OP-5', 144.94, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (978, 20260001, 'NODE-15', 'NODE-19', '短消息', 1240830, 1248179, 35, 77, 377.082, 1, 67796, 16.746, 7826297.85, 20.41, 3429492.42, 29.05, -39.23, 12.82, 846217, 13075, 2504, 6, NULL, 1, -92.40, -60.01, 11.67, 16.42, 0, 0, 'OP-10', 1338.27, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (979, 20260001, 'NODE-19', 'NODE-03', '短消息', 1913182, 1938110, 81, 171, 499.282, 1, 438653, 35.269, 9854661.80, 34.82, 8372325.45, 28.16, -87.83, 43.23, 766296, 24456, 2806, 60, NULL, 2, -82.98, -120.00, 25.07, 9.42, 0, 0, 'OP-8', 1719.94, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (980, 20260002, 'NODE-04', 'NODE-13', '视频', 3242725, 3266407, 73, 130, 312.224, 1, 185921, 17.534, 11329586.77, 26.26, 6139681.53, 33.48, -56.13, 27.87, 157244, 5443, 1783, 36, NULL, 1, -87.17, -74.06, 16.35, 10.79, 0, 0, 'OP-9', 620.10, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (981, 20260002, 'NODE-12', 'NODE-07', '语音', 1011118, 1031142, 166, 112, 458.704, 1, 169288, 38.556, 18509042.22, 22.86, 9207160.01, 23.73, -48.97, 29.62, 529804, 4842, 113, 1, NULL, 2, -88.29, -81.84, 14.61, 7.91, 0, 0, 'OP-2', 1029.08, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (982, 20260002, 'NODE-13', 'NODE-02', '短消息', 1305427, 1319670, 66, 142, 213.192, 1, 159946, 37.815, 16357900.38, 34.68, 7089213.98, 36.19, -43.25, 43.90, 783549, 6966, 350, 5, NULL, 0, -92.47, -120.00, 26.17, 9.76, 1, 0, 'OP-3', 1556.91, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (983, 20260002, 'NODE-20', 'NODE-18', '数据', 139607, 144161, 117, 103, 83.005, 1, 319914, 43.935, 6513696.97, 20.50, 3297577.19, 36.30, -29.91, 41.84, 487945, 23617, 4642, 60, NULL, 1, -80.64, -120.00, 7.07, 5.05, 0, 0, 'OP-4', 120.41, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (984, 20260002, 'NODE-02', 'NODE-10', '短消息', 3122273, 3146962, 159, 178, 408.084, 1, 426207, 30.667, 7966807.49, 23.76, 3303245.31, 37.52, -63.84, 34.66, 903217, 29172, 1785, 9, NULL, 2, -89.91, -120.00, 18.34, 3.27, 0, 0, 'OP-2', 718.62, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (985, 20260002, 'NODE-13', 'NODE-10', '数据', 2805519, 2809869, 49, 185, 210.002, 1, 372700, 10.031, 11985200.77, 21.92, 7195820.28, 33.82, -33.97, 45.31, 289079, 12837, 653, 6, NULL, 0, -84.08, -71.85, 11.43, 14.39, 0, 1, 'OP-5', 1170.23, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (986, 20260002, 'NODE-02', 'NODE-09', '视频', 104546, 113735, 115, 24, 446.124, 1, 137043, 14.885, 15155204.38, 34.37, 12503814.54, 25.48, -92.09, 37.57, 38739, 184, 3786, 37, NULL, 1, -80.41, -61.65, 28.07, 16.79, 0, 1, 'OP-7', 1689.92, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (987, 20260002, 'NODE-10', 'NODE-08', '数据', 2021147, 2024482, 186, 107, 491.388, 1, 394679, 35.305, 14662330.23, 33.49, 8382105.23, 34.20, -46.49, 38.13, 654097, 32005, 4507, 20, NULL, 1, -99.41, -120.00, 25.56, 5.69, 0, 0, 'OP-3', 438.54, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (988, 20260002, 'NODE-04', 'NODE-06', '数据', 504244, 513878, 122, 192, 398.025, 1, 373469, 28.632, 11615139.73, 27.48, 10180875.99, 29.66, -62.68, 15.80, 735826, 14655, 3683, 37, NULL, 1, -99.79, -83.71, 13.16, 6.53, 1, 0, 'OP-8', 800.33, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (989, 20260002, 'NODE-03', 'NODE-18', '数据', 2336267, 2361073, 125, 76, 416.130, 1, 189988, 40.578, 14332571.71, 39.51, 7708266.07, 34.38, -64.16, 2.91, 719573, 8946, 4140, 97, NULL, 1, -88.81, -120.00, 31.77, 18.25, 0, 0, 'OP-7', 1613.88, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (990, 20260002, 'NODE-03', 'NODE-08', '语音', 3563853, 3565374, 94, 182, 199.801, 1, 449596, 13.749, 13594049.62, 27.61, 9211296.83, 24.51, -90.90, 35.80, 844928, 16470, 2650, 69, NULL, 0, -89.28, -87.03, 20.99, 18.32, 0, 0, 'OP-1', 742.81, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (991, 20260002, 'NODE-06', 'NODE-14', '视频', 1533698, 1554766, 138, 27, 219.367, 1, 403324, 26.412, 11036984.49, 29.93, 4382872.25, 26.25, -40.70, 26.19, 73626, 1298, 4169, 14, NULL, 0, -98.89, -120.00, 22.45, 16.70, 0, 1, 'OP-6', 386.20, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (992, 20260002, 'NODE-15', 'NODE-17', '语音', 2001119, 2021861, 152, 138, 140.555, 1, 368139, 43.730, 13410462.39, 28.19, 4644276.21, 32.92, -28.03, 49.40, 745602, 21712, 4905, 30, NULL, 1, -92.34, -85.16, 14.67, 15.76, 0, 0, 'OP-10', 875.68, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (993, 20260002, 'NODE-05', 'NODE-12', '数据', 147520, 168529, 31, 68, 473.728, 1, 34589, 40.599, 16131881.93, 35.68, 12912764.18, 24.59, -48.83, 2.07, 297011, 5485, 1880, 16, NULL, 2, -91.65, -79.07, 29.30, 6.72, 0, 0, 'OP-1', 116.97, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (994, 20260003, 'NODE-02', 'NODE-14', '视频', 2299772, 2311084, 149, 147, 306.929, 1, 124730, 29.607, 14824607.16, 25.56, 8556668.95, 22.51, -83.59, 42.63, 744971, 12795, 1690, 42, NULL, 0, -88.89, -82.44, 19.22, 9.94, 0, 0, 'OP-9', 1482.04, 0, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (995, 20260003, 'NODE-06', 'NODE-07', '短消息', 117735, 146740, 122, 135, 329.968, 1, 497228, 21.547, 12511897.07, 24.36, 6392392.90, 21.62, -67.54, 11.11, 625920, 12046, 2803, 80, NULL, 1, -97.00, -120.00, 15.89, 7.97, 1, 0, 'OP-1', 503.96, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (996, 20260003, 'NODE-01', 'NODE-03', '短消息', 817675, 821862, 197, 166, 232.880, 1, 246695, 14.000, 14284721.75, 31.11, 8201926.55, 33.91, -36.63, 25.92, 793745, 7476, 1244, 2, NULL, 1, -94.80, -120.00, 22.57, 9.45, 0, 0, 'OP-8', 413.43, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (997, 20260003, 'NODE-12', 'NODE-11', '短消息', 2784394, 2796314, 35, 57, 221.977, 1, 277705, 38.759, 7487829.27, 32.06, 6000102.16, 36.23, -80.30, 12.81, 443220, 12952, 820, 13, NULL, 0, -84.01, -120.00, 21.04, 4.30, 0, 0, 'OP-4', 817.52, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (998, 20260003, 'NODE-20', 'NODE-13', '语音', 3349477, 3367591, 65, 155, 308.002, 1, 201974, 16.621, 11550825.60, 21.50, 9804319.14, 39.29, -34.21, 34.85, 67227, 2188, 2528, 58, NULL, 1, -86.14, -72.52, 11.24, 7.39, 0, 1, 'OP-4', 1282.14, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (999, 20260003, 'NODE-14', 'NODE-08', '数据', 2647396, 2669542, 185, 62, 462.329, 1, 450541, 22.687, 12998396.62, 24.39, 6117391.00, 35.72, -55.67, 30.62, 212015, 5776, 1833, 37, NULL, 0, -84.31, -120.00, 11.76, 14.47, 0, 0, 'OP-4', 1200.77, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1000, 20260003, 'NODE-10', 'NODE-16', '视频', 2445145, 2446912, 198, 35, 396.378, 1, 409393, 12.872, 5733383.34, 29.18, 2734723.00, 32.76, -77.22, 10.64, 265375, 3888, 1281, 28, NULL, 2, -98.26, -83.19, 22.39, 17.09, 0, 0, 'OP-1', 246.48, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1001, 20260003, 'NODE-07', 'NODE-19', '视频', 1663888, 1669970, 130, 149, 177.777, 1, 109929, 34.001, 12169770.29, 22.16, 7973942.77, 29.20, -31.82, 39.57, 740706, 31807, 2789, 28, NULL, 2, -88.98, -71.94, 15.59, 14.52, 0, 0, 'OP-9', 1175.73, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1002, 20260003, 'NODE-20', 'NODE-08', '视频', 1005278, 1009452, 20, 118, 144.988, 0, 210041, 48.120, 15114437.99, 35.71, 6779895.34, 21.66, -55.16, 1.50, 616085, 103694, 1021, 289, '节点离线', 1, -87.00, -120.00, 30.17, 16.95, 0, 0, 'OP-9', 696.95, 0, 0, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1003, 20260003, 'NODE-01', 'NODE-16', '短消息', 120720, 125910, 183, 50, 383.572, 1, 462872, 31.994, 9646819.19, 22.17, 6561014.13, 38.13, -50.26, 36.87, 685967, 24814, 2177, 30, NULL, 2, -94.00, -66.40, 15.99, 10.96, 0, 1, 'OP-5', 1677.74, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1004, 20260003, 'NODE-18', 'NODE-02', '语音', 1519532, 1548685, 189, 70, 236.428, 1, 225489, 32.286, 18845530.54, 30.35, 14348064.18, 36.57, -33.26, 47.02, 866165, 11490, 3669, 94, NULL, 0, -85.67, -86.88, 23.00, 6.10, 0, 0, 'OP-3', 1547.47, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1005, 20260004, 'NODE-16', 'NODE-04', '短消息', 1988000, 2012293, 196, 89, 257.403, 1, 473653, 18.462, 7003630.95, 25.87, 3558826.84, 22.12, -46.53, 26.81, 202306, 7626, 1910, 39, NULL, 1, -87.43, -65.56, 16.21, 5.63, 0, 0, 'OP-7', 292.73, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1006, 20260004, 'NODE-16', 'NODE-19', '数据', 428016, 431538, 162, 111, 350.885, 1, 296007, 32.646, 19499543.64, 24.96, 8888993.34, 28.38, -87.20, 36.40, 151587, 4738, 781, 11, NULL, 2, -91.32, -71.07, 15.23, 13.17, 0, 0, 'OP-4', 101.97, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1007, 20260004, 'NODE-20', 'NODE-03', '语音', 2785145, 2792001, 85, 126, 198.545, 1, 214785, 23.664, 19338328.91, 20.12, 11646020.57, 33.04, -31.02, 48.82, 830041, 17110, 3549, 71, NULL, 0, -93.90, -73.43, 11.51, 6.98, 0, 0, 'OP-8', 1724.55, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1008, 20260004, 'NODE-14', 'NODE-02', '语音', 273339, 299206, 79, 122, 332.284, 1, 291286, 5.334, 10586424.66, 27.49, 6644668.37, 33.11, -60.13, 15.09, 582071, 14437, 3842, 59, NULL, 2, -91.74, -120.00, 16.50, 14.39, 0, 0, 'OP-2', 1923.31, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1009, 20260004, 'NODE-02', 'NODE-15', '数据', 18127, 41386, 183, 66, 59.803, 1, 150358, 32.664, 11936536.42, 31.81, 9237881.62, 30.06, -52.39, 43.63, 394852, 10925, 4335, 93, NULL, 0, -83.91, -120.00, 26.71, 9.58, 0, 0, 'OP-8', 635.98, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1010, 20260004, 'NODE-09', 'NODE-12', '语音', 688424, 704350, 139, 135, 53.428, 1, 312530, 11.026, 5752268.18, 32.72, 4944922.12, 26.16, -93.76, 28.50, 563830, 2400, 4939, 53, NULL, 0, -83.16, -87.04, 22.32, 3.68, 0, 0, 'OP-1', 564.88, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1011, 20260004, 'NODE-06', 'NODE-03', '短消息', 2065154, 2068314, 94, 70, 462.804, 0, 235631, 89.051, 6054860.24, 29.62, 3536184.22, 22.91, -57.70, 15.58, 496342, 207288, 4740, 966, '节点离线', 2, -86.87, -120.00, 16.16, 5.30, 0, 0, 'OP-4', 1205.10, 1, 0, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1012, 20260004, 'NODE-15', 'NODE-11', '数据', 1338939, 1358785, 62, 92, 228.589, 1, 321377, 15.994, 15208257.01, 31.30, 5659525.42, 27.49, -75.69, 29.73, 327269, 9771, 3443, 12, NULL, 2, -81.75, -120.00, 18.85, 16.21, 0, 1, 'OP-9', 805.50, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1013, 20260004, 'NODE-02', 'NODE-16', '视频', 1392889, 1407063, 77, 151, 324.146, 1, 80092, 19.339, 18403284.31, 21.46, 9238929.37, 36.35, -78.06, 27.60, 278425, 13374, 1234, 3, NULL, 0, -98.26, -67.39, 9.03, 6.92, 0, 0, 'OP-9', 1554.56, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1014, 20260004, 'NODE-04', 'NODE-03', '语音', 850682, 873008, 41, 143, 311.522, 1, 336576, 49.264, 10876906.12, 29.22, 6290682.81, 29.34, -36.54, 46.45, 354783, 3393, 658, 19, NULL, 2, -85.94, -120.00, 22.64, 9.21, 0, 0, 'OP-10', 1833.20, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1015, 20260004, 'NODE-06', 'NODE-07', '短消息', 3393571, 3413242, 161, 131, 52.946, 1, 298503, 18.075, 19809758.76, 27.88, 10839350.10, 34.79, -74.69, 9.65, 508456, 5251, 1594, 37, NULL, 2, -96.64, -120.00, 13.51, 16.47, 0, 0, 'OP-3', 1836.18, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1016, 20260004, 'NODE-08', 'NODE-18', '视频', 2382107, 2391542, 94, 86, 494.282, 1, 443212, 17.875, 15282879.22, 27.56, 11101571.02, 39.69, -36.81, 12.42, 404477, 15651, 348, 2, NULL, 0, -96.30, -120.00, 12.78, 4.12, 0, 0, 'OP-8', 1596.73, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1017, 20260005, 'NODE-18', 'NODE-13', '短消息', 2983134, 3005750, 12, 57, 129.978, 1, 268837, 7.366, 11564031.44, 33.13, 4548664.34, 34.67, -80.43, 32.34, 471136, 15085, 2068, 17, NULL, 0, -81.83, -89.20, 21.50, 10.69, 0, 0, 'OP-6', 1010.18, 0, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1018, 20260005, 'NODE-03', 'NODE-14', '短消息', 537499, 542806, 153, 98, 231.084, 1, 102013, 26.449, 18942656.11, 33.72, 7144542.12, 36.19, -47.76, 4.70, 584701, 14779, 3826, 100, NULL, 0, -97.11, -120.00, 25.88, 8.68, 0, 0, 'OP-6', 240.57, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1019, 20260005, 'NODE-01', 'NODE-11', '视频', 3151799, 3168033, 182, 50, 215.741, 1, 406623, 20.634, 8555539.20, 26.48, 5186488.73, 24.37, -41.53, 46.56, 701703, 32832, 948, 4, NULL, 0, -97.61, -120.00, 13.00, 8.82, 0, 0, 'OP-3', 584.68, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1020, 20260005, 'NODE-13', 'NODE-17', '语音', 1483396, 1485895, 69, 25, 375.350, 0, 383026, 148.327, 6932444.25, 27.35, 2936763.64, 26.19, -84.34, 8.12, 298518, 34710, 2296, 799, '信道忙', 0, -99.49, -120.00, 18.94, 17.02, 0, 0, 'OP-6', 892.16, 1, 0, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1021, 20260005, 'NODE-16', 'NODE-05', '数据', 510023, 512411, 14, 51, 357.958, 1, 426637, 17.832, 18296430.83, 26.86, 14125703.16, 22.80, -83.10, 20.31, 502214, 3633, 1690, 13, NULL, 0, -87.03, -120.00, 18.31, 12.94, 0, 0, 'OP-10', 1222.92, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1022, 20260005, 'NODE-18', 'NODE-14', '语音', 1078046, 1078632, 29, 24, 389.333, 0, 442643, 108.150, 16093287.26, 22.17, 9979830.67, 20.73, -95.86, 11.40, 961656, 414236, 2959, 536, '节点离线', 0, -98.89, -120.00, 12.92, 18.63, 0, 0, 'OP-10', 696.55, 1, 0, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1023, 20260005, 'NODE-18', 'NODE-01', '语音', 3383868, 3397862, 82, 37, 389.716, 1, 284030, 17.096, 6532032.38, 23.89, 3986697.97, 32.10, -51.44, 37.95, 416678, 1137, 3255, 7, NULL, 2, -93.96, -82.57, 18.19, 5.44, 0, 0, 'OP-3', 319.47, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1024, 20260005, 'NODE-08', 'NODE-06', '短消息', 3439792, 3448496, 198, 99, 283.076, 1, 6018, 35.195, 16795420.34, 21.81, 13082796.07, 39.92, -22.72, 4.79, 513454, 21077, 169, 4, NULL, 2, -83.52, -120.00, 10.08, 10.28, 0, 0, 'OP-3', 1097.68, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1025, 20260005, 'NODE-20', 'NODE-08', '数据', 1842801, 1853812, 135, 106, 236.724, 1, 396581, 45.012, 7622391.37, 34.20, 6800501.74, 23.95, -74.04, 7.46, 161496, 6863, 4712, 5, NULL, 1, -96.43, -120.00, 23.22, 10.48, 0, 0, 'OP-4', 495.54, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1026, 20260005, 'NODE-11', 'NODE-08', '数据', 1844419, 1848654, 12, 90, 328.729, 1, 290198, 34.350, 13864685.61, 37.44, 6045670.68, 28.74, -89.30, 25.36, 949939, 1437, 3189, 93, NULL, 0, -89.45, -120.00, 30.13, 10.54, 0, 0, 'OP-5', 353.00, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1027, 20260005, 'NODE-16', 'NODE-05', '语音', 479365, 509106, 57, 123, 119.015, 1, 126993, 39.836, 13275807.58, 22.16, 5877079.19, 21.26, -59.72, 8.42, 975183, 20878, 3572, 1, NULL, 2, -88.20, -120.00, 12.96, 11.83, 0, 0, 'OP-6', 736.33, 0, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1028, 20260005, 'NODE-13', 'NODE-04', '短消息', 2109421, 2123680, 160, 81, 401.419, 1, 181649, 32.972, 16971833.13, 25.59, 11946996.13, 32.64, -37.00, 3.73, 492102, 17265, 4127, 52, NULL, 1, -98.20, -120.00, 19.72, 16.01, 0, 0, 'OP-9', 274.47, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1029, 20260005, 'NODE-11', 'NODE-10', '数据', 2823260, 2828721, 135, 74, 290.219, 1, 109047, 35.163, 7289508.77, 39.91, 6502916.70, 32.06, -76.25, 10.10, 974868, 22542, 1009, 8, NULL, 0, -85.83, -120.00, 34.58, 8.85, 0, 0, 'OP-5', 1623.59, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1030, 20260006, 'NODE-06', 'NODE-09', '语音', 1680504, 1701192, 197, 43, 62.409, 1, 111471, 44.725, 15584711.75, 33.49, 4921841.97, 25.59, -42.99, 37.94, 598693, 22924, 4700, 125, NULL, 1, -99.73, -120.00, 19.01, 5.12, 0, 0, 'OP-3', 1500.80, 0, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1031, 20260006, 'NODE-19', 'NODE-18', '语音', 3393143, 3409296, 118, 121, 205.788, 1, 118706, 19.574, 18183005.94, 20.42, 14450463.08, 27.47, -57.86, 48.72, 651233, 18011, 4382, 69, NULL, 0, -86.20, -120.00, 10.32, 3.98, 0, 0, 'OP-1', 861.30, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1032, 20260006, 'NODE-14', 'NODE-06', '语音', 417650, 426989, 41, 137, 203.823, 1, 326398, 13.651, 16273808.67, 37.67, 8393510.59, 23.29, -81.83, 13.72, 73939, 696, 1164, 5, NULL, 0, -97.07, -120.00, 29.37, 10.99, 0, 0, 'OP-9', 486.42, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1033, 20260006, 'NODE-14', 'NODE-15', '视频', 1408559, 1415227, 48, 55, 431.432, 1, 203018, 30.212, 10449962.51, 32.81, 6622108.62, 27.47, -70.09, 14.27, 17615, 550, 1302, 30, NULL, 0, -88.08, -78.96, 19.07, 14.56, 0, 1, 'OP-7', 1476.15, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1034, 20260006, 'NODE-03', 'NODE-09', '语音', 2286080, 2298624, 151, 14, 381.360, 1, 310534, 5.700, 11638289.66, 38.57, 6252557.12, 23.89, -93.55, 18.57, 874259, 38947, 4140, 31, NULL, 0, -96.25, -120.00, 25.96, 6.59, 0, 0, 'OP-10', 527.17, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1035, 20260006, 'NODE-07', 'NODE-09', '短消息', 3096410, 3097277, 14, 109, 353.224, 1, 499123, 30.480, 6999646.51, 32.58, 2794427.90, 31.49, -69.67, 29.28, 855551, 27347, 385, 7, NULL, 1, -97.93, -76.39, 23.86, 14.14, 0, 0, 'OP-5', 591.45, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1036, 20260006, 'NODE-20', 'NODE-06', '短消息', 746626, 748937, 134, 172, 157.853, 1, 365264, 25.493, 7082692.89, 33.04, 2930509.44, 33.90, -75.99, 36.19, 773943, 15345, 4335, 17, NULL, 1, -87.38, -89.38, 18.65, 8.96, 0, 0, 'OP-7', 1385.63, 0, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1037, 20260006, 'NODE-02', 'NODE-03', '数据', 2291123, 2320141, 62, 127, 263.606, 1, 317372, 44.042, 7137533.38, 32.89, 4351905.58, 33.24, -27.14, 18.11, 73378, 872, 1970, 57, NULL, 2, -80.34, -82.23, 21.98, 10.53, 0, 1, 'OP-9', 636.94, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1038, 20260006, 'NODE-11', 'NODE-15', '短消息', 586980, 608448, 176, 179, 219.624, 1, 94074, 5.666, 11949577.59, 24.01, 10084631.10, 35.55, -73.16, 44.08, 136956, 6074, 4015, 21, NULL, 0, -98.44, -74.48, 9.15, 9.63, 0, 0, 'OP-6', 1460.77, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1039, 20260006, 'NODE-09', 'NODE-16', '短消息', 375388, 403409, 10, 38, 145.558, 1, 391807, 38.282, 9716340.62, 28.16, 7246328.56, 32.12, -81.08, 11.93, 93405, 2768, 2508, 9, NULL, 0, -82.33, -120.00, 17.45, 14.50, 0, 0, 'OP-1', 1099.72, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1040, 20260006, 'NODE-15', 'NODE-12', '视频', 1467642, 1475722, 170, 131, 388.857, 1, 456811, 13.479, 15923524.38, 28.67, 9381799.28, 34.51, -73.80, 35.37, 410820, 4368, 2727, 57, NULL, 2, -82.42, -120.00, 20.26, 7.66, 0, 1, 'OP-10', 506.10, 0, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1041, 20260007, 'NODE-19', 'NODE-15', '语音', 2214832, 2218686, 27, 191, 217.619, 0, 145012, 146.798, 14015762.23, 27.87, 7774961.04, 34.41, -48.07, 6.61, 152825, 49548, 4905, 1851, '功率不足', 1, -87.00, -61.30, 18.31, 14.98, 0, 0, 'OP-5', 453.27, 0, 0, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1042, 20260007, 'NODE-05', 'NODE-11', '视频', 1734858, 1743007, 100, 32, 168.565, 1, 92079, 41.446, 14453416.92, 36.20, 10053767.56, 25.03, -45.30, 49.55, 836135, 6420, 2302, 46, NULL, 1, -95.43, -64.13, 23.52, 6.37, 0, 0, 'OP-7', 806.54, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1043, 20260007, 'NODE-03', 'NODE-19', '视频', 202146, 206462, 180, 134, 72.759, 1, 483101, 29.827, 10513895.28, 32.79, 3360517.04, 21.68, -68.80, 31.14, 324307, 15806, 647, 11, NULL, 2, -98.40, -120.00, 20.81, 8.37, 0, 0, 'OP-5', 1387.65, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1044, 20260007, 'NODE-09', 'NODE-05', '视频', 2132415, 2152735, 137, 172, 267.925, 1, 377309, 37.068, 18198324.36, 24.57, 6095668.64, 36.64, -83.07, 32.23, 863239, 21130, 1173, 16, NULL, 1, -83.33, -120.00, 18.89, 6.25, 0, 0, 'OP-2', 417.82, 0, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1045, 20260007, 'NODE-19', 'NODE-16', '短消息', 40522, 51409, 85, 52, 478.090, 1, 243280, 38.887, 13484232.89, 36.49, 5304990.68, 23.75, -90.71, 21.57, 358653, 2464, 4888, 63, NULL, 1, -85.93, -68.22, 24.16, 4.91, 0, 0, 'OP-6', 698.32, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1046, 20260007, 'NODE-16', 'NODE-03', '视频', 3526912, 3546161, 92, 10, 58.301, 1, 23902, 45.484, 13137838.63, 35.20, 4599490.10, 27.23, -70.35, 16.15, 485216, 12205, 938, 16, NULL, 0, -88.19, -120.00, 26.58, 19.20, 0, 0, 'OP-6', 700.40, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1047, 20260007, 'NODE-15', 'NODE-01', '视频', 2861183, 2887137, 135, 108, 369.804, 1, 393943, 46.877, 11378406.04, 21.98, 4000112.03, 21.67, -55.92, 18.27, 160585, 3976, 771, 21, NULL, 2, -89.99, -78.55, 13.97, 9.15, 0, 1, 'OP-9', 1327.40, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1048, 20260007, 'NODE-04', 'NODE-12', '数据', 1983447, 1992882, 92, 82, 227.420, 1, 480772, 31.396, 11091544.99, 22.11, 7909921.25, 28.76, -31.83, 25.88, 820493, 26913, 3565, 93, NULL, 1, -93.14, -75.29, 10.87, 4.45, 0, 0, 'OP-3', 1912.92, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1049, 20260007, 'NODE-04', 'NODE-19', '视频', 2226945, 2238121, 114, 57, 232.762, 1, 216020, 15.782, 5717973.23, 26.59, 3159969.68, 28.68, -60.64, 29.65, 776668, 26647, 537, 13, NULL, 0, -90.59, -120.00, 13.70, 3.01, 0, 0, 'OP-8', 1787.25, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1050, 20260007, 'NODE-04', 'NODE-08', '短消息', 2763885, 2774746, 171, 163, 265.575, 1, 83001, 27.154, 10984535.53, 31.08, 5031613.94, 35.32, -84.37, 13.77, 974678, 41623, 1861, 31, NULL, 0, -89.79, -64.07, 19.11, 7.35, 0, 0, 'OP-2', 632.20, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1051, 20260007, 'NODE-10', 'NODE-06', '视频', 3146844, 3172691, 69, 188, 342.679, 1, 313361, 46.791, 13446814.26, 23.34, 9968489.42, 32.08, -39.64, 7.80, 486354, 10538, 2446, 1, NULL, 2, -82.31, -120.00, 16.23, 7.07, 0, 0, 'OP-4', 676.88, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1052, 20260007, 'NODE-20', 'NODE-07', '语音', 821986, 850958, 61, 20, 226.717, 1, 317217, 5.322, 8863770.20, 31.17, 7053232.00, 35.56, -36.77, 41.54, 350075, 11694, 3837, 32, NULL, 2, -96.82, -70.13, 17.18, 7.27, 0, 0, 'OP-9', 542.63, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1053, 20260007, 'NODE-17', 'NODE-19', '视频', 1422237, 1440359, 175, 120, 135.804, 1, 227455, 43.306, 8921956.76, 38.86, 7371200.77, 39.77, -45.72, 34.38, 625568, 5344, 520, 13, NULL, 1, -80.23, -65.16, 32.28, 6.88, 0, 0, 'OP-10', 1701.29, 0, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1054, 20260008, 'NODE-05', 'NODE-20', '数据', 146797, 175039, 124, 68, 209.985, 1, 383512, 43.200, 15849989.69, 22.43, 6313282.21, 29.31, -48.08, 4.32, 804770, 189, 1540, 16, NULL, 0, -83.33, -120.00, 16.32, 12.63, 0, 0, 'OP-3', 490.11, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1055, 20260008, 'NODE-10', 'NODE-12', '语音', 1097358, 1119072, 197, 101, 425.584, 1, 198974, 42.037, 8232710.38, 32.76, 5303010.43, 26.09, -50.99, 6.49, 205260, 8599, 2342, 0, NULL, 0, -98.33, -120.00, 26.28, 3.87, 0, 0, 'OP-6', 931.44, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1056, 20260008, 'NODE-08', 'NODE-06', '数据', 2236202, 2264300, 165, 152, 424.161, 1, 341295, 22.466, 10270275.68, 26.94, 5124775.02, 22.05, -71.96, 26.73, 36849, 5, 3284, 38, NULL, 1, -86.74, -62.38, 20.88, 9.46, 0, 0, 'OP-8', 791.94, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1057, 20260008, 'NODE-16', 'NODE-02', '短消息', 3368883, 3397259, 82, 24, 367.895, 1, 443550, 45.691, 11341967.69, 27.02, 6141981.52, 21.49, -70.53, 26.78, 876163, 19198, 2852, 6, NULL, 1, -94.29, -120.00, 16.37, 18.26, 0, 1, 'OP-1', 485.65, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1058, 20260008, 'NODE-17', 'NODE-02', '语音', 1058490, 1075489, 187, 168, 80.295, 1, 321633, 48.578, 6897947.62, 34.70, 2194242.41, 21.05, -67.46, 6.10, 617627, 19275, 2072, 39, NULL, 0, -90.42, -88.56, 19.98, 19.54, 0, 0, 'OP-2', 663.14, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1059, 20260008, 'NODE-04', 'NODE-17', '数据', 3379649, 3399919, 48, 15, 163.264, 1, 182634, 47.237, 12909263.57, 24.26, 6922581.30, 38.32, -59.68, 16.98, 184553, 5504, 2511, 62, NULL, 1, -96.64, -73.24, 16.43, 17.45, 0, 1, 'OP-9', 529.86, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1060, 20260008, 'NODE-05', 'NODE-01', '数据', 2622190, 2625308, 93, 142, 176.331, 0, 205706, 61.955, 11654540.13, 30.75, 8820495.32, 30.11, -55.19, 14.23, 515771, 130337, 1002, 287, '功率不足', 0, -81.72, -72.69, 22.82, 3.99, 0, 1, 'OP-9', 1348.08, 1, 0, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1061, 20260008, 'NODE-11', 'NODE-18', '数据', 2343172, 2359230, 86, 59, 416.326, 1, 22737, 25.689, 18036450.01, 30.24, 12387617.46, 35.32, -40.28, 14.31, 585847, 18071, 3387, 29, NULL, 2, -94.24, -74.21, 20.69, 9.98, 0, 0, 'OP-4', 942.24, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1062, 20260008, 'NODE-04', 'NODE-15', '语音', 2952540, 2956209, 72, 135, 174.723, 1, 381463, 16.526, 17714523.91, 32.57, 8072573.40, 34.97, -51.79, 20.27, 27719, 1309, 2123, 1, NULL, 2, -84.95, -120.00, 25.96, 10.87, 0, 0, 'OP-9', 756.74, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1063, 20260008, 'NODE-01', 'NODE-09', '数据', 641721, 656593, 127, 185, 253.796, 1, 347039, 27.375, 5277507.51, 32.31, 4238196.14, 30.14, -81.88, 25.22, 702333, 2778, 2084, 23, NULL, 1, -96.63, -84.71, 25.08, 11.03, 0, 0, 'OP-10', 1479.40, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1064, 20260009, 'NODE-15', 'NODE-03', '数据', 2770763, 2781239, 16, 32, 199.447, 1, 164309, 24.665, 15439109.85, 35.86, 12447739.98, 38.45, -65.07, 36.48, 376333, 7121, 849, 10, NULL, 2, -90.24, -75.83, 23.93, 5.39, 0, 0, 'OP-3', 1106.84, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1065, 20260009, 'NODE-09', 'NODE-06', '视频', 3463870, 3482102, 107, 63, 327.408, 1, 358122, 33.821, 8467653.41, 21.06, 3778020.27, 21.88, -57.76, 24.19, 648300, 17644, 4380, 76, NULL, 0, -92.58, -120.00, 12.15, 15.35, 0, 0, 'OP-9', 539.30, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1066, 20260009, 'NODE-20', 'NODE-06', '语音', 1304277, 1321247, 106, 57, 170.279, 1, 142257, 30.501, 12998558.88, 23.70, 11640834.82, 37.65, -24.42, 3.36, 658634, 16580, 3713, 93, NULL, 1, -82.55, -120.00, 15.79, 11.92, 0, 0, 'OP-8', 1116.57, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1067, 20260009, 'NODE-04', 'NODE-09', '短消息', 3041689, 3052591, 133, 65, 157.403, 1, 124448, 23.357, 5956465.93, 39.00, 2490804.57, 22.66, -61.45, 23.48, 980946, 49000, 2460, 56, NULL, 0, -92.74, -120.00, 30.68, 18.14, 0, 0, 'OP-8', 396.00, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1068, 20260009, 'NODE-11', 'NODE-16', '数据', 2681377, 2706340, 31, 73, 299.946, 1, 2623, 19.670, 6790750.65, 37.42, 3377792.48, 30.35, -31.55, 28.40, 371625, 11170, 3303, 93, NULL, 0, -83.37, -120.00, 27.88, 11.79, 0, 0, 'OP-5', 130.29, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1069, 20260009, 'NODE-14', 'NODE-09', '数据', 454260, 465063, 79, 55, 213.158, 1, 230426, 36.653, 15399708.81, 25.62, 10800466.79, 27.41, -77.34, 13.63, 821424, 25899, 4617, 73, NULL, 1, -80.37, -61.59, 20.08, 17.16, 0, 0, 'OP-1', 783.39, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1070, 20260009, 'NODE-02', 'NODE-05', '短消息', 1963616, 1966803, 142, 192, 130.769, 1, 379377, 42.461, 13650680.73, 31.71, 6131492.28, 32.42, -34.41, 17.14, 461944, 6662, 789, 11, NULL, 2, -87.59, -81.51, 25.37, 13.41, 0, 0, 'OP-9', 819.37, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1071, 20260009, 'NODE-15', 'NODE-04', '短消息', 1543592, 1557400, 81, 156, 420.474, 1, 143797, 12.841, 12468137.97, 33.55, 4794178.10, 21.25, -60.00, 48.03, 12322, 601, 1223, 20, NULL, 1, -94.72, -84.83, 20.25, 5.32, 0, 0, 'OP-7', 309.50, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1072, 20260009, 'NODE-12', 'NODE-18', '短消息', 2228475, 2247448, 25, 152, 491.398, 1, 53893, 39.674, 10767163.10, 23.21, 5168883.84, 38.66, -24.50, 10.05, 598711, 4667, 2280, 31, NULL, 1, -95.54, -120.00, 12.44, 12.27, 0, 0, 'OP-1', 632.23, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1073, 20260009, 'NODE-03', 'NODE-15', '数据', 1267296, 1284414, 175, 131, 486.700, 1, 305998, 5.401, 13388468.82, 20.95, 5123053.41, 27.92, -77.81, 47.46, 528437, 3595, 3377, 32, NULL, 1, -80.84, -64.02, 6.61, 7.33, 0, 0, 'OP-4', 1392.55, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1074, 20260010, 'NODE-14', 'NODE-15', '视频', 2037419, 2038596, 71, 179, 367.803, 1, 328088, 12.641, 18821026.84, 27.39, 10738418.43, 29.55, -58.59, 8.66, 837679, 37824, 1191, 1, NULL, 2, -99.47, -89.87, 16.13, 14.58, 0, 0, 'OP-10', 1797.85, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1075, 20260010, 'NODE-05', 'NODE-14', '短消息', 1802396, 1823163, 179, 133, 371.714, 1, 489001, 24.236, 7499004.19, 38.71, 6610637.96, 33.72, -84.79, 43.44, 150146, 5390, 2746, 68, NULL, 1, -88.63, -120.00, 29.26, 18.53, 0, 0, 'OP-5', 501.35, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1076, 20260010, 'NODE-03', 'NODE-13', '视频', 117652, 118999, 53, 75, 148.772, 1, 451385, 47.354, 11830984.63, 37.49, 6128998.35, 38.90, -41.19, 39.50, 906245, 42825, 3141, 0, NULL, 1, -92.17, -120.00, 31.94, 13.77, 0, 0, 'OP-2', 600.63, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1077, 20260010, 'NODE-07', 'NODE-01', '数据', 515496, 536235, 106, 48, 465.780, 1, 115908, 33.050, 14927727.81, 33.66, 7398458.25, 29.56, -36.09, 10.65, 132098, 1903, 3229, 48, NULL, 0, -80.92, -120.00, 24.61, 7.41, 0, 0, 'OP-3', 1715.84, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1078, 20260010, 'NODE-13', 'NODE-06', '短消息', 2732829, 2752624, 81, 162, 371.244, 1, 278411, 9.142, 15701824.77, 25.44, 9668077.20, 31.92, -64.00, 34.88, 149373, 473, 2285, 5, NULL, 1, -88.47, -65.37, 19.82, 16.96, 0, 1, 'OP-2', 1988.74, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1079, 20260010, 'NODE-02', 'NODE-11', '数据', 739367, 751081, 180, 152, 277.164, 1, 24897, 9.126, 8071180.03, 31.90, 3313935.09, 35.96, -36.48, 1.90, 144560, 2126, 225, 5, NULL, 2, -87.32, -67.52, 20.74, 3.90, 0, 0, 'OP-7', 429.26, 0, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1080, 20260010, 'NODE-08', 'NODE-20', '数据', 1394959, 1424036, 139, 80, 469.072, 1, 223448, 29.992, 6038437.93, 23.29, 5253793.49, 31.36, -65.07, 49.01, 176046, 81, 4542, 122, NULL, 0, -86.99, -120.00, 12.38, 10.02, 0, 1, 'OP-3', 1977.28, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1081, 20260010, 'NODE-04', 'NODE-08', '语音', 682594, 708086, 169, 107, 339.615, 1, 470214, 40.441, 15012145.20, 29.36, 12566426.66, 32.44, -55.04, 29.20, 756507, 11911, 435, 7, NULL, 1, -98.99, -62.96, 14.58, 18.62, 0, 0, 'OP-7', 1087.42, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1082, 20260010, 'NODE-17', 'NODE-12', '短消息', 1054255, 1057037, 14, 153, 428.302, 0, 3237, 21.539, 18006581.52, 24.29, 8038836.29, 26.00, -93.46, 24.10, 410025, 123332, 2069, 349, '功率不足', 1, -89.44, -72.08, 11.38, 11.77, 0, 0, 'OP-1', 412.26, 1, 0, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1083, 20260010, 'NODE-11', 'NODE-19', '语音', 905916, 927181, 166, 112, 410.426, 1, 161457, 21.652, 15048854.39, 35.89, 12600067.38, 38.05, -41.63, 47.11, 379917, 9701, 4932, 110, NULL, 1, -96.12, -120.00, 22.34, 11.64, 0, 0, 'OP-6', 463.86, 1, 1, NULL, '2026-03-25 19:46:46');
INSERT INTO `records_military_communication_info` VALUES (1084, 20260010, 'NODE-02', 'NODE-01', '语音', 598708, 612258, 146, 130, 79.476, 1, 473043, 38.290, 10768072.64, 21.15, 7471315.53, 39.78, -27.16, 6.14, 674184, 25445, 4520, 86, NULL, 2, -82.88, -78.84, 13.95, 6.99, 1, 0, 'OP-5', 163.37, 1, 1, NULL, '2026-03-25 19:46:46');

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
) ENGINE = InnoDB AUTO_INCREMENT = 116 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '作战信息基础表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of records_military_operation_info
-- ----------------------------
INSERT INTO `records_military_operation_info` VALUES (106, 20260001, 1184, 58, 5, 133, 23, 60, 10, 9.7, 797, 12.44, 158, 0, 0.85, 4610.37, 65185.26, 34026.85, 139.46, 0.95, 1075.34, 4871.50, '大雾', 13.5, 2, '作战编号 20260001，整体表现优秀', '2026-03-25 19:46:46');
INSERT INTO `records_military_operation_info` VALUES (107, 20260002, 907, 51, 5, 90, 27, 54, 15, 10.1, 962, 16.35, 136, 1, 0.95, 3370.00, 38619.29, 16814.00, 50.19, 0.94, 1175.41, 3742.83, '小雨', 10.8, 2, '军事行动 20260002 的优秀评估', '2026-03-25 19:46:46');
INSERT INTO `records_military_operation_info` VALUES (108, 20260003, 1331, 58, 5, 136, 12, 39, 19, 9.7, 542, 19.85, 120, 0, 0.83, 3855.73, 37840.32, 27257.94, 28.14, 0.91, 1577.52, 853.48, '雷雨', 13.6, 2, '批次 20260003 的优秀记录', '2026-03-25 19:46:46');
INSERT INTO `records_military_operation_info` VALUES (109, 20260004, 681, 53, 5, 86, 22, 78, 16, 13.6, 538, 19.65, 165, 0, 0.96, 3555.13, 40244.17, 8201.21, 134.29, 0.95, 422.15, 2690.23, '小雨', 23.7, 1, '作战编号 20260004，整体表现优秀', '2026-03-25 19:46:46');
INSERT INTO `records_military_operation_info` VALUES (110, 20260005, 1355, 24, 5, 46, 25, 66, 13, 8.9, 989, 13.79, 196, 1, 0.90, 3072.30, 68374.26, 10737.13, 180.35, 0.94, 826.09, 4543.96, '多云', 25.9, 2, '批次 20260005 的优秀记录', '2026-03-25 19:46:46');
INSERT INTO `records_military_operation_info` VALUES (111, 20260006, 1157, 25, 6, 37, 15, 62, 13, 13.2, 344, 13.06, 184, 0, 0.87, 1858.17, 31804.23, 25334.86, 185.97, 0.96, 773.82, 4055.42, '雷雨', 23.1, 2, '作战编号 20260006，整体表现优秀', '2026-03-25 19:46:46');
INSERT INTO `records_military_operation_info` VALUES (112, 20260007, 610, 45, 6, 105, 16, 30, 12, 8.1, 927, 16.74, 70, 0, 0.87, 2278.43, 68906.00, 30277.24, 186.57, 0.98, 488.97, 4743.45, '雷雨', 25.0, 2, '作战编号 20260007，整体表现优秀', '2026-03-25 19:46:46');
INSERT INTO `records_military_operation_info` VALUES (113, 20260008, 1055, 44, 6, 84, 15, 71, 12, 11.0, 698, 14.99, 53, 1, 0.95, 2687.81, 13718.17, 15239.30, 75.91, 0.94, 1569.62, 2584.86, '大雾', 26.8, 1, '作战编号 20260008，整体表现优秀', '2026-03-25 19:46:46');
INSERT INTO `records_military_operation_info` VALUES (114, 20260009, 1186, 32, 6, 54, 19, 51, 10, 14.7, 219, 17.52, 95, 1, 0.84, 4459.64, 53702.53, 22686.16, 126.87, 0.93, 1459.54, 1510.07, '雷雨', 28.7, 2, '作战编号 20260009，整体表现优秀', '2026-03-25 19:46:46');
INSERT INTO `records_military_operation_info` VALUES (115, 20260010, 1181, 46, 5, 131, 20, 71, 7, 12.4, 686, 18.88, 199, 1, 0.93, 4548.10, 46736.79, 13915.01, 143.22, 0.95, 1469.72, 1243.90, '阴', 29.2, 1, '批次 20260010 的优秀记录', '2026-03-25 19:46:46');

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
) ENGINE = InnoDB AUTO_INCREMENT = 419 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '安全事件表，记录密钥泄露、被拦截等安全事件' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of records_security_events
-- ----------------------------
INSERT INTO `records_security_events` VALUES (408, 20260001, 'intercepted', 985621, 'NODE-16', 'KEY-0045', 20372751, '信号截获设备', '通信内容...', '相关检测', NULL, '轻微', NULL);
INSERT INTO `records_security_events` VALUES (409, 20260003, 'detected_by_enemy', 495287, 'NODE-16', 'KEY-0097', 24713933, '信号截获设备', NULL, NULL, NULL, '危急', NULL);
INSERT INTO `records_security_events` VALUES (410, 20260003, 'intercepted', 2369585, 'NODE-01', 'KEY-0055', 77608079, '电子对抗单元', '通信内容...', '频率分析', NULL, '轻微', NULL);
INSERT INTO `records_security_events` VALUES (411, 20260005, 'key_leak', 290951, 'NODE-01', 'KEY-0006', 9088579, NULL, NULL, NULL, NULL, '一般', NULL);
INSERT INTO `records_security_events` VALUES (412, 20260005, 'key_leak', 1774435, 'NODE-16', 'KEY-0033', 49346498, NULL, NULL, NULL, NULL, '轻微', NULL);
INSERT INTO `records_security_events` VALUES (413, 20260007, 'detected_by_enemy', 191146, 'NODE-04', 'KEY-0058', 4383161, '电子对抗单元', NULL, NULL, NULL, '一般', NULL);
INSERT INTO `records_security_events` VALUES (414, 20260007, 'detected_by_enemy', 1645101, 'NODE-18', 'KEY-0013', 57321466, '信号截获设备', NULL, NULL, NULL, '一般', NULL);
INSERT INTO `records_security_events` VALUES (415, 20260008, 'key_leak', 3203951, 'NODE-03', 'KEY-0042', 36960479, NULL, NULL, NULL, NULL, '轻微', NULL);
INSERT INTO `records_security_events` VALUES (416, 20260008, 'detected_by_enemy', 3136108, 'NODE-13', 'KEY-0082', 34010858, '信号截获设备', NULL, NULL, NULL, '严重', NULL);
INSERT INTO `records_security_events` VALUES (417, 20260009, 'intercepted', 2124867, 'NODE-02', 'KEY-0019', 34945989, '敌方侦察系统', NULL, '侧信道攻击', NULL, '轻微', NULL);
INSERT INTO `records_security_events` VALUES (418, 20260010, 'detected_by_enemy', 3136852, 'NODE-19', 'KEY-0048', 21055699, '敌方侦察系统', NULL, NULL, NULL, '严重', NULL);

-- ----------------------------
-- Table structure for score_military_comm_effect
-- ----------------------------
DROP TABLE IF EXISTS `score_military_comm_effect`;
CREATE TABLE `score_military_comm_effect`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `evaluation_id` varchar(50) CHARACTER SET utf8mb4  NOT NULL COMMENT '评估批次ID',
  `operation_id` varchar(50) CHARACTER SET utf8mb4  NOT NULL COMMENT '作战任务ID（与各层级表关联）',
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
) ENGINE = InnoDB AUTO_INCREMENT = 84 CHARACTER SET = utf8mb4  COMMENT = '军事作战效果指标表（带一级指标前缀，全量化得分版）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of score_military_comm_effect
-- ----------------------------
INSERT INTO `score_military_comm_effect` VALUES (64, 'EVAL-BATCH-20260325163853228-706F025F', '20260001', 1.00, 0.73, 0.55, 0.50, 0.96, 0.50, 0.75, 0.74, 0.45, 0.00, 0.82, 0.31, 0.38, 0.81, 0.32, 1.00, 0.13, 1.00, '2026-03-25 16:39:25', '2026-03-25 16:39:25');
INSERT INTO `score_military_comm_effect` VALUES (65, 'EVAL-BATCH-20260325163853228-706F025F', '20260002', 1.00, 0.57, 0.86, 0.50, 0.20, 0.69, 0.60, 1.00, 0.80, 0.98, 0.63, 0.38, 0.72, 0.69, 0.34, 0.07, 1.00, 0.46, '2026-03-25 16:39:25', '2026-03-25 16:39:25');
INSERT INTO `score_military_comm_effect` VALUES (66, 'EVAL-BATCH-20260325163853228-706F025F', '20260003', 0.67, 0.00, 1.00, 0.50, 0.13, 0.00, 0.00, 0.65, 0.00, 0.53, 0.00, 0.48, 0.00, 0.37, 0.34, 0.76, 0.20, 0.00, '2026-03-25 16:39:25', '2026-03-25 16:39:25');
INSERT INTO `score_military_comm_effect` VALUES (67, 'EVAL-BATCH-20260325163853228-706F025F', '20260004', 0.00, 0.18, 0.00, 0.50, 0.69, 0.57, 1.00, 0.96, 0.57, 0.37, 1.00, 0.28, 1.00, 1.00, 0.53, 0.92, 0.35, 0.79, '2026-03-25 16:39:25', '2026-03-25 16:39:25');
INSERT INTO `score_military_comm_effect` VALUES (68, 'EVAL-BATCH-20260325163853228-706F025F', '20260005', 0.67, 0.62, 0.75, 0.50, 0.31, 0.40, 0.09, 0.36, 1.00, 1.00, 0.63, 1.00, 0.82, 0.98, 1.00, 0.71, 1.00, 0.81, '2026-03-25 16:39:25', '2026-03-25 16:39:25');
INSERT INTO `score_military_comm_effect` VALUES (69, 'EVAL-BATCH-20260325163853228-706F025F', '20260006', 0.67, 1.00, 1.00, 0.00, 0.45, 0.80, 0.46, 0.81, 0.79, 0.79, 0.90, 0.79, 0.91, 0.49, 0.91, 0.00, 0.80, 0.82, '2026-03-25 16:39:25', '2026-03-25 16:39:25');
INSERT INTO `score_military_comm_effect` VALUES (70, 'EVAL-BATCH-20260325163853228-706F025F', '20260007', 0.00, 1.00, 0.00, 1.00, 1.00, 0.94, 0.73, 0.43, 0.52, 0.18, 0.98, 0.62, 0.49, 0.00, 0.54, 0.49, 0.47, 0.65, '2026-03-25 16:39:25', '2026-03-25 16:39:25');
INSERT INTO `score_military_comm_effect` VALUES (71, 'EVAL-BATCH-20260325163853228-706F025F', '20260008', 0.67, 0.57, 0.86, 0.50, 0.71, 1.00, 0.58, 0.44, 0.36, 0.85, 0.18, 0.00, 0.40, 0.20, 0.00, 0.76, 0.66, 0.95, '2026-03-25 16:39:25', '2026-03-25 16:39:25');
INSERT INTO `score_military_comm_effect` VALUES (72, 'EVAL-BATCH-20260325163853228-706F025F', '20260009', 0.67, 0.77, 0.46, 0.50, 0.26, 0.70, 0.77, 0.00, 0.28, 0.68, 0.78, 0.31, 0.50, 0.02, 0.84, 0.97, 0.63, 0.50, '2026-03-25 16:39:25', '2026-03-25 16:39:25');
INSERT INTO `score_military_comm_effect` VALUES (73, 'EVAL-BATCH-20260325163853228-706F025F', '20260010', 0.67, 0.75, 0.50, 1.00, 0.00, 0.57, 0.73, 0.45, 0.26, 0.00, 0.95, 0.52, 0.50, 0.38, 0.65, 0.42, 0.00, 0.65, '2026-03-25 16:39:25', '2026-03-25 16:39:25');
INSERT INTO `score_military_comm_effect` VALUES (74, 'EVAL-BATCH-20260325194702753-6A9AB557', '20260001', 1.00, 1.00, 0.26, 1.00, 0.42, 0.34, 0.00, 0.01, 0.83, 0.46, 0.00, 0.40, 0.78, 1.00, 1.00, 1.00, 0.50, 1.00, '2026-03-25 19:47:25', '2026-03-25 19:47:25');
INSERT INTO `score_military_comm_effect` VALUES (75, 'EVAL-BATCH-20260325194702753-6A9AB557', '20260002', 1.00, 0.00, 0.71, 1.00, 0.02, 0.09, 0.90, 0.19, 0.71, 1.00, 0.72, 0.20, 0.75, 0.58, 0.90, 0.61, 1.00, 0.92, '2026-03-25 19:47:25', '2026-03-25 19:47:25');
INSERT INTO `score_military_comm_effect` VALUES (76, 'EVAL-BATCH-20260325194702753-6A9AB557', '20260003', 1.00, 0.36, 0.61, 0.00, 0.89, 0.45, 0.62, 0.46, 0.85, 0.68, 0.73, 0.70, 0.51, 0.52, 0.65, 1.00, 0.41, 1.00, '2026-03-25 19:47:25', '2026-03-25 19:47:25');
INSERT INTO `score_military_comm_effect` VALUES (77, 'EVAL-BATCH-20260325194702753-6A9AB557', '20260004', 1.00, 1.00, 0.28, 1.00, 0.20, 0.81, 1.00, 0.76, 0.84, 0.13, 0.75, 0.40, 0.00, 0.23, 0.75, 1.00, 0.46, 0.95, '2026-03-25 19:47:25', '2026-03-25 19:47:25');
INSERT INTO `score_military_comm_effect` VALUES (78, 'EVAL-BATCH-20260325194702753-6A9AB557', '20260005', 0.00, 1.00, 0.00, 0.00, 0.32, 0.00, 0.70, 0.49, 0.00, 0.00, 0.71, 0.60, 0.74, 0.66, 0.05, 0.73, 0.00, 0.21, '2026-03-25 19:47:25', '2026-03-25 19:47:25');
INSERT INTO `score_military_comm_effect` VALUES (79, 'EVAL-BATCH-20260325194702753-6A9AB557', '20260006', 1.00, 1.00, 0.91, 1.00, 0.03, 0.69, 0.47, 0.81, 1.00, 0.94, 0.46, 0.30, 0.64, 0.31, 0.73, 1.00, 1.00, 0.00, '2026-03-25 19:47:25', '2026-03-25 19:47:25');
INSERT INTO `score_military_comm_effect` VALUES (80, 'EVAL-BATCH-20260325194702753-6A9AB557', '20260007', 1.00, 1.00, 0.26, 0.00, 1.00, 1.00, 0.46, 1.00, 0.05, 0.38, 0.10, 0.00, 0.67, 0.00, 0.56, 1.00, 0.50, 0.69, '2026-03-25 19:47:25', '2026-03-25 19:47:25');
INSERT INTO `score_military_comm_effect` VALUES (81, 'EVAL-BATCH-20260325194702753-6A9AB557', '20260008', 0.50, 1.00, 1.00, 1.00, 0.00, 0.81, 0.44, 0.68, 0.30, 0.45, 0.23, 0.10, 1.00, 0.69, 0.00, 0.00, 0.35, 0.67, '2026-03-25 19:47:25', '2026-03-25 19:47:25');
INSERT INTO `score_military_comm_effect` VALUES (82, 'EVAL-BATCH-20260325194702753-6A9AB557', '20260009', 1.00, 1.00, 0.00, 0.00, 0.88, 0.69, 0.33, 0.48, 0.88, 0.97, 0.24, 0.00, 0.64, 0.71, 0.56, 0.44, 1.00, 0.34, '2026-03-25 19:47:25', '2026-03-25 19:47:25');
INSERT INTO `score_military_comm_effect` VALUES (83, 'EVAL-BATCH-20260325194702753-6A9AB557', '20260010', 1.00, 0.36, 0.61, 1.00, 0.25, 0.26, 0.84, 0.00, 0.92, 0.37, 1.00, 1.00, 0.69, 0.78, 0.65, 0.74, 0.41, 0.85, '2026-03-25 19:47:25', '2026-03-25 19:47:25');

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
