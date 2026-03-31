<template>
  <el-card class="ahp-config-card">
    <template #header>
      <div class="ahp-header-wrap">
        <div class="card-header card-header-row1">
          <el-icon class="header-icon"><Setting /></el-icon>
          <span>专家AHP权重配置</span>
          <el-tag type="warning" effect="plain" style="margin-left: 10px;">仅需输入上三角区域，下三角自动生成倒数</el-tag>
        </div>
        <!-- 专家与模拟入口放在标题栏内，避免被下方长说明表顶出首屏 -->
        <div class="ahp-header-expert" @click.stop>
          <el-form :inline="true" class="expert-ahp-form expert-ahp-form--header">
            <el-form-item label="当前专家">
              <el-select
                v-model="selectedExpertId"
                placeholder="选择专家（与可信度库 expert_id 一致）"
                clearable
                filterable
                style="width: min(280px, 100%)"
                @change="onExpertChange"
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
      <el-alert type="info" :closable="false" show-icon class="matrix-guide compact expert-ahp-hint">
        <template v-if="!selectedExpertId">请先在标题栏选择「当前专家」；选择后将展示维度层与指标层判断矩阵。未选专家时仍可使用下方「批量模拟入库」生成数据。</template>
        <template v-else>
          已选择专家：自动加载该专家的比较打分，以及表 <code>expert_ahp_individual_weights</code> 中已保存的层次权重结果；若无打分记录则标度全为 1、把握度均为 {{ defaultBlankConfidence }}（低于 0.6）。保存或批量模拟入库时会同时覆盖更新两张表。
        </template>
      </el-alert>

      <!-- 标度说明（1、3、5、7、9 及中间值 2、4、6、8） -->
      <div class="scale-section scale-legend-block">
        <h3 class="scale-legend-title">
          <el-icon><InfoFilled /></el-icon>
          AHP 判断标度说明（Saaty 1～9 标度）
        </h3>
        <p class="scale-legend-intro">
          上三角「重要性」为 <strong>Saaty 正互反标度</strong>，取值区间 <strong>[1/9，9]</strong>（可填小数）：表示<strong>行要素相对列要素</strong>。<strong>1</strong> 为同等重要；<strong>&gt;1</strong> 表示行比列重要（如 3、5）；<strong>&lt;1</strong> 表示行不如列重要（如 0.33≈1/3、0.2=1/5）。下三角为互反倒数。
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
          1 级对应 λ=1，2 级对应 0.8，3 级对应 0.6；4 级表示不能确认，建议 λ 取 <strong>小于 0.6</strong> 的数值（如 0.5、0.4）。新选专家且无已存数据时，系统默认标度全为 1、把握度为 {{ defaultBlankConfidence }}（低于 0.6）。
        </p>
        <el-table :data="ahpConfidenceLevelLegend" border stripe size="small" class="scale-legend-table">
          <el-table-column prop="level" label="等级" width="72" align="center" />
          <el-table-column prop="meaning" label="等级含义" width="100" />
          <el-table-column prop="standard" label="等级标准" min-width="280" />
          <el-table-column prop="lambda" label="判断可信度 λ" width="120" align="center" />
        </el-table>
      </div>

      <el-empty
        v-if="!selectedExpertId"
        class="ahp-select-expert-empty"
        description="请先在上方的「当前专家」中选择一名专家，选择后将展示维度层与指标层判断矩阵。"
      />

      <!-- 维度层判断矩阵 -->
      <div v-if="selectedExpertId" class="matrix-section">
        <h3 class="section-title">
          <el-icon><Grid /></el-icon>
          维度层判断矩阵（5×5，共 10 对上三角比较）
        </h3>

        <el-alert type="info" :closable="false" class="matrix-guide">
          <p class="guide-line">
            <strong>矩阵含义：</strong>第 <em>i</em> 行与第 <em>j</em> 列交叉处（上三角可编辑）表示<strong>行方向要素相对于列方向要素</strong>的重要性标度。
            <strong>重要性</strong>请输入 <strong>{{ AHP_SCORE_MIN.toFixed(4) }}～{{ AHP_SCORE_MAX }}</strong> 内的小数或整数（与 Saaty 标度一致；&lt;1 表示行不如列重要）。
            下三角由系统自动填写为互反倒数（1/标度）。
          </p>
          <p class="guide-line">
            <strong>本层包含的五个评估维度：</strong>安全性、可靠性、传输能力、抗干扰能力、效能影响（与效能评估指标体系一致）。
            表头与行名可悬停查看各维度释义。
          </p>
          <p class="guide-line">
            <strong>把握度：</strong>填写 0～1 的小数，表示对该比较判断的可信度（参照上方<strong>把握度等级与 λ</strong>表；空白默认 {{ defaultBlankConfidence }}）；将随提交一并发送后端备查。
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
      <div v-if="selectedExpertId" class="matrix-section indicator-layer-section">
        <h3 class="section-title">
          <el-icon><Grid /></el-icon>
          指标层判断矩阵
        </h3>

        <el-alert type="info" :closable="false" class="matrix-guide compact">
          与维度层相同：<strong>上三角</strong>为行相对列的标度 <strong>[1/9，9]</strong>（可小于 1）；下三角自动互反。切换 Tab 编辑各维度下属指标。
        </el-alert>

        <el-tabs v-model="activeIndicatorTab" type="border-card" class="ahp-indicator-tabs">
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
                        :min="AHP_SCORE_MIN"
                        :max="AHP_SCORE_MAX"
                        :step="0.01"
                        :precision="4"
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
                      {{ indicatorMatrix[dim.code][rowIdx][colIdx].toFixed(4) }}
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
          计算AHP权重
        </el-button>

        <el-button size="large" @click="resetToDefault">
          <el-icon><RefreshLeft /></el-icon>
          恢复默认
        </el-button>
      </div>

      <!-- AHP结果展示 -->
      <div v-if="selectedExpertId && result" class="result-section">
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
          >
            <template #default>
              <span style="font-size: 12px; color: #666;">
                CI = {{ result.dimensionResult.ci.toFixed(4) }} | RI = {{ result.dimensionResult.ri.toFixed(2) }} | 一致性阈值 CR &lt; 0.1
              </span>
            </template>
          </el-alert>

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

          <el-collapse class="consistency-calc-collapse">
            <el-collapse-item name="dim-consistency">
              <template #title>
                <span class="consistency-collapse-title">
                  <el-icon><InfoFilled /></el-icon>
                  一致性检验如何计算？为何「差别不大」也可能不通过？
                </span>
              </template>
              <div v-if="dimensionConsistencyDetail" class="consistency-calc-body">
                <p class="consistency-intro">
                  <strong>为何「改动不大」也可能不通过？</strong>一致性检验看的是<strong>传递关系是否自洽</strong>，不是看单个格子变化大不大。例如：若「可靠性相对传输能力」为 3、「传输能力相对抗干扰能力」为 2，则按 Saaty 标度的传递性，「可靠性相对抗干扰能力」应约为 3×2=6 附近；若填成 1（同等重要），就会在数学上偏离完全一致矩阵，使 λ<sub>max</sub> 明显大于阶数 n，从而 CI、CR 偏大。
                </p>
                <p class="consistency-formula-intro">
                  <strong>计算步骤（与后端一致）</strong>：设判断矩阵为 <strong>A</strong>，权重向量为 <strong>w</strong>（已由特征值法/列和归一化求得）。对第 <em>i</em> 行：先算 <strong>(Aw)<sub>i</sub> = Σ<sub>j</sub> a<sub>ij</sub> w<sub>j</sub></strong>，再算 <strong>λ<sub>i</sub> = (Aw)<sub>i</sub> / w<sub>i</sub></strong>；然后 <strong>λ<sub>max</sub> = (λ<sub>1</sub>+…+λ<sub>n</sub>)/n</strong>，<strong>CI = (λ<sub>max</sub>−n)/(n−1)</strong>，<strong>CR = CI / RI</strong>。当 <strong>CR &lt; 0.1</strong> 时认为通过一致性检验。
                </p>
                <div class="consistency-layer-tag">维度层（n = {{ dimensionConsistencyDetail.n }}）</div>
                <el-table :data="dimensionConsistencyDetail.rows" border size="small" class="consistency-step-table">
                  <el-table-column type="index" label="#" width="48" align="center" />
                  <el-table-column prop="name" label="要素" min-width="100" />
                  <el-table-column prop="w" label="权重 wᵢ" align="center" width="110">
                    <template #default="{ row }">{{ row.w.toFixed(6) }}</template>
                  </el-table-column>
                  <el-table-column prop="aw" label="(Aw)ᵢ = Σⱼ aᵢⱼwⱼ" align="center" min-width="140">
                    <template #default="{ row }">{{ row.aw.toFixed(6) }}</template>
                  </el-table-column>
                  <el-table-column prop="lambdaI" label="λᵢ = (Aw)ᵢ/wᵢ" align="center" width="120">
                    <template #default="{ row }">{{ row.lambdaI.toFixed(6) }}</template>
                  </el-table-column>
                </el-table>
                <ul class="consistency-summary">
                  <li>λ<sub>max</sub> = 上表 λ<sub>i</sub> 的算术平均 = <strong>{{ dimensionConsistencyDetail.lambdaMax.toFixed(6) }}</strong>（与接口返回一致；本地验算 {{ dimensionConsistencyDetail.lambdaMaxCalc.toFixed(6) }}）</li>
                  <li>CI = (λ<sub>max</sub> − n) / (n − 1) = <strong>{{ dimensionConsistencyDetail.ci.toFixed(6) }}</strong></li>
                  <li>RI（随机一致性指标，n={{ dimensionConsistencyDetail.n }}）= <strong>{{ dimensionConsistencyDetail.ri.toFixed(4) }}</strong></li>
                  <li>CR = CI / RI = <strong>{{ dimensionConsistencyDetail.cr.toFixed(6) }}</strong>，阈值 CR &lt; 0.1 → {{ dimensionConsistencyDetail.consistent ? '当前通过' : '当前未通过' }}</li>
                </ul>
              </div>
            </el-collapse-item>
          </el-collapse>
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
              </el-table>

              <template v-for="ic in [consistencyDetail(indResult)]" :key="dimName + '-cons'">
                <el-collapse v-if="ic" class="consistency-calc-collapse nested">
                  <el-collapse-item :name="'ind-cc-' + dimName">
                    <template #title>
                      <span class="consistency-collapse-title subtle">
                        <el-icon><InfoFilled /></el-icon>
                        {{ dimName }} — 一致性检验计算过程
                      </span>
                    </template>
                    <div class="consistency-calc-body">
                      <p class="consistency-formula-intro subtle">公式与上方「维度层」相同，下列为<strong>{{ dimName }}</strong>指标层判断矩阵对应的数值。</p>
                      <div class="consistency-layer-tag">指标层 · {{ dimName }}（n = {{ ic.n }}）</div>
                      <el-table :data="ic.rows" border size="small" class="consistency-step-table">
                        <el-table-column type="index" label="#" width="48" align="center" />
                        <el-table-column prop="name" label="要素" min-width="120" />
                        <el-table-column prop="w" label="权重 wᵢ" align="center" width="110">
                          <template #default="{ row }">{{ row.w.toFixed(6) }}</template>
                        </el-table-column>
                        <el-table-column prop="aw" label="(Aw)ᵢ" align="center" min-width="120">
                          <template #default="{ row }">{{ row.aw.toFixed(6) }}</template>
                        </el-table-column>
                        <el-table-column prop="lambdaI" label="λᵢ" align="center" width="110">
                          <template #default="{ row }">{{ row.lambdaI.toFixed(6) }}</template>
                        </el-table-column>
                      </el-table>
                      <ul class="consistency-summary compact">
                        <li>λ<sub>max</sub> = <strong>{{ ic.lambdaMax.toFixed(6) }}</strong>（验算 {{ ic.lambdaMaxCalc.toFixed(6) }}）</li>
                        <li>CI = <strong>{{ ic.ci.toFixed(6) }}</strong>，RI = <strong>{{ ic.ri.toFixed(4) }}</strong>，CR = <strong>{{ ic.cr.toFixed(6) }}</strong></li>
                      </ul>
                    </div>
                  </el-collapse-item>
                </el-collapse>
              </template>
            </el-collapse-item>
          </el-collapse>
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
                <el-table-column prop="dimension" label="维度" width="120" fixed />
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

        <!-- 导出与保存打分 -->
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

    <el-dialog v-model="simulateDialogVisible" title="批量模拟 AHP 打分入库" width="520px" destroy-on-close @open="onSimulateDialogOpen">
      <p class="simulate-dialog-tip">为所选多名专家随机生成把握度（约 20% 小于 0.6）。标度由<strong>随机权重</strong>反推：每层在 [1，9] 上抽样正数 a_i，令比较标度 a_ij = a_i/a_j（等价于先得到和为 1 的权重 w_i ∝ a_i 再取 w_i/w_j），理论上一致性完美（CI=0）；两位小数舍入后仍校验各矩阵 CR &lt; 0.1。数据写入上述两张表。</p>
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
import { Setting, Grid, PieChart, Promotion, RefreshLeft, InfoFilled, Histogram, Download, Document } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import {
  calculateExpertAHP,
  getExpertList,
  getExpertAhpScores,
  getExpertAhpIndividualWeights,
  saveExpertAhpScores,
  simulateExpertAhpScores
} from '@/api'
import {
  dimensions,
  indicatorElements,
  dimensionDescriptions,
  indicatorMetaByDimension,
  indicatorDisplayLabel,
  indicatorShortLabel,
  indicatorTooltipText,
  defaultBlankConfidence,
  AHP_SCORE_MIN,
  AHP_SCORE_MAX,
  ahpScaleFullLegend,
  ahpConfidenceLevelLegend
} from '@/config/ahpComparisons'

