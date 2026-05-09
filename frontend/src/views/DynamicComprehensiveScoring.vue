<template>
  <div class="dynamic-comprehensive-scoring">
    <!-- 头部筛选 -->
    <el-card class="header-card">
      <div class="header-content">
        <h2>动态综合打分</h2>
        <div class="filter-section">
          <el-select
            v-model="selectedTemplateId"
            placeholder="请选择指标模板"
            filterable
            @change="handleTemplateChange"
            style="width: 260px"
          >
            <el-option
              v-for="tpl in templates"
              :key="tpl.id"
              :label="tpl.templateName"
              :value="tpl.id"
            >
              <span>{{ tpl.templateName }}</span>
              <span class="template-stats">{{ tpl.levelCount || 0 }}层级</span>
            </el-option>
          </el-select>

          <el-select
            v-model="selectedBatchId"
            placeholder="请选择批次"
            filterable
            :disabled="!selectedTemplateId"
            @change="handleBatchChange"
            style="width: 260px"
          >
            <el-option
              v-for="batch in filteredBatches"
              :key="batch.batch_id"
              :label="batch.batch_id"
              :value="batch.batch_id"
            >
              <span>{{ batch.batch_id }}</span>
              <span class="batch-info">| {{ batch.operationCount }} 个作战</span>
            </el-option>
          </el-select>

          <el-tag v-if="loading" type="info">加载中...</el-tag>
        </div>
      </div>
    </el-card>

    <!-- 原始数据集结卡片 -->
    <el-card v-if="rawData && rawData.operationData && rawData.operationData.length > 0" class="raw-data-card">
      <template #header>
        <div class="card-header">
          <span>原始数据集结表</span>
        </div>
      </template>

      <el-tabs v-model="activeLevelTab" type="border-card" @tab-change="handleLevelTabChange">
        <el-tab-pane
          v-for="levelName in rawDataLevelNames"
          :key="levelName"
          :label="levelName"
          :name="levelName"
        >
          <div class="table-wrapper">
            <el-table
              :data="rawData.operationData"
              border
              stripe
              size="small"
              max-height="500"
            >
              <el-table-column prop="operationId" label="作战ID" width="120" fixed />

              <el-table-column
                v-for="dim in getRawDataDimensionsByLevel(levelName)"
                :key="dim.code"
                :label="dim.name"
                align="center"
                min-width="120"
              >
                <el-table-column
                  v-for="ind in dim.indicators"
                  :key="ind.code"
                  :label="ind.name"
                  align="center"
                  width="90"
                >
                  <template #header>
                    <span :class="ind.metricType === 'QUALITATIVE' ? 'indicator-tag qual' : 'indicator-tag qt'">
                      {{ ind.metricType === 'QUALITATIVE' ? '定性' : '定量' }}
                    </span>
                    <br />
                    <span class="indicator-name">{{ ind.name }}</span>
                  </template>
                  <template #default="{ row }">
                    <span class="score-value" :class="{ 'no-data': !hasIndicatorScore(row, ind) }">
                      {{ getIndicatorScoreValue(row, ind) }}
                    </span>
                  </template>
                </el-table-column>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 综合评分详情卡片 -->
    <el-card v-if="scoreResults && scoreResults.length > 0" class="scores-card">
      <template #header>
        <div class="card-header">
          <span>综合评分详情</span>
          <el-tag type="success">平均综合得分: {{ formatScore(averageTotalScore) }}</el-tag>
        </div>
      </template>

      <!-- 统计卡片 -->
      <el-row :gutter="20" class="stat-row">
        <el-col :span="6">
          <el-statistic title="作战数量" :value="scoreResults.length" suffix="个" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="平均综合得分" :value="averageTotalScore" suffix="分" :precision="2" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="平均定性得分" :value="averageQualScore" suffix="分" :precision="2" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="平均定量得分" :value="averageQtScore" suffix="分" :precision="2" />
        </el-col>
      </el-row>

      <!-- 图表 -->
      <el-row :gutter="20" class="chart-row">
        <el-col :span="12">
          <div class="chart-container">
            <h4>各作战综合得分对比</h4>
            <div ref="barChartRef" style="width: 100%; height: 280px"></div>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="chart-container">
            <h4>各作战得分构成</h4>
            <div ref="stackedChartRef" style="width: 100%; height: 280px"></div>
          </div>
        </el-col>
      </el-row>

      <!-- 综合权重分布表 - 按层级分Tab -->
      <div class="weight-section" v-if="combinedWeights && combinedWeights.length > 0">
        <h4>综合权重分布</h4>
        <el-tabs v-model="activeWeightTab" type="border-card" class="weight-tabs">
          <!-- 按层级显示Tab -->
          <el-tab-pane
            v-for="level in levelList"
            :key="level"
            :label="level"
            :name="level"
          >
            <el-table
              :data="getFlatWeightByLevel(level)"
              border
              stripe
              size="small"
              max-height="400"
              :span-method="(p) => primarySpanMethod(p)"
            >
              <el-table-column label="一级维度" width="160" align="center" fixed>
                <template #default="{ row }">
                  <span v-if="row.primaryRowspan > 0">
                    <strong>{{ row.primaryName }}</strong>
                    <br />
                    <el-tag size="small" type="success">{{ formatWeight(row.primaryWeight) }}</el-tag>
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="secondaryName" label="二级指标" min-width="150" align="center" />
              <el-table-column label="综合权重" width="120" align="center">
                <template #default="{ row }">
                  <el-tag size="small" type="warning" effect="dark">
                    {{ formatWeight(row.combinedWeight) }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
            <div class="weight-footer">
              <span>{{ level }} 权重合计：</span>
              <el-tag type="primary" size="large">{{ formatWeight(getLevelTotalWeight(level)) }}</el-tag>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 得分明细Tab -->
      <el-tabs v-model="activeTab" type="border-card" @tab-change="handleTabChange" class="score-tabs">
        <!-- 综合概览Tab -->
        <el-tab-pane label="综合概览" name="overview">
          <el-table
            :data="scoreResults"
            border
            stripe
            size="small"
            max-height="350"
          >
            <el-table-column prop="operationId" label="作战ID" width="140" fixed />
            <el-table-column
              v-for="level in levelScores"
              :key="level.levelName"
              :label="level.levelName"
              align="center"
              min-width="140"
            >
              <template #header>
                {{ level.levelName }}
                <br />
                <el-tag size="small" type="primary">{{ formatWeight(level.weight) }}</el-tag>
              </template>
              <el-table-column label="综合" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="getScoreTagType(getLevelScore(row, level.levelName))" size="small">
                    {{ formatScore(getLevelScore(row, level.levelName)) }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table-column>
            <el-table-column label="综合得分" width="120" align="center" fixed="right">
              <template #default="{ row }">
                <el-tag :type="getScoreTagType(row.totalScore)" size="small" effect="dark">
                  {{ formatScore(row.totalScore) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 按层级显示Tab -->
        <el-tab-pane
          v-for="level in levelScores"
          :key="level.levelName"
          :label="level.levelName"
          :name="level.levelName"
        >
          <el-row :gutter="20" class="stat-row">
            <el-col :span="6">
              <el-statistic title="平均定性得分" :value="getLevelAvgQualScore(level.levelName)" suffix="分" :precision="2" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="平均定量得分" :value="getLevelAvgQtScore(level.levelName)" suffix="分" :precision="2" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="平均综合得分" :value="getLevelAvgScore(level.levelName)" suffix="分" :precision="2" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="层级权重" :value="level.weight * 100" suffix="%" :precision="1" />
            </el-col>
          </el-row>

          <el-row :gutter="20" class="chart-row">
            <el-col :span="12">
              <div class="chart-container">
                <h4>{{ level.levelName }} - 各作战得分对比</h4>
                <div :ref="el => setChartRef(el, level.levelName)" style="width: 100%; height: 250px"></div>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="chart-container">
                <h4>{{ level.levelName }} - 得分构成</h4>
                <div :ref="el => setStackedChartRef(el, level.levelName)" style="width: 100%; height: 250px"></div>
              </div>
            </el-col>
          </el-row>

          <el-table
            :data="scoreResults"
            border
            stripe
            size="small"
            max-height="350"
          >
            <el-table-column prop="operationId" label="作战ID" width="140" fixed />
            <el-table-column
              v-for="primary in level.primaryDimensions"
              :key="primary.dimensionCode"
              :label="primary.dimensionName"
              align="center"
              min-width="150"
            >
              <template #header>
                {{ primary.dimensionName }}
                <br />
                <el-tag size="small" type="success">{{ formatWeight(primary.weight) }}</el-tag>
              </template>
              <el-table-column label="定性" width="70" align="center">
                <template #default="{ row }">
                  {{ formatScore(getPrimaryQualScore(row, level.levelName, primary.dimensionCode)) }}
                </template>
              </el-table-column>
              <el-table-column label="定量" width="70" align="center">
                <template #default="{ row }">
                  {{ formatScore(getPrimaryQtScore(row, level.levelName, primary.dimensionCode)) }}
                </template>
              </el-table-column>
              <el-table-column label="综合" width="70" align="center">
                <template #default="{ row }">
                  {{ formatScore(getPrimaryScore(row, level.levelName, primary.dimensionCode)) }}
                </template>
              </el-table-column>
            </el-table-column>
            <el-table-column :label="level.levelName + '综合'" width="120" align="center" fixed="right">
              <template #default="{ row }">
                <el-tag :type="getScoreTagType(getLevelScore(row, level.levelName))" size="small">
                  {{ formatScore(getLevelScore(row, level.levelName)) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 空状态 -->
    <el-empty v-if="!selectedTemplateId" description="请先选择指标模板" />
    <el-empty v-else-if="selectedTemplateId && !selectedBatchId" description="请选择评估批次以开始" />
    <el-empty v-else-if="selectedBatchId && (!rawData || rawData.operationData?.length === 0)" description="该批次暂无数据" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import {
  getDynamicComprehensiveBatches,
  getDynamicComprehensiveRawData,
  getDynamicComprehensiveScores,
  getDynamicAhpTemplates
} from '@/api'

// 数据
const templates = ref([])
const selectedTemplateId = ref(null)
const batches = ref([])
const selectedBatchId = ref(null)
const rawData = ref(null)
const scoreResults = ref([])
const activeTab = ref('overview')
const activeLevelTab = ref('')
const activeWeightTab = ref('')
const loading = ref(false)

// 图表引用
const barChartRef = ref(null)
const stackedChartRef = ref(null)
const chartRefs = ref({})
const stackedChartRefs = ref({})

// 计算属性
const filteredBatches = computed(() => {
  if (!selectedTemplateId.value) return batches.value
  return batches.value.filter(b =>
    b.template_id != null && Number(b.template_id) === Number(selectedTemplateId.value)
  )
})

// 从 rawData 中提取层级名称
const rawDataLevelNames = computed(() => {
  if (!rawData.value || !rawData.value.primaryDimensions) return []
  const levels = new Set()
  rawData.value.primaryDimensions.forEach(d => {
    if (d.levelName) levels.add(d.levelName)
  })
  return Array.from(levels)
})

// 从 scoreResults 中提取层级得分结构
const levelScores = computed(() => {
  if (!scoreResults.value || !scoreResults.value[0]) return []
  return scoreResults.value[0].levelScores || []
})

// 综合权重
const combinedWeights = computed(() => {
  if (!scoreResults.value || !scoreResults.value[0]) return []
  return scoreResults.value[0].combinedWeights || []
})

// 权重合计
const totalWeight = computed(() => {
  const weights = combinedWeights.value
  if (!weights || weights.length === 0) return null
  return weights.reduce((sum, w) => sum + (w.combinedWeight || 0), 0)
})

// 平均得分
const averageTotalScore = computed(() => {
  if (!scoreResults.value || scoreResults.value.length === 0) return 0
  const sum = scoreResults.value.reduce((acc, r) => acc + (r.totalScore || 0), 0)
  return sum / scoreResults.value.length
})

const averageQualScore = computed(() => {
  if (!scoreResults.value || scoreResults.value.length === 0) return 0
  const sum = scoreResults.value.reduce((acc, r) => acc + (r.qualitativeWeightedScore || 0), 0)
  return sum / scoreResults.value.length
})

const averageQtScore = computed(() => {
  if (!scoreResults.value || scoreResults.value.length === 0) return 0
  const sum = scoreResults.value.reduce((acc, r) => acc + (r.quantitativeWeightedScore || 0), 0)
  return sum / scoreResults.value.length
})

// 工具方法
const formatScore = (score) => {
  if (score === null || score === undefined) return '-'
  return Number(score).toFixed(1)
}

const formatWeight = (weight) => {
  if (weight === null || weight === undefined) return '-'
  return (Number(weight) * 100).toFixed(2) + '%'
}

// 层级列表
const levelList = computed(() => {
  if (!combinedWeights.value || combinedWeights.value.length === 0) return []
  const levels = [...new Set(combinedWeights.value.map(w => w.levelName))]
  return levels.sort()
})

// 颜色辅助方法
const getLevelColor = (levelName) => {
  const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#009688']
  const index = levelList.value.indexOf(levelName) % colors.length
  return colors[index]
}

// 获取指定层级的扁平化权重数据
const getFlatWeightByLevel = (levelName) => {
  if (!combinedWeights.value) return []
  const levelItems = combinedWeights.value.filter(w => w.levelName === levelName)
  if (levelItems.length === 0) return []

  const result = []
  const sortedPrimaries = [...new Set(levelItems.map(w => w.primaryName))].sort()

  sortedPrimaries.forEach(primaryName => {
    const primaryItems = levelItems.filter(w => w.primaryName === primaryName)
    const primaryWeightSum = primaryItems.reduce((sum, w) => sum + (w.combinedWeight || 0), 0)

    primaryItems.forEach((w, idx) => {
      result.push({
        levelName: w.levelName,
        primaryName: w.primaryName,
        primaryWeight: primaryWeightSum,
        primaryRowspan: idx === 0 ? primaryItems.length : 0,
        secondaryName: w.secondaryName,
        combinedWeight: w.combinedWeight
      })
    })
  })

  return result
}

// 获取指定层级的总权重
const getLevelTotalWeight = (levelName) => {
  if (!combinedWeights.value) return 0
  return combinedWeights.value
    .filter(w => w.levelName === levelName)
    .reduce((sum, w) => sum + (w.combinedWeight || 0), 0)
}

// 一级维度列合并方法
const primarySpanMethod = ({ row, columnIndex }) => {
  if (columnIndex === 0) {
    if (row.primaryRowspan > 0) {
      return { rowspan: row.primaryRowspan, colspan: 1 }
    }
    return { rowspan: 0, colspan: 0 }
  }
  return { rowspan: 1, colspan: 1 }
}

const getScoreTagType = (score) => {
  if (!score) return 'info'
  if (score >= 90) return 'success'
  if (score >= 80) return ''
  if (score >= 70) return 'warning'
  return 'danger'
}

// 获取某层级的表格数据
const getRawDataDimensionsByLevel = (levelName) => {
  if (!rawData.value || !rawData.value.primaryDimensions) return []
  return rawData.value.primaryDimensions.filter(d => d.levelName === levelName)
}

// 获取某行某指标的得分
const getIndicatorScore = (row, indicatorCode) => {
  if (!row || !row.scores) return null
  return row.scores.find(s => s.indicatorCode === indicatorCode)
}

// 判断该行该指标是否有得分
const hasIndicatorScore = (row, indicator) => {
  const score = getIndicatorScore(row, indicator.code)
  if (!score) return false
  if (indicator.metricType === 'QUALITATIVE') {
    return score.qualitativeScore !== null
  }
  return score.quantitativeScore !== null
}

// 根据指标类型获取对应的得分值
const getIndicatorScoreValue = (row, indicator) => {
  const score = getIndicatorScore(row, indicator.code)
  if (!score) return '-'
  if (indicator.metricType === 'QUALITATIVE') {
    return score.qualitativeScore !== null ? formatScore(score.qualitativeScore) : '-'
  }
  return score.quantitativeScore !== null ? formatScore(score.quantitativeScore) : '-'
}

// 获取某作战在某个层级的得分
const getLevelScore = (row, levelName) => {
  if (!row.levelScores) return null
  const level = row.levelScores.find(l => l.levelName === levelName)
  return level ? level.comprehensiveScore : null
}

// 获取某作战在某个一级维度的定性得分
const getPrimaryQualScore = (row, levelName, dimCode) => {
  if (!row.levelScores) return null
  const level = row.levelScores.find(l => l.levelName === levelName)
  if (!level) return null
  const primary = level.primaryDimensions.find(p => p.dimensionCode === dimCode)
  return primary ? primary.qualitativeScore : null
}

// 获取某作战在某个一级维度的定量得分
const getPrimaryQtScore = (row, levelName, dimCode) => {
  if (!row.levelScores) return null
  const level = row.levelScores.find(l => l.levelName === levelName)
  if (!level) return null
  const primary = level.primaryDimensions.find(p => p.dimensionCode === dimCode)
  return primary ? primary.quantitativeScore : null
}

// 获取某作战在某个一级维度的综合得分
const getPrimaryScore = (row, levelName, dimCode) => {
  if (!row.levelScores) return null
  const level = row.levelScores.find(l => l.levelName === levelName)
  if (!level) return null
  const primary = level.primaryDimensions.find(p => p.dimensionCode === dimCode)
  return primary ? primary.comprehensiveScore : null
}

// 获取层级的平均得分
const getLevelAvgScore = (levelName) => {
  const scores = scoreResults.value
    .map(r => getLevelScore(r, levelName))
    .filter(s => s != null)
  if (scores.length === 0) return 0
  return (scores.reduce((a, b) => a + b, 0) / scores.length).toFixed(1)
}

const getLevelAvgQualScore = (levelName) => {
  const level = levelScores.value.find(l => l.levelName === levelName)
  if (!level) return 0
  const scores = scoreResults.value
    .map(r => {
      const ls = r.levelScores?.find(l => l.levelName === levelName)
      return ls ? ls.qualitativeScore : null
    })
    .filter(s => s != null)
  if (scores.length === 0) return 0
  return (scores.reduce((a, b) => a + b, 0) / scores.length).toFixed(1)
}

const getLevelAvgQtScore = (levelName) => {
  const scores = scoreResults.value
    .map(r => {
      const ls = r.levelScores?.find(l => l.levelName === levelName)
      return ls ? ls.quantitativeScore : null
    })
    .filter(s => s != null)
  if (scores.length === 0) return 0
  return (scores.reduce((a, b) => a + b, 0) / scores.length).toFixed(1)
}

// 加载模板列表
const loadTemplates = async () => {
  try {
    const res = await getDynamicAhpTemplates()
    templates.value = res || []
  } catch (error) {
    console.error('加载模板列表失败:', error)
  }
}

// 加载批次列表
const loadBatches = async () => {
  try {
    const res = await getDynamicComprehensiveBatches(selectedTemplateId.value)
    if (res && res.data) {
      batches.value = res.data
    } else if (Array.isArray(res)) {
      batches.value = res
    }
  } catch (error) {
    console.error('加载批次列表失败:', error)
  }
}

// 模板变更处理
const handleTemplateChange = () => {
  selectedBatchId.value = null
  rawData.value = null
  scoreResults.value = []
  activeTab.value = 'overview'
  activeLevelTab.value = ''
  activeWeightLevelTab.value = 'overview'
  activePrimaryTab.value = 'summary'
  loadBatches()
}

// 批次变更处理
const handleBatchChange = () => {
  if (selectedBatchId.value) {
    loadData()
  } else {
    rawData.value = null
    scoreResults.value = []
    activeTab.value = 'overview'
    activeLevelTab.value = ''
    activeWeightLevelTab.value = 'overview'
    activePrimaryTab.value = 'summary'
  }
}

// 层级Tab变更
const handleLevelTabChange = () => {
  // 无需特殊处理
}

// Tab变更
const handleTabChange = (tabName) => {
  if (tabName !== 'overview') {
    nextTick(() => {
      renderLevelChart(tabName)
    })
  } else {
    nextTick(() => {
      renderOverviewCharts()
    })
  }
}

// 加载数据
const loadData = async () => {
  if (!selectedBatchId.value) return

  loading.value = true
  try {
    const rawRes = await getDynamicComprehensiveRawData(selectedBatchId.value)
    if (rawRes && rawRes.operationData) {
      rawData.value = rawRes
      if (rawDataLevelNames.value.length > 0 && !activeLevelTab.value) {
        activeLevelTab.value = rawDataLevelNames.value[0]
      }
    }

    const scoresRes = await getDynamicComprehensiveScores(selectedBatchId.value)
    if (scoresRes && Array.isArray(scoresRes)) {
      scoreResults.value = scoresRes
    }

    await nextTick()
    renderOverviewCharts()
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('数据加载失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

// 图表渲染
const setChartRef = (el, key) => {
  if (el) chartRefs.value[key] = el
}

const setStackedChartRef = (el, key) => {
  if (el) stackedChartRefs.value[key] = el
}

const renderOverviewCharts = () => {
  if (!scoreResults.value || scoreResults.value.length === 0) return
  renderBarChart()
  renderStackedChart()
}

const renderBarChart = () => {
  if (!barChartRef.value) return

  const chart = echarts.init(barChartRef.value)
  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['综合得分', '定性得分', '定量得分'], bottom: 0 },
    xAxis: {
      type: 'category',
      data: scoreResults.value.map(r => r.operationId)
    },
    yAxis: { type: 'value', min: 0, max: 100, name: '得分' },
    series: [
      {
        name: '综合得分',
        type: 'bar',
        data: scoreResults.value.map(r => r.totalScore?.toFixed(1)),
        itemStyle: { color: '#409EFF' }
      },
      {
        name: '定性得分',
        type: 'bar',
        data: scoreResults.value.map(r => r.qualitativeWeightedScore?.toFixed(1)),
        itemStyle: { color: '#67C23A' }
      },
      {
        name: '定量得分',
        type: 'bar',
        data: scoreResults.value.map(r => r.quantitativeWeightedScore?.toFixed(1)),
        itemStyle: { color: '#E6A23C' }
      }
    ]
  }
  chart.setOption(option)
}

const renderStackedChart = () => {
  if (!stackedChartRef.value) return

  const chart = echarts.init(stackedChartRef.value)
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        let res = params[0].name + '<br/>'
        let total = 0
        params.forEach(p => {
          total += p.value
          res += p.marker + p.seriesName + ': ' + Number(p.value).toFixed(1) + '<br/>'
        })
        res += '合计: ' + total.toFixed(1)
        return res
      }
    },
    legend: { data: ['定性得分', '定量得分'], bottom: 0 },
    xAxis: {
      type: 'category',
      data: scoreResults.value.map(r => r.operationId)
    },
    yAxis: { type: 'value', max: 100, name: '得分', axisLabel: { formatter: '{value}' } },
    series: [
      {
        name: '定性得分',
        type: 'bar',
        stack: 'total',
        data: scoreResults.value.map(r => r.qualitativeWeightedScore?.toFixed(1)),
        itemStyle: { color: '#67C23A' }
      },
      {
        name: '定量得分',
        type: 'bar',
        stack: 'total',
        data: scoreResults.value.map(r => r.quantitativeWeightedScore?.toFixed(1)),
        itemStyle: { color: '#E6A23C' }
      }
    ]
  }
  chart.setOption(option)
}

const renderLevelChart = (levelName) => {
  // 得分对比图
  const chartEl = chartRefs.value[levelName]
  if (chartEl) {
    const chart = echarts.init(chartEl)
    const option = {
      tooltip: { trigger: 'axis' },
      legend: { data: ['定性', '定量', '综合'], bottom: 0 },
      xAxis: { type: 'category', data: scoreResults.value.map(r => r.operationId) },
      yAxis: { type: 'value', min: 0, max: 100 },
      series: [
        {
          name: '定性',
          type: 'bar',
          data: scoreResults.value.map(r => {
            const ls = r.levelScores?.find(l => l.levelName === levelName)
            return ls ? ls.qualitativeScore?.toFixed(1) : 0
          }),
          itemStyle: { color: '#67C23A' }
        },
        {
          name: '定量',
          type: 'bar',
          data: scoreResults.value.map(r => {
            const ls = r.levelScores?.find(l => l.levelName === levelName)
            return ls ? ls.quantitativeScore?.toFixed(1) : 0
          }),
          itemStyle: { color: '#E6A23C' }
        },
        {
          name: '综合',
          type: 'line',
          data: scoreResults.value.map(r => getLevelScore(r, levelName)?.toFixed(1) || 0),
          itemStyle: { color: '#F56C6C' }
        }
      ]
    }
    chart.setOption(option)
  }

  // 堆叠图
  const stackedEl = stackedChartRefs.value[levelName]
  if (stackedEl) {
    const chart = echarts.init(stackedEl)
    const option = {
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { data: ['定性', '定量'], bottom: 0 },
      xAxis: { type: 'category', data: scoreResults.value.map(r => r.operationId) },
      yAxis: { type: 'value', max: 100 },
      series: [
        {
          name: '定性',
          type: 'bar',
          stack: 'total',
          data: scoreResults.value.map(r => {
            const ls = r.levelScores?.find(l => l.levelName === levelName)
            return ls ? ls.qualitativeScore?.toFixed(1) : 0
          }),
          itemStyle: { color: '#67C23A' }
        },
        {
          name: '定量',
          type: 'bar',
          stack: 'total',
          data: scoreResults.value.map(r => {
            const ls = r.levelScores?.find(l => l.levelName === levelName)
            return ls ? ls.quantitativeScore?.toFixed(1) : 0
          }),
          itemStyle: { color: '#E6A23C' }
        }
      ]
    }
    chart.setOption(option)
  }
}

// 监听窗口变化
const handleResize = () => {
  if (barChartRef.value) echarts.getInstanceByDom(barChartRef.value)?.resize()
  if (stackedChartRef.value) echarts.getInstanceByDom(stackedChartRef.value)?.resize()
  Object.values(chartRefs.value).forEach(el => { if (el) echarts.getInstanceByDom(el)?.resize() })
  Object.values(stackedChartRefs.value).forEach(el => { if (el) echarts.getInstanceByDom(el)?.resize() })
}

onMounted(() => {
  loadTemplates()
  loadBatches()
  window.addEventListener('resize', handleResize)
})
</script>

<style scoped>
.dynamic-comprehensive-scoring {
  padding: 20px;
}

.header-card {
  margin-bottom: 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-content h2 {
  margin: 0;
  font-size: 20px;
}

.filter-section {
  display: flex;
  gap: 10px;
  align-items: center;
}

.template-stats,
.batch-info {
  color: #909399;
  font-size: 12px;
  margin-left: 8px;
}

.raw-data-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-wrapper {
  overflow-x: auto;
}

.score-value {
  font-size: 11px;
}

.score-value.no-data {
  color: #c0c4cc;
}

.indicator-tag {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 3px;
  display: inline-block;
  margin-bottom: 2px;
}

.indicator-tag.qual {
  background-color: #e8f4e8;
  color: #67c23a;
  border: 1px solid #c2e0b3;
}

.indicator-tag.qt {
  background-color: #fdf6ec;
  color: #e6a23c;
  border: 1px solid #f5d3a6;
}

.indicator-name {
  font-size: 11px;
  color: #606266;
}

.scores-card {
  margin-bottom: 20px;
}

.stat-row {
  margin-bottom: 20px;
}

.chart-row {
  margin-bottom: 20px;
}

.chart-container {
  background: #fafafa;
  border-radius: 8px;
  padding: 15px;
}

.chart-container h4,
.weight-section h4,
.table-section h4 {
  margin: 0 0 15px 0;
  font-size: 14px;
  color: #606266;
}

.weight-section {
  margin-bottom: 20px;
}

.weight-tabs {
  margin-top: 10px;
}

.weight-level-tabs {
  margin-bottom: 16px;
}

.weight-progress {
  margin: 4px 0;
}

.weight-summary {
  margin-top: 10px;
  text-align: right;
  font-size: 14px;
  color: #606266;
}

.weight-ok {
  color: #67c23a;
  margin-left: 8px;
}

.weight-error {
  color: #f56c6c;
  margin-left: 8px;
}

.weight-footer {
  margin-top: 10px;
  text-align: right;
  font-size: 14px;
  color: #606266;
}

.level-progress {
  display: flex;
  align-items: center;
  gap: 8px;
}

.level-progress .level-name {
  min-width: 80px;
  font-weight: 500;
}

.level-progress .level-pct {
  font-size: 12px;
  color: #909399;
}

.score-tabs {
  margin-top: 20px;
}
</style>
