#!/usr/bin/env bash
# Build and run DSG backend, stopping any prior instance on port 8080 first.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

"$ROOT/scripts/dev-stop.sh"

echo "Building modules (skip tests)..."
mvn install -pl dsg-api -am -DskipTests -q

echo ""
echo "Starting DSG backend on http://localhost:8080"
echo "Press Ctrl+C to stop."
mvn -pl dsg-api spring-boot:run
