package com.ccnu.military.dto;

import lombok.Data;
import java.util.List;

@Data
public class ImportResultDTO {
    private Long templateId;
    private String templateName;
    private String templateCode;
    private IndicatorTreeDTO treeData;
}
