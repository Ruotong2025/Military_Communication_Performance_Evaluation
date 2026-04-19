<template>
  <div class="cross-domain-ahp">
    <div class="cross-domain-head">
      <h3 class="cross-domain-title">
        <el-icon><Grid /></el-icon>
        一级域间判断矩阵（效能指标 vs 装备操作）
      </h3>
      <p class="cross-domain-desc">
        与上方 Saaty 标度、把握度表相同规则。标度表示<strong>行「效能指标体系」相对列「装备操作体系」</strong>的重要性；归一化后得到两套下层 AHP 权重合并时的全局系数（见下方预览）。
      </p>
    </div>

    <el-empty
      v-if="!expertId"
      description="请先选择页顶「当前专家」，再填写域间比较。"
      :image-size="72"
    />

    <template v-else>
      <div class="ahp-matrix-scroll cross-domain-scroll">
        <div class="matrix-grid-container matrix-with-confidence cross-domain-grid">
          <div
            class="matrix-header ahp-matrix-grid-row"
            :style="{ gridTemplateColumns: gridTpl }"
          >
            <div class="matrix-corner" />
            <div class="matrix-header-cell">
              <span class="header-label">装备操作体系</span>
            </div>
          </div>
          <div class="matrix-row ahp-matrix-grid-row" :style="{ gridTemplateColumns: gridTpl }">
            <div class="matrix-row-label">
              <span class="header-label">效能指标体系</span>
            </div>
            <div class="matrix-cell cell-upper cell-editable">
              <div class="cell-inline-inputs" title="效能相对装备的重要性标度；右侧为把握度">
                <el-input-number
                  v-model="crossScore"
                  :min="AHP_SCORE_MIN"
                  :max="AHP_SCORE_MAX"
                  :step="0.01"
                  :precision="4"
                  size="small"
                  :controls="false"
                  class="inline-score ahp-plain-number"
                  @change="onScoreChange"
                />
                <span class="inline-conf-label">把握度</span>
                <el-input-number
                  v-model="crossConfidence"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  :precision="2"
                  size="small"
                  :controls="false"
                  class="inline-conf ahp-plain-number"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="cross-domain-weights-preview">
        <span class="preview-label">域间归一化权重（用于合并两套下层权重）：</span>
        <el-tag type="primary" effect="plain">效能 {{ effPct }}%</el-tag>
        <el-tag type="warning" effect="plain">装备 {{ eqPct }}%</el-tag>
        <span class="preview-formula">（w<sub>效能</sub> = a/(1+a)，w<sub>装备</sub> = 1/(1+a)，a 为上行标度）</span>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Grid } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getExpertAhpScores, saveCrossDomainExpertAhpScore } from '@/api'
import { CROSS_DOMAIN_COMPARISON_KEY } from '@/config/ahpCrossDomain'
import { defaultBlankConfidence, AHP_SCORE_MIN, AHP_SCORE_MAX } from '@/config/ahpComparisons'

const props = defineProps({
  expertId: { type: [Number, String, null], default: null },
  expertName: { type: String, default: '' }
})

const crossScore = ref(1)
const crossConfidence = ref(defaultBlankConfidence)

const gridTpl = 'minmax(168px, 220px) minmax(240px, 1fr)'

const effWeight = computed(() => {
  const a = Number(crossScore.value)
  if (!Number.isFinite(a) || a <= 0) return 0.5
  return a / (1 + a)
})

const effPct = computed(() => (effWeight.value * 100).toFixed(2))
const eqPct = computed(() => ((1 - effWeight.value) * 100).toFixed(2))

function clampScore(v) {
  let x = Number(v)
  if (!Number.isFinite(x)) x = 1
  return Math.min(AHP_SCORE_MAX, Math.max(AHP_SCORE_MIN, x))
}

function onScoreChange(val) {
  crossScore.value = clampScore(val)
}

