## Overview

This design outlines the refactoring of the command system to respect the API/Core boundaries and restructures the command tree for better UX.

## Architecture & Interfaces

### CommandRegistrationService Fix
- **Problem**: `MultiBlockEngine.java` currently registers `MBECommandManager.class` into the service registry. `MBECommandManager` is a core class. Addons cannot access it without breaking architecture rules.
- **Solution**: 
  - `MBECommandManager` will implement `dev.darkblade.mbe.api.command.CommandRegistrationService`.
  - In `MultiBlockEngine.java`, change the registration to `addonManager.registerCoreService(CommandRegistrationService.class, commandManager);`.

## Data Models & State

N/A - this is purely routing and dependency injection.

## Workflows

### Refactoring the Command Tree
All existing commands will be updated to match the new schema using Cloud annotations:
- `ExportCommand.java`: Currently uses builders. Will be migrated to use `@Command("mbe structure export ...")`.
- `DeveloperCommand.java` and `*CommandService.java`: Will be flattened. E.g., `mbe dev services assembly assemble` will just be `mbe dev debug assembly` or similar.
- **New Commands**: 
  - `CatalogCommand.java` (`@Command("mbe catalog")`)
  - `BlueprintCommand.java` (`@Command("mbe blueprint <id>")`)
  - `HelpCommand.java` (`@Command("mbe help")`)

## Constraints & Assumptions

- Addons that were wrongly injecting `MBECommandManager` (if any exist) will need to be updated to inject `CommandRegistrationService`.
- Cloud-incendo annotations must be fully utilized in the new `ExportCommand` migration.
