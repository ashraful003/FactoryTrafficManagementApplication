package com.example.factorytraffic.mapper;

import com.example.factorytraffic.domain.model.Direction;
import com.example.factorytraffic.domain.model.Junction;
import com.example.factorytraffic.domain.model.TrafficQueue;
import com.example.factorytraffic.dto.JunctionStatusResponseDto;
import com.example.factorytraffic.dto.VehicleResponseDto;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class JunctionDtoMapper {

    public JunctionStatusResponseDto toStatusResponseDto(Junction junction) {
        if (junction == null) return null;

        JunctionStatusResponseDto dto = new JunctionStatusResponseDto();
        dto.setJunctionId(junction.getId());
        dto.setName(junction.getName());
        dto.setMode(junction.getMode().name());
        dto.setPhase(junction.getCurrentPhase() != null ? junction.getCurrentPhase().name() : "ALL_RED");
        dto.setTransitionState(junction.getTransitionState().name());
        dto.setControllerStatus(junction.getControllerStatus().name());

        Map<String, String> desired = new LinkedHashMap<>();
        for (Direction d : Direction.values()) {
            desired.put(d.name(), junction.getDesiredSignals().get(d).name());
        }
        dto.setDesiredSignals(desired);

        Map<String, String> actual = new LinkedHashMap<>();
        for (Direction d : Direction.values()) {
            actual.put(d.name(), junction.getActualSignals().get(d).name());
        }
        dto.setActualSignals(actual);

        Map<String, Integer> queues = new LinkedHashMap<>();
        Map<String, List<VehicleResponseDto>> queuedVehicles = new LinkedHashMap<>();
        Instant now = Instant.now();

        for (Direction d : Direction.values()) {
            TrafficQueue q = junction.getQueue(d);
            queues.put(d.name(), q.size());

            List<VehicleResponseDto> vList = q.getVehicles().stream()
                    .map(v -> new VehicleResponseDto(
                            v.getVehicleId(),
                            v.getVehicleType().name(),
                            v.getDirection().name(),
                            v.getSequenceNo(),
                            v.getArrivedAt(),
                            v.getWaitingSeconds(now)
                    )).toList();
            queuedVehicles.put(d.name(), vList);
        }
        dto.setQueues(queues);
        dto.setQueuedVehicles(queuedVehicles);

        dto.setActiveEmergency(junction.hasEmergency());
        dto.setEmergencyDirection(junction.getEmergencyDirection() != null ? junction.getEmergencyDirection().name() : null);
        dto.setVersion(junction.getVersion());

        return dto;
    }
}
