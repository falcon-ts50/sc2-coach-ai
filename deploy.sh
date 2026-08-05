#!/usr/bin/env bash
set -Eeuo pipefail

BRANCH="${SC2_COACH_BRANCH:-main}"
PORT="${SC2_COACH_PORT:?SC2_COACH_PORT must be configured on the deployment host}"

printf 'Updating SC2 Match Review from %s...\n' "$BRANCH"
git fetch --prune origin
git checkout "$BRANCH"
git pull --ff-only origin "$BRANCH"

RELEASE_VERSION="$(tr -d '[:space:]' < VERSION)"
python3 scripts/release_version.py --check-sync >/dev/null

export APP_VERSION="${APP_VERSION:-$RELEASE_VERSION}"
export BUILD_NUMBER="${BUILD_NUMBER:-$(date -u +%Y%m%d%H%M%S)}"
export BUILD_TIME="${BUILD_TIME:-$(date -u +%Y-%m-%dT%H:%M:%SZ)}"
export GIT_COMMIT="${GIT_COMMIT:-$(git rev-parse --short=12 HEAD)}"

if [[ "$APP_VERSION" != "$RELEASE_VERSION" ]]; then
  printf 'APP_VERSION %s differs from VERSION %s.\n' "$APP_VERSION" "$RELEASE_VERSION" >&2
  exit 1
fi

printf 'Building version %s, build %s, commit %s...\n' "$APP_VERSION" "$BUILD_NUMBER" "$GIT_COMMIT"
docker compose build --pull
docker compose up -d --remove-orphans

printf 'Waiting for the local application endpoint...\n'
for attempt in $(seq 1 30); do
  if curl --fail --silent --show-error "http://127.0.0.1:${PORT}/" >/dev/null; then
    docker compose ps
    printf 'SC2 Match Review %s build %s (%s) is ready.\n' "$APP_VERSION" "$BUILD_NUMBER" "$GIT_COMMIT"
    exit 0
  fi
  sleep 2
done

printf 'Deployment health check failed. Recent logs:\n' >&2
docker compose logs --tail=100 >&2
exit 1
