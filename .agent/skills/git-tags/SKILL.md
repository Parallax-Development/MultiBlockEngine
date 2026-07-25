---
name: git-tags
description: Define the Git tag structure for releases. Invoke when creating or validating release, pre-release, and hotfix tags.
---

## Purpose
Define a stable convention for creating readable, traceable Git tags that align with the project's versioning.

## Trigger
- Creating a tag for a stable release
- Creating a tag for a pre-release
- Tagging a production hotfix
- Auditing existing tags for consistency

## Scope
- Release tag format
- Pre-release tag format
- Hotfix convention
- Immutability and annotation rules

## Non-goals
- Replacing SemVer versioning rules
- Defining branch naming
- Designing complete CI/CD policies

## Rules
- Use the `v` prefix for releases and pre-releases: `vMAJOR.MINOR.PATCH`
- For pre-releases, use `vMAJOR.MINOR.PATCH-<channel>.<n>` with channels `alpha`, `beta`, `rc`
- For immediate hotfixes, use the new PATCH version and standard tag format, without a parallel structure
- Create annotated tags, not lightweight tags
- A tag points to an immutable release commit
- Never move or rewrite a published tag

## Required checks
- The tag exactly matches the built artifact version
- The tagged commit corresponds to the approved release state
- The tag format complies with the expected convention
- The tag does not already exist remotely

## Failure modes
- Tags without prefixes mixed with prefixed tags
- Reusing existing tags for other commits
- Tagging commits without a verifiable build
- Pre-releases with an invalid channel or sequence

## Test checklist
- Verify the ordered list of tags and consistent formatting
- Verify tag resolution to the correct commit
- Verify that the associated release exists and is downloadable
- Verify traceability between the tag, changelog, and build version

## Implementation checklist
- Confirm the final version of the cycle
- Create an annotated tag with a release message
- Push the tag to remote
- Validate remotely that the tag is visible and correct

## Example: do vs avoid
- Do: `v2.3.0`, `v2.4.0-rc.1`
- Avoid: `release-2.3`, `2.3.0`, `hotfix-final`

## Patterns
- Tagging convention with prefix
- Annotated immutable tags
- Release traceability

## Anti-patterns
- Lightweight tags for official releases
- Overwriting already shared tags
- Mixing multiple naming conventions
