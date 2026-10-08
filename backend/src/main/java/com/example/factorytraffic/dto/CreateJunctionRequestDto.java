package com.example.factorytraffic.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class CreateJunctionRequestDto {

    @NotBlank(message = "id is required (e.g. A, B, C)")
    @JsonProperty("id")
    private String id;

    @NotBlank(message = "name is required")
    @JsonProperty("name")
    private String name;

    public CreateJunctionRequestDto() {}

    public CreateJunctionRequestDto(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
