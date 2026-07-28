# Tasks: Dynamic Cable Connectivity & Multiblock Coupling (`mbe-wiring-multiblock-coupling`)

## 1. Cable Connection Predicates (`api` & `core`)
- [x] 1.1 Define `ConnectionPredicate` interface or contract in `api/wiring`.
- [x] 1.2 Implement `WireConnectionEvaluator` in `core` validating directional face compatibility and matching `NetworkType`.
- [x] 1.3 Ensure visual state synchronization (BlockDisplay models) strictly reflects 1:1 topological connection status.

## 2. Multiblock IO Port Integration (`core`)
- [x] 2.1 Integrate `PortResolutionService` with `WireConnectionEvaluator` to validate target multiblock blocks.
- [x] 2.2 Reject connection attempts targeting casing or decorative multiblock blocks.

## 3. Dynamic Topology Lifecycle Bridge (`core`)
- [x] 3.1 Implement `MultiblockWiringBridge` listener on `MultiblockFormEvent` to auto-register structure `IOPort`s into `NetworkService`.
- [x] 3.2 Implement `MultiblockWiringBridge` listener on `MultiblockBreakEvent` to auto-unregister structure `IOPort`s and trigger graph topology recomputation.

## 4. Verification & Testing
- [x] 4.1 Unit tests for `WireConnectionEvaluator` face matching, type matching, and port resolution.
- [x] 4.2 Unit tests for `MultiblockFormEvent` / `MultiblockBreakEvent` topological graph updates.
