## PROJECT_STATE — sau Phase 12 — 2026-07-06

### Đã chốt (Phase 12 — DevOps & tài liệu)
- `server/Dockerfile` (multi-stage: `maven:3.9-eclipse-temurin-21` build → `eclipse-temurin:21-jre-alpine`
  runtime, user non-root, healthcheck `/actuator/health`) và `client/Dockerfile` (multi-stage:
  `node:20-alpine` build → `nginx:1.27-alpine` runtime, healthcheck `GET /`) + `.dockerignore` riêng
  cho từng service (client cần thiết yếu — thiếu sẽ khiến `COPY . .` sau `npm ci` ghi đè
  `node_modules` mới cài bằng bản trên host).
- `docker/docker-compose.yml` (postgres, redis, server, web dùng chung 1 file cho cả 2 kịch bản
  triển khai VPS/LAN, chỉ khác `.env`) + `docker/nginx.conf` (SPA fallback, proxy `/api` sang
  `server`, gzip, cache tài nguyên tĩnh có hash) — `nginx.conf` mount qua volume thay vì `COPY`
  trong Dockerfile vì Docker không cho `COPY` đọc ngoài build context (`../docker/`).
  **Bẫy thật phát hiện lúc verify bằng `docker compose config`**: biến top-level (`${DB_NAME}`,
  `${WEB_PORT}`...) không được thay thế dù `.env` đã có ở root repo — Compose mặc định chỉ tự tìm
  `.env` cạnh chính file compose (`docker/.env`), không phải theo CWD hay root repo; phải luôn gọi
  kèm `--env-file .env` khi compose file nằm ở thư mục con. Đã ghi rõ vào README.
- **Bug thật phát hiện khi rà lại `pom.xml` trước khi viết CI**: thiếu `maven-failsafe-plugin` —
  Surefire mặc định chỉ nhận diện `*Test.java`, nên mọi `*IT.java` (Testcontainers, gồm cả
  `ProductRepositoryIT` từ Phase 3 và 2 IT mới ở Phase 11) **chưa từng thực sự chạy** dưới bất kỳ
  lệnh `mvn` nào từ đầu dự án — file tồn tại, compile sạch, nhưng không nằm trong tập test nào được
  thực thi, tạo cảm giác an toàn giả. Đã sửa bằng cách thêm `maven-failsafe-plugin` gắn vào goal
  `integration-test`+`verify`; xác nhận `mvn test` vẫn nhanh/không chạm Docker (chỉ Surefire) còn
  `mvn verify` (dùng trong CI) chạy cả 2. Bug này lẽ ra phải bắt được từ Phase 3 nếu có CI sớm hơn
  — minh chứng cụ thể cho lý do cần có Phase 12 sớm trong vòng đời dự án thật.
- **29 file Java lệch format phát hiện khi thử `mvn verify` lần đầu**: toàn bộ code viết tay xuyên
  suốt FH-12 → FH-16, Phase 11 và bản vá refresh-token chưa từng chạy qua `spotless:check` (chỉ
  chạy `mvn test`/`spring-boot:run` trong lúc phát triển, không phải `verify`). Đã sửa bằng
  `mvn spotless:apply` (thuần whitespace/import-order, không đổi ngữ nghĩa) — xác nhận lại
  `OrderPricingServiceTest` vẫn 23/23 xanh sau khi format lại.
- `.github/workflows/ci.yml`: 3 job — `backend` (`mvn verify`, upload surefire+failsafe reports),
  `frontend` (lint, `tsc --noEmit`, Vitest, build), `e2e` (services Postgres 16 + Redis 7 thật trên
  runner, build+chạy backend thật với profile `local` + `JWT_SECRET` CI riêng, build+chạy frontend
  dev server thật, chạy bộ Playwright E2E của Phase 11 thật, upload report khi fail).
  **Bug thật phát hiện khi rà `playwright.config.ts` trước khi viết job `e2e`**: hardcode
  `executablePath: "/opt/pw-browsers/chromium"` (chỉ tồn tại trong sandbox phát triển) — sẽ crash
  ngay trên GitHub Actions runner (không có đường dẫn này, CI tự `playwright install --with-deps
  chromium` rồi để Playwright tự tìm browser mặc định). Đã sửa bằng điều kiện `existsSync` — verify
  lại: 5/5 test Playwright vẫn pass trong sandbox sau khi sửa (nhánh `existsSync` vẫn đúng ở đây).
- `scripts/backup.sh`/`scripts/restore.sh` (pg_dump/psql qua `docker compose exec postgres`, đọc
  `.env` ở root, `restore.sh` có bước xác nhận trước khi ghi đè). `README.md` thay phần Docker
  Compose placeholder bằng hướng dẫn thật (`--env-file .env`, bảng so sánh biến môi trường 2 kịch
  bản triển khai) + mục "Vận hành" (backup/restore, tóm tắt CI).
- **Giới hạn môi trường (nhất quán với Phase 3/11)**: sandbox làm Phase 12 vẫn KHÔNG có Docker
  daemon (`docker ps`/`service docker start` đều lỗi) — Dockerfile/compose chỉ verify được qua
  `docker compose config` (thay thế biến + resolve path đúng) và review thủ công kỹ lưỡng, **chưa
  chạy được `docker build`/`docker compose up` thật** trong phiên này. Sẽ được GitHub Actions runner
  (có Docker daemon thật) xác nhận khi job `e2e` chạy (`mvn -B -q package` + start jar thật, dù
  không qua Dockerfile mà chạy trực tiếp — bản thân Dockerfile build image thì chưa có job CI nào
  build/push image, chỉ mới viết đúng cú pháp và review thủ công).

