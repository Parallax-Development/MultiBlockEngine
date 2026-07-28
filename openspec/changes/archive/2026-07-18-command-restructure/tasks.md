## 1. Architecture Fixes

- [x] 1.1 Update `MBECommandManager` to implement `CommandRegistrationService` from the API.
- [x] 1.2 Modify `MultiBlockEngine.java` to register the service as `CommandRegistrationService.class` instead of the core implementation class.

## 2. Command Tree Refactor

- [x] 2.1 Refactor `ExportCommand.java` to use `@Command("mbe structure export ...")` annotations instead of manual Cloud builders.
- [x] 2.2 Flatten the deeply nested developer commands in `*CommandService.java` classes (e.g., move `mbe dev services assembly ...` to `mbe dev debug assembly ...`).

## 3. Player-Facing Commands

- [x] 3.1 Create a new `CatalogCommand.java` providing `/mbe catalog` to open the Blueprint GUI.
- [x] 3.2 Update `BlueprintCommand.java` (or create a new player command) to provide `/mbe blueprint <id>` for giving blueprints directly.
- [x] 3.3 Update `ItemCommand.java` to include a `/mbe item list` command.
- [x] 3.4 Create a `HelpCommand.java` providing `/mbe help`.
