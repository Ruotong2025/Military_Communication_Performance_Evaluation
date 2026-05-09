<template>
  <div class="dynamic-ahp-aggregation">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>
        <el-icon><TrendCharts /></el-icon>
        动态指标AHP专家集结
      </h2>
      <p class="subtitle">对多个专家的动态指标AHP打分进行集结，生成集体判断矩阵和综合权重</p>
    </div>

    <!-- 工具栏 -->
    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-select
            v-model="selectedTemplateId"
            placeholder="请选择指标模板"
            style="width: 260px"
            @change="handleTemplateChange"
          >
            <el-option
              v-for="tpl in templates"
              :key="tpl.id"
              :label="tpl.templateName"
              :value="tpl.id"
            />
          </el-select>

          <el-radio-group v-model="expertSelectionMode" size="default" :disabled="!selectedTemplateId">
            <el-radio-button value="all">全部专家</el-radio-button>
            <el-radio-button value="specific">指定专家</el-radio-button>
          </el-radio-group>

          <el-select
            v-if="expertSelectionMode === 'specific'"
            v-model="selectedExpertIds"
            multiple
            filterable
            collapse-tags
            placeholder="选择专家"
            style="width: 280px"
            :disabled="!selectedTemplateId"
          >
            <el-option
              v-for="e in availableExperts"
              :key="e.expertId"
              :label="`${e.expertName} (可信度: ${e.credibility}%)`"
              :value="e.expertId"
            />
          </el-select>

          <el-button
            type="info"
            :icon="View"
            :loading="previewLoading"
            :disabled="!canPreview"
            @click="previewAggregation"
          >
            预览集结
          </el-button>

          <el-button
            type="primary"
            :icon="Cpu"
            :loading="executeLoading"
            :disabled="!canPreview"
            @click="executeAggregation"
          >
            执行集结计算
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 集结结果展示区域 -->
    <div v-if="aggregationResult" class="aggregation-content">
      <!-- 参与信息 -->
      <el-alert
        :title="`集结完成 | 模板: ${aggregationResult.templateName} | 参与专家: ${aggregationResult.expertCount} 位`"
        type="success"
        :closable="false"
        show-icon
        style="margin-bottom: 16px;"
      />

      <!-- 1. 层级间比较矩阵 -->
      <el-card v-if="levelBetweenMatrix" class="result-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span><el-icon><Grid /></el-icon> 层级间比较矩阵</span>
            <el-tag type="info" size="small">集体判断矩阵</el-tag>
          </div>
        </template>

        <!-- 矩阵表格 -->
        <el-table :data="formatLevelMatrix" border size="small" class="matrix-table">
          <el-table-column prop="row" label="" width="150" />
          <el-table-column
            v-for="col in levelBetweenHeaders"
            :key="col"
            :prop="col"
            :label="col"
            align="center"
            width="100"
          >
            <template #default="{ row }">
              {{ row[col] !== undefined ? Number(row[col]).toFixed(3) : '1.000' }}
            </template>
          </el-table-column>
        </el-table>

        <!-- 权重向量 -->
        <div class="weight-result">
          <h4>层级权重结果</h4>
          <el-table :data="levelBetweenWeightsTable" border size="small">
            <el-table-column prop="name" label="层级" />
            <el-table-column label="权重" align="center">
              <template #default="{ row }">
                <el-tag type="primary" size="small">{{ (row.weight * 100).toFixed(2) }}%</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-alert
            :title="`λmax = ${levelBetweenMatrix.lambdaMax?.toFixed(4)}, CR = ${levelBetweenMatrix.cr?.toFixed(4)}, ${levelBetweenMatrix.cr < 0.1 ? '通过一致性检验' : '未通过一致性检验'}`"
            :type="levelBetweenMatrix.cr < 0.1 ? 'success' : 'warning'"
            :closable="false"
            style="margin-top: 12px"
          />
        </div>
      </el-card>

      <!-- 2. 层级内矩阵（按层级选择） -->
      <el-card class="result-card" shadow="never" style="margin-top: 16px;">
        <template #header>
          <div class="card-header">
            <span><el-icon><Grid /></el-icon> 层级内比较矩阵</span>
            <el-radio-group v-model="activeLevel" size="small">
              <el-radio-button v-for="level in levels" :key="level" :value="level">
                {{ level }}
              </el-radio-button>
            </el-radio-group>
          </div>
        </template>

        <!-- 一级维度间比较矩阵 -->
        <div class="matrix-section">
          <h4 class="section-subtitle">
            <el-icon><FolderOpened /></el-icon>
            {{ activeLevel }} - 一级维度间比较矩阵
          </h4>

          <div v-if="getPrimaryMatrix(activeLevel)" class="matrix-container">
            <el-table :data="getPrimaryMatrixTable(activeLevel)" border size="small" class="matrix-table">
              <el-table-column prop="row" label="" width="150" />
              <el-table-column
                v-for="col in getPrimaryHeaders(activeLevel)"
                :key="col"
                :prop="col"
                :label="col"
                align="center"
                width="100"
              >
                <template #default="{ row }">
                  {{ row[col] !== undefined ? Number(row[col]).toFixed(3) : '1.000' }}
                </template>
              </el-table-column>
            </el-table>

            <!-- 权重向量 -->
            <div class="weight-result">
              <h4>一级维度权重结果</h4>
              <el-table :data="getPrimaryWeightsTable(activeLevel)" border size="small">
                <el-table-column prop="name" label="一级维度" />
                <el-table-column label="权重" align="center">
                  <template #default="{ row }">
                    <el-tag type="primary" size="small">{{ (row.weight * 100).toFixed(2) }}%</el-tag>
                  </template>
                </el-table-column>
              </el-table>
              <el-alert
                :title="`λmax = ${getPrimaryMatrix(activeLevel)?.lambdaMax?.toFixed(4)}, CR = ${getPrimaryMatrix(activeLevel)?.cr?.toFixed(4)}, ${getPrimaryMatrix(activeLevel)?.cr < 0.1 ? '通过一致性检验' : '未通过一致性检验'}`"
                :type="getPrimaryMatrix(activeLevel)?.cr < 0.1 ? 'success' : 'warning'"
                :closable="false"
                style="margin-top: 12px"
              />
            </div>
          </div>
          <el-empty v-else description="暂无一级维度间矩阵数据" :image-size="60" />
        </div>

        <el-divider />

        <!-- 二级指标间比较矩阵 -->
        <div class="matrix-section">
          <h4 class="section-subtitle">
            <el-icon><Document /></el-icon>
            {{ activeLevel }} - 二级指标间比较矩阵
          </h4>

          <el-radio-group v-model="selectedPrimaryForSecondary" size="small" class="secondary-tabs">
            <el-radio-button
              v-for="p in getPrimariesForLevel(activeLevel)"
              :key="p.code"
              :value="p.code"
            >
              {{ p.name }}
            </el-radio-button>
          </el-radio-group>

          <div v-if="getSecondaryMatrix(activeLevel, selectedPrimaryForSecondary)" class="matrix-container">
            <el-table
              :data="getSecondaryMatrixTable(activeLevel, selectedPrimaryForSecondary)"
              border
              size="small"
              class="matrix-table"
            >
              <el-table-column prop="row" label="" width="150" />
              <el-table-column
                v-for="col in getSecondaryHeaders(activeLevel, selectedPrimaryForSecondary)"
                :key="col"
                :prop="col"
                :label="col"
                align="center"
                width="90"
              >
                <template #default="{ row }">
                  {{ row[col] !== undefined ? Number(row[col]).toFixed(3) : '1.000' }}
                </template>
              </el-table-column>
            </el-table>

            <!-- 权重向量 -->
            <div class="weight-result">
              <h4>二级指标权重结果</h4>
              <el-table :data="getSecondaryWeightsTable(activeLevel, selectedPrimaryForSecondary)" border size="small">
                <el-table-column prop="name" label="二级指标" />
                <el-table-column label="权重" align="center">
                  <template #default="{ row }">
                    <el-tag type="primary" size="small">{{ (row.weight * 100).toFixed(2) }}%</el-tag>
                  </template>
                </el-table-column>
              </el-table>
              <el-alert
                :title="`λmax = ${getSecondaryMatrix(activeLevel, selectedPrimaryForSecondary)?.lambdaMax?.toFixed(4)}, CR = ${getSecondaryMatrix(activeLevel, selectedPrimaryForSecondary)?.cr?.toFixed(4)}, ${getSecondaryMatrix(activeLevel, selectedPrimaryForSecondary)?.cr < 0.1 ? '通过一致性检验' : '未通过一致性检验'}`"
                :type="getSecondaryMatrix(activeLevel, selectedPrimaryForSecondary)?.cr < 0.1 ? 'success' : 'warning'"
                :closable="false"
                style="margin-top: 12px"
              />
            </div>
          </div>
          <el-empty v-else description="请选择一个一级维度查看其下的二级指标矩阵" :image-size="60" />
        </div>
      </el-card>

      <!-- 3. 综合权重结果 -->
      <el-card class="result-card" shadow="never" style="margin-top: 16px;">
        <template #header>
          <div class="card-header">
            <span><el-icon><DataLine /></el-icon> 综合权重结果</span>
            <el-tag type="success" size="small">
              总权重: {{ totalWeight.toFixed(2) }}%
            </el-tag>
          </div>
        </template>

        <!-- 层级Tab切换 -->
        <el-radio-group v-model="activeWeightLevel" size="small" class="level-tabs" style="margin-bottom: 16px;">
          <el-radio-button v-for="level in levels" :key="level" :value="level">
            {{ level }}
          </el-radio-button>
        </el-radio-group>

        <el-table :data="filteredCombinedWeights" border stripe max-height="400" size="small">
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column prop="primaryName" label="一级维度" min-width="120" />
          <el-table-column prop="secondaryName" label="二级指标" min-width="150" />
          <el-table-column label="层级权重" align="center" width="100">
            <template #default="{ row }">
              {{ (row.levelWeight * 100).toFixed(2) }}%
            </template>
          </el-table-column>
          <el-table-column label="维度内权重" align="center" width="110">
            <template #default="{ row }">
              {{ (row.primaryWeight * 100).toFixed(2) }}%
            </template>
          </el-table-column>
          <el-table-column label="指标权重" align="center" width="100">
            <template #default="{ row }">
              {{ (row.secondaryWeight * 100).toFixed(2) }}%
            </template>
          </el-table-column>
          <el-table-column label="综合权重" align="center" width="120">
            <template #default="{ row }">
              <el-tag type="primary" size="small">
                {{ (row.combinedWeight * 100).toFixed(2) }}%
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <div class="sunburst-section">
          <h4><el-icon><DataLine /></el-icon> 综合权重旭日图</h4>
          <p class="sunburst-hint">内圈: 层级 | 中圈: 一级维度 | 外圈: 二级指标（扇区大小表示综合权重）</p>
          <div ref="sunburstChartRef" class="sunburst-chart"></div>
        </div>
      </el-card>
    </div>

    <!-- 空状态 -->
    <el-empty v-if="!selectedTemplateId" description="请先选择指标模板，然后选择专家并点击「预览集结」">
      <template #image>
        <el-icon :size="80" style="color: #c0c4cc"><TrendCharts /></el-icon>
      </template>
    </el-empty>

    <el-empty
      v-else-if="selectedTemplateId && !aggregationResult"
      :description="`模板「${selectedTemplateName}」已选择，请选择专家并点击「预览集结」`"
    >
      <template #image>
        <el-icon :size="80" style="color: #c0c4cc"><Cpu /></el-icon>
      </template>
    </el-empty>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import {
  TrendCharts,
  DataLine,
  Grid,
  View,
  Cpu,
  CircleCheck,
  CircleClose,
  FolderOpened,
  Document
} from '@element-plus/icons-vue'
import {
  getDynamicAhpAggregationTemplates,
  getDynamicAhpAggregationExperts,
  previewDynamicAhpAggregation,
  executeDynamicAhpAggregation
} from '@/api'

