#!/usr/bin/env bash
set -Eeuo pipefail

BRANCH="${SC2_COACH_BRANCH:-main}"
PORT="${SC2_COACH_PORT:-18080}"

printf 'Updating SC2 Coach from %s...\n' "$BRANCH"
git fetch --prune origin
git checkout "$BRANCH"
git pull --ff-only origin "$BRANCH"

docker compose build --pull
docker compose up -d --remove-orphans

printf 'Waiting for http://127.0.0.1:%s ...\n' "$PORT"
for attempt in $(seq 1 30); do
  if curl --fail --silent --show-error "http://127.0.0.1:${PORT}/" >/dev/null; then
    docker compose ps
    printf 'SC2 Coach is ready.\n'
    exit 0
  fi
  sleep 2
done

printf 'Deployment health check failed. Recent logs:\n' >&2
docker compose logs --tail=100 >&2
exit 1