### Đã chốt (FH-1 → FH-16 — Redesign giao diện FruitHouse + tính năng mới)
Sau Phase 10, toàn bộ FE được redesign lại theo mockup FruitHouse (theme jade, sidebar tối màu cố
định) và bổ sung 6 tính năng nghiệp vụ hoàn toàn mới ở cả Backend lẫn FE (trước đó chưa có
Controller/Service/trang nào): Công nợ chi tiết (aging theo đối tác + đối chiếu + ghi nhận thanh
toán FIFO), Ma trận phân quyền (CRUD nhân viên + lưới sửa quyền theo vai trò), Ca & két tiền (mở/
đóng ca, thu chi tiền mặt, đối chiếu ket tien nối trực tiếp vào POS), Báo cáo (chart 2 chuỗi Doanh
thu/Lợi nhuận), Cài đặt (đọc/ghi cấu hình `settings` thật, nối `login_rate_limit_attempts` vào
AuthService runtime). Chi tiết từng trang không lặp lại ở đây — xem lịch sử commit
`FH-1`..`FH-16` trên nhánh `claude/new-session-4qaqne`. Nhân tiện rà soát hồi quy toàn bộ 16 trang
bằng Playwright, phát hiện và sửa 1 bug thật ở tầng nền tảng Phase 2 (không liên quan FH): cơ chế
rotation refresh token thu hồi nhầm cả chuỗi (đăng xuất oan) khi có ≥2 lệnh `/auth/refresh` đua
nhau dùng cùng 1 token hợp lệ (xảy ra thật ở multi-tab hoặc React StrictMode dev) — sửa bằng cách
giữ mỗi jti vừa rotate qua trong 1 khóa Redis riêng có TTL 30 giây thay vì 1 ô nhớ chung.

### Đã chốt (Phase 11 — Testing)
- `OrderPricingServiceTest` mở rộng từ 3 lên 23 case, phủ hết 7 bước B4 riêng lẻ và kết hợp (CK
  dòng về 0, guard chia 0 khi subtotal triệt tiêu, phân bổ CK đơn/voucher dư vào dòng cuối không
  lệch dù 5 dòng, VAT trộn/trên nhiều thuế suất/thuế suất 0%, số lượng lẻ làm tròn HALF_UP, 3 mức
  làm tròn 500/100/null, tiền thừa âm, số lượng 0). Tự phát hiện và sửa 5 lỗi tính tay trong chính
  test mới viết (dùng `roundingUnit=1000` lúc muốn kiểm tra tổng trước làm tròn, vô tình trúng
  điểm giữa làm tròn đổi kết quả) trước khi coi là xong — cùng kỷ luật "verify bằng chạy thật" như
  mọi Phase trước, chỉ khác đối tượng chạy thật ở đây là chính bộ test.
- Testcontainers integration test mới: `DebtRepositoryIT` (khóa lại bug thật FH-12 — JOIN UNION ALL
  customers/suppliers theo id trùng lặp từng làm lẫn tên đối tác sai chiều nợ) và
  `OrderRepositoryRevenueIT` (khóa lại cách tính giá vốn theo từng nhóm thêm ở FH-15 — subquery
  riêng tránh nhân đôi revenue trên đơn nhiều dòng). Môi trường sandbox làm Phase 11 vẫn KHÔNG có
  Docker daemon (đã xác minh lại qua `docker ps` và `service docker start` đều thất bại, đúng tình
  trạng ghi nhận từ Phase 3) — 2 file compile sạch, logic query đã verify gián tiếp qua curl thật
  trên Postgres 16 local trong lúc làm FH-12/FH-15, chạy được thật trên CI/local có Docker.
- Playwright E2E chính thức lần đầu (trước giờ chỉ chạy script tay để verify, không phải test suite
  commit vào repo): cài `@playwright/test`, `playwright.config.ts` trỏ thẳng Chromium cài sẵn trong
  môi trường (không tải lại), 5 test thật chạy qua `npm run test:e2e` — đăng nhập đúng/sai, bán
  hàng POS end-to-end, trang Công nợ, trang Ca & két tiền. Bug thật tự phát hiện lúc chạy lần đầu:
  toast Radix render 2 node cùng text (div hiển thị + span `aria-live` cho screen reader) khiến
  `getByText` strict-mode violation — sửa bằng `.first()`.

### Đã chốt (Phase 10 — Báo cáo)
- **Bug thật phát hiện qua verify actuator**: `spring-boot-starter-mail` (thêm ở Phase 9) tự đăng
  ký `MailHealthIndicator` góp phần vào status tổng hợp `/actuator/health` — SMTP gián đoạn tạm
  thời (không liên quan gì đến khả năng phục vụ của hệ thống) kéo cả `/actuator/health` xuống
  `DOWN`, mâu thuẫn trực tiếp nguyên tắc Phase 9 "email không được ảnh hưởng ngược lại luồng bán
  hàng" — nếu dùng làm readiness probe (K8s/load balancer) sẽ loại bỏ nhầm 1 instance đang bán hàng
  bình thường. Đã sửa bằng `management.health.mail.enabled: false`. Chi tiết:
  `docs/phase10/reports-module.md`.
- 7 endpoint báo cáo (doanh thu 5 chiều nhóm, lợi nhuận gộp, top SP, top KH, hiệu suất nhân viên,
  giá trị tồn kho, công nợ theo tuổi nợ) dùng `@Query(nativeQuery = true)` trực tiếp trên
  orders/order_items/returns/debts/inventory — chưa cần bảng tổng hợp `daily_sales_summary`.
  Xuất Excel qua Apache POI (`ReportExcelExporter` dùng chung mọi loại bảng).
- `ReportsPage` FE: biểu đồ Recharts (1 chuỗi doanh thu), bảng tái dùng `DataTable` (Phase 5), xuất
  Excel qua tải Blob (endpoint export cần header Authorization, không dùng `<a href>` được).
- Đã verify toàn bộ số liệu đối chiếu trực tiếp với SQL tay và dữ liệu seed + đơn test Phase 8/9
  (doanh thu theo chi nhánh khớp theo thu ngân, lợi nhuận gộp khớp từng đồng, giá trị tồn kho theo
  danh mục cộng lại khớp theo chi nhánh, phân quyền đúng ma trận, file Excel mở bằng `openpyxl`
  xác nhận đúng dữ liệu) — cả qua curl lẫn qua Playwright + Chromium thật. Bảng đầy đủ:
  `docs/phase10/reports-module.md`.

### Đã chốt (Phase 9 — Module Hóa đơn)
- `GET /api/v1/invoices/{id}` (quyền `invoice:view`) + `GET /api/v1/invoices/lookup/{lookupCode}`
  (công khai, khách quét QR không cần đăng nhập, an toàn IDOR vì `lookupCode` là UUID rút gọn
  không đoán được). `InvoiceDetailAssembler` (Java thuần, cùng phong cách `OrderPricingService`)
  gộp VAT theo từng thuế suất — verify bằng unit test "snapshot" dựng lại đúng đơn hàng thật
  HD-000021 đã kiểm chứng ở Phase 8, khớp từng đồng.
