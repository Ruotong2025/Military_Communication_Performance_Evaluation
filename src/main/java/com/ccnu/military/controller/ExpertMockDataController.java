package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.dto.GenerateExpertsRequest;
import com.ccnu.military.entity.ExpertBaseInfo;
import com.ccnu.military.entity.ExpertCredibilityScore;
import com.ccnu.military.service.ExpertMockDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 模拟专家数据生成控制器
 */
@Slf4j
@RestController
@RequestMapping("/expert/mock")
@RequiredArgsConstructor
@Tag(name = "模拟数据生成", description = "生成专家模拟数据")
public class ExpertMockDataController {

    private final ExpertMockDataService mockDataService;

    @PostMapping("/generate")
    @Operation(summary = "生成专家模拟数据", description = "生成指定数量的专家模拟数据，不自动评估")
    public ApiResponse<List<ExpertBaseInfo>> generateExperts(@RequestBody GenerateExpertsRequest request) {
        log.info("API调用: 生成专家模拟数据 - count={}", request.getCount());
        try {
            List<ExpertBaseInfo> experts = mockDataService.generateMockExperts(request);
            return ApiResponse.success("成功生成 " + experts.size() + " 位专家模拟数据", experts);
        } catch (Exception e) {
            log.error("生成专家模拟数据失败", e);
            return ApiResponse.error("生成失败: " + e.getMessage());
        }
    }

    @PostMapping("/generate-and-evaluate")
    @Operation(summary = "生成并评估专家数据", description = "生成专家模拟数据并自动进行可信度评估")
    public ApiResponse<Map<String, Object>> generateAndEvaluate(@RequestBody GenerateExpertsRequest request) {
        log.info("API调用: 生成并评估专家数据 - count={}", request.getCount());
        try {
            Map<String, Object> result = mockDataService.generateAndEvaluate(request);
            return ApiResponse.success(
                    "成功生成 " + result.get("totalGenerated") + " 位专家并评估了 " + result.get("totalEvaluated") + " 位",
                    result
            );
        } catch (Exception e) {
            log.error("生成并评估专家数据失败", e);
            return ApiResponse.error("生成失败: " + e.getMessage());
        }
    }
}
