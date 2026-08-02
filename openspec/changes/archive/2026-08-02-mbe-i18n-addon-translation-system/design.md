## Context

MultiBlockEngine core provides `I18nService` and `YamlI18nService` for multi-locale message resolution. However, addons currently have no simple way to access translations. Addon developers are forced to manually handle `MessageKey.of(addonId, path)`, extract resources manually, or hardcode user messages in Java.

This design establishes a centralized, scoped translation architecture for addons, integrating deeply with `AddonContext`, `ServiceInjector`, `YamlI18nService`, and UI/Item placeholder systems.

## Goals / Non-Goals

**Goals:**
- Provide a clean, scoped `AddonI18n` interface in `api` bound to the addon's ID (`context.i18n()`).
- Support `@InjectService AddonI18n` field injection in addon components via `ServiceInjector`.
- Implement automatic extraction of bundled `lang/*.yml` resources from addon JARs into the configured lang directory (default `lang/`, configurable via `context.setLangDirectory(...)`).
- Enhance `YamlI18nService` to render Kyori MiniMessage formatting (`<green>`, `<bold>`, etc.) and legacy ampersand codes (`&a`, `&l`) transparently.
- Add lore list resolution (`trList(...)`).
- Standardize placeholder syntax across MBE into `{param}`, `%i18n:key%`, and `%mbe_<query>%`.

**Non-Goals:**
- Replacing paper/bukkit client translation engines entirely.
- Cloud-based live translation backends.

## Decisions

### Decision 1: Scoped `AddonI18n` Interface & `AddonContext.i18n()`
Add `AddonI18n` interface under `dev.darkblade.mbe.api.i18n`. `AddonI18n` wraps `I18nService` and automatically supplies the addon's `origin` to all key resolutions.
Expose `context.i18n()` on `AddonContext`.

### Decision 2: Scoped Injection in `ServiceInjector`
Modify `ServiceInjector` so that when encountering `@InjectService AddonI18n i18n` or `@InjectService Optional<AddonI18n> i18n`:
- `ServiceInjector` uses the target component's `ownerId` (Addon ID) to instantiate `new ScopedAddonI18n(coreI18nService, ownerId)`.
- Eliminates manual boilerplate in addon classes.

### Decision 3: Automatic JAR Resource Extraction & `setLangDirectory`
Extend `AddonContext` with:
- `Path getLangDirectory()`
- `void setLangDirectory(Path folder)`

During `AddonLifecycleService` loading phase, scan the addon's JAR resources under the configured relative lang path (e.g. `lang/`) for `.yml` files. If missing from `plugins/MultiBlockEngine/addons/<AddonId>/lang/`, save them to disk and register the source with `YamlI18nService`.

### Decision 4: Dual MiniMessage + Legacy Color Rendering Engine
Enhance `YamlI18nService` and `MessageTemplate` rendering pipeline:
1. Replace `{param}` variables in the message template.
2. Attempt Kyori MiniMessage deserialization for tags like `<red>`, `<gradient:red:blue>`.
3. Process Bukkit ampersand color codes (`&a`, `&l`) transparently.
4. Output Adventure `Component` for modern clients or legacy string for Bukkit `sendMessage`.

### Decision 5: Placeholder Syntax Standardization
Estandarize placeholders across MBE:
1. `{param}`: Java context variables passed in `Map<String, ?> params` (e.g. `{count}`, `{required_power}`).
2. `%i18n:key%`: Inline translation keys in YAML configurations (`inventories.yml`, `items.yml`). Parsed via `I18nService` when rendering UIs or building ItemStacks.
3. `%mbe_<query>%`: PlaceholderAPI expansion placeholders.

## Risks / Trade-offs

- [Risk: Invalid MiniMessage syntax in user YAML] → Catch parsing exceptions in `MessageTemplate` and fallback gracefully to ampersand color translation without throwing runtime errors.
- [Risk: Missing translation file in Addon JAR] → If an addon provides no `lang/*.yml` files, log a debug message and fallback gracefully to `origin:path` key representation.
