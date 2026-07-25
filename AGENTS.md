# `AGENTS.md` — MultiBlockEngine

## 1. Project Overview

**MultiBlockEngine (MBE)** is a modular engine designed for defining, assembling, and executing **multiblock structures in Minecraft**. It is built upon an **extensible, decoupled, and service-oriented architecture**.

The objective is not merely to detect structures, but to construct a **complete runtime ecosystem** where multiblocks are living entities with:

* Behavior
* State
* Interaction
* Integration with other systems (energy, UI, wiring, etc.)

MBE acts as a **platform**, not as an isolated plugin.

---

## 2. Architectural Philosophy

### 2.1 Key Principles

* **API-First Design**

  * The entire system is defined from the [`api`](api/) module.
  * The `core` module is an implementation, not the single source of truth.

* **Extreme Modularity via Addons**

  * Every significant feature must be externalizable.
  * The core must remain minimal and act purely as an orchestrator.

* **Service-Oriented Architecture (SOA)**

  * All relevant behavior resides within services (`MBEService`).
  * Dependency injection is handled via `@InjectService`.

* **Strict Decoupling**

  * Avoid rigid dependencies between modules.
  * Extensive use of contracts (interfaces).

* **Event-Driven**

  * Utilization of standard Bukkit events alongside custom events (`MultiblockFormEvent`, etc.).

---

### 2.2 System Layers

#### 1. [`api`](api/)

The public contract of the engine:

* Addons (`addon`)
* Multiblocks (`assembly`, `domain`)
* Wiring (`wiring`)
* Energy (`electricity`)
* UI (`ui`)
* Persistence (`persistence`)
* Services (`service`)

> Rule: **Never introduce concrete business logic here.**

---

#### 2. `core`

The runtime implementation:

* Orchestration (`ServiceLifecycleOrchestrator`)
* Service Registry (`MBEServiceRegistry`)
* Addons (`AddonLifecycleService`)
* Catalog (`StructureCatalogServiceImpl`)
* Blueprints (`dev.darkblade.mbe.blueprint`)
* Parsing (`MultiblockParser`)

---

#### 3. Addons (External to Core)

Responsible for:

* Specific features
* Integrations
* Custom UI
* Additional overarching systems

---

## 3. Fundamental Concepts

### 3.1 Multiblock

An entity composed of:

* Pattern (`PatternEntry`)
* State (`MultiblockState`)
* Instance (`MultiblockInstance`)

It is constructed via:

* `MultiblockBuilder`
* `AssemblyTrigger`

---

### 3.2 Assembly System

The system responsible for detecting and forming structures:

* `AssemblyTrigger` → defines when to attempt assembly
* `AssemblyCoordinator` → coordinates the process
* `AssemblyReport` → the result of the process

---

### 3.3 Blueprint System

A guided construction flow:

* Selection → Preview → Placement
* `BlueprintItem`
* `PreviewPlacementController`

---

### 3.4 Wiring System

Graph-based network systems:

* Nodes (`NetworkNode`)
* Connections (`NetworkConnection`)
* Graphs (`NetworkGraph`)

Enables:

* Energy transfer
* Signal transmission
* Distributed logic

---

### 3.5 Energy System

A comprehensive abstraction for energy:

* Producers (`EnergyProducer`)
* Consumers (`EnergyConsumer`)
* Storage (`EnergyStorage`)
* Networks (`EnergyNetwork`)

---

### 3.6 UI System

A decoupled view system:

* `PanelViewService`
* Bindings (`PanelBinding`)

Designed for:

* Inventories
* Dynamic panels
* Contextual interactions

---

### 3.7 Persistence

A robust storage and serialization system:

* `StorageService`
* `StorageRegistry`
* Snapshots, metrics, and recovery systems

---

## 4. Addon System

### 4.1 Nature

Addons are **first-class extension modules**, not secondary plugins.

They define:

* Services
* Domain logic
* Integrations

---

### 4.2 Lifecycle

Managed by:

* `AddonLifecycleService`
* `AddonDependencyResolver`

Includes phases for:

* Loading
* Dependency resolution
* Initialization
* Shutdown

---

### 4.3 Cross-Addon References

Key system component:

* `CrossReferenceResolver`
* `CrossReferenceDeclaration`

Allows for:

* Controlled interaction between addons
* Prevention of rigid coupling

---

## 5. Catalog and Configuration

### 5.1 Structure Catalog

* `StructureCatalogService`
* `CatalogMenu`

Responsible for:

* Exposing structures to the user
* Integrating seamlessly with the UI

---

### 5.2 YAML-Based Configuration

* Parsing via `MultiblockParser`
* Exporting via `StructureExporter`

---

## 6. Action and Condition System

### 6.1 Actions

Execute runtime effects:

* `SendMessageAction`
* `TeleportAction`
* `SetStateAction`

---

### 6.2 Conditions

Control execution flows:

* Permissions
* State checks
* Variable evaluation

