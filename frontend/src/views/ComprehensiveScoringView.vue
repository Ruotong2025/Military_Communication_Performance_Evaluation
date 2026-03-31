<template>
  <div class="comprehensive-scoring-page">
    <div class="page-header">
      <div>
        <h2>
          <el-icon><DataAnalysis /></el-icon>
          评估结果计算
        </h2>
        <p class="subtitle">综合打分 — 原始作战指标与集结加权后的综合评估得分（对应原权重集结打分模块五、六）</p>
      </div>
    </div>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="hint-alert"
      title="请先在「权重集结打分」执行「执行集结计算」（仅生成集结权重），再在本页选择同一批次并点击「加载综合结果」以计算并保存「权重×归一化得分」的综合结果。"
    />

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
            <el-option v-for="id in evaluationIds" :key="id" :label="id" :value="id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Refresh" :loading="loadResultLoading" :disabled="!evaluationId" @click="loadSavedResults">
            加载综合结果
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 一、原始指标对照（原五） -->
    <el-card v-if="evaluationId" class="content-card metrics-raw-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><List /></el-icon> 一、原始指标对照（metrics_military_comm_effect）</span>
          <el-tag type="info" size="small">{{ metricsRows.length }} 条作战记录</el-tag>
        </div>
      </template>
      <p class="metrics-hint">
        以下为「计算指标」写入库的<strong>未归一化</strong>原始值；经「生成归一化 score」后与集结权重相乘得到综合得分。
      </p>
      <el-table
        v-loading="metricsLoading"
        :data="metricsRows"
        border
        stripe
        size="small"
        max-height="380"
        class="metrics-raw-table"
      >
        <el-table-column prop="operation_id" label="作战ID" width="90" fixed align="center" />
        <el-table-column
          v-for="col in metricsFieldMeta"
          :key="col.prop"
          :prop="col.prop"
          :label="col.label"
          align="center"
          min-width="108"
        >
          <template #default="{ row }">
            {{ formatMetricCell(row[col.prop]) }}
          </template>
        </el-table-column>
      </el-table>

      <el-divider content-position="left">归一化得分（score_military_comm_effect）</el-divider>
      <p class="metrics-hint">
        以下为该评估批次内对原始指标做 <strong>Min-Max</strong> 归一化后的得分（0~1，越大越好），数据来自
        <code>score_military_comm_effect</code>；需先在指标计算流程中执行「生成归一化 score」。
      </p>
      <el-table
        v-loading="scoreLoading"
        :data="scoreRows"
        border
        stripe
        size="small"
        max-height="380"
        class="metrics-raw-table"
      >
        <el-table-column prop="operation_id" label="作战ID" width="90" fixed align="center" />
        <el-table-column
          v-for="col in scoreFieldMeta"
          :key="col.prop"
          :prop="col.prop"
          :label="col.label"
          align="center"
          min-width="100"
        >
          <template #default="{ row }">
            {{ formatScoreCell(row[col.prop]) }}
          </template>
        </el-table-column>
      </el-table>

      <el-collapse class="metrics-legend-collapse">
        <el-collapse-item title="指标字段含义与对应 AHP 打分项" name="legend">
          <el-table :data="metricsFieldMeta" border size="small" max-height="320">
            <el-table-column prop="category" label="维度" width="100" align="center" />
            <el-table-column prop="label" label="字段（列名）" min-width="160" />
            <el-table-column prop="prop" label="库字段名" width="200">
              <template #default="{ row: r }">
                <code class="code-field">{{ r.prop }}</code>
              </template>
            </el-table-column>
            <el-table-column prop="meaning" label="含义" min-width="260" />
            <el-table-column prop="scoreName" label="对应归一化得分项" width="140" align="center" />
          </el-table>
        </el-collapse-item>
      </el-collapse>
    </el-card>

    <!-- 二、综合评估得分（原六） -->
    <el-card v-if="collectiveResult" class="content-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><Finished /></el-icon> 二、综合评估得分（集结权重 × 归一化 score）</span>
          <el-button type="danger" size="small" :icon="Delete" @click="deleteResults">删除结果</el-button>
        </div>
      </template>

      <div class="result-section">
        <div class="result-toolbar">
          <h5>2.1 各作战任务一级维度得分对比</h5>
          <el-radio-group v-model="primaryDimChartType" size="small" @change="renderPrimaryDimensionByExperimentChart">
            <el-radio-button value="bar">分组柱状图</el-radio-button>
            <el-radio-button value="line">折线图</el-radio-button>
          </el-radio-group>
        </div>
        <p class="chart-hint">
          横轴为各作战实验；不同颜色代表五大一级维度得分（0~1）。纵轴固定 0~1。底部<strong>滑块</strong>可缩放横轴，也可在图内<strong>滚轮</strong>缩放、按住拖拽平移。
        </p>
        <div ref="primaryDimByExperimentChartRef" class="chart-container-large"></div>
      </div>

      <div class="result-section">
        <div class="result-toolbar">
          <h5>2.2 各作战任务二级指标加权得分对比</h5>
          <el-radio-group v-model="dimensionScoreChartType" size="small" @change="renderDimensionScoreDetailChart">
            <el-radio-button value="bar">分组柱状图</el-radio-button>
            <el-radio-button value="line">折线图</el-radio-button>
          </el-radio-group>
        </div>
        <p class="chart-hint">
          柱高为 <strong>归一化得分 × 该指标集结二级权重</strong>（对综合分的分项贡献）。横轴为 18 个二级指标，标签下行为权重占比。纵轴为加权得分，刻度随数据自动缩放。请用底部滑块或图内滚轮缩放横轴。
        </p>
        <div ref="dimensionScoreChartRef" class="chart-container-large"></div>
      </div>

      <div class="result-section">
        <h5>2.3 各作战任务综合得分对比</h5>
        <div ref="scoreChartRef" class="chart-container-large"></div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import request from '../utils/request'
