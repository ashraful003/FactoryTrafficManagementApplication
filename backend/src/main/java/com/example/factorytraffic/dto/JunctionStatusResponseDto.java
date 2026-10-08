package com.example.factorytraffic.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class JunctionStatusResponseDto {

    @JsonProperty("junction_id")
    private String junctionId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("mode")
    private String mode;

    @JsonProperty("phase")
    private String phase;

    @JsonProperty("transition_state")
    private String transitionState;

    @JsonProperty("controller_status")
    private String controllerStatus;

    @JsonProperty("desired_signals")
    private Map<String, String> desiredSignals;

    @JsonProperty("actual_signals")
    private Map<String, String> actualSignals;

    @JsonProperty("queues")
    private Map<String, Integer> queues;

    @JsonProperty("queued_vehicles")
    private Map<String, List<VehicleResponseDto>> queuedVehicles;

    @JsonProperty("active_emergency")
    private boolean activeEmergency;

    @JsonProperty("emergency_direction")
    private String emergencyDirection;

    @JsonProperty("version")
    private Long version;

    public JunctionStatusResponseDto() {}

    public String getJunctionId() { return junctionId; }
    public void setJunctionId(String junctionId) { this.junctionId = junctionId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public String getTransitionState() { return transitionState; }
    public void setTransitionState(String transitionState) { this.transitionState = transitionState; }

    public String getControllerStatus() { return controllerStatus; }
    public void setControllerStatus(String controllerStatus) { this.controllerStatus = controllerStatus; }

    public Map<String, String> getDesiredSignals() { return desiredSignals; }
    public void setDesiredSignals(Map<String, String> desiredSignals) { this.desiredSignals = desiredSignals; }

    public Map<String, String> getActualSignals() { return actualSignals; }
    public void setActualSignals(Map<String, String> actualSignals) { this.actualSignals = actualSignals; }

    public Map<String, Integer> getQueues() { return queues; }
    public void setQueues(Map<String, Integer> queues) { this.queues = queues; }

    public Map<String, List<VehicleResponseDto>> getQueuedVehicles() { return queuedVehicles; }
    public void setQueuedVehicles(Map<String, List<VehicleResponseDto>> queuedVehicles) { this.queuedVehicles = queuedVehicles; }

    public boolean isActiveEmergency() { return activeEmergency; }
    public void setActiveEmergency(boolean activeEmergency) { this.activeEmergency = activeEmergency; }

    public String getEmergencyDirection() { return emergencyDirection; }
    public void setEmergencyDirection(String emergencyDirection) { this.emergencyDirection = emergencyDirection; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
