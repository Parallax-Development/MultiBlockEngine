## Why

Currently, many developer and admin commands in MultiBlockEngine lack argument autocompletion. This is due to the mixing of the new Incendo Cloud command framework with legacy manual command routers (`MbeCommandService`, `AddonsCommandRouter`, `ServicesCommandRouter`). When arguments are captured as `@Greedy String` to pass to old routers, Cloud cannot provide native subcommands or autocomplete suggestions. Additionally, some arguments use `String` when they should use domain types (like `Player`) or Enums, and custom suggestion providers are missing for registries like UI Panels. Fixing this will vastly improve the UX for developers and admins using the plugin in-game.

## What Changes

- **Convert legacy routers to native Cloud subcommands**: Move logic out of `AddonsCommandRouter` and `ServicesCommandRouter` into explicitly annotated `@Command` methods (e.g. `@Command("mbe dev addons list")`).
- **Use Domain Types instead of Strings**: Update `AdminCommand` to use `Player` instead of `String` for the `target` argument, enabling native Bukkit online player autocompletion.
- **Implement Suggestion Providers**: Create and register a custom suggestion provider in `MBECommandManager` for UI Panel IDs (`mbe dev ui panels`).
- **Use Enums**: Update `StructureCommand` to properly resolve `InspectionLevel` argument with suggestions.
- **BREAKING**: Legacy command routers (`MbeCommandRouter`, `MbeCommandService` implementations inside `core/application/command/service`) will be removed or completely bypassed for these commands.

## Capabilities

### New Capabilities
- `command-autocomplete`: Add comprehensive tab completion across all core developer and admin commands using native Cloud annotations and custom Suggestion Providers.

### Modified Capabilities

- *(No requirement changes to existing capabilities, this is purely an architectural refactor and UX improvement.)*

## Impact

- **Affected Code**: `DeveloperCommand`, `AdminCommand`, `StructureCommand`, `MBECommandManager`, and legacy `*CommandRouter` / `*CommandService` classes.
- **APIs**: The way addons register commands won't change here, but core commands will fully migrate to Cloud.
- **Dependencies**: Relies entirely on the already integrated Incendo Cloud framework.
