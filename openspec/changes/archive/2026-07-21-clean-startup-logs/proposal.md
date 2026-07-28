## Why

The `MultiBlockEngine` plugin currently logs verbose startup information (like individual service registrations and multiblock loadings) at the `INFO` level. While this detailed output is invaluable for debugging, it creates significant "noise" in the server console for end users, obscuring important milestones and errors. We want a cleaner startup log for production while retaining the ability to view detailed traces when debugging.

## What Changes

- Down-level noisy, individual log messages (e.g., `SERVICE_REGISTER`, `LOAD` for multiblocks and templates) from `INFO` to `DEBUG`.
- Rely on the existing `LoggingConfig` and `CoreLogger` infrastructure to automatically filter these debug logs when the `debug` setting in `config.yml` is false.
- Introduce summary log messages (accumulators) that print at `INFO` level at the end of the `BOOT` or startup phase to provide high-level metrics (e.g., "Loaded 14 multiblocks", "Registered 25 services") without the per-item spam.

## Capabilities

### New Capabilities
- `startup-log-summary`: A new capability that tracks the count of loaded items, multiblocks, and registered services during startup and prints a consolidated summary.

### Modified Capabilities

## Impact

- `dev.darkblade.mbe.core.application.service.ServiceLifecycleOrchestrator`
- `dev.darkblade.mbe.core.MultiBlockEngine`
- Console output readability will significantly improve for all server administrators.
