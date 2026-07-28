## ADDED Requirements

### Requirement: Debug Log Filtering
The system MUST log individual service registrations, item loadings, and multiblock loadings at the DEBUG level instead of INFO, so they are only visible when the debug flag is enabled in the configuration.

#### Scenario: Debug mode disabled
- **WHEN** the plugin starts with `debug: false` in `config.yml`
- **THEN** the console MUST NOT display individual `SERVICE_REGISTER` or `Loaded multiblock` logs.

#### Scenario: Debug mode enabled
- **WHEN** the plugin starts with `debug: true` in `config.yml`
- **THEN** the console MUST display individual `SERVICE_REGISTER` and `Loaded multiblock` logs.

### Requirement: Startup Summary Logs
The system MUST maintain an internal count of multiblocks loaded and services registered during the BOOT and LOAD phases, and output a consolidated summary message at the INFO level when the plugin is fully enabled.

#### Scenario: Plugin startup completion
- **WHEN** the plugin finishes loading its components and phases
- **THEN** it MUST print summary `INFO` logs indicating the total number of multiblocks loaded, addons loaded, and services registered.
