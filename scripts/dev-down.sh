#!/usr/bin/env bash
# Tear down the local stack, including volumes (fresh Postgres/Grafana data next run).
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose down -v
