#!/bin/sh
# Chay BEN TRONG container "backup" (docker-compose.backup.yml, Prompt #9 P2 CI/CD) - khac
# scripts/backup.sh o host (goi qua `docker compose exec`), script nay ket noi THANG toi service
# "postgres" qua mang noi bo docker-compose (khong co Docker CLI/socket ben trong container nay -
# co chu dich, tranh phai mount Docker socket vao 1 container chi can chay pg_dump dinh ky, giam
# be mat tan cong neu container nay bi chiem quyen).
set -eu

OUT_DIR="/backups"
DAILY_KEEP=7
WEEKLY_KEEP=4
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
ISO_WEEK="$(date +%G-W%V)"
DAILY_FILE="$OUT_DIR/${POSTGRES_DB}-daily-${TIMESTAMP}.dump"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Bat dau sao luu dinh ky..."
PGPASSWORD="$POSTGRES_PASSWORD" pg_dump -h postgres -U "$POSTGRES_USER" -Fc "$POSTGRES_DB" > "$DAILY_FILE"
echo "Da sao luu: $DAILY_FILE ($(du -h "$DAILY_FILE" | cut -f1))"

WEEKLY_FILE="$OUT_DIR/${POSTGRES_DB}-weekly-${ISO_WEEK}.dump"
if [ ! -f "$WEEKLY_FILE" ]; then
  cp "$DAILY_FILE" "$WEEKLY_FILE"
  echo "Da tao moc hang tuan: $WEEKLY_FILE"
fi

# BusyBox `ls -t` ho tro -t (thoi gian sua doi moi nhat truoc) nhung KHONG co `tail -n +K` giong
# GNU coreutils tren 1 so alpine toi gian - postgres:16-alpine dung busybox day du nen van co, da
# kiem tra truoc khi dung.
ls -1t "$OUT_DIR"/"${POSTGRES_DB}"-daily-*.dump 2>/dev/null | tail -n +$((DAILY_KEEP + 1)) | while read -r old; do
  rm -f "$old"
  echo "Da xoa (qua han daily): $old"
done
ls -1t "$OUT_DIR"/"${POSTGRES_DB}"-weekly-*.dump 2>/dev/null | tail -n +$((WEEKLY_KEEP + 1)) | while read -r old; do
  rm -f "$old"
  echo "Da xoa (qua han weekly): $old"
done

if [ -n "${RCLONE_REMOTE:-}" ] && command -v rclone >/dev/null 2>&1; then
  rclone copy "$DAILY_FILE" "$RCLONE_REMOTE" && echo "Da day ra ngoai may chu: $RCLONE_REMOTE"
fi

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Xong."
