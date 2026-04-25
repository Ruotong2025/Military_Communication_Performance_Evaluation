<template>
  <div class="simulation-training-home">
    <!-- 页面标题区 -->
    <div class="page-hero">
      <div class="hero-content">
        <h1>
          <el-icon class="hero-icon"><Guide /></el-icon>
          军事通信性能仿真训练平台
        </h1>
        <p class="hero-subtitle">系统化评估军事通信装备作战效能与综合性能</p>
      </div>
      <div class="hero-stats">
        <div class="stat-item">
          <span class="stat-number">10</span>
          <span class="stat-label">功能模块</span>
        </div>
        <div class="stat-item">
          <span class="stat-number">3</span>
          <span class="stat-label">评估阶段</span>
        </div>
        <div class="stat-item">
          <span class="stat-number">5</span>
          <span class="stat-label">操作步骤</span>
        </div>
      </div>
    </div>

    <!-- 操作流程指引 -->
    <el-card class="flow-guide-card" shadow="hover">
      <template #header>
        <div class="flow-header">
          <span>
            <el-icon><Connection /></el-icon>
            系统操作流程图
          </span>
          <el-tag type="primary" size="small" effect="dark">按 1 → 2 → 3 → 4 → 5 顺序执行</el-tag>
        </div>
      </template>

      <!-- 流程图可视化 -->
      <div class="visual-flow">
        <div class="flow-track">
          <div v-for="(step, index) in visualFlowSteps" :key="index" class="flow-step-wrapper">
            <!-- 连接线 -->
            <div v-if="index > 0" class="flow-connector">
              <div class="connector-line" :style="{ background: step.connectorColor }"></div>
              <div class="connector-arrow"></div>
            </div>

            <!-- 步骤节点 -->
            <div class="flow-step" :style="{ '--step-color': step.color }">
              <div class="step-badge">{{ step.num }}</div>
              <div class="step-icon-wrapper">
                <el-icon class="step-icon"><component :is="step.icon" /></el-icon>
              </div>
              <div class="step-content">
                <span class="step-title">{{ step.title }}</span>
                <span class="step-modules">{{ step.modules }}</span>
              </div>
              <div class="step-indicator" :style="{ background: step.color }"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 阶段说明 -->
      <el-divider content-position="center">
        <span class="phase-divider-text">三大评估阶段</span>
      </el-divider>

      <div class="phase-legend">
        <div v-for="phase in phaseInfo" :key="phase.id" class="phase-legend-item">
          <div class="phase-color-bar" :style="{ background: phase.color }"></div>
          <div class="phase-legend-content">
            <span class="phase-legend-title">{{ phase.title }}</span>
            <span class="phase-legend-desc">{{ phase.desc }}</span>
          </div>
        </div>
      </div>
    </el-card>

      <!-- 功能模块网格 -->
    <div class="page-header">
      <h2>
        <el-icon><Grid /></el-icon>
        功能模块中心
      </h2>
      <p class="subtitle">选择以下模块开始评估工作，必做模块需按顺序完成</p>
    </div>

    <!-- 阶段一：数据准备 -->
    <div class="phase-section">
      <div class="phase-title-bar" style="--phase-color: #001f3f;">
        <el-icon><Document /></el-icon>
        <span>阶段一：数据准备</span>
        <el-tag style="background: #1c4a9a; border-color: #1c4a9a;" size="small" effect="dark">3个模块</el-tag>
      </div>
      <div class="cards-grid">
        <div v-for="card in phase1Cards" :key="card.path" class="module-card" @click="go(card.path)">
          <div class="card-accent" :style="{ background: card.accentColor }"></div>
          <div class="card-body">
            <div class="card-icon-wrapper" :style="{ background: card.iconBg }">
              <el-icon class="card-icon"><component :is="card.icon" /></el-icon>
            </div>
            <div class="card-info">
              <h3>{{ card.title }}</h3>
              <div class="card-tags">
                <el-tag :type="card.required ? 'primary' : 'info'" size="small" effect="dark">
                  {{ card.required ? '必做' : '可选' }}
                </el-tag>
              </div>
            </div>
            <p class="card-desc">{{ card.desc }}</p>
            <div class="card-usage">
              <el-icon><Clock /></el-icon>
              <span>{{ card.whenToUse }}</span>
            </div>
            <div class="card-footer">
              <span class="enter-hint">点击进入</span>
              <el-icon class="arrow-icon"><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 阶段二：权重确定 -->
    <div class="phase-section">
      <div class="phase-title-bar" style="--phase-color: #003580;">
        <el-icon><DataAnalysis /></el-icon>
        <span>阶段二：权重确定</span>
        <el-tag style="background: #003580; border-color: #003580;" size="small" effect="dark">3个模块</el-tag>
      </div>
      <div class="cards-grid">
        <div v-for="card in phase2Cards" :key="card.path" class="module-card" @click="go(card.path)">
          <div class="card-accent" :style="{ background: card.accentColor }"></div>
          <div class="card-body">
            <div class="card-icon-wrapper" :style="{ background: card.iconBg }">
              <el-icon class="card-icon"><component :is="card.icon" /></el-icon>
            </div>
            <div class="card-info">
              <h3>{{ card.title }}</h3>
              <div class="card-tags">
                <el-tag :type="card.required ? 'primary' : 'info'" size="small" effect="dark">
                  {{ card.required ? '必做' : '可选' }}
                </el-tag>
              </div>
            </div>
            <p class="card-desc">{{ card.desc }}</p>
            <div class="card-usage">
              <el-icon><Clock /></el-icon>
              <span>{{ card.whenToUse }}</span>
            </div>
            <div class="card-footer">
              <span class="enter-hint">点击进入</span>
              <el-icon class="arrow-icon"><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 阶段三：结果计算 -->
    <div class="phase-section">
      <div class="phase-title-bar" style="--phase-color: #0050a0;">
        <el-icon><PieChart /></el-icon>
        <span>阶段三：结果计算</span>
        <el-tag style="background: #0050a0; border-color: #0050a0;" size="small" effect="dark">3个模块</el-tag>
      </div>
      <div class="cards-grid">
        <div v-for="card in phase3Cards" :key="card.path" class="module-card" @click="go(card.path)">
          <div class="card-accent" :style="{ background: card.accentColor }"></div>
          <div class="card-body">
            <div class="card-icon-wrapper" :style="{ background: card.iconBg }">
              <el-icon class="card-icon"><component :is="card.icon" /></el-icon>
            </div>
            <div class="card-info">
              <h3>{{ card.title }}</h3>
              <div class="card-tags">
                <el-tag :type="card.required ? 'primary' : 'info'" size="small" effect="dark">
                  {{ card.required ? '必做' : '可选' }}
                </el-tag>
              </div>
            </div>
            <p class="card-desc">{{ card.desc }}</p>
            <div class="card-usage">
              <el-icon><Clock /></el-icon>
              <span>{{ card.whenToUse }}</span>
            </div>
            <div class="card-footer">
              <span class="enter-hint">点击进入</span>
              <el-icon class="arrow-icon"><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部操作提示 -->
    <el-card class="tips-card" shadow="never">
      <template #header>
        <span>
          <el-icon><InfoFilled /></el-icon>
          温馨提示
        </span>
      </template>
      <div class="tips-grid">
        <div class="tip-item">
          <el-icon><CircleCheck /></el-icon>
          <span>必做模块请按阶段顺序完成，确保数据连贯性</span>
        </div>
        <div class="tip-item">
          <el-icon><Timer /></el-icon>
          <span>可选模块可随时使用，提供更全面的分析视角</span>
        </div>
        <div class="tip-item">
          <el-icon><FolderOpened /></el-icon>
          <span>数据会自动保存，可在各模块间切换查看</span>
        </div>
        <div class="tip-item">
          <el-icon><QuestionFilled /></el-icon>
          <span>如有疑问，请查看各模块内的操作说明</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import {
  Guide,
  ArrowRight,
  DataAnalysis,
  User,
  TrendCharts,
  DataLine,
  Flag,
  CircleCheck,
  Warning,
  PieChart,
  EditPen,
  Upload,
  Clock,
  Grid,
  Document,
  InfoFilled,
  Timer,
  FolderOpened,
  QuestionFilled,
  Connection
} from '@element-plus/icons-vue'

