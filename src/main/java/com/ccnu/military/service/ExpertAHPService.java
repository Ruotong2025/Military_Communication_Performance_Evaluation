package com.ccnu.military.service;

import com.ccnu.military.dto.MatrixCalculationRequest;
import com.ccnu.military.dto.MatrixCalculationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

import java.util.ArrayList;

/**
 * AHP层次分析法服务（支持直接矩阵计算）
 */
@Slf4j
@Service
public class ExpertAHPService {

    // RI值表（随机一致性指标）
    private static final double[] RI = {0, 0, 0, 0.58, 0.90, 1.12, 1.24, 1.32, 1.41, 1.45, 1.49, 1.51, 1.54, 1.56, 1.57, 1.58};

    // 维度名称列表（5维）
    private static final String[] DIMENSIONS = {"安全性", "可靠性", "传输能力", "抗干扰能力", "效能影响"};

    // 指标层维度定义
    private static final Map<String, String[]> DIMENSION_INDICATORS;

    static {
        DIMENSION_INDICATORS = new LinkedHashMap<>();
        DIMENSION_INDICATORS.put("安全性", new String[]{"密钥泄露得分", "被侦察得分", "抗拦截得分"});
        DIMENSION_INDICATORS.put("可靠性", new String[]{"崩溃比例得分", "恢复能力得分", "通信可用得分"});
        DIMENSION_INDICATORS.put("传输能力", new String[]{"带宽得分", "呼叫建立得分", "传输时延得分", "误码率得分", "吞吐量得分", "频谱效率得分"});
        DIMENSION_INDICATORS.put("抗干扰能力", new String[]{"信干噪比得分", "抗干扰余量得分", "通信距离得分"});
        DIMENSION_INDICATORS.put("效能影响", new String[]{"战损率得分", "任务完成率得分", "致盲率得分"});
    }

