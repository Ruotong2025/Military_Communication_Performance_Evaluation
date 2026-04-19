import request from "@/utils/request";

// 获取允许查询的表列表
export function getAllowedTables() {
  return request({
    url: "/table/allowed",
    method: "get",
  });
}

// 获取表结构
export function getTableStructure(tableName) {
  return request({
    url: `/table/structure/${tableName}`,
    method: "get",
  });
}

// 删除表中的一行（按主键）
export function deleteTableRow(tableName, row) {
  return request({
    url: `/table/data/${tableName}`,
    method: "delete",
    data: row,
  });
}

// 计算AHP权重
export function calculateAHP(priorities) {
  return request({
    url: "/evaluation/ahp/calculate",
    method: "post",
    data: { priorities },
  });
}

// 计算综合评估
export function calculateComprehensive(ahpWeights) {
  return request({
    url: "/evaluation/comprehensive/calculate",
    method: "post",
    data: { ahpWeights },
  });
}

/** 生成作战模拟四表数据（records_*） */
export function generateCombatSimulation(payload) {
  return request({
    url: "/combat/simulation/generate",
    method: "post",
    data: payload,
  });
}

/** 获取可选的作战ID列表 */
export function getAvailableOperations() {
  return request({
    url: "/metrics/operations",
    method: "get",
  });
}

/** 计算指标数据 */
export function calculateMetrics(payload) {
  return request({
    url: "/metrics/calculate",
    method: "post",
    data: payload,
  });
}

/** 获取评估批次列表 */
export function getMetricBatches() {
  return request({
    url: "/metrics/batches",
    method: "get",
  });
}

/** 按评估批次获取已计算的指标结果 */
export function getCalculatedMetrics(evaluationId) {
  return request({
    url: "/metrics/results",
    method: "get",
    params: evaluationId ? { evaluationId } : {},
  });
}

/** 删除指定评估批次下的全部指标 */
export function deleteMetricBatch(evaluationId) {
  return request({
    url: "/metrics/batches",
    method: "delete",
    params: { evaluationId },
  });
}

/** 获取归一化 score 数据 */
export function getScoreData(evaluationId) {
  return request({
    url: "/metrics/scores",
    method: "get",
    params: evaluationId ? { evaluationId } : {},
  });
}

/** 获取 ECharts 图表数据（多实验对比） */
export function getScoreChartData(evaluationId) {
  return request({
    url: `/metrics/score-chart/${evaluationId}`,
    method: "get",
  });
}

/** 计算归一化 score 数据 */
export function generateScore(evaluationId) {
  return request({
    url: "/metrics/generate-score",
    method: "post",
    params: { evaluationId },
  });
}

/** 获取专家AHP矩阵结构定义 */
export function getExpertAHPMeta() {
  return request({
    url: "/evaluation/expert-ahp/meta",
    method: "get",
  });
}

/** 计算专家AHP矩阵（传入上三角比较对，自动生成完整矩阵） */
export function calculateExpertAHP(payload) {
  return request({
    url: "/evaluation/expert-ahp/calculate",
    method: "post",
    data: payload,
  });
}

/** 按专家查询已保存的 AHP 比较打分 */
export function getExpertAhpScores(expertId) {
  return request({
    url: "/evaluation/expert-ahp/scores",
    method: "get",
    params: { expertId },
  });
}

/** 按专家查询已保存的 AHP 层次权重快照 */
export function getExpertAhpIndividualWeights(expertId) {
  return request({
    url: "/evaluation/expert-ahp/individual-weights",
    method: "get",
    params: { expertId },
  });
}

/** 查询统一 AHP 快照（域间 + 效能 + 装备，含 allLeaves 叶子全局权重） */
export function getExpertUnifiedWeights(expertId) {
  return request({
    url: "/evaluation/expert-ahp/unified-weights",
    method: "get",
    params: { expertId },
  });
}

/** 按库中打分重算并写入 expert_ahp_individual_weights，再用于 getExpertUnifiedWeights */
export function recalculateExpertUnifiedWeights(expertId) {
  return request({
    url: "/evaluation/expert-ahp/unified-weights/recalculate",
    method: "post",
    params: { expertId },
  });
}

