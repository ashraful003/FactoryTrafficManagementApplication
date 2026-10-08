-- Factory Traffic Management System
-- V2 Seed Data Migration (Populate realistic traffic operations data across all tables)

-- 1. JUNCTIONS (Junction A, B, C)
INSERT INTO junctions (
    id, name, mode, current_phase, target_phase, transition_state,
    phase_start_time, transition_step_start_time,
    desired_north, desired_south, desired_east, desired_west,
    actual_north, actual_south, actual_east, actual_west,
    controller_status, active_emergency_vehicle_id, emergency_direction,
    manual_requested_direction, manual_mode_expiry_time, version
) VALUES
(
    'A', 'Factory Gate Junction A', 'AUTOMATIC', 'NORTH_SOUTH', NULL, 'STABLE',
    NOW() - INTERVAL '45 seconds', NOW() - INTERVAL '45 seconds',
    'GREEN', 'GREEN', 'RED', 'RED',
    'GREEN', 'GREEN', 'RED', 'RED',
    'ONLINE', NULL, NULL, NULL, NULL, 1
),
(
    'B', 'Warehouse & Loading Dock Junction B', 'AUTOMATIC', 'EAST_WEST', NULL, 'STABLE',
    NOW() - INTERVAL '25 seconds', NOW() - INTERVAL '25 seconds',
    'RED', 'RED', 'GREEN', 'GREEN',
    'RED', 'RED', 'GREEN', 'GREEN',
    'ONLINE', NULL, NULL, NULL, NULL, 1
),
(
    'C', 'Garment Assembly & Packing Junction C', 'AUTOMATIC', 'NORTH_SOUTH', NULL, 'STABLE',
    NOW() - INTERVAL '15 seconds', NOW() - INTERVAL '15 seconds',
    'GREEN', 'GREEN', 'RED', 'RED',
    'GREEN', 'GREEN', 'RED', 'RED',
    'ONLINE', NULL, NULL, NULL, NULL, 1
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    mode = EXCLUDED.mode,
    current_phase = EXCLUDED.current_phase,
    transition_state = EXCLUDED.transition_state,
    desired_north = EXCLUDED.desired_north,
    desired_south = EXCLUDED.desired_south,
    desired_east = EXCLUDED.desired_east,
    desired_west = EXCLUDED.desired_west,
    actual_north = EXCLUDED.actual_north,
    actual_south = EXCLUDED.actual_south,
    actual_east = EXCLUDED.actual_east,
    actual_west = EXCLUDED.actual_west,
    controller_status = EXCLUDED.controller_status;

-- 2. QUEUE VEHICLES (Clear existing seed and repopulate cleanly)
DELETE FROM queue_vehicles;

-- Junction A Vehicles
INSERT INTO queue_vehicles (junction_id, direction, vehicle_id, vehicle_type, sequence_no, arrived_at, queue_order) VALUES
('A', 'NORTH', 'VH-TRK-101', 'TRUCK', 1001, NOW() - INTERVAL '35 seconds', 0),
('A', 'NORTH', 'VH-FL-204', 'FORKLIFT', 1002, NOW() - INTERVAL '22 seconds', 1),
('A', 'NORTH', 'VH-EMP-551', 'EMPLOYEE_VEHICLE', 1003, NOW() - INTERVAL '10 seconds', 2),
('A', 'SOUTH', 'VH-FL-209', 'FORKLIFT', 1004, NOW() - INTERVAL '18 seconds', 0),
('A', 'EAST', 'VH-TRK-302', 'TRUCK', 1005, NOW() - INTERVAL '48 seconds', 0),
('A', 'EAST', 'VH-TRK-305', 'TRUCK', 1006, NOW() - INTERVAL '40 seconds', 1),
('A', 'EAST', 'VH-FL-215', 'FORKLIFT', 1007, NOW() - INTERVAL '25 seconds', 2),
('A', 'EAST', 'VH-EMP-712', 'EMPLOYEE_VEHICLE', 1008, NOW() - INTERVAL '12 seconds', 3),
('A', 'WEST', 'VH-EMP-801', 'EMPLOYEE_VEHICLE', 1009, NOW() - INTERVAL '28 seconds', 0),
('A', 'WEST', 'VH-EMP-804', 'EMPLOYEE_VEHICLE', 1010, NOW() - INTERVAL '14 seconds', 1);

-- Junction B Vehicles
INSERT INTO queue_vehicles (junction_id, direction, vehicle_id, vehicle_type, sequence_no, arrived_at, queue_order) VALUES
('B', 'NORTH', 'VH-TRK-401', 'TRUCK', 2001, NOW() - INTERVAL '30 seconds', 0),
('B', 'NORTH', 'VH-FL-301', 'FORKLIFT', 2002, NOW() - INTERVAL '15 seconds', 1),
('B', 'SOUTH', 'VH-TRK-405', 'TRUCK', 2003, NOW() - INTERVAL '25 seconds', 0),
('B', 'SOUTH', 'VH-FL-308', 'FORKLIFT', 2004, NOW() - INTERVAL '10 seconds', 1),
('B', 'EAST', 'VH-EMP-902', 'EMPLOYEE_VEHICLE', 2005, NOW() - INTERVAL '8 seconds', 0),
('B', 'WEST', 'VH-FL-315', 'FORKLIFT', 2006, NOW() - INTERVAL '5 seconds', 0);

-- Junction C Vehicles
INSERT INTO queue_vehicles (junction_id, direction, vehicle_id, vehicle_type, sequence_no, arrived_at, queue_order) VALUES
('C', 'NORTH', 'VH-FL-501', 'FORKLIFT', 3001, NOW() - INTERVAL '20 seconds', 0),
('C', 'SOUTH', 'VH-EMP-111', 'EMPLOYEE_VEHICLE', 3002, NOW() - INTERVAL '18 seconds', 0),
('C', 'SOUTH', 'VH-EMP-112', 'EMPLOYEE_VEHICLE', 3003, NOW() - INTERVAL '12 seconds', 1),
('C', 'EAST', 'VH-TRK-601', 'TRUCK', 3004, NOW() - INTERVAL '22 seconds', 0),
('C', 'WEST', 'VH-TRK-603', 'TRUCK', 3005, NOW() - INTERVAL '16 seconds', 0);

-- 3. PROCESSED EVENTS (Idempotency Audit)
INSERT INTO processed_events (event_id, junction_id, event_type, vehicle_id, processed_at) VALUES
('evt-seed-1001', 'A', 'VEHICLE_ARRIVED', 'VH-TRK-101', NOW() - INTERVAL '35 seconds'),
('evt-seed-1002', 'A', 'VEHICLE_ARRIVED', 'VH-FL-204', NOW() - INTERVAL '22 seconds'),
('evt-seed-1003', 'A', 'VEHICLE_ARRIVED', 'VH-EMP-551', NOW() - INTERVAL '10 seconds'),
('evt-seed-1004', 'A', 'VEHICLE_ARRIVED', 'VH-FL-209', NOW() - INTERVAL '18 seconds'),
('evt-seed-1005', 'A', 'VEHICLE_ARRIVED', 'VH-TRK-302', NOW() - INTERVAL '48 seconds'),
('evt-seed-1006', 'A', 'VEHICLE_ARRIVED', 'VH-TRK-305', NOW() - INTERVAL '40 seconds'),
('evt-seed-1007', 'A', 'VEHICLE_ARRIVED', 'VH-FL-215', NOW() - INTERVAL '25 seconds'),
('evt-seed-1008', 'A', 'VEHICLE_ARRIVED', 'VH-EMP-712', NOW() - INTERVAL '12 seconds'),
('evt-seed-1009', 'A', 'VEHICLE_ARRIVED', 'VH-EMP-801', NOW() - INTERVAL '28 seconds'),
('evt-seed-1010', 'A', 'VEHICLE_ARRIVED', 'VH-EMP-804', NOW() - INTERVAL '14 seconds')
ON CONFLICT (event_id) DO NOTHING;

-- 4. TRAFFIC COMMANDS (Hardware Actuator History)
INSERT INTO traffic_commands (command_id, junction_id, direction, requested_state, status, created_at, acknowledged_at, retry_count) VALUES
('cmd-8001', 'A', 'NORTH', 'GREEN', 'ACK', NOW() - INTERVAL '45 seconds', NOW() - INTERVAL '45 seconds' + INTERVAL '120 milliseconds', 0),
('cmd-8002', 'A', 'SOUTH', 'GREEN', 'ACK', NOW() - INTERVAL '45 seconds', NOW() - INTERVAL '45 seconds' + INTERVAL '135 milliseconds', 0),
('cmd-8003', 'A', 'EAST', 'RED', 'ACK', NOW() - INTERVAL '45 seconds', NOW() - INTERVAL '45 seconds' + INTERVAL '110 milliseconds', 0),
('cmd-8004', 'A', 'WEST', 'RED', 'ACK', NOW() - INTERVAL '45 seconds', NOW() - INTERVAL '45 seconds' + INTERVAL '140 milliseconds', 0),
('cmd-8101', 'B', 'EAST', 'GREEN', 'ACK', NOW() - INTERVAL '25 seconds', NOW() - INTERVAL '25 seconds' + INTERVAL '95 milliseconds', 0),
('cmd-8102', 'B', 'WEST', 'GREEN', 'ACK', NOW() - INTERVAL '25 seconds', NOW() - INTERVAL '25 seconds' + INTERVAL '105 milliseconds', 0),
('cmd-8201', 'C', 'NORTH', 'GREEN', 'ACK', NOW() - INTERVAL '15 seconds', NOW() - INTERVAL '15 seconds' + INTERVAL '80 milliseconds', 0),
('cmd-8202', 'C', 'SOUTH', 'GREEN', 'ACK', NOW() - INTERVAL '15 seconds', NOW() - INTERVAL '15 seconds' + INTERVAL '90 milliseconds', 0)
ON CONFLICT (command_id) DO NOTHING;

-- 5. AUDIT EVENTS (System Event History)
INSERT INTO audit_events (id, junction_id, event_type, direction, previous_state, new_state, command_id, vehicle_id, details, timestamp) VALUES
('aud-001', 'A', 'JUNCTION_CREATED', NULL, NULL, NULL, NULL, NULL, 'Factory Gate Junction A initialized and connected to PostgreSQL.', NOW() - INTERVAL '120 seconds'),
('aud-002', 'A', 'VEHICLE_ARRIVED', 'EAST', NULL, NULL, NULL, 'VH-TRK-302', 'Vehicle VH-TRK-302 (TRUCK) arrived from EAST. Queue size: 1', NOW() - INTERVAL '48 seconds'),
('aud-003', 'A', 'SIGNAL_CHANGED', 'NORTH', 'RED', 'GREEN', 'cmd-8001', NULL, 'Signal transitioned from RED to GREEN on command cmd-8001', NOW() - INTERVAL '45 seconds'),
('aud-004', 'A', 'CONTROLLER_ACK', 'NORTH', NULL, 'GREEN', 'cmd-8001', NULL, 'Controller confirmed state GREEN for direction NORTH', NOW() - INTERVAL '45 seconds'),
('aud-005', 'A', 'VEHICLE_ARRIVED', 'EAST', NULL, NULL, NULL, 'VH-TRK-305', 'Vehicle VH-TRK-305 (TRUCK) arrived from EAST. Queue size: 2', NOW() - INTERVAL '40 seconds'),
('aud-006', 'A', 'VEHICLE_ARRIVED', 'NORTH', NULL, NULL, NULL, 'VH-TRK-101', 'Vehicle VH-TRK-101 (TRUCK) arrived from NORTH. Queue size: 1', NOW() - INTERVAL '35 seconds'),
('aud-007', 'A', 'VEHICLE_ARRIVED', 'WEST', NULL, NULL, NULL, 'VH-EMP-801', 'Vehicle VH-EMP-801 (EMPLOYEE_VEHICLE) arrived from WEST. Queue size: 1', NOW() - INTERVAL '28 seconds'),
('aud-008', 'A', 'VEHICLE_ARRIVED', 'EAST', NULL, NULL, NULL, 'VH-FL-215', 'Vehicle VH-FL-215 (FORKLIFT) arrived from EAST. Queue size: 3', NOW() - INTERVAL '25 seconds'),
('aud-009', 'A', 'VEHICLE_ARRIVED', 'NORTH', NULL, NULL, NULL, 'VH-FL-204', 'Vehicle VH-FL-204 (FORKLIFT) arrived from NORTH. Queue size: 2', NOW() - INTERVAL '22 seconds'),
('aud-010', 'A', 'VEHICLE_ARRIVED', 'SOUTH', NULL, NULL, NULL, 'VH-FL-209', 'Vehicle VH-FL-209 (FORKLIFT) arrived from SOUTH. Queue size: 1', NOW() - INTERVAL '18 seconds'),
('aud-011', 'A', 'VEHICLE_ARRIVED', 'WEST', NULL, NULL, NULL, 'VH-EMP-804', 'Vehicle VH-EMP-804 (EMPLOYEE_VEHICLE) arrived from WEST. Queue size: 2', NOW() - INTERVAL '14 seconds'),
('aud-012', 'A', 'VEHICLE_ARRIVED', 'EAST', NULL, NULL, NULL, 'VH-EMP-712', 'Vehicle VH-EMP-712 (EMPLOYEE_VEHICLE) arrived from EAST. Queue size: 4', NOW() - INTERVAL '12 seconds'),
('aud-013', 'A', 'VEHICLE_ARRIVED', 'NORTH', NULL, NULL, NULL, 'VH-EMP-551', 'Vehicle VH-EMP-551 (EMPLOYEE_VEHICLE) arrived from NORTH. Queue size: 3', NOW() - INTERVAL '10 seconds')
ON CONFLICT (id) DO NOTHING;
