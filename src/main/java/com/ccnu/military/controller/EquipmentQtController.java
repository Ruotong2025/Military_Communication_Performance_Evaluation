package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.service.EquipmentQtCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 装备操作定量指标计算控制器
 */
@RestController
@RequestMapping("/equipment/qt")
@RequiredArgsConstructor
public class EquipmentQtController {

    private final EquipmentQtCalculationService calculationService;

    /**
     * 获取定量指标配置列表
     */
    @GetMapping("/indicators")
    public ApiResponse<List<Map<String, Object>>> getIndicators() {
        return ApiResponse.success("获取成功", calculationService.getIndicatorDefs());
    }

    /**
     * 计算定量指标
     */
    @PostMapping("/calculate")
    public ApiResponse<Map<String, Object>> calculate(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<?> rawIds = (List<?>) request.get("operationIds");
        if (rawIds == null || rawIds.isEmpty()) {
            return ApiResponse.error(400, "operationIds 不能为空，请先选择作战或生成模拟数据");
        }
        List<Long> operationIds = rawIds.stream()
                .map(o -> o instanceof Number ? ((Number) o).longValue() : Long.parseLong(o.toString()))
                .toList();
        String batchId = (String) request.get("evaluationBatchId");
        Map<String, Object> result = calculationService.calculateForOperations(operationIds, batchId);
        Boolean success = (Boolean) result.get("success");
        if (Boolean.TRUE.equals(success)) {
            return ApiResponse.success((String) result.get("message"), result);
        }
        return ApiResponse.error(500, (String) result.get("message"));
    }

    /**
     * 生成归一化得分
     */
    @PostMapping("/normalize")
    public ApiResponse<Map<String, Object>> normalize(@RequestParam("evaluationBatchId") String batchId) {
        Map<String, Object> result = calculationService.generateNormalizedScores(batchId);
        Boolean success = (Boolean) result.get("success");
        if (Boolean.TRUE.equals(success)) {
            return ApiResponse.success((String) result.get("message"), result);
        }
        return ApiResponse.error(500, (String) result.get("message"));
    }

    /**
     * 获取评估批次列表
     */
    @GetMapping("/batches")
    public ApiResponse<List<Map<String, Object>>> listBatches() {
        return ApiResponse.success("获取成功", calculationService.listBatches());
    }

    /**
     * 获取批次下的记录
     */
    @GetMapping("/records")
    public ApiResponse<List<Map<String, Object>>> getRecords(@RequestParam("evaluationBatchId") String batchId) {
        return ApiResponse.success("获取成功", calculationService.getRecordsByBatch(batchId));
    }

    /**
     * 删除批次
     */
    @DeleteMapping("/batches")
    public ApiResponse<Map<String, Object>> deleteBatch(@RequestParam("evaluationBatchId") String batchId) {
        int deleted = calculationService.deleteByBatch(batchId);
        if (deleted > 0) {
            return ApiResponse.success("删除成功", Map.of("deleted", deleted));
        }
        return ApiResponse.error(404, "未找到该批次");
    }
}
