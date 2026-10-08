package com.example.factorytraffic.domain.model;

import java.time.Instant;
import java.util.Objects;

public class AuditEvent {
    private final String id;
    private final String junctionId;
    private final String eventType;
    private final Direction direction;
    private final SignalColor previousState;
    private final SignalColor newState;
    private final String commandId;
    private final String vehicleId;
    private final String details;
    private final Instant timestamp;

    public AuditEvent(String id, String junctionId, String eventType, Direction direction,
                      SignalColor previousState, SignalColor newState, String commandId,
                      String vehicleId, String details, Instant timestamp) {
        this.id = id != null ? id : UUIDUtil.randomId();
        this.junctionId = Objects.requireNonNull(junctionId, "junctionId must not be null");
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.direction = direction;
        this.previousState = previousState;
        this.newState = newState;
        this.commandId = commandId;
        this.vehicleId = vehicleId;
        this.details = details;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public static AuditEvent create(String junctionId, String eventType, String details) {
        return new AuditEvent(null, junctionId, eventType, null, null, null, null, null, details, Instant.now());
    }

    public static AuditEvent signalChanged(String junctionId, Direction direction, SignalColor prev,
                                           SignalColor next, String commandId) {
        return new AuditEvent(null, junctionId, "SIGNAL_CHANGED", direction, prev, next, commandId, null,
                "Signal transitioned from " + prev + " to " + next, Instant.now());
    }

    public String getId() {
        return id;
    }

    public String getJunctionId() {
        return junctionId;
    }

    public String getEventType() {
        return eventType;
    }

    public Direction getDirection() {
        return direction;
    }

    public SignalColor getPreviousState() {
        return previousState;
    }

    public SignalColor getNewState() {
        return newState;
    }

    public String getCommandId() {
        return commandId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public String getDetails() {
        return details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
