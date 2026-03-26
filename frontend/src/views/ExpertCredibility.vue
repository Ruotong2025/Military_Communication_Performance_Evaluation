<template>
  <div class="expert-credibility">
    <el-card class="page-card">
      <template #header>
        <div class="card-header">
          <el-icon class="header-icon"><User /></el-icon>
          <span>专家可信度评估</span>
        </div>
      </template>

      <!-- 工具栏 -->
      <div class="toolbar">
        <el-space wrap>
          <el-dropdown @command="handleGenerateCommand" trigger="click">
            <el-button type="primary">
              <el-icon><MagicStick /></el-icon>
              生成模拟数据
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="append">
                  <el-icon><DocumentAdd /></el-icon>
                  新增数据（保留已有）
                </el-dropdown-item>
                <el-dropdown-item command="overwrite">
                  <el-icon><RefreshRight /></el-icon>
                  覆盖数据（清空后重新生成）
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button type="success" :loading="evaluating" @click="handleBatchEvaluate" :disabled="unevaluatedCount === 0">
            <el-icon><TrendCharts /></el-icon>
            批量评估未评估专家 ({{ unevaluatedCount }})
          </el-button>
          <el-button type="warning" @click="handleRefresh">
            <el-icon><Refresh /></el-icon>
            刷新数据
          </el-button>
          <el-button type="info" plain @click="openWeightsDialog">
            <el-icon><Setting /></el-icon>
            全局权重
          </el-button>
        </el-space>

        <el-space>
          <el-select v-model="levelFilter" placeholder="按等级筛选" clearable style="width: 140px" @change="handleLevelFilter">
            <el-option label="A级 (≥90)" value="A" />
            <el-option label="B级 (≥75)" value="B" />
            <el-option label="C级 (≥60)" value="C" />
            <el-option label="D级 (<60)" value="D" />
          </el-select>
          <el-select v-model="viewMode" style="width: 120px">
            <el-option label="全部" value="all" />
            <el-option label="仅已评估" value="evaluated" />
            <el-option label="仅未评估" value="unevaluated" />
          </el-select>
        </el-space>
      </div>

      <!-- 统计概览 -->
      <div class="stats-overview">
        <el-row :gutter="16">
          <el-col :span="4">
            <div class="stat-card stat-total">
              <div class="stat-value">{{ statistics.totalExperts || 0 }}</div>
              <div class="stat-label">专家总数</div>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="stat-card stat-a">
              <div class="stat-value">{{ statistics.levelCount?.A || 0 }}</div>
              <div class="stat-label">A级专家</div>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="stat-card stat-b">
              <div class="stat-value">{{ statistics.levelCount?.B || 0 }}</div>
              <div class="stat-label">B级专家</div>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="stat-card stat-c">
              <div class="stat-value">{{ statistics.levelCount?.C || 0 }}</div>
              <div class="stat-label">C级专家</div>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="stat-card stat-d">
              <div class="stat-value">{{ statistics.levelCount?.D || 0 }}</div>
              <div class="stat-label">D级专家</div>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="stat-card stat-avg">
              <div class="stat-value">{{ statistics.averageScore || '0.00' }}</div>
              <div class="stat-label">平均得分</div>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 专家数据表格 -->
      <el-table
        :data="displayedExperts"
        border
        stripe
        v-loading="loading"
        style="width: 100%; margin-top: 16px"
        @row-click="handleRowClick"
        highlight-current-row
      >
        <el-table-column prop="expertId" label="ID" width="70" align="center" />
        <el-table-column prop="expertName" label="姓名" width="100" fixed>
          <template #default="{ row }">
            <span class="expert-name">{{ row.expertName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="职称" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.title || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="education" label="学历" width="80" align="center">
          <template #default="{ row }">
            <span>{{ row.education || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="workUnit" label="单位" min-width="150" show-overflow-tooltip />

        <!-- 评估状态列（避免 tag 内嵌 icon 导致文字发糊/重影） -->
        <el-table-column label="评估状态" width="112" align="center">
          <template #default="{ row }">
            <el-tag
              v-if="row.hasScore"
              class="eval-status-tag"
              type="success"
              effect="plain"
              size="small"
            >
              已评估
            </el-tag>
            <el-tag
              v-else
              class="eval-status-tag"
              type="warning"
              effect="plain"
              size="small"
            >
              待评估
            </el-tag>
          </template>
        </el-table-column>

        <!-- 评估结果显示 -->
        <el-table-column prop="totalScore" label="综合得分" width="110" sortable align="center">
          <template #default="{ row }">
            <template v-if="row.hasScore">
              <el-tag :type="getScoreTagType(row.totalScore)" effect="dark" size="large">
                {{ Number(row.totalScore).toFixed(2) }}
              </el-tag>
            </template>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>

        <el-table-column prop="credibilityLevel" label="等级" width="70" align="center">
          <template #default="{ row }">
            <template v-if="row.hasScore">
              <el-tag :type="getLevelTagType(row.credibilityLevel)" size="large" effect="dark">
                {{ row.credibilityLevel }}
              </el-tag>
            </template>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>

        <!-- 10个维度得分（仅已评估显示） -->
        <el-table-column v-if="showDimensionColumns" label="职称" width="70" align="center">
          <template #default="{ row }">
            <span v-if="row.hasScore" :style="{ color: getScoreColor(row.titleQl) }">
              {{ Number(row.titleQl || 0).toFixed(1) }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column v-if="showDimensionColumns" label="职务" width="70" align="center">
          <template #default="{ row }">
            <span v-if="row.hasScore" :style="{ color: getScoreColor(row.positionQl) }">
              {{ Number(row.positionQl || 0).toFixed(1) }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column v-if="showDimensionColumns" label="学历" width="70" align="center">
          <template #default="{ row }">
            <span v-if="row.hasScore" :style="{ color: getScoreColor(row.educationExperienceQl) }">
              {{ Number(row.educationExperienceQl || 0).toFixed(1) }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column v-if="showDimensionColumns" label="学术" width="70" align="center">
          <template #default="{ row }">
            <span v-if="row.hasScore" :style="{ color: getScoreColor(row.academicAchievementsQl) }">
              {{ Number(row.academicAchievementsQl || 0).toFixed(1) }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column v-if="showDimensionColumns" label="科研" width="70" align="center">
          <template #default="{ row }">
            <span v-if="row.hasScore" :style="{ color: getScoreColor(row.researchAchievementsQl) }">
              {{ Number(row.researchAchievementsQl || 0).toFixed(1) }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column v-if="showDimensionColumns" label="演习" width="70" align="center">
          <template #default="{ row }">
            <span v-if="row.hasScore" :style="{ color: getScoreColor(row.exerciseExperienceQl) }">
              {{ Number(row.exerciseExperienceQl || 0).toFixed(1) }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column v-if="showDimensionColumns" label="军训" width="70" align="center">
          <template #default="{ row }">
            <span v-if="row.hasScore" :style="{ color: getScoreColor(row.militaryTrainingKnowledgeQl) }">
              {{ Number(row.militaryTrainingKnowledgeQl || 0).toFixed(1) }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column v-if="showDimensionColumns" label="仿真" width="70" align="center">
          <template #default="{ row }">
            <span v-if="row.hasScore" :style="{ color: getScoreColor(row.systemSimulationKnowledgeQl) }">
              {{ Number(row.systemSimulationKnowledgeQl || 0).toFixed(1) }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column v-if="showDimensionColumns" label="统计" width="70" align="center">
          <template #default="{ row }">
            <span v-if="row.hasScore" :style="{ color: getScoreColor(row.statisticsKnowledgeQl) }">
              {{ Number(row.statisticsKnowledgeQl || 0).toFixed(1) }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column v-if="showDimensionColumns" label="年限" width="70" align="center">
          <template #default="{ row }">
            <span v-if="row.hasScore" :style="{ color: getScoreColor(row.professionalYearsQt) }">
              {{ Number(row.professionalYearsQt || 0).toFixed(1) }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>

        <!-- 操作列 -->
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.hasScore"
              type="primary"
              link
              size="small"
              @click.stop="handleShowDetail(row)"
            >
              详情
            </el-button>
            <el-button
              v-else
              type="warning"
              link
              size="small"
              @click.stop="handleEvaluateOne(row)"
            >
              评估
            </el-button>
            <el-button
              type="danger"
              link
              size="small"
              @click.stop="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态提示 -->
      <el-empty v-if="!loading && displayedExperts.length === 0" :description="emptyDescription">
        <el-button type="primary" @click="handleGenerateCommand('append')">生成模拟数据</el-button>
      </el-empty>
    </el-card>

    <!-- 评估详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      title="专家可信度评估详情"
      size="70%"
      :destroy-on-close="true"
    >
      <div v-if="selectedExpert" class="expert-detail">
        <!-- 专家基本信息 -->
        <el-card class="info-card">
          <template #header>
            <span>基本信息</span>
          </template>
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="姓名">{{ formatDash(selectedExpert.expertName) }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ formatDash(selectedExpert.gender) }}</el-descriptions-item>
            <el-descriptions-item label="年龄">{{ calculateAge(selectedExpert.birthDate) }}</el-descriptions-item>
            <el-descriptions-item label="工作单位">{{ formatDash(selectedExpert.workUnit) }}</el-descriptions-item>
            <el-descriptions-item label="部门">{{ formatDash(selectedExpert.department) }}</el-descriptions-item>
            <el-descriptions-item label="职称">{{ formatDash(selectedExpert.title) }}</el-descriptions-item>
            <el-descriptions-item label="职务">{{ formatDash(selectedExpert.position) }}</el-descriptions-item>
            <el-descriptions-item label="学历">{{ formatDash(selectedExpert.education) }}</el-descriptions-item>
            <el-descriptions-item label="毕业院校">{{ formatDash(selectedExpert.graduatedSchool) }}</el-descriptions-item>
            <el-descriptions-item label="专业">{{ formatDash(selectedExpert.major) }}</el-descriptions-item>
            <el-descriptions-item label="工作年限">{{ formatYears(selectedExpert.workYears) }}</el-descriptions-item>
            <el-descriptions-item label="专业年限">{{ formatYears(selectedExpert.professionalYears) }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 综合得分 -->
        <el-card class="score-card">
          <template #header>
            <span>综合评估结果</span>
          </template>
          <div class="total-score-display">
            <div class="score-circle" :class="'level-' + selectedScore?.credibilityLevel">
              <div class="score-value">{{ Number(selectedScore?.totalScore || 0).toFixed(2) }}</div>
              <div class="score-level">{{ selectedScore?.credibilityLevel || '-' }}级</div>
            </div>
            <div class="score-meta">
              <span>评估日期: {{ selectedScore?.evaluationDate }}</span>
            </div>
          </div>
        </el-card>

        <!-- 雷达图 + 柱状图 -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-card class="chart-card">
              <template #header>
                <span>十维能力雷达图</span>
              </template>
              <div ref="radarChartRef" style="height: 350px"></div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card class="chart-card">
              <template #header>
                <span>各维度得分详情</span>
              </template>
              <div ref="barChartRef" style="height: 350px"></div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 评估依据说明 -->
        <el-card class="reasoning-card" v-if="evaluationReasoning">
          <template #header>
            <span>评估依据说明</span>
          </template>
          <div class="reasoning-grid">
            <div class="reasoning-item" v-if="evaluationReasoning.titleReasoning">
              <div class="reasoning-title">
                <el-icon><Medal /></el-icon>
                职称评估 <span class="score-tag">{{ evaluationReasoning.titleReasoning.score }}</span>
              </div>
              <div class="reasoning-detail">
                <div class="reasoning-base">{{ evaluationReasoning.titleReasoning.baseTitle }}</div>
                <div class="reasoning-factors">
                  <el-tag
                    v-for="(factor, idx) in evaluationReasoning.titleReasoning.factors"
                    :key="idx"
                    size="small"
                    type="info"
                    class="factor-tag"
                  >
                    {{ factor }}
                  </el-tag>
                </div>
              </div>
            </div>

            <div class="reasoning-item" v-if="evaluationReasoning.positionReasoning">
              <div class="reasoning-title">
                <el-icon><Briefcase /></el-icon>
                职务评估 <span class="score-tag">{{ evaluationReasoning.positionReasoning.score }}</span>
              </div>
              <div class="reasoning-detail">
                <div class="reasoning-base">{{ evaluationReasoning.positionReasoning.summary }}</div>
              </div>
            </div>

            <div class="reasoning-item" v-if="evaluationReasoning.educationReasoning">
              <div class="reasoning-title">
                <el-icon><Reading /></el-icon>
                学历评估 <span class="score-tag">{{ evaluationReasoning.educationReasoning.score }}</span>
              </div>
              <div class="reasoning-detail">
                <div class="reasoning-base">{{ evaluationReasoning.educationReasoning.summary }}</div>
                <div class="reasoning-factors">
                  <el-tag
                    v-for="(factor, idx) in evaluationReasoning.educationReasoning.factors"
                    :key="idx"
                    size="small"
                    type="info"
                    class="factor-tag"
                  >
                    {{ factor }}
                  </el-tag>
                </div>
              </div>
            </div>

            <div class="reasoning-item" v-if="evaluationReasoning.academicReasoning">
              <div class="reasoning-title">
                <el-icon><Document /></el-icon>
                学术成果 <span class="score-tag">{{ evaluationReasoning.academicReasoning.score }}</span>
              </div>
              <div class="reasoning-detail">
                <div class="reasoning-base">{{ evaluationReasoning.academicReasoning.summary }}</div>
                <div class="reasoning-factors">
                  <el-tag
                    v-for="(factor, idx) in evaluationReasoning.academicReasoning.factors"
                    :key="idx"
                    size="small"
                    type="info"
                    class="factor-tag"
                  >
                    {{ factor }}
                  </el-tag>
                </div>
              </div>
            </div>

            <div class="reasoning-item" v-if="evaluationReasoning.researchReasoning">
              <div class="reasoning-title">
                <el-icon><Connection /></el-icon>
                科研成果 <span class="score-tag">{{ evaluationReasoning.researchReasoning.score }}</span>
              </div>
              <div class="reasoning-detail">
                <div class="reasoning-base">{{ evaluationReasoning.researchReasoning.summary }}</div>
                <div class="reasoning-factors">
                  <el-tag
                    v-for="(factor, idx) in evaluationReasoning.researchReasoning.factors"
                    :key="idx"
                    size="small"
                    type="info"
                    class="factor-tag"
                  >
                    {{ factor }}
                  </el-tag>
                </div>
              </div>
            </div>

            <div class="reasoning-item" v-if="evaluationReasoning.exerciseReasoning">
              <div class="reasoning-title">
                <el-icon><Aim /></el-icon>
                演习训练 <span class="score-tag">{{ evaluationReasoning.exerciseReasoning.score }}</span>
              </div>
              <div class="reasoning-detail">
                <div class="reasoning-base">{{ evaluationReasoning.exerciseReasoning.summary }}</div>
                <div class="reasoning-factors">
                  <el-tag
                    v-for="(factor, idx) in evaluationReasoning.exerciseReasoning.factors"
                    :key="idx"
                    size="small"
                    type="info"
                    class="factor-tag"
                  >
                    {{ factor }}
                  </el-tag>
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 权重配置 -->
        <el-card class="weights-card">
          <template #header>
            <span>权重配置（总和 = 1.00）</span>
          </template>
          <el-descriptions :column="5" border size="small">
            <el-descriptions-item label="职称权重">{{ Number(selectedScore?.weightTitle || 0).toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="职务权重">{{ Number(selectedScore?.weightPosition || 0).toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="学历权重">{{ Number(selectedScore?.weightEducation || 0).toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="学术权重">{{ Number(selectedScore?.weightAcademic || 0).toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="科研权重">{{ Number(selectedScore?.weightResearch || 0).toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="演习权重">{{ Number(selectedScore?.weightExercise || 0).toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="军训权重">{{ Number(selectedScore?.weightMilitaryTraining || 0).toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="仿真权重">{{ Number(selectedScore?.weightSystemSimulation || 0).toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="统计权重">{{ Number(selectedScore?.weightStatistics || 0).toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="年限权重">{{ Number(selectedScore?.weightProfessionalYears || 0).toFixed(2) }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </div>
    </el-drawer>

    <!-- 生成模拟数据对话框 -->
    <el-dialog
      v-model="generateDialogVisible"
      :title="generateMode === 'overwrite' ? '覆盖生成模拟数据' : '新增生成模拟数据'"
      width="580px"
    >
      <el-form label-width="100px" :model="generateForm" ref="generateFormRef">
        <!-- 生成模式说明 -->
        <el-alert
          v-if="generateMode === 'overwrite'"
          title="覆盖模式已选中"
          type="warning"
          :closable="false"
          show-icon
          style="margin-bottom: 16px"
        >
          将清空所有现有专家数据和评估数据，重新生成。
        </el-alert>

        <el-form-item label="数据分布">
          <el-radio-group v-model="generateForm.distributionMode" size="default">
            <el-radio-button label="excellent">
              优秀偏多
              <el-tooltip content="A级40%, B级35%, C级20%, D级5%" placement="top">
                <el-icon class="info-icon"><InfoFilled /></el-icon>
              </el-tooltip>
            </el-radio-button>
            <el-radio-button label="balanced">均衡分布</el-radio-button>
            <el-radio-button label="ordinary">
              普通偏多
              <el-tooltip content="A级5%, B级20%, C级40%, D级35%" placement="top">
                <el-icon class="info-icon"><InfoFilled /></el-icon>
              </el-tooltip>
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <!-- 分布可视化 -->
        <el-form-item label="">
          <div class="distribution-preview">
            <div class="distribution-bar">
              <div class="bar-item bar-a" :style="{ width: distributionPreview[generateForm.distributionMode].A + '%' }">
                <span v-if="distributionPreview[generateForm.distributionMode].A > 8">A</span>
              </div>
              <div class="bar-item bar-b" :style="{ width: distributionPreview[generateForm.distributionMode].B + '%' }">
                <span v-if="distributionPreview[generateForm.distributionMode].B > 8">B</span>
              </div>
              <div class="bar-item bar-c" :style="{ width: distributionPreview[generateForm.distributionMode].C + '%' }">
                <span v-if="distributionPreview[generateForm.distributionMode].C > 8">C</span>
              </div>
              <div class="bar-item bar-d" :style="{ width: distributionPreview[generateForm.distributionMode].D + '%' }">
                <span v-if="distributionPreview[generateForm.distributionMode].D > 8">D</span>
              </div>
            </div>
            <div class="distribution-legend">
              <span><i class="legend-a"></i>A级 ≥90分</span>
              <span><i class="legend-b"></i>B级 ≥75分</span>
              <span><i class="legend-c"></i>C级 ≥60分</span>
              <span><i class="legend-d"></i>D级 &lt;60分</span>
            </div>
          </div>
        </el-form-item>

        <!-- 生成数量 -->
        <el-form-item label="生成数量">
          <el-input-number v-model="generateForm.count" :min="1" :max="100" />
          <span class="form-tip">位专家</span>
        </el-form-item>

        <el-form-item label="离散程度">
          <el-slider
            v-model="generateForm.dispersionPercent"
            :min="10"
            :max="100"
            :format-tooltip="(v) => v + '%'"
            style="max-width: 420px"
          />
          <div class="form-tip">越高则论文、项目、考核分等随机范围越宽，专家差异更明显</div>
        </el-form-item>

        <!-- 评估设置 -->
        <el-form-item label="评估设置">
          <el-checkbox v-model="generateForm.autoEvaluate">生成后自动评估</el-checkbox>
        </el-form-item>

        <!-- 预估结果 -->
        <el-divider content-position="left">预估分布结果</el-divider>
        <div class="estimate-result">
          <el-row :gutter="12">
            <el-col :span="6">
              <div class="estimate-item estimate-a">
                <div class="estimate-count">{{ estimateGradeCounts.A }}</div>
                <div class="estimate-label">A级</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="estimate-item estimate-b">
                <div class="estimate-count">{{ estimateGradeCounts.B }}</div>
                <div class="estimate-label">B级</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="estimate-item estimate-c">
                <div class="estimate-count">{{ estimateGradeCounts.C }}</div>
                <div class="estimate-label">C级</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="estimate-item estimate-d">
                <div class="estimate-count">{{ estimateGradeCounts.D }}</div>
                <div class="estimate-label">D级</div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="generateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="confirmGenerate">
          {{ generateMode === 'overwrite' ? '确认覆盖生成' : '确认新增生成' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 全局权重：统一应用到所有已评估记录并重算综合分 -->
    <el-dialog v-model="weightsDialogVisible" title="全局权重调整" width="640px" destroy-on-close>
      <p class="weights-hint">以下十项将写入每条评估记录；提交后按当前数值比例归一化为总和 1，并重算综合得分与等级。</p>
      <el-form label-width="120px" class="weights-form">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="职称权重"><el-input-number v-model="weightsForm.weightTitle" :min="0" :max="1" :step="0.01" :precision="2" controls-position="right" class="w-full-num" /></el-form-item>
            <el-form-item label="职务权重"><el-input-number v-model="weightsForm.weightPosition" :min="0" :max="1" :step="0.01" :precision="2" controls-position="right" class="w-full-num" /></el-form-item>
            <el-form-item label="学历权重"><el-input-number v-model="weightsForm.weightEducation" :min="0" :max="1" :step="0.01" :precision="2" controls-position="right" class="w-full-num" /></el-form-item>
            <el-form-item label="学术权重"><el-input-number v-model="weightsForm.weightAcademic" :min="0" :max="1" :step="0.01" :precision="2" controls-position="right" class="w-full-num" /></el-form-item>
            <el-form-item label="科研权重"><el-input-number v-model="weightsForm.weightResearch" :min="0" :max="1" :step="0.01" :precision="2" controls-position="right" class="w-full-num" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="演习权重"><el-input-number v-model="weightsForm.weightExercise" :min="0" :max="1" :step="0.01" :precision="2" controls-position="right" class="w-full-num" /></el-form-item>
            <el-form-item label="军训权重"><el-input-number v-model="weightsForm.weightMilitaryTraining" :min="0" :max="1" :step="0.01" :precision="2" controls-position="right" class="w-full-num" /></el-form-item>
            <el-form-item label="仿真权重"><el-input-number v-model="weightsForm.weightSystemSimulation" :min="0" :max="1" :step="0.01" :precision="2" controls-position="right" class="w-full-num" /></el-form-item>
            <el-form-item label="统计权重"><el-input-number v-model="weightsForm.weightStatistics" :min="0" :max="1" :step="0.01" :precision="2" controls-position="right" class="w-full-num" /></el-form-item>
            <el-form-item label="年限权重"><el-input-number v-model="weightsForm.weightProfessionalYears" :min="0" :max="1" :step="0.01" :precision="2" controls-position="right" class="w-full-num" /></el-form-item>
          </el-col>
        </el-row>
        <div class="weights-sum">当前合计：<strong>{{ weightsSumDisplay }}</strong>（提交后自动按比例归一化）</div>
      </el-form>
      <template #footer>
        <el-button @click="weightsDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="weightsSaving" :disabled="weightsSum <= 0" @click="submitGlobalWeights">应用并重算</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  User, MagicStick, TrendCharts, Refresh, ArrowDown, DocumentAdd, RefreshRight,
  Medal, Briefcase, Reading, Document, Connection, Aim, InfoFilled, Setting
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  getExpertsWithStatus,
  getStatistics,
  getEvaluationDetails,
  evaluateExpert,
  evaluateBatch,
  generateAndEvaluate,
  deleteExpert as apiDeleteExpert,
  getAllScores,
  applyGlobalWeights
} from '@/api'

// ========== 数据状态 ==========
const loading = ref(false)
const generating = ref(false)
const evaluating = ref(false)
const drawerVisible = ref(false)
const generateDialogVisible = ref(false)
const generateMode = ref('append')

const generateForm = ref({
  count: 10,
  distributionMode: 'balanced',
  autoEvaluate: true,
  dispersionPercent: 65
})

const weightsDialogVisible = ref(false)
const weightsSaving = ref(false)
const defaultWeightsForm = () => ({
  weightTitle: 0.1,
  weightPosition: 0.08,
  weightEducation: 0.08,
  weightAcademic: 0.1,
  weightResearch: 0.1,
  weightExercise: 0.1,
  weightMilitaryTraining: 0.12,
  weightSystemSimulation: 0.12,
  weightStatistics: 0.1,
  weightProfessionalYears: 0.1
})
const weightsForm = ref(defaultWeightsForm())

const WEIGHT_KEYS = [
  'weightTitle', 'weightPosition', 'weightEducation', 'weightAcademic', 'weightResearch',
  'weightExercise', 'weightMilitaryTraining', 'weightSystemSimulation', 'weightStatistics', 'weightProfessionalYears'
]

const weightsSum = computed(() =>
  WEIGHT_KEYS.reduce((acc, k) => acc + Number(weightsForm.value[k] ?? 0), 0)
)
const weightsSumDisplay = computed(() => weightsSum.value.toFixed(2))

/** 最大余额法：预估各等级人数，保证 A+B+C+D = total */
function gradeEstimateCounts(total, preview) {
  const keys = ['A', 'B', 'C', 'D']
  const exact = keys.map((k) => (total * preview[k]) / 100)
  const base = exact.map((x) => Math.floor(x))
  let rem = total - base.reduce((a, b) => a + b, 0)
  const order = exact.map((x, i) => ({ i, r: x - base[i] })).sort((a, b) => b.r - a.r)
  for (let k = 0; k < rem; k++) base[order[k].i]++
  return { A: base[0], B: base[1], C: base[2], D: base[3] }
}

const estimateGradeCounts = computed(() => {
  const mode = generateForm.value.distributionMode
  const preview = distributionPreview[mode]
  return gradeEstimateCounts(generateForm.value.count, preview)
})

const allExperts = ref([])
const evaluatedExperts = ref([])
const unevaluatedExperts = ref([])
const selectedExpert = ref(null)
const selectedScore = ref(null)
const evaluationReasoning = ref(null)
const statistics = ref({})
const levelFilter = ref('')
const viewMode = ref('all')

const radarChartRef = ref(null)
const barChartRef = ref(null)
let radarChart = null
let barChart = null

// ========== 分布预览配置 ==========
const distributionPreview = {
  excellent: { A: 40, B: 35, C: 20, D: 5 },
  balanced: { A: 20, B: 30, C: 35, D: 15 },
  ordinary: { A: 5, B: 20, C: 40, D: 35 }
}

// ========== 计算属性 ==========
const unevaluatedCount = computed(() => unevaluatedExperts.value.length)

const showDimensionColumns = computed(() => viewMode.value !== 'unevaluated')

const emptyDescription = computed(() => {
  if (loading.value) return '加载中...'
  if (viewMode.value === 'evaluated') return '暂无已评估专家'
  if (viewMode.value === 'unevaluated') return '暂无待评估专家'
  return '暂无专家数据，请先生成模拟数据'
})

const displayedExperts = computed(() => {
  let list = [...allExperts.value]

  // 按视图模式过滤
  if (viewMode.value === 'evaluated') {
    list = list.filter(e => e.hasScore)
  } else if (viewMode.value === 'unevaluated') {
    list = list.filter(e => !e.hasScore)
  }

  // 按等级过滤
  if (levelFilter.value) {
    list = list.filter(e => e.credibilityLevel === levelFilter.value)
  }

  return list
})

// ========== 加载数据 ==========
const loadData = async () => {
  loading.value = true
  try {
    const [statusData, statsData] = await Promise.all([
      getExpertsWithStatus(),
      getStatistics()
    ])

    evaluatedExperts.value = statusData.evaluated || []
    unevaluatedExperts.value = statusData.unevaluated || []
    allExperts.value = [...evaluatedExperts.value, ...unevaluatedExperts.value]
    statistics.value = statsData || {}
  } catch (e) {
    ElMessage.error('加载数据失败: ' + (e.message || ''))
  } finally {
    loading.value = false
  }
}

// ========== 操作处理 ==========
const handleGenerateCommand = (command) => {
  generateMode.value = command
  generateForm.value.count = 10
  generateForm.value.distributionMode = 'balanced'
  generateForm.value.autoEvaluate = true
  generateForm.value.dispersionPercent = 65
  generateDialogVisible.value = true
}

const confirmGenerate = async () => {
  generating.value = true
  try {
    const res = await generateAndEvaluate({
      count: generateForm.value.count,
      generateMode: generateMode.value,
      distributionMode: generateForm.value.distributionMode,
      autoEvaluate: generateForm.value.autoEvaluate,
      dispersion: generateForm.value.dispersionPercent / 100
    })
    ElMessage.success(`成功生成 ${res.totalGenerated || 0} 位专家${res.totalEvaluated > 0 ? `，评估了 ${res.totalEvaluated} 位` : ''}`)
    generateDialogVisible.value = false
    await loadData()
  } catch (e) {
    ElMessage.error('生成失败: ' + (e.message || ''))
  } finally {
    generating.value = false
  }
}

const handleBatchEvaluate = async () => {
  try {
    await ElMessageBox.confirm(
      `将对 ${unevaluatedCount.value} 位未评估专家进行可信度评估，是否继续？`,
      '批量评估确认',
      { type: 'warning' }
    )
  } catch {
    return
  }

  evaluating.value = true
  try {
    const unevaluatedIds = unevaluatedExperts.value.map(e => e.expertId)
    await evaluateBatch(unevaluatedIds, true)
    ElMessage.success('批量评估完成')
    await loadData()
  } catch (e) {
    ElMessage.error('评估失败: ' + (e.message || ''))
  } finally {
    evaluating.value = false
  }
}

const handleRefresh = () => {
  levelFilter.value = ''
  viewMode.value = 'all'
  loadData()
  ElMessage.success('数据已刷新')
}

const handleLevelFilter = () => {
  // displayedExperts 会自动响应
}

const handleEvaluateOne = async (row) => {
  try {
    await ElMessageBox.confirm(`将对专家「${row.expertName}」进行可信度评估，是否继续？`, '确认', { type: 'info' })
  } catch {
    return
  }

  try {
    await evaluateExpert(row.expertId)
    ElMessage.success('评估成功')
    await loadData()
  } catch (e) {
    ElMessage.error('评估失败: ' + (e.message || ''))
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除专家「${row.expertName}」吗？此操作将同时删除其评估记录。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger'
    })
  } catch {
    return
  }

  try {
    await apiDeleteExpert(row.expertId)
    ElMessage.success('删除成功')
    await loadData()
  } catch (e) {
    ElMessage.error('删除失败: ' + (e.message || ''))
  }
}

// ========== 详情抽屉 ==========
const handleRowClick = async (row) => {
  if (row.hasScore) {
    handleShowDetail(row)
  } else {
    handleEvaluateOne(row)
  }
}

const handleShowDetail = async (row) => {
  try {
    const detailsData = await getEvaluationDetails(row.expertId)
    const ex = detailsData?.expert
    selectedExpert.value = ex && typeof ex === 'object' ? { ...ex } : { ...row }

    if (detailsData?.score) {
      selectedScore.value = detailsData.score
      evaluationReasoning.value = detailsData.reasoning ?? null
    } else {
      selectedScore.value = {
        totalScore: row.totalScore,
        credibilityLevel: row.credibilityLevel,
        evaluationDate: row.evaluationDate
      }
      evaluationReasoning.value = null
    }

    drawerVisible.value = true
    await nextTick()
    initCharts()
  } catch (e) {
    ElMessage.error('加载详情失败: ' + (e.message || ''))
  }
}

const formatDash = (v) => (v != null && v !== '' ? String(v) : '—')
const formatYears = (y) => (y != null && y !== '' ? `${y}年` : '—')

const openWeightsDialog = async () => {
  try {
    const scores = await getAllScores()
    const s = scores && scores[0]
    if (s) {
      weightsForm.value = {
        weightTitle: Number(s.weightTitle ?? 0.1),
        weightPosition: Number(s.weightPosition ?? 0.08),
        weightEducation: Number(s.weightEducation ?? 0.08),
        weightAcademic: Number(s.weightAcademic ?? 0.1),
        weightResearch: Number(s.weightResearch ?? 0.1),
        weightExercise: Number(s.weightExercise ?? 0.1),
        weightMilitaryTraining: Number(s.weightMilitaryTraining ?? 0.12),
        weightSystemSimulation: Number(s.weightSystemSimulation ?? 0.12),
        weightStatistics: Number(s.weightStatistics ?? 0.1),
        weightProfessionalYears: Number(s.weightProfessionalYears ?? 0.1)
      }
    } else {
      weightsForm.value = defaultWeightsForm()
    }
    weightsDialogVisible.value = true
  } catch (e) {
    ElMessage.error('加载当前权重失败: ' + (e.message || ''))
  }
}

const submitGlobalWeights = async () => {
  if (weightsSum.value <= 0) {
    ElMessage.warning('十项权重之和须大于 0')
    return
  }
  weightsSaving.value = true
  try {
    await applyGlobalWeights(weightsForm.value)
    ElMessage.success('全局权重已应用，综合得分已按新权重重算')
    weightsDialogVisible.value = false
    await loadData()
  } catch (e) {
    ElMessage.error('更新失败: ' + (e.message || ''))
  } finally {
    weightsSaving.value = false
  }
}

// ========== 图表 ==========
const dimensionLabels = [
  '职称', '职务', '学历', '学术', '科研', '演习', '军训', '仿真', '统计', '年限'
]

const dimensionKeys = [
  'titleQl', 'positionQl', 'educationExperienceQl', 'academicAchievementsQl',
  'researchAchievementsQl', 'exerciseExperienceQl', 'militaryTrainingKnowledgeQl',
  'systemSimulationKnowledgeQl', 'statisticsKnowledgeQl', 'professionalYearsQt'
]

const initCharts = () => {
  if (!selectedScore.value) return

  const values = dimensionKeys.map(k => Number(selectedScore.value[k] || 0))

  // 雷达图
  if (radarChartRef.value) {
    if (radarChart) radarChart.dispose()
    radarChart = echarts.init(radarChartRef.value)
    radarChart.setOption({
      tooltip: {},
      radar: {
        indicator: dimensionLabels.map((name, i) => ({
          name,
          max: 100
        })),
        radius: '65%',
        axisName: { color: '#409EFF', fontSize: 12 }
      },
      series: [{
        type: 'radar',
        data: [{
          value: values,
          name: '可信度得分',
          areaStyle: { color: 'rgba(64, 158, 255, 0.3)' },
          lineStyle: { color: '#409EFF', width: 2 },
          itemStyle: { color: '#409EFF' }
        }]
      }]
    })
  }

  // 柱状图
  if (barChartRef.value) {
    if (barChart) barChart.dispose()
    barChart = echarts.init(barChartRef.value)
    const colors = values.map(v => getScoreColor(v))
    barChart.setOption({
      tooltip: { trigger: 'axis', formatter: '{b}: {c} 分' },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
      xAxis: { type: 'category', data: dimensionLabels, axisLabel: { fontSize: 11 } },
      yAxis: { type: 'value', max: 100, axisLabel: { fontSize: 11 } },
      series: [{
        type: 'bar',
        data: values.map((v, i) => ({
          value: v,
          itemStyle: { color: colors[i] }
        })),
        barWidth: '50%',
        label: { show: true, position: 'top', fontSize: 10, formatter: '{c}' }
      }]
    })
  }
}

const destroyCharts = () => {
  if (radarChart) { radarChart.dispose(); radarChart = null }
  if (barChart) { barChart.dispose(); barChart = null }
}

watch(drawerVisible, (val) => {
  if (!val) destroyCharts()
})

// ========== 工具方法 ==========
const getScoreTagType = (score) => {
  const s = Number(score || 0)
  if (s >= 90) return 'success'
  if (s >= 75) return 'primary'
  if (s >= 60) return 'warning'
  return 'danger'
}

const getLevelTagType = (level) => {
  const map = { A: 'success', B: 'primary', C: 'warning', D: 'info' }
  return map[level] || 'info'
}

const getScoreColor = (score) => {
  const s = Number(score || 0)
  if (s >= 90) return '#67C23A'
  if (s >= 75) return '#409EFF'
  if (s >= 60) return '#E6A23C'
  return '#F56C6C'
}

const calculateAge = (birthDate) => {
  if (!birthDate) return '—'
  let birth
  if (Array.isArray(birthDate)) {
    const [y, mo, d] = birthDate
    birth = new Date(y, (mo || 1) - 1, d || 1)
  } else {
    birth = new Date(birthDate)
  }
  if (Number.isNaN(birth.getTime())) return '—'
  const today = new Date()
  let age = today.getFullYear() - birth.getFullYear()
  const m = today.getMonth() - birth.getMonth()
  if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age--
  return age
}

// ========== 初始化 ==========
onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.expert-credibility {
  .page-card {
    min-height: calc(100vh - 120px);
  }

  .card-header {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 18px;

    .header-icon {
      font-size: 24px;
      color: var(--accent-gold);
    }
  }

  .toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    padding: 12px 16px;
    background: #f8faff;
    border-radius: 6px;
    border: 1px solid #e4eaf3;
  }

  .stats-overview {
    margin-bottom: 16px;

    .stat-card {
      padding: 16px;
      border-radius: 8px;
      text-align: center;
      color: #fff;

      .stat-value {
        font-size: 28px;
        font-weight: bold;
        margin-bottom: 4px;
      }

      .stat-label {
        font-size: 13px;
        opacity: 0.9;
      }
    }

    .stat-total { background: linear-gradient(135deg, #667eea, #764ba2); }
    .stat-a { background: linear-gradient(135deg, #67C23A, #85ce61); }
    .stat-b { background: linear-gradient(135deg, #409EFF, #66b1ff); }
    .stat-c { background: linear-gradient(135deg, #E6A23C, #ebb563); }
    .stat-d { background: linear-gradient(135deg, #909399, #a6a9ad); }
    .stat-avg { background: linear-gradient(135deg, #F56C6C, #f78989); }
  }

  .eval-status-tag {
    min-width: 64px;
    justify-content: center;
    font-weight: 500;
    -webkit-font-smoothing: antialiased;
    text-rendering: geometricPrecision;
  }

  .text-muted {
    color: #c0c4cc;
  }

  .expert-name {
    font-weight: 500;
  }

  .expert-detail {
    .info-card, .score-card, .chart-card, .weights-card, .reasoning-card {
      margin-bottom: 16px;
    }

    .total-score-display {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 20px 0;

      .score-circle {
        width: 140px;
        height: 140px;
        border-radius: 50%;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        color: #fff;

        &.level-A { background: linear-gradient(135deg, #67C23A, #85ce61); }
        &.level-B { background: linear-gradient(135deg, #409EFF, #66b1ff); }
        &.level-C { background: linear-gradient(135deg, #E6A23C, #ebb563); }
        &.level-D { background: linear-gradient(135deg, #909399, #a6a9ad); }

        .score-value {
          font-size: 42px;
          font-weight: bold;
          line-height: 1;
        }

        .score-level {
          font-size: 16px;
          margin-top: 6px;
        }
      }

      .score-meta {
        margin-top: 12px;
        color: #909399;
        font-size: 14px;
      }
    }

    .reasoning-card {
      .reasoning-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 16px;
      }

      .reasoning-item {
        background: #f8f9fa;
        border-radius: 8px;
        padding: 12px 16px;
        border-left: 4px solid #409EFF;

        .reasoning-title {
          display: flex;
          align-items: center;
          gap: 8px;
          font-weight: 600;
          margin-bottom: 8px;
          color: #303133;

          .score-tag {
            margin-left: auto;
            background: #409EFF;
            color: #fff;
            padding: 2px 8px;
            border-radius: 4px;
            font-size: 14px;
          }
        }

        .reasoning-detail {
          .reasoning-base {
            font-size: 14px;
            color: #606266;
            margin-bottom: 8px;
          }

          .reasoning-factors {
            display: flex;
            flex-wrap: wrap;
            gap: 4px;

            .factor-tag {
              margin-right: 4px;
            }
          }
        }
      }
    }
  }

  // 生成对话框样式
  .distribution-preview {
    width: 100%;
    background: #f5f7fa;
    border-radius: 8px;
    padding: 12px;

    .distribution-bar {
      display: flex;
      height: 32px;
      border-radius: 4px;
      overflow: hidden;
      margin-bottom: 8px;

      .bar-item {
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
        font-size: 12px;
        font-weight: 600;
        transition: width 0.3s;

        &.bar-a { background: linear-gradient(135deg, #67C23A, #85ce61); }
        &.bar-b { background: linear-gradient(135deg, #409EFF, #66b1ff); }
        &.bar-c { background: linear-gradient(135deg, #E6A23C, #ebb563); }
        &.bar-d { background: linear-gradient(135deg, #909399, #a6a9ad); }
      }
    }

    .distribution-legend {
      display: flex;
      justify-content: space-around;
      font-size: 12px;
      color: #606266;

      span {
        display: flex;
        align-items: center;
        gap: 4px;
      }

      i {
        width: 12px;
        height: 12px;
        border-radius: 2px;

        &.legend-a { background: #67C23A; }
        &.legend-b { background: #409EFF; }
        &.legend-c { background: #E6A23C; }
        &.legend-d { background: #909399; }
      }
    }
  }

  .form-tip {
    margin-left: 8px;
    color: #909399;
  }

  .estimate-result {
    .estimate-item {
      background: linear-gradient(135deg, #f0f2f5, #e4e7ed);
      border-radius: 8px;
      padding: 16px;
      text-align: center;

      .estimate-count {
        font-size: 28px;
        font-weight: bold;
        color: #303133;
      }

      .estimate-label {
        font-size: 13px;
        color: #909399;
        margin-top: 4px;
      }

      &.estimate-a { background: linear-gradient(135deg, rgba(103, 194, 58, 0.1), rgba(133, 206, 97, 0.1)); border: 1px solid rgba(103, 194, 58, 0.3); }
      &.estimate-b { background: linear-gradient(135deg, rgba(64, 158, 255, 0.1), rgba(102, 177, 255, 0.1)); border: 1px solid rgba(64, 158, 255, 0.3); }
      &.estimate-c { background: linear-gradient(135deg, rgba(230, 162, 60, 0.1), rgba(235, 181, 99, 0.1)); border: 1px solid rgba(230, 162, 60, 0.3); }
      &.estimate-d { background: linear-gradient(135deg, rgba(144, 147, 153, 0.1), rgba(166, 169, 173, 0.1)); border: 1px solid rgba(144, 147, 153, 0.3); }
    }
  }

  .info-icon {
    margin-left: 4px;
    cursor: help;
  }

  .weights-hint {
    font-size: 13px;
    color: #606266;
    margin: 0 0 12px;
    line-height: 1.5;
  }

  .weights-form .w-full-num {
    width: 100%;
  }

  .weights-sum {
    margin-top: 8px;
    font-size: 13px;
    color: #909399;
  }
}
</style>
