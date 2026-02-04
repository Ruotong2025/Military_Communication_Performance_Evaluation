<template>
  <div class="data-view">
    <el-card class="page-card">
      <template #header>
        <div class="card-header">
          <el-icon class="header-icon"><DataAnalysis /></el-icon>
          <span>基础数据查看</span>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="实验信息表" name="communication_network_lifecycle">
          <DynamicTable :table-name="activeTab" />
        </el-tab-pane>
        
        <el-tab-pane label="通信信息表" name="during_battle_communications">
          <DynamicTable :table-name="activeTab" />
        </el-tab-pane>
        
        <el-tab-pane label="指标信息表" name="military_effectiveness_evaluation">
          <DynamicTable :table-name="activeTab" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { DataAnalysis } from '@element-plus/icons-vue'
import DynamicTable from '@/components/DynamicTable.vue'

const activeTab = ref('communication_network_lifecycle')

const handleTabChange = (tabName) => {
  console.log('切换到表:', tabName)
}
</script>

<style scoped lang="scss">
.data-view {
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
}
</style>
