## Why

MultiBlockEngine's addon discovery currently validates uniqueness solely by the `id` declared in `addon.yml`. If two JARs use different IDs but share the exact same main class (or package footprint), both addons load, instantiate, and execute concurrently, causing race conditions and service collisions. Furthermore, when an addon is disabled or fails during lifecycle phases, registered Bukkit event listeners and contextual resources remain active in Bukkit's event handler registry, leading to severe memory leaks and ghost event executions.

## What Changes

- Add main class uniqueness validation during addon discovery to prevent loading multiple JARs that instantiate the same main class (even if their IDs differ).
- Implement automatic tracking and unregistration of Bukkit `Listener` objects registered via `SimpleAddonContext.registerListener()` upon addon disable or failure.
- Implement cleanup of custom actions, conditions, block matchers, wrench actions, and multiblock types registered by an addon upon disable or failure.
- Ensure `AddonClassLoader` instances are safely released only after all registered listeners, tasks, and services are fully unregistered.

## Capabilities

### New Capabilities
- `addon-lifecycle-integrity`: Guarantees main class uniqueness across discovered addons and automatic resource/listener cleanup upon addon disablement or failure.

### Modified Capabilities

## Impact

- `AddonDiscoveryService.java`: Enhanced candidate validation to detect main class collisions.
- `AddonAuditService.java`: Indexing and cross-auditing of main classes across discovered JARs.
- `SimpleAddonContext.java`: Active tracking of registered Bukkit event listeners, actions, conditions, matchers, and multiblock types.
- `AddonRuntimeLifecycleService.java`: Invocation of resource cleanup routines during `disableAddons()` and `failAddon()`.
