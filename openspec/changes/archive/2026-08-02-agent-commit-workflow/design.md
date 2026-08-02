## Context

MultiBlockEngine relies on clear domain boundaries (`api`, `core`, `addons/`) and strict adherence to architectural rules defined in `AGENTS.md` and repo skills (`git-branch`, `git-versioning`). This design specifies an end-to-end agent workflow to inspect uncommitted changes, execute pre-commit builds, audit architectural guidelines, and perform atomic Conventional Commits.

## Goals / Non-Goals

**Goals:**
- Provide an interactive scope prompt ("El plugin", "Los addons", "Todo el proyecto").
- Enforce branch safety when operating on `main` branch with fallback to `dev`.
- Validate project compilation with `./gradlew.bat compileJava` prior to committing.
- Support subagent spawning to debug and repair compilation errors when build fails.
- Perform domain audits against `AGENTS.md` rules (`api` purity, service injection, event-driven flow).
- Construct atomic conventional commits structured by scope (`api`, `core`, `addons/<name>`).

**Non-Goals:**
- Replacing standard Git CLI tools for arbitrary manual commits.
- Bypassing build validation or masking compilation failures.

## Decisions

### Decision 1: Interactive Scope Selection via UI Modal
- **Choice**: Use interactive prompt / modal (`ask_question` tool in agent execution) to select target modules (`Plugin` -> `api/` + `core/`; `Addons` -> `addons/*`; `All` -> workspace).
- **Rationale**: Ensures clear user intent and prevents accidental commits across un-reviewed modules.

### Decision 2: Pre-Commit Compilation Check & Subagent Remediation
- **Choice**: Execute `./gradlew.bat compileJava` before formatting or staging commits. If compilation fails, capture error logs, present them to the user, and offer to spawn a subagent to repair the build.
- **Rationale**: Eliminates broken commits on base/feature branches and provides automated debugging assistance.

### Decision 3: Branch Safety Enforcement with Fallback
- **Choice**: Check active branch (`git branch --show-current`). If on `main`, prompt the user to create a feature branch (`feature/<scope>-...`). If the user declines, automatically switch to/use `dev` branch by default.
- **Rationale**: Prevents direct commits on production/stable `main` branch while providing a seamless fallback.

### Decision 4: Domain & Architectural Rule Verification
- **Choice**: Audit staged/modified files against `AGENTS.md` constraints:
  - `api`: No Bukkit imports, no concrete business logic.
  - `core`: Services handle business logic; listeners delegate to services.
  - `addons`: Inter-addon references resolved via contracts/service registry.

## Risks / Trade-offs

- **[Risk] Long build times on large change sets** → **Mitigation**: Run fast compilation target (`compileJava`) rather than full test suite unless explicitly requested.
- **[Risk] Unresolved compilation errors by subagent** → **Mitigation**: Pause workflow gracefully, leaving workspace untouched so the user can intervene.
