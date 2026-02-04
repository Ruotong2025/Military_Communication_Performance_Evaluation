package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.dto.EvaluationResultDTO;
import com.ccnu.military.entity.MilitaryEffectivenessEvaluation;
import com.ccnu.military.service.MilitaryEffectivenessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 军事效能评估控制器
 */
@Slf4j
@RestController
@RequestMapping("/evaluation")
@RequiredArgsConstructor
@Tag(name = "军事效能评估", description = "军事通信效能评估相关接口")
public class MilitaryEffectivenessController {

    private final MilitaryEffectivenessService service;

    @GetMapping("/list")
    @Operation(summary = "查询所有评估记录", description = "获取所有军事效能评估记录")
    public ApiResponse<List<MilitaryEffectivenessEvaluation>> getAllEvaluations() {
        log.info("API调用: 查询所有评估记录");
        List<MilitaryEffectivenessEvaluation> evaluations = service.findAll();
        return ApiResponse.success("查询成功", evaluations);
    }

    @GetMapping("/test/{testId}")
    @Operation(summary = "根据测试批次ID查询", description = "根据测试批次ID查询评估记录")
    public ApiResponse<MilitaryEffectivenessEvaluation> getByTestId(
            @Parameter(description = "测试批次ID", example = "TEST-2026-001")
            @PathVariable String testId) {
        log.info("API调用: 查询测试批次 {}", testId);
        return service.findByTestId(testId)
                .map(eval -> ApiResponse.success("查询成功", eval))
                .orElse(ApiResponse.error(404, "未找到该测试批次的评估记录"));
    }

    @GetMapping("/scenario/{scenarioId}")
    @Operation(summary = "根据场景ID查询", description = "根据场景ID查询所有评估记录")
    public ApiResponse<List<MilitaryEffectivenessEvaluation>> getByScenarioId(
            @Parameter(description = "场景ID", example = "1")
            @PathVariable Integer scenarioId) {
        log.info("API调用: 查询场景ID {}", scenarioId);
        List<MilitaryEffectivenessEvaluation> evaluations = service.findByScenarioId(scenarioId);
        return ApiResponse.success("查询成功", evaluations);
    }

    @GetMapping("/crashes")
    @Operation(summary = "查询有崩溃记录的评估", description = "查询网络崩溃次数大于0的评估记录")
    public ApiResponse<List<MilitaryEffectivenessEvaluation>> getWithCrashes() {
        log.info("API调用: 查询有崩溃记录的评估");
        List<MilitaryEffectivenessEvaluation> evaluations = service.findWithCrashes();
        return ApiResponse.success("查询成功", evaluations);
    }

    @GetMapping("/low-success-rate")
    @Operation(summary = "查询低成功率评估", description = "查询任务成功率低于指定阈值的评估记录")
    public ApiResponse<List<MilitaryEffectivenessEvaluation>> getLowSuccessRate(
            @Parameter(description = "成功率阈值", example = "0.9")
            @RequestParam(defaultValue = "0.9") Double threshold) {
        log.info("API调用: 查询成功率低于 {} 的评估", threshold);
        List<MilitaryEffectivenessEvaluation> evaluations = service.findLowSuccessRate(threshold);
        return ApiResponse.success("查询成功", evaluations);
    }

    @GetMapping("/results")
    @Operation(summary = "计算综合评估结果", description = "计算所有测试批次的综合评估结果，包含得分和排名")
    public ApiResponse<List<EvaluationResultDTO>> getEvaluationResults() {
        log.info("API调用: 计算综合评估结果");
        List<EvaluationResultDTO> results = service.calculateEvaluationResults();
        return ApiResponse.success("计算成功", results);
    }

    @GetMapping("/count")
    @Operation(summary = "获取评估记录总数", description = "获取数据库中的评估记录总数")
    public ApiResponse<Long> getTotalCount() {
        log.info("API调用: 获取评估记录总数");
        Long count = service.getTotalCount();
        return ApiResponse.success("查询成功", count);
    }
}
