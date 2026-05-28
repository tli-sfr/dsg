#!/usr/bin/env bash
# Build and run DSG backend, stopping any prior instance on port 8080 first.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

"$ROOT/scripts/dev-stop.sh"

echo "Building modules (skip tests)..."
mvn install -pl dsg-api -am -DskipTests -q

echo ""
echo "Starting DSG backend on http://localhost:8080 (profile: local if application-local.yml exists)"
echo "Press Ctrl+C to stop."
if [[ -f "$ROOT/dsg-api/src/main/resources/application-local.yml" ]]; then
  mvn -pl dsg-api spring-boot:run -Dspring-boot.run.profiles=local
else
  echo "Tip: copy dsg-api/src/main/resources/application-local.yml.example to application-local.yml for RC OAuth."
  mvn -pl dsg-api spring-boot:run
fi
