# Proposal: Advanced Input System for `mbe-ui` (`mbe-ui-input-system`)

## Summary
Add a comprehensive, multi-modal input system to `mbe-ui` that enables panel configurations to capture player inputs. The system supports three core input mechanisms:
1. **Item Deposit Slots**: Declarative inventory slots (`type: input`) allowing players to place or withdraw items with optional material filters, placeholders, and callbacks (`on-deposit`, `on-withdraw`).
2. **Chat Prompts**: Temporary chat listeners (`AsyncPlayerChatEvent`) that capture player text input, store it in session variables, and execute submission/cancellation actions.
3. **PacketEvents Anvil & Sign GUIs**: High-performance, NMS-free packet interfaces powered by PacketEvents (`WrapperPlayClientNameItem`, `WrapperPlayClientUpdateSign`) to open virtual Anvil/Sign input GUIs for in-game text entry with automatic fallback to Chat Prompts if PacketEvents is unavailable.

## Motivation
`mbe-ui` panels currently operate strictly as menu/view displays. Interactive features (such as renaming multiblock structures, depositing machine fuel, setting prices, or entering numeric amounts) require external commands or custom code. Introducing a unified, declarative input system empowers developers to build rich, interactive UI flows directly from YAML panel definitions without writing business logic in listener classes.

## Proposed Changes

### 1. Item Deposit Slots in `UiInventoryRuntime`
- Extend `UiPanelModel.ItemDefinition` with `type: input`, `allowed-materials`, `placeholder`, `on-deposit`, and `on-withdraw`.
- Un-lock specified input slots during `inventory-lock: true`.
- Track deposited item stacks and update panel session variables (e.g. `{{input.<slot_id>.material}}`, `{{input.<slot_id>.amount}}`).
- Safely handle inventory close events to return deposited items to the player's inventory or drop them at the player's location to prevent item loss or duplication.

### 2. Chat Prompt Engine (`UiChatInputService`)
- Register a dedicated chat prompt service listening for `AsyncPlayerChatEvent`.
- Support configurable timeout (e.g. 30 seconds), prompt messages, variable binding, and `on-submit` / `on-cancel` callbacks.

### 3. PacketEvents Provider (`UiPacketInputService`)
- Integrate PacketEvents wrapper service for packet-based Anvil and Sign editor GUIs.
- Capture client rename packets (`WrapperPlayClientNameItem`) and sign text updates (`WrapperPlayClientUpdateSign`).
- Provide safe fallback to Chat Prompts when PacketEvents is not present in the server environment.

## Design & Architecture Alignment
- Complies strictly with MBE architectural guidelines: decoupled service contracts, event-driven flow, no business logic in listeners.
- Purely declarative YAML syntax parsing in `UiPanelYamlParser`.
