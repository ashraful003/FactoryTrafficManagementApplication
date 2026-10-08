# Factory Traffic Management System - Architecture & Design

## 1. High-Level Architecture Overview

```
                      +---------------------------------------+
                      |       Web Dashboard / REST API        |
                      +---------------------------------------+
                                          |
                      +---------------------------------------+
                      |     Application Service Orchestrator  |
                      |  (Concurrency Lock, Idempotency Gate) |
                      +---------------------------------------+
                                          |
                      +---------------------------------------+
                      |         Traffic Domain Engine         |
                      |    (State Machine, Scheduling Alg)    |
                      +---------------------------------------+
                             |                         |
              +--------------+----------+    +---------+------------+
              |   Persistence Port      |    |   Controller Port    |
              +-------------------------+    +----------------------+
              | PostgreSQL Adapter      |    | REST Simulator /     |
              | (Spring Data JPA)       |    | Future MQTT Adapter  |
              +-------------------------+    +----------------------+
```

## 2. Invariant Rules Enforcement
- **Rule 1 (Conflicting Greens Prohibition)**: `SignalStateMap.hasConflictingGreens()` validates that `NORTH_SOUTH` and `EAST_WEST` phases can never possess simultaneous green lights. Any illegal state triggers failsafe clamp to `ALL_RED`.
- **Rule 2 (No Direct Phase Switching)**: All green-to-green transitions must transition through `YELLOW_CLEARANCE` followed by `ALL_RED_CLEARANCE`.
- **Rule 3 & 4 (Manual Override & Emergency Priority Safety)**: Neither emergency preemption nor administrator manual commands can bypass yellow clearance or all-red buffer periods.
- **Rule 5 & 6 (Command Sanitization & State Protection)**: Clients never write signal states directly; they send high-level intents which are executed by the state machine.

## 3. Scheduling & Priority Engine
```
PhaseScore = sum(DirectionScores)
DirectionScore = (2.0 * QueueSize) 
               + (5.0 * HighestVehiclePriority) 
               + (0.5 * OldestWaitingTimeSeconds) 
               + StarvationBonus(25.0 if waiting > 40s)
```
Emergency vehicles automatically inject `+1000.0` priority boost.

## 4. Concurrency & Idempotency Model
- Concurrency serialized per junction using `ReentrantLock` in `TrafficManagementApplicationService`.
- Idempotency verified via unique `event_id` stored in `processed_events` table before modifying queue state.
