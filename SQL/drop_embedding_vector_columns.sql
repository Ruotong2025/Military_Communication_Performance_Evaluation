-- ============================================================
-- 删除 MySQL embedding_vector 字段的脚本
-- 迁移到 Chroma 后执行此脚本
-- 执行前请确保：
-- 1. Chroma 数据迁移已完成
-- 2. 应用已使用新的 Chroma 服务正常运行
-- ============================================================

-- 警告：此操作不可逆，请先备份数据库！

-- 1. 先备份向量数据（可选但强烈建议）
-- CREATE TABLE indicator_definition_backup_vectors AS
-- SELECT id, indicator_name, embedding_vector
-- FROM indicator_definition
-- WHERE embedding_vector IS NOT NULL;

-- CREATE TABLE indicator_source_data_backup_vectors AS
-- SELECT id, source_data_name, embedding_vector
-- FROM indicator_source_data
-- WHERE embedding_vector IS NOT NULL;

-- 2. 删除 indicator_definition 表的 embedding_vector 字段
ALTER TABLE indicator_definition DROP COLUMN embedding_vector;

-- 3. 删除 indicator_source_data 表的 embedding_vector 字段
ALTER TABLE indicator_source_data DROP COLUMN embedding_vector;

-- 4. 验证删除结果
-- SELECT column_name FROM information_schema.columns
-- WHERE table_schema = 'military_operational_effectiveness_evaluation'
-- AND table_name = 'indicator_definition'
-- AND column_name = 'embedding_vector';

-- SELECT column_name FROM information_schema.columns
-- WHERE table_schema = 'military_operational_effectiveness_evaluation'
-- AND table_name = 'indicator_source_data'
-- AND column_name = 'embedding_vector';
