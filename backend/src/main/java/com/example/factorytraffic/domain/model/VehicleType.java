package com.example.factorytraffic.domain.model;

public enum VehicleType {
    EMERGENCY(4),
    TRUCK(3),
    FORKLIFT(2),
    EMPLOYEE_VEHICLE(1);

    private final int priorityWeight;

    VehicleType(int priorityWeight) {
        this.priorityWeight = priorityWeight;
    }

    public int getPriorityWeight() {
        return priorityWeight;
    }
}
