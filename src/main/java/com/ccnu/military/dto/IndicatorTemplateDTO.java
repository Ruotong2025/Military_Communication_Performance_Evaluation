package com.ccnu.military.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class IndicatorTemplateDTO {
    private Long id;
    private String templateName;
    private String templateCode;
    private String description;
    private Integer levelCount;
    private Integer primaryCount;
    private Integer secondaryCount;
    private String status;
    private String sourceFile;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
