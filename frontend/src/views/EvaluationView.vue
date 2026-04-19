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
              请先在上方选择「当前专家」。先阅读 <strong>标度</strong>与<strong>把握度</strong>，再填写<strong>域间一级矩阵</strong>（效能 vs 装备）与下方两套<strong>下层 AHP 矩阵</strong>；切换 Tab 不丢失已填内容。
            </template>
            <template v-else>
              已选择专家：域间比较键为 <code>域间一级_效能指标_装备操作</code>；效能下层矩阵无「装备操作_」前缀，装备下层为 <code>装备操作_</code> 前缀。点「保存到数据库」会同时保存当前 Tab 下层矩阵与域间对。
            </template>
          </el-alert>
          <AhpSharedLegends />
        </div>

        <!-- 操作条：置于一级域间判断之上（保存 / 重载 / 批量模拟 + 下层 Tab） -->
        <div class="ahp-matrix-toolbar ahp-matrix-toolbar--above-cross">
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

        <!-- 域间一级：效能指标体系 vs 装备操作体系（合并两套权重的依据） -->
        <el-card class="ahp-cross-domain-card" shadow="hover">
          <template #header>
            <span class="ahp-cross-domain-card-head">域间一级判断（合并两套权重）</span>
          </template>
          <CrossDomainAhpPanel
            ref="crossDomainPanelRef"
            :expert-id="sharedExpertId"
            :expert-name="sharedExpertName"
          />
        </el-card>

        <!-- 下层 AHP：效能 / 装备 各维度与指标矩阵 -->
        <el-card class="ahp-matrices-card" shadow="hover">
          <template #header>
            <div class="ahp-matrices-card-header">
              <div class="ahp-matrices-card-title-wrap">
                <el-icon><Grid /></el-icon>
                <span class="ahp-matrices-card-title">AHP 判断矩阵</span>
              </div>
              <el-tag type="info" effect="plain" size="small">
                各体系内部的维度层与指标层；请先选择页顶「当前专家」
              </el-tag>
            </div>
          </template>
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

        <!-- 完整全局权重：库内统一快照（域间 w_eff/w_eq × 两套体系层次展开），依赖已保存的打分 -->
        <el-card v-if="sharedExpertId" class="ahp-unified-leaves-card" shadow="hover">
          <template #header>
            <div class="ahp-unified-leaves-head">
              <span class="ahp-unified-leaves-title">
                <el-icon><Histogram /></el-icon>
                完整全局权重（效能 + 装备 + 域间一级）
              </span>
              <el-tag type="warning" effect="plain" size="small">需已将域间、效能、装备矩阵保存到数据库</el-tag>
            </div>
          </template>
          <el-alert type="info" :closable="false" show-icon class="ahp-unified-leaves-alert">
            加载后展示与上方 AHP 结果区一致的分块样式：<strong>一级</strong>域间体系权重、<strong>二级</strong>维度层（对总目标与体系内）、<strong>三级</strong>叶子指标及<strong>旭日图</strong>（内圈体系·中圈维度·外圈叶子）。数据来自
            <code>expert_ahp_individual_weights</code>。若尚未保存矩阵，请先「保存到数据库」再「重算并加载」。
          </el-alert>
          <div class="ahp-unified-leaves-actions">
            <el-button type="primary" :loading="unifiedLoading" @click="refreshUnifiedAhpFromDb(true)">
              重算并加载完整全局权重
            </el-button>
            <el-button :loading="unifiedLoading" @click="refreshUnifiedAhpFromDb(false)">
              仅加载（不重算）
            </el-button>
            <span v-if="unifiedLeafSum > 0" class="ahp-unified-leaves-sum">
              叶子权重合计：{{ (unifiedLeafSum * 100).toFixed(4) }}%
            </span>
          </div>
          <UnifiedGlobalWeightsPanel v-if="unifiedAhpSnapshot" :snapshot="unifiedAhpSnapshot" />
          <el-empty v-else description="暂无数据：请先保存打分，再点击「重算并加载」" />
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
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Trophy, Setting, Grid, Histogram } from '@element-plus/icons-vue'
import { getExpertUnifiedWeights, recalculateExpertUnifiedWeights } from '@/api'
import AHPConfig from '@/components/AHPConfig.vue'
import EquipmentAhpConfig from '@/components/EquipmentAhpConfig.vue'
import AhpSharedLegends from '@/components/AhpSharedLegends.vue'
import CrossDomainAhpPanel from '@/components/CrossDomainAhpPanel.vue'
import UnifiedGlobalWeightsPanel from '@/components/UnifiedGlobalWeightsPanel.vue'
import DimensionCharts from '@/components/DimensionCharts.vue'

