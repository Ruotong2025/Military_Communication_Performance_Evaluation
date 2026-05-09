-- ============================================
-- 动态定性评估表结构（简化版 - 2个表）
-- ============================================

-- ============================================
-- 表1: dynamic_ql_record - 评估记录表
-- 存储：专家对各作战在各定性指标上的评分
-- ============================================
CREATE TABLE IF NOT EXISTS dynamic_ql_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 批次与模板
    batch_id VARCHAR(64) NOT NULL COMMENT '评估批次ID',
    template_id BIGINT COMMENT '模板ID',

    -- 专家信息
    expert_id BIGINT NOT NULL COMMENT '专家ID',
    expert_name VARCHAR(100) COMMENT '专家姓名（冗余）',
    expert_credibility DECIMAL(5,2) COMMENT '专家可信度（冗余）',

    -- 作战信息
    operation_id VARCHAR(64) NOT NULL COMMENT '作战ID',

    -- 指标信息
    secondary_code VARCHAR(64) NOT NULL COMMENT '定性指标编码',
    secondary_name VARCHAR(128) COMMENT '定性指标名称',
    level_name VARCHAR(128) COMMENT '层级名称',
    primary_dimension VARCHAR(128) COMMENT '一级维度',

    -- 评分数据（15级评分）
    score_level VARCHAR(8) NOT NULL COMMENT '评分等级: A+,A,A-,B+,B,B-,C+,C,C-,D+,D,D-,E+,E,E-',
    score_value DECIMAL(10,6) COMMENT '评分对应数值',

    -- 把握度（0-100）
    confidence_value DECIMAL(5,2) COMMENT '把握度（0-100）',

    -- 备注与时间
    evaluation_remark TEXT COMMENT '评估备注',
    evaluated_at DATETIME COMMENT '评估时间',

    -- 时间戳
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 唯一约束
    UNIQUE KEY uk_expert_batch_op_indicator (batch_id, expert_id, operation_id, secondary_code),
    INDEX idx_batch_id (batch_id),
    INDEX idx_expert_id (expert_id),
    INDEX idx_operation_id (operation_id),
    INDEX idx_level_name (level_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定性评估记录表';

-- ============================================
-- 表2: dynamic_ql_aggregation - 集结结果表
-- 存储：多位专家评分的集结计算结果
-- ============================================
CREATE TABLE IF NOT EXISTS dynamic_ql_aggregation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    batch_id VARCHAR(64) NOT NULL COMMENT '批次ID',
    template_id BIGINT COMMENT '模板ID',
    operation_id VARCHAR(64) NOT NULL COMMENT '作战ID',
    secondary_code VARCHAR(64) NOT NULL COMMENT '定性指标编码',
    secondary_name VARCHAR(128) COMMENT '指标名称',

    -- 集结结果
    aggregated_value DECIMAL(10,6) COMMENT '集结后的评分值',
    aggregation_method VARCHAR(32) DEFAULT 'WEIGHTED_CENTROID' COMMENT '集结方法',

    -- 参与专家信息（JSON）
    participating_experts JSON COMMENT '参与专家: [{expertId, name, credibility, score, confidence}]',
    expert_count INT COMMENT '专家数量',

    -- 权重参数
    weight_alpha DECIMAL(5,4) DEFAULT 0.5 COMMENT '权威度权重',
    weight_lambda DECIMAL(5,4) DEFAULT 0.5 COMMENT '把握度权重',

    -- 统计信息
    min_score DECIMAL(10,6) COMMENT '最低分',
    max_score DECIMAL(10,6) COMMENT '最高分',
    avg_score DECIMAL(10,6) COMMENT '平均分',
    std_dev DECIMAL(10,6) COMMENT '标准差',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_batch_op_indicator (batch_id, operation_id, secondary_code),
    INDEX idx_batch_id (batch_id),
    INDEX idx_operation_id (operation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定性评估集结结果表';

-- ============================================
-- 清理旧表（如果存在且不再需要）
-- ============================================
-- DROP TABLE IF EXISTS dynamic_ql_session;
-- DROP TABLE IF EXISTS dynamic_ql_reference;
