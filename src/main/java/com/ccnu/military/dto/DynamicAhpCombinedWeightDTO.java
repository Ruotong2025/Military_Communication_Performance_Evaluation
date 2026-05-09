package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 动态AHP综合权重DTO
 * 对应 combined_weights_json 字段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicAhpCombinedWeightDTO {

    /**
     * 层级名称，如"战术级"
     */
    private String levelName;

    /**
     * 一级维度名称，如"通信质量"
     */
    private String primaryName;

    /**
     * 二级指标名称，如"时延"
     */
    private String secondaryName;

    /**
     * 综合权重（二级指标在整体中的权重，相加为1）
     */
    private Double combinedWeight;

    /**
     * 一级维度在层级内的权重（如通信质量在战术级内占35%）
     */
    private Double primaryWeight;

    /**
     * 层级权重（如战术级占60%）
     */
    private Double levelWeight;
}
