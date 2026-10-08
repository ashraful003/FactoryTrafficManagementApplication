package com.example.factorytraffic.domain.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public class SignalStateMap {
    private final Map<Direction, SignalColor> signals = new EnumMap<>(Direction.class);

    public SignalStateMap() {
        for (Direction d : Direction.values()) {
            signals.put(d, SignalColor.RED);
        }
    }

    public SignalStateMap(SignalColor defaultColor) {
        for (Direction d : Direction.values()) {
            signals.put(d, defaultColor);
        }
    }

    public SignalStateMap(Map<Direction, SignalColor> initialSignals) {
        this();
        if (initialSignals != null) {
            signals.putAll(initialSignals);
        }
    }

    public static SignalStateMap allRed() {
        return new SignalStateMap(SignalColor.RED);
    }

    public static SignalStateMap forPhase(TrafficPhase phase) {
        SignalStateMap map = allRed();
        if (phase != null && phase != TrafficPhase.ALL_RED) {
            for (Direction d : phase.getActiveDirections()) {
                map.set(d, SignalColor.GREEN);
            }
        }
        return map;
    }

    public SignalColor get(Direction direction) {
        return signals.getOrDefault(direction, SignalColor.RED);
    }

    public void set(Direction direction, SignalColor color) {
        signals.put(direction, Objects.requireNonNull(color, "color must not be null"));
    }

    public Map<Direction, SignalColor> asMap() {
        return new EnumMap<>(signals);
    }

    /**
     * Safety invariant: Rule 1
     * Conflicting traffic phases must NEVER simultaneously receive GREEN.
     */
    public boolean hasConflictingGreens() {
        boolean northSouthGreen = (signals.get(Direction.NORTH) == SignalColor.GREEN || signals.get(Direction.SOUTH) == SignalColor.GREEN);
        boolean eastWestGreen = (signals.get(Direction.EAST) == SignalColor.GREEN || signals.get(Direction.WEST) == SignalColor.GREEN);
        return northSouthGreen && eastWestGreen;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SignalStateMap that)) return false;
        return Objects.equals(signals, that.signals);
    }

    @Override
    public int hashCode() {
        return Objects.hash(signals);
    }

    @Override
    public String toString() {
        return signals.toString();
    }
}
