# Hướng dẫn deploy production — FruitHouse ERP

Hệ thống: Spring Boot (Java 21) + React/TypeScript (Vite) + PostgreSQL + Redis, đóng gói bằng
Docker Compose. Kiến trúc **multi-tenant**: 1 lần triển khai phục vụ nhiều cửa hàng, quản lý tập
trung qua trang Super Admin (`/platform-admin`).

## 1. Yêu cầu server

- Docker Engine ≥ 24 + Docker Compose plugin v2 (`docker compose version`)
- Tối thiểu 2 vCPU / 4GB RAM cho quy mô vài chục cửa hàng đồng thời
- Ổ đĩa còn trống cho volume Postgres (`docker/postgres-data/`) — tăng dần theo dữ liệu
- 2 kịch bản triển khai được hỗ trợ sẵn (không cần sửa code, chỉ khác `.env`):
  - **VPS có domain + HTTPS**: đặt Nginx/Caddy + Let's Encrypt phía trước container `web`
  - **LAN nội bộ, chỉ HTTP**: không cần domain/TLS, đặt `AUTH_REFRESH_COOKIE_SECURE=false`

## 2. Cài Docker

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER   # rồi đăng nhập lại
```

## 3. Clone project

```bash
git clone <repo-url> quanlycuahang && cd quanlycuahang
```

## 4. Cấu hình `.env`

```bash
cp .env.example .env
```

Điền các giá trị thật vào `.env` (file này đã nằm trong `.gitignore`, không commit):

| Biến | Mô tả |
|---|---|
| `DB_PASSWORD` | Mật khẩu Postgres — sinh ngẫu nhiên ≥ 20 ký tự, **không** để trống hay giữ `change-me` |
| `JWT_SECRET` | Khoá ký JWT, tối thiểu 32 ký tự ngẫu nhiên. **Không dùng giá trị mẫu trong `.env.example`** — bất kỳ ai đọc được repo đều giả mạo được token nếu giữ nguyên giá trị đó |
| `REDIS_PASSWORD` | Để trống chỉ chấp nhận được khi Redis không lộ ra ngoài container network (mặc định đúng); nên đặt mật khẩu cho production thật |
| `CORS_ALLOWED_ORIGINS` | Domain thật của frontend (vd `https://banhang.example.com`) |
| `AUTH_REFRESH_COOKIE_SECURE` | `true` nếu có HTTPS trước container `web`; `false` nếu LAN thuần HTTP |
| `SPRING_PROFILES_ACTIVE` | Giữ `docker` (mặc định) — đã tắt Swagger/chi tiết health ở profile này |
| `AI_DB_READONLY_PASSWORD` | *(Tuỳ chọn — chỉ cần nếu bật tính năng Trợ lý AI)* Mật khẩu role Postgres chỉ-đọc riêng cho AI (`fruithouse_ai_readonly`, V30) — sinh ngẫu nhiên, khác `DB_PASSWORD` |
| `AI_SETTINGS_ENCRYPTION_KEY` | *(Tuỳ chọn)* Khoá gốc AES-256 (base64, 32 byte) mã hoá khoá API AI của từng tenant lúc lưu — sinh bằng `openssl rand -base64 32`. Để trống = tính năng AI bị khoá hoàn toàn (không ảnh hưởng phần còn lại của hệ thống) |

Sinh giá trị ngẫu nhiên an toàn:

```bash
openssl rand -base64 32   # dùng cho DB_PASSWORD hoặc JWT_SECRET
```

## 5. Build image

```bash
docker compose --env-file .env -f docker/docker-compose.yml build
```

Dùng thêm override production (giới hạn tài nguyên, `restart: always`):

```bash
docker compose --env-file .env -f docker/docker-compose.yml -f docker/docker-compose.prod.yml build
```

## 6. Run container

```bash
docker compose --env-file .env -f docker/docker-compose.yml -f docker/docker-compose.prod.yml up -d
```

