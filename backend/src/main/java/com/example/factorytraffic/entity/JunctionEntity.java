package com.example.factorytraffic.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "junctions")
public class JunctionEntity {

    @Id
    @Column(name = "id", length = 32, nullable = false)
    private String id;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "mode", length = 32, nullable = false)
    private String mode;

    @Column(name = "current_phase", length = 32, nullable = false)
    private String currentPhase;

    @Column(name = "target_phase", length = 32)
    private String targetPhase;

    @Column(name = "transition_state", length = 32, nullable = false)
    private String transitionState;

    @Column(name = "phase_start_time", nullable = false)
    private Instant phaseStartTime;

    @Column(name = "transition_step_start_time")
    private Instant transitionStepStartTime;

    @Column(name = "desired_north", length = 16, nullable = false)
    private String desiredNorth;

    @Column(name = "desired_south", length = 16, nullable = false)
    private String desiredSouth;

    @Column(name = "desired_east", length = 16, nullable = false)
    private String desiredEast;

    @Column(name = "desired_west", length = 16, nullable = false)
    private String desiredWest;

    @Column(name = "actual_north", length = 16, nullable = false)
    private String actualNorth;

    @Column(name = "actual_south", length = 16, nullable = false)
    private String actualSouth;

    @Column(name = "actual_east", length = 16, nullable = false)
    private String actualEast;

    @Column(name = "actual_west", length = 16, nullable = false)
    private String actualWest;

    @Column(name = "controller_status", length = 32, nullable = false)
    private String controllerStatus;

    @Column(name = "active_emergency_vehicle_id", length = 64)
    private String activeEmergencyVehicleId;

    @Column(name = "emergency_direction", length = 16)
    private String emergencyDirection;

    @Column(name = "manual_requested_direction", length = 16)
    private String manualRequestedDirection;

    @Column(name = "manual_mode_expiry_time")
    private Instant manualModeExpiryTime;

    @Version
    @Column(name = "version")
    private Long version;

    public JunctionEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(String currentPhase) { this.currentPhase = currentPhase; }

    public String getTargetPhase() { return targetPhase; }
    public void setTargetPhase(String targetPhase) { this.targetPhase = targetPhase; }

    public String getTransitionState() { return transitionState; }
    public void setTransitionState(String transitionState) { this.transitionState = transitionState; }

    public Instant getPhaseStartTime() { return phaseStartTime; }
    public void setPhaseStartTime(Instant phaseStartTime) { this.phaseStartTime = phaseStartTime; }

    public Instant getTransitionStepStartTime() { return transitionStepStartTime; }
    public void setTransitionStepStartTime(Instant transitionStepStartTime) { this.transitionStepStartTime = transitionStepStartTime; }

    public String getDesiredNorth() { return desiredNorth; }
    public void setDesiredNorth(String desiredNorth) { this.desiredNorth = desiredNorth; }

    public String getDesiredSouth() { return desiredSouth; }
    public void setDesiredSouth(String desiredSouth) { this.desiredSouth = desiredSouth; }

    public String getDesiredEast() { return desiredEast; }
    public void setDesiredEast(String desiredEast) { this.desiredEast = desiredEast; }

    public String getDesiredWest() { return desiredWest; }
    public void setDesiredWest(String desiredWest) { this.desiredWest = desiredWest; }

    public String getActualNorth() { return actualNorth; }
    public void setActualNorth(String actualNorth) { this.actualNorth = actualNorth; }

    public String getActualSouth() { return actualSouth; }
    public void setActualSouth(String actualSouth) { this.actualSouth = actualSouth; }

    public String getActualEast() { return actualEast; }
    public void setActualEast(String actualEast) { this.actualEast = actualEast; }

    public String getActualWest() { return actualWest; }
    public void setActualWest(String actualWest) { this.actualWest = actualWest; }

    public String getControllerStatus() { return controllerStatus; }
    public void setControllerStatus(String controllerStatus) { this.controllerStatus = controllerStatus; }

    public String getActiveEmergencyVehicleId() { return activeEmergencyVehicleId; }
    public void setActiveEmergencyVehicleId(String activeEmergencyVehicleId) { this.activeEmergencyVehicleId = activeEmergencyVehicleId; }

    public String getEmergencyDirection() { return emergencyDirection; }
    public void setEmergencyDirection(String emergencyDirection) { this.emergencyDirection = emergencyDirection; }

    public String getManualRequestedDirection() { return manualRequestedDirection; }
    public void setManualRequestedDirection(String manualRequestedDirection) { this.manualRequestedDirection = manualRequestedDirection; }

    public Instant getManualModeExpiryTime() { return manualModeExpiryTime; }
    public void setManualModeExpiryTime(Instant manualModeExpiryTime) { this.manualModeExpiryTime = manualModeExpiryTime; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
