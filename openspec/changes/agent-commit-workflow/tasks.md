## 1. Scope Selection & Branch Safety

- [x] 1.1 Implement interactive module scope prompt (`ask_question`) offering options for "El plugin" (`api/` + `core/`), "Los addons" (`addons/`), or "Todo el proyecto"
- [x] 1.2 Implement branch safety check to detect if active branch is `main`, prompt for `feature/<scope>-...` branch, and fallback to `dev` branch if declined

## 2. Pre-Commit Build & Subagent Error Handling

- [x] 2.1 Integrate `./gradlew.bat compileJava` build verification step
- [x] 2.2 Add error log extraction and interactive prompt offering to launch a subagent to repair compilation errors

## 3. Domain Rules & Conventional Commit Formatting

- [x] 3.1 Implement `AGENTS.md` compliance audit for `api/` purity, service-oriented architecture in `core/`, and decoupled inter-addon references
- [x] 3.2 Implement atomic Conventional Commit bundler and preview report for user confirmation
- [x] 3.3 Execute atomic `git add` and `git commit` sequence based on approved plan