// ==================== 状态 ====================
const templates = ref([])
const availableExperts = ref([])
const selectedTemplateId = ref(null)
const selectedTemplateName = ref('')
const selectedExpertIds = ref([])
const expertSelectionMode = ref('all')
const aggregationResult = ref(null)

const previewLoading = ref(false)
const executeLoading = ref(false)

const activeLevel = ref('LEVEL_BETWEEN')
const selectedPrimaryForSecondary = ref('')
const activeWeightLevel = ref('')
const sunburstChartRef = ref(null)

// ==================== 计算属性 ====================
const canPreview = computed(() => {
  if (!selectedTemplateId.value) return false
  if (expertSelectionMode.value === 'all') return true
  return selectedExpertIds.value.length > 0
})

const levels = computed(() => {
  if (!aggregationResult.value?.primaryMatrices) return []
  return Object.keys(aggregationResult.value.primaryMatrices)
})

const expertWeights = computed(() => {
  return aggregationResult.value?.expertWeights || []
})

const crResults = computed(() => {
  return aggregationResult.value?.crResults || {}
})

const totalWeight = computed(() => {
  const weights = aggregationResult.value?.combinedWeights || []
  if (weights.length === 0) return 0
  return weights.reduce((sum, w) => sum + (w.combinedWeight || 0), 0) * 100
})

