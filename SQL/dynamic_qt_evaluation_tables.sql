-- ============================================
-- 动态定量评估表结构（简化版）
-- 原始数据 + 归一化结果 统一存储在一张表
-- 每次归一化用不同的 normalization_name 标识
-- ============================================

-- 动态定量评估记录表（核心数据表）
-- 原始数据: normalization_name = NULL
-- 归一化结果: normalization_name = 'xxx'
CREATE TABLE IF NOT EXISTS dynamic_qt_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 批次标识
    batch_id VARCHAR(64) NOT NULL COMMENT '评估批次ID',
    template_id BIGINT COMMENT '模板ID',
    template_name VARCHAR(128) COMMENT '模板名称',

    -- 作战和指标
    operation_id VARCHAR(64) NOT NULL COMMENT '作战ID',
    secondary_code VARCHAR(64) NOT NULL COMMENT '二级维度编码',
    secondary_name VARCHAR(128) COMMENT '指标名称',
    level_name VARCHAR(128) COMMENT '层级名称',
    primary_dimension VARCHAR(128) COMMENT '一级维度名称',

    -- 原始数据
    raw_value DECIMAL(10, 6) COMMENT '原始值',
    is_simulated BOOLEAN DEFAULT FALSE COMMENT '是否为模拟值',
    metric_type VARCHAR(32) DEFAULT 'QUANTITATIVE' COMMENT '指标类型: QUANTITATIVE/QUALITATIVE',
    qualitative_level VARCHAR(32) COMMENT '定性等级: EXCELLENT/GOOD/FAIR/POOR',
    direction VARCHAR(16) DEFAULT 'POSITIVE' COMMENT '方向: POSITIVE/NEGATIVE',

    -- 归一化结果
    normalization_name VARCHAR(128) COMMENT '归一化名称（如：第1次归一化）',
    normalized_value DECIMAL(10, 6) COMMENT '归一化值',

    -- 元数据
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 唯一约束: 同一批次+作战+指标+归一化名称唯一
    UNIQUE KEY uk_key (batch_id, operation_id, secondary_code, normalization_name),
    INDEX idx_batch_id (batch_id),
    INDEX idx_operation_id (operation_id),
    INDEX idx_normalization_name (normalization_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态定量评估记录表（原始+归一化）';

-- ============================================
-- 如果旧表存在，先删除
-- ============================================
DROP TABLE IF EXISTS dynamic_qt_evaluation_batch;
DROP TABLE IF EXISTS dynamic_qt_evaluation_record;
DROP TABLE IF EXISTS dynamic_qt_normalization_batch;
DROP TABLE IF EXISTS dynamic_qt_normalization_record;
DROP TABLE IF EXISTS dynamic_qt_normalization_config;
