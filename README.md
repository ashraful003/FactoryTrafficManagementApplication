# Factory Traffic Management System

An event-driven, safety-critical traffic control application for the internal road network of a garment manufacturing facility.

---

## Project Overview

The **Factory Traffic Management System** orchestrates internal vehicle flow (forklifts, material trucks, employee vehicles, and emergency units) through multi-directional factory intersections (initially **Junction A**). Unlike CRUD applications, this system is an event-driven control system that enforces strict safety invariants, separates desired logical commands from confirmed physical hardware states, and guarantees safe transition dynamics.

### Key Capabilities
- **Event-Driven Vehicle Detection**: Processes sensor events (`VEHICLE_ARRIVED`, `VEHICLE_CLEARED`) with deduplication.
- **Priority-Aware Scheduling**: Prioritizes emergency vehicles, logistics trucks, and forklifts while preventing starvation of employee vehicles.
- **Strict Safety Invariants**: Conflicting green signals are mathematically impossible; green signals never directly swap without mandatory yellow clearance and all-red buffers.
- **Hardware Controller Port/Adapter Architecture**: Decouples physical actuators and simulator adapters from core traffic rules, making it ready for future MQTT brokers.
- **Idempotency & Concurrency Safety**: Guaranteed per-junction serialization and persistent deduplication keys.
- **State Recovery Across Restarts**: Reconciles interrupted transitions into all-red safety buffers without blindly assuming physical states.
- **Interactive Web Dashboard & Simulator**: Built-in visual intersection monitor, manual override controls, and scenario injection suite.

---

## Architecture

The project follows **Hexagonal / Clean Architecture (Ports & Adapters)** principles:

```
                                  +------------------------------------+
                                  |         REST API / Web UI          |
                                  +------------------------------------+
                                                    |
                                  +------------------------------------+
                                  |    Application Service Layer       |
                                  | (Locking, Idempotency, Transaction)|
                                  +------------------------------------+
                                                    |
                                  +------------------------------------+
                                  |       Traffic Domain Engine        |
                                  |  - State Machine (Transitions)     |
                                  |  - Scoring Algorithm (Fairness)    |
                                  +------------------------------------+
                                         |                      |
                    +--------------------+----+    +------------+--------------------+
                    |  Persistence Port       |    |     Controller Port             |
                    +-------------------------+    +---------------------------------+
                    | PostgreSQL Adapter      |    | RestControllerSimulatorAdapter  |
                    | (Spring Data JPA)       |    | (Auto-ACK toggle / future MQTT) |
                    +-------------------------+    +---------------------------------+
```

### Layer Isolation Guarantees
- The **Domain Engine** (`com.example.factorytraffic.domain.*`) has **ZERO dependencies** on Spring Boot, JPA, HTTP, MQTT, PostgreSQL, or web frameworks.
- It is 100% unit testable in pure Java without mocks or containers.
- Communication with external actuators is mediated strictly through `TrafficControllerPort`.

---

## Traffic-Control Algorithm

The deterministic scheduling engine evaluates queues dynamically in **AUTOMATIC** mode to select the next green phase:

$$\text{DirectionScore} = (W_q \cdot \text{QueueSize}) + (W_p \cdot \text{HighestPriority}) + (W_w \cdot \text{OldestWaitTimeSeconds}) + \text{StarvationBonus}$$

### Configured Weights
- **$W_q$ (Queue Weight)**: `2.0` (favors throughput for dense lines)
- **$W_p$ (Priority Weight)**: `5.0`
  - `EMERGENCY`: Weight `4` (+ `1000.0` emergency preemption boost)
  - `TRUCK`: Weight `3`
  - `FORKLIFT`: Weight `2`
  - `EMPLOYEE_VEHICLE`: Weight `1`
- **$W_w$ (Waiting Weight)**: `0.5` per second of wait time
- **Starvation Threshold**: `40 seconds`. If vehicles wait longer than 40s, an extra `+25.0` bonus is added.
- **Switching Hysteresis Threshold**: `5.0`. A challenger phase must exceed current phase score by at least 5.0 to prevent rapid flapping.
- **Phase Constraints**: Minimum green time of `10s` before routine switching (unless preemption occurs), and maximum green cap of `60s`.

---

## Signal-State Transitions

