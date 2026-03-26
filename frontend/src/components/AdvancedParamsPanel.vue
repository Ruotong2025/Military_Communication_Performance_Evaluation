<template>
  <div class="advanced-params-panel" v-loading="structureLoading">
    <div class="mode-row">
      <span class="mode-label">数据写入模式：</span>
      <el-radio-group v-model="localMode" size="small">
        <el-radio label="overwrite" border>覆盖现有数据</el-radio>
        <el-radio label="append" border>新增数据</el-radio>
      </el-radio-group>
      <el-tooltip
        content="覆盖：清空【作战基础信息、通信记录、链路维护事件、安全事件】四表后再写入；新增：在现有数据末尾追加"
        placement="top"
      >
        <el-icon class="tip-icon"><QuestionFilled /></el-icon>
      </el-tooltip>
    </div>

    <p class="table-intro">
      仅展示可调整的数值字段（非数值、ID、字符串暂不参与）。默认展示当前档位下的均值和离散，可直接在表格内调整；生成时自动换算为 min/max 下发，键名为 <code>表名.列名</code>。
    </p>

    <div class="dispersion-note">
      <span class="label">离散度说明：</span>
      <span>离散度表示围绕均值的波动范围，数值越大，生成数据越分散。</span>
      <span class="formula">实际半宽 = 参考半宽 x (行离散系数 / 顶部离散系数)</span>
      <span class="levels">
        当前表系数：高 {{ factorText(activeTableFamily, 'high') }} / 中 {{ factorText(activeTableFamily, 'medium') }} / 低 {{ factorText(activeTableFamily, 'low') }}
      </span>
    </div>

    <el-tabs v-model="activeTable" class="table-tabs">
      <el-tab-pane
        v-for="table in RECORDS_TABLE_KEYS"
        :key="table"
        :name="table"
        :label="RECORDS_TABLE_LABELS[table] || table"
      />
    </el-tabs>

    <el-table
      :data="activeRows"
      border
      stripe
      size="small"
      class="override-table"
      max-height="520"
    >
      <el-table-column label="字段" width="168" fixed>
        <template #default="{ row }">
          <code class="field-key">{{ row.column }}</code>
        </template>
      </el-table-column>
      <el-table-column label="含义" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.label }}
        </template>
      </el-table-column>
      <el-table-column label="参考均值" width="100" align="right">
        <template #default="{ row }">
          <template v-if="row.kind === 'numeric'">
            {{ fmtRef(refMeanVal(row), row) }}
          </template>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="参考离散" width="88" align="center">
        <template #default="{ row }">
          <template v-if="row.kind === 'numeric'">
            {{ qualityLabel(dispersionLevel) }} ({{ factorText(row.rngFamily || 'op', dispersionLevel) }})
          </template>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="调整均值" min-width="168" align="center">
        <template #default="{ row }">
          <el-input-number
            :model-value="displayMean(row)"
            :precision="row.decimals ?? 0"
            :step="numStep(row)"
            controls-position="right"
            size="small"
            class="adj-mean"
            @update:model-value="(v) => setNumMean(row, v)"
          />
        </template>
      </el-table-column>
      <el-table-column label="调整离散" width="118" align="center">
        <template #default="{ row }">
          <el-input-number
            :model-value="displayDispersionScale(row)"
            :min="0"
            :step="0.05"
            :precision="2"
            controls-position="right"
            size="small"
            class="disp-scale-input"
            @update:model-value="(v) => setDispersionScale(row, v)"
          />
        </template>
      </el-table-column>
      <el-table-column label="推算范围" width="148" align="center">
        <template #default="{ row }">
          <template v-if="row.kind === 'numeric'">
            <el-tooltip :content="rangeTooltip(row)" placement="top">
              <span class="range-hint">{{ rangeBrief(row) }}</span>
            </el-tooltip>
          </template>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="64" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.kind === 'numeric'"
            type="primary"
            link
            size="small"
            @click="clearRow(row)"
          >
            清除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="panel-footer">
      <el-button size="small" @click="resetAll">
        <el-icon><RefreshRight /></el-icon>
        重置高级参数
      </el-button>
      <span class="footer-tip">未调整的行不占用 overrides / enumOverrides；数值行为「参考」随顶部档位变化</span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, reactive, watch, onMounted } from 'vue'
import { QuestionFilled, RefreshRight } from '@element-plus/icons-vue'
import { getTableStructure } from '@/api'
import {
  RECORDS_TABLE_KEYS,
  RECORDS_TABLE_LABELS,
  resolveFieldRow,
  qualityLabel,
  DISPERSION_FACTORS,
} from '@/config/recordsFieldCatalog.js'

const props = defineProps({
  modelValue: { type: Object, default: null },
  excellentLevel: { type: String, default: 'medium' },
  dispersionLevel: { type: String, default: 'medium' },
})

const emit = defineEmits(['update:modelValue'])

