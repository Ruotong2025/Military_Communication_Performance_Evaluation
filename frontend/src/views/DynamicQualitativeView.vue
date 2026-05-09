<template>
  <div class="dynamic-qualitative-view">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>
        <el-icon><User /></el-icon>
        专家定性指标评估
      </h2>
      <p class="subtitle">多个专家对作战的定性指标进行评估，采用质心式加权平均进行集结</p>
    </div>

    <!-- 顶部工具栏 -->
    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-select
            v-model="selectedTemplateId"
            placeholder="请选择指标模板"
            style="width: 240px"
            @change="handleTemplateChange"
          >
            <el-option
              v-for="tpl in templates"
              :key="tpl.id"
              :label="tpl.templateName"
              :value="tpl.id"
            >
              <span>{{ tpl.templateName }}</span>
              <span class="template-stats">{{ tpl.secondaryCount || 0 }}指标</span>
            </el-option>
          </el-select>

          <el-button
            type="primary"
            :icon="Refresh"
            :loading="rendering"
            :disabled="!selectedTemplateId"
            @click="handleRender"
          >
            渲染
          </el-button>
        </div>

        <div class="toolbar-right" v-if="hasRendered">
          <el-tag type="success" size="large">
            当前模板: {{ selectedTemplateName }}
          </el-tag>
        </div>
      </div>
    </el-card>

    <!-- 评估配置栏 -->
    <el-card v-if="hasRendered" class="config-card" shadow="never">
      <template #header>
        <div class="config-header">
          <span>评估配置</span>
          <el-button text @click="configCollapsed = !configCollapsed">
            {{ configCollapsed ? '展开' : '收起' }}
            <el-icon>
              <ArrowUp v-if="!configCollapsed" />
              <ArrowDown v-else />
            </el-icon>
          </el-button>
        </div>
      </template>

      <div v-show="!configCollapsed" class="config-content">
        <el-row :gutter="20">
          <!-- 批次选择 -->
          <el-col :span="8">
            <div class="config-item">
              <label>评估批次:</label>
              <el-select
                v-model="selectedBatchId"
                placeholder="选择批次"
                clearable
                style="width: 100%"
                @change="handleBatchChange"
              >
                <el-option
                  v-for="batch in batches"
                  :key="batch.batch_id"
                  :label="batch.batch_id"
                  :value="batch.batch_id"
                >
                  <div class="batch-option">
                    <span class="batch-id">{{ batch.batch_id }}</span>
                    <span class="batch-count">{{ batch.operationCount }}作战</span>
                  </div>
                </el-option>
              </el-select>
            </div>
          </el-col>

          <!-- 专家选择（用于查看评估结果） -->
          <el-col :span="8">
            <div class="config-item">
              <label>查看专家:</label>
              <el-select
                v-model="selectedExpertId"
                placeholder="选择专家查看结果"
                clearable
                style="width: 100%"
                @change="handleExpertChange"
              >
                <el-option
                  v-for="expert in availableExperts"
                  :key="expert.expert_id"
                  :label="expert.expert_name"
                  :value="expert.expert_id"
                />
              </el-select>
            </div>
          </el-col>

          <!-- 操作按钮 -->
          <el-col :span="8">
            <div class="config-item config-actions">
              <el-button
                type="primary"
                :disabled="!selectedBatchId || availableExperts.length === 0 || isEvaluating"
                :loading="isEvaluating"
                @click="startBatchEvaluation"
              >
                {{ isEvaluating ? '评估中...' : '批量评估' }}
              </el-button>
            </div>
          </el-col>
        </el-row>

        <!-- 评估进度 -->
        <div class="progress-section" v-if="selectedBatchId && availableExperts.length > 0">
          <el-divider />
          <div class="progress-info">
            <span class="progress-label">评估进度:</span>
            <el-progress
              :percentage="progress.percentage"
              :color="progressColors"
              style="width: 200px"
            />
            <span class="progress-text">{{ progress.completed }}/{{ progress.total }}</span>
            <span class="progress-hint" v-if="progress.percentage < 100">
              （还需评估 {{ progress.total - progress.completed }} 项）
            </span>
            <span class="progress-hint" v-else style="color: #67c23a">
              （评估完成，可执行集结）
            </span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 评估表格区域 -->
    <el-card v-if="hasRendered && levels.length > 0" class="table-card" shadow="never">
      <template #header>
        <div class="table-header">
          <div class="header-left">
            <el-icon><Document /></el-icon>
            <span>定性指标评估表</span>
            <el-tag v-if="operations.length > 0" type="info" size="small">
              {{ operations.length }} 个作战
            </el-tag>
            <el-tag v-if="selectedExperts.length > 0" type="warning" size="small">
              {{ selectedExperts.length }} 位专家
            </el-tag>
          </div>
          <div class="header-actions">
            <el-button
              type="success"
              :icon="DataLine"
              :disabled="!canAggregate"
              @click="showAggregateDialog = true"
            >
              集结计算
            </el-button>
          </div>
        </div>
      </template>

      <!-- 层级Tab -->
      <el-tabs v-model="activeLevel" type="border-card" class="level-tabs" @tab-change="handleLevelChange">
        <el-tab-pane
          v-for="level in levels"
          :key="level.levelName"
          :label="level.levelName"
          :name="level.levelName"
        >
          <!-- 多级表头表格 -->
          <el-table
            :data="tableData"
            border
            stripe
            max-height="600"
            size="small"
          >
            <!-- 作战ID列 -->
            <el-table-column
              prop="operationId"
              label="作战ID"
              width="120"
              fixed="left"
              align="center"
              header-align="center"
            >
              <template #default="{ row }">
                <el-tag type="primary" size="small">{{ row.operationId }}</el-tag>
              </template>
            </el-table-column>

            <!-- 动态多级表头：一级维度 + 二级指标 -->
            <template v-for="(primary, pIdx) in level.primaries" :key="'primary-' + pIdx">
              <el-table-column
                :label="primary.primaryName"
                align="center"
                header-align="center"
                :colspan="primary.secondaries.length"
              >
                <el-table-column
                  v-for="(sec, sIdx) in primary.secondaries"
                  :key="'sec-' + pIdx + '-' + sIdx"
                  :label="sec.name"
                  align="center"
                  header-align="center"
                  min-width="150"
                >
                  <template #header>
                    <div class="sec-header">
                      <span>{{ sec.name }}</span>
                      <div class="header-tags">
                        <el-tag
                          size="small"
                          :type="sec.direction === 'POSITIVE' ? 'success' : 'warning'"
                        >
                          {{ sec.direction === 'POSITIVE' ? '正向' : '反向' }}
                        </el-tag>
                      </div>
                    </div>
                  </template>

                  <!-- 单专家单元格 -->
                  <template #default="{ row }">
                    <div
                      v-if="selectedExpertId"
                      class="expert-score-item"
                      @click="handleCellClick(row, sec, selectedExperts[0])"
                    >
                      <div class="score-display">
                        <template v-if="getCellScore(row.operationId, sec.code, selectedExpertId)">
                          <el-tag
                            size="small"
                            :type="getScoreTagType(getCellScore(row.operationId, sec.code, selectedExpertId).level)"
                          >
                            {{ getCellScore(row.operationId, sec.code, selectedExpertId).level }}
                          </el-tag>
                          <div class="confidence-display">
                            把握: {{ getCellScore(row.operationId, sec.code, selectedExpertId).confidenceValue }}%
                          </div>
                        </template>
                        <template v-else>
                          <span class="empty-score">—</span>
                          <div class="empty-confidence">未评分</div>
                        </template>
                      </div>
                    </div>
                    <div v-else class="empty-expert-hint">
                      请选择专家
                    </div>
                  </template>
                </el-table-column>
              </el-table-column>
            </template>
          </el-table>

          <!-- 空状态 -->
          <div v-if="tableData.length === 0" class="empty-table-tip">
            <el-empty description="暂无数据，请先选择批次或添加作战数据">
              <template #image>
                <el-icon :size="60" style="color: #c0c4cc"><Document /></el-icon>
              </template>
            </el-empty>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- ==================== 集结结果展示区域 ==================== -->
    <el-card v-if="showAggregationResult" class="aggregation-result-card" shadow="never">
      <template #header>
        <div class="aggregation-header">
          <div class="header-left">
            <el-icon><DataLine /></el-icon>
            <span>集结结果</span>
            <el-tag type="success" size="small">
              {{ aggregationResult.operationIds?.length || 0 }} 个作战
            </el-tag>
            <el-tag type="info" size="small">
              {{ aggregationResult.aggregationResults?.length || 0 }} 个指标
            </el-tag>
            <el-tag type="warning" size="small">
              {{ levelTabList.length }} 个层级
            </el-tag>
          </div>
          <div class="header-actions">
            <el-button size="small" @click="showAggregationResult = false">
              收起
            </el-button>
          </div>
        </div>
      </template>

      <!-- 权重信息 -->
      <div class="weight-info" v-if="aggregationResult.weights">
        <span>集结权重（已归一化）：w_α = {{ aggregationResult.weights.wAlpha?.toFixed(4) }}，w_λ = {{ aggregationResult.weights.wLambda?.toFixed(4) }}</span>
        <span class="weight-raw" v-if="aggregationResult.weightsInput">
          （输入比例 {{ aggregationResult.weightsInput.wAlpha?.toFixed(2) }} : {{ aggregationResult.weightsInput.wLambda?.toFixed(2) }}）
        </span>
      </div>

      <!-- 作战ID筛选 -->
      <div class="operation-filter" v-if="aggregationResult.operationIds?.length > 0">
        <span class="filter-label">作战筛选：</span>
        <el-select v-model="selectedOperationFilter" placeholder="全部作战" clearable size="small" style="width: 200px">
          <el-option
            v-for="opId in aggregationResult.operationIds"
            :key="opId"
            :label="opId"
            :value="opId"
          />
        </el-select>
      </div>

      <!-- 层级Tab -->
      <el-tabs v-model="activeAggregationLevel" type="border-card" class="level-tabs" v-if="levelTabList.length > 0">
        <el-tab-pane
          v-for="level in levelTabList"
          :key="level.levelName"
          :label="`${level.levelName} (${getFilteredResultsByLevel(level.levelName).length})`"
          :name="level.levelName"
        >
          <!-- 质心主表（按作战ID分组展示） -->
          <div class="centroid-table">
            <div class="section-title">群体结论质心 x*（加权平均分）</div>
            <el-table
              :data="getFilteredResultsByLevel(level.levelName)"
              border
              stripe
              size="small"
              max-height="500"
              style="width: 100%"
            >
              <el-table-column prop="operationId" label="作战ID" width="100" fixed>
                <template #default="{ row }">
                  <el-tag type="primary" size="small">{{ row.operationId }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="indicatorName" label="指标" min-width="160">
                <template #default="{ row }">
                  <span class="indicator-cell">{{ row.indicatorName }}</span>
                </template>
              </el-table-column>
              <el-table-column label="质心分 x*" min-width="100" align="center">
                <template #default="{ row }">
                  <span v-if="row.xStar != null" class="xstar-val">{{ row.xStar.toFixed(2) }}</span>
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
                  {{ row.gradeRange ? `[${row.gradeRange[0]}, ${row.gradeRange[1]})` : '—' }}
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
                    :type="aggDetailExpanded[row.indicatorCode + '_' + row.operationId] ? 'primary' : 'default'"
                    @click="toggleDetail(row.indicatorCode, row.operationId)"
                  >明细</el-button>
                  <el-button
                    size="small"
                    :type="aggDetailExpanded['__chart_' + row.indicatorCode + '_' + row.operationId] ? 'primary' : 'default'"
                    @click="toggleChart(row.indicatorCode, row.operationId)"
                  >质心图</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 明细展开（各专家中间量） -->
          <template v-for="row in getFilteredResultsByLevel(level.levelName)" :key="'detail-' + row.indicatorCode + '_' + row.operationId">
            <div v-if="aggDetailExpanded[row.indicatorCode + '_' + row.operationId]" class="agg-detail-block">
              <div class="detail-head">
                <span class="detail-head-title">计算明细 — {{ row.operationId }} · {{ row.indicatorName }}</span>
                <span class="formula-hint">γ = w_α·(α/100) + w_λ·λ，x* = Σγ·中点 / Σγ</span>
              </div>
              <el-table :data="row.details" border stripe size="small" max-height="320" style="width: 100%">
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
                  <template #default="{ row: dr }">{{ dr.lambda01?.toFixed(4) }}</template>
                </el-table-column>
                <el-table-column label="γ 综合可信度" min-width="112" align="center">
                  <template #default="{ row: dr }">
                    <strong style="color:#1a3a5c;">{{ dr.gamma?.toFixed(4) }}</strong>
                  </template>
                </el-table-column>
                <el-table-column label="等级" min-width="76" align="center">
                  <template #default="{ row: dr }">
                    <el-tag size="small" :type="getLevelTagType(row.category)">{{ dr.gradeCode }}</el-tag>
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
                <el-table-column label="γ·中点" min-width="120" align="center">
                  <template #default="{ row: dr }">{{ dr.gammaTimesMidpoint?.toFixed(4) }}</template>
                </el-table-column>
              </el-table>
            </div>
          </template>

          <!-- 质心图展开 -->
          <template v-for="row in getFilteredResultsByLevel(level.levelName)" :key="'chart-' + row.indicatorCode + '_' + row.operationId">
            <div v-if="aggDetailExpanded['__chart_' + row.indicatorCode + '_' + row.operationId]" class="agg-chart-block">
              <div class="detail-head">
                <span class="detail-head-title">加权覆盖函数 — {{ row.operationId }} · {{ row.indicatorName }}</span>
                <span class="formula-hint">P̄(x)=Σ γ_k·p_k(x)</span>
              </div>
              <div :id="'ql-agg-chart-' + row.indicatorCode + '-' + row.operationId" class="agg-chart-px" />
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
        </el-tab-pane>
      </el-tabs>

      <!-- 如果只有一个层级，也显示一个总览表（兼容旧逻辑） -->
      <div v-if="levelTabList.length <= 1 && aggregationResult.aggregationResults?.length > 0" class="centroid-table">
        <div class="section-title">群体结论质心 x*（加权平均分）</div>
        <el-table
          :data="filteredAggregationResults"
          border
          stripe
          size="small"
          max-height="500"
          style="width: 100%"
        >
          <el-table-column prop="operationId" label="作战ID" width="100" fixed>
            <template #default="{ row }">
              <el-tag type="primary" size="small">{{ row.operationId }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="indicatorName" label="指标" min-width="160">
            <template #default="{ row }">
              <span class="indicator-cell">{{ row.indicatorName }}</span>
            </template>
          </el-table-column>
          <el-table-column label="质心分 x*" min-width="100" align="center">
            <template #default="{ row }">
              <span v-if="row.xStar != null" class="xstar-val">{{ row.xStar.toFixed(2) }}</span>
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
              {{ row.gradeRange ? `[${row.gradeRange[0]}, ${row.gradeRange[1]})` : '—' }}
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
                :type="aggDetailExpanded[row.indicatorCode + '_' + row.operationId] ? 'primary' : 'default'"
                @click="toggleDetail(row.indicatorCode, row.operationId)"
              >明细</el-button>
              <el-button
                size="small"
                :type="aggDetailExpanded['__chart_' + row.indicatorCode + '_' + row.operationId] ? 'primary' : 'default'"
                @click="toggleChart(row.indicatorCode, row.operationId)"
              >质心图</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- 空状态 -->
    <el-empty
      v-if="!hasRendered"
      description="请先选择指标模板，然后点击「渲染」按钮"
    >
      <template #image>
        <el-icon :size="80" style="color: #c0c4cc"><User /></el-icon>
      </template>
    </el-empty>

    <!-- 评分对话框 -->
    <el-dialog v-model="showScoreDialog" title="定性指标评分" width="950px">
      <el-form label-width="80px">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="指标名称">
              <span class="indicator-name">{{ currentCell?.sec?.name }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="作战ID">
              <el-tag type="primary">{{ currentCell?.row?.operationId }}</el-tag>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="评估专家">
              <span>{{ currentCell?.expert?.expert_name }}</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">等级打分表</el-divider>
        <div class="level-table-wrapper">
          <el-table :data="scoreLevels" border size="small" max-height="200">
            <el-table-column prop="code" label="等级代码" width="70" align="center">
              <template #default="{ row }">
                <el-tag :type="getLevelTagType(row.category)" size="small">{{ row.code }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="名称" width="70" align="center" />
            <el-table-column prop="category" label="大类" width="70" align="center" />
            <el-table-column prop="range" label="分数区间" width="100" align="center" />
            <el-table-column prop="description" label="等级说明" min-width="300" />
          </el-table>
        </div>

        <el-divider content-position="left">选择等级</el-divider>
        <el-form-item label="等级">
          <el-select v-model="selectedScoreLevel" placeholder="请选择等级" style="width: 200px">
            <el-option
              v-for="level in scoreLevels"
              :key="level.code"
              :label="`${level.code} - ${level.name}`"
              :value="level.code"
            >
              <span style="float: left">{{ level.code }}</span>
              <span style="float: right; color: #8492a6; font-size: 13px">{{ level.name }} ({{ level.range }})</span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-divider content-position="left">把握度标准</el-divider>
        <div class="confidence-table-wrapper">
          <el-table :data="confidenceLevels" border size="small">
            <el-table-column prop="level" label="等级" width="80" align="center" />
            <el-table-column prop="range" label="建议百分区间" width="120" align="center" />
            <el-table-column prop="lambda" label="参照λ" width="80" align="center" />
            <el-table-column prop="name" label="含义" width="100" align="center" />
            <el-table-column prop="description" label="说明" min-width="200" />
          </el-table>
        </div>

        <el-divider content-position="left">填写把握度</el-divider>
        <el-form-item label="把握度(%)">
          <el-input-number
            v-model="selectedConfidence"
            :min="0"
            :max="100"
            :precision="0"
            controls-position="right"
            style="width: 200px"
          />
          <span class="confidence-level" style="margin-left: 15px">({{ getConfidenceLevelName(selectedConfidence) }})</span>
        </el-form-item>

        <el-form-item label="备注说明">
          <el-input
            v-model="scoreRemark"
            type="textarea"
            :rows="2"
            placeholder="请输入评估备注（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showScoreDialog = false">取消</el-button>
        <el-button type="primary" @click="handleScoreSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量评估对话框 -->
    <el-dialog v-model="showBatchEvaluateDialog" title="批量评估" width="500px">
      <el-form label-width="100px">
        <el-form-item label="评估信息">
          <div class="batch-info">
            <p>作战数量: <strong>{{ operations.length }}</strong></p>
            <p>专家数量: <strong>{{ availableExperts.length }}</strong></p>
            <p>指标数量: <strong>{{ indicatorCount }}</strong></p>
            <p>总计评估项: <strong>{{ operations.length * availableExperts.length * indicatorCount }}</strong></p>
          </div>
        </el-form-item>

        <el-alert
          title="批量评估说明"
          type="info"
          :closable="false"
          style="margin-bottom: 15px"
        >
          <p>将对全部 <strong>{{ availableExperts.length }}</strong> 位专家的
             <strong>{{ operations.length }}</strong> 个作战进行批量评估，
             自动生成带有随机性的定性评分和把握度。</p>
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="showBatchEvaluateDialog = false">取消</el-button>
        <el-button type="primary" :loading="batchEvaluating" @click="executeBatchEvaluation">
          确认批量评估
        </el-button>
      </template>
    </el-dialog>

    <!-- 集结计算对话框 -->
    <el-dialog v-model="showAggregateDialog" title="集结计算" width="500px">
      <el-form label-width="120px">
        <el-alert
          title="集结说明"
          type="info"
          :closable="false"
          style="margin-bottom: 15px"
        >
          <template #default>
            <p>将对 <strong>{{ operations.length }}</strong> 个作战、<strong>{{ availableExperts.length }}</strong> 位专家的评分进行集结计算。</p>
            <p>每个作战ID的每个指标将单独进行集结。</p>
          </template>
        </el-alert>

        <el-divider content-position="left">权重配置</el-divider>

        <el-form-item label="权威度权重(α)">
          <div class="weight-slider">
            <el-slider
              v-model="aggregateWeightAlpha"
              :min="0"
              :max="100"
              :format-tooltip="(val) => (val / 100).toFixed(2)"
            />
            <span class="weight-value">{{ (aggregateWeightAlpha / 100).toFixed(2) }}</span>
          </div>
        </el-form-item>

        <el-form-item label="把握度权重(λ)">
          <div class="weight-slider">
            <el-slider
              v-model="aggregateWeightLambda"
              :min="0"
              :max="100"
              :format-tooltip="(val) => (val / 100).toFixed(2)"
            />
            <span class="weight-value">{{ (aggregateWeightLambda / 100).toFixed(2) }}</span>
          </div>
        </el-form-item>

        <el-alert
          title="集结公式说明"
          type="info"
          :closable="false"
          style="margin-top: 10px"
        >
          <template #default>
            <p>采用质心式加权平均公式:</p>
            <p style="font-family: monospace">γ = w_α·(α/100) + w_λ·λ</p>
            <p style="font-family: monospace">x* = Σγ·中点 / Σγ</p>
            <p class="formula-desc">质心式加权平均</p>
          </template>
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="showAggregateDialog = false">取消</el-button>
        <el-button type="success" :loading="aggregating" @click="handleAggregate">
          执行集结
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Refresh, Document, DataLine, Plus, ArrowUp, ArrowDown } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  getIndicatorTemplates,
  getDynamicQlIndicators,
  getDynamicQlBatches,
  getDynamicQlOperations,
  getDynamicQlExperts,
  getDynamicQlTableData,
  getDynamicQlReference,
  saveDynamicQlRecord,
  aggregateDynamicQlScores,
} from '@/api'

// 状态
const templates = ref([])
const selectedTemplateId = ref(null)
const selectedTemplateName = ref('')
const rendering = ref(false)
const hasRendered = ref(false)

// 配置
const configCollapsed = ref(false)
const batches = ref([])
const selectedBatchId = ref(null)
const operations = ref([])
const availableExperts = ref([])
const selectedExperts = ref([])
const selectedExpertId = ref(null) // 当前选中的专家

// 表格数据
const levels = ref([])
const tableData = ref([])
const savedRecords = ref({}) // { operationId -> { expertId -> { code -> score } } }
const activeLevel = ref('')

// 评分相关
const showScoreDialog = ref(false)
const currentCell = ref(null)
const selectedScoreLevel = ref('A')
const selectedConfidence = ref(80) // 百分数 0-100
const scoreRemark = ref('')

// 等级打分表数据（15级）
const scoreLevels = [
  { code: 'A+', name: '优+', category: '优', range: '[95,100]', description: '优秀+：表现极其出色，超出预期' },
  { code: 'A', name: '优', category: '优', range: '[90,95)', description: '优秀：表现出色，达到优秀标准' },
  { code: 'A-', name: '优-', category: '优', range: '[85,90)', description: '优秀-：表现良好，接近优秀标准' },
  { code: 'B+', name: '良+', category: '良', range: '[80,85)', description: '良好+：表现较好，超出良好标准' },
  { code: 'B', name: '良', category: '良', range: '[75,80)', description: '良好：表现良好，达到良好标准' },
  { code: 'B-', name: '良-', category: '良', range: '[70,75)', description: '良好-：表现尚可，接近良好标准' },
  { code: 'C+', name: '合格+', category: '合格', range: '[65,70)', description: '合格+：基本达标，略高于合格线' },
  { code: 'C', name: '合格', category: '合格', range: '[60,65)', description: '合格：达到基本要求，刚好合格' },
  { code: 'C-', name: '合格-', category: '合格', range: '[55,60)', description: '合格-：勉强合格，需要改进' },
  { code: 'D+', name: '差+', category: '差', range: '[47,55)', description: '差+：表现较差，明显低于标准' },
  { code: 'D', name: '差', category: '差', range: '[39,47)', description: '差：表现差，远低于标准' },
  { code: 'D-', name: '差-', category: '差', range: '[30,39)', description: '差-：表现很差，严重不达标' },
  { code: 'E+', name: '极差+', category: '极差', range: '[20,30)', description: '极差+：表现极差，几乎无法接受' },
  { code: 'E', name: '极差', category: '极差', range: '[10,20)', description: '极差：表现极其糟糕，完全不可接受' },
  { code: 'E-', name: '极差-', category: '极差', range: '[0,10)', description: '极差-：表现最差，毫无价值' },
]

// 把握度等级数据
const confidenceLevels = [
  { level: '1级', range: '[80, 100]', lambda: '1', name: '完全确认', description: '对指标非常熟悉、依据充分，判断完全可信' },
  { level: '2级', range: '[60, 80)', lambda: '0.8', name: '确认', description: '对指标较熟悉，判断可信；与矩阵把握度 λ≈0.8 相当' },
  { level: '3级', range: '[40, 60)', lambda: '0.6', name: '基本确认', description: '了解有限，判断有一定可信度但把握一般；与 λ≈0.6 相当' },
  { level: '4级', range: '[0, 40)', lambda: '< 0.6', name: '不能确认', description: '依据不足或指标不熟，不宜高报把握度；建议补充材料或酌情降低百分值' },
]

// 获取把握度等级名称
const getConfidenceLevelName = (value) => {
  if (value >= 80) return '完全确认'
  if (value >= 60) return '确认'
  if (value >= 40) return '基本确认'
  return '不能确认'
}

// 集结相关
const showAggregateDialog = ref(false)
const aggregateWeightAlpha = ref(50)
const aggregateWeightLambda = ref(50)
const aggregating = ref(false)

// 集结结果筛选
const selectedOperationFilter = ref('')

// 集结结果展示
const showAggregationResult = ref(false)
const aggregationResult = ref({})
const aggDetailExpanded = ref({})
const aggCharts = ref({})
const aggChartReady = ref({})
const activeAggregationLevel = ref('') // 集结结果Tab选中的层级

// 层级Tab列表（从集结结果中提取）
const levelTabList = computed(() => {
  if (!aggregationResult.value || !aggregationResult.value.levelInfos) {
    return []
  }
  return aggregationResult.value.levelInfos
})

// 获取指定层级的集结结果
const getAggregationByLevel = (levelName) => {
  if (!aggregationResult.value || !aggregationResult.value.resultsByLevel) {
    return []
  }
  return aggregationResult.value.resultsByLevel[levelName] || []
}

// 根据作战筛选过滤集结结果
const filteredAggregationResults = computed(() => {
  const results = aggregationResult.value?.aggregationResults || []
  if (!selectedOperationFilter.value) {
    return results
  }
  return results.filter(r => r.operationId === selectedOperationFilter.value)
})

// 根据层级和作战筛选获取结果
const getFilteredResultsByLevel = (levelName) => {
  const levelResults = getAggregationByLevel(levelName)
  if (!selectedOperationFilter.value) {
    return levelResults
  }
  return levelResults.filter(r => r.operationId === selectedOperationFilter.value)
}

// 批量评估相关
const showBatchEvaluateDialog = ref(false)
const batchEvaluating = ref(false)
const isEvaluating = ref(false)

// 进度
const progress = ref({ total: 0, completed: 0, percentage: 0 })

// 进度颜色
const progressColors = [
  { color: '#f56c6c', percentage: 25 },
  { color: '#e6a23c', percentage: 50 },
  { color: '#409eff', percentage: 75 },
  { color: '#67c23a', percentage: 100 },
]

// 计算属性
const canAggregate = computed(() => {
  if (!selectedBatchId.value || !hasRendered.value) return false
  if (operations.value.length === 0) return false
  if (availableExperts.value.length === 0) return false

  // 检查所有可用专家、所有指标是否都已评估
  const levelData = levels.value.find(l => l.levelName === activeLevel.value)
  if (!levelData) return false

  const indicatorCount = countIndicators(levelData)
  if (indicatorCount === 0) return false

  // 检查每个专家是否都评估了所有作战的所有指标
  for (const expert of availableExperts.value) {
    for (const op of operations.value) {
      const opId = typeof op === 'string' ? op : (op.operationId || op)
      const expId = String(expert.expert_id)

      // 检查该专家对该作战的评估数量
      const expOpRecords = savedRecords.value[opId]?.[expId]
      if (!expOpRecords || Object.keys(expOpRecords).length < indicatorCount) {
        return false
      }
    }
  }

  return true
})

// 加载模板列表
const loadTemplates = async () => {
  try {
    const res = await getIndicatorTemplates()
    templates.value = res || []
  } catch (e) {
    console.error('加载模板失败:', e)
  }
}

// 模板选择变化
const handleTemplateChange = async (templateId) => {
  const template = templates.value.find(t => t.id === templateId)
  selectedTemplateName.value = template?.templateName || ''
  hasRendered.value = false
  selectedBatchId.value = null
  selectedExpertId.value = null
  selectedExperts.value = []
  levels.value = []
  tableData.value = []
}

// 渲染表格
const handleRender = async () => {
  if (!selectedTemplateId.value) {
    ElMessage.warning('请先选择指标模板')
    return
  }

  rendering.value = true
  try {
    // 获取定性指标结构
    const res = await getDynamicQlIndicators(selectedTemplateId.value)
    if (res && res.levels) {
      // 过滤掉没有一级维度或二级指标的层级
      const filteredLevels = res.levels.filter(level => {
        return level.primaries && level.primaries.length > 0 &&
               level.primaries.some(primary => primary.secondaries && primary.secondaries.length > 0)
      })

      if (filteredLevels.length === 0) {
        ElMessage.warning('该模板没有定性指标')
        levels.value = []
        hasRendered.value = false
        rendering.value = false
        return
      }

      levels.value = filteredLevels
      selectedTemplateName.value = res.templateName

      if (levels.value.length > 0) {
        activeLevel.value = levels.value[0].levelName
      }
    } else {
      ElMessage.warning('该模板没有定性指标')
      levels.value = []
      hasRendered.value = false
      rendering.value = false
      return
    }

    // 加载可用批次
    await loadBatches()

    // 加载可用专家
    await loadExperts()

    // 初始化表格数据
    tableData.value = []
    savedRecords.value = {}
    updateProgress()

    hasRendered.value = true
    ElMessage.success('渲染成功')
  } catch (e) {
    ElMessage.error('渲染失败: ' + (e.message || '未知错误'))
  } finally {
    rendering.value = false
  }
}

// 加载批次
const loadBatches = async () => {
  try {
    const res = await getDynamicQlBatches(selectedTemplateId.value)
    batches.value = res || []
  } catch (e) {
    console.error('加载批次失败:', e)
    batches.value = []
  }
}

// 加载专家
const loadExperts = async () => {
  try {
    const res = await getDynamicQlExperts()
    availableExperts.value = res || []
  } catch (e) {
    console.error('加载专家失败:', e)
    availableExperts.value = []
  }
}

// 批次选择变化
const handleBatchChange = async (batchId) => {
  if (!batchId) {
    operations.value = []
    tableData.value = []
    savedRecords.value = {}
    updateProgress()
    return
  }

  try {
    // 获取作战列表（返回 List<String>，request拦截器自动返回 res.data）
    const res = await getDynamicQlOperations(batchId)

    // res 是字符串数组，如 ["OP001", "OP002", ...]
    if (Array.isArray(res) && res.length > 0) {
      operations.value = res.map(opId => ({
        operationId: opId,
        operationName: opId
      }))
    } else {
      operations.value = []
    }

    // 构建表格数据
    buildTableData()

    // 加载表格数据
    await loadTableData()
  } catch (e) {
    console.error('加载作战列表失败:', e)
    ElMessage.error('加载作战列表失败: ' + (e.message || '未知错误'))
    operations.value = []
    buildTableData()
  }
}

// 专家选择变化
const handleExpertChange = (expertId) => {
  if (expertId) {
    const expert = availableExperts.value.find(e => e.expert_id === expertId)
    if (expert) {
      selectedExperts.value = [expert]
    }
  } else {
    selectedExperts.value = []
  }
  // 切换专家后重新加载表格数据
  loadTableData()
}

// 开始批量评估（打开批量评估对话框）
const startBatchEvaluation = async () => {
  if (!selectedBatchId.value) {
    ElMessage.warning('请先选择批次')
    return
  }
  if (availableExperts.value.length === 0) {
    ElMessage.warning('没有可用的专家')
    return
  }
  if (operations.value.length === 0) {
    ElMessage.warning('没有可评估的作战')
    return
  }

  // 打开批量评估对话框
  showBatchEvaluateDialog.value = true
}

// 执行批量评估（评估所有专家）
const executeBatchEvaluation = async () => {
  if (availableExperts.value.length === 0) {
    ElMessage.warning('没有可用的专家')
    return
  }

  isEvaluating.value = true
  batchEvaluating.value = true
  let successCount = 0
  let failCount = 0

  try {
    // 使用所有可用专家进行评估
    for (const expert of availableExperts.value) {
      for (const op of operations.value) {
        const opId = typeof op === 'string' ? op : (op.operationId || op)
        const expId = String(expert.expert_id)

        // 获取该作战的定量参考数据
        let referenceData = null
        try {
          const refRes = await getDynamicQlReference(selectedBatchId.value, opId)
          if (refRes) {
            referenceData = refRes
          }
        } catch (e) {
          console.warn('获取参考数据失败:', opId, e)
        }

        // 为该专家该作战的所有指标打分
        const scores = []
        for (const levelData of levels.value) {
          for (const primary of levelData.primaries || []) {
            for (const sec of primary.secondaries || []) {
              // 生成随机评分和把握度
              const autoScore = generateRandomScore(referenceData, sec, expert, opId, expId)
              scores.push({
                code: sec.code,
                level: autoScore.level,
                confidence: autoScore.confidence,
                remark: autoScore.remark
              })
            }
          }
        }

        // 保存记录
        try {
          await saveDynamicQlRecord(
            selectedBatchId.value,
            selectedTemplateId.value,
            expert.expert_id,
            opId,
            scores
          )
          successCount++

          // 更新本地缓存
          if (!savedRecords.value[opId]) {
            savedRecords.value[opId] = {}
          }
          if (!savedRecords.value[opId][expId]) {
            savedRecords.value[opId][expId] = {}
          }
          for (const score of scores) {
            savedRecords.value[opId][expId][score.code] = {
              level: score.level,
              confidenceValue: score.confidence,
              remark: score.remark
            }
          }
        } catch (e) {
          console.error('保存评估失败:', expert.expert_id, opId, e)
          failCount++
        }
      }
    }

    ElMessage.success(`批量评估完成！成功: ${successCount}, 失败: ${failCount}`)
    showBatchEvaluateDialog.value = false

    // 更新selectedExperts为所有专家
    selectedExperts.value = [...availableExperts.value]

    // 重新加载表格数据
    await loadTableData()
    updateProgress()

  } catch (e) {
    ElMessage.error('批量评估失败: ' + (e.message || '未知错误'))
  } finally {
    batchEvaluating.value = false
    isEvaluating.value = false
  }
}

// 生成随机评分（评分和把握度都有随机性，更离散）
const generateRandomScore = (referenceData, indicator, expert, opId, expId) => {
  // 基于参考数据计算基准分
  let baseScore = 70 // 默认基准分
  if (referenceData && referenceData.rawData) {
    const rawData = referenceData.rawData
    const indicatorData = rawData[indicator.code] || rawData[indicator.name]
    if (indicatorData && indicatorData.value != null) {
      baseScore = Number(indicatorData.value)
      // 如果是负向指标，取反
      if (indicatorData.direction === 'NEGATIVE') {
        baseScore = 100 - baseScore
      }
    }
  }

  // 等级列表
  const levels = [
    { code: 'A+', name: '优+', min: 95 },
    { code: 'A', name: '优', min: 90 },
    { code: 'A-', name: '优-', min: 85 },
    { code: 'B+', name: '良+', min: 80 },
    { code: 'B', name: '良', min: 75 },
    { code: 'B-', name: '良-', min: 70 },
    { code: 'C+', name: '合格+', min: 65 },
    { code: 'C', name: '合格', min: 60 },
    { code: 'C-', name: '合格-', min: 55 },
    { code: 'D+', name: '差+', min: 47 },
    { code: 'D', name: '差', min: 39 },
    { code: 'D-', name: '差-', min: 30 },
    { code: 'E+', name: '极差+', min: 20 },
    { code: 'E', name: '极差', min: 10 },
    { code: 'E-', name: '极差-', min: 0 },
  ]

  // 基于专家ID、作战ID、指标编码生成唯一的随机种子
  const seed1 = (expert.expert_id * 7 + opId.charCodeAt(0) * 13 + (opId.length % 10) * 17) % 100
  const seed2 = (indicator.code.charCodeAt(0) * 3 + expert.expert_id * 11) % 100
  const seed3 = (expert.expert_id * 5 + opId.charCodeAt(opId.length - 1) * 7) % 50

  // 大的随机波动（±25分）
  const randomOffset = (Math.random() - 0.5) * 50
  let finalScore = baseScore + randomOffset

  // 基于专家的个性化偏移（±20分，让不同专家评分差异大）
  const expertOffset = (seed1 / 100 - 0.5) * 40
  finalScore = finalScore + expertOffset

  // 基于指标的随机偏移（让同一专家对不同指标评分也有差异）
  const indicatorOffset = (seed2 / 100 - 0.5) * 20
  finalScore = finalScore + indicatorOffset

  // 基于作战ID的随机偏移
  const operationOffset = (seed3 / 100 - 0.5) * 10
  finalScore = finalScore + operationOffset

  // 确保分数在0-100范围内
  finalScore = Math.max(0, Math.min(100, finalScore))

  // 根据最终分数确定等级
  let levelCode = 'E-'
  for (const level of levels) {
    if (finalScore >= level.min) {
      levelCode = level.code
      break
    }
  }

  // 把握度也更加离散（30-100之间，不均匀分布）
  let confidence
  const confSeed = (expert.expert_id * 13 + indicator.code.charCodeAt(0) * 7) % 100
  if (confSeed < 20) {
    // 20%概率：高把握 85-100
    confidence = Math.floor(85 + Math.random() * 15)
  } else if (confSeed < 50) {
    // 30%概率：中把握 65-85
    confidence = Math.floor(65 + Math.random() * 20)
  } else if (confSeed < 80) {
    // 30%概率：一般把握 45-65
    confidence = Math.floor(45 + Math.random() * 20)
  } else {
    // 20%概率：低把握 30-50
    confidence = Math.floor(30 + Math.random() * 20)
  }

  return {
    level: levelCode,
    confidence: confidence,
    remark: referenceData ? '批量评估' : '系统评分'
  }
}

// 根据定量参考数据自动生成定性等级（保留旧方法兼容）
const generateAutoLevel = (referenceData, indicator) => {
  if (!referenceData || !referenceData.rawData) {
    return { level: 'B', confidence: 70, remark: '无参考数据' }
  }

  const rawData = referenceData.rawData || {}
  const indicatorData = rawData[indicator.code] || rawData[indicator.name]

  if (!indicatorData) {
    return { level: 'B', confidence: 65, remark: '指标无参考数据' }
  }

  const value = indicatorData.value || indicatorData.normalized || 50
  const direction = indicatorData.direction || 'POSITIVE'

  let level, confidence

  if (direction === 'POSITIVE') {
    if (value >= 90) { level = 'A+'; confidence = 85 }
    else if (value >= 80) { level = 'A'; confidence = 80 }
    else if (value >= 70) { level = 'B+'; confidence = 75 }
    else if (value >= 60) { level = 'B'; confidence = 70 }
    else if (value >= 50) { level = 'C'; confidence = 65 }
    else { level = 'D'; confidence = 50 }
  } else {
    if (value <= 10) { level = 'A+'; confidence = 85 }
    else if (value <= 20) { level = 'A'; confidence = 80 }
    else if (value <= 30) { level = 'B+'; confidence = 75 }
    else if (value <= 40) { level = 'B'; confidence = 70 }
    else if (value <= 50) { level = 'C'; confidence = 65 }
    else { level = 'D'; confidence = 50 }
  }

  return { level, confidence, remark: '系统参考评分' }
}

// 当前指标数量
const indicatorCount = computed(() => {
  const levelData = levels.value.find(l => l.levelName === activeLevel.value)
  return levelData ? countIndicators(levelData) : 0
})

// 层级切换
const handleLevelChange = (levelName) => {
  activeLevel.value = levelName
  loadTableData()
}

// 加载表格数据
const loadTableData = async () => {
  if (!hasRendered.value) return

  try {
    const expertIds = selectedExperts.value.map(e => e.expert_id)
    
    const res = await getDynamicQlTableData(
      selectedBatchId.value,
      selectedTemplateId.value,
      activeLevel.value,
      expertIds.length > 0 ? expertIds : null
    )

    if (res) {
      // 更新作战列表
      if (res.operations && Array.isArray(res.operations)) {
        operations.value = res.operations.map(op => {
          if (typeof op === 'string') {
            return { operationId: op, operationName: op }
          }
          return { operationId: op.operationId || op, operationName: op.operationName || op.operationId || op }
        })
      }

      // 如果后端返回了完整的 levels 结构，更新整个 levels 数组
      if (res.levels && Array.isArray(res.levels) && res.levels.length > 0) {
        levels.value = res.levels
        // 确保 activeLevel 指向一个存在的层级
        if (!levels.value.find(l => l.levelName === activeLevel.value) && levels.value.length > 0) {
          activeLevel.value = levels.value[0].levelName
        }
      }

      // 更新保存的记录
      if (res.tableData) {
        savedRecords.value = res.tableData
      }

      // 更新进度
      if (res.progress) {
        progress.value = res.progress
      }

      // 构建表格数据
      buildTableData()
    }
  } catch (e) {
    console.error('加载表格数据失败:', e)
    ElMessage.error('加载表格数据失败: ' + (e.message || '未知错误'))
  }
}

// 构建表格数据
const buildTableData = () => {
  tableData.value = operations.value.map(op => {
    const opId = typeof op === 'string' ? op : (op.operationId || op)
    const opName = typeof op === 'string' ? op : (op.operationName || op.operationId || op)
    return {
      operationId: opId,
      operationName: opName
    }
  })
}

// 更新进度
const updateProgress = () => {
  const levelData = levels.value.find(l => l.levelName === activeLevel.value)
  const indicatorCount = levelData ? countIndicators(levelData) : 0
  const totalExperts = selectedExperts.value.length || 1
  const total = operations.value.length * indicatorCount * totalExperts

  let completed = 0
  for (const opId in savedRecords.value) {
    for (const expId in savedRecords.value[opId]) {
      completed += Object.keys(savedRecords.value[opId][expId]).length
    }
  }

  progress.value = {
    total,
    completed,
    percentage: total > 0 ? Math.round((completed / total) * 100) : 0
  }
}

// 统计指标数量
const countIndicators = (levelData) => {
  let count = 0
  for (const primary of levelData.primaries || []) {
    count += (primary.secondaries || []).length
  }
  return count
}

// 获取单元格评分
const getCellScore = (operationId, indicatorCode, expertId) => {
  if (!savedRecords.value[operationId]) return null
  if (!savedRecords.value[operationId][expertId]) return null
  return savedRecords.value[operationId][expertId][indicatorCode]
}

// 点击单元格
const handleCellClick = async (row, sec, expert) => {
  currentCell.value = { row, sec, expert }
  selectedScoreLevel.value = 'B'
  selectedConfidence.value = 80
  scoreRemark.value = ''

  // 回显已有评分
  const existingScore = getCellScore(row.operationId, sec.code, expert.expert_id)
  if (existingScore) {
    selectedScoreLevel.value = existingScore.level || 'B'
    selectedConfidence.value = existingScore.confidenceValue || 80
    scoreRemark.value = existingScore.remark || ''
  }

  showScoreDialog.value = true
}

// 保存评分
const handleScoreSave = async () => {
  if (!currentCell.value) return

  const { row, sec, expert } = currentCell.value

  try {
    await saveDynamicQlRecord(
      selectedBatchId.value,
      selectedTemplateId.value,
      expert.expert_id,
      row.operationId,
      [{
        code: sec.code,
        level: selectedScoreLevel.value,
        confidence: selectedConfidence.value,
        remark: scoreRemark.value
      }]
    )

    // 更新本地记录
    if (!savedRecords.value[row.operationId]) {
      savedRecords.value[row.operationId] = {}
    }
    if (!savedRecords.value[row.operationId][expert.expert_id]) {
      savedRecords.value[row.operationId][expert.expert_id] = {}
    }
    savedRecords.value[row.operationId][expert.expert_id][sec.code] = {
      level: selectedScoreLevel.value,
      confidenceValue: selectedConfidence.value,
      remark: scoreRemark.value
    }

    showScoreDialog.value = false
    updateProgress()
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || '未知错误'))
  }
}

