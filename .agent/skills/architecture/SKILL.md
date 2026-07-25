---
name: architecture
description: Define global architectural principles.
---

## Purpose
Define global architectural principles.

## Trigger
- Designing a new module or refactoring an existing one
- Making boundary decisions between layers and components
- Evaluating the impact of a new dependency on the engine

## Scope
- Modularity
- Separation of concerns
- Scalability
- Contracts between modules
- Domain and infrastructure boundaries

## Non-goals
- Resolving low-level implementation details
- Prioritizing delivery speed over architectural consistency
- Introducing dependencies that break engine decoupling

## Rules
- Loose coupling, high cohesion
- Interfaces before implementations
- Design the contract first, then the implementation
- Keep the responsibilities of each module explicit and bounded

## Required checks
- The change respects the separation between API, runtime, and addons
- No direct coupling is added between addons
- No concrete logic is introduced in contract layers
- New dependencies have a clear technical justification

## Failure modes
- Eroded architecture due to contract bypasses
- Modules that are difficult to test because of mixed responsibilities
- Evolutionary gridlock caused by cross-cutting coupling
- Inconsistency due to domain rules scattered across multiple layers

## Test checklist
- Verification of dependencies per module without cycles
- Verification of extension via interfaces, not concrete implementations
- Verification of a single orchestration point for critical flows
- Verification of impact on compatibility with existing addons

## Implementation checklist
- Define module boundaries and public contracts
- Model expected extension cases
- Implement infrastructure adapters outside the domain
- Validate integration without breaking decoupling

## Example: do vs avoid
- Do: expose a service interface in the API and resolve the implementation in the runtime
- Avoid: consuming concrete classes from another module to bypass the contract
- Avoid circular dependencies

## Patterns
- API-first design
- Hexagonal Architecture
- Service-Oriented Architecture

## Anti-patterns
- Business rules scattered across listeners, commands, and UI
