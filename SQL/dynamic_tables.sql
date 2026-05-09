-- ============================================
-- 军事通信性能评估 - 动态指标表结构（2表版）
-- ============================================

-- 1. 指标模板表
CREATE TABLE IF NOT EXISTS dynamic_template (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_name   VARCHAR(200) NOT NULL COMMENT '模板名称',
    template_code   VARCHAR(50) NOT NULL UNIQUE COMMENT '模板编码',
    description     TEXT COMMENT '模板描述',
    level_count     INT DEFAULT 0 COMMENT '层级数量',
    primary_count   INT DEFAULT 0 COMMENT '一级维度数量',
    secondary_count INT DEFAULT 0 COMMENT '二级维度数量',
    status          ENUM('DRAFT', 'ACTIVE', 'ARCHIVED') DEFAULT 'DRAFT' COMMENT '状态',
    source_file     VARCHAR(500) COMMENT '来源文件',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标模板表';

-- 2. 维度表（合并所有层级）
CREATE TABLE IF NOT EXISTS dynamic_dimension (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id         BIGINT NOT NULL COMMENT '所属模板ID',
    dimension_level     ENUM('LEVEL', 'PRIMARY', 'SECONDARY') NOT NULL COMMENT '维度层级',
    parent_id           BIGINT DEFAULT NULL COMMENT '父节点ID（用于树形结构）',
    name                VARCHAR(200) NOT NULL COMMENT '维度名称',
    code                VARCHAR(100) NOT NULL COMMENT '维度编码',
    description         TEXT COMMENT '维度描述',
    sort_order          INT DEFAULT 0 COMMENT '排序顺序',
    weight              DECIMAL(10,6) DEFAULT 0 COMMENT '权重',
    metric_type         ENUM('QUANTITATIVE', 'QUALITATIVE') DEFAULT NULL COMMENT '指标类型：定量/定性',
    aggregation_method  VARCHAR(50) DEFAULT NULL COMMENT '聚合方式',
    score_direction     ENUM('POSITIVE', 'NEGATIVE') DEFAULT NULL COMMENT '得分方向',
    unit                VARCHAR(50) DEFAULT NULL COMMENT '单位',
    baseline_value      DECIMAL(20,6) DEFAULT NULL COMMENT '基线值',
    target_value        DECIMAL(20,6) DEFAULT NULL COMMENT '目标值',
    average_value       DECIMAL(20,6) DEFAULT NULL COMMENT '平均数（用于归一化参考）',
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (template_id) REFERENCES dynamic_template(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES dynamic_dimension(id) ON DELETE CASCADE,
    UNIQUE KEY uk_template_code (template_id, dimension_level, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='维度表（合并所有层级）';
