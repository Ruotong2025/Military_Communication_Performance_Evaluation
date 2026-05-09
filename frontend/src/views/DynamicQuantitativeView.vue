<template>
  <div class="dynamic-quantitative-view">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>
        <el-icon><Grid /></el-icon>
        动态定量评估
      </h2>
      <p class="subtitle">根据指标模板动态生成评估表格，支持模拟数据生成与归一化</p>
    </div>

    <!-- 工具栏 -->
    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-select
            v-model="selectedTemplateId"
            placeholder="请选择指标模板"
            style="width: 240px"
          >
            <el-option
              v-for="tpl in templates"
              :key="tpl.id"
              :label="tpl.templateName"
              :value="tpl.id"
            >
              <span>{{ tpl.templateName }}</span>
              <span class="template-stats">{{ tpl.secondaryCount || 0 }}指标</span>
            </el-option>
          </el-select>

          <el-button
            type="primary"
            :icon="Refresh"
            :loading="rendering"
            :disabled="!selectedTemplateId"
            @click="handleRender"
          >
            渲染
          </el-button>
        </div>

        <div class="toolbar-right" v-if="hasRendered">
          <el-select
            v-model="selectedBatchId"
            placeholder="选择已有批次"
            style="width: 260px"
            clearable
            @change="handleBatchChange"
          >
            <el-option
              v-for="batch in batches"
              :key="batch.batch_id"
              :label="batch.batch_id"
              :value="batch.batch_id"
            >
              <div class="batch-option">
                <span class="batch-id">{{ batch.batch_id }}</span>
                <span class="batch-count">{{ batch.operationCount }}作战</span>
              </div>
            </el-option>
          </el-select>

          <el-button
            type="warning"
            :icon="Plus"
            @click="handleNewBatchAndSimulate"
          >
            新建并模拟
          </el-button>

          <el-button
            v-if="currentBatchId"
            type="danger"
            plain
            :icon="Delete"
            @click="handleDeleteBatch"
          >
            删除批次
          </el-button>

          <el-tag v-if="currentBatchId" type="success" size="large">
            当前: {{ currentBatchId }}
          </el-tag>
        </div>
      </div>
    </el-card>

    <!-- 动态内容区域 -->
    <el-card v-if="hasRendered && levels.length > 0" class="table-card" shadow="never">
      <template #header>
        <div class="table-header">
          <div class="header-left">
            <el-icon><Document /></el-icon>
            <span>评估数据表</span>
            <el-tag v-if="operationCount > 0" type="info" size="small">{{ operationCount }} 条作战数据</el-tag>
          </div>
          <div class="header-actions">
            <el-button
              v-if="currentBatchId"
              type="warning"
              :icon="MagicStick"
              @click="openSimulateDialog"
            >
              全局模拟
            </el-button>
          </div>
        </div>
      </template>

      <!-- 按层级分 Tab -->
      <el-tabs v-model="activeLevel" type="border-card" class="level-tabs" @tab-change="handleTabChange">
        <el-tab-pane
          v-for="level in filteredLevels"
          :key="level.levelName"
          :label="level.levelName"
          :name="level.levelName"
        >
          <!-- 多级表头表格 -->
          <el-table
            :data="tableData"
            border
            stripe
            max-height="500"
            size="small"
            :span-method="mergePrimaryCells"
          >
            <!-- 作战ID列 -->
            <el-table-column
              prop="operationId"
              label="作战ID"
              width="120"
              fixed="left"
              align="center"
              header-align="center"
              class-name="operation-column"
            >
              <template #default="{ row }">
                <el-tag type="primary" size="small">{{ row.operationId }}</el-tag>
              </template>
            </el-table-column>

            <!-- 动态多级表头：一级维度 + 二级指标 -->
            <template v-for="(primary, pIdx) in level.primaries" :key="'primary-' + pIdx">
              <el-table-column
                :label="primary.primaryName"
                align="center"
                header-align="center"
                :colspan="primary.secondaries.length"
              >
                <el-table-column
                  v-for="(sec, sIdx) in primary.secondaries"
                  :key="'sec-' + pIdx + '-' + sIdx"
                  :label="sec.name"
                  align="center"
                  header-align="center"
                  min-width="120"
                >
                  <template #header>
                    <div class="sec-header">
                      <span>{{ sec.name }}</span>
                      <div class="header-tags">
                        <el-tag
                          size="small"
                          :type="sec.direction === 'POSITIVE' ? 'success' : 'warning'"
                        >
                          {{ sec.direction === 'POSITIVE' ? '正向' : '反向' }}
                        </el-tag>
                        <el-tag
                          size="small"
                          :type="sec.metricType === 'QUALITATIVE' ? 'info' : 'primary'"
                        >
                          {{ sec.metricType === 'QUALITATIVE' ? '定性' : '定量' }}
                        </el-tag>
                      </div>
                    </div>
                  </template>

                  <template #default="{ row }">
                    <div
                      v-if="sec.metricType === 'QUALITATIVE'"
                      class="cell-value"
                      :class="{
                        'has-value': row.levels && row.levels[sec.code]
                      }"
                      @click="handleQualitativeClick(row, sec)"
                    >
                      <template v-if="row.levels && row.levels[sec.code]">
                        <el-tag
                          size="small"
                          :type="getQualitativeTagType(row.levels[sec.code])"
                        >
                          {{ formatQualitativeValue(row.levels[sec.code]) }}
                        </el-tag>
                      </template>
                      <template v-else>
                        <span class="empty-value">—</span>
                      </template>
                    </div>
                    <div
                      v-else
                      class="cell-value"
                      :class="{
                        'has-value': row.values && row.values[sec.code] !== undefined && row.values[sec.code] !== null
                      }"
                      @click="handleCellClick(row, sec)"
                    >
                      <template v-if="row.values && row.values[sec.code] !== undefined && row.values[sec.code] !== null">
                        <span class="raw-value">{{ formatValue(row.values[sec.code]) }}</span>
                      </template>
                      <template v-else>
                        <span class="empty-value">—</span>
                      </template>
                    </div>
                  </template>
                </el-table-column>
              </el-table-column>
            </template>
          </el-table>

          <!-- 空状态提示 -->
          <div v-if="tableData.length === 0 && currentBatchId" class="empty-table-tip">
            <el-empty description="暂无数据，点击上方「全局模拟」生成数据" :image-size="80" />
          </div>
          <div v-if="!currentBatchId" class="empty-table-tip">
            <el-empty description="请选择已有批次或新建批次并模拟数据" :image-size="80" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 归一化结果区域 -->
    <el-card v-if="hasRendered && levels.length > 0" class="normalize-card" shadow="never">
      <template #header>
        <div class="table-header">
          <div class="header-left">
            <el-icon><DataLine /></el-icon>
            <span>归一化结果</span>
            <el-tag v-if="normalizationName" type="success" size="small">{{ normalizationName }}</el-tag>
          </div>
          <div class="header-actions">
            <el-button
              v-if="normalizationName && currentBatchId"
              type="danger"
              plain
              size="small"
              :icon="Delete"
              @click="handleDeleteNormalization"
            >
              删除
            </el-button>
            <el-button
              type="success"
              :icon="VideoPlay"
              :loading="normalizing"
              :disabled="!currentBatchId || operationCount === 0"
              @click="handleNormalize"
            >
              执行归一化
            </el-button>
          </div>
        </div>
      </template>

      <!-- 归一化结果表格 -->
      <div v-if="normalizedData.length > 0" class="normalized-table-section">
        <div class="table-section-title">
          <el-icon><DataLine /></el-icon>
          <span>归一化结果</span>
        </div>
        <el-table
          :data="normalizedData"
          border
          stripe
          max-height="500"
          size="small"
          :span-method="mergePrimaryCells"
        >
          <el-table-column
            prop="operationId"
            label="作战ID"
            width="120"
            fixed="left"
            align="center"
            header-align="center"
          >
            <template #default="{ row }">
              <el-tag type="primary" size="small">{{ row.operationId }}</el-tag>
            </template>
          </el-table-column>

          <template v-for="level in filteredLevels" :key="'norm-all-' + level.levelName">
            <template v-for="(primary, pIdx) in level.primaries" :key="'norm-primary-' + pIdx">
              <el-table-column
                :label="primary.primaryName"
                align="center"
                header-align="center"
                :colspan="primary.secondaries.length"
              >
                <el-table-column
                  v-for="(sec, sIdx) in primary.secondaries"
                  :key="'norm-sec-' + pIdx + '-' + sIdx"
                  :label="sec.name"
                  align="center"
                  header-align="center"
                  min-width="120"
                >
                  <template #header>
                    <div class="sec-header">
                      <span>{{ sec.name }}</span>
                      <div class="header-tags">
                        <el-tag
                          size="small"
                          :type="sec.direction === 'POSITIVE' ? 'success' : 'warning'"
                        >
                          {{ sec.direction === 'POSITIVE' ? '正向' : '反向' }}
                        </el-tag>
                      </div>
                    </div>
                  </template>
                  <template #default="{ row }">
                    <span :class="getNormalizedValueClass(row.values[sec.code])">
                      {{ formatNormalizedValue(row.values[sec.code]) }}
                    </span>
                  </template>
                </el-table-column>
              </el-table-column>
            </template>
          </template>
        </el-table>

        <!-- 综合评分说明 -->
        <div class="comprehensive-score">
          <span class="score-label">综合评分说明：</span>
          <span class="score-desc">
            综合评分 = 各维度归一化得分的加权求和，数值越大表示该作战综合表现越好
          </span>
        </div>

        <!-- 归一化数据可视化图表 -->
        <div class="chart-section">
          <div class="chart-header">
            <div class="chart-title">
              <el-icon><TrendCharts /></el-icon>
              <span>归一化数据可视化</span>
            </div>
            <div class="chart-controls">
              <el-radio-group v-model="chartType" size="small">
                <el-radio-button label="bar">柱状图</el-radio-button>
                <el-radio-button label="line">折线图</el-radio-button>
              </el-radio-group>
            </div>
          </div>

          <el-tabs type="border-card" class="chart-level-tabs" v-if="filteredLevels.length > 0" v-model="activeChartTab">
            <el-tab-pane
              v-for="level in filteredLevels"
              :key="'chart-' + level.levelName"
              :label="level.levelName"
              :name="level.levelName"
            >
              <div :id="'chart-' + level.levelName" class="level-chart-container"></div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>

      <el-empty v-else description="暂无归一化数据，请先选择批次并执行归一化">
        <template #image>
          <el-icon :size="60" style="color: #c0c4cc"><DataLine /></el-icon>
        </template>
      </el-empty>
    </el-card>

    <!-- 空状态 -->
    <el-empty
      v-if="!hasRendered"
      description="请先选择指标模板，然后点击「渲染」按钮"
    >
      <template #image>
        <el-icon :size="80" style="color: #c0c4cc"><Grid /></el-icon>
      </template>
    </el-empty>

    <!-- 定性指标等级选择对话框 -->
    <el-dialog v-model="showQualitativeDialog" title="选择定性等级" width="400px">
      <el-form>
        <el-form-item label="指标名称">
          <span>{{ currentQualitative?.sec?.name }}</span>
        </el-form-item>
        <el-form-item label="作战ID">
          <el-tag type="primary">{{ currentQualitative?.row?.operationId }}</el-tag>
        </el-form-item>
        <el-form-item label="请选择等级">
          <el-radio-group v-model="selectedQualitativeLevel">
            <el-radio label="EXCELLENT">优秀</el-radio>
            <el-radio label="GOOD">良好</el-radio>
            <el-radio label="FAIR">一般</el-radio>
            <el-radio label="POOR">差</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showQualitativeDialog = false">取消</el-button>
        <el-button type="primary" @click="handleQualitativeSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 全局模拟对话框 -->
    <el-dialog v-model="showSimulateDialog" title="全局模拟" width="680px">
      <el-form label-width="120px">
        <el-form-item label="生成作战次数">
          <el-input-number
            v-model="simulateCount"
            :min="1"
            :max="100"
            placeholder="输入要生成的作战次数"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="模拟模式">
          <el-radio-group v-model="simulateMode">
            <el-radio label="APPEND">
              新增到当前批次
              <el-tooltip content="保留已有数据，追加新的作战数据" placement="top">
                <el-icon class="info-icon"><InfoFilled /></el-icon>
              </el-tooltip>
            </el-radio>
            <el-radio label="COVER">
              覆盖当前批次
              <el-tooltip content="清除所有已有数据，重新生成" placement="top">
                <el-icon class="info-icon"><InfoFilled /></el-icon>
              </el-tooltip>
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-divider content-position="left">
          <span class="divider-title">定量指标离散度配置</span>
        </el-divider>

        <!-- 离散度模式选择 -->
        <el-form-item label="离散度设置">
          <el-radio-group v-model="dispersionMode">
            <el-radio label="unified">统一设置</el-radio>
            <el-radio label="separate">分开设置</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 统一设置模式 -->
        <el-form-item v-if="dispersionMode === 'unified'" label="全局离散度">
          <div class="dispersion-control">
            <el-slider
              v-model="simulateDispersion"
              :min="0"
              :max="100"
              :step="5"
              :format-tooltip="formatDispersion"
              style="flex: 1"
            />
            <span class="dispersion-value">{{ formatDispersion(simulateDispersion) }}</span>
          </div>
          <div class="dispersion-tip">
            <span v-if="simulateDispersion === 0">所有值都等于平均值</span>
            <span v-else-if="simulateDispersion <= 25">数据波动较小，集中在平均值附近</span>
            <span v-else-if="simulateDispersion <= 50">数据波动适中</span>
            <span v-else-if="simulateDispersion <= 75">数据波动较大</span>
            <span v-else>数据波动非常大</span>
          </div>
        </el-form-item>

        <!-- 分开设置模式 -->
        <div v-if="dispersionMode === 'separate'" class="indicator-dispersions">
          <!-- 统一调整区域 -->
          <div class="unified-adjust-area">
            <span class="unified-label">统一调整所有离散度：</span>
            <div class="unified-adjust-control">
              <el-slider
                v-model="unifiedDispersionValue"
                :min="0"
                :max="100"
                :step="5"
                :format-tooltip="(val) => (val / 100).toFixed(2)"
                style="flex: 1"
              />
              <el-button
                type="primary"
                size="small"
                @click="applyUnifiedDispersion"
              >
                应用到所有
              </el-button>
            </div>
          </div>

          <el-divider style="margin: 12px 0" />

          <!-- 各指标独立调节 -->
          <el-form-item
            v-for="indicator in indicatorStats"
            :key="indicator.code"
            :label="indicator.name"
            label-width="140px"
          >
            <div class="indicator-dispersion-item">
              <div class="indicator-info">
                <el-tag size="small" type="info">平均数: {{ indicator.averageValue?.toFixed(1) || 'N/A' }}</el-tag>
                <span class="range-info">
                  范围: [{{ calculateRange(indicator, indicatorDispersions[indicator.code] ?? 20).minVal.toFixed(1) }},
                        {{ calculateRange(indicator, indicatorDispersions[indicator.code] ?? 20).maxVal.toFixed(1) }}]
                </span>
              </div>
              <div class="dispersion-control">
                <el-slider
                  v-model="indicatorDispersions[indicator.code]"
                  :min="0"
                  :max="100"
                  :step="5"
                  :format-tooltip="(val) => (val / 100).toFixed(2)"
                  style="flex: 1"
                />
                <span class="dispersion-value">{{ ((indicatorDispersions[indicator.code] ?? 0) / 100).toFixed(2) }}</span>
              </div>
              <!-- 范围预览条 -->
              <div class="range-preview">
                <span class="range-min">{{ calculateRange(indicator, indicatorDispersions[indicator.code] ?? 20).minVal.toFixed(1) }}</span>
                <div class="range-bar">
                  <div class="range-center" :style="{ left: '50%' }">
                    <div class="center-mark"></div>
                    <span class="center-value">{{ indicator.averageValue?.toFixed(1) || 'N/A' }}</span>
                  </div>
                </div>
                <span class="range-max">{{ calculateRange(indicator, indicatorDispersions[indicator.code] ?? 20).maxVal.toFixed(1) }}</span>
              </div>
            </div>
          </el-form-item>
        </div>

        <el-form-item v-if="simulateMode === 'COVER' && operationCount > 0">
          <el-alert type="warning" :closable="false">
            警告：覆盖模式将删除当前批次的 {{ operationCount }} 条作战数据！
          </el-alert>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSimulateDialog = false">取消</el-button>
        <el-button
          type="warning"
          :loading="simulating"
          :disabled="!simulateCount"
          @click="handleGlobalSimulate"
        >
          开始模拟
        </el-button>
      </template>
    </el-dialog>

    <!-- 删除批次确认对话框 -->
    <el-dialog v-model="showDeleteDialog" title="删除批次" width="450px">
      <div class="delete-confirm">
        <el-alert type="error" :closable="false">
          确定要删除批次 "{{ currentBatchId }}" 吗？此操作不可恢复！
        </el-alert>
        <div class="delete-info" v-if="operationCount > 0 || normalizationName">
          <p>此操作将删除：</p>
          <ul>
            <li v-if="operationCount > 0">{{ operationCount }} 条原始数据</li>
            <li v-if="normalizationName">归一化结果</li>
          </ul>
        </div>
      </div>
      <template #footer>
        <el-button @click="showDeleteDialog = false">取消</el-button>
        <el-button type="danger" @click="confirmDeleteBatch">确认删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Grid, Document, Refresh, Plus, VideoPlay, Delete, MagicStick, DataLine, TrendCharts, InfoFilled } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  getIndicatorTemplates,
  getDynamicQtIndicators,
  getDynamicQtBatchesByTemplate,
  createDynamicQtBatch,
  getDynamicQtRecords,
  globalSimulateDynamicQt,
  normalizeDynamicQt,
  getNormalizationRecords,
  deleteDynamicQtBatch,
  simulateDynamicQtCell,
  saveDynamicQtRecord,
  deleteNormalization,
  getQtIndicatorStats,
} from '@/api'

