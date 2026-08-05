# Design: Release versioning

## Decision

A release is the merge of `develop` into `main`. Feature development does not change semantic version. The release author explicitly chooses PATCH, MINOR or MAJOR by changing `VERSION`; automation validates but never chooses the increment.

## Version flow

```text
VERSION
  -> synchronization check for Maven, frontend and container defaults
  -> release PR guard
  -> merge to main
  -> immutable v<version> tag
  -> production deployment
  -> /api/v1/build and website build badge
```

`VERSION` contains strict `MAJOR.MINOR.PATCH`. Existing build number, Git commit and build time remain independent build-identity fields.

## Release PR validation

For pull requests into `main`, CI:

1. relies on the existing source guard to require head branch `develop`;
2. validates current and previous versions;
3. requires current version to be numerically greater than the version on the base `main` commit;
4. verifies committed Maven, frontend, Docker and Compose version references are synchronized;
5. rejects a version whose `v<version>` tag already exists.

## Deployment

After tests pass on `main`, the workflow creates an annotated immutable tag. A rerun is idempotent only when the existing tag points to the same commit. Deployment reads `VERSION`; an external `APP_VERSION` override is accepted only when equal.

## Trade-off

Build-tool files retain explicit synchronized values because Maven parent references and package metadata require committed versions. `VERSION` is authoritative, while `scripts/release_version.py --check-sync` prevents drift. A release author changes `VERSION`, synchronizes the explicit references, and CI verifies the result.
