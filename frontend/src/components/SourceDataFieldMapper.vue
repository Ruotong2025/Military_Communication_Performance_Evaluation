<template>
  <div class="source-data-mapper">
    <!-- 数据源基本信息 -->
    <div class="source-data-header">
      <div class="source-data-title">
        <span class="source-name">{{ sourceData.sourceDataName }}</span>
        <span class="formula-symbol" v-if="sourceData.formulaSymbol">({{ sourceData.formulaSymbol }})</span>
        <span class="unit" v-if="sourceData.unit">[{{ sourceData.unit }}]</span>
      </div>
    </div>

    <!-- 选择区域 -->
    <div class="mapping-options">
      <!-- API 建议选项 -->
      <div
        class="option-item"
        :class="{ 'is-selected': selectionType === 'API_RECOMMENDED' }"
        @click="selectApi"
      >
        <div class="option-radio">
          <el-radio
            v-model="localSelectionType"
            value="API_RECOMMENDED"
            @click.stop="selectApi"
          >
            <span class="option-label">采用 API 建议</span>
          </el-radio>
        </div>
        <div class="option-content" v-if="localSelectionType === 'API_RECOMMENDED'">
          <div v-if="loading" class="loading-hint">
            <el-icon class="is-loading"><Loading /></el-icon>
            正在搜索相似字段...
          </div>
          <div v-else-if="apiSuggestion" class="api-suggestion">
            <div class="suggestion-item api-item">
              <span class="suggestion-icon">📄</span>
              <span class="suggestion-field">{{ apiSuggestion.columnName }}</span>
              <span class="suggestion-source">
                来源: {{ apiSuggestion.tableLabel || 'API建议' }}/{{ apiSuggestion.columnName }}
              </span>
              <el-tag type="success" size="small" class="similarity-tag">
                {{ ((apiSuggestion.similarity || 0.95) * 100).toFixed(0) }}%
              </el-tag>
            </div>
          </div>
          <div v-else class="empty-hint">
            正在搜索数据库字段...
          </div>
        </div>
      </div>

      <!-- 数据库已有选项 -->
      <div
        class="option-item"
        :class="{ 'is-selected': selectionType === 'EXISTING_DATABASE', 'is-expanded': showDatabaseOptions }"
        @click="toggleDatabase"
      >
        <div class="option-radio">
          <el-radio
            v-model="localSelectionType"
            value="EXISTING_DATABASE"
            @click.stop="toggleDatabase"
          >
            <span class="option-label">采用已有</span>
          </el-radio>
        </div>

        <!-- 数据库匹配列表 -->
        <div class="option-content" v-if="showDatabaseOptions">
          <div v-if="loading" class="loading-hint">
            <el-icon class="is-loading"><Loading /></el-icon>
            正在搜索相似字段...
          </div>
          <div v-else-if="dbSuggestions && dbSuggestions.length > 0" class="db-suggestions">
            <div
              v-for="(item, index) in dbSuggestions"
              :key="index"
              class="suggestion-item db-item"
              :class="{ 'is-selected': selectedDbField === item.columnName }"
              @click.stop="selectDbField(item)"
            >
              <el-radio
                :model-value="selectedDbField"
                :value="item.columnName"
                @click.stop="selectDbField(item)"
                class="db-radio"
              />
              <span class="suggestion-field">{{ item.columnLabel || item.columnName }}</span>
              <span class="suggestion-source">{{ item.tableLabel }}</span>
              <el-tag type="info" size="small" class="similarity-tag">
                {{ (item.similarity * 100).toFixed(0) }}%
              </el-tag>
            </div>
          </div>
          <div v-else class="empty-hint">
            未找到匹配字段
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { searchColumnSuggestions } from '@/api'

