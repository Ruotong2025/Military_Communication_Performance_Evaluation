package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 动态AHP矩阵单元格数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicAhpCellDTO {

    /**
     * 行索引
     */
    private Integer rowIndex;

    /**
     * 列索引
     */
    private Integer colIndex;

    /**
     * 行维度编码
     */
    private String rowCode;

    /**
     * 行维度名称
     */
    private String rowName;

    /**
     * 列维度编码
     */
    private String colCode;

    /**
     * 列维度名称
     */
    private String colName;

    /**
     * 标度值
     */
    private BigDecimal score;

    /**
     * 把握度
     */
    private BigDecimal confidence;

    /**
     * 是否可编辑（上三角可编辑）
     */
    private Boolean editable;

    /**
     * 是否为对角线（对角线恒为1）
     */
    private Boolean diagonal;
}
