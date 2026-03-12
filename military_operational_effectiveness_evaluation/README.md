# 军事通信效能评估 - 专家可信度与AHP权重计算方案

## 一、方案概述

本方案实现了基于**专家可信度评估**与**AHP层次分析法**的军事通信效能指标权重计算系统。核心目标是通过评估专家的主观可信度、客观可信度和把握度，对专家给出的指标权重进行科学修正。

**特别说明**：当某些指标出现"全员无把握"（所有专家的把握度都 < 0.6）时，使用**熵权法**作为备用方案，基于 `equipment_operation_score` 表中的实验数据离散程度来确定权重。

---

## 二、数据输入

| 数据表 | 内容 |
|--------|------|
| `expert_credibility_evaluation_score` | 10位专家的背景数据（职称、职务、学历、专业知识等） |
| `ahp_expert_military_operation_effect_weights` | 10位专家 × 20个二级指标 的评分 + 把握度 |
| `equipment_operation_score` | 10个实验 × 21个评分维度的操作评分数据（熵权法数据源） |

---

## 三、第一步：专家可信度评估

### 3.1 主观可信度 αi（基于专家背景）

**计算公式：**

$$\alpha_i = \gamma_1 \cdot \Omega_e + \gamma_2 \cdot \Omega_k$$

其中：

- **Ωe（影响力得分）：**
  
  $$\Omega_e = \sum_{m=1}^{6} \omega_m \cdot F_m$$
  
  | 因素 | 权重 ωm | 评分字段 |
  |------|---------|----------|
  | 职称 | 0.20 | title_ql |
  | 职务 | 0.15 | position_ql |
  | 学历经历 | 0.15 | education_experience_ql |
  | 学术成果 | 0.20 | academic_achievements_ql |
  | 科研成果 | 0.20 | research_achievements_ql |
  | 演习训练 | 0.10 | exercise_experience_ql |

- **Ωk（专业知识得分）：**
  
  $$\Omega_k = \sum_{m=1}^{4} \beta_m \cdot F_m$$

  | 因素 | 权重 βm | 评分字段 |
  |------|---------|----------|
  | 军事训练学知识 | 0.30 | military_training_knowledge_ql |
  | 系统仿真知识 | 0.25 | system_simulation_knowledge_ql |
  | 数理统计学知识 | 0.20 | statistics_knowledge_ql |
  | 专业年限 | 0.25 | professional_years_qt |

- **γ1 = γ2 = 0.5**（主观综合权重各占一半）

**最终归一化：**

$$\alpha_i = \frac{\Omega_e + \Omega_k}{100}$$

---

### 3.2 客观可信度 βi（基于评分一致性）

**步骤1：计算专家个人CV（变异系数）**

首先对每条评分进行**把握度过滤**：

```
if confidence_ij < 0.6:
    该评分标记为"无效"
else:
    该评分标记为"有效"
```

然后计算专家i的评分偏差：

$$\text{CV}_i = \frac{\sqrt{\frac{1}{n-1}\sum_{j=1}^{n}(w_{ij} - \bar{w}_j)^2}}{\bar{w}} \times 100\%$$

其中：
- $w_{ij}$：专家i对指标j的有效评分（把握度≥0.6）
- $\bar{w}_j$：指标j的有效评分均值
- $\bar{w}$：所有有效评分的整体均值

**步骤2：基于CV计算βi（分段衰减）：**

$$\beta_i = f(\text{CV}_i) = \begin{cases}
1.0 & \text{CV} \leq 15\% \\
1.0 - \dfrac{\text{CV} - 15}{10} \times 0.5 & 15\% < \text{CV} \leq 25\% \\
0.5 - \dfrac{\text{CV} - 25}{10} \times 0.5 & 25\% < \text{CV} \leq 35\% \\
0 & \text{CV} > 35\%
\end{cases}$$

---

### 3.3 综合可信度（最终可信度）

$$\text{综合可信度}_i = \lambda \cdot \alpha_i + (1-\lambda) \cdot \beta_i$$

