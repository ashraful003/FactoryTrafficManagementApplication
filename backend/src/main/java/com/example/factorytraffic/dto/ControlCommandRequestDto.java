package com.example.factorytraffic.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class ControlCommandRequestDto {

    @NotBlank(message = "command is required (e.g. MANUAL_GREEN_REQUEST, RETURN_TO_AUTOMATIC)")
    @JsonProperty("command")
    private String command;

    @JsonProperty("direction")
    private String direction;

    public ControlCommandRequestDto() {}

    public ControlCommandRequestDto(String command, String direction) {
        this.command = command;
        this.direction = direction;
    }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
}
