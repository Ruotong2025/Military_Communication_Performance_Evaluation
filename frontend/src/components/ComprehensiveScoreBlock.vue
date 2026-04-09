<template>
    <!-- ==================== 综合评分：集结计算 + 指标 + 得分 ==================== -->
    <el-card class="collective-card comprehensive-score-outer" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><Histogram /></el-icon> 综合评分</span>
        </div>
      </template>

      <el-tabs
        v-model="comprehensiveScoreInnerTab"
        type="border-card"
        class="comprehensive-score-tabs"
        @tab-change="onComprehensiveInnerTabChange"
      >
        <el-tab-pane label="集结与矩阵" name="aggregate">
          <el-divider content-position="left">
            <el-icon><TrendCharts /></el-icon> 三、专家集结计算
          </el-divider>

    <!-- 集结参数配置 -->
    <el-card class="collective-card inner-nested-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><Setting /></el-icon> 集结参数配置</span>
        </div>
      </template>

      <el-form :inline="true" :model="collectiveForm" label-width="100px">
        <el-form-item label="评估批次">
          <el-select
            v-model="collectiveForm.evaluationId"
            placeholder="请选择评估批次"
            clearable
            filterable
            style="width: 380px"
            @change="onEvaluationIdChange"
          >
            <el-option
              v-for="id in evaluationIds"
              :key="id"
              :label="id"
              :value="id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="专家选择">
          <el-radio-group v-model="collectiveForm.useAllExperts" size="default">
            <el-radio-button :value="true">全部专家</el-radio-button>
            <el-radio-button :value="false">指定专家</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="!collectiveForm.useAllExperts" label="指定专家">
          <el-select
            v-model="collectiveForm.expertIds"
            multiple
            placeholder="请选择专家"
            style="width: 300px"
          >
            <el-option
              v-for="e in availableExperts"
              :key="e.expertId"
              :label="`${e.expertId} - ${e.expertName}`"
              :value="e.expertId"
            />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button
            type="info"
            :icon="View"
            :loading="previewLoading"
            :disabled="!collectiveForm.evaluationId"
            @click="previewWeights"
          >
            预览集结权重
          </el-button>
          <el-button
            type="primary"
            :icon="Cpu"
            :loading="calculateLoading"
            :disabled="!collectiveForm.evaluationId"
            @click="executeCalculation"
          >
            执行集结计算
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ==================== 1. 专家权重明细 ==================== -->
    <el-card v-if="activeCollectiveWeights" class="collective-card inner-nested-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><User /></el-icon> 一、专家权重明细</span>
          <el-tag type="success" size="small">参与专家 {{ activeCollectiveWeights.expertCount }} 位</el-tag>
        </div>
      </template>
      <el-table :data="activeCollectiveWeights.expertWeights" border stripe size="small" max-height="280">
        <el-table-column prop="expertId" label="专家ID" width="80" align="center" />
        <el-table-column prop="expertName" label="姓名" width="120" />
        <el-table-column label="自身可信度" align="center" width="120">
          <template #default="{ row }">
            <el-tag type="info">{{ Number(row.credibility || 0).toFixed(2) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <p class="card-hint">自身可信度来自可信度评估表，取值 0~100。</p>
    </el-card>

    <!-- ==================== 2. 一致性检验 ==================== -->
    <el-card v-if="activeCollectiveWeights" class="collective-card inner-nested-card" shadow="hover">
      <template #header>
        <span><el-icon><PieChart /></el-icon> 二、一致性检验（CR &lt; 0.1 为通过）</span>
      </template>
      <el-row :gutter="12">
        <el-col :span="4" v-for="(cr, key) in activeCollectiveWeights.crResults" :key="key">
          <div class="cr-item">
            <span class="cr-label">{{ crLabelMap[key] || key }}</span>
            <el-tag :type="(Number(cr) < 0.1) ? 'success' : 'danger'" size="large">
              {{ Number(cr).toFixed(4) }}
            </el-tag>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- ==================== 3. 集体判断矩阵 ==================== -->
    <el-card v-if="activeCollectiveWeights" class="collective-card inner-nested-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><Grid /></el-icon> 三、集体判断矩阵</span>
        </div>
      </template>

      <!-- 矩阵选择器 -->
      <div class="matrix-selector">
        <el-radio-group v-model="selectedMatrix" size="large">
          <el-radio-button value="dim">维度层矩阵 (5×5)</el-radio-button>
          <el-radio-button value="security">安全性指标层 (3×3)</el-radio-button>
          <el-radio-button value="reliability">可靠性指标层 (3×3)</el-radio-button>
          <el-radio-button value="transmission">传输能力指标层 (6×6)</el-radio-button>
          <el-radio-button value="antiJamming">抗干扰指标层 (3×3)</el-radio-button>
          <el-radio-button value="effect">效能影响指标层 (3×3)</el-radio-button>
        </el-radio-group>
        <el-tag :type="currentMatrixCR < 0.1 ? 'success' : 'danger'" size="large" style="margin-left: 16px">
          CR = {{ currentMatrixCR.toFixed(4) }}
          {{ currentMatrixCR < 0.1 ? '（一致性通过）' : '（一致性未通过）' }}
        </el-tag>
      </div>

      <!-- 矩阵表格 -->
      <div class="matrix-table-container">
        <el-table :data="currentMatrixData" border size="small" class="matrix-table">
          <el-table-column prop="row" label="" width="120" fixed />
          <el-table-column
            v-for="(col, idx) in currentMatrixHeaders"
            :key="idx"
            :prop="col"
            :label="col"
            align="center"
            width="100"
          />
        </el-table>
      </div>

      <!-- 矩阵内嵌权重向量 -->
      <div class="matrix-weights">
        <h5>{{ selectedMatrix === 'dim' ? '维度' : getMatrixIndicatorName(selectedMatrix) }}层权重向量</h5>
        <el-row :gutter="12">
          <el-col :span="4" v-for="(w, idx) in currentMatrixWeights" :key="idx">
            <div class="weight-card">
              <span class="weight-name">{{ currentMatrixHeaders[idx] }}</span>
              <strong>{{ (w * 100).toFixed(2) }}%</strong>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- ==================== 4. 维度与指标综合权重 ==================== -->
    <el-card v-if="activeCollectiveWeights" class="collective-card inner-nested-card" shadow="hover">
      <template #header>
        <span><el-icon><PieChart /></el-icon> 四、维度与指标综合权重</span>
      </template>

      <!-- 维度权重（文字卡片） -->
      <div class="preview-section">
        <h5>4.1 一级维度集结权重</h5>
        <el-row :gutter="12">
          <el-col :span="4" v-for="(w, dim) in activeCollectiveWeights.dimensionWeights" :key="dim">
            <div class="weight-item">
              <span class="weight-label">{{ dim }}</span>
              <strong>{{ (Number(w) * 100).toFixed(2) }}%</strong>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 维度权重柱状图 -->
      <div class="weight-chart-section">
        <h5>4.2 维度层权重分布图</h5>
        <div ref="dimWeightChartRef" class="chart-container-medium"></div>
      </div>

      <!-- 二级指标综合权重表格 -->
      <div class="preview-section">
        <h5>4.3 二级指标综合权重（对总目标，归一化）</h5>
        <el-table :data="indicatorWeightTableData" border stripe size="small" max-height="320">
          <el-table-column prop="dimension" label="维度" width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small">{{ row.dimension }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="indicator" label="指标" min-width="140" />
          <el-table-column label="综合权重" align="center" width="130">
            <template #default="{ row }">
              <span class="score-cell">{{ (Number(row.weight) * 100).toFixed(2) }}%</span>
            </template>
          </el-table-column>
          <el-table-column label="权重条" min-width="160">
            <template #default="{ row }">
              <el-progress
                :percentage="Number(row.weight) * 100"
                :stroke-width="10"
                :show-text="false"
              />
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 指标权重柱状图 -->
      <div class="weight-chart-section">
        <h5>4.4 二级指标综合权重分布图</h5>
        <div ref="indWeightChartRef" class="chart-container-large"></div>
      </div>
    </el-card>

        </el-tab-pane>

        <el-tab-pane label="原始指标" name="metrics">
    <!-- ==================== 5. 原始指标对照 ==================== -->
          <el-empty
            v-if="!collectiveForm.evaluationId"
            description="请先在「集结与矩阵」中选择评估批次"
          />
          <el-card v-else class="collective-card metrics-raw-card inner-nested-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><List /></el-icon> 五、原始指标对照（metrics_military_comm_effect）</span>
          <el-tag type="info" size="small">{{ metricsRows.length }} 条作战记录</el-tag>
        </div>
      </template>
      <p class="metrics-hint">
        以下为「计算指标」写入库的<strong>未归一化</strong>原始值；经「生成归一化 score」后与上方集结权重相乘得到综合得分。字段含义见下方说明表。
      </p>
      <el-table
        v-loading="metricsLoading"
        :data="metricsRows"
        border
        stripe
        size="small"
        max-height="380"
        class="metrics-raw-table"
      >
        <el-table-column prop="operation_id" label="作战ID" width="90" fixed align="center" />
        <el-table-column
          v-for="col in metricsFieldMeta"
          :key="col.prop"
          :prop="col.prop"
          :label="col.label"
          align="center"
          min-width="108"
        >
          <template #default="{ row }">
            {{ formatMetricCell(row[col.prop]) }}
          </template>
        </el-table-column>
      </el-table>

      <el-divider content-position="left">归一化得分（score_military_comm_effect）</el-divider>
      <p class="metrics-hint">
        以下为该评估批次内对原始指标做 <strong>Min-Max</strong> 归一化后的得分（0~1，越大越好），数据来自
        <code>score_military_comm_effect</code>；需先在指标计算流程中执行「生成归一化 score」。
      </p>
      <el-table
        v-loading="scoreLoading"
        :data="scoreRows"
        border
        stripe
        size="small"
        max-height="380"
        class="metrics-raw-table"
      >
        <el-table-column prop="operation_id" label="作战ID" width="90" fixed align="center" />
        <el-table-column
          v-for="col in scoreFieldMeta"
          :key="col.prop"
          :prop="col.prop"
          :label="col.label"
          align="center"
          min-width="100"
        >
          <template #default="{ row }">
            {{ formatScoreCell(row[col.prop]) }}
          </template>
        </el-table-column>
      </el-table>

      <el-collapse class="metrics-legend-collapse">
        <el-collapse-item title="指标字段含义与对应 AHP 打分项" name="legend">
          <el-table :data="metricsFieldMeta" border size="small" max-height="320">
            <el-table-column prop="category" label="维度" width="100" align="center" />
            <el-table-column prop="label" label="字段（列名）" min-width="160" />
            <el-table-column prop="prop" label="库字段名" width="200">
              <template #default="{ row }">
                <code class="code-field">{{ row.prop }}</code>
              </template>
            </el-table-column>
            <el-table-column prop="meaning" label="含义" min-width="260" />
            <el-table-column prop="scoreName" label="对应归一化得分项" width="140" align="center" />
          </el-table>
        </el-collapse-item>
      </el-collapse>
          </el-card>

        </el-tab-pane>

        <el-tab-pane label="综合评估得分" name="scores">
          <el-empty
            v-if="!collectiveForm.evaluationId"
            description="请先在「集结与矩阵」中选择评估批次"
          />
          <el-empty v-else-if="!collectiveResult" description="请先执行「执行集结计算」生成集结权重" />
          <el-empty
            v-else-if="!collectiveResult.results?.length"
            description="尚未生成本批次的综合得分（权重 × 归一化 score）。与「评估结果计算」页「加载综合结果」等价。"
          >
            <el-button
              type="primary"
              :icon="Refresh"
              :loading="computeScoresLoading"
              @click="loadComprehensiveScores"
            >
              生成综合得分
            </el-button>
          </el-empty>
          <template v-else>
            <div class="scores-tab-toolbar">
              <p class="scores-tab-title">
                <el-icon><Finished /></el-icon>
                六、综合评估得分（集结权重 × 归一化 score）
              </p>
              <div class="scores-tab-actions">
                <el-button
                  type="primary"
                  plain
                  size="small"
                  :icon="Refresh"
                  :loading="computeScoresLoading"
                  @click="loadComprehensiveScores"
                >
                  重新生成
                </el-button>
                <el-button type="danger" size="small" :icon="Delete" @click="deleteResults">
                  删除结果
                </el-button>
              </div>
            </div>

      <!-- 6.1 一级维度：按实验分组对比（第一张，上方） -->
      <div class="result-section">
        <div class="result-toolbar">
          <h5>6.1 各作战任务一级维度得分对比</h5>
          <el-radio-group v-model="primaryDimChartType" size="small" @change="renderPrimaryDimensionByExperimentChart">
            <el-radio-button value="bar">分组柱状图</el-radio-button>
            <el-radio-button value="line">折线图</el-radio-button>
          </el-radio-group>
        </div>
        <p class="chart-hint">
          横轴为各作战实验；不同颜色代表五大一级维度得分（0~1）。纵轴固定 0~1。底部<strong>滑块</strong>可缩放横轴，也可在图内<strong>滚轮</strong>缩放、按住拖拽平移。
        </p>
        <div ref="primaryDimByExperimentChartRef" class="chart-container-large"></div>
      </div>

      <!-- 6.2 二级指标（18 项）：按实验对比（第二张，下方） -->
      <div class="result-section">
        <div class="result-toolbar">
          <h5>6.2 各作战任务二级指标加权得分对比</h5>
          <el-radio-group v-model="dimensionScoreChartType" size="small" @change="renderDimensionScoreDetailChart">
            <el-radio-button value="bar">分组柱状图</el-radio-button>
            <el-radio-button value="line">折线图</el-radio-button>
          </el-radio-group>
        </div>
        <p class="chart-hint">
          柱高为 <strong>归一化得分 × 该指标集结二级权重</strong>（对综合分的分项贡献）。横轴为 18 个二级指标，标签下行为权重占比。纵轴为加权得分，刻度随数据自动缩放。请用底部滑块或图内滚轮缩放横轴。
        </p>
        <div ref="dimensionScoreChartRef" class="chart-container-large"></div>
      </div>

      <!-- 6.3 各作战任务综合得分对比（原 6.1 位置） -->
      <div class="result-section">
        <h5>6.3 各作战任务综合得分对比</h5>
        <div ref="scoreChartRef" class="chart-container-large"></div>
      </div>
          </template>
        </el-tab-pane>
      </el-tabs>
    </el-card>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import request from '../utils/request'
import {
  getCollectiveEvaluationIds,
  previewCollectiveWeights,
  executeCollectiveCalculation,
  computeCollectiveResults,
  deleteCollectiveResults
} from '../api/index'
import {
  Setting,
  View,
  Cpu,
  User,
  PieChart,
  Grid,
  List,
  Finished,
  Delete,
  Refresh,
  Histogram,
  TrendCharts
} from '@element-plus/icons-vue'

const activeMainTab = ref('evaluation')
/** 综合评分区内子 Tab：集结与矩阵 / 原始指标 / 综合评估得分 */
const comprehensiveScoreInnerTab = ref('aggregate')
const dimensionChartRef = ref(null)
const indicatorChartRef = ref(null)
let dimensionChart = null
let indicatorChart = null

// ==================== 集结计算状态 ====================
const collectiveLoading = ref(false)
const previewLoading = ref(false)
const calculateLoading = ref(false)
const computeScoresLoading = ref(false)
const evaluationIds = ref([])
const availableExperts = ref([])
const weightPreview = ref(null)
const collectiveResult = ref(null)

// 矩阵展示相关
const selectedMatrix = ref('dim')
const scoreChartRef = ref(null)
const dimWeightChartRef = ref(null)
const indWeightChartRef = ref(null)
const dimensionScoreChartRef = ref(null)
const primaryDimByExperimentChartRef = ref(null)
let scoreChart = null
let dimWeightChart = null
let indWeightChart = null
let dimensionScoreChart = null
let primaryDimByExperimentChart = null

/** 6.1 / 6.2 图表类型 */
const dimensionScoreChartType = ref('bar')
const primaryDimChartType = ref('bar')

// ==================== 模拟打分状态 ====================
const expertOptions = ref([])
const simulateDialogVisible = ref(false)
const simulateExpertIds = ref([])
const simulating = ref(false)

async function fetchExpertOptions() {
  try {
    const list = await getExpertList()
    expertOptions.value = Array.isArray(list) ? list : []
  } catch (e) {
    console.error(e)
    ElMessage.error('加载专家列表失败：' + (e.message || ''))
  }
}

function openSimulateDialog() {
  simulateDialogVisible.value = true
}

function onSimulateDialogOpen() {
  if (simulateExpertIds.value.length === 0) {
    simulateExpertIds.value = []
  }
}

async function runSimulateBatch() {
  if (!simulateExpertIds.value.length) {
    ElMessage.warning('请至少选择一名专家')
    return
  }
  simulating.value = true
  try {
    const res = await simulateExpertAhpScores({
      expertIds: simulateExpertIds.value
    })
    const skip = res.skippedCount ?? 0
    const eqN = res.equipmentInsertedCount ?? res.insertedCount
    ElMessage.success(
      `效能指标 AHP：已写入 ${res.insertedCount} 名专家，跳过 ${skip} 名；装备操作 AHP：已写入 ${eqN} 名专家`
    )
    simulateDialogVisible.value = false
  } catch (e) {
    console.error(e)
    ElMessage.error(e?.message || '模拟失败')
  } finally {
    simulating.value = false
  }
}

const collectiveForm = ref({
  evaluationId: '',
  useAllExperts: true,
  expertIds: []
})

/** 与 metrics_military_comm_effect 列一致（MySQL 返回小写字段名） */
const metricsFieldMeta = [
  { prop: 'security_key_leakage_count', label: '密钥泄露次数', category: '安全性', meaning: '统计窗口内密钥泄露事件次数；越小越好，归一化后进入「密钥泄露得分」。', scoreName: '密钥泄露得分' },
  { prop: 'security_detected_probability', label: '被侦察概率', category: '安全性', meaning: '被侦察通信占比(%)；越小越好，归一化后进入「被侦察得分」。', scoreName: '被侦察得分' },
  { prop: 'security_interception_resistance_probability', label: '抗拦截(被拦占比)', category: '安全性', meaning: '被拦截通信占比(%)；数值含义与业务定义一致，归一化后进入「抗拦截得分」。', scoreName: '抗拦截得分' },
  { prop: 'reliability_crash_count', label: '网络崩溃次数', category: '可靠性', meaning: '链路崩溃类中断次数；越少越好，归一化后进入「崩溃比例得分」。', scoreName: '崩溃比例得分' },
  { prop: 'reliability_recovery_time', label: '平均恢复时间', category: '可靠性', meaning: '成功恢复事件的平均耗时(ms)；越短越好，归一化后进入「恢复能力得分」。', scoreName: '恢复能力得分' },
  { prop: 'reliability_communication_availability', label: '通信可用性', category: '可靠性', meaning: '可用时间占比(%)；越大越好，归一化后进入「通信可用得分」。', scoreName: '通信可用得分' },
  { prop: 'transmission_bandwidth', label: '平均带宽', category: '传输能力', meaning: '平均带宽(Mbps)；越大越好，归一化后进入「带宽得分」。', scoreName: '带宽得分' },
  { prop: 'transmission_call_setup_time', label: '呼叫建立时间', category: '传输能力', meaning: '平均呼叫建立耗时(ms)；越短越好，归一化后进入「呼叫建立得分」。', scoreName: '呼叫建立得分' },
  { prop: 'transmission_delay', label: '传输时延', category: '传输能力', meaning: '平均传输时延(ms)；越短越好，归一化后进入「传输时延得分」。', scoreName: '传输时延得分' },
  { prop: 'transmission_bit_error_rate', label: '误码率', category: '传输能力', meaning: '平均误码率(%)；越低越好，归一化后进入「误码率得分」。', scoreName: '误码率得分' },
  { prop: 'transmission_throughput', label: '吞吐量', category: '传输能力', meaning: '平均吞吐量(Mbps)；越大越好，归一化后进入「吞吐量得分」。', scoreName: '吞吐量得分' },
  { prop: 'transmission_spectral_efficiency', label: '频谱效率', category: '传输能力', meaning: 'bit/Hz 类效率指标；越大越好，归一化后进入「频谱效率得分」。', scoreName: '频谱效率得分' },
  { prop: 'anti_jamming_sinr', label: '信干噪比', category: '抗干扰能力', meaning: '平均 SINR(dB)；越大越好，归一化后进入「信干噪比得分」。', scoreName: '信干噪比得分' },
  { prop: 'anti_jamming_margin', label: '抗干扰余量', category: '抗干扰能力', meaning: '平均抗干扰余量(dB)；越大越好，归一化后进入「抗干扰余量得分」。', scoreName: '抗干扰余量得分' },
  { prop: 'anti_jamming_communication_distance', label: '通信距离', category: '抗干扰能力', meaning: '平均通信距离(km)；越大越好，归一化后进入「通信距离得分」。', scoreName: '通信距离得分' },
  { prop: 'effect_damage_rate', label: '毁伤率', category: '效能影响', meaning: '装备毁伤占比(%)；越低越好，归一化后进入「战损率得分」。', scoreName: '战损率得分' },
  { prop: 'effect_mission_completion_rate', label: '任务完成率', category: '效能影响', meaning: '通信成功占比(%)；越高越好，归一化后进入「任务完成率得分」。', scoreName: '任务完成率得分' },
  { prop: 'effect_blind_rate', label: '盲区率', category: '效能影响', meaning: '孤立节点占比(%)；越低越好，归一化后进入「致盲率得分」。', scoreName: '致盲率得分' }
]

const scoreFieldMeta = [
  { prop: 'security_key_leakage_qt', label: '密钥泄露得分' },
  { prop: 'security_detected_probability_qt', label: '被侦察得分' },
  { prop: 'security_interception_resistance_ql', label: '抗拦截得分' },
  { prop: 'reliability_crash_rate_qt', label: '崩溃比例得分' },
  { prop: 'reliability_recovery_capability_qt', label: '恢复能力得分' },
  { prop: 'reliability_communication_availability_qt', label: '通信可用得分' },
  { prop: 'transmission_bandwidth_qt', label: '带宽得分' },
  { prop: 'transmission_call_setup_time_qt', label: '呼叫建立得分' },
  { prop: 'transmission_transmission_delay_qt', label: '传输时延得分' },
  { prop: 'transmission_bit_error_rate_qt', label: '误码率得分' },
  { prop: 'transmission_throughput_qt', label: '吞吐量得分' },
  { prop: 'transmission_spectral_efficiency_qt', label: '频谱效率得分' },
  { prop: 'anti_jamming_sinr_qt', label: '信干噪比得分' },
  { prop: 'anti_jamming_anti_jamming_margin_qt', label: '抗干扰余量得分' },
  { prop: 'anti_jamming_communication_distance_qt', label: '通信距离得分' },
  { prop: 'effect_damage_rate_qt', label: '战损率得分' },
  { prop: 'effect_mission_completion_rate_qt', label: '任务完成率得分' },
  { prop: 'effect_blind_rate_qt', label: '致盲率得分' }
]

const INDICATOR_LABELS = scoreFieldMeta.map((c) => c.label)

const metricsRows = ref([])
const metricsLoading = ref(false)
const scoreRows = ref([])
const scoreLoading = ref(false)

/** 优先使用最近一次「执行集结计算」结果，否则用「预览」数据，保证矩阵与图表有数据 */
const activeCollectiveWeights = computed(() => collectiveResult.value ?? weightPreview.value)

function formatMetricCell(v) {
  if (v === null || v === undefined || v === '') return '—'
  const n = Number(v)
  if (!Number.isFinite(n)) return String(v)
  return Math.abs(n) >= 1000 ? n.toFixed(2) : n.toFixed(4)
}

function formatScoreCell(v) {
  if (v === null || v === undefined || v === '') return '—'
  const n = Number(v)
  if (!Number.isFinite(n)) return String(v)
  return n.toFixed(2)
}

async function loadMetricsForBatch() {
  const id = collectiveForm.value.evaluationId
  if (!id) {
    metricsRows.value = []
    scoreRows.value = []
    return
  }
  metricsLoading.value = true
  scoreLoading.value = true
  try {
    const list = await request.get('/metrics/results', { params: { evaluationId: id } })
    metricsRows.value = Array.isArray(list) ? normalizeMetricsRows(list) : []
  } catch (e) {
    console.error('加载原始指标失败:', e)
    metricsRows.value = []
  } finally {
    metricsLoading.value = false
  }
  try {
    const scores = await request.get('/metrics/scores', { params: { evaluationId: id } })
    scoreRows.value = Array.isArray(scores) ? normalizeMetricsRows(scores) : []
  } catch (e) {
    console.error('加载归一化 score 失败:', e)
    scoreRows.value = []
  } finally {
    scoreLoading.value = false
  }
}

/** 统一小写字段名，避免驱动返回别名不一致 */
function normalizeMetricsRows(list) {
  return list.map((row) => {
    const o = {}
    for (const [k, val] of Object.entries(row)) {
      o[String(k).toLowerCase()] = val
    }
    return o
  })
}
// 矩阵元素定义
const matrixConfig = {
  dim: {
    name: '维度层',
    headers: ['安全性', '可靠性', '传输能力', '抗干扰能力', '效能影响']
  },
  security: {
    name: '安全性',
    headers: ['密钥泄露得分', '被侦察得分', '抗拦截得分']
  },
  reliability: {
    name: '可靠性',
    headers: ['崩溃比例得分', '恢复能力得分', '通信可用得分']
  },
  transmission: {
    name: '传输能力',
    headers: ['带宽得分', '呼叫建立得分', '传输时延得分', '误码率得分', '吞吐量得分', '频谱效率得分']
  },
  antiJamming: {
    name: '抗干扰能力',
    headers: ['信干噪比得分', '抗干扰余量得分', '通信距离得分']
  },
  effect: {
    name: '效能影响',
    headers: ['战损率得分', '任务完成率得分', '致盲率得分']
  }
}

const crLabelMap = {
  dim: '维度层',
  '安全性': '安全性',
  '可靠性': '可靠性',
  '传输能力': '传输能力',
  '抗干扰能力': '抗干扰能力',
  '效能影响': '效能影响'
}

// 维度定义
const dimensionTabs = [
  { code: 'security', name: '安全性' },
  { code: 'reliability', name: '可靠性' },
  { code: 'transmission', name: '传输能力' },
  { code: 'anti_jamming', name: '抗干扰能力' },
  { code: 'effect', name: '效能影响' }
]

// 计算属性
const allData = computed(() => aggregationResult.value?.allDataResult || null)
const filteredExtreme = computed(() => aggregationResult.value?.filteredExtremeResult || null)

/** 所有数据方案下的参与专家（含 id、名称） */
const participatingExperts = computed(() => {
  const list = allData.value?.participatingExperts
  if (list && list.length) return list
  const ids = allData.value?.expertIds || []
  return ids.map((id) => ({ expertId: id, expertName: '' }))
})

// 维度表格数据（合并两个数据源）
const dimensionTableData = computed(() => {
  if (!allData.value?.dimensionCvs || !filteredExtreme.value?.dimensionCvs) return []
  const allDims = allData.value.dimensionCvs
  const filteredDims = filteredExtreme.value.dimensionCvs
  return allDims.map(item => {
    const filtered = filteredDims.find(f => f.indicatorCode === item.indicatorCode) || {}
    return {
      indicatorCode: item.indicatorCode,
      indicatorName: item.indicatorName,
      allMean: item.mean,
      filteredMean: filtered.mean || 0,
      allCv: item.cv,
      filteredCv: filtered.cv || 0,
      allLevel: item.consistencyLevel,
      filteredLevel: filtered.consistencyLevel || '-'
    }
  })
})

// 指标表格数据（合并两个数据源）
const indicatorTableData = computed(() => {
  if (!allData.value?.indicatorCvs || !filteredExtreme.value?.indicatorCvs) return []
  const allIndicators = allData.value.indicatorCvs
  const filteredIndicators = filteredExtreme.value.indicatorCvs
  return allIndicators.map(item => {
    const filtered = filteredIndicators.find(f => f.indicatorCode === item.indicatorCode) || {}
    return {
      indicatorCode: item.indicatorCode,
      indicatorName: item.indicatorName,
      dimension: item.dimension,
      allMean: item.mean,
      filteredMean: filtered.mean || 0,
      allCv: item.cv,
      filteredCv: filtered.cv || 0,
      allLevel: item.consistencyLevel,
      filteredLevel: filtered.consistencyLevel || '-'
    }
  })
})

function getIndicatorsByDimension(dimensionCode) {
  const dimensionMap = {
    'security': '安全性',
    'reliability': '可靠性',
    'transmission': '传输能力',
    'anti_jamming': '抗干扰能力',
    'effect': '效能影响'
  }
  return indicatorTableData.value.filter(item => item.dimension === dimensionMap[dimensionCode])
}

// ==================== 矩阵展示计算属性 ====================

// 当前选中的矩阵配置
const currentMatrixConfig = computed(() => matrixConfig[selectedMatrix.value] || matrixConfig.dim)

// 当前矩阵CR值
const currentMatrixCR = computed(() => {
  if (!activeCollectiveWeights.value?.crResults) return 0
  const crMap = activeCollectiveWeights.value.crResults
  if (selectedMatrix.value === 'dim') return Number(crMap['dim']) || 0
  return Number(crMap[matrixConfig[selectedMatrix.value]?.name]) || 0
})

// 当前矩阵表头
const currentMatrixHeaders = computed(() => currentMatrixConfig.value?.headers || [])

// 当前矩阵数据（集体判断矩阵）
const currentMatrixData = computed(() => {
  if (!activeCollectiveWeights.value) return []
  const headers = currentMatrixHeaders.value
  const collectiveScores = activeCollectiveWeights.value.collectiveScores || {}

  // 构建矩阵
  const matrix = []
  for (let i = 0; i < headers.length; i++) {
    const row = { row: headers[i] }
    for (let j = 0; j < headers.length; j++) {
      if (i === j) {
        row[headers[j]] = '1.000'
      } else {
        const key = `${headers[i]}_${headers[j]}`
        const score = collectiveScores[key]
        row[headers[j]] = score ? Number(score).toFixed(3) : '1.000'
      }
    }
    matrix.push(row)
  }
  return matrix
})

// 当前矩阵权重向量
const currentMatrixWeights = computed(() => {
  if (!activeCollectiveWeights.value) return []
  const headers = currentMatrixHeaders.value
  const config = currentMatrixConfig.value

  if (selectedMatrix.value === 'dim') {
    const dimWeights = activeCollectiveWeights.value.dimensionWeights || {}
    return headers.map(h => Number(dimWeights[h] || 0))
  } else {
    const indWeightsByDim = activeCollectiveWeights.value.indicatorWeightsByDimension || {}
    const dimIndWeights = indWeightsByDim[config.name] || {}
    return headers.map(h => Number(dimIndWeights[h] || 0))
  }
})

// 获取矩阵名称
function getMatrixIndicatorName(key) {
  return matrixConfig[key]?.name || ''
}
// ==================== 图表渲染 ====================

function renderScoreChart() {
  if (!scoreChartRef.value || !collectiveResult.value?.results) return

  if (!scoreChart) {
    scoreChart = echarts.init(scoreChartRef.value)
  }

  const results = collectiveResult.value.results
  const option = {
    title: {
      text: '各作战任务综合得分对比',
      left: 'center',
      textStyle: { fontSize: 16 }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const p = params[0]
        return `<strong>${p.name}</strong><br/>
                综合得分: <strong style="color:#67C23A">${Number(p.value).toFixed(2)}</strong>`
      }
    },
    grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      data: results.map(r => `实验${r.operationId}`),
      axisLabel: { fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      name: '综合得分',
      min: 0,
      max: 1,
      axisLabel: { formatter: (v) => Number(v).toFixed(2) }
    },
    series: [{
      type: 'bar',
      data: results.map((r, i) => ({
        value: Number(r.totalScore),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#67C23A' },
            { offset: 1, color: '#95D475' }
          ])
        }
      })),
      label: {
        show: true,
        position: 'top',
        formatter: (p) => Number(p.value).toFixed(2),
        fontSize: 11
      },
      barWidth: '50%'
    }]
  }
  scoreChart.setOption(option)
}