其中 **λ = 0.6**（主观权重），**1-λ = 0.4**（客观权重）

---

## 四、第二步：指标CV计算

### 4.1 单指标CV计算

对每个二级指标 $j$：

**有效评分集合：**

$$S_j = \{ w_{ij} \mid \text{confidence}_{ij} \geq 0.6 \}$$

**CV计算：**

$$
\text{CV}_j = \begin{cases}
\text{NaN} & |S_j| = 0 \text{（全员无把握）} \\
0 & |S_j| = 1 \text{（仅1条有效评分）} \\
\dfrac{\text{std}(S_j)}{\text{mean}(S_j)} \times 100\% & |S_j| \geq 2
\end{cases}
$$

### 4.2 异常值删除（可选步骤）

> 当 $|S_j| \geq 3$ 时，先删除离均值最远的1条评分，再计算CV。

---

## 五、第三步：AHP权重计算（核心）

### 5.1 计算每条评分的"调整因子"

对专家 $i$ 在指标 $j$ 的评分 $w_{ij}$，计算**调整因子** $q_{ij}$：

$$q_{ij} = 0.5 \times (0.6\alpha_i + 0.4\beta_i) + 0.5 \times \text{confidence}_{ij}$$

其中：
- **αi**：专家 i 的主观可信度（来自第二步）
- **βi**：专家 i 的客观可信度（来自第二步）
- **confidenceij**：专家 i 对指标 j 的把握度
- **专家综合可信度** = 0.6 × αi + 0.4 × βi

> 调整因子权重分配：
> - 把握度：0.5（直接反映该条评分的可靠程度）
> - 专家综合可信度：0.5（综合考虑专家主观和客观可信度）
> - 其中专家综合可信度 = 0.6×主观可信度 + 0.4×客观可信度

---

### 5.2 把握度过滤（单条评分级别）

对每条评分 $w_{ij}$：

```
if confidence_ij < 0.6:
    该评分不参与后续任何计算
else:
    参与AHP计算
```

---

### 5.3 构建加权判断矩阵

对一级指标 $k$ 下的二级指标集合 $J = \{j_1, j_2, ..., j_n\}$：

**构造 $n \times n$ 判断矩阵 $A$：**

```
for i in J:
    for j in J:
        if i == j:
            A[i,j] = 1
        else:
            # 找出同时对两个指标都有把握(>=0.6)的专家
            有效专家集合 = { e | confidence_ei >= 0.6 AND confidence_ej >= 0.6 }

            if 有效专家集合为空:
                A[i,j] = 1
            else:
                # 【核心】每条评分乘以调整因子 q_e
                加权比值列表 = []
                for e in 有效专家集合:
                    # 评分 × 调整因子 = 加权评分
                    加权评分_i = w_ei × q_ei
                    加权评分_j = w_ej × q_ej
                    比值 = 加权评分_i / 加权评分_j
                    加权比值列表.append(比值)

                # 【新增】离群值删除：当样本数≥3时，删除离平均值最远的1个值
                if len(加权比值列表) >= 3:
                    均值 = mean(加权比值列表)
                    距离列表 = [abs(比值 - 均值) for 比值 in 加权比值列表]
                    删除索引 = 距离列表.index(max(距离列表))
                    del 加权比值列表[删除索引]

                # 几何平均
                A[i,j] = \left( \prod_{k=1}^{m} \text{比值}_k \right)^{\frac{1}{m}}

            # 限制在1/9 ~ 9之间
            A[i,j] = max(1/9, min(9, A[i,j]))
```

**公式表达：**

$$A_{ij} = \left( \prod_{e \in E_{ij}} \frac{w_{ei} \cdot q_{ei}}{w_{ej} \cdot q_{ej}} \right)^{\frac{1}{|E_{ij}|}}$$

其中 $E_{ij}$ 表示同时对指标 $i$ 和 $j$ 都有把握（confidence ≥ 0.6）的专家集合。

> **离群值删除规则**：当有效比值数量 ≥ 3 时，删除离均值最远的 1 个专家评分，避免极端值影响判断矩阵。

