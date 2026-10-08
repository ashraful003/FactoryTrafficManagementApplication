package com.example.factorytraffic.infrastructure.persistence;

import com.example.factorytraffic.domain.model.*;
import com.example.factorytraffic.domain.port.JunctionRepositoryPort;
import com.example.factorytraffic.entity.JunctionEntity;
import com.example.factorytraffic.entity.QueueVehicleEntity;
import com.example.factorytraffic.repository.JunctionJpaRepository;
import com.example.factorytraffic.repository.QueueVehicleJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
public class JunctionPersistenceAdapter implements JunctionRepositoryPort {

    private final JunctionJpaRepository junctionJpaRepository;
    private final QueueVehicleJpaRepository queueVehicleJpaRepository;

    public JunctionPersistenceAdapter(JunctionJpaRepository junctionJpaRepository,
                                    QueueVehicleJpaRepository queueVehicleJpaRepository) {
        this.junctionJpaRepository = junctionJpaRepository;
        this.queueVehicleJpaRepository = queueVehicleJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Junction> findById(String id) {
        return junctionJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Junction> findAll() {
        return junctionJpaRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public Junction save(Junction junction) {
        Objects.requireNonNull(junction, "junction must not be null");

        JunctionEntity entity = junctionJpaRepository.findById(junction.getId())
                .orElseGet(() -> {
                    JunctionEntity newEntity = new JunctionEntity();
                    newEntity.setId(junction.getId());
                    return newEntity;
                });

        copyDomainToEntity(junction, entity);
        JunctionEntity saved = junctionJpaRepository.save(entity);

        // Sync queue vehicles
        queueVehicleJpaRepository.deleteByJunctionId(junction.getId());
        List<QueueVehicleEntity> queueEntities = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            TrafficQueue queue = junction.getQueue(direction);
            if (queue != null) {
                List<Vehicle> vehicles = queue.getVehicles();
                for (int i = 0; i < vehicles.size(); i++) {
                    Vehicle v = vehicles.get(i);
                    queueEntities.add(new QueueVehicleEntity(
                            junction.getId(),
                            direction.name(),
                            v.getVehicleId(),
                            v.getVehicleType().name(),
                            v.getSequenceNo(),
                            v.getArrivedAt(),
                            i
                    ));
                }
            }
        }

        if (!queueEntities.isEmpty()) {
            queueVehicleJpaRepository.saveAll(queueEntities);
        }

        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        return junctionJpaRepository.existsById(id);
    }

    private Junction toDomain(JunctionEntity entity) {
        SignalStateMap desired = new SignalStateMap();
        desired.set(Direction.NORTH, SignalColor.valueOf(entity.getDesiredNorth()));
        desired.set(Direction.SOUTH, SignalColor.valueOf(entity.getDesiredSouth()));
        desired.set(Direction.EAST, SignalColor.valueOf(entity.getDesiredEast()));
        desired.set(Direction.WEST, SignalColor.valueOf(entity.getDesiredWest()));

        SignalStateMap actual = new SignalStateMap();
        actual.set(Direction.NORTH, SignalColor.valueOf(entity.getActualNorth()));
        actual.set(Direction.SOUTH, SignalColor.valueOf(entity.getActualSouth()));
        actual.set(Direction.EAST, SignalColor.valueOf(entity.getActualEast()));
        actual.set(Direction.WEST, SignalColor.valueOf(entity.getActualWest()));

        Map<Direction, TrafficQueue> queuesMap = new EnumMap<>(Direction.class);
        for (Direction d : Direction.values()) {
            queuesMap.put(d, new TrafficQueue(d));
        }

        // Load queued vehicles
        List<QueueVehicleEntity> qVehicles = queueVehicleJpaRepository
                .findByJunctionIdOrderByQueueOrderAsc(entity.getId());
        for (QueueVehicleEntity qv : qVehicles) {
            try {
                Direction d = Direction.valueOf(qv.getDirection());
                VehicleType vt = VehicleType.valueOf(qv.getVehicleType());
                Vehicle vehicle = new Vehicle(qv.getVehicleId(), vt, d, qv.getSequenceNo(), qv.getArrivedAt());
                queuesMap.get(d).enqueue(vehicle);
            } catch (Exception ignored) {}
        }

        OperatingMode mode = OperatingMode.valueOf(entity.getMode());
        TrafficPhase currentPhase = TrafficPhase.valueOf(entity.getCurrentPhase());
        TrafficPhase targetPhase = entity.getTargetPhase() != null ? TrafficPhase.valueOf(entity.getTargetPhase()) : null;
        TransitionState transitionState = TransitionState.valueOf(entity.getTransitionState());
        ControllerStatus controllerStatus = ControllerStatus.valueOf(entity.getControllerStatus());
        Direction emergencyDir = entity.getEmergencyDirection() != null ? Direction.valueOf(entity.getEmergencyDirection()) : null;
        Direction manualDir = entity.getManualRequestedDirection() != null ? Direction.valueOf(entity.getManualRequestedDirection()) : null;

        return new Junction(
                entity.getId(),
                entity.getName(),
                mode,
                currentPhase,
                targetPhase,
                transitionState,
                entity.getPhaseStartTime(),
                entity.getTransitionStepStartTime(),
                desired,
                actual,
                queuesMap,
                controllerStatus,
                entity.getActiveEmergencyVehicleId(),
                emergencyDir,
                manualDir,
                entity.getManualModeExpiryTime(),
                null, // pending command loaded separately if needed
                entity.getVersion() != null ? entity.getVersion() : 1L
        );
    }

    private void copyDomainToEntity(Junction domain, JunctionEntity entity) {
        entity.setName(domain.getName());
        entity.setMode(domain.getMode().name());
        entity.setCurrentPhase(domain.getCurrentPhase().name());
        entity.setTargetPhase(domain.getTargetPhase() != null ? domain.getTargetPhase().name() : null);
        entity.setTransitionState(domain.getTransitionState().name());
        entity.setPhaseStartTime(domain.getPhaseStartTime());
        entity.setTransitionStepStartTime(domain.getTransitionStepStartTime());

        SignalStateMap desired = domain.getDesiredSignals();
        entity.setDesiredNorth(desired.get(Direction.NORTH).name());
        entity.setDesiredSouth(desired.get(Direction.SOUTH).name());
        entity.setDesiredEast(desired.get(Direction.EAST).name());
        entity.setDesiredWest(desired.get(Direction.WEST).name());

        SignalStateMap actual = domain.getActualSignals();
        entity.setActualNorth(actual.get(Direction.NORTH).name());
        entity.setActualSouth(actual.get(Direction.SOUTH).name());
        entity.setActualEast(actual.get(Direction.EAST).name());
        entity.setActualWest(actual.get(Direction.WEST).name());

        entity.setControllerStatus(domain.getControllerStatus().name());
        entity.setActiveEmergencyVehicleId(domain.getActiveEmergencyVehicleId());
        entity.setEmergencyDirection(domain.getEmergencyDirection() != null ? domain.getEmergencyDirection().name() : null);
        entity.setManualRequestedDirection(domain.getManualRequestedDirection() != null ? domain.getManualRequestedDirection().name() : null);
        entity.setManualModeExpiryTime(domain.getManualModeExpiryTime());
    }
}
