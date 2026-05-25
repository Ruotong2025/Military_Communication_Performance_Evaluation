<template>
  <div class="indicator-identification-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2>
        <el-icon><Cpu /></el-icon>
        指标智能识别
      </h2>
      <p class="subtitle">
        输入指标名称，智能匹配相似指标
      </p>
    </div>

    <!-- 输入区域 -->
    <el-card class="input-card" shadow="hover">
      <template #header>
        <span><el-icon><Edit /></el-icon> 输入指标</span>
      </template>

      <el-space wrap>
        <el-input
          v-model="indicatorName"
          placeholder="请输入指标名称，如：信干噪比、误码率"
          clearable
          style="width: 300px"
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

    <!-- 结果展示 -->
    <div v-if="queryResult" class="result-container">

      <!-- 选择指标列表 -->
      <el-card class="result-card" shadow="hover">
        <template #header>
          <span><el-icon><List /></el-icon> 选择指标</span>
        </template>

        <el-radio-group v-model="selectedOption" style="display: flex; flex-direction: column; gap: 8px;">

          <!-- 相似度候选 -->
          <el-radio
            v-for="candidate in queryResult.candidates"
            :key="candidate.id"
            :value="'candidate-' + candidate.id"
            class="option-item"
            @change="selectCandidate(candidate)"
          >
            <div class="option-simple">
              <span class="option-name">{{ candidate.name }}</span>
              <el-tag size="small" :type="candidate.indicatorType === 'QUANTITATIVE' ? 'success' : 'warning'">
                {{ candidate.indicatorType === 'QUANTITATIVE' ? '定量' : '定性' }}
              </el-tag>
              <span class="unit" v-if="candidate.unit">{{ candidate.unit }}</span>
              <el-tag type="info" size="small" class="similarity-tag">
                {{ (candidate.similarity * 100).toFixed(1) }}%
              </el-tag>
            </div>
          </el-radio>

          <!-- API分析选项：只有语义相似（非100%匹配）时才显示 -->
          <el-radio
            v-if="!isExactMatch"
            value="api"
            class="option-item api-option"
            :disabled="apiLoading"
            @click.native="selectApi"
          >
            <div class="option-simple">
              <span class="option-name">使用API分析</span>
              <el-tag type="primary" size="small">
                <span v-if="apiLoading">分析中...</span>
                <span v-else-if="apiResult">置信度 {{ (apiResult.confidence * 100).toFixed(0) }}%</span>
                <span v-else>点击分析</span>
              </el-tag>
            </div>
          </el-radio>

        </el-radio-group>

        <!-- 无匹配时提示 -->
        <el-empty v-if="queryResult.candidates?.length === 0" description="未找到相似指标，请使用API分析" />
      </el-card>

      <!-- 选中详情预览 -->
      <el-card class="detail-card" shadow="hover">
        <template #header>
          <span><el-icon><InfoFilled /></el-icon> 选中详情</span>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="指标名称" :span="2">
            {{ selectedCandidate?.name || apiResult?.indicatorName || '-' }}
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
          <el-descriptions-item v-if="currentSourceData.length > 0" label="数据源" :span="2">
            <el-space wrap>
              <el-tag
                v-for="(data, idx) in currentSourceData"
                :key="idx"
                size="small"
                type="info"
              >
                {{ data.sourceDataName }}
                <span v-if="data.formulaSymbol" class="symbol">({{ data.formulaSymbol }})</span>
                <span v-if="data.unit" class="unit-info">[{{ data.unit }}]</span>
                <span v-if="data.dataType" class="type-info">[{{ data.dataType }}]</span>
              </el-tag>
            </el-space>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <el-button
          type="success"
          :disabled="!selectedOption"
          @click="handleConfirm"
        >
          <el-icon><Check /></el-icon>
          确认选择
        </el-button>
        <el-button @click="handleReset">
          <el-icon><RefreshLeft /></el-icon>
          重置
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { intelligentQuery, analyzeSingleIndicator, saveIndicatorSelection } from '@/api'
import {
  Cpu,
  Edit,
  Search,
  Connection,
  List,
  Check,
  RefreshLeft,
  InfoFilled
} from '@element-plus/icons-vue'

// 状态
const indicatorName = ref('')
const queryLoading = ref(false)
const apiLoading = ref(false)

// 查询结果
const queryResult = ref(null)
const apiResult = ref(null)

// 选择状态
const selectedOption = ref('')
const selectedCandidate = ref(null)

// 选择候选指标
const selectCandidate = (candidate) => {
  selectedOption.value = 'candidate-' + candidate.id
  selectedCandidate.value = candidate
}

// 选择API分析
const selectApi = () => {
  selectedOption.value = 'api'
  selectedCandidate.value = null
  handleApiAnalyze()
}

