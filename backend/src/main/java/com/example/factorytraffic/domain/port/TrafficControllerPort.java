package com.example.factorytraffic.domain.port;

import com.example.factorytraffic.domain.model.TrafficCommand;

public interface TrafficControllerPort {
    /**
     * Dispatches a signal change command to the physical controller or simulator.
     *
     * @param command the physical command to send
     */
    void sendSignalCommand(TrafficCommand command);

    /**
     * Queries physical controller for status and actual signal confirmation.
     *
     * @param junctionId junction identifier
     */
    void queryControllerStatus(String junctionId);
}
