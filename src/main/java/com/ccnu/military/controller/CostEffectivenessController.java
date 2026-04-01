package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.dto.CostEffectivenessCalculateRequest;
import com.ccnu.military.dto.CostEffectivenessCalculateResponse;
import com.ccnu.military.dto.CostEffectivenessResultDTO;
import com.ccnu.military.entity.CostIndicatorConfig;
import com.ccnu.military.service.CostEffectivenessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 效费分析控制器
 */
@Slf4j
@RestController
@RequestMapping("/evaluation/cost-effectiveness")
@RequiredArgsConstructor
@Tag(name = "效费分析", description = "成本评估与效费比分析")
public class CostEffectivenessController {

    private final CostEffectivenessService costEffectivenessService;

    /**
     * 获取所有启用的成本指标配置
     */
    @Operation(summary = "获取成本指标配置", description = "获取所有启用的成本指标及其配置")
    @GetMapping("/indicators")
    public ApiResponse<List<CostIndicatorConfig>> getIndicators() {
        try {
            List<CostIndicatorConfig> indicators = costEffectivenessService.getActiveIndicators();
            return ApiResponse.success(indicators);
        } catch (Exception e) {
            log.error("获取成本指标配置失败", e);
            return ApiResponse.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有指标类别
     */
    @Operation(summary = "获取指标类别", description = "获取所有成本指标类别")
    @GetMapping("/categories")
    public ApiResponse<List<String>> getCategories() {
        try {
            List<String> categories = costEffectivenessService.getAllCategories();
            return ApiResponse.success(categories);
        } catch (Exception e) {
            log.error("获取指标类别失败", e);
            return ApiResponse.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 按类别获取指标
     */
    @Operation(summary = "按类别获取指标", description = "按类别获取启用的成本指标")
    @GetMapping("/indicators/by-category")
    public ApiResponse<List<CostIndicatorConfig>> getIndicatorsByCategory(@RequestParam String category) {
        try {
            List<CostIndicatorConfig> indicators = costEffectivenessService.getIndicatorsByCategory(category);
            return ApiResponse.success(indicators);
        } catch (Exception e) {
            log.error("按类别获取指标失败", e);
            return ApiResponse.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取评估结果
     */
    @Operation(summary = "获取评估结果", description = "根据评估批次ID获取效费分析结果")
    @GetMapping("/results")
    public ApiResponse<List<CostEffectivenessResultDTO>> getResults(@RequestParam String evaluationId) {
        try {
            List<CostEffectivenessResultDTO> results = costEffectivenessService.getResults(evaluationId);
            return ApiResponse.success(results);
        } catch (Exception e) {
            log.error("获取评估结果失败", e);
            return ApiResponse.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取作战任务原始成本数据预览（选择批次后自动加载，供用户查看原始数据）
     */
    @Operation(summary = "获取作战任务原始成本数据预览", description = "根据批次和作战任务ID列表获取原始成本数据，用于效费分析前的数据预览")
    @GetMapping("/raw-data-preview")
    public ApiResponse<List<Map<String, Object>>> getRawDataPreview(
            @RequestParam String evaluationId,
            @RequestParam List<String> operationIds) {
        try {
            List<Map<String, Object>> previewData = costEffectivenessService.getRawDataPreview(evaluationId, operationIds);
            return ApiResponse.success(previewData);
        } catch (Exception e) {
            log.error("获取原始数据预览失败", e);
            return ApiResponse.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 执行效费分析计算
     */
    @Operation(summary = "执行效费分析计算", description = "根据配置执行成本评估和效费比计算")
    @PostMapping("/calculate")
    public ApiResponse<CostEffectivenessCalculateResponse> calculate(
            @RequestBody CostEffectivenessCalculateRequest request) {
        try {
            if (request.getEvaluationId() == null || request.getEvaluationId().trim().isEmpty()) {
                return ApiResponse.error(400, "评估批次ID不能为空");
            }
            if (request.getOperationIds() == null || request.getOperationIds().isEmpty()) {
                return ApiResponse.error(400, "作战任务ID列表不能为空");
            }
            CostEffectivenessCalculateResponse response = costEffectivenessService.calculate(request);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("执行效费分析计算失败", e);
            return ApiResponse.error("计算失败: " + e.getMessage());
        }
    }

    /**
     * 删除评估结果
     */
    @Operation(summary = "删除评估结果", description = "根据评估批次ID删除效费分析结果")
    @DeleteMapping("/results")
    public ApiResponse<Void> deleteResults(@RequestParam String evaluationId) {
        try {
            costEffectivenessService.deleteResults(evaluationId);
            return ApiResponse.success("删除成功", null);
        } catch (Exception e) {
            log.error("删除评估结果失败", e);
            return ApiResponse.error("删除失败: " + e.getMessage());
        }
    }
}
