package com.example.factorytraffic.repository;

import com.example.factorytraffic.entity.TrafficCommandEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrafficCommandJpaRepository extends JpaRepository<TrafficCommandEntity, String> {
    List<TrafficCommandEntity> findByJunctionIdAndStatus(String junctionId, String status);
    List<TrafficCommandEntity> findByStatus(String status);
}
