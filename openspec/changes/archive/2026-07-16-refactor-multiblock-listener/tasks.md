## 1. API Changes

- [x] 1.1 Create `MultiblockLifecycleService` interface in `api/src/main/java/dev/darkblade/mbe/api/service/lifecycle/MultiblockLifecycleService.java`

## 2. Core Implementation

- [x] 2.1 Create `DefaultMultiblockLifecycleService` implementation in `core` that implements `tryDisassemble`
- [x] 2.2 Register `DefaultMultiblockLifecycleService` in the `MBEServiceRegistry`

## 3. Platform (Bukkit) Refactor

- [x] 3.1 Refactor `MultiblockListener` constructors to remove Service Locator usages and inject `MultiblockLifecycleService`
- [x] 3.2 Update `onBlockBreak` in `MultiblockListener` to delegate break logic to `MultiblockLifecycleService` and correctly respect cancellation
- [x] 3.3 Clean up redundant domain logic and `getInstance` calls from `MultiblockListener`
