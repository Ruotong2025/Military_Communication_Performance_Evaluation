package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.service.DynamicQtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 动态定量评估控制器
 * 完全动态化的定量评估系统，根据指标模板配置动态生成Tab和表格
 */
@Slf4j
@RestController
@RequestMapping("/dynamic-qt")
@RequiredArgsConstructor
@Tag(name = "动态定量评估", description = "完全动态化的定量评估API接口")
public class DynamicQtController {

    private final DynamicQtService dynamicQtService;

    /**
     * 获取动态评估批次列表
     */
    @GetMapping("/batches")
    @Operation(summary = "获取批次列表", description = "获取所有动态定量评估批次")
    public ApiResponse<List<Map<String, Object>>> getBatches() {
        try {
            return ApiResponse.success("查询成功", dynamicQtService.getBatches());
        } catch (Exception e) {
            log.error("获取批次列表失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    /**
     * 创建新批次（只创建批次，不创建记录）
     */
    @PostMapping("/batch")
    @Operation(summary = "创建批次", description = "根据模板创建新批次")
    public ApiResponse<Map<String, Object>> createBatch(@RequestBody Map<String, Object> request) {
        try {
            Long templateId = Long.valueOf(request.get("templateId").toString());
            String description = (String) request.getOrDefault("description", "");

            Map<String, Object> result = dynamicQtService.createBatch(templateId, description);
            return ApiResponse.success("创建成功", result);
        } catch (Exception e) {
            log.error("创建批次失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 获取批次的定量指标列表（按层级分组）
     */
    @GetMapping("/indicators")
    @Operation(summary = "获取指标列表", description = "获取指定批次的定量指标列表，按层级分组")
    public ApiResponse<Map<String, Object>> getIndicators(
            @Parameter(description = "批次ID") @RequestParam("batchId") String batchId) {
        try {
            return ApiResponse.success("查询成功", dynamicQtService.getIndicatorsByBatch(batchId));
        } catch (Exception e) {
            log.error("获取指标列表失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    /**
     * 获取评估记录（转置表格数据）
     * 行=作战ID，列=指标名称
     */
    @GetMapping("/records")
    @Operation(summary = "获取评估记录", description = "获取批次的评估记录，表格转置：行=作战ID，列=指标")
    public ApiResponse<Map<String, Object>> getRecords(
            @Parameter(description = "批次ID") @RequestParam("batchId") String batchId) {
        try {
            return ApiResponse.success("查询成功", dynamicQtService.getRecords(batchId));
        } catch (Exception e) {
            log.error("获取评估记录失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    /**
     * 全局模拟：根据指定次数生成作战数据
     */
    @PostMapping("/global-simulate")
    @Operation(summary = "全局模拟", description = "生成指定次数的作战数据")
    public ApiResponse<Map<String, Object>> globalSimulate(@RequestBody Map<String, Object> request) {
        try {
            String batchId = (String) request.get("batchId");
            Integer count = ((Number) request.get("count")).intValue();

            if (count == null || count <= 0) {
                return ApiResponse.error(400, "请输入有效的生成次数");
            }

            Map<String, Object> result = dynamicQtService.globalSimulate(batchId, count);
            return ApiResponse.success("模拟成功", result);
        } catch (Exception e) {
            log.error("全局模拟失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 模拟单个单元格值
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
     * 批量模拟
     */
    @PostMapping("/batch-simulate")
    @Operation(summary = "批量模拟", description = "批量生成随机模拟值")
    public ApiResponse<Map<String, Object>> batchSimulate(@RequestBody Map<String, Object> request) {
        try {
            String batchId = (String) request.get("batchId");
            @SuppressWarnings("unchecked")
            List<Map<String, String>> cells = (List<Map<String, String>>) request.get("cells");

            Map<String, Double> results = dynamicQtService.batchSimulate(batchId, cells);
            return ApiResponse.success("批量模拟成功", Map.of("results", results));
        } catch (Exception e) {
            log.error("批量模拟失败", e);
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

    /**
     * 批量保存记录
     */
    @PostMapping("/records")
    @Operation(summary = "批量保存记录", description = "批量保存评估记录")
    public ApiResponse<Map<String, Object>> saveRecords(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> records = (List<Map<String, Object>>) request.get("records");
            int count = dynamicQtService.saveRecords(records);
            return ApiResponse.success("保存成功", Map.of("count", count));
        } catch (Exception e) {
            log.error("批量保存失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 生成归一化得分
     */
    @PostMapping("/normalize")
    @Operation(summary = "生成归一化得分", description = "对批次内的数据进行归一化处理")
    public ApiResponse<Map<String, Object>> normalize(
            @Parameter(description = "批次ID") @RequestParam("batchId") String batchId) {
        try {
            Map<String, Object> result = dynamicQtService.normalize(batchId);
            return ApiResponse.success("归一化成功", result);
        } catch (Exception e) {
            log.error("归一化失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 获取归一化后的记录
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
     * 删除批次
     */
    @DeleteMapping("/batch")
    @Operation(summary = "删除批次", description = "删除指定的评估批次")
    public ApiResponse<Map<String, Object>> deleteBatch(
            @Parameter(description = "批次ID") @RequestParam("batchId") String batchId) {
        try {
            int count = dynamicQtService.deleteBatch(batchId);
            return ApiResponse.success("删除成功", Map.of("deleted", count));
        } catch (Exception e) {
            log.error("删除批次失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 获取可用的作战ID列表（所有作战ID）
     */
    @GetMapping("/operations")
    @Operation(summary = "获取作战ID列表", description = "获取系统中可用的作战ID列表")
    public ApiResponse<List<Map<String, Object>>> getOperations() {
        try {
            return ApiResponse.success("查询成功", dynamicQtService.getAvailableOperations());
        } catch (Exception e) {
            log.error("获取作战ID失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    /**
     * 获取批次已有的作战ID列表
     */
    @GetMapping("/batch/operations")
    @Operation(summary = "获取批次作战ID", description = "获取指定批次已有的作战ID列表")
    public ApiResponse<List<String>> getBatchOperations(
            @Parameter(description = "批次ID") @RequestParam("batchId") String batchId) {
        try {
            return ApiResponse.success("查询成功", dynamicQtService.getBatchOperations(batchId));
        } catch (Exception e) {
            log.error("获取批次作战ID失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }
}