Kiểm tra cả 4 container `healthy`:

```bash
docker ps --format "table {{.Names}}\t{{.Status}}"
```

## 7. Migration database

**Không cần chạy tay** — Flyway tự động áp dụng toàn bộ migration (`server/src/main/resources/db/migration/`)
ngay khi container `server` khởi động lần đầu, bao gồm cả seed dữ liệu bắt buộc (roles, permissions,
tài khoản Super Admin). Xem log để xác nhận:

```bash
docker logs docker-server-1 | grep -i flyway
```

Kiểm tra lịch sử migration đã áp dụng:

```bash
docker compose -f docker/docker-compose.yml --env-file .env exec postgres \
  psql -U "$DB_USERNAME" -d "$DB_NAME" -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

## 8. Seed database

- **Bắt buộc** (Super Admin, roles, permissions): đã tự chạy ở bước 7, không cần làm gì thêm.
- Đăng nhập Super Admin lần đầu tại `/login` bằng tài khoản `superadmin` với mật khẩu đã ghi trong
  `V15__seed_platform_admin.sql` — **đổi mật khẩu ngay** qua nút "Đổi mật khẩu" trên trang quản trị.
- **Tuỳ chọn** (dữ liệu demo/staging — 1 tenant mẫu với sản phẩm/danh mục mẫu, không đụng đến dữ
  liệu tenant thật): chỉ chạy nếu cần dữ liệu để QA/demo.

```bash
docker compose -f docker/docker-compose.yml --env-file .env exec -T postgres \
  psql -U "$DB_USERNAME" -d "$DB_NAME" < database/seed.sql
```

An toàn chạy lại nhiều lần (idempotent) — chạy lần 2 trở đi sẽ không tạo thêm dòng nào.

## 9. Backup database

```bash
./scripts/backup.sh                # xuất ra backups/<DB_NAME>-daily-<timestamp>.dump (định dạng
                                    # custom -Fc) + tự tạo mốc backups/<DB_NAME>-weekly-<năm-Wtuần>.dump
./scripts/restore.sh <file.dump>   # khôi phục từ 1 bản backup — GHI ĐÈ TOÀN BỘ database hiện có
./scripts/restore-test.sh          # kiểm tra bản mới nhất phục hồi ĐƯỢC + đọc đúng dữ liệu, vào
                                    # 1 container Postgres TẠM (không đụng gì đến database thật)
```

Chính sách giữ lại: **7 bản hàng ngày + 4 bản hàng tuần** (mốc tuần không bị dọn theo chu kỳ 7 ngày
của bản ngày) — script tự dọn bản cũ mỗi lần chạy, không cần thao tác thêm.

**"Backup chưa từng restore thử = chưa có backup"** — chạy `./scripts/restore-test.sh` định kỳ
(khuyến nghị hàng tuần), không chỉ tin vào việc `backup.sh` chạy xong không báo lỗi. Kết quả ghi
vào `backups/restore-test.log`.

**Tự động hoá**: đặt `scripts/backup.sh` vào cron của máy chủ (host), hoặc bật overlay container
phụ tự chạy cron bên trong Docker (không cần cron của máy chủ):

```bash
docker compose --env-file .env -f docker/docker-compose.yml -f docker/docker-compose.backup.yml up -d
```

**Sao lưu ra NGOÀI máy chủ** (bắt buộc cho production thật — backup nằm cùng ổ đĩa với dữ liệu gốc
không bảo vệ được trước sự cố hỏng ổ đĩa/máy chủ): đặt `RCLONE_REMOTE` trong `.env` (cần
`rclone config` trước) để `scripts/backup.sh` tự đẩy bản mới nhất lên cloud storage sau mỗi lần
chạy; nếu chưa có tài khoản cloud storage, phương án tối thiểu là `rsync`/`scp` định kỳ thư mục
`backups/` sang 1 máy khác (script đã in sẵn câu lệnh mẫu khi `RCLONE_REMOTE` để trống).

## 10. Update version

```bash
git pull
docker compose --env-file .env -f docker/docker-compose.yml -f docker/docker-compose.prod.yml build
docker compose --env-file .env -f docker/docker-compose.yml -f docker/docker-compose.prod.yml up -d
```

Flyway tự động áp dụng migration mới (nếu có) khi container `server` khởi động lại — không cần
thao tác thủ công. Compose đợi `server` báo `healthy` (đã gọi `/actuator/health` thành công, nghĩa
là Flyway migrate xong + kết nối được Postgres/Redis) rồi mới cho `web` nhận traffic — nếu `server`
không lên được `healthy` (vd migration lỗi), `web` vẫn chạy phiên bản cũ, tránh người dùng thấy lỗi
502 giữa chừng. **Luôn backup trước khi update** (bước 9).

## 11. Rollback (quay lại phiên bản trước)

Dùng khi bản mới vừa deploy có lỗi nghiêm trọng phát hiện ngay sau khi lên production.

```bash
git checkout <git-sha-hoặc-tag-bản-trước>
docker compose --env-file .env -f docker/docker-compose.yml -f docker/docker-compose.prod.yml build
docker compose --env-file .env -f docker/docker-compose.yml -f docker/docker-compose.prod.yml up -d
```

**Ràng buộc bắt buộc phải biết trước khi rollback — Flyway CHỈ ROLL-FORWARD**: Flyway không có khái
niệm "migrate xuống" (down-migration) trong bản Community đang dùng ở đây — mỗi migration mới
(`V<n>__*.sql`) chỉ được áp dụng theo chiều tiến. Quay lại image cũ **không** tự động hoàn tác
migration đã chạy trên schema. Vì vậy:

- **An toàn để rollback ngay**: nếu bản mới KHÔNG thêm migration schema (chỉ sửa code Java/FE), quay
  lại image cũ hoàn toàn an toàn — schema không đổi gì.
- **Nếu bản mới CÓ thêm migration**: chỉ rollback an toàn nếu migration đó tuân thủ nguyên tắc
  tương thích ngược 1 phiên bản (áp dụng từ đầu dự án — xem `docs/UPGRADE_ROADMAP.md`):
  - Thêm cột mới → luôn **nullable** hoặc có `DEFAULT`, để code phiên bản CŨ (không biết cột này)
    vẫn insert/update bình thường.
  - Đổi tên/xoá cột → **KHÔNG làm ngay trong 1 bước** — phải qua 2 phiên bản: (1) thêm cột mới +
    code đọc/ghi CẢ 2 cột song song, deploy, chạy ổn định; (2) phiên bản SAU mới xoá cột cũ.
  - Nếu migration mới vi phạm nguyên tắc trên (vd `ALTER COLUMN ... SET NOT NULL` cho cột code cũ
    không biết gán giá trị, hoặc `DROP COLUMN` code cũ vẫn còn đọc) — rollback code KHÔNG đủ, phải
    khôi phục lại từ bản backup gần nhất (bước 9) TRƯỚC migration đó, chấp nhận mất dữ liệu ghi
    thêm từ lúc backup đến lúc phát hiện lỗi. Đây là lý do backup phải chạy **trước mỗi lần update**
    (bước 9), không chỉ định kỳ.
- Sau rollback: kiểm tra `docker logs docker-server-1 | grep -i flyway` để xác nhận KHÔNG có
  migration mới nào cố áp dụng lại (Flyway sẽ báo lỗi nếu schema history không khớp version đang
  chạy — dấu hiệu rollback không an toàn, đã tới bước "phải khôi phục từ backup" ở trên).

## 12. Trợ lý AI (tuỳ chọn, Prompt #11)

Tính năng "Hỏi đáp báo cáo" + "Gợi ý nhập hàng" thêm ở Prompt #11 — **tắt mặc định** cho tới khi cấu
hình đủ 2 lớp bên dưới, không ảnh hưởng gì phần còn lại của hệ thống nếu bỏ qua mục này.

1. **Bắt buộc trước tiên** — đặt `AI_DB_READONLY_PASSWORD` và `AI_SETTINGS_ENCRYPTION_KEY` trong
   `.env` (xem bảng biến môi trường ở bước 4) **trước khi** container `server` khởi động lần đầu sau
   khi nâng cấp lên bản có Prompt #11 — migration `V30` tạo role Postgres `fruithouse_ai_readonly`
   với đúng mật khẩu này ngay lúc chạy, đổi `.env` SAU đó sẽ không tự cập nhật lại mật khẩu role đã
   tạo (phải tự `ALTER ROLE fruithouse_ai_readonly WITH PASSWORD '...'` tay nếu cần đổi).
2. Đăng nhập bằng tài khoản **chủ cửa hàng** (owner), vào **Cài đặt → Trợ lý AI**, nhập khoá API của
   nhà cung cấp AI (mặc định Claude, xem `AI_DEFAULT_MODEL`) — khoá được mã hoá (AES-256-GCM) bằng
   `AI_SETTINGS_ENCRYPTION_KEY` trước khi lưu vào database, không bao giờ lưu dạng thường.
3. Tính năng "Gợi ý nhập hàng" (nút **Gợi ý từ AI** ở trang Tạo phiếu nhập) hoạt động ngay cả khi
   CHƯA cấu hình khoá API ở bước 2 — đây là công thức tính toán xác định (tốc độ bán 30 ngày × định
   mức tồn tối thiểu), không gọi AI/LLM, xem Javadoc `AiPurchaseSuggestionService`. Chỉ tính năng
   "Hỏi đáp báo cáo" (widget trên Dashboard) mới cần khoá API thật.

**Cảnh báo chưa kiểm chứng**: `ClaudeAiProvider` (tích hợp Anthropic Messages API) được viết đúng
theo tài liệu chính thức tại thời điểm viết code nhưng **CHƯA được gọi thử với 1 khoá API thật**
(không có sẵn trong môi trường phát triển, giống tình trạng `EInvoiceProvider` trước đây) — lớp an
toàn kết nối DB (role chỉ-đọc, 4 view whitelist) đã được kiểm chứng đầy đủ qua
`AiReadOnlyPermissionIT`, nhưng bản thân lời gọi HTTP tới Anthropic thì chưa. Trước khi bật tính năng
"Hỏi đáp báo cáo" cho 1 tenant thật, nên tự thử 1 câu hỏi với khoá API thật và xác nhận câu trả lời
hợp lý trước khi thông báo tính năng này cho người dùng cuối.

## Khắc phục sự cố nhanh

| Triệu chứng | Kiểm tra |
|---|---|
| Container `postgres` không khởi động | `DB_PASSWORD` rỗng hoặc thiếu `--env-file .env` trong lệnh compose |
| 401 khi gọi API dù đăng nhập đúng | `JWT_SECRET` khác nhau giữa các lần restart (không cố định trong `.env`) |
| Trang trắng, `/api/*` trả 502 | Container `server` chưa `healthy` khi `web` đã start — đợi thêm hoặc `docker logs docker-server-1` |
| Không đăng nhập được qua LAN (HTTP) | `AUTH_REFRESH_COOKIE_SECURE` đang để `true` — trình duyệt từ chối cookie Secure trên kết nối không HTTPS |
| Trợ lý AI báo "Chưa cấu hình trợ lý AI" dù đã nhập khoá API | Kiểm tra `AI_SETTINGS_ENCRYPTION_KEY` không đổi giữa lúc lưu khoá và lúc dùng — đổi khoá gốc sau khi đã lưu sẽ làm dữ liệu cũ giải mã lỗi, phải nhập lại khoá API |
