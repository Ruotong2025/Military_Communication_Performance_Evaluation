#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
军事通信效能评估 - 基于组合赋权法
主观权重(AHP) + 客观权重(熵权法) → 组合权重 → 加权求和

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
print("军事通信效能评估系统 - 组合赋权法")
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
# 第三部分：军事级别标准分段配置
# ============================================================================

print("步骤3: 定义军事级别标准分段")
print("-"*80)
print("使用归一化方法: 基于军事通信标准的分段评分")
print("说明: 参考GJB军标、3GPP、ITU-T等标准，结合军事通信严格要求")
print("      每个指标都有明确的性能分级标准")
print()

# 军事无线通信标准配置（超短波、短波、卫星通信）
# 参考标准：
# 【国际标准】
# - ITU-R P.533：短波（HF）传播预测方法
# - ITU-R P.1546：陆地移动业务（VHF/UHF）传播预测
# - ITU-R M.1079：移动卫星业务性能目标
# - ITU-R M.1225：IMT-2000无线电接口规范
# - ITU-R M.1645：频谱效率评估框架
# - ITU-T G.114：单向传输时延建议
# - ITU-T G.826：数字传输系统误码性能
# - ITU-T Y.1541：IP网络性能参数
# 【北约标准（公开）】
# - STANAG 4285：短波（HF）数据传输标准（1.2-9.6kbps）
# - STANAG 4415：短波（HF）自动链路建立（ALE）
# - STANAG 4538：短波（HF）自动链路建立（ALE）增强版
# - STANAG 4406：军事消息处理系统
# 【行业标准】
# - TIA-102 (P25)：专业数字无线电标准（美国公共安全）
# - ETSI EN 300 392 (TETRA)：陆地集群无线电标准
# - IEEE 802.11：无线局域网（扩频技术参考）
# - IEEE 802.15.4：低速无线个域网（自组网参考）
# 【技术参考】
# - 超短波电台：VHF/UHF 30-512MHz，视距5-50km，空地100-400km，数据速率64kbps-2Mbps
# - 短波电台：HF 1.5-30MHz，天波1000-4000km，数据速率1.2-9.6kbps
# - 卫星通信：GEO时延250-300ms，LEO时延20-50ms，数据速率1-10Mbps

