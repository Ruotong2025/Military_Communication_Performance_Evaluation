package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 成本评估计算请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostEffectivenessCalculateRequest {

    /** 评估批次ID */
    private String evaluationId;

    /** 作战任务ID列表 */
    private List<String> operationIds;

    /** 权重确定方法: equal, ahp, manual */
    private String weightMethod = "equal";

    /** 归一化方法: minmax, zscore, manual */
    private String normalizationMethod = "minmax";

    /** 手动设置的权重（key: indicatorKey, value: weight） */
    private Map<String, BigDecimal> manualWeights;

    /** 是否使用实际数据范围作为归一化边界 */
    private Boolean useActualRange = true;
}
