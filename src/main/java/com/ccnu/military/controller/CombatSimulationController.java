package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.dto.CombatSimulationRequest;
import com.ccnu.military.service.CombatSimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 作战模拟数据生成（records_* 四表）
 */
@RestController
@RequestMapping("/combat/simulation")
@RequiredArgsConstructor
@Tag(name = "作战模拟", description = "生成 records_* 四表模拟数据")
public class CombatSimulationController {

    private final CombatSimulationService combatSimulationService;

    @PostMapping("/generate")
    @Operation(summary = "生成模拟数据", description = "调用 Python 写入四张 records_* 表")
    public ApiResponse<Map<String, Object>> generate(@RequestBody(required = false) CombatSimulationRequest body) {
        CombatSimulationRequest req = body != null ? body : new CombatSimulationRequest();
        Map<String, Object> result = combatSimulationService.generateSimulation(req);
        Boolean ok = (Boolean) result.get("success");
        if (Boolean.TRUE.equals(ok)) {
            return ApiResponse.success((String) result.getOrDefault("message", "操作成功"), result);
        }
        return ApiResponse.error(500, (String) result.getOrDefault("message", "生成失败"));
    }
}
