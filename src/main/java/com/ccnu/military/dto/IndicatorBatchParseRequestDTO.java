package com.ccnu.military.dto;

import lombok.*;
import java.util.List;

/**
 * 指标批量解析请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorBatchParseRequestDTO {

    private List<String> indicators;

    private String category;

    private String domain;

    private Boolean forceReIdentify = false;
}
