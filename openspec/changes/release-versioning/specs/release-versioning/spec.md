# Release versioning capability

## Requirements

### Release boundary

The application semantic version SHALL change only as part of a release from `develop` to `main`. Ordinary merges into `develop` SHALL NOT require a version change.

### Monotonic SemVer

Every release PR into `main` SHALL contain a strict `MAJOR.MINOR.PATCH` version numerically greater than the version on its base `main` commit. PATCH is the minimum valid increase; the release author SHALL choose whether PATCH, MINOR or MAJOR is appropriate.

### Source of truth and synchronization

The root `VERSION` file SHALL be authoritative. CI SHALL reject a release when Maven, frontend, Docker or Compose version references disagree with it.

### Build identity

Semantic version, build number, Git commit and build time SHALL remain separate. Rebuilding the same commit SHALL preserve semantic version and MAY produce a new build number.

### Immutable release tag

A successful build of a newly merged `main` release SHALL create annotated tag `v<version>` before deployment. Existing tags SHALL NOT be moved. A rerun MAY reuse the tag only when it already points to the same commit.

### Runtime consistency

Production deployment SHALL read semantic version from `VERSION` and SHALL fail when an explicit version override disagrees. The build API and website badge SHALL expose that same version.

## Scenarios

### Patch release

Given `main` is version `0.8.0`, when a release PR declares `0.8.1` with synchronized references and no existing tag, then the guard passes and the merged release is tagged `v0.8.1`.

### Missing increase

Given `main` is version `0.8.0`, when a release PR still declares `0.8.0`, then the guard fails.

### Repeated deployment

Given tag `v0.8.1` points to the current `main` commit, when the deployment workflow is rerun, then no new semantic version is created and deployment may proceed with a new build number.
