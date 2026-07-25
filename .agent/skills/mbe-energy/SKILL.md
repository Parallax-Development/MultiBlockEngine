---
name: mbe-energy
description: Energy system.
---

## Purpose
Energy system.

## Trigger
- Implement producer/consumer/storage blocks.
- Execute energy transfer between nodes or networks.
- Integrate energy with assembly, UI, or automation.

## Scope
- Producers
- Consumers
- Networks
- Storage
- Energy flow balancing

## Non-goals
- Transfer energy outside system services.
- Hardcode energy rules per block type in listeners.
- Mix energy network logic with rendering/UI.

## Rules
- No hardcoded energy logic.
- Apply consistent contracts for production, consumption, and capacity.
- Maintain network updates and transfers in a controlled flow.

## Required checks
- All transfers pass through official energy services.
- Capacity limits and transfer rates are respected.
- No energy network mutations happen without coordination.
- Energy updates avoid heavy work on the main thread.

## Failure modes
- Energy duplication or loss due to non-atomic calculations.
- Desynchronized networks caused by concurrent mutations.
- Gameplay exploitation due to incomplete validations.
- TPS degradation caused by costly update cycles.

## Test checklist
- Conservative energy balance during transfers.
- Validation of capacity limits and saturation.
- Stable behavior upon node connection/disconnection.
- Performance verification under a large energy network.

## Implementation checklist
- Define producer/consumer/storage contracts.
- Orchestrate transfers from `EnergyService`.
- Synchronize network state before applying transfers.
- Expose metrics for transfers and relevant failures.

## Example: do vs avoid
- **Do**: Producer requests transfer via `EnergyService` with validated limits.
- **Avoid**: Block directly adds/subtracts energy from neighboring entities.

## Patterns
- Flow-based systems
- Graph propagation
- Pull/push transfer strategies
- Use `EnergyService`

## Anti-patterns
- Scattered energy calculation across multiple uncoordinated components.
