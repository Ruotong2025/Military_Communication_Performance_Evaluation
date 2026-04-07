<template>
  <div class="cost-effectiveness-page">
    <!-- ==================== 页面头部 ==================== -->
    <div class="page-header">
      <div>
        <h2>
          <el-icon><PieChart /></el-icon>
          评估结果计算 — 效费分析
        </h2>
        <p class="subtitle">
          评估通信系统在作战运用中的投入产出效率，为资源优化配置、装备论证及训练效益评估提供量化依据
        </p>
      </div>
    </div>

    <!-- ==================== 提示信息 ==================== -->
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="hint-alert"
      title="选择评估批次后，系统将自动获取该批次下的所有作战任务进行批量效费分析"
    />

    <!-- ==================== 批次选择 ==================== -->
    <el-card class="toolbar-card" shadow="hover">
      <el-form :inline="true" label-width="120px">
        <el-form-item label="评估批次">
          <el-select
            v-model="evaluationId"
            placeholder="请选择评估批次"
            clearable
            filterable
            style="width: 320px"
            @change="onBatchChange"
          >
            <el-option v-for="id in evaluationIds" :key="id" :label="id" :value="id" />
          </el-select>
        </el-form-item>
        <el-form-item label="作战任务">
          <el-tag type="info" size="large">
            {{ batchOperationCount }} 个任务（自动获取）
          </el-tag>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="DataAnalysis" :loading="computing" :disabled="!canCalculate" @click="calculate">
            执行效费分析
          </el-button>
          <el-button :icon="Download" :disabled="!evaluationId" @click="loadResults">
            加载已保存
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ==================== 一、效费分析理论说明 ==================== -->
    <!-- 已隐藏：用户要求隐藏理论说明卡片 -->
    <el-card v-if="false" class="content-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><Document /></el-icon> 一、效费分析理论</span>
          <el-button text size="small" @click="showTheory = !showTheory">
            <el-icon><ArrowUp v-if="showTheory" /><ArrowDown v-else /></el-icon>
            {{ showTheory ? '收起' : '展开详情' }}
          </el-button>
        </div>
      </template>
      <div v-if="showTheory" class="theory-section">
        <h4>1.1 成本指标归一化（min-max 变换）</h4>
        <p>设共选取 p 个成本指标，每个指标有原始测量值 c<sub>k</sub>（k=1,2,…,p）。由于量纲不同，需进行归一化处理，得到成本分量 c<sub>k</sub>′∈[0,1]。</p>
        <div class="formula-box">
          <div>成本型指标（数值越大表示成本越高）：</div>
          <code>c<sub>k</sub>′ = (c<sub>k</sub> - c<sub>k,min</sub>) / (c<sub>k,max</sub> - c<sub>k,min</sub>)</code>
          <div style="margin-top: 8px">效益型指标（数值越大表示成本越低）：</div>
          <code>c<sub>k</sub>′ = (c<sub>k,max</sub> - c<sub>k</sub>) / (c<sub>k,max</sub> - c<sub>k,min</sub>)</code>
        </div>

        <h4>1.2 成本指数合成</h4>
        <p>采用加权求和得到综合成本指数 C：</p>
        <div class="formula-box">
          <code>C = Σ<sub>k=1</sub><sup>p</sup> ξ<sub>k</sub> · c<sub>k</sub>′</code>
          <div style="margin-top: 4px; font-size: 12px; color: #909399">其中 ξ<sub>k</sub> 为各成本指标的权重，满足 Σξ<sub>k</sub> = 1</div>
        </div>

        <h4>1.3 效费比计算</h4>
        <p>以最终效能得分 S<sub>final</sub> 作为效能指数 E，则效费比 R 定义为：</p>
        <div class="formula-box">
          <code>R = E / C</code>
          <div style="margin-top: 4px; font-size: 12px; color: #909399">R 值越大，表示单位投入获得的作战效能越高</div>
        </div>

        <h4>1.4 边际分析</h4>
        <p>通过调整某项投入，观察效能得分的变化，分析边际效费比：</p>
        <div class="formula-box">
          <code>MR = ΔE / ΔC</code>
          <div style="margin-top: 4px; font-size: 12px; color: #909399">当 MR > R<sub>当前</sub> 时，增加投入可提升效率；当 MR &lt; R<sub>当前</sub> 时，效率下降</div>
        </div>
      </div>
    </el-card>

    <!-- ==================== 二、成本指标配置 ==================== -->
    <el-card class="content-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><Setting /></el-icon> 二、成本指标配置</span>
          <div class="header-actions">
            <el-tag type="info" size="small">共 {{ activeIndicators.length }} 个指标</el-tag>
            <el-button size="small" :icon="Refresh" @click="loadIndicators">刷新配置</el-button>
            <el-button size="small" type="warning" @click="onResetWeights">
              <el-icon><RefreshRight /></el-icon> 重置为默认等权
            </el-button>
            <el-button size="small" type="primary" :icon="Check" :disabled="!weightChanged" @click="onSaveWeights">
              <el-icon><Check /></el-icon> 保存权重配置
            </el-button>
          </div>
        </div>
      </template>

      <!-- 权重说明 -->
      <el-alert type="info" :closable="false" show-icon class="weight-hint">
        <template #title>
          <span>两层等权规则：一级四类各 <strong>25%</strong>，类内各指标均分权重（如人力成本 5 个指标 → 每指标 <strong>5%</strong>）</span>
        </template>
      </el-alert>

      <el-tabs v-model="activeCategory" class="category-tabs">
        <el-tab-pane label="全部" name="all">
          <EditableIndicatorTable :indicators="activeIndicators" :weights="editableWeights" @update:weight="onWeightChange" />
        </el-tab-pane>
        <el-tab-pane v-for="cat in categories" :key="cat" :label="cat" :name="cat">
          <EditableIndicatorTable :indicators="getIndicatorsByCategory(cat)" :weights="editableWeights" @update:weight="onWeightChange" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- ==================== 三、原始成本数据（直接从 records_military_operation_info 取数）==================== -->
    <!-- 有数据时或加载中时都显示该卡片，只要选择了批次就显示 -->
    <el-card class="content-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><List /></el-icon> 三、原始成本数据（来源：records_military_operation_info）</span>
          <div class="header-actions">
            <el-tag type="warning" size="small">已选 {{ batchOperationCount }} 个作战任务</el-tag>
            <el-tag type="info" size="small">共 {{ dataColumns.length }} 项成本指标</el-tag>
          </div>
        </div>
      </template>

      <!-- 加载中 -->
      <el-skeleton v-if="loadingRawData" :rows="4" animated />

      <!-- 有数据 -->
      <template v-else-if="operationDataList.length > 0">
        <!-- 按四大成本类别 + 全量预览 Tab 分页展示 -->
        <el-tabs class="cost-preview-tabs" type="border-card">
          <el-tab-pane label="全部">
            <el-table :data="operationDataList" border stripe size="small" max-height="420" class="cost-data-table" :scrollbar-always-on="true">
              <el-table-column type="index" label="#" width="50" align="center" fixed />
              <el-table-column prop="operationId" label="作战ID" width="100" fixed align="center">
                <template #default="{ row }">
                  <el-tag type="primary" size="small">实验{{ row.operationId }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column
                v-for="col in dataColumns"
                :key="col.key"
                :prop="col.key"
                :label="col.label"
                width="130"
                align="center"
                show-overflow-tooltip
              >
                <template #header>
                  <span :title="col.category + '：' + col.label">{{ col.label }}</span>
                  <div style="font-size:9px;color:#b0b4bb">{{ col.format }}</div>
                </template>
                <template #default="{ row }">
                  {{ formatValue(row[col.key], col.format) }}
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane
            v-for="group in categoryColumns"
            :key="group.category"
            :label="group.category"
          >
            <el-table :data="operationDataList" border stripe size="small" max-height="360" class="cost-data-table">
              <el-table-column type="index" label="#" width="50" align="center" fixed />
              <el-table-column prop="operationId" label="作战ID" width="100" fixed align="center">
                <template #default="{ row }">
                  <el-tag type="primary" size="small">实验{{ row.operationId }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column
                v-for="col in group.columns"
                :key="col.key"
                :prop="col.key"
                :label="col.label"
                width="140"
                align="center"
                show-overflow-tooltip
              >
                <template #header>
                  <span>{{ col.label }}</span>
                  <div style="font-size:9px;color:#b0b4bb">{{ col.format }}</div>
                </template>
                <template #default="{ row }">
                  {{ formatValue(row[col.key], col.format) }}
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </template>

      <!-- 无数据（选择了批次但未加载原始数据） -->
      <template v-else-if="evaluationId && batchOperationCount > 0">
        <el-empty description="原始成本数据加载失败，请检查 records_military_operation_info 表或联系管理员">
          <el-button type="primary" size="small" :icon="Refresh" @click="onBatchChange">
            重新加载
          </el-button>
        </el-empty>
      </template>

      <!-- 无批次时提示 -->
      <template v-else>
        <el-empty description="请在上方选择评估批次，系统将自动加载原始成本数据">
          <template #image>
            <el-icon :size="60" color="#c0c4cc"><List /></el-icon>
          </template>
        </el-empty>
      </template>
    </el-card>

    <!-- ==================== 四、效费分析结果（Tab A 分量 + Tab B 结果）==================== -->
    <el-card class="content-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><Finished /></el-icon> 四、效费分析结果</span>
          <div class="header-actions">
            <el-tag type="warning" size="small">批次: {{ evaluationId || '—' }}</el-tag>
            <el-tag v-if="resultData.length === 0 && evaluationId" type="warning" size="small">
              暂无结果
            </el-tag>
          </div>
        </div>
      </template>

      <!-- 有计算结果：自上而下 Min–Max 边界 → 方向调整后分量 → 效费比 -->
      <template v-if="resultData.length > 0">
        <!-- 4.1 Min–Max 归一化边界（本批次用于 min-max 的全局上下界） -->
        <div class="result-block">
          <div class="result-block-title">Min–Max 归一化边界</div>
          <p class="result-block-desc">
            下列 min / max 为当前批次内各指标原始取值范围，用于 min-max 归一化；当前全部按成本型处理，不做方向取反。
          </p>
          <el-table :data="minMaxTableRows" border stripe size="small" max-height="320" class="minmax-table">
            <el-table-column prop="indicatorName" label="指标" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">
                <span :style="{ color: getCategoryColor(row.category) }">{{ row.indicatorName }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="min" label="Min" width="110" align="center">
              <template #default="{ row }">
                {{ formatBoundValue(row.min) }}
              </template>
            </el-table-column>
            <el-table-column prop="max" label="Max" width="110" align="center">
              <template #default="{ row }">
                {{ formatBoundValue(row.max) }}
              </template>
            </el-table-column>
            <el-table-column label="指标类型" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.indicatorType === 'benefit' ? 'success' : 'info'" size="small">
                  {{ row.indicatorType === 'benefit' ? '效益型' : '成本型' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="方向调整" min-width="200" align="center">
              <template #default="{ row }">
                <span class="dir-adjust-no">成本型：不反向（数值越大，可比成本分量越高）</span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 4.2 方向调整后归一化分量 -->
        <div class="result-block">
          <div class="result-block-title">归一化分量（0～1，均为成本型）</div>
          <div class="tab-tip">全部指标按成本型处理：min-max 归一化后直接作为可比成本分量，数值越大表示该维度投入/负担越高。</div>
          <el-tabs class="category-preview-tabs" type="border-card">
            <el-tab-pane label="全部">
              <el-table :data="normalizedDataList" border stripe size="small" max-height="400" :scrollbar-always-on="true">
                <el-table-column type="index" label="#" width="50" align="center" fixed />
                <el-table-column prop="operationId" label="作战ID" width="100" fixed align="center">
                  <template #default="{ row }">
                    <el-tag type="primary" size="small">实验{{ row.operationId }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column
                  v-for="col in dataColumns"
                  :key="col.key"
                  :prop="col.key"
                  :label="col.label"
                  width="150"
                  align="center"
                  show-overflow-tooltip
                >
                  <template #header>
                    <div class="ind-header">
                      <span :style="{ color: getCategoryColor(col.category) }" :title="col.category + '：' + col.label">{{ col.label }}</span>
                      <el-tag size="small" type="info" class="dir-tag">正向</el-tag>
                    </div>
                  </template>
                  <template #default="{ row }">
                    <span :style="{ color: getCategoryColor(col.category) }">
                      {{ formatNormalizedValue(row[col.key]) }}
                    </span>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
            <el-tab-pane v-for="group in categoryColumns" :key="group.category" :label="group.category" :name="group.category">
              <el-table :data="normalizedDataList" border stripe size="small" max-height="360" :scrollbar-always-on="true">
                <el-table-column type="index" label="#" width="50" align="center" fixed />
                <el-table-column prop="operationId" label="作战ID" width="100" fixed align="center">
                  <template #default="{ row }">
                    <el-tag type="primary" size="small">实验{{ row.operationId }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column
                  v-for="col in group.columns"
                  :key="col.key"
                  :prop="col.key"
                  :label="col.label"
                  width="150"
                  align="center"
                  show-overflow-tooltip
                >
                  <template #header>
                    <div class="ind-header">
                      <span :style="{ color: getCategoryColor(group.category) }">{{ col.label }}</span>
                      <el-tag size="small" type="info" class="dir-tag">正向</el-tag>
                    </div>
                  </template>
                  <template #default="{ row }">
                    <span :style="{ color: getCategoryColor(group.category) }">
                      {{ formatNormalizedValue(row[col.key]) }}
                    </span>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </div>

        <!-- 4.3 效费比结果 -->
        <div class="result-block">
          <div class="result-block-title">效费比结果</div>
          <el-table :data="resultData" border stripe size="small" max-height="400" class="result-summary-table" :scrollbar-always-on="true">
            <el-table-column prop="operationId" label="作战ID" width="100" fixed align="center">
              <template #default="{ row }">
                <el-tag type="primary" size="small">实验{{ row.operationId }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column v-for="(cat, idx) in COST_CATEGORIES_ORDER" :key="cat" :label="cat" width="130" align="center">
              <template #header>
                <span class="cat-header" :style="{ color: categoryColors[idx] }">{{ cat }}</span>
              </template>
              <template #default="{ row }">
                <span class="cat-value-cell" :style="{ color: categoryColors[idx] }">
                  {{ row.costByCategory?.[cat]?.toFixed(4) || '—' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="综合花费 C" width="140" fixed align="center">
              <template #default="{ row }">
                <span class="cost-index">{{ row.costIndex?.toFixed(4) || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="效能 E" width="140" align="center">
              <template #default="{ row }">
                <span class="effect-score">{{ row.effectivenessScore?.toFixed(4) || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="效费比 R" width="140" fixed align="center">
              <template #default="{ row }">
                <span class="ratio-value" :class="getRatioClass(row.costEffectivenessRatio)">
                  {{ row.costEffectivenessRatio?.toFixed(4) || '—' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="等级" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getGradeType(row.efficiencyGrade)" size="small">
                  {{ row.efficiencyGrade || '—' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>

      <!-- 无计算结果时显示空状态 -->
      <template v-else>
        <el-empty
          v-if="evaluationId && batchOperationCount > 0"
          description="该批次暂无效费分析结果，请点击「执行效费分析」计算"
        >
          <template #image>
            <el-icon :size="60" color="#c0c4cc"><Finished /></el-icon>
          </template>
          <el-button type="primary" :loading="computing" :disabled="!canCalculate" @click="calculate">
            执行效费分析
          </el-button>
        </el-empty>
        <el-empty v-else description="请在上方选择评估批次后查看效费分析结果">
          <template #image>
            <el-icon :size="60" color="#c0c4cc"><Finished /></el-icon>
          </template>
        </el-empty>
      </template>
    </el-card>

    <!-- ==================== 五、本批次作战：效能 · 投入 · 效费比（同图对比）==================== -->
    <el-card v-if="resultData.length > 0" class="content-card chart-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><TrendCharts /></el-icon> 五、本批次作战综合对比</span>
        </div>
      </template>

      <el-alert type="info" :closable="false" show-icon class="chart-hint-alert">
        下图展示当前评估批次下<strong>全部作战</strong>的<strong>效能 E</strong>、<strong>综合投入 C</strong>（成本指数 0~1）、<strong>效费比 R</strong>（E/C），便于横向对比。
        左侧纵轴为效能 E；右侧纵轴为投入 C 与效费比 R（量纲不同，请勿用柱高直接比较 E 与 C、R）。
      </el-alert>

      <div class="chart-container" ref="chartRef" />
    </el-card>

    <!-- ==================== 加载中状态 ==================== -->
    <div v-if="computing" class="loading-overlay">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>正在计算效费分析...</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import {
  PieChart, DataAnalysis, Document, Setting, List, Finished,
  TrendCharts, Refresh, Download, ArrowUp, ArrowDown, Loading,
  RefreshRight, Check
} from '@element-plus/icons-vue'
import {
  getCollectiveEvaluationIds, getOperationIdsByEvaluationId, getCostRawDataPreview,
  getCostIndicators, getCostCategories, getCostEffectivenessResults,
  calculateCostEffectiveness, saveCostIndicatorWeights, resetCostWeights
} from '@/api'

// ==================== 状态 ====================
const evaluationIds = ref([])
const evaluationId = ref('')
const batchOperationIds = ref([])  // 当前批次的所有作战任务ID
const computing = ref(false)
const showTheory = ref(true)
const activeCategory = ref('all')

// 指标配置
const activeIndicators = ref([])
const categories = ref([])
const editableWeights = ref({})      // 可编辑的权重 Map<indicatorKey, weight>
const originalWeights = ref({})      // 原始权重（用于判断是否修改）
const weightChanged = computed(() => JSON.stringify(editableWeights.value) !== JSON.stringify(originalWeights.value))

// 计算结果
const resultData = ref([])
const statistics = ref(null)
const normalizationBounds = ref(null)
const weightsConfig = ref(null)

// 原始成本数据（从 records_military_operation_info 表，用于预览）
const operationRawData = ref([])   // 数组，每项含 operationId 和各指标原始值
const loadingRawData = ref(false)  // 原始数据加载状态

// 图表（单图：效能 + 投入 + 效费比）
const chartRef = ref(null)
let chartInstance = null

// 类别颜色（对应 COST_CATEGORIES_ORDER）
const categoryColors = ['#409EFF', '#67C23A', '#E6A23C', '#9B59B6']

// ==================== 计算属性 ====================
const canCalculate = computed(() => {
  return evaluationId.value && batchOperationIds.value.length > 0
})

// 批次作战任务数量
const batchOperationCount = computed(() => batchOperationIds.value.length)

// 成本数据预览列（完整展示全部 16 个成本指标 + 1 个推导指标）
const dataColumns = computed(() => {
  if (!activeIndicators.value.length) return []
  // 全部展示（不再 slice 截断），每个指标附加所属类别和格式化信息
  return activeIndicators.value.map(ind => ({
    key: ind.indicatorKey,
    label: ind.indicatorName,
    format: ind.unit,
    category: ind.category
  }))
})

// 成本类别（按人力/装备/能源/备件物流四大类分组）
const COST_CATEGORIES_ORDER = ['人力成本', '装备成本', '能源成本', '备件物流']

// 按类别分组的成本数据列（用于预览表格的分栏展示）
const categoryColumns = computed(() => {
  if (!activeIndicators.value.length) return []
  const grouped = {}
  for (const cat of COST_CATEGORIES_ORDER) {
    grouped[cat] = []
  }
  // 兜底未知类别
  grouped['其他'] = []

  for (const ind of activeIndicators.value) {
    const col = {
      key: ind.indicatorKey,
      label: ind.indicatorName,
      format: ind.unit,
      category: ind.category
    }
    if (grouped[ind.category]) {
      grouped[ind.category].push(col)
    } else {
      grouped['其他'].push(col)
    }
  }
  return COST_CATEGORIES_ORDER.filter(cat => grouped[cat].length > 0).map(cat => ({
    category: cat,
    columns: grouped[cat]
  }))
})

// 操作数据列表（直接使用已获取的原始数据）
const operationDataList = computed(() => {
  return operationRawData.value.map(row => ({
    operationId: row.operationId,
    ...row
  }))
})

/**
 * 归一化分量（全部按成本型）：优先用 normalizedIndicators 直接作为 0~1 可比分量，与后端 min-max 一致、不做 1−x 取反。
 * 若无 normalizedIndicators，再回退接口中的 effectiveNormalized。
 */
function getEffectiveNormalizedMap(row) {
  const norm = row.normalizedIndicators ?? row.normalized_indicators
  if (norm && typeof norm === 'object' && Object.keys(norm).length > 0) {
    const out = {}
    for (const ind of activeIndicators.value) {
      const k = ind.indicatorKey
      const n = Number(norm[k])
      if (Number.isNaN(n)) continue
      out[k] = n
    }
    return out
  }
  const direct = row.effectiveNormalized ?? row.effective_normalized
  if (direct && typeof direct === 'object' && Object.keys(direct).length > 0) {
    return { ...direct }
  }
  return {}
}

// 批次内各指标 min/max（用于「Min–Max 边界」表）
const batchIndicatorBounds = computed(() => {
  const r0 = resultData.value[0]
  const nb = r0?.normalizationBounds ?? r0?.normalization_bounds
  if (nb && typeof nb === 'object' && Object.keys(nb).length > 0) {
    return nb
  }
  // 刚执行「效费分析」时，汇总边界在响应顶层 normalizationBounds：{ minValues, maxValues }
  const top = normalizationBounds.value
  if (top?.minValues && top?.maxValues) {
    const m = {}
    const keys = new Set([
      ...Object.keys(top.minValues),
      ...Object.keys(top.maxValues)
    ])
    for (const k of keys) {
      m[k] = {
        min: top.minValues[k],
        max: top.maxValues[k]
      }
    }
    return m
  }
  const raw0 = operationRawData.value[0]
  const arr = raw0?._indicatorBounds
  if (Array.isArray(arr) && arr.length) {
    const m = {}
    for (const b of arr) {
      if (b?.indicatorKey != null) {
        m[b.indicatorKey] = { min: b.min, max: b.max }
      }
    }
    return m
  }
  return {}
})

// Min–Max 边界表（每指标一行）
const minMaxTableRows = computed(() => {
  const bounds = batchIndicatorBounds.value
  return activeIndicators.value.map(ind => {
    const b = bounds[ind.indicatorKey] || {}
    return {
      indicatorKey: ind.indicatorKey,
      indicatorName: ind.indicatorName,
      category: ind.category,
      min: b.min,
      max: b.max,
      indicatorType: ind.indicatorType
    }
  })
})

// 归一化分量数据列表（方向调整后，0~1）
const normalizedDataList = computed(() => {
  return resultData.value.map(row => {
    const item = {
      operationId: row.operationId
    }
    const eff = getEffectiveNormalizedMap(row)
    for (const [key, val] of Object.entries(eff)) {
      item[key] = val
    }
    return item
  })
})

/** 格式化归一化分量值 */
function formatNormalizedValue(value) {
  if (value == null) return '—'
  const num = Number(value)
  if (isNaN(num)) return '—'
  return num.toFixed(4)
}

/** Min/Max 边界展示 */
function formatBoundValue(value) {
  if (value == null || value === '') return '—'
  const num = Number(value)
  if (isNaN(num)) return '—'
  return num.toFixed(4)
}

/** 获取类别颜色 */
function getCategoryColor(category) {
  const idx = COST_CATEGORIES_ORDER.indexOf(category)
  return idx >= 0 ? categoryColors[idx] : '#909399'
}

// 权重配置分组（用于显示两层等权配置）
const weightConfigGroups = computed(() => {
  if (!weightsConfig.value || !activeIndicators.value.length) return []

  const groups = []
  for (const cat of COST_CATEGORIES_ORDER) {
    const catIndicators = activeIndicators.value.filter(ind => ind.category === cat)
    if (catIndicators.length === 0) continue

    const indicators = []
    let totalWeight = 0
    for (const ind of catIndicators) {
      const w = weightsConfig.value[ind.indicatorKey] || 0
      indicators.push({ key: ind.indicatorKey, name: ind.indicatorName, weight: w })
      totalWeight += w
    }

    const perIndicatorWeight = indicators.length > 0 ? totalWeight / indicators.length : 0
    groups.push({
      category: cat,
      indicators,
      weight: totalWeight,
      perIndicatorWeight
    })
  }
  return groups
})

/** 加载评估批次 */
async function loadEvaluationIds() {
  try {
    // 拦截器已解包，直接得到 List<String>
    const ids = await getCollectiveEvaluationIds()
    evaluationIds.value = Array.isArray(ids) ? ids : []
  } catch (e) {
    console.error(e)
    evaluationIds.value = []
  }
}

/** 加载作战任务列表（初始加载，批次对应的任务在 onBatchChange 中单独获取） */
async function loadOperationIds() {
  // 批次对应的作战任务在选择批次时通过 getOperationIdsByEvaluationId 获取
  // 此处无需额外操作
}

/** 加载指标配置 */
async function loadIndicators() {
  try {
    // 拦截器已解包，直接得到数据
    const [indicators, cats] = await Promise.all([
      getCostIndicators(),
      getCostCategories()
    ])
    activeIndicators.value = (indicators || []).map(ind => ({
      ...ind,
      indicatorType: 'cost'
    }))
    categories.value = cats || []
    // 初始化权重配置
    initEditableWeights()
  } catch (e) {
    console.error(e)
    activeIndicators.value = []
    categories.value = []
  }
}

/** 初始化可编辑权重（两层等权或数据库已有值） */
function initEditableWeights() {
  const weights = {}
  // 按类别分组计算两层等权
  const categoryGroups = {}
  for (const cat of COST_CATEGORIES_ORDER) {
    categoryGroups[cat] = []
  }
  for (const ind of activeIndicators.value) {
    const cat = ind.category || '_other'
    if (!categoryGroups[cat]) categoryGroups[cat] = []
    categoryGroups[cat].push(ind)
  }

  const nonEmptyCategories = COST_CATEGORIES_ORDER.filter(cat => categoryGroups[cat].length > 0).length
  const baseCatWeight = 0.25
  const emptyCount = 4 - nonEmptyCategories
  let extraWeight = 0
  if (emptyCount > 0 && nonEmptyCategories > 0) {
    extraWeight = (baseCatWeight * emptyCount) / nonEmptyCategories
  }

  for (const cat of COST_CATEGORIES_ORDER) {
    const indicators = categoryGroups[cat]
    if (indicators.length === 0) continue
    const catWeight = baseCatWeight + extraWeight
    const perIndWeight = catWeight / indicators.length
    for (const ind of indicators) {
      // 数据库有值用数据库值，否则用两层等权计算值
      weights[ind.indicatorKey] = ind.weight != null ? Number(ind.weight) : perIndWeight
    }
  }
  editableWeights.value = weights
  normalizeIndicatorWeightsToOne()
  originalWeights.value = JSON.parse(JSON.stringify(editableWeights.value))
}

/**
 * 将当前所有启用指标的权重归一化，使总和严格为 1（保留 4 位小数，最后一项吸收舍入误差）
 */
function normalizeIndicatorWeightsToOne() {
  const keys = activeIndicators.value.map(i => i.indicatorKey)
  if (!keys.length) return

  const prev = { ...editableWeights.value }
  const positive = keys.map(k => {
    const v = Number(prev[k])
    return Number.isFinite(v) && v > 0 ? v : 0
  })
  let sum = positive.reduce((a, b) => a + b, 0)

  const out = {}
  if (sum <= 0) {
    const eq = Math.round((1 / keys.length) * 1e4) / 1e4
    let acc = 0
    for (let i = 0; i < keys.length - 1; i++) {
      out[keys[i]] = eq
      acc += eq
    }
    out[keys[keys.length - 1]] = Math.round((1 - acc) * 1e4) / 1e4
    editableWeights.value = out
    return
  }

  let acc = 0
  for (let i = 0; i < keys.length - 1; i++) {
    const v = Math.round((positive[i] / sum) * 1e4) / 1e4
    out[keys[i]] = v
    acc += v
  }
  out[keys[keys.length - 1]] = Math.round((1 - acc) * 1e4) / 1e4
  editableWeights.value = out
}

/** 权重变更回调（修改后自动归一化，总和为 1） */
function onWeightChange({ key, value }) {
  const v = value == null ? 0 : Number(value)
  const clamped = Number.isFinite(v) ? Math.max(0, v) : 0
  editableWeights.value = { ...editableWeights.value, [key]: clamped }
  normalizeIndicatorWeightsToOne()
}

/** 保存权重配置 */
async function onSaveWeights() {
  try {
    normalizeIndicatorWeightsToOne()
    const weights = {}
    for (const [key, val] of Object.entries(editableWeights.value)) {
      weights[key] = Number(val)
    }
    await saveCostIndicatorWeights(weights)
    originalWeights.value = JSON.parse(JSON.stringify(editableWeights.value))
    ElMessage.success('权重配置保存成功')
  } catch (e) {
    console.error(e)
    ElMessage.error('保存失败: ' + (e?.message || e))
  }
}

/** 重置为默认等权 */
async function onResetWeights() {
  try {
    const weights = await resetCostWeights()
    if (weights) {
      // 将返回的权重映射到 editableWeights
      const newWeights = {}
      for (const ind of activeIndicators.value) {
        newWeights[ind.indicatorKey] = weights[ind.indicatorKey] != null
          ? Number(weights[ind.indicatorKey])
          : editableWeights.value[ind.indicatorKey]
      }
      editableWeights.value = newWeights
      normalizeIndicatorWeightsToOne()
      originalWeights.value = JSON.parse(JSON.stringify(editableWeights.value))
      ElMessage.success('已重置为两层等权权重')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('重置失败: ' + (e?.message || e))
  }
}

/** 执行效费分析计算 */
async function calculate() {
  if (!canCalculate.value) {
    ElMessage.warning('请选择评估批次和作战任务')
    return
  }

  computing.value = true
  try {
    // 后端返回 ApiResponse，拦截器已解包，直接得到 CostEffectivenessCalculateResponse
    const resp = await calculateCostEffectiveness({
      evaluationId: evaluationId.value,
      operationIds: batchOperationIds.value,
      weightMethod: 'equal',
      normalizationMethod: 'minmax',
      useActualRange: true
    })

    if (resp && resp.results && resp.results.length > 0) {
      resultData.value = resp.results
      statistics.value = resp.statistics || null
      normalizationBounds.value = resp.normalizationBounds || null
      weightsConfig.value = resp.weightsConfig || null
      // 清空原始数据预览（已切换到计算结果视图）
      operationRawData.value = []
      ElMessage.success(`效费分析计算完成，共 ${resultData.value.length} 条记录`)
      await nextTick()
      renderChart()
    } else {
      ElMessage.warning('计算完成但无结果数据，请检查成本指标配置和效能得分数据')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error(e?.message || '计算失败')
  } finally {
    computing.value = false
  }
}

/** 加载已保存的结果 */
async function loadResults() {
  if (!evaluationId.value) {
    ElMessage.warning('请先选择评估批次')
    return
  }

  try {
    // 拦截器已解包，直接得到 List<CostEffectivenessResultDTO>
    const results = await getCostEffectivenessResults(evaluationId.value)
    const resultsList = Array.isArray(results) ? results : []
    if (resultsList && resultsList.length > 0) {
      resultData.value = resultsList
      batchOperationIds.value = resultsList.map(r => r.operationId)
      // 清空原始数据预览（已切换到结果视图）
      operationRawData.value = []
      // 计算统计信息
      const ratios = results.map(r => Number(r.costEffectivenessRatio || 0))
      const costs = results.map(r => Number(r.costIndex || 0))
      const effects = results.map(r => Number(r.effectivenessScore || 0))

      statistics.value = {
        avgCostEffectivenessRatio: ratios.reduce((a, b) => a + b, 0) / ratios.length,
        minCostEffectivenessRatio: Math.min(...ratios),
        maxCostEffectivenessRatio: Math.max(...ratios),
        avgCostIndex: costs.reduce((a, b) => a + b, 0) / costs.length,
        minCostIndex: Math.min(...costs),
        maxCostIndex: Math.max(...costs),
        avgEffectivenessScore: effects.reduce((a, b) => a + b, 0) / effects.length,
        minEffectivenessScore: Math.min(...effects),
        maxEffectivenessScore: Math.max(...effects)
      }

      ElMessage.success(`已加载 ${results.length} 条保存的结果`)
      await nextTick()
      renderChart()
    } else {
      ElMessage.info('该批次暂无保存的结果')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error(e?.message || '加载失败')
  }
}

/** 按类别获取指标 */
function getIndicatorsByCategory(category) {
  return activeIndicators.value.filter(ind => ind.category === category)
}

/** 格式化数值 */
function formatValue(value, format) {
  if (value == null) return '—'
  const num = Number(value)
  if (isNaN(num)) return '—'
  return num.toFixed(2) + (format ? ' ' + format : '')
}

/**
 * 构建展开行的数据（用于显示原始指标、归一化值、权重等明细）
 * @param {Object} data 原始指标或归一化数据 Map
 * @param {'raw'|'norm'} type raw=原始值表格，norm=归一化值表格
 * @param {Object} normBounds 归一化边界数据 {key: {min, max}}
 */
function buildExpandRows(data, type, normBounds) {
  if (!data) return []
  return Object.entries(data).map(([key, rawVal]) => {
    // 从 dataColumns 中查找该指标的配置
    const col = dataColumns.value.find(c => c.key === key) || {}
    const weight = weightsConfig.value?.[key] ?? null
    const isBenefit = false

    // 获取 min/max 边界
    const bounds = normBounds?.[key] || {}
    const minVal = bounds.min != null ? Number(bounds.min) : null
    const maxVal = bounds.max != null ? Number(bounds.max) : null

    // 归一化表格显示加权成本（需要用归一化值 × 权重）
    let weighted = null
    if (type === 'norm' && weight != null) {
      const normVal = Number(rawVal) || 0
      weighted = (normVal * Number(weight)).toFixed(6)
    }

    return {
      key,
      label: col.label || key,
      value: type === 'raw' ? (Number(rawVal)?.toFixed(4) ?? '—') : (Number(rawVal)?.toFixed(6) ?? '—'),
      unit: col.format || '—',
      category: col.category || '—',
      isBenefit,
      weight: weight != null ? Number(weight).toFixed(4) : '—',
      weighted: weighted ?? '—',
      type: type === 'norm' ? (isBenefit ? 'benefit' : 'cost') : '—',
      min: minVal != null ? minVal.toFixed(4) : '—',
      max: maxVal != null ? maxVal.toFixed(4) : '—'
    }
  })
}

/** 效费比样式 */
function getRatioClass(ratio) {
  if (ratio == null) return ''
  if (ratio >= 1.5) return 'ratio-excellent'
  if (ratio >= 1.2) return 'ratio-good'
  if (ratio >= 1.0) return 'ratio-pass'
  return 'ratio-poor'
}

/** 效费等级标签类型 */
function getGradeType(grade) {
  const types = {
    '优秀': 'success',
    '良好': '',
    '合格': 'warning',
    '较差': 'danger',
    '差': 'danger'
  }
  return types[grade] || 'info'
}

/** 批次变更 - 自动获取批次中的作战任务及其原始成本数据 */
async function onBatchChange() {
  // 清除所有之前的状态（结果/图表/原始数据）
  resultData.value = []
  statistics.value = null
  normalizationBounds.value = null
  weightsConfig.value = null
  batchOperationIds.value = []
  operationRawData.value = []
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }

  if (!evaluationId.value) return

  try {
    // Step 1: 获取该批次包含的作战任务ID列表
    const ids = await getOperationIdsByEvaluationId(evaluationId.value)
    const idList = Array.isArray(ids) ? ids.map(id => String(id)) : []
    batchOperationIds.value = idList

    if (idList.length === 0) {
      ElMessage.warning(`批次 [${evaluationId.value}] 暂无作战任务数据`)
      return
    }

    ElMessage.info(`批次 [${evaluationId.value}] 共 ${idList.length} 个作战任务，正在加载...`)

    // Step 2: 并行加载原始数据预览 + 已保存的效费分析结果
    const [previewData, savedResults] = await Promise.all([
      getCostRawDataPreview(evaluationId.value, idList).catch(() => []),
      getCostEffectivenessResults(evaluationId.value).catch(() => [])
    ])

    // 填入原始成本数据（原始数据预览表格用）
    operationRawData.value = Array.isArray(previewData) ? previewData : []

    // 如果有已保存的结果，直接加载显示（效费比/成本指数等）
    if (Array.isArray(savedResults) && savedResults.length > 0) {
      resultData.value = savedResults
      // 计算统计（后端返回的 result 里可能有 costEffectivenessRatio）
      const ratios = savedResults.map(r => Number(r.costEffectivenessRatio || 0))
      const costs = savedResults.map(r => Number(r.costIndex || 0))
      const effects = savedResults.map(r => Number(r.effectivenessScore || 0))
      statistics.value = {
        avgCostEffectivenessRatio: ratios.reduce((a, b) => a + b, 0) / ratios.length,
        minCostEffectivenessRatio: Math.min(...ratios),
        maxCostEffectivenessRatio: Math.max(...ratios),
        avgCostIndex: costs.reduce((a, b) => a + b, 0) / costs.length,
        minCostIndex: Math.min(...costs),
        maxCostIndex: Math.max(...costs),
        avgEffectivenessScore: effects.reduce((a, b) => a + b, 0) / effects.length,
        minEffectivenessScore: Math.min(...effects),
        maxEffectivenessScore: Math.max(...effects)
      }
      await nextTick()
      renderChart()
      ElMessage.success(`已自动加载 ${savedResults.length} 条已保存结果（效费比等）`)
    } else {
      // 无已保存结果，仅显示原始数据预览
      ElMessage.success(`原始数据加载完成：${operationRawData.value.length} 条，可点击「执行效费分析」计算`)
    }
  } catch (e) {
    console.error('批次切换失败:', e)
    ElMessage.error('加载失败: ' + (e?.message || e))
  }
}

// ==================== 图表渲染（单图：效能 E · 投入 C · 效费比 R）====================
function sortResultsForChart(rows) {
  return [...rows].sort((a, b) => {
    const na = Number(a.operationId)
    const nb = Number(b.operationId)
    if (!Number.isNaN(na) && !Number.isNaN(nb)) return na - nb
    return String(a.operationId).localeCompare(String(b.operationId), 'zh-CN')
  })
}

function renderChart() {
  if (!chartRef.value || !resultData.value.length) return
  if (!chartInstance) chartInstance = echarts.init(chartRef.value)

  const sorted = sortResultsForChart(resultData.value)
  const labels = sorted.map(r => `实验${r.operationId}`)
  const seriesE = sorted.map(r => Number(r.effectivenessScore ?? 0))
  const seriesC = sorted.map(r => Number(r.costIndex ?? 0))
  const seriesR = sorted.map(r => Number(r.costEffectivenessRatio ?? 0))
  const batchId = evaluationId.value || sorted[0]?.evaluationId || ''

  chartInstance.setOption({
    title: {
      text: `批次 ${batchId}`,
      subtext: '每组三根柱：效能 E（左轴）｜投入 C、效费比 R（右轴）',
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
      data: ['效能 E', '投入 C', '效费比 R'],
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
        name: '投入 C / 效费比 R',
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
        name: '投入 C',
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

  chartInstance.resize()
}

// ==================== 生命周期 ====================

onMounted(async () => {
  await Promise.all([
    loadEvaluationIds(),
    loadOperationIds(),
    loadIndicators()
  ])
})
</script>

<script>
// 指标表格组件
import { defineComponent, h } from 'vue'
import { ElTable, ElTableColumn, ElTag, ElInputNumber } from 'element-plus'

const IndicatorTable = defineComponent({
  name: 'IndicatorTable',
  props: {
    indicators: { type: Array, default: () => [] },
    editable: { type: Boolean, default: false }
  },
  setup(props) {
    return () => h(ElTable, {
      data: props.indicators,
      border: true,
      stripe: true,
      size: 'small',
      maxHeight: 280
    }, () => [
      h(ElTableColumn, { prop: 'indicatorName', label: '指标名称', width: 160 }),
      h(ElTableColumn, { prop: 'indicatorKey', label: '字段标识', width: 200 }),
      h(ElTableColumn, { prop: 'category', label: '类别', width: 100, align: 'center' }),
      h(ElTableColumn, { prop: 'indicatorType', label: '类型', width: 100, align: 'center' }, {
        default: ({ row }) => h(ElTag, {
          type: row.indicatorType === 'cost' ? 'danger' : 'success',
          size: 'small'
        }, () => row.indicatorType === 'cost' ? '成本型' : '效益型')
      }),
      h(ElTableColumn, { prop: 'unit', label: '单位', width: 80, align: 'center' }),
      h(ElTableColumn, { prop: 'weight', label: '权重', width: 80, align: 'center' }, {
        default: ({ row }) => row.weight ? Number(row.weight).toFixed(3) : '—'
      }),
      h(ElTableColumn, { prop: 'description', label: '说明', minWidth: 150 })
    ])
  }
})

// 可编辑指标表格组件
const EditableIndicatorTable = defineComponent({
  name: 'EditableIndicatorTable',
  props: {
    indicators: { type: Array, default: () => [] },
    weights: { type: Object, default: () => ({}) }
  },
  emits: ['update:weight'],
  setup(props, { emit }) {
    return () => h(ElTable, {
      data: props.indicators,
      border: true,
      stripe: true,
      size: 'small',
      maxHeight: 320
    }, () => [
      h(ElTableColumn, { prop: 'indicatorName', label: '指标名称', width: 160 }),
      h(ElTableColumn, { prop: 'indicatorKey', label: '字段标识', width: 200 }),
      h(ElTableColumn, { prop: 'category', label: '一级维度', width: 100, align: 'center' }),
      h(ElTableColumn, { label: '维度权重', width: 100, align: 'center' }, {
        default: ({ row }) => {
          const catWeights = { '人力成本': '25%', '装备成本': '25%', '能源成本': '25%', '备件物流': '25%' }
          return catWeights[row.category] || '—'
        }
      }),
      h(ElTableColumn, { label: '二级指标数', width: 100, align: 'center' }, {
        default: ({ row }) => {
          const catCounts = {}
          props.indicators.forEach(ind => {
            catCounts[ind.category] = (catCounts[ind.category] || 0) + 1
          })
          return catCounts[row.category] || 1
        }
      }),
      h(ElTableColumn, { label: '本指标权重', width: 150, align: 'center' }, {
        default: ({ row }) => {
          const weight = props.weights[row.indicatorKey]
          return h(ElInputNumber, {
            modelValue: weight,
            min: 0,
            max: 1,
            step: 0.001,
            precision: 4,
            size: 'small',
            style: { width: '120px' },
            'onUpdate:modelValue': (val) => {
              emit('update:weight', { key: row.indicatorKey, value: val })
            }
          })
        }
      }),
      h(ElTableColumn, { prop: 'indicatorType', label: '指标类型', width: 100, align: 'center' }, {
        default: ({ row }) => h(ElTag, {
          type: row.indicatorType === 'cost' ? 'danger' : 'success',
          size: 'small'
        }, () => row.indicatorType === 'cost' ? '成本型' : '效益型')
      }),
      h(ElTableColumn, { prop: 'unit', label: '单位', width: 80, align: 'center' })
    ])
  }
})

export default {
  components: { IndicatorTable, EditableIndicatorTable }
}
</script>

<style scoped lang="scss">
.cost-effectiveness-page {
  padding: 0;
}

.page-header {
  margin-bottom: 16px;
  h2 {
    display: flex;
    align-items: center;
    gap: 10px;
    margin: 0 0 8px 0;
    font-size: 22px;
    color: #303133;
  }
  .subtitle {
    margin: 0;
    color: #909399;
    font-size: 14px;
  }
}

.hint-alert {
  margin-bottom: 16px;
}

.toolbar-card {
  margin-bottom: 16px;
}

.content-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;

  .header-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }
}

// 理论说明区
.theory-section {
  h4 {
    margin: 16px 0 8px;
    color: #303133;
    font-size: 14px;
  }
  p {
    margin: 6px 0;
    line-height: 1.7;
    color: #606266;
    font-size: 13px;
  }
  .formula-box {
    background: #f5f7fa;
    border: 1px solid #e4e8ef;
    border-radius: 6px;
    padding: 10px 16px;
    margin: 8px 0;
    font-family: 'Consolas', monospace;
    font-size: 13px;
    color: #303133;
    line-height: 1.8;
  }
}

// 成本数据表格
.cost-data-table {
  .cell {
    font-size: 12px;
  }
}

// 成本预览 Tabs
.cost-preview-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 8px;
  }
}

// 成本指标说明图例
.cost-indicator-legend {
  margin-top: 12px;
  padding: 8px 12px;
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 6px;

  .legend-item {
    display: flex;
    flex-direction: column;
    margin-bottom: 4px;
    font-size: 12px;
    line-height: 1.5;

    strong {
      color: #303133;
    }
    .legend-unit {
      color: #909399;
      font-size: 11px;
    }
    .legend-source {
      color: #c0c4cc;
      font-size: 10px;
    }
  }
}

// 结果表格
.result-table {
  .cost-index {
    color: #409EFF;
    font-weight: 500;
  }
  .effect-score {
    color: #67C23A;
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

.cost-breakdown {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: center;

  .cost-item {
    display: inline-flex;
    flex-direction: column;
    align-items: center;
    padding: 2px 6px;
    background: #f5f7fa;
    border-radius: 4px;
    font-size: 10px;

    .cat-label {
      color: #909399;
    }
    .cat-value {
      color: #409EFF;
      font-weight: 500;
    }
  }
}

.chart-hint-alert {
  margin-bottom: 12px;
}

.chart-container {
  width: 100%;
  height: 460px;
}

// 加载状态
// ==================== 新增样式 ====================

// 结果汇总统计卡片
.summary-cards {
  margin-bottom: 16px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e8eef5;

  .summary-card {
    background: #fff;
    border: 1px solid #e4e8ef;
    border-radius: 8px;
    padding: 12px 16px;
    text-align: center;

    .summary-label {
      font-size: 12px;
      color: #909399;
      margin-bottom: 6px;
    }
    .summary-value {
      font-size: 24px;
      font-weight: 700;
      color: #303133;
      line-height: 1.2;
      &.cost-blue { color: #409EFF; }
      &.cost-green { color: #67C23A; }
      &.cost-gold { color: #E6A23C; }
    }
    .summary-range {
      font-size: 11px;
      color: #c0c4cc;
      margin-top: 4px;
    }
  }
}

// 展开行内容
.expand-content {
  padding: 8px 16px;
  background: #fafbfc;

  .expand-section {
    margin-bottom: 16px;

    .expand-section-title {
      font-size: 13px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 8px;
      padding-left: 8px;
      border-left: 3px solid #409EFF;
    }
  }
}

// 展开行内嵌表格
.expand-inner-table {
  font-size: 12px;

  .norm-benefit { color: #67C23A; font-weight: 600; }
  .norm-cost { color: #E6A23C; font-weight: 600; }
}

// 类别汇总卡片
.cat-summary-card {
  background: #fff;
  border: 1px solid #e4e8ef;
  border-radius: 6px;
  padding: 8px 12px;
  text-align: center;
  margin-bottom: 8px;

  .cat-name {
    font-size: 11px;
    color: #909399;
    margin-bottom: 4px;
  }
  .cat-value {
    font-size: 14px;
    font-weight: 600;
    color: #409EFF;
  }
}

// 权重项
.weight-item {
  display: flex;
  justify-content: space-between;
  background: #f5f7fa;
  border: 1px solid #e4e8ef;
  border-radius: 4px;
  padding: 4px 8px;
  margin-bottom: 4px;
  font-size: 11px;

  .weight-key {
    color: #606266;
    font-family: 'Consolas', monospace;
  }
  .weight-val {
    color: #409EFF;
    font-weight: 600;
  }
}

// 指标摘要图例
.cost-indicator-legend {
  .legend-desc {
    font-size: 11px;
    line-height: 1.6;

    code {
      color: #303133;
      font-size: 10px;
      font-family: 'Consolas', monospace;
    }
    .legend-unit {
      color: #909399;
      font-size: 10px;
    }
    .legend-source {
      color: #c0c4cc;
      font-size: 10px;
    }
  }
}


.loading-overlay {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(255, 255, 255, 0.95);
  padding: 24px 40px;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 12px;
  z-index: 1000;

  .el-icon {
    font-size: 24px;
    color: #409EFF;
  }
}

.category-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 12px;
  }
}

// 权重说明
.weight-hint {
  margin-bottom: 12px;
}

.tab-tip {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
  padding: 4px 8px;
  background: #f5f7fa;
  border-radius: 4px;
}

.category-preview-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 8px;
  }
}

.result-block {
  margin-bottom: 20px;

  &:last-child {
    margin-bottom: 0;
  }
}

.result-block-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.result-block-desc {
  font-size: 12px;
  color: #909399;
  margin: 0 0 10px;
  line-height: 1.5;
}

.minmax-table {
  .dir-adjust-yes {
    color: #e6a23c;
    font-size: 12px;
  }
  .dir-adjust-no {
    color: #606266;
    font-size: 12px;
  }
}

.ind-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  line-height: 1.2;

  .dir-tag {
    transform: scale(0.92);
  }
}

.result-summary-table {
  .cat-header {
    font-weight: 600;
  }
  .cat-value-cell {
    font-weight: 500;
  }
}
</style>
