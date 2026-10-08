package com.example.factorytraffic.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "queue_vehicles", indexes = {
        @Index(name = "idx_qv_junction_dir", columnList = "junction_id, direction")
})
public class QueueVehicleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "junction_id", length = 32, nullable = false)
    private String junctionId;

    @Column(name = "direction", length = 16, nullable = false)
    private String direction;

    @Column(name = "vehicle_id", length = 64, nullable = false)
    private String vehicleId;

    @Column(name = "vehicle_type", length = 32, nullable = false)
    private String vehicleType;

    @Column(name = "sequence_no", nullable = false)
    private Long sequenceNo;

    @Column(name = "arrived_at", nullable = false)
    private Instant arrivedAt;

    @Column(name = "queue_order", nullable = false)
    private int queueOrder;

    public QueueVehicleEntity() {}

    public QueueVehicleEntity(String junctionId, String direction, String vehicleId,
                              String vehicleType, Long sequenceNo, Instant arrivedAt, int queueOrder) {
        this.junctionId = junctionId;
        this.direction = direction;
        this.vehicleId = vehicleId;
        this.vehicleType = vehicleType;
        this.sequenceNo = sequenceNo;
        this.arrivedAt = arrivedAt;
        this.queueOrder = queueOrder;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getJunctionId() { return junctionId; }
    public void setJunctionId(String junctionId) { this.junctionId = junctionId; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public Long getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(Long sequenceNo) { this.sequenceNo = sequenceNo; }

    public Instant getArrivedAt() { return arrivedAt; }
    public void setArrivedAt(Instant arrivedAt) { this.arrivedAt = arrivedAt; }

    public int getQueueOrder() { return queueOrder; }
    public void setQueueOrder(int queueOrder) { this.queueOrder = queueOrder; }
}
