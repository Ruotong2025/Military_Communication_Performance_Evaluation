package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.entity.DuringBattleCommunication;
import com.ccnu.military.service.DuringBattleCommunicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 战中通信记录控制器
 */
@Slf4j
@RestController
@RequestMapping("/communication")
@RequiredArgsConstructor
@Tag(name = "战中通信记录", description = "战中通信记录相关接口")
public class DuringBattleCommunicationController {

    private final DuringBattleCommunicationService service;

    @GetMapping("/list")
    @Operation(summary = "查询所有通信记录", description = "获取所有战中通信记录")
    public ApiResponse<List<DuringBattleCommunication>> getAllCommunications() {
        log.info("API调用: 查询所有通信记录");
        List<DuringBattleCommunication> communications = service.findAll();
        return ApiResponse.success("查询成功", communications);
    }

    @GetMapping("/test/{testId}")
    @Operation(summary = "根据测试批次ID查询", description = "根据测试批次ID查询通信记录")
    public ApiResponse<List<DuringBattleCommunication>> getByTestId(
            @Parameter(description = "测试批次ID", example = "TEST-2026-001")
            @PathVariable String testId) {
        log.info("API调用: 查询测试批次 {} 的通信记录", testId);
        List<DuringBattleCommunication> communications = service.findByTestId(testId);
        return ApiResponse.success("查询成功", communications);
    }

    @GetMapping("/scenario/{scenarioId}")
    @Operation(summary = "根据场景ID查询", description = "根据场景ID查询通信记录")
    public ApiResponse<List<DuringBattleCommunication>> getByScenarioId(
            @Parameter(description = "场景ID", example = "1")
            @PathVariable Integer scenarioId) {
        log.info("API调用: 查询场景ID {} 的通信记录", scenarioId);
        List<DuringBattleCommunication> communications = service.findByScenarioId(scenarioId);
        return ApiResponse.success("查询成功", communications);
    }

    @GetMapping("/failed")
    @Operation(summary = "查询失败的通信记录", description = "查询所有通信失败的记录")
    public ApiResponse<List<DuringBattleCommunication>> getFailedCommunications() {
        log.info("API调用: 查询失败的通信记录");
        List<DuringBattleCommunication> communications = service.findFailedCommunications();
        return ApiResponse.success("查询成功", communications);
    }

    @GetMapping("/failed/test/{testId}")
    @Operation(summary = "查询指定测试批次的失败记录", description = "根据测试批次ID查询失败的通信记录")
    public ApiResponse<List<DuringBattleCommunication>> getFailedByTestId(
            @Parameter(description = "测试批次ID", example = "TEST-2026-001")
            @PathVariable String testId) {
        log.info("API调用: 查询测试批次 {} 的失败通信记录", testId);
        List<DuringBattleCommunication> communications = service.findFailedCommunicationsByTestId(testId);
        return ApiResponse.success("查询成功", communications);
    }

    @GetMapping("/statistics/test/{testId}")
    @Operation(summary = "获取测试批次统计信息", description = "获取指定测试批次的通信统计信息")
    public ApiResponse<Map<String, Object>> getStatistics(
            @Parameter(description = "测试批次ID", example = "TEST-2026-001")
            @PathVariable String testId) {
        log.info("API调用: 获取测试批次 {} 的统计信息", testId);
        
        Long total = service.countByTestId(testId);
        Long successful = service.countSuccessfulByTestId(testId);
        Double successRate = service.calculateSuccessRate(testId);
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("testId", testId);
        statistics.put("totalCommunications", total);
        statistics.put("successfulCommunications", successful);
        statistics.put("failedCommunications", total - successful);
        statistics.put("successRate", String.format("%.2f%%", successRate));
        
        return ApiResponse.success("查询成功", statistics);
    }

    @GetMapping("/detected")
    @Operation(summary = "查询被侦察的通信记录", description = "查询所有被侦察的通信记录")
    public ApiResponse<List<DuringBattleCommunication>> getDetectedCommunications() {
        log.info("API调用: 查询被侦察的通信记录");
        List<DuringBattleCommunication> communications = service.findDetectedCommunications();
        return ApiResponse.success("查询成功", communications);
    }

    @GetMapping("/intercepted")
    @Operation(summary = "查询被拦截的通信记录", description = "查询所有被拦截的通信记录")
    public ApiResponse<List<DuringBattleCommunication>> getInterceptedCommunications() {
        log.info("API调用: 查询被拦截的通信记录");
        List<DuringBattleCommunication> communications = service.findInterceptedCommunications();
        return ApiResponse.success("查询成功", communications);
    }
}
