package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.dto.GlobalWeightsRequest;
import com.ccnu.military.entity.ExpertBaseInfo;
import com.ccnu.military.entity.ExpertCredibilityScore;
import com.ccnu.military.service.ExpertCredibilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 专家可信度评估控制器
 */
@Slf4j
@RestController
@RequestMapping("/expert/credibility")
@RequiredArgsConstructor
@Tag(name = "专家可信度评估", description = "专家基础信息管理与可信度评分")
public class ExpertCredibilityController {

    private final ExpertCredibilityService credibilityService;

    // ============================================================
    // 专家基础信息管理
    // ============================================================

    @GetMapping("/experts")
    @Operation(summary = "获取所有专家列表", description = "返回所有启用的专家基础信息")
    public ApiResponse<List<ExpertBaseInfo>> getAllExperts() {
        log.info("API调用: 获取所有专家列表");
        List<ExpertBaseInfo> experts = credibilityService.getAllActiveExperts();
        return ApiResponse.success("查询成功", experts);
    }

    @GetMapping("/experts/{id}")
    @Operation(summary = "获取专家详情", description = "根据ID获取专家详细信息")
    public ApiResponse<ExpertBaseInfo> getExpertById(
            @Parameter(description = "专家ID") @PathVariable Long id) {
        log.info("API调用: 获取专家详情 - id={}", id);
        return credibilityService.getAllActiveExperts().stream()
                .filter(e -> e.getExpertId().equals(id))
                .findFirst()
                .map(e -> ApiResponse.success("查询成功", e))
                .orElse(ApiResponse.error(404, "专家不存在"));
    }

    @PostMapping("/experts")
    @Operation(summary = "添加专家", description = "添加新专家的基础信息")
    public ApiResponse<ExpertBaseInfo> addExpert(@RequestBody ExpertBaseInfo expert) {
        log.info("API调用: 添加专家 - {}", expert.getExpertName());
        try {
            ExpertBaseInfo saved = credibilityService.saveExpert(expert);
            return ApiResponse.success("添加成功", saved);
        } catch (Exception e) {
            log.error("添加专家失败", e);
            return ApiResponse.error("添加失败: " + e.getMessage());
        }
    }

