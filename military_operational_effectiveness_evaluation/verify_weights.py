"""
验证权重表权重是否加起来为1
"""

import mysql.connector


def verify_weights():
    connection = mysql.connector.connect(
        host='localhost',
        database='military_operational_effectiveness_evaluation',
        user='root',
        password='root'
    )

    cursor = connection.cursor(dictionary=True)

    # 权重字段映射
    weight_fields = [
        'security_key_leakage_weight',
        'security_detected_probability_weight',
        'security_interception_resistance_weight',
        'reliability_crash_rate_weight',
        'reliability_recovery_capability_weight',
        'reliability_communication_availability_weight',
        'transmission_bandwidth_weight',
        'transmission_call_setup_time_weight',
        'transmission_transmission_delay_weight',
        'transmission_bit_error_rate_weight',
        'transmission_throughput_weight',
        'transmission_spectral_efficiency_weight',
        'anti_jamming_sinr_weight',
        'anti_jamming_anti_jamming_margin_weight',
        'anti_jamming_communication_distance_weight',
        'effect_damage_rate_weight',
        'effect_mission_completion_rate_weight',
    ]

    # 查询所有权重数据
    cursor.execute("SELECT * FROM ahp_expert_military_operation_effect_weights ORDER BY id")
    weights = cursor.fetchall()

    print(f"共有 {len(weights)} 位专家的权重数据\n")

    # 计算每个专家的权重总和
    for i, w in enumerate(weights):
        total = 0
        print(f"专家 {i+1}: {w.get('expert_name', '未知')}")
        for field in weight_fields:
            val = float(w.get(field, 0) or 0)
            total += val
        print(f"  权重总和: {total}")

    # 计算平均权重总和
    total_sum = 0
    for field in weight_fields:
        field_sum = 0
        for w in weights:
            field_sum += float(w.get(field, 0) or 0)
        avg = field_sum / len(weights) if weights else 0
        total_sum += avg

    print(f"\n平均权重总和: {total_sum}")

    # 验证归一化后的权重
    print("\n" + "="*50)
    print("归一化后的平均权重:")
    print("="*50)

    avg_weights = {}
    for field in weight_fields:
        field_sum = 0
        for w in weights:
            field_sum += float(w.get(field, 0) or 0)
        avg_weights[field] = field_sum / len(weights) if weights else 0

    # 归一化
    normalized_weights = {}
    for field, avg in avg_weights.items():
        normalized_weights[field] = avg / total_sum if total_sum > 0 else 0

    # 打印归一化后的权重
    for field, w in normalized_weights.items():
        print(f"  {field}: {w:.4f}")

    print(f"\n归一化后权重总和: {sum(normalized_weights.values())}")

    connection.close()


if __name__ == "__main__":
    verify_weights()
