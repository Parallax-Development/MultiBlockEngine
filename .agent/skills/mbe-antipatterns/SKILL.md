---
name: mbe-antipatterns
description: Define prohibited practices.
---

## Purpose
Define prohibited practices.

## Trigger
- Reviewing design before implementing a feature
- Auditing PRs and refactors with structural impact
- Diagnosing recurring technical debt

## Scope
- Module architecture
- Services and events
- Integration with Bukkit and infrastructure

## Non-goals
- Replacing detailed technical documentation with a rigid list
- Stopping valid refactors due to misapplied rules
- Prioritizing personal style over architectural criteria

## Rules
- No logic in the `api` module.
- No God classes.
- No circular dependencies.
- No direct use of Bukkit outside of infrastructure boundaries.
- No heavy business logic in listeners or commands.
- No blocking I/O on the main thread.

## Required checks
- No module violates the API/runtime/addon separation.
- There are no circular dependencies in services or addons.
- Listeners delegate and do not orchestrate the entire domain.
- There are no direct accesses to infrastructure outside of adapters.

## Failure modes
- Progressive coupling that makes the engine fragile.
- Tick lag due to improper execution on the main thread.
- Bugs that are hard to isolate due to overlapping responsibilities.
- Broken extensibility of the addon ecosystem.

## Test checklist
- Review dependencies per module to ensure there are no cycles.
- Review hot paths to detect synchronous I/O.
- Review the size and responsibilities of critical classes.
- Review integration points with Bukkit.

## Implementation checklist
- Move domain logic to single-purpose services.
- Introduce contracts where concrete coupling exists.
- Extract infrastructure into explicit adapters.
- Add thread validations for sensitive operations.

## Example: do vs avoid
- **Do**: A thin listener that delegates to a service and publishes a domain event.
- **Avoid**: A listener that validates, persists, mutates domain state, and renders UI.
