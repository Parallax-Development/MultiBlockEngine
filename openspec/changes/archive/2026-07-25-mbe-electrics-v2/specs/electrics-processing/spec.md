## ADDED Requirements

### Requirement: Processing Machine Recipes
The system SHALL resolve recipes using native Bukkit recipes (e.g., `FurnaceRecipe` or `BlastingRecipe`) for item transformations when standard vanilla machines are used as controllers.

#### Scenario: Valid recipe resolution
- **WHEN** an Electric Furnace has an iron ore in its input slot
- **THEN** it resolves the vanilla blasting recipe for iron ore to iron ingot.

### Requirement: Processing Progress Updates
The system SHALL update the native progress indicator of the controller block (e.g. `BlastFurnace` `cookTime`) while it draws energy and processes an item.

#### Scenario: Visual smelting arrow fills up
- **WHEN** an Electric Furnace is actively smelting an item
- **THEN** the system updates the `cookTime` property of the `BlastFurnace` state natively so clients see the smelting arrow progress.
