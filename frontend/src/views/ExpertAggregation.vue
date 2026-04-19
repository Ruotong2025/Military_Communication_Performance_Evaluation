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

    <!-- ==================== 综合叶子全局权重 CV（效能+装备统一排序） ==================== -->
    <div v-if="isDispersion" class="unified-leaf-section">
      <el-card class="unified-leaf-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span>
              <el-icon><Histogram /></el-icon>
              三、综合叶子全局权重 CV（效能 + 装备）
            </span>
            <el-tag size="small" type="info">所有叶子已乘域间一级权重，直接可比</el-tag>
          </div>
        </template>

        <el-tabs v-model="unifiedLeafActiveTab" class="unified-leaf-tabs">
          <el-tab-pane label="所有数据" name="all">
            <el-table :data="unifiedLeafTableData" stripe border size="small" max-height="480">
              <el-table-column type="index" label="排名" width="60" align="center" />
              <el-table-column label="域" width="70" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.domainTag === '效能' ? 'success' : 'primary'" size="small">
                    {{ row.domainTag }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="indicatorName" label="指标名称" min-width="140" />
              <el-table-column prop="dimension" label="维度" width="110" align="center" />
              <el-table-column prop="allMean" label="均值" width="90" align="center" :formatter="fmtPct" />
              <el-table-column prop="allCv" label="CV(%)" width="90" align="center">
                <template #default="{ row }">
                  <span :style="{ color: cvColor(row.allCv) }">{{ fmtCv(row.allCv) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="allLevel" label="一致性" width="90" align="center">
                <template #default="{ row }">
                  <el-tag :type="levelTagType(row.allLevel)" size="small">{{ row.allLevel }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="allStdDev" label="标准差" width="80" align="center" :formatter="fmtPct" />
            </el-table>
            <el-empty v-if="!unifiedLeafTableData?.length" description="暂无综合叶子数据" :image-size="72" style="margin:16px 0" />
          </el-tab-pane>

          <el-tab-pane label="去除极端值" name="filtered">
            <el-table :data="unifiedLeafFilteredData" stripe border size="small" max-height="480">
              <el-table-column type="index" label="排名" width="60" align="center" />
              <el-table-column label="域" width="70" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.domainTag === '效能' ? 'success' : 'primary'" size="small">
                    {{ row.domainTag }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="indicatorName" label="指标名称" min-width="140" />
              <el-table-column prop="dimension" label="维度" width="110" align="center" />
              <el-table-column prop="filteredMean" label="均值" width="90" align="center" :formatter="fmtPct" />
              <el-table-column prop="filteredCv" label="CV(%)" width="90" align="center">
                <template #default="{ row }">
                  <span :style="{ color: cvColor(row.filteredCv) }">{{ fmtCv(row.filteredCv) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="filteredLevel" label="一致性" width="90" align="center">
                <template #default="{ row }">
                  <el-tag :type="levelTagType(row.filteredLevel)" size="small">{{ row.filteredLevel }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="filteredStdDev" label="标准差" width="80" align="center" :formatter="fmtPct" />
            </el-table>
            <el-empty v-if="!unifiedLeafFilteredData?.length" description="暂无过滤后数据" :image-size="72" style="margin:16px 0" />
          </el-tab-pane>
        </el-tabs>

        <!-- 柱状图：按均值降序 top-N -->
        <div class="chart-section">
          <h4><el-icon><DataLine /></el-icon> 综合叶子全局权重均值对比（Top 20）</h4>
          <div ref="unifiedLeafChartRef" class="chart-container-large"></div>
        </div>
      </el-card>
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
            构造集体判断矩阵：效能侧（维度层 + 各维度指标层）、装备操作侧（带「装备操作_」前缀的键）、以及域间一级（效能 vs 装备）；
            分别做 AHP 得权重向量并检验 CR。再按与个体 AHP 相同规则合成<strong>一级域间 · 二级维度 · 三级叶子</strong>的全局权重（含旭日图，见下方第五节）。
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
        <el-tag v-if="collectiveCrItems.some((x) => x.branch === 'eq')" type="info" effect="plain" size="small" style="margin-left: 10px">
          含装备操作矩阵
        </el-tag>
      </template>
      <el-row :gutter="12">
        <el-col
          v-for="item in collectiveCrItems"
          :key="item.key"
          :xs="12"
          :sm="8"
          :md="6"
          :lg="4"
        >
          <div class="cr-item">
            <span class="cr-label">{{ item.label }}</span>
            <el-tag :type="(Number(item.cr) < 0.1) ? 'success' : 'danger'" size="large">
              {{ Number(item.cr).toFixed(4) }}
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

      <!-- 体系切换 + 矩阵选择器 -->
      <div class="matrix-selector matrix-selector--system">
        <span class="matrix-system-label">集体矩阵体系：</span>
        <el-radio-group v-model="matrixSystem" size="default" @change="onMatrixSystemChange">
          <el-radio-button value="efficacy">效能指标体系</el-radio-button>
          <el-radio-button value="equipment" :disabled="!eqCollectiveMatrixAvailable">装备操作体系</el-radio-button>
        </el-radio-group>
        <el-tag v-if="!eqCollectiveMatrixAvailable" type="warning" effect="plain" size="small" style="margin-left: 8px">
          装备侧需存在「装备操作_」前缀的集体打分；完整层次见第五节
        </el-tag>
      </div>

      <div v-show="matrixSystem === 'efficacy'" class="matrix-selector">
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

      <div v-show="matrixSystem === 'equipment'" class="matrix-selector matrix-selector--eq">
        <el-radio-group v-model="selectedEqView" size="large">
          <el-radio-button value="eq-dim">装备维度层 ({{ eqDimMatrixSize }}×{{ eqDimMatrixSize }})</el-radio-button>
          <el-radio-button
            v-for="d in eqCollectiveDimensionNames"
            :key="'eqind-' + d"
            :value="'eq-ind:' + d"
          >
            {{ d }} 指标层
          </el-radio-button>
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
        <h5>{{ matrixWeightsTitle }}</h5>
        <el-row :gutter="12">
          <el-col :xs="12" :sm="8" :md="6" :lg="4" v-for="(w, idx) in currentMatrixWeights" :key="idx">
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
        <h5>4.1 效能指标体系 · 一级维度集结权重</h5>
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
        <h5>4.2 效能 · 维度层权重分布图</h5>
        <div ref="dimWeightChartRef" class="chart-container-medium"></div>
      </div>

      <!-- 装备操作：体系内维度权重 -->
      <div v-if="eqCollectiveDimensionNames.length" class="preview-section">
        <h5>4.2b 装备操作体系 · 一级维度集结权重（体系内归一化）</h5>
        <el-row :gutter="12">
          <el-col :xs="12" :sm="8" :md="6" :lg="4" v-for="dim in eqCollectiveDimensionNames" :key="'eqdw-' + dim">
            <div class="weight-item weight-item--eq">
              <span class="weight-label">{{ dim }}</span>
              <strong>{{ (Number(aggregatedUnified?.equipment?.dimensionWeights?.[dim]) * 100 || 0).toFixed(2) }}%</strong>
            </div>
          </el-col>
        </el-row>
      </div>
      <div v-if="eqCollectiveDimensionNames.length" class="weight-chart-section">
        <h5>4.2c 装备操作 · 维度层权重分布图</h5>
        <div ref="eqCollectiveDimChartRef" class="chart-container-medium"></div>
      </div>

      <!-- 二级叶子：效能 + 装备，同一综合权重表（与第五节 allLeaves 一致） -->
      <div class="preview-section">
        <h5>4.3 二级叶子指标综合权重（效能 + 装备操作，对总目标）</h5>
        <p v-if="!aggregatedUnified?.allLeaves?.length" class="combined-leaf-hint">
          当前为效能侧集结权重预览；执行集结且含域间与装备数据后，本表与下图将合并展示全部叶子的全局综合权重。
        </p>
        <el-table :data="collectiveAllLeafRows" border stripe size="small" max-height="420">
          <el-table-column prop="systemLabel" label="体系" width="120" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.systemKey === 'equipment' ? 'warning' : 'success'">
                {{ row.systemLabel }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="dimension" label="维度" width="120" align="center">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ row.dimension }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="indicator" label="指标（叶子）" min-width="140" show-overflow-tooltip />
          <el-table-column label="综合权重" align="center" width="130">
            <template #default="{ row }">
              <span class="score-cell">{{ (Number(row.weight) * 100).toFixed(2) }}%</span>
            </template>
          </el-table-column>
          <el-table-column label="权重条" min-width="160">
            <template #default="{ row }">
              <el-progress
                :percentage="Math.min(100, Number(row.weight) * 100)"
                :stroke-width="10"
                :show-text="false"
              />
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="weight-chart-section weight-chart-section--sunburst">
        <h5>4.4 二级叶子综合权重旭日图（内圈体系 · 中圈维度 · 外圈叶子）</h5>
        <p class="sunburst-hint">
          扇区大小表示对总目标的综合权重（%）。含完整域间与装备数据时展示效能 + 装备双体系；仅预览效能集结时为单体系旭日图。
        </p>
        <div ref="collectiveSunburstRef" class="collective-sunburst-chart"></div>
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
  Guide,
  Histogram
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

// 综合叶子权重
const unifiedLeafActiveTab = ref('all')
const unifiedLeafChartRef = ref(null)
let unifiedLeafChart = null

// ==================== 集结计算状态 ====================
const collectiveLoading = ref(false)
const previewLoading = ref(false)
const calculateLoading = ref(false)
const evaluationIds = ref([])
const availableExperts = ref([])
const weightPreview = ref(null)
const collectiveResult = ref(null)

// 矩阵展示相关（与后端 comparison_key 一致：装备操作_ 前缀）
const EQ_PREFIX = '装备操作_'
const selectedMatrix = ref('dim')
/** efficacy | equipment：集体判断矩阵切换 */
const matrixSystem = ref('efficacy')
/** equipment：eq-dim 维度层；eq-ind:维度名 指标层 */
const selectedEqView = ref('eq-dim')
const dimWeightChartRef = ref(null)
const collectiveSunburstRef = ref(null)
const eqCollectiveDimChartRef = ref(null)
let dimWeightChart = null
let collectiveSunburstChart = null
let eqCollectiveDimChart = null

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
  collectiveSunburstChart?.dispose()
  eqCollectiveDimChart?.dispose()
  dimWeightChart = null
  collectiveSunburstChart = null
  eqCollectiveDimChart = null
}

const collectiveForm = ref({
  evaluationId: '',
  useAllExperts: true,
  expertIds: []
})

/** 优先使用最近一次「执行集结计算」结果，否则用「预览」数据，保证矩阵与图表有数据 */
const activeCollectiveWeights = computed(() => collectiveResult.value ?? weightPreview.value)

// 效能集体矩阵元素（须早于 collectiveCrItems / 矩阵计算属性）
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

/** 集结合成的完整层次（含装备），与后端 aggregatedUnified 一致 */
const aggregatedUnified = computed(() => activeCollectiveWeights.value?.aggregatedUnified ?? null)

const collectiveCrItems = computed(() => {
  const src = activeCollectiveWeights.value
  if (!src) return []
  const items = []
  const crMap = src.crResults
  if (crMap && typeof crMap === 'object') {
    for (const [k, v] of Object.entries(crMap)) {
      items.push({
        key: 'eff-cr-' + k,
        branch: 'eff',
        label: '效能·' + (crLabelMap[k] || k),
        cr: Number(v) || 0
      })
    }
  }
  const crEq = aggregatedUnified.value?.equipment?.crByDimension
  if (crEq && typeof crEq === 'object') {
    for (const [k, v] of Object.entries(crEq)) {
      const label = k === 'dimension' ? '装备·维度层' : '装备·' + k + '·指标层'
      items.push({
        key: 'eq-cr-' + k,
        branch: 'eq',
        label,
        cr: Number(v) || 0
      })
    }
  }
  return items
})

const eqCollectiveDimensionNames = computed(() => {
  const dw = aggregatedUnified.value?.equipment?.dimensionWeights
  if (!dw || typeof dw !== 'object') return []
  return Object.keys(dw).sort()
})

const eqCollectiveMatrixAvailable = computed(() => {
  if (eqCollectiveDimensionNames.value.length > 0) return true
  const cs = activeCollectiveWeights.value?.collectiveScores
  if (!cs || typeof cs !== 'object') return false
  return Object.keys(cs).some((k) => k && k.startsWith(EQ_PREFIX))
})

const eqDimMatrixSize = computed(() => Math.max(1, eqCollectiveDimensionNames.value.length))

function onMatrixSystemChange() {
  if (matrixSystem.value === 'equipment' && !eqCollectiveMatrixAvailable.value) {
    matrixSystem.value = 'efficacy'
    return
  }
  if (matrixSystem.value === 'equipment') {
    selectedEqView.value = 'eq-dim'
  }
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

/** 装备集体矩阵单元格：comparison_key 为 装备操作_维A_维B 或 装备操作_维度_指标A_指标B */
function equipmentMatrixCell(collectiveScores, rowName, colName, indicatorDimension) {
  if (rowName === colName) return '1.000'
  let ratio = 1
  if (indicatorDimension) {
    const k1 = EQ_PREFIX + indicatorDimension + '_' + rowName + '_' + colName
    const k2 = EQ_PREFIX + indicatorDimension + '_' + colName + '_' + rowName
    let s = collectiveScores[k1]
    if (s != null && Number.isFinite(Number(s))) ratio = Number(s)
    else {
      s = collectiveScores[k2]
      if (s != null && Number.isFinite(Number(s))) ratio = 1 / Number(s)
    }
  } else {
    const k1 = EQ_PREFIX + rowName + '_' + colName
    const k2 = EQ_PREFIX + colName + '_' + rowName
    let s = collectiveScores[k1]
    if (s != null && Number.isFinite(Number(s))) ratio = Number(s)
    else {
      s = collectiveScores[k2]
      if (s != null && Number.isFinite(Number(s))) ratio = 1 / Number(s)
    }
  }
  return ratio.toFixed(3)
}

const efficacyMatrixHeaders = computed(
  () => (matrixConfig[selectedMatrix.value] || matrixConfig.dim).headers
)

const currentMatrixCR = computed(() => {
  if (matrixSystem.value === 'efficacy') {
    if (!activeCollectiveWeights.value?.crResults) return 0
    const crMap = activeCollectiveWeights.value.crResults
    if (selectedMatrix.value === 'dim') return Number(crMap['dim']) || 0
    return Number(crMap[matrixConfig[selectedMatrix.value]?.name]) || 0
  }
  const crBy = aggregatedUnified.value?.equipment?.crByDimension
  if (!crBy) return 0
  if (selectedEqView.value === 'eq-dim') {
    return Number(crBy['dimension']) || 0
  }
  const dim = selectedEqView.value.startsWith('eq-ind:') ? selectedEqView.value.slice(7) : ''
  return Number(crBy[dim]) || 0
})

const currentMatrixHeaders = computed(() => {
  if (matrixSystem.value === 'efficacy') {
    return efficacyMatrixHeaders.value
  }
  const eq = aggregatedUnified.value?.equipment
  if (!eq) return []
  if (selectedEqView.value === 'eq-dim') {
    return eqCollectiveDimensionNames.value
  }
  const dim = selectedEqView.value.startsWith('eq-ind:') ? selectedEqView.value.slice(7) : ''
  const indMap = eq.indicators?.[dim]
  if (!indMap || typeof indMap !== 'object') return []
  return Object.keys(indMap).sort()
})

const currentMatrixData = computed(() => {
  const src = activeCollectiveWeights.value
  if (!src) return []
  const headers = currentMatrixHeaders.value
  const collectiveScores = src.collectiveScores || {}

  if (matrixSystem.value === 'efficacy') {
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
  }

  const indDim = selectedEqView.value.startsWith('eq-ind:') ? selectedEqView.value.slice(7) : null
  const matrix = []
  for (let i = 0; i < headers.length; i++) {
    const row = { row: headers[i] }
    for (let j = 0; j < headers.length; j++) {
      if (i === j) row[headers[j]] = '1.000'
      else row[headers[j]] = equipmentMatrixCell(collectiveScores, headers[i], headers[j], indDim)
    }
    matrix.push(row)
  }
  return matrix
})

const currentMatrixWeights = computed(() => {
  const src = activeCollectiveWeights.value
  if (!src) return []
  const headers = currentMatrixHeaders.value

  if (matrixSystem.value === 'efficacy') {
    const config = matrixConfig[selectedMatrix.value] || matrixConfig.dim
    if (selectedMatrix.value === 'dim') {
      const dimWeights = src.dimensionWeights || {}
      return headers.map((h) => Number(dimWeights[h] || 0))
    }
    const indWeightsByDim = src.indicatorWeightsByDimension || {}
    const dimIndWeights = indWeightsByDim[config.name] || {}
    return headers.map((h) => Number(dimIndWeights[h] || 0))
  }

  const eq = aggregatedUnified.value?.equipment
  if (!eq) return headers.map(() => 0)
  if (selectedEqView.value === 'eq-dim') {
    const dw = eq.dimensionWeights || {}
    return headers.map((h) => Number(dw[h] || 0))
  }
  const dim = selectedEqView.value.slice(7)
  const dimInd = eq.indicators?.[dim] || {}
  return headers.map((h) => Number(dimInd[h] || 0))
})

const matrixWeightsTitle = computed(() => {
  if (matrixSystem.value === 'efficacy') {
    if (selectedMatrix.value === 'dim') return '维度层权重向量'
    const n = matrixConfig[selectedMatrix.value]?.name || ''
    return n ? `${n} · 指标层权重向量` : '指标层权重向量'
  }
  if (selectedEqView.value === 'eq-dim') return '装备操作 · 维度层权重向量'
  const dim = selectedEqView.value.startsWith('eq-ind:') ? selectedEqView.value.slice(7) : ''
  return dim ? `装备操作 · ${dim} · 指标层权重向量` : '装备操作 · 指标层权重向量'
})

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

const SB_SUNBURST_BRANCH = ['#3D9970', '#409EFF']
const SB_SUNBURST_DIM_PALETTE = ['#0074D9', '#39CCCC', '#3D9970', '#FF851B', '#FFDC00', '#B10DC9', '#85144b', '#F012BE']

function shortLeafLabel(name) {
  if (!name || typeof name !== 'string') return String(name ?? '')
  return name.replace(/得分$/u, '').slice(0, 12)
}

function crossBranchWeights(snap) {
  const c = snap?.crossDomain
  const wEff = Number.isFinite(Number(c?.effWeight)) ? Number(c.effWeight) : 0.5
  const wEq = Number.isFinite(Number(c?.eqWeight)) ? Number(c.eqWeight) : 0.5
  return { wEff, wEq }
}

/** 与 UnifiedGlobalWeightsPanel 一致：由 aggregatedUnified 合成旭日图数据 */
function buildSunburstTreeFromSnapshot(snap) {
  if (!snap) return null
  const { wEff, wEq } = crossBranchWeights(snap)
  const eff = snap.effectiveness
  const eq = snap.equipment
  const effDims = eff?.dimensionWeights || {}
  const eqDims = eq?.dimensionWeights || {}
  const effInd = eff?.indicators || {}
  const eqInd = eq?.indicators || {}

  function dimChildren(branchW, dimWeights, indicators) {
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
          name: shortLeafLabel(indName),
          value: v,
          fullPath: `${dimName} · ${indName}`
        })
      }
      if (!leaves.length) continue
      const base = SB_SUNBURST_DIM_PALETTE[ci % SB_SUNBURST_DIM_PALETTE.length]
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

  const effChildren = dimChildren(wEff, effDims, effInd)
  const eqChildren = dimChildren(wEq, eqDims, eqInd)

  const children = []
  if (effChildren.length) {
    children.push({
      name: '效能',
      itemStyle: { color: SB_SUNBURST_BRANCH[0] },
      children: effChildren
    })
  }
  if (eqChildren.length) {
    children.push({
      name: '装备',
      itemStyle: { color: SB_SUNBURST_BRANCH[1] },
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

/** 无 aggregatedUnified 时，仅由叶子行（多为效能）组树 */
function buildSunburstTreeFromLeafRows(rows) {
  if (!rows?.length) return null
  const effRows = rows.filter((r) => r.systemKey === 'efficiency')
  const eqRows = rows.filter((r) => r.systemKey === 'equipment')
  const children = []

  function branch(name, list, color) {
    const byDim = new Map()
    for (const r of list) {
      if (!byDim.has(r.dimension)) byDim.set(r.dimension, [])
      byDim.get(r.dimension).push(r)
    }
    let ci = 0
    const dimChildren = []
    for (const [dimName, arr] of byDim) {
      const base = SB_SUNBURST_DIM_PALETTE[ci % SB_SUNBURST_DIM_PALETTE.length]
      ci++
      dimChildren.push({
        name: dimName,
        itemStyle: { color: base },
        children: arr.map((row, i) => ({
          name: shortLeafLabel(row.indicator),
          value: Math.max(1e-9, Number(row.weight) * 100),
          fullPath: `${dimName} · ${row.indicator}`,
          itemStyle: { color: echarts.color.lift(base, (i % 5) * 0.07) }
        }))
      })
    }
    return { name, itemStyle: { color }, children: dimChildren }
  }

  if (effRows.length) children.push(branch('效能', effRows, SB_SUNBURST_BRANCH[0]))
  if (eqRows.length) children.push(branch('装备', eqRows, SB_SUNBURST_BRANCH[1]))
  if (!children.length) return null
  return {
    name: '',
    itemStyle: { color: '#001f3f' },
    children
  }
}

function renderCollectiveLeafSunburst() {
  const src = collectiveResult.value ?? weightPreview.value
  if (!collectiveSunburstRef.value || !src) return

  if (collectiveSunburstChart) {
    const dom = collectiveSunburstChart.getDom()
    if (!dom?.isConnected) {
      collectiveSunburstChart.dispose()
      collectiveSunburstChart = null
    }
  }
  if (!collectiveSunburstChart) {
    collectiveSunburstChart = echarts.init(collectiveSunburstRef.value)
  }

  const snap = aggregatedUnified.value
  let root = snap ? buildSunburstTreeFromSnapshot(snap) : null
  if (!root) {
    root = buildSunburstTreeFromLeafRows(collectiveAllLeafRows.value)
  }
  if (!root) {
    collectiveSunburstChart.clear()
    return
  }

  collectiveSunburstChart.setOption({
    tooltip: {
      trigger: 'item',
      confine: true,
      formatter(p) {
        const v = p.value
        const pct = typeof v === 'number' ? `${v.toFixed(4)}%` : String(p.value)
        const extra = p.data?.fullPath
        if (extra && p.treePathInfo?.length >= 4) {
          return `<div style="max-width:300px;line-height:1.6">${extra}<br/>占全体：<b>${pct}</b></div>`
        }
        const path = p.treePathInfo?.map((x) => x.name).filter(Boolean).slice(1).join(' → ') || ''
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
  collectiveSunburstChart.resize()
}

/** 集结结果中装备操作体系内维度权重柱状图（4.2c） */
function renderEqCollectiveDimChart() {
  const eq = aggregatedUnified.value?.equipment?.dimensionWeights
  if (!eqCollectiveDimChartRef.value || !eq || typeof eq !== 'object') return

  if (eqCollectiveDimChart) {
    const dom = eqCollectiveDimChart.getDom()
    if (!dom?.isConnected) {
      eqCollectiveDimChart.dispose()
      eqCollectiveDimChart = null
    }
  }
  if (!eqCollectiveDimChart) {
    eqCollectiveDimChart = echarts.init(eqCollectiveDimChartRef.value)
  }

  const dims = Object.keys(eq).sort()
  const values = dims.map((d) =>
    Number((Number(eq[d]) || 0) * 100).toFixed(2)
  )

  const option = {
    title: {
      text: '装备操作 · 维度层权重分布',
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
      axisLabel: { rotate: 18, fontSize: 12 }
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
          color: ['#E6A23C', '#F56C6C', '#909399', '#67C23A', '#409EFF', '#b37feb'][i % 6]
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
  eqCollectiveDimChart.setOption(option, true)
  eqCollectiveDimChart.resize()
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
      renderUnifiedLeafChart()
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

function renderUnifiedLeafChart() {
  if (!unifiedLeafChartRef.value) return
  if (!unifiedLeafChart) {
    unifiedLeafChart = echarts.init(unifiedLeafChartRef.value)
  }
  const unified = allData.value?.unifiedLeafCvs || []
  if (!unified.length) {
    unifiedLeafChart.clear()
    return
  }
  // Top 20 按均值降序
  const sorted = [...unified].sort((a, b) => b.mean - a.mean).slice(0, 20)
  const option = {
    title: { text: '综合叶子全局权重均值 Top 20', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const item = sorted[params[0].dataIndex]
        return `${item.indicatorName}<br/>域: ${item.domainTag}<br/>维度: ${item.dimension}<br/>均值: ${(item.mean * 100).toFixed(2)}%<br/>CV: ${item.cv.toFixed(2)}%`
      }
    },
    xAxis: { type: 'category', data: sorted.map((i) => i.indicatorName), axisLabel: { rotate: 40, fontSize: 9 } },
    yAxis: { type: 'value', name: '全局权重均值', axisLabel: { formatter: (v) => (v * 100).toFixed(1) + '%' } },
    series: [
      {
        name: '均值',
        type: 'bar',
        data: sorted.map((i) => i.mean),
        itemStyle: {
          color: (params) => params.dataIndex < 10 ? '#67C23A' : '#909399'
        },
        label: {
          show: true,
          position: 'top',
          formatter: (params) => (params.value * 100).toFixed(1) + '%',
          fontSize: 9,
          rotate: 40
        }
      }
    ],
    grid: { left: '3%', right: '4%', bottom: '25%', containLabel: true }
  }
  unifiedLeafChart.setOption(option)
  unifiedLeafChart.resize()
}

// 综合叶子表格数据
const unifiedLeafTableData = computed(() => {
  const all = allData.value?.unifiedLeafCvs || []
  return all.map((item) => ({
    indicatorCode: item.indicatorCode,
    indicatorName: item.indicatorName,
    dimension: item.dimension,
    domainTag: item.domainTag,
    allMean: item.mean,
    allCv: item.cv,
    allLevel: item.consistencyLevel,
    allStdDev: item.stdDev
  }))
})

const unifiedLeafFilteredData = computed(() => {
  const filtered = filteredExtreme.value?.unifiedLeafCvs || []
  return filtered.map((item) => ({
    indicatorCode: item.indicatorCode,
    indicatorName: item.indicatorName,
    dimension: item.dimension,
    domainTag: item.domainTag,
    filteredMean: item.mean,
    filteredCv: item.cv,
    filteredLevel: item.consistencyLevel,
    filteredStdDev: item.stdDev
  }))
})

function fmtPct(row, col, val) {
  return val != null ? (val * 100).toFixed(2) + '%' : '-'
}

function fmtCv(cv) {
  return cv != null ? cv.toFixed(2) + '%' : '-'
}

function cvColor(cv) {
  if (cv == null) return '#666'
  if (cv <= 15) return '#67C23A'
  if (cv <= 25) return '#E6A23C'
  if (cv <= 35) return '#F56C6C'
  return '#C0392B'
}

function levelTagType(level) {
  if (level === '高度一致') return 'success'
  if (level === '较为一致') return 'warning'
  if (level === '轻度分歧') return 'warning'
  if (level === '严重分歧') return 'danger'
  return 'info'
}

// ==================== 集结计算函数 ====================

const dimensionIndicatorMap = {
  '安全性': ['密钥泄露得分', '被侦察得分', '抗拦截得分'],
  '可靠性': ['崩溃比例得分', '恢复能力得分', '通信可用得分'],
  '传输能力': ['带宽得分', '呼叫建立得分', '传输时延得分', '误码率得分', '吞吐量得分', '频谱效率得分'],
  '抗干扰能力': ['信干噪比得分', '抗干扰余量得分', '通信距离得分'],
  '效能影响': ['战损率得分', '任务完成率得分', '致盲率得分']
}

/** 第四节合并表/图：叶子全局综合权重（优先 aggregatedUnified.allLeaves，否则仅效能 indicatorWeights） */
const collectiveAllLeafRows = computed(() => {
  const unified = aggregatedUnified.value
  const leaves = unified?.allLeaves
  if (Array.isArray(leaves) && leaves.length > 0) {
    return leaves
      .map((leaf) => {
        const dk = leaf.domain === 'equipment' ? 'equipment' : 'efficiency'
        return {
          systemKey: dk,
          systemLabel: dk === 'equipment' ? '装备操作' : '效能',
          dimension: leaf.dimension || '—',
          indicator: leaf.indicator || '—',
          weight: Number(leaf.globalWeight) || 0
        }
      })
      .sort((a, b) => b.weight - a.weight)
  }
  const src = activeCollectiveWeights.value
  if (!src?.indicatorWeights) return []
  const iw = src.indicatorWeights
  const result = []
  for (const [dim, indicators] of Object.entries(dimensionIndicatorMap)) {
    for (const ind of indicators) {
      const w = iw[ind]
      if (w !== undefined) {
        result.push({
          systemKey: 'efficiency',
          systemLabel: '效能',
          dimension: dim,
          indicator: ind,
          weight: Number(w) || 0
        })
      }
    }
  }
  return result.sort((a, b) => b.weight - a.weight)
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
    matrixSystem.value = 'efficacy'
    selectedEqView.value = 'eq-dim'

    ElMessage.success('权重预览已更新')
    nextTick(() => {
      renderDimWeightChart()
      renderCollectiveLeafSunburst()
      renderEqCollectiveDimChart()
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
      renderCollectiveLeafSunburst()
      renderEqCollectiveDimChart()
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
    collectiveSunburstChart?.resize()
    eqCollectiveDimChart?.resize()
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
.combined-leaf-hint {
  font-size: 12px;
  color: #909399;
  margin: 0 0 10px 0;
  line-height: 1.5;
}

.weight-chart-section--sunburst {
  .sunburst-hint {
    font-size: 12px;
    color: #909399;
    margin: 0 0 10px 0;
    line-height: 1.5;
    text-align: center;
  }
}

.collective-sunburst-chart {
  width: 100%;
  min-height: 420px;
  height: 440px;
}

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

  .weight-item--eq strong {
    color: #e6a23c;
  }
}

.matrix-selector--system {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 12px;
}

.matrix-system-label {
  font-size: 13px;
  color: #606266;
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

.unified-leaf-section {
  margin-bottom: 20px;
}

.unified-leaf-card {
  .el-tabs {
    margin-bottom: 16px;
  }
}

</style>
