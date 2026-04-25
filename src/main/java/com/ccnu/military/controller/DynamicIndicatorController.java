package com.ccnu.military.controller;

import com.ccnu.military.dto.AhpWeightResultDTO;
import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.dto.ImportResultDTO;
import com.ccnu.military.dto.IndicatorTemplateDTO;
import com.ccnu.military.dto.IndicatorTreeDTO;
import com.ccnu.military.entity.PrimaryDimension;
import com.ccnu.military.entity.SecondaryDimension;
import com.ccnu.military.service.DynamicIndicatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/dynamic-indicator")
@RequiredArgsConstructor
@Tag(name = "动态指标管理", description = "动态指标系统的API接口")
public class DynamicIndicatorController {

    private final DynamicIndicatorService dynamicIndicatorService;

    @PostMapping("/parse")
    @Operation(summary = "解析Excel指标文件", description = "解析Excel文件并返回指标结构，不保存到数据库")
    public ApiResponse<IndicatorTreeDTO> parseExcel(
            @Parameter(description = "Excel文件")
            @RequestParam("file") MultipartFile file) {
        log.info("API调用: 解析Excel文件 - {}", file.getOriginalFilename());
        try {
            IndicatorTreeDTO result = dynamicIndicatorService.parseExcel(file);
            return ApiResponse.success("解析成功", result);
        } catch (Exception e) {
            log.error("解析Excel失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/import")
    @Operation(summary = "导入指标到数据库", description = "解析Excel文件并保存到数据库")
    public ApiResponse<ImportResultDTO> importIndicators(
            @Parameter(description = "Excel文件")
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "模板名称")
            @RequestParam(value = "templateName", required = false) String templateName) {
        log.info("API调用: 导入指标 - {}", templateName);
        try {
            ImportResultDTO result = dynamicIndicatorService.importIndicators(file, templateName);
            return ApiResponse.success("导入成功", result);
        } catch (Exception e) {
            log.error("导入指标失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/templates")
    @Operation(summary = "获取指标模板列表", description = "获取所有已保存的指标模板")
    public ApiResponse<List<IndicatorTemplateDTO>> getTemplates() {
        log.info("API调用: 获取模板列表");
        try {
            return ApiResponse.success("查询成功", dynamicIndicatorService.getTemplates());
        } catch (Exception e) {
            log.error("获取模板列表失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @GetMapping("/tree")
    @Operation(summary = "获取指标树结构", description = "获取指定模板的完整指标树结构")
    public ApiResponse<IndicatorTreeDTO> getIndicatorTree(
            @Parameter(description = "模板ID")
            @RequestParam(value = "templateId", required = false) Long templateId) {
        log.info("API调用: 获取指标树 - templateId={}", templateId);
        try {
            if (templateId == null) {
                // 获取第一个模板
                List templates = dynamicIndicatorService.getTemplates();
                if (templates.isEmpty()) {
                    return ApiResponse.error(404, "没有找到任何模板");
                }
                templateId = (Long) templates.get(0);
            }
            IndicatorTreeDTO result = dynamicIndicatorService.getIndicatorTree(templateId);
            return ApiResponse.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取指标树失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @GetMapping("/level/primaries")
    @Operation(summary = "获取一级维度列表", description = "获取指定层级下的所有一级维度")
    public ApiResponse<List<PrimaryDimension>> getPrimaryDimensions(
            @Parameter(description = "层级ID")
            @RequestParam("levelId") Long levelId) {
        log.info("API调用: 获取一级维度 - levelId={}", levelId);
        try {
            return ApiResponse.success("查询成功", dynamicIndicatorService.getPrimaryDimensions(levelId));
        } catch (Exception e) {
            log.error("获取一级维度失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @GetMapping("/primary/secondaries")
    @Operation(summary = "获取二级维度列表", description = "获取指定一级维度下的所有二级维度")
    public ApiResponse<List<SecondaryDimension>> getSecondaryDimensions(
            @Parameter(description = "一级维度ID")
            @RequestParam("primaryId") Long primaryId) {
        log.info("API调用: 获取二级维度 - primaryId={}", primaryId);
        try {
            return ApiResponse.success("查询成功", dynamicIndicatorService.getSecondaryDimensions(primaryId));
        } catch (Exception e) {
            log.error("获取二级维度失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @GetMapping("/ahp/matrix")
    @Operation(summary = "获取AHP矩阵", description = "获取指定层级的AHP判断矩阵")
    public ApiResponse<Map<String, Object>> getAhpMatrix(
            @Parameter(description = "层级ID")
            @RequestParam("levelId") Long levelId) {
        log.info("API调用: 获取AHP矩阵 - levelId={}", levelId);
        try {
            return ApiResponse.success("查询成功", dynamicIndicatorService.getAhpMatrix(levelId));
        } catch (Exception e) {
            log.error("获取AHP矩阵失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @PostMapping("/ahp/calculate")
    @Operation(summary = "计算AHP权重", description = "根据判断矩阵计算权重和一致性比率")
    public ApiResponse<AhpWeightResultDTO> calculateAhpWeights(
            @RequestBody Map<String, Object> request) {
        log.info("API调用: 计算AHP权重");
        try {
            Long levelId = Long.valueOf(request.get("levelId").toString());
            @SuppressWarnings("unchecked")
            List<List<Number>> matrixData = (List<List<Number>>) request.get("matrix");
            double[][] matrix = new double[matrixData.size()][];
            for (int i = 0; i < matrixData.size(); i++) {
                matrix[i] = new double[matrixData.get(i).size()];
                for (int j = 0; j < matrixData.get(i).size(); j++) {
                    matrix[i][j] = matrixData.get(i).get(j).doubleValue();
                }
            }
            AhpWeightResultDTO result = dynamicIndicatorService.calculateAhpWeights(levelId, matrix);
            return ApiResponse.success("计算成功", result);
        } catch (Exception e) {
            log.error("计算AHP权重失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/ahp/save")
    @Operation(summary = "保存AHP权重", description = "保存计算后的权重到数据库")
    public ApiResponse<Void> saveAhpWeights(
            @RequestBody Map<String, Object> request) {
        log.info("API调用: 保存AHP权重");
        try {
            Long levelId = Long.valueOf(request.get("levelId").toString());
            @SuppressWarnings("unchecked")
            Map<String, Double> weights = (Map<String, Double>) request.get("weights");
            dynamicIndicatorService.saveAhpWeights(levelId, weights);
            return ApiResponse.success("保存成功", null);
        } catch (Exception e) {
            log.error("保存AHP权重失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/secondary/{id}")
    @Operation(summary = "更新二级维度配置", description = "更新二级维度的配置信息")
    public ApiResponse<Void> updateSecondaryConfig(
            @Parameter(description = "二级维度ID")
            @PathVariable Long id,
            @RequestBody SecondaryDimension config) {
        log.info("API调用: 更新二级维度配置 - id={}", id);
        try {
            dynamicIndicatorService.updateSecondaryConfig(id, config);
            return ApiResponse.success("更新成功", null);
        } catch (Exception e) {
            log.error("更新二级维度配置失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/template/{templateId}")
    @Operation(summary = "删除指标模板", description = "删除指定的指标模板及其所有关联数据")
    public ApiResponse<Void> deleteTemplate(
            @Parameter(description = "模板ID")
            @PathVariable Long templateId) {
        log.info("API调用: 删除模板 - templateId={}", templateId);
        try {
            dynamicIndicatorService.deleteTemplate(templateId);
            return ApiResponse.success("删除成功", null);
        } catch (Exception e) {
            log.error("删除模板失败", e);
            return ApiResponse.error(400, e.getMessage());
        }
    }
}