// 层级间矩阵
const levelBetweenMatrix = computed(() => {
  return aggregationResult.value?.levelBetweenMatrix || null
})

const levelBetweenHeaders = computed(() => {
  return levelBetweenMatrix.value?.headers || []
})

const levelBetweenWeights = computed(() => {
  return levelBetweenMatrix.value?.weights || []
})

const levelBetweenWeightsTable = computed(() => {
  const headers = levelBetweenHeaders.value
  const weights = levelBetweenMatrix.value?.weights || []
  return headers.map((name, i) => ({
    name,
    weight: weights[i] || 0
  }))
})

const formatLevelMatrix = computed(() => {
  if (!levelBetweenMatrix.value?.matrix) return []
  const headers = levelBetweenHeaders.value
  const matrix = levelBetweenMatrix.value.matrix
  return matrix.map((row, i) => {
    const data = { row: headers[i] }
    row.forEach((val, j) => {
      data[headers[j]] = val
    })
    return data
  })
})

// 一级维度矩阵
const getPrimaryMatrix = (level) => {
  if (!aggregationResult.value?.primaryMatrices) return null
  return aggregationResult.value.primaryMatrices[level] || null
}

const getPrimaryHeaders = (level) => {
  const matrix = getPrimaryMatrix(level)
  return matrix?.headers || []
}

