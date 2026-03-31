package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 矩阵计算请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatrixCalculationRequest {
    
    /**
     * 维度层判断矩阵
     * 格式: [["维度1_维度2", 2.0], ...]
     * key = "i_j"（上三角任意位置），value = 标度值(1-9)
     */
    private List<MatrixEntry> dimensionMatrix;
    
    /**
     * 各维度内部指标层判断矩阵
     * 格式: {
     *   "安全性": [["密钥泄露_被侦察", 2.0], ...],
     *   "可靠性": [["崩溃比例_恢复能力", 1.0], ...],
     *   ...
     * }
     */
    private Map<String, List<MatrixEntry>> indicatorMatrices;
    
    /**
     * 矩阵条目
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatrixEntry {
        /** 比较对标识，如 "安全性_可靠性" 或 "密钥泄露_被侦察" */
        private String key;
        /** 正互反标度 aᵢⱼ∈[1/9,9]：&gt;1 行比列重要，&lt;1 行不如列重要 */
        private Double score;
        /** 把握度 (0~1)，可选 */
        private Double confidence;
    }
}
