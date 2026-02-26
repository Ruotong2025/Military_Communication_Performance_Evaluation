# 能力矩阵C的详细解释（通俗易懂版）

## 一、矩阵C到底是什么？

### 1.1 核心概念

**矩阵C = 系统在不同状态下的"能力得分表"**

想象一下：
- 你的手机电量100%时，能力最强（打电话、上网、玩游戏都行）
- 电量50%时，能力下降（只能打电话、上网）
- 电量10%时，能力很弱（只能打电话）
- 电量0%时，完全没能力（什么都做不了）

**通信系统也是一样的！**

---

## 二、两种状态的矩阵C（2×5矩阵）

### 2.1 矩阵结构

```
       评价等级
         ↓
C = [b₁  b₂  b₃  b₄  b₅]  ← 状态1：系统正常
    [0   0   0   0   0 ]  ← 状态2：系统故障
     ↑   ↑   ↑   ↑   ↑
    优  良  中  及格 差
```

### 2.2 物理意义

**第1行（正常状态）**：
- b₁ = 系统正常时，达到"优"等级的概率
- b₂ = 系统正常时，达到"良"等级的概率
- b₃ = 系统正常时，达到"中"等级的概率
- b₄ = 系统正常时，达到"及格"等级的概率
- b₅ = 系统正常时，达到"差"等级的概率

**约束**：b₁ + b₂ + b₃ + b₄ + b₅ = 1（概率和为1）

**第2行（故障状态）**：
- 全部为0，因为系统故障了，没有任何能力

### 2.3 实际例子

**例子1：优秀的通信系统**
```python
C = [0.7  0.3  0.0  0.0  0.0]  # 正常时：70%优，30%良
    [0    0    0    0    0  ]  # 故障时：完全没能力
```
**解释**：
- 系统正常时，有70%的概率表现"优秀"，30%的概率表现"良好"
- 系统故障时，什么都做不了

**例子2：一般的通信系统**
```python
C = [0.1  0.3  0.5  0.1  0.0]  # 正常时：10%优，30%良，50%中，10%及格
    [0    0    0    0    0  ]  # 故障时：完全没能力
```
**解释**：
- 系统正常时，表现主要集中在"中"等级（50%）
- 也有一些"良好"（30%）和"优秀"（10%）的表现

**例子3：较差的通信系统**
```python
C = [0.0  0.2  0.3  0.4  0.1]  # 正常时：20%良，30%中，40%及格，10%差
    [0    0    0    0    0  ]  # 故障时：完全没能力
```
**解释**：
- 系统正常时，表现主要集中在"及格"等级（40%）
- 甚至有10%的时候表现"差"

### 2.4 如何得到这些数字？

#### 方法1：从数据库统计（推荐）

```python
# 从 during_battle_communications 表统计
def calculate_capability_from_data(df):
    """
    从实际数据计算能力向量
    """
    # 计算综合得分
    scores = []
    for _, row in df.iterrows():
        # 各项指标归一化
        ber_score = 1 - row['instant_ber']  # 误码率越低越好
        plr_score = 1 - row['instant_plr']  # 丢包率越低越好
        sinr_score = row['instant_sinr'] / 30  # 信噪比越高越好（假设最大30dB）
        throughput_score = row['instant_throughput'] / 100  # 吞吐量越高越好
        
        # 综合得分（简单平均）
        score = (ber_score + plr_score + sinr_score + throughput_score) / 4
        scores.append(score)
    
    # 统计得分分布
    scores = np.array(scores)
    b1 = np.sum((scores >= 0.9) & (scores <= 1.0)) / len(scores)  # 优：90-100分
    b2 = np.sum((scores >= 0.8) & (scores < 0.9)) / len(scores)   # 良：80-90分
    b3 = np.sum((scores >= 0.6) & (scores < 0.8)) / len(scores)   # 中：60-80分
    b4 = np.sum((scores >= 0.4) & (scores < 0.6)) / len(scores)   # 及格：40-60分
    b5 = np.sum((scores >= 0.0) & (scores < 0.4)) / len(scores)   # 差：0-40分
    
    return [b1, b2, b3, b4, b5]

# 示例
# 假设有100次通信记录
# 其中：10次优秀，30次良好，50次中等，10次及格，0次差
# 结果：[0.1, 0.3, 0.5, 0.1, 0.0]
```

#### 方法2：基于通信成功率（简化）

```python
def calculate_capability_simple(success_rate):
    """
    基于通信成功率简化计算
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

# 示例
success_rate = 0.88  # 通信成功率88%
C_row1 = calculate_capability_simple(0.88)
# 结果：[0.2, 0.6, 0.2, 0.0, 0.0]
```

#### 方法3：专家打分

```python
# 请5位专家对系统能力打分（0-100分）
expert_scores = [85, 90, 80, 88, 87]  # 5位专家的评分

# 计算平均分
avg_score = np.mean(expert_scores) / 100  # 转为0-1之间

# 根据平均分映射到能力向量
if avg_score >= 0.9:
    C_row1 = [0.7, 0.3, 0.0, 0.0, 0.0]
elif avg_score >= 0.8:
    C_row1 = [0.2, 0.6, 0.2, 0.0, 0.0]
# ... 以此类推
```

