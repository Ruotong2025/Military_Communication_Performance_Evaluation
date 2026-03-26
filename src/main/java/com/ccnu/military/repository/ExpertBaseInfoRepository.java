package com.ccnu.military.repository;

import com.ccnu.military.entity.ExpertBaseInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 专家基础信息数据访问层
 */
@Repository
public interface ExpertBaseInfoRepository extends JpaRepository<ExpertBaseInfo, Long> {

    /**
     * 根据姓名查找专家
     */
    Optional<ExpertBaseInfo> findByExpertName(String expertName);

    /**
     * 根据姓名是否存在
     */
    boolean existsByExpertName(String expertName);

    /**
     * 查询所有启用的专家
     */
    List<ExpertBaseInfo> findByStatusOrderByExpertNameAsc(Integer status);

    /**
     * 查询所有启用的专家（带综合得分）
     */
    @Query("SELECT e FROM ExpertBaseInfo e WHERE e.status = 1 ORDER BY e.expertName ASC")
    List<ExpertBaseInfo> findAllActiveExperts();

    /**
     * 根据工作单位查询
     */
    List<ExpertBaseInfo> findByWorkUnitContaining(String workUnit);

    /**
     * 根据职称等级查询
     */
    List<ExpertBaseInfo> findByTitleLevel(Integer titleLevel);
}
