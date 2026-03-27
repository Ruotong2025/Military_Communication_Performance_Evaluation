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

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 调用 Python 脚本生成 records_* 四表模拟数据
 */
@Slf4j
@Service
public class CombatSimulationService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DataSource dataSource;

    public CombatSimulationService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Value("${python.executable:python}")
    private String pythonExecutable;

    @Value("${spring.datasource.username:root}")
    private String dbUsername;

    @Value("${spring.datasource.password:root}")
    private String dbPassword;

    @Value("${python.combat-data-script:military_operational_effectiveness_evaluation/generate/generate_all_data.py}")
    private String combatDataScript;

    @Value("${python.combat-timeout:600}")
    private long combatTimeoutSeconds;

    public Map<String, Object> generateSimulation(CombatSimulationRequest req) {
        int count = req.getCount() == null ? 10 : Math.max(1, Math.min(100, req.getCount()));
        String quality = normalizeLevel(req.getExcellentLevel());
        String dispersion = normalizeLevel(req.getDispersionLevel());
        String seed = req.getSeed() == null ? "" : req.getSeed().trim();
        String mode = (req.getMode() == null || req.getMode().trim().isEmpty())
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

        // 支持绝对路径和相对路径
        File scriptFile;
        if (new File(combatDataScript).isAbsolute()) {
            scriptFile = new File(combatDataScript);
        } else {
            scriptFile = new File(System.getProperty("user.dir"), combatDataScript);
        }
        scriptFile = scriptFile.getAbsoluteFile();
        if (!scriptFile.isFile()) {
            log.error("未找到生成脚本: {}", scriptFile.getAbsolutePath());
            return createResult(false, "未找到生成脚本: " + scriptFile.getAbsolutePath());
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
            // 从 DataSource 获取数据库连接信息
            String jdbcUrl = "";
            String dbHost = "localhost";
            String dbPort = "3306";
            String dbName = "military_operational_effectiveness_evaluation";

            try (Connection conn = dataSource.getConnection()) {
                jdbcUrl = conn.getMetaData().getURL();
                // 解析 jdbc:mysql://host:port/dbname
                if (jdbcUrl != null && jdbcUrl.startsWith("jdbc:mysql://")) {
                    String rest = jdbcUrl.substring("jdbc:mysql://".length());
                    int slashIdx = rest.indexOf('/');
                    if (slashIdx > 0) {
                        String hostPart = rest.substring(0, slashIdx);
                        dbName = rest.substring(slashIdx + 1).split("\\?")[0];
                        int colonIdx = hostPart.indexOf(':');
                        if (colonIdx > 0) {
                            dbHost = hostPart.substring(0, colonIdx);
                            dbPort = hostPart.substring(colonIdx + 1);
                        } else {
                            dbHost = hostPart;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("无法从 DataSource 获取连接URL，使用默认值", e);
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(System.getProperty("user.dir")));
            Map<String, String> env = pb.environment();
            env.put("PYTHONIOENCODING", "utf-8");
            // 通过环境变量传递数据库配置
            env.put("DB_HOST", dbHost);
            env.put("DB_PORT", dbPort);
            env.put("DB_NAME", dbName);
            env.put("DB_USER", dbUsername);
            env.put("DB_PASS", dbPassword);
            log.info("数据库配置: {}@{}:{}/{}", dbUsername, dbHost, dbPort, dbName);
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
                return createResult(false, "生成超时（" + combatTimeoutSeconds + " 秒）");
            }

            int code = process.exitValue();
            if (code != 0) {
                log.error("Python 退出码 {}，stderr: {}", code, stderr);
                return createResult(false, "生成失败（退出码 " + code + "）: " + truncate(stderr.toString(), 500), "detail", stdout + "\n" + stderr);
            }

            return createResult(true, "模拟数据已生成（模式：" + ("overwrite".equals(mode) ? "覆盖" : "追加") + "）",
                    "count", count, "quality", quality, "dispersion", dispersion, "mode", mode, "log", truncate(stdout.toString(), 2000));
        } catch (Exception e) {
            log.error("调用生成脚本失败", e);
            return createResult(false, "调用生成脚本失败: " + e.getMessage());
        }
    }

    private static Map<String, Object> createResult(boolean success, String message, Object... kvs) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("success", success);
        result.put("message", message);
        for (int i = 0; i < kvs.length; i += 2) {
            result.put(String.valueOf(kvs[i]), kvs[i + 1]);
        }
        return result;
    }

    private static String normalizeLevel(String level) {
        if (level == null || level.trim().isEmpty()) {
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
