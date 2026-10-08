package com.example.factorytraffic.domain.model;

import java.time.Instant;
import java.util.*;

public class TrafficQueue {
    private final Direction direction;
    private final LinkedList<Vehicle> vehicles = new LinkedList<>();

    public TrafficQueue(Direction direction) {
        this.direction = Objects.requireNonNull(direction, "direction must not be null");
    }

    public Direction getDirection() {
        return direction;
    }

    public synchronized int size() {
        return vehicles.size();
    }

    public synchronized boolean isEmpty() {
        return vehicles.isEmpty();
    }

    public synchronized List<Vehicle> getVehicles() {
        return Collections.unmodifiableList(new ArrayList<>(vehicles));
    }

    public synchronized void enqueue(Vehicle vehicle) {
        if (vehicle == null) {
            return;
        }
        // Avoid adding duplicate vehicle instance if already in queue
        boolean alreadyExists = vehicles.stream()
                .anyMatch(v -> v.getVehicleId().equalsIgnoreCase(vehicle.getVehicleId()));
        if (!alreadyExists) {
            vehicles.addLast(vehicle);
        }
    }

    /**
     * Dequeues a vehicle.
     * Policy:
     * 1. If specific vehicleId matches, remove that vehicle.
     * 2. If vehicleId does not match (or is null), remove oldest vehicle (FIFO) if queue not empty.
     * 3. If queue is already empty, return null and never drop size below 0.
     *
     * @param vehicleId optional specific vehicle ID
     * @return the dequeued vehicle, or null if queue was empty
     */
    public synchronized Vehicle dequeue(String vehicleId) {
        if (vehicles.isEmpty()) {
            return null;
        }

        if (vehicleId != null && !vehicleId.isBlank()) {
            Iterator<Vehicle> iterator = vehicles.iterator();
            while (iterator.hasNext()) {
                Vehicle v = iterator.next();
                if (v.getVehicleId().equalsIgnoreCase(vehicleId)) {
                    iterator.remove();
                    return v;
                }
            }
        }

        // Fallback policy: dequeue oldest (head) vehicle
        return vehicles.removeFirst();
    }

    public synchronized int getHighestPriority() {
        int highest = 0;
        for (Vehicle v : vehicles) {
            int p = v.getVehicleType().getPriorityWeight();
            if (p > highest) {
                highest = p;
            }
        }
        return highest;
    }

    public synchronized boolean hasEmergencyVehicle() {
        for (Vehicle v : vehicles) {
            if (v.getVehicleType() == VehicleType.EMERGENCY) {
                return true;
            }
        }
        return false;
    }

    public synchronized long getOldestWaitingSeconds(Instant currentInstant) {
        if (vehicles.isEmpty() || currentInstant == null) {
            return 0;
        }
        Vehicle oldest = vehicles.peekFirst();
        return oldest != null ? oldest.getWaitingSeconds(currentInstant) : 0;
    }

    public synchronized void clear() {
        vehicles.clear();
    }
}
