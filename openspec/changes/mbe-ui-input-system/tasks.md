# Implementation Tasks: `mbe-ui-input-system`

## 1. Item Deposit Slots
- [x] 1.1 Extend `UiPanelModel.ItemDefinition` and `UiPanelYamlParser` to parse `type: input`, `allowed-materials`, `placeholder`, `on-deposit`, and `on-withdraw`.
- [x] 1.2 Update `UiInventoryRuntime` to un-lock `type: input` slots and validate placed items against `allowed-materials`.
- [x] 1.3 Implement variable binding for deposited items (`{{input.<slot_id>.material}}`, `{{input.<slot_id>.amount}}`) and safety item return on inventory close.

## 2. Chat Prompt Engine
- [x] 2.1 Implement `UiChatPromptService` listening to `AsyncPlayerChatEvent`.
- [x] 2.2 Add `prompt:` action parsing support to `UiPanelYamlParser` and `UiActionExecutor`.
- [x] 2.3 Implement prompt timeout, cancellation, variable binding, and panel reopening flow.

## 3. PacketEvents Provider & Anvil / Sign GUIs
- [x] 3.1 Integrate PacketEvents dependency and build `UiPacketInputService`.
- [x] 3.2 Implement Anvil text interceptor (`WrapperPlayClientNameItem`) and Sign editor interceptor (`WrapperPlayClientUpdateSign`).
- [x] 3.3 Implement automatic fallback to `UiChatPromptService` when PacketEvents is not present.

## 4. Verification & Testing
- [x] 4.1 Unit tests for item input parsing and variable resolution.
- [x] 4.2 Unit tests for chat prompt lifecycle and fallback mechanism.
- [x] 4.3 Full build verification via `./gradlew build`.
