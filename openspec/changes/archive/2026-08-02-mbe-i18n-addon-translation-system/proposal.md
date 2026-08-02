## Why

Currently, MBE addons lack a standardized, centralized, and convenient translation system. As a result, addons resort to hardcoding user-facing messages in Java code or creating ad-hoc messaging solutions.
Providing a first-class, centralized i18n system for addons will ensure consistent multi-language support, seamless MiniMessage + legacy color parsing, automatic resource extraction from addon JARs, scoped addon translation APIs, and a unified placeholder syntax across the entire MultiBlockEngine ecosystem.

## What Changes

- Expose a scoped `AddonI18n` service interface in `api` bound automatically to an addon's ID (`context.i18n()`).
- Support `@InjectService AddonI18n` injection in addon components via `ServiceInjector` scoped factory resolution.
- Extend `AddonContext` with `getLangDirectory()` and `setLangDirectory(Path folder)`, defaulting to `lang/`.
- Automatic JAR resource extraction during addon loading to export default translation files (e.g. `lang/en_us.yml`, `lang/es_es.yml`) to the addon's data folder.
- Enhance `YamlI18nService` dual-parser formatting to seamlessly render Kyori MiniMessage tags (`<green>`, `<bold>`) alongside legacy color codes (`&`, `§`).
- Add helper methods for multi-line lore/list translations (`trList(...)`).
- Standardize placeholder syntax across MBE into `{param}` for dynamic parameters, `%i18n:key%` for YAML references, and `%mbe_<query>%` for PlaceholderAPI.

## Capabilities

### New Capabilities
- `addon-i18n-system`: Centralized, scoped translation service for addons with automatic resource extraction, dual MiniMessage/Legacy formatting, `@InjectService` support, and standardized placeholder parsing.

### Modified Capabilities

## Impact

- `api`: New `AddonI18n` interface in `dev.darkblade.mbe.api.i18n`. Add `getLangDirectory()` and `setLangDirectory(Path)` methods to `AddonContext`.
- `core`: Enhance `YamlI18nService` for MiniMessage + Legacy color rendering, update `ServiceInjector` for scoped `AddonI18n` injection, and update `AddonLifecycleService` / `SimpleAddonContext` to handle automatic JAR `lang/` resource extraction.
- Addons: Addons can easily translate all messages, UI items, and lore without hardcoded strings or manual file management.
