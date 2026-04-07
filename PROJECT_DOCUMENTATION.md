# 军事通信效能评估系统 - 完整项目文档

> 本文档记录系统的前端设计、数据表结构、后端方案等完整内容。

---

## 一、系统概述

这是一个 **军事通信效能评估系统**，采用 **Spring Boot + Vue 3** 的前后端分离架构。系统包含三大核心模块：

1. **专家可信度评估** - 评估专家的权威性和可信度
2. **装备操作评估** - 定量指标计算 + 定性专家评分
3. **效费分析** - 投入产出比分析

---

## 二、前端结构 (`frontend/`)

### 2.1 前端目录结构

```
frontend/
├── src/
│   ├── api/
│   │   └── index.js              # API 接口定义
│   ├── router/
│   │   └── index.js              # Vue Router 配置
│   ├── views/                    # 页面视图（10个Vue文件）
│   ├── components/               # 公共组件（6个Vue文件）
│   ├── utils/
│   │   └── request.js            # Axios 封装
│   ├── composables/
│   │   └── useGlobalOperationFilter.js
│   ├── config/
│   │   ├── ahpComparisons.js
│   │   └── recordsFieldCatalog.js
│   ├── styles/
│   │   └── navy-theme.scss
│   ├── App.vue
│   └── main.js
└── dist/                         # 构建产物
    ├── index.html
    └── assets/
```

### 2.2 前端视图文件 (`frontend/src/views/`)

| 文件名 | 页面功能 | 行数 |
|--------|----------|------|
| `SimulationTrainingHome.vue` | 系统导航首页 | - |
| `SimulationTrainingLayout.vue` | 主布局组件 | - |
| `DataView.vue` | 军事作战模拟数据管理 | - |
| `EvaluationView.vue` | 专家AHP打分 | - |
| `ExpertCredibility.vue` | 专家可信度评估 | - |
| `ExpertAggregation.vue` | 专家集结 | - |
| `ComprehensiveScoringView.vue` | 综合打分结果展示 | - |
| `PenaltyFactorAnalysisView.vue` | 惩罚因子分析 | - |
| `CostEffectivenessAnalysisView.vue` | 效费分析 | 1718 |
| `ExpertEquipmentEvaluation.vue` | 装备操作评估 | 622 |

### 2.3 前端组件 (`frontend/src/components/`)

| 文件名 | 组件功能 |
|--------|----------|
| `AHPConfig.vue` | AHP层次分析法配置 |
| `AdvancedParamsPanel.vue` | 高级参数面板 |
| `ComprehensiveScoreBlock.vue` | 综合得分展示块 |
| `DimensionCharts.vue` | 多维度图表 |
| `DynamicTable.vue` | 动态数据表格 |
| `MetricsCalculation.vue` | 指标计算组件 |

### 2.4 路由配置

```
/simulation-training
├── /                          # SimulationTrainingHome
├── /data                      # DataView
├── /weights/expert            # ExpertCredibility
├── /weights/evaluation        # EvaluationView
├── /weights/ahp-dispersion    # ExpertAggregation (分散度模式)
├── /weights/aggregation-scoring # ExpertAggregation (集结模式)
├── /collective-scoring         # 重定向到集结打分
├── /results/comprehensive-scoring # ComprehensiveScoringView
├── /results/penalty-factor     # PenaltyFactorAnalysisView
├── /results/cost-effectiveness # CostEffectivenessAnalysisView
└── /equipment-evaluation       # ExpertEquipmentEvaluation
```

---

## 三、数据表结构 (`SQL/`)

### 3.1 效费分析表 (`cost_effectiveness_tables.sql`)

#### 表1: `cost_indicator_config` - 成本指标配置表

| 字段 | 类型 | 说明 |
|------|------|------|
| indicator_key | VARCHAR(50) | 指标唯一标识 |
| indicator_name | VARCHAR(100) | 中文名称 |
| indicator_name_en | VARCHAR(100) | 英文名称 |
| category | VARCHAR(50) | 所属类别 |
| source_table | VARCHAR(100) | 来源表 |
| source_field | VARCHAR(100) | 来源字段 |
| aggregation_method | ENUM | 聚合方式 |
| unit | VARCHAR(20) | 单位 |
| score_direction | ENUM | 得分方向(positive/negative) |
| display_order | INT | 显示顺序 |
| enabled | TINYINT | 是否启用 |

