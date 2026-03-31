package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 矩阵计算结果DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatrixCalculationResult {
    
    /** 维度层结果 */
    private MatrixResult dimensionResult;
    
    /** 各维度指标层结果 */
    private Map<String, MatrixResult> indicatorResults;
    
    /** 综合权重列表（维度权重 × 指标权重），每个元素包含维度名、指标名及综合权重值 */
    private List<CombinedWeight> combinedWeights;
    
    /**
     * 单个矩阵计算结果
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatrixResult {
        /** 完整的判断矩阵（二维数组） */
        private double[][] matrix;

        /** 各元素权重 */
        private double[] weights;

        /** 一致性比率 CR */
        private double cr;

        /** 是否通过一致性检验 (CR < 0.1) */
        private boolean consistent;

        /** 最大特征值 λmax */
        private double lambdaMax;

        /** 一致性指标 CI */
        private double ci;

        /** 随机一致性指标 RI */
        private double ri;

        /** 元素名称列表（按矩阵行列顺序） */
        private List<String> elementNames;

        /** 权重映射 {name: weight} */
        private Map<String, Double> weightMap;
    }

    /**
     * 综合权重（维度权重 × 指标权重）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CombinedWeight {
        /** 维度名称 */
        private String dimension;
        /** 指标名称 */
        private String indicator;
        /** 维度层权重 */
        private double dimensionWeight;
        /** 指标层权重 */
        private double indicatorWeight;
        /** 综合权重 = 维度权重 × 指标权重 */
        private double combinedWeight;
    }
}
