"""
军事通信装备效能评估 - 带惩罚模型的综合效能计算

功能：
1. 从 military_operation_effect_score 表获取各实验指标得分
2. 结合 ahp_expert_military_operation_effect_weights 表的权重计算加权效能得分
3. 应用崩溃比例惩罚模型（核心指标）
4. 生成柱状图可视化

惩罚模型说明（基于文献）：
- 核心惩罚指标：崩溃比例得分 (reliability_crash_rate_qt)
- 惩罚阈值 s = 50 分（崩溃比例超过50分开始惩罚）
- 惩罚系数 m = 0.8（可调整，m越小惩罚越重）
- 惩罚函数 F_i：
    F_i = 1, x ≤ s (崩溃率低，无惩罚)
    F_i = (s/x) × m, x > s (崩溃率高，惩罚重)
- 综合惩罚因子 P = min(F_i)（取所有指标中最小的惩罚因子）
- 最终效能得分 Sfinal = Sstage × P

文献来源：王思远, 张目. 基于惩罚与激励的TOPSIS多属性决策方法[J]. 统计与决策, 2018, 34(10): 82-84.

批次: AHP-2026-001
"""

import mysql.connector
from mysql.connector import Error
import matplotlib.pyplot as plt
import numpy as np
import os

# 设置中文字体
plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei', 'Arial Unicode MS']
plt.rcParams['axes.unicode_minus'] = False


