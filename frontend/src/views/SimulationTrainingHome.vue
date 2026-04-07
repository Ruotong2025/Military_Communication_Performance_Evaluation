<template>
  <div class="simulation-training-home">
    <div class="page-header">
      <h2>
        <el-icon><Guide /></el-icon>
        模拟训练评估系统导航
      </h2>
      <p class="subtitle">
        以下为各功能模块入口，请从左侧菜单或下方卡片进入对应页面（本页不包含具体业务计算）。
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
  EditPen
} from '@element-plus/icons-vue'

const router = useRouter()

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

function go(path) {
  router.push(path)
}
</script>

<style scoped lang="scss">
.simulation-training-home {
  max-width: 1100px;
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
