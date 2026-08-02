## 1. Addon Discovery & Main Class Uniqueness

- [x] 1.1 Add main class candidate tracking in `AddonDiscoveryService.java` to group candidates by `mainClass()` as well as `id()`.
- [x] 1.2 Implement collision handling in `AddonDiscoveryService` to mark addons as `FAILED` and log errors when duplicate `mainClass` entries are detected.

## 2. Resource & Listener Tracking in Context

- [x] 2.1 Update `SimpleAddonContext.java` to maintain thread-safe collections for registered Bukkit listeners, actions, conditions, matchers, wrench actions, and multiblock types.
- [x] 2.2 Add a `cleanup()` method in `SimpleAddonContext.java` that unregisters Bukkit listeners via `HandlerList.unregisterAll(listener)` and clears registered contextual resources.

## 3. Lifecycle Teardown Integration

- [x] 3.1 Invoke `context.cleanup()` in `AddonRuntimeLifecycleService.java` during `disableAddons()` teardown for loaded addons.
- [x] 3.2 Ensure `context.cleanup()` is called when an addon fails during `onLoad()`, `onEnable()`, or service exposure failure.
- [x] 3.3 Verify classloader closure occurs strictly after listener unregistration and service disposal.

## 4. Verification & Validation

- [x] 4.1 Verify compilation and run build check (`mvn test-compile` or `gradlew build`).
- [x] 4.2 Verify addon discovery and cleanup logic against test scenarios.