class EffectivenessEvaluationWithPenalty:
    """带惩罚模型的效能评估计算器"""

    def __init__(self):
        self.connection = mysql.connector.connect(
            host='localhost',
            database='military_operational_effectiveness_evaluation',
            user='root',
            password='root'
        )

        # 核心惩罚指标配置（基于文献公式）
        # 崩溃比例得分：得分越高=崩溃率越低=越好，低于阈值开始惩罚
        # 文献: 王思远, 张目. 基于惩罚与激励的TOPSIS多属性决策方法
        # 公式: F_i = 1, x ≥ s; F_i = (x/s) × m, x < s
        self.penalty_config = {
            'reliability_crash_rate_qt': {
                'name': '崩溃比例得分',
                'threshold': 70,       # 惩罚阈值 s (崩溃比例得分低于70分开始惩罚)
                'm': 0.8,              # 惩罚系数 m (0<m≤1, 越小惩罚越重)
                'category': 'reliability',
                'reverse': False       # 标记：得分越高越好，低于阈值惩罚
            }
        }

        # military_operation_effect_score 表的字段列表（17个指标）
        self.score_fields = [
            # 安全指标 (3个)
            'security_key_leakage_qt',
            'security_detected_probability_qt',
            'security_interception_resistance_ql',
            # 可靠性指标 (3个)
            'reliability_crash_rate_qt',
            'reliability_recovery_capability_qt',
            'reliability_communication_availability_qt',
            # 传输指标 (6个)
            'transmission_bandwidth_qt',
            'transmission_call_setup_time_qt',
            'transmission_transmission_delay_qt',
            'transmission_bit_error_rate_qt',
            'transmission_throughput_qt',
            'transmission_spectral_efficiency_qt',
            # 抗干扰指标 (3个)
            'anti_jamming_sinr_qt',
            'anti_jamming_anti_jamming_margin_qt',
            'anti_jamming_communication_distance_qt',
            # 效能指标 (2个)
            'effect_damage_rate_qt',
            'effect_mission_completion_rate_qt',
        ]

        # 字段中文名称
        self.field_names_cn = {
            # 安全指标
            'security_key_leakage_qt': '密钥泄露率得分',
            'security_detected_probability_qt': '被发现概率得分',
            'security_interception_resistance_ql': '抗截获能力得分',
            # 可靠性指标
            'reliability_crash_rate_qt': '崩溃比例得分',
            'reliability_recovery_capability_qt': '恢复能力得分',
            'reliability_communication_availability_qt': '通信可用性得分',
            # 传输指标
            'transmission_bandwidth_qt': '带宽得分',
            'transmission_call_setup_time_qt': '建立时间得分',
            'transmission_transmission_delay_qt': '传输时延得分',
            'transmission_bit_error_rate_qt': '比特误码率得分',
            'transmission_throughput_qt': '吞吐量得分',
            'transmission_spectral_efficiency_qt': '频谱效率得分',
            # 抗干扰指标
            'anti_jamming_sinr_qt': '信干噪比得分',
            'anti_jamming__qt': '抗干扰余量anti_jamming_margin得分',
            'anti_jamming_communication_distance_qt': '通信距离得分',
            # 效能指标
            'effect_damage_rate_qt': '毁伤率得分',
            'effect_mission_completion_rate_qt': '任务完成率得分',
        }

        # 权重字段映射（打分表字段 -> 权重表字段）
        self.weight_field_mapping = {
            'security_key_leakage_qt': 'security_key_leakage_weight',
            'security_detected_probability_qt': 'security_detected_probability_weight',
            'security_interception_resistance_ql': 'security_interception_resistance_weight',
            'reliability_crash_rate_qt': 'reliability_crash_rate_weight',
            'reliability_recovery_capability_qt': 'reliability_recovery_capability_weight',
            'reliability_communication_availability_qt': 'reliability_communication_availability_weight',
            'transmission_bandwidth_qt': 'transmission_band_weight',
            'transmission_call_setup_time_qt': 'transmission_call_setup_time_weight',
            'transmission_transmission_delay_qt': 'transmission_transmission_delay_weight',
            'transmission_bit_error_rate_qt': 'transmission_bit_error_rate_weight',
            'transmission_throughput_qt': 'transmission_throughput_weight',
            'transmission_spectral_efficiency_qt': 'transmission_spectral_efficiency_weight',
            'anti_jamming_sinr_qt': 'anti_jamming_sinr_weight',
            'anti_jamming_anti_jamming_margin_qt': 'anti_jamming_anti_jamming_margin_weight',
            'anti_jamming_communication_distance_qt': 'anti_jamming_communication_distance_weight',
            'effect_damage_rate_qt': 'effect_damage_rate_weight',
            'effect_mission_completion_rate_qt': 'effect_mission_completion_rate_weight',
        }

    def get_ahp_weights(self):
        """从 ahp_final_weights 表获取最终权重（final_weight）"""
        cursor = self.connection.cursor(dictionary=True)

        # 查询最终权重表（包含归一化后的 final_weight）
        query = """
            SELECT indicator_key, final_weight FROM ahp_final_weights
            ORDER BY id
        """

        cursor.execute(query)
        weights = cursor.fetchall()
        cursor.close()

        if not weights:
            print("[警告] ahp_final_weights表中无数据!")
            return {}

        # 将 indicator_key 转换为 score 字段格式，并提取 final_weight
        final_weights = {}
        for weight_row in weights:
            indicator_key = weight_row['indicator_key']
            final_weight = weight_row['final_weight']

            if final_weight is None:
                continue

            # 将 indicator_key 转换为 score 字段格式
            # 例如: security_key_leakage -> security_key_leakage_qt
            score_field = indicator_key + '_qt'

            # 只保留评分表中存在的指标
            if score_field in self.score_fields:
                final_weights[score_field] = float(final_weight)

        # 验证权重总和
        total = sum(final_weights.values())
        print(f"[OK] 已加载最终权重数据，有效指标数: {len(final_weights)}, 权重总和: {total:.4f}")

        # 如果权重不为一，进行归一化
        if total > 0 and abs(total - 1.0) > 0.01:
            print(f"[警告] 权重总和不为1，已进行归一化处理")
            for key in final_weights:
                final_weights[key] /= total
            print(f"[OK] 归一化后权重总和: {sum(final_weights.values()):.4f}")

        # 打印权重详情
        print("\n权重详情:")
        for field, weight in final_weights.items():
            print(f"  {field}: {weight:.4f}")

        return final_weights

    def get_equipment_scores(self):
        """从military_operation_effect_score表获取各实验的指标得分"""
        cursor = self.connection.cursor(dictionary=True)

        # 构建查询字段列表
        select_fields = ['operation_id'] + self.score_fields

        # 查询所有实验数据
        query = f"""
            SELECT {', '.join(select_fields)}
            FROM military_operation_effect_score
            ORDER BY operation_id
        """

        cursor.execute(query)
        scores = cursor.fetchall()
        cursor.close()

        if not scores:
            print("[警告] military_operation_effect_score表中无数据!")
            return []

        print(f"已加载 {len(scores)} 个实验的操作评分数据")
        return scores

    def get_cost_evaluation(self):
        """从cost_evaluation表获取各实验的成本数据"""
        cursor = self.connection.cursor(dictionary=True)

        # 查询所有成本数据
        query = """
            SELECT operation_id, evaluation_time, 
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
                   cost_infrastructure_spectrum_fee_qt
            FROM cost_evaluation
            ORDER BY operation_id
        """

        cursor.execute(query)
        costs = cursor.fetchall()
        cursor.close()

        if not costs:
            print("[警告] cost_evaluation表中无数据!")
            return []

        print(f"已加载 {len(costs)} 个实验的成本数据")
        return costs

    def normalize_cost_maxmin(self, cost_data):
        """
        对成本数据进行max-min归一化（正向指标：费用越高，归一化值越高）
        
        x_normalized = (x - x_min) / (x_max - x_min)
        
        返回：归一化后的成本数据列表
        """
        if not cost_data:
            return []

        # 获取所有成本字段
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

        # 计算每个字段的min和max
        field_minmax = {}
        for field in cost_fields:
            values = [float(c.get(field, 0) or 0) for c in cost_data if c.get(field) is not None]
            if values:
                field_minmax[field] = {'min': min(values), 'max': max(values)}

        # 归一化每个实验的成本数据
        normalized_costs = []
        for cost in cost_data:
            normalized = {'operation_id': cost['operation_id']}
            total_normalized = 0
            valid_count = 0

            for field in cost_fields:
                if field in field_minmax and cost.get(field) is not None:
                    value = float(cost[field])
                    min_val = field_minmax[field]['min']
                    max_val = field_minmax[field]['max']

                    # max-min归一化
                    if max_val > min_val:
                        normalized[field] = (value - min_val) / (max_val - min_val)
                    else:
                        normalized[field] = 1.0

                    total_normalized += normalized[field]
                    valid_count += 1
                else:
                    normalized[field] = 0

            # 计算平均成本（归一化后的平均值）
            if valid_count > 0:
                normalized['avg_normalized_cost'] = total_normalized / valid_count
            else:
                normalized['avg_normalized_cost'] = 0

            # 保存原始成本总和（用于参考）
            original_total = sum([float(cost.get(f, 0) or 0) for f in cost_fields if cost.get(f) is not None])
            normalized['original_total_cost'] = original_total

            normalized_costs.append(normalized)

        print(f"[OK] 成本数据归一化完成")
        print(f"  原始成本范围: {min([c['original_total_cost'] for c in normalized_costs]):.2f} - {max([c['original_total_cost'] for c in normalized_costs]):.2f}")
        print(f"  归一化成本范围: {min([c['avg_normalized_cost'] for c in normalized_costs]):.4f} - {max([c['avg_normalized_cost'] for c in normalized_costs]):.4f}")

        return normalized_costs

    def calculate_penalty_factor(self, score_field, score_value):
        """
        计算惩罚因子（基于文献公式）

        崩溃比例得分逻辑：
        - 得分越高 = 崩溃率越低 = 越好
        - 得分低于阈值才开始惩罚

        文献来源：王思远, 张目. 基于惩罚与激励的TOPSIS多属性决策方法[J]. 统计与决策, 2018.

        公式:
        F_i = 1, x ≥ s (无惩罚)
        F_i = (x/s) × m, x < s (惩罚)

        参数:
            score_field: 指标字段名
            score_value: 指标得分 (x)

        返回:
            惩罚因子 F_i
        """
        if score_field not in self.penalty_config:
            return 1.0

        config = self.penalty_config[score_field]
        threshold = config['threshold']  # s: 惩罚阈值
        m = config['m']                   # m: 惩罚系数 (0<m≤1)

        # 崩溃比例得分：得分越高越好，低于阈值开始惩罚
        if score_value >= threshold:
            return 1.0  # 得分高，无惩罚
        else:
            # 惩罚因子 = (x/s) × m
            penalty = (score_value / threshold) * m
            return max(penalty, 0.1)  # 最小惩罚因子为0.1

    def calculate_weighted_effectiveness(self, exp, weights):
        """
        计算加权效能得分

        参数:
            exp: 实验数据字典
            weights: 权重字典

        返回:
            加权效能得分
        """
        total_weighted_score = 0
        total_weight = 0

        for field in self.score_fields:
            score_value = float(exp.get(field, 0) or 0)
            weight = weights.get(field, 1.0)

            total_weighted_score += score_value * weight
            total_weight += weight

        if total_weight > 0:
            return total_weighted_score / total_weight
        return 0

    def calculate_effectiveness(self):
        """计算各实验的效能得分（带惩罚）"""
        print("\n" + "="*80)
        print("开始计算效能得分（带崩溃比例惩罚模型）")
        print("="*80)

        # 1. 获取AHP权重
        weights = self.get_ahp_weights()
        if not weights:
            print("[错误] 无法获取权重数据!")
            return []

        # 2. 获取实验评分
        experiment_scores = self.get_equipment_scores()
        if not experiment_scores:
            return []

        # 3. 计算每个实验的效能得分
        results = []

        print("\n" + "-"*80)
        print("效能得分计算过程")
        print("-"*80)

        for exp in experiment_scores:
            operation_id = exp['operation_id']

            # 获取崩溃比例得分（核心惩罚指标）
            crash_rate = float(exp.get('reliability_crash_rate_qt', 0) or 0)

            # 计算崩溃比例惩罚因子
            crash_penalty = self.calculate_penalty_factor(
                'reliability_crash_rate_qt',
                crash_rate
            )

            # 计算加权效能得分 Sstage
            stage_score = self.calculate_weighted_effectiveness(exp, weights)

            # 应用惩罚因子
            # Sfinal = Sstage × P
            final_score = stage_score * crash_penalty

            result = {
                'operation_id': operation_id,
                'crash_rate': crash_rate,
                'crash_penalty': crash_penalty,
                'stage_score': stage_score,
                'final_score': final_score,
                'exp_data': exp
            }
            results.append(result)

            # 打印详细信息
            print(f"\n【{operation_id}】")
            print(f"  崩溃比例得分: {crash_rate:.2f} 分")
            print(f"  惩罚因子: {crash_penalty:.4f} {'[无惩罚]' if crash_penalty == 1.0 else '[已惩罚]'}")
            print(f"  加权阶段得分: {stage_score:.4f}")
            print(f"  最终得分: {final_score:.4f}")

            # 如果崩溃比例低于阈值，提示
            if crash_rate < 70:
                print(f"  [!] 崩溃比例得分 < 70分，应用惩罚后最终得分上限: {0.8 * 70:.2f}%")

        return results

    def display_summary(self, results):
        """显示汇总结果"""
        print("\n" + "="*80)
        print("效能评估汇总结果（带崩溃比例惩罚）")
        print("="*80)

        print(f"\n{'实验ID':<15} {'崩溃比例':<10} {'惩罚因子':<12} {'阶段得分':<12} {'最终得分':<12}")
        print("-"*60)

        for r in results:
            print(f"{r['operation_id']:<15} {r['crash_rate']:>8.2f} {r['crash_penalty']:>10.4f} "
                  f"{r['stage_score']:>10.4f} {r['final_score']:>10.4f}")

        # 统计信息
        final_scores = [r['final_score'] for r in results]
        stage_scores = [r['stage_score'] for r in results]

        print("\n" + "="*80)
        print("统计信息")
        print("="*80)
        print(f"  实验数量: {len(results)}")
        print(f"  阶段得分范围: {min(stage_scores):.4f} - {max(stage_scores):.4f}")
        print(f"  阶段得分均值: {np.mean(stage_scores):.4f}")
        print(f"  最终得分范围: {min(final_scores):.4f} - {max(final_scores):.4f}")
        print(f"  最终得分均值: {np.mean(final_scores):.4f}")
        print(f"  最终得分标准差: {np.std(final_scores):.4f}")

        # 显示被惩罚的实验
        penalized = [r for r in results if r['crash_penalty'] < 1.0]
        if penalized:
            print(f"\n  [!] 有 {len(penalized)} 个实验受到惩罚:")
            for r in penalized:
                print(f"    {r['operation_id']}: 崩溃比例 {r['crash_rate']:.2f} -> 惩罚因子 {r['crash_penalty']:.4f}")

    def visualize_results(self, results, save_path='document/result'):
        """生成柱状图可视化"""

        # 确保保存目录存在
        os.makedirs(save_path, exist_ok=True)

        # 提取数据
        operation_ids = [r['operation_id'] for r in results]
        stage_scores = [r['stage_score'] for r in results]  # 已经是百分制
        final_scores = [r['final_score'] for r in results]
        crash_rates = [r['crash_rate'] for r in results]

        x = np.arange(len(operation_ids))
        width = 0.35

        # 创建图表
        fig, ax = plt.subplots(figsize=(14, 8))

        # 绘制柱状图
        bars1 = ax.bar(x - width/2, stage_scores, width, label='阶段得分 (Sstage)',
                       color='#3498db', alpha=0.8, edgecolor='black', linewidth=0.5)
        bars2 = ax.bar(x + width/2, final_scores, width, label='最终得分 (Sfinal)',
                       color='#e74c3c', alpha=0.8, edgecolor='black', linewidth=0.5)

        # 添加崩溃比例折线
        ax2 = ax.twinx()
        line = ax2.plot(x, crash_rates, 'g-o', linewidth=2, markersize=6,
                        label='崩溃比例得分', color='#27ae60')
        ax2.set_ylabel('崩溃比例得分 (分)', fontsize=12, color='#27ae60')
        ax2.tick_params(axis='y', labelcolor='#27ae60')
        ax2.set_ylim(0, 100)

        # 添加70分阈值线
        ax2.axhline(y=70, color='#e67e22', linestyle='--', linewidth=1.5,
                     label='惩罚阈值 (70分)', alpha=0.7)

        # 设置标签和标题
        ax.set_xlabel('实验编号', fontsize=12)
        ax.set_ylabel('效能得分 (百分制)', fontsize=12)
        ax.set_title('军事通信装备效能评估 - 带崩溃比例惩罚模型\n(基于文献: 惩罚阈值=50分, m=0.8)',
                     fontsize=14, fontweight='bold')

        ax.set_xticks(x)
        ax.set_xticklabels(operation_ids, rotation=45, ha='right')

        # 合并图例
        lines1, labels1 = ax.get_legend_handles_labels()
        lines2, labels2 = ax2.get_legend_handles_labels()
        ax.legend(lines1 + lines2, labels1 + labels2, loc='upper right', fontsize=10)

        # 添加网格
        ax.grid(axis='y', alpha=0.3, linestyle='--')
        ax.set_ylim(0, 100)

        # 在柱子上添加数值标签
        for bar in bars1:
            height = bar.get_height()
            ax.annotate(f'{height:.1f}',
                        xy=(bar.get_x() + bar.get_width() / 2, height),
                        xytext=(0, 3),
                        textcoords="offset points",
                        ha='center', va='bottom', fontsize=8, rotation=0)

        for bar in bars2:
            height = bar.get_height()
            ax.annotate(f'{height:.1f}',
                        xy=(bar.get_x() + bar.get_width() / 2, height),
                        xytext=(0, 3),
                        textcoords="offset points",
                        ha='center', va='bottom', fontsize=8, rotation=0)

        plt.tight_layout()

        # 保存图表
        output_file = os.path.join(save_path, 'effectiveness_evaluation_with_penalty.png')
        plt.savefig(output_file, dpi=150, bbox_inches='tight',
                    facecolor='white', edgecolor='none')
        plt.close()

    def calculate_cost_effectiveness(self, effectiveness_results, normalized_costs):
        """
        计算效费比
        
        效费比 = 最终效能得分 / 成本投入
        
        参数:
            effectiveness_results: 效能评估结果列表
            normalized_costs: 归一化后的成本数据列表
        
        返回: 包含效费比的结果列表
        """
        # 创建operation_id到cost的映射
        cost_map = {c['operation_id']: c for c in normalized_costs}

        for result in effectiveness_results:
            operation_id = result['operation_id']
            final_score = result['final_score']
            
            if operation_id in cost_map:
                cost_data = cost_map[operation_id]
                avg_cost = cost_data['avg_normalized_cost']
                original_cost = cost_data['original_total_cost']
                
                # 计算效费比（效能/成本）
                # 如果成本为0，效费比设为最大
                if avg_cost > 0:
                    cost_effectiveness_ratio = final_score / (avg_cost * 100)  # 放大100倍
                else:
                    cost_effectiveness_ratio = final_score
                
                result['avg_normalized_cost'] = avg_cost
                result['original_cost'] = original_cost
                result['cost_effectiveness_ratio'] = cost_effectiveness_ratio
            else:
                result['avg_normalized_cost'] = 0
                result['original_cost'] = 0
                result['cost_effectiveness_ratio'] = 0

        # 打印效费比结果
        print("\n" + "="*80)
        print("效费比分析结果")
        print("="*80)
        print(f"\n{'实验ID':<15} {'最终得分':<12} {'归一化成本':<12} {'原始成本':<12} {'效费比':<12}")
        print("-"*70)

        for r in effectiveness_results:
            print(f"{r['operation_id']:<15} {r['final_score']:>10.2f} {r['avg_normalized_cost']:>10.4f} "
                  f"{r['original_cost']:>10.2f} {r['cost_effectiveness_ratio']:>10.4f}")

        # 统计信息
        ratios = [r['cost_effectiveness_ratio'] for r in effectiveness_results]
        costs = [r['avg_normalized_cost'] for r in effectiveness_results]
        scores = [r['final_score'] for r in effectiveness_results]

        print("\n" + "-"*80)
        print("统计信息:")
        print(f"  效费比范围: {min(ratios):.4f} - {max(ratios):.4f}")
        print(f"  效费比均值: {np.mean(ratios):.4f}")
        print(f"  归一化成本范围: {min(costs):.4f} - {max(costs):.4f}")
        print(f"  最终得分范围: {min(scores):.2f} - {max(scores):.2f}")

        # 找出最佳效费比方案
        best_ratio_idx = np.argmax(ratios)
        best_result = effectiveness_results[best_ratio_idx]
        print(f"\n  [最佳效费比方案]: {best_result['operation_id']} (效费比: {best_result['cost_effectiveness_ratio']:.4f})")

        # 找出最高效能方案
        best_score_idx = np.argmax(scores)
        best_score_result = effectiveness_results[best_score_idx]
        print(f"  [最高效能方案]: {best_score_result['operation_id']} (最终得分: {best_score_result['final_score']:.2f})")

        return effectiveness_results

    def visualize_cost_effectiveness(self, results, save_path='document/result'):
        """
        绘制效费比与最终得分对比图
        """
        os.makedirs(save_path, exist_ok=True)

        operation_ids = [r['operation_id'] for r in results]
        final_scores = [r['final_score'] for r in results]
        cost_effectiveness_ratios = [r['cost_effectiveness_ratio'] for r in results]
        normalized_costs = [r['avg_normalized_cost'] * 100 for r in results]  # 放大到百分制

        x = np.arange(len(operation_ids))

        # 创建图表 - 效费比用折线图
        fig, axes = plt.subplots(2, 1, figsize=(14, 12))

        # 图1: 效能得分用柱状图，效费比用折线图
        ax1 = axes[0]
        
        # 柱状图 - 最终效能得分
        bars = ax1.bar(x, final_scores, width=0.6, label='最终效能得分', color='#3498db', alpha=0.8)
        
        # 折线图 - 效费比 (放在右Y轴)
        ax1_twin = ax1.twinx()
        line = ax1_twin.plot(x, cost_effectiveness_ratios, 'r-o', linewidth=2.5, markersize=8, 
                             label='效费比', color='#e74c3c')
        
        ax1.set_xlabel('实验编号', fontsize=12)
        ax1.set_ylabel('最终效能得分', fontsize=12, color='#3498db')
        ax1_twin.set_ylabel('效费比', fontsize=12, color='#e74c3c')
        ax1.set_title('最终效能得分与效费比对比', fontsize=14, fontweight='bold')
        ax1.set_xticks(x)
        ax1.set_xticklabels(operation_ids, rotation=45, ha='right')
        
        # 设置Y轴范围
        ax1.set_ylim(0, max(final_scores) * 1.2)
        ax1_twin.set_ylim(0, max(cost_effectiveness_ratios) * 1.3)
        
        # 设置刻度颜色
        ax1.tick_params(axis='y', labelcolor='#3498db')
        ax1_twin.tick_params(axis='y', labelcolor='#e74c3c')
        
        # 添加数值标签 - 效能得分
        for bar, score in zip(bars, final_scores):
            height = bar.get_height()
            ax1.annotate(f'{score:.1f}',
                        xy=(bar.get_x() + bar.get_width() / 2, height),
                        xytext=(0, 3),
                        textcoords="offset points",
                        ha='center', va='bottom', fontsize=9, color='#3498db', fontweight='bold')
        
        # 添加数值标签 - 效费比
        for i, ratio in enumerate(cost_effectiveness_ratios):
            ax1_twin.annotate(f'{ratio:.2f}',
                        xy=(i, ratio),
                        xytext=(0, 8),
                        textcoords="offset points",
                        ha='center', va='bottom', fontsize=9, color='#e74c3c', fontweight='bold')
        
        # 合并图例
        lines1, labels1 = ax1.get_legend_handles_labels()
        lines2, labels2 = ax1_twin.get_legend_handles_labels()
        ax1.legend(lines1 + lines2, labels1 + labels2, loc='upper right')
        
        ax1.grid(axis='y', alpha=0.3)

        # 图2: 成本投入与效能关系散点图
        ax2 = axes[1]

        # 归一化成本作为X轴，最终得分作为Y轴
        scatter = ax2.scatter(normalized_costs, final_scores, 
                              c=cost_effectiveness_ratios, cmap='RdYlGn', 
                              s=200, alpha=0.8, edgecolors='black', linewidth=1)

        # 添加实验标签
        for i, op_id in enumerate(operation_ids):
            ax2.annotate(op_id.replace('OP-', ''), 
                        (normalized_costs[i], final_scores[i]),
                        xytext=(5, 5), textcoords='offset points', fontsize=9)

        ax2.set_xlabel('成本投入 (归一化, ×100)', fontsize=12)
        ax2.set_ylabel('最终效能得分', fontsize=12)
        ax2.set_title('成本-效能关系图 (颜色表示效费比)', fontsize=14, fontweight='bold')
        ax2.grid(True, alpha=0.3)

        # 添加颜色条
        cbar = plt.colorbar(scatter, ax=ax2)
        cbar.set_label('效费比', fontsize=11)

        # 找出最佳方案（高效低本）
        best_idx = np.argmax(cost_effectiveness_ratios)
        ax2.scatter([normalized_costs[best_idx]], [final_scores[best_idx]], 
                   c='gold', s=400, marker='*', edgecolors='black', linewidth=2, 
                   zorder=5, label='最佳效费比')
        ax2.legend(loc='upper right')

        # 添加文字总结
        summary_text = self.generate_summary_text(results)
        ax2.text(0.02, 0.98, summary_text, transform=ax2.transAxes, fontsize=10,
                verticalalignment='top', bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.8))

        plt.tight_layout()

        # 保存图表
        output_file = os.path.join(save_path, 'cost_effectiveness_comparison.png')
        plt.savefig(output_file, dpi=150, bbox_inches='tight',
                    facecolor='white', edgecolor='none')
        plt.close()

        print(f"\n[OK] 效费比对比图已保存至: {output_file}")

        return output_file

    def generate_summary_text(self, results):
        """生成文字总结"""
        # 按效费比排序
        sorted_by_ratio = sorted(results, key=lambda x: x['cost_effectiveness_ratio'], reverse=True)
        # 按效能得分排序
        sorted_by_score = sorted(results, key=lambda x: x['final_score'], reverse=True)
        # 按成本排序（越低越好）
        sorted_by_cost = sorted(results, key=lambda x: x['original_cost'])

        best_ratio = sorted_by_ratio[0]
        best_score = sorted_by_score[0]
        lowest_cost = sorted_by_cost[0]

        # 计算高于平均效费比的方案
        avg_ratio = sum([r['cost_effectiveness_ratio'] for r in results]) / len(results)
        above_avg = [r['operation_id'].replace('OP-', '') for r in results if r['cost_effectiveness_ratio'] >= avg_ratio]

        summary = f"""[分析总结]

[最佳效费比] {best_ratio['operation_id'].replace('OP-', '')} (效费比: {best_ratio['cost_effectiveness_ratio']:.2f})
   - 最终得分: {best_ratio['final_score']:.1f}分
   - 成本投入: {best_ratio['original_cost']:.0f}

[最高效能] {best_score['operation_id'].replace('OP-', '')} (得分: {best_score['final_score']:.1f})
   - 效费比: {best_score['cost_effectiveness_ratio']:.2f}
   - 成本投入: {best_score['original_cost']:.0f}

[最低成本] {lowest_cost['operation_id'].replace('OP-', '')} (成本: {lowest_cost['original_cost']:.0f})
   - 最终得分: {lowest_cost['final_score']:.1f}分
   - 效费比: {lowest_cost['cost_effectiveness_ratio']:.2f}

[效费比均值]: {avg_ratio:.2f}
   高于均值: {', '.join(above_avg) if above_avg else '无'}"""
        
        return summary

    def export_cost_effectiveness_csv(self, results, save_path='document/result'):
        """导出效费比结果到CSV"""
        os.makedirs(save_path, exist_ok=True)

        import csv

        output_file = os.path.join(save_path, 'cost_effectiveness_results.csv')

        headers = ['实验ID', '崩溃比例得分', '惩罚因子', '阶段得分', '最终得分', 
                   '归一化成本', '原始成本', '效费比']

        with open(output_file, 'w', newline='', encoding='utf-8-sig') as f:
            writer = csv.writer(f)
            writer.writerow(headers)

            for r in results:
                writer.writerow([
                    r['operation_id'],
                    r['crash_rate'],
                    r['crash_penalty'],
                    r['stage_score'],
                    r['final_score'],
                    r['avg_normalized_cost'],
                    r['original_cost'],
                    r['cost_effectiveness_ratio']
                ])

        print(f"[OK] 效费比CSV已保存至: {output_file}")
        return output_file

    def create_detailed_chart(self, results, save_path='document/result'):
        """生成更详细的图表，包含各指标的详细信息"""

        os.makedirs(save_path, exist_ok=True)

        # 创建子图
        fig, axes = plt.subplots(2, 2, figsize=(16, 12))

        # 图1: 阶段得分 vs 最终得分对比
        ax1 = axes[0, 0]
        operation_ids = [r['operation_id'] for r in results]
        stage_scores = [r['stage_score'] for r in results]
        final_scores = [r['final_score'] for r in results]

        x = np.arange(len(operation_ids))
        width = 0.35

        bars1 = ax1.bar(x - width/2, stage_scores, width, label='阶段得分', color='#3498db', alpha=0.8)
        bars2 = ax1.bar(x + width/2, final_scores, width, label='最终得分', color='#e74c3c', alpha=0.8)

        ax1.set_xlabel('实验编号')
        ax1.set_ylabel('效能得分')
        ax1.set_title('阶段得分 vs 最终得分（含惩罚）')
        ax1.set_xticks(x)
        ax1.set_xticklabels(operation_ids, rotation=45, ha='right')
        ax1.legend()
        ax1.grid(axis='y', alpha=0.3)
        ax1.set_ylim(0, 100)

        # 图2: 崩溃比例与惩罚因子关系
        ax2 = axes[0, 1]
        crash_rates = [r['crash_rate'] for r in results]
        penalties = [r['crash_penalty'] for r in results]

        colors = ['#e74c3c' if p < 1.0 else '#27ae60' for p in penalties]

        bars = ax2.bar(x, penalties, color=colors, alpha=0.8, edgecolor='black', linewidth=0.5)
        ax2.axhline(y=1.0, color='gray', linestyle='--', linewidth=1, alpha=0.7)

        ax2.set_xlabel('实验编号')
        ax2.set_ylabel('惩罚因子')
        ax2.set_title('崩溃比例惩罚因子')
        ax2.set_xticks(x)
        ax2.set_xticklabels(operation_ids, rotation=45, ha='right')
        ax2.set_ylim(0, 1.1)
        ax2.grid(axis='y', alpha=0.3)

        # 添加图例
        from matplotlib.patches import Patch
        legend_elements = [Patch(facecolor='#e74c3c', alpha=0.8, label='受惩罚 (<1.0)'),
                          Patch(facecolor='#27ae60', alpha=0.8, label='无惩罚 (=1.0)')]
        ax2.legend(handles=legend_elements, loc='lower right')

        # 图3: 崩溃比例得分
        ax3 = axes[1, 0]
        colors3 = ['#e74c3c' if c < 70 else '#27ae60' for c in crash_rates]
        bars3 = ax3.bar(x, crash_rates, color=colors3, alpha=0.8, edgecolor='black', linewidth=0.5)
        ax3.axhline(y=70, color='#e67e22', linestyle='--', linewidth=2, label='惩罚阈值 (70分)')

        ax3.set_xlabel('实验编号')
        ax3.set_ylabel('崩溃比例得分')
        ax3.set_title('各实验崩溃比例得分')
        ax3.set_xticks(x)
        ax3.set_xticklabels(operation_ids, rotation=45, ha='right')
        ax3.set_ylim(0, 100)
        ax3.legend()
        ax3.grid(axis='y', alpha=0.3)

        # 图4: 得分差异（惩罚导致的降低）
        ax4 = axes[1, 1]
        score_differences = [r['stage_score'] - r['final_score'] for r in results]
        colors4 = ['#e74c3c' if d > 0 else '#27ae60' for d in score_differences]

        bars4 = ax4.bar(x, score_differences, color=colors4, alpha=0.8, edgecolor='black', linewidth=0.5)
        ax4.axhline(y=0, color='gray', linestyle='-', linewidth=1)

        ax4.set_xlabel('实验编号')
        ax4.set_ylabel('得分降低值')
        ax4.set_title('惩罚导致的得分降低')
        ax4.set_xticks(x)
        ax4.set_xticklabels(operation_ids, rotation=45, ha='right')
        ax4.grid(axis='y', alpha=0.3)

        plt.suptitle('军事通信装备效能评估 - 惩罚模型详细分析', fontsize=16, fontweight='bold', y=1.02)
        plt.tight_layout()

        # 保存
        output_file = os.path.join(save_path, 'effectiveness_evaluation_detailed.png')
        plt.savefig(output_file, dpi=150, bbox_inches='tight',
                    facecolor='white', edgecolor='none')
        plt.close()

        print(f"[OK] 详细图表已保存至: {output_file}")

        return output_file

    def export_to_csv(self, results, save_path='document/result'):
        """导出结果到CSV文件"""
        import csv

        os.makedirs(save_path, exist_ok=True)

        csv_file = os.path.join(save_path, 'effectiveness_evaluation_results.csv')

        with open(csv_file, 'w', newline='', encoding='utf-8-sig') as f:
            writer = csv.writer(f)

            # 写入表头
            writer.writerow([
                '实验ID', '崩溃比例得分', '惩罚因子', '阶段得分', '最终得分',
                '得分降低', '是否受惩罚'
            ])

            # 写入数据
            for r in results:
                writer.writerow([
                    r['operation_id'],
                    round(r['crash_rate'], 2),
                    round(r['crash_penalty'], 4),
                    round(r['stage_score'], 4),
                    round(r['final_score'], 4),
                    round(r['stage_score'] - r['final_score'], 2),
                    '是' if r['crash_penalty'] < 1.0 else '否'
                ])

        print(f"[OK] CSV结果已保存至: {csv_file}")
        return csv_file

    def close(self):
        """关闭数据库连接"""
        if self.connection:
            self.connection.close()
            print("\n数据库连接已关闭")


