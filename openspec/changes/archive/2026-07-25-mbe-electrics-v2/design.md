## Context

The `mbe-electrics` addon aims to provide a suite of electric processing machines (like the Electric Furnace) without relying on heavy UI frameworks (`mbe-ui`). The `ElectricFurnace` multiblock is currently defined using a vanilla `BLAST_FURNACE` as its controller block. The goal is to make it a fully functional machine that can process recipes by consuming items and electrical energy.

## Goals / Non-Goals

**Goals:**
- Implement a fully functional Electric Furnace that processes vanilla (and custom, if supported) smelting recipes.
- Utilize the native inventory of the `BLAST_FURNACE` block for item input/output.
- Integrate with `mbe-wiring` for energy consumption.
- Keep the system modular so future machines can be added easily.
- Support standard Vanilla interactions (e.g., Hoppers pushing/pulling items).

**Non-Goals:**
- Creating custom UI panels using `mbe-ui` for this specific machine.
- Redefining the vanilla recipe system (we should reuse Bukkit's recipe querying).

## Decisions

**Decision 1: Native Inventory Usage**
Instead of creating virtual inventories stored in `MultiblockInstance` state, we will cast the anchor block state to `org.bukkit.block.Furnace` (or `org.bukkit.inventory.InventoryHolder`). We will directly manipulate the `FurnaceInventory`. This provides immediate compatibility with Vanilla hoppers, droppers, and player interactions.

**Decision 2: Native Smelting Progress Visualization**
Since the controller is a `BLAST_FURNACE`, we can manipulate its `cookTime` and `cookTimeTotal` via Bukkit's API to show the native UI smelting arrow progress to players viewing the block. This enhances UX without custom packets or GUIs.

**Decision 3: Recipe Resolution**
We will query Bukkit's `Server#getRecipesFor` or rely on `FurnaceRecipe` / `BlastingRecipe` to resolve what the input item yields. 

**Decision 4: Energy Draw Strategy**
Energy will be drawn in `ElectricFurnaceTickAction`. The furnace will only draw energy if:
1. It has a valid recipe.
2. The output slot has room.
3. If energy draw fails (not enough energy in network), smelting progress stalls.

## Risks / Trade-offs

- **[Risk] Syncing Native State**: Manipulating `Furnace` states requires calling `.update()` which could be heavy if done every tick.
  → **Mitigation**: Only call `.update()` when `cookTime` changes (which is expected for active furnaces) and ensure we do it asynchronously if possible, or accept the minor overhead since vanilla furnaces do this anyway.
- **[Risk] Vanilla Fuel Burning**: Players might put coal in the fuel slot, causing vanilla to smelt items without electricity.
  → **Mitigation**: We can either block inserting fuel via events, or cancel the `FurnaceBurnEvent` for blocks that are part of an electric multiblock.
