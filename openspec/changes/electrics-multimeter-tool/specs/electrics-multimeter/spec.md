## ADDED Requirements

### Requirement: Multimeter item is registered in the electrics item registry
The system SHALL register an item with key `mbe-electrics:multimeter` in the MBE `ItemRegistry` during `ElectricsAddon.onEnable()`. The item SHALL use material `CLOCK`, `customModelData = 200`, and SHALL be marked as unstackable.

#### Scenario: Item is grantable via command
- **WHEN** the server enables and the `ItemService` is available
- **THEN** the item `mbe-electrics:multimeter` is registered and can be retrieved from the registry

---

### Requirement: Multimeter is a Tool with a single Inspect mode
The system SHALL register `MultimeterTool` in the `ToolRegistry`. The tool SHALL declare a single `ToolMode`: `InspectMode` (id = `inspect`). The `InspectMode` SHALL bind `ActionTrigger.RIGHT_CLICK` to the `INSPECT` action and `ActionTrigger.SHIFT_RIGHT_CLICK` to the `RESET_SELECTION` action.

#### Scenario: Multimeter tool is dispatched on right-click
- **WHEN** a player holding the multimeter item performs a `RIGHT_CLICK` on a block
- **THEN** the `InspectNetworkAction` is executed via the standard `ToolModeExecutionService` dispatch chain

#### Scenario: Multimeter tool cancels selection on shift-right-click
- **WHEN** a player holding the multimeter item performs a `SHIFT_RIGHT_CLICK`
- **THEN** the `ResetSelectionAction` is executed

---

### Requirement: First endpoint selection is stored per player
The system SHALL maintain a per-player pending selection in `MultimeterSessionService` (keyed by player `UUID`). When a player `RIGHT_CLICKs` a block that resolves to at least one `NetworkNode`, the node and its position SHALL be stored as the pending selection. A feedback message SHALL be sent to the player indicating the first endpoint was captured.

#### Scenario: Player clicks a valid network node for the first time
- **WHEN** the player has no pending selection and `RIGHT_CLICKs` a block that is a registered `NetworkNode`
- **THEN** the node is stored as `PendingSelection` and the player receives a confirmation message

#### Scenario: Player clicks a block with no network node
- **WHEN** the player `RIGHT_CLICKs` a block that is not a registered `NetworkNode`
- **THEN** no selection is stored and the player receives an informational message that no network node was found at that location

---

### Requirement: Second endpoint selection triggers circuit evaluation
When a player with a pending first selection `RIGHT_CLICKs` a second block that resolves to a `NetworkNode`, the system SHALL:
1. Determine if both nodes share the same `NetworkGraph`.
2. If they do, run a BFS to find the shortest path between them.
3. Collect all `NetworkNode` objects on that path.
4. For each node on the path, attempt to read energy data from `ElectricsService`.
5. Render the results via `ChatNetworkRenderer`.
6. Clear the pending selection from `MultimeterSessionService`.

#### Scenario: Both endpoints are in the same energy network with a path
- **WHEN** the player selects two nodes connected within the same `NetworkGraph`
- **THEN** the player receives a chat report listing the network ID, all nodes on the path, and energy values for any node that is a multiblock instance

#### Scenario: Both endpoints are in the same graph but no path exists
- **WHEN** the player selects two nodes that are in the same `NetworkGraph` object but have no traversable connection between them
- **THEN** the player receives a message "No circuit between selected points" and the selection is cleared

#### Scenario: Endpoints are in different networks
- **WHEN** the player selects a second node whose `NetworkGraph` differs from the first node's graph (or the first node has no graph)
- **THEN** the player receives a message indicating the nodes are on different networks and the selection is cleared

---

### Requirement: Selection can be cancelled at any time via SHIFT+RIGHT_CLICK
The `ResetSelectionAction` SHALL remove any pending selection for the player from `MultimeterSessionService` and send a cancellation confirmation message. The action SHALL succeed silently if no selection was pending.

#### Scenario: Player cancels an active selection
- **WHEN** the player has a pending selection and performs `SHIFT+RIGHT_CLICK`
- **THEN** the selection is cleared and the player receives a "Selection cancelled" message

#### Scenario: Player cancels with no active selection
- **WHEN** the player has no pending selection and performs `SHIFT+RIGHT_CLICK`
- **THEN** no error occurs and the action returns `WrenchResult.pass()`

---

### Requirement: Player session is cleaned up on disconnect
The `MultimeterSessionService` SHALL remove the pending selection of a player when that player disconnects from the server, to prevent stale session entries.

#### Scenario: Player disconnects mid-selection
- **WHEN** a player with a pending selection disconnects
- **THEN** the session entry for that player's UUID is removed from `MultimeterSessionService`

---

### Requirement: Chat output includes network topology and energy readings
The `ChatNetworkRenderer` SHALL format the inspection result as follows:
- A header line indicating the network type and graph UUID (abbreviated).
- One line per node on the BFS path showing the node's position.
- For nodes that correspond to a `MultiblockInstance` with energy variables, an additional sub-line showing `energy / max_energy` (e.g., `800 / 2000 FE`).
- Nodes with no energy data (cables) SHALL be labelled as "Cable" or "Node".

#### Scenario: Chat report for a path with one producer and one consumer
- **WHEN** the BFS path includes a generator node (has energy variables) and a consumer node (has energy variables)
- **THEN** the chat output shows both nodes with their energy values

#### Scenario: Chat report for a path through cables
- **WHEN** the BFS path includes intermediate cable nodes with no `MultiblockInstance`
- **THEN** cable nodes are shown with their position but without energy values
