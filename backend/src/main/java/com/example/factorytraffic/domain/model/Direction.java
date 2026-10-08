package com.example.factorytraffic.domain.model;

public enum Direction {
    NORTH,
    SOUTH,
    EAST,
    WEST;

    public TrafficPhase getAssociatedPhase() {
        if (this == NORTH || this == SOUTH) {
            return TrafficPhase.NORTH_SOUTH;
        } else {
            return TrafficPhase.EAST_WEST;
        }
    }

    public Direction getOpposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
        };
    }
}
