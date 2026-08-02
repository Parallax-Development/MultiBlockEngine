## ADDED Requirements

### Requirement: Cloud Command Framework Integration
The system SHALL use Incendo Cloud as the primary command parsing and routing framework for the `/mbe` root command.

#### Scenario: Subcommand resolution
- **WHEN** a player executes an `/mbe` subcommand
- **THEN** Incendo Cloud resolves the subcommand, parses arguments, and invokes the correct isolated command class.

### Requirement: Custom Parsers
The system SHALL provide native Cloud ArgumentParsers to resolve common MBE domain objects from command arguments, such as `MultiblockType`.

#### Scenario: Parse MultiblockType
- **WHEN** a command defines a `MultiblockType` argument
- **THEN** the parser attempts to resolve it from the `MultiblockTypeRegistry`, throwing a localized Cloud error if not found.
