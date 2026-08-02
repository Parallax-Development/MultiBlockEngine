## MODIFIED Requirements

### Requirement: Electrics Items and Resources
The system SHALL register basic items and resources required for crafting and assembling the machines and cables in `mbe-electrics`. The system SHALL also register tool items used for network diagnostics, specifically `mbe-electrics:multimeter`.

#### Scenario: Registering crafting components
- **WHEN** the `mbe-electrics` addon loads
- **THEN** it registers items such as Copper Ingot, Iron Plate, Basic Circuit, and Machine Casing via the standard item registry or external plugins (like Oraxen/ItemsAdder if configured).

#### Scenario: Registering the multimeter tool item
- **WHEN** the `mbe-electrics` addon enables and `ItemService` is available
- **THEN** it registers `mbe-electrics:multimeter` with material `CLOCK`, `customModelData = 200`, and `unstackable = true` via the standard `ItemRegistry`.
