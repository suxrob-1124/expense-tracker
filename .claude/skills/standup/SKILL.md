---
name: standup
description: Generates a daily standup report from yesterday's git commits. Run manually before a standup meeting to get a structured summary of completed work.
model: claude-sonnet-4-6
effort: low
user_invocable: true
allowed-tools: Bash(git:*), Bash(date:*)
---

# Standup Report Generator

Generate a structured standup report based on yesterday's commits in the current repository.

## Steps

1. Determine yesterday's date:
   ```bash
   date -v-1d +%Y-%m-%d   # macOS
   # or
   date -d yesterday +%Y-%m-%d  # Linux
   ```

2. Fetch all commits authored yesterday (use the git user's local timezone):
   ```bash
   git log --oneline --no-merges --after="YESTERDAY 00:00" --before="YESTERDAY 23:59" --format="%H %s"
   git log --no-merges --after="YESTERDAY 00:00" --before="YESTERDAY 23:59" --format="%H|||%s|||%b|||%an"
   ```

3. For each commit, get the full diff stat to understand the scope:
   ```bash
   git show --stat <hash>
   ```

4. Compile and output the report following the format below.

## Output format

Print the report always in Russian.

```
## Стендап — <дата>

### Что сделано вчера

**<Краткий заголовок области изменений>**
- <конкретное действие 1>
- <конкретное действие 2>
...

**<Следующая область>**
- ...

### Метрика
- Коммитов: N
- Файлов изменено: M
- Добавлено строк: +X / Удалено строк: -Y

### Блокеры
(нет — если блокеров не обнаружено в коммитах)
```

## Rules

- Group related commits under a single heading (e.g., "Backend — transactions", "Frontend — UI fixes").
- Expand terse commit subjects into human-readable sentences — a non-technical person should understand what was done.
- Do NOT invent work that is not in the commits.
- If there are no commits yesterday, say so clearly and suggest checking the date/branch.
- Merge commits are excluded automatically (`--no-merges`).
- If the user passes an argument (e.g., `/standup 2026-05-12`), use that date instead of yesterday.
