# -*- coding: utf-8 -*-
"""
军事作战效能评估数据生成工具 - 统一版本
整合所有数据生成功能到一个脚本中
"""
import json
import os
import mysql.connector
from mysql.connector import Error
from datetime import datetime, timedelta, date
import random
import numpy as np


def get_db_config():
    """从环境变量获取数据库配置，兼容旧代码"""
    return {
        'host': os.environ.get('DB_HOST', 'localhost'),
        'port': int(os.environ.get('DB_PORT', '3306')),
        'database': os.environ.get('DB_NAME', 'military_operational_effectiveness_evaluation'),
        'user': os.environ.get('DB_USER', 'root'),
        'password': os.environ.get('DB_PASS', 'root')
    }


class DataGenerator:
    """统一的数据生成器"""

    # ── 全局字段 override 注册表 ──────────────────────────────────────────────
    # 格式: field_name -> (group_key, default_quality_ranges, default_dispersion_factor_key)
    # group_key: 1=网络连通, 2=通信质量, 3=人员装备, 4=链路维护, 5=能耗后勤, 6=安全
    OVERRIDE_FIELD_META = {
        # G1 网络连通
        'isolated_node_count':           (1, {'high': (0, 1),   'medium': (1, 4),   'low': (3, 8)},    None),
        'damaged_equipment_count':       (1, {'high': (0, 2),   'medium': (2, 8),   'low': (5, 15)},   None),
        'avg_network_setup_time_ms':     (1, {'high': (500, 1500), 'medium': (500, 3000), 'low': (1500, 5000)}, 'ms'),
        'electromagnetic_interference_level': (1, {'high': (1, 3), 'medium': (1, 9), 'low': (5, 9)}, None),
        'total_node_count':              (1, None, None),          # 只走全局离散
        'actual_connections':            (1, None, None),
        'total_equipment_count':         (1, None, None),
        # G2 通信质量
        'call_success_rate':             (2, {'high': (0.90, 0.99), 'medium': (0.70, 0.90), 'low': (0.40, 0.72)}, None),
        'trans_delay_ms':                (2, {'high': (5, 50),   'medium': (30, 150),  'low': (100, 500)}, None),
        'bandwidth_hz':                   (2, {'high': (5e6, 20e6), 'medium': (1e6, 8e6), 'low': (0.1e6, 2e6)}, None),
        'snr_db':                        (2, {'high': (20, 40),  'medium': (10, 25),   'low': (2, 15)},   None),
        'throughput_bps':                (2, None, None),
        'call_setup_ms':                 (2, None, None),
        'comm_success_rate':              (2, None, None),
        'detected_prob':                 (2, {'high': (0.0, 0.05), 'medium': (0.0, 0.30), 'low': (0.10, 0.50)}, None),
        'intercepted_prob':              (2, None, None),
        # G3 人员装备
        'avg_experience_years':         (3, {'high': (8.0, 15.0), 'medium': (4.0, 8.0), 'low': (1.0, 5.0)}, None),
        'avg_training_frequency_per_year': (3, {'high': (12.0, 20.0), 'medium': (6.0, 12.0), 'low': (2.0, 7.0)}, None),
        'new_equipment_ratio':          (3, {'high': (0.80, 0.98), 'medium': (0.50, 0.80), 'low': (0.20, 0.55)}, None),
        'spare_parts_satisfaction_rate': (3, {'high': (0.90, 0.99), 'medium': (0.70, 0.90), 'low': (0.50, 0.72)}, None),
        'command_personnel_count':       (3, None, None),
        'operator_personnel_count':      (3, None, None),
        'maintenance_personnel_count':   (3, None, None),
        'annual_maintenance_hours':      (3, None, None),
        # G4 链路维护
        'interruption_count':            (4, {'high': (1, 4),    'medium': (3, 8),    'low': (6, 15)},    None),
        'recovery_duration_ms':          (4, {'high': (500, 3000),  'medium': (2000, 8000),  'low': (5000, 20000)}, None),
        'recovery_success_rate':         (4, None, None),
        'maintenance_required_rate':     (4, None, None),
        'maintenance_success_rate':      (4, None, None),
        # G5 能耗后勤
        'total_power_consumption_kw':   (5, None, None),
        'annual_electricity_consumption_kwh': (5, None, None),
        'annual_fuel_consumption_liters': (5, None, None),
        'total_transport_distance_km':  (5, None, None),
        'spectrum_reserve_mhz':         (5, None, None),
        # G6 安全
        'security_event_count':           (6, {'high': (0, 3),   'medium': (2, 8),   'low': (5, 15)},    None),
        'impact_level_distribution':     (6, None, None),
    }

    def __init__(self):
        db_config = get_db_config()
        print(f"[INFO] 连接数据库: {db_config['host']}:{db_config['port']}/{db_config['database']}")
        self.connection = mysql.connector.connect(**db_config)
        self.expert_names = [
            '张军', '李建国', '王海峰', '刘芳', '陈伟',
            '赵敏', '孙强', '周婷', '吴磊', '郑雪'
        ]
        self._overrides = {}
        self._enum_overrides = {}

    def close(self):
        if self.connection and self.connection.is_connected():
            self.connection.close()

    # ── override 辅助 ────────────────────────────────────────────────────────
    def _get_override_range(self, field: str):
        """从 overrides 中取 {min, max}；支持键 '列名' 或 '表名.列名'（后者在后端与前端统一）"""
        if field in self._overrides:
            return self._overrides[field]
        dotted = '.' + field
        hits = [k for k in self._overrides if k.endswith(dotted)]
        if len(hits) == 1:
            return self._overrides[hits[0]]
        return None

    def _resolve_range(self, field: str, quality: str):
        """
        优先级: overrides > quality_ranges > 全局硬编码默认
        返回 (lo, hi)
        """
        ov = self._get_override_range(field)
        if ov is not None:
            return ov['min'], ov['max']

        meta = self.OVERRIDE_FIELD_META.get(field)
        if meta is None:
            return None
        # 元组格式: (group_key, quality_ranges_dict, dispersion_factor_key)
        _, q_ranges, _ = meta
        if q_ranges is None:
            return None
        return q_ranges.get(quality, q_ranges.get('medium'))

    def _rng(self, lo, hi, dispersion):
        """根据离散因子在 [lo, hi] 范围内采样"""
        df = {'high': 1.4, 'medium': 1.0, 'low': 0.65}.get(dispersion, 1.0)
        lo2 = lo * df
        hi2 = hi / df if df < 1 else hi * df
        return random.uniform(lo2, hi2)

    def _rng_comm(self, lo, hi, dispersion):
        """通信表专用的离散因子（与作战基础不同步）"""
        df = {'high': 0.6, 'medium': 1.0, 'low': 1.5}.get(dispersion, 1.0)
        lo2 = lo * df
        hi2 = hi / df if df < 1 else hi * df
        return random.uniform(lo2, hi2)

    def _rng_sec(self, lo, hi, dispersion):
        """安全表专用的离散因子"""
        df = {'high': 0.4, 'medium': 1.0, 'low': 1.6}.get(dispersion, 1.0)
        lo2 = lo * df
        hi2 = hi / df if df < 1 else hi * df
        return random.uniform(lo2, hi2)

    def _pick_enum(self, table: str, col: str, choices: list):
        """枚举列固定值：键为 '表.列' 或 '列'；未配置时在 choices 内随机"""
        if not choices:
            return None
        for k in (f'{table}.{col}', col):
            if k not in self._enum_overrides:
                continue
            raw = self._enum_overrides[k]
            if raw is None:
                break
            s = str(raw).strip()
            if s == '':
                break
            if s in choices:
                return s
        return random.choice(choices)

    def _ov_bounds_only(self, table, col, default_lo, default_hi):
        """仅使用 overrides（支持 表.列），无元数据档位；无覆盖时返回默认区间"""
        for k in (f'{table}.{col}', col):
            if k in self._overrides:
                o = self._overrides[k]
                return float(o['min']), float(o['max'])
        return float(default_lo), float(default_hi)

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
        """生成成本评估数据（按实验 OP-2026-001 ~ OP-2026-010）"""
        print("\n" + "=" * 80)
        print("【4/4】生成成本评估参数数据")
        print("=" * 80)

        cursor = self.connection.cursor()

        # 清空旧数据（与其它步骤一致）
        cursor.execute("DELETE FROM cost_evaluation")
        self.connection.commit()

        insert_query = """
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

        # 随机种子：保证每次生成一致
        random.seed(2026)

        num_experiments = 10
        for i in range(num_experiments):
            op_num = i + 1
            operation_id = f"OP-2026-{op_num:03d}"

            # 评估时间：错开到不同小时，便于查看
            evaluation_time = datetime(2026, 3, 1, 10, 0, 0) + timedelta(hours=i)

            # 使用 op_num 派生种子，保证每条记录可复现且不同
            random.seed(1000 + op_num)

            # 生成成本数据（单位：万元；表字段 decimal(5,2)，最大 999.99）
            personnel_strategic = round(random.uniform(80, 150) + op_num * 2, 2)
            personnel_campaign = round(random.uniform(60, 120) + op_num * 1.5, 2)
            personnel_tactical = round(random.uniform(40, 80) + op_num * 1, 2)
            personnel_operators = round(random.uniform(50, 100) + op_num * 1.2, 2)
            personnel_campaign_maint = round(random.uniform(30, 60) + op_num * 0.8, 2)
            personnel_tactical_maint = round(random.uniform(20, 50) + op_num * 0.6, 2)
            personnel_unit_maint = round(random.uniform(15, 40) + op_num * 0.5, 2)

            equipment_procurement = round(random.uniform(200, 500) + op_num * 10, 2)
            equipment_depreciation = round(equipment_procurement * random.uniform(0.1, 0.2), 2)
            equipment_campaign_maint = round(random.uniform(50, 120) + op_num * 3, 2)

            energy_campaign = round(random.uniform(40, 100) + op_num * 2, 2)
            energy_tactical = round(random.uniform(30, 70) + op_num * 1.5, 2)
            energy_unit = round(random.uniform(20, 50) + op_num * 1, 2)

            logistics_spare = round(random.uniform(30, 80) + op_num * 2, 2)
            logistics_storage = round(random.uniform(20, 60) + op_num * 1.5, 2)
            logistics_forward = round(random.uniform(15, 50) + op_num * 1, 2)

            training_total = round(random.uniform(40, 100) + op_num * 3, 2)
            training_tactical = round(random.uniform(20, 60) + op_num * 1.5, 2)
            training_per_soldier = round(random.uniform(2, 8) + op_num * 0.1, 2)

            infra_base = round(random.uniform(80, 200) + op_num * 4, 2)
            infra_spectrum = round(random.uniform(10, 30) + op_num * 0.5, 2)

            remarks = f"军事通信装备效能评估成本数据 - 实验批次 {operation_id}"

            values = (
                operation_id, evaluation_time,
                personnel_strategic,
                personnel_campaign,
                personnel_tactical,
                personnel_operators,
                personnel_campaign_maint,
                personnel_tactical_maint,
                personnel_unit_maint,
                equipment_procurement,
                equipment_depreciation,
                equipment_campaign_maint,
                energy_campaign,
                energy_tactical,
                energy_unit,
                logistics_spare,
                logistics_storage,
                logistics_forward,
                training_total,
                training_tactical,
                training_per_soldier,
                infra_base,
                infra_spectrum,
                remarks,
            )

            cursor.execute(insert_query, values)
            print(f"  - 实验 {operation_id}: 成本数据已生成")

        self.connection.commit()
        cursor.close()
        print(f"  >> 成功生成 {num_experiments} 条成本评估数据")

    # ── G1: 作战基础信息 ──────────────────────────────────────────────────────
    def generate_records_operation_info(self, count=10, quality='medium', dispersion='medium',
                                        mode='overwrite', overrides=None):
        """生成 records_military_operation_info（作战基础信息）

        Args:
            count:      作战条数
            quality:    优秀程度  high / medium / low
            dispersion: 离散程度  high / medium / low
            mode:       'overwrite' 先 DELETE 再 INSERT；'append' 直接 INSERT
            overrides:  dict  字段名 -> {'min': float, 'max': float}
        """
        print("\n" + "=" * 80)
        print("【5/4】生成作战基础信息 (records_military_operation_info)")
        print("=" * 80)

        self._overrides = overrides or {}
        weathers = ['晴', '多云', '阴', '小雨', '大雾', '雷雨']
        notes_tpls = [
            '作战编号 {}，整体表现{}',
            '批次 {} 的{}记录',
            '军事行动 {} 的{}评估',
        ]
        quality_labels = {'high': '优秀', 'medium': '良好', 'low': '一般'}

        cursor = self.connection.cursor()
        if mode == 'overwrite':
            cursor.execute("DELETE FROM records_military_operation_info")
            self.connection.commit()
            print("  [覆盖模式] 已清空旧数据")

        insert = """
            INSERT INTO records_military_operation_info (
                operation_id, avg_network_setup_time_ms, total_node_count, isolated_node_count,
                actual_connections, command_personnel_count, operator_personnel_count,
                maintenance_personnel_count, avg_experience_years, annual_maintenance_hours,
                avg_training_frequency_per_year, total_equipment_count, damaged_equipment_count,
                new_equipment_ratio, total_power_consumption_kw, annual_electricity_consumption_kwh,
                annual_fuel_consumption_liters, spectrum_reserve_mhz, spare_parts_satisfaction_rate,
                total_transport_distance_km, avg_altitude_m, weather_condition,
                temperature_celsius, electromagnetic_interference_level, notes
            ) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
        """

        To = 'records_military_operation_info'
        for i in range(count):
            op_id = 20260001 + i

            # ── 关键字段：优先用 overrides ─────────────────────────────────
            # 孤立节点数
            r = self._resolve_range('isolated_node_count', quality)
            iso_nodes = int(self._rng(*r, dispersion)) if r else int(self._rng(1, 4, dispersion))

            # 战损装备数
            r = self._resolve_range('damaged_equipment_count', quality)
            dmg_eq = int(self._rng(*r, dispersion)) if r else int(self._rng(2, 8, dispersion))

            # 新装备占比
            r = self._resolve_range('new_equipment_ratio', quality)
            new_eq_ratio = round(self._rng(*r, dispersion), 4) if r else round(self._rng(0.5, 0.8, dispersion), 4)

            # 备件满足率
            r = self._resolve_range('spare_parts_satisfaction_rate', quality)
            spare_rate = round(self._rng(*r, dispersion), 4) if r else round(self._rng(0.7, 0.9, dispersion), 4)

            # 平均经验年限
            r = self._resolve_range('avg_experience_years', quality)
            avg_exp = round(self._rng(*r, dispersion), 1) if r else round(self._rng(4.0, 8.0, dispersion), 1)

            # 年培训频次
            r = self._resolve_range('avg_training_frequency_per_year', quality)
            training_freq = round(self._rng(*r, dispersion), 2) if r else round(self._rng(6.0, 12.0, dispersion), 2)

            # 建网时间（ms）
            r = self._resolve_range('avg_network_setup_time_ms', quality)
            net_setup_ms = int(self._rng(*r, dispersion)) if r else int(self._rng(500, 3000, dispersion))

            # 电磁干扰等级
            r = self._resolve_range('electromagnetic_interference_level', quality)
            emi = int(self._rng(*r, dispersion)) if r else int(self._rng(1, 9, dispersion))

            # 人员与设备（支持 表.列 overrides）
            total_nodes = int(self._rng(*self._ov_bounds_only(To, 'total_node_count', 20, 60), dispersion))
            lo_ac, hi_ac = max(1, int(total_nodes * 1.5)), max(2, int(total_nodes * 3))
            actual_conn = int(self._rng(*self._ov_bounds_only(To, 'actual_connections', lo_ac, hi_ac), dispersion))
            total_eq = int(self._rng(*self._ov_bounds_only(To, 'total_equipment_count', 50, 200), dispersion))
            cmd_cnt = int(self._rng(*self._ov_bounds_only(To, 'command_personnel_count', 10, 30), dispersion))
            op_cnt = int(self._rng(*self._ov_bounds_only(To, 'operator_personnel_count', 30, 80), dispersion))
            maint_cnt = int(self._rng(*self._ov_bounds_only(To, 'maintenance_personnel_count', 5, 20), dispersion))
            maint_hrs = int(self._rng(*self._ov_bounds_only(To, 'annual_maintenance_hours', 200, 1000), dispersion))
            # 能耗后勤（可通过 overrides 指定功耗/油耗范围）
            r = self._resolve_range('total_power_consumption_kw', quality)
            power_kw = round(self._rng(*r, dispersion), 2) if r else round(self._rng(500, 5000, dispersion), 2)
            elec_kwh = round(self._rng(*self._ov_bounds_only(To, 'annual_electricity_consumption_kwh', 10000, 80000), dispersion), 2)
            r = self._resolve_range('annual_fuel_consumption_liters', quality)
            fuel_l = round(self._rng(*r, dispersion), 2) if r else round(self._rng(5000, 40000, dispersion), 2)
            spectrum = round(self._rng(*self._ov_bounds_only(To, 'spectrum_reserve_mhz', 10, 200), dispersion), 2)
            trans_km = round(self._rng(*self._ov_bounds_only(To, 'total_transport_distance_km', 100, 2000), dispersion), 2)
            altitude = round(self._rng(*self._ov_bounds_only(To, 'avg_altitude_m', 0, 5000), dispersion), 2)

            weather = self._pick_enum(To, 'weather_condition', weathers)
            t_lo, t_hi = self._ov_bounds_only(To, 'temperature_celsius', 5, 35)
            temp = round(random.uniform(t_lo, t_hi), 1)
            notes = random.choice(notes_tpls).format(op_id, quality_labels.get(quality, '良好'))

            vals = (
                op_id, net_setup_ms, total_nodes, iso_nodes, actual_conn,
                cmd_cnt, op_cnt, maint_cnt,
                avg_exp, maint_hrs,
                training_freq,
                total_eq, dmg_eq,
                new_eq_ratio,
                power_kw, elec_kwh,
                fuel_l, spectrum,
                spare_rate,
                trans_km, altitude,
                weather, temp, emi, notes
            )
            cursor.execute(insert, vals)

        self.connection.commit()
        cursor.close()
        print(f"  >> 成功{'覆盖' if mode == 'overwrite' else '追加'}生成 {count} 条作战基础信息")

    # ── G2: 通信记录 ─────────────────────────────────────────────────────────
    def generate_records_communication_info(self, count=10, quality='medium', dispersion='medium',
                                             mode='overwrite', overrides=None):
        """生成 records_military_communication_info（通信记录）

        每条作战生成 5~15 条通信明细
        """
        print("\n" + "=" * 80)
        print("【6/4】生成通信记录 (records_military_communication_info)")
        print("=" * 80)

        self._overrides = overrides or {}

        cursor = self.connection.cursor(dictionary=True)
        cursor.execute("SELECT DISTINCT operation_id FROM records_military_operation_info ORDER BY operation_id")
        op_ids = [r['operation_id'] for r in cursor.fetchall()]
        cursor.close()

        if not op_ids:
            print("  未找到作战基础数据，请先运行 generate_records_operation_info()")
            return

        if mode == 'overwrite':
            cursor = self.connection.cursor()
            cursor.execute("DELETE FROM records_military_communication_info")
            self.connection.commit()
            cursor.close()
            print("  [覆盖模式] 已清空旧数据")

        insert = """
            INSERT INTO records_military_communication_info (
                operation_id, src_node_id, dst_node_id, comm_type,
                start_time_ms, end_time_ms, call_req_ms, call_resp_ms, call_setup_ms,
                call_success, msg_bytes, trans_delay_ms, bandwidth_hz, snr_db,
                throughput_bps, tx_power_dbm, rx_power_dbm, distance_km,
                total_bits, error_bits, packets_sent, packets_lost,
                fail_reason, retry_cnt, noise_power_dbm, jamming_power_dbm,
                sinr_db, jamming_margin_db, detected, intercepted,
                operator_id, operator_reaction_ms, op_success, comm_success, notes
            ) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
        """

        comm_types = ['语音', '数据', '视频', '短消息']
        node_ids   = [f'NODE-{i:02d}' for i in range(1, 21)]
        fail_reasons = ['信道忙', '干扰过强', '节点离线', '功率不足', '带宽不足']
        op_ids_list = ['OP-' + str(i) for i in range(1, 11)]

        # ── 关键字段：从 overrides / quality 解析 ────────────────────────────
        def _comm_range(field, default_ranges, dispersion):
            r = self._resolve_range(field, quality)
            if r:
                return r
            return default_ranges.get(quality, default_ranges['medium'])

        sr = _comm_range('call_success_rate', {'high': (0.90, 0.99), 'medium': (0.70, 0.90), 'low': (0.40, 0.72)}, dispersion)
        dr = _comm_range('trans_delay_ms', {'high': (5, 50), 'medium': (30, 150), 'low': (100, 500)}, dispersion)
        br = _comm_range('bandwidth_hz', {'high': (5e6, 20e6), 'medium': (1e6, 8e6), 'low': (0.1e6, 2e6)}, dispersion)
        nr = _comm_range('snr_db', {'high': (20, 40), 'medium': (10, 25), 'low': (2, 15)}, dispersion)

        # 被侦获 / 被截获概率
        det_range = self._resolve_range('detected_prob', quality)
        int_range = self._resolve_range('intercepted_prob', quality)

        total_rows = 0
        Tc = 'records_military_communication_info'
        for op_id in op_ids:
            rows = random.randint(5, 15)
            for _ in range(rows):
                cs_raw = self._enum_overrides.get(f'{Tc}.call_success') or self._enum_overrides.get('call_success')
                if cs_raw is not None and str(cs_raw).strip() in ('0', '1'):
                    success = str(cs_raw).strip() == '1'
                else:
                    success = random.random() < self._rng_comm(sr[0], sr[1], dispersion)
                start_ms = int(self._rng_comm(*self._ov_bounds_only(Tc, 'start_time_ms', 0, 3600000), dispersion))
                end_ms   = start_ms + int(self._rng_comm(*self._ov_bounds_only(Tc, 'end_time_ms', 500, 30000), dispersion)) if success else start_ms + int(self._rng_comm(*self._ov_bounds_only(Tc, 'end_time_ms', 100, 5000), dispersion))
                dist = round(self._rng_comm(*self._ov_bounds_only(Tc, 'distance_km', 0.5, 50), dispersion), 2)
                bw    = self._rng_comm(*br, dispersion)
                delay = self._rng_comm(*dr, dispersion) if success else self._rng_comm(dr[0] * 2, dr[1] * 3, dispersion)
                snr   = self._rng_comm(*nr, dispersion)
                sinr  = snr - random.uniform(5, 15) if snr > 10 else self._rng_comm(0, 10, dispersion)
                total_bits = int(self._rng_comm(*self._ov_bounds_only(Tc, 'total_bits', 10000, 1000000), dispersion))
                err_bits   = int(total_bits * self._rng_comm(0, 0.05, dispersion)) if success else int(total_bits * self._rng_comm(0.1, 0.5, dispersion))
                pk_sent    = int(self._rng_comm(*self._ov_bounds_only(Tc, 'packets_sent', 100, 5000), dispersion))
                pk_lost    = int(pk_sent * self._rng_comm(0, 0.03, dispersion)) if success else int(pk_sent * self._rng_comm(0.1, 0.4, dispersion))
                tx_pow = round(self._rng_comm(*self._ov_bounds_only(Tc, 'tx_power_dbm', 20, 40), dispersion), 2)
                rx_pow = round(tx_pow - self._rng_comm(60, 120, dispersion), 2)
                noise  = round(self._rng_comm(*self._ov_bounds_only(Tc, 'noise_power_dbm', -100, -80), dispersion), 2)
                jam    = round(self._rng_comm(*self._ov_bounds_only(Tc, 'jamming_power_dbm', -90, -60), dispersion), 2) if random.random() < 0.4 else -120.0

                det_o = self._enum_overrides.get(f'{Tc}.detected') or self._enum_overrides.get('detected')
                int_o = self._enum_overrides.get(f'{Tc}.intercepted') or self._enum_overrides.get('intercepted')
                if det_o is not None and str(det_o).strip() in ('0', '1'):
                    det = int(str(det_o).strip())
                else:
                    det_prob = det_range[1] if det_range else 0.3
                    det = 1 if random.random() < self._rng_comm(0, det_prob, dispersion) else 0
                if int_o is not None and str(int_o).strip() in ('0', '1'):
                    inte = int(str(int_o).strip())
                else:
                    int_prob = int_range[1] if int_range else 0.15
                    inte = 1 if random.random() < self._rng_comm(0, int_prob, dispersion) else 0

                src_n = self._pick_enum(Tc, 'src_node_id', node_ids)
                dst_n = self._pick_enum(Tc, 'dst_node_id', node_ids)
                if dst_n == src_n and len(node_ids) > 1:
                    dst_n = random.choice([n for n in node_ids if n != src_n])
                ct = self._pick_enum(Tc, 'comm_type', comm_types)

                op_succ_o = self._enum_overrides.get(f'{Tc}.op_success') or self._enum_overrides.get('op_success')
                op_succ_v = int(str(op_succ_o).strip()) if op_succ_o is not None and str(op_succ_o).strip() in ('0', '1') else (1 if random.random() < 0.85 else 0)
                comm_succ_o = self._enum_overrides.get(f'{Tc}.comm_success') or self._enum_overrides.get('comm_success')
                comm_succ_v = int(str(comm_succ_o).strip()) if comm_succ_o is not None and str(comm_succ_o).strip() in ('0', '1') else (1 if success else 0)

                vals = (
                    op_id,
                    src_n, dst_n,
                    ct,
                    start_ms, end_ms,
                    int(self._rng_comm(*self._ov_bounds_only(Tc, 'call_req_ms', 10, 200), dispersion)),
                    int(self._rng_comm(*self._ov_bounds_only(Tc, 'call_resp_ms', 10, 200), dispersion)),
                    round(self._rng_comm(*self._ov_bounds_only(Tc, 'call_setup_ms', 50, 500), dispersion), 3),
                    1 if success else 0,
                    int(self._rng_comm(*self._ov_bounds_only(Tc, 'msg_bytes', 1000, 500000), dispersion)),
                    round(delay, 3),
                    round(bw, 2),
                    round(snr, 2),
                    round(bw * self._rng_comm(0.3, 0.9, dispersion), 2),
                    tx_pow, rx_pow, dist,
                    total_bits, err_bits,
                    pk_sent, pk_lost,
                    (self._pick_enum(Tc, 'fail_reason', fail_reasons) if not success else None),
                    int(self._rng_comm(*self._ov_bounds_only(Tc, 'retry_cnt', 0, 3), dispersion)),
                    noise, jam,
                    round(sinr, 2), round(self._rng_comm(*self._ov_bounds_only(Tc, 'jamming_margin_db', 3, 20), dispersion), 2),
                    det, inte,
                    self._pick_enum(Tc, 'operator_id', op_ids_list),
                    round(self._rng_comm(*self._ov_bounds_only(Tc, 'operator_reaction_ms', 100, 2000), dispersion), 2),
                    op_succ_v,
                    comm_succ_v,
                    None
                )
                cursor = self.connection.cursor()
                cursor.execute(insert, vals)
                total_rows += 1

        self.connection.commit()
        cursor.close()
        print(f"  >> 成功{'覆盖' if mode == 'overwrite' else '追加'}生成 {total_rows} 条通信记录（{len(op_ids)} 次作战）")

    # ── G4: 链路维护事件 ──────────────────────────────────────────────────────
    def generate_records_link_maintenance(self, count=10, quality='medium', dispersion='medium',
                                           mode='overwrite', overrides=None):
        """生成 records_link_maintenance_events（链路维护事件）"""
        print("\n" + "=" * 80)
        print("【7/4】生成链路维护事件 (records_link_maintenance_events)")
        print("=" * 80)

        self._overrides = overrides or {}

        cursor = self.connection.cursor(dictionary=True)
        cursor.execute("SELECT DISTINCT operation_id FROM records_military_operation_info ORDER BY operation_id")
        op_ids = [r['operation_id'] for r in cursor.fetchall()]
        cursor.close()

        if not op_ids:
            print("  未找到作战基础数据")
            return

        if mode == 'overwrite':
            cursor = self.connection.cursor()
            cursor.execute("DELETE FROM records_link_maintenance_events")
            self.connection.commit()
            cursor.close()
            print("  [覆盖模式] 已清空旧数据")

        # ── 关键字段 ────────────────────────────────────────────────────────
        def _link_range(field, default_ranges):
            r = self._resolve_range(field, quality)
            return r if r else default_ranges.get(quality, default_ranges['medium'])

        ir = _link_range('interruption_count', {'high': (1, 4), 'medium': (3, 8), 'low': (6, 15)})
        rcr = _link_range('recovery_duration_ms', {'high': (500, 3000), 'medium': (2000, 8000), 'low': (5000, 20000)})

        insert = """
            INSERT INTO records_link_maintenance_events (
                event_id, operation_id, source_node, target_node, equipment_id,
                is_critical_link, interruption_start_ms, interruption_end_ms,
                interruption_reason, interruption_type,
                recovery_start_ms, recovery_end_ms, recovery_duration_ms,
                recovery_method, recovery_success, maintenance_required,
                maintenance_start_ms, maintenance_end_ms, maintenance_duration_ms,
                maintenance_success, failure_reason, repair_method,
                operator_id, feedback_content, feedback_submitted, notes
            ) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
        """

        nodes  = [f'NODE-{i:02d}' for i in range(1, 21)]
        equip  = [f'EQ-{i:03d}' for i in range(1, 51)]
        ops    = [f'OP-{i:02d}' for i in range(1, 21)]
        intr_rs = ['节点宕机', '光纤断裂', '电磁干扰', '电源故障', '配置错误', '链路拥塞']
        intr_ts = ['hardware_fault', 'software_fault', 'external_attack', 'maintenance', 'crash']
        rec_ms  = ['自动切换', '人工抢修', '备用链路启用', '重启恢复']
        fail_rs = ['元器件老化', '外力破坏', '过载运行', '配置失误']
        rep_ms  = ['更换模块', '重新配置', '光纤熔接', '电源修复', '重启设备']

        Tl = 'records_link_maintenance_events'
        total_rows = 0
        event_counter = 1
        for op_id in op_ids:
            n_events = int(self._rng_comm(*ir, dispersion))
            for _ in range(n_events):
                src = self._pick_enum(Tl, 'source_node', nodes)
                dst = self._pick_enum(Tl, 'target_node', nodes)
                while dst == src and len(nodes) > 1:
                    dst = random.choice([n for n in nodes if n != src])
                crit_o = self._enum_overrides.get(f'{Tl}.is_critical_link') or self._enum_overrides.get('is_critical_link')
                crit = int(str(crit_o).strip()) if crit_o is not None and str(crit_o).strip() in ('0', '1') else (1 if random.random() < 0.3 else 0)
                istart = int(self._rng_comm(0, 3600000, dispersion))
                iend   = istart + int(self._rng_comm(1000, 30000, dispersion))
                iend   = max(iend, istart + 500)
                rstart = iend + int(self._rng_comm(100, 5000, dispersion))
                rend   = rstart + int(self._rng_comm(*rcr, dispersion))
                rsucc_o = self._enum_overrides.get(f'{Tl}.recovery_success') or self._enum_overrides.get('recovery_success')
                rsucc = int(str(rsucc_o).strip()) if rsucc_o is not None and str(rsucc_o).strip() in ('0', '1') else (1 if random.random() < self._rng_comm(0.85, 0.98, dispersion) else 0)
                mreq_o = self._enum_overrides.get(f'{Tl}.maintenance_required') or self._enum_overrides.get('maintenance_required')
                mreq = int(str(mreq_o).strip()) if mreq_o is not None and str(mreq_o).strip() in ('0', '1') else (1 if random.random() < self._rng_comm(0.2, 0.6, dispersion) else 0)
                msucc_o = self._enum_overrides.get(f'{Tl}.maintenance_success') or self._enum_overrides.get('maintenance_success')
                msucc = int(str(msucc_o).strip()) if msucc_o is not None and str(msucc_o).strip() in ('0', '1') else (1 if mreq and random.random() < self._rng_comm(0.75, 0.95, dispersion) else 0)
                fb_o = self._enum_overrides.get(f'{Tl}.feedback_submitted') or self._enum_overrides.get('feedback_submitted')
                if fb_o is not None and str(fb_o).strip() in ('0', '1'):
                    fb_sub = int(str(fb_o).strip())
                else:
                    fb_sub = 0 if random.random() < 0.4 else 1

                vals = (
                    event_counter, op_id, src, dst, self._pick_enum(Tl, 'equipment_id', equip),
                    crit, istart, iend,
                    self._pick_enum(Tl, 'interruption_reason', intr_rs), self._pick_enum(Tl, 'interruption_type', intr_ts),
                    rstart, rend, rend - rstart,
                    self._pick_enum(Tl, 'recovery_method', rec_ms), rsucc, mreq,
                    rend + int(self._rng_comm(500, 10000, dispersion)) if mreq else None,
                    None, None, msucc,
                    self._pick_enum(Tl, 'failure_reason', fail_rs) if not rsucc else None,
                    self._pick_enum(Tl, 'repair_method', rep_ms) if not msucc else None,
                    self._pick_enum(Tl, 'operator_id', ops),
                    None, fb_sub, None
                )
                cursor = self.connection.cursor()
                cursor.execute(insert, vals)
                total_rows += 1
                event_counter += 1

        self.connection.commit()
        cursor.close()
        print(f"  >> 成功{'覆盖' if mode == 'overwrite' else '追加'}生成 {total_rows} 条链路维护事件")

    # ── G6: 安全事件 ──────────────────────────────────────────────────────────
    def generate_records_security_events(self, count=10, quality='medium', dispersion='medium',
                                         mode='overwrite', overrides=None):
        """生成 records_security_events（安全事件）"""
        print("\n" + "=" * 80)
        print("【8/4】生成安全事件 (records_security_events)")
        print("=" * 80)

        self._overrides = overrides or {}

        cursor = self.connection.cursor(dictionary=True)
        cursor.execute("SELECT DISTINCT operation_id FROM records_military_operation_info ORDER BY operation_id")
        op_ids = [r['operation_id'] for r in cursor.fetchall()]
        cursor.close()

        if not op_ids:
            print("  未找到作战基础数据")
            return

        if mode == 'overwrite':
            cursor = self.connection.cursor()
            cursor.execute("DELETE FROM records_security_events")
            self.connection.commit()
            cursor.close()
            print("  [覆盖模式] 已清空旧数据")

        # ── 关键字段 ────────────────────────────────────────────────────────
        nr = self._resolve_range('security_event_count', quality)
        if nr is None:
            nr = {'high': (0, 3), 'medium': (2, 8), 'low': (5, 15)}.get(quality, (2, 8))

        insert = """
            INSERT INTO records_security_events (
                operation_id, event_type, event_time_ms, node_id, key_id,
                key_age_ms, detected_by, intercepted_content, intercept_method,
                interception_attempt_source, impact_level, notes
            ) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
        """

        event_types = ['key_leak', 'detected_by_enemy', 'intercepted', 'interception_attempt']
        nodes       = [f'NODE-{i:02d}' for i in range(1, 21)]
        keys        = [f'KEY-{i:04d}' for i in range(1, 101)]
        det_by      = ['敌方侦察系统', '电子对抗单元', '信号截获设备', '人工情报']
        int_methods = ['频率分析', '相关检测', '深度包检测', '侧信道攻击']
        int_sources  = ['敌方情报站', '电子侦察机', '网络攻击平台', '无线电监测站']
        impacts      = ['轻微', '一般', '严重', '危急']

        Ts = 'records_security_events'
        total_rows = 0
        for op_id in op_ids:
            n_events = int(self._rng_sec(*nr, dispersion))
            for _ in range(n_events):
                etype = self._pick_enum(Ts, 'event_type', event_types)
                node = self._pick_enum(Ts, 'node_id', nodes)
                keyid = self._pick_enum(Ts, 'key_id', keys)

                if etype in ('detected_by_enemy', 'intercepted'):
                    db = self._pick_enum(Ts, 'detected_by', det_by)
                else:
                    db = None
                if etype == 'intercepted':
                    ic_raw = self._pick_enum(Ts, 'intercepted_content', ['通信内容...', '（空）'])
                    ic_val = None if ic_raw == '（空）' else ic_raw
                else:
                    ic_val = None
                if etype in ('intercepted', 'interception_attempt'):
                    im = self._pick_enum(Ts, 'intercept_method', int_methods)
                else:
                    im = None
                if etype == 'interception_attempt':
                    isrc = self._pick_enum(Ts, 'interception_attempt_source', int_sources)
                else:
                    isrc = None
                imp = self._pick_enum(Ts, 'impact_level', impacts)

                vals = (
                    op_id, etype,
                    int(self._rng_sec(0, 3600000, dispersion)),
                    node, keyid,
                    int(self._rng_sec(10000, 86400000, dispersion)),
                    db,
                    ic_val,
                    im,
                    isrc,
                    imp,
                    None
                )
                cursor = self.connection.cursor()
                cursor.execute(insert, vals)
                total_rows += 1

        self.connection.commit()
        cursor.close()
        print(f"  >> 成功{'覆盖' if mode == 'overwrite' else '追加'}生成 {total_rows} 条安全事件")

    # ==================== 主函数 ====================
    def generate_all(self, records_count=10, quality='medium', dispersion='medium',
                     mode='overwrite', overrides=None, enum_overrides=None):
        """生成所有数据

        Args:
            records_count: 作战条数（影响 records_* 四表）
            quality:       优秀程度  high / medium / low
            dispersion:    离散程度  high / medium / low
            mode:          'overwrite' / 'append'
            overrides:     dict 字段名 -> {'min': float, 'max': float}
            enum_overrides: dict 表.列 或 列 -> 枚举固定值
        """
        print("\n" + "=" * 80)
        print("         军事作战效能评估数据生成工具 - 统一版本")
        print("=" * 80)

        try:
            self.generate_expert_credibility_data()
            self.generate_ahp_weights_data()
            self.generate_equipment_scores_data()
            self.generate_cost_evaluation_data()

            self._enum_overrides = dict(enum_overrides) if enum_overrides else {}
            self.generate_records_operation_info(records_count, quality, dispersion, mode, overrides)
            self.generate_records_communication_info(records_count, quality, dispersion, mode, overrides)
            self.generate_records_link_maintenance(records_count, quality, dispersion, mode, overrides)
            self.generate_records_security_events(records_count, quality, dispersion, mode, overrides)
            self.generate_records_comm_attack_operation(records_count, quality, dispersion, mode)
            self.generate_records_comm_defense_operation(records_count, quality, dispersion, mode)

            print("\n" + "=" * 80)
            print("         所有数据生成完成！")
            print("  （包含：专家可信度、AHP权重、装备评分、成本评估、")
            print("         records_*六表含进攻/防御操作记录）")
            print("=" * 80)

        except Error as e:
            print(f"\n数据库错误: {e}")
            self.connection.rollback()
        finally:
            self.close()
            print("\n数据库连接已关闭")

    def generate_records_only(self, records_count=10, quality='medium', dispersion='medium',
                                seed=None, mode='overwrite', overrides=None, enum_overrides=None):
        """仅生成 records_* 四表（供页面「生成模拟数据」调用）

        Args:
            mode:      'overwrite' / 'append'
            overrides: dict 字段名或 表.列 -> {'min': float, 'max': float}
            enum_overrides: dict 表.列 或 列 -> 枚举固定值
        """
        if seed is not None and str(seed).strip() != '':
            try:
                random.seed(int(str(seed).strip()))
            except ValueError:
                random.seed(str(seed).strip())
        self._enum_overrides = dict(enum_overrides) if enum_overrides else {}
        print("\n" + "=" * 80)
        print("         仅生成作战模拟四表 (records_*)")
        print("=" * 80)
        try:
            self.generate_records_operation_info(records_count, quality, dispersion, mode, overrides)
            self.generate_records_communication_info(records_count, quality, dispersion, mode, overrides)
            self.generate_records_link_maintenance(records_count, quality, dispersion, mode, overrides)
            self.generate_records_security_events(records_count, quality, dispersion, mode, overrides)
            self.generate_records_comm_attack_operation(records_count, quality, dispersion, mode)
            self.generate_records_comm_defense_operation(records_count, quality, dispersion, mode)
            print("\n" + "=" * 80)
            print("         作战模拟四表及攻防操作记录生成完成！")
            print("=" * 80)
        except Error as e:
            print(f"\n数据库错误: {e}")
            self.connection.rollback()
            raise
        finally:
            self.close()
            print("\n数据库连接已关闭")

    # ── 进攻操作记录 ────────────────────────────────────────────────────────────
    def generate_records_comm_attack_operation(self, count=10, quality='medium', dispersion='medium', mode='overwrite'):
        """生成 records_comm_attack_operation（进攻操作记录）

        operation_type: jamming_target_lock / jamming_effect / spoofing_signal
        每条作战生成 3~8 条进攻操作
        """
        print("\n" + "=" * 80)
        print("【进攻】生成进攻操作记录 (records_comm_attack_operation)")
        print("=" * 80)

        cursor = self.connection.cursor(dictionary=True)
        cursor.execute("SELECT DISTINCT operation_id FROM records_military_operation_info ORDER BY operation_id")
        op_ids = [r['operation_id'] for r in cursor.fetchall()]
        cursor.close()

        if not op_ids:
            print("  未找到作战基础数据，请先运行 generate_records_operation_info()")
            return

        if mode == 'overwrite':
            cursor = self.connection.cursor()
            cursor.execute("DELETE FROM records_comm_attack_operation")
            self.connection.commit()
            cursor.close()
            print("  [覆盖模式] 已清空旧数据")

        insert = """
            INSERT INTO records_comm_attack_operation (
                operation_id, operation_type, start_time_ms, end_time_ms,
                operator_id, target_node, target_communication_id,
                jamming_power_dbm, jamming_frequency_hz, effect_assessment,
                spoofing_signal_type, spoofing_success, notes
            ) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
        """

        op_types = ['jamming_target_lock', 'jamming_effect', 'spoofing_signal']
        op_id_list = [f'OP-{i:02d}' for i in range(1, 11)]
        target_nodes = [f'NODE-{i:02d}' for i in range(1, 21)]
        jam_powers = [round(random.uniform(-90, -60), 2) for _ in range(5)]
        jam_freqs = [round(random.uniform(100e6, 500e6), 2) for _ in range(5)]
        effect_texts = [
            '通信质量降级30%', '通信质量降级50%', '通信质量降级80%',
            '完全中断', '部分干扰', '信号压制', '目标失锁'
        ]
        spoof_types = ['敌方指挥', '虚假指令', '伪装友军', '错误频点']
        operators = [f'OPR-{i:02d}' for i in range(1, 11)]

        total_rows = 0
        for op_id in op_ids:
            rows = random.randint(3, 8)
            for _ in range(rows):
                op_type = random.choice(op_types)
                start_ms = random.randint(0, 3600000)

                if op_type == 'jamming_target_lock':
                    end_ms = start_ms + random.randint(500, 3000)
                    jamming_power = random.choice(jam_powers)
                    jamming_freq = random.choice(jam_freqs)
                    target_node = random.choice(target_nodes)
                    effect_assessment = random.choice(effect_texts) if random.random() < 0.6 else None
                    spoofing_signal_type = None
                    spoofing_success = None
                elif op_type == 'jamming_effect':
                    end_ms = start_ms + random.randint(1000, 10000)
                    jamming_power = random.choice(jam_powers)
                    jamming_freq = random.choice(jam_freqs)
                    target_node = random.choice(target_nodes)
                    effect_assessment = random.choice(effect_texts)
                    spoofing_signal_type = None
                    spoofing_success = None
                else:  # spoofing_signal
                    end_ms = start_ms + random.randint(2000, 8000)
                    jamming_power = None
                    jamming_freq = None
                    target_node = None
                    effect_assessment = None
                    spoofing_signal_type = random.choice(spoof_types)
                    spoofing_success = 1 if random.random() < 0.75 else 0

                operator_id = random.choice(operators)
                notes = random.choice(['正常执行', '目标变更', '功率调整', '频率切换', ''])

                cursor = self.connection.cursor()
                cursor.execute(insert, (
                    op_id, op_type, start_ms, end_ms,
                    operator_id, target_node, None,
                    jamming_power, jamming_freq, effect_assessment,
                    spoofing_signal_type, spoofing_success, notes
                ))
                total_rows += 1

        self.connection.commit()
        cursor.close()
        print(f"  >> 成功{'覆盖' if mode == 'overwrite' else '追加'}生成 {total_rows} 条进攻操作记录")

    # ── 防御操作记录 ────────────────────────────────────────────────────────────
    def generate_records_comm_defense_operation(self, count=10, quality='medium', dispersion='medium', mode='overwrite'):
        """生成 records_comm_defense_operation（防御操作记录）

        operation_type: detection_awareness / anti_jamming / anti_deception
        每条作战生成 3~8 条防御操作
        """
        print("\n" + "=" * 80)
        print("【防御】生成防御操作记录 (records_comm_defense_operation)")
        print("=" * 80)

        cursor = self.connection.cursor(dictionary=True)
        cursor.execute("SELECT DISTINCT operation_id FROM records_military_operation_info ORDER BY operation_id")
        op_ids = [r['operation_id'] for r in cursor.fetchall()]
        cursor.close()

        if not op_ids:
            print("  未找到作战基础数据，请先运行 generate_records_operation_info()")
            return

        if mode == 'overwrite':
            cursor = self.connection.cursor()
            cursor.execute("DELETE FROM records_comm_defense_operation")
            self.connection.commit()
            cursor.close()
            print("  [覆盖模式] 已清空旧数据")

        insert = """
            INSERT INTO records_comm_defense_operation (
                operation_id, operation_type, start_time_ms, end_time_ms,
                operator_id, detection_time_ms, detection_method,
                jamming_detected, anti_jamming_actions, anti_jamming_success,
                suspicious_signal_detected, verification_method, verification_result,
                notes
            ) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
        """

        op_types = ['detection_awareness', 'anti_jamming', 'anti_deception']
        operators = [f'OPR-{i:02d}' for i in range(1, 11)]
        detection_methods = ['仪表检测', '耳听', '告警', '目视', '协同感知']
        jam_actions = ['跳频', '扩频', '增功率', '切换频点', '启用备份链路', '改变调制方式']
        verify_methods = ['询问上级', '信号特征比对', '频谱分析', '交叉验证']
        verify_results = ['确认为敌', '误判', '存疑待查', '正常信号']

        total_rows = 0
        for op_id in op_ids:
            rows = random.randint(3, 8)
            for _ in range(rows):
                op_type = random.choice(op_types)
                start_ms = random.randint(0, 3600000)

                if op_type == 'detection_awareness':
                    end_ms = start_ms + random.randint(500, 5000)
                    detection_time_ms = random.randint(100, 2000)
                    detection_method = random.choice(detection_methods)
                    jamming_detected = None
                    anti_jamming_actions = None
                    anti_jamming_success = None
                    suspicious_signal_detected = None
                    verification_method = None
                    verification_result = None
                elif op_type == 'anti_jamming':
                    end_ms = start_ms + random.randint(1000, 8000)
                    detection_time_ms = random.randint(200, 3000)
                    detection_method = random.choice(detection_methods)
                    jamming_detected = 1
                    anti_jamming_actions = random.choice(jam_actions)
                    anti_jamming_success = 1 if random.random() < 0.80 else 0
                    suspicious_signal_detected = None
                    verification_method = None
                    verification_result = None
                else:  # anti_deception
                    end_ms = start_ms + random.randint(2000, 10000)
                    detection_time_ms = random.randint(500, 4000)
                    detection_method = random.choice(detection_methods)
                    jamming_detected = None
                    anti_jamming_actions = None
                    anti_jamming_success = None
                    suspicious_signal_detected = 1
                    verification_method = random.choice(verify_methods)
                    verification_result = random.choice(verify_results)
                    # 根据结果决定真假阳性
                    if random.random() < 0.70:
                        verification_result = '确认为敌'
                    else:
                        verification_result = random.choice(['误判', '存疑待查', '正常信号'])

                operator_id = random.choice(operators)
                notes = random.choice(['正常处置', '上报指挥', '协同处置', ''])

                cursor = self.connection.cursor()
                cursor.execute(insert, (
                    op_id, op_type, start_ms, end_ms,
                    operator_id, detection_time_ms, detection_method,
                    jamming_detected, anti_jamming_actions, anti_jamming_success,
                    suspicious_signal_detected, verification_method, verification_result,
                    notes
                ))
                total_rows += 1

        self.connection.commit()
        cursor.close()
        print(f"  >> 成功{'覆盖' if mode == 'overwrite' else '追加'}生成 {total_rows} 条防御操作记录")


def main():
    """主入口（支持命令行参数和环境变量）"""
    import argparse
    import os

    parser = argparse.ArgumentParser(description='军事作战效能评估数据生成工具')
    parser.add_argument('--count', type=int, default=10, help='作战条数（默认 10）')
    parser.add_argument('--quality', choices=['high', 'medium', 'low'], default='medium',
                        help='优秀程度：high / medium / low（默认 medium）')
    parser.add_argument('--dispersion', choices=['high', 'medium', 'low'], default='medium',
                        help='离散程度：high / medium / low（默认 medium）')
    parser.add_argument('--records-only', action='store_true',
                        help='仅生成 records_* 四表，不生成专家/AHP/装备评分/成本等')
    parser.add_argument('--seed', type=str, default='', help='随机种子（可选，整数或字符串）')
    parser.add_argument('--mode', choices=['overwrite', 'append'], default='overwrite',
                        help='写入模式：overwrite 先清空再写入，append 追加（默认 overwrite）')
    parser.add_argument('--overrides', type=str, default='',
                        help='字段范围覆盖，JSON 字符串，如 \'{"isolated_node_count":{"min":1,"max":4}}\'')
    parser.add_argument('--enum-overrides', type=str, default='',
                        help='枚举列固定值 JSON，如 \'{"records_military_operation_info.weather_condition":"晴"}\'')
    parser.add_argument('--overrides-env', action='store_true',
                        help='从环境变量 COMBAT_OVERRIDES 读取 overrides')
    parser.add_argument('--enum-overrides-env', action='store_true',
                        help='从环境变量 COMBAT_ENUM_OVERRIDES 读取 enumOverrides')
    args = parser.parse_args()

    quality_labels = {'high': '高', 'medium': '中', 'low': '低'}

    # 解析 overrides JSON（优先命令行参数，其次环境变量）
    overrides = None
    overrides_source = '无'
    if args.overrides.strip():
        try:
            overrides = json.loads(args.overrides)
            overrides_source = f'命令行（{len(overrides)} 个字段）'
            print(f"  [overrides] 从命令行加载 {len(overrides)} 个字段覆盖")
        except json.JSONDecodeError as e:
            print(f"  [ERROR] overrides JSON 解析失败: {e}")
            return
    elif args.overrides_env:
        # 从环境变量读取
        env_overrides = os.environ.get('COMBAT_OVERRIDES', '').strip()
        if env_overrides:
            try:
                overrides = json.loads(env_overrides)
                overrides_source = f'环境变量（{len(overrides)} 个字段）'
                print(f"  [overrides] 从环境变量 COMBAT_OVERRIDES 加载 {len(overrides)} 个字段覆盖")
            except json.JSONDecodeError as e:
                print(f"  [ERROR] COMBAT_OVERRIDES JSON 解析失败: {e}")
                return

    # 解析 enumOverrides JSON
    enum_overrides = None
    enum_source = '无'
    if args.enum_overrides.strip():
        try:
            enum_overrides = json.loads(args.enum_overrides)
            enum_source = f'命令行（{len(enum_overrides)} 个）'
            print(f"  [enum-overrides] 从命令行加载 {len(enum_overrides)} 个枚举固定值")
        except json.JSONDecodeError as e:
            print(f"  [ERROR] enum-overrides JSON 解析失败: {e}")
            return
    elif args.enum_overrides_env:
        # 从环境变量读取
        env_enum = os.environ.get('COMBAT_ENUM_OVERRIDES', '').strip()
        if env_enum:
            try:
                enum_overrides = json.loads(env_enum)
                enum_source = f'环境变量（{len(enum_overrides)} 个）'
                print(f"  [enum-overrides] 从环境变量 COMBAT_ENUM_OVERRIDES 加载 {len(enum_overrides)} 个枚举固定值")
            except json.JSONDecodeError as e:
                print(f"  [ERROR] COMBAT_ENUM_OVERRIDES JSON 解析失败: {e}")
                return

    mode_label = '覆盖' if args.mode == 'overwrite' else '追加'

    print(f"""
    ╔════════════════════════════════════════════════════════════════════════════╗
    ║           军事作战效能评估数据生成工具 - 统一版本                        ║
    ╠════════════════════════════════════════════════════════════════════════════╣
    ║  当前参数：                                                              ║
    ║    作战条数  = {args.count}                                                       ║
    ║    优秀程度  = {quality_labels[args.quality]}（{args.quality}）                                        ║
    ║    离散程度  = {quality_labels[args.dispersion]}（{args.dispersion}）                                        ║
    ║    写入模式  = {mode_label}（{args.mode}）                                        ║
    ║    字段覆盖  = {overrides_source}                                          ║
    ║    枚举固定  = {enum_source}                                          ║
    ╚════════════════════════════════════════════════════════════════════════════╝
    """)

    generator = DataGenerator()
    seed_val = args.seed.strip() if getattr(args, 'seed', None) else ''
    if args.records_only:
        generator.generate_records_only(
            records_count=args.count,
            quality=args.quality,
            dispersion=args.dispersion,
            seed=seed_val if seed_val else None,
            mode=args.mode,
            overrides=overrides,
            enum_overrides=enum_overrides,
        )
    else:
        generator.generate_all(
            records_count=args.count,
            quality=args.quality,
            dispersion=args.dispersion,
            mode=args.mode,
            overrides=overrides,
            enum_overrides=enum_overrides,
        )


if __name__ == "__main__":
    main()
