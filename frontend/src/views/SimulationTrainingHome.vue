<template>
  <div class="simulation-training-home">
    <!-- 重要提示横幅 -->
    <el-alert
      type="warning"
      :closable="false"
      show-icon
      class="guide-banner"
    >
      <template #title>
        <span class="banner-title">🔔 使用前请阅读：系统操作指南</span>
      </template>
    </el-alert>

    <!-- 操作流程说明 - 图形化展示 -->
    <el-card class="guide-card" shadow="hover">
      <template #header>
        <div class="card-header-custom">
          <span>📌 系统使用流程（共5步）</span>
          <el-tag type="success" size="small">按顺序执行效果最佳</el-tag>
        </div>
      </template>

      <!-- 顶部流程图 -->
      <div class="flow-diagram">
        <div class="flow-container">
          <div v-for="(step, index) in flowSteps" :key="index" class="flow-item">
            <div class="flow-node" :style="{ backgroundColor: step.color }">
              <el-icon class="flow-icon"><component :is="step.icon" /></el-icon>
            </div>
            <div class="flow-label">
              <span class="flow-title">{{ step.title }}</span>
              <span class="flow-desc">{{ step.desc }}</span>
            </div>
            <div v-if="index < flowSteps.length - 1" class="flow-arrow">
              <el-icon><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </div>

      <!-- 展开的详细模块 -->
      <el-collapse class="detail-collapse">
        <el-collapse-item title="🔍 查看各阶段详细模块" name="details">
          <div class="phase-container">
            <!-- 第一阶段：数据准备 -->
            <div class="phase-block">
              <div class="phase-header">
                <el-tag type="primary" size="large">阶段一：数据准备</el-tag>
                <span class="phase-hint">以下3个模块按顺序完成</span>
              </div>
              <div class="phase-flow">
                <div class="module-node" @click="go('/simulation-training/data')">
                  <el-icon><DataAnalysis /></el-icon>
                  <span>军事作战模拟数据</span>
                  <el-tag type="danger" size="small" class="must-tag">必做</el-tag>
                </div>
                <el-icon class="phase-arrow"><ArrowRight /></el-icon>
                <div class="module-node" @click="go('/simulation-training/weights/expert')">
                  <el-icon><User /></el-icon>
                  <span>专家可信度评估</span>
                  <el-tag type="danger" size="small" class="must-tag">必做</el-tag>
                </div>
                <el-icon class="phase-arrow"><ArrowRight /></el-icon>
                <div class="module-node" @click="go('/simulation-training/equipment-evaluation')">
                  <el-icon><EditPen /></el-icon>
                  <span>专家定性数据评估</span>
                  <el-tag type="danger" size="small" class="must-tag">必做</el-tag>
                </div>
              </div>
            </div>

            <!-- 第二阶段：权重确定 -->
            <div class="phase-block">
              <div class="phase-header">
                <el-tag type="warning" size="large">阶段二：权重确定</el-tag>
                <span class="phase-hint">AHP分析核心步骤</span>
              </div>
              <div class="phase-flow">
                <div class="module-node" @click="go('/simulation-training/weights/evaluation')">
                  <el-icon><TrendCharts /></el-icon>
                  <span>专家AHP打分</span>
                  <el-tag type="danger" size="small" class="must-tag">必做</el-tag>
                </div>
                <el-icon class="phase-arrow"><ArrowRight /></el-icon>
                <div class="module-node optional" @click="go('/simulation-training/weights/ahp-dispersion')">
                  <el-icon><DataLine /></el-icon>
                  <span>权重离散度分析</span>
                  <el-tag type="info" size="small">可选</el-tag>
                </div>
                <el-icon class="phase-arrow"><ArrowRight /></el-icon>
                <div class="module-node" @click="go('/simulation-training/weights/aggregation-scoring')">
                  <el-icon><Flag /></el-icon>
                  <span>权重集结打分</span>
                  <el-tag type="danger" size="small" class="must-tag">必做</el-tag>
                </div>
              </div>
            </div>

            <!-- 第三阶段：结果计算 -->
            <div class="phase-block">
              <div class="phase-header">
                <el-tag type="success" size="large">阶段三：结果计算</el-tag>
                <span class="phase-hint">最终评估输出</span>
              </div>
              <div class="phase-flow">
                <div class="module-node" @click="go('/simulation-training/results/comprehensive-scoring')">
                  <el-icon><CircleCheck /></el-icon>
                  <span>综合打分</span>
                  <el-tag type="danger" size="small" class="must-tag">必做</el-tag>
                </div>
                <el-icon class="phase-arrow"><ArrowRight /></el-icon>
                <div class="module-node optional" @click="go('/simulation-training/results/penalty-factor')">
                  <el-icon><Warning /></el-icon>
                  <span>惩罚因子分析</span>
                  <el-tag type="info" size="small">可选</el-tag>
                </div>
                <el-icon class="phase-arrow"><ArrowRight /></el-icon>
                <div class="module-node optional" @click="go('/simulation-training/results/cost-effectiveness')">
                  <el-icon><PieChart /></el-icon>
                  <span>效费分析</span>
                  <el-tag type="info" size="small">可选</el-tag>
                </div>
              </div>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-card>

    <!-- 功能模块入口 -->
    <div class="page-header" style="margin-top: 24px;">
      <h2>
        <el-icon><Guide /></el-icon>
        功能模块入口
      </h2>
      <p class="subtitle">
        以下为各功能模块入口，请从左侧菜单或下方卡片进入对应页面
      </p>
    </div>

    <el-row :gutter="16">
      <el-col v-for="card in entryCards" :key="card.path" :xs="24" :sm="12" :lg="8">
        <el-card class="entry-card" shadow="hover" @click="go(card.path)">
          <div class="entry-card-inner">
            <el-icon class="entry-icon"><component :is="card.icon" /></el-icon>
            <div class="entry-text">
              <h3>{{ card.title }}</h3>
              <p>{{ card.desc }}</p>
            </div>
            <el-icon class="entry-arrow"><ArrowRight /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import {
  Guide,
  ArrowRight,
  DataAnalysis,
  User,
  TrendCharts,
  DataLine,
  Flag,
  CircleCheck,
  Warning,
  PieChart,
  EditPen,
  Upload
} from '@element-plus/icons-vue'

