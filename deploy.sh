#!/usr/bin/env bash
set -Eeuo pipefail

BRANCH="${SC2_COACH_BRANCH:-main}"
PORT="${SC2_COACH_PORT:-18080}"

printf 'Updating SC2 Coach from %s...\n' "$BRANCH"
git fetch --prune origin
git checkout "$BRANCH"
git pull --ff-only origin "$BRANCH"

export APP_VERSION="${APP_VERSION:-0.8.0}"
export BUILD_NUMBER="${BUILD_NUMBER:-$(date -u +%Y%m%d%H%M%S)}"
export BUILD_TIME="${BUILD_TIME:-$(date -u +%Y-%m-%dT%H:%M:%SZ)}"
export GIT_COMMIT="${GIT_COMMIT:-$(git rev-parse --short=12 HEAD)}"

printf 'Building version %s, build %s, commit %s...\n' "$APP_VERSION" "$BUILD_NUMBER" "$GIT_COMMIT"
docker compose build --pull
docker compose up -d --remove-orphans

printf 'Waiting for http://127.0.0.1:%s ...\n' "$PORT"
for attempt in $(seq 1 30); do
  if curl --fail --silent --show-error "http://127.0.0.1:${PORT}/" >/dev/null; then
    docker compose ps
    printf 'SC2 Coach %s build %s (%s) is ready.\n' "$APP_VERSION" "$BUILD_NUMBER" "$GIT_COMMIT"
    exit 0
  fi
  sleep 2
done

printf 'Deployment health check failed. Recent logs:\n' >&2
docker compose logs --tail=100 >&2
exit 1
