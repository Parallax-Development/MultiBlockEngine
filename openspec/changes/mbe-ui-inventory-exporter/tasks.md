# Implementation Tasks: `mbe-ui-inventory-exporter`

## 1. Export Session & Command Foundation
- [x] 1.1 Create `ExportSession` record and `UiExportSessionService` in `dev.darkblade.mbe.addon.ui.runtime`.
- [x] 1.2 Implement session registration, expiration timeout (30s), cleanup, and file existence checking logic.
- [x] 1.3 Register `exportpanel <panel_id> [--overwrite|-f]` subcommand in `UiCommandService`.

## 2. Block Listener & Inventory Capture
- [x] 2.1 Create `UiExportBlockListener` in `dev.darkblade.mbe.addon.ui.runtime`.
- [x] 2.2 Listen for `PlayerInteractEvent` (RIGHT_CLICK_BLOCK), verify active session, cancel event, and delegate to exporter.
- [x] 2.3 Handle player disconnect cleanup listener to remove active sessions on disconnect.

## 3. Panel Model Conversion & YAML Exporter
- [x] 3.1 Create `UiPanelYamlExporter` to serialize `InventoryPanelDefinition` into valid `mbe-ui` YAML output.
- [x] 3.2 Implement `UiPanelExporterService` to inspect container inventory, build item definitions (`item_0`, `item_13`), extract item meta (material, name, lore, CMD, etc.), and construct `InventoryPanelDefinition`.
- [x] 3.3 Ensure disk I/O saving (`addons/mbe-ui/exports/<panel_id>.yml`) runs asynchronously.

## 4. Verification & Testing
- [x] 4.1 Unit tests for `UiExportSessionService` verifying session registration, expiration, and overwrite protection.
- [x] 4.2 Unit tests for `UiPanelYamlExporter` verifying exported YAML files re-parse cleanly using `UiPanelYamlParser`.
- [x] 4.3 Full build verification via `./gradlew build`.
