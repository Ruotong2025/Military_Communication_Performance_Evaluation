<template>
  <div class="indicator-identification-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2>
        <el-icon><Cpu /></el-icon>
        指标智能识别
      </h2>
      <p class="subtitle">
        上传指标名称，AI自动识别定性/定量类型、计算公式及可测得数据
      </p>
    </div>

    <!-- 输入区域 -->
    <el-card class="input-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><Edit /></el-icon> 输入指标</span>
          <el-button text size="small" @click="loadSample">
            <el-icon><Download /></el-icon>
            加载示例
          </el-button>
        </div>
      </template>

      <el-form label-width="100px">
        <el-form-item label="指标大类">
          <el-input
            v-model="formData.category"
            placeholder="如：通信性能指标、指控能力指标"
            clearable
            style="width: 300px"
          />
        </el-form-item>

        <el-form-item label="领域上下文">
          <el-input
            v-model="formData.domain"
            placeholder="如：军事通信、电子对抗（可选）"
            clearable
            style="width: 300px"
          />
        </el-form-item>

        <el-form-item label="指标列表">
          <el-input
            type="textarea"
            v-model="indicatorsText"
            :rows="12"
            placeholder="每行一个指标名称，如：
信干噪比
抗干扰余量
通信距离
误码率
传输时延
战术适用性
操作简便性"
          />
          <div class="textarea-tip">
            <el-icon><InfoFilled /></el-icon>
            当前 {{ indicatorCount }} 个指标（支持最多100个）
          </div>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            :disabled="indicatorCount === 0 || indicatorCount > 100"
            @click="handleIdentify"
          >
            <el-icon><Cpu /></el-icon>
            开始识别 ({{ indicatorCount }}个)
          </el-button>
          <el-button @click="handleReset">
            <el-icon><RefreshLeft /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 结果展示 -->
    <el-card v-if="result" class="result-card" shadow="hover">
      <template #header>
        <div class="result-header">
          <span>
            <el-icon><Finished /></el-icon>
            识别结果
          </span>
          <div class="result-stats">
            <el-tag type="success" size="large">定量 {{ result.statistics?.quantitativeCount || 0 }} 个</el-tag>
            <el-tag type="warning" size="large">定性 {{ result.statistics?.qualitativeCount || 0 }} 个</el-tag>

            <el-divider direction="vertical" />

            <el-tooltip content="直接从数据库返回，未调用API">
              <el-tag type="info" size="large">
                <el-icon><Collection /></el-icon>
                MySQL命中 {{ result.statistics?.hitFromDb || 0 }} 个
              </el-tag>
            </el-tooltip>

            <el-tooltip content="调用DeepSeek API进行识别">
              <el-tag type="primary" size="large">
                <el-icon><Cpu /></el-icon>
                API新识别 {{ result.statistics?.newFromApi || 0 }} 个
              </el-tag>
            </el-tooltip>

            <el-tag v-if="result.statistics?.hitFromDb > 0" type="success" size="large">
              节省约 {{ result.statistics?.hitFromDb * 50 || 0 }} Token
            </el-tag>

            <el-divider direction="vertical" />
            <el-tag type="info" size="large">耗时 {{ result.processingTimeMs }} ms</el-tag>
          </div>
        </div>
      </template>

      <!-- 指标详情列表 -->
      <div class="indicator-list">
        <el-card
          v-for="(item, index) in result.results"
          :key="index"
          class="indicator-item"
          shadow="hover"
        >
          <!-- 基本信息 -->
          <div class="indicator-header">
            <div class="indicator-title">
              <span class="indicator-name">{{ item.indicatorName }}</span>
              <el-tag
                :type="item.indicatorType === 'QUALITATIVE' ? 'warning' : 'success'"
                size="small"
              >
                {{ item.indicatorTypeDesc }}
              </el-tag>
              <el-tag v-if="!item.isNew" type="info" size="small">
                <el-icon><Collection /></el-icon>
                MySQL缓存
              </el-tag>
              <el-tag v-else type="success" size="small">
                <el-icon><Cpu /></el-icon>
                API新识别
              </el-tag>
            </div>
            <span v-if="item.confidence" class="confidence">
              置信度: {{ (item.confidence * 100).toFixed(0) }}%
            </span>
          </div>

          <!-- 定量指标 -->
          <template v-if="item.indicatorType === 'QUANTITATIVE'">
            <div v-if="item.formula" class="formula-section">
              <div class="section-label">
                <el-icon><Operation /></el-icon>
                计算公式
              </div>
              <div class="formula-box">
                <div class="formula-main">{{ item.formula }}</div>
                <div v-if="item.formulaDescription" class="formula-desc">
                  {{ item.formulaDescription }}
                </div>
              </div>
            </div>

            <div v-if="item.calculationMethod" class="calc-method">
              <span class="method-label">
                <el-icon><InfoFilled /></el-icon>
                计算方法：
              </span>
              {{ item.calculationMethod }}
            </div>

            <div
              v-if="item.formulaRelatedData && item.formulaRelatedData.length > 0"
              class="source-data-section"
            >
              <div class="section-label">
                <el-icon><DataAnalysis /></el-icon>
                公式数据源
              </div>
              <div class="source-data-tags">
                <el-tag
                  v-for="(data, idx) in item.formulaRelatedData"
                  :key="idx"
                  :type="data.isEssential ? 'primary' : 'info'"
                  size="default"
                  class="source-data-tag"
                  effect="light"
                >
                  <span class="data-content">
                    <span class="data-name">{{ data.dataName }}</span>
                    <span v-if="data.formulaSymbol" class="data-symbol">
                      ({{ data.formulaSymbol }})
                    </span>
                    <span v-if="data.unit" class="data-unit">
                      [{{ data.unit }}]
                    </span>
                  </span>
                </el-tag>
              </div>
            </div>

            <div v-if="item.unit" class="unit-info">
              <el-icon><InfoFilled /></el-icon>
              <span>指标单位: <strong>{{ item.unit }}</strong></span>
            </div>
          </template>

          <!-- 定性指标 -->
          <template v-else>
            <div class="qualitative-note">
              <el-icon><InfoFilled /></el-icon>
              <span>定性指标，需要专家主观评估</span>
            </div>
          </template>

          <!-- 错误信息 -->
          <div v-if="item.message && item.message.includes('失败')" class="error-message">
            <el-icon><CircleCloseFilled /></el-icon>
            {{ item.message }}
          </div>
        </el-card>
      </div>

      <!-- 优化提示 -->
      <el-alert
        v-if="result.statistics?.hitFromDb > 0"
        type="success"
        :closable="false"
        show-icon
        style="margin-top: 16px"
      >
        <template #title>
          <span>
            🎉 优化效果：{{ result.statistics?.hitFromDb }} 个指标已在数据库中，
            无需调用API，共节省约 {{ result.statistics?.hitFromDb * 50 }} Token
          </span>
        </template>
      </el-alert>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { batchParseIndicators } from '@/api'
