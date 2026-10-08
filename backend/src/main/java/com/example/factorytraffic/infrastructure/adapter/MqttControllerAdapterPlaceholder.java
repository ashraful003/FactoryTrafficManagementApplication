package com.example.factorytraffic.infrastructure.adapter;

import com.example.factorytraffic.domain.model.TrafficCommand;
import com.example.factorytraffic.domain.port.TrafficControllerPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Architectural Adapter for MQTT protocol.
 * Meets Requirement 25: The domain engine interacts with TrafficControllerPort abstraction,
 * allowing this MQTT adapter to seamlessly replace or supplement REST controller simulation
 * without rewriting any traffic-control or domain logic.
 *
 * Topic design:
 *   Commands:  factory/junction/{junctionId}/commands
 *   Telemetry: factory/junction/{junctionId}/telemetry
 *   ACKs:      factory/junction/{junctionId}/acks
 */
@Component("mqttControllerAdapter")
public class MqttControllerAdapterPlaceholder implements TrafficControllerPort {

    private static final Logger log = LoggerFactory.getLogger(MqttControllerAdapterPlaceholder.class);

    @Override
    public void sendSignalCommand(TrafficCommand command) {
        // In full MQTT deployment, this serializes the command to JSON and publishes to:
        // topic: factory/junction/{junctionId}/commands
        log.info("[MQTT ADAPTER READY] Would publish MQTT message to topic factory/junction/{}/commands: {}",
                command.getJunctionId(), command.getCommandId());
    }

    @Override
    public void queryControllerStatus(String junctionId) {
        // Publishes ping / status query message to MQTT
        log.info("[MQTT ADAPTER READY] Would publish MQTT status inquiry to topic factory/junction/{}/status_req", junctionId);
    }
}
