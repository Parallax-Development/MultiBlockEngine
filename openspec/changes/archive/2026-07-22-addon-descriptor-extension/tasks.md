## 1. Update AddonMetadata Model

- [x] 1.1 Add `Environment` inner record to `AddonMetadata` for java, minecraft, and plugins constraints
- [x] 1.2 Add `capabilities` (List of Strings) to `AddonMetadata`
- [x] 1.3 Add `loadBefore` and `loadAfter` (Lists of Strings) to `AddonMetadata`
- [x] 1.4 Add `authors`, `description`, `website` (Strings/Lists) to `AddonMetadata`

## 2. Extend Discovery Service

- [x] 2.1 Update `AddonDiscoveryService.java` to parse `environment` constraints from `addon.yml`
- [x] 2.2 Update `AddonDiscoveryService.java` to parse `capabilities` list from `addon.yml`
- [x] 2.3 Update `AddonDiscoveryService.java` to parse `load-before` and `load-after` from `addon.yml`
- [x] 2.4 Update `AddonDiscoveryService.java` to parse string metadata (`name`, `description`, `authors`, `website`)
- [x] 2.5 Ensure fallbacks and validation (regex, null checks) are correctly implemented for new fields

## 3. Implement Capabilities and Environment Auditing

- [x] 3.1 Update `AddonAuditService` to validate the `environment` constraints against the host server configuration
- [x] 3.2 Update `AddonAuditService` to fail the addon if required capabilities are not declared but are attempted to be used

## 4. Refine Dependency Resolution

- [x] 4.1 Update topological sort or DAG to account for `load-before` and `load-after` soft rules
- [x] 4.2 Verify that `load-before`/`load-after` rules affect load order but DO NOT cause dependency-related failures if target addon is missing
- [x] 4.3 Address potential cyclic dependency handling in the sort (e.g. A loads before B, but B is a hard dependency of A)