---

### 5.4 一致性检验

**计算最大特征值 λmax：**

$$A \cdot w = \lambda_{\text{max}} \cdot w$$

**一致性指标：**

$$\text{CI} = \frac{\lambda_{\text{max}} - n}{n - 1}$$

**一致性比例：**

$$\text{CR} = \frac{\text{CI}}{\text{RI}}$$

其中 RI 为随机一致性指标：

| n | RI |
|---|-----|
| 1 | 0 |
| 2 | 0 |
| 3 | 0.58 |
| 4 | 0.90 |
| 5 | 1.12 |
| 6 | 1.24 |
| 7 | 1.32 |
| 8 | 1.41 |
| 9 | 1.45 |
| 10 | 1.49 |

**判断标准：**

$$\text{CR} \leq 0.1 \quad \text{通过一致性检验}$$

---

### 5.5 权重计算（几何平均法）

$$w_i = \left( \prod_{k=1}^{n} A_{ik} \right)^{\frac{1}{n}}$$

**归一化：**

$$w_i = \frac{w_i}{\sum_{k=1}^{n} w_k}$$

**二级权重（在各自一级指标内归一化和为1）：**

$$\text{secondary\_weights}[j] = w_j$$

---

## 六、第四步：处理"全员无把握"指标

### 6.1 判定标准

如果某二级指标的所有专家评分都满足 $\text{confidence}_{ij} < 0.6$，则该指标判定为**全员无把握**。

### 6.2 熵权法备用方案（基于 equipment_operation_score）

当指标被判定为"全员无把握"时，使用**熵权法**基于 `equipment_operation_score` 表的数据来确定权重。

#### 6.2.1 数据映射

将AHP的20个二级指标与 `equipment_operation_score` 的21个评分维度进行映射：

| AHP二级指标 | 对应 equipment_operation_score 字段 |
|-------------|-------------------------------------|
| 密钥泄露 | maintenance_maintenance_skill_ql |
| 被侦察概率 | comm_defense_signal_interception_awareness_ql |
| 抗拦截能力 | comm_defense_anti_deception_awareness_ql |
| 误码率 | system_performance_connectivity_rate_qt |
| 恢复能力 | comm_support_emergency_restoration_qt |
| 通信可靠性 | system_performance_mission_reliability_qt |
| 带宽 | comm_attack_jamming_effectiveness_ql |
| 完成建链时间 | system_setup_network_setup_time_qt |
| 通信时延 | comm_support_link_maintenance_qt |
| 数据率 | comm_attack_target_acquisition_qt |
| 吞吐量 | comm_support_service_activation_qt |
| 频谱效率 | comm_attack_deception_signal_generation_ql |
| 信干噪比 | comm_defense_anti_jamming_operation_ql |
| 抗多径干扰 | response_emergency_handling_qt |
| 通信距离 | personnel_work_experience_qt |
| 功耗 | maintenance_spare_parts_availability_qt |
| 人力需求 | personnel_personnel_count_qt |
| 灵活性 | personnel_training_experience_qt |
| 任务完成率 | maintenance_feedback_field_repair_qt |
| 效费比 | maintenance_feedback_equipment_feedback_ql |

> 注：返工率(rework_rate)为备用字段，可用于某些特殊指标的映射。

#### 6.2.2 熵权法计算步骤

**步骤1：构建数据矩阵**

从 `equipment_operation_score` 表中提取10个实验的评分数据，构建矩阵：

$$X = (x_{ij})_{10 \times 21}$$

其中 $x_{ij}$ 表示第 $i$ 个实验对第 $j$ 个评分维度的评分。

**步骤2：归一化处理**

根据指标类型进行归一化：

- **效益型指标（越大越好）：**
  
  $$r_{ij} = \frac{x_{ij} - \min(x_j)}{\max(x_j) - \min(x_j)}$$

- **成本型指标（越小越好）：**
  
  $$r_{ij} = \frac{\max(x_j) - x_{ij}}{\max(x_j) - \min(x_j)}$$

**步骤3：计算信息熵**

