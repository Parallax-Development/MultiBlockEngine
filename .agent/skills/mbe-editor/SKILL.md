---
name: mbe-editor
description: Interactive editor.
---

## Purpose
Interactive editor.

## Trigger
- Create or modify structures from an interactive flow
- Manage player inputs for pattern editing
- Persist editing session results

## Scope
- EditorSession
- Inputs
- Change validation
- Session confirmation/cancellation

## Non-goals
- Persist partial changes without transactional control
- Mix UI render/editor with complex domain logic
- Share session state between players without isolation

## Rules
- Apply changes only after validation and confirmation
- Isolate temporary editing state from final persistent state

## Required checks
- Each player has an isolated and consistent session
- Invalid inputs do not corrupt session state
- Confirmation applies changes; cancellation reverts temporary ones
- Unexpected player disconnection cleans up session and associated resources

## Failure modes
- Structure corruption due to applying incomplete changes
- Session leaks after disconnections or errors
- Race conditions when editing the same target in parallel
- Inconsistent UX due to absence of early validations

## Test checklist
- Create/edit/cancel session with expected states
- Invalid input handling and session recovery
- Confirmation of correctly persisted changes
- Session cleanup on logout and shutdown

## Implementation checklist
- Define `EditorSession` state machine
- Validate input at the edge before mutating session
- Separate temporary changes from final commit
- Implement cleanup hooks per player lifecycle

## Example: do vs avoid
- Do: Explicit session with `draft -> validate -> commit/cancel`
- Avoid: Applying every input directly to persisted state

## Patterns
- Session state machine
- Draft/commit workflow

## Anti-patterns
- Shared global state for all sessions
- Direct persistent mutation during interactive editing
