<template>
  <div class="dynamic-cost-effectiveness">
    <!-- 顶部导航 -->
    <el-card class="header-card">
      <div class="header-content">
        <div class="title-section">
          <el-button @click="$router.back()" link>
            <el-icon><ArrowLeft /></el-icon> 返回
          </el-button>
          <h2>动态效费分析</h2>
        </div>
        <div class="action-buttons">
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon> 重置
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 步骤条 -->
    <el-card class="steps-card">
      <el-steps :active="currentStep" finish-status="success" align-center>
        <el-step title="成本指标" description="配置成本指标" />
        <el-step title="模拟生成" description="生成模拟数据" />
        <el-step title="结果分析" description="查看分析结果" />
      </el-steps>
    </el-card>

    <!-- 步骤1: 成本指标配置 -->
    <el-card v-show="currentStep === 0" class="step-card">
      <template #header>
        <div class="card-header">
          <span>步骤1：成本指标配置</span>
          <div class="header-actions">
            <el-button type="success" @click="handleDownloadTemplate">
              <el-icon><Download /></el-icon>
              下载模板
            </el-button>
            <el-button type="primary" @click="handleUploadTemplate">
              <el-icon><Upload /></el-icon>
              上传模板
            </el-button>
          </div>
        </div>
      </template>

      <!-- 模板选择 -->
      <el-form inline class="template-select">
        <el-form-item label="选择模板">
          <el-select v-model="selectedTemplateId" placeholder="请选择模板" style="width: 300px" @change="handleTemplateChange">
            <el-option
              v-for="t in templateList.filter(t => t.id)"
              :key="t.id"
              :label="t.templateName"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="selectedTemplateId">
          <el-button type="danger" @click="handleDeleteTemplate">
            <el-icon><Delete /></el-icon> 删除模板
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 效费分析区域（选择成本模板后显示，在表头上方） -->
      <div v-if="currentTemplate && currentTemplate.costIndicators?.length > 0" class="effectiveness-analysis-section">
        <el-divider content-position="left">
          <el-icon><DataLine /></el-icon>
          效费分析模拟数据
        </el-divider>

        <el-alert type="info" :closable="false" show-icon>
          <template #title>
            选择 <strong>dynamic_qt_record</strong> 中的原数据模板和批次，基于成本指标配置进行效费分析
          </template>
        </el-alert>

        <el-form inline class="qt-source-select" style="margin-top: 20px">
          <el-form-item label="原数据模板">
            <el-select v-model="selectedQtTemplateId" placeholder="请选择原数据模板" style="width: 250px" @change="handleQtTemplateChange">
              <el-option
                v-for="t in qtTemplateList"
                :key="t.template_id"
                :label="t.template_name"
                :value="t.template_id"
              >
                <span>{{ t.template_name }}</span>
                <span style="float: right; color: #8492a6; font-size: 12px">批次: {{ t.batch_count }}</span>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item v-if="selectedQtTemplateId" label="批次选择">
            <el-select v-model="selectedQtBatchId" placeholder="请选择批次" style="width: 250px" @change="handleQtBatchChange">
              <el-option
                v-for="b in qtBatchList"
                :key="b.batch_id"
                :label="b.batch_id"
                :value="b.batch_id"
              >
                <span>{{ b.batch_id }}</span>
                <span style="float: right; color: #8492a6; font-size: 12px">作战: {{ b.operation_count }}</span>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="success" :disabled="!currentTemplate?.id || !selectedQtBatchId" @click="generateEffectivenessData">
              <el-icon><Cpu /></el-icon>
              模拟数据
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 模板详情 - 成本指标多级表头（带数据或不带数据） -->
      <div v-if="currentTemplate && currentTemplate.costIndicators?.length > 0" class="template-detail">
        <el-divider content-position="left">成本指标配置 - 多级表头</el-divider>

        <!-- 多级表头表格 -->
        <el-table :data="effectivenessTableData" border stripe max-height="400">
          <el-table-column prop="operationId" label="作战ID" width="120" />
          <template v-for="level1 in level1Groups" :key="level1.name">
            <el-table-column :label="level1.name" :width="level1.width">
              <template v-for="col in level1.columns" :key="col.prop">
                <el-table-column :prop="col.prop" :label="col.label" :width="col.width" align="center">
                  <template #header>
                    <div class="weight-header">
                      <span>{{ col.label }}</span>
                      <el-tag size="small" type="primary">{{ col.weight }}</el-tag>
                    </div>
                  </template>
                  <template #default="{ row }">
                    <span v-if="row && row[col.prop] !== null && row[col.prop] !== undefined">
                      {{ row[col.prop].toFixed(4) }}
                    </span>
                    <span v-else class="text-muted">-</span>
                  </template>
                </el-table-column>
              </template>
            </el-table-column>
          </template>
        </el-table>

        <div class="weight-summary">
          <span>权重合计：</span>
          <el-tag :type="Math.abs(totalWeight - 1) < 0.01 ? 'success' : 'danger'" size="large">
            {{ totalWeight.toFixed(4) }}
          </el-tag>
          <el-tag v-if="Math.abs(totalWeight - 1) > 0.01" type="warning" style="margin-left: 10px">
            权重之和应等于1
          </el-tag>
        </div>
      </div>

      <el-empty v-else description="请选择或上传模板" />

      <div class="step-footer">
        <el-button type="primary" :disabled="!currentTemplate || currentTemplate.costIndicators?.length === 0" :loading="generatingChart" @click="generateCostEffectivenessChart">
          <el-icon><DataAnalysis /></el-icon>
          生成效费分析
        </el-button>
      </div>
    </el-card>

    <!-- 效费分析柱状图 -->
    <el-card v-if="showChart" class="content-card chart-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><TrendCharts /></el-icon> 效费分析结果</span>
          <el-tag type="success">批次: {{ selectedQtBatchId }}</el-tag>
        </div>
      </template>

      <el-alert type="info" :closable="false" show-icon class="chart-hint-alert">
        下图展示每个作战的<strong>效能 E</strong>（综合评分）、<strong>成本 C</strong>（成本指标加权得分）、<strong>效费比 R</strong>（E/C），便于横向对比。
        左侧纵轴为效能 E；右侧纵轴为成本 C 与效费比 R（量纲不同，请勿用柱高直接比较 E 与 C、R）。
      </el-alert>

      <div class="chart-container" ref="costEffectivenessChartRef" />
    </el-card>

    <!-- 效费分析结果表格 -->
    <el-card v-if="showChart && costEffectivenessData.length > 0" class="content-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><List /></el-icon> 效费分析明细</span>
        </div>
      </template>

      <el-table :data="costEffectivenessData" border stripe size="small" max-height="400" class="result-table">
        <el-table-column prop="operationId" label="作战ID" width="120" fixed align="center">
          <template #default="{ row }">
            <el-tag type="primary" size="small">{{ row.operationId }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="效能 E（综合评分）" width="160" align="center">
          <template #default="{ row }">
            <span class="effect-score">{{ row.effectivenessScore?.toFixed(4) || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="成本 C（加权得分）" width="160" align="center">
          <template #default="{ row }">
            <span class="cost-score">{{ row.costScore?.toFixed(4) || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="效费比 R（E/C）" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <span class="ratio-value" :class="getRatioClass(row.ratio)">
              {{ row.ratio?.toFixed(4) || '—' }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 上传模板弹窗 -->
    <el-dialog v-model="showUploadDialog" title="上传模板" width="800px">
      <el-form :model="uploadForm" label-width="120px">
        <el-form-item label="模板名称" required>
          <el-input v-model="uploadForm.templateName" placeholder="请输入模板名称" style="width: 300px" />
        </el-form-item>
      </el-form>

      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 15px">
        <template #title>
          请上传包含"成本指标"Sheet的Excel文件
        </template>
      </el-alert>

      <el-upload
        ref="uploadRef"
        class="upload-area"
        drag
        :auto-upload="false"
        :on-change="handleFileChange"
        accept=".xlsx,.xls"
      >
        <el-icon><UploadFilled /></el-icon>
        <div class="el-upload__text">将Excel文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">支持.xlsx和.xls格式</div>
        </template>
      </el-upload>

      <!-- 预览上传数据 -->
      <div v-if="uploadPreview.length > 0" class="upload-preview">
        <el-divider content-position="left">数据预览（共 {{ uploadPreview.length }} 个指标）</el-divider>
        <el-table :data="uploadPreview" border size="small" max-height="250">
          <el-table-column type="index" label="#" width="60" />
          <el-table-column prop="level1Name" label="一级维度" width="150" />
          <el-table-column prop="level2Name" label="二级维度" width="180" />
          <el-table-column prop="weight" label="权重" width="80" />
          <el-table-column prop="minValue" label="最小值" width="80" />
          <el-table-column prop="maxValue" label="最大值" width="80" />
          <el-table-column prop="distributionType" label="分布" width="80" />
        </el-table>
      </div>

      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!uploadFile" @click="handleConfirmUpload">确认上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import * as XLSX from 'xlsx'
import {
  ArrowLeft, ArrowRight, Refresh, Delete, Cpu, DataLine, Setting, Download, Upload, UploadFilled,
  DataAnalysis, TrendCharts, List
} from '@element-plus/icons-vue'
import request from '@/utils/request'
import { getDynamicComprehensiveScores } from '@/api'

// 统一响应成功判断（响应拦截器已返回 res.data，所以这里检查数组）
const isSuccess = (res) => Array.isArray(res) || (res && typeof res === 'object')

const router = useRouter()

// ==================== 状态定义 ====================
const currentStep = ref(0)
const simulating = ref(false)
const simulationProgress = ref(0)

// 模板相关
const templateList = ref([])
const selectedTemplateId = ref(null)
const currentTemplate = ref(null)

// 批次相关
const batchList = ref([])
const selectedBatchId = ref(null)

// 模拟结果
const simulationResult = ref(null)
const simulationRecords = ref([])

// 模拟配置
const simulationConfig = ref({
  simulationCount: 1000,
  randomSeed: 123456,
})
const batchName = ref('')

// 上传相关
const showUploadDialog = ref(false)
const uploadFile = ref(null)
const uploadRef = ref(null)
const uploadPreview = ref([])
const uploadForm = ref({
  templateName: '',
})

// 效费分析相关
const qtTemplateList = ref([])
const selectedQtTemplateId = ref(null)
const qtBatchList = ref([])
const selectedQtBatchId = ref(null)
const qtOperationIds = ref([])
const effectivenessData = ref(null)

// 效费分析表格数据
const effectivenessTableData = ref([])
const histogramRef = ref(null)
const cdfRef = ref(null)

// 效费分析柱状图相关
const showChart = ref(false)
const generatingChart = ref(false)
const costEffectivenessChartRef = ref(null)
const costEffectivenessData = ref([])
let costEffectivenessChart = null

// ==================== 计算属性 ====================
const totalWeight = computed(() => {
  if (!currentTemplate.value?.costIndicators) return 0
  return currentTemplate.value.costIndicators.reduce((sum, ind) => sum + (ind.weight || 0), 0)
})

// 多级表头分组
const level1Groups = computed(() => {
  if (!currentTemplate.value?.costIndicators) return []
  const groups = []
  const level1Map = new Map()

  currentTemplate.value.costIndicators.forEach(ind => {
    if (!level1Map.has(ind.level1Name)) {
      level1Map.set(ind.level1Name, [])
    }
    level1Map.get(ind.level1Name).push(ind)
  })

  level1Map.forEach((items, level1Name) => {
    const columns = items.map(ind => ({
      prop: `col_${ind.indicatorCode}`,
      label: ind.level2Name,
      weight: (ind.weight * 100).toFixed(2) + '%',
    }))
    groups.push({
      name: level1Name,
      width: items.length * 120,
      columns,
    })
  })

  return groups
})

const distribution = computed(() => simulationResult.value?.distribution || null)

const cvValue = computed(() => {
  if (!distribution.value || distribution.value.mean === 0) return 0
  return (distribution.value.std / distribution.value.mean) * 100
})

const previewRecords = computed(() => {
  return simulationRecords.value.slice(0, 10)
})

const sensitivityData = computed(() => {
  if (!currentTemplate.value?.costIndicators) return []
  const weights = currentTemplate.value.costIndicators.map(ind => ({
    ...ind,
    contribution: (ind.weight || 0) * 100,
  }))
  return weights.sort((a, b) => b.contribution - a.contribution)
})

// ==================== 方法 ====================

// 加载模板列表
const loadTemplateList = async () => {
  try {
    const res = await request.get('/api/dynamic-cost/template/list')
    if (isSuccess(res)) {
      templateList.value = res || []
    }
  } catch (error) {
    console.error('加载模板列表失败:', error)
    templateList.value = []
  }
}

// 加载批次列表
const loadBatchList = async (templateId) => {
  if (!templateId) {
    batchList.value = []
    return
  }
  try {
    const res = await request.get('/api/dynamic-cost/batch/list', { params: { templateId } })
    if (isSuccess(res)) {
      batchList.value = res || []
    }
  } catch (error) {
    console.error('加载批次列表失败:', error)
    batchList.value = []
  }
}

// 模板选择变化
const handleTemplateChange = async (templateId) => {
  if (!templateId) {
    currentTemplate.value = null
    qtTemplateList.value = []
    return
  }
  try {
    const res = await request.get(`/api/dynamic-cost/template/${templateId}`)
    if (isSuccess(res)) {
      const template = res
      console.log('原始模板数据:', template)
      console.log('原始costConfig:', template.costConfig)
      template.costIndicators = JSON.parse(template.costConfig || '[]')
      console.log('解析后的costIndicators:', template.costIndicators)
      console.log('权重列表:', template.costIndicators.map(ind => ind.weight))
      console.log('权重合计:', template.costIndicators.reduce((sum, ind) => sum + (ind.weight || 0), 0))
      // 为每条记录设置列值（用于多级表头显示）
      template.costIndicators.forEach(ind => {
        ind[`col_${ind.indicatorCode}`] = '-'
      })
      currentTemplate.value = template
      await loadBatchList(templateId)
      // 清空效费分析相关状态
      selectedQtTemplateId.value = null
      selectedQtBatchId.value = null
      qtTemplateList.value = []
      qtBatchList.value = []
      effectivenessData.value = null
      effectivenessTableData.value = []
      // 加载原数据模板列表
      await loadQtTemplateList()
    }
  } catch (error) {
    console.error('加载模板详情失败:', error)
    ElMessage.error('加载模板详情失败')
  }
}

// ==================== 效费分析相关方法 ====================

// 加载原数据模板列表（来自dynamic_qt_record）
const loadQtTemplateList = async () => {
  try {
    const res = await request.get('/api/dynamic-cost/qt/template-list')
    if (isSuccess(res)) {
      qtTemplateList.value = res || []
    }
  } catch (error) {
    console.error('加载原数据模板列表失败:', error)
    qtTemplateList.value = []
  }
}

// 加载原数据批次列表
const loadQtBatchList = async (templateId) => {
  if (!templateId) {
    qtBatchList.value = []
    return
  }
  try {
    const res = await request.get('/api/dynamic-cost/qt/batch-list', { params: { templateId } })
    if (isSuccess(res)) {
      qtBatchList.value = res || []
    }
  } catch (error) {
    console.error('加载原数据批次列表失败:', error)
    qtBatchList.value = []
  }
}

// 原数据模板选择变化
const handleQtTemplateChange = async (templateId) => {
  selectedQtTemplateId.value = templateId
  selectedQtBatchId.value = null
  qtOperationIds.value = []
  effectivenessData.value = null
  effectivenessTableData.value = []
  if (templateId) {
    await loadQtBatchList(templateId)
  }
}

// 原数据批次选择变化
const handleQtBatchChange = async (batchId) => {
  selectedQtBatchId.value = batchId
  qtOperationIds.value = []
  effectivenessData.value = null
  effectivenessTableData.value = []
  if (batchId) {
    await loadQtOperationList(batchId)
  }
}

// 加载作战ID列表
const loadQtOperationList = async (batchId) => {
  try {
    const res = await request.get('/api/dynamic-cost/qt/operation-list', { params: { batchId } })
    if (isSuccess(res)) {
      qtOperationIds.value = res || []
    }
  } catch (error) {
    console.error('加载作战ID列表失败:', error)
    qtOperationIds.value = []
  }
}

// 生成效费分析模拟数据
const generateEffectivenessData = async () => {
  if (!currentTemplate.value?.id || !selectedQtBatchId.value) {
    ElMessage.warning('请先选择成本模板和批次')
    return
  }
  try {
    const res = await request.get('/api/dynamic-cost/qt/effectiveness-data', {
      params: {
        templateId: currentTemplate.value.id,  // 使用当前成本模板ID
        batchId: selectedQtBatchId.value
      }
    })
    if (isSuccess(res)) {
      effectivenessData.value = res
      buildEffectivenessTable(res)
      ElMessage.success('效费分析数据生成成功')
    }
  } catch (error) {
    console.error('生成效费分析数据失败:', error)
    ElMessage.error('生成效费分析数据失败')
  }
}

// 构建效费分析表格数据
const buildEffectivenessTable = (data) => {
  const operationIds = data.operationIds || []
  const costIndicators = data.costIndicators || []
  const normData = data.normalizedData || []

  console.log('=== 效费分析数据调试 ===')
  console.log('作战ID列表:', operationIds)
  console.log('成本指标列表:', costIndicators)
  console.log('归一化数据列表:', normData)

  // 构建作战ID+指标名称到归一化数据的映射（使用secondary_name匹配）
  const normMap = new Map()
  normData.forEach(item => {
    const key = `${item.operation_id}_${item.secondary_name}`
    normMap.set(key, item.normalized_value)
  })

  // 构建表格数据
  const rows = []
  for (const opId of operationIds) {
    const row = { operationId: opId }
    for (const ind of costIndicators) {
      // 前端 level1Groups 生成的 prop 格式是 col_indicatorCode
      const key = `${opId}_col_${ind.indicatorCode}`
      row[`col_${ind.indicatorCode}`] = normMap.get(key) ?? null
    }
    rows.push(row)
  }

  console.log('最终表格数据:', rows)
  effectivenessTableData.value = rows
}

// 批次选择变化
const handleBatchChange = async (batchId) => {
  if (!batchId) {
    simulationResult.value = null
    simulationRecords.value = []
    return
  }
  try {
    const res = await request.get(`/api/dynamic-cost/batch/${batchId}`)
    if (isSuccess(res)) {
      simulationResult.value = {
        batchId: res.batchId,
        batchCode: res.batchCode,
        costScore: res.costScore,
        effectivenessCostRatio: res.effectivenessCostRatio,
        distribution: res.distribution,
      }
      // 加载模拟详情
      loadSimulationDetails(batchId)
    }
  } catch (error) {
    console.error('加载批次详情失败:', error)
    ElMessage.error('加载批次详情失败')
  }
}

// 加载模拟详情
const loadSimulationDetails = async (batchId) => {
  try {
    const res = await request.get(`/api/dynamic-cost/batch/${batchId}`)
    if (isSuccess(res) && res.detailList) {
      // 将详情转换为预览格式
      const details = res.detailList || []
      const recordsMap = new Map()
      details.forEach(d => {
        if (!recordsMap.has(d.iterationNumber)) {
          recordsMap.set(d.iterationNumber, { [d.indicatorCode]: d.simulatedValue })
        } else {
          recordsMap.get(d.iterationNumber)[d.indicatorCode] = d.simulatedValue
        }
      })
      simulationRecords.value = Array.from(recordsMap.values()).slice(0, 100)
    }
  } catch (error) {
    console.error('加载模拟详情失败:', error)
  }
}

// 删除模板
const handleDeleteTemplate = async () => {
  if (!selectedTemplateId.value) return
  try {
    await ElMessageBox.confirm('确定要删除该模板吗？', '提示', { type: 'warning' })
    await request.delete(`/api/dynamic-cost/template/${selectedTemplateId.value}`)
    ElMessage.success('删除成功')
    selectedTemplateId.value = null
    currentTemplate.value = null
    await loadTemplateList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 下载模板
const handleDownloadTemplate = () => {
  const wb = XLSX.utils.book_new()

  // 成本指标模板
  const costData = [
    { 一级维度: '人力成本', 二级维度: '人员配置成本', 权重: 0.2, 最小值: 10, 最大值: 50, 分布类型: 'uniform' },
    { 一级维度: '', 二级维度: '培训成本', 权重: 0.15, 最小值: 5, 最大值: 30, 分布类型: 'uniform' },
    { 一级维度: '', 二级维度: '维修成本', 权重: 0.15, 最小值: 10, 最大值: 40, 分布类型: 'normal' },
    { 一级维度: '装备成本', 二级维度: '设备采购成本', 权重: 0.2, 最小值: 50, 最大值: 100, 分布类型: 'uniform' },
    { 一级维度: '', 二级维度: '折旧成本', 权重: 0.1, 最小值: 20, 最大值: 60, 分布类型: 'normal' },
    { 一级维度: '运行成本', 二级维度: '能耗成本', 权重: 0.1, 最小值: 15, 最大值: 45, 分布类型: 'uniform' },
    { 一级维度: '', 二级维度: '维护成本', 权重: 0.1, 最小值: 10, 最大值: 35, 分布类型: 'normal' },
  ]
  const ws1 = XLSX.utils.json_to_sheet(costData)
  ws1['!cols'] = [
    { wch: 15 }, { wch: 18 }, { wch: 10 }, { wch: 10 }, { wch: 10 }, { wch: 12 }
  ]
  XLSX.utils.book_append_sheet(wb, ws1, '成本指标')

  // 说明Sheet
  const instructionData = [
    { 说明: '1. Excel必须包含"成本指标"Sheet' },
    { 说明: '2. 一级维度列：相同维度只需填写第一个单元格（向下填充）' },
    { 说明: '3. 权重会自动归一化，无需手动计算' },
    { 说明: '4. 分布类型：uniform(均匀分布)、normal(正态分布)' },
    { 说明: '5. 上传后系统将自动计算加权成本分布' },
  ]
  const ws2 = XLSX.utils.json_to_sheet(instructionData)
  XLSX.utils.book_append_sheet(wb, ws2, '填写说明')

  const fileName = `成本指标模板_${new Date().toLocaleDateString().replace(/\//g, '-')}.xlsx`
  XLSX.writeFile(wb, fileName)
  ElMessage.success('模板下载成功')
}

// 上传模板
const handleUploadTemplate = () => {
  showUploadDialog.value = true
  uploadFile.value = null
  uploadPreview.value = []
  uploadForm.value.templateName = ''
}

// 文件变化处理
const handleFileChange = (file) => {
  uploadFile.value = file.raw
  parseExcelFile(file.raw)
}

// 解析Excel文件
const parseExcelFile = (file) => {
  const reader = new FileReader()
  reader.onload = (evt) => {
    try {
      const data = new Uint8Array(evt.target.result)
      const workbook = XLSX.read(data, { type: 'array' })

      uploadPreview.value = []

      // 解析成本指标Sheet
      if (workbook.SheetNames.includes('成本指标')) {
        const sheet = workbook.Sheets['成本指标']
        const jsonData = XLSX.utils.sheet_to_json(sheet, { header: 1, defval: '' })
        parseCostSheet(jsonData)
      } else {
        ElMessage.warning('未找到"成本指标"Sheet')
      }
    } catch (error) {
      console.error('解析Excel失败:', error)
      ElMessage.error('解析Excel文件失败')
    }
  }
  reader.readAsArrayBuffer(file)
}

// 解析成本Sheet
const parseCostSheet = (jsonData) => {
  if (jsonData.length === 0) return

  const headers = jsonData[0]
  const level1Index = headers.findIndex(h => h.includes('一级') || h === '维度1')
  const level2Index = headers.findIndex(h => h.includes('二级') || h.includes('指标'))
  const weightIndex = headers.findIndex(h => h.includes('权重'))
  const minIndex = headers.findIndex(h => h.includes('最小'))
  const maxIndex = headers.findIndex(h => h.includes('最大'))
  const distIndex = headers.findIndex(h => h.includes('分布'))

  let currentLevel1 = ''

  for (let i = 1; i < jsonData.length; i++) {
    const row = jsonData[i]
    if (!row || row.length === 0 || !row[level2Index]) continue

    const level1 = String(row[level1Index] || '').trim()
    if (level1) currentLevel1 = level1

    uploadPreview.value.push({
      level1Name: currentLevel1,
      level2Name: String(row[level2Index] || '').trim(),
      weight: parseFloat(row[weightIndex]) || 0,
      minValue: parseFloat(row[minIndex]) || 0,
      maxValue: parseFloat(row[maxIndex]) || 100,
      distributionType: String(row[distIndex] || 'uniform').toLowerCase(),
    })
  }
}

// 确认上传
const handleConfirmUpload = async () => {
  if (!uploadForm.value.templateName) {
    ElMessage.warning('请输入模板名称')
    return
  }
  if (uploadPreview.value.length === 0) {
    ElMessage.warning('请上传有效的模板文件')
    return
  }

  // 归一化权重
  const total = uploadPreview.value.reduce((sum, ind) => sum + ind.weight, 0)
  if (total > 0) {
    uploadPreview.value.forEach(ind => {
      ind.weight = parseFloat((ind.weight / total).toFixed(4))
    })
  }

  // 生成指标编码
  uploadPreview.value.forEach((ind, i) => {
    ind.indicatorCode = `cost_${ind.level1Name?.substring(0, 2) || 'x'}_${i + 1}`
  })

  try {
    const res = await request.post('/api/dynamic-cost/template/save', {
      templateName: uploadForm.value.templateName,
      costIndicators: uploadPreview.value,
    })
    if (isSuccess(res)) {
      showUploadDialog.value = false
      // 显示成功弹窗
      ElMessageBox.alert('模板上传成功！', '提示', {
        confirmButtonText: '确定',
        type: 'success',
        showClose: false,
      }).then(() => {
        // 刷新页面
        window.location.reload()
      })
    }
  } catch (error) {
    const msg = error?.response?.data?.message || error?.message || '上传失败'
    if (msg.includes('名称已存在')) {
      ElMessage.error('模板名称已存在，请使用其他名称')
    } else {
      ElMessage.error(msg)
    }
  }
}

// 开始模拟
const startSimulation = async () => {
  if (!currentTemplate.value) {
    ElMessage.warning('请先选择模板')
    return
  }

  simulating.value = true
  simulationProgress.value = 0

  try {
    const progressTimer = setInterval(() => {
      if (simulationProgress.value < 90) {
        simulationProgress.value += Math.random() * 15
      }
    }, 200)

    const res = await request.post('/api/dynamic-cost/simulate', {
      templateId: selectedTemplateId.value,
      batchName: batchName.value || undefined,
      simulationCount: simulationConfig.value.simulationCount,
      randomSeed: simulationConfig.value.randomSeed,
    })

    clearInterval(progressTimer)

    if (isSuccess(res)) {
      simulationResult.value = res
      simulationProgress.value = 100
      ElMessage.success('模拟完成')
      await loadBatchList(selectedTemplateId.value)
      selectedBatchId.value = res.batchId

      // 生成前端预览数据
      generatePreviewData()
    }
  } catch (error) {
    console.error('模拟失败:', error)
    ElMessage.error('模拟失败')
  } finally {
    simulating.value = false
  }
}

// 生成预览数据
const generatePreviewData = () => {
  if (!currentTemplate.value?.costIndicators) return

  const indicators = currentTemplate.value.costIndicators
  const count = Math.min(100, simulationConfig.value.simulationCount)
  const random = seededRandom(simulationConfig.value.randomSeed)

  simulationRecords.value = []
  for (let i = 0; i < count; i++) {
    const record = {}
    let totalCost = 0

    indicators.forEach(ind => {
      let value
      if (ind.distributionType === 'normal') {
        const mean = (ind.minValue + ind.maxValue) / 2
        const std = (ind.maxValue - ind.minValue) / 6
        value = mean + std * (random() * 2 - 1) * 3
        value = Math.max(ind.minValue, Math.min(ind.maxValue, value))
      } else {
        value = ind.minValue + random() * (ind.maxValue - ind.minValue)
      }
      record[ind.indicatorCode] = value
      totalCost += value * ind.weight
    })

    record.totalCost = totalCost
    simulationRecords.value.push(record)
  }
}

// 简单伪随机数生成器
const seededRandom = (seed) => {
  let s = seed
  return () => {
    s = Math.sin(s) * 10000
    return s - Math.floor(s)
  }
}

// 导出报告
const handleExportResults = () => {
  ElMessage.info('导出功能开发中')
}

// 步骤切换
const nextStep = () => {
  if (currentStep.value < 2) {
    currentStep.value++
    nextTick(() => {
      if (currentStep.value === 2) {
        renderCharts()
      }
    })
  }
}

const prevStep = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

// 生成效费分析柱状图
const generateCostEffectivenessChart = async () => {
  if (!currentTemplate.value?.id || !selectedQtBatchId.value) {
    ElMessage.warning('请先选择成本模板和批次')
    return
  }

  generatingChart.value = true
  showChart.value = false
  costEffectivenessData.value = []

  console.log('【前端】开始生成效费分析, templateId=', currentTemplate.value.id, ', batchId=', selectedQtBatchId.value)

  // 1. 获取效费分析数据（成本指标模拟值）
  let effectRes = null
  try {
    console.log('【前端】请求效费分析数据, URL=/api/dynamic-cost/qt/effectiveness-data')
    effectRes = await request.get('/api/dynamic-cost/qt/effectiveness-data', {
      params: {
        templateId: currentTemplate.value.id,
        batchId: selectedQtBatchId.value
      }
    })
    console.log('【前端】效费分析数据响应:', effectRes)
  } catch (error) {
    console.error('【前端】获取效费分析数据失败:', error)
    ElMessage.error('获取效费分析数据失败: ' + (error.message || error))
    generatingChart.value = false
    return
  }

  console.log('【前端】效费分析数据响应, effectRes=', JSON.stringify(effectRes))
  console.log('【前端】isSuccess检查, effectRes=', effectRes, ', typeof=', typeof effectRes, ', isSuccess=', isSuccess(effectRes))

  if (!isSuccess(effectRes)) {
    console.error('【前端】效费分析数据响应失败, isSuccess=false')
    ElMessage.error('获取效费分析数据失败')
    generatingChart.value = false
    return
  }

  console.log('【前端】isSuccess检查通过, 准备请求综合评分数据')

  // 2. 获取综合评分数据（从数据库）
  let scoreRes = null
  try {
    console.log('【前端】请求综合评分数据, batchId=', selectedQtBatchId.value)
    scoreRes = await getDynamicComprehensiveScores(selectedQtBatchId.value)
    console.log('【前端】综合评分数据响应成功, scoreRes=', scoreRes)
  } catch (error) {
    console.error('【前端】获取综合评分数据失败:', error)
    ElMessage.error('获取综合评分数据失败: ' + (error.message || error))
    generatingChart.value = false
    return
  }

  // 构建综合评分映射
  const scoreMap = new Map()
  if (Array.isArray(scoreRes) && scoreRes.length > 0) {
    scoreRes.forEach(item => {
      scoreMap.set(item.operationId, item.totalScore)
    })
  } else {
    console.warn('【前端】综合评分数据为空，请先在"评估结果计算"页面计算综合评分')
    ElMessage.warning('综合评分数据为空，请先在"评估结果计算"页面点击"生成综合得分"')
    generatingChart.value = false
    return
  }
  console.log('【前端】综合评分映射:', scoreMap)

  // 3. 计算每个作战的成本评分和效费比
  try {
    const operationIds = effectRes.operationIds || []
    const costIndicators = effectRes.costIndicators || []
    const normData = effectRes.normalizedData || []

    console.log('【前端】operationIds:', operationIds)
    console.log('【前端】costIndicators:', costIndicators)
    console.log('【前端】normData:', normData)

    // 构建归一化数据映射
    const normMap = new Map()
    normData.forEach(item => {
      normMap.set(`${item.operation_id}_${item.secondary_name}`, item.normalized_value)
    })

    // 计算成本评分、效能评分、效费比
    const chartData = []
    for (const opId of operationIds) {
      let costScore = 0
      for (const ind of costIndicators) {
        const key = `${opId}_col_${ind.indicatorCode}`
        const normVal = normMap.get(key) || 0
        costScore += normVal * (ind.weight || 0)
      }

      const effectivenessScore = scoreMap.get(opId) || 0
      const ratio = costScore > 0 ? effectivenessScore / costScore : 0

      // 评估等级
      let grade = '较差'
      if (ratio >= 1.5) grade = '优秀'
      else if (ratio >= 1.2) grade = '良好'
      else if (ratio >= 1.0) grade = '合格'

      chartData.push({
        operationId: opId,
        costScore,
        effectivenessScore,
        ratio,
        grade
      })
    }

    costEffectivenessData.value = chartData
    showChart.value = true

    ElMessage.success('效费分析数据生成成功')

    await nextTick()
    renderCostEffectivenessChart()
  } catch (calcError) {
    console.error('【前端】计算效费分析数据失败:', calcError)
    ElMessage.error('计算效费分析数据失败: ' + (calcError.message || calcError))
  } finally {
    generatingChart.value = false
  }
}

// 渲染效费分析柱状图
const renderCostEffectivenessChart = () => {
  if (!costEffectivenessChartRef.value || costEffectivenessData.value.length === 0) return

  if (!costEffectivenessChart) {
    costEffectivenessChart = echarts.init(costEffectivenessChartRef.value)
  }

  const data = [...costEffectivenessData.value].sort((a, b) => {
    const na = Number(a.operationId)
    const nb = Number(b.operationId)
    if (!Number.isNaN(na) && !Number.isNaN(nb)) return na - nb
    return String(a.operationId).localeCompare(String(b.operationId), 'zh-CN')
  })

  const labels = data.map(r => r.operationId)
  const seriesE = data.map(r => Number(r.effectivenessScore ?? 0))
  const seriesC = data.map(r => Number(r.costScore ?? 0))
  const seriesR = data.map(r => Number(r.ratio ?? 0))
  const batchId = selectedQtBatchId.value || ''

  costEffectivenessChart.setOption({
    title: {
      text: `批次 ${batchId}`,
      subtext: '每组三根柱：效能 E（左轴）｜成本 C、效费比 R（右轴）',
      left: 'center',
      top: 8,
      textStyle: { fontSize: 14 },
      subtextStyle: { fontSize: 11, color: '#909399' }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter(params) {
        if (!params?.length) return ''
        const name = params[0].axisValue
        const lines = [`<strong>${name}</strong>`, `批次：${batchId}`]
        for (const p of params) {
          const v = Number(p.value)
          const t = Number.isFinite(v) ? v.toFixed(4) : '—'
          lines.push(`${p.marker}${p.seriesName}：<strong>${t}</strong>`)
        }
        return lines.join('<br/>')
      }
    },
    legend: {
      data: ['效能 E', '成本 C', '效费比 R'],
      top: 56
    },
    grid: { left: '3%', right: '4%', top: 108, bottom: labels.length > 10 ? 64 : 40, containLabel: true },
    xAxis: {
      type: 'category',
      data: labels,
      axisLabel: {
        fontSize: 11,
        rotate: labels.length > 8 ? 30 : 0,
        interval: 0
      }
    },
    yAxis: [
      {
        type: 'value',
        name: '效能 E',
        position: 'left',
        alignTicks: true,
        axisLabel: { formatter: v => Number(v).toFixed(2) },
        splitLine: { lineStyle: { type: 'dashed' } }
      },
      {
        type: 'value',
        name: '成本 C / 效费比 R',
        position: 'right',
        alignTicks: true,
        axisLabel: { formatter: v => Number(v).toFixed(2) },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '效能 E',
        type: 'bar',
        yAxisIndex: 0,
        data: seriesE,
        itemStyle: { color: '#67C23A' },
        barMaxWidth: 22,
        label: { show: labels.length <= 12, position: 'top', formatter: p => Number(p.value).toFixed(2), fontSize: 9 }
      },
      {
        name: '成本 C',
        type: 'bar',
        yAxisIndex: 1,
        data: seriesC,
        itemStyle: { color: '#409EFF' },
        barMaxWidth: 22,
        label: { show: labels.length <= 12, position: 'top', formatter: p => Number(p.value).toFixed(2), fontSize: 9 }
      },
      {
        name: '效费比 R',
        type: 'bar',
        yAxisIndex: 1,
        data: seriesR,
        itemStyle: { color: '#E6A23C' },
        barMaxWidth: 22,
        label: { show: labels.length <= 12, position: 'top', formatter: p => Number(p.value).toFixed(2), fontSize: 9 }
      }
    ]
  }, true)

  costEffectivenessChart.resize()
}

// 效费比样式
const getRatioClass = (ratio) => {
  if (ratio == null) return ''
  if (ratio >= 1.5) return 'ratio-excellent'
  if (ratio >= 1.2) return 'ratio-good'
  if (ratio >= 1.0) return 'ratio-pass'
  return 'ratio-poor'
}

// 效费等级标签类型
const getGradeType = (grade) => {
  const types = {
    '优秀': 'success',
    '良好': '',
    '合格': 'warning',
    '较差': 'danger',
    '差': 'danger'
  }
  return types[grade] || 'info'
}

// 重置
const handleReset = () => {
  currentStep.value = 0
  selectedTemplateId.value = null
  currentTemplate.value = null
  selectedBatchId.value = null
  batchName.value = ''
  simulationResult.value = null
  simulationRecords.value = []
  simulationProgress.value = 0
}

// 进度条颜色
const getProgressColor = (percentage) => {
  if (percentage > 30) return '#F56C6C'
  if (percentage > 15) return '#E6A23C'
  return '#67C23A'
}

// ==================== 图表渲染 ====================
const renderCharts = () => {
  renderHistogram()
  renderCdf()
}

const renderHistogram = () => {
  if (!histogramRef.value || !distribution.value) return

  const chart = echarts.init(histogramRef.value)
  const stats = distribution.value
  const binCount = 20
  const binWidth = (stats.max - stats.min) / binCount

  const bins = []
  for (let i = 0; i < binCount; i++) {
    const start = stats.min + i * binWidth
    const end = stats.min + (i + 1) * binWidth
    const mid = (start + end) / 2
    const z = stats.std > 0 ? (mid - stats.mean) / stats.std : 0
    const density = Math.exp(-0.5 * z * z) / (stats.std * Math.sqrt(2 * Math.PI))
    bins.push({
      name: `${start.toFixed(1)}-${end.toFixed(1)}`,
      value: Math.max(0, Math.round(density * binWidth * simulationConfig.value.simulationCount)),
    })
  }

  const option = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'category', data: bins.map(b => b.name), axisLabel: { rotate: 45, fontSize: 10 } },
    yAxis: { type: 'value', name: '频次' },
    series: [{
      type: 'bar',
      data: bins.map(b => b.value),
      itemStyle: { color: '#409EFF' },
      barWidth: '80%',
    }],
  }

  chart.setOption(option)
}

const renderCdf = () => {
  if (!cdfRef.value || !distribution.value) return

  const chart = echarts.init(cdfRef.value)
  const stats = distribution.value

  const data = []
  for (let i = 0; i <= 100; i++) {
    const z = stats.mean + stats.std * (i - 50) / 10
    const x = (z - stats.mean) / (stats.std || 1)
    const cdf = 0.5 * (1 + erf(x / Math.sqrt(2)))
    data.push([z, Math.max(0, Math.min(1, cdf))])
  }

  const option = {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'value', name: '成本值' },
    yAxis: { type: 'value', name: '累积概率', max: 1 },
    series: [{
      type: 'line',
      data: data,
      smooth: true,
      areaStyle: { color: 'rgba(103, 194, 58, 0.3)' },
      lineStyle: { color: '#67C23A', width: 2 },
      showSymbol: false,
    }],
  }

  chart.setOption(option)
}

// 误差函数近似
const erf = (x) => {
  const a1 = 0.254829592, a2 = -0.284496736, a3 = 1.421413741
  const a4 = -1.453152027, a5 = 1.061405429, p = 0.3275911
  const sign = x < 0 ? -1 : 1
  x = Math.abs(x)
  const t = 1 / (1 + p * x)
  const y = 1 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * Math.exp(-x * x)
  return sign * y
}

const handleResize = () => {
  ;[histogramRef, cdfRef].forEach(ref => {
    if (ref.value) {
      echarts.getInstanceByDom(ref.value)?.resize()
    }
  })
}

onMounted(() => {
  loadTemplateList()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  ;[histogramRef, cdfRef].forEach(ref => {
    if (ref.value) {
      echarts.getInstanceByDom(ref.value)?.dispose()
    }
  })
})
</script>

<style scoped>
.dynamic-cost-effectiveness {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 40px);
}

.header-card { margin-bottom: 20px; }

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 15px;
}

.title-section h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.action-buttons { display: flex; gap: 10px; }

.steps-card { margin-bottom: 20px; }

.step-card { margin-bottom: 20px; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions { display: flex; gap: 10px; }

.template-select { margin-bottom: 20px; }

.template-detail { margin-top: 20px; }

.weight-summary {
  margin-top: 15px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.template-info { margin-bottom: 20px; }

.simulation-config { margin: 20px 0; }

.progress-section {
  margin: 20px 0;
  text-align: center;
}

.progress-text {
  display: block;
  margin-top: 10px;
  color: #606266;
}

.simulation-result { margin-top: 20px; }

.result-cards { margin-top: 20px; }

.cost-value { color: #e6a23c; font-weight: 600; }

.stat-row { margin-bottom: 20px; }

.chart-section { margin-bottom: 20px; }

.chart-container {
  background: #fff;
  border-radius: 8px;
  padding: 15px;
}

.chart-container h4 {
  margin: 0 0 15px 0;
  color: #303133;
}

.step-footer {
  margin-top: 30px;
  display: flex;
  justify-content: flex-end;
  gap: 15px;
}

.upload-area {
  margin: 20px 0;
  text-align: center;
}

.chart-hint-alert {
  margin-bottom: 12px;
}

.chart-container {
  width: 100%;
  height: 460px;
}

.result-table {
  .effect-score {
    color: #67C23A;
    font-weight: 500;
  }
  .cost-score {
    color: #409EFF;
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

.upload-preview { margin-top: 20px; }

:deep(.el-step__description) { font-size: 12px; }

.weight-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
</style>
