package com.example.factorytraffic.controller;

import com.example.factorytraffic.application.service.TrafficManagementApplicationService;
import com.example.factorytraffic.domain.model.AuditEvent;
import com.example.factorytraffic.domain.model.Junction;
import com.example.factorytraffic.dto.*;
import com.example.factorytraffic.mapper.JunctionDtoMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/junctions")
@CrossOrigin(origins = "*")
public class JunctionController {

    private final TrafficManagementApplicationService applicationService;
    private final JunctionDtoMapper mapper;

    public JunctionController(TrafficManagementApplicationService applicationService, JunctionDtoMapper mapper) {
        this.applicationService = applicationService;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<JunctionStatusResponseDto>> getAllJunctions() {
        List<JunctionStatusResponseDto> list = applicationService.getAllJunctions().stream()
                .map(mapper::toStatusResponseDto)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JunctionStatusResponseDto> getJunction(@PathVariable String id) {
        Junction junction = applicationService.getJunction(id)
                .orElseThrow(() -> new NoSuchElementException("Junction not found: " + id));
        return ResponseEntity.ok(mapper.toStatusResponseDto(junction));
    }

    @PostMapping
    public ResponseEntity<JunctionStatusResponseDto> createJunction(
            @Valid @RequestBody CreateJunctionRequestDto request) {
        Junction created = applicationService.createJunction(request.getId(), request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toStatusResponseDto(created));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<JunctionStatusResponseDto> getJunctionStatus(@PathVariable String id) {
        Junction junction = applicationService.getJunction(id)
                .orElseThrow(() -> new NoSuchElementException("Junction not found: " + id));
        return ResponseEntity.ok(mapper.toStatusResponseDto(junction));
    }

    @PostMapping("/{id}/commands")
    public ResponseEntity<JunctionStatusResponseDto> handleCommand(
            @PathVariable String id,
            @Valid @RequestBody ControlCommandRequestDto request) {
        applicationService.processManualCommand(id, request.getCommand(), request.getDirection());
        Junction updated = applicationService.getJunction(id)
                .orElseThrow(() -> new NoSuchElementException("Junction not found: " + id));
        return ResponseEntity.ok(mapper.toStatusResponseDto(updated));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<AuditEventResponseDto>> getHistory(
            @PathVariable String id,
            @RequestParam(defaultValue = "50") int limit) {
        List<AuditEvent> history = applicationService.getRecentHistory(id, limit);
        List<AuditEventResponseDto> dtoList = history.stream().map(h -> new AuditEventResponseDto(
                h.getId(),
                h.getJunctionId(),
                h.getEventType(),
                h.getDirection() != null ? h.getDirection().name() : null,
                h.getPreviousState() != null ? h.getPreviousState().name() : null,
                h.getNewState() != null ? h.getNewState().name() : null,
                h.getCommandId(),
                h.getVehicleId(),
                h.getDetails(),
                h.getTimestamp()
        )).toList();
        return ResponseEntity.ok(dtoList);
    }
}
