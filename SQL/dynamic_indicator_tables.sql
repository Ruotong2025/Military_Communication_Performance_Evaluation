-- ============================================
-- 动态指标系统数据库表
-- ============================================

-- 1. 指标模板表
CREATE TABLE IF NOT EXISTS mtl_indicator_template (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_name    VARCHAR(200) NOT NULL,
    template_code    VARCHAR(50) NOT NULL UNIQUE,
    description      TEXT,
    level_count      INT DEFAULT 0,
    primary_count    INT DEFAULT 0,
    secondary_count  INT DEFAULT 0,
    status           ENUM('DRAFT', 'ACTIVE', 'ARCHIVED') DEFAULT 'DRAFT',
    source_file      VARCHAR(500),
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 层级定义表
CREATE TABLE IF NOT EXISTS mtl_level_definition (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id     BIGINT NOT NULL,
    level_name      VARCHAR(200) NOT NULL,
    level_code      VARCHAR(100) NOT NULL,
    description     TEXT,
    sort_order      INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (template_id) REFERENCES mtl_indicator_template(id) ON DELETE CASCADE,
    UNIQUE KEY uk_template_level (template_id, level_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 一级维度定义表
CREATE TABLE IF NOT EXISTS mtl_primary_dimension (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    level_id        BIGINT NOT NULL,
    dimension_name  VARCHAR(200) NOT NULL,
    dimension_code  VARCHAR(100) NOT NULL,
    sort_order      INT DEFAULT 0,
    weight          DECIMAL(10,6) DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (level_id) REFERENCES mtl_level_definition(id) ON DELETE CASCADE,
    UNIQUE KEY uk_level_dimension (level_id, dimension_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 二级维度定义表
CREATE TABLE IF NOT EXISTS mtl_secondary_dimension (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    primary_dimension_id BIGINT NOT NULL,
    dimension_name      VARCHAR(200) NOT NULL,
    dimension_code      VARCHAR(100) NOT NULL,
    sort_order          INT DEFAULT 0,
    metric_type         ENUM('QUANTITATIVE', 'QUALITATIVE') DEFAULT 'QUANTITATIVE',
    aggregation_method  VARCHAR(50) DEFAULT 'avg',
    score_direction     ENUM('POSITIVE', 'NEGATIVE') DEFAULT 'POSITIVE',
    unit                VARCHAR(50),
    baseline_value      DECIMAL(20,6),
    target_value        DECIMAL(20,6),
    weight              DECIMAL(10,6) DEFAULT 0,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (primary_dimension_id) REFERENCES mtl_primary_dimension(id) ON DELETE CASCADE,
    UNIQUE KEY uk_primary_secondary (primary_dimension_id, dimension_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. AHP判断矩阵表
CREATE TABLE IF NOT EXISTS mtl_ahp_judgment_matrix (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    level_id            BIGINT NOT NULL,
    matrix_data         JSON NOT NULL,
    consistency_ratio   DECIMAL(10,6) DEFAULT 0,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (level_id) REFERENCES mtl_level_definition(id) ON DELETE CASCADE,
    UNIQUE KEY uk_level_matrix (level_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