对于第 $j$ 个指标：

$$p_{ij} = \frac{r_{ij}}{\sum_{i=1}^{10} r_{ij}}$$

$$E_j = -\frac{1}{\ln 10} \sum_{i=1}^{10} p_{ij} \ln p_{ij}$$

> 注意：当 $p_{ij} = 0$ 时，定义 $p_{ij} \ln p_{ij} = 0$

**步骤4：计算权重**

$$w_j = \frac{1 - E_j}{\sum_{j=1}^{21}(1 - E_j)}$$

> **核心思想**：数据分布越分散（变异程度越大）的指标，其信息熵越小，权重越大。

#### 6.2.3 指标映射与权重提取

1. 根据需要使用熵权法的AHP指标，找到对应的 `equipment_operation_score` 字段
2. 提取该字段在10个实验中的评分数据
3. 按照上述熵权法步骤计算权重
4. 将计算得到的权重作为该AHP指标的最终权重

---

## 七、第五步：最终权重合成

### 7.1 合成公式

$$\text{最终权重}_j = \text{二级权重}_j \times \text{一级权重}_k$$

其中一级权重为预先设定的固定值：

| 一级指标 | 一级权重 |
|----------|---------|
| 安全性 | 0.25 |
| 可靠性 | 0.20 |
| 传输性能 | 0.20 |
| 抗干扰 | 0.15 |
| 资源消耗 | 0.10 |
| 作战效能 | 0.10 |

### 7.2 全局归一化

$$\text{最终权重}_{j,\text{归一化}} = \frac{\text{最终权重}_j}{\sum_{j=1}^{20} \text{最终权重}_j}$$

确保所有二级指标权重之和 **= 1**。

---

## 八、完整流程汇总图

```
┌─────────────────────────────────────────┐
│           输入数据                       │
│  专家背景 + 评分 + 把握度               │
│  equipment_operation_score (熵权法备用) │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│  步骤1：计算专家可信度                   │
│  ┌───────────────────────────────────┐  │
│  │ ① 主观可信度 αi（背景数据）      │  │
│  │    αi = (Ωe + Ωk) / 100         │  │
│  │                                   │  │
│  │ ② 客观可信度 βi（CV分段衰减）    │  │
│  │    βi = f(CVi)                  │  │
│  │                                   │  │
│  │ ③ 综合可信度 = 0.6α + 0.4β      │  │
│  └───────────────────────────────────┘  │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│  步骤2：计算指标CV（有效评分）          │
│  识别全员无把握指标                      │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│  步骤3：计算每条评分的调整因子           │
│  q_ij = 0.5*(0.6α_i+0.4β_i) + 0.5*conf │
│  步骤4：删除离群值（≥3样本时删除最远值）│
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│  步骤4：AHP权重计算                     │
│  ┌───────────────────────────────────┐  │
│  │ ① 把握度过滤（<0.6剔除 离散值）        │  │
│  │                                   │  │
│  │ ② 构建判断矩阵：                 │  │
│  │    加权评分 = 评分 × q_ij       │  │
│  │    比值 = 加权评分_i / 加权_j   │  │
│  │    几何平均得 A_ij              │  │
│  │                                   │  │
│  │  ③ 一致性检验（CR≤0.1）        │  │
│  │                                   │  │
│  │  ④ 几何平均法求权重             │  │
│  └───────────────────────────────────┘  │
└────────────────┬────────────────────────┘
                 │
      ┌──────────┴──────────┐
      ▼                     ▼
┌─────────────┐      ┌─────────────────┐
│ 正常指标    │      │ 全员无把握指标  │
│ → AHP权重   │      │ → 熵权法        │
│             │      │ (基于equipment  │
│             │      │  _operation_    │
│             │      │  score离散度)   │
└─────────────┘      └─────────────────┘
      │                     │
      └────────┬────────────┘
               ▼
      ┌──────────────────┐
      │  步骤5：最终权重 │
      │  = 二级×一级     │
      │  全局归一化 = 1  │
      └──────────────────┘
```

---

## 九、关键公式汇总表

