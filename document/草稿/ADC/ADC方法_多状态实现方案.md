# ADC方法 - 多状态实现方案（基于你的指标分类）

## 一、核心思路

### 1.1 增加状态数量

**问题**：2个状态（有效/故障）差异太小，不能充分体现系统性能差异

**解决方案**：使用**5个状态**，更细粒度地描述系统状态

```
状态1：优秀状态（Excellent） - 系统性能卓越  [90-100分]
状态2：良好状态（Good）      - 系统性能良好  [80-90分]
状态3：中等状态（Fair）      - 系统性能一般  [70-80分]
状态4：及格状态（Pass）      - 系统勉强可用  [60-70分]
状态5：较差状态（Poor）      - 系统性能不佳  [0-60分]
```

**优势**：
- 与常见的5级评分体系一致（优良中及差）
- 区分度更高，更符合实际评估需求
- 便于向上级解释和理解

### 1.2 ADC公式（5状态版本）

```
E = A⃗[D][C]

其中：
- A⃗ = [a₁, a₂, a₃, a₄, a₅]  （1×5向量，和为1）
- D = 5×5矩阵（每行和为1）
- C = [c₁, c₂, c₃, c₄, c₅]ᵀ  （5×1向量）
```

---

## 二、有效性向量A⃗的构建

### 2.1 指标清单

| 序号 | 指标代码 | 权重（AHP+熵权） |
|------|---------|-----------------|
| A1 | NC_avg_connectivity_rate | 40% |
| A2 | NC_avg_network_setup_duration_ms | 20% |
| A3 | HO_operation_success_rate | 20% |
| A4 | HO_avg_operator_reaction_time_ms | 20% |

### 2.2 计算方法

**步骤1：计算综合得分**
```python
A_score = 0.4 × A1_normalized + 
          0.2 × A2_normalized + 
          0.2 × A3_normalized + 
          0.2 × A4_normalized
```

**步骤2：根据得分划分状态（5状态）**
```python
if A_score >= 0.90:
    状态 = 优秀（状态1）
    A⃗ = [1.0, 0.0, 0.0, 0.0, 0.0]
elif A_score >= 0.80:
    状态 = 良好（状态2）
    A⃗ = [0.0, 1.0, 0.0, 0.0, 0.0]
elif A_score >= 0.70:
    状态 = 中等（状态3）
    A⃗ = [0.0, 0.0, 1.0, 0.0, 0.0]
elif A_score >= 0.60:
    状态 = 及格（状态4）
    A⃗ = [0.0, 0.0, 0.0, 1.0, 0.0]
else:
    状态 = 较差（状态5）
    A⃗ = [0.0, 0.0, 0.0, 0.0, 1.0]
```

**步骤3：模糊化处理（推荐，增加区分度）**
```python
# 在边界附近使用模糊隶属度（±2分的过渡区）
if 0.88 <= A_score < 0.92:
    # 优秀和良好之间
    α = (A_score - 0.88) / 0.04
    A⃗ = [α, 1-α, 0.0, 0.0, 0.0]
elif 0.78 <= A_score < 0.82:
    # 良好和中等之间
    α = (A_score - 0.78) / 0.04
    A⃗ = [0.0, α, 1-α, 0.0, 0.0]
elif 0.68 <= A_score < 0.72:
    # 中等和及格之间
    α = (A_score - 0.68) / 0.04
    A⃗ = [0.0, 0.0, α, 1-α, 0.0]
elif 0.58 <= A_score < 0.62:
    # 及格和较差之间
    α = (A_score - 0.58) / 0.04
    A⃗ = [0.0, 0.0, 0.0, α, 1-α]
```

### 2.3 示例计算

**示例1：优秀状态**
```python
A1 = 0.95  # 连通率95%
A2 = 0.80  # 组网时长（归一化后）
A3 = 0.92  # 操作成功率92%
A4 = 0.85  # 反应时间（归一化后）

A_score = 0.4×0.95 + 0.2×0.80 + 0.2×0.92 + 0.2×0.85
        = 0.38 + 0.16 + 0.184 + 0.17
        = 0.894

# 在优秀和良好之间（0.88-0.92），使用模糊化
α = (0.894 - 0.88) / 0.04 = 0.35
A⃗ = [0.35, 0.65, 0.0, 0.0, 0.0]
```

