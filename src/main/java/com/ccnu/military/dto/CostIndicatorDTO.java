package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 成本指标DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostIndicatorDTO {

    private Long id;

    private Long configId;

    private String indicatorCode;

    private String indicatorName;

    /**
     * 指标类型：cost-成本型，benefit-效益型
     */
    private String indicatorType;

    /**
     * 权重
     */
    private Double weight;

    /**
     * 最小值
     */
    private Double minValue;

    /**
     * 最大值
     */
    private Double maxValue;

    /**
     * 单位
     */
    private String unit;

    /**
     * 排序顺序
     */
    private Integer sortOrder;
}
