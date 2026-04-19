<template>
  <el-card
    class="ahp-config-card"
    :class="{ 'ahp-config-card--embedded': embedded }"
    :shadow="embedded ? 'never' : 'always'"
  >
    <template v-if="!embedded" #header>
      <div class="ahp-header-wrap">
        <div class="card-header card-header-row1">
          <el-icon class="header-icon"><Setting /></el-icon>
          <span>装备操作 AHP 权重配置</span>
          <el-tag type="warning" effect="plain" style="margin-left: 10px;">仅需输入上三角区域，下三角自动生成倒数</el-tag>
        </div>
        <div class="ahp-header-expert" @click.stop>
          <el-form :inline="true" class="expert-ahp-form expert-ahp-form--header">
            <el-form-item label="当前专家">
              <el-select
                v-model="selectedExpertId"
                placeholder="选择专家（与可信度库 expert_id 一致）"
                clearable
                filterable
                style="width: min(280px, 100%)"
              >
                <el-option
                  v-for="e in expertOptions"
                  :key="e.expertId"
                  :label="`${e.expertName}（ID ${e.expertId}）`"
                  :value="e.expertId"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :disabled="!selectedExpertId" :loading="savingScores" @click="saveScoresToDb">
                保存到数据库
              </el-button>
              <el-button :disabled="!selectedExpertId" :loading="loadingScores" @click="reloadScoresFromDb">
                重新加载
              </el-button>
              <el-button type="success" @click="openSimulateDialog">批量模拟入库</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </template>

    <div class="config-content">
      <!-- embedded：父页统一顶栏只有专家选择，操作按钮在此补全 -->
      <div v-if="embedded && !toolbarInParent" class="ahp-embedded-actions" @click.stop>
        <el-form :inline="true" class="expert-ahp-form expert-ahp-form--embedded-actions">
          <el-form-item>
            <el-button type="primary" :disabled="!selectedExpertId" :loading="savingScores" @click="saveScoresToDb">
              保存到数据库
            </el-button>
            <el-button :disabled="!selectedExpertId" :loading="loadingScores" @click="reloadScoresFromDb">
              重新加载
            </el-button>
            <el-button type="success" @click="openSimulateDialog">批量模拟入库</el-button>
          </el-form-item>
        </el-form>
      </div>
      <el-alert
        v-if="!legendsInParent"
        type="info"
        :closable="false"
        show-icon
        class="matrix-guide compact expert-ahp-hint"
      >
        <template v-if="!selectedExpertId">请先在标题栏选择「当前专家」；选择后将展示维度层与指标层判断矩阵。指标层已合并定性（QL）与定量（QT）指标，同维度内一起比较。</template>
        <template v-else>
          已选择专家：自动加载该专家在「装备操作」域的比较打分；若无打分记录则标度全为 1、把握度均为 {{ defaultBlankConfidence }}（低于 0.6）。
        </template>
      </el-alert>

      <el-empty
        v-if="!selectedExpertId"
        class="ahp-select-expert-empty"
        description="请先在上方的「当前专家」中选择一名专家，选择后将展示维度层与指标层判断矩阵。"
      />

      <!-- 维度层判断矩阵 -->
      <div v-if="selectedExpertId && meta" class="matrix-section">
        <h3 class="section-title">
          <el-icon><Grid /></el-icon>
          维度层判断矩阵（{{ meta.dimensions.length }}×{{ meta.dimensions.length }}，共 {{ meta.dimensionMatrixPairs }} 对上三角比较）
        </h3>

        <el-alert type="info" :closable="false" class="matrix-guide">
          <p class="guide-line">
            <strong>矩阵含义：</strong>第 <em>i</em> 行与第 <em>j</em> 列交叉处（上三角可编辑）表示<strong>行方向要素相对于列方向要素</strong>的重要性标度。
            <strong>重要性</strong>请输入 <strong>{{ AHP_SCORE_MIN.toFixed(4) }}～{{ AHP_SCORE_MAX }}</strong> 内的小数或整数（与 Saaty 标度一致；&lt;1 表示行不如列重要）。
            下三角由系统自动填写为互反倒数（1/标度）。
          </p>
          <p class="guide-line">
            <strong>本层包含的维度：</strong>{{ meta.dimensions.join('、') }}
          </p>
          <p class="guide-line">
            <strong>把握度：</strong>填写 0～1 的小数，表示对该比较判断的可信度（空白默认 {{ defaultBlankConfidence }}）；将随提交一并发送后端备查。
          </p>
        </el-alert>

        <div class="ahp-matrix-scroll">
        <div class="matrix-grid-container matrix-with-confidence">
          <!-- 表头 -->
          <div
            class="matrix-header ahp-matrix-grid-row"
            :style="{ gridTemplateColumns: dimensionMatrixGridTemplate }"
          >
            <div class="matrix-corner"></div>
            <div v-for="(dim, idx) in meta.dimensions" :key="idx" class="matrix-header-cell">
              <span class="header-label">{{ dim }}</span>
            </div>
          </div>

          <!-- 矩阵主体 -->
          <div
            v-for="(rowDim, rowIdx) in meta.dimensions"
            :key="rowDim"
            class="matrix-row ahp-matrix-grid-row"
            :style="{ gridTemplateColumns: dimensionMatrixGridTemplate }"
          >
            <div class="matrix-row-label">
              <span class="header-label">{{ rowDim }}</span>
            </div>
            <div
              v-for="(colDim, colIdx) in meta.dimensions"
              :key="colDim"
              class="matrix-cell"
              :class="{
                'cell-diagonal': rowIdx === colIdx,
                'cell-upper': rowIdx < colIdx,
                'cell-lower': rowIdx > colIdx,
                'cell-editable': rowIdx < colIdx
              }"
            >
              <!-- 对角线 = 1 -->
              <template v-if="rowIdx === colIdx">
                <span class="cell-value">1</span>
              </template>
              <!-- 上三角 = 可编辑 -->
              <template v-else-if="rowIdx < colIdx">
                <div class="cell-inline-inputs" title="行相对列的重要性标度；右侧为把握度">
                  <el-input-number
                    v-model="dimensionMatrix[rowIdx][colIdx]"
                    :min="AHP_SCORE_MIN"
                    :max="AHP_SCORE_MAX"
                    :step="0.01"
                    :precision="4"
                    size="small"
                    :controls="false"
                    class="inline-score ahp-plain-number"
                    @change="(val) => onDimensionCellChange(rowIdx, colIdx, val)"
                  />
                  <span class="inline-conf-label">把握度</span>
                  <el-input-number
                    v-model="dimensionConfidence[rowIdx][colIdx]"
                    :min="0"
                    :max="1"
                    :step="0.05"
                    :precision="2"
                    size="small"
                    :controls="false"
                    class="inline-conf ahp-plain-number"
                  />
                </div>
              </template>
              <!-- 下三角 = 自动生成倒数（只读） -->
              <template v-else>
                <span class="cell-value auto-generated">
                  {{ dimensionMatrix[rowIdx][colIdx].toFixed(4) }}
                </span>
              </template>
            </div>
          </div>
        </div>
        </div>
      </div>

      <!-- 指标层：按维度 Tab 切换 -->
      <div v-if="selectedExpertId && meta" class="matrix-section indicator-layer-section">
        <h3 class="section-title">
          <el-icon><Grid /></el-icon>
          指标层判断矩阵（同维度内定性+定量指标一起比较）
        </h3>

        <el-alert type="info" :closable="false" class="matrix-guide compact">
          与维度层相同：<strong>上三角</strong>为行相对列的标度 <strong>[1/9，9]</strong>（可小于 1）；下三角自动互反。切换 Tab 编辑各维度下属指标。
        </el-alert>

        <el-tabs v-model="activeIndicatorTab" type="border-card" class="ahp-indicator-tabs">
          <el-tab-pane
            v-for="dim in meta.dimensions"
            :key="dim"
            :label="`${dim}（${(meta.indicators[dim] || []).length}×${(meta.indicators[dim] || []).length}）`"
            :name="dim"
          >
            <div v-if="(meta.indicators[dim] || []).length === 0" style="padding: 20px; color: #909399; text-align: center;">
              该维度暂无指标定义
            </div>

            <div v-else class="ahp-matrix-scroll">
            <div class="matrix-grid-container matrix-with-confidence indicator-matrix-wrap">
              <div
                class="matrix-header ahp-matrix-grid-row"
                :style="{ gridTemplateColumns: indicatorMatrixGridTemplate(dim) }"
              >
                <div class="matrix-corner"></div>
                <div
                  v-for="ind in meta.indicators[dim]"
                  :key="ind.name"
                  class="matrix-header-cell small"
                >
                  <el-tooltip
                    :content="ind.description || ind.name"
                    placement="top"
                    :show-after="300"
                  >
                    <span class="header-label">{{ ind.name }}</span>
                  </el-tooltip>
                </div>
              </div>

              <div
                v-for="(rowInd, rowIdx) in meta.indicators[dim]"
                :key="rowInd.name"
                class="matrix-row ahp-matrix-grid-row"
                :style="{ gridTemplateColumns: indicatorMatrixGridTemplate(dim) }"
              >
                <div class="matrix-row-label small">
                  <el-tooltip
                    :content="rowInd.description || rowInd.name"
                    placement="top"
                    :show-after="300"
                  >
                    <span class="header-label">{{ rowInd.name }}</span>
                  </el-tooltip>
                </div>
                <div
                  v-for="(colInd, colIdx) in meta.indicators[dim]"
                  :key="colInd.name"
                  class="matrix-cell small"
                  :class="{
                    'cell-diagonal': rowIdx === colIdx,
                    'cell-upper': rowIdx < colIdx,
                    'cell-lower': rowIdx > colIdx,
                    'cell-editable': rowIdx < colIdx
                  }"
                >
                  <template v-if="rowIdx === colIdx">
                    <span class="cell-value">1</span>
                  </template>
                  <template v-else-if="rowIdx < colIdx">
                    <div class="cell-inline-inputs" title="行相对列的重要性标度；右侧为把握度">
                      <el-input-number
                        v-model="indicatorMatrix[dim][rowIdx][colIdx]"
                        :min="AHP_SCORE_MIN"
                        :max="AHP_SCORE_MAX"
                        :step="0.01"
                        :precision="4"
                        size="small"
                        :controls="false"
                        class="inline-score ahp-plain-number"
                        @change="(val) => onIndicatorCellChange(dim, rowIdx, colIdx, val)"
                      />
                      <span class="inline-conf-label">把握度</span>
                      <el-input-number
                        v-model="indicatorConfidence[dim][rowIdx][colIdx]"
                        :min="0"
                        :max="1"
                        :step="0.05"
                        :precision="2"
                        size="small"
                        :controls="false"
                        class="inline-conf ahp-plain-number"
                      />
                    </div>
                  </template>
                  <template v-else>
                    <span class="cell-value auto-generated">
                      {{ indicatorMatrix[dim][rowIdx][colIdx].toFixed(4) }}
                    </span>
                  </template>
                </div>
              </div>
            </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 操作按钮 -->
      <div v-if="selectedExpertId" class="action-buttons">
        <el-button type="primary" size="large" :loading="calculating" @click="calculateWeights">
          <el-icon><Promotion /></el-icon>
          计算 AHP 权重
        </el-button>

        <el-button size="large" @click="resetToDefault">
          <el-icon><RefreshLeft /></el-icon>
          恢复默认
        </el-button>
      </div>

      <!-- AHP结果展示 -->
      <div v-if="selectedExpertId && result" ref="eqAhpResultSectionRef" class="result-section">
        <!-- 维度层结果 -->
        <div class="result-block">
          <h3 class="section-title">
            <el-icon><PieChart /></el-icon>
            维度层权重结果
          </h3>

          <el-alert
            :title="`最大特征值 λmax = ${result.dimensionResult.lambdaMax.toFixed(4)} | CR = ${result.dimensionResult.cr.toFixed(4)} | ${result.dimensionResult.consistent ? '✓ 通过一致性检验' : '✗ 未通过一致性检验（建议调整比较值）'}`"
            :type="result.dimensionResult.consistent ? 'success' : 'warning'"
            :closable="false"
            show-icon
            style="margin-bottom: 14px;"
          />

          <el-table :data="dimensionResultTable" border size="small" class="dim-result-table">
            <el-table-column prop="name" label="维度" />
            <el-table-column prop="weight" label="权重" align="center">
              <template #default="{ row }">
                <el-tag type="primary" size="large" effect="dark">
                  {{ (row.weight * 100).toFixed(2) }}%
                </el-tag>
              </template>
            </el-table-column>
          </el-table>

          <el-alert type="info" :closable="false" show-icon class="ahp-result-scroll-hint">
            下方还有<strong>指标层</strong>与<strong>装备体系叶子分解总表</strong>、<strong>综合权重</strong>；计算完成后将自动滚至此处，请继续<strong>向下滚动</strong>。
          </el-alert>
        </div>

        <!-- 指标层结果 -->
        <div class="result-block">
          <h3 class="section-title">
            <el-icon><DataAnalysis /></el-icon>
            指标层权重结果（各维度内指标单排序）
          </h3>

          <el-alert type="info" :closable="false" show-icon class="ahp-result-indicator-intro">
            下表为<strong>装备操作</strong>各维度内部对指标的相对权重。
          </el-alert>

          <el-empty
            v-if="!result.indicatorResults || !Object.keys(result.indicatorResults).length"
            description="未返回指标层结果"
          />
          <el-tabs
            v-else
            v-model="eqResultIndicatorTab"
            type="border-card"
            class="ahp-result-indicator-tabs"
          >
            <el-tab-pane
              v-for="(indResult, dimName) in result.indicatorResults"
              :key="String(dimName)"
              :label="`${dimName} · CR ${indResult.cr.toFixed(4)}`"
              :name="String(dimName)"
            >
              <div class="ahp-result-tab-head">
                <el-tag :type="indResult.consistent ? 'success' : 'warning'" size="small">
                  {{ indResult.consistent ? '通过一致性检验' : '未通过一致性检验' }}
                </el-tag>
                <span class="ahp-result-tab-meta">{{ indResult.elementNames?.length || 0 }} 个指标</span>
              </div>
              <el-table :data="formatIndicatorResult(indResult)" border size="small">
                <el-table-column prop="name" label="指标" min-width="200" />
                <el-table-column prop="weight" label="权重（本维度内）" align="center" width="160">
                  <template #default="{ row }">
                    <el-tag type="success" size="small">{{ (row.weight * 100).toFixed(2) }}%</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </div>

        <!-- 装备体系：叶子分解权重总表 -->
        <div v-if="combinedWeightSorted.length" class="result-block">
          <h3 class="section-title">
            <el-icon><Grid /></el-icon>
            装备操作体系叶子分解权重总表（维度×指标）
          </h3>
          <el-alert type="success" :closable="false" show-icon class="ahp-result-total-alert">
            共 <strong>{{ combinedWeightSorted.length }}</strong> 个叶子指标；综合权重之和为
            <strong>{{ equipLeafSumPct.toFixed(4) }}%</strong>（在装备体系内应为 100%）。
          </el-alert>
          <el-table :data="combinedWeightSorted" border size="small" stripe max-height="420" class="combined-total-table">
            <el-table-column type="index" label="#" width="48" align="center" />
            <el-table-column prop="dimension" label="维度" width="150" />
            <el-table-column prop="indicator" label="指标" min-width="180" />
            <el-table-column prop="dimWeight" label="维度权重" width="110" align="center">
              <template #default="{ row }">{{ (row.dimWeight * 100).toFixed(2) }}%</template>
            </el-table-column>
            <el-table-column prop="indWeight" label="指标权重（维内）" width="130" align="center">
              <template #default="{ row }">{{ (row.indWeight * 100).toFixed(2) }}%</template>
            </el-table-column>
            <el-table-column label="综合权重（维×指）" width="150" align="center">
              <template #default="{ row }">
                <el-tag type="danger" size="small" effect="dark">{{ (row.combinedWeight * 100).toFixed(4) }}%</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 综合权重结果 -->
        <div class="result-block">
          <h3 class="section-title">
            <el-icon><Histogram /></el-icon>
            综合权重（维度权重 × 指标权重）
          </h3>

          <el-row :gutter="20" class="combined-weight-row">
            <el-col :xs="24" :lg="14">
              <el-table :data="combinedWeightTable" border size="small" max-height="520" class="combined-weight-table">
                <el-table-column prop="dimension" label="维度" width="150" fixed />
                <el-table-column prop="dimWeight" label="维度权重" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag type="primary" size="small">{{ (row.dimWeight * 100).toFixed(2) }}%</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="indicator" label="指标" min-width="150" />
                <el-table-column prop="indWeight" label="指标权重" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag type="success" size="small">{{ (row.indWeight * 100).toFixed(2) }}%</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="综合权重" width="130" align="center">
                  <template #default="{ row }">
                    <el-tag type="danger" size="large" effect="dark">
                      {{ (row.combinedWeight * 100).toFixed(4) }}%
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-col>
            <el-col :xs="24" :lg="10">
              <div class="combined-sunburst-wrap">
                <div class="combined-sunburst-title">综合权重旭日图（内圈维度 · 外圈指标）</div>
                <div ref="combinedSunburstRef" class="combined-sunburst-chart" />
              </div>
            </el-col>
          </el-row>
        </div>

        <!-- 保存按钮 -->
        <div class="action-buttons result-footer-actions">
          <el-button type="primary" size="large" :loading="savingScores" @click="saveScoresToDb">
            <el-icon><Document /></el-icon>
            保存打分记录
          </el-button>
          <el-button type="success" size="large" @click="exportWeights">
            <el-icon><Download /></el-icon>
            导出权重JSON
          </el-button>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="simulateDialogVisible"
      title="批量模拟装备操作 AHP 打分入库"
      width="520px"
      top="8vh"
      append-to-body
      align-center
      destroy-on-close
      class="ahp-simulate-dialog"
      @open="onSimulateDialogOpen"
    >
      <p class="simulate-dialog-tip">
        为所选多名专家随机生成「把握度」（约 20% 小于 0.6）。
        标度由<strong>随机权重</strong>反推：每层在 [1，9] 上抽样正数 a_i，令比较标度 a_ij = a_i/a_j，
        理论上一致性完美（CI=0）；两位小数舍入后仍校验各矩阵 CR &lt; 0.1。
        数据写入 expert_ahp_comparison_scores 表（装备操作前缀）。
      </p>
      <el-select
        v-model="simulateExpertIds"
        multiple
        filterable
        collapse-tags
        collapse-tags-tooltip
        placeholder="选择多名专家"
        style="width: 100%; margin-bottom: 12px"
      >
        <el-option
          v-for="e in expertOptions"
          :key="e.expertId"
          :label="`${e.expertName}（ID ${e.expertId}）`"
          :value="e.expertId"
        />
      </el-select>
      <template #footer>
        <el-button @click="simulateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="simulating" @click="runSimulateBatch">开始模拟</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted, onUnmounted, watch } from 'vue'