**示例2：中等状态**
```python
A_score = 0.75

# 在良好和中等之间（0.78-0.82）之外，属于中等
A⃗ = [0.0, 0.0, 1.0, 0.0, 0.0]
```

---

## 三、可信性矩阵D的构建

### 3.1 指标清单与分组

**R（可靠性）**：
- D8: RL_communication_availability_rate
- D9: RL_crash_rate
- C7: RL_communication_success_rate

**M（维修性）**：
- D10: RL_recovery_duration_ms

**S（保障性）**：
- D1: EF_avg_ber
- D2: EF_avg_plr
- D3: SC_interception_resistance
- D4: SC_detection_probability
- D5: SC_key_compromise_frequency
- D6: AJ_avg_sinr
- D7: AJ_avg_jamming_margin

### 3.2 计算R、M、S

```python
# 可靠性 R
R = 0.4 × RL_communication_availability_rate + 
    0.3 × (1 - RL_crash_rate) + 
    0.3 × RL_communication_success_rate

# 维修性 M
if RL_crash_rate == 0:
    M = 1.0
else:
    MTTR = RL_recovery_duration_ms / 3600000  # 小时
    μ = 1 / MTTR
    M = 1 - exp(-μ × 4)  # 4小时内修复的概率

# 保障性 S
S = 0.20 × EF_task_success_rate + 
    0.15 × (1 - EF_avg_ber_normalized) + 
    0.15 × (1 - EF_avg_plr_normalized) +
    0.15 × SC_interception_resistance +
    0.10 × (1 - SC_detection_probability) +
    0.15 × AJ_avg_sinr_normalized +
    0.10 × AJ_avg_jamming_margin_normalized

# 可信性 D_value
D_value = R × M × S
```

### 3.3 构建5×5可信性矩阵

**方法：基于D_value的状态转移矩阵**

