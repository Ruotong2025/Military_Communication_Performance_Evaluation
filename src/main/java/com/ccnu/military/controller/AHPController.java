package com.ccnu.military.controller;

import com.ccnu.military.dto.AHPRequest;
import com.ccnu.military.dto.AHPResult;
import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.service.AHPService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * AHP层次分析法控制器
 */
@Slf4j
@RestController
@RequestMapping("/evaluation/ahp")
@RequiredArgsConstructor
@Tag(name = "AHP层次分析法", description = "AHP权重计算相关接口")
public class AHPController {

    private final AHPService ahpService;

    @PostMapping("/calculate")
    @Operation(summary = "计算AHP权重", description = "根据维度优先级计算AHP判断矩阵和权重")
    public ApiResponse<AHPResult> calculateAHP(@RequestBody AHPRequest request) {
        log.info("API调用: 计算AHP权重 - priorities={}", request.getPriorities());
        try {
            AHPResult result = ahpService.calculate(request.getPriorities());
            return ApiResponse.success("计算成功", result);
        } catch (Exception e) {
            log.error("AHP计算失败", e);
            return ApiResponse.error("计算失败: " + e.getMessage());
        }
    }
}
