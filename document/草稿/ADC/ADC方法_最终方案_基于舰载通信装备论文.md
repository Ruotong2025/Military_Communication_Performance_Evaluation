# 军事通信效能评估 - ADC方法最终方案

## 参考文献
魏玉人等. 基于ADC的舰载通信装备FAHP效能评估方法研究[J]. 舰船电子工程, 2020, 第12期: 110.

---

## 一、ADC法核心思想

### 1.1 基本公式

```
E = A × D × C
```

其中：
- **A**：可用度矩阵（1×2向量）
- **D**：可信度矩阵（2×2矩阵）
- **C**：能力矩阵（2×5矩阵）
- **E**：效能向量（1×5向量）

### 1.2 系统状态

通信装备仅存在**两种状态**：
- **状态1**：正常工作状态
- **状态2**：故障状态

---

## 二、可用度矩阵A（1×2）

### 2.1 定义

```
A = [a₁, a₂]
```

其中：
- **a₁**：装备正常工作的概率
- **a₂**：装备故障的概率
- **约束**：a₂ = 1 - a₁

### 2.2 计算方法

**方法1：基于MTBF和MTTR**
```python
a₁ = MTBF / (MTBF + MTTR)
a₂ = 1 - a₁
```

**方法2：基于数据库统计**
```python
# 从 communication_network_lifecycle 表
总运行时间 = SUM(total_lifecycle_duration_ms)
总故障时间 = SUM(total_interruption_duration_ms)

a₁ = (总运行时间 - 总故障时间) / 总运行时间
a₂ = 1 - a₁
```

**方法3：基于通信成功率**
```python
# 从 during_battle_communications 表
成功次数 = COUNT(communication_success = 1)
总次数 = COUNT(*)

a₁ = 成功次数 / 总次数
a₂ = 1 - a₁
```

### 2.3 示例

```python
# 假设通信系统可用性为95%
a₁ = 0.95
a₂ = 0.05

A = [0.95, 0.05]
```

---

## 三、可信度矩阵D（2×2）

### 3.1 定义

```
D = [d₁₁  d₁₂]
    [d₂₁  d₂₂]
```

其中：
- **d₁₁**：通信装备正常使用概率（保持正常）
- **d₁₂**：通信装备故障概率（从正常到故障）
- **d₂₁**：修复机器故障概率（从故障到正常）
- **d₂₂**：保持故障状态概率

### 3.2 D矩阵的物理意义

**D矩阵的4个元素代表状态转移概率**：

```
       任务开始时状态
           ↓
D = [d₁₁  d₁₂]  ← 初始正常状态
    [d₂₁  d₂₂]  ← 初始故障状态
     ↑    ↑
   正常  故障  ← 任务结束时状态
```

| 元素 | 物理意义 | 影响因素 |
|------|---------|---------|
| **d₁₁** | 正常→正常 | 设备可靠性（MTBF） |
| **d₁₂** | 正常→故障 | 故障率（1/MTBF） |
| **d₂₁** | 故障→正常 | 维修能力（MTTR、备件、人员） |
| **d₂₂** | 故障→故障 | 无法修复的概率 |

**约束条件**：
- d₁₁ + d₁₂ = 1（从正常状态出发，要么保持正常，要么转为故障）
- d₂₁ + d₂₂ = 1（从故障状态出发，要么修复成功，要么保持故障）

### 3.3 两种场景对比

#### 场景1：无维修保障（论文场景 - 舰艇出海）

> "限于出海舰艇难以提供足够的维修保证，因此该概率为0"

```python
# 任务期间无法修复故障
d₂₁ = 0  # 故障后无法修复
d₂₂ = 1  # 保持故障状态

D = [d₁₁  d₁₂]
    [0    1   ]
```

**适用情况**：
- 舰艇远洋航行
- 野外作战环境
- 缺乏维修人员和备件
- 任务时间短，来不及修复

#### 场景2：有维修保障（实际情况 - 基地驻扎）

```python
# 任务期间可以修复故障
d₂₁ = MTTR / (MTBF + MTTR) × repair_success_rate  # 修复成功概率
d₂₂ = 1 - d₂₁  # 保持故障概率

D = [d₁₁  d₁₂]
    [d₂₁  d₂₂]
```

**适用情况**：
- 基地驻扎
- 有完善的维修保障
- 有充足的备件和维修人员
- 任务时间长，有时间修复

