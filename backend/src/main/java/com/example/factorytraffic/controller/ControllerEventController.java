package com.example.factorytraffic.controller;

import com.example.factorytraffic.application.service.TrafficManagementApplicationService;
import com.example.factorytraffic.domain.model.SignalColor;
import com.example.factorytraffic.dto.ControllerEventRequestDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/controller-events")
@CrossOrigin(origins = "*")
public class ControllerEventController {

    private final TrafficManagementApplicationService applicationService;

    public ControllerEventController(TrafficManagementApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> receiveControllerEvent(
            @Valid @RequestBody ControllerEventRequestDto request) {

        // Case 1: Device / Controller Status event (Section 9, 14.6, 14.8)
        // e.g. status: "OFFLINE", "ONLINE", "DEGRADED", or device_type present
        if (request.getDeviceType() != null
                || "OFFLINE".equalsIgnoreCase(request.getStatus())
                || "ONLINE".equalsIgnoreCase(request.getStatus())
                || "DEGRADED".equalsIgnoreCase(request.getStatus())
                || request.getCommandId() == null) {

            String junctionId = request.getJunctionId() != null ? request.getJunctionId() : "A";
            boolean updated = applicationService.handleDeviceStatus(
                    junctionId,
                    request.getDeviceType(),
                    request.getDirection(),
                    request.getStatus(),
                    request.getTimestamp()
            );

            return ResponseEntity.ok(Map.of(
                    "status", "RECORDED",
                    "junction_id", junctionId,
                    "device_type", request.getDeviceType() != null ? request.getDeviceType() : "SIGNAL_CONTROLLER",
                    "controller_status", request.getStatus().toUpperCase(),
                    "processed", updated
            ));
        }

        // Case 2: Command ACK / NACK event (Section 10.5)
        SignalColor color = null;
        if (request.getActualState() != null && !request.getActualState().isBlank()) {
            try {
                color = SignalColor.valueOf(request.getActualState().toUpperCase());
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid actual_state: " + request.getActualState());
            }
        }

        boolean processed = applicationService.handleControllerAck(
                request.getCommandId(),
                request.getStatus(),
                color,
                request.getTimestamp()
        );

        return ResponseEntity.ok(Map.of(
                "command_id", request.getCommandId(),
                "processed", processed,
                "status", "RECORDED"
        ));
    }
}
