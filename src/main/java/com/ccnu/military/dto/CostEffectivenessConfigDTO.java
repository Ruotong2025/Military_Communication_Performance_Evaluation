package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 成本效益配置DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostEffectivenessConfigDTO {

    private Long id;

    private String configName;

    private Long linkedTemplateId;

    private String linkedTemplateName;

    private String linkedBatchId;

    private Double effectivenessScore;

    private String description;

    private Integer status;

    private List<CostIndicatorDTO> costIndicators;

    private String createdBy;

    private String createdTime;

    private String updatedTime;
}
