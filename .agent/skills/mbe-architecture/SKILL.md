---
name: mbe-architecture
description: Explain the global architecture of MultiBlockEngine.
---

## Purpose
Explain the global architecture of MultiBlockEngine.

## Trigger
- Defining the design of a cross-cutting engine feature
- Deciding the distribution of responsibilities between `api`, `core`, and `addons`
- Evaluating a refactor with an impact on public contracts

## Scope
- `api`
- `core`
- `addons`
- Extensibility boundaries
- Runtime orchestration flows

## Non-goals
- Converting `api` into an implementation layer
- Placing business logic in infrastructure for convenience
- Designing addons as rigid, interdependent extensions

## Rules
- `api` is a contract, not an implementation.
- `core` orchestrates; it does not define business rules.
- Prioritize services and events to decouple flows.
- Define integrations through explicit contracts.

## Required checks
- The change maintains a clear separation between contract and implementation.
- No concrete logic is added in API packages.
- The core retains its role as an orchestrator and lifecycle manager.
- Addons remain independent, extensible modules.

## Failure modes
- Breaking compatibility due to implementation leaking into the API.
- Bloated core containing addon responsibilities.
- Difficult evolution due to cross-coupling.
- Fragile flows due to the lack of integration contracts.

## Test checklist
- Validation of compatibility for the affected public contract.
- Validation of service initialization per layer.
- Validation of extension via an addon without modifying the core.
- Validation of domain events for decoupled integration.

## Implementation checklist
- Model the API contract before implementing the runtime.
- Place orchestration in the core and specialization in addons.
- Expose hooks/events for extensions instead of direct accesses.
- Review dependencies to avoid crossing layer boundaries.

## Example: do vs avoid
- **Do**: Define an interface in the API and publish an event for addon extension.
- **Avoid**: Add concrete assembly logic inside `api`.

## Patterns
- Event-driven architecture
- API-first design
- SOA (Service-Oriented Architecture)

## Anti-patterns
- Core converted into a God module
- Logic inside `api`
- Coupling between addons
