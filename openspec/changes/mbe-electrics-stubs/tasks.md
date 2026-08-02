## 1. Network Node Registration

- [x] 1.1 Update `ElectricsManager` to inject `NetworkService` from `mbe-wiring`.
- [x] 1.2 In `ElectricsManager.onMultiblockForm`, map the anchor location to a `NetworkNode` of type `ENERGY` with appropriate connectable faces via `NetworkService`.
- [x] 1.3 In `ElectricsManager.onMultiblockBreak`, unregister the `NetworkNode` from `NetworkService`.

## 2. Graph Traversal for Energy

- [x] 2.1 Update `ElectricsService.pushEnergy` to find the `NetworkGraph` containing the source node.
- [x] 2.2 Implement graph iteration in `pushEnergy` to find storage/consumer nodes and distribute energy among them up to their limits.
- [x] 2.3 Update `ElectricsService.drawEnergy` to find the `NetworkGraph` containing the consumer node.
- [x] 2.4 Implement graph iteration in `drawEnergy` to find producer/storage nodes and draw energy until the requested amount is met.

## 3. Real Fuel Consumption

- [x] 3.1 Update `CoalGeneratorTickAction` to access the native Bukkit `FurnaceInventory` via the anchor block state.
- [x] 3.2 Check for a valid fuel item in the fuel slot when `fuel_ticks` <= 0.
- [x] 3.3 Consume 1 item from the fuel slot and set `fuel_ticks` to the standard burn time of that item using Bukkit's recipe API (or item types).
- [x] 3.4 Ensure the generator only produces energy if `fuel_ticks` > 0 and successfully pushes it via `ElectricsService`.
