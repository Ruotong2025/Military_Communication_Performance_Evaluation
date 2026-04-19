-- ============================================================
-- SQL 迁移脚本：score_military_comm_effect 表字段改名
-- evaluation_id -> evaluation_batch_id
-- 创建时间: 2026-04-19
-- ============================================================

SET NAMES utf8mb4;

-- 添加新字段 evaluation_batch_id（如果不存在）
ALTER TABLE `score_military_comm_effect`
  ADD COLUMN IF NOT EXISTS `evaluation_batch_id` VARCHAR(50) DEFAULT NULL AFTER `id`;

-- 复制数据（如果 evaluation_batch_id 为空且 evaluation_id 有值）
UPDATE `score_military_comm_effect`
SET `evaluation_batch_id` = `evaluation_id`
WHERE `evaluation_batch_id` IS NULL AND `evaluation_id` IS NOT NULL;

-- 删除旧的 evaluation_id 字段（可选，先保留以防回滚）
-- ALTER TABLE `score_military_comm_effect` DROP COLUMN `evaluation_id`;

-- 添加索引以优化查询性能
ALTER TABLE `score_military_comm_effect`
  ADD INDEX IF NOT EXISTS `idx_evaluation_batch` (`evaluation_batch_id`);

-- 验证
-- SELECT id, evaluation_batch_id, evaluation_id, operation_id FROM score_military_comm_effect LIMIT 5;
