-- ============================================
-- 军事通信性能评估 - 动态指标表结构
-- ============================================

-- 1. 指标模板表
CREATE TABLE IF NOT EXISTS mtl_indicator_template (
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

-- 2. 层级定义表
CREATE TABLE IF NOT EXISTS mtl_level_definition (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id     BIGINT NOT NULL COMMENT '所属模板ID',
    level_name      VARCHAR(200) NOT NULL COMMENT '层级名称',
    level_code      VARCHAR(100) NOT NULL COMMENT '层级编码',
    description     TEXT COMMENT '层级描述',
    sort_order      INT DEFAULT 0 COMMENT '排序顺序',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (template_id) REFERENCES mtl_indicator_template(id) ON DELETE CASCADE,
    UNIQUE KEY uk_template_level_code (template_id, level_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='层级定义表';

-- 3. 一级维度表
CREATE TABLE IF NOT EXISTS mtl_primary_dimension (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    level_id        BIGINT NOT NULL COMMENT '所属层级ID',
    dimension_name  VARCHAR(200) NOT NULL COMMENT '维度名称',
    dimension_code  VARCHAR(100) NOT NULL COMMENT '维度编码',
    sort_order      INT DEFAULT 0 COMMENT '排序顺序',
    weight          DECIMAL(10,6) DEFAULT 0 COMMENT '权重',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (level_id) REFERENCES mtl_level_definition(id) ON DELETE CASCADE,
    UNIQUE KEY uk_level_code (level_id, dimension_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='一级维度表';

-- 4. 二级维度表（关键指标）
CREATE TABLE IF NOT EXISTS mtl_secondary_dimension (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    primary_dimension_id BIGINT NOT NULL COMMENT '所属一级维度ID',
    dimension_name      VARCHAR(200) NOT NULL COMMENT '维度名称',
    dimension_code      VARCHAR(100) NOT NULL COMMENT '维度编码',
    sort_order          INT DEFAULT 0 COMMENT '排序顺序',
    metric_type         ENUM('QUANTITATIVE', 'QUALITATIVE') DEFAULT 'QUANTITATIVE' COMMENT '指标类型：定量/定性',
    aggregation_method  VARCHAR(50) DEFAULT 'avg' COMMENT '聚合方式',
    score_direction     ENUM('POSITIVE', 'NEGATIVE') DEFAULT 'POSITIVE' COMMENT '得分方向',
    unit                VARCHAR(50) COMMENT '单位',
    baseline_value      DECIMAL(20,6) COMMENT '基线值',
    target_value        DECIMAL(20,6) COMMENT '目标值',
    average_value       DECIMAL(20,6) COMMENT '平均数（用于归一化参考）',
    weight              DECIMAL(10,6) DEFAULT 0 COMMENT '权重',
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (primary_dimension_id) REFERENCES mtl_primary_dimension(id) ON DELETE CASCADE,
    UNIQUE KEY uk_primary_code (primary_dimension_id, dimension_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='二级维度表（关键指标）';