/** 保存当前矩阵为该专家的 AHP 比较打分（覆盖写入） */
export function saveExpertAhpScores(payload) {
  return request({
    url: "/evaluation/expert-ahp/scores",
    method: "post",
    data: payload,
  });
}

/** 保存「效能指标 vs 装备操作」一级域间比较（单对） */
export function saveCrossDomainExpertAhpScore(payload) {
  return request({
    url: "/evaluation/expert-ahp/cross-domain-score",
    method: "post",
    data: payload,
  });
}

/** 为多名专家批量生成模拟 AHP 打分并入库 */
export function simulateExpertAhpScores(payload) {
  return request({
    url: "/evaluation/expert-ahp/scores/simulate",
    method: "post",
    data: payload,
    timeout: 300000,
  });
}

// ============================================================
// 专家可信度评估 API
// ============================================================

/** 获取所有专家列表 */
export function getExpertList() {
  return request({
    url: "/expert/credibility/experts",
    method: "get",
  });
}

/** 获取专家详情 */
export function getExpertById(id) {
  return request({
    url: `/expert/credibility/experts/${id}`,
    method: "get",
  });
}

/** 添加专家 */
export function addExpert(expert) {
  return request({
    url: "/expert/credibility/experts",
    method: "post",
    data: expert,
  });
}

/** 更新专家信息 */
export function updateExpert(id, expert) {
  return request({
    url: `/expert/credibility/experts/${id}`,
    method: "put",
    data: expert,
  });
}

/** 删除专家 */
export function deleteExpert(id) {
  return request({
    url: `/expert/credibility/experts/${id}`,
    method: "delete",
  });
}

/** 评估单个专家 */
export function evaluateExpert(expertId) {
  return request({
    url: `/expert/credibility/evaluate/${expertId}`,
    method: "post",
  });
}

/** 批量评估所有专家 */
export function evaluateAllExperts() {
  return request({
    url: "/expert/credibility/evaluate/all",
    method: "post",
  });
}

/** 批量评估指定专家 */
export function evaluateBatch(expertIds, overwrite = true) {
  return request({
    url: "/expert/credibility/evaluate/batch",
    method: "post",
    data: { expertIds, overwrite },
  });
}

/** 获取所有评估得分 */
export function getAllScores() {
  return request({
    url: "/expert/credibility/scores",
    method: "get",
  });
}

/** 获取专家评估得分 */
export function getScoreByExpertId(expertId) {
  return request({
    url: `/expert/credibility/scores/${expertId}`,
    method: "get",
  });
}

/** 按等级筛选评估结果 */
export function getScoresByLevel(level) {
  return request({
    url: `/expert/credibility/scores/level/${level}`,
    method: "get",
  });
}

/** 获取评估详情（包含10个维度得分） */
export function getEvaluationDetails(expertId) {
  return request({
    url: `/expert/credibility/details/${expertId}`,
    method: "get",
  });
}

/** 获取专家列表（含评估状态） */
export function getExpertsWithStatus() {
  return request({
    url: "/expert/credibility/experts/with-status",
    method: "get",
  });
}

/** 获取评估统计信息 */
export function getStatistics() {
  return request({
    url: "/expert/credibility/statistics",
    method: "get",
  });
}

/** 全局调整十维权重并重算所有已评估记录的综合分 */
export function applyGlobalWeights(payload) {
  return request({
    url: "/expert/credibility/weights/global",
    method: "post",
    data: payload,
  });
}

/** 生成专家模拟数据 */
export function generateMockExperts(payload) {
  return request({
    url: "/expert/mock/generate",
    method: "post",
    data: payload,
  });
}

/** 生成并评估专家数据 */
export function generateAndEvaluate(payload) {
  return request({
    url: "/expert/mock/generate-and-evaluate",
    method: "post",
    data: payload,
  });
}

// ============================================================
// 专家集结计算 API（对比打分层集结）
// ============================================================

/** 获取所有评估批次ID */
export function getCollectiveEvaluationIds() {
  return request({
    url: "/evaluation/collective/evaluation-ids",
    method: "get",
  });
}

/** 预览集结权重（不保存） */
export function previewCollectiveWeights(params) {
  return request({
    url: "/evaluation/collective/weights-preview",
    method: "get",
    params,
  });
}

