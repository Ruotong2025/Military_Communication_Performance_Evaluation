package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.entity.DynamicQlAggregation;
import com.ccnu.military.service.DynamicQlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 动态定性评估控制器
 * 完全动态化：指标结构来自 dynamic_template + dynamic_dimension
 */
@Slf4j
@RestController
@RequestMapping("/dynamic-ql")
@RequiredArgsConstructor
@Tag(name = "动态定性评估", description = "专家对定性指标的动态评估API接口")
public class DynamicQlController {

    private final DynamicQlService dynamicQlService;

    // ==================== 指标结构 ====================

    /**
     * 获取定性指标列表（按层级分组）
     * 选择模板后即可调用
     */
    @GetMapping("/indicators")
    @Operation(summary = "获取定性指标", description = "获取指定模板的定性指标列表，按层级分组")
    public ApiResponse<Map<String, Object>> getQualitativeIndicators(
            @Parameter(description = "模板ID") @RequestParam("templateId") Long templateId) {
        try {
            return ApiResponse.success("查询成功", dynamicQlService.getQualitativeIndicators(templateId));
        } catch (Exception e) {
            log.error("获取定性指标失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    /**
     * 获取所有层级名称
     */
    @GetMapping("/levels")
    @Operation(summary = "获取层级列表", description = "获取模板下的所有层级名称")
    public ApiResponse<List<String>> getLevelNames(
            @Parameter(description = "模板ID") @RequestParam("templateId") Long templateId) {
        try {
            return ApiResponse.success("查询成功", dynamicQlService.getLevelNames(templateId));
        } catch (Exception e) {
            log.error("获取层级列表失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    // ==================== 批次和作战管理 ====================

    /**
     * 获取可用的定量评估批次列表
     */
    @GetMapping("/batches")
    @Operation(summary = "获取批次列表", description = "获取可用的定量评估批次")
    public ApiResponse<List<Map<String, Object>>> getAvailableBatches(
            @Parameter(description = "模板ID") @RequestParam(value = "templateId", required = false) Long templateId) {
        try {
            return ApiResponse.success("查询成功", dynamicQlService.getAvailableBatches(templateId));
        } catch (Exception e) {
            log.error("获取批次列表失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    /**
     * 获取批次下的作战列表
     */
    @GetMapping("/operations")
    @Operation(summary = "获取作战列表", description = "获取指定批次下的作战ID列表")
    public ApiResponse<List<String>> getOperations(
            @Parameter(description = "批次ID") @RequestParam("batchId") String batchId) {
        try {
            return ApiResponse.success("查询成功", dynamicQlService.getOperationsByBatch(batchId));
        } catch (Exception e) {
            log.error("获取作战列表失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    /**
     * 获取作战的定量参考数据
     */
    @GetMapping("/reference")
    @Operation(summary = "获取参考数据", description = "获取作战的定量评估参考数据")
    public ApiResponse<Map<String, Object>> getReferenceData(
            @Parameter(description = "批次ID") @RequestParam("batchId") String batchId,
            @Parameter(description = "作战ID") @RequestParam("operationId") String operationId) {
        try {
            return ApiResponse.success("查询成功", dynamicQlService.getReferenceData(batchId, operationId));
        } catch (Exception e) {
            log.error("获取参考数据失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    // ==================== 专家管理 ====================

    /**
     * 获取可选专家列表
     */
    @GetMapping("/experts")
    @Operation(summary = "获取专家列表", description = "获取可选的专家列表（含可信度评分）")
    public ApiResponse<List<Map<String, Object>>> getAvailableExperts() {
        try {
            return ApiResponse.success("查询成功", dynamicQlService.getAvailableExperts());
        } catch (Exception e) {
            log.error("获取专家列表失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    /**
     * 批量获取专家可信度
     */
    @GetMapping("/experts/credibility")
    @Operation(summary = "获取专家可信度", description = "批量获取指定专家的可信度评分")
    public ApiResponse<Map<Long, Double>> getExpertsCredibility(
            @Parameter(description = "专家ID列表") @RequestParam("expertIds") List<Long> expertIds) {
        try {
            return ApiResponse.success("查询成功", dynamicQlService.getExpertsCredibility(expertIds));
        } catch (Exception e) {
            log.error("获取专家可信度失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    // ==================== 评估记录 ====================

    /**
     * 获取表格数据（完整结构，用于前端渲染）
     */
    @GetMapping("/table-data")
    @Operation(summary = "获取表格数据", description = "获取完整的表格数据结构，用于前端渲染")
    public ApiResponse<Map<String, Object>> getTableData(
            @Parameter(description = "批次ID") @RequestParam(value = "batchId", required = false) String batchId,
            @Parameter(description = "模板ID") @RequestParam("templateId") Long templateId,
            @Parameter(description = "层级名称") @RequestParam(value = "levelName", required = false) String levelName,
            @Parameter(description = "专家ID列表") @RequestParam(value = "expertIds", required = false) List<Long> expertIds) {
        try {
            return ApiResponse.success("查询成功",
                    dynamicQlService.getTableData(batchId, templateId, levelName, expertIds));
        } catch (Exception e) {
            log.error("获取表格数据失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    /**
     * 获取已保存的评估记录
     */
    @GetMapping("/records")
    @Operation(summary = "获取评估记录", description = "获取已保存的评估记录")
    public ApiResponse<Map<String, Object>> getEvaluationRecords(
            @Parameter(description = "批次ID") @RequestParam(value = "batchId", required = false) String batchId,
            @Parameter(description = "专家ID") @RequestParam(value = "expertId", required = false) Long expertId,
            @Parameter(description = "作战ID") @RequestParam(value = "operationId", required = false) String operationId) {
        try {
            return ApiResponse.success("查询成功",
                    dynamicQlService.getEvaluationRecords(batchId, expertId, operationId));
        } catch (Exception e) {
            log.error("获取评估记录失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    /**
     * 保存评估记录
     */
    @PostMapping("/records")
    @Operation(summary = "保存评估记录", description = "保存专家对作战的定性指标评估")
    public ApiResponse<Void> saveEvaluation(@RequestBody Map<String, Object> request) {
        try {
            String batchId = (String) request.get("batchId");
            Long templateId = Long.valueOf(request.get("templateId").toString());
            Long expertId = Long.valueOf(request.get("expertId").toString());
            String operationId = (String) request.get("operationId");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> scores = (List<Map<String, Object>>) request.get("scores");

            dynamicQlService.saveEvaluation(batchId, templateId, expertId, operationId, scores);
            return ApiResponse.success("保存成功", null);
        } catch (Exception e) {
            log.error("保存评估记录失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 批量保存评估记录
     */
    @PostMapping("/records/batch")
    @Operation(summary = "批量保存评估记录", description = "批量保存多条评估记录")
    public ApiResponse<Map<String, Object>> saveEvaluationBatch(@RequestBody List<Map<String, Object>> records) {
        try {
            int savedCount = 0;
            for (Map<String, Object> record : records) {
                String batchId = (String) record.get("batchId");
                Long templateId = Long.valueOf(record.get("templateId").toString());
                Long expertId = Long.valueOf(record.get("expertId").toString());
                String operationId = (String) record.get("operationId");

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> scores = (List<Map<String, Object>>) record.get("scores");

                dynamicQlService.saveEvaluation(batchId, templateId, expertId, operationId, scores);
                savedCount++;
            }

            return ApiResponse.success("保存成功", Map.of("savedCount", savedCount));
        } catch (Exception e) {
            log.error("批量保存评估记录失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    // ==================== 集结计算 ====================

    /**
     * 执行集结计算
     */
    @PostMapping("/aggregate")
    @Operation(summary = "执行集结计算", description = "对多位专家的评分进行集结计算")
    public ApiResponse<Map<String, Object>> aggregateScores(@RequestBody Map<String, Object> request) {
        try {
            String batchId = (String) request.get("batchId");
            String operationId = (String) request.getOrDefault("operationId", null);
            String levelName = (String) request.getOrDefault("levelName", null);

            double weightAlpha = 0.5;
            double weightLambda = 0.5;

            if (request.containsKey("weightAlpha")) {
                weightAlpha = ((Number) request.get("weightAlpha")).doubleValue();
            }
            if (request.containsKey("weightLambda")) {
                weightLambda = ((Number) request.get("weightLambda")).doubleValue();
            }

            Map<String, Object> result = dynamicQlService.aggregateScores(
                    batchId, operationId, levelName, weightAlpha, weightLambda);

            if ((Boolean) result.getOrDefault("success", false)) {
                return ApiResponse.success("集结计算成功", result);
            } else {
                return ApiResponse.error(400, (String) result.getOrDefault("message", "集结计算失败"));
            }
        } catch (Exception e) {
            log.error("集结计算失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 获取集结结果
     */
    @GetMapping("/aggregation")
    @Operation(summary = "获取集结结果", description = "获取已保存的集结计算结果")
    public ApiResponse<List<DynamicQlAggregation>> getAggregationResults(
            @Parameter(description = "批次ID") @RequestParam("batchId") String batchId,
            @Parameter(description = "作战ID") @RequestParam(value = "operationId", required = false) String operationId) {
        try {
            return ApiResponse.success("查询成功",
                    dynamicQlService.getAggregationResults(batchId, operationId));
        } catch (Exception e) {
            log.error("获取集结结果失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    // ==================== 会话管理 ====================

    /**
     * 创建评估会话
     */
    @PostMapping("/session")
    @Operation(summary = "创建会话", description = "创建新的定性评估会话")
    public ApiResponse<Map<String, Object>> createSession(@RequestBody Map<String, Object> request) {
        try {
            Long templateId = Long.valueOf(request.get("templateId").toString());
            String batchId = (String) request.getOrDefault("batchId", null);

            @SuppressWarnings("unchecked")
            List<Long> expertIds = request.containsKey("expertIds") ?
                    (List<Long>) request.get("expertIds") : null;

            Map<String, Object> result = dynamicQlService.createSession(templateId, batchId, expertIds);
            return ApiResponse.success("创建成功", result);
        } catch (Exception e) {
            log.error("创建会话失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/session/{sessionId}")
    @Operation(summary = "获取会话详情", description = "获取指定会话的详细信息")
    public ApiResponse<Map<String, Object>> getSessionDetail(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        try {
            return ApiResponse.success("查询成功", dynamicQlService.getSessionDetail(sessionId));
        } catch (Exception e) {
            log.error("获取会话详情失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }
}
