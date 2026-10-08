package com.example.factorytraffic.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class ErrorResponseDto {

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("status")
    private int status;

    @JsonProperty("error")
    private String error;

    @JsonProperty("message")
    private String message;

    @JsonProperty("path")
    private String path;

    public ErrorResponseDto() {}

    public ErrorResponseDto(Instant timestamp, int status, String error, String message, String path) {
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public Instant getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
}
