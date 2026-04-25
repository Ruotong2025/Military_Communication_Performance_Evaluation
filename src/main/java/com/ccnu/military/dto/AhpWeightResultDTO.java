package com.ccnu.military.dto;

import lombok.Data;
import java.util.Map;

@Data
public class AhpWeightResultDTO {
    private Long levelId;
    private String levelName;
    private Map<String, Double> weights;
    private Double consistencyRatio;
    private Boolean consistencyPassed;

    public AhpWeightResultDTO() {
    }

    public AhpWeightResultDTO(Long levelId, Map<String, Double> weights, Double cr) {
        this.levelId = levelId;
        this.weights = weights;
        this.consistencyRatio = cr;
        this.consistencyPassed = cr < 0.1;
    }
}