- Bổ sung `orders.cash_received`/`change_amount` (trước đây chỉ tính lúc tạo đơn, không lưu nên
  không tái tạo được khi xem hóa đơn cũ), `customers.email`, setting `store_name`/`store_tax_code`
  (migration `V4__invoice_fields.sql`).
- Gửi email hóa đơn qua Thymeleaf + `spring-boot-starter-mail`, publish sau khi `Invoice` được lưu
  và chỉ xử lý thật sự **sau khi transaction bán hàng commit** (`@TransactionalEventListener(phase
  = AFTER_COMMIT)`, D4) — **verify bằng SMTP server thật** (dựng nhanh `python3 -m smtpd` debug
  server cục bộ), xác nhận email đến đúng nội dung/đúng thời điểm (sau commit, không chặn luồng
  bán hàng), tiếng Việt có dấu hiển thị đúng.
- `EInvoiceProvider` (interface) + `NoOpEInvoiceProvider` (bean mặc định) — **chưa** tích hợp thật
  với nhà cung cấp hóa đơn điện tử nào (Viettel/MISA/VNPT đều cần hợp đồng thương mại thật, không
  thể kiểm chứng trong phiên làm việc này) — đã ghi rõ trong Javadoc cách thay thế sau.
- FE: `InvoiceK80`/`InvoiceA4` (template in K80 80mm + A4, `@page` CSS động theo khổ đang chọn),
  `InvoicePrintPage` (route bảo vệ, quyền `invoice:view`), `InvoiceLookupPage` (route công khai
  `/tra-cuu/:code`, **không** bọc `RequireAuth` — khách quét QR trên hóa đơn giấy), QR code qua
  `qrcode.react` mã hóa URL tra cứu.
- Đã verify toàn bộ bằng ứng dụng chạy thật (Playwright + Chromium + SMTP debug thật, không chỉ
  đọc code): email tự động gửi khi khách có email, endpoint JSON đúng/đủ (VAT breakdown khớp tổng
  tiền), in K80 và A4 đều đúng dữ liệu, trang tra cứu công khai hoạt động từ browser context mới
  hoàn toàn không có cookie đăng nhập, mã tra cứu sai hiện đúng lỗi tiếng Việt không crash trang.
  Chi tiết đầy đủ: `docs/phase9/invoice-module.md`.

### Đã chốt (Phase 5 — Frontend Foundation)
- **2 bug thật phát hiện khi verify bằng trình duyệt thật (Playwright + Chromium)** — cả hai
  chỉ lộ ra vì test bằng trình duyệt thật, không phải curl: (1) `AuthController` hard-code cookie
  refresh token `Secure=true` khiến trình duyệt từ chối lưu cookie khi chạy `http://localhost`
  (RFC 6265) — đã sửa bằng property `app.auth.refresh-cookie-secure` (mặc định `true`, override
  `false` chỉ ở `application-local.yml`); (2) origin `127.0.0.1:5173` không khớp CORS whitelist
  `localhost:5173` gây lỗi mạng chung chung. Chi tiết: `docs/phase5/frontend-foundation.md`.
- Scaffold Vite 5 + React 18 + TS 5 + Tailwind 3 (ép version thủ công vì `npm create vite` mặc
  định cài bản mới nhất, vi phạm Part C đã chốt); shadcn/ui viết tay từng primitive (không dùng
  CLI vì sandbox không truy cập được registry ngoài whitelist).
- 3 layout (`MainLayout` Sidebar+Topbar, `AuthLayout`, `PosLayout` toàn màn hình 2 cột) đúng
  `docs/phase4/layout.md`/`pos-design.md`; router `react-router-dom` 6 + `React.lazy` (xác nhận
  code-splitting qua `vite build` — mỗi trang 1 chunk riêng); dark/light qua Redux `ui.theme`.
- `apiClient` (axios) tự refresh khi 401 (hàng đợi chống refresh trùng lặp); `bootstrapSession()`
  silent-refresh lúc khởi động app để F5 giữa ca không đá thu ngân về `/login` dù access token
  chỉ sống trong RAM (D3); Redux Toolkit + `redux-persist` chỉ persist `cart`/`ui`, **`auth`
  không bao giờ persist**; TanStack Query cho toàn bộ dữ liệu API; RHF + Zod cho form.
- Component nền đủ theo Gate: `DataTable` (đọc `meta.page/limit/total`), `FormField`,
  `ConfirmDialog`, `Money`, `DateRangePicker`, `Toast`, `PermissionGate` + `RequireAuth`/
  `RequirePermission` (route guard).
- **Nợ kỹ thuật đã ghi nhận** (không phải lỗi ẩn): endpoint `/products`, `/customers` dùng native
  query có `ORDER BY` cố định (Phase 7) nên không nhận `Pageable.getSort()` động — demo sắp xếp ở
  `ProductsPage` tạm làm phía client trên trang hiện tại; cần chuyển sang
  `JpaSpecificationExecutor` khi xây màn danh sách thật cho từng module.
- Đã verify toàn bộ luồng bằng Playwright + Chromium thật (không chỉ đọc code): đăng nhập sai/đúng,
  redirect chưa đăng nhập, reload giữ phiên, DataTable phân trang/lọc/sắp xếp với dữ liệu thật,
  POS layout, dark mode, đăng xuất xoá cookie, chặn truy cập sau đăng xuất. Bảng đầy đủ tại
  `docs/phase5/frontend-foundation.md`.

