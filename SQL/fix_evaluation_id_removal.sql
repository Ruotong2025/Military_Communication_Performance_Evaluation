-- 修复 expert_ahp_comparison_score 表结构（移除 evaluation_id 列）
-- 执行前请确保已备份数据

ALTER TABLE `expert_ahp_comparison_score`
DROP COLUMN IF EXISTS `evaluation_id`;

-- 如果索引存在问题，重新创建
ALTER TABLE `expert_ahp_comparison_score`
DROP INDEX IF EXISTS `idx_evaluation_id`,
DROP INDEX IF EXISTS `idx_expert_evaluation`;

-- 确保 expert_id 索引存在
ALTER TABLE `expert_ahp_comparison_score`
ADD INDEX IF NOT EXISTS `idx_expert_id` (`expert_id`);
