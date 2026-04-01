<template>
  <div class="penalty-page">
    <!-- ==================== 页面头部 ==================== -->
    <div class="page-header">
      <div>
        <h2>
          <el-icon><Warning /></el-icon>
          评估结果计算 — 惩罚因子分析
        </h2>
        <p class="subtitle">
          基于集结综合得分，对选定的核心指标施加惩罚因子，体现"一票否决"的军事底线逻辑
        </p>
      </div>
    </div>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="hint-alert"
      title="请先在「权重集结打分」执行「执行集结计算」，再在「评估结果计算 → 综合打分」中点击「加载综合结果」，然后返回本页进行惩罚分析。"
    />

    <!-- ==================== 批次选择工具栏 ==================== -->
    <el-card class="toolbar-card" shadow="hover">
      <el-form :inline="true" label-width="100px">
        <el-form-item label="评估批次">
          <el-select
            v-model="evaluationId"
            placeholder="请选择评估批次"
            clearable
            filterable
            style="width: 380px"
            @change="onBatchChange"
          >
            <el-option v-for="id in evaluationIds" :key="id" :label="id" :value="id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Refresh" :loading="loading" :disabled="!evaluationId" @click="loadResults">
            加载综合结果
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-empty
      v-if="!collectiveResult && !loading"
      description="请先选择评估批次并加载综合结果"
      style="margin-top: 24px"
    />

    <template v-if="collectiveResult">

      <!-- ==================== 一、惩罚模型说明 ==================== -->
      <el-card class="content-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span><el-icon><Document /></el-icon> 一、惩罚模型说明</span>
            <el-button text size="small" @click="showTheory = !showTheory">
              <el-icon>
                <ArrowUp v-if="showTheory" />
                <ArrowDown v-else />
              </el-icon>
              {{ showTheory ? '收起' : '展开详情' }}
            </el-button>
          </div>
        </template>
        <div v-if="showTheory" class="theory-section">
          <h4>1.1 模型背景</h4>
          <p>在 AHP 权重计算和专家可信度评估的基础上，引入惩罚模型对军事通信装备效能进行综合评估。惩罚模型针对通信作战的<strong>底线要求</strong>，对核心指标设定阈值，确保低于阈值的指标会对最终效能得分产生惩罚性影响，体现"一票否决"的军事逻辑。</p>

          <h4>1.2 惩罚函数</h4>
          <p>设第 i 个核心指标得分为 <code>x</code>（百分制），惩罚因子 <code>Fi</code> 定义为：</p>
          <div class="formula-box">
            <code>Fi = 1,              x ≥ s</code><br>
            <code>     (x / s) × m,   x &lt; s</code>
          </div>
          <p>其中：</p>
          <ul class="formula-desc">
            <li><code>s</code> — 惩罚阈值（默认为 70 分，可自定义 0~100）</li>
            <li><code>m</code> — 惩罚系数，满足 <code>0 &lt; m ≤ 1</code>，<code>m</code> 越小惩罚越重</li>
          </ul>

          <h4>1.3 综合惩罚因子</h4>
          <p>综合惩罚因子取各核心指标惩罚因子的<strong>最小值</strong>（最严格原则）：</p>
          <div class="formula-box">
            <code>P = min(F1, F2, ..., Fn)</code>
          </div>
          <p>采用最小值原则的意义在于：只要有一个核心指标低于阈值，就按该指标的惩罚因子计算整体惩罚，避免因多指标轻微惩罚而导致的整体惩罚过轻，从而突出底线指标的否决作用。</p>

          <h4>1.4 最终效能得分</h4>
          <p>将集结综合得分 <code>Sstage</code> 乘以综合惩罚因子，得到最终效能得分：</p>
          <div class="formula-box">
            <code>Sfinal = Sstage × P</code>
          </div>

          <h4>1.5 惩罚效果示例（s=70, m=0.8）</h4>
          <el-table :data="exampleData" border size="small" max-height="240">
            <el-table-column prop="x" label="连通率得分 x（分）" width="160" align="center" />
            <el-table-column prop="f" label="惩罚因子 F=(x/s)×m" align="center" />
            <el-table-column prop="note" label="说明" align="center" />
          </el-table>
        </div>
      </el-card>

      <!-- ==================== 二、惩罚参数配置 ==================== -->
      <el-card class="content-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span><el-icon><Setting /></el-icon> 二、惩罚参数配置</span>
            <div class="header-actions">
              <el-tag type="info" size="small">共 {{ penaltyConfig.length }} 个惩罚指标</el-tag>
              <el-button size="small" @click="resetToDefaults">恢复默认</el-button>
              <el-button size="small" @click="saveConfig">保存配置</el-button>
              <el-button size="small" @click="loadConfigDialogVisible = true">加载配置</el-button>
            </div>
          </div>
        </template>

        <!-- 模板预设 + 批量设置 -->
        <div class="batch-config-bar">
          <span class="batch-label">预设模板：</span>
          <el-radio-group v-model="presetTemplate" size="small" @change="applyPreset">
            <el-radio-button value="light">轻度（m=0.9）</el-radio-button>
            <el-radio-button value="medium">中度（m=0.8）</el-radio-button>
            <el-radio-button value="heavy">重度（m=0.5）</el-radio-button>
          </el-radio-group>
          <el-divider direction="vertical"></el-divider>
          <span class="batch-label">批量阈值：</span>
          <el-input-number v-model="batchThreshold" :min="0" :max="100" :step="1" size="small" controls-position="right" style="width: 100px" />
          <span class="batch-label" style="margin-left: 16px">批量系数：</span>
          <el-input-number v-model="batchM" :min="0.1" :max="1" :step="0.05" :precision="2" size="small" controls-position="right" style="width: 100px" />
          <el-button size="small" style="margin-left: 8px" @click="applyBatch">应用至已选指标</el-button>
        </div>

        <!-- 指标选择行 -->
        <div class="indicator-select-row">
          <span class="select-label">选择惩罚指标（可多选）：</span>
          <el-checkbox-group v-model="selectedIndicatorKeys" size="small">
            <el-checkbox-button
              v-for="ind in allIndicators"
              :key="ind.key"
              :label="ind.key"
            >
              {{ ind.label }}
            </el-checkbox-button>
          </el-checkbox-group>
        </div>

        <p class="config-table-hint">
          配置表中「Fi（均值）」按当前批次该指标的<strong>算术平均得分</strong>代入公式；「三、惩罚计算结果」与各作战行的 Fi 按<strong>该作战实际得分</strong>计算。二者口径不同，数值可能不一致。若各作战得分低于均值，可参考「Fi（批次最低分）」与结果表更接近。
        </p>

        <!-- 每个指标的详细配置 -->
        <el-table
          :data="penaltyConfig"
          border
          stripe
          size="small"
          max-height="320"
          class="penalty-config-table"
        >
          <el-table-column label="指标名称" width="160" align="center">
            <template #default="{ row }">
              <el-tag type="primary" size="small">{{ row.label }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="字段名" width="200" align="center">
            <template #default="{ row }">
              <code style="font-size: 11px">{{ row.key }}</code>
            </template>
          </el-table-column>
          <el-table-column label="阈值 s（分）" width="160" align="center">
            <template #default="{ row }">
              <el-input-number
                v-model="row.threshold"
                :min="0"
                :max="100"
                :step="1"
                size="small"
                controls-position="right"
                style="width: 110px"
                @change="recalculate"
              />
            </template>
          </el-table-column>
          <el-table-column label="惩罚系数 m" width="160" align="center">
            <template #default="{ row }">
              <el-slider
                v-model="row.m"
                :min="0.1"
                :max="1"
                :step="0.05"
                :format-tooltip="(v) => v.toFixed(2)"
                style="width: 110px"
                @change="recalculate"
              />
              <span style="margin-left: 6px; font-size: 11px; color: #909399">{{ row.m.toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="均值（分）" width="110" align="center">
            <template #default="{ row }">
              <span :class="getScoreClass(row.currentScore)">{{ row.currentScore != null ? row.currentScore.toFixed(1) : '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="批次最低（分）" width="120" align="center">
            <template #default="{ row }">
              <span :class="getScoreClass(row.batchMinScore)">{{ row.batchMinScore != null ? row.batchMinScore.toFixed(1) : '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="Fi（均值）" width="110" align="center">
            <template #default="{ row }">
              <span :class="getPenaltyClass(row.factor)">{{ row.factor != null ? row.factor.toFixed(4) : '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="Fi（批次最低分）" width="140" align="center">
            <template #default="{ row }">
              <span :class="getPenaltyClass(row.factorAtMin)">{{ row.factorAtMin != null ? row.factorAtMin.toFixed(4) : '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ row }">
              <el-button type="danger" link size="small" @click="removeIndicator(row.key)">
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- ==================== 三、惩罚计算结果 ==================== -->
      <el-card class="content-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span><el-icon><Finished /></el-icon> 三、惩罚计算结果</span>
            <div class="header-actions">
              <el-tag v-if="penaltyConfig.length" type="warning" size="small">
                综合惩罚因子 P（全作战最小）= {{ overallPenalty.toFixed(4) }}
              </el-tag>
              <el-button
                type="success"
                size="small"
                :loading="saving"
                :disabled="!penaltyResults.length"
                @click="savePenaltyToDatabase"
              >
                保存结果
              </el-button>
              <el-button
                type="primary"
                size="small"
                :loading="saving"
                :disabled="!evaluationId"
                @click="loadPenaltyFromDatabase"
              >
                加载已保存
              </el-button>
            </div>
          </div>
        </template>

        <div v-if="!penaltyConfig.length" class="empty-hint">
          <el-empty description="请先在「二、惩罚参数配置」中选择至少一个惩罚指标" :image-size="80" />
        </div>

        <template v-else>
          <el-table
            :data="penaltyResults"
            border
            stripe
            size="small"
            max-height="420"
            class="penalty-result-table"
          >
            <el-table-column prop="operationId" label="作战ID" width="90" fixed align="center" />
            <el-table-column label="综合得分（原始）" width="130" align="center">
              <template #default="{ row }">
                <span class="score-orig">{{ row.originalScore?.toFixed(4) }}</span>
              </template>
            </el-table-column>
            <el-table-column
              v-for="cfg in penaltyConfig"
              :key="cfg.key"
              :label="`${cfg.label}（分）`"
              width="110"
              align="center"
            >
              <template #default="{ row }">
                <div class="penalty-cell">
                  <span>{{ row.indicatorScores?.[cfg.key]?.toFixed(1) ?? '—' }}</span>
                  <span class="fi-label">Fi={{ row.penaltyFactors?.[cfg.key]?.toFixed(2) ?? '—' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="综合惩罚因子 P" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="row.overallPenalty >= 1 ? 'success' : row.overallPenalty >= 0.8 ? 'warning' : 'danger'" size="small">
                  {{ row.overallPenalty?.toFixed(4) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="综合得分（惩罚后）" width="130" fixed="right" align="center">
              <template #default="{ row }">
                <span class="score-final" :class="getFinalScoreClass(row.originalScore, row.finalScore)">
                  {{ row.finalScore?.toFixed(4) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="惩罚幅度" width="100" fixed="right" align="center">
              <template #default="{ row }">
                <span v-if="row.penaltyAmplitude > 0" class="penalty-amplitude">
                  ↓{{ (row.penaltyAmplitude * 100).toFixed(1) }}%
                </span>
                <span v-else class="no-penalty">无</span>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </el-card>

      <!-- ==================== 四、惩罚效果图表 ==================== -->
      <el-card v-if="penaltyResults.length" class="content-card chart-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span><el-icon><TrendCharts /></el-icon> 四、惩罚效果图表</span>
            <el-radio-group v-model="chartType" size="small">
              <el-radio-button value="bar">分组柱状图</el-radio-button>
              <el-radio-button value="line">折线图</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <p class="chart-hint">
          蓝色柱/线为惩罚前综合得分，橙色柱/线为惩罚后最终得分。
          综合惩罚因子最小值 <strong>Pmin = {{ overallPenalty.toFixed(4) }}</strong>
          （{{ penaltyConfig.length }} 个指标参与惩罚）。
        </p>
        <div ref="chartRef" class="chart-container" />

        <el-divider content-position="left">4.2 惩罚函数解释折线图</el-divider>
        <p class="chart-hint">
          横轴为指标得分 <code>x</code>（0~100 分），纵轴为惩罚因子 <code>F</code>。
          当前示例参数：<code>s={{ curveThreshold }}</code>，<code>m={{ curveM.toFixed(2) }}</code>。
          当 <code>x ≥ s</code> 时无惩罚（F=1），当 <code>x &lt; s</code> 时按 <code>(x/s)×m</code> 线性下降。
        </p>
        <div ref="functionChartRef" class="function-chart-container" />
      </el-card>

    </template>

    <!-- ==================== 加载配置弹窗 ==================== -->
    <el-dialog v-model="loadConfigDialogVisible" title="加载已保存的配置" width="480px">
      <el-empty v-if="savedConfigs.length === 0" description="暂无已保存的配置" :image-size="60" />
      <el-radio-group v-else v-model="selectedConfigName" style="width: 100%">
        <div v-for="cfg in savedConfigs" :key="cfg.name" class="saved-config-item">
          <el-radio :value="cfg.name">
            <strong>{{ cfg.name }}</strong>
            <span class="config-meta">{{ cfg.date }} · {{ cfg.config.indicators?.length ?? 0 }} 个指标</span>
          </el-radio>
          <el-button type="danger" link size="small" @click="deleteSavedConfig(cfg.name)">删除</el-button>
        </div>
      </el-radio-group>
      <template #footer>
        <el-button @click="loadConfigDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedConfigName" @click="applySavedConfig">加载</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { Warning, Document, Setting, Finished, TrendCharts, Refresh, ArrowUp, ArrowDown } from '@element-plus/icons-vue'
import { getCollectiveEvaluationIds, computeCollectiveResults, getScoreData, savePenaltyResults, getPenaltyResults, hasPenaltyResults, deletePenaltyResults } from '@/api'

// ==================== 18 个二级指标元数据 ====================
const allIndicators = [
  { key: 'security_key_leakage_qt',                  label: '密钥泄露得分',   category: '安全性' },
  { key: 'security_detected_probability_qt',          label: '被侦察得分',     category: '安全性' },
  { key: 'security_interception_resistance_ql',        label: '抗拦截得分',     category: '安全性' },
  { key: 'reliability_crash_rate_qt',                 label: '崩溃比例得分',   category: '可靠性' },
  { key: 'reliability_recovery_capability_qt',         label: '恢复能力得分',   category: '可靠性' },
  { key: 'reliability_communication_availability_qt',  label: '通信可用得分',   category: '可靠性' },
  { key: 'transmission_bandwidth_qt',                 label: '带宽得分',       category: '传输' },
  { key: 'transmission_call_setup_time_qt',           label: '呼叫建立得分',   category: '传输' },
  { key: 'transmission_transmission_delay_qt',        label: '传输时延得分',   category: '传输' },
  { key: 'transmission_bit_error_rate_qt',            label: '误码率得分',     category: '传输' },
  { key: 'transmission_throughput_qt',                label: '吞吐量得分',     category: '传输' },
  { key: 'transmission_spectral_efficiency_qt',       label: '频谱效率得分',   category: '传输' },
  { key: 'anti_jamming_sinr_qt',                     label: '信干噪比得分',   category: '抗干扰' },
  { key: 'anti_jamming_anti_jamming_margin_qt',        label: '抗干扰余量得分', category: '抗干扰' },
  { key: 'anti_jamming_communication_distance_qt',    label: '通信距离得分',   category: '抗干扰' },
  { key: 'effect_damage_rate_qt',                    label: '战损率得分',     category: '效能' },
  { key: 'effect_mission_completion_rate_qt',         label: '任务完成率得分', category: '效能' },
  { key: 'effect_blind_rate_qt',                     label: '致盲率得分',     category: '效能' },
]

// ==================== 状态 ====================
const evaluationIds = ref([])
const evaluationId = ref('')
const loading = ref(false)
const saving = ref(false)
const collectiveResult = ref(null)
const chartRef = ref(null)
const functionChartRef = ref(null)
const chartType = ref('bar')
let chartInstance = null
let functionChartInstance = null

const showTheory = ref(true)
const presetTemplate = ref('medium')
const batchThreshold = ref(70)
const batchM = ref(0.8)

// 选中的指标 key 列表
const selectedIndicatorKeys = ref(['reliability_crash_rate_qt', 'effect_mission_completion_rate_qt'])

// 惩罚配置：{ key, label, threshold, m, currentScore, factor }
const penaltyConfig = ref([])

const loadConfigDialogVisible = ref(false)
const savedConfigs = ref([])
const selectedConfigName = ref('')

// 惩罚计算结果
const penaltyResults = ref([])

const exampleData = [
  { x: 100, f: '1.0000', note: '无惩罚' },
  { x: 90,  f: '1.0000', note: '无惩罚' },
  { x: 80,  f: '1.0000', note: '无惩罚' },
  { x: 70,  f: '1.0000', note: '刚好达到阈值' },
  { x: 60,  f: '(60/70)×0.8 = 0.6857', note: '惩罚约 31%' },
  { x: 50,  f: '(50/70)×0.8 = 0.5714', note: '惩罚约 43%' },
  { x: 40,  f: '(40/70)×0.8 = 0.4571', note: '惩罚约 54%' },
]

// ==================== 计算 ====================
/** 全局综合惩罚因子（各作战相同） */
const overallPenalty = computed(() => {
  // 统计口径：结果表中所有作战任务的 P 最小值（最严格）
  if (!penaltyResults.value.length) return 1
  const vals = penaltyResults.value
    .map((r) => Number(r.overallPenalty))
    .filter((v) => Number.isFinite(v))
  if (!vals.length) return 1
  return Math.min(...vals)
})

const curveThreshold = computed(() => {
  if (penaltyConfig.value.length > 0) return Number(penaltyConfig.value[0].threshold || 70)
  return Number(batchThreshold.value || 70)
})

const curveM = computed(() => {
  if (penaltyConfig.value.length > 0) return Number(penaltyConfig.value[0].m || 0.8)
  return Number(batchM.value || 0.8)
})

/** 构建当前选中指标的惩罚配置项 */
function buildPenaltyItems() {
  const scoreRows = collectiveResult.value?.scoreRows ?? []
  const results = []
  for (const key of selectedIndicatorKeys.value) {
    const meta = allIndicators.find((i) => i.key === key)
    if (!meta) continue
    const existing = penaltyConfig.value.find((c) => c.key === key)
    const avgScore = computeAvgScore(key, scoreRows)
    const minRaw = computeMinRawScore(key, scoreRows)
    const x = avgScore != null ? avgScore * 100 : null
    const min100 = minRaw != null ? minRaw * 100 : null
    const factor = x != null ? computeFactor(x, existing?.threshold ?? 70, existing?.m ?? 0.8) : null
    const factorAtMin = min100 != null ? computeFactor(min100, existing?.threshold ?? 70, existing?.m ?? 0.8) : null
    results.push({
      key,
      label: meta.label,
      category: meta.category,
      threshold: existing?.threshold ?? 70,
      m: existing?.m ?? 0.8,
      currentScore: avgScore != null ? avgScore * 100 : null,
      batchMinScore: min100,
      factor,
      factorAtMin,
    })
  }
  penaltyConfig.value = results
  recalculate()
}

function computeAvgScore(key, rows) {
  if (!rows || !rows.length) return null
  const vals = rows.map((r) => Number(r[key])).filter((v) => v != null && !isNaN(v))
  if (!vals.length) return null
  return vals.reduce((a, b) => a + b, 0) / vals.length
}

/** 批次内该指标原始得分的最小值（与均值同一量纲，0~1），用于与结果表最低作战分口径对齐 */
function computeMinRawScore(key, rows) {
  if (!rows || !rows.length) return null
  const vals = rows.map((r) => Number(r[key])).filter((v) => v != null && !isNaN(v))
  if (!vals.length) return null
  return Math.min(...vals)
}

function computeFactor(x, threshold, m) {
  if (x >= threshold) return 1
  return (x / threshold) * m
}

function recalculate() {
  if (!collectiveResult.value) return
  const scoreRows = collectiveResult.value.scoreRows ?? []
  const results = collectiveResult.value.results ?? []

  // 配置区：Fi（均值）用批次算术平均；Fi（批次最低分）用批次最低得分，与结果表中最差作战该指标口径一致
  for (const cfg of penaltyConfig.value) {
    const avgScore = computeAvgScore(cfg.key, scoreRows)
    const score100 = avgScore != null ? avgScore * 100 : null
    const minRaw = computeMinRawScore(cfg.key, scoreRows)
    const min100 = minRaw != null ? minRaw * 100 : null
    cfg.currentScore = score100
    cfg.batchMinScore = min100
    cfg.factor = score100 != null ? computeFactor(score100, cfg.threshold, cfg.m) : null
    cfg.factorAtMin = min100 != null ? computeFactor(min100, cfg.threshold, cfg.m) : null
  }

  penaltyResults.value = results.map((r) => {
    const indicatorScores = {}
    const opFactors = {}
    for (const cfg of penaltyConfig.value) {
      const raw = scoreRows.find((sr) => String(sr.operation_id) === String(r.operationId))
      const rawVal = raw ? Number(raw[cfg.key]) : null
      const score = rawVal != null ? rawVal * 100 : null
      indicatorScores[cfg.key] = score
      opFactors[cfg.key] = score != null ? computeFactor(score, cfg.threshold, cfg.m) : 1
    }
    const factors = Object.values(opFactors)
    const op = factors.length > 0 ? Math.min(...factors) : 1
    const orig = Number(r.totalScore)
    const final = orig * op
    const amp = orig > 0 ? (orig - final) / orig : 0
    return {
      operationId: r.operationId,
      originalScore: orig,
      indicatorScores,
      penaltyFactors: opFactors,
      overallPenalty: op,
      finalScore: final,
      penaltyAmplitude: amp,
    }
  })
  // 本区块依赖 v-if="penaltyResults.length"，同步调用时 DOM 尚未挂载，ref 为 null，需等下一帧再绘制
  nextTick(() => {
    renderChart()
    renderFunctionChart()
  })
}

/** 计算表格行的样式类 */
function getScoreClass(score) {
  if (score == null) return ''
  if (score >= 70) return 'score-good'
  if (score >= 50) return 'score-warn'
  return 'score-bad'
}

function getPenaltyClass(factor) {
  if (factor == null) return ''
  if (factor >= 1) return 'fi-good'
  if (factor >= 0.8) return 'fi-warn'
  return 'fi-bad'
}

function getFinalScoreClass(orig, final) {
  if (orig == null || final == null) return ''
  if (final >= orig) return 'score-good'
  const drop = (orig - final) / orig
  if (drop >= 0.3) return 'score-bad'
  if (drop >= 0.1) return 'score-warn'
  return ''
}

// ==================== 预设模板 ====================
function applyPreset(template) {
  const presets = {
    light:  { threshold: 60, m: 0.9 },
    medium: { threshold: 70, m: 0.8 },
    heavy:  { threshold: 70, m: 0.5 },
  }
  const p = presets[template]
  if (!p) return
  batchThreshold.value = p.threshold
  batchM.value = p.m
  applyBatch()
}

function applyBatch() {
  for (const cfg of penaltyConfig.value) {
    cfg.threshold = batchThreshold.value
    cfg.m = batchM.value
  }
  recalculate()
}

function resetToDefaults() {
  selectedIndicatorKeys.value = ['reliability_crash_rate_qt', 'effect_mission_completion_rate_qt']
  batchThreshold.value = 70
  batchM.value = 0.8
  presetTemplate.value = 'medium'
}

function removeIndicator(key) {
  selectedIndicatorKeys.value = selectedIndicatorKeys.value.filter((k) => k !== key)
}

// ==================== 配置持久化 ====================
const STORAGE_KEY = 'penalty_model_configs'

function loadSavedConfigs() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    savedConfigs.value = raw ? JSON.parse(raw) : []
  } catch {
    savedConfigs.value = []
  }
}

function saveConfig() {
  if (!penaltyConfig.value.length) {
    ElMessage.warning('请先选择至少一个惩罚指标')
    return
  }
  ElMessageBox.prompt('请输入配置名称', '保存惩罚配置', {
    confirmButtonText: '保存',
    cancelButtonText: '取消',
    inputPattern: /\S+/,
    inputErrorMessage: '名称不能为空',
  }).then(({ value }) => {
    const cfg = {
      name: value,
      date: new Date().toLocaleString('zh-CN'),
      config: {
        selectedKeys: [...selectedIndicatorKeys.value],
        batchThreshold: batchThreshold.value,
        batchM: batchM.value,
      },
    }
    const existing = savedConfigs.value.findIndex((c) => c.name === value)
    if (existing >= 0) {
      savedConfigs.value[existing] = cfg
    } else {
      savedConfigs.value.push(cfg)
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(savedConfigs.value))
    ElMessage.success(`配置「${value}」已保存`)
  }).catch(() => {})
}

function applySavedConfig() {
  const cfg = savedConfigs.value.find((c) => c.name === selectedConfigName.value)
  if (!cfg) return
  const { selectedKeys, batchThreshold: bt, batchM: bm } = cfg.config
  selectedIndicatorKeys.value = selectedKeys
  batchThreshold.value = bt
  batchM.value = bm
  loadConfigDialogVisible.value = false
  ElMessage.success(`已加载配置「${selectedConfigName.value}」`)
}

function deleteSavedConfig(name) {
  savedConfigs.value = savedConfigs.value.filter((c) => c.name !== name)
  localStorage.setItem(STORAGE_KEY, JSON.stringify(savedConfigs.value))
  if (selectedConfigName.value === name) selectedConfigName.value = ''
}

// ==================== 惩罚结果保存与加载（数据库） ====================

/** 准备要保存的结果数据 */
function prepareResultsForSave() {
  if (!penaltyResults.value.length) return []

  // 构建批次统计口径数据
  const scoreRows = collectiveResult.value?.scoreRows ?? []
  const batchAvgScoreMap = {}
  const batchMinScoreMap = {}
  const batchAvgFiMap = {}
  const batchMinFiMap = {}

  for (const cfg of penaltyConfig.value) {
    const avg = computeAvgScore(cfg.key, scoreRows)
    const min = computeMinRawScore(cfg.key, scoreRows)
    batchAvgScoreMap[cfg.key] = avg
    batchMinScoreMap[cfg.key] = min
    batchAvgFiMap[cfg.key] = avg != null ? computeFactor(avg * 100, cfg.threshold, cfg.m) : null
    batchMinFiMap[cfg.key] = min != null ? computeFactor(min * 100, cfg.threshold, cfg.m) : null
  }

  return penaltyResults.value.map((r) => {
    const penaltyDetails = penaltyConfig.value.map((cfg) => ({
      indicatorKey: cfg.key,
      indicatorScore: r.indicatorScores?.[cfg.key] != null
        ? Number(r.indicatorScores[cfg.key])
        : null,
      threshold: Number(cfg.threshold),
      m: Number(cfg.m),
      fi: r.penaltyFactors?.[cfg.key] != null
        ? Number(r.penaltyFactors[cfg.key])
        : null,
    }))

    return {
      evaluationId: evaluationId.value,
      operationId: r.operationId,
      originalScore: Number(r.originalScore),
      overallPenalty: Number(r.overallPenalty),
      finalScore: Number(r.finalScore),
      penaltyAmplitude: Number(r.penaltyAmplitude),
      penaltyDetails,
      batchAvgScoreMap,
      batchMinScoreMap,
      batchAvgFiMap,
      batchMinFiMap,
    }
  })
}

/** 保存惩罚结果到数据库 */
async function savePenaltyToDatabase() {
  if (!evaluationId.value) {
    ElMessage.warning('请先选择评估批次')
    return
  }
  if (!penaltyResults.value.length) {
    ElMessage.warning('请先加载综合结果并配置惩罚参数')
    return
  }

  try {
    saving.value = true
    const results = prepareResultsForSave()
    await savePenaltyResults(evaluationId.value, results)
    ElMessage.success('惩罚计算结果已保存到数据库')
  } catch (e) {
    console.error(e)
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

/** 从数据库加载惩罚结果 */
async function loadPenaltyFromDatabase() {
  if (!evaluationId.value) {
    ElMessage.warning('请先选择评估批次')
    return
  }

  try {
    saving.value = true
    const results = await getPenaltyResults(evaluationId.value)
    if (!results || !results.length) {
      ElMessage.info('该批次暂无保存的惩罚结果')
      return
    }
    ElMessage.success(`已加载 ${results.length} 条保存的惩罚结果`)
    // TODO: 可以在这里恢复惩罚配置和结果到界面
    console.log('加载的惩罚结果:', results)
  } catch (e) {
    console.error(e)
    ElMessage.error(e?.message || '加载失败')
  } finally {
    saving.value = false
  }
}

// ==================== 图表 ====================
function renderChart() {
  if (!chartRef.value || !penaltyResults.value.length) return
  if (!chartInstance) chartInstance = echarts.init(chartRef.value)
  const results = penaltyResults.value
  const labels = results.map((r) => `实验${r.operationId}`)
  const origData = results.map((r) => r.originalScore)
  const finalData = results.map((r) => r.finalScore)
  const type = chartType.value === 'line' ? 'line' : 'bar'

  const option = {
    title: {
      text: '惩罚前后综合得分对比',
      left: 'center',
      textStyle: { fontSize: 15 },
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter(params) {
        if (!params?.length) return ''
        const lines = params.map((p) =>
          `${p.marker} ${p.seriesName}: <strong>${Number(p.value).toFixed(4)}</strong>`
        )
        const p2 = params[0]
        const r = results.find((x) => `实验${x.operationId}` === p2?.name)
        if (r) {
          lines.push(`<span style="font-size:11px; color:#909399">综合惩罚因子 P=${r.overallPenalty.toFixed(4)}</span>`)
        }
        return `<strong>${p2.name}</strong><br/>${lines.join('<br/>')}`
      },
    },
    legend: {
      data: ['惩罚前（原始）', '惩罚后（最终）'],
      bottom: 0,
      left: 'center',
    },
    grid: { left: '3%', right: '4%', top: 52, bottom: 60, containLabel: true },
    xAxis: {
      type: 'category',
      data: labels,
      axisLabel: { fontSize: 12 },
    },
    yAxis: {
      type: 'value',
      name: '综合得分',
      min: 0,
      max: 1,
      axisLabel: { formatter: (v) => Number(v).toFixed(2) },
      splitLine: { lineStyle: { type: 'dashed' } },
    },
    series: [
      {
        name: '惩罚前（原始）',
        type,
        data: origData,
        itemStyle: { color: '#409EFF' },
        barMaxWidth: 28,
        lineStyle: type === 'line' ? { width: 2 } : undefined,
        smooth: type === 'line',
        symbolSize: type === 'line' ? 7 : undefined,
      },
      {
        name: '惩罚后（最终）',
        type,
        data: finalData,
        itemStyle: { color: '#F56C6C' },
        barMaxWidth: 28,
        lineStyle: type === 'line' ? { width: 2 } : undefined,
        smooth: type === 'line',
        symbolSize: type === 'line' ? 7 : undefined,
      },
    ],
  }
  chartInstance.setOption(option, true)
  chartInstance.resize()
}

function renderFunctionChart() {
  if (!functionChartRef.value) return
  if (!functionChartInstance) functionChartInstance = echarts.init(functionChartRef.value)

  const s = Math.max(1, Number(curveThreshold.value || 70))
  const m = Math.max(0.1, Math.min(1, Number(curveM.value || 0.8)))
  const x = Array.from({ length: 101 }, (_, i) => i)
  const y = x.map((v) => (v >= s ? 1 : (v / s) * m))

  const option = {
    title: {
      text: '惩罚函数曲线（单指标）',
      left: 'center',
      textStyle: { fontSize: 14 },
    },
    tooltip: {
      trigger: 'axis',
      formatter(params) {
        if (!params?.length) return ''
        const p = params[0]
        return `x=${p.axisValue} 分<br/>F=${Number(p.value).toFixed(4)}`
      },
    },
    grid: { left: '4%', right: '4%', top: 44, bottom: 38, containLabel: true },
    xAxis: {
      type: 'value',
      min: 0,
      max: 100,
      name: '指标得分 x（分）',
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 1.05,
      name: '惩罚因子 F',
      axisLabel: { formatter: (v) => Number(v).toFixed(2) },
    },
    series: [
      {
        type: 'line',
        smooth: false,
        symbol: 'none',
        lineStyle: { width: 3, color: '#E6A23C' },
        data: x.map((v, idx) => [v, y[idx]]),
        markLine: {
          symbol: ['none', 'none'],
          label: { formatter: `阈值 s=${s}` },
          lineStyle: { type: 'dashed', color: '#909399' },
          data: [{ xAxis: s }],
        },
        markPoint: {
          symbolSize: 40,
          label: { formatter: '拐点' },
          itemStyle: { color: '#409EFF' },
          data: [{ coord: [s, 1] }],
        },
      },
    ],
  }
  functionChartInstance.setOption(option, true)
  functionChartInstance.resize()
}

// ==================== 生命周期 ====================
async function loadEvaluationIds() {
  try {
    const ids = await getCollectiveEvaluationIds()
    evaluationIds.value = Array.isArray(ids) ? ids : []
  } catch (e) {
    console.error(e)
    evaluationIds.value = []
  }
}

async function loadResults() {
  if (!evaluationId.value) return
  loading.value = true
  try {
    const [data, scoreRows] = await Promise.all([
      computeCollectiveResults(evaluationId.value),
      getScoreData(evaluationId.value),
    ])
    collectiveResult.value = { ...data, scoreRows: Array.isArray(scoreRows) ? scoreRows : [] }
    buildPenaltyItems()
    ElMessage.success('综合结果已加载，开始惩罚分析')
    await nextTick()
    renderChart()
    renderFunctionChart()
  } catch (e) {
    collectiveResult.value = null
    penaltyResults.value = []
    ElMessage.error(e?.message || '加载综合结果失败')
  } finally {
    loading.value = false
  }
}

function onBatchChange() {
  collectiveResult.value = null
  penaltyResults.value = []
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
  if (functionChartInstance) {
    functionChartInstance.dispose()
    functionChartInstance = null
  }
}

watch(chartType, () => {
  renderChart()
})

/** 勾选/取消惩罚指标时需同步 penaltyConfig 与结果表（原先仅 v-model 未触发构建） */
watch(
  selectedIndicatorKeys,
  () => {
    buildPenaltyItems()
  },
  { deep: true },
)

watch([curveThreshold, curveM], () => {
  renderFunctionChart()
})

onMounted(() => {
  loadEvaluationIds()
  loadSavedConfigs()
})
</script>

<style scoped lang="scss">
.penalty-page {
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

// ==================== 理论说明 ====================
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
  ul {
    margin: 4px 0;
    padding-left: 20px;
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

// ==================== 配置区 ====================
.batch-config-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  border: 1px solid #e4e8ef;

  .batch-label {
    font-size: 13px;
    color: #606266;
    font-weight: 500;
  }
}

.indicator-select-row {
  margin-bottom: 12px;

  .select-label {
    font-size: 13px;
    color: #606266;
    font-weight: 500;
    display: block;
    margin-bottom: 6px;
  }
}

.config-table-hint {
  margin: 0 0 10px;
  font-size: 12px;
  line-height: 1.65;
  color: #606266;
}

.penalty-config-table {
  .penalty-cell {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;

    .fi-label {
      font-size: 10px;
      color: #909399;
    }
  }
}

// 样式类
.score-good { color: #67C23A; font-weight: 600; }
.score-warn { color: #E6A23C; font-weight: 600; }
.score-bad  { color: #F56C6C; font-weight: 600; }

.fi-good { color: #67C23A; }
.fi-warn { color: #E6A23C; }
.fi-bad  { color: #F56C6C; }

.score-orig {
  color: #409EFF;
  font-weight: 500;
}
.score-final {
  font-weight: 700;
}

.penalty-amplitude {
  color: #F56C6C;
  font-weight: 600;
  font-size: 12px;
}
.no-penalty {
  color: #67C23A;
  font-size: 12px;
}

.penalty-result-table {
  .penalty-cell {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 1px;

    .fi-label {
      font-size: 10px;
      color: #909399;
    }
  }
}

.empty-hint {
  padding: 24px 0;
}

.chart-card {
  .chart-hint {
    margin: 0 0 12px;
    font-size: 13px;
    color: #606266;
  }
}

.chart-container {
  width: 100%;
  height: 400px;
}

.function-chart-container {
  width: 100%;
  min-width: 0;
  height: 320px;
  min-height: 320px;
}

// ==================== 保存配置弹窗 ====================
.saved-config-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 4px;
  border-bottom: 1px solid #f0f0f0;

  .config-meta {
    margin-left: 12px;
    font-size: 11px;
    color: #909399;
  }
}
</style>
