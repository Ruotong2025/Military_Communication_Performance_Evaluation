<template>
  <div class="evaluation-view">
    <!-- AHP权重配置 -->
    <AHPConfig @weights-calculated="handleWeightsCalculated" />

    <!-- 评估结果展示 -->
    <div v-if="evaluationResult" class="results-section">
      <!-- 综合得分排行榜 -->
      <el-card class="ranking-card">
        <template #header>
          <div class="card-header">
            <el-icon class="header-icon"><Trophy /></el-icon>
            <span>综合评估排行榜</span>
          </div>
        </template>
        
        <el-table
          :data="evaluationResult.evaluationResults"
          border
          stripe
          :default-sort="{ prop: 'rank', order: 'ascending' }"
          style="width: 100%"
        >
          <el-table-column prop="rank" label="排名" width="80" align="center" fixed>
            <template #default="{ row }">
              <el-tag
                :type="getRankType(row.rank)"
                size="large"
                effect="dark"
              >
                <span v-if="row.rank === 1">🥇</span>
                <span v-else-if="row.rank === 2">🥈</span>
                <span v-else-if="row.rank === 3">🥉</span>
                <span v-else>{{ row.rank }}</span>
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column prop="testId" label="测试批次" width="150" fixed />
          <el-table-column prop="scenarioId" label="场景ID" width="100" align="center" />
          
          <el-table-column prop="totalScore" label="综合得分" width="120" align="center" sortable>
            <template #default="{ row }">
              <el-tag :type="getScoreType(row.totalScore)" size="large" effect="dark">
                {{ row.totalScore.toFixed(2) }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column prop="grade" label="评估等级" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getGradeType(row.grade)" size="large">
                {{ row.grade }}
              </el-tag>
            </template>
          </el-table-column>
          
          <!-- 8个维度得分 -->
          <el-table-column
            v-for="dim in dimensions"
            :key="dim.code"
            :label="dim.name"
            width="100"
            align="center"
          >
            <template #default="{ row }">
              <span :style="{ color: getScoreColor(row.dimensionScores[dim.code]) }">
                {{ row.dimensionScores[dim.code].toFixed(1) }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 维度图表 -->
      <DimensionCharts :evaluation-result="evaluationResult" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Trophy } from '@element-plus/icons-vue'
import AHPConfig from '@/components/AHPConfig.vue'
import DimensionCharts from '@/components/DimensionCharts.vue'

const dimensions = [
  { code: 'RL', name: '可靠性' },
  { code: 'SC', name: '安全性' },
  { code: 'AJ', name: '抗干扰性' },
  { code: 'EF', name: '有效性' },
  { code: 'PO', name: '处理能力' },
  { code: 'NC', name: '组网能力' },
  { code: 'HO', name: '人为操作' },
  { code: 'RS', name: '响应能力' }
]

const ahpWeights = ref(null)
const evaluationResult = ref(null)

const handleWeightsCalculated = (data) => {
  ahpWeights.value = data.weights
  evaluationResult.value = data.pythonResult
}

// 获取排名类型
const getRankType = (rank) => {
  if (rank === 1) return 'danger'
  if (rank === 2) return 'warning'
  if (rank === 3) return 'success'
  return 'info'
}

// 获取得分类型
const getScoreType = (score) => {
  if (score >= 90) return 'success'
  if (score >= 80) return 'primary'
  if (score >= 70) return 'warning'
  if (score >= 60) return 'info'
  return 'danger'
}

// 获取等级类型
const getGradeType = (grade) => {
  const gradeMap = {
    '优秀': 'success',
    '良好': 'primary',
    '中等': 'warning',
    '及格': 'info',
    '较差': 'danger'
  }
  return gradeMap[grade] || 'info'
}

// 获取得分颜色
const getScoreColor = (score) => {
  if (score >= 90) return '#67C23A'
  if (score >= 80) return '#409EFF'
  if (score >= 70) return '#E6A23C'
  if (score >= 60) return '#909399'
  return '#F56C6C'
}
</script>

<style scoped lang="scss">
.evaluation-view {
  .results-section {
    margin-top: 20px;

    .ranking-card {
      margin-bottom: 20px;

      .card-header {
        display: flex;
        align-items: center;
        gap: 10px;
        font-size: 18px;

        .header-icon {
          font-size: 24px;
          color: var(--accent-gold);
        }
      }
    }
  }
}
</style>
