-- 动态综合评分结果存储表
-- 用于存储每次综合评分计算的结果，便于后续效费分析等场景直接查询

CREATE TABLE IF NOT EXISTS dynamic_comprehensive_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    batch_id VARCHAR(64) NOT NULL COMMENT '批次ID',
    operation_id VARCHAR(64) NOT NULL COMMENT '作战ID',
    template_id BIGINT COMMENT '模板ID',
    template_name VARCHAR(255) COMMENT '模板名称',

    -- 综合得分
    total_score DECIMAL(10, 4) COMMENT '综合得分（总得分）',

    -- 分项得分
    qualitative_weighted_score DECIMAL(10, 4) COMMENT '定性加权分',
    quantitative_weighted_score DECIMAL(10, 4) COMMENT '定量加权分',

    -- 计算时间
    calculated_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '计算时间',

    -- 层级得分JSON（存储详细的层级得分结构）
    level_scores_json TEXT COMMENT '层级得分JSON',

    -- 权重配置JSON（存储综合权重配置）
    weights_config_json TEXT COMMENT '权重配置JSON',

    -- 唯一约束：同一批次+同一作战ID只能有一条记录
    UNIQUE KEY uk_batch_operation (batch_id, operation_id),

    -- 索引
    INDEX idx_batch_id (batch_id),
    INDEX idx_operation_id (operation_id),
    INDEX idx_template_id (template_id),
    INDEX idx_calculated_at (calculated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态综合评分结果表';
