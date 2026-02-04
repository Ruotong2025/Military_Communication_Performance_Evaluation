package com.ccnu.military.repository;

import com.ccnu.military.entity.DuringBattleCommunication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 战中通信记录数据访问层
 */
@Repository
public interface DuringBattleCommunicationRepository extends JpaRepository<DuringBattleCommunication, Long> {

    /**
     * 根据测试批次ID查询所有通信记录
     */
    List<DuringBattleCommunication> findByTestId(String testId);

    /**
     * 根据场景ID查询所有通信记录
     */
    List<DuringBattleCommunication> findByScenarioId(Integer scenarioId);

    /**
     * 查询失败的通信记录
     */
    List<DuringBattleCommunication> findByCommunicationSuccessFalse();

    /**
     * 根据测试批次ID查询失败的通信记录
     */
    @Query("SELECT c FROM DuringBattleCommunication c WHERE c.testId = :testId AND c.communicationSuccess = false")
    List<DuringBattleCommunication> findFailedCommunicationsByTestId(@Param("testId") String testId);

    /**
     * 统计指定测试批次的通信总数
     */
    @Query("SELECT COUNT(c) FROM DuringBattleCommunication c WHERE c.testId = :testId")
    Long countByTestId(@Param("testId") String testId);

    /**
     * 统计指定测试批次的成功通信数
     */
    @Query("SELECT COUNT(c) FROM DuringBattleCommunication c WHERE c.testId = :testId AND c.communicationSuccess = true")
    Long countSuccessfulByTestId(@Param("testId") String testId);

    /**
     * 查询被侦察的通信记录
     */
    @Query("SELECT c FROM DuringBattleCommunication c WHERE c.detected = true")
    List<DuringBattleCommunication> findDetectedCommunications();

    /**
     * 查询被拦截的通信记录
     */
    @Query("SELECT c FROM DuringBattleCommunication c WHERE c.intercepted = true")
    List<DuringBattleCommunication> findInterceptedCommunications();
}
