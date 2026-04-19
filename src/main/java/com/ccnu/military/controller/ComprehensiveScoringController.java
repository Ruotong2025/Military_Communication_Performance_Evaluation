package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.service.ComprehensiveScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 综合评分控制器
 * 基于效能指标、装备定量指标、装备定性指标进行综合评分
 */
@RestController
@RequestMapping("/equipment/comprehensive")
@RequiredArgsConstructor
public class ComprehensiveScoringController {

    private final ComprehensiveScoringService scoringService;

    /**
     * 获取评估批次列表
     */
    @GetMapping("/batches")
    public ApiResponse<List<String>> getBatches() {
        List<String> batches = scoringService.getEvaluationBatches();
        return ApiResponse.success("获取成功", batches);
    }

    /**
     * 执行综合评分计算
     */
    @PostMapping("/calculate")
    public ApiResponse<Map<String, Object>> calculate(@RequestParam String evaluationBatchId) {
        Map<String, Object> result = scoringService.calculateComprehensiveScores(evaluationBatchId);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ApiResponse.success((String) result.get("message"), result);
        }
        return ApiResponse.error(400, (String) result.get("message"));
    }

    /**
     * 获取已保存的综合评分结果
     */
    @GetMapping("/results")
    public ApiResponse<Map<String, Object>> getResults(@RequestParam String evaluationBatchId) {
        Map<String, Object> result = scoringService.getSavedResults(evaluationBatchId);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ApiResponse.success("获取成功", result);
        }
        return ApiResponse.error(404, (String) result.get("message"));
    }

    /**
     * 删除综合评分结果
     */
    @DeleteMapping("/results")
    public ApiResponse<Void> deleteResults(@RequestParam String evaluationBatchId) {
        scoringService.deleteResults(evaluationBatchId);
        return ApiResponse.success("删除成功", null);
    }

    /**
     * 获取效能指标原始数据
     */
    @GetMapping("/metrics-raw")
    public ApiResponse<Map<String, Object>> getMetricsRaw(@RequestParam String evaluationBatchId) {
        Map<String, Object> data = scoringService.getMetricsRaw(evaluationBatchId);
        return ApiResponse.success("获取成功", data);
    }

    /**
     * 获取效能指标归一化得分
     */
    @GetMapping("/metrics-score")
    public ApiResponse<Map<String, Object>> getMetricsScore(@RequestParam String evaluationBatchId) {
        Map<String, Object> data = scoringService.getMetricsScore(evaluationBatchId);
        return ApiResponse.success("获取成功", data);
    }

    /**
     * 获取装备操作原始数据
     */
    @GetMapping("/equipment-raw")
    public ApiResponse<Map<String, Object>> getEquipmentRaw(@RequestParam String evaluationBatchId) {
        Map<String, Object> data = scoringService.getEquipmentRaw(evaluationBatchId);
        return ApiResponse.success("获取成功", data);
    }

    /**
     * 获取装备操作归一化得分
     */
    @GetMapping("/equipment-score")
    public ApiResponse<Map<String, Object>> getEquipmentScore(@RequestParam String evaluationBatchId) {
        Map<String, Object> data = scoringService.getEquipmentScore(evaluationBatchId);
        return ApiResponse.success("获取成功", data);
    }

    /**
     * 获取所有批次信息（用于调试批次不统一问题）
     */
    @GetMapping("/batch-info")
    public ApiResponse<Map<String, Object>> getBatchInfo() {
        Map<String, Object> info = scoringService.getBatchInfo();
        return ApiResponse.success("获取成功", info);
    }

    /**
     * 获取该批次下的所有作战ID
     */
    @GetMapping("/operations")
    public ApiResponse<Map<String, Object>> getOperations(@RequestParam String evaluationBatchId) {
        Map<String, Object> result = scoringService.getOperationsByBatch(evaluationBatchId);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ApiResponse.success("获取成功", result);
        }
        return ApiResponse.error(404, (String) result.get("message"));
    }

    /**
     * 获取指定作战的完整评估数据（统一视图）
     */
    @GetMapping("/operation-data")
    public ApiResponse<Map<String, Object>> getOperationData(
            @RequestParam String evaluationBatchId,
            @RequestParam String operationId) {
        Map<String, Object> data = scoringService.getOperationData(evaluationBatchId, operationId);
        if (data != null && !data.isEmpty()) {
            return ApiResponse.success("获取成功", data);
        }
        return ApiResponse.error(404, "未找到该作战的数据");
    }
}