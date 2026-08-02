# agent-commit-workflow Specification

## Purpose
TBD - created by archiving change agent-commit-workflow. Update Purpose after archive.
## Requirements
### Requirement: Interactive Scope Selection
The workflow SHALL prompt the user to choose the target scope for analysis and commit.

#### Scenario: Scope selection presented
- **WHEN** the agent workflow is initialized
- **THEN** the user is presented with options for "El plugin" (`api/` + `core/`), "Los addons" (`addons/`), or "Todo el proyecto"

### Requirement: Branch Safety Enforcement
The workflow SHALL prevent direct commits to the `main` branch and fallback to `dev` if feature branch creation is declined.

#### Scenario: Branch check on main branch
- **WHEN** the current active branch is `main`
- **THEN** the workflow prompts to create a feature branch (`feature/<scope>-...`)
- **THEN** if the user declines, the workflow defaults to using the `dev` branch

### Requirement: Pre-Commit Build Verification
The workflow SHALL execute `./gradlew.bat compileJava` before formatting or executing commits.

#### Scenario: Build failure handling
- **WHEN** `./gradlew.bat compileJava` fails with errors
- **THEN** the workflow extracts compilation error logs, presents them to the user, and asks whether to spawn a fix subagent

### Requirement: Domain Standards Audit
The workflow SHALL audit uncommitted files against architectural constraints in `AGENTS.md`.

#### Scenario: API module purity check
- **WHEN** changes include files under `api/`
- **THEN** the workflow verifies there are no concrete business implementations or direct Bukkit imports in `api/`

### Requirement: Atomic Conventional Commits
The workflow SHALL group modifications into atomic commits adhering to Conventional Commits standards.

#### Scenario: Atomic commit proposal
- **WHEN** all build and audit checks pass
- **THEN** the workflow presents a structured list of conventional commits grouped by module (`feat(api): ...`, `fix(core): ...`, `chore(addons/name): ...`) for user confirmation

