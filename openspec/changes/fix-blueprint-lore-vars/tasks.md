## 1. Core Translation Implementation

- [x] 1.1 Update `PdcItemStackBridge#resolveText` method signature to accept `Map<String, Object> data`.
- [x] 1.2 Update `PdcItemStackBridge#parseLore` method signature to accept `Map<String, Object> data` and pass it to `resolveText`.
- [x] 1.3 Update `PdcItemStackBridge#toItemStack(ItemInstance, Locale)` to extract `instance.data()` and pass it to `parseLore` and `resolveText`.
- [x] 1.4 Ensure the item's display name translation in `PdcItemStackBridge` also receives the `instance.data()`.

## 2. Blueprint Integration

- [x] 2.1 Modify `BlueprintItem#create` (and overloads) to inject `multiblock_display_name`, `count` (0), and `total` (blocks size) into the `ItemInstance` data map alongside `DATA_STRUCTURE_ID`.
- [x] 2.2 Verify if `BlueprintCraftingServiceImpl` or `BlueprintCommand` requires any tweaks to provide correct arguments to `BlueprintItem.create` so it has access to the full definition data.
- [x] 2.3 Verify the placeholders are successfully parsed in-game using `/mbe blueprint give` or similar.
