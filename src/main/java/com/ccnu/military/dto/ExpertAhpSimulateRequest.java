package com.ccnu.military.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpertAhpSimulateRequest {

    private List<Long> expertIds;
}
