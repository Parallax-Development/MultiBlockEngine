# Wiring & Multiblock Coupling Specification

## ADDED Requirements

### Requirement: Single NetworkType per Cable Node
Each cable node MUST support exactly one `NetworkType`. Cable nodes SHALL NOT combine multiple network channels or network types in a single block position.

#### Scenario: Cable connection evaluation for matching network type
Given a cable node `A` of `NetworkType.ENERGY` at block position `P1`
When evaluating connectivity towards adjacent position `P2` containing a node `B` of `NetworkType.ENERGY`
Then the connection evaluation SHALL return true if directional faces permit.

#### Scenario: Cable connection evaluation for mismatched network type
Given a cable node `A` of `NetworkType.ENERGY` at block position `P1`
When evaluating connectivity towards adjacent position `P2` containing a node `B` of `NetworkType.FLUID`
Then the connection evaluation SHALL return false.

### Requirement: Connection Restricted to Formal IO Ports
Cable nodes SHALL ONLY establish connections with multiblock structure blocks that are formally defined as `IOPort` or `PortDefinition` via `PortResolutionService`. Decorative and casing blocks SHALL be rejected.

#### Scenario: Connecting cable to formal multiblock IO port
Given an assembled `MultiblockInstance` with an `IOPort` at position `P_PORT`
When an adjacent cable node evaluates connectivity towards `P_PORT`
Then the connection evaluation SHALL succeed and establish a topological `NetworkConnection`.

#### Scenario: Rejecting connection to multiblock casing block
Given an assembled `MultiblockInstance` with a non-port casing block at position `P_CASING`
When an adjacent cable node evaluates connectivity towards `P_CASING`
Then the connection evaluation SHALL return false.

### Requirement: Dynamic Topology on Multiblock Form & Break
When a multiblock is formed or broken, network topology SHALL be automatically updated.

#### Scenario: Registering IO Ports on MultiblockFormEvent
Given a newly formed `MultiblockInstance`
When `MultiblockFormEvent` fires
Then all resolved `IOPort`s SHALL be registered as `NetworkNode`s in `NetworkService` and adjacent cables SHALL re-evaluate connectivity.

#### Scenario: Unregistering IO Ports on MultiblockBreakEvent
Given a broken `MultiblockInstance`
When `MultiblockBreakEvent` fires
Then all associated `IOPort` nodes SHALL be unregistered from `NetworkService` and graph topology SHALL be recomputed.