#### 表2: `cost_evaluation_record` - 成本评估记录表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| batch_id | BIGINT | 批次ID |
| operation_id | BIGINT | 作战任务ID |
| cost_index | DECIMAL | 成本指数 |
| effectiveness_index | DECIMAL | 效能指数 |
| cost_effectiveness_ratio | DECIMAL | 效费比 |
| effectiveness_level | VARCHAR(20) | 效费等级 |
| raw_indicator_values | JSON | 原始指标值 |
| normalized_indicator_values | JSON | 归一化指标值 |
| created_at | TIMESTAMP | 创建时间 |

#### 表3: `cost_evaluation_batch` - 成本评估批次表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| batch_name | VARCHAR(100) | 批次名称 |
| operation_ids | JSON | 作战任务ID列表 |
| category_weights | JSON | 类别权重 |
| result_summary | JSON | 结果汇总 |
| created_at | TIMESTAMP | 创建时间 |

### 3.2 装备操作评估表 (`equipment_operation_tables.sql`)

#### 表4: `equipment_qt_indicator_def` - 定量指标配置模板

| 字段 | 类型 | 说明 |
|------|------|------|
| indicator_key | VARCHAR(50) | 指标唯一标识 |
| indicator_name | VARCHAR(100) | 中文名称 |
| indicator_name_en | VARCHAR(100) | 英文名称 |
| phase | ENUM | 阶段(pre_war/mid_war/post_war) |
| dimension | VARCHAR(50) | 维度 |
| description | TEXT | 描述 |
| source_table | VARCHAR(100) | 来源表 |
| source_field | VARCHAR(100) | 来源字段 |
| aggregation_method | ENUM | 聚合方式 |
| custom_sql_template | TEXT | 自定义SQL模板 |
| unit | VARCHAR(20) | 单位 |
| score_direction | ENUM | 得分方向 |
| display_order | INT | 显示顺序 |
| enabled | TINYINT | 是否启用 |

#### 表5: `equipment_ql_indicator_def` - 定性指标配置模板

| 字段 | 类型 | 说明 |
|------|------|------|
| indicator_key | VARCHAR(50) | 指标唯一标识 |
| indicator_name | VARCHAR(100) | 中文名称 |
| indicator_name_en | VARCHAR(100) | 英文名称 |
| phase | ENUM | 阶段 |
| dimension | VARCHAR(50) | 维度 |
| description | TEXT | 描述 |
| reference_table | VARCHAR(100) | 参考表 |
| reference_field | VARCHAR(100) | 参考字段 |
| reference_sql_template | TEXT | SQL模板 |
| evaluation_method | ENUM | 评估方法 |
| grade_keys | JSON | 等级选项 |
| confidence_required | TINYINT | 是否需要把握度 |
| confidence_method | ENUM | 把握度计算方式 |
| allow_comment | TINYINT | 是否允许评语 |
| scoring_help | TEXT | 打分说明 |
| display_order | INT | 显示顺序 |
| enabled | TINYINT | 是否启用 |

#### 表6: `equipment_qt_evaluation_record` - 定量评估记录

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| batch_id | BIGINT | 批次ID |
| operation_id | BIGINT | 作战任务ID |
| indicator_key | VARCHAR(50) | 指标标识 |
| raw_value | DECIMAL | 原始值 |
| normalized_score | DECIMAL | 归一化得分 |
| calculated_at | TIMESTAMP | 计算时间 |

#### 表7: `equipment_ql_evaluation_record` - 定性评估记录

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| batch_id | BIGINT | 批次ID |
| operation_id | BIGINT | 作战任务ID |
| indicator_key | VARCHAR(50) | 指标标识 |
| expert_id | BIGINT | 专家ID |
| grade | VARCHAR(20) | 等级 |
| confidence | DECIMAL | 把握度 |
| numeric_score | DECIMAL | 数值得分 |
| comment | TEXT | 评语 |
| submitted_at | TIMESTAMP | 提交时间 |

### 3.3 操作记录表

#### 表8: `records_comm_attack_operation` - 进攻操作记录

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| operation_id | BIGINT | 作战任务ID |
| operation_type | VARCHAR(50) | 操作类型 |
| start_time_ms | BIGINT | 开始时间 |
| end_time_ms | BIGINT | 结束时间 |
| jamming_success | TINYINT | 干扰是否成功 |
| effect_assessment | TEXT | 效果评估 |
| spoofing_signal_type | VARCHAR(50) | 欺骗信号类型 |
| spoofing_success | TINYINT | 欺骗是否成功 |

