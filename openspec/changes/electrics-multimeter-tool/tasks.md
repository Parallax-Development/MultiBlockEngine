## 1. Item Registration

- [x] 1.1 Create `MultimeterItemDefinition` implementing `ItemDefinition` with key `mbe-electrics:multimeter`, material `CLOCK`, `customModelData = 200`, and `unstackable = true`
- [x] 1.2 Register `MultimeterItemDefinition` in `ElectricsAddon.onEnable()` via `ItemRegistry` alongside the existing crafting component items

## 2. Tool Action Constants

- [x] 2.1 Create `MultimeterActions` class with `ActionId` constants: `INSPECT` (ns=`mbe-electrics`, name=`inspect_network`) and `RESET_SELECTION` (ns=`mbe-electrics`, name=`reset_multimeter`)

## 3. Tool Mode

- [x] 3.1 Create `InspectMode` implementing `ToolMode` with id `inspect`, binding `RIGHT_CLICK → MultimeterActions.INSPECT` and `SHIFT_RIGHT_CLICK → MultimeterActions.RESET_SELECTION`

## 4. Tool Definition

- [x] 4.1 Create `MultimeterTool` implementing `Tool` with id `multimeter`, containing a single mode: `InspectMode`

## 5. Session Service

- [x] 5.1 Create `MultimeterSessionService` with a `ConcurrentHashMap<UUID, PendingSelection>` where `PendingSelection` is a simple record/class holding a `NetworkNode` and its `BlockPos`
- [x] 5.2 Add `store(UUID, NetworkNode)`, `get(UUID) → Optional<PendingSelection>`, and `clear(UUID)` methods
- [x] 5.3 Implement `Listener` in `MultimeterSessionService` (or a companion listener) to call `clear(uuid)` on `PlayerQuitEvent`

## 6. BFS Path Resolution (inside InspectNetworkAction)

- [x] 6.1 Implement a private `bfsPath(NetworkNode from, NetworkNode to, NetworkGraph graph) → List<NetworkNode>` method that performs BFS over `NetworkGraph.connections()` using `NetworkConnection.from()` / `NetworkConnection.to()`
- [x] 6.2 Handle the "no path" case (BFS exhausted with no route to `to`) by returning an empty list

## 7. Inspect Network Action

- [x] 7.1 Create `InspectNetworkAction` implementing `ToolAction` with id `MultimeterActions.INSPECT`
- [x] 7.2 Inject `NetworkService`, `ElectricsService`, and `MultimeterSessionService` via constructor
- [x] 7.3 In `execute(WrenchContext)`: if `clickedBlock()` is null, return `WrenchResult.pass()`
- [x] 7.4 Resolve nodes at `clickedBlock` via `NetworkService.findAllNodes(block)`; if empty, send "no node found" feedback and return `WrenchResult.pass()`
- [x] 7.5 **Phase 1** (no pending selection): store the first resolved node in `MultimeterSessionService`, send "first endpoint selected" feedback, return `WrenchResult.success(...)`
- [x] 7.6 **Phase 2** (pending selection exists): retrieve pending node A; resolve node B from the clicked block; resolve graph for each; check they share the same graph (compare `NetworkGraph.id()`)
- [x] 7.7 If different graphs: send "different networks" message, clear session, return `WrenchResult.pass()`
- [x] 7.8 Run `bfsPath(A, B, graph)`; if empty path: send "no circuit" message, clear session, return `WrenchResult.pass()`
- [x] 7.9 Collect energy data: for each node on path, call `ElectricsService.getInstance(blockLocation)` and read `energy` / `max_energy` variables
- [x] 7.10 Invoke `ChatNetworkRenderer.render(player, path, energyData)`, clear session, return `WrenchResult.success(...)`

## 8. Reset Selection Action

- [x] 8.1 Create `ResetSelectionAction` implementing `ToolAction` with id `MultimeterActions.RESET_SELECTION`
- [x] 8.2 Inject `MultimeterSessionService` via constructor
- [x] 8.3 In `execute(WrenchContext)`: call `session.clear(player.getUniqueId())`; if a selection was present send "Selection cancelled", otherwise return `WrenchResult.pass()`

## 9. Chat Renderer

- [x] 9.1 Create `ChatNetworkRenderer` with a `render(Player, List<NetworkNode>, Map<UUID, EnergyReading>)` method (where `EnergyReading` is a local record `{ long energy, long maxEnergy }`)
- [x] 9.2 Format header: `§e--- [Multímetro] Red §6<networkType>§e (<4-char uuid prefix>) ---`
- [x] 9.3 Format per node: `§7● §f(<x>, <y>, <z>)` and if energy data exists: `  §a<energy>§7/§a<maxEnergy> §7FE`
- [x] 9.4 Label cable nodes (no energy data) with `§7[Cable]`; label multiblock nodes with `§b[<type id short>]`

## 10. Addon Wiring (ElectricsAddon.onEnable())

- [x] 10.1 Obtain `ToolRegistry`, `ToolModeRegistry`, `ToolActionRegistry` from `AddonContext`
- [x] 10.2 Instantiate `MultimeterSessionService`, register it as a service via `context.registerService()`
- [x] 10.3 Register the session listener via `context.registerListener()` for `PlayerQuitEvent` cleanup
- [x] 10.4 Register `MultimeterTool` in `ToolRegistry`
- [x] 10.5 Register `InspectMode` in `ToolModeRegistry`
- [x] 10.6 Instantiate and register `InspectNetworkAction` (with `NetworkService`, `ElectricsService`, `MultimeterSessionService`) in `ToolActionRegistry`
- [x] 10.7 Instantiate and register `ResetSelectionAction` (with `MultimeterSessionService`) in `ToolActionRegistry`
