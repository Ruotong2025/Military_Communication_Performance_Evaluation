-- ============================================
-- 动态定量评估表结构修复脚本
-- 移除 dynamic_qt_record 表中的 average_value 字段
-- ============================================

-- 如果 average_value 字段存在，则删除它
ALTER TABLE dynamic_qt_record DROP COLUMN IF EXISTS average_value;
