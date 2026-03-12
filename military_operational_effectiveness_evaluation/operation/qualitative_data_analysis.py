# -*- coding: utf-8 -*-
"""
专家定性数据集结与可视化模块
基于"综合方法的确定""覆盖频度与质心法"等文献
实现区间数 + 可信度加权的专家集结方法，并提供可视化功能

数据来源：
- expert_credibility_results: 专家可信度 (主观α、客观β)
- equipment_operation_qualitative_score: 专家定性打分数据 (等级+置信度)
"""

import json
import os
import math
from datetime import datetime
from typing import Dict, List, Tuple, Optional, Any
import pymysql
from pymysql.cursors import DictCursor
import matplotlib.pyplot as plt
import numpy as np
from matplotlib import rcParams

# 与 calculate_expert_credibility.py 一致：中文字体，避免乱码
rcParams['font.sans-serif'] = ['SimHei']  # 用来正常显示中文标签
rcParams['axes.unicode_minus'] = False     # 用来正常显示负号


# =====================================================
# 等级代码到分数区间的映射表（表 4-1）
# =====================================================
GRADE_TO_INTERVAL = {
    'A+': [95, 100],
    'A': [90, 95],
    'A-': [85, 90],
    'B+': [80, 85],
    'B': [75, 80],
    'B-': [70, 75],
    'C+': [65, 70],
    'C': [60, 65],
    'C-': [55, 60],
    'D+': [47, 55],
    'D': [39, 47],
    'D-': [30, 39],
    'E+': [20, 30],
    'E': [10, 20],
    'E-': [0, 10],
}

# 指标键名与中文名映射
INDICATOR_NAMES = {
    'maintenance_maintenance_skill': '维修技能',
    'comm_attack_jamming_effectiveness': '干扰效能',
    'comm_attack_deception_signal_generation': '欺骗信号生成',
    'comm_defense_signal_interception_awareness': '信号截获感知',
    'comm_defense_anti_jamming_operation': '抗干扰操作',
    'comm_defense_anti_deception_awareness': '反欺骗感知',
    'maintenance_feedback_equipment_feedback': '装备反馈'
}

# 简短的指标名（用于图表标签）
INDICATOR_NAMES_SHORT = {
    'maintenance_maintenance_skill': '维修技能',
    'comm_attack_jamming_effectiveness': '干扰效能',
    'comm_attack_deception_signal_generation': '欺骗信号',
    'comm_defense_signal_interception_awareness': '信号截获',
    'comm_defense_anti_jamming_operation': '抗干扰',
    'comm_defense_anti_deception_awareness': '反欺骗',
    'maintenance_feedback_equipment_feedback': '装备反馈'
}

# 指标类别
INDICATOR_CATEGORIES = {
    'maintenance_maintenance_skill': '维修保障',
    'comm_attack_jamming_effectiveness': '通信攻击',
    'comm_attack_deception_signal_generation': '通信攻击',
    'comm_defense_signal_interception_awareness': '通信防御',
    'comm_defense_anti_jamming_operation': '通信防御',
    'comm_defense_anti_deception_awareness': '通信防御',
    'maintenance_feedback_equipment_feedback': '维修保障'
}

# 颜色方案
COLORS = {
    '维修保障': '#2E86AB',
    '通信攻击': '#E94F37',
    '通信防御': '#44AF69',
    'default': '#6C757D'
}

# 指标键名列表（按顺序）
INDICATOR_KEYS = list(INDICATOR_NAMES.keys())

# 判断可信度阈值
CONFIDENCE_THRESHOLD = 0.6

# 综合判断可信度权重参数（可调整）
DEFAULT_MU = 0.3      # 主观可信度权重
DEFAULT_NU = 0.2      # 客观可信度权重
DEFAULT_ETA = 0.5     # 判断把握度权重

# 默认数据批次（不传参时使用，与基础数据表一致）
DEFAULT_OPERATION_ID = 'OP-2026-001'   # 定性评分表 equipment_operation_qualitative_score 的 operation_id
DEFAULT_BATCH_ID = 'AHP-2026-001'      # 专家可信度表 expert_credibility_results 的 batch_id


# =====================================================
# 数据集结类
# =====================================================

