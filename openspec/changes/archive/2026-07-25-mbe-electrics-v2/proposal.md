## Why

The first version of `mbe-electrics` introduced the base infrastructure for electric multiblocks (generators and batteries) and the basic tick actions. However, processing machines like the Electric Furnace currently only simulate their logic (e.g., simulated draw, simulated smelting progress). We need a functional second version that fully implements processing machines, including item input/output handling, inventory abstractions, and recipe processing natively without relying on heavy UI frameworks like `mbe-ui`.

## What Changes

- Full implementation of processing logic for the `ElectricFurnace` (and establishing a generic base for future machines).
- Implementation of a native, declarative way for processing machines to interact with vanilla inventories (or custom inventory states) for input and output items.
- Real energy consumption based on the active state of processing (drawing energy only when items are being processed).
- Native interaction support (e.g., hoppers, manual insertion) for items.

## Capabilities

### New Capabilities
- `electrics-processing`: Handles the logic for electric processing machines (recipes, progress, item consumption, and production).
- `electrics-inventory`: Native inventory handling and integration for electric machines to hold inputs and outputs.

### Modified Capabilities
- `electrics-consumption`: Modifying to support conditional, recipe-driven energy draw (drawing energy per tick based on recipe requirements and processing state).

## Impact

- `mbe-electrics` addon internals (TickActions, Services, States).
- Potential impact on how Multiblock instances hold or reference inventory data.
- Will provide a standard approach for declarative processing machines that can be extended by other addons.
