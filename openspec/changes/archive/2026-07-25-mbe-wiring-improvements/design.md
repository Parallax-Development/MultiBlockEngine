## Context

The `mbe-wiring` addon currently manages physical connections between cables and networks in memory. However, there are three key areas where it falls short for future extensibility (such as the upcoming `mbe-electrics` addon):
1. State changes from the `wire_cutter` tool (disconnecting two adjacent cables) are not persisted across server restarts.
2. The debug visualization particle system fails to render for `information` network types.
3. Client-side visual connections (rendering of cable models to connect to neighboring blocks) are rigid and need a decoupled way to visually hook into new machines.

## Goals / Non-Goals

**Goals:**
- Persist wire cutter disconnections reliably so networks do not auto-reconnect on load.
- Ensure debug particles correctly query and traverse all network types (energy, information, etc.).
- Introduce a configuration-driven mapping system (`mappings/*.yml`) to define which standard blocks (by namespace) a specific cable type visually connects to.

**Non-Goals:**
- Do not make the mappings system define logical graph connections. Logical connections will remain the responsibility of the multiblock nodes explicitly registering in the `NetworkGraph`.
- Do not refactor the core `NetworkService` graph traversal, only the persistence and visualization layers.

## Decisions

1. **Wire Cutter Persistence**: 
   - *Decision*: Store the disconnected faces in the chunk metadata via `StorageService`. 
   - *Rationale*: It ensures that when the chunk loads and the block assembly attempts to auto-connect to adjacent cables, it skips the explicitly disconnected faces.
2. **Debug Particles Filter**:
   - *Decision*: Remove any hardcoded network-type filters in the particle visualization logic.
   - *Rationale*: The visualizer should just traverse the `NetworkGraph` independent of its type (`energy`, `information`, etc.).
3. **Visual Mappings via Configuration**:
   - *Decision*: Create a `mappings/` directory in the addon folder. Each cable will have a file (e.g., `copper_wire.yml`) containing a list of `namespace:id` block identifiers it can visually connect to.
   - *Rationale*: This decouples the visual rendering of the cable from the actual implementations of the machines. The `BlockDisplay` generation logic will query this map before deciding to stretch the cable model towards a neighboring block.

## Risks / Trade-offs

- **[Risk] Syncing issues between Logical Graph and Visual Mappings** → The mappings are purely visual. If a mapping exists but the multiblock doesn't register a node, the cable will visually connect but not transfer energy. We accept this trade-off for the sake of decoupling; the addon developer is responsible for keeping both aligned.
- **[Risk] Performance cost of checking mappings on chunk load** → Caching the loaded YAML mappings in a highly optimized in-memory lookup table (like an `EnumMap` or `HashSet` per cable type) during plugin startup will mitigate runtime overhead.
