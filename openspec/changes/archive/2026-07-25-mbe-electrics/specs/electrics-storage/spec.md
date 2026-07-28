## ADDED Requirements

### Requirement: Energy Storage Buffers
The system SHALL provide energy storage blocks (e.g., Battery Box) that act as buffers in the `energy` network, accepting excess energy and providing it to consumers when generation is insufficient.

#### Scenario: Storing excess energy
- **WHEN** the network generates more energy than the consumers require
- **THEN** the excess energy is routed into connected Battery Boxes up to their maximum capacity.

#### Scenario: Providing stored energy
- **WHEN** the network consumers require more energy than is actively being generated
- **THEN** the Battery Boxes drain their stored energy to fulfill the network demand.