// 评分等级标签
const scoreLevelLabels = {
  'A+': { label: '优+', type: 'success' },
  'A': { label: '优', type: 'success' },
  'A-': { label: '优-', type: 'success' },
  'B+': { label: '良+', type: 'primary' },
  'B': { label: '良', type: 'primary' },
  'B-': { label: '良-', type: 'primary' },
  'C+': { label: '合格+', type: 'warning' },
  'C': { label: '合格', type: 'warning' },
  'C-': { label: '合格-', type: 'warning' },
  'D+': { label: '差+', type: 'danger' },
  'D': { label: '差', type: 'danger' },
  'D-': { label: '差-', type: 'danger' },
  'E+': { label: '极差+', type: 'info' },
  'E': { label: '极差', type: 'info' },
  'E-': { label: '极差-', type: 'info' },
}

const getScoreTagType = (level) => {
  return scoreLevelLabels[level]?.type || 'info'
}

const getScoreLabel = (level) => {
  return scoreLevelLabels[level]?.label || level
}

// 根据大类获取标签类型
const getLevelTagType = (category) => {
  switch (category) {
    case '优': return 'success'
    case '良': return 'primary'
    case '合格': return 'warning'
    case '差': return 'danger'
    case '极差': return 'info'
    default: return 'info'
  }
}

