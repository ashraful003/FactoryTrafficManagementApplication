package com.example.factorytraffic.application;

import com.example.factorytraffic.application.service.TrafficManagementApplicationService;
import com.example.factorytraffic.domain.model.*;
import com.example.factorytraffic.domain.port.*;
import com.example.factorytraffic.infrastructure.adapter.RestControllerSimulatorAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("Application Service Tests: Idempotency, Controller ACK, Timeouts, and Recovery")
class TrafficManagementApplicationServiceTest {

    private JunctionRepositoryPort junctionRepository;
    private ProcessedEventRepositoryPort processedEventRepository;
    private AuditEventRepositoryPort auditEventRepository;
    private TrafficCommandRepositoryPort commandRepository;
    private RestControllerSimulatorAdapter simulatorAdapter;
    private TrafficManagementApplicationService service;

    private final Map<String, Junction> junctionStore = new ConcurrentHashMap<>();
    private final Set<String> processedEventStore = ConcurrentHashMap.newKeySet();
    private final Map<String, TrafficCommand> commandStore = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        junctionStore.clear();
        processedEventStore.clear();
        commandStore.clear();

        junctionRepository = mock(JunctionRepositoryPort.class);
        when(junctionRepository.findById(anyString())).thenAnswer(inv -> Optional.ofNullable(junctionStore.get(inv.getArgument(0))));
        when(junctionRepository.findAll()).thenAnswer(inv -> new ArrayList<>(junctionStore.values()));
        when(junctionRepository.save(any(Junction.class))).thenAnswer(inv -> {
            Junction j = inv.getArgument(0);
            junctionStore.put(j.getId(), j);
            return j;
        });

        processedEventRepository = mock(ProcessedEventRepositoryPort.class);
        when(processedEventRepository.existsByEventId(anyString()))
                .thenAnswer(inv -> processedEventStore.contains(inv.getArgument(0)));
        doAnswer(inv -> {
            processedEventStore.add(inv.getArgument(0));
            return null;
        }).when(processedEventRepository).recordProcessedEvent(anyString(), anyString(), anyString(), any());

        auditEventRepository = mock(AuditEventRepositoryPort.class);
        commandRepository = mock(TrafficCommandRepositoryPort.class);
        doAnswer(inv -> {
            TrafficCommand cmd = inv.getArgument(0);
            commandStore.put(cmd.getCommandId(), cmd);
            return null;
        }).when(commandRepository).save(any(TrafficCommand.class));
        when(commandRepository.findById(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(commandStore.get(inv.getArgument(0))));
        when(commandRepository.findPendingByJunctionId(anyString()))
                .thenAnswer(inv -> commandStore.values().stream()
                        .filter(c -> c.getJunctionId().equals(inv.getArgument(0)) && c.isPending())
                        .toList());

        simulatorAdapter = new RestControllerSimulatorAdapter();
        simulatorAdapter.setAutoAckEnabled(false);

        service = new TrafficManagementApplicationService(
                junctionRepository,
                processedEventRepository,
                auditEventRepository,
                commandRepository,
                simulatorAdapter,
                simulatorAdapter
        );
        service.setAckTimeoutMillis(100);
        service.setMaxCommandRetries(1);

        Junction j = new Junction("A", "Test Junction");
        junctionStore.put("A", j);
    }

    @Test
    @DisplayName("Idempotency: Processing identical eventId twice leaves queue at 1")
    void idempotency_duplicateEventHandledOnce() {
        String eventId = "evt-uniq-1";
        boolean first = service.processSensorEvent(eventId, "A", "NORTH", "VEHICLE_ARRIVED",
                "VH-1", "FORKLIFT", 1L, Instant.now());
        assertTrue(first);
        assertEquals(1, junctionStore.get("A").getQueue(Direction.NORTH).size());

        // Submit same eventId again
        boolean second = service.processSensorEvent(eventId, "A", "NORTH", "VEHICLE_ARRIVED",
                "VH-1", "FORKLIFT", 1L, Instant.now());
        assertFalse(second, "Second submission must return false and not reprocess");
        assertEquals(1, junctionStore.get("A").getQueue(Direction.NORTH).size(),
                "Queue size must remain exactly 1");
    }

