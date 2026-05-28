package com.ccnu.military.service;

import com.ccnu.military.config.ChromaConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeoutException;

/**
 * Chroma 向量服务（优化版）
 * 使用进程池 + 持久化模式提升性能
 */
@Slf4j
@Service
public class ChromaVectorService {

    private final ChromaConfig config;
    private final ObjectMapper objectMapper;

    private Path scriptFilePath;
    private Path persistPath;

    // 进程池配置
    private ExecutorService executor;
    private final List<PersistentProcess> processPool = new CopyOnWriteArrayList<>();
    private final BlockingQueue<PersistentProcess> availableProcesses = new LinkedBlockingQueue<>();
    private final AtomicInteger poolIndex = new AtomicInteger(0);

    // 进程池配置参数
    private static final int DEFAULT_POOL_SIZE = 5;  // 从3改为5，提高并发能力
    private static final int PROCESS_TIMEOUT_SECONDS = 30;
    private static final int MAX_RETRIES = 3;
    private static final int PROCESS_STARTUP_TIMEOUT_MS = 15000;  // 进程启动超时 15秒
    private static final int READ_TIMEOUT_MS = 10000;  // 读取响应超时 10秒

    public ChromaVectorService(ChromaConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        scriptFilePath = Paths.get(config.getScriptPath()).toAbsolutePath();
        if (!Files.exists(scriptFilePath)) {
            String projectRoot = System.getProperty("user.dir");
            scriptFilePath = Paths.get(projectRoot, config.getScriptPath()).toAbsolutePath();
        }

        persistPath = Paths.get(config.getPersistPath()).toAbsolutePath();
        try {
            if (!Files.exists(persistPath)) {
                Files.createDirectories(persistPath);
                log.info("创建 Chroma 存储目录: {}", persistPath);
            }
        } catch (IOException e) {
            log.warn("创建 Chroma 存储目录失败: {}", e.getMessage());
        }

        // 初始化进程池
        int poolSize = config.getPoolSize() > 0 ? config.getPoolSize() : DEFAULT_POOL_SIZE;
        initProcessPool(poolSize);

        log.info("Chroma 管理脚本路径: {}", scriptFilePath);
        log.info("Chroma 存储目录: {}", persistPath);
        log.info("Chroma 进程池大小: {}", poolSize);
    }

    /**
     * 初始化进程池
     */
    private void initProcessPool(int poolSize) {
        executor = Executors.newFixedThreadPool(poolSize);

        for (int i = 0; i < poolSize; i++) {
            try {
                PersistentProcess process = startPersistentProcess(i);
                processPool.add(process);
                availableProcesses.offer(process);
                log.info("启动 Chroma 持久化进程 {} 成功", i);
            } catch (Exception e) {
                log.error("启动 Chroma 持久化进程 {} 失败", i, e);
            }
        }

        if (processPool.isEmpty()) {
            log.warn("Chroma 进程池为空，将使用单次命令模式");
        } else {
            // 预热进程池
            warmupProcessPool();
        }
    }

    /**
     * 预热进程池 - 确保所有进程都能正常响应
     */
    private void warmupProcessPool() {
        log.info("开始预热 Chroma 进程池...");
        int successCount = 0;

        for (PersistentProcess process : processPool) {
            try {
                // 发送预热命令
                String warmupCmd = objectMapper.writeValueAsString(Map.of(
                        "command", "warmup",
                        "payload", Collections.emptyMap()
                ));
                process.write(warmupCmd);

                // 读取响应（带超时）
                String response = process.read();
                if (response != null && response.contains("\"success\":true")) {
                    successCount++;
                    log.debug("进程 {} 预热成功", process.index);
                }
            } catch (Exception e) {
                log.warn("进程 {} 预热失败: {}", process.index, e.getMessage());
            }
        }

        log.info("Chroma 进程池预热完成: {}/{} 进程就绪", successCount, processPool.size());
    }

