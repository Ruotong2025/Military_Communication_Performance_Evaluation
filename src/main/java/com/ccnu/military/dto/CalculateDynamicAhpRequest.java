package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 计算动态AHP权重请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculateDynamicAhpRequest {

    /**
     * 专家ID
     */
    private Long expertId;

    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 是否计算综合权重
     */
    private Boolean calculateCombined;

    /**
     * 层级名称（可选，不传则计算所有层级）
     */
    private String levelName;

    /**
     * 自定义矩阵数据（可选，如果不传则使用数据库中已保存的数据）
     */
    private List<MatrixData> matrices;

    /**
     * 矩阵数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatrixData {
        /**
         * 层级名称
         */
        private String levelName;

        /**
         * 矩阵类型
         */
        private String matrixType;

        /**
         * 父级维度编码（二级矩阵时使用）
         */
        private String parentCode;

        /**
         * 维度列表
         */
        private List<String> dimensionCodes;

        /**
         * 矩阵值（二维数组）
         */
        private List<List<BigDecimal>> matrix;

        /**
         * 把握度矩阵
         */
        private List<List<BigDecimal>> confidenceMatrix;
    }
}