import { getCollectiveEvaluationIds, computeCollectiveResults, deleteCollectiveResults } from '../api/index'
import { DataAnalysis, List, Finished, Delete, Refresh } from '@element-plus/icons-vue'

const evaluationIds = ref([])
const evaluationId = ref('')
const collectiveResult = ref(null)
const loadResultLoading = ref(false)

const metricsFieldMeta = [
  { prop: 'security_key_leakage_count', label: '密钥泄露次数', category: '安全性', meaning: '统计窗口内密钥泄露事件次数；越小越好，归一化后进入「密钥泄露得分」。', scoreName: '密钥泄露得分' },
  { prop: 'security_detected_probability', label: '被侦察概率', category: '安全性', meaning: '被侦察通信占比(%)；越小越好，归一化后进入「被侦察得分」。', scoreName: '被侦察得分' },
  { prop: 'security_interception_resistance_probability', label: '抗拦截(被拦占比)', category: '安全性', meaning: '被拦截通信占比(%)；数值含义与业务定义一致，归一化后进入「抗拦截得分」。', scoreName: '抗拦截得分' },
  { prop: 'reliability_crash_count', label: '网络崩溃次数', category: '可靠性', meaning: '链路崩溃类中断次数；越少越好，归一化后进入「崩溃比例得分」。', scoreName: '崩溃比例得分' },
  { prop: 'reliability_recovery_time', label: '平均恢复时间', category: '可靠性', meaning: '成功恢复事件的平均耗时(ms)；越短越好，归一化后进入「恢复能力得分」。', scoreName: '恢复能力得分' },
  { prop: 'reliability_communication_availability', label: '通信可用性', category: '可靠性', meaning: '可用时间占比(%)；越大越好，归一化后进入「通信可用得分」。', scoreName: '通信可用得分' },
  { prop: 'transmission_bandwidth', label: '平均带宽', category: '传输能力', meaning: '平均带宽(Mbps)；越大越好，归一化后进入「带宽得分」。', scoreName: '带宽得分' },
  { prop: 'transmission_call_setup_time', label: '呼叫建立时间', category: '传输能力', meaning: '平均呼叫建立耗时(ms)；越短越好，归一化后进入「呼叫建立得分」。', scoreName: '呼叫建立得分' },
  { prop: 'transmission_delay', label: '传输时延', category: '传输能力', meaning: '平均传输时延(ms)；越短越好，归一化后进入「传输时延得分」。', scoreName: '传输时延得分' },
  { prop: 'transmission_bit_error_rate', label: '误码率', category: '传输能力', meaning: '平均误码率(%)；越低越好，归一化后进入「误码率得分」。', scoreName: '误码率得分' },
  { prop: 'transmission_throughput', label: '吞吐量', category: '传输能力', meaning: '平均吞吐量(Mbps)；越大越好，归一化后进入「吞吐量得分」。', scoreName: '吞吐量得分' },
  { prop: 'transmission_spectral_efficiency', label: '频谱效率', category: '传输能力', meaning: 'bit/Hz 类效率指标；越大越好，归一化后进入「频谱效率得分」。', scoreName: '频谱效率得分' },
  { prop: 'anti_jamming_sinr', label: '信干噪比', category: '抗干扰能力', meaning: '平均 SINR(dB)；越大越好，归一化后进入「信干噪比得分」。', scoreName: '信干噪比得分' },
  { prop: 'anti_jamming_margin', label: '抗干扰余量', category: '抗干扰能力', meaning: '平均抗干扰余量(dB)；越大越好，归一化后进入「抗干扰余量得分」。', scoreName: '抗干扰余量得分' },
  { prop: 'anti_jamming_communication_distance', label: '通信距离', category: '抗干扰能力', meaning: '平均通信距离(km)；越大越好，归一化后进入「通信距离得分」。', scoreName: '通信距离得分' },
  { prop: 'effect_damage_rate', label: '毁伤率', category: '效能影响', meaning: '装备毁伤占比(%)；越低越好，归一化后进入「战损率得分」。', scoreName: '战损率得分' },
  { prop: 'effect_mission_completion_rate', label: '任务完成率', category: '效能影响', meaning: '通信成功占比(%)；越高越好，归一化后进入「任务完成率得分」。', scoreName: '任务完成率得分' },
  { prop: 'effect_blind_rate', label: '盲区率', category: '效能影响', meaning: '孤立节点占比(%)；越低越好，归一化后进入「致盲率得分」。', scoreName: '致盲率得分' }
]