// 状态
const templates = ref([])
const batches = ref([])
const selectedTemplateId = ref(null)
const selectedBatchId = ref(null)
const currentBatchId = ref(null)
const operationCount = ref(0)
const rendering = ref(false)
const normalizing = ref(false)
const simulating = ref(false)
const hasRendered = ref(false)

// 模拟相关
const showSimulateDialog = ref(false)
const simulateCount = ref(5)
const simulateMode = ref('APPEND')
const simulateDispersion = ref(20) // 离散度 0-100，对应 0-1

// 离散度配置模式：true=统一设置，false=分开设置
const dispersionMode = ref('unified') // 'unified' | 'separate'

// 统一调整离散度的临时值
const unifiedDispersionValue = ref(20)

// 指标统计信息（从后端获取）
const indicatorStats = ref([])
const indicatorDispersions = ref({}) // 各指标独立离散度 { code: value }

// 删除相关
const showDeleteDialog = ref(false)

// 定性指标相关
const showQualitativeDialog = ref(false)
const currentQualitative = ref(null)
const selectedQualitativeLevel = ref('GOOD')

// 归一化相关
const normalizedData = ref([])
const normalizationName = ref('')

// 图表相关
const chartType = ref('bar')
const chartInstances = ref({})
const activeChartTab = ref('')

