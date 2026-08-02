## Context

The `Blueprint` item uses a translation configuration (`items.yml`) containing placeholder variables such as `{multiblock_display_name}`, `{count}`, and `{total}`. These variables are meant to dynamically display the multiblock's status in the item lore.
Currently, when `PdcItemStackBridge` translates the item's name and lore via `I18nService.resolve()`, it does not supply the `ItemInstance.data()` properties map, meaning the translation engine (`MessageTemplate`) leaves these placeholders unparsed.

## Goals / Non-Goals

**Goals:**
- Ensure that item state variables are successfully passed to the translation engine when generating the localized text for items.
- Specifically fix the blueprint item so it correctly displays the display name, construction count, and total blocks.

**Non-Goals:**
- We are not refactoring the `I18nService` or how item definitions work.
- We are not altering the underlying blueprint multiblock definition structures.

## Decisions

- **Pass `instance.data()` to `i18n.resolve` in `PdcItemStackBridge`**: 
  Instead of calling `i18n.resolve(key, target)`, the bridge will invoke `i18n.resolve(key, target, instance.data())`. This automatically maps any data keys (like `multiblock_display_name`) to the placeholders in the translation string.
- **Inject Blueprint variables into `BlueprintItem` creation**: 
  When `BlueprintItem.create` is called, we will ensure that `multiblock_display_name`, `count`, and `total` are placed in the `Map` sent to the item factory, alongside `DATA_STRUCTURE_ID`.
  - `multiblock_display_name`: can be resolved using the localized display name of the multiblock definition (if available) or falling back to the raw ID.
  - `count`: defaults to `0` initially.
  - `total`: retrieved from `definition.blocks().size()` or similar properties of the definition.

## Risks / Trade-offs

- **Risk**: Injecting the entire `instance.data()` map into the translation parser could accidentally overlap with standard translation keys if names clash.
- **Mitigation**: Blueprint data keys like `count`, `total`, and `multiblock_display_name` are standard and well-understood by the `MessageTemplate`. This is the intended usage pattern for dynamic item lore in the engine.
