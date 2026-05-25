package com.ccnu.military.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 指标向量服务
 * 负责调用 Python 脚本计算文本向量，并提供向量相关的工具方法
 *
 * 优化：使用 Python 进程池，避免每次请求都 fork 新进程
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorVectorService {

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${semantic.similarity.python.path:python}")
    private String pythonPath;

    @Value("${semantic.similarity.script.path:scripts/similarity_calculator.py}")
    private String scriptPath;

    @Value("${semantic.similarity.timeout:60}")
    private int timeoutSeconds;

    @Value("${semantic.similarity.pool.size:3}")
    private int poolSize;

    // Python 进程池
    private final Queue<ProcessWrapper> processPool = new ConcurrentLinkedQueue<>();
    private final AtomicInteger waitingRequests = new AtomicInteger(0);
    private ExecutorService executor;
    private volatile boolean shutdown = false;
    private Path scriptFilePath;

    /**
     * 计算单个文本的向量
     *
     * @param text 文本
     * @return 向量列表
     */
    public List<Double> encodeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("texts", Collections.singletonList(text.trim()));
            String jsonInput = objectMapper.writeValueAsString(request);

            String result = executeWithPool(jsonInput);

            if (result == null || result.isEmpty()) {
                log.warn("Python脚本返回空结果");
                return Collections.emptyList();
            }

            Map<String, Object> response = objectMapper.readValue(result,
                    new TypeReference<Map<String, Object>>() {});

            @SuppressWarnings("unchecked")
            List<Number> vectors = (List<Number>) response.get("vectors");
            if (vectors == null || vectors.isEmpty()) {
                return Collections.emptyList();
            }

            List<Double> resultVector = new ArrayList<>();
            List<Number> firstVector = (List<Number>) vectors.get(0);
            for (Number v : firstVector) {
                resultVector.add(v.doubleValue());
            }

            log.debug("向量计算完成: text='{}', 向量维度={}", text, resultVector.size());
            return resultVector;

        } catch (Exception e) {
            log.error("向量计算失败: text={}", text, e);
            return Collections.emptyList();
        }
    }

    /**
     * 批量计算文本向量
     *
     * @param texts 文本列表
     * @return 向量列表（每个文本对应一个向量）
     */
    public List<List<Double>> encodeTexts(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("texts", texts);
            String jsonInput = objectMapper.writeValueAsString(request);

            String result = executeWithPool(jsonInput);

            if (result == null || result.isEmpty()) {
                log.warn("Python脚本返回空结果");
                return Collections.emptyList();
            }

            Map<String, Object> response = objectMapper.readValue(result,
                    new TypeReference<Map<String, Object>>() {});

            @SuppressWarnings("unchecked")
            List<List<Number>> allVectors = (List<List<Number>>) response.get("vectors");

            List<List<Double>> resultList = new ArrayList<>();
            if (allVectors != null) {
                for (List<Number> vector : allVectors) {
                    List<Double> doubleVector = new ArrayList<>();
                    for (Number v : vector) {
                        doubleVector.add(v.doubleValue());
                    }
                    resultList.add(doubleVector);
                }
            }

            log.debug("批量向量计算完成: texts数量={}", texts.size());
            return resultList;

        } catch (Exception e) {
            log.error("批量向量计算失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 反序列化向量（JSON -> List<Double>）
     */
    public List<Double> deserializeVector(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<List<Double>>() {});
        } catch (Exception e) {
            log.error("向量反序列化失败: json长度={}", json.length(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 序列化向量（List<Double> -> JSON）
     */
    public String serializeVector(List<Double> vector) {
        if (vector == null || vector.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(vector);
        } catch (Exception e) {
            log.error("向量序列化失败", e);
            return null;
        }
    }

    /**
     * 计算两个向量的余弦相似度
     *
     * @param v1 向量1
     * @param v2 向量2
     * @return 余弦相似度 (-1 到 1)
     */
    public double computeCosineSimilarity(List<Double> v1, List<Double> v2) {
        if (v1 == null || v2 == null || v1.isEmpty() || v2.isEmpty()) {
            return 0.0;
        }

        if (v1.size() != v2.size()) {
            log.warn("向量维度不一致: v1={}, v2={}", v1.size(), v2.size());
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < v1.size(); i++) {
            dotProduct += v1.get(i) * v2.get(i);
            norm1 += v1.get(i) * v1.get(i);
            norm2 += v2.get(i) * v2.get(i);
        }

        norm1 = Math.sqrt(norm1);
        norm2 = Math.sqrt(norm2);

        if (norm1 < 1e-10 || norm2 < 1e-10) {
            return 0.0;
        }

        return dotProduct / (norm1 * norm2);
    }

    /**
     * 批量计算余弦相似度
     *
     * @param queryVector 查询向量
     * @param candidateVectors 候选向量列表
     * @return 相似度列表
     */
    public List<Double> computeBatchCosineSimilarity(List<Double> queryVector, List<List<Double>> candidateVectors) {
        if (queryVector == null || candidateVectors == null || candidateVectors.isEmpty()) {
            return Collections.emptyList();
        }

        List<Double> similarities = new ArrayList<>();
        for (List<Double> candidate : candidateVectors) {
            similarities.add(computeCosineSimilarity(queryVector, candidate));
        }

        return similarities;
    }

    /**
     * 进程包装器，包含进程及其 I/O 流
     */
    private static class ProcessWrapper {
        final Process process;
        final BufferedWriter writer;
        final BufferedReader reader;
        final BufferedReader errorReader;
        volatile boolean inUse = false;
        volatile long lastUsed = 0;

        ProcessWrapper(Process process, BufferedWriter writer, BufferedReader reader, BufferedReader errorReader) {
            this.process = process;
            this.writer = writer;
            this.reader = reader;
            this.errorReader = errorReader;
        }
    }

    /**
     * 初始化 Python 进程池
     */
    @PostConstruct
    public void initProcessPool() {
        try {
            // 解析脚本路径
            scriptFilePath = Paths.get(scriptPath).toAbsolutePath();
            if (!Files.exists(scriptFilePath)) {
                String projectRoot = System.getProperty("user.dir");
                scriptFilePath = Paths.get(projectRoot, scriptPath).toAbsolutePath();
            }

            if (!Files.exists(scriptFilePath)) {
                log.error("Python脚本不存在: {}", scriptFilePath);
                return;
            }

            executor = Executors.newFixedThreadPool(poolSize);

            // 预启动进程池
            for (int i = 0; i < poolSize; i++) {
                ProcessWrapper wrapper = createPythonProcess();
                if (wrapper != null) {
                    processPool.offer(wrapper);
                    log.info("Python进程池初始化: 第 {} 个进程已启动", i + 1);
                } else {
                    log.warn("Python进程池初始化: 第 {} 个进程启动失败", i + 1);
                }
            }

            log.info("Python进程池初始化完成，池大小: {}", processPool.size());

        } catch (Exception e) {
            log.error("Python进程池初始化失败", e);
        }
    }

    /**
     * 创建单个 Python 进程
     */
    private ProcessWrapper createPythonProcess() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    pythonPath,
                    scriptFilePath.toString(),
                    "--persistent"
            );
            pb.directory(scriptFilePath.getParent().toFile());
            pb.redirectErrorStream(false);

            Map<String, String> env = pb.environment();
            env.put("PYTHONIOENCODING", "utf-8");

            Process process = pb.start();

            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream(), "UTF-8"));
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"));
            BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), "UTF-8"));

            // 等待进程就绪（读取一行就绪信号）
            String ready = reader.readLine();
            if (!"READY".equals(ready)) {
                // 读取错误信息
                StringBuilder error = new StringBuilder();
                String line;
                while ((line = errorReader.readLine()) != null) {
                    error.append(line).append("\n");
                }
                log.error("Python进程启动失败: {}", error);
                process.destroyForcibly();
                return null;
            }

            log.debug("Python进程已就绪: pid={}", process.pid());
            return new ProcessWrapper(process, writer, reader, errorReader);

        } catch (Exception e) {
            log.error("创建Python进程失败", e);
            return null;
        }
    }

    /**
     * 获取可用的 Python 进程
     */
    private synchronized ProcessWrapper acquireProcess() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long maxWait = Math.max(timeoutSeconds * 1000L, 5000L);

        while (true) {
            // 尝试从池中获取空闲进程
            Iterator<ProcessWrapper> iterator = processPool.iterator();
            while (iterator.hasNext()) {
                ProcessWrapper wrapper = iterator.next();
                if (!wrapper.inUse && wrapper.process.isAlive()) {
                    wrapper.inUse = true;
                    wrapper.lastUsed = System.currentTimeMillis();
                    return wrapper;
                }
                // 如果进程已死，移除并重建
                if (!wrapper.process.isAlive()) {
                    closeWrapper(wrapper);
                    iterator.remove();
                    ProcessWrapper newWrapper = createPythonProcess();
                    if (newWrapper != null) {
                        newWrapper.inUse = true;
                        newWrapper.lastUsed = System.currentTimeMillis();
                        processPool.offer(newWrapper);
                        return newWrapper;
                    }
                }
            }

            // 如果池未满，创建新进程
            if (processPool.size() < poolSize) {
                ProcessWrapper newWrapper = createPythonProcess();
                if (newWrapper != null) {
                    newWrapper.inUse = true;
                    newWrapper.lastUsed = System.currentTimeMillis();
                    processPool.offer(newWrapper);
                    return newWrapper;
                }
            }

            // 等待或超时
            if (System.currentTimeMillis() - startTime > maxWait) {
                throw new RuntimeException("获取Python进程超时，等待队列: " + waitingRequests.get());
            }
            Thread.sleep(50);
        }
    }

    /**
     * 释放 Python 进程回池
     */
    private synchronized void releaseProcess(ProcessWrapper wrapper) {
        if (wrapper != null) {
            wrapper.inUse = false;
            wrapper.lastUsed = System.currentTimeMillis();
        }
    }

    /**
     * 使用进程池执行脚本
     */
    private String executeWithPool(String input) throws Exception {
        if (shutdown || processPool.isEmpty()) {
            // 降级到普通模式
            return executePythonScriptDirect(input);
        }

        ProcessWrapper wrapper = acquireProcess();
        try {
            // 发送输入
            wrapper.writer.write(input);
            wrapper.writer.newLine();
            wrapper.writer.flush();

            // 读取输出
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = wrapper.reader.readLine()) != null) {
                if ("DONE".equals(line)) {
                    break;
                }
                output.append(line);
            }

            return output.toString();

        } catch (Exception e) {
            // 如果执行失败，销毁进程并重建
            closeWrapper(wrapper);
            removeFromPool(wrapper);
            throw e;
        } finally {
            releaseProcess(wrapper);
        }
    }

    /**
     * 直接执行脚本（无进程池，用于降级）
     */
    private String executePythonScriptDirect(String input) throws Exception {
        log.debug("使用直接模式执行Python脚本");

        ProcessBuilder pb = new ProcessBuilder(
                pythonPath,
                scriptFilePath.toString(),
                "--encode"
        );
        pb.directory(scriptFilePath.getParent().toFile());
        pb.redirectErrorStream(false);

        Map<String, String> env = pb.environment();
        env.put("PYTHONIOENCODING", "utf-8");

        Process process = pb.start();

        try (OutputStream os = process.getOutputStream()) {
            os.write(input.getBytes("UTF-8"));
            os.flush();
        }

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Python脚本执行超时");
        }

        return output.toString();
    }

    private void removeFromPool(ProcessWrapper wrapper) {
        processPool.remove(wrapper);
    }

    private void closeWrapper(ProcessWrapper wrapper) {
        try {
            wrapper.writer.close();
            wrapper.reader.close();
            wrapper.errorReader.close();
            if (wrapper.process.isAlive()) {
                wrapper.process.destroyForcibly();
            }
        } catch (Exception e) {
            log.debug("关闭进程包装器时出错", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        shutdown = true;
        log.info("关闭Python进程池...");

        if (executor != null) {
            executor.shutdownNow();
        }

        for (ProcessWrapper wrapper : processPool) {
            closeWrapper(wrapper);
        }
        processPool.clear();

        log.info("Python进程池已关闭");
    }
}