function buildDimensionConfidenceMatrix() {
  const n = dimensions.length
  return Array.from({ length: n }, (_, i) =>
    Array.from({ length: n }, (_, j) => (i < j ? defaultBlankConfidence : 0))
  )
}

function buildIndicatorConfidenceMatrices() {
  const matrices = {}
  for (const dim of dimensions) {
    const dimCode = dim.code
    const n = indicatorElements[dimCode].length
    matrices[dimCode] = Array.from({ length: n }, (_, i) =>
      Array.from({ length: n }, (_, j) => (i < j ? defaultBlankConfidence : 0))
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
const combinedSunburstRef = ref(null)
let combinedSunburstChart = null

// 维度层矩阵（5×5）：默认上三角标度均为 1（与无数据时的库表语义一致）
const dimensionMatrix = reactive(
  (() => {
    const n = dimensions.length
    const matrix = []
    for (let i = 0; i < n; i++) {
      const row = []
      for (let j = 0; j < n; j++) {
        if (i === j) row.push(1)
        else if (i < j) row.push(1)
        else row.push(0)
      }
      matrix.push(row)
    }
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

// 指标层矩阵：默认上三角标度均为 1
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
          if (i === j) row.push(1)
          else if (i < j) row.push(1)
          else row.push(0)
        }
        matrix.push(row)
      }
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

// 初始化维度层矩阵（用于重置）：标度全 1，把握度默认低于 0.6
const initDimensionMatrix = () => {
  const n = dimensions.length
  dimensionMatrix.length = 0

  for (let i = 0; i < n; i++) {
    const row = []
    for (let j = 0; j < n; j++) {
      if (i === j) row.push(1)
      else if (i < j) row.push(1)
      else row.push(0)
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
        if (i === j) row.push(1)
        else if (i < j) row.push(1)
        else row.push(0)
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
      Array.from({ length: nc }, (_, j) => (i < j ? defaultBlankConfidence : 0))
    )
  }
}

/** Saaty 正互反标度 [1/9, 9]；>1 行更重要，<1 行不如列重要 */
function clampAhpScore(val) {
  let v = Number(val)
  if (!Number.isFinite(v)) v = 1
  v = Math.max(AHP_SCORE_MIN, Math.min(AHP_SCORE_MAX, v))
  return Math.round(v * 10000) / 10000
}

const expertOptions = ref([])
const selectedExpertId = ref(null)
const loadingScores = ref(false)
const savingScores = ref(false)
const simulateDialogVisible = ref(false)
const simulateExpertIds = ref([])
const simulating = ref(false)
/** 指标层 Tab 须绑定，否则未选 activeName 时内容区可能空白 */
const activeIndicatorTab = ref(dimensions[0].code)

async function fetchExpertOptions() {
  try {
    const list = await getExpertList()
    expertOptions.value = Array.isArray(list) ? list : []
  } catch (e) {
    console.error(e)
    ElMessage.error('加载专家列表失败：' + (e.message || ''))
  }
}

function applyDbRowsToMatrices(rows) {
  initDimensionMatrix()
  initIndicatorMatrix()
  const map = new Map(rows.map((r) => [r.comparisonKey, r]))
  const n = dimensions.length
  for (let i = 0; i < n; i++) {
    for (let j = i + 1; j < n; j++) {
      const key = `${dimensions[i].name}_${dimensions[j].name}`
      const r = map.get(key)
      if (!r) continue
      const sc = clampAhpScore(Number(r.score))
      dimensionMatrix[i][j] = sc
      dimensionMatrix[j][i] = 1 / sc
      const cf = r.confidence != null && r.confidence !== '' ? Number(r.confidence) : defaultBlankConfidence
      dimensionConfidence[i][j] = Math.min(1, Math.max(0, cf))
    }
  }
  for (const dim of dimensions) {
    const dimCode = dim.code
    const elements = indicatorElements[dimCode]
    const matrix = indicatorMatrix[dimCode]
    const conf = indicatorConfidence[dimCode]
    for (let i = 0; i < elements.length; i++) {
      for (let j = i + 1; j < elements.length; j++) {
        const key = `${elements[i]}_${elements[j]}`
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

watch(selectedExpertId, (id) => {
  if (id) activeIndicatorTab.value = dimensions[0].code
})

async function loadStoredWeightsSnapshot() {
  if (!selectedExpertId.value) {
    result.value = null
    disposeCombinedSunburst()
    return
  }
  try {
    const snap = await getExpertAhpIndividualWeights(selectedExpertId.value)
    if (snap && snap.dimensionResult) {
      result.value = snap
      await nextTick()
      renderCombinedSunburst()
    } else {
      result.value = null
      disposeCombinedSunburst()
    }
  } catch (e) {
    result.value = null
    disposeCombinedSunburst()
  }
}

async function loadExpertScoresIntoMatrices() {
  if (!selectedExpertId.value) {
    initDimensionMatrix()
    initIndicatorMatrix()
    result.value = null
    return
  }
  loadingScores.value = true
  try {
    const rows = await getExpertAhpScores(selectedExpertId.value)
    if (!rows || rows.length === 0) {
      initDimensionMatrix()
      initIndicatorMatrix()
      result.value = null
      disposeCombinedSunburst()
    } else {
      applyDbRowsToMatrices(rows)
      await loadStoredWeightsSnapshot()
    }
  } catch (e) {
    ElMessage.error('加载失败：' + (e.message || ''))
  } finally {
    loadingScores.value = false
  }
}

async function onExpertChange() {
  await loadExpertScoresIntoMatrices()
}

async function reloadScoresFromDb() {
  await loadExpertScoresIntoMatrices()
}

function buildPersistPayload() {
  const dimensionEntries = []
  for (let i = 0; i < dimensions.length; i++) {
    for (let j = i + 1; j < dimensions.length; j++) {
      dimensionEntries.push({
        key: `${dimensions[i].name}_${dimensions[j].name}`,
        score: dimensionMatrix[i][j],
        confidence: dimensionConfidence[i][j] ?? defaultBlankConfidence
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
          confidence: indicatorConfidence[dimCode][i][j] ?? defaultBlankConfidence
        })
      }
    }
    indicatorEntries[dimCode] = entries
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
    const n = await saveExpertAhpScores(buildPersistPayload())
    ElMessage.success(`已保存 ${n} 条比较打分，并已更新权重快照`)
    await loadStoredWeightsSnapshot()
  } catch (e) {
    ElMessage.error('保存失败：' + (e.message || ''))
  } finally {
    savingScores.value = false
  }
}

function openSimulateDialog() {
  simulateDialogVisible.value = true
}

function onSimulateDialogOpen() {
  if (simulateExpertIds.value.length === 0 && selectedExpertId.value) {
    simulateExpertIds.value = [selectedExpertId.value]
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
    ElMessage.success(`已写入 ${res.insertedCount} 名专家的模拟打分，跳过 ${res.skippedCount} 名`)
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

onMounted(() => {
  fetchExpertOptions()
  window.addEventListener('resize', onAhpChartsResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', onAhpChartsResize)
  disposeCombinedSunburst()
})

function onAhpChartsResize() {
  combinedSunburstChart?.resize()
}

watch(
  () => [selectedExpertId.value, result.value],
  () => {
    if (!selectedExpertId.value || !result.value) {
      disposeCombinedSunburst()
    }
  }
)

// 维度矩阵单元格变化时自动更新下三角倒数（Saaty 标度 [1/9,9]）
const onDimensionCellChange = (row, col, val) => {
  const v = clampAhpScore(val)
  dimensionMatrix[row][col] = v
  dimensionMatrix[col][row] = 1 / v
}

const onIndicatorCellChange = (dimCode, row, col, val) => {
  const v = clampAhpScore(val)
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

/**
 * 根据后端返回的判断矩阵 A、权重 w 推导一致性检验中间步骤（与 ExpertAHPService.calculateLambdaMax 一致）
 */
function consistencyDetail(mr) {
  if (!mr?.matrix?.length || !mr?.weights?.length || !mr?.elementNames?.length) return null
  const n = mr.weights.length
  if (mr.matrix.length !== n || mr.elementNames.length !== n) return null
  const rows = []
  let sumLambda = 0
  for (let i = 0; i < n; i++) {
    let aw = 0
    for (let j = 0; j < n; j++) {
      aw += Number(mr.matrix[i][j]) * Number(mr.weights[j])
    }
    const wi = Number(mr.weights[i])
    const lambdaI = wi > 1e-15 ? aw / wi : 0
    sumLambda += lambdaI
    rows.push({ name: String(mr.elementNames[i]), w: wi, aw, lambdaI })
  }
  return {
    rows,
    n,
    lambdaMax: Number(mr.lambdaMax),
    lambdaMaxCalc: sumLambda / n,
    ci: Number(mr.ci),
    ri: Number(mr.ri),
    cr: Number(mr.cr),
    consistent: Boolean(mr.consistent)
  }
}

const dimensionConsistencyDetail = computed(() =>
  result.value?.dimensionResult ? consistencyDetail(result.value.dimensionResult) : null
)

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
        indicatorKey: indName,
        indicator: indicatorDisplayLabel(dimName, indName),
        indWeight,
        combinedWeight
      })
    }
  }

  return table
})

const SUNBURST_DIM_COLORS = ['#0074D9', '#39CCCC', '#3D9970', '#FF851B', '#FFDC00']

function buildCombinedSunburstData() {
  if (!result.value?.dimensionResult?.elementNames?.length) return null
  const byDim = new Map()
  for (const row of combinedWeightTable.value) {
    if (!byDim.has(row.dimension)) byDim.set(row.dimension, [])
    byDim.get(row.dimension).push({
      name: indicatorShortLabel(row.dimension, row.indicatorKey),
      value: Math.max(1e-6, row.combinedWeight * 100),
      indicatorTooltipLine: indicatorTooltipText(row.dimension, row.indicatorKey)
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
        const dimName = p.treePathInfo[1]?.name
        const tipLine = p.data?.indicatorTooltipLine
        if (p.treePathInfo.length >= 3 && tipLine && dimName) {
          return `<div style="max-width:280px;line-height:1.6"><strong>${dimName}</strong> · ${tipLine}<br/>占全体：<b>${pct}</b></div>`
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
          {
            r0: '0%',
            r: '12%',
            label: { show: false },
            itemStyle: { color: '#001f3f', borderWidth: 0 }
          },
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
          {
            r0: '34%',
            r: '90%',
            label: { show: false },
            itemStyle: { borderWidth: 1 }
          }
        ]
      }
    ]
  })
}

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
          confidence: dimensionConfidence[i][j] ?? defaultBlankConfidence
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
            confidence: indicatorConfidence[dimCode][i][j] ?? defaultBlankConfidence
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
    renderCombinedSunburst()

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

// 恢复默认
const resetToDefault = () => {
  initDimensionMatrix()
  initIndicatorMatrix()
  result.value = null
  disposeCombinedSunburst()
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
        confidence: dimensionConfidence[i][j] ?? defaultBlankConfidence
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
          confidence: indicatorConfidence[dimCode][i][j] ?? defaultBlankConfidence
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
  .ahp-select-expert-empty {
    padding: 28px 16px 8px;
  }

  .expert-ahp-hint {
    margin-bottom: 16px;
  }

  .simulate-dialog-tip {
    margin: 0 0 14px;
    font-size: 13px;
    line-height: 1.55;
    color: #606266;
  }

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
    grid-template-columns: minmax(72px, 1fr) min-content 58px;
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

    .result-footer-actions {
      margin-top: 8px;
    }
  }

  .consistency-calc-collapse {
    margin-top: 14px;

    &.nested {
      margin-top: 12px;
    }

    :deep(.el-collapse-item__header) {
      background: #f0f4f8;
      font-weight: 500;
      padding-left: 12px;
    }

    :deep(.el-collapse-item__content) {
      padding: 12px 14px 16px;
    }
  }

  .consistency-collapse-title {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    color: var(--navy-primary);

    &.subtle {
      font-size: 13px;
      font-weight: normal;
    }
  }

  .consistency-calc-body {
    .consistency-intro,
    .consistency-formula-intro {
      margin: 0 0 12px;
      font-size: 13px;
      line-height: 1.65;
      color: #606266;

      &.subtle {
        font-size: 12px;
        color: #909399;
      }
    }

    .consistency-layer-tag {
      margin-bottom: 8px;
      font-size: 12px;
      font-weight: 600;
      color: var(--navy-primary);
    }

    .consistency-step-table {
      margin-bottom: 10px;
    }

    .consistency-summary {
      margin: 0;
      padding-left: 18px;
      font-size: 13px;
      line-height: 1.75;
      color: #303133;

      &.compact {
        font-size: 12px;
        line-height: 1.6;
      }

      li {
        margin-bottom: 4px;
      }
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