// 可信度类型
const getCredibilityType = (credibility) => {
  if (credibility >= 90) return 'success'
  if (credibility >= 80) return 'primary'
  if (credibility >= 70) return 'warning'
  return 'danger'
}

// 执行集结
const handleAggregate = async () => {
  if (!selectedBatchId.value) {
    ElMessage.warning('请先选择批次')
    return
  }

  aggregating.value = true
  try {
    const res = await aggregateDynamicQlScores(
      selectedBatchId.value,
      null,  // operationId: null 表示对所有作战进行集结
      null,  // levelName: null 表示对所有层级进行集结
      aggregateWeightAlpha.value / 100,
      aggregateWeightLambda.value / 100
    )

    if (res && res.success) {
      showAggregateDialog.value = false
      aggregationResult.value = res
      showAggregationResult.value = true
      aggDetailExpanded.value = {}
      aggChartReady.value = {}
      selectedOperationFilter.value = ''
      // 设置默认选中的层级
      if (res.levelInfos && res.levelInfos.length > 0) {
        activeAggregationLevel.value = res.levelInfos[0].levelName
      }
      ElMessage.success(`集结成功！计算了 ${res.operationIds?.length || 0} 个作战共 ${res.aggregationResults?.length || 0} 个指标的集结结果`)
    } else {
      ElMessage.error(res?.message || '集结失败')
    }
  } catch (e) {
    ElMessage.error('集结失败: ' + (e.message || '未知错误'))
  } finally {
    aggregating.value = false
  }
}

