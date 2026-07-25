---
name: concurrency
description: Safe handling of concurrency.
---

## Purpose
Safe handling of concurrency.

## Trigger
- Executing IO, queries, or persistence writes
- Processing heavy tasks outside the main thread
- Sharing mutable state between tasks or listeners

## Scope
- Bukkit main thread vs async workers
- Access to shared state
- Task scheduling and coordination

## Non-goals
- Moving everything to async without validating thread-safety
- Accessing the Bukkit API from disallowed threads
- Solving design problems with excessive synchronization

## Rules
- Do not block the Bukkit main thread
- Use async for IO/persistence
- Avoid race conditions
- Protect shared state with explicit strategies
- Return to the main thread before interacting with the world/entities

## Required checks
- No IO operations run in Bukkit sync handlers
- Every concurrent access to mutable state has a defined strategy
- No Bukkit API usage in an unsafe async context
- Tasks have cancellation or lifecycle control

## Failure modes
- Tick lag due to heavy work on the main thread
- State corruption caused by races between tasks
- Deadlocks from nested locks or inconsistent ordering
- Orphaned task leaks after disable/reload

## Test checklist
- Simulation of concurrent loads on the same state
- Verification of thread safety in critical operations
- Verification of cancellation upon shutting down services/plugins
- Verification of latency/stable tick under load

## Implementation checklist
- Identify the sync/async boundary of the flow
- Encapsulate access to shared state
- Chain tasks with `CompletableFuture` or a controlled scheduler
- Re-enter the main thread for world/player changes

## Example: do vs avoid
- Do: read storage async and apply the result sync on the main thread
- Avoid: querying storage and modifying blocks within the same sync handler

## Patterns
- CompletableFuture
- Producer-consumer
- Task scheduling

## Anti-patterns
- Concurrent access without synchronization
- Shared mutable state without clear ownership
