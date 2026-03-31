package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.dto.ExpertAhpScoresPersistRequest;
import com.ccnu.military.dto.ExpertAhpSimulateRequest;
import com.ccnu.military.dto.MatrixCalculationRequest;
import com.ccnu.military.dto.MatrixCalculationResult;
import com.ccnu.military.entity.ExpertAhpComparisonScore;
import com.ccnu.military.service.ExpertAHPService;
import com.ccnu.military.service.ExpertAhpComparisonScoreService;
import com.ccnu.military.service.ExpertAhpIndividualWeightsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 专家AHP判断矩阵计算控制器
 */
@Slf4j
@RestController
@RequestMapping("/evaluation/expert-ahp")
@Tag(name = "专家AHP层次分析法", description = "基于专家打分的AHP权重计算接口")
public class ExpertAHPController {

    private final ExpertAHPService expertAHPService;
    private final ExpertAhpComparisonScoreService expertAhpComparisonScoreService;
    private final ExpertAhpIndividualWeightsService expertAhpIndividualWeightsService;

    public ExpertAHPController(
            ExpertAHPService expertAHPService,
            ExpertAhpComparisonScoreService expertAhpComparisonScoreService,
            ExpertAhpIndividualWeightsService expertAhpIndividualWeightsService) {
        this.expertAHPService = expertAHPService;
        this.expertAhpComparisonScoreService = expertAhpComparisonScoreService;
        this.expertAhpIndividualWeightsService = expertAhpIndividualWeightsService;
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

    @Operation(summary = "查询专家已保存的AHP权重快照", description = "按 expert_id 读取 expert_ahp_individual_weights")
    @GetMapping("/individual-weights")
    public ApiResponse<MatrixCalculationResult> getIndividualWeights(@RequestParam Long expertId) {
        try {
            MatrixCalculationResult result = expertAhpIndividualWeightsService.findResult(expertId);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("查询专家AHP权重快照失败", e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "查询专家已保存的AHP比较打分", description = "按 expert_id 读取 expert_ahp_comparison_score")
    @GetMapping("/scores")
    public ApiResponse<List<ExpertAhpComparisonScore>> getScores(@RequestParam Long expertId) {
        try {
            List<ExpertAhpComparisonScore> list = expertAhpComparisonScoreService.listByExpert(expertId);
            return ApiResponse.success(list);
        } catch (Exception e) {
            log.error("查询AHP打分失败", e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "保存专家AHP比较打分", description = "覆盖写入该专家全部比较对到 expert_ahp_comparison_score")
    @PostMapping("/scores")
    public ApiResponse<Integer> saveScores(@RequestBody ExpertAhpScoresPersistRequest request) {
        try {
            int n = expertAhpComparisonScoreService.persist(request);
            return ApiResponse.success(n);
        } catch (Exception e) {
            log.error("保存AHP打分失败", e);
            return ApiResponse.error("保存失败: " + e.getMessage());
        }
    }

    @Operation(summary = "批量模拟专家AHP打分", description = "为多名专家生成随机标度与把握度（约20%把握度<0.6）；标度由随机权重比 a_i/a_j 反推，理论一致性好，舍入后仍校验各矩阵CR<0.1")
    @PostMapping("/scores/simulate")
    public ApiResponse<Map<String, Object>> simulateScores(@RequestBody ExpertAhpSimulateRequest request) {
        try {
            Map<String, Object> result = expertAhpComparisonScoreService.simulate(request);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("模拟AHP打分失败", e);
            return ApiResponse.error("模拟失败: " + e.getMessage());
        }
    }
}