def main():
    """主函数"""
    print("="*80)
    print("军事通信装备效能评估 - 带崩溃比例惩罚模型")
    print("="*80)
    print("\n惩罚模型配置（基于文献）:")
    print("  - 文献来源: 王思远, 张目. 基于惩罚与激励的TOPSIS多属性决策方法")
    print("  - 核心惩罚指标: 崩溃比例得分 (reliability_crash_rate_qt)")
    print("  - 惩罚阈值 s: 70 分 (崩溃比例得分低于70分开始惩罚)")
    print("  - 惩罚系数 m: 0.8 (0<m≤1, 越小惩罚越重)")
    print("  - 公式: F_i = 1, x ≥ 70; F_i = (x/70)×0.8, x < 70")
    print("  - 最终得分: Sfinal = Sstage × min(F_i)")
    print("  - 数据源: military_operation_effect_score + ahp_final_weights")
    print("="*80)

    evaluator = EffectivenessEvaluationWithPenalty()

    try:
        # 计算效能得分
        results = evaluator.calculate_effectiveness()

        if not results:
            print("\n[错误] 无法计算效能得分，请检查数据!")
            return

        # 获取成本数据并进行max-min归一化
        cost_data = evaluator.get_cost_evaluation()
        normalized_costs = evaluator.normalize_cost_maxmin(cost_data)

        # 计算效费比
        results = evaluator.calculate_cost_effectiveness(results, normalized_costs)

        # 显示汇总
        evaluator.display_summary(results)

        # 生成可视化图表
        evaluator.visualize_results(results)
        evaluator.create_detailed_chart(results)
        # 绘制效费比对比图
        evaluator.visualize_cost_effectiveness(results)

        # 导出CSV
        evaluator.export_to_csv(results)
        evaluator.export_cost_effectiveness_csv(results)

        print("\n" + "="*80)
        print("计算完成!")
        print("="*80)

    except Error as e:
        print(f"\n[数据库错误] {e}")
    finally:
        evaluator.close()


if __name__ == '__main__':
    main()
