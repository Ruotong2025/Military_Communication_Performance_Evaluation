<template>
  <el-card class="ahp-config-card">
    <template #header>
      <div class="card-header">
        <el-icon class="header-icon"><Setting /></el-icon>
        <span>AHP权重配置</span>
      </div>
    </template>

    <div class="config-content">
      <!-- 优先级输入 -->
      <div class="priority-section">
        <h3 class="section-title">
          <el-icon><Edit /></el-icon>
          输入8个维度的优先级（1-8，数字越小优先级越高）
        </h3>
        
        <el-form :model="priorities" label-width="120px">
          <el-row :gutter="20">
            <el-col :span="6" v-for="dim in dimensions" :key="dim.code">
              <el-form-item :label="dim.name">
                <el-input-number
                  v-model="priorities[dim.code]"
                  :min="1"
                  :max="8"
                  size="large"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <div class="action-buttons">
          <el-button
            type="primary"
            size="large"
            :loading="calculating"
            @click="calculateAHP"
          >
            <el-icon><Promotion /></el-icon>
            生成AHP矩阵并计算权重
          </el-button>
          
          <el-button size="large" @click="resetPriorities">
            <el-icon><RefreshLeft /></el-icon>
            重置为默认值
          </el-button>
        </div>
      </div>

      <!-- AHP结果展示 -->
      <div v-if="ahpResult" class="result-section">
        <!-- 判断矩阵 -->
        <div class="matrix-section">
          <h3 class="section-title">
            <el-icon><Grid /></el-icon>
            AHP判断矩阵
          </h3>
          <el-table :data="matrixTableData" border size="small" class="matrix-table">
            <el-table-column prop="dimension" label="" width="100" fixed />
            <el-table-column
              v-for="dim in dimensions"
              :key="dim.code"
              :prop="dim.code"
              :label="dim.name"
              width="80"
              align="center"
            >
              <template #default="{ row }">
                <span :class="{ 'matrix-diagonal': row[dim.code] === 1 }">
                  {{ row[dim.code].toFixed(2) }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 权重结果 -->
        <div class="weights-section">
          <h3 class="section-title">
            <el-icon><PieChart /></el-icon>
            权重结果
          </h3>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-descriptions :column="2" border>
                <el-descriptions-item
                  v-for="dim in dimensions"
                  :key="dim.code"
                  :label="dim.name"
                >
                  <el-tag type="primary" size="large" effect="dark">
                    {{ (ahpResult.weights[dim.code] * 100).toFixed(2) }}%
                  </el-tag>
                </el-descriptions-item>
              </el-descriptions>
            </el-col>
            
            <el-col :span="12">
              <div ref="weightChartRef" style="height: 300px"></div>
            </el-col>
          </el-row>
        </div>

        <!-- 一致性检验 -->
        <div class="consistency-section">
          <el-alert
            :title="`一致性比率 CR = ${ahpResult.cr.toFixed(4)} ${ahpResult.consistent ? '✓ 通过一致性检验' : '✗ 未通过一致性检验'}`"
            :type="ahpResult.consistent ? 'success' : 'error'"
            :closable="false"
            show-icon
          >
            <template #default>
              <p v-if="ahpResult.consistent">
                CR < 0.1，判断矩阵具有满意的一致性，权重结果可靠。
              </p>
              <p v-else>
                CR ≥ 0.1，判断矩阵一致性不足，建议调整优先级设置。
              </p>
            </template>
          </el-alert>
        </div>

        <!-- 熵权法权重展示 -->
        <div class="entropy-weights-section" v-if="pythonResult && pythonResult.indicatorWeights">
          <h3 class="section-title">
            <el-icon><DataAnalysis /></el-icon>
            熵权法指标权重与得分计算详情
          </h3>
          
          <!-- 测试批次选择器 -->
          <div class="batch-selector" v-if="pythonResult.evaluationResults && pythonResult.evaluationResults.length > 0">
            <el-form inline>
              <el-form-item label="选择测试批次：">
                <el-select v-model="selectedBatchId" placeholder="请选择测试批次" style="width: 300px;">
                  <el-option
                    v-for="result in pythonResult.evaluationResults"
                    :key="result.evaluationId"
                    :label="`实验编号: ${result.testId}`"
                    :value="result.evaluationId"
                  />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-tag v-if="selectedBatch" type="success" size="large" effect="dark">
                  综合得分: {{ selectedBatch.totalScore.toFixed(2) }}
                </el-tag>
              </el-form-item>
              <el-form-item>
                <el-tag v-if="selectedBatch" type="warning" size="large" effect="dark">
                  排名: {{ selectedBatch.rank }}
                </el-tag>
              </el-form-item>
            </el-form>
          </div>
          
          <el-collapse accordion v-if="selectedBatch">
            <el-collapse-item
              v-for="dim in dimensions"
              :key="dim.code"
              :name="dim.code"
            >
              <template #title>
                <div style="display: flex; align-items: center; gap: 10px; width: 100%;">
                  <span style="font-weight: bold; font-size: 16px;">{{ dim.name }}</span>
                  <el-tag type="primary" effect="dark">
                    AHP权重: {{ (pythonResult.dimensionWeights[dim.code].weight * 100).toFixed(2) }}%
                  </el-tag>
                  <el-tag v-if="selectedBatch.dimensionCalculations[dim.code]" type="success" effect="dark">
                    维度得分: {{ selectedBatch.dimensionCalculations[dim.code].dimensionScore.toFixed(2) }}
                  </el-tag>
                </div>
              </template>
              
              <div v-if="selectedBatch.dimensionCalculations[dim.code]">
                <!-- 计算公式说明 -->
                <el-alert
                  :title="`维度得分 = Σ(归一化得分 × 熵权值 × AHP权重) = Σ贡献值`"
                  type="info"
                  :closable="false"
                  style="margin-bottom: 15px;"
                >
                  <template #default>
                    <p style="margin: 5px 0; font-size: 13px;">
                      其中 AHP权重 = {{ (pythonResult.dimensionWeights[dim.code].weight * 100).toFixed(2) }}%
                    </p>
                  </template>
                </el-alert>
                
                <!-- 详细计算表格 -->
                <el-table
                  :data="selectedBatch.dimensionCalculations[dim.code].indicators"
                  border
                  size="small"
                  style="width: 100%"
                  show-summary
                  :summary-method="(param) => getSummaryRow(param, dim.code)"
                >
                  <el-table-column prop="name" label="指标名称" width="180" fixed />
                  
                  <el-table-column label="原始数据" width="120" align="center">
                    <template #default="{ row }">
                      <el-tag type="info">{{ row.rawValue.toFixed(4) }}</el-tag>
                    </template>
                  </el-table-column>
                  
                  <el-table-column label="归一化得分" width="120" align="center">
                    <template #default="{ row }">
                      <el-tag type="success">{{ row.normalizedValue.toFixed(2) }}</el-tag>
                    </template>
                  </el-table-column>
                  
                  <el-table-column label="熵权值" width="100" align="center">
                    <template #default="{ row }">
                      <span style="font-size: 12px;">{{ (row.entropyWeight * 100).toFixed(2) }}%</span>
                    </template>
                  </el-table-column>
                  
                  <el-table-column label="贡献值" width="120" align="center">
                    <template #default="{ row }">
                      <el-tag type="warning">{{ getContribution(row, dim.code).toFixed(4) }}</el-tag>
                    </template>
                  </el-table-column>
                  
                  <el-table-column label="计算过程" align="left" min-width="350">
                    <template #default="{ row }">
                      <span style="font-size: 12px; color: #666; font-family: monospace;">
                        {{ row.normalizedValue.toFixed(2) }} × {{ (row.entropyWeight * 100).toFixed(2) }}% × {{ (pythonResult.dimensionWeights[dim.code].weight * 100).toFixed(2) }}%
                        = {{ getContribution(row, dim.code).toFixed(4) }}
                      </span>
                    </template>
                  </el-table-column>
                  
                  <el-table-column label="最终权重" width="100" align="center">
                    <template #default="{ row }">
                      <span style="font-size: 12px;">{{ (row.finalWeight * 100).toFixed(2) }}%</span>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-collapse-item>
          </el-collapse>
          
          <!-- 如果没有选择批次，显示权重信息 -->
          <el-collapse accordion v-else>
            <el-collapse-item
              v-for="dim in dimensions"
              :key="dim.code"
              :name="dim.code"
            >
              <template #title>
                <div style="display: flex; align-items: center; gap: 10px; width: 100%;">
                  <span style="font-weight: bold; font-size: 16px;">{{ dim.name }}</span>
                  <el-tag type="primary" effect="dark">
                    AHP权重: {{ (pythonResult.dimensionWeights[dim.code].weight * 100).toFixed(2) }}%
                  </el-tag>
                </div>
              </template>
              
              <el-table
                :data="getIndicatorWeights(dim.code)"
                border
                size="small"
                style="width: 100%"
              >
                <el-table-column prop="name" label="指标名称" width="250" />
                <el-table-column label="熵权值" width="150" align="center">
                  <template #default="{ row }">
                    <el-tag type="success" size="large">{{ (row.entropyWeight * 100).toFixed(2) }}%</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="最终权重" width="150" align="center">
                  <template #default="{ row }">
                    <el-tag type="primary" size="large">{{ (row.finalWeight * 100).toFixed(2) }}%</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="权重说明" align="left">
                  <template #default="{ row }">
                    <span style="font-size: 12px; color: #666;">
                      最终权重 = AHP权重({{ (pythonResult.dimensionWeights[dim.code].weight * 100).toFixed(2) }}%) 
                      × 熵权值({{ (row.entropyWeight * 100).toFixed(2) }}%)
                      = {{ (row.finalWeight * 100).toFixed(2) }}%
                    </span>
                  </template>
                </el-table-column>
              </el-table>
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { ref, reactive, watch, nextTick, h } from 'vue'
import { Setting, Edit, Promotion, RefreshLeft, Grid, PieChart, DataAnalysis } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { calculateAHP as calculateAHPApi } from '@/api'
import { calculatePythonEvaluation } from '@/api/pythonEvaluation'

const emit = defineEmits(['weights-calculated'])

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

const priorities = reactive({
  RL: 1, SC: 2, AJ: 3, EF: 4,
  PO: 5, NC: 6, HO: 7, RS: 8
})

const calculating = ref(false)
const ahpResult = ref(null)
const pythonResult = ref(null)
const matrixTableData = ref([])
const weightChartRef = ref(null)
let weightChart = null

// 测试批次选择
const selectedBatchId = ref(null)
const selectedBatch = ref(null)

// 监听批次选择变化
watch(selectedBatchId, (newBatchId) => {
  if (newBatchId && pythonResult.value && pythonResult.value.evaluationResults) {
    selectedBatch.value = pythonResult.value.evaluationResults.find(
      result => result.evaluationId === newBatchId
    )
    console.log('[DEBUG] 选择的批次:', selectedBatch.value)
  } else {
    selectedBatch.value = null
  }
})

// 监听 pythonResult 变化，自动选择第一个批次
watch(pythonResult, (newResult) => {
  if (newResult && newResult.evaluationResults && newResult.evaluationResults.length > 0) {
    selectedBatchId.value = newResult.evaluationResults[0].evaluationId
    console.log('[DEBUG] 自动选择第一个批次 ID:', selectedBatchId.value)
  } else {
    selectedBatchId.value = null
    selectedBatch.value = null
  }
})

// 计算AHP
const calculateAHP = async () => {
  calculating.value = true
  try {
    console.log('[DEBUG] 开始计算 AHP，当前优先级配置:', priorities)
    
    // 调用 Python 评估服务计算综合评估（包含 AHP 矩阵和权重）
    console.log('[DEBUG] 调用 Python 评估服务，传入优先级:', priorities)
    pythonResult.value = await calculatePythonEvaluation(priorities)
    console.log('[DEBUG] Python 评估结果:', pythonResult.value)
    console.log('[DEBUG] Python 返回的维度权重:', pythonResult.value?.dimensionWeights)
    console.log('[DEBUG] Python 返回的 AHP 矩阵:', pythonResult.value?.ahpMatrix)
    
    // 使用 Python 返回的结果构建 ahpResult
    ahpResult.value = {
      matrix: pythonResult.value.ahpMatrix,
      weights: pythonResult.value.weights,
      cr: pythonResult.value.consistencyRatio,
      consistent: pythonResult.value.consistencyPassed
    }
    console.log('[DEBUG] 构建的 ahpResult:', ahpResult.value)
    
    // 生成矩阵表格数据
    generateMatrixTableData()
    
    // 绘制权重饼图
    await nextTick()
    renderWeightChart()
    
    // 通知父组件
    emit('weights-calculated', {
      weights: ahpResult.value.weights,
      pythonResult: pythonResult.value
    })
    
    ElMessage.success('AHP权重计算成功！')
  } catch (error) {
    ElMessage.error('计算失败：' + error.message)
    console.error('[ERROR] 计算失败:', error)
  } finally {
    calculating.value = false
  }
}

// 生成矩阵表格数据
const generateMatrixTableData = () => {
  const matrix = ahpResult.value.matrix
  matrixTableData.value = dimensions.map((dim, i) => {
    const row = { dimension: dim.name }
    dimensions.forEach((d, j) => {
      row[d.code] = matrix[i][j]
    })
    return row
  })
}

// 绘制权重饼图
const renderWeightChart = () => {
  if (!weightChartRef.value) return
  
  if (!weightChart) {
    weightChart = echarts.init(weightChartRef.value)
  }
  
  const data = dimensions.map(dim => ({
    name: dim.name,
    value: (ahpResult.value.weights[dim.code] * 100).toFixed(2)
  }))
  
  const option = {
    title: {
      text: '权重分布',
      left: 'center',
      textStyle: {
        color: '#001f3f',
        fontSize: 16,
        fontWeight: 'bold'
      }
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}%'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      textStyle: {
        color: '#001f3f'
      }
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}\n{c}%',
          color: '#001f3f'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold'
          }
        },
        data: data,
        color: ['#0074D9', '#7FDBFF', '#39CCCC', '#3D9970', '#2ECC40', '#01FF70', '#FFDC00', '#FF851B']
      }
    ]
  }
  
  weightChart.setOption(option)
}