const props = defineProps({
  // 数据源信息
  sourceData: {
    type: Object,
    required: true,
    default: () => ({})
  },
  // API 建议的字段
  apiSuggestion: {
    type: Object,
    default: null
  },
  // 外部传入的选择类型: EXISTING_DATABASE / API_RECOMMENDED
  selectionType: {
    type: String,
    default: 'API_RECOMMENDED'
  },
  // 外部传入的数据库选择字段
  selectedDbField: {
    type: String,
    default: ''
  },
  // 外部传入的关联已有数据源ID
  relatedSourceDataId: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['update:selectionType', 'update:selectedDbField', 'update:relatedSourceDataId', 'change'])

// 本地状态
const localSelectionType = ref(props.selectionType)
const showDatabaseOptions = ref(false)
const dbSuggestions = ref([])
const loading = ref(false)

// 选中的数据库字段
const selectedField = ref(props.selectedDbField || '')

// 选中的数据库字段对应的 relatedSourceDataId
const selectedRelatedSourceDataId = ref(props.relatedSourceDataId)

// 监听外部 selectionType 变化
watch(() => props.selectionType, (newVal) => {
  localSelectionType.value = newVal
  if (newVal === 'EXISTING_DATABASE') {
    showDatabaseOptions.value = true
    if (dbSuggestions.value.length === 0) {
      loadDbSuggestions()
    }
  }
})

// 监听本地选择类型变化
watch(localSelectionType, (newVal) => {
  emit('update:selectionType', newVal)
  if (newVal === 'EXISTING_DATABASE') {
    showDatabaseOptions.value = true
    if (dbSuggestions.value.length === 0) {
      loadDbSuggestions()
    }
  } else {
    showDatabaseOptions.value = false
  }
  emitChange()
})

// 监听外部 selectedDbField 变化
watch(() => props.selectedDbField, (newVal) => {
  if (newVal !== '') {
    selectedField.value = newVal
  }
})

// 选择 API 建议
const selectApi = () => {
  localSelectionType.value = 'API_RECOMMENDED'
  showDatabaseOptions.value = false
}

// 切换到数据库已有
const toggleDatabase = () => {
  if (localSelectionType.value !== 'EXISTING_DATABASE') {
    localSelectionType.value = 'EXISTING_DATABASE'
    showDatabaseOptions.value = true
    loadDbSuggestions()
  }
}

// 选择数据库字段
const selectDbField = (item) => {
  selectedField.value = item.columnName
  selectedRelatedSourceDataId.value = item.relatedSourceDataId || null
  emit('update:selectedDbField', item.columnName)
  emit('update:relatedSourceDataId', item.relatedSourceDataId || null)
  emitChange()
}

// 加载数据库相似字段
const loadDbSuggestions = async () => {
  if (!props.sourceData.sourceDataName) return

  loading.value = true
  try {
    // 注意：响应拦截器已经自动解包，所以 res 就是数组
    const res = await searchColumnSuggestions(props.sourceData.sourceDataName, 5)
    console.log('[调试] loadDbSuggestions res:', res)
    if (Array.isArray(res)) {
      dbSuggestions.value = res
      // 如果还没有选中字段，默认选中相似度最高的
      if (!selectedField.value && dbSuggestions.value.length > 0) {
        selectedField.value = dbSuggestions.value[0].columnName
        selectedRelatedSourceDataId.value = dbSuggestions.value[0].relatedSourceDataId || null
        emit('update:selectedDbField', dbSuggestions.value[0].columnName)
        emit('update:relatedSourceDataId', dbSuggestions.value[0].relatedSourceDataId || null)
      }
    }
  } catch (error) {
    console.error('搜索字段失败:', error)
    dbSuggestions.value = []
  } finally {
    loading.value = false
  }
}

// 触发变更事件
const emitChange = () => {
  emit('change', {
    sourceData: props.sourceData,
    selectionType: localSelectionType.value,
    selectedField: localSelectionType.value === 'API_RECOMMENDED'
      ? (props.apiSuggestion?.columnName || '')
      : selectedField.value,
    relatedSourceDataId: localSelectionType.value === 'API_RECOMMENDED'
      ? (props.apiSuggestion?.relatedSourceDataId || null)
      : selectedRelatedSourceDataId.value,
    apiSuggestion: props.apiSuggestion,
    dbSuggestions: dbSuggestions.value
  })
}

// 初始化时加载数据库建议
onMounted(() => {
  if (props.selectionType === 'EXISTING_DATABASE' || localSelectionType.value === 'EXISTING_DATABASE') {
    showDatabaseOptions.value = true
    loadDbSuggestions()
  }
})
</script>

<style scoped>
.source-data-mapper {
  background: #fafafa;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 12px 16px;
}

.source-data-header {
  margin-bottom: 12px;
}

.source-data-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #303133;
}

.source-name {
  font-size: 14px;
}

.formula-symbol {
  color: #409eff;
  font-family: 'Courier New', monospace;
}

.unit {
  color: #909399;
  font-size: 13px;
}

.mapping-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-item {
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 10px 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.option-item:hover {
  border-color: #c0d4ff;
}

.option-item.is-selected {
  border-color: #409eff;
  background: #f0f7ff;
}

.option-item.is-expanded {
  border-color: #409eff;
}

.option-radio {
  display: flex;
  align-items: center;
}

.option-label {
  font-weight: 500;
  color: #303133;
  margin-left: 4px;
}

.option-content {
  margin-top: 10px;
  padding-left: 24px;
}

.loading-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
  font-size: 13px;
  padding: 8px 0;
}

.empty-hint {
  color: #909399;
  font-size: 13px;
  padding: 8px 0;
}

.suggestion-item {
  display: grid;
  grid-template-columns: auto minmax(100px, 1fr) minmax(100px, 1.5fr) auto;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 4px;
  transition: background 0.2s ease;
}

.suggestion-item:hover {
  background: #f5f7fa;
}

.suggestion-item.is-selected {
  background: #ecf5ff;
}

.api-item {
  background: #f0f9eb;
  border: 1px solid #c2e7b0;
}

.db-item {
  /* grid 布局 */
}

.suggestion-icon {
  font-size: 14px;
  justify-self: center;
}

.suggestion-field {
  font-weight: 500;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
}

.suggestion-source {
  color: #909399;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
}

.similarity-tag {
  min-width: 50px;
  text-align: center;
  flex-shrink: 0;
}

.db-suggestions {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

/* 数据库选项的 radio 样式 */
.db-radio {
  justify-self: center;
}
</style>