The system enforces six core safety invariants:
1. **Rule 1**: Conflicting traffic phases (`NORTH_SOUTH` vs `EAST_WEST`) can **NEVER** simultaneously receive GREEN.
2. **Rule 2**: A GREEN phase can **NEVER** directly switch to a conflicting GREEN phase.
3. **Rule 3**: Manual administrator control cannot bypass safety transitions.
4. **Rule 4**: Emergency vehicles cannot bypass safety transitions.
5. **Rule 5**: Invalid commands are rejected and never leave the intersection in an undefined state.
6. **Rule 6**: Clients send intent/commands; the backend state machine decides how to transition safely.

### Required Transition Pipeline
```
   NORTH_SOUTH GREEN (Active)
               |
               v
   NORTH_SOUTH YELLOW (Yellow Clearance: 5s)
               |
               v
   ALL RED (Safety Buffer: 2s)
               |
               v
   EAST_WEST GREEN (Active)
```

---

## Persistence Strategy

Data persistence is implemented using **PostgreSQL** (with H2 in-memory for testing):
- `junctions`: Stores configuration, active operating mode, desired signal states, controller-confirmed actual states, active emergency trackers, and versioning.
- `queue_vehicles`: Retains FIFO ordered vehicle records per direction with sequence numbers and timestamps.
- `processed_events`: Idempotency tracking table storing all unique `event_id` records.
- `traffic_commands`: Correlates outbound commands with controller acknowledgements and retry counts.
- `audit_events`: Immutable audit trail for post-incident diagnostics.

---

## Restart Recovery

When the backend restarts:
1. **Persisted Queues & Junctions**: Restored from PostgreSQL into memory.
2. **Incomplete Transitions**: If the system was interrupted during `YELLOW_CLEARANCE` or `ALL_RED_CLEARANCE`, it is safely clamped to `ALL_RED`.
3. **Physical State Reconciliation**: Confirmed controller state is flagged as `UNKNOWN` until verified via status inquiry or ACK.
4. **Pending Commands**: Commands pending across restarts are flagged `TIMEOUT` to avoid duplicate actuation.

---

## Concurrency Strategy

- Every junction is guarded by a dedicated `ReentrantLock` (`fair = true`) managed in `TrafficManagementApplicationService`.
- All operations affecting the same junction (burst sensor arrivals, manual overrides, ACKs) are serialized sequentially in arrival order.
- Verified in **Scenario 9** with 5 concurrent requests fired at $T=0, 4, 8, 12, 17$ ms.

---

## Controller Communication

Hardware communication is abstracted via `TrafficControllerPort`:
- `RestControllerSimulatorAdapter`: Dispatches commands via REST and provides an auto-ACK simulation mode (configurable to toggle instant ACK or delayed/timed-out ACK).
- `MqttControllerAdapterPlaceholder`: Ready for MQTT brokers (`factory/junction/{id}/commands` and `factory/junction/{id}/acks`).

---

## Assumptions / Questions / Requirement Issues

As instructed in **Section 17 & 19 of the Assessment Specification**, the specification intentionally contains incomplete, ambiguous, and conflicting areas where engineering judgment must be exercised. Below are our documented decisions for all 21 items:

1. **What exactly counts as a conflicting traffic movement?**  
   *Decision*: `NORTH_SOUTH` and `EAST_WEST` are strictly conflicting phases. Any simultaneous green across conflicting approaches is an unacceptable safety invariant violation. If an invalid state ever arises, the system immediately clamps to `ALL_RED`.

2. **How is maximum waiting time calculated?**  
   *Decision*: Measured from the `arrived_at` timestamp of the head (oldest) vehicle in that direction's queue compared against current server time.

3. **What happens if a `VEHICLE_CLEARED` event arrives without a corresponding arrival?**  
   *Decision*: The queue size is bounded at `0` (preventing negative queue counters such as `queue = -1`). The unmatched clearance is recorded as an `UNMATCHED_VEHICLE_CLEARED_EMPTY_QUEUE` audit event for operational diagnostics.

4. **How should delayed sensor events be treated?**  
   *Decision*: Events older than 5 minutes relative to server time are flagged with an audit warning. The event is still recorded, but waiting time scoring is bounded to prevent stale events from monopolizing green phases.