// 获取指标权重数据
const getIndicatorWeights = (dimCode) => {
  if (!pythonResult.value || !pythonResult.value.indicatorWeights) return []
  
  const dimWeights = pythonResult.value.indicatorWeights[dimCode]
  if (!dimWeights) return []
  
  return dimWeights.indicators
}

// 计算贡献值：归一化得分 × 熵权值 × AHP权重
const getContribution = (row, dimCode) => {
  const normalizedValue = row.normalizedValue
  const entropyWeight = row.entropyWeight
  const ahpWeight = pythonResult.value.dimensionWeights[dimCode].weight
  return normalizedValue * entropyWeight * ahpWeight
}

// 计算汇总行
const getSummaryRow = (param, dimCode) => {
  const { columns, data } = param
  const sums = []
  
  columns.forEach((column, index) => {
    if (index === 0) {
      sums[index] = '合计'
      return
    }
    
    if (column.label === '贡献值') {
      // 贡献值列：计算所有指标的贡献值总和（即维度得分）
      let totalContribution = 0
      data.forEach(row => {
        totalContribution += getContribution(row, dimCode)
      })
      
      sums[index] = h('div', { style: 'display: flex; flex-direction: column; align-items: center;' }, [
        h('el-tag', { type: 'danger', size: 'large', effect: 'dark' }, `维度得分 = ${totalContribution.toFixed(4)}`)
      ])
    } else if (column.label === '归一化得分') {
      // 归一化得分列：显示平均值
      const values = data.map(item => Number(item.normalizedValue))
      const sum = values.reduce((prev, curr) => prev + curr, 0)
      const avg = sum / values.length
      sums[index] = `平均 = ${avg.toFixed(2)}`
    } else if (column.label === '熵权值') {
      // 熵权值列：显示总和
      const values = data.map(item => Number(item.entropyWeight))
      const sum = values.reduce((prev, curr) => prev + curr, 0)
      sums[index] = `Σ = ${(sum * 100).toFixed(2)}%`
    } else {
      sums[index] = ''
    }
  })
  
  return sums
}

