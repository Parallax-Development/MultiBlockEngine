## ADDED Requirements

### Requirement: Declare Capabilities
The `addon.yml` format SHALL support a `capabilities` field (list of strings) representing namespaced keys of capabilities the addon intends to consume or provide (e.g., `mbe-core:energy`).

#### Scenario: Capability definition
- **WHEN** an addon includes `capabilities: ["mbe-core:ui"]`
- **THEN** the `AddonMetadata` correctly parses and stores this capability requirement

### Requirement: Enforce Capabilities During Audit
The `AddonAuditService` SHALL verify if an addon attempts to access services or APIs protected by a specific capability, and block/fail the addon if that capability was not declared in its `addon.yml`.

#### Scenario: Authorized capability usage
- **WHEN** an addon interacts with a protected API and HAS declared the required capability
- **THEN** the audit passes successfully

#### Scenario: Unauthorized capability usage
- **WHEN** an addon interacts with a protected API but HAS NOT declared the required capability
- **THEN** the `AddonAuditService` SHALL fail the addon and produce an audit violation reporting the missing capability