### Đã chốt (Phase 8 — Module Bán hàng POS, QUAN TRỌNG NHẤT)
- **Bug thật phát hiện qua test tích hợp 2 luồng (gate bắt buộc)**: `generateOrderNumber()`/`generateInvoiceNumber()`/`generateSku()` dùng pattern `count()+existsBy()` không atomic — 2 request tạo đơn đồng thời đọc cùng `count()` trước khi bên nào commit, sinh trùng `order_number`, vi phạm unique constraint, che mất lỗi `PRODUCT_OUT_OF_STOCK` đúng nghĩa (trả về `409 CONFLICT` chung chung thay vì đúng mã lỗi nghiệp vụ). Đã sửa bằng PostgreSQL `SEQUENCE` (migration `V3__number_sequences.sql` + `NumberSequenceService` dùng chung) — atomic ở mức DB, không phụ thuộc transaction isolation. Verify lại: 1 luồng thành công/1 luồng đúng `PRODUCT_OUT_OF_STOCK`, tồn kho cuối chính xác. Chi tiết: `docs/phase8/pos-module.md`.
- **Gap thật phát hiện khi test trả hàng**: `OrderItemResponse` thiếu trường `id` — FE không có cách lấy `orderItemId` để gọi API trả hàng (`POST /returns` bắt buộc trường này). Đã bổ sung.
- `OrderPricingService` (Java thuần, đúng 7 bước B4, dòng cuối nhận phần dư phân bổ/làm tròn để tổng luôn khớp tuyệt đối) — verify cả bằng unit test lẫn đơn hàng thật qua API, khớp tính tay từng đồng.
- `OrderService.createOrder()`: 1 transaction duy nhất, Backend là nguồn giá/VAT/giá vốn duy nhất, chống oversell 2 lớp (kiểm tra tồn kho + `@Version` optimistic lock với `saveAndFlush()`), idempotency qua `Idempotency-Key` header (Redis), snapshot đầy đủ trên `OrderItem`, tự tạo `Debt`/`Invoice`/VietQR khi cần.
- `ParkedOrderService` (đặt trước giỏ hàng), `ReturnService` (trả hàng theo đơn giá hiệu lực đã phân bổ CK, giới hạn số lượng còn lại, hoàn kho theo giá vốn snapshot, trừ nợ liên kết trước), `VietQrService` (EMVCo + CRC16-CCITT tự viết, verify khớp test vector chuẩn).
- Đã verify bằng ứng dụng chạy thật (không chỉ compile): tạo đơn phức tạp (CK dòng+đơn+voucher+tiền thừa), idempotency (lặp key không tạo trùng), 2 và 3 luồng tranh tồn kho cuối, park/resume, trả hàng 1 phần + validate vượt số lượng, huỷ đơn cùng ngày (+ chặn huỷ lần 2), voucher 10%+bank_transfer (VietQR sinh đúng, `used_count` tăng đúng). Bảng đầy đủ tại `docs/phase8/pos-module.md`.

### Đã chốt (Phase 7 — Module Sản phẩm & Kho)
- **Bug thật phát hiện qua test chạy thật**: `BaseEntity.createdAt/updatedAt` kiểu `OffsetDateTime` làm Spring Data Auditing crash (`DefaultAuditableBeanWrapperFactory` bản Spring Boot 3.3.5 chỉ hỗ trợ `Instant` trong nhóm java.time, không hỗ trợ `OffsetDateTime`) — đã sửa sang `Instant`, verify tạo sản phẩm thành công sau khi sửa
- Category (cây 2 cấp) + Product (SKU tự sinh, lịch sử giá ghi trong Service thay vì `@EntityListeners` — lý do: JPA EntityListener không phải Spring bean, khó inject Repository an toàn) CRUD đầy đủ
- Tìm không dấu + gần đúng (`immutable_unaccent` + `ILIKE` + `pg_trgm %`) — **verify bằng dữ liệu có dấu thật**: "ca phe sua da" tìm ra "Cà phê sữa đá đặc biệt"
- `AverageCostService` Java thuần (không Spring) tính giá vốn bình quân gia quyền — **verify bằng phép tính tay chính xác**: tồn 98@38.500 + nhập 50@40.000 → giá vốn mới 39.007 (khớp `5.773.000/148=39006,756→HALF_UP`)
- `PurchaseOrderService`: khóa PESSIMISTIC_WRITE chống race condition, tự tạo công nợ NCC nếu mua thiếu — verify Debt payable tạo đúng số tiền
- `StockTakeService`: kiểm kê snapshot → nhập thực tế → duyệt bắt buộc lý do khi có chênh lệch — verify chặn duyệt thiếu lý do (422) và duyệt thành công điều chỉnh đúng tồn kho

### Đã chốt (Phase 6 — Backend Foundation)
- SecurityConfig STATELESS, CORS whitelist, CSRF tắt (giải thích lý do); JwtAuthenticationEntryPoint/AccessDeniedHandler trả JSON đúng format D2 ngay ở filter chain
- JWT (jjwt 0.12.6, API xác nhận qua javap): access token 15p chứa claim authorities; refresh token 7 ngày chứa jti+tokenFamily
- Refresh rotation + reuse detection qua Redis (`refresh:family:{family}` → jti hợp lệ hiện tại) — thu hồi cả chuỗi khi phát hiện token cũ bị dùng lại
- Rate limit đăng nhập 5 lần/15 phút/IP bằng Bucket4j 8.10.1 thật (LettuceBasedProxyManager, groupId `com.bucket4j` nhưng package Java `io.github.bucket4j` — đã xác nhận qua javap, không suy đoán)
- CustomUserDetailsService: authorities = hợp permission của mọi role được gán; ResourceActionPermissionEvaluator cho `hasPermission(id,'resource','action')`
- `@ValidPhoneVN`, upload ảnh (whitelist MIME, 2MB, UUID rename, serve qua endpoint riêng permitAll cho `<img>`), AuditAspect (`@Audited` + AOP), SettingsService (cache Redis, fallback branch→global)
- **Đã verify bằng ứng dụng chạy thật**: login/refresh/reuse-detection/rate-limit/upload đều test qua curl với PostgreSQL+Redis local thật — kết quả đầy đủ tại `docs/phase6/backend-foundation.md`

### Đã chốt (Phase 1–4, giữ nguyên)
- Design tokens: palette hex + HSL (light/dark), font Inter, radius 8px, thang shadow sm/md/lg — `docs/phase4/design-tokens.md` kèm `tailwind.config.ts` + CSS variables sẵn dùng cho Phase 5.1
- Layout chính: Sidebar (240px, thu gọn 64px) + Topbar (56px); POS dùng layout riêng toàn màn hình, không sidebar/topbar
- POS: 2 cột (60% sản phẩm / 40% giỏ hàng + thanh toán), 10 phím tắt F1–F9+Esc, scanner-friendly (auto-focus/refocus ô tìm kiếm), tối thiểu 1024×768, touch target ≥44px
- 15 wireframe màn hình (Dashboard, Sản phẩm, Nhập kho, Tồn kho, Kiểm kê, Đơn hàng, Trả hàng, Hóa đơn, Khách hàng, NCC, Công nợ, Nhân viên & phân quyền, Ca & két, Báo cáo, Cài đặt) + POS riêng — mỗi màn đủ mục đích/thành phần/hành động/trạng thái/phím tắt
- Trạng thái UI chuẩn dùng chung: loading (skeleton, không spinner toàn màn hình), empty (phân biệt chưa có dữ liệu vs lọc không ra kết quả), error (toast + map code→message, không trắng trang), dark mode (qua CSS variable, không hardcode hex), responsive 3 breakpoint

