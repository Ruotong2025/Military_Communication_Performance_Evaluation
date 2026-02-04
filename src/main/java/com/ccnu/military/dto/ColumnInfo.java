package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表列信息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColumnInfo {
    private String columnName;      // 列名
    private String dataType;        // 数据类型
    private String columnComment;   // 列注释
    private Integer columnLength;   // 列长度
    private Boolean nullable;       // 是否可空
    private String columnKey;       // 键类型（PRI/UNI/MUL）
}
