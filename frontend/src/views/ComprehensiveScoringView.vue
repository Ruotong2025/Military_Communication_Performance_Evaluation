<template>
  <div class="comprehensive-scoring-page">
    <div class="page-header">
      <div>
        <h2><el-icon><DataAnalysis /></el-icon> 综合评分</h2>
        <p class="subtitle">基于集结权重的效能指标、装备定量指标、装备定性指标综合评估</p>
      </div>
    </div>

    <el-alert
      type="warning"
      :closable="false"
      show-icon
      class="hint-alert"
      title="重要：效能数据和装备数据必须使用相同的批次ID才能正确关联计算。"
    />

    <!-- 批次选择和操作 -->
    <el-card class="toolbar-card" shadow="hover">
      <el-form :inline="true" label-width="100px">
        <el-form-item label="评估批次">
          <el-select
            v-model="evaluationId"
            placeholder="请选择评估批次"
            clearable
            filterable
            style="width: 380px"
            @change="onBatchChange"
          >
            <el-option v-for="id in evaluationBatches" :key="id" :label="id" :value="id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Refresh" :loading="calculating" :disabled="!evaluationId" @click="calculateScores">
            计算综合评分
          </el-button>
          <el-button type="success" :icon="List" :loading="loading" :disabled="!evaluationId" @click="loadResults">
            加载结果
          </el-button>
          <el-button type="danger" :icon="Delete" :disabled="!evaluationId" @click="deleteResults">
            删除结果
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 权重快照信息 -->
    <el-card v-if="weightSnapshot" class="weight-info-card" shadow="hover">
      <template #header>
        <span><el-icon><Key /></el-icon> 当前集结权重</span>
      </template>
      <div class="weight-info">
        <el-tag type="primary" size="large">效能体系权重: {{ Number(weightSnapshot.effDomainWeight || 0.5).toFixed(4) }}</el-tag>
        <el-tag type="success" size="large">装备体系权重: {{ Number(weightSnapshot.eqDomainWeight || 0.5).toFixed(4) }}</el-tag>
      </div>
    </el-card>

    <!-- 综合评分结果 -->
    <el-card v-if="results.length > 0" class="content-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><Finished /></el-icon> 综合评分结果</span>
          <el-tag type="info">{{ results.length }} 个作战</el-tag>
        </div>
      </template>

      <div class="result-section">
        <div class="result-toolbar">
          <h5>综合总分对比</h5>
          <el-radio-group v-model="totalScoreChartType" size="small">
            <el-radio-button value="bar">柱状图</el-radio-button>
            <el-radio-button value="line">折线图</el-radio-button>
          </el-radio-group>
        </div>
        <div ref="totalScoreChartRef" class="chart-container-large"></div>
      </div>

      <div class="result-section">
        <div class="result-toolbar">
          <h5>各维度得分对比</h5>
          <div class="filter-group">
            <el-checkbox-group v-model="selectedDomains" size="small">
              <el-checkbox-button value="eff">效能维度</el-checkbox-button>
              <el-checkbox-button value="eq">装备维度</el-checkbox-button>
            </el-checkbox-group>
            <el-select v-model="selectedDimensions" multiple collapse-tags collapse-tags-tooltip placeholder="选择维度" size="small" style="width: 300px; margin-left: 8px">
              <el-option v-for="dim in availableDimensions" :key="dim" :label="dim" :value="dim" />
            </el-select>
          </div>
        </div>
        <div ref="domainScoreChartRef" class="chart-container-large"></div>
      </div>

      <!-- 指标细分图表 -->
      <div v-for="(indData, dimName) in indicatorScores" :key="dimName" class="result-section">
        <div class="result-toolbar">
          <h5>{{ dimName }} - 指标细分 <el-tag size="small" type="info">权重: {{ formatScore(dimensionWeights[dimName]) }}</el-tag></h5>
        </div>
        <div :ref="el => { if (el) indicatorChartRefs[dimName] = el }" class="chart-container-large"></div>
      </div>

      <div class="result-section">
        <h5>综合得分排名</h5>
        <el-table :data="rankedResults" border stripe size="small">
          <el-table-column type="index" label="排名" width="80" align="center" />
          <el-table-column prop="operationId" label="作战ID" width="120" align="center" />
          <el-table-column label="综合总分" align="center">
            <template #default="{ row }">
              <span class="score-cell total"><strong>{{ formatScore(row.totalScore) }}</strong></span>
            </template>
          </el-table-column>
          <el-table-column label="效能分" align="center">
            <template #default="{ row }">
              <span class="score-cell eff">{{ formatScore(row.effectivenessScore) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="装备分" align="center">
            <template #default="{ row }">
              <span class="score-cell eq">{{ formatScore(row.equipmentScore) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { DataAnalysis, Finished, Delete, Refresh, List, Key } from '@element-plus/icons-vue'
import {
  getComprehensiveBatches,
  calculateComprehensiveScores,
  getComprehensiveResults,
  deleteComprehensiveResults
} from '../api/index'

const evaluationBatches = ref([])
const evaluationId = ref('')
const results = ref([])
const weightSnapshot = ref(null)
const calculating = ref(false)
const loading = ref(false)
const dimensionWeights = ref({})

const totalScoreChartRef = ref(null)
const domainScoreChartRef = ref(null)
let totalScoreChart = null
let domainScoreChart = null

// 指标图表引用
const indicatorChartRefs = {}
const indicatorCharts = {}

const totalScoreChartType = ref('bar')
const selectedDomains = ref(['eff', 'eq'])
const selectedDimensions = ref([])
const availableDimensions = ref([])

// 指标得分数据（用于指标细分图表）
const indicatorScores = ref({})

const rankedResults = computed(() => {
  return [...results.value]
    .filter(r => r.totalScore != null)
    .sort((a, b) => Number(b.totalScore) - Number(a.totalScore))
})

// 根据选择计算要显示的维度键
const selectedDimensionKeys = computed(() => {
  if (selectedDimensions.value.length === 0) {
    return availableDimensions.value
  }
  return selectedDimensions.value
})

function formatScore(val) {
  if (val == null) return '—'
  const n = Number(val)
  if (isNaN(n)) return '—'
  return n.toFixed(4)
}

function getScoreClass(val) {
  const n = Number(val)
  if (isNaN(n)) return ''
  if (n >= 0.8) return 'score-high'
  if (n >= 0.5) return 'score-mid'
  return 'score-low'
}

function getScoreTagType(val) {
  const n = Number(val)
  if (isNaN(n)) return 'info'
  if (n >= 0.8) return 'success'
  if (n >= 0.5) return 'warning'
  return 'danger'
}

async function loadBatches() {
  try {
    const data = await getComprehensiveBatches()
    evaluationBatches.value = Array.isArray(data) ? data : []
  } catch (e) {
    console.error(e)
    evaluationBatches.value = []
  }
}

function onBatchChange() {
  results.value = []
  weightSnapshot.value = null
  disposeCharts()
  // 选择批次后自动加载
  if (evaluationId.value) {
    // nothing to do
  }
}

async function calculateScores() {
  if (!evaluationId.value) return
  calculating.value = true
  try {
    const data = await calculateComprehensiveScores(evaluationId.value)
    if (data?.success) {
      results.value = data.results || []
      weightSnapshot.value = data.weightSnapshot || null
      // 从第一个结果中提取维度权重
      if (data.results && data.results.length > 0) {
        dimensionWeights.value = data.results[0].dimensionWeights || {}
      }
      extractAvailableDimensions()
      ElMessage.success(data.message || '计算成功')
      await nextTick()
      renderCharts()
    } else {
      ElMessage.error(data?.message || '计算失败')
    }
  } catch (e) {
    ElMessage.error(e?.message || '计算失败')
  } finally {
    calculating.value = false
  }
}

async function loadResults() {
  if (!evaluationId.value) return
  loading.value = true
  try {
    const data = await getComprehensiveResults(evaluationId.value)
    if (data?.success) {
      results.value = data.results || []
      weightSnapshot.value = data.weightSnapshot || null
      // 从第一个结果中提取维度权重
      if (data.results && data.results.length > 0) {
        dimensionWeights.value = data.results[0].dimensionWeights || {}
      }
      extractAvailableDimensions()
      await nextTick()
      renderCharts()
    } else {
      ElMessage.warning(data?.message || '暂无结果，请先计算')
    }
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

async function deleteResults() {
  if (!evaluationId.value) return
  try {
    await ElMessageBox.confirm('确定要删除该批次的综合评分结果吗？', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteComprehensiveResults(evaluationId.value)
    results.value = []
    weightSnapshot.value = null
    disposeCharts()
    ElMessage.success('删除成功')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

function disposeCharts() {
  totalScoreChart?.dispose()
  domainScoreChart?.dispose()
  totalScoreChart = null
  domainScoreChart = null
  // 销毁指标图表
  Object.values(indicatorCharts).forEach(chart => chart?.dispose())
  Object.keys(indicatorCharts).forEach(key => delete indicatorCharts[key])
}

function renderCharts() {
  if (results.value.length === 0) return
  console.log('开始渲染所有图表...')
  renderTotalScoreChart()
  renderDomainScoreChart()
  extractAvailableDimensions()
  console.log('indicatorScores.value:', indicatorScores.value)
  renderIndicatorCharts()
}

// 提取所有可用维度
function extractAvailableDimensions() {
  const dims = new Set()
  results.value.forEach(r => {
    // 从 dimensionScores 提取
    const dimsData = r.dimensionScores || {}
    Object.keys(dimsData).forEach(key => dims.add(key))
    // 从 indicatorScores 提取
    const indData = r.indicatorScores || {}
    Object.keys(indData).forEach(key => dims.add(key))
  })
  availableDimensions.value = Array.from(dims).sort()
  // 默认选择所有维度
  selectedDimensions.value = [...availableDimensions.value]
}

// 提取指标得分数据
function extractIndicatorScores() {
  const data = {}

  console.log('开始提取指标得分...')
  console.log('results:', results.value)

  // 遍历所有结果，从 indicatorScores 中提取数据
  results.value.forEach(r => {
    const indScores = r.indicatorScores || {}
    console.log(`作战 ${r.operationId} 的 indicatorScores:`, indScores)

    Object.entries(indScores).forEach(([dimName, indicators]) => {
      if (!data[dimName]) {
        data[dimName] = {
          labels: [],
          datasets: {}
        }
      }
      const opId = `作战${r.operationId}`
      if (!data[dimName].labels.includes(opId)) {
        data[dimName].labels.push(opId)
      }
      Object.entries(indicators).forEach(([indicator, score]) => {
        if (!data[dimName].datasets[indicator]) {
          data[dimName].datasets[indicator] = []
        }
        data[dimName].datasets[indicator].push(Number(score || 0))
      })
    })
  })

  indicatorScores.value = data
  console.log('提取后的 indicatorScores:', indicatorScores.value)
}

// 渲染指标细分图表
function renderIndicatorCharts() {
  nextTick(() => {
    extractIndicatorScores()
  })
}

function renderSingleIndicatorChart(dimName) {
  console.log(`渲染指标图表: ${dimName}`)
  const el = indicatorChartRefs[dimName]
  if (!el) {
    console.log(`DOM 元素未找到: ${dimName}, 可用的 refs:`, Object.keys(indicatorChartRefs))
    return
  }

  if (!indicatorCharts[dimName]) {
    indicatorCharts[dimName] = echarts.init(el)
  }
  const chart = indicatorCharts[dimName]

  const data = indicatorScores.value[dimName]
  console.log(`图表 ${dimName} 的数据:`, data)

  if (!data || !data.labels.length) {
    console.log(`数据为空，跳过渲染`)
    chart.clear()
    return
  }

  const indicators = Object.keys(data.datasets)
  if (indicators.length === 0) {
    console.log(`没有指标数据，跳过渲染`)
    chart.clear()
    return
  }

  const xData = data.labels

  // 计算所有数据的最大最小值
  let allValues = []
  indicators.forEach(ind => {
    allValues = allValues.concat(data.datasets[ind])
  })
  const maxVal = Math.max(0.1, ...allValues)
  const minVal = Math.min(0, ...allValues)

  const series = indicators.map((ind, idx) => ({
    name: ind,
    type: 'bar',
    data: data.datasets[ind],
    itemStyle: {
      color: idx % 2 === 0 ? '#409EFF' : '#67C23A'
    },
    label: {
      show: xData.length <= 5,
      formatter: (p) => p.value.toFixed(2),
      position: 'top'
    }
  }))

  const option = {
    title: {
      text: '',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    legend: {
      data: indicators,
      bottom: 10,
      type: 'scroll'
    },
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      data: xData,
      axisLabel: { fontSize: 11, rotate: xData.length > 6 ? 25 : 0 }
    },
    yAxis: {
      type: 'value',
      name: '得分',
      min: 0,
      max: Math.ceil(maxVal * 10) / 10 + 0.1,
      axisLabel: { formatter: (v) => v.toFixed(2) }
    },
    series
  }

  chart.setOption(option, true)
  chart.resize()
}

function renderTotalScoreChart() {
  if (!totalScoreChartRef.value) return
  if (!totalScoreChart) totalScoreChart = echarts.init(totalScoreChartRef.value)

  const chartType = totalScoreChartType.value
  const xData = results.value.map(r => `作战${r.operationId}`)
  const yData = results.value.map(r => Number(r.totalScore || 0))

  const option = {
    title: { text: '各作战综合总分对比', left: 'center', textStyle: { fontSize: 16 } },
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const p = params[0]
        return `<strong>${p.name}</strong><br/>综合总分: <strong style="color:#67C23A">${Number(p.value).toFixed(4)}</strong>`
      }
    },
    grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      data: xData,
      axisLabel: { fontSize: 12, rotate: xData.length > 6 ? 25 : 0 }
    },
    yAxis: {
      type: 'value',
      name: '综合总分',
      min: 0,
      max: 1,
      axisLabel: { formatter: (v) => v.toFixed(2) }
    },
    series: [{
      type: chartType,
      data: yData,
      itemStyle: chartType === 'bar' ? {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#409EFF' },
          { offset: 1, color: '#67C23A' }
        ])
      } : { color: '#409EFF' },
      lineStyle: chartType === 'line' ? { width: 3 } : undefined,
      smooth: chartType === 'line',
      barWidth: chartType === 'bar' ? '50%' : undefined
    }]
  }

  totalScoreChart.setOption(option, true)
  totalScoreChart.resize()
}

function renderDomainScoreChart() {
  if (!domainScoreChartRef.value) return
  if (!domainScoreChart) domainScoreChart = echarts.init(domainScoreChartRef.value)

  // 根据选择过滤维度
  const filteredDimensions = selectedDimensionKeys.value.filter(key => {
    if (key.startsWith('效能_') && !selectedDomains.value.includes('eff')) return false
    if (key.startsWith('装备_') && !selectedDomains.value.includes('eq')) return false
    return true
  })

  if (filteredDimensions.length === 0) {
    domainScoreChart.clear()
    return
  }

  const xData = results.value.map(r => `作战${r.operationId}`)

  // 准备数据：每个维度一个系列
  const effDims = filteredDimensions.filter(d => d.startsWith('效能_')).map(d => d.replace('效能_', ''))
  const eqDims = filteredDimensions.filter(d => d.startsWith('装备_')).map(d => d.replace('装备_', ''))

  const series = []

  // 效能维度用蓝色系
  const effColors = ['#409EFF', '#79BBFF', '#A0CFFF', '#C6E2FF', '#D9ECFF']
  effDims.forEach((dim, idx) => {
    const data = results.value.map(r => {
      const dimsData = r.dimensionScores || {}
      return Number(dimsData[`效能_${dim}`] || 0)
    })
    series.push({
      name: `效能_${dim}`,
      type: 'bar',
      data,
      itemStyle: { color: effColors[idx % effColors.length] },
      label: {
        show: results.value.length <= 8,
        formatter: (p) => p.value.toFixed(2),
        position: 'top'
      }
    })
  })

  // 装备维度用绿色系
  const eqColors = ['#67C23A', '#95D475', '#B3E19D', '#D1FCAC', '#E8F7D8']
  eqDims.forEach((dim, idx) => {
    const data = results.value.map(r => {
      const dimsData = r.dimensionScores || {}
      return Number(dimsData[`装备_${dim}`] || 0)
    })
    series.push({
      name: `装备_${dim}`,
      type: 'bar',
      data,
      itemStyle: { color: eqColors[idx % eqColors.length] },
      label: {
        show: results.value.length <= 8,
        formatter: (p) => p.value.toFixed(2),
        position: 'top'
      }
    })
  })

  // 构建图例名称（带权重）
  const legendData = series.map(s => s.name)

  // 计算 Y 轴范围
  let allDimValues = []
  filteredDimensions.forEach(dimKey => {
    results.value.forEach(r => {
      const dimsData = r.dimensionScores || {}
      allDimValues.push(Number(dimsData[dimKey] || 0))
    })
  })
  const maxDimVal = Math.max(0.1, ...allDimValues)

  const option = {
    title: {
      text: '各维度得分对比',
      left: 'center',
      textStyle: { fontSize: 16 }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        let html = `<strong>${params[0].name}</strong><br/>`
        params.forEach(p => {
          html += `${p.marker} ${p.seriesName}: <strong>${Number(p.value).toFixed(4)}</strong><br/>`
        })
        return html
      }
    },
    legend: {
      data: legendData,
      bottom: 10,
      type: 'scroll'
    },
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      data: xData,
      axisLabel: { fontSize: 12, rotate: xData.length > 6 ? 25 : 0 }
    },
    yAxis: {
      type: 'value',
      name: '得分',
      min: 0,
      max: Math.ceil(maxDimVal * 10) / 10 + 0.1,
      axisLabel: { formatter: (v) => v.toFixed(2) }
    },
    series
  }

  domainScoreChart.setOption(option, true)
  domainScoreChart.resize()
}

watch(totalScoreChartType, () => renderTotalScoreChart())
watch([selectedDomains, selectedDimensions], () => renderDomainScoreChart(), { deep: true })

// 监听 indicatorScores 变化，确保 DOM 更新后渲染图表
watch(indicatorScores, () => {
  nextTick(() => {
    Object.keys(indicatorScores.value).forEach(dimName => {
      renderSingleIndicatorChart(dimName)
    })
  })
}, { deep: true })

onMounted(() => {
  loadBatches()
  window.addEventListener('resize', () => {
    totalScoreChart?.resize()
    domainScoreChart?.resize()
    Object.values(indicatorCharts).forEach(chart => chart?.resize())
  })
})
</script>

<style scoped lang="scss">
.comprehensive-scoring-page {
  padding: 0;
}

.page-header {
  margin-bottom: 16px;
  h2 {
    display: flex;
    align-items: center;
    gap: 10px;
    margin: 0 0 8px 0;
    color: #303133;
    font-size: 22px;
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

.weight-info-card {
  margin-bottom: 16px;
  .weight-info {
    display: flex;
    gap: 16px;
  }
}

.content-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.batch-list {
  .batch-section {
    margin-bottom: 8px;
    strong {
      display: inline-block;
      width: 150px;
      color: #606266;
    }
  }
}

.operation-compare {
  margin-bottom: 12px;
  div {
    margin-bottom: 4px;
    font-size: 13px;
  }
}

.composite-score {
  margin-top: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  strong {
    margin-right: 12px;
  }
}

.chart-container-large {
  width: 100%;
  height: 350px;
}

.result-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  h5 {
    margin: 0;
    color: #409eff;
    font-size: 14px;
    font-weight: 600;
  }
  .filter-group {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.result-section {
  margin-bottom: 24px;
  h5 {
    margin: 0 0 15px 0;
    color: #409eff;
    font-size: 14px;
    font-weight: 600;
  }
}

.score-cell {
  font-family: 'Monaco', 'Menlo', monospace;
  &.eff { color: #409EFF; }
  &.eq { color: #67C23A; }
  &.total { color: #E6A23C; }
}

.score-high { color: #67C23A; }
.score-mid { color: #E6A23C; }
.score-low { color: #F56C6C; }

.empty-text {
  color: #909399;
  font-style: italic;
}
</style>