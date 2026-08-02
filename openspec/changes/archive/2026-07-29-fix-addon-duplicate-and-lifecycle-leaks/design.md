## Context

MultiBlockEngine's addon subsystem handles discovery, classloader creation, dependency resolution, and service lifecycle management. Currently, discovery groups candidates by `metadata.id()` in `AddonDiscoveryService`. If two JARs use different IDs but declare the same `main` class or package structure, both pass discovery and instantiate their main classes concurrently. Additionally, when `SimpleAddonContext.registerListener(Listener)` registers Bukkit listeners with `plugin.getServer().getPluginManager().registerEvents(...)`, those listeners are registered globally under the engine plugin without tracking. When an addon is disabled or fails, Bukkit retains references to those listeners, preventing garbage collection of the `AddonClassLoader` and allowing event handlers to fire on disabled addons.

## Goals / Non-Goals

**Goals:**
- Detect and prevent loading duplicate main classes during `AddonDiscoveryService.loadAddons()`.
- Automatically track all Bukkit listeners registered via `SimpleAddonContext` per addon ID.
- Automatically unregister Bukkit listeners via `HandlerList.unregisterAll(listener)` during `disableAddons()` or `failAddon()`.
- Track registered actions, conditions, matchers, wrench actions, and multiblock types per addon context and provide unregistration capabilities during teardown.
- Prevent ClassLoader leaks and ensure predictable, fail-safe teardown.

**Non-Goals:**
- Replacing Bukkit's plugin manager or reimplementing full Java module system isolation.
- Changing `addon.yml` schema structure or breaking backwards compatibility for valid single-version addons.

## Decisions

1. **Dual Candidate Grouping in `AddonDiscoveryService`**:
   - In addition to `candidatesById` (`Map<String, List<DiscoveredAddon>>`), introduce `candidatesByMainClass` (`Map<String, List<DiscoveredAddon>>`).
   - If multiple discovered JARs share the same `mainClass()`, mark all of them as `FAILED` with a clear log message: `Addon failed: duplicate main class detected`.

2. **Resource & Listener Registry in `SimpleAddonContext`**:
   - Maintain a thread-safe `Set<Listener> registeredListeners` inside `SimpleAddonContext`.
   - Maintain collections for registered action keys, condition keys, matcher prefixes, wrench action keys, and multiblock type IDs.
   - Expose a `cleanup()` method on `SimpleAddonContext` that is invoked by `AddonRuntimeLifecycleService` during addon disable or failure teardown.

3. **Teardown Sequence in `AddonRuntimeLifecycleService`**:
   - Step 1: `unexposeAddonServices(addonId)`.
   - Step 2: `loaded.context().cleanup()` (unregisters Bukkit listeners via `HandlerList.unregisterAll(l)`, removes actions/conditions/matchers/multiblocks).
   - Step 3: `loaded.addon().onDisable()`.
   - Step 4: `serviceLifecycleManager.disableServices(addonId)`.
   - Step 5: Close `AddonClassLoader`.

## Risks / Trade-offs

- **[Risk]** If an addon registers Bukkit listeners directly via `Bukkit.getPluginManager().registerEvents(l, plugin)` instead of `context.registerListener(l)`, MBE cannot auto-detect them.
  - *Mitigation:* Document `context.registerListener(l)` as the mandatory API for addons, and add a check or recommendation in `AddonAuditService`.
- **[Risk]** Unregistering multiblock types at runtime could affect active multiblock instances if disabled mid-game.
  - *Mitigation:* Standard behavior for disabling an addon; active instances managed by that addon are gracefully invalidated or suspended.
