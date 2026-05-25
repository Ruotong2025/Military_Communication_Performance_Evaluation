-- ============================================
-- 指标智能识别模块 - 数据库脚本
-- 创建时间: 2026-05-15
-- 更新: 2026-05-24 (添加 FAISS 向量索引支持，为 indicator_source_data 添加 embedding_vector 字段)
-- ============================================

-- ============================================
-- 更新现有数据库表结构
-- ============================================
-- 1. 删除旧的唯一约束（indicator_name + category）
-- 2. 添加新的全局唯一约束（indicator_name）
-- 3. 添加 embedding_vector 字段

ALTER TABLE `indicator_definition` DROP INDEX IF EXISTS `uk_indicator_name`;
ALTER TABLE `indicator_definition` ADD CONSTRAINT `uk_indicator_name` UNIQUE (`indicator_name`);
ALTER TABLE `indicator_definition` ADD COLUMN IF NOT EXISTS `embedding_vector` TEXT DEFAULT NULL COMMENT '向量嵌入(JSON格式)';

-- 为 indicator_source_data 添加向量字段
ALTER TABLE `indicator_source_data` ADD COLUMN IF NOT EXISTS `embedding_vector` TEXT DEFAULT NULL COMMENT '向量嵌入(JSON格式)';

-- ============================================
-- 指标定义表
-- ============================================
CREATE TABLE IF NOT EXISTS `indicator_definition` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `indicator_name` VARCHAR(200) NOT NULL COMMENT '指标名称',
    `indicator_name_en` VARCHAR(200) DEFAULT NULL COMMENT '指标英文名称',
    `indicator_type` VARCHAR(20) NOT NULL COMMENT '指标类型：QUALITATIVE-定性，QUANTITATIVE-定量',
    `category` VARCHAR(100) DEFAULT NULL COMMENT '指标大类',
    `unit` VARCHAR(50) DEFAULT NULL COMMENT '计量单位',
    `description` TEXT DEFAULT NULL COMMENT '指标说明',

    -- 计算公式相关字段
    `formula` TEXT DEFAULT NULL COMMENT '计算公式（如：SINR = Ps / (Pn + Pi)）',
    `formula_description` TEXT DEFAULT NULL COMMENT '公式说明（如：信干噪比 = 信号功率 / (噪声功率 + 干扰功率)）',
    `calculation_method` VARCHAR(500) DEFAULT NULL COMMENT '计算方法简述',

    `source_data_hint` TEXT DEFAULT NULL COMMENT '可测得的基础数据提示',

    -- 向量嵌入（用于语义相似度计算）
    `embedding_vector` TEXT DEFAULT NULL COMMENT '向量嵌入(JSON格式)',

    `is_active` BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    `is_from_ai` BOOLEAN DEFAULT FALSE COMMENT '是否由AI自动识别生成',
    `ai_confidence` DECIMAL(5,2) DEFAULT NULL COMMENT 'AI识别置信度',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY `uk_indicator_name` (`indicator_name`),
    INDEX `idx_category` (`category`),
    INDEX `idx_indicator_type` (`indicator_type`),
    INDEX `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标定义表';

-- ============================================
-- 指标可测得数据关联表
-- ============================================
CREATE TABLE IF NOT EXISTS `indicator_source_data` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `indicator_id` BIGINT NOT NULL COMMENT '指标ID',
    `source_data_name` VARCHAR(200) NOT NULL COMMENT '可测得的基础数据名称',
    `source_data_code` VARCHAR(100) DEFAULT NULL COMMENT '基础数据编码',
    `measurement_method` VARCHAR(500) DEFAULT NULL COMMENT '测量方法说明',
    `data_type` VARCHAR(50) DEFAULT NULL COMMENT '数据类型：NUMERIC-数值，PERCENTAGE-百分比',
    `unit` VARCHAR(50) DEFAULT NULL COMMENT '单位',
    `priority` INT DEFAULT 0 COMMENT '优先级（1-主要，2-次要）',
    `confidence` DECIMAL(5,2) DEFAULT NULL COMMENT '可信度',

    -- 公式关联字段
    `is_formula_related` BOOLEAN DEFAULT FALSE COMMENT '是否与计算公式相关',
    `formula_symbol` VARCHAR(50) DEFAULT NULL COMMENT '公式中的符号（如：Ps, Pn）',
    `is_essential` BOOLEAN DEFAULT TRUE COMMENT '是否为必要数据（false=可选）',

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    FOREIGN KEY (`indicator_id`) REFERENCES `indicator_definition`(`id`) ON DELETE CASCADE,
    INDEX `idx_indicator_id` (`indicator_id`),
    INDEX `idx_formula_related` (`is_formula_related`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标可测得数据关联表';

-- ============================================
-- AI识别历史记录表
-- ============================================
CREATE TABLE IF NOT EXISTS `indicator_ai_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `indicator_names` TEXT NOT NULL COMMENT '本次识别的指标名称列表（JSON格式）',
    `request_payload` TEXT COMMENT '请求载荷',
    `response_payload` TEXT COMMENT '响应内容',
    `identified_results` TEXT COMMENT '识别结果（JSON格式）',
    `is_success` BOOLEAN DEFAULT TRUE COMMENT '是否成功',
    `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
    `processing_time_ms` INT DEFAULT NULL COMMENT '处理耗时（毫秒）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX `idx_is_success` (`is_success`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI识别历史记录表';
