## Why

The blueprint item relies on placeholder variables (`{multiblock_display_name}`, `{count}`, `{total}`) in its localized lore to display dynamic state to the user. However, when the `PdcItemStackBridge` converts the `ItemInstance` into an `ItemStack`, it does not pass the item's state variables (`instance.data()`) to the `I18nService`. As a result, the placeholders are left unparsed (or displayed raw), which negatively impacts the user experience and breaks the intended design.

## What Changes

- Modify `PdcItemStackBridge` to pass `instance.data()` as parameters when resolving text translations for item name and lore.
- Ensure that the blueprint variables (`multiblock_display_name`, `count`, `total`) are correctly injected into the `ItemInstance.data()` when the blueprint item is created in `BlueprintItem.create` or related components.
- No translation keys need to be added or removed; this is strictly an infrastructure fix to correctly propagate item state to the translation engine.

## Capabilities

### New Capabilities

*(None)*

### Modified Capabilities

*(None)*

## Impact

- **Affected code**: `PdcItemStackBridge` (core bridge implementation), `BlueprintItem` (item creation), and potentially any other system relying on item lore placeholders.
- **Dependencies/Systems**: Core I18n translation engine (`YamlI18nService`) and Item generation systems.