### Đã chốt (Phase 1–3, giữ nguyên)
- Không dùng Lombok (getter/setter/constructor viết tay, tường minh, không phụ thuộc annotation processor)
- Build tool: Maven
- Package gốc Java: `com.quanlycuahang.erp`
- Cấu trúc repo: `client/` + `server/` đặt thẳng ở root repo (không có thư mục wrapper `kiotclone/`)
- Format code Java: Spotless (google-java-format), chạy ở phase `verify`
- Spring Boot 3.3.5, Java 21, PostgreSQL 16, Redis 7, Flyway 10 (qua BOM Spring Boot)
- 6 vai trò RBAC: `owner`, `manager`, `cashier`, `sales_staff`, `warehouse_staff`, `accountant` — ma trận resource:action đầy đủ tại `docs/phase1/permission-matrix.md` (51 quyền)
- 19 use case (10 MUST + 9 SHOULD) đặc tả đầy đủ luồng chính/phụ/ngoại lệ/quy tắc, đối chiếu khớp mọi quy tắc B4
- Mermaid không có cú pháp `usecaseDiagram`/`deploymentDiagram` chuẩn UML → dùng `flowchart` thay thế
- Kiến trúc Controller → Service → Repository → Entity, constructor injection bắt buộc, DTO+MapStruct, Exception hierarchy (`AppException` + 5 lớp con), `GlobalExceptionHandler`, `ApiResponse<T>` D2, `CorrelationIdFilter` + logback JSON
- **Mâu thuẫn phát hiện & đã xử lý (Phase 3)**: B4 yêu cầu đồng thời `allow_negative_stock` (cho bán âm) và `CHECK (stock >= 0)` tĩnh ở DB — 2 điều này loại trừ nhau. Đã thay `CHECK` tĩnh bằng `TRIGGER` (`fn_check_inventory_stock`) đọc cấu hình `settings.allow_negative_stock` theo chi nhánh (hoặc global) trước khi chặn — đã test thực tế cả 2 kịch bản (chặn khi tắt, cho phép khi bật). Chi tiết: `docs/phase3/erd.md`.
- Schema đầy đủ 37 bảng (7 nhóm B4, gồm cả `user_branches` bổ sung cho kiểm soát IDOR đa chi nhánh — Phase 1.1 quy tắc 4), 34 JPA Entity tương ứng (3 bảng join thuần túy `role_permissions`/`user_roles`/`user_branches` không có Entity riêng, ánh xạ qua `@ManyToMany` + `@JoinTable`)
- unaccent() mặc định STABLE, không dùng được trực tiếp trong index expression → tạo hàm wrapper `immutable_unaccent()` (IMMUTABLE) — phát hiện qua test thật, không phải suy đoán
- **Đã verify thực tế** (không chỉ giả định): cài PostgreSQL 16 + Redis 7 local (sandbox không có Docker daemon), chạy `mvn spring-boot:run` với Flyway tự động migrate V1+V2 và **Hibernate `ddl-auto: validate` PASS** — xác nhận toàn bộ 34 Entity khớp chính xác schema. Actuator health trả `UP`.

### Cấu trúc project hiện tại
```
Quanlycuahang/
├── client/                          # Vite + React 18 + TS 5 (Phase 5)
│   ├── src/
│   │   ├── main.tsx, App.tsx, index.css
│   │   ├── components/
│   │   │   ├── ui/          # 18 primitive shadcn (button, input, dialog, form, toast...)
│   │   │   ├── layout/      # MainLayout, AuthLayout, PosLayout, Sidebar, Topbar, ThemeToggle
│   │   │   └── common/      # DataTable, FormField, ConfirmDialog, Money, DateRangePicker, PermissionGate
│   │   ├── pages/           # auth/LoginPage, DashboardPage, products/ProductsPage, pos/PosPage, invoices/{Print,Lookup}Page, reports/ReportsPage, NotFound/Forbidden
│   │   ├── routes/          # router.tsx, RequireAuth, RequirePermission
│   │   ├── store/           # index.ts (Redux + persist), slices/{auth,cart,ui}Slice
│   │   ├── lib/{http,api}/  # apiClient (refresh interceptor), queryClient, bootstrap, auth.ts, products.ts, invoices.ts, reports.ts
│   │   └── types/           # api.ts (ApiResponse<T>), permission.ts
│   ├── package.json, tailwind.config.ts, vite.config.ts, tsconfig*.json
│   └── *.test.ts(x)         # Vitest — utils, jwt decode, Money (6 test)
├── server/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/quanlycuahang/erp/
│       │   │   ├── ErpApplication.java
│       │   │   ├── config/JpaAuditingConfig.java
│       │   │   ├── common/{entity,dto,exception,web}/       # 12 file (Phase 2)
│       │   │   ├── auth/entity/          # User, Role, Permission
│       │   │   ├── system/entity/        # Branch, Settings, AuditLog
│       │   │   ├── product/entity/       # Category, Product, ProductUnit, PriceHistory
│       │   │   ├── product/repository/   # ProductRepository
│       │   │   ├── inventory/entity/     # Inventory, InventoryTransaction, PurchaseOrder(Item), StockTake(Item)
│       │   │   ├── partner/entity/       # CustomerGroup, Customer, Supplier, Debt, DebtPayment
│       │   │   ├── sales/{entity,service,controller,dto,pricing,statemachine,repository,web}/  # Order, ParkedOrder, Return, OrderPricingService, IdempotencyInterceptor...
│       │   │   ├── promotion/{entity,service,repository}/  # Voucher, VoucherUsage
│       │   │   ├── operation/{entity,repository,service,controller,dto,invoice}/  # Shift, Invoice, InvoiceDetailAssembler, EInvoiceProvider, InvoiceEmailListener
│       │   │   ├── report/{dto,service,controller,excel}/  # ReportService, ReportController, ReportExcelExporter (Phase 10)
│       │   │   └── common/sequence/NumberSequenceService.java   # SEQUENCE atomic cho order/invoice/sku
│       │   ├── resources/templates/invoice-email.html   # Thymeleaf (Phase 9)
│       │   └── resources/
│       │       ├── application*.yml, logback-spring.xml
│       │       └── db/migration/
│       │           ├── V1__init_schema.sql        (37 bang, trigger, index, extension)
│       │           ├── V2__seed_data.sql          (1 CN, 6 role, 51 quyen, 3 user, 5 danh muc, 30 SP, 5 KH, 3 NCC, 20 don)
│       │           ├── V3__number_sequences.sql   (order_number_seq, invoice_number_seq, sku_seq)
│       │           └── V4__invoice_fields.sql     (orders.cash_received/change_amount, customers.email, store_name/store_tax_code)
│       └── test/java/com/quanlycuahang/erp/
│           ├── product/ProductRepositoryIT.java
│           ├── partner/DebtRepositoryIT.java             (Phase 11)
│           ├── sales/OrderRepositoryRevenueIT.java        (Phase 11)
│           ├── sales/pricing/OrderPricingServiceTest.java (23 case, Phase 11)
│           └── operation/invoice/InvoiceDetailAssemblerTest.java
├── docker/                          # docker-compose.yml, nginx.conf (Phase 12)
├── docs/
│   ├── conventions.md, PROJECT_STATE.md
│   ├── phase1/  (8 file — permission matrix, business specs, diagrams)
│   ├── phase2/architecture.md
│   ├── phase3/erd.md
│   ├── phase4/  (design-tokens, layout, wireframes, pos-design, ui-states)
│   ├── phase5/frontend-foundation.md
│   ├── phase6/backend-foundation.md
│   ├── phase7/product-inventory-module.md
│   ├── phase8/pos-module.md
│   ├── phase9/invoice-module.md
│   └── phase10/reports-module.md
├── client/e2e/                      # Playwright E2E chính thức (Phase 11): auth, pos-checkout, debts, shifts
├── scripts/                         # backup.sh, restore.sh (Phase 12)
├── .github/workflows/ci.yml         # backend / frontend / e2e (Phase 12)
├── .env.example, .gitignore, README.md
```