### 3.4 d₂₁的计算方法（有维修保障时）

#### 方法1：基于指数分布的精确公式（推荐）⭐

**参考文献**：孟庆德等. 基于ADC法的舰炮武器系统作战效能评估模型[J]. 火炮发射与控制学报, 2015, 36(1).

**理论基础**：
- 系统故障服从指数分布（故障率λ）
- 系统维修服从指数分布（维修率μ）
- 任务期间可以进行有效修理

**完整公式**：
```python
λ = 1 / MTBF  # 故障率
μ = 1 / MTTR  # 维修率
T = 任务时间

# D矩阵的4个元素（论文式7-10）
d₁₁ = μ/(λ+μ) + λ/(λ+μ) × exp[-(λ+μ)T]
d₁₂ = λ/(λ+μ) × {1 - exp[-(λ+μ)T]}
d₂₁ = μ/(λ+μ) × {1 - exp[-(λ+μ)T]}
d₂₂ = λ/(λ+μ) + μ/(λ+μ) × exp[-(λ+μ)T]
```

**物理意义**：
- **μ/(λ+μ)**：稳态正常概率（长期运行后系统正常的概率）
- **λ/(λ+μ)**：稳态故障概率（长期运行后系统故障的概率）
- **exp[-(λ+μ)T]**：衰减因子（随时间衰减到稳态）

**验证约束**：
```python
# 验证每行和为1
d₁₁ + d₁₂ = μ/(λ+μ) + λ/(λ+μ) × exp[-(λ+μ)T] + λ/(λ+μ) × {1 - exp[-(λ+μ)T]}
           = μ/(λ+μ) + λ/(λ+μ)
           = 1 ✓

d₂₁ + d₂₂ = μ/(λ+μ) × {1 - exp[-(λ+μ)T]} + λ/(λ+μ) + μ/(λ+μ) × exp[-(λ+μ)T]
           = μ/(λ+μ) + λ/(λ+μ)
           = 1 ✓
```

#### 方法2：简化公式（无维修保障）

**论文式6**：
```python
# 任务期间不能修理
d₁₁ = exp(-λT)
d₁₂ = 1 - exp(-λT)
d₂₁ = 0
d₂₂ = 1
```

#### 方法3：基于MTTR的近似公式

```python
# 修复概率 = 能够修复的概率 × 修复成功率
repair_probability = min(t / MTTR, 1.0)
repair_success_rate = 0.9

d₂₁ = repair_probability × repair_success_rate
d₂₂ = 1 - d₂₁
```

#### 方法4：基于历史数据统计

```python
# 从数据库统计
故障修复成功次数 = COUNT(故障后恢复正常)
故障总次数 = COUNT(发生故障)

d₂₁ = 故障修复成功次数 / 故障总次数
d₂₂ = 1 - d₂₁
```

### 3.5 完整计算示例

#### 示例1：无维修保障（舰艇出海场景）

```python
# 假设任务时间24小时，MTBF=1000小时
T = 24
MTBF = 1000
λ = 1/1000 = 0.001

# 使用论文式6（不可修复）
d₁₁ = exp(-λT) = exp(-0.001×24) = exp(-0.024) = 0.9763
d₁₂ = 1 - exp(-λT) = 1 - 0.9763 = 0.0237
d₂₁ = 0  # 无法修复
d₂₂ = 1

D = [0.9763  0.0237]
    [0       1     ]
```

**物理意义**：
- 97.63%的概率保持正常工作
- 2.37%的概率发生故障
- 一旦故障，无法修复（d₂₁=0）

#### 示例2：有维修保障（基地驻扎场景）⭐

