package com.example.factorytraffic.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.factorytraffic.FactoryTrafficManagementApplication;

@SpringBootTest(classes = FactoryTrafficManagementApplication.class)
@AutoConfigureMockMvc
@DisplayName("Integration Tests: REST API Endpoints and Validation")
class JunctionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/junctions returns 200 with Junction A")
    void getJunctions_returnsOk() throws Exception {
        mockMvc.perform(get("/api/junctions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].junction_id", is("A")));
    }

    @Test
    @DisplayName("GET /api/junctions/{id}/status returns 200 and matches expected structure")
    void getJunctionStatus_returnsOk() throws Exception {
        mockMvc.perform(get("/api/junctions/A/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.junction_id", is("A")))
                .andExpect(jsonPath("$.desired_signals.NORTH", notNullValue()))
                .andExpect(jsonPath("$.actual_signals.NORTH", notNullValue()))
                .andExpect(jsonPath("$.queues.NORTH", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/junctions/UNKNOWN returns 404 Not Found")
    void getUnknownJunction_returns404() throws Exception {
        mockMvc.perform(get("/api/junctions/UNKNOWN_X/status"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("NOT_FOUND")));
    }

    @Test
    @DisplayName("POST /api/sensor-events with invalid vehicle type returns 400 Bad Request")
    void invalidVehicleType_returns400() throws Exception {
        String invalidPayload = """
                {
                    "event_id": "test-err-1",
                    "junction_id": "A",
                    "direction": "NORTH",
                    "event_type": "VEHICLE_ARRIVED",
                    "vehicle_id": "VH-999",
                    "vehicle_type": "SPACESHIP",
                    "sequence_no": 1
                }
                """;

        mockMvc.perform(post("/api/sensor-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/sensor-events with missing event_id returns 400 Bad Request")
    void missingEventId_returns400() throws Exception {
        String invalidPayload = """
                {
                    "junction_id": "A",
                    "direction": "NORTH",
                    "event_type": "VEHICLE_ARRIVED",
                    "vehicle_id": "VH-999"
                }
                """;

        mockMvc.perform(post("/api/sensor-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("POST /api/junctions/A/commands processes manual override successfully")
    void manualCommand_returns200() throws Exception {
        String manualPayload = """
                {
                    "command": "MANUAL_GREEN_REQUEST",
                    "direction": "WEST"
                }
                """;

        mockMvc.perform(post("/api/junctions/A/commands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode", is("MANUAL")));
    }

    @Test
    @DisplayName("POST /api/controller-events with device status reports 200 OK")
    void controllerDeviceStatus_returns200() throws Exception {
        String payload = """
                {
                    "event_id": "status-test-1",
                    "junction_id": "A",
                    "device_type": "SIGNAL_CONTROLLER",
                    "direction": "SOUTH",
                    "status": "ONLINE"
                }
                """;

        mockMvc.perform(post("/api/controller-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RECORDED")))
                .andExpect(jsonPath("$.controller_status", is("ONLINE")));
    }
}
