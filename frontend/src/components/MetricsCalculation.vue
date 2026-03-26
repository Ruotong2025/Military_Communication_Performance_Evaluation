<template>
  <div class="metrics-calculation">

    <!-- ====== 第一步：效能指标计算 ====== -->
    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <el-icon class="header-icon"><Setting /></el-icon>
          <span>第一步：效能指标计算</span>
        </div>
      </template>
      <el-form :model="form" label-width="120px">
        <el-form-item label="作战选择">
          <div class="select-with-hint">
            <el-select v-model="form.operationIds" multiple placeholder="请选择作战ID（留空则计算全部已有作战）" collapse-tags collapse-tags-tooltip :max-collapse-tags="5" style="width: 100%" clearable filterable>
              <el-option v-for="op in availableOperations" :key="op.operation_id" :label="`${op.operation_id} - ${op.notes || '作战'}`" :value="op.operation_id">
                <span style="float: left">{{ op.operation_id }}</span>
                <span style="float: right; color: var(--el-text-color-secondary); font-size: 12px;">{{ op.weather_condition || "" }} | {{ op.total_node_count }}节点</span>
              </el-option>
            </el-select>
            <p v-if="availableOperations.length === 0" class="field-hint">
              当前无作战记录，请先生成模拟数据后再执行指标计算。
            </p>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleCalculate" :loading="calculating" :icon="VideoPlay">计算指标</el-button>
          <el-button @click="handleRefresh" :icon="Refresh">刷新列表</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 计算结果（按评估批次） -->
    <el-card v-if="evaluationBatches.length > 0 || calculatedData.length > 0" class="results-card">
      <template #header>
        <div class="results-header">
          <div class="card-header">
            <el-icon class="header-icon"><Document /></el-icon>
            <span>原始指标数据</span>
            <el-tag type="success" style="margin-left: 10px">当前批次 {{ calculatedData.length }} 条作战记录</el-tag>
          </div>
          <div class="batch-toolbar">
            <span class="batch-label">评估批次</span>
            <el-select
              v-model="selectedEvaluationId"
              placeholder="切换历史批次（每次「计算指标」都会生成新的 evaluation_id）"
              filterable
              class="batch-select"
            >
              <el-option v-for="b in evaluationBatches" :key="b.evaluation_batch_id" :label="formatBatchLabel(b)" :value="b.evaluation_batch_id" />
            </el-select>
            <el-tag v-if="evaluationBatches.length" type="info" effect="plain" class="batch-count-tag">共 {{ evaluationBatches.length }} 个批次</el-tag>
            <el-button type="danger" plain :disabled="!selectedEvaluationId" @click="handleDeleteBatch">删除当前批次</el-button>
          </div>
          <p class="batch-hint-bar">
            每次点击「计算指标」都会在库中新增一组记录，并分配<strong>新的</strong>
            <code>evaluation_id</code>；下拉框按时间从新到旧列出全部历史批次，可任意切换查看对应原始指标与（若已算过）归一化结果。
          </p>
        </div>
      </template>

      <!-- 一级指标说明 -->
      <div class="metrics-legend">
        <div class="legend-title">一级指标与计算逻辑说明</div>
        <p class="legend-intro">下表各列为<strong>二级指标</strong>，按作战 <code>operation_id</code> 从四张 <code>records_*</code> 表聚合计算。</p>
        <el-collapse class="legend-collapse">
          <el-collapse-item title="1. 安全性（3 项）" name="sec">
            <ul class="legend-list">
              <li><strong>密钥泄露次数</strong>：<code>records_security_events</code> 中 <code>event_type = 'key_leak'</code> 的条数。</li>
              <li><strong>被侦察概率(%)</strong>：<code>detected = 1</code> 的通信条数 ÷ 总条数 × 100。</li>
              <li><strong>抗拦截能力(%)</strong>：<code>intercepted = 1</code> 的通信条数 ÷ 总条数 × 100（值越低抗拦截能力越强）。</li>
            </ul>
          </el-collapse-item>
          <el-collapse-item title="2. 可靠性（3 项）" name="rel">
            <ul class="legend-list">
              <li><strong>网络崩溃次数</strong>：<code>records_link_maintenance_events</code> 中 <code>interruption_type = 'crash'</code> 的条数（值越低越好）。</li>
              <li><strong>平均恢复时间(ms)</strong>：链路维护表中 <code>recovery_success = 1</code> 且 <code>recovery_duration_ms</code> 非空的平均值。</li>
              <li><strong>通信可用性(%)</strong>：通信时间跨度减去中断时长 ÷ 总时长 × 100。</li>
            </ul>
          </el-collapse-item>
          <el-collapse-item title="3. 传输（6 项）" name="tx">
            <ul class="legend-list">
              <li><strong>带宽(Mbps)</strong>：<code>AVG(bandwidth_hz) / 1e6</code>。</li>
              <li><strong>呼叫建立(ms)</strong>：<code>AVG(call_setup_ms)</code>。</li>
              <li><strong>传输时延(ms)</strong>：<code>AVG(trans_delay_ms)</code>。</li>
              <li><strong>误码率(%)</strong>：各条通信 <code>error_bits / total_bits</code> 的平均值 × 100。</li>
              <li><strong>吞吐量(Mbps)</strong>：<code>AVG(throughput_bps) / 1e6</code>。</li>
              <li><strong>频谱效率(bit/Hz)</strong>：<code>AVG(throughput_bps / bandwidth_hz)</code>。</li>
            </ul>
          </el-collapse-item>
          <el-collapse-item title="4. 抗干扰（3 项）" name="aj">
            <ul class="legend-list">
              <li><strong>信干噪比(dB)</strong>：<code>AVG(sinr_db)</code>。</li>
              <li><strong>抗干扰余量(dB)</strong>：<code>AVG(jamming_margin_db)</code>。</li>
              <li><strong>通信距离(km)</strong>：<code>AVG(distance_km)</code>。</li>
            </ul>
          </el-collapse-item>
          <el-collapse-item title="5. 效能（3 项）" name="ef">
            <ul class="legend-list">
              <li><strong>毁伤率(%)</strong>：<code>damaged_equipment_count / total_equipment_count × 100</code>（对己方越低越好）。</li>
              <li><strong>任务完成率(%)</strong>：<code>comm_success = 1</code> 条数 ÷ 总通信条数 × 100。</li>
              <li><strong>盲区率(%)</strong>：<code>isolated_node_count / total_node_count × 100</code>（越低越好）。</li>
            </ul>
          </el-collapse-item>
        </el-collapse>
      </div>

      <!-- ====== 原始指标表格 ====== -->
      <el-table v-if="selectedEvaluationId && calculatedData.length > 0" :data="calculatedData" border stripe max-height="500" class="metrics-result-table">
        <el-table-column prop="operationId" label="作战ID" width="100" fixed="left" align="center" header-align="center">
          <template #default="{ row }"><el-tag type="primary">{{ row.operationId }}</el-tag></template>
        </el-table-column>

        <el-table-column label="安全性" align="center" header-align="center">
          <el-table-column prop="securityKeyLeakageCount" label="密钥泄露次数" min-width="118" align="center" header-align="center">
            <template #default="{ row }"><span :class="getAlertClass(row.securityKeyLeakageCount, 0, 3)">{{ row.securityKeyLeakageCount }}</span></template>
          </el-table-column>
          <el-table-column prop="securityDetectedProbability" label="被侦察概率(%)" min-width="128" align="center" header-align="center">
            <template #default="{ row }">{{ row.securityDetectedProbability?.toFixed(2) }}%</template>
          </el-table-column>
          <el-table-column prop="securityInterceptionResistanceProbability" label="抗拦截能力(%)" min-width="128" align="center" header-align="center">
            <template #default="{ row }"><span :class="getPercentageClass(row.securityInterceptionResistanceProbability)">{{ row.securityInterceptionResistanceProbability?.toFixed(2) }}%</span></template>
          </el-table-column>
        </el-table-column>

        <el-table-column label="可靠性" align="center" header-align="center">
          <el-table-column prop="reliabilityCrashCount" label="网络崩溃次数" min-width="118" align="center" header-align="center">
            <template #default="{ row }"><span :class="getAlertClass(row.reliabilityCrashCount, 0, 20, true)">{{ row.reliabilityCrashCount }}</span></template>
          </el-table-column>
          <el-table-column prop="reliabilityRecoveryTime" label="平均恢复时间(ms)" min-width="138" align="center" header-align="center">
            <template #default="{ row }"><span :class="getAlertClass(row.reliabilityRecoveryTime, 0, 5000, true)">{{ row.reliabilityRecoveryTime?.toFixed(0) }}</span></template>
          </el-table-column>
          <el-table-column prop="reliabilityCommunicationAvailability" label="通信可用性(%)" min-width="128" align="center" header-align="center">
            <template #default="{ row }"><span :class="getPercentageClass(row.reliabilityCommunicationAvailability)">{{ row.reliabilityCommunicationAvailability?.toFixed(2) }}%</span></template>
          </el-table-column>
        </el-table-column>

        <el-table-column label="传输" align="center" header-align="center">
          <el-table-column prop="transmissionBandwidth" label="带宽(Mbps)" min-width="108" align="center" header-align="center">
            <template #default="{ row }">{{ row.transmissionBandwidth?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="transmissionCallSetupTime" label="呼叫建立(ms)" min-width="118" align="center" header-align="center">
            <template #default="{ row }">{{ row.transmissionCallSetupTime?.toFixed(0) }}</template>
          </el-table-column>
          <el-table-column prop="transmissionDelay" label="传输时延(ms)" min-width="118" align="center" header-align="center">
            <template #default="{ row }"><span :class="getAlertClass(row.transmissionDelay, 0, 100, true)">{{ row.transmissionDelay?.toFixed(2) }}</span></template>
          </el-table-column>
          <el-table-column prop="transmissionBitErrorRate" label="误码率(%)" min-width="108" align="center" header-align="center">
            <template #default="{ row }"><span :class="getAlertClass(row.transmissionBitErrorRate, 0, 1, true)">{{ row.transmissionBitErrorRate?.toFixed(4) }}%</span></template>
          </el-table-column>
          <el-table-column prop="transmissionThroughput" label="吞吐量(Mbps)" min-width="118" align="center" header-align="center">
            <template #default="{ row }">{{ row.transmissionThroughput?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="transmissionSpectralEfficiency" label="频谱效率" min-width="100" align="center" header-align="center">
            <template #default="{ row }">{{ row.transmissionSpectralEfficiency?.toFixed(4) }}</template>
          </el-table-column>
        </el-table-column>

        <el-table-column label="抗干扰" align="center" header-align="center">
          <el-table-column prop="antiJammingSinr" label="信干噪比(dB)" min-width="118" align="center" header-align="center">
            <template #default="{ row }"><span :class="getPercentageClass(row.antiJammingSinr, 0, 40)">{{ row.antiJammingSinr?.toFixed(2) }}</span></template>
          </el-table-column>
          <el-table-column prop="antiJammingMargin" label="抗干扰余量(dB)" min-width="128" align="center" header-align="center">
            <template #default="{ row }"><span :class="getPercentageClass(row.antiJammingMargin, 0, 20)">{{ row.antiJammingMargin?.toFixed(2) }}</span></template>
          </el-table-column>
          <el-table-column prop="antiJammingCommunicationDistance" label="通信距离(km)" min-width="118" align="center" header-align="center">
            <template #default="{ row }">{{ row.antiJammingCommunicationDistance?.toFixed(2) }}</template>
          </el-table-column>
        </el-table-column>

        <el-table-column label="效能" align="center" header-align="center">
          <el-table-column prop="effectDamageRate" label="毁伤率(%)" min-width="100" align="center" header-align="center">
            <template #default="{ row }"><span :class="getAlertClass(row.effectDamageRate, 0, 10, true)">{{ row.effectDamageRate?.toFixed(2) }}%</span></template>
          </el-table-column>
          <el-table-column prop="effectMissionCompletionRate" label="任务完成率(%)" min-width="128" align="center" header-align="center">
            <template #default="{ row }"><span :class="getPercentageClass(row.effectMissionCompletionRate)">{{ row.effectMissionCompletionRate?.toFixed(2) }}%</span></template>
          </el-table-column>
          <el-table-column prop="effectBlindRate" label="盲区率(%)" min-width="100" align="center" header-align="center">
            <template #default="{ row }"><span :class="getAlertClass(row.effectBlindRate, 0, 10, true)">{{ row.effectBlindRate?.toFixed(2) }}%</span></template>
          </el-table-column>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- ====== 第二步：归一化数据计算 ====== -->
    <el-card class="score-card" v-if="selectedEvaluationId && calculatedData.length > 0">
      <template #header>
        <div class="card-header">
          <el-icon class="header-icon"><TrendCharts /></el-icon>
          <span>第二步：计算归一化数据</span>
          <el-tag type="warning" style="margin-left: 8px">Min-Max 归一化 + 方向翻转</el-tag>
        </div>
      </template>

      <div class="score-hint">
        <p>基于上方 <strong>「{{ selectedEvaluationId }}」</strong> 批次的原始指标数据进行归一化处理：</p>
        <ul class="score-formula">
          <li><strong>正向指标</strong>（原始值越大越好）：<code>score = (x − min) / (max − min)</code></li>
          <li><strong>负向指标</strong>（原始值越小越好）：<code>score = (max − x) / (max − min)</code>（先翻转再按同批次 min/max 归一化）</li>
          <li>同一 <code>evaluation_id</code> 内对所有作战求 min、max；归一化后 Score ∈ [0, 1]，<strong>分数越高表示该指标表现越好</strong>。</li>
        </ul>
        <div class="direction-lists">
          <div class="direction-block direction-positive">
            <div class="direction-title">
              <el-tag type="success" size="small">正向</el-tag>
              <span>共 {{ POSITIVE_METRICS.length }} 项 · 越大越好 → 直接 Min-Max</span>
            </div>
            <ol>
              <li v-for="(t, i) in POSITIVE_METRICS" :key="'p' + i">{{ t }}</li>
            </ol>
          </div>
          <div class="direction-block direction-negative">
            <div class="direction-title">
              <el-tag type="danger" size="small">负向</el-tag>
              <span>共 {{ NEGATIVE_METRICS.length }} 项 · 越小越好 → 翻转后 Min-Max</span>
            </div>
            <ol>
              <li v-for="(t, i) in NEGATIVE_METRICS" :key="'n' + i">{{ t }}</li>
            </ol>
          </div>
        </div>
      </div>

      <div class="score-actions">
        <el-button type="success" @click="handleGenerateScore" :loading="generatingScore" :icon="VideoPlay">
          计算归一化得分
        </el-button>
        <el-button v-if="scoreGenerated" type="info" plain @click="handleRefreshScore" :icon="Refresh">刷新数据</el-button>
      </div>

      <!-- 归一化说明 -->
      <div v-if="!scoreGenerated" class="score-waiting-hint">
        <el-empty description="请点击上方「计算归一化得分」按钮开始归一化处理" :image-size="80">
          <template #image><el-icon :size="48" style="color: #c0c4cc"><TrendCharts /></el-icon></template>
        </el-empty>
      </div>

      <!-- ====== 归一化 Score 表格 ====== -->
      <div v-if="scoreGenerated && scoreData.length > 0" class="section-title" style="margin-top: 16px">
        <el-icon><Document /></el-icon> 归一化得分表（0~1，越大越好）
        <el-tag type="success" style="margin-left: 8px">{{ scoreData.length }} 条作战记录</el-tag>
      </div>
      <el-table v-if="scoreGenerated && scoreData.length > 0" :data="scoreData" border stripe max-height="500" class="metrics-result-table score-table">
        <el-table-column prop="operationId" label="作战ID" width="100" fixed="left" align="center" header-align="center">
          <template #default="{ row }"><el-tag type="primary">{{ row.operationId }}</el-tag></template>
        </el-table-column>

        <el-table-column label="安全性" align="center" header-align="center">
          <el-table-column prop="securityKeyLeakageCount" label="密钥泄露次数" min-width="118" align="center" header-align="center">
            <template #default="{ row }"><span :class="getScoreClass(row.securityKeyLeakageCount)">{{ row.securityKeyLeakageCount != null ? row.securityKeyLeakageCount.toFixed(3) : '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="securityDetectedProbability" label="被侦察概率" min-width="118" align="center" header-align="center">
            <template #default="{ row }"><span :class="getScoreClass(row.securityDetectedProbability)">{{ row.securityDetectedProbability != null ? row.securityDetectedProbability.toFixed(3) : '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="securityInterceptionResistanceProbability" label="抗拦截能力" min-width="118" align="center" header-align="center">
            <template #default="{ row }"><span :class="getScoreClass(row.securityInterceptionResistanceProbability)">{{ row.securityInterceptionResistanceProbability != null ? row.securityInterceptionResistanceProbability.toFixed(3) : '—' }}</span></template>
          </el-table-column>
        </el-table-column>

        <el-table-column label="可靠性" align="center" header-align="center">
          <el-table-column prop="reliabilityCrashCount" label="网络崩溃次数" min-width="118" align="center" header-align="center">
            <template #default="{ row }"><span :class="getScoreClass(row.reliabilityCrashCount)">{{ row.reliabilityCrashCount != null ? row.reliabilityCrashCount.toFixed(3) : '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="reliabilityRecoveryTime" label="恢复时间" min-width="118" align="center" header-align="center">
            <template #default="{ row }"><span :class="getScoreClass(row.reliabilityRecoveryTime)">{{ row.reliabilityRecoveryTime != null ? row.reliabilityRecoveryTime.toFixed(3) : '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="reliabilityCommunicationAvailability" label="通信可用性" min-width="118" align="center" header-align="center">
            <template #default="{ row }"><span :class="getScoreClass(row.reliabilityCommunicationAvailability)">{{ row.reliabilityCommunicationAvailability != null ? row.reliabilityCommunicationAvailability.toFixed(3) : '—' }}</span></template>
          </el-table-column>
        </el-table-column>

        <el-table-column label="传输" align="center" header-align="center">
          <el-table-column prop="transmissionBandwidth" label="带宽" min-width="100" align="center" header-align="center">
            <template #default="{ row }"><span :class="getScoreClass(row.transmissionBandwidth)">{{ row.transmissionBandwidth != null ? row.transmissionBandwidth.toFixed(3) : '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="transmissionCallSetupTime" label="呼叫建立" min-width="100" align="center" header-align="center">
            <template #default="{ row }"><span :class="getScoreClass(row.transmissionCallSetupTime)">{{ row.transmissionCallSetupTime != null ? row.transmissionCallSetupTime.toFixed(3) : '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="transmissionDelay" label="传输时延" min-width="100" align="center" header-align="center">
            <template #default="{ row }"><span :class="getScoreClass(row.transmissionDelay)">{{ row.transmissionDelay != null ? row.transmissionDelay.toFixed(3) : '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="transmissionBitErrorRate" label="误码率" min-width="100" align="center" header-align="center">
            <template #default="{ row }"><span :class="getScoreClass(row.transmissionBitErrorRate)">{{ row.transmissionBitErrorRate != null ? row.transmissionBitErrorRate.toFixed(3) : '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="transmissionThroughput" label="吞吐量" min-width="100" align="center" header-align="center">
            <template #default="{ row }"><span :class="getScoreClass(row.transmissionThroughput)">{{ row.transmissionThroughput != null ? row.transmissionThroughput.toFixed(3) : '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="transmissionSpectralEfficiency" label="频谱效率" min-width="100" align="center" header-align="center">
            <template #default="{ row }"><span :class="getScoreClass(row.transmissionSpectralEfficiency)">{{ row.transmissionSpectralEfficiency != null ? row.transmissionSpectralEfficiency.toFixed(3) : '—' }}</span></template>
          </el-table-column>
        </el-table-column>

        <el-table-column label="抗干扰" align="center" header-align="center">
          <el-table-column prop="antiJammingSinr" label="信干噪比" min-width="110" align="center" header-align="center">
            <template #default="{ row }"><span :class="getScoreClass(row.antiJammingSinr)">{{ row.antiJammingSinr != null ? row.antiJammingSinr.toFixed(3) : '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="antiJammingMargin" label="抗干扰余量" min-width="110" align="center" header-align="center">
            <template #default="{ row }"><span :class="getScoreClass(row.antiJammingMargin)">{{ row.antiJammingMargin != null ? row.antiJammingMargin.toFixed(3) : '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="antiJammingCommunicationDistance" label="通信距离" min-width="110" align="center" header-align="center">
            <template #default="{ row }"><span :class="getScoreClass(row.antiJammingCommunicationDistance)">{{ row.antiJammingCommunicationDistance != null ? row.antiJammingCommunicationDistance.toFixed(3) : '—' }}</span></template>
          </el-table-column>
        </el-table-column>

        <el-table-column label="效能" align="center" header-align="center">
          <el-table-column prop="effectDamageRate" label="毁伤率" min-width="100" align="center" header-align="center">
            <template #default="{ row }"><span :class="getScoreClass(row.effectDamageRate)">{{ row.effectDamageRate != null ? row.effectDamageRate.toFixed(3) : '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="effectMissionCompletionRate" label="任务完成率" min-width="110" align="center" header-align="center">
            <template #default="{ row }"><span :class="getScoreClass(row.effectMissionCompletionRate)">{{ row.effectMissionCompletionRate != null ? row.effectMissionCompletionRate.toFixed(3) : '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="effectBlindRate" label="盲区率" min-width="100" align="center" header-align="center">
            <template #default="{ row }"><span :class="getScoreClass(row.effectBlindRate)">{{ row.effectBlindRate != null ? row.effectBlindRate.toFixed(3) : '—' }}</span></template>
          </el-table-column>
        </el-table-column>
      </el-table>

      <!-- ====== 多实验对比图 ====== -->
      <div v-if="scoreGenerated && chartData.series && chartData.series.length > 0" class="section-title" style="margin-top: 24px">
        <el-icon><TrendCharts /></el-icon> 多实验对比图
        <el-tag type="warning" style="margin-left: 8px">批次: {{ selectedEvaluationId }}</el-tag>
        <el-button text size="small" style="margin-left: 12px" @click="chartType = chartType === 'line' ? 'bar' : 'line'">
          切换为{{ chartType === 'line' ? '柱状图' : '折线图' }}
        </el-button>
      </div>
      <div
        v-if="scoreGenerated && chartData.series && chartData.series.length > 0"
        class="chart-scroll-wrap"
      >
        <div
          ref="chartRef"
          class="chart-container"
          :style="{ minWidth: chartMinWidthPx + 'px' }"
        />
      </div>
    </el-card>

    <!-- 空状态 -->
    <el-empty v-if="!calculating && evaluationBatches.length === 0" description="暂无评估批次，请先点击「计算指标」生成原始指标数据">
      <template #image><el-icon :size="60"><DataAnalysis /></el-icon></template>
    </el-empty>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick, computed } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  Setting,
  Document,
  Refresh,
  DataAnalysis,
  VideoPlay,
  TrendCharts,
} from "@element-plus/icons-vue";
import {
  getAvailableOperations,
  calculateMetrics,
  getCalculatedMetrics,
  getMetricBatches,
  deleteMetricBatch,
  getScoreData,
  getScoreChartData,
  generateScore,
} from "@/api";
import * as echarts from "echarts";