import { Setting, Grid, PieChart, Promotion, RefreshLeft, InfoFilled, Histogram, Download, Document, DataAnalysis } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import {
  getExpertList,
} from '@/api'
import {
  getEquipmentAhpMeta,
  calculateEquipmentAhp,
  getEquipmentAhpScores,
  saveEquipmentAhpScores,
  simulateEquipmentAhpScores,
} from '@/api/equipmentAhp'
import {
  defaultBlankConfidence,
  AHP_SCORE_MIN,
  AHP_SCORE_MAX,
} from '@/config/ahpComparisons'

const AHP_PREFIX = '装备操作_'

const props = defineProps({
  embedded: { type: Boolean, default: false },
  legendsInParent: { type: Boolean, default: false },
  toolbarInParent: { type: Boolean, default: false },
  expertId: { type: [Number, null], default: undefined }
})
const emit = defineEmits(['weights-calculated', 'update:expertId', 'expert-options'])

const localExpertId = ref(null)
const selectedExpertId = computed({
  get() {
    if (props.embedded) return props.expertId ?? null
    return localExpertId.value
  },
  set(v) {
    if (props.embedded) emit('update:expertId', v)
    localExpertId.value = v
  }
})

// 元数据
const meta = ref(null)

// 维度层矩阵
const dimensionMatrix = ref([])
const dimensionConfidence = ref([])

