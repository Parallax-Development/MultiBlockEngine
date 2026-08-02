## Why

`mbe-electrics` is the first major content addon for MultiBlockEngine, demonstrating the capabilities of `mbe-wiring` and the overall event-driven, decoupled architecture. It provides an energy-based gameplay loop, which is a staple in technical Minecraft addons, by introducing multiblocks that generate, store, and consume energy through the established graph network systems.

## What Changes

- Introduce the `mbe-electrics` addon module structure, registering it properly with MBE Core.
- Add energy generator multiblocks (e.g., Coal Generator) that produce energy and push it to the network.
- Add energy consumer multiblocks (e.g., Electric Furnace, Pulverizer) that draw energy from the network to perform tasks.
- Introduce energy storage blocks (e.g., Battery Box) to act as network buffers.
- Register specific cables, items, and crafting materials needed for these machines.
- Establish `energy` and `information` interaction standards within the addon utilizing `mbe-wiring` infrastructure.

## Capabilities

### New Capabilities
- `electrics-generation`: Multiblocks that produce energy (e.g., Coal Generator) and insert it into the wiring network.
- `electrics-consumption`: Multiblocks that consume energy (e.g., Electric Furnace) to process items or provide utility.
- `electrics-storage`: Blocks or multiblocks that store energy and act as network buffers.
- `electrics-items`: The items, components, and basic blocks registered for crafting and gameplay.

### Modified Capabilities
- 

## Impact

- **New Module:** `addons/mbe-electrics` will be created.
- **Dependencies:** This addon strictly depends on `mbe-wiring` for its graph network and energy propagation logic, and on MBE core/api for multiblock lifecycle.
- **Game Content:** Adds custom items, block displays, and multiblock patterns to the server.