const availableOperations = ref([]);
const calculatedData = ref([]);
const scoreData = ref([]);
const chartData = ref({ indicators: [], series: [] });
const evaluationBatches = ref([]);
const selectedEvaluationId = ref(null);
const calculating = ref(false);
const generatingScore = ref(false);
const scoreGenerated = ref(false);
const chartRef = ref(null);
const chartType = ref("line");
let chartInstance = null;

const form = ref({
  operationIds: [],
});

/** 与后端 MetricsDirection 一致：正向 = 原始值越大越好 */
const POSITIVE_METRICS = [
  "抗拦截能力(%)",
  "通信可用性(%)",
  "带宽(Mbps)",
  "吞吐量(Mbps)",
  "频谱效率",
  "信干噪比(dB)",
  "抗干扰余量(dB)",
  "通信距离(km)",
  "任务完成率(%)",
];

/** 负向 = 原始值越小越好，归一化时先做 (max−x)/(max−min) */
const NEGATIVE_METRICS = [
  "密钥泄露次数",
  "被侦察概率(%)",
  "网络崩溃次数",
  "平均恢复时间(ms)",
  "呼叫建立(ms)",
  "传输时延(ms)",
  "误码率(%)",
  "毁伤率(%)",
  "盲区率(%)",
];

const chartMinWidthPx = computed(() => {
  const n =
    chartData.value.indicators && chartData.value.indicators.length
      ? chartData.value.indicators.length
      : 18;
  return Math.max(1100, n * 80 + 160);
});

