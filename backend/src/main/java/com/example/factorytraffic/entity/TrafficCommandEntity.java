package com.example.factorytraffic.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "traffic_commands", indexes = {
        @Index(name = "idx_cmd_junc_status", columnList = "junction_id, status")
})
public class TrafficCommandEntity {

    @Id
    @Column(name = "command_id", length = 64, nullable = false)
    private String commandId;

    @Column(name = "junction_id", length = 32, nullable = false)
    private String junctionId;

    @Column(name = "direction", length = 16)
    private String direction;

    @Column(name = "requested_state", length = 16, nullable = false)
    private String requestedState;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    public TrafficCommandEntity() {}

    public TrafficCommandEntity(String commandId, String junctionId, String direction,
                                String requestedState, String status, Instant createdAt,
                                Instant acknowledgedAt, int retryCount) {
        this.commandId = commandId;
        this.junctionId = junctionId;
        this.direction = direction;
        this.requestedState = requestedState;
        this.status = status;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.acknowledgedAt = acknowledgedAt;
        this.retryCount = retryCount;
    }

    public String getCommandId() { return commandId; }
    public void setCommandId(String commandId) { this.commandId = commandId; }

    public String getJunctionId() { return junctionId; }
    public void setJunctionId(String junctionId) { this.junctionId = junctionId; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getRequestedState() { return requestedState; }
    public void setRequestedState(String requestedState) { this.requestedState = requestedState; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(Instant acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
}
