package com.ccnu.military.repository;

import com.ccnu.military.entity.RecordsCommDefenseOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecordsCommDefenseOperationRepository extends JpaRepository<RecordsCommDefenseOperation, Long> {

    List<RecordsCommDefenseOperation> findByOperationIdOrderByStartTimeMs(Integer operationId);

    List<RecordsCommDefenseOperation> findByOperationIdAndOperationType(Integer operationId, RecordsCommDefenseOperation.OperationType operationType);
}
