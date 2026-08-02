# Design: Dynamic Cable Connectivity & Multiblock Coupling (`mbe-wiring-multiblock-coupling`)

## Architectural Overview

```
 ┌─────────────────────────────────────────────────────────────────────────────┐
 │                             NetworkService                                  │
 └─────────────────────────────────────────────────────────────────────────────┘
                                        │
           ┌────────────────────────────┴────────────────────────────┐
           ▼                                                         ▼
┌───────────────────────┐                               ┌───────────────────────┐
│ WireConnectionService │                               │ MultiblockWiringBridge│
│ (Evaluates Predicates)│                               │ (Lifecycle Listener)  │
└───────────────────────┘                               └───────────────────────┘
           │                                                         │
           ▼                                                         ▼
┌───────────────────────┐                               ┌───────────────────────┐
│ ConnectionPredicate   │                               │ PortResolutionService │
│ - Match NetworkType   │                               │ - Resolve IOPorts     │
│ - Check Faces & Pos   │                               │ - Register Nodes      │
└───────────────────────┘                               └───────────────────────┘
           │                                                         │
           ▼                                                         ▼
┌───────────────────────┐                               ┌───────────────────────┐
│ Visual-Topological    │                               │ Dynamic Graph Update  │
│ 1:1 State Sync        │                               │ (NetworkGraph BFS)    │
└───────────────────────┘                               └───────────────────────┘
```

## Core Decisions & Principles

1. **Strict 1-Block 1-Function Rule**:
   - Each cable node position belongs to exactly one `NetworkType`. No multi-channel hybrid node instances in a single block pos.

2. **Strict Port Resolution for Multiblocks**:
   - Connectability evaluation queries `PortResolutionService`.
   - Cable connections target ONLY valid `IOPort` / `PortDefinition` locations declared on a `MultiblockInstance`. Casing or decorative blocks return `false` on connection evaluations.

3. **Visual-Topological Alignment (1:1)**:
   - Visual state (e.g. `BlockDisplay` model extensions) is derived directly from topological connections in `NetworkService`. If `NetworkService.connect(...)` is false or invalid, no visual connection displays.

4. **Event-Driven Lifecycle Coupling**:
   - `MultiblockFormEvent`: Triggers registration of resolved `IOPort` nodes in `NetworkService` and prompts neighboring cable nodes to evaluate connection predicates.
   - `MultiblockBreakEvent`: Unregisters `IOPort` nodes, tearing down graph connections and triggering topological graph recomputations (`recomputeNetworks`).
