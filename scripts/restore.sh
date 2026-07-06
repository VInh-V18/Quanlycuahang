#!/usr/bin/env bash
# Phuc hoi database Postgres cua container "postgres" (docker/docker-compose.yml) tu file .sql.gz
# do scripts/backup.sh tao ra. CANH BAO: ghi de toan bo du lieu hien co trong database dich.
# Chay tu thu muc goc repo: ./scripts/restore.sh <duong-dan-file.sql.gz>
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$REPO_ROOT/.env"
BACKUP_FILE="${1:-}"

if [ -z "$BACKUP_FILE" ] || [ ! -f "$BACKUP_FILE" ]; then
  echo "Cach dung: ./scripts/restore.sh <duong-dan-file.sql.gz>" >&2
  exit 1
fi

if [ ! -f "$ENV_FILE" ]; then
  echo "Khong tim thay $ENV_FILE — hay tao tu .env.example roi dien gia tri that." >&2
  exit 1
fi

# shellcheck disable=SC1090
set -a && source "$ENV_FILE" && set +a

read -r -p "CANH BAO: se ghi de toan bo du lieu trong database '$DB_NAME'. Tiep tuc? [y/N] " confirm
if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
  echo "Da huy."
  exit 0
fi

gunzip -c "$BACKUP_FILE" | docker compose -f "$REPO_ROOT/docker/docker-compose.yml" exec -T postgres \
  psql -U "$DB_USERNAME" "$DB_NAME"

echo "Da phuc hoi tu: $BACKUP_FILE"
