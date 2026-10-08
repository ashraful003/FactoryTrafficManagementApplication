package com.example.factorytraffic.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ControllerEventRequestDto {

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("junction_id")
    private String junctionId;

    @JsonProperty("command_id")
    private String commandId;

    @NotBlank(message = "status is required (e.g. ACK, NACK, OFFLINE, ONLINE, DEGRADED)")
    @JsonProperty("status")
    private String status;

    @JsonProperty("actual_state")
    private String actualState;

    @JsonProperty("device_type")
    private String deviceType;

    @JsonProperty("direction")
    private String direction;

    @JsonProperty("timestamp")
    private Instant timestamp;

    public ControllerEventRequestDto() {}

    public ControllerEventRequestDto(String commandId, String status, String actualState, Instant timestamp) {
        this.commandId = commandId;
        this.status = status;
        this.actualState = actualState;
        this.timestamp = timestamp;
    }

    public ControllerEventRequestDto(String eventId, String junctionId, String deviceType,
                                     String direction, String status, Instant timestamp) {
        this.eventId = eventId;
        this.junctionId = junctionId;
        this.deviceType = deviceType;
        this.direction = direction;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getJunctionId() { return junctionId; }
    public void setJunctionId(String junctionId) { this.junctionId = junctionId; }

    public String getCommandId() { return commandId; }
    public void setCommandId(String commandId) { this.commandId = commandId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getActualState() { return actualState; }
    public void setActualState(String actualState) { this.actualState = actualState; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