const getPrimaryWeights = (level) => {
  const matrix = getPrimaryMatrix(level)
  return matrix?.weights || []
}

const getPrimaryWeightsTable = (level) => {
  const matrix = getPrimaryMatrix(level)
  if (!matrix) return []
  const headers = matrix.headers || []
  const weights = matrix.weights || []
  return headers.map((name, i) => ({
    name,
    weight: weights[i] || 0
  }))
}

const getPrimaryMatrixTable = (level) => {
  const matrix = getPrimaryMatrix(level)
  if (!matrix?.matrix) return []
  const headers = matrix.headers
  return matrix.matrix.map((row, i) => {
    const data = { row: headers[i] }
    row.forEach((val, j) => {
      data[headers[j]] = val
    })
    return data
  })
}

// 二级指标矩阵
const getSecondaryMatrix = (level, primaryCode) => {
  if (!aggregationResult.value?.secondaryMatrices) return null
  const levelMatrices = aggregationResult.value.secondaryMatrices[level]
  if (!levelMatrices) return null
  return levelMatrices[primaryCode] || null
}

const getSecondaryHeaders = (level, primaryCode) => {
  const matrix = getSecondaryMatrix(level, primaryCode)
  return matrix?.headers || []
}

const getSecondaryMatrixTable = (level, primaryCode) => {
  const matrix = getSecondaryMatrix(level, primaryCode)
  if (!matrix?.matrix) return []
  const headers = matrix.headers
  return matrix.matrix.map((row, i) => {
    const data = { row: headers[i] }
    row.forEach((val, j) => {
      data[headers[j]] = val
    })
    return data
  })
}

const getSecondaryWeightsTable = (level, primaryCode) => {
  const matrix = getSecondaryMatrix(level, primaryCode)
  if (!matrix) return []
  const headers = matrix.headers || []
  const weights = matrix.weights || []
  return headers.map((name, i) => ({
    name,
    weight: weights[i] || 0
  }))
}

const getPrimariesForLevel = (level) => {
  const tree = aggregationResult.value?.sunburstData
  if (!tree?.children) return []
  const levelNode = tree.children.find((l) => l.name === level)
  if (!levelNode?.children) return []
  return levelNode.children.map((p) => ({
    code: p.name,
    name: p.name
  }))
}

const getPrimaryName = (code) => {
  const primaries = getPrimariesForLevel(activeLevel.value)
  const found = primaries.find((p) => p.code === code)
  return found?.name || code
}

const combinedWeightsTable = computed(() => {
  return aggregationResult.value?.combinedWeights || []
})

const filteredCombinedWeights = computed(() => {
  if (!activeWeightLevel.value) return combinedWeightsTable.value
  return combinedWeightsTable.value.filter(w => w.levelName === activeWeightLevel.value)
})

