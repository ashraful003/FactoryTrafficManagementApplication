package com.example.factorytraffic.infrastructure.persistence;

import com.example.factorytraffic.domain.model.AuditEvent;
import com.example.factorytraffic.domain.model.Direction;
import com.example.factorytraffic.domain.model.SignalColor;
import com.example.factorytraffic.domain.port.AuditEventRepositoryPort;
import com.example.factorytraffic.entity.AuditEventEntity;
import com.example.factorytraffic.repository.AuditEventJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class AuditEventPersistenceAdapter implements AuditEventRepositoryPort {

    private final AuditEventJpaRepository auditEventJpaRepository;

    public AuditEventPersistenceAdapter(AuditEventJpaRepository auditEventJpaRepository) {
        this.auditEventJpaRepository = auditEventJpaRepository;
    }

    @Override
    @Transactional
    public void record(AuditEvent auditEvent) {
        if (auditEvent == null) {
            return;
        }
        AuditEventEntity entity = new AuditEventEntity(
                auditEvent.getId(),
                auditEvent.getJunctionId(),
                auditEvent.getEventType(),
                auditEvent.getDirection() != null ? auditEvent.getDirection().name() : null,
                auditEvent.getPreviousState() != null ? auditEvent.getPreviousState().name() : null,
                auditEvent.getNewState() != null ? auditEvent.getNewState().name() : null,
                auditEvent.getCommandId(),
                auditEvent.getVehicleId(),
                auditEvent.getDetails(),
                auditEvent.getTimestamp()
        );
        auditEventJpaRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEvent> findRecentByJunctionId(String junctionId, int limit) {
        PageRequest pageRequest = PageRequest.of(0, Math.max(1, limit));
        List<AuditEventEntity> list;
        if (junctionId != null && !junctionId.isBlank()) {
            list = auditEventJpaRepository.findByJunctionIdOrderByTimestampDesc(junctionId, pageRequest);
        } else {
            list = auditEventJpaRepository.findAllByOrderByTimestampDesc(pageRequest);
        }

        return list.stream().map(e -> new AuditEvent(
                e.getId(),
                e.getJunctionId(),
                e.getEventType(),
                e.getDirection() != null ? Direction.valueOf(e.getDirection()) : null,
                e.getPreviousState() != null ? SignalColor.valueOf(e.getPreviousState()) : null,
                e.getNewState() != null ? SignalColor.valueOf(e.getNewState()) : null,
                e.getCommandId(),
                e.getVehicleId(),
                e.getDetails(),
                e.getTimestamp()
        )).toList();
    }
}
