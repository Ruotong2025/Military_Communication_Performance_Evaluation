<template>
  <div class="unified-gw-root">
    <!-- 一级：域间（两大体系） -->
    <div class="result-block">
      <h3 class="section-title">
        <el-icon><PieChart /></el-icon>
        一级：域间体系权重（对总目标）
      </h3>
      <el-alert
        v-if="crossInfo"
        :title="`域间 Saaty 标度 a = ${crossInfo.score.toFixed(4)} · 把握度 λ = ${crossInfo.confidence.toFixed(2)}`"
        type="success"
        :closable="false"
        show-icon
        class="unified-gw-alert"
      />
      <el-table :data="level1Rows" border size="small" class="dim-result-table">
        <el-table-column prop="label" label="一级（体系）" min-width="200" />
        <el-table-column label="全局权重" width="160" align="center">
          <template #default="{ row }">
            <el-tag type="primary" size="large" effect="dark">{{ row.globalPct.toFixed(4) }}%</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 二级：各体系内维度层 -->
    <div class="result-block">
      <h3 class="section-title">
        <el-icon><DataAnalysis /></el-icon>
        二级：维度层（准则层）— 全局权重与体系内占比
      </h3>
      <el-alert type="info" :closable="false" show-icon class="unified-gw-alert">
        「对总目标」= 域间体系权重 × 维度在体系内权重；「体系内占比」在各效能/装备分支内合计为 100%。
      </el-alert>
      <el-table :data="level2Rows" border size="small" stripe max-height="360">
        <el-table-column prop="domainLabel" label="所属体系" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.domainKey === 'efficiency' ? 'success' : 'primary'">
              {{ row.domainLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dimension" label="二级（维度）" min-width="140" show-overflow-tooltip />
        <el-table-column label="对总目标权重" width="150" align="center">
          <template #default="{ row }">
            <el-tag type="danger" size="small" effect="plain">{{ row.globalPct.toFixed(4) }}%</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="在本体系内维度权重" width="170" align="center">
          <template #default="{ row }">
            {{ row.localInDomainPct.toFixed(4) }}%
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 三级：叶子 + 旭日图 -->
    <div class="result-block">
      <h3 class="section-title">
        <el-icon><Histogram /></el-icon>
        三级：叶子指标 — 全局权重与维内占比
      </h3>
      <el-alert type="success" :closable="false" show-icon class="unified-gw-alert">
        共 <strong>{{ level3Rows.length }}</strong> 个叶子；全局权重之和 <strong>{{ leafSumPct.toFixed(4) }}%</strong>（应为 100%）。
      </el-alert>
      <el-row :gutter="20" class="combined-weight-row">
        <el-col :xs="24" :lg="14">
          <el-table :data="level3Rows" border size="small" stripe max-height="520" class="combined-weight-table">
            <el-table-column type="index" label="#" width="48" align="center" />
            <el-table-column prop="domainLabel" label="一级体系" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.domainKey === 'efficiency' ? 'success' : 'primary'">
                  {{ row.domainLabel }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="dimension" label="二级（维度）" min-width="120" show-overflow-tooltip />
            <el-table-column prop="indicator" label="三级（叶子指标）" min-width="140" show-overflow-tooltip />
            <el-table-column label="维内指标权重" width="120" align="center">
              <template #default="{ row }">
                {{ row.localInDimPct.toFixed(4) }}%
              </template>
            </el-table-column>
            <el-table-column label="叶子全局权重" width="130" align="center">
              <template #default="{ row }">
                <el-tag type="danger" size="small" effect="dark">{{ row.globalPct.toFixed(4) }}%</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-col>
        <el-col :xs="24" :lg="10">
          <div class="combined-sunburst-wrap">
            <div class="combined-sunburst-title">全局权重旭日图（内圈体系 · 中圈维度 · 外圈叶子）</div>
            <p class="combined-sunburst-hint">与上方「计算 AHP 权重」中综合权重图一致为旭日图；本图为跨效能/装备与域间一级后的完整层次。</p>
            <div ref="sunburstRef" class="combined-sunburst-chart" />
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { PieChart, DataAnalysis, Histogram } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const props = defineProps({
  /** getExpertUnifiedWeights 返回的快照（AhpIndividualResult 结构） */
  snapshot: { type: Object, default: null }
})

const sunburstRef = ref(null)
let sunburstChart = null

const DOMAIN_EFF = 'efficiency'
const DOMAIN_EQ = 'equipment'

const crossInfo = computed(() => {
  const c = props.snapshot?.crossDomain
  if (!c) return null
  const conf = Number(c.confidence)
  return {
    score: Number(c.score) || 1,
    confidence: Number.isFinite(conf) ? conf : 0.8,
    effWeight: Number.isFinite(Number(c.effWeight)) ? Number(c.effWeight) : 0.5,
    eqWeight: Number.isFinite(Number(c.eqWeight)) ? Number(c.eqWeight) : 0.5
  }
})

const wEff = computed(() => {
  const c = crossInfo.value
  if (c) return c.effWeight
  return 0.5
})
const wEq = computed(() => {
  const c = crossInfo.value
  if (c) return c.eqWeight
  return 0.5
})

const level1Rows = computed(() => [
  { key: 'eff', label: '效能指标体系', globalPct: wEff.value * 100 },
  { key: 'eq', label: '装备操作体系', globalPct: wEq.value * 100 }
])

function collectLevel2(domainKey, domainLabel, branchW, dimWeights) {
  const rows = []
  if (!dimWeights || typeof dimWeights !== 'object') return rows
  for (const [dim, w] of Object.entries(dimWeights)) {
    const lw = Number(w) || 0
    rows.push({
      domainKey,
      domainLabel,
      dimension: dim,
      globalPct: branchW * lw * 100,
      localInDomainPct: lw * 100
    })
  }
  return rows
}

const level2Rows = computed(() => {
  const eff = props.snapshot?.effectiveness?.dimensionWeights
  const eq = props.snapshot?.equipment?.dimensionWeights
  const a = [
    ...collectLevel2(DOMAIN_EFF, '效能', wEff.value, eff),
    ...collectLevel2(DOMAIN_EQ, '装备', wEq.value, eq)
  ]
  return a.sort((x, y) => y.globalPct - x.globalPct)
})

function localIndWeightInDim(snapshot, leaf) {
  const domain = leaf.domain
  const dim = leaf.dimension
  const gw = Number(leaf.globalWeight) || 0
  if (domain === DOMAIN_EFF) {
    const dimW = Number(snapshot?.effectiveness?.dimensionWeights?.[dim]) || 0
    const denom = wEff.value * dimW
    return denom > 1e-15 ? gw / denom : 0
  }
  const dimW = Number(snapshot?.equipment?.dimensionWeights?.[dim]) || 0
  const denom = wEq.value * dimW
  return denom > 1e-15 ? gw / denom : 0
}

const level3Rows = computed(() => {
  const snap = props.snapshot
  const leaves = snap?.allLeaves
  if (!Array.isArray(leaves)) return []
  return leaves.map((leaf) => {
    const domainKey = leaf.domain
    const domainLabel = domainKey === DOMAIN_EFF ? '效能' : '装备'
    const localInDim = localIndWeightInDim(snap, leaf)
    return {
      domainKey,
      domainLabel,
      dimension: leaf.dimension,
      indicator: leaf.indicator,
      localInDimPct: localInDim * 100,
      globalPct: (Number(leaf.globalWeight) || 0) * 100
    }
  }).sort((a, b) => b.globalPct - a.globalPct)
})

const leafSumPct = computed(() =>
  level3Rows.value.reduce((s, r) => s + r.globalPct, 0)
)

const SUNBURST_BRANCH = ['#3D9970', '#409EFF']
const SUNBURST_DIM_PALETTE = ['#0074D9', '#39CCCC', '#3D9970', '#FF851B', '#FFDC00', '#B10DC9', '#85144b', '#F012BE']

function buildSunburstTree() {
  const snap = props.snapshot
  if (!snap) return null

  const eff = snap.effectiveness
  const eq = snap.equipment
  const effDims = eff?.dimensionWeights || {}
  const eqDims = eq?.dimensionWeights || {}
  const effInd = eff?.indicators || {}
  const eqInd = eq?.indicators || {}

  function dimChildren(domainKey, branchW, dimWeights, indicators) {
    const list = []
    let ci = 0
    for (const [dimName, dimLocal] of Object.entries(dimWeights)) {
      const dw = Number(dimLocal) || 0
      const indMap = indicators[dimName] || {}
      const leaves = []
      for (const [indName, indLocal] of Object.entries(indMap)) {
        const iw = Number(indLocal) || 0
        const v = Math.max(1e-9, branchW * dw * iw * 100)
        leaves.push({
          name: shortIndName(indName),
          value: v,
          fullPath: `${dimName} · ${indName}`
        })
      }
      if (!leaves.length) continue
      const base = SUNBURST_DIM_PALETTE[ci % SUNBURST_DIM_PALETTE.length]
      ci++
      list.push({
        name: dimName,
        itemStyle: { color: base },
        children: leaves.map((leaf, i) => ({
          name: leaf.name,
          value: leaf.value,
          fullPath: leaf.fullPath,
          itemStyle: { color: echarts.color.lift(base, (i % 5) * 0.07) }
        }))
      })
    }
    return list
  }

  const effChildren = dimChildren(DOMAIN_EFF, wEff.value, effDims, effInd)
  const eqChildren = dimChildren(DOMAIN_EQ, wEq.value, eqDims, eqInd)

  const children = []
  if (effChildren.length) {
    children.push({
      name: '效能',
      itemStyle: { color: SUNBURST_BRANCH[0] },
      children: effChildren
    })
  }
  if (eqChildren.length) {
    children.push({
      name: '装备',
      itemStyle: { color: SUNBURST_BRANCH[1] },
      children: eqChildren
    })
  }
  if (!children.length) return null

  return {
    name: '',
    itemStyle: { color: '#001f3f' },
    children
  }
}

function shortIndName(name) {
  if (!name || typeof name !== 'string') return String(name ?? '')
  return name.replace(/得分$/u, '').slice(0, 12)
}

function disposeSunburst() {
  if (sunburstChart) {
    sunburstChart.dispose()
    sunburstChart = null
  }
}

function renderSunburst() {
  disposeSunburst()
  if (!sunburstRef.value) return
  const root = buildSunburstTree()
  if (!root) return
  sunburstChart = echarts.init(sunburstRef.value)
  sunburstChart.setOption({
    tooltip: {
      trigger: 'item',
      confine: true,
      formatter(p) {
        const v = p.value
        const pct = typeof v === 'number' ? `${v.toFixed(4)}%` : String(p.value)
        const extra = p.data?.fullPath
        const path = p.treePathInfo?.map((x) => x.name).filter(Boolean).slice(1).join(' → ') || ''
        if (extra && p.treePathInfo?.length >= 4) {
          return `<div style="max-width:300px;line-height:1.6">${extra}<br/>占全体：<b>${pct}</b></div>`
        }
        return `<div style="max-width:300px;line-height:1.6">${path}<br/>占全体：<b>${pct}</b></div>`
      }
    },
    series: [
      {
        type: 'sunburst',
        data: [root],
        radius: ['10%', '92%'],
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
          { r0: '0%', r: '10%', label: { show: false }, itemStyle: { color: '#001f3f', borderWidth: 0 } },
          {
            r0: '10%',
            r: '30%',
            label: {
              show: true,
              rotate: 'radial',
              minAngle: 6,
              fontSize: 11,
              fontWeight: 'bold',
              color: '#fff',
              textBorderColor: 'rgba(0,31,63,0.45)',
              textBorderWidth: 1.2,
              position: 'inside',
              overflow: 'truncate',
              width: 56
            },
            itemStyle: { borderWidth: 1 }
          },
          {
            r0: '30%',
            r: '54%',
            label: {
              show: true,
              rotate: 'radial',
              minAngle: 4,
              fontSize: 10,
              color: '#fff',
              textBorderColor: 'rgba(0,0,0,0.25)',
              position: 'inside',
              overflow: 'truncate',
              width: 64
            },
            itemStyle: { borderWidth: 1 }
          },
          { r0: '54%', r: '92%', label: { show: false }, itemStyle: { borderWidth: 1 } }
        ]
      }
    ]
  })
}

function onResize() {
  sunburstChart?.resize()
}

watch(
  () => props.snapshot,
  () => {
    nextTick(() => renderSunburst())
  },
  { deep: true }
)

onMounted(() => {
  window.addEventListener('resize', onResize)
  nextTick(() => renderSunburst())
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  disposeSunburst()
})
</script>

<style scoped lang="scss">
.unified-gw-root {
  --navy-primary: #0d3a66;
  --navy-light: #b8c5d9;
}

/* 与 AHPConfig 结果区对齐 */
.result-block {
  margin-bottom: 30px;
  padding: 20px;
  background: #fafbfc;
  border-radius: 8px;
  border: 1px solid #e4e7ed;

  &:last-child {
    margin-bottom: 0;
  }
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 14px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;

  .el-icon {
    font-size: 20px;
    color: var(--navy-primary);
  }
}

.unified-gw-alert {
  margin-bottom: 12px;
}

.dim-result-table {
  width: 100%;
}

.combined-weight-row {
  align-items: stretch;
}

.combined-weight-table {
  width: 100%;
}

.combined-sunburst-wrap {
  display: flex;
  flex-direction: column;
  min-height: 420px;
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

.combined-sunburst-hint {
  flex-shrink: 0;
  margin: 0 0 8px;
  padding: 0 8px;
  text-align: center;
  font-size: 12px;
  line-height: 1.45;
  color: #909399;
}

.combined-sunburst-chart {
  flex: 1;
  width: 100%;
  min-height: 380px;
}
</style>
