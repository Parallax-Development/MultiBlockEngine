---
description: Analyzes project changes across plugin (api/core) or addons, performs build & standards audit, and commits atomically according to AGENTS.md.
---

# Agent Commit & Audit Workflow

Analyzes modified files in the working directory and Git submodules, executes compilation safety checks, audits architectural compliance against `AGENTS.md`, and creates structured, atomic Conventional Commits.

**Usage:** `/agent-commit-workflow`

---

## CRITICAL: Git Submodule Awareness for Addons

Addons located in `addons/` (`addons/mbe-crafting`, `addons/mbe-electrics`, `addons/mbe-ui`, `addons/mbe-wiring`) are **independent Git submodules**.
- **DO NOT** rely solely on `git status` or `git status --short addons/` from the root repository to detect changes inside addons.
- **ALWAYS** inspect each submodule directory directly using `git -C addons/<addon-name> status` or run `git submodule foreach 'git status --short'`.
- All staging (`git add`) and commit (`git commit`) operations for an addon **MUST** be executed within that submodule's working context (using `git -C addons/<addon-name> ...`).
- After committing inside an addon submodule, stage and commit the updated submodule commit pointer in the root repository (`git add addons/<addon-name>` and `git commit -m "chore(submodule): update <addon-name> pointer"`).

---

## Workflow Steps

### 1. Scope Selection
Prompt the user interactively (using `ask_question`) to specify which modules should be analyzed and committed:
- **The plugin**: Focus exclusively on `api/` and `core/` modules.
- **The addons**: Focus exclusively on modules inside `addons/` (evaluating each submodule individually).
- **Entire project**: Analyze all modified files across the main repository and all Git submodules.

### 2. Branch Safety Verification
1. Run `git branch --show-current` to check the active Git branch in the main repository (and in submodules if analyzing addons).
2. If the current branch is `main`:
   - Prompt the user to create a new feature branch (e.g., `feature/<scope>-<summary>`).
   - If the user declines to create a new branch, automatically switch to or use the **`dev`** branch by default.
3. If the active branch is already a feature/dev branch, proceed normally.

### 3. Pre-Commit Compilation Check
1. Execute `./gradlew.bat compileJava`.
2. Evaluate the build result:
   - **Build Success**: Proceed to Step 4.
   - **Build Failure**:
     - Extract clean error details and stack trace from the output.
     - Present the compilation errors to the user.
     - Prompt the user whether to spawn a fix subagent (`invoke_subagent`) to resolve the errors before continuing.
     - If the user declines, pause the workflow gracefully.

### 4. Domain & Architectural Compliance Audit
Audit all modified files within the selected scope against `AGENTS.md` and repository skills:
- **`api` Layer**: Ensure no concrete business logic or direct Bukkit imports exist. Verify interface contracts only.
- **`core` Layer**: Verify business logic is encapsulated within services (`MBEService`), using event-driven flows for decoupling.
- **`addons` Layer**: Ensure no rigid inter-addon dependencies exist without proper service abstraction (`CrossReferenceResolver`).

### 5. Atomic Conventional Commit Plan
1. Group modified files by logical module and impact (`feat`, `fix`, `chore`, `docs`, `refactor`).
2. Format commit messages adhering to `git-branch` guidelines:
   - Main repo plugin: `feat(api): ...`, `fix(core): ...`
   - Addon submodules: `feat(ui): ...`, `fix(electrics): ...` (executed via `git -C addons/<addon-name> commit -m "..."`)
   - Root repo submodule pointer update: `chore(submodule): update <addon-name> pointer`
3. Present the proposed commit plan to the user for final confirmation.

### 6. Execution & Verification
1. For changes in the main repository (`api/`, `core/`):
   - Stage files: `git add <files>`
   - Commit: `git commit -m "<message>"`
2. For changes in an addon submodule (`addons/<addon-name>`):
   - Stage inside submodule: `git -C addons/<addon-name> add <files>`
   - Commit inside submodule: `git -C addons/<addon-name> commit -m "<message>"`
   - Stage submodule pointer in root repo: `git add addons/<addon-name>`
   - Commit pointer update in root repo: `git commit -m "chore(submodule): update <addon-name> pointer"`
3. Verify final state across root and submodules (`git status` and `git submodule foreach 'git status'`).
