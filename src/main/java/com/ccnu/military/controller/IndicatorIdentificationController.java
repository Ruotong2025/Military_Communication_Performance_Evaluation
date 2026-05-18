package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.dto.IndicatorBatchParseRequestDTO;
import com.ccnu.military.dto.IndicatorBatchParseResultDTO;
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

            log.info("开始批量识别 {} 个指标", request.getIndicators().size());

            IndicatorBatchParseResultDTO result = indicatorService.batchParseAndIdentify(request);

            log.info("批量识别完成，共 {} 个，MySQL命中 {} 个，API新识别 {} 个",
                    result.getTotalCount(), result.getSkippedCount(), result.getSuccessCount());

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
