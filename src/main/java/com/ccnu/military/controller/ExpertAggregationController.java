package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.dto.ExpertAggregationResult;
import com.ccnu.military.entity.ExpertAhpIndividualWeights;
import com.ccnu.military.service.ExpertAggregationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 专家集结 AHP 聚合控制器 - 计算各专家权重的变异系数 CV
 */
@Slf4j
@RestController
@RequestMapping("/evaluation/expert-aggregation")
@RequiredArgsConstructor
@Tag(name = "专家集结AHP聚合", description = "专家AHP权重变异系数CV计算与一致性分析")
public class ExpertAggregationController {

    private final ExpertAggregationService aggregationService;

    @Operation(summary = "获取CV聚合结果", description = "计算所有专家权重的变异系数CV，包含三种数据筛选方式的结果")
    @GetMapping("/cv-result")
    public ApiResponse<ExpertAggregationResult> getCvResult() {
        try {
            ExpertAggregationResult result = aggregationService.getAggregationResult();
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取CV聚合结果失败", e);
            return ApiResponse.error("获取CV结果失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取所有专家权重数据", description = "返回expert_ahp_individual_weights表中所有专家的权重快照")
    @GetMapping("/weights")
    public ApiResponse<List<ExpertAhpIndividualWeights>> getAllWeights() {
        try {
            List<ExpertAhpIndividualWeights> weights = aggregationService.getAllExpertWeights();
            return ApiResponse.success(weights);
        } catch (Exception e) {
            log.error("获取专家权重数据失败", e);
            return ApiResponse.error("获取权重数据失败: " + e.getMessage());
        }
    }
}
