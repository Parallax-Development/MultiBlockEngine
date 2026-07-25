---
description: Synchronizes the wiki information (`.\WIKI\MultiBlockEngine.wiki`) with the codebase by comparing the Git history.
---

# Wiki Sync Workflow

Synchronizes the wiki information (`.\WIKI\MultiBlockEngine.wiki`) with the codebase by comparing the Git history.

**Usage:** `/wiki-sync [commit-range]` or `/wiki-sync` and the agent will ask you.

## Steps

1. **Get the commit range:**
   - If the user does not provide a commit range or branch (e.g., `HEAD~5` or `main`), ask them against what they want to compare the recent changes.

2. **Analyze the Git changes:**
   - Execute git commands such as `git diff --name-only <range>` or `git log --name-only <range>` to get the list of modified files.
   - Filter the files to focus on relevant source code (e.g., `.java`, `.yml`).

3. **Map files to the Wiki:**
   - Read the `.agent/skills/wiki-sync/mapping.yml` file to find which wiki pages are associated with the modified files.
   - If there are modified files that have no correspondence in the mapping, notify briefly but continue.

4. **Read the Wiki content:**
   - Read the corresponding `.md` files inside `.\WIKI\MultiBlockEngine.wiki\`.

5. **Evaluate differences (The "Why"):**
   - Compare the logic implemented in the modified code files with the current text of the corresponding wiki pages.
   - Identify if the wiki mentions behaviors or architectures that are no longer true or are incomplete.

6. **Generate Changes Draft:**
   - Create a markdown artifact (`wiki_updates_draft.md`) with your findings.
   - For each page to be updated, include:
     - **Affected page:** The `.md` file name.
     - **Justification:** Clearly explain the difference between what the code currently reflects and what the wiki says.
     - **Change proposal:** A text block or diff with the suggested content for the wiki.

7. **Wait for Approval:**
   - Present the draft to the user and ask if they want you to apply these changes to the wiki files (using file editing tools) or if they prefer to do it manually.