// 层级权重表格数据
const levelWeightsTable = computed(() => {
  const levelWeights = aggregationResult.value?.levelWeights || {}
  return Object.entries(levelWeights).map(([name, weight]) => ({
    name,
    weight
  }))
})

// 一级维度权重表格数据
const primaryWeightsTable = computed(() => {
  const combinedWeights = aggregationResult.value?.combinedWeights || []
  const result = []

  // 按一级维度分组
  const byPrimary = {}
  for (const item of combinedWeights) {
    const key = `${item.levelName}|${item.primaryName}`
    if (!byPrimary[key]) {
      byPrimary[key] = {
        levelName: item.levelName,
        primaryName: item.primaryName,
        weight: 0
      }
    }
    byPrimary[key].weight += (item.combinedWeight || 0)
  }

  return Object.values(byPrimary).sort((a, b) => {
    if (a.levelName !== b.levelName) return a.levelName.localeCompare(b.levelName)
    return b.weight - a.weight
  })
})

// ==================== 方法 ====================
const loadTemplates = async () => {
  try {
    const res = await getDynamicAhpAggregationTemplates()
    templates.value = res || []
  } catch (e) {
    console.error('加载模板失败:', e)
    templates.value = []
  }
}

const loadExperts = async () => {
  if (!selectedTemplateId.value) {
    availableExperts.value = []
    return
  }
  try {
    const res = await getDynamicAhpAggregationExperts(selectedTemplateId.value)
    availableExperts.value = res || []
  } catch (e) {
    console.error('加载专家失败:', e)
    availableExperts.value = []
  }
}

const handleTemplateChange = () => {
  const tpl = templates.value.find((t) => t.id === selectedTemplateId.value)
  selectedTemplateName.value = tpl?.templateName || ''
  aggregationResult.value = null
  loadExperts()
}

const previewAggregation = async () => {
  if (!canPreview.value) return

  previewLoading.value = true
  try {
    const data = {
      templateId: selectedTemplateId.value,
      useAllExperts: expertSelectionMode.value === 'all',
      expertIds: expertSelectionMode.value === 'specific' ? selectedExpertIds.value : undefined
    }
    aggregationResult.value = await previewDynamicAhpAggregation(data)
    activeLevel.value = 'LEVEL_BETWEEN'

    if (levels.value.length > 0) {
      activeLevel.value = levels.value[0]
      activeWeightLevel.value = levels.value[0]
      const primaries = getPrimariesForLevel(levels.value[0])
      if (primaries.length > 0) {
        selectedPrimaryForSecondary.value = primaries[0].code
      }
    }

    nextTick(() => {
      renderSunburst()
    })

    ElMessage.success('预览集结成功')
  } catch (e) {
    console.error('预览集结失败:', e)
    ElMessage.error('预览集结失败: ' + (e.message || e.description || '未知错误'))
  } finally {
    previewLoading.value = false
  }
}

const executeAggregation = async () => {
  if (!canPreview.value) return

  executeLoading.value = true
  try {
    const data = {
      templateId: selectedTemplateId.value,
      useAllExperts: expertSelectionMode.value === 'all',
      expertIds: expertSelectionMode.value === 'specific' ? selectedExpertIds.value : undefined
    }
    aggregationResult.value = await executeDynamicAhpAggregation(data)
    activeLevel.value = 'LEVEL_BETWEEN'

    if (levels.value.length > 0) {
      activeLevel.value = levels.value[0]
      activeWeightLevel.value = levels.value[0]
      const primaries = getPrimariesForLevel(levels.value[0])
      if (primaries.length > 0) {
        selectedPrimaryForSecondary.value = primaries[0].code
      }
    }

    nextTick(() => {
      renderSunburst()
    })

    ElMessage.success(`集结计算完成，groupId: ${aggregationResult.value.groupId}`)
  } catch (e) {
    console.error('执行集结失败:', e)
    ElMessage.error('执行集结失败: ' + (e.message || e.description || '未知错误'))
  } finally {
    executeLoading.value = false
  }
}

const getWeightColor = (weight) => {
  const pct = weight * 100
  if (pct >= 15) return '#67C23A'
  if (pct >= 8) return '#409EFF'
  if (pct >= 4) return '#E6A23C'
  return '#909399'
}

