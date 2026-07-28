## 1. Core Display Adjustments

- [x] 1.1 Add translation and scale vector setters to `DisplayCompatService` API
- [x] 1.2 Implement the translation and scale modifications in `BukkitDisplayCompatService` using Bukkit's transformation API
- [x] 1.3 Update `PreviewSession` (or related display generation logic) to apply a uniform 0.5f scale and a 0.25f offset translation to all preview `BlockDisplay`s

## 2. Dynamic Orientation

- [x] 2.1 Update `BlueprintStartEvent` / `PreviewPlacementController` to capture the player's initial yaw
- [x] 2.2 Calculate a 90-degree aligned rotation based on the captured yaw
- [x] 2.3 Apply this rotation transformation to the blueprint matrix before generating the holograms

## 3. Layer-by-layer Building

- [x] 3.1 Add a `currentLayer` index to `PreviewSession` state, defaulting to the lowest Y level
- [x] 3.2 Update display rendering to hide blocks where relative Y > `currentLayer`
- [x] 3.3 Add an input mechanism (e.g. command or scroll interaction) to increment/decrement `currentLayer`

## 4. Error Feedback

- [x] 4.1 In `PreviewBlockPlaceListener`, detect when a block is placed that doesn't match the expected material at that coordinate
- [x] 4.2 Fetch the associated `BlockDisplay` for the mismatched block from the active `PreviewSession`
- [x] 4.3 Use `DisplayCompatService` to apply a red tint/glow color to the specific `BlockDisplay`

## 5. Nudge Controls

- [x] 5.1 Add a `Vector3i` offset field to `PreviewSession` to track nudge delta
- [x] 5.2 Implement an input mechanism (e.g. command `/mbe nudge <x> <y> <z>` or specific item interactions) to update the nudge delta
- [x] 5.3 Implement a refresh function that applies the updated offset to all existing displays asynchronously without rebuilding them entirely