    @PutMapping("/experts/{id}")
    @Operation(summary = "更新专家信息", description = "更新专家的基础信息")
    public ApiResponse<ExpertBaseInfo> updateExpert(
            @Parameter(description = "专家ID") @PathVariable Long id,
            @RequestBody ExpertBaseInfo expert) {
        log.info("API调用: 更新专家 - id={}", id);
        try {
            expert.setExpertId(id);
            ExpertBaseInfo updated = credibilityService.saveExpert(expert);
            return ApiResponse.success("更新成功", updated);
        } catch (Exception e) {
            log.error("更新专家失败", e);
            return ApiResponse.error("更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/experts/{id}")
    @Operation(summary = "删除专家", description = "删除专家及其评估记录")
    public ApiResponse<Void> deleteExpert(
            @Parameter(description = "专家ID") @PathVariable Long id) {
        log.info("API调用: 删除专家 - id={}", id);
        try {
            credibilityService.deleteExpert(id);
            return ApiResponse.success("删除成功", null);
        } catch (Exception e) {
            log.error("删除专家失败", e);
            return ApiResponse.error("删除失败: " + e.getMessage());
        }
    }

    // ============================================================
    // 可信度评估
    // ============================================================

    @PostMapping("/evaluate/{expertId}")
    @Operation(summary = "评估单个专家", description = "根据专家ID进行可信度评估，生成10个维度得分")
    public ApiResponse<ExpertCredibilityScore> evaluateExpert(
            @Parameter(description = "专家ID") @PathVariable Long expertId) {
        log.info("API调用: 评估专家 - id={}", expertId);
        try {
            ExpertCredibilityScore score = credibilityService.evaluateExpert(expertId);
            return ApiResponse.success("评估成功", score);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("评估失败", e);
            return ApiResponse.error("评估失败: " + e.getMessage());
        }
    }

    @PostMapping("/evaluate/all")
    @Operation(summary = "批量评估所有专家", description = "对所有启用的专家进行可信度评估")
    public ApiResponse<List<ExpertCredibilityScore>> evaluateAllExperts() {
        log.info("API调用: 批量评估所有专家");
        try {
            List<ExpertCredibilityScore> scores = credibilityService.evaluateAllExperts();
            return ApiResponse.success("批量评估完成，共评估" + scores.size() + "位专家", scores);
        } catch (Exception e) {
            log.error("批量评估失败", e);
            return ApiResponse.error("批量评估失败: " + e.getMessage());
        }
    }

    @PostMapping("/evaluate/batch")
    @Operation(summary = "批量评估指定专家", description = "对指定的专家ID列表进行可信度评估")
    public ApiResponse<Map<String, Object>> evaluateBatch(@RequestBody Map<String, Object> request) {
        log.info("API调用: 批量评估指定专家");
        try {
            @SuppressWarnings("unchecked")
            List<Long> expertIds = (List<Long>) request.get("expertIds");
            Boolean overwrite = (Boolean) request.getOrDefault("overwrite", true);
            Map<String, Object> result = credibilityService.evaluateBatch(expertIds, overwrite);
            return ApiResponse.success("批量评估完成", result);
        } catch (Exception e) {
            log.error("批量评估失败", e);
            return ApiResponse.error("批量评估失败: " + e.getMessage());
        }
    }

    @GetMapping("/scores")
    @Operation(summary = "获取所有评估结果", description = "返回所有专家的可信度评估得分，按综合得分降序排列")
    public ApiResponse<List<ExpertCredibilityScore>> getAllScores() {
        log.info("API调用: 获取所有评估结果");
        List<ExpertCredibilityScore> scores = credibilityService.getAllScores();
        return ApiResponse.success("查询成功", scores);
    }

    @GetMapping("/scores/{expertId}")
    @Operation(summary = "获取专家评估得分", description = "根据专家ID获取其评估得分")
    public ApiResponse<ExpertCredibilityScore> getScoreByExpertId(
            @Parameter(description = "专家ID") @PathVariable Long expertId) {
        log.info("API调用: 获取专家评估得分 - id={}", expertId);
        return credibilityService.getScoreByExpertId(expertId)
                .map(s -> ApiResponse.success("查询成功", s))
                .orElse(ApiResponse.error(404, "未找到评估记录"));
    }

    @GetMapping("/scores/level/{level}")
    @Operation(summary = "按等级筛选评估结果", description = "根据可信度等级(A/B/C/D)筛选")
    public ApiResponse<List<ExpertCredibilityScore>> getScoresByLevel(
            @Parameter(description = "可信度等级", example = "A") @PathVariable String level) {
        log.info("API调用: 按等级筛选 - level={}", level);
        List<ExpertCredibilityScore> scores = credibilityService.getScoresByLevel(level);
        return ApiResponse.success("查询成功", scores);
    }

    @GetMapping("/details/{expertId}")
    @Operation(summary = "获取评估详情（包含评估依据）", description = "获取专家的详细评估信息，包括10个维度得分、权重和评估依据")
    public ApiResponse<Map<String, Object>> getEvaluationDetails(
            @Parameter(description = "专家ID") @PathVariable Long expertId) {
        log.info("API调用: 获取评估详情 - id={}", expertId);
        try {
            Map<String, Object> details = credibilityService.getEvaluationDetails(expertId);
            return ApiResponse.success("查询成功", details);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(404, e.getMessage());
        }
    }

    @GetMapping("/experts/with-status")
    @Operation(summary = "获取专家列表（含评估状态）", description = "返回所有专家及其评估状态，用于区分已评估和未评估专家")
    public ApiResponse<Map<String, Object>> getExpertsWithStatus() {
        log.info("API调用: 获取专家列表（含评估状态）");
        try {
            Map<String, Object> result = credibilityService.getExpertsWithStatus();
            return ApiResponse.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取专家列表失败", e);
            return ApiResponse.error("获取失败: " + e.getMessage());
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取评估统计信息", description = "返回评估统计信息，包括总数、各等级数量、平均分等")
    public ApiResponse<Map<String, Object>> getStatistics() {
        log.info("API调用: 获取评估统计信息");
        try {
            Map<String, Object> stats = credibilityService.getStatistics();
            return ApiResponse.success("查询成功", stats);
        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            return ApiResponse.error("获取失败: " + e.getMessage());
        }
    }

    @PostMapping("/weights/global")
    @Operation(summary = "全局调整权重并重算", description = "将十维权重应用到所有已评估记录，重算综合得分与等级")
    public ApiResponse<Map<String, Object>> applyGlobalWeights(@RequestBody GlobalWeightsRequest request) {
        log.info("API调用: 全局权重调整");
        try {
            Map<String, Object> result = credibilityService.applyGlobalWeights(request);
            return ApiResponse.success("权重已更新并重算综合分", result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("全局权重更新失败", e);
            return ApiResponse.error("更新失败: " + e.getMessage());
        }
    }
}
