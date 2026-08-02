## Context

The `addon.yml` currently parsed by `AddonDiscoveryService` only reads `id`, `version`, `api`, `main`, `dependencies`, and `soft-dependencies`. As MultiBlockEngine moves towards a heavily decoupled, service-oriented ecosystem, addons need a way to declare more metadata, specify runtime constraints (Java, Minecraft, and Bukkit plugin versions), influence load order beyond strict code dependencies, and explicitly list the capabilities they use.

## Goals / Non-Goals

**Goals:**
- Provide a robust way to describe addon constraints without coupling to Bukkit directly.
- Ensure the `AddonAuditService` has enough descriptor data to enforce capability security.
- Parse capabilities as namespaced keys (e.g., `mbe-core:energy`).
- Influence `AddonDependencyResolver` with topological hints (`load-before`, `load-after`).

**Non-Goals:**
- We are not rewriting the entire YAML configuration engine; we will stick to Bukkit's `YamlConfiguration` for compatibility.
- We will not download dependencies automatically. The descriptor only validates constraints.

## Decisions

### 1. Representation in `AddonMetadata`
The Java record `AddonMetadata` will be extended with new fields. Since Java records are immutable, these fields will be wrapped in `Map.copyOf` or `List.copyOf`. 

- `List<String> authors`, `String description`, `String website`
- `Environment environment` (A nested record containing Java ver, MC ver, Plugins map)
- `List<String> capabilities`
- `List<String> loadBefore`, `List<String> loadAfter`

### 2. Capabilities as Namespaced Strings
Instead of enums, capabilities will be modeled as `namespace:key`. The audit service will cross-reference the capability declarations in `AddonMetadata` when an addon registers or requests a protected service from `AddonContext`.

### 3. Load Order Resolution
`AddonDependencyResolver` will process `load-before` and `load-after` as directed edges in the dependency DAG, but unlike `dependencies`, if a target addon is missing, it will simply ignore the edge rather than failing the resolution.

## Risks / Trade-offs

- **Risk**: Increased parsing complexity might make `addon.yml` verbose for simple addons.
  - **Mitigation**: All new fields (`environment`, `capabilities`, `load-before`, etc.) are entirely optional. If absent, the addon parses fine under legacy assumptions.
- **Risk**: Directed cycles in `load-before`/`load-after`.
  - **Mitigation**: The topological sort algorithm already detects cycles for standard dependencies. We will handle cycle detection gracefully and fail addons participating in a cyclic load order constraint.