MILITARY_STANDARDS = {
    # ========== C1. 响应能力 (RS) ==========
    'RS_avg_call_setup_duration_ms': {
        'name': '平均呼叫建立时长',
        'unit': 'ms',
        'direction': 'min',
        'type': 'threshold',
        'segments': [
            # (阈值, 基准分, 等级说明)
            # 参考：超短波快速呼叫0.5-2s，短波ALE 3-10s，卫星1-3s
            (500, 100, '优秀 - 超短波快速呼叫'),     # <500ms: 超短波快速
            (1500, 90, '良好 - 超短波标准'),         # 0.5-1.5s: 超短波标准
            (3000, 80, '中上 - 卫星/短波良好'),      # 1.5-3s: 卫星标准
            (5000, 70, '中等 - 短波ALE可接受'),      # 3-5s: 短波可接受
            (8000, 60, '及格 - 短波ALE勉强'),        # 5-8s: 短波勉强
            (10000, 40, '较差 - 影响作战'),          # 8-10s: 较差
            (float('inf'), 20, '不合格 - 严重延迟')  # >10s: 不合格
        ],
        'reference': 'STANAG 4538 (HF ALE), TIA-102 (P25), 超短波电台技术指标'
    },
    
    'RS_avg_transmission_delay_ms': {
        'name': '平均传输时延',
        'unit': 'ms',
        'direction': 'min',
        'type': 'threshold',
        'segments': [
            # 参考：超短波10-50ms，短波100-200ms，GEO卫星250-300ms，LEO卫星20-50ms
            (20, 100, '优秀 - 超短波/LEO卫星'),      # <20ms: 超短波直通
            (50, 90, '良好 - 超短波标准'),           # 20-50ms: 超短波/LEO
            (100, 80, '中上 - 短波良好'),            # 50-100ms: 短波单跳良好
            (200, 70, '中等 - 短波标准'),            # 100-200ms: 短波标准
            (280, 60, '及格 - GEO卫星标准'),         # 200-280ms: GEO卫星
            (400, 40, '较差 - 影响实时性'),          # 280-400ms: 较差
            (float('inf'), 20, '不合格 - 无法实时')  # >400ms: 不合格
        ],
        'reference': 'ITU-T G.114 (语音时延), ITU-R M.1079 (卫星), 短波通信标准'
    },
    
    # ========== C2. 处理能力 (PO) ==========
    'PO_effective_throughput': {
        'name': '有效吞吐量',
        'unit': 'Mbps',
        'direction': 'max',
        'type': 'threshold',
        'segments': [
            # 参考：短波1.2-9.6kbps，超短波窄带64-256kbps，超短波宽带1-2Mbps，卫星1-10Mbps
            (5, 100, '优秀 - 宽带卫星/超短波'),      # >5Mbps: 宽带卫星
            (2, 90, '良好 - 超短波宽带'),            # 2-5Mbps: 超短波宽带
            (1, 80, '中上 - 超短波宽带标准'),        # 1-2Mbps: 超短波宽带标准
            (0.256, 70, '中等 - 超短波窄带'),        # 256kbps-1Mbps: 超短波窄带高端
            (0.064, 60, '及格 - 超短波窄带标准'),    # 64-256kbps: 超短波窄带
            (0.0096, 40, '较差 - 短波高速'),         # 9.6-64kbps: 短波高速
            (0, 20, '不合格 - 短波低速')             # <9.6kbps: 短波低速
        ],
        'reference': 'STANAG 4285/4415 (HF 1.2-9.6kbps), TETRA (25-100kbps), 超短波电台技术指标'
    },
    
    'PO_spectral_efficiency': {
        'name': '频谱效率',
        'unit': 'bps/Hz',
        'direction': 'max',
        'type': 'threshold',
        'segments': [
            # 参考：短波SSB约1bps/Hz，超短波FM 0.5-1bps/Hz，数字调制QAM 2-4bps/Hz
            (4, 100, '优秀 - 高阶调制QAM'),          # >4 bps/Hz: 高阶QAM
            (3, 90, '良好 - 16QAM/64QAM'),          # 3-4 bps/Hz: 16/64QAM
            (2, 80, '中上 - QPSK/8PSK'),            # 2-3 bps/Hz: QPSK/8PSK
            (1, 70, '中等 - 短波SSB标准'),           # 1-2 bps/Hz: SSB标准
            (0.5, 60, '及格 - 超短波FM'),            # 0.5-1 bps/Hz: FM
            (0.3, 40, '较差 - 低效调制'),            # 0.3-0.5 bps/Hz: 低效
            (0, 20, '不合格 - 极低效')               # <0.3 bps/Hz: 极低效
        ],
        'reference': 'ITU-R M.1645 (频谱效率), STANAG 4285 (HF调制), TETRA标准'
    },
    
    # ========== C3. 有效性 (EF) ==========
    'EF_avg_communication_distance': {
        'name': '平均通信距离',
        'unit': 'km',
        'direction': 'max',
        'type': 'threshold',
        'segments': [
            # 参考：超短波地面5-50km，空地100-400km；短波1000-4000km；卫星全球
            (1000, 100, '优秀 - 短波/卫星远程'),    # >1000km: 短波/卫星
            (100, 90, '良好 - 超短波空地'),         # 100-1000km: 超短波空地
            (50, 80, '中上 - 超短波地面远程'),      # 50-100km: 超短波地面远程
            (30, 70, '中等 - 超短波地面标准'),      # 30-50km: 超短波标准
            (15, 60, '及格 - 超短波地面近程'),      # 15-30km: 超短波近程
            (5, 40, '较差 - 短距离通信'),           # 5-15km: 短距离
            (0, 20, '不合格 - 覆盖不足')            # <5km: 覆盖不足
        ],
        'reference': 'ITU-R P.1546 (VHF/UHF传播), ITU-R P.533 (HF传播), 军用电台技术指标'
    },
    
    'EF_avg_ber': {
        'name': '平均误码率',
        'unit': '',
        'direction': 'min',
        'type': 'exponential',
        'segments': [
            # 参考：超短波数据BER≤10^-5，短波BER 10^-3~10^-4，卫星BER可达10^-6
            (1e-7, 100, '优秀 - 卫星通信级别'),     # <10^-7: 卫星优秀
            (1e-6, 95, '卓越 - 卫星标准'),          # 10^-7 ~ 10^-6: 卫星标准
            (1e-5, 90, '良好 - 超短波数据标准'),    # 10^-6 ~ 10^-5: 超短波数据
            (1e-4, 75, '中上 - 短波良好'),          # 10^-5 ~ 10^-4: 短波良好
            (1e-3, 60, '中等 - 短波标准/超短波语音'), # 10^-4 ~ 10^-3: 短波标准
            (5e-3, 40, '及格 - 短波恶劣条件'),      # 10^-3 ~ 5×10^-3: 短波恶劣
            (1e-2, 25, '较差 - 严重误码'),          # 5×10^-3 ~ 10^-2: 严重误码
            (float('inf'), 10, '不合格 - 无法通信') # >10^-2: 无法通信
        ],
        'reference': 'ITU-T G.826, STANAG 4285 (HF BER), 超短波/卫星通信标准'
    },
    
    'EF_avg_plr': {
        'name': '平均丢包率',
        'unit': '',
        'direction': 'min',
        'type': 'exponential',
        'segments': [
            # 参考：战术数据链≤1%，短波恶劣条件3%-5%
            (1e-5, 100, '优秀 - 无损传输'),         # <10^-5: 无损
            (1e-4, 95, '卓越 - 超低丢包'),          # 10^-5 ~ 10^-4: 超低
            (1e-3, 90, '良好 - 低丢包'),            # 10^-4 ~ 10^-3: 低丢包
            (1e-2, 80, '中上 - 战术数据链标准'),    # 10^-3 ~ 10^-2 (1%): 战术标准
            (3e-2, 65, '中等 - 可接受'),            # 1% ~ 3%: 可接受
            (5e-2, 50, '及格 - 短波恶劣条件'),      # 3% ~ 5%: 短波恶劣
            (1e-1, 30, '较差 - 严重丢包'),          # 5% ~ 10%: 严重丢包
            (float('inf'), 10, '不合格 - 无法通信') # >10%: 无法通信
        ],
        'reference': 'ITU-T Y.1541, STANAG 4406, 战术数据链标准'
    },
    
    'EF_task_success_rate': {
        'name': '任务成功率',
        'unit': '%',
        'direction': 'max',
        'type': 'probability',
        'formula': lambda x: x * 100,  # 直接转百分制
        'reference': '军事任务标准'
    },
    
    # ========== C4. 可靠性 (RL) ==========
    'RL_communication_availability_rate': {
        'name': '通信可用性',
        'unit': '%',
        'direction': 'max',
        'type': 'probability_strict',
        'segments': [
            (0.9999, 100, '优秀 - 4个9'),           # >99.99%
            (0.999, 95, '良好 - 3个9'),             # 99.9-99.99%
            (0.99, 85, '中上 - 2个9'),              # 99-99.9%
            (0.98, 75, '中等 - 98%以上'),           # 98-99%
            (0.95, 65, '及格 - 95%以上'),           # 95-98%
            (0.90, 45, '较差 - 90%以上'),           # 90-95%
            (0, 20, '不合格 - 低于90%')             # <90%
        ],
        'reference': 'GJB 367A, ITU-T E.800'
    },
    
    'RL_communication_success_rate': {
        'name': '通信成功率',
        'unit': '%',
        'direction': 'max',
        'type': 'probability_strict',
        'segments': [
            (0.99, 100, '优秀 - 极高成功率'),       # >99%
            (0.98, 95, '良好 - 高成功率'),          # 98-99%
            (0.95, 85, '中上 - 较高成功率'),        # 95-98%
            (0.90, 75, '中等 - 可接受'),            # 90-95%
            (0.85, 60, '及格 - 勉强可用'),          # 85-90%
            (0.80, 40, '较差 - 频繁失败'),          # 80-85%
            (0, 15, '不合格 - 严重不可靠')          # <80%
        ],
        'reference': '军事通信可靠性标准'
    },
    
    'RL_recovery_duration_ms': {
        'name': '恢复时长',
        'unit': 'ms',
        'direction': 'min',
        'type': 'threshold',
        'segments': [
            (100, 100, '优秀 - 快速恢复'),          # <100ms
            (500, 90, '良好 - 较快恢复'),           # 100-500ms
            (1000, 80, '中上 - 秒级恢复'),          # 0.5-1s
            (3000, 70, '中等 - 可接受'),            # 1-3s
            (5000, 55, '及格 - 较慢'),              # 3-5s
            (10000, 35, '较差 - 严重延迟'),         # 5-10s
            (float('inf'), 10, '不合格 - 极慢')     # >10s
        ],
        'reference': 'GJB 7396, 电信级标准'
    },
    
    'RL_crash_rate': {
        'name': '崩溃比例',
        'unit': '%',
        'direction': 'min',
        'type': 'probability_inverse',
        'segments': [
            (0.001, 100, '优秀 - 极少崩溃'),        # <0.1%
            (0.005, 90, '良好 - 很少崩溃'),         # 0.1-0.5%
            (0.01, 80, '中上 - 偶尔崩溃'),          # 0.5-1%
            (0.02, 70, '中等 - 少量崩溃'),          # 1-2%
            (0.05, 50, '及格 - 可接受'),            # 2-5%
            (0.10, 30, '较差 - 频繁崩溃'),          # 5-10%
            (float('inf'), 5, '不合格 - 严重不稳定') # >10%
        ],
        'reference': '高可靠性系统标准'
    },
    
    # ========== C5. 抗干扰性 (AJ) ==========
    'AJ_avg_sinr': {
        'name': '平均信干噪比',
        'unit': 'dB',
        'direction': 'max',
        'type': 'threshold',
        'segments': [
            # 参考：超短波FM门限12dB，短波SSB门限10dB，扩频可在负SNR下工作
            (20, 100, '优秀 - 极强信号'),           # >20dB: 极强
            (15, 90, '良好 - 强信号'),              # 15-20dB: 强信号
            (12, 80, '中上 - 超短波FM门限'),        # 12-15dB: 超短波FM门限
            (10, 70, '中等 - 短波SSB门限'),         # 10-12dB: 短波SSB门限
            (5, 60, '及格 - 弱信号可用'),           # 5-10dB: 弱信号
            (0, 45, '较差 - 扩频勉强可用'),         # 0-5dB: 扩频勉强
            (-5, 30, '很差 - 扩频极限'),            # -5-0dB: 扩频极限
            (float('-inf'), 10, '不合格 - 无法通信') # <-5dB: 无法通信
        ],
        'reference': 'ITU-R M.1225, IEEE 802.11 (扩频), 超短波/短波电台技术指标'
    },
    
    'AJ_avg_jamming_margin': {
        'name': '平均抗干扰余量',
        'unit': 'dB',
        'direction': 'max',
        'type': 'threshold',
        'segments': [
            # 参考：扩频处理增益20-40dB，跳频10-20dB
            (40, 100, '优秀 - 直扩高增益'),         # >40dB: 直扩高增益
            (30, 95, '卓越 - 直扩标准'),            # 30-40dB: 直扩标准
            (20, 85, '良好 - 直扩/跳频'),           # 20-30dB: 直扩低端/跳频高端
            (15, 75, '中上 - 跳频标准'),            # 15-20dB: 跳频标准
            (10, 65, '中等 - 跳频低端'),            # 10-15dB: 跳频低端
            (5, 50, '及格 - 弱抗干扰'),             # 5-10dB: 弱抗干扰
            (0, 30, '较差 - 很弱抗干扰'),           # 0-5dB: 很弱
            (float('-inf'), 10, '不合格 - 无抗干扰能力') # <0dB: 无抗干扰
        ],
        'reference': 'IEEE 802.11 (扩频), 跳频/扩频通信系统标准'
    },
    
    # ========== C6. 人为操作 (HO) ==========
    'HO_avg_operator_reaction_time_ms': {
        'name': '平均操作员反应时间',
        'unit': 'ms',
        'direction': 'min',
        'type': 'threshold',
        'segments': [
            (200, 100, '优秀 - 极快反应'),          # <200ms
            (500, 90, '良好 - 快速反应'),           # 200-500ms
            (1000, 80, '中上 - 较快反应'),          # 0.5-1s
            (2000, 70, '中等 - 正常反应'),          # 1-2s
            (3000, 60, '及格 - 可接受'),            # 2-3s
            (5000, 40, '较差 - 反应慢'),            # 3-5s
            (float('inf'), 15, '不合格 - 严重迟缓') # >5s
        ],
        'reference': '人机工程学标准, MIL-STD-1472'
    },
    
    'HO_operation_success_rate': {
        'name': '操作成功率',
        'unit': '%',
        'direction': 'max',
        'type': 'probability',
        'formula': lambda x: x * 100,
        'reference': '人机交互标准'
    },
    
    # ========== C7. 组网能力 (NC) ==========
    'NC_avg_network_setup_duration_ms': {
        'name': '平均组网时长',
        'unit': 'ms',
        'direction': 'min',
        'type': 'threshold',
        'segments': [
            # 参考：超短波MANET 10-30s，短波网络30-60s
            (5000, 100, '优秀 - 快速组网'),         # <5s: 快速
            (10000, 90, '良好 - 超短波MANET良好'),  # 5-10s: 良好
            (20000, 80, '中上 - 超短波MANET标准'),  # 10-20s: 超短波标准
            (30000, 70, '中等 - 超短波MANET可接受'), # 20-30s: 可接受
            (45000, 60, '及格 - 短波网络良好'),     # 30-45s: 短波良好
            (60000, 45, '较差 - 短波网络勉强'),     # 45-60s: 短波勉强
            (float('inf'), 25, '不合格 - 组网过慢') # >60s: 过慢
        ],
        'reference': 'IEEE 802.15.4 (自组网), Ad-hoc网络标准, 短波组网标准'
    },
    
    'NC_avg_connectivity_rate': {
        'name': '平均连通率',
        'unit': '%',
        'direction': 'max',
        'type': 'probability',
        'formula': lambda x: x * 100,
        'reference': '网络拓扑标准'
    },
    
    # ========== C8. 安全性 (SC) ==========
    'SC_key_compromise_frequency': {
        'name': '密钥泄露频率',
        'unit': '次/小时',
        'direction': 'min',
        'type': 'threshold',
        'segments': [
            (0.001, 100, '优秀 - 极少泄露'),        # <0.001次/小时
            (0.01, 90, '良好 - 很少泄露'),          # 0.001-0.01次/小时
            (0.05, 75, '中上 - 偶尔泄露'),          # 0.01-0.05次/小时
            (0.1, 60, '中等 - 少量泄露'),           # 0.05-0.1次/小时
            (0.5, 40, '较差 - 频繁泄露'),           # 0.1-0.5次/小时
            (1.0, 20, '严重 - 大量泄露'),           # 0.5-1次/小时
            (float('inf'), 0, '不合格 - 极度不安全') # >1次/小时
        ],
        'reference': '军事密码标准, FIPS 140-2'
    },
    
    'SC_detection_probability': {
        'name': '被侦察概率',
        'unit': '%',
        'direction': 'min',
        'type': 'probability_inverse',
        'segments': [
            (0.01, 100, '优秀 - 极难侦察'),         # <1%
            (0.05, 90, '良好 - 很难侦察'),          # 1-5%
            (0.10, 80, '中上 - 较难侦察'),          # 5-10%
            (0.20, 70, '中等 - 中等隐蔽'),          # 10-20%
            (0.30, 55, '及格 - 一般隐蔽'),          # 20-30%
            (0.50, 35, '较差 - 易被侦察'),          # 30-50%
            (float('inf'), 10, '不合格 - 极易暴露') # >50%
        ],
        'reference': '军事隐蔽通信标准, LPI标准'
    },
    
    'SC_interception_resistance': {
        'name': '抗拦截能力',
        'unit': '%',
        'direction': 'max',
        'type': 'probability',
        'formula': lambda x: x * 100,
        'reference': '军事抗截获标准, LPD标准'
    }
}

