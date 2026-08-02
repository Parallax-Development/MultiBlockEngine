## Context

The `mbe-electrics` addon contains placeholder implementations for multiblock fuel consumption, energy drawing, and energy pushing. In order for the electric multiblock systems to interact properly, they must interface with real inventories (Bukkit) and the wiring graph provided by `mbe-wiring`.

## Goals / Non-Goals

**Goals:**
- Replace the "fuel ticks" simulation in `CoalGeneratorTickAction` with an integration that reads from a Bukkit inventory (e.g. Furnace fuel slot) and consumes valid fuel items to generate fuel ticks.
- Implement graph traversal in `ElectricsService` to correctly push energy to active `EnergyStorage` or `EnergyConsumer` nodes on the same network.
- Implement graph traversal in `ElectricsService` to correctly draw energy from active `EnergyStorage` or `EnergyProducer` nodes on the same network.
- Map multiblock anchor locations and their I/O ports to `mbe-wiring` via `ElectricsManager`.

**Non-Goals:**
- Adding new electric multiblocks (e.g., batteries or processing machines).
- Overhauling the `mbe-wiring` graph structure itself.

## Decisions

- **Node Registration**: `ElectricsManager` already listens to `MultiblockFormEvent` and `MultiblockBreakEvent` for tracking multiblock location. It will use the `NetworkService` from `mbe-wiring` to register these anchor blocks as `NetworkNode`s. We will define their capabilities based on their multiblock type (e.g. `electric_furnace` as consumer, `coal_generator` as producer).
- **Graph Traversal**: `ElectricsService.pushEnergy` will find the `NetworkGraph` containing the source block, iterate over connected `NetworkNode`s, check their multiblock capabilities using `MultiblockInstanceRegistry`, and deposit energy up to their capacity until the pushed amount is exhausted.
- **Generator Fuel**: `CoalGeneratorTickAction` will check the Furnace block state's inventory. If it finds fuel, it consumes 1 item and sets `fuel_ticks` to the item's standard burn time.

## Risks / Trade-offs

- **Performance of Traversal**: Traversing large wiring networks every tick could impact performance.
  - *Mitigation*: Rely on `mbe-wiring`'s optimized graph caching, and only iterate nodes rather than performing expensive pathfinding for each push/draw tick.
