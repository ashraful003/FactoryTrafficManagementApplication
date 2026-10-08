package com.example.factorytraffic.domain.model;

public enum OperatingMode {
    AUTOMATIC,
    MANUAL,
    EMERGENCY,
    DEGRADED,
    FAILURE;

    public boolean canTransitionTo(OperatingMode target) {
        if (this == target) {
            return true;
        }
        // Failure or Degraded can be entered from any state when device faults occur
        if (target == DEGRADED || target == FAILURE) {
            return true;
        }
        return switch (this) {
            case AUTOMATIC -> target == MANUAL || target == EMERGENCY;
            case MANUAL -> target == AUTOMATIC || target == EMERGENCY;
            case EMERGENCY -> target == AUTOMATIC || target == MANUAL;
            case DEGRADED -> target == AUTOMATIC || target == MANUAL || target == FAILURE;
            case FAILURE -> target == DEGRADED || target == AUTOMATIC;
        };
    }
}
