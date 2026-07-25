---
name: mbe-ui
description: Decoupled UI system.
---

## Purpose
Decoupled UI system.

## Trigger
- Create or modify panels for player interaction.
- Display multiblock state or configuration flows.
- Connect UI actions with domain services.

## Scope
- PanelViewService
- Bindings
- Inventory/dynamic panel views
- Contextual interactions

## Non-goals
- Execute business rules within UI handlers.
- Access storage or infrastructure directly from the view.
- Couple the view to concrete service implementations.

## Rules
- The UI contains no business logic.
- UI actions must delegate to domain services.
- Displayed state must derive from clear models/bindings.

## Required checks
- UI handlers do not mutate the domain without going through a service.
- The binding covers relevant state updates.
- The view does not depend on internal classes of other modules.
- UI closing/refreshing leaves no orphaned resources or sessions.

## Failure modes
- Visual inconsistency due to desynchronized state.
- Bugs caused by domain logic embedded in click handlers.
- Session/UI leaks from failing to clear player context.
- Tight coupling that breaks when changing the service backend.

## Test checklist
- Render verification for base states and edge cases.
- Verification of correctly delegated UI actions.
- Verification of dynamic binding updates.
- Verification of cleanup upon panel close or player disconnect.

## Implementation checklist
- Define the data contract consumed by the view.
- Implement bindings between the model and UI components.
- Delegate user commands to domain services.
- Manage panel/session lifecycle explicitly.

## Example: do vs avoid
- **Do**: Button click invokes a service and then refreshes the binding.
- **Avoid**: Button click executes validation, persistence, and direct mutation.

## Patterns
- Presentation model
- MV* with decoupled domain
- Event-driven UI updates
- Use bindings

## Anti-patterns
- Mutable UI state with no defined source of truth.
