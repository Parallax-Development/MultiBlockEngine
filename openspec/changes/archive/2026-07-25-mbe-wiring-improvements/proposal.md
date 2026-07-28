## Why

The `mbe-wiring` addon provides a robust foundation for building network-based systems, but it currently lacks full persistence for manual wire disconnections (via wire cutter) and struggles to visually integrate with external systems dynamically. Additionally, the debug tools are narrowly focused on energy networks. These improvements are necessary to prepare the wiring infrastructure for the upcoming `mbe-electrics` addon, ensuring stable topology, seamless visual integration with new machines, and proper debugging capabilities.

## What Changes

- **Wire Cutter Persistence**: Manual network splits performed by players using the wire cutter tool will be persisted. The system will save the disconnected state between adjacent wire blocks so that networks do not unintentionally reconnect upon server restart or chunk reload.
- **Debug Particles Expansion**: The debug visualization tool will be updated to support all network types, explicitly fixing the issue where particles fail to render on `information` networks.
- **Visual Connection Mappings**: A new configuration-driven mapping system will be introduced (`mappings/*.yml`). This will allow users to define which standard blocks (by `namespace:id`) a specific cable type should visually connect to. This creates visual coherence without hardcoding dependencies or altering the logical graph directly.

## Capabilities

### New Capabilities
- `wiring-visual-mappings`: Configuration system to define visual connections between cables and specific block types.
- `wire-cutter-persistence`: Logic to persist the disconnected state of manually cut wires across restarts.

### Modified Capabilities
- `wiring-debug-particles`: Update requirement to ensure particles render for all network types, including `information`.

## Impact

- **Core Wiring Persistence**: Minor impact on how wiring chunk data or metadata is stored to include disconnected faces.
- **Debug Tools**: Minor change to the debug visualization service.
- **Wiring Rendering (BlockDisplay)**: Changes to the client-side visual connection logic to query the new mapping configurations before rendering connections to adjacent blocks.