function toNum(v) {
  if (v == null || v === "") return 0;
  const n = Number(v);
  return Number.isFinite(n) ? n : 0;
}

function normalizeMetricsRow(r) {
  if (!r) return null;
  if (r.operationId != null) return r;
  return {
    operationId: r.operation_id,
    evaluationId: r.evaluation_id,
    securityKeyLeakageCount: toNum(r.security_key_leakage_count),
    securityDetectedProbability: toNum(r.security_detected_probability),
    securityInterceptionResistanceProbability: toNum(
      r.security_interception_resistance_probability,
    ),
    reliabilityCrashCount: toNum(r.reliability_crash_count),
    reliabilityRecoveryTime: toNum(r.reliability_recovery_time),
    reliabilityCommunicationAvailability: toNum(
      r.reliability_communication_availability,
    ),
    transmissionBandwidth: toNum(r.transmission_bandwidth),
    transmissionCallSetupTime: toNum(r.transmission_call_setup_time),
    transmissionDelay: toNum(r.transmission_delay),
    transmissionBitErrorRate: toNum(r.transmission_bit_error_rate),
    transmissionThroughput: toNum(r.transmission_throughput),
    transmissionSpectralEfficiency: toNum(r.transmission_spectral_efficiency),
    antiJammingSinr: toNum(r.anti_jamming_sinr),
    antiJammingMargin: toNum(r.anti_jamming_margin),
    antiJammingCommunicationDistance: toNum(
      r.anti_jamming_communication_distance,
    ),
    effectDamageRate: toNum(r.effect_damage_rate),
    effectMissionCompletionRate: toNum(r.effect_mission_completion_rate),
    effectBlindRate: toNum(r.effect_blind_rate),
  };
}