// 计算属性：当前详情
const currentIndicatorType = computed(() => {
  if (selectedOption.value === 'api') {
    return apiResult.value?.indicatorType
  }
  return selectedCandidate.value?.indicatorType
})

const currentUnit = computed(() => {
  if (selectedOption.value === 'api') {
    return apiResult.value?.unit
  }
  return selectedCandidate.value?.unit
})

const currentFormula = computed(() => {
  if (selectedOption.value === 'api') {
    return apiResult.value?.formula
  }
  return selectedCandidate.value?.formula
})

const currentFormulaDescription = computed(() => {
  if (selectedOption.value === 'api') {
    return apiResult.value?.formulaDescription
  }
  return selectedCandidate.value?.formulaDescription
})

const currentCalculationMethod = computed(() => {
  if (selectedOption.value === 'api') {
    return apiResult.value?.calculationMethod
  }
  return selectedCandidate.value?.calculationMethod
})

const currentDescription = computed(() => {
  if (selectedOption.value === 'api') {
    return apiResult.value?.description
  }
  return selectedCandidate.value?.description
})

const currentSourceData = computed(() => {
  if (selectedOption.value === 'api') {
    return apiResult.value?.sourceDataList || []
  }
  return selectedCandidate.value?.sourceDataList || []
})

// 是否是精确匹配（相似度100%）
const isExactMatch = computed(() => {
  return queryResult.value?.candidates?.some(c => c.similarity === 1.0) || false
})

// 查询
const handleQuery = async () => {
  if (!indicatorName.value.trim()) {
    ElMessage.warning('请输入指标名称')
    return
  }

  queryLoading.value = true
  queryResult.value = null
  apiResult.value = null
  selectedOption.value = ''
  selectedCandidate.value = null

  try {
    const result = await intelligentQuery({ indicatorName: indicatorName.value.trim() })
    queryResult.value = result

    if (result?.candidates?.length > 0) {
      selectedCandidate.value = result.candidates[0]
      selectedOption.value = 'candidate-' + result.candidates[0].id
    }

    ElMessage.success(`查询完成，匹配 ${result?.candidates?.length || 0} 个结果`)
  } catch (error) {
    console.error('查询失败:', error)
    ElMessage.error('查询失败: ' + (error.message || '未知错误'))
  } finally {
    queryLoading.value = false
  }
}

// API分析
const handleApiAnalyze = async () => {
  if (!indicatorName.value.trim()) {
    ElMessage.warning('请输入指标名称')
    selectedOption.value = ''
    return
  }

  apiLoading.value = true

  try {
    const result = await analyzeSingleIndicator({ indicatorName: indicatorName.value.trim() })
    apiResult.value = result
    ElMessage.success('API分析完成')
  } catch (error) {
    console.error('API分析失败:', error)
    ElMessage.error('API分析失败: ' + (error.message || '未知错误'))
    selectedOption.value = selectedCandidate.value ? 'candidate-' + selectedCandidate.value.id : ''
  } finally {
    apiLoading.value = false
  }
}

// 确认选择
const handleConfirm = async () => {
  if (!selectedOption.value) {
    ElMessage.warning('请先选择一个指标')
    return
  }

  try {
    let request = {
      originalName: indicatorName.value.trim(),
      selectedSource: selectedOption.value === 'api' ? 'API' : 'DATABASE'
    }

    if (selectedOption.value === 'api') {
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

    await saveIndicatorSelection(request)
    ElMessage.success('保存成功')
    handleReset()
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败: ' + (error.message || '未知错误'))
  }
}

// 重置
const handleReset = () => {
  indicatorName.value = ''
  queryResult.value = null
  apiResult.value = null
  selectedOption.value = ''
  selectedCandidate.value = null
}
</script>

<style scoped>
.indicator-identification-page {
  padding: 20px;
  max-width: 800px;
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

.result-card {
  border-left: 4px solid #409eff;
}

.detail-card {
  border-left: 4px solid #67c23a;
}

.option-item {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  margin-right: 0;
}

.option-item:hover {
  background: #f5f7fa;
}

.api-option {
  border-left: 3px solid #409eff;
}

.option-simple {
  display: flex;
  align-items: center;
  gap: 10px;
}

.option-name {
  font-weight: bold;
  min-width: 120px;
}

.unit {
  color: #909399;
  font-size: 13px;
}

.similarity-tag {
  margin-left: auto;
}

.symbol {
  color: #409eff;
  font-size: 11px;
}

.unit-info {
  color: #67c23a;
  font-size: 11px;
  margin-left: 4px;
}

.type-info {
  color: #e6a23c;
  font-size: 11px;
  margin-left: 4px;
}

.formula-code {
  background: #f5f7fa;
  padding: 4px 8px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  color: #409eff;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding: 16px 0;
}
</style>