// 指标层矩阵
const indicatorMatrix = ref({})
const indicatorConfidence = ref({})

// 维度矩阵网格模板
const dimensionMatrixGridTemplate = computed(() => {
  if (!meta.value) return ''
  const n = meta.value.dimensions.length
  return `minmax(168px, 220px) repeat(${n}, minmax(240px, 1fr))`
})

function indicatorMatrixGridTemplate(dimName) {
  if (!meta.value || !meta.value.indicators[dimName]) return ''
  const n = meta.value.indicators[dimName].length
  const colMin = n >= 6 ? '220px' : '248px'
  return `minmax(168px, 240px) repeat(${n}, minmax(${colMin}, 1fr))`
}

// 专家相关
const expertOptions = ref([])
const loadingScores = ref(false)
const savingScores = ref(false)
const calculating = ref(false)
const result = ref(null)
const activeIndicatorTab = ref('')
const eqAhpResultSectionRef = ref(null)
const eqResultIndicatorTab = ref('')
const combinedSunburstRef = ref(null)
let combinedSunburstChart = null

const simulateDialogVisible = ref(false)
const simulateExpertIds = ref([])
const simulating = ref(false)

async function fetchMeta() {
  try {
    const data = await getEquipmentAhpMeta(null)
    meta.value = data
    if (meta.value.dimensions.length > 0) {
      activeIndicatorTab.value = meta.value.dimensions[0]
    }
    initMatrices()
    if (selectedExpertId.value) {
      await loadExpertScoresIntoMatrices()
    }
  } catch (e) {
    ElMessage.error('加载装备操作 AHP 元数据失败：' + (e.message || ''))
  }
}

