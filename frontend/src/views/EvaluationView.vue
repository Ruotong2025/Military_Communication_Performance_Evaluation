<template>
  <div class="evaluation-view">
    <!-- 专家 AHP：统一顶栏（Tab + 当前专家），两页 v-show 保持切换前状态 -->
    <el-card class="ahp-unified-card" shadow="hover">
      <template #header>
        <div class="ahp-unified-toolbar">
          <div class="ahp-unified-toolbar__title">
            <el-icon class="header-icon"><Setting /></el-icon>
            <span>专家AHP权重配置</span>
            <el-tag type="warning" effect="plain" size="small">上三角输入，下三角自动倒数</el-tag>
          </div>
          <div class="ahp-unified-toolbar__expert" @click.stop>
            <el-form :inline="true" class="ahp-unified-expert-form">
              <el-form-item label="当前专家">
                <el-select
                  v-model="sharedExpertId"
                  placeholder="选择专家（与可信度库 expert_id 一致）"
                  clearable
                  filterable
                  style="width: min(280px, 100%)"
                >
                  <el-option
                    v-for="e in expertOptionsForAhp"
                    :key="e.expertId"
                    :label="`${e.expertName}（ID ${e.expertId}）`"
                    :value="e.expertId"
                  />
                </el-select>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </template>

      <div class="ahp-unified-body">
        <!-- 一、标度与把握度说明（始终展示） -->
        <div class="ahp-unified-legends-block">
          <el-alert type="info" :closable="false" show-icon class="ahp-unified-top-alert">
            <template v-if="!sharedExpertId">
              请先在上方选择「当前专家」。下方先阅读 <strong>AHP 判断标度说明</strong>与<strong>把握度等级</strong>，再在其下的<strong>AHP 判断矩阵</strong>中录入；切换「效能指标 / 装备操作」不丢失已填内容。
            </template>
            <template v-else>
              已选择专家：效能指标矩阵保存于 <code>expert_ahp_comparison_score</code>（无「装备操作_」前缀）；装备操作矩阵保存为 <code>装备操作_</code> 前缀记录。两套数据互不影响。
            </template>
          </el-alert>
          <AhpSharedLegends />
        </div>

        <!-- 二、把握度说明正下方：两套 AHP 判断矩阵（效能 / 装备） -->
        <el-card class="ahp-matrices-card" shadow="hover">
          <template #header>
            <div class="ahp-matrices-card-header">
              <div class="ahp-matrices-card-title-wrap">
                <el-icon><Grid /></el-icon>
                <span class="ahp-matrices-card-title">AHP 判断矩阵</span>
              </div>
              <el-tag type="info" effect="plain" size="small">
                维度层与指标层成对比较；请先选择页顶「当前专家」后矩阵才会展开
              </el-tag>
            </div>
          </template>
          <div class="ahp-matrix-toolbar">
            <div class="ahp-matrix-toolbar__actions">
              <el-button
                type="primary"
                :disabled="!sharedExpertId"
                :loading="toolbarSaving"
                @click="onToolbarSave"
              >
                保存到数据库
              </el-button>
              <el-button
                :disabled="!sharedExpertId"
                :loading="toolbarLoading"
                @click="onToolbarReload"
              >
                重新加载
              </el-button>
              <el-button type="success" @click="onToolbarSimulate">批量模拟入库</el-button>
            </div>
            <el-radio-group v-model="activeAhpTab" size="default" class="ahp-matrix-toolbar__tabs">
              <el-radio-button label="effectiveness">效能指标 AHP</el-radio-button>
              <el-radio-button label="equipment">装备操作 AHP</el-radio-button>
            </el-radio-group>
          </div>
          <div class="ahp-unified-panes">
            <div v-show="activeAhpTab === 'effectiveness'" class="ahp-unified-pane">
              <AHPConfig
                ref="ahpEffectivenessRef"
                embedded
                legends-in-parent
                toolbar-in-parent
                :expert-id="sharedExpertId"
                @update:expert-id="sharedExpertId = $event"
                @weights-calculated="handleWeightsCalculated"
                @expert-options="onAhpExpertOptions"
              />
            </div>
            <div v-show="activeAhpTab === 'equipment'" class="ahp-unified-pane">
              <EquipmentAhpConfig
                ref="ahpEquipmentRef"
                embedded
                legends-in-parent
                toolbar-in-parent
                :expert-id="sharedExpertId"
                @update:expert-id="sharedExpertId = $event"
                @weights-calculated="handleEquipmentWeightsCalculated"
                @expert-options="onAhpExpertOptions"
              />
            </div>
          </div>
        </el-card>
      </div>
    </el-card>

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
import { Trophy, Setting, Grid } from '@element-plus/icons-vue'
import AHPConfig from '@/components/AHPConfig.vue'
import EquipmentAhpConfig from '@/components/EquipmentAhpConfig.vue'
import AhpSharedLegends from '@/components/AhpSharedLegends.vue'
import DimensionCharts from '@/components/DimensionCharts.vue'