| 环节 | 公式编号 | 公式 |
|------|----------|------|
| 主观可信度 | (1) | $\alpha_i = \gamma_1 \cdot \Omega_e + \gamma_2 \cdot \Omega_k$ |
| 客观可信度 | (2) | $\beta_i = f(\text{CV}_i)$ |
| 综合可信度 | (3) | $\text{综合}_i = 0.6\alpha_i + 0.4\beta_i$ |
| 调整因子 | (4) | $q_{ij} = 0.5\,(0.6\alpha_i + 0.4\beta_i) + 0.5 \cdot \text{confidence}_{ij}$ |
| 加权评分 | (5) | $w'_{ij} = w_{ij} \times q_{ij}$ |
| AHP判断矩阵 | (6) | $A_{ij} = \text{GM}\left(\dfrac{w'_i}{w'_j}\right)$ |
| 权重计算 | (7) | $w_i = \left(\prod_{k=1}^{n} A_{ik}\right)^{1/n}$ |
| 一致性指标 | (8) | $\text{CI} = \dfrac{\lambda_{\text{max}} - n}{n - 1}$ |
| 一致性比例 | (9) | $\text{CR} = \dfrac{\text{CI}}{\text{RI}}$ |
| 熵权法-归一化 | (10) | $r_{ij} = \dfrac{x_{ij} - \min(x_j)}{\max(x_j) - \min(x_j)}$ |
| 熵权法-信息熵 | (11) | $E_j = -\dfrac{1}{\ln 10} \sum_{i=1}^{10} p_{ij} \ln p_{ij}$ |
| 熵权法-权重 | (12) | $w_j = \dfrac{1 - E_j}{\sum_{j=1}^{21}(1 - E_j)}$ |
| 最终权重 | (13) | $\text{权重}_j = \text{二级权重}_j \times \text{一级权重}_k$ |
| 全局归一化 | (14) | $\text{权重}_{j,\text{norm}} = \dfrac{\text{权重}_j}{\sum \text{权重}}$ |

---

## 十、数据库表结构

### 10.1 `expert_credibility_evaluation_score` 表（专家背景）

```sql
字段：
  - id: 专家ID
  - expert_name: 专家姓名
  - title_ql: 职称评分
  - position_ql: 职务评分
  - education_experience_ql: 学历经历评分
  - academic_achievements_ql: 学术成果评分
  - research_achievements_ql: 科研成果评分
  - exercise_experience_ql: 演习训练评分
  - military_training_knowledge_ql: 军事训练学知识评分
  - system_simulation_knowledge_ql: 系统仿真知识评分
  - statistics_knowledge_ql: 数理统计学知识评分
  - professional_years_qt: 专业年限
```

### 10.2 `ahp_expert_military_operation_effect_weights` 表（AHP评分）

```sql
字段：
  - id: 记录ID
  - batch_id: 批次ID
  - expert_name: 专家姓名
  
  # 20个权重字段 (_weight)
  - key_leakage_weight: 密钥泄露权重
  - detected_probability_weight: 被侦察概率权重
  - interception_resistance_weight: 抗拦截能力权重
  # ... (共20个)
  
  # 20个把握度字段 (_confidence)
  - key_leakage_confidence: 密钥泄露把握度
  - detected_probability_confidence: 被侦察概率把握度
  # ... (共20个)
```

### 10.3 `equipment_operation_score` 表（熵权法数据源）

