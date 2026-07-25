---
name: git-branch
description: Define branching guidelines for new features. Invoke when starting new development, synchronizing changes, and preparing a merge.
---

## Purpose
Establish a clear strategy for creating, maintaining, and closing new feature branches without degrading repository stability.

## Trigger
- Starting a new feature
- Breaking down an epic's work into manageable branches
- Preparing a feature's integration into the base branch
- Resolving drift from long-running branches

## Scope
- Naming conventions for feature, fix, and hotfix branches
- Using Conventional Commits to automate SemVer
- Expected branch size and duration
- Synchronization with the base branch
- Rules for merging and closing branches

## Non-goals
- Defining the complete release policy
- Replacing quality checks or code reviews
- Covering production incident hotfix flows

## Rules
- **Conventional Commits**: Use prefixes like `feat:`, `fix:`, `chore:`, `docs:`, `BREAKING CHANGE:` in commits to automate SemVer version control.
- **Features**: Create branches from the stable branch (e.g., `main`). Name them as `feature/<scope>-<short-summary>`. This leads to a MINOR bump.
- **Fixes**: Create branches from the stable branch. Name them as `fix/<scope>-<short-summary>` or `bugfix/...`. This leads to a PATCH bump.
- **Hotfixes**: Create branches from the stable tag in production. Name them as `hotfix/<scope>-<short-summary>`.
- Keep branches small and focused on a single goal.
- Synchronize frequently with the base branch to reduce conflicts.
- Avoid direct commits to the main branch.
- Close and delete the branch after an approved merge.

## Required checks
- The branch clearly describes the business or technical goal.
- The feature does not mix unrelated changes.
- The branch is up-to-date with the base branch before merging.
- The merge maintains a traceable history and has an approved review.

## Failure modes
- Overly long branches with escalating conflicts
- Mixed features within a single branch
- Ambiguous and untraceable branch names
- Late integrations with a high risk of regression

## Test checklist
- Verify that the branch compiles and passes required checks
- Verify that commits maintain clear context per change
- Verify that the final diff is focused solely on the declared feature
- Verify that the integration does not break existing tests

## Implementation checklist
- Create the branch from the correct base
- Make atomic commits with clear messages
- Re-sync with the base before opening or updating a PR
- Complete the review, merge, and delete the branch

## Example: do vs avoid
- Do: `feature/catalog-filter-by-addon`, `fix/missing-dependency`, `hotfix/crash-on-startup`
- Avoid: `new-thing`, `feature/fix-and-feature-and-refactor`

## Patterns
- Short-lived feature branches
- Single-responsibility branch scope
- Frequent integration

## Anti-patterns
- Long-running branches without synchronization
- Mega-branches containing multiple features
- Feature work directly on the main branch
