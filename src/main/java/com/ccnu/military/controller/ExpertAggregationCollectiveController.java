package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.dto.CollectiveCalculateRequest;
import com.ccnu.military.dto.CollectiveCalculateResponse;
import com.ccnu.military.dto.CollectiveWeightPreview;
import com.ccnu.military.entity.ExpertAhpGroupWeights;
import com.ccnu.military.service.ExpertAggregationCollectiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 专家集结计算控制器（对比打分层集结）
 */
@Slf4j
@RestController
@RequestMapping("/evaluation/collective")
@RequiredArgsConstructor
@Tag(name = "专家集结计算", description = "基于对比打分层的专家集结，计算集体判断矩阵和集结权重")
public class ExpertAggregationCollectiveController {

    private final ExpertAggregationCollectiveService collectiveService;

    /**
     * 获取所有评估批次ID（用于前端选择批次进行计算）
     */
    @Operation(summary = "获取所有评估批次ID", description = "返回score_military_comm_effect表中所有评估批次")
    @GetMapping("/evaluation-ids")
    public ApiResponse<List<String>> getEvaluationIds() {
        try {
            List<String> ids = collectiveService.getEvaluationIds();
            return ApiResponse.success(ids);
        } catch (Exception e) {
            log.error("获取评估批次ID失败", e);
            return ApiResponse.error("获取评估批次失败: " + e.getMessage());
        }
    }

    @Operation(summary = "预览集结权重", description = "仅计算并返回集结权重，不保存评估结果")
    @GetMapping("/weights-preview")
    public ApiResponse<CollectiveWeightPreview> previewWeights(
            @RequestParam String evaluationId,
            @RequestParam(required = false) List<Long> expertIds,
            @RequestParam(required = false, defaultValue = "true") Boolean useAllExperts) {
        try {
            CollectiveCalculateRequest request = new CollectiveCalculateRequest();
            request.setEvaluationId(evaluationId);
            request.setExpertIds(expertIds);
            request.setUseAllExperts(useAllExperts);
            CollectiveWeightPreview preview = collectiveService.previewWeights(request);
            return ApiResponse.success(preview);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("预览集结权重失败", e);
            return ApiResponse.error("预览失败: " + e.getMessage());
        }
    }

    @Operation(summary = "执行集结计算并保存", description = "仅计算并保存集结权重到 expert_ahp_group_weights（专家集合相同则更新）。综合得分请在「加载综合结果」中计算。")
    @PostMapping("/calculate")
    public ApiResponse<CollectiveCalculateResponse> calculate(
            @RequestBody CollectiveCalculateRequest request) {
        try {
            CollectiveCalculateResponse response = collectiveService.calculateAndSave(request);
            return ApiResponse.success(response);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("执行集结计算失败", e);
            return ApiResponse.error("集结计算失败: " + e.getMessage());
        }
    }

    @Operation(summary = "计算并保存综合结果", description = "用最近一次集结的二级权重 × 该批次 score 表归一化得分，写入 expert_weighted_evaluation_result 并返回（对应「加载综合结果」按钮）")
    @PostMapping("/results/compute")
    public ApiResponse<CollectiveCalculateResponse> computeWeightedResults(
            @RequestParam String evaluationId) {
        try {
            return ApiResponse.success(collectiveService.computeAndPersistWeightedResults(evaluationId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("计算综合结果失败", e);
            return ApiResponse.error("计算失败: " + e.getMessage());
        }
    }

    @Operation(summary = "查询已保存的加权评估结果", description = "按评估批次读取 expert_weighted_evaluation_result（仅读取，不重新计算）")
    @GetMapping("/results")
    public ApiResponse<CollectiveCalculateResponse> getWeightedResults(
            @RequestParam String evaluationId) {
        try {
            return ApiResponse.success(collectiveService.loadWeightedResults(evaluationId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("查询加权评估结果失败", e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除指定批次的加权评估结果")
    @DeleteMapping("/results/{evaluationId}")
    public ApiResponse<Void> deleteWeightedResults(@PathVariable String evaluationId) {
        try {
            collectiveService.deleteWeightedResults(evaluationId);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("删除加权评估结果失败", e);
            return ApiResponse.error("删除失败: " + e.getMessage());
        }
    }

    @Operation(summary = "查询所有已保存的专家组集结权重")
    @GetMapping("/group-weights")
    public ApiResponse<List<ExpertAhpGroupWeights>> getAllGroupWeights() {
        try {
            List<ExpertAhpGroupWeights> weights = collectiveService.getAllGroupWeights();
            return ApiResponse.success(weights);
        } catch (Exception e) {
            log.error("查询专家组集结权重失败", e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "根据专家组ID查询集结权重")
    @GetMapping("/group-weights/{groupId}")
    public ApiResponse<ExpertAhpGroupWeights> getGroupWeightsByGroupId(@PathVariable String groupId) {
        try {
            Optional<ExpertAhpGroupWeights> weights = collectiveService.getGroupWeightsByGroupId(groupId);
            if (weights.isPresent()) {
                return ApiResponse.success(weights.get());
            } else {
                return ApiResponse.error("未找到指定的专家组");
            }
        } catch (Exception e) {
            log.error("查询专家组集结权重失败", e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除指定的专家组集结权重")
    @DeleteMapping("/group-weights/{groupId}")
    public ApiResponse<Void> deleteGroupWeights(@PathVariable String groupId) {
        try {
            collectiveService.deleteGroupWeights(groupId);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("删除专家组集结权重失败", e);
            return ApiResponse.error("删除失败: " + e.getMessage());
        }
    }
}