/** 横轴缩放：滑块 + 图内滚轮/拖拽（类目轴） */
function buildCategoryAxisDataZoom() {
  return [
    {
      type: 'inside',
      xAxisIndex: 0,
      zoomOnMouseWheel: true,
      moveOnMouseMove: true,
      moveOnMouseWheel: false
    },
    {
      type: 'slider',
      xAxisIndex: 0,
      height: 22,
      bottom: 4,
      showDetail: false,
      handleStyle: { color: '#409eff' },
      dataBackground: { lineStyle: { opacity: 0.3 }, areaStyle: { opacity: 0.1 } },
      borderColor: '#dcdfe6'
    }
  ]
}

/** 6.2：横轴=18 个二级指标（含二级权重占比）；系列=各实验 */
function paletteForExperimentSeries(n) {
  const base = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc', '#409EFF']
  if (n <= base.length) return base.slice(0, n)
  const out = [...base]
  for (let i = base.length; i < n; i++) {
    const h = Math.round((360 * i) / n) % 360
    out.push(`hsl(${h}, 62%, 52%)`)
  }
  return out
}

function renderDimensionScoreDetailChart() {
  if (!dimensionScoreChartRef.value || !collectiveResult.value?.results?.length) return
  if (!dimensionScoreChart) {
    dimensionScoreChart = echarts.init(dimensionScoreChartRef.value)
  }
  const results = collectiveResult.value.results
  const iw = collectiveResult.value.indicatorWeights || {}
  const weightPctInd = (name) => {
    const v = Number(iw[name] ?? 0)
    return (v * 100).toFixed(1)
  }
  const weightNum = (name) => Number(iw[name] ?? 0)
  const weightedValue = (r, name) => {
    const s = Number(r.indicatorScores?.[name] ?? 0)
    return s * weightNum(name)
  }
  let peak = 0
  for (const r of results) {
    for (const name of INDICATOR_LABELS) {
      peak = Math.max(peak, weightedValue(r, name))
    }
  }
  const yMax = peak > 1e-9 ? Math.min(1, peak * 1.12) : 0.1

  const xCategories = INDICATOR_LABELS.map((n) => `${n}\n权重 ${weightPctInd(n)}%`)

  const chartType = dimensionScoreChartType.value === 'line' ? 'line' : 'bar'
  const colors = paletteForExperimentSeries(results.length)

  const series = results.map((r, idx) => ({
    name: `实验${r.operationId}`,
    type: chartType,
    data: INDICATOR_LABELS.map((name) => weightedValue(r, name)),
    itemStyle: { color: colors[idx] },
    lineStyle: chartType === 'line' ? { width: 2 } : undefined,
    smooth: chartType === 'line',
    symbolSize: chartType === 'line' ? 7 : undefined,
    barMaxWidth: chartType === 'bar' ? 10 : undefined
  }))

  const option = {
    title: {
      text: '各二级指标加权得分 — 按实验对比（得分 × 集结二级权重）',
      left: 'center',
      textStyle: { fontSize: 15 }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter(params) {
        if (!params?.length) return ''
        const dataIndex = params[0].dataIndex
        const indTitle = INDICATOR_LABELS[dataIndex] ?? ''
        const w = weightNum(indTitle)
        let head = `<strong>${indTitle}</strong>`
        if (indTitle) {
          head += `<br/><span style="opacity:.85">集结二级权重：${weightPctInd(indTitle)}%（${w.toFixed(2)}）</span>`
        }
        const lines = params
          .filter((p) => p.value != null && !Number.isNaN(Number(p.value)))
          .map((p) => {
            const weighted = Number(p.value)
            const raw = w > 1e-12 ? weighted / w : 0
            return `${p.marker} ${p.seriesName}: 归一化得分 ${raw.toFixed(2)} × 权重 → <strong>${weighted.toFixed(2)}</strong>`
          })
        return `${head}<br/>${lines.join('<br/>')}`
      }
    },
    legend: {
      type: 'scroll',
      bottom: 36,
      left: 'center',
      data: results.map((r) => `实验${r.operationId}`)
    },
    grid: { left: '3%', right: '4%', top: 58, bottom: 118, containLabel: true },
    dataZoom: buildCategoryAxisDataZoom(),
    xAxis: {
      type: 'category',
      data: xCategories,
      axisLabel: { interval: 0, rotate: 32, fontSize: 9, lineHeight: 12 }
    },
    yAxis: {
      type: 'value',
      name: '加权得分（得分×权重）',
      min: 0,
      max: yMax,
      axisLabel: { formatter: (v) => Number(v).toFixed(2) },
      splitLine: { lineStyle: { type: 'dashed' } }
    },
    series
  }
  dimensionScoreChart.setOption(option, true)
}