---

## 三、完整的计算示例

### 3.1 场景设定

假设你的通信系统：
- MTBF = 1000小时
- MTTR = 2小时
- 任务时间 = 24小时
- 通信成功率 = 88%

### 3.2 步骤1：计算A（可用性）

```python
A = [a₁, a₂]

a₁ = MTBF / (MTBF + MTTR) = 1000 / 1002 = 0.998  # 正常概率
a₂ = 1 - a₁ = 0.002  # 故障概率

A = [0.998, 0.002]
```

### 3.3 步骤2：计算D（可信性）

```python
# 可修复系统
λ = 1/1000 = 0.001
μ = 1/2 = 0.5
T = 24

D = [0.998  0.002]
    [0.998  0.002]
```

### 3.4 步骤3：计算C（能力）

```python
# 基于通信成功率88%
success_rate = 0.88

# 使用简化方法
C_row1 = [0.2, 0.6, 0.2, 0.0, 0.0]  # 主要为良（60%）

C = [0.2  0.6  0.2  0.0  0.0]  # 正常状态
    [0    0    0    0    0  ]  # 故障状态
```

**物理意义**：
- 系统正常时：20%概率表现优秀，60%概率表现良好，20%概率表现中等
- 系统故障时：完全没能力

### 3.5 步骤4：计算E（效能）

```python
E = A × D × C

# 详细计算
# 步骤1：A × D
AD = [0.998, 0.002] × [0.998  0.002]
                      [0.998  0.002]
   = [0.998×0.998 + 0.002×0.998,  0.998×0.002 + 0.002×0.002]
   = [0.998,  0.002]

# 步骤2：(A × D) × C
E = [0.998, 0.002] × [0.2  0.6  0.2  0.0  0.0]
                      [0    0    0    0    0  ]
  = [0.998×0.2 + 0.002×0,
     0.998×0.6 + 0.002×0,
     0.998×0.2 + 0.002×0,
     0.998×0.0 + 0.002×0,
     0.998×0.0 + 0.002×0]
  = [0.1996, 0.5988, 0.1996, 0, 0]

# 最终效能向量
E = [0.1996, 0.5988, 0.1996, 0, 0]
```

**结果解释**：
- 优：19.96%
- 良：59.88%（最高）
- 中：19.96%
- 及格：0%
- 差：0%

**综合评价**：系统效能主要集中在"良好"等级

---

## 四、直观理解矩阵C

### 4.1 用表格理解

| 系统状态 | 优(90-100分) | 良(80-90分) | 中(60-80分) | 及格(40-60分) | 差(0-40分) |
|---------|-------------|------------|------------|-------------|-----------|
| 正常 | 20% | 60% | 20% | 0% | 0% |
| 故障 | 0% | 0% | 0% | 0% | 0% |

### 4.2 用图形理解

```
正常状态的能力分布：

优  ████ (20%)
良  ████████████ (60%)  ← 最多
中  ████ (20%)
及格 (0%)
差  (0%)

故障状态的能力分布：
全部为0（没有任何能力）
```

### 4.3 用实际场景理解

**场景1：打电话**
- 正常时：60%的通话质量很好（良），20%非常好（优），20%一般（中）
- 故障时：完全打不了电话

**场景2：传输数据**
- 正常时：60%的时候速度快（良），20%的时候非常快（优），20%的时候一般（中）
- 故障时：完全传不了数据

---

## 五、完整Python代码示例

