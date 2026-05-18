package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 成本指标解析请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostIndicatorParseRequestDTO {

    /**
     * 指标列表
     */
    private List<CostIndicatorDTO> indicators;
}
