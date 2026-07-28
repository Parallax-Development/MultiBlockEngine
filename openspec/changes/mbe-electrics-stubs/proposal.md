## Why

The `mbe-electrics` addon currently relies on stubs for critical mechanics. Specifically, fuel consumption for generators is simulated, and energy distribution across the `mbe-wiring` graph is mocked (it currently does not traverse the network to find consumers or storages). These stubs need to be replaced with real, fully functional logic to make the electrics system usable and integrated.

## What Changes

- Replace simulated fuel consumption in `CoalGeneratorTickAction` with real logic that consumes valid fuel items from the generator's inventory (e.g., Furnace inventory).
- Implement `ElectricsService.pushEnergy` to traverse the `mbe-wiring` `NetworkGraph` and distribute energy to connected storage blocks and consumers.
- Implement `ElectricsService.drawEnergy` to traverse the `mbe-wiring` `NetworkGraph` and draw energy from connected storage blocks and producers.
- Update `ElectricsManager` to properly register and unregister multiblock I/O ports as `NetworkNode`s in the `mbe-wiring` system during formation and destruction, if they are not already managed.

## Capabilities

### New Capabilities

- `electrics-network-integration`: Defines how electric multiblocks register their input/output ports as nodes in the `mbe-wiring` graph.

### Modified Capabilities

- `electrics-generation`: Replace the simulated fuel consumption requirement with actual inventory fuel consumption.
- `electrics-consumption`: Detail the graph traversal requirements for pushing and drawing energy across the network.

## Impact

- **mbe-electrics**: Major logic changes in `ElectricsService`, `ElectricsManager`, and `CoalGeneratorTickAction`.
- **mbe-wiring**: Will now experience active graph traversal and node queries.