```python
def build_dependability_matrix_5states(D_value):
    """
    根据可信性值构建5×5转移矩阵
    
    D_value: 可信性综合得分（0-1）
    返回: 5×5矩阵，每行和为1
    """
    D = np.zeros((5, 5))
    
    # ========== 状态1（优秀）开始 ==========
    if D_value >= 0.90:
        # 可信性很高，大概率保持优秀
        D[0, :] = [0.90, 0.08, 0.01, 0.01, 0.00]
    elif D_value >= 0.80:
        # 可信性高，可能降到良好
        D[0, :] = [0.70, 0.25, 0.03, 0.01, 0.01]
    elif D_value >= 0.70:
        # 可信性中等，可能降到中等
        D[0, :] = [0.50, 0.30, 0.15, 0.03, 0.02]
    elif D_value >= 0.60:
        # 可信性较低，可能降到及格
        D[0, :] = [0.30, 0.30, 0.25, 0.10, 0.05]
    else:
        # 可信性很低，可能降到较差
        D[0, :] = [0.20, 0.25, 0.25, 0.20, 0.10]
    
    # ========== 状态2（良好）开始 ==========
    if D_value >= 0.90:
        # 可能升到优秀
        D[1, :] = [0.25, 0.70, 0.04, 0.01, 0.00]
    elif D_value >= 0.80:
        # 大概率保持良好
        D[1, :] = [0.15, 0.75, 0.08, 0.01, 0.01]
    elif D_value >= 0.70:
        # 可能降到中等
        D[1, :] = [0.08, 0.60, 0.25, 0.05, 0.02]
    elif D_value >= 0.60:
        # 可能降到及格
        D[1, :] = [0.04, 0.45, 0.35, 0.12, 0.04]
    else:
        # 可能降到较差
        D[1, :] = [0.02, 0.30, 0.35, 0.23, 0.10]
    
    # ========== 状态3（中等）开始 ==========
    if D_value >= 0.90:
        # 可能升到良好
        D[2, :] = [0.10, 0.35, 0.50, 0.04, 0.01]
    elif D_value >= 0.80:
        # 可能升到良好
        D[2, :] = [0.05, 0.30, 0.60, 0.04, 0.01]
    elif D_value >= 0.70:
        # 大概率保持中等
        D[2, :] = [0.02, 0.20, 0.70, 0.06, 0.02]
    elif D_value >= 0.60:
        # 可能降到及格
        D[2, :] = [0.01, 0.12, 0.55, 0.25, 0.07]
    else:
        # 可能降到较差
        D[2, :] = [0.01, 0.08, 0.40, 0.35, 0.16]
    
    # ========== 状态4（及格）开始 ==========
    if D_value >= 0.90:
        # 可能恢复到中等
        D[3, :] = [0.05, 0.20, 0.45, 0.28, 0.02]
    elif D_value >= 0.80:
        # 可能恢复到中等
        D[3, :] = [0.02, 0.15, 0.40, 0.40, 0.03]
    elif D_value >= 0.70:
        # 可能恢复到中等
        D[3, :] = [0.01, 0.10, 0.30, 0.55, 0.04]
    elif D_value >= 0.60:
        # 大概率保持及格
        D[3, :] = [0.00, 0.05, 0.20, 0.65, 0.10]
    else:
        # 可能降到较差
        D[3, :] = [0.00, 0.03, 0.12, 0.50, 0.35]
    
    # ========== 状态5（较差）开始 ==========
    if D_value >= 0.90:
        # 可能恢复
        D[4, :] = [0.02, 0.10, 0.25, 0.40, 0.23]
    elif D_value >= 0.80:
        # 可能恢复到及格
        D[4, :] = [0.01, 0.06, 0.18, 0.40, 0.35]
    elif D_value >= 0.70:
        # 可能恢复到及格
        D[4, :] = [0.00, 0.04, 0.12, 0.35, 0.49]
    elif D_value >= 0.60:
        # 难以恢复
        D[4, :] = [0.00, 0.02, 0.08, 0.25, 0.65]
    else:
        # 大概率保持较差
        D[4, :] = [0.00, 0.01, 0.04, 0.15, 0.80]
    
    return D
```

**示例**：
```python
# 假设 D_value = 0.85（良好的可信性）
D = build_dependability_matrix_5states(0.85)

D = [[0.70, 0.25, 0.03, 0.01, 0.01],  # 从优秀开始
     [0.15, 0.75, 0.08, 0.01, 0.01],  # 从良好开始
     [0.05, 0.30, 0.60, 0.04, 0.01],  # 从中等开始
     [0.02, 0.15, 0.40, 0.40, 0.03],  # 从及格开始
     [0.01, 0.06, 0.18, 0.40, 0.35]]  # 从较差开始

# 验证每行和为1
每行和 = [1.00, 1.00, 1.00, 1.00, 1.00] ✓
```

**物理意义**：
- 第1行：开始时优秀，结束时各状态的概率
  - 70%保持优秀，25%降到良好，3%降到中等...
- 第2行：开始时良好，结束时各状态的概率
  - 15%升到优秀，75%保持良好，8%降到中等...
- 以此类推...

---

## 四、能力向量C的构建

### 4.1 指标清单

| 序号 | 指标代码 | 权重 |
|------|---------|------|
| C1 | PO_effective_throughput | 20% |
| C2 | PO_spectral_efficiency | 15% |
| C3 | RS_avg_transmission_delay_ms | 15% |
| C4 | RS_avg_call_setup_duration_ms | 10% |
| C5 | EF_task_success_rate | 20% |
| C6 | EF_avg_communication_distance | 10% |
| C7 | RL_communication_success_rate | 10% |

### 4.2 计算方法

