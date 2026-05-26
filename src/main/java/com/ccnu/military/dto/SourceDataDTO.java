package com.ccnu.military.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * 指标数据源DTO - 统一的数据结构
 * 数据库查询和API分析结果复用此类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "指标数据源DTO")
public class SourceDataDTO {
    
    @Schema(description = "数据源名称")
    private String sourceDataName;
    
    @Schema(description = "测量方法")
    private String measurementMethod;
    
    @Schema(description = "数据类型")
    private String dataType;
    
    @Schema(description = "单位")
    private String unit;
    
    @Schema(description = "优先级")
    private Integer priority;
    
    @Schema(description = "置信度")
    private Double confidence;
    
    @Schema(description = "是否与公式相关")
    private Boolean isFormulaRelated;
    
    @Schema(description = "公式符号")
    private String formulaSymbol;
    
    @Schema(description = "是否必需")
    private Boolean isEssential;
}
