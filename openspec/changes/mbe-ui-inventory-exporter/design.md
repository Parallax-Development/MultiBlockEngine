# Design Document: In-Game Inventory Panel Exporter for `mbe-ui`

## Context & Architecture

`mbe-ui` is a decoupled UI addon in MultiBlockEngine responsible for loading, parsing, and rendering UI panels defined in YAML files. The goal of this change is to introduce an in-game panel exporter that captures physical block inventories (Chests, DoubleChests, Barrels) and converts them into valid `mbe-ui` inventory panel YAML definitions.

## Key Service Components

```
┌────────────────────────────────────────────────────────┐
│                   UiCommandService                     │
│           (/mbeui exportpanel <id> [-f])               │
└───────────────────────┬────────────────────────────────┘
                        │ Start Session
                        ▼
┌────────────────────────────────────────────────────────┐
│               UiExportSessionService                   │
│   - Map<UUID, ExportSession>                           │
│   - Expire after 30s                                   │
│   - Pre-check file existence / overwrite flag          │
└───────────────────────┬────────────────────────────────┘
                        │ Right Click Block Event
                        ▼
┌────────────────────────────────────────────────────────┐
│               UiExportBlockListener                    │
│   - Intercepts RIGHT_CLICK_BLOCK on InventoryHolder    │
│   - Cancels interaction event                          │
│   - Invokes Exporter Service                           │
└───────────────────────┬────────────────────────────────┘
                        │ Pass Inventory
                        ▼
┌────────────────────────────────────────────────────────┐
│               UiPanelExporterService                   │
│   - Resolves rows (1 to 6) & DoubleChest support       │
│   - Extracts item material, name, lore, CMD, etc.      │
│   - Builds InventoryPanelDefinition                    │
└───────────────────────┬────────────────────────────────┘
                        │ Async Save
                        ▼
┌────────────────────────────────────────────────────────┐
│               UiPanelYamlExporter                      │
│   - Serializes InventoryPanelDefinition to YAML        │
│   - Saves to addons/mbe-ui/exports/<panel_id>.yml      │
└────────────────────────────────────────────────────────┘
```

## Detailed Component Specifications

### 1. `ExportSession` Record
```java
public record ExportSession(
    UUID playerUuid,
    String panelId,
    boolean overwrite,
    long createdAtMillis
) {
    public boolean isExpired(long timeoutMillis) {
        return System.currentTimeMillis() - createdAtMillis > timeoutMillis;
    }
}
```

### 2. `UiExportSessionService`
- **Responsibilities**:
  - Validates `panelId` syntax (alphanumeric, underscores, hyphens).
  - Checks if `exports/<panel_id>.yml` exists. If it exists and `overwrite` is `false`, rejects the command request with an error message to the player.
  - Stores player session with a 30-second timeout.
  - Clears sessions upon expiration, completion, or player disconnect.

### 3. `UiExportBlockListener`
- **Responsibilities**:
  - Listens for `PlayerInteractEvent` with `Action.RIGHT_CLICK_BLOCK`.
  - Ignores players without active export sessions.
  - Checks if clicked block state or holder implements `InventoryHolder`.
  - Cancels event (`event.setCancelled(true)`) to prevent chest GUI opening.
  - Calls `UiPanelExporterService.exportInventory(player, holder.getInventory(), session)`.

### 4. `UiPanelExporterService` & `UiPanelYamlExporter`
- **Inventory Resolution**:
  - Inventory size determines `rows`: `rows = ceil(inventory.getSize() / 9.0)`. Restricted between 1 and 6 rows.
  - For each non-air `ItemStack` at slot `index`:
    - Key: `item_<index>` (e.g. `item_0`, `item_13`).
    - Layout: `indexed.put(index, List.of("item_" + index))`.
    - Item Properties:
      - `material`: `itemStack.getType().name()`
      - `name`: Custom display name (formatted) if present.
      - `lore`: Lore string list if present.
      - `stack`: `itemStack.getAmount()`
      - `custom-model-data`: Custom model data int if present (> 0).
      - `enchantments`: List of enchantment names and levels.
  - Title: default set to `panelId`.
  - `inventory-lock`: defaults to `true`.

- **YAML Output Structure**:
```yaml
type: inventory
title: "my_panel_id"
rows: 3
inventory-lock: true

layout:
  0: item_0
  13: item_13

items:
  item_0:
    material: DIAMOND_SWORD
    name: "<gold>Legendary Sword"
    lore:
      - "<gray>Damage: +10"
  item_13:
    material: NETHER_STAR
    name: "<yellow>Energy Core"
```

- **File Output Location**:
  - `addons/mbe-ui/exports/<panel_id>.yml`

## Concurrency & Thread Safety
- Session state mutations run on the main Bukkit thread.
- File serialization and disk write (`UiPanelYamlExporter`) execute on an asynchronous task pool (`CompletableFuture.runAsync(...)`) to prevent main thread stutters.
- Post-export success/error notifications are dispatched back to the main thread for player messaging.

## Verification Plan
- Unit tests for `UiPanelExporterService` and `UiPanelYamlExporter` verifying that exported panel models pass re-parsing via `UiPanelYamlParser.parse(...)`.
- Test session expiration and overwrite prevention logic.
