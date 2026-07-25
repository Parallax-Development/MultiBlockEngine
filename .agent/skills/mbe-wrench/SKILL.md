---
name: mbe-wrench
description: Tool interaction system.
---

## Purpose
Tool interaction system.

## Trigger
- Process player interactions with a wrench-like tool
- Route actions based on context, block, or multiblock state
- Extend interaction actions from addons

## Scope
- WrenchDispatcher
- Interactions
- Context resolution
- Action routing

## Non-goals
- Implement complete business logic in click listeners
- Hardcode actions without going through a dispatcher
- Couple interaction to a single multiblock implementation

## Rules
- No logic in listeners
- Resolve action based on explicit and validated context
- Apply permissions and preconditions before executing action

## Required checks
- Listeners only extract context and delegate to the dispatcher
- Dispatcher selects action deterministically
- Each interaction validates permissions/state before mutating domain
- Action failures return a manageable and traceable result

## Failure modes
- Inconsistent behavior due to ambiguous routing
- Actions executed without authorization or valid state
- Complexity growth due to action if/else chains in listeners
- Duplication of interaction logic in multiple points

## Test checklist
- Correct action resolution by representative context
- Correct denial when permissions or preconditions are missing
- Error handling without breaking interaction flow
- Extension of new action without touching existing listeners

## Implementation checklist
- Model interaction contract and result
- Implement `WrenchDispatcher` as a single point of routing
- Register actions/handlers by context type
- Keep listeners thin and free of domain rules

## Example: do vs avoid
- Do: Listener delegates to the dispatcher, dispatcher resolves action, and service executes
- Avoid: Listener with giant switch statement that modifies domain directly

- Use dispatcher

- Command dispatch
- Context-based handler resolution

## Anti-patterns
- Anemic dispatcher that only forwards to static utilities
- Wrench actions implemented directly in listeners
