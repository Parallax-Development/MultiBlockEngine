## MODIFIED Requirements

### Requirement: Multiblock Assembly Event
The system SHALL emit an assembly event when a multiblock is formed, using the agnostic event bus and domain wrappers instead of Bukkit events.

#### Scenario: Successful assembly event emission
- **WHEN** a multiblock is successfully assembled by a player
- **THEN** the system publishes an `MBEMultiblockFormEvent` to the event bus containing the `MBEPlayer` and `MultiblockInstance`
