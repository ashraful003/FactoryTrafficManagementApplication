package com.example.factorytraffic.domain.statemachine;

import com.example.factorytraffic.domain.model.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public class TrafficStateMachine {
    private final long defaultYellowDurationSeconds;
    private final long defaultAllRedDurationSeconds;

    public TrafficStateMachine() {
        this(5, 2);
    }

    public TrafficStateMachine(long yellowDurationSeconds, long allRedDurationSeconds) {
        this.defaultYellowDurationSeconds = yellowDurationSeconds;
        this.defaultAllRedDurationSeconds = allRedDurationSeconds;
    }

    /**
     * Initiates a safe phase transition towards targetPhase.
     * Enforces Rule 1, Rule 2, Rule 3, Rule 4:
     * GREEN phase must NEVER switch directly to conflicting GREEN.
     * Transitions ALWAYS enter YELLOW_CLEARANCE first.
     *
     * @param junction target junction
     * @param targetPhase desired target phase
     * @param now current time
     * @return true if a transition was started or updated
     */
    public boolean initiatePhaseTransition(Junction junction, TrafficPhase targetPhase, Instant now) {
        Objects.requireNonNull(junction, "junction must not be null");
        Objects.requireNonNull(targetPhase, "targetPhase must not be null");

        if (targetPhase == TrafficPhase.ALL_RED) {
            // Direct request to go all-red (e.g. failure or emergency halt)
            applyAllRed(junction, now);
            return true;
        }

        // If junction is already stable in targetPhase, no transition required
        if (junction.getCurrentPhase() == targetPhase && !junction.isTransitioning()) {
            return false;
        }

        // If already transitioning to this target, do nothing
        if (junction.isTransitioning() && junction.getTargetPhase() == targetPhase) {
            return false;
        }

        // Rule 2 & 4: Start safe transition with YELLOW_CLEARANCE
        TrafficPhase current = junction.getCurrentPhase();
        junction.setTargetPhase(targetPhase);
        junction.setTransitionState(TransitionState.YELLOW_CLEARANCE);
        junction.setTransitionStepStartTime(now);

        // Active phase turns YELLOW, other directions remain RED
        SignalStateMap desired = SignalStateMap.allRed();
        if (current != null && current != TrafficPhase.ALL_RED) {
            for (Direction d : current.getActiveDirections()) {
                desired.set(d, SignalColor.YELLOW);
            }
        }
        junction.setDesiredSignals(desired);

        assertSafety(junction);
        return true;
    }

    /**
     * Advances the state machine timer if enough duration has elapsed.
     * Sequence: YELLOW_CLEARANCE -> ALL_RED_CLEARANCE -> TARGET_PHASE STABLE
     *
     * @param junction target junction
     * @param now current time
     * @param yellowDurationSeconds configurable yellow duration
     * @param allRedDurationSeconds configurable all-red duration
     * @return true if transition state progressed
     */
    public boolean advanceTransitionStep(Junction junction, Instant now,
                                         long yellowDurationSeconds, long allRedDurationSeconds) {
        if (!junction.isTransitioning()) {
            return false;
        }

        Instant stepStart = junction.getTransitionStepStartTime();
        if (stepStart == null) {
            junction.setTransitionStepStartTime(now);
            return false;
        }

        long elapsedSeconds = Math.max(0, Duration.between(stepStart, now).getSeconds());

        if (junction.getTransitionState() == TransitionState.YELLOW_CLEARANCE) {
            if (elapsedSeconds >= yellowDurationSeconds) {
                // Step 2: Transition from YELLOW to ALL_RED safety buffer
                junction.setTransitionState(TransitionState.ALL_RED_CLEARANCE);
                junction.setTransitionStepStartTime(now);
                junction.setDesiredSignals(SignalStateMap.allRed());
                assertSafety(junction);
                return true;
            }
        } else if (junction.getTransitionState() == TransitionState.ALL_RED_CLEARANCE) {
            if (elapsedSeconds >= allRedDurationSeconds) {
                // Step 3: Transition from ALL_RED into TARGET_PHASE GREEN
                TrafficPhase target = junction.getTargetPhase();
                if (target == null || target == TrafficPhase.ALL_RED) {
                    target = TrafficPhase.NORTH_SOUTH;
                }
                junction.setCurrentPhase(target);
                junction.setTargetPhase(null);
                junction.setTransitionState(TransitionState.STABLE);
                junction.setPhaseStartTime(now);
                junction.setTransitionStepStartTime(now);

                junction.setDesiredSignals(SignalStateMap.forPhase(target));
                assertSafety(junction);
                return true;
            }
        }

        return false;
    }

    public boolean advanceTransitionStep(Junction junction, Instant now) {
        return advanceTransitionStep(junction, now, defaultYellowDurationSeconds, defaultAllRedDurationSeconds);
    }

    /**
     * Puts the junction into safe ALL_RED state immediately.
     */
    public void applyAllRed(Junction junction, Instant now) {
        junction.setCurrentPhase(TrafficPhase.ALL_RED);
        junction.setTargetPhase(null);
        junction.setTransitionState(TransitionState.STABLE);
        junction.setPhaseStartTime(now);
        junction.setTransitionStepStartTime(now);
        junction.setDesiredSignals(SignalStateMap.allRed());
    }

    /**
     * Enforces Rule 1: Conflicting traffic phases must NEVER simultaneously receive GREEN.
     */
    public void assertSafety(Junction junction) {
        if (junction.getDesiredSignals().hasConflictingGreens()) {
            // Failsafe: immediately clamp to ALL_RED
            applyAllRed(junction, Instant.now());
            throw new IllegalStateException("CRITICAL INVARIANT VIOLATION: Conflicting phases received GREEN!");
        }
    }

    public long getDefaultYellowDurationSeconds() {
        return defaultYellowDurationSeconds;
    }

    public long getDefaultAllRedDurationSeconds() {
        return defaultAllRedDurationSeconds;
    }
}
