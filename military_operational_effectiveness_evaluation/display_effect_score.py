"""
验证并显示 military_operation_effect_score 表数据
"""

import mysql.connector
from mysql.connector import Error


def display_effect_score():
    """显示 military_operation_effect_score 表数据"""
    try:
        connection = mysql.connector.connect(
            host='localhost',
            database='military_operational_effectiveness_evaluation',
            user='root',
            password='root'
        )
        
        if connection.is_connected():
            cursor = connection.cursor(dictionary=True)
            
            cursor.execute("SELECT * FROM military_operation_effect_score ORDER BY id")
            rows = cursor.fetchall()
            
            print("=" * 80)
            print("military_operation_effect_score 表数据摘要")
            print("=" * 80)
            print(f"共 {len(rows)} 条记录\n")
            
            # 打印表格头部
            print(f"{'ID':<4} {'operation_id':<15} {'安全':<8} {'可靠性':<8} {'传输':<8} {'抗干扰':<8} {'效能':<8}")
            print("-" * 65)
            
            for row in rows:
                # 计算各分类的平均分（简化显示）
                security = (row.get('security_detected_probability_qt', 0) or 0 +
                           row.get('security_interception_resistance_ql', 0) or 0) / 2
                reliability = (row.get('reliability_recovery_capability_qt', 0) or 0 +
                             row.get('reliability_communication_availability_qt', 0) or 0) / 2
                transmission = (row.get('transmission_throughput_qt', 0) or 0 +
                              row.get('transmission_spectral_efficiency_qt', 0) or 0) / 2
                anti_jamming = (row.get('anti_jamming_sinr_qt', 0) or 0 +
                               row.get('anti_jamming_anti_jamming_margin_qt', 0) or 0) / 2
                effect = (row.get('effect_damage_rate_qt', 0) or 0 +
                         row.get('effect_mission_completion_rate_qt', 0) or 0) / 2
                
                print(f"{row['id']:<4} {row['operation_id']:<15} {security:<8.1f} {reliability:<8.1f} {transmission:<8.1f} {anti_jamming:<8.1f} {effect:<8.1f}")
            
            # 详细分类
            print("\n" + "=" * 80)
            print("详细指标分类")
            print("=" * 80)
            
            for row in rows:
                print(f"\n【{row['operation_id']}】")
                
                # 安全指标
                print("  安全指标:")
                print(f"    密钥泄露率: {row.get('security_key_leakage_qt', 0) or 0}")
                print(f"    被发现概率: {row.get('security_detected_probability_qt', 0) or 0}")
                print(f"    抗截获能力: {row.get('security_interception_resistance_ql', 0) or 0}")
                
                # 可靠性指标
                print("  可靠性指标:")
                print(f"    故障率: {row.get('reliability_crash_rate_qt', 0) or 0}")
                print(f"    恢复能力: {row.get('reliability_recovery_capability_qt', 0) or 0}")
                print(f"    通信可用性: {row.get('reliability_communication_availability_qt', 0) or 0}")
                
                # 传输指标
                print("  传输指标:")
                print(f"    带宽(MHz): {row.get('transmission_bandwidth_qt', 0) or 0}")
                print(f"    建立时间(s): {row.get('transmission_call_setup_time_qt', 0) or 0}")
                print(f"    传输时延(ms): {row.get('transmission_transmission_delay_qt', 0) or 0}")
                print(f"    吞吐量(Mbps): {row.get('transmission_throughput_qt', 0) or 0}")
                print(f"    频谱效率(bps/Hz): {row.get('transmission_spectral_efficiency_qt', 0) or 0}")
                
                # 抗干扰指标
                print("  抗干扰指标:")
                print(f"    信干噪比(dB): {row.get('anti_jamming_sinr_qt', 0) or 0}")
                print(f"    抗干扰余量(dB): {row.get('anti_jamming_anti_jamming_margin_qt', 0) or 0}")
                print(f"    通信距离(km): {row.get('anti_jamming_communication_distance_qt', 0) or 0}")
                
                # 效能指标
                print("  效能指标:")
                print(f"    毁伤率: {row.get('effect_damage_rate_qt', 0) or 0}")
                print(f"    任务完成率: {row.get('effect_mission_completion_rate_qt', 0) or 0}")
            
            return rows
            
    except Error as e:
        print(f"数据库错误: {e}")
        return None
    finally:
        if connection and connection.is_connected():
            connection.close()


if __name__ == "__main__":
    display_effect_score()