/** score_military_comm_effect 指标列（顺序与后端 MetricsDirection.getScoreFields 一致） */
const scoreFieldMeta = [
  { prop: 'security_key_leakage_qt', label: '密钥泄露得分' },
  { prop: 'security_detected_probability_qt', label: '被侦察得分' },
  { prop: 'security_interception_resistance_ql', label: '抗拦截得分' },
  { prop: 'reliability_crash_rate_qt', label: '崩溃比例得分' },
  { prop: 'reliability_recovery_capability_qt', label: '恢复能力得分' },
  { prop: 'reliability_communication_availability_qt', label: '通信可用得分' },
  { prop: 'transmission_bandwidth_qt', label: '带宽得分' },
  { prop: 'transmission_call_setup_time_qt', label: '呼叫建立得分' },
  { prop: 'transmission_transmission_delay_qt', label: '传输时延得分' },
  { prop: 'transmission_bit_error_rate_qt', label: '误码率得分' },
  { prop: 'transmission_throughput_qt', label: '吞吐量得分' },
  { prop: 'transmission_spectral_efficiency_qt', label: '频谱效率得分' },
  { prop: 'anti_jamming_sinr_qt', label: '信干噪比得分' },
  { prop: 'anti_jamming_anti_jamming_margin_qt', label: '抗干扰余量得分' },
  { prop: 'anti_jamming_communication_distance_qt', label: '通信距离得分' },
  { prop: 'effect_damage_rate_qt', label: '战损率得分' },
  { prop: 'effect_mission_completion_rate_qt', label: '任务完成率得分' },
  { prop: 'effect_blind_rate_qt', label: '致盲率得分' }
]

/** 18 个二级指标得分项名称（与集结结果 indicatorScores 的 key 一致） */
const INDICATOR_LABELS = scoreFieldMeta.map((c) => c.label)

