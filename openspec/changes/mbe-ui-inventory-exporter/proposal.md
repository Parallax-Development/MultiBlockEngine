# Proposal: In-Game Inventory Panel Exporter for `mbe-ui`

## Summary
Add an in-game panel export command `/mbeui exportpanel <panel_id> [--overwrite | -f]` to `mbe-ui`. This command allows authorized players to initiate an export session and click on any block inventory (such as single/double chests or barrels) in the world to convert its structure, items, names, lores, and item metadata into a clean `mbe-ui` panel configuration YAML file saved at `plugins/MultiBlockEngine/addons/mbe-ui/exports/<panel_id>.yml`.

## Motivation
Creating `mbe-ui` panel YAML definitions by hand can be tedious and prone to syntax errors. Server admins and developers frequently design panel interfaces in-game using chests and decorative items. An automated export tool streamlines UI design by capturing in-game inventory contents and converting them directly into validated, ready-to-use `mbe-ui` panel configurations.

## Proposed Changes
1. **Export Session Service (`UiExportSessionService`)**:
   - Manages active player export sessions in memory (`Map<UUID, ExportSession>`) with a 30-second idle expiration.
   - Handles file existence checking prior to session registration: prevents accidental file overwrites unless `--overwrite` (or `-f`) flag is supplied.

2. **Block Interaction Listener (`UiExportBlockListener`)**:
   - Intercepts `PlayerInteractEvent` (RIGHT_CLICK_BLOCK).
   - If the player has an active export session and clicks a block with an `InventoryHolder` (e.g. Chest, DoubleChest, Barrel):
     - Cancels interaction event to prevent normal inventory opening.
     - Hands the container inventory off to `UiPanelExporterService`.
     - Clears the player session and sends feedback.

3. **Panel Exporter Engine (`UiPanelExporterService`) & YAML Dumper (`UiPanelYamlExporter`)**:
   - Inspects inventory size (1 to 6 rows).
   - Maps non-air slots to sequential item keys (`item_0`, `item_13`, etc.) following index-based mapping (Option B).
   - Extracts item metadata: material, display name (MiniMessage/legacy format), lore, stack count, enchantments, custom model data, and flags.
   - Builds `UiPanelModel.InventoryPanelDefinition` with `title` set to `<panel_id>` by default.
   - Formats and writes YAML output asynchronously to `plugins/MultiBlockEngine/addons/mbe-ui/exports/<panel_id>.yml`.

4. **Command Registration (`UiCommandService`)**:
   - Register subcommand `exportpanel <panel_id> [--overwrite|-f]` (and alias `/mbe ui export`).

## Design & Architecture Alignment
- Strictly adheres to `AGENTS.md` and `mbe-ui` skill principles: UI handlers contain no business logic, delegating execution entirely to service abstractions (`UiExportSessionService`, `UiPanelExporterService`).
- Disk I/O operations are offloaded asynchronously to avoid blocking the Bukkit main thread.

## Impact & Safety
- Non-breaking change: adds new subcommand and services without modifying existing panel parsing or rendering contracts.
- Overwrite protection guarantees existing exported panels are not replaced accidentally.
