## MODIFIED Requirements

### Requirement: Energy Generation Mechanics
The system SHALL provide machines (e.g., Coal Generator) that burn fuel or passively generate energy and push it to the `energy` network graph. Active generators SHALL consume fuel dynamically from their native Bukkit inventories (e.g. Furnace fuel slot) instead of relying on external simulated fuel increments.

#### Scenario: Active generation pushes energy
- **WHEN** a Coal Generator multiblock has a valid burnable fuel item in its native block inventory
- **THEN** it consumes 1 fuel item
- **AND** it sets its internal `fuel_ticks` to match the item's standard burn time
- **AND** it produces energy during each tick while `fuel_ticks` is > 0, pushing it into the adjacent `energy` network nodes via `NetworkService`.
