package com.ccnu.military.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Chroma 向量存储配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "chroma")
public class ChromaConfig {

    /**
     * Python 解释器路径
     * Windows: python 或 python.exe
     * Linux/Mac: python3
     */
    private String pythonPath = "python";

    /**
     * Chroma 管理脚本路径
     */
    private String scriptPath = "scripts/chroma_manager.py";

    /**
     * Chroma 持久化存储目录
     */
    private String persistPath = "chroma_storage";

    /**
     * 命令执行超时时间（秒）
     */
    private int timeout = 60;

    /**
     * 向量维度
     * paraphrase-multilingual-MiniLM-L12-v2 输出 384 维
     */
    private int dimension = 384;

    /**
     * 进程池大小
     * 默认 3 个持久化进程
     */
    private int poolSize = 3;
}
