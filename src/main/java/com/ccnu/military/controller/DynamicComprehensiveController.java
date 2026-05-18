package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.dto.DynamicComprehensiveResultDTO;
import com.ccnu.military.dto.RawDataAggregationDTO;
import com.ccnu.military.service.DynamicComprehensiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 动态综合评分控制器
 * 提供动态指标体系的综合评分功能
 */
@Slf4j
@RestController
@RequestMapping("/dynamic-comprehensive")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class DynamicComprehensiveController {

    private final DynamicComprehensiveService service;

    /**
     * 获取评估批次列表
     * @param templateId 可选的模板ID，用于筛选该模板下的批次
     */
    @GetMapping("/batches")
    public ApiResponse<List<Map<String, Object>>> getBatches(
            @RequestParam(required = false) Long templateId) {
        log.info("【综合评分】获取批次列表, templateId={}", templateId);
        List<Map<String, Object>> batches = service.getBatches(templateId);
        log.info("【综合评分】获取批次列表成功, 数量={}", batches.size());
        return ApiResponse.success("获取成功", batches);
    }

    /**
     * 获取原始数据集结表
     * 横向：指标（一级维度 > 二级指标）
     * 纵向：作战ID
     */
    @GetMapping("/raw-data")
    public ApiResponse<RawDataAggregationDTO> getRawDataAggregation(@RequestParam String batchId) {
        log.info("【综合评分】获取原始数据集结表, batchId={}", batchId);
        RawDataAggregationDTO data = service.getRawDataAggregation(batchId);
        if (data.getOperationIds() == null || data.getOperationIds().isEmpty()) {
            log.warn("【综合评分】未找到该批次的数据, batchId={}", batchId);
            return ApiResponse.error(404, "未找到该批次的数据");
        }
        log.info("【综合评分】获取原始数据集结表成功, operationIds数量={}", data.getOperationIds().size());
        return ApiResponse.success("获取成功", data);
    }

    /**
     * 获取综合评分结果
     */
    @GetMapping("/scores")
    public ApiResponse<List<DynamicComprehensiveResultDTO>> getComprehensiveScores(@RequestParam String batchId) {
        log.info("【综合评分】获取综合评分结果, batchId={}", batchId);
        try {
            List<DynamicComprehensiveResultDTO> scores = service.getComprehensiveScores(batchId);
            log.info("【综合评分】获取综合评分结果成功, 数量={}", scores.size());
            if (scores.isEmpty()) {
                log.warn("【综合评分】该批次暂无评分数据, batchId={}", batchId);
                return ApiResponse.error(404, "未找到该批次的评分数据");
            }
            return ApiResponse.success("获取成功", scores);
        } catch (Exception e) {
            log.error("【综合评分】获取综合评分结果失败, batchId={}, error={}", batchId, e.getMessage(), e);
            return ApiResponse.error(500, "获取评分数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取指标树结构
     */
    @GetMapping("/indicator-tree")
    public ApiResponse<Map<String, Object>> getIndicatorTree(@RequestParam Long templateId) {
        log.info("【综合评分】获取指标树结构, templateId={}", templateId);
        Map<String, Object> tree = service.getIndicatorTree(templateId);
        return ApiResponse.success("获取成功", tree);
    }
}
