package com.example.factorytraffic.controller;

import com.example.factorytraffic.application.service.TrafficManagementApplicationService;
import com.example.factorytraffic.domain.model.Junction;
import com.example.factorytraffic.dto.JunctionStatusResponseDto;
import com.example.factorytraffic.dto.SensorEventRequestDto;
import com.example.factorytraffic.mapper.JunctionDtoMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/sensor-events")
@CrossOrigin(origins = "*")
public class SensorEventController {

    private final TrafficManagementApplicationService applicationService;
    private final JunctionDtoMapper mapper;

    public SensorEventController(TrafficManagementApplicationService applicationService, JunctionDtoMapper mapper) {
        this.applicationService = applicationService;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<JunctionStatusResponseDto> receiveSensorEvent(
            @Valid @RequestBody SensorEventRequestDto request) {
        applicationService.processSensorEvent(
                request.getEventId(),
                request.getJunctionId(),
                request.getDirection(),
                request.getEventType(),
                request.getVehicleId(),
                request.getVehicleType(),
                request.getSequenceNo(),
                request.getTimestamp()
        );

        Junction junction = applicationService.getJunction(request.getJunctionId())
                .orElseThrow(() -> new NoSuchElementException("Junction not found: " + request.getJunctionId()));

        return ResponseEntity.ok(mapper.toStatusResponseDto(junction));
    }
}
