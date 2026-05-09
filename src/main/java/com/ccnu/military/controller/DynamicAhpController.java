package com.ccnu.military.controller;

import com.ccnu.military.dto.*;
import com.ccnu.military.entity.DynamicAhpMatrix;
import com.ccnu.military.entity.DynamicAhpWeights;
import com.ccnu.military.service.DynamicAhpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 动态AHP控制器
 * 支持动态指标体系的层次分析法权重配置
 */
@Slf4j
@RestController
@RequestMapping("/dynamic-ahp")
@RequiredArgsConstructor
@Tag(name = "动态AHP权重配置", description = "动态指标体系的AHP层次分析法权重配置")
public class DynamicAhpController {

    private final DynamicAhpService dynamicAhpService;

    @GetMapping("/templates")
    @Operation(summary = "获取指标模板列表", description = "获取所有已保存的动态指标模板")
    public ApiResponse<List<Map<String, Object>>> getTemplates() {
        log.info("API调用: 获取动态指标模板列表");
        try {
            List<Map<String, Object>> templates = dynamicAhpService.getIndicatorTemplates();
            return ApiResponse.success("查询成功", templates);
        } catch (Exception e) {
            log.error("获取模板列表失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @GetMapping("/tree")
    @Operation(summary = "获取指标树结构", description = "获取指定模板的完整指标树结构，用于展示层级关系")
    public ApiResponse<DynamicIndicatorTreeDTO> getIndicatorTree(
            @Parameter(description = "模板ID")
            @RequestParam(value = "templateId") Long templateId) {
        log.info("API调用: 获取指标树 - templateId={}", templateId);
        try {
            DynamicIndicatorTreeDTO tree = dynamicAhpService.getIndicatorTree(templateId);
            return ApiResponse.success("查询成功", tree);
        } catch (Exception e) {
            log.error("获取指标树失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @GetMapping("/experts")
    @Operation(summary = "获取专家列表", description = "获取所有可用于AHP打分的专家")
    public ApiResponse<List<Map<String, Object>>> getExperts() {
        log.info("API调用: 获取专家列表");
        try {
            List<Map<String, Object>> experts = dynamicAhpService.getExperts();
            return ApiResponse.success("查询成功", experts);
        } catch (Exception e) {
            log.error("获取专家列表失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @GetMapping("/levels")
    @Operation(summary = "获取层级列表", description = "获取指定模板的所有层级名称")
    public ApiResponse<List<String>> getLevels(
            @Parameter(description = "模板ID")
            @RequestParam(value = "templateId") Long templateId) {
        log.info("API调用: 获取层级列表 - templateId={}", templateId);
        try {
            List<String> levels = dynamicAhpService.getLevelNames(templateId);
            return ApiResponse.success("查询成功", levels);
        } catch (Exception e) {
            log.error("获取层级列表失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @GetMapping("/primaries")
    @Operation(summary = "获取一级维度列表", description = "获取指定层级的所有一级维度")
    public ApiResponse<List<Map<String, Object>>> getPrimaries(
            @Parameter(description = "模板ID")
            @RequestParam(value = "templateId") Long templateId,
            @Parameter(description = "层级名称")
            @RequestParam(value = "levelName") String levelName) {
        log.info("API调用: 获取一级维度 - templateId={}, levelName={}", templateId, levelName);
        try {
            List<Map<String, Object>> primaries = dynamicAhpService.getPrimaryDimensions(templateId, levelName);
            return ApiResponse.success("查询成功", primaries);
        } catch (Exception e) {
            log.error("获取一级维度失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @GetMapping("/secondaries")
    @Operation(summary = "获取二级指标列表", description = "获取指定一级维度下的所有二级指标")
    public ApiResponse<List<Map<String, Object>>> getSecondaries(
            @Parameter(description = "模板ID")
            @RequestParam(value = "templateId") Long templateId,
            @Parameter(description = "层级名称")
            @RequestParam(value = "levelName") String levelName,
            @Parameter(description = "一级维度编码")
            @RequestParam(value = "primaryCode") String primaryCode) {
        log.info("API调用: 获取二级指标 - templateId={}, levelName={}, primaryCode={}", templateId, levelName, primaryCode);
        try {
            List<Map<String, Object>> secondaries = dynamicAhpService.getSecondaryDimensions(templateId, levelName, primaryCode);
            return ApiResponse.success("查询成功", secondaries);
        } catch (Exception e) {
            log.error("获取二级指标失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    // ==================== 矩阵操作 ====================

    @GetMapping("/matrix/level-between")
    @Operation(summary = "获取层级间比较矩阵", description = "获取某专家对指定模板各层级间的一级维度进行AHP比较的矩阵")
    public ApiResponse<DynamicAhpMatrixDTO> getLevelBetweenMatrix(
            @Parameter(description = "模板ID")
            @RequestParam(value = "templateId") Long templateId,
            @Parameter(description = "专家ID")
            @RequestParam(value = "expertId") Long expertId) {
        log.info("API调用: 获取层级间矩阵 - templateId={}, expertId={}", templateId, expertId);
        try {
            DynamicAhpMatrixDTO matrix = dynamicAhpService.getLevelBetweenMatrix(templateId, expertId);
            return ApiResponse.success("查询成功", matrix);
        } catch (Exception e) {
            log.error("获取层级间矩阵失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @GetMapping("/matrix/primary-between")
    @Operation(summary = "获取一级维度间比较矩阵", description = "获取某专家对指定层级内一级维度进行AHP比较的矩阵")
    public ApiResponse<DynamicAhpMatrixDTO> getPrimaryBetweenMatrix(
            @Parameter(description = "模板ID")
            @RequestParam(value = "templateId") Long templateId,
            @Parameter(description = "层级名称")
            @RequestParam(value = "levelName") String levelName,
            @Parameter(description = "专家ID")
            @RequestParam(value = "expertId") Long expertId) {
        log.info("API调用: 获取一级维度间矩阵 - templateId={}, levelName={}, expertId={}", templateId, levelName, expertId);
        try {
            DynamicAhpMatrixDTO matrix = dynamicAhpService.getPrimaryBetweenMatrix(templateId, levelName, expertId);
            return ApiResponse.success("查询成功", matrix);
        } catch (Exception e) {
            log.error("获取一级维度间矩阵失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @GetMapping("/matrix/secondary-between")
    @Operation(summary = "获取二级指标间比较矩阵", description = "获取某专家对指定一级维度下二级指标进行AHP比较的矩阵")
    public ApiResponse<DynamicAhpMatrixDTO> getSecondaryBetweenMatrix(
            @Parameter(description = "模板ID")
            @RequestParam(value = "templateId") Long templateId,
            @Parameter(description = "层级名称")
            @RequestParam(value = "levelName") String levelName,
            @Parameter(description = "一级维度编码")
            @RequestParam(value = "primaryCode") String primaryCode,
            @Parameter(description = "专家ID")
            @RequestParam(value = "expertId") Long expertId) {
        log.info("API调用: 获取二级指标间矩阵 - templateId={}, levelName={}, primaryCode={}, expertId={}",
                templateId, levelName, primaryCode, expertId);
        try {
            DynamicAhpMatrixDTO matrix = dynamicAhpService.getSecondaryBetweenMatrix(templateId, levelName, primaryCode, expertId);
            return ApiResponse.success("查询成功", matrix);
        } catch (Exception e) {
            log.error("获取二级指标间矩阵失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @PostMapping("/matrix/save")
    @Operation(summary = "保存AHP矩阵打分", description = "保存专家的AHP矩阵打分记录")
    public ApiResponse<Integer> saveMatrix(
            @RequestBody SaveDynamicAhpMatrixRequest request) {
        log.info("API调用: 保存AHP矩阵 - expertId={}, templateId={}, levelName={}, matrixType={}, entriesCount={}",
                request.getExpertId(), request.getTemplateId(), request.getLevelName(), request.getMatrixType(),
                request.getEntries() != null ? request.getEntries().size() : 0);
        try {
            int count = dynamicAhpService.saveMatrix(request);
            log.info("保存AHP矩阵成功, count={}", count);
            return ApiResponse.success("保存成功", count);
        } catch (Exception e) {
            log.error("保存AHP矩阵失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    // ==================== 权重计算 ====================

    @PostMapping("/calculate")
    @Operation(summary = "计算AHP权重", description = "根据已保存的矩阵数据计算AHP权重")
    public ApiResponse<DynamicAhpResultDTO> calculateWeights(
            @Parameter(description = "模板ID")
            @RequestParam(value = "templateId") Long templateId,
            @Parameter(description = "专家ID")
            @RequestParam(value = "expertId") Long expertId,
            @Parameter(description = "层级名称")
            @RequestParam(value = "levelName") String levelName,
            @Parameter(description = "矩阵类型：LEVEL_BETWEEN, PRIMARY_BETWEEN, SECONDARY_BETWEEN")
            @RequestParam(value = "matrixType") String matrixType,
            @Parameter(description = "父级维度编码（二级矩阵时使用）")
            @RequestParam(value = "parentCode", required = false) String parentCode) {
        log.info("API调用: 计算AHP权重 - templateId={}, expertId={}, levelName={}, matrixType={}",
                templateId, expertId, levelName, matrixType);
        try {
            DynamicAhpResultDTO result = dynamicAhpService.calculateWeights(
                    templateId, expertId, levelName, matrixType, parentCode);
            return ApiResponse.success("计算成功", result);
        } catch (Exception e) {
            log.error("计算AHP权重失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/calculate/all")
    @Operation(summary = "计算所有AHP权重", description = "计算某专家在某模板下的所有层级和维度的AHP权重")
    public ApiResponse<DynamicAhpCompleteResultDTO> calculateAllWeights(
            @Parameter(description = "模板ID")
            @RequestParam(value = "templateId") Long templateId,
            @Parameter(description = "专家ID")
            @RequestParam(value = "expertId") Long expertId) {
        log.info("API调用: 计算所有AHP权重 - templateId={}, expertId={}", templateId, expertId);
        try {
            DynamicAhpCompleteResultDTO result = dynamicAhpService.calculateAllWeights(templateId, expertId);
            return ApiResponse.success("计算成功", result);
        } catch (Exception e) {
            log.error("计算所有AHP权重失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/weights/save")
    @Operation(summary = "保存计算结果", description = "将计算得到的权重结果保存到数据库")
    public ApiResponse<Integer> saveWeights(
            @Parameter(description = "模板ID")
            @RequestParam(value = "templateId") Long templateId,
            @Parameter(description = "专家ID")
            @RequestParam(value = "expertId") Long expertId) {
        log.info("API调用: 保存权重结果 - templateId={}, expertId={}", templateId, expertId);
        try {
            int count = dynamicAhpService.saveWeights(templateId, expertId);
            return ApiResponse.success("保存成功", count);
        } catch (Exception e) {
            log.error("保存权重结果失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/weights")
    @Operation(summary = "获取已保存的权重", description = "获取某专家在某模板下已保存的AHP权重结果")
    public ApiResponse<DynamicAhpCompleteResultDTO> getWeights(
            @Parameter(description = "模板ID")
            @RequestParam(value = "templateId") Long templateId,
            @Parameter(description = "专家ID")
            @RequestParam(value = "expertId") Long expertId) {
        log.info("API调用: 获取权重 - templateId={}, expertId={}", templateId, expertId);
        try {
            DynamicAhpCompleteResultDTO weights = dynamicAhpService.getWeights(templateId, expertId);
            return ApiResponse.success("查询成功", weights);
        } catch (Exception e) {
            log.error("获取权重失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @GetMapping("/weights/by-level")
    @Operation(summary = "获取指定层级的权重", description = "获取某专家在某模板某层级下的权重结果")
    public ApiResponse<List<DynamicAhpResultDTO>> getWeightsByLevel(
            @Parameter(description = "模板ID")
            @RequestParam(value = "templateId") Long templateId,
            @Parameter(description = "专家ID")
            @RequestParam(value = "expertId") Long expertId,
            @Parameter(description = "层级名称")
            @RequestParam(value = "levelName") String levelName) {
        log.info("API调用: 获取层级权重 - templateId={}, expertId={}, levelName={}", templateId, expertId, levelName);
        try {
            List<DynamicAhpResultDTO> weights = dynamicAhpService.getWeightsByLevel(templateId, expertId, levelName);
            return ApiResponse.success("查询成功", weights);
        } catch (Exception e) {
            log.error("获取层级权重失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    // ==================== 辅助功能 ====================

    @PostMapping("/simulate")
    @Operation(summary = "模拟AHP打分", description = "为专家生成随机的AHP打分数据")
    public ApiResponse<Integer> simulateScores(
            @Parameter(description = "模板ID")
            @RequestParam(value = "templateId") Long templateId,
            @Parameter(description = "专家ID列表")
            @RequestParam(value = "expertIds") List<Long> expertIds) {
        log.info("API调用: 模拟AHP打分 - templateId={}, expertCount={}", templateId, expertIds.size());
        try {
            int count = dynamicAhpService.simulateScores(templateId, expertIds);
            return ApiResponse.success("模拟成功", count);
        } catch (Exception e) {
            log.error("模拟AHP打分失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/batch-simulate")
    @Operation(summary = "批量模拟AHP打分", description = "为模板下所有专家生成随机的AHP打分数据")
    public ApiResponse<Integer> batchSimulateScores(
            @Parameter(description = "模板ID")
            @RequestParam(value = "templateId") Long templateId) {
        log.info("API调用: 批量模拟AHP打分 - templateId={}", templateId);
        try {
            int count = dynamicAhpService.batchSimulateScores(templateId);
            return ApiResponse.success("批量模拟成功，已生成 " + count + " 条数据", count);
        } catch (Exception e) {
            log.error("批量模拟AHP打分失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/matrix")
    @Operation(summary = "清除AHP打分", description = "清除某专家在某模板下的所有AHP矩阵数据")
    public ApiResponse<Void> clearMatrix(
            @Parameter(description = "模板ID")
            @RequestParam(value = "templateId") Long templateId,
            @Parameter(description = "专家ID")
            @RequestParam(value = "expertId") Long expertId) {
        log.info("API调用: 清除AHP矩阵 - templateId={}, expertId={}", templateId, expertId);
        try {
            dynamicAhpService.clearMatrix(templateId, expertId);
            return ApiResponse.success("清除成功", null);
        } catch (Exception e) {
            log.error("清除AHP矩阵失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/status")
    @Operation(summary = "获取AHP配置状态", description = "获取某专家在某模板下的AHP配置完成状态")
    public ApiResponse<Map<String, Object>> getStatus(
            @Parameter(description = "模板ID")
            @RequestParam(value = "templateId") Long templateId,
            @Parameter(description = "专家ID")
            @RequestParam(value = "expertId") Long expertId) {
        log.info("API调用: 获取AHP状态 - templateId={}, expertId={}", templateId, expertId);
        try {
            Map<String, Object> status = dynamicAhpService.getConfigurationStatus(templateId, expertId);
            return ApiResponse.success("查询成功", status);
        } catch (Exception e) {
            log.error("获取AHP状态失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }
}
