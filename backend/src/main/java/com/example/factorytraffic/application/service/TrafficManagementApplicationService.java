package com.example.factorytraffic.application.service;

import com.example.factorytraffic.domain.model.*;
import com.example.factorytraffic.domain.port.*;
import com.example.factorytraffic.domain.service.TrafficDomainEngine;
import com.example.factorytraffic.infrastructure.adapter.RestControllerSimulatorAdapter;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class TrafficManagementApplicationService {

    private static final Logger log = LoggerFactory.getLogger(TrafficManagementApplicationService.class);

    private final JunctionRepositoryPort junctionRepository;
    private final ProcessedEventRepositoryPort processedEventRepository;
    private final AuditEventRepositoryPort auditEventRepository;
    private final TrafficCommandRepositoryPort commandRepository;
    private final TrafficControllerPort controllerPort;
    private final RestControllerSimulatorAdapter simulatorAdapter;
    private final TrafficDomainEngine trafficEngine;

    private final Map<String, ReentrantLock> junctionLocks = new ConcurrentHashMap<>();

    @Value("${traffic.timings.yellow-duration-seconds:5}")
    private long yellowDurationSeconds = 5;

    @Value("${traffic.timings.all-red-duration-seconds:2}")
    private long allRedDurationSeconds = 2;

    @Value("${traffic.timings.ack-timeout-millis:3000}")
    private long ackTimeoutMillis = 3000;

    @Value("${traffic.timings.max-command-retries:2}")
    private int maxCommandRetries = 2;

    public TrafficManagementApplicationService(
            JunctionRepositoryPort junctionRepository,
            ProcessedEventRepositoryPort processedEventRepository,
            AuditEventRepositoryPort auditEventRepository,
            TrafficCommandRepositoryPort commandRepository,
            TrafficControllerPort controllerPort,
            RestControllerSimulatorAdapter simulatorAdapter) {
        this.junctionRepository = junctionRepository;
        this.processedEventRepository = processedEventRepository;
        this.auditEventRepository = auditEventRepository;
        this.commandRepository = commandRepository;
        this.controllerPort = controllerPort;
        this.simulatorAdapter = simulatorAdapter;
        this.trafficEngine = new TrafficDomainEngine();

        // Wire auto-ack callback from simulator
        this.simulatorAdapter.setAckListener(cmd -> {
            try {
                handleControllerAck(cmd.getCommandId(), "ACK", cmd.getRequestedState(), Instant.now());
            } catch (Exception e) {
                log.error("Error executing auto-ack callback for command {}: {}", cmd.getCommandId(), e.getMessage());
            }
        });
    }

    private ReentrantLock getLockForJunction(String junctionId) {
        return junctionLocks.computeIfAbsent(junctionId, k -> new ReentrantLock(true));
    }

    @PostConstruct
    public void onStartup() {
        log.info("[STARTUP] Initializing Factory Traffic Management System...");
        recoverOrInitializeState();
    }

    /**
     * Startup Recovery (Section 24):
     * Restores persisted state, resolves pending/interrupted transitions,
     * marks physical states as UNKNOWN unless confirmed, recovers pending commands.
     */
    @Transactional
    public void recoverOrInitializeState() {
        List<Junction> junctions = junctionRepository.findAll();
        if (junctions.isEmpty()) {
            log.info("[STARTUP] No existing junctions found. Initializing default Junction A.");
            Junction junctionA = new Junction("A", "Factory Gate Junction A");
            junctionRepository.save(junctionA);
            auditEventRepository.record(AuditEvent.create("A", "JUNCTION_CREATED", "Default Junction A initialized."));
            return;
        }

        for (Junction junction : junctions) {
            ReentrantLock lock = getLockForJunction(junction.getId());
            lock.lock();
            try {
                log.info("[RECOVERY] Recovering persisted state for Junction {}", junction.getId());

                List<TrafficCommand> pending = commandRepository.findPendingByJunctionId(junction.getId());
                for (TrafficCommand cmd : pending) {
                    log.warn("[RECOVERY] Found unconfirmed pending command {} from prior run. Marking TIMEOUT.", cmd.getCommandId());
                    cmd.setStatus(CommandStatus.TIMEOUT);
                    commandRepository.save(cmd);
                }

                if (junction.isTransitioning()) {
                    log.warn("[RECOVERY] Junction {} was interrupted during {}. Safely resetting to ALL_RED safety buffer.",
                            junction.getId(), junction.getTransitionState());
                    trafficEngine.getStateMachine().applyAllRed(junction, Instant.now());
                }

                junction.setControllerStatus(ControllerStatus.UNKNOWN);
                junctionRepository.save(junction);

                auditEventRepository.record(AuditEvent.create(junction.getId(), "RESTART_RECOVERY",
                        "System restarted. Persisted queues restored. Controller status marked UNKNOWN until confirmation."));
            } finally {
                lock.unlock();
            }
        }
    }

    public List<Junction> getAllJunctions() {
        return junctionRepository.findAll();
    }

    public Optional<Junction> getJunction(String id) {
        return junctionRepository.findById(id);
    }

    public Junction createJunction(String id, String name) {
        ReentrantLock lock = getLockForJunction(id);
        lock.lock();
        try {
            if (junctionRepository.existsById(id)) {
                throw new IllegalArgumentException("Junction already exists: " + id);
            }
            Junction j = new Junction(id, name);
            Junction saved = junctionRepository.save(j);
            auditEventRepository.record(AuditEvent.create(id, "JUNCTION_CREATED", "Created junction " + id));
            return saved;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Process Sensor Event (VEHICLE_ARRIVED or VEHICLE_CLEARED) with Idempotency (Section 8, 10, 11).
     */
    public boolean processSensorEvent(String eventId, String junctionId, String directionStr,
                                      String eventTypeStr, String vehicleId, String vehicleTypeStr,
                                      long sequenceNo, Instant timestamp) {
        ReentrantLock lock = getLockForJunction(junctionId);
        lock.lock();
        try {
            // 1. Idempotency Check: Same event_id MUST NOT modify queue state more than once
            if (processedEventRepository.existsByEventId(eventId)) {
                log.info("[IDEMPOTENCY] Duplicate sensor event ignored: event_id={}", eventId);
                auditEventRepository.record(new AuditEvent(null, junctionId, "DUPLICATE_SENSOR_EVENT",
                        null, null, null, null, vehicleId,
                        "Duplicate event " + eventId + " submitted; ignored.", Instant.now()));
                return false; // Already processed
            }

            Junction junction = junctionRepository.findById(junctionId)
                    .orElseThrow(() -> new NoSuchElementException("Unknown junction: " + junctionId));

            Direction direction;
            try {
                direction = Direction.valueOf(directionStr.toUpperCase());
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid direction: " + directionStr);
            }

            VehicleEventType eventType;
            try {
                eventType = VehicleEventType.valueOf(eventTypeStr.toUpperCase());
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid event type: " + eventTypeStr);
            }

            VehicleType vehicleType = VehicleType.EMPLOYEE_VEHICLE;
            if (vehicleTypeStr != null && !vehicleTypeStr.isBlank()) {
                try {
                    vehicleType = VehicleType.valueOf(vehicleTypeStr.toUpperCase());
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid vehicle type: " + vehicleTypeStr);
                }
            }

            Instant eventTime = timestamp != null ? timestamp : Instant.now();
            SignalStateMap prevDesired = new SignalStateMap(junction.getDesiredSignals().asMap());

            if (eventType == VehicleEventType.VEHICLE_ARRIVED) {
                trafficEngine.processVehicleArrival(junction, vehicleId, vehicleType, direction, sequenceNo, eventTime);
                auditEventRepository.record(new AuditEvent(null, junctionId, "VEHICLE_ARRIVED", direction,
                        null, null, null, vehicleId,
                        String.format("Vehicle %s (%s) arrived from %s. Queue size: %d",
                                vehicleId, vehicleType, direction, junction.getQueue(direction).size()),
                        Instant.now()));

                if (vehicleType == VehicleType.EMERGENCY) {
                    auditEventRepository.record(new AuditEvent(null, junctionId, "EMERGENCY_DETECTED", direction,
                            null, null, null, vehicleId,
                            "EMERGENCY vehicle arrived at " + direction + ". Preemption initiated safely.", Instant.now()));
                }
            } else if (eventType == VehicleEventType.VEHICLE_CLEARED) {
                int sizeBefore = junction.getQueue(direction).size();
                Vehicle cleared = trafficEngine.processVehicleClearance(junction, vehicleId, direction, sequenceNo, eventTime);

                if (sizeBefore == 0 && cleared == null) {
                    // Documented policy: Unmatched clearance on empty queue
                    auditEventRepository.record(new AuditEvent(null, junctionId, "UNMATCHED_VEHICLE_CLEARED_EMPTY_QUEUE",
                            direction, null, null, null, vehicleId,
                            "Clearance event received for empty queue in direction " + direction + ". Queue unchanged.",
                            Instant.now()));
                } else {
                    auditEventRepository.record(new AuditEvent(null, junctionId, "VEHICLE_CLEARED", direction,
                            null, null, null, vehicleId,
                            String.format("Vehicle %s cleared from %s. Remaining queue: %d",
                                    vehicleId, direction, junction.getQueue(direction).size()),
                            Instant.now()));
                }
            }

            // Persist the event ID to guarantee idempotency across restarts
            processedEventRepository.recordProcessedEvent(eventId, junctionId, eventType.name(), vehicleId);

            // If a transition was triggered, issue physical commands
            dispatchCommandsIfDesiredChanged(junction, prevDesired);

            junction.incrementVersion();
            junctionRepository.save(junction);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Manual Control Command (Section 15):
     * - MANUAL_GREEN_REQUEST
     * - RETURN_TO_AUTOMATIC
     */
    public boolean processManualCommand(String junctionId, String commandStr, String directionStr) {
        ReentrantLock lock = getLockForJunction(junctionId);
        lock.lock();
        try {
            Junction junction = junctionRepository.findById(junctionId)
                    .orElseThrow(() -> new NoSuchElementException("Unknown junction: " + junctionId));

            SignalStateMap prevDesired = new SignalStateMap(junction.getDesiredSignals().asMap());
            Instant now = Instant.now();

            if ("MANUAL_GREEN_REQUEST".equalsIgnoreCase(commandStr)) {
                if (directionStr == null || directionStr.isBlank()) {
                    throw new IllegalArgumentException("Direction is required for MANUAL_GREEN_REQUEST");
                }
                Direction direction;
                try {
                    direction = Direction.valueOf(directionStr.toUpperCase());
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid direction: " + directionStr);
                }

                boolean started = trafficEngine.processManualGreenRequest(junction, direction, now);
                auditEventRepository.record(AuditEvent.create(junctionId, "MANUAL_OVERRIDE",
                        "Manual green requested for direction " + direction + ". Safe transition started: " + started));

                dispatchCommandsIfDesiredChanged(junction, prevDesired);
                junction.incrementVersion();
                junctionRepository.save(junction);
                return true;
            } else if ("RETURN_TO_AUTOMATIC".equalsIgnoreCase(commandStr)) {
                trafficEngine.processReturnToAutomatic(junction, now);
                auditEventRepository.record(AuditEvent.create(junctionId, "RETURN_TO_AUTOMATIC",
                        "Manual override cancelled. Reverted to AUTOMATIC mode."));

                dispatchCommandsIfDesiredChanged(junction, prevDesired);
                junction.incrementVersion();
                junctionRepository.save(junction);
                return true;
            } else {
                throw new IllegalArgumentException("Unknown command: " + commandStr);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Process Controller Acknowledgement (Section 17, 18).
     */
    public boolean handleControllerAck(String commandId, String status, SignalColor actualState, Instant timestamp) {
        Optional<TrafficCommand> cmdOpt = commandRepository.findById(commandId);
        if (cmdOpt.isEmpty()) {
            log.warn("[CONTROLLER ACK] Received ACK for unknown command_id: {}", commandId);
            return false;
        }

        TrafficCommand cmd = cmdOpt.get();
        ReentrantLock lock = getLockForJunction(cmd.getJunctionId());
        lock.lock();
        try {
            cmd.setStatus(CommandStatus.ACK);
            cmd.setAcknowledgedAt(timestamp != null ? timestamp : Instant.now());
            commandRepository.save(cmd);
            simulatorAdapter.removePendingCommand(commandId);

            Junction junction = junctionRepository.findById(cmd.getJunctionId()).orElse(null);
            if (junction == null) {
                return true;
            }

            if (cmd.getDirection() != null && actualState != null) {
                junction.getActualSignals().set(cmd.getDirection(), actualState);
            }

            boolean matches = true;
            for (Direction d : Direction.values()) {
                if (junction.getDesiredSignals().get(d) != junction.getActualSignals().get(d)) {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                junction.setControllerStatus(ControllerStatus.ONLINE);
            } else {
                junction.setControllerStatus(ControllerStatus.WARNING);
            }

            auditEventRepository.record(new AuditEvent(null, junction.getId(), "CONTROLLER_ACK",
                    cmd.getDirection(), null, actualState, commandId, null,
                    "Controller confirmed state " + actualState + " for direction " + cmd.getDirection(),
                    Instant.now()));

            junction.incrementVersion();
            junctionRepository.save(junction);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Process Device / Controller Status Events (Section 9, 14.6, 14.8):
     * e.g. SIGNAL_CONTROLLER reporting OFFLINE, ONLINE, DEGRADED.
     */
    public boolean handleDeviceStatus(String junctionId, String deviceType, String directionStr,
                                      String statusStr, Instant timestamp) {
        String targetJunctionId = (junctionId != null && !junctionId.isBlank()) ? junctionId : "A";
        ReentrantLock lock = getLockForJunction(targetJunctionId);
        lock.lock();
        try {
            Junction junction = junctionRepository.findById(targetJunctionId)
                    .orElseThrow(() -> new NoSuchElementException("Unknown junction: " + targetJunctionId));

            ControllerStatus controllerStatus;
            try {
                controllerStatus = ControllerStatus.valueOf(statusStr.toUpperCase());
            } catch (Exception e) {
                controllerStatus = ControllerStatus.WARNING;
            }

            junction.setControllerStatus(controllerStatus);
            if (controllerStatus == ControllerStatus.OFFLINE) {
                junction.setMode(OperatingMode.FAILURE);
            } else if (controllerStatus == ControllerStatus.ONLINE && junction.getMode() == OperatingMode.FAILURE) {
                junction.setMode(OperatingMode.AUTOMATIC);
            } else if (controllerStatus == ControllerStatus.DEGRADED) {
                junction.setMode(OperatingMode.DEGRADED);
            }

            Direction dir = null;
            if (directionStr != null && !directionStr.isBlank()) {
                try {
                    dir = Direction.valueOf(directionStr.toUpperCase());
                } catch (Exception ignored) {}
            }

            auditEventRepository.record(new AuditEvent(null, targetJunctionId, "DEVICE_STATUS_EVENT",
                    dir, null, null, null, null,
                    String.format("Device %s status updated to %s", deviceType != null ? deviceType : "SIGNAL_CONTROLLER", statusStr),
                    timestamp != null ? timestamp : Instant.now()));

            junction.incrementVersion();
            junctionRepository.save(junction);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Periodic Tick (invoked by Background Scheduler):
     * 1. Progresses active phase transitions (YELLOW -> ALL_RED -> GREEN).
     * 2. Checks pending controller commands for timeouts (> 3000ms).
     * 3. Evaluates automatic phase scheduling.
     */
    public void tickJunction(String junctionId) {
        ReentrantLock lock = getLockForJunction(junctionId);
        lock.lock();
        try {
            Junction junction = junctionRepository.findById(junctionId).orElse(null);
            if (junction == null) {
                return;
            }

            Instant now = Instant.now();
            SignalStateMap prevDesired = new SignalStateMap(junction.getDesiredSignals().asMap());

            // 1. Advance state machine transition if active
            if (junction.isTransitioning()) {
                boolean advanced = trafficEngine.getStateMachine().advanceTransitionStep(
                        junction, now, yellowDurationSeconds, allRedDurationSeconds);
                if (advanced) {
                    log.info("[STATE MACHINE TICK] Junction {} advanced to step: {}, desired: {}",
                            junctionId, junction.getTransitionState(), junction.getDesiredSignals());
                    auditEventRepository.record(AuditEvent.create(junctionId, "SIGNAL_TRANSITION_STEP",
                            "Transition advanced to " + junction.getTransitionState() + " with signals: " + junction.getDesiredSignals()));
                    dispatchCommandsIfDesiredChanged(junction, prevDesired);
                    junction.incrementVersion();
                    junctionRepository.save(junction);
                }
            } else if (junction.getMode() == OperatingMode.AUTOMATIC) {
                // 2. Evaluate scheduling in AUTOMATIC mode
                TrafficPhase optimal = trafficEngine.getSchedulingAlgorithm().selectOptimalPhase(junction, now);
                if (optimal != junction.getCurrentPhase()) {
                    boolean started = trafficEngine.getStateMachine().initiatePhaseTransition(junction, optimal, now);
                    if (started) {
                        auditEventRepository.record(AuditEvent.create(junctionId, "PHASE_CHANGE_SCHEDULED",
                                "Scheduled switch to " + optimal + ". Safe transition started."));
                        dispatchCommandsIfDesiredChanged(junction, prevDesired);
                        junction.incrementVersion();
                        junctionRepository.save(junction);
                    }
                }
            }

            // 3. Check pending commands for ACK timeout
            checkPendingCommandTimeouts(junction, now);

        } finally {
            lock.unlock();
        }
    }

    private void checkPendingCommandTimeouts(Junction junction, Instant now) {
        List<TrafficCommand> pendingList = commandRepository.findPendingByJunctionId(junction.getId());
        for (TrafficCommand cmd : pendingList) {
            long elapsed = Duration.between(cmd.getCreatedAt(), now).toMillis();
            if (elapsed >= ackTimeoutMillis) {
                if (cmd.getRetryCount() < maxCommandRetries) {
                    cmd.incrementRetryCount();
                    log.warn("[CONTROLLER RETRY] Retrying command {} (attempt {}/{})",
                            cmd.getCommandId(), cmd.getRetryCount(), maxCommandRetries);
                    commandRepository.save(cmd);
                    controllerPort.sendSignalCommand(cmd);
                } else {
                    // Maximum retries exhausted: Controller Failure!
                    log.error("[CONTROLLER TIMEOUT] Command {} timed out after {} retries! Marking controller OFFLINE.",
                            cmd.getCommandId(), maxCommandRetries);
                    cmd.setStatus(CommandStatus.TIMEOUT);
                    commandRepository.save(cmd);
                    simulatorAdapter.removePendingCommand(cmd.getCommandId());

                    junction.setControllerStatus(ControllerStatus.OFFLINE);
                    junction.setMode(OperatingMode.FAILURE);

                    auditEventRepository.record(new AuditEvent(null, junction.getId(), "CONTROLLER_TIMEOUT",
                            cmd.getDirection(), null, null, cmd.getCommandId(), null,
                            "Controller failed to acknowledge command " + cmd.getCommandId() + ". Marked OFFLINE.",
                            Instant.now()));

                    junctionRepository.save(junction);
                }
            }
        }
    }

    private void dispatchCommandsIfDesiredChanged(Junction junction, SignalStateMap prevDesired) {
        SignalStateMap currentDesired = junction.getDesiredSignals();
        for (Direction d : Direction.values()) {
            SignalColor prevColor = prevDesired.get(d);
            SignalColor newColor = currentDesired.get(d);
            if (prevColor != newColor) {
                String cmdId = "cmd-" + UUIDUtil.randomId().substring(0, 8);
                TrafficCommand cmd = new TrafficCommand(cmdId, junction.getId(), d, newColor);
                commandRepository.save(cmd);

                auditEventRepository.record(AuditEvent.signalChanged(junction.getId(), d, prevColor, newColor, cmdId));
                controllerPort.sendSignalCommand(cmd);
            }
        }
    }

    public List<AuditEvent> getRecentHistory(String junctionId, int limit) {
        return auditEventRepository.findRecentByJunctionId(junctionId, limit);
    }

    public void setYellowDurationSeconds(long yellowDurationSeconds) {
        this.yellowDurationSeconds = yellowDurationSeconds;
    }

    public void setAllRedDurationSeconds(long allRedDurationSeconds) {
        this.allRedDurationSeconds = allRedDurationSeconds;
    }

    public void setAckTimeoutMillis(long ackTimeoutMillis) {
        this.ackTimeoutMillis = ackTimeoutMillis;
    }

    public void setMaxCommandRetries(int maxCommandRetries) {
        this.maxCommandRetries = maxCommandRetries;
    }

    public TrafficDomainEngine getTrafficEngine() {
        return trafficEngine;
    }
}
