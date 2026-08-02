# Command Autocomplete

## Purpose
TBD (incorporating native cloud command resolution, domain types for command arguments, and explicit argument suggestion providers).

## Requirements

### Requirement: Native Cloud Command Resolution
The system SHALL use Incendo Cloud natively for all core subcommands, abandoning manual string-based routers.

#### Scenario: Developer invokes addons command
- **WHEN** user types `/mbe dev addons ` in chat
- **THEN** Cloud provides autocomplete suggestions for `list` and `status`

#### Scenario: Developer invokes services command
- **WHEN** user types `/mbe dev services ` in chat
- **THEN** Cloud provides autocomplete suggestions for `ui`, `items`, `blueprint`, `assembly`, `list`, and `status`

### Requirement: Domain Types for Command Arguments
The system SHALL use domain-specific types (e.g. `Player`) instead of raw strings to leverage Cloud's native Bukkit argument parsing.

#### Scenario: Admin reports a player
- **WHEN** user types `/mbe admin report ` in chat
- **THEN** Cloud provides autocomplete suggestions containing the names of currently online players

### Requirement: Explicit Argument Suggestion Providers
The system SHALL define `@Suggestions` for arguments that require dynamic or predefined sets of strings (like Enums or Registry keys).

#### Scenario: Player inspects structure level
- **WHEN** user types `/mbe structure inspect ` in chat
- **THEN** Cloud provides autocomplete suggestions for `player`, `operator`, `debug`, and `internal`

#### Scenario: Developer lists UI panels
- **WHEN** user types `/mbe dev ui panels `
- **THEN** Cloud provides autocomplete suggestions for all registered panel IDs