const metricsRows = ref([])
const metricsLoading = ref(false)
const scoreRows = ref([])
const scoreLoading = ref(false)

const scoreChartRef = ref(null)
const dimensionScoreChartRef = ref(null)
const primaryDimByExperimentChartRef = ref(null)
let scoreChart = null
let dimensionScoreChart = null
let primaryDimByExperimentChart = null

const dimensionScoreChartType = ref('bar')
const primaryDimChartType = ref('bar')

function formatMetricCell(v) {
  if (v === null || v === undefined || v === '') return '—'
  const n = Number(v)
  if (!Number.isFinite(n)) return String(v)
  return Math.abs(n) >= 1000 ? n.toFixed(2) : n.toFixed(4)
}

function formatScoreCell(v) {
  if (v === null || v === undefined || v === '') return '—'
  const n = Number(v)
  if (!Number.isFinite(n)) return String(v)
  return n.toFixed(2)
}

function normalizeMetricsRows(list) {
  return list.map((row) => {
    const o = {}
    for (const [k, val] of Object.entries(row)) {
      o[String(k).toLowerCase()] = val
    }
    return o
  })
}

async function loadMetricsForBatch() {
  const id = evaluationId.value
  if (!id) {
    metricsRows.value = []
    scoreRows.value = []
    return
  }
  metricsLoading.value = true
  scoreLoading.value = true
  try {
    const list = await request.get('/metrics/results', { params: { evaluationId: id } })
    metricsRows.value = Array.isArray(list) ? normalizeMetricsRows(list) : []
  } catch (e) {
    console.error('加载原始指标失败:', e)
    metricsRows.value = []
  } finally {
    metricsLoading.value = false
  }
  try {
    const scores = await request.get('/metrics/scores', { params: { evaluationId: id } })
    scoreRows.value = Array.isArray(scores) ? normalizeMetricsRows(scores) : []
  } catch (e) {
    console.error('加载归一化 score 失败:', e)
    scoreRows.value = []
  } finally {
    scoreLoading.value = false
  }
}

function onBatchChange() {
  collectiveResult.value = null
  disposeResultCharts()
  loadMetricsForBatch()
}

function disposeResultCharts() {
  scoreChart?.dispose()
  dimensionScoreChart?.dispose()
  primaryDimByExperimentChart?.dispose()
  scoreChart = null
  dimensionScoreChart = null
  primaryDimByExperimentChart = null
}

function paletteForExperimentSeries(n) {
  const base = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc', '#409EFF']
  if (n <= base.length) return base.slice(0, n)
  const out = [...base]
  for (let i = base.length; i < n; i++) {
    const h = Math.round((360 * i) / n) % 360
    out.push(`hsl(${h}, 62%, 52%)`)
  }
  return out
}

/** 横轴缩放：滑块 + 图内滚轮/拖拽（类目轴） */
function buildCategoryAxisDataZoom() {
  return [
    {
      type: 'inside',
      xAxisIndex: 0,
      zoomOnMouseWheel: true,
      moveOnMouseMove: true,
      moveOnMouseWheel: false
    },
    {
      type: 'slider',
      xAxisIndex: 0,
      height: 22,
      bottom: 4,
      showDetail: false,
      handleStyle: { color: '#409eff' },
      dataBackground: { lineStyle: { opacity: 0.3 }, areaStyle: { opacity: 0.1 } },
      borderColor: '#dcdfe6'
    }
  ]
}

function renderScoreChart() {
  if (!scoreChartRef.value || !collectiveResult.value?.results) return
  if (!scoreChart) scoreChart = echarts.init(scoreChartRef.value)
  const results = collectiveResult.value.results
  const option = {
    title: { text: '各作战任务综合得分对比', left: 'center', textStyle: { fontSize: 16 } },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const p = params[0]
        return `<strong>${p.name}</strong><br/>综合得分: <strong style="color:#67C23A">${Number(p.value).toFixed(2)}</strong>`
      }
    },
    grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      data: results.map((r) => `实验${r.operationId}`),
      axisLabel: { fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      name: '综合得分',
      min: 0,
      max: 1,
      axisLabel: { formatter: (v) => Number(v).toFixed(2) }
    },
    series: [{
      type: 'bar',
      data: results.map((r) => ({
        value: Number(r.totalScore),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#67C23A' },
            { offset: 1, color: '#95D475' }
          ])
        }
      })),
      label: { show: true, position: 'top', formatter: (p) => Number(p.value).toFixed(2), fontSize: 11 },
      barWidth: '50%'
    }]
  }
  scoreChart.setOption(option)
  scoreChart.resize()
}

