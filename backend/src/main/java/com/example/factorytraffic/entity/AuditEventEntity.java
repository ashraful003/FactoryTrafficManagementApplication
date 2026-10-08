package com.example.factorytraffic.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_events", indexes = {
        @Index(name = "idx_ae_junc_time", columnList = "junction_id, timestamp")
})
public class AuditEventEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "junction_id", length = 32, nullable = false)
    private String junctionId;

    @Column(name = "event_type", length = 64, nullable = false)
    private String eventType;

    @Column(name = "direction", length = 16)
    private String direction;

    @Column(name = "previous_state", length = 16)
    private String previousState;

    @Column(name = "new_state", length = 16)
    private String newState;

    @Column(name = "command_id", length = 64)
    private String commandId;

    @Column(name = "vehicle_id", length = 64)
    private String vehicleId;

    @Column(name = "details", length = 1024)
    private String details;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    public AuditEventEntity() {}

    public AuditEventEntity(String id, String junctionId, String eventType, String direction,
                            String previousState, String newState, String commandId,
                            String vehicleId, String details, Instant timestamp) {
        this.id = id;
        this.junctionId = junctionId;
        this.eventType = eventType;
        this.direction = direction;
        this.previousState = previousState;
        this.newState = newState;
        this.commandId = commandId;
        this.vehicleId = vehicleId;
        this.details = details;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getJunctionId() { return junctionId; }
    public void setJunctionId(String junctionId) { this.junctionId = junctionId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getPreviousState() { return previousState; }
    public void setPreviousState(String previousState) { this.previousState = previousState; }

    public String getNewState() { return newState; }
    public void setNewState(String newState) { this.newState = newState; }

    public String getCommandId() { return commandId; }
    public void setCommandId(String commandId) { this.commandId = commandId; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
