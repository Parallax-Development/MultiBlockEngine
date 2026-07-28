## Context

The current `mbe-preview` system projects a 1:1 holographic model of a multiblock structure using `BlockDisplay` entities. This creates visual clutter, making it hard to see the internal components of large structures. This design aims to implement a 0.5 scale offset to improve visibility, along with other QoL features like layer-by-layer building, nudge controls, error feedback, and intelligent rotation.

## Goals / Non-Goals

**Goals:**
- Scale `BlockDisplay` entities down to 0.5 and center them within their original 1x1x1 volume.
- Allow players to filter the preview vertically (layer-by-layer).
- Provide visual feedback (red tint) when an incorrect block is placed in the previewed area.
- Provide nudge controls to shift the preview without restarting it.
- Dynamically set the initial rotation of the preview based on player facing.

**Non-Goals:**
- Completely rewriting the multiblock assembly or blueprint logic.
- Adding complex animations to the preview beyond basic translations.
- Full "auto-build" capability that places blocks for the player without interaction.

## Decisions

- **Block Scaling and Translation**: `BlockDisplay` entities support matrix transformations. We will apply a uniform scale of `0.5f` and a translation offset of `0.25f` in X, Y, and Z. This will be managed through the `DisplayCompatService` / `BukkitDisplayCompatService` abstraction to ensure cross-version compatibility.
- **Layer-by-layer rendering**: We will add a `currentLayer` index to `PreviewSession`. When updating the display, blocks with `Y > currentLayer` will have their view range set to 0 or be hidden.
- **Error Feedback**: `PreviewBlockPlaceListener` will intercept block placements. If the placed block doesn't match the expected `MultiblockDefinition` material, the corresponding `BlockDisplay`'s glow color or block color will be updated to a red tint to signify an error.
- **Nudge Controls**: We will add interactions (e.g., specific item interactions or commands) that adjust a `Vector3i offset` stored in `PreviewSession`. When changed, all active `BlockDisplay`s in the session will update their translations asynchronously.
- **Initial Auto-Rotation**: When a player starts a blueprint via `BlueprintStartEvent` or `PreviewPlacementController`, their yaw will determine the 90-degree aligned rotation applied to the structure's blueprint matrix before generating the displays.

## Risks / Trade-offs

- **Performance**: Updating translations or scales dynamically for large multiblocks (hundreds of blocks) can cause server or client lag.
  - *Mitigation*: Process updates asynchronously and batch `BlockDisplay` updates where possible.
- **Compatibility**: BlockDisplay API changes between Minecraft versions.
  - *Mitigation*: We will strictly use the existing `DisplayCompatService` abstraction and only add required methods to it.
