## 1. Environment Setup

- [x] 1.1 Add the `incendo cloud-paper` dependencies to `build.gradle` (and possibly `cloud-annotations` if decided later).

## 2. Command Framework Core

- [x] 2.1 Create the `MBECommandManager` class that initializes and configures the `PaperCommandManager` instance.
- [x] 2.2 Register the `MBECommandManager` into the core services so addons and internal systems can access it.
- [x] 2.3 Implement custom Cloud parsers (e.g. `MultiblockTypeParser`) to resolve `MultiblockType` instances natively from arguments.

## 3. Subcommand Refactoring

- [x] 3.1 Migrate the `Debug` command (`/mbe debug`) to its own isolated class, testing the new framework and parsing.
- [x] 3.2 Migrate the `Blueprint` command (`/mbe blueprint`) and inject the required services.
- [x] 3.3 Migrate the `Export` command (`/mbe export`) and inject the required services.
- [x] 3.4 Migrate the `Tool` command (`/mbe tool`) and any remaining minor subcommands into their own classes.

## 4. Cleanup and Routing

- [x] 4.1 Remove the monolithic `MultiblockCommand.java` and all native Bukkit `CommandExecutor` logic.
- [x] 4.2 Delete old routers like `ServicesCommandRouter` and `AddonsCommandRouter`, as Cloud now handles routing seamlessly.
- [x] 4.3 Ensure the `MultiBlockEngine` plugin initialization starts the new `MBECommandManager` properly instead of the old command.
