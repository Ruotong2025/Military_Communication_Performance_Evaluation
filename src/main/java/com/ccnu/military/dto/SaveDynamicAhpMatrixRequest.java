package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 保存动态AHP矩阵打分请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveDynamicAhpMatrixRequest {

    /**
     * 专家ID
     */
    private Long expertId;

    /**
     * 专家名称
     */
    private String expertName;

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
     * 矩阵类型
     */
    private String matrixType;

    /**
     * 父级维度编码（二级矩阵时使用）
     */
    private String parentCode;

    /**
     * 上三角矩阵数据
     */
    private List<MatrixEntry> entries;

    /**
     * 矩阵条目
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatrixEntry {
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
    }
}
