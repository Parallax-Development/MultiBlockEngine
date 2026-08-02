## ADDED Requirements

### Requirement: Scoped Addon Translation Interface
The API MUST expose a scoped `AddonI18n` interface in `dev.darkblade.mbe.api.i18n` that automatically binds translation calls to the specific addon's ID (`origin`), accessible via `AddonContext.i18n()`.

#### Scenario: Sending localized message from AddonContext
- **WHEN** an addon invokes `context.i18n().send(player, "messages.welcome")`
- **THEN** the system SHALL resolve the translation key using `origin = addonId` and `path = "messages.welcome"` and dispatch the formatted message to the player.

#### Scenario: Resolving multi-line lore list
- **WHEN** an addon calls `context.i18n().trList(sender, "item.lore", params)`
- **THEN** the system SHALL return a list of formatted strings translated for the sender's locale with parameter placeholders replaced.

### Requirement: Automatic Addon Language Resource Extraction
The addon lifecycle manager SHALL automatically extract bundled default language `.yml` files from the addon's JAR into the addon's configured lang directory on disk if they do not exist.

#### Scenario: Extracting missing lang resources on addon load
- **WHEN** an addon is loaded and contains `lang/en_us.yml` inside its JAR but the file is missing from disk
- **THEN** MBE SHALL save `lang/en_us.yml` into the addon's data directory and register the source with `YamlI18nService`.

### Requirement: Configurable Addon Lang Directory
The `AddonContext` interface MUST provide `getLangDirectory()` and `setLangDirectory(Path folder)` methods allowing addons to customize their translation file directory.

#### Scenario: Setting custom lang directory
- **WHEN** an addon invokes `context.setLangDirectory(context.getDataFolder().resolve("locales"))` during initialization
- **THEN** MBE SHALL extract resources and register translation files from the specified `locales` directory instead of the default `lang/`.

### Requirement: Dual MiniMessage and Legacy Color Rendering
The translation engine SHALL format message templates by transparently rendering both Kyori MiniMessage tags and legacy Bukkit ampersand (`&` / `§`) color codes.

#### Scenario: Formatting template containing both MiniMessage and Legacy codes
- **WHEN** a translation template contains `"&a[MBE] <gold><bold>Status:</bold></gold> {status}"`
- **THEN** the engine SHALL process both MiniMessage formatting and legacy color codes into a single seamlessly formatted message.

### Requirement: Service Injection of AddonI18n
The `ServiceInjector` SHALL support field injection of `@InjectService AddonI18n` fields by resolving a scoped `AddonI18n` instance bound to the field target's owner addon ID.

#### Scenario: Injecting AddonI18n into an addon component
- **WHEN** an addon component class declares `@InjectService private AddonI18n i18n;` and is injected by `ServiceInjector`
- **THEN** the field SHALL be populated with a valid `AddonI18n` instance bound to that addon's ID.

### Requirement: Standardized Placeholder Resolution
The system SHALL support standardized placeholder formats across the ecosystem: `{param}` for Java contextual variables, `%i18n:key%` for YAML references, and `%mbe_<query>%` for PlaceholderAPI expansions.

#### Scenario: Resolving YAML inline i18n placeholders
- **WHEN** a UI panel model or item definition contains `%i18n:gui.title%`
- **THEN** MBE SHALL resolve `%i18n:gui.title%` using the contextual `I18nService` for the viewing player.
