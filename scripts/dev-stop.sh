#!/usr/bin/env bash
# Stop DSG backend on port 8080 (from spring-boot:run).
set -euo pipefail
PIDS=$(lsof -i :8080 -sTCP:LISTEN -t 2>/dev/null || true)
if [ -z "$PIDS" ]; then
  echo "Nothing listening on port 8080."
  exit 0
fi
echo "Stopping process(es) on port 8080: $PIDS"
kill $PIDS
echo "Done."
