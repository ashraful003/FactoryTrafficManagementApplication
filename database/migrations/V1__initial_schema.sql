-- Factory Traffic Management System
-- V1 Schema Migration (PostgreSQL)

CREATE TABLE IF NOT EXISTS junctions (
    id VARCHAR(32) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    mode VARCHAR(32) NOT NULL,
    current_phase VARCHAR(32) NOT NULL,
    target_phase VARCHAR(32),
    transition_state VARCHAR(32) NOT NULL,
    phase_start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    transition_step_start_time TIMESTAMP WITH TIME ZONE,
    desired_north VARCHAR(16) NOT NULL,
    desired_south VARCHAR(16) NOT NULL,
    desired_east VARCHAR(16) NOT NULL,
    desired_west VARCHAR(16) NOT NULL,
    actual_north VARCHAR(16) NOT NULL,
    actual_south VARCHAR(16) NOT NULL,
    actual_east VARCHAR(16) NOT NULL,
    actual_west VARCHAR(16) NOT NULL,
    controller_status VARCHAR(32) NOT NULL,
    active_emergency_vehicle_id VARCHAR(64),
    emergency_direction VARCHAR(16),
    manual_requested_direction VARCHAR(16),
    manual_mode_expiry_time TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS queue_vehicles (
    id BIGSERIAL PRIMARY KEY,
    junction_id VARCHAR(32) NOT NULL,
    direction VARCHAR(16) NOT NULL,
    vehicle_id VARCHAR(64) NOT NULL,
    vehicle_type VARCHAR(32) NOT NULL,
    sequence_no BIGINT NOT NULL,
    arrived_at TIMESTAMP WITH TIME ZONE NOT NULL,
    queue_order INT NOT NULL,
    CONSTRAINT fk_qv_junction FOREIGN KEY (junction_id) REFERENCES junctions(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_qv_junction_dir ON queue_vehicles(junction_id, direction);

CREATE TABLE IF NOT EXISTS processed_events (
    event_id VARCHAR(64) PRIMARY KEY,
    junction_id VARCHAR(32) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    vehicle_id VARCHAR(64),
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_pe_junc_time ON processed_events(junction_id, processed_at);

CREATE TABLE IF NOT EXISTS traffic_commands (
    command_id VARCHAR(64) PRIMARY KEY,
    junction_id VARCHAR(32) NOT NULL,
    direction VARCHAR(16),
    requested_state VARCHAR(16) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    retry_count INT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_cmd_junc_status ON traffic_commands(junction_id, status);

CREATE TABLE IF NOT EXISTS audit_events (
    id VARCHAR(64) PRIMARY KEY,
    junction_id VARCHAR(32) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    direction VARCHAR(16),
    previous_state VARCHAR(16),
    newState VARCHAR(16),
    command_id VARCHAR(64),
    vehicle_id VARCHAR(64),
    details VARCHAR(1024),
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ae_junc_time ON audit_events(junction_id, timestamp DESC);
