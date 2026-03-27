package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.dto.MetricsCalculationRequest;
import com.ccnu.military.service.MetricsCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 指标计算控制器
 */
@RestController
@RequestMapping("/metrics")
@RequiredArgsConstructor
@Tag(name = "指标计算", description = "军事通信效能指标计算")
public class MetricsCalculationController {

    private final MetricsCalculationService metricsCalculationService;

    /**
     * 计算指标数据
     * @param request 包含要计算的作战ID列表
     */
    @PostMapping("/calculate")
    @Operation(summary = "计算指标", description = "根据作战ID计算军事通信效能指标")
    public ApiResponse<Map<String, Object>> calculateMetrics(@RequestBody MetricsCalculationRequest request) {
        Map<String, Object> result = metricsCalculationService.calculateMetrics(request);
        Boolean success = (Boolean) result.get("success");
        if (Boolean.TRUE.equals(success)) {
            return ApiResponse.success((String) result.get("message"), result);
        }
        return ApiResponse.error(500, (String) result.get("message"));
    }

    /**
     * 获取可选的作战ID列表
     */
    @GetMapping("/operations")
    @Operation(summary = "获取作战ID列表", description = "获取所有可选的作战ID用于指标计算")
    public ApiResponse<List<Map<String, Object>>> getAvailableOperations() {
        List<Map<String, Object>> operations = metricsCalculationService.getAvailableOperations();
        return ApiResponse.success("获取成功", operations);
    }

    /**
     * 获取评估批次列表
     */
    @GetMapping("/batches")
    @Operation(summary = "评估批次列表", description = "按批次汇总已保存的指标计算记录")
    public ApiResponse<List<Map<String, Object>>> listEvaluationBatches() {
        List<Map<String, Object>> batches = metricsCalculationService.listEvaluationBatches();
        return ApiResponse.success("获取成功", batches);
    }

    /**
     * 获取已计算的指标数据（按评估批次）
     */
    @GetMapping("/results")
    @Operation(summary = "获取计算结果", description = "按 evaluationId（评估批次）获取指标数据")
    public ApiResponse<List<Map<String, Object>>> getCalculatedMetrics(
            @RequestParam(value = "evaluationId", required = false) String evaluationId) {
        List<Map<String, Object>> metrics = metricsCalculationService.getCalculatedMetrics(evaluationId);
        return ApiResponse.success("获取成功", metrics);
    }

    /**
     * 删除指定评估批次的全部指标
     */
    @DeleteMapping("/batches")
    @Operation(summary = "删除评估批次", description = "删除该批次下所有作战指标行")
    public ApiResponse<Map<String, Object>> deleteEvaluationBatch(@RequestParam("evaluationId") String evaluationId) {
        int deleted = metricsCalculationService.deleteEvaluationBatch(evaluationId);
        if (deleted <= 0) {
            return ApiResponse.error(404, "未找到该评估批次或已无数据");
        }
        return ApiResponse.success("已删除 " + deleted + " 条记录", new java.util.LinkedHashMap<String, Object>() {{ put("deleted", deleted); }});
    }

    /**
     * 获取归一化 score 数据
     */
    @GetMapping("/scores")
    @Operation(summary = "获取归一化得分", description = "按 evaluationId 获取归一化后的 score 数据（0~1，越大越好）")
    public ApiResponse<List<Map<String, Object>>> getScoreData(
            @RequestParam(value = "evaluationId", required = false) String evaluationId) {
        if (evaluationId == null || evaluationId.trim().isEmpty()) {
            return ApiResponse.error(400, "evaluationId 不能为空");
        }
        List<Map<String, Object>> data = metricsCalculationService.getScoreData(evaluationId);
        return ApiResponse.success("获取成功", data);
    }

    /**
     * 获取 ECharts 图表数据（多实验对比）
     */
    @GetMapping("/score-chart/{evaluationId}")
    @Operation(summary = "获取图表数据", description = "返回 ECharts 直接可用的多实验对比图数据")
    public ApiResponse<Map<String, Object>> getScoreChartData(
            @PathVariable("evaluationId") String evaluationId) {
        Map<String, Object> data = metricsCalculationService.getScoreChartData(evaluationId);
        if (data == null || data.isEmpty()) {
            return ApiResponse.error(404, "未找到该评估批次的 score 数据");
        }
        return ApiResponse.success("获取成功", data);
    }

    /**
     * 计算归一化 score 数据（独立接口，需先执行「计算指标」）
     */
    @PostMapping("/generate-score")
    @Operation(summary = "计算归一化得分", description = "根据已有指标数据（metrics_military_comm_effect）计算归一化 score 并写入 score 表")
    public ApiResponse<Map<String, Object>> generateScore(
            @RequestParam("evaluationId") String evaluationId) {
        Map<String, Object> result = metricsCalculationService.generateScore(evaluationId);
        Boolean success = (Boolean) result.get("success");
        if (Boolean.TRUE.equals(success)) {
            return ApiResponse.success((String) result.get("message"), result);
        }
        return ApiResponse.error(400, (String) result.get("message"));
    }
}