function renderDimensionScoreDetailChart() {
  if (!dimensionScoreChartRef.value || !collectiveResult.value?.results?.length) return
  if (!dimensionScoreChart) dimensionScoreChart = echarts.init(dimensionScoreChartRef.value)
  const results = collectiveResult.value.results
  const iw = collectiveResult.value.indicatorWeights || {}
  const weightPctInd = (name) => {
    const v = Number(iw[name] ?? 0)
    return (v * 100).toFixed(1)
  }
  const weightNum = (name) => Number(iw[name] ?? 0)
  const weightedValue = (r, name) => {
    const s = Number(r.indicatorScores?.[name] ?? 0)
    return s * weightNum(name)
  }
  let peak = 0
  for (const r of results) {
    for (const name of INDICATOR_LABELS) {
      peak = Math.max(peak, weightedValue(r, name))
    }
  }
  const yMax = peak > 1e-9 ? Math.min(1, peak * 1.12) : 0.1

  const xCategories = INDICATOR_LABELS.map((n) => `${n}\n权重 ${weightPctInd(n)}%`)
  const chartType = dimensionScoreChartType.value === 'line' ? 'line' : 'bar'
  const colors = paletteForExperimentSeries(results.length)
  const series = results.map((r, idx) => ({
    name: `实验${r.operationId}`,
    type: chartType,
    data: INDICATOR_LABELS.map((name) => weightedValue(r, name)),
    itemStyle: { color: colors[idx] },
    lineStyle: chartType === 'line' ? { width: 2 } : undefined,
    smooth: chartType === 'line',
    symbolSize: chartType === 'line' ? 7 : undefined,
    barMaxWidth: chartType === 'bar' ? 10 : undefined
  }))
  const option = {
    title: {
      text: '各二级指标加权得分 — 按实验对比（得分 × 集结二级权重）',
      left: 'center',
      textStyle: { fontSize: 15 }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter(params) {
        if (!params?.length) return ''
        const dataIndex = params[0].dataIndex
        const indTitle = INDICATOR_LABELS[dataIndex] ?? ''
        const w = weightNum(indTitle)
        let head = `<strong>${indTitle}</strong>`
        if (indTitle) {
          head += `<br/><span style="opacity:.85">集结二级权重：${weightPctInd(indTitle)}%（${w.toFixed(2)}）</span>`
        }
        const lines = params
          .filter((p) => p.value != null && !Number.isNaN(Number(p.value)))
          .map((p) => {
            const weighted = Number(p.value)
            const raw = w > 1e-12 ? weighted / w : 0
            return `${p.marker} ${p.seriesName}: 归一化得分 ${raw.toFixed(2)} × 权重 → <strong>${weighted.toFixed(2)}</strong>`
          })
        return `${head}<br/>${lines.join('<br/>')}`
      }
    },
    legend: { type: 'scroll', bottom: 36, left: 'center', data: results.map((r) => `实验${r.operationId}`) },
    grid: { left: '3%', right: '4%', top: 58, bottom: 118, containLabel: true },
    dataZoom: buildCategoryAxisDataZoom(),
    xAxis: {
      type: 'category',
      data: xCategories,
      axisLabel: { interval: 0, rotate: 32, fontSize: 9, lineHeight: 12 }
    },
    yAxis: {
      type: 'value',
      name: '加权得分（得分×权重）',
      min: 0,
      max: yMax,
      axisLabel: { formatter: (v) => Number(v).toFixed(2) },
      splitLine: { lineStyle: { type: 'dashed' } }
    },
    series
  }
  dimensionScoreChart.setOption(option, true)
  dimensionScoreChart.resize()
}

