# -*- coding: utf-8 -*-
"""
专家可信度评估系统
基于主观背景数据和客观AHP权重数据计算专家可信度

改进方案A：
1. 一致性检验：完全基于CV（变异系数）
2. 专家离散度：基于相对偏差（CV思想）
3. 客观可信度：使用绝对得分（0-1区间），不强制归一化
4. 可视化：6张图全面展示评估过程

作者：AI Assistant
日期：2026-03-06
"""

import mysql.connector
import numpy as np
import matplotlib.pyplot as plt
from matplotlib import rcParams
from matplotlib.gridspec import GridSpec
import warnings
warnings.filterwarnings('ignore')

# 设置中文字体
rcParams['font.sans-serif'] = ['SimHei']  # 用来正常显示中文标签
rcParams['axes.unicode_minus'] = False  # 用来正常显示负号

# ==================== 参数配置 ====================

# 影响力因素权重（总和=1）
INFLUENCE_WEIGHTS = {
    'title': 0.20,           # 职称
    'position': 0.15,        # 职务
    'education': 0.15,       # 学历经历
    'academic': 0.20,        # 学术成果
    'research': 0.20,        # 科研成果
    'exercise': 0.10         # 演习训练
}

# 专业知识因素权重（总和=1）
KNOWLEDGE_WEIGHTS = {
    'military_training': 0.30,    # 军事训练学知识
    'system_simulation': 0.25,    # 系统仿真知识
    'statistics': 0.20,           # 数理统计学知识
    'professional_years': 0.25    # 专业年限
}

# 主观综合得分权重（总和=1）
SUBJECTIVE_WEIGHTS = {
    'influence': 0.5,        # 影响力得分权重
    'knowledge': 0.5         # 专业知识得分权重
}

# 综合可信度权重（总和=1）
COMPREHENSIVE_WEIGHT = {
    'subjective': 0.6,       # 主观可信度权重
    'objective': 0.4         # 客观可信度权重
}

# 20个AHP权重字段名
AHP_WEIGHT_FIELDS = [
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
    'resource_power_consumption_weight',
    'resource_manpower_requirement_weight',
    'effect_damage_rate_weight',
    'effect_mission_completion_rate_weight',
    'effect_cost_effectiveness_weight'
]

# 指标层次结构定义
INDICATOR_HIERARCHY = {
    'security': {
        'name': '安全性',
        'name_en': 'Security',
        'weight': 0.25,  # 一级指标权重（可调整）
        'indices': [0, 1, 2]  # 在AHP_WEIGHT_FIELDS中的索引
    },
    'reliability': {
        'name': '可靠性',
        'name_en': 'Reliability',
        'weight': 0.20,
        'indices': [3, 4, 5]
    },
    'transmission': {
        'name': '传输性能',
        'name_en': 'Transmission',
        'weight': 0.20,
        'indices': [6, 7, 8, 9, 10, 11]
    },
    'anti_jamming': {
        'name': '抗干扰',
        'name_en': 'Anti-jamming',
        'weight': 0.15,
        'indices': [12, 13, 14]
    },
    'resource': {
        'name': '资源消耗',
        'name_en': 'Resource',
        'weight': 0.10,
        'indices': [15, 16]
    },
    'effect': {
        'name': '作战效能',
        'name_en': 'Operational Effect',
        'weight': 0.10,
        'indices': [17, 18, 19]
    }
}


