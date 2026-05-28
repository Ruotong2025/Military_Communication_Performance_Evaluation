-- ============================================
-- 指标智能识别功能完善 - 数据库脚本
-- 创建时间: 2026-05-26
-- 功能: 支持已有指标关联、数据源关联
-- ============================================

-- ============================================
-- 1. indicator_definition 表新增字段
-- 说明: 如果列已存在会报错，可手动删除后重新执行
-- ============================================

-- related_indicators: 关联的原始输入指标映射关系（JSON格式）
-- 场景: 用户输入"信号干扰比"，匹配到已有指标"信干比"
ALTER TABLE `indicator_definition`
ADD COLUMN `related_indicators` JSON DEFAULT NULL
COMMENT '关联的原始输入指标映射关系';

-- related_source_data: 关联的已有数据源ID列表（JSON格式）
-- 场景: API返回数据源"信号功率"，用户选择数据库已有的同名记录
ALTER TABLE `indicator_definition`
ADD COLUMN `related_source_data` JSON DEFAULT NULL
COMMENT '关联的已有数据源';

-- ============================================
-- 2. 如果上述语句报错说列已存在，执行以下语句删除后重试
-- ============================================
-- ALTER TABLE `indicator_definition` DROP COLUMN IF EXISTS `related_indicators`;
-- ALTER TABLE `indicator_definition` DROP COLUMN IF EXISTS `related_source_data`;
