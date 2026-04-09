-- ============================================================
-- 迁移：将原一级维度「操作响应」下的定量指标并入「系统性能」
-- 说明：此前仅「应急处理」挂在「操作响应」，导致装备操作 AHP 中该 Tab 为 1×1 无法两两比较。
-- 执行后请重启后端；专家已保存的 comparison_key 含「装备操作_操作响应_…」的旧数据需重新填写或手工清理。
-- ============================================================

SET NAMES utf8mb4;

UPDATE `equipment_qt_indicator_def`
SET `dimension` = '系统性能',
    `updated_at` = CURRENT_TIMESTAMP
WHERE `indicator_key` = 'eqt_emergency_handling'
  AND `dimension` = '操作响应';

-- 验证
-- SELECT indicator_key, indicator_name, dimension FROM equipment_qt_indicator_def WHERE indicator_key = 'eqt_emergency_handling';