class QualitativeDataAggregation:
    """专家定性数据集结类"""

    def __init__(self, connection: pymysql.Connection):
        self.connection = connection

    def grade_to_interval(self, grade: str) -> Optional[Tuple[float, float]]:
        """
        将等级代码转换为区间数 [a1, a2]

        Args:
            grade: 等级代码，如 'A+', 'B', 'C-' 等

        Returns:
            区间 [a1, a2] 或 None（若等级无效）
        """
        if grade is None or grade == '':
            return None

        grade = grade.strip().upper()
        return GRADE_TO_INTERVAL.get(grade, None)

    def get_interval_midpoint(self, interval: Tuple[float, float]) -> float:
        """计算区间中点"""
        return (interval[0] + interval[1]) / 2

    def get_interval_width(self, interval: Tuple[float, float]) -> float:
        """计算区间宽度"""
        return interval[1] - interval[0]

    def load_expert_credibility(self, batch_id: str = None) -> Dict[str, Dict[str, float]]:
        """
        从 expert_credibility_results 加载专家可信度

        Args:
            batch_id: 批次ID，若为None则加载最新的

        Returns:
            字典 {expert_name: {'alpha': α, 'beta': β, 'comprehensive': 综合可信度}}
        """
        cursor = self.connection.cursor(DictCursor)

        if batch_id:
            query = """
                SELECT expert_name, subjective_credibility, objective_credibility,
                       comprehensive_credibility
                FROM expert_credibility_results
                WHERE batch_id = %s
            """
            cursor.execute(query, (batch_id,))
        else:
            # 获取最新的批次
            query = """
                SELECT expert_name, subjective_credibility, objective_credibility,
                       comprehensive_credibility
                FROM expert_credibility_results
                ORDER BY evaluation_date DESC, id DESC
            """
            cursor.execute(query)

        results = cursor.fetchall()
        cursor.close()

        credibility_dict = {}
        for row in results:
            expert_name = row['expert_name']
            credibility_dict[expert_name] = {
                'alpha': float(row['subjective_credibility']) if row['subjective_credibility'] else 0.5,
                'beta': float(row['objective_credibility']) if row['objective_credibility'] else 0.5,
                'comprehensive': float(row['comprehensive_credibility']) if row['comprehensive_credibility'] else 0.5
            }

        return credibility_dict

    def load_qualitative_scores(self, operation_id: str = None) -> List[Dict[str, Any]]:
        """
        从 equipment_operation_qualitative_score 加载专家定性评分

        Args:
            operation_id: 演练/任务ID，若为None则加载最新的

        Returns:
            专家评分记录列表
        """
        cursor = self.connection.cursor(DictCursor)

        if operation_id:
            query = """
                SELECT * FROM equipment_operation_qualitative_score
                WHERE operation_id = %s
                ORDER BY expert_name
            """
            cursor.execute(query, (operation_id,))
        else:
            # 获取最新的评估记录
            query = """
                SELECT * FROM equipment_operation_qualitative_score
                ORDER BY evaluation_time DESC, expert_name
            """
            cursor.execute(query)

        results = cursor.fetchall()
        cursor.close()

        return results

    def get_confidence_value(self, indicator_key: str, record: Dict[str, Any]) -> float:
        """
        获取指定指标的判断把握度（置信度）

        Args:
            indicator_key: 指标键名
            record: 评分记录

        Returns:
            判断把握度 λ (0-1)
        """
        confidence_field = f"{indicator_key}_confidence"
        confidence = record.get(confidence_field)

        if confidence is None:
            return 0.0

        return float(confidence)

    def get_grade_value(self, indicator_key: str, record: Dict[str, Any]) -> Optional[str]:
        """
        获取指定指标的等级代码

        Args:
            indicator_key: 指标键名
            record: 评分记录

        Returns:
            等级代码字符串
        """
        grade_field = f"{indicator_key}_ql"
        return record.get(grade_field)

    def calculate_comprehensive_credibility(
        self,
        alpha: float,
        beta: float,
        lambda_kj: float,
        mu: float = DEFAULT_MU,
        nu: float = DEFAULT_NU,
        eta: float = DEFAULT_ETA
    ) -> float:
        """
        计算综合判断可信度 γ_kj

        公式: γ_kj = μ·α_k + ν·β_k + η·λ_kj
        """
        # 确保权重和为1
        total_weight = mu + nu + eta
        if abs(total_weight - 1.0) > 0.001:
            # 归一化权重
            mu = mu / total_weight
            nu = nu / total_weight
            eta = eta / total_weight

        gamma = mu * alpha + nu * beta + eta * lambda_kj
        return gamma

    def compute_centroid_via_coverage(
        self,
        experts: List[Dict[str, Any]],
        x_min: float = 0.0,
        x_max: float = 100.0,
        step: float = 0.1
    ) -> Tuple[float, float]:
        """
        按照文献中的覆盖频度方法显式构造 P(x)，再用积分公式计算质心：

            P(x) = Σ γ_kj · p_kj(x)
            x*_j = ∫ x·P(x) dx / ∫ P(x) dx

        这里在 [x_min, x_max] 上构造均匀网格，用数值积分近似上述公式。

        Returns:
            (质心 x*, 覆盖函数积分 ∫P(x)dx)
        """
        if not experts:
            return 0.0, 0.0

        xs = np.arange(x_min, x_max + step / 2.0, step)
        P_vals = np.zeros_like(xs, dtype=float)

        # 对每个专家区间叠加覆盖频度 γ_kj
        for e in experts:
            a1 = float(e["interval_lower"])
            a2 = float(e["interval_upper"])
            gamma = float(e["gamma"])
            mask = (xs >= a1) & (xs <= a2)
            P_vals[mask] += gamma

        denom = float(P_vals.sum() * step)  # 近似 ∫ P(x) dx
        if denom <= 1e-8:
            return 0.0, 0.0

        num = float((xs * P_vals).sum() * step)  # 近似 ∫ x·P(x) dx
        centroid = num / denom
        return centroid, denom

    def aggregate_single_indicator(
        self,
        indicator_key: str,
        qualitative_scores: List[Dict[str, Any]],
        expert_credibility: Dict[str, Dict[str, float]],
        mu: float = DEFAULT_MU,
        nu: float = DEFAULT_NU,
        eta: float = DEFAULT_ETA
    ) -> Dict[str, Any]:
        """
        对单个指标进行专家集结（覆盖函数 + 质心法）

        Args:
            indicator_key: 指标键名
            qualitative_scores: 所有专家的定性评分记录
            expert_credibility: 专家可信度字典
            mu: 主观可信度权重
            nu: 客观可信度权重
            eta: 判断把握度权重

        Returns:
            集结结果字典
        """
        valid_experts = []  # 有效的专家评分 (λ >= 0.6)

        for record in qualitative_scores:
            expert_name = record.get('expert_name')
            if not expert_name:
                continue

            # 获取判断把握度 λ_kj
            lambda_kj = self.get_confidence_value(indicator_key, record)

            # 把握度过滤：λ < 0.6 不参与集结
            if lambda_kj < CONFIDENCE_THRESHOLD:
                continue

            # 获取等级并转换为区间
            grade = self.get_grade_value(indicator_key, record)
            interval = self.grade_to_interval(grade)

            if interval is None:
                continue

            # 获取专家可信度
            cred = expert_credibility.get(expert_name, {'alpha': 0.5, 'beta': 0.5})
            alpha = cred.get('alpha', 0.5)
            beta = cred.get('beta', 0.5)

            # 计算综合判断可信度 γ_kj
            gamma = self.calculate_comprehensive_credibility(alpha, beta, lambda_kj, mu, nu, eta)

            valid_experts.append({
                'expert_name': expert_name,
                'grade': grade,
                'interval': interval,
                'interval_lower': interval[0],
                'interval_upper': interval[1],
                'interval_midpoint': self.get_interval_midpoint(interval),
                'interval_width': self.get_interval_width(interval),
                'lambda': lambda_kj,
                'alpha': alpha,
                'beta': beta,
                'gamma': gamma
            })

        # 计算质心（群体结论）
        if not valid_experts:
            return {
                'indicator_key': indicator_key,
                'indicator_name': INDICATOR_NAMES.get(indicator_key, ''),
                'expert_count': 0,
                'total_expert_count': len(qualitative_scores),
                'centroid_value': None,
                'interval_lower': None,
                'interval_upper': None,
                'coverage_sum': 0,
                'expert_contributions': []
            }

        # 显式构造覆盖频度 P(x)，按文献公式用数值积分计算质心
        centroid, coverage_integral = self.compute_centroid_via_coverage(valid_experts)

        # 为保持与数据库字段含义一致，coverage_sum 采用 Σγ·(a2-a1)，
        # 理论上与 ∫P(x)dx 等价，数值上也非常接近。
        denominator = sum(e['gamma'] * e['interval_width'] for e in valid_experts)

        # 计算综合区间（按权重覆盖的区间）
        weighted_lower = sum(e['gamma'] * e['interval_width'] * e['interval_lower'] for e in valid_experts) / denominator if denominator > 0 else 0
        weighted_upper = sum(e['gamma'] * e['interval_width'] * e['interval_upper'] for e in valid_experts) / denominator if denominator > 0 else 0

        return {
            'indicator_key': indicator_key,
            'indicator_name': INDICATOR_NAMES.get(indicator_key, ''),
            'expert_count': len(valid_experts),
            'total_expert_count': len(qualitative_scores),
            'centroid_value': round(centroid, 2),
            'interval_lower': round(weighted_lower, 2),
            'interval_upper': round(weighted_upper, 2),
            'coverage_sum': round(denominator, 4),
            'expert_contributions': valid_experts
        }

    def aggregate_all_indicators(
        self,
        operation_id: str = None,
        batch_id: str = None,
        mu: float = DEFAULT_MU,
        nu: float = DEFAULT_NU,
        eta: float = DEFAULT_ETA
    ) -> List[Dict[str, Any]]:
        """
        对所有7个指标进行专家集结

        Args:
            operation_id: 演练/任务ID
            batch_id: 批次ID（用于获取专家可信度）
            mu: 主观可信度权重
            nu: 客观可信度权重
            eta: 判断把握度权重

        Returns:
            各指标的集结结果列表
        """
        # 加载数据
        qualitative_scores = self.load_qualitative_scores(operation_id)

        if not qualitative_scores:
            print("警告: 未找到定性评分数据")
            return []

        # 获取涉及到的专家列表
        experts_in_scores = set(record['expert_name'] for record in qualitative_scores)

        # 加载专家可信度（过滤只在评分中出现的专家）
        all_credibility = self.load_expert_credibility(batch_id)
        expert_credibility = {k: v for k, v in all_credibility.items() if k in experts_in_scores}

        # 如果没有找到可信度数据，使用默认值
        if not expert_credibility:
            print("警告: 未找到专家可信度数据，将使用默认值 0.5")
            for expert_name in experts_in_scores:
                expert_credibility[expert_name] = {'alpha': 0.5, 'beta': 0.5}

        # 对每个指标进行集结
        results = []
        for indicator_key in INDICATOR_KEYS:
            result = self.aggregate_single_indicator(
                indicator_key,
                qualitative_scores,
                expert_credibility,
                mu, nu, eta
            )
            results.append(result)

        return results

    def aggregate_indicator_system(
        self,
        indicator_results: List[Dict[str, Any]],
        weights: Dict[str, float] = None
    ) -> Dict[str, Any]:
        """
        将7个指标综合为单一效能值

        公式: E_system = Σ ω_j * x_j*

        Args:
            indicator_results: 各指标的集结结果
            weights: 指标权重字典，若为None则使用等权

        Returns:
            指标体系综合结果
        """
        # 过滤有效的指标结果
        valid_results = [r for r in indicator_results if r['centroid_value'] is not None]

        if not valid_results:
            return {
                'system_score': None,
                'indicator_scores': {},
                'weights_used': {}
            }

        # 使用等权或给定权重
        if weights is None:
            weights = {r['indicator_key']: 1.0 / len(valid_results) for r in valid_results}

        # 计算综合效能值
        system_score = sum(
            r['centroid_value'] * weights.get(r['indicator_key'], 0)
            for r in valid_results
        )

        # 构建输出
        indicator_scores = {r['indicator_key']: r['centroid_value'] for r in valid_results}

        return {
            'system_score': round(system_score, 2),
            'indicator_scores': indicator_scores,
            'weights_used': weights
        }

    def save_aggregation_results(
        self,
        operation_id: str,
        batch_id: str,
        indicator_results: List[Dict[str, Any]],
        system_result: Dict[str, Any],
        mu: float = DEFAULT_MU,
        nu: float = DEFAULT_NU,
        eta: float = DEFAULT_ETA
    ) -> bool:
        """
        将集结结果保存到数据库

        Args:
            operation_id: 演练/任务ID
            batch_id: 批次ID
            indicator_results: 指标集结结果
            system_result: 指标体系综合结果
            mu: 主观可信度权重
            nu: 客观可信度权重
            eta: 判断把握度权重

        Returns:
            是否保存成功
        """
        cursor = self.connection.cursor()

        try:
            evaluation_date = datetime.now().date()

            # 保存每个指标的集结结果
            for result in indicator_results:
                # 将专家贡献转换为JSON
                contributions_json = json.dumps(
                    [
                        {
                            'expert_name': c['expert_name'],
                            'grade': c['grade'],
                            'interval': c['interval'],
                            'gamma': round(c['gamma'], 4)
                        }
                        for c in result.get('expert_contributions', [])
                    ],
                    ensure_ascii=False
                )

                query = """
                    INSERT INTO expert_qualitative_aggregation_results (
                        batch_id, evaluation_date, operation_id,
                        indicator_key, indicator_name,
                        expert_count, total_expert_count,
                        interval_lower, interval_upper,
                        centroid_value, coverage_sum,
                        expert_contributions,
                        mu_weight, nu_weight, eta_weight
                    ) VALUES (
                        %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s
                    ) ON DUPLICATE KEY UPDATE
                        expert_count = VALUES(expert_count),
                        total_expert_count = VALUES(total_expert_count),
                        interval_lower = VALUES(interval_lower),
                        interval_upper = VALUES(interval_upper),
                        centroid_value = VALUES(centroid_value),
                        coverage_sum = VALUES(coverage_sum),
                        expert_contributions = VALUES(expert_contributions)
                """

                values = (
                    batch_id,
                    evaluation_date,
                    operation_id,
                    result['indicator_key'],
                    result['indicator_name'],
                    result['expert_count'],
                    result['total_expert_count'],
                    result['interval_lower'],
                    result['interval_upper'],
                    result['centroid_value'],
                    result['coverage_sum'],
                    contributions_json,
                    mu, nu, eta
                )

                cursor.execute(query, values)

            # 保存指标体系综合结果
            system_query = """
                INSERT INTO indicator_system_aggregation_results (
                    batch_id, evaluation_date, operation_id,
                    system_score, indicator_scores, weights_used,
                    aggregation_method
                ) VALUES (
                    %s, %s, %s, %s, %s, %s, %s
                ) ON DUPLICATE KEY UPDATE
                    system_score = VALUES(system_score),
                    indicator_scores = VALUES(indicator_scores),
                    weights_used = VALUES(weights_used)
            """

            system_values = (
                batch_id,
                evaluation_date,
                operation_id,
                system_result['system_score'],
                json.dumps(system_result['indicator_scores'], ensure_ascii=False),
                json.dumps(system_result['weights_used'], ensure_ascii=False),
                'weighted_sum'
            )

            cursor.execute(system_query, system_values)

            self.connection.commit()
            return True

        except Exception as e:
            self.connection.rollback()
            print(f"保存结果时出错: {e}")
            return False
        finally:
            cursor.close()

    # 指标键名到 equipment_operation_score 字段的映射（定性字段 _ql 后缀）
    INDICATOR_TO_SCORE_FIELD = {
        'maintenance_maintenance_skill': 'maintenance_maintenance_skill_ql',
        'comm_attack_jamming_effectiveness': 'comm_attack_jamming_effectiveness_ql',
        'comm_attack_deception_signal_generation': 'comm_attack_deception_signal_generation_ql',
        'comm_defense_signal_interception_awareness': 'comm_defense_signal_interception_awareness_ql',
        'comm_defense_anti_jamming_operation': 'comm_defense_anti_jamming_operation_ql',
        'comm_defense_anti_deception_awareness': 'comm_defense_anti_deception_awareness_ql',
        'maintenance_feedback_equipment_feedback': 'maintenance_feedback_equipment_feedback_ql',
    }

    def update_operation_score_with_centroids(
        self,
        operation_id: str,
        indicator_results: List[Dict[str, Any]]
    ) -> bool:
        """
        将集结后的质心值更新到 equipment_operation_score 表

        Args:
            operation_id: 演练/任务ID（如 OP-2026-001）
            indicator_results: 指标集结结果列表

        Returns:
            是否更新成功
        """
        if not operation_id:
            print("[!] 未指定 operation_id，无法更新 equipment_operation_score 表")
            return False

        cursor = self.connection.cursor()

        try:
            # 检查表是否存在
            cursor.execute("SHOW TABLES LIKE 'equipment_operation_score'")
            if not cursor.fetchone():
                print("[!] equipment_operation_score 表不存在，跳过更新")
                return False

            # 构建更新语句
            update_fields = []
            values = []

            for result in indicator_results:
                indicator_key = result.get('indicator_key')
                centroid_value = result.get('centroid_value')

                # 只更新有效的质心值
                if centroid_value is None:
                    continue

                score_field = self.INDICATOR_TO_SCORE_FIELD.get(indicator_key)
                if score_field:
                    update_fields.append(f"{score_field} = %s")
                    values.append(centroid_value)

            if not update_fields:
                print("[!] 没有有效的质心值需要更新")
                return False

            # 添加 operation_id 到参数列表
            values.append(operation_id)

            update_sql = f"""
                UPDATE equipment_operation_score
                SET {', '.join(update_fields)},
                    evaluation_time = NOW(),
                    updated_at = NOW()
                WHERE operation_id = %s
            """

            cursor.execute(update_sql, values)
            self.connection.commit()

            if cursor.rowcount > 0:
                print(f"[✓] 已将质心值更新到 equipment_operation_score 表 (operation_id={operation_id})")
                return True
            else:
                # 如果没有更新任何行，可能是 operation_id 不存在，尝试插入新记录
                print(f"[!] operation_id={operation_id} 在 equipment_operation_score 中不存在，尝试插入新记录")

                # 构建插入语句
                insert_fields = ['operation_id', 'evaluation_time', 'updated_at']
                insert_values = [operation_id, datetime.now(), datetime.now()]
                placeholders = ['%s', '%s', '%s']

                for result in indicator_results:
                    indicator_key = result.get('indicator_key')
                    centroid_value = result.get('centroid_value')

                    if centroid_value is None:
                        continue

                    score_field = self.INDICATOR_TO_SCORE_FIELD.get(indicator_key)
                    if score_field:
                        insert_fields.append(score_field)
                        insert_values.append(centroid_value)
                        placeholders.append('%s')

                insert_sql = f"""
                    INSERT INTO equipment_operation_score ({', '.join(insert_fields)})
                    VALUES ({', '.join(placeholders)})
                """

                cursor.execute(insert_sql, insert_values)
                self.connection.commit()
                print(f"[✓] 已插入新记录到 equipment_operation_score 表 (operation_id={operation_id})")
                return True

        except Exception as e:
            self.connection.rollback()
            print(f"[!] 更新 equipment_operation_score 表时出错: {e}")
            return False
        finally:
            cursor.close()

    def run_aggregation(
        self,
        operation_id: str = None,
        batch_id: str = None,
        weights: Dict[str, float] = None,
        mu: float = DEFAULT_MU,
        nu: float = DEFAULT_NU,
        eta: float = DEFAULT_ETA,
        print_results: bool = True
    ) -> Tuple[List[Dict[str, Any]], Dict[str, Any]]:
        """
        执行完整的专家定性数据集结流程

        Args:
            operation_id: 演练/任务ID
            batch_id: 批次ID
            weights: 指标权重
            mu: 主观可信度权重
            nu: 客观可信度权重
            eta: 判断把握度权重
            print_results: 是否打印结果

        Returns:
            (指标集结结果列表, 指标体系综合结果)
        """
        if print_results:
            print("=" * 80)
            print("专家定性数据集结")
            print("=" * 80)
            print(f"权重参数: μ={mu}, ν={nu}, η={eta}")
            print(f"判断可信度阈值: {CONFIDENCE_THRESHOLD}")
            print("-" * 80)

        # 1. 对所有指标进行集结
        indicator_results = self.aggregate_all_indicators(
            operation_id=operation_id,
            batch_id=batch_id,
            mu=mu, nu=nu, eta=eta
        )

        if print_results:
            print("\n【单指标集结结果】")
            print("-" * 80)
            print(f"{'指标键名':<45} {'专家数':>6} {'区间[a1,a2]':>15} {'质心x*':>8}")
            print("-" * 80)

            for r in indicator_results:
                indicator_name = f"{r['indicator_name']}({r['indicator_key']})"
                if r['centroid_value'] is not None:
                    interval_str = f"[{r['interval_lower']:.1f},{r['interval_upper']:.1f}]"
                    print(f"{indicator_name:<45} {r['expert_count']:>6}/{r['total_expert_count']:<4} {interval_str:>15} {r['centroid_value']:>8.2f}")
                else:
                    print(f"{indicator_name:<45} {r['expert_count']:>6}/{r['total_expert_count']:<4} {'无有效数据':>15} {'-':>8}")

        # 2. 指标体系综合
        system_result = self.aggregate_indicator_system(indicator_results, weights)

        if print_results:
            print("\n【指标体系综合结果】")
            print("-" * 80)
            if system_result['system_score'] is not None:
                print(f"综合效能值 E_system = {system_result['system_score']:.2f}")
            else:
                print("综合效能值: 无有效数据")

        # 只更新 equipment_operation_score 表的质心值
        if operation_id:
            update_success = self.update_operation_score_with_centroids(
                operation_id=operation_id,
                indicator_results=indicator_results
            )
            if print_results:
                if update_success:
                    print("[✓] 质心值已更新到 equipment_operation_score 表")
                else:
                    print("[!] equipment_operation_score 表更新失败或表不存在")

        if print_results:
            print("=" * 80)

        return indicator_results, system_result