print("✓ 已定义21个指标的军事级别标准分段")
print(f"✓ 参考标准: GJB军标、3GPP、ITU-T、NATO STANAG、MIL-STD等")
print()

def normalize_indicator(series, direction, indicator_code=None):
    """
    基于军事标准的分段评分归一化方法
    
    处理流程：
    1. 查找指标的军事标准配置
    2. 根据标准类型进行分段评分
    3. 返回0-100分的标准化得分
    
    参数:
        series: 指标数据序列
        direction: 'max'表示越大越好，'min'表示越小越好
        indicator_code: 指标代码
    
    返回:
        标准化后的序列（0-100分）
    
    优点:
        - 有明确的军事标准参考
        - 可解释性强
        - 跨批次可比
        - 符合工程实践
    """
    # 处理空值
    if series.isna().all():
        return pd.Series([50] * len(series), index=series.index)
    
    # 检查是否有标准配置
    if indicator_code not in MILITARY_STANDARDS:
        # 如果没有标准，使用Min-Max归一化
        return normalize_by_minmax(series, direction)
    
    config = MILITARY_STANDARDS[indicator_code]
    norm_type = config['type']
    
    # 类型1：概率类（直接转百分制）
    if norm_type == 'probability':
        return config['formula'](series)
    
    # 类型2：阈值分段（时延、距离、吞吐量等）
    elif norm_type == 'threshold':
        return normalize_by_threshold(series, config)
    
    # 类型3：指数分段（误码率、丢包率）
    elif norm_type == 'exponential':
        return normalize_by_exponential(series, config)
    
    # 类型4：严格概率分段（可用性、成功率）
    elif norm_type == 'probability_strict':
        return normalize_by_probability_strict(series, config)
    
    # 类型5：逆向概率分段（崩溃率、被侦察概率）
    elif norm_type == 'probability_inverse':
        return normalize_by_probability_inverse(series, config)
    
    else:
        # 默认使用Min-Max
        return normalize_by_minmax(series, direction)


