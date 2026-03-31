-- 修复 expert_ahp_individual_weights 表结构（移除 evaluation_id 列）
-- 执行前请确保已备份数据

ALTER TABLE `expert_ahp_individual_weights`
DROP COLUMN IF EXISTS `evaluation_id`;

-- 重新创建唯一约束（仅 expert_id）
ALTER TABLE `expert_ahp_individual_weights`
DROP INDEX IF EXISTS `uk_expert_evaluation`,
ADD UNIQUE INDEX IF NOT EXISTS `uk_expert_id` (`expert_id`);