const router = useRouter()

// 可视化流程步骤 - 基于主色 #001f3f 的渐变色系
const visualFlowSteps = [
  {
    num: '1',
    title: '数据准备',
    modules: '上传模拟数据',
    icon: Upload,
    color: '#001f3f',
    connectorColor: '#0a2d5a'
  },
  {
    num: '2',
    title: '专家评估',
    modules: '可信度与定性',
    icon: User,
    color: '#1c4a9a',
    connectorColor: '#2558ba'
  },
  {
    num: '3',
    title: '权重确定',
    modules: 'AHP分析与集结',
    icon: TrendCharts,
    color: '#3774fa',
    connectorColor: '#4082ff'
  },
  {
    num: '4',
    title: '综合评估',
    modules: '计算综合得分',
    icon: CircleCheck,
    color: '#5390ff',
    connectorColor: '#669eff'
  },
  {
    num: '5',
    title: '效费分析',
    modules: '成本效益评估',
    icon: PieChart,
    color: '#7AACFF',
    connectorColor: '#8EBBFF'
  }
]

// 阶段信息 - 基于主色 #001f3f 的渐变色系
const phaseInfo = [
  {
    id: 1,
    title: '数据准备',
    desc: '上传作战模拟数据、专家可信度评估、装备定性数据',
    color: '#001f3f'
  },
  {
    id: 2,
    title: '权重确定',
    desc: 'AHP层次分析、离散度检验、权重集结计算',
    color: '#2558ba'
  },
  {
    id: 3,
    title: '结果计算',
    desc: '综合评估打分、惩罚因子、效费比分析',
    color: '#4082ff'
  }
]