const router = useRouter()

// 顶部流程图数据
const flowSteps = [
  { title: '数据准备', desc: '上传数据', icon: Upload, color: '#409EFF' },
  { title: '专家评估', desc: '可信度评估', icon: User, color: '#67C23A' },
  { title: '定性评估', desc: '专家打分', icon: EditPen, color: '#E6A23C' },
  { title: '权重确定', desc: 'AHP+集结', icon: TrendCharts, color: '#F56C6C' },
  { title: '结果计算', desc: '综合+效费', icon: PieChart, color: '#9B59B6' }
]

// 跳转到指定页面
function go(path) {
  router.push(path)
}

const entryCards = [
  {
    path: '/simulation-training/data',
    title: '军事作战模拟数据',
    desc: '模拟训练数据准备：作战实验与指标数据。',
    icon: DataAnalysis
  },
  {
    path: '/simulation-training/weights/expert',
    title: '专家可信度评估',
    desc: '模拟训练数据准备：专家可信度与把握度。',
    icon: User
  },
  {
    path: '/simulation-training/equipment-evaluation',
    title: '装备操作评估',
    desc: '装备操作定量指标计算与专家定性评估录入。',
    icon: EditPen
  },
  {
    path: '/simulation-training/weights/evaluation',
    title: '专家 AHP 打分',
    desc: '权重确定：层次分析法打分与权重。',
    icon: TrendCharts
  },
  {
    path: '/simulation-training/weights/ahp-dispersion',
    title: '权重离散度分析',
    desc: '权重确定：CV 与专家意见一致性分析。',
    icon: DataLine
  },
  {
    path: '/simulation-training/weights/aggregation-scoring',
    title: '权重集结打分',
    desc: '集结参数、专家权重、一致性检验、集体判断矩阵与指标综合权重。',
    icon: Flag
  },
  {
    path: '/simulation-training/results/comprehensive-scoring',
    title: '综合打分',
    desc: '评估结果计算：原始指标表与综合评估得分图表（需先完成集结计算）。',
    icon: CircleCheck
  },
  {
    path: '/simulation-training/results/penalty-factor',
    title: '惩罚因子分析',
    desc: '评估结果计算：惩罚因子相关分析（规划中）。',
    icon: Warning
  },
  {
    path: '/simulation-training/results/cost-effectiveness',
    title: '效费分析',
    desc: '评估结果计算：效费比分析（规划中）。',
    icon: PieChart
  }
]
</script>

