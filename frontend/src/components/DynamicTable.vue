<template>
  <div class="dynamic-table">
    <el-table
      v-loading="loading"
      :data="tableData"
      border
      stripe
      style="width: 100%"
      max-height="600"
    >
      <el-table-column
        v-for="col in columns"
        :key="col.columnName"
        :prop="col.columnName"
        :label="col.columnComment || col.columnName"
        :width="getColumnWidth(col)"
        show-overflow-tooltip
      >
        <template #default="{ row }">
          {{ formatValue(row[col.columnName], col.dataType) }}
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      style="margin-top: 20px; justify-content: center"
    />
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { getTableStructure, getTableData } from '@/api'
import { ElMessage } from 'element-plus'

const props = defineProps({
  tableName: {
    type: String,
    required: true
  }
})

const loading = ref(false)
const columns = ref([])
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 加载表结构
const loadTableStructure = async () => {
  try {
    columns.value = await getTableStructure(props.tableName)
  } catch (error) {
    ElMessage.error('加载表结构失败')
    console.error(error)
  }
}

// 加载表数据
const loadTableData = async () => {
  loading.value = true
  try {
    const result = await getTableData(props.tableName, currentPage.value, pageSize.value)
    tableData.value = result.records
    total.value = result.total
  } catch (error) {
    ElMessage.error('加载表数据失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 格式化值
const formatValue = (value, dataType) => {
  if (value === null || value === undefined) return '-'
  if (dataType === 'datetime' || dataType === 'timestamp') {
    return new Date(value).toLocaleString('zh-CN')
  }
  if (typeof value === 'number') {
    return value.toFixed(4)
  }
  return value
}

// 获取列宽
const getColumnWidth = (col) => {
  if (col.columnName.includes('id')) return 100
  if (col.dataType === 'datetime' || col.dataType === 'timestamp') return 180
  if (col.dataType.includes('decimal')) return 150
  return undefined
}

const handleSizeChange = () => {
  currentPage.value = 1
  loadTableData()
}

const handleCurrentChange = () => {
  loadTableData()
}

// 监听表名变化
watch(() => props.tableName, () => {
  currentPage.value = 1
  loadTableStructure()
  loadTableData()
}, { immediate: true })

onMounted(() => {
  loadTableStructure()
  loadTableData()
})
</script>

<style scoped lang="scss">
.dynamic-table {
  :deep(.el-table) {
    font-size: 13px;

    th {
      font-size: 14px;
    }
  }
}
</style>
