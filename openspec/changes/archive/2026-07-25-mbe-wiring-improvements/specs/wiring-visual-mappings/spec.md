## ADDED Requirements

### Requirement: Configuration-driven Visual Connections
The visual rendering system for cables SHALL read connection mappings from configuration files (`mappings/<cable_id>.yml`) to determine if a cable's model should visually extend to connect to an adjacent non-cable block.

#### Scenario: Cable connects to allowed block
- **WHEN** a copper wire block is placed next to a `minecraft:target` block
- **AND WHEN** `mappings/copper_wire.yml` contains `minecraft:target` in its allowed visual connections list
- **THEN** the client-side visual model of the copper wire is rendered with a connection stretching towards the `target` block

#### Scenario: Logical separation
- **WHEN** a cable visually connects to an adjacent block based on mappings
- **THEN** this visual connection SHALL NOT automatically create a logical node connection in the `NetworkGraph` (this remains the responsibility of the multiblock)
