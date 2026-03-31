<template>
  <div class="data-view">
    <el-card class="page-card">
      <template #header>
        <div class="card-header">
          <el-icon class="header-icon"><DataAnalysis /></el-icon>
          <span>军事作战模拟数据</span>
        </div>
      </template>

      <!-- 参数表单区 -->
      <el-form :inline="true" :model="form" class="param-form" label-width="100px">
        <el-form-item label="优秀程度">
          <el-select v-model="form.excellentLevel" placeholder="请选择优秀程度" style="width: 180px">
            <el-option label="高" value="high" />
            <el-option label="中" value="medium" />
            <el-option label="低" value="low" />
          </el-select>
        </el-form-item>

        <el-form-item label="离散程度">
          <el-select v-model="form.dispersionLevel" placeholder="请选择离散程度" style="width: 180px">
            <el-option label="高" value="high" />
            <el-option label="中" value="medium" />
            <el-option label="低" value="low" />
          </el-select>
        </el-form-item>

        <el-form-item label="作战条数">
          <el-input-number
            v-model="form.count"
            :min="1"
            :max="100"
            placeholder="作战条数"
            style="width: 160px"
          />
        </el-form-item>

        <el-form-item label="随机种子">
          <el-input v-model="form.seed" placeholder="可不填" style="width: 160px" clearable />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="generating" @click="handleGenerate">生成模拟数据</el-button>
          <el-button @click="handleReset" :disabled="generating">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 高级参数折叠 -->
      <div class="advanced-toggle-row">
        <el-button text size="small" @click="showAdvanced = !showAdvanced">
          <el-icon>
            <ArrowDown v-if="!showAdvanced" />
            <ArrowUp v-else />
          </el-icon>
          {{ showAdvanced ? '收起高级参数' : '展开高级参数' }}
        </el-button>
        <span class="advanced-hint" v-if="!showAdvanced">（可自定义数据范围与写入模式）</span>
      </div>

      <AdvancedParamsPanel
        ref="advancedPanelRef"
        v-show="showAdvanced"
        :key="advancedPanelKey"
        v-model="advancedParams"
        :excellent-level="form.excellentLevel"
        :dispersion-level="form.dispersionLevel"
      />

      <!-- 当前参数展示 -->
      <div class="current-params" v-if="hasGenerated">
        <el-tag type="success">
          当前模拟参数：优秀程度={{ excellentLabel }}，离散程度={{ dispersionLabel }}，
          作战条数={{ form.count }}，模式={{ modeLabel }}
        </el-tag>
      </div>

      <!-- 四张模拟表共用作战 ID 筛选：选一次后切换 Tab 无需重选 -->
      <div v-if="globalOperationIds.length" class="global-operation-filter">
        <span class="filter-label">作战ID：</span>
        <el-select
          v-model="globalOperationId"
          placeholder="全部（不筛选）"
          clearable
          filterable
          style="width: 200px"
        >
          <el-option
            v-for="id in globalOperationIds"
            :key="id"
            :label="`作战ID ${id}`"
            :value="id"
          />
        </el-select>
        <span v-if="globalOperationId" class="filter-hint">四张表与下方「作战选择」均按此 ID 筛选</span>
      </div>

      <!-- 四表 Tab：lazy 仅挂载当前页；table-name 固定，避免四个表格同时抢同一 activeTab 重复请求 -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="table-tabs">
        <el-tab-pane label="作战基础信息" name="records_military_operation_info" lazy>
          <DynamicTable table-name="records_military_operation_info" :refresh-tick="tableRefreshTick" />
        </el-tab-pane>

        <el-tab-pane label="通信记录" name="records_military_communication_info" lazy>
          <DynamicTable table-name="records_military_communication_info" :refresh-tick="tableRefreshTick" />
        </el-tab-pane>

        <el-tab-pane label="链路维护事件" name="records_link_maintenance_events" lazy>
          <DynamicTable table-name="records_link_maintenance_events" :refresh-tick="tableRefreshTick" />
        </el-tab-pane>

        <el-tab-pane label="安全事件" name="records_security_events" lazy>
          <DynamicTable table-name="records_security_events" :refresh-tick="tableRefreshTick" />
        </el-tab-pane>
      </el-tabs>

      <!-- 效能指标计算：放在四张模拟表下方 -->
      <div class="metrics-section">
        <MetricsCalculation ref="metricsCalculationRef" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { DataAnalysis, ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import DynamicTable from '@/components/DynamicTable.vue'
import AdvancedParamsPanel from '@/components/AdvancedParamsPanel.vue'
import MetricsCalculation from '@/components/MetricsCalculation.vue'
import { generateCombatSimulation } from '@/api'
import {
  globalOperationId,
  globalOperationIds,
  loadGlobalOperationIds
} from '@/composables/useGlobalOperationFilter'

const activeTab = ref('records_military_operation_info')
const generating = ref(false)
/** 递增后子表格会重新拉取数据 */
const tableRefreshTick = ref(0)

const form = ref({
  excellentLevel: 'medium',
  dispersionLevel: 'medium',
  count: 10,
  seed: ''
})

/** 高级参数：{ mode, overrides, enumOverrides } */
const showAdvanced = ref(false)
const advancedParams = ref({ mode: 'overwrite', overrides: null, enumOverrides: null })
/** 重置时递增，使高级参数面板重新挂载并清空内部状态 */
const advancedPanelKey = ref(0)
/** 高级参数面板实例：生成前用 getModel() 取最新 overrides，避免 v-model 与 watch 不同步导致未下发 */
const advancedPanelRef = ref(null)
const metricsCalculationRef = ref(null)

const hasGenerated = ref(false)

const excellentLabel = computed(() => {
  const map = { high: '高', medium: '中', low: '低' }
  return map[form.value.excellentLevel] || '-'
})

const dispersionLabel = computed(() => {
  const map = { high: '高', medium: '中', low: '低' }
  return map[form.value.dispersionLevel] || '-'
})

const modeLabel = computed(() => {
  return advancedParams.value.mode === 'append' ? '新增' : '覆盖'
})

const handleGenerate = async () => {
  const c = form.value.count
  if (c == null || c < 1 || c > 100) {
    ElMessage.warning('作战条数请填写 1～100')
    return
  }
  generating.value = true
  try {
    await nextTick()
    const adv =
      typeof advancedPanelRef.value?.getModel === 'function'
        ? advancedPanelRef.value.getModel()
        : advancedParams.value

    const payload = {
      count: c,
      excellentLevel: form.value.excellentLevel,
      dispersionLevel: form.value.dispersionLevel,
      seed: form.value.seed || undefined,
    }
    if (adv.mode === 'append') {
      payload.mode = 'append'
    }
    if (adv.overrides && Object.keys(adv.overrides).length > 0) {
      payload.overrides = adv.overrides
    }
    if (adv.enumOverrides && Object.keys(adv.enumOverrides).length > 0) {
      payload.enumOverrides = adv.enumOverrides
    }
    advancedParams.value = { mode: adv.mode, overrides: adv.overrides, enumOverrides: adv.enumOverrides }

    await generateCombatSimulation(payload)
    hasGenerated.value = true
    tableRefreshTick.value += 1
    await loadGlobalOperationIds()
    await nextTick()
    metricsCalculationRef.value?.refreshFromParent?.()
    ElMessage.success(`模拟数据已生成（模式：${modeLabel.value}），表格已刷新`)
  } catch (e) {
    ElMessage.error(e?.message || '生成失败，请查看后端日志与 Python 环境')
  } finally {
    generating.value = false
  }
}

const handleReset = () => {
  form.value = {
    excellentLevel: 'medium',
    dispersionLevel: 'medium',
    count: 10,
    seed: ''
  }
  advancedParams.value = { mode: 'overwrite', overrides: null, enumOverrides: null }
  advancedPanelKey.value += 1
  showAdvanced.value = false
  hasGenerated.value = false
  ElMessage('参数已重置')
}

const handleTabChange = (tabName) => {
  console.log('切换到表:', tabName)
}

onMounted(() => {
  loadGlobalOperationIds()
})
</script>

<style scoped lang="scss">
.data-view {
  .page-card {
    min-height: calc(100vh - 120px);
  }

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

  .param-form {
    background: #f8faff;
    padding: 16px 16px 0;
    border-radius: 6px;
    border: 1px solid #e4eaf3;
    margin-bottom: 12px;
  }

  .advanced-toggle-row {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 8px;

    .advanced-hint {
      font-size: 12px;
      color: #aaa;
    }
  }

  .current-params {
    margin-bottom: 12px;
  }

  .global-operation-filter {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 12px;
    padding: 8px 12px;
    background: #f5f7fa;
    border-radius: 6px;
    border: 1px solid #e4e8ef;

    .filter-label {
      font-size: 13px;
      color: #606266;
      font-weight: 500;
    }

    .filter-hint {
      font-size: 12px;
      color: #909399;
    }
  }

  .table-tabs {
    :deep(.el-tabs__header) {
      margin-bottom: 12px;
    }
  }

  .metrics-section {
    margin-top: 28px;
    padding-top: 20px;
    border-top: 1px solid #e4eaf3;
  }
}
</style>
