package com.ccnu.military.repository;

import com.ccnu.military.entity.RecordsCommAttackOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecordsCommAttackOperationRepository extends JpaRepository<RecordsCommAttackOperation, Long> {

    List<RecordsCommAttackOperation> findByOperationIdOrderByStartTimeMs(Integer operationId);

    List<RecordsCommAttackOperation> findByOperationIdAndOperationType(Integer operationId, RecordsCommAttackOperation.OperationType operationType);
}
