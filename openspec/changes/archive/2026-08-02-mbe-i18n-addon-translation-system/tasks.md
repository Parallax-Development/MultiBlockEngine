## 1. API Contracts

- [x] 1.1 Create `AddonI18n` interface in `dev.darkblade.mbe.api.i18n` defining scoped translation and message dispatching methods (`tr`, `trList`, `send`).
- [x] 1.2 Extend `AddonContext` interface with `getLangDirectory()` and `setLangDirectory(Path folder)` methods.
- [x] 1.3 Add `AddonI18n i18n()` method to `AddonContext`.

## 2. Core Implementation & Service Scoping

- [x] 2.1 Implement `DefaultAddonI18n` in `core` wrapping `I18nService` with pre-bound `addonId`.
- [x] 2.2 Update `ServiceInjector` to support `@InjectService AddonI18n` field injection using the component's `ownerId`.
- [x] 2.3 Update `AddonLifecycleService` and `SimpleAddonContext` to manage `langDirectory` and automatically extract missing `lang/*.yml` files from addon JAR resources on load.

## 3. Dual MiniMessage & Legacy Rendering Engine

- [x] 3.1 Update `MessageTemplate` and `YamlI18nService` to render Kyori MiniMessage tags and legacy ampersand (`&` / `§`) color codes transparently.
- [x] 3.2 Add `trList(...)` implementation to `I18nService` and `YamlI18nService` for list and lore string array resolution.

## 4. Placeholder Standardization & Verification

- [x] 4.1 Implement `%i18n:key%` inline placeholder resolver helper for UI and item lore definitions.
- [x] 4.2 Add unit tests in `core` verifying `AddonI18n`, `ServiceInjector` scoped injection, dual color rendering, and placeholder resolution.
