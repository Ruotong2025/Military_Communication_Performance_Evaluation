<template>
  <div class="dynamic-indicator-view">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>
        <el-icon><Setting /></el-icon>
        动态指标管理
      </h2>
      <p class="subtitle">通过导入Excel定义指标体系，支持动态配置评估维度</p>
    </div>

    <!-- 导入区域 -->
    <el-card class="import-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>
            <el-icon><Upload /></el-icon>
            指标定义导入
          </span>
          <div class="header-actions">
            <el-select
              v-if="templates.length > 0"
              v-model="selectedTemplateId"
              placeholder="选择已有模板"
              size="default"
              style="width: 200px; margin-right: 10px"
              @change="handleTemplateChange"
            >
              <el-option
                v-for="tpl in templates"
                :key="tpl.id"
                :label="tpl.templateName"
                :value="tpl.id"
              />
            </el-select>
            <el-button
              v-if="selectedTemplateId"
              type="danger"
              size="default"
              @click="handleDeleteTemplate"
            >
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
            <el-button type="primary" @click="triggerUpload">
              <el-icon><Upload /></el-icon>
              导入Excel
            </el-button>
          </div>
        </div>
      </template>

      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
        style="display: none"
      >
        <template #trigger>
          <el-button ref="uploadBtn">选择文件</el-button>
        </template>
      </el-upload>

      <!-- 已选文件显示 -->
      <div v-if="currentFile" class="file-info">
        <el-icon><Document /></el-icon>
        <span>{{ currentFile.name }}</span>
        <el-button type="primary" size="small" @click="handleParse">
          解析Excel
        </el-button>
      </div>

      <!-- Excel格式说明 -->
      <div class="format-tip">
        <el-alert type="info" :closable="false" show-icon>
          <template #title>
            <strong>Excel格式要求</strong>
          </template>
          <template #default>
            <ul class="format-list">
              <li><strong>第1列（层级）：</strong>包含层级名称和描述信息（支持合并单元格）</li>
              <li><strong>第2列（一级维度）：</strong>用于权重配置的评价维度</li>
              <li><strong>第3列（二级维度）：</strong>最小评估粒度的指标项</li>
            </ul>
          </template>
        </el-alert>
      </div>

      <!-- 模板名称输入 -->
      <div class="template-name-input" v-if="currentFile">
        <el-input
          v-model="templateName"
          placeholder="请输入模板名称（用于保存）"
          style="width: 300px"
        >
          <template #prepend>模板名称</template>
        </el-input>
      </div>
    </el-card>

    <!-- 解析结果展示 -->
    <el-card v-if="parsedData" class="result-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>
            <el-icon><DataAnalysis /></el-icon>
            指标体系预览
          </span>
          <div class="header-actions">
            <el-tag type="primary">层级: {{ parsedData.statistics.levelCount }}</el-tag>
            <el-tag type="success">一级维度: {{ parsedData.statistics.totalPrimaryDimensions }}</el-tag>
            <el-tag type="warning">二级维度: {{ parsedData.statistics.totalSecondaryDimensions }}</el-tag>
          </div>
        </div>
      </template>

      <!-- 按层级显示Tab -->
      <el-tabs v-model="activeLevelName" type="border-card" @tab-change="handleLevelChange">
        <el-tab-pane
          v-for="level in parsedData.levels"
          :key="level.name"
          :label="level.name"
          :name="level.name"
        >
          <!-- 层级描述 -->
          <el-alert
            v-if="level.description"
            :title="level.description"
            type="info"
            :closable="true"
            style="margin-bottom: 16px"
            show-icon
          />

          <!-- 一级维度列表 -->
          <div class="primary-section">
            <!-- 一级维度卡片网格 -->
            <el-row :gutter="16">
              <el-col
                v-for="(primary, index) in level.primaryDimensions"
                :key="index"
                :span="primarySpan"
              >
                <el-card class="primary-card" shadow="hover">
                  <template #header>
                    <div class="primary-header">
                      <span class="primary-name">{{ primary.name }}</span>
                      <div class="primary-actions">
                        <el-tag size="small" type="primary">
                          {{ primary.secondaryDimensions?.length || 0 }}项
                        </el-tag>
                      </div>
                    </div>
                  </template>

                  <!-- 二级维度列表 -->
                  <div class="secondary-list">
                    <div
                      v-for="sec in primary.secondaryDimensions"
                      :key="sec.code"
                      class="secondary-item"
                    >
                      <span class="sec-name">{{ sec.name }}</span>
                      <div class="sec-tags">
                        <el-tag size="small" type="info">{{ sec.code }}</el-tag>
                        <el-tag 
                          size="small" 
                          :type="sec.metricType === 'QUANTITATIVE' ? 'success' : 'warning'"
                        >
                          {{ sec.metricType === 'QUANTITATIVE' ? '定量' : '定性' }}
                        </el-tag>
                      </div>
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </el-tab-pane>
      </el-tabs>

      <!-- 保存按钮 -->
      <div class="action-bar">
        <el-button type="success" size="large" @click="handleSave">
          <el-icon><Check /></el-icon>
          保存到数据库
        </el-button>
        <el-button size="large" @click="handleReset">
          <el-icon><RefreshRight /></el-icon>
          重新导入
        </el-button>
      </div>
    </el-card>

    <!-- 保存成功提示 -->
    <el-result
      v-if="showSuccessResult"
      icon="success"
      title="保存成功"
      sub-title="指标体系已保存到数据库"
    >
      <template #extra>
        <el-button type="primary" @click="handleContinue">
          继续编辑
        </el-button>
        <el-button @click="$router.push('/simulation-training/home')">
          返回首页
        </el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Setting,
  Upload,
  Document,
  DataAnalysis,
  TrendCharts,
  Check,
  RefreshRight,
  Delete
} from '@element-plus/icons-vue'
import ECharts from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, PieChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'

