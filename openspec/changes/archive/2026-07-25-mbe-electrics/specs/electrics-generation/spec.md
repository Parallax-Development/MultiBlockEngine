## ADDED Requirements

### Requirement: Energy Generation Mechanics
The system SHALL provide machines (e.g., Coal Generator) that burn fuel or passively generate energy and push it to the `energy` network graph.

#### Scenario: Active generation pushes energy
- **WHEN** a Coal Generator multiblock has burnable fuel in its internal inventory
- **THEN** it consumes the fuel over time
- **AND** it produces energy during each tick, pushing it into the adjacent `energy` network nodes via `NetworkService`.
