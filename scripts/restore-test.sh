#!/usr/bin/env bash
# "Backup chua tung restore thu = chua co backup" (Prompt #9, P2 CI/CD) - script nay KHONG dung
# den database that dang chay: khoi dong 1 container Postgres RIENG, TAM THOI (ten ngau nhien,
# tu xoa khi xong qua --rm), restore ban sao luu MOI NHAT vao do, chay 3 cau SQL "smoke test" don
# gian, roi in ket qua PASS/FAIL. An toan chay dinh ky (cron hang tuan) ma khong anh huong gi den
# he thong dang phuc vu nguoi dung that.
#
# Chay tu thu muc goc repo: ./scripts/restore-test.sh [duong-dan-file.dump]
# Khong truyen tham so = tu dong lay ban "-daily-" moi nhat trong backups/.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$REPO_ROOT/.env"
BACKUP_DIR="$REPO_ROOT/backups"
LOG_FILE="$BACKUP_DIR/restore-test.log"
BACKUP_FILE="${1:-}"
CONTAINER_NAME="restore-test-pg-$$"

if [ ! -f "$ENV_FILE" ]; then
  echo "Khong tim thay $ENV_FILE — hay tao tu .env.example roi dien gia tri that." >&2
  exit 1
fi
# shellcheck disable=SC1090
set -a && source "$ENV_FILE" && set +a

if [ -z "$BACKUP_FILE" ]; then
  # shellcheck disable=SC2012
  BACKUP_FILE="$(ls -1t "$BACKUP_DIR"/"${DB_NAME}"-daily-*.dump 2>/dev/null | head -n1 || true)"
fi
if [ -z "$BACKUP_FILE" ] || [ ! -f "$BACKUP_FILE" ]; then
  echo "Khong tim thay ban sao luu nao de kiem tra (chay scripts/backup.sh truoc)." >&2
  exit 1
fi

log() {
  echo "$1"
  mkdir -p "$BACKUP_DIR"
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOG_FILE"
}

cleanup() {
  docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
}
trap cleanup EXIT

log "=== restore-test bat dau: $BACKUP_FILE ==="

docker run -d --name "$CONTAINER_NAME" \
  -e POSTGRES_PASSWORD=restore-test-throwaway \
  -e POSTGRES_DB="$DB_NAME" \
  postgres:16-alpine >/dev/null

echo "Cho Postgres tam khoi dong..."
for i in $(seq 1 30); do
  if docker exec "$CONTAINER_NAME" pg_isready -U postgres >/dev/null 2>&1; then
    break
  fi
  sleep 1
  if [ "$i" -eq 30 ]; then
    log "THAT BAI: container Postgres tam khong san sang sau 30s"
    exit 1
  fi
done

# Git Bash/Windows: MSYS tu dong "dich" duong dan kieu /tmp/xxx (phia CONTAINER, phai giu nguyen)
# thanh duong dan Windows truoc khi truyen cho docker.exe - MSYS_NO_PATHCONV=1 tat dich nay, nhung
# khi do duong dan host ($BACKUP_FILE, dang /c/Users/...) cung KHONG duoc dich nua nen phai tu dich
# tay bang cygpath truoc. Tren Linux/macOS that, "command -v cygpath" khong co nen giu nguyen
# BACKUP_FILE, khong anh huong gi.
HOST_BACKUP_FILE="$BACKUP_FILE"
if command -v cygpath >/dev/null 2>&1; then
  HOST_BACKUP_FILE="$(cygpath -w "$BACKUP_FILE")"
fi
MSYS_NO_PATHCONV=1 docker cp "$HOST_BACKUP_FILE" "$CONTAINER_NAME:/tmp/restore.dump"

# unaccent/pg_trgm duoc V1__init_schema.sql tao qua Flyway tren database THAT - container tam nay
# la 1 database TRANG hoan toan (khong qua Flyway), phai tu tao truoc 2 extension nay (co san trong
# postgres:16-alpine, chi chua duoc BAT) - neu khong pg_restore se loi luc tao lai 2 index dung
# unaccent() (idx_customers_name_trgm/idx_products_name_trgm), phat hien khi chay thu that lan dau.
docker exec "$CONTAINER_NAME" psql -U postgres -d "$DB_NAME" \
  -c "CREATE EXTENSION IF NOT EXISTS unaccent;" -c "CREATE EXTENSION IF NOT EXISTS pg_trgm;" >/dev/null

if ! MSYS_NO_PATHCONV=1 docker exec "$CONTAINER_NAME" pg_restore -U postgres -d "$DB_NAME" --no-owner --no-privileges /tmp/restore.dump; then
  log "THAT BAI: pg_restore bao loi (co the chi la warning object phu - xem chi tiet o tren)"
fi

run_sql() {
  docker exec "$CONTAINER_NAME" psql -U postgres -d "$DB_NAME" -t -A -c "$1"
}

TENANT_COUNT="$(run_sql "SELECT count(*) FROM tenants;" || echo "LOI")"
ORDER_COUNT="$(run_sql "SELECT count(*) FROM orders;" || echo "LOI")"
INVOICE_CHECK="$(run_sql "SELECT count(*) FROM invoices LIMIT 1;" || echo "LOI")"

log "So tenants: $TENANT_COUNT"
log "So don hang: $ORDER_COUNT"
log "So hoa don (kiem tra bang invoices doc duoc): $INVOICE_CHECK"

if [ "$TENANT_COUNT" = "LOI" ] || [ "$ORDER_COUNT" = "LOI" ] || [ "$INVOICE_CHECK" = "LOI" ]; then
  log "=== KET QUA: THAT BAI - restore duoc nhung khong doc duoc du lieu mong doi ==="
  exit 1
fi
if [ "$TENANT_COUNT" -lt 1 ]; then
  log "=== KET QUA: THAT BAI - ban sao luu khong co tenant nao (co the la ban rong/hong) ==="
  exit 1
fi

log "=== KET QUA: PASS - restore thanh cong, du lieu doc duoc dung nhu mong doi ==="
