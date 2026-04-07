import { ref } from 'vue'
import { getDistinctOperationIds } from '@/api'

/**
 * 模拟表共用的「作战 ID」筛选：选一次后切换 Tab 无需重选。
 * 与 DataView 顶部下拉、DynamicTable 请求参数、MetricsCalculation 作战多选保持同步。
 */
export const globalOperationId = ref(null)
export const globalOperationIds = ref([])

export async function loadGlobalOperationIds() {
  try {
    globalOperationIds.value = await getDistinctOperationIds()
  } catch {
    globalOperationIds.value = []
  }
}
