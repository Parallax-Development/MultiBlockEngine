## 1. Down-level verbose logs

- [x] 1.1 In `ServiceLifecycleOrchestrator.java`, change the `LogLevel.INFO` to `LogLevel.DEBUG` when logging "Service registered".
- [x] 1.2 In `MultiBlockEngine.java`, change the `log.info` to `log.debug` when logging "Loaded multiblock", item definitions, and triggers.
- [x] 1.3 In `AddonServiceRegistry.java` and `MBEServiceRegistry.java`, down-level any legacy adapter "Service registered" messages from INFO to DEBUG.

## 2. Implement Accumulators and Summary Logs

- [x] 2.1 In `ServiceLifecycleOrchestrator.java` (or related registry classes), add counters to track the total number of services registered.
- [x] 2.2 In `MultiBlockEngine.java`, add counters to track the total number of multiblocks, items, and addons loaded.
- [x] 2.3 Ensure a consolidated `INFO` log (e.g. "Loaded X multiblocks, Y services registered") is printed at the end of the startup phase (e.g. end of `onEnable` or end of `BOOT` phase).
