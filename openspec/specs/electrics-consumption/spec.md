# electrics-consumption

## Purpose
TBD

## Requirements

### Requirement: Energy Consumption Mechanics
The system SHALL provide consumer machines (e.g., Electric Furnace) that pull energy from the `energy` network graph to process items or provide utility. The energy SHALL ONLY be pulled when there is an active valid recipe and space for output.

#### Scenario: Active consumption for processing
- **WHEN** an Electric Furnace has processable items in its inventory
- **AND** it is connected to a network with sufficient energy supply
- **AND** its output inventory has space for the result
- **THEN** it draws the required energy from the network per tick
- **AND** it progresses the item processing.

#### Scenario: Paused consumption when idle
- **WHEN** an Electric Furnace has no processable items
- **OR** its output is full
- **THEN** it draws NO energy from the network.
