---
name: All file contents in English
description: Everything written to project files must be in English, regardless of chat language
type: feedback
---

All content written to files in this project MUST be in English. This includes README.md, all `.claude/docs/*.md`, `.claude/memory/*.md`, JavaDoc, TSDoc, OpenAPI annotations, code comments, and any other file content. This applies even when the chat conversation with the user is in Russian.

**Why:** the user explicitly requested it. The project's `CLAUDE.md` also mandates English for JavaDoc / TSDoc / OpenAPI; the user extended this to *all* file content for consistency.

**How to apply:** when writing or editing any file (docs, code, comments, configs with human-readable text), use English. Only spoken/written replies to the user in the chat follow the chat language (Russian).