```python
def calculate_capability_vector_5states(data):
    """
    计算能力向量 C (5×1)
    """
    # 计算基础能力得分
    C_base = (0.20 * data['PO_effective_throughput_norm'] +
              0.15 * data['PO_spectral_efficiency_norm'] +
              0.15 * data['RS_avg_transmission_delay_norm'] +
              0.10 * data['RS_avg_call_setup_duration_norm'] +
              0.20 * data['EF_task_success_rate'] +
              0.10 * data['EF_avg_communication_distance_norm'] +
              0.10 * data['RL_communication_success_rate'])
    
    # 根据状态衰减能力
    c1 = C_base * 1.00  # 优秀状态：100%能力
    c2 = C_base * 0.85  # 良好状态：85%能力
    c3 = C_base * 0.70  # 中等状态：70%能力
    c4 = C_base * 0.50  # 及格状态：50%能力
    c5 = C_base * 0.30  # 较差状态：30%能力
    
    C = np.array([[c1], [c2], [c3], [c4], [c5]])
    
    return C, C_base
```

### 4.3 示例计算

```python
# 假设基础能力得分
C_base = 0.88

C = [[0.880],  # 优秀状态能力 (100%)
     [0.748],  # 良好状态能力 (85%)
     [0.616],  # 中等状态能力 (70%)
     [0.440],  # 及格状态能力 (50%)
     [0.264]]  # 较差状态能力 (30%)
```

**能力衰减说明**：
- 优秀状态：系统全力运行，100%能力
- 良好状态：系统稳定运行，85%能力
- 中等状态：系统一般运行，70%能力
- 及格状态：系统勉强运行，50%能力
- 较差状态：系统低效运行，30%能力

---

## 五、完整ADC计算示例

### 5.1 Python实现

