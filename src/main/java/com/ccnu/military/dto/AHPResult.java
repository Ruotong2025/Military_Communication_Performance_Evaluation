package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * AHP计算结果DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AHPResult {
    private double[][] matrix;              // AHP判断矩阵（8x8）
    private Map<String, Double> weights;    // 各维度权重
    private double cr;                      // 一致性比率
    private boolean consistent;             // 是否通过一致性检验（CR < 0.1）
    private String[] dimensions;            // 维度顺序
}
