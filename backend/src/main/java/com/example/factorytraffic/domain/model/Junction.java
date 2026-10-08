package com.example.factorytraffic.domain.model;

import java.time.Instant;
import java.util.*;

public class Junction {
    private final String id;
    private String name;
    private OperatingMode mode;
    private TrafficPhase currentPhase;
    private TrafficPhase targetPhase;
    private TransitionState transitionState;
    private Instant phaseStartTime;
    private Instant transitionStepStartTime;
    private SignalStateMap desiredSignals;
    private SignalStateMap actualSignals;
    private final Map<Direction, TrafficQueue> queues = new EnumMap<>(Direction.class);
    private ControllerStatus controllerStatus;

    private String activeEmergencyVehicleId;
    private Direction emergencyDirection;
    private Direction manualRequestedDirection;
    private Instant manualModeExpiryTime;
    private TrafficCommand pendingCommand;
    private long version;

    public Junction(String id, String name) {
        this.id = Objects.requireNonNull(id, "junction id must not be null");
        this.name = name != null ? name : "Junction " + id;
        this.mode = OperatingMode.AUTOMATIC;
        this.currentPhase = TrafficPhase.NORTH_SOUTH;
        this.targetPhase = null;
        this.transitionState = TransitionState.STABLE;
        this.phaseStartTime = Instant.now();
        this.transitionStepStartTime = Instant.now();
        this.desiredSignals = SignalStateMap.forPhase(TrafficPhase.NORTH_SOUTH);
        this.actualSignals = SignalStateMap.forPhase(TrafficPhase.NORTH_SOUTH);
        this.controllerStatus = ControllerStatus.ONLINE;
        this.version = 1L;

        for (Direction d : Direction.values()) {
            queues.put(d, new TrafficQueue(d));
        }
    }

    public Junction(String id, String name, OperatingMode mode, TrafficPhase currentPhase,
                    TrafficPhase targetPhase, TransitionState transitionState,
                    Instant phaseStartTime, Instant transitionStepStartTime,
                    SignalStateMap desiredSignals, SignalStateMap actualSignals,
                    Map<Direction, TrafficQueue> queuesMap, ControllerStatus controllerStatus,
                    String activeEmergencyVehicleId, Direction emergencyDirection,
                    Direction manualRequestedDirection, Instant manualModeExpiryTime,
                    TrafficCommand pendingCommand, long version) {
        this.id = id;
        this.name = name;
        this.mode = mode;
        this.currentPhase = currentPhase;
        this.targetPhase = targetPhase;
        this.transitionState = transitionState;
        this.phaseStartTime = phaseStartTime;
        this.transitionStepStartTime = transitionStepStartTime;
        this.desiredSignals = desiredSignals != null ? desiredSignals : SignalStateMap.allRed();
        this.actualSignals = actualSignals != null ? actualSignals : SignalStateMap.allRed();
        this.controllerStatus = controllerStatus != null ? controllerStatus : ControllerStatus.UNKNOWN;
        this.activeEmergencyVehicleId = activeEmergencyVehicleId;
        this.emergencyDirection = emergencyDirection;
        this.manualRequestedDirection = manualRequestedDirection;
        this.manualModeExpiryTime = manualModeExpiryTime;
        this.pendingCommand = pendingCommand;
        this.version = version;

        for (Direction d : Direction.values()) {
            if (queuesMap != null && queuesMap.containsKey(d)) {
                this.queues.put(d, queuesMap.get(d));
            } else {
                this.queues.put(d, new TrafficQueue(d));
            }
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OperatingMode getMode() {
        return mode;
    }

    public void setMode(OperatingMode mode) {
        this.mode = mode;
    }

    public TrafficPhase getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(TrafficPhase currentPhase) {
        this.currentPhase = currentPhase;
    }

    public TrafficPhase getTargetPhase() {
        return targetPhase;
    }

    public void setTargetPhase(TrafficPhase targetPhase) {
        this.targetPhase = targetPhase;
    }

    public TransitionState getTransitionState() {
        return transitionState;
    }

    public void setTransitionState(TransitionState transitionState) {
        this.transitionState = transitionState;
    }

    public Instant getPhaseStartTime() {
        return phaseStartTime;
    }

    public void setPhaseStartTime(Instant phaseStartTime) {
        this.phaseStartTime = phaseStartTime;
    }

    public Instant getTransitionStepStartTime() {
        return transitionStepStartTime;
    }

    public void setTransitionStepStartTime(Instant transitionStepStartTime) {
        this.transitionStepStartTime = transitionStepStartTime;
    }

    public SignalStateMap getDesiredSignals() {
        return desiredSignals;
    }

    public void setDesiredSignals(SignalStateMap desiredSignals) {
        this.desiredSignals = desiredSignals;
    }

    public SignalStateMap getActualSignals() {
        return actualSignals;
    }

    public void setActualSignals(SignalStateMap actualSignals) {
        this.actualSignals = actualSignals;
    }

    public Map<Direction, TrafficQueue> getQueues() {
        return Collections.unmodifiableMap(queues);
    }

    public TrafficQueue getQueue(Direction direction) {
        return queues.get(direction);
    }

    public Map<Direction, Integer> getQueueSizes() {
        Map<Direction, Integer> map = new EnumMap<>(Direction.class);
        for (Direction d : Direction.values()) {
            map.put(d, queues.get(d).size());
        }
        return map;
    }

    public ControllerStatus getControllerStatus() {
        return controllerStatus;
    }

    public void setControllerStatus(ControllerStatus controllerStatus) {
        this.controllerStatus = controllerStatus;
    }

    public String getActiveEmergencyVehicleId() {
        return activeEmergencyVehicleId;
    }

    public void setActiveEmergencyVehicleId(String activeEmergencyVehicleId) {
        this.activeEmergencyVehicleId = activeEmergencyVehicleId;
    }

    public Direction getEmergencyDirection() {
        return emergencyDirection;
    }

    public void setEmergencyDirection(Direction emergencyDirection) {
        this.emergencyDirection = emergencyDirection;
    }

    public Direction getManualRequestedDirection() {
        return manualRequestedDirection;
    }

    public void setManualRequestedDirection(Direction manualRequestedDirection) {
        this.manualRequestedDirection = manualRequestedDirection;
    }

    public Instant getManualModeExpiryTime() {
        return manualModeExpiryTime;
    }

    public void setManualModeExpiryTime(Instant manualModeExpiryTime) {
        this.manualModeExpiryTime = manualModeExpiryTime;
    }

    public TrafficCommand getPendingCommand() {
        return pendingCommand;
    }

    public void setPendingCommand(TrafficCommand pendingCommand) {
        this.pendingCommand = pendingCommand;
    }

    public long getVersion() {
        return version;
    }

    public void incrementVersion() {
        this.version++;
    }

    public boolean isTransitioning() {
        return transitionState != TransitionState.STABLE;
    }

    public boolean hasEmergency() {
        return activeEmergencyVehicleId != null || emergencyDirection != null;
    }

    /**
     * Safety Rule 1: Conflicting phases must never simultaneously receive GREEN.
     */
    public boolean isSafe() {
        return !desiredSignals.hasConflictingGreens() && !actualSignals.hasConflictingGreens();
    }
}
