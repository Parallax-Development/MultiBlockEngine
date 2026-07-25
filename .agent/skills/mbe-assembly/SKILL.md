---
name: mbe-assembly
description: Multiblock assembly system.
---

## Purpose
Multiblock assembly system.

## Trigger
- Detect block changes relevant to patterns.
- Attempt to form/disband multiblock structures.
- Orchestrate pattern validation and instance creation.

## Scope
- AssemblyTrigger
- AssemblyCoordinator
- AssemblyReport
- Formation and rollback cycle

## Non-goals
- Execute manual assembly outside the official flow.
- Mix pattern validation with persistence or UI logic.
- Couple triggers to concrete multiblock implementations.

## Rules
- Do not execute logic outside the coordinator.
- The coordinator decides the final formation outcome.
- Any assembly output must produce an explicit report.

## Required checks
- The trigger only detects/routes; it does not orchestrate the entire domain.
- The coordinator validates the pattern, state, and preconditions.
- The result includes success/failure and a traceable cause.
- No permanent side effects occur before confirming the formation.

## Failure modes
- Inconsistent formation due to partial validations.
- Double assembly of the same structure caused by event race conditions.
- Tick lag due to excessive validation in hot paths.
- Corrupted state when an intermediate rollback fails.

## Test checklist
- Happy path formation with a valid pattern.
- Rejection with a clear report for an invalid pattern.
- Idempotency against repeated triggers.
- Rollback verification when an intermediate step fails.

## Implementation checklist
- Keep triggers lightweight and focused on detection.
- Centralize assembly rules in the coordinator.
- Emit a structured report for each attempt.
- Isolate side effects until the final state is confirmed.

## Example: do vs avoid
- **Do**: Trigger invokes the coordinator and consumes the `AssemblyReport`.
- **Avoid**: Trigger validates the pattern, creates the instance, and persists it on its own.

## Patterns
- Coordinator pattern
- Trigger-based execution
- Decoupled triggers

## Anti-patterns
- Duplicated assembly logic across multiple listeners.
