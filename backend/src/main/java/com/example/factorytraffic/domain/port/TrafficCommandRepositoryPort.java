package com.example.factorytraffic.domain.port;

import com.example.factorytraffic.domain.model.TrafficCommand;

import java.util.List;
import java.util.Optional;

public interface TrafficCommandRepositoryPort {
    void save(TrafficCommand command);
    Optional<TrafficCommand> findById(String commandId);
    List<TrafficCommand> findPendingByJunctionId(String junctionId);
    List<TrafficCommand> findAllPending();
}
