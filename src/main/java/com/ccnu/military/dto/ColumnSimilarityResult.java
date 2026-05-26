package com.ccnu.military.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字段相似度搜索结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "字段相似度搜索结果")
public class ColumnSimilarityResult {

    @Schema(description = "字段名称")
    private String columnName;

    @Schema(description = "字段中文标签/注释")
    private String columnLabel;

    @Schema(description = "所属表名")
    private String tableName;

    @Schema(description = "所属表中文名")
    private String tableLabel;

    @Schema(description = "相似度 (0-1)")
    private Double similarity;

    @Schema(description = "数据类型")
    private String dataType;

    @Schema(description = "单位（如果有）")
    private String unit;

    @Schema(description = "字段描述")
    private String description;
}