async function fetchExpertOptions() {
  try {
    const list = await getExpertList()
    expertOptions.value = Array.isArray(list) ? list : []
    emit('expert-options', expertOptions.value)
  } catch (e) {
    console.error(e)
    ElMessage.error('加载专家列表失败：' + (e.message || ''))
  }
}

function initMatrices() {
  if (!meta.value) return
  const dims = meta.value.dimensions
  const n = dims.length

  // 初始化维度层矩阵
  dimensionMatrix.value = []
  dimensionConfidence.value = []
  for (let i = 0; i < n; i++) {
    const row = []
    const confRow = []
    for (let j = 0; j < n; j++) {
      if (i === j) {
        row.push(1)
        confRow.push(0)
      } else if (i < j) {
        row.push(1)
        confRow.push(defaultBlankConfidence)
      } else {
        row.push(0)
        confRow.push(0)
      }
    }
    dimensionMatrix.value.push(row)
    dimensionConfidence.value.push(confRow)
  }
  for (let i = 0; i < n; i++) {
    for (let j = i + 1; j < n; j++) {
      dimensionMatrix.value[j][i] = 1 / dimensionMatrix.value[i][j]
    }
  }

  // 初始化指标层矩阵
  indicatorMatrix.value = {}
  indicatorConfidence.value = {}
  for (const dim of dims) {
    const inds = meta.value.indicators[dim] || []
    const m = inds.length
    const matrix = []
    const conf = []
    for (let i = 0; i < m; i++) {
      const row = []
      const confRow = []
      for (let j = 0; j < m; j++) {
        if (i === j) {
          row.push(1)
          confRow.push(0)
        } else if (i < j) {
          row.push(1)
          confRow.push(defaultBlankConfidence)
        } else {
          row.push(0)
          confRow.push(0)
        }
      }
      matrix.push(row)
      conf.push(confRow)
    }
    for (let i = 0; i < m; i++) {
      for (let j = i + 1; j < m; j++) {
        matrix[j][i] = 1 / matrix[i][j]
      }
    }
    indicatorMatrix.value[dim] = matrix
    indicatorConfidence.value[dim] = conf
  }
}

function clampAhpScore(val) {
  let v = Number(val)
  if (!Number.isFinite(v)) v = 1
  v = Math.max(AHP_SCORE_MIN, Math.min(AHP_SCORE_MAX, v))
  return Math.round(v * 10000) / 10000
}

const onDimensionCellChange = (row, col, val) => {
  const v = clampAhpScore(val)
  dimensionMatrix.value[row][col] = v
  dimensionMatrix.value[col][row] = 1 / v
}

const onIndicatorCellChange = (dim, row, col, val) => {
  const v = clampAhpScore(val)
  indicatorMatrix.value[dim][row][col] = v
  indicatorMatrix.value[dim][col][row] = 1 / v
}

