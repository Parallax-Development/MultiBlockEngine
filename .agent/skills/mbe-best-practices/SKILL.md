---
name: mbe-best-practices
description: Project best practices.
---

## Purpose
Project best practices.

## Trigger
- Implementing a new feature in any MBE module
- Executing maintainability or performance refactors
- Reviewing architectural quality in PRs

## Scope
- Service design
- Event-driven flows
- Modularity between core and addons

## Non-goals
- Forcing over-engineering on small changes
- Ignoring performance impact in the name of architectural purity
- Duplicating existing rules without providing operational criteria

## Rules
- Prefer services.
- Use events.
- Maintain modularity.
- Design the contract first, then the implementation.
- Delegate infrastructure outside of domain logic.

## Required checks
- Core business logic lives in services.
- Events are used to decouple, not to hide complexity.
- There are no direct dependencies between addons.
- The flow does not introduce main thread blocking.

## Failure modes
- Design degradation due to scattered logic.
- Accidental coupling that breaks extensibility.
- Duplication of business rules across multiple components.
- Unstable performance due to incorrect threading decisions.

## Test checklist
- Verification of domain/infrastructure separation.
- Verification of error handling coverage in services.
- Verification of async/sync flows in expensive operations.
- Verification of compatibility with the addon architecture.

## Implementation checklist
- Define the domain objective of the feature.
- Model the main service and associated events.
- Integrate with existing contracts without skipping layers.
- Validate performance and stability impact.

## Example: do vs avoid
- **Do**: The main service orchestrates, events notify, and listeners react.
- **Avoid**: Commands or listeners execute the complete business logic.
