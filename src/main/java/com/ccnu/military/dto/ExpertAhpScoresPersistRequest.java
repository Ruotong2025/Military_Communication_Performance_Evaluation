package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 将当前矩阵保存到 expert_ahp_comparison_score
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpertAhpScoresPersistRequest {

    private Long expertId;
    private String expertName;
    private List<MatrixCalculationRequest.MatrixEntry> dimensionMatrix;
    private Map<String, List<MatrixCalculationRequest.MatrixEntry>> indicatorMatrices;
}
