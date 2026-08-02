## ADDED Requirements

### Requirement: MBE Event Bus
The system SHALL provide an agnostic event bus interface (`EventBusService`) to publish and subscribe to domain events without Bukkit dependencies.

#### Scenario: Subscribing to an event
- **WHEN** an addon subscribes to `MultiblockAssembleEvent` via the `EventBusService`
- **THEN** the handler is registered and called when the event is published

#### Scenario: Publishing an event
- **WHEN** the core publishes a domain event through the bus
- **THEN** all registered listeners receive the event payload in priority order
