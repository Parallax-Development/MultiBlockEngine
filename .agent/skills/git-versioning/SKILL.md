---
name: git-versioning
description: Define SemVer versioning for the repo. Invoke when planning releases, hotfixes, or changes that alter compatibility.
---

## Purpose
Establish consistent versioning guidelines for the repository using SemVer and explicit compatibility rules.

## Trigger
- Preparing a release or pre-release
- Determining the impact of a new feature, fix, or refactor on the version
- Resolving backwards compatibility questions
- Defining the next version number after a batch of changes

## Scope
- SemVer (`MAJOR.MINOR.PATCH`)
- Pre-releases (`-alpha.N`, `-beta.N`, `-rc.N`)
- Build metadata (`+build.N`) when applicable
- Compatibility and deprecation policy

## Non-goals
- Replacing the branching or tagging strategy
- Defining the complete changelog for a release
- Covering infrastructure deployment policies

## Rules
- Use SemVer as the public contract for changes
- Increment `MAJOR` when there is a public compatibility break
- Increment `MINOR` when adding backwards-compatible functionality
- Increment `PATCH` for backwards-compatible bug fixes
- Maintain monotonic numbering without reusing published versions
- In pre-releases, maintain consistent prefixes (`alpha`, `beta`, `rc`)

## Required checks
- Each relevant change is classified as a breaking change, feature, or fix
- The proposed version matches the highest detected impact
- There is no identical prior version already published remotely
- If there is a breaking change, it is explicitly communicated in the release notes

## Failure modes
- Inflating the minor version for trivial changes without real impact
- Publishing a PATCH with a compatibility break
- Inconsistent version jumps between modules or artifacts
- Confusion caused by mixing pre-release conventions

## Test checklist
- Manual verification of public API compatibility after changes
- Verification that the packaged artifact uses the expected version
- Verification that release scripts consume the correct version
- Verification of the chronological order of published versions

## Implementation checklist
- Classify changes from the current cycle by impact
- Select the next version according to the highest impact rule
- Update the version in build files
- Validate that release tags and branches reflect the chosen version

## Example: do vs avoid
- Do: add a compatible endpoint and bump from `1.4.2` to `1.5.0`
- Avoid: change a public contract and bump from `1.4.2` to `1.4.3`

## Patterns
- Semantic Versioning
- Explicit compatibility policy
- Increment strategy by impact

## Anti-patterns
- Version bumping by intuition without classifying changes
- Breaking contracts in PATCH/MINOR releases
- Reusing an already published version
