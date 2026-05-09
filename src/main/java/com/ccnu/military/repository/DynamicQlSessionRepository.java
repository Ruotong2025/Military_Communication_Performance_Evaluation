package com.ccnu.military.repository;

import com.ccnu.military.entity.DynamicQlSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 动态定性评估会话Repository
 */
@Repository
public interface DynamicQlSessionRepository extends JpaRepository<DynamicQlSession, Long> {

    /**
     * 根据会话ID查询
     */
    Optional<DynamicQlSession> findBySessionId(String sessionId);

    /**
     * 根据批次ID查询会话
     */
    List<DynamicQlSession> findByBatchId(String batchId);

    /**
     * 根据模板ID查询会话
     */
    List<DynamicQlSession> findByTemplateId(Long templateId);

    /**
     * 根据状态查询会话
     */
    List<DynamicQlSession> findByStatus(DynamicQlSession.SessionStatus status);

    /**
     * 根据批次ID和状态查询
     */
    List<DynamicQlSession> findByBatchIdAndStatus(String batchId, DynamicQlSession.SessionStatus status);

    /**
     * 统计某模板的会话数量
     */
    @Query("SELECT COUNT(s) FROM DynamicQlSession s WHERE s.templateId = :templateId")
    long countByTemplateId(@Param("templateId") Long templateId);

    /**
     * 检查会话是否存在
     */
    boolean existsBySessionId(String sessionId);

    /**
     * 删除会话
     */
    void deleteBySessionId(String sessionId);
}
