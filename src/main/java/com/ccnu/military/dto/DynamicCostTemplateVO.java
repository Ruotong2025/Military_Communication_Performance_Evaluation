package com.ccnu.military.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DynamicCostTemplateVO {
    
    private Long id;
    
    private String templateName;
    
    private String templateCode;
    
    private Integer totalCostCount;
    
    private LocalDateTime createdTime;
}
