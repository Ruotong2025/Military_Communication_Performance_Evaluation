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
            熵权法指标权重与得分计算详情 - 所有实验对比
          </h3>
          
          <!-- 按维度展示大表格 -->
          <el-collapse v-if="pythonResult.evaluationResults && pythonResult.evaluationResults.length > 0" accordion>
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
              
              <div>
                <!-- 计算公式说明 -->
                <el-alert
                  :title="`维度得分 = Σ(归一化得分 × 熵权值 × AHP权重)`"
                  type="info"
                  :closable="false"
                  style="margin-bottom: 15px;"
                >
                  <template #default>
                    <p style="margin: 5px 0; font-size: 13px;">
                      AHP权重 = {{ (pythonResult.dimensionWeights[dim.code].weight * 100).toFixed(2) }}% | 
                      贡献值 = 归一化得分 × 熵权值 × AHP权重
                    </p>
                  </template>
                </el-alert>
                
                <!-- 横向对比大表格 -->
                <el-table
                  :data="getComparisonTableData(dim.code)"
                  border
                  size="small"
                  style="width: 100%"
                  :max-height="600"
                >
                  <!-- 指标名称列（固定） -->
                  <el-table-column prop="indicatorName" label="指标名称" width="150" fixed>
                    <template #default="{ row }">
                      <div style="font-weight: bold; font-size: 13px;">{{ row.indicatorName }}</div>
                      <div v-if="!row.isTotal" style="font-size: 11px; color: #666; margin-top: 3px;">
                        熵权: {{ row.entropyWeight }}
                      </div>
                    </template>
                  </el-table-column>
                  
                  <!-- 每个实验一列 -->
                  <el-table-column
                    v-for="result in pythonResult.evaluationResults"
                    :key="result.evaluationId"
                    :label="result.testId"
                    width="160"
                    align="center"
                  >
                    <template #header>
                      <div style="display: flex; flex-direction: column; align-items: center; gap: 3px; padding: 5px 0;">
                        <span style="font-weight: bold; font-size: 13px;">{{ result.testId }}</span>
                        <el-tag type="success" size="small" effect="plain">得分: {{ result.totalScore.toFixed(2) }}</el-tag>
                        <el-tag type="warning" size="small" effect="plain">排名: {{ result.rank }}</el-tag>
                      </div>
                    </template>
                    <template #default="{ row }">
                      <!-- 维度得分行 -->
                      <div v-if="row.isTotal" style="background: linear-gradient(135deg, #f0f9ff, #e0f2fe); padding: 10px; border-radius: 4px;">
                        <el-tag type="danger" size="large" effect="dark">
                          {{ row.experiments[result.evaluationId] }}
                        </el-tag>
                      </div>
                      <!-- 指标数据行 -->
                      <div v-else style="display: flex; flex-direction: column; gap: 4px; padding: 8px 5px; line-height: 1.4;">
                        <div style="font-size: 11px; color: #666;">
                          原始: <span style="font-weight: bold; color: #333;">{{ row.experiments[result.evaluationId]?.raw }}</span>
                        </div>
                        <div style="font-size: 11px; color: #666;">
                          归一: <el-tag type="success" size="small" effect="plain">{{ row.experiments[result.evaluationId]?.normalized }}</el-tag>
                        </div>
                        <div style="font-size: 11px; color: #666;">
                          贡献: <el-tag type="warning" size="small" effect="plain">{{ row.experiments[result.evaluationId]?.contribution }}</el-tag>
                        </div>
                      </div>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-collapse-item>
          </el-collapse>
          
          <!-- 如果没有实验数据，显示权重信息 -->
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

