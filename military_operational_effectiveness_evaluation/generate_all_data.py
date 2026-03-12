# -*- coding: utf-8 -*-
"""
军事作战效能评估数据生成工具 - 统一版本
整合所有数据生成功能到一个脚本中
"""
import mysql.connector
from mysql.connector import Error
from datetime import datetime, timedelta, date
import random
import numpy as np


class DataGenerator:
    """统一的数据生成器"""

    def __init__(self):
        self.connection = mysql.connector.connect(
            host='localhost',
            database='military_operational_effectiveness_evaluation',
            user='root',
            password='root'
        )
        # 专家名称列表
        self.expert_names = [
            '张军', '李建国', '王海峰', '刘芳', '陈伟',
            '赵敏', '孙强', '周婷', '吴磊', '郑雪'
        ]

    def close(self):
        """关闭数据库连接"""
        if self.connection and self.connection.is_connected():
            self.connection.close()

    # ==================== 1. 专家可信度评估数据 ====================
    def generate_expert_credibility_data(self):
        """生成10位专家的可信度评估数据"""
        print("\n" + "=" * 80)
        print("【1/4】生成专家可信度评估数据")
        print("=" * 80)

        # 专家配置
        experts_config = [
            {'name': '张军', 'profile': '学术型专家', 'strengths': ['学术', '科研', '理论', '统计'], 'weaknesses': ['演习', '仿真']},
            {'name': '李建国', 'profile': '实战型专家', 'strengths': ['演习', '军事', '职务'], 'weaknesses': ['学术', '统计']},
            {'name': '王海峰', 'profile': '技术型专家', 'strengths': ['仿真', '统计', '科研'], 'weaknesses': ['职务', '演习']},
            {'name': '刘芳', 'profile': '综合型专家', 'strengths': [], 'weaknesses': []},  # 均衡
            {'name': '陈伟', 'profile': '年轻学术新星', 'strengths': ['学历', '仿真', '统计'], 'weaknesses': ['职称', '职务', '年限']},
            {'name': '赵敏', 'profile': '资深管理者', 'strengths': ['职务', '职称', '年限', '演习'], 'weaknesses': ['仿真', '统计']},
            {'name': '孙强', 'profile': '科研骨干', 'strengths': ['科研', '学术', '军事'], 'weaknesses': ['职务', '演习']},
            {'name': '周婷', 'profile': '仿真技术专家', 'strengths': ['仿真', '统计', '学历'], 'weaknesses': ['职称', '职务', '演习', '年限']},
            {'name': '吴磊', 'profile': '基层实战者', 'strengths': ['演习', '军事'], 'weaknesses': ['职称', '学历', '学术', '统计']},
            {'name': '郑雪', 'profile': '年轻技术员', 'strengths': ['学历', '仿真', '统计'], 'weaknesses': ['职称', '职务', '演习', '年限', '学术']},
        ]

        # 基础分数范围
        score_ranges = {
            'title_ql': (95, 100), 'position_ql': (85, 92), 'education_experience_ql': (95, 100),
            'academic_achievements_ql': (95, 100), 'research_achievements_ql': (92, 98),
            'exercise_experience_ql': (65, 75), 'military_training_knowledge_ql': (90, 98),
            'system_simulation_knowledge_ql': (70, 80), 'statistics_knowledge_ql': (92, 98),
            'professional_years_qt': (95, 100)
        }

        # 专家特定调整
        expert_adjustments = {
            '张军': {'exercise_experience_ql': (65, 75), 'system_simulation_knowledge_ql': (70, 80)},
            '李建国': {'academic_achievements_ql': (65, 75), 'statistics_knowledge_ql': (60, 70), 'title_ql': (80, 88)},
            '王海峰': {'exercise_experience_ql': (60, 70), 'position_ql': (65, 75), 'military_training_knowledge_ql': (75, 85)},
            '刘芳': {},  # 均衡
            '陈伟': {'title_ql': (60, 70), 'position_ql': (55, 65), 'professional_years_qt': (50, 60), 'exercise_experience_ql': (65, 75)},
            '赵敏': {'system_simulation_knowledge_ql': (55, 65), 'statistics_knowledge_ql': (58, 68), 'academic_achievements_ql': (68, 78)},
            '孙强': {'position_ql': (55, 65), 'exercise_experience_ql': (50, 60)},
            '周婷': {'title_ql': (58, 68), 'position_ql': (52, 62), 'exercise_experience_ql': (48, 58), 'professional_years_qt': (48, 58)},
            '吴磊': {'title_ql': (45, 55), 'education_experience_ql': (50, 60), 'academic_achievements_ql': (40, 50), 'statistics_knowledge_ql': (45, 55)},
            '郑雪': {'title_ql': (42, 52), 'position_ql': (45, 55), 'exercise_experience_ql': (40, 50), 'professional_years_qt': (40, 50), 'academic_achievements_ql': (48, 58)},
        }

        cursor = self.connection.cursor()

        # 清空旧数据
        cursor.execute("DELETE FROM expert_credibility_evaluation_score")
        self.connection.commit()

        insert_query = """
            INSERT INTO expert_credibility_evaluation_score (
                expert_name, title_ql, position_ql, education_experience_ql,
                academic_achievements_ql, research_achievements_ql, exercise_experience_ql,
                military_training_knowledge_ql, system_simulation_knowledge_ql,
                statistics_knowledge_ql, professional_years_qt, evaluation_date, remarks
            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """

        for config in experts_config:
            name = config['name']
            adjustments = expert_adjustments.get(name, {})

            # 生成各维度分数
            expert_data = {}
            for field, default_range in score_ranges.items():
                if field in adjustments:
                    expert_data[field] = random.uniform(*adjustments[field])
                else:
                    expert_data[field] = random.uniform(*default_range)

            # 计算综合得分
            scores = list(expert_data.values())
            avg_score = sum(scores) / len(scores)

            remarks = f"{config['profile']}，综合得分：{avg_score:.2f}"

            values = (
                name,
                round(expert_data['title_ql'], 2),
                round(expert_data['position_ql'], 2),
                round(expert_data['education_experience_ql'], 2),
                round(expert_data['academic_achievements_ql'], 2),
                round(expert_data['research_achievements_ql'], 2),
                round(expert_data['exercise_experience_ql'], 2),
                round(expert_data['military_training_knowledge_ql'], 2),
                round(expert_data['system_simulation_knowledge_ql'], 2),
                round(expert_data['statistics_knowledge_ql'], 2),
                round(expert_data['professional_years_qt'], 2),
                date.today(),
                remarks
            )

            cursor.execute(insert_query, values)
            print(f"  - {name}: {config['profile']}, 综合得分: {avg_score:.2f}")

        self.connection.commit()
        cursor.close()
        print(f"  >> 成功生成 {len(experts_config)} 位专家数据")

    # ==================== 2. AHP权重数据 ====================
    def generate_ahp_weights_data(self):
        """生成AHP权重评分数据"""
        print("\n" + "=" * 80)
        print("【2/4】生成AHP权重评分数据")
        print("=" * 80)

        # 获取专家列表
        cursor = self.connection.cursor(dictionary=True)
        cursor.execute("SELECT id, expert_name FROM expert_credibility_evaluation_score ORDER BY id")
        experts = cursor.fetchall()
        cursor.close()

        if not experts:
            print("  未找到专家数据，请先运行 generate_expert_credibility_data()")
            return

        # 指标配置 - 低离散度版本
        indicator_categories = {
            'security': {'key_leakage': 6, 'detected_probability': 5, 'interception_resistance': 7},
            'reliability': {'crash_rate': 9, 'recovery_capability': 7, 'communication_availability': 8},
            'transmission': {'bandwidth': 6, 'call_setup_time': 4, 'transmission_delay': 6, 'bit_error_rate': 6, 'throughput': 5, 'spectral_efficiency': 5},
            'anti_jamming': {'sinr': 8, 'anti_jamming_margin': 7, 'communication_distance': 7},
            'resource': {'power_consumption': 3, 'manpower_requirement': 2.5},
            'effect': {'damage_rate': 8, 'mission_completion_rate': 9, 'cost_effectiveness': 5}
        }

        cursor = self.connection.cursor()

        # 清空旧数据（可选）
        cursor.execute("DELETE FROM ahp_expert_military_operation_effect_weights WHERE batch_id = 'AHP-2026-001'")
        self.connection.commit()

        batch_id = "AHP-2026-001"

        insert_query = """
            INSERT INTO ahp_expert_military_operation_effect_weights (
                batch_id, expert_name,
                security_key_leakage_weight, security_key_leakage_confidence,
                security_detected_probability_weight, security_detected_probability_confidence,
                security_interception_resistance_weight, security_interception_resistance_confidence,
                reliability_crash_rate_weight, reliability_crash_rate_confidence,
                reliability_recovery_capability_weight, reliability_recovery_capability_confidence,
                reliability_communication_availability_weight, reliability_communication_availability_confidence,
                transmission_bandwidth_weight, transmission_bandwidth_confidence,
                transmission_call_setup_time_weight, transmission_call_setup_time_confidence,
                transmission_transmission_delay_weight, transmission_transmission_delay_confidence,
                transmission_bit_error_rate_weight, transmission_bit_error_rate_confidence,
                transmission_throughput_weight, transmission_throughput_confidence,
                transmission_spectral_efficiency_weight, transmission_spectral_efficiency_confidence,
                anti_jamming_sinr_weight, anti_jamming_sinr_confidence,
                anti_jamming_anti_jamming_margin_weight, anti_jamming_anti_jamming_margin_confidence,
                anti_jamming_communication_distance_weight, anti_jamming_communication_distance_confidence,
                resource_power_consumption_weight, resource_power_consumption_confidence,
                resource_manpower_requirement_weight, resource_manpower_requirement_confidence,
                effect_damage_rate_weight, effect_damage_rate_confidence,
                effect_mission_completion_rate_weight, effect_mission_completion_rate_confidence,
                effect_cost_effectiveness_weight, effect_cost_effectiveness_confidence,
                remarks
            ) VALUES (
                %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s,
                %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s,
                %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s
            )
        """

        all_weights = []

        for expert in experts:
            weights = []
            confidences = []

            # 生成各指标权重
            for category, indicators in indicator_categories.items():
                for indicator_key, base_weight in indicators.items():
                    # 添加随机扰动
                    weight = base_weight + random.uniform(-1.5, 1.5)
                    weight = max(0, min(10, round(weight)))
                    weights.append(weight)

                    # 把握度
                    confidence = 0.7 + random.uniform(-0.15, 0.2)
                    confidence = max(0.5, min(1.0, round(confidence, 2)))
                    confidences.append(confidence)

            all_weights.append(weights)

            avg_weight = sum(weights) / len(weights)
            avg_confidence = sum(confidences) / len(confidences)
            remarks = f"批次AHP-2026-001，专家：{expert['expert_name']}，平均权重：{avg_weight:.2f}，平均把握度：{avg_confidence:.2f}"

            values = [batch_id, expert['expert_name']]
            values.extend(weights)
            values.extend(confidences)
            values.append(remarks)

            cursor.execute(insert_query, tuple(values))
            print(f"  - 专家 {expert['expert_name']}: 平均权重={avg_weight:.2f}, 平均把握度={avg_confidence:.2f}")

        self.connection.commit()
        cursor.close()
        print(f"  >> 成功生成 {len(experts)} 位专家的AHP权重数据")

        # 计算离散度
        all_weights = np.array(all_weights)
        cvs = []
        for i in range(all_weights.shape[1]):
            mean_val = np.mean(all_weights[:, i])
            std_val = np.std(all_weights[:, i], ddof=1)
            cv = (std_val / mean_val) * 100 if mean_val > 0 else 0
            cvs.append(cv)

        avg_cv = np.mean(cvs)
        print(f"  >> 整体平均CV: {avg_cv:.2f}%")

    # ==================== 3. 装备操作评分数据 ====================
    def generate_equipment_scores_data(self):
        """生成装备操作评分数据"""
        print("\n" + "=" * 80)
        print("【3/4】生成装备操作评分数据")
        print("=" * 80)

        # 获取专家列表
        cursor = self.connection.cursor(dictionary=True)
        cursor.execute("SELECT id, expert_name FROM expert_credibility_evaluation_score ORDER BY id")
        experts = cursor.fetchall()
        cursor.close()

        if not experts:
            print("  未找到专家数据")
            return

        # 评分字段
        score_fields = [
            'personnel_personnel_count_qt', 'personnel_work_experience_qt', 'personnel_training_experience_qt',
            'system_setup_network_setup_time_qt', 'maintenance_maintenance_skill_ql',
            'maintenance_spare_parts_availability_qt', 'response_emergency_handling_qt',
            'comm_support_link_maintenance_qt', 'comm_support_service_activation_qt',
            'comm_support_emergency_restoration_qt', 'comm_attack_target_acquisition_qt',
            'comm_attack_jamming_effectiveness_ql', 'comm_attack_deception_signal_generation_ql',
            'comm_defense_signal_interception_awareness_ql', 'comm_defense_anti_jamming_operation_ql',
            'comm_defense_anti_deception_awareness_ql', 'system_performance_connectivity_rate_qt',
            'system_performance_mission_reliability_qt', 'maintenance_feedback_field_repair_qt',
            'maintenance_feedback_rework_rate_qt', 'maintenance_feedback_equipment_feedback_ql'
        ]

        cursor = self.connection.cursor()

        # 清空旧数据
        cursor.execute("DELETE FROM equipment_operation_score")
        self.connection.commit()

        insert_query = """
            INSERT INTO equipment_operation_score (
                operation_id, evaluation_time, updated_at,
                personnel_personnel_count_qt, personnel_work_experience_qt, personnel_training_experience_qt,
                system_setup_network_setup_time_qt, maintenance_maintenance_skill_ql,
                maintenance_spare_parts_availability_qt, response_emergency_handling_qt,
                comm_support_link_maintenance_qt, comm_support_service_activation_qt,
                comm_support_emergency_restoration_qt, comm_attack_target_acquisition_qt,
                comm_attack_jamming_effectiveness_ql, comm_attack_deception_signal_generation_ql,
                comm_defense_signal_interception_awareness_ql, comm_defense_anti_jamming_operation_ql,
                comm_defense_anti_deception_awareness_ql, system_performance_connectivity_rate_qt,
                system_performance_mission_reliability_qt, maintenance_feedback_field_repair_qt,
                maintenance_feedback_rework_rate_qt, maintenance_feedback_equipment_feedback_ql,
                remarks
            ) VALUES (
                %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s
            )
        """

        # 生成10个实验
        num_experiments = 10

        for exp_idx in range(num_experiments):
            expert = experts[exp_idx % len(experts)]
            operation_id = f"OP-2026-{exp_idx + 1:03d}"
            evaluation_time = datetime.now() - timedelta(days=random.randint(0, 30))

            # 生成评分
            scores = []
            for _ in range(len(score_fields)):
                score = random.uniform(60, 95)
                scores.append(round(score, 2))

            avg_score = sum(scores) / len(scores)
            remarks = f"实验{exp_idx + 1}：{expert['expert_name']}，平均分：{avg_score:.2f}"

            values = [operation_id, evaluation_time, evaluation_time]
            values.extend(scores)
            values.append(remarks)

            cursor.execute(insert_query, tuple(values))
            print(f"  - 实验 {operation_id}: 平均分={avg_score:.2f}")

        self.connection.commit()
        cursor.close()
        print(f"  >> 成功生成 {num_experiments} 个实验的评分数据")

    # ==================== 4. 成本评估数据 ====================
    def generate_cost_evaluation_data(self):
        """生成成本评估参数数据"""
        print("\n" + "=" * 80)
        print("【4/4】生成成本评估参数数据")
        print("=" * 80)

        # 指标配置
        indicators = [
            ('personnel_personnel_count_qt', '人员数量', (50, 200), 'direct'),
            ('personnel_work_experience_qt', '工作经验', (30, 100), 'direct'),
            ('personnel_training_experience_qt', '培训经验', (20, 80), 'direct'),
            ('system_setup_network_setup_time_qt', '网络设置时间', (10, 50), 'inverse'),
            ('maintenance_maintenance_skill_ql', '维修技能', (40, 120), 'direct'),
            ('maintenance_spare_parts_availability_qt', '备件可用性', (30, 90), 'direct'),
            ('response_emergency_handling_qt', '应急处理', (50, 150), 'direct'),
            ('comm_support_link_maintenance_qt', '链路维护', (40, 120), 'direct'),
            ('comm_support_service_activation_qt', '服务激活', (30, 100), 'direct'),
            ('comm_support_emergency_restoration_qt', '应急恢复', (60, 180), 'direct'),
            ('comm_attack_target_acquisition_qt', '目标获取', (80, 250), 'direct'),
            ('comm_attack_jamming_effectiveness_ql', '干扰效能', (100, 300), 'direct'),
            ('comm_attack_deception_signal_generation_ql', '欺骗信号生成', (90, 280), 'direct'),
            ('comm_defense_signal_interception_awareness_ql', '信号截获感知', (70, 200), 'direct'),
            ('comm_defense_anti_jamming_operation_ql', '抗干扰操作', (80, 250), 'direct'),
            ('comm_defense_anti_deception_awareness_ql', '反欺骗感知', (70, 220), 'direct'),
            ('system_performance_connectivity_rate_qt', '连通率', (60, 180), 'direct'),
            ('system_performance_mission_reliability_qt', '任务可靠性', (80, 250), 'direct'),
            ('maintenance_feedback_field_repair_qt', '现场维修', (40, 120), 'direct'),
            ('maintenance_feedback_rework_rate_qt', '返工率', (20, 80), 'inverse'),
            ('maintenance_feedback_equipment_feedback_ql', '装备反馈', (30, 100), 'direct'),
        ]

        cursor = self.connection.cursor()

        # 清空旧数据
        cursor.execute("DELETE FROM cost_evaluation")
        self.connection.commit()

        model_id = "COST-2026-001"

        insert_query = """
            INSERT INTO cost_evaluation (
                model_id, indicator_key, indicator_name, unit, price_min, price_max,
                relation, noise_ratio, remarks
            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
        """

        for indicator_key, indicator_name, price_range, relation in indicators:
            price_min, price_max = price_range
            noise_ratio = 0.05
            remark = f"得分越高，{'价格越高' if relation == 'direct' else '价格越低'}（{price_min}-{price_max}万元）"

            values = (model_id, indicator_key, indicator_name, '万元', price_min, price_max, relation, noise_ratio, remark)
            cursor.execute(insert_query, values)
            print(f"  - {indicator_name}: {price_min}-{price_max}万元 ({'正相关' if relation == 'direct' else '负相关'})")

        self.connection.commit()
        cursor.close()
        print(f"  >> 成功生成 {len(indicators)} 个成本评估指标")

    # ==================== 主函数 ====================
    def generate_all(self):
        """生成所有数据"""
        print("\n" + "=" * 80)
        print("         军事作战效能评估数据生成工具 - 统一版本")
        print("=" * 80)

        try:
            # 1. 生成专家可信度数据
            self.generate_expert_credibility_data()

            # 2. 生成AHP权重数据
            self.generate_ahp_weights_data()

            # 3. 生成装备操作评分数据
            self.generate_equipment_scores_data()

            # 4. 生成成本评估数据
            self.generate_cost_evaluation_data()

            print("\n" + "=" * 80)
            print("         所有数据生成完成！")
            print("=" * 80)

        except Error as e:
            print(f"\n数据库错误: {e}")
            self.connection.rollback()
        finally:
            self.close()
            print("\n数据库连接已关闭")


def main():
    """主入口"""
    print("""
    ╔════════════════════════════════════════════════════════════════════════════╗
    ║           军事作战效能评估数据生成工具 - 统一版本                        ║
    ╠════════════════════════════════════════════════════════════════════════════╣
    ║  功能说明：                                                              ║
    ║  1. 生成10位专家的可信度评估数据                                        ║
    ║  2. 生成AHP权重评分数据                                                ║
    ║  3. 生成10个实验的装备操作评分数据                                      ║
    ║  4. 生成成本评估参数数据                                                ║
    ╚════════════════════════════════════════════════════════════════════════════╝
    """)

    generator = DataGenerator()
    generator.generate_all()


if __name__ == "__main__":
    main()