// 等级标签类型（按分数）
const gradeTagTypeByScore = (score) => {
  if (score == null) return 'info'
  const s = Number(score)
  if (s >= 90) return 'success'
  if (s >= 75) return 'primary'
  if (s >= 60) return 'warning'
  return 'danger'
}

// 切换明细展开
const toggleDetail = (indicatorCode, operationId) => {
  const key = indicatorCode + '_' + operationId
  aggDetailExpanded.value[key] = !aggDetailExpanded.value[key]
}

// 切换图表展开
const toggleChart = (indicatorCode, operationId) => {
  const key = '__chart_' + indicatorCode + '_' + operationId
  aggDetailExpanded.value[key] = !aggDetailExpanded.value[key]
  if (aggDetailExpanded.value[key]) {
    nextTick(() => renderCentroidChart(indicatorCode, operationId))
  }
}

// 构建加权覆盖阶梯数据
const buildWeightedCoverageStepData = (details) => {
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

// 销毁图表实例
const disposeAggChartsForKey = (indicatorKey) => {
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

// 渲染质心图
const renderCentroidChart = (indicatorKey, operationId) => {
  const elPx = document.getElementById('ql-agg-chart-' + indicatorKey + '-' + operationId)
  if (!elPx) return

  // 从 aggregationResults 中查找对应的指标结果
  const indicators = aggregationResult.value?.aggregationResults || []
  const ind = indicators.find(i => i.indicatorCode === indicatorKey && i.operationId === operationId)
  if (!ind) return

  disposeAggChartsForKey(indicatorKey + '-' + operationId)

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
      subtext: `作战 ${operationId} · ${ind.indicatorName}`,
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

  aggCharts.value[indicatorKey + '-' + operationId] = chartPx
  aggChartReady.value[indicatorKey + '-' + operationId] = true
}

// 窗口 resize 时重绘图表
const resizeAggCharts = () => {
  Object.values(aggCharts.value).forEach((entry) => {
    if (Array.isArray(entry)) {
      entry.forEach((c) => c && c.resize())
    } else if (entry && entry.resize) {
      entry.resize()
    }
  })
}

// 监听层级变化
watch(activeLevel, () => {
  updateProgress()
})

onMounted(() => {
  loadTemplates()
  window.addEventListener('resize', resizeAggCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeAggCharts)
  // 销毁所有图表
  Object.keys(aggCharts.value).forEach(key => {
    disposeAggChartsForKey(key)
  })
})
</script>

<style scoped lang="scss">
// 表头样式（参照定量评估）
:deep(.el-table th.el-table__cell) {
  background-color: #304156 !important;
  color: #ffffff !important;
  font-weight: 600;
}

:deep(.el-table .el-table__header-wrapper th) {
  background-color: #304156 !important;
  color: #ffffff !important;
}

:deep(.el-table .el-table__header-wrapper .el-table__row:first-child th) {
  background-color: #1e3a5f !important;
  border-color: #2d5a8a !important;
}

:deep(.el-table .el-table__header-wrapper .el-table__row:last-child th) {
  background-color: #304156 !important;
  border-color: #3d6a9a !important;
}

.dynamic-qualitative-view {
  padding: 20px;
  max-width: 1800px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;

  h2 {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 20px;
    color: #001f3f;
    margin-bottom: 6px;

    .el-icon {
      font-size: 24px;
      color: #409EFF;
    }
  }

  .subtitle {
    color: #606266;
    font-size: 13px;
    margin: 0;
  }
}

.toolbar-card, .config-card {
  margin-bottom: 16px;

  :deep(.el-card__body) {
    padding: 12px 16px;
  }
}

.table-card {
  :deep(.el-card__header) {
    padding: 12px 16px;
    background: linear-gradient(135deg, #1e3a5f 0%, #2d5a8a 100%);
  }
}

// 集结结果卡片样式
.aggregation-result-card {
  margin-top: 20px;

  :deep(.el-card__header) {
    padding: 12px 16px;
    background: linear-gradient(135deg, #1e5a1e 0%, #2d8a2d 100%);
    color: #ffffff;
  }

  .aggregation-header {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .header-left {
      display: flex;
      align-items: center;
      font-weight: 700;
      font-size: 16px;
      color: #ffffff;
      gap: 8px;

      .el-icon {
        font-size: 20px;
      }
    }
  }

.operation-filter {
  margin-bottom: 16px;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  border-left: 3px solid #409eff;
  display: flex;
  align-items: center;
  gap: 12px;

  .filter-label {
    font-weight: 600;
    color: #303133;
    font-size: 13px;
  }
}

.weight-info {
  font-size: 13px;
  color: #1a3a5c;
  margin-bottom: 16px;
  padding: 8px 12px;
  background: #ecf5ff;
  border-radius: 4px;
  border-left: 3px solid #409eff;

  .weight-raw {
    margin-left: 12px;
    color: #909399;
    font-size: 12px;
  }
}

  .centroid-table {
    margin-bottom: 16px;

    .section-title {
      font-size: 14px;
      font-weight: 600;
      color: #1a3a5c;
      margin-bottom: 8px;
      padding-left: 4px;
      border-left: 3px solid var(--el-color-success);
    }

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

  .detail-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 14px;
    margin-bottom: 10px;
    background: linear-gradient(90deg, #e8f2fc 0%, #f7fbff 55%, #fafcff 100%);
    border-radius: 6px;
    border: 1px solid #c5d9ed;

    .detail-head-title {
      font-size: 14px;
      font-weight: 600;
      color: #1a3a5c;
    }

    .formula-hint {
      font-size: 11px;
      color: #606266;
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
      width: 100%;
      margin-bottom: 2px;
      color: #606266;
      font-weight: 600;
    }

    .agg-gamma-tag {
      margin: 0;
      font-family: 'Courier New', monospace;
    }
  }
}

.toolbar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-left, .toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.config-content {
  padding: 12px 0;
}

.config-item {
  label {
    display: block;
    font-weight: 600;
    margin-bottom: 8px;
    color: #303133;
  }
}

.batch-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;

  .batch-id {
    font-weight: 500;
  }

  .batch-count {
    font-size: 12px;
    color: #909399;
  }
}

.expert-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.selected-experts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.expert-tag {
  .credibility {
    font-size: 11px;
    color: #909399;
    margin-left: 4px;
  }
}

.template-stats {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}

.progress-section {
  margin-top: 12px;
}

.progress-info {
  display: flex;
  align-items: center;
  gap: 12px;

  .progress-label {
    font-weight: 600;
    color: #303133;
  }

  .progress-text {
    font-size: 13px;
    color: #606266;
  }
}

.table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .header-left {
    display: flex;
    align-items: center;
    font-weight: 700;
    font-size: 16px;
    color: #ffffff;
    gap: 8px;

    .el-icon {
      font-size: 20px;
      color: #409EFF;
    }
  }

  .header-actions {
    display: flex;
    gap: 8px;
  }
}

.level-tabs {
  margin-top: 12px;
}

.sec-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  line-height: 1.4;

  span {
    font-size: 13px;
    font-weight: 600;
    color: #ffffff;
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  }

  .header-tags {
    display: flex;
    gap: 4px;
    flex-wrap: wrap;
    justify-content: center;
  }

  :deep(.el-tag) {
    font-size: 10px;
    font-weight: 700;
    padding: 0 4px;
    height: 16px;
    line-height: 14px;

    &.el-tag--success {
      background-color: #2d6a2d;
      border-color: #4a9a4a;
      color: #a8e6a8;
    }

    &.el-tag--warning {
      background-color: #6a4a2d;
      border-color: #9a6a4a;
      color: #ffe4a8;
    }

    &.el-tag--primary {
      background-color: #1a4a6a;
      border-color: #4a6a9a;
      color: #a8d4ff;
    }

    &.el-tag--info {
      background-color: #4a4a5a;
      border-color: #7a7a9a;
      color: #d4d4e8;
    }
  }
}

.expert-score-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 6px 8px;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
  background: rgba(0, 0, 0, 0.02);
  margin: 2px 0;
  border: 1px solid transparent;

  &:hover {
    background-color: #e6f0ff;
    border-color: #409eff;
    transform: scale(1.02);
  }

  .score-display {
    min-width: 80px;
    text-align: center;
  }

  .empty-score {
    color: #c0c4cc;
    font-size: 16px;
    font-weight: 300;
  }

  .confidence-display {
    font-size: 11px;
    color: #909399;
    margin-top: 2px;
  }

  .empty-confidence {
    font-size: 11px;
    color: #c0c4cc;
    margin-top: 2px;
  }
}

.empty-expert-hint {
  text-align: center;
  color: #909399;
  font-size: 12px;
  padding: 20px 0;
}

.empty-table-tip {
  padding: 40px;
  background: #fafafa;
  border-radius: 4px;
}

.indicator-name {
  font-weight: 600;
  color: #409EFF;
}

.weight-slider {
  display: flex;
  align-items: center;
  gap: 16px;

  .weight-value {
    min-width: 50px;
    font-weight: 600;
    color: #409EFF;
    font-size: 16px;
  }
}

.formula-desc {
  font-size: 12px;
  color: #909399;
  margin: 4px 0 0 0;
}

// 评分对话框样式
.level-table-wrapper {
  margin-bottom: 16px;
}

.level-select-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
  margin-bottom: 16px;
}

.level-select-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: #409eff;
    background-color: #ecf5ff;
  }

  &.active {
    border-color: #409eff;
    background-color: #409eff;
    color: #fff;
  }

  .level-code {
    font-size: 14px;
    font-weight: 700;
  }

  .level-name {
    font-size: 12px;
    margin-top: 2px;
  }
}

.confidence-table-wrapper {
  margin-bottom: 16px;
}

.confidence-input {
  display: flex;
  align-items: center;
  gap: 16px;

  .confidence-value {
    font-size: 18px;
    font-weight: 700;
    color: #409eff;
    min-width: 60px;
  }

  .confidence-level {
    font-size: 14px;
    color: #606266;
  }
}
</style>
