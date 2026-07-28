## ADDED Requirements

### Requirement: Domain Wrappers
The system SHALL define agnostic wrapper interfaces for platform concepts: `MBEPlayer`, `MBEBlock`, `MBELocation`, and `MBEItemStack` inside the `api` module.

#### Scenario: Player interaction
- **WHEN** a domain service needs to send a message to a player
- **THEN** it calls `MBEPlayer#sendMessage(String)` without knowing if it's a Bukkit player underneath

#### Scenario: Block manipulation
- **WHEN** a multiblock needs to change a block state
- **THEN** it calls `MBEBlock#setType(String)` using agnostic material identifiers