### Database
- Bảng đã có: đủ 37 bảng (xem `docs/phase3/erd.md` mục 4)
- Migration Flyway mới nhất: `V4__invoice_fields.sql`
- Extension: `unaccent`, `pg_trgm`; function: `immutable_unaccent()`, `fn_check_inventory_stock()` + trigger; sequence: `order_number_seq`, `invoice_number_seq`, `sku_seq`

### API đã sinh
- Auth: `POST /api/v1/auth/{login,refresh,logout}`, `POST /api/v1/auth/change-password`
- Sản phẩm/Kho: `/api/v1/products`, `/api/v1/categories`, `/api/v1/inventory`, `/api/v1/purchase-orders`, `/api/v1/stock-takes`
- Khách hàng/Voucher: `/api/v1/customers`
- Bán hàng POS: `POST /api/v1/orders` (header `Idempotency-Key`), `GET /api/v1/orders/{id}`, `POST /api/v1/orders/{id}/cancel`, `/api/v1/parked-orders` (list/park/resume), `POST /api/v1/returns`
- Hóa đơn: `GET /api/v1/invoices/{id}` (quyền `invoice:view`), `GET /api/v1/invoices/lookup/{lookupCode}` (permitAll)
- Báo cáo: `GET /api/v1/reports/{revenue,gross-profit,top-products,top-customers,employee-performance,inventory-value,debt-aging}` + `.../export` (Excel, cần thêm quyền `report:export`)
- Upload: `POST /api/v1/uploads`, `GET /api/v1/uploads/{fileName}` (permitAll)
- Actuator mặc định (`/actuator/health`, `/actuator/info`) — `management.health.mail.enabled: false` (Phase 10, xem Nợ kỹ thuật)
- **Nợ**: chưa có `BranchController` (CRUD chi nhánh) — hiện chỉ có `BranchRepository`, dùng nội bộ trong `OrderService`/`SettingsService`; cần bổ sung nếu FE cần màn quản lý chi nhánh
- Property mới `app.auth.refresh-cookie-secure` (phát hiện ở Phase 5 khi verify bằng trình duyệt thật — xem mục Phase 5 bên dưới)

### FE đã sinh
- Scaffold đầy đủ Vite + React 18 + TS 5 + Tailwind 3, 18 component `ui/` (shadcn viết tay), 3
  layout, router lazy-load, Redux+persist, axios refresh interceptor, TanStack Query, RHF+Zod.
- 2 trang thật gọi API Backend: `LoginPage` (đăng nhập/đăng xuất/refresh), `ProductsPage`
  (`DataTable` phân trang/lọc/sắp xếp với dữ liệu thật). `DashboardPage`/`PosPage` là khung/placeholder
  (số liệu và tính năng bán hàng thật thuộc phạm vi module riêng, ngoài Phase 5 Foundation).
- Chi tiết đầy đủ + bảng verify bằng trình duyệt thật: `docs/phase5/frontend-foundation.md`.
- (Phase 9) Bổ sung `InvoicePrintPage` (khổ K80/A4, route bảo vệ) và `InvoiceLookupPage` (route
  công khai `/tra-cuu/:code`, không đăng nhập) + QR qua `qrcode.react` — xem `docs/phase9/invoice-module.md`.
- (Phase 10) `ReportsPage`: biểu đồ Recharts + bảng `DataTable` cho 7 loại báo cáo, xuất Excel qua
  tải Blob (endpoint export cần header Authorization) — xem `docs/phase10/reports-module.md`.

### Nợ kỹ thuật / dang dở
- Dockerfile/docker-compose.yml/CI đã có (Phase 12) nhưng **chưa từng chạy `docker build`/
  `docker compose up` thật trong sandbox này** (không có Docker daemon) — chỉ verify qua
  `docker compose config` + review thủ công; cần build/chạy thật lần đầu trên máy/CI có Docker
  trước khi coi image là đã kiểm chứng đầy đủ.
