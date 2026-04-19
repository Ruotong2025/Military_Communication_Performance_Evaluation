-- ============================================================
-- 装备操作定性指标集结结果表
-- 存储每次集结计算的最终质心分 x* 和映射等级
-- 创建时间: 2026-04-08
-- 说明: 定性指标集结结果持久化，供综合评分复用
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `equipment_ql_aggregation_result`;
CREATE TABLE `equipment_ql_aggregation_result` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `evaluation_batch_id` VARCHAR(50) NOT NULL COMMENT '评估批次ID',
  `operation_id` VARCHAR(50) NOT NULL COMMENT '作战ID',
  `indicator_key` VARCHAR(50) NOT NULL COMMENT '定性指标唯一标识',
  `indicator_name` VARCHAR(100) NOT NULL COMMENT '指标中文名（冗余存储）',
  `x_star` DECIMAL(10,4) NULL COMMENT '群体结论质心分 x*',
  `mapped_grade` VARCHAR(10) NULL COMMENT '质心映射等级',
  `denominator` DECIMAL(14,6) NULL COMMENT '质心公式分母 Σγ·L（用于溯源验算）',
  `expert_count` INT NOT NULL DEFAULT 0 COMMENT '参与专家数量',
  `expert_ids` JSON NULL COMMENT '参与专家ID数组',
  `weight_snapshot` JSON NULL COMMENT '集结时权重快照 {wAlpha, wLambda}',
  `warnings` JSON NULL COMMENT '集结过程中产生的警告（如把握度全为0的指标）',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_op_indicator` (`evaluation_batch_id`, `operation_id`, `indicator_key`),
  INDEX `idx_batch` (`evaluation_batch_id`),
  INDEX `idx_operation` (`operation_id`),
  INDEX `idx_indicator` (`indicator_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='装备操作定性指标集结结果（质心分）';

-- 验证
-- SELECT * FROM equipment_ql_aggregation_result ORDER BY id DESC LIMIT 5;

SET FOREIGN_KEY_CHECKS = 1;
