-- ============================================
-- 动态定性评估表结构
-- 存储：专家对各作战在各定性指标上的评分
-- 完全动态化：指标结构来自 dynamic_template + dynamic_dimension
-- ============================================

-- ============================================
-- 动态定性评估记录表
-- ============================================
CREATE TABLE IF NOT EXISTS dynamic_ql_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 批次与模板（与定量评估共用）
    batch_id VARCHAR(64) NOT NULL COMMENT '评估批次ID',
    template_id BIGINT COMMENT '模板ID',
    template_name VARCHAR(128) COMMENT '模板名称',

    -- 专家信息（关联 expert_base_info）
    expert_id BIGINT NOT NULL COMMENT '专家ID',
    expert_name VARCHAR(100) COMMENT '专家姓名（冗余存储）',
    expert_credibility DECIMAL(5,2) COMMENT '专家可信度评分（0-100，冗余存储）',

    -- 作战信息（来自定量评估批次）
    operation_id VARCHAR(64) NOT NULL COMMENT '作战ID',

    -- 指标信息（动态结构，来自 dynamic_dimension）
    secondary_code VARCHAR(64) NOT NULL COMMENT '二级维度编码（定性指标）',
    secondary_name VARCHAR(128) COMMENT '定性指标名称',
    level_name VARCHAR(128) COMMENT '层级名称',
    primary_dimension VARCHAR(128) COMMENT '一级维度名称',

    -- 评分数据
    score_level VARCHAR(32) NOT NULL COMMENT '评分等级: EXCELLENT/GOOD/FAIR/POOR',
    score_value DECIMAL(10,6) COMMENT '评分对应数值: 优秀=1.0, 良好=0.75, 一般=0.5, 差=0.25',
    confidence_level VARCHAR(32) COMMENT '把握度: HIGH/MEDIUM/LOW',
    confidence_value DECIMAL(5,2) COMMENT '把握度数值（0-100）',

    -- 评估元数据
    evaluation_remark TEXT COMMENT '评估备注/说明',
    evaluated_at DATETIME COMMENT '评估时间',

    -- 时间戳
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 唯一约束：同一专家对同一作战的同一指标只能有一条记录
    UNIQUE KEY uk_expert_batch_op_indicator (batch_id, expert_id, operation_id, secondary_code),
    INDEX idx_batch_id (batch_id),
    INDEX idx_expert_id (expert_id),
    INDEX idx_operation_id (operation_id),
    INDEX idx_template_id (template_id),
    INDEX idx_level_name (level_name),
    INDEX idx_score_level (score_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态定性评估记录表';

-- ============================================
-- 动态定性评估会话表（用于管理评估会话）
-- ============================================
CREATE TABLE IF NOT EXISTS dynamic_ql_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    session_id VARCHAR(64) NOT NULL UNIQUE COMMENT '评估会话ID',

    -- 关联的定量批次（可为空）
    batch_id VARCHAR(64) COMMENT '关联的定量批次ID',
    template_id BIGINT COMMENT '模板ID',
    template_name VARCHAR(128) COMMENT '模板名称',

    -- 会话状态
    status VARCHAR(32) DEFAULT 'DRAFT' COMMENT '状态: DRAFT/IN_PROGRESS/COMPLETED',

    -- 已选专家列表（JSON数组）
    selected_experts JSON COMMENT '已选专家列表: [{expertId, expertName, credibility}]',

    -- 评估配置
    evaluation_mode VARCHAR(32) DEFAULT 'SINGLE_EXPERT' COMMENT '评估模式: SINGLE_EXPERT/MULTI_EXPERT',
    show_reference_data BOOLEAN DEFAULT TRUE COMMENT '是否显示参考数据',

    -- 进度跟踪
    total_count INT DEFAULT 0 COMMENT '总评估项数',
    completed_count INT DEFAULT 0 COMMENT '已完成项数',

    -- 元数据
    description TEXT COMMENT '评估说明',
    created_by VARCHAR(100) COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_batch_id (batch_id),
    INDEX idx_template_id (template_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态定性评估会话表';

-- ============================================
-- 动态定性评估集结结果表
-- ============================================
CREATE TABLE IF NOT EXISTS dynamic_ql_aggregation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    batch_id VARCHAR(64) NOT NULL COMMENT '评估批次ID',
    template_id BIGINT COMMENT '模板ID',
    operation_id VARCHAR(64) NOT NULL COMMENT '作战ID',
    secondary_code VARCHAR(64) NOT NULL COMMENT '定性指标编码',
    secondary_name VARCHAR(128) COMMENT '定性指标名称',

    -- 集结结果
    aggregated_value DECIMAL(10,6) COMMENT '集结后的评分值',
    aggregation_method VARCHAR(32) DEFAULT 'WEIGHTED_CENTROID' COMMENT '集结方法: WEIGHTED_CENTROID/WEIGHTED_AVG/MEAN',

    -- 参与集结的专家信息
    participating_experts JSON COMMENT '参与专家列表: [{expertId, expertName, credibility, scoreValue, confidenceValue}]',
    expert_count INT COMMENT '参与专家数量',

    -- 集结权重参数
    weight_alpha DECIMAL(5,4) DEFAULT 0.5 COMMENT '权威度权重α',
    weight_lambda DECIMAL(5,4) DEFAULT 0.5 COMMENT '把握度权重λ',

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
-- 动态定性评估参考数据表（存储作战的定量参考数据快照）
-- ============================================
CREATE TABLE IF NOT EXISTS dynamic_ql_reference (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    batch_id VARCHAR(64) NOT NULL COMMENT '批次ID',
    operation_id VARCHAR(64) NOT NULL COMMENT '作战ID',
    template_id BIGINT COMMENT '模板ID',

    -- 参考数据类型
    reference_type VARCHAR(32) DEFAULT 'QUANTITATIVE' COMMENT '参考数据类型: QUANTITATIVE/EXTERNAL',

    -- 参考数据（JSON格式，动态结构）
    reference_data JSON COMMENT '参考数据: {指标code: {value, unit, normalized}}',

    -- 统计汇总
    overall_score DECIMAL(10,6) COMMENT '整体参考评分',
    rank_position INT COMMENT '排名位置',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_batch_operation (batch_id, operation_id),
    INDEX idx_batch_id (batch_id),
    INDEX idx_operation_id (operation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定性评估参考数据表';
