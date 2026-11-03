#!/usr/bin/env bash
# Bring up the full local stack and follow backend logs.
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose up --build -d
echo "Stack starting. Console: http://localhost:5173  API: http://localhost:8080  Grafana: http://localhost:3000"
docker compose logs -f backend
