package com.example.factorytraffic.repository;

import com.example.factorytraffic.entity.QueueVehicleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QueueVehicleJpaRepository extends JpaRepository<QueueVehicleEntity, Long> {
    List<QueueVehicleEntity> findByJunctionIdOrderByQueueOrderAsc(String junctionId);
    List<QueueVehicleEntity> findByJunctionIdAndDirectionOrderByQueueOrderAsc(String junctionId, String direction);
    void deleteByJunctionId(String junctionId);
    void deleteByJunctionIdAndDirectionAndVehicleId(String junctionId, String direction, String vehicleId);
}