```python
# 假设任务时间24小时，MTBF=1000小时，MTTR=2小时
T = 24
MTBF = 1000
MTTR = 2

λ = 1/MTBF = 1/1000 = 0.001  # 故障率
μ = 1/MTTR = 1/2 = 0.5       # 维修率

# 使用论文式7-10（可修复系统的精确公式）
λ_plus_μ = λ + μ = 0.001 + 0.5 = 0.501

# 稳态概率
steady_normal = μ/(λ+μ) = 0.5/0.501 = 0.9980
steady_fault = λ/(λ+μ) = 0.001/0.501 = 0.0020

# 衰减因子
decay = exp(-(λ+μ)T) = exp(-0.501×24) = exp(-12.024) ≈ 0.00000599

# D矩阵元素（论文式7-10）
d₁₁ = μ/(λ+μ) + λ/(λ+μ) × exp[-(λ+μ)T]
    = 0.9980 + 0.0020 × 0.00000599
    = 0.9980 + 0.00000001198
    ≈ 0.9980

d₁₂ = λ/(λ+μ) × {1 - exp[-(λ+μ)T]}
    = 0.0020 × (1 - 0.00000599)
    = 0.0020 × 0.99999401
    ≈ 0.0020

d₂₁ = μ/(λ+μ) × {1 - exp[-(λ+μ)T]}
    = 0.9980 × (1 - 0.00000599)
    = 0.9980 × 0.99999401
    ≈ 0.9980

d₂₂ = λ/(λ+μ) + μ/(λ+μ) × exp[-(λ+μ)T]
    = 0.0020 + 0.9980 × 0.00000599
    = 0.0020 + 0.00000598
    ≈ 0.0020

D = [0.9980  0.0020]
    [0.9980  0.0020]
```

**验证**：
```python
# 验证每行和为1
d₁₁ + d₁₂ = 0.9980 + 0.0020 = 1.0 ✓
d₂₁ + d₂₂ = 0.9980 + 0.0020 = 1.0 ✓
```

**物理意义**：
- **d₁₁=0.9980**：初始正常，任务结束时仍正常的概率为99.80%
- **d₁₂=0.0020**：初始正常，任务结束时转为故障的概率为0.20%
- **d₂₁=0.9980**：初始故障，任务结束时修复成功的概率为99.80%（关键！）
- **d₂₂=0.0020**：初始故障，任务结束时仍故障的概率为0.20%

**关键发现**：
- 由于MTTR=2小时远小于任务时间24小时
- 系统有充足时间修复（可以修复12次）
- 因此d₂₁≈d₁₁，都接近稳态正常概率99.80%
- 这说明维修能力极强，几乎可以完全恢复！

#### 示例3：中等维修保障

```python
# 假设任务时间24小时，MTBF=1000小时，MTTR=20小时
T = 24
MTBF = 1000
MTTR = 20

λ = 1/1000 = 0.001
μ = 1/20 = 0.05

λ_plus_μ = 0.051
steady_normal = 0.05/0.051 = 0.9804
steady_fault = 0.001/0.051 = 0.0196
decay = exp(-0.051×24) = exp(-1.224) = 0.2942

# D矩阵
d₁₁ = 0.9804 + 0.0196 × 0.2942 = 0.9862
d₁₂ = 0.0196 × (1 - 0.2942) = 0.0138
d₂₁ = 0.9804 × (1 - 0.2942) = 0.6919
d₂₂ = 0.0196 + 0.9804 × 0.2942 = 0.3081

D = [0.9862  0.0138]
    [0.6919  0.3081]
```

**物理意义**：
- d₂₁=0.6919：初始故障，有69.19%的概率修复成功
- d₂₂=0.3081：初始故障，有30.81%的概率仍故障
- 由于MTTR=20小时接近任务时间24小时，修复能力有限

### 3.6 d₂₁对效能的影响

**对比分析**：

```python
# 场景1：无维修保障（d₂₁=0）
A = [0.95, 0.05]
D1 = [0.9763  0.0237]
     [0       1     ]
C = [0.090  0.249  0.430  0.206  0.026]
    [0      0      0      0      0    ]

E1 = A × D1 × C = [0.0835, 0.2309, 0.3988, 0.1911, 0.0241]
综合得分1 = 0.6847

# 场景2：有维修保障（d₂₁=0.9）
D2 = [0.9763  0.0237]
     [0.9     0.1   ]

E2 = A × D2 × C = [0.0876, 0.2423, 0.4186, 0.2006, 0.0253]
综合得分2 = 0.7189

# 效能提升
提升幅度 = (0.7189 - 0.6847) / 0.6847 = 5.0%
```

**结论**：
- d₂₁从0提升到0.9，系统效能提升约5%
- 维修保障能力直接影响系统效能
- d₂₁越大，系统容错能力越强

---

## 四、能力矩阵C（2×5）

### 4.1 定义

```
C = [b₁  b₂  b₃  b₄  b₅]  ← 正常状态的能力向量
    [0   0   0   0   0 ]  ← 故障状态的能力向量
```

