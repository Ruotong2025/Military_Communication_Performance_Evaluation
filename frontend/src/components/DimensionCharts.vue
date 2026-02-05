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
              AHP权重: {{ (evaluationResult.dimensionWeights[dim.code]?.weight * 100 || 0).toFixed(2) }}%
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
                  <span>{{ formatIndicatorValue(indicator.indicatorCode, row.indicators[indicator.indicatorCode]) }}</span>
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
  if (!props.evaluationResult.indicatorWeights) return []
  
  const dimWeights = props.evaluationResult.indicatorWeights[dimCode]
  if (!dimWeights) return []
  
  return dimWeights.indicators.map(ind => ({
    indicatorCode: ind.code,
    indicatorName: ind.name,
    entropyWeight: ind.entropyWeight,
    finalWeight: ind.finalWeight
  }))
}

// 获取详细数据
const getDetailData = (dimCode) => {
  if (!props.evaluationResult.evaluationResults) return []
  
  return props.evaluationResult.evaluationResults.map(result => {
    return {
      testId: result.testId,
      score: result.dimensionScores[dimCode] || 0,
      indicators: result.indicatorRawValues?.[dimCode] || {}  // 使用原始值
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

// 格式化指标值
const formatIndicatorValue = (indicatorCode, value) => {
  if (value === null || value === undefined) return '-'
  
  // 百分比类型的指标（rate, ratio）
  if (indicatorCode.includes('rate') || indicatorCode.includes('ratio')) {
    return (value * 100).toFixed(2) + '%'
  }
  
  // 时间类型的指标（ms, duration）
  if (indicatorCode.includes('_ms') || indicatorCode.includes('duration') || indicatorCode.includes('time')) {
    return value.toFixed(2) + ' ms'
  }
  
  // 距离类型的指标
  if (indicatorCode.includes('distance')) {
    return value.toFixed(2) + ' km'
  }
  
  // 频率类型的指标
  if (indicatorCode.includes('frequency')) {
    return value.toFixed(4)
  }
  
  // 概率类型的指标
  if (indicatorCode.includes('probability')) {
    return (value * 100).toFixed(2) + '%'
  }
  
  // 吞吐量类型的指标
  if (indicatorCode.includes('throughput')) {
    return value.toFixed(2) + ' Mbps'
  }
  
  // 频谱效率
  if (indicatorCode.includes('efficiency')) {
    return value.toFixed(2) + ' bps/Hz'
  }
  
  // 信干噪比
  if (indicatorCode.includes('sinr')) {
    return value.toFixed(2) + ' dB'
  }
  
  // 抗干扰余量
  if (indicatorCode.includes('margin')) {
    return value.toFixed(2) + ' dB'
  }
  
  // 抗拦截能力（0-1之间的值）
  if (indicatorCode.includes('resistance')) {
    return value.toFixed(4)
  }
  
  // 默认保留2位小数
  return value.toFixed(2)
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
  
  // 初始化图表（使用 SVG 渲染器以避免中文乱码）
  if (!charts[dimCode]) {
    charts[dimCode] = echarts.init(chartEl, null, { renderer: 'svg' })
  }
  
  const chart = charts[dimCode]
  
  // 准备数据
  const testIds = props.evaluationResult.evaluationResults.map(r => r.testId)
  const dimScores = props.evaluationResult.evaluationResults.map(
    r => r.dimensionScores[dimCode] || 0
  )
  
  // 获取指标数据
  const indicators = getDimensionIndicators(dimCode)
  const series = []
  
  // 海军蓝色系
  const colors = ['#0074D9', '#7FDBFF', '#39CCCC', '#3D9970', '#2ECC40', '#01FF70', '#FFDC00', '#FF851B']
  
  // 为每个指标创建柱状图系列
  indicators.forEach((indicator, index) => {
    const data = props.evaluationResult.evaluationResults.map(result => {
      return result.indicatorScores[dimCode]?.[indicator.indicatorCode] || 0
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
        color: '#001f3f',
        fontFamily: 'Microsoft YaHei, SimHei, Arial, sans-serif'
      }
    })
  })
  
  // 添加维度得分折线
  series.push({
    name: '维度得分',
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
      formatter: (params) => params.value.toFixed(2),
      fontSize: 12,
      fontWeight: 'bold',
      color: '#FF4136',
      backgroundColor: '#FFD700',
      padding: [4, 8],
      borderRadius: 4,
      fontFamily: 'Microsoft YaHei, SimHei, Arial, sans-serif'
    },
    z: 10
  })
  
  // 获取 AHP 权重
  const ahpWeight = props.evaluationResult.dimensionWeights?.[dimCode]?.weight || 0
  
  const option = {
    title: {
      text: `${dimName} - 细粒度指标分析`,
      subtext: `AHP权重: ${(ahpWeight * 100).toFixed(2)}%`,
      left: 'center',
      textStyle: {
        color: '#001f3f',
        fontSize: 18,
        fontWeight: 'bold',
        fontFamily: 'Microsoft YaHei, SimHei, Arial, sans-serif'
      },
      subtextStyle: {
        color: '#0074D9',
        fontSize: 14,
        fontFamily: 'Microsoft YaHei, SimHei, Arial, sans-serif'
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
        color: '#fff',
        fontFamily: 'Microsoft YaHei, SimHei, Arial, sans-serif'
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
      data: [...indicators.map(i => i.indicatorName), '维度得分'],
      top: 50,
      left: 'center',
      textStyle: {
        color: '#001f3f',
        fontFamily: 'Microsoft YaHei, SimHei, Arial, sans-serif',
        fontSize: 11
      },
      type: 'scroll',
      pageIconSize: 12,
      pageTextStyle: {
        fontSize: 11
      }
    },
    grid: {
      left: '5%',
      right: '5%',
      bottom: '18%',
      top: 120,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: testIds,
      axisLabel: {
        rotate: 30,
        color: '#001f3f',
        fontSize: 11,
        fontFamily: 'Microsoft YaHei, SimHei, Arial, sans-serif',
        interval: 0,
        margin: 10
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
      nameTextStyle: {
        fontFamily: 'Microsoft YaHei, SimHei, Arial, sans-serif'
      },
      min: 0,
      max: 110,
      axisLabel: {
        color: '#001f3f',
        fontSize: 12,
        fontFamily: 'Microsoft YaHei, SimHei, Arial, sans-serif'
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
    series: series
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
      height: 500px;
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
