package com.example.factorytraffic.infrastructure.persistence;

import com.example.factorytraffic.domain.port.ProcessedEventRepositoryPort;
import com.example.factorytraffic.entity.ProcessedEventEntity;
import com.example.factorytraffic.repository.ProcessedEventJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class ProcessedEventPersistenceAdapter implements ProcessedEventRepositoryPort {

    private final ProcessedEventJpaRepository processedEventJpaRepository;

    public ProcessedEventPersistenceAdapter(ProcessedEventJpaRepository processedEventJpaRepository) {
        this.processedEventJpaRepository = processedEventJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEventId(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }
        return processedEventJpaRepository.existsById(eventId);
    }

    @Override
    @Transactional
    public void recordProcessedEvent(String eventId, String junctionId, String eventType, String vehicleId) {
        if (eventId == null || eventId.isBlank()) {
            return;
        }
        ProcessedEventEntity entity = new ProcessedEventEntity(
                eventId,
                junctionId,
                eventType,
                vehicleId,
                Instant.now()
        );
        processedEventJpaRepository.save(entity);
    }
}
