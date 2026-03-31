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

      <el-table-column v-if="canDeleteRows" label="操作" width="84" fixed="right" align="center">
        <template #default="{ row }">
          <el-button type="danger" link size="small" @click="handleDelete(row)">
            删除
          </el-button>
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
import { computed, ref, watch } from 'vue'
import { getTableStructure, getTableData, deleteTableRow } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { globalOperationId } from '@/composables/useGlobalOperationFilter'

const props = defineProps({
  tableName: {
    type: String,
    required: true
  },
  /** 父组件递增以触发整表刷新（生成数据后） */
  refreshTick: {
    type: Number,
    default: 0
  }
})

const loading = ref(false)
const columns = ref([])
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const deleting = ref(false)
const canDeleteRows = computed(() => props.tableName !== 'records_military_operation_info')

/** 作战ID筛选 */
const OPERATION_FILTERABLE = new Set([
  'records_military_operation_info',
  'records_military_communication_info',
  'records_link_maintenance_events',
  'records_security_events'
])
const showOperationFilter = computed(() => OPERATION_FILTERABLE.has(props.tableName))

const loadTableStructure = async () => {
  columns.value = await getTableStructure(props.tableName)
}

const loadTableData = async () => {
  const op =
    showOperationFilter.value ? globalOperationId.value : null
  const result = await getTableData(props.tableName, currentPage.value, pageSize.value, op)
  tableData.value = result.records
  total.value = result.total
}

const showLoadError = (error) => {
  const msg = error?.message || String(error)
  const net =
    msg.includes('Network') ||
    msg.includes('ECONNREFUSED') ||
    msg.includes('ERR_NETWORK') ||
    (error?.request && !error?.response)
  ElMessage.error(
    net
      ? '无法连接后端，请确认 Spring 服务已启动（默认 http://localhost:8080/api）'
      : `加载失败：${msg}`
  )
  console.error(error)
}

/** 表结构 + 分页数据一次拉取，失败只弹一条提示（避免刷屏） */
const loadTable = async () => {
  loading.value = true
  try {
    await loadTableStructure()
    await loadTableData()
  } catch (error) {
    showLoadError(error)
  } finally {
    loading.value = false
  }
}

/** 仅刷新当前页数据（翻页、改每页条数） */
const loadDataOnly = async () => {
  loading.value = true
  try {
    await loadTableData()
  } catch (error) {
    showLoadError(error)
  } finally {
    loading.value = false
  }
}

// 格式化值
const formatValue = (value, dataType) => {
  if (value === null || value === undefined) return '-'
  const dt = (dataType || '').toLowerCase()
  // 整数类型（int / bigint / smallint / tinyint）→ 整数显示
  if (/^(bigint|int|smallint|tinyint|mediumint)$/.test(dt) || /^(bigint|int|smallint|tinyint|mediumint)\(/i.test(dt)) {
    return Number.isInteger(Number(value)) ? Number(value) : Number(value).toFixed(0)
  }
  // 小数类型（decimal / float / double / real）→ 保留2位
  if (/^(decimal|float|double|real)/i.test(dt)) {
    return Number(value).toFixed(2)
  }
  // 日期时间 → 格式化
  if (dt.includes('datetime') || dt.includes('timestamp') || dt === 'date') {
    if (!value) return '-'
    const d = new Date(value)
    if (isNaN(d)) return value
    return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' })
  }
  // 其余（varchar / text / enum 等）→ 原样
  return value
}

// 获取列宽
const getColumnWidth = (col) => {
  const dt = (col.dataType || '').toLowerCase()
  if (col.columnName.includes('id')) return 100
  if (dt.includes('datetime') || dt.includes('timestamp')) return 180
  if (dt.includes('decimal') || dt.includes('double') || dt.includes('float')) return 150
  if (dt.includes('int') || dt.includes('bigint')) return 110
  if (dt.includes('text') || dt.includes('blob')) return 220
  return undefined
}

const handleSizeChange = () => {
  currentPage.value = 1
  loadDataOnly()
}

const handleCurrentChange = () => {
  loadDataOnly()
}

const handleDelete = async (row) => {
  if (!canDeleteRows.value || deleting.value) return
  try {
    await ElMessageBox.confirm('确认删除该条数据？此操作不可撤销。', '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  deleting.value = true
  try {
    await deleteTableRow(props.tableName, row)
    ElMessage.success('删除成功')
    await loadDataOnly()
    if (tableData.value.length === 0 && currentPage.value > 1) {
      currentPage.value -= 1
      await loadDataOnly()
    }
  } catch (error) {
    const msg = error?.message || '删除失败'
    ElMessage.error(msg)
  } finally {
    deleting.value = false
  }
}

// 监听表名变化（含首次挂载，勿再写 onMounted，否则会重复请求）
watch(
  () => props.tableName,
  () => {
    currentPage.value = 1
    loadTable()
  },
  { immediate: true }
)

watch(
  () => props.refreshTick,
  () => {
    if (props.refreshTick > 0) {
      currentPage.value = 1
      loadTable()
    }
  }
)

/** 与 DataView 顶部「作战 ID」全局筛选联动 */
watch(
  globalOperationId,
  () => {
    if (!showOperationFilter.value) return
    currentPage.value = 1
    loadDataOnly()
  }
)
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
