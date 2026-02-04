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
      <el-aside width="220px" class="app-aside">
        <el-menu
          :default-active="activeMenu"
          class="navy-menu"
          @select="handleMenuSelect"
        >
          <el-menu-item index="data">
            <el-icon><DataAnalysis /></el-icon>
            <span>基础数据查看</span>
          </el-menu-item>
          <el-menu-item index="evaluation">
            <el-icon><TrendCharts /></el-icon>
            <span>效能评估分析</span>
          </el-menu-item>
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
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Trophy, User, Setting, DataAnalysis, TrendCharts } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const activeMenu = ref('data')

const handleMenuSelect = (index) => {
  activeMenu.value = index
  router.push(`/${index}`)
}

// 监听路由变化
router.afterEach((to) => {
  activeMenu.value = to.path.substring(1) || 'data'
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
