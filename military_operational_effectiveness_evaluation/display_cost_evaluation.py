"""
从本地MySQL数据库读取 cost_evaluation 表数据并格式化显示
"""

import mysql.connector
from mysql.connector import Error


def read_cost_evaluation_display():
    """从MySQL数据库读取 cost_evaluation 表并显示"""
    try:
        connection = mysql.connector.connect(
            host='localhost',
            database='military_operational_effectiveness_evaluation',
            user='root',
            password='root'
        )
        
        if connection.is_connected():
            cursor = connection.cursor(dictionary=True)
            
            # 读取所有数据
            cursor.execute("SELECT * FROM cost_evaluation ORDER BY id")
            rows = cursor.fetchall()
            
            # 打印简洁的摘要
            print("=" * 80)
            print("cost_evaluation 表数据摘要")
            print("=" * 80)
            print(f"共 {len(rows)} 条记录\n")
            
            # 打印表格头部
            print(f"{'ID':<4} {'operation_id':<15} {'总成本(万元)':<12} {'评估时间':<20}")
            print("-" * 55)
            
            cost_fields = [
                'cost_personnel_strategic_command_staff_qt',
                'cost_personnel_campaign_command_staff_qt',
                'cost_personnel_tactical_staff_qt',
                'cost_personnel_equipment_operators_qt',
                'cost_personnel_campaign_maintenance_hours_qt',
                'cost_personnel_tactical_maintenance_hours_qt',
                'cost_personnel_unit_maintenance_hours_qt',
                'cost_equipment_procurement_total_qt',
                'cost_equipment_depreciation_qt',
                'cost_equipment_campaign_support_maintenance_qt',
                'cost_energy_campaign_fuel_electricity_qt',
                'cost_energy_tactical_fuel_battery_qt',
                'cost_energy_unit_direct_qt',
                'cost_logistics_spare_parts_availability_qt',
                'cost_logistics_campaign_storage_transport_qt',
                'cost_logistics_tactical_forward_delivery_qt',
                'cost_training_total_budget_qt',
                'cost_training_tactical_consumption_qt',
                'cost_training_per_soldier_qt',
                'cost_infrastructure_base_construction_qt',
                'cost_infrastructure_spectrum_fee_qt'
            ]
            
            for row in rows:
                # 计算总成本
                total = sum([row.get(f, 0) or 0 for f in cost_fields])
                print(f"{row['id']:<4} {row['operation_id']:<15} {total:<12.2f} {str(row['evaluation_time']):<20}")
            
            print("\n" + "=" * 80)
            print("详细成本分类汇总 (万元)")
            print("=" * 80)
            
            for row in rows:
                print(f"\n【{row['operation_id']}】")
                
                # 人员成本
                personnel = (row.get('cost_personnel_strategic_command_staff_qt', 0) or 0 +
                            row.get('cost_personnel_campaign_command_staff_qt', 0) or 0 +
                            row.get('cost_personnel_tactical_staff_qt', 0) or 0 +
                            row.get('cost_personnel_equipment_operators_qt', 0) or 0 +
                            row.get('cost_personnel_campaign_maintenance_hours_qt', 0) or 0 +
                            row.get('cost_personnel_tactical_maintenance_hours_qt', 0) or 0 +
                            row.get('cost_personnel_unit_maintenance_hours_qt', 0) or 0)
                
                # 设备成本
                equipment = (row.get('cost_equipment_procurement_total_qt', 0) or 0 +
                           row.get('cost_equipment_depreciation_qt', 0) or 0 +
                           row.get('cost_equipment_campaign_support_maintenance_qt', 0) or 0)
                
                # 能源成本
                energy = (row.get('cost_energy_campaign_fuel_electricity_qt', 0) or 0 +
                         row.get('cost_energy_tactical_fuel_battery_qt', 0) or 0 +
                         row.get('cost_energy_unit_direct_qt', 0) or 0)
                
                # 物流成本
                logistics = (row.get('cost_logistics_spare_parts_availability_qt', 0) or 0 +
                           row.get('cost_logistics_campaign_storage_transport_qt', 0) or 0 +
                           row.get('cost_logistics_tactical_forward_delivery_qt', 0) or 0)
                
                # 训练成本
                training = (row.get('cost_training_total_budget_qt', 0) or 0 +
                          row.get('cost_training_tactical_consumption_qt', 0) or 0 +
                          row.get('cost_training_per_soldier_qt', 0) or 0)
                
                # 基础设施成本
                infra = (row.get('cost_infrastructure_base_construction_qt', 0) or 0 +
                        row.get('cost_infrastructure_spectrum_fee_qt', 0) or 0)
                
                print(f"  人员成本: {personnel:.2f}")
                print(f"  设备成本: {equipment:.2f}")
                print(f"  能源成本: {energy:.2f}")
                print(f"  物流成本: {logistics:.2f}")
                print(f"  训练成本: {training:.2f}")
                print(f"  基础设施: {infra:.2f}")
                print(f"  总计: {personnel + equipment + energy + logistics + training + infra:.2f}")
            
            return rows
            
    except Error as e:
        print(f"数据库错误: {e}")
        return None
    finally:
        if connection and connection.is_connected():
            connection.close()


if __name__ == "__main__":
    read_cost_evaluation_display()
