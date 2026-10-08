package com.example.factorytraffic.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public class Vehicle {
    private final String vehicleId;
    private final VehicleType vehicleType;
    private final Direction direction;
    private final long sequenceNo;
    private final Instant arrivedAt;

    public Vehicle(String vehicleId, VehicleType vehicleType, Direction direction, long sequenceNo, Instant arrivedAt) {
        this.vehicleId = Objects.requireNonNull(vehicleId, "vehicleId must not be null");
        this.vehicleType = Objects.requireNonNull(vehicleType, "vehicleType must not be null");
        this.direction = Objects.requireNonNull(direction, "direction must not be null");
        this.sequenceNo = sequenceNo;
        this.arrivedAt = arrivedAt != null ? arrivedAt : Instant.now();
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public Direction getDirection() {
        return direction;
    }

    public long getSequenceNo() {
        return sequenceNo;
    }

    public Instant getArrivedAt() {
        return arrivedAt;
    }

    public long getWaitingSeconds(Instant currentInstant) {
        if (currentInstant == null || arrivedAt == null) {
            return 0;
        }
        long seconds = Duration.between(arrivedAt, currentInstant).getSeconds();
        return Math.max(0, seconds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vehicle vehicle)) return false;
        return Objects.equals(vehicleId, vehicle.vehicleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vehicleId);
    }

    @Override
    public String toString() {
        return "Vehicle{" +
                "vehicleId='" + vehicleId + '\'' +
                ", vehicleType=" + vehicleType +
                ", direction=" + direction +
                ", sequenceNo=" + sequenceNo +
                ", arrivedAt=" + arrivedAt +
                '}';
    }
}
