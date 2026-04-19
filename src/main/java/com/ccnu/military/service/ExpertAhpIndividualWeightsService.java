package com.ccnu.military.service;

import com.ccnu.military.dto.AhpIndividualResult;
import com.ccnu.military.entity.ExpertAhpIndividualWeights;
import com.ccnu.military.repository.ExpertAhpIndividualWeightsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 持久化专家 AHP 统一层次权重快照（效能+装备统一展开）
 * <p>
 * 所有数据通过 JSON 列存储，实体类通过 Map/List 动态读写，
 * 避免维度数量变化时需要修改表结构。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpertAhpIndividualWeightsService {

    private final ExpertAhpIndividualWeightsRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * 保存或更新专家 AHP 统一权重快照
     *
     * @param expertId   专家ID
     * @param expertName 专家姓名
     * @param result     统一AHP计算结果
     */
    @Transactional
    public void upsert(Long expertId, String expertName, AhpIndividualResult result) {
        if (expertId == null) {
            throw new IllegalArgumentException("expertId 不能为空");
        }

        ExpertAhpIndividualWeights row = repository.findByExpertId(expertId)
                .orElseGet(ExpertAhpIndividualWeights::new);

        row.setExpertId(expertId);
        row.setExpertName(expertName);
        row.setUpdatedAt(LocalDateTime.now());

        if (result == null) {
            repository.save(row);
            return;
        }

        // 域间权重
        if (result.getCrossDomain() != null) {
            row.setCrossDomainScore(BigDecimal.valueOf(result.getCrossDomain().getScore()));
            row.setCrossDomainConfidence(BigDecimal.valueOf(result.getCrossDomain().getConfidence()));
            row.setEffDomainWeight(BigDecimal.valueOf(result.getCrossDomain().getEffWeight()));
            row.setEqDomainWeight(BigDecimal.valueOf(result.getCrossDomain().getEqWeight()));
        }

        // 效能叶子权重（JSON）
        if (result.getEffectiveness() != null && result.getEffectiveness().getIndicators() != null) {
            row.setEffLeafWeightsJson(toJson(result.getEffectiveness().getIndicators()));
            // 计算叶子数量
            int leafCount = result.getEffectiveness().getIndicators().values().stream()
                    .mapToInt(m -> m != null ? m.size() : 0).sum();
            row.setEffLeafCount(leafCount);
        }

        // 效能维度权重（JSON）
        if (result.getEffectiveness() != null && result.getEffectiveness().getDimensionWeights() != null) {
            row.setEffDimWeightsJson(toJson(result.getEffectiveness().getDimensionWeights()));
            row.setEffDimCount(result.getEffectiveness().getDimensionWeights().size());
        }

        // 效能 CR
        if (result.getEffectiveness() != null && result.getEffectiveness().getCr() != null) {
            row.setEffCr(BigDecimal.valueOf(result.getEffectiveness().getCr()));
        }

        // 装备叶子权重（JSON）
        if (result.getEquipment() != null && result.getEquipment().getIndicators() != null) {
            row.setEqLeafWeightsJson(toJson(result.getEquipment().getIndicators()));
            // 计算叶子数量
            int leafCount = result.getEquipment().getIndicators().values().stream()
                    .mapToInt(m -> m != null ? m.size() : 0).sum();
            row.setEqLeafCount(leafCount);
        }

        // 装备维度权重（JSON）
        if (result.getEquipment() != null && result.getEquipment().getDimensionWeights() != null) {
            row.setEqDimWeightsJson(toJson(result.getEquipment().getDimensionWeights()));
            row.setEqDimCount(result.getEquipment().getDimensionWeights().size());
        }

        // 装备 CR（JSON）
        if (result.getEquipment() != null && result.getEquipment().getCrByDimension() != null) {
            row.setEqCrJson(toJson(result.getEquipment().getCrByDimension()));
        }

        // 完整结果 JSON
        row.setAhpResultJson(toJson(result));

        repository.save(row);
        log.debug("已写入专家 AHP 统一权重快照 expertId={}", expertId);
    }

    /**
     * 查询某专家的权重快照
     */
    public AhpIndividualResult findResult(Long expertId) {
        if (expertId == null) {
            return null;
        }
        return repository.findByExpertId(expertId)
                .map(this::parseJson)
                .orElse(null);
    }

    private AhpIndividualResult parseJson(ExpertAhpIndividualWeights row) {
        if (row.getAhpResultJson() == null || row.getAhpResultJson().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(row.getAhpResultJson(), AhpIndividualResult.class);
        } catch (Exception e) {
            log.warn("反序列化 AHP 统一权重快照失败 expertId={}", row.getExpertId(), e);
            return null;
        }
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("序列化 JSON 失败", e);
            throw new RuntimeException("序列化失败", e);
        }
    }
}
