# Capability: command-dispatcher

## Purpose
TBD: Decentralized subcommands and dependency injection for commands.

## Requirements

### Requirement: Decentralized Subcommands
Each major feature (Export, Blueprint, Tool, Debug, etc.) SHALL define its own isolated command class with its dependencies injected via constructors.

#### Scenario: Execute Blueprint Subcommand
- **WHEN** a player runs `/mbe blueprint`
- **THEN** only the `BlueprintCommand` instance handles the request using the injected `BlueprintService`, keeping the command handler clean of other domain logic.
