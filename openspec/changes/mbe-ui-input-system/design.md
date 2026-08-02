# Design Document: Advanced Input System for `mbe-ui`

## Context & System Architecture

The `mbe-ui-input-system` expands `mbe-ui` by introducing three input channels managed via a central input orchestration service (`UiInputSessionManager`).

```
 ┌───────────────────────────────────────────────────────────────┐
 │                     UiInputSessionManager                     │
 └───────────────────────────────┬───────────────────────────────┘
                                 │
         ┌───────────────────────┼────────────────────────┐
         ▼                       ▼                        ▼
┌──────────────────┐    ┌─────────────────┐    ┌──────────────────────┐
│  UiItemInput     │    │  UiChatPrompt   │    │ UiPacketInput        │
│  Handler         │    │  Service        │    │ Service              │
│  (Inventory      │    │  (Bukkit Chat   │    │ (PacketEvents        │
│   Deposit Slots) │    │   Listener)     │    │  Anvil / Sign GUIs)  │
└────────┬─────────┘    └────────┬────────┘    └──────────┬───────────┘
         │                       │                        │
         └───────────────────────┼────────────────────────┘
                                 ▼
              ┌─────────────────────────────────────┐
              │      Session Variable Injection     │
              │  {{input.<id>.material}}, {{var}}   │
              └─────────────────────────────────────┘
```

## Detailed Component Design

### 1. Item Deposit Slots (`UiItemInputHandler`)
- **YAML Schema**:
  ```yaml
  items:
    fuel_slot:
      type: input
      allowed-materials: [COAL, CHARCOAL]
      placeholder:
        material: BARRIER
        name: "<red>Deposit Coal"
      on-deposit:
        commands: ["mbe ui refresh"]
      on-withdraw:
        commands: ["mbe ui refresh"]
  ```
- **Runtime Rules**:
  - `UiInventoryRuntime` leaves slots marked `type: input` unlocked.
  - On `InventoryClickEvent` / `InventoryDragEvent`, validates against `allowed-materials`.
  - When an item is placed, stores `{{input.<key>.material}}`, `{{input.<key>.amount}}`, `{{input.<key>.name}}` in the session's variable map.
  - On `InventoryCloseEvent`, returns items remaining in `type: input` slots to player inventory (or drops them if inventory is full).

### 2. Chat Prompt Service (`UiChatPromptService`)
- **YAML Schema**:
  ```yaml
  left-click:
    prompt:
      message: "<green>Type new name in chat (or 'cancel'):"
      timeout: 30
      variable: "structure_name"
      on-submit:
        commands: ["mbe ui open my_panel"]
      on-cancel:
        commands: ["mbe ui open my_panel"]
  ```
- **Runtime Rules**:
  - Temporarily suspends the player's panel session and stores an active prompt token in `UiChatPromptService`.
  - Intercepts `AsyncPlayerChatEvent`. If message equals `"cancel"`, triggers `on-cancel`. Otherwise, binds text to variable key and triggers `on-submit`.

### 3. PacketEvents Provider (`UiPacketInputService`)
- **PacketEvents Integration**:
  - Listens for `WrapperPlayClientNameItem` (Anvil text modification) and `WrapperPlayClientUpdateSign` (Sign editor submission).
  - Handles virtual Anvil inventory creation via PacketEvents / Bukkit Anvil inventory holder.
  - Fallback mechanism: if PacketEvents is absent at runtime, automatically routes text prompts through `UiChatPromptService`.

## Concurrency & Thread Safety
- PacketEvents listeners handle incoming packet payloads asynchronously; variable mutation and panel re-open commands are scheduled back to the Bukkit main thread via BukkitScheduler.
