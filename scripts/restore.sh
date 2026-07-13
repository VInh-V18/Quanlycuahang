#!/usr/bin/env bash
# Phuc hoi database Postgres cua container "postgres" (docker/docker-compose.yml) tu file .dump
# (dinh dang custom -Fc) do scripts/backup.sh tao ra. CANH BAO: ghi de toan bo du lieu hien co
# trong database dich - CHI dung cho database THAT khi ban CHAC CHAN muon ghi de (vd sau su co).
# Muon THU restore ma khong dung cham gi den database that, dung scripts/restore-test.sh thay the
# (restore vao 1 container Postgres RIENG, khong lien quan gi database dang chay).
#
# Chay tu thu muc goc repo: ./scripts/restore.sh <duong-dan-file.dump>
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$REPO_ROOT/.env"
BACKUP_FILE="${1:-}"

if [ -z "$BACKUP_FILE" ] || [ ! -f "$BACKUP_FILE" ]; then
  echo "Cach dung: ./scripts/restore.sh <duong-dan-file.dump>" >&2
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

# --clean --if-exists: xoa object cu truoc khi tao lai (tranh loi "already exists" khi restore de
# len 1 database da co schema) - can ket noi lai thanh 1 database khac ("postgres") de DROP/CREATE
# lai chinh database dich, khong the DROP database dang duoc ket noi vao.
docker compose -f "$REPO_ROOT/docker/docker-compose.yml" exec -T postgres \
  psql -U "$DB_USERNAME" -d postgres -c "DROP DATABASE IF EXISTS \"$DB_NAME\";" \
  -c "CREATE DATABASE \"$DB_NAME\";"

# unaccent/pg_trgm - CREATE DATABASE o tren tao database TRANG, phai tu bat lai 2 extension nay
# (V1__init_schema.sql lam luc Flyway migrate lan dau, khong chay lai o day) truoc khi restore, neu
# khong pg_restore se loi luc tao lai idx_customers_name_trgm/idx_products_name_trgm.
docker compose -f "$REPO_ROOT/docker/docker-compose.yml" exec -T postgres \
  psql -U "$DB_USERNAME" -d "$DB_NAME" -c "CREATE EXTENSION IF NOT EXISTS unaccent;" \
  -c "CREATE EXTENSION IF NOT EXISTS pg_trgm;"

cat "$BACKUP_FILE" | docker compose -f "$REPO_ROOT/docker/docker-compose.yml" exec -T postgres \
  pg_restore -U "$DB_USERNAME" -d "$DB_NAME" --no-owner --no-privileges

echo "Da phuc hoi tu: $BACKUP_FILE"