    @Test
    @DisplayName("Unmatched clearance on empty queue keeps queue at 0")
    void unmatchedClearance_neverGoesNegative() {
        boolean res = service.processSensorEvent("evt-clr-empty", "A", "SOUTH", "VEHICLE_CLEARED",
                "VH-UNKNOWN", "EMPLOYEE_VEHICLE", 1L, Instant.now());
        assertTrue(res);
        assertEquals(0, junctionStore.get("A").getQueue(Direction.SOUTH).size());
    }

    @Test
    @DisplayName("Controller ACK correlates command and updates actual physical state")
    void controllerAck_updatesActualState() {
        TrafficCommand cmd = new TrafficCommand("cmd-55", "A", Direction.NORTH, SignalColor.GREEN);
        commandStore.put("cmd-55", cmd);

        boolean acked = service.handleControllerAck("cmd-55", "ACK", SignalColor.GREEN, Instant.now());
        assertTrue(acked);
        assertEquals(CommandStatus.ACK, cmd.getStatus());
        assertEquals(SignalColor.GREEN, junctionStore.get("A").getActualSignals().get(Direction.NORTH));
    }

    @Test
    @DisplayName("Controller timeout triggers OFFLINE status and FAILURE mode")
    void controllerTimeout_marksOffline() throws Exception {
        TrafficCommand cmd = new TrafficCommand("cmd-timeout-1", "A", Direction.EAST, SignalColor.GREEN);
        // Created 500ms ago, exceeding 100ms timeout
        TrafficCommand oldCmd = new TrafficCommand("cmd-timeout-1", "A", Direction.EAST, SignalColor.GREEN,
                CommandStatus.PENDING, Instant.now().minusMillis(500), null, 1); // retryCount already 1 (maxRetries=1)
        commandStore.put(oldCmd.getCommandId(), oldCmd);

        service.tickJunction("A");

        assertEquals(CommandStatus.TIMEOUT, oldCmd.getStatus());
        assertEquals(ControllerStatus.OFFLINE, junctionStore.get("A").getControllerStatus());
        assertEquals(OperatingMode.FAILURE, junctionStore.get("A").getMode());
    }

    @Test
    @DisplayName("Startup recovery reconciles interrupted transitions into ALL_RED")
    void startupRecovery_reconcilesInterruptedTransition() {
        Junction j = junctionStore.get("A");
        j.setTransitionState(TransitionState.YELLOW_CLEARANCE);
        j.setTargetPhase(TrafficPhase.EAST_WEST);

        service.recoverOrInitializeState();

        assertEquals(TrafficPhase.ALL_RED, j.getCurrentPhase());
        assertEquals(TransitionState.STABLE, j.getTransitionState());
        assertEquals(ControllerStatus.UNKNOWN, j.getControllerStatus());
    }

    @Test
    @DisplayName("Device status event OFFLINE sets FAILURE mode and OFFLINE controller status")
    void deviceStatus_offlineSetsFailureMode() {
        boolean updated = service.handleDeviceStatus("A", "SIGNAL_CONTROLLER", "SOUTH", "OFFLINE", Instant.now());
        assertTrue(updated);
        assertEquals(ControllerStatus.OFFLINE, junctionStore.get("A").getControllerStatus());
        assertEquals(OperatingMode.FAILURE, junctionStore.get("A").getMode());

        // Reconnect ONLINE
        boolean reconnected = service.handleDeviceStatus("A", "SIGNAL_CONTROLLER", "SOUTH", "ONLINE", Instant.now());
        assertTrue(reconnected);
        assertEquals(ControllerStatus.ONLINE, junctionStore.get("A").getControllerStatus());
        assertEquals(OperatingMode.AUTOMATIC, junctionStore.get("A").getMode());
    }
}