/** 执行集结计算并保存 */
export function executeCollectiveCalculation(payload) {
  return request({
    url: "/evaluation/collective/calculate",
    method: "post",
    data: payload,
    timeout: 60000,
  });
}

/** 查询已保存的集结综合结果（只读，不重新计算） */
export function getCollectiveResults(evaluationId) {
  return request({
    url: "/evaluation/collective/results",
    method: "get",
    params: { evaluationId },
  });
}

/** 计算并保存综合结果：集结二级权重 × 该批次归一化 score（「加载综合结果」按钮） */
export function computeCollectiveResults(evaluationId) {
  return request({
    url: "/evaluation/collective/results/compute",
    method: "post",
    params: { evaluationId },
    timeout: 60000,
  });
}

/** 删除指定批次的集结结果 */
export function deleteCollectiveResults(evaluationId) {
  return request({
    url: `/evaluation/collective/results/${evaluationId}`,
    method: "delete",
  });
}

/** 根据批次ID获取作战任务ID列表（从score表直接查询，无需预存加权结果） */
export function getOperationIdsByEvaluationId(evaluationId) {
  return request({
    url: "/evaluation/collective/operation-ids",
    method: "get",
    params: { evaluationId },
  });
}

/** 作战基础信息表中不重复的 operation_id 列表 */
export function getDistinctOperationIds() {
  return request({
    url: "/table/distinct-operation-ids",
    method: "get",
  });
}

/** 分页查询表数据（支持 operationId 筛选） */
export function getTableData(tableName, page = 1, size = 20, operationId = null) {
  const params = { page, size }
  if (operationId) {
    params.operationId = operationId
  }
  return request({
    url: `/table/data/${tableName}`,
    method: "get",
    params,
  });
}

// ============================================================
// 效费分析 API
// ============================================================

/**
 * 获取成本指标配置
 */
export function getCostIndicators() {
  return request({
    url: "/evaluation/cost-effectiveness/indicators",
    method: "get",
  });
}

/**
 * 获取指标类别
 */
export function getCostCategories() {
  return request({
    url: "/evaluation/cost-effectiveness/categories",
    method: "get",
  });
}

/**
 * 按类别获取指标
 */
export function getCostIndicatorsByCategory(category) {
  return request({
    url: "/evaluation/cost-effectiveness/indicators/by-category",
    method: "get",
    params: { category },
  });
}

/**
 * 获取效费分析结果
 */
export function getCostEffectivenessResults(evaluationId) {
  return request({
    url: "/evaluation/cost-effectiveness/results",
    method: "get",
    params: { evaluationId },
  });
}

/**
 * 执行效费分析计算
 */
export function calculateCostEffectiveness(requestData) {
  return request({
    url: "/evaluation/cost-effectiveness/calculate",
    method: "post",
    data: requestData,
  });
}

/**
 * 删除效费分析结果
 */
export function deleteCostEffectivenessResults(evaluationId) {
  return request({
    url: "/evaluation/cost-effectiveness/results",
    method: "delete",
    params: { evaluationId },
  });
}

/**
 * 获取作战任务原始成本数据预览（选择批次后自动加载）
 */
export function getCostRawDataPreview(evaluationId, operationIds) {
  return request({
    url: "/evaluation/cost-effectiveness/raw-data-preview",
    method: "get",
    params: { evaluationId, operationIds },
    paramsSerializer: {
      serialize: (params) => {
        const parts = []
        if (params.evaluationId != null) {
          parts.push(`evaluationId=${encodeURIComponent(params.evaluationId)}`)
        }
        if (params.operationIds && Array.isArray(params.operationIds)) {
          params.operationIds.forEach(id => {
            parts.push(`operationIds=${encodeURIComponent(id)}`)
          })
        }
        return parts.join('&')
      }
    }
  });
}

/**
 * 保存指标权重配置
 */
export function saveCostIndicatorWeights(weights) {
  return request({
    url: "/evaluation/cost-effectiveness/weights/save",
    method: "post",
    data: weights,
  });
}

/**
 * 重置为两层等权权重
 */
export function resetCostWeights() {
  return request({
    url: "/evaluation/cost-effectiveness/weights/reset",
    method: "post",
  });
}

