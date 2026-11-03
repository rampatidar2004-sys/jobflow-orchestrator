#!/usr/bin/env bash
# Run everything CI runs, locally.
set -euo pipefail
cd "$(dirname "$0")/.."

echo "==> backend: mvn verify"
(cd backend && mvn -B verify)

echo "==> frontend: npm build"
(cd frontend && npm ci && npm run build)

echo "==> docker compose config"
docker compose config -q

echo "==> scanning for TODO/FIXME"
if grep -rn "TODO\|FIXME" backend/src frontend/src 2>/dev/null; then
  echo "Found TODO/FIXME markers above — resolve before merging."
  exit 1
fi

echo "All checks passed."
