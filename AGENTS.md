# Tinkers Fabric: agent guide

Unofficial Fabric port of Tinkers' Construct + Mantle for Minecraft 26.3 (Java 25, Gradle multi-project: `mantle/`, `tconstruct/`). Windows: use `gradlew.bat`.
`reference/` holds upstream clones (git-ignored, read-only): port by reading a subsystem and rewriting it; never bulk-copy Forge/NeoForge code. Unported features go in PORTING.md.
Versions live only in `gradle.properties` + the wrapper. `scripts/env/` = clean/restore machine setup, `scripts/dev.*` = build/test/run; both read those versions.

This file loads every session (CLAUDE.md imports it). Keep it under 70 lines. `.md_references/` is local-only (git-ignored); without it, read PORTING.md.

## Read order (`.md_references/`, number = read order)

1. `01_last_communication.md`: always, first.
2. `02_for_ai.md`: before changing code (commands, mental model, invariants, gotchas).
3. `03_code_style.md`: before writing code.
4. Open work only: `grep -v ",done,\|,fixed,\|,wontfix," .md_references/04_project_task.csv .md_references/05_project_issues.csv`
5. `06_code_comment_index.csv`: grep it, never read it whole. `grep -i "<word>" .md_references/06_code_comment_index.csv`, then read only lines `code_from` to `code_to` of the file it names.
6. Higher numbers only when the task touches them.
7. `10_security_guide.md`: before touching dependencies, secrets, config, auth, DB, storage, packaging or deploy.
8. `18_system_design.md`: before adding a component, changing data flow, the data model or an interface, or revisiting a D-nn decision.

## Change loop (every code change)

0. New task: `python .md_references/tools/forge.py branch --files <planned files> --type <feat|fix|refactor|spike|...> --slug <name> --task <T-nn>`. Follow Branching below.
1. Edit, following `03_code_style.md`. First line of every comment: a summary of 8 words or fewer.
2. Verify with the commands in `02_for_ai.md`. Stop on red.
3. `python .md_references/tools/forge.py index --files <changed files>`
4. Update memory rows the change touches (table below).
5. Update README.md, AGENTS.md or CLAUDE.md only if a row below says so.
6. `python .md_references/tools/forge.py scan` must report 0 errors.
7. Commit: automatic. After steps 1 to 6 pass, commit code and docs together, one logical change per commit. Subject `type(scope): summary` (feat, fix, refactor, docs, test, chore, sec), body says why, footer lists refs (T-07, A-003). Never commit on red tests or scan errors; record why in `01_last_communication.md` instead.

## Doc sync

| Change | Update |
|---|---|
| Decision or finding | `07_devjournal.md` (dated, max 3 lines) |
| Task started or done | `04_project_task.csv` |
| Bug found or fixed | `05_project_issues.csv` (A-nnn fixed by agent, I-nnn open) |
| Test added or run | `08_test_cases.md` |
| Command, toolchain, invariant or gotcha | `02_for_ai.md` (commands also `09_quick_test_run.md`) |
| New domain term or naming rule | `03_code_style.md` glossary |
| Install, usage or user-visible feature | `README.md` |
| Env var, secret, config key, dependency, packaging | `10_security_guide.md` inventory (names only) |
| Version bump (Java, MC, Fabric, Gradle) | `gradle.properties` / wrapper only; scripts follow; log in `07` |
| New component, interface, data model or architecture decision | `18_system_design.md` (section and a D-nn decision row); devjournal cites D-nn |
| Folder layout or agent workflow | this file |
| Claude-only harness rule | `CLAUDE.md` |
| End of a reply that changed things | `01_last_communication.md` (replace State and Next, append to Asked) |

## Branching (always)

- `suggest` (policy always): create the branch without asking, one per task, and log it in `01`.
- `stay`: work on the current branch, no question asked.
- On a branch: record its name in the task row notes and in `01` State. When done (tests green, scan clean), ask before merging or pushing; the `done:` line from `forge.py branch` has the exact commands. Delete a branch only after its merge, and only with a yes.
- Never rebase, force-push or rewrite shared history without asking.

## Rules

- Edit or append memory files; never rewrite them whole. One line per fact.
- Never hand-edit `06_code_comment_index.csv`; `forge.py index` owns it.
- Reference by path and ID (T-07, A-003); do not paste file contents into docs or replies.
- Say plainly what was not run. Never claim a test passed that did not run.
- Never push, publish, or create or delete remotes without asking.
