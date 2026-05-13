---
name: Memory storage location
description: Store all memory, plans, and docs for this project inside the repo under .claude/
type: feedback
---

All memory files, plans, and documentation for this project must be stored inside the repository:
- Memory: `.claude/memory/`
- Plans: `.claude/plans/`
- Docs: `.claude/docs/`

**Why:** the user wants these files to be visible and version-controlled inside the project, not hidden in `~/.claude/projects/…/`.

**How to apply:** when saving any new memory file, write it to `.claude/memory/` inside the project and update `.claude/memory/MEMORY.md`. Never write memory to `~/.claude/projects/…/memory/`.
