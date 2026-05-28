#!/usr/bin/env bash
# Start local DSG dependencies and the backend (single Spring Boot process).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "Starting MySQL (host port 3307) and ElasticMQ..."
docker compose up -d

echo "Waiting for MySQL..."
for i in {1..30}; do
  if docker compose exec -T mysql mysqladmin ping -h localhost -u root -pdsg_root --silent 2>/dev/null; then
    break
  fi
  sleep 2
done

if lsof -i :8080 -sTCP:LISTEN -t >/dev/null 2>&1; then
  echo "Port 8080 is already in use (DSG may already be running)."
  echo "  Use it:  curl http://localhost:8080/dsg/v1/{accountId}/directory"
  echo "  Stop it: ./scripts/dev-stop.sh"
  exit 0
fi

echo "Building modules (skip tests)..."
mvn install -pl dsg-api -am -DskipTests -q

echo ""
echo "Starting DSG backend on http://localhost:8080"
echo "Press Ctrl+C to stop."
mvn -pl dsg-api spring-boot:run