// 阶段一：数据准备 - 基于主色 #001f3f
const phase1Cards = [
  {
    path: '/simulation-training/data',
    title: '军事作战模拟数据',
    desc: '上传作战实验数据、装备性能指标数据',
    icon: Upload,
    required: true,
    phase: '阶段一',
    accentColor: '#001f3f',
    iconBg: 'rgba(0, 31, 63, 0.1)',
    whenToUse: '项目启动时，首先上传作战模拟数据'
  },
  {
    path: '/simulation-training/weights/expert',
    title: '专家可信度评估',
    desc: '评估专家权威性与判断把握度',
    icon: User,
    required: true,
    phase: '阶段一',
    accentColor: '#0a2d5a',
    iconBg: 'rgba(10, 45, 90, 0.1)',
    whenToUse: '数据准备完成后，评估各专家可信度'
  },
  {
    path: '/simulation-training/equipment-evaluation',
    title: '专家定性数据评估',
    desc: '专家对装备操作性能进行定性打分',
    icon: EditPen,
    required: true,
    phase: '阶段一',
    accentColor: '#1c4a9a',
    iconBg: 'rgba(28, 74, 154, 0.1)',
    whenToUse: '可信度评估后，专家进行定性评估'
  },
  {
    path: '/simulation-training/dynamic-indicator',
    title: '动态指标管理',
    desc: '通过Excel导入自定义指标体系',
    icon: TrendCharts,
    required: true,
    phase: '阶段一',
    accentColor: '#2563eb',
    iconBg: 'rgba(37, 99, 235, 0.1)',
    whenToUse: '导入指标定义后进行AHP权重配置'
  },
  {
    path: '/simulation-training/dynamic-quantitative',
    title: '动态定量评估',
    desc: '根据指标模板动态生成评估表格，支持模拟数据',
    icon: Grid,
    required: false,
    phase: '阶段一',
    accentColor: '#059669',
    iconBg: 'rgba(5, 150, 105, 0.1)',
    whenToUse: '指标模板配置完成后，进行定量数据评估'
  }
]

// 阶段二：权重确定
const phase2Cards = [
  {
    path: '/simulation-training/weights/evaluation',
    title: '专家 AHP 打分',
    desc: '构建判断矩阵，进行层次分析法分析',
    icon: TrendCharts,
    required: true,
    phase: '阶段二',
    accentColor: '#2e66da',
    iconBg: 'rgba(46, 102, 218, 0.1)',
    whenToUse: '定性评估完成后，进行权重分析'
  },
  {
    path: '/simulation-training/weights/ahp-dispersion',
    title: '权重离散度分析',
    desc: '分析各专家权重意见的一致性',
    icon: DataLine,
    required: false,
    phase: '阶段二',
    accentColor: '#3774fa',
    iconBg: 'rgba(55, 116, 250, 0.1)',
    whenToUse: 'AHP打分后，检查专家意见一致性'
  },
  {
    path: '/simulation-training/weights/aggregation-scoring',
    title: '权重集结打分',
    desc: '计算集结参数与最终指标权重',
    icon: Flag,
    required: true,
    phase: '阶段二',
    accentColor: '#4082ff',
    iconBg: 'rgba(64, 130, 255, 0.1)',
    whenToUse: 'AHP完成后，集结专家权重意见'
  }
]

