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

// ============================================================
// 动态指标系统 API
// ============================================================

/**
 * 解析Excel指标文件（不保存到数据库）
 */
export function parseExcelIndicators(file) {
  const formData = new FormData();
  formData.append('file', file);
  return request({
    url: "/dynamic-indicator/parse",
    method: "post",
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
}

/**
 * 导入指标到数据库
 */
export function importIndicators(file, templateName) {
  const formData = new FormData();
  formData.append('file', file);
  if (templateName) {
    formData.append('templateName', templateName);
  }
  return request({
    url: "/dynamic-indicator/import",
    method: "post",
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
}

/**
 * 获取指标模板列表
 */
export function getIndicatorTemplates() {
  return request({
    url: "/dynamic-indicator/templates",
    method: "get",
  });
}

/**
 * 获取指标树结构
 */
export function getIndicatorTree(templateId) {
  return request({
    url: "/dynamic-indicator/tree",
    method: "get",
    params: { templateId },
  });
}

/**
 * 获取指定层级的一级维度列表
 */
export function getPrimaryDimensions(levelId) {
  return request({
    url: "/dynamic-indicator/level/primaries",
    method: "get",
    params: { levelId },
  });
}

/**
 * 获取指定一级维度的二级维度列表
 */
export function getSecondaryDimensions(primaryId) {
  return request({
    url: "/dynamic-indicator/primary/secondaries",
    method: "get",
    params: { primaryId },
  });
}

/**
 * 获取AHP判断矩阵数据
 */
export function getAhpMatrix(levelId) {
  return request({
    url: "/dynamic-indicator/ahp/matrix",
    method: "get",
    params: { levelId },
  });
}

/**
 * 计算AHP权重
 */
export function calculateAhpWeights(levelId, matrix) {
  return request({
    url: "/dynamic-indicator/ahp/calculate",
    method: "post",
    data: { levelId, matrix },
  });
}

/**
 * 保存AHP权重
 */
export function saveAhpWeights(levelId, weights) {
  return request({
    url: "/dynamic-indicator/ahp/save",
    method: "post",
    data: { levelId, weights },
  });
}

/**
 * 更新二级维度配置
 */
export function updateSecondaryConfig(id, config) {
  return request({
    url: `/dynamic-indicator/secondary/${id}`,
    method: "put",
    data: config,
  });
}

/**
 * 删除指标模板
 */
export function deleteTemplate(templateId) {
  return request({
    url: `/dynamic-indicator/template/${templateId}`,
    method: "delete",
  });
}

/**
 * 通过矩阵计算AHP权重（直接传入矩阵，前端计算用）
 */
export function calculateAhpByMatrix(matrix) {
  return request({
    url: "/python/ahp/calculate",
    method: "post",
    data: { matrix },
  });
}

// ============================================
// 动态定量评估 API
// ============================================

/**
 * 获取动态评估批次列表
 */
export function getDynamicQtBatches() {
  return request({
    url: "/dynamic-qt/batches",
    method: "get",
  });
}

/**
 * 根据模板获取批次列表
 */
export function getDynamicQtBatchesByTemplate(templateId) {
  return request({
    url: "/dynamic-qt/batches/by-template",
    method: "get",
    params: { templateId },
  });
}

/**
 * 获取批次信息
 */
export function getDynamicQtBatchInfo(batchId) {
  return request({
    url: `/dynamic-qt/batch/info/${batchId}`,
    method: "get",
  });
}

/**
 * 创建新批次（只创建批次，不创建记录）
 */
export function createDynamicQtBatch(templateId, description = '') {
  return request({
    url: "/dynamic-qt/batch",
    method: "post",
    data: { templateId, description },
  });
}

/**
 * 获取批次的定量指标列表（按一级维度分组）
 */
export function getDynamicQtIndicators(batchId, templateId) {
  return request({
    url: "/dynamic-qt/indicators",
    method: "get",
    params: { batchId, templateId },
  });
}

/**
 * 获取评估记录（转置表格：行=作战ID，列=指标）
 */
export function getDynamicQtRecords(batchId) {
  return request({
    url: "/dynamic-qt/records",
    method: "get",
    params: { batchId },
  });
}

/**
 * 模拟单个单元格
 */
export function simulateDynamicQtCell(batchId, operationId, secondaryCode) {
  return request({
    url: "/dynamic-qt/simulate",
    method: "post",
    data: { batchId, operationId, secondaryCode },
  });
}

/**
 * 全局模拟：根据指定次数生成作战数据
 * @param {string} batchId - 批次ID
 * @param {number} count - 生成数量
 * @param {number} templateId - 模板ID
 * @param {string} templateName - 模板名称
 * @param {string} mode - 模式: COVER-覆盖当前批次, APPEND-追加到当前批次
 * @param {number} dispersion - 全局离散度（0-1），仅在 dispersions 为空时使用
 * @param {object} dispersions - 各指标独立离散度，格式: {指标code: 离散度}
 */
export function globalSimulateDynamicQt(batchId, count, templateId, templateName, mode = 'APPEND', dispersion = 0.2, dispersions = null) {
  const data = { batchId, count, templateId, templateName, mode, dispersion };
  if (dispersions && Object.keys(dispersions).length > 0) {
    data.dispersions = dispersions;
  }
  return request({
    url: "/dynamic-qt/global-simulate",
    method: "post",
    data: data,
  });
}

/**
 * 获取定量指标统计信息（用于模拟配置）
 * @param {number} templateId - 模板ID
 */
export function getQtIndicatorStats(templateId) {
  return request({
    url: "/dynamic-qt/indicator-stats",
    method: "get",
    params: { templateId },
  });
}

/**
 * 批量模拟
 */
export function batchSimulateDynamicQt(batchId, cells) {
  return request({
    url: "/dynamic-qt/batch-simulate",
    method: "post",
    data: { batchId, cells },
  });
}

/**
 * 保存单条记录
 */
export function saveDynamicQtRecord(batchId, operationId, secondaryCode, value, level = null) {
  return request({
    url: "/dynamic-qt/record",
    method: "post",
    data: { batchId, operationId, secondaryCode, value, level },
  });
}

/**
 * 批量保存记录
 */
export function saveDynamicQtRecords(records) {
  return request({
    url: "/dynamic-qt/records",
    method: "post",
    data: { records },
  });
}

/**
 * 生成归一化得分
 */
export function normalizeDynamicQt(batchId) {
  return request({
    url: "/dynamic-qt/normalize",
    method: "post",
    data: { batchId },
  });
}

/**
 * 获取归一化后的记录
 */
export function getNormalizedDynamicQtRecords(batchId) {
  return request({
    url: "/dynamic-qt/normalized-records",
    method: "get",
    params: { batchId },
  });
}

/**
 * 删除批次
 */
export function deleteDynamicQtBatch(batchId) {
  return request({
    url: `/dynamic-qt/batch/${batchId}`,
    method: "delete",
  });
}

/**
 * 获取可用的作战ID列表
 */
export function getDynamicQtOperations() {
  return request({
    url: "/dynamic-qt/operations",
    method: "get",
  });
}

/**
 * 获取批次已有的作战ID列表
 */
export function getDynamicQtBatchOperations(batchId) {
  return request({
    url: "/dynamic-qt/batch/operations",
    method: "get",
    params: { batchId },
  });
}

/**
 * 为批次添加新的作战ID
 */
export function addDynamicQtOperations(batchId, operationIds) {
  return request({
    url: "/dynamic-qt/batch/operations",
    method: "post",
    data: { batchId, operationIds },
  });
}

// ============================================
// 动态定量评估 - 归一化管理 API
// ============================================

/**
 * 获取某批次的所有归一化批次列表
 */
export function getNormalizationBatches(batchId) {
  return request({
    url: "/dynamic-qt/normalization-batches",
    method: "get",
    params: { batchId },
  });
}

/**
 * 创建归一化
 * @param {string} batchId - 评估批次ID
 * @param {string} normalizationName - 归一化名称（如"第1次归一化"），不传则自动生成
 * @param {string} description - 描述
 */
export function createNormalization(batchId, normalizationName = null, description = '') {
  return request({
    url: "/dynamic-qt/normalize",
    method: "post",
    data: { batchId, normalizationName, description },
  });
}

/**
 * 删除归一化
 */
export function deleteNormalization(batchId, normalizationName) {
  return request({
    url: "/dynamic-qt/normalization",
    method: "delete",
    params: { batchId, normalizationName },
  });
}

/**
 * 获取归一化结果数据
 * @param {string} batchId - 评估批次ID
 * @param {string} normalizationName - 归一化名称，不传则获取最新的
 */
export function getNormalizationRecords(batchId, normalizationName = null) {
  return request({
    url: "/dynamic-qt/normalization-records",
    method: "get",
    params: { batchId, normalizationName },
  });
}

// ============================================
// 动态定性评估 API
// ============================================

/**
 * 获取定性指标列表（按层级分组）
 * 选择模板后即可调用
 */
export function getDynamicQlIndicators(templateId) {
  return request({
    url: "/dynamic-ql/indicators",
    method: "get",
    params: { templateId },
  });
}

/**
 * 获取所有层级名称
 */
export function getDynamicQlLevels(templateId) {
  return request({
    url: "/dynamic-ql/levels",
    method: "get",
    params: { templateId },
  });
}

/**
 * 获取可用的定量评估批次列表
 */
export function getDynamicQlBatches(templateId = null) {
  return request({
    url: "/dynamic-ql/batches",
    method: "get",
    params: templateId ? { templateId } : {},
  });
}

/**
 * 获取批次下的作战列表
 */
export function getDynamicQlOperations(batchId) {
  return request({
    url: "/dynamic-ql/operations",
    method: "get",
    params: { batchId },
  });
}

/**
 * 获取作战的定量参考数据
 */
export function getDynamicQlReference(batchId, operationId) {
  return request({
    url: "/dynamic-ql/reference",
    method: "get",
    params: { batchId, operationId },
  });
}

/**
 * 获取可选专家列表
 */
export function getDynamicQlExperts() {
  return request({
    url: "/dynamic-ql/experts",
    method: "get",
  });
}

/**
 * 批量获取专家可信度
 */
export function getDynamicQlExpertsCredibility(expertIds) {
  return request({
    url: "/dynamic-ql/experts/credibility",
    method: "get",
    params: { expertIds },
  });
}

/**
 * 获取表格数据（完整结构，用于前端渲染）
 */
export function getDynamicQlTableData(batchId, templateId, levelName = null, expertIds = null) {
  const params = { templateId };
  if (batchId) params.batchId = batchId;
  if (levelName) params.levelName = levelName;
  if (expertIds && expertIds.length > 0) params.expertIds = expertIds;
  return request({
    url: "/dynamic-ql/table-data",
    method: "get",
    params,
  });
}

/**
 * 获取已保存的评估记录
 */
export function getDynamicQlRecords(batchId, expertId = null, operationId = null) {
  const params = {};
  if (batchId) params.batchId = batchId;
  if (expertId) params.expertId = expertId;
  if (operationId) params.operationId = operationId;
  return request({
    url: "/dynamic-ql/records",
    method: "get",
    params,
  });
}

/**
 * 保存评估记录
 */
export function saveDynamicQlRecord(batchId, templateId, expertId, operationId, scores) {
  return request({
    url: "/dynamic-ql/records",
    method: "post",
    data: { batchId, templateId, expertId, operationId, scores },
  });
}

/**
 * 批量保存评估记录
 */
export function saveDynamicQlRecordsBatch(records) {
  return request({
    url: "/dynamic-ql/records/batch",
    method: "post",
    data: records,
  });
}

/**
 * 执行集结计算
 * @param {string} batchId - 批次ID
 * @param {string} operationId - 作战ID（可选）
 * @param {string} levelName - 层级名称（可选）
 * @param {number} weightAlpha - 权威度权重（0-1）
 * @param {number} weightLambda - 把握度权重（0-1）
 */
export function aggregateDynamicQlScores(batchId, operationId = null, levelName = null, weightAlpha = 0.5, weightLambda = 0.5) {
  return request({
    url: "/dynamic-ql/aggregate",
    method: "post",
    data: { batchId, operationId, levelName, weightAlpha, weightLambda },
  });
}

/**
 * 获取集结结果
 */
export function getDynamicQlAggregation(batchId, operationId = null) {
  return request({
    url: "/dynamic-ql/aggregation",
    method: "get",
    params: { batchId, operationId },
  });
}

/**
 * 创建评估会话
 */
export function createDynamicQlSession(templateId, batchId = null, expertIds = null) {
  return request({
    url: "/dynamic-ql/session",
    method: "post",
    data: { templateId, batchId, expertIds },
  });
}

/**
 * 获取会话详情
 */
export function getDynamicQlSession(sessionId) {
  return request({
    url: `/dynamic-ql/session/${sessionId}`,
    method: "get",
  });
}

// ============================================
// 动态AHP权重配置 API
// ============================================

/**
 * 获取动态指标模板列表
 */
export function getDynamicAhpTemplates() {
  return request({
    url: "/dynamic-ahp/templates",
    method: "get",
  });
}

/**
 * 获取指标树结构
 */
export function getDynamicAhpTree(templateId) {
  return request({
    url: "/dynamic-ahp/tree",
    method: "get",
    params: { templateId },
  });
}

/**
 * 获取专家列表
 */
export function getDynamicAhpExperts() {
  return request({
    url: "/dynamic-ahp/experts",
    method: "get",
  });
}

/**
 * 获取层级列表
 */
export function getDynamicAhpLevels(templateId) {
  return request({
    url: "/dynamic-ahp/levels",
    method: "get",
    params: { templateId },
  });
}

/**
 * 获取一级维度列表
 */
export function getDynamicAhpPrimaries(templateId, levelName) {
  return request({
    url: "/dynamic-ahp/primaries",
    method: "get",
    params: { templateId, levelName },
  });
}

/**
 * 获取二级指标列表
 */
export function getDynamicAhpSecondaries(templateId, levelName, primaryCode) {
  return request({
    url: "/dynamic-ahp/secondaries",
    method: "get",
    params: { templateId, levelName, primaryCode },
  });
}

/**
 * 获取层级间比较矩阵
 */
export function getDynamicAhpLevelBetweenMatrix(templateId, expertId) {
  return request({
    url: "/dynamic-ahp/matrix/level-between",
    method: "get",
    params: { templateId, expertId },
  });
}

/**
 * 获取一级维度间比较矩阵
 */
export function getDynamicAhpPrimaryBetweenMatrix(templateId, levelName, expertId) {
  return request({
    url: "/dynamic-ahp/matrix/primary-between",
    method: "get",
    params: { templateId, levelName, expertId },
  });
}

/**
 * 获取二级指标间比较矩阵
 */
export function getDynamicAhpSecondaryBetweenMatrix(templateId, levelName, primaryCode, expertId) {
  return request({
    url: "/dynamic-ahp/matrix/secondary-between",
    method: "get",
    params: { templateId, levelName, primaryCode, expertId },
  });
}

/**
 * 保存AHP矩阵打分
 */
export function saveDynamicAhpMatrix(data) {
  return request({
    url: "/dynamic-ahp/matrix/save",
    method: "post",
    data,
  });
}

/**
 * 计算AHP权重
 */
export function calculateDynamicAhpWeights(templateId, expertId, levelName, matrixType, parentCode = null) {
  const params = { templateId, expertId, levelName, matrixType };
  if (parentCode) {
    params.parentCode = parentCode;
  }
  return request({
    url: "/dynamic-ahp/calculate",
    method: "post",
    params,
  });
}

/**
 * 计算所有AHP权重
 */
export function calculateAllDynamicAhpWeights(templateId, expertId) {
  return request({
    url: "/dynamic-ahp/calculate/all",
    method: "post",
    params: { templateId, expertId },
  });
}

/**
 * 保存权重计算结果
 */
export function saveDynamicAhpWeights(templateId, expertId) {
  return request({
    url: "/dynamic-ahp/weights/save",
    method: "post",
    params: { templateId, expertId },
  });
}

/**
 * 获取已保存的权重
 */
export function getDynamicAhpWeights(templateId, expertId) {
  return request({
    url: "/dynamic-ahp/weights",
    method: "get",
    params: { templateId, expertId },
  });
}

/**
 * 获取指定层级的权重
 */
export function getDynamicAhpWeightsByLevel(templateId, expertId, levelName) {
  return request({
    url: "/dynamic-ahp/weights/by-level",
    method: "get",
    params: { templateId, expertId, levelName },
  });
}

/**
 * 模拟AHP打分
 */
export function simulateDynamicAhpScores(templateId, expertIds) {
  return request({
    url: "/dynamic-ahp/simulate",
    method: "post",
    params: { templateId, expertIds },
    paramsSerializer: {
      serialize: (params) => {
        const parts = [];
        if (params.templateId != null) parts.push(`templateId=${params.templateId}`);
        if (params.expertIds && Array.isArray(params.expertIds)) {
          params.expertIds.forEach(id => parts.push(`expertIds=${id}`));
        }
        return parts.join('&');
      }
    }
  });
}

/**
 * 批量模拟AHP打分（为所有专家）
 */
export function batchSimulateDynamicAhpScores(templateId) {
  return request({
    url: "/dynamic-ahp/batch-simulate",
    method: "post",
    params: { templateId },
  });
}

/**
 * 清除AHP打分
 */
export function clearDynamicAhpMatrix(templateId, expertId) {
  return request({
    url: "/dynamic-ahp/matrix",
    method: "delete",
    params: { templateId, expertId },
  });
}

/**
 * 获取AHP配置状态
 */
export function getDynamicAhpStatus(templateId, expertId) {
  return request({
    url: "/dynamic-ahp/status",
    method: "get",
    params: { templateId, expertId },
  });
}

// ==================== 动态AHP专家集结相关API ====================

/**
 * 获取可用模板列表
 */
export function getDynamicAhpAggregationTemplates() {
  return request({
    url: "/dynamic-ahp/collective/templates",
    method: "get",
  });
}

/**
 * 获取可用专家列表
 */
export function getDynamicAhpAggregationExperts(templateId) {
  return request({
    url: "/dynamic-ahp/collective/experts",
    method: "get",
    params: { templateId },
  });
}

/**
 * 预览集结结果
 */
export function previewDynamicAhpAggregation(data) {
  return request({
    url: "/dynamic-ahp/collective/preview",
    method: "post",
    data,
  });
}

/**
 * 执行集结计算
 */
export function executeDynamicAhpAggregation(data) {
  return request({
    url: "/dynamic-ahp/collective/calculate",
    method: "post",
    data,
  });
}

/**
 * 获取集结结果列表
 */
export function getDynamicAhpAggregationResults(templateId) {
  return request({
    url: "/dynamic-ahp/collective/results",
    method: "get",
    params: { templateId },
  });
}

/**
 * 获取单个集结结果
 */
export function getDynamicAhpAggregationResult(groupId) {
  return request({
    url: `/dynamic-ahp/collective/results/${groupId}`,
    method: "get",
  });
}

/**
 * 删除集结结果
 */
export function deleteDynamicAhpAggregationResult(groupId) {
  return request({
    url: `/dynamic-ahp/collective/results/${groupId}`,
    method: "delete",
  });
}

// ============================================
// 动态综合评分 API
// ============================================

/**
 * 获取动态综合评分批次列表
 */
export function getDynamicComprehensiveBatches(templateId = null) {
  return request({
    url: "/dynamic-comprehensive/batches",
    method: "get",
    params: templateId ? { templateId } : {},
  });
}

/**
 * 获取原始数据集结表
 * 横向：指标（一级维度 > 二级指标）
 * 纵向：作战ID
 */
export function getDynamicComprehensiveRawData(batchId) {
  return request({
    url: "/dynamic-comprehensive/raw-data",
    method: "get",
    params: { batchId },
  });
}

/**
 * 获取综合评分结果
 */
export function getDynamicComprehensiveScores(batchId) {
  return request({
    url: "/dynamic-comprehensive/scores",
    method: "get",
    params: { batchId },
  });
}

/**
 * 获取指标树结构
 */
export function getDynamicComprehensiveIndicatorTree(templateId) {
  return request({
    url: "/dynamic-comprehensive/indicator-tree",
    method: "get",
    params: { templateId },
  });
}

// =====================================================
// 动态成本效益分析 API
// =====================================================

/**
 * 获取可关联的动态指标模板列表
 */
export function getAvailableTemplates() {
  return request({
    url: "/dynamic-cost-effectiveness/templates/available",
    method: "get",
  });
}

/**
 * 获取指定模板的批次列表
 */
export function getCostEffectivenessBatches(templateId) {
  return request({
    url: "/dynamic-cost-effectiveness/batches",
    method: "get",
    params: { templateId },
  });
}

/**
 * 获取指定批次的效能得分
 */
export function getEffectivenessScore(templateId, batchId) {
  return request({
    url: "/dynamic-cost-effectiveness/effectiveness-score",
    method: "get",
    params: { templateId, batchId },
  });
}

/**
 * 创建成本效益配置
 */
export function createCostEffectivenessConfig(data) {
  return request({
    url: "/dynamic-cost-effectiveness/config",
    method: "post",
    data,
  });
}

/**
 * 更新成本效益配置
 */
export function updateCostEffectivenessConfig(id, data) {
  return request({
    url: `/dynamic-cost-effectiveness/config/${id}`,
    method: "put",
    data,
  });
}

/**
 * 获取配置详情
 */
export function getCostEffectivenessConfig(id) {
  return request({
    url: `/dynamic-cost-effectiveness/config/${id}`,
    method: "get",
  });
}

/**
 * 获取配置列表
 */
export function getCostEffectivenessConfigs() {
  return request({
    url: "/dynamic-cost-effectiveness/configs",
    method: "get",
  });
}

/**
 * 删除配置
 */
export function deleteCostEffectivenessConfig(id) {
  return request({
    url: `/dynamic-cost-effectiveness/config/${id}`,
    method: "delete",
  });
}

/**
 * 保存成本指标配置
 */
export function saveCostIndicators(configId, indicators) {
  return request({
    url: "/dynamic-cost-effectiveness/indicators/save",
    method: "post",
    params: { configId },
    data: indicators,
  });
}

/**
 * 获取成本效益指标列表
 */
export function getCostEffectivenessIndicators(configId) {
  return request({
    url: `/dynamic-cost-effectiveness/indicators/${configId}`,
    method: "get",
  });
}

/**
 * 解析成本指标（JSON格式）
 */
export function parseCostIndicators(indicators) {
  return request({
    url: "/dynamic-cost-effectiveness/indicators/parse",
    method: "post",
    data: { indicators },
  });
}

/**
 * 获取Excel模板信息（返回模板结构说明）
 */
export function getCostTemplateInfo() {
  return request({
    url: "/dynamic-cost-effectiveness/template/info",
    method: "get",
  });
}

/**
 * 执行蒙特卡洛模拟
 */
export function monteCarloSimulation(data) {
  return request({
    url: "/dynamic-cost-effectiveness/simulate/monte-carlo",
    method: "post",
    data,
  });
}

/**
 * 获取模拟历史
 */
export function getSimulationHistory(configId) {
  return request({
    url: `/dynamic-cost-effectiveness/simulation/history/${configId}`,
    method: "get",
  });
}

/**
 * 导出模拟结果
 */
export function exportSimulationResults(simulationId) {
  return request({
    url: `/dynamic-cost-effectiveness/export/${simulationId}`,
    method: "get",
    responseType: "blob",
  });
}

/**
 * 下载效能指标模板
 */
export function downloadEffectivenessTemplate() {
  return request({
    url: "/dynamic-cost-effectiveness/template/effectiveness/download",
    method: "get",
    responseType: "blob",
  }).then((res) => {
    const blob = new Blob([res], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `效能指标模板_${new Date().toLocaleDateString()}.xlsx`;
    link.click();
    window.URL.revokeObjectURL(url);
    return res;
  });
}

/**
 * 下载成本效益模板（包含效能得分+成本指标+权重）
 */
export function downloadCostEffectivenessTemplate(params) {
  return request({
    url: "/dynamic-cost-effectiveness/template/cost/download",
    method: "post",
    data: params,
    responseType: "blob",
  }).then((res) => {
    const blob = new Blob([res], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `成本效益模板_${new Date().toLocaleDateString()}.xlsx`;
    link.click();
    window.URL.revokeObjectURL(url);
    return res;
  });
}

/**
 * 上传效能指标
 */
export function uploadEffectivenessIndicators(formData) {
  return request({
    url: "/dynamic-cost-effectiveness/effectiveness/upload",
    method: "post",
    data: formData,
    headers: { "Content-Type": "multipart/form-data" },
  });
}

/**
 * 上传成本指标
 */
export function uploadCostIndicators(formData) {
  return request({
    url: "/dynamic-cost-effectiveness/cost/upload",
    method: "post",
    data: formData,
    headers: { "Content-Type": "multipart/form-data" },
  });
}

// ============================================
// 指标智能识别 API
// ============================================

/**
 * 批量识别指标
 * @param {string[]} indicators - 指标名称数组
 * @param {object} options - 配置选项
 */
export function batchParseIndicators(indicators, options = {}) {
  return request({
    url: "/indicator/batch-parse",
    method: "post",
    data: {
      indicators,
      category: options.category || '',
      domain: options.domain || '',
      forceReIdentify: options.forceReIdentify || false,
    },
    timeout: 180000,
  });
}

/**
 * 获取指标列表
 */
export function getIndicatorList(category = null, type = null) {
  const params = {};
  if (category) params.category = category;
  if (type) params.type = type;
  return request({
    url: "/indicator/list",
    method: "get",
    params,
  });
}

/**
 * 获取指标详情
 */
export function getIndicatorById(id) {
  return request({
    url: `/indicator/${id}`,
    method: "get",
  });
}

/**
 * 删除指标
 */
export function deleteIndicator(id) {
  return request({
    url: `/indicator/${id}`,
    method: "delete",
  });
}

// ============================================
// 指标智能识别 - 新版 API（三种情况）
// ============================================

/**
 * 智能查询指标（新接口）
 * 返回Top3相似度候选和全部指标下拉列表
 */
export function intelligentQuery(data) {
  return request({
    url: "/indicator/intelligent-query",
    method: "post",
    data: data,
  });
}

/**
 * 查询指标（情况一 + 情况三）
 * 返回数据库语义匹配结果和全部指标下拉列表
 */
export function queryIndicator(data) {
  return request({
    url: "/indicator/query",
    method: "post",
    data: data,
  });
}

/**
 * API分析单个指标（情况二）
 * 调用DeepSeek API分析单个指标
 */
export function analyzeSingleIndicator(data) {
  return request({
    url: "/indicator/analyze-single",
    method: "post",
    data: data,
  });
}

/**
 * 保存用户选择
 */
export function saveIndicatorSelection(data) {
  return request({
    url: "/indicator/save-selection",
    method: "post",
    data: data,
  });
}

// ============================================
// 指标数据源字段映射 API
// ============================================

/**
 * 搜索相似字段
 * 根据数据源名称搜索数据库中的相似字段
 * @param {string} dataName - 数据源名称
 * @param {number} limit - 返回数量
 */
export function searchColumnSuggestions(dataName, limit = 5) {
  return request({
    url: "/indicator/source-data/column-suggestions",
    method: "get",
    params: { dataName, limit },
  });
}

/**
 * 获取指标数据源字段建议
 * 结合API分析结果和数据库搜索
 * @param {object} data - 请求数据 { indicatorName }
 */
export function getFieldSuggestions(data) {
  return request({
    url: "/indicator/source-data/field-suggestions",
    method: "post",
    data: data,
  });
}
