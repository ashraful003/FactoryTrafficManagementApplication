package com.example.factorytraffic.domain.model;

import java.util.Set;

public enum TrafficPhase {
    NORTH_SOUTH(Set.of(Direction.NORTH, Direction.SOUTH)),
    EAST_WEST(Set.of(Direction.EAST, Direction.WEST)),
    ALL_RED(Set.of());

    private final Set<Direction> activeDirections;

    TrafficPhase(Set<Direction> activeDirections) {
        this.activeDirections = activeDirections;
    }

    public Set<Direction> getActiveDirections() {
        return activeDirections;
    }

    public boolean conflictsWith(TrafficPhase other) {
        if (this == ALL_RED || other == ALL_RED) {
            return false;
        }
        return this != other;
    }

    public TrafficPhase getConflictingPhase() {
        return switch (this) {
            case NORTH_SOUTH -> EAST_WEST;
            case EAST_WEST -> NORTH_SOUTH;
            case ALL_RED -> null;
        };
    }
}
