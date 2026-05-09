package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 动态AHP矩阵数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicAhpMatrixDTO {

    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 层级名称
     */
    private String levelName;

    /**
     * 矩阵类型：LEVEL_BETWEEN（一级维度间）、PRIMARY_BETWEEN（二级维度间）
     */
    private String matrixType;

    /**
     * 父级维度编码（二级矩阵时使用）
     */
    private String parentCode;

    /**
     * 父级维度名称
     */
    private String parentName;

    /**
     * 参与比较的维度列表
     */
    private List<DimensionInfo> dimensions;

    /**
     * 矩阵数据（上三角+对角线）
     */
    private List<List<DynamicAhpCellDTO>> matrix;

    /**
     * 矩阵行数
     */
    private Integer rowCount;

    /**
     * 矩阵列数
     */
    private Integer colCount;

    /**
     * 维度信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionInfo {
        private String code;
        private String name;
        private String description;
        private Integer sortOrder;
        private String metricType;
        private String direction;
    }
}