// 获取对比表格数据
const getComparisonTableData = (dimCode) => {
  if (!pythonResult.value || !pythonResult.value.evaluationResults) return []
  
  const dimInfo = INDICATOR_SYSTEM[dimCode]
  if (!dimInfo) return []
  
  const tableData = []
  
  // 为每个指标创建一行
  dimInfo.indicators.forEach(indicator => {
    const row = {
      indicatorName: indicator.name,
      indicatorCode: indicator.code,
      entropyWeight: `${(pythonResult.value.indicatorWeights[dimCode].indicators.find(ind => ind.code === indicator.code)?.entropyWeight * 100 || 0).toFixed(2)}%`,
      experiments: {},
      isTotal: false
    }
    
    // 为每个实验添加数据
    pythonResult.value.evaluationResults.forEach(result => {
      const dimCalc = result.dimensionCalculations[dimCode]
      if (dimCalc) {
        const indicatorData = dimCalc.indicators.find(ind => ind.code === indicator.code)
        if (indicatorData) {
          row.experiments[result.evaluationId] = {
            raw: indicatorData.rawValue.toFixed(4),
            normalized: indicatorData.normalizedValue.toFixed(2),
            contribution: getContribution(indicatorData, dimCode).toFixed(4)
          }
        }
      }
    })
    
    tableData.push(row)
  })
  
  // 添加维度得分行
  const totalRow = {
    indicatorName: '维度得分',
    indicatorCode: 'total',
    entropyWeight: '',
    experiments: {},
    isTotal: true
  }
  
  pythonResult.value.evaluationResults.forEach(result => {
    const dimCalc = result.dimensionCalculations[dimCode]
    if (dimCalc) {
      let totalContribution = 0
      dimCalc.indicators.forEach(indicator => {
        totalContribution += getContribution(indicator, dimCode)
      })
      totalRow.experiments[result.evaluationId] = totalContribution.toFixed(4)
    }
  })
  
  tableData.push(totalRow)
  
  return tableData
}

// INDICATOR_SYSTEM 定义（与 Python 保持一致）
const INDICATOR_SYSTEM = {
  'RL': {
    'name': '可靠性',
    'indicators': [
      {'code': 'RL_communication_availability_rate', 'name': '通信可用性'},
      {'code': 'RL_communication_success_rate', 'name': '通信成功率'},
      {'code': 'RL_recovery_duration_ms', 'name': '恢复时长'},
      {'code': 'RL_crash_rate', 'name': '崩溃比例'}
    ]
  },
  'SC': {
    'name': '安全性',
    'indicators': [
      {'code': 'SC_key_compromise_frequency', 'name': '密钥泄露频率'},
      {'code': 'SC_detection_probability', 'name': '被侦察概率'},
      {'code': 'SC_interception_resistance', 'name': '抗拦截能力'}
    ]
  },
  'AJ': {
    'name': '抗干扰性',
    'indicators': [
      {'code': 'AJ_avg_sinr', 'name': '平均信干噪比'},
      {'code': 'AJ_avg_jamming_margin', 'name': '平均抗干扰余量'}
    ]
  },
  'EF': {
    'name': '有效性',
    'indicators': [
      {'code': 'EF_avg_communication_distance', 'name': '平均通信距离'},
      {'code': 'EF_avg_ber', 'name': '平均误码率'},
      {'code': 'EF_avg_plr', 'name': '平均丢包率'},
      {'code': 'EF_task_success_rate', 'name': '任务成功率'}
    ]
  },
  'PO': {
    'name': '处理能力',
    'indicators': [
      {'code': 'PO_effective_throughput', 'name': '有效吞吐量'},
      {'code': 'PO_spectral_efficiency', 'name': '频谱效率'}
    ]
  },
  'NC': {
    'name': '组网能力',
    'indicators': [
      {'code': 'NC_avg_network_setup_duration_ms', 'name': '平均组网时长'},
      {'code': 'NC_avg_connectivity_rate', 'name': '平均连通率'}
    ]
  },
  'HO': {
    'name': '人为操作',
    'indicators': [
      {'code': 'HO_avg_operator_reaction_time_ms', 'name': '平均操作员反应时间'},
      {'code': 'HO_operation_success_rate', 'name': '操作成功率'}
    ]
  },
  'RS': {
    'name': '响应能力',
    'indicators': [
      {'code': 'RS_avg_call_setup_duration_ms', 'name': '平均呼叫建立时长'},
      {'code': 'RS_avg_transmission_delay_ms', 'name': '平均传输时延'}
    ]
  }
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
      :deep(.el-tabs--border-card) {
        border: 2px solid var(--navy-light);
        box-shadow: 0 2px 12px 0 rgba(0, 31, 63, 0.1);
      }

      :deep(.el-tabs__item) {
        font-size: 14px;
        padding: 0 20px;
      }

      :deep(.el-tabs__item.is-active) {
        background: linear-gradient(135deg, var(--navy-primary), var(--navy-secondary));
        color: white;
        font-weight: bold;
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