import * as dynamicApi from '@/api/index'

use([
  CanvasRenderer,
  BarChart,
  PieChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

// 状态
const templates = ref([])
const selectedTemplateId = ref(null)
const currentFile = ref(null)
const templateName = ref('')
const parsedData = ref(null)
const activeLevelName = ref('')
const uploadRef = ref(null)

// 保存成功
const showSuccessResult = ref(false)

// 计算属性
const primarySpan = computed(() => {
  const level = parsedData.value?.levels?.find(l => l.name === activeLevelName.value)
  const count = level?.primaryDimensions?.length || 1
  return Math.min(12, Math.max(6, Math.floor(24 / count)))
})

// 方法
const triggerUpload = () => {
  document.querySelector('.el-upload__input')?.click()
}

const handleFileChange = (file) => {
  currentFile.value = file.raw
  // 自动设置模板名
  if (!templateName.value) {
    templateName.value = file.name.replace(/\.(xlsx|xls)$/i, '')
  }
}

const handleFileRemove = () => {
  currentFile.value = null
  parsedData.value = null
}

const handleParse = async () => {
  if (!currentFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  try {
    const formData = new FormData()
    formData.append('file', currentFile.value)

    const response = await dynamicApi.parseExcelIndicators(currentFile.value)
    parsedData.value = response

    if (response.levels?.length > 0) {
      activeLevelName.value = response.levels[0].name
      ElMessage.success(`解析成功：${response.statistics.levelCount}个层级，${response.statistics.totalPrimaryDimensions}个一级维度，${response.statistics.totalSecondaryDimensions}个二级维度`)
    } else {
      ElMessage.warning('未解析到任何指标数据，请检查Excel格式')
    }
  } catch (error) {
    ElMessage.error('解析失败: ' + (error.message || '未知错误'))
    console.error(error)
  }
}

const handleLevelChange = (levelName) => {
  activeLevelName.value = levelName
}

const handleTemplateChange = async (templateId) => {
  try {
    const tree = await dynamicApi.getIndicatorTree(templateId)
    // 转换为解析结果的格式
    parsedData.value = convertTreeToParsedData(tree)
    if (parsedData.value.levels?.length > 0) {
      activeLevelName.value = parsedData.value.levels[0].name
    }
    ElMessage.success('已加载模板')
  } catch (error) {
    ElMessage.error('加载模板失败: ' + error.message)
  }
}

const handleDeleteTemplate = async () => {
  if (!selectedTemplateId.value) {
    ElMessage.warning('请先选择一个模板')
    return
  }

  try {
    await ElMessageBox.confirm(
      '确定要删除该模板吗？删除后无法恢复。',
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    await dynamicApi.deleteTemplate(selectedTemplateId.value)
    ElMessage.success('删除成功')
    selectedTemplateId.value = null
    parsedData.value = null
    loadTemplates()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + error.message)
    }
  }
}

const convertTreeToParsedData = (tree) => {
  return {
    levels: tree.levels || [],
    statistics: tree.statistics || {}
  }
}

const handleSave = async () => {
  if (!currentFile.value) {
    ElMessage.warning('请先导入Excel文件')
    return
  }

  if (!templateName.value) {
    ElMessage.warning('请输入模板名称')
    return
  }

  try {
    await dynamicApi.importIndicators(currentFile.value, templateName.value)
    showSuccessResult.value = true
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error('保存失败: ' + error.message)
  }
}

const handleReset = () => {
  currentFile.value = null
  templateName.value = ''
  parsedData.value = null
  activeLevelName.value = ''
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
}

const handleContinue = () => {
  showSuccessResult.value = false
}

// 加载模板列表
const loadTemplates = async () => {
  try {
    const list = await dynamicApi.getIndicatorTemplates()
    templates.value = list || []
  } catch (error) {
    console.error('加载模板失败:', error)
  }
}

// 生命周期
onMounted(() => {
  loadTemplates()
})
</script>

<style scoped lang="scss">
.dynamic-indicator-view {
  padding: 20px;
  max-width: 1400px;
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

.import-card,
.result-card {
  margin-bottom: 20px;

  :deep(.el-card__header) {
    padding: 12px 20px;
    background: #f5f7fa;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  > span {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 600;
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin: 16px 0;

  .el-icon {
    font-size: 24px;
    color: #409EFF;
  }

  span {
    flex: 1;
  }
}

.format-tip {
  margin-top: 16px;
}

.format-list {
  margin: 8px 0 0 0;
  padding-left: 20px;

  li {
    margin-bottom: 4px;
    line-height: 1.6;
  }
}

.template-name-input {
  margin-top: 16px;
}

.primary-section {
  margin-top: 16px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  font-weight: 600;
  color: #303133;

  .header-actions {
    display: flex;
    gap: 10px;
  }
}

.primary-card {
  margin-bottom: 16px;

  .primary-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .primary-name {
      font-weight: 600;
      color: #001f3f;
    }

    .primary-actions {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  .secondary-list {
    max-height: 300px;
    overflow-y: auto;
  }

  .secondary-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    margin-bottom: 6px;
    background: #f5f7fa;
    border-radius: 4px;
    font-size: 13px;

    .sec-name {
      flex: 1;
    }

    .sec-tags {
      display: flex;
      gap: 6px;
      align-items: center;
    }
  }

  .primary-weight-badge {
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px dashed #dcdfe6;
    text-align: center;
  }
}

.action-bar {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}
</style>
