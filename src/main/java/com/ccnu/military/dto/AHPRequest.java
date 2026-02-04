package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * AHP计算请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AHPRequest {
    // 8个维度的优先级 {RL:1, SC:2, AJ:3, EF:4, PO:5, NC:6, HO:7, RS:8}
    private Map<String, Integer> priorities;
}