/** 6.1：横轴=各实验；系列=五大一级维度得分 */
function renderPrimaryDimensionByExperimentChart() {
  if (!primaryDimByExperimentChartRef.value || !collectiveResult.value?.results?.length) return
  if (!primaryDimByExperimentChart) {
    primaryDimByExperimentChart = echarts.init(primaryDimByExperimentChartRef.value)
  }
  const results = collectiveResult.value.results
  const dimNames = ['安全性', '可靠性', '传输能力', '抗干扰能力', '效能影响']
  const labels = results.map((r) => `实验${r.operationId}`)
  const chartType = primaryDimChartType.value === 'line' ? 'line' : 'bar'
  const palette = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399']
  const series = dimNames.map((d, idx) => ({
    name: d,
    type: chartType,
    data: results.map((r) => Number(r.dimensionScores?.[d] ?? 0)),
    itemStyle: { color: palette[idx] },
    smooth: chartType === 'line',
    symbolSize: chartType === 'line' ? 6 : undefined,
    barMaxWidth: chartType === 'bar' ? 22 : undefined,
    lineStyle: chartType === 'line' ? { width: 2 } : undefined
  }))

  const option = {
    title: {
      text: '各作战任务 — 一级维度得分对比',
      left: 'center',
      textStyle: { fontSize: 15 }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter(params) {
        if (!params?.length) return ''
        const expName = params[0].name
        const lines = params
          .filter((p) => p.value != null && !Number.isNaN(Number(p.value)))
          .map((p) => `${p.marker} ${p.seriesName}: <strong>${Number(p.value).toFixed(2)}</strong>`)
        return `<strong>${expName}</strong><br/>${lines.join('<br/>')}`
      }
    },
    legend: { type: 'scroll', bottom: 36, left: 'center', data: dimNames },
    grid: { left: '3%', right: '4%', top: 55, bottom: 100, containLabel: true },
    dataZoom: buildCategoryAxisDataZoom(),
    xAxis: { type: 'category', data: labels, axisLabel: { rotate: labels.length > 8 ? 25 : 0, fontSize: 11 } },
    yAxis: {
      type: 'value',
      name: '得分(0~1)',
      min: 0,
      max: 1,
      axisLabel: { formatter: (v) => Number(v).toFixed(2) },
      splitLine: { lineStyle: { type: 'dashed' } }
    },
    series
  }
  primaryDimByExperimentChart.setOption(option, true)
}

function disposeCollectiveResultCharts() {
  scoreChart?.dispose()
  scoreChart = null
  dimensionScoreChart?.dispose()
  dimensionScoreChart = null
  primaryDimByExperimentChart?.dispose()
  primaryDimByExperimentChart = null
}

function renderDimWeightChart() {
  const src = collectiveResult.value ?? weightPreview.value
  if (!dimWeightChartRef.value || !src?.dimensionWeights) return

  if (dimWeightChart) {
    const dom = dimWeightChart.getDom()
    if (!dom?.isConnected) {
      dimWeightChart.dispose()
      dimWeightChart = null
    }
  }
  if (!dimWeightChart) {
    dimWeightChart = echarts.init(dimWeightChartRef.value)
  }

  const dimWeights = src.dimensionWeights
  const dims = Object.keys(dimWeights)
  const values = dims.map((d) =>
    Number((Number(dimWeights[d]) || 0) * 100).toFixed(2)
  )

  const option = {
    title: {
      text: '维度层权重分布',
      left: 'center',
      textStyle: { fontSize: 14 }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const p = params[0]
        return `<strong>${p.name}</strong><br/>权重: <strong>${p.value}%</strong>`
      }
    },
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      data: dims,
      axisLabel: { rotate: 15, fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      name: '权重(%)',
      axisLabel: { formatter: '{value}%' }
    },
    series: [{
      type: 'bar',
      data: values.map((v, i) => ({
        value: v,
        itemStyle: {
          color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399'][i % 5]
        }
      })),
      label: {
        show: true,
        position: 'top',
        formatter: (p) => p.value + '%',
        fontSize: 11
      }
    }]
  }
  dimWeightChart.setOption(option, true)
  dimWeightChart.resize()
}

function renderIndWeightChart() {
  const src = collectiveResult.value ?? weightPreview.value
  if (!indWeightChartRef.value || !src?.indicatorWeights) return

  if (indWeightChart) {
    const dom = indWeightChart.getDom()
    if (!dom?.isConnected) {
      indWeightChart.dispose()
      indWeightChart = null
    }
  }
  if (!indWeightChart) {
    indWeightChart = echarts.init(indWeightChartRef.value)
  }

  const indWeights = src.indicatorWeights
  const indicators = Object.keys(indWeights).sort()
  const values = indicators.map((ind) =>
    Number((Number(indWeights[ind]) || 0) * 100).toFixed(2)
  )

  const colors = []
  const colorMap = {
    '安全性': '#409EFF',
    '可靠性': '#67C23A',
    '传输能力': '#E6A23C',
    '抗干扰能力': '#F56C6C',
    '效能影响': '#909399'
  }
  const indDimMap = {
    '密钥泄露得分': '安全性', '被侦察得分': '安全性', '抗拦截得分': '安全性',
    '崩溃比例得分': '可靠性', '恢复能力得分': '可靠性', '通信可用得分': '可靠性',
    '带宽得分': '传输能力', '呼叫建立得分': '传输能力', '传输时延得分': '传输能力',
    '误码率得分': '传输能力', '吞吐量得分': '传输能力', '频谱效率得分': '传输能力',
    '信干噪比得分': '抗干扰能力', '抗干扰余量得分': '抗干扰能力', '通信距离得分': '抗干扰能力',
    '战损率得分': '效能影响', '任务完成率得分': '效能影响', '致盲率得分': '效能影响'
  }

  const option = {
    title: {
      text: '二级指标综合权重分布',
      left: 'center',
      textStyle: { fontSize: 14 }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const p = params[0]
        return `<strong>${p.name}</strong><br/>权重: <strong>${p.value}%</strong>`
      }
    },
    grid: { left: '3%', right: '4%', bottom: '25%', containLabel: true },
    xAxis: {
      type: 'category',
      data: indicators,
      axisLabel: { rotate: 45, fontSize: 10, interval: 0 }
    },
    yAxis: {
      type: 'value',
      name: '权重(%)',
      axisLabel: { formatter: '{value}%' }
    },
    series: [{
      type: 'bar',
      data: values.map((v, i) => ({
        value: v,
        itemStyle: { color: colorMap[indDimMap[indicators[i]]] || '#409EFF' }
      })),
      label: { show: false }
    }]
  }
  indWeightChart.setOption(option, true)
  indWeightChart.resize()
}
// ==================== 集结计算函数 ====================

const dimensionIndicatorMap = {
  '安全性': ['密钥泄露得分', '被侦察得分', '抗拦截得分'],
  '可靠性': ['崩溃比例得分', '恢复能力得分', '通信可用得分'],
  '传输能力': ['带宽得分', '呼叫建立得分', '传输时延得分', '误码率得分', '吞吐量得分', '频谱效率得分'],
  '抗干扰能力': ['信干噪比得分', '抗干扰余量得分', '通信距离得分'],
  '效能影响': ['战损率得分', '任务完成率得分', '致盲率得分']
}

const indicatorWeightTableData = computed(() => {
  if (!activeCollectiveWeights.value?.indicatorWeights) return []
  const result = []
  const iw = activeCollectiveWeights.value.indicatorWeights
  for (const [dim, indicators] of Object.entries(dimensionIndicatorMap)) {
    for (const ind of indicators) {
      const w = iw[ind]
      if (w !== undefined) {
        result.push({ indicator: ind, dimension: dim, weight: w })
      }
    }
  }
  return result
})

async function loadEvaluationIds() {
  try {
    evaluationIds.value = await getCollectiveEvaluationIds()
  } catch (error) {
    console.error('加载评估批次失败:', error)
  }
}

async function loadAvailableExperts() {
  try {
    availableExperts.value = await request.get('/expert/credibility/experts')
  } catch (error) {
    console.error('加载专家列表失败:', error)
  }
}