async function loadExpertScoresIntoMatrices() {
  if (!selectedExpertId.value) {
    initMatrices()
    result.value = null
    disposeCombinedSunburst()
    return
  }
  loadingScores.value = true
  try {
    const rows = await getEquipmentAhpScores(selectedExpertId.value)
    if (!rows || rows.length === 0) {
      initMatrices()
      result.value = null
      disposeCombinedSunburst()
    } else {
      applyDbRowsToMatrices(rows)
    }
  } catch (e) {
    ElMessage.error('加载失败：' + (e.message || ''))
  } finally {
    loadingScores.value = false
  }
}

function applyDbRowsToMatrices(rows) {
  if (!meta.value) return
  initMatrices()
  const map = new Map(rows.map((r) => [r.comparisonKey, r]))
  const dims = meta.value.dimensions
  const n = dims.length

  // 维度层
  for (let i = 0; i < n; i++) {
    for (let j = i + 1; j < n; j++) {
      const key = AHP_PREFIX + dims[i] + '_' + dims[j]
      const r = map.get(key)
      if (!r) continue
      const sc = clampAhpScore(Number(r.score))
      dimensionMatrix.value[i][j] = sc
      dimensionMatrix.value[j][i] = 1 / sc
      const cf = r.confidence != null && r.confidence !== '' ? Number(r.confidence) : defaultBlankConfidence
      dimensionConfidence.value[i][j] = Math.min(1, Math.max(0, cf))
    }
  }

  // 指标层
  for (const dim of dims) {
    const inds = meta.value.indicators[dim] || []
    if (!inds.length) continue
    const matrix = indicatorMatrix.value[dim]
    const conf = indicatorConfidence.value[dim]
    for (let i = 0; i < inds.length; i++) {
      for (let j = i + 1; j < inds.length; j++) {
        const key = AHP_PREFIX + dim + '_' + inds[i].name + '_' + inds[j].name
        const r = map.get(key)
        if (!r) continue
        const sc = clampAhpScore(Number(r.score))
        matrix[i][j] = sc
        matrix[j][i] = 1 / sc
        const cf = r.confidence != null && r.confidence !== '' ? Number(r.confidence) : defaultBlankConfidence
        conf[i][j] = Math.min(1, Math.max(0, cf))
      }
    }
  }
}

async function reloadScoresFromDb() {
  await loadExpertScoresIntoMatrices()
}

watch(
  selectedExpertId,
  (id) => {
    if (id && meta.value?.dimensions?.length) {
      activeIndicatorTab.value = meta.value.dimensions[0]
    }
    void loadExpertScoresIntoMatrices()
  },
  { immediate: true }
)

function openSimulateDialog() {
  simulateDialogVisible.value = true
  if (simulateExpertIds.value.length === 0 && selectedExpertId.value) {
    simulateExpertIds.value = [selectedExpertId.value]
  }
}

function onSimulateDialogOpen() {
  if (simulateExpertIds.value.length === 0) {
    simulateExpertIds.value = expertOptions.value.map((e) => e.expertId)
  }
}

async function runSimulateBatch() {
  if (!simulateExpertIds.value.length) {
    ElMessage.warning('请至少选择一名专家')
    return
  }
  simulating.value = true
  try {
    const res = await simulateEquipmentAhpScores({
      expertIds: simulateExpertIds.value
    })
    ElMessage.success(`已写入 ${res.insertedCount} 名专家的模拟打分`)
    simulateDialogVisible.value = false
    if (selectedExpertId.value && simulateExpertIds.value.includes(selectedExpertId.value)) {
      await loadExpertScoresIntoMatrices()
    }
  } catch (e) {
    ElMessage.error('模拟失败：' + (e.message || ''))
  } finally {
    simulating.value = false
  }
}

function buildPersistPayload() {
  if (!meta.value || !selectedExpertId.value) return null
  const dims = meta.value.dimensions
  const dimensionEntries = []
  for (let i = 0; i < dims.length; i++) {
    for (let j = i + 1; j < dims.length; j++) {
      dimensionEntries.push({
        key: AHP_PREFIX + dims[i] + '_' + dims[j],
        score: dimensionMatrix.value[i][j],
        confidence: dimensionConfidence.value[i][j] ?? defaultBlankConfidence
      })
    }
  }
  const indicatorEntries = {}
  for (const dim of dims) {
    const inds = meta.value.indicators[dim] || []
    const entries = []
    for (let i = 0; i < inds.length; i++) {
      for (let j = i + 1; j < inds.length; j++) {
        entries.push({
          key: AHP_PREFIX + dim + '_' + inds[i].name + '_' + inds[j].name,
          score: indicatorMatrix.value[dim][i][j],
          confidence: indicatorConfidence.value[dim][i][j] ?? defaultBlankConfidence
        })
      }
    }
    indicatorEntries[dim] = entries
  }
  const name = expertOptions.value.find((e) => e.expertId === selectedExpertId.value)?.expertName || ''
  return {
    expertId: selectedExpertId.value,
    expertName: name,
    dimensionMatrix: dimensionEntries,
    indicatorMatrices: indicatorEntries
  }
}

async function saveScoresToDb() {
  if (!selectedExpertId.value) return
  savingScores.value = true
  try {
    const payload = buildPersistPayload()
    const n = await saveEquipmentAhpScores(payload)
    ElMessage.success(`已保存 ${n} 条比较打分`)
  } catch (e) {
    ElMessage.error('保存失败：' + (e.message || ''))
  } finally {
    savingScores.value = false
  }
}

const calculateWeights = async () => {
  calculating.value = true
  try {
    const dims = meta.value.dimensions
    const dimensionEntries = []
    for (let i = 0; i < dims.length; i++) {
      for (let j = i + 1; j < dims.length; j++) {
        dimensionEntries.push({
          key: AHP_PREFIX + dims[i] + '_' + dims[j],
          score: dimensionMatrix.value[i][j],
          confidence: dimensionConfidence.value[i][j] ?? defaultBlankConfidence
        })
      }
    }
    const indicatorEntries = {}
    for (const dim of dims) {
      const inds = meta.value.indicators[dim] || []
      const entries = []
      for (let i = 0; i < inds.length; i++) {
        for (let j = i + 1; j < inds.length; j++) {
          entries.push({
            key: AHP_PREFIX + dim + '_' + inds[i].name + '_' + inds[j].name,
            score: indicatorMatrix.value[dim][i][j],
            confidence: indicatorConfidence.value[dim][i][j] ?? defaultBlankConfidence
          })
        }
      }
      indicatorEntries[dim] = entries
    }
    const response = await calculateEquipmentAhp({
      dimensionMatrix: dimensionEntries,
      indicatorMatrices: indicatorEntries
    })
    result.value = response
    ElMessage.success('AHP权重计算成功！')
    await nextTick()
    renderCombinedSunburst()
    await nextTick()
    eqAhpResultSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    emit('weights-calculated', {
      dimensionResult: result.value.dimensionResult,
      indicatorResults: result.value.indicatorResults,
      combinedWeights: combinedWeightTable.value
    })
  } catch (error) {
    ElMessage.error('计算失败：' + error.message)
    console.error('[ERROR] AHP计算失败:', error)
  } finally {
    calculating.value = false
  }
}

