# addon-lifecycle-integrity Specification

## Purpose
TBD - created by archiving change fix-addon-duplicate-and-lifecycle-leaks. Update Purpose after archive.
## Requirements
### Requirement: Main Class Uniqueness Enforcement
The addon discovery system SHALL ensure that no two discovered JAR files instantiate the same main class (`main` field in `addon.yml`). If multiple JAR files declare the exact same main class, the discovery service SHALL fail all conflicting candidates and prevent any of them from being loaded or initialized.

#### Scenario: Duplicate main class across different addon IDs
- **WHEN** two JAR files (`addon-a.jar` and `addon-b.jar`) declare different IDs in `addon.yml` but specify the exact same main class
- **THEN** the discovery service marks both addons as `FAILED`, logs a warning detailing the duplicate main class conflict, and does not load either addon

### Requirement: Automatic Bukkit Listener Unregistration
The addon runtime lifecycle system SHALL track all Bukkit `Listener` instances registered via `AddonContext.registerListener(Listener)` and automatically unregister them from Bukkit's `HandlerList` when the addon is disabled or fails during initialization.

#### Scenario: Addon disabled cleanly
- **WHEN** an addon registered Bukkit listeners via its `AddonContext` and is subsequently disabled
- **THEN** all of its registered Bukkit listeners are unregistered from `HandlerList`, preventing any lingering event invocations or ClassLoader leaks

#### Scenario: Addon fails during enable phase
- **WHEN** an addon fails during `onEnable()` after registering Bukkit listeners
- **THEN** all listeners registered by the failing addon are immediately unregistered before the classloader is closed

### Requirement: Contextual Resource Cleanup
The addon runtime lifecycle system SHALL track custom actions, conditions, block matchers, wrench actions, and multiblock types registered by an addon, and unregister them when the addon is disabled or fails.

#### Scenario: Resource unregistration on disable
- **WHEN** an addon is disabled
- **THEN** its registered actions, conditions, matchers, wrench actions, and multiblock types are removed from the engine runtime, ensuring no orphan references remain