def normalize_by_minmax(series, direction):
    """Min-Max归一化（兜底方案）"""
    min_val = series.min()
    max_val = series.max()
    
    if max_val == min_val:
        return pd.Series([50] * len(series), index=series.index)
    
    if direction == 'max':
        score = (series - min_val) / (max_val - min_val) * 100
    else:
        score = (max_val - series) / (max_val - min_val) * 100
    
    return score


def normalize_by_threshold(series, config):
    """阈值分段评分"""
    segments = config['segments']
    direction = config['direction']
    scores = []
    
    for value in series:
        score = 0
        
        if direction == 'min':
            # 越小越好（时延类）
            for i, (threshold, base_score, label) in enumerate(segments):
                if value < threshold:
                    if i == 0:
                        score = base_score
                    else:
                        # 在两个阈值之间线性插值
                        prev_threshold, prev_score, _ = segments[i-1]
                        ratio = (value - prev_threshold) / (threshold - prev_threshold)
                        score = prev_score - ratio * (prev_score - base_score)
                    break
        else:
            # 越大越好（吞吐量、距离、SINR等）
            for i, (threshold, base_score, label) in enumerate(segments):
                if value >= threshold:
                    score = base_score
                    break
            else:
                # 如果小于所有阈值，使用最低分
                score = segments[-1][1]
        
        scores.append(max(0, min(100, score)))
    
    return pd.Series(scores, index=series.index)


def normalize_by_exponential(series, config):
    """指数分段评分（误码率、丢包率）"""
    segments = config['segments']
    scores = []
    
    for value in series:
        score = 0
        
        for i, (threshold, base_score, label) in enumerate(segments):
            if value < threshold:
                if i == 0:
                    score = base_score
                else:
                    # 在两个阈值之间对数插值
                    prev_threshold, prev_score, _ = segments[i-1]
                    log_value = np.log10(value + 1e-12)
                    log_prev = np.log10(prev_threshold + 1e-12)
                    log_curr = np.log10(threshold + 1e-12)
                    ratio = (log_value - log_prev) / (log_curr - log_prev)
                    score = prev_score - ratio * (prev_score - base_score)
                break
        
        scores.append(max(0, min(100, score)))
    
    return pd.Series(scores, index=series.index)


def normalize_by_probability_strict(series, config):
    """严格概率分段（可用性、成功率）"""
    segments = config['segments']
    scores = []
    
    for value in series:
        score = 0
        
        for i, (threshold, base_score, label) in enumerate(segments):
            if value >= threshold:
                if i == 0:
                    score = base_score
                else:
                    # 在两个阈值之间线性插值
                    prev_threshold, prev_score, _ = segments[i-1]
                    ratio = (value - prev_threshold) / (threshold - prev_threshold)
                    score = prev_score + ratio * (base_score - prev_score)
                break
        else:
            # 如果小于所有阈值，使用最低分
            score = segments[-1][1]
        
        scores.append(max(0, min(100, score)))
    
    return pd.Series(scores, index=series.index)


def normalize_by_probability_inverse(series, config):
    """逆向概率分段（崩溃率、被侦察概率 - 越小越好）"""
    segments = config['segments']
    scores = []
    
    for value in series:
        score = 0
        
        for i, (threshold, base_score, label) in enumerate(segments):
            if value < threshold:
                if i == 0:
                    score = base_score
                else:
                    # 在两个阈值之间线性插值
                    prev_threshold, prev_score, _ = segments[i-1]
                    ratio = (value - prev_threshold) / (threshold - prev_threshold)
                    score = prev_score - ratio * (prev_score - base_score)
                break
        
        scores.append(max(0, min(100, score)))
    
    return pd.Series(scores, index=series.index)


print("归一化方法说明:")
print("  1. 概率类（成功率、可用性等）: 直接转百分制")
print("  2. 阈值分段（时延、距离、吞吐量等）: 基于军事标准分段评分")
print("  3. 指数分段（误码率、丢包率）: 对数插值分段评分")
print("  4. 严格概率分段（高可靠性指标）: 精细化分段")
print("  5. 逆向概率分段（崩溃率、被侦察概率）: 越小越好的概率指标")
print()
print("✅ 优点: 有明确标准、可解释性强、跨批次可比、符合工程实践")
print()

# 创建标准化数据框
df_normalized = df_raw[['test_id', 'scenario_id']].copy()

# 对每个指标进行标准化
print("开始标准化各指标:")
for dim_code, dim_info in INDICATOR_SYSTEM.items():
    print(f"\n  {dim_info['name']} ({dim_code}):")
    for indicator in dim_info['indicators']:
        col_name = indicator['code']
        direction = indicator['direction']
        
        if col_name in df_raw.columns:
            # 传入指标代码，使用军事标准分段评分
            df_normalized[col_name] = normalize_indicator(df_raw[col_name], direction, col_name)
            
            # 显示标准化信息
            if col_name in MILITARY_STANDARDS:
                std_info = MILITARY_STANDARDS[col_name]
                print(f"    ✓ {indicator['name']}: {std_info['type']}类型, 参考{std_info['reference']}")
            else:
                print(f"    ✓ {indicator['name']}: Min-Max归一化（无标准）")
        else:
            print(f"    ⚠ 警告: 指标 {col_name} 不存在于数据中")
            df_normalized[col_name] = 50  # 默认中等水平

print()
print(f"✓ 完成21个指标的军事标准分段评分")
print(f"✓ 所有指标已归一化到0-100分区间")
print()

# 显示标准化后的数据预览
print("标准化后数据预览（0-100分，前3个测试批次）:")
display_cols = ['test_id'] + [ind['code'] for dim in list(INDICATOR_SYSTEM.values())[:3] for ind in dim['indicators'][:2]]
print(df_normalized[display_cols].head(3).to_string(index=False))
print()

# 显示关键指标的评分统计
print("关键指标评分统计:")
key_indicators = ['EF_avg_ber', 'EF_avg_plr', 'RL_communication_availability_rate', 
                  'RS_avg_transmission_delay_ms', 'AJ_avg_sinr']
for ind_code in key_indicators:
    if ind_code in df_normalized.columns:
        scores = df_normalized[ind_code]
        ind_name = next((ind['name'] for dim in INDICATOR_SYSTEM.values() 
                        for ind in dim['indicators'] if ind['code'] == ind_code), ind_code)
        print(f"  {ind_name}:")
        print(f"    最高分: {scores.max():.2f}, 最低分: {scores.min():.2f}, 平均分: {scores.mean():.2f}")
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
    基于标准化后的数据（0-100分）计算熵权
    
    参数:
        df_norm: 标准化后的数据框（0-100分）
        indicator_system: 指标体系
    
    返回:
        维度内指标权重字典 {indicator_code: weight_in_dimension}
    """
    
    print("在每个维度内部计算指标的熵权（客观权重）:")
    print("说明: 基于军事标准分段评分数据（0-100分）计算熵权")
    
    indicator_weights = {}
    
    for dim_code, dim_info in sorted(indicator_system.items(), key=lambda x: x[1]['priority']):
        print(f"\n  {dim_info['name']} ({dim_code}):")
        
        indicator_cols = [ind['code'] for ind in dim_info['indicators']]
        
        # 提取该维度的指标数据（Z-score标准化后的数据）
        dim_indicator_data = df_norm[indicator_cols].values
        n_samples, n_indicators = dim_indicator_data.shape
        
        if n_indicators == 1:
            # 只有1个指标，权重为1
            indicator_weights[indicator_cols[0]] = 1.0
            print(f"    {dim_info['indicators'][0]['name']}: 1.000000 (100.00%)")
        else:
            # 步骤1: 确保数据为非负值
            # 标准化后的数据已经是0-100分，直接使用
            data_positive = dim_indicator_data + 1  # +1避免log(0)
            
            # 步骤2: 归一化为概率分布（每列求和为1）
            data_sum = data_positive.sum(axis=0)
            data_sum[data_sum == 0] = 1  # 避免除以0
            p = data_positive / data_sum
            
            # 步骤3: 计算信息熵
            k = 1 / np.log(n_samples)
            entropy = np.zeros(n_indicators)
            
            for j in range(n_indicators):
                p_j = p[:, j]
                p_j = p_j[p_j > 0]  # 只考虑正值
                if len(p_j) > 0:
                    entropy[j] = -k * np.sum(p_j * np.log(p_j))
                else:
                    entropy[j] = 0
            
            # 步骤4: 计算信息效用值（差异系数）
            d = 1 - entropy
            
            # 步骤5: 计算权重
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
                print(f"    {ind_name}: 熵={entropy[i]:.4f}, 差异系数={d[i]:.4f}, 权重={weights[i]:.6f} ({weights[i]*100:.2f}%)")
    
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

# 5.2 组合AHP维度权重和熵权法指标权重
print("5.2 组合AHP维度权重和熵权法指标权重")
print("    最终权重 = AHP维度权重 × 熵权法指标权重")

final_weights = {}

for i, dim_code in enumerate(dim_codes_ordered):
    dim_weight = criteria_weights[i]  # AHP维度权重
    dim_info = INDICATOR_SYSTEM[dim_code]
    
    print(f"\n    {dim_info['name']} ({dim_code}) - AHP维度权重: {dim_weight:.6f}")
    
    for indicator in dim_info['indicators']:
        code = indicator['code']
        entropy_weight = indicator_entropy_weights[code]  # 熵权法指标权重
        total_weight = dim_weight * entropy_weight
        final_weights[code] = total_weight
        print(f"      {indicator['name']}: {entropy_weight:.6f} × {dim_weight:.6f} = {total_weight:.6f} ({total_weight*100:.2f}%)")

print()

# ============================================================================
# 第六部分：综合评分计算（移除Sigmoid映射）
# ============================================================================

print("步骤6: 综合评分计算")
print("-"*80)
print("使用混合权重（AHP维度权重 × 熵权法指标权重）")
print("指标得分已通过军事标准分段评分归一化到0-100分")
print()

# 6.1 数据已经是0-100分，直接使用
df_scores = df_normalized.copy()

print("6.1 指标得分已完成（0-100分，基于军事标准）")
print()

# 6.2 计算维度层得分
print("6.2 计算维度层得分:")
for dim_code in dim_codes_ordered:
    dim_info = INDICATOR_SYSTEM[dim_code]
    dim_score = 0
    
    for indicator in dim_info['indicators']:
        code = indicator['code']
        # 使用维度内的熵权（已归一化）
        entropy_weight = indicator_entropy_weights[code]
        dim_score += df_scores[code] * entropy_weight
    
    df_scores[f'{dim_code}_score'] = dim_score
    print(f"  {dim_info['name']} ({dim_code}): 完成")

print()
    
# 6.3 计算综合得分（目标层）
print("6.3 计算综合得分（目标层）:")
df_scores['total_score'] = 0

for i, dim_code in enumerate(dim_codes_ordered):
    dim_weight = criteria_weights[i]
    df_scores['total_score'] += df_scores[f'{dim_code}_score'] * dim_weight
    print(f"  {INDICATOR_SYSTEM[dim_code]['name']}: 权重={dim_weight:.4f}")

print()
print(f"✓ 综合得分计算完成")


# 7.4 评估等级
print("7.4 评估等级划分:")
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

# 统计各等级数量
grade_counts = df_scores['grade'].value_counts()
print("  等级分布:")
for grade in ['优秀', '良好', '中等', '及格', '较差']:
    count = grade_counts.get(grade, 0)
    print(f"    {grade}: {count}个测试批次")
print()

# 7.5 排名
df_scores['rank'] = df_scores['total_score'].rank(ascending=False, method='min').astype(int)

# 按排名排序
df_scores = df_scores.sort_values('rank')

print("✓ 完成综合评分计算")
print()

# 显示得分统计信息
print("综合得分统计:")
print(f"  最高分: {df_scores['total_score'].max():.2f}分")
print(f"  最低分: {df_scores['total_score'].min():.2f}分")
print(f"  平均分: {df_scores['total_score'].mean():.2f}分")
print(f"  标准差: {df_scores['total_score'].std():.2f}")
print()

# 显示各维度得分统计
print("各维度得分统计:")
for dim_code in dim_codes_ordered:
    dim_name = INDICATOR_SYSTEM[dim_code]['name']
    dim_scores = df_scores[f'{dim_code}_score']
    print(f"  {dim_name:8s}: 最高={dim_scores.max():.2f}, 最低={dim_scores.min():.2f}, 平均={dim_scores.mean():.2f}")
print()

# ============================================================================
# 第八部分：评估结果输出
# ============================================================================

print("="*80)
print("评估结果汇总")
print("="*80)
print()
print("说明: 综合得分 = Σ(维度得分 × AHP维度权重)")
print("      维度得分 = Σ(指标得分 × 熵权法指标权重)")
print("      指标得分 = 基于军事标准的分段评分 ∈ [0, 100]")
print("      参考标准: GJB军标、3GPP、ITU-T、NATO STANAG、MIL-STD等")
print()

# 输出排名表
print("综合排名:")
print("-"*80)
result_cols = ['rank', 'test_id', 'total_score', 'grade'] + [f'{dim}_score' for dim in dim_codes_ordered]
result_display = df_scores[result_cols].copy()
result_display.columns = ['排名', '测试批次', '综合得分', '等级'] + [INDICATOR_SYSTEM[dim]['name'] for dim in dim_codes_ordered]

# 格式化输出
for col in result_display.columns[2:]:
    if col != '等级':
        result_display[col] = result_display[col].apply(lambda x: f"{x:.2f}")

print(result_display.to_string(index=False))
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
    print()
    
    print("维度得分:")
    for dim_code in dim_codes_ordered:
        dim_name = INDICATOR_SYSTEM[dim_code]['name']
        dim_score = row[f'{dim_code}_score']
        print(f"  {dim_name:8s}: {dim_score:6.2f}分", end="")
        
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
        
        # 获取对应的评分数据
        score_row = df_scores[df_scores['test_id'] == test_id].iloc[0]
        
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
                total_score = %s,
                grade = %s,
                rank_position = %s,
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
                float(score_row['total_score']),
                score_row['grade'],
                int(score_row['rank']),
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
                total_communications, total_lifecycles,
                total_score, grade, rank_position
            ) VALUES (
                %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s
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
                row['total_lifecycles'],
                float(score_row['total_score']),
                score_row['grade'],
                int(score_row['rank'])
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
print("说明: 使用前面计算的AHP权重和算法得分")
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
    
    # 添加维度得分线（算法计算的得分）
    ax.plot(x, dim_scores, 'r-o', linewidth=3, markersize=8, 
           label=f'算法得分', zorder=10)
    
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

plt.suptitle('8维度细粒度指标分析 - 所有测试批次对比\n(柱状图=各指标归一化值, 红线=算法计算的维度得分)', 
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
        elif score >= 80:
            grade = "⭐⭐⭐⭐ 良好"
        elif score >= 70:
            grade = "⭐⭐⭐ 中等"
        elif score >= 60:
            grade = "⭐⭐ 及格"
        else:
            grade = "⚠ 不及格"
        
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
print("  2. 红色折线显示算法计算的维度综合得分")
print("  3. 绿色虚线(90分)表示优秀线，橙色虚线(60分)表示及格线")
print("  4. 红色背景标记表示该测试批次在此维度得分不及格(<60分)")
print("  5. 黄色标签显示算法计算的具体得分，可用于验证算法合理性")
print()
print("💡 验证建议:")
print("  - 检查红线(算法得分)是否与柱状图(指标值)趋势一致")
print("  - 如果某个测试批次所有指标都很高，但算法得分很低，说明权重可能有问题")
print("  - 如果某个测试批次有明显短板(某个指标很低)，算法得分应该受到影响")
print()

plt.show()

print("\n" + "="*80)
print("全部评估流程完成！")
print("="*80)