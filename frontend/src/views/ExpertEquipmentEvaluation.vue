<template>
  <div class="ql-evaluation">
    <el-card class="page-card">
      <template #header>
        <div class="card-header">
          <el-icon class="header-icon"><Guide /></el-icon>
          <span>装备操作定性评估</span>
        </div>
      </template>

      <!-- 顶部筛选条件 -->
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="评估批次" required>
          <el-select v-model="filterForm.batchId" placeholder="请选择批次（与指标计算一致）" clearable filterable style="width: 360px" @change="handleBatchChange">
            <el-option v-for="b in batches" :key="b.evaluation_batch_id" :label="formatBatchLabel(b)" :value="b.evaluation_batch_id" />
          </el-select>
        </el-form-item>
        <el-form-item label="作战ID">
          <el-select v-model="filterForm.operationId" placeholder="请选择作战" clearable filterable style="width: 220px" @change="handleOperationChange">
            <el-option v-if="filterForm.batchId" :label="`全部作战（本批次 ${availableOperations.length} 个）`" :value="ALL_OPERATIONS" />
            <el-option v-for="op in availableOperations" :key="op.operation_id" :label="`作战 ${op.operation_id}`" :value="String(op.operation_id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="专家" required>
          <el-select v-model="filterForm.expertId" placeholder="请选择专家（含可信度）" filterable style="width: 220px" @change="handleExpertChange">
            <el-option
              v-for="e in experts"
              :key="e.expertId"
              :label="`${e.expertName}`"
              :value="e.expertId"
            >
              <span style="float:left">{{ e.expertName }}</span>
              <span style="float:right;color:#909399;font-size:12px;">
                可信度 {{ Number(e.credibilityScore ?? 0).toFixed(2) }}
                <el-tag size="small" type="info" style="margin-left:4px">{{ e.credibilityLevel || '—' }}</el-tag>
              </span>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>

      <!-- 专家定性数据模拟（覆盖当前批次·当前作战下各专家记录） -->
      <div class="section-block simulate-block">
        <div class="section-header">
          <span class="section-title">专家定性 · 数据模拟</span>
          <span class="section-hint">
            依据等级分数区间采样：平均范围控制整体水平，离散度控制专家间差异；将覆盖已有评分。
            <template v-if="isAllOperationsSelected()">当前为「全部作战」，将对批次内每个作战各生成全部专家的定性数据。</template>
          </span>
        </div>
        <div class="simulate-body">
          <el-form label-width="100px" class="simulate-global-form">
            <el-form-item label="平均范围">
              <div class="range-slider-wrap">
                <el-slider
                  v-model="simGlobalRange"
                  range
                  :min="0"
                  :max="100"
                  :step="1"
                  :marks="simRangeMarks"
                />
                <span class="range-label">{{ simGlobalRange[0] }} ~ {{ simGlobalRange[1] }} 分（意向区间中点分布带）</span>
              </div>
            </el-form-item>
            <el-form-item label="离散度">
              <el-slider v-model="simGlobal.dispersion" :min="0" :max="100" :step="5" show-input />
              <span class="field-hint">越高则各专家抽样噪声越大（与后端公式一致）</span>
            </el-form-item>
            <el-form-item label="预计范围">
              <div class="expected-range-box">
                <span class="expected-range-main">
                  预计抽样意向分（约 95%，映射等级前）：
                  <strong>{{ simGlobalExpected.lo95.toFixed(1) }} ~ {{ simGlobalExpected.hi95.toFixed(1) }}</strong> 分
                </span>
                <span class="expected-range-sub">
                  中心 {{ simGlobalExpected.mean.toFixed(1) }} · 噪声 σ {{ simGlobalExpected.sigma.toFixed(2) }}（与生成逻辑一致）
                </span>
                <p class="expected-range-note">
                  说明：生成时先在该分值带附近做高斯抽样，再映射为等级；等级仅有 15 档且综合得分用档内中点，故结果表里常见相邻档（如 B / B+），与上方「意向分」范围不完全等同。
                </p>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="warning" :icon="MagicStick" :loading="simLoading" :disabled="!filterForm.batchId || !filterForm.operationId" @click="handleSimulateBatch">
                生成模拟数据（全部专家·覆盖已有）
              </el-button>
              <el-button size="small" @click="syncSimPerIndicatorFromGlobal">高级表与默认对齐</el-button>
            </el-form-item>
          </el-form>

          <el-collapse v-model="simCollapse" class="simulate-advanced">
            <el-collapse-item title="高级：按单个指标调整平均范围与离散度" name="adv">
              <el-table :data="qlIndicators" border size="small" max-height="280">
                <el-table-column prop="indicator_name" label="指标" min-width="140" show-overflow-tooltip />
                <el-table-column label="分数下限" width="120" align="center">
                  <template #default="{ row }">
                    <el-input-number
                      v-if="simPerIndicator[row.indicator_key]"
                      v-model="simPerIndicator[row.indicator_key].scoreLow"
                      :min="0"
                      :max="100"
                      size="small"
                      controls-position="right"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="分数上限" width="120" align="center">
                  <template #default="{ row }">
                    <el-input-number
                      v-if="simPerIndicator[row.indicator_key]"
                      v-model="simPerIndicator[row.indicator_key].scoreHigh"
                      :min="0"
                      :max="100"
                      size="small"
                      controls-position="right"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="离散度" width="120" align="center">
                  <template #default="{ row }">
                    <el-input-number
                      v-if="simPerIndicator[row.indicator_key]"
                      v-model="simPerIndicator[row.indicator_key].dispersion"
                      :min="0"
                      :max="100"
                      size="small"
                      controls-position="right"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="预计意向分(约95%)" min-width="168" align="center">
                  <template #default="{ row }">
                    <span v-if="simPerIndicator[row.indicator_key]" class="expected-cell">
                      {{ formatSimExpectedRangeForRow(row) }}
                    </span>
                    <span v-else>—</span>
                  </template>
                </el-table-column>
              </el-table>
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>

      <!-- 15 档评分等级标准 + 单项把握度参照（上下排列） -->
      <div class="section-block grade-standard-block">
        <div class="standard-subsection">
          <div class="section-header standard-subheader">
            <span class="section-title">定性评分等级标准</span>
            <span class="section-hint">打分时以「分数区间」为准；系统按区间内点参与综合分计算</span>
          </div>
          <el-table :data="gradeStandardRows" border stripe size="small" max-height="420" class="grade-standard-table">
            <el-table-column prop="code" label="等级代号" width="88" align="center" fixed>
              <template #default="{ row }">
                <el-tag size="small" :type="gradeTagTypeForCode(row.code)">{{ row.code }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="等级名称" width="72" align="center" />
            <el-table-column prop="category" label="大类" width="72" align="center" />
            <el-table-column prop="range" label="分数区间" width="120" align="center" />
            <el-table-column prop="level" label="序" width="48" align="center" />
            <el-table-column prop="description" label="等级说明" min-width="260" show-overflow-tooltip />
          </el-table>
        </div>
        <div class="standard-subsection">
          <div class="section-header standard-subheader">
            <span class="section-title">单项指标把握度标准</span>
            <span class="section-hint">每项指标单独填写 0～100%（百分数），与 AHP 矩阵把握度、判断可信度 λ 含义一致，供对照</span>
          </div>
          <el-table :data="confidenceStandardRows" border stripe size="small" class="confidence-standard-table">
            <el-table-column prop="level" label="把握度等级" width="88" align="center">
              <template #default="{ row }">
                <el-tag size="small" type="info">{{ row.level }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="percentRange" label="建议百分区间" width="120" align="center" />
            <el-table-column prop="lambda" label="参照 λ" width="88" align="center" />
            <el-table-column prop="meaning" label="含义" width="100" align="center" />
            <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
          </el-table>
        </div>
      </div>

      <!-- 定性指标评分区 -->
      <div class="section-block scoring-section">
        <div class="section-header">
          <span class="section-title">专家评分</span>
          <span class="section-hint">（请为每项指标选择等级，选中后把握度自动置为80，可自行调整）</span>
        </div>

        <div class="scoring-grid">
          <div v-for="ind in qlIndicators" :key="ind.indicator_key" class="indicator-card">
            <div class="indicator-card-header">
              <span class="indicator-name">{{ ind.indicator_name }}</span>
              <el-tag size="small" type="info">{{ ind.dimension }}</el-tag>
              <span class="required-star">*</span>
            </div>
            <div class="indicator-card-body">
              <p class="indicator-desc">{{ ind.description }}</p>
              <div v-if="ind.scoring_help" class="scoring-help">
                <strong>评分指引：</strong>{{ ind.scoring_help }}
              </div>
              <div class="scoring-row">
                <div class="score-item">
                  <label class="score-label">选择等级</label>
                  <el-select v-model="qlForm.scores[ind.indicator_key].gradeCode" placeholder="请选择等级" clearable filterable>
                    <el-option
                      v-for="opt in gradeSelectOptions"
                      :key="opt.value"
                      :label="`${opt.value}　区间 ${opt.range}`"
                      :value="opt.value"
                    />
                  </el-select>
                </div>
                <div class="score-item">
                  <label class="score-label">把握度(%)</label>
                  <el-input-number v-model="qlForm.scores[ind.indicator_key].confidence" :min="0" :max="100" :step="5" controls-position="right" />
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="submit-area">
          <el-button type="primary" size="large" @click="handleQlSubmit" :loading="qlSubmitting" :icon="VideoPlay">
            {{ hasExistingRecord ? '更新评分' : '提交评分' }}
          </el-button>
          <el-button v-if="hasExistingRecord" size="large" @click="initQlForm" :icon="Refresh">
            重置表单
          </el-button>
        </div>
      </div>

      <!-- 评分结果展示 -->
      <div class="section-block results-section">
        <div class="section-header">
          <span class="section-title">评分结果</span>
          <el-button type="primary" size="small" @click="loadQlRecords" :icon="Refresh">刷新</el-button>
        </div>
        <el-table :data="qlRecords" border stripe size="small" max-height="400" v-loading="qlRecordsLoading">
          <el-table-column prop="expert_name" label="专家" width="120" align="center" fixed />
          <el-table-column prop="operation_id" label="作战ID" width="100" align="center">
            <template #default="{ row }">
              <el-tag type="primary" size="small">{{ row.operation_id }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column v-for="ind in qlIndicators" :key="ind.indicator_key" :label="ind.indicator_name" min-width="108" align="center">
            <template #default="{ row }">
              <div class="metric-cell">
                <div :class="['metric-grade', getGradeClass(row.scores?.[ind.indicator_key]?.gradeCode)]">
                  {{ row.scores?.[ind.indicator_key]?.gradeCode || '—' }}
                </div>
                <div class="metric-confidence">
                  {{ formatIndicatorConfidence(row.scores?.[ind.indicator_key]?.confidence) }}
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="submitted_at" label="提交时间" width="160" align="center">
            <template #default="{ row }">{{ formatDate(row.submitted_at) }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="qlRecords.length === 0 && !qlRecordsLoading" description="暂无评分记录" />
      </div>

      <!-- ==================== 专家定性指标的集结 ==================== -->
      <div class="section-block ql-agg-section">
        <div class="section-header">
          <span class="section-title">专家定性指标的集结</span>
          <span class="section-hint">γ = w_α·(α/100) + w_λ·λ（λ 为各人把握度÷100）；提交计算时自动将 w_α、w_λ 归一化使 <strong>w_α+w_λ=1</strong>。质心 x* 按文献式 4-21。</span>
        </div>

        <!-- 权重调节 & 刷新 -->
        <div class="agg-control-bar">
          <el-form :inline="true" label-width="120px">
            <el-form-item label="w_α (权威度)">
              <el-input-number v-model="aggWeights.wAlpha" :min="0" :max="10" :step="0.05" :precision="2" style="width: 110px" />
              <span class="agg-weight-hint">与 w_λ 为比例系数，计算时归一化为 1</span>
            </el-form-item>
            <el-form-item label="w_λ (把握度)">
              <el-input-number v-model="aggWeights.wLambda" :min="0" :max="10" :step="0.05" :precision="2" style="width: 110px" />
              <span class="agg-weight-hint">与 w_α 为比例系数，计算时归一化为 1</span>
            </el-form-item>
            <el-form-item label="">
              <el-button type="primary" :icon="Refresh" :loading="aggLoading" @click="loadQualitativeAggregation(false)">
                计算集结结果
              </el-button>
              <el-button type="success" :icon="Refresh" :loading="aggSaving" :disabled="!aggResult || !aggResult.indicators" @click="saveAggregationResult">
                保存集结结果
              </el-button>
            </el-form-item>
          </el-form>
          <div v-if="aggResult?.weights" class="agg-weights-effective">
            本次计算使用（已归一）：w_α = {{ Number(aggResult.weights.wAlpha).toFixed(4) }}，w_λ = {{ Number(aggResult.weights.wLambda).toFixed(4) }}
            <template v-if="aggResult.weightsInput">
              <span class="agg-weights-raw">（输入比例 {{ Number(aggResult.weightsInput.wAlpha).toFixed(2) }} : {{ Number(aggResult.weightsInput.wLambda).toFixed(2) }}）</span>
            </template>
          </div>
          <div v-if="aggResult?.warnings?.length > 0" class="agg-warnings">
            <el-alert
              v-for="(w, wi) in aggResult.warnings"
              :key="wi"
              :title="w"
              type="warning"
              :closable="false"
              show-icon
              style="margin-bottom: 4px; padding: 6px 12px;"
            />
          </div>
        </div>

        <!-- 批量模式作战切换 -->
        <div v-if="aggResult?.byOperation?.length > 0" class="agg-op-tabs">
          <el-radio-group v-model="aggActiveOp" size="small">
            <el-radio-button
              v-for="op in aggResult.byOperation"
              :key="op.operationId"
              :value="op.operationId"
            >
              作战 {{ op.operationId }}
              <el-badge :value="op.indicators?.length || 0" type="info" style="margin-left: 4px;" />
            </el-radio-button>
          </el-radio-group>
        </div>

        <!-- 集结结果主体 -->
        <div v-if="currentIndicators.length > 0" class="agg-body agg-body-full">
          <!-- 群体结论质心主表 -->
          <div class="agg-centroid-table">
            <div class="agg-sub-title">群体结论质心 x*（加权平均分）</div>
            <el-table :data="currentIndicators" border stripe size="small" max-height="320" class="agg-main-table" style="width: 100%">
              <el-table-column prop="indicatorName" label="指标" min-width="160" fixed />
              <el-table-column label="质心分 x*" min-width="120" align="center">
                <template #default="{ row }">
                  <span v-if="row.xStar != null" class="xstar-val">
                    {{ Number(row.xStar).toFixed(2) }}
                  </span>
                  <span v-else class="xstar-null">—</span>
                </template>
              </el-table-column>
              <el-table-column label="映射等级" min-width="100" align="center">
                <template #default="{ row }">
                  <el-tag
                    v-if="row.mappedGrade"
                    :type="gradeTagTypeByScore(row.xStar)"
                    size="small"
                    effect="dark"
                  >{{ row.mappedGrade }}</el-tag>
                  <span v-else>—</span>
                </template>
              </el-table-column>
              <el-table-column label="等级区间" min-width="100" align="center">
                <template #default="{ row }">
                  {{ gradeRangeByCode(row.mappedGrade) }}
                </template>
              </el-table-column>
              <el-table-column prop="denominator" label="分母 Σγ·L" min-width="120" align="center">
                <template #default="{ row }">
                  {{ row.denominator?.toFixed(4) ?? '—' }}
                </template>
              </el-table-column>
              <el-table-column prop="expertCount" label="参与专家" min-width="96" align="center">
                <template #default="{ row }">
                  <el-tag type="info" size="small">{{ row.expertCount }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" min-width="128" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button
                    size="small"
                    :type="aggDetailExpanded[row.indicatorKey] ? 'primary' : 'default'"
                    @click="toggleDetail(row.indicatorKey)"
                  >明细</el-button>
                  <el-button
                    size="small"
                    :type="aggDetailExpanded['__chart_' + row.indicatorKey] ? 'primary' : 'default'"
                    @click="toggleChart(row.indicatorKey)"
                  >质心图</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 明细展开（各专家中间量） -->
          <template v-for="row in currentIndicators" :key="'detail-' + row.indicatorKey">
            <div v-if="aggDetailExpanded[row.indicatorKey]" class="agg-detail-block">
              <div class="agg-sub-title agg-sub-title-sm agg-detail-head">
                <span class="agg-detail-head-title">计算明细 — {{ row.indicatorName }}</span>
                <span class="agg-formula-hint">
                  γ = w_α·(α/100) + w_λ·λ（λ∈[0,1]），x* = Σγ·L·中点 / Σγ·L
                </span>
              </div>
              <el-table :data="row.details" class="agg-detail-table" border stripe size="small" max-height="320" style="width: 100%">
                <el-table-column prop="expertName" label="专家" min-width="96" align="center" show-overflow-tooltip />
                <el-table-column label="α 权威度" min-width="88" align="center">
                  <template #default="{ row: dr }">{{ Number(dr.alphaRaw).toFixed(1) }}</template>
                </el-table-column>
                <el-table-column label="α_norm" min-width="92" align="center">
                  <template #default="{ row: dr }">{{ dr.alphaNorm?.toFixed(4) }}</template>
                </el-table-column>
                <el-table-column label="λ 把握度(%)" min-width="100" align="center">
                  <template #default="{ row: dr }">{{ Number(dr.lambdaRaw).toFixed(0) }}%</template>
                </el-table-column>
                <el-table-column label="λ (0～1)" min-width="92" align="center">
                  <template #default="{ row: dr }">{{ (dr.lambda01 != null ? dr.lambda01 : dr.lambdaNorm)?.toFixed(4) }}</template>
                </el-table-column>
                <el-table-column label="γ 综合可信度" min-width="112" align="center">
                  <template #default="{ row: dr }">
                    <strong style="color:#1a3a5c;">{{ dr.gamma?.toFixed(4) }}</strong>
                  </template>
                </el-table-column>
                <el-table-column label="等级" min-width="76" align="center">
                  <template #default="{ row: dr }">
                    <el-tag size="small" :type="gradeTagTypeForCode(dr.gradeCode)">{{ dr.gradeCode }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="[a₁, a₂]" min-width="108" align="center">
                  <template #default="{ row: dr }">
                    [{{ Number(dr.a1).toFixed(1) }}, {{ Number(dr.a2).toFixed(1) }}]
                  </template>
                </el-table-column>
                <el-table-column label="中点" min-width="80" align="center">
                  <template #default="{ row: dr }">{{ Number(dr.midpoint).toFixed(2) }}</template>
                </el-table-column>
                <el-table-column label="γ·L·中点" min-width="120" align="center">
                  <template #default="{ row: dr }">{{ dr.weightedMidpoint?.toFixed(4) }}</template>
                </el-table-column>
              </el-table>
            </div>
          </template>

          <!-- 质心图展开 -->
          <template v-for="row in currentIndicators" :key="'chart-' + row.indicatorKey">
            <div v-if="aggDetailExpanded['__chart_' + row.indicatorKey]" class="agg-chart-block">
              <div class="agg-sub-title agg-sub-title-sm">
                加权覆盖函数 — {{ row.indicatorName }}
                <span class="agg-formula-hint">P̄(x)=Σ γ<sub>k</sub>·p<sub>k</sub>(x)，γ=w<sub>α</sub>·(α/100)+w<sub>λ</sub>·λ（λ 为把握度÷100，不归一）</span>
              </div>
              <div :id="'ql-agg-chart-px-' + row.indicatorKey" class="agg-chart-px" />
              <div v-if="row.details?.length" class="agg-gamma-strip">
                <span class="agg-gamma-strip-title">各专家综合可信度 γ：</span>
                <el-tag
                  v-for="(dr, gi) in row.details"
                  :key="gi"
                  type="info"
                  effect="plain"
                  class="agg-gamma-tag"
                >
                  {{ dr.expertName || ('专家' + (gi + 1)) }}　γ={{ dr.gamma != null ? Number(dr.gamma).toFixed(4) : '—' }}
                </el-tag>
              </div>
            </div>
          </template>
        </div>

        <!-- 无数据时 -->
        <el-empty v-if="!aggResult || currentIndicators.length === 0" description="请选择评估批次后点击「计算集结结果」（可选作战；未选时将依据上方评分结果表中的作战ID自动集结）" :image-size="80" />
      </div>

    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Guide, Refresh, VideoPlay, MagicStick } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  getQlIndicators,
  getQlBatches,
  getQlRecords,
  submitQlEvaluation,
  getExpertsForEvaluation,
  getAvailableOperations,
  getQlOperationsForBatch,
  getQlRecordForEdit,
  simulateQlBatch,
  getQlQualitativeAggregation,
  getStoredQlAggregationResult
} from '@/api'

/** 与 evaluation_grade_definition 一致的 15 档（展示以分数区间为准，与库表 score_range 一致） */
const gradeStandardRows = Object.freeze([
  { code: 'A+', name: '优+', category: '优', range: '[95,100]', level: 1, description: '优秀+：表现极其出色，超出预期' },
  { code: 'A', name: '优', category: '优', range: '[90,95)', level: 2, description: '优秀：表现出色，达到优秀标准' },
  { code: 'A-', name: '优-', category: '优', range: '[85,90)', level: 3, description: '优秀-：表现良好，接近优秀标准' },
  { code: 'B+', name: '良+', category: '良', range: '[80,85)', level: 4, description: '良好+：表现较好，超出良好标准' },
  { code: 'B', name: '良', category: '良', range: '[75,80)', level: 5, description: '良好：表现良好，达到良好标准' },
  { code: 'B-', name: '良-', category: '良', range: '[70,75)', level: 6, description: '良好-：表现尚可，接近良好标准' },
  { code: 'C+', name: '合格+', category: '合格', range: '[65,70)', level: 7, description: '合格+：基本达标，略高于合格线' },
  { code: 'C', name: '合格', category: '合格', range: '[60,65)', level: 8, description: '合格：达到基本要求，刚好合格' },
  { code: 'C-', name: '合格-', category: '合格', range: '[55,60)', level: 9, description: '合格-：勉强合格，需要改进' },
  { code: 'D+', name: '差+', category: '差', range: '[47,55)', level: 10, description: '差+：表现较差，明显低于标准' },
  { code: 'D', name: '差', category: '差', range: '[39,47)', level: 11, description: '差：表现差，远低于标准' },
  { code: 'D-', name: '差-', category: '差', range: '[30,39)', level: 12, description: '差-：表现很差，严重不达标' },
  { code: 'E+', name: '极差+', category: '极差', range: '[20,30)', level: 13, description: '极差+：表现极差，几乎无法接受' },
  { code: 'E', name: '极差', category: '极差', range: '[10,20)', level: 14, description: '极差：表现极其糟糕，完全不可接受' },
  { code: 'E-', name: '极差-', category: '极差', range: '[0,10)', level: 15, description: '极差-：表现最差，毫无价值' }
])

/**
 * 与 AHP 页「把握度等级与判断可信度 λ」一致；百分区间为在本页填写 0～100 时的对照建议（非强制分区）。
 */
const confidenceStandardRows = Object.freeze([
  {
    level: '1 级',
    percentRange: '[80, 100]',
    lambda: '1',
    meaning: '完全确认',
    description:
      '对指标非常熟悉、依据充分，判断完全可信；本页选等级后默认把握度 80% 即落在该档。'
  },
  {
    level: '2 级',
    percentRange: '[60, 80)',
    lambda: '0.8',
    meaning: '确认',
    description: '对指标较熟悉，判断可信；与矩阵把握度 λ≈0.8 相当。'
  },
  {
    level: '3 级',
    percentRange: '[40, 60)',
    lambda: '0.6',
    meaning: '基本确认',
    description: '了解有限，判断有一定可信度但把握一般；与 λ≈0.6 相当。'
  },
  {
    level: '4 级',
    percentRange: '[0, 40)',
    lambda: '< 0.6',
    meaning: '不能确认',
    description: '依据不足或指标不熟，不宜高报把握度；建议补充材料或酌情降低百分值。'
  }
])

const gradeSelectOptions = computed(() =>
  gradeStandardRows.map((r) => ({
    value: r.code,
    range: r.range
  }))
)

/** 与后端 allOperations / operationId ALL 对应 */
const ALL_OPERATIONS = '__ALL__'

const filterForm = reactive({
  batchId: null,
  operationId: null,
  expertId: null
})

function isAllOperationsSelected() {
  return filterForm.operationId === ALL_OPERATIONS
}

// 数据
const batches = ref([])
const qlIndicators = ref([])
const qlRecords = ref([])
const experts = ref([])
const availableOperations = ref([])

// 状态
const qlSubmitting = ref(false)
const qlRecordsLoading = ref(false)
const hasExistingRecord = ref(false)

// ==================== 定性指标集结 ====================

/** 集结权重（前端可调，实时反映到后端） */
const aggWeights = reactive({ wAlpha: 0.5, wLambda: 0.5 })

/** 集结结果：单作战时 indicators[]；多作战时 byOperation[] */
const aggResult = ref(null)
/** 当前选中展开的作战 ID（byOperation 模式下） */
const aggActiveOp = ref(null)
/** 明细展开状态：Map<indicatorKey, Boolean> */
const aggDetailExpanded = ref({})
/** 质心图 DOM ref（每个指标一个） */
const aggCharts = ref({})
const aggChartReady = ref({})
/** 集结加载中 */
const aggLoading = ref(false)
const aggSaving = ref(false)
const aggCollapse = ref(['detail', 'chart'])

/** 模拟：全局平均范围 [低, 高]、离散度；按指标可单独覆盖 */
const simGlobalRange = ref([72, 86])
const simGlobal = reactive({ dispersion: 40 })
const simPerIndicator = reactive({})
const simCollapse = ref([])
const simLoading = ref(false)
const simRangeMarks = { 0: '0', 50: '50', 100: '100' }

function clampNum(x, min, max) {
  return Math.min(max, Math.max(min, x))
}

/** 与后端 EquipmentQlEvaluationService.simulateQlBatch 中噪声公式一致 */
function calcSimSigma(low, high, dispersion) {
  const lo = Number(low)
  const hi = Number(high)
  const a = Math.min(lo, hi)
  const b = Math.max(lo, hi)
  const span = Math.max(b - a, 1e-6)
  const d = clampNum(Number(dispersion), 0, 100)
  return Math.max(0.8, span / 4) * (0.15 + 0.85 * (d / 100))
}

/** 意向分高斯抽样约 95% 落在 mean±2σ（映射为等级前） */
function calcExpectedIntentRange95(low, high, dispersion) {
  const lo = Number(low)
  const hi = Number(high)
  const a = Math.min(lo, hi)
  const b = Math.max(lo, hi)
  const mean = (a + b) / 2
  const sigma = calcSimSigma(a, b, dispersion)
  const lo95 = clampNum(mean - 2 * sigma, 0, 100)
  const hi95 = clampNum(mean + 2 * sigma, 0, 100)
  return { mean, sigma, lo95, hi95 }
}

const simGlobalExpected = computed(() => {
  const [r0, r1] = simGlobalRange.value
  return calcExpectedIntentRange95(r0, r1, simGlobal.dispersion)
})

function formatSimExpectedRangeForRow(row) {
  const r = simPerIndicator[row.indicator_key]
  if (!r) return '—'
  const { lo95, hi95, sigma } = calcExpectedIntentRange95(r.scoreLow, r.scoreHigh, r.dispersion)
  return `${lo95.toFixed(1)} ~ ${hi95.toFixed(1)} (σ${sigma.toFixed(2)})`
}

function ensureSimPerIndicatorRows() {
  const [lo, hi] = simGlobalRange.value
  qlIndicators.value.forEach((ind) => {
    const k = ind.indicator_key
    if (!simPerIndicator[k]) {
      simPerIndicator[k] = { scoreLow: lo, scoreHigh: hi, dispersion: simGlobal.dispersion }
    }
  })
}

function syncSimPerIndicatorFromGlobal() {
  const [lo, hi] = simGlobalRange.value
  qlIndicators.value.forEach((ind) => {
    const k = ind.indicator_key
    if (!simPerIndicator[k]) {
      simPerIndicator[k] = {}
    }
    simPerIndicator[k].scoreLow = lo
    simPerIndicator[k].scoreHigh = hi
    simPerIndicator[k].dispersion = simGlobal.dispersion
  })
}

/**
 * 全局滑块驱动「预计范围」展示，但提交模拟用的是 simPerIndicator。
 * 若只初始化过一次（默认 72~86）后不再同步，会出现界面显示 93~100 而实际仍按 72~86 抽样。
 */
watch(
  () => [simGlobalRange.value[0], simGlobalRange.value[1], simGlobal.dispersion],
  () => {
    syncSimPerIndicatorFromGlobal()
  }
)

async function handleSimulateBatch() {
  if (!filterForm.batchId) {
    ElMessage.warning('请先选择评估批次')
    return
  }
  if (!filterForm.operationId) {
    ElMessage.warning('请先选择作战ID，或选择「全部作战」')
    return
  }
  if (isAllOperationsSelected() && availableOperations.value.length === 0) {
    ElMessage.warning('当前批次下没有可选作战，无法执行全部作战模拟')
    return
  }
  const confirmText = isAllOperationsSelected()
    ? `将为当前批次下全部 ${availableOperations.value.length} 个作战分别生成：每个作战下「全部已完成可信度评估的专家」的定性评分并写入数据库，已存在的记录将被覆盖。是否继续？`
    : '将为当前批次、当前作战下「全部已完成可信度评估的专家」重新生成定性评分并写入数据库，已存在的记录将被覆盖。是否继续？'
  try {
    await ElMessageBox.confirm(confirmText, '确认模拟', {
      type: 'warning',
      confirmButtonText: '覆盖生成',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  ensureSimPerIndicatorRows()
  const [low, high] = simGlobalRange.value
  const indicatorParams = {}
  qlIndicators.value.forEach((ind) => {
    const k = ind.indicator_key
    const r = simPerIndicator[k] || { scoreLow: low, scoreHigh: high, dispersion: simGlobal.dispersion }
    let sl = Number(r.scoreLow)
    let sh = Number(r.scoreHigh)
    if (sl > sh) {
      const t = sl
      sl = sh
      sh = t
    }
    indicatorParams[k] = {
      scoreLow: sl,
      scoreHigh: sh,
      dispersion: Number(r.dispersion)
    }
  })
  simLoading.value = true
  try {
    const payload = {
      evaluationBatchId: filterForm.batchId,
      defaultScoreLow: low,
      defaultScoreHigh: high,
      defaultDispersion: simGlobal.dispersion,
      indicatorParams
    }
    if (isAllOperationsSelected()) {
      payload.allOperations = true
    } else {
      payload.operationId = Number(filterForm.operationId)
    }
    const res = await simulateQlBatch(payload)
    ElMessage.success(res?.message || '模拟完成')
    await loadQlRecords()
    if (filterForm.expertId) {
      await loadExistingRecord()
    }
  } catch (e) {
    ElMessage.error(e?.message || '模拟失败')
  } finally {
    simLoading.value = false
  }
}

// 定性评分表单
const qlForm = reactive({
  scores: {}
})

// 初始化定性表单结构
function initQlForm() {
  const scores = {}
  qlIndicators.value.forEach(ind => {
    scores[ind.indicator_key] = {
      gradeCode: null,
      confidence: 80
    }
  })
  qlForm.scores = scores
  hasExistingRecord.value = false
}

// 加载数据
async function loadQlIndicators() {
  try {
    const res = await getQlIndicators()
    qlIndicators.value = Array.isArray(res) ? res : []
    initQlForm()
    ensureSimPerIndicatorRows()
  } catch (e) {
    console.error('加载定性指标失败', e)
  }
}

async function loadQlBatches() {
  try {
    const res = await getQlBatches()
    batches.value = Array.isArray(res) ? res : []
  } catch (e) {
    console.error('加载批次失败', e)
  }
}

async function loadQlRecords() {
  if (!filterForm.batchId) {
    qlRecords.value = []
    return
  }
  qlRecordsLoading.value = true
  try {
    const opFilter = isAllOperationsSelected() ? null : filterForm.operationId || null
    const res = await getQlRecords(filterForm.batchId, opFilter)
    qlRecords.value = Array.isArray(res) ? res : []
  } catch (e) {
    console.error('加载定性记录失败', e)
  } finally {
    qlRecordsLoading.value = false
  }
}

async function loadExperts() {
  try {
    const res = await getExpertsForEvaluation()
    experts.value = Array.isArray(res) ? res : []
  } catch (e) {
    console.error('加载专家失败', e)
  }
}

async function loadOperations() {
  try {
    const res = filterForm.batchId
      ? await getQlOperationsForBatch(filterForm.batchId)
      : await getAvailableOperations()
    availableOperations.value = Array.isArray(res) ? res : []
  } catch (e) {
    console.error('加载作战失败', e)
  }
}

// 操作
async function handleOperationChange() {
  filterForm.expertId = null
  loadQlRecords()
  initQlForm()
  if (filterForm.batchId && filterForm.operationId && filterForm.expertId && !isAllOperationsSelected()) {
    await loadExistingRecord()
  }
}

async function handleExpertChange() {
  initQlForm()
  if (filterForm.batchId && filterForm.operationId && filterForm.expertId && !isAllOperationsSelected()) {
    await loadExistingRecord()
  }
}

async function loadExistingRecord() {
  if (!filterForm.batchId || !filterForm.operationId || !filterForm.expertId || isAllOperationsSelected()) return
  try {
    const res = await getQlRecordForEdit(filterForm.batchId, filterForm.operationId, filterForm.expertId)
    if (res && res.scores && typeof res.scores === 'object') {
      hasExistingRecord.value = true
      Object.keys(res.scores).forEach(key => {
        if (qlForm.scores[key]) {
          const s = res.scores[key]
          qlForm.scores[key].gradeCode = s.gradeCode != null ? String(s.gradeCode) : null
          const c = s.confidence
          qlForm.scores[key].confidence = c != null ? Number(c) : 80
        }
      })
      ElMessage.info('已载入该专家在本批次、本作战下的评分，修改后提交即为更新')
    } else {
      hasExistingRecord.value = false
    }
  } catch (e) {
    hasExistingRecord.value = false
  }
}

async function handleQlSubmit() {
  if (!filterForm.batchId) {
    ElMessage.warning('请选择评估批次（与「指标计算」生成的批次一致）')
    return
  }
  if (isAllOperationsSelected()) {
    ElMessage.warning('「全部作战」仅用于模拟与结果总览，提交专家评分请先选择具体作战ID')
    return
  }
  if (!filterForm.operationId) {
    ElMessage.warning('请选择作战ID')
    return
  }
  if (!filterForm.expertId) {
    ElMessage.warning('请选择专家')
    return
  }
  // 全量非空校验：每个启用指标必须有 gradeCode
  const missingIndicators = []
  qlIndicators.value.forEach(ind => {
    const score = qlForm.scores[ind.indicator_key]
    if (!score || !score.gradeCode) {
      missingIndicators.push(ind.indicator_name)
    }
  })
  if (missingIndicators.length > 0) {
    ElMessage.warning('以下指标未打分: ' + missingIndicators.join('、'))
    return
  }

  qlSubmitting.value = true
  try {
    const payload = {
      evaluationBatchId: filterForm.batchId || null,
      operationId: Number(filterForm.operationId),
      expertId: filterForm.expertId,
      scores: qlForm.scores
    }
    const res = await submitQlEvaluation(payload)
    if (res && res.success) {
      ElMessage.success(res.message || '提交成功')
      filterForm.batchId = res.evaluationBatchId
      await loadQlBatches()
      await loadOperations()
      await loadQlRecords()
      hasExistingRecord.value = true
    } else {
      ElMessage.error(res?.message || '提交失败')
    }
  } catch (e) {
    ElMessage.error(e?.message || '提交失败')
  } finally {
    qlSubmitting.value = false
  }
}

function handleBatchChange() {
  filterForm.operationId = null
  filterForm.expertId = null
  loadOperations()
  loadQlRecords()
  initQlForm()
}

// 工具函数
function formatBatchLabel(b) {
  if (!b) return ''
  const id = b.evaluation_batch_id
  const n = b.row_count
  const t = b.created_at
  const timeStr = t ? String(t).replace('T', ' ').slice(0, 19) : ''
  return `${id}（${n} 条）${timeStr ? ' · ' + timeStr : ''}`
}

function gradeTagTypeForCode(code) {
  if (!code) return 'info'
  if (code.startsWith('A')) return 'success'
  if (code.startsWith('B')) return 'primary'
  if (code.startsWith('C')) return 'warning'
  if (code.startsWith('D')) return 'danger'
  return 'danger'
}

function getGradeClass(grade) {
  if (!grade) return ''
  if (grade.startsWith('A')) return 'grade-a'
  if (grade.startsWith('B')) return 'grade-b'
  if (grade.startsWith('C')) return 'grade-c'
  if (grade.startsWith('D')) return 'grade-d'
  return 'grade-e'
}

function formatDate(dateStr) {
  if (!dateStr) return '—'
  return String(dateStr).replace('T', ' ').slice(0, 19)
}

/** 单项指标把握度：后端为 0–100 的数，与表单一致 */
function formatIndicatorConfidence(raw) {
  if (raw == null || raw === '') return '—'
  const n = Number(raw)
  if (Number.isNaN(n)) return '—'
  const s = Number.isInteger(n) ? String(n) : n.toFixed(1)
  return `把握度 ${s}%`
}

// ==================== 定性指标集结（γ 与质心式 4-21） ====================

/**
 * 确定本次集结的作战范围（与上方「评分结果」表一致）：
 * - 已选「全部作战」→ ALL（本批次每个作战分别集结）
 * - 已选具体作战 → 该作战下全部专家一次性集结
 * - 未选作战 → 从当前已加载的 qlRecords 推断：仅一种作战ID则用该作战；多种则 ALL
 */
function resolveOperationIdForAggregation() {
  if (isAllOperationsSelected()) return 'ALL'
  if (filterForm.operationId != null && String(filterForm.operationId).trim() !== '') {
    return String(filterForm.operationId).trim()
  }
  const ids = [
    ...new Set(
      (qlRecords.value || [])
        .map((r) => r.operation_id)
        .filter((id) => id != null && String(id).trim() !== '')
        .map((id) => String(id).trim())
    )
  ]
  if (ids.length === 1) return ids[0]
  if (ids.length > 1) return 'ALL'
  return null
}

async function loadQualitativeAggregation(saveResult = false) {
  if (!filterForm.batchId) {
    ElMessage.warning('请先选择评估批次')
    return
  }
  const operationToken = resolveOperationIdForAggregation()
  if (!operationToken) {
    ElMessage.warning('无法确定作战范围：请先在下拉框选择作战，或先点击「刷新」加载本批次评分结果后再计算集结')
    return
  }
  aggLoading.value = true
  try {
    const params = {
      evaluationBatchId: filterForm.batchId,
      operationId: operationToken,
      wAlpha: aggWeights.wAlpha,
      wLambda: aggWeights.wLambda,
      saveResult
    }
    const res = await getQlQualitativeAggregation(params)
    if (res && Array.isArray(res.indicators)) {
      aggResult.value = res
      if (res.byOperation && res.byOperation.length > 0) {
        aggActiveOp.value = res.byOperation[0].operationId
      }
    } else if (res && Array.isArray(res.byOperation)) {
      aggResult.value = res
      aggActiveOp.value = res.byOperation[0]?.operationId
    } else {
      aggResult.value = null
    }
    if (res?.weights) {
      aggWeights.wAlpha = Number(res.weights.wAlpha)
      aggWeights.wLambda = Number(res.weights.wLambda)
    }
    if (saveResult && res?.savedCount > 0) {
      ElMessage.success(`集结结果已保存（${res.savedCount} 条记录）`)
    }
    aggDetailExpanded.value = {}
    aggChartReady.value = {}
  } catch (e) {
    ElMessage.error(e?.message || '集结失败')
  } finally {
    aggLoading.value = false
  }
}

async function saveAggregationResult() {
  aggSaving.value = true
  try {
    await loadQualitativeAggregation(true)
  } finally {
    aggSaving.value = false
  }
}

/** 当前展示的 indicators（单作战直接返回，多作战按选中的 op 返回） */
const currentIndicators = computed(() => {
  if (!aggResult.value) return []
  if (aggResult.value.indicators) {
    return aggResult.value.indicators
  }
  if (aggResult.value.byOperation) {
    const op = aggResult.value.byOperation.find(o => o.operationId === aggActiveOp.value)
    return op ? op.indicators : []
  }
  return []
})

function gradeTagTypeByScore(score) {
  if (score == null) return 'info'
  const s = Number(score)
  if (s >= 90) return 'success'
  if (s >= 75) return 'primary'
  if (s >= 60) return 'warning'
  return 'danger'
}

function gradeRangeByCode(code) {
  const row = gradeStandardRows.find(r => r.code === code)
  return row ? row.range : '—'
}

function toggleDetail(key) {
  aggDetailExpanded.value[key] = !aggDetailExpanded.value[key]
}

function toggleChart(key) {
  aggDetailExpanded.value['__chart_' + key] = !aggDetailExpanded.value['__chart_' + key]
  if (aggDetailExpanded.value['__chart_' + key]) {
    nextTick(() => renderCentroidChart(key))
  }
}

/** 文献式 4-19：在 x 处 P̄(x)=Σ γ_k·p_k(x)，p_k 为区间示性函数 → 分段常值阶梯 */
function buildWeightedCoverageStepData(details) {
  const breaks = new Set([0, 100])
  for (const d of details) {
    breaks.add(Number(d.a1))
    breaks.add(Number(d.a2))
  }
  const boundaries = [...breaks].sort((a, b) => a - b)
  const Pvals = []
  for (let j = 0; j < boundaries.length - 1; j++) {
    const L = boundaries[j]
    const R = boundaries[j + 1]
    const mid = (L + R) / 2
    let sum = 0
    for (const d of details) {
      const a1 = Number(d.a1)
      const a2 = Number(d.a2)
      if (mid >= a1 && mid <= a2) sum += Number(d.gamma)
    }
    Pvals.push(sum)
  }
  const data = []
  for (let j = 0; j < Pvals.length; j++) {
    const L = boundaries[j]
    const R = boundaries[j + 1]
    const P = Pvals[j]
    if (j === 0) {
      data.push([L, P])
    } else {
      const Pprev = Pvals[j - 1]
      if (Math.abs(P - Pprev) > 1e-9) {
        data.push([L, Pprev])
        data.push([L, P])
      }
    }
    data.push([R, P])
  }
  const maxP = Math.max(...Pvals, 0.01)
  return { data, maxP, boundaries }
}

function disposeAggChartsForKey(indicatorKey) {
  const arr = aggCharts.value[indicatorKey]
  if (Array.isArray(arr)) {
    arr.forEach((c) => {
      if (c && typeof c.dispose === 'function') c.dispose()
    })
  } else if (arr && typeof arr.dispose === 'function') {
    arr.dispose()
  }
  delete aggCharts.value[indicatorKey]
}

/** P̄(x) 阶梯图 + 质心 x* 与 60/80/90 参考线；各专家 γ 在图下方标签展示 */
function renderCentroidChart(indicatorKey) {
  const elPx = document.getElementById('ql-agg-chart-px-' + indicatorKey)
  if (!elPx) return
  const indicators = currentIndicators.value
  const ind = indicators.find((i) => i.indicatorKey === indicatorKey)
  if (!ind) return

  disposeAggChartsForKey(indicatorKey)

  const details = ind.details || []
  const xStar = ind.xStar != null ? Number(ind.xStar) : null

  elPx.style.height = '300px'

  const markLineRefs = (extra) => ({
    silent: true,
    symbol: 'none',
    data: [
      { xAxis: 60, lineStyle: { color: '#E6A23C', type: 'dotted', width: 1.5 }, label: { show: true, formatter: '60', color: '#E6A23C', fontSize: 10 } },
      { xAxis: 80, lineStyle: { color: '#F5D76E', type: 'dotted', width: 1.5 }, label: { show: true, formatter: '80', color: '#c9a227', fontSize: 10 } },
      { xAxis: 90, lineStyle: { color: '#D4A574', type: 'dotted', width: 1.5 }, label: { show: true, formatter: '90', color: '#a67c52', fontSize: 10 } },
      ...(xStar != null && !Number.isNaN(xStar)
        ? [{
            xAxis: parseFloat(xStar.toFixed(2)),
            lineStyle: { color: '#E53935', type: 'dashed', width: 2 },
            label: {
              show: true,
              formatter: `x*=${xStar.toFixed(2)}`,
              color: '#E53935',
              fontWeight: 'bold',
              fontSize: 11
            }
          }]
        : []),
      ...(extra || [])
    ]
  })

  const { data: stepData, maxP } = buildWeightedCoverageStepData(details)
  const chartPx = echarts.init(elPx)
  chartPx.setOption({
    legend: { show: false },
    title: {
      text: '加权覆盖函数 P̄(x) = Σ γ_k·p_k(x)',
      subtext: '图中曲线高度为各 x 处覆盖区间专家的 γ 之和；下方为每位专家的 γ 明细',
      left: 'center',
      top: 2,
      textStyle: { fontSize: 13, fontWeight: '600', color: '#1a3a5c' },
      subtextStyle: { fontSize: 11, color: '#909399' }
    },
    tooltip: {
      trigger: 'axis',
      formatter(params) {
        const p = Array.isArray(params) ? params[0] : params
        if (!p) return ''
        const pt = p.data
        const xv = Array.isArray(pt) ? pt[0] : p.axisValue
        const yv = Array.isArray(pt) ? pt[1] : p.value
        let tip = `x ≈ ${Number(xv).toFixed(2)}<br/>P̄(x) = ${Number(yv).toFixed(4)}（覆盖区间内 γ 之和）`
        const mid = Number(xv)
        const covering = details.filter((d) => {
          const a1 = Number(d.a1)
          const a2 = Number(d.a2)
          return mid >= a1 && mid <= a2
        })
        if (covering.length) {
          tip += '<br/><span style="color:#606266;font-size:12px">' + covering.map((d) => `${d.expertName || '—'} <b>γ=${Number(d.gamma).toFixed(4)}</b>`).join('；') + '</span>'
        }
        return tip
      }
    },
    grid: { left: 56, right: 24, top: 72, bottom: 36 },
    xAxis: {
      type: 'value',
      min: 0,
      max: 100,
      name: '评价值 x',
      nameLocation: 'middle',
      nameGap: 24,
      splitLine: { show: false }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: Math.max(Math.ceil(maxP * 1.12 * 10) / 10, 0.5),
      name: 'P̄(x)',
      splitLine: { show: true, lineStyle: { type: 'dashed', opacity: 0.35 } }
    },
    series: [
      {
        type: 'line',
        name: 'P̄(x)',
        data: stepData,
        step: false,
        showSymbol: false,
        lineStyle: { color: '#409eff', width: 2 },
        areaStyle: { color: 'rgba(64, 158, 255, 0.14)' },
        z: 2
      },
      {
        type: 'line',
        markLine: markLineRefs(),
        data: [],
        silent: true,
        z: 10
      }
    ]
  })

  aggCharts.value[indicatorKey] = chartPx
  aggChartReady.value[indicatorKey] = true
}

// 窗口 resize 时重绘图表
function resizeAggCharts() {
  Object.values(aggCharts.value).forEach((entry) => {
    if (Array.isArray(entry)) {
      entry.forEach((c) => c && c.resize())
    } else if (entry && entry.resize) {
      entry.resize()
    }
  })
}

onMounted(async () => {
  await Promise.all([
    loadQlIndicators(),
    loadQlBatches(),
    loadExperts(),
    loadOperations()
  ])
  window.addEventListener('resize', resizeAggCharts)
})
</script>

<style scoped lang="scss">
.ql-evaluation {
  .page-card {
    min-height: calc(100vh - 120px);
  }

  .card-header {
    display: flex;
    align-items: center;
    font-size: 18px;
    .header-icon {
      font-size: 24px;
      color: var(--accent-gold);
      margin-right: 8px;
    }
  }

  .filter-form {
    background: #f8faff;
    padding: 14px 16px;
    border-radius: 6px;
    border: 1px solid #e4eaf3;
    margin-bottom: 16px;
  }

  .simulate-block {
    background: #fffaf5;
    border-color: #f5e6d3;

    .simulate-body {
      padding-top: 4px;
    }

    .simulate-global-form {
      max-width: 720px;

      .range-slider-wrap {
        width: 100%;
        padding-right: 8px;
      }

      .range-label {
        display: block;
        font-size: 12px;
        color: #909399;
        margin-top: 6px;
      }

      .field-hint {
        display: block;
        font-size: 12px;
        color: #909399;
        margin-top: 4px;
      }

      .expected-range-box {
        line-height: 1.5;
        max-width: 640px;

        .expected-range-main {
          display: block;
          font-size: 14px;
          color: #303133;

          strong {
            color: #b88230;
            font-weight: 700;
          }
        }

        .expected-range-sub {
          display: block;
          font-size: 12px;
          color: #909399;
          margin-top: 4px;
        }

        .expected-range-note {
          margin: 10px 0 0;
          font-size: 12px;
          color: #606266;
          line-height: 1.55;
        }
      }

      .expected-cell {
        font-size: 12px;
        color: #b88230;
        font-weight: 600;
      }
    }

    .simulate-advanced {
      margin-top: 8px;
      border: none;

      :deep(.el-collapse-item__header) {
        font-weight: 600;
        color: #1a3a5c;
      }
    }
  }

  .grade-standard-block {
    background: #fafbfc;

    .standard-subsection {
      & + .standard-subsection {
        margin-top: 20px;
        padding-top: 16px;
        border-top: 1px dashed #dcdfe6;
      }
    }

    .standard-subheader {
      flex-direction: column;
      align-items: flex-start;
      justify-content: flex-start;
      margin-bottom: 10px;
      gap: 6px;

      .section-hint {
        margin-left: 0;
        line-height: 1.5;
      }
    }

    .grade-standard-table,
    .confidence-standard-table {
      width: 100%;

      :deep(.el-table) {
        --el-table-header-bg-color: #1a3a5c;
        --el-table-header-text-color: #fff;
      }

      :deep(.el-table__body-wrapper) {
        font-size: 13px;
      }
    }
  }

  .section-block {
    margin-bottom: 20px;
    padding: 14px;
    background: #fff;
    border-radius: 6px;
    border: 1px solid #e4eaf3;
  }

  .scoring-section {
    background: #fafcff;
  }

  .section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;

    .section-title {
      font-size: 15px;
      font-weight: 600;
      color: #1a3a5c;
    }

    .section-hint {
      font-size: 12px;
      color: #909399;
      margin-left: 12px;
    }
  }

  .scoring-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .indicator-card {
    border: 1px solid #e4eaf3;
    border-radius: 8px;
    overflow: hidden;
    background: #fff;

    .indicator-card-header {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 10px 14px;
      background: #f0f7ff;
      border-bottom: 1px solid #e4eaf3;

      .indicator-name {
        font-weight: 600;
        font-size: 14px;
        color: #1a3a5c;
      }

      .required-star {
        color: #f56c6c;
        font-size: 14px;
      }
    }

    .indicator-card-body {
      padding: 12px 14px;

      .indicator-desc {
        margin: 0 0 8px;
        font-size: 13px;
        color: #606266;
      }

      .scoring-help {
        font-size: 12px;
        color: #909399;
        margin-bottom: 10px;
        padding: 6px 10px;
        background: #f5f7fa;
        border-radius: 4px;
      }

      .scoring-row {
        display: flex;
        gap: 12px;
        align-items: center;

        .score-item {
          flex: 1;

          .score-label {
            display: block;
            font-size: 12px;
            color: #606266;
            margin-bottom: 4px;
          }

          .el-select, .el-input-number {
            width: 100%;
          }
        }
      }
    }
  }

  .submit-area {
    display: flex;
    justify-content: center;
    padding: 20px 0 10px;
    border-top: 1px dashed #e4eaf3;
    margin-top: 16px;
  }

  .results-section {
    .el-table {
      margin-top: 8px;
    }

    .metric-cell {
      padding: 4px 2px;
      line-height: 1.35;
    }

    .metric-grade {
      font-weight: 600;
      font-size: 13px;
    }

    .metric-confidence {
      margin-top: 4px;
      font-size: 11px;
      color: #909399;
    }
  }

  :deep(.el-table) {
    .text-success { color: #67c23a; font-weight: 600; }
    .text-warning { color: #e6a23c; font-weight: 600; }
    .text-danger { color: #f56c6c; font-weight: 600; }

    .grade-a { color: #67c23a; font-weight: 600; }
    .grade-b { color: #409eff; font-weight: 600; }
    .grade-c { color: #e6a23c; font-weight: 600; }
    .grade-d { color: #f56c6c; font-weight: 600; }
    .grade-e { color: #909399; font-weight: 600; }
  }

  // ==================== 集结区块 ====================
  .ql-agg-section {
    width: 100%;
    box-sizing: border-box;
    background: #f0f7ff;
    border-color: #b3d4fc;

    .agg-control-bar {
      margin-bottom: 12px;

      :deep(.el-form-item) {
        margin-bottom: 0;
      }

      .agg-weight-hint {
        margin-left: 6px;
        font-size: 11px;
        color: #909399;
      }
    }

    .agg-weights-effective {
      font-size: 13px;
      color: #1a3a5c;
      margin: 8px 0 4px;
      padding: 8px 12px;
      background: #ecf5ff;
      border-radius: 4px;
      border-left: 3px solid #409eff;

      .agg-weights-raw {
        font-size: 12px;
        color: #909399;
        margin-left: 6px;
      }
    }

    .agg-warnings {
      margin-top: 8px;
    }

    .agg-op-tabs {
      margin-bottom: 12px;

      :deep(.el-radio-group) {
        flex-wrap: wrap;
        gap: 6px;
      }
    }

    .agg-body {
      .agg-body-full {
        width: 100%;
        box-sizing: border-box;
      }

      .agg-centroid-table {
        width: 100%;

        .agg-sub-title {
          font-size: 13px;
          font-weight: 600;
          color: #1a3a5c;
          margin-bottom: 8px;
          padding-left: 4px;
          border-left: 3px solid var(--accent-gold);
        }
      }

      .agg-main-table {
        width: 100% !important;
        .xstar-val {
          font-size: 15px;
          font-weight: 700;
          color: #E53935;
          font-family: 'Courier New', monospace;
        }

        .xstar-null {
          color: #c0c4cc;
        }
      }

      .agg-detail-block,
      .agg-chart-block {
        margin-top: 16px;
        padding-top: 12px;
        border-top: 1px dashed #d0d9e8;
        width: 100%;
        box-sizing: border-box;
      }

      .agg-detail-head {
        width: 100%;
        justify-content: space-between;
        align-items: center;
        flex-wrap: wrap;
        gap: 8px 16px;
        padding: 10px 14px;
        margin-bottom: 10px;
        background: linear-gradient(90deg, #e8f2fc 0%, #f7fbff 55%, #fafcff 100%);
        border-radius: 6px;
        border: 1px solid #c5d9ed;
        box-sizing: border-box;

        .agg-detail-head-title {
          font-size: 14px;
          font-weight: 600;
          color: #1a3a5c;
          white-space: nowrap;
        }

        .agg-formula-hint {
          flex: 1 1 260px;
          text-align: right;
          line-height: 1.5;
          font-size: 11px;
          color: #606266;
          font-weight: normal;
          font-family: 'Courier New', monospace;
        }
      }

      .agg-detail-table {
        width: 100% !important;
      }

      .agg-detail-block :deep(.el-table) {
        width: 100% !important;
      }

      .agg-sub-title-sm {
        font-size: 13px;
        font-weight: 600;
        color: #1a3a5c;
        margin-bottom: 8px;
        display: flex;
        align-items: center;
        gap: 8px;

        .agg-formula-hint {
          font-size: 11px;
          color: #909399;
          font-weight: normal;
          font-family: 'Courier New', monospace;
        }
      }

      .agg-chart-px {
        width: 100%;
        height: 300px;
        min-height: 280px;
        margin-bottom: 10px;
      }

      .agg-gamma-strip {
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 8px 10px;
        padding: 10px 12px;
        background: #f5f9ff;
        border: 1px solid #d9e8f7;
        border-radius: 6px;
        font-size: 12px;
        line-height: 1.5;

        .agg-gamma-strip-title {
          color: #606266;
          font-weight: 600;
          width: 100%;
          margin-bottom: 2px;
        }

        .agg-gamma-tag {
          margin: 0;
          font-family: 'Courier New', monospace;
        }
      }
    }
  }
}
</style>