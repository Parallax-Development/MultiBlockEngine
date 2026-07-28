## ADDED Requirements

### Requirement: Player Catalog Command
The system SHALL provide a `/mbe catalog` command that opens the interactive multiblock blueprint catalog for the player executing it.

#### Scenario: Player opens catalog
- **WHEN** a player executes `/mbe catalog`
- **THEN** the system opens the Catalog GUI without requiring administrative permissions.

### Requirement: Player Blueprint Command
The system SHALL provide a `/mbe blueprint <id>` command to give a specific blueprint item directly.

#### Scenario: Player requests blueprint
- **WHEN** a player executes `/mbe blueprint oven`
- **THEN** the system gives the blueprint for the `oven` multiblock to the player, provided they have permission or it is unrestricted.
