## Why

The command system migration to cloud-incendo left the command tree fragmented and partially broken. Key player features like opening the catalog or getting blueprints lack accessible commands. Additionally, there is a major architectural flaw: `CommandRegistrationService` is defined in the API for addons to use, but the core never implements it or registers it, forcing addons to depend directly on the core if they want to register commands, which breaks decoupled architecture rules.

## What Changes

- **Architectural Fix**: `MBECommandManager` will implement `CommandRegistrationService` and will be registered in the service registry under the API interface class, not the core class.
- **Tree Consolidation**: Migrate isolated commands like `/mbe export` to the cloud-incendo annotation system under `/mbe structure export`.
- **Player-Facing Commands**: Add new commands `/mbe catalog` and `/mbe blueprint <id>` so players can interact with multiblocks without needing developer commands.
- **Utility Commands**: Add `/mbe help` and `/mbe item list` for better UX.
- **Dev Command Cleanup**: Flatten deeply nested `/mbe dev services ...` commands into a more manageable `/mbe dev debug <module>` or similar structure.

## Capabilities

### New Capabilities
- `player-commands`: New commands for players to interact with the catalog and blueprints.

### Modified Capabilities
- `command-framework`: Needs to enforce the `CommandRegistrationService` interface correctly.
- `modular-commands`: The core command tree is being heavily reorganized to consolidate export, dev, and item commands.

## Impact

- **Core**: `MBECommandManager.java` and `MultiBlockEngine.java` will be modified to fix the service registration.
- **Commands**: All `*Command.java` and `*CommandService.java` classes in the core will be updated to reflect the new `@Command` paths.
- **Addons**: Addons can now cleanly inject `CommandRegistrationService` to register their own commands without depending on the core.
