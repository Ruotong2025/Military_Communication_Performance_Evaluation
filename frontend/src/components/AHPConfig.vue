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
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { ref, reactive, watch, nextTick } from 'vue'
import { Setting, Edit, Promotion, RefreshLeft, Grid, PieChart } from '@element-plus/icons-vue'
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
const matrixTableData = ref([])
const weightChartRef = ref(null)
let weightChart = null

// 计算AHP
const calculateAHP = async () => {
  calculating.value = true
  try {
    // 1. 计算AHP权重（仅用于前端预览）
    ahpResult.value = await calculateAHPApi(priorities)
    
    // 2. 生成矩阵表格数据
    generateMatrixTableData()
    
    // 3. 绘制权重饼图
    await nextTick()
    renderWeightChart()
    
    // 4. 调用 Python 评估服务计算综合评估
    const pythonResult = await calculatePythonEvaluation(priorities)
    
    // 5. 通知父组件
    emit('weights-calculated', {
      weights: ahpResult.value.weights,
      pythonResult: pythonResult
    })
    
    ElMessage.success('AHP权重计算成功！')
  } catch (error) {
    ElMessage.error('计算失败：' + error.message)
    console.error(error)
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
    .consistency-section {
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
  }
}
</style>
