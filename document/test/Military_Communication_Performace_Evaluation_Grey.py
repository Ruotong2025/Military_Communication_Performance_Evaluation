#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
军事通信效能评估 - 基于灰色层次评估法
主观权重(AHP) + 客观权重(熵权法) + 灰色评估系数 → 灰色综合评估

评估方法：
1. 指标层：使用熵权法计算局部权重
2. 维度层：使用AHP计算维度权重
3. 评分方法：灰色层次评估法（白化权函数 + 灰色评估系数）

维度优先级排序（主观）：
1. 可靠性 (RL)
2. 安全性 (SC)
3. 抗干扰性 (AJ)
4. 有效性 (EF)
5. 处理能力 (PO)
6. 组网能力 (NC)
7. 人为操作 (HO)
8. 响应能力 (RS)
"""

import mysql.connector
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from datetime import datetime
import warnings
warnings.filterwarnings('ignore')

# 设置中文显示
plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei']
plt.rcParams['axes.unicode_minus'] = False

print("="*80)
print("军事通信效能评估系统 - 灰色层次评估法")
print("="*80)
print(f"评估时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
print()

# ============================================================================
# 第一部分：数据库配置和数据提取
# ============================================================================

DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': 'root',
    'database': 'military_communication_effectiveness',
    'charset': 'utf8mb4'
}

def get_db_connection():
    """获取数据库连接"""
    return mysql.connector.connect(**DB_CONFIG)

def extract_indicators_from_raw_tables():
    """
    从原始表中提取20个指标
    整合 during_battle_communications 和 communication_network_lifecycle 表的数据
    
    返回:
        DataFrame: 包含20个指标的数据框
    """
    conn = get_db_connection()
    
    # SQL查询1: 从 during_battle_communications 表聚合数据
    query_dbc = """
    SELECT 
        test_id,
        scenario_id,
        
        -- C1. 响应能力 (RS)
        AVG(call_setup_duration_ms) as RS_avg_call_setup_duration_ms,
        AVG(transmission_delay_ms) as RS_avg_transmission_delay_ms,
        
        -- C2. 处理能力 (PO)
        AVG(instant_throughput) as PO_effective_throughput,
        AVG(instant_throughput / channel_bandwidth) as PO_spectral_efficiency,
        
        -- C3. 有效性 (EF)
        AVG(communication_distance) as EF_avg_communication_distance,
        AVG(instant_ber) as EF_avg_ber,
        AVG(instant_plr) as EF_avg_plr,
        SUM(CASE WHEN communication_success = 1 THEN 1 ELSE 0 END) / COUNT(*) as EF_task_success_rate,
        
        -- C4. 可靠性 (RL) - 部分指标
        SUM(CASE WHEN communication_success = 1 THEN 1 ELSE 0 END) / COUNT(*) as RL_communication_success_rate,
        
        -- C5. 抗干扰性 (AJ)
        AVG(instant_sinr) as AJ_avg_sinr,
        AVG(jamming_margin) as AJ_avg_jamming_margin,
        
        -- C6. 人为操作 (HO)
        AVG(operator_reaction_time_ms) as HO_avg_operator_reaction_time_ms,
        SUM(CASE WHEN operation_error = 0 THEN 1 ELSE 0 END) / COUNT(*) as HO_operation_success_rate,
        
        -- C8. 安全性 (SC)
        SUM(CASE WHEN key_updated = 1 THEN 1 ELSE 0 END) / 
            NULLIF(SUM(communication_duration_ms) / 3600000.0, 0) as SC_key_compromise_frequency,
        SUM(CASE WHEN detected = 1 THEN 1 ELSE 0 END) / COUNT(*) as SC_detection_probability,
        1 - (SUM(CASE WHEN intercepted = 1 THEN 1 ELSE 0 END) / 
            NULLIF(COUNT(*), 0)) as SC_interception_resistance,
        
        -- 统计信息
        COUNT(*) as total_communications
        
    FROM during_battle_communications
    GROUP BY test_id, scenario_id
    ORDER BY test_id
    """
    
    # SQL查询2: 从 communication_network_lifecycle 表聚合数据
    query_lc = """
    SELECT 
        test_id,
        scenario_id,
        
        -- C4. 可靠性 (RL) - 剩余指标
        (SUM(total_lifecycle_duration_ms) - COALESCE(SUM(total_interruption_duration_ms), 0)) / 
            NULLIF(SUM(total_lifecycle_duration_ms), 0) as RL_communication_availability_rate,
        AVG(CASE WHEN network_crash_occurred = 1 THEN total_interruption_duration_ms ELSE NULL END) as RL_recovery_duration_ms,
        SUM(CASE WHEN network_crash_occurred = 1 THEN 1 ELSE 0 END) / COUNT(*) as RL_crash_rate,
        
        -- C7. 组网能力 (NC)
        AVG(network_setup_duration_ms) as NC_avg_network_setup_duration_ms,
        AVG(connectivity_rate) as NC_avg_connectivity_rate,
        
        -- 统计信息
        COUNT(*) as total_lifecycles
        
    FROM communication_network_lifecycle
    GROUP BY test_id, scenario_id
    ORDER BY test_id
    """
    
    # 执行查询
    df_dbc = pd.read_sql(query_dbc, conn)
    df_lc = pd.read_sql(query_lc, conn)
    conn.close()
    
    # 合并两个数据框
    df_merged = pd.merge(df_dbc, df_lc, on=['test_id', 'scenario_id'], how='left')
    
    # 处理缺失值
    df_merged['RL_communication_availability_rate'] = df_merged['RL_communication_availability_rate'].fillna(1.0)
    df_merged['RL_recovery_duration_ms'] = df_merged['RL_recovery_duration_ms'].fillna(0)
    df_merged['RL_crash_rate'] = df_merged['RL_crash_rate'].fillna(0)
    df_merged['NC_avg_connectivity_rate'] = df_merged['NC_avg_connectivity_rate'] / 100.0  # 转换为0-1
    
    return df_merged

print("步骤1: 从原始表提取20个评估指标")
print("-"*80)

# 从原始表提取数据
df_raw = extract_indicators_from_raw_tables()

print(f"✓ 成功从 during_battle_communications 表提取数据")
print(f"✓ 成功从 communication_network_lifecycle 表提取数据")
print(f"✓ 合并后共 {len(df_raw)} 个测试批次")
print(f"✓ 包含 21 个评估指标")
print()

# 显示提取的数据预览
print("提取的数据预览:")
preview_cols = ['test_id', 'RS_avg_call_setup_duration_ms', 'PO_effective_throughput', 
                'RL_crash_rate', 'SC_detection_probability', 'total_communications']
print(df_raw[preview_cols].to_string(index=False))
print()

# ============================================================================
# 第二部分：指标体系定义
# ============================================================================

print("步骤2: 定义指标体系")
print("-"*80)

# 指标配置：维度 → 指标列表
INDICATOR_SYSTEM = {
    'RL': {  # 可靠性 - 优先级1
        'name': '可靠性',
        'priority': 1,
        'indicators': [
            {'code': 'RL_communication_availability_rate', 'name': '通信可用性', 'direction': 'max'},
            {'code': 'RL_communication_success_rate', 'name': '通信成功率', 'direction': 'max'},
            {'code': 'RL_recovery_duration_ms', 'name': '恢复时长', 'direction': 'min'},
            {'code': 'RL_crash_rate', 'name': '崩溃比例', 'direction': 'min'}
        ]
    },
    'SC': {  # 安全性 - 优先级2
        'name': '安全性',
        'priority': 2,
        'indicators': [
            {'code': 'SC_key_compromise_frequency', 'name': '密钥泄露频率', 'direction': 'min'},
            {'code': 'SC_detection_probability', 'name': '被侦察概率', 'direction': 'min'},
            {'code': 'SC_interception_resistance', 'name': '抗拦截能力', 'direction': 'max'}
        ]
    },
    'AJ': {  # 抗干扰性 - 优先级3
        'name': '抗干扰性',
        'priority': 3,
        'indicators': [
            {'code': 'AJ_avg_sinr', 'name': '平均信干噪比', 'direction': 'max'},
            {'code': 'AJ_avg_jamming_margin', 'name': '平均抗干扰余量', 'direction': 'max'}
        ]
    },
    'EF': {  # 有效性 - 优先级4
        'name': '有效性',
        'priority': 4,
        'indicators': [
            {'code': 'EF_avg_communication_distance', 'name': '平均通信距离', 'direction': 'max'},
            {'code': 'EF_avg_ber', 'name': '平均误码率', 'direction': 'min'},
            {'code': 'EF_avg_plr', 'name': '平均丢包率', 'direction': 'min'},
            {'code': 'EF_task_success_rate', 'name': '任务成功率', 'direction': 'max'}
        ]
    },
    'PO': {  # 处理能力 - 优先级5
        'name': '处理能力',
        'priority': 5,
        'indicators': [
            {'code': 'PO_effective_throughput', 'name': '有效吞吐量', 'direction': 'max'},
            {'code': 'PO_spectral_efficiency', 'name': '频谱效率', 'direction': 'max'}
        ]
    },
    'NC': {  # 组网能力 - 优先级6
        'name': '组网能力',
        'priority': 6,
        'indicators': [
            {'code': 'NC_avg_network_setup_duration_ms', 'name': '平均组网时长', 'direction': 'min'},
            {'code': 'NC_avg_connectivity_rate', 'name': '平均连通率', 'direction': 'max'}
        ]
    },
    'HO': {  # 人为操作 - 优先级7
        'name': '人为操作',
        'priority': 7,
        'indicators': [
            {'code': 'HO_avg_operator_reaction_time_ms', 'name': '平均操作员反应时间', 'direction': 'min'},
            {'code': 'HO_operation_success_rate', 'name': '操作成功率', 'direction': 'max'}
        ]
    },
    'RS': {  # 响应能力 - 优先级8
        'name': '响应能力',
        'priority': 8,
        'indicators': [
            {'code': 'RS_avg_call_setup_duration_ms', 'name': '平均呼叫建立时长', 'direction': 'min'},
            {'code': 'RS_avg_transmission_delay_ms', 'name': '平均传输时延', 'direction': 'min'}
        ]
    }
}

# 打印指标体系
print("指标体系（按主观优先级排序）:")
for dim_code in sorted(INDICATOR_SYSTEM.keys(), key=lambda x: INDICATOR_SYSTEM[x]['priority']):
    dim = INDICATOR_SYSTEM[dim_code]
    print(f"  {dim['priority']}. {dim_code} - {dim['name']} ({len(dim['indicators'])}个指标)")

print(f"\n总计: 8个维度, 21个指标")
print()

# ============================================================================
# 第三部分：数据标准化（0-100分）
# ============================================================================

print("步骤3: 数据标准化（归一化到0-100分）")
print("-"*80)
print("使用归一化方法: Min-Max归一化")
print("说明: 所有指标统一使用Min-Max归一化，简单直观，适合小样本数据")
print("      极小值指标（误码率、丢包率）先进行对数变换")
print()

# 定义极小值指标（需要对数变换）
# 这些指标跨越多个数量级（10⁻⁶ ~ 10⁻²），需要对数变换将指数级差异转为线性差异
LOGARITHMIC_INDICATORS = {
    'EF_avg_ber',   # 误码率（10⁻⁵ ~ 10⁻³，跨3个数量级）
    'EF_avg_plr',   # 丢包率（10⁻⁴ ~ 10⁻²，跨2个数量级）
}

def normalize_indicator(series, direction, indicator_code=None):
    """
    Min-Max归一化方法：所有指标都使用Min-Max归一化
    
    处理流程：
    1. 极小值指标（误码率、丢包率）→ 先对数变换，将指数级差异转为线性差异
    2. 所有指标 → 统一使用Min-Max归一化到0-100分
    
    原理（Min-Max归一化）:
        normalized = (x - min) / (max - min) × 100
        
        对于逆向指标（越小越好）：
        normalized = (max - x) / (max - min) × 100
    
    参数:
        series: 指标数据序列
        direction: 'max'表示越大越好，'min'表示越小越好
        indicator_code: 指标代码，用于判断是否需要对数变换
    
    返回:
        归一化后的序列（0-100分）
    
    优点:
        - 简单直观，易于理解
        - 保留原始数据的相对关系
        - 不受样本量影响
        - 适合小样本数据
        - 即使只有2个不同值也能正常工作
    """
    # 处理空值
    if series.isna().all():
        return pd.Series([50] * len(series), index=series.index)
    
    # 步骤1：预处理（对数变换）
    # 只对极小值指标进行对数变换，将指数级差异转为线性差异
    if indicator_code in LOGARITHMIC_INDICATORS:
        # 对数变换：-log10(value)
        # 误码率：10⁻⁵ → 5, 10⁻³ → 3
        # 丢包率：10⁻⁴ → 4, 10⁻² → 2
        series = -np.log10(series + 1e-10)  # 加极小值避免log(0)
    
    # 步骤2：Min-Max归一化
    min_val = series.min()
    max_val = series.max()
    
    # 如果最大值等于最小值（所有值相同），返回中等水平
    if max_val == min_val:
        return pd.Series([50] * len(series), index=series.index)
    
    # Min-Max归一化
    if direction == 'max':
        # 越大越好：(x - min) / (max - min) × 100
        normalized = (series - min_val) / (max_val - min_val) * 100
    else:
        # 越小越好：(max - x) / (max - min) × 100
        normalized = (max_val - series) / (max_val - min_val) * 100
    
    return normalized

print("极小值指标（先对数变换，再Min-Max归一化）:")
for idx, indicator in enumerate(sorted(LOGARITHMIC_INDICATORS), 1):
    print(f"  {idx}. {indicator}")
print()
print("其他指标（直接Min-Max归一化）:")
print("  - 概率类: 成功率、可用性、连通率等（0-1范围）")
print("  - 时延类: 呼叫建立时长、传输时延、反应时间等")
print("  - 距离类: 通信距离")
print("  - 吞吐量: 有效吞吐量、频谱效率")
print("  - 信号类: 信干噪比、抗干扰余量")
print("  - 频率类: 密钥泄露频率")
print("  - 等等...")
print()
print("✅ 优点: 简单直观，适合小样本数据，不会出现IQR=0的问题")
print()

# 创建标准化数据框
df_normalized = df_raw[['test_id', 'scenario_id']].copy()

# 对每个指标进行标准化
for dim_code, dim_info in INDICATOR_SYSTEM.items():
    for indicator in dim_info['indicators']:
        col_name = indicator['code']
        direction = indicator['direction']
        
        if col_name in df_raw.columns:
            # 传入指标代码，用于判断是否为概率类指标
            df_normalized[col_name] = normalize_indicator(df_raw[col_name], direction, col_name)
        else:
            print(f"⚠ 警告: 指标 {col_name} 不存在于数据中")
            df_normalized[col_name] = 50  # 默认中等水平

print(f"✓ 完成21个指标的标准化")
print(f"✓ 标准化后数据范围: 0-100分")
print()

# 显示标准化后的数据预览
print("标准化后数据预览（前3个测试批次）:")
display_cols = ['test_id'] + [ind['code'] for dim in INDICATOR_SYSTEM.values() for ind in dim['indicators'][:2]]
print(df_normalized[display_cols].head(3).to_string(index=False))
print()

# ============================================================================
# 第四部分：熵权法计算客观权重
# ============================================================================

print("步骤4: 混合赋权法计算权重")
print("-"*80)
print("说明: 采用混合赋权法")
print("      一级维度（8个维度）：使用AHP主观权重")
print("      二级指标（维度内部）：使用熵权法客观权重")
print("      最终权重 = AHP维度权重 × 熵权法指标权重")
print()

def calculate_indicator_entropy_weights(df_norm, indicator_system):
    """
    在每个维度内部使用熵权法计算指标的客观权重
    
    参数:
        df_norm: 标准化后的数据框
        indicator_system: 指标体系
    
    返回:
        维度内指标权重字典 {indicator_code: weight_in_dimension}
    """
    
    print("在每个维度内部计算指标的熵权（客观权重）:")
    
    indicator_weights = {}
    
    for dim_code, dim_info in sorted(indicator_system.items(), key=lambda x: x[1]['priority']):
        print(f"\n  {dim_info['name']} ({dim_code}):")
        
        indicator_cols = [ind['code'] for ind in dim_info['indicators']]
        
        # 提取该维度的指标数据
        dim_indicator_data = df_norm[indicator_cols].values
        n_samples, n_indicators = dim_indicator_data.shape
        
        if n_indicators == 1:
            # 只有1个指标，权重为1
            indicator_weights[indicator_cols[0]] = 1.0
            print(f"    {dim_info['indicators'][0]['name']}: 1.000000 (100.00%)")
        else:
            # 计算维度内指标的熵权
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
                else:
                    entropy[j] = 0
            
            d = 1 - entropy
            
            # 避免所有权重为0的情况
            if d.sum() == 0:
                # 如果所有指标的信息熵都是1（完全均匀分布），则平均分配权重
                weights = np.ones(n_indicators) / n_indicators
            else:
                weights = d / d.sum()
            
            # 存储维度内权重
            for i, ind_code in enumerate(indicator_cols):
                indicator_weights[ind_code] = weights[i]
                ind_name = dim_info['indicators'][i]['name']
                print(f"    {ind_name}: {weights[i]:.6f} ({weights[i]*100:.2f}%)")
    
    print()
    return indicator_weights

# 计算维度内指标的熵权（客观权重）
indicator_entropy_weights = calculate_indicator_entropy_weights(df_normalized, INDICATOR_SYSTEM)

print()

# ============================================================================
# 第五部分：AHP层次分析法计算主观权重
# ============================================================================

print("步骤5: AHP层次分析法计算主观权重（α）")
print("-"*80)

def ahp_calculate_weights(judgment_matrix):
    """
    AHP计算权重
    
    参数:
        judgment_matrix: 判断矩阵（numpy数组）
    
    返回:
        weights: 权重向量
        CR: 一致性比率
    """
    n = len(judgment_matrix)
    
    # 计算特征值和特征向量
    eigenvalues, eigenvectors = np.linalg.eig(judgment_matrix)
    
    # 找到最大特征值及其对应的特征向量
    max_eigenvalue_index = np.argmax(eigenvalues.real)
    max_eigenvalue = eigenvalues[max_eigenvalue_index].real
    max_eigenvector = eigenvectors[:, max_eigenvalue_index].real
    
    # 归一化得到权重
    weights = max_eigenvector / max_eigenvector.sum()
    
    # 一致性检验
    CI = (max_eigenvalue - n) / (n - 1)
    
    # RI值表（随机一致性指标）
    RI_dict = {1: 0, 2: 0, 3: 0.58, 4: 0.90, 5: 1.12, 6: 1.24, 7: 1.32, 
               8: 1.41, 9: 1.45, 10: 1.49}
    RI = RI_dict.get(n, 1.41)
    
    CR = CI / RI if RI != 0 else 0
    
    return weights, CR

# 5.1 准则层判断矩阵（8个维度）
# 优先级: RL > SC > AJ > EF > PO > NC > HO > RS
print("5.1 准则层判断矩阵（8个维度）")
print("    优先级: 可靠性 > 安全性 > 抗干扰性 > 有效性 > 处理能力 > 组网能力 > 人为操作 > 响应能力")

# 构建判断矩阵（按优先级排序）
# RL, SC, AJ, EF, PO, NC, HO, RS
criteria_matrix = np.array([
    [1,   2,   3,   4,   5,   6,   7,   8],    # RL 可靠性
    [1/2, 1,   2,   3,   4,   5,   6,   7],    # SC 安全性
    [1/3, 1/2, 1,   2,   3,   4,   5,   6],    # AJ 抗干扰性
    [1/4, 1/3, 1/2, 1,   2,   3,   4,   5],    # EF 有效性
    [1/5, 1/4, 1/3, 1/2, 1,   2,   3,   4],    # PO 处理能力
    [1/6, 1/5, 1/4, 1/3, 1/2, 1,   2,   3],    # NC 组网能力
    [1/7, 1/6, 1/5, 1/4, 1/3, 1/2, 1,   2],    # HO 人为操作
    [1/8, 1/7, 1/6, 1/5, 1/4, 1/3, 1/2, 1]     # RS 响应能力
])

criteria_weights, criteria_CR = ahp_calculate_weights(criteria_matrix)

print(f"    一致性比率 CR = {criteria_CR:.4f}", end="")
if criteria_CR < 0.1:
    print(" ✓ (通过一致性检验)")
else:
    print(" ✗ (未通过一致性检验)")

# 维度代码（按优先级排序）
dim_codes_ordered = ['RL', 'SC', 'AJ', 'EF', 'PO', 'NC', 'HO', 'RS']

print("\n    准则层权重:")
for i, dim_code in enumerate(dim_codes_ordered):
    weight = criteria_weights[i]
    print(f"      {INDICATOR_SYSTEM[dim_code]['name']} ({dim_code}): {weight:.6f} ({weight*100:.2f}%)")

print()
print("说明: 准则层（维度层）使用AHP主观权重")
print("      因素层（指标层）使用熵权法客观权重（已在步骤4中计算）")
print()

# 5.2 定义灰色层次评估法的白化权函数（端点三角形白化权函数 - 无平台）
print("5.2 定义灰色层次评估法的白化权函数")
print("    采用端点三角形白化权函数（5灰类，无平台区域）")
print("-"*80)

# 端点定义
a = {
    0: -2,   # 左延拓
    1: 0,    # 第1个端点
    2: 2,    # 第2个端点
    3: 4,    # 第3个端点
    4: 6,    # 第4个端点
    5: 8,    # 第5个端点
    6: 10,   # 第6个端点
    7: 12    # 右延拓
}

print("    端点定义：a_0=-2, a_1=0, a_2=2, a_3=4, a_4=6, a_5=8, a_6=10, a_7=12")

# 灰类名称
grey_names = {1: "劣", 2: "差", 3: "中", 4: "良", 5: "优"}

print("    灰类区间定义（按公式 [a_{k-1}, a_{k+2}]）：")
for k in [1, 2, 3, 4, 5]:
    a_k_minus_1 = a[k-1]
    a_k_plus_2 = a[k+2]
    print(f"      灰类{k}（{grey_names[k]}）：[{a_k_minus_1}, {a_k_plus_2}]")
print()

def calculate_grey_params(k):
    """计算灰类k的参数"""
    a_k = a[k]
    a_k_plus_1 = a[k+1]
    
    # λ_k = (a_k + a_{k+1}) / 2
    lambda_k = (a_k + a_k_plus_1) / 2
    
    # 区间边界
    a_k_minus_1 = a[k-1]
    a_k_plus_2 = a[k+2]
    
    # 梯形平台参数（用于对比）
    c_k = (a_k + lambda_k) / 2
    d_k = (lambda_k + a_k_plus_1) / 2
    
    return {
        'a_k': a_k,
        'a_k_plus_1': a_k_plus_1,
        'lambda_k': lambda_k,
        'c_k': c_k,
        'd_k': d_k,
        'a_k_minus_1': a_k_minus_1,
        'a_k_plus_2': a_k_plus_2
    }

def whitening_function_k(x, k):
    """
    通用的第k个灰类三角形白化权函数（无平台）
    左边界: a_{k-1}, 顶点: λ_k, 右边界: a_{k+2}
    """
    params = calculate_grey_params(k)
    
    a_k_minus_1 = params['a_k_minus_1']
    lambda_k = params['lambda_k']
    a_k_plus_2 = params['a_k_plus_2']
    
    if x < a_k_minus_1 or x > a_k_plus_2:
        return 0
    elif a_k_minus_1 <= x < lambda_k:
        # 上升段 - 到顶点
        return (x - a_k_minus_1) / (lambda_k - a_k_minus_1)
    elif x == lambda_k:
        return 1
    elif lambda_k < x <= a_k_plus_2:
        # 下降段 - 从顶点下降
        return (a_k_plus_2 - x) / (a_k_plus_2 - lambda_k)
    else:
        return 0

def whitening_function_k_trapezoid(x, k):
    """
    梯形白化权函数（带平台）- 仅用于对比
    """
    params = calculate_grey_params(k)
    
    a_k_minus_1 = params['a_k_minus_1']
    c_k = params['c_k']
    d_k = params['d_k']
    a_k_plus_2 = params['a_k_plus_2']
    
    if x < a_k_minus_1 or x > a_k_plus_2:
        return 0
    elif a_k_minus_1 <= x < c_k:
        return (x - a_k_minus_1) / (c_k - a_k_minus_1)
    elif c_k <= x <= d_k:
        return 1
    elif d_k < x <= a_k_plus_2:
        return (a_k_plus_2 - x) / (a_k_plus_2 - d_k)
    else:
        return 0

# 创建5个三角形白化权函数（无平台）
def whitening_function_1(x):
    """灰类1（劣）- 三角形"""
    return whitening_function_k(x, 1)

def whitening_function_2(x):
    """灰类2（差）- 三角形"""
    return whitening_function_k(x, 2)

def whitening_function_3(x):
    """灰类3（中）- 三角形"""
    return whitening_function_k(x, 3)

def whitening_function_4(x):
    """灰类4（良）- 三角形"""
    return whitening_function_k(x, 4)

def whitening_function_5(x):
    """灰类5（优）- 三角形"""
    return whitening_function_k(x, 5)

# 创建5个梯形白化权函数（带平台）- 用于对比
def whitening_function_1_trapezoid(x):
    """灰类1（劣）- 梯形"""
    return whitening_function_k_trapezoid(x, 1)

def whitening_function_2_trapezoid(x):
    """灰类2（差）- 梯形"""
    return whitening_function_k_trapezoid(x, 2)

def whitening_function_3_trapezoid(x):
    """灰类3（中）- 梯形"""
    return whitening_function_k_trapezoid(x, 3)

def whitening_function_4_trapezoid(x):
    """灰类4（良）- 梯形"""
    return whitening_function_k_trapezoid(x, 4)

def whitening_function_5_trapezoid(x):
    """灰类5（优）- 梯形"""
    return whitening_function_k_trapezoid(x, 5)

WHITENING_FUNCTIONS = [
    whitening_function_1,
    whitening_function_2,
    whitening_function_3,
    whitening_function_4,
    whitening_function_5
]

WHITENING_FUNCTIONS_TRAPEZOID = [
    whitening_function_1_trapezoid,
    whitening_function_2_trapezoid,
    whitening_function_3_trapezoid,
    whitening_function_4_trapezoid,
    whitening_function_5_trapezoid
]

# 计算灰类代表值（使用λ_k）
GREY_CLASS_VALUES = [
    calculate_grey_params(1)['lambda_k'],  # 劣: 1.0
    calculate_grey_params(2)['lambda_k'],  # 差: 3.0
    calculate_grey_params(3)['lambda_k'],  # 中: 5.0
    calculate_grey_params(4)['lambda_k'],  # 良: 7.0
    calculate_grey_params(5)['lambda_k'],  # 优: 9.0
]

print("✓ 白化权函数定义完成（三角形，无平台）")
print(f"✓ 灰类代表值（λ值）: {GREY_CLASS_VALUES}")
print()

# 打印详细参数
print("    灰类参数详细计算（三角形）：")
for k in [1, 2, 3, 4, 5]:
    params = calculate_grey_params(k)
    print(f"      灰类{k}（{grey_names[k]}）：λ_{k}={params['lambda_k']:.1f}, 顶点x={params['lambda_k']:.1f}")
print()

# 绘制白化权函数图形
print("绘制白化权函数图形...")
print("-"*80)

x_plot = np.linspace(-2, 12, 700)

# 三角形函数值
y1 = np.array([whitening_function_1(xi) for xi in x_plot])
y2 = np.array([whitening_function_2(xi) for xi in x_plot])
y3 = np.array([whitening_function_3(xi) for xi in x_plot])
y4 = np.array([whitening_function_4(xi) for xi in x_plot])
y5 = np.array([whitening_function_5(xi) for xi in x_plot])

# 梯形函数值（用于对比）
y1_trap = np.array([whitening_function_1_trapezoid(xi) for xi in x_plot])
y2_trap = np.array([whitening_function_2_trapezoid(xi) for xi in x_plot])
y3_trap = np.array([whitening_function_3_trapezoid(xi) for xi in x_plot])
y4_trap = np.array([whitening_function_4_trapezoid(xi) for xi in x_plot])
y5_trap = np.array([whitening_function_5_trapezoid(xi) for xi in x_plot])

# 颜色和名称定义
colors = ['purple', 'orange', 'blue', 'green', 'red']
class_names_en = ['Poor', 'Bad', 'Medium', 'Good', 'Excellent']
class_names_cn = ['劣', '差', '中', '良', '优']
lambda_values = [f'{v:.1f}' for v in GREY_CLASS_VALUES]

# 图1：三角形白化权函数（无平台）
fig_whitening = plt.figure(figsize=(16, 9))

# 绘制三角形函数曲线
plt.plot(x_plot, y1, color=colors[0], linewidth=3, 
         label=f'Class 1 ({class_names_en[0]}) 三角形 [-2,2] λ={lambda_values[0]}')
plt.plot(x_plot, y2, color=colors[1], linewidth=3, 
         label=f'Class 2 ({class_names_en[1]}) 三角形 [0,4] λ={lambda_values[1]}')
plt.plot(x_plot, y3, color=colors[2], linewidth=3, 
         label=f'Class 3 ({class_names_en[2]}) 三角形 [2,6] λ={lambda_values[2]}')
plt.plot(x_plot, y4, color=colors[3], linewidth=3, 
         label=f'Class 4 ({class_names_en[3]}) 三角形 [4,8] λ={lambda_values[3]}')
plt.plot(x_plot, y5, color=colors[4], linewidth=3, 
         label=f'Class 5 ({class_names_en[4]}) 三角形 [6,10] λ={lambda_values[4]}')

# 标注端点
for i in range(8):
    plt.axvline(a[i], color='gray', linestyle=':', alpha=0.5, linewidth=1)
    plt.text(a[i], -0.08, f'a{i}={a[i]}', ha='center', fontsize=9, color='gray')

# 标注顶点
for k in [1, 2, 3, 4, 5]:
    params = calculate_grey_params(k)
    color = colors[k-1]
    
    # 标注顶点（三角形）
    plt.scatter([params['lambda_k']], [1], color=color, s=180, zorder=5, 
                marker='^', edgecolors='black', linewidths=2)
    plt.axvline(params['lambda_k'], color=color, linestyle='--', alpha=0.3, linewidth=1)
    
    # 添加λ标注
    plt.text(params['lambda_k'], 1.05, f'λ={params["lambda_k"]:.1f}', 
             ha='center', fontsize=10, fontweight='bold', color=color)

plt.title('端点三角形白化权函数 (End-point Triangular Whitenization Functions) - 无平台', 
          fontsize=15, fontweight='bold')
plt.xlabel('指标得分 (Index Score x)', fontsize=13)
plt.ylabel('白化权函数值 (Whitenization Value)', fontsize=13)
plt.grid(alpha=0.25, linestyle='--')
plt.legend(loc='upper left', fontsize=10, framealpha=0.9)
plt.ylim(-0.2, 1.2)
plt.xlim(-2, 12)

# 添加三角形函数公式说明
formula_text = '三角形函数 (无平台):\n' \
               'f(x) = 0 (x < a_{k-1} or x > a_{k+2})\n' \
               'f(x) = (x - a_{k-1})/(λ_k - a_{k-1}) (a_{k-1} ≤ x < λ_k)\n' \
               'f(x) = 1 (x = λ_k)\n' \
               'f(x) = (a_{k+2} - x)/(a_{k+2} - λ_k) (λ_k < x ≤ a_{k+2})'
plt.text(11.5, 0.3, formula_text, fontsize=10, 
         bbox=dict(boxstyle='round,pad=0.5', facecolor='lightblue', alpha=0.8),
         verticalalignment='center', horizontalalignment='right')

# 添加重叠区域说明
plt.text(1, 0.3, '重叠区域\n[0,2]', ha='center', fontsize=9, 
         bbox=dict(boxstyle='round,pad=0.3', facecolor='gray', alpha=0.15))
plt.text(3, 0.3, '重叠区域\n[2,4]', ha='center', fontsize=9,
         bbox=dict(boxstyle='round,pad=0.3', facecolor='gray', alpha=0.15))
plt.text(5, 0.3, '重叠区域\n[4,6]', ha='center', fontsize=9,
         bbox=dict(boxstyle='round,pad=0.3', facecolor='gray', alpha=0.15))
plt.text(7, 0.3, '重叠区域\n[6,8]', ha='center', fontsize=9,
         bbox=dict(boxstyle='round,pad=0.3', facecolor='gray', alpha=0.15))

plt.tight_layout()

# 显示白化权函数图形
print(f"✓ 三角形白化权函数图已生成，正在显示...")
plt.show()
plt.close(fig_whitening)

# 图2：三角形 vs 梯形 对比图（灰类3（中）作为示例）
print(f"✓ 生成三角形 vs 梯形对比图...")
fig_compare = plt.figure(figsize=(16, 9))

# 绘制三角形（无平台）- 实线
plt.plot(x_plot, y3, 'b-', linewidth=3.5, 
         label=f'三角形函数 (无平台) - 灰类3 中 λ={lambda_values[2]}')

# 绘制梯形（带平台）- 虚线
plt.plot(x_plot, y3_trap, 'b--', linewidth=3, 
         label=f'梯形函数 (带平台) - 灰类3 中 λ={lambda_values[2]}')

# 标注对比区域
params = calculate_grey_params(3)
c_k = params['c_k']
d_k = params['d_k']

# 三角形顶点
plt.scatter([params['lambda_k']], [1], color='blue', s=200, zorder=5, 
            marker='^', edgecolors='black', linewidths=2, label='三角形顶点')

# 梯形平台区域
plt.axvline(c_k, color='blue', linestyle=':', alpha=0.7, linewidth=1.5)
plt.axvline(d_k, color='blue', linestyle=':', alpha=0.7, linewidth=1.5)
plt.axvspan(c_k, d_k, alpha=0.15, color='blue')
plt.text((c_k + d_k)/2, 0.5, '梯形平台区域', ha='center', fontsize=11, 
         bbox=dict(boxstyle='round,pad=0.3', facecolor='yellow', alpha=0.7))

# 添加对比说明
diff_text = '关键区别:\n' \
            '• 三角形: 仅在λ点处f(x)=1，无平台\n' \
            '• 梯形: 在[c_k, d_k]区间f(x)=1，有平台\n' \
            '• 三角形更严格，梯形更宽松\n' \
            f'• 三角形顶点: x={params["lambda_k"]:.1f}\n' \
            f'• 梯形平台: [{c_k:.2f}, {d_k:.2f}]'
plt.text(7, 0.7, diff_text, fontsize=12, 
         bbox=dict(boxstyle='round,pad=0.7', facecolor='lightyellow', alpha=0.9, edgecolor='blue'),
         verticalalignment='center')

plt.title('三角形 vs 梯形白化权函数对比 (灰类3 中)', fontsize=15, fontweight='bold')
plt.xlabel('指标得分 (Index Score x)', fontsize=13)
plt.ylabel('白化权函数值 (Whitenization Value)', fontsize=13)
plt.grid(alpha=0.25, linestyle='--')
plt.legend(loc='upper right', fontsize=12, framealpha=0.9)
plt.ylim(-0.1, 1.1)
plt.xlim(1, 7)
plt.tight_layout()
plt.show()
plt.close(fig_compare)

# 输出归一化示例
print("\n归一化白化权向量示例（各灰类权重之和=1）：")
print("得分\t劣\t差\t中\t良\t优\t辅助得分(10分制)")
test_scores = [1.0, 3.0, 5.0, 7.0, 9.0]
for s in test_scores:
    w = [f(s) for f in WHITENING_FUNCTIONS]
    w_sum = sum(w)
    if w_sum > 0:
        w_norm = [wi / w_sum for wi in w]
    else:
        w_norm = [0.2, 0.2, 0.2, 0.2, 0.2]
    
    # 计算辅助得分
    aux_score = sum(w_norm[i] * GREY_CLASS_VALUES[i] for i in range(5))
    
    print(f'{s:.1f}\t{w_norm[0]:.3f}\t{w_norm[1]:.3f}\t{w_norm[2]:.3f}\t{w_norm[3]:.3f}\t{w_norm[4]:.3f}\t{aux_score:.2f}')
print()
print("说明: 辅助得分 = 灰色评估权向量 · [1.0, 3.0, 5.0, 7.0, 9.0]")
print("      三角形函数特点：仅在λ点处达到最大值1，无平台区域")
print()


# ============================================================================
# 第六部分：灰色层次评估计算
# ============================================================================

print("步骤6: 灰色层次评估计算")
print("-"*80)
print("使用白化权函数计算灰色评估系数")
print()

def calculate_grey_coefficients(value_100):
    """
    计算单个指标值的灰色评估系数
    
    参数:
        value_100: 标准化后的指标值（0-100分）
    
    返回:
        grey_vector: 灰色评估权向量 [r1, r2, r3, r4, r5]
    """
    # 转换到10分制
    value_10 = value_100 / 10.0
    
    # 计算白化权函数值
    f_values = [f(value_10) for f in WHITENING_FUNCTIONS]
    
    # 归一化得到灰色评估权向量
    total = sum(f_values)
    if total == 0:
        # 如果所有白化权函数值都为0，返回均匀分布
        return [0.2, 0.2, 0.2, 0.2, 0.2]
    else:
        return [f / total for f in f_values]

# 6.1 计算指标层灰色评估系数
print("6.1 计算指标层灰色评估系数")
print("    对每个指标的标准化值计算其对5个灰类的隶属度")
print()

# 创建得分数据框
df_scores = df_normalized[['test_id', 'scenario_id']].copy()

# 存储每个指标的灰色评估权向量
df_grey_coefficients = {}

for dim_code, dim_info in INDICATOR_SYSTEM.items():
    for indicator in dim_info['indicators']:
        code = indicator['code']
        
        # 对每个测试方案的该指标计算灰色评估权向量
        grey_vectors = []
        for idx, row in df_normalized.iterrows():
            value = row[code]
            grey_vector = calculate_grey_coefficients(value)
            grey_vectors.append(grey_vector)
        
        df_grey_coefficients[code] = grey_vectors

print(f"✓ 完成21个指标的灰色评估系数计算")
print(f"✓ 每个指标生成5个灰色评估系数（对应5个灰类）")
print()

# 6.2 计算维度层灰色评估
print("6.2 计算维度层灰色评估")
print("    使用熵权法局部权重加权指标层灰色评估系数")
print()

for dim_code, dim_info in INDICATOR_SYSTEM.items():
    print(f"  计算 {dim_info['name']} ({dim_code}) 维度的灰色评估...")
    
    # 对每个测试方案
    dim_grey_vectors = []
    for test_idx in range(len(df_normalized)):
        # 初始化维度的灰色评估权向量 [r1, r2, r3, r4, r5]
        dim_grey_vector = [0.0, 0.0, 0.0, 0.0, 0.0]
        
        # 使用熵权法局部权重加权
        for indicator in dim_info['indicators']:
            code = indicator['code']
            entropy_weight = indicator_entropy_weights[code]  # 熵权法局部权重
            
            # 该指标的灰色评估权向量
            indicator_grey_vector = df_grey_coefficients[code][test_idx]
            
            # 加权累加
            for i in range(5):
                dim_grey_vector[i] += entropy_weight * indicator_grey_vector[i]
        
        dim_grey_vectors.append(dim_grey_vector)
    
    # 存储维度的灰色评估权向量
    for i in range(5):
        df_scores[f'{dim_code}_grey_{i+1}'] = [vec[i] for vec in dim_grey_vectors]
    
    # 计算维度得分（10分制）
    dim_scores_10 = [sum(vec[i] * GREY_CLASS_VALUES[i] for i in range(5)) for vec in dim_grey_vectors]
    
    # 转换回100分制
    df_scores[f'{dim_code}_score'] = [score * 10 for score in dim_scores_10]

print(f"✓ 完成8个维度的灰色评估计算")
print()

# 6.3 计算目标层综合评估
print("6.3 计算目标层综合评估")
print("    使用AHP维度权重加权维度层灰色评估系数")
print()

# 对每个测试方案
total_grey_vectors = []
for test_idx in range(len(df_normalized)):
    # 初始化总体灰色评估权向量 [r1, r2, r3, r4, r5]
    total_grey_vector = [0.0, 0.0, 0.0, 0.0, 0.0]
    
    # 使用AHP维度权重加权
    for i, dim_code in enumerate(dim_codes_ordered):
        ahp_weight = criteria_weights[i]  # AHP维度权重
        
        # 该维度的灰色评估权向量
        dim_grey_vector = [
            df_scores.iloc[test_idx][f'{dim_code}_grey_1'],
            df_scores.iloc[test_idx][f'{dim_code}_grey_2'],
            df_scores.iloc[test_idx][f'{dim_code}_grey_3'],
            df_scores.iloc[test_idx][f'{dim_code}_grey_4'],
            df_scores.iloc[test_idx][f'{dim_code}_grey_5']
        ]
        
        # 加权累加
        for j in range(5):
            total_grey_vector[j] += ahp_weight * dim_grey_vector[j]
    
    total_grey_vectors.append(total_grey_vector)

# 存储总体灰色评估权向量
for j in range(5):
    df_scores[f'total_grey_{j+1}'] = [vec[j] for vec in total_grey_vectors]

# 计算综合得分（10分制）
total_scores_10 = [sum(vec[j] * GREY_CLASS_VALUES[j] for j in range(5)) for vec in total_grey_vectors]

# 转换回100分制
df_scores['total_score'] = [score * 10 for score in total_scores_10]

print(f"✓ 完成综合评估计算")
print()

# 6.4 评估等级
def get_grade(score):
    """
    根据灰色评估得分确定等级
    基于10分制的灰类定义转换到100分制
    """
    if score >= 90:  # 对应10分制的9分以上
        return '优秀'
    elif score >= 70:  # 对应10分制的7分以上
        return '良好'
    elif score >= 50:  # 对应10分制的5分以上
        return '中等'
    elif score >= 30:  # 对应10分制的3分以上
        return '较差'
    else:  # 对应10分制的3分以下
        return '很差'

df_scores['grade'] = df_scores['total_score'].apply(get_grade)

# 6.5 排名
df_scores['rank'] = df_scores['total_score'].rank(ascending=False, method='min').astype(int)

# 按排名排序
df_scores = df_scores.sort_values('rank')

print("✓ 完成综合评分计算")
print()

# ============================================================================
# 第八部分：评估结果输出
# ============================================================================

print("="*80)
print("评估结果汇总")
print("="*80)
print()

# 输出排名表（基于灰色评估权向量）
print("综合排名（基于灰色层次评估法）:")
print("="*130)
print(f"{'排名':<6}{'测试批次':<15}{'灰色评估权向量 [劣,差,中,良,优]':<45}{'辅助得分':<12}{'主导等级':<15}{'评级':<10}")
print("="*130)

for idx, row in df_scores.iterrows():
    rank = row['rank']
    test_id = row['test_id']
    grey_vec = [row[f'total_grey_{i}'] for i in range(1, 6)]
    aux_score = row['total_score']
    grade = row['grade']
    
    # 确定主导等级
    max_idx = grey_vec.index(max(grey_vec))
    dominant_class = ['很差', '较差', '中等', '良好', '优秀'][max_idx]
    dominant_percent = grey_vec[max_idx] * 100
    
    grey_vec_str = f"[{grey_vec[0]:.3f}, {grey_vec[1]:.3f}, {grey_vec[2]:.3f}, {grey_vec[3]:.3f}, {grey_vec[4]:.3f}]"
    
    print(f"{rank:<6}{test_id:<15}{grey_vec_str:<45}{aux_score:>6.2f}分    {dominant_class}({dominant_percent:.0f}%)    {grade:<10}")

print("="*130)
print()
print("说明:")
print("  1. 主要结果: 灰色评估权向量 - 反映对各灰类的隶属度分布")
print("  2. 辅助得分: 便于快速比较的单一数值（灰色评估权向量 × 灰类代表值 × 10）")
print("  3. 主导等级: 权重最大的灰类")
print("  4. 排名依据: 基于辅助得分")
print()

# 输出维度得分表
print("\n维度得分汇总:")
print("-"*80)
dim_result_cols = ['rank', 'test_id'] + [f'{dim}_score' for dim in dim_codes_ordered]
dim_result_display = df_scores[dim_result_cols].copy()
dim_result_display.columns = ['排名', '测试批次'] + [INDICATOR_SYSTEM[dim]['name'] for dim in dim_codes_ordered]

# 格式化输出
for col in dim_result_display.columns[2:]:
    dim_result_display[col] = dim_result_display[col].apply(lambda x: f"{x:.2f}")

print(dim_result_display.to_string(index=False))
print()

# 详细报告（每个测试批次）
print("="*80)
print("详细评估报告")
print("="*80)

for idx, row in df_scores.iterrows():
    test_id = row['test_id']
    total_score = row['total_score']
    grade = row['grade']
    rank = row['rank']
    
    print(f"\n测试批次: {test_id}")
    print(f"综合得分: {total_score:.2f}分")
    print(f"评估等级: {grade}")
    print(f"排名: {rank}/{len(df_scores)}")
    
    # 显示灰色评估权向量
    grey_vector = [row[f'total_grey_{i}'] for i in range(1, 6)]
    print(f"灰色评估权向量: [{grey_vector[0]:.3f}, {grey_vector[1]:.3f}, {grey_vector[2]:.3f}, {grey_vector[3]:.3f}, {grey_vector[4]:.3f}]")
    print(f"  → 很差倾向: {grey_vector[0]*100:.1f}%")
    print(f"  → 较差倾向: {grey_vector[1]*100:.1f}%")
    print(f"  → 中等倾向: {grey_vector[2]*100:.1f}%")
    print(f"  → 良好倾向: {grey_vector[3]*100:.1f}%")
    print(f"  → 优秀倾向: {grey_vector[4]*100:.1f}%")
    print()
    
    print("维度得分:")
    for dim_code in dim_codes_ordered:
        dim_name = INDICATOR_SYSTEM[dim_code]['name']
        dim_score = row[f'{dim_code}_score']
        dim_grey = [row[f'{dim_code}_grey_{i}'] for i in range(1, 6)]
        print(f"  {dim_name:8s}: {dim_score:6.2f}分  灰色评估: [{dim_grey[0]:.2f}, {dim_grey[1]:.2f}, {dim_grey[2]:.2f}, {dim_grey[3]:.2f}, {dim_grey[4]:.2f}]", end="")
        
        # 特殊标记
        if dim_code == 'RL':
            crash_rate = df_raw.loc[df_raw['test_id'] == test_id, 'RL_crash_rate'].values[0]
            if crash_rate == 0:
                print(" ⭐ (无崩溃)")
            else:
                print(f" ⚠ (崩溃比例: {crash_rate*100:.1f}%)")
        else:
            print()
    
    print()
    print("关键指标:")
    test_raw = df_raw[df_raw['test_id'] == test_id].iloc[0]
    
    # 可靠性关键指标
    crash_rate = test_raw['RL_crash_rate']
    availability = test_raw['RL_communication_availability_rate']
    success_rate = test_raw['RL_communication_success_rate']
    recovery_time = test_raw['RL_recovery_duration_ms']
    
    if crash_rate == 0:
        print(f"  ✓ 崩溃比例: 0% (优秀)")
    else:
        print(f"  ✗ 崩溃比例: {crash_rate*100:.1f}% (需改进)")
    
    print(f"  {'✓' if availability >= 0.95 else '⚠'} 通信可用性: {availability*100:.1f}%")
    print(f"  {'✓' if success_rate >= 0.90 else '⚠'} 通信成功率: {success_rate*100:.1f}%")
    
    if recovery_time == 0:
        print(f"  ✓ 恢复时长: 0ms (无崩溃)")
    else:
        print(f"  ⚠ 恢复时长: {recovery_time:.0f}ms ({recovery_time/1000:.1f}秒)")
    
    print("-"*80)

print()

# ============================================================================
# 第九部分：存储评估数据到数据库
# ============================================================================

print("步骤8: 存储评估数据到数据库")
print("-"*80)

def save_to_database(df_raw, df_scores):
    """
    将原始指标数据和评估结果存储到 military_effectiveness_evaluation 表
    
    参数:
        df_raw: 原始指标数据
        df_scores: 评估得分数据
    """
    conn = get_db_connection()
    cursor = conn.cursor()
    
    # 准备插入数据
    insert_count = 0
    update_count = 0
    
    for idx, row in df_raw.iterrows():
        test_id = row['test_id']
        scenario_id = row['scenario_id']
        
        # 检查是否已存在
        cursor.execute("SELECT evaluation_id FROM military_effectiveness_evaluation WHERE test_id = %s", (test_id,))
        existing = cursor.fetchone()
        
        if existing:
            # 更新现有记录
            update_sql = """
            UPDATE military_effectiveness_evaluation SET
                scenario_id = %s,
                RS_avg_call_setup_duration_ms = %s,
                RS_avg_transmission_delay_ms = %s,
                PO_effective_throughput = %s,
                PO_spectral_efficiency = %s,
                EF_avg_communication_distance = %s,
                EF_avg_ber = %s,
                EF_avg_plr = %s,
                EF_task_success_rate = %s,
                RL_communication_availability_rate = %s,
                RL_communication_success_rate = %s,
                RL_recovery_duration_ms = %s,
                RL_crash_rate = %s,
                AJ_avg_sinr = %s,
                AJ_avg_jamming_margin = %s,
                HO_avg_operator_reaction_time_ms = %s,
                HO_operation_success_rate = %s,
                NC_avg_network_setup_duration_ms = %s,
                NC_avg_connectivity_rate = %s,
                SC_key_compromise_frequency = %s,
                SC_detection_probability = %s,
                SC_interception_resistance = %s,
                total_communications = %s,
                total_lifecycles = %s,
                updated_at = CURRENT_TIMESTAMP
            WHERE test_id = %s
            """
            
            cursor.execute(update_sql, (
                scenario_id,
                row['RS_avg_call_setup_duration_ms'],
                row['RS_avg_transmission_delay_ms'],
                row['PO_effective_throughput'],
                row['PO_spectral_efficiency'],
                row['EF_avg_communication_distance'],
                row['EF_avg_ber'],
                row['EF_avg_plr'],
                row['EF_task_success_rate'],
                row['RL_communication_availability_rate'],
                row['RL_communication_success_rate'],
                row['RL_recovery_duration_ms'],
                row['RL_crash_rate'],
                row['AJ_avg_sinr'],
                row['AJ_avg_jamming_margin'],
                row['HO_avg_operator_reaction_time_ms'],
                row['HO_operation_success_rate'],
                row['NC_avg_network_setup_duration_ms'],
                row['NC_avg_connectivity_rate'],
                row['SC_key_compromise_frequency'],
                row['SC_detection_probability'],
                row['SC_interception_resistance'],
                row['total_communications'],
                row['total_lifecycles'],
                test_id
            ))
            update_count += 1
        else:
            # 插入新记录
            insert_sql = """
            INSERT INTO military_effectiveness_evaluation (
                scenario_id, test_id,
                RS_avg_call_setup_duration_ms, RS_avg_transmission_delay_ms,
                PO_effective_throughput, PO_spectral_efficiency,
                EF_avg_communication_distance, EF_avg_ber, EF_avg_plr, EF_task_success_rate,
                RL_communication_availability_rate, RL_communication_success_rate,
                RL_recovery_duration_ms, RL_crash_rate,
                AJ_avg_sinr, AJ_avg_jamming_margin,
                HO_avg_operator_reaction_time_ms, HO_operation_success_rate,
                NC_avg_network_setup_duration_ms, NC_avg_connectivity_rate,
                SC_key_compromise_frequency, SC_detection_probability, SC_interception_resistance,
                total_communications, total_lifecycles
            ) VALUES (
                %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s
            )
            """
            
            cursor.execute(insert_sql, (
                scenario_id, test_id,
                row['RS_avg_call_setup_duration_ms'],
                row['RS_avg_transmission_delay_ms'],
                row['PO_effective_throughput'],
                row['PO_spectral_efficiency'],
                row['EF_avg_communication_distance'],
                row['EF_avg_ber'],
                row['EF_avg_plr'],
                row['EF_task_success_rate'],
                row['RL_communication_availability_rate'],
                row['RL_communication_success_rate'],
                row['RL_recovery_duration_ms'],
                row['RL_crash_rate'],
                row['AJ_avg_sinr'],
                row['AJ_avg_jamming_margin'],
                row['HO_avg_operator_reaction_time_ms'],
                row['HO_operation_success_rate'],
                row['NC_avg_network_setup_duration_ms'],
                row['NC_avg_connectivity_rate'],
                row['SC_key_compromise_frequency'],
                row['SC_detection_probability'],
                row['SC_interception_resistance'],
                row['total_communications'],
                row['total_lifecycles']
            ))
            insert_count += 1
    
    conn.commit()
    cursor.close()
    conn.close()
    
    return insert_count, update_count

try:
    insert_count, update_count = save_to_database(df_raw, df_scores)
    print(f"✓ 数据存储成功")
    print(f"  - 新增记录: {insert_count} 条")
    print(f"  - 更新记录: {update_count} 条")
    print(f"  - 总计: {insert_count + update_count} 条")
    print()
    print("💡 提示: 现在可以通过以下SQL查询查看异常数据:")
    print()
    print("   -- 查看崩溃比例异常的测试批次")
    print("   SELECT test_id, RL_crash_rate, RL_recovery_duration_ms")
    print("   FROM military_effectiveness_evaluation")
    print("   WHERE RL_crash_rate > 0")
    print("   ORDER BY RL_crash_rate DESC;")
    print()
    print("   -- 查看通信成功率低的测试批次")
    print("   SELECT test_id, RL_communication_success_rate, EF_task_success_rate")
    print("   FROM military_effectiveness_evaluation")
    print("   WHERE RL_communication_success_rate < 0.9")
    print("   ORDER BY RL_communication_success_rate ASC;")
    print()
    print("   -- 查看所有评估数据")
    print("   SELECT * FROM v_effectiveness_evaluation_summary;")
    print()
except Exception as e:
    print(f"✗ 数据存储失败: {str(e)}")
    print("  评估结果已生成，但未能保存到数据库")
    print()

print()
print("="*80)
print("评估完成")
print("="*80)

# ============================================================================
# 第十部分：8维度细粒度指标可视化
# ============================================================================

print("\n" + "="*80)
print("步骤9: 8维度细粒度指标可视化")
print("="*80)
print("绘制8个维度的细粒度指标对比图...")
print("说明: 使用灰色层次评估法计算的维度得分")
print()

from matplotlib.patches import Rectangle

# 创建8个子图（每行一个维度，显示更大）
fig = plt.figure(figsize=(24, 48))

# 使用前面定义的INDICATOR_SYSTEM和计算的criteria_weights
for idx, dim_code in enumerate(dim_codes_ordered, 1):
    dim_info = INDICATOR_SYSTEM[dim_code]
    dim_weight = criteria_weights[idx-1]  # 使用AHP计算的权重
    ax = plt.subplot(8, 1, idx)  # 8行1列，每行一个维度
    
    # 从INDICATOR_SYSTEM获取指标信息
    indicators = [ind['code'] for ind in dim_info['indicators']]
    labels = [ind['name'] for ind in dim_info['indicators']]
    n_indicators = len(indicators)
    n_tests = len(df_normalized)
    
    # 准备数据
    test_ids = df_normalized['test_id'].values
    dim_scores = df_scores[f'{dim_code}_score'].values  # 使用前面计算的维度得分
    
    # 设置柱状图参数
    x = np.arange(n_tests)
    width = 0.8 / n_indicators
    colors = plt.cm.Set3(np.linspace(0, 1, n_indicators))
    
    # 绘制分组柱状图
    for i, (indicator, label) in enumerate(zip(indicators, labels)):
        values = df_normalized[indicator].values
        offset = (i - n_indicators/2 + 0.5) * width
        bars = ax.bar(x + offset, values, width, label=label, 
                     color=colors[i], alpha=0.8, edgecolor='black', linewidth=0.5)
        
        # 在柱子上标注数值（只标注大于5的值）
        for j, (bar, val) in enumerate(zip(bars, values)):
            if val > 5:
                ax.text(bar.get_x() + bar.get_width()/2, val + 2, 
                       f'{val:.0f}', ha='center', va='bottom', fontsize=7)
    
    # 添加维度得分线（灰色评估法计算的得分）
    ax.plot(x, dim_scores, 'r-o', linewidth=3, markersize=8, 
           label=f'灰色评估得分', zorder=10)
    
    # 在得分线上标注分数
    for i, (xi, score) in enumerate(zip(x, dim_scores)):
        ax.text(xi, score + 5, f'{score:.1f}', ha='center', va='bottom',
               fontsize=9, weight='bold', color='red',
               bbox=dict(boxstyle='round,pad=0.3', facecolor='yellow', alpha=0.7))
    
    # 添加参考线
    ax.axhline(y=90, color='green', linestyle='--', linewidth=1.5, alpha=0.5, label='优秀线(90)')
    ax.axhline(y=60, color='orange', linestyle='--', linewidth=1.5, alpha=0.5, label='及格线(60)')
    
    # 设置坐标轴
    ax.set_xlabel('测试批次', fontsize=11, weight='bold')
    ax.set_ylabel('归一化得分 (0-100)', fontsize=11, weight='bold')
    ax.set_title(f'{dim_info["name"]} (AHP权重: {dim_weight*100:.2f}%)', 
                fontsize=13, weight='bold', pad=10)
    ax.set_xticks(x)
    ax.set_xticklabels(test_ids, rotation=45, ha='right', fontsize=9)
    ax.set_ylim(0, 110)
    ax.legend(loc='upper left', fontsize=8, ncol=2)
    ax.grid(axis='y', alpha=0.3)
    
    # 高亮显示得分异常的测试（低于60分）
    for i, score in enumerate(dim_scores):
        if score < 60:
            # 添加红色背景标记
            rect = Rectangle((x[i]-0.4, 0), 0.8, 110, 
                           facecolor='red', alpha=0.1, zorder=0)
            ax.add_patch(rect)
            ax.text(x[i], 105, '⚠', ha='center', fontsize=16, color='red')

plt.suptitle('8维度细粒度指标分析 - 所有测试批次对比\n(柱状图=各指标归一化值, 红线=灰色层次评估法计算的维度得分)', 
            fontsize=16, weight='bold', y=0.995)
plt.tight_layout()

# 保存图片
timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
save_path = f'8维度细粒度指标分析_{timestamp}.png'
plt.savefig(save_path, dpi=300, bbox_inches='tight')
print(f"✓ 图表已保存: {save_path}")

# 生成详细分析报告
print("\n" + "="*80)
print("【8维度细粒度分析报告】")
print("="*80)

for i, dim_code in enumerate(dim_codes_ordered):
    dim_info = INDICATOR_SYSTEM[dim_code]
    dim_weight = criteria_weights[i]  # 使用AHP计算的权重
    
    print(f"\n【{dim_info['name']}】AHP权重: {dim_weight*100:.2f}%")
    print("-"*80)
    
    # 获取该维度的数据
    dim_data = df_scores[['test_id', f'{dim_code}_score']].copy()
    
    # 添加细分指标
    for indicator in dim_info['indicators']:
        dim_data[indicator['code']] = df_normalized[indicator['code']]
    
    dim_data = dim_data.sort_values(f'{dim_code}_score', ascending=False)
    
    print("\n排名:")
    for rank, (idx, row) in enumerate(dim_data.iterrows(), 1):
        score = row[f'{dim_code}_score']
        test_id = row['test_id']
        
        if score >= 90:
            grade = "⭐⭐⭐⭐⭐ 优秀"
        elif score >= 70:
            grade = "⭐⭐⭐⭐ 良好"
        elif score >= 50:
            grade = "⭐⭐⭐ 中等"
        else:
            grade = "⚠ 较差"
        
        print(f"  {rank}. {test_id}: {score:.2f}分 {grade}")
        
        # 显示各指标得分
        indicator_scores = []
        for indicator in dim_info['indicators']:
            code = indicator['code']
            name = indicator['name']
            val = row[code]
            indicator_scores.append(f"{name}={val:.1f}")
        print(f"     细分: {', '.join(indicator_scores)}")
    
    # 分析异常值
    print("\n⚠ 需要关注的问题:")
    has_issues = False
    for idx, row in dim_data.iterrows():
        test_id = row['test_id']
        issues = []
        
        for indicator in dim_info['indicators']:
            code = indicator['code']
            name = indicator['name']
            val = row[code]
            if val < 40:
                issues.append(f"{name}过低({val:.1f})")
        
        if issues:
            print(f"  - {test_id}: {', '.join(issues)}")
            has_issues = True
    
    if not has_issues:
        print("  无明显问题 ✓")

print("\n" + "="*80)
print("可视化分析完成！")
print("="*80)
print("\n说明:")
print("  1. 柱状图显示各指标的归一化值（0-100分）")
print("  2. 红色折线显示灰色层次评估法计算的维度综合得分")
print("  3. 绿色虚线(90分)表示优秀线，橙色虚线(60分)表示及格线")
print("  4. 红色背景标记表示该测试批次在此维度得分不及格(<60分)")
print("  5. 黄色标签显示灰色评估得分，反映了不确定性的处理")
print()
print("💡 灰色评估法特点:")
print("  - 通过白化权函数处理指标的模糊性和不确定性")
print("  - 灰色评估权向量反映了各灰类（优秀/良好/中等/较差）的隶属度")
print("  - 综合得分 = 灰色评估权向量 · 灰类代表值")
print("  - 相比简单加权求和，更能反映评估的灰色特性")
print()

plt.show()

print("\n" + "="*80)
print("全部评估流程完成！")
print("="*80)
print("\n评估方法总结:")
print("  ✓ 指标标准化: Min-Max归一化 (0-100分)")
print("  ✓ 指标权重: 熵权法（客观权重）")
print("  ✓ 维度权重: AHP层次分析法（主观权重）")
print("  ✓ 评分方法: 灰色层次评估法（白化权函数 + 灰色评估系数）")
print("  ✓ 灰类定义: 优[7,9,10]梯形、良[5,7,9]三角、中[3,5,7]三角、差[0,2,4]梯形")
print("  ✓ 灰类重心: [9, 7, 5, 2] - 作为灰类代表值")
print("  ✓ 主要结果: 灰色评估权向量（保留不确定性信息）")
print("  ✓ 辅助结果: 单一得分 = 灰色评估权向量 · [9.0, 7.0, 5.0, 2.0] × 10")
print()