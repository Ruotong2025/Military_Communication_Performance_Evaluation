"""
为 military_operation_effect_score 表插入模拟数据（OP-2026-001 到 OP-2026-010）
所有指标均匀分布在 0-100 的打分数据
"""

import mysql.connector
from mysql.connector import Error
import random


def insert_effect_score_data():
    """为 military_operation_effect_score 表插入10条模拟数据（0-100分制均匀分布）"""
    
    # 操作ID列表
    operation_ids = [f"OP-2026-{str(i).zfill(3)}" for i in range(1, 11)]
    
    # 随机种子
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
            cursor.execute("TRUNCATE TABLE military_operation_effect_score")
            print("已清空 military_operation_effect_score 表")
            
            # 插入数据
            insert_sql = """
            INSERT INTO military_operation_effect_score (
                operation_id,
                security_key_leakage_qt,
                security_detected_probability_qt,
                security_interception_resistance_ql,
                reliability_crash_rate_qt,
                reliability_recovery_capability_qt,
                reliability_communication_availability_qt,
                transmission_bandwidth_qt,
                transmission_call_setup_time_qt,
                transmission_transmission_delay_qt,
                transmission_bit_error_rate_qt,
                transmission_throughput_qt,
                transmission_spectral_efficiency_qt,
                anti_jamming_sinr_qt,
                anti_jamming_anti_jamming_margin_qt,
                anti_jamming_communication_distance_qt,
                effect_damage_rate_qt,
                effect_mission_completion_rate_qt
            ) VALUES (
                %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s
            )
            """
            
            for i, operation_id in enumerate(operation_ids):
                op_num = i + 1
                random.seed(3000 + op_num)
                
                # ========== 安全指标 (0-100分) ==========
                # 均匀分布在 20-95 之间
                security_key_leakage = round(random.uniform(20, 95), 2)
                security_detected_prob = round(random.uniform(60, 95), 2)
                security_interception = round(random.uniform(55, 90), 2)
                
                # ========== 可靠性指标 (0-100分) ==========
                # 均匀分布在 25-95 之间
                reliability_crash = round(random.uniform(25, 95), 2)
                reliability_recovery = round(random.uniform(60, 95), 2)
                reliability_availability = round(random.uniform(65, 98), 2)
                
                # ========== 传输指标 (0-100分) ==========
                # 均匀分布在 30-95 之间
                transmission_bandwidth = round(random.uniform(30, 95), 2)
                transmission_call_setup = round(random.uniform(35, 95), 2)
                transmission_delay = round(random.uniform(30, 95), 2)
                transmission_ber = round(random.uniform(25, 95), 2)
                transmission_throughput = round(random.uniform(40, 95), 2)
                transmission_spectral = round(random.uniform(35, 90), 2)
                
                # ========== 抗干扰指标 (0-100分) ==========
                # 均匀分布在 25-90 之间
                anti_jamming_sinr = round(random.uniform(25, 90), 2)
                anti_jamming_margin = round(random.uniform(30, 90), 2)
                anti_jamming_distance = round(random.uniform(35, 95), 2)
                
                # ========== 效能指标 (0-100分) ==========
                # 均匀分布在 50-98 之间
                effect_damage = round(random.uniform(50, 98), 2)
                effect_mission = round(random.uniform(55, 98), 2)
                
                data = (
                    operation_id,
                    security_key_leakage,
                    security_detected_prob,
                    security_interception,
                    reliability_crash,
                    reliability_recovery,
                    reliability_availability,
                    transmission_bandwidth,
                    transmission_call_setup,
                    transmission_delay,
                    transmission_ber,
                    transmission_throughput,
                    transmission_spectral,
                    anti_jamming_sinr,
                    anti_jamming_margin,
                    anti_jamming_distance,
                    effect_damage,
                    effect_mission
                )
                
                cursor.execute(insert_sql, data)
                print(f"已插入: {operation_id}")
            
            # 提交事务
            connection.commit()
            print(f"\n成功插入 {len(operation_ids)} 条记录!")
            
            # 验证
            cursor.execute("SELECT COUNT(*) as cnt FROM military_operation_effect_score")
            result = cursor.fetchone()
            print(f"表中共有 {result[0]} 条记录")
            
    except Error as e:
        print(f"数据库错误: {e}")
    finally:
        if connection and connection.is_connected():
            connection.close()
            print("\n数据库连接已关闭")


if __name__ == "__main__":
    insert_effect_score_data()