const structureLoading = ref(false)
const displayRows = ref([])
const activeTable = ref(RECORDS_TABLE_KEYS[0])

const localMode = ref('overwrite')
/** 表.列 -> { min, max } */
const overrides = reactive({})
/** 表.列 -> { mean?: number|null, rowDispersion: 'inherit'|'high'|'medium'|'low', dispersionScale?: number } */
const numAdj = reactive({})

const activeRows = computed(() => displayRows.value.filter((row) => row.table === activeTable.value))
const activeTableFamily = computed(() => {
  const first = activeRows.value[0]
  return first?.rngFamily || 'op'
})

function dispFactor(fam, level) {
  const f = DISPERSION_FACTORS[fam] || DISPERSION_FACTORS.op
  return f[level] ?? 1
}

function factorText(fam, level) {
  return `x${dispFactor(fam, level).toFixed(2)}`
}

function refInterval(row) {
  const q = props.excellentLevel || 'medium'
  const r = row.ranges?.[q] ?? row.ranges?.medium
  return Array.isArray(r) ? r : [0, 0]
}

function refMeanVal(row) {
  const [lo, hi] = refInterval(row)
  return (Number(lo) + Number(hi)) / 2
}

function fmtRef(v, row) {
  if (row.decimals > 0) return Number(v).toFixed(row.decimals)
  return String(Math.round(v))
}

function numStep(row) {
  if ((row.decimals ?? 0) > 0) return 0.1
  if (row.column === 'bandwidth_hz') return 10000
  return 1
}

function ensureNumAdj(qKey) {
  if (!numAdj[qKey]) {
    numAdj[qKey] = { mean: undefined, rowDispersion: 'inherit', dispersionScale: 1 }
  }
}

function setNumMean(row, v) {
  ensureNumAdj(row.qKey)
  numAdj[row.qKey].mean = v === undefined ? refMeanVal(row) : v
}

function setRowDispersion(row, v) {
  ensureNumAdj(row.qKey)
  numAdj[row.qKey].rowDispersion = v || 'inherit'
}

function setDispersionScale(row, v) {
  ensureNumAdj(row.qKey)
  const n = Number(v)
  numAdj[row.qKey].dispersionScale = Number.isFinite(n) && n >= 0 ? n : 1
  // 兼容旧字段：手动输入系数后，不再跟随高/中/低档位。
  numAdj[row.qKey].rowDispersion = 'inherit'
}

function displayDispersionScale(row) {
  ensureNumAdj(row.qKey)
  const v = Number(numAdj[row.qKey].dispersionScale)
  return Number.isFinite(v) ? v : 1
}

function displayMean(row) {
  ensureNumAdj(row.qKey)
  const v = numAdj[row.qKey].mean
  if (v === undefined || v === null || Number.isNaN(v)) {
    return refMeanVal(row)
  }
  return Number(v)
}

function shouldSendNumeric(row) {
  if (row.kind !== 'numeric') return false
  ensureNumAdj(row.qKey)
  const st = numAdj[row.qKey]
  const refM = refMeanVal(row)
  const meanTouched = st.mean !== undefined && st.mean !== null && !Number.isNaN(st.mean)
  const meanDiff = meanTouched && Math.abs(Number(st.mean) - refM) > (row.decimals > 0 ? 1e-6 : 0.501)
  const scale = Number(st.dispersionScale)
  const dispDiff = Number.isFinite(scale) ? Math.abs(scale - 1) > 1e-6 : (st.rowDispersion && st.rowDispersion !== 'inherit')
  return meanDiff || dispDiff
}

function computeMinMax(row) {
  const topD = props.dispersionLevel || 'medium'
  const [lo, hi] = refInterval(row)
  const halfRef = (Number(hi) - Number(lo)) / 2
  const refM = refMeanVal(row)
  ensureNumAdj(row.qKey)
  const st = numAdj[row.qKey]
  const mean = st.mean !== undefined && st.mean !== null && !Number.isNaN(st.mean) ? Number(st.mean) : refM
  const rowD = st.rowDispersion === 'inherit' ? topD : st.rowDispersion
  const fam = row.rngFamily || 'op'
  const fAdj = dispFactor(fam, rowD)
  const fTop = dispFactor(fam, topD)
  const scale = Number(st.dispersionScale)
  const customScale = Number.isFinite(scale) ? scale : 1
  const half = halfRef * (fAdj / (fTop || 1)) * customScale
  return { min: mean - half, max: mean + half }
}

function rangeBrief(row) {
  const { min, max } = computeMinMax(row)
  const d = row.decimals ?? 0
  if (d > 0) return `${min.toFixed(d)} ~ ${max.toFixed(d)}`
  return `${Math.round(min)} ~ ${Math.round(max)}`
}

function rangeTooltip(row) {
  if (shouldSendNumeric(row)) return `将随请求以 min/max 下发：${rangeBrief(row)}`
  return `默认参考范围（当前未下发 override）：${rangeBrief(row)}`
}