// 阶段三：结果计算
const phase3Cards = [
  {
    path: '/simulation-training/results/comprehensive-scoring',
    title: '综合打分',
    desc: '计算装备综合评估得分与排名',
    icon: CircleCheck,
    required: true,
    phase: '阶段三',
    accentColor: '#5390ff',
    iconBg: 'rgba(83, 144, 255, 0.1)',
    whenToUse: '权重集结后，计算最终综合得分'
  },
  {
    path: '/simulation-training/results/penalty-factor',
    title: '惩罚因子分析',
    desc: '分析关键指标不达标时的惩罚影响',
    icon: Warning,
    required: false,
    phase: '阶段三',
    accentColor: '#669eff',
    iconBg: 'rgba(102, 158, 255, 0.1)',
    whenToUse: '综合评估后，分析惩罚因子影响'
  },
  {
    path: '/simulation-training/results/cost-effectiveness',
    title: '效费分析',
    desc: '分析装备性能与成本的效益比',
    icon: PieChart,
    required: false,
    phase: '阶段三',
    accentColor: '#7AACFF',
    iconBg: 'rgba(122, 172, 255, 0.1)',
    whenToUse: '综合评估后，进行成本效益分析'
  }
]

// 跳转到指定页面
function go(path) {
  router.push(path)
}
</script>

<style scoped lang="scss">
.simulation-training-home {
  width: 100%;
  max-width: 100%;
  padding: 0 24px 40px;
}

/* 页面英雄区 */
.page-hero {
  background: #001f3f;
  border-radius: 12px;
  padding: 32px;
  margin-bottom: 24px;
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 24px;
  box-shadow: 0 4px 20px rgba(0, 31, 63, 0.3);

  .hero-content {
    h1 {
      font-size: 28px;
      margin-bottom: 8px;
      display: flex;
      align-items: center;
      gap: 12px;
      color: white;

      .hero-icon {
        font-size: 36px;
        color: #409EFF;
      }
    }

    .hero-subtitle {
      font-size: 15px;
      color: rgba(255, 255, 255, 0.8);
      margin: 0;
    }
  }

  .hero-stats {
    display: flex;
    gap: 32px;

    .stat-item {
      text-align: center;

      .stat-number {
        font-size: 36px;
        font-weight: bold;
        color: white;
        display: block;
      }

      .stat-label {
        font-size: 12px;
        color: rgba(255, 255, 255, 0.85);
        text-transform: uppercase;
        letter-spacing: 1px;
        font-weight: 500;
      }
    }
  }
}

/* 流程指引卡片 */
.flow-guide-card {
  margin-bottom: 24px;

  .flow-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 16px;
    font-weight: 600;

    > span {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }
}

/* 可视化流程图 */
.visual-flow {
  padding: 24px 0;

  .flow-track {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-wrap: wrap;
    gap: 0;
  }

  .flow-step-wrapper {
    display: flex;
    align-items: center;
  }

  .flow-connector {
    display: flex;
    align-items: center;
    width: 40px;
    position: relative;

    .connector-line {
      width: 100%;
      height: 3px;
      border-radius: 2px;
    }

    .connector-arrow {
      position: absolute;
      right: -2px;
      width: 0;
      height: 0;
      border-top: 6px solid transparent;
      border-bottom: 6px solid transparent;
      border-left: 8px solid currentColor;
    }
  }

  .flow-step {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    position: relative;
    padding: 16px 20px;
    background: white;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    transition: all 0.3s;
    min-width: 120px;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 24px rgba(0, 31, 63, 0.15);

      .step-icon-wrapper {
        transform: scale(1.1);
        background: linear-gradient(135deg, var(--step-color), color-mix(in srgb, var(--step-color) 60%, white));
      }
    }

    .step-badge {
      position: absolute;
      top: -8px;
      left: -8px;
      width: 24px;
      height: 24px;
      background: #001f3f;
      color: white;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      font-weight: bold;
      box-shadow: 0 2px 6px rgba(0, 31, 63, 0.3);
    }

    .step-icon-wrapper {
      width: 56px;
      height: 56px;
      border-radius: 50%;
      background: var(--step-color);
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.3s;
      box-shadow: 0 4px 12px color-mix(in srgb, var(--step-color) 50%, transparent);

      .step-icon {
        font-size: 28px;
        color: white;
      }
    }

    .step-content {
      text-align: center;

      .step-title {
        font-size: 14px;
        font-weight: 600;
        color: #303133;
        display: block;
      }

      .step-modules {
        font-size: 11px;
        color: #909399;
      }
    }

    .step-indicator {
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 40px;
      height: 3px;
      border-radius: 2px;
    }
  }
}

