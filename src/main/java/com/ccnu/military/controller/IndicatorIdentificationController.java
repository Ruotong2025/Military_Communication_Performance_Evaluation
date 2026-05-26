package com.ccnu.military.controller;

import com.ccnu.military.dto.*;
import com.ccnu.military.entity.IndicatorDefinition;
import com.ccnu.military.service.ColumnSearchService;
import com.ccnu.military.service.IndicatorIdentificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    private final ColumnSearchService columnSearchService;

    // ============================================
    // 字段相似度搜索 API
    // ============================================

    /**
     * 搜索相似字段
     * 根据数据源名称搜索数据库中的相似字段
     */
    @Operation(summary = "搜索相似字段", description = "根据数据源名称搜索数据库中的相似字段")
    @GetMapping("/source-data/column-suggestions")
    public ApiResponse<List<ColumnSimilarityResult>> searchColumnSuggestions(
            @Parameter(description = "数据源名称", example = "成功传输的数据量")
            @RequestParam String dataName,
            @Parameter(description = "返回数量", example = "5")
            @RequestParam(defaultValue = "5") int limit) {
        try {
            log.info("========================================");
            log.info("[调试] searchColumnSuggestions API 被调用");
            log.info("[调试] - dataName: {}", dataName);
            log.info("[调试] - limit: {}", limit);
            log.info("========================================");

            if (dataName == null || dataName.trim().isEmpty()) {
                return ApiResponse.error(400, "数据源名称不能为空");
            }
            List<ColumnSimilarityResult> results = columnSearchService.searchSimilarColumns(dataName.trim(), limit);
            log.info("[调试] searchColumnSuggestions 返回 {} 条结果", results.size());
            return ApiResponse.success(results);
        } catch (Exception e) {
            log.error("[调试] searchColumnSuggestions 异常: {}", e.getMessage(), e);
            return ApiResponse.error("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 根据指标详情获取字段建议
     * 结合API分析结果和数据库搜索
     */
    @Operation(summary = "获取指标数据源字段建议", description = "结合API分析结果和数据库搜索，获取数据源字段映射建议")
    @PostMapping("/source-data/field-suggestions")
    public ApiResponse<List<ColumnSimilarityResult>> getFieldSuggestions(
            @RequestBody IndicatorQueryRequest request) {
        try {
            if (request.getIndicatorName() == null || request.getIndicatorName().trim().isEmpty()) {
                return ApiResponse.error(400, "指标名称不能为空");
            }

            // 先通过API分析获取数据源
            IndicatorAnalysisResult apiResult = indicatorService.analyzeSingleIndicatorApi(request);

            // 从API结果中提取字段建议
            List<ColumnSimilarityResult> suggestions = columnSearchService.extractFromApiResult(apiResult);
            return ApiResponse.success(suggestions);
        } catch (Exception e) {
            log.error("获取字段建议失败", e);
            return ApiResponse.error("获取失败: " + e.getMessage());
        }
    }

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
            log.info("========================================");
            log.info("[调试] analyzeSingleIndicator API 被调用");
            log.info("[调试] - indicatorName: {}", request.getIndicatorName());
            log.info("[调试] - domain: {}", request.getDomain());
            log.info("========================================");

            if (request.getIndicatorName() == null || request.getIndicatorName().trim().isEmpty()) {
                return ApiResponse.error(400, "指标名称不能为空");
            }

            IndicatorAnalysisResult result = indicatorService.analyzeSingleIndicatorApi(request);

            // 打印解析结果
            log.info("[调试] API 分析完成:");
            log.info("[调试] - indicatorName: {}", result.getIndicatorName());
            log.info("[调试] - indicatorType: {}", result.getIndicatorType());
            log.info("[调试] - formula: {}", result.getFormula());
            if (result.getSourceDataList() != null) {
                log.info("[调试] - sourceDataList 大小: {}", result.getSourceDataList().size());
                for (int i = 0; i < result.getSourceDataList().size(); i++) {
                    SourceDataDTO sd = result.getSourceDataList().get(i);
                    log.info("[调试]   [{}/{}] dataName: {}, formulaSymbol: {}",
                        i + 1, result.getSourceDataList().size(), sd.getSourceDataName(), sd.getFormulaSymbol());
                }
            } else {
                log.info("[调试] - sourceDataList: null");
            }
            log.info("========================================");

            return ApiResponse.success(result);

        } catch (Exception e) {
            log.error("[调试] analyzeSingleIndicator 异常: {}", e.getMessage(), e);
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