// score 表字段名（来自 DESCRIBE score_military_comm_effect）
const SCORE_FIELDS = [
  "security_key_leakage_qt", "security_detected_probability_qt", "security_interception_resistance_ql",
  "reliability_crash_rate_qt", "reliability_recovery_capability_qt", "reliability_communication_availability_qt",
  "transmission_bandwidth_qt", "transmission_call_setup_time_qt", "transmission_transmission_delay_qt", "transmission_bit_error_rate_qt",
  "transmission_throughput_qt", "transmission_spectral_efficiency_qt",
  "anti_jamming_sinr_qt", "anti_jamming_anti_jamming_margin_qt", "anti_jamming_communication_distance_qt",
  "effect_damage_rate_qt", "effect_mission_completion_rate_qt", "effect_blind_rate_qt",
];

// score 表字段名 → 模板 prop 使用的 camelCase 字段名（对应 metrics 表的原始字段）
const SCORE_TO_CAMEL = {
  "security_key_leakage_qt": "securityKeyLeakageCount",
  "security_detected_probability_qt": "securityDetectedProbability",
  "security_interception_resistance_ql": "securityInterceptionResistanceProbability",
  "reliability_crash_rate_qt": "reliabilityCrashCount",
  "reliability_recovery_capability_qt": "reliabilityRecoveryTime",
  "reliability_communication_availability_qt": "reliabilityCommunicationAvailability",
  "transmission_bandwidth_qt": "transmissionBandwidth",
  "transmission_call_setup_time_qt": "transmissionCallSetupTime",
  "transmission_transmission_delay_qt": "transmissionDelay",
  "transmission_bit_error_rate_qt": "transmissionBitErrorRate",
  "transmission_throughput_qt": "transmissionThroughput",
  "transmission_spectral_efficiency_qt": "transmissionSpectralEfficiency",
  "anti_jamming_sinr_qt": "antiJammingSinr",
  "anti_jamming_anti_jamming_margin_qt": "antiJammingMargin",
  "anti_jamming_communication_distance_qt": "antiJammingCommunicationDistance",
  "effect_damage_rate_qt": "effectDamageRate",
  "effect_mission_completion_rate_qt": "effectMissionCompletionRate",
  "effect_blind_rate_qt": "effectBlindRate",
};

