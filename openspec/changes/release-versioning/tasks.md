# Tasks: Release versioning

Lifecycle gate: `APPLY`

- [x] Add root `VERSION` with strict `MAJOR.MINOR.PATCH` syntax.
- [x] Add deterministic validation for syntax, monotonic increase and synchronized references.
- [x] Guard release PRs into `main` and reject reused tags.
- [x] Read the semantic version from `VERSION` during deployment.
- [x] Create immutable `v<version>` tags after successful `main` validation.
- [x] Preserve build number, Git commit and build time as separate identity fields.
- [x] Record the release-versioning ADR.

## Expected result on the website

After a release PR from `develop` is merged into `main`, the website build badge and `GET /api/v1/build` expose the new semantic version from `VERSION`.

Example:

```text
before release: v0.8.0 · build <old build> · <old commit>
after release:  v0.8.1 · build <new build> · <new commit>
```

A repeated deployment of the same `main` commit keeps `v0.8.1` and the same Git commit, while the build number may increase.

A release PR fails before merge when the version did not increase, is not strict SemVer, is inconsistent with committed component references, or already has a Git tag.
