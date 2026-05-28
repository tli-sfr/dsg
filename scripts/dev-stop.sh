#!/usr/bin/env bash
# Stop DSG backend on port 8080 (from spring-boot:run).
set -euo pipefail
PIDS=$(lsof -i :8080 -sTCP:LISTEN -t 2>/dev/null || true)
if [ -z "$PIDS" ]; then
  echo "Nothing listening on port 8080."
  exit 0
fi
echo "Stopping process(es) on port 8080: $PIDS"
kill $PIDS 2>/dev/null || true
for _ in 1 2 3 4 5; do
  if ! lsof -i :8080 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo "Done."
    exit 0
  fi
  sleep 1
done
echo "Port 8080 still in use; sending SIGKILL..."
lsof -i :8080 -sTCP:LISTEN -t | xargs kill -9 2>/dev/null || true
echo "Done."