// 旭日图
const renderSunburst = () => {
  if (!sunburstChartRef.value || !aggregationResult.value?.sunburstData) return

  if (sunburstChart) {
    const dom = sunburstChart.getDom()
    if (!dom?.isConnected) {
      sunburstChart.dispose()
      sunburstChart = null
    }
  }
  if (!sunburstChart) {
    sunburstChart = echarts.init(sunburstChartRef.value)
  }

  const data = buildSunburstData(aggregationResult.value.sunburstData)

  const option = {
    tooltip: {
      trigger: 'item',
      confine: true,
      formatter: (params) => {
        const v = params.value
        const pct = typeof v === 'number' ? `${v.toFixed(4)}%` : String(params.value)
        const extra = params.data?.fullPath
        if (extra && params.treePathInfo?.length >= 4) {
          return `<div style="max-width:300px;line-height:1.6">${extra}<br/>占全体：<b>${pct}</b></div>`
        }
        const path = params.treePathInfo?.map((x) => x.name).filter(Boolean).slice(1).join(' → ') || ''
        return `<div style="max-width:300px;line-height:1.6">${path}<br/>占全体：<b>${pct}</b></div>`
      }
    },
    series: [
      {
        type: 'sunburst',
        data: [data],
        radius: ['10%', '92%'],
        sort: 'desc',
        nodeClick: false,
        emphasis: {
          focus: 'ancestor',
          itemStyle: { shadowBlur: 8, shadowColor: 'rgba(0,0,0,0.18)' }
        },
        itemStyle: {
          borderRadius: 2,
          borderWidth: 1,
          borderColor: 'rgba(255,255,255,0.92)'
        },
        levels: [
          { r0: '0%', r: '10%', label: { show: false }, itemStyle: { color: '#001f3f', borderWidth: 0 } },
          {
            r0: '10%',
            r: '30%',
            label: {
              show: true,
              rotate: 'radial',
              minAngle: 6,
              fontSize: 11,
              fontWeight: 'bold',
              color: '#fff',
              textBorderColor: 'rgba(0,31,63,0.45)',
              textBorderWidth: 1.2,
              position: 'inside',
              overflow: 'truncate',
              width: 56
            },
            itemStyle: { borderWidth: 1 }
          },
          {
            r0: '30%',
            r: '54%',
            label: {
              show: true,
              rotate: 'radial',
              minAngle: 4,
              fontSize: 10,
              color: '#fff',
              textBorderColor: 'rgba(0,0,0,0.25)',
              position: 'inside',
              overflow: 'truncate',
              width: 64
            },
            itemStyle: { borderWidth: 1 }
          },
          { r0: '54%', r: '92%', label: { show: false }, itemStyle: { borderWidth: 1 } }
        ]
      }
    ]
  }

  sunburstChart.setOption(option)
  sunburstChart.resize()
}

let sunburstChart = null

const buildSunburstData = (sunburstData) => {
  if (!sunburstData) return null

  // 维度颜色调色板
  const SB_DIM_PALETTE = [
    '#3498db', '#e74c3c', '#2ecc71', '#9b59b6', '#f39c12',
    '#1abc9c', '#e67e22', '#34495e', '#16a085', '#c0392b',
    '#27ae60', '#8e44ad', '#d35400', '#2980b9', '#7f8c8d'
  ]

  // 叶子指标颜色（对基础色进行明度调整）
  const buildChildren = (children, level, colorIndex) => {
    if (!children || children.length === 0) return []

    return children.map((child, idx) => {
      const baseColor = level === 0
        ? SB_DIM_PALETTE[colorIndex % SB_DIM_PALETTE.length]
        : level === 1
          ? echarts.color.lift(SB_DIM_PALETTE[colorIndex % SB_DIM_PALETTE.length], -0.1)
          : SB_DIM_PALETTE[colorIndex % SB_DIM_PALETTE.length]

      const leafColor = level === 1
        ? echarts.color.lift(baseColor, (idx % 5) * 0.07)
        : baseColor

      return {
        name: child.name,
        value: child.value || 0,
        fullPath: child.fullPath || child.name,
        itemStyle: { color: leafColor },
        children: buildChildren(child.children, level + 1, idx)
      }
    })
  }

  return {
    name: sunburstData.name || '综合权重',
    itemStyle: { color: '#001f3f' },
    children: buildChildren(sunburstData.children || [], 0, 0)
  }
}

onMounted(() => {
  loadTemplates()
  window.addEventListener('resize', () => {
    if (aggregationResult.value?.sunburstData && sunburstChart) {
      sunburstChart.resize()
    }
  })
})