```python
import numpy as np

class SimpleCommunicationADC:
    """简化的通信系统ADC评估（2状态版本）"""
    
    def __init__(self, MTBF, MTTR, mission_time=24):
        self.MTBF = MTBF
        self.MTTR = MTTR
        self.t = mission_time
    
    def calculate_A(self):
        """计算可用性A"""
        a1 = self.MTBF / (self.MTBF + self.MTTR)
        a2 = 1 - a1
        return np.array([a1, a2])
    
    def calculate_D(self, repairable=True):
        """计算可信性D"""
        λ = 1 / self.MTBF
        
        if not repairable:
            d11 = np.exp(-λ * self.t)
            d12 = 1 - d11
            d21 = 0
            d22 = 1
        else:
            μ = 1 / self.MTTR
            λ_plus_μ = λ + μ
            steady_normal = μ / λ_plus_μ
            steady_fault = λ / λ_plus_μ
            decay = np.exp(-λ_plus_μ * self.t)
            
            d11 = steady_normal + steady_fault * decay
            d12 = steady_fault * (1 - decay)
            d21 = steady_normal * (1 - decay)
            d22 = steady_fault + steady_normal * decay
        
        return np.array([[d11, d12], [d21, d22]])
    
    def calculate_C_from_success_rate(self, success_rate):
        """
        从通信成功率计算能力矩阵C
        
        参数:
            success_rate: 通信成功率（0-1）
        
        返回:
            C: 能力矩阵（2×5）
        """
        # 根据成功率映射到能力分布
        if success_rate >= 0.95:
            row1 = [0.7, 0.3, 0.0, 0.0, 0.0]  # 主要为优
        elif success_rate >= 0.85:
            row1 = [0.2, 0.6, 0.2, 0.0, 0.0]  # 主要为良
        elif success_rate >= 0.70:
            row1 = [0.0, 0.3, 0.5, 0.2, 0.0]  # 主要为中
        elif success_rate >= 0.50:
            row1 = [0.0, 0.0, 0.3, 0.5, 0.2]  # 主要为及格
        else:
            row1 = [0.0, 0.0, 0.0, 0.3, 0.7]  # 主要为差
        
        # 故障状态能力为0
        row2 = [0, 0, 0, 0, 0]
        
        return np.array([row1, row2])
    
    def calculate_effectiveness(self, success_rate, repairable=True):
        """
        计算效能E
        
        参数:
            success_rate: 通信成功率
            repairable: 是否可修复
        
        返回:
            E: 效能向量（1×5）
        """
        A = self.calculate_A()
        D = self.calculate_D(repairable)
        C = self.calculate_C_from_success_rate(success_rate)
        
        # E = A × D × C
        AD = A @ D  # (1×2) × (2×2) = (1×2)
        E = AD @ C  # (1×2) × (2×5) = (1×5)
        
        return E, A, D, C
    
    def print_results(self, success_rate, repairable=True):
        """打印详细结果"""
        E, A, D, C = self.calculate_effectiveness(success_rate, repairable)
        
        print("=" * 70)
        print("军事通信系统效能评估结果（简化版）")
        print("=" * 70)
        
        print(f"\n输入参数:")
        print(f"  MTBF: {self.MTBF}小时")
        print(f"  MTTR: {self.MTTR}小时")
        print(f"  任务时间: {self.t}小时")
        print(f"  通信成功率: {success_rate*100:.1f}%")
        print(f"  系统类型: {'可修复' if repairable else '不可修复'}")
        
        print(f"\n可用性A:")
        print(f"  正常概率: {A[0]:.4f} ({A[0]*100:.2f}%)")
        print(f"  故障概率: {A[1]:.4f} ({A[1]*100:.2f}%)")
        
        print(f"\n可信性D:")
        print(f"  D = [{D[0,0]:.4f}  {D[0,1]:.4f}]")
        print(f"      [{D[1,0]:.4f}  {D[1,1]:.4f}]")
        
        print(f"\n能力矩阵C:")
        print(f"  正常状态: {C[0]}")
        print(f"  故障状态: {C[1]}")
        print(f"\n  解释：")
        print(f"    系统正常时：")
        print(f"      - {C[0][0]*100:.1f}% 概率表现优秀")
        print(f"      - {C[0][1]*100:.1f}% 概率表现良好")
        print(f"      - {C[0][2]*100:.1f}% 概率表现中等")
        print(f"      - {C[0][3]*100:.1f}% 概率表现及格")
        print(f"      - {C[0][4]*100:.1f}% 概率表现较差")
        
        print(f"\n效能向量E:")
        print(f"  E = {E}")
        print(f"\n  最终评估：")
        grades = ['优', '良', '中', '及格', '差']
        for i, (grade, prob) in enumerate(zip(grades, E)):
            bar = '█' * int(prob * 100)
            print(f"    {grade:4s}: {prob:.4f} ({prob*100:5.2f}%) {bar}")
        
        # 综合得分
        weights = [1.0, 0.8, 0.6, 0.4, 0.2]
        score = np.sum(E * weights)
        max_idx = np.argmax(E)
        main_grade = grades[max_idx]
        
        print(f"\n{'='*70}")
        print(f"综合评价: {main_grade}（综合得分: {score:.4f}）")
        print(f"{'='*70}")


# 使用示例
if __name__ == "__main__":
    # 创建模型
    model = SimpleCommunicationADC(MTBF=1000, MTTR=2, mission_time=24)
    
    # 场景1：通信成功率88%
    print("场景1：通信成功率88%")
    model.print_results(success_rate=0.88, repairable=True)
    
    print("\n\n")
    
    # 场景2：通信成功率95%
    print("场景2：通信成功率95%")
    model.print_results(success_rate=0.95, repairable=True)
```

---

## 六、总结

### 6.1 矩阵C的本质

```
C = 系统在不同状态下的"能力得分分布表"

对于2状态系统：
C = [正常状态的能力分布]  ← 5个数字，表示在5个等级上的概率
    [故障状态的能力分布]  ← 全是0，因为没能力
```

### 6.2 如何得到C

```
方法1：从数据库统计（最准确）
  → 统计实际通信记录的得分分布

方法2：基于成功率映射（简单）
  → 根据通信成功率，映射到预定义的分布

方法3：专家打分（主观）
  → 请专家评估系统能力
```

### 6.3 C在ADC中的作用

```
E = A × D × C

A: 告诉我们系统初始是正常还是故障
D: 告诉我们系统在任务期间会不会变化
C: 告诉我们系统在各状态下的能力如何

最终E: 综合考虑所有因素，得到系统的整体效能
```

希望这个解释能让你完全理解矩阵C的含义和计算方法！
