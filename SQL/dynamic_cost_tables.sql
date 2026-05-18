-- 动态效费分析表

-- 1. 效费分析模板表
CREATE TABLE dynamic_cost_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    template_name VARCHAR(200) NOT NULL COMMENT '模板名称',
    template_code VARCHAR(100) UNIQUE NOT NULL COMMENT '模板编码',
    cost_config JSON NOT NULL COMMENT '成本指标配置JSON',
    total_cost_count INT DEFAULT 0 COMMENT '成本指标数量',
    status TINYINT DEFAULT 1 COMMENT '状态：1-有效，0-删除',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_template_code (template_code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态效费分析模板表';

-- 2. 模拟批次表
CREATE TABLE dynamic_cost_batch (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    template_id BIGINT NOT NULL COMMENT '所属模板ID',
    batch_code VARCHAR(100) NOT NULL COMMENT '批次编码',
    batch_name VARCHAR(200) COMMENT '批次名称',
    simulation_count INT DEFAULT 1000 COMMENT '模拟次数',
    random_seed BIGINT COMMENT '随机种子',
    effectiveness_score DECIMAL(10,4) COMMENT '效能得分',
    cost_score DECIMAL(10,4) COMMENT '成本得分',
    effectiveness_cost_ratio DECIMAL(10,4) COMMENT '效费比',
    result_summary JSON COMMENT '结果摘要JSON（包含分布统计）',
    status TINYINT DEFAULT 1 COMMENT '状态：1-有效，0-删除',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (template_id) REFERENCES dynamic_cost_template(id),
    INDEX idx_template_id (template_id),
    INDEX idx_batch_code (batch_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态效费分析模拟批次表';

-- 3. 模拟详细数据表
CREATE TABLE dynamic_cost_simulation_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    batch_id BIGINT NOT NULL COMMENT '所属批次ID',
    iteration_number INT NOT NULL COMMENT '迭代序号',
    indicator_name VARCHAR(200) NOT NULL COMMENT '指标名称',
    indicator_code VARCHAR(100) COMMENT '指标编码',
    level1_name VARCHAR(100) COMMENT '一级维度',
    level2_name VARCHAR(100) COMMENT '二级维度',
    simulated_value DECIMAL(15,4) COMMENT '模拟值',
    normalized_value DECIMAL(10,4) COMMENT '归一化值',
    weight DECIMAL(8,4) COMMENT '权重',
    FOREIGN KEY (batch_id) REFERENCES dynamic_cost_batch(id),
    INDEX idx_batch_id (batch_id),
    INDEX idx_iteration (iteration_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态效费分析模拟详细数据表';
