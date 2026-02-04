package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统健康检查控制器
 */
@Slf4j
@RestController
@RequestMapping("/")
@Tag(name = "系统信息", description = "系统健康检查和基本信息")
public class SystemController {

    @GetMapping
    @Operation(summary = "系统欢迎页", description = "返回系统基本信息")
    public ApiResponse<Map<String, Object>> welcome() {
        Map<String, Object> info = new HashMap<>();
        info.put("system", "军事通信效能评估系统");
        info.put("version", "1.0.0");
        info.put("status", "running");
        info.put("timestamp", LocalDateTime.now());
        info.put("apiDoc", "/swagger-ui.html");
        
        return ApiResponse.success("系统运行正常", info);
    }

    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查系统运行状态")
    public ApiResponse<String> health() {
        return ApiResponse.success("系统健康", "OK");
    }
}
