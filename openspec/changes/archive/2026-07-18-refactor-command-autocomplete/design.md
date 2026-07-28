## Context

The MultiBlockEngine uses Incendo Cloud for its command framework. However, a significant portion of developer and administrative commands (`mbe dev ...`, `mbe admin ...`) wrap older command router classes (`MbeCommandRouter`, `MbeCommandService`) by capturing a `@Greedy String` argument and parsing it manually. This effectively blinds the Cloud framework to any subcommands or argument types, completely preventing native tab completion for these commands. Furthermore, some arguments request generic `String`s when they should ask for domain objects (like `Player` or an Enum) which Cloud natively understands and provides suggestions for.

## Goals / Non-Goals

**Goals:**
- Completely remove the usage of legacy `MbeCommandRouter` and `MbeCommandService` for the core commands (addons, services, ui, items, blueprint, assembly).
- Refactor all subcommands inside `DeveloperCommand` into explicit Cloud `@Command` methods.
- Correct the argument types in `AdminCommand` to utilize Cloud's native parsers (e.g. `Player`).
- Add specific Suggestion Providers for dynamically registered entities (like UI Panels).

**Non-Goals:**
- We are not changing how addons register their own commands. The `AddonContext#registerCommand` API remains intact.
- We are not altering the underlying logic of what these commands *do*, only how they are registered and parsed.

## Decisions

**1. Eradicating `AddonsCommandRouter` and `ServicesCommandRouter`**
- *Rationale*: These classes exist solely to manually parse string arguments. Since Cloud handles tree-based subcommand routing natively, we will dismantle these routers and move their logic (or direct delegation) into explicit `@Command` annotated methods within `DeveloperCommand` or their respective command classes.
- *Alternative Considered*: Keep the routers but manually define Cloud `@Suggestions` that parse the string and return subcommands. *Rejected* because it defeats the purpose of the framework and makes permission handling per-subcommand harder.

**2. Utilizing Domain Types (`Player`, `InspectionLevel`)**
- *Rationale*: Changing `@Argument("target") String` to `@Argument("target") Player` in `AdminCommand` allows Cloud to automatically suggest online players and inject the `Player` object directly, removing boilerplate `Bukkit.getPlayer()` lookup logic.
- *Rationale*: Changing `@Argument("level") String` to `@Argument("level") InspectionLevel` in `StructureCommand` natively limits inputs to valid enum values and provides them as suggestions.

**3. Custom Suggestion Providers**
- *Rationale*: For dynamic registries like UI Panels, we cannot use an Enum. We will register a suggestion provider `"panelIds"` in `MBECommandManager` that queries `PanelViewService.getRegisteredPanelIds()`.

## Risks / Trade-offs

- **Risk: Breaking changes to internal command APIs** → By removing `MbeCommandService` from core commands, any addon that was manually reflecting into these internal routers might break. However, addons shouldn't be doing this.
- **Trade-off**: The `DeveloperCommand` class will become significantly larger as all subcommands are explicitly declared. This is an acceptable trade-off for full type safety and autocompletion. We can split it into `DeveloperAddonCommand`, `DeveloperServicesCommand` if it becomes too large.
