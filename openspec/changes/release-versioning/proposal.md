# Release versioning

## Problem

Application version references are maintained manually and a release can reach `main` without proving that its semantic version increased.

## Outcome

`VERSION` becomes the release-version source of truth. Every release PR from `develop` to `main` must increase it by at least PATCH, all committed version references must agree, and a successful `main` build creates immutable tag `v<version>` before deployment.

## Scope

- strict `MAJOR.MINOR.PATCH` versions;
- version change only at the `develop` to `main` release boundary;
- CI validation of increase, synchronization and tag uniqueness;
- deployment reads `VERSION` and rejects overrides that disagree;
- repeated deployment of the same commit preserves semantic version while build number may change.

## Non-goals

- automatic choice of PATCH, MINOR or MAJOR;
- version bumps for ordinary merges into `develop`;
- prerelease/build metadata in `VERSION`;
- a separate browser or tester-agent gate.
