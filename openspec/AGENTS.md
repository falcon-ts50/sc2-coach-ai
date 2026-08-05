# Agent Execution Protocol

## Transmission rule

A Telegram message is never the complete specification for a material task. It may only identify an active change and authorize a lifecycle step.

Do not start work from partial copied instructions. Do not infer that the final Telegram fragment has arrived.

## Mandatory read gate

Before planning, editing code or changing task status, read all of:

1. `openspec/project.md`;
2. `docs/PROJECT_STATE.md`;
3. `docs/DECISIONS.md`;
4. `ROADMAP.md`;
5. `ARCHITECTURE.md`;
6. every file in the named active change directory;
7. all files explicitly referenced by that change;
8. relevant current implementation and tests;
9. relevant open PR state.

After reading, respond with a Read Gate report containing:

- active change ID;
- base branch and intended PR target;
- files read;
- understood objective;
- in-scope items;
- explicit non-goals;
- unresolved contradictions or missing inputs;
- next lifecycle step.

Until the Read Gate is complete, no production file may be modified.

## Lifecycle gates

### REVIEW

Inspect the repository and complete only analysis/design tasks. Do not modify production code.

### APPLY

Implementation is allowed only when the change artifacts are internally consistent and implementation tasks are explicitly marked ready.

### VERIFY

Run required validation, compare implementation with every requirement and scenario, and document deviations. Do not hide failing or unavailable checks.

### ARCHIVE

Archive only after the implementation PR has merged and current capability specifications have been synchronized.

The Telegram command must explicitly name one lifecycle gate. When no gate is named, default to `REVIEW`.

## Scope control

Implement only behaviour explicitly required by the active change.

Do not:

- invent product requirements;
- silently change module boundaries;
- expand scope because an adjacent refactor looks attractive;
- mark tasks complete without corresponding code and validation;
- rewrite established decisions without proposing a new ADR;
- continue when the repository and task packet materially contradict each other.

When ambiguity blocks safe work, record it under `Open questions` in the change and stop that affected task. Continue unrelated tasks only when doing so cannot prejudice the unresolved decision.

## Task bookkeeping

`tasks.md` is an auditable execution record.

- Mark a task complete only after its acceptance condition is met.
- Add concise evidence beneath completed tasks: files, tests or commands.
- Record deviations explicitly.
- Never delete incomplete tasks to make the checklist appear complete.

## Pull requests

Every implementation PR must:

- target `develop` unless explicitly authorized otherwise;
- reference the active change path;
- summarize requirements implemented;
- list tests and real-replay validation performed;
- disclose deviations and known limitations;
- include specification/task updates in the same PR when applicable.
