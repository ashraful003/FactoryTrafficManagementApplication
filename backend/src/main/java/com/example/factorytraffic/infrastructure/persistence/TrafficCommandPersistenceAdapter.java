package com.example.factorytraffic.infrastructure.persistence;

import com.example.factorytraffic.domain.model.CommandStatus;
import com.example.factorytraffic.domain.model.Direction;
import com.example.factorytraffic.domain.model.SignalColor;
import com.example.factorytraffic.domain.model.TrafficCommand;
import com.example.factorytraffic.domain.port.TrafficCommandRepositoryPort;
import com.example.factorytraffic.entity.TrafficCommandEntity;
import com.example.factorytraffic.repository.TrafficCommandJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class TrafficCommandPersistenceAdapter implements TrafficCommandRepositoryPort {

    private final TrafficCommandJpaRepository commandJpaRepository;

    public TrafficCommandPersistenceAdapter(TrafficCommandJpaRepository commandJpaRepository) {
        this.commandJpaRepository = commandJpaRepository;
    }

    @Override
    @Transactional
    public void save(TrafficCommand command) {
        if (command == null) return;
        TrafficCommandEntity entity = new TrafficCommandEntity(
                command.getCommandId(),
                command.getJunctionId(),
                command.getDirection() != null ? command.getDirection().name() : null,
                command.getRequestedState().name(),
                command.getStatus().name(),
                command.getCreatedAt(),
                command.getAcknowledgedAt(),
                command.getRetryCount()
        );
        commandJpaRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TrafficCommand> findById(String commandId) {
        return commandJpaRepository.findById(commandId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrafficCommand> findPendingByJunctionId(String junctionId) {
        return commandJpaRepository.findByJunctionIdAndStatus(junctionId, CommandStatus.PENDING.name())
                .stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrafficCommand> findAllPending() {
        return commandJpaRepository.findByStatus(CommandStatus.PENDING.name())
                .stream().map(this::toDomain).toList();
    }

    private TrafficCommand toDomain(TrafficCommandEntity e) {
        Direction dir = e.getDirection() != null ? Direction.valueOf(e.getDirection()) : null;
        SignalColor req = SignalColor.valueOf(e.getRequestedState());
        CommandStatus status = CommandStatus.valueOf(e.getStatus());
        return new TrafficCommand(
                e.getCommandId(),
                e.getJunctionId(),
                dir,
                req,
                status,
                e.getCreatedAt(),
                e.getAcknowledgedAt(),
                e.getRetryCount()
        );
    }
}