```sql
字段：
  - id: 记录ID
  - operation_id: 操作ID
  - evaluation_time: 评估时间
  
  # 21个评分维度
  - personnel_personnel_count_qt: 人员数量
  - personnel_work_experience_qt: 工作经验
  - personnel_training_experience_qt: 培训经验
  - system_setup_network_setup_time_qt: 网络设置时间
  - maintenance_maintenance_skill_ql: 维修技能
  - maintenance_spare_parts_availability_qt: 备件可用性
  - response_emergency_handling_qt: 应急处理
  - comm_support_link_maintenance_qt: 链路维护
  - comm_support_service_activation_qt: 服务激活
  - comm_support_emergency_restoration_qt: 应急恢复
  - comm_attack_target_acquisition_qt: 目标获取
  - comm_attack_jamming_effectiveness_ql: 干扰效能
  - comm_attack_deception_signal_generation_ql: 欺骗信号生成
  - comm_defense_signal_interception_awareness_ql: 信号截获感知
  - comm_defense_anti_jamming_operation_ql: 抗干扰操作
  - comm_defense_anti_deception_awareness_ql: 反欺骗感知
  - system_performance_connectivity_rate_qt: 连通率
  - system_performance_mission_reliability_qt: 任务可靠性
  - maintenance_feedback_field_repair_qt: 现场维修
  - maintenance_feedback_rework_rate_qt: 返工率
  - maintenance_feedback_equipment_feedback_ql: 装备反馈
  
  - remarks: 备注
```

---

## 十一、可视化方案

本方案使用 Python 的 **matplotlib** 和 **seaborn** 库生成可视化图表。

### 11.1 图表清单

| 序号 | 图表名称 | 图表类型 | 内容 | 用途 |
|------|---------|---------|------|------|
| 图1 | 专家可信度综合图 | 3×2布局6子图 | 原始评分折线、单指标CV（删除前后对比）、专家CV、主观可信度、客观可信度、专家排序(0.6α+0.4β) | 展示专家评估全过程数据 |
| 图2 | 专家与权重综合图 | 2×2布局 | 专家可信度对比、指标权重旭日图（内/外环均显示百分比）、二级指标权重排名、原始vs最终权重对比 | 综合展示可信度与权重结构 |

### 11.2 图表详细设计

#### 图1：专家可信度综合图（6子图）

```
┌─────────────────┬─────────────────┐
│  原始评分折线   │  单指标CV柱状(前后) │
├─────────────────┼─────────────────┤
│  专家CV折线    │  主观可信度柱状 │
├─────────────────┼─────────────────┤
│  客观可信度柱状│  专家排序(0.6α+0.4β) │
└─────────────────┴─────────────────┘
```

#### 图2：专家可信度排名

- **类型**：带数值标注的降序柱状图
- **X轴**：专家姓名
- **Y轴**：综合可信度（0-1）
- **标注**：显示具体数值，按从高到低排序

#### 图3：堆叠柱状图 - 一级指标下的二级权重分布

- **X轴**：6个一级指标
- **Y轴**：权重值
- **颜色**：每个二级指标用不同颜色

#### 图3续：旭日图 - 指标权重层级分布

- **内环**：一级指标权重（显示百分比）
- **外环**：二级指标权重（显示百分比）
- **特点**：双层饼图展示指标权重的层级结构

#### 图4：水平条形图 - 二级指标权重排名

- **特殊标注**：
  - AHP权重：蓝色
  - 熵权法权重：橙色 + 标注"[熵权]"

#### 图5：双柱对比图 - 原始AHP vs 最终权重

- **标注**：显示变化百分比，变化超过±5%用特殊颜色标注
  - 红色：权重增加（+X%）
  - 蓝色：权重减少（-X%）


---

## 十二、算法变更记录

### 12.1 旭日图百分比显示
- **内环（一级指标）**：显示百分比
- **外环（二级指标）**：显示百分比

### 12.2 AHP判断矩阵离群值删除
- **触发条件**：当有效比值数量 >= 3 时
- **删除规则**：删除离均值最远的1个专家评分
- **目的**：避免极端值影响判断矩阵的准确性

### 12.3 权重变化幅度标注
- 在"原始vs最终权重对比"图中标注变化百分比
- 变化率 > 5% 的指标显示具体百分比
- 红色 = 权重增加，蓝色 = 权重减少

---

## 十三、方案版本

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.1 | 2026-03-07 | 旭日图添加百分比、AHP离群值删除、权重变化标注 |
| v1.0 | 2026-03-07 | 初始版本，包含专家可信度、AHP权重计算、熵权法备用方案、可视化方案 |

---

**文档最后更新**：2026年3月7日
