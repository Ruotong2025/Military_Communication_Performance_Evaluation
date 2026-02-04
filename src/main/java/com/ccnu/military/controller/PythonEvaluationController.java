package com.ccnu.military.controller;

import com.ccnu.military.dto.ApiResponse;
import com.ccnu.military.service.PythonEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Python 评估控制器
 * 调用 Python 脚本执行评估计算
 */
@Slf4j
@RestController
@RequestMapping("/python-evaluation")
@RequiredArgsConstructor
@Tag(name = "Python 评估接口", description = "调用 Python 脚本执行评估计算")
public class PythonEvaluationController {

    private final PythonEvaluationService pythonEvaluationService;

    /**
     * 执行评估计算
     * 
     * @param request 包含 priorities 的请求体，例如：
     *                {
     *                  "priorities": {
     *                    "RL": 1,
     *                    "SC": 2,
     *                    "AJ": 3,
     *                    "EF": 4,
     *                    "PO": 5,
     *                    "NC": 6,
     *                    "HO": 7,
     *                    "RS": 8
     *                  }
     *                }
     */
    @PostMapping("/calculate")
    @Operation(summary = "执行评估计算", description = "调用 Python 脚本执行完整的评估计算（AHP + 熵权法 + 综合评分）")
    public ApiResponse<Map<String, Object>> calculate(@RequestBody Map<String, Object> request) {
        log.info("API调用: Python 评估计算 - request={}", request);

        try {
            // 提取优先级配置
            @SuppressWarnings("unchecked")
            Map<String, Integer> priorities = (Map<String, Integer>) request.get("priorities");

            if (priorities == null || priorities.isEmpty()) {
                return ApiResponse.error("缺少 priorities 参数");
            }

            // 验证优先级配置
            String[] requiredDimensions = {"RL", "SC", "AJ", "EF", "PO", "NC", "HO", "RS"};
            for (String dim : requiredDimensions) {
                if (!priorities.containsKey(dim)) {
                    return ApiResponse.error("缺少维度: " + dim);
                }
            }

            // 调用 Python 服务
            Map<String, Object> result = pythonEvaluationService.evaluate(priorities);

            // 检查结果
            Boolean success = (Boolean) result.get("success");
            if (success != null && success) {
                log.info("Python 评估计算成功");
                return ApiResponse.success("评估计算成功", result);
            } else {
                String message = (String) result.get("message");
                log.error("Python 评估计算失败: {}", message);
                return ApiResponse.error(message != null ? message : "评估计算失败");
            }

        } catch (Exception e) {
            log.error("评估计算失败", e);
            return ApiResponse.error("评估计算失败: " + e.getMessage());
        }
    }

    /**
     * 测试 Python 环境
     */
    @GetMapping("/test-environment")
    @Operation(summary = "测试 Python 环境", description = "检查 Python 是否正确配置")
    public ApiResponse<Map<String, Object>> testEnvironment() {
        log.info("API调用: 测试 Python 环境");

        try {
            Map<String, Object> result = pythonEvaluationService.testPythonEnvironment();
            Boolean success = (Boolean) result.get("success");

            if (success != null && success) {
                return ApiResponse.success("Python 环境正常", result);
            } else {
                return ApiResponse.error((String) result.get("message"));
            }

        } catch (Exception e) {
            log.error("测试 Python 环境失败", e);
            return ApiResponse.error("测试失败: " + e.getMessage());
        }
    }
}
