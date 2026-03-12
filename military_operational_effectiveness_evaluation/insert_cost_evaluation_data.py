"""
为 cost_evaluation 表插入模拟数据（OP-2026-001 到 OP-2026-010）
"""

import mysql.connector
from mysql.connector import Error
import random
from datetime import datetime


def insert_cost_evaluation_data():
    """为 cost_evaluation 表插入10条模拟数据"""
    
    # 操作ID列表
    operation_ids = [f"OP-2026-{str(i).zfill(3)}" for i in range(1, 11)]
    
    # 评估时间
    base_time = datetime(2026, 3, 1, 10, 0, 0)
    
    # 随机种子，保证可复现
    random.seed(2026)
    
    try:
        connection = mysql.connector.connect(
            host='localhost',
            database='military_operational_effectiveness_evaluation',
            user='root',
            password='root'
        )
        
        if connection.is_connected():
            print("成功连接到数据库!")
            
            cursor = connection.cursor()
            
            # 先清空表
            cursor.execute("TRUNCATE TABLE cost_evaluation")
            print("已清空 cost_evaluation 表")
            
            # 插入数据
            insert_sql = """
            INSERT INTO cost_evaluation (
                operation_id, evaluation_time,
                cost_personnel_strategic_command_staff_qt,
                cost_personnel_campaign_command_staff_qt,
                cost_personnel_tactical_staff_qt,
                cost_personnel_equipment_operators_qt,
                cost_personnel_campaign_maintenance_hours_qt,
                cost_personnel_tactical_maintenance_hours_qt,
                cost_personnel_unit_maintenance_hours_qt,
                cost_equipment_procurement_total_qt,
                cost_equipment_depreciation_qt,
                cost_equipment_campaign_support_maintenance_qt,
                cost_energy_campaign_fuel_electricity_qt,
                cost_energy_tactical_fuel_battery_qt,
                cost_energy_unit_direct_qt,
                cost_logistics_spare_parts_availability_qt,
                cost_logistics_campaign_storage_transport_qt,
                cost_logistics_tactical_forward_delivery_qt,
                cost_training_total_budget_qt,
                cost_training_tactical_consumption_qt,
                cost_training_per_soldier_qt,
                cost_infrastructure_base_construction_qt,
                cost_infrastructure_spectrum_fee_qt,
                remarks
            ) VALUES (
                %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s
            )
            """
            
            for i, operation_id in enumerate(operation_ids):
                # 评估时间稍微错开
                eval_time = base_time.replace(hour=10 + i)
                
                # 根据operation_id生成不同的成本数据（每个OP有不同的成本配置）
                # 使用 operation_id 中的数字作为随机种子的一部分
                op_num = i + 1
                random.seed(1000 + op_num)
                
                # 人员成本 (万元)
                personnel_strategic = round(random.uniform(80, 150) + op_num * 2, 2)
                personnel_campaign = round(random.uniform(60, 120) + op_num * 1.5, 2)
                personnel_tactical = round(random.uniform(40, 80) + op_num * 1, 2)
                personnel_operators = round(random.uniform(50, 100) + op_num * 1.2, 2)
                personnel_campaign_maint = round(random.uniform(30, 60) + op_num * 0.8, 2)
                personnel_tactical_maint = round(random.uniform(20, 50) + op_num * 0.6, 2)
                personnel_unit_maint = round(random.uniform(15, 40) + op_num * 0.5, 2)
                
                # 设备成本 (万元) - decimal(5,2) 最大999.99
                equipment_procurement = round(random.uniform(200, 500) + op_num * 10, 2)
                equipment_depreciation = round(equipment_procurement * random.uniform(0.1, 0.2), 2)
                equipment_campaign_maint = round(random.uniform(50, 120) + op_num * 3, 2)
                
                # 能源成本 (万元)
                energy_campaign = round(random.uniform(40, 100) + op_num * 2, 2)
                energy_tactical = round(random.uniform(30, 70) + op_num * 1.5, 2)
                energy_unit = round(random.uniform(20, 50) + op_num * 1, 2)
                
                # 物流成本 (万元)
                logistics_spare = round(random.uniform(30, 80) + op_num * 2, 2)
                logistics_storage = round(random.uniform(20, 60) + op_num * 1.5, 2)
                logistics_forward = round(random.uniform(15, 50) + op_num * 1, 2)
                
                # 训练成本 (万元)
                training_total = round(random.uniform(40, 100) + op_num * 3, 2)
                training_tactical = round(random.uniform(20, 60) + op_num * 1.5, 2)
                training_per_soldier = round(random.uniform(2, 8) + op_num * 0.1, 2)
                
                # 基础设施成本 (万元)
                infra_base = round(random.uniform(80, 200) + op_num * 4, 2)
                infra_spectrum = round(random.uniform(10, 30) + op_num * 0.5, 2)
                
                # 备注
                remarks = f"军事通信装备效能评估成本数据 - 实验批次 {operation_id}"
                
                data = (
                    operation_id, eval_time,
                    personnel_strategic, personnel_campaign, personnel_tactical,
                    personnel_operators, personnel_campaign_maint, personnel_tactical_maint,
                    personnel_unit_maint, equipment_procurement, equipment_depreciation,
                    equipment_campaign_maint, energy_campaign, energy_tactical, energy_unit,
                    logistics_spare, logistics_storage, logistics_forward,
                    training_total, training_tactical, training_per_soldier,
                    infra_base, infra_spectrum, remarks
                )
                
                cursor.execute(insert_sql, data)
                print(f"已插入: {operation_id}")
            
            # 提交事务
            connection.commit()
            print(f"\n成功插入 {len(operation_ids)} 条记录!")
            
            # 验证插入结果
            cursor.execute("SELECT COUNT(*) as cnt FROM cost_evaluation")
            result = cursor.fetchone()
            print(f"表中共有 {result[0]} 条记录")
            
    except Error as e:
        print(f"数据库错误: {e}")
    finally:
        if connection and connection.is_connected():
            connection.close()
            print("\n数据库连接已关闭")


if __name__ == "__main__":
    insert_cost_evaluation_data()
