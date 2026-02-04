import request from '@/utils/request'

// 获取允许查询的表列表
export function getAllowedTables() {
  return request({
    url: '/table/allowed',
    method: 'get'
  })
}

// 获取表结构
export function getTableStructure(tableName) {
  return request({
    url: `/table/structure/${tableName}`,
    method: 'get'
  })
}

// 获取表数据
export function getTableData(tableName, page = 1, size = 20) {
  return request({
    url: `/table/data/${tableName}`,
    method: 'get',
    params: { page, size }
  })
}

// 计算AHP权重
export function calculateAHP(priorities) {
  return request({
    url: '/evaluation/ahp/calculate',
    method: 'post',
    data: { priorities }
  })
}

// 计算综合评估
export function calculateComprehensive(ahpWeights) {
  return request({
    url: '/evaluation/comprehensive/calculate',
    method: 'post',
    data: { ahpWeights }
  })
}
