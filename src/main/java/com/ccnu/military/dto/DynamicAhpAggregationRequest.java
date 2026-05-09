package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 动态AHP集结请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicAhpAggregationRequest {

    /**
     * 指标模板ID（必填）
     */
    private Long templateId;

    /**
     * 专家ID列表（可选，为空则使用全部专家）
     */
    private List<Long> expertIds;

    /**
     * 是否使用全部专家
     */
    private Boolean useAllExperts;
}
