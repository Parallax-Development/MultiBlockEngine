---
name: mbe-addon-system
description: Define how addons work.
---

## Purpose
Define how addons work.

## Trigger
- Incorporating a new addon into the MBE ecosystem
- Resolving dependencies between existing addons
- Designing cross-addon integration without rigid coupling

## Scope
- Lifecycle
- Dependency resolution
- Cross-references
- Load, initialization, and shutdown phases

## Non-goals
- Allowing direct references between addon implementations
- Mixing core rules with addon-specific logic
- Bypassing dependency validations during bootstrap

## Rules
- Addons must not depend directly on each other.
- Declare explicit and verifiable dependencies.
- Isolate addon services behind API contracts.

## Required checks
- All addon dependencies are declared and resolvable.
- There are no direct imports to internal classes of another addon.
- The addon lifecycle respects the initialization order.
- Extension points are exposed by contract and not by implementation.

## Failure modes
- Load failure due to a missing or improperly declared dependency.
- Circular dependency between addons during bootstrap.
- Partial initialization leaving services in an inconsistent state.
- Hidden coupling that breaks compatibility when updating addons.

## Test checklist
- Successful loading of the addon without optional dependencies.
- Expected behavior when a required dependency is missing.
- Successful resolution of declared cross-references.
- Orderly shutdown with resource cleanup.

## Implementation checklist
- Define the addon descriptor/dependency declaration.
- Register addon services in the corresponding lifecycle.
- Resolve cross-references via the official resolver.
- Validate startup and shutdown in nominal and degraded scenarios.

## Example: do vs avoid
- **Do**: Addon A consumes a contract published by Addon B via the resolver (`CrossReferenceResolver`).
- **Avoid**: Addon A instantiates internal classes of Addon B directly.

## Patterns
- Modular extensions
- Plugin lifecycle management
- Dependency graph resolution

## Anti-patterns
- Implicit initialization by side effects.
