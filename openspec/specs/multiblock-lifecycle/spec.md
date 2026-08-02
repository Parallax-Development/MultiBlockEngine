# Multiblock Lifecycle

## Purpose

TBD

## Requirements

### Requirement: Multiblock disassembly lifecycle orchestration
The system SHALL provide a centralized service (`MultiblockLifecycleService`) to handle the complete disassembly process of a Multiblock, which includes firing domain events, executing break actions, unregistering limits, and removing the instance from the runtime.

#### Scenario: Clean disassembly
- **WHEN** a player or the system requests to break a valid multiblock instance
- **THEN** the service fires a `MultiblockBreakEvent`
- **THEN** all `onBreakActions` are executed safely
- **THEN** the player's limits for this multiblock type are unregistered
- **THEN** the multiblock instance is removed from the `MultiblockRuntimeService`
- **THEN** a disassembled message is sent to the player (if applicable)

#### Scenario: Cancelled disassembly
- **WHEN** a player breaks a multiblock BUT an addon or system cancels the `MultiblockBreakEvent`
- **THEN** the service halts the disassembly process
- **THEN** no actions are executed and limits/runtime are not modified
- **THEN** the method returns `false` to notify the infrastructure (e.g. Bukkit Listener) that the break must be cancelled

### Requirement: Safe action execution during breakdown
The system SHALL execute multiblock break actions safely, ensuring that if one action throws an exception, the remaining actions are still executed and the multiblock is still successfully disassembled.

#### Scenario: Action throws exception
- **WHEN** a break action throws an unhandled exception during the disassembly process
- **THEN** the exception is caught, logged, and reported via the `AddonLifecycleService`
- **THEN** the lifecycle service continues to process the remaining actions and completes the disassembly
