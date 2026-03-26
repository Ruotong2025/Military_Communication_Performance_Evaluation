package com.ccnu.military.service;

import com.ccnu.military.dto.CombatSimulationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 调用 Python 脚本生成 records_* 四表模拟数据
 */
@Slf4j
@Service
public class CombatSimulationService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${python.executable:python}")
    private String pythonExecutable;

    @Value("${python.combat-data-script:military_operational_effectiveness_evaluation/generate/generate_all_data.py}")
    private String combatDataScriptRelative;

    @Value("${python.combat-timeout:600}")
    private long combatTimeoutSeconds;

    public Map<String, Object> generateSimulation(CombatSimulationRequest req) {
        int count = req.getCount() == null ? 10 : Math.max(1, Math.min(100, req.getCount()));
        String quality = normalizeLevel(req.getExcellentLevel());
        String dispersion = normalizeLevel(req.getDispersionLevel());
        String seed = req.getSeed() == null ? "" : req.getSeed().trim();
        String mode = (req.getMode() == null || req.getMode().isBlank())
                ? "overwrite" : req.getMode().trim().toLowerCase();
        if (!mode.equals("overwrite") && !mode.equals("append")) {
            mode = "overwrite";
        }

        // overrides 序列化为 JSON 字符串传给 Python
        String overridesJson = "";
        if (req.getOverrides() != null && !req.getOverrides().isEmpty()) {
            try {
                overridesJson = objectMapper.writeValueAsString(req.getOverrides());
                log.info("overrides JSON: {}", overridesJson);
            } catch (Exception e) {
                log.warn("overrides 序列化失败，使用空覆盖", e);
                overridesJson = "";
            }
        }

        Path scriptPath = Paths.get(System.getProperty("user.dir"), combatDataScriptRelative).normalize();
        File scriptFile = scriptPath.toFile();
        if (!scriptFile.isFile()) {
            log.error("未找到生成脚本: {}", scriptFile.getAbsolutePath());
            return Map.of(
                    "success", false,
                    "message", "未找到生成脚本: " + scriptFile.getAbsolutePath()
            );
        }

        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add("-u");
        command.add(scriptFile.getAbsolutePath());
        command.add("--records-only");
        command.add("--count");
        command.add(String.valueOf(count));
        command.add("--quality");
        command.add(quality);
        command.add("--dispersion");
        command.add(dispersion);
        command.add("--mode");
        command.add(mode);
        if (!seed.isEmpty()) {
            command.add("--seed");
            command.add(seed);
        }
        // overrides 和 enumOverrides 通过环境变量传递，避免 Windows 命令行对 JSON {} 的转义问题
        String enumOverridesJson = "";
        if (req.getEnumOverrides() != null && !req.getEnumOverrides().isEmpty()) {
            try {
                enumOverridesJson = objectMapper.writeValueAsString(req.getEnumOverrides());
                log.info("enumOverrides JSON: {}", enumOverridesJson);
            } catch (Exception e) {
                log.warn("enumOverrides 序列化失败", e);
                enumOverridesJson = "";
            }
        }
        if (!overridesJson.isEmpty()) {
            command.add("--overrides-env");
        }
        if (!enumOverridesJson.isEmpty()) {
            command.add("--enum-overrides-env");
        }

        log.info("执行作战模拟生成: {}", String.join(" ", command));

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(System.getProperty("user.dir")));
            Map<String, String> env = pb.environment();
            env.put("PYTHONIOENCODING", "utf-8");
            // 通过环境变量传递 JSON，避免命令行特殊字符转义问题
            if (!overridesJson.isEmpty()) {
                env.put("COMBAT_OVERRIDES", overridesJson);
            }
            if (!enumOverridesJson.isEmpty()) {
                env.put("COMBAT_ENUM_OVERRIDES", enumOverridesJson);
            }
            Process process = pb.start();

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            Thread errThread = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        stderr.append(line).append('\n');
                    }
                } catch (Exception e) {
                    log.warn("读取 stderr 失败", e);
                }
            });
            errThread.start();

            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    stdout.append(line).append('\n');
                }
            }

            errThread.join(5000);

            boolean finished = process.waitFor(combatTimeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return Map.of("success", false, "message", "生成超时（" + combatTimeoutSeconds + " 秒）");
            }

            int code = process.exitValue();
            if (code != 0) {
                log.error("Python 退出码 {}，stderr: {}", code, stderr);
                return Map.of(
                        "success", false,
                        "message", "生成失败（退出码 " + code + "）: " + truncate(stderr.toString(), 500),
                        "detail", stdout + "\n" + stderr
                );
            }

            return Map.of(
                    "success", true,
                    "message", "模拟数据已生成（模式：" + ("overwrite".equals(mode) ? "覆盖" : "追加") + "）",
                    "count", count,
                    "quality", quality,
                    "dispersion", dispersion,
                    "mode", mode,
                    "log", truncate(stdout.toString(), 2000)
            );
        } catch (Exception e) {
            log.error("调用生成脚本失败", e);
            return Map.of("success", false, "message", "调用生成脚本失败: " + e.getMessage());
        }
    }

    private static String normalizeLevel(String level) {
        if (level == null || level.isBlank()) {
            return "medium";
        }
        String l = level.trim().toLowerCase();
        if ("高".equals(level) || "high".equals(l)) {
            return "high";
        }
        if ("低".equals(level) || "low".equals(l)) {
            return "low";
        }
        return "medium";
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