// 数据
const levels = ref([])
const tableData = ref([])
const activeLevel = ref('')

// 计算属性：过滤后的层级数据（只显示定量指标）
const filteredLevels = computed(() => {
  return levels.value.map(level => ({
    ...level,
    primaries: level.primaries
      .map(primary => ({
        ...primary,
        secondaries: primary.secondaries.filter(sec => sec.metricType !== 'QUALITATIVE')
      }))
      .filter(primary => primary.secondaries.length > 0)
  })).filter(level => level.primaries.length > 0)
})

// 合并一级维度单元格
const mergePrimaryCells = ({ row, column, rowIndex, columnIndex }) => {
  return { rowspan: 1, colspan: 1 }
}

// 加载模板列表
const loadTemplates = async () => {
  try {
    const res = await getIndicatorTemplates()
    templates.value = res || []
  } catch (e) {
    console.error('加载模板失败:', e)
  }
}

// 渲染：根据模板加载表格结构
const handleRender = async () => {
  if (!selectedTemplateId.value) {
    ElMessage.warning('请先选择指标模板')
    return
  }

  rendering.value = true
  try {
    // 加载该模板下的批次列表
    await loadBatches(selectedTemplateId.value)

    // 创建临时批次用于获取指标结构
    const createRes = await createDynamicQtBatch(selectedTemplateId.value, '动态定量评估')

    if (createRes && createRes.batchId) {
      // 临时批次ID用于获取指标结构
      const tempBatchId = createRes.batchId

      // 获取指标结构
      const indicatorsRes = await getDynamicQtIndicators(tempBatchId, selectedTemplateId.value)
      if (indicatorsRes && indicatorsRes.levels) {
        levels.value = indicatorsRes.levels

        if (filteredLevels.value.length > 0) {
          activeLevel.value = filteredLevels.value[0].levelName
        } else if (levels.value.length > 0) {
          activeLevel.value = levels.value[0].levelName
        }
      }

      // 删除临时批次
      await deleteDynamicQtBatch(tempBatchId)

      hasRendered.value = true
      currentBatchId.value = null
      selectedBatchId.value = null
      tableData.value = []
      normalizedData.value = []
      normalizationName.value = ''
      operationCount.value = 0

      ElMessage.success('渲染成功，请选择已有批次或新建批次并模拟数据')
    }
  } catch (e) {
    ElMessage.error('渲染失败: ' + (e.message || '未知错误'))
  } finally {
    rendering.value = false
  }
}

