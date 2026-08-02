## 1. Addon Structure and Initialization

- [x] 1.1 Create `mbe-electrics` gradle module (or directory structure) and configure its `build.gradle.kts`.
- [x] 1.2 Create `ElectricsAddon.java` extending `MBEAddon` and register it.
- [x] 1.3 Set up resource structure (`plugin.yml` equivalent or module metadata) and load it in the core.

## 2. Infrastructure & Services

- [x] 2.1 Create `ElectricsService` to handle ticking updates for electrical machines.
- [x] 2.2 Create `ElectricsManager` to handle Bukkit events (e.g. placing machines).
- [x] 2.3 Register `energy` network type interactions if not fully covered by `mbe-wiring`.

## 3. Items and Resources

- [x] 3.1 Register basic components (e.g., Copper Ingot, Circuit, Casing).
- [x] 3.2 Define configuration files for basic items (if YAML-driven).

## 4. Machines (Multiblocks)

- [x] 4.1 Implement `CoalGenerator` multiblock logic (fuel consumption, `energy` pushing).
- [x] 4.2 Implement `ElectricFurnace` multiblock logic (item processing, `energy` drawing).
- [x] 4.3 Implement `BatteryBox` multiblock logic (energy buffer, pushing/pulling).
- [x] 4.4 Define YAML structures for these machines and register them in the Catalog.

## 5. Testing and Validation

- [x] 5.1 Test placing and connecting machines via cables.
- [x] 5.2 Validate that the Coal Generator effectively pushes power to the Battery Box.
- [x] 5.3 Validate that the Electric Furnace draws power from the network to smelt items.
