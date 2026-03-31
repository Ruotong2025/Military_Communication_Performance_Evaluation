package com.ccnu.military.service;

import com.ccnu.military.dto.AHPResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * AHP层次分析法服务
 */
@Slf4j
@Service
public class AHPService {

    // 维度顺序（按优先级排序）
    private static final String[] DIMENSIONS = {"RL", "SC", "AJ", "EF", "PO", "NC", "HO", "RS"};
    
    // RI值表（随机一致性指标）
    private static final double[] RI = {0, 0, 0.58, 0.90, 1.12, 1.24, 1.32, 1.41, 1.45, 1.49};

    /**
     * 计算AHP权重
     */
    public AHPResult calculate(Map<String, Integer> priorities) {
        // 1. 生成判断矩阵
        double[][] matrix = generateJudgmentMatrix(priorities);
        
        // 2. 计算权重（特征向量法）
        double[] weights = calculateWeights(matrix);
        
        // 3. 一致性检验
        double cr = calculateConsistencyRatio(matrix, weights);
        
        // 4. 构建结果
        Map<String, Double> weightMap = new LinkedHashMap<>();
        for (int i = 0; i < DIMENSIONS.length; i++) {
            weightMap.put(DIMENSIONS[i], weights[i]);
        }
        
        AHPResult result = new AHPResult();
        result.setMatrix(matrix);
        result.setWeights(weightMap);
        result.setCr(cr);
        result.setConsistent(cr < 0.1);
        result.setDimensions(DIMENSIONS);
        
        log.info("AHP计算完成 - CR={}, 一致性={}", cr, result.isConsistent());
        
        return result;
    }

    /**
     * 生成判断矩阵
     * 根据优先级差值生成判断值：
     * 差1 -> 2, 差2 -> 3, 差3 -> 4, 差4 -> 5, 差5 -> 6, 差6 -> 7, 差7 -> 8
     */
    private double[][] generateJudgmentMatrix(Map<String, Integer> priorities) {
        int n = DIMENSIONS.length;
        double[][] matrix = new double[n][n];
        
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    matrix[i][j] = 1.0;
                } else {
                    int priorityI = priorities.get(DIMENSIONS[i]);
                    int priorityJ = priorities.get(DIMENSIONS[j]);
                    int diff = priorityJ - priorityI;  // 优先级差值
                    
                    if (diff > 0) {
                        // i的优先级高于j，判断值为 diff+1
                        matrix[i][j] = diff + 1.0;
                    } else {
                        // i的优先级低于j，判断值为 1/(|diff|+1)
                        matrix[i][j] = 1.0 / (Math.abs(diff) + 1.0);
                    }
                }
            }
        }
        
        return matrix;
    }

    /**
     * 计算权重（特征值法 - 列和归一化/算术平均法）
     * 步骤：
     *   1. 将判断矩阵每一列归一化（元素/列和）
     *   2. 按行求算术平均，即得权重向量
     *   3. 最后归一化，确保权重和为1
     */
    private double[] calculateWeights(double[][] matrix) {
        int n = matrix.length;

        // Step 1: 计算每列的列和
        double[] colSums = new double[n];
        for (int j = 0; j < n; j++) {
            double sum = 0;
            for (int i = 0; i < n; i++) {
                sum += matrix[i][j];
            }
            colSums[j] = sum;
        }

        // Step 2: 列归一化后按行求算术平均
        double[] rawWeights = new double[n];
        for (int i = 0; i < n; i++) {
            double rowSum = 0;
            for (int j = 0; j < n; j++) {
                if (colSums[j] > 0) {
                    rowSum += matrix[i][j] / colSums[j];
                }
            }
            rawWeights[i] = rowSum / n;
        }

        // Step 3: 归一化，使权重和为1
        double total = 0;
        for (double w : rawWeights) total += w;
        if (total > 0) {
            for (int i = 0; i < n; i++) rawWeights[i] /= total;
        }

        return rawWeights;
    }

    /**
     * 计算一致性比率 CR = CI / RI
     */
    private double calculateConsistencyRatio(double[][] matrix, double[] weights) {
        int n = matrix.length;
        
        // 计算最大特征值 λmax
        double lambdaMax = 0;
        for (int i = 0; i < n; i++) {
            double sum = 0;
            for (int j = 0; j < n; j++) {
                sum += matrix[i][j] * weights[j];
            }
            lambdaMax += sum / weights[i];
        }
        lambdaMax /= n;
        
        // 计算一致性指标 CI
        double ci = (lambdaMax - n) / (n - 1);
        
        // 计算一致性比率 CR
        double cr = ci / RI[n];
        
        return cr;
    }
}
