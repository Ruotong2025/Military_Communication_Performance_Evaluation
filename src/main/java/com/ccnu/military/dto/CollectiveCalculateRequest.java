package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 专家集结计算请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectiveCalculateRequest {

    /** 评估批次ID */
    private String evaluationId;

    /** 指定专家ID列表（useAllExperts=false时使用） */
    private List<Long> expertIds;

    /** 是否使用全部专家（true时忽略expertIds） */
    private Boolean useAllExperts;
}
