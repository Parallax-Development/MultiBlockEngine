## ADDED Requirements

### Requirement: Blueprint item translations evaluate state variables
The system SHALL inject multiblock state variables into the translation engine when generating blueprint item lore.

#### Scenario: Translating lore with state variables
- **WHEN** generating a Blueprint item stack
- **THEN** variables `{multiblock_display_name}`, `{count}`, and `{total}` from the `items.yml` language file are replaced with their actual state values
