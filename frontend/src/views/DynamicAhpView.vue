<template>
  <div class="dynamic-ahp-view">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>
        <el-icon><Setting /></el-icon>
        动态指标AHP权重配置
      </h2>
      <p class="subtitle">对动态指标体系进行层次分析法（AHP）权重配置，支持多层级、一级维度间、二级指标间的比较打分</p>
    </div>

    <!-- 顶部工具栏 -->
    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <!-- 模板选择 -->
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
              <span class="template-stats">{{ tpl.levelCount || 0 }}层级</span>
            </el-option>
          </el-select>

          <!-- 专家选择 -->
          <el-select
            v-model="selectedExpertId"
            placeholder="请选择专家"
            clearable
            filterable
            style="width: 220px"
            @change="handleExpertChange"
          >
            <el-option
              v-for="e in experts"
              :key="e.expertId"
              :label="`${e.expertName}（ID ${e.expertId}）`"
              :value="e.expertId"
            />
          </el-select>

          <el-button
            type="primary"
            :icon="Refresh"
            :loading="loading"
            :disabled="!selectedTemplateId || !selectedExpertId"
            @click="loadData"
          >
            加载数据
          </el-button>

          <el-button
            v-if="hasData"
            type="primary"
            :loading="saving"
            @click="saveAllMatrices"
          >
            保存
          </el-button>

          <el-button
            v-if="hasData"
            type="success"
            :loading="calculating"
            @click="calculateCombinedWeights"
          >
            计算
          </el-button>
        </div>

        <div class="toolbar-right">
          <el-tag v-if="selectedTemplateId" type="success" size="large">
            {{ selectedTemplateName }}
          </el-tag>
          <el-tag v-if="selectedExpertId" type="warning" size="large">
            {{ selectedExpertName }}
          </el-tag>
          <el-button
            v-if="selectedTemplateId"
            type="success"
            :loading="simulating"
            @click="handleBatchSimulate"
          >
            批量模拟
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 状态栏 -->
    <el-card v-if="hasData" class="status-card" shadow="never">
      <div class="status-row">
        <div class="status-item">
          <span class="status-label">配置进度:</span>
          <el-progress
            :percentage="status.progress"
            :color="progressColors"
            style="width: 200px"
          />
          <span class="status-value">{{ status.completedMatrices }}/{{ status.totalMatrices }} 矩阵</span>
        </div>
        <div class="status-actions">
          <el-button @click="handleClear" :disabled="!selectedExpertId">
            清除数据
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- AHP矩阵配置区域 -->
    <div v-if="hasData" class="ahp-content">
      <!-- AHP说明 -->
      <el-card class="guide-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>
              <el-icon><QuestionFilled /></el-icon>
              AHP 判断矩阵填写说明
            </span>
          </div>
        </template>

        <el-collapse>
          <el-collapse-item title="一、AHP 判断标度说明（Saaty 1~9 标度）" name="scale">
            <div class="guide-content">
              <p>上三角「重要性」为 Saaty 正互反标度，取值区间 [1/9，9]（可填小数）：表示行要素相对列要素。1 为同等重要；>1 表示行比列重要（如 3、5）；<1 表示行不如列重要（如 0.33≈1/3、0.2=1/5）。下三角为互反倒数。</p>
              <el-table :data="scaleTableData" border size="small" class="guide-table">
                <el-table-column prop="scale" label="标度" width="80" align="center" />
                <el-table-column prop="meaning" label="含义" width="100" align="center" />
                <el-table-column prop="description" label="说明" />
                <el-table-column prop="scenario" label="适用场景" />
              </el-table>
            </div>
          </el-collapse-item>

          <el-collapse-item title="二、把握度等级与判断可信度" name="confidence">
            <div class="guide-content">
              <p>矩阵上三角中「把握度」为 0～1 的小数，表示您对该次重要性比较的可信程度，可按下表判断可信度 λ 取值：1 级对应 λ=1，2 级对应 0.8，3 级对应 0.6；4 级表示不能确认，建议 λ 取小于 0.6 的数值（如 0.5、0.4）。</p>
              <p>新选专家且无已存数据时，系统默认标度全为 1、把握度为 0.55（低于 0.6）。</p>
              <el-table :data="confidenceTableData" border size="small" class="guide-table">
                <el-table-column prop="level" label="等级" width="80" align="center" />
                <el-table-column prop="meaning" label="等级含义" width="100" align="center" />
                <el-table-column prop="standard" label="等级标准" />
                <el-table-column prop="lambda" label="判断可信度 λ" width="140" align="center" />
              </el-table>
            </div>
          </el-collapse-item>
        </el-collapse>
      </el-card>

      <!-- 层级间比较矩阵 -->
      <el-card v-if="levels.length > 1" class="level-between-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>
              <el-icon><Grid /></el-icon>
              层级间比较矩阵
            </span>
            <el-tag type="info" size="small">
              {{ levels.length }} 个层级间的权重比较
            </el-tag>
          </div>
        </template>

        <div class="matrix-guide">
          <p><strong>说明：</strong>请先阅读上方的「AHP 判断矩阵填写说明」，然后比较各层级对评估目标的重要程度，确定层级间的相对权重。</p>
          <p><strong>当前专家：</strong>{{ selectedExpertName || '未选择' }}（先选择专家，再填写矩阵）</p>
        </div>

        <div class="ahp-matrix-scroll">
          <table class="ahp-matrix-table">
            <thead>
              <tr>
                <th class="matrix-corner-cell"></th>
                <th
                  v-for="(lvl, idx) in levels"
                  :key="'level-header-' + idx"
                  class="matrix-header-cell"
                >
                  {{ lvl }}
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(rowLevel, rowIdx) in levels"
                :key="'level-row-' + rowIdx"
              >
                <td class="matrix-row-label">{{ rowLevel }}</td>
                        <td
                          v-for="(colLevel, colIdx) in levels"
                          :key="'level-cell-' + rowIdx + '-' + colIdx"
                          class="matrix-cell"
                          :class="{
                            'cell-diagonal': rowIdx === colIdx,
                            'cell-editable': rowIdx < colIdx,
                            'cell-lower': rowIdx > colIdx
                          }"
                        >
                          <span v-if="rowIdx === colIdx">1</span>
                          <div v-else-if="rowIdx < colIdx" class="cell-inputs">
                            <el-input-number
                              v-model="levelMatrixData[rowIdx][colIdx].score"
                              :min="0.111"
                              :max="9"
                              :step="0.1"
                              :precision="2"
                              size="small"
                              :controls="false"
                              @change="(val) => onLevelCellChange(rowIdx, colIdx, val)"
                            />
                            <el-tooltip content="把握度" placement="top" :show-after="300">
                              <span class="conf-label">把握度:</span>
                            </el-tooltip>
                            <el-input-number
                              v-model="levelMatrixData[rowIdx][colIdx].confidence"
                              :min="0"
                              :max="1"
                              :step="0.05"
                              :precision="2"
                              size="small"
                              :controls="false"
                            />
                          </div>
                          <span v-else class="auto-value">{{ levelMatrixData[colIdx]?.[rowIdx]?.score ? getReciprocal(levelMatrixData[colIdx][rowIdx].score) : '-' }}</span>
                        </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="matrix-actions">
          <el-button @click="calculateLevelWeights" :loading="calculating">
            计算层级权重
          </el-button>
        </div>

        <div v-if="levelBetweenResult" class="weight-result">
          <h4>层级权重结果</h4>
          <el-table :data="formatLevelWeights" border size="small">
            <el-table-column prop="name" label="层级" />
            <el-table-column prop="weight" label="权重" align="center">
              <template #default="{ row }">
                <el-tag type="primary" size="small">{{ (row.weight * 100).toFixed(2) }}%</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-alert
            :title="`λmax = ${levelBetweenResult.lambdaMax?.toFixed(2)}, CR = ${levelBetweenResult.cr?.toFixed(2)}, ${levelBetweenResult.consistent ? '通过一致性检验' : '未通过一致性检验'}`"
            :type="levelBetweenResult.consistent ? 'success' : 'warning'"
            :closable="false"
            style="margin-top: 12px"
          />
        </div>
      </el-card>

      <!-- 层级Tab -->
      <el-card class="level-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>
              <el-icon><Grid /></el-icon>
              层级内权重配置
            </span>
            <el-tag type="info" size="small">
              {{ levels.length }} 个层级
            </el-tag>
          </div>
        </template>

        <el-tabs v-model="activeLevel" type="border-card" class="level-tabs" @tab-change="handleLevelChange">
          <el-tab-pane
            v-for="level in levels"
            :key="level"
            :label="level"
            :name="level"
          >
            <!-- 层级说明 -->
            <el-alert
              :title="`当前层级：${level}。请进行以下比较打分：`"
              type="info"
              :closable="false"
              style="margin-bottom: 16px"
            />

            <!-- 一、一级维度间比较 -->
            <div class="matrix-section">
              <h3 class="section-title">
                <el-icon><Grid /></el-icon>
                一、一级维度间比较矩阵
                <el-tag v-if="levelResults[level]?.primaryResult" :type="levelResults[level].primaryResult.consistent ? 'success' : 'warning'" size="small" style="margin-left: 12px">
                  CR: {{ levelResults[level].primaryResult.cr }}
                </el-tag>
              </h3>

              <div class="matrix-card">
                <div class="matrix-guide">
                  <p><strong>说明：</strong>上三角区域为可编辑的比较标度（1-9 Saaty标度），下三角自动生成倒数。</p>
                  <p>请比较各一级维度对评估目标的重要程度。</p>
                </div>

                <div class="ahp-matrix-scroll">
                  <table class="ahp-matrix-table">
                    <thead>
                      <tr>
                        <th class="matrix-corner-cell"></th>
                        <th
                          v-for="(dim, idx) in getPrimariesForLevel(level)"
                          :key="'header-' + idx"
                          class="matrix-header-cell"
                        >
                          {{ dim.name }}
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr
                        v-for="(rowDim, rowIdx) in getPrimariesForLevel(level)"
                        :key="'row-' + rowIdx"
                      >
                        <td class="matrix-row-label">{{ rowDim.name }}</td>
                        <td
                          v-for="(colDim, colIdx) in getPrimariesForLevel(level)"
                          :key="'cell-' + rowIdx + '-' + colIdx"
                          class="matrix-cell"
                          :class="{
                            'cell-diagonal': rowIdx === colIdx,
                            'cell-editable': rowIdx < colIdx,
                            'cell-lower': rowIdx > colIdx
                          }"
                        >
                          <span v-if="rowIdx === colIdx">1</span>
                          <div v-else-if="rowIdx < colIdx" class="cell-inputs">
                            <el-input-number
                              v-model="primaryMatrixData[level][rowIdx][colIdx].score"
                              :min="0.111"
                              :max="9"
                              :step="0.1"
                              :precision="2"
                              size="small"
                              :controls="false"
                              @change="(val) => onPrimaryCellChange(level, rowIdx, colIdx, val)"
                            />
                            <el-tooltip content="把握度" placement="top" :show-after="300">
                              <span class="conf-label">把握度:</span>
                            </el-tooltip>
                            <el-input-number
                              v-model="primaryMatrixData[level][rowIdx][colIdx].confidence"
                              :min="0"
                              :max="1"
                              :step="0.05"
                              :precision="2"
                              size="small"
                              :controls="false"
                            />
                          </div>
                          <span v-else class="auto-value">{{ primaryMatrixData[level]?.[colIdx]?.[rowIdx]?.score ? getReciprocal(primaryMatrixData[level][colIdx][rowIdx].score) : '-' }}</span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>

                <div class="matrix-actions">
                  <el-button @click="calculatePrimaryWeights(level)" :loading="calculating">
                    计算权重
                  </el-button>
                </div>

                <!-- 一级维度权重结果 -->
                <div v-if="levelResults[level]?.primaryResult" class="weight-result">
                  <h4>一级维度权重结果</h4>
                  <el-table :data="formatPrimaryWeights(level)" border size="small">
                    <el-table-column prop="name" label="一级维度" />
                    <el-table-column prop="levelWeight" label="层级权重" align="center">
                      <template #default="{ row }">
                        <el-tag type="info" size="small">{{ (row.levelWeight * 100).toFixed(2) }}%</el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="weight" label="维度内权重" align="center">
                      <template #default="{ row }">
                        <el-tag type="primary" size="small">{{ (row.weight * 100).toFixed(2) }}%</el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="combinedWeight" label="综合权重" align="center">
                      <template #default="{ row }">
                        <el-tag type="danger" effect="dark" size="small">{{ (row.combinedWeight * 100).toFixed(2) }}%</el-tag>
                      </template>
                    </el-table-column>
                  </el-table>
                  <el-alert
                    :title="`λmax = ${levelResults[level].primaryResult.lambdaMax?.toFixed(2)}, CR = ${levelResults[level].primaryResult.cr?.toFixed(2)}, ${levelResults[level].primaryResult.consistent ? '通过一致性检验' : '未通过一致性检验'}`"
                    :type="levelResults[level].primaryResult.consistent ? 'success' : 'warning'"
                    :closable="false"
                    style="margin-top: 12px"
                  />
                </div>
              </div>
            </div>

            <!-- 二、二级指标间比较 -->
            <div class="matrix-section secondary-section">
              <h3 class="section-title">
                <el-icon><Grid /></el-icon>
                二、二级指标间比较矩阵
              </h3>

              <el-alert
                title="请在下方选择一级维度，然后对属于该维度下的二级指标进行两两比较"
                type="info"
                :closable="false"
                style="margin-bottom: 16px"
              />

              <!-- 一级维度选择Tab -->
              <el-tabs v-model="activePrimaryTabs[level]" type="border-card" class="primary-tabs">
                <el-tab-pane
                  v-for="primary in getPrimariesForLevel(level)"
                  :key="primary.code"
                  :label="`${primary.name}（${getSecondaryCount(level, primary.code)}项）`"
                  :name="primary.code"
                >
                  <!-- 二级指标间矩阵 -->
                  <div v-if="getSecondariesForLevel(level, primary.code).length > 1" class="matrix-card">
                    <div class="matrix-guide">
                      <p><strong>比较说明：</strong>请比较 {{ primary.name }} 下的各二级指标的重要程度。</p>
                    </div>

                    <div class="ahp-matrix-scroll">
                      <table class="ahp-matrix-table small-table">
                        <thead>
                          <tr>
                            <th class="matrix-corner-cell"></th>
                            <th
                              v-for="(sec, idx) in getSecondariesForLevel(level, primary.code)"
                              :key="'sec-header-' + idx"
                              class="matrix-header-cell"
                            >
                              {{ sec.name }}
                            </th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr
                            v-for="(rowSec, rowIdx) in getSecondariesForLevel(level, primary.code)"
                            :key="'sec-row-' + rowIdx"
                          >
                            <td class="matrix-row-label">{{ rowSec.name }}</td>
                            <td
                              v-for="(colSec, colIdx) in getSecondariesForLevel(level, primary.code)"
                              :key="'sec-cell-' + rowIdx + '-' + colIdx"
                              class="matrix-cell"
                              :class="{
                                'cell-diagonal': rowIdx === colIdx,
                                'cell-editable': rowIdx < colIdx,
                                'cell-lower': rowIdx > colIdx
                              }"
                            >
                              <span v-if="rowIdx === colIdx">1</span>
                              <div v-else-if="rowIdx < colIdx" class="cell-inputs">
                                <el-input-number
                                  v-model="secondaryMatrixData[level][primary.code][rowIdx][colIdx].score"
                                  :min="0.111"
                                  :max="9"
                                  :step="0.1"
                                  :precision="2"
                                  size="small"
                                  :controls="false"
                                  @change="(val) => onSecondaryCellChange(level, primary.code, rowIdx, colIdx, val)"
                                />
                                <el-tooltip content="把握度" placement="top" :show-after="300">
                                  <span class="conf-label">把握度:</span>
                                </el-tooltip>
                                <el-input-number
                                  v-model="secondaryMatrixData[level][primary.code][rowIdx][colIdx].confidence"
                                  :min="0"
                                  :max="1"
                                  :step="0.05"
                                  :precision="2"
                                  size="small"
                                  :controls="false"
                                />
                              </div>
                              <span v-else class="auto-value">{{ secondaryMatrixData[level]?.[primary.code]?.[colIdx]?.[rowIdx]?.score ? getReciprocal(secondaryMatrixData[level][primary.code][colIdx][rowIdx].score) : '-' }}</span>
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>

                    <div class="matrix-actions">
                      <el-button @click="calculateSecondaryWeights(level, primary.code)" :loading="calculating">
                        计算权重
                      </el-button>
                    </div>

                    <!-- 二级指标权重结果 -->
                    <div v-if="levelResults[level]?.secondaryResults?.[primary.code]" class="weight-result">
                      <h4>{{ primary.name }} - 二级指标权重</h4>
                      <el-table :data="formatSecondaryWeights(level, primary.code)" border size="small">
                        <el-table-column prop="name" label="二级指标" />
                        <el-table-column prop="metricType" label="类型" width="80" align="center">
                          <template #default="{ row }">
                            <el-tag :type="row.metricType === 'QUANTITATIVE' ? 'primary' : 'warning'" size="small">
                              {{ row.metricType === 'QUANTITATIVE' ? '定量' : '定性' }}
                            </el-tag>
                          </template>
                        </el-table-column>
                        <el-table-column prop="weight" label="指标权重（维度内）" align="center">
                          <template #default="{ row }">
                            <el-tag type="success" size="small">{{ (row.weight * 100).toFixed(2) }}%</el-tag>
                          </template>
                        </el-table-column>
                        <el-table-column prop="primaryCombinedWeight" label="一级综合权重" align="center">
                          <template #default="{ row }">
                            <el-tag type="warning" size="small">{{ (row.primaryCombinedWeight * 100).toFixed(2) }}%</el-tag>
                          </template>
                        </el-table-column>
                        <el-table-column prop="combinedWeight" label="二级综合权重" align="center">
                          <template #default="{ row }">
                            <el-tag type="danger" effect="dark" size="small">{{ (row.combinedWeight * 100).toFixed(2) }}%</el-tag>
                          </template>
                        </el-table-column>
                      </el-table>
                    </div>
                  </div>

                  <el-alert
                    v-else
                    :title="`${primary.name} 下只有 ${getSecondariesForLevel(level, primary.code).length} 个指标，无需进行两两比较`"
                    type="info"
                    :closable="false"
                  />
                </el-tab-pane>
              </el-tabs>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-card>

      <!-- 综合权重结果 -->
      <el-card v-if="hasCompleteResults" class="result-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>
              <el-icon><DataLine /></el-icon>
              综合权重结果
            </span>
          </div>
        </template>

        <el-alert
          :title="`综合权重总和: ${(totalWeight * 100).toFixed(2)}% | ${allConsistent ? '所有矩阵通过一致性检验' : '存在未通过一致性检验的矩阵'}`"
          :type="allConsistent ? 'success' : 'warning'"
          :closable="false"
          style="margin-bottom: 16px"
        />

        <el-table :data="combinedWeightsTable" border stripe max-height="500" size="small">
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column prop="levelName" label="层级" width="100" />
          <el-table-column prop="primaryName" label="一级维度" min-width="120">
            <template #default="{ row }">
              <div>{{ row.primaryName }}</div>
              <div v-if="row.primaryDescription" class="description-text">{{ row.primaryDescription }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="secondaryName" label="二级指标" min-width="150">
            <template #default="{ row }">
              <div>{{ row.secondaryName }}</div>
              <div v-if="row.secondaryDescription" class="description-text">{{ row.secondaryDescription }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="metricType" label="类型" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.metricType === 'QUANTITATIVE' ? 'primary' : 'warning'" size="small">
                {{ row.metricType === 'QUANTITATIVE' ? '定量' : '定性' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="primaryWeight" label="一级维度权重" width="120" align="center">
            <template #default="{ row }">
              {{ (row.primaryWeight * 100).toFixed(2) }}%
            </template>
          </el-table-column>
          <el-table-column prop="secondaryWeight" label="指标权重（维度内）" width="140" align="center">
            <template #default="{ row }">
              {{ (row.secondaryWeight * 100).toFixed(2) }}%
            </template>
          </el-table-column>
          <el-table-column label="综合权重" width="120" align="center">
            <template #default="{ row }">
              <el-tag type="danger" effect="dark" size="small">
                {{ (row.combinedWeight * 100).toFixed(2) }}%
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 空状态 -->
    <el-empty
      v-if="!hasData"
      description="请先选择指标模板和专家，然后点击「加载数据」"
    >
      <template #image>
        <el-icon :size="80" style="color: #c0c4cc"><Setting /></el-icon>
      </template>
    </el-empty>

    <!-- 模拟对话框 -->
    <el-dialog v-model="simulateDialogVisible" title="批量模拟AHP打分" width="500px">
      <el-form label-width="100px">
        <el-form-item label="选择专家">
          <el-select
            v-model="simulateExpertIds"
            multiple
            filterable
            collapse-tags
            placeholder="选择专家"
            style="width: 100%"
          >
            <el-option
              v-for="e in experts"
              :key="e.expertId"
              :label="`${e.expertName}（ID ${e.expertId}）`"
              :value="e.expertId"
            />
          </el-select>
        </el-form-item>
        <el-alert
          title="模拟说明"
          type="info"
          :closable="false"
        >
          将为选中的专家生成随机的AHP比较打分数据。
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="simulateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSimulate" :loading="simulating">
          开始模拟
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Setting, Grid, DataLine, Refresh, QuestionFilled } from '@element-plus/icons-vue'
import * as dynamicAhpApi from '@/api'
import * as expertApi from '@/api'

// 状态
const templates = ref([])
const experts = ref([])
const selectedTemplateId = ref(null)

// AHP说明表格数据
const scaleTableData = [
  { scale: '1', meaning: '同样重要', description: '两个因素相比，具有相同的重要性', scenario: '两者同等重要，难以区分' },
  { scale: '3', meaning: '稍微重要', description: '一个因素比另一个稍微重要', scenario: '经验判断轻微偏向一方' },
  { scale: '5', meaning: '明显重要', description: '一个因素比另一个明显重要', scenario: '经验判断明显偏向一方' },
  { scale: '7', meaning: '强烈重要', description: '一个因素比另一个强烈重要', scenario: '实际显示非常偏向一方' },
  { scale: '9', meaning: '极端重要', description: '一个因素比另一个极端重要', scenario: '绝对偏向一方，证据确凿' },
  { scale: '2,4,6,8', meaning: '相邻中间值', description: '需要折中时使用', scenario: '介于两个判断之间' },
  { scale: '1/2~1/9', meaning: '反比较', description: '<1 表示行不如列重要', scenario: '0.33≈1/3, 0.2=1/5' },
]

const confidenceTableData = [
  { level: '1 级', meaning: '完全确认', standard: '评价者对评价指标非常熟悉，经验丰富，认为自己的评价结果完全可信，无任何异议；数据测量和分析完全可信。', lambda: '1' },
  { level: '2 级', meaning: '确认', standard: '评价者对指标熟悉，有一定的从业经历，认为自己的评价结果可信；数据测量和分析可信。', lambda: '0.8' },
  { level: '3 级', meaning: '基本确认', standard: '评价者对指标有一定的了解，但不是特别熟悉，认为自己的评价结果有一定的可信度，但没有较大的把握；数据测量和分析基本可信。', lambda: '0.6' },
  { level: '4 级', meaning: '不能确认', standard: '评价者对评价指标不熟悉，认为自己的评价结果不能确认；数据测量和分析不完全可信。', lambda: '< 0.6（如 0.5、0.4）' },
]
const selectedExpertId = ref(null)
const selectedTemplateName = ref('')
const selectedExpertName = ref('')
const loading = ref(false)
const saving = ref(false)
const calculating = ref(false)
const simulating = ref(false)

// 指标结构数据
const levels = ref([])
const indicatorTree = ref(null)
const primaryMatrixData = reactive({}) // { levelName: [[{score, confidence}]] }
const secondaryMatrixData = reactive({}) // { levelName: { primaryCode: [[{score, confidence}]] } }

// 层级间矩阵数据
const levelMatrixData = ref([]) // [[{score, confidence}]]
const levelBetweenResult = ref(null)

// 结果数据
const levelResults = reactive({}) // { levelName: { primaryResult, secondaryResults: {} } }
const combinedResults = ref(null)

// Tab状态
const activeLevel = ref('')
const activePrimaryTabs = reactive({})

// 模拟
const simulateDialogVisible = ref(false)
const simulateExpertIds = ref([])

// 进度颜色
const progressColors = [
  { color: '#f56c6c', percentage: 25 },
  { color: '#e6a23c', percentage: 50 },
  { color: '#409eff', percentage: 75 },
  { color: '#67c23a', percentage: 100 },
]

// 计算属性
const hasData = computed(() => selectedTemplateId.value && selectedExpertId.value)

// 检查矩阵上三角是否有任意数据
const hasAnyUpperTriangleData = (matrix, size) => {
  if (!matrix) return false
  for (let i = 0; i < size; i++) {
    for (let j = i + 1; j < size; j++) {
      if (matrix[i]?.[j]?.score !== null && matrix[i]?.[j]?.score !== undefined) {
        return true
      }
    }
  }
  return false
}

const status = computed(() => {
  if (!hasData.value) return { totalMatrices: 0, completedMatrices: 0, progress: 0 }
  
  let totalMatrices = 0
  let completedMatrices = 0
  
  // 层级间矩阵
  if (levels.value.length > 1) {
    totalMatrices++
    if (hasAnyUpperTriangleData(levelMatrixData.value, levels.value.length)) completedMatrices++
  }
  
  // 一级维度矩阵
  for (const level of levels.value) {
    const primaries = getPrimariesForLevel(level)
    if (primaries.length > 1) {
      totalMatrices++
      if (hasAnyUpperTriangleData(primaryMatrixData.value?.[level], primaries.length)) completedMatrices++
    }
  }
  
  // 二级维度矩阵
  for (const level of levels.value) {
    const primaries = getPrimariesForLevel(level)
    for (const primary of primaries) {
      const secondaries = getSecondariesForLevel(level, primary.code)
      if (secondaries.length > 1) {
        totalMatrices++
        if (hasAnyUpperTriangleData(secondaryMatrixData.value?.[level]?.[primary.code], secondaries.length)) completedMatrices++
      }
    }
  }
  
  return {
    totalMatrices,
    completedMatrices,
    progress: totalMatrices > 0 ? Math.round(completedMatrices / totalMatrices * 100) : 0
  }
})

const hasCompleteResults = computed(() => {
  return combinedResults.value && combinedResults.value.allLeaves && combinedResults.value.allLeaves.length > 0
})

const totalWeight = computed(() => {
  if (!combinedResults.value) return 0
  return combinedResults.value.totalWeight || 0
})

const allConsistent = computed(() => {
  if (!combinedResults.value) return true
  return combinedResults.value.allConsistent !== false
})

const combinedWeightsTable = computed(() => {
  if (!combinedResults.value?.allLeaves) return []
  return combinedResults.value.allLeaves.map(leaf => ({
    levelName: leaf.levelName,
    primaryName: leaf.primaryName,
    primaryDescription: leaf.primaryDescription || '',
    secondaryName: leaf.secondaryName,
    secondaryDescription: leaf.secondaryDescription || '',
    metricType: leaf.metricType,
    primaryWeight: leaf.primaryWeight || 0,
    secondaryWeight: leaf.secondaryWeight || 0,
    combinedWeight: leaf.combinedWeight || 0,
  }))
})

// 方法
const loadTemplates = async () => {
  try {
    const res = await dynamicAhpApi.getDynamicAhpTemplates()
    templates.value = res || []
  } catch (e) {
    console.error('加载模板失败:', e)
  }
}

const loadExperts = async () => {
  try {
    const res = await expertApi.getExpertList()
    experts.value = res || []
  } catch (e) {
    console.error('加载专家失败:', e)
  }
}

const handleTemplateChange = () => {
  const tpl = templates.value.find(t => t.id === selectedTemplateId.value)
  selectedTemplateName.value = tpl?.templateName || ''
  resetData()
}

const handleExpertChange = () => {
  const exp = experts.value.find(e => e.expertId === selectedExpertId.value)
  selectedExpertName.value = exp?.expertName || ''
  resetData()
}

const resetData = () => {
  levels.value = []
  indicatorTree.value = null
  Object.keys(primaryMatrixData).forEach(k => delete primaryMatrixData[k])
  Object.keys(secondaryMatrixData).forEach(k => delete secondaryMatrixData[k])
  Object.keys(levelResults).forEach(k => delete levelResults[k])
  Object.keys(activePrimaryTabs).forEach(k => delete activePrimaryTabs[k])
  activeLevel.value = ''
  combinedResults.value = null
  levelMatrixData.value = []
  levelBetweenResult.value = null
}

const loadData = async () => {
  if (!selectedTemplateId.value || !selectedExpertId.value) {
    ElMessage.warning('请先选择模板和专家')
    return
  }

  loading.value = true
  try {
    // 加载指标树
    const tree = await dynamicAhpApi.getDynamicAhpTree(selectedTemplateId.value)
    indicatorTree.value = tree

    // 构建层级列表
    levels.value = tree?.levels?.map(l => l.name) || []
    if (levels.value.length > 0) {
      activeLevel.value = levels.value[0]
    }

    // 初始化数据结构
    initMatrixData()

    // 加载已有矩阵数据
    await loadExistingMatrices()

    // 加载配置状态
    const statusRes = await dynamicAhpApi.getDynamicAhpStatus(selectedTemplateId.value, selectedExpertId.value)
    if (statusRes) {
      // 更新进度
    }

    ElMessage.success('数据加载成功')
  } catch (e) {
    ElMessage.error('加载失败: ' + (e.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const initMatrixData = () => {
  if (!indicatorTree.value?.levels) return

  // 初始化层级间矩阵
  initLevelMatrixData()

  for (const levelNode of indicatorTree.value.levels) {
    const levelName = levelNode.name

    // 初始化一级维度矩阵
    const primaries = levelNode.primaryDimensions || []
    const n = primaries.length
    primaryMatrixData[levelName] = []
    for (let i = 0; i < n; i++) {
      primaryMatrixData[levelName][i] = []
      for (let j = 0; j < n; j++) {
        primaryMatrixData[levelName][i][j] = {
          score: null,
          confidence: 0.5,
          rowCode: primaries[i]?.code || `P${i}`,
          rowName: primaries[i]?.name || `维度${i}`,
          colCode: primaries[j]?.code || `P${j}`,
          colName: primaries[j]?.name || `维度${j}`,
        }
      }
    }

    // 初始化二级指标矩阵
    secondaryMatrixData[levelName] = {}
    for (const primary of primaries) {
      const secondaries = primary.secondaryDimensions || []
      const m = secondaries.length
      secondaryMatrixData[levelName][primary.code] = []
      for (let i = 0; i < m; i++) {
        secondaryMatrixData[levelName][primary.code][i] = []
        for (let j = 0; j < m; j++) {
          secondaryMatrixData[levelName][primary.code][i][j] = {
            score: null,
            confidence: 0.5,
            rowCode: secondaries[i]?.code || `S${i}`,
            rowName: secondaries[i]?.name || `指标${i}`,
            colCode: secondaries[j]?.code || `S${j}`,
            colName: secondaries[j]?.name || `指标${j}`,
          }
        }
      }

      // 设置当前活跃的一级维度Tab
      if (!activePrimaryTabs[levelName]) {
        activePrimaryTabs[levelName] = primaries[0]?.code || ''
      }
    }
  }
}

const initLevelMatrixData = () => {
  const n = levels.value.length
  levelMatrixData.value = []
  for (let i = 0; i < n; i++) {
    levelMatrixData.value[i] = []
    for (let j = 0; j < n; j++) {
      levelMatrixData.value[i][j] = {
        score: null,
        confidence: 0.5,
        rowCode: levels.value[i],
        rowName: levels.value[i],
        colCode: levels.value[j],
        colName: levels.value[j],
      }
    }
  }
}

const loadExistingMatrices = async () => {
  // 加载层级间矩阵
  if (levels.value.length > 1) {
    try {
      const levelMatrix = await dynamicAhpApi.getDynamicAhpLevelBetweenMatrix(
        selectedTemplateId.value, selectedExpertId.value
      )
      if (levelMatrix?.matrix) {
        for (const row of levelMatrix.matrix) {
          for (const cell of row) {
            if (cell.rowIndex !== undefined && cell.colIndex !== undefined && cell.score != null) {
              if (levelMatrixData.value[cell.rowIndex]?.[cell.colIndex]) {
                levelMatrixData.value[cell.rowIndex][cell.colIndex].score = parseFloat(cell.score)
                levelMatrixData.value[cell.rowIndex][cell.colIndex].confidence = cell.confidence != null ? parseFloat(cell.confidence) : 0.5
              }
            }
          }
        }
      }
    } catch (e) {
      console.warn('加载层级间矩阵失败:', e)
    }
  }

  for (const levelName of levels.value) {
    try {
      // 加载一级维度矩阵
      const primaryMatrix = await dynamicAhpApi.getDynamicAhpPrimaryBetweenMatrix(
        selectedTemplateId.value, levelName, selectedExpertId.value
      )
      if (primaryMatrix?.matrix) {
        for (const row of primaryMatrix.matrix) {
          for (const cell of row) {
            if (cell.rowIndex !== undefined && cell.colIndex !== undefined && cell.score != null) {
              if (primaryMatrixData[levelName]?.[cell.rowIndex]?.[cell.colIndex]) {
                primaryMatrixData[levelName][cell.rowIndex][cell.colIndex].score = parseFloat(cell.score)
                primaryMatrixData[levelName][cell.rowIndex][cell.colIndex].confidence = cell.confidence != null ? parseFloat(cell.confidence) : 0.5
              }
            }
          }
        }
      }

      // 加载二级指标矩阵
      const primaries = getPrimariesForLevel(levelName)
      for (const primary of primaries) {
        const secondaries = getSecondariesForLevel(levelName, primary.code)
        if (secondaries.length > 1) {
          try {
            const secMatrix = await dynamicAhpApi.getDynamicAhpSecondaryBetweenMatrix(
              selectedTemplateId.value, levelName, primary.code, selectedExpertId.value
            )
            if (secMatrix?.matrix) {
              for (const row of secMatrix.matrix) {
                for (const cell of row) {
                  if (cell.rowIndex !== undefined && cell.colIndex !== undefined && cell.score != null) {
                    if (secondaryMatrixData[levelName]?.[primary.code]?.[cell.rowIndex]?.[cell.colIndex]) {
                      secondaryMatrixData[levelName][primary.code][cell.rowIndex][cell.colIndex].score = parseFloat(cell.score)
                    }
                  }
                }
              }
            }
          } catch (e) {
            console.warn('加载二级矩阵失败:', e)
          }
        }
      }
    } catch (e) {
      console.warn('加载一级矩阵失败:', e)
    }
  }
}

const handleLevelChange = (levelName) => {
  activeLevel.value = levelName
  // 设置当前一级维度Tab
  const primaries = getPrimariesForLevel(levelName)
  if (primaries.length > 0 && !activePrimaryTabs[levelName]) {
    activePrimaryTabs[levelName] = primaries[0].code
  }
}

const getPrimariesForLevel = (levelName) => {
  if (!indicatorTree.value?.levels) return []
  const levelNode = indicatorTree.value.levels.find(l => l.name === levelName)
  return levelNode?.primaryDimensions || []
}

const getSecondariesForLevel = (levelName, primaryCode) => {
  const primaries = getPrimariesForLevel(levelName)
  const primary = primaries.find(p => p.code === primaryCode)
  return primary?.secondaryDimensions || []
}

const getSecondaryCount = (levelName, primaryCode) => {
  return getSecondariesForLevel(levelName, primaryCode).length
}

const onLevelCellChange = (rowIdx, colIdx, value) => {
  const val = parseFloat(value) || 1
  const clamped = Math.max(0.111, Math.min(9, val))
  levelMatrixData.value[rowIdx][colIdx].score = clamped
  if (levelMatrixData.value[colIdx]) {
    levelMatrixData.value[colIdx][rowIdx].score = 1 / clamped
  }
}

const getReciprocal = (value) => {
  if (!value || value === 0) return '—'
  const reciprocal = 1 / parseFloat(value)
  return reciprocal.toFixed(2)
}

const saveLevelMatrix = async () => {
  try {
    const entries = []

    for (let i = 0; i < levels.value.length; i++) {
      for (let j = i + 1; j < levels.value.length; j++) {
        const score = levelMatrixData.value[i][j].score
        if (score !== null && score !== undefined) {
          entries.push({
            rowCode: levelMatrixData.value[i][j].rowName,
            rowName: levelMatrixData.value[i][j].rowName,
            colCode: levelMatrixData.value[i][j].colName,
            colName: levelMatrixData.value[i][j].colName,
            score,
            confidence: levelMatrixData.value[i][j].confidence,
          })
        }
      }
    }

    if (entries.length === 0) return 0

    await dynamicAhpApi.saveDynamicAhpMatrix({
      expertId: selectedExpertId.value,
      expertName: selectedExpertName.value,
      templateId: selectedTemplateId.value,
      templateName: selectedTemplateName.value,
      levelName: 'LEVEL_BETWEEN',
      matrixType: 'LEVEL_BETWEEN',
      entries,
    })

    return entries.length
  } catch (e) {
    console.error('保存层级矩阵失败:', e)
    throw e
  }
}

const calculateLevelWeights = async () => {
  saving.value = true
  calculating.value = true
  try {
    // 先保存层级矩阵
    if (levels.value.length > 1) {
      await saveLevelMatrix()
    }

    // 计算权重
    const result = await dynamicAhpApi.calculateDynamicAhpWeights(
      selectedTemplateId.value,
      selectedExpertId.value,
      'LEVEL_BETWEEN',
      'LEVEL_BETWEEN',
      null
    )

    levelBetweenResult.value = result

    ElMessage.success('层级权重计算成功')
  } catch (e) {
    console.error('计算失败:', e)
    ElMessage.error('计算失败: ' + (e.message || '未知错误'))
  } finally {
    saving.value = false
    calculating.value = false
  }
}

const formatLevelWeights = computed(() => {
  if (!levelBetweenResult.value?.weights) return []
  return levelBetweenResult.value.weights.map((w, idx) => ({
    name: w.name,
    weight: w.weight,
  }))
})

const onPrimaryCellChange = (levelName, rowIdx, colIdx, value) => {
  // 确保是数字
  const val = parseFloat(value) || 1
  // 限制范围
  const clamped = Math.max(0.111, Math.min(9, val))
  primaryMatrixData[levelName][rowIdx][colIdx].score = clamped
  // 更新下三角
  if (primaryMatrixData[levelName][colIdx]) {
    primaryMatrixData[levelName][colIdx][rowIdx].score = 1 / clamped
  }
}

const onSecondaryCellChange = (levelName, primaryCode, rowIdx, colIdx, value) => {
  const val = parseFloat(value) || 1
  const clamped = Math.max(0.111, Math.min(9, val))
  secondaryMatrixData[levelName][primaryCode][rowIdx][colIdx].score = clamped
  if (secondaryMatrixData[levelName][primaryCode][colIdx]) {
    secondaryMatrixData[levelName][primaryCode][colIdx][rowIdx].score = 1 / clamped
  }
}

const savePrimaryMatrix = async (levelName) => {
  try {
    const primaries = getPrimariesForLevel(levelName)
    const entries = []

    for (let i = 0; i < primaries.length; i++) {
      for (let j = i + 1; j < primaries.length; j++) {
        const score = primaryMatrixData[levelName][i][j].score
        if (score !== null && score !== undefined) {
          entries.push({
            rowCode: primaryMatrixData[levelName][i][j].rowCode,
            rowName: primaryMatrixData[levelName][i][j].rowName,
            colCode: primaryMatrixData[levelName][i][j].colCode,
            colName: primaryMatrixData[levelName][i][j].colName,
            score,
            confidence: primaryMatrixData[levelName][i][j].confidence,
          })
        }
      }
    }

    if (entries.length === 0) return 0

    await dynamicAhpApi.saveDynamicAhpMatrix({
      expertId: selectedExpertId.value,
      expertName: selectedExpertName.value,
      templateId: selectedTemplateId.value,
      templateName: selectedTemplateName.value,
      levelName: levelName,
      matrixType: 'PRIMARY_BETWEEN',
      entries,
    })

    return entries.length
  } catch (e) {
    console.error('保存一级维度矩阵失败:', e)
    throw e
  }
}

const saveSecondaryMatrix = async (levelName, primaryCode) => {
  try {
    const secondaries = getSecondariesForLevel(levelName, primaryCode)
    const entries = []

    for (let i = 0; i < secondaries.length; i++) {
      for (let j = i + 1; j < secondaries.length; j++) {
        const score = secondaryMatrixData[levelName][primaryCode][i][j].score
        if (score !== null && score !== undefined) {
          entries.push({
            rowCode: secondaryMatrixData[levelName][primaryCode][i][j].rowCode,
            rowName: secondaryMatrixData[levelName][primaryCode][i][j].rowName,
            colCode: secondaryMatrixData[levelName][primaryCode][i][j].colCode,
            colName: secondaryMatrixData[levelName][primaryCode][i][j].colName,
            score,
            confidence: 0.5,
          })
        }
      }
    }

    if (entries.length === 0) return 0

    await dynamicAhpApi.saveDynamicAhpMatrix({
      expertId: selectedExpertId.value,
      expertName: selectedExpertName.value,
      templateId: selectedTemplateId.value,
      templateName: selectedTemplateName.value,
      levelName: levelName,
      matrixType: 'SECONDARY_BETWEEN',
      parentCode: primaryCode,
      entries,
    })

    return entries.length
  } catch (e) {
    console.error('保存二级指标矩阵失败:', e)
    throw e
  }
}

const calculatePrimaryWeights = async (levelName) => {
  saving.value = true
  calculating.value = true
  try {
    // 先保存当前层级的矩阵
    await savePrimaryMatrix(levelName)

    // 计算权重
    const result = await dynamicAhpApi.calculateDynamicAhpWeights(
      selectedTemplateId.value,
      selectedExpertId.value,
      levelName,
      'PRIMARY_BETWEEN',
      null
    )

    if (!levelResults[levelName]) {
      levelResults[levelName] = {}
    }
    levelResults[levelName].primaryResult = result

    ElMessage.success('权重计算成功')
  } catch (e) {
    console.error('计算失败:', e)
    ElMessage.error('计算失败: ' + (e.message || '未知错误'))
  } finally {
    saving.value = false
    calculating.value = false
  }
}

const calculateSecondaryWeights = async (levelName, primaryCode) => {
  saving.value = true
  calculating.value = true
  try {
    // 先保存当前矩阵
    await saveSecondaryMatrix(levelName, primaryCode)

    // 计算权重
    const result = await dynamicAhpApi.calculateDynamicAhpWeights(
      selectedTemplateId.value,
      selectedExpertId.value,
      levelName,
      'SECONDARY_BETWEEN',
      primaryCode
    )

    if (!levelResults[levelName]) {
      levelResults[levelName] = {}
    }
    if (!levelResults[levelName].secondaryResults) {
      levelResults[levelName].secondaryResults = {}
    }
    levelResults[levelName].secondaryResults[primaryCode] = result

    ElMessage.success('权重计算成功')
  } catch (e) {
    console.error('计算失败:', e)
    ElMessage.error('计算失败: ' + (e.message || '未知错误'))
  } finally {
    saving.value = false
    calculating.value = false
  }
}

const formatPrimaryWeights = (levelName) => {
  const result = levelResults[levelName]?.primaryResult
  if (!result?.weights) return []

  // 获取层级权重
  const levelWeight = levelBetweenResult.value?.weights?.find(
    w => w.name === levelName
  )?.weight || 0

  return result.weights.map(w => ({
    name: w.name,
    code: w.code,
    levelWeight,  // 层级权重
    weight: w.weight,  // 维度内权重
    combinedWeight: w.weight * levelWeight,  // 综合权重 = 维度内权重 × 层级权重
  }))
}

const formatSecondaryWeights = (levelName, primaryCode) => {
  const result = levelResults[levelName]?.secondaryResults?.[primaryCode]
  if (!result?.weights) return []

  // 获取层级权重
  const levelWeight = levelBetweenResult.value?.weights?.find(
    w => w.name === levelName
  )?.weight || 0

  // 获取一级维度权重
  const primaryWeight = levelResults[levelName]?.primaryResult?.weights?.find(
    w => w.code === primaryCode
  )?.weight || 0

  // 一级综合权重 = 层级权重 × 一级权重
  const primaryCombinedWeight = levelWeight * primaryWeight

  return result.weights.map(w => {
    const secondary = getSecondariesForLevel(levelName, primaryCode).find(s => s.code === w.code)
    return {
      name: w.name,
      code: w.code,
      weight: w.weight,  // 指标权重（维度内）
      metricType: secondary?.metricType,
      levelWeight,  // 层级权重
      primaryWeight,  // 一级权重
      primaryCombinedWeight,  // 一级综合权重
      // 二级综合权重 = 层级权重 × 一级权重 × 二级权重
      combinedWeight: w.weight * levelWeight * primaryWeight,
    }
  })
}

const calculateAllWeights = async () => {
  saving.value = true
  calculating.value = true
  try {
    // 先保存所有矩阵
    await saveAllMatrices()

    // 然后计算综合权重
    const result = await dynamicAhpApi.calculateAllDynamicAhpWeights(
      selectedTemplateId.value,
      selectedExpertId.value
    )
    combinedResults.value = result

    // 保存权重结果到数据库
    await dynamicAhpApi.saveDynamicAhpWeights(selectedTemplateId.value, selectedExpertId.value)

    ElMessage.success('保存并计算完成')
  } catch (e) {
    console.error('计算失败:', e)
    ElMessage.error('保存并计算失败: ' + (e.message || '未知错误'))
  } finally {
    saving.value = false
    calculating.value = false
  }
}

// 仅计算综合权重（不保存矩阵）
const calculateCombinedWeights = async () => {
  calculating.value = true
  try {
    const result = await dynamicAhpApi.calculateAllDynamicAhpWeights(
      selectedTemplateId.value,
      selectedExpertId.value
    )
    combinedResults.value = result
    ElMessage.success('计算完成')
  } catch (e) {
    console.error('计算失败:', e)
    ElMessage.error('计算失败: ' + (e.message || '未知错误'))
  } finally {
    calculating.value = false
  }
}

const saveAllMatrices = async () => {
  saving.value = true
  try {
    // 保存层级间矩阵
    if (levels.value.length > 1) {
      await saveLevelMatrix()
    }

    // 保存每个层级的一级维度矩阵
    for (const levelName of levels.value) {
      await savePrimaryMatrix(levelName)
    }

    // 保存每个层级的二级矩阵
    for (const levelName of levels.value) {
      const primaries = getPrimariesForLevel(levelName)
      for (const primary of primaries) {
        const secondaries = getSecondariesForLevel(levelName, primary.code)
        if (secondaries.length > 1) {
          await saveSecondaryMatrix(levelName, primary.code)
        }
      }
    }

    ElMessage.success('所有矩阵保存成功')
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

const saveAllWeights = async () => {
  saving.value = true
  try {
    await dynamicAhpApi.saveDynamicAhpWeights(selectedTemplateId.value, selectedExpertId.value)
    ElMessage.success('权重结果保存成功')
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

const openSimulateDialog = () => {
  simulateExpertIds.value = selectedExpertId.value ? [selectedExpertId.value] : []
  simulateDialogVisible.value = true
}

// 批量模拟：为所有专家生成模拟数据
const handleBatchSimulate = async () => {
  if (!selectedTemplateId.value) {
    ElMessage.warning('请先选择模板')
    return
  }

  simulating.value = true
  try {
    const count = await dynamicAhpApi.batchSimulateDynamicAhpScores(selectedTemplateId.value)
    ElMessage.success(`批量模拟成功，已生成 ${count} 条数据`)

    // 重新加载数据（如果已选择专家）
    if (selectedExpertId.value) {
      await loadData()
    }
  } catch (e) {
    console.error('批量模拟失败:', e)
    ElMessage.error('批量模拟失败: ' + (e.message || e.description || '未知错误'))
  } finally {
    simulating.value = false
  }
}

const handleSimulate = async () => {
  if (simulateExpertIds.value.length === 0) {
    ElMessage.warning('请选择至少一位专家')
    return
  }

  simulating.value = true
  try {
    await dynamicAhpApi.simulateDynamicAhpScores(selectedTemplateId.value, simulateExpertIds.value)
    ElMessage.success('模拟成功')
    simulateDialogVisible.value = false

    // 重新加载数据
    await loadData()
  } catch (e) {
    ElMessage.error('模拟失败: ' + (e.message || '未知错误'))
  } finally {
    simulating.value = false
  }
}

const handleClear = async () => {
  if (!selectedTemplateId.value || !selectedExpertId.value) return

  try {
    await dynamicAhpApi.clearDynamicAhpMatrix(selectedTemplateId.value, selectedExpertId.value)
    resetData()
    ElMessage.success('数据已清除')
  } catch (e) {
    ElMessage.error('清除失败: ' + (e.message || '未知错误'))
  }
}

// 生命周期
onMounted(() => {
  loadTemplates()
  loadExperts()
})
</script>

<style scoped lang="scss">
.dynamic-ahp-view {
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

.toolbar-card, .status-card, .level-between-card {
  margin-bottom: 16px;

  :deep(.el-card__body) {
    padding: 12px 16px;
  }
}

.level-between-card {
  :deep(.el-card__header) {
    background: linear-gradient(135deg, #1e3a5f 0%, #2d5a8a 100%);
    color: #ffffff;
    padding: 12px 16px;
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

.template-stats {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}

.status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 12px;

  .status-label {
    font-weight: 600;
    color: #303133;
  }

  .status-value {
    font-size: 13px;
    color: #606266;
  }
}

.guide-card {
  margin-bottom: 20px;

  :deep(.el-collapse-item__header) {
    font-weight: 600;
    font-size: 14px;
  }

  .guide-content {
    padding: 12px;

    p {
      margin: 0 0 12px;
      font-size: 13px;
      color: #606266;
      line-height: 1.8;
    }
  }

  .guide-table {
    margin-top: 8px;

    :deep(.el-table__header th) {
      background: #f5f7fa;
      font-weight: 600;
    }
  }
}

.ahp-content {
  .level-card, .result-card {
    margin-bottom: 16px;

    :deep(.el-card__header) {
      padding: 12px 16px;
      background: linear-gradient(135deg, #1e3a5f 0%, #2d5a8a 100%);
      color: #ffffff;
    }

    .description-text {
      font-size: 11px;
      color: #909399;
      line-height: 1.3;
      margin-top: 2px;
    }
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  color: #ffffff;

  .el-icon {
    margin-right: 8px;
  }

  .header-actions {
    display: flex;
    gap: 8px;
  }
}

.level-tabs {
  margin-top: 12px;
}

.matrix-section {
  margin-top: 24px;

  .section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;
    padding-bottom: 10px;
    border-bottom: 2px solid var(--el-color-primary);
    color: #303133;
    font-size: 16px;

    .el-icon {
      color: var(--el-color-primary);
    }
  }
}

.secondary-section {
  margin-top: 32px;
}

.matrix-card {
  background: #fafbfc;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
}

.matrix-guide {
  margin-bottom: 16px;
  padding: 12px;
  background: #fff;
  border-radius: 4px;
  border-left: 3px solid var(--el-color-primary);

  p {
    margin: 0 0 4px;
    font-size: 13px;
    color: #606266;
    line-height: 1.6;

    &:last-child {
      margin-bottom: 0;
    }
  }
}

.ahp-matrix-scroll {
  overflow-x: auto;
  margin-bottom: 16px;
}

.ahp-matrix-table {
  border-collapse: collapse;
  background: white;
  border: 2px solid var(--el-color-primary);
  border-radius: 8px;

  th, td {
    border: 1px solid #e4e7ed;
    padding: 0;
    vertical-align: middle;
  }

  .matrix-corner-cell {
    width: 180px;
    min-width: 180px;
    background: linear-gradient(135deg, var(--el-color-primary), #2d5a8a);
  }

  .matrix-header-cell {
    width: 160px;
    min-width: 160px;
    padding: 12px;
    text-align: center;
    font-weight: 600;
    background: linear-gradient(135deg, var(--el-color-primary), #2d5a8a);
    color: white;
  }

  .matrix-row-label {
    width: 180px;
    min-width: 180px;
    padding: 12px;
    font-weight: 600;
    background: #f5f7fa;
    text-align: left;
  }

  .matrix-cell {
    width: 160px;
    min-width: 160px;
    min-height: 40px;
    padding: 4px 6px;
    text-align: center;
    vertical-align: middle;

    &.cell-diagonal {
      background: #f0f9ff;
      color: var(--el-color-primary);
      font-weight: bold;
      font-size: 16px;
    }

    &.cell-editable {
      background: #fff;
    }

    &.cell-lower {
      background: #f5f7fa;
    }

    .cell-inputs {
      display: flex;
      flex-direction: row;
      align-items: center;
      justify-content: center;
      gap: 2px;
      font-size: 11px;

      .el-input-number {
        width: 55px;

        :deep(.el-input__wrapper) {
          padding: 0 4px;
        }

        :deep(.el-input__inner) {
          font-size: 11px;
        }
      }

      .conf-label {
        font-size: 11px;
        color: #606266;
        background: #f5f7fa;
        padding: 2px 4px;
        border-radius: 3px;
        font-weight: 500;
        white-space: nowrap;
        cursor: help;
      }
    }

    .confidence-row {
      display: flex;
      align-items: center;
      gap: 2px;
      font-size: 11px;
      color: #909399;
      white-space: nowrap;

      .conf-label {
        white-space: nowrap;
      }

      :deep(.el-input-number) {
        width: 70px;
      }

      :deep(.el-input-number .el-input__wrapper) {
        padding: 0 4px;
      }
    }

    .auto-value {
      color: #999;
      font-style: italic;
      font-size: 11px;
    }
  }

  &.small-table {
    font-size: 12px;

    th, td {
      padding: 4px;
    }

    .matrix-corner-cell {
      width: 140px;
      min-width: 140px;
    }

    .matrix-header-cell {
      width: 100px;
      min-width: 100px;
      padding: 6px;
      font-size: 12px;
    }

    .matrix-row-label {
      width: 120px;
      min-width: 120px;
      padding: 6px;
      font-size: 12px;
    }

    .matrix-cell {
      width: 120px;
      min-width: 120px;
      min-height: 36px;
      padding: 2px 4px;

      &.cell-editable {
        .el-input-number {
          width: 48px;
        }
      }

      .cell-inputs {
        font-size: 10px;
        gap: 1px;

        .el-input-number {
          width: 48px;

          :deep(.el-input__wrapper) {
            padding: 0 3px;
          }

          :deep(.el-input__inner) {
            font-size: 10px;
          }
        }

        .conf-label {
          font-size: 10px;
          padding: 1px 2px;
        }
      }

      .auto-value {
        font-size: 10px;
      }
    }
  }
}

.matrix-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #e4e7ed;
}

.weight-result {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #e4e7ed;

  h4 {
    margin: 0 0 12px;
    font-size: 14px;
    color: #303133;
  }
}

.primary-tabs {
  margin-top: 12px;
}

// 表头样式
:deep(.el-table th.el-table__cell) {
  background-color: #304156 !important;
  color: #ffffff !important;
  font-weight: 600;
}
</style>
