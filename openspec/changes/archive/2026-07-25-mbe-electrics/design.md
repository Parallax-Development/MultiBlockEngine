## Context

MultiBlockEngine provides `mbe-wiring`, a generic graph-based networking system, but lacks a concrete implementation of an energy system that utilizes it for gameplay purposes. `mbe-electrics` aims to fulfill this role by providing standard energy mechanics (generation, storage, and consumption) entirely decoupled from the core engine, utilizing `mbe-wiring` for distribution.

## Goals / Non-Goals

**Goals:**
- Implement a robust `mbe-electrics` addon following SOA (Service-Oriented Architecture).
- Provide energy generation (Coal Generator), storage (Battery Box), and consumption (Electric Furnace).
- Register unique `NetworkNode` types and `Multiblock` patterns via `mbe-wiring` and the catalog.
- Demonstrate best practices for integrating an addon with the core and `mbe-wiring`.

**Non-Goals:**
- Do not modify `mbe-wiring` or the core engine logic (unless absolutely necessary for bug fixes).
- Do not implement complex GUIs natively (rely on `mbe-ui` if applicable or keep simple block interactions for MVP).

## Decisions

**1. Addon Structure and Lifecycle:**
`mbe-electrics` will implement an `MBEAddon` class. It will register its own `ElectricsService` to handle ticking logic and `ElectricsManager` to handle Bukkit events (e.g., interactions).

**2. Energy Network Integration:**
Machines will register themselves as nodes in the `energy` network provided by `mbe-wiring`. 
- **Generators** will tick and push energy to the graph via the `NetworkService`.
- **Consumers** will request energy from the graph.
- **Batteries** will act as both receivers and providers.

**3. Multiblock Definition:**
Multiblock structures will be defined programmatically or via YAML configurations parsed during the addon's initialization phase, utilizing the core `MultiblockBuilder` and `AssemblyTrigger`.

## Risks / Trade-offs

- **Performance Risk:** Frequent graph traversal for energy propagation might lag the server. 
  *Mitigation:* Use `mbe-wiring`'s optimized graph state and cache routes/demands. Ensure ticks are distributed properly across ticks (e.g., staggering machine updates).
- **Coupling Risk:** Tight coupling to `mbe-wiring` internals.
  *Mitigation:* Stick strictly to the `api` module interfaces provided by `mbe-wiring` (e.g., `NetworkService`, `NetworkGraph`).