其中：
- **第1行**：装备正常使用状态下的能力向量（来自模糊评判）
- **第2行**：装备故障状态时能力向量为全0

### 4.2 能力向量的获取

**论文方法**：利用单因素模糊评判获得

```python
# 通过模糊评判计算得到
B' = W × R  # 权重向量 × 隶属度矩阵

# 示例（论文中的结果）
B' = [0.090, 0.249, 0.430, 0.206, 0.026]
```

**简化方法**：直接使用指标得分

```python
# 5个评价等级：优、良、中、及格、差
# 对应分数：1.0, 0.8, 0.6, 0.4, 0.2

# 如果系统综合得分为0.85（良好）
# 可以映射为能力向量
b = [0.1, 0.7, 0.2, 0.0, 0.0]  # 10%优，70%良，20%中
```

### 4.3 示例

```python
# 使用论文中的数据
C = [0.090  0.249  0.430  0.206  0.026]
    [0      0      0      0      0    ]
```

---

## 五、效能计算E（1×5）

### 5.1 计算公式

```
E = A × D × C
```

### 5.2 详细计算过程

```python
# 步骤1：A × D（1×2 矩阵乘 2×2 矩阵 = 1×2 向量）
A × D = [a₁, a₂] × [d₁₁  d₁₂]
                    [d₂₁  d₂₂]

     = [a₁×d₁₁ + a₂×d₂₁,  a₁×d₁₂ + a₂×d₂₂]

# 步骤2：(A × D) × C（1×2 向量乘 2×5 矩阵 = 1×5 向量）
E = (A × D) × C
  = [e₁, e₂, e₃, e₄, e₅]
```

### 5.3 完整示例

```python
# 输入数据
A = [0.95, 0.05]

D = [0.9763  0.0237]
    [0       1     ]

C = [0.090  0.249  0.430  0.206  0.026]
    [0      0      0      0      0    ]

# 计算步骤1：A × D
AD = [0.95×0.9763 + 0.05×0,  0.95×0.0237 + 0.05×1]
   = [0.9275,  0.0725]

# 计算步骤2：(A × D) × C
E = [0.9275, 0.0725] × [0.090  0.249  0.430  0.206  0.026]
                        [0      0      0      0      0    ]

  = [0.9275×0.090 + 0.0725×0,
     0.9275×0.249 + 0.0725×0,
     0.9275×0.430 + 0.0725×0,
     0.9275×0.206 + 0.0725×0,
     0.9275×0.026 + 0.0725×0]

  = [0.0835, 0.2309, 0.3988, 0.1911, 0.0241]

# 最终效能向量
E = [0.0835, 0.2309, 0.3988, 0.1911, 0.0241]
```

### 5.4 结果解释

效能向量E表示系统在5个评价等级上的隶属度：

| 等级 | 隶属度 | 说明 |
|------|--------|------|
| 优 | 0.0835 | 8.35% |
| 良 | 0.2309 | 23.09% |
| 中 | 0.3988 | 39.88%（最高） |
| 及格 | 0.1911 | 19.11% |
| 差 | 0.0241 | 2.41% |

**综合评价**：系统效能主要集中在"中"等级（39.88%），整体表现为中等偏良。

---

## 六、完整Python实现