function onEvaluationIdChange() {
  weightPreview.value = null
  collectiveResult.value = null
  dimWeightChart?.dispose()
  dimWeightChart = null
  indWeightChart?.dispose()
  indWeightChart = null
  loadMetricsForBatch()
}

function onComprehensiveInnerTabChange(tabName) {
  if (tabName === 'aggregate') {
    nextTick(() => {
      renderDimWeightChart()
      renderIndWeightChart()
      dimWeightChart?.resize()
      indWeightChart?.resize()
    })
  }
}

async function previewWeights() {
  if (!collectiveForm.value.evaluationId) {
    ElMessage.warning('请先选择评估批次')
    return
  }
  previewLoading.value = true
  try {
    const params = {
      evaluationId: collectiveForm.value.evaluationId,
      expertIds: collectiveForm.value.useAllExperts ? undefined : collectiveForm.value.expertIds,
      useAllExperts: collectiveForm.value.useAllExperts
    }
    weightPreview.value = await previewCollectiveWeights(params)
    collectiveResult.value = null
    selectedMatrix.value = 'dim'

    ElMessage.success('权重预览已更新')
    nextTick(() => {
      renderDimWeightChart()
      renderIndWeightChart()
    })
  } catch (error) {
    console.error('预览权重失败:', error)
    ElMessage.error(error?.message || '预览权重失败')
  } finally {
    previewLoading.value = false
  }
}

