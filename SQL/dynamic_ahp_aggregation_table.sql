-- 动态AHP集结结果表
-- 用于存储专家对动态指标体系AHP打分的集结结果

CREATE TABLE IF NOT EXISTS `dynamic_ahp_aggregation_result` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `group_id` VARCHAR(16) NOT NULL UNIQUE COMMENT '专家组ID（8位随机码）',
    `template_id` BIGINT NOT NULL COMMENT '指标模板ID',
    `template_name` VARCHAR(200) COMMENT '模板名称',
    `expert_ids` TEXT NOT NULL COMMENT '参与专家ID列表（逗号分隔）',
    `expert_count` INT NOT NULL COMMENT '参与专家人数',
    `level_weights_json` TEXT COMMENT '层级权重JSON',
    `dimension_weights_json` TEXT COMMENT '一级维度权重JSON（按层级分组）',
    `combined_weights_json` TEXT COMMENT '二级指标综合权重JSON',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_template_id` (`template_id`),
    INDEX `idx_group_id` (`group_id`),
    INDEX `idx_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态AHP集结结果表';

-- JSON字段格式说明：
-- level_weights_json: {"战术级": 0.6, "战役级": 0.4}
-- dimension_weights_json: {"战术级": {"通信质量": 0.35, "抗干扰": 0.25}, "战役级": {...}}
-- combined_weights_json: [{"levelName": "战术级", "primaryName": "通信质量", "secondaryName": "时延", "combinedWeight": 0.05}, ...]
