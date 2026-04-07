-- ============================================================
-- 装备操作评估模块 - 数据库表结构
-- 创建时间: 2026-04-03
-- 说明: 定量指标(10项) + 定性指标(6项) + 进攻/防御操作记录表
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 表1: equipment_qt_indicator_def - 定量指标配置模板
-- ============================================================
DROP TABLE IF EXISTS `equipment_qt_indicator_def`;
CREATE TABLE `equipment_qt_indicator_def` (
  `indicator_key` VARCHAR(50) NOT NULL COMMENT '指标唯一标识',
  `indicator_name` VARCHAR(100) NOT NULL COMMENT '中文名称',
  `indicator_name_en` VARCHAR(100) NULL COMMENT '英文名称',
  `phase` ENUM('pre_war','mid_war','post_war') NOT NULL COMMENT '所属阶段',
  `dimension` VARCHAR(50) NOT NULL COMMENT '维度名称',
  `description` TEXT NULL COMMENT '指标含义说明',
  `source_table` VARCHAR(100) NULL COMMENT '主来源表',
  `source_field` VARCHAR(100) NULL COMMENT '来源字段',
  `aggregation_method` ENUM('direct','avg','sum','count','percentage','custom','avg_conditional') NOT NULL DEFAULT 'direct' COMMENT '聚合方式',
  `custom_sql_template` TEXT NULL COMMENT '自定义SQL模板（占位符 {operation_id}）',
  `unit` VARCHAR(20) NOT NULL COMMENT '单位',
  `score_direction` ENUM('positive','negative') NOT NULL COMMENT '分数方向',
  `display_order` INT NOT NULL DEFAULT 0 COMMENT '界面排序',
  `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `remarks` TEXT NULL,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`indicator_key`),
  INDEX `idx_phase` (`phase`),
  INDEX `idx_dimension` (`dimension`),
  INDEX `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定量指标配置模板';

-- ============================================================
-- 表2: equipment_ql_indicator_def - 定性指标配置模板
-- ============================================================
DROP TABLE IF EXISTS `equipment_ql_indicator_def`;
CREATE TABLE `equipment_ql_indicator_def` (
  `indicator_key` VARCHAR(50) NOT NULL COMMENT '指标唯一标识',
  `indicator_name` VARCHAR(100) NOT NULL COMMENT '中文名称',
  `indicator_name_en` VARCHAR(100) NULL COMMENT '英文名称',
  `phase` ENUM('pre_war','mid_war','post_war') NOT NULL COMMENT '所属阶段',
  `dimension` VARCHAR(50) NOT NULL COMMENT '维度名称',
  `description` TEXT NULL COMMENT '指标含义说明',
  `reference_table` VARCHAR(100) NULL COMMENT '参考数据表（展示给专家的原始数据）',
  `reference_field` VARCHAR(100) NULL COMMENT '参考字段',
  `reference_sql_template` TEXT NULL COMMENT '参考数据SQL模板',
  `evaluation_method` ENUM('grade','likert5','likert10','raw_score') NOT NULL DEFAULT 'grade' COMMENT '评分方式',
  `grade_keys` JSON NULL COMMENT '允许使用的等级key数组',
  `confidence_required` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否必须填把握度',
  `confidence_method` ENUM('percentage','level5') NOT NULL DEFAULT 'percentage' COMMENT '把握度输入方式',
  `allow_comment` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否允许评语',
  `scoring_help` TEXT NULL COMMENT '评分指引',
  `display_order` INT NOT NULL DEFAULT 0 COMMENT '界面排序',
  `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `remarks` TEXT NULL,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`indicator_key`),
  INDEX `idx_phase` (`phase`),
  INDEX `idx_dimension` (`dimension`),
  INDEX `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定性指标配置模板';

-- ============================================================
-- 表3: equipment_qt_evaluation_record - 定量评估记录
-- ============================================================
DROP TABLE IF EXISTS `equipment_qt_evaluation_record`;
CREATE TABLE `equipment_qt_evaluation_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `operation_id` VARCHAR(50) NOT NULL COMMENT '作战任务ID',
  `evaluation_time` DATETIME NOT NULL COMMENT '评估时间',
  `evaluation_batch_id` VARCHAR(50) NOT NULL COMMENT '评估批次ID',
  `raw_data` JSON NOT NULL COMMENT '原始值 {indicator_key: value}',
  `normalized_scores` JSON NOT NULL COMMENT '归一化得分 {indicator_key: score}',
  `composite_score` DECIMAL(10,4) NULL COMMENT '综合得分',
  `dimension_scores` JSON NULL COMMENT '各维度加权得分 {dimension: score}',
  `remarks` TEXT NULL,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_operation` (`evaluation_batch_id`, `operation_id`),
  INDEX `idx_operation` (`operation_id`),
  INDEX `idx_batch` (`evaluation_batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定量评估记录';

-- ============================================================
-- 表4: equipment_ql_evaluation_record - 定性评估记录
-- ============================================================
DROP TABLE IF EXISTS `equipment_ql_evaluation_record`;
CREATE TABLE `equipment_ql_evaluation_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `operation_id` VARCHAR(50) NOT NULL COMMENT '作战任务ID',
  `evaluation_time` DATETIME NOT NULL COMMENT '评估时间',
  `evaluation_batch_id` VARCHAR(50) NOT NULL COMMENT '评估批次ID',
  `expert_id` BIGINT UNSIGNED NOT NULL COMMENT '专家ID',
  `expert_name` VARCHAR(100) NOT NULL COMMENT '专家姓名',
  `scores` JSON NOT NULL COMMENT '{indicator_key: {grade_code, numeric_value, confidence, comment}}',
  `aggregated_grade` VARCHAR(10) NULL COMMENT '集结后等级',
  `aggregated_score` DECIMAL(10,4) NULL COMMENT '集结后数值得分',
  `overall_confidence` DECIMAL(5,2) NULL COMMENT '综合把握度',
  `overall_comment` TEXT NULL COMMENT '总评语',
  `submitted_at` TIMESTAMP NULL COMMENT '提交时间',
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_operation_expert` (`evaluation_batch_id`, `operation_id`, `expert_id`),
  INDEX `idx_operation` (`operation_id`),
  INDEX `idx_expert` (`expert_id`),
  INDEX `idx_batch` (`evaluation_batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定性评估记录';

-- ============================================================
-- 表5: records_comm_attack_operation - 进攻操作记录表
-- ============================================================
DROP TABLE IF EXISTS `records_comm_attack_operation`;
CREATE TABLE `records_comm_attack_operation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `operation_id` INT NOT NULL COMMENT '作战ID',
  `operation_type` ENUM('jamming_target_lock','jamming_effect','spoofing_signal') NOT NULL COMMENT '操作类型',
  `start_time_ms` BIGINT NOT NULL COMMENT '操作开始时间戳',
  `end_time_ms` BIGINT NULL COMMENT '操作结束时间戳',
  `operator_id` VARCHAR(50) NULL COMMENT '操作员ID',
  `target_node` VARCHAR(50) NULL COMMENT '目标节点',
  `target_communication_id` BIGINT NULL COMMENT '关联通信记录ID',
  `jamming_power_dbm` DECIMAL(8,2) NULL COMMENT '干扰功率dBm',
  `jamming_frequency_hz` DECIMAL(15,2) NULL COMMENT '干扰频率Hz',
  `effect_assessment` VARCHAR(200) NULL COMMENT '干扰效能评估文本',
  `spoofing_signal_type` VARCHAR(50) NULL COMMENT '欺骗信号类型',
  `spoofing_success` TINYINT(1) NULL COMMENT '欺骗是否成功',
  `notes` TEXT NULL,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_operation` (`operation_id`),
  INDEX `idx_type` (`operation_type`),
  INDEX `idx_time` (`start_time_ms`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='进攻操作记录表';

-- ============================================================
-- 表6: records_comm_defense_operation - 防御操作记录表
-- ============================================================
DROP TABLE IF EXISTS `records_comm_defense_operation`;
CREATE TABLE `records_comm_defense_operation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `operation_id` INT NOT NULL COMMENT '作战ID',
  `operation_type` ENUM('detection_awareness','anti_jamming','anti_deception') NOT NULL COMMENT '操作类型',
  `start_time_ms` BIGINT NOT NULL COMMENT '操作开始时间戳',
  `end_time_ms` BIGINT NULL COMMENT '操作结束时间戳',
  `operator_id` VARCHAR(50) NULL COMMENT '操作员ID',
  `detection_time_ms` BIGINT NULL COMMENT '感知异常时间ms',
  `detection_method` VARCHAR(100) NULL COMMENT '发现手段',
  `jamming_detected` TINYINT(1) NULL COMMENT '是否检测到干扰',
  `anti_jamming_actions` VARCHAR(200) NULL COMMENT '抗干扰措施',
  `anti_jamming_success` TINYINT(1) NULL COMMENT '抗干扰是否成功',
  `suspicious_signal_detected` TINYINT(1) NULL COMMENT '是否发现可疑信号',
  `verification_method` VARCHAR(100) NULL COMMENT '核实方式',
  `verification_result` VARCHAR(100) NULL COMMENT '核实结果',
  `notes` TEXT NULL,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_operation` (`operation_id`),
  INDEX `idx_type` (`operation_type`),
  INDEX `idx_time` (`start_time_ms`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='防御操作记录表';

-- ============================================================
-- 修改: evaluation_grade_definition - 新增 grade_key 和 numeric_value
-- ============================================================
-- 注意: 如果表已存在且有数据，先备份或确保兼容
-- ALTER TABLE `evaluation_grade_definition`
--   ADD COLUMN `grade_key` VARCHAR(20) NULL COMMENT '配置化key（A_plus/A/A_minus等）' AFTER `grade_level`,
--   ADD COLUMN `numeric_value` DECIMAL(5,2) NULL COMMENT '等级对应数值（用于计算）' AFTER `grade_key`;

SET FOREIGN_KEY_CHECKS = 1;
