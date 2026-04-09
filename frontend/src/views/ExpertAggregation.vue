<template>
  <div class="expert-aggregation-container">
    <!-- 页面标题：离散度分析 -->
    <div v-if="isDispersion" class="page-header">
      <div class="page-header-row">
        <div>
          <h2>
            <el-icon><DataAnalysis /></el-icon>
            权重离散度分析
          </h2>
          <p class="subtitle">计算各专家权重的变异系数(CV)，分析专家意见一致性</p>
        </div>
        <el-button type="primary" :icon="Refresh" @click="loadData" :loading="loading">
          刷新数据
        </el-button>
      </div>
    </div>

    <!-- 页面标题：权重集结打分 -->
    <div v-if="isCollective" class="page-header">
      <div>
        <h2>
          <el-icon><Flag /></el-icon>
          权重集结打分
        </h2>
        <p class="subtitle">专家权重明细、一致性检验（CR）、集体判断矩阵与指标/综合评估得分</p>
      </div>
    </div>

    <!-- 一致性等级说明 -->
    <el-card v-if="isDispersion" class="legend-card" shadow="hover">
      <template #header>
        <span><el-icon><InfoFilled /></el-icon> 一致性等级说明</span>
      </template>
      <div class="legend-items">
        <div class="legend-item">
          <el-tag type="success" size="large">高度一致</el-tag>
          <span class="legend-range">CV ≤ 15%</span>
        </div>
        <div class="legend-item">
          <el-tag type="warning" size="large">较为一致</el-tag>
          <span class="legend-range">15% < CV ≤ 25%</span>
        </div>
        <div class="legend-item">
          <el-tag type="warning" size="large" effect="plain">轻度分歧</el-tag>
          <span class="legend-range">25% < CV ≤ 35%</span>
        </div>
        <div class="legend-item">
          <el-tag type="danger" size="large">严重分歧</el-tag>
          <span class="legend-range">CV > 35%</span>
        </div>
      </div>
    </el-card>

    <!-- 参与计算的专家（合并两类来源） -->
    <el-card v-if="isDispersion" class="experts-card" shadow="hover">
      <template #header>
        <span><el-icon><User /></el-icon> 参与计算的专家 (共 {{ participatingExperts?.length || 0 }} 位)</span>
      </template>
      <div class="expert-ids">
        <el-tag
          v-for="e in participatingExperts"
          :key="e.expertId"
          type="info"
          class="expert-tag"
        >
          ID: {{ e.expertId }}<template v-if="e.expertName"> · {{ e.expertName }}</template>
        </el-tag>
        <el-empty v-if="!participatingExperts?.length" description="暂无数据" :image-size="60" />
      </div>
    </el-card>

    <!-- ==================== 离散度分析：效能指标 / 装备操作（顶层切换） ==================== -->
    <div v-if="isDispersion" class="cv-domain-switch-wrap">
      <el-card class="cv-top-switch-card" shadow="never">
        <div class="cv-top-switch-row">
          <span class="cv-top-switch-label">分析域</span>
          <el-radio-group v-model="activeTopTab" size="large" class="cv-top-radio">
            <el-radio-button value="effectiveness">效能指标</el-radio-button>
            <el-radio-button value="equipment">装备操作</el-radio-button>
          </el-radio-group>
        </div>
      </el-card>

      <div v-show="activeTopTab === 'effectiveness'" class="cv-domain-panel">
        <!-- 一级维度权重 CV（维度层） -->
        <el-card class="cv-card cv-domain-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span><el-icon><Grid /></el-icon> 一、一级维度权重 CV 分析</span>
            </div>
          </template>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-table :data="dimensionTableData" border stripe size="small">
                <el-table-column prop="indicatorName" label="维度" min-width="100" />
                <el-table-column prop="allMean" label="所有数据均值" align="center" width="120">
                  <template #default="{ row }">
                    {{ (row.allMean * 100).toFixed(2) }}%
                  </template>
                </el-table-column>
                <el-table-column prop="filteredMean" label="过滤后均值" align="center" width="120">
                  <template #default="{ row }">
                    {{ (row.filteredMean * 100).toFixed(2) }}%
                  </template>
                </el-table-column>
                <el-table-column prop="allCv" label="所有数据CV" align="center" width="100">
                  <template #default="{ row }">
                    <el-tag :type="getLevelType(row.allCv)" size="small" effect="dark">
                      {{ row.allCv.toFixed(2) }}%
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="filteredCv" label="过滤后CV" align="center" width="100">
                  <template #default="{ row }">
                    <el-tag :type="getLevelType(row.filteredCv)" size="small" effect="dark">
                      {{ row.filteredCv.toFixed(2) }}%
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="allLevel" label="所有数据一致性" align="center" width="110">
                  <template #default="{ row }">
                    <el-tag :type="getLevelType(row.allCv)" size="small">{{ row.allLevel }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="filteredLevel" label="过滤后一致性" align="center" width="110">
                  <template #default="{ row }">
                    <el-tag :type="getLevelType(row.filteredCv)" size="small">{{ row.filteredLevel }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-col>
            <el-col :span="12">
              <div ref="dimensionChartRef" class="chart-container"></div>
            </el-col>
          </el-row>
        </el-card>

        <!-- 二级指标权重 CV（指标层） -->
        <el-card class="cv-card cv-domain-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span><el-icon><List /></el-icon> 二、二级指标权重 CV 分析</span>
              <el-tag type="info">共 {{ indicatorTableData?.length || 0 }} 个指标</el-tag>
            </div>
          </template>
          <el-tabs v-model="activeDimensionTab" type="border-card">
            <el-tab-pane
              v-for="dim in dimensionTabs"
              :key="dim.code"
              :label="dim.name"
              :name="dim.code"
            >
              <el-table :data="getIndicatorsByDimension(dim.code)" border stripe size="small" max-height="400">
                <el-table-column prop="indicatorName" label="指标" min-width="120" />
                <el-table-column prop="allMean" label="所有数据均值" align="center" width="110">
                  <template #default="{ row }">
                    {{ (row.allMean * 100).toFixed(4) }}%
                  </template>
                </el-table-column>
                <el-table-column prop="filteredMean" label="过滤后均值" align="center" width="110">
                  <template #default="{ row }">
                    {{ (row.filteredMean * 100).toFixed(4) }}%
                  </template>
                </el-table-column>
                <el-table-column prop="allCv" label="所有数据CV" align="center" width="100">
                  <template #default="{ row }">
                    <el-tag :type="getLevelType(row.allCv)" size="small" effect="dark">
                      {{ row.allCv.toFixed(2) }}%
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="filteredCv" label="过滤后CV" align="center" width="100">
                  <template #default="{ row }">
                    <el-tag :type="getLevelType(row.filteredCv)" size="small" effect="dark">
                      {{ row.filteredCv.toFixed(2) }}%
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="allLevel" label="所有数据一致性" align="center" width="110">
                  <template #default="{ row }">
                    <el-tag :type="getLevelType(row.allCv)" size="small">{{ row.allLevel }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="filteredLevel" label="过滤后一致性" align="center" width="110">
                  <template #default="{ row }">
                    <el-tag :type="getLevelType(row.filteredCv)" size="small">{{ row.filteredLevel }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
            <el-tab-pane label="全部" name="eff-all">
              <el-table :data="indicatorTableData" border stripe size="small" max-height="400">
                <el-table-column prop="dimension" label="维度" width="120" />
                <el-table-column prop="indicatorName" label="指标" min-width="120" />
                <el-table-column prop="allMean" label="所有数据均值" align="center" width="110">
                  <template #default="{ row }">
                    {{ (row.allMean * 100).toFixed(4) }}%
                  </template>
                </el-table-column>
                <el-table-column prop="filteredMean" label="过滤后均值" align="center" width="110">
                  <template #default="{ row }">
                    {{ (row.filteredMean * 100).toFixed(4) }}%
                  </template>
                </el-table-column>
                <el-table-column prop="allCv" label="所有数据CV" align="center" width="100">
                  <template #default="{ row }">
                    <el-tag :type="getLevelType(row.allCv)" size="small" effect="dark">
                      {{ row.allCv.toFixed(2) }}%
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="filteredCv" label="过滤后CV" align="center" width="100">
                  <template #default="{ row }">
                    <el-tag :type="getLevelType(row.filteredCv)" size="small" effect="dark">
                      {{ row.filteredCv.toFixed(2) }}%
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="allLevel" label="所有数据一致性" align="center" width="110">
                  <template #default="{ row }">
                    <el-tag :type="getLevelType(row.allCv)" size="small">{{ row.allLevel }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="filteredLevel" label="过滤后一致性" align="center" width="110">
                  <template #default="{ row }">
                    <el-tag :type="getLevelType(row.filteredCv)" size="small">{{ row.filteredLevel }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
          <div class="chart-section">
            <h4><el-icon><DataLine /></el-icon> 效能指标二级 CV 对比图</h4>
            <div ref="effIndicatorChartRef" class="chart-container-large"></div>
          </div>
        </el-card>
      </div>

      <div v-show="activeTopTab === 'equipment'" class="cv-domain-panel">
        <!-- 一级维度 CV（维度层） -->
        <el-card class="cv-card cv-domain-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span><el-icon><Grid /></el-icon> 一、一级维度权重 CV 分析</span>
            </div>
          </template>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-table :data="equipmentDimensionTableData" border stripe size="small">
                <el-table-column prop="indicatorName" label="维度" min-width="100" />
                <el-table-column prop="allMean" label="所有数据均值" align="center" width="120">
                  <template #default="{ row }">
                    {{ (row.allMean * 100).toFixed(2) }}%
                  </template>
                </el-table-column>
                <el-table-column prop="filteredMean" label="过滤后均值" align="center" width="120">
                  <template #default="{ row }">
                    {{ (row.filteredMean * 100).toFixed(2) }}%
                  </template>
                </el-table-column>
                <el-table-column prop="allCv" label="所有数据CV" align="center" width="100">
                  <template #default="{ row }">
                    <el-tag :type="getLevelType(row.allCv)" size="small" effect="dark">
                      {{ row.allCv.toFixed(2) }}%
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="filteredCv" label="过滤后CV" align="center" width="100">
                  <template #default="{ row }">
                    <el-tag :type="getLevelType(row.filteredCv)" size="small" effect="dark">
                      {{ row.filteredCv.toFixed(2) }}%
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="allLevel" label="所有数据一致性" align="center" width="110">
                  <template #default="{ row }">
                    <el-tag :type="getLevelType(row.allCv)" size="small">{{ row.allLevel }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="filteredLevel" label="过滤后一致性" align="center" width="110">
                  <template #default="{ row }">
                    <el-tag :type="getLevelType(row.filteredCv)" size="small">{{ row.filteredLevel }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty
                v-if="!equipmentDimensionTableData?.length"
                description="暂无装备操作 AHP 数据；请先在「装备操作 AHP」中保存或批量模拟入库"
                :image-size="72"
                style="margin: 16px 0"
              />
            </el-col>
            <el-col :span="12">
              <div ref="eqDimensionChartRef" class="chart-container"></div>
            </el-col>
          </el-row>
        </el-card>

        <!-- 二级指标 CV（指标层） -->
        <el-card class="cv-card cv-domain-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span><el-icon><List /></el-icon> 二、二级指标权重 CV 分析</span>
              <el-tag type="info">共 {{ equipmentIndicatorTableData?.length || 0 }} 个指标</el-tag>
            </div>
          </template>
          <el-tabs v-model="activeEqDimTab" type="border-card">
            <el-tab-pane
              v-for="dim in equipmentDimensionTabs"
              :key="dim.code"
              :label="dim.name"
              :name="dim.code"
            >
              <el-table :data="getEqIndicatorsByDimension(dim.name)" border stripe size="small" max-height="400">
                <el-table-column prop="indicatorName" label="指标" min-width="120" />
                <el-table-column prop="allMean" label="所有数据均值" align="center" width="110">
                  <template #default="{ row }">
                    {{ (row.allMean * 100).toFixed(4) }}%
                  </template>
                </el-table-column>
                <el-table-column prop="filteredMean" label="过滤后均值" align="center" width="110">
                  <template #default="{ row }">
                    {{ (row.filteredMean * 100).toFixed(4) }}%
                  </template>
                </el-table-column>
                <el-table-column prop="allCv" label="所有数据CV" align="center" width="100">
                  <template #default="{ row }">
                    <el-tag :type="getLevelType(row.allCv)" size="small" effect="dark">
                      {{ row.allCv.toFixed(2) }}%
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="filteredCv" label="过滤后CV" align="center" width="100">
                  <template #default="{ row }">
                    <el-tag :type="getLevelType(row.filteredCv)" size="small" effect="dark">
                      {{ row.filteredCv.toFixed(2) }}%
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="allLevel" label="所有数据一致性" align="center" width="110">
                  <template #default="{ row }">
                    <el-tag :type="getLevelType(row.allCv)" size="small">{{ row.allLevel }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="filteredLevel" label="过滤后一致性" align="center" width="110">
                  <template #default="{ row }">
                    <el-tag :type="getLevelType(row.filteredCv)" size="small">{{ row.filteredLevel }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
            <el-tab-pane label="全部" name="eq-all">
              <el-table :data="equipmentIndicatorTableData" border stripe size="small" max-height="400">
                <el-table-column prop="dimension" label="维度" width="140" />
                <el-table-column prop="indicatorName" label="指标" min-width="120" />
                <el-table-column prop="allMean" label="所有数据均值" align="center" width="110">
                  <template #default="{ row }">
                    {{ (row.allMean * 100).toFixed(4) }}%
                  </template>
                </el-table-column>
                <el-table-column prop="filteredMean" label="过滤后均值" align="center" width="110">
                  <template #default="{ row }">
                    {{ (row.filteredMean * 100).toFixed(4) }}%
                  </template>
                </el-table-column>
                <el-table-column prop="allCv" label="所有数据CV" align="center" width="100">
                  <template #default="{ row }">
                    <el-tag :type="getLevelType(row.allCv)" size="small" effect="dark">
                      {{ row.allCv.toFixed(2) }}%
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="filteredCv" label="过滤后CV" align="center" width="100">
                  <template #default="{ row }">
                    <el-tag :type="getLevelType(row.filteredCv)" size="small" effect="dark">
                      {{ row.filteredCv.toFixed(2) }}%
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="allLevel" label="所有数据一致性" align="center" width="110">
                  <template #default="{ row }">
                    <el-tag :type="getLevelType(row.allCv)" size="small">{{ row.allLevel }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="filteredLevel" label="过滤后一致性" align="center" width="110">
                  <template #default="{ row }">
                    <el-tag :type="getLevelType(row.filteredCv)" size="small">{{ row.filteredLevel }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty
                v-if="!equipmentIndicatorTableData?.length"
                description="暂无装备操作 AHP 数据；请先在「装备操作 AHP」中保存或批量模拟入库"
                :image-size="72"
                style="margin: 16px 0"
              />
            </el-tab-pane>
          </el-tabs>
          <div class="chart-section">
            <h4><el-icon><DataLine /></el-icon> 装备操作二级 CV 对比图</h4>
            <div ref="eqIndicatorChartRef" class="chart-container-large"></div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- ==================== 专家集结计算（对比打分层） ==================== -->
    <template v-if="isCollective">
    <el-divider content-position="left">
      <el-icon><TrendCharts /></el-icon> 一、专家集结计算
    </el-divider>

    <!-- 集结参数配置 -->
    <el-card class="collective-card" shadow="hover">
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
    <el-card v-if="activeCollectiveWeights" class="collective-card" shadow="hover">
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

    <!-- ==================== 2. 集结计算流程说明 ==================== -->
    <el-card v-if="activeCollectiveWeights" class="collective-card collective-card--info" shadow="hover">
      <template #header>
        <span><el-icon><Guide /></el-icon> 二、集结计算流程</span>
      </template>
      <el-steps :active="5" align-center finish-status="success">
        <el-step title="Step 1" description="选定参与专家" />
        <el-step title="Step 2" description="加载可信度（Cred_i）" />
        <el-step title="Step 3" description="计算各 key 权重" />
        <el-step title="Step 4" description="加权平均构造矩阵" />
        <el-step title="Step 5" description="AHP + 综合权重" />
      </el-steps>
      <el-divider />
      <div class="calc-flow-detail">
        <div class="flow-step">
          <el-tag type="primary">Step 1</el-tag>
          <span>选定参与集结的专家（指定或使用全部专家）。</span>
        </div>
        <div class="flow-step">
          <el-tag type="primary">Step 2</el-tag>
          <span>加载每位专家的可信度得分 <code>Cred_i</code>（来自可信度评估表，取值 0~100）。</span>
        </div>
        <div class="flow-step">
          <el-tag type="primary">Step 3</el-tag>
          <span>
            对每个 <code>comparison_key</code>（每对两两比较），计算专家 i 在该 key 上的综合权重：
            <code>W<sub>i,k</sub> = (Cred_i / 100) × 0.5 + confidence<sub>i,k</sub> × 0.5</code>，
            再归一化：<code>w<sub>i,k</sub> = W<sub>i,k</sub> / Σ W<sub>j,k</sub></code>。
            归一化在每个 comparison_key <strong>内部独立进行</strong>，因此每对的专家权重系数都不同。
          </span>
        </div>
        <div class="flow-step">
          <el-tag type="primary">Step 4</el-tag>
          <span>
            对每个 <code>comparison_key</code>，用对应的 <code>w<sub>i,k</sub></code> 加权平均构造集体判断值：
            <code>collective_score<sub>k</sub> = Σ ( w<sub>i,k</sub> × score<sub>i,k</sub> )</code>，
            限制在 [1/9, 9] 范围内。
          </span>
        </div>
        <div class="flow-step">
          <el-tag type="primary">Step 5</el-tag>
          <span>
            构造 6 个集体判断矩阵（维度层 + 5 个指标层），分别执行 AHP 层次单排序得权重向量，
            计算 λ<sub>max</sub> 和 CR（一致性比率）。综合权重 = 维度权重 × 指标层权重，归一化至和为 1。
          </span>
        </div>
      </div>
    </el-card>

    <!-- 三、权重集结打分（含 CR 检验及后续矩阵/综合权重） -->
    <el-divider v-if="activeCollectiveWeights" content-position="left">
      <el-icon><Flag /></el-icon> 三、权重集结打分
    </el-divider>

    <!-- ==================== 一致性检验 ==================== -->
    <el-card v-if="activeCollectiveWeights" class="collective-card" shadow="hover">
      <template #header>
        <span><el-icon><PieChart /></el-icon> 一致性检验（CR &lt; 0.1 为通过）</span>
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
    <el-card v-if="activeCollectiveWeights" class="collective-card" shadow="hover">
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
    <el-card v-if="activeCollectiveWeights" class="collective-card" shadow="hover">
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

    <el-alert
      v-if="isCollective"
      type="success"
      :closable="false"
      show-icon
      class="collective-follow-hint"
      title="原始指标与综合评估得分已移至左侧菜单「评估结果计算 → 综合打分」。请在本页执行集结计算后，到该页选择同一批次并点击「加载综合结果」。"
    />
    </template>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import request from '../utils/request'
import {
  getCollectiveEvaluationIds,
  previewCollectiveWeights,
  executeCollectiveCalculation
} from '../api/index'
import {
  Refresh,
  DataAnalysis,
  InfoFilled,
  Grid,
  List,
  User,
  DataLine,
  TrendCharts,
  Setting,
  View,
  Cpu,
  PieChart,
  Flag,
  Guide
} from '@element-plus/icons-vue'

const props = defineProps({
  pageMode: {
    type: String,
    required: true,
    validator: (v) => v === 'dispersion' || v === 'collective'
  }
})

const isDispersion = computed(() => props.pageMode === 'dispersion')
const isCollective = computed(() => props.pageMode === 'collective')

// ==================== 现有状态（原CV分析） ====================
const loading = ref(false)
const aggregationResult = ref(null)
const activeTopTab = ref('effectiveness')
const activeDimensionTab = ref('security')
const activeEqDimTab = ref('eq-all')
const dimensionChartRef = ref(null)
const effIndicatorChartRef = ref(null)
const eqDimensionChartRef = ref(null)
const eqIndicatorChartRef = ref(null)
let dimensionChart = null
let effIndicatorChart = null
let eqDimensionChart = null
let eqIndicatorChart = null

// ==================== 集结计算状态 ====================
const collectiveLoading = ref(false)
const previewLoading = ref(false)
const calculateLoading = ref(false)
const evaluationIds = ref([])
const availableExperts = ref([])
const weightPreview = ref(null)
const collectiveResult = ref(null)

// 矩阵展示相关
const selectedMatrix = ref('dim')
const dimWeightChartRef = ref(null)
const indWeightChartRef = ref(null)
let dimWeightChart = null
let indWeightChart = null

function disposeDispersionCharts() {
  dimensionChart?.dispose()
  effIndicatorChart?.dispose()
  eqDimensionChart?.dispose()
  eqIndicatorChart?.dispose()
  dimensionChart = null
  effIndicatorChart = null
  eqDimensionChart = null
  eqIndicatorChart = null
}

function disposeCollectiveCharts() {
  dimWeightChart?.dispose()
  indWeightChart?.dispose()
  dimWeightChart = null
  indWeightChart = null
}

const collectiveForm = ref({
  evaluationId: '',
  useAllExperts: true,
  expertIds: []
})

/** 优先使用最近一次「执行集结计算」结果，否则用「预览」数据，保证矩阵与图表有数据 */
const activeCollectiveWeights = computed(() => collectiveResult.value ?? weightPreview.value)

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

// 装备操作维度（动态取后端维度名）
const equipmentDimensionTabs = computed(() => {
  const eqDims = allData.value?.equipmentDimensionCvs || []
  return eqDims.map((item) => ({
    code: item.indicatorCode,
    name: item.indicatorName
  }))
})

// 计算属性
const allData = computed(() => aggregationResult.value?.allDataResult || null)
const filteredExtreme = computed(() => aggregationResult.value?.filteredExtremeResult || null)

/** 所有数据方案下的参与专家（含 id、名称）；合并效能权重快照与装备操作打分两类来源 */
const participatingExperts = computed(() => {
  const eff = allData.value?.participatingExperts || []
  const eq = allData.value?.equipmentParticipatingExperts || []
  const map = new Map()
  for (const e of eff) {
    if (e?.expertId != null) map.set(e.expertId, e)
  }
  for (const e of eq) {
    if (e?.expertId != null && !map.has(e.expertId)) map.set(e.expertId, e)
  }
  if (map.size) return Array.from(map.values())
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

/** 装备操作维度层 CV */
const equipmentDimensionTableData = computed(() => {
  const allEq = allData.value?.equipmentDimensionCvs ?? []
  const filteredEq = filteredExtreme.value?.equipmentDimensionCvs ?? []
  if (!allEq.length) return []
  return allEq.map((item) => {
    const filtered = filteredEq.find((f) => f.indicatorCode === item.indicatorCode) || {}
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

/** 装备操作二级指标 CV（后端由 comparison_key 前缀 装备操作_ 现场计算） */
const equipmentIndicatorTableData = computed(() => {
  const allEq = allData.value?.equipmentIndicatorCvs ?? []
  const filteredEq = filteredExtreme.value?.equipmentIndicatorCvs ?? []
  if (!allEq.length) return []
  return allEq.map((item) => {
    const filtered = filteredEq.find((f) => f.indicatorCode === item.indicatorCode) || {}
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

/** 装备操作按维度（中文名）筛选指标 */
function getEqIndicatorsByDimension(dimName) {
  return equipmentIndicatorTableData.value.filter((item) => item.dimension === dimName)
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

// ==================== 图表渲染（集结：维度/指标权重图；综合得分图已迁至「综合打分」页） ====================

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

async function loadData() {
  loading.value = true
  try {
    // request 拦截器已解包为后端 data 字段，此处即为聚合结果对象
    const data = await request.get('/evaluation/expert-aggregation/cv-result')
    aggregationResult.value = data
    ElMessage.success('数据加载成功')
    nextTick(() => {
      renderDimensionChart()
      renderIndicatorChart()
      renderEqDimensionChart()
      renderEqIndicatorChart()
    })
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error(error?.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

function renderDimensionChart() {
  if (!dimensionChartRef.value) return

  if (!dimensionChart) {
    dimensionChart = echarts.init(dimensionChartRef.value)
  }

  const allDims = allData.value?.dimensionCvs || []
  const filteredDims = filteredExtreme.value?.dimensionCvs || []

  if (allDims.length === 0) return

  const option = {
    title: {
      text: '一级维度 CV 对比',
      left: 'center',
      textStyle: { fontSize: 14 }
    },
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        let result = params[0].name + '<br/>'
        params.forEach(p => {
          result += `${p.seriesName}: ${p.value.toFixed(2)}%<br/>`
        })
        return result
      }
    },
    legend: {
      data: ['所有数据', '去除极端值'],
      top: 30
    },
    xAxis: {
      type: 'category',
      data: allDims.map(item => item.indicatorName),
      axisLabel: { rotate: 15, fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      name: 'CV(%)',
      max: 50,
      axisLabel: { formatter: '{value}%' }
    },
    series: [
      {
        name: '所有数据',
        type: 'bar',
        data: allDims.map(item => ({
          value: item.cv,
          itemStyle: { color: '#409EFF' }
        })),
        label: { show: false }
      },
      {
        name: '去除极端值',
        type: 'bar',
        data: allDims.map(item => {
          const filtered = filteredDims.find(f => f.indicatorCode === item.indicatorCode)
          return {
            value: filtered ? filtered.cv : 0,
            itemStyle: { color: '#67C23A' }
          }
        }),
        label: { show: false }
      }
    ],
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true }
  }
  dimensionChart.setOption(option)
  dimensionChart.resize()
}

function renderIndicatorChart() {
  if (!effIndicatorChartRef.value) return
  if (!effIndicatorChart) {
    effIndicatorChart = echarts.init(effIndicatorChartRef.value)
  }

  const allIndicators = allData.value?.indicatorCvs || []
  const filteredIndicators = filteredExtreme.value?.indicatorCvs || []
  if (allIndicators.length === 0) {
    effIndicatorChart.clear()
    return
  }

  const option = {
    title: { text: '效能指标二级 CV 对比', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        let result = params[0].name + '<br/>'
        params.forEach(p => { result += `${p.seriesName}: ${p.value.toFixed(2)}%<br/>` })
        return result
      }
    },
    legend: { data: ['所有数据', '去除极端值'], top: 30 },
    xAxis: { type: 'category', data: allIndicators.map((item) => item.indicatorName), axisLabel: { rotate: 30, fontSize: 10 } },
    yAxis: { type: 'value', name: 'CV(%)', axisLabel: { formatter: '{value}%' } },
    series: [
      { name: '所有数据', type: 'line', data: allIndicators.map((item) => item.cv), itemStyle: { color: '#409EFF' }, lineStyle: { width: 2 }, smooth: true },
      {
        name: '去除极端值',
        type: 'line',
        data: allIndicators.map((item) => {
          const filtered = filteredIndicators.find((f) => f.indicatorCode === item.indicatorCode)
          return filtered ? filtered.cv : 0
        }),
        itemStyle: { color: '#67C23A' },
        lineStyle: { width: 2, type: 'dashed' },
        smooth: true
      }
    ],
    grid: { left: '3%', right: '4%', bottom: '20%', containLabel: true }
  }
  effIndicatorChart.setOption(option)
  effIndicatorChart.resize()
}

function renderEqDimensionChart() {
  if (!eqDimensionChartRef.value) return
  if (!eqDimensionChart) {
    eqDimensionChart = echarts.init(eqDimensionChartRef.value)
  }
  const allDims = allData.value?.equipmentDimensionCvs || []
  const filteredDims = filteredExtreme.value?.equipmentDimensionCvs || []
  if (!allDims.length) {
    eqDimensionChart.clear()
    return
  }
  const option = {
    title: { text: '装备操作维度 CV 对比', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        let result = params[0].name + '<br/>'
        params.forEach((p) => { result += `${p.seriesName}: ${p.value.toFixed(2)}%<br/>` })
        return result
      }
    },
    legend: { data: ['所有数据', '去除极端值'], top: 30 },
    xAxis: { type: 'category', data: allDims.map((item) => item.indicatorName), axisLabel: { rotate: 20, fontSize: 11 } },
    yAxis: { type: 'value', name: 'CV(%)', axisLabel: { formatter: '{value}%' } },
    series: [
      { name: '所有数据', type: 'bar', data: allDims.map((item) => item.cv), itemStyle: { color: '#E6A23C' } },
      {
        name: '去除极端值',
        type: 'bar',
        data: allDims.map((item) => {
          const filtered = filteredDims.find((f) => f.indicatorCode === item.indicatorCode)
          return filtered ? filtered.cv : 0
        }),
        itemStyle: { color: '#F56C6C' }
      }
    ],
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true }
  }
  eqDimensionChart.setOption(option)
  eqDimensionChart.resize()
}

function renderEqIndicatorChart() {
  if (!eqIndicatorChartRef.value) return
  if (!eqIndicatorChart) {
    eqIndicatorChart = echarts.init(eqIndicatorChartRef.value)
  }
  const allEq = allData.value?.equipmentIndicatorCvs || []
  const filteredEq = filteredExtreme.value?.equipmentIndicatorCvs || []
  if (!allEq.length) {
    eqIndicatorChart.clear()
    return
  }
  const option = {
    title: { text: '装备操作二级 CV 对比', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        let result = params[0].name + '<br/>'
        params.forEach((p) => { result += `${p.seriesName}: ${p.value.toFixed(2)}%<br/>` })
        return result
      }
    },
    legend: { data: ['所有数据', '去除极端值'], top: 30 },
    xAxis: { type: 'category', data: allEq.map((item) => item.indicatorName), axisLabel: { rotate: 30, fontSize: 10 } },
    yAxis: { type: 'value', name: 'CV(%)', axisLabel: { formatter: '{value}%' } },
    series: [
      { name: '所有数据', type: 'line', data: allEq.map((item) => item.cv), itemStyle: { color: '#E6A23C' }, lineStyle: { width: 2 }, smooth: true },
      {
        name: '去除极端值',
        type: 'line',
        data: allEq.map((item) => {
          const filtered = filteredEq.find((f) => f.indicatorCode === item.indicatorCode)
          return filtered ? filtered.cv : 0
        }),
        itemStyle: { color: '#F56C6C' },
        lineStyle: { width: 2, type: 'dashed' },
        smooth: true
      }
    ],
    grid: { left: '3%', right: '4%', bottom: '20%', containLabel: true }
  }
  eqIndicatorChart.setOption(option)
  eqIndicatorChart.resize()
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
    const ids = await getCollectiveEvaluationIds()
    evaluationIds.value = Array.isArray(ids) ? ids : []
  } catch (error) {
    console.error('加载评估批次失败:', error)
    evaluationIds.value = []
  }
}

async function loadAvailableExperts() {
  try {
    const list = await request.get('/expert/credibility/experts')
    availableExperts.value = Array.isArray(list) ? list : []
  } catch (error) {
    console.error('加载专家列表失败:', error)
    availableExperts.value = []
  }
}

function onEvaluationIdChange() {
  weightPreview.value = null
  collectiveResult.value = null
  disposeCollectiveCharts()
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
    ElMessage.success('集结权重已保存。综合得分请至「评估结果计算」点击「加载综合结果」，或在综合评分区「生成综合得分」。')
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

function getLevelType(cv) {
  if (cv <= 15) return 'success'
  if (cv <= 25) return 'warning'
  if (cv <= 35) return 'warning'
  return 'danger'
}

watch(
  () => props.pageMode,
  (mode) => {
    if (mode === 'dispersion') {
      disposeCollectiveCharts()
      loadData()
    } else {
      disposeDispersionCharts()
      loadEvaluationIds()
      loadAvailableExperts()
    }
  },
  { immediate: true, flush: 'post' }
)

watch(activeTopTab, () => {
  nextTick(() => {
    if (activeTopTab.value === 'equipment') {
      renderEqDimensionChart()
      renderEqIndicatorChart()
    } else {
      renderDimensionChart()
      renderIndicatorChart()
    }
  })
})

onMounted(() => {
  window.addEventListener('resize', () => {
    dimensionChart?.resize()
    effIndicatorChart?.resize()
    eqDimensionChart?.resize()
    eqIndicatorChart?.resize()
    dimWeightChart?.resize()
    indWeightChart?.resize()
  })
})
</script>

<style scoped lang="scss">
.expert-aggregation-container {
  padding: 0;
}

.collective-follow-hint {
  margin-top: 16px;
}

.page-header {
  margin-bottom: 20px;

  .page-header-row {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 16px;
  }

  h2 {
    display: flex;
    align-items: center;
    gap: 10px;
    margin: 0 0 8px 0;
    color: #303133;
    font-size: 22px;
  }

  .subtitle {
    margin: 0;
    color: #909399;
    font-size: 14px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.legend-card {
  margin-bottom: 16px;

  .legend-items {
    display: flex;
    gap: 30px;
    flex-wrap: wrap;
  }

  .legend-item {
    display: flex;
    align-items: center;
    gap: 10px;

    .legend-range {
      font-size: 13px;
      color: #606266;
    }
  }
}

.experts-card {
  margin-bottom: 16px;

  .expert-ids {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    min-height: 40px;
  }

  .expert-tag {
    font-family: monospace;
  }
}

.cv-card {
  margin-bottom: 16px;
}

.cv-domain-switch-wrap {
  margin-bottom: 20px;
}

.cv-top-switch-card {
  margin-bottom: 16px;
  border: 1px solid #dcdfe6;

  :deep(.el-card__body) {
    padding: 12px 20px;
  }
}

.cv-top-switch-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.cv-top-switch-label {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.cv-top-radio {
  flex: 1;
  min-width: 240px;
}

.cv-domain-panel {
  margin-top: 0;
}

.cv-domain-card {
  border: none;
  box-shadow: none;

  .el-card__header {
    background: #f5f7fa;
    padding: 10px 20px;
  }
}

.chart-container {
  width: 100%;
  height: 300px;
}

.chart-container-large {
  width: 100%;
  height: 350px;
}

.chart-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;

  h4 {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0 0 15px 0;
    color: #606266;
    font-size: 15px;
  }
}

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