```python
import numpy as np
import pandas as pd

class CommunicationADC_Final:
    """
    军事通信效能ADC评估模型（基于舰载通信装备论文）
    
    参考：魏玉人等. 基于ADC的舰载通信装备FAHP效能评估方法研究[J]. 
         舰船电子工程, 2020, 第12期: 110.
    """
    
    def __init__(self, MTBF, MTTR, mission_time=24):
        """
        参数:
            MTBF: 平均故障间隔时间（小时）
            MTTR: 平均修复时间（小时）
            mission_time: 任务时间（小时）
        """
        self.MTBF = MTBF
        self.MTTR = MTTR
        self.t = mission_time
    
    def calculate_availability_matrix(self):
        """
        计算可用度矩阵A（1×2）
        A = [a₁, a₂]
        """
        a1 = self.MTBF / (self.MTBF + self.MTTR)
        a2 = 1 - a1
        
        A = np.array([a1, a2])
        return A
    
    def calculate_dependability_matrix(self, repairable=False):
        """
        计算可信度矩阵D（2×2）
        D = [d₁₁  d₁₂]
            [d₂₁  d₂₂]
        
        参数:
            repairable: 是否可修复
                      False = 不可修复（论文式6，舰艇出海场景）
                      True = 可修复（论文式7-10，基地驻扎场景）
        
        参考：
            孟庆德等. 基于ADC法的舰炮武器系统作战效能评估模型[J]. 
            火炮发射与控制学报, 2015, 36(1).
        """
        λ = 1 / self.MTBF  # 故障率
        
        if not repairable:
            # 不可修复系统（论文式6）
            d11 = np.exp(-λ * self.t)
            d12 = 1 - d11
            d21 = 0
            d22 = 1
        else:
            # 可修复系统（论文式7-10）
            μ = 1 / self.MTTR  # 维修率
            λ_plus_μ = λ + μ
            
            # 稳态概率
            steady_normal = μ / λ_plus_μ
            steady_fault = λ / λ_plus_μ
            
            # 衰减因子
            decay = np.exp(-λ_plus_μ * self.t)
            
            # D矩阵元素（论文式7-10）
            d11 = steady_normal + steady_fault * decay
            d12 = steady_fault * (1 - decay)
            d21 = steady_normal * (1 - decay)
            d22 = steady_fault + steady_normal * decay
        
        D = np.array([
            [d11, d12],
            [d21, d22]
        ])
        return D
    
    def calculate_effectiveness(self, capability_vector, repairable=False):
        """
        计算效能向量E（1×5）
        E = A × D × C
        
        参数:
            capability_vector: 正常状态下的能力向量（1×5）
            repairable: 是否可修复
                      False = 不可修复（默认）
                      True = 可修复
        
        返回:
            E: 效能向量（1×5）
        """
        A = self.calculate_availability_matrix()
        D = self.calculate_dependability_matrix(repairable)
        C = self.calculate_capability_matrix(capability_vector)
        
        # 矩阵运算：E = A × D × C
        # (1×2) × (2×2) × (2×5) = (1×5)
        AD = A @ D  # (1×2) × (2×2) = (1×2)
        E = AD @ C  # (1×2) × (2×5) = (1×5)
        
        return E, A, D, C, AD
    
    def print_results(self, capability_vector, repairable=False):
        """打印详细结果"""
        E, A, D, C, AD = self.calculate_effectiveness(capability_vector, repairable)
        grade, score = self.evaluate_grade(E)
        
        print("=" * 70)
        print("军事通信系统效能评估结果（ADC法 - 基于舰载通信装备论文）")
        print("=" * 70)
        
        print(f"\n系统参数:")
        print(f"  MTBF（平均故障间隔时间）: {self.MTBF}小时")
        print(f"  MTTR（平均修复时间）: {self.MTTR}小时")
        print(f"  任务时间: {self.t}小时")
        print(f"  系统类型: {'可修复' if repairable else '不可修复'}")
        
        if repairable:
            λ = 1 / self.MTBF
            μ = 1 / self.MTTR
            print(f"  故障率λ: {λ:.6f} (1/小时)")
            print(f"  维修率μ: {μ:.6f} (1/小时)")
            print(f"  稳态正常概率: {μ/(λ+μ):.4f}")
        
        print(f"\n可用度矩阵A (1×2):")
        print(f"  A = [{A[0]:.4f}, {A[1]:.4f}]")
        print(f"  说明: a₁={A[0]:.2%}（正常概率）, a₂={A[1]:.2%}（故障概率）")
        
        print(f"\n可信度矩阵D (2×2):")
        print(f"  D = [{D[0,0]:.4f}  {D[0,1]:.4f}]")
        print(f"      [{D[1,0]:.4f}  {D[1,1]:.4f}]")
        print(f"  说明: d₁₁={D[0,0]:.2%}（保持正常）, d₁₂={D[0,1]:.2%}（转为故障）")
        print(f"       d₂₁={D[1,0]:.2%}（修复成功）, d₂₂={D[1,1]:.2%}（保持故障）")
        
        if repairable:
            print(f"  关键: d₂₁={D[1,0]:.2%} 表示故障后有{D[1,0]*100:.2f}%的概率修复成功！")
        
        print(f"\n能力矩阵C (2×5):")
        print(f"  正常状态: [{C[0,0]:.3f}, {C[0,1]:.3f}, {C[0,2]:.3f}, {C[0,3]:.3f}, {C[0,4]:.3f}]")
        print(f"  故障状态: [{C[1,0]:.3f}, {C[1,1]:.3f}, {C[1,2]:.3f}, {C[1,3]:.3f}, {C[1,4]:.3f}]")
        
        print(f"\n中间结果 A×D (1×2):")
        print(f"  A×D = [{AD[0]:.4f}, {AD[1]:.4f}]")
        print(f"  说明: 任务结束时，{AD[0]*100:.2f}%正常，{AD[1]*100:.2f}%故障")
        
        print(f"\n{'='*70}")
        print(f"效能向量E (1×5):")
        print(f"  E = [{E[0]:.4f}, {E[1]:.4f}, {E[2]:.4f}, {E[3]:.4f}, {E[4]:.4f}]")
        print(f"\n隶属度分布:")
        grades = ['优', '良', '中', '及格', '差']
        for i, (g, e) in enumerate(zip(grades, E)):
            bar = '█' * int(e * 100)
            print(f"  {g:4s}: {e:.4f} ({e*100:5.2f}%) {bar}")
        
        print(f"\n{'='*70}")
        print(f"综合评价: {grade}（综合得分: {score:.4f}）")
        print(f"{'='*70}")


# 使用示例
if __name__ == "__main__":
    # 系统参数
    MTBF = 1000  # 平均故障间隔时间1000小时
    MTTR = 2     # 平均修复时间2小时
    
    # 创建模型
    model = CommunicationADC_Final(MTBF, MTTR, mission_time=24)
    
    # 能力向量（来自模糊评判或指标计算）
    capability_vector = [0.090, 0.249, 0.430, 0.206, 0.026]
    
    print("场景1：不可修复系统（舰艇出海）")
    print("-" * 70)
    model.print_results(capability_vector, repairable=False)
    
    print("\n\n")
    
    print("场景2：可修复系统（基地驻扎）")
    print("-" * 70)
    model.print_results(capability_vector, repairable=True)
    
    print("\n\n对比分析：")
    print("=" * 70)
    E1, _, D1, _, _ = model.calculate_effectiveness(capability_vector, repairable=False)
    E2, _, D2, _, _ = model.calculate_effectiveness(capability_vector, repairable=True)
    
    score1 = np.sum(E1 * [1.0, 0.8, 0.6, 0.4, 0.2])
    score2 = np.sum(E2 * [1.0, 0.8, 0.6, 0.4, 0.2])
    
    print(f"不可修复系统: d₂₁={D1[1,0]:.4f}, 综合得分={score1:.4f}")
    print(f"可修复系统:   d₂₁={D2[1,0]:.4f}, 综合得分={score2:.4f}")
    print(f"效能提升:     {(score2-score1)/score1*100:.2f}%")
    print("=" * 70)
```

