package com.example.factorytraffic.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class VehicleResponseDto {

    @JsonProperty("vehicle_id")
    private String vehicleId;

    @JsonProperty("vehicle_type")
    private String vehicleType;

    @JsonProperty("direction")
    private String direction;

    @JsonProperty("sequence_no")
    private long sequenceNo;

    @JsonProperty("arrived_at")
    private Instant arrivedAt;

    @JsonProperty("waiting_seconds")
    private long waitingSeconds;

    public VehicleResponseDto() {}

    public VehicleResponseDto(String vehicleId, String vehicleType, String direction,
                              long sequenceNo, Instant arrivedAt, long waitingSeconds) {
        this.vehicleId = vehicleId;
        this.vehicleType = vehicleType;
        this.direction = direction;
        this.sequenceNo = sequenceNo;
        this.arrivedAt = arrivedAt;
        this.waitingSeconds = waitingSeconds;
    }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public long getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(long sequenceNo) { this.sequenceNo = sequenceNo; }

    public Instant getArrivedAt() { return arrivedAt; }
    public void setArrivedAt(Instant arrivedAt) { this.arrivedAt = arrivedAt; }

    public long getWaitingSeconds() { return waitingSeconds; }
    public void setWaitingSeconds(long waitingSeconds) { this.waitingSeconds = waitingSeconds; }
}