function normalizeScoreRow(r) {
  if (!r) return null;
  if (r.operationId != null) return r;
  const row = { operationId: r.operation_id };
  SCORE_FIELDS.forEach((f) => {
    const camel = SCORE_TO_CAMEL[f] || f.replace(/_([a-z])/g, (_, c) => c.toUpperCase());
    row[camel] = r[f] != null ? Number(r[f]) : null;
  });
  return row;
}

function formatBatchLabel(b) {
  if (!b) return "";
  const id = b.evaluation_batch_id;
  const n = b.row_count;
  const t = b.created_at;
  const timeStr = t ? String(t).replace("T", " ").slice(0, 19) : "";
  return `${id}（${n} 条）${timeStr ? " · " + timeStr : ""}`;
}

const loadBatches = async () => {
  try {
    const data = await getMetricBatches();
    const list = Array.isArray(data) ? data : [];
    evaluationBatches.value = list.filter(
      (x) => x && x.evaluation_batch_id != null,
    );
    const ids = new Set(
      evaluationBatches.value.map((x) => x.evaluation_batch_id),
    );
    if (selectedEvaluationId.value && !ids.has(selectedEvaluationId.value)) {
      selectedEvaluationId.value = null;
    }
    if (!selectedEvaluationId.value && evaluationBatches.value.length > 0) {
      selectedEvaluationId.value =
        evaluationBatches.value[0].evaluation_batch_id;
    }
  } catch (error) {
    console.error("加载评估批次失败", error);
    ElMessage.error(error?.message || "加载评估批次失败");
  }
};

