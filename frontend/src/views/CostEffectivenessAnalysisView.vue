<template>
  <div class="placeholder-page">
    <div class="page-header">
      <h2>
        <el-icon><PieChart /></el-icon>
        评估结果计算 — 效费分析
      </h2>
      <p class="subtitle">本模块用于效费比分析，功能规划中。</p>
    </div>
    <el-empty description="敬请期待" />
  </div>
</template>

<script setup>
import { PieChart } from '@element-plus/icons-vue'
</script>

<style scoped lang="scss">
.placeholder-page {
  padding: 0;
}
.page-header {
  margin-bottom: 24px;
  h2 {
    display: flex;
    align-items: center;
    gap: 10px;
    margin: 0 0 8px 0;
    font-size: 22px;
    color: #303133;
  }
  .subtitle {
    margin: 0;
    color: #909399;
    font-size: 14px;
  }
}
</style>