---

## 7. Project Goals

### 7.1 Technical

* Create a fully extensible engine.
* Support independent and reusable addons.
* Maintain low coupling across the board.
* Support multiple concurrent sub-systems (UI, energy, wiring, etc.).

---

### 7.2 Functional

* Define complex structures via YAML.
* Enable rich, stateful interactions with multiblocks.
* Integrate with advanced gameplay mechanics.

---

### 7.3 Strategic

* Become a de-facto standard for multiblock structures.
* Foster a third-party ecosystem of addons.
* Provide robust tooling (editors, exporters, previews).

---

## 8. Rules for AI Agents

### 8.1 Critical Rules

* DO NOT directly couple addons to one another.
* DO NOT introduce concrete logic into the `api` module.
* ALWAYS prioritize services over direct static logic.
* PREFER using events over direct code hooks wherever applicable.

---

### 8.2 Mandatory Patterns

* Service Registry Pattern
* Dependency Injection (`@InjectService`)
* Event-Driven Flows
* Domain-Driven Modeling (Actions, Conditions)

---

### 8.3 Anti-Patterns to Avoid

* God classes.
* Hardcoded business logic in listeners.
* Circular dependencies between addons.
* Direct usage of Bukkit API outside of infrastructure/adapters.
* Hardcoding player or console messages (only console error messages can be hardcoded, provided they are in English).

---

### 8.4 Normative Precedence (Mandatory)

In case of conflict or ambiguity, apply the following order of precedence:

1. **Critical rules from this `AGENTS.md`** (Section 8.1).
2. **MBE skills rules and constraints** (`mbe-design-rules`, `mbe-antipatterns`, `mbe-best-practices`).
3. **Domain-specific skills** (`mbe-services`, `mbe-events`, `mbe-persistence`, etc.).
4. **General skills** (`java`, `architecture`, `concurrency`).

Resolution Rules:

* If two rules collide, apply the **most restrictive** one to preserve decoupling, stability, and extensibility.
* If no explicit rule exists, default to service-oriented design and decoupled events.
* Never sacrifice architectural boundaries for implementation convenience.

---

### 8.5 Global Definition of Done (DoD)

An implementation task is only considered complete if it fulfills all of the following:

* Respects the separation between `api` (contracts), `core` (orchestration), and addons (features).
* Does not introduce business logic into listeners, commands, or UI classes.
* Uses services as the primary entry point for logic and utilizes events for decoupling where applicable.
* Avoids blocking the main thread during I/O or persistence operations, handling concurrency safely.
* Does not use Bukkit API directly outside of infrastructure/adapters.
* Does not introduce circular dependencies or direct coupling between addons.
* Complies with the `Required checks` and `Test checklist` of relevant skills (Skill v2).
* Delivers code with explicit error handling and predictable failure states.

---

## 9. Evolutionary Context (IMPORTANT)

The project is currently transitioning towards:

* Increased flexibility for addons.
* Elimination of rigid restrictions between modules.
* More declarative configurations (e.g., `inventories.yml`).
* Reduced dependency on heavy UI frameworks (`mbe-ui`).

---

## 10. How an Agent Should Think About MBE

An agent must interpret MBE as:

> "An extensible, service-based engine where multiblocks are domain entities that interact through decoupled systems (energy, wiring, UI, etc.), and whose behavior emerges from the composition of independent addons."

---

## 11. Skills Registry

### General Skills

| Skill | Description | Type |
|------|------------|------|
| java | Java Best Practices | General |
| architecture | Architectural Principles | General |
| concurrency | Concurrency Handling | General |
| git-branch | Git Branching Guidelines | General |
| git-versioning | Git Versioning | General |
| git-tags | Git Tagging | General |

---

### MBE Core Skills

| Skill | Description | Type |
|------|------------|------|
| mbe-architecture | General Architecture | Concept |
| mbe-services | Service System | Pattern |
| mbe-events | Event System | Pattern |
| mbe-addon-system | Addon System | Concept |

---

### MBE Systems

| Skill | Description | Type |
|------|------------|------|
| mbe-assembly | Assembly System | Concept |
| mbe-wiring | Wiring System | Concept |
| mbe-energy | Energy System | Concept |
| mbe-ui | UI System | Concept |
| mbe-persistence | Persistence | Concept |

---

### MBE Tooling

| Skill | Description | Type |
|------|------------|------|
| mbe-blueprints | Guided Construction | Execution |
| mbe-preview | BlockDisplay Previews | Execution |
| mbe-wrench | Tool Interactions | Execution |
| mbe-editor | Interactive Editor | Execution |
| wiki-sync | Code-Wiki Synchronization | Execution |

---

### MBE Rules

| Skill | Description | Type |
|------|------------|------|
| mbe-antipatterns | Prohibited Practices | Constraint |
| mbe-best-practices | Best Practices | Constraint |
| mbe-design-rules | Fundamental Engine Rules | Constraint |
