package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 专家 AHP 个体层次计算结果 DTO：一份结果含效能+装备所有叶子指标的全局权重。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AhpIndividualResult {

    /** 域间一级 */
    private CrossDomainResult crossDomain;

    /** 效能体系（含维度层 + 各维度指标层） */
    private EffectivenessResult effectiveness;

    /** 装备体系（含维度层 + 各维度指标层） */
    private EquipmentResult equipment;

    /** 所有叶子指标全局权重列表（效能 + 装备），总和应为 1 */
    private List<LeafWeight> allLeaves;

    /** 叶子数量合计 */
    private int totalLeafCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrossDomainResult {
        /** Saaty 标度 a（效能相对装备） */
        private double score;
        private double confidence;
        /** w_eff = a / (1 + a) */
        private double effWeight;
        /** w_eq = 1 / (1 + a) */
        private double eqWeight;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EffectivenessResult {
        /** 各维度归一化权重（维度名 → 权重），数量不固定 */
        private Map<String, Double> dimensionWeights;

        /** 各维度下指标权重（维度名 → 指标名 → 权重），数量不固定 */
        private Map<String, Map<String, Double>> indicators;

        /** 维度层 CR */
        private double cr;

        public Double getCr() { return cr; }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EquipmentResult {
        /** 各维度归一化权重（维度名 → 权重） */
        private Map<String, Double> dimensionWeights;

        /** 各维度下指标权重 */
        private Map<String, Map<String, Double>> indicators;

        /** 各层 CR（key = "dimension" 或维度名） */
        private Map<String, Double> crByDimension;

        public Map<String, Double> getCrByDimension() { return crByDimension; }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeafWeight {
        /** "efficiency" | "equipment" */
        private String domain;
        private String dimension;
        private String indicator;
        /** 该叶子对总目标的全局权重 */
        private double globalWeight;
    }
}
