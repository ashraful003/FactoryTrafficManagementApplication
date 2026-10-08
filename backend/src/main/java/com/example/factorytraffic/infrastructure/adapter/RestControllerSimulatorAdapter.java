package com.example.factorytraffic.infrastructure.adapter;

import com.example.factorytraffic.domain.model.TrafficCommand;
import com.example.factorytraffic.domain.port.TrafficControllerPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Map;

@Component
@Primary
public class RestControllerSimulatorAdapter implements TrafficControllerPort {

    private static final Logger log = LoggerFactory.getLogger(RestControllerSimulatorAdapter.class);

    // Auto-ACK toggle: enabled by default for seamless automatic simulation,
    // can be disabled via API / dashboard to test controller timeouts & failures.
    private volatile boolean autoAckEnabled = true;

    // In-memory buffer of recent transmitted commands for inspection
    private final List<TrafficCommand> sentCommands = new CopyOnWriteArrayList<>();
    private final Map<String, TrafficCommand> pendingCommands = new ConcurrentHashMap<>();

    // Callback listener for auto-ack delivery
    private ControllerAckListener ackListener;

    public interface ControllerAckListener {
        void onAutoAck(TrafficCommand command);
    }

    public void setAckListener(ControllerAckListener listener) {
        this.ackListener = listener;
    }

    @Override
    public void sendSignalCommand(TrafficCommand command) {
        log.info("[CONTROLLER PORT] Transmitting physical signal command: commandId={}, junction={}, dir={}, state={}",
                command.getCommandId(), command.getJunctionId(), command.getDirection(), command.getRequestedState());

        sentCommands.add(command);
        pendingCommands.put(command.getCommandId(), command);

        if (autoAckEnabled && ackListener != null) {
            // Trigger auto-ack asynchronously or directly
            log.info("[CONTROLLER SIMULATOR] Auto-ACK is ON. Simulating instant confirmation for commandId={}", command.getCommandId());
            ackListener.onAutoAck(command);
        } else {
            log.info("[CONTROLLER SIMULATOR] Auto-ACK is OFF. Awaiting manual ACK for commandId={}", command.getCommandId());
        }
    }

    @Override
    public void queryControllerStatus(String junctionId) {
        log.info("[CONTROLLER PORT] Querying controller status for junctionId={}", junctionId);
    }

    public boolean isAutoAckEnabled() {
        return autoAckEnabled;
    }

    public void setAutoAckEnabled(boolean autoAckEnabled) {
        this.autoAckEnabled = autoAckEnabled;
        log.info("[CONTROLLER SIMULATOR] autoAckEnabled changed to: {}", autoAckEnabled);
    }

    public List<TrafficCommand> getSentCommands() {
        return sentCommands;
    }

    public TrafficCommand getPendingCommand(String commandId) {
        return pendingCommands.get(commandId);
    }

    public void removePendingCommand(String commandId) {
        pendingCommands.remove(commandId);
    }

    public void clear() {
        sentCommands.clear();
        pendingCommands.clear();
    }
}
