<template>
  <el-card class="ahp-config-card">
    <template #header>
      <div class="card-header">
        <el-icon class="header-icon"><Setting /></el-icon>
        <span>专家AHP权重配置</span>
        <el-tag type="warning" effect="plain" style="margin-left: 10px;">仅需输入上三角区域，下三角自动生成倒数</el-tag>
      </div>
    </template>

    <div class="config-content">
      <!-- 标度说明（1、3、5、7、9 及中间值 2、4、6、8） -->
      <div class="scale-section scale-legend-block">
        <h3 class="scale-legend-title">
          <el-icon><InfoFilled /></el-icon>
          AHP 判断标度说明（Saaty 1～9 标度）
        </h3>
        <p class="scale-legend-intro">
          上三角单元格中「重要性」请填写 <strong>1～9 的整数</strong>：表示<strong>行要素相对列要素</strong>的重要程度；<strong>1</strong> 表示同等重要，数值越大行相对列越重要。
          下三角由系统自动填写为互反倒数。标度 <strong>2、4、6、8</strong> 为相邻整数标度的折中判断，若仅需粗粒度判断可直接选用 1、3、5、7、9。
        </p>
        <el-table :data="ahpScaleFullLegend" border stripe size="small" class="scale-legend-table">
          <el-table-column prop="scale" label="标度" width="110" align="center" />
          <el-table-column prop="meaning" label="含义" min-width="100" />
          <el-table-column prop="note" label="说明" min-width="200" />
          <el-table-column prop="scene" label="适用场景" min-width="180" />
        </el-table>
      </div>

      <!-- 把握度等级与判断可信度 λ -->
      <div class="scale-section scale-legend-block confidence-legend-block">
        <h3 class="scale-legend-title">
          <el-icon><InfoFilled /></el-icon>
          把握度等级与判断可信度 λ
        </h3>
        <p class="scale-legend-intro">
          矩阵上三角中「把握度」为 0～1 的小数，表示您对该次<strong>重要性比较</strong>的可信程度，可按下表<strong>判断可信度 λ</strong>取值：
          1 级对应 λ=1，2 级对应 0.8，3 级对应 0.6；4 级表示不能确认，建议 λ 取 <strong>小于 0.6</strong> 的数值（如 0.5、0.4）。
        </p>
        <el-table :data="ahpConfidenceLevelLegend" border stripe size="small" class="scale-legend-table">
          <el-table-column prop="level" label="等级" width="72" align="center" />
          <el-table-column prop="meaning" label="等级含义" width="100" />
          <el-table-column prop="standard" label="等级标准" min-width="280" />
          <el-table-column prop="lambda" label="判断可信度 λ" width="120" align="center" />
        </el-table>
      </div>

      <!-- 维度层判断矩阵 -->
      <div class="matrix-section">
        <h3 class="section-title">
          <el-icon><Grid /></el-icon>
          维度层判断矩阵（5×5，共 10 对上三角比较）
        </h3>

        <el-alert type="info" :closable="false" class="matrix-guide">
          <p class="guide-line">
            <strong>矩阵含义：</strong>第 <em>i</em> 行与第 <em>j</em> 列交叉处（上三角可编辑）表示<strong>行方向要素相对于列方向要素</strong>的重要性标度。
            <strong>重要性请输入 1～9 的整数</strong>（与上方标度表一致；2、4、6、8 为折中，可手填整数实现）。
            下三角由系统自动填写为互反倒数（1/标度）。
          </p>
          <p class="guide-line">
            <strong>本层包含的五个评估维度：</strong>安全性、可靠性、传输能力、抗干扰能力、效能影响（与效能评估指标体系一致）。
            表头与行名可悬停查看各维度释义。
          </p>
          <p class="guide-line">
            <strong>把握度：</strong>填写 0～1 的小数，表示对该比较判断的可信度（参照上方<strong>把握度等级与 λ</strong>表，默认 {{ defaultConfidence }}）；将随提交一并发送后端备查，权重计算仍以重要性整数为准。
          </p>
        </el-alert>

        <el-descriptions :column="2" border size="small" class="field-descriptions">
          <el-descriptions-item
            v-for="d in dimensions"
            :key="d.code"
            :label="d.name"
            label-class-name="desc-label-narrow"
          >
            {{ dimensionDescriptions[d.code] }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="ahp-matrix-scroll">
        <div class="matrix-grid-container matrix-with-confidence">
          <!-- 表头 -->
          <div
            class="matrix-header ahp-matrix-grid-row"
            :style="{ gridTemplateColumns: dimensionMatrixGridTemplate }"
          >
            <div class="matrix-corner"></div>
            <div v-for="dim in dimensions" :key="dim.code" class="matrix-header-cell">
              <el-tooltip :content="dimensionDescriptions[dim.code]" placement="top" :show-after="400">
                <span class="header-label">{{ dim.name }}</span>
              </el-tooltip>
            </div>
          </div>

          <!-- 矩阵主体 -->
          <div
            v-for="(rowDim, rowIdx) in dimensions"
            :key="rowDim.code"
            class="matrix-row ahp-matrix-grid-row"
            :style="{ gridTemplateColumns: dimensionMatrixGridTemplate }"
          >
            <div class="matrix-row-label">
              <el-tooltip :content="dimensionDescriptions[rowDim.code]" placement="top" :show-after="400">
                <span class="header-label">{{ rowDim.name }}</span>
              </el-tooltip>
            </div>
            <div
              v-for="(colDim, colIdx) in dimensions"
              :key="colDim.code"
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
                    :min="1"
                    :max="9"
                    :step="1"
                    :precision="0"
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
                  {{ dimensionMatrix[rowIdx][colIdx].toFixed(2) }}
                </span>
              </template>
            </div>
          </div>
        </div>
        </div>
      </div>

      <!-- 指标层：按维度 Tab 切换 -->
      <div class="matrix-section indicator-layer-section">
        <h3 class="section-title">
          <el-icon><Grid /></el-icon>
          指标层判断矩阵
        </h3>

        <el-alert type="info" :closable="false" class="matrix-guide compact">
          与维度层相同：<strong>上三角</strong>为<strong>行指标相对列指标</strong>的整数标度（1～9）；下三角自动互反。标度与<strong>把握度</strong>同一行排列，无步进按钮。切换 Tab 编辑各维度下属指标。
        </el-alert>

        <el-tabs type="border-card" class="ahp-indicator-tabs">
          <el-tab-pane
            v-for="dim in dimensions"
            :key="dim.code"
            :label="`${dim.name}（${indicatorElements[dim.code].length}×${indicatorElements[dim.code].length}）`"
            :name="dim.code"
          >
            <el-descriptions
              v-if="indicatorMetaByDimension[dim.code]?.length"
              :column="1"
              border
              size="small"
              class="field-descriptions indicator-desc"
            >
              <el-descriptions-item
                v-for="meta in indicatorMetaByDimension[dim.code]"
                :key="meta.key"
                :label="`${meta.code} ${meta.shortLabel}`"
              >
                {{ meta.description }}
              </el-descriptions-item>
            </el-descriptions>

            <div class="ahp-matrix-scroll">
            <div class="matrix-grid-container matrix-with-confidence indicator-matrix-wrap">
              <div
                class="matrix-header ahp-matrix-grid-row"
                :style="{ gridTemplateColumns: indicatorMatrixGridTemplate(dim.code) }"
              >
                <div class="matrix-corner"></div>
                <div
                  v-for="ind in indicatorElements[dim.code]"
                  :key="ind"
                  class="matrix-header-cell small"
                >
                  <el-tooltip
                    :content="indicatorTooltipText(dim.code, ind)"
                    placement="top"
                    :show-after="300"
                  >
                    <span class="header-label">{{ indicatorDisplayLabel(dim.code, ind) }}</span>
                  </el-tooltip>
                </div>
              </div>

              <div
                v-for="(rowInd, rowIdx) in indicatorElements[dim.code]"
                :key="rowInd"
                class="matrix-row ahp-matrix-grid-row"
                :style="{ gridTemplateColumns: indicatorMatrixGridTemplate(dim.code) }"
              >
                <div class="matrix-row-label small">
                  <el-tooltip
                    :content="indicatorTooltipText(dim.code, rowInd)"
                    placement="top"
                    :show-after="300"
                  >
                    <span class="header-label">{{ indicatorDisplayLabel(dim.code, rowInd) }}</span>
                  </el-tooltip>
                </div>
                <div
                  v-for="(colInd, colIdx) in indicatorElements[dim.code]"
                  :key="colInd"
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
                        v-model="indicatorMatrix[dim.code][rowIdx][colIdx]"
                        :min="1"
                        :max="9"
                        :step="1"
                        :precision="0"
                        size="small"
                        :controls="false"
                        class="inline-score ahp-plain-number"
                        @change="(val) => onIndicatorCellChange(dim.code, rowIdx, colIdx, val)"
                      />
                      <span class="inline-conf-label">把握度</span>
                      <el-input-number
                        v-model="indicatorConfidence[dim.code][rowIdx][colIdx]"
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
                      {{ indicatorMatrix[dim.code][rowIdx][colIdx].toFixed(2) }}
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
      <div class="action-buttons">
        <el-button type="primary" size="large" :loading="calculating" @click="calculateWeights">
          <el-icon><Promotion /></el-icon>
          计算AHP权重
        </el-button>

        <el-button size="large" @click="resetToDefault">
          <el-icon><RefreshLeft /></el-icon>
          恢复默认
        </el-button>
      </div>

      <!-- AHP结果展示 -->
      <div v-if="result" class="result-section">
        <!-- 维度层结果 -->
        <div class="result-block">
          <h3 class="section-title">
            <el-icon><PieChart /></el-icon>
            维度层权重结果
          </h3>

          <el-alert
            :title="`一致性比率 CR = ${result.dimensionResult.cr.toFixed(4)} ${result.dimensionResult.consistent ? '✓ 通过一致性检验' : '✗ 未通过一致性检验（建议调整比较值）'}`"
            :type="result.dimensionResult.consistent ? 'success' : 'warning'"
            :closable="false"
            show-icon
            style="margin-bottom: 15px;"
          >
            <template #default>
              <span style="font-size: 12px; color: #666;">
                λmax = {{ result.dimensionResult.lambdaMax.toFixed(4) }} |
                CI = {{ result.dimensionResult.ci.toFixed(4) }} |
                RI = {{ result.dimensionResult.ri.toFixed(2) }}
              </span>
            </template>
          </el-alert>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-table :data="dimensionResultTable" border size="small">
                <el-table-column prop="name" label="维度" />
                <el-table-column prop="weight" label="权重" align="center">
                  <template #default="{ row }">
                    <el-tag type="primary" size="large" effect="dark">
                      {{ (row.weight * 100).toFixed(2) }}%
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="可视化" align="center">
                  <template #default="{ row }">
                    <div class="weight-bar">
                      <div class="weight-bar-fill" :style="{ width: (row.weight * 100) + '%' }"></div>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </el-col>
            <el-col :span="12">
              <div ref="dimensionChartRef" style="height: 280px"></div>
            </el-col>
          </el-row>
        </div>

        <!-- 指标层结果 -->
        <div class="result-block">
          <h3 class="section-title">
            <el-icon><DataAnalysis /></el-icon>
            指标层权重结果
          </h3>

          <el-collapse accordion>
            <el-collapse-item
              v-for="(indResult, dimName) in result.indicatorResults"
              :key="dimName"
              :name="dimName"
            >
              <template #title>
                <div style="display: flex; align-items: center; gap: 10px; width: 100%;">
                  <span style="font-weight: bold; font-size: 15px;">{{ dimName }}</span>
                  <el-tag :type="indResult.consistent ? 'success' : 'warning'" size="small">
                    CR={{ indResult.cr.toFixed(4) }}
                    {{ indResult.consistent ? '✓' : '✗' }}
                  </el-tag>
                  <span style="margin-left: auto; font-size: 12px; color: #999;">
                    {{ indResult.elementNames.length }}个指标
                  </span>
                </div>
              </template>

              <el-table :data="formatIndicatorResult(dimName, indResult)" border size="small">
                <el-table-column prop="name" label="指标" />
                <el-table-column prop="weight" label="权重" align="center">
                  <template #default="{ row }">
                    <el-tag type="success" size="small">{{ (row.weight * 100).toFixed(2) }}%</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="可视化" align="center">
                  <template #default="{ row }">
                    <div class="weight-bar small">
                      <div class="weight-bar-fill" :style="{ width: (row.weight * 100) + '%' }"></div>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </el-collapse-item>
          </el-collapse>
        </div>

        <!-- 综合权重结果 -->
        <div class="result-block">
          <h3 class="section-title">
            <el-icon><Histogram /></el-icon>
            综合权重（维度权重 × 指标权重）
          </h3>

          <el-table :data="combinedWeightTable" border size="small" max-height="500">
            <el-table-column prop="dimension" label="维度" width="120" fixed />
            <el-table-column prop="dimWeight" label="维度权重" width="100" align="center">
              <template #default="{ row }">
                <el-tag type="primary" size="small">{{ (row.dimWeight * 100).toFixed(2) }}%</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="indicator" label="指标" width="150" />
            <el-table-column prop="indWeight" label="指标权重" width="100" align="center">
              <template #default="{ row }">
                <el-tag type="success" size="small">{{ (row.indWeight * 100).toFixed(2) }}%</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="综合权重" align="center">
              <template #default="{ row }">
                <el-tag type="danger" size="large" effect="dark">
                  {{ (row.combinedWeight * 100).toFixed(4) }}%
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="可视化" align="center">
              <template #default="{ row }">
                <div class="weight-bar">
                  <div class="weight-bar-fill combined" :style="{ width: (row.combinedWeight * 100 / maxCombinedWeight * 100) + '%' }"></div>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 导出结果 -->
        <div class="action-buttons">
          <el-button type="success" @click="exportWeights">
            <el-icon><Download /></el-icon>
            导出权重JSON
          </el-button>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed, nextTick } from 'vue'
