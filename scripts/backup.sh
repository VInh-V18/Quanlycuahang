#!/usr/bin/env bash
# Sao luu database Postgres cua container "postgres" (docker/docker-compose.yml) ra file .sql.gz.
# Chay tu thu muc goc repo: ./scripts/backup.sh [thu-muc-dich]
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$REPO_ROOT/.env"
OUT_DIR="${1:-$REPO_ROOT/backups}"

if [ ! -f "$ENV_FILE" ]; then
  echo "Khong tim thay $ENV_FILE — hay tao tu .env.example roi dien gia tri that." >&2
  exit 1
fi

# shellcheck disable=SC1090
set -a && source "$ENV_FILE" && set +a

mkdir -p "$OUT_DIR"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
OUT_FILE="$OUT_DIR/${DB_NAME}-${TIMESTAMP}.sql.gz"

docker compose -f "$REPO_ROOT/docker/docker-compose.yml" exec -T postgres \
  pg_dump -U "$DB_USERNAME" "$DB_NAME" | gzip > "$OUT_FILE"

echo "Da sao luu: $OUT_FILE"