// ============================================================
// 惩罚模型计算 API（原有）
// ============================================================

/**
 * 保存惩罚计算结果
 * @param {string} evaluationId - 评估批次ID
 * @param {Array} results - 惩罚计算结果列表
 */
export function savePenaltyResults(evaluationId, results) {
  return request({
    url: "/evaluation/penalty/results",
    method: "post",
    params: { evaluationId },
    data: results,
  });
}

/**
 * 查询惩罚计算结果
 * @param {string} evaluationId - 评估批次ID
 */
export function getPenaltyResults(evaluationId) {
  return request({
    url: "/evaluation/penalty/results",
    method: "get",
    params: { evaluationId },
  });
}

/**
 * 检查惩罚结果是否存在
 * @param {string} evaluationId - 评估批次ID
 */
export function hasPenaltyResults(evaluationId) {
  return request({
    url: "/evaluation/penalty/results/exists",
    method: "get",
    params: { evaluationId },
  });
}

/**
 * 删除惩罚计算结果
 * @param {string} evaluationId - 评估批次ID
 */
export function deletePenaltyResults(evaluationId) {
  return request({
    url: "/evaluation/penalty/results",
    method: "delete",
    params: { evaluationId },
  });
}

// ============================================================
// 装备操作评估 API（定量指标）
// ============================================================

/**
 * 获取定量指标配置列表
 */
export function getQtIndicators() {
  return request({
    url: "/equipment/qt/indicators",
    method: "get",
  });
}

/**
 * 计算定量指标
 */
export function calculateQtMetrics(payload) {
  return request({
    url: "/equipment/qt/calculate",
    method: "post",
    data: payload,
  });
}

/**
 * 生成归一化得分
 */
export function normalizeQtScores(evaluationBatchId) {
  return request({
    url: "/equipment/qt/normalize",
    method: "post",
    params: { evaluationBatchId },
  });
}

/**
 * 获取定量评估批次列表
 */
export function getQtBatches() {
  return request({
    url: "/equipment/qt/batches",
    method: "get",
  });
}

/**
 * 获取批次下的定量评估记录
 */
export function getQtRecords(evaluationBatchId) {
  return request({
    url: "/equipment/qt/records",
    method: "get",
    params: { evaluationBatchId },
  });
}

/**
 * 删除定量评估批次
 */
export function deleteQtBatch(evaluationBatchId) {
  return request({
    url: "/equipment/qt/batches",
    method: "delete",
    params: { evaluationBatchId },
  });
}

// ============================================================
// 装备操作评估 API（定性指标）
// ============================================================

/**
 * 获取定性指标配置列表
 */
export function getQlIndicators() {
  return request({
    url: "/equipment/ql/indicators",
    method: "get",
  });
}

/**
 * 获取指定指标的参考数据
 */
export function getQlReferenceData(operationId, indicatorKey) {
  return request({
    url: `/equipment/ql/reference/${operationId}/${indicatorKey}`,
    method: "get",
  });
}

/**
 * 专家提交定性评分
 */
export function submitQlEvaluation(payload) {
  return request({
    url: "/equipment/ql/submit",
    method: "post",
    data: payload,
  });
}

/**
 * 批量模拟定性评分（覆盖当前批次·作战下各专家已有记录）
 */
export function simulateQlBatch(payload) {
  return request({
    url: "/equipment/ql/simulate-batch",
    method: "post",
    data: payload,
    timeout: 120000,
  });
}

/**
 * 获取定性评估批次列表
 */
export function getQlBatches() {
  return request({
    url: "/equipment/ql/batches",
    method: "get",
  });
}

/**
 * 获取某评估批次下可选作战（与指标计算批次一致；不传批次则返回全部作战）
 */
export function getQlOperationsForBatch(evaluationBatchId) {
  return request({
    url: "/equipment/ql/operations-for-batch",
    method: "get",
    params: evaluationBatchId ? { evaluationBatchId } : {},
  });
}

/**
 * 获取批次下的定性评估记录
 */
export function getQlRecords(evaluationBatchId, operationId) {
  const params = { evaluationBatchId };
  if (operationId) params.operationId = operationId;
  return request({
    url: "/equipment/ql/records",
    method: "get",
    params,
  });
}