const resetToDefault = () => {
  initMatrices()
  result.value = null
  disposeCombinedSunburst()
}

const dimensionResultTable = computed(() => {
  if (!result.value || !result.value.dimensionResult) return []
  return result.value.dimensionResult.elementNames.map((name, idx) => ({
    name,
    weight: result.value.dimensionResult.weights[idx]
  }))
})

const formatIndicatorResult = (indResult) => {
  const names = indResult?.elementNames
  const weights = indResult?.weights
  if (!names?.length || !weights?.length) return []
  return names.map((name, idx) => ({
    name,
    weight: weights[idx]
  }))
}

const combinedWeightTable = computed(() => {
  if (!result.value?.dimensionResult?.elementNames?.length) return []
  const table = []
  const dimResult = result.value.dimensionResult
  const indMap = result.value.indicatorResults || {}
  for (let dIdx = 0; dIdx < dimResult.elementNames.length; dIdx++) {
    const dimName = dimResult.elementNames[dIdx]
    const dimWeight = dimResult.weights[dIdx]
    const indResult = indMap[dimName]
    if (!indResult) continue
    for (let iIdx = 0; iIdx < indResult.elementNames.length; iIdx++) {
      const indName = indResult.elementNames[iIdx]
      const indWeight = indResult.weights[iIdx]
      const combinedWeight = dimWeight * indWeight
      table.push({
        dimension: dimName,
        dimWeight,
        indicatorKey: indName,
        indicator: indName,
        indWeight,
        combinedWeight
      })
    }
  }
  return table
})

const combinedWeightSorted = computed(() => {
  const rows = combinedWeightTable.value
  if (!rows?.length) return []
  return [...rows].sort((a, b) => (b.combinedWeight || 0) - (a.combinedWeight || 0))
})

const equipLeafSumPct = computed(() =>
  combinedWeightTable.value.reduce((s, r) => s + (Number(r.combinedWeight) || 0), 0) * 100
)

watch(
  () => result.value?.indicatorResults,
  (ir) => {
    if (!ir || typeof ir !== 'object') {
      eqResultIndicatorTab.value = ''
      return
    }
    const keys = Object.keys(ir)
    if (!keys.length) {
      eqResultIndicatorTab.value = ''
      return
    }
    if (!keys.includes(eqResultIndicatorTab.value)) {
      eqResultIndicatorTab.value = keys[0]
    }
  },
  { immediate: true }
)

const SUNBURST_DIM_COLORS = ['#0074D9', '#39CCCC', '#3D9970', '#FF851B', '#FFDC00', '#B10DC9', '#85144b']

function buildCombinedSunburstData() {
  if (!result.value?.dimensionResult?.elementNames?.length) return null
  const byDim = new Map()
  for (const row of combinedWeightTable.value) {
    if (!byDim.has(row.dimension)) byDim.set(row.dimension, [])
    byDim.get(row.dimension).push({
      name: row.indicator,
      value: Math.max(1e-6, row.combinedWeight * 100),
      indicatorTooltipLine: `${row.dimension} · ${row.indicator}`
    })
  }
  const children = []
  let c = 0
  for (const dimName of result.value.dimensionResult.elementNames) {
    const list = byDim.get(dimName)
    if (!list?.length) continue
    const base = SUNBURST_DIM_COLORS[c % SUNBURST_DIM_COLORS.length]
    children.push({
      name: dimName,
      itemStyle: { color: base },
      children: list.map((leaf, i) => ({
        name: leaf.name,
        value: leaf.value,
        indicatorTooltipLine: leaf.indicatorTooltipLine,
        itemStyle: { color: echarts.color.lift(base, (i % 4) * 0.08) }
      }))
    })
    c++
  }
  if (!children.length) return null
  return {
    name: '',
    itemStyle: { color: '#001f3f' },
    children
  }
}

function disposeCombinedSunburst() {
  if (combinedSunburstChart) {
    combinedSunburstChart.dispose()
    combinedSunburstChart = null
  }
}

function renderCombinedSunburst() {
  disposeCombinedSunburst()
  if (!combinedSunburstRef.value || !result.value) return
  const root = buildCombinedSunburstData()
  if (!root) return
  combinedSunburstChart = echarts.init(combinedSunburstRef.value)
  combinedSunburstChart.setOption({
    tooltip: {
      trigger: 'item',
      confine: true,
      formatter(p) {
        const v = p.value
        const pct = typeof v === 'number' ? `${v.toFixed(4)}%` : String(p.value)
        const tipLine = p.data?.indicatorTooltipLine
        if (p.treePathInfo.length >= 3 && tipLine) {
          return `<div style="max-width:280px;line-height:1.6">${tipLine}<br/>占全体：<b>${pct}</b></div>`
        }
        const path = p.treePathInfo.map((x) => x.name).filter(Boolean).slice(1).join(' → ')
        return `<div style="max-width:280px;line-height:1.6">${path}<br/>占全体：<b>${pct}</b></div>`
      }
    },
    series: [
      {
        type: 'sunburst',
        data: [root],
        radius: ['12%', '90%'],
        sort: 'desc',
        nodeClick: false,
        emphasis: {
          focus: 'ancestor',
          itemStyle: { shadowBlur: 8, shadowColor: 'rgba(0,0,0,0.18)' }
        },
        itemStyle: {
          borderRadius: 2,
          borderWidth: 1,
          borderColor: 'rgba(255,255,255,0.92)'
        },
        levels: [
          { r0: '0%', r: '12%', label: { show: false }, itemStyle: { color: '#001f3f', borderWidth: 0 } },
          {
            r0: '12%',
            r: '34%',
            label: {
              show: true,
              rotate: 'radial',
              minAngle: 8,
              fontSize: 11,
              fontWeight: 'bold',
              color: '#fff',
              textBorderColor: 'rgba(0,31,63,0.5)',
              textBorderWidth: 1.5,
              position: 'inside',
              align: 'center',
              verticalAlign: 'middle',
              overflow: 'truncate',
              width: 72
            },
            itemStyle: { borderWidth: 1 }
          },
          { r0: '34%', r: '90%', label: { show: false }, itemStyle: { borderWidth: 1 } }
        ]
      }
    ]
  })
}