async function executeCalculation() {
  if (!collectiveForm.value.evaluationId) {
    ElMessage.warning('请先选择评估批次')
    return
  }
  calculateLoading.value = true
  try {
    const payload = {
      evaluationId: collectiveForm.value.evaluationId,
      expertIds: collectiveForm.value.useAllExperts ? undefined : collectiveForm.value.expertIds,
      useAllExperts: collectiveForm.value.useAllExperts
    }
    collectiveResult.value = await executeCollectiveCalculation(payload)
    ElMessage.success('集结权重已保存。请在「综合评估得分」中点击「生成综合得分」，或前往「评估结果计算」点击「加载综合结果」。')
    comprehensiveScoreInnerTab.value = 'aggregate'
    nextTick(() => {
      renderDimWeightChart()
      renderIndWeightChart()
    })
  } catch (error) {
    console.error('集结计算失败:', error)
    ElMessage.error(error?.message || '集结计算失败')
  } finally {
    calculateLoading.value = false
  }
}

async function loadComprehensiveScores() {
  if (!collectiveForm.value.evaluationId) {
    ElMessage.warning('请先选择评估批次')
    return
  }
  computeScoresLoading.value = true
  try {
    collectiveResult.value = await computeCollectiveResults(collectiveForm.value.evaluationId)
    ElMessage.success('综合得分已计算并保存')
    await nextTick()
    renderPrimaryDimensionByExperimentChart()
    renderDimensionScoreDetailChart()
    renderScoreChart()
  } catch (e) {
    ElMessage.error(e?.message || '生成综合得分失败')
  } finally {
    computeScoresLoading.value = false
  }
}

async function deleteResults() {
  if (!collectiveForm.value.evaluationId) return
  try {
    await ElMessageBox.confirm('确定要删除该批次的集结结果吗？', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteCollectiveResults(collectiveForm.value.evaluationId)
    collectiveResult.value = null
    disposeCollectiveResultCharts()
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

onMounted(() => {
  loadEvaluationIds()
  loadAvailableExperts()
  window.addEventListener('resize', () => {
    scoreChart?.resize()
    dimWeightChart?.resize()
    indWeightChart?.resize()
    dimensionScoreChart?.resize()
    primaryDimByExperimentChart?.resize()
  })
})
</script>

<style scoped lang="scss">

// ==================== 集结计算样式 ====================
.collective-card {
  margin-bottom: 16px;

  .preview-section {
    margin-bottom: 20px;

    h5 {
      margin: 0 0 12px 0;
      color: #409eff;
      font-size: 14px;
      font-weight: 600;
    }
  }

  .cr-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    padding: 10px 8px;
    background: #f5f7fa;
    border-radius: 6px;

    .cr-label {
      font-size: 12px;
      color: #606266;
    }
  }

  .weight-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    padding: 12px 8px;
    background: #f5f7fa;
    border-radius: 6px;

    .weight-label {
      font-size: 12px;
      color: #606266;
    }

    strong {
      font-size: 16px;
      color: #409eff;
    }
  }
}

.comprehensive-score-outer {
  .comprehensive-score-tabs {
    margin-top: 4px;
  }

  .inner-nested-card {
    margin-bottom: 12px;
  }
}

.scores-tab-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.scores-tab-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #409eff;
}

.scores-tab-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.result-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;

  h5 {
    margin: 0;
    color: #409eff;
    font-size: 14px;
    font-weight: 600;
  }
}

.chart-hint {
  font-size: 12px;
  color: #909399;
  margin: 0 0 10px 0;
  line-height: 1.5;
}

.result-section {
  margin-bottom: 24px;

  h5 {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0 0 15px 0;
    color: #409eff;
    font-size: 14px;
    font-weight: 600;
  }
}

.matrix-selector {
  margin-bottom: 16px;
  text-align: center;
}

.matrix-cr {
  text-align: center;
  margin-bottom: 16px;
}

.matrix-table-container {
  overflow-x: auto;
  margin-bottom: 20px;
}

.matrix-table {
  min-width: 500px;
}

.matrix-weights {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px dashed #ebeef5;

  h5 {
    margin: 0 0 12px 0;
    color: #67C23A;
    font-size: 14px;
  }
}

.weight-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px 8px;
  background: linear-gradient(135deg, #f0f9eb 0%, #e8f5e1 100%);
  border-radius: 8px;
  border: 1px solid #c8e6c9;

  .weight-name {
    font-size: 12px;
    color: #606266;
  }

  strong {
    font-size: 15px;
    color: #67C23A;
  }
}

.weight-chart-section {
  margin-bottom: 24px;

  h5 {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0 0 12px 0;
    color: #409eff;
    font-size: 14px;
    font-weight: 600;
  }
}

.score-high {
  color: #67C23A;
  font-weight: 600;
}

.score-medium {
  color: #E6A23C;
  font-weight: 600;
}

.score-low {
  color: #F56C6C;
  font-weight: 600;
}

.score-cell {
  font-family: 'Consolas', monospace;
}

.card-hint {
  font-size: 12px;
  color: #909399;
  margin: 8px 0 0 0;
  line-height: 1.5;
}

.chart-container-medium {
  width: 100%;
  height: 320px;
}

.metrics-hint {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin: 0 0 12px 0;
}

.metrics-hint code {
  font-size: 12px;
  padding: 1px 6px;
  background: #f4f4f5;
  border-radius: 4px;
}

.metrics-legend-collapse {
  margin-top: 14px;
}

.metrics-raw-table {
  width: 100%;
}

.code-field {
  font-size: 12px;
}
</style>
