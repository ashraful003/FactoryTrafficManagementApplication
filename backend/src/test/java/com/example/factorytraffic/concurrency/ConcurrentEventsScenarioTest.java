package com.example.factorytraffic.concurrency;

import com.example.factorytraffic.application.service.TrafficManagementApplicationService;
import com.example.factorytraffic.domain.model.*;
import com.example.factorytraffic.domain.port.*;
import com.example.factorytraffic.infrastructure.adapter.RestControllerSimulatorAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Concurrency Requirement (Section 37 & Scenario 9): Burst Arrival Test")
class ConcurrentEventsScenarioTest {

    private JunctionRepositoryPort junctionRepository;
    private ProcessedEventRepositoryPort processedEventRepository;
    private AuditEventRepositoryPort auditEventRepository;
    private TrafficCommandRepositoryPort commandRepository;
    private RestControllerSimulatorAdapter simulatorAdapter;
    private TrafficManagementApplicationService service;

    private final Map<String, Junction> junctionStore = new ConcurrentHashMap<>();
    private final Set<String> processedEventStore = ConcurrentHashMap.newKeySet();
    private final Map<String, TrafficCommand> commandStore = new ConcurrentHashMap<>();
    private final List<AuditEvent> auditStore = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() {
        junctionStore.clear();
        processedEventStore.clear();
        commandStore.clear();
        auditStore.clear();

        junctionRepository = mock(JunctionRepositoryPort.class);
        when(junctionRepository.findById("A")).thenAnswer(inv -> Optional.ofNullable(junctionStore.get("A")));
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
        doAnswer(inv -> {
            auditStore.add(inv.getArgument(0));
            return null;
        }).when(auditEventRepository).record(any(AuditEvent.class));

        commandRepository = mock(TrafficCommandRepositoryPort.class);
        doAnswer(inv -> {
            TrafficCommand cmd = inv.getArgument(0);
            commandStore.put(cmd.getCommandId(), cmd);
            return null;
        }).when(commandRepository).save(any(TrafficCommand.class));
        when(commandRepository.findById(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(commandStore.get(inv.getArgument(0))));

        simulatorAdapter = new RestControllerSimulatorAdapter();
        simulatorAdapter.setAutoAckEnabled(false); // manual control for exact test

        service = new TrafficManagementApplicationService(
                junctionRepository,
                processedEventRepository,
                auditEventRepository,
                commandRepository,
                simulatorAdapter,
                simulatorAdapter
        );

        // Initialize Junction A
        Junction junctionA = new Junction("A", "Junction A");
        junctionStore.put("A", junctionA);
    }

    @Test
    @DisplayName("Verify Scenario 9: 5 simultaneous concurrent operations executed at T=0, 4, 8, 12, 17 ms")
    void scenario9_concurrentBurstSequence() throws Exception {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
        String emergencyEventId = "evt-emg-burst-1";

        // Pre-create command for T=17ms ACK test
        TrafficCommand preCommand = new TrafficCommand("cmd-test-17", "A", Direction.NORTH, SignalColor.YELLOW);
        commandStore.put(preCommand.getCommandId(), preCommand);

        // T = 0 ms: NORTH truck arrives
        Future<?> f1 = executor.schedule(() -> {
            service.processSensorEvent("evt-truck-1", "A", "NORTH",
                    "VEHICLE_ARRIVED", "VH-TRK-01", "TRUCK", 101L, Instant.now());
        }, 0, TimeUnit.MILLISECONDS);

        // T = 4 ms: EAST emergency arrives
        Future<?> f2 = executor.schedule(() -> {
            service.processSensorEvent(emergencyEventId, "A", "EAST",
                    "VEHICLE_ARRIVED", "VH-EMG-99", "EMERGENCY", 102L, Instant.now());
        }, 4, TimeUnit.MILLISECONDS);

        // T = 8 ms: Administrator requests WEST manual control
        Future<?> f3 = executor.schedule(() -> {
            try {
                service.processManualCommand("A", "MANUAL_GREEN_REQUEST", "WEST");
            } catch (Exception ignored) {}
        }, 8, TimeUnit.MILLISECONDS);

        // T = 12 ms: Duplicate EAST emergency arrives
        Future<?> f4 = executor.schedule(() -> {
            service.processSensorEvent(emergencyEventId, "A", "EAST",
                    "VEHICLE_ARRIVED", "VH-EMG-99", "EMERGENCY", 102L, Instant.now());
        }, 12, TimeUnit.MILLISECONDS);

        // T = 17 ms: Controller ACK arrives
        Future<?> f5 = executor.schedule(() -> {
            service.handleControllerAck(preCommand.getCommandId(), "ACK", SignalColor.YELLOW, Instant.now());
        }, 17, TimeUnit.MILLISECONDS);

        f1.get(2, TimeUnit.SECONDS);
        f2.get(2, TimeUnit.SECONDS);
        f3.get(2, TimeUnit.SECONDS);
        f4.get(2, TimeUnit.SECONDS);
        f5.get(2, TimeUnit.SECONDS);
        executor.shutdown();

        Junction junction = junctionStore.get("A");
        assertNotNull(junction);

        // 1. Remain internally consistent: Rule 1 invariant maintained
        assertFalse(junction.getDesiredSignals().hasConflictingGreens(), "Conflicting greens must never be GREEN!");
        assertTrue(junction.isSafe());

        // 2. Deduplicate the repeated emergency event: East queue must have 1 vehicle, NOT 2!
        assertEquals(1, junction.getQueue(Direction.EAST).size(), "Duplicate emergency event must not double-count!");

        // 3. North queue has the truck
        assertEquals(1, junction.getQueue(Direction.NORTH).size());

        // 4. Respect emergency priority: Junction must be in EMERGENCY mode (Emergency overrides manual command)
        assertEquals(OperatingMode.EMERGENCY, junction.getMode(), "Emergency has highest priority and overrides manual request");
        assertEquals("VH-EMG-99", junction.getActiveEmergencyVehicleId());
        assertEquals(Direction.EAST, junction.getEmergencyDirection());

        // 5. Correlate controller acknowledgement
        assertEquals(CommandStatus.ACK, preCommand.getStatus());

        // 6. Preserve audit history: Contains deduplication and arrivals
        assertFalse(auditStore.isEmpty());
        boolean hasDuplicateAudit = auditStore.stream()
                .anyMatch(a -> "DUPLICATE_SENSOR_EVENT".equals(a.getEventType()));
        assertTrue(hasDuplicateAudit, "Audit log must contain DUPLICATE_SENSOR_EVENT");
    }
}
