# MultiBlockEngine

**MultiBlockEngine** is a high-performance, extensible, and maintainable multi-block structure system designed for Minecraft servers (Paper/Spigot).

---

## 🎯 Key Objectives

- **Event-Driven Detection**: Detects structures based on events rather than periodic scans.
- **YAML Definitions**: Define multi-block structures using external YAML files.
- **Immutable Models**: Compiles definitions into an internal immutable model for thread safety and performance.
- **Efficient Lifecycle**: Creates live instances only when valid and maintains state with fast invalidation.
- **Separation of Concerns**: Strict separation between definition, instance, and behavior.

---

## 🧠 Design Philosophy

> "The plugin does not search for structures; it recognizes them when they are born and maintains them while they exist."

**Core Principles:**
- **Immutability**: Definitions cannot be changed at runtime, ensuring stability.
- **Early Validation**: Fail fast during parsing to prevent runtime errors.
- **Lightweight Runtime**: Minimal overhead during gameplay.
- **Extensibility**: Designed to grow without breaking existing implementations.

---

## 🧱 Configuration (YAML)

Structures are defined in `plugins/MultiBlockEngine/multiblocks/`.

### Basic Example

```yaml
id: simple_core
version: 1.0

controller:
  # The anchor block that triggers detection
  block: DIAMOND_BLOCK

pattern:
  # Relative offsets from the controller (x, y, z)
  - offset: [0, -1, 0]
    match: OBSIDIAN
  
  - offset: [1, 0, 0]
    match: "#logs"  # Minecraft Tag support

behavior:
  custom_key: "value"
```

### Matcher Types

The engine compiles YAML patterns into efficient `BlockMatcher` instances:

- **Exact Material**: Matches a specific `Material`.
- **Tag Matcher**: Matches a block tag (e.g., `#logs`, `#wool`).
- **AnyOf**: Matches any from a list of matchers.
- **Air**: Explicitly matches air blocks.

---

## 🏗️ Project Structure

The codebase follows a modular architecture:

```text
src/main/java
└─ com.darkbladedev.engine
   ├─ model
   │  ├─ MultiblockType.java       # Immutable definition
   │  ├─ MultiblockInstance.java   # Live structure instance
   │  ├─ PatternEntry.java         # Relative pattern rule
   │  └─ matcher
   │     ├─ BlockMatcher.java      # Functional interface
   │     ├─ ExactMaterialMatcher.java
   │     ├─ TagMatcher.java
   │     └─ ...
   ├─ parser
   │  └─ MultiblockParser.java     # YAML to Object compiler
   ├─ manager
   │  └─ MultiblockManager.java    # State & Logic controller
   ├─ listener
   │  └─ MultiblockListener.java   # Event handling
   └─ storage
      └─ SqlStorage.java           # Persistence layer
```

---

## ⚙️ Execution Flow

1.  **Startup**:
    - Load YAML definitions.
    - Validate and compile into `MultiblockType`.
    - Restore persisted instances from database.
2.  **Runtime**:
    - Listen for `BlockPlaceEvent` and `PlayerInteractEvent`.
    - Check if the block is a known **Controller**.
    - Validate the surrounding pattern candidates.
    - If valid, create and register a `MultiblockInstance`.
3.  **Invalidation**:
    - Listen for `BlockBreakEvent`.
    - Check if the block belongs to an active instance (O(1) lookup).
    - Destroy the instance and clean up resources.

---

## 💾 Persistence

- **State Management**: Active instances are tracked in memory for fast access.
- **Shutdown**: Instances are serialized and saved to the database (SQLite via HikariCP).
- **Startup**: Instances are restored and lazily re-validated.

---

## 🚀 Future Development Plan

The following features are planned for future releases:

1.  **Phase 1: Condition & Action systems**
    - [ ] Implement a flexible condition system to control when a structure is valid.
    - [ ] Add actions to be performed when a structure is valid (e.g., trigger events, run commands).

2.  **Phase 2: Advanced Matching**
    - [ ] `BlockState` support (e.g., stairs, slabs).
    - [ ] NBT/Component data matching.

3.  **Phase 3: Dynamic Features**
    - [ ] Dynamic structures (growing/shrinking).
    - [ ] Multi-chunk structure support.

4.  **Phase 4: API & Integration**
    - [ ] Developer API for custom behaviors.
    - [ ] Integration with protection plugins (WorldGuard, GriefPrevention).

---

## 📚 References

- [PaperMC](https://papermc.io/) - High performance Minecraft server software.
- [HikariCP](https://github.com/brettwooldridge/HikariCP) - A "zero-overhead" production ready JDBC connection pool.
- [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) - Placeholder expansion for Bukkit plugins.
