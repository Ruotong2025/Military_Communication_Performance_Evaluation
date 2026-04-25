-- ============================================
-- 动态定量评估表结构
-- 用于存储完全动态配置的定量评估数据
-- ============================================

-- 动态评估批次表
CREATE TABLE IF NOT EXISTS dynamic_qt_evaluation_batch (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id VARCHAR(64) NOT NULL UNIQUE COMMENT '批次唯一标识',
    template_id BIGINT NOT NULL COMMENT '使用的指标模板ID',
    template_name VARCHAR(128) COMMENT '模板名称（冗余存储）',
    description VARCHAR(512) COMMENT '批次描述',
    operation_ids TEXT COMMENT '关联的作战ID列表，JSON数组',
    status VARCHAR(32) DEFAULT 'active' COMMENT '状态: active-活跃, normalized-已归一化, archived-归档',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_template_id (template_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态定量评估批次表';

-- 动态定量评估记录表（核心数据表）
CREATE TABLE IF NOT EXISTS dynamic_qt_evaluation_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id VARCHAR(64) NOT NULL COMMENT '批次ID',
    template_id BIGINT NOT NULL COMMENT '指标模板ID',

    -- 作战信息
    operation_id VARCHAR(64) NOT NULL COMMENT '作战ID',

    -- 指标维度信息（完全动态化）
    level_id BIGINT COMMENT '层级ID',
    level_name VARCHAR(128) COMMENT '层级名称',
    primary_dimension_id BIGINT COMMENT '一级维度ID',
    primary_dimension VARCHAR(128) COMMENT '一级维度名称',
    secondary_dimension_id BIGINT COMMENT '二级维度ID',
    secondary_dimension VARCHAR(128) COMMENT '二级维度名称',
    secondary_code VARCHAR(64) COMMENT '二级维度编码',

    -- 指标值
    raw_value DECIMAL(10, 6) COMMENT '原始值 (0~1)',
    normalized_value DECIMAL(10, 6) COMMENT '归一化值 (0~1)',
    is_simulated BOOLEAN DEFAULT FALSE COMMENT '是否为模拟值',

    -- 指标类型信息
    metric_type VARCHAR(32) DEFAULT 'QUANTITATIVE' COMMENT '指标类型',
    unit VARCHAR(32) COMMENT '单位',

    -- 元数据
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 唯一约束：同一批次下，作战+二级维度唯一
    UNIQUE KEY uk_batch_operation_secondary (
        batch_id, operation_id, secondary_code
    ),
    INDEX idx_batch_id (batch_id),
    INDEX idx_template_id (template_id),
    INDEX idx_operation_id (operation_id),
    INDEX idx_primary_dimension (primary_dimension),
    INDEX idx_level_id (level_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态定量评估记录表';

-- 归一化配置表（用于存储归一化参数）
CREATE TABLE IF NOT EXISTS dynamic_qt_normalization_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id VARCHAR(64) NOT NULL COMMENT '批次ID',
    secondary_code VARCHAR(64) NOT NULL COMMENT '二级维度编码',
    min_value DECIMAL(10, 6) COMMENT '该指标在批次内的最小值',
    max_value DECIMAL(10, 6) COMMENT '该指标在批次内的最大值',
    direction VARCHAR(16) DEFAULT 'positive' COMMENT '方向: positive-正向(越大越好), negative-负向(越小越好)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_batch_secondary (batch_id, secondary_code),
    INDEX idx_batch_id (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态定量归一化配置表';
