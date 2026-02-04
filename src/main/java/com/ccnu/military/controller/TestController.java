package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.entity.MilitaryEffectivenessEvaluation;
import com.ccnu.military.repository.MilitaryEffectivenessRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试控制器 - 用于调试
 */
@Slf4j
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Tag(name = "测试接口", description = "用于调试的测试接口")
public class TestController {

    private final MilitaryEffectivenessRepository repository;
    private final DataSource dataSource;

    @GetMapping("/db-connection")
    @Operation(summary = "测试数据库连接", description = "测试是否能正常查询数据")
    public ApiResponse<Map<String, Object>> testDbConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 测试查询
            List<MilitaryEffectivenessEvaluation> evaluations = repository.findAll();
            
            result.put("success", true);
            result.put("recordCount", evaluations.size());
            result.put("message", "数据库连接正常");
            
            if (!evaluations.isEmpty()) {
                MilitaryEffectivenessEvaluation first = evaluations.get(0);
                Map<String, Object> firstRecord = new HashMap<>();
                firstRecord.put("testId", first.getTestId());
                firstRecord.put("scenarioId", first.getScenarioId());
                firstRecord.put("totalCommunications", first.getTotalCommunications());
                result.put("firstRecord", firstRecord);
            }
            
            return ApiResponse.success("测试成功", result);
        } catch (Exception e) {
            log.error("数据库连接测试失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getName());
            return ApiResponse.error("测试失败: " + e.getMessage());
        }
    }

    @GetMapping("/table-columns")
    @Operation(summary = "查看表结构", description = "查看military_effectiveness_evaluation表的实际列名")
    public ApiResponse<List<Map<String, String>>> getTableColumns() {
        List<Map<String, String>> columns = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getColumns(null, null, "military_effectiveness_evaluation", null);
            
            while (rs.next()) {
                Map<String, String> column = new HashMap<>();
                column.put("columnName", rs.getString("COLUMN_NAME"));
                column.put("dataType", rs.getString("TYPE_NAME"));
                column.put("columnSize", rs.getString("COLUMN_SIZE"));
                column.put("nullable", rs.getString("IS_NULLABLE"));
                columns.add(column);
            }
            
            log.info("表 military_effectiveness_evaluation 共有 {} 列", columns.size());
            return ApiResponse.success("查询成功", columns);
            
        } catch (Exception e) {
            log.error("查询表结构失败", e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }
}
