package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.service.DynamicQtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 动态定量评估控制器
 * 核心流程：
 * 1. 选择模板 -> 新建批次 或 选择已有批次
 * 2. 全局模拟 -> 生成作战数据（覆盖/新增模式）
 * 3. 执行归一化 -> 保存归一化结果
 */
@Slf4j
@RestController
@RequestMapping("/dynamic-qt")
@RequiredArgsConstructor
@Tag(name = "动态定量评估", description = "完全动态化的定量评估API接口")
public class DynamicQtController {

    private final DynamicQtService dynamicQtService;

    // ==================== 评估批次管理 ====================

    /**
     * 获取所有评估批次列表（带统计）
     */
    @GetMapping("/batches")
    @Operation(summary = "获取批次列表", description = "获取所有动态定量评估批次")
    public ApiResponse<List<Map<String, Object>>> getBatches() {
        try {
            return ApiResponse.success("查询成功", dynamicQtService.getEvaluationBatches());
        } catch (Exception e) {
            log.error("获取批次列表失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    /**
     * 根据模板ID获取批次列表
     */
    @GetMapping("/batches/by-template")
    @Operation(summary = "按模板获取批次", description = "获取指定模板下的所有批次")
    public ApiResponse<List<Map<String, Object>>> getBatchesByTemplate(
            @Parameter(description = "模板ID") @RequestParam("templateId") Long templateId) {
        try {
            return ApiResponse.success("查询成功", dynamicQtService.getBatchesByTemplate(templateId));
        } catch (Exception e) {
            log.error("按模板获取批次列表失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    /**
     * 创建评估批次
     */
    @PostMapping("/batch")
    @Operation(summary = "创建批次", description = "根据模板创建新批次（空批次，无数据）")
    public ApiResponse<Map<String, Object>> createBatch(@RequestBody Map<String, Object> request) {
        try {
            Long templateId = Long.valueOf(request.get("templateId").toString());
            String description = (String) request.getOrDefault("description", "");

            Map<String, Object> result = dynamicQtService.createEvaluationBatch(templateId, description);
            return ApiResponse.success("创建成功", result);
        } catch (Exception e) {
            log.error("创建批次失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 删除评估批次
     */
    @DeleteMapping("/batch/{batchId}")
    @Operation(summary = "删除批次", description = "删除指定的评估批次（包括所有原始和归一化数据）")
    public ApiResponse<Void> deleteBatch(
            @Parameter(description = "批次ID") @PathVariable String batchId) {
        try {
            dynamicQtService.deleteEvaluationBatch(batchId);
            return ApiResponse.success("删除成功", null);
        } catch (Exception e) {
            log.error("删除批次失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 获取批次信息
     */
    @GetMapping("/batch/info/{batchId}")
    @Operation(summary = "获取批次信息", description = "获取指定批次的统计信息")
    public ApiResponse<Map<String, Object>> getBatchInfo(
            @Parameter(description = "批次ID") @PathVariable String batchId) {
        try {
            int operationCount = dynamicQtService.getOperationCount(batchId);
            boolean hasNormalization = dynamicQtService.hasNormalization(batchId);
            String normalizationName = dynamicQtService.getNormalizationName(batchId);

            Map<String, Object> info = new java.util.LinkedHashMap<>();
            info.put("batchId", batchId);
            info.put("operationCount", operationCount);
            info.put("hasNormalization", hasNormalization);
            info.put("normalizationName", normalizationName);

            return ApiResponse.success("查询成功", info);
        } catch (Exception e) {
            log.error("获取批次信息失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    // ==================== 原始数据管理 ====================

    /**
     * 获取批次的定量指标列表（按层级分组）
     */
    @GetMapping("/indicators")
    @Operation(summary = "获取指标列表", description = "获取指定批次的定量指标列表")
    public ApiResponse<Map<String, Object>> getIndicators(
            @Parameter(description = "批次ID") @RequestParam("batchId") String batchId,
            @Parameter(description = "模板ID") @RequestParam(value = "templateId", required = false) Long templateId) {
        try {
            return ApiResponse.success("查询成功", dynamicQtService.getIndicatorsByBatch(batchId, templateId));
        } catch (Exception e) {
            log.error("获取指标列表失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    /**
     * 获取原始评估数据
     */
    @GetMapping("/records")
    @Operation(summary = "获取原始数据", description = "获取批次的原始评估数据")
    public ApiResponse<Map<String, Object>> getRecords(
            @Parameter(description = "批次ID") @RequestParam("batchId") String batchId) {
        try {
            return ApiResponse.success("查询成功", dynamicQtService.getEvaluationRecords(batchId));
        } catch (Exception e) {
            log.error("获取评估记录失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    /**
     * 全局模拟
     * @param mode 模式: COVER-覆盖当前批次, APPEND-追加到当前批次
     * @param dispersion 离散度（0-1），表示数据围绕平均值的波动范围
     * @param dispersions 可选的各指标独立离散度，格式: {指标code: 离散度}
     */
    @PostMapping("/global-simulate")
    @Operation(summary = "全局模拟", description = "生成指定次数的作战数据，支持覆盖或追加模式")
    public ApiResponse<Map<String, Object>> globalSimulate(@RequestBody Map<String, Object> request) {
        try {
            String batchId = (String) request.get("batchId");
            Integer count = ((Number) request.get("count")).intValue();
            Long templateId = Long.valueOf(request.get("templateId").toString());
            String templateName = (String) request.get("templateName");
            String mode = (String) request.getOrDefault("mode", "APPEND");
            
            // 获取全局离散度参数，默认0.2
            Double dispersion = 0.2;
            if (request.containsKey("dispersion")) {
                dispersion = ((Number) request.get("dispersion")).doubleValue();
            }
            
            // 获取各指标独立离散度（可选）
            Map<String, Double> dispersions = null;
            if (request.containsKey("dispersions")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dispMap = (Map<String, Object>) request.get("dispersions");
                dispersions = new java.util.LinkedHashMap<>();
                for (Map.Entry<String, Object> entry : dispMap.entrySet()) {
                    dispersions.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
                }
            }

            if (count == null || count <= 0) {
                return ApiResponse.error(400, "请输入有效的生成次数");
            }

            if (dispersion < 0 || dispersion > 1) {
                return ApiResponse.error(400, "离散度应在0-1之间");
            }

            Map<String, Object> result;
            if (dispersions != null && !dispersions.isEmpty()) {
                // 使用独立离散度
                result = dynamicQtService.globalSimulateWithDispersions(batchId, templateId, templateName, count, mode, dispersions);
            } else {
                // 使用全局离散度
                result = dynamicQtService.globalSimulate(batchId, templateId, templateName, count, mode, dispersion);
            }
            return ApiResponse.success("模拟成功", result);
        } catch (Exception e) {
            log.error("全局模拟失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 获取定量指标统计信息（用于模拟配置）
     * 返回各指标的名称、平均值等
     */
    @GetMapping("/indicator-stats")
    @Operation(summary = "获取指标统计", description = "获取定量指标的统计信息，用于模拟配置")
    public ApiResponse<Map<String, Object>> getIndicatorStats(
            @Parameter(description = "模板ID") @RequestParam("templateId") Long templateId) {
        try {
            return ApiResponse.success("查询成功", dynamicQtService.getIndicatorStats(templateId));
        } catch (Exception e) {
            log.error("获取指标统计失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    /**
     * 模拟单个单元格
     */
    @PostMapping("/simulate")
    @Operation(summary = "模拟单元格", description = "生成0~1的随机模拟值")
    public ApiResponse<Map<String, Object>> simulateCell(@RequestBody Map<String, Object> request) {
        try {
            String batchId = (String) request.get("batchId");
            String operationId = (String) request.get("operationId");
            String secondaryCode = (String) request.get("secondaryCode");
            Double value = dynamicQtService.simulateCell(batchId, operationId, secondaryCode);
            return ApiResponse.success("模拟成功", Map.of("value", value));
        } catch (Exception e) {
            log.error("模拟单元格失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 保存单条记录
     */
    @PostMapping("/record")
    @Operation(summary = "保存记录", description = "保存单条评估记录")
    public ApiResponse<Void> saveRecord(@RequestBody Map<String, Object> request) {
        try {
            dynamicQtService.saveRecord(request);
            return ApiResponse.success("保存成功", null);
        } catch (Exception e) {
            log.error("保存记录失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    // ==================== 归一化管理 ====================

    /**
     * 执行归一化（直接覆盖当前批次的归一化结果）
     */
    @PostMapping("/normalize")
    @Operation(summary = "执行归一化", description = "对当前批次的原始数据执行归一化，结果会覆盖之前的归一化")
    public ApiResponse<Map<String, Object>> normalize(@RequestBody Map<String, Object> request) {
        try {
            String batchId = (String) request.get("batchId");
            if (batchId == null || batchId.isBlank()) {
                return ApiResponse.error(400, "批次ID不能为空");
            }
            Map<String, Object> result = dynamicQtService.createNormalization(batchId);
            if ((Boolean) result.getOrDefault("success", false)) {
                return ApiResponse.success("归一化成功", result);
            } else {
                return ApiResponse.error(400, (String) result.getOrDefault("message", "归一化失败"));
            }
        } catch (Exception e) {
            log.error("归一化失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 删除归一化结果
     */
    @DeleteMapping("/normalization/{batchId}")
    @Operation(summary = "删除归一化", description = "删除指定批次的归一化结果")
    public ApiResponse<Void> deleteNormalization(
            @Parameter(description = "批次ID") @PathVariable String batchId) {
        try {
            dynamicQtService.deleteNormalization(batchId);
            return ApiResponse.success("删除成功", null);
        } catch (Exception e) {
            log.error("删除归一化失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 获取归一化结果数据
     */
    @GetMapping("/normalization-records")
    @Operation(summary = "获取归一化结果", description = "获取指定批次的归一化结果数据")
    public ApiResponse<Map<String, Object>> getNormalizationRecords(
            @Parameter(description = "批次ID") @RequestParam("batchId") String batchId) {
        try {
            return ApiResponse.success("查询成功", dynamicQtService.getNormalizationRecords(batchId));
        } catch (Exception e) {
            log.error("获取归一化记录失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    /**
     * 检查是否存在归一化结果
     */
    @GetMapping("/normalization/exists/{batchId}")
    @Operation(summary = "检查归一化", description = "检查指定批次是否有归一化结果")
    public ApiResponse<Map<String, Object>> hasNormalization(
            @Parameter(description = "批次ID") @PathVariable String batchId) {
        try {
            boolean hasNorm = dynamicQtService.hasNormalization(batchId);
            String normName = dynamicQtService.getNormalizationName(batchId);
            return ApiResponse.success("查询成功", Map.of(
                    "hasNormalization", hasNorm,
                    "normalizationName", normName != null ? normName : ""
            ));
        } catch (Exception e) {
            log.error("检查归一化失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    // ==================== 兼容旧API ====================

    /**
     * 批量模拟（兼容旧API）
     */
    @PostMapping("/batch-simulate")
    @Operation(summary = "批量模拟", description = "批量生成随机模拟值")
    public ApiResponse<Map<String, Object>> batchSimulate(@RequestBody Map<String, Object> request) {
        try {
            String batchId = (String) request.get("batchId");
            @SuppressWarnings("unchecked")
            List<Map<String, String>> cells = (List<Map<String, String>>) request.get("cells");

            Map<String, Double> results = new java.util.LinkedHashMap<>();
            for (Map<String, String> cell : cells) {
                String operationId = cell.get("operationId");
                String secondaryCode = cell.get("secondaryCode");
                Double value = dynamicQtService.simulateCell(batchId, operationId, secondaryCode);
                results.put(operationId + "_" + secondaryCode, value);
            }
            return ApiResponse.success("批量模拟成功", Map.of("results", results));
        } catch (Exception e) {
            log.error("批量模拟失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 批量保存记录（兼容旧API）
     */
    @PostMapping("/records")
    @Operation(summary = "批量保存记录", description = "批量保存评估记录")
    public ApiResponse<Map<String, Object>> saveRecords(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> records = (List<Map<String, Object>>) request.get("records");
            for (Map<String, Object> record : records) {
                dynamicQtService.saveRecord(record);
            }
            return ApiResponse.success("保存成功", Map.of("count", records.size()));
        } catch (Exception e) {
            log.error("批量保存失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 获取归一化后的记录（兼容旧API）
     */
    @GetMapping("/normalized-records")
    @Operation(summary = "获取归一化记录", description = "获取归一化后的评估记录")
    public ApiResponse<Map<String, Object>> getNormalizedRecords(
            @Parameter(description = "批次ID") @RequestParam("batchId") String batchId) {
        try {
            return ApiResponse.success("查询成功", dynamicQtService.getNormalizedRecords(batchId));
        } catch (Exception e) {
            log.error("获取归一化记录失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    /**
     * 创建归一化（兼容旧API）
     */
    @PostMapping("/normalize-legacy")
    @Operation(summary = "创建归一化(旧)", description = "兼容旧API，内部调用新的归一化逻辑")
    public ApiResponse<Map<String, Object>> createNormalization(@RequestBody Map<String, Object> request) {
        try {
            String batchId = (String) request.get("batchId");
            Map<String, Object> result = dynamicQtService.createNormalization(batchId);
            return ApiResponse.success("归一化成功", result);
        } catch (Exception e) {
            log.error("归一化失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 获取归一化批次列表（兼容旧API）
     */
    @GetMapping("/normalization-batches")
    @Operation(summary = "获取归一化批次列表", description = "兼容旧API")
    public ApiResponse<List<Map<String, Object>>> getNormalizationBatches(
            @Parameter(description = "批次ID") @RequestParam("batchId") String batchId) {
        try {
            return ApiResponse.success("查询成功", dynamicQtService.getNormalizationBatches(batchId));
        } catch (Exception e) {
            log.error("获取归一化批次列表失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }
}
