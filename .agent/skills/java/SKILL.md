---
name: java
description: Establish Java development standards within the project.
---

## Purpose
Establish Java development standards within the project.

## Trigger
- Implementing new features in Java
- Refactoring existing classes, services, or listeners
- Reviewing PRs focusing on maintainability and robustness

## Scope
- Object-oriented design
- Immutability
- Streams vs loops
- Exception handling
- Public API design
- Domain modeling

## When to use
- Whenever Java code is written
- Refactors
- PR reviews

## Non-goals
- Premature optimization without measurement
- Introducing abstractions without domain necessity
- Using language features that reduce team readability

## Rules
- Prefer immutability whenever possible
- Avoid nulls → use `Optional` where it makes sense
- Do not use complex logic in lambdas
- Exceptions should be meaningful (not generic)
- Class/method names must express domain intent
- Keep methods small with a single responsibility

## Required checks
- No `null` is propagated in critical public contracts
- Exceptions provide actionable context
- There are no classes with mixed responsibilities
- Avoid coupling to implementations when an interface exists

## Failure modes
- Accidental complexity due to static utilities with implicit state
- Bugs caused by inconsistent null handling
- Code that is hard to test due to anemic objects or god classes
- Silent errors due to generic `catch` blocks without context

## Test checklist
- Error branch coverage for business rules
- Verification of public contracts with invalid inputs
- Verification of domain object invariants
- Verification of behavior without unexpected side-effects

## Implementation checklist
- Define the domain model before writing infrastructure
- Design interfaces and value objects when applicable
- Implement input validations at system boundaries
- Ensure explicit error handling and contextual propagation

## Example: do vs avoid
- Do: service with a clear interface, immutable DTOs/VOs, and domain exceptions
- Avoid: static utility class that centralizes global mutable state

## Patterns
- Value Objects
- Factory Methods
- Strategy Pattern
- Builder Pattern

## Anti-patterns
- God classes
- Utility classes with state
- Optional used as an entity field

## References
- Java 21+
