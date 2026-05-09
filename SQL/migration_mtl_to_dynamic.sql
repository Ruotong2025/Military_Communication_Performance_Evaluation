-- ============================================
-- MTL表合并迁移脚本
-- 5表合并为2表: dynamic_template + dynamic_dimension
-- ============================================

-- 1. 重命名原表（如果需要保留历史数据）
-- 如果是新环境，可以直接创建新表，跳过此步骤

-- ALTER TABLE mtl_indicator_template RENAME TO dynamic_template;
-- ALTER TABLE mtl_level_definition RENAME TO dynamic_level;
-- ALTER TABLE mtl_primary_dimension RENAME TO dynamic_primary;
-- ALTER TABLE mtl_secondary_dimension RENAME TO dynamic_secondary;

-- 2. 为原表添加average_value字段
-- 只需要修改SecondaryDimension表，添加平均数字段

ALTER TABLE mtl_secondary_dimension
ADD COLUMN IF NOT EXISTS average_value DECIMAL(20, 6) DEFAULT NULL COMMENT '平均数（用于归一化参考）';

-- 3. 验证修改
-- DESC mtl_secondary_dimension;

-- ============================================
-- 如果需要完全重建表结构（全新安装）
-- ============================================

-- 指标模板表
CREATE TABLE IF NOT EXISTS dynamic_template (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_name   VARCHAR(200) NOT NULL,
    template_code   VARCHAR(50) NOT NULL UNIQUE,
    description     TEXT,
    level_count     INT DEFAULT 0,
    primary_count   INT DEFAULT 0,
    secondary_count INT DEFAULT 0,
    status          ENUM('DRAFT', 'ACTIVE', 'ARCHIVED') DEFAULT 'DRAFT',
    source_file     VARCHAR(500),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 维度表（合并所有层级）
CREATE TABLE IF NOT EXISTS dynamic_dimension (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id         BIGINT NOT NULL,
    dimension_level     ENUM('LEVEL', 'PRIMARY', 'SECONDARY') NOT NULL,
    parent_id           BIGINT DEFAULT NULL,
    name                VARCHAR(200) NOT NULL,
    code                VARCHAR(100) NOT NULL,
    description         TEXT,
    sort_order          INT DEFAULT 0,
    weight              DECIMAL(10,6) DEFAULT 0,
    metric_type         ENUM('QUANTITATIVE', 'QUALITATIVE') DEFAULT NULL,
    aggregation_method  VARCHAR(50) DEFAULT NULL,
    score_direction     ENUM('POSITIVE', 'NEGATIVE') DEFAULT NULL,
    unit                VARCHAR(50) DEFAULT NULL,
    baseline_value      DECIMAL(20,6) DEFAULT NULL,
    target_value        DECIMAL(20,6) DEFAULT NULL,
    average_value       DECIMAL(20,6) DEFAULT NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (template_id) REFERENCES dynamic_template(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES dynamic_dimension(id) ON DELETE CASCADE,
    UNIQUE KEY uk_template_code (template_id, dimension_level, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
