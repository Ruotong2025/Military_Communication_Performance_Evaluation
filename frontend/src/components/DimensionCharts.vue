<template>
  <div class="dimension-charts">
    <el-card class="charts-card">
      <template #header>
        <div class="card-header">
          <el-icon class="header-icon"><TrendCharts /></el-icon>
          <span>8维度细粒度指标分析</span>
        </div>
      </template>

      <!-- 每个维度一个图表 -->
      <div
        v-for="(dim, index) in dimensions"
        :key="dim.code"
        class="dimension-section"
      >
        <div class="dimension-header">
          <h3 class="dimension-title">
            【{{ dim.name }}】
            <el-tag type="primary" size="large" effect="dark">
              AHP权重: {{ (ahpWeights[dim.code] * 100).toFixed(2) }}%
            </el-tag>
          </h3>
        </div>

        <div :ref="el => chartRefs[dim.code] = el" class="chart-container"></div>

        <!-- 详细数据表格 -->
        <el-collapse class="detail-collapse">
          <el-collapse-item :title="`查看${dim.name}详细数据`">
            <el-table
              :data="getDetailData(dim.code)"
              border
              size="small"
              max-height="400"
            >
              <el-table-column prop="testId" label="测试批次" width="150" fixed />
              <el-table-column label="维度得分" width="120" fixed>
                <template #default="{ row }">
                  <el-tag :type="getScoreType(row.score)" size="large">
                    {{ row.score.toFixed(2) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column
                v-for="indicator in getDimensionIndicators(dim.code)"
                :key="indicator.indicatorCode"
                :label="indicator.indicatorName"
                width="150"
              >
                <template #default="{ row }">
                  <span>{{ row.indicators[indicator.indicatorCode]?.toFixed(1) || '-' }}</span>
                </template>
              </el-table-column>
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { TrendCharts } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const props = defineProps({
  evaluationResult: {
    type: Object,
    required: true
  },
  ahpWeights: {
    type: Object,
    required: true
  }
})

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

const chartRefs = ref({})
const charts = {}

// 获取维度的指标列表
const getDimensionIndicators = (dimCode) => {
  const firstBatch = props.evaluationResult.testBatchScores[0]
  if (!firstBatch) return []
  
  const dimScore = firstBatch.dimensionScores[dimCode]
  if (!dimScore || !dimScore.indicators) return []
  
  return Object.values(dimScore.indicators)
}

// 获取详细数据
const getDetailData = (dimCode) => {
  return props.evaluationResult.testBatchScores.map(batch => {
    const dimScore = batch.dimensionScores[dimCode]
    const indicators = {}
    
    if (dimScore && dimScore.indicators) {
      Object.entries(dimScore.indicators).forEach(([key, value]) => {
        indicators[key] = value.normalizedValue
      })
    }
    
    return {
      testId: batch.testId,
      score: dimScore?.score || 0,
      indicators
    }
  })
}

// 获取得分类型
const getScoreType = (score) => {
  if (score >= 90) return 'success'
  if (score >= 80) return 'primary'
  if (score >= 70) return 'warning'
  if (score >= 60) return 'info'
  return 'danger'
}

// 渲染所有图表
const renderAllCharts = () => {
  dimensions.forEach(dim => {
    renderDimensionChart(dim.code, dim.name)
  })
}

// 渲染单个维度图表
const renderDimensionChart = (dimCode, dimName) => {
  const chartEl = chartRefs.value[dimCode]
  if (!chartEl) return
  
  // 初始化图表
  if (!charts[dimCode]) {
    charts[dimCode] = echarts.init(chartEl)
  }
  
  const chart = charts[dimCode]
  
  // 准备数据
  const testIds = props.evaluationResult.testBatchScores.map(b => b.testId)
  const dimScores = props.evaluationResult.testBatchScores.map(
    b => b.dimensionScores[dimCode]?.score || 0
  )
  
  // 获取指标数据
  const indicators = getDimensionIndicators(dimCode)
  const series = []
  
  // 海军蓝色系
  const colors = ['#0074D9', '#7FDBFF', '#39CCCC', '#3D9970', '#2ECC40', '#01FF70', '#FFDC00', '#FF851B']
  
  // 为每个指标创建柱状图系列
  indicators.forEach((indicator, index) => {
    const data = props.evaluationResult.testBatchScores.map(batch => {
      const dimScore = batch.dimensionScores[dimCode]
      return dimScore?.indicators[indicator.indicatorCode]?.normalizedValue || 0
    })
    
    series.push({
      name: indicator.indicatorName,
      type: 'bar',
      data: data,
      itemStyle: {
        color: colors[index % colors.length],
        borderColor: '#001f3f',
        borderWidth: 1
      },
      label: {
        show: true,
        position: 'top',
        formatter: (params) => params.value > 5 ? params.value.toFixed(0) : '',
        fontSize: 10,
        color: '#001f3f'
      }
    })
  })
  
  // 添加维度得分折线
  series.push({
    name: '算法得分',
    type: 'line',
    data: dimScores,
    yAxisIndex: 0,
    lineStyle: {
      color: '#FF4136',
      width: 3
    },
    itemStyle: {
      color: '#FF4136',
      borderWidth: 2,
      borderColor: '#fff'
    },
    symbol: 'circle',
    symbolSize: 10,
    label: {
      show: true,
      position: 'top',
      formatter: '{c}',
      fontSize: 12,
      fontWeight: 'bold',
      color: '#FF4136',
      backgroundColor: '#FFD700',
      padding: [4, 8],
      borderRadius: 4
    },
    z: 10
  })
  
  const option = {
    title: {
      text: `${dimName} - 细粒度指标分析`,
      subtext: `AHP权重: ${(props.ahpWeights[dimCode] * 100).toFixed(2)}%`,
      left: 'center',
      textStyle: {
        color: '#001f3f',
        fontSize: 18,
        fontWeight: 'bold'
      },
      subtextStyle: {
        color: '#0074D9',
        fontSize: 14
      }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      backgroundColor: 'rgba(0, 31, 63, 0.9)',
      borderColor: '#0074D9',
      borderWidth: 2,
      textStyle: {
        color: '#fff'
      },
      formatter: (params) => {
        let result = `<div style="font-weight:bold;margin-bottom:5px;">${params[0].axisValue}</div>`
        params.forEach(param => {
          result += `<div>${param.marker} ${param.seriesName}: ${param.value.toFixed(2)}</div>`
        })
        return result
      }
    },
    legend: {
      data: [...indicators.map(i => i.indicatorName), '算法得分'],
      top: 40,
      textStyle: {
        color: '#001f3f'
      },
      type: 'scroll'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: 100,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: testIds,
      axisLabel: {
        rotate: 45,
        color: '#001f3f',
        fontSize: 12
      },
      axisLine: {
        lineStyle: {
          color: '#001f3f',
          width: 2
        }
      }
    },
    yAxis: {
      type: 'value',
      name: '归一化得分 (0-100)',
      min: 0,
      max: 110,
      axisLabel: {
        color: '#001f3f',
        fontSize: 12
      },
      axisLine: {
        show: true,
        lineStyle: {
          color: '#001f3f',
          width: 2
        }
      },
      splitLine: {
        lineStyle: {
          color: '#e6f7ff',
          type: 'dashed'
        }
      }
    },
    series: series,
    // 添加参考线
    markLine: {
      silent: true,
      symbol: 'none',
      data: [
        {
          yAxis: 90,
          lineStyle: {
            color: '#2ECC40',
            type: 'dashed',
            width: 2
          },
          label: {
            formatter: '优秀线(90)',
            position: 'end',
            color: '#2ECC40',
            fontSize: 12,
            fontWeight: 'bold'
          }
        },
        {
          yAxis: 60,
          lineStyle: {
            color: '#FF851B',
            type: 'dashed',
            width: 2
          },
          label: {
            formatter: '及格线(60)',
            position: 'end',
            color: '#FF851B',
            fontSize: 12,
            fontWeight: 'bold'
          }
        }
      ]
    }
  }
  
  chart.setOption(option)
  
  // 响应式
  window.addEventListener('resize', () => {
    chart.resize()
  })
}

// 监听数据变化
watch(() => props.evaluationResult, () => {
  nextTick(() => {
    renderAllCharts()
  })
}, { deep: true, immediate: true })

onMounted(() => {
  nextTick(() => {
    renderAllCharts()
  })
})
</script>

<style scoped lang="scss">
.dimension-charts {
  .charts-card {
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

  .dimension-section {
    margin-bottom: 40px;
    padding: 20px;
    background: linear-gradient(135deg, rgba(0, 116, 217, 0.05), rgba(127, 219, 255, 0.05));
    border-radius: 8px;
    border: 2px solid var(--navy-light);

    &:last-child {
      margin-bottom: 0;
    }

    .dimension-header {
      margin-bottom: 20px;

      .dimension-title {
        display: flex;
        align-items: center;
        gap: 15px;
        color: var(--navy-primary);
        font-size: 20px;
        font-weight: bold;
        padding-bottom: 15px;
        border-bottom: 3px solid var(--navy-secondary);
      }
    }

    .chart-container {
      height: 450px;
      margin-bottom: 20px;
    }

    .detail-collapse {
      margin-top: 20px;
      
      :deep(.el-collapse-item__header) {
        background: var(--navy-primary);
        color: white;
        font-weight: bold;
        padding: 0 15px;
        border-radius: 4px;
      }
    }
  }
}
</style>
