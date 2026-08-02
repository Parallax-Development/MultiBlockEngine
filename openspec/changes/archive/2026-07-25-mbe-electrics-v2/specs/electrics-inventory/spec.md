## ADDED Requirements

### Requirement: Native Inventory Integration
The system SHALL use the vanilla block states (e.g., `org.bukkit.block.Furnace`) and their respective inventories (`FurnaceInventory`) as the source of truth for input and output items for machines that use them as controllers.

#### Scenario: Dropper inserts item into furnace
- **WHEN** a vanilla hopper pushes an item into the Blast Furnace controller
- **THEN** the Electric Furnace recognizes the item natively without requiring custom inventory syncing events.

### Requirement: Blocking Vanilla Mechanics
The system SHALL prevent vanilla mechanics (like burning coal as fuel) from triggering if the block is part of an active electric machine that uses electricity instead of fuel.

#### Scenario: Player inserts coal in fuel slot
- **WHEN** a player places coal in the fuel slot of the Blast Furnace
- **THEN** the vanilla furnace burn event is cancelled or ignored, preventing non-electrical smelting.
