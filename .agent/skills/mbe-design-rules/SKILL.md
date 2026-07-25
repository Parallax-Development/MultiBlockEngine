---
name: mbe-design-rules
description: Fundamental design rules.
---

## Purpose
Fundamental design rules.

## Trigger
- Designing the contract for a new feature
- Making structural decisions between modules
- Evaluating whether a change respects the MBE philosophy

## Scope
- API-first
- Decoupling between layers and addons
- Extensibility model

## Non-goals
- Turning design rules into inflexible constraints
- Prioritizing quick implementation that breaks contracts
- Coupling components to resolve immediate urgencies

## Rules
- API-first always.
- Strong decoupling.
- Addons act as extensions.
- Core acts as an orchestrator, not a container for all features.
- Explicit contracts must precede cross-cutting integration.

## Required checks
- The public contract is defined before implementing details.
- There is no concrete logic in API layers.
- The feature can be extended from an addon without modifying the core.
- The design avoids circular dependencies and layer crossings.

## Failure modes
- Unstable API due to impulsive implementation changes.
- Bloated core that competes with addons.
- Ecosystem that is difficult to extend due to a lack of clear contracts.
- High maintenance cost due to structural coupling.

## Test checklist
- Verification of public contract compatibility.
- Verification of extension via an example addon.
- Verification of the absence of cross-coupling.
- Verification of clear responsibilities per module.

## Implementation checklist
- Define the contract in the API and anticipate extension cases.
- Implement orchestration in the core using services/events.
- Publish integration points for addons.
- Review dependencies and eliminate implementation shortcuts.

## Example: do vs avoid
- **Do**: Feature defined by an interface + runtime implementation + extension via an addon.
- **Avoid**: Feature hardcoded in the core without an extensible contract.