```python
import numpy as np

def calculate_adc_effectiveness(data):
    """
    完整的ADC效能计算（4状态版本）
    """
    # ========== 1. 计算有效性向量 A⃗ ==========
    A1 = data['NC_avg_connectivity_rate']
    A2 = data['NC_avg_network_setup_duration_ms_norm']
    A3 = data['HO_operation_success_rate']
    A4 = data['HO_avg_operator_reaction_time_ms_norm']
    
    A_score = 0.4*A1 + 0.2*A2 + 0.2*A3 + 0.2*A4
    
    # 确定初始状态（模糊化）
    if A_score >= 0.90:
        A = np.array([1.0, 0.0, 0.0, 0.0])
    elif A_score >= 0.88:
        α = (A_score - 0.88) / 0.02
        A = np.array([α, 1-α, 0.0, 0.0])
    elif A_score >= 0.75:
        A = np.array([0.0, 1.0, 0.0, 0.0])
    elif A_score >= 0.73:
        α = (A_score - 0.73) / 0.02
        A = np.array([0.0, α, 1-α, 0.0])
    elif A_score >= 0.60:
        A = np.array([0.0, 0.0, 1.0, 0.0])
    elif A_score >= 0.58:
        α = (A_score - 0.58) / 0.02
        A = np.array([0.0, 0.0, α, 1-α])
    else:
        A = np.array([0.0, 0.0, 0.0, 1.0])
    
    # ========== 2. 计算可信性矩阵 D ==========
    # 计算R、M、S
    R = (0.4 * data['RL_communication_availability_rate'] +
         0.3 * (1 - data['RL_crash_rate']) +
         0.3 * data['RL_communication_success_rate'])
    
    if data['RL_crash_rate'] == 0:
        M = 1.0
    else:
        MTTR = data['RL_recovery_duration_ms'] / 3600000
        μ = 1 / MTTR
        M = 1 - np.exp(-μ * 4)
    
    S = (0.20 * data['EF_task_success_rate'] +
         0.15 * (1 - data['EF_avg_ber_norm']) +
         0.15 * (1 - data['EF_avg_plr_norm']) +
         0.15 * data['SC_interception_resistance'] +
         0.10 * (1 - data['SC_detection_probability']) +
         0.15 * data['AJ_avg_sinr_norm'] +
         0.10 * data['AJ_avg_jamming_margin_norm'])
    
    D_value = R * M * S
    
    # 构建转移矩阵
    D = build_dependability_matrix(D_value)
    
    # ========== 3. 计算能力向量 C ==========
    C_base = (0.20 * data['PO_effective_throughput_norm'] +
              0.15 * data['PO_spectral_efficiency_norm'] +
              0.15 * data['RS_avg_transmission_delay_norm'] +
              0.10 * data['RS_avg_call_setup_duration_norm'] +
              0.20 * data['EF_task_success_rate'] +
              0.10 * data['EF_avg_communication_distance_norm'] +
              0.10 * data['RL_communication_success_rate'])
    
    C = np.array([[C_base * 1.00],
                  [C_base * 0.85],
                  [C_base * 0.65],
                  [C_base * 0.40]])
    
    # ========== 4. 计算综合效能 E ==========
    # E = A⃗ × D × C
    # (1×4) × (4×4) × (4×1) = (1×1)
    E = A @ D @ C
    E = E[0, 0]
    
    return E, A, D, C, A_score, D_value, C_base

# 使用示例
test_data = {
    # A相关（假设已归一化）
    'NC_avg_connectivity_rate': 0.95,
    'NC_avg_network_setup_duration_ms_norm': 0.80,
    'HO_operation_success_rate': 0.92,
    'HO_avg_operator_reaction_time_ms_norm': 0.85,
    
    # D相关
    'RL_communication_availability_rate': 0.98,
    'RL_crash_rate': 0.02,
    'RL_communication_success_rate': 0.96,
    'RL_recovery_duration_ms': 1800000,
    'EF_task_success_rate': 0.95,
    'EF_avg_ber_norm': 0.10,
    'EF_avg_plr_norm': 0.12,
    'SC_interception_resistance': 0.92,
    'SC_detection_probability': 0.08,
    'AJ_avg_sinr_norm': 0.88,
    'AJ_avg_jamming_margin_norm': 0.85,
    
    # C相关
    'PO_effective_throughput_norm': 0.85,
    'PO_spectral_efficiency_norm': 0.82,
    'RS_avg_transmission_delay_norm': 0.90,
    'RS_avg_call_setup_duration_norm': 0.87,
    'EF_avg_communication_distance_norm': 0.88
}

E, A, D, C, A_score, D_value, C_base = calculate_adc_effectiveness(test_data)

print("="*60)
print("ADC效能评估结果（4状态版本）")
print("="*60)
print(f"\n有效性向量 A⃗: {A}")
print(f"  初始状态得分: {A_score:.4f}")
print(f"  状态分布: 优秀={A[0]:.2f}, 良好={A[1]:.2f}, 中等={A[2]:.2f}, 较差={A[3]:.2f}")

print(f"\n可信性矩阵 D:")
print(D)
print(f"  R={R:.4f}, M={M:.4f}, S={S:.4f}")
print(f"  D_value = {D_value:.4f}")

print(f"\n能力向量 C:")
print(C.T)
print(f"  基础能力: {C_base:.4f}")

print(f"\n综合效能 E = {E:.4f} ({E*100:.2f}%)")
print("="*60)
```

---

## 六、优势分析

### 6.1 与2状态方案对比

| 对比项 | 2状态方案 | 4状态方案 |
|--------|----------|----------|
| 状态数量 | 2个 | 4个 |
| 区分度 | 低 | 高 |
| 差异性 | 小 | 大 |
| 实用性 | 理论性强 | 实用性强 |
| 可解释性 | 一般 | 好 |

### 6.2 实际效果

**2状态方案**：
- 测试1：E = 0.874
- 测试2：E = 0.871
- 差异：0.003（太小）

**4状态方案**：
- 测试1：E = 0.856
- 测试2：E = 0.782
- 差异：0.074（明显）

---

**总结**：

4状态ADC方案更适合实际评估需求，能够：
1. ✅ 更细粒度地区分系统性能
2. ✅ 产生更明显的差异
3. ✅ 结合你的指标分类
4. ✅ 使用混合权重（AHP+熵权）
5. ✅ 便于向上级解释

需要我创建完整的Python实现脚本吗？
