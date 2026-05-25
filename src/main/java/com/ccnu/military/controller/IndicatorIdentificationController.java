package com.ccnu.military.controller;

import com.ccnu.military.dto.*;
import com.ccnu.military.entity.IndicatorDefinition;
import com.ccnu.military.service.IndicatorIdentificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 指标智能识别控制器
 */
@Slf4j
@RestController
@RequestMapping("/indicator")
@RequiredArgsConstructor
@Tag(name = "指标智能识别", description = "上传指标名称，AI自动识别定性/定量类型及计算公式")
public class IndicatorIdentificationController {

    private final IndicatorIdentificationService indicatorService;

    // ============================================
    // 新增接口：查询指标（情况一 + 情况三）
    // ============================================

    /**
     * 智能查询指标（新接口）
     * 返回 Top3 相似度候选 + 全量指标下拉列表
     */
    @Operation(summary = "智能查询指标", description = "返回Top3相似度候选和全部指标下拉列表")
    @PostMapping("/intelligent-query")
    public ApiResponse<IntelligentQueryResult> intelligentQuery(
            @RequestBody IndicatorQueryRequest request) {
        try {
            if (request.getIndicatorName() == null || request.getIndicatorName().trim().isEmpty()) {
                return ApiResponse.error(400, "指标名称不能为空");
            }

            IntelligentQueryResult result = indicatorService.intelligentQuery(request);
            return ApiResponse.success(result);

        } catch (Exception e) {
            log.error("智能查询失败", e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询指标
     * 返回数据库语义匹配结果和全部指标列表
     */
    @Operation(summary = "查询指标", description = "查询指标，返回语义匹配结果和全部指标下拉列表")
    @PostMapping("/query")
    public ApiResponse<IndicatorQueryResultDTO> queryIndicator(
            @RequestBody IndicatorQueryRequest request) {
        try {
            if (request.getIndicatorName() == null || request.getIndicatorName().trim().isEmpty()) {
                return ApiResponse.error(400, "指标名称不能为空");
            }

            IndicatorQueryResultDTO result = indicatorService.queryIndicator(request);
            return ApiResponse.success(result);

        } catch (Exception e) {
            log.error("查询指标失败", e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * API分析单个指标（情况二）
     */
    @Operation(summary = "API分析单个指标", description = "调用DeepSeek API分析单个指标")
    @PostMapping("/analyze-single")
    public ApiResponse<IndicatorAnalysisResult> analyzeSingleIndicator(
            @RequestBody IndicatorQueryRequest request) {
        try {
            if (request.getIndicatorName() == null || request.getIndicatorName().trim().isEmpty()) {
                return ApiResponse.error(400, "指标名称不能为空");
            }

            IndicatorAnalysisResult result = indicatorService.analyzeSingleIndicatorApi(request);
            return ApiResponse.success(result);

        } catch (Exception e) {
            log.error("API分析失败", e);
            return ApiResponse.error("API分析失败: " + e.getMessage());
        }
    }

    /**
     * 保存用户选择
     */
    @Operation(summary = "保存用户选择", description = "保存用户选择的指标结果")
    @PostMapping("/save-selection")
    public ApiResponse<IndicatorDefinition> saveSelection(
            @RequestBody IndicatorSelectionRequest request) {
        try {
            if (request.getOriginalName() == null || request.getOriginalName().trim().isEmpty()) {
                return ApiResponse.error(400, "指标名称不能为空");
            }

            IndicatorDefinition result = indicatorService.saveSelection(request);
            return ApiResponse.success("保存成功", result);

        } catch (Exception e) {
            log.error("保存失败", e);
            return ApiResponse.error("保存失败: " + e.getMessage());
        }
    }

    // ============================================
    // 原有接口
    // ============================================

    /**
     * 批量识别指标
     */
    @Operation(summary = "批量识别指标", description = "上传指标名称列表，AI一次性分析所有指标，先匹配MySQL再调用API")
    @PostMapping("/batch-parse")
    public ApiResponse<IndicatorBatchParseResultDTO> batchParseIndicators(
            @RequestBody IndicatorBatchParseRequestDTO request) {
        try {
            if (request.getIndicators() == null || request.getIndicators().isEmpty()) {
                return ApiResponse.error(400, "指标列表不能为空");
            }
            if (request.getIndicators().size() > 100) {
                return ApiResponse.error(400, "单次最多支持100个指标");
            }

            IndicatorBatchParseResultDTO result = indicatorService.batchParseAndIdentify(request);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("指标批量识别失败", e);
            return ApiResponse.error("识别失败: " + e.getMessage());
        }
    }

    /**
     * 获取指标列表
     */
    @Operation(summary = "获取指标列表", description = "获取所有已保存的指标定义")
    @GetMapping("/list")
    public ApiResponse<List<IndicatorDefinition>> getIndicatorList(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type) {
        try {
            List<IndicatorDefinition> list = indicatorService.getIndicators(category, type);
            return ApiResponse.success(list);
        } catch (Exception e) {
            log.error("获取指标列表失败", e);
            return ApiResponse.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取指标详情
     */
    @Operation(summary = "获取指标详情", description = "根据ID获取指标详细信息")
    @GetMapping("/{id}")
    public ApiResponse<IndicatorDefinition> getIndicatorById(@PathVariable Long id) {
        try {
            IndicatorDefinition indicator = indicatorService.getIndicatorById(id);
            return ApiResponse.success(indicator);
        } catch (Exception e) {
            log.error("获取指标详情失败", e);
            return ApiResponse.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 删除指标
     */
    @Operation(summary = "删除指标", description = "删除指定的指标定义")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteIndicator(@PathVariable Long id) {
        try {
            indicatorService.deleteIndicator(id);
            return ApiResponse.success("删除成功", null);
        } catch (Exception e) {
            log.error("删除指标失败", e);
            return ApiResponse.error("删除失败: " + e.getMessage());
        }
    }
}
