package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.service.EquipmentQlEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 装备操作定性指标评估控制器
 */
@RestController
@RequestMapping("/equipment/ql")
@RequiredArgsConstructor
public class EquipmentQlController {

    private final EquipmentQlEvaluationService evaluationService;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 获取定性指标配置列表
     */
    @GetMapping("/indicators")
    public ApiResponse<List<Map<String, Object>>> getIndicators() {
        return ApiResponse.success("获取成功", evaluationService.getIndicatorDefs());
    }

    /**
     * 获取指定指标的参考数据
     */
    @GetMapping("/reference/{operationId}/{indicatorKey}")
    public ApiResponse<List<Map<String, Object>>> getReferenceData(
            @PathVariable("operationId") Long operationId,
            @PathVariable("indicatorKey") String indicatorKey) {
        return ApiResponse.success("获取成功", evaluationService.getReferenceData(operationId, indicatorKey));
    }

    /**
     * 批量模拟定性评分：当前批次下全部（已可信度评估）专家对指定作战生成数据，覆盖已有记录。
     * 请求体 {@code allOperations: true}（或 {@code operationId} 为 ALL / __ALL__）时，对该批次内全部作战各生成一遍。
     */
    @PostMapping("/simulate-batch")
    public ApiResponse<Map<String, Object>> simulateBatch(@RequestBody Map<String, Object> payload) {
        Map<String, Object> result = evaluationService.simulateQlBatch(payload);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ApiResponse.success((String) result.get("message"), result);
        }
        return ApiResponse.error(400, (String) result.get("message"));
    }

    /**
     * 专家提交定性评分
     */
    @PostMapping("/submit")
    public ApiResponse<Map<String, Object>> submit(@RequestBody Map<String, Object> payload) {
        Map<String, Object> result = evaluationService.submitEvaluation(payload);
        Boolean success = (Boolean) result.get("success");
        if (Boolean.TRUE.equals(success)) {
            return ApiResponse.success((String) result.get("message"), result);
        }
        return ApiResponse.error(500, (String) result.get("message"));
    }

    /**
     * 获取评估批次列表
     */
    @GetMapping("/batches")
    public ApiResponse<List<Map<String, Object>>> listBatches() {
        return ApiResponse.success("获取成功", evaluationService.listBatches());
    }

    /**
     * 获取某评估批次下可选作战（与指标计算批次内作战一致）
     */
    @GetMapping("/operations-for-batch")
    public ApiResponse<List<Map<String, Object>>> operationsForBatch(
            @RequestParam(value = "evaluationBatchId", required = false) String evaluationBatchId) {
        return ApiResponse.success("获取成功", evaluationService.listOperationsForEvaluationBatch(evaluationBatchId));
    }

    /**
     * 获取批次下的记录
     */
    @GetMapping("/records")
    public ApiResponse<List<Map<String, Object>>> getRecords(
            @RequestParam("evaluationBatchId") String batchId,
            @RequestParam(value = "operationId", required = false) String operationId) {
        return ApiResponse.success("获取成功", evaluationService.getRecordsByBatchAndOperation(batchId, operationId));
    }

    /**
     * 删除批次
     */
    @DeleteMapping("/batches")
    public ApiResponse<Map<String, Object>> deleteBatch(@RequestParam("evaluationBatchId") String batchId) {
        int deleted = evaluationService.deleteByBatch(batchId);
        if (deleted > 0) {
            return ApiResponse.success("删除成功", Map.of("deleted", deleted));
        }
        return ApiResponse.error(404, "未找到该批次");
    }

    /**
     * 获取专家列表（供前端选择）- 仅返回已完成可信度评估的专家
     */
    @GetMapping("/experts")
    public ApiResponse<List<Map<String, Object>>> getExperts() {
        String sql = "SELECT e.expert_id AS expertId, e.expert_name AS expertName, "
                + "c.total_score AS credibilityScore, c.credibility_level AS credibilityLevel "
                + "FROM expert_base_info e "
                + "INNER JOIN expert_credibility_evaluation_score c ON e.expert_id = c.expert_id "
                + "ORDER BY c.total_score DESC";
        List<Map<String, Object>> experts = jdbcTemplate.queryForList(sql);
        return ApiResponse.success("获取成功", experts);
    }

    /**
     * 获取指定专家在指定批次的定性评估记录（用于二次打开回显）
     */
    @GetMapping("/record-for-edit")
    public ApiResponse<Map<String, Object>> getRecordForEdit(
            @RequestParam String evaluationBatchId,
            @RequestParam String operationId,
            @RequestParam Long expertId) {
        Map<String, Object> record = evaluationService.getRecordForEdit(evaluationBatchId, operationId, expertId);
        if (record == null) {
            return ApiResponse.error(404, "未找到该记录");
        }
        return ApiResponse.success("获取成功", record);
    }
}
