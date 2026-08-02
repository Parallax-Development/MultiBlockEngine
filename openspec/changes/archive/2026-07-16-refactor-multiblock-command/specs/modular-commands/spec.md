## ADDED Requirements

### Requirement: Modular Subcommand Registration
The core system SHALL provide a central `MBECommandManager` component that exposes the Incendo Cloud manager instance or a wrapper, allowing internal modules and third-party addons to register commands under the `/mbe` node.

#### Scenario: Addon registers a command
- **WHEN** an addon is initialized
- **THEN** it can inject `MBECommandManager` to register custom Cloud commands without interfering with Bukkit's `PluginCommand` fallback mechanism.
