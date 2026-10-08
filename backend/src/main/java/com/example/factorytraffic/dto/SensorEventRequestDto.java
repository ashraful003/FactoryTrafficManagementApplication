package com.example.factorytraffic.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public class SensorEventRequestDto {

    @NotBlank(message = "event_id is required")
    @JsonProperty("event_id")
    private String eventId;

    @NotBlank(message = "junction_id is required")
    @JsonProperty("junction_id")
    private String junctionId;

    @NotBlank(message = "direction is required (NORTH, SOUTH, EAST, WEST)")
    @JsonProperty("direction")
    private String direction;

    @NotBlank(message = "event_type is required (VEHICLE_ARRIVED, VEHICLE_CLEARED)")
    @JsonProperty("event_type")
    private String eventType;

    @NotBlank(message = "vehicle_id is required")
    @JsonProperty("vehicle_id")
    private String vehicleId;

    @JsonProperty("vehicle_type")
    private String vehicleType;

    @JsonProperty("sequence_no")
    private Long sequenceNo;

    @JsonProperty("timestamp")
    private Instant timestamp;

    public SensorEventRequestDto() {}

    public SensorEventRequestDto(String eventId, String junctionId, String direction,
                                 String eventType, String vehicleId, String vehicleType,
                                 Long sequenceNo, Instant timestamp) {
        this.eventId = eventId;
        this.junctionId = junctionId;
        this.direction = direction;
        this.eventType = eventType;
        this.vehicleId = vehicleId;
        this.vehicleType = vehicleType;
        this.sequenceNo = sequenceNo;
        this.timestamp = timestamp;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getJunctionId() { return junctionId; }
    public void setJunctionId(String junctionId) { this.junctionId = junctionId; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public Long getSequenceNo() { return sequenceNo != null ? sequenceNo : 0L; }
    public void setSequenceNo(Long sequenceNo) { this.sequenceNo = sequenceNo; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