import { Setting, Grid, PieChart, DataAnalysis, Promotion, RefreshLeft, InfoFilled, Histogram, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { calculateExpertAHP } from '@/api'
import {
  dimensions,
  indicatorElements,
  dimensionComparisons,
  indicatorComparisons,
  dimensionDescriptions,
  indicatorMetaByDimension,
  indicatorDisplayLabel,
  indicatorTooltipText,
  defaultConfidence,
  ahpScaleFullLegend,
  ahpConfidenceLevelLegend,
  defaultScoreToInteger
} from '@/config/ahpComparisons'

function buildDimensionConfidenceMatrix() {
  const n = dimensions.length
  return Array.from({ length: n }, (_, i) =>
    Array.from({ length: n }, (_, j) => (i < j ? defaultConfidence : 0))
  )
}

function buildIndicatorConfidenceMatrices() {
  const matrices = {}
  for (const dim of dimensions) {
    const dimCode = dim.code
    const n = indicatorElements[dimCode].length
    matrices[dimCode] = Array.from({ length: n }, (_, i) =>
      Array.from({ length: n }, (_, j) => (i < j ? defaultConfidence : 0))
    )
  }
  return matrices
}

const emit = defineEmits(['weights-calculated'])

/** 行/列表头列 + n 个等宽数据列（表头与数据行共用同一模板，严格对齐） */
const dimensionMatrixGridTemplate = computed(() => {
  const n = dimensions.length
  return `minmax(168px, 220px) repeat(${n}, minmax(240px, 1fr))`
})

function indicatorMatrixGridTemplate(dimCode) {
  const n = indicatorElements[dimCode]?.length || 1
  const colMin = n >= 6 ? '220px' : '248px'
  return `minmax(168px, 240px) repeat(${n}, minmax(${colMin}, 1fr))`
}

const calculating = ref(false)
const result = ref(null)
const dimensionChartRef = ref(null)
let dimensionChart = null

// 维度层矩阵（5×5）
const dimensionMatrix = reactive(
  (() => {
    const n = dimensions.length
    const matrix = []
    for (let i = 0; i < n; i++) {
      const row = []
      for (let j = 0; j < n; j++) {
        if (i === j) {
          row.push(1)
        } else if (i < j) {
          const comp = dimensionComparisons.find(c => c.itemA === dimensions[i].name && c.itemB === dimensions[j].name)
          row.push(comp ? defaultScoreToInteger(comp.defaultScore) : 1)
        } else {
          row.push(0)
        }
      }
      matrix.push(row)
    }
    // 填充下三角
    for (let i = 0; i < n; i++) {
      for (let j = i + 1; j < n; j++) {
        matrix[j][i] = 1 / matrix[i][j]
      }
    }
    return matrix
  })()
)

// 维度层把握度（仅上三角有效，下三角与对角为 null）
const dimensionConfidence = reactive(buildDimensionConfidenceMatrix())

// 指标层矩阵（按维度存储）
const indicatorMatrix = reactive(
  (() => {
    const matrices = {}
    for (const dim of dimensions) {
      const dimCode = dim.code
      const elements = indicatorElements[dimCode]
      const n = elements.length
      const matrix = []
      for (let i = 0; i < n; i++) {
        const row = []
        for (let j = 0; j < n; j++) {
          if (i === j) {
            row.push(1)
          } else if (i < j) {
            const comps = indicatorComparisons[dimCode] || []
            const comp = comps.find(c => c.itemA === elements[i] && c.itemB === elements[j])
            row.push(comp ? defaultScoreToInteger(comp.defaultScore) : 1)
          } else {
            row.push(0)
          }
        }
        matrix.push(row)
      }
      // 填充下三角
      for (let i = 0; i < n; i++) {
        for (let j = i + 1; j < n; j++) {
          matrix[j][i] = 1 / matrix[i][j]
        }
      }
      matrices[dimCode] = matrix
    }
    return matrices
  })()
)

// 指标层把握度
const indicatorConfidence = reactive(buildIndicatorConfidenceMatrices())

// 初始化维度层矩阵（用于重置）
const initDimensionMatrix = () => {
  const n = dimensions.length
  dimensionMatrix.length = 0

  for (let i = 0; i < n; i++) {
    const row = []
    for (let j = 0; j < n; j++) {
      if (i === j) {
        row.push(1)
      } else if (i < j) {
        const comp = dimensionComparisons.find(c => c.itemA === dimensions[i].name && c.itemB === dimensions[j].name)
        row.push(comp ? defaultScoreToInteger(comp.defaultScore) : 1)
      } else {
        row.push(0)
      }
    }
    dimensionMatrix.push(row)
  }

  for (let i = 0; i < n; i++) {
    for (let j = i + 1; j < n; j++) {
      dimensionMatrix[j][i] = 1 / dimensionMatrix[i][j]
    }
  }

  const conf = buildDimensionConfidenceMatrix()
  for (let i = 0; i < n; i++) {
    for (let j = 0; j < n; j++) {
      dimensionConfidence[i][j] = conf[i][j]
    }
  }
}

// 初始化指标层矩阵（用于重置）
const initIndicatorMatrix = () => {
  for (const dim of dimensions) {
    const dimCode = dim.code
    const elements = indicatorElements[dimCode]
    const n = elements.length
    const matrix = []

    for (let i = 0; i < n; i++) {
      const row = []
      for (let j = 0; j < n; j++) {
        if (i === j) {
          row.push(1)
        } else if (i < j) {
          const comps = indicatorComparisons[dimCode] || []
          const comp = comps.find(c => c.itemA === elements[i] && c.itemB === elements[j])
          row.push(comp ? defaultScoreToInteger(comp.defaultScore) : 1)
        } else {
          row.push(0)
        }
      }
      matrix.push(row)
    }

    for (let i = 0; i < n; i++) {
      for (let j = i + 1; j < n; j++) {
        matrix[j][i] = 1 / matrix[i][j]
      }
    }

    indicatorMatrix[dimCode] = matrix

    const nc = elements.length
    indicatorConfidence[dimCode] = Array.from({ length: nc }, (_, i) =>
      Array.from({ length: nc }, (_, j) => (i < j ? defaultConfidence : 0))
    )
  }
}

function clampAhpInteger(val) {
  let v = Math.round(Number(val))
  if (!Number.isFinite(v) || v < 1) v = 1
  if (v > 9) v = 9
  return v
}

// 维度矩阵单元格变化时自动更新下三角倒数（重要性为 1～9 整数）
const onDimensionCellChange = (row, col, val) => {
  const v = clampAhpInteger(val)
  dimensionMatrix[row][col] = v
  dimensionMatrix[col][row] = 1 / v
}

const onIndicatorCellChange = (dimCode, row, col, val) => {
  const v = clampAhpInteger(val)
  indicatorMatrix[dimCode][row][col] = v
  indicatorMatrix[dimCode][col][row] = 1 / v
}

// 维度层结果表格
const dimensionResultTable = computed(() => {
  if (!result.value || !result.value.dimensionResult) return []
  return result.value.dimensionResult.elementNames.map((name, idx) => ({
    name,
    weight: result.value.dimensionResult.weights[idx]
  }))
})

// 指标层结果格式化
const formatIndicatorResult = (dimName, indResult) => {
  return indResult.elementNames.map((name, idx) => ({
    name: indicatorDisplayLabel(dimName, name),
    weight: indResult.weights[idx]
  }))
}

// 综合权重表格
const combinedWeightTable = computed(() => {
  if (!result.value) return []

  const table = []
  const dimResult = result.value.dimensionResult

  for (let dIdx = 0; dIdx < dimResult.elementNames.length; dIdx++) {
    const dimName = dimResult.elementNames[dIdx]
    const dimWeight = dimResult.weights[dIdx]

    const indResult = result.value.indicatorResults[dimName]
    if (!indResult) continue

    for (let iIdx = 0; iIdx < indResult.elementNames.length; iIdx++) {
      const indName = indResult.elementNames[iIdx]
      const indWeight = indResult.weights[iIdx]
      const combinedWeight = dimWeight * indWeight

      table.push({
        dimension: dimName,
        dimWeight,
        indicator: indicatorDisplayLabel(dimName, indName),
        indWeight,
        combinedWeight
      })
    }
  }

  return table
})

// 最大综合权重（用于可视化比例）
const maxCombinedWeight = computed(() => {
  if (combinedWeightTable.value.length === 0) return 1
  return Math.max(...combinedWeightTable.value.map(r => r.combinedWeight))
})

// 计算AHP权重
const calculateWeights = async () => {
  calculating.value = true

  try {
    // 构建请求数据
    const dimensionEntries = []
    for (let i = 0; i < dimensions.length; i++) {
      for (let j = i + 1; j < dimensions.length; j++) {
        dimensionEntries.push({
          key: `${dimensions[i].name}_${dimensions[j].name}`,
          score: dimensionMatrix[i][j],
          confidence: dimensionConfidence[i][j] ?? defaultConfidence
        })
      }
    }

    const indicatorEntries = {}
    for (const dim of dimensions) {
      const dimCode = dim.code
      const elements = indicatorElements[dimCode]
      const matrix = indicatorMatrix[dimCode]
      const entries = []

      for (let i = 0; i < elements.length; i++) {
        for (let j = i + 1; j < elements.length; j++) {
          entries.push({
            key: `${elements[i]}_${elements[j]}`,
            score: matrix[i][j],
            confidence: indicatorConfidence[dimCode][i][j] ?? defaultConfidence
          })
        }
      }

      indicatorEntries[dimCode] = entries
    }

    // axios 拦截器已解包为 ApiResponse.data，此处即为 MatrixCalculationResult
    const response = await calculateExpertAHP({
      dimensionMatrix: dimensionEntries,
      indicatorMatrices: indicatorEntries
    })

    result.value = response
    ElMessage.success('AHP权重计算成功！')

    await nextTick()
    renderDimensionChart()

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

// 绘制维度权重图表
const renderDimensionChart = () => {
  if (!dimensionChartRef.value) return

  if (!dimensionChart) {
    dimensionChart = echarts.init(dimensionChartRef.value)
  }

  const data = dimensionResultTable.value.map(item => ({
    name: item.name,
    value: (item.weight * 100).toFixed(2)
  }))

  const option = {
    title: {
      text: '维度权重分布',
      left: 'center',
      textStyle: {
        color: '#001f3f',
        fontSize: 14,
        fontWeight: 'bold'
      }
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}%'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      textStyle: { color: '#001f3f' }
    },
    series: [{
      type: 'pie',
      radius: ['35%', '65%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 8,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: true,
        formatter: '{b}\n{c}%',
        color: '#001f3f',
        fontSize: 11
      },
      emphasis: {
        label: { show: true, fontSize: 14, fontWeight: 'bold' }
      },
      data: data,
      color: ['#0074D9', '#39CCCC', '#3D9970', '#FF851B', '#FFDC00']
    }]
  }

  dimensionChart.setOption(option)
}

// 恢复默认
const resetToDefault = () => {
  initDimensionMatrix()
  initIndicatorMatrix()
  result.value = null
}

// 导出权重JSON
const exportWeights = () => {
  if (!result.value) return

  const data = {
    dimensionWeights: {},
    indicatorWeights: {},
    combinedWeights: [],
    pairwise: { dimensions: [], indicators: {} }
  }

  for (let i = 0; i < dimensions.length; i++) {
    for (let j = i + 1; j < dimensions.length; j++) {
      data.pairwise.dimensions.push({
        key: `${dimensions[i].name}_${dimensions[j].name}`,
        score: dimensionMatrix[i][j],
        confidence: dimensionConfidence[i][j] ?? defaultConfidence
      })
    }
  }
  for (const dim of dimensions) {
    const dimCode = dim.code
    const elements = indicatorElements[dimCode]
    data.pairwise.indicators[dimCode] = []
    for (let i = 0; i < elements.length; i++) {
      for (let j = i + 1; j < elements.length; j++) {
        data.pairwise.indicators[dimCode].push({
          key: `${elements[i]}_${elements[j]}`,
          score: indicatorMatrix[dimCode][i][j],
          confidence: indicatorConfidence[dimCode][i][j] ?? defaultConfidence
        })
      }
    }
  }

  // 维度权重
  dimensionResultTable.value.forEach(item => {
    data.dimensionWeights[item.name] = item.weight
  })

  // 指标权重
  for (const [dimName, indResult] of Object.entries(result.value.indicatorResults)) {
    data.indicatorWeights[dimName] = {}
    indResult.elementNames.forEach((name, idx) => {
      data.indicatorWeights[dimName][name] = indResult.weights[idx]
    })
  }

  // 综合权重
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
  a.download = `ahp_weights_${Date.now()}.json`
  a.click()
  URL.revokeObjectURL(url)

  ElMessage.success('权重已导出为JSON文件')
}

</script>

<style scoped lang="scss">
.ahp-config-card {
  margin-bottom: 20px;

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
}

.config-content {
  .scale-section {
    margin-bottom: 20px;
    padding: 10px;
    background: #f5f7fa;
    border-radius: 4px;
  }

  .scale-legend-block {
    padding: 16px 18px;
    background: #fff;
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    box-shadow: 0 1px 6px rgba(0, 31, 63, 0.06);
  }

  .scale-legend-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0 0 10px;
    font-size: 16px;
    font-weight: bold;
    color: var(--navy-primary);
  }

  .scale-legend-intro {
    margin: 0 0 14px;
    font-size: 13px;
    line-height: 1.65;
    color: #606266;
  }

  .scale-legend-table {
    width: 100%;
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

  /* Grid 矩阵：宽度占满容器，列过多时横向滚动 */
  .matrix-grid-container.matrix-with-confidence {
    display: block;
    width: 100%;
    max-width: 100%;
    overflow-x: auto;
    overflow-y: visible;
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

  .field-descriptions {
    margin-bottom: 16px;

    &.indicator-desc {
      margin-bottom: 12px;
    }
  }

  :deep(.desc-label-narrow) {
    width: 96px;
  }

  .matrix-header .header-label {
    cursor: help;
    border-bottom: 1px dashed rgba(255, 255, 255, 0.55);
    padding-bottom: 1px;
  }

  .matrix-row-label .header-label {
    cursor: help;
    border-bottom: 1px dashed #909399;
    padding-bottom: 1px;
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

  .matrix-with-confidence {
    --ahp-row-min-h: 58px;

    /* 未使用 Grid 的回退（若某处未加 ahp-matrix-grid-row） */
    .matrix-header:not(.ahp-matrix-grid-row),
    .matrix-row:not(.ahp-matrix-grid-row) {
      display: flex;
      flex-wrap: nowrap;
    }

    &.indicator-matrix-wrap {
      --ahp-row-min-h: 60px;
    }
  }

  /* CSS Grid：表头行与数据行使用相同 gridTemplateColumns，列线完全重合 */
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
      min-height: var(--ahp-row-min-h);
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
      min-height: var(--ahp-row-min-h);
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

    .matrix-corner {
      border-right: 1px solid rgba(255, 255, 255, 0.3);
      background: transparent;
    }
  }

  .matrix-with-confidence .matrix-row.ahp-matrix-grid-row {
    background: #fff;
  }

  .matrix-grid-container > .ahp-matrix-grid-row:last-child {
    border-bottom: none;
  }

  /* 单元格内三列固定栅格：重要性 | 把握度 | λ，避免挤错位 */
  .matrix-with-confidence .matrix-cell .cell-inline-inputs {
    display: grid;
    grid-template-columns: 52px min-content 58px;
    column-gap: 6px;
    align-items: center;
    justify-content: center;
    justify-items: stretch;
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
    justify-self: center;
  }

  .cell-inline-inputs .inline-score,
  .cell-inline-inputs .inline-conf {
    width: 100% !important;
    max-width: 100%;
    min-width: 0;
  }

  .matrix-with-confidence .cell-inline-inputs :deep(.el-input-number) {
    width: 100%;
  }

  .matrix-with-confidence .cell-inline-inputs :deep(.el-input-number .el-input__wrapper) {
    padding: 2px 4px;
    min-height: 28px;
  }

  .ahp-plain-number :deep(.el-input-number__increase),
  .ahp-plain-number :deep(.el-input-number__decrease) {
    display: none !important;
  }

  .ahp-plain-number :deep(.el-input-number .el-input__inner) {
    text-align: center;
    font-size: 12px;
  }

  .confidence-legend-block {
    margin-top: 20px;
  }

  .action-buttons {
    display: flex;
    justify-content: center;
    gap: 20px;
    margin: 30px 0;
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
  }

  .weight-bar {
    width: 100px;
    height: 16px;
    background: #e4e7ed;
    border-radius: 8px;
    overflow: hidden;

    .weight-bar-fill {
      height: 100%;
      background: linear-gradient(90deg, var(--navy-primary), var(--navy-secondary));
      border-radius: 8px;
      transition: width 0.3s ease;

      &.combined {
        background: linear-gradient(90deg, #e74c3c, #ff6b6b);
      }
    }

    &.small {
      width: 60px;
      height: 12px;
    }
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
