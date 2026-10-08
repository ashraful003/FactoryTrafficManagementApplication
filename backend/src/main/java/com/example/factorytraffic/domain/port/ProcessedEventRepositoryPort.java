package com.example.factorytraffic.domain.port;

public interface ProcessedEventRepositoryPort {
    boolean existsByEventId(String eventId);
    void recordProcessedEvent(String eventId, String junctionId, String eventType, String vehicleId);
}
