## Why

Managing commits in MultiBlockEngine requires strict adherence to project standards defined in `AGENTS.md` (e.g. strict separation between `api` and `core`, service-oriented architecture, event-driven decoupling, and no direct Bukkit dependencies in API) as well as Git standards (Conventional Commits, atomic commits, SemVer, branch safety).

Creating an automated, interactive Agent Commit Workflow streamlines project maintenance by performing automated scope filtering, branch protection checks, pre-commit compilation validation, subagent error remediation, and atomic conventional commit packaging.

## What Changes

- Introduce a structured Agent Workflow for analyzing and committing changes across MultiBlockEngine.
- Interactive Scope Selection: Allow the user to choose target modules ("El plugin" (`api/` + `core/`), "Los addons" (`addons/`), or "Todo el proyecto").
- Branch Protection: Enforce branch policies by checking if currently on `main`. Prompt for `feature/...` branch creation, falling back to `dev` by default if declined.
- Build & Health Validation: Execute `./gradlew.bat compileJava` prior to committing. On compilation failure, present full error details and offer to spawn a fix subagent.
- Domain & Standards Audit: Verify compliance with `AGENTS.md` architectural constraints prior to committing.
- Atomic Conventional Commits: Group and format changes cleanly according to `git-branch` and `git-versioning` guidelines.

## Capabilities

### New Capabilities
- `agent-commit-workflow`: Automated, interactive agent workflow for auditing, building, and committing MultiBlockEngine modules.

### Modified Capabilities
*(None - existing capabilities requirements remain intact)*

## Impact

- Workflow & Agent Tooling: Adds standardized workflow procedures for repository management.
- Git & Build Integration: Interacts directly with `./gradlew.bat` and Git repository state.
