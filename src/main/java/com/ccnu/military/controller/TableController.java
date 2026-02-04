package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.dto.ColumnInfo;
import com.ccnu.military.dto.PageResult;
import com.ccnu.military.service.TableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 动态表查询控制器
 */
@Slf4j
@RestController
@RequestMapping("/table")
@RequiredArgsConstructor
@Tag(name = "动态表查询", description = "动态查询数据库表结构和数据")
public class TableController {

    private final TableService tableService;

    @GetMapping("/allowed")
    @Operation(summary = "获取允许查询的表列表", description = "返回所有可以查询的表名")
    public ApiResponse<Set<String>> getAllowedTables() {
        log.info("API调用: 获取允许查询的表列表");
        Set<String> tables = tableService.getAllowedTables();
        return ApiResponse.success("查询成功", tables);
    }

    @GetMapping("/structure/{tableName}")
    @Operation(summary = "获取表结构", description = "动态获取指定表的列信息")
    public ApiResponse<List<ColumnInfo>> getTableStructure(
            @Parameter(description = "表名", example = "during_battle_communications")
            @PathVariable String tableName) {
        log.info("API调用: 获取表结构 - {}", tableName);
        try {
            List<ColumnInfo> columns = tableService.getTableStructure(tableName);
            return ApiResponse.success("查询成功", columns);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/data/{tableName}")
    @Operation(summary = "获取表数据", description = "分页查询指定表的数据")
    public ApiResponse<PageResult> getTableData(
            @Parameter(description = "表名", example = "during_battle_communications")
            @PathVariable String tableName,
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        log.info("API调用: 获取表数据 - {} (page={}, size={})", tableName, page, size);
        try {
            PageResult result = tableService.getTableData(tableName, page, size);
            return ApiResponse.success("查询成功", result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
}
