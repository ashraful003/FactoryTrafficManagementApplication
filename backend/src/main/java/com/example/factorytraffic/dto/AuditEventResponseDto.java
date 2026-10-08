package com.example.factorytraffic.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class AuditEventResponseDto {

    @JsonProperty("id")
    private String id;

    @JsonProperty("junction_id")
    private String junctionId;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("direction")
    private String direction;

    @JsonProperty("previous_state")
    private String previousState;

    @JsonProperty("new_state")
    private String newState;

    @JsonProperty("command_id")
    private String commandId;

    @JsonProperty("vehicle_id")
    private String vehicleId;

    @JsonProperty("details")
    private String details;

    @JsonProperty("timestamp")
    private Instant timestamp;

    public AuditEventResponseDto() {}

    public AuditEventResponseDto(String id, String junctionId, String eventType, String direction,
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
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getJunctionId() { return junctionId; }
    public String getEventType() { return eventType; }
    public String getDirection() { return direction; }
    public String getPreviousState() { return previousState; }
    public String getNewState() { return newState; }
    public String getCommandId() { return commandId; }
    public String getVehicleId() { return vehicleId; }
    public String getDetails() { return details; }
    public Instant getTimestamp() { return timestamp; }
}