// 加载批次列表
const loadBatches = async (templateId) => {
  try {
    const res = await getDynamicQtBatchesByTemplate(templateId)
    batches.value = res || []
  } catch (e) {
    console.error('加载批次列表失败:', e)
    batches.value = []
  }
}

// 批次选择变化
const handleBatchChange = async (batchId) => {
  if (!batchId) {
    currentBatchId.value = null
    tableData.value = []
    normalizedData.value = []
    normalizationName.value = ''
    operationCount.value = 0
    return
  }

  currentBatchId.value = batchId
  await loadBatchData()
}

// 新建并模拟
const handleNewBatchAndSimulate = async () => {
  if (!selectedTemplateId.value) {
    ElMessage.warning('请先选择指标模板')
    return
  }

  try {
    // 1. 创建新批次
    const createRes = await createDynamicQtBatch(selectedTemplateId.value, '动态定量评估')
    if (!createRes || !createRes.batchId) {
      ElMessage.error('创建批次失败')
      return
    }

    currentBatchId.value = createRes.batchId
    selectedBatchId.value = createRes.batchId

    // 2. 加载指标结构
    const indicatorsRes = await getDynamicQtIndicators(currentBatchId.value, selectedTemplateId.value)
    if (indicatorsRes && indicatorsRes.levels) {
      levels.value = indicatorsRes.levels
      if (filteredLevels.value.length > 0) {
        activeLevel.value = filteredLevels.value[0].levelName
      } else if (levels.value.length > 0) {
        activeLevel.value = levels.value[0].levelName
      }
    }

    // 3. 加载指标统计信息（用于模拟配置）
    await loadIndicatorStats()

    // 4. 重置模拟参数并弹出对话框
    simulateCount.value = 5
    simulateMode.value = 'APPEND'
    simulateDispersion.value = 20
    dispersionMode.value = 'unified'
    unifiedDispersionValue.value = 20
    showSimulateDialog.value = true

    // 5. 刷新批次列表
    await loadBatches(selectedTemplateId.value)

  } catch (e) {
    ElMessage.error('操作失败: ' + (e.message || '未知错误'))
  }
}