- `ProductRepositoryIT`/`DebtRepositoryIT`/`OrderRepositoryRevenueIT` dùng Testcontainers — viết đúng chuẩn, giờ đã được `maven-failsafe-plugin` (Phase 12) nhận diện và chạy đúng ở phase `verify`, nhưng **chưa tự chạy được trong sandbox này** (không có Docker daemon khả dụng); đã verify tương đương bằng PostgreSQL/Redis cài trực tiếp + `spring-boot:run` thật (xem trên) — sẽ chạy thật lần đầu trên GitHub Actions (runner có Docker daemon sẵn, xem `.github/workflows/ci.yml` job `backend`)
- `stock_transfers` (chuyển kho đa chi nhánh, COULD) chưa thiết kế
- Wireframe hiện là mô tả text + Mermaid box diagram (chưa phải hình ảnh/Figma) — đủ chi tiết để code Phase 5 nhưng không có mockup trực quan; có thể bổ sung sau nếu cần
- Chưa có `BranchController` (CRUD chi nhánh qua API) — chỉ 1 chi nhánh seed sẵn, đủ cho Phase 8 test nhưng cần bổ sung trước khi FE cần màn quản lý đa chi nhánh (Phase 10: `ReportsPage` cũng chưa có bộ lọc chi nhánh vì lý do này)
- Chưa seed `vouchers` mẫu trong `V2__seed_data.sql` (test Phase 8 tự thêm 1 voucher tạm qua SQL trực tiếp, không lưu vào migration) — nên bổ sung vào seed data chính thức ở phase sau nếu cần demo
- Báo cáo "Công nợ kèm tuổi nợ" mới trả tổng hợp theo mức tuổi nợ (0-30/31-60/61-90/>90 ngày), chưa có danh sách chi tiết từng khách hàng/NCC kèm tuổi nợ riêng — cần module CRUD Công nợ (`DebtController` chưa tồn tại) để hỗ trợ duyệt/xem từng khoản
- `DataTable` sắp xếp mới hoạt động phía client (trang hiện tại) cho `/products` vì native query Phase 7 có `ORDER BY` cố định, chưa nhận `Pageable.getSort()` động — xem `docs/phase5/frontend-foundation.md` để biết cách chuyển sang `JpaSpecificationExecutor` khi cần sắp xếp server-side thật cho từng module
- Chưa có endpoint `/me` (thông tin user hiện tại) — FE giải mã payload JWT (`sub`, `authorities`) để lấy username/quyền hiển thị UI, `fullName` tạm dùng lại `username` vì token không có trường này; nên bổ sung `/me` nếu cần hiển thị đầy đủ hồ sơ nhân viên
- Các trang nghiệp vụ FE (CRUD sản phẩm/khách hàng/kho, giỏ hàng POS thật, báo cáo...) chưa xây — Phase 5 chỉ là "Foundation" (scaffold + hạ tầng + component nền) đúng phạm vi master prompt, không phải toàn bộ giao diện
- `EInvoiceProvider` mới có `NoOpEInvoiceProvider` (bean mặc định, không gửi đi đâu) — chưa tích hợp thật với Viettel S-Invoice/MISA/VNPT (cần hợp đồng thương mại thật, ngoài khả năng phiên làm việc này); xem Javadoc `EInvoiceProvider` để biết cách thay thế khi có nhà cung cấp thật
- Email hóa đơn mới verify bằng SMTP debug server cục bộ (`python3 -m smtpd`), chưa test với SMTP thật (Gmail/SES/SendGrid...) — cần kiểm tra lại cấu hình `spring.mail.properties.mail.smtp.starttls`/`auth` khi triển khai thật với nhà cung cấp SMTP yêu cầu STARTTLS/xác thực

### Tự đánh giá Phase 10
- **Mạnh**: mọi số liệu báo cáo đều đối chiếu bằng SQL tay trực tiếp trên DB (không chỉ tin API trả về đúng) — phát hiện 1 bug thật (mail health indicator kéo sập `/actuator/health` toàn hệ thống) mà chỉ lộ ra khi gọi thật `/actuator/health` sau khi thêm dependency mail ở Phase 9, compile/test không bao giờ phát hiện được vì đó là hành vi runtime của auto-configuration, không phải lỗi logic.
- **Thiếu**: chưa có bảng tổng hợp `daily_sales_summary`/`@Scheduled` (chấp nhận được ở quy mô mục tiêu, đã ghi rõ điều kiện cần bổ sung); "công nợ kèm tuổi nợ" mới là tổng hợp theo mức, chưa phải danh sách chi tiết từng đối tác (cần module Công nợ riêng); chưa xuất PDF báo cáo (nhất quán với quyết định "ưu tiên in trình duyệt" đã chốt ở Phase 9, chưa thêm gì mới).
- **Rủi ro**: COGS trong lợi nhuận gộp tính theo số lượng bán gốc (không trừ hàng đã hoàn) trong khi "ảnh hưởng hoàn trả" trừ riêng theo tổng tiền hoàn — đúng theo đúng nghĩa đen công thức B4 nhưng là 1 lựa chọn kế toán đơn giản hóa (không khớp lại COGS với return cùng kỳ); nếu sau này cần độ chính xác kế toán cao hơn (return ăn khớp đúng theo lô hàng gốc), cần thiết kế lại.

### Tự đánh giá Phase 9
- **Mạnh**: verify bằng cả ứng dụng thật (Playwright + Chromium) lẫn hạ tầng ngoài thật (SMTP debug server thật, không mock) — xác nhận đúng thứ tự event (gửi email SAU commit, không chặn luồng bán hàng), nội dung email/JSON/2 khổ in đều khớp dữ liệu gốc từng đồng; unit test "snapshot" tái sử dụng đúng số liệu đơn hàng thật đã verify ở Phase 8 thay vì bịa dữ liệu mới, tăng độ tin cậy liên phase.
- **Thiếu**: chưa tích hợp `EInvoiceProvider` thật (đã ghi nợ rõ ràng, không thể làm được trong phạm vi phiên này); chưa xuất PDF lưu server (chỉ in trình duyệt qua `window.print()`, đúng khuyến nghị "ưu tiên in trình duyệt" của master prompt nhưng chưa có phương án lưu file phía server nếu cần).
- **Rủi ro**: trang tra cứu công khai (`/tra-cuu/:code`) lộ `customerPhone`/`customerEmail` cho bất kỳ ai có `lookupCode` — chấp nhận được vì hóa đơn giấy vật lý vốn đã có các thông tin này, nhưng cần cân nhắc lại nếu sau này có yêu cầu ẩn bớt thông tin nhạy cảm trên bản tra cứu công khai.

