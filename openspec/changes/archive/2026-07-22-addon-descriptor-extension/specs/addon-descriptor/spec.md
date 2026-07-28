## ADDED Requirements

### Requirement: Addon Metadata Fields
The `addon.yml` format SHALL support optional metadata fields for descriptive purposes. These fields include `name`, `description`, `website`, and `authors` (list). These fields SHALL be parsed and stored within the `AddonMetadata` object.

#### Scenario: Valid metadata
- **WHEN** the `addon.yml` contains `name`, `description`, `authors`, and `website`
- **THEN** the parsed `AddonMetadata` exposes these fields correctly without failing discovery

### Requirement: Environment Constraints
The `addon.yml` format SHALL support an `environment` block to define host requirements, including `minecraft` (Minecraft version), `java` (Java version), and `plugins` (a map of Bukkit plugins and their minimum versions).

#### Scenario: Valid environment
- **WHEN** the host environment meets all conditions specified in the `environment` block
- **THEN** the addon is marked as discovered and audited successfully

#### Scenario: Unsatisfied environment
- **WHEN** the host environment fails to meet an environment condition (e.g., outdated Java version)
- **THEN** the discovery or audit service SHALL fail the addon and log an appropriate warning

### Requirement: Load Ordering Directives
The `addon.yml` format SHALL support `load-before` and `load-after` fields (lists of addon IDs) to influence the topological load order without introducing a strict code dependency.

#### Scenario: Load before directive
- **WHEN** addon A defines `load-before: [B]` and both are present
- **THEN** the dependency resolver SHALL place addon A before addon B in the resolved load order

#### Scenario: Missing optional order target
- **WHEN** addon A defines `load-before: [C]` but addon C is not installed
- **THEN** the dependency resolver SHALL safely ignore the directive and proceed loading addon A