// 加载批次数据
const loadBatchData = async () => {
  if (!currentBatchId.value) return

  try {
    // 加载原始数据
    const res = await getDynamicQtRecords(currentBatchId.value)
    if (res && res.tableData) {
      tableData.value = res.tableData
      operationCount.value = res.operationIds?.length || tableData.value.length
    } else {
      tableData.value = []
      operationCount.value = 0
    }

    // 加载归一化数据
    await loadNormalizedData()

    hasRendered.value = true
  } catch (e) {
    console.error('加载批次数据失败:', e)
    tableData.value = []
    operationCount.value = 0
  }
}

// Tab切换
const handleTabChange = async (levelName) => {
  activeLevel.value = levelName
}

// 格式化离散度显示
const formatDispersion = (val) => {
  return (val / 100).toFixed(2)
}

// 打开模拟对话框
const openSimulateDialog = async () => {
  await loadIndicatorStats()
  simulateCount.value = 5
  simulateMode.value = 'APPEND'
  simulateDispersion.value = 20
  dispersionMode.value = 'unified'
  unifiedDispersionValue.value = 20
  showSimulateDialog.value = true
}

// 加载指标统计信息（用于模拟配置）
const loadIndicatorStats = async () => {
  if (!selectedTemplateId.value) return
  
  try {
    const res = await getQtIndicatorStats(selectedTemplateId.value)
    if (res && res.indicators) {
      indicatorStats.value = res.indicators
      // 初始化各指标离散度为全局离散度值
      const dispersions = {}
      res.indicators.forEach(ind => {
        dispersions[ind.code] = simulateDispersion.value
      })
      indicatorDispersions.value = dispersions
    }
  } catch (e) {
    console.error('加载指标统计失败:', e)
    indicatorStats.value = []
  }
}

// 计算单个指标的范围
const calculateRange = (indicator, dispersionValue) => {
  const avg = indicator.averageValue ?? 50  // 默认50
  const disp = (dispersionValue ?? 20) / 100
  const range = Math.abs(avg) * disp
  const minVal = Math.max(0, avg - range)
  const maxVal = avg + range
  return { minVal, maxVal, avg }
}

// 应用统一离散度到所有指标
const applyUnifiedDispersion = () => {
  const newDispersions = {}
  indicatorStats.value.forEach(ind => {
    newDispersions[ind.code] = unifiedDispersionValue.value
  })
  indicatorDispersions.value = newDispersions
}

