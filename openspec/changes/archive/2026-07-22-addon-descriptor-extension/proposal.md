## Why

The current `addon.yml` descriptor model is functional but basic. As the MultiBlockEngine ecosystem grows, addons will need to declare more context about their environment constraints (like required Minecraft/Java versions or Bukkit plugins), provide rich metadata for UI catalogues, and safely declare inter-addon dependencies and capabilities (permissions). Expanding the descriptor allows for a more secure, decentralized, and decoupled architecture, preventing runtime crashes and enforcing stricter contracts through the `AddonAuditService`.

## What Changes

- Add **metadata fields** (`name`, `description`, `authors`, `website`) to the descriptor model.
- Add **environment constraints** (`minecraft`, `java`, `plugins` under an `environment` key) to validate the host environment before loading.
- Add **load ordering constraints** (`load-before`, `load-after`) to allow topological sorting even without strict dependencies.
- Add a **namespaced capabilities system** (`capabilities` list) where addons declare which features they intend to consume (e.g., `mbe-core:energy`, `mbe-ui:panels`).
- Update `AddonDiscoveryService` to parse these new fields and map them to a new, extended `AddonMetadata` model.
- Update `AddonAuditService` and `AddonDependencyResolver` to enforce these constraints and topological orderings.

## Capabilities

### New Capabilities
- `addon-descriptor`: Extends the addon configuration format, validation, and lifecycle metadata mapping.
- `addon-capabilities`: Introduces a namespaced capability declaration and enforcement system within the addon lifecycle.

### Modified Capabilities
- (None)

## Impact

- **Core Discovery**: `AddonDiscoveryService` and `AddonMetadata` will be heavily updated to parse the new YAML schema.
- **Auditing**: `AddonAuditService` will be expanded to validate capabilities, java versions, and server plugin dependencies.
- **Dependency Resolution**: `AddonDependencyResolver` will factor in `load-before` and `load-after` constraints in the DAG generation.
- **Addon API**: The `AddonContext` or `MultiblockAddon` interfaces might expose some of these capabilities for addons to verify at runtime.