5. **How should out-of-order events be treated?**  
   *Decision*: Monotonic `sequence_no` is tracked per approach sensor. Incoming events are inserted or removed based on vehicle identifier and sequence ordering, ensuring deterministic queue state regardless of arrival order.

6. **How should duplicate events be detected?**  
   *Decision*: Idempotency keys are maintained in the persistent `processed_events` table. If the same `event_id` is submitted more than once, the system logs `DUPLICATE_SENSOR_EVENT` and rejects duplicate state modification, preserving queue consistency.

7. **Is `event_id`, `sequence_no`, or another mechanism authoritative?**  
   *Decision*: `event_id` is authoritative for idempotency and network deduplication. `sequence_no` is authoritative for sensor telemetry ordering and FIFO lane placement.

8. **Which timestamp is authoritative: sensor time or server time?**  
   *Decision*: Sensor timestamp is used for queue wait duration and vehicle arrival logging; Server timestamp is authoritative for state machine transitions, command timeouts, and audit trail ordering.

9. **How long does manual override remain active?**  
   *Decision*: Manual override has a configurable 120-second lease timeout. If the administrator does not renew or return control, the scheduler automatically reverts the junction to `AUTOMATIC` mode to prevent permanent deadlocks.

10. **What happens when two administrators issue commands simultaneously?**  
    *Decision*: Handled via per-junction `ReentrantLock` (`fair = true`). Commands are processed in strict arrival order; the second command safely supersedes the target phase or follows the standard transition pipeline.

11. **Should emergency mode override manual mode?**  
    *Decision*: Yes. Emergency preemption strictly overrides manual mode. Once the emergency vehicle clears, the junction returns to manual mode (if still within the active lease) or reverts to automatic mode.

12. **What happens if two emergency vehicles arrive from conflicting directions?**  
    *Decision*: Handled via First-In-First-Served (FIFO) priority preemption. The first detected emergency completes safe green clearance; the conflicting emergency is queued with top priority and served immediately through the standard yellow/all-red transition.

13. **When is an emergency considered cleared?**  
    *Decision*: An emergency is cleared either upon receipt of a `VEHICLE_CLEARED` event matching the emergency vehicle ID, or after an automatic safety fallback timeout of 90 seconds.

14. **How long should the backend wait for a controller acknowledgement?**  
    *Decision*: The default timeout is `3000 ms` (3 seconds) per command.

15. **Should controller commands be retried?**  
    *Decision*: Yes. Commands are retried up to `2` times upon timeout before declaring controller failure.

16. **How should duplicate ACK messages be treated?**  
    *Decision*: Duplicate ACKs are matched by `command_id` and acknowledged idempotently without triggering duplicate phase transitions or audit noise.

17. **What happens when the controller reconnects?**  
    *Decision*: When an `ONLINE` status event or valid ACK is received from an offline controller, the status is restored to `ONLINE`, and operating mode transitions from `FAILURE` back to `AUTOMATIC`.

18. **What happens if desired state and actual state disagree?**  
    *Decision*: The junction controller status is set to `WARNING` / `DEGRADED`, a visual alert is raised on the dashboard, and safe signal rules are enforced until states resynchronize.

19. **What should happen when the backend loses communication with the controller?**  
    *Decision*: After maximum retries fail, controller status becomes `OFFLINE`, junction mode transitions to `FAILURE`, and an audit alert `CONTROLLER_TIMEOUT` is recorded.

20. **How should signal timers behave after a server restart?**  
    *Decision*: Signal timers reset to zero upon startup recovery. The recovered phase restarts its timer so that traffic receives the full minimum green duration without truncation.

21. **How should the application recover if it restarts in the middle of a signal transition?**  
    *Decision*: If the server restarts during `YELLOW_CLEARANCE` or `ALL_RED_CLEARANCE`, startup recovery clamps the junction to `ALL_RED` safety clearance, restores vehicle queues from PostgreSQL, and flags controller state as `UNKNOWN` until confirmed by physical telemetry.

---

## Architectural Decisions

- **Domain Isolation**: By keeping the traffic state machine and scheduling algorithm purely in POJOs without Spring or JPA dependencies, the safety invariants can be thoroughly unit tested and verified independently of database or web containers.
- **Per-Junction Concurrency Locking**: A synchronized lock per junction guarantees FIFO serialized execution while allowing multiple junctions (Junction A, B, C) to run concurrently without bottlenecking each other.
- **Idempotency Strategy**: The `processed_events` table enforces unique constraint semantics, preventing duplicate network retries from double-incrementing queues.
- **State vs Actuation Separation**: Desired state represents intent; actual state represents hardware truth. This architecture prevents "blind actuation" bugs.

