package com.example.factorytraffic.controller;

import com.example.factorytraffic.application.service.TrafficManagementApplicationService;
import com.example.factorytraffic.domain.model.*;
import com.example.factorytraffic.domain.port.JunctionRepositoryPort;
import com.example.factorytraffic.dto.JunctionStatusResponseDto;
import com.example.factorytraffic.infrastructure.adapter.RestControllerSimulatorAdapter;
import com.example.factorytraffic.mapper.JunctionDtoMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@RestController
@RequestMapping("/api/simulation")
@CrossOrigin(origins = "*")
public class SimulationController {

    private final TrafficManagementApplicationService applicationService;
    private final RestControllerSimulatorAdapter simulatorAdapter;
    private final JunctionRepositoryPort junctionRepository;
    private final JunctionDtoMapper mapper;

    public SimulationController(TrafficManagementApplicationService applicationService,
                                RestControllerSimulatorAdapter simulatorAdapter,
                                JunctionRepositoryPort junctionRepository,
                                JunctionDtoMapper mapper) {
        this.applicationService = applicationService;
        this.simulatorAdapter = simulatorAdapter;
        this.junctionRepository = junctionRepository;
        this.mapper = mapper;
    }

    @PostMapping("/auto-ack")
    public ResponseEntity<Map<String, Object>> setAutoAck(@RequestParam boolean enabled) {
        simulatorAdapter.setAutoAckEnabled(enabled);
        return ResponseEntity.ok(Map.of(
                "auto_ack_enabled", simulatorAdapter.isAutoAckEnabled(),
                "message", "Controller auto-ACK " + (enabled ? "enabled" : "disabled")
        ));
    }

    @GetMapping("/auto-ack")
    public ResponseEntity<Map<String, Object>> getAutoAck() {
        return ResponseEntity.ok(Map.of(
                "auto_ack_enabled", simulatorAdapter.isAutoAckEnabled()
        ));
    }

    @PostMapping("/reset")
    public ResponseEntity<JunctionStatusResponseDto> resetSimulation(@RequestParam(defaultValue = "A") String junctionId) {
        Junction junction = junctionRepository.findById(junctionId)
                .orElseGet(() -> new Junction(junctionId, "Factory Gate Junction " + junctionId));

        junction.setMode(OperatingMode.AUTOMATIC);
        junction.setCurrentPhase(TrafficPhase.NORTH_SOUTH);
        junction.setTargetPhase(null);
        junction.setTransitionState(TransitionState.STABLE);
        junction.setPhaseStartTime(Instant.now());
        junction.setTransitionStepStartTime(Instant.now());
        junction.setDesiredSignals(SignalStateMap.forPhase(TrafficPhase.NORTH_SOUTH));
        junction.setActualSignals(SignalStateMap.forPhase(TrafficPhase.NORTH_SOUTH));
        junction.setControllerStatus(ControllerStatus.ONLINE);
        junction.setActiveEmergencyVehicleId(null);
        junction.setEmergencyDirection(null);
        junction.setManualRequestedDirection(null);
        junction.setManualModeExpiryTime(null);

        for (Direction d : Direction.values()) {
            junction.getQueue(d).clear();
        }

        junctionRepository.save(junction);
        simulatorAdapter.clear();

        return ResponseEntity.ok(mapper.toStatusResponseDto(junction));
    }