# =====================================================
# 可视化类
# =====================================================

class QualitativeAggregationVisualizer:
    """专家定性数据集结可视化类"""

    def __init__(self, connection: pymysql.Connection = None, output_dir: str = './visualization_results'):
        """
        初始化可视化器

        Args:
            connection: 数据库连接（可选，用于从数据库加载数据）
            output_dir: 图表输出目录
        """
        self.connection = connection
        self.output_dir = output_dir
        os.makedirs(output_dir, exist_ok=True)
        plt.style.use('seaborn-v0_8-whitegrid')
        # 画图风格可能覆盖字体，此处与 calculate_expert_credibility.py 保持一致，避免中文乱码
        rcParams['font.sans-serif'] = ['SimHei']
        rcParams['axes.unicode_minus'] = False

    def load_aggregation_results(
        self,
        operation_id: str = None,
        batch_id: str = None
    ) -> Tuple[List[Dict[str, Any]], Dict[str, Any]]:
        """
        从数据库加载专家集结结果

        Args:
            operation_id: 演练/任务ID，若为None则加载最新的
            batch_id: 批次ID，若为None则加载最新的

        Returns:
            (指标集结结果列表, 指标体系综合结果)
        """
        if self.connection is None:
            raise ValueError("未设置数据库连接，请先初始化Visualizer时传入connection参数")

        cursor = self.connection.cursor(DictCursor)

        try:
            # 1. 加载指标集结结果
            if batch_id:
                indicator_query = """
                    SELECT * FROM expert_qualitative_aggregation_results
                    WHERE batch_id = %s
                    ORDER BY indicator_key
                """
                cursor.execute(indicator_query, (batch_id,))
            elif operation_id:
                indicator_query = """
                    SELECT * FROM expert_qualitative_aggregation_results
                    WHERE operation_id = %s
                    ORDER BY indicator_key
                """
                cursor.execute(indicator_query, (operation_id,))
            else:
                # 获取最新的
                indicator_query = """
                    SELECT * FROM expert_qualitative_aggregation_results
                    ORDER BY evaluation_date DESC, id DESC
                """
                cursor.execute(indicator_query)

            indicator_rows = cursor.fetchall()

            # 按batch_id和operation_id分组获取最新的结果
            if not indicator_rows:
                print("警告: 未找到集结结果数据")
                return [], {}

            # 获取当前batch_id和operation_id
            current_batch_id = indicator_rows[0]['batch_id']
            current_operation_id = indicator_rows[0]['operation_id']

            # 过滤出最新的批次
            indicator_results = []
            for row in indicator_rows:
                if row['batch_id'] == current_batch_id and row['operation_id'] == current_operation_id:
                    # 解析expert_contributions JSON
                    contributions = []
                    if row.get('expert_contributions'):
                        try:
                            contributions = json.loads(row['expert_contributions'])
                        except:
                            contributions = []

                    indicator_results.append({
                        'indicator_key': row['indicator_key'],
                        'indicator_name': row['indicator_name'],
                        'expert_count': row['expert_count'],
                        'total_expert_count': row['total_expert_count'],
                        'interval_lower': float(row['interval_lower']) if row['interval_lower'] else None,
                        'interval_upper': float(row['interval_upper']) if row['interval_upper'] else None,
                        'centroid_value': float(row['centroid_value']) if row['centroid_value'] else None,
                        'coverage_sum': float(row['coverage_sum']) if row['coverage_sum'] else 0,
                        'expert_contributions': contributions
                    })

            # 2. 加载指标体系综合结果
            system_query = """
                SELECT * FROM indicator_system_aggregation_results
                WHERE batch_id = %s AND operation_id = %s
                ORDER BY evaluation_date DESC
                LIMIT 1
            """
            cursor.execute(system_query, (current_batch_id, current_operation_id))
            system_row = cursor.fetchone()

            system_result = {}
            if system_row:
                indicator_scores = {}
                weights_used = {}
                if system_row.get('indicator_scores'):
                    try:
                        indicator_scores = json.loads(system_row['indicator_scores'])
                    except:
                        pass
                if system_row.get('weights_used'):
                    try:
                        weights_used = json.loads(system_row['weights_used'])
                    except:
                        pass

                system_result = {
                    'system_score': float(system_row['system_score']) if system_row['system_score'] else None,
                    'indicator_scores': indicator_scores,
                    'weights_used': weights_used,
                    'batch_id': current_batch_id,
                    'operation_id': current_operation_id
                }

            print(f"\n[✓] 成功从数据库加载数据:")
            print(f"    - 批次ID: {current_batch_id}")
            print(f"    - 演练ID: {current_operation_id}")
            print(f"    - 指标数量: {len(indicator_results)}")
            if system_result.get('system_score'):
                print(f"    - 综合效能值: {system_result['system_score']:.2f}")

            return indicator_results, system_result

        except Exception as e:
            print(f"从数据库加载数据失败: {e}")
            raise
        finally:
            cursor.close()

    def visualize_from_db(
        self,
        operation_id: str = None,
        batch_id: str = None,
        generate_all: bool = True
    ) -> Dict[str, str]:
        """
        从数据库加载数据并生成可视化

        Args:
            operation_id: 演练/任务ID
            batch_id: 批次ID
            generate_all: 是否生成所有图表

        Returns:
            图表路径字典
        """
        if self.connection is None:
            raise ValueError("未设置数据库连接")

        # 加载数据
        indicator_results, system_result = self.load_aggregation_results(operation_id, batch_id)

        if not indicator_results:
            print("无可视化数据")
            return {}

        # 生成图表
        if generate_all:
            return self.generate_all_charts(indicator_results, system_result)
        else:
            # 只生成综合仪表盘
            return {'dashboard': self.plot_comprehensive_dashboard(indicator_results, system_result)}

    def plot_single_indicator(
        self,
        indicator_result: Dict[str, Any],
        save_path: str = None
    ) -> str:
        """
        绘制单个指标的专家打分与质心对比图
        - 展示每位专家的原始打分区间
        - 显示质心位置
        - 包含详细数据表格
        """
        if indicator_result.get('centroid_value') is None:
            print(f"[!] 指标 {indicator_result.get('indicator_name', '')} 无有效数据，跳过")
            return None

        indicator_name = indicator_result['indicator_name']
        indicator_key = indicator_result['indicator_key']
        centroid = indicator_result['centroid_value']
        contributions = indicator_result.get('expert_contributions', [])

        if not contributions:
            print(f"[!] 指标 {indicator_name} 无专家贡献数据，跳过")
            return None

        if save_path is None:
            save_path = os.path.join(
                self.output_dir,
                f"indicator_{indicator_key}_{datetime.now().strftime('%Y%m%d_%H%M%S')}.png"
            )

        # 准备数据
        experts = [c['expert_name'] for c in contributions]
        lowers = [c['interval_lower'] for c in contributions]
        uppers = [c['interval_upper'] for c in contributions]
        midpoints = [c['interval_midpoint'] for c in contributions]
        gammas = [c['gamma'] for c in contributions]
        grades = [c['grade'] for c in contributions]

        # 创建图形：上方区间图 + 中间 P(x) + 下方数据表格
        fig = plt.figure(figsize=(14, 10))
        gs = fig.add_gridspec(3, 1, height_ratios=[3, 2, 2], hspace=0.35)

        # 上半部分：区间图
        ax = fig.add_subplot(gs[0])

        # 绘制每个专家的区间（水平条形）
        y_positions = range(len(experts))
        colors = plt.cm.Blues(np.linspace(0.4, 0.9, len(experts)))

        for i, (exp, lower, upper, mid, grade) in enumerate(zip(experts, lowers, uppers, midpoints, grades)):
            # 区间条
            ax.barh(i, upper - lower, left=lower, height=0.6, color=colors[i], alpha=0.7, edgecolor='navy')
            # 区间端点
            ax.scatter([lower, upper], [i, i], color='navy', s=60, zorder=5)
            # 中点（原始打分）
            ax.scatter([mid], [i], color='blue', s=100, marker='o', zorder=6, edgecolor='white', linewidth=1)
            # 标注等级
            ax.text(mid, i + 0.35, f'{grade}', ha='center', va='bottom', fontsize=10, fontweight='bold', color='blue')

        # 质心线（红色虚线 + 五角星）
        ax.axvline(x=centroid, color='red', linestyle='--', linewidth=2.5, label=f'质心 x* = {centroid:.2f}')
        ax.scatter([centroid], [len(experts) / 2 - 0.5], color='red', s=300, marker='*', zorder=7)

        # 参考线
        ax.axvline(x=60, color='orange', linestyle=':', linewidth=1.5, alpha=0.7, label='及格线 (60)')
        ax.axvline(x=80, color='green', linestyle=':', linewidth=1.5, alpha=0.7, label='良好线 (80)')
        ax.axvline(x=90, color='gold', linestyle=':', linewidth=1.5, alpha=0.7, label='优秀线 (90)')

        # 设置坐标轴
        ax.set_yticks(y_positions)
        ax.set_yticklabels(experts, fontsize=11)
        ax.set_xlabel('分数', fontsize=12)
        ax.set_xlim(0, 105)
        ax.set_title(f'指标: {indicator_name}\n专家原始打分区间 vs 最终质心', fontsize=14, fontweight='bold', pad=10)
        ax.legend(loc='lower right', fontsize=9)
        ax.grid(axis='x', linestyle='--', alpha=0.5)

        # 中间部分：覆盖频度 P(x) 台阶图
        ax_p = fig.add_subplot(gs[1])
        # 使用与计算质心相同的方式构造 P(x)，相邻区间采用左闭右开避免边界处“尖刺”
        xs = np.arange(0.0, 100.0 + 0.05, 0.1)
        P_vals = np.zeros_like(xs, dtype=float)

        # 找到所有区间的最大右端点，用于确保整体右边界仍然闭区间
        max_upper = max(float(c['interval_upper']) for c in contributions)

        for c in contributions:
            a1 = float(c['interval_lower'])
            a2 = float(c['interval_upper'])
            gamma = float(c['gamma'])

            # 一般区间：[a1, a2)；只有全局最右端区间用 [a1, max_upper]
            if math.isclose(a2, max_upper):
                mask = (xs >= a1) & (xs <= a2)
            else:
                mask = (xs >= a1) & (xs < a2)

            P_vals[mask] += gamma

        ax_p.step(xs, P_vals, where='post', color='#333399', linewidth=2, label='覆盖频度 P(x)')
        ax_p.axvline(x=centroid, color='red', linestyle='--', linewidth=2.0, label=f'质心 x* = {centroid:.2f}')
        ax_p.set_xlim(0, 100)
        ax_p.set_ylabel('P(x)', fontsize=11)
        ax_p.set_title('覆盖频度函数 P(x)', fontsize=12, fontweight='bold')
        ax_p.grid(axis='both', linestyle='--', alpha=0.4)
        ax_p.legend(loc='upper right', fontsize=9)

        # 下半部分：数据表格
        ax_table = fig.add_subplot(gs[2])
        ax_table.axis('off')

        # 表格数据：专家、等级、置信度(λ)、可信度(γ)、区间
        table_data = []
        for c in contributions:
            table_data.append([
                c['expert_name'],
                c['grade'],
                f"{c['lambda']:.2f}",
                f"{c['gamma']:.3f}",
                f"[{c['interval_lower']:.0f}, {c['interval_upper']:.0f}]"
            ])

        col_labels = ['专家', '等级', '置信度λ', '可信度γ', '区间']

        table = ax_table.table(
            cellText=table_data,
            colLabels=col_labels,
            cellLoc='center',
            loc='center',
            colWidths=[0.2, 0.1, 0.15, 0.15, 0.25]
        )
        table.auto_set_font_size(False)
        table.set_fontsize(10)
        table.scale(1, 1.8)

        # 表头样式
        for i, label in enumerate(col_labels):
            table[(0, i)].set_facecolor('#4472C4')
            table[(0, i)].set_text_props(color='white', fontweight='bold')

        # 质心行高亮
        table_last_row = len(table_data) + 1
        ax_table.text(0.5, -0.15, f'质心值 x* = {centroid:.2f}', transform=ax_table.transAxes,
                     ha='center', fontsize=12, fontweight='bold', color='red')

        plt.tight_layout()
        plt.savefig(save_path, dpi=150, bbox_inches='tight', facecolor='white')
        plt.close()
        print(f"[OK] 指标图已保存: {save_path}")
        return save_path

    def generate_all_charts(
        self,
        indicator_results: List[Dict[str, Any]],
        system_result: Dict[str, Any] = None
    ) -> Dict[str, str]:
        """
        生成所有图表：每个指标单独一张图
        """
        print("\n" + "=" * 60)
        print("开始生成可视化图表（每个指标单独一张图）")
        print("=" * 60)

        chart_paths = {}

        for result in indicator_results:
            indicator_key = result.get('indicator_key', 'unknown')
            path = self.plot_single_indicator(result)
            if path:
                chart_paths[indicator_key] = path

        # 额外生成一张汇总图：所有指标的质心对比
        if chart_paths:
            summary_path = self._plot_summary(indicator_results)
            if summary_path:
                chart_paths['summary'] = summary_path

        print("\n" + "=" * 60)
        print(f"共生成 {len(chart_paths)} 张图表，保存在: {self.output_dir}")
        print("=" * 60)
        return chart_paths

    def _plot_summary(
        self,
        indicator_results: List[Dict[str, Any]],
        save_path: str = None
    ) -> str:
        """绘制汇总图：所有指标的质心值对比"""
        if save_path is None:
            save_path = os.path.join(self.output_dir, f"summary_centroids_{datetime.now().strftime('%Y%m%d_%H%M%S')}.png")

        valid = [r for r in indicator_results if r.get('centroid_value') is not None]
        if not valid:
            return None

        # 按质心值排序
        valid.sort(key=lambda x: x['centroid_value'], reverse=True)

        names = [INDICATOR_NAMES_SHORT.get(r['indicator_key'], r['indicator_key']) for r in valid]
        values = [r['centroid_value'] for r in valid]
        colors = [self._get_category_color(r['indicator_key']) for r in valid]

        fig, ax = plt.subplots(figsize=(12, 6))
        bars = ax.barh(range(len(names)), values, color=colors, edgecolor='white', linewidth=1.5)

        # 标注数值
        for bar, val in zip(bars, values):
            ax.text(bar.get_width() + 1, bar.get_y() + bar.get_height() / 2,
                   f'{val:.2f}', va='center', fontsize=11, fontweight='bold')

        ax.set_yticks(range(len(names)))
        ax.set_yticklabels(names, fontsize=11)
        ax.set_xlabel('质心值', fontsize=12)
        ax.set_title('所有指标质心值汇总', fontsize=14, fontweight='bold')
        ax.set_xlim(0, 105)
        ax.axvline(x=60, color='orange', linestyle=':', linewidth=1.5, alpha=0.7, label='及格线 (60)')
        ax.axvline(x=80, color='green', linestyle=':', linewidth=1.5, alpha=0.7, label='良好线 (80)')
        ax.legend(loc='lower right')
        ax.grid(axis='x', linestyle='--', alpha=0.5)

        plt.tight_layout()
        plt.savefig(save_path, dpi=150, bbox_inches='tight', facecolor='white')
        plt.close()
        print(f"[OK] 汇总图已保存: {save_path}")
        return save_path

    def _get_category_color(self, indicator_key: str) -> str:
        """获取指标类别的颜色"""
        category = INDICATOR_CATEGORIES.get(indicator_key, 'default')
        return COLORS.get(category, COLORS['default'])


