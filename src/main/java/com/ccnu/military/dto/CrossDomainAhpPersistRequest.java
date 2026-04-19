package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 效能指标体系 vs 装备操作体系 的一级域间 AHP 比较（单对上三角）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrossDomainAhpPersistRequest {

    private Long expertId;
    private String expertName;
    /** Saaty 标度：行「效能指标」相对列「装备操作」的重要性 */
    private Double score;
    private Double confidence;
}
