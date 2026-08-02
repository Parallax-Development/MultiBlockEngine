## 1. Tick Action Refactor

- [x] 1.1 Update `ElectricFurnaceTickAction` to accept the multiblock instance and get the anchor location.
- [x] 1.2 Access the `FurnaceInventory` from the anchor block if it is a Furnace/BlastFurnace.
- [x] 1.3 Query Bukkit's recipe system using the input slot to find the smelting result.

## 2. Conditional Energy & Processing

- [x] 2.1 Check if the output slot can accommodate the recipe result.
- [x] 2.2 If the recipe is valid and output has space, draw energy via `ElectricsService`.
- [x] 2.3 On successful energy draw, increment native `cookTime` and decrement `cookTimeTotal` appropriately.
- [x] 2.4 When `cookTime` reaches completion, consume input and add output item to the inventory.

## 3. Vanilla Override

- [x] 3.1 Create `FurnaceBurnListener` in `mbe-electrics` package to intercept `FurnaceBurnEvent`.
- [x] 3.2 If the block is part of a multiblock from `mbe-electrics`, cancel the event to prevent vanilla fuel burning.
- [x] 3.3 Register the listener in `ElectricsAddon`.