function onEqAhpChartsResize() {
  combinedSunburstChart?.resize()
}

const exportWeights = () => {
  if (!result.value) return
  const data = {
    dimensionWeights: {},
    indicatorWeights: {},
    combinedWeights: [],
    scope: '装备操作'
  }
  dimensionResultTable.value.forEach(item => {
    data.dimensionWeights[item.name] = item.weight
  })
  for (const [dimName, indResult] of Object.entries(result.value.indicatorResults)) {
    data.indicatorWeights[dimName] = {}
    indResult.elementNames.forEach((name, idx) => {
      data.indicatorWeights[dimName][name] = indResult.weights[idx]
    })
  }
  combinedWeightTable.value.forEach(item => {
    data.combinedWeights.push({
      dimension: item.dimension,
      dimensionWeight: item.dimWeight,
      indicator: item.indicator,
      indicatorWeight: item.indWeight,
      combinedWeight: item.combinedWeight
    })
  })
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `equipment_ahp_weights_${Date.now()}.json`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('权重已导出为JSON文件')
}

watch(
  () => [selectedExpertId.value, result.value],
  () => {
    if (!selectedExpertId.value || !result.value) {
      disposeCombinedSunburst()
    }
  }
)

onMounted(() => {
  fetchMeta()
  fetchExpertOptions()
  window.addEventListener('resize', onEqAhpChartsResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', onEqAhpChartsResize)
  disposeCombinedSunburst()
})

defineExpose({
  saveScoresToDb,
  reloadScoresFromDb,
  openSimulateDialog
})
</script>

<style scoped lang="scss">
.ahp-config-card {
  margin-bottom: 20px;

  &.ahp-config-card--embedded {
    margin-bottom: 0;
    border: none;
    box-shadow: none !important;

    :deep(.el-card__body) {
      padding: 0;
    }
  }

  :deep(.el-card__header) {
    padding-bottom: 14px;
  }

  .ahp-header-wrap {
    width: 100%;
  }

  .card-header-row1 {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 18px;
    flex-wrap: wrap;

    .header-icon {
      font-size: 24px;
      color: var(--accent-gold);
    }
  }

  .ahp-header-expert {
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px solid var(--el-border-color-lighter);
    width: 100%;
  }

  .expert-ahp-form--header {
    margin-bottom: 0;
    width: 100%;
    flex-wrap: wrap;
    row-gap: 4px;
  }
}

.config-content {
  .ahp-embedded-actions {
    padding: 0 0 12px;
    margin-bottom: 8px;
    border-bottom: 1px dashed var(--el-border-color-lighter);

    .expert-ahp-form--embedded-actions {
      margin-bottom: 0;
      flex-wrap: wrap;
      row-gap: 8px;
    }
  }

  .ahp-select-expert-empty {
    padding: 28px 16px 8px;
  }

  .expert-ahp-hint {
    margin-bottom: 16px;
  }

  .ahp-matrix-scroll {
    display: block;
    width: 100%;
    overflow-x: auto;
    padding-bottom: 6px;
  }

  .section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 15px;
    color: var(--navy-primary);
    font-size: 15px;
    font-weight: bold;
    padding-bottom: 10px;
    border-bottom: 2px solid var(--navy-light);
  }

  .matrix-section {
    margin-bottom: 30px;
    padding: 20px;
    background: #fafbfc;
    border-radius: 8px;
    border: 1px solid #e4e7ed;
  }

  .matrix-grid-container {
    display: inline-block;
    background: white;
    border: 2px solid var(--navy-light);
    border-radius: 8px;
    overflow: hidden;
  }

  .matrix-grid-container.matrix-with-confidence {
    display: block;
    width: 100%;
    max-width: 100%;
    overflow-x: auto;
    overflow-y: visible;
  }

  .matrix-guide {
    margin-bottom: 14px;

    &.compact {
      margin-bottom: 10px;
      padding: 8px 12px;
    }

    .guide-line {
      margin: 0 0 8px;
      line-height: 1.55;
      font-size: 13px;

      &:last-child {
        margin-bottom: 0;
      }
    }
  }

  .matrix-header {
    display: flex;
    background: linear-gradient(135deg, var(--navy-primary), var(--navy-secondary));
    color: white;

    .matrix-corner {
      width: 80px;
      height: 50px;
      border-right: 1px solid rgba(255,255,255,0.3);
    }

    .matrix-header-cell {
      width: 100px;
      height: 50px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: bold;
      font-size: 13px;
      border-right: 1px solid rgba(255,255,255,0.3);

      &:last-child {
        border-right: none;
      }

      &.small {
        width: 90px;
        font-size: 11px;
      }
    }
  }

  .matrix-row {
    display: flex;
    border-bottom: 1px solid #e4e7ed;

    &:last-child {
      border-bottom: none;
    }

    .matrix-row-label {
      width: 80px;
      height: 45px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: bold;
      font-size: 13px;
      background: #f5f7fa;
      border-right: 1px solid #e4e7ed;

      &.small {
        width: 90px;
        font-size: 11px;
      }
    }
  }

  .matrix-cell {
    width: 100px;
    height: 45px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-right: 1px solid #e4e7ed;

    &:last-child {
      border-right: none;
    }

    &.cell-diagonal {
      background: #f0f9ff;
      .cell-value {
        font-weight: bold;
        color: var(--navy-secondary);
      }
    }

    &.cell-upper {
      background: #fff;
    }

    &.cell-lower {
      background: #f5f7fa;
    }

    &.cell-editable :deep(.el-input-number) {
      width: 75px;
    }

    &.small {
      width: 90px;

      &.cell-editable :deep(.el-input-number) {
        width: 65px;
      }
    }

    .cell-value {
      font-size: 13px;

      &.auto-generated {
        color: #999;
        font-style: italic;
        font-size: 11px;
      }
    }
  }

  .matrix-with-confidence .ahp-matrix-grid-row {
    display: grid;
    width: 100%;
    min-width: min-content;
    box-sizing: border-box;
    align-items: stretch;
    column-gap: 0;
    border-bottom: 1px solid #e4e7ed;

    .matrix-corner,
    .matrix-row-label,
    .matrix-header-cell,
    .matrix-cell {
      flex: unset !important;
      width: auto !important;
      min-width: 0;
      max-width: none !important;
      box-sizing: border-box;
    }

    .matrix-corner {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 52px;
      border-right: 1px solid rgba(255, 255, 255, 0.3);
    }

    .matrix-header-cell {
      display: flex;
      align-items: center;
      justify-content: center;
      text-align: center;
      min-height: 52px;
      padding: 10px 12px;
      line-height: 1.35;
      border-right: 1px solid rgba(255, 255, 255, 0.3);

      &:last-child {
        border-right: none;
      }

      &.small {
        font-size: 11px;
        padding: 10px 8px;
      }
    }

    .matrix-row-label {
      display: flex;
      align-items: center;
      justify-content: center;
      text-align: center;
      min-height: var(--ahp-row-min-h, 58px);
      padding: 10px 12px;
      line-height: 1.35;
      font-weight: bold;
      font-size: 13px;
      background: #f5f7fa;
      border-right: 1px solid #e4e7ed;

      &.small {
        font-size: 11px;
        padding: 10px 8px;
      }
    }

    .matrix-cell {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: var(--ahp-row-min-h, 58px);
      padding: 10px 12px;
      border-right: 1px solid #e4e7ed;

      &:last-child {
        border-right: none;
      }

      &.small {
        font-size: 11px;
        padding: 10px 8px;
      }
    }
  }

  .matrix-with-confidence .matrix-header.ahp-matrix-grid-row {
    background: linear-gradient(135deg, var(--navy-primary), var(--navy-secondary));
    color: #fff;
    border-bottom: 1px solid #e4e7ed;
  }

  .matrix-with-confidence .matrix-row.ahp-matrix-grid-row {
    background: #fff;
  }

  .matrix-with-confidence .matrix-cell .cell-inline-inputs {
    display: grid;
    grid-template-columns: minmax(72px, 1fr) min-content 58px;
    column-gap: 6px;
    align-items: center;
    justify-content: center;
    width: fit-content;
    max-width: 100%;
    min-width: 0;
    margin: 0 auto;
    box-sizing: border-box;
    padding: 2px 4px;
  }

  .cell-inline-inputs .inline-conf-label {
    font-size: 11px;
    color: #909399;
    white-space: nowrap;
    user-select: none;
    text-align: center;
    line-height: 1.2;
    padding: 0 2px;
  }

  .ahp-plain-number :deep(.el-input-number__increase),
  .ahp-plain-number :deep(.el-input-number__decrease) {
    display: none !important;
  }

  .ahp-plain-number :deep(.el-input-number .el-input__inner) {
    text-align: center;
    font-size: 12px;
  }

  .indicator-layer-section {
    .ahp-indicator-tabs {
      margin-top: 8px;
      border-radius: 8px;
      overflow: hidden;
      box-shadow: 0 1px 8px rgba(0, 31, 63, 0.08);
    }

    :deep(.el-tabs__header) {
      margin: 0;
      background: #f5f7fa;
    }

    :deep(.el-tabs__item) {
      font-weight: 500;
    }

    :deep(.el-tabs__item.is-active) {
      color: var(--navy-primary);
      font-weight: 600;
    }

    :deep(.el-tabs__content) {
      padding: 16px;
      background: #fff;
    }
  }

  .action-buttons {
    display: flex;
    justify-content: center;
    gap: 20px;
    margin: 30px 0;
  }

  .ahp-result-scroll-hint {
    margin: 12px 0 0;
  }

  .ahp-result-indicator-intro {
    margin-bottom: 12px;
  }

  .ahp-result-indicator-tabs {
    margin-top: 4px;

    :deep(.el-tabs__content) {
      padding-top: 12px;
    }
  }

  .ahp-result-tab-head {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 10px;
  }

  .ahp-result-tab-meta {
    font-size: 12px;
    color: #909399;
  }

  .ahp-result-total-alert {
    margin-bottom: 12px;
  }

  .combined-total-table {
    width: 100%;
  }

  .result-section {
    margin-top: 30px;
    padding-top: 20px;
    border-top: 2px dashed var(--navy-light);

    .result-block {
      margin-bottom: 30px;
      padding: 20px;
      background: #fafbfc;
      border-radius: 8px;
      border: 1px solid #e4e7ed;
    }

    .combined-weight-row {
      align-items: stretch;
    }

    .combined-sunburst-wrap {
      display: flex;
      flex-direction: column;
      min-height: 400px;
      padding: 12px 8px 16px;
      background: #fff;
      border: 1px solid #e4e7ed;
      border-radius: 8px;
    }

    .combined-sunburst-title {
      flex-shrink: 0;
      margin-bottom: 4px;
      text-align: center;
      font-size: 13px;
      font-weight: 600;
      color: var(--navy-primary);
    }

    .combined-sunburst-chart {
      flex: 1;
      width: 100%;
      min-height: 380px;
    }

    .result-footer-actions {
      margin-top: 8px;
    }
  }

  .simulate-dialog-tip {
    margin: 0 0 14px;
    font-size: 13px;
    line-height: 1.55;
    color: #606266;
  }

  :deep(.el-collapse-item__header) {
    background: linear-gradient(135deg, var(--navy-primary), var(--navy-secondary));
    color: white;
    font-weight: bold;
    border-radius: 8px;
    padding: 12px 20px;
    margin-bottom: 10px;
  }

  :deep(.el-collapse-item__content) {
    padding: 15px;
  }

  :deep(.el-collapse-item__wrap) {
    background: transparent;
    border: none;
  }

  :deep(.el-input-number .el-input__inner) {
    text-align: center;
    font-size: 12px;
  }
}
</style>

<style lang="scss">
.ahp-simulate-dialog .el-dialog__body {
  max-height: min(420px, 62vh);
  overflow-y: auto;
  box-sizing: border-box;
}
</style>
