---
name: mbe-services
description: Define how to implement and use services in MBE.
---

## Purpose
Define how to implement and use services in MBE.

## Trigger
- Creating a new domain capability in MBE
- Refactoring logic by moving it out of listeners/commands/UI
- Integrating an addon feature that requires core orchestration

## Scope
- `MBEService`
- `ServiceRegistry`
- Injection (`@InjectService`)

## Non-goals
- Implementing concrete logic inside `core-api`
- Coupling addons to each other through direct references
- Resolving business rules inside listeners

## Rules
- All relevant logic must live in services.
- Use `@InjectService` for dependencies.
- Do not instantiate services manually.
- Keep contracts in interfaces and details in implementation.
- Expose domain operations, not infrastructure details.

## Required checks
- The service registers and participates in the engine's lifecycle.
- There are no `new` instantiations of other services within a service.
- There are no imports of concrete implementations when an API contract exists.
- The Bukkit API is not accessed outside of infrastructure/adapters.

## Failure modes
- Service unavailable due to incomplete registration.
- Circular dependency between services or addons.
- Inconsistent state due to execution out of lifecycle order.
- Main thread blocking caused by I/O inside the service.

## Test checklist
- Coverage of happy path and service validation errors.
- Verification of injected dependency via `@InjectService`.
- Verification of behavior when a dependency is unavailable.
- Verification of no main thread blocking during I/O operations.

## Implementation checklist
- Define the interface in the API when applicable.
- Implement the concrete service in the corresponding runtime module.
- Register the service in the engine's registry/lifecycle.
- Consume the service from other components via injection.

## Example: do vs avoid
- **Do**: A listener delegates to `AssemblyCoordinatorService` and finishes.
- **Avoid**: A listener contains validation, I/O, and domain state mutation.

## Patterns
- Service Registry Pattern
- Dependency Injection

## Anti-patterns
- Logic in listeners
- Direct access to implementations
- Manual service instantiation
- Services with multiple, unrelated responsibilities

## References
- `dev.darkblade.mbe.api.service.*`
