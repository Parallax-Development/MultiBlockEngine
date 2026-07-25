---
name: mbe-persistence
description: Data persistence.
---

## Purpose
Data persistence.

## Trigger
- Save multiblock state, snapshots, or metrics.
- Recover state after server restart/crash.
- Migrate or version persisted data structures.

## Scope
- StorageService
- Snapshots
- Storage registry
- Recovery flow

## Non-goals
- Perform direct IO from domain logic.
- Allow free access to the storage backend.
- Block the main thread during reads/writes.

## Rules
- No direct storage access.
- Use `StorageService`.
- All persistence operations must go through system contracts.
- Define explicit retry, timeout, and fallback strategies.

## Required checks
- No IO calls in sync Bukkit listeners or handlers.
- Each write operation handles errors and reports useful results.
- Data versioning or backward compatibility mechanisms exist.
- Critical operations have a recovery path documented in code.

## Failure modes
- Data loss due to partial writes without confirmation.
- Logical corruption due to schema changes without migration.
- Server saturation caused by poorly scheduled save tasks.
- Inconsistency caused by race conditions between saving and state mutation.

## Test checklist
- Happy path of save/load for relevant domain entities.
- Simulated backend error with fallback/retry verification.
- Read compatibility with previous versions of persisted data.
- Async execution verification for IO operations.

## Implementation checklist
- Expose operation in `StorageService` before using a concrete backend.
- Implement stable and versioned serialization/deserialization.
- Isolate backend inside an infrastructure adapter.
- Emit basic metrics for success/failure and operation latency.

## Example: do vs avoid
- **Do**: Domain service delegates persistence to `StorageService` asynchronously.
- **Avoid**: Event listener writes to file or DB directly.

## Patterns
- Repository pattern (via `StorageService`)
- Asynchronous IO

## Anti-patterns
- Direct IO from domain logic.
- Persistence without data versioning.
- Silent handling of storage exceptions.
