## ADDED Requirements

### Requirement: BlockDisplay Scaling and Translation
The system SHALL scale all `BlockDisplay` entities rendered during a multiblock preview to 0.5 of their original size, and apply an offset translation of (0.25, 0.25, 0.25) to center them within their original 1x1x1 block space.

#### Scenario: Hologram generated for preview
- **WHEN** a preview session generates a `BlockDisplay` for a multiblock part
- **THEN** the display scale is set to (0.5, 0.5, 0.5)
- **THEN** the display translation offset is set to (0.25, 0.25, 0.25) relative to the corner

### Requirement: Layer-by-layer Building Mode
The system SHALL provide an option for the preview to only render the current layer (Y-level) that the player is building.

#### Scenario: Player toggles layer mode
- **WHEN** the player activates the layer-by-layer view in a preview session
- **THEN** the preview hides blocks above the current lowest incomplete Y-level

### Requirement: Error Feedback Rendering
The system SHALL provide visual feedback when a player places an incorrect block where a multiblock component is expected.

#### Scenario: Incorrect block placed in preview
- **WHEN** the player places a block that does not match the multiblock pattern at a projected location
- **THEN** the preview tint color for that specific `BlockDisplay` changes to red (or an error material)

### Requirement: Preview Nudge Controls
The system SHALL allow players to incrementally shift the origin of the preview (nudge) along the X, Y, and Z axes without needing to cancel and restart the preview.

#### Scenario: Player nudges the preview
- **WHEN** the player triggers a nudge command or interaction (e.g. shift+click with a tool)
- **THEN** the entire multiblock preview moves by 1 block in the specified direction

### Requirement: Auto-rotation Based on Look Direction
The system SHALL orient the preview dynamically based on the player's current yaw/pitch when initially generating the projection.

#### Scenario: Player starts a preview while facing East
- **WHEN** the player requests to preview a multiblock and their camera faces East
- **THEN** the multiblock projection is rotated to face East relative to its defined 'front'
