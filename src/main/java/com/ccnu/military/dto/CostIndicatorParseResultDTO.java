package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 成本指标解析结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostIndicatorParseResultDTO {

    /**
     * 是否解析成功
     */
    private Boolean success;

    /**
     * 解析消息
     */
    private String message;

    /**
     * 解析出的指标数量
     */
    private Integer indicatorCount;

    /**
     * 解析出的指标列表
     */
    private List<CostIndicatorDTO> indicators;

    /**
     * 原始数据（预览用）
     */
    private List<List<String>> rawData;
}