const activeAhpTab = ref('effectiveness')
/** 两页共用的专家 ID（v-show 保留子组件状态，专家选择统一在此） */
const sharedExpertId = ref(null)
const expertOptionsForAhp = ref([])

const ahpEffectivenessRef = ref(null)
const ahpEquipmentRef = ref(null)
const crossDomainPanelRef = ref(null)
const toolbarSaving = ref(false)
const toolbarLoading = ref(false)

const sharedExpertName = computed(() => {
  const e = expertOptionsForAhp.value.find((x) => x.expertId === sharedExpertId.value)
  return e?.expertName ?? ''
})

function activeAhpPaneRef() {
  return activeAhpTab.value === 'effectiveness' ? ahpEffectivenessRef.value : ahpEquipmentRef.value
}

async function onToolbarSave() {
  const pane = activeAhpPaneRef()
  if (!pane?.saveScoresToDb) return
  toolbarSaving.value = true
  try {
    await pane.saveScoresToDb()
    await crossDomainPanelRef.value?.saveToDb?.()
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
    await crossDomainPanelRef.value?.reload?.()
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

const handleEquipmentWeightsCalculated = () => {
  ElMessage.info({
    message:
      '装备侧层次总排序已算完。若需「效能+装备+域间」合并后的叶子全局权重，请先保存打分，再在本页底部「完整全局权重」卡片中点击加载。',
    duration: 6000
  })
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

const unifiedAhpSnapshot = ref(null)
const unifiedLoading = ref(false)

const unifiedLeafSum = computed(() => {
  const list = unifiedAhpSnapshot.value?.allLeaves
  if (!Array.isArray(list)) return 0
  return list.reduce((s, x) => s + (Number(x.globalWeight) || 0), 0)
})

async function refreshUnifiedAhpFromDb(recalculate) {
  if (!sharedExpertId.value) return
  unifiedLoading.value = true
  try {
    if (recalculate) {
      await recalculateExpertUnifiedWeights(sharedExpertId.value)
    }
    unifiedAhpSnapshot.value = await getExpertUnifiedWeights(sharedExpertId.value)
    ElMessage.success(recalculate ? '已重算并加载完整全局权重' : '已加载完整全局权重')
  } catch (e) {
    unifiedAhpSnapshot.value = null
    ElMessage.error(e?.message || '加载统一权重失败')
  } finally {
    unifiedLoading.value = false
  }
}

const handleWeightsCalculated = (payload) => {
  ahpWeights.value = payload
  ElMessage.info({
    message:
      '效能侧层次总排序已算完（见下方结果区）。若需「效能+装备+域间」合并后的叶子全局权重，请先保存打分，再在本页底部「完整全局权重」卡片中点击加载。',
    duration: 6000
  })
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

  .ahp-cross-domain-card {
    margin: 0 18px 16px;
    border: 1px solid #e4e7ed;

    :deep(.el-card__header) {
      padding: 10px 18px;
      background: #f8fafc;
      border-bottom: 1px solid #e4e7ed;
    }

    :deep(.el-card__body) {
      padding: 16px 18px 18px;
    }
  }

  .ahp-cross-domain-card-head {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
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

    &--above-cross {
      margin: 8px 18px 14px;
      padding: 14px 16px 16px;
      background: #fff;
      border: 1px solid #e4e7ed;
      border-radius: 8px;
      box-shadow: 0 1px 4px rgba(0, 31, 63, 0.06);
    }
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

  .ahp-unified-leaves-card {
    margin: 0 18px 20px;
    border: 1px solid #e4e7ed;

    :deep(.el-card__header) {
      padding: 12px 18px;
      background: linear-gradient(180deg, #f0f7ff 0%, #e8f2fc 100%);
      border-bottom: 1px solid #e4e7ed;
    }

    :deep(.el-card__body) {
      padding: 16px 18px 20px;
    }
  }

  .ahp-unified-leaves-head {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 10px 14px;
    justify-content: space-between;
  }

  .ahp-unified-leaves-title {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 600;
    color: #303133;

    .el-icon {
      font-size: 20px;
      color: var(--navy-primary, #0d3a66);
    }
  }

  .ahp-unified-leaves-alert {
    margin-bottom: 14px;
  }

  .ahp-unified-leaves-actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 10px;
    margin-bottom: 14px;
  }

  .ahp-unified-leaves-sum {
    margin-left: auto;
    font-size: 13px;
    color: #606266;
    font-variant-numeric: tabular-nums;
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
