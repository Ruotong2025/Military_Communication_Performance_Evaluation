package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.dto.DynamicAhpAggregationRequest;
import com.ccnu.military.dto.DynamicAhpAggregationResponse;
import com.ccnu.military.entity.DynamicAhpAggregationResult;
import com.ccnu.military.service.DynamicAhpAggregationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 动态AHP专家集结控制器
 */
@Slf4j
@RestController
@RequestMapping("/dynamic-ahp/collective")
@RequiredArgsConstructor
@Tag(name = "动态AHP专家集结", description = "动态指标体系的AHP专家集结计算")
public class DynamicAhpAggregationController {

    private final DynamicAhpAggregationService aggregationService;

    @GetMapping("/templates")
    @Operation(summary = "获取可用模板列表", description = "获取有AHP打分数据的指标模板列表")
    public ApiResponse<List<Map<String, Object>>> getAvailableTemplates() {
        log.info("API调用: 获取可用模板列表");
        try {
            List<Map<String, Object>> templates = aggregationService.getAvailableTemplates();
            return ApiResponse.success("查询成功", templates);
        } catch (Exception e) {
            log.error("获取模板列表失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @GetMapping("/experts")
    @Operation(summary = "获取可用专家列表", description = "获取指定模板下有AHP打分的专家列表")
    public ApiResponse<List<Map<String, Object>>> getAvailableExperts(
            @Parameter(description = "模板ID")
            @RequestParam Long templateId) {
        log.info("API调用: 获取可用专家列表 - templateId={}", templateId);
        try {
            List<Map<String, Object>> experts = aggregationService.getAvailableExperts(templateId);
            return ApiResponse.success("查询成功", experts);
        } catch (Exception e) {
            log.error("获取专家列表失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @PostMapping("/preview")
    @Operation(summary = "预览集结结果", description = "预览集结结果（不保存到数据库）")
    public ApiResponse<DynamicAhpAggregationResponse> previewAggregation(
            @RequestBody DynamicAhpAggregationRequest request) {
        log.info("API调用: 预览集结 - templateId={}, useAllExperts={}, expertIds={}",
                request.getTemplateId(), request.getUseAllExperts(), request.getExpertIds());
        try {
            DynamicAhpAggregationResponse response = aggregationService.previewAggregation(request);
            return ApiResponse.success("预览成功", response);
        } catch (Exception e) {
            log.error("预览集结失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/calculate")
    @Operation(summary = "执行集结计算", description = "执行集结计算并保存到数据库")
    public ApiResponse<DynamicAhpAggregationResponse> executeAggregation(
            @RequestBody DynamicAhpAggregationRequest request) {
        log.info("API调用: 执行集结计算 - templateId={}, useAllExperts={}, expertIds={}",
                request.getTemplateId(), request.getUseAllExperts(), request.getExpertIds());
        try {
            DynamicAhpAggregationResponse response = aggregationService.executeAggregation(request);
            return ApiResponse.success("集结计算完成", response);
        } catch (Exception e) {
            log.error("执行集结计算失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/results")
    @Operation(summary = "获取集结结果列表", description = "获取指定模板的所有集结结果")
    public ApiResponse<List<DynamicAhpAggregationResult>> getAggregationResults(
            @Parameter(description = "模板ID")
            @RequestParam Long templateId) {
        log.info("API调用: 获取集结结果列表 - templateId={}", templateId);
        try {
            List<DynamicAhpAggregationResult> results = aggregationService.getAggregationResults(templateId);
            return ApiResponse.success("查询成功", results);
        } catch (Exception e) {
            log.error("获取集结结果列表失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @GetMapping("/results/{groupId}")
    @Operation(summary = "获取单个集结结果", description = "根据groupId获取集结结果详情")
    public ApiResponse<DynamicAhpAggregationResponse> getAggregationResult(
            @Parameter(description = "专家组ID")
            @PathVariable String groupId) {
        log.info("API调用: 获取集结结果详情 - groupId={}", groupId);
        try {
            DynamicAhpAggregationResponse response = aggregationService.getAggregationResult(groupId);
            return ApiResponse.success("查询成功", response);
        } catch (Exception e) {
            log.error("获取集结结果失败", e);
            return ApiResponse.error(404, e.getMessage());
        }
    }

    @DeleteMapping("/results/{groupId}")
    @Operation(summary = "删除集结结果", description = "根据groupId删除集结结果")
    public ApiResponse<Void> deleteAggregationResult(
            @Parameter(description = "专家组ID")
            @PathVariable String groupId) {
        log.info("API调用: 删除集结结果 - groupId={}", groupId);
        try {
            aggregationService.deleteAggregationResult(groupId);
            return ApiResponse.success("删除成功", null);
        } catch (Exception e) {
            log.error("删除集结结果失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }
}
