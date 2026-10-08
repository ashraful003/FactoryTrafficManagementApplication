package com.example.factorytraffic.domain;

import com.example.factorytraffic.domain.model.*;
import com.example.factorytraffic.domain.service.TrafficDomainEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Domain Tests: TrafficDomainEngine Emergency, Manual Override and Clearance")
class TrafficDomainEngineTest {

    private final TrafficDomainEngine engine = new TrafficDomainEngine();

    @Test
    @DisplayName("Emergency vehicle arrival sets EMERGENCY mode and initiates safe transition")
    void emergencyArrival_setsEmergencyModeAndInitiatesSafeTransition() {
        Junction junction = new Junction("A", "Test Junction");
        junction.setCurrentPhase(TrafficPhase.NORTH_SOUTH);
        junction.setDesiredSignals(SignalStateMap.forPhase(TrafficPhase.NORTH_SOUTH));

        Instant now = Instant.now();
        engine.processVehicleArrival(junction, "AMB-911", VehicleType.EMERGENCY, Direction.EAST, 100L, now);

        assertEquals(OperatingMode.EMERGENCY, junction.getMode());
        assertEquals("AMB-911", junction.getActiveEmergencyVehicleId());
        assertEquals(Direction.EAST, junction.getEmergencyDirection());

        // Rule 4: Emergency cannot bypass safety transitions!
        assertEquals(TransitionState.YELLOW_CLEARANCE, junction.getTransitionState(),
                "Must enter YELLOW_CLEARANCE first, never jump directly to GREEN");
        assertEquals(TrafficPhase.EAST_WEST, junction.getTargetPhase());
        assertFalse(junction.getDesiredSignals().hasConflictingGreens());
    }

    @Test
    @DisplayName("Clearing emergency vehicle reverts mode back to AUTOMATIC")
    void emergencyClearance_revertsMode() {
        Junction junction = new Junction("A", "Test Junction");
        junction.setCurrentPhase(TrafficPhase.NORTH_SOUTH);

        Instant now = Instant.now();
        engine.processVehicleArrival(junction, "AMB-911", VehicleType.EMERGENCY, Direction.EAST, 100L, now);
        assertEquals(OperatingMode.EMERGENCY, junction.getMode());

        // Clearance event
        Vehicle cleared = engine.processVehicleClearance(junction, "AMB-911", Direction.EAST, 101L, now.plusSeconds(10));
        assertNotNull(cleared);
        assertEquals("AMB-911", cleared.getVehicleId());
        assertNull(junction.getActiveEmergencyVehicleId());
        assertEquals(OperatingMode.AUTOMATIC, junction.getMode(), "Should return to AUTOMATIC mode after emergency cleared");
    }

    @Test
    @DisplayName("Manual green request switches to MANUAL mode and initiates safe transition")
    void manualGreenRequest_switchesToManualModeAndInitiatesSafeTransition() {
        Junction junction = new Junction("A", "Test Junction");
        junction.setCurrentPhase(TrafficPhase.NORTH_SOUTH);
        junction.setDesiredSignals(SignalStateMap.forPhase(TrafficPhase.NORTH_SOUTH));

        Instant now = Instant.now();
        boolean started = engine.processManualGreenRequest(junction, Direction.WEST, now);

        assertTrue(started);
        assertEquals(OperatingMode.MANUAL, junction.getMode());
        assertEquals(Direction.WEST, junction.getManualRequestedDirection());
        assertEquals(TransitionState.YELLOW_CLEARANCE, junction.getTransitionState(),
                "Manual override must follow safe transition via YELLOW");
        assertEquals(TrafficPhase.EAST_WEST, junction.getTargetPhase());
    }

    @Test
    @DisplayName("Return to automatic reverts mode to AUTOMATIC")
    void returnToAutomatic_revertsMode() {
        Junction junction = new Junction("A", "Test Junction");
        Instant now = Instant.now();
        engine.processManualGreenRequest(junction, Direction.WEST, now);
        assertEquals(OperatingMode.MANUAL, junction.getMode());

        engine.processReturnToAutomatic(junction, now.plusSeconds(5));
        assertEquals(OperatingMode.AUTOMATIC, junction.getMode());
        assertNull(junction.getManualRequestedDirection());
    }

    @Test
    @DisplayName("Competing emergencies from conflicting directions respect first detected")
    void competingEmergencies_respectFirstDetected() {
        Junction junction = new Junction("A", "Test Junction");
        Instant now = Instant.now();

        // First emergency arrives at EAST (EAST_WEST phase)
        engine.processVehicleArrival(junction, "EMG-1", VehicleType.EMERGENCY, Direction.EAST, 1L, now);
        assertEquals("EMG-1", junction.getActiveEmergencyVehicleId());
        assertEquals(Direction.EAST, junction.getEmergencyDirection());

        // Second emergency arrives at NORTH (conflicting NORTH_SOUTH phase)
        engine.processVehicleArrival(junction, "EMG-2", VehicleType.EMERGENCY, Direction.NORTH, 2L, now.plusMillis(10));

        // First emergency retains preemption
        assertEquals("EMG-1", junction.getActiveEmergencyVehicleId());
        assertEquals(Direction.EAST, junction.getEmergencyDirection());
        // Both vehicles are safely queued in their respective directions
        assertEquals(1, junction.getQueue(Direction.EAST).size());
        assertEquals(1, junction.getQueue(Direction.NORTH).size());
    }
}