#### 表9: `records_comm_defense_operation` - 防御操作记录

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| operation_id | BIGINT | 作战任务ID |
| operation_type | VARCHAR(50) | 操作类型 |
| detection_time_ms | BIGINT | 检测耗时 |
| anti_jamming_success | TINYINT | 抗干扰是否成功 |
| suspicious_signal_detected | TINYINT | 是否检测到可疑信号 |
| verification_result | VARCHAR(50) | 核实结果 |

---

## 四、装备操作指标详情

### 4.1 定量指标（10项）

| 指标Key | 指标名称 | 英文名称 | 阶段 | 维度 |
|---------|----------|----------|------|------|
| eqt_network_setup_time | 网络建连耗时 | Network Setup Time | pre_war | 网络连通性 |
| eqt_call_success_rate | 通话成功率 | Call Success Rate | pre_war | 网络连通性 |
| eqt_recovery_duration | 链路恢复时长 | Link Recovery Duration | pre_war | 网络连通性 |
| eqt_security_event_rate | 安全事件发生率 | Security Event Rate | pre_war | 网络连通性 |
| eqt_operator_reaction_time | 操作员反应时间 | Operator Reaction Time | mid_war | 人员操作 |
| eqt_comm_success_rate | 通信综合成功率 | Communication Success Rate | mid_war | 人员操作 |
| eqt_operator_count | 人员投入数量 | Operator Count | mid_war | 人员操作 |
| eqt_jamming_detection_rate | 干扰检出率 | Jamming Detection Rate | mid_war | 人员操作 |
| eqt_jamming_target_time | 干扰目标锁定耗时 | Jamming Target Lock Time | mid_war | 通信进攻操作 |
| eqt_deception_success_rate | 欺骗信号成功率 | Deception Success Rate | mid_war | 通信进攻操作 |

### 4.2 定性指标（6项）

| 指标Key | 指标名称 | 英文名称 | 阶段 | 维度 |
|---------|----------|----------|------|------|
| eql_maintenance_skill | 维修能力 | Maintenance Skill | post_war | 维修与反馈 |
| eql_jamming_effectiveness | 干扰效能达成 | Jamming Effectiveness | mid_war | 通信进攻操作 |
| eql_deception_signal | 欺骗信号生成 | Deception Signal Generation | mid_war | 通信进攻操作 |
| eql_signal_interception | 信号截获感知 | Signal Interception Awareness | mid_war | 通信防御操作 |
| eql_anti_jamming | 抗干扰操作 | Anti-Jamming Operation | mid_war | 通信防御操作 |
| eql_anti_deception | 防骗反骗 | Anti-Deception | mid_war | 通信防御操作 |

---

## 五、后端结构 (`src/main/java/com/ccnu/military/`)

### 5.1 后端包结构

```
src/main/java/com/ccnu/military/
├── controller/                   # 控制器层（13个文件）
├── service/                     # 服务层
├── entity/                       # 实体类
├── dto/                          # 数据传输对象
└── repository/                   # 数据访问层
```

### 5.2 核心 Controller

| Controller | 职责 |
|------------|------|
| `CostEffectivenessController` | 效费分析 API |
| `EquipmentQtController` | 定量指标计算 API |
| `EquipmentQlController` | 定性指标评估 API |
| `ExpertAggregationCollectiveController` | 专家集结计算 API |
| `ExpertAggregationController` | 专家分散度分析 API |
| `ExpertAHPController` | AHP层次分析法 API |
| `PenaltyModelController` | 惩罚模型 API |
| `TableController` | 数据表管理 API |

### 5.3 核心 Service

| Service | 职责 |
|---------|------|
| `CostEffectivenessService` | 效费比计算、权重配置、归一化处理 |
| `EquipmentQtCalculationService` | 定量指标动态SQL计算、归一化得分 |
| `EquipmentQlEvaluationService` | 定性指标专家打分录入与集结 |
| `ExpertAggregationCollectiveService` | 专家集结算法实现 |
| `ExpertAHPSevice` | AHP矩阵计算与权重推导 |
| `PenaltyModelService` | 惩罚因子模型计算 |

### 5.4 核心算法说明

