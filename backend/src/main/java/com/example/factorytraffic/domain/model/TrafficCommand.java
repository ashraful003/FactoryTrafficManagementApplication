package com.example.factorytraffic.domain.model;

import java.time.Instant;
import java.util.Objects;

public class TrafficCommand {
    private final String commandId;
    private final String junctionId;
    private final Direction direction;
    private final SignalColor requestedState;
    private CommandStatus status;
    private final Instant createdAt;
    private Instant acknowledgedAt;
    private int retryCount;

    public TrafficCommand(String commandId, String junctionId, Direction direction, SignalColor requestedState) {
        this(commandId, junctionId, direction, requestedState, CommandStatus.PENDING, Instant.now(), null, 0);
    }

    public TrafficCommand(String commandId, String junctionId, Direction direction, SignalColor requestedState,
                          CommandStatus status, Instant createdAt, Instant acknowledgedAt, int retryCount) {
        this.commandId = Objects.requireNonNull(commandId, "commandId must not be null");
        this.junctionId = Objects.requireNonNull(junctionId, "junctionId must not be null");
        this.direction = direction; // null can mean all directions (e.g. ALL_RED)
        this.requestedState = Objects.requireNonNull(requestedState, "requestedState must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.acknowledgedAt = acknowledgedAt;
        this.retryCount = retryCount;
    }

    public String getCommandId() {
        return commandId;
    }

    public String getJunctionId() {
        return junctionId;
    }

    public Direction getDirection() {
        return direction;
    }

    public SignalColor getRequestedState() {
        return requestedState;
    }

    public CommandStatus getStatus() {
        return status;
    }

    public void setStatus(CommandStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(Instant acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public boolean isPending() {
        return status == CommandStatus.PENDING;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TrafficCommand that)) return false;
        return Objects.equals(commandId, that.commandId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commandId);
    }

    @Override
    public String toString() {
        return "TrafficCommand{" +
                "commandId='" + commandId + '\'' +
                ", junctionId='" + junctionId + '\'' +
                ", direction=" + direction +
                ", requestedState=" + requestedState +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", retryCount=" + retryCount +
                '}';
    }
}