    /**
     * 根据比较对数据构建完整判断矩阵
     * @param elements 元素名称列表（按行列顺序）
     * @param entries 比较对数据 [{key: "A_B", score: 2.0}, ...]
     * @return 完整的 n×n 判断矩阵
     */
    public double[][] buildMatrix(String[] elements, List<MatrixCalculationRequest.MatrixEntry> entries) {
        int n = elements.length;
        double[][] matrix = new double[n][n];
        
        // 初始化为单位矩阵
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = (i == j) ? 1.0 : 0.0;
            }
        }
        
        // 构建元素名到索引的映射
        Map<String, Integer> nameToIndex = new HashMap<>();
        for (int i = 0; i < elements.length; i++) {
            nameToIndex.put(elements[i], i);
        }
        
        // 填充矩阵（上三角或下三角任意位置）
        if (entries != null) {
            for (MatrixCalculationRequest.MatrixEntry entry : entries) {
                String key = entry.getKey();
                Double score = entry.getScore();
                if (key == null || score == null) continue;
                
                // 解析 key，格式可能是 "A_B" 或 "安全性_可靠性"
                String itemA, itemB;
                if (key.contains("_")) {
                    String[] parts = key.split("_", 2);
                    itemA = parts[0];
                    itemB = parts[1];
                } else {
                    continue;
                }
                
                Integer idxA = nameToIndex.get(itemA);
                Integer idxB = nameToIndex.get(itemB);
                
                if (idxA != null && idxB != null) {
                    matrix[idxA][idxB] = score;
                    matrix[idxB][idxA] = 1.0 / score;  // 自动生成倒数
                }
            }
        }
        
        return matrix;
    }

    /**
     * 计算特征向量权重（特征值法 - 列和归一化/算术平均法）
     * 步骤：
     *   1. 将判断矩阵每一列归一化（元素/列和）
     *   2. 按行求算术平均，即得权重向量
     *   3. 最后再归一化，确保权重和为1
     * 此法等价于先求最大特征值对应的特征向量，在正互反矩阵下近似最优。
     */
    public double[] calculateWeights(double[][] matrix) {
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
     * 计算一致性比率
     */
    public double calculateLambdaMax(double[][] matrix, double[] weights) {
        int n = matrix.length;
        double lambdaMax = 0;
        for (int i = 0; i < n; i++) {
            double sum = 0;
            for (int j = 0; j < n; j++) {
                sum += matrix[i][j] * weights[j];
            }
            if (weights[i] > 0) {
                lambdaMax += sum / weights[i];
            }
        }
        return lambdaMax / n;
    }

    /**
     * 计算一致性指标
     */
    public double calculateCI(double lambdaMax, int n) {
        return (lambdaMax - n) / (n - 1);
    }

    /**
     * 获取RI值
     */
    public double getRI(int n) {
        if (n < RI.length) {
            return RI[n];
        }
        return 1.59;  // 默认值
    }

    /**
     * 计算单个矩阵的完整结果
     */
    public MatrixCalculationResult.MatrixResult calculateSingleMatrix(
            String[] elements, 
            List<MatrixCalculationRequest.MatrixEntry> entries) {
        
        // 构建矩阵
        double[][] matrix = buildMatrix(elements, entries);
        
        // 计算权重
        double[] weights = calculateWeights(matrix);
        
        // 计算一致性
        double lambdaMax = calculateLambdaMax(matrix, weights);
        double ci = calculateCI(lambdaMax, elements.length);
        double ri = getRI(elements.length);
        double cr = (ri > 0) ? ci / ri : 0;
        
        // 构建权重映射
        Map<String, Double> weightMap = new LinkedHashMap<>();
        for (int i = 0; i < elements.length; i++) {
            weightMap.put(elements[i], weights[i]);
        }
        
        MatrixCalculationResult.MatrixResult result = new MatrixCalculationResult.MatrixResult();
        result.setMatrix(matrix);
        result.setWeights(weights);
        result.setCr(cr);
        result.setConsistent(cr < 0.1);
        result.setLambdaMax(lambdaMax);
        result.setCi(ci);
        result.setRi(ri);
        result.setElementNames(Arrays.asList(elements));
        result.setWeightMap(weightMap);
        
        return result;
    }

    /**
     * 计算所有矩阵（维度层 + 各指标层）
     */
    public MatrixCalculationResult calculateAll(MatrixCalculationRequest request) {
        MatrixCalculationResult result = new MatrixCalculationResult();

        // 1. 计算维度层
        MatrixCalculationResult.MatrixResult dimResult = calculateSingleMatrix(
                DIMENSIONS,
                request.getDimensionMatrix()
        );
        result.setDimensionResult(dimResult);
        log.info("维度层AHP计算完成 - CR={}, 一致性={}",
                String.format("%.4f", dimResult.getCr()), dimResult.isConsistent());

        // 2. 计算各维度指标层
        Map<String, MatrixCalculationResult.MatrixResult> indicatorResults = new LinkedHashMap<>();
        Map<String, List<MatrixCalculationRequest.MatrixEntry>> reqIndicators = request.getIndicatorMatrices();

        for (Map.Entry<String, String[]> entry : DIMENSION_INDICATORS.entrySet()) {
            String dimName = entry.getKey();
            String[] indicators = entry.getValue();

            List<MatrixCalculationRequest.MatrixEntry> dimEntries =
                    (reqIndicators != null) ? reqIndicators.get(dimName) : null;

            MatrixCalculationResult.MatrixResult indResult = calculateSingleMatrix(indicators, dimEntries);
            indicatorResults.put(dimName, indResult);

            log.info("指标层[{}]AHP计算完成 - CR={}, 一致性={}",
                    dimName, String.format("%.4f", indResult.getCr()), indResult.isConsistent());
        }
        result.setIndicatorResults(indicatorResults);

        // 3. 计算综合权重（维度权重 × 指标权重）
        List<MatrixCalculationResult.CombinedWeight> combinedWeights = new ArrayList<>();
        Map<String, Double> dimWeightMap = dimResult.getWeightMap();

        for (Map.Entry<String, MatrixCalculationResult.MatrixResult> indEntry : indicatorResults.entrySet()) {
            String dimName = indEntry.getKey();
            MatrixCalculationResult.MatrixResult indResult = indEntry.getValue();
            double dimWeight = dimWeightMap.getOrDefault(dimName, 0.0);
            Map<String, Double> indWeightMap = indResult.getWeightMap();

            for (Map.Entry<String, Double> wEntry : indWeightMap.entrySet()) {
                String indName = wEntry.getKey();
                double indWeight = wEntry.getValue();
                double combined = dimWeight * indWeight;

                MatrixCalculationResult.CombinedWeight cw = new MatrixCalculationResult.CombinedWeight();
                cw.setDimension(dimName);
                cw.setIndicator(indName);
                cw.setDimensionWeight(dimWeight);
                cw.setIndicatorWeight(indWeight);
                cw.setCombinedWeight(combined);
                combinedWeights.add(cw);
            }
        }
        result.setCombinedWeights(combinedWeights);

        log.info("综合权重计算完成，共 {} 个二级指标", combinedWeights.size());
        return result;
    }

    /**
     * 获取维度名称列表
     */
    public static String[] getDimensions() {
        return DIMENSIONS;
    }

    /**
     * 获取指标层维度定义
     */
    public static Map<String, String[]> getDimensionIndicators() {
        return DIMENSION_INDICATORS;
    }
}