// 重置优先级
const resetPriorities = () => {
  priorities.RL = 1
  priorities.SC = 2
  priorities.AJ = 3
  priorities.EF = 4
  priorities.PO = 5
  priorities.NC = 6
  priorities.HO = 7
  priorities.RS = 8
  ahpResult.value = null
  pythonResult.value = null
}
</script>

<style scoped lang="scss">
.ahp-config-card {
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

.config-content {
  .section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 20px;
    color: var(--navy-primary);
    font-size: 16px;
    font-weight: bold;
    padding-bottom: 10px;
    border-bottom: 2px solid var(--navy-light);
  }

  .priority-section {
    margin-bottom: 30px;

    .action-buttons {
      display: flex;
      justify-content: center;
      gap: 20px;
      margin-top: 30px;
    }
  }

  .result-section {
    .matrix-section,
    .weights-section,
    .consistency-section,
    .entropy-weights-section {
      margin-bottom: 30px;
    }

    .matrix-table {
      :deep(td) {
        text-align: center;
      }

      .matrix-diagonal {
        font-weight: bold;
        color: var(--navy-secondary);
      }
    }

    .entropy-weights-section {
      .batch-selector {
        background: linear-gradient(135deg, #f0f9ff, #e0f2fe);
        padding: 20px;
        border-radius: 8px;
        margin-bottom: 20px;
        border: 2px solid var(--navy-light);

        :deep(.el-form-item__label) {
          font-weight: bold;
          color: var(--navy-primary);
        }
      }

      :deep(.el-collapse-item__header) {
        background: linear-gradient(135deg, var(--navy-primary), var(--navy-secondary));
        color: white;
        font-weight: bold;
        padding: 15px 20px;
        border-radius: 8px;
        margin-bottom: 10px;
      }

      :deep(.el-collapse-item__content) {
        padding: 15px;
      }

      :deep(.el-table) {
        .el-table__footer-wrapper {
          .cell {
            font-weight: bold;
            color: var(--navy-primary);
          }
        }
      }
    }
  }
}
</style>
