package com.ccnu.military.dto;

import lombok.Data;

import java.util.List;

/**
 * 指标计算请求
 */
@Data
public class MetricsCalculationRequest {

    /**
     * 作战ID列表（从 records_military_operation_info 中选择）
     */
    private List<Integer> operationIds;

    /**
     * 是否仅计算指定ID，false则计算全部
     */
    private Boolean specificOnly = false;
}
