## ADDED Requirements

### Requirement: Energy Consumption Mechanics
The system SHALL provide consumer machines (e.g., Electric Furnace) that pull energy from the `energy` network graph to process items or provide utility.

#### Scenario: Active consumption for processing
- **WHEN** an Electric Furnace has processable items in its inventory
- **AND** it is connected to a network with sufficient energy supply
- **THEN** it draws the required energy from the network per tick
- **AND** it progresses the item processing.