#### 5.4.1 效费比计算

```
效费比 R = 效能指数 E / 成本指数 C

步骤：
1. 从数据库读取原始成本数据
2. 对每个指标进行Min-Max归一化
3. 按类别权重加权求和得到成本指数
4. 从综合评估系统获取效能指数
5. 计算效费比并评定等级
```

#### 5.4.2 定量指标计算

```
支持多种聚合方式：
- direct: 直接取值
- avg: 平均值
- sum: 求和
- count: 计数
- percentage: 百分比
- custom: 自定义SQL
- avg_conditional: 条件平均值
```

#### 5.4.3 定性指标评估

```
专家打分流程：
1. 专家选择等级（A+ ~ D）
2. 填写把握度（百分比）
3. 可选填写评语
4. 系统计算综合得分 = 等级分 × 把握度
5. 集结多位专家评分
```

---

## 六、API 接口汇总

### 6.1 效费分析 API

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/cost/categories` | GET | 获取指标类别 |
| `/api/cost/indicators` | GET | 获取成本指标配置 |
| `/api/cost/raw-data` | GET | 获取原始成本数据预览 |
| `/api/cost/calculate` | POST | 执行效费分析计算 |
| `/api/cost/results` | GET | 获取效费分析结果 |
| `/api/cost/weights` | POST | 保存指标权重 |
| `/api/cost/weights/reset` | POST | 重置为等权权重 |

### 6.2 装备操作评估 API（定量）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/equipment/qt/indicators` | GET | 获取定量指标配置 |
| `/api/equipment/qt/calculate` | POST | 计算定量指标 |
| `/api/equipment/qt/normalize` | POST | 生成归一化得分 |
| `/api/equipment/qt/batches` | GET | 获取批次列表 |
| `/api/equipment/qt/records` | GET | 获取评估记录 |

### 6.3 装备操作评估 API（定性）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/equipment/ql/indicators` | GET | 获取定性指标配置 |
| `/api/equipment/ql/reference-data` | GET | 获取参考数据 |
| `/api/equipment/ql/submit` | POST | 提交专家评分 |
| `/api/equipment/ql/batches` | GET | 获取批次列表 |
| `/api/equipment/ql/records` | GET | 获取评估记录 |

### 6.4 专家评估 API

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/expert/list` | GET | 获取专家列表 |
| `/api/expert/evaluate` | POST | 评估专家 |
| `/api/expert/scores` | GET | 获取评估得分 |
| `/api/expert/ahp/calculate` | POST | AHP权重计算 |
| `/api/expert/collective/preview` | POST | 预览集结权重 |
| `/api/expert/collective/execute` | POST | 执行集结计算 |

---

## 七、Python 工具脚本

### 7.1 数据生成脚本

```bash
# 生成模拟数据
python military_operational_effectiveness_evaluation/generate/generate_all_data.py
```

### 7.2 指标插入脚本

```bash
# 插入装备操作指标数据
python military_operational_effectiveness_evaluation/generate/insert_equipment_indicators.py
```

---

## 八、部署说明

### 8.1 环境要求

- Java 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.8+

### 8.2 数据库初始化

```bash
# 1. 创建数据库
mysql -u root -p < SQL/military_operational_effectiveness_evaluation.sql

# 2. 执行效费分析表
mysql -u root -p < SQL/cost_effectiveness_tables.sql

# 3. 执行装备操作评估表
mysql -u root -p < SQL/equipment_operation_tables.sql

# 4. 插入装备操作指标数据
python military_operational_effectiveness_evaluation/generate/insert_equipment_indicators.py
```

### 8.3 后端启动

```bash
cd src
mvn spring-boot:run
```

### 8.4 前端启动

```bash
cd frontend
npm install
npm run dev
```

---

## 九、项目总结

这是一个功能完整的**军事通信效能评估系统**，具备以下特点：

1. **效费分析模块** - 完整的成本-效益分析流程
2. **装备操作评估模块** - 定量指标自动计算 + 定性专家打分
3. **专家评估模块** - AHP层次分析法 + 可信度评估 + 集结算法
4. **数据管理模块** - 作战模拟数据管理

系统采用前后端分离架构，后端基于 Spring Boot + JPA + MySQL，前端基于 Vue 3 + Element Plus + ECharts，具备良好的可扩展性和可维护性。

---

*文档生成时间: 2026-04-03*
