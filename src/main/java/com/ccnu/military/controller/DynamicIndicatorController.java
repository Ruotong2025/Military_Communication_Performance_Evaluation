package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.dto.ImportResultDTO;
import com.ccnu.military.dto.DynamicIndicatorTreeDTO;
import com.ccnu.military.entity.DynamicDimension;
import com.ccnu.military.entity.DynamicTemplate;
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
    @Operation(summary = "解析Excel文件", description = "仅解析Excel文件，不保存到数据库")
    public ApiResponse<Map<String, Object>> parseExcel(
            @Parameter(description = "Excel文件")
            @RequestParam("file") MultipartFile file) {
        log.info("API调用: 解析Excel - {}", file.getOriginalFilename());
        try {
            Map<String, Object> result = dynamicIndicatorService.parseExcelOnly(file);
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
        log.info("API调用: 导入指标 - {}, templateName={}", file.getOriginalFilename(), templateName);
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
    public ApiResponse<List<DynamicTemplate>> getTemplates() {
        log.info("API调用: 获取模板列表");
        try {
            return ApiResponse.success("查询成功", dynamicIndicatorService.getTemplateList());
        } catch (Exception e) {
            log.error("获取模板列表失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @GetMapping("/tree")
    @Operation(summary = "获取指标树结构", description = "获取指定模板的完整指标树结构")
    public ApiResponse<DynamicIndicatorTreeDTO> getIndicatorTree(
            @Parameter(description = "模板ID")
            @RequestParam(value = "templateId", required = false) Long templateId) {
        log.info("API调用: 获取指标树 - templateId={}", templateId);
        try {
            if (templateId == null) {
                List<DynamicTemplate> templates = dynamicIndicatorService.getTemplateList();
                if (templates.isEmpty()) {
                    return ApiResponse.error(404, "没有找到任何模板");
                }
                templateId = templates.get(0).getId();
            }
            DynamicIndicatorTreeDTO result = dynamicIndicatorService.getIndicatorTree(templateId);
            return ApiResponse.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取指标树失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @GetMapping("/quantitative")
    @Operation(summary = "获取定量指标列表", description = "获取指定模板的所有定量指标（含平均数）")
    public ApiResponse<List<DynamicDimension>> getQuantitativeIndicators(
            @Parameter(description = "模板ID")
            @RequestParam("templateId") Long templateId) {
        log.info("API调用: 获取定量指标 - templateId={}", templateId);
        try {
            return ApiResponse.success("查询成功", dynamicIndicatorService.getQuantitativeIndicators(templateId));
        } catch (Exception e) {
            log.error("获取定量指标失败", e);
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @PutMapping("/average-value/{id}")
    @Operation(summary = "更新平均数", description = "更新定量指标的平均数值")
    public ApiResponse<DynamicDimension> updateAverageValue(
            @Parameter(description = "维度ID")
            @PathVariable Long id,
            @RequestParam("averageValue") Double averageValue) {
        log.info("API调用: 更新平均数 - id={}, value={}", id, averageValue);
        try {
            return ApiResponse.success("更新成功", dynamicIndicatorService.updateAverageValue(id, averageValue));
        } catch (Exception e) {
            log.error("更新平均数失败", e);
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
