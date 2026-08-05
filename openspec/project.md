# SC2 Coach AI — Agent Project Context

## Purpose

SC2 Coach AI turns an explicitly supplied StarCraft II replay into an explainable, evidence-backed coaching report. The deterministic analysis core must remain reproducible and must not depend on nondeterministic LLM output.

## Sources of truth

Before planning or implementation, read:

1. `docs/PROJECT_STATE.md`
2. `docs/DECISIONS.md`
3. `ROADMAP.md`
4. `ARCHITECTURE.md`
5. the complete active change directory under `openspec/changes/`
6. every current capability specification referenced by that change
7. relevant open pull requests and branch state

Repository files and current Git state override conversational memory and Telegram fragments.

## Git workflow

- Primary integration branch: `develop`.
- Production branch: `main`.
- Ordinary feature and bug-fix PRs target `develop` directly.
- One feature branch per active change.
- Stacked PRs are forbidden unless explicitly authorized.
- Never commit implementation directly to `develop` or `main`.

## Architecture constraints

- Python owns Blizzard replay decoding and low-level extraction.
- `java/coach-domain` owns replay-independent domain concepts and deterministic analysis.
- `java/portal` owns orchestration, HTTP and runtime concerns.
- `frontend` renders existing facts and conclusions; it does not invent match causality.
- Facts, deterministic derivations and heuristics must remain distinguishable.
- Recommendations must carry evidence and confidence.
- Missing replay data must not be silently reconstructed.

## Change lifecycle

Each material change is represented by a directory under `openspec/changes/<change-id>/` containing:

- `proposal.md` — problem, outcome and scope;
- `design.md` — architecture and trade-offs when relevant;
- `tasks.md` — executable checklist;
- `specs/**/spec.md` — required capability behaviour when behaviour changes.

The change directory is the complete task packet. Telegram is only a transport for the change ID and explicit execution command.
