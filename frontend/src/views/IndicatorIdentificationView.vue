<template>
  <div class="indicator-identification-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2>
        <el-icon><Cpu /></el-icon>
        指标智能识别
      </h2>
      <p class="subtitle">
        输入指标名称，智能匹配相似指标及数据源字段映射
      </p>
    </div>

    <!-- 第一部分：输入区域 -->
    <el-card class="input-card" shadow="hover">
      <template #header>
        <span><el-icon><Edit /></el-icon> 输入指标名称</span>
      </template>

      <el-space wrap>
        <el-input
          v-model="indicatorName"
          placeholder="请输入指标名称，如：成功传输的数据量、信干噪比"
          clearable
          style="width: 400px"
          @keyup.enter="handleQuery"
        />
        <el-button
          type="primary"
          :loading="queryLoading"
          @click="handleQuery"
          :disabled="!indicatorName.trim()"
        >
          <el-icon><Search /></el-icon>
          查询
        </el-button>
      </el-space>
    </el-card>

    <!-- 结果展示区域 -->
    <div v-if="queryResult" class="result-container">

      <!-- ========================================= -->
      <!-- 第一部分：指标选择 -->
      <!-- ========================================= -->
      <el-card class="indicator-selection-card" shadow="hover">
        <template #header>
          <span><el-icon><Connection /></el-icon> 指标匹配</span>
          <el-tag v-if="selectedIndicator" type="success" size="small" style="margin-left: 12px;">
            已选择
          </el-tag>
        </template>

        <el-radio-group v-model="selectedIndicatorId" style="display: flex; flex-direction: column; gap: 12px;">
          <!-- 数据库候选指标 -->
          <div
            v-for="candidate in queryResult.candidates"
            :key="'db-' + candidate.id"
            class="indicator-option"
            :class="{ 'is-selected': selectedIndicatorId === 'db-' + candidate.id }"
            @click="selectDbIndicator(candidate)"
          >
            <el-radio :value="'db-' + candidate.id">
              <div class="option-content">
                <div class="option-main">
                  <span class="option-name">{{ candidate.name }}</span>
                  <el-tag
                    size="small"
                    :type="candidate.indicatorType === 'QUANTITATIVE' ? 'success' : 'warning'"
                  >
                    {{ candidate.indicatorType === 'QUANTITATIVE' ? '定量' : '定性' }}
                  </el-tag>
                  <span class="unit" v-if="candidate.unit">{{ candidate.unit }}</span>
                </div>
                <div class="option-meta">
                  <span class="similarity">
                    <el-icon><Star /></el-icon>
                    {{ (candidate.similarity * 100).toFixed(0) }}% 相似度
                  </span>
                  <span class="description" v-if="candidate.description">
                    {{ candidate.description }}
                  </span>
                </div>
              </div>
            </el-radio>
          </div>

          <!-- API 分析选项 -->
          <div
            v-if="!isExactMatch"
            class="indicator-option api-option"
            :class="{ 'is-selected': selectedIndicatorId === 'api' }"
            @click="selectApiIndicator"
          >
            <el-radio :value="'api'">
              <div class="option-content">
                <div class="option-main">
                  <span class="option-name">使用 API 分析</span>
                  <el-tag type="primary" size="small">
                    <span v-if="apiLoading">分析中...</span>
                    <span v-else-if="apiResult">置信度 {{ (apiResult.confidence * 100).toFixed(0) }}%</span>
                    <span v-else>点击分析</span>
                  </el-tag>
                </div>
                <div class="option-meta">
                  <span class="similarity">
                    <el-icon><Star /></el-icon>
                    AI 分析
                  </span>
                  <span class="description">
                    未找到精确匹配，使用 AI 分析识别指标定义
                  </span>
                </div>
              </div>
            </el-radio>
          </div>
        </el-radio-group>

        <!-- 无匹配提示 -->
        <el-empty
          v-if="queryResult.candidates?.length === 0 && !apiLoading"
          description="未找到相似指标，请使用API分析"
        />
      </el-card>

      <!-- ========================================= -->
      <!-- 第二部分：数据源字段映射 - 仅在选择API分析时显示 -->
      <!-- ========================================= -->
      <el-card v-if="showSourceDataMapping" class="source-data-card" shadow="hover">
        <template #header>
          <span><el-icon><Document /></el-icon> 数据源字段映射</span>
          <span class="header-tip">（每个数据源需选择字段来源）</span>
        </template>

        <div v-if="sourceDataLoading" class="loading-container">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>正在加载数据源信息...</span>
        </div>

        <div v-else-if="currentSourceDataList.length > 0" class="source-data-list">
          <SourceDataFieldMapper
            v-for="(sourceData, index) in currentSourceDataList"
            :key="index"
            :ref="el => setSourceDataRef(el, index)"
            :source-data="sourceData"
            :api-suggestion="getApiSuggestion(sourceData)"
            :selection-type="getSourceDataSelectionType(sourceData)"
            :selected-db-field="getSourceDataSelectedField(sourceData)"
            :related-source-data-id="getRelatedSourceDataId(sourceData)"
            @change="handleSourceDataChange($event, index)"
          />

            <!-- 映射汇总 -->
            <div class="mapping-summary">
              <div class="summary-title">
                <el-icon><InfoFilled /></el-icon>
                字段映射汇总
              </div>
              <div class="summary-content">
                <div
                  v-for="(mapping, index) in sourceDataMappings"
                  :key="index"
                  class="summary-item"
                >
                  <span class="summary-source">{{ mapping.sourceDataName }}</span>
                  <span class="summary-arrow">→</span>
                  <span class="summary-target">
                    {{ mapping.selectedField }}
                    <el-tag type="info" size="small">
                      {{ mapping.selectionType === 'API_RECOMMENDED' ? 'API建议' : '数据库已有' }}
                    </el-tag>
                  </span>
                </div>
              </div>
            </div>
        </div>

        <el-empty v-else description="暂无数据源信息" />
      </el-card>

      <!-- ========================================= -->
      <!-- 第三部分：指标详情预览 -->
      <!-- ========================================= -->
      <el-card v-if="selectedIndicator || apiResult" class="detail-card" shadow="hover">
        <template #header>
          <span><el-icon><InfoFilled /></el-icon> 指标详情预览</span>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="指标名称" :span="2">
            {{ currentIndicatorName }}
          </el-descriptions-item>
          <el-descriptions-item label="指标类型">
            <el-tag :type="currentIndicatorType === 'QUANTITATIVE' ? 'success' : 'warning'" size="small">
              {{ currentIndicatorType === 'QUANTITATIVE' ? '定量指标' : '定性指标' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="单位">
            {{ currentUnit || '-' }}
          </el-descriptions-item>
          <el-descriptions-item v-if="currentFormula" label="计算公式" :span="2">
            <code class="formula-code">{{ currentFormula }}</code>
          </el-descriptions-item>
          <el-descriptions-item v-if="currentFormulaDescription" label="公式说明" :span="2">
            {{ currentFormulaDescription }}
          </el-descriptions-item>
          <el-descriptions-item v-if="currentCalculationMethod" label="计算方法" :span="2">
            {{ currentCalculationMethod }}
          </el-descriptions-item>
          <el-descriptions-item v-if="currentDescription" label="描述" :span="2">
            {{ currentDescription }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <el-button
          type="success"
          size="large"
          :disabled="!canConfirm"
          :loading="confirmLoading"
          @click="handleConfirm"
        >
          <el-icon><Check /></el-icon>
          确认选择
        </el-button>
        <el-button size="large" @click="handleReset">
          <el-icon><RefreshLeft /></el-icon>
          重置
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { intelligentQuery, analyzeSingleIndicator, saveIndicatorSelection, searchColumnSuggestions } from '@/api'
import SourceDataFieldMapper from '@/components/SourceDataFieldMapper.vue'
import {
  Cpu,
  Edit,
  Search,
  Connection,
  Document,
  Check,
  RefreshLeft,
  InfoFilled,
  Star,
  Loading
} from '@element-plus/icons-vue'

// 状态
const indicatorName = ref('')
const queryLoading = ref(false)
const apiLoading = ref(false)
const confirmLoading = ref(false)
const sourceDataLoading = ref(false)

// 查询结果
const queryResult = ref(null)
const apiResult = ref(null)

// 选择状态
const selectedIndicatorId = ref('')
const selectedCandidate = ref(null)

// 数据源映射状态
const sourceDataRefs = ref([])
const sourceDataMappings = ref([])
const dbFieldSuggestions = ref({}) // { sourceDataName: [] }

// 计算属性
const isExactMatch = computed(() => {
  return queryResult.value?.candidates?.some(c => c.similarity === 1.0) || false
})

const currentIndicatorName = computed(() => {
  if (selectedIndicatorId.value === 'api') {
    return apiResult.value?.indicatorName || indicatorName.value
  }
  return selectedCandidate.value?.name || '-'
})

const currentIndicatorType = computed(() => {
  if (selectedIndicatorId.value === 'api') {
    return apiResult.value?.indicatorType
  }
  return selectedCandidate.value?.indicatorType
})

const currentUnit = computed(() => {
  if (selectedIndicatorId.value === 'api') {
    return apiResult.value?.unit
  }
  return selectedCandidate.value?.unit
})

const currentFormula = computed(() => {
  if (selectedIndicatorId.value === 'api') {
    return apiResult.value?.formula
  }
  return selectedCandidate.value?.formula
})

const currentFormulaDescription = computed(() => {
  if (selectedIndicatorId.value === 'api') {
    return apiResult.value?.formulaDescription
  }
  return selectedCandidate.value?.formulaDescription
})

const currentCalculationMethod = computed(() => {
  if (selectedIndicatorId.value === 'api') {
    return apiResult.value?.calculationMethod
  }
  return selectedCandidate.value?.calculationMethod
})

const currentDescription = computed(() => {
  if (selectedIndicatorId.value === 'api') {
    return apiResult.value?.description
  }
  return selectedCandidate.value?.description
})

const selectedIndicator = computed(() => {
  return selectedCandidate.value
})

const currentSourceDataList = computed(() => {
  if (selectedIndicatorId.value === 'api') {
    return apiResult.value?.sourceDataList || []
  }
  return selectedCandidate.value?.sourceDataList || []
})

// 是否显示数据源字段映射
const showSourceDataMapping = computed(() => {
  return selectedIndicatorId.value === 'api' && currentSourceDataList.value.length > 0
})

// 是否可以确认
const canConfirm = computed(() => {
  if (!selectedIndicatorId.value) return false
  // 只有选择API分析时才检查映射
  if (selectedIndicatorId.value === 'api') {
    if (sourceDataMappings.value.length === 0) return false
    return sourceDataMappings.value.every(m => m.selectedField)
  }
  // 选择数据库指标，直接可确认
  return true
})

// 方法
const setSourceDataRef = (el, index) => {
  if (el) {
    sourceDataRefs.value[index] = el
  }
}

const selectDbIndicator = (candidate) => {
  selectedIndicatorId.value = 'db-' + candidate.id
  selectedCandidate.value = candidate
  // 加载该指标的数据库字段建议
  loadDbFieldSuggestions()
}

const selectApiIndicator = () => {
  selectedIndicatorId.value = 'api'
  selectedCandidate.value = null
  handleApiAnalyze()
}

const handleApiAnalyze = async () => {
  if (!indicatorName.value.trim()) {
    ElMessage.warning('请输入指标名称')
    return
  }

  apiLoading.value = true

  try {
    const result = await analyzeSingleIndicator({ indicatorName: indicatorName.value.trim() })
    console.log(`[调试] API分析结果:`, result)
    console.log(`[调试] result 类型:`, typeof result)
    console.log(`[调试] result.sourceDataList:`, result?.sourceDataList)
    console.log(`[调试] result.sourceDataList 长度:`, result?.sourceDataList?.length)

    // 设置 apiResult
    apiResult.value = result
    console.log(`[调试] apiResult.value 设置后:`, apiResult.value)
    console.log(`[调试] apiResult.value.sourceDataList:`, apiResult.value?.sourceDataList)

    ElMessage.success('API分析完成')

    // 重要：设置 selectedIndicatorId 为 'api'，这样 currentSourceDataList 才能返回正确的数据
    selectedIndicatorId.value = 'api'
    console.log(`[调试] selectedIndicatorId 设置为 'api'`)

    // API 分析完成后也加载字段建议
    console.log(`[调试] 准备调用 loadDbFieldSuggestions`)
    console.log(`[调试] selectedIndicatorId.value:`, selectedIndicatorId.value)
    console.log(`[调试] currentSourceDataList.value:`, currentSourceDataList.value)

    loadDbFieldSuggestions()
  } catch (error) {
    console.error('API分析失败:', error)
    ElMessage.error('API分析失败: ' + (error.message || '未知错误'))
  } finally {
    apiLoading.value = false
  }
}

// 加载数据库字段建议
const loadDbFieldSuggestions = async () => {
  console.log('[调试] loadDbFieldSuggestions 开始')
  console.log('[调试] currentSourceDataList:', currentSourceDataList.value)
  console.log('[调试] currentSourceDataList.length:', currentSourceDataList.value.length)

  if (currentSourceDataList.value.length === 0) {
    console.warn('[调试] currentSourceDataList 为空，跳过加载')
    return
  }

  sourceDataLoading.value = true
  try {
    for (const sourceData of currentSourceDataList.value) {
      // 添加空值检查
      if (!sourceData?.sourceDataName) {
        console.warn('[调试] 跳过空数据源:', sourceData)
        continue
      }
      console.log(`[调试] 搜索字段建议: ${sourceData.sourceDataName}`)
      // 注意：响应拦截器已经自动解包，所以 res 就是数组
      const res = await searchColumnSuggestions(sourceData.sourceDataName, 5)
      console.log(`[调试] 返回结果 (res):`, res)
      console.log(`[调试] res 是否为数组:`, Array.isArray(res))
      if (Array.isArray(res)) {
        dbFieldSuggestions.value[sourceData.sourceDataName] = res
      }
    }
    console.log(`[调试] 所有字段建议:`, dbFieldSuggestions.value)
    console.log(`[调试] sourceDataMappings 初始化前:`, sourceDataMappings.value)
    // 初始化映射状态
    initSourceDataMappings()
    console.log(`[调试] sourceDataMappings 初始化后:`, sourceDataMappings.value)
  } catch (error) {
    console.error('加载字段建议失败:', error)
  } finally {
    sourceDataLoading.value = false
  }
}

// 初始化数据源映射
const initSourceDataMappings = () => {
  console.log('[调试] initSourceDataMappings 开始')
  console.log('[调试] currentSourceDataList.value:', currentSourceDataList.value)
  console.log('[调试] dbFieldSuggestions.value:', dbFieldSuggestions.value)
  console.log('[调试] apiResult.value:', apiResult.value)

  sourceDataMappings.value = currentSourceDataList.value.map(sourceData => {
    console.log('[调试] 处理 sourceData:', sourceData.sourceDataName)
    const suggestions = dbFieldSuggestions.value[sourceData.sourceDataName] || []
    console.log('[调试] suggestions for', sourceData.sourceDataName, ':', suggestions)

    // 从 API 结果中查找对应的 sourceData 的建议
    let apiSuggestionField = null
    if (apiResult.value?.sourceDataList) {
      const apiSourceData = apiResult.value.sourceDataList.find(
        s => s.sourceDataName === sourceData.sourceDataName
      )
      console.log('[调试] apiSourceData found:', apiSourceData)
      if (apiSourceData) {
        // 搜索该数据源名称对应的数据库字段
        const matched = suggestions.length > 0 ? suggestions[0] : null
        apiSuggestionField = matched ? {
          columnName: matched.columnName,
          columnLabel: matched.columnLabel,
          tableLabel: matched.tableLabel,
          similarity: matched.similarity || 0.95,
          relatedSourceDataId: matched.relatedSourceDataId || null
        } : {
          columnName: apiSourceData.sourceDataName,
          columnLabel: apiSourceData.sourceDataName,
          tableLabel: 'API建议',
          similarity: 0.8,
          relatedSourceDataId: null
        }
      }
    }

    // 默认选择数据库已有（如果找到匹配）
    const hasDbMatch = suggestions.length > 0
    const mapping = {
      sourceData: sourceData,
      sourceDataName: sourceData.sourceDataName,
      formulaSymbol: sourceData.formulaSymbol,
      unit: sourceData.unit,
      dataType: sourceData.dataType,
      selectionType: hasDbMatch ? 'EXISTING_DATABASE' : 'API_RECOMMENDED',
      selectedField: hasDbMatch ? suggestions[0].columnName : (apiSuggestionField?.columnName || ''),
      relatedSourceDataId: hasDbMatch ? (suggestions[0].relatedSourceDataId || null) : null,
      apiSuggestion: apiSuggestionField,
      dbSuggestions: suggestions
    }
    console.log('[调试] 生成的 mapping:', mapping)
    return mapping
  })
  console.log('[调试] sourceDataMappings 最终结果:', sourceDataMappings.value)
}

// 获取 API 建议
const getApiSuggestion = (sourceData) => {
  const mapping = sourceDataMappings.value.find(m => m.sourceDataName === sourceData.sourceDataName)
  return mapping?.apiSuggestion || null
}

// 获取数据源选择类型
const getSourceDataSelectionType = (sourceData) => {
  const mapping = sourceDataMappings.value.find(m => m.sourceDataName === sourceData.sourceDataName)
  return mapping?.selectionType || 'api'
}

// 获取数据源选中的字段
const getSourceDataSelectedField = (sourceData) => {
  const mapping = sourceDataMappings.value.find(m => m.sourceDataName === sourceData.sourceDataName)
  return mapping?.selectedField || ''
}

// 获取关联的数据源ID
const getRelatedSourceDataId = (sourceData) => {
  const mapping = sourceDataMappings.value.find(m => m.sourceDataName === sourceData.sourceDataName)
  return mapping?.relatedSourceDataId || null
}

// 处理数据源变化
const handleSourceDataChange = (event, index) => {
  if (sourceDataMappings.value[index]) {
    sourceDataMappings.value[index] = {
      ...sourceDataMappings.value[index],
      selectionType: event.selectionType,
      selectedField: event.selectedField,
      relatedSourceDataId: event.relatedSourceDataId
    }
  }
}

// 查询
const handleQuery = async () => {
  if (!indicatorName.value.trim()) {
    ElMessage.warning('请输入指标名称')
    return
  }

  queryLoading.value = true
  queryResult.value = null
  apiResult.value = null
  selectedIndicatorId.value = ''
  selectedCandidate.value = null
  sourceDataMappings.value = []
  dbFieldSuggestions.value = {}

  try {
    const result = await intelligentQuery({ indicatorName: indicatorName.value.trim() })
    queryResult.value = result

    // 自动选中第一个候选
    if (result?.candidates?.length > 0) {
      const firstCandidate = result.candidates[0]
      selectDbIndicator(firstCandidate)
    }

    ElMessage.success(`查询完成，匹配 ${result?.candidates?.length || 0} 个结果`)
  } catch (error) {
    console.error('查询失败:', error)
    ElMessage.error('查询失败: ' + (error.message || '未知错误'))
  } finally {
    queryLoading.value = false
  }
}

// 确认选择
const handleConfirm = async () => {
  if (!selectedIndicatorId.value) {
    ElMessage.warning('请先选择一个指标')
    return
  }

  confirmLoading.value = true

  try {
    let request = {
      originalName: indicatorName.value.trim(),
      selectedSource: selectedIndicatorId.value === 'api' ? 'API' : 'DATABASE',
      sourceDataMappings: sourceDataMappings.value.map(m => ({
        sourceDataName: m.sourceDataName,
        formulaSymbol: m.formulaSymbol || null,
        unit: m.unit || null,
        dataType: m.dataType || null,
        selectionType: m.selectionType,
        relatedSourceDataId: m.relatedSourceDataId || null,
        selectedField: m.selectedField || null
      }))
    }

    if (selectedIndicatorId.value === 'api') {
      request.indicatorType = apiResult.value?.indicatorType
      request.formula = apiResult.value?.formula
      request.formulaDescription = apiResult.value?.formulaDescription
      request.calculationMethod = apiResult.value?.calculationMethod
      request.unit = apiResult.value?.unit
      request.description = apiResult.value?.description
    } else if (selectedCandidate.value) {
      request.selectedDbId = selectedCandidate.value.id
      request.matchedIndicatorName = selectedCandidate.value.name
      request.matchSimilarity = selectedCandidate.value.similarity
    }

    console.log('[调试] 发送保存请求:', request)
    await saveIndicatorSelection(request)
    ElMessage.success('保存成功')
    handleReset()
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败: ' + (error.message || '未知错误'))
  } finally {
    confirmLoading.value = false
  }
}

// 重置
const handleReset = () => {
  indicatorName.value = ''
  queryResult.value = null
  apiResult.value = null
  selectedIndicatorId.value = ''
  selectedCandidate.value = null
  sourceDataMappings.value = []
  sourceDataRefs.value = []
  dbFieldSuggestions.value = {}
}

// 监听数据源列表变化，重新初始化映射
watch(currentSourceDataList, (newList) => {
  if (newList.length > 0 && Object.keys(dbFieldSuggestions.value).length > 0) {
    initSourceDataMappings()
  }
}, { immediate: true })
</script>

<style scoped>
.indicator-identification-page {
  padding: 20px;
  max-width: 900px;
  margin: 0 auto;
}

.page-header h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 8px 0;
  font-size: 20px;
}

.subtitle {
  color: #909399;
  margin: 0;
}

.input-card {
  margin-bottom: 20px;
}

.result-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 确保 radio-group 内的选项宽度一致 */
.el-radio-group {
  width: 100%;
}

.el-radio-group > .indicator-option {
  width: 100%;
  box-sizing: border-box;
}

/* 指标选择卡片 */
.indicator-selection-card {
  border-left: 4px solid #409eff;
}

.header-tip {
  color: #909399;
  font-size: 12px;
  font-weight: normal;
  margin-left: 8px;
}

.indicator-option {
  padding: 12px 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  width: 100%;
  box-sizing: border-box;
}

.indicator-option:hover {
  border-color: #c0d4ff;
  background: #f5f7fa;
}

.indicator-option.is-selected {
  border-color: #409eff;
  background: #ecf5ff;
}

.indicator-option.api-option {
  border-left: 3px solid #409eff;
}

.option-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.option-main {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  min-height: 24px;
}

.option-name {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
  flex-shrink: 0;
}

.unit {
  color: #909399;
  font-size: 13px;
  flex-shrink: 0;
}

.option-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: #606266;
  width: 100%;
  min-height: 24px;
}

.similarity {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #e6a23c;
  font-weight: 500;
  min-width: 120px;
  flex-shrink: 0;
}

.description {
  color: #909399;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

/* 确保指标选项内的内容对齐 */
.indicator-option .el-radio {
  display: flex;
  align-items: center;
  width: 100%;
}

.indicator-option .el-radio__input {
  flex-shrink: 0;
  width: 14px;
}

.indicator-option .el-radio__label {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.indicator-option .option-content {
  width: 100%;
  min-width: 0;
}

/* 数据源卡片 */
.source-data-card {
  border-left: 4px solid #67c23a;
}

.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px;
  color: #909399;
}

.source-data-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mapping-summary {
  background: #f5f7fa;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  padding: 12px 16px;
  margin-top: 16px;
}

.summary-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 10px;
}

.summary-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.summary-source {
  color: #409eff;
  font-weight: 500;
}

.summary-arrow {
  color: #909399;
}

.summary-target {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #67c23a;
}

/* 详情卡片 */
.detail-card {
  border-left: 4px solid #e6a23c;
}

.formula-code {
  background: #f5f7fa;
  padding: 4px 8px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  color: #409eff;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 20px 0;
}
</style>
