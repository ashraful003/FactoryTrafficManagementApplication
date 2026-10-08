package com.example.factorytraffic.domain.port;

import com.example.factorytraffic.domain.model.Junction;

import java.util.List;
import java.util.Optional;

public interface JunctionRepositoryPort {
    Optional<Junction> findById(String id);
    List<Junction> findAll();
    Junction save(Junction junction);
    boolean existsById(String id);
}
