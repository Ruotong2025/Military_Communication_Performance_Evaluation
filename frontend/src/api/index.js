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

// 获取表数据
export function getTableData(tableName, page = 1, size = 20) {
  return request({
    url: `/table/data/${tableName}`,
    method: "get",
    params: { page, size },
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