// 监听层级切换，自动选择第一个一级维度
watch(activeLevel, (newLevel) => {
  const primaries = getPrimariesForLevel(newLevel)
  if (primaries.length > 0) {
    selectedPrimaryForSecondary.value = primaries[0].code
  }
})
</script>

<style scoped lang="scss">
.dynamic-ahp-aggregation {
  padding: 20px;
  max-width: 1600px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;

  h2 {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 20px;
    color: #001f3f;
    margin-bottom: 6px;

    .el-icon {
      font-size: 24px;
      color: #409eff;
    }
  }

  .subtitle {
    color: #606266;
    font-size: 13px;
    margin: 0;
  }
}

.toolbar-card {
  margin-bottom: 16px;

  :deep(.el-card__body) {
    padding: 12px 16px;
  }
}

.toolbar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.aggregation-content {
  .result-card {
    margin-bottom: 16px;

    :deep(.el-card__header) {
      padding: 12px 16px;
      background: linear-gradient(135deg, #1e3a5f 0%, #2d5a8a 100%);
      color: #ffffff;
    }
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  color: #ffffff;

  .el-icon {
    margin-right: 8px;
  }
}

.credibility-cell {
  display: flex;
  align-items: center;
  gap: 8px;

  .credibility-value {
    font-weight: 600;
    color: #409eff;
  }
}

.cr-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px 8px;
  background: #f5f7fa;
  border-radius: 6px;
  margin-bottom: 8px;

  .cr-label {
    font-size: 13px;
    color: #606266;
    text-align: center;
  }
}

.matrix-selector {
  margin-bottom: 16px;
}

.matrix-section {
  margin-top: 16px;

  h4 {
    margin: 0 0 12px;
    color: #303133;
    font-size: 15px;
  }

  h5 {
    margin: 16px 0 12px;
    color: #606266;
    font-size: 14px;
  }
}

.matrix-container {
  background: #fafbfc;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
}

.matrix-table {
  margin-bottom: 16px;

  :deep(.el-table__header th) {
    background: linear-gradient(135deg, #1e3a5f 0%, #2d5a8a 100%);
    color: #ffffff;
  }

  &.small {
    font-size: 12px;
  }
}

.weight-vector {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f0f9eb;
  border-radius: 6px;

  .weight-label {
    font-weight: 600;
    color: #67c23a;
  }

  .weight-tag {
    margin: 4px;
  }
}

.aggregation-detail {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
  margin-bottom: 12px;

  p {
    margin: 0 0 8px;
    line-height: 1.8;
  }

  ol {
    margin: 8px 0 0;
    padding-left: 20px;
    color: #606266;
  }
}

.combined-weight {
  display: block;
  text-align: center;
  font-weight: 600;
  color: #303133;
  font-size: 12px;
  margin-top: 4px;
}

.secondary-tabs {
  margin-bottom: 16px;

  :deep(.el-radio-button__inner) {
    border-radius: 0;
  }
}

.weight-table-section {
  h5 {
    margin: 0 0 8px;
    color: #303133;
    font-size: 13px;
    font-weight: 600;
  }

  .weight-value {
    display: block;
    text-align: center;
    font-weight: 600;
    color: #303133;
    font-size: 12px;
    margin-top: 2px;
  }

  :deep(.el-table) {
    .el-progress {
      margin-bottom: 2px;
    }
  }
}

.weight-overview-section {
  margin-bottom: 20px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;

  h5 {
    display: flex;
    align-items: center;
    gap: 6px;
    margin: 0 0 12px;
    color: #303133;
    font-size: 14px;

    .el-icon {
      color: #409eff;
    }
  }

  .weight-value {
    display: block;
    text-align: center;
    font-weight: 600;
    color: #303133;
    font-size: 13px;
    margin-top: 4px;
  }

  :deep(.el-table) {
    .el-progress {
      margin-bottom: 2px;
    }
  }
}

.sunburst-section {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px dashed #e4e7ed;

  h4 {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0 0 8px;
    color: #303133;
    font-size: 15px;

    .el-icon {
      color: #409eff;
    }
  }

  .sunburst-hint {
    color: #909399;
    font-size: 12px;
    margin: 0 0 16px;
  }
}

.sunburst-chart {
  width: 100%;
  height: 450px;
  min-height: 400px;
}
</style>
