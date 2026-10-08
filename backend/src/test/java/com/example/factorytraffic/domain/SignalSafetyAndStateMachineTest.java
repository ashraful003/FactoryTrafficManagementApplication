package com.example.factorytraffic.domain;

import com.example.factorytraffic.domain.model.*;
import com.example.factorytraffic.domain.statemachine.TrafficStateMachine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Domain Tests: Signal Safety Invariants and State Machine Transitions")
class SignalSafetyAndStateMachineTest {

    private final TrafficStateMachine stateMachine = new TrafficStateMachine(5, 2);

    @Test
    @DisplayName("Rule 1: Conflicting phases must never simultaneously receive GREEN")
    void rule1_conflictingPhasesNeverGreenSimultaneously() {
        SignalStateMap map = new SignalStateMap();
        map.set(Direction.NORTH, SignalColor.GREEN);
        map.set(Direction.SOUTH, SignalColor.GREEN);
        map.set(Direction.EAST, SignalColor.RED);
        map.set(Direction.WEST, SignalColor.RED);

        assertFalse(map.hasConflictingGreens(), "NORTH_SOUTH green alone should not have conflicting greens");

        // Attempt to turn EAST GREEN while NORTH is GREEN
        map.set(Direction.EAST, SignalColor.GREEN);
        assertTrue(map.hasConflictingGreens(), "NORTH and EAST green simultaneously MUST trigger conflicting greens");
    }

    @Test
    @DisplayName("Rule 2: GREEN phase must never directly switch to conflicting GREEN phase")
    void rule2_greenNeverDirectlySwitchesToConflictingGreen() {
        Junction junction = new Junction("A", "Test Junction");
        junction.setCurrentPhase(TrafficPhase.NORTH_SOUTH);
        junction.setDesiredSignals(SignalStateMap.forPhase(TrafficPhase.NORTH_SOUTH));

        Instant t0 = Instant.now();
        // Request transition to EAST_WEST
        boolean initiated = stateMachine.initiatePhaseTransition(junction, TrafficPhase.EAST_WEST, t0);

        assertTrue(initiated);
        assertEquals(TrafficPhase.NORTH_SOUTH, junction.getCurrentPhase(), "Current phase must remain NORTH_SOUTH during yellow clearance");
        assertEquals(TrafficPhase.EAST_WEST, junction.getTargetPhase(), "Target phase must be set to EAST_WEST");
        assertEquals(TransitionState.YELLOW_CLEARANCE, junction.getTransitionState());

        // Desired signals must be YELLOW on previous green directions, RED on others
        assertEquals(SignalColor.YELLOW, junction.getDesiredSignals().get(Direction.NORTH));
        assertEquals(SignalColor.YELLOW, junction.getDesiredSignals().get(Direction.SOUTH));
        assertEquals(SignalColor.RED, junction.getDesiredSignals().get(Direction.EAST));
        assertEquals(SignalColor.RED, junction.getDesiredSignals().get(Direction.WEST));
        assertFalse(junction.getDesiredSignals().hasConflictingGreens());
    }

    @Test
    @DisplayName("Step-by-step transition sequence: YELLOW -> ALL_RED -> TARGET_GREEN")
    void transitionSequenceFollowsYellowToAllRedToGreen() {
        Junction junction = new Junction("A", "Test Junction");
        junction.setCurrentPhase(TrafficPhase.NORTH_SOUTH);
        junction.setDesiredSignals(SignalStateMap.forPhase(TrafficPhase.NORTH_SOUTH));

        Instant t0 = Instant.now();
        stateMachine.initiatePhaseTransition(junction, TrafficPhase.EAST_WEST, t0);

        // Step 1: Still in YELLOW before 5 seconds
        boolean step1 = stateMachine.advanceTransitionStep(junction, t0.plusSeconds(3), 5, 2);
        assertFalse(step1, "Should not advance before yellow duration elapses");
        assertEquals(TransitionState.YELLOW_CLEARANCE, junction.getTransitionState());

        // Step 2: At 5 seconds, advances to ALL_RED_CLEARANCE
        boolean step2 = stateMachine.advanceTransitionStep(junction, t0.plusSeconds(5), 5, 2);
        assertTrue(step2, "Must advance to ALL_RED after yellow duration");
        assertEquals(TransitionState.ALL_RED_CLEARANCE, junction.getTransitionState());
        assertEquals(SignalColor.RED, junction.getDesiredSignals().get(Direction.NORTH));
        assertEquals(SignalColor.RED, junction.getDesiredSignals().get(Direction.SOUTH));
        assertEquals(SignalColor.RED, junction.getDesiredSignals().get(Direction.EAST));
        assertEquals(SignalColor.RED, junction.getDesiredSignals().get(Direction.WEST));

        // Step 3: Before 2 seconds of ALL_RED, remains in ALL_RED
        Instant tAllRed = junction.getTransitionStepStartTime();
        boolean step3 = stateMachine.advanceTransitionStep(junction, tAllRed.plusSeconds(1), 5, 2);
        assertFalse(step3);
        assertEquals(TransitionState.ALL_RED_CLEARANCE, junction.getTransitionState());

        // Step 4: After 2 seconds of ALL_RED, completes into EAST_WEST GREEN
        boolean step4 = stateMachine.advanceTransitionStep(junction, tAllRed.plusSeconds(2), 5, 2);
        assertTrue(step4);
        assertEquals(TransitionState.STABLE, junction.getTransitionState());
        assertEquals(TrafficPhase.EAST_WEST, junction.getCurrentPhase());
        assertNull(junction.getTargetPhase());
        assertEquals(SignalColor.GREEN, junction.getDesiredSignals().get(Direction.EAST));
        assertEquals(SignalColor.GREEN, junction.getDesiredSignals().get(Direction.WEST));
        assertEquals(SignalColor.RED, junction.getDesiredSignals().get(Direction.NORTH));
        assertEquals(SignalColor.RED, junction.getDesiredSignals().get(Direction.SOUTH));
        assertFalse(junction.getDesiredSignals().hasConflictingGreens());
    }

    @Test
    @DisplayName("Failsafe: assertSafety immediately detects and clamps conflicting greens")
    void assertSafety_clampsToAllRedIfConflictingGreensViolated() {
        Junction junction = new Junction("A", "Test Junction");
        junction.getDesiredSignals().set(Direction.NORTH, SignalColor.GREEN);
        junction.getDesiredSignals().set(Direction.EAST, SignalColor.GREEN);

        assertThrows(IllegalStateException.class, () -> stateMachine.assertSafety(junction));
        // Verify junction was clamped to ALL_RED
        assertEquals(TrafficPhase.ALL_RED, junction.getCurrentPhase());
        assertEquals(SignalColor.RED, junction.getDesiredSignals().get(Direction.NORTH));
        assertEquals(SignalColor.RED, junction.getDesiredSignals().get(Direction.EAST));
    }
}
