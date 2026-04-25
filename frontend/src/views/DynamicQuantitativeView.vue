<template>
  <div class="dynamic-quantitative-view">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>
        <el-icon><Grid /></el-icon>
        动态定量评估
      </h2>
      <p class="subtitle">根据指标模板动态生成评估表格，支持模拟数据生成</p>
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

        <div class="toolbar-right">
          <el-tag v-if="currentBatchId" type="success">批次: {{ currentBatchId }}</el-tag>
          <el-tag v-if="levelCount > 0" type="info">{{ levelCount }} 个层级</el-tag>
        </div>
      </div>
    </el-card>

    <!-- 动态内容区域 -->
    <el-card v-if="hasRendered" class="table-card" shadow="never">
      <template #header>
        <div class="table-header">
          <div class="header-left">
            <el-icon><Document /></el-icon>
            <span>评估数据表</span>
          </div>
          <div class="header-actions">
            <el-button type="warning" :icon="MagicStick" @click="showSimulateDialog = true">
              全局模拟
            </el-button>
            <el-button type="success" :icon="VideoPlay" :loading="normalizing" @click="handleNormalize">
              归一化
            </el-button>
            <el-button type="danger" plain :icon="Delete" @click="handleDeleteBatch">
              删除批次
            </el-button>
          </div>
        </div>
      </template>

      <!-- 按层级分 Tab -->
      <el-tabs v-model="activeLevel" type="border-card" class="level-tabs" @tab-change="handleTabChange">
        <el-tab-pane
          v-for="level in levels"
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
              <!-- 一级维度列（合并下面的二级指标） -->
              <el-table-column
                :label="primary.primaryName"
                align="center"
                header-align="center"
                :colspan="primary.secondaries.length"
              >
                <!-- 二级指标列 -->
                <el-table-column
                  v-for="(sec, sIdx) in primary.secondaries"
                  :key="'sec-' + pIdx + '-' + sIdx"
                  :label="sec.name"
                  align="center"
                  header-align="center"
                  min-width="120"
                >
                  <!-- 自定义表头：显示正向/负向 -->
                  <template #header>
                    <div class="sec-header">
                      <span>{{ sec.name }}</span>
                      <el-tag
                        size="small"
                        :type="sec.direction === 'POSITIVE' ? 'success' : 'warning'"
                      >
                        {{ sec.direction === 'POSITIVE' ? '正向' : '反向' }}
                      </el-tag>
                    </div>
                  </template>

                  <!-- 数据单元格 -->
                  <template #default="{ row }">
                    <div
                      class="cell-value"
                      :class="{
                        'has-value': row.values && row.values[sec.code] !== undefined && row.values[sec.code] !== null
                      }"
                      @click="handleCellClick(row, sec)"
                    >
                      <template v-if="normalizedMode && row.values && row.values[sec.code] !== undefined">
                        <span class="normalized-value">
                          {{ formatValue(row.values[sec.code]) }}
                        </span>
                      </template>
                      <template v-else-if="row.values && row.values[sec.code] !== undefined && row.values[sec.code] !== null">
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
        </el-tab-pane>
      </el-tabs>

      <div v-if="normalizedMode" class="normalize-info">
        <el-alert type="success" :closable="false">
          <template #title>
            当前显示的是归一化得分（0~1），数值越大表示该指标表现越好
          </template>
        </el-alert>
      </div>
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

    <!-- 全局模拟对话框 -->
    <el-dialog v-model="showSimulateDialog" title="全局模拟" width="500px">
      <el-form>
        <el-form-item label="生成作战次数">
          <el-input-number
            v-model="simulateCount"
            :min="1"
            :max="100"
            placeholder="输入要生成的作战次数"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item>
          <el-alert type="info" :closable="false">
            将自动生成指定次数的作战数据
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Grid, Document, Refresh, VideoPlay, Delete, MagicStick } from '@element-plus/icons-vue'
import {
  getIndicatorTemplates,
  createDynamicQtBatch,
  getDynamicQtIndicators,
  getDynamicQtRecords,
  globalSimulateDynamicQt,
  normalizeDynamicQt,
  getNormalizedDynamicQtRecords,
  deleteDynamicQtBatch,
  simulateDynamicQtCell,
} from '@/api'

// 状态
const templates = ref([])
const selectedTemplateId = ref(null)
const currentBatchId = ref(null)
const rendering = ref(false)
const normalizing = ref(false)
const simulating = ref(false)
const hasRendered = ref(false)
const normalizedMode = ref(false)
const activeLevel = ref('')
const showSimulateDialog = ref(false)
const simulateCount = ref(1)

// 数据
const levels = ref([])
const tableData = ref([])

// 计算属性
const levelCount = computed(() => levels.value.length)

// 合并一级维度单元格
const mergePrimaryCells = ({ row, column, rowIndex, columnIndex }) => {
  // 不需要额外合并，因为使用了 colspan
  return { rowspan: 1, colspan: 1 }
}

