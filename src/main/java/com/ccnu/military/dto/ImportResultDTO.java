package com.ccnu.military.dto;

import lombok.Data;

@Data
public class ImportResultDTO {
    private Long templateId;
    private String templateName;
    private String templateCode;
    private DynamicIndicatorTreeDTO treeData;
}
