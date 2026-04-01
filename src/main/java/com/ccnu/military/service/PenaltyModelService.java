package com.ccnu.military.service;

import com.ccnu.military.dto.PenaltyModelResultDTO;
import com.ccnu.military.entity.PenaltyModelResult;
import com.ccnu.military.repository.PenaltyModelResultRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 惩罚模型计算服务
 * <p>注意: 惩罚模型参数在前端代码中默认写死（阈值=70, 系数=0.8），
 * 后端仅负责保存前端传来的计算结果。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PenaltyModelService {

    private final PenaltyModelResultRepository penaltyResultRepository;
    private final ObjectMapper objectMapper;

    /**
     * 保存惩罚计算结果
     * <p>接收前端计算好的惩罚结果，直接保存到数据库。
     *
     * @param evaluationId 评估批次ID
     * @param results      前端计算好的惩罚结果列表
     * @return 保存的记录数
     */
    @Transactional
    public int savePenaltyResults(String evaluationId, List<PenaltyModelResultDTO> results) {
        log.info("保存惩罚计算结果，评估批次: {}, 记录数: {}", evaluationId, results.size());

        // 先删除该批次已有的惩罚结果（覆盖写入）
        penaltyResultRepository.deleteByEvaluationId(evaluationId);

        int savedCount = 0;
        for (PenaltyModelResultDTO dto : results) {
            PenaltyModelResult entity = toEntity(dto, evaluationId);
            penaltyResultRepository.save(entity);
            savedCount++;
        }

        log.info("惩罚计算结果保存完成，共 {} 条", savedCount);
        return savedCount;
    }

    /**
     * 查询指定批次的惩罚计算结果
     *
     * @param evaluationId 评估批次ID
     * @return 惩罚结果列表
     */
    public List<PenaltyModelResultDTO> getPenaltyResults(String evaluationId) {
        List<PenaltyModelResult> entities = penaltyResultRepository.findByEvaluationId(evaluationId);
        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 检查指定批次是否有惩罚计算结果
     *
     * @param evaluationId 评估批次ID
     * @return 是否有结果
     */
    public boolean hasResults(String evaluationId) {
        return penaltyResultRepository.existsByEvaluationId(evaluationId);
    }

    /**
     * 删除指定批次的惩罚计算结果
     *
     * @param evaluationId 评估批次ID
     */
    @Transactional
    public void deletePenaltyResults(String evaluationId) {
        penaltyResultRepository.deleteByEvaluationId(evaluationId);
        log.info("删除惩罚计算结果，评估批次: {}", evaluationId);
    }

    /**
     * 将 DTO 转换为实体
     */
    private PenaltyModelResult toEntity(PenaltyModelResultDTO dto, String evaluationId) {
        PenaltyModelResult entity = new PenaltyModelResult();
        entity.setEvaluationId(evaluationId);
        entity.setOperationId(dto.getOperationId());
        entity.setOriginalScore(dto.getOriginalScore());
        entity.setOverallPenalty(dto.getOverallPenalty());
        entity.setFinalScore(dto.getFinalScore());
        entity.setPenaltyAmplitude(dto.getPenaltyAmplitude());

        try {
            entity.setPenaltyDetails(toJson(dto.getPenaltyDetails()));
            entity.setBatchAvgScoreMap(toJson(dto.getBatchAvgScoreMap()));
            entity.setBatchMinScoreMap(toJson(dto.getBatchMinScoreMap()));
            entity.setBatchAvgFiMap(toJson(dto.getBatchAvgFiMap()));
            entity.setBatchMinFiMap(toJson(dto.getBatchMinFiMap()));
        } catch (JsonProcessingException e) {
            log.error("序列化 JSON 失败", e);
            throw new RuntimeException("序列化惩罚详情失败", e);
        }

        return entity;
    }

    /**
     * 将实体转换为 DTO
     */
    private PenaltyModelResultDTO toDTO(PenaltyModelResult entity) {
        PenaltyModelResultDTO dto = new PenaltyModelResultDTO();
        dto.setId(entity.getId());
        dto.setEvaluationId(entity.getEvaluationId());
        dto.setOperationId(entity.getOperationId());
        dto.setOriginalScore(entity.getOriginalScore());
        dto.setOverallPenalty(entity.getOverallPenalty());
        dto.setFinalScore(entity.getFinalScore());
        dto.setPenaltyAmplitude(entity.getPenaltyAmplitude());

        try {
            dto.setPenaltyDetails(fromJson(entity.getPenaltyDetails(),
                    new TypeReference<List<PenaltyModelResultDTO.PenaltyDetailItem>>() {}));
            dto.setBatchAvgScoreMap(fromJson(entity.getBatchAvgScoreMap(),
                    new TypeReference<Map<String, java.math.BigDecimal>>() {}));
            dto.setBatchMinScoreMap(fromJson(entity.getBatchMinScoreMap(),
                    new TypeReference<Map<String, java.math.BigDecimal>>() {}));
            dto.setBatchAvgFiMap(fromJson(entity.getBatchAvgFiMap(),
                    new TypeReference<Map<String, java.math.BigDecimal>>() {}));
            dto.setBatchMinFiMap(fromJson(entity.getBatchMinFiMap(),
                    new TypeReference<Map<String, java.math.BigDecimal>>() {}));
        } catch (JsonProcessingException e) {
            log.error("反序列化 JSON 失败", e);
        }

        if (entity.getCreatedAt() != null) {
            dto.setCreatedAt(entity.getCreatedAt().toString());
        }
        if (entity.getUpdatedAt() != null) {
            dto.setUpdatedAt(entity.getUpdatedAt().toString());
        }

        return dto;
    }

    private String toJson(Object obj) throws JsonProcessingException {
        if (obj == null) return null;
        return objectMapper.writeValueAsString(obj);
    }

    private <T> T fromJson(String json, TypeReference<T> typeRef) throws JsonProcessingException {
        if (json == null || json.isEmpty()) return null;
        return objectMapper.readValue(json, typeRef);
    }
}
