package com.example.factorytraffic.domain.port;

import com.example.factorytraffic.domain.model.AuditEvent;

import java.util.List;

public interface AuditEventRepositoryPort {
    void record(AuditEvent auditEvent);
    List<AuditEvent> findRecentByJunctionId(String junctionId, int limit);
}
