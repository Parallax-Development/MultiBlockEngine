---
name: mbe-wiring
description: Network and wiring system.
---

## Purpose
Network and connections system.

## Trigger
- Connect/disconnect network nodes between multiblocks.
- Recalculate topology after structural changes.
- Route energy/signals using the wiring graph.

## Scope
- NetworkGraph
- NetworkNode
- Connections
- Network rebuild/repath
- Topological state synchronization

## Non-goals
- Mutate the graph from components outside the service.
- Maintain dangling references between removed nodes.
- Execute full recomputation on every micro-change without a strategy.

## Rules
- Do not modify the graph directly.
- Every mutation must preserve graph invariants.
- Recalculate routes in a controlled and measurable way.

## Required checks
- No direct write access to `NetworkGraph` outside the service.
- Each connection validates node existence and compatibility.
- Connections are cleaned up when nodes are removed.
- Heavy recomputations are scheduled to avoid tick impact.

## Failure modes
- Corrupted graph due to orphaned links or invalid cycles.
- Desynchronization between topology and block state.
- Lag caused by excessive full recomputations.
- Incorrect transfer/routing due to stale caches.

## Test checklist
- Node/connection addition and removal with invariants kept intact.
- Network rebuild after multiple structural changes.
- Expected route validation in representative topologies.
- Performance verification on medium/large graphs.

## Implementation checklist
- Encapsulate graph operations within `NetworkService`.
- Define connection and disconnection validations.
- Implement an incremental recomputation strategy when applicable.
- Expose metrics for network size and recomputation cost.

## Example: do vs avoid
- **Do**: Node change triggers an operation in `NetworkService` and a controlled recalculation.
- **Avoid**: Listener manually edits internal node/connection lists.

## Patterns
- Graph-based systems
- Incremental recomputation
- Topology management
- Use `NetworkService`

## Anti-patterns
- Direct node manipulation.
- Full graph reconstruction without criteria.
