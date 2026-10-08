package com.example.factorytraffic.scheduler;

import com.example.factorytraffic.application.service.TrafficManagementApplicationService;
import com.example.factorytraffic.domain.model.Junction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TrafficScheduler {

    private static final Logger log = LoggerFactory.getLogger(TrafficScheduler.class);
    private final TrafficManagementApplicationService applicationService;

    public TrafficScheduler(TrafficManagementApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /**
     * Ticks the traffic state machine, advances transition steps,
     * checks ACK timeouts, and evaluates automatic scheduling.
     */
    @Scheduled(fixedDelay = 1000)
    public void runTrafficLoop() {
        try {
            List<Junction> junctions = applicationService.getAllJunctions();
            for (Junction j : junctions) {
                applicationService.tickJunction(j.getId());
            }
        } catch (Exception e) {
            log.error("[SCHEDULER ERROR] Exception in traffic loop: {}", e.getMessage(), e);
        }
    }
}
