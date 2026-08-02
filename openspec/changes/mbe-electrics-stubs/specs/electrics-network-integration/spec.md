## ADDED Requirements

### Requirement: Node Registration
Electric multiblocks SHALL register themselves as `NetworkNode`s in the `mbe-wiring` subsystem when they are formed or loaded, and unregister themselves when broken.

#### Scenario: Register on form
- **WHEN** a player successfully forms an electric multiblock (e.g. Coal Generator or Electric Furnace)
- **THEN** the `ElectricsManager` captures the formation event
- **AND** registers the anchor block as a `NetworkNode` of type `ENERGY` with the `NetworkService`
- **AND** exposes the appropriate block faces as connectable.

#### Scenario: Unregister on break
- **WHEN** an electric multiblock is broken
- **THEN** the `ElectricsManager` captures the break event
- **AND** unregisters the anchor block from the `NetworkService`.

### Requirement: Graph Energy Distribution
The `ElectricsService` SHALL provide capabilities to push and draw energy by traversing the `NetworkGraph` associated with a specific `NetworkNode`.

#### Scenario: Pushing energy
- **WHEN** a generator produces energy and requests to push it via `ElectricsService.pushEnergy`
- **THEN** the service locates the source `NetworkNode` and its `NetworkGraph`
- **AND** iterates over the graph's nodes to find valid consumers or storage blocks
- **AND** deposits the energy evenly or sequentially until the pushed amount is fully absorbed or all nodes are full.

#### Scenario: Drawing energy
- **WHEN** a consumer requires energy and requests to draw it via `ElectricsService.drawEnergy`
- **THEN** the service locates the source `NetworkNode` and its `NetworkGraph`
- **AND** iterates over the graph's nodes to find valid producers or storage blocks
- **AND** extracts energy up to the requested amount.