/* 阶段图例 */
.phase-legend {
  display: flex;
  justify-content: center;
  gap: 32px;
  flex-wrap: wrap;
  padding: 8px 0;

  .phase-legend-item {
    display: flex;
    align-items: center;
    gap: 12px;

    .phase-color-bar {
      width: 4px;
      height: 40px;
      border-radius: 2px;
    }

    .phase-legend-content {
      display: flex;
      flex-direction: column;
      gap: 2px;

      .phase-legend-title {
        font-size: 14px;
        font-weight: 600;
        color: #303133;
      }

      .phase-legend-desc {
        font-size: 11px;
        color: #909399;
      }
    }
  }
}

.phase-divider-text {
  font-size: 13px;
  color: #909399;
  font-weight: 500;
}

/* 页面标题 */
.page-header {
  margin-bottom: 20px;

  h2 {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 20px;
    color: var(--text-primary, #001f3f);
    margin-bottom: 6px;
  }

  .subtitle {
    color: #606266;
    font-size: 13px;
    line-height: 1.6;
    margin: 0;
  }
}

/* 阶段区块 */
.phase-section {
  margin-bottom: 32px;
}

.cards-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.phase-title-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  background: linear-gradient(90deg, var(--phase-color), color-mix(in srgb, var(--phase-color) 50%, #7AACFF));
  border-radius: 8px;
  margin-bottom: 16px;
  color: white;
  font-size: 15px;
  font-weight: 600;
  box-shadow: 0 3px 12px rgba(0, 31, 63, 0.2);

  .el-icon {
    font-size: 18px;
  }

  .el-tag {
    margin-left: auto;
  }
}

/* 模块卡片 */
.module-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 16px;
  border: 1px solid #ebeef5;
  position: relative;
  height: calc(100% - 16px);

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 32px rgba(0, 31, 63, 0.15);
    border-color: transparent;

    .card-accent {
      height: 100%;
    }

    .arrow-icon {
      transform: translateX(4px);
      color: #3774fa;
    }

    .enter-hint {
      color: #3774fa;
    }

    .card-icon {
      color: #3774fa;
    }
  }

  .card-accent {
    position: absolute;
    left: 0;
    top: 0;
    width: 4px;
    height: 0;
    transition: height 0.3s;
  }

  .card-body {
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .card-icon-wrapper {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;

    .card-icon {
      font-size: 24px;
      color: #303133;
    }
  }

  .card-info {
    display: flex;
    flex-direction: column;
    gap: 8px;

    h3 {
      font-size: 16px;
      margin: 0;
      color: #303133;
      line-height: 1.4;
    }

    .card-tags {
      display: flex;
      gap: 6px;
    }
  }

  .card-desc {
    font-size: 13px;
    color: #606266;
    line-height: 1.5;
    margin: 0;
    flex: 1;
  }

  .card-usage {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    padding: 10px 12px;
    background: #f5f7fa;
    border-radius: 6px;
    font-size: 12px;
    color: #909399;

    .el-icon {
      flex-shrink: 0;
      margin-top: 1px;
    }
  }

  .card-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 8px;
    border-top: 1px solid #ebeef5;

    .enter-hint {
      font-size: 13px;
      color: #909399;
      font-weight: 500;
      transition: color 0.3s;
    }

    .arrow-icon {
      font-size: 16px;
      color: #c0c4cc;
      transition: all 0.3s;
    }
  }
}

/* 提示卡片 - 基于主色 #001f3f 的渐变色系 */
.tips-card {
  background: linear-gradient(135deg, rgba(0, 31, 63, 0.05) 0%, rgba(55, 116, 250, 0.08) 100%);
  border: 1px solid rgba(0, 31, 63, 0.15);

  :deep(.el-card__header) {
    background: rgba(0, 31, 63, 0.08);
    border-bottom: 1px solid rgba(0, 31, 63, 0.12);

    > span {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 600;
      color: #001f3f;
    }
  }

  .tips-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 16px;

    .tip-item {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 12px;
      background: white;
      border-radius: 8px;
      font-size: 13px;
      color: #606266;

      .el-icon {
        font-size: 18px;
        color: #3774fa;
        flex-shrink: 0;
      }
    }
  }
}

/* 响应式适配 */
@media (max-width: 768px) {
  .page-hero {
    flex-direction: column;
    text-align: center;

    .hero-content h1 {
      justify-content: center;
      font-size: 22px;
    }

    .hero-stats {
      justify-content: center;
    }
  }

  .visual-flow {
    .flow-track {
      flex-direction: column;
      gap: 16px;
    }

    .flow-connector {
      width: 3px;
      height: 24px;
      transform: rotate(90deg);
    }

    .flow-step {
      min-width: 160px;
    }
  }

  .phase-legend {
    flex-direction: column;
    align-items: center;
  }
}
</style>