const loadTemplates = async () => {
  try {
    const res = await getIndicatorTemplates()
    templates.value = res || []
  } catch (e) {
    console.error('加载模板失败:', e)
  }
}

const handleRender = async () => {
  if (!selectedTemplateId.value) {
    ElMessage.warning('请先选择指标模板')
    return
  }

  rendering.value = true
  try {
    const createRes = await createDynamicQtBatch(selectedTemplateId.value, '动态定量评估')

    if (createRes && createRes.batchId) {
      currentBatchId.value = createRes.batchId
    }

    await loadIndicators()
    hasRendered.value = true
    ElMessage.success('渲染成功')
  } catch (e) {
    ElMessage.error('渲染失败: ' + (e.message || '未知错误'))
    console.error(e)
  } finally {
    rendering.value = false
  }
}

const loadIndicators = async () => {
  if (!currentBatchId.value) return

  try {
    const res = await getDynamicQtIndicators(currentBatchId.value)
    if (res && res.levels) {
      levels.value = res.levels

      if (levels.value.length > 0) {
        activeLevel.value = levels.value[0].levelName
        await loadTableData()
      }
    }
  } catch (e) {
    console.error('加载指标失败:', e)
  }
}

const loadTableData = async () => {
  if (!currentBatchId.value) return

  try {
    const res = normalizedMode.value
      ? await getNormalizedDynamicQtRecords(currentBatchId.value)
      : await getDynamicQtRecords(currentBatchId.value)

    if (res && res.tableData) {
      tableData.value = res.tableData
    }
  } catch (e) {
    console.error('加载表格数据失败:', e)
  }
}

const handleTabChange = async (levelName) => {
  activeLevel.value = levelName
  await loadTableData()
}

const handleGlobalSimulate = async () => {
  if (!currentBatchId.value || !simulateCount.value) {
    ElMessage.warning('请输入生成次数')
    return
  }

  simulating.value = true
  try {
    const res = await globalSimulateDynamicQt(currentBatchId.value, simulateCount.value)

    if (res && res.success) {
      ElMessage.success(`模拟成功！共模拟了 ${res.simulatedCount || 0} 个数据`)
      showSimulateDialog.value = false
      await loadTableData()
    } else {
      ElMessage.error(res?.message || '模拟失败')
    }
  } catch (e) {
    ElMessage.error('模拟失败: ' + (e.message || '未知错误'))
  } finally {
    simulating.value = false
  }
}

const formatValue = (value) => {
  if (value === undefined || value === null) return '—'
  const num = Number(value)
  if (Number.isNaN(num)) return '—'
  return num.toFixed(3)
}

const handleCellClick = async (row, sec) => {
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

const handleNormalize = async () => {
  if (!currentBatchId.value) return

  normalizing.value = true
  try {
    const res = await normalizeDynamicQt(currentBatchId.value)

    if (res && res.success) {
      normalizedMode.value = true
      await loadTableData()
      ElMessage.success(`归一化完成: ${res.normalizedCount || 0} 条记录`)
    } else {
      ElMessage.error(res?.message || '归一化失败')
    }
  } catch (e) {
    ElMessage.error('归一化失败: ' + (e.message || '未知错误'))
  } finally {
    normalizing.value = false
  }
}

const handleDeleteBatch = async () => {
  if (!currentBatchId.value) return

  try {
    await ElMessageBox.confirm(
      '确定要删除当前批次吗？此操作不可恢复。',
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )

    await deleteDynamicQtBatch(currentBatchId.value)
    ElMessage.success('删除成功')
    handleReset()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败: ' + (e.message || '未知错误'))
    }
  }
}

const handleReset = () => {
  currentBatchId.value = null
  hasRendered.value = false
  normalizedMode.value = false
  levels.value = []
  tableData.value = []
  activeLevel.value = ''
  selectedTemplateId.value = null
  simulateCount.value = 1
  showSimulateDialog.value = false
}

onMounted(() => {
  loadTemplates()
})
</script>

<style scoped lang="scss">
// 深色表头样式
:deep(.el-table th.el-table__cell) {
  background-color: #304156 !important;
  color: #ffffff !important;
  font-weight: 600;
}

:deep(.el-table .el-table__header-wrapper th) {
  background-color: #304156 !important;
  color: #ffffff !important;
}

// 一级维度表头（一级嵌套列）
:deep(.el-table .el-table__header-wrapper .el-table__row:first-child th) {
  background-color: #1e3a5f !important;
  border-color: #2d5a8a !important;
}

// 二级指标表头
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

    .el-icon {
      font-size: 20px;
      margin-right: 8px;
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

  :deep(.el-tag) {
    font-size: 11px;
    font-weight: 700;
    padding: 0 6px;
    height: 18px;
    line-height: 16px;

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

.normalized-value {
  color: #67c23a;
  font-weight: 600;
}

.empty-value {
  color: #c0c4cc;
}

.normalize-info {
  margin-top: 12px;
}

.operation-column {
  :deep(.el-table__header-wrapper th) {
    background-color: #1e3a5f !important;
  }
}
</style>
