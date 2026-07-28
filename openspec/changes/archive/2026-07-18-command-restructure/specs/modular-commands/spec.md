## MODIFIED Requirements

### Requirement: Modular Subcommand Registration
The core system SHALL provide a central `CommandRegistrationService` component, exposed via the API, allowing internal modules and third-party addons to register commands cleanly using Incendo Cloud annotations.

#### Scenario: Addon registers a command
- **WHEN** an addon is initialized
- **THEN** it can inject `CommandRegistrationService` (from the API) to register custom Cloud commands without directly depending on the core `MBECommandManager` class.

## ADDED Requirements

### Requirement: Tree Consolidation
The command tree SHALL be organized logically, avoiding excessive nesting for dev tools, grouping structure manipulation under `/mbe structure`, and moving export functionalities from `/mbe export` to `/mbe structure export`.

#### Scenario: Exporting a structure
- **WHEN** an admin wants to export a structure
- **THEN** they execute `/mbe structure export start` instead of the old `/mbe export start` path.