const syncBatchView = async () => {
  await loadBatches();
};

const onBatchChange = () => {
  loadResults();
};

const loadOperations = async () => {
  try {
    const data = await getAvailableOperations();
    const list = Array.isArray(data) ? data : [];
    availableOperations.value = list.filter(
      (op) => op && op.operation_id != null,
    );
  } catch (error) {
    console.error("加载作战ID失败", error);
    ElMessage.error(error?.message || "加载作战ID失败");
  }
};

const handleCalculate = async () => {
  calculating.value = true;
  try {
    const payload = {
      operationIds: form.value.operationIds,
      specificOnly: form.value.operationIds.length > 0,
    };
    const res = await calculateMetrics(payload);
    if (res && res.success) {
      ElMessage.success(res.message || "计算成功");
      const newBatchId = res.evaluationBatchId;
      // 直接切换到新批次，syncBatchView 刷新下拉列表并保持选中
      selectedEvaluationId.value = newBatchId;
      await syncBatchView();
      // 新批次默认没有 score，重置相关状态
      scoreGenerated.value = false;
      scoreData.value = [];
      chartData.value = { indicators: [], series: [] };
      if (chartInstance) {
        chartInstance.dispose();
        chartInstance = null;
      }
    } else {
      ElMessage.error(res?.message || "计算失败");
    }
  } catch (error) {
    console.error("计算指标失败", error);
    ElMessage.error(error?.message || "计算指标失败");
  } finally {
    calculating.value = false;
  }
};

const handleGenerateScore = async () => {
  if (!selectedEvaluationId.value) {
    ElMessage.warning("请先选择一个评估批次");
    return;
  }
  generatingScore.value = true;
  try {
    const res = await generateScore(selectedEvaluationId.value);
    if (res && res.success) {
      ElMessage.success(res.message || "归一化得分计算成功");
      await loadScoreData();
      scoreGenerated.value = scoreData.value.length > 0;
      if (scoreGenerated.value) {
        await loadChartData();
      }
    } else {
      ElMessage.error(res?.message || "归一化得分计算失败");
    }
  } catch (error) {
    console.error("计算归一化得分失败", error);
    ElMessage.error(error?.message || "计算归一化得分失败");
  } finally {
    generatingScore.value = false;
  }
};

const handleRefreshScore = async () => {
  await loadScoreData();
  await loadChartData();
};

const loadResults = async () => {
  try {
    if (!selectedEvaluationId.value) {
      calculatedData.value = [];
      return;
    }
    const data = await getCalculatedMetrics(selectedEvaluationId.value);
    const list = Array.isArray(data) ? data : [];
    calculatedData.value = list.map(normalizeMetricsRow).filter(Boolean);
  } catch (error) {
    console.error("加载结果失败", error);
    calculatedData.value = [];
  }
};

const loadScoreData = async () => {
  try {
    if (!selectedEvaluationId.value) {
      scoreData.value = [];
      return;
    }
    const data = await getScoreData(selectedEvaluationId.value);
    const list = Array.isArray(data) ? data : [];
    scoreData.value = list.map(normalizeScoreRow).filter(Boolean);
  } catch (error) {
    console.error("加载 score 数据失败", error);
    scoreData.value = [];
  }
};

const loadChartData = async () => {
  try {
    if (!selectedEvaluationId.value) {
      chartData.value = { indicators: [], series: [] };
      return;
    }
    const res = await getScoreChartData(selectedEvaluationId.value);
    if (res && res.success !== false) {
      chartData.value = res.data || res || { indicators: [], series: [] };
      await nextTick();
      renderChart();
    } else {
      chartData.value = { indicators: [], series: [] };
    }
  } catch (error) {
    console.error("加载图表数据失败", error);
    chartData.value = { indicators: [], series: [] };
  }
};

