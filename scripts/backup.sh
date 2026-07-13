#!/usr/bin/env bash
# Sao luu database Postgres cua container "postgres" (docker/docker-compose.yml) ra dinh dang
# custom (-Fc, giong cach cac ban sao luu tay truoc day trong backups/*.dump da dung) - khac plain
# SQL text: nen san, cho phep restore chon loc tung bang/schema (pg_restore -t), va la dinh dang
# BAT BUOC neu sau nay muon restore song song nhieu luong (pg_restore -j).
#
# Chinh sach giu lai (Prompt #9, P2 CI/CD): 7 ban HANG NGAY + 4 ban HANG TUAN (ban dau tien cua moi
# tuan ISO duoc giu rieng, khong bi don dep theo chu ky 7 ngay) - du de khoi phuc nham 1 ngay gan
# day, VA con co "luoi an toan" xa hon (toi 4 tuan truoc) neu loi du lieu am tham khong phat hien
# ngay (vd 1 bug tinh sai cong no ma vai ngay sau moi lo ra).
#
# Chay tu thu muc goc repo: ./scripts/backup.sh [thu-muc-dich]
# Day ra ngoai may chu (tuy chon): dat RCLONE_REMOTE (vd "b2:fruithouse-backups") - can rclone da
# cau hinh remote tu truoc (`rclone config`). Khong dat gi = chi luu tai cho, script se in huong
# dan copy thu cong sang may khac (scp/rsync) o cuoi.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$REPO_ROOT/.env"
OUT_DIR="${1:-$REPO_ROOT/backups}"
DAILY_KEEP=7
WEEKLY_KEEP=4

if [ ! -f "$ENV_FILE" ]; then
  echo "Khong tim thay $ENV_FILE — hay tao tu .env.example roi dien gia tri that." >&2
  exit 1
fi

# shellcheck disable=SC1090
set -a && source "$ENV_FILE" && set +a

mkdir -p "$OUT_DIR"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
ISO_WEEK="$(date +%G-W%V)"
DAILY_FILE="$OUT_DIR/${DB_NAME}-daily-${TIMESTAMP}.dump"

echo "Dang sao luu (custom format -Fc)..."
docker compose -f "$REPO_ROOT/docker/docker-compose.yml" exec -T postgres \
  pg_dump -U "$DB_USERNAME" -Fc "$DB_NAME" > "$DAILY_FILE"
echo "Da sao luu: $DAILY_FILE ($(du -h "$DAILY_FILE" | cut -f1))"

# Ban dau tien cua tuan ISO nay chua co ban "weekly" -> giu rieng 1 ban lam moc hang tuan (khong
# bi xoa theo chu ky 7 ngay cua ban "daily" o duoi).
WEEKLY_FILE="$OUT_DIR/${DB_NAME}-weekly-${ISO_WEEK}.dump"
if [ ! -f "$WEEKLY_FILE" ]; then
  cp "$DAILY_FILE" "$WEEKLY_FILE"
  echo "Da tao moc hang tuan: $WEEKLY_FILE"
fi

echo "Don ban cu (giu $DAILY_KEEP ban daily + $WEEKLY_KEEP ban weekly gan nhat)..."
# shellcheck disable=SC2012
ls -1t "$OUT_DIR"/"${DB_NAME}"-daily-*.dump 2>/dev/null | tail -n +$((DAILY_KEEP + 1)) | while read -r old; do
  rm -f "$old"
  echo "  Da xoa (qua han daily): $old"
done
# shellcheck disable=SC2012
ls -1t "$OUT_DIR"/"${DB_NAME}"-weekly-*.dump 2>/dev/null | tail -n +$((WEEKLY_KEEP + 1)) | while read -r old; do
  rm -f "$old"
  echo "  Da xoa (qua han weekly): $old"
done

if [ -n "${RCLONE_REMOTE:-}" ]; then
  if command -v rclone >/dev/null 2>&1; then
    echo "Day ban sao ra ngoai may chu qua rclone (remote: $RCLONE_REMOTE)..."
    rclone copy "$DAILY_FILE" "$RCLONE_REMOTE" && echo "Da day: $DAILY_FILE -> $RCLONE_REMOTE"
  else
    echo "CANH BAO: da dat RCLONE_REMOTE nhung chua cai rclone (apt/brew install rclone) - BAN SAO CHUA DUOC DAY RA NGOAI MAY CHU." >&2
  fi
else
  echo
  echo "Chua cau hinh day ban sao ra NGOAI may chu (RCLONE_REMOTE trong .env)."
  echo "Backup chi nam TREN CUNG 1 O DIA voi du lieu goc - neu may chu hong/mat o dia, mat luon ca 2."
  echo "Phuong an don gian nhat neu chua co cloud storage: dinh ky copy thu muc backups/ sang 1 may KHAC qua rsync/scp, vi du:"
  echo "  rsync -avz $OUT_DIR/ user@may-khac:/duong/dan/luu/backup/"
fi
