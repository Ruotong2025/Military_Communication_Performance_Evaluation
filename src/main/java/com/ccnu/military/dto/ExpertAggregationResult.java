package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 专家集结 AHP 聚合结果，包含所有数据和去除极端值后的 CV 计算结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpertAggregationResult {

    /**
     * 所有数据计算CV
     */
    private CvDataResult allDataResult;

    /**
     * 去除离群点后的CV
     */
    private CvDataResult filteredExtremeResult;

    /**
     * 数据统计摘要
     */
    private DataSummary summary;

    /**
     * 单个CV数据结果
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CvDataResult {
        /** 一级维度CV分析 */
        private List<IndicatorCvItem> dimensionCvs;

        /** 二级指标CV分析 */
        private List<IndicatorCvItem> indicatorCvs;

        /** 参与计算的数据量 */
        private int dataCount;

        /** 数据筛选方式说明 */
        private String method;

        /** 专家ID列表（用于前端展示哪些专家参与计算） */
        private List<Long> expertIds;

        /** 参与计算的专家（ID + 名称，与 expertIds 顺序一致） */
        private List<ExpertParticipant> participatingExperts;

        /** 装备操作一级维度 CV */
        private List<IndicatorCvItem> equipmentDimensionCvs;

        /** 装备操作二级指标 CV（由 comparison_key 前缀 装备操作_ 的现场计算得到，与效能指标分列） */
        private List<IndicatorCvItem> equipmentIndicatorCvs;

        /** 参与装备操作 CV 的专家 */
        private List<ExpertParticipant> equipmentParticipatingExperts;

        /** 综合叶子全局权重 CV（效能+装备所有叶子指标按 globalWeight 直接合并） */
        private List<IndicatorCvItem> unifiedLeafCvs;
    }

    /**
     * 参与聚合的专家简要信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpertParticipant {
        private Long expertId;
        private String expertName;
    }

    /**
     * 单个指标的CV分析项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndicatorCvItem {
        /** 指标编码（如：security, reliability_collapse_rate） */
        private String indicatorCode;

        /** 指标名称（如：安全性、密钥泄露得分） */
        private String indicatorName;

        /** 所属维度 */
        private String dimension;

        /** 权重值列表（用于计算） */
        private List<Double> weightValues;

        /** 均值 */
        private double mean;

        /** 标准差 */
        private double stdDev;

        /** 变异系数(%) */
        private double cv;

        /** 一致性等级 */
        private String consistencyLevel;

        /** 等级标签类型（el-tag type） */
        private String levelType;

        /** 域标签：效能 / 装备（用于综合叶子展示） */
        private String domainTag;
    }

    /**
     * 数据统计摘要
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataSummary {
        /** 总专家数 */
        private int totalExperts;

        /** 所有数据有效数据量 */
        private int allDataCount;

        /** 去除极端值后有效数据量 */
        private int filteredExtremeCount;

        /** 所有数据 - 高度一致指标数 */
        private int allHighConsistency;

        /** 所有数据 - 严重分歧指标数 */
        private int allSevereDisagreement;

        /** 去除极端值 - 高度一致指标数 */
        private int extremeHighConsistency;

        /** 去除极端值 - 严重分歧指标数 */
        private int extremeSevereDisagreement;
    }
}
