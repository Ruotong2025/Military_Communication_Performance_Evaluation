import pymysql

# 连接数据库
conn = pymysql.connect(
    host='localhost',
    user='root',
    password='root',
    database='military_operational_effectiveness_evaluation',
    charset='utf8mb4'
)
cursor = conn.cursor()

# 创建设备操作评分表
sql = '''
CREATE TABLE IF NOT EXISTS equipment_operation_score (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    batch_id VARCHAR(50) NOT NULL COMMENT '批次ID',
    expert_name VARCHAR(100) NOT NULL COMMENT '专家名称',
    
    -- 操作便捷性指标
    operation_ease_weight DECIMAL(5,2) COMMENT '操作便捷性权重',
    operation_ease_confidence DECIMAL(5,2) COMMENT '操作便捷性置信度',
    
    -- 维护性能指标
    maintenance_difficulty_weight DECIMAL(5,2) COMMENT '维护难度权重',
    maintenance_difficulty_confidence DECIMAL(5,2) COMMENT '维护难度置信度',
    maintenance_time_weight DECIMAL(5,2) COMMENT '维护时间权重',
    maintenance_time_confidence DECIMAL(5,2) COMMENT '维护时间置信度',
    
    -- 部署能力指标
    deployment_speed_weight DECIMAL(5,2) COMMENT '部署速度权重',
    deployment_speed_confidence DECIMAL(5,2) COMMENT '部署速度置信度',
    deployment_flexibility_weight DECIMAL(5,2) COMMENT '部署灵活性权重',
    deployment_flexibility_confidence DECIMAL(5,2) COMMENT '部署灵活性置信度',
    
    -- 人员培训指标
    training_required_weight DECIMAL(5,2) COMMENT '培训需求权重',
    training_required_confidence DECIMAL(5,2) COMMENT '培训需求置信度',
    operator_skill_requirement_weight DECIMAL(5,2) COMMENT '操作人员技能要求权重',
    operator_skill_requirement_confidence DECIMAL(5,2) COMMENT '操作人员技能要求置信度',
    
    -- 设备可靠性指标
    equipment_reliability_weight DECIMAL(5,2) COMMENT '设备可靠性权重',
    equipment_reliability_confidence DECIMAL(5,2) COMMENT '设备可靠性置信度',
    failure_rate_weight DECIMAL(5,2) COMMENT '故障率权重',
    failure_rate_confidence DECIMAL(5,2) COMMENT '故障率置信度',
    mtbf_weight DECIMAL(5,2) COMMENT '平均故障间隔时间权重',
    mtbf_confidence DECIMAL(5,2) COMMENT '平均故障间隔时间置信度',
    
    -- 操作安全性指标
    operation_safety_weight DECIMAL(5,2) COMMENT '操作安全性权重',
    operation_safety_confidence DECIMAL(5,2) COMMENT '操作安全性置信度',
    radiation_safety_weight DECIMAL(5,2) COMMENT '辐射安全性权重',
    radiation_safety_confidence DECIMAL(5,2) COMMENT '辐射安全性置信度',
    
    -- 成本指标
    operation_cost_weight DECIMAL(5,2) COMMENT '运营成本权重',
    operation_cost_confidence DECIMAL(5,2) COMMENT '运营成本置信度',
    lifecycle_cost_weight DECIMAL(5,2) COMMENT '全生命周期成本权重',
    lifecycle_cost_confidence DECIMAL(5,2) COMMENT '全生命周期成本置信度',
    
    -- 兼容性指标
    compatibility_weight DECIMAL(5,2) COMMENT '兼容性权重',
    compatibility_confidence DECIMAL(5,2) COMMENT '兼容性置信度',
    interoperability_weight DECIMAL(5,2) COMMENT '互操作性权重',
    interoperability_confidence DECIMAL(5,2) COMMENT '互操作性置信度',
    
    remarks TEXT COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_batch_id (batch_id),
    INDEX idx_expert_name (expert_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备操作AHP专家评分表'
'''

cursor.execute(sql)
conn.commit()
print('表创建成功！')

# 查看表结构
cursor.execute('DESCRIBE equipment_operation_score')
print('\n=== equipment_operation_score 表结构 ===')
for col in cursor.fetchall():
    print(col[0], col[1])

conn.close()
