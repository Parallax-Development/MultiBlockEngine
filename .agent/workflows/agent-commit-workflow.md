---
description: Analyzes project changes across plugin (api/core) or addons, performs build & standards audit, and commits atomically according to AGENTS.md.
---

# Agent Commit & Audit Workflow

Analyzes modified files in the working directory, executes compilation safety checks, audits architectural compliance against `AGENTS.md`, and creates structured, atomic Conventional Commits.

**Usage:** `/agent-commit-workflow`

---

## Workflow Steps

### 1. Scope Selection
Prompt the user interactively (using `ask_question`) to specify which modules should be analyzed and committed:
- **El plugin**: Focus exclusively on `api/` and `core/` modules.
- **Los addons**: Focus exclusively on modules inside `addons/`.
- **Todo el proyecto**: Analyze all modified files across the entire repository.

### 2. Branch Safety Verification
1. Run `git branch --show-current` to check the active Git branch.
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
   - `feat(api): ...`
   - `fix(core): ...`
   - `feat(addons/<addon-name>): ...`
   - `chore(workflow): ...`
3. Present the proposed commit plan to the user for final confirmation.

### 6. Execution & Verification
1. Stage files atomically for each commit block using `git add <files>`.
2. Commit each block with its corresponding message using `git commit -m "<message>"`.
3. Verify the final repository state using `git status` and report the clean state to the user.
