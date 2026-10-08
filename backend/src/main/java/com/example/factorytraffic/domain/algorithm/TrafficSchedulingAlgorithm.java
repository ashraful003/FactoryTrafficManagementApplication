package com.example.factorytraffic.domain.algorithm;

import com.example.factorytraffic.domain.model.Direction;
import com.example.factorytraffic.domain.model.Junction;
import com.example.factorytraffic.domain.model.TrafficPhase;
import com.example.factorytraffic.domain.model.TrafficQueue;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public class TrafficSchedulingAlgorithm {
    // Configurable algorithmic weights
    private final double queueWeight;
    private final double priorityWeight;
    private final double waitingWeight;
    private final double starvationBonus;
    private final long starvationThresholdSeconds;
    private final double switchingThreshold;
    private final long minGreenSeconds;
    private final long maxGreenSeconds;

    public TrafficSchedulingAlgorithm() {
        this(2.0, 5.0, 0.5, 25.0, 40, 5.0, 10, 60);
    }

    public TrafficSchedulingAlgorithm(double queueWeight, double priorityWeight, double waitingWeight,
                                      double starvationBonus, long starvationThresholdSeconds,
                                      double switchingThreshold, long minGreenSeconds, long maxGreenSeconds) {
        this.queueWeight = queueWeight;
        this.priorityWeight = priorityWeight;
        this.waitingWeight = waitingWeight;
        this.starvationBonus = starvationBonus;
        this.starvationThresholdSeconds = starvationThresholdSeconds;
        this.switchingThreshold = switchingThreshold;
        this.minGreenSeconds = minGreenSeconds;
        this.maxGreenSeconds = maxGreenSeconds;
    }

    public double calculateDirectionScore(TrafficQueue queue, Instant now) {
        if (queue == null || queue.isEmpty()) {
            return 0.0;
        }

        int size = queue.size();
        int highestPriority = queue.getHighestPriority();
        long waitingSeconds = queue.getOldestWaitingSeconds(now);

        double score = (queueWeight * size)
                + (priorityWeight * highestPriority)
                + (waitingWeight * waitingSeconds);

        if (waitingSeconds >= starvationThresholdSeconds) {
            score += starvationBonus;
        }

        // Emergency vehicle in queue gets massive immediate boost
        if (queue.hasEmergencyVehicle()) {
            score += 1000.0;
        }

        return score;
    }

    public double calculatePhaseScore(Junction junction, TrafficPhase phase, Instant now) {
        if (junction == null || phase == null || phase == TrafficPhase.ALL_RED) {
            return 0.0;
        }

        double total = 0.0;
        for (Direction direction : phase.getActiveDirections()) {
            TrafficQueue queue = junction.getQueue(direction);
            total += calculateDirectionScore(queue, now);
        }
        return total;
    }

    /**
     * Determines whether the junction should switch phase and which phase is next.
     * Respects minGreenSeconds, maxGreenSeconds, and switchingThreshold.
     *
     * @param junction the current junction
     * @param now current time
     * @return the optimal phase (either currentPhase to stay, or the conflicting phase to switch)
     */
    public TrafficPhase selectOptimalPhase(Junction junction, Instant now) {
        Objects.requireNonNull(junction, "junction must not be null");

        TrafficPhase current = junction.getCurrentPhase();
        if (current == null || current == TrafficPhase.ALL_RED) {
            current = TrafficPhase.NORTH_SOUTH;
        }

        TrafficPhase other = current.getConflictingPhase();
        if (other == null) {
            return current;
        }

        long greenDuration = junction.getPhaseStartTime() != null
                ? Math.max(0, Duration.between(junction.getPhaseStartTime(), now).getSeconds())
                : 0;

        // Check for emergency vehicle presence in conflicting phase
        boolean currentHasEmergency = hasEmergencyInPhase(junction, current);
        boolean otherHasEmergency = hasEmergencyInPhase(junction, other);

        if (otherHasEmergency && !currentHasEmergency) {
            return other;
        }

        // Enforce Minimum Green Time before voluntary switching, UNLESS other phase has emergency
        if (greenDuration < minGreenSeconds && !otherHasEmergency) {
            return current;
        }

        double currentScore = calculatePhaseScore(junction, current, now);
        double otherScore = calculatePhaseScore(junction, other, now);

        // Maximum Green Time exceeded: force switch if there are waiting vehicles on other phase
        if (greenDuration >= maxGreenSeconds && otherScore > 0.0) {
            return other;
        }

        // Switch only if the challenger phase exceeds current score by the hysteresis switching threshold
        if (otherScore > (currentScore + switchingThreshold)) {
            return other;
        }

        // If current phase has 0 score but other has waiting vehicles, switch immediately
        if (currentScore == 0.0 && otherScore > 0.0) {
            return other;
        }

        return current;
    }

    private boolean hasEmergencyInPhase(Junction junction, TrafficPhase phase) {
        if (phase == null || phase == TrafficPhase.ALL_RED) {
            return false;
        }
        for (Direction d : phase.getActiveDirections()) {
            TrafficQueue q = junction.getQueue(d);
            if (q != null && q.hasEmergencyVehicle()) {
                return true;
            }
        }
        return false;
    }

    public double getQueueWeight() { return queueWeight; }
    public double getPriorityWeight() { return priorityWeight; }
    public double getWaitingWeight() { return waitingWeight; }
    public double getStarvationBonus() { return starvationBonus; }
    public long getStarvationThresholdSeconds() { return starvationThresholdSeconds; }
    public double getSwitchingThreshold() { return switchingThreshold; }
    public long getMinGreenSeconds() { return minGreenSeconds; }
    public long getMaxGreenSeconds() { return maxGreenSeconds; }
}
