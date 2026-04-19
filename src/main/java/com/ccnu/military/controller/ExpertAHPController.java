package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.dto.CrossDomainAhpPersistRequest;
import com.ccnu.military.dto.ExpertAhpScoresPersistRequest;
import com.ccnu.military.dto.ExpertAhpSimulateRequest;
import com.ccnu.military.dto.MatrixCalculationRequest;
import com.ccnu.military.dto.MatrixCalculationResult;
import com.ccnu.military.entity.ExpertAhpComparisonScore;
import com.ccnu.military.service.AhpBatchSimulateOrchestrator;
import com.ccnu.military.service.ExpertAHPService;
import com.ccnu.military.service.ExpertAhpComparisonScoreService;
import com.ccnu.military.service.ExpertAhpIndividualWeightsService;
import com.ccnu.military.service.AhpIndividualService;
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
    private final AhpBatchSimulateOrchestrator ahpBatchSimulateOrchestrator;
    private final AhpIndividualService unifiedAhpService;

    public ExpertAHPController(
            ExpertAHPService expertAHPService,
            ExpertAhpComparisonScoreService expertAhpComparisonScoreService,
            ExpertAhpIndividualWeightsService expertAhpIndividualWeightsService,
            AhpBatchSimulateOrchestrator ahpBatchSimulateOrchestrator,
            AhpIndividualService unifiedAhpService) {
        this.expertAHPService = expertAHPService;
        this.expertAhpComparisonScoreService = expertAhpComparisonScoreService;
        this.expertAhpIndividualWeightsService = expertAhpIndividualWeightsService;
        this.ahpBatchSimulateOrchestrator = ahpBatchSimulateOrchestrator;
        this.unifiedAhpService = unifiedAhpService;
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
    public ApiResponse<com.ccnu.military.dto.AhpIndividualResult> getIndividualWeights(@RequestParam Long expertId) {
        try {
            com.ccnu.military.dto.AhpIndividualResult result = expertAhpIndividualWeightsService.findResult(expertId);
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

    @Operation(summary = "保存域间一级 AHP 比较", description = "效能指标体系 vs 装备操作体系 的单对比较，comparison_key=域间一级_效能指标_装备操作")
    @PostMapping("/cross-domain-score")
    public ApiResponse<Void> saveCrossDomainScore(@RequestBody CrossDomainAhpPersistRequest request) {
        try {
            expertAhpComparisonScoreService.persistCrossDomain(request);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("保存域间一级 AHP 比较失败", e);
            return ApiResponse.error("保存失败: " + e.getMessage());
        }
    }

    @Operation(summary = "批量模拟专家AHP打分", description = "为多名专家同时生成并入库效能指标 AHP 与装备操作 AHP 比较打分（同表不同前缀）；随机标度与把握度规则同单套模拟")
    @PostMapping("/scores/simulate")
    public ApiResponse<Map<String, Object>> simulateScores(@RequestBody ExpertAhpSimulateRequest request) {
        try {
            Map<String, Object> result = ahpBatchSimulateOrchestrator.simulateEffectivenessAndEquipment(request);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("模拟AHP打分失败", e);
            return ApiResponse.error("模拟失败: " + e.getMessage());
        }
    }

    @Operation(summary = "查询统一 AHP 权重快照", description = "返回 expert_ahp_individual_weights 中该专家的统一快照（域间权重 + 效能叶子全局权重 + 装备叶子 JSON），含所有叶子权重列表")
    @GetMapping("/unified-weights")
    public ApiResponse<com.ccnu.military.dto.AhpIndividualResult> getUnifiedWeights(@RequestParam Long expertId) {
        try {
            com.ccnu.military.dto.AhpIndividualResult result = unifiedAhpService.findUnifiedResult(expertId);
            if (result == null) {
                return ApiResponse.error("未找到该专家的统一权重快照");
            }
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("查询统一 AHP 权重快照失败", e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "触发统一 AHP 快照重算", description = "根据当前数据库中域间 + 效能 + 装备打分记录，重新计算并覆盖写入 expert_ahp_individual_weights")
    @PostMapping("/unified-weights/recalculate")
    public ApiResponse<Void> recalculateUnifiedWeights(@RequestParam Long expertId) {
        try {
            String name = expertAhpComparisonScoreService.listByExpert(expertId)
                    .stream().findFirst()
                    .map(com.ccnu.military.entity.ExpertAhpComparisonScore::getExpertName)
                    .orElse(null);
            unifiedAhpService.persistUnified(expertId, name);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("重算统一 AHP 快照失败", e);
            return ApiResponse.error("重算失败: " + e.getMessage());
        }
    }
}
