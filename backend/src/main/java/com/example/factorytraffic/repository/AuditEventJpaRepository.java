package com.example.factorytraffic.repository;

import com.example.factorytraffic.entity.AuditEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEventJpaRepository extends JpaRepository<AuditEventEntity, String> {
    List<AuditEventEntity> findByJunctionIdOrderByTimestampDesc(String junctionId, Pageable pageable);
    List<AuditEventEntity> findAllByOrderByTimestampDesc(Pageable pageable);
}
