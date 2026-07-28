## Context

`MultiBlockEngine` uses a powerful internal logging system with components like `CoreLogger`, `LogScope`, and `LoggingConfig`. Currently, many high-volume startup events like service registrations (e.g. via `ServiceLifecycleOrchestrator`) and multiblock template loads (e.g. via `MultiBlockEngine` or catalogs) are hardcoded to log at `LogLevel.INFO`. This causes excessive console noise for end users, drowning out important information or errors. The existing `LoggingConfig` is already equipped to filter logs based on severity and the plugin's `debug` configuration flags, so we simply need to leverage it properly.

## Goals / Non-Goals

**Goals:**
- Reduce default console output during plugin startup by hiding per-item service registration and multiblock loading logs.
- Provide high-level startup metrics in `INFO` logs (e.g. total multiblocks loaded, total services registered).
- Maintain the ability to see verbose logs when the `debug` setting is enabled in the config.

**Non-Goals:**
- Modifying the logging framework infrastructure (e.g. `LogPhase`, `LogLevel`, `LogBackend`).
- Hiding warning or error logs.

## Decisions

**Decision 1: Down-level verbose logs to DEBUG**
*Rationale*: Instead of creating a new logging wrapper or custom filter, we will simply change the hardcoded `LogLevel.INFO` calls in `ServiceLifecycleOrchestrator` and `log.info(...)` in `MultiBlockEngine` to `LogLevel.DEBUG` and `log.debug(...)`. The existing `LoggingConfig` will automatically filter these out when debug is disabled, meaning we get the desired behavior with minimal changes.

**Decision 2: Implement Accumulators for Startup Metrics**
*Rationale*: Since we are hiding individual item loads, users still need to know that the plugin successfully loaded its content. We will add integer counters (e.g., `loadedMultiblocksCount`, `registeredServicesCount`) during the `BOOT` and `LOAD` phases. At the end of the phase (or at the end of `onEnable`), a single `INFO` log will be emitted containing these summaries.

## Risks / Trade-offs

- **Risk**: A user might think the plugin isn't loading anything if they don't see the individual logs.
  - **Mitigation**: The summary logs (accumulators) ensure they still see that X items were loaded, giving confidence that the system is functioning.
- **Trade-off**: Slightly more state (counters) kept during startup. Negligible performance or memory impact.