---

## 七、与数据库集成

### 7.1 从数据库计算能力向量

```python
def calculate_capability_from_db(df_battle):
    """
    从战时通信数据计算能力向量
    
    参数:
        df_battle: during_battle_communications表的数据
    
    返回:
        capability_vector: 能力向量（1×5）
    """
    # 方法1：基于综合得分分布
    # 计算各项指标的综合得分
    scores = []
    for _, row in df_battle.iterrows():
        # 归一化各项指标
        ber_score = 1 - row['instant_ber']
        plr_score = 1 - row['instant_plr']
        sinr_score = row['instant_sinr'] / 30  # 假设最大30dB
        throughput_score = row['instant_throughput'] / 100  # 假设最大100Mbps
        
        # 综合得分
        score = (ber_score + plr_score + sinr_score + throughput_score) / 4
        scores.append(score)
    
    # 统计得分分布
    scores = np.array(scores)
    capability_vector = [
        np.sum((scores >= 0.9) & (scores <= 1.0)) / len(scores),  # 优
        np.sum((scores >= 0.8) & (scores < 0.9)) / len(scores),   # 良
        np.sum((scores >= 0.6) & (scores < 0.8)) / len(scores),   # 中
        np.sum((scores >= 0.4) & (scores < 0.6)) / len(scores),   # 及格
        np.sum((scores >= 0.0) & (scores < 0.4)) / len(scores)    # 差
    ]
    
    return capability_vector


# 方法2：基于通信成功率
def calculate_capability_simple(success_rate):
    """
    基于通信成功率简化计算能力向量
    
    参数:
        success_rate: 通信成功率（0-1）
    
    返回:
        capability_vector: 能力向量（1×5）
    """
    if success_rate >= 0.95:
        return [0.7, 0.3, 0.0, 0.0, 0.0]  # 主要为优
    elif success_rate >= 0.85:
        return [0.2, 0.6, 0.2, 0.0, 0.0]  # 主要为良
    elif success_rate >= 0.70:
        return [0.0, 0.3, 0.5, 0.2, 0.0]  # 主要为中
    elif success_rate >= 0.50:
        return [0.0, 0.0, 0.3, 0.5, 0.2]  # 主要为及格
    else:
        return [0.0, 0.0, 0.0, 0.3, 0.7]  # 主要为差
```