function getScoreClass(value) {
  if (value == null) return "";
  if (value >= 0.8) return "text-success";
  if (value >= 0.5) return "text-warning";
  return "text-danger";
}

function renderChart() {
  if (!chartRef.value || !chartData.value.series || chartData.value.series.length === 0) {
    return;
  }
  if (chartInstance) {
    chartInstance.dispose();
    chartInstance = null;
  }
  chartInstance = echarts.init(chartRef.value);

  const indicators = chartData.value.indicators || [];
  const series = chartData.value.series || [];
  const xAxisData = indicators.map((i) => i.name);

  const colors = [
    "#5470c6", "#91cc75", "#fac858", "#ee6666", "#73c0de",
    "#3ba272", "#fc8452", "#9a60b4", "#ea7ccc", "#1d6eac",
  ];

  const seriesData = series.map((s, idx) => ({
    name: s.name,
    type: chartType.value,
    data: s.data,
    itemStyle: { color: colors[idx % colors.length] },
    smooth: true,
    label: {
      show: chartType.value === "bar",
      formatter: (p) => p.value != null ? p.value.toFixed(2) : "—",
      fontSize: 10,
    },
  }));

  const option = {
    backgroundColor: "#fff",
    tooltip: {
      trigger: "axis",
      axisPointer: { type: chartType.value === "line" ? "line" : "shadow" },
      formatter: (params) => {
        if (!params || params.length === 0) return "";
        const axisValue = params[0].axisValue;
        let html = `<strong>${axisValue}</strong><br/>`;
        params.forEach((p) => {
          const val = p.value != null ? p.value.toFixed(3) : "—";
          const color = p.color.colorStops ? p.color.colorStops[0].color : p.color;
          html += `<span style="display:inline-block;margin-right:4px;border-radius:50%;width:10px;height:10px;background-color:${color};"></span>${p.seriesName}: ${val}<br/>`;
        });
        return html;
      },
    },
    legend: {
      data: series.map((s) => s.name),
      top: 30,
      type: "scroll",
    },
    grid: {
      left: "48px",
      right: "24px",
      bottom: "28%",
      top: series.length > 4 ? "88px" : "68px",
      containLabel: true,
    },
    dataZoom: [
      {
        type: "inside",
        xAxisIndex: 0,
        filterMode: "none",
      },
      {
        type: "slider",
        xAxisIndex: 0,
        height: 22,
        bottom: 6,
        filterMode: "none",
      },
    ],
    xAxis: {
      type: "category",
      data: xAxisData,
      axisLabel: {
        interval: 0,
        rotate: 42,
        fontSize: 11,
        margin: 14,
        hideOverlap: false,
      },
    },
    yAxis: {
      type: "value",
      name: "Score",
      min: 0,
      max: 1,
      axisLabel: { formatter: "{value}" },
    },
    series: seriesData,
  };

  chartInstance.setOption(option, true);
  nextTick(() => {
    chartInstance?.resize();
  });
}

const handleDeleteBatch = async () => {
  const id = selectedEvaluationId.value;
  if (!id) return;
  try {
    await ElMessageBox.confirm(
      `确定删除评估批次「${id}」下的全部指标记录？此操作不可恢复。`,
      "删除评估批次",
      { type: "warning", confirmButtonText: "删除", cancelButtonText: "取消" },
    );
    await deleteMetricBatch(id);
    ElMessage.success("已删除该批次");
    calculatedData.value = [];
    scoreData.value = [];
    chartData.value = { indicators: [], series: [] };
    if (chartInstance) {
      chartInstance.dispose();
      chartInstance = null;
    }
    selectedEvaluationId.value = null;
    await loadBatches();
    await loadResults();
  } catch (e) {
    if (e !== "cancel") {
      ElMessage.error(e?.message || "删除失败");
    }
  }
};

const handleRefresh = () => {
  loadOperations();
  loadBatches().then(() => {
    loadResults();
    loadScoreData().then(() => {
      scoreGenerated.value = scoreData.value.length > 0;
    });
    chartData.value = { indicators: [], series: [] };
    if (chartInstance) {
      chartInstance.dispose();
      chartInstance = null;
    }
  });
};

function refreshFromParent() {
  loadOperations();
  loadBatches().then(() => {
    loadResults();
    loadScoreData().then(() => {
      scoreGenerated.value = scoreData.value.length > 0;
    });
    chartData.value = { indicators: [], series: [] };
    if (chartInstance) {
      chartInstance.dispose();
      chartInstance = null;
    }
  });
}

defineExpose({ refreshFromParent, loadOperations, loadResults });

watch(chartType, () => {
  renderChart();
});

watch(selectedEvaluationId, async () => {
  if (!selectedEvaluationId.value) return;
  calculatedData.value = [];
  scoreData.value = [];
  chartData.value = { indicators: [], series: [] };
  if (chartInstance) {
    chartInstance.dispose();
    chartInstance = null;
  }
  await loadResults();
  await loadScoreData();
  scoreGenerated.value = scoreData.value.length > 0;
  if (scoreGenerated.value) {
    await loadChartData();
  }
});

watch(chartMinWidthPx, () => {
  nextTick(() => chartInstance?.resize());
});

function onWindowResize() {
  chartInstance?.resize();
}

onMounted(() => {
  loadOperations();
  loadBatches().then(() => {
    loadResults();
    // 页面初始化时自动检测当前批次是否已有 score 数据（watch 也会处理）
    loadScoreData().then(() => {
      scoreGenerated.value = scoreData.value.length > 0;
      if (scoreGenerated.value) {
        loadChartData();
      }
    });
  });
  window.addEventListener("resize", onWindowResize);
});