    /**
     * 启动一个持久化 Python 进程
     */
    private PersistentProcess startPersistentProcess(int index) throws IOException {
        List<String> cmd = Arrays.asList(
                config.getPythonPath(),
                scriptFilePath.toString(),
                "--persistent"
        );

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File(System.getProperty("user.dir")));
        pb.environment().put("CHROMA_PERSIST_DIR", persistPath.toString());
        pb.environment().put("PYTHONIOENCODING", "utf-8");
        pb.environment().put("PYTHONUNBUFFERED", "1");

        Process process = pb.start();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));

        // 等待进程就绪
        String readyLine = reader.readLine();
        if (!"READY".equals(readyLine)) {
            process.destroyForcibly();
            throw new IOException("Chroma 进程启动失败，未收到 READY 信号");
        }

        return new PersistentProcess(process, reader, writer, index);
    }

    @PreDestroy
    public void shutdown() {
        log.info("关闭 Chroma 进程池...");

        // 停止接收新任务
        executor.shutdown();

        // 关闭所有持久化进程
        for (PersistentProcess p : processPool) {
            try {
                p.shutdown();
            } catch (Exception e) {
                log.warn("关闭进程 {} 失败", p.index, e);
            }
        }

        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("Chroma 进程池已关闭");
    }

    // ==================== 指标向量操作 ====================

    public void addIndicators(List<Long> ids, List<String> names, List<List<Double>> vectors) {
        Map<String, Object> data = new HashMap<>();
        data.put("ids", ids);
        data.put("names", names);
        data.put("vectors", vectors);

        String result = executePersistentCommand("add-indicators", data);
        Map<String, Object> response = parseResult(result);

        if (!Boolean.TRUE.equals(response.get("success"))) {
            log.error("批量添加指标向量失败: {}", response.get("error"));
            throw new RuntimeException("批量添加指标向量失败: " + response.get("error"));
        }
        log.info("批量添加指标向量成功: {} 条", ids.size());
    }

    public List<SearchResult> searchIndicators(List<Double> queryVector, int topK) {
        Map<String, Object> data = new HashMap<>();
        data.put("query", queryVector);
        data.put("top_k", topK);

        String result = executePersistentCommand("search-indicators", data);
        return parseSearchResults(result);
    }

    public void deleteIndicator(Long id) {
        Map<String, Object> data = Map.of("id", id);
        executePersistentCommand("delete-indicator", data);
    }

    public void clearIndicators() {
        executePersistentCommand("clear-indicators", Collections.emptyMap());
        log.info("指标集合已清空");
    }

    public int getIndicatorCount() {
        Map<String, Object> response = parseResult(
                executePersistentCommand("stats", Collections.emptyMap()));
        if (Boolean.TRUE.equals(response.get("success"))) {
            return ((Number) response.getOrDefault("indicator_count", 0)).intValue();
        }
        return 0;
    }

    // ==================== 数据源向量操作 ====================

    public void addSourceData(List<Long> ids, List<String> names, List<List<Double>> vectors) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            items.add(Map.of(
                    "id", ids.get(i),
                    "name", names.get(i),
                    "vector", vectors.get(i)
            ));
        }

        Map<String, Object> data = Map.of("items", items);
        String result = executePersistentCommand("add-source-data", data);
        Map<String, Object> response = parseResult(result);

        if (!Boolean.TRUE.equals(response.get("success"))) {
            log.error("批量添加数据源向量失败: {}", response.get("error"));
            throw new RuntimeException("批量添加数据源向量失败: " + response.get("error"));
        }
        log.info("批量添加数据源向量成功: {} 条", ids.size());
    }

    public List<SearchResult> searchSourceData(List<Double> queryVector, int topK) {
        Map<String, Object> data = new HashMap<>();
        data.put("query", queryVector);
        data.put("top_k", topK);

        String result = executePersistentCommand("search-source-data", data);
        return parseSearchResults(result);
    }

    public void deleteSourceData(Long id) {
        Map<String, Object> data = Map.of("id", id);
        executePersistentCommand("delete-source-data", data);
    }

    public int getSourceDataCount() {
        Map<String, Object> response = parseResult(
                executePersistentCommand("stats", Collections.emptyMap()));
        if (Boolean.TRUE.equals(response.get("success"))) {
            return ((Number) response.getOrDefault("source_data_count", 0)).intValue();
        }
        return 0;
    }

    // ==================== 统计信息 ====================

    public Map<String, Object> getStats() {
        return parseResult(executePersistentCommand("stats", Collections.emptyMap()));
    }

    public void reset() {
        executePersistentCommand("reset", Collections.emptyMap());
        log.info("Chroma 数据已重置");
    }

    /**
     * 重建索引
     * 用于应用新的距离函数配置（如从 L2 改为 cosine）
     *
     * @param collection 要重建的集合: "indicators", "source_data", "all"
     */
    public void rebuildIndex(String collection) {
        Map<String, Object> data = Map.of("collection", collection);
        String result = executePersistentCommand("rebuild-index", data);
        Map<String, Object> response = parseResult(result);

        if (Boolean.TRUE.equals(response.get("success"))) {
            log.info("索引重建成功: {}", response.get("message"));
        } else {
            log.error("索引重建失败: {}", response.get("error"));
            throw new RuntimeException("索引重建失败: " + response.get("error"));
        }
    }

    // ==================== 核心方法 ====================

    /**
     * 通过持久化进程执行命令
     */
    private String executePersistentCommand(String command, Map<String, Object> data) {
        if (processPool.isEmpty()) {
            log.warn("进程池为空，尝试使用单次命令模式");
            return executeSingleCommand(command, data);
        }

        PersistentProcess process = null;
        int retries = 0;
        String result = null;

        try {
            while (retries < MAX_RETRIES) {
                try {
                    // 从池中获取可用进程
                    process = availableProcesses.take();

                    // 发送命令
                    String request = objectMapper.writeValueAsString(Map.of(
                            "command", command,
                            "payload", data
                    ));
                    process.write(request);

                    // 读取响应
                    String response = process.read();
                    Map<String, Object> parsed = parseResult(response);

                    // 检查是否成功
                    if (Boolean.TRUE.equals(parsed.get("success"))) {
                        result = response;
                        return result;
                    }

                    // 如果是连接错误，重试
                    String error = (String) parsed.get("error");
                    if (error != null && (error.contains("Connection") || error.contains("Broken pipe"))) {
                        log.warn("进程 {} 连接异常，尝试重启", process.index);
                        restartProcess(process);
                        process = null; // 已重启，不再归还
                        retries++;
                        continue;
                    }

                    result = response;
                    return result;

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("等待进程池时被中断", e);
                } catch (Exception e) {
                    log.error("执行命令失败", e);
                    if (process != null) {
                        restartProcess(process);
                        process = null; // 已重启，不再归还
                    }
                    retries++;
                }
            }
        } finally {
            // 关键修复：归还进程回池
            if (process != null) {
                availableProcesses.offer(process);
            }
        }

        // 重试失败，尝试单次命令模式
        log.warn("持久化模式执行失败，尝试单次命令模式");
        return executeSingleCommand(command, data);
    }

    /**
     * 单次命令模式（后备方案）
     */
    private String executeSingleCommand(String command, Map<String, Object> data) {
        try {
            Map<String, String> env = new HashMap<>(System.getenv());
            env.put("CHROMA_PERSIST_DIR", persistPath.toString());
            env.put("PYTHONIOENCODING", "utf-8");

            List<String> cmd = Arrays.asList(
                    config.getPythonPath(),
                    scriptFilePath.toString(),
                    command
            );

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new File(System.getProperty("user.dir")));
            pb.environment().putAll(env);

            Process process = pb.start();

            String jsonData = objectMapper.writeValueAsString(data);
            try (OutputStream os = process.getOutputStream()) {
                os.write(jsonData.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            boolean finished = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Chroma 命令执行超时");
            }

            return output.toString();

        } catch (Exception e) {
            log.error("单次命令模式执行失败", e);
            return "{\"success\": false, \"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 重启一个持久化进程
     */
    private void restartProcess(PersistentProcess oldProcess) {
        try {
            oldProcess.shutdown();
            processPool.remove(oldProcess);
            availableProcesses.remove(oldProcess);

            PersistentProcess newProcess = startPersistentProcess(poolIndex.incrementAndGet());
            processPool.add(newProcess);
            availableProcesses.offer(newProcess);

            log.info("进程 {} 已重启", newProcess.index);
        } catch (Exception e) {
            log.error("重启进程失败", e);
        }
    }

    /**
     * 解析搜索结果
     */
    private List<SearchResult> parseSearchResults(String json) {
        List<SearchResult> results = new ArrayList<>();
        try {
            Map<String, Object> response = parseResult(json);
            if (Boolean.TRUE.equals(response.get("success"))) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rawResults =
                        (List<Map<String, Object>>) response.get("results");
                if (rawResults != null) {
                    for (Map<String, Object> r : rawResults) {
                        SearchResult sr = new SearchResult();
                        sr.setId(((Number) r.get("id")).longValue());
                        // Chroma cosine 距离范围 [0, 2]，直接转换为相似度
                        Double distance = ((Number) r.get("distance")).doubleValue();
                        sr.setScore(1.0 - distance);
                        sr.setMetadata(r.get("metadata"));
                        results.add(sr);
                    }
                }
            } else {
                log.warn("搜索失败: {}", response.get("error"));
            }
        } catch (Exception e) {
            log.error("解析搜索结果失败", e);
        }
        return results;
    }

    private Map<String, Object> parseResult(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("解析 JSON 失败: {}", json, e);
            return Collections.singletonMap("success", false);
        }
    }

    // ==================== 内部类 ====================

    /**
     * 持久化进程封装
     */
    private static class PersistentProcess {
        final Process process;
        final BufferedReader reader;
        final BufferedWriter writer;
        final int index;
        volatile long lastUsed = 0;

        PersistentProcess(Process process, BufferedReader reader, BufferedWriter writer, int index) {
            this.process = process;
            this.reader = reader;
            this.writer = writer;
            this.index = index;
            this.lastUsed = System.currentTimeMillis();
        }

        synchronized void write(String data) throws IOException {
            writer.write(data);
            writer.newLine();
            writer.flush();
            lastUsed = System.currentTimeMillis();
        }

        synchronized String read() throws IOException {
            return reader.readLine();
        }

        /**
         * 带超时的读取方法
         * @param timeoutMs 超时时间（毫秒）
         * @return 读取的行内容
         * @throws TimeoutException 超时异常
         */
        synchronized String readWithTimeout(long timeoutMs) throws IOException, TimeoutException {
            long startTime = System.currentTimeMillis();
            long deadline = startTime + timeoutMs;

            while (System.currentTimeMillis() < deadline) {
                if (reader.ready()) {
                    String line = reader.readLine();
                    if (line != null) {
                        lastUsed = System.currentTimeMillis();
                        return line;
                    }
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("读取被中断", e);
                }
            }
            throw new TimeoutException("读取超时");
        }

        /**
         * 检查进程是否还活着
         */
        boolean isAlive() {
            return process.isAlive();
        }

        void shutdown() {
            try {
                write("{\"command\": \"SHUTDOWN\", \"payload\": {}}");
                if (!process.waitFor(3, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (Exception e) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * 搜索结果
     */
    @lombok.Data
    public static class SearchResult {
        private Long id;
        private Double score;
        private Object metadata;
    }
}
