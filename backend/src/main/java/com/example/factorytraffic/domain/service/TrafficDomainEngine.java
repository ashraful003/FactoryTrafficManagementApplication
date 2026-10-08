package com.example.factorytraffic.domain.service;

import com.example.factorytraffic.domain.algorithm.TrafficSchedulingAlgorithm;
import com.example.factorytraffic.domain.model.*;
import com.example.factorytraffic.domain.statemachine.TrafficStateMachine;

import java.time.Instant;
import java.util.Objects;

public class TrafficDomainEngine {
    private final TrafficStateMachine stateMachine;
    private final TrafficSchedulingAlgorithm schedulingAlgorithm;

    public TrafficDomainEngine() {
        this(new TrafficStateMachine(), new TrafficSchedulingAlgorithm());
    }

    public TrafficDomainEngine(TrafficStateMachine stateMachine, TrafficSchedulingAlgorithm schedulingAlgorithm) {
        this.stateMachine = Objects.requireNonNull(stateMachine, "stateMachine must not be null");
        this.schedulingAlgorithm = Objects.requireNonNull(schedulingAlgorithm, "schedulingAlgorithm must not be null");
    }

    /**
     * Processes a valid vehicle arrival into the traffic queue.
     */
    public boolean processVehicleArrival(Junction junction, String vehicleId, VehicleType vehicleType,
                                         Direction direction, long sequenceNo, Instant arrivalTimestamp) {
        Objects.requireNonNull(junction, "junction must not be null");
        Objects.requireNonNull(direction, "direction must not be null");
        Objects.requireNonNull(vehicleType, "vehicleType must not be null");

        Vehicle vehicle = new Vehicle(vehicleId, vehicleType, direction, sequenceNo, arrivalTimestamp);
        junction.getQueue(direction).enqueue(vehicle);

        // Emergency handling: Section 14
        if (vehicleType == VehicleType.EMERGENCY) {
            handleEmergencyArrival(junction, vehicle, arrivalTimestamp);
            return true;
        }

        // Automatic scheduling evaluation if in AUTOMATIC mode
        if (junction.getMode() == OperatingMode.AUTOMATIC && !junction.isTransitioning()) {
            TrafficPhase optimal = schedulingAlgorithm.selectOptimalPhase(junction, arrivalTimestamp);
            if (optimal != junction.getCurrentPhase()) {
                stateMachine.initiatePhaseTransition(junction, optimal, arrivalTimestamp);
            }
        }

        return false;
    }

    /**
     * Processes a vehicle clearance from the traffic queue.
     */
    public Vehicle processVehicleClearance(Junction junction, String vehicleId, Direction direction,
                                           long sequenceNo, Instant timestamp) {
        Objects.requireNonNull(junction, "junction must not be null");
        Objects.requireNonNull(direction, "direction must not be null");

        Vehicle clearedVehicle = junction.getQueue(direction).dequeue(vehicleId);

        // Check if cleared vehicle was an active emergency
        if (junction.hasEmergency()) {
            boolean matchesActive = vehicleId != null && vehicleId.equalsIgnoreCase(junction.getActiveEmergencyVehicleId());
            boolean wasEmergencyDirection = direction == junction.getEmergencyDirection();
            if (matchesActive || wasEmergencyDirection) {
                clearEmergencyState(junction, timestamp);
            }
        }

        // Check if queue transition should be evaluated
        if (junction.getMode() == OperatingMode.AUTOMATIC && !junction.isTransitioning()) {
            TrafficPhase optimal = schedulingAlgorithm.selectOptimalPhase(junction, timestamp);
            if (optimal != junction.getCurrentPhase()) {
                stateMachine.initiatePhaseTransition(junction, optimal, timestamp);
            }
        }

        return clearedVehicle;
    }

    private void handleEmergencyArrival(Junction junction, Vehicle emergencyVehicle, Instant now) {
        Direction direction = emergencyVehicle.getDirection();
        TrafficPhase targetPhase = direction.getAssociatedPhase();

        // Policy for competing emergency vehicles: First-come-first-served (FIFO)
        // If an emergency is already active on the same phase, do not disrupt.
        if (junction.hasEmergency() && junction.getEmergencyDirection() != null) {
            TrafficPhase currentEmergencyPhase = junction.getEmergencyDirection().getAssociatedPhase();
            if (currentEmergencyPhase == targetPhase) {
                // Same phase emergency: already prioritized
                return;
            }
            // Conflicting emergency request: the first active emergency retains green until cleared,
            // or if the first has been cleared / timed out, the next emergency takes over.
            return;
        }

        junction.setActiveEmergencyVehicleId(emergencyVehicle.getVehicleId());
        junction.setEmergencyDirection(direction);
        junction.setMode(OperatingMode.EMERGENCY);

        // Rule 4: Emergency priority CANNOT bypass safety transitions!
        stateMachine.initiatePhaseTransition(junction, targetPhase, now);
    }

    public void clearEmergencyState(Junction junction, Instant now) {
        junction.setActiveEmergencyVehicleId(null);
        junction.setEmergencyDirection(null);

        // Revert mode: if manual override was pending and not expired, return to MANUAL; else AUTOMATIC
        if (junction.getManualRequestedDirection() != null &&
                junction.getManualModeExpiryTime() != null &&
                now.isBefore(junction.getManualModeExpiryTime())) {
            junction.setMode(OperatingMode.MANUAL);
        } else {
            junction.setMode(OperatingMode.AUTOMATIC);
            junction.setManualRequestedDirection(null);
            junction.setManualModeExpiryTime(null);
        }
    }

    /**
     * Administrator requests manual GREEN for a specific direction.
     * Enforces Rule 3: Manual control cannot bypass safety transitions.
     */
    public boolean processManualGreenRequest(Junction junction, Direction direction, Instant now) {
        Objects.requireNonNull(junction, "junction must not be null");
        Objects.requireNonNull(direction, "direction must not be null");

        // Emergency overrides manual mode
        if (junction.hasEmergency()) {
            return false;
        }

        junction.setMode(OperatingMode.MANUAL);
        junction.setManualRequestedDirection(direction);
        junction.setManualModeExpiryTime(now.plusSeconds(120)); // 2 minutes default manual duration

        TrafficPhase targetPhase = direction.getAssociatedPhase();
        return stateMachine.initiatePhaseTransition(junction, targetPhase, now);
    }

    /**
     * Administrator requests returning to AUTOMATIC mode.
     */
    public void processReturnToAutomatic(Junction junction, Instant now) {
        Objects.requireNonNull(junction, "junction must not be null");

        if (junction.hasEmergency()) {
            return;
        }

        junction.setMode(OperatingMode.AUTOMATIC);
        junction.setManualRequestedDirection(null);
        junction.setManualModeExpiryTime(null);

        if (!junction.isTransitioning()) {
            TrafficPhase optimal = schedulingAlgorithm.selectOptimalPhase(junction, now);
            if (optimal != junction.getCurrentPhase()) {
                stateMachine.initiatePhaseTransition(junction, optimal, now);
            }
        }
    }

    public TrafficStateMachine getStateMachine() {
        return stateMachine;
    }

    public TrafficSchedulingAlgorithm getSchedulingAlgorithm() {
        return schedulingAlgorithm;
    }
}