# =====================================================
# 数据库连接和工具函数
# =====================================================

def create_connection():
    """创建数据库连接（根据实际情况修改）"""
    return pymysql.connect(
        host='localhost',
        port=3306,
        user='root',
        password='root',
        database='military_operational_effectiveness_evaluation',
        charset='utf8mb4',
        cursorclass=DictCursor
    )


# =====================================================
# 主函数
# =====================================================

def main():
    """
    主函数 - 执行完整流程
    不传参时使用脚本内默认值（operation_id=OP-2026-001, batch_id=AHP-2026-001）跑基础数据。
    用法: python qualitative_data_analysis.py
          python qualitative_data_analysis.py [operation_id] [batch_id] [mu] [nu] [eta]
    """
    import sys

    # 解析命令行参数；不传参则使用默认值
    operation_id = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_OPERATION_ID
    batch_id = sys.argv[2] if len(sys.argv) > 2 else DEFAULT_BATCH_ID
    mu = float(sys.argv[3]) if len(sys.argv) > 3 else DEFAULT_MU
    nu = float(sys.argv[4]) if len(sys.argv) > 4 else DEFAULT_NU
    eta = float(sys.argv[5]) if len(sys.argv) > 5 else DEFAULT_ETA

    # 创建数据库连接
    try:
        connection = create_connection()
        print("[✓] 数据库连接成功")
        print(f"[*] 使用参数: operation_id={operation_id}, batch_id={batch_id}, μ={mu}, ν={nu}, η={eta}")
    except Exception as e:
        print(f"[✗] 数据库连接失败: {e}")
        print("\n使用方法:")
        print("  python qualitative_data_analysis.py                    # 使用默认值跑基础数据")
        print("  python qualitative_data_analysis.py [operation_id] [batch_id] [mu] [nu] [eta]")
        return

    try:
        # 0. 数据诊断：先检查库里是否有数据
        aggregator = QualitativeDataAggregation(connection)
        cursor = connection.cursor(DictCursor)
        try:
            cursor.execute("SELECT COUNT(*) AS n FROM equipment_operation_qualitative_score")
            n_scores = cursor.fetchone()['n'] or 0
            cursor.execute("SELECT COUNT(DISTINCT expert_name) AS n FROM expert_credibility_results")
            n_cred = cursor.fetchone()['n'] or 0
        except Exception as e:
            print(f"[!] 数据检查失败（请确认表已创建）: {e}")
            n_scores = 0
            n_cred = 0
        finally:
            cursor.close()

        print("\n【数据检查】")
        print(f"  - 定性评分表 equipment_operation_qualitative_score: {n_scores} 条")
        print(f"  - 专家可信度表 expert_credibility_results: {n_cred} 位专家")
        if n_scores == 0:
            print("\n[✗] 未找到定性评分数据，图表无法生成。")
            print("  请先运行以下脚本生成数据：")
            print("  1. calculate_expert_credibility.py  → 生成专家可信度（expert_credibility_results）")
            print("  2. generate_equipment_scores_v2.py   → 生成装备定性评分（equipment_operation_qualitative_score）")
            print("  确认数据库名一致（当前为 military_operational_effectiveness_evaluation），表结构见 create_tables.sql / create_qualitative_aggregation_tables.sql")
            return
        if n_cred == 0:
            print("\n[!] 未找到专家可信度，将使用默认值 0.5 参与计算。建议先运行 calculate_expert_credibility.py")

        # 1. 执行数据集结
        print("\n" + "=" * 80)
        print("1. 专家定性数据集结")
        print("=" * 80)

        indicator_results, system_result = aggregator.run_aggregation(
            operation_id=operation_id,
            batch_id=batch_id,
            mu=mu, nu=nu, eta=eta,
            print_results=True
        )

        # 检查是否有有效集结结果（至少有一个指标有质心值）
        valid_count = sum(1 for r in indicator_results if r.get('centroid_value') is not None)
        if valid_count == 0:
            print("\n[✗] 集结后无有效数据（所有指标的质心均为空）。")
            print("  可能原因：")
            print("  - 所有专家在该指标上的判断把握度 λ 均 < 0.6，被过滤掉")
            print("  - 定性评分表中等级字段（*_ql）或置信度字段（*_confidence）为空或格式不符")
            print("  请检查 equipment_operation_qualitative_score 表数据及字段名是否与 INDICATOR_KEYS 一致")
            return

        # 2. 生成可视化（直接使用上面的结果变量，不从数据库读取）
        print("\n" + "=" * 80)
        print("2. 生成可视化图表")
        print("=" * 80)

        visualizer = QualitativeAggregationVisualizer(connection=None, output_dir='./visualization_results')
        chart_paths = visualizer.generate_all_charts(indicator_results, system_result)

        print("\n生成的可视化图表:")
        for chart_type, path in chart_paths.items():
            print(f"  - {chart_type}: {path}")

        print("\n" + "=" * 80)
        print("执行完成!")
        print("=" * 80)

    except Exception as e:
        print(f"\n[✗] 执行失败: {e}")
        import traceback
        traceback.print_exc()
    finally:
        connection.close()
        print("\n[✓] 数据库连接已关闭")


if __name__ == '__main__':
    main()
