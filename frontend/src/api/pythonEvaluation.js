import request from '@/utils/request'

/**
 * Python 评估服务 API
 */

/**
 * 执行评估计算（调用 Python 脚本）
 * @param {Object} priorities - 维度优先级配置，例如 { RL: 1, SC: 2, AJ: 3, EF: 4, PO: 5, NC: 6, HO: 7, RS: 8 }
 * @returns {Promise}
 */
export function calculatePythonEvaluation(priorities) {
  return request({
    url: '/python-evaluation/calculate',
    method: 'post',
    data: {
      priorities
    }
  })
}

/**
 * 测试 Python 环境
 * @returns {Promise}
 */
export function testPythonEnvironment() {
  return request({
    url: '/python-evaluation/test-environment',
    method: 'get'
  })
}