<style scoped lang="scss">
.simulation-training-home {
  max-width: 1100px;
}

/* 指南横幅 */
.guide-banner {
  margin-bottom: 20px;

  .banner-title {
    font-size: 16px;
    font-weight: bold;
  }
}

/* 操作流程卡片 */
.guide-card {
  margin-bottom: 16px;

  .card-header-custom {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 15px;
    font-weight: 600;
  }
}

/* 顶部流程图 */
.flow-diagram {
  padding: 20px 10px;
  overflow-x: auto;
}

.flow-container {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-width: max-content;
}

.flow-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.flow-node {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);

  .flow-icon {
    font-size: 28px;
    color: white;
  }
}

.flow-label {
  display: flex;
  flex-direction: column;
  gap: 2px;

  .flow-title {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
  }

  .flow-desc {
    font-size: 11px;
    color: #909399;
  }
}

.flow-arrow {
  font-size: 20px;
  color: #c0c4cc;
  margin: 0 8px;
}

/* 详细模块折叠面板 */
.detail-collapse {
  margin-top: 20px;

  :deep(.el-collapse-item__header) {
    font-size: 14px;
    font-weight: 500;
    color: #409EFF;
    background: #f0f7ff;
    border-radius: 6px;
    padding: 12px 16px;
  }

  :deep(.el-collapse-item__content) {
    padding-top: 16px;
  }
}

/* 阶段容器 */
.phase-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.phase-block {
  background: #fafbfc;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #ebeef5;

  .phase-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;

    .phase-hint {
      font-size: 12px;
      color: #909399;
    }
  }
}

/* 阶段内的模块流程 */
.phase-flow {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.module-node {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: white;
  border: 2px solid #409EFF;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 160px;

  .el-icon {
    font-size: 18px;
    color: #409EFF;
  }

  span {
    font-size: 13px;
    color: #303133;
    font-weight: 500;
  }

  &:hover {
    background: #409EFF;
    color: white;

    .el-icon {
      color: white;
    }

    span {
      color: white;
    }
  }

  &.optional {
    border-color: #909399;

    .el-icon {
      color: #909399;
    }

    &:hover {
      background: #909399;

      .el-icon {
        color: white;
      }
    }
  }
}

.must-tag {
  margin-left: auto;
}

.phase-arrow {
  font-size: 18px;
  color: #c0c4cc;
}

.page-header {
  margin-bottom: 22px;

  h2 {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 22px;
    color: var(--text-primary, #001f3f);
    margin-bottom: 8px;
  }

  .subtitle {
    color: #606266;
    font-size: 14px;
    line-height: 1.6;
  }
}

.entry-card {
  cursor: pointer;
  border: 1px solid rgba(0, 116, 217, 0.2);
  transition: transform 0.2s, box-shadow 0.2s;
  margin-bottom: 16px;
  height: calc(100% - 16px);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 31, 63, 0.12);
  }

  :deep(.el-card__body) {
    padding: 18px 20px;
  }
}

.entry-card-inner {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.entry-icon {
  font-size: 36px;
  color: #0074d9;
  flex-shrink: 0;
  margin-top: 2px;
}

.entry-text {
  flex: 1;
  min-width: 0;

  h3 {
    font-size: 16px;
    margin-bottom: 6px;
    color: #001f3f;
    line-height: 1.35;
  }

  p {
    font-size: 12px;
    color: #606266;
    line-height: 1.5;
    margin: 0;
  }
}

.entry-arrow {
  font-size: 18px;
  color: #909399;
  flex-shrink: 0;
  margin-top: 4px;
}
</style>