// 全局模拟
const handleGlobalSimulate = async () => {
  if (!currentBatchId.value || !simulateCount.value) {
    ElMessage.warning('请输入生成次数')
    return
  }

  simulating.value = true
  try {
    const selectedTemplate = templates.value.find(t => t.id === selectedTemplateId.value)
    // 将 0-100 的离散度转换为 0-1
    const dispersion = simulateDispersion.value / 100
    
    let dispersions = null
    // 如果是分开设置模式，传递各指标独立离散度
    if (dispersionMode.value === 'separate' && Object.keys(indicatorDispersions.value).length > 0) {
      dispersions = {}
      for (const [code, value] of Object.entries(indicatorDispersions.value)) {
        dispersions[code] = value / 100 // 转换为 0-1
      }
    }
    
    const res = await globalSimulateDynamicQt(
      currentBatchId.value,
      simulateCount.value,
      selectedTemplateId.value,
      selectedTemplate?.templateName || '',
      simulateMode.value,
      dispersion,
      dispersions
    )

    if (res && res.success) {
      ElMessage.success(`模拟成功！共 ${res.operationCount} 条作战数据`)
      showSimulateDialog.value = false
      simulateCount.value = 5
      simulateMode.value = 'APPEND'
      simulateDispersion.value = 20
      dispersionMode.value = 'unified'
      unifiedDispersionValue.value = 20

      // 刷新数据
      await loadTableData()
      await loadNormalizedData()
      await loadBatches(selectedTemplateId.value)
    } else {
      ElMessage.error(res?.message || '模拟失败')
    }
  } catch (e) {
    ElMessage.error('模拟失败: ' + (e.message || '未知错误'))
  } finally {
    simulating.value = false
  }
}

// 加载表格数据
const loadTableData = async () => {
  if (!currentBatchId.value) return

  try {
    const res = await getDynamicQtRecords(currentBatchId.value)
    if (res && res.tableData) {
      tableData.value = res.tableData
      operationCount.value = res.operationIds?.length || tableData.value.length
    } else {
      tableData.value = []
      operationCount.value = 0
    }
  } catch (e) {
    console.error('加载表格数据失败:', e)
    tableData.value = []
    operationCount.value = 0
  }
}

// 格式化数值
const formatValue = (value) => {
  if (value === undefined || value === null) return '—'
  const num = Number(value)
  if (Number.isNaN(num)) return '—'
  return num.toFixed(3)
}

// 归一化值格式化
const formatNormalizedValue = (value) => {
  if (value === undefined || value === null) return '—'
  const num = Number(value)
  if (Number.isNaN(num)) return '—'
  return num.toFixed(4)
}

// 归一化值样式
const getNormalizedValueClass = (value) => {
  if (value === undefined || value === null) return 'empty-value'
  const num = Number(value)
  if (num >= 0.8) return 'score-excellent'
  if (num >= 0.6) return 'score-good'
  if (num >= 0.4) return 'score-fair'
  return 'score-poor'
}

// 定性指标等级映射
const qualitativeLevels = {
  'EXCELLENT': { label: '优秀', value: 1.0 },
  'GOOD': { label: '良好', value: 0.75 },
  'FAIR': { label: '一般', value: 0.5 },
  'POOR': { label: '差', value: 0.25 },
}

const getQualitativeTagType = (level) => {
  const types = {
    'EXCELLENT': 'success',
    'GOOD': 'primary',
    'FAIR': 'warning',
    'POOR': 'danger',
  }
  return types[level] || 'info'
}

const formatQualitativeValue = (level) => {
  return qualitativeLevels[level]?.label || level || '—'
}

const handleQualitativeClick = (row, sec) => {
  currentQualitative.value = { row, sec }
  selectedQualitativeLevel.value = row.values?.[sec.code + '_level'] || 'GOOD'
  showQualitativeDialog.value = true
}

const handleQualitativeSave = async () => {
  if (!currentQualitative.value || !selectedQualitativeLevel.value) return

  const { row, sec } = currentQualitative.value
  try {
    await saveDynamicQtRecord(
      currentBatchId.value,
      row.operationId,
      sec.code,
      qualitativeLevels[selectedQualitativeLevel.value].value,
      selectedQualitativeLevel.value
    )

    showQualitativeDialog.value = false
    await loadTableData()
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || '未知错误'))
  }
}

const handleCellClick = async (row, sec) => {
  if (!currentBatchId.value) {
    ElMessage.warning('请先选择或新建批次')
    return
  }

  try {
    const res = await simulateDynamicQtCell(currentBatchId.value, row.operationId, sec.code)

    if (res && res.value !== undefined) {
      if (!row.values) row.values = {}
      row.values[sec.code] = res.value
      await loadTableData()
      ElMessage.success('模拟成功')
    }
  } catch (e) {
    ElMessage.error('模拟失败: ' + (e.message || '未知错误'))
  }
}

// 执行归一化
const handleNormalize = async () => {
  if (!currentBatchId.value) return

  if (operationCount.value === 0) {
    ElMessage.warning('没有可归一化的数据，请先进行模拟')
    return
  }

  normalizing.value = true
  try {
    const res = await normalizeDynamicQt(currentBatchId.value)

    if (res && res.success) {
      ElMessage.success(`归一化完成: ${res.operationCount} 条记录`)
      normalizationName.value = res.normalizationName || ''

      // 加载归一化数据
      await loadNormalizedData()

      // 滚动到归一化结果区域
      nextTick(() => {
        const normalizeCard = document.querySelector('.normalize-card')
        if (normalizeCard) {
          normalizeCard.scrollIntoView({ behavior: 'smooth' })
        }
      })
    } else {
      ElMessage.error(res?.message || '归一化失败')
    }
  } catch (e) {
    ElMessage.error('归一化失败: ' + (e.message || '未知错误'))
  } finally {
    normalizing.value = false
  }
}

