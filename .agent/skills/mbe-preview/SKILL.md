---
name: mbe-preview
description: Preview system using BlockDisplays.
---

## Purpose
Preview system using BlockDisplays.

## Trigger
- Show the player a structure preview before building
- Visually validate blueprint orientation/position
- Synchronize temporary rendering with editing or placement flow

## Scope
- StructurePreviewService
- DisplayEntityRenderer
- Lifecycle of temporary displays
- Real-time preview updates

## Non-goals
- Modify real blocks during preview
- Leave display entities orphaned after closing the flow
- Couple preview to assembly/persistence logic

## Rules
- Do not modify the real world
- Manage the complete lifecycle of temporary entities
- Limit update frequency to avoid TPS impact

## Required checks
- The preview exclusively uses temporary display entities
- Entities are cleaned up on cancellation, logout, and shutdown
- Visual updates do not depend on blocking operations
- Rendering respects the player's context and their active session

## Failure modes
- Orphaned entities accumulated in the world
- Performance drop due to excessive refreshing of displays
- Visual desynchronization between preview and expected final position
- Synchronization errors when changing orientation rapidly

## Test checklist
- Create/update/destroy preview in nominal flow
- Correct cleanup on cancellation and player disconnection
- Verification of no world mutation during preview
- Verification of FPS/TPS stability with multiple active previews

## Implementation checklist
- Centralize operations in `StructurePreviewService`
- Encapsulate rendering in `DisplayEntityRenderer`
- Register and clean up entities per session/player
- Apply throttling or coalescing to frequent updates

## Example: do vs avoid
- Do: Temporary rendering with displays and cleanup guaranteed by lifecycle
- Avoid: Placing real blocks and reverting later as a "preview"

## Patterns
- Ephemeral rendering
- Session-scoped resources
- Throttled updates

- Use Display Entities

## Anti-patterns
- Manage preview entities outside of a central service