    /**
     * Executes the exact Scenario 9 concurrency requirement (Section 37):
     * T = 0 ms    NORTH truck arrives
     * T = 4 ms    EAST emergency arrives
     * T = 8 ms    Administrator requests WEST manual control
     * T = 12 ms   Duplicate EAST emergency arrives
     * T = 17 ms   Controller ACK arrives
     */
    @PostMapping("/concurrency-test")
    public ResponseEntity<Map<String, Object>> runConcurrencyTest(
            @RequestParam(defaultValue = "A") String junctionId) throws Exception {

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
        List<String> auditLogs = new CopyOnWriteArrayList<>();
        String emergencyEventId = "sim-emg-" + UUID.randomUUID();

        // Save current auto-ack setting
        boolean prevAutoAck = simulatorAdapter.isAutoAckEnabled();
        simulatorAdapter.setAutoAckEnabled(true);

        try {
            // Task 1: T = 0 ms NORTH truck arrives
            Future<?> f1 = executor.schedule(() -> {
                try {
                    applicationService.processSensorEvent(
                            "sim-truck-" + UUID.randomUUID(), junctionId, "NORTH",
                            "VEHICLE_ARRIVED", "VH-TRUCK-01", "TRUCK", 101L, Instant.now()
                    );
                    auditLogs.add("T=0ms: NORTH truck arrived");
                } catch (Exception e) {
                    auditLogs.add("T=0ms Error: " + e.getMessage());
                }
            }, 0, TimeUnit.MILLISECONDS);

            // Task 2: T = 4 ms EAST emergency arrives
            Future<?> f2 = executor.schedule(() -> {
                try {
                    applicationService.processSensorEvent(
                            emergencyEventId, junctionId, "EAST",
                            "VEHICLE_ARRIVED", "VH-EMG-99", "EMERGENCY", 102L, Instant.now()
                    );
                    auditLogs.add("T=4ms: EAST emergency arrived");
                } catch (Exception e) {
                    auditLogs.add("T=4ms Error: " + e.getMessage());
                }
            }, 4, TimeUnit.MILLISECONDS);

            // Task 3: T = 8 ms Administrator requests WEST manual control
            Future<?> f3 = executor.schedule(() -> {
                try {
                    applicationService.processManualCommand(junctionId, "MANUAL_GREEN_REQUEST", "WEST");
                    auditLogs.add("T=8ms: Admin requested WEST manual control");
                } catch (Exception e) {
                    auditLogs.add("T=8ms Error: " + e.getMessage());
                }
            }, 8, TimeUnit.MILLISECONDS);

            // Task 4: T = 12 ms Duplicate EAST emergency arrives (must be deduplicated!)
            Future<?> f4 = executor.schedule(() -> {
                try {
                    applicationService.processSensorEvent(
                            emergencyEventId, junctionId, "EAST",
                            "VEHICLE_ARRIVED", "VH-EMG-99", "EMERGENCY", 102L, Instant.now()
                    );
                    auditLogs.add("T=12ms: Duplicate EAST emergency arrived (deduplicated)");
                } catch (Exception e) {
                    auditLogs.add("T=12ms Error: " + e.getMessage());
                }
            }, 12, TimeUnit.MILLISECONDS);

            // Task 5: T = 17 ms Controller ACK arrives
            Future<?> f5 = executor.schedule(() -> {
                try {
                    // ACK any pending command
                    List<TrafficCommand> sent = simulatorAdapter.getSentCommands();
                    if (!sent.isEmpty()) {
                        TrafficCommand last = sent.get(sent.size() - 1);
                        applicationService.handleControllerAck(
                                last.getCommandId(), "ACK", last.getRequestedState(), Instant.now()
                        );
                        auditLogs.add("T=17ms: Controller ACK processed for " + last.getCommandId());
                    } else {
                        auditLogs.add("T=17ms: Controller ACK evaluated (no commands pending)");
                    }
                } catch (Exception e) {
                    auditLogs.add("T=17ms Error: " + e.getMessage());
                }
            }, 17, TimeUnit.MILLISECONDS);

            // Wait for completion
            f1.get(2, TimeUnit.SECONDS);
            f2.get(2, TimeUnit.SECONDS);
            f3.get(2, TimeUnit.SECONDS);
            f4.get(2, TimeUnit.SECONDS);
            f5.get(2, TimeUnit.SECONDS);

        } finally {
            executor.shutdown();
            simulatorAdapter.setAutoAckEnabled(prevAutoAck);
        }

        Junction junction = junctionRepository.findById(junctionId).orElseThrow();
        boolean isSafe = junction.isSafe();
        int eastQueue = junction.getQueue(Direction.EAST).size();

        return ResponseEntity.ok(Map.of(
                "scenario", "Scenario 9 - Concurrent Events Burst",
                "execution_log", auditLogs,
                "is_safe_invariant_maintained", isSafe,
                "no_conflicting_greens", !junction.getDesiredSignals().hasConflictingGreens(),
                "east_emergency_deduplicated_queue_size", eastQueue, // must be 1, not 2
                "current_mode", junction.getMode().name(),
                "status", mapper.toStatusResponseDto(junction)
        ));
    }
}