import {
  Cpu,
  Edit,
  Download,
  RefreshLeft,
  Finished,
  InfoFilled,
  Operation,
  DataAnalysis,
  CircleCloseFilled,
  Collection
} from '@element-plus/icons-vue'

const formData = ref({
  category: '',
  domain: ''
})
const indicatorsText = ref('')
const loading = ref(false)
const result = ref(null)

const indicatorCount = computed(() => {
  return indicatorsText.value
    .split('\n')
    .filter(line => line.trim())
    .length
})

const loadSample = () => {
  formData.value.category = '通信性能指标'
  formData.value.domain = '军事通信'
  indicatorsText.value = `信干噪比
抗干扰余量
通信距离
误码率
传输时延
抖动
带宽利用率
频谱利用率
信号覆盖范围
战术适用性
操作简便性
系统可靠性`
}

const handleIdentify = async () => {
  const indicators = indicatorsText.value
    .split('\n')
    .map(line => line.trim())
    .filter(line => line)

  if (indicators.length === 0) {
    ElMessage.warning('请输入指标列表')
    return
  }

  if (indicators.length > 100) {
    ElMessage.warning('单次最多支持100个指标')
    return
  }

  loading.value = true
  result.value = null

  try {
    const response = await batchParseIndicators(indicators, {
      category: formData.value.category,
      domain: formData.value.domain
    })

    // batchParseIndicators 在 request.js 中被拦截器处理后返回的是 data
    // 如果识别成功，返回 IndicatorBatchParseResultDTO
    // 如果识别失败且 code != 200，会被 reject
    if (!response) {
      ElMessage.warning('识别完成，但返回数据为空')
      return
    }

    result.value = response

    // 尝试从结果中获取统计信息
    const stats = response.statistics || {}
    const quantitativeCount = stats.quantitativeCount || 0
    const qualitativeCount = stats.qualitativeCount || 0
    const hitFromDb = stats.hitFromDb || 0
    const newFromApi = stats.newFromApi || 0

    if (response.results && response.results.length > 0) {
      ElMessage.success(
        `识别完成！共 ${response.results.length} 个，` +
        `定量 ${quantitativeCount} 个，` +
        `定性 ${qualitativeCount} 个，` +
        `MySQL命中 ${hitFromDb} 个，` +
        `API新识别 ${newFromApi} 个`
      )
    } else {
      ElMessage.info('未识别到有效指标')
    }
  } catch (error) {
    console.error('识别失败:', error)
    ElMessage.error('识别失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  formData.value = { category: '', domain: '' }
  indicatorsText.value = ''
  result.value = null
}
</script>

<style scoped>
.indicator-identification-page {
  padding: 20px;
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

.input-card,
.result-card {
  margin-bottom: 20px;
}

.card-header,
.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-stats {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.textarea-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.indicator-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.indicator-item {
  border-left: 4px solid #409eff;
}

.indicator-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}

.indicator-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.indicator-name {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.confidence {
  color: #909399;
  font-size: 12px;
}

.formula-section {
  margin-bottom: 16px;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 8px;
  font-size: 14px;
}

.formula-box {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
  border-left: 3px solid #409eff;
  overflow: hidden;
}

.formula-main {
  font-size: 18px;
  font-family: 'Courier New', monospace;
  color: #303133;
  font-weight: bold;
  margin-bottom: 8px;
  word-break: break-all;
}

.formula-desc {
  color: #606266;
  font-size: 14px;
  word-break: break-all;
}

.calc-method {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  color: #606266;
  font-size: 14px;
  margin-bottom: 12px;
  padding-left: 8px;
  border-left: 2px solid #e6e8eb;
}

.method-label {
  font-weight: bold;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 4px;
}

.source-data-section {
  margin-bottom: 16px;
}

.source-data-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.source-data-tag {
  padding: 6px 10px;
  line-height: 1.6;
}

.data-content {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}

.data-name {
  font-weight: bold;
}

.data-symbol {
  color: #409eff;
  font-family: 'Courier New', monospace;
}

.data-unit {
  color: #909399;
  font-size: 12px;
}

.unit-info {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #606266;
  font-size: 14px;
}

.qualitative-note {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-style: italic;
}

.error-message {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #f56c6c;
  font-size: 14px;
  margin-top: 12px;
  padding: 8px 12px;
  background: #fef0f0;
  border-radius: 4px;
}
</style>
