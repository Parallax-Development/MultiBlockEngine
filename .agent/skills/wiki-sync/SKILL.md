---
name: wiki-sync
description: Skill to synchronize source code changes with wiki documentation using predefined mappings.
---

# Wiki Sync Skill

This skill defines the agent's behavior when the user seeks to update the wiki documentation to reflect recent code changes (`/wiki-sync`).

## Critical Rules

1. **Never modify the wiki directly without approval:** Your main goal is to propose changes. Generate a draft (e.g., `wiki_updates_draft.md`) with the proposals and ALWAYS explain the reasoning behind each change, comparing the state of the code with what the wiki currently says.
2. **Use the mapping file:** Always read `mapping.yml` in this same folder to understand which code files correspond to which wiki pages.
3. **Do not write code:** This skill is exclusively for updating documentation.
4. **Respect the Wiki structure:** The wiki files live in `.\WIKI\MultiBlockEngine.wiki\`.

## How to use `mapping.yml`

The `mapping.yml` file contains a list of objects under the `mappings` key. Each object has:
- `paths`: A list of file path patterns (glob style or simple substrings/directories).
- `wiki_files`: A list of `.md` file names within the wiki that are affected by changes in those paths.

When analyzing the files returned by `git diff`, check if their path matches any of the `paths` and, if so, add the corresponding `wiki_files` to your list of pages to review.
