package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 动态指标模板DTO（用于成本效益关联选择）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicTemplateDTO {

    private Long id;

    private String templateName;

    private String templateCode;

    private String description;

    /**
     * 层级数量
     */
    private Integer levelCount;

    /**
     * 指标数量
     */
    private Integer indicatorCount;

    private String status;

    private String createdTime;
}
