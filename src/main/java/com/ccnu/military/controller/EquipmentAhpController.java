package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.dto.ExpertAhpScoresPersistRequest;
import com.ccnu.military.dto.ExpertAhpSimulateRequest;
import com.ccnu.military.dto.MatrixCalculationRequest;
import com.ccnu.military.dto.MatrixCalculationResult;
import com.ccnu.military.entity.ExpertAhpComparisonScore;
import com.ccnu.military.service.EquipmentAhpScoreService;
import com.ccnu.military.service.EquipmentAhpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 装备操作 AHP 控制器
 * 与 ExpertAHPController 并列，按 comparison_key 前缀 "装备操作_" 隔离数据。
 */
@Slf4j
@RestController
@RequestMapping("/equipment/ahp")
@Tag(name = "装备操作AHP", description = "基于专家打分的装备操作AHP权重计算接口")
public class EquipmentAhpController {

    private final EquipmentAhpService equipmentAhpService;
    private final EquipmentAhpScoreService equipmentAhpScoreService;

    public EquipmentAhpController(EquipmentAhpService equipmentAhpService,
                                   EquipmentAhpScoreService equipmentAhpScoreService) {
        this.equipmentAhpService = equipmentAhpService;
        this.equipmentAhpScoreService = equipmentAhpScoreService;
    }

    @Operation(summary = "获取装备操作AHP矩阵结构定义",
            description = "从 equipment_qt_indicator_def / equipment_ql_indicator_def 读取维度与指标元数据，" +
                    "type=ql 只核定性，qt 只含定量，both 或 null 合并")
    @GetMapping("/meta")
    public ApiResponse<Map<String, Object>> getMeta(
            @RequestParam(value = "type", required = false) String type) {
        try {
            return ApiResponse.success(equipmentAhpService.getMeta(type));
        } catch (Exception e) {
            log.error("获取装备操作AHP元信息失败", e);
            return ApiResponse.error("获取元信息失败: " + e.getMessage());
        }
    }

    @Operation(summary = "计算装备操作AHP权重",
            description = "根据判断矩阵（上三角任意位置输入，自动生成下三角倒数）计算各层权重和一致性比率，" +
                    "comparison_key 必须带前缀 \"装备操作_\"")
    @PostMapping("/calculate")
    public ApiResponse<MatrixCalculationResult> calculateMatrix(@RequestBody MatrixCalculationRequest request) {
        try {
            log.info("收到装备操作AHP矩阵计算请求");
            MatrixCalculationResult result = equipmentAhpService.calculate(request);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("装备操作AHP矩阵计算失败", e);
            return ApiResponse.error("计算失败: " + e.getMessage());
        }
    }

    @Operation(summary = "查询专家装备操作AHP打分",
            description = "按 expert_id 读取带前缀 \"装备操作_\" 的比较打分记录")
    @GetMapping("/scores")
    public ApiResponse<List<ExpertAhpComparisonScore>> getScores(@RequestParam Long expertId) {
        try {
            List<ExpertAhpComparisonScore> list = equipmentAhpScoreService.listByExpert(expertId);
            return ApiResponse.success(list);
        } catch (Exception e) {
            log.error("查询装备操作AHP打分失败", e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "保存专家���备操作AHP打分",
            description = "覆盖写入该专家 comparison_key 前缀为 \"装备操作_\" 的全部比较对")
    @PostMapping("/scores")
    public ApiResponse<Integer> saveScores(@RequestBody ExpertAhpScoresPersistRequest request) {
        try {
            int n = equipmentAhpScoreService.persist(request);
            return ApiResponse.success(n);
        } catch (Exception e) {
            log.error("保存装备操作AHP打分失败", e);
            return ApiResponse.error("保存失败: " + e.getMessage());
        }
    }

    @Operation(summary = "批量模拟装备操作AHP打分",
            description = "为多名专家生成随机标度与把握度，只模拟 \"装备操作_\" 前缀数据")
    @PostMapping("/scores/simulate")
    public ApiResponse<Map<String, Object>> simulateScores(@RequestBody ExpertAhpSimulateRequest request) {
        try {
            Map<String, Object> result = equipmentAhpScoreService.simulate(request);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("模拟装备操作AHP打分失败", e);
            return ApiResponse.error("模拟失败: " + e.getMessage());
        }
    }
}
