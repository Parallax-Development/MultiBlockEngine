## Why

The current multiblock preview system displays blocks at a 1:1 scale relative to the real world, which causes visual clutter and collisions that make it difficult for players to see the interior structure of large multiblocks. Improving the Quality of Life (QoL) of this system is necessary to provide a cleaner, more readable, and intuitive guided building experience for players.

## What Changes

- Introduce a dynamic scaling system for `BlockDisplay` entities in previews, defaulting to 0.5 (50%) scale.
- Automatically apply a positional offset (translation) to scaled preview blocks so they remain perfectly centered within their 1x1x1 world grid space.
- Implement layer-by-layer building visualization, allowing players to view the multiblock slice by slice.
- Add visual feedback for errors (e.g., tinting a block red if the player places the wrong material).
- Add nudge/offset controls allowing players to shift the preview position around before committing.
- Add intelligent auto-rotation for the preview based on the player's look direction (pitch/yaw).
- (Optional/Exploratory) Provide partial auto-completion if the player has the required materials in their inventory.

## Capabilities

### New Capabilities
- `multiblock-preview`: Covers all mechanics and visual configurations for projecting holographic multiblock blueprints into the world using BlockDisplay entities, including scaling, translation, interaction, and rendering feedback.

### Modified Capabilities

## Impact

- `dev.darkblade.mbe.preview.*` packages, specifically dealing with `BlockDisplay` rendering, translation, and scale transformations.
- `PreviewSession` logic to handle layered visualization, nudging, and intelligent rotation.
- Compatibility layer `DisplayCompatService` / `BukkitDisplayCompatService` to support setting interpolation, scale, and translation vectors.
- Player interaction listeners for nudging, error detection, and auto-completion.