onUnmounted(() => {
  window.removeEventListener("resize", onWindowResize);
  if (chartInstance) {
    chartInstance.dispose();
    chartInstance = null;
  }
});

const getPercentageClass = (value, min = 0, max = 100) => {
  if (value == null) return "";
  const percentage = ((value - min) / (max - min)) * 100;
  if (percentage >= 80) return "text-success";
  if (percentage >= 50) return "text-warning";
  return "text-danger";
};

const getAlertClass = (value, min, max, reverse = false) => {
  if (value == null) return "";
  const percentage = ((value - min) / (max - min)) * 100;
  if (reverse) {
    if (percentage <= 30) return "text-success";
    if (percentage <= 60) return "text-warning";
    return "text-danger";
  }
  if (percentage >= 80) return "text-success";
  if (percentage >= 50) return "text-warning";
  return "text-danger";
};

</script>

<style scoped lang="scss">
.metrics-calculation {
  .metrics-legend {
    margin-bottom: 16px;
    padding: 12px 14px;
    background: #f8fafc;
    border: 1px solid #e4eaf3;
    border-radius: 8px;

    .legend-title {
      font-size: 15px;
      font-weight: 600;
      color: #1a1a2e;
      margin-bottom: 8px;
    }

    .legend-intro {
      font-size: 13px;
      color: #606266;
      line-height: 1.6;
      margin: 0 0 10px;
    }

    .legend-collapse {
      border: none;
      --el-collapse-header-height: 44px;
    }

    .legend-list {
      margin: 0;
      padding-left: 1.25rem;
      font-size: 13px;
      line-height: 1.75;
      color: #303133;

      li {
        margin-bottom: 6px;
      }

      code {
        font-size: 12px;
        padding: 0 4px;
        background: #eef2f7;
        border-radius: 3px;
      }
    }
  }

  .select-with-hint {
    width: 100%;
  }

  .field-hint {
    margin: 8px 0 0;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    line-height: 1.5;
  }

  .config-card {
    margin-bottom: 20px;
  }

  .results-card {
    margin-bottom: 20px;
  }

  .score-card {
    margin-bottom: 20px;
  }

  .score-hint {
    padding: 12px 14px;
    background: #f0f9eb;
    border: 1px solid #c2e7b0;
    border-radius: 6px;
    margin-bottom: 16px;
    font-size: 13px;
    color: #606266;

    p {
      margin: 0 0 6px;
      font-weight: 500;
    }

    ul {
      margin: 0;
      padding-left: 20px;
    }

    li {
      line-height: 1.8;
    }

    .score-formula {
      margin-bottom: 12px;

      code {
        font-size: 12px;
        padding: 2px 6px;
        background: #fff;
        border-radius: 4px;
        border: 1px solid #dcdfe6;
      }
    }

    .direction-lists {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 14px;
      margin-top: 10px;

      @media (max-width: 900px) {
        grid-template-columns: 1fr;
      }
    }

    .direction-block {
      padding: 10px 12px;
      border-radius: 8px;
      border: 1px solid #e4eaf3;
      background: #fff;

      ol {
        margin: 8px 0 0;
        padding-left: 1.25rem;
        font-size: 12px;
        line-height: 1.85;
        color: #303133;
      }

      .direction-title {
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 8px;
        font-size: 12px;
        color: #606266;
        font-weight: 500;
      }
    }

    .direction-positive {
      border-left: 3px solid #67c23a;
    }

    .direction-negative {
      border-left: 3px solid #f56c6c;
    }

  }

  .score-actions {
    margin-bottom: 16px;
    display: flex;
    gap: 10px;
    align-items: center;
  }

  .score-waiting-hint {
    margin: 24px 0;
    text-align: center;
  }

  .results-header {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .batch-toolbar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 10px;
  }

  .batch-label {
    font-size: 13px;
    color: var(--el-text-color-secondary);
    white-space: nowrap;
  }

  .batch-hint-bar {
    margin: 8px 0 0;
    font-size: 13px;
    color: var(--el-text-color-secondary);
    line-height: 1.5;
  }

  .batch-select {
    min-width: 280px;
    max-width: 520px;
  }

  .batch-hint {
    margin: 0 0 12px;
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  .card-header {
    display: flex;
    align-items: center;
    font-size: 16px;

    .header-icon {
      font-size: 20px;
      margin-right: 8px;
      color: var(--el-color-primary);
    }
  }

  .section-title {
    display: flex;
    align-items: center;
    font-size: 15px;
    font-weight: 600;
    color: #1a3a5c;
    margin-bottom: 12px;
    gap: 6px;

    .el-icon {
      color: var(--el-color-primary);
      font-size: 18px;
    }
  }

  .metrics-result-table {
    width: 100%;

    :deep(.el-table__header-wrapper thead tr:first-child th.el-table__cell) {
      background: #e8f1fc;
      font-weight: 600;
      color: #1a3a5c;
    }
  }

  .chart-scroll-wrap {
    width: 100%;
    overflow-x: auto;
    overflow-y: hidden;
    padding-bottom: 4px;
    -webkit-overflow-scrolling: touch;
  }

  .chart-container {
    width: 100%;
    height: 460px;
    border: 1px solid #e4eaf3;
    border-radius: 8px;
    padding: 8px;
    background: #fff;
    box-sizing: border-box;
  }

  :deep(.el-table) {
    .text-success {
      color: #67c23a;
      font-weight: 600;
    }

    .text-warning {
      color: #e6a23c;
      font-weight: 600;
    }

    .text-danger {
      color: #f56c6c;
      font-weight: 600;
    }
  }
}
</style>
