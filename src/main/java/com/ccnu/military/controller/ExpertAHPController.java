package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.dto.MatrixCalculationRequest;
import com.ccnu.military.dto.MatrixCalculationResult;
import com.ccnu.military.service.ExpertAHPService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 专家AHP判断矩阵计算控制器
 */
@Slf4j
@RestController
@RequestMapping("/evaluation/expert-ahp")
@Tag(name = "专家AHP层次分析法", description = "基于专家打分的AHP权重计算接口")
@CrossOrigin(origins = "*")
public class ExpertAHPController {

    private final ExpertAHPService expertAHPService;

    public ExpertAHPController(ExpertAHPService expertAHPService) {
        this.expertAHPService = expertAHPService;
    }

    @Operation(summary = "获取AHP矩阵结构定义", description = "返回维度层和各维度指标层的元素名称列表")
    @GetMapping("/meta")
    public ApiResponse<Map<String, Object>> getMeta() {
        try {
            Map<String, Object> meta = new java.util.LinkedHashMap<>();
            meta.put("dimensions", ExpertAHPService.getDimensions());
            meta.put("indicators", ExpertAHPService.getDimensionIndicators());
            return ApiResponse.success(meta);
        } catch (Exception e) {
            log.error("获取AHP元信息失败", e);
            return ApiResponse.error("获取元信息失败: " + e.getMessage());
        }
    }

    @Operation(summary = "计算AHP权重", description = "根据判断矩阵（上三角任意位置输入，自动生成下三角倒数）计算各层权重和一致性比率")
    @PostMapping("/calculate")
    public ApiResponse<MatrixCalculationResult> calculateMatrix(@RequestBody MatrixCalculationRequest request) {
        try {
            log.info("收到AHP矩阵计算请求");
            MatrixCalculationResult result = expertAHPService.calculateAll(request);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("AHP矩阵计算失败", e);
            return ApiResponse.error("计算失败: " + e.getMessage());
        }
    }
}
