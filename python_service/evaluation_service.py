#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
军事通信效能评估服务 - RESTful API 版本
提供给 Java 后端调用的 Python 计算服务

接口说明：
1. 接收 AHP 优先级参数（JSON）
2. 从数据库读取基础数据
3. 执行完整的评估计算（AHP + 熵权法 + 综合评分）
4. 返回评估结果（JSON）
"""

import sys
import json
import mysql.connector
import pandas as pd
import numpy as np
from datetime import datetime
import warnings
warnings.filterwarnings('ignore')

# ============================================================================
# 数据库配置
# ============================================================================

DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': 'root',
    'database': 'military_communication_effectiveness',
    'charset': 'utf8mb4'
}

# ============================================================================
# 指标体系定义
# ============================================================================

INDICATOR_SYSTEM = {
    'RL': {
        'name': '可靠性',
        'indicators': [
            {'code': 'RL_communication_availability_rate', 'name': '通信可用性', 'direction': 'max'},
            {'code': 'RL_communication_success_rate', 'name': '通信成功率', 'direction': 'max'},
            {'code': 'RL_recovery_duration_ms', 'name': '恢复时长', 'direction': 'min'},
            {'code': 'RL_crash_rate', 'name': '崩溃比例', 'direction': 'min'}
        ]
    },
    'SC': {
        'name': '安全性',
        'indicators': [
            {'code': 'SC_key_compromise_frequency', 'name': '密钥泄露频率', 'direction': 'min'},
            {'code': 'SC_detection_probability', 'name': '被侦察概率', 'direction': 'min'},
            {'code': 'SC_interception_resistance', 'name': '抗拦截能力', 'direction': 'max'}
        ]
    },
    'AJ': {
        'name': '抗干扰性',
        'indicators': [
            {'code': 'AJ_avg_sinr', 'name': '平均信干噪比', 'direction': 'max'},
            {'code': 'AJ_avg_jamming_margin', 'name': '平均抗干扰余量', 'direction': 'max'}
        ]
    },
    'EF': {
        'name': '有效性',
        'indicators': [
            {'code': 'EF_avg_communication_distance', 'name': '平均通信距离', 'direction': 'max'},
            {'code': 'EF_avg_ber', 'name': '平均误码率', 'direction': 'min'},
            {'code': 'EF_avg_plr', 'name': '平均丢包率', 'direction': 'min'},
            {'code': 'EF_task_success_rate', 'name': '任务成功率', 'direction': 'max'}
        ]
    },
    'PO': {
        'name': '处理能力',
        'indicators': [
            {'code': 'PO_effective_throughput', 'name': '有效吞吐量', 'direction': 'max'},
            {'code': 'PO_spectral_efficiency', 'name': '频谱效率', 'direction': 'max'}
        ]
    },
    'NC': {
        'name': '组网能力',
        'indicators': [
            {'code': 'NC_avg_network_setup_duration_ms', 'name': '平均组网时长', 'direction': 'min'},
            {'code': 'NC_avg_connectivity_rate', 'name': '平均连通率', 'direction': 'max'}
        ]
    },
    'HO': {
        'name': '人为操作',
        'indicators': [
            {'code': 'HO_avg_operator_reaction_time_ms', 'name': '平均操作员反应时间', 'direction': 'min'},
            {'code': 'HO_operation_success_rate', 'name': '操作成功率', 'direction': 'max'}
        ]
    },
    'RS': {
        'name': '响应能力',
        'indicators': [
            {'code': 'RS_avg_call_setup_duration_ms', 'name': '平均呼叫建立时长', 'direction': 'min'},
            {'code': 'RS_avg_transmission_delay_ms', 'name': '平均传输时延', 'direction': 'min'}
        ]
    }
}

# 需要对数变换的指标
LOGARITHMIC_INDICATORS = {'EF_avg_ber', 'EF_avg_plr'}

# ============================================================================
# 数据提取
# ============================================================================

def extract_data_from_database():
    """从 military_effectiveness_evaluation 表读取数据"""
    conn = mysql.connector.connect(**DB_CONFIG)
    
    # 获取所有列名
    indicator_cols = []
    for dim_info in INDICATOR_SYSTEM.values():
        for ind in dim_info['indicators']:
            indicator_cols.append(ind['code'])
    
    # 构建查询
    cols_str = ', '.join(['evaluation_id', 'test_id', 'scenario_id'] + indicator_cols + 
                         ['total_communications', 'total_lifecycles'])
    query = f"SELECT {cols_str} FROM military_effectiveness_evaluation"
    
    df = pd.read_sql(query, conn)
    conn.close()
    
    return df

# ============================================================================
# 数据标准化
# ============================================================================

def normalize_indicator(series, direction, indicator_code=None):
    """Min-Max 归一化到 0-100 分"""
    if series.isna().all():
        return pd.Series([50] * len(series), index=series.index)
    
    # 对数变换（针对误码率、丢包率）
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

def normalize_data(df_raw):
    """标准化所有指标"""
    df_normalized = df_raw[['evaluation_id', 'test_id', 'scenario_id']].copy()
    
    for dim_code, dim_info in INDICATOR_SYSTEM.items():
        for indicator in dim_info['indicators']:
            col_name = indicator['code']
            direction = indicator['direction']
            
            if col_name in df_raw.columns:
                df_normalized[col_name] = normalize_indicator(
                    df_raw[col_name], direction, col_name
                )
            else:
                df_normalized[col_name] = 50
    
    return df_normalized

# ============================================================================
# 熵权法计算二级权重
# ============================================================================

def calculate_entropy_weights(df_norm, indicator_system):
    """在每个维度内部使用熵权法计算指标权重"""
    indicator_weights = {}
    
    for dim_code, dim_info in indicator_system.items():
        indicator_cols = [ind['code'] for ind in dim_info['indicators']]
        dim_data = df_norm[indicator_cols].values
        n_samples, n_indicators = dim_data.shape
        
        if n_indicators == 1:
            indicator_weights[indicator_cols[0]] = 1.0
        else:
            data_normalized = dim_data / 100.0
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
                indicator_weights[ind_code] = float(weights[i])
    
    return indicator_weights

# ============================================================================
# AHP 计算一级权重
# ============================================================================

def build_ahp_matrix(priorities):
    """
    根据优先级构建 AHP 判断矩阵
    
    参数:
        priorities: dict, 例如 {'RL': 1, 'SC': 2, 'AJ': 3, ...}
    
    返回:
        numpy 数组形式的判断矩阵和维度代码列表
    """
    # 调试日志
    sys.stderr.write(f"[DEBUG] build_ahp_matrix 接收到的 priorities: {priorities}\n")
    sys.stderr.flush()
    
    # 固定维度顺序（与前端一致）
    dim_codes = ['RL', 'SC', 'AJ', 'EF', 'PO', 'NC', 'HO', 'RS']
    n = len(dim_codes)
    
    # 调试日志
    sys.stderr.write(f"[DEBUG] 维度顺序（固定）: {[(code, priorities[code]) for code in dim_codes]}\n")
    sys.stderr.flush()
    
    # 构建判断矩阵
    matrix = np.zeros((n, n))
    
    for i in range(n):
        for j in range(n):
            if i == j:
                matrix[i][j] = 1
            else:
                # 获取两个维度的优先级
                priority_i = priorities[dim_codes[i]]
                priority_j = priorities[dim_codes[j]]
                
                if priority_i < priority_j:
                    # i 的优先级高于 j（数字小表示优先级高）
                    diff = priority_j - priority_i
                    matrix[i][j] = diff + 1  # 优先级差1用2，差2用3，以此类推
                elif priority_i > priority_j:
                    # j 的优先级高于 i
                    diff = priority_i - priority_j
                    matrix[i][j] = 1.0 / (diff + 1)
                else:
                    # 优先级相同
                    matrix[i][j] = 1
    
    # 调试日志
    sys.stderr.write(f"[DEBUG] 构建的 AHP 矩阵:\n{matrix}\n")
    sys.stderr.flush()
    
    return matrix, dim_codes

def ahp_calculate_weights(judgment_matrix):
    """AHP 计算权重和一致性比率"""
    n = len(judgment_matrix)
    
    eigenvalues, eigenvectors = np.linalg.eig(judgment_matrix)
    max_eigenvalue_index = np.argmax(eigenvalues.real)
    max_eigenvalue = eigenvalues[max_eigenvalue_index].real
    max_eigenvector = eigenvectors[:, max_eigenvalue_index].real
    
    weights = max_eigenvector / max_eigenvector.sum()
    
    # 调试日志
    sys.stderr.write(f"[DEBUG] AHP 计算的权重向量: {weights}\n")
    sys.stderr.flush()
    
    CI = (max_eigenvalue - n) / (n - 1)
    RI_dict = {1: 0, 2: 0, 3: 0.58, 4: 0.90, 5: 1.12, 6: 1.24, 7: 1.32, 
               8: 1.41, 9: 1.45, 10: 1.49}
    RI = RI_dict.get(n, 1.41)
    CR = CI / RI if RI != 0 else 0
    
    sys.stderr.write(f"[DEBUG] 一致性比率 CR: {CR}\n")
    sys.stderr.flush()
    
    return weights, CR

# ============================================================================
# 综合评分计算
# ============================================================================

def calculate_scores(df_normalized, dim_codes_ordered, criteria_weights, indicator_entropy_weights):
    """计算维度得分和综合得分"""
    df_scores = df_normalized.copy()
    
    # 计算最终权重
    final_weights = {}
    for i, dim_code in enumerate(dim_codes_ordered):
        dim_weight = criteria_weights[i]
        dim_info = INDICATOR_SYSTEM[dim_code]
        
        for indicator in dim_info['indicators']:
            code = indicator['code']
            entropy_weight = indicator_entropy_weights[code]
            final_weights[code] = dim_weight * entropy_weight
    
    # 计算维度得分
    for dim_code in dim_codes_ordered:
        dim_info = INDICATOR_SYSTEM[dim_code]
        dim_score = 0
        
        for indicator in dim_info['indicators']:
            code = indicator['code']
            weight = final_weights[code]
            dim_score += df_normalized[code] * weight
        
        dim_total_weight = sum(final_weights[ind['code']] for ind in dim_info['indicators'])
        df_scores[f'{dim_code}_score'] = dim_score / dim_total_weight
    
    # 计算综合得分
    df_scores['total_score'] = 0
    for i, dim_code in enumerate(dim_codes_ordered):
        dim_weight = criteria_weights[i]
        df_scores['total_score'] += df_scores[f'{dim_code}_score'] * dim_weight
    
    # 评估等级
    def get_grade(score):
        if score >= 90:
            return '优秀'
        elif score >= 80:
            return '良好'
        elif score >= 70:
            return '中等'
        elif score >= 60:
            return '及格'
        else:
            return '较差'
    
    df_scores['grade'] = df_scores['total_score'].apply(get_grade)
    df_scores['rank'] = df_scores['total_score'].rank(ascending=False, method='min').astype(int)
    
    return df_scores, final_weights

# ============================================================================
# 主函数
# ============================================================================

def evaluate(priorities):
    """
    执行完整的评估流程
    
    参数:
        priorities: dict, 例如 {'RL': 1, 'SC': 2, 'AJ': 3, 'EF': 4, 'PO': 5, 'NC': 6, 'HO': 7, 'RS': 8}
    
    返回:
        dict, 包含评估结果的 JSON 对象
    """
    try:
        # 1. 读取数据
        df_raw = extract_data_from_database()
        
        if len(df_raw) == 0:
            return {
                'success': False,
                'message': '数据库中没有评估数据'
            }
        
        # 2. 数据标准化
        df_normalized = normalize_data(df_raw)
        
        # 3. 计算熵权（二级权重）
        indicator_entropy_weights = calculate_entropy_weights(df_normalized, INDICATOR_SYSTEM)
        
        # 4. 构建 AHP 判断矩阵并计算权重（一级权重）
        ahp_matrix, dim_codes_ordered = build_ahp_matrix(priorities)
        criteria_weights, CR = ahp_calculate_weights(ahp_matrix)
        
        # 5. 计算综合得分
        df_scores, final_weights = calculate_scores(
            df_normalized, dim_codes_ordered, criteria_weights, indicator_entropy_weights
        )
        
        # 6. 构建返回结果
        result = {
            'success': True,
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
            'consistencyRatio': float(CR),
            'consistencyPassed': bool(CR < 0.1),
            
            # AHP 判断矩阵（添加）
            'ahpMatrix': ahp_matrix.tolist(),
            
            # 维度代码顺序（添加）
            'dimensionCodes': dim_codes_ordered,
            
            # 一级权重（维度权重）
            'dimensionWeights': {
                dim_code: {
                    'name': INDICATOR_SYSTEM[dim_code]['name'],
                    'weight': float(criteria_weights[i]),
                    'priority': priorities[dim_code]
                }
                for i, dim_code in enumerate(dim_codes_ordered)
            },
            
            # 权重字典（便于前端使用）
            'weights': {
                dim_code: float(criteria_weights[i])
                for i, dim_code in enumerate(dim_codes_ordered)
            },
            
            # 二级权重（指标权重）
            'indicatorWeights': {
                dim_code: {
                    'name': INDICATOR_SYSTEM[dim_code]['name'],
                    'indicators': [
                        {
                            'code': ind['code'],
                            'name': ind['name'],
                            'entropyWeight': float(indicator_entropy_weights[ind['code']]),
                            'finalWeight': float(final_weights[ind['code']])
                        }
                        for ind in INDICATOR_SYSTEM[dim_code]['indicators']
                    ]
                }
                for dim_code in dim_codes_ordered
            },
            
            # 评估结果
            'evaluationResults': []
        }
        
        # 添加每个测试批次的评估结果
        df_scores_sorted = df_scores.sort_values('rank')
        for idx, row in df_scores_sorted.iterrows():
            # 计算每个维度的详细计算过程
            dimension_calculations = {}
            for i, dim_code in enumerate(dim_codes_ordered):
                dim_info = INDICATOR_SYSTEM[dim_code]
                dim_weight = criteria_weights[i]
                
                # 计算该维度下每个指标的贡献
                indicator_contributions = []
                dim_score_sum = 0
                
                for indicator in dim_info['indicators']:
                    code = indicator['code']
                    normalized_value = float(df_normalized.loc[idx, code])
                    entropy_weight = indicator_entropy_weights[code]
                    final_weight = final_weights[code]
                    contribution = normalized_value * entropy_weight
                    
                    indicator_contributions.append({
                        'code': code,
                        'name': indicator['name'],
                        'rawValue': float(df_raw.loc[idx, code]),
                        'normalizedValue': normalized_value,
                        'entropyWeight': float(entropy_weight),
                        'finalWeight': float(final_weight),
                        'contribution': float(contribution)
                    })
                    
                    dim_score_sum += contribution
                
                # 维度得分 = 各指标贡献之和 / 该维度指标权重之和
                dim_total_weight = sum(indicator_entropy_weights[ind['code']] for ind in dim_info['indicators'])
                dim_score = dim_score_sum / dim_total_weight
                
                dimension_calculations[dim_code] = {
                    'dimensionName': dim_info['name'],
                    'ahpWeight': float(dim_weight),
                    'dimensionScore': float(dim_score),
                    'indicators': indicator_contributions,
                    'calculation': f"维度得分 = Σ(归一化值 × 熵权) / Σ熵权 = {dim_score:.2f}"
                }
            
            # 计算综合得分的详细过程
            total_score_calculation = []
            for i, dim_code in enumerate(dim_codes_ordered):
                dim_weight = criteria_weights[i]
                dim_score = float(row[f'{dim_code}_score'])
                contribution = dim_weight * dim_score
                
                total_score_calculation.append({
                    'dimensionCode': dim_code,
                    'dimensionName': INDICATOR_SYSTEM[dim_code]['name'],
                    'ahpWeight': float(dim_weight),
                    'dimensionScore': dim_score,
                    'contribution': float(contribution)
                })
            
            test_result = {
                'evaluationId': int(row['evaluation_id']),
                'testId': row['test_id'],
                'scenarioId': int(row['scenario_id']),
                'totalScore': float(row['total_score']),
                'grade': row['grade'],
                'rank': int(row['rank']),
                'dimensionScores': {
                    dim_code: float(row[f'{dim_code}_score'])
                    for dim_code in dim_codes_ordered
                },
                'indicatorScores': {},
                'indicatorRawValues': {},
                'dimensionCalculations': dimension_calculations,  # 添加维度计算过程
                'totalScoreCalculation': total_score_calculation  # 添加综合得分计算过程
            }
            
            # 添加指标得分（归一化后）
            for dim_code in dim_codes_ordered:
                test_result['indicatorScores'][dim_code] = {
                    ind['code']: float(df_normalized.loc[idx, ind['code']])
                    for ind in INDICATOR_SYSTEM[dim_code]['indicators']
                }
            
            # 添加指标原始值（归一化前）
            for dim_code in dim_codes_ordered:
                test_result['indicatorRawValues'][dim_code] = {
                    ind['code']: float(df_raw.loc[idx, ind['code']])
                    for ind in INDICATOR_SYSTEM[dim_code]['indicators']
                }
            
            result['evaluationResults'].append(test_result)
        
        return result
        
    except Exception as e:
        return {
            'success': False,
            'message': f'评估计算失败: {str(e)}',
            'error': str(e)
        }

# ============================================================================
# 命令行接口
# ============================================================================

if __name__ == '__main__':
    try:
        # 从标准输入读取 JSON
        input_json = sys.stdin.read().strip()
        
        # 调试日志：记录接收到的输入
        sys.stderr.write(f"[DEBUG] 接收到的输入: {input_json}\n")
        sys.stderr.flush()
        
        if input_json:
            # 解析 JSON 输入
            input_data = json.loads(input_json)
            priorities = input_data.get('priorities', {})
            
            # 调试日志：记录解析后的优先级
            sys.stderr.write(f"[DEBUG] 解析后的优先级: {priorities}\n")
            sys.stderr.flush()
        else:
            # 默认优先级（用于测试）
            priorities = {
                'RL': 1,  # 可靠性
                'SC': 2,  # 安全性
                'AJ': 3,  # 抗干扰性
                'EF': 4,  # 有效性
                'PO': 5,  # 处理能力
                'NC': 6,  # 组网能力
                'HO': 7,  # 人为操作
                'RS': 8   # 响应能力
            }
            
            # 调试日志：使用默认优先级
            sys.stderr.write(f"[DEBUG] 使用默认优先级: {priorities}\n")
            sys.stderr.flush()
        
        # 执行评估
        result = evaluate(priorities)
        
        # 调试日志：记录计算结果中的权重
        if result.get('success'):
            sys.stderr.write(f"[DEBUG] 计算成功，维度权重: {result.get('dimensionWeights', {})}\n")
            sys.stderr.flush()
        
        # 输出 JSON 结果
        print(json.dumps(result, ensure_ascii=False, indent=2))
        
    except Exception as e:
        # 输出错误信息
        error_result = {
            'success': False,
            'message': f'脚本执行失败: {str(e)}',
            'error': str(e)
        }
        sys.stderr.write(f"[ERROR] 脚本执行失败: {str(e)}\n")
        sys.stderr.flush()
        print(json.dumps(error_result, ensure_ascii=False, indent=2))
        sys.exit(1)
