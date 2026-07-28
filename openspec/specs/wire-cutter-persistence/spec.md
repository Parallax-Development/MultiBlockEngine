# Wire Cutter Persistence

## Purpose
TBD: Persisting cut states for wire cutter.

## Requirements

### Requirement: Cut State Persistence
The system SHALL persist the explicitly disconnected state of adjacent nodes (created via the wire cutter tool) so that they do not automatically reconnect when the chunk or server reloads.

#### Scenario: Server restart retains cut wires
- **WHEN** a player uses a wire cutter to disconnect two adjacent wire blocks
- **THEN** the disconnected faces are saved to persistent storage
- **AND WHEN** the server restarts or chunk reloads
- **THEN** the system reads the persistent storage and prevents those adjacent blocks from forming a network connection
