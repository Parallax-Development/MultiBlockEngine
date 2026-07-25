---
name: mbe-blueprints
description: Guided construction system.
---

## Purpose
Guided construction system.

## Trigger
- Initiate assisted multiblock construction flow
- Guide the player from selection to final placement
- Coordinate placement validations with visual feedback

## Scope
- BlueprintItem
- PreviewPlacementController
- Structure selection
- Placement confirmation

## Non-goals
- Place real blocks during the preview phase
- Mix domain validation logic with guide rendering
- Skip final verification of placement conditions

## Rules
- Maintain construction session state per player
- Validate preconditions before confirming placement

## Required checks
- The preview does not alter the real world state
- The flow clearly distinguishes between the preview stage and confirmation
- Permissions/context are validated before placing
- Canceling the session cleans up associated visual resources

## Failure modes
- Accidental placement due to confusing preview with execution
- Orphaned sessions with persistent displays
- Desynchronization between selected blueprint and applied structure
- Tick lag due to excessive rendering/validation without control

## Test checklist
- Start/cancel/finish blueprint session
- Verification of no world mutation in preview
- Verification of correct placement upon confirmation
- Verification of visual cleanup after cancellation or logout

## Implementation checklist
- Model blueprint session with explicit state
- Render preview with a dedicated controller
- Execute final validation prior to real placement
- Clean up entities/temporary state when finishing the flow

## Example: do vs avoid
- Do: Visual preview + explicit confirmation before placing
- Avoid: Using the same routine for preview and real placement

- Separate preview from real placement

- Session-based interaction
- Command/query separation

## Anti-patterns
- Fusing preview and placement into a single step
- Implicit session state without lifecycle control