/**
 * 删除定性评估批次
 */
export function deleteQlBatch(evaluationBatchId) {
  return request({
    url: "/equipment/ql/batches",
    method: "delete",
    params: { evaluationBatchId },
  });
}

/**
 * 获取专家列表（用于定性评估选择）- 含可信度评分
 */
export function getExpertsForEvaluation() {
  return request({
    url: "/equipment/ql/experts",
    method: "get",
  });
}

/**
 * 获取指定专家在指定批次的定性评估记录（用于二次打开回显）
 */
export function getQlRecordForEdit(evaluationBatchId, operationId, expertId) {
  return request({
    url: "/equipment/ql/record-for-edit",
    method: "get",
    params: { evaluationBatchId, operationId, expertId },
  });
}

/**
 * 专家定性指标集结（γ = w_α·α/100 + w_λ·λ，λ 为把握度/100；质心式 4-21）。
 * 请求参数：
 *   evaluationBatchId  必填
 *   operationId       必填（支持 "ALL" 批量）
 *   wAlpha            可选，默认 0.5（权威度权重）
 *   wLambda           可选，默认 0.5（把握度权重）
 *   saveResult        可选，默认 false（是否持久化到 equipment_ql_aggregation_result 表）
 */
export function getQlQualitativeAggregation(params) {
  return request({
    url: "/equipment/ql/qualitative-aggregation",
    method: "post",
    data: params,
  });
}

/**
 * 查询已存储的集结结果
 */
export function getStoredQlAggregationResult(evaluationBatchId, operationId) {
  return request({
    url: "/equipment/ql/aggregation-result",
    method: "get",
    params: { evaluationBatchId, operationId },
  });
}

// ==================== 综合评分 API ====================

/**
 * 获取评估批次列表
 */
export function getComprehensiveBatches() {
  return request({
    url: "/equipment/comprehensive/batches",
    method: "get",
  });
}

/**
 * 执行综合评分计算
 */
export function calculateComprehensiveScores(evaluationBatchId) {
  return request({
    url: "/equipment/comprehensive/calculate",
    method: "post",
    params: { evaluationBatchId },
  });
}

/**
 * 获取已保存的综合评分结果
 */
export function getComprehensiveResults(evaluationBatchId) {
  return request({
    url: "/equipment/comprehensive/results",
    method: "get",
    params: { evaluationBatchId },
  });
}

/**
 * 删除综合评分结果
 */
export function deleteComprehensiveResults(evaluationBatchId) {
  return request({
    url: "/equipment/comprehensive/results",
    method: "delete",
    params: { evaluationBatchId },
  });
}

/**
 * 获取效能指标原始数据
 */
export function getMetricsRaw(evaluationBatchId) {
  return request({
    url: "/equipment/comprehensive/metrics-raw",
    method: "get",
    params: { evaluationBatchId },
  });
}

/**
 * 获取效能指标归一化得分
 */
export function getMetricsScore(evaluationBatchId) {
  return request({
    url: "/equipment/comprehensive/metrics-score",
    method: "get",
    params: { evaluationBatchId },
  });
}

/**
 * 获取装备操作原始数据
 */
export function getEquipmentRaw(evaluationBatchId) {
  return request({
    url: "/equipment/comprehensive/equipment-raw",
    method: "get",
    params: { evaluationBatchId },
  });
}

/**
 * 获取装备操作归一化得分
 */
export function getEquipmentScore(evaluationBatchId) {
  return request({
    url: "/equipment/comprehensive/equipment-score",
    method: "get",
    params: { evaluationBatchId },
  });
}

/**
 * 获取批次信息（调试用）
 */
export function getBatchInfo() {
  return request({
    url: "/equipment/comprehensive/batch-info",
    method: "get",
  });
}

/**
 * 获取批次下的作战ID列表
 */
export function getOperationsByBatch(evaluationBatchId) {
  return request({
    url: "/equipment/comprehensive/operations",
    method: "get",
    params: { evaluationBatchId },
  });
}

/**
 * 获取指定作战的完整评估数据
 */
export function getOperationData(evaluationBatchId, operationId) {
  return request({
    url: "/equipment/comprehensive/operation-data",
    method: "get",
    params: { evaluationBatchId, operationId },
  });
}