const activeAhpTab = ref('effectiveness')
/** 两页共用的专家 ID（v-show 保留子组件状态，专家选择统一在此） */
const sharedExpertId = ref(null)
const expertOptionsForAhp = ref([])

const ahpEffectivenessRef = ref(null)
const ahpEquipmentRef = ref(null)
const toolbarSaving = ref(false)
const toolbarLoading = ref(false)

function activeAhpPaneRef() {
  return activeAhpTab.value === 'effectiveness' ? ahpEffectivenessRef.value : ahpEquipmentRef.value
}

async function onToolbarSave() {
  const pane = activeAhpPaneRef()
  if (!pane?.saveScoresToDb) return
  toolbarSaving.value = true
  try {
    await pane.saveScoresToDb()
  } finally {
    toolbarSaving.value = false
  }
}

async function onToolbarReload() {
  const pane = activeAhpPaneRef()
  if (!pane?.reloadScoresFromDb) return
  toolbarLoading.value = true
  try {
    await pane.reloadScoresFromDb()
  } finally {
    toolbarLoading.value = false
  }
}

function onToolbarSimulate() {
  activeAhpPaneRef()?.openSimulateDialog?.()
}

function onAhpExpertOptions(list) {
  if (Array.isArray(list) && list.length) {
    expertOptionsForAhp.value = list
  }
}

const handleEquipmentWeightsCalculated = (data) => {
  // 装备操作 AHP 权重计算后的处理（可根据业务需求扩展）
  console.log('装备操作 AHP 权重已计算', data)
}

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
  .ahp-unified-card {
    margin-bottom: 20px;

    :deep(.el-card__header) {
      padding: 14px 18px;
      background: linear-gradient(135deg, #0d3a66 0%, #1a5a8a 100%);
      border-bottom: 1px solid rgba(255, 255, 255, 0.12);
    }

    :deep(.el-card__body) {
      padding: 0;
    }
  }

  .ahp-unified-toolbar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 12px 20px;
    color: #fff;

    .header-icon {
      font-size: 22px;
      color: var(--accent-gold, #e6c35c);
      vertical-align: middle;
    }

    &__title {
      display: inline-flex;
      align-items: center;
      gap: 10px;
      font-size: 17px;
      font-weight: 600;
      flex: 1 1 auto;
    }

    &__expert {
      flex: 0 1 420px;
      display: flex;
      justify-content: flex-end;
      min-width: 0;
    }
  }

  .ahp-unified-body {
    background: #fafbfc;
  }

  .ahp-unified-legends-block {
    padding: 16px 18px 8px;
  }

  .ahp-unified-top-alert {
    margin-bottom: 16px;
  }

  .ahp-matrices-card {
    margin: 0 18px 20px;
    border: 1px solid #e4e7ed;

    :deep(.el-card__header) {
      padding: 12px 18px;
      background: linear-gradient(180deg, #f5f8fc 0%, #eef3f9 100%);
      border-bottom: 1px solid #e4e7ed;
    }

    :deep(.el-card__body) {
      padding: 16px 18px 20px;
    }
  }

  .ahp-matrices-card-header {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 12px;
  }

  .ahp-matrices-card-title-wrap {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }

  .ahp-matrices-card-title-wrap .el-icon {
    font-size: 20px;
    color: var(--navy-primary, #0d3a66);
  }

  .ahp-matrix-toolbar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: 14px;
    margin-bottom: 16px;
    padding-bottom: 14px;
    border-bottom: 1px dashed #d0d9e8;
  }

  .ahp-matrix-toolbar__actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .ahp-matrix-toolbar__tabs {
    flex: 1 1 auto;
    display: flex;
    justify-content: center;
    min-width: 280px;

    :deep(.el-radio-button__inner) {
      padding: 8px 20px;
      font-weight: 500;
    }
  }

  .ahp-unified-expert-form {
    margin-bottom: 0;

    :deep(.el-form-item) {
      margin-bottom: 0;
    }

    :deep(.el-form-item__label) {
      color: rgba(255, 255, 255, 0.92);
    }
  }

  .ahp-unified-panes {
    position: relative;
    padding: 0;
  }

  .ahp-unified-pane {
    min-height: 1px;
  }

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
