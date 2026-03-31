<template>
  <el-container class="app-container">
    <!-- 顶部导航栏 -->
    <el-header class="app-header">
      <div class="header-left">
        <el-icon class="header-icon"><Trophy /></el-icon>
        <span class="header-title">军事通信效能评估系统</span>
      </div>
      <div class="header-right">
        <el-button type="primary" :icon="User" circle />
        <el-button type="primary" :icon="Setting" circle />
      </div>
    </el-header>

    <el-container class="main-container">
      <!-- 左侧导航栏 -->
      <el-aside width="240px" class="app-aside">
        <el-menu
          :default-active="activeMenu"
          :default-openeds="defaultOpenSubmenus"
          class="navy-menu"
          @select="handleMenuSelect"
        >
          <el-menu-item index="simulation-training">
            <el-icon><Guide /></el-icon>
            <span>模拟训练评估系统导航</span>
          </el-menu-item>

          <el-sub-menu index="sub-simulation-data">
            <template #title>
              <el-icon><FolderOpened /></el-icon>
              <span>模拟训练数据准备</span>
            </template>
            <el-menu-item index="simulation-training/data">
              <el-icon><DataAnalysis /></el-icon>
              <span>军事作战模拟数据</span>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="sub-simulation-weights">
            <template #title>
              <el-icon><Operation /></el-icon>
              <span>模拟训练权重确定</span>
            </template>
            <el-menu-item index="simulation-training/weights/expert">
              <el-icon><User /></el-icon>
              <span>专家可信度评估</span>
            </el-menu-item>
            <el-menu-item index="simulation-training/weights/evaluation">
              <el-icon><TrendCharts /></el-icon>
              <span>专家 AHP 打分</span>
            </el-menu-item>
            <el-menu-item index="simulation-training/weights/ahp-dispersion">
              <el-icon><DataLine /></el-icon>
              <span>权重离散度分析</span>
            </el-menu-item>
            <el-menu-item index="simulation-training/weights/aggregation-scoring">
              <el-icon><Flag /></el-icon>
              <span>权重集结打分</span>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="sub-simulation-results">
            <template #title>
              <el-icon><Histogram /></el-icon>
              <span>评估结果计算</span>
            </template>
            <el-menu-item index="simulation-training/results/comprehensive-scoring">
              <el-icon><CircleCheck /></el-icon>
              <span>综合打分</span>
            </el-menu-item>
            <el-menu-item index="simulation-training/results/penalty-factor">
              <el-icon><Warning /></el-icon>
              <span>惩罚因子分析</span>
            </el-menu-item>
            <el-menu-item index="simulation-training/results/cost-effectiveness">
              <el-icon><Coin /></el-icon>
              <span>效费分析</span>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>

      <!-- 主内容区 -->
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  Trophy,
  Setting,
  DataAnalysis,
  TrendCharts,
  User,
  DataLine,
  Guide,
  FolderOpened,
  Operation,
  Flag,
  Histogram,
  CircleCheck,
  Warning,
  Coin
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const activeMenu = ref('simulation-training')

const defaultOpenSubmenus = computed(() => {
  const open = []
  if (route.path.startsWith('/simulation-training/data')) {
    open.push('sub-simulation-data')
  }
  if (route.path.startsWith('/simulation-training/weights')) {
    open.push('sub-simulation-weights')
  }
  if (route.path.startsWith('/simulation-training/results')) {
    open.push('sub-simulation-results')
  }
  return open
})

const handleMenuSelect = (index) => {
  activeMenu.value = index
  router.push(`/${index}`)
}

router.afterEach((to) => {
  const p = to.path.replace(/^\//, '') || 'simulation-training'
  activeMenu.value = p
})
</script>

<style scoped lang="scss">
.app-container {
  height: 100vh;
  background: #f0f2f5;
}

.app-header {
  background: linear-gradient(135deg, #001f3f, #0074D9);
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  box-shadow: 0 2px 8px rgba(0, 31, 63, 0.3);

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .header-icon {
      font-size: 28px;
      color: #FFD700;
    }

    .header-title {
      font-size: 20px;
      font-weight: bold;
      letter-spacing: 1px;
    }
  }

  .header-right {
    display: flex;
    gap: 10px;
  }
}

.main-container {
  height: calc(100vh - 60px);
}

.app-aside {
  background: #001529;
  box-shadow: 2px 0 8px rgba(0, 31, 63, 0.2);
}

.app-main {
  background: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>
