# electrics-items

## Purpose
TBD

## Requirements

### Requirement: Electrics Items and Resources
The system SHALL register basic items and resources required for crafting and assembling the machines and cables in `mbe-electrics`.

#### Scenario: Registering crafting components
- **WHEN** the `mbe-electrics` addon loads
- **THEN** it registers items such as Copper Ingot, Iron Plate, Basic Circuit, and Machine Casing via the standard item registry or external plugins (like Oraxen/ItemsAdder if configured).