// 加载归一化数据
const loadNormalizedData = async () => {
  if (!currentBatchId.value) return

  try {
    const res = await getNormalizationRecords(currentBatchId.value)
    if (res && res.tableData) {
      normalizedData.value = res.tableData
      normalizationName.value = res.normalizationName || ''
      nextTick(() => {
        renderChart()
      })
    } else {
      normalizedData.value = []
      normalizationName.value = ''
    }
  } catch (e) {
    console.error('加载归一化数据失败:', e)
    normalizedData.value = []
    normalizationName.value = ''
  }
}

// 删除归一化
const handleDeleteNormalization = async () => {
  if (!currentBatchId.value) return

  try {
    await ElMessageBox.confirm(
      '确定要删除归一化结果吗？',
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )

    await deleteNormalization(currentBatchId.value)
    ElMessage.success('删除成功')
    normalizedData.value = []
    normalizationName.value = ''
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败: ' + (e.message || '未知错误'))
    }
  }
}

// 删除批次
const handleDeleteBatch = async () => {
  if (!currentBatchId.value) return
  showDeleteDialog.value = true
}

const confirmDeleteBatch = async () => {
  if (!currentBatchId.value) return

  try {
    await deleteDynamicQtBatch(currentBatchId.value)
    ElMessage.success('删除成功')
    showDeleteDialog.value = false

    // 重置状态
    selectedBatchId.value = null
    currentBatchId.value = null
    tableData.value = []
    normalizedData.value = []
    normalizationName.value = ''
    operationCount.value = 0

    // 刷新批次列表
    await loadBatches(selectedTemplateId.value)
  } catch (e) {
    ElMessage.error('删除失败: ' + (e.message || '未知错误'))
  }
}

// 渲染图表
const renderChart = () => {
  if (normalizedData.value.length === 0 || filteredLevels.value.length === 0) {
    return
  }

  nextTick(() => {
    for (const level of filteredLevels.value) {
      renderLevelChart(level)
    }
  })
}

const renderLevelChart = (level) => {
  const container = document.getElementById('chart-' + level.levelName)
  if (!container) return

  const indicators = []
  for (const primary of level.primaries || []) {
    for (const sec of primary.secondaries || []) {
      if (sec.metricType !== 'QUALITATIVE') {
        indicators.push(sec)
      }
    }
  }

  if (indicators.length === 0) return

  if (chartInstances.value[level.levelName]) {
    chartInstances.value[level.levelName].dispose()
    chartInstances.value[level.levelName] = null
  }

  const xAxisData = normalizedData.value.map(row => row.operationId)

  const series = indicators.map((sec, index) => {
    const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc']
    return {
      name: sec.name,
      type: chartType.value,
      data: normalizedData.value.map(row => {
        const val = row.values?.[sec.code]
        return val !== undefined && val !== null ? Number(val) : null
      }),
      itemStyle: {
        color: colors[index % colors.length]
      },
      label: {
        show: chartType.value === 'bar',
        position: 'top',
        fontSize: 10,
        formatter: (params) => params.value !== null ? params.value.toFixed(2) : ''
      }
    }
  })

  const chartInstance = echarts.init(container)
  chartInstances.value[level.levelName] = chartInstance

  const option = {
    title: {
      text: `${level.levelName} 归一化结果`,
      subtext: `包含 ${indicators.length} 个定量指标`,
      left: 'center',
      textStyle: {
        fontSize: 14,
        fontWeight: 'bold'
      }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: indicators.map(sec => sec.name),
      top: 50,
      type: 'scroll',
      width: '80%'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '8%',
      top: '100px',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: xAxisData,
      axisLabel: {
        rotate: 0,
        interval: 0
      }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 1,
      axisLabel: {
        formatter: (value) => value.toFixed(2)
      }
    },
    series: series
  }

  if (chartType.value === 'line') {
    series.forEach(s => {
      s.smooth = true
      s.label.show = false
    })
  } else {
    series.forEach(s => {
      s.barMaxWidth = 40
    })
  }

  chartInstance.setOption(option)
}

watch(chartType, () => {
  renderChart()
})

watch(activeChartTab, () => {
  setTimeout(() => {
    if (activeChartTab.value && chartInstances.value[activeChartTab.value]) {
      chartInstances.value[activeChartTab.value].resize()
    }
  }, 100)
})

watch(normalizedData, () => {
  if (normalizedData.value.length > 0) {
    nextTick(() => {
      renderChart()
    })
  }
}, { deep: true })

onMounted(() => {
  loadTemplates()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  Object.values(chartInstances.value).forEach(instance => {
    if (instance) {
      instance.dispose()
    }
  })
  chartInstances.value = {}
})

const handleResize = () => {
  Object.values(chartInstances.value).forEach(instance => {
    if (instance) {
      instance.resize()
    }
  })
}
</script>

<style scoped lang="scss">
:deep(.el-table th.el-table__cell) {
  background-color: #304156 !important;
  color: #ffffff !important;
  font-weight: 600;
}

:deep(.el-table .el-table__header-wrapper th) {
  background-color: #304156 !important;
  color: #ffffff !important;
}

:deep(.el-table .el-table__header-wrapper .el-table__row:first-child th) {
  background-color: #1e3a5f !important;
  border-color: #2d5a8a !important;
}

:deep(.el-table .el-table__header-wrapper .el-table__row:last-child th) {
  background-color: #304156 !important;
  border-color: #3d6a9a !important;
}