---

## AI / Tool Usage

- **Tool Name**: Antigravity AI Pair Programming Agent (Google DeepMind).
- **Assistance Provided**: Assisted in generating the clean layered architecture structure, scaffolding domain models and state machines, writing automated JUnit 5 / Mockito test cases, crafting the interactive web dashboard with SVG/CSS intersection layout, and documenting edge cases and assumptions.

---

## API Usage & Endpoints

| Endpoint | Method | Description |
|---|---|---|
| `/api/junctions` | GET | List all junctions |
| `/api/junctions/{id}/status` | GET | Current status (mode, phase, signals, queues) |
| `/api/junctions` | POST | Register new junction (e.g. Junction B) |
| `/api/sensor-events` | POST | Ingest vehicle arrival / clearance |
| `/api/junctions/{id}/commands` | POST | Manual override or return to auto |
| `/api/controller-events` | POST | Ingest physical controller ACK |
| `/api/junctions/{id}/history` | GET | Retrieve audit history |
| `/api/simulation/auto-ack` | POST | Toggle controller auto-ACK (testing tool) |
| `/api/simulation/concurrency-test` | POST | Trigger Scenario 9 burst concurrency test |
| `/api/simulation/reset` | POST | Reset Junction A to clean baseline |

### Example Payloads

#### Sensor Event (Arrival)
```json
POST /api/sensor-events
{
  "event_id": "evt-10001",
  "junction_id": "A",
  "direction": "NORTH",
  "event_type": "VEHICLE_ARRIVED",
  "vehicle_id": "VH-501",
  "vehicle_type": "FORKLIFT",
  "sequence_no": 1501,
  "timestamp": "2026-10-05T10:15:20Z"
}
```

#### Manual Override
```json
POST /api/junctions/A/commands
{
  "command": "MANUAL_GREEN_REQUEST",
  "direction": "WEST"
}
```

---

## Demo Scenarios

1. **Scenario 1 — Normal Traffic**: Ingest arrivals on North and East; observe scheduling algorithm picking phase based on queue size and wait time.
2. **Scenario 2 — Priority Traffic**: Send a TRUCK on East and an EMPLOYEE_VEHICLE on North; observe truck receiving higher clearance score.
3. **Scenario 3 — Emergency Preemption**: Send an EMERGENCY vehicle on East while North is GREEN. Observe transition sequence: `NORTH GREEN` $\to$ `NORTH YELLOW` $\to$ `ALL RED` $\to$ `EAST GREEN`.
4. **Scenario 4 — Manual Override**: Send `MANUAL_GREEN_REQUEST` for West. System enters `MANUAL` mode safely. Send `RETURN_TO_AUTOMATIC` to resume.
5. **Scenario 5 — Duplicate Event**: Send identical `event_id` twice. Queue increases only once; duplicate is logged.
6. **Scenario 6 — Clearance**: Send `VEHICLE_CLEARED` for an existing vehicle; queue decrements safely.
7. **Scenario 7 — Controller Failure**: Disable Auto-ACK (`/api/simulation/auto-ack?enabled=false`) and trigger a signal change. System retries and transitions to `OFFLINE` / `FAILURE` mode upon timeout.
8. **Scenario 8 — Restart Recovery**: Restart backend; observe queues restored and transitions clamped to safe state.
9. **Scenario 9 — Concurrency Burst**: Trigger `/api/simulation/concurrency-test` to execute the 5 simultaneous events at $T=0, 4, 8, 12, 17$ ms and verify consistency.

---

## Running the Application

### Prerequisites
- Java 21 LTS
- PostgreSQL 14+ (or run via provided `docker-compose.yml`)
- Maven 3.9+ (or use bundled `./mvnw`)

### Quick Start
```bash
# 1. Run tests
cd backend
./mvnw clean test

# 2. Run backend application (serves dashboard at http://localhost:8080)
./mvnw spring-boot:run
```

Access the Web Dashboard at: `http://localhost:8080/`  
Access OpenAPI / Swagger at: `http://localhost:8080/swagger-ui.html`
