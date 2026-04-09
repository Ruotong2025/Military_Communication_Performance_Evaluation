package com.ccnu.military.service;

import com.ccnu.military.dto.ExpertAhpSimulateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 批量模拟 AHP：效能指标与装备操作共用 expert_ahp_comparison_score，一次请求同时写入两套数据。
 */
@Service
@RequiredArgsConstructor
public class AhpBatchSimulateOrchestrator {

    private final ExpertAhpComparisonScoreService expertAhpComparisonScoreService;
    private final EquipmentAhpScoreService equipmentAhpScoreService;

    /**
     * 同时生成并入库效能指标 AHP 与装备操作 AHP 比较打分（同表不同 comparison_key 前缀），同一事务。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> simulateEffectivenessAndEquipment(ExpertAhpSimulateRequest req) {
        Map<String, Object> eff = expertAhpComparisonScoreService.simulate(req);
        Map<String, Object> eq = equipmentAhpScoreService.simulate(req);
        Map<String, Object> out = new LinkedHashMap<>(eff);
        out.put("equipmentInsertedCount", eq.get("insertedCount"));
        return out;
    }
}
