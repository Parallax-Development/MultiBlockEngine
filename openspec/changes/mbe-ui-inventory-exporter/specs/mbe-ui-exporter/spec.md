# Spec Delta: `mbe-ui-exporter`

## ADDED Requirements

### Requirement: Export Command & Session Handling
The system MUST support starting a 30-second export session via command, respecting file overwrite protections.

#### Scenario: Starting an export session for a new panel
- **WHEN** an authorized player executes `/mbeui exportpanel my_panel`
- **AND** `addons/mbe-ui/exports/my_panel.yml` does not exist
- **THEN** the system registers a 30-second export session for the player and sends a confirmation message.

#### Scenario: Preventing accidental file overwrite without flag
- **WHEN** an authorized player executes `/mbeui exportpanel my_panel`
- **AND** `addons/mbe-ui/exports/my_panel.yml` already exists
- **THEN** the system aborts without creating a session and notifies the player that the file exists.

#### Scenario: Overwriting an existing panel with flag
- **WHEN** an authorized player executes `/mbeui exportpanel my_panel --overwrite`
- **AND** `addons/mbe-ui/exports/my_panel.yml` already exists
- **THEN** the system registers a 30-second export session and allows overwriting the file.

### Requirement: In-Game Inventory Capture
The system MUST capture physical block inventories when clicked by a player with an active export session.

#### Scenario: Exporting a chest inventory on right-click
- **WHEN** a player with an active export session right-clicks a block holding an inventory (e.g. Chest or DoubleChest)
- **THEN** the system cancels the interaction event, processes the container contents, and triggers YAML generation.

#### Scenario: Session auto-expiration
- **WHEN** 30 seconds elapse without the player clicking an inventory block
- **THEN** the export session automatically expires and is removed.

### Requirement: Panel YAML Generation & Storage
The system MUST serialize captured inventory metadata into a valid `mbe-ui` panel configuration YAML file.

#### Scenario: Generating sequential items and layout mapping
- **WHEN** the container inventory is processed
- **THEN** each non-air slot `N` is mapped to key `item_N` in `items` and referenced as `N: item_N` in `layout`.

#### Scenario: Asynchronous YAML file storage
- **WHEN** the panel model is constructed
- **THEN** the YAML file is saved asynchronously to `addons/mbe-ui/exports/<panel_id>.yml` and can be successfully re-parsed by `UiPanelYamlParser`.
