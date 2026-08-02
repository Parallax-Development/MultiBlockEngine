## Context

`mbe-electrics` is an addon for MultiBlockEngine that models electrical networks on top of `mbe-wiring`'s `NetworkService`. It registers producers, consumers, and storage nodes into energy graphs, but provides no in-game diagnostic surface. The wiring layer already exposes a `DebugWiringAction` that visualises cable topology via particles, but nothing reads or displays live energy data. The codebase has mature infrastructure for tool-based interactions (`Tool` / `ToolMode` / `ToolAction`) and a service-level inspection pipeline (`Inspectable`, `InspectionPipelineService`, `InspectionRenderer`) that is ready to be reused.

## Goals / Non-Goals

**Goals:**
- Introduce `mbe-electrics:multimeter` as a first-class MBE Tool Item, following the same pattern as the Wire Cutter in `mbe-wiring`.
- Implement a single **Inspect** `ToolMode` with a two-click closed-circuit interaction: select endpoint A, select endpoint B, resolve the BFS path, report topology + energy data via chat.
- Allow `SHIFT+RIGHT_CLICK` to cancel/reset the pending selection at any point.
- Keep all new code inside the `mbe-electrics` addon; no changes to `api` or `core`.

**Non-Goals:**
- GUI / inventory panel rendering (deferred to future change).
- Multi-mode cycling (only one mode for now).
- Persistence of multimeter sessions across restarts.
- Support for non-energy network types in the Inspect action (data/signal cables are out of scope for this change).

## Decisions

### D1 — Reuse the existing Tool / ToolMode / ToolAction pattern

**Decision**: Model the multimeter exactly like `WireCutterTool` — a `Tool` with one `ToolMode`, actions registered in `ToolActionRegistry`.

**Alternatives considered**:
- Custom Bukkit listener for item interact: rejected; violates the MBE rule that no logic lives in listeners.
- Extending `WireCutterTool` with extra modes: rejected; the multimeter is conceptually a different tool with a different namespace.

**Rationale**: Zero friction with the existing dispatch pipeline. `ToolModeExecutionService` already routes `WrenchContext` → `ActionId` → `ToolAction`; we simply add our entries.

---

### D2 — Two-click session via `MultimeterSessionService`

**Decision**: Maintain a per-player `Map<UUID, PendingSelection>` inside a dedicated `MultimeterSessionService`. The `InspectNetworkAction` is stateful but thread-safe (ConcurrentHashMap).

```
PendingSelection {
    NetworkNode node;
    BlockPos    pos;    // for display in feedback messages
}
```

**Alternatives considered**:
- Item NBT / PDC to store the first selection: rejected; requires Bukkit API in a domain action, breaks decoupling.
- Re-using `WireCutterSessionService`: rejected; that service stores `NetworkNode` pairs for disconnect/split; sharing state would create hidden coupling.

**Rationale**: Clean service ownership. The session service is injected into both `InspectNetworkAction` and `ResetSelectionAction`, keeping them stateless with respect to the service itself.

---

### D3 — BFS path-finding over `NetworkGraph.connections()`

**Decision**: Implement a BFS from node A to node B using `NetworkGraph.connections()` as the adjacency list. Return the ordered list of `NetworkNode` objects on the shortest path.

```
BFS(A, B, graph):
  if getGraph(A) != getGraph(B) → no circuit (different networks)
  queue = [(A, [A])]
  visited = {A}
  while queue not empty:
    (current, path) = dequeue
    if current == B → return path
    for each connection incident to current:
      neighbour = other end
      if neighbour not in visited:
        enqueue (neighbour, path + neighbour)
        visited.add(neighbour)
  return empty  → no path (disconnected graph)
```

**Alternatives considered**:
- Report all nodes in the shared graph (not just the path): rejected; the user explicitly wants the multimeter to only account for nodes the circuit actually passes through.
- Dijkstra / weighted path: rejected; edges in this network are unweighted (all cables are equal); BFS gives shortest path in O(V+E).

**Rationale**: Correct semantics for "closed-circuit" metaphor. Nodes not on the path are ignored, matching how a real multimeter works when probing two terminals.

---

### D4 — Energy data sourced from `ElectricsService`

**Decision**: For each `NetworkNode` on the path, attempt `ElectricsService.getInstance(blockLocation)` to read `energy` and `max_energy` variables. Nodes with no instance entry (i.e., cables, not multiblocks) are shown as topology-only entries.

**Rationale**: `ElectricsService` already holds the canonical mapping from block location to `MultiblockInstance`. No new data store is needed.

---

### D5 — Chat rendering via `ChatNetworkRenderer`

**Decision**: Implement `InspectionRenderer` as `ChatNetworkRenderer`. It formats one header line and one line per path node, sent with `player.sendMessage()` using legacy `§` colour codes (consistent with the rest of the codebase).

**Alternatives considered**:
- Adventure components / MiniMessage: not yet standardized in this codebase; deferred.
- Action bar: too little space for multi-node readouts.

**Rationale**: Simplest approach that works today. The renderer is behind the `InspectionRenderer` interface, so it can be swapped later with no changes to the action.

---

### D6 — Item material: `CLOCK`

**Decision**: Base material `CLOCK`, `customModelData = 200`, `unstackable = true`.

**Rationale**: `CLOCK` is non-placeable, visually distinct, and evokes measurement. CMD 200 is well clear of the wiring cables (1–2). Resource packs can override the texture.

## Risks / Trade-offs

| Risk | Mitigation |
|------|-----------|
| BFS on large networks may be slow on main thread | Networks are bounded by cable count (practical limit ~500 nodes). BFS at O(V+E) is negligible at this scale. |
| Session leak if player disconnects mid-selection | `PlayerQuitEvent` listener in `MultimeterSessionService.onDisable()` clears all sessions; or handle in the listener registered via `context.registerListener()`. |
| `NetworkGraph` may not expose connections in a traversable form | Already confirmed: `NetworkGraph.connections()` returns `Collection<NetworkConnection>` with `from()` and `to()` giving `NetworkNode` handles. BFS is directly implementable. |
| Two nodes in the same graph but with no path (disconnected sub-components) | BFS returns empty → render "No circuit between selected points." |

## Open Questions

- None blocking implementation. Future enhancement: swap `ChatNetworkRenderer` for a rich Adventure-based renderer once a standard is adopted.
