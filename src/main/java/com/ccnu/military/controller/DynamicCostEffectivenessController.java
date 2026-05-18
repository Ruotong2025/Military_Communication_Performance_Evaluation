package com.ccnu.military.controller;

import com.ccnu.military.dto.*;
import com.ccnu.military.entity.DynamicCostBatch;
import com.ccnu.military.entity.DynamicCostTemplate;
import com.ccnu.military.service.DynamicCostEffectivenessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/dynamic-cost")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class DynamicCostEffectivenessController {

    private final DynamicCostEffectivenessService service;
    private final JdbcTemplate jdbcTemplate;

    // ==================== 模板管理 ====================

    @PostMapping("/template/save")
    public ApiResponse<DynamicCostTemplate> saveTemplate(
            @RequestBody DynamicCostTemplateSaveRequest request) {
        DynamicCostTemplate template = service.saveTemplate(request);
        return ApiResponse.success("保存成功", template);
    }

    @GetMapping("/template/list")
    public ApiResponse<List<DynamicCostTemplateVO>> getTemplateList() {
        List<DynamicCostTemplateVO> list = service.getTemplateList();
        return ApiResponse.success("获取成功", list);
    }

    @GetMapping("/template/{id}")
    public ApiResponse<DynamicCostTemplate> getTemplateById(@PathVariable Long id) {
        DynamicCostTemplate template = service.getTemplateById(id);
        return ApiResponse.success("获取成功", template);
    }

    @DeleteMapping("/template/{id}")
    public ApiResponse<Boolean> deleteTemplate(@PathVariable Long id) {
        service.deleteTemplate(id);
        return ApiResponse.success("删除成功", true);
    }

    // ==================== 批次管理 ====================

    @GetMapping("/batch/list")
    public ApiResponse<List<DynamicCostBatch>> getBatchList(@RequestParam Long templateId) {
        List<DynamicCostBatch> list = service.getBatchList(templateId);
        return ApiResponse.success("获取成功", list);
    }

    @GetMapping("/batch/{id}")
    public ApiResponse<DynamicCostBatchDetailVO> getBatchDetail(@PathVariable Long id) {
        DynamicCostBatchDetailVO detail = service.getBatchDetail(id);
        return ApiResponse.success("获取成功", detail);
    }

    @DeleteMapping("/batch/{id}")
    public ApiResponse<Boolean> deleteBatch(@PathVariable Long id) {
        service.deleteBatch(id);
        return ApiResponse.success("删除成功", true);
    }

    // ==================== 模拟计算 ====================

    @PostMapping("/simulate")
    public ApiResponse<DynamicCostSimulationResult> runSimulation(
            @RequestBody DynamicCostSimulationRequest request) {
        DynamicCostSimulationResult result = service.runSimulation(request);
        return ApiResponse.success("模拟完成", result);
    }

    // ==================== 原数据来源（效费分析）====================

    /**
     * 获取 dynamic_qt_record 中的模板列表
     */
    @GetMapping("/qt/template-list")
    public ApiResponse<List<Map<String, Object>>> getQtTemplateList() {
        List<Map<String, Object>> list = service.getQtTemplateList();
        return ApiResponse.success("获取成功", list);
    }

    /**
     * 根据模板ID获取批次列表
     */
    @GetMapping("/qt/batch-list")
    public ApiResponse<List<Map<String, Object>>> getQtBatchList(@RequestParam Long templateId) {
        List<Map<String, Object>> list = service.getQtBatchListByTemplate(templateId);
        return ApiResponse.success("获取成功", list);
    }

    /**
     * 获取作战ID列表
     */
    @GetMapping("/qt/operation-list")
    public ApiResponse<List<String>> getQtOperationList(@RequestParam String batchId) {
        List<String> list = service.getQtOperationIds(batchId);
        return ApiResponse.success("获取成功", list);
    }

    /**
     * 获取效费分析数据
     */
    @GetMapping("/qt/effectiveness-data")
    public ApiResponse<Map<String, Object>> getEffectivenessData(
            @RequestParam Long templateId,
            @RequestParam String batchId) {
        log.info("【效费分析】获取效费分析数据, templateId={}, batchId={}", templateId, batchId);
        try {
            Map<String, Object> data = service.getEffectivenessData(templateId, batchId);
            log.info("【效费分析】获取效费分析数据成功, operationIds数量={}", data.containsKey("operationIds") ? ((List<?>)data.get("operationIds")).size() : 0);
            return ApiResponse.success("获取成功", data);
        } catch (Exception e) {
            log.error("【效费分析】获取效费分析数据失败, templateId={}, batchId={}, error={}", templateId, batchId, e.getMessage(), e);
            return ApiResponse.error(500, "获取效费分析数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取批次下的所有作战记录（原始数据）
     */
    @GetMapping("/qt/records")
    public ApiResponse<List<Map<String, Object>>> getQtRecords(
            @RequestParam String batchId) {
        log.info("【效费分析】获取批次作战记录, batchId={}", batchId);
        String sql = "SELECT * FROM dynamic_qt_record WHERE batch_id = ? ORDER BY operation_id, secondary_code";
        List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, batchId);
        log.info("【效费分析】获取批次作战记录成功, 数量={}", records.size());
        return ApiResponse.success("获取成功", records);
    }
}