async function loadFromDb() {
  if (!props.expertId) {
    crossScore.value = 1
    crossConfidence.value = defaultBlankConfidence
    return
  }
  try {
    const rows = await getExpertAhpScores(props.expertId)
    const list = Array.isArray(rows) ? rows : []
    const r = list.find((x) => x.comparisonKey === CROSS_DOMAIN_COMPARISON_KEY)
    if (r && r.score != null) {
      crossScore.value = clampScore(Number(r.score))
      const cf = r.confidence != null && r.confidence !== '' ? Number(r.confidence) : defaultBlankConfidence
      crossConfidence.value = Math.min(1, Math.max(0, cf))
    } else {
      crossScore.value = 1
      crossConfidence.value = defaultBlankConfidence
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('加载域间比较失败：' + (e.message || ''))
  } finally {
    loading.value = false
  }
}

async function saveToDb() {
  if (!props.expertId) {
    ElMessage.warning('请先选择专家')
    return
  }
  try {
    await saveCrossDomainExpertAhpScore({
      expertId: props.expertId,
      expertName: props.expertName || undefined,
      score: clampScore(crossScore.value),
      confidence: Math.min(1, Math.max(0, Number(crossConfidence.value) || defaultBlankConfidence))
    })
    ElMessage.success('域间一级比较已保存')
  } catch (e) {
    ElMessage.error('保存域间比较失败：' + (e.message || ''))
  }
}

function reload() {
  return loadFromDb()
}

watch(
  () => props.expertId,
  () => {
    void loadFromDb()
  },
  { immediate: true }
)

defineExpose({
  saveToDb,
  reload
})
</script>

<style scoped lang="scss">
.cross-domain-ahp {
  .cross-domain-head {
    margin-bottom: 12px;
  }

  .cross-domain-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0 0 8px;
    font-size: 16px;
    font-weight: 600;
    color: var(--navy-primary, #0d3a66);
  }

  .cross-domain-desc {
    margin: 0;
    font-size: 13px;
    line-height: 1.65;
    color: #606266;
  }

  .cross-domain-scroll {
    margin-bottom: 14px;
  }

  .cross-domain-grid {
    --ahp-row-min-h: 56px;
    min-width: 360px;
  }

  .ahp-matrix-grid-row {
    display: grid;
    align-items: stretch;
  }

  .matrix-header-cell,
  .matrix-row-label {
    display: flex;
    align-items: center;
    justify-content: center;
    text-align: center;
    font-weight: 600;
    font-size: 13px;
  }

  .matrix-header {
    background: linear-gradient(135deg, var(--navy-primary, #0d3a66), var(--navy-secondary, #1a5a8a));
    color: #fff;
    border-radius: 8px 8px 0 0;
    border: 1px solid #e4e7ed;
    border-bottom: none;
  }

  .matrix-corner {
    border-right: 1px solid rgba(255, 255, 255, 0.3);
    min-height: 48px;
  }

  .matrix-header-cell {
    min-height: 48px;
    padding: 10px 12px;
    border-right: none;
  }

  .matrix-row {
    border: 1px solid #e4e7ed;
    border-top: none;
    border-radius: 0 0 8px 8px;
    background: #fff;
  }

  .matrix-row-label {
    background: #f5f7fa;
    border-right: 1px solid #e4e7ed;
    padding: 10px 12px;
    line-height: 1.35;
  }

  .matrix-cell {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 10px 12px;
    min-height: var(--ahp-row-min-h);
  }

  .cell-inline-inputs {
    display: grid;
    grid-template-columns: minmax(72px, 1fr) min-content 58px;
    column-gap: 6px;
    align-items: center;
    width: fit-content;
    max-width: 100%;
    margin: 0 auto;
  }

  .inline-conf-label {
    font-size: 11px;
    color: #909399;
    white-space: nowrap;
    text-align: center;
  }

  .cross-domain-weights-preview {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 10px;
    font-size: 13px;
    color: #606266;

    .preview-label {
      font-weight: 500;
      color: #303133;
    }

    .preview-formula {
      font-size: 12px;
      color: #909399;
      width: 100%;
      margin-top: 4px;
    }
  }
}
</style>
