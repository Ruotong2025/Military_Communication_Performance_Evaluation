package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 惩罚模型计算结果 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PenaltyModelResultDTO {

    /** 主键ID */
    private Long id;

    /** 评估批次ID */
    private String evaluationId;

    /** 作战任务ID */
    private String operationId;

    /** 集结综合得分（原始，Sstage） */
    private BigDecimal originalScore;

    /** 综合惩罚因子 P = min(F1..Fn) */
    private BigDecimal overallPenalty;

    /** 最终效能得分 Sfinal = Sstage × P */
    private BigDecimal finalScore;

    /** 惩罚幅度 (Sstage - Sfinal) / Sstage */
    private BigDecimal penaltyAmplitude;

    /** 各指标惩罚因子详情列表 */
    private List<PenaltyDetailItem> penaltyDetails;

    /** 各指标批次算术平均分 { indicator_key: avg_score } */
    private Map<String, BigDecimal> batchAvgScoreMap;

    /** 各指标批次最低分 { indicator_key: min_score } */
    private Map<String, BigDecimal> batchMinScoreMap;

    /** 各指标 Fi（均值口径）{ indicator_key: fi } */
    private Map<String, BigDecimal> batchAvgFiMap;

    /** 各指标 Fi（批次最低分口径）{ indicator_key: fi } */
    private Map<String, BigDecimal> batchMinFiMap;

    /** 创建时间 */
    private String createdAt;

    /** 更新时间 */
    private String updatedAt;

    /**
     * 单个指标的惩罚详情
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PenaltyDetailItem {
        /** 指标键 */
        private String indicatorKey;
        /** 该作战该指标得分（百分制） */
        private BigDecimal indicatorScore;
        /** 阈值 s */
        private BigDecimal threshold;
        /** 惩罚系数 m */
        private BigDecimal m;
        /** 惩罚因子 fi */
        private BigDecimal fi;
    }
}