.dynamic-quantitative-view {
  padding: 20px;
  max-width: 1800px;
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
      color: #409EFF;
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
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.batch-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;

  .batch-id {
    font-weight: 500;
  }

  .batch-count {
    font-size: 12px;
    color: #909399;
  }
}

.template-stats {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}

.table-card {
  :deep(.el-card__header) {
    padding: 12px 16px;
    background: #f5f7fa;
  }
}

.table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .header-left {
    display: flex;
    align-items: center;
    font-weight: 700;
    font-size: 16px;
    color: #1a1a1a;
    gap: 8px;

    .el-icon {
      font-size: 20px;
      margin-right: 0;
      color: #409EFF;
    }
  }

  .header-actions {
    display: flex;
    gap: 8px;
  }
}

.level-tabs {
  margin-top: 12px;
}

.empty-table-tip {
  padding: 20px;
  background: #fafafa;
  border-radius: 4px;
}

.sec-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  line-height: 1.4;

  span {
    font-size: 13px;
    font-weight: 600;
    color: #ffffff;
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  }

  .header-tags {
    display: flex;
    gap: 4px;
    flex-wrap: wrap;
    justify-content: center;
  }

  :deep(.el-tag) {
    font-size: 10px;
    font-weight: 700;
    padding: 0 4px;
    height: 16px;
    line-height: 14px;

    &.el-tag--success {
      background-color: #2d6a2d;
      border-color: #4a9a4a;
      color: #a8e6a8;
    }

    &.el-tag--warning {
      background-color: #6a4a2d;
      border-color: #9a6a4a;
      color: #ffe4a8;
    }

    &.el-tag--primary {
      background-color: #1a4a6a;
      border-color: #4a6a9a;
      color: #a8d4ff;
    }

    &.el-tag--info {
      background-color: #4a4a5a;
      border-color: #7a7a9a;
      color: #d4d4e8;
    }
  }
}

.cell-value {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 4px;

  &:hover {
    background: #ecf5ff;
  }

  &.has-value .raw-value {
    color: #303133;
    font-weight: 500;
  }
}

.raw-value {
  color: #303133;
}

.empty-value {
  color: #c0c4cc;
}

.operation-column {
  :deep(.el-table__header-wrapper th) {
    background-color: #1e3a5f !important;
  }
}

.normalize-card {
  margin-top: 20px;

  :deep(.el-card__header) {
    padding: 12px 16px;
    background: #f5f7fa;
  }
}

.score-excellent {
  color: #67c23a;
  font-weight: 600;
}

.score-good {
  color: #409eff;
  font-weight: 500;
}

.score-fair {
  color: #e6a23c;
  font-weight: 500;
}

.score-poor {
  color: #f56c6c;
  font-weight: 500;
}

.comprehensive-score {
  margin-top: 16px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;

  .score-label {
    font-weight: 600;
    color: #303133;
    margin-right: 8px;
  }

  .score-desc {
    color: #606266;
  }
}

.normalized-table-section {
  margin-top: 16px;

  .table-section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 15px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 12px;
    padding-bottom: 8px;
    border-bottom: 2px solid #409eff;

    .el-icon {
      color: #409eff;
    }
  }
}

.chart-section {
  margin-top: 24px;
  padding: 16px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;

  .chart-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid #ebeef5;

    .chart-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 15px;
      font-weight: 600;
      color: #303133;

      .el-icon {
        color: #409eff;
      }
    }

    .chart-controls {
      display: flex;
      align-items: center;
    }
  }

  .chart-level-tabs {
    :deep(.el-tabs__content) {
      padding: 16px;
    }
    :deep(.el-tab-pane) {
      display: block;
    }
    :deep(.el-tabs__header) {
      margin-bottom: 0;
    }
  }

  .level-chart-container {
    width: 100%;
    height: 400px;
    min-height: 350px;
    display: block;
  }
}

.info-icon {
  margin-left: 4px;
  color: #909399;
  cursor: pointer;
}

.delete-confirm {
  .delete-info {
    margin-top: 16px;
    padding: 12px;
    background: #f5f7fa;
    border-radius: 4px;

    p {
      margin: 0 0 8px 0;
      font-weight: 500;
    }

    ul {
      margin: 0;
      padding-left: 20px;
      color: #606266;
    }
  }
}

.dispersion-control {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
}

.dispersion-value {
  min-width: 50px;
  font-weight: 600;
  color: #409eff;
  font-size: 16px;
}

.dispersion-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.divider-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.indicator-dispersions {
  max-height: 400px;
  overflow-y: auto;
  margin: 8px 0;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.unified-adjust-area {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: #ecf5ff;
  border-radius: 6px;
  margin-bottom: 8px;
}

.unified-label {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
}

.unified-adjust-control {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.indicator-dispersion-item {
  width: 100%;
}

.indicator-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.range-info {
  font-size: 12px;
  color: #606266;
}

.range-preview {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.range-bar {
  flex: 1;
  height: 8px;
  background: linear-gradient(to right, #67c23a, #e6a23c, #67c23a);
  border-radius: 4px;
  position: relative;
}

.range-center {
  position: absolute;
  top: -4px;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.center-mark {
  width: 2px;
  height: 16px;
  background: #303133;
}

.center-value {
  font-size: 11px;
  color: #303133;
  font-weight: 600;
  white-space: nowrap;
}
</style>
