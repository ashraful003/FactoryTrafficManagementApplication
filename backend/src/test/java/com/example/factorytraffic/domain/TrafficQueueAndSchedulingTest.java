package com.example.factorytraffic.domain;

import com.example.factorytraffic.domain.algorithm.TrafficSchedulingAlgorithm;
import com.example.factorytraffic.domain.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Domain Tests: Traffic Queue Management and Scheduling Algorithm")
class TrafficQueueAndSchedulingTest {

    @Test
    @DisplayName("Queue size increases on arrival and decreases on clearance")
    void queue_increasesOnArrivalAndDecreasesOnClearance() {
        TrafficQueue queue = new TrafficQueue(Direction.NORTH);
        assertEquals(0, queue.size());

        Vehicle v1 = new Vehicle("VH-1", VehicleType.FORKLIFT, Direction.NORTH, 1L, Instant.now());
        queue.enqueue(v1);
        assertEquals(1, queue.size());

        Vehicle v2 = new Vehicle("VH-2", VehicleType.TRUCK, Direction.NORTH, 2L, Instant.now());
        queue.enqueue(v2);
        assertEquals(2, queue.size());

        // Dequeue specific vehicle
        Vehicle dequeued = queue.dequeue("VH-1");
        assertNotNull(dequeued);
        assertEquals("VH-1", dequeued.getVehicleId());
        assertEquals(1, queue.size());

        // Dequeue next vehicle FIFO
        Vehicle dequeued2 = queue.dequeue(null);
        assertNotNull(dequeued2);
        assertEquals("VH-2", dequeued2.getVehicleId());
        assertEquals(0, queue.size());
    }

    @Test
    @DisplayName("Queue never becomes negative when clearance executed on empty queue")
    void queue_neverBecomesNegative() {
        TrafficQueue queue = new TrafficQueue(Direction.WEST);
        assertEquals(0, queue.size());

        Vehicle dequeued = queue.dequeue("NON-EXISTENT");
        assertNull(dequeued);
        assertEquals(0, queue.size(), "Queue size must never drop below 0");
    }

    @Test
    @DisplayName("Priority score ordering: EMERGENCY > TRUCK > FORKLIFT > EMPLOYEE_VEHICLE")
    void priorityOrdering_verified() {
        assertTrue(VehicleType.EMERGENCY.getPriorityWeight() > VehicleType.TRUCK.getPriorityWeight());
        assertTrue(VehicleType.TRUCK.getPriorityWeight() > VehicleType.FORKLIFT.getPriorityWeight());
        assertTrue(VehicleType.FORKLIFT.getPriorityWeight() > VehicleType.EMPLOYEE_VEHICLE.getPriorityWeight());
    }

    @Test
    @DisplayName("Scheduling algorithm awards starvation bonus to long waiting queues")
    void scheduling_starvationProtectionWorks() {
        TrafficSchedulingAlgorithm algorithm = new TrafficSchedulingAlgorithm(
                2.0, 5.0, 0.5, 25.0, 40, 5.0, 10, 60);

        TrafficQueue freshQueue = new TrafficQueue(Direction.EAST);
        freshQueue.enqueue(new Vehicle("VH-1", VehicleType.EMPLOYEE_VEHICLE, Direction.EAST, 1L, Instant.now()));

        TrafficQueue starvingQueue = new TrafficQueue(Direction.WEST);
        // Vehicle waiting for 50 seconds (exceeds 40s threshold)
        starvingQueue.enqueue(new Vehicle("VH-2", VehicleType.EMPLOYEE_VEHICLE, Direction.WEST, 2L,
                Instant.now().minusSeconds(50)));

        Instant now = Instant.now();
        double freshScore = algorithm.calculateDirectionScore(freshQueue, now);
        double starvingScore = algorithm.calculateDirectionScore(starvingQueue, now);

        assertTrue(starvingScore > freshScore + 25.0,
                "Starving queue must receive substantial starvation bonus");
    }

    @Test
    @DisplayName("Optimal phase selection switches when competing phase has higher score exceeding threshold")
    void scheduling_selectOptimalPhase_switchesOnScoreThreshold() {
        TrafficSchedulingAlgorithm algorithm = new TrafficSchedulingAlgorithm(
                2.0, 5.0, 0.5, 25.0, 40, 5.0, 10, 60);

        Junction junction = new Junction("A", "Test Junction");
        junction.setCurrentPhase(TrafficPhase.NORTH_SOUTH);
        // Give current phase 15 seconds of green (past minGreenSeconds = 10s)
        junction.setPhaseStartTime(Instant.now().minusSeconds(15));

        // Add 5 trucks to EAST
        for (int i = 0; i < 5; i++) {
            junction.getQueue(Direction.EAST).enqueue(
                    new Vehicle("TRK-" + i, VehicleType.TRUCK, Direction.EAST, i, Instant.now().minusSeconds(20)));
        }

        TrafficPhase optimal = algorithm.selectOptimalPhase(junction, Instant.now());
        assertEquals(TrafficPhase.EAST_WEST, optimal, "Should select EAST_WEST due to high truck queue");
    }
}