### Tự đánh giá Phase 5
- **Mạnh**: verify bằng Playwright + Chromium thật (không chỉ đọc code hay chỉ chạy Vitest) — phát hiện 2 bug thật (cookie Secure chặn refresh ở dev local, CORS origin 127.0.0.1 vs localhost) mà chỉ hiện ra khi trình duyệt thật áp dụng đúng chính sách cookie/CORS, curl không bao giờ phát hiện được; DataTable/routing/theme/auth đều test qua thao tác thật trên UI, có ảnh chụp màn hình đối chiếu.
- **Thiếu**: chưa xây các trang nghiệp vụ thật ngoài Products demo; DataTable sort chưa server-side cho mọi endpoint (đã ghi nợ kỹ thuật rõ ràng ở trên); chưa có test Playwright tự động hoá trong CI (mới chạy tay 1 lần), nên viết thành E2E suite chính thức ở Phase 11.
- **Rủi ro**: `fullName` hiển thị tạm bằng `username` do thiếu endpoint `/me`; nếu sau này thêm claim `fullName` vào JWT cần nhớ cập nhật `decodeJwtPayload`/`AccessTokenClaims` cho khớp.

### Tự đánh giá Phase 8
- **Mạnh**: đúng kỷ luật verify bằng ứng dụng chạy thật + test tích hợp đa luồng (không chỉ unit test) theo đúng yêu cầu gate — phát hiện 1 bug thật (race condition sinh số đơn/hoá đơn/SKU) và 1 gap thật (thiếu `orderItemId` trong response) mà unit test/compile không thể phát hiện được, vì cả hai chỉ lộ ra khi có 2 request đồng thời chạm DB thật hoặc khi thử dùng đúng luồng trả hàng end-to-end.
- **Thiếu**: chưa test luồng bán nợ đầy đủ (tạo Debt khi thanh toán thiếu) qua API thật, mới verify qua đọc code; chưa test kịch bản `allow_negative_stock=true` ở Phase 8 (đã test ở Phase 3 cho cơ chế DB, chưa test lại qua `createOrder()`).
- **Rủi ro**: cơ chế chống oversell hiện tại không tự động retry khi thua optimistic lock — client (FE) phải tự xử lý lỗi `PRODUCT_OUT_OF_STOCK` và có thể yêu cầu người dùng thử lại; cần đảm bảo Phase 5 (FE POS) xử lý đúng lỗi này bằng cách tải lại giỏ hàng/tồn kho thay vì chỉ hiện thông báo.

### Tự đánh giá Phase 4
- **Mạnh**: token màu đạt tương phản WCAG AA cả 2 theme; POS thiết kế đủ chi tiết để code thẳng không cần hỏi lại (đủ 10 phím tắt, hành vi auto-focus rõ ràng); mọi màn đều có đủ 5 mục theo Gate (mục đích/thành phần/hành động/trạng thái/phím tắt).
- **Thiếu**: chưa có mockup hình ảnh trực quan (Figma-style) — chỉ có mô tả text/Mermaid; nếu cần trình bày cho stakeholder không kỹ thuật, nên bổ sung mockup HTML/hình ảnh riêng.
- **Rủi ro**: chưa test tương phản màu thực tế bằng công cụ (mới tính toán HSL thủ công) — cần kiểm chứng lại bằng contrast checker khi có code thật ở Phase 5.

### Tự đánh giá Phase 3 (giữ nguyên)
- **Mạnh**: verify bằng ứng dụng Spring Boot chạy thật (không chỉ đọc code), phát hiện và sửa 2 lỗi thực tế (unaccent IMMUTABLE, mâu thuẫn allow_negative_stock) trước khi bàn giao thay vì để lại nợ kỹ thuật ẩn.
- **Thiếu**: chưa có Controller/Service (đúng phạm vi Phase 3, sẽ có ở Phase 6 trở đi); seed data đơn giản hóa (20 đơn không có chiết khấu/voucher — đủ cho dev/demo, kịch bản đầy đủ để ở Phase 11 test).
- **Rủi ro**: `ProductRepositoryIT` chưa được CI thực thi trong phiên làm việc này do thiếu Docker — cần chạy xác nhận trên môi trường có Docker trước khi merge.

### Tự đánh giá Phase 12
- **Mạnh**: đúng kỷ luật "verify bằng chạy thật" dù bị giới hạn Docker daemon — thay vì chỉ đọc lại
  code, đã chủ động thử `mvn verify`/`docker compose config`/`npx playwright test` trước khi coi là
  xong, và nhờ vậy phát hiện 3 lỗi thật không lộ ra nếu chỉ viết file rồi dừng: thiếu
  `maven-failsafe-plugin` (IT test câm lặng từ Phase 3), 29 file lệch format (chưa từng qua
  spotless), và `playwright.config.ts` hardcode path sẽ crash job `e2e` trên CI thật.
- **Thiếu**: chưa build/chạy được `docker build`/`docker compose up` thật (không có Docker daemon
  trong sandbox) — đây là rủi ro lớn nhất còn lại của Phase 12, vì Dockerfile/compose mới được
  review thủ công + `docker compose config`, chưa có bằng chứng runtime thật (ví dụ: healthcheck
  có thật sự pass, image build có lỗi dependency ẩn nào không). Cần chạy thật lần đầu ngay khi có
  máy/CI có Docker trước khi tin tưởng hoàn toàn.
- **Rủi ro**: `.github/workflows/ci.yml` bản thân nó cũng chưa từng chạy thật trên GitHub Actions
  (chỉ viết đúng cú pháp dựa trên tài liệu + kinh nghiệm, review kỹ từng bước) — cần theo dõi lần
  chạy CI đầu tiên sau khi push để xử lý các lỗi phát sinh chỉ lộ ra trên runner thật (khác biệt
  version tool, timing khởi động service, quyền Docker trên runner...).

### Kế tiếp
Phase 0–12 của master prompt đã hoàn tất (Khởi tạo, Nghiệp vụ, Kiến trúc, Database, Thiết kế giao
diện, Frontend Foundation, Backend Foundation, Sản phẩm & Kho, Bán hàng POS, Hóa đơn, Báo cáo,
Testing, DevOps & tài liệu), cộng thêm FH-1 → FH-16 (redesign FruitHouse + 6 tính năng mới ngoài
phạm vi master prompt gốc). Không còn phase nào được tài liệu dự án liệt kê là chưa làm; việc còn
lại là xác nhận CI/Docker chạy thật lần đầu trên môi trường có Docker daemon (xem "Nợ kỹ thuật" và
"Tự đánh giá Phase 12" ở trên) — không phải công việc phát triển mới.
