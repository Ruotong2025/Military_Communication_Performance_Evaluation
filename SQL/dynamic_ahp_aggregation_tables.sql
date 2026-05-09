-- ============================================
-- 动态AHP集结结果表
-- 用于存储动态指标体系的AHP专家集结计算结果
-- ============================================

CREATE TABLE IF NOT EXISTS `dynamic_ahp_aggregation_result` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `group_id` VARCHAR(16) NOT NULL UNIQUE COMMENT '专家组ID（8位随机码）',
    `template_id` BIGINT NOT NULL COMMENT '指标模板ID',
    `template_name` VARCHAR(200) COMMENT '模板名称',
    `expert_ids` TEXT NOT NULL COMMENT '参与专家ID列表，逗号分隔',
    `expert_count` INT NOT NULL COMMENT '专家人数',
    `expert_names` TEXT COMMENT '参与专家名称列表',

    -- 层级权重（JSON存储）
    `level_weights_json` TEXT COMMENT '层级权重 JSON，格式: {"战术级": 0.6, "战役级": 0.4}',

    -- 维度权重（JSON存储，按层级分组）
    `dimension_weights_json` TEXT COMMENT '一级维度权重 JSON，格式: {"战术级": {"通信质量": 0.35, "抗干扰": 0.25}}',

    -- 综合权重（JSON存储）
    `combined_weights_json` TEXT COMMENT '二级指标综合权重 JSON，格式: {"战术级|通信质量|时延": 0.05}',

    -- 集体比较打分（JSON存储，用于展示矩阵）
    `collective_scores_json` TEXT COMMENT '集体比较打分 JSON',

    -- 一致性检验结果（JSON存储）
    `cr_results_json` TEXT COMMENT '各矩阵CR值 JSON，格式: {"LEVEL_BETWEEN": 0.023, "战术级:PRIMARY": 0.015}',

    -- 专家权重明细（JSON存储）
    `expert_weights_json` TEXT COMMENT '专家权重明细 JSON',

    -- 维度下的指标结构（JSON存储，用于前端动态渲染）
    `dimension_indicators_json` TEXT COMMENT '维度指标结构 JSON',

    -- 元数据
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX `idx_aggregation_template_id` (`template_id`),
    INDEX `idx_aggregation_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态AHP集结结果表';
