---
name: mbe-events
description: Correct usage of the event system.
---

## Purpose
Correct usage of the event system.

## Trigger
- Notifying relevant domain changes between decoupled modules
- Reacting to player actions without coupling internal components
- Extending behavior from addons without invading base services

## Scope
- Bukkit events
- MBE domain events
- Decoupled publishing and consuming

## Non-goals
- Replacing domain services with event chains
- Chaining events to model critical flows without an orchestrator
- Executing heavy I/O operations in synchronous handlers

## Rules
- Prefer events over direct calls.
- Events must be decoupled.
- Do not abuse synchronous events.
- The event payload must be minimal and domain-oriented.
- Handlers must delegate to services and remain lightweight.

## Required checks
- The event has an explicit domain name and purpose.
- The handler does not contain complex logic or direct storage access.
- It is validated whether the operation should be sync or async before publishing/consuming.
- There is no direct dependency between addons for the same flow.

## Failure modes
- Event storms caused by redundant publishing.
- Inconsistency caused by mutating state across multiple uncoordinated handlers.
- Tick lag caused by synchronous handlers with expensive operations.
- Implicit coupling caused by depending on accidental listener execution order.

## Test checklist
- Verification of event publishing at the correct domain point.
- Verification of cancellation and priorities when applicable.
- Verification of handler idempotency against retries.
- Verification of the absence of blocking operations in sync handlers.

## Implementation checklist
- Define the event with the minimum necessary data.
- Publish the event from the responsible service/coordinator.
- Implement a thin listener that delegates to a service.
- Define listener priority based on gameplay impact and consistency.

## Example: do vs avoid
- **Do**: `MultiblockFormEvent` triggers UI reactions and metrics in separate listeners.
- **Avoid**: A central listener coordinates all assembly and persistence logic.

## Patterns
- Domain events
- Decoupled observer
- Event-driven architecture

## Anti-patterns
- Complete business logic inside listeners
- Using events for rigid, direct communication between addons
