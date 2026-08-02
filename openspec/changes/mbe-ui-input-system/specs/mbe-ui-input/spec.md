# Spec Delta: `mbe-ui-input`

## ADDED Requirements

### Requirement: Item Deposit Slots
The system MUST allow defining deposit slots in panel YAML configurations where players can insert or withdraw items while the rest of the GUI remains locked.

#### Scenario: Depositing a valid item into an input slot
- **WHEN** a player places an item into a slot defined with `type: input`
- **AND** the item matches the `allowed-materials` filter (if specified)
- **THEN** the system accepts the deposit, updates session variables `{{input.<slot_id>.material}}` and `{{input.<slot_id>.amount}}`, and executes `on-deposit` actions.

#### Scenario: Rejecting invalid item deposits
- **WHEN** a player attempts to place an item into a `type: input` slot that does not match `allowed-materials`
- **THEN** the system cancels the click event and retains the item in the player's cursor.

#### Scenario: Returning input items on panel close
- **WHEN** a player closes an inventory panel containing items in `type: input` slots
- **THEN** the system returns all deposited items to the player's inventory or drops them safely on the ground.

### Requirement: Chat Prompt Text Entry
The system MUST support prompt actions that request player text input via chat with configurable expiration and session variable binding.

#### Scenario: Submitting a chat prompt input
- **WHEN** a player with an active chat prompt types text into chat
- **THEN** the system cancels public chat broadcasting, stores the input text in the designated session variable, and executes `on-submit` actions.

#### Scenario: Cancelling a chat prompt input
- **WHEN** a player types "cancel" or the 30-second timeout elapses during a chat prompt
- **THEN** the system cancels the prompt and executes `on-cancel` actions.

### Requirement: PacketEvents Anvil & Sign GUIs
The system MUST support packet-level Anvil and Sign GUI text inputs via PacketEvents with fallback to chat prompts.

#### Scenario: Intercepting Anvil rename text via PacketEvents
- **WHEN** PacketEvents is enabled and a player confirms text in an Anvil input GUI
- **THEN** the system intercepts `WrapperPlayClientNameItem`, stores the result in the session variable, and re-opens the target panel.

#### Scenario: PacketEvents fallback when plugin is missing
- **WHEN** PacketEvents is not present on the server environment
- **THEN** the system automatically falls back to `UiChatPromptService` without throwing exceptions or failing panel load.
