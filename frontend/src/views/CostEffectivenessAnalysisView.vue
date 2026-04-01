<template>
  <div class="cost-effectiveness-page">
    <!-- ==================== 页面头部 ==================== -->
    <div class="page-header">
      <div>
        <h2>
          <el-icon><PieChart /></el-icon>
          评估结果计算 — 效费分析
        </h2>
        <p class="subtitle">
          评估通信系统在作战运用中的投入产出效率，为资源优化配置、装备论证及训练效益评估提供量化依据
        </p>
      </div>
    </div>

    <!-- ==================== 提示信息 ==================== -->
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="hint-alert"
      title="选择评估批次后，系统将自动获取该批次下的所有作战任务进行批量效费分析"
    />

    <!-- ==================== 批次选择 ==================== -->
    <el-card class="toolbar-card" shadow="hover">
      <el-form :inline="true" label-width="120px">
        <el-form-item label="评估批次">
          <el-select
            v-model="evaluationId"
            placeholder="请选择评估批次"
            clearable
            filterable
            style="width: 320px"
            @change="onBatchChange"
          >
            <el-option v-for="id in evaluationIds" :key="id" :label="id" :value="id" />
          </el-select>
        </el-form-item>
        <el-form-item label="作战任务">
          <el-tag type="info" size="large">
            {{ batchOperationCount }} 个任务（自动获取）
          </el-tag>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="DataAnalysis" :loading="computing" :disabled="!canCalculate" @click="calculate">
            执行效费分析
          </el-button>
          <el-button :icon="Download" :disabled="!evaluationId" @click="loadResults">
            加载已保存
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ==================== 一、效费分析理论说明 ==================== -->
    <el-card class="content-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><Document /></el-icon> 一、效费分析理论</span>
          <el-button text size="small" @click="showTheory = !showTheory">
            <el-icon><ArrowUp v-if="showTheory" /><ArrowDown v-else /></el-icon>
            {{ showTheory ? '收起' : '展开详情' }}
          </el-button>
        </div>
      </template>
      <div v-if="showTheory" class="theory-section">
        <h4>1.1 成本指标归一化（min-max 变换）</h4>
        <p>设共选取 p 个成本指标，每个指标有原始测量值 c<sub>k</sub>（k=1,2,…,p）。由于量纲不同，需进行归一化处理，得到成本分量 c<sub>k</sub>′∈[0,1]。</p>
        <div class="formula-box">
          <div>成本型指标（数值越大表示成本越高）：</div>
          <code>c<sub>k</sub>′ = (c<sub>k</sub> - c<sub>k,min</sub>) / (c<sub>k,max</sub> - c<sub>k,min</sub>)</code>
          <div style="margin-top: 8px">效益型指标（数值越大表示成本越低）：</div>
          <code>c<sub>k</sub>′ = (c<sub>k,max</sub> - c<sub>k</sub>) / (c<sub>k,max</sub> - c<sub>k,min</sub>)</code>
        </div>

        <h4>1.2 成本指数合成</h4>
        <p>采用加权求和得到综合成本指数 C：</p>
        <div class="formula-box">
          <code>C = Σ<sub>k=1</sub><sup>p</sup> ξ<sub>k</sub> · c<sub>k</sub>′</code>
          <div style="margin-top: 4px; font-size: 12px; color: #909399">其中 ξ<sub>k</sub> 为各成本指标的权重，满足 Σξ<sub>k</sub> = 1</div>
        </div>

        <h4>1.3 效费比计算</h4>
        <p>以最终效能得分 S<sub>final</sub> 作为效能指数 E，则效费比 R 定义为：</p>
        <div class="formula-box">
          <code>R = E / C</code>
          <div style="margin-top: 4px; font-size: 12px; color: #909399">R 值越大，表示单位投入获得的作战效能越高</div>
        </div>

        <h4>1.4 边际分析</h4>
        <p>通过调整某项投入，观察效能得分的变化，分析边际效费比：</p>
        <div class="formula-box">
          <code>MR = ΔE / ΔC</code>
          <div style="margin-top: 4px; font-size: 12px; color: #909399">当 MR > R<sub>当前</sub> 时，增加投入可提升效率；当 MR &lt; R<sub>当前</sub> 时，效率下降</div>
        </div>
      </div>
    </el-card>

    <!-- ==================== 二、成本指标配置 ==================== -->
    <el-card class="content-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><Setting /></el-icon> 二、成本指标配置</span>
          <div class="header-actions">
            <el-tag type="info" size="small">共 {{ activeIndicators.length }} 个指标</el-tag>
            <el-button size="small" :icon="Refresh" @click="loadIndicators">刷新配置</el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeCategory" class="category-tabs">
        <el-tab-pane label="全部" name="all">
          <IndicatorTable :indicators="activeIndicators" :editable="false" />
        </el-tab-pane>
        <el-tab-pane v-for="cat in categories" :key="cat" :label="cat" :name="cat">
          <IndicatorTable :indicators="getIndicatorsByCategory(cat)" :editable="false" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- ==================== 三、成本数据预览 ==================== -->
    <el-card v-if="operationDataList.length > 0" class="content-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><List /></el-icon> 三、成本数据预览</span>
          <el-tag type="warning" size="small">已选择 {{ batchOperationCount }} 个作战任务</el-tag>
        </div>
      </template>
      <el-table :data="operationDataList" border stripe size="small" max-height="320" class="cost-data-table">
        <el-table-column prop="operationId" label="作战ID" width="100" fixed align="center" />
        <el-table-column v-for="col in dataColumns" :key="col.key" :prop="col.key" :label="col.label" width="130" align="center">
          <template #default="{ row }">
            {{ formatValue(row[col.key], col.format) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- ==================== 四、效费分析结果 ==================== -->
    <el-card v-if="resultData.length > 0" class="content-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><Finished /></el-icon> 四、效费分析结果</span>
          <div class="header-actions">
            <el-tag type="warning" size="small">批次: {{ evaluationId }}</el-tag>
            <el-tag v-if="statistics" type="success" size="small">
              平均效费比: {{ statistics.avgCostEffectivenessRatio?.toFixed(4) || '—' }}
            </el-tag>
            <el-tag v-if="statistics" type="info" size="small">
              成本指数: {{ statistics.avgCostIndex?.toFixed(4) || '—' }}
            </el-tag>
          </div>
        </div>
      </template>

      <el-table :data="resultData" border stripe size="small" max-height="400" class="result-table">
        <el-table-column prop="evaluationId" label="评估批次" width="160" fixed align="center">
          <template #default="{ row }">
            <el-tag type="warning" size="small">{{ row.evaluationId || '—' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operationId" label="作战ID" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="primary" size="small">实验{{ row.operationId }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="成本指数 C" width="140" align="center">
          <template #default="{ row }">
            <span class="cost-index">{{ row.costIndex?.toFixed(4) || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="效能得分 E" width="140" align="center">
          <template #default="{ row }">
            <span class="effect-score">{{ row.effectivenessScore?.toFixed(4) || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="效费比 R" width="140" fixed align="center">
          <template #default="{ row }">
            <span class="ratio-value" :class="getRatioClass(row.costEffectivenessRatio)">
              {{ row.costEffectivenessRatio?.toFixed(4) || '—' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="效费等级" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getGradeType(row.efficiencyGrade)" size="small">
              {{ row.efficiencyGrade || '—' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="成本构成" min-width="300" align="center">
          <template #default="{ row }">
            <div class="cost-breakdown">
              <span v-for="(val, cat) in row.costByCategory" :key="cat" class="cost-item">
                <span class="cat-label">{{ cat }}</span>
                <span class="cat-value">{{ val?.toFixed(4) || '—' }}</span>
              </span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- ==================== 五、结果图表展示 ==================== -->
    <el-card v-if="resultData.length > 0" class="content-card chart-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><TrendCharts /></el-icon> 五、结果图表展示</span>
          <div class="chart-controls">
            <el-radio-group v-model="chartType" size="small">
              <el-radio-button value="ratio">效费比对比</el-radio-button>
              <el-radio-button value="cost">成本指数对比</el-radio-button>
              <el-radio-button value="scatter">成本-效能散点图</el-radio-button>
              <el-radio-button value="radar">成本结构雷达图</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>

      <div class="chart-container" ref="chartRef" />
    </el-card>

    <!-- ==================== 六、边际分析（可选扩展） ==================== -->
    <el-card v-if="resultData.length > 1" class="content-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><Connection /></el-icon> 六、边际分析</span>
          <el-tag type="info" size="small">基于已计算的作战任务数据</el-tag>
        </div>
      </template>

      <el-alert type="info" :closable="false" show-icon class="margin-analysis-hint">
        边际分析通过比较相邻作战任务的投入变化与效能变化，评估增加投入的边际效益。
        当边际效费比 MR > 当前平均效费比 R 时，增加投入可提升整体效率。
      </el-alert>

      <el-table :data="marginalAnalysisData" border stripe size="small" class="marginal-table">
        <el-table-column label="对比组" width="180" align="center">
          <template #default="{ row }">
            {{ row.comparison }}
          </template>
        </el-table-column>
        <el-table-column label="Δ成本" width="120" align="center">
          <template #default="{ row }">
            <span :class="row.deltaCost > 0 ? 'delta-cost' : 'delta-benefit'">
              {{ row.deltaCost?.toFixed(4) || '—' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="Δ效能" width="120" align="center">
          <template #default="{ row }">
            <span :class="row.deltaEffect > 0 ? 'delta-benefit' : 'delta-cost'">
              {{ row.deltaEffect?.toFixed(4) || '—' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="边际效费比 MR" width="160" align="center">
          <template #default="{ row }">
            <span class="marginal-ratio" :class="getMarginalClass(row.marginalRatio)">
              {{ row.marginalRatio?.toFixed(4) || '—' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="评价" align="center">
          <template #default="{ row }">
            <el-tag :type="row.evaluationType" size="small">{{ row.evaluation }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- ==================== 加载中状态 ==================== -->
    <div v-if="computing" class="loading-overlay">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>正在计算效费分析...</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import {
  PieChart, DataAnalysis, Document, Setting, List, Finished,
  TrendCharts, Connection, Refresh, Download, ArrowUp, ArrowDown, Loading
} from '@element-plus/icons-vue'
import {
  getCollectiveEvaluationIds, getOperationIdsByEvaluationId, getCostRawDataPreview,
  getCostIndicators, getCostCategories, getCostEffectivenessResults,
  calculateCostEffectiveness
} from '@/api'

// ==================== 状态 ====================
const evaluationIds = ref([])
const evaluationId = ref('')
const batchOperationIds = ref([])  // 当前批次的所有作战任务ID
const computing = ref(false)
const showTheory = ref(true)
const activeCategory = ref('all')

// 指标配置
const activeIndicators = ref([])
const categories = ref([])

// 计算结果
const resultData = ref([])
const statistics = ref(null)
const normalizationBounds = ref(null)
const weightsConfig = ref(null)

// 原始成本数据（从 records_military_operation_info 表，用于预览）
const operationRawData = ref([])   // 数组，每项含 operationId 和各指标原始值

// 图表
const chartRef = ref(null)
const chartType = ref('ratio')
let chartInstance = null

// ==================== 计算属性 ====================
const canCalculate = computed(() => {
  return evaluationId.value && batchOperationIds.value.length > 0
})

// 批次作战任务数量
const batchOperationCount = computed(() => batchOperationIds.value.length)

// 成本数据预览列（从指标配置动态生成）
const dataColumns = computed(() => {
  if (!activeIndicators.value.length) return []
  return activeIndicators.value.slice(0, 8).map(ind => ({
    key: ind.indicatorKey,
    label: ind.indicatorName,
    format: ind.unit
  }))
})

// 操作数据列表（直接使用已获取的原始数据）
const operationDataList = computed(() => {
  return operationRawData.value.map(row => ({
    operationId: row.operationId,
    ...row
  }))
})

// 边际分析数据
const marginalAnalysisData = computed(() => {
  if (resultData.value.length < 2) return []

  const results = [...resultData.value].sort((a, b) =>
    Number(a.costIndex || 0) - Number(b.costIndex || 0)
  )
  const avgRatio = statistics.value?.avgCostEffectivenessRatio || 0

  const analysis = []
  for (let i = 0; i < results.length - 1; i++) {
    const curr = results[i]
    const next = results[i + 1]

    const deltaCost = Number(next.costIndex || 0) - Number(curr.costIndex || 0)
    const deltaEffect = Number(next.effectivenessScore || 0) - Number(curr.effectivenessScore || 0)
    const marginalRatio = deltaCost !== 0 ? deltaEffect / deltaCost : 0

    let evaluation = ''
    let evaluationType = 'info'

    if (marginalRatio > avgRatio) {
      evaluation = '增加投入可提升效率'
      evaluationType = 'success'
    } else if (marginalRatio < 0) {
      evaluation = '投入增加但效能下降'
      evaluationType = 'danger'
    } else {
      evaluation = '边际效率偏低'
      evaluationType = 'warning'
    }

    analysis.push({
      comparison: `${curr.evaluationId || ''}实验${curr.operationId} → ${next.evaluationId || ''}实验${next.operationId}`,
      deltaCost,
      deltaEffect,
      marginalRatio,
      evaluation,
      evaluationType
    })
  }

  return analysis
})

// ==================== 方法 ====================

/** 加载评估批次 */
async function loadEvaluationIds() {
  try {
    // 拦截器已解包，直接得到 List<String>
    const ids = await getCollectiveEvaluationIds()
    evaluationIds.value = Array.isArray(ids) ? ids : []
  } catch (e) {
    console.error(e)
    evaluationIds.value = []
  }
}

/** 加载作战任务列表（初始加载，批次对应的任务在 onBatchChange 中单独获取） */
async function loadOperationIds() {
  // 批次对应的作战任务在选择批次时通过 getOperationIdsByEvaluationId 获取
  // 此处无需额外操作
}

/** 加载指标配置 */
async function loadIndicators() {
  try {
    // 拦截器已解包，直接得到数据
    const [indicators, cats] = await Promise.all([
      getCostIndicators(),
      getCostCategories()
    ])
    activeIndicators.value = indicators || []
    categories.value = cats || []
  } catch (e) {
    console.error(e)
    activeIndicators.value = []
    categories.value = []
  }
}

/** 执行效费分析计算 */
async function calculate() {
  if (!canCalculate.value) {
    ElMessage.warning('请选择评估批次和作战任务')
    return
  }

  computing.value = true
  try {
    // 后端返回 ApiResponse，拦截器已解包，直接得到 CostEffectivenessCalculateResponse
    const resp = await calculateCostEffectiveness({
      evaluationId: evaluationId.value,
      operationIds: batchOperationIds.value,
      weightMethod: 'equal',
      normalizationMethod: 'minmax',
      useActualRange: true
    })

    if (resp && resp.results && resp.results.length > 0) {
      resultData.value = resp.results
      statistics.value = resp.statistics || null
      normalizationBounds.value = resp.normalizationBounds || null
      weightsConfig.value = resp.weightsConfig || null
      // 清空原始数据预览（已切换到计算结果视图）
      operationRawData.value = []
      ElMessage.success(`效费分析计算完成，共 ${resultData.value.length} 条记录`)
      await nextTick()
      renderChart()
    } else {
      ElMessage.warning('计算完成但无结果数据，请检查成本指标配置和效能得分数据')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error(e?.message || '计算失败')
  } finally {
    computing.value = false
  }
}

/** 加载已保存的结果 */
async function loadResults() {
  if (!evaluationId.value) {
    ElMessage.warning('请先选择评估批次')
    return
  }

  try {
    // 拦截器已解包，直接得到 List<CostEffectivenessResultDTO>
    const results = await getCostEffectivenessResults(evaluationId.value)
    const resultsList = Array.isArray(results) ? results : []
    if (resultsList && resultsList.length > 0) {
      resultData.value = resultsList
      batchOperationIds.value = resultsList.map(r => r.operationId)
      // 清空原始数据预览（已切换到结果视图）
      operationRawData.value = []
      // 计算统计信息
      const ratios = results.map(r => Number(r.costEffectivenessRatio || 0))
      const costs = results.map(r => Number(r.costIndex || 0))
      const effects = results.map(r => Number(r.effectivenessScore || 0))

      statistics.value = {
        avgCostEffectivenessRatio: ratios.reduce((a, b) => a + b, 0) / ratios.length,
        minCostEffectivenessRatio: Math.min(...ratios),
        maxCostEffectivenessRatio: Math.max(...ratios),
        avgCostIndex: costs.reduce((a, b) => a + b, 0) / costs.length,
        minCostIndex: Math.min(...costs),
        maxCostIndex: Math.max(...costs),
        avgEffectivenessScore: effects.reduce((a, b) => a + b, 0) / effects.length,
        minEffectivenessScore: Math.min(...effects),
        maxEffectivenessScore: Math.max(...effects)
      }

      ElMessage.success(`已加载 ${results.length} 条保存的结果`)
      await nextTick()
      renderChart()
    } else {
      ElMessage.info('该批次暂无保存的结果')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error(e?.message || '加载失败')
  }
}

/** 按类别获取指标 */
function getIndicatorsByCategory(category) {
  return activeIndicators.value.filter(ind => ind.category === category)
}

/** 格式化数值 */
function formatValue(value, format) {
  if (value == null) return '—'
  const num = Number(value)
  if (isNaN(num)) return '—'
  return num.toFixed(2) + (format ? ' ' + format : '')
}

/** 效费比样式 */
function getRatioClass(ratio) {
  if (ratio == null) return ''
  if (ratio >= 1.5) return 'ratio-excellent'
  if (ratio >= 1.2) return 'ratio-good'
  if (ratio >= 1.0) return 'ratio-pass'
  return 'ratio-poor'
}

/** 效费等级标签类型 */
function getGradeType(grade) {
  const types = {
    '优秀': 'success',
    '良好': '',
    '合格': 'warning',
    '较差': 'danger',
    '差': 'danger'
  }
  return types[grade] || 'info'
}

/** 边际效费比样式 */
function getMarginalClass(ratio) {
  if (ratio == null) return ''
  const avgRatio = statistics.value?.avgCostEffectivenessRatio || 0
  if (ratio > avgRatio) return 'marginal-good'
  if (ratio < 0) return 'marginal-bad'
  return 'marginal-normal'
}

/** 批次变更 - 自动获取批次中的作战任务及其原始成本数据 */
async function onBatchChange() {
  resultData.value = []
  statistics.value = null
  batchOperationIds.value = []
  operationRawData.value = []
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }

  if (!evaluationId.value) return

  try {
    // Step 1: 获取该批次包含的作战任务ID列表（拦截器已解包）
    const ids = await getOperationIdsByEvaluationId(evaluationId.value)
    const idList = Array.isArray(ids) ? ids : []
    batchOperationIds.value = idList.map(id => String(id))

    if (idList.length === 0) {
      ElMessage.warning('该批次暂无作战任务数据')
      return
    }

    // Step 2: 获取这些作战任务的原始成本数据（用于预览表格）
    const previewData = await getCostRawDataPreview(evaluationId.value, idList)
    operationRawData.value = Array.isArray(previewData) ? previewData : []

    ElMessage.success(`已加载 ${idList.length} 个作战任务，原始数据 ${operationRawData.value.length} 条`)
  } catch (e) {
    console.error('获取批次作战任务失败:', e)
    ElMessage.error('获取作战任务列表失败: ' + (e?.message || ''))
  }
}

// ==================== 图表渲染 ====================
function renderChart() {
  if (!chartRef.value || !resultData.value.length) return
  if (!chartInstance) chartInstance = echarts.init(chartRef.value)

  switch (chartType.value) {
    case 'ratio':
      renderRatioChart()
      break
    case 'cost':
      renderCostChart()
      break
    case 'scatter':
      renderScatterChart()
      break
    case 'radar':
      renderRadarChart()
      break
  }
}

function renderRatioChart() {
  const labels = resultData.value.map(r => `${r.evaluationId}\n实验${r.operationId}`)
  const ratios = resultData.value.map(r => Number(r.costEffectivenessRatio || 0))

  chartInstance.setOption({
    title: {
      text: `效费比对比图 [${evaluationId.value}]`,
      left: 'center',
      textStyle: { fontSize: 15 }
    },
    tooltip: {
      trigger: 'axis',
      formatter(params) {
        if (!params?.length) return ''
        const p = params[0]
        const parts = p.name.split('\n')
        return `<strong>批次: ${parts[0]}</strong><br/><strong>实验${parts[1]}</strong><br/>效费比: <strong>${Number(p.value).toFixed(4)}</strong>`
      }
    },
    grid: { left: '3%', right: '4%', top: 52, bottom: 38, containLabel: true },
    xAxis: {
      type: 'category',
      data: labels,
      axisLabel: { fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      name: '效费比 R',
      axisLabel: { formatter: v => v.toFixed(2) },
      splitLine: { lineStyle: { type: 'dashed' } }
    },
    series: [{
      type: 'bar',
      data: ratios,
      itemStyle: {
        color: params => {
          const val = params.value
          if (val >= 1.5) return '#67C23A'
          if (val >= 1.2) return '#409EFF'
          if (val >= 1.0) return '#E6A23C'
          return '#F56C6C'
        }
      },
      barMaxWidth: 40,
      label: {
        show: true,
        position: 'top',
        formatter: p => p.value.toFixed(2)
      }
    }]
  }, true)
}

function renderCostChart() {
  const labels = resultData.value.map(r => `${r.evaluationId}\n实验${r.operationId}`)
  const costs = resultData.value.map(r => Number(r.costIndex || 0))

  chartInstance.setOption({
    title: {
      text: `成本指数对比图 [${evaluationId.value}]`,
      left: 'center',
      textStyle: { fontSize: 15 }
    },
    tooltip: {
      trigger: 'axis',
      formatter(params) {
        if (!params?.length) return ''
        const p = params[0]
        const parts = p.name.split('\n')
        return `<strong>批次: ${parts[0]}</strong><br/><strong>实验${parts[1]}</strong><br/>成本指数: <strong>${Number(p.value).toFixed(4)}</strong>`
      }
    },
    grid: { left: '3%', right: '4%', top: 52, bottom: 38, containLabel: true },
    xAxis: {
      type: 'category',
      data: labels,
      axisLabel: { fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      name: '成本指数 C',
      min: 0,
      max: 1,
      axisLabel: { formatter: v => v.toFixed(2) },
      splitLine: { lineStyle: { type: 'dashed' } }
    },
    series: [{
      type: 'bar',
      data: costs,
      itemStyle: { color: '#409EFF' },
      barMaxWidth: 40,
      label: {
        show: true,
        position: 'top',
        formatter: p => p.value.toFixed(2)
      }
    }]
  }, true)
}

function renderScatterChart() {
  const data = resultData.value.map(r => [
    Number(r.costIndex || 0),
    Number(r.effectivenessScore || 0),
    r.operationId,
    r.evaluationId
  ])

  chartInstance.setOption({
    title: {
      text: `成本-效能散点图 [${evaluationId.value}]`,
      left: 'center',
      textStyle: { fontSize: 15 }
    },
    tooltip: {
      formatter(params) {
        return `<strong>批次: ${params.value[3]}</strong><br/><strong>实验${params.value[2]}</strong><br/>成本指数: ${params.value[0].toFixed(4)}<br/>效能得分: ${params.value[1].toFixed(4)}`
      }
    },
    grid: { left: '3%', right: '4%', top: 52, bottom: 38, containLabel: true },
    xAxis: {
      type: 'value',
      name: '成本指数 C',
      axisLabel: { formatter: v => v.toFixed(2) },
      splitLine: { lineStyle: { type: 'dashed' } }
    },
    yAxis: {
      type: 'value',
      name: '效能得分 E',
      axisLabel: { formatter: v => v.toFixed(2) },
      splitLine: { lineStyle: { type: 'dashed' } }
    },
    series: [{
      type: 'scatter',
      data,
      symbolSize: 14,
      itemStyle: {
        color: params => {
          const ratio = params.value[1] / params.value[0]
          if (ratio >= 1.5) return '#67C23A'
          if (ratio >= 1.2) return '#409EFF'
          if (ratio >= 1.0) return '#E6A23C'
          return '#F56C6C'
        }
      },
      label: {
        show: true,
        position: 'right',
        formatter: params => `实验${params.value[2]}`,
        fontSize: 10
      }
    }]
  }, true)
}

function renderRadarChart() {
  // 按类别汇总成本
  const categoryData = {}
  const categorySet = new Set()

  resultData.value.forEach(r => {
    if (r.costByCategory) {
      Object.entries(r.costByCategory).forEach(([cat, val]) => {
        if (!categoryData[r.operationId]) categoryData[r.operationId] = {}
        categoryData[r.operationId][cat] = Number(val || 0)
        categorySet.add(cat)
      })
    }
  })

  const categories = Array.from(categorySet)
  const maxValues = {}
  categories.forEach(cat => {
    maxValues[cat] = Math.max(...resultData.value.map(r =>
      Math.abs(Number(r.costByCategory?.[cat] || 0))
    ))
  })

  const indicator = categories.map(cat => ({
    name: cat,
    max: maxValues[cat] * 1.2 || 1
  }))

  const seriesData = resultData.value.map(r => ({
    value: categories.map(cat => Math.abs(Number(r.costByCategory?.[cat] || 0))),
    name: `${r.evaluationId}\n实验${r.operationId}`
  }))

  chartInstance.setOption({
    title: {
      text: `成本结构雷达图 [${evaluationId.value}]`,
      left: 'center',
      textStyle: { fontSize: 15 }
    },
    tooltip: {},
    legend: {
      data: seriesData.map(s => s.name.replace('\n', ' ')),
      bottom: 0,
      textStyle: { fontSize: 10 }
    },
    radar: {
      indicator,
      radius: '60%'
    },
    series: [{
      type: 'radar',
      data: seriesData
    }]
  }, true)
}

// ==================== 生命周期 ====================
watch(chartType, () => {
  nextTick(() => renderChart())
})

onMounted(async () => {
  await Promise.all([
    loadEvaluationIds(),
    loadOperationIds(),
    loadIndicators()
  ])
})
</script>

<script>
// 指标表格组件
import { defineComponent, h } from 'vue'
import { ElTable, ElTableColumn, ElTag } from 'element-plus'

const IndicatorTable = defineComponent({
  name: 'IndicatorTable',
  props: {
    indicators: { type: Array, default: () => [] },
    editable: { type: Boolean, default: false }
  },
  setup(props) {
    return () => h(ElTable, {
      data: props.indicators,
      border: true,
      stripe: true,
      size: 'small',
      maxHeight: 280
    }, () => [
      h(ElTableColumn, { prop: 'indicatorName', label: '指标名称', width: 160 }),
      h(ElTableColumn, { prop: 'indicatorKey', label: '字段标识', width: 200 }),
      h(ElTableColumn, { prop: 'category', label: '类别', width: 100, align: 'center' }),
      h(ElTableColumn, { prop: 'indicatorType', label: '类型', width: 100, align: 'center' }, {
        default: ({ row }) => h(ElTag, {
          type: row.indicatorType === 'cost' ? 'danger' : 'success',
          size: 'small'
        }, () => row.indicatorType === 'cost' ? '成本型' : '效益型')
      }),
      h(ElTableColumn, { prop: 'unit', label: '单位', width: 80, align: 'center' }),
      h(ElTableColumn, { prop: 'weight', label: '权重', width: 80, align: 'center' }, {
        default: ({ row }) => row.weight ? Number(row.weight).toFixed(3) : '—'
      }),
      h(ElTableColumn, { prop: 'description', label: '说明', minWidth: 150 })
    ])
  }
})

export default {
  components: { IndicatorTable }
}
</script>

<style scoped lang="scss">
.cost-effectiveness-page {
  padding: 0;
}

.page-header {
  margin-bottom: 16px;
  h2 {
    display: flex;
    align-items: center;
    gap: 10px;
    margin: 0 0 8px 0;
    font-size: 22px;
    color: #303133;
  }
  .subtitle {
    margin: 0;
    color: #909399;
    font-size: 14px;
  }
}

.hint-alert {
  margin-bottom: 16px;
}

.toolbar-card {
  margin-bottom: 16px;
}

.content-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;

  .header-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }
}

// 理论说明区
.theory-section {
  h4 {
    margin: 16px 0 8px;
    color: #303133;
    font-size: 14px;
  }
  p {
    margin: 6px 0;
    line-height: 1.7;
    color: #606266;
    font-size: 13px;
  }
  .formula-box {
    background: #f5f7fa;
    border: 1px solid #e4e8ef;
    border-radius: 6px;
    padding: 10px 16px;
    margin: 8px 0;
    font-family: 'Consolas', monospace;
    font-size: 13px;
    color: #303133;
    line-height: 1.8;
  }
}

// 成本数据表格
.cost-data-table {
  .cell {
    font-size: 12px;
  }
}

// 结果表格
.result-table {
  .cost-index {
    color: #409EFF;
    font-weight: 500;
  }
  .effect-score {
    color: #67C23A;
    font-weight: 500;
  }
  .ratio-value {
    font-weight: 700;
    &.ratio-excellent { color: #67C23A; }
    &.ratio-good { color: #409EFF; }
    &.ratio-pass { color: #E6A23C; }
    &.ratio-poor { color: #F56C6C; }
  }
}

.cost-breakdown {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: center;

  .cost-item {
    display: inline-flex;
    flex-direction: column;
    align-items: center;
    padding: 2px 6px;
    background: #f5f7fa;
    border-radius: 4px;
    font-size: 10px;

    .cat-label {
      color: #909399;
    }
    .cat-value {
      color: #409EFF;
      font-weight: 500;
    }
  }
}

// 图表区
.chart-card {
  .chart-controls {
    display: flex;
    gap: 8px;
  }
}

.chart-container {
  width: 100%;
  height: 420px;
}

// 边际分析
.margin-analysis-hint {
  margin-bottom: 12px;
}

.marginal-table {
  .delta-cost { color: #F56C6C; }
  .delta-benefit { color: #67C23A; }
  .marginal-ratio {
    font-weight: 600;
    &.marginal-good { color: #67C23A; }
    &.marginal-normal { color: #E6A23C; }
    &.marginal-bad { color: #F56C6C; }
  }
}

// 加载状态
.loading-overlay {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(255, 255, 255, 0.95);
  padding: 24px 40px;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 12px;
  z-index: 1000;

  .el-icon {
    font-size: 24px;
    color: #409EFF;
  }
}

.category-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 12px;
  }
}
</style>
