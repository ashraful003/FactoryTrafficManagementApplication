package com.example.factorytraffic.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "processed_events", indexes = {
        @Index(name = "idx_pe_event_id", columnList = "event_id", unique = true)
})
public class ProcessedEventEntity {

    @Id
    @Column(name = "event_id", length = 64, nullable = false)
    private String eventId;

    @Column(name = "junction_id", length = 32, nullable = false)
    private String junctionId;

    @Column(name = "event_type", length = 32, nullable = false)
    private String eventType;

    @Column(name = "vehicle_id", length = 64)
    private String vehicleId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public ProcessedEventEntity() {}

    public ProcessedEventEntity(String eventId, String junctionId, String eventType, String vehicleId, Instant processedAt) {
        this.eventId = eventId;
        this.junctionId = junctionId;
        this.eventType = eventType;
        this.vehicleId = vehicleId;
        this.processedAt = processedAt != null ? processedAt : Instant.now();
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getJunctionId() { return junctionId; }
    public void setJunctionId(String junctionId) { this.junctionId = junctionId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}
