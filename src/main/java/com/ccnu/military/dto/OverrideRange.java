package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字段范围覆盖：min ~ max
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OverrideRange {
    private Double min;
    private Double max;
}
