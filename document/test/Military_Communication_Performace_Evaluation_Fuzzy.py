#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
军事通信效能评估 - 基于模糊综合评判法
模糊关系矩阵 + 模糊算子 → 模糊综合评判

评判方法：
1. 建立隶属度函数（梯形/三角形）
2. 构建模糊关系矩阵 R
3. 确定权重向量 A（AHP + 熵权法）
4. 模糊合成运算 B = A ○ R
5. 去模糊化得到最终得分
"""

import mysql.connector
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from datetime import datetime
import warnings
warnings.filterwarnings('ignore')

plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei']
plt.rcParams['axes.unicode_minus'] = False

print("="*80)
print("军事通信效能评估系统 - 模糊综合评判法")
print("="*80)
print(f"评估时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
print()

# ============================================================================
# 数据库配置（复用MaxMin.py的配置）
# ============================================================================

DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': 'root',
    'database': 'military_communication_effectiveness',
    'charset': 'utf8mb4'
}

def get_db_connection():
    return mysql.connector.connect(**DB_CONFIG)

# ============================================================================
# 指标体系定义（复用MaxMin.py的指标体系）
# ============================================================================

INDICATOR_SYSTEM = {
    'RL': {'name': '可靠性', 'priority': 1, 'indicators': [
        {'code': 'RL_communication_availability_rate', 'name': '通信可用性', 'direction': 'max'},
        {'code': 'RL_communication_success_rate', 'name': '通信成功率', 'direction': 'max'},
        {'code': 'RL_recovery_duration_ms', 'name': '恢复时长', 'direction': 'min'},
        {'code': 'RL_crash_rate', 'name': '崩溃比例', 'direction': 'min'}
    ]},
    'SC': {'name': '安全性', 'priority': 2, 'indicators': [
        {'code': 'SC_key_compromise_frequency', 'name': '密钥泄露频率', 'direction': 'min'},
        {'code': 'SC_detection_probability', 'name': '被侦察概率', 'direction': 'min'},
        {'code': 'SC_interception_resistance', 'name': '抗拦截能力', 'direction': 'max'}
    ]},
    'AJ': {'name': '抗干扰性', 'priority': 3, 'indicators': [
        {'code': 'AJ_avg_sinr', 'name': '平均信干噪比', 'direction': 'max'},
        {'code': 'AJ_avg_jamming_margin', 'name': '平均抗干扰余量', 'direction': 'max'}
    ]},
    'EF': {'name': '有效性', 'priority': 4, 'indicators': [
        {'code': 'EF_avg_communication_distance', 'name': '平均通信距离', 'direction': 'max'},
        {'code': 'EF_avg_ber', 'name': '平均误码率', 'direction': 'min'},
        {'code': 'EF_avg_plr', 'name': '平均丢包率', 'direction': 'min'},
        {'code': 'EF_task_success_rate', 'name': '任务成功率', 'direction': 'max'}
    ]},
    'PO': {'name': '处理能力', 'priority': 5, 'indicators': [
        {'code': 'PO_effective_throughput', 'name': '有效吞吐量', 'direction': 'max'},
        {'code': 'PO_spectral_efficiency', 'name': '频谱效率', 'direction': 'max'}
    ]},
    'NC': {'name': '组网能力', 'priority': 6, 'indicators': [
        {'code': 'NC_avg_network_setup_duration_ms', 'name': '平均组网时长', 'direction': 'min'},
        {'code': 'NC_avg_connectivity_rate', 'name': '平均连通率', 'direction': 'max'}
    ]},
    'HO': {'name': '人为操作', 'priority': 7, 'indicators': [
        {'code': 'HO_avg_operator_reaction_time_ms', 'name': '平均操作员反应时间', 'direction': 'min'},
        {'code': 'HO_operation_success_rate', 'name': '操作成功率', 'direction': 'max'}
    ]},
    'RS': {'name': '响应能力', 'priority': 8, 'indicators': [
        {'code': 'RS_avg_call_setup_duration_ms', 'name': '平均呼叫建立时长', 'direction': 'min'},
        {'code': 'RS_avg_transmission_delay_ms', 'name': '平均传输时延', 'direction': 'min'}
    ]}
}

print("步骤1: 指标体系定义")
print("-"*80)
for dim_code in sorted(INDICATOR_SYSTEM.keys(), key=lambda x: INDICATOR_SYSTEM[x]['priority']):
    dim = INDICATOR_SYSTEM[dim_code]
    print(f"  {dim['priority']}. {dim_code} - {dim['name']} ({len(dim['indicators'])}个指标)")
print()

# ============================================================================
# 模糊综合评判法核心部分
# ============================================================================

print("步骤2: 定义评语集（模糊评判等级）")
print("-"*80)

# 评语集 V = {v1, v2, v3, v4, v5}
EVALUATION_SET = {
    'v1': {'name': '优秀', 'score': 95, 'range': [90, 100]},
    'v2': {'name': '良好', 'score': 85, 'range': [80, 90]},
    'v3': {'name': '中等', 'score': 75, 'range': [70, 80]},
    'v4': {'name': '及格', 'score': 65, 'range': [60, 70]},
    'v5': {'name': '较差', 'score': 50, 'range': [0, 60]}
}

print("评语集 V = {优秀, 良好, 中等, 及格, 较差}")
for k, v in EVALUATION_SET.items():
    print(f"  {k}: {v['name']} - 分数区间[{v['range'][0]}, {v['range'][1]}], 代表值{v['score']}")
print()

print("步骤3: 定义隶属度函数（梯形/三角形）")
print("-"*80)
print("采用梯形隶属度函数，将标准化得分映射到各评语等级的隶属度")
print()

def membership_excellent(x):
    """优秀的隶属度函数：梯形 [80, 90, 100, 100]"""
    if x < 80:
        return 0
    elif 80 <= x < 90:
        return (x - 80) / 10
    else:
        return 1

def membership_good(x):
    """良好的隶属度函数：梯形 [70, 80, 90, 95]"""
    if x < 70:
        return 0
    elif 70 <= x < 80:
        return (x - 70) / 10
    elif 80 <= x <= 90:
        return 1
    elif 90 < x <= 95:
        return (95 - x) / 5
    else:
        return 0

def membership_medium(x):
    """中等的隶属度函数：梯形 [60, 70, 80, 85]"""
    if x < 60:
        return 0
    elif 60 <= x < 70:
        return (x - 60) / 10
    elif 70 <= x <= 80:
        return 1
    elif 80 < x <= 85:
        return (85 - x) / 5
    else:
        return 0

def membership_pass(x):
    """及格的隶属度函数：梯形 [50, 60, 70, 75]"""
    if x < 50:
        return 0
    elif 50 <= x < 60:
        return (x - 50) / 10
    elif 60 <= x <= 70:
        return 1
    elif 70 < x <= 75:
        return (75 - x) / 5
    else:
        return 0

def membership_poor(x):
    """较差的隶属度函数：梯形 [0, 0, 50, 60]"""
    if x <= 50:
        return 1
    elif 50 < x <= 60:
        return (60 - x) / 10
    else:
        return 0

MEMBERSHIP_FUNCTIONS = [
    membership_excellent,
    membership_good,
    membership_medium,
    membership_pass,
    membership_poor
]

# 绘制隶属度函数
x_plot = np.linspace(0, 100, 500)
plt.figure(figsize=(14, 7))
plt.plot(x_plot, [membership_excellent(x) for x in x_plot], 'r-', linewidth=2.5, label='优秀')
plt.plot(x_plot, [membership_good(x) for x in x_plot], 'g-', linewidth=2.5, label='良好')
plt.plot(x_plot, [membership_medium(x) for x in x_plot], 'b-', linewidth=2.5, label='中等')
plt.plot(x_plot, [membership_pass(x) for x in x_plot], 'orange', linewidth=2.5, label='及格')
plt.plot(x_plot, [membership_poor(x) for x in x_plot], 'purple', linewidth=2.5, label='较差')
plt.title('模糊隶属度函数', fontsize=14, fontweight='bold')
plt.xlabel('标准化得分', fontsize=12)
plt.ylabel('隶属度', fontsize=12)
plt.grid(alpha=0.3)
plt.legend(fontsize=11)
plt.ylim(-0.05, 1.05)
plt.tight_layout()
plt.show()

print("✓ 隶属度函数定义完成")
print()

# ============================================================================
# 数据提取和标准化（复用MaxMin.py的逻辑）
# ============================================================================

def extract_indicators_from_raw_tables():
    """从原始表中提取21个指标"""
    conn = get_db_connection()
    
    query_dbc = """
    SELECT test_id, scenario_id,
        AVG(call_setup_duration_ms) as RS_avg_call_setup_duration_ms,
        AVG(transmission_delay_ms) as RS_avg_transmission_delay_ms,
        AVG(instant_throughput) as PO_effective_throughput,
        AVG(instant_throughput / channel_bandwidth) as PO_spectral_efficiency,
        AVG(communication_distance) as EF_avg_communication_distance,
        AVG(instant_ber) as EF_avg_ber,
        AVG(instant_plr) as EF_avg_plr,
        SUM(CASE WHEN communication_success = 1 THEN 1 ELSE 0 END) / COUNT(*) as EF_task_success_rate,
        SUM(CASE WHEN communication_success = 1 THEN 1 ELSE 0 END) / COUNT(*) as RL_communication_success_rate,
        AVG(instant_sinr) as AJ_avg_sinr,
        AVG(jamming_margin) as AJ_avg_jamming_margin,
        AVG(operator_reaction_time_ms) as HO_avg_operator_reaction_time_ms,
        SUM(CASE WHEN operation_error = 0 THEN 1 ELSE 0 END) / COUNT(*) as HO_operation_success_rate,
        SUM(CASE WHEN key_updated = 1 THEN 1 ELSE 0 END) / NULLIF(SUM(communication_duration_ms) / 3600000.0, 0) as SC_key_compromise_frequency,
        SUM(CASE WHEN detected = 1 THEN 1 ELSE 0 END) / COUNT(*) as SC_detection_probability,
        1 - (SUM(CASE WHEN intercepted = 1 THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0)) as SC_interception_resistance,
        COUNT(*) as total_communications
    FROM during_battle_communications
    GROUP BY test_id, scenario_id
    """
    
    query_lc = """
    SELECT test_id, scenario_id,
        (SUM(total_lifecycle_duration_ms) - COALESCE(SUM(total_interruption_duration_ms), 0)) / NULLIF(SUM(total_lifecycle_duration_ms), 0) as RL_communication_availability_rate,
        AVG(CASE WHEN network_crash_occurred = 1 THEN total_interruption_duration_ms ELSE NULL END) as RL_recovery_duration_ms,
        SUM(CASE WHEN network_crash_occurred = 1 THEN 1 ELSE 0 END) / COUNT(*) as RL_crash_rate,
        AVG(network_setup_duration_ms) as NC_avg_network_setup_duration_ms,
        AVG(connectivity_rate) as NC_avg_connectivity_rate,
        COUNT(*) as total_lifecycles
    FROM communication_network_lifecycle
    GROUP BY test_id, scenario_id
    """
    
    df_dbc = pd.read_sql(query_dbc, conn)
    df_lc = pd.read_sql(query_lc, conn)
    conn.close()
    
    df_merged = pd.merge(df_dbc, df_lc, on=['test_id', 'scenario_id'], how='left')
    df_merged['RL_communication_availability_rate'] = df_merged['RL_communication_availability_rate'].fillna(1.0)
    df_merged['RL_recovery_duration_ms'] = df_merged['RL_recovery_duration_ms'].fillna(0)
    df_merged['RL_crash_rate'] = df_merged['RL_crash_rate'].fillna(0)
    df_merged['NC_avg_connectivity_rate'] = df_merged['NC_avg_connectivity_rate'] / 100.0
    
    return df_merged

def normalize_indicator(series, direction, indicator_code=None):
    """Min-Max归一化到0-100分"""
    if series.isna().all():
        return pd.Series([50] * len(series), index=series.index)
    
    LOGARITHMIC_INDICATORS = {'EF_avg_ber', 'EF_avg_plr'}
    if indicator_code in LOGARITHMIC_INDICATORS:
        series = -np.log10(series + 1e-10)
    
    min_val = series.min()
    max_val = series.max()
    
    if max_val == min_val:
        return pd.Series([50] * len(series), index=series.index)
    
    if direction == 'max':
        normalized = (series - min_val) / (max_val - min_val) * 100
    else:
        normalized = (max_val - series) / (max_val - min_val) * 100
    
    return normalized

print("步骤4: 数据提取和标准化")
print("-"*80)

df_raw = extract_indicators_from_raw_tables()
print(f"✓ 提取 {len(df_raw)} 个测试批次的数据")

df_normalized = df_raw[['test_id', 'scenario_id']].copy()
for dim_code, dim_info in INDICATOR_SYSTEM.items():
    for indicator in dim_info['indicators']:
        col_name = indicator['code']
        direction = indicator['direction']
        if col_name in df_raw.columns:
            df_normalized[col_name] = normalize_indicator(df_raw[col_name], direction, col_name)
        else:
            df_normalized[col_name] = 50

print(f"✓ 完成21个指标的标准化（0-100分）")
print()

# ============================================================================
# 构建模糊关系矩阵
# ============================================================================

print("步骤5: 构建模糊关系矩阵 R")
print("-"*80)

def build_fuzzy_relation_matrix(scores):
    """
    构建模糊关系矩阵 R
    R[i,j] = 指标i对评语j的隶属度
    """
    m = len(scores)
    n = len(MEMBERSHIP_FUNCTIONS)
    R = np.zeros((m, n))
    
    for i, score in enumerate(scores):
        for j, func in enumerate(MEMBERSHIP_FUNCTIONS):
            R[i, j] = func(score)
    
    return R

# ============================================================================
# AHP计算权重
# ============================================================================

print("步骤6: AHP计算权重向量 A")
print("-"*80)

def ahp_calculate_weights(judgment_matrix):
    """AHP计算权重"""
    n = len(judgment_matrix)
    eigenvalues, eigenvectors = np.linalg.eig(judgment_matrix)
    max_eigenvalue_index = np.argmax(eigenvalues.real)
    max_eigenvalue = eigenvalues[max_eigenvalue_index].real
    max_eigenvector = eigenvectors[:, max_eigenvalue_index].real
    weights = max_eigenvector / max_eigenvector.sum()
    
    CI = (max_eigenvalue - n) / (n - 1)
    RI_dict = {1: 0, 2: 0, 3: 0.58, 4: 0.90, 5: 1.12, 6: 1.24, 7: 1.32, 8: 1.41, 9: 1.45, 10: 1.49}
    RI = RI_dict.get(n, 1.41)
    CR = CI / RI if RI != 0 else 0
    
    return weights, CR

# 准则层判断矩阵（8个维度）
criteria_matrix = np.array([
    [1,   2,   3,   4,   5,   6,   7,   8],
    [1/2, 1,   2,   3,   4,   5,   6,   7],
    [1/3, 1/2, 1,   2,   3,   4,   5,   6],
    [1/4, 1/3, 1/2, 1,   2,   3,   4,   5],
    [1/5, 1/4, 1/3, 1/2, 1,   2,   3,   4],
    [1/6, 1/5, 1/4, 1/3, 1/2, 1,   2,   3],
    [1/7, 1/6, 1/5, 1/4, 1/3, 1/2, 1,   2],
    [1/8, 1/7, 1/6, 1/5, 1/4, 1/3, 1/2, 1]
])

criteria_weights, criteria_CR = ahp_calculate_weights(criteria_matrix)
dim_codes_ordered = ['RL', 'SC', 'AJ', 'EF', 'PO', 'NC', 'HO', 'RS']

print(f"一致性比率 CR = {criteria_CR:.4f} {'✓' if criteria_CR < 0.1 else '✗'}")
print("\n准则层权重:")
for i, dim_code in enumerate(dim_codes_ordered):
    print(f"  {INDICATOR_SYSTEM[dim_code]['name']} ({dim_code}): {criteria_weights[i]:.4f} ({criteria_weights[i]*100:.2f}%)")
print()

# 计算指标层权重（熵权法）
def calculate_indicator_entropy_weights(df_norm, indicator_system):
    """在每个维度内部使用熵权法"""
    indicator_weights = {}
    
    for dim_code, dim_info in indicator_system.items():
        indicator_cols = [ind['code'] for ind in dim_info['indicators']]
        dim_indicator_data = df_norm[indicator_cols].values
        n_samples, n_indicators = dim_indicator_data.shape
        
        if n_indicators == 1:
            indicator_weights[indicator_cols[0]] = 1.0
        else:
            data_normalized = dim_indicator_data / 100.0
            data_sum = data_normalized.sum(axis=0)
            data_sum[data_sum == 0] = 1
            p = data_normalized / data_sum
            
            k = 1 / np.log(n_samples)
            entropy = np.zeros(n_indicators)
            
            for j in range(n_indicators):
                p_j = p[:, j]
                p_j = p_j[p_j > 0]
                if len(p_j) > 0:
                    entropy[j] = -k * np.sum(p_j * np.log(p_j))
            
            d = 1 - entropy
            if d.sum() == 0:
                weights = np.ones(n_indicators) / n_indicators
            else:
                weights = d / d.sum()
            
            for i, ind_code in enumerate(indicator_cols):
                indicator_weights[ind_code] = weights[i]
    
    return indicator_weights

indicator_entropy_weights = calculate_indicator_entropy_weights(df_normalized, INDICATOR_SYSTEM)

# 组合权重
final_weights = {}
for i, dim_code in enumerate(dim_codes_ordered):
    dim_weight = criteria_weights[i]
    for indicator in INDICATOR_SYSTEM[dim_code]['indicators']:
        code = indicator['code']
        entropy_weight = indicator_entropy_weights[code]
        final_weights[code] = dim_weight * entropy_weight

print("✓ 完成权重计算（AHP × 熵权法）")
print()

# ============================================================================
# 模糊综合评判
# ============================================================================

print("步骤7: 模糊综合评判（二级模糊评判）")
print("-"*80)

def fuzzy_composition_weighted_average(A, R):
    """模糊合成运算：加权平均型 M(·, ⊕)"""
    B = np.dot(A, R)
    return B

def defuzzification(B, method='weighted_average'):
    """去模糊化"""
    if method == 'weighted_average':
        scores = [95, 85, 75, 65, 50]
        final_score = np.dot(B, scores)
        return final_score
    elif method == 'max_membership':
        max_idx = np.argmax(B)
        grades = ['优秀', '良好', '中等', '及格', '较差']
        return grades[max_idx]

# 对每个测试批次进行模糊综合评判
results = []

for idx, row in df_normalized.iterrows():
    test_id = row['test_id']
    
    # 第一级：对每个维度进行模糊评判
    dim_fuzzy_vectors = {}
    dim_scores = {}
    
    for dim_code in dim_codes_ordered:
        dim_info = INDICATOR_SYSTEM[dim_code]
        indicator_codes = [ind['code'] for ind in dim_info['indicators']]
        
        # 提取该维度的指标得分
        indicator_scores = [row[code] for code in indicator_codes]
        
        # 构建模糊关系矩阵
        R_dim = build_fuzzy_relation_matrix(indicator_scores)
        
        # 计算该维度内指标的权重向量
        A_dim = np.array([indicator_entropy_weights[code] for code in indicator_codes])
        A_dim = A_dim / A_dim.sum()  # 归一化
        
        # 模糊合成
        B_dim = fuzzy_composition_weighted_average(A_dim, R_dim)
        dim_fuzzy_vectors[dim_code] = B_dim
        
        # 去模糊化得到维度得分
        dim_scores[dim_code] = defuzzification(B_dim, 'weighted_average')
    
    # 第二级：对所有维度进行综合评判
    # 构建二级模糊关系矩阵（8个维度 × 5个评语）
    R_total = np.array([dim_fuzzy_vectors[dim_code] for dim_code in dim_codes_ordered])
    
    # 使用AHP维度权重
    A_total = criteria_weights
    
    # 二级模糊合成
    B_total = fuzzy_composition_weighted_average(A_total, R_total)
    
    # 去模糊化得到最终得分
    final_score = defuzzification(B_total, 'weighted_average')
    final_grade = defuzzification(B_total, 'max_membership')
    
    results.append({
        'test_id': test_id,
        'final_score': final_score,
        'final_grade': final_grade,
        'fuzzy_vector': B_total,
        **{f'{dim}_score': dim_scores[dim] for dim in dim_codes_ordered}
    })

df_results = pd.DataFrame(results)
df_results['rank'] = df_results['final_score'].rank(ascending=False, method='min').astype(int)
df_results = df_results.sort_values('rank')

print("✓ 完成模糊综合评判")
print()

# ============================================================================
# 结果输出
# ============================================================================

print("="*80)
print("模糊综合评判结果")
print("="*80)
print()

print("综合排名:")
print("-"*80)
display_cols = ['rank', 'test_id', 'final_score', 'final_grade'] + [f'{dim}_score' for dim in dim_codes_ordered]
result_display = df_results[display_cols].copy()
result_display.columns = ['排名', '测试批次', '综合得分', '评语'] + [INDICATOR_SYSTEM[dim]['name'] for dim in dim_codes_ordered]

for col in result_display.columns[2:]:
    if col not in ['评语']:
        result_display[col] = result_display[col].apply(lambda x: f"{x:.2f}")

print(result_display.to_string(index=False))
print()

# 详细报告
print("="*80)
print("详细模糊评判报告")
print("="*80)

for idx, row in df_results.iterrows():
    test_id = row['test_id']
    final_score = row['final_score']
    final_grade = row['final_grade']
    fuzzy_vector = row['fuzzy_vector']
    rank = row['rank']
    
    print(f"\n测试批次: {test_id}")
    print(f"综合得分: {final_score:.2f}分")
    print(f"评语等级: {final_grade}")
    print(f"排名: {rank}/{len(df_results)}")
    print()
    
    print("模糊评判向量 B:")
    print(f"  优秀: {fuzzy_vector[0]:.3f}")
    print(f"  良好: {fuzzy_vector[1]:.3f}")
    print(f"  中等: {fuzzy_vector[2]:.3f}")
    print(f"  及格: {fuzzy_vector[3]:.3f}")
    print(f"  较差: {fuzzy_vector[4]:.3f}")
    print()
    
    print("维度得分:")
    for dim_code in dim_codes_ordered:
        dim_name = INDICATOR_SYSTEM[dim_code]['name']
        dim_score = row[f'{dim_code}_score']
        print(f"  {dim_name:8s}: {dim_score:6.2f}分")
    
    print("-"*80)

print()
print("="*80)
print("模糊综合评判法完成")
print("="*80)
