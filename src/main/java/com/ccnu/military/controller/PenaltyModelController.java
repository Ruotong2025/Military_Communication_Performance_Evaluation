package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.dto.PenaltyModelResultDTO;
import com.ccnu.military.service.PenaltyModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 惩罚模型控制器
 * <p>负责保存和查询惩罚计算结果。
 * 注意: 惩罚模型参数在前端代码中默认写死，后端仅负责数据持久化。
 */
@Slf4j
@RestController
@RequestMapping("/evaluation/penalty")
@RequiredArgsConstructor
@Tag(name = "惩罚模型计算", description = "惩罚因子分析结果保存与查询")
public class PenaltyModelController {

    private final PenaltyModelService penaltyModelService;

    /**
     * 保存惩罚计算结果
     * <p>接收前端计算好的惩罚结果，直接保存到数据库。
     * 如果该批次已有结果，则覆盖。
     */
    @Operation(summary = "保存惩罚计算结果", description = "将前端计算好的惩罚结果保存到数据库")
    @PostMapping("/results")
    public ApiResponse<Void> savePenaltyResults(
            @RequestParam String evaluationId,
            @RequestBody List<PenaltyModelResultDTO> results) {
        try {
            if (evaluationId == null || evaluationId.trim().isEmpty()) {
                return ApiResponse.error(400, "评估批次ID不能为空");
            }
            if (results == null || results.isEmpty()) {
                return ApiResponse.error(400, "惩罚结果数据不能为空");
            }
            int savedCount = penaltyModelService.savePenaltyResults(evaluationId, results);
            return ApiResponse.success("保存成功，共 " + savedCount + " 条记录", null);
        } catch (Exception e) {
            log.error("保存惩罚结果失败", e);
            return ApiResponse.error("保存失败: " + e.getMessage());
        }
    }

    /**
     * 查询惩罚计算结果
     * <p>根据评估批次ID查询已保存的惩罚计算结果。
     */
    @Operation(summary = "查询惩罚计算结果", description = "根据评估批次ID查询已保存的惩罚计算结果")
    @GetMapping("/results")
    public ApiResponse<List<PenaltyModelResultDTO>> getPenaltyResults(
            @RequestParam String evaluationId) {
        try {
            if (evaluationId == null || evaluationId.trim().isEmpty()) {
                return ApiResponse.error(400, "评估批次ID不能为空");
            }
            List<PenaltyModelResultDTO> results = penaltyModelService.getPenaltyResults(evaluationId);
            return ApiResponse.success(results);
        } catch (Exception e) {
            log.error("查询惩罚结果失败", e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 检查指定批次是否有惩罚计算结果
     */
    @Operation(summary = "检查惩罚结果是否存在", description = "检查指定批次是否已有保存的惩罚计算结果")
    @GetMapping("/results/exists")
    public ApiResponse<Boolean> hasResults(@RequestParam String evaluationId) {
        try {
            boolean exists = penaltyModelService.hasResults(evaluationId);
            return ApiResponse.success(exists);
        } catch (Exception e) {
            log.error("检查惩罚结果失败", e);
            return ApiResponse.error("检查失败: " + e.getMessage());
        }
    }

    /**
     * 删除指定批次的惩罚计算结果
     */
    @Operation(summary = "删除惩罚计算结果", description = "删除指定评估批次的惩罚计算结果")
    @DeleteMapping("/results")
    public ApiResponse<Void> deletePenaltyResults(@RequestParam String evaluationId) {
        try {
            penaltyModelService.deletePenaltyResults(evaluationId);
            return ApiResponse.success("删除成功", null);
        } catch (Exception e) {
            log.error("删除惩罚结果失败", e);
            return ApiResponse.error("删除失败: " + e.getMessage());
        }
    }
}
