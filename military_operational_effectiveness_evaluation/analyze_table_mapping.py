"""
分析权重表和打分表的指标对应关系
"""

import mysql.connector
from mysql.connector import Error


def analyze_table_structure():
    """分析两个表的指标对应关系"""
    try:
        connection = mysql.connector.connect(
            host='localhost',
            database='military_operational_effectiveness_evaluation',
            user='root',
            password='root'
        )
        
        if connection.is_connected():
            cursor = connection.cursor(dictionary=True)
            
            # ========== 1. 查看权重表结构 ==========
            print("=" * 80)
            print("1. 权重表 ahp_expert_military_operation_effect_weights")
            print("=" * 80)
            
            cursor.execute("DESCRIBE ahp_expert_military_operation_effect_weights")
            weight_cols = cursor.fetchall()
            
            print("\n权重表字段:")
            weight_fields = []
            for col in weight_cols:
                field = col['Field']
                print(f"  {field}")
                if 'weight' in field.lower() or 'indicator' in field.lower():
                    weight_fields.append(field)
            
            # 读取权重表数据
            cursor.execute("SELECT * FROM ahp_expert_military_operation_effect_weights LIMIT 3")
            weight_rows = cursor.fetchall()
            
            if weight_rows:
                print("\n权重表数据示例:")
                for row in weight_rows:
                    print(f"\n  权重ID: {row.get('id')}")
                    print(f"  权重名称: {row.get('weight_name')}")
                    # 打印所有权重字段
                    for key, value in row.items():
                        if value is not None and key not in ['id', 'weight_name', 'created_at', 'updated_at']:
                            print(f"    {key}: {value}")
            
            # ========== 2. 查看打分表结构 ==========
            print("\n" + "=" * 80)
            print("2. 打分表 military_operation_effect_score")
            print("=" * 80)
            
            cursor.execute("DESCRIBE military_operation_effect_score")
            score_cols = cursor.fetchall()
            
            print("\n打分表字段:")
            score_fields = []
            for col in score_cols:
                field = col['Field']
                print(f"  {field} ({col['Type']})")
                if field not in ['id', 'operation_id', 'created_at', 'updated_at']:
                    score_fields.append(field)
            
            # 读取打分表数据
            cursor.execute("SELECT * FROM military_operation_effect_score LIMIT 2")
            score_rows = cursor.fetchall()
            
            if score_rows:
                print("\n打分表数据示例:")
                for row in score_rows:
                    print(f"\n  操作ID: {row.get('operation_id')}")
                    for key, value in row.items():
                        if key not in ['id', 'operation_id', 'created_at', 'updated_at']:
                            print(f"    {key}: {value}")
            
            # ========== 3. 分析指标对应关系 ==========
            print("\n" + "=" * 80)
            print("3. 指标对应关系分析")
            print("=" * 80)
            
            print("\n打分表指标 -> 权重表对应字段:")
            
            # 建立映射关系
            mapping = {
                # 安全指标
                'security_key_leakage_qt': 'security_key_leakage_weight',
                'security_detected_probability_qt': 'security_detected_probability_weight',
                'security_interception_resistance_ql': 'security_interception_resistance_weight',
                
                # 可靠性指标
                'reliability_crash_rate_qt': 'reliability_crash_rate_weight',
                'reliability_recovery_capability_qt': 'reliability_recovery_capability_weight',
                'reliability_communication_availability_qt': 'reliability_communication_availability_weight',
                
                # 传输指标
                'transmission_bandwidth_qt': 'transmission_bandwidth_weight',
                'transmission_call_setup_time_qt': 'transmission_call_setup_time_weight',
                'transmission_transmission_delay_qt': 'transmission_transmission_delay_weight',
                'transmission_bit_error_rate_qt': 'transmission_bit_error_rate_weight',
                'transmission_throughput_qt': 'transmission_throughput_weight',
                'transmission_spectral_efficiency_qt': 'transmission_spectral_efficiency_weight',
                
                # 抗干扰指标
                'anti_jamming_sinr_qt': 'anti_jamming_sinr_weight',
                'anti_jamming_anti_jamming_margin_qt': 'anti_jamming_anti_jamming_margin_weight',
                'anti_jamming_communication_distance_qt': 'anti_jamming_communication_distance_weight',
                
                # 效能指标
                'effect_damage_rate_qt': 'effect_damage_rate_weight',
                'effect_mission_completion_rate_qt': 'effect_mission_completion_rate_weight',
            }
            
            for score_field, weight_field in mapping.items():
                print(f"  {score_field}")
                print(f"    -> {weight_field}")
            
            print(f"\n共计: {len(mapping)} 个指标对应")
            
    except Error as e:
        print(f"数据库错误: {e}")
    finally:
        if connection and connection.is_connected():
            connection.close()


if __name__ == "__main__":
    analyze_table_structure()
