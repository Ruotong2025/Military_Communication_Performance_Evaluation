-- ============================================
-- 动态AHP表结构
-- 用于存储动态指标体系的AHP层次分析法配置
-- ============================================

-- 1. 动态AHP判断矩阵表
-- 存储专家对动态指标体系的AHP判断矩阵打分
CREATE TABLE IF NOT EXISTS `dynamic_ahp_matrix` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `expert_id` BIGINT NOT NULL COMMENT '专家ID',
    `expert_name` VARCHAR(100) COMMENT '专家名称',
    `template_id` BIGINT NOT NULL COMMENT '指标模板ID',
    `template_name` VARCHAR(200) COMMENT '模板名称',
    `level_name` VARCHAR(100) COMMENT '层级名称（如"战术级"、"战役级"）',
    `comparison_type` VARCHAR(50) NOT NULL COMMENT '比较类型：LEVEL_BETWEEN（一级维度间）、PRIMARY_BETWEEN（一级维度间）、SECONDARY_BETWEEN（二级指标间）',
    `parent_code` VARCHAR(100) COMMENT '父级维度编码（二级矩阵时使用）',
    `dimension_codes` VARCHAR(500) COMMENT '比较的维度编码',
    `dimension_names` VARCHAR(1000) COMMENT '比较的维度名称',
    `dimension_count` INT COMMENT '维度数量',
    `matrix_values` TEXT COMMENT '完整矩阵值（JSON格式）',
    `score` DECIMAL(10, 4) COMMENT '判断标度值',
    `confidence` DECIMAL(5, 4) COMMENT '判断可信度（把握度）0-1',
    `row_code` VARCHAR(100) COMMENT '行维度编码',
    `row_name` VARCHAR(200) COMMENT '行维度名称',
    `col_code` VARCHAR(100) COMMENT '列维度编码',
    `col_name` VARCHAR(200) COMMENT '列维度名称',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX `idx_ahp_expert_template` (`expert_id`, `template_id`),
    INDEX `idx_ahp_comparison_key` (`expert_id`, `template_id`, `comparison_type`, `row_code`, `col_code`),
    INDEX `idx_ahp_template_level` (`template_id`, `level_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态AHP判断矩阵表';


-- 2. 动态AHP权重结果表
-- 存储专家对动态指标体系的AHP层次分析权重计算结果
CREATE TABLE IF NOT EXISTS `dynamic_ahp_weights` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `expert_id` BIGINT NOT NULL COMMENT '专家ID',
    `expert_name` VARCHAR(100) COMMENT '专家名称',
    `template_id` BIGINT NOT NULL COMMENT '指标模板ID',
    `template_name` VARCHAR(200) COMMENT '模板名称',
    `level_name` VARCHAR(100) COMMENT '层级名称',
    `dimension_type` VARCHAR(50) COMMENT '维度类型：LEVEL、PRIMARY、SECONDARY',
    `dimension_code` VARCHAR(100) COMMENT '维度编码',
    `dimension_name` VARCHAR(200) COMMENT '维度名称',
    `dimension_description` VARCHAR(500) COMMENT '维度描述',
    `weight` DECIMAL(10, 6) COMMENT '权重值',
    `sort_order` INT COMMENT '排序',
    `lambda_max` DECIMAL(10, 6) COMMENT '最大特征值',
    `ci` DECIMAL(10, 6) COMMENT '一致性指标CI',
    `ri` DECIMAL(10, 4) COMMENT '随机一致性指标RI',
    `cr` DECIMAL(10, 6) COMMENT '一致性比率CR',
    `is_consistent` TINYINT(1) COMMENT '是否通过一致性检验',
    `combined_weight` DECIMAL(10, 6) COMMENT '综合权重（跨层级累积）',
    `parent_code` VARCHAR(100) COMMENT '父级维度编码',
    `weight_path` VARCHAR(500) COMMENT '路径（如：战术级/通信质量/时延）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX `idx_weights_expert_template` (`expert_id`, `template_id`),
    INDEX `idx_weights_level` (`template_id`, `level_name`),
    INDEX `idx_weights_dimension` (`expert_id`, `template_id`, `level_name`, `dimension_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态AHP权重结果表';


-- ============================================
-- 初始化数据说明
-- ============================================
-- 
-- 1. 需要先在 dynamic_template 表中有指标模板数据
-- 2. 需要先在 expert_credibility/expert 表中有专家数据
-- 3. 然后才能进行AHP权重配置
--
-- 使用示例：
-- 
-- -- 查询已存在的模板
-- SELECT * FROM dynamic_template;
--
-- -- 查询已存在的专家
-- SELECT * FROM expert;
--
-- -- 查询某个模板的层级结构
-- SELECT * FROM dynamic_dimension WHERE template_id = 1 AND dimension_level = 'LEVEL';
-- SELECT * FROM dynamic_dimension WHERE template_id = 1 AND dimension_level = 'PRIMARY';
-- SELECT * FROM dynamic_dimension WHERE template_id = 1 AND dimension_level = 'SECONDARY';