function flushNumericOverrides() {
  for (const row of displayRows.value) {
    if (row.kind !== 'numeric') continue
    if (!shouldSendNumeric(row)) {
      delete overrides[row.qKey]
      continue
    }
    const { min, max } = computeMinMax(row)
    if (min <= max) {
      overrides[row.qKey] = { min, max }
    }
  }
}

function buildModel() {
  flushNumericOverrides()
  const ov = {}
  for (const [k, v] of Object.entries(overrides)) {
    if (v && v.min != null && v.max != null && Number(v.min) <= Number(v.max)) {
      ov[k] = { min: Number(v.min), max: Number(v.max) }
    }
  }
  return {
    mode: localMode.value,
    overrides: Object.keys(ov).length ? ov : null,
    enumOverrides: null,
  }
}

function seedFromParent(v) {
  localMode.value = v?.mode === 'append' ? 'append' : 'overwrite'
  for (const k of Object.keys(overrides)) delete overrides[k]
  for (const k of Object.keys(numAdj)) delete numAdj[k]
  if (v?.overrides && typeof v.overrides === 'object') {
    for (const [k, r] of Object.entries(v.overrides)) {
      if (r && r.min != null && r.max != null) {
        overrides[k] = { min: r.min, max: r.max }
        numAdj[k] = {
          mean: (Number(r.min) + Number(r.max)) / 2,
          rowDispersion: 'inherit',
          dispersionScale: 1,
        }
      }
    }
  }
  if (v?.enumOverrides && typeof v.enumOverrides === 'object') {
    // 非数值字段暂不开放调整，保留兼容但忽略回填
  }
}

async function loadStructures() {
  structureLoading.value = true
  const rows = []
  try {
    for (const table of RECORDS_TABLE_KEYS) {
      const cols = await getTableStructure(table)
      for (const col of cols || []) {
        const name = col.columnName
        const meta = resolveFieldRow(table, name, col.dataType)
        const isNumeric = meta.kind === 'numeric'
        const blockedByName = /(^|_)id$|_id$/.test(name)
        if (!isNumeric || blockedByName) {
          continue
        }
        rows.push({
          ...meta,
          columnComment: col.columnComment,
          label: meta.label || col.columnComment || name,
        })
      }
    }
    displayRows.value = rows
  } catch (e) {
    console.error(e)
    displayRows.value = []
  } finally {
    structureLoading.value = false
  }
}

function clearRow(row) {
  if (row.kind === 'numeric') {
    delete overrides[row.qKey]
    delete numAdj[row.qKey]
  }
}

function resetAll() {
  for (const k of Object.keys(overrides)) delete overrides[k]
  for (const k of Object.keys(numAdj)) delete numAdj[k]
  localMode.value = 'overwrite'
  activeTable.value = RECORDS_TABLE_KEYS[0]
}

onMounted(async () => {
  await loadStructures()
  seedFromParent(props.modelValue)
})

watch(
  [localMode, () => props.excellentLevel, () => props.dispersionLevel, numAdj, displayRows],
  () => emit('update:modelValue', buildModel()),
  { deep: true, immediate: true }
)

defineExpose({ getModel: buildModel, resetAll, reloadStructures: loadStructures })
</script>

<style scoped lang="scss">
.advanced-params-panel {
  background: #f5f9ff;
  border: 1px solid #dde6f0;
  border-radius: 8px;
  padding: 12px 16px;
  margin-top: 8px;
}

.mode-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;

  .mode-label {
    font-size: 13px;
    color: #334;
    font-weight: 500;
  }

  .tip-icon {
    color: #909399;
    cursor: help;
    font-size: 14px;
  }
}

.table-intro {
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
  margin: 0 0 10px;

  code {
    font-size: 11px;
  }
}

.dispersion-note {
  margin: 0 0 10px;
  padding: 8px 10px;
  border-radius: 6px;
  background: #eef5ff;
  border: 1px solid #d8e6ff;
  font-size: 12px;
  color: #4b5563;
  line-height: 1.6;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  .label {
    font-weight: 600;
    color: #1f3f73;
  }

  .formula {
    color: #2c5aa0;
    font-family: 'Consolas', monospace;
  }

  .levels {
    color: #334155;
  }
}

.override-table {
  width: 100%;

  .field-key {
    font-size: 11px;
    color: #2c5aa0;
  }

  .adj-mean {
    width: 130px;
  }

  .enum-select {
    width: 100%;
    min-width: 140px;
  }

  .disp-scale-input {
    width: 96px;
  }

  .range-hint {
    font-size: 11px;
    color: #2c5aa0;
    font-family: 'Consolas', monospace;
    cursor: default;
  }

  .muted {
    color: #c0c4cc;
    &.small {
      font-size: 12px;
    }
  }
}

.panel-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #c8d8ee;

  .footer-tip {
    font-size: 11px;
    color: #999;
  }
}
</style>
