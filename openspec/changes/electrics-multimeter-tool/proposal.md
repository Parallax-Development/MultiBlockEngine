## Why

The `mbe-electrics` addon lacks any in-game diagnostic tool, making it impossible for players or server admins to inspect the state of electrical networks without resorting to commands or external logging. A multimeter item provides an intuitive, item-based mechanism to probe live network topology and energy readings directly in the game world.

## What Changes

- A new item `mbe-electrics:multimeter` is registered via the standard `ItemDefinition` / `ItemRegistry` pipeline.
- The multimeter is a **Tool** (implements `Tool`) with a single `ToolMode` named **Inspect**.
- The Inspect mode uses a two-click "closed-circuit" interaction model:
  - `RIGHT_CLICK` on a first network node → stores the selection in a per-player session.
  - `RIGHT_CLICK` on a second network node → performs a BFS path-find between the two endpoints, collects all nodes on the path, queries topology + energy data, and renders the result to the player's chat.
  - `SHIFT+RIGHT_CLICK` at any time → cancels and clears the current selection.
- A `MultimeterSessionService` manages the pending first-endpoint selection per player UUID.
- A `ChatNetworkRenderer` formats the inspection output as structured chat messages (no external chat library required).
- All wiring follows the established MBE Tool pattern: `Tool` → `ToolMode` → `ToolAction`, registered through `ToolRegistry`, `ToolModeRegistry`, and `ToolActionRegistry`.

## Capabilities

### New Capabilities

- `electrics-multimeter`: The full multimeter item — item registration, Tool wiring, Inspect mode, session service, BFS path resolution, and chat rendering.

### Modified Capabilities

- `electrics-items`: The multimeter (`mbe-electrics:multimeter`) is a new item added to the electrics item registry. The existing spec covers item registration requirements; this change adds one more entry to that registry.

## Impact

- **`mbe-electrics` addon**: New classes under `item/`, `tool/`, and `renderer/` packages.
- **`ElectricsAddon.onEnable()`**: Registration of the item, Tool, ToolMode, ToolAction, and session service.
- **Dependencies (read-only)**: `NetworkService` (from `mbe-wiring`) and `ElectricsService` (local) — already used in `ElectricsAddon`, no new cross-addon coupling introduced.
- **API surface**: No changes to the `api` module. All new code lives in the addon.
