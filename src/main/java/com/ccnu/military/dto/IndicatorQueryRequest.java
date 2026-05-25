package com.ccnu.military.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 指标查询请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "指标查询请求")
public class IndicatorQueryRequest {

    @Schema(description = "指标名称", required = true)
    private String indicatorName;

    @Schema(description = "指标大类（可选）")
    private String category;

    @Schema(description = "领域上下文（可选）")
    private String domain;
}
