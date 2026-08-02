# Proposal: Dynamic Cable Connectivity & Multiblock Coupling (`mbe-wiring-multiblock-coupling`)

## Context & Motivation
Currently, `mbe-wiring` manages basic network topologies through `NetworkService` and `NetworkNode`. However, there is no standardized predicate system to evaluate whether a cable block dynamically connects (both visually and topologically) to adjacent blocks or multiblock IO ports. Furthermore, multiblocks assembled via MBE need seamless coupling into active networks, allowing multiblock machines to register as network endpoints dynamically upon assembly and unregister upon structure breakdown.

## Proposed Solution
1. **Cable Connection Predicates (`ConnectionPredicate` / `WireConnectionEvaluator`)**:
   - Evaluate connectivity for cable blocks based on:
     - Single `NetworkType` per cable block (strictly following the 1-block 1-function rule).
     - Directional face compatibility (`Direction`).
     - Target node validation (only blocks registered as valid network nodes or formal multiblock IO ports).
2. **Multiblock IO Port Coupling**:
   - Restrict cable connections strictly to blocks formally defined as `IOPort` / `PortDefinition` within a `MultiblockInstance`. Decorative or casing blocks reject all cable connections.
   - Synchronize visual rendering (BlockDisplay models) strictly with topological graph connection status (1:1 visual-topological alignment).
3. **Dynamic Topology Lifecycle Integration**:
   - `MultiblockFormEvent`: Resolves `IOPort`s via `PortResolutionService`, registers them into `NetworkService`, and triggers adjacent cable re-evaluations.
   - `MultiblockBreakEvent`: Unregisters structure `IOPort`s, removes network graph edges, and triggers graph component splits (`SplitNetwork`) as necessary.

## Scope
- `api`: Extend/refine contracts for wiring connection evaluation and IO port resolution where appropriate without introducing concrete logic into `api`.
- `core`: Implement connection evaluation services, multiblock lifecycle listeners for wiring, and visual-topological synchronization state handlers.

## Non-Goals
- Multi-channel or hybrid network cables occupying a single block position.
- Cable connections to non-port decorative or casing multiblock components.
- Direct graph mutation outside of `NetworkService`.