function renderPrimaryDimensionByExperimentChart() {
  if (!primaryDimByExperimentChartRef.value || !collectiveResult.value?.results?.length) return
  if (!primaryDimByExperimentChart) primaryDimByExperimentChart = echarts.init(primaryDimByExperimentChartRef.value)
  const results = collectiveResult.value.results
  const dimNames = ['安全性', '可靠性', '传输能力', '抗干扰能力', '效能影响']
  const labels = results.map((r) => `实验${r.operationId}`)
  const chartType = primaryDimChartType.value === 'line' ? 'line' : 'bar'
  const palette = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399']
  const series = dimNames.map((d, idx) => ({
    name: d,
    type: chartType,
    data: results.map((r) => Number(r.dimensionScores?.[d] ?? 0)),
    itemStyle: { color: palette[idx] },
    smooth: chartType === 'line',
    symbolSize: chartType === 'line' ? 6 : undefined,
    barMaxWidth: chartType === 'bar' ? 22 : undefined,
    lineStyle: chartType === 'line' ? { width: 2 } : undefined
  }))
  const option = {
    title: { text: '各作战任务 — 一级维度得分对比', left: 'center', textStyle: { fontSize: 15 } },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter(params) {
        if (!params?.length) return ''
        const expName = params[0].name
        const lines = params
          .filter((p) => p.value != null && !Number.isNaN(Number(p.value)))
          .map((p) => `${p.marker} ${p.seriesName}: <strong>${Number(p.value).toFixed(2)}</strong>`)
        return `<strong>${expName}</strong><br/>${lines.join('<br/>')}`
      }
    },
    legend: { type: 'scroll', bottom: 36, left: 'center', data: dimNames },
    grid: { left: '3%', right: '4%', top: 55, bottom: 100, containLabel: true },
    dataZoom: buildCategoryAxisDataZoom(),
    xAxis: { type: 'category', data: labels, axisLabel: { rotate: labels.length > 8 ? 25 : 0, fontSize: 11 } },
    yAxis: {
      type: 'value',
      name: '得分(0~1)',
      min: 0,
      max: 1,
      axisLabel: { formatter: (v) => Number(v).toFixed(2) },
      splitLine: { lineStyle: { type: 'dashed' } }
    },
    series
  }
  primaryDimByExperimentChart.setOption(option, true)
  primaryDimByExperimentChart.resize()
}

async function loadEvaluationIds() {
  try {
    const ids = await getCollectiveEvaluationIds()
    evaluationIds.value = Array.isArray(ids) ? ids : []
  } catch (e) {
    console.error(e)
    evaluationIds.value = []
  }
}

async function loadSavedResults() {
  if (!evaluationId.value) return
  loadResultLoading.value = true
  try {
    const data = await computeCollectiveResults(evaluationId.value)
    collectiveResult.value = data
    ElMessage.success('已计算并加载该批次综合评估结果（保留两位小数）')
    await nextTick()
    renderPrimaryDimensionByExperimentChart()
    renderDimensionScoreDetailChart()
    renderScoreChart()
  } catch (e) {
    collectiveResult.value = null
    disposeResultCharts()
    ElMessage.error(e?.message || '加载失败，请确认已执行集结计算')
  } finally {
    loadResultLoading.value = false
  }
}

async function deleteResults() {
  if (!evaluationId.value) return
  try {
    await ElMessageBox.confirm('确定要删除该批次的综合评估结果吗？', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteCollectiveResults(evaluationId.value)
    collectiveResult.value = null
    disposeResultCharts()
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

onMounted(() => {
  loadEvaluationIds()
  window.addEventListener('resize', () => {
    scoreChart?.resize()
    dimensionScoreChart?.resize()
    primaryDimByExperimentChart?.resize()
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

.content-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
}

.chart-hint {
  font-size: 12px;
  color: #909399;
  margin: 0 0 10px 0;
  line-height: 1.5;
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

.metrics-hint {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin: 0 0 12px 0;
}

.metrics-legend-collapse {
  margin-top: 14px;
}

.metrics-raw-table {
  width: 100%;
}

.code-field {
  font-size: 12px;
}
</style>
