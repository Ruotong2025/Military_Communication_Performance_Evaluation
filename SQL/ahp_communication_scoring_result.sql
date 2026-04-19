-- ============================================================
-- AHP通信效能综合评分结果表
-- 存储每次综合评分计算的结果
-- 创建时间: 2026-04-13
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 删除旧表（如存在）
DROP TABLE IF EXISTS `equipment_comprehensive_scoring_result`;

-- 创建新表
DROP TABLE IF EXISTS `ahp_communication_scoring_result`;
CREATE TABLE `ahp_communication_scoring_result` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `evaluation_batch_id` VARCHAR(50) NOT NULL COMMENT '评估批次ID',
  `operation_id` VARCHAR(50) NOT NULL COMMENT '作战ID',

  -- 域间权重快照
  `eff_domain_weight` DECIMAL(10,6) DEFAULT NULL COMMENT '效能体系全局权重',
  `eq_domain_weight` DECIMAL(10,6) DEFAULT NULL COMMENT '装备体系全局权重',

  -- 效能综合分（0~1）
  `effectiveness_score` DECIMAL(10,6) DEFAULT NULL COMMENT '效能综合分',
  -- 装备综合分（0~1）
  `equipment_score` DECIMAL(10,6) DEFAULT NULL COMMENT '装备综合分',
  -- 综合总分
  `total_score` DECIMAL(10,6) DEFAULT NULL COMMENT '综合总分',

  -- 各维度得分明细 JSON
  `dimension_scores_json` LONGTEXT CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '维度得分 JSON {dimName: score}',
  -- 指标得分明细 JSON
  `indicator_scores_json` LONGTEXT CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '指标得分 JSON',
  -- 参与专家信息 JSON
  `expert_info_json` LONGTEXT CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '专家信息 JSON',
  -- 权重快照 JSON
  `weight_snapshot_json` LONGTEXT CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '权重快照 JSON',

  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_operation` (`evaluation_batch_id`, `operation_id`),
  INDEX `idx_batch` (`evaluation_batch_id`),
  INDEX `idx_operation` (`operation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AHP通信效能综合评分结果';

SET FOREIGN_KEY_CHECKS = 1;