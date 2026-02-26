# ADC方法 - 可信性（D）计算方案详解

## 一、可信性（D）的计算公式

### 1.1 标准ADC公式

```
D = R(t) × M × S
```

其中：
- **R(t)**：可靠性（Reliability）- 在时间t内无故障工作的概率
- **M**：维修性（Maintainability）- 在规定时间内完成维修的概率
- **S**：保障性（Supportability）- 后勤保障满足需求的概率

---

## 二、从现有指标计算R、M、S

### 2.1 可靠性（R）的计算

#### 方法1：基于故障率的理论公式

**标准公式**：
```
R(t) = e^(-λt)
```

**参数映射**：
```python
# 从现有指标计算故障率λ
λ = RL_crash_rate / 任务时间

# 假设任务时间为8小时
t = 8  # 小时

# 计算可靠性
R = np.exp(-λ * t)
```

**示例计算**：
```python
# 测试批次1的数据
RL_crash_rate = 0.05  # 崩溃比例5%
t = 8  # 任务时间8小时

# 故障率
λ = 0.05 / 8 = 0.00625 次/小时

# 可靠性
R = e^(-0.00625 × 8) = e^(-0.05) ≈ 0.9512 = 95.12%
```

#### 方法2：基于可用性的直接映射（推荐）

**公式**：
```python
R = RL_communication_availability_rate
```

**理由**：
- 通信可用性直接反映系统在任务期间的可靠性
- 数据直接可用，无需额外计算
- 更符合实际情况

**示例**：
```python
R = 0.98  # 通信可用性98%
```

#### 方法3：综合多个可靠性指标（最推荐）⭐

**公式**：
```python
R = w1 × RL_communication_availability_rate + 
    w2 × (1 - RL_crash_rate) + 
    w3 × RL_communication_success_rate

其中：
w1 = 0.40  # 可用性权重
w2 = 0.30  # 崩溃率权重（取反）
w3 = 0.30  # 成功率权重
```

**示例计算**：
```python
# 测试批次1的数据
RL_communication_availability_rate = 0.98
RL_crash_rate = 0.05
RL_communication_success_rate = 0.95

R = 0.40 × 0.98 + 0.30 × (1 - 0.05) + 0.30 × 0.95
  = 0.392 + 0.285 + 0.285
  = 0.962 = 96.2%
```

---

### 2.2 维修性（M）的计算

#### 方法1：基于维修率的理论公式

**标准公式**：
```
M(t) = 1 - e^(-μt)
```

**参数映射**：
```python
# 从恢复时长计算维修率μ
MTTR = RL_recovery_duration_ms / 1000 / 3600  # 转换为小时
μ = 1 / MTTR  # 维修率

# 假设规定维修时间为4小时
t_repair = 4  # 小时

# 计算维修性
M = 1 - np.exp(-μ * t_repair)
```

**示例计算**：
```python
# 测试批次1的数据
RL_recovery_duration_ms = 3600000  # 3600秒 = 1小时

# 平均修复时间
MTTR = 3600000 / 1000 / 3600 = 1 小时

# 维修率
μ = 1 / 1 = 1 次/小时

# 在4小时内完成维修的概率
M = 1 - e^(-1 × 4) = 1 - e^(-4) ≈ 0.9817 = 98.17%
```

#### 方法2：基于恢复时长的归一化（推荐）

**公式**：
```python
# 定义理想恢复时长（如1小时 = 3600000ms）
ideal_recovery_time = 3600000  # ms

# 定义最差恢复时长（如24小时）
worst_recovery_time = 86400000  # ms

# 归一化计算维修性（恢复越快，维修性越好）
M = (worst_recovery_time - RL_recovery_duration_ms) / 
    (worst_recovery_time - ideal_recovery_time)

# 限制在[0, 1]范围
M = max(0, min(1, M))
```

**示例计算**：
```python
RL_recovery_duration_ms = 7200000  # 2小时

M = (86400000 - 7200000) / (86400000 - 3600000)
  = 79200000 / 82800000
  = 0.9565 = 95.65%
```

#### 方法3：简化映射（最简单）

**公式**：
```python
# 如果没有崩溃，维修性为1
if RL_crash_rate == 0:
    M = 1.0
else:
    # 基于恢复时长归一化
    M = 1 - (RL_recovery_duration_ms / max_recovery_time)
```

---

### 2.3 保障性（S）的计算

#### 方法1：基于多个保障指标的综合

**公式**：
```python
S = w1 × EF_task_success_rate + 
    w2 × (1 - EF_avg_ber_normalized) + 
    w3 × (1 - EF_avg_plr_normalized) +
    w4 × SC_interception_resistance +
    w5 × (1 - SC_detection_probability) +
    w6 × AJ_avg_sinr_normalized

其中：
w1 = 0.25  # 任务成功率
w2 = 0.15  # 误码率（归一化后取反）
w3 = 0.15  # 丢包率（归一化后取反）
w4 = 0.15  # 抗拦截能力
w5 = 0.15  # 被侦察概率（取反）
w6 = 0.15  # 信干噪比（归一化）
```

**示例计算**：
```python
# 测试批次1的数据（假设已归一化到0-1）
EF_task_success_rate = 0.95
EF_avg_ber_normalized = 0.10  # 误码率低，归一化后为0.10
EF_avg_plr_normalized = 0.12  # 丢包率低，归一化后为0.12
SC_interception_resistance = 0.92
SC_detection_probability = 0.15
AJ_avg_sinr_normalized = 0.88

S = 0.25 × 0.95 + 
    0.15 × (1 - 0.10) + 
    0.15 × (1 - 0.12) +
    0.15 × 0.92 +
    0.15 × (1 - 0.15) +
    0.15 × 0.88
  = 0.2375 + 0.135 + 0.132 + 0.138 + 0.1275 + 0.132
  = 0.902 = 90.2%
```

#### 方法2：简化为任务成功率（最简单）

**公式**：
```python
S = EF_task_success_rate
```

**理由**：
- 任务成功率直接反映保障性
- 简单直观
- 适合快速评估

---

## 三、完整的D计算流程

### 3.1 推荐方案（综合计算）

```python
def calculate_dependability(data):
    """
    计算可信性 D = R × M × S
    
    参数:
        data: 包含所有指标的数据字典
    
    返回:
        D: 可信性（0-1之间）
        R: 可靠性
        M: 维修性
        S: 保障性
    """
    
    # ========== 1. 计算可靠性 R ==========
    R = (0.40 * data['RL_communication_availability_rate'] + 
         0.30 * (1 - data['RL_crash_rate']) + 
         0.30 * data['RL_communication_success_rate'])
    
    # ========== 2. 计算维修性 M ==========
    if data['RL_crash_rate'] == 0:
        M = 1.0  # 没有崩溃，维修性为1
    else:
        # 基于恢复时长计算
        worst_recovery_time = 86400000  # 24小时（ms）
        ideal_recovery_time = 3600000   # 1小时（ms）
        
        M = (worst_recovery_time - data['RL_recovery_duration_ms']) / \
            (worst_recovery_time - ideal_recovery_time)
        M = max(0, min(1, M))  # 限制在[0, 1]
    
    # ========== 3. 计算保障性 S ==========
    # 需要先归一化误码率、丢包率、信干噪比
    # 假设已经归一化到0-1（通过normalize_indicator函数）
    
    S = (0.25 * data['EF_task_success_rate'] + 
         0.15 * (1 - data['EF_avg_ber_normalized']) + 
         0.15 * (1 - data['EF_avg_plr_normalized']) +
         0.15 * data['SC_interception_resistance'] +
         0.15 * (1 - data['SC_detection_probability']) +
         0.15 * data['AJ_avg_sinr_normalized'])
    
    # ========== 4. 计算可信性 D ==========
    D = R * M * S
    
    return D, R, M, S
```

### 3.2 计算示例

```python
# 测试批次1的数据
test_data = {
    # 可靠性相关
    'RL_communication_availability_rate': 0.98,
    'RL_crash_rate': 0.05,
    'RL_communication_success_rate': 0.95,
    'RL_recovery_duration_ms': 7200000,  # 2小时
    
    # 保障性相关（假设已归一化）
    'EF_task_success_rate': 0.95,
    'EF_avg_ber_normalized': 0.10,
    'EF_avg_plr_normalized': 0.12,
    'SC_interception_resistance': 0.92,
    'SC_detection_probability': 0.15,
    'AJ_avg_sinr_normalized': 0.88
}

# 计算
D, R, M, S = calculate_dependability(test_data)

print(f"可靠性 R = {R:.4f} ({R*100:.2f}%)")
print(f"维修性 M = {M:.4f} ({M*100:.2f}%)")
print(f"保障性 S = {S:.4f} ({S*100:.2f}%)")
print(f"可信性 D = {D:.4f} ({D*100:.2f}%)")
```

**输出**：
```
可靠性 R = 0.9620 (96.20%)
维修性 M = 0.9565 (95.65%)
保障性 S = 0.9020 (90.20%)
可信性 D = 0.8298 (82.98%)
```

---

## 四、生成概率矩阵

### 4.1 什么是概率矩阵？

在ADC方法中，概率矩阵表示每个测试批次在不同状态下的概率分布。

**矩阵结构**：
```
        R      M      S      D
Test1  [0.96,  0.96,  0.90,  0.83]
Test2  [0.94,  0.98,  0.88,  0.81]
Test3  [0.98,  1.00,  0.92,  0.90]
...
```

### 4.2 生成概率矩阵的代码

```python
import pandas as pd
import numpy as np

def generate_dependability_matrix(df_raw):
    """
    生成可信性概率矩阵
    
    参数:
        df_raw: 原始数据框，包含所有指标
    
    返回:
        df_matrix: 概率矩阵，包含R、M、S、D四列
    """
    
    # 初始化结果矩阵
    results = []
    
    for idx, row in df_raw.iterrows():
        test_id = row['test_id']
        
        # ========== 1. 计算可靠性 R ==========
        R = (0.40 * row['RL_communication_availability_rate'] + 
             0.30 * (1 - row['RL_crash_rate']) + 
             0.30 * row['RL_communication_success_rate'])
        
        # ========== 2. 计算维修性 M ==========
        if row['RL_crash_rate'] == 0:
            M = 1.0
        else:
            worst_recovery_time = 86400000  # 24小时
            ideal_recovery_time = 3600000   # 1小时
            
            M = (worst_recovery_time - row['RL_recovery_duration_ms']) / \
                (worst_recovery_time - ideal_recovery_time)
            M = max(0, min(1, M))
        
        # ========== 3. 计算保障性 S ==========
        # 先归一化误码率、丢包率、信干噪比
        ber_normalized = normalize_ber(row['EF_avg_ber'])
        plr_normalized = normalize_plr(row['EF_avg_plr'])
        sinr_normalized = normalize_sinr(row['AJ_avg_sinr'])
        
        S = (0.25 * row['EF_task_success_rate'] + 
             0.15 * (1 - ber_normalized) + 
             0.15 * (1 - plr_normalized) +
             0.15 * row['SC_interception_resistance'] +
             0.15 * (1 - row['SC_detection_probability']) +
             0.15 * sinr_normalized)
        
        # ========== 4. 计算可信性 D ==========
        D = R * M * S
        
        # 保存结果
        results.append({
            'test_id': test_id,
            'R': R,
            'M': M,
            'S': S,
            'D': D
        })
    
    # 转换为DataFrame
    df_matrix = pd.DataFrame(results)
    
    return df_matrix

# 辅助函数：归一化
def normalize_ber(ber):
    """归一化误码率（对数变换）"""
    # 误码率范围：10^-7 到 10^-3
    log_ber = -np.log10(ber + 1e-10)
    # 归一化到0-1：7对应1，3对应0
    normalized = (log_ber - 3) / (7 - 3)
    return max(0, min(1, normalized))

def normalize_plr(plr):
    """归一化丢包率（对数变换）"""
    # 丢包率范围：10^-4 到 10^-2
    log_plr = -np.log10(plr + 1e-10)
    # 归一化到0-1：4对应1，2对应0
    normalized = (log_plr - 2) / (4 - 2)
    return max(0, min(1, normalized))

def normalize_sinr(sinr):
    """归一化信干噪比"""
    # 信干噪比范围：-10dB 到 30dB
    normalized = (sinr - (-10)) / (30 - (-10))
    return max(0, min(1, normalized))
```

### 4.3 使用示例

```python
# 假设df_raw是从数据库提取的原始数据
df_matrix = generate_dependability_matrix(df_raw)

print("可信性概率矩阵:")
print(df_matrix)
```

**输出示例**：
```
可信性概率矩阵:
   test_id      R      M      S      D
0  TEST001  0.962  0.957  0.902  0.830
1  TEST002  0.945  0.980  0.885  0.819
2  TEST003  0.980  1.000  0.920  0.902
3  TEST004  0.935  0.950  0.870  0.773
4  TEST005  0.970  0.990  0.910  0.874
```

---

## 五、完整的ADC计算流程

### 5.1 计算A、D、C三要素

```python
def calculate_adc_full(df_raw):
    """
    完整的ADC计算流程
    
    返回:
        df_adc: 包含A、D、C、E的数据框
    """
    
    results = []
    
    for idx, row in df_raw.iterrows():
        test_id = row['test_id']
        
        # ========== 计算可用性 A ==========
        A = (0.5250 * row['NC_avg_connectivity_rate'] +
             0.2250 * normalize_time(row['NC_avg_network_setup_duration_ms'], 'min') +
             0.1750 * row['HO_operation_success_rate'] +
             0.0750 * normalize_time(row['HO_avg_operator_reaction_time_ms'], 'min'))
        
        # ========== 计算可信性 D ==========
        # 可靠性 R
        R = (0.40 * row['RL_communication_availability_rate'] + 
             0.30 * (1 - row['RL_crash_rate']) + 
             0.30 * row['RL_communication_success_rate'])
        
        # 维修性 M
        if row['RL_crash_rate'] == 0:
            M = 1.0
        else:
            M = (86400000 - row['RL_recovery_duration_ms']) / (86400000 - 3600000)
            M = max(0, min(1, M))
        
        # 保障性 S
        S = (0.25 * row['EF_task_success_rate'] + 
             0.15 * (1 - normalize_ber(row['EF_avg_ber'])) + 
             0.15 * (1 - normalize_plr(row['EF_avg_plr'])) +
             0.15 * row['SC_interception_resistance'] +
             0.15 * (1 - row['SC_detection_probability']) +
             0.15 * normalize_sinr(row['AJ_avg_sinr']))
        
        D = R * M * S
        
        # ========== 计算能力 C ==========
        C = (0.1200 * normalize_throughput(row['PO_effective_throughput']) +
             0.0800 * normalize_efficiency(row['PO_spectral_efficiency']) +
             0.0780 * normalize_time(row['RS_avg_transmission_delay_ms'], 'min') +
             0.0420 * normalize_time(row['RS_avg_call_setup_duration_ms'], 'min') +
             0.2040 * row['EF_task_success_rate'] +
             0.1360 * normalize_distance(row['EF_avg_communication_distance']) +
             0.3400 * row['RL_communication_success_rate'])
        
        # ========== 计算综合效能 E ==========
        E = A * D * C
        
        # 保存结果
        results.append({
            'test_id': test_id,
            'A': A,
            'R': R,
            'M': M,
            'S': S,
            'D': D,
            'C': C,
            'E': E
        })
    
    df_adc = pd.DataFrame(results)
    return df_adc
```

### 5.2 输出完整矩阵

```python
# 计算ADC
df_adc = calculate_adc_full(df_raw)

print("ADC效能矩阵:")
print(df_adc)
```

**输出示例**：
```
ADC效能矩阵:
   test_id      A      R      M      S      D      C      E
0  TEST001  0.920  0.962  0.957  0.902  0.830  0.885  0.677
1  TEST002  0.905  0.945  0.980  0.885  0.819  0.870  0.646
2  TEST003  0.950  0.980  1.000  0.920  0.902  0.910  0.778
3  TEST004  0.890  0.935  0.950  0.870  0.773  0.850  0.585
4  TEST005  0.935  0.970  0.990  0.910  0.874  0.895  0.730
```

---

## 六、验证与分析

### 6.1 验证D的合理性

```python
# 检查D的范围
assert (df_adc['D'] >= 0).all() and (df_adc['D'] <= 1).all()

# 检查D = R × M × S
for idx, row in df_adc.iterrows():
    calculated_D = row['R'] * row['M'] * row['S']
    assert abs(calculated_D - row['D']) < 0.0001
```

### 6.2 短板分析

```python
def analyze_bottleneck(row):
    """分析短板"""
    components = {'R': row['R'], 'M': row['M'], 'S': row['S']}
    min_component = min(components, key=components.get)
    min_value = components[min_component]
    
    print(f"Test {row['test_id']}:")
    print(f"  可靠性 R = {row['R']:.4f}")
    print(f"  维修性 M = {row['M']:.4f}")
    print(f"  保障性 S = {row['S']:.4f}")
    print(f"  可信性 D = {row['D']:.4f}")
    print(f"  短板: {min_component} = {min_value:.4f}")
    print()

# 分析每个测试批次
for idx, row in df_adc.iterrows():
    analyze_bottleneck(row)
```

---

**总结**：

通过D = R × M × S公式计算可信性的关键步骤：
1. **R（可靠性）**：综合可用性、崩溃率、成功率
2. **M（维修性）**：基于恢复时长计算
3. **S（保障性）**：综合任务成功率、误码率、丢包率、安全性、抗干扰等
4. **D（可信性）**：R × M × S

最终生成包含R、M、S、D的概率矩阵，每行代表一个测试批次的可信性分解。
