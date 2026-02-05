package com.ccnu.military.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Python 评估服务调用器
 * 负责调用 Python 脚本执行评估计算
 */
@Slf4j
@Service
public class PythonEvaluationService {

    @Value("${python.executable:python}")
    private String pythonExecutable;

    @Value("${python.script.path:python_service/evaluation_service.py}")
    private String pythonScriptPath;

    @Value("${python.timeout:300}")
    private long pythonTimeout;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 调用 Python 脚本执行评估
     *
     * @param priorities 维度优先级，例如 {"RL": 1, "SC": 2, ...}
     * @return 评估结果 JSON
     */
    public Map<String, Object> evaluate(Map<String, Integer> priorities) {
        try {
            log.info("开始调用 Python 评估服务，优先级配置: {}", priorities);

            // 构建输入 JSON
            Map<String, Object> input = new HashMap<>();
            input.put("priorities", priorities);
            String inputJson = objectMapper.writeValueAsString(input);
            log.info("发送给 Python 的 JSON: {}", inputJson);

            // 构建命令（不传递 JSON 参数）
            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonExecutable,
                    "-u",  // 无缓冲模式
                    pythonScriptPath
            );

            // 设置环境变量，确保 Python 使用 UTF-8 编码
            Map<String, String> env = processBuilder.environment();
            env.put("PYTHONIOENCODING", "utf-8");

            // 不要重定向 stderr 到 stdout，分别读取
            // processBuilder.redirectErrorStream(true);  // 注释掉这行

            // 启动进程
            Process process = processBuilder.start();

            // 通过标准输入传递 JSON
            try (java.io.OutputStream writer = process.getOutputStream()) {
                writer.write(inputJson.getBytes(StandardCharsets.UTF_8));
                writer.flush();
            }

            // 读取输出
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            
            // 在单独的线程中读取 stderr（调试信息）
            Thread errorThread = new Thread(() -> {
                try (BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorOutput.append(line).append("\n");
                        // 记录调试信息到日志
                        if (line.contains("[DEBUG]") || line.contains("[ERROR]")) {
                            log.info("Python 调试信息: {}", line);
                        }
                    }
                } catch (Exception e) {
                    log.error("读取 Python stderr 失败", e);
                }
            });
            errorThread.start();
            
            // 读取标准输出（JSON 结果）
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            // 等待 stderr 读取完成
            errorThread.join(5000);

            // 等待进程完成
            boolean finished = process.waitFor(pythonTimeout, TimeUnit.SECONDS);

            if (!finished) {
                process.destroy();
                log.error("Python 脚本执行超时");
                return createErrorResponse("Python 脚本执行超时");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("Python 脚本执行失败，退出码: {}, 输出: {}", exitCode, output);
                return createErrorResponse("Python 脚本执行失败: " + output);
            }

            // 解析 JSON 输出
            String jsonOutput = output.toString().trim();
            log.info("Python 脚本执行成功，输出长度: {} 字符", jsonOutput.length());

            Map<String, Object> result = objectMapper.readValue(jsonOutput, Map.class);
            return result;

        } catch (Exception e) {
            log.error("调用 Python 评估服务失败", e);
            return createErrorResponse("调用 Python 评估服务失败: " + e.getMessage());
        }
    }

    /**
     * 测试 Python 环境
     */
    public Map<String, Object> testPythonEnvironment() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(pythonExecutable, "--version");
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroy();
                return createErrorResponse("Python 版本检测超时");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("pythonVersion", output.toString());
            result.put("pythonExecutable", pythonExecutable);
            result.put("scriptPath", pythonScriptPath);
            return result;

        } catch (Exception e) {
            log.error("测试 Python 环境失败", e);
            return createErrorResponse("测试 Python 环境失败: " + e.getMessage());
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        return error;
    }
}
