## Context

The main command of the plugin (`MultiblockCommand.java`) has grown excessively, reaching nearly 1300 lines of code. It currently acts as a "God Object", handling responsibilities like argument parsing, manual permission validation, message formatting, block raytracing, and heavy logic like item creation and exporting. It manages all subcommands (`/mbe export`, `/mbe blueprint`, `/mbe debug`, etc.) centrally. This violates the Single Responsibility Principle (SRP) and severely affects maintainability.

The goal is to adopt the **Incendo Cloud** (`cloud-paper`) framework to handle parsing, autocompletion, and fragmentation of this logic into small, manageable classes, enabling a cleaner integration for core subcommands and addons.

## Goals / Non-Goals

**Goals:**
- Completely replace Bukkit's native `CommandExecutor` and `TabCompleter` implementations with Incendo Cloud.
- Split `MultiblockCommand` into specific subcommand classes (e.g., `ExportCommand`, `BlueprintCommand`, `DebugCommand`).
- Delegate repetitive validations (finding players by name, getting `MultiblockType` by ID, getting target block) to Cloud's native parsers/resolvers.
- Register all commands under a unified, modular command manager.

**Non-Goals:**
- Completely rewriting the business logic of each subcommand. (The logic remains the same, only the routing and argument parsing changes).
- Refactoring commands outside of the `/mbe` hierarchy if any exist.

## Decisions

### 1. Incendo Cloud as the Command Framework
**Why:** Incendo Cloud is an industry standard for Minecraft command management. It supports Brigadier integration natively, provides strict type validation, and removes all parsing boilerplate.
**Alternative:** CommandAPI, but Incendo Cloud is preferred by the user and integrates smoothly into Paper/Bukkit ecosystems.

### 2. Dependency Injection for Commands
**Why:** Command classes will only accept their required dependencies (e.g., `BlueprintService`, `MultiblockRuntimeService`) via constructor injection (using `MBEServiceRegistry` or the new `EventBusService` context). This ensures commands act purely as presenters/controllers and do not hold state or fetch dependencies manually.

### 3. Custom Parsers/Resolvers
**Why:** We need custom parsers for domains specific to MBE, such as `MultiblockTypeParser` (to resolve `MultiblockType` by ID from arguments), and `ItemDefinitionParser` (for custom MBE items). Cloud makes it easy to register custom `ArgumentParser` implementations.

## Risks / Trade-offs

- **Risk:** Existing addons that programmatically invoke `/mbe` commands or register subcommands natively using the Bukkit API will break.
  **Mitigation:** This is an accepted BREAKING change. We will expose the `PaperCommandManager` (or `LegacyPaperCommandManager`) so addons can register their own branches under `/mbe` natively with Cloud.
- **Risk:** Cloud's annotation system vs. builder system.
  **Mitigation:** We will prefer Cloud's Builder API (or Annotation API if the user prefers, but Builder is explicit and fits well with plugin initialization). We'll clarify this in the specs.

## Migration Plan

1. Add `cloud-paper` dependency to `build.gradle`.
2. Create `MBECommandManager` to wrap the Cloud manager setup.
3. Migrate one subcommand first (e.g., `/mbe debug`) to test the custom parsers.
4. Progressively migrate all other subcommands (`export`, `blueprint`, `tool`, etc.) into their own classes.
5. Remove the old `MultiblockCommand.java` and related legacy routing classes.
