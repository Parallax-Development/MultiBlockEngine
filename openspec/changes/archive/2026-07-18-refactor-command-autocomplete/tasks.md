## 1. Domain Types and Simple Fixes

- [x] 1.1 Update `AdminCommand.java`: Change `@Argument("target") String targetName` to `@Argument("target") Player targetArg`.
- [x] 1.2 Refactor `AdminCommand.java` internal logic to rely on the injected `Player` object rather than calling `Bukkit.getPlayer()`.
- [x] 1.3 Update `StructureCommand.java`: Change `@Argument("level") String levelStr` to `@Argument("level") InspectionLevel level`.
- [x] 1.4 Refactor `StructureCommand.java` to use the parsed enum natively, removing the raw string case/switch block.

## 2. Suggestion Providers

- [x] 2.1 Update `MBECommandManager.java`: Add a suggestion provider registration for `panelIds` that queries `PanelViewService.getRegisteredPanelIds()`.
- [x] 2.2 Update `DeveloperCommand.java`: Modify `mbe dev ui panels` to accept an argument `@Argument(value = "panel", suggestions = "panelIds") String panelId` (or implement interactive subcommand if none exist).

## 3. Decommissioning Legacy Routers

- [x] 3.1 Refactor `DeveloperCommand.java`: Replace `@Command("mbe dev addons [args]")` with explicit subcommands like `@Command("mbe dev addons list")` and `@Command("mbe dev addons status <addonId>")`.
- [x] 3.2 Move logic from `AddonsCommandRouter.java` into the new `@Command` methods in `DeveloperCommand.java`.
- [x] 3.3 Delete `AddonsCommandRouter.java`.
- [x] 3.4 Refactor `DeveloperCommand.java`: Replace `@Command("mbe dev services [args]")` with explicit subcommands (e.g. `@Command("mbe dev services ui ...")`).
- [x] 3.5 Map the logic from `UiCommandService`, `ItemsCommandService`, `BlueprintCommandService`, and `AssemblyCommandService` into native `@Command` methods, either in `DeveloperCommand.java` or separate command classes (e.g. `DeveloperServicesCommand.java`).
- [x] 3.6 Remove `ServicesCommandRouter.java` and all implementations of `MbeCommandService` in the `core/application/command/service` package.