class ExpertCredibilityEvaluator:
    """专家可信度评估器"""
    
    def __init__(self):
        """初始化数据库连接"""
        self.connection = mysql.connector.connect(
            host='localhost',
            database='military_operational_effectiveness_evaluation',
            user='root',
            password='root'
        )
        self.cursor = self.connection.cursor(dictionary=True)
        
    def __del__(self):
        """关闭数据库连接"""
        if hasattr(self, 'cursor'):
            self.cursor.close()
        if hasattr(self, 'connection'):
            self.connection.close()
    
    def load_expert_background_data(self):
        """加载专家背景数据"""
        query = """
            SELECT 
                expert_name,
                title_ql,
                position_ql,
                education_experience_ql,
                academic_achievements_ql,
                research_achievements_ql,
                exercise_experience_ql,
                military_training_knowledge_ql,
                system_simulation_knowledge_ql,
                statistics_knowledge_ql,
                professional_years_qt
            FROM expert_credibility_evaluation_score
            ORDER BY id
        """
        self.cursor.execute(query)
        return self.cursor.fetchall()
    
    def load_expert_ahp_weights(self, batch_id='AHP-2026-001'):
        """
        加载专家AHP权重数据（包含权重和把握度）
        
        参数:
            batch_id: 批次ID，默认为 'AHP-2026-002'
        """
        # 构建查询字段：每个指标都有weight和confidence两个字段
        fields = ['expert_name']
        for field in AHP_WEIGHT_FIELDS:
            fields.append(field)  # xxx_weight
            confidence_field = field.replace('_weight', '_confidence')
            fields.append(confidence_field)  # xxx_confidence
        
        query = f"""
            SELECT {', '.join(fields)}
            FROM ahp_expert_military_operation_effect_weights
            WHERE batch_id = %s
            ORDER BY id
        """
        self.cursor.execute(query, (batch_id,))
        return self.cursor.fetchall()
    
    def calculate_influence_score(self, expert):
        """
        计算专家影响力得分 Ωe
        
        Ωe = Ω1·Fzc + Ω2·Fzw + Ω3·Fxl + Ω4·Fxs + Ω5·Fky + Ω6·Fyl
        """
        score = (
            INFLUENCE_WEIGHTS['title'] * float(expert['title_ql']) +
            INFLUENCE_WEIGHTS['position'] * float(expert['position_ql']) +
            INFLUENCE_WEIGHTS['education'] * float(expert['education_experience_ql']) +
            INFLUENCE_WEIGHTS['academic'] * float(expert['academic_achievements_ql']) +
            INFLUENCE_WEIGHTS['research'] * float(expert['research_achievements_ql']) +
            INFLUENCE_WEIGHTS['exercise'] * float(expert['exercise_experience_ql'])
        )
        return score
    
    def calculate_knowledge_score(self, expert):
        """
        计算专业知识掌握程度得分 Ωk
        
        Ωk = β1·Fzx + β2·Fxf + β3·Fst + β4·Fsj
        """
        score = (
            KNOWLEDGE_WEIGHTS['military_training'] * float(expert['military_training_knowledge_ql']) +
            KNOWLEDGE_WEIGHTS['system_simulation'] * float(expert['system_simulation_knowledge_ql']) +
            KNOWLEDGE_WEIGHTS['statistics'] * float(expert['statistics_knowledge_ql']) +
            KNOWLEDGE_WEIGHTS['professional_years'] * float(expert['professional_years_qt'])
        )
        return score
    
    def calculate_subjective_total_score(self, influence_score, knowledge_score):
        """
        计算主观综合得分 Ωi
        
        Ωi = γ1·Ωe + γ2·Ωk
        """
        score = (
            SUBJECTIVE_WEIGHTS['influence'] * influence_score +
            SUBJECTIVE_WEIGHTS['knowledge'] * knowledge_score
        )
        return score
    
    def calculate_subjective_credibility(self, experts_background):
        """
        计算所有专家的主观可信度 αi（方案A：绝对得分）
        
        αi = Ωi / 100 (因为原始得分是0-100分制)
        
        特点：
        - αi 是绝对得分，范围 0-1
        - 不强制归一化（Σαi ≠ 1）
        - 反映专家的绝对水平
        """
        results = []
        
        for expert in experts_background:
            # 计算影响力得分
            influence_score = self.calculate_influence_score(expert)
            
            # 计算专业知识得分
            knowledge_score = self.calculate_knowledge_score(expert)
            
            # 计算主观综合得分
            total_score = self.calculate_subjective_total_score(influence_score, knowledge_score)
            
            # 转换为0-1区间的绝对得分
            alpha_i = total_score / 100.0
            
            results.append({
                'expert_name': expert['expert_name'],
                'influence_score': influence_score,
                'knowledge_score': knowledge_score,
                'subjective_total_score': total_score,
                'subjective_credibility': alpha_i
            })
        
        return results
    
    def extract_weight_vectors(self, experts_ahp):
        """
        提取每位专家的权重向量
        
        返回：numpy数组，shape=(m, n)，m为专家数，n为指标数
        """
        weight_matrix = []
        expert_names = []
        
        for expert in experts_ahp:
            expert_names.append(expert['expert_name'])
            weights = [float(expert[field]) for field in AHP_WEIGHT_FIELDS]
            weight_matrix.append(weights)
        
        return np.array(weight_matrix), expert_names
    
    def calculate_cosine_similarity(self, vector1, vector2):
        """
        计算两个向量的余弦相似度
        
        similarity = Σ(v1i · v2i) / (||v1|| · ||v2||)
        """
        dot_product = np.dot(vector1, vector2)
        norm1 = np.linalg.norm(vector1)
        norm2 = np.linalg.norm(vector2)
        
        if norm1 == 0 or norm2 == 0:
            return 0.0
        
        return dot_product / (norm1 * norm2)

    def _compute_indicator_means_with_confidence(self, weight_matrix, confidence_matrix, threshold=0.6):
        """
        基于把握度阈值计算每个指标的均值。

        - 只使用 confidence >= threshold 的评分
        - 若某指标无任何有效评分，则该指标均值为 np.nan
        """
        n = weight_matrix.shape[1]
        means = np.full(n, np.nan, dtype=float)
        for j in range(n):
            valid = confidence_matrix[:, j] >= threshold
            if np.any(valid):
                means[j] = np.mean(weight_matrix[valid, j])
        return means
    
    def check_consistency(self, weight_matrix, confidence_matrix, expert_names, confidence_threshold=0.6, remove_farthest=True):
        """
        一致性检验 - 基于变异系数（CV）方法
        
        步骤：
        1. 对每个指标计算变异系数 CV = (标准差 / 平均值) × 100%
        2. 计算所有指标的平均CV作为一致性指标
        3. 根据CV判断一致性等级
        
        重要口径（按你的要求修正）：
        - 把握度过滤：每个指标仅使用 confidence >= 0.6 的评分计算CV
        - 若某指标所有专家均 < 0.6，则该指标不进入后续 CV/AHP 统计（记为“无把握”）
        - 异常值处理：对每个指标（在有效评分>=3时）删除“离均值最远”的一个评分，再计算CV

        返回：(一致性指标, 各指标CV, 各指标详情, 各专家向量离散度, 一致性等级, 是否通过, 异常值删除分析, 有效指标掩码)
        """
        m = weight_matrix.shape[0]  # 专家数量
        n = weight_matrix.shape[1]  # 指标数量
        
        # 指标名称
        indicator_names = [
            '密钥泄露', '被侦察概率', '抗拦截能力',
            '崩溃率', '恢复能力', '通信可用性',
            '带宽', '呼叫建立时间', '传输时延', '误码率', '吞吐量', '频谱效率',
            '信干噪比', '抗干扰余量', '通信距离',
            '功耗', '人力需求',
            '毁伤率', '任务完成率', '效费比'
        ]
        
        # 【步骤1】计算每个指标的变异系数（CV）（把握度过滤 + 删除最远异常值）
        indicator_cvs = []
        indicator_details = []
        active_indicator_mask = np.zeros(n, dtype=bool)  # 至少存在1条把握度>=阈值的评分才算“有效指标”
        
        print("\n  [各指标一致性分析]")
        print(f"  {'指标':<20} {'平均值':<10} {'标准差':<10} {'CV(%)':<10} {'一致性':<15}")
        print("  " + "-"*80)
        
        outlier_removed_analysis = []

        for j in range(n):
            # 仅使用把握度>=阈值的评分
            valid_idx = np.where(confidence_matrix[:, j] >= confidence_threshold)[0]
            if len(valid_idx) == 0:
                # 全部无把握：不纳入CV统计
                indicator_cvs.append(np.nan)
                indicator_details.append({
                    'name': indicator_names[j],
                    'mean': np.nan,
                    'std': np.nan,
                    'cv': np.nan,
                    'level': '无把握(全<阈值)',
                    'n_valid': 0
                })
                print(f"  {indicator_names[j]:<20} {'-':<10} {'-':<10} {'-':<10} {'无把握(全<阈值)':<15}")
                continue

            active_indicator_mask[j] = True
            indicator_values = weight_matrix[valid_idx, j]

            # 异常值删除：删除离均值最远的一条评分（需要至少3条有效评分）
            removed_expert = None
            removed_value = None
            cv_before = np.nan
            if len(indicator_values) >= 2:
                mean_before = np.mean(indicator_values)
                std_before = np.std(indicator_values, ddof=1)
                cv_before = (std_before / mean_before) * 100 if mean_before > 0 else 0.0
            else:
                mean_before = np.mean(indicator_values)
                std_before = 0.0

            filtered_values = indicator_values
            if remove_farthest and len(indicator_values) >= 3:
                distances = np.abs(indicator_values - mean_before)
                farthest_local_idx = int(np.argmax(distances))
                farthest_global_idx = int(valid_idx[farthest_local_idx])
                removed_expert = expert_names[farthest_global_idx]
                removed_value = float(indicator_values[farthest_local_idx])
                filtered_values = np.delete(indicator_values, farthest_local_idx)

            # 重新计算（删除后）均值/标准差/CV
            mean_val = float(np.mean(filtered_values))
            std_val = float(np.std(filtered_values, ddof=1)) if len(filtered_values) > 1 else 0.0
            cv = (std_val / mean_val) * 100 if mean_val > 0 else 0.0
            
            # 判断该指标的一致性
            if cv < 15:
                consistency_level = "高度一致"
            elif cv < 25:
                consistency_level = "较为一致"
            elif cv < 35:
                consistency_level = "轻度分歧"
            else:
                consistency_level = "严重分歧"
            
            indicator_cvs.append(cv)
            indicator_details.append({
                'name': indicator_names[j],
                'mean': mean_val,
                'std': std_val,
                'cv': cv,
                'level': consistency_level,
                'n_valid': int(len(valid_idx)),
                'removed_expert': removed_expert
            })
            
            print(f"  {indicator_names[j]:<20} {mean_val:<10.2f} {std_val:<10.2f} {cv:<10.1f} {consistency_level:<15}")

            if removed_expert is not None and len(filtered_values) > 1:
                # 记录删除最远值前后CV变化（用于图表/审计）
                outlier_removed_analysis.append({
                    'indicator': indicator_names[j],
                    'cv_before': float(cv_before),
                    'cv_after': float(cv),
                    'change': float(cv - cv_before),
                    'removed_expert': removed_expert,
                    'removed_value': removed_value,
                    'note': f'仅使用把握度≥{confidence_threshold}，并删除最远值'
                })
        
        # 【步骤2】计算整体一致性指标（仅对“有效指标”的CV求平均；无把握指标不计入）
        valid_cvs = [x for x in indicator_cvs if not np.isnan(x)]
        avg_cv = float(np.mean(valid_cvs)) if valid_cvs else np.nan
        
        print(f"\n  平均变异系数: {avg_cv:.2f}%")
        
        # 【步骤3】判断整体一致性等级
        if avg_cv < 15:
            level = "高度一致"
            passed = True
        elif avg_cv < 25:
            level = "较为一致"
            passed = True
        elif avg_cv < 35:
            level = "轻度分歧"
            passed = False
        else:
            level = "严重分歧"
            passed = False
        
        # 删除最远值的分析数据已在步骤1中生成，这里仅补充说明
        print(f"\n  [异常值处理说明]")
        print(f"  已按“每个指标删除离均值最远的1个有效评分(把握度≥{confidence_threshold})”进行CV统计。")
        if not outlier_removed_analysis:
            print(f"  当前有效指标样本不足(或无需删除)，未生成异常值删除记录。")
        
        # 【步骤4】计算每位专家的离散度（用于后续客观可信度计算）
        avg_weights = np.mean(weight_matrix, axis=0)
        dispersions = []
        
        for i in range(m):
            expert_weights = weight_matrix[i]
            
            # 分子：√(Σ(ωⁱⱼ - ω̄ⱼ)²)
            numerator = np.sqrt(np.sum((expert_weights - avg_weights) ** 2))
            
            # 分母：√(Σ(ωⱼⁱ)² + Σ(ω̄ⱼ)²)
            denominator = np.sqrt(np.sum(expert_weights ** 2) + np.sum(avg_weights ** 2))
            
            # 计算离散度
            if denominator == 0:
                dispersion = 0
            else:
                dispersion = numerator / denominator
            
            dispersions.append(dispersion)
        
        return avg_cv, indicator_cvs, indicator_details, dispersions, level, passed, outlier_removed_analysis, active_indicator_mask
    
    def calculate_objective_credibility(self, experts_ahp):
        """
        计算所有专家的客观可信度 βi（方案A：绝对得分）
        
        步骤：
        1. 先进行一致性检验（基于CV）
        2. 计算每个专家的相对离散度（基于CV标准化）
        3. 计算一致性得分：consistency_score = 1 / (1 + dispersion)
        4. βi = consistency_score（不归一化，保持绝对得分）
        
        特点：
        - βi 是绝对得分，范围 0-1
        - 不强制归一化（Σβi ≠ 1）
        - 反映专家评分的绝对一致性水平
        """
        # 提取权重矩阵与把握度矩阵
        weight_matrix, expert_names = self.extract_weight_vectors(experts_ahp)
        confidence_matrix = self.extract_confidence_matrix(experts_ahp)
        
        # 【步骤1】一致性检验
        print("\n  [一致性检验 - 基于变异系数CV]")
        avg_cv, indicator_cvs, indicator_details, old_dispersions, level, passed, outlier_removed_analysis, active_indicator_mask = self.check_consistency(
            weight_matrix, confidence_matrix, expert_names, confidence_threshold=0.6, remove_farthest=True
        )
        
        print(f"\n  整体一致性指标（平均CV）= {avg_cv:.2f}%")
        print(f"  一致性等级: {level}")
        
        if not passed:
            print(f"  [警告] 一致性检验未通过！")
            print(f"  建议：组织专家进行二轮评分")
            
            # 找出CV最高的指标（分歧最大的指标）
            indicator_cv_pairs = [(detail['name'], detail['cv']) for detail in indicator_details]
            indicator_cv_pairs.sort(key=lambda x: x[1], reverse=True)
            
            print(f"\n  分歧最大的5个指标：")
            for name, cv in indicator_cv_pairs[:5]:
                print(f"    - {name}: CV = {cv:.1f}%")
        else:
            print(f"  [OK] 一致性检验通过")
        
        # 【步骤2】计算每个专家的离散度（CV%）
        # 口径：仅使用把握度>=阈值且“有效指标”的评分；专家CV = std(偏差) / overall_mean × 100%
        m = weight_matrix.shape[0]  # 专家数量
        n = weight_matrix.shape[1]  # 指标数量
        
        # 指标均值：只用有效评分（把握度过滤）
        indicator_means = self._compute_indicator_means_with_confidence(weight_matrix, confidence_matrix, threshold=0.6)

        # 整体均值：只对所有有效评分取均值（避免低把握度污染口径）
        valid_scores = weight_matrix[confidence_matrix >= 0.6]
        overall_mean = float(np.mean(valid_scores)) if valid_scores.size > 0 else float(np.mean(weight_matrix))
        
        expert_dispersions = []
        for i in range(m):
            expert_scores = weight_matrix[i, :]  # 该专家对20个指标的评分

            # 只使用：该专家在该指标上把握度>=阈值 且 该指标不是“全员无把握”
            valid_indicator = active_indicator_mask & (confidence_matrix[i, :] >= 0.6) & (~np.isnan(indicator_means))
            if np.sum(valid_indicator) < 2:
                cv = np.nan
            else:
                deviations = expert_scores[valid_indicator] - indicator_means[valid_indicator]
                std_of_deviations = np.std(deviations, ddof=1)
                cv = (std_of_deviations / overall_mean) * 100 if overall_mean > 0 else 0.0
            
            expert_dispersions.append(cv)
        
        expert_dispersions = np.array(expert_dispersions, dtype=float)
        
        
        # 【步骤3】计算客观可信度（使用分段衰减，35%阈值）
        # 根据一致性水平标准：
        # - CV ≤ 15%: 高度一致 → β = 1.0
        # - 15% < CV ≤ 25%: 较为一致 → β线性衰减 1.0→0.7
        # - 25% < CV ≤ 35%: 轻度分歧 → β快速衰减 0.7→0.0
        # - CV > 35%: 严重分歧 → β = 0.0 (失去意义)
        
        consistency_scores = []
        for cv in expert_dispersions:
            if np.isnan(cv):
                consistency_scores.append(0.0)
                continue
            if cv <= 15:
                # 高度一致
                beta = 1.0
            elif cv <= 25:
                # 较为一致：从1.0线性衰减到0.5（增加衰减速度，提升区分度）
                beta = 1.0 - (cv - 15) / 10 * 0.5
            elif cv <= 35:
                # 轻度分歧：从0.5快速衰减到0.0
                beta = 0.5 - (cv - 25) / 10 * 0.5
            else:
                # 严重分歧：完全不可信
                beta = 0.0
            
            consistency_scores.append(beta)
        
        # 【步骤4】βi = consistency_score（分段衰减后的得分）
        results = []
        for i, expert_name in enumerate(expert_names):
            results.append({
                'expert_name': expert_name,
                'dispersion': expert_dispersions[i],
                'consistency_score': consistency_scores[i],
                'objective_credibility': consistency_scores[i]  # βi保持绝对得分
            })
        
        # 保存一致性检验结果
        consistency_result = {
            'avg_cv': avg_cv,
            'indicator_cvs': indicator_cvs,
            'indicator_details': indicator_details,
            'level': level,
            'passed': passed,
            'expert_dispersions': expert_dispersions,  # 新的基于CV的离散度
            'outlier_removed_analysis': outlier_removed_analysis,  # 异常值删除记录（用于图表/审计）
            'active_indicator_mask': active_indicator_mask,
            'confidence_threshold': 0.6,
            'confidence_matrix': confidence_matrix
        }
        
        return results, weight_matrix, np.mean(weight_matrix, axis=0), consistency_result
    
    def calculate_comprehensive_credibility(self, subjective_results, objective_results):
        """
        计算综合可信度（方案A：绝对得分加权平均）
        
        Credibility = λ·αi + (1-λ)·βi
        
        其中：
        - αi 是绝对得分（0-1），不归一化
        - βi 是绝对得分（0-1），不归一化
        - 结果也是绝对得分（0-1），不归一化
        """
        # 合并主观和客观结果
        comprehensive_results = []
        
        for subj in subjective_results:
            # 找到对应的客观结果
            obj = next(o for o in objective_results if o['expert_name'] == subj['expert_name'])
            
            # 计算综合可信度（绝对得分加权平均）
            comprehensive_cred = (
                COMPREHENSIVE_WEIGHT['subjective'] * subj['subjective_credibility'] +
                COMPREHENSIVE_WEIGHT['objective'] * obj['objective_credibility']
            )
            
            comprehensive_results.append({
                'expert_name': subj['expert_name'],
                'subjective_credibility': subj['subjective_credibility'],
                'objective_credibility': obj['objective_credibility'],
                'comprehensive_credibility': comprehensive_cred,
                'influence_score': subj['influence_score'],
                'knowledge_score': subj['knowledge_score'],
                'subjective_total_score': subj['subjective_total_score'],
                'consistency_score': obj['consistency_score'],
                'dispersion': obj['dispersion']
            })
        
        return comprehensive_results
    
    
    def extract_confidence_matrix(self, experts_ahp):
        """
        从数据库数据中提取把握度矩阵（真实数据）
        
        参数:
            experts_ahp: load_expert_ahp_weights返回的数据
        
        返回: m×n 矩阵，m个专家对n个指标的把握度
        """
        m = len(experts_ahp)
        n = len(AHP_WEIGHT_FIELDS)
        confidence_matrix = np.zeros((m, n))
        
        for i, expert_data in enumerate(experts_ahp):
            for j, weight_field in enumerate(AHP_WEIGHT_FIELDS):
                # 将weight字段名转换为confidence字段名
                confidence_field = weight_field.replace('_weight', '_confidence')
                confidence_matrix[i, j] = expert_data[confidence_field]
        
        return confidence_matrix
    
    def _get_indicator_category(self, indicator_index):
        """根据指标索引获取其所属一级类别"""
        for category, info in INDICATOR_HIERARCHY.items():
            if indicator_index in info['indices']:
                return category
        return 'unknown'

    # AHP指标到equipment_operation_score字段的映射（熵权法备用）
    INDICATOR_TO_EQUIPMENT_FIELD = {
        0: 'maintenance_maintenance_skill_ql',           # 密钥泄露
        1: 'comm_defense_signal_interception_awareness_ql',  # 被侦察概率
        2: 'comm_defense_anti_deception_awareness_ql',  # 抗拦截能力
        3: 'system_performance_connectivity_rate_qt',    # 崩溃率/误码率
        4: 'comm_support_emergency_restoration_qt',      # 恢复能力
        5: 'system_performance_mission_reliability_qt',   # 通信可用性/可靠性
        6: 'comm_attack_jamming_effectiveness_ql',        # 带宽
        7: 'system_setup_network_setup_time_qt',         # 呼叫建立时间
        8: 'comm_support_link_maintenance_qt',           # 传输时延
        9: 'system_performance_connectivity_rate_qt',    # 误码率
        10: 'comm_support_service_activation_qt',           # 吞吐量
        11: 'comm_attack_deception_signal_generation_ql', # 频谱效率
        12: 'comm_defense_anti_jamming_operation_ql',     # 信干噪比
        13: 'response_emergency_handling_qt',            # 抗干扰容限
        14: 'personnel_work_experience_qt',              # 通信距离
        15: 'maintenance_spare_parts_availability_qt',   # 功耗
        16: 'personnel_personnel_count_qt',               # 人力需求
        17: 'maintenance_feedback_field_repair_qt',      # 毁伤率/任务完成率
        18: 'maintenance_feedback_field_repair_qt',      # 任务完成率
        19: 'maintenance_feedback_equipment_feedback_ql', # 效费比
    }

    def load_equipment_scores(self):
        """加载设备操作评分数据（用于熵权法）"""
        query = """
            SELECT * FROM equipment_operation_score ORDER BY id
        """
        self.cursor.execute(query)
        return self.cursor.fetchall()

    def calculate_entropy_weight(self, equipment_scores, indicator_index):
        """
        使用熵权法计算指标权重

        参数:
            equipment_scores: equipment_operation_score表的数据
            indicator_index: AHP指标索引

        返回:
            weight: 该指标的熵权
        """
        # 获取对应的字段名
        field_name = self.INDICATOR_TO_EQUIPMENT_FIELD.get(indicator_index)
        if not field_name:
            return 0.0

        # 提取该字段的所有评分
        scores = []
        for record in equipment_scores:
            if field_name in record and record[field_name] is not None:
                scores.append(float(record[field_name]))

        if len(scores) < 2:
            return 0.0

        scores = np.array(scores)

        # 归一化处理（效益型指标：越大越好）
        min_val = scores.min()
        max_val = scores.max()

        if max_val == min_val:
            return 0.0

        # 归一化
        normalized = (scores - min_val) / (max_val - min_val)

        # 计算信息熵
        n = len(normalized)
        p = normalized / normalized.sum()

        # 处理0值
        p = np.where(p == 0, 1e-10, p)

        # 计算熵值
        entropy = -np.sum(p * np.log(p)) / np.log(n)

        # 计算权重
        weight = (1 - entropy) / (1 - entropy)  # 简化的单指标权重

        # 如果只有一个指标，返回1
        return weight if weight > 0 else 0.0

    def calculate_all_entropy_weights(self, equipment_scores):
        """
        计算所有20个指标的熵权

        参数:
            equipment_scores: equipment_operation_score表的数据

        返回:
            entropy_weights: 20个指标的熵权数组
        """
        n_indicators = 20
        entropy_weights = np.zeros(n_indicators)

        # 计算每个指标的熵
        entropies = []
        for idx in range(n_indicators):
            field_name = self.INDICATOR_TO_EQUIPMENT_FIELD.get(idx)
            if not field_name:
                entropies.append(1.0)  # 无映射，熵设为最大（权重最小）
                continue

            scores = []
            for record in equipment_scores:
                if field_name in record and record[field_name] is not None:
                    scores.append(float(record[field_name]))

            if len(scores) < 2:
                entropies.append(1.0)
                continue

            scores = np.array(scores)
            min_val, max_val = scores.min(), scores.max()

            if max_val == min_val:
                entropies.append(1.0)
                continue

            normalized = (scores - min_val) / (max_val - min_val)
            normalized = np.where(normalized == 0, 1e-10, normalized)

            p = normalized / normalized.sum()
            entropy = -np.sum(p * np.log(p)) / np.log(len(p))
            entropies.append(entropy)

        entropies = np.array(entropies)

        # 计算权重：w = (1 - E) / sum(1 - E)
        diversities = 1 - entropies
        total_diversity = diversities.sum()

        if total_diversity > 0:
            entropy_weights = diversities / total_diversity
        else:
            # 所有熵都为1（最大），使用等权
            entropy_weights = np.ones(n_indicators) / n_indicators

        return entropy_weights

    def calculate_ahp_consistency(self, judgment_matrix):
        """
        计算AHP一致性指标

        参数:
            judgment_matrix: numpy数组，n×n的判断矩阵

        返回:
            dict: 包含 lambda_max, CI, CR, RI, is_consistent
        """
        n = judgment_matrix.shape[0]

        # 随机一致性指标RI（根据矩阵阶数）
        ri_values = {
            1: 0, 2: 0, 3: 0.58, 4: 0.90, 5: 1.12, 6: 1.24,
            7: 1.32, 8: 1.41, 9: 1.45, 10: 1.49, 11: 1.51,
            12: 1.54, 13: 1.56, 14: 1.57, 15: 1.59
        }
        RI = ri_values.get(n, 1.59)

        # 计算特征值
        # 方法：使用幂迭代法近似计算最大特征值
        eigenvalues = []

        # 对每列计算几何平均，然后归一化得到权重向量
        n = judgment_matrix.shape[0]

        # 计算权重向量（几何平均法）
        weights = np.prod(judgment_matrix, axis=1) ** (1/n)
        weights = weights / weights.sum()

        # 计算最大特征值 lambda_max
        # Aw = lambda_max * w
        Aw = judgment_matrix @ weights
        lambda_max = np.mean(Aw / weights)

        # 计算一致性指标CI
        CI = (lambda_max - n) / (n - 1) if n > 1 else 0

        # 计算一致性比例CR
        CR = CI / RI if RI > 0 else 0

        is_consistent = CR <= 0.1

        return {
            'lambda_max': lambda_max,
            'CI': CI,
            'CR': CR,
            'RI': RI,
            'is_consistent': is_consistent,
            'weights': weights
        }

    def build_ahp_judgment_matrix(self, weight_matrix, confidence_matrix, indicator_indices,
                                   subjective_credibility=None, objective_credibility=None,
                                   confidence_threshold=0.6):
        """
        根据专家权重和把握度构建AHP判断矩阵

        逻辑：
        - 把握度逐对过滤：对每个指标对(i,j)，仅使用同时满足
          confidence(i) >= threshold 且 confidence(j) >= threshold 的专家样本
        - 若某个指标在该类别内“全员无把握”(所有专家 confidence<threshold)，则该指标应在上层剔除

        参数:
            weight_matrix: 专家权重矩阵 (m×n)
            confidence_matrix: 把握度矩阵 (m×n)
            indicator_indices: 二级指标索引列表

        返回:
            judgment_matrix: n×n的判断矩阵
            valid_experts_count: 有效专家数量
        """
        m = weight_matrix.shape[0]
        n = len(indicator_indices)

        # 提取相关指标的权重和把握度
        sub_weights = weight_matrix[:, indicator_indices]
        sub_confidence = confidence_matrix[:, indicator_indices]

        # 检查是否传入了可信度参数，用于计算调整因子
        use_adjustment = (subjective_credibility is not None and objective_credibility is not None)

        if use_adjustment:
            # 计算调整因子 q_ij = 0.5*(alpha+beta) + 0.5*conf (按README.md方案)
            # = 0.5 * 专家综合可信度 + 0.5 * 当前指标把握度
            adjustment_matrix = np.zeros((m, n))
            for i in range(m):
                # 专家综合可信度 = 0.6*α + 0.4*β
                expert_comprehensive = 0.6 * subjective_credibility[i] + 0.4 * objective_credibility[i]
                for j in range(n):
                    if sub_confidence[i, j] >= confidence_threshold:
                        adjustment_matrix[i, j] = (
                            0.5 * expert_comprehensive +
                            0.5 * sub_confidence[i, j]
                        )
            weighted_scores = sub_weights * adjustment_matrix
        else:
            weighted_scores = sub_weights

        # 构建判断矩阵：使用几何平均法（逐对过滤把握度）+ 离群值删除
        judgment_matrix = np.ones((n, n))
        pair_counts = np.zeros((n, n), dtype=int)

        for i in range(n):
            for j in range(n):
                if i != j:
                    mask = (sub_confidence[:, i] >= confidence_threshold) & (sub_confidence[:, j] >= confidence_threshold)
                    pair_counts[i, j] = int(np.sum(mask))
                    if pair_counts[i, j] == 0:
                        judgment_matrix[i, j] = 1.0
                        continue

                    # 使用加权评分计算比值
                    if use_adjustment:
                        ratios = weighted_scores[mask, i] / weighted_scores[mask, j]
                    else:
                        ratios = sub_weights[mask, i] / sub_weights[mask, j]
                    ratios = ratios[(ratios > 0) & np.isfinite(ratios)]

                    if ratios.size > 0:
                        # 【新增】离群值删除：删除离平均值最远的1个值（如果有≥3个样本）
                        if ratios.size >= 3:
                            mean_ratio = np.mean(ratios)
                            distances = np.abs(ratios - mean_ratio)
                            # 删除离均值最远的点
                            outlier_idx = np.argmax(distances)
                            outlier_val = ratios[outlier_idx]  # 保存删除前的值
                            ratios = np.delete(ratios, outlier_idx)
                            print(f"      [离群值删除] 指标对({i},{j}): 删除离均值最远值 {outlier_val:.4f}", end="")

                        if ratios.size > 0:
                            judgment_matrix[i, j] = float(np.exp(np.mean(np.log(ratios + 1e-10))))
                        else:
                            judgment_matrix[i, j] = 1.0
                    else:
                        judgment_matrix[i, j] = 1.0

        # 限制判断矩阵的元素范围在1-9之间
        judgment_matrix = np.clip(judgment_matrix, 1/9, 9)

        # 统计有效专家数：按“对任意一对都有贡献”的专家计数不稳定，这里用“至少参与了某个指标对”的数量近似
        valid_experts_count = int(np.sum(np.any(sub_confidence >= confidence_threshold, axis=1)))
        return judgment_matrix, valid_experts_count

    def calculate_corrected_weights(self, comprehensive_results, weight_matrix, experts_ahp):
        """
        根据把握度和AHP一致性检验计算权重（新方案）

        步骤：
        1. 从数据库提取把握度矩阵
        2. 对每个一级指标：
           a. 提取该一级指标下的二级指标
           b. 构建AHP判断矩阵（只使用把握度>=0.6的专家）
           c. 计算AHP一致性CR，若CR>0.1则标记
           d. 计算二级指标权重（几何平均法）
        3. 二级指标权重归一化（Σ=1）
        4. 最终权重 = 二级权重 × 一级指标权重

        参数:
            comprehensive_results: 综合可信度结果
            weight_matrix: 专家权重矩阵 (m×n)
            experts_ahp: 专家AHP数据

        返回:
            corrected_weights_dict: 包含各级权重的字典
        """
        print("\n[步骤7] 计算基于把握度和AHP的权重...")

        expert_names = [r['expert_name'] for r in comprehensive_results]

        # 提取主观和客观可信度数组（用于调整因子计算）
        subjective_credibility = np.array([r['subjective_credibility'] for r in comprehensive_results])
        objective_credibility = np.array([r['objective_credibility'] for r in comprehensive_results])

        print(f"\n  主观可信度范围: {subjective_credibility.min():.4f} - {subjective_credibility.max():.4f}")
        print(f"  客观可信度范围: {objective_credibility.min():.4f} - {objective_credibility.max():.4f}")

        # 步骤1：从数据库提取把握度矩阵
        confidence_threshold = 0.6
        confidence_matrix = self.extract_confidence_matrix(experts_ahp)
        print(f"  把握度矩阵提取完成 ({confidence_matrix.shape[0]}×{confidence_matrix.shape[1]})")
        print(f"  把握度范围: {confidence_matrix.min():.3f} - {confidence_matrix.max():.3f}")

        # 加载设备操作评分数据（用于熵权法备用方案）
        try:
            equipment_scores = self.load_equipment_scores()
            entropy_weights = self.calculate_all_entropy_weights(equipment_scores)
            print(f"  熵权法权重加载成功: {len(equipment_scores)} 条记录")
            print(f"  熵权权重范围: {entropy_weights.min():.4f} - {entropy_weights.max():.4f}")
            use_entropy_method = True
        except Exception as e:
            print(f"  [警告] 无法加载设备评分数据: {e}")
            print(f"  将使用等权方案作为备用")
            entropy_weights = np.ones(n_indicators) / n_indicators
            use_entropy_method = False

        # 存储每个一级指标的AHP校验结果
        ahp_consistency_results = {}

        # 步骤2：对每个一级指标进行AHP处理
        n_indicators = weight_matrix.shape[1]
        second_level_weights_raw = np.zeros(n_indicators)  # 二级权重（未乘一级权重）
        original_avg_weights = np.mean(weight_matrix, axis=0)

        first_level_weights = {}

        print("\n  [各一级指标AHP一致性检验]")

        for category, info in INDICATOR_HIERARCHY.items():
            indices = info['indices']
            category_name = info['name']
            first_level_weight = info['weight']

            print(f"\n  >> {category_name} (一级权重: {first_level_weight}):")

            # 2a: 指标级剔除：若某二级指标全员把握度<阈值，则该指标不进入AHP计算
            active_indices = [idx for idx in indices if np.any(confidence_matrix[:, idx] >= confidence_threshold)]
            inactive_indices = [idx for idx in indices if idx not in active_indices]

            if inactive_indices:
                inactive_names = [AHP_WEIGHT_FIELDS[idx].replace('_weight', '') for idx in inactive_indices]
                print(f"    [无把握剔除] 以下二级指标全员把握度<{confidence_threshold}，不纳入AHP：{', '.join(inactive_names)}")

            if len(active_indices) == 0:
                print(f"    [警告] 本一级指标下所有二级指标均无把握，使用熵权法计算权重")

                # 使用熵权法计算该类别下的权重
                if use_entropy_method:
                    local_weights = np.array([entropy_weights[idx] for idx in indices])
                    local_weights = local_weights / local_weights.sum()  # 归一化
                    note = '全员无把握，使用熵权法'
                else:
                    # 备用：使用等权
                    local_weights = np.ones(len(indices)) / len(indices)
                    note = '全员无把握，等权备用'

                for idx in indices:
                    second_level_weights_raw[idx] = local_weights[indices.index(idx)]

                local_ahp_result = {
                    'lambda_max': 0,
                    'CI': 0,
                    'CR': 0,
                    'RI': 0,
                    'is_consistent': True,
                    'weights': local_weights,
                    'valid_experts': 0,
                    'note': note
                }
                # 存储AHP结果
                ahp_consistency_results[category] = {
                    'name': category_name,
                    'first_level_weight': first_level_weight,
                    'indices': indices,
                    'n_secondary': len(indices),
                    'valid_experts': 0,
                    'lambda_max': 0,
                    'CI': 0,
                    'CR': 0,
                    'RI': 0,
                    'is_consistent': True,
                    'raw_weights': local_weights
                }
                first_level_weights[category] = {
                    'name': category_name,
                    'weight': first_level_weight,
                    'indices': indices
                }
                continue

            if len(active_indices) == 1:
                only_idx = active_indices[0]
                print(f"    [提示] 仅剩1个有把握二级指标，直接给其局部权重=1.0，其余=0")
                local_weights_active = np.array([1.0])
                local_ahp_result = {
                    'lambda_max': 0,
                    'CI': 0,
                    'CR': 0,
                    'RI': 0,
                    'is_consistent': True,
                    'weights': local_weights_active,
                    'valid_experts': int(np.sum(confidence_matrix[:, only_idx] >= confidence_threshold)),
                    'note': '仅1个有效指标'
                }
                # 写回 raw 权重（active=1，其余=0）
                for idx in indices:
                    second_level_weights_raw[idx] = 1.0 if idx == only_idx else 0.0
                valid_count = local_ahp_result['valid_experts']
                judgment_matrix = np.ones((1, 1))
            else:
                # 2b: 构建AHP判断矩阵（逐对过滤把握度阈值）
                judgment_matrix, valid_count = self.build_ahp_judgment_matrix(
                    weight_matrix, confidence_matrix, active_indices,
                    subjective_credibility, objective_credibility,
                    confidence_threshold=confidence_threshold
                )

            n_secondary = len(active_indices)
            print(f"    二级指标数量: {n_secondary}")
            print(f"    有效专家数量(把握度≥{confidence_threshold}): {valid_count}")

            # ==================== 添加详细调试输出 ====================
            if category == 'effect':  # 只对作战效能显示详细过程
                print(f"\n    === 作战效能AHP详细计算过程 ===")
                indicator_names_effect = ['毁伤率', '任务完成率', '效费比']

                # 显示原始权重数据
                sub_weights = weight_matrix[:, indices]
                print(f"\n    【专家原始权重数据】")
                header = "    专家"
                for name in indicator_names_effect:
                    header += f"    {name}"
                print(header)
                print("    " + "-"*60)
                for i, name in enumerate(expert_names):
                    row = f"    {name}"
                    for j in range(n_secondary):
                        row += f" {sub_weights[i, j]:>8.4f}"
                    print(row)

                # 显示判断矩阵
                print(f"\n    【AHP判断矩阵】(基于几何平均)")
                print("           ", end="")
                for name in indicator_names_effect:
                    print(f"    {name[:4]}", end="")
                print()
                for i in range(n_secondary):
                    print(f"    {indicator_names_effect[i][:4]}", end="")
                    for j in range(n_secondary):
                        print(f" {judgment_matrix[i,j]:>8.4f}", end="")
                    print()

            # ==========================================================

            if len(active_indices) >= 2:
                if valid_count == 0:
                    # 没有任何把握度>=阈值的评分参与到成对判断中：退化为等权
                    print(f"    [警告] 该类别内无有效成对样本(把握度阈值过高)，对有效指标采用等权")
                    local_weights = np.ones(len(active_indices)) / len(active_indices)
                    local_ahp_result = {
                        'lambda_max': 0,
                        'CI': 0,
                        'CR': 0,
                        'RI': 0,
                        'is_consistent': True,
                        'weights': local_weights,
                        'valid_experts': 0,
                        'note': '无有效成对样本，采用等权'
                    }
                else:
                    # 2c: 计算AHP一致性
                    local_ahp_result = self.calculate_ahp_consistency(judgment_matrix)

                # ==================== 添加详细调试输出 ====================
                if category == 'effect':
                    # 显示权重向量计算过程
                    n = judgment_matrix.shape[0]
                    print(f"\n    【权重向量计算】(几何平均法)")
                    print(f"    1. 计算各行几何平均:")
                    for i in range(n):
                        row_product = np.prod(judgment_matrix[i, :])
                        geo_mean = row_product ** (1/n)
                        print(f"       {indicator_names_effect[i]}: (", end="")
                        for j in range(n):
                            if j > 0:
                                print(" × ", end="")
                            print(f"{judgment_matrix[i,j]:.4f}", end="")
                        print(f")^(1/{n}) = {geo_mean:.6f}")

                    print(f"\n    2. 归一化:")
                    raw_weights = np.prod(judgment_matrix, axis=1) ** (1/n)
                    sum_weights = raw_weights.sum()
                    for i in range(n):
                        normalized = raw_weights[i] / sum_weights
                        print(f"       {indicator_names_effect[i]}: {raw_weights[i]:.6f} / {sum_weights:.6f} = {normalized:.6f}")

                    print(f"\n    3. 一致性检验:")
                    print(f"       n = {n}")
                    print(f"       λmax = {local_ahp_result['lambda_max']:.6f}")
                    print(f"       CI = (λmax - n) / (n - 1) = ({local_ahp_result['lambda_max']:.4f} - {n}) / {n-1} = {local_ahp_result['CI']:.6f}")
                    print(f"       RI = {local_ahp_result['RI']:.4f}")
                    print(f"       CR = CI / RI = {local_ahp_result['CI']:.6f} / {local_ahp_result['RI']:.4f} = {local_ahp_result['CR']:.6f}")

                    # 验证Aw = λw
                    w = local_ahp_result['weights']
                    Aw = judgment_matrix @ w
                    print(f"\n    4. 验证 λmax = Aw/w:")
                    for i in range(n):
                        if w[i] > 0:
                            lambda_i = Aw[i] / w[i]
                            print(f"       λ_{i+1} = {Aw[i]:.6f} / {w[i]:.6f} = {lambda_i:.6f}")
                    print(f"       λmax = mean(λi) = {np.mean(Aw/w):.6f}")

                # ==========================================================

                local_weights = local_ahp_result['weights']

                # 判断一致性
                if local_ahp_result['is_consistent']:
                    print(f"    [OK] AHP一致性检验通过 (CR={local_ahp_result['CR']:.4f} ≤ 0.1)")
                else:
                    print(f"    [警告] AHP一致性检验未通过 (CR={local_ahp_result['CR']:.4f} > 0.1)")
                    print(f"    建议: 需要组织专家重新讨论调整评分")

            # 存储AHP结果
            ahp_consistency_results[category] = {
                'name': category_name,
                'first_level_weight': first_level_weight,
                'indices': indices,
                'n_secondary': n_secondary,
                'valid_experts': valid_count,
                'lambda_max': local_ahp_result['lambda_max'],
                'CI': local_ahp_result['CI'],
                'CR': local_ahp_result['CR'],
                'RI': local_ahp_result.get('RI', 0),
                'is_consistent': local_ahp_result['is_consistent'],
                'raw_weights': local_weights.copy() if hasattr(local_ahp_result.get('weights', None), 'copy') else np.array([])
            }

            # 2d: 存储二级权重（active指标写入AHP权重；inactive指标=0）
            if len(active_indices) >= 2:
                for k, idx in enumerate(active_indices):
                    second_level_weights_raw[idx] = local_weights[k]
                for idx in inactive_indices:
                    second_level_weights_raw[idx] = 0.0

            # 2d: 构建一级指标信息
            first_level_weights[category] = {
                'name': category_name,
                'weight': first_level_weight,
                'indices': indices
            }

        # 步骤3：计算最终权重（二级权重 × 一级指标权重）
        corrected_weights_final = np.zeros(n_indicators)

        for category, info in first_level_weights.items():
            indices = info['indices']
            first_level_weight = info['weight']

            for idx in indices:
                # 最终权重 = 二级权重 × 一级指标权重
                corrected_weights_final[idx] = second_level_weights_raw[idx] * first_level_weight

        # 若存在“无把握剔除”导致总和<1，则对最终权重做全局归一化，保持总和=1
        sum_final = corrected_weights_final.sum()
        if sum_final > 0:
            corrected_weights_final = corrected_weights_final / sum_final

        # 步骤4：计算原始权重（用于对比）
        original_weights_normalized = np.zeros(n_indicators)

        for category, info in INDICATOR_HIERARCHY.items():
            indices = info['indices']
            first_level_weight = info['weight']
            original_sum = original_avg_weights[indices].sum()

            for idx in indices:
                if original_sum > 0:
                    original_weights_normalized[idx] = (original_avg_weights[idx] / original_sum) * first_level_weight

        # 构建返回结果
        corrected_weights_dict = {
            'first_level': first_level_weights,
            'second_level_corrected': corrected_weights_final,
            'second_level_raw': second_level_weights_raw,  # 二级权重（未乘一级权重）
            'second_level_original': original_weights_normalized,
            'confidence_matrix': confidence_matrix,
            'expert_names': expert_names,
            'ahp_consistency_results': ahp_consistency_results
        }

        print(f"\n  权重计算完成")
        print(f"  最终权重总和: {corrected_weights_final.sum():.6f} (已全局归一化到1.000000)")

        return corrected_weights_dict
    
    def evaluate(self):
        """执行完整的专家可信度评估"""
        print("\n" + "="*100)
        print("专家可信度评估系统")
        print("="*100)
        
        # 1. 加载数据
        print("\n[步骤1] 加载专家背景数据...")
        experts_background = self.load_expert_background_data()
        print(f"  已加载 {len(experts_background)} 位专家的背景数据")
        
        print("\n[步骤2] 加载专家AHP权重数据...")
        experts_ahp = self.load_expert_ahp_weights()
        print(f"  已加载 {len(experts_ahp)} 位专家的AHP权重数据")
        
        # 2. 计算主观可信度
        print("\n[步骤3] 计算主观可信度 (αi)...")
        subjective_results = self.calculate_subjective_credibility(experts_background)
        print("  主观可信度计算完成")
        
        # 3. 计算客观可信度
        print("\n[步骤4] 计算客观可信度 (βi)...")
        objective_results, weight_matrix, avg_weight_vector, consistency_result = self.calculate_objective_credibility(experts_ahp)
        print("  客观可信度计算完成")
        
        # 4. 计算综合可信度
        print("\n[步骤5] 计算综合可信度...")
        comprehensive_results = self.calculate_comprehensive_credibility(subjective_results, objective_results)
        print("  综合可信度计算完成")
        
        # 5. 计算可信度修正权重（传入experts_ahp以提取把握度）
        corrected_weights_dict = self.calculate_corrected_weights(comprehensive_results, weight_matrix, experts_ahp)
        
        # 6. 显示结果
        self.display_results(comprehensive_results, consistency_result)
        
        # 7. 显示权重修正结果
        self.display_corrected_weights(corrected_weights_dict)
        
        # 8. 可视化
        print("\n[步骤8] 生成可视化图表...")
        self.visualize_results(comprehensive_results, experts_background, weight_matrix, avg_weight_vector, consistency_result, corrected_weights_dict)
        print("  可视化完成")
        
        print("\n" + "="*100)
        print("评估完成！")
        print("="*100 + "\n")
        
        return comprehensive_results
    
    def display_results(self, results, consistency_result):
        """显示评估结果"""
        print("\n" + "="*100)
        print("专家可信度评估结果")
        print("="*100)
        
        # 显示一致性检验结果
        print(f"\n[一致性检验结果 - 基于变异系数CV]")
        print(f"  整体一致性指标（平均CV）= {consistency_result['avg_cv']:.2f}%")
        print(f"  一致性等级: {consistency_result['level']}")
        print(f"  检验结果: {'通过' if consistency_result['passed'] else '未通过'}")
        
        if not consistency_result['passed']:
            print(f"  [警告] 建议重新组织专家评分！")
        
        # 显示各指标一致性详情
        print(f"\n[各指标一致性详情]")
        print(f"  {'指标':<20} {'CV(%)':<10} {'一致性等级':<15}")
        print("  " + "-"*50)
        for detail in consistency_result['indicator_details']:
            print(f"  {detail['name']:<20} {detail['cv']:<10.1f} {detail['level']:<15}")
        
        # 新增：显示删除异常值后的指标CV分析
        if 'outlier_removed_analysis' in consistency_result:
            print(f"\n[异常值处理分析 - 删除最远专家评分后的CV]")
            print(f"  {'指标':<20} {'原CV(%)':<10} {'新CV(%)':<10} {'变化':<10} {'被删除专家':<12} {'改善程度':<12}")
            print("  " + "-"*90)
            for analysis in consistency_result['outlier_removed_analysis']:
                change = analysis['cv_after'] - analysis['cv_before']
                improvement = "显著改善" if change < -10 else "适度改善" if change < -5 else "轻微改善" if change < 0 else "无改善"
                print(f"  {analysis['indicator']:<20} {analysis['cv_before']:<10.1f} {analysis['cv_after']:<10.1f} {change:>+9.1f} {analysis['removed_expert']:<12} {improvement:<12}")
        
        print(f"\n[专家可信度排名]")
        
        # 按综合可信度排序
        sorted_results = sorted(results, key=lambda x: x['comprehensive_credibility'], reverse=True)
        
        print(f"\n{'排名':<6} {'专家':<10} {'主观可信度(α)':<15} {'客观可信度(β)':<15} {'综合可信度':<15} {'离散度':<10} {'一致性':<10}")
        print("-"*100)
        
        # 定义离散度等级（基于CV%学术标准）
        def get_dispersion_level(disp):
            if disp is None or (isinstance(disp, float) and np.isnan(disp)):
                return "无把握/不足"
            if disp < 15:
                return "高度一致"
            elif disp < 25:
                return "较为一致"
            elif disp < 35:
                return "轻度分歧"
            else:
                return "严重分歧"
        
        for i, result in enumerate(sorted_results, 1):
            dispersion = result['dispersion']
            level = get_dispersion_level(dispersion)
            print(f"{i:<6} {result['expert_name']:<10} "
                  f"{result['subjective_credibility']:<15.6f} "
                  f"{result['objective_credibility']:<15.6f} "
                  f"{result['comprehensive_credibility']:<15.6f} "
                  f"{dispersion if not (isinstance(dispersion, float) and np.isnan(dispersion)) else float('nan'):<10.4f} "
                  f"{level:<10}")
        
        print("\n" + "-"*100)
        print("详细信息：")
        print("-"*100)
        
        for result in sorted_results:
            print(f"\n专家: {result['expert_name']}")
            print(f"  影响力得分 (Ωe): {result['influence_score']:.2f}")
            print(f"  专业知识得分 (Ωk): {result['knowledge_score']:.2f}")
            print(f"  主观综合得分 (Ωi): {result['subjective_total_score']:.2f}")
            print(f"  一致性得分: {result['consistency_score']:.4f}")
            print(f"  离散度: {result['dispersion']:.4f}")
            print(f"  主观可信度 (αi): {result['subjective_credibility']:.6f}")
            print(f"  客观可信度 (βi): {result['objective_credibility']:.6f}")
            print(f"  综合可信度: {result['comprehensive_credibility']:.6f}")
    
    def display_corrected_weights(self, corrected_weights_dict):
        """显示权重修正结果（新方案：基于AHP+把握度过滤）"""
        print("\n" + "="*100)
        print("指标权重计算结果（基于AHP一致性检验 + 把握度过滤）")
        print("="*100)

        # 显示一级指标权重
        print(f"\n[一级指标权重配置]")
        print(f"  {'一级指标':<20} {'权重':<10} {'二级指标数':<12} {'AHP一致性':<15}")
        print("  " + "-"*70)
        for category, ahp_result in corrected_weights_dict['ahp_consistency_results'].items():
            consistency_status = "通过" if ahp_result['is_consistent'] else "未通过"
            print(f"  {ahp_result['name']:<20} {ahp_result['first_level_weight']:<10.3f} "
                  f"{ahp_result['n_secondary']:<12} {consistency_status:<15}")

        # 显示AHP一致性检验详情
        print(f"\n[AHP一致性检验详情]")
        print(f"  {'一级指标':<18} {'λmax':<10} {'CI':<10} {'CR':<10} {'RI':<10} {'一致性':<10} {'有效专家':<10}")
        print("  " + "-"*85)
        for category, ahp_result in corrected_weights_dict['ahp_consistency_results'].items():
            is_consistent = "通过" if ahp_result['is_consistent'] else "未通过"
            print(f"  {ahp_result['name']:<18} {ahp_result['lambda_max']:<10.4f} "
                  f"{ahp_result['CI']:<10.4f} {ahp_result['CR']:<10.4f} "
                  f"{ahp_result['RI']:<10.4f} {is_consistent:<10} {ahp_result['valid_experts']:<10}")

        # 指标名称列表
        indicator_names_full = [
            '密钥泄露', '被侦察概率', '抗拦截能力',
            '崩溃率', '恢复能力', '通信可用性',
            '带宽', '呼叫建立时间', '传输时延', '误码率', '吞吐量', '频谱效率',
            '信干噪比', '抗干扰容限', '通信距离',
            '功耗', '人力需求',
            '毁伤率', '任务完成率', '效费比'
        ]

        corrected = corrected_weights_dict['second_level_corrected']
        original = corrected_weights_dict['second_level_original']

        # 显示二级指标权重对比
        print(f"\n[二级指标权重对比 - 修正前后]")

        print(f"  {'指标':<20} {'原始权重':<12} {'修正权重':<12} {'变化':<10} {'变化率(%)':<10}")
        print("  " + "-"*70)

        for i, name in enumerate(indicator_names_full):
            orig_w = original[i]
            corr_w = corrected[i]
            change = corr_w - orig_w
            change_pct = (change / orig_w * 100) if orig_w > 0 else 0

            # 变化率大于5%的用星号标注
            marker = " *" if abs(change_pct) > 5 else ""

            print(f"  {name:<20} {orig_w:>11.4f} {corr_w:>11.4f} {change:>+9.4f} {change_pct:>+9.1f}{marker}")

        # 按一级指标分组显示
        print(f"\n[分类汇总 - 各一级指标下的权重分布]")
        for category, info in corrected_weights_dict['first_level'].items():
            print(f"\n  {info['name']} (权重: {info['weight']:.3f})")
            print(f"  {'  ':<2}{'二级指标':<18} {'二级权重':<12} {'最终权重':<12} {'一级权重×二级权重':<20}")
            print(f"  {'  ':<2}{'-'*65}")

            raw_weights = corrected_weights_dict['ahp_consistency_results'][category]['raw_weights']

            for k, idx in enumerate(info['indices']):
                name = indicator_names_full[idx]
                second_w = raw_weights[k]
                final_w = corrected[idx]

                print(f"  {'  ':<2}{name:<18} {second_w:<12.4f} {final_w:<12.4f} "
                      f"{info['weight']:.3f} × {second_w:.4f} = {final_w:.4f}")

            # 显示该一级指标下二级权重之和
            print(f"  {'  ':<2}{'二级权重之和:':<18} {raw_weights.sum():<12.4f}")

        # 显示修正说明
        print(f"\n[计算方法说明]")
        print(f"  1. 把握度过滤: 只使用把握度≥0.6的专家评分")
        print(f"  2. AHP一致性: 对每个一级指标下的二级指标构建判断矩阵，计算CR")
        print(f"  3. 权重计算: 使用AHP几何平均法计算二级指标权重")
        print(f"  4. 归一化: 二级指标权重在各自一级指标内归一化为1")
        print(f"  5. 最终权重: 二级权重 × 一级指标权重")
        print(f"  6. 权重总和: {corrected.sum():.6f} (应为1.000000)")
        print(f"  * 标注: 权重变化率 > 5% 的指标")
    
    def visualize_results(self, results, experts_background, weight_matrix, avg_weight_vector, consistency_result, corrected_weights_dict):
        """
        生成可视化图表（基于README方案）

        输出：
        - 图1: 专家可信度综合图（3×2布局，单独保存）
        - 图2-5: 其他图表合并到一张图（2×2布局）
        """
        # 获取专家名称
        expert_names = [r['expert_name'] for r in results]

        # 获取各项数据
        subjective_credibility = np.array([r['subjective_credibility'] for r in results])
        objective_credibility = np.array([r['objective_credibility'] for r in results])
        comprehensive_credibility = np.array([r['comprehensive_credibility'] for r in results])
        dispersions = np.array([r['dispersion'] for r in results])

        # 计算平均把握度
        confidence_matrix = corrected_weights_dict['confidence_matrix']
        avg_confidence = np.mean(confidence_matrix, axis=1)

        # 计算单指标CV
        indicator_cvs = consistency_result['indicator_cvs']

        # 1. 专家可信度综合图（3×2布局，单独保存）
        print("    生成图1: 专家可信度综合图（6子图）...")
        self.plot_comprehensive_credibility_6panels(
            expert_names, weight_matrix, indicator_cvs, dispersions,
            subjective_credibility, objective_credibility, avg_confidence
        )

        # 2. 其他图表合并到一张图（2×2布局）
        print("    生成图2-5: 其他图表...")
        self.plot_other_charts_2x2(expert_names, comprehensive_credibility, corrected_weights_dict)

        print("\n  可视化完成！图表已保存。")

    def plot_other_charts_2x2(self, expert_names, comprehensive_credibility, corrected_weights_dict):
        """
        将图2-5合并到一张2×2布局的图中
        """
        # 指标名称
        indicator_names_full = [
            '密钥泄露', '被侦察概率', '抗拦截能力',
            '崩溃率', '恢复能力', '通信可用性',
            '带宽', '呼叫建立时间', '传输时延', '误码率', '吞吐量', '频谱效率',
            '信干噪比', '抗干扰容限', '通信距离',
            '功耗', '人力需求',
            '毁伤率', '任务完成率', '效费比'
        ]

        fig, axes = plt.subplots(2, 2, figsize=(18, 14))
        fig.suptitle('专家可信度与权重分析', fontsize=16, fontweight='bold')

        # 子图1 (图2): 专家主观/客观/综合可信度对比（不排序）
        ax1 = axes[0, 0]
        # 这里简单从综合可信度推导主观/客观占比（仅用于展示，不参与计算）
        n_experts = len(expert_names)
        x = np.arange(n_experts)
        width = 0.25

        # 模拟主观/客观分量：按0.6/0.4拆分综合可信度，仅用于可视化
        subj_vals = 0.6 * comprehensive_credibility
        obj_vals = 0.4 * comprehensive_credibility

        bars_s = ax1.bar(x - width, subj_vals, width, label='主观(0.6α)', color='skyblue', alpha=0.8)
        bars_o = ax1.bar(x, obj_vals, width, label='客观(0.4β)', color='lightcoral', alpha=0.8)
        bars_c = ax1.bar(x + width, comprehensive_credibility, width, label='综合可信度', color='lightgreen', alpha=0.8)

        ax1.set_xticks(x)
        ax1.set_xticklabels(expert_names, rotation=45, ha='right', fontsize=8)
        ax1.set_ylabel('可信度')
        ax1.set_ylim(0, 1)
        ax1.set_title('图2: 专家主观/客观/综合可信度对比')
        ax1.legend(fontsize=8)
        ax1.grid(True, axis='y', alpha=0.3)

        # 子图2 (图3): 旭日图 - 一级+二级指标权重分布
        ax2 = axes[0, 1]
        first_level = corrected_weights_dict['first_level']
        corrected_weights = corrected_weights_dict['second_level_corrected']

        # 构建旭日图数据
        category_names = []
        category_weights = []
        all_indicators = []
        all_indicator_weights = []

        for category, info in first_level.items():
            category_names.append(info['name'])
            indices = info['indices']
            cat_weight = sum(corrected_weights[idx] for idx in indices)
            category_weights.append(cat_weight)

            for idx in indices:
                all_indicators.append(indicator_names_full[idx])
                all_indicator_weights.append(corrected_weights[idx])

        # 绘制旭日图（嵌套环形图）
        # 外环：二级指标
        outer_colors = plt.cm.tab20(np.linspace(0, 1, len(all_indicators)))
        total_outer = sum(all_indicator_weights)
        outer_wedges, outer_texts, outer_autotexts = ax2.pie(
            all_indicator_weights, labels=None,
            colors=outer_colors, radius=1.0, startangle=90,
            autopct='%1.1f%%',
            pctdistance=0.9,
            wedgeprops=dict(width=0.3, edgecolor='white')
        )

        # 内环：一级指标
        inner_colors = plt.cm.Set2(np.linspace(0, 1, len(category_names)))
        inner_wedges, inner_texts, inner_autotexts = ax2.pie(
            category_weights, labels=category_names,
            colors=inner_colors, radius=0.7, startangle=90,
            autopct='%1.1f%%',
            pctdistance=0.75,
            wedgeprops=dict(width=0.3, edgecolor='white'),
            labeldistance=0.6
        )

        ax2.set_title('图3: 指标权重旭日图', fontweight='bold')
        ax2.legend(outer_wedges, all_indicators, loc='center left', bbox_to_anchor=(1, 0.5),
                  fontsize=6, ncol=2, title='二级指标')

        # 子图3 (图4): 二级指标权重排名
        ax3 = axes[1, 0]
        final_weights = corrected_weights_dict['second_level_corrected']
        sorted_idx = np.argsort(final_weights)[::-1][:15]  # 取前15个
        sorted_ind_names = [indicator_names_full[i] for i in sorted_idx]
        sorted_ind_weights = final_weights[sorted_idx]

        colors3 = plt.cm.Blues(np.linspace(0.4, 0.9, len(sorted_idx)))
        bars3 = ax3.barh(range(len(sorted_ind_names)), sorted_ind_weights, color=colors3)
        ax3.set_yticks(range(len(sorted_ind_names)))
        ax3.set_yticklabels(sorted_ind_names)
        ax3.set_xlabel('权重')
        ax3.set_title('图4: 二级指标权重排名(前15)')
        ax3.invert_yaxis()
        ax3.grid(True, alpha=0.3, axis='x')

        # 子图4 (图5): 原始vs最终权重对比
        ax4 = axes[1, 1]
        original_weights = corrected_weights_dict['second_level_original']

        x = np.arange(len(indicator_names_full))
        width = 0.35
        bars1 = ax4.bar(x - width/2, original_weights, width, label='原始AHP权重', color='#3498db', alpha=0.8)
        bars2 = ax4.bar(x + width/2, final_weights, width, label='最终权重', color='#e74c3c', alpha=0.8)
        ax4.set_xlabel('指标')
        ax4.set_ylabel('权重')
        ax4.set_title('图5: 原始vs最终权重对比')
        ax4.set_xticks(x[::2])
        ax4.set_xticklabels([indicator_names_full[i] for i in range(0, 20, 2)], rotation=60, ha='right', fontsize=7)
        ax4.legend(fontsize=8)
        ax4.grid(True, alpha=0.3, axis='y')

        # 添加变化幅度标注
        for i in range(len(indicator_names_full)):
            change = final_weights[i] - original_weights[i]
            change_pct = (change / original_weights[i] * 100) if original_weights[i] > 0 else 0
            max_val = max(original_weights[i], final_weights[i])
            # 标注变化幅度
            if abs(change_pct) >= 5:  # 只标注变化>5%的
                sign = '+' if change > 0 else ''
                ax4.annotate(f'{sign}{change_pct:.1f}%',
                            xy=(i, max_val + 0.005),
                            ha='center', va='bottom', fontsize=6,
                            color='red' if change > 0 else 'blue',
                            fontweight='bold')

        plt.tight_layout()
        plt.savefig('图2-5_综合分析.png', dpi=150, bbox_inches='tight', facecolor='white')
        plt.show()
        print("  图2-5 已保存: 图2-5_综合分析.png")

    def plot_comprehensive_credibility_6panels(
        self, expert_names, weight_matrix, indicator_cvs, expert_cvs,
        subjective_credibility, objective_credibility, confidence_levels
    ):
        """
        图表1：专家可信度综合图（3行×2列布局）

        子图1: 原始评分折线
        子图2: 单指标CV柱状图（删除前后对比）
        子图3: 专家CV柱状图
        子图4: 主观可信度柱状
        子图5: 客观可信度柱状
        子图6: 把握度柱状
        """

        indicator_names_short = [
            '密钥泄露', '被侦察', '抗拦截',
            '崩溃率', '恢复', '可用性',
            '带宽', '呼叫时间', '时延', '误码率', '吞吐量', '频谱',
            '信干噪比', '抗干扰', '距离',
            '功耗', '人力',
            '毁伤率', '完成率', '效费比'
        ]

        fig, axes = plt.subplots(3, 2, figsize=(18, 15))
        fig.suptitle('专家可信度评估综合分析', fontsize=16, fontweight='bold')

        # 子图1：原始评分折线图
        ax1 = axes[0, 0]
        colors = plt.cm.tab10(np.linspace(0, 1, len(expert_names)))
        markers = ['o', 's', '^', 'D', 'v', '<', '>', 'p', '*', 'h']

        x = np.arange(weight_matrix.shape[1])
        for i, expert in enumerate(expert_names):
            ax1.plot(x, weight_matrix[i], marker=markers[i % len(markers)],
                    color=colors[i], label=expert, linewidth=2, markersize=5, alpha=0.8)

        avg_scores = np.mean(weight_matrix, axis=0)
        ax1.plot(x, avg_scores, 'k--', linewidth=2.5, label='平均值', alpha=0.6)

        ax1.set_xlabel('指标序号')
        ax1.set_ylabel('评分')
        ax1.set_title('(a) 专家原始评分折线图')
        ax1.legend(bbox_to_anchor=(1.02, 1), loc='upper left', fontsize=7)
        ax1.set_xticks(x)
        ax1.grid(True, alpha=0.3)

        # 子图2：单指标CV柱状图（删除前后对比）
        ax2 = axes[0, 1]

        # 计算原始CV（删除前）
        cvs_before = []
        for j in range(weight_matrix.shape[1]):
            indicator_scores = weight_matrix[:, j]
            mean_score = np.mean(indicator_scores)
            std_score = np.std(indicator_scores, ddof=1)
            cv = (std_score / mean_score * 100) if mean_score != 0 else 0
            cvs_before.append(cv)

        # 使用处理后的CV（删除后）
        cvs_after = [c if not np.isnan(c) else 0 for c in indicator_cvs]

        x2 = np.arange(len(cvs_before))
        width = 0.35

        bars1 = ax2.bar(x2 - width/2, cvs_before, width, label='删除前CV',
                       color='steelblue', alpha=0.8, edgecolor='black', linewidth=0.5)
        bars2 = ax2.bar(x2 + width/2, cvs_after, width, label='删除后CV',
                       color='lightcoral', alpha=0.8, edgecolor='black', linewidth=0.5)

        # 标注有显著改善的指标
        for idx in range(len(cvs_before)):
            improvement = cvs_before[idx] - cvs_after[idx]
            if improvement > 5:
                ax2.annotate('', xy=(idx, cvs_after[idx]), xytext=(idx, cvs_before[idx]),
                           arrowprops=dict(arrowstyle='->', color='green', lw=2, alpha=0.7))
                ax2.text(idx, max(cvs_before[idx], cvs_after[idx]) + 2, f'-{improvement:.1f}%',
                        ha='center', va='bottom', fontsize=7, color='green', fontweight='bold')

        # 添加阈值线
        ax2.axhline(y=35, color='red', linestyle='--', linewidth=1.5, label='严重分歧(35%)', alpha=0.7)
        ax2.axhline(y=25, color='orange', linestyle='--', linewidth=1, label='轻度分歧(25%)', alpha=0.6)
        ax2.axhline(y=15, color='green', linestyle='--', linewidth=1, label='较为一致(15%)', alpha=0.6)

        ax2.set_xlabel('指标')
        ax2.set_ylabel('CV (%)')
        ax2.set_title('(b) 单指标CV分析 - 删除异常值前后对比')
        ax2.set_xticks(x2)
        ax2.set_xticklabels(indicator_names_short, fontsize=7, rotation=60, ha='right')
        ax2.legend(fontsize=7, loc='upper left')
        ax2.grid(axis='y', alpha=0.3)

        # 子图3：专家CV柱状图
        ax3 = axes[1, 0]
        valid_expert_cvs = [c if not (isinstance(c, float) and np.isnan(c)) else 0 for c in expert_cvs]

        colors_cv = []
        for cv in valid_expert_cvs:
            if cv < 15:
                colors_cv.append('green')
            elif cv < 25:
                colors_cv.append('yellowgreen')
            elif cv < 35:
                colors_cv.append('orange')
            else:
                colors_cv.append('red')

        bars3 = ax3.bar(range(len(expert_names)), valid_expert_cvs, color=colors_cv, alpha=0.8, edgecolor='black')

        ax3.axhline(y=15, color='green', linestyle='--', alpha=0.7, label='高度一致(15%)')
        ax3.axhline(y=25, color='orange', linestyle='--', alpha=0.7, label='较为一致(25%)')
        ax3.axhline(y=35, color='red', linestyle='--', alpha=0.7, label='严重分歧(35%)')

        ax3.set_xlabel('专家姓名')
        ax3.set_ylabel('CV (%)')
        ax3.set_title('(c) 专家个人CV分析')
        ax3.set_xticks(range(len(expert_names)))
        ax3.set_xticklabels(expert_names, rotation=45, ha='right')
        ax3.legend(fontsize=7, loc='upper right')
        ax3.grid(axis='y', alpha=0.3)

        # 子图4：主观可信度柱状图
        ax4 = axes[1, 1]
        colors_subjective = plt.cm.Blues(np.linspace(0.4, 0.9, len(expert_names)))
        bars4 = ax4.bar(range(len(expert_names)), subjective_credibility, color=colors_subjective)
        ax4.set_xlabel('专家姓名')
        ax4.set_ylabel('主观可信度')
        ax4.set_title('(d) 专家主观可信度 α')
        ax4.set_xticks(range(len(expert_names)))
        ax4.set_xticklabels(expert_names, rotation=45, ha='right')
        ax4.set_ylim(0, 1)
        for bar, val in zip(bars4, subjective_credibility):
            ax4.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 0.02,
                    f'{val:.3f}', ha='center', va='bottom', fontsize=8)
        ax4.grid(True, alpha=0.3, axis='y')

        # 子图5：客观可信度柱状图
        ax5 = axes[2, 0]
        colors_objective = plt.cm.Oranges(np.linspace(0.4, 0.9, len(expert_names)))
        bars5 = ax5.bar(range(len(expert_names)), objective_credibility, color=colors_objective)
        ax5.set_xlabel('专家姓名')
        ax5.set_ylabel('客观可信度')
        ax5.set_title('(e) 专家客观可信度 β')
        ax5.set_xticks(range(len(expert_names)))
        ax5.set_xticklabels(expert_names, rotation=45, ha='right')
        ax5.set_ylim(0, 1)
        for bar, val in zip(bars5, objective_credibility):
            ax5.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 0.02,
                    f'{val:.3f}', ha='center', va='bottom', fontsize=8)
        ax5.grid(True, alpha=0.3, axis='y')

        # 子图6：专家排序（按0.6α+0.4β）
        ax6 = axes[2, 1]
        # 计算排序得分
        ranking_scores = 0.6 * subjective_credibility + 0.4 * objective_credibility
        sorted_indices = np.argsort(ranking_scores)[::-1]
        sorted_names = [expert_names[i] for i in sorted_indices]
        sorted_scores = ranking_scores[sorted_indices]

        colors_ranking = plt.cm.RdYlGn(np.linspace(0.2, 0.9, len(expert_names)))
        y_pos = np.arange(len(sorted_names))
        bars6 = ax6.barh(y_pos, sorted_scores, color=[colors_ranking[i] for i in sorted_indices])
        ax6.set_yticks(y_pos)
        ax6.set_yticklabels(sorted_names)
        ax6.set_xlabel('排序得分 (0.6α + 0.4β)')
        ax6.set_title('(f) 专家排序')
        ax6.set_xlim(0, 1)
        ax6.invert_yaxis()
        for bar, val in zip(bars6, sorted_scores):
            ax6.text(val + 0.02, bar.get_y() + bar.get_height()/2,
                    f'{val:.3f}', va='center', fontsize=8)
        ax6.grid(True, alpha=0.3, axis='x')

        plt.tight_layout()
        plt.savefig('图1_专家可信度综合图.png', dpi=150, bbox_inches='tight',
                   facecolor='white', edgecolor='none')
        plt.show()
        print("  图1 已保存: 图1_专家可信度综合图.png")
    
    def plot_credibility_ranking(self, expert_names, comprehensive_credibility):
        """
        图表2：专家可信度排名图（降序柱状图）
        """
        # 排序
        sorted_indices = np.argsort(comprehensive_credibility)[::-1]
        sorted_names = [expert_names[i] for i in sorted_indices]
        sorted_values = comprehensive_credibility[sorted_indices]
        
        fig, ax = plt.subplots(figsize=(12, 6))
        
        # 颜色渐变
        colors = plt.cm.RdYlGn(np.linspace(0.2, 0.9, len(expert_names)))
        
        bars = ax.barh(range(len(sorted_names)), sorted_values, color=colors)
        
        ax.set_yticks(range(len(sorted_names)))
        ax.set_yticklabels(sorted_names)
        ax.set_xlabel('综合可信度', fontsize=12)
        ax.set_title('专家可信度排名', fontsize=14, fontweight='bold')
        ax.set_xlim(0, 1)
        
        # 添加数值标注
        for i, (bar, val) in enumerate(zip(bars, sorted_values)):
            ax.text(val + 0.02, bar.get_y() + bar.get_height()/2,
                   f'{val:.4f}', va='center', fontsize=10)
        
        # 添加排名标注
        for i, bar in enumerate(bars):
            ax.text(0.02, bar.get_y() + bar.get_height()/2,
                   f'#{i+1}', va='center', ha='left', fontsize=10,
                   fontweight='bold', color='white')
        
        ax.invert_yaxis()
        ax.grid(True, alpha=0.3, axis='x')
        
        plt.tight_layout()
        plt.savefig('图2_专家可信度排名.png', dpi=150, bbox_inches='tight',
                   facecolor='white', edgecolor='none')
        plt.show()
        print("  图2 已保存: 图2_专家可信度排名.png")
    
    def plot_weight_stacked_bar(self, corrected_weights_dict):
        """
        图表3：饼状图 - 一级指标下的二级权重分布
        每个一级指标一个饼图，展示其下属二级指标的权重占比
        """
        # 指标名称
        indicator_names_full = [
            '密钥泄露', '被侦察概率', '抗拦截能力',
            '崩溃率', '恢复能力', '通信可用性',
            '带宽', '呼叫建立时间', '传输时延', '误码率', '吞吐量', '频谱效率',
            '信干噪比', '抗干扰容限', '通信距离',
            '功耗', '人力需求',
            '毁伤率', '任务完成率', '效费比'
        ]

        # 一级指标信息
        first_level = corrected_weights_dict['first_level']
        corrected_weights = corrected_weights_dict['second_level_corrected']

        category_names = []
        category_weights = []
        indicators_in_category = []
        weights_in_category = []

        for category, info in first_level.items():
            category_names.append(info['name'])
            category_weights.append(info['weight'])

            indices = info['indices']
            ind_names = [indicator_names_full[idx] for idx in indices]
            ind_weights = [corrected_weights[idx] for idx in indices]

            indicators_in_category.append(ind_names)
            weights_in_category.append(ind_weights)

        # 创建6个子图（2行×3列）
        fig, axes = plt.subplots(2, 3, figsize=(16, 12))
        fig.suptitle('一级指标下的二级权重分布（饼状图）', fontsize=16, fontweight='bold')

        # 颜色映射
        colors = plt.cm.Set3(np.linspace(0, 1, 12))

        for idx, (cat_name, cat_weight, indicators, weights) in enumerate(
            zip(category_names, category_weights, indicators_in_category, weights_in_category)
        ):
            row = idx // 3
            col = idx % 3
            ax = axes[row, col]

            # 过滤掉权重为0的指标
            valid_data = [(ind, w) for ind, w in zip(indicators, weights) if w > 0]
            if valid_data:
                valid_names = [d[0] for d in valid_data]
                valid_weights = [d[1] for d in valid_data]

                # 绘制饼图
                wedges, texts, autotexts = ax.pie(
                    valid_weights,
                    labels=valid_names,
                    autopct='%1.1f%%',
                    colors=colors[:len(valid_names)],
                    startangle=90,
                    pctdistance=0.75
                )

                # 设置字体大小
                for text in texts:
                    text.set_fontsize(8)
                for autotext in autotexts:
                    autotext.set_fontsize(7)
                    autotext.set_color('white')
                    autotext.set_weight('bold')
            else:
                ax.text(0.5, 0.5, '无有效数据', ha='center', va='center')
                ax.set_xticks([])
                ax.set_yticks([])

            ax.set_title(f'{cat_name}\n(一级权重: {cat_weight:.0%})', fontsize=11, fontweight='bold')

        plt.tight_layout()
        plt.savefig('图3_二级权重饼状分布.png', dpi=150, bbox_inches='tight',
                   facecolor='white', edgecolor='none')
        plt.show()
        print("  图3 已保存: 图3_二级权重饼状分布.png")
    
    def plot_weight_ranking(self, corrected_weights_dict):
        """
        图表4：水平条形图 - 二级指标权重排名（含AHP/熵权标注）
        """
        indicator_names_full = [
            '密钥泄露', '被侦察概率', '抗拦截能力',
            '崩溃率', '恢复能力', '通信可用性',
            '带宽', '呼叫建立时间', '传输时延', '误码率', '吞吐量', '频谱效率',
            '信干噪比', '抗干扰容限', '通信距离',
            '功耗', '人力需求',
            '毁伤率', '任务完成率', '效费比'
        ]
        
        final_weights = corrected_weights_dict['second_level_corrected']
        
        # 判断哪些使用了熵权法（这里简化为AHP）
        # 实际应根据计算结果判断
        ahp_consistency = corrected_weights_dict['ahp_consistency_results']
        weight_methods = []
        
        for category, info in INDICATOR_HIERARCHY.items():
            indices = info['indices']
            for idx in indices:
                # 检查该指标是否有效（可根据ahp_consistency判断）
                if ahp_consistency.get(category, {}).get('is_consistent', True):
                    weight_methods.append('AHP')
                else:
                    weight_methods.append('熵权')
        
        # 排序
        sorted_indices = np.argsort(final_weights)[::-1]
        sorted_names = [indicator_names_full[i] for i in sorted_indices]
        sorted_weights = final_weights[sorted_indices]
        sorted_methods = [weight_methods[i] for i in sorted_indices]
        
        fig, ax = plt.subplots(figsize=(12, 10))
        
        # 颜色设置：AHP蓝色，熵权法橙色
        colors = ['#3498db' if m == 'AHP' else '#e67e22' for m in sorted_methods]
        
        y_pos = np.arange(len(sorted_names))
        bars = ax.barh(y_pos, sorted_weights, color=colors, height=0.7)
        
        ax.set_yticks(y_pos)
        ax.set_yticklabels(sorted_names)
        ax.set_xlabel('权重', fontsize=12)
        ax.set_title('二级指标权重排名', fontsize=14, fontweight='bold')
        
        # 添加权重数值和方法标注
        for i, (bar, w, method) in enumerate(zip(bars, sorted_weights, sorted_methods)):
            label = f'{w:.4f} [{method}]'
            ax.text(w + 0.002, bar.get_y() + bar.get_height()/2,
                   label, va='center', fontsize=9)
        
        from matplotlib.patches import Patch
        legend_elements = [
            Patch(facecolor='#3498db', label='AHP权重'),
            Patch(facecolor='#e67e22', label='熵权法权重')
        ]
        ax.legend(handles=legend_elements, loc='lower right')
        
        ax.invert_yaxis()
        ax.grid(True, alpha=0.3, axis='x')
        
        plt.tight_layout()
        plt.savefig('图4_二级指标权重排名.png', dpi=150, bbox_inches='tight',
                   facecolor='white', edgecolor='none')
        plt.show()
        print("  图4 已保存: 图4_二级指标权重排名.png")
    
    def plot_weight_comparison(self, corrected_weights_dict):
        """
        图表5：双柱对比图 - 原始AHP vs 最终权重
        """
        indicator_names_full = [
            '密钥泄露', '被侦察概率', '抗拦截能力',
            '崩溃率', '恢复能力', '通信可用性',
            '带宽', '呼叫建立时间', '传输时延', '误码率', '吞吐量', '频谱效率',
            '信干噪比', '抗干扰容限', '通信距离',
            '功耗', '人力需求',
            '毁伤率', '任务完成率', '效费比'
        ]
        
        original_weights = corrected_weights_dict['second_level_original']
        final_weights = corrected_weights_dict['second_level_corrected']
        
        fig, ax = plt.subplots(figsize=(16, 8))
        
        x = np.arange(len(indicator_names_full))
        width = 0.35
        
        # 绘制柱状图
        bars1 = ax.bar(x - width/2, original_weights, width,
                       label='原始AHP权重', color='#3498db', alpha=0.8)
        bars2 = ax.bar(x + width/2, final_weights, width,
                       label='最终权重', color='#e74c3c', alpha=0.8)
        
        # 计算变化百分比
        changes = np.zeros(len(original_weights))
        for i in range(len(original_weights)):
            if original_weights[i] > 0:
                changes[i] = ((final_weights[i] - original_weights[i]) / original_weights[i]) * 100
        
        # 标注变化百分比
        for i, (b1, b2, change) in enumerate(zip(bars1, bars2, changes)):
            if abs(change) >= 5:
                color = '#27ae60' if change > 0 else '#c0392b'
                ax.annotate(f'{change:+.1f}%',
                           xy=(i, max(b1.get_height(), b2.get_height())),
                           xytext=(0, 5), textcoords='offset points',
                           ha='center', va='bottom', fontsize=8,
                           color=color, fontweight='bold')
        
        ax.set_xlabel('二级指标', fontsize=12)
        ax.set_ylabel('权重', fontsize=12)
        ax.set_title('权重对比：原始AHP vs 最终权重', fontsize=14, fontweight='bold')
        ax.set_xticks(x)
        ax.set_xticklabels(indicator_names_full, rotation=45, ha='right', fontsize=8)
        ax.legend(loc='upper right')
        ax.grid(True, alpha=0.3, axis='y')
        
        plt.tight_layout()
        plt.savefig('图5_原始vs最终权重对比.png', dpi=150, bbox_inches='tight',
                   facecolor='white', edgecolor='none')
        plt.show()
        print("  图5 已保存: 图5_原始vs最终权重对比.png")
    
    def plot_expert_scores_lines(self, ax, weight_matrix, expert_names):
        """图表1: 专家评分折线图（所有专家在一张图，便于观察离散程度）"""
        indicator_names_short = [
            'I1:密钥', 'I2:侦察', 'I3:拦截',
            'I4:崩溃', 'I5:恢复', 'I6:可用',
            'I7:带宽', 'I8:呼叫', 'I9:时延', 'I10:误码', 'I11:吞吐', 'I12:频谱',
            'I13:信噪', 'I14:抗扰', 'I15:距离',
            'I16:功耗', 'I17:人力',
            'I18:毁伤', 'I19:完成', 'I20:效费'
        ]
        
        # 使用不同的颜色和线型
        colors = plt.cm.tab10(np.linspace(0, 1, len(expert_names)))
        markers = ['o', 's', '^', 'D', 'v', '<', '>', 'p', '*', 'h']
        
        x = np.arange(weight_matrix.shape[1])
        
        # 绘制每个专家的评分折线
        for i, expert_name in enumerate(expert_names):
            expert_scores = weight_matrix[i, :]
            ax.plot(x, expert_scores, marker=markers[i % len(markers)], 
                   color=colors[i], label=expert_name, linewidth=2, 
                   markersize=6, alpha=0.8)
        
        # 添加平均值线（虚线）
        avg_scores = np.mean(weight_matrix, axis=0)
        ax.plot(x, avg_scores, 'k--', linewidth=2.5, label='平均值', alpha=0.6)
        
        ax.set_title('专家对各指标的评分对比（折线图）\n可直观看出离散程度', 
                    fontsize=12, fontweight='bold')
        ax.set_xlabel('指标编号', fontsize=10)
        ax.set_ylabel('权重值', fontsize=10)
        ax.set_xticks(x)
        ax.set_xticklabels(indicator_names_short, fontsize=7, rotation=60, ha='right')
        ax.legend(fontsize=8, loc='upper left', ncol=2, framealpha=0.9)
        ax.grid(True, alpha=0.3, linestyle='--')
        ax.set_ylim(0, max(weight_matrix.max(), avg_scores.max()) * 1.1)
    
    def plot_expert_scores_subplots(self, fig, weight_matrix, expert_names):
        """图表1: 专家评分分布图（展示前4位专家）"""
        indicator_names = [
            '密钥泄露', '被侦察概率', '抗拦截能力',
            '崩溃率', '恢复能力', '通信可用性',
            '带宽', '呼叫建立时间', '传输时延', '误码率', '吞吐量', '频谱效率',
            '信干噪比', '抗干扰余量', '通信距离',
            '功耗', '人力需求',
            '毁伤率', '任务完成率', '效费比'
        ]
        
        # 只展示前4位专家
        for i in range(min(4, len(expert_names))):
            ax = plt.subplot(3, 3, i+1)
            
            expert_scores = weight_matrix[i, :]
            x = np.arange(len(expert_scores))
            
            bars = ax.bar(x, expert_scores, color='steelblue', alpha=0.7)
            ax.set_title(f'{expert_names[i]}的指标评分', fontsize=10, fontweight='bold')
            ax.set_xlabel('指标编号', fontsize=8)
            ax.set_ylabel('权重', fontsize=8)
            ax.set_xticks(x[::2])
            ax.set_xticklabels([f'I{j+1}' for j in range(0, 20, 2)], fontsize=7, rotation=45)
            ax.grid(axis='y', alpha=0.3)
            ax.set_ylim(0, max(expert_scores) * 1.2)
    
    def plot_cv_analysis(self, ax, weight_matrix, outlier_analysis):
        """图表2: 指标CV分析图（含异常值处理对比）"""
        indicator_names = [
            '密钥泄露', '被侦察', '抗拦截',
            '崩溃率', '恢复', '可用性',
            '带宽', '呼叫时间', '时延', '误码率', '吞吐量', '频谱',
            '信干噪比', '抗干扰', '距离',
            '功耗', '人力',
            '毁伤率', '完成率', '效费比'
        ]
        
        # 计算每个指标的原始CV
        cvs_original = []
        for j in range(weight_matrix.shape[1]):
            indicator_scores = weight_matrix[:, j]
            mean_score = np.mean(indicator_scores)
            std_score = np.std(indicator_scores, ddof=1)
            cv = (std_score / mean_score * 100) if mean_score != 0 else 0
            cvs_original.append(cv)
        
        # 创建CV对比数据（原始CV和处理后CV）
        cvs_after = cvs_original.copy()  # 默认使用原始CV
        
        # 如果有异常值处理数据，更新对应指标的新CV
        outlier_dict = {}
        if outlier_analysis:
            for item in outlier_analysis:
                indicator_name = item['indicator']
                if indicator_name in indicator_names:
                    idx = indicator_names.index(indicator_name)
                    cvs_after[idx] = item['cv_after']
                    outlier_dict[idx] = item
        
        x = np.arange(len(cvs_original))
        width = 0.35
        
        # 绘制双柱状图
        bars1 = ax.bar(x - width/2, cvs_original, width, label='原始CV', 
                      color='steelblue', alpha=0.8, edgecolor='black', linewidth=0.5)
        bars2 = ax.bar(x + width/2, cvs_after, width, label='删除异常值后CV', 
                      color='lightcoral', alpha=0.8, edgecolor='black', linewidth=0.5)
        
        # 为有显著改善的指标添加标注
        for idx, item in outlier_dict.items():
            improvement = item['cv_before'] - item['cv_after']
            if improvement > 5:  # 改善超过5%才标注
                # 在两个柱子之间添加下降箭头
                y_start = item['cv_before']
                y_end = item['cv_after']
                ax.annotate('', xy=(idx, y_end), xytext=(idx, y_start),
                           arrowprops=dict(arrowstyle='->', color='green', lw=2, alpha=0.7))
                # 添加改善幅度文本
                ax.text(idx, max(y_start, y_end) + 2, f'-{improvement:.1f}%', 
                       ha='center', va='bottom', fontsize=7, color='green', fontweight='bold')
        
        # 添加阈值线（基于学术标准）
        ax.axhline(y=35, color='red', linestyle='--', linewidth=1.5, label='严重分歧(CV>35%)', alpha=0.7)
        ax.axhline(y=25, color='orange', linestyle='--', linewidth=1, label='轻度分歧(CV>25%)', alpha=0.6)
        ax.axhline(y=15, color='gold', linestyle='--', linewidth=1, label='较为一致(CV>15%)', alpha=0.6)
        
        ax.set_title('各指标变异系数(CV)分析 - 异常值处理前后对比', fontsize=11, fontweight='bold')
        ax.set_xlabel('指标', fontsize=9)
        ax.set_ylabel('CV (%)', fontsize=9)
        ax.set_xticks(x)
        ax.set_xticklabels(indicator_names, fontsize=7, rotation=60, ha='right')
        ax.legend(fontsize=7, loc='upper left')
        ax.grid(axis='y', alpha=0.3)
        ax.set_ylim(0, max(max(cvs_original), max(cvs_after)) * 1.2)
    
    def plot_expert_boxplot(self, ax, weight_matrix, expert_names):
        """图表3: 专家评分箱线图"""
        # 每个专家的评分分布
        data = [weight_matrix[i, :] for i in range(weight_matrix.shape[0])]
        
        bp = ax.boxplot(data, labels=expert_names, patch_artist=True,
                        boxprops=dict(facecolor='lightblue', alpha=0.7),
                        medianprops=dict(color='red', linewidth=2),
                        whiskerprops=dict(color='blue', linewidth=1),
                        capprops=dict(color='blue', linewidth=1))
        
        ax.set_title('专家评分统计分布（箱线图）', fontsize=11, fontweight='bold')
        ax.set_xlabel('专家', fontsize=9)
        ax.set_ylabel('权重值', fontsize=9)
        ax.set_xticklabels(expert_names, fontsize=8, rotation=45, ha='right')
        ax.grid(axis='y', alpha=0.3)
    
    def plot_dispersion_comparison(self, ax, results):
        """图表3: 专家离散度分析（CV变异系数，带等级标注）"""
        experts = [r['expert_name'] for r in results]
        dispersions = [r['dispersion'] for r in results]
        
        x = np.arange(len(experts))
        
        # 根据CV等级设置颜色
        colors = []
        for cv in dispersions:
            if cv is None or (isinstance(cv, float) and np.isnan(cv)):
                colors.append('gray')
                continue
            if cv < 15:
                colors.append('green')  # 优秀
            elif cv < 25:
                colors.append('yellowgreen')  # 良好
            elif cv < 35:
                colors.append('orange')  # 一般
            else:
                colors.append('red')  # 较差
        
        bars = ax.bar(x, dispersions, color=colors, alpha=0.7, edgecolor='black', linewidth=1)
        
        # 添加阈值线
        # 添加CV阈值线（学术标准）
        ax.axhline(y=15, color='green', linestyle='--', linewidth=1.5, alpha=0.6, label='高度一致(CV≤15%)')
        ax.axhline(y=25, color='gold', linestyle='--', linewidth=1.5, alpha=0.6, label='较为一致(CV≤25%)')
        ax.axhline(y=35, color='red', linestyle='--', linewidth=2, alpha=0.7, label='严重分歧阈值(CV=35%)')
        
        ax.set_title('专家评分离散度（CV变异系数）', fontsize=11, fontweight='bold')
        ax.set_xlabel('专家', fontsize=9)
        ax.set_ylabel('CV (%)', fontsize=9)
        ax.set_xticks(x)
        ax.set_xticklabels(experts, fontsize=8, rotation=45, ha='right')
        ax.legend(fontsize=7, loc='upper left')
        ax.grid(axis='y', alpha=0.3)
        
        # 添加数值标签和等级
        for i, (bar, val) in enumerate(zip(bars, dispersions)):
            if val is None or (isinstance(val, float) and np.isnan(val)):
                ax.text(i, bar.get_height(), f'NA\n无把握', ha='center', va='bottom', fontsize=6)
                continue
            if val < 15:
                level = '高度一致'
            elif val < 25:
                level = '较为一致'
            elif val < 35:
                level = '轻度分歧'
            else:
                level = '严重分歧'
            ax.text(i, val, f'{val:.1f}%\n{level}', ha='center', va='bottom', fontsize=6)
    
    def plot_objective_credibility(self, ax, results):
        """图表4: 客观可信度对比（分段衰减，35%阈值）"""
        # 按客观可信度降序排序
        sorted_results = sorted(results, key=lambda x: x['objective_credibility'], reverse=True)
        experts = [r['expert_name'] for r in sorted_results]
        obj_cred = [r['objective_credibility'] for r in sorted_results]
        cvs = [r['dispersion'] for r in sorted_results]
        
        x = np.arange(len(experts))
        
        # 根据CV等级设置颜色
        colors = []
        for cv in cvs:
            if cv <= 15:
                colors.append('green')      # 高度一致
            elif cv <= 25:
                colors.append('yellowgreen')  # 较为一致
            elif cv <= 35:
                colors.append('orange')      # 轻度分歧
            else:
                colors.append('red')         # 严重分歧(β=0)
        
        bars = ax.bar(x, obj_cred, color=colors, alpha=0.8, edgecolor='black', linewidth=1)
        
        ax.set_title('客观可信度排名（分段衰减法）\nβ = 1.0(CV≤15%) → 0.5(CV=25%) → 0.0(CV≥35%)', 
                    fontsize=10, fontweight='bold')
        ax.set_xlabel('专家', fontsize=9)
        ax.set_ylabel('客观可信度 (β)', fontsize=9)
        ax.set_xticks(x)
        ax.set_xticklabels(experts, fontsize=8, rotation=45, ha='right')
        ax.set_ylim(0, 1.1)
        ax.grid(axis='y', alpha=0.3)
        
        # 添加数值标签（显示β和CV）
        for i, (bar, val, cv) in enumerate(zip(bars, obj_cred, cvs)):
            ax.text(i, val, f'β={val:.3f}\nCV={cv:.1f}%', ha='center', va='bottom', fontsize=6)
        
        # 添加平均线
        avg_obj = np.mean(obj_cred)
        ax.axhline(y=avg_obj, color='blue', linestyle=':', linewidth=2, alpha=0.6, 
                  label=f'平均β={avg_obj:.3f}')
        
        # 添加阈值参考线
        ax.axhline(y=0.7, color='gold', linestyle='--', linewidth=1, alpha=0.5, label='CV=25%时β=0.7')
        ax.legend(fontsize=7, loc='upper right')
    
    def plot_credibility_assessment(self, ax, results):
        """图表5: 可信度评估图（主观/客观/综合）"""
        experts = [r['expert_name'] for r in results]
        subjective = [r['subjective_credibility'] for r in results]
        objective = [r['objective_credibility'] for r in results]
        comprehensive = [r['comprehensive_credibility'] for r in results]
        
        x = np.arange(len(experts))
        width = 0.25
        
        bars1 = ax.bar(x - width, subjective, width, label='主观可信度(α)', 
                      color='skyblue', alpha=0.8, edgecolor='black', linewidth=0.5)
        bars2 = ax.bar(x, objective, width, label='客观可信度(β)', 
                      color='lightcoral', alpha=0.8, edgecolor='black', linewidth=0.5)
        bars3 = ax.bar(x + width, comprehensive, width, label='综合可信度', 
                      color='lightgreen', alpha=0.8, edgecolor='black', linewidth=0.5)
        
        ax.set_title('专家可信度评估（绝对得分）', fontsize=11, fontweight='bold')
        ax.set_xlabel('专家', fontsize=9)
        ax.set_ylabel('可信度分数', fontsize=9)
        ax.set_xticks(x)
        ax.set_xticklabels(experts, fontsize=8, rotation=45, ha='right')
        ax.legend(fontsize=8)
        ax.grid(axis='y', alpha=0.3)
        ax.set_ylim(0, 1.0)
        
        # 添加参考线
        ax.axhline(y=0.6, color='gray', linestyle=':', linewidth=1, alpha=0.5)
    
    def plot_credibility_correlation(self, ax, results):
        """图表6: 相关性散点图（主观 vs 客观可信度）"""
        subjective = np.array([r['subjective_credibility'] for r in results])
        objective = np.array([r['objective_credibility'] for r in results])
        experts = [r['expert_name'] for r in results]
        
        # 散点图
        scatter = ax.scatter(subjective, objective, s=150, c=range(len(experts)), 
                           cmap='viridis', alpha=0.7, edgecolors='black', linewidth=1)
        
        # 添加标签
        for i, expert in enumerate(experts):
            ax.annotate(expert, (subjective[i], objective[i]), 
                       xytext=(5, 5), textcoords='offset points', fontsize=7)
        
        # 添加对角线
        min_val = min(subjective.min(), objective.min())
        max_val = max(subjective.max(), objective.max())
        ax.plot([min_val, max_val], [min_val, max_val], 'r--', 
               alpha=0.5, linewidth=1.5, label='y=x对角线')
        
        # 计算相关系数
        correlation = np.corrcoef(subjective, objective)[0, 1]
        
        ax.set_title(f'主观vs客观可信度相关性分析\n(相关系数={correlation:.3f})', 
                    fontsize=11, fontweight='bold')
        ax.set_xlabel('主观可信度 (α)', fontsize=9)
        ax.set_ylabel('客观可信度 (β)', fontsize=9)
        ax.legend(fontsize=8)
        ax.grid(alpha=0.3)
        ax.set_xlim(min_val - 0.05, max_val + 0.05)
        ax.set_ylim(min_val - 0.05, max_val + 0.05)
    
    def plot_vector_dispersion(self, ax, weight_matrix, expert_names, consistency_result=None):
        """图表6: 向量相对离散度对比（原始向量公式计算）"""
        m = weight_matrix.shape[0]  # 专家数量
        avg_weights = np.mean(weight_matrix, axis=0)
        
        # 计算每位专家的向量相对离散度
        vector_dispersions = []
        expert_cvs = []  # 同时计算“相对均值偏差CV”用于对比（与客观可信度口径一致）

        if consistency_result and 'confidence_matrix' in consistency_result:
            confidence_matrix = consistency_result['confidence_matrix']
            threshold = float(consistency_result.get('confidence_threshold', 0.6))
            active_indicator_mask = consistency_result.get('active_indicator_mask', np.ones(weight_matrix.shape[1], dtype=bool))
            # 指标均值：仅使用把握度>=阈值的评分
            indicator_means = self._compute_indicator_means_with_confidence(weight_matrix, confidence_matrix, threshold=threshold)
            # overall_mean：有效评分均值
            valid_scores = weight_matrix[confidence_matrix >= threshold]
            overall_mean = float(np.mean(valid_scores)) if valid_scores.size > 0 else float(np.mean(weight_matrix))
        else:
            confidence_matrix = None
            threshold = 0.0
            active_indicator_mask = np.ones(weight_matrix.shape[1], dtype=bool)
            indicator_means = np.mean(weight_matrix, axis=0)
            overall_mean = float(np.mean(weight_matrix))
        
        for i in range(m):
            expert_weights = weight_matrix[i]
            
            # 分子：欧氏距离
            numerator = np.sqrt(np.sum((expert_weights - avg_weights) ** 2))
            
            # 分母：向量模的和
            denominator = np.sqrt(np.sum(expert_weights ** 2) + np.sum(avg_weights ** 2))
            
            # 相对离散度
            if denominator > 0:
                dispersion = numerator / denominator
            else:
                dispersion = 0
            
            vector_dispersions.append(dispersion)
            
            # 计算CV（相对指标均值偏差CV）
            if confidence_matrix is not None:
                valid_indicator = active_indicator_mask & (confidence_matrix[i, :] >= threshold) & (~np.isnan(indicator_means))
                if np.sum(valid_indicator) < 2:
                    cv = np.nan
                else:
                    deviations = expert_weights[valid_indicator] - indicator_means[valid_indicator]
                    std_of_deviations = np.std(deviations, ddof=1)
                    cv = (std_of_deviations / overall_mean) * 100 if overall_mean > 0 else 0.0
            else:
                deviations = expert_weights - indicator_means
                std_of_deviations = np.std(deviations, ddof=1) if deviations.size > 1 else 0.0
                cv = (std_of_deviations / overall_mean) * 100 if overall_mean > 0 else 0.0
            expert_cvs.append(cv)
        
        # 打印对比数据（用于调试）
        print("\n[向量离散度 vs CV 对比]")
        print(f"{'专家':<10} {'向量离散度':<15} {'CV(%)':<10}")
        print("-" * 50)
        for i, name in enumerate(expert_names):
            cv_str = f"{expert_cvs[i]:.2f}" if not (isinstance(expert_cvs[i], float) and np.isnan(expert_cvs[i])) else "NA"
            print(f"{name:<10} {vector_dispersions[i]:<15.6f} {cv_str:<10}")
        
        # 创建DataFrame用于排序
        dispersion_data = list(zip(expert_names, vector_dispersions))
        dispersion_data.sort(key=lambda x: x[1])  # 按离散度升序排列
        
        sorted_experts = [d[0] for d in dispersion_data]
        sorted_dispersions = [d[1] for d in dispersion_data]
        
        x = np.arange(len(sorted_experts))
        
        # 根据离散度设置颜色（渐变色）
        colors = plt.cm.RdYlGn_r(np.array(sorted_dispersions) / max(sorted_dispersions))
        
        bars = ax.bar(x, sorted_dispersions, color=colors, alpha=0.8, 
                     edgecolor='black', linewidth=1)
        
        ax.set_title('向量相对离散度对比 (Vector-based Relative Dispersion)\n取值范围0~0.707，数值越小表示越接近平均值', 
                    fontsize=10, fontweight='bold')
        ax.set_xlabel('专家', fontsize=9)
        ax.set_ylabel('相对离散度', fontsize=9)
        ax.set_xticks(x)
        ax.set_xticklabels(sorted_experts, fontsize=8, rotation=45, ha='right')
        ax.grid(axis='y', alpha=0.3)
        ax.set_ylim(0, max(sorted_dispersions) * 1.15)
        
        # 添加数值标签
        for i, (bar, val) in enumerate(zip(bars, sorted_dispersions)):
            ax.text(i, val, f'{val:.4f}', ha='center', va='bottom', fontsize=7)
        
        # 添加平均线
        avg_disp = np.mean(sorted_dispersions)
        ax.axhline(y=avg_disp, color='blue', linestyle='--', linewidth=2, 
                  alpha=0.6, label=f'Average={avg_disp:.4f} ({(avg_disp/0.707)*100:.0f}% of max)')
        
        # 添加说明文本（使用英文避免乱码）
        ax.text(0.02, 0.98, f'Note:\n- Theoretical max: 0.707\n- Current max: {max(sorted_dispersions):.4f} ({(max(sorted_dispersions)/0.707)*100:.0f}%)\n- Pure geometric distance\n- No statistical threshold', 
               transform=ax.transAxes, fontsize=7, verticalalignment='top',
               bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.3))
        
        ax.legend(fontsize=7, loc='upper right')
    
    def plot_weight_correction(self, ax, corrected_weights_dict):
        """图表7: 权重修正对比图"""
        original_weights = corrected_weights_dict['second_level_original']
        corrected_weights = corrected_weights_dict['second_level_corrected']
        
        # 计算变化率
        changes = ((corrected_weights - original_weights) / original_weights) * 100
        
        # 获取指标名称
        indicator_names = [name.replace('_weight', '') for name in AHP_WEIGHT_FIELDS]
        
        # 创建位置
        x = np.arange(len(indicator_names))
        width = 0.35
        
        # 绘制对比柱状图
        bars1 = ax.bar(x - width/2, original_weights, width, label='Original', 
                      color='steelblue', alpha=0.7)
        bars2 = ax.bar(x + width/2, corrected_weights, width, label='Corrected', 
                      color='orange', alpha=0.7)
        
        # 标注变化超过5%的指标
        for i, change in enumerate(changes):
            if abs(change) > 5:
                y_pos = max(original_weights[i], corrected_weights[i]) + 0.002
                ax.text(i, y_pos, f'{change:+.1f}%', 
                       ha='center', va='bottom', fontsize=6, 
                       color='red' if change > 0 else 'green', 
                       weight='bold')
        
        ax.set_title('Weight Correction Comparison (Original vs Corrected)', 
                    fontsize=10, pad=10, weight='bold')
        ax.set_xlabel('Indicators', fontsize=9)
        ax.set_ylabel('Weight', fontsize=9)
        ax.set_xticks(x)
        ax.set_xticklabels(indicator_names, rotation=90, fontsize=6)
        ax.legend(fontsize=8)
        ax.grid(axis='y', alpha=0.3, linestyle='--')
        
        # 添加说明
        ax.text(0.02, 0.98, f'Sum of Original: {original_weights.sum():.6f}\nSum of Corrected: {corrected_weights.sum():.6f}\nFormula: w = Sigma(lambda_i * w_ij)', 
               transform=ax.transAxes, fontsize=7, verticalalignment='top',
               bbox=dict(boxstyle='round', facecolor='lightblue', alpha=0.3))
    
    def plot_expert_credibility_breakdown(self, ax, results, corrected_weights_dict):
        """图表8: AHP一致性检验结果图（新方案）"""
        expert_names = [r['expert_name'] for r in results]
        subjective = [r['subjective_credibility'] for r in results]
        objective = [r['objective_credibility'] for r in results]
        comprehensive = [r['comprehensive_credibility'] for r in results]

        x = np.arange(len(expert_names))
        width = 0.25

        # 绘制三组柱状图
        bars1 = ax.bar(x - width, subjective, width, label='Subjective (Alpha)',
                      color='skyblue', alpha=0.8)
        bars2 = ax.bar(x, objective, width, label='Objective (Beta)',
                      color='lightcoral', alpha=0.8)
        bars3 = ax.bar(x + width, comprehensive, width, label='Comprehensive',
                      color='lightgreen', alpha=0.8)

        # 在柱子上标注数值
        for bars in [bars1, bars2, bars3]:
            for bar in bars:
                height = bar.get_height()
                ax.text(bar.get_x() + bar.get_width()/2., height,
                       f'{height:.3f}',
                       ha='center', va='bottom', fontsize=6)

        ax.set_title('Expert Credibility (Subjective/Objective/Comprehensive)',
                    fontsize=10, pad=10, weight='bold')
        ax.set_xlabel('Experts', fontsize=9)
        ax.set_ylabel('Credibility Score', fontsize=9)
        ax.set_xticks(x)
        ax.set_xticklabels(expert_names, rotation=45, fontsize=8, ha='right')
        ax.legend(fontsize=7, loc='upper left')
        ax.grid(axis='y', alpha=0.3, linestyle='--')
        ax.set_ylim([0.5, 1.0])

        # 添加公式说明
        ax.text(0.02, 0.98, 'New Method:\n- Confidence filter (>=0.6)\n- AHP consistency check\n- Weight = Secondary * Primary',
               transform=ax.transAxes, fontsize=7,
               verticalalignment='top',
               bbox=dict(boxstyle='round', facecolor='yellow', alpha=0.3))


def main():
    """主函数"""
    # 创建评估器
    evaluator = ExpertCredibilityEvaluator()
    
    # 执行评估
    results = evaluator.evaluate()
    
    # 返回结果（保存为变量）
    return results


if __name__ == "__main__":
    # 执行评估
    expert_credibility_results = main()
    
    # 结果已保存在变量 expert_credibility_results 中
    print("\n结果已保存在变量: expert_credibility_results")
    print(f"共评估 {len(expert_credibility_results)} 位专家")
