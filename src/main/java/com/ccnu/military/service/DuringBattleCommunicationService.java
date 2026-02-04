package com.ccnu.military.service;

import com.ccnu.military.entity.DuringBattleCommunication;
import com.ccnu.military.repository.DuringBattleCommunicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 战中通信记录服务层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DuringBattleCommunicationService {

    private final DuringBattleCommunicationRepository repository;

    /**
     * 查询所有通信记录
     */
    @Transactional(readOnly = true)
    public List<DuringBattleCommunication> findAll() {
        log.info("查询所有通信记录");
        return repository.findAll();
    }

    /**
     * 根据测试批次ID查询
     */
    @Transactional(readOnly = true)
    public List<DuringBattleCommunication> findByTestId(String testId) {
        log.info("查询测试批次的通信记录: {}", testId);
        return repository.findByTestId(testId);
    }

    /**
     * 根据场景ID查询
     */
    @Transactional(readOnly = true)
    public List<DuringBattleCommunication> findByScenarioId(Integer scenarioId) {
        log.info("查询场景ID的通信记录: {}", scenarioId);
        return repository.findByScenarioId(scenarioId);
    }

    /**
     * 查询失败的通信记录
     */
    @Transactional(readOnly = true)
    public List<DuringBattleCommunication> findFailedCommunications() {
        log.info("查询失败的通信记录");
        return repository.findByCommunicationSuccessFalse();
    }

    /**
     * 根据测试批次ID查询失败的通信记录
     */
    @Transactional(readOnly = true)
    public List<DuringBattleCommunication> findFailedCommunicationsByTestId(String testId) {
        log.info("查询测试批次{}的失败通信记录", testId);
        return repository.findFailedCommunicationsByTestId(testId);
    }

    /**
     * 统计指定测试批次的通信总数
     */
    @Transactional(readOnly = true)
    public Long countByTestId(String testId) {
        log.info("统计测试批次{}的通信总数", testId);
        return repository.countByTestId(testId);
    }

    /**
     * 统计指定测试批次的成功通信数
     */
    @Transactional(readOnly = true)
    public Long countSuccessfulByTestId(String testId) {
        log.info("统计测试批次{}的成功通信数", testId);
        return repository.countSuccessfulByTestId(testId);
    }

    /**
     * 计算指定测试批次的成功率
     */
    @Transactional(readOnly = true)
    public Double calculateSuccessRate(String testId) {
        Long total = countByTestId(testId);
        if (total == 0) {
            return 0.0;
        }
        Long successful = countSuccessfulByTestId(testId);
        return (successful.doubleValue() / total.doubleValue()) * 100;
    }

    /**
     * 查询被侦察的通信记录
     */
    @Transactional(readOnly = true)
    public List<DuringBattleCommunication> findDetectedCommunications() {
        log.info("查询被侦察的通信记录");
        return repository.findDetectedCommunications();
    }

    /**
     * 查询被拦截的通信记录
     */
    @Transactional(readOnly = true)
    public List<DuringBattleCommunication> findInterceptedCommunications() {
        log.info("查询被拦截的通信记录");
        return repository.findInterceptedCommunications();
    }
}