### 7.2 完整评估流程

```python
import sqlite3

class CommunicationEvaluator:
    """完整的通信效能评估系统"""
    
    def __init__(self, db_path):
        self.db_path = db_path
        self.conn = sqlite3.connect(db_path)
    
    def evaluate_batch(self, test_batch_id):
        """评估指定测试批次"""
        # 1. 加载数据
        lifecycle_df = pd.read_sql(
            f"SELECT * FROM communication_network_lifecycle WHERE test_batch_id={test_batch_id}",
            self.conn
        )
        battle_df = pd.read_sql(
            f"SELECT * FROM during_battle_communications WHERE test_batch_id={test_batch_id}",
            self.conn
        )
        
        # 2. 计算MTBF和MTTR
        total_time = lifecycle_df['total_lifecycle_duration_ms'].sum() / 3600000
        failure_count = lifecycle_df['network_crash_occurred'].sum()
        MTBF = total_time / failure_count if failure_count > 0 else 1000
        
        crash_df = lifecycle_df[lifecycle_df['network_crash_occurred'] == 1]
        MTTR = crash_df['total_interruption_duration_ms'].mean() / 3600000 if len(crash_df) > 0 else 30
        
        # 3. 计算能力向量
        success_rate = battle_df['communication_success'].mean()
        capability_vector = calculate_capability_simple(success_rate)
        
        # 4. ADC评估
        model = CommunicationADC_Final(MTBF, MTTR, mission_time=24)
        E, A, D, C, AD = model.calculate_effectiveness(capability_vector)
        grade, score = model.evaluate_grade(E)
        
        return {
            'batch_id': test_batch_id,
            'MTBF': MTBF,
            'MTTR': MTTR,
            'success_rate': success_rate,
            'E': E,
            'grade': grade,
            'score': score
        }
    
    def compare_batches(self, batch_ids):
        """比较多个批次"""
        results = []
        for batch_id in batch_ids:
            result = self.evaluate_batch(batch_id)
            results.append(result)
        
        df = pd.DataFrame(results)
        return df.sort_values('score', ascending=False)


# 使用示例
evaluator = CommunicationEvaluator('military_communication_effectiveness.db')
comparison = evaluator.compare_batches([1, 2, 3, 4, 5])
print(comparison)
```

---

## 八、总结

### 8.1 方案特点

✅ **完全基于论文**：
- 2种状态（正常/故障）
- A是1×2向量
- D是2×2矩阵
- C是2×5矩阵
- E是1×5效能向量

✅ **计算清晰**：
- E = A × D × C
- 矩阵维度：(1×2) × (2×2) × (2×5) = (1×5)

✅ **结果直观**：
- 效能向量E表示在5个等级上的隶属度
- 可以直接看出系统主要处于哪个等级

### 8.2 关键公式

```python
# 可用度矩阵A (1×2)
A = [MTBF/(MTBF+MTTR), 1-MTBF/(MTBF+MTTR)]

# 可信度矩阵D (2×2)
D = [exp(-t/MTBF),  1-exp(-t/MTBF)]
    [0,             1              ]

# 能力矩阵C (2×5)
C = [b₁, b₂, b₃, b₄, b₅]  # 来自模糊评判或指标计算
    [0,  0,  0,  0,  0 ]

# 效能向量E (1×5)
E = A × D × C
```

### 8.3 优势

1. **理论严谨**：完全遵循论文方法
2. **结果丰富**：得到5个等级的隶属度分布
3. **易于比较**：可以直观对比不同批次
4. **灵活性强**：能力向量可以来自多种方法

---

**参考文献**：
魏玉人等. 基于ADC的舰载通信装备FAHP效能评估方法研究[J]. 舰船电子工程, 2020, 第12期: 110.
