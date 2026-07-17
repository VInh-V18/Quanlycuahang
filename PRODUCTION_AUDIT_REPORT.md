# PROJECT PRODUCTION AUDIT REPORT

**Dự án:** FruitHouse ERP/POS (Vietnamese multi-tenant SaaS)
**Ngày audit:** 2026-07-17
**Phạm vi:** Toàn bộ source code (client React/TS + server Spring Boot/Java + Docker/CI/CD + PostgreSQL) trước khi deploy production.
**Phương pháp:** Đọc trực tiếp source code hiện tại (không suy đoán), đối chiếu với tài liệu audit đã có sẵn (`SECURITY_AUDIT_REPORT.md`, `BUGS_FOUND.md`, `docs/PROJECT_STATE.md`) để không lặp lại việc đã làm, xác nhận từng claim cũ vẫn đúng ở code hiện tại thay vì chỉ tin tài liệu.

---

## 1. Tổng quan project

FruitHouse là hệ thống POS/ERP bán lẻ (trái cây/tạp hóa) cho nhiều cửa hàng, vận hành như multi-tenant SaaS (1 database, cách ly bằng `tenant_id` + chi nhánh `branch_id`).

**Stack thực tế:**

| Lớp | Công nghệ |
|---|---|
| Frontend | React 18 + TypeScript + Vite + Tailwind CSS + React Router + Axios + TanStack Query + Redux Toolkit |
| Backend | Java 21 + Spring Boot 3.3.5 + Spring Security + Spring Data JPA/Hibernate + JWT (access token trong bộ nhớ, refresh token httpOnly cookie) |
| Database | PostgreSQL 16 (Flyway migration, hiện tại V1→V35) |
| Cache/Queue | Redis (rate limit, AI conversation memory) |
| Hạ tầng | Docker multi-stage + Docker Compose (base + prod/observability/backup overlay) + Nginx reverse proxy |
| Phụ trợ | `ml-service` (Python FastAPI, dự báo AI), Ollama (tùy chọn, AI tự host) |

**Mức độ trưởng thành — điểm quan trọng nhất của audit này:** Dự án đã trải qua **12 vòng nâng cấp có hệ thống** (`docs/UPGRADE_ROADMAP.md` "Prompt #1–#12", tất cả đã ✅ Xong, xem `docs/PROJECT_STATE.md`), bao gồm: viết test tầng service, refactor god-class `OrderService`, bật rate limiting, **audit cách ly tenant/IDOR chuyên sâu bằng test thật** (`SECURITY_AUDIT_REPORT.md`), tối ưu N+1/pagination/cache, observability (Prometheus/Grafana), CI/CD + backup/restore **đã test thật trên database production**, dọn dead-code, module AI (đã có lớp bảo vệ riêng: DB role chỉ đọc, mã hóa khóa API AES-256-GCM).

**Kết quả audit lần này:** Đọc lại toàn bộ 11 phase theo đúng yêu cầu, dùng 5 agent song song đọc trực tiếp code (không dựa vào tài liệu cũ) cho Frontend/Backend/Security/Database/Docker. **Không phát hiện lỗi Critical nào còn tồn tại.** Phần lớn phát hiện là cải tiến Medium/Low (hiệu năng, index, chuẩn hóa) — phản ánh đúng thực tế 1 codebase đã được hardening kỹ, không phải 1 dự án mới toanh chưa ai audit.

---

## 2. Architecture review

### 2.1 Cấu trúc thư mục Backend (`server/src/main/java/com/quanlycuahang/erp/`)

Chia theo **module nghiệp vụ** (package-by-feature), mỗi module có `controller/dto/entity/mapper/repository/service` riêng — KHÔNG chia theo layer toàn cục (package-by-layer):

```
auth/ (36 file)        — đăng nhập, JWT, phân quyền
sales/ (48 file)       — đơn hàng, trả hàng, giá, state machine
inventory/ (37 file)   — tồn kho, nhập hàng, kiểm kê
partner/ (31 file)     — khách hàng, nhà cung cấp, công nợ
operation/ (28 file)   — ca làm việc, hóa đơn, upload
common/ (30 file)      — hạ tầng dùng chung (exception, audit, rate-limit...)
system/ (19 file)      — cài đặt, danh mục
ai/ (20 file)          — trợ lý AI
platform/ (17 file)    — Super Admin (quản trị nhiều tenant)
product/ (17 file)     — sản phẩm, danh mục
promotion/ (8 file)    — voucher/khuyến mãi
reconciliation/ (9)    — đối soát dữ liệu
report/ (11 file)      — báo cáo + xuất Excel
dashboard/ (4), config/ (4)
```

**Điểm tốt:**
- Package-by-feature giúp tìm code liên quan đến 1 nghiệp vụ nhanh, giảm coupling giữa các domain không liên quan (vd sửa `inventory` không đụng `partner`).
- Mọi entity nghiệp vụ đều kế thừa `TenantScopedEntity` — 1 điểm chặn duy nhất cho cách ly tenant, không rải rác `tenant_id` thủ công ở từng entity (trừ 14 native-query đã audit riêng).
- **Không entity nào được trả trực tiếp ra Controller** — xác nhận 100% qua agent audit, luôn qua DTO. Đây là điểm kiến trúc quan trọng nhất giúp loại bỏ cả lớp lỗi "infinite JSON recursion"/rò rỉ field nội bộ.
- `OrderService` (nghiệp vụ lõi, phức tạp nhất) đã được refactor từ 721 dòng xuống 524 dòng + 4 collaborator chuyên biệt (`OrderValidationService`/`InventoryDeductionService`/`OrderPaymentService`/`OrderFinalizationService`) — xác nhận qua agent vẫn đúng ở code hiện tại.
- **Không có `@OneToMany`/`@ManyToMany` (ngoài Role/User phân quyền) trên bất kỳ entity nghiệp vụ nào** — dòng con (order items, purchase order items...) luôn load tường minh qua repository query trong Service, không navigate qua object graph. Loại bỏ hoàn toàn rủi ro N+1-qua-lazy-collection và JSON-recursion mà kiến trúc JPA truyền thống hay mắc phải.

**Điểm yếu:**
- `OrderService.java` (524 dòng) vẫn là service lớn nhất — đã refactor 1 lần nhưng do là orchestrator trung tâm của nghiệp vụ bán hàng nên khó nhỏ hơn nữa mà không mất tính mạch lạc.
- 1 vài Controller (`InventoryController`, `OrderController` — đúng 2 file vừa sửa trong phiên làm việc gần nhất để thêm xuất Excel) đã để lọt logic tính toán/nhãn trạng thái vào tầng Controller thay vì Service (chi tiết ở mục 8).
- Không có ranh giới module rõ ràng bằng Java module system (`module-info.java`) hay Maven multi-module — mọi package đều `public`, không có gì ngăn `inventory` import trực tiếp nội bộ `sales` ngoài kỷ luật code-review. Ở quy mô hiện tại (15 module nghiệp vụ) chưa phải vấn đề thực tế, nhưng đáng cân nhắc nếu team lớn hơn.

### 2.2 Cấu trúc thư mục Frontend (`client/src/`)

```
components/  — common/ai/invoice/layout/partners/products/ui (shadcn)
pages/       — 1 thư mục con / 1 domain nghiệp vụ, khớp 1-1 với route
lib/         — api/ (1 file/domain, gọi REST), hooks/, http/ (axios), pos/ (business logic thuần)
routes/      — router.tsx (khai báo route + lazy load)
store/       — Redux Toolkit (auth, branch...)
types/       — kiểu dùng chung
```

**Điểm tốt:**
- **100% route được `lazy()` + `Suspense`** — code-splitting theo route đã làm đúng chuẩn từ đầu, không phải bổ sung.
- Tách rõ `lib/api/*.ts` (gọi HTTP thuần) khỏi `pages/*.tsx` (UI) — logic gọi API không lẫn vào component.
- `lib/pos/pricing.ts` — có tiền lệ tách business logic thuần (không phụ thuộc React) ra khỏi component để test độc lập (`clampEditablePrice()`, xem `BUGS_FOUND.md`).
- Named export theo file, không có barrel file `index.ts` gom toàn bộ (tránh vòng lặp import ẩn — vấn đề thường gặp ở dự án React lớn).

**Điểm yếu:**
- `pages/*.tsx` không tách `hooks/` cục bộ riêng — 1 số trang lớn (`PosPage.tsx` 669 dòng, `SettingsPage.tsx` 477 dòng) nhét toàn bộ state/mutation/JSX vào 1 file thay vì tách `usePosCart()`/`useSettingsForm()` riêng (chi tiết mục 8).
- Không có `contexts/` như cấu trúc chuẩn đề xuất trong yêu cầu — dự án dùng Redux Toolkit thay cho Context API cho state toàn cục (auth, chi nhánh hiện tại), đây là lựa chọn hợp lý hơn Context cho state hay đổi, không phải thiếu sót.

### 2.3 Đề xuất kiến trúc production

Kiến trúc hiện tại **đã ở mức phù hợp production** cho quy mô hiện tại (SaaS 1 database dùng chung, ~50 tenant mục tiêu theo comment trong `ReportService`). Không đề xuất tái cấu trúc lớn. 2 cải tiến đáng cân nhắc khi team/traffic lớn hơn:

1. Tách `lib/api/*.ts` + `pages/*.tsx` chung 1 domain lớn (`PosPage`) thành `features/pos/{api,hooks,components}/` khi file vượt 400-500 dòng — feature-folder thay vì domain dồn hết vào `pages/`.
2. Nếu sau này tách microservice, ranh giới package hiện tại (`sales`/`inventory`/`partner`...) đã là ứng viên tự nhiên cho bounded context — không cần thiết kế lại từ đầu.

---

## 3. Critical issues

**Không phát hiện lỗi Critical nào trong toàn bộ audit lần này** (0 issue, cả 5 agent + phần tôi tự kiểm tra). Đây là kết quả thực tế của 12 vòng hardening trước đó, không phải audit hời hợt — mỗi agent đọc trực tiếp code, chạy `npm audit`/`npm run build`, grep toàn repo tìm secret, đối chiếu migration với repository, không dừng ở mức tin tài liệu cũ.

2 phát hiện được xếp **High** (không phải Critical — không có bằng chứng đang bị khai thác hoặc sẽ crash production ngay) đáng chú ý nhất, chi tiết đầy đủ ở mục 6/8:

- **Không có CHECK constraint chặn số tiền âm** ở cấp database (`orders`, `order_items`, `debts`...) — chỉ được chặn ở tầng ứng dụng (validate). Nếu có bug tương lai ở tầng service bỏ sót validate, database vẫn chấp nhận ghi số âm.
- **Không có ràng buộc DB-level đảm bảo FK cùng tenant** (vd `orders.customer_id` có thể trỏ tới customer của tenant khác nếu tầng ứng dụng có lỗ hổng) — hiện được bảo vệ hoàn toàn ở tầng ứng dụng (`TenantAwareRepositoryImpl` + Hibernate `@Filter`, đã xác nhận bằng 8 test tích hợp thật), nhưng không có lớp phòng thủ thứ 2 ở DB.

Cả 2 đều là "defense-in-depth" còn thiếu, không phải lỗ hổng đang khai thác được — xem đề xuất khắc phục ở mục 6.

---

## 4. Security issues

Không có Critical/High được XÁC NHẬN khai thác được. Tổng hợp từ agent Security (đọc trực tiếp `JwtService`/`SecurityConfig`/`AuthService`/`FileStorageService`, chạy grep toàn repo tìm secret) + xác nhận chéo từ agent Docker/DevOps:

| # | File | Dòng | Nguyên nhân | Cách sửa | Mức độ |
|---|---|---|---|---|---|
| 1 | `docker/docker-compose.observability.yml` | 28 | `GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_ADMIN_PASSWORD:-admin}` — nếu bật overlay observability mà quên đặt biến này trong `.env` (hiện đang THIẾU trong `.env` thật), Grafana dùng mật khẩu mặc định `admin` ai cũng đoán được. Rủi ro giảm nhẹ vì Grafana không publish port ra ngoài (chỉ qua SSH tunnel theo comment trong file), nhưng vẫn là 1 lớp phòng thủ yếu. | Đặt `GRAFANA_ADMIN_PASSWORD=<mật khẩu ngẫu nhiên>` vào `.env` TRƯỚC khi bật overlay này; cân nhắc bỏ hẳn giá trị `:-admin` mặc định để compose báo lỗi rõ ràng thay vì âm thầm dùng mật khẩu yếu. | Low |
| 2 | `server/pom.xml` | 10 | Spring Boot cố định `3.3.5` — chưa xác nhận với CVE feed mới nhất tại đúng thời điểm deploy (agent không có quyền truy cập mạng để tra cứu CVE thời gian thực). | Chạy `mvn versions:display-dependency-updates` hoặc OWASP dependency-check ngay trước khi deploy, nâng lên bản patch 3.3.x mới nhất nếu có. | Medium (việc cần làm ngay trước deploy, không phải lỗ hổng đã xác nhận) |

**Đã xác nhận VẪN đúng ở code hiện tại (không phải chỉ tin tài liệu cũ):**
- Cách ly tenant (`TenantAwareRepositoryImpl`, Hibernate `@Filter`, `BranchAccessGuard`) — còn nguyên như `SECURITY_AUDIT_REPORT.md` mô tả.
- `.env` KHÔNG được commit vào git (`git log --all -- .env` rỗng) — `JWT_SECRET` thật hiện tại KHÁC giá trị mẫu trong `.env.example` (sự cố Critical từng xảy ra trước đây, đã vá, xác nhận không tái diễn).
- JWT: HS256 + secret ≥256-bit bắt buộc (ném lỗi khi khởi động nếu ngắn hơn), access token 15 phút, refresh token 7 ngày qua **httpOnly + Secure + SameSite=Strict cookie** (không phải header — thiết kế chủ động chống XSS đánh cắp token), có cơ chế phát hiện refresh-token-reuse (thu hồi cả family).
- Mật khẩu: BCrypt cost 12, không có endpoint tự đặt lại mật khẩu không xác thực nào (chỉ admin reset), thông báo lỗi đăng nhập giống nhau cho "sai mật khẩu" và "không tồn tại user" (chống dò tài khoản).
- CORS: đọc từ `CORS_ALLOWED_ORIGINS` (env), KHÔNG dùng `"*"` kết hợp `allowCredentials(true)` (tổ hợp nguy hiểm kinh điển).
- CSRF: tắt có chủ đích + đúng mô hình (JWT bearer stateless, cookie duy nhất là refresh token đã `SameSite=Strict` nên tự chống CSRF độc lập).
- XSS: 0 kết quả `dangerouslySetInnerHTML`/`innerHTML`/`eval` trong toàn bộ `client/src`. Header `Content-Disposition` ở mọi endpoint xuất file (Excel, ảnh) đều dùng tên file cố định hoặc đã qua validate UUID+đuôi file — không có đường tiêm header.
- Rate limit: đăng nhập có bucket riêng theo IP + theo username (5 lần/15 phút, cấu hình được theo tenant), tách biệt khỏi `ApiRateLimitFilter` chung.
- Upload file: whitelist MIME + kiểm tra magic-byte thật (không chỉ tin header), đổi tên UUID, chặn path traversal, giới hạn 2MB, lưu ngoài webroot.

---

## 5. Performance issues

| # | File | Dòng | Nguyên nhân | Cách sửa | Mức độ |
|---|---|---|---|---|---|
| 1 | `client/vite.config.ts` | 6-31 | Không cấu hình `build.rollupOptions.output.manualChunks` — React/ReactDOM/Redux Toolkit/react-redux/TanStack Query/Axios/toàn bộ Radix UI bị Rollup gộp chung vào 1 chunk **506.75 kB (166.56 kB gzip)** tải NGAY LẦN ĐẦU ở mọi trang, dù route-level code đã lazy-split đúng. Đây chính là warning thật khi chạy `npm run build`. | Thêm `manualChunks` tách vendor thành nhóm riêng (`react-vendor`, `radix-ui`, `query-redux`) để chunk chặn-render đầu tiên nhỏ lại và cache độc lập với code ứng dụng. | **High** |
| 2 | `client/src/pages/pos/PosPage.tsx` | toàn file (669 dòng) | 0 chỗ dùng `React.memo` trong toàn bộ `client/src`. Ô tìm kiếm/sửa số lượng giỏ hàng làm re-render lại TOÀN BỘ lưới sản phẩm (tối đa 60 item) lẫn danh sách giỏ hàng, dù chỉ 1 phần thay đổi — đây là trang tần suất thao tác cao nhất hệ thống (thu ngân). | Tách `ProductGrid`/`CartLines` thành component riêng bọc `React.memo`. | Medium |
| 3 | `client/src/pages/pos/PosPage.tsx` | 122-145 | `useEffect` đăng ký phím tắt F1/F8/F9 có `eslint-disable` cho `exhaustive-deps`, thiếu `parkMutation.isPending`/`checkoutMutation.isPending` trong dependency — closure có thể dùng trạng thái "đang xử lý" CŨ, khiến F8 (đóng gói đơn) có thể bắn 2 lần khi request đầu chưa xong (F9/thanh toán đã có idempotency key ở server nên an toàn hơn, F8 thì chưa). | Thêm `parkMutation.isPending`/`checkoutMutation.isPending` vào dependency array, hoặc bọc handler bằng `useCallback` phụ thuộc đúng state. | Medium |
| 4 | `client/src/lib/qrImageDecoder.ts` | 1 | `jsQR` import tĩnh ở đầu file, bị kéo vào chunk `SettingsPage` (đã 148.70 kB) dù chỉ dùng khi người dùng tải ảnh QR ngân hàng — 1 tính năng phụ hiếm dùng của 1 route đã lazy. | Đổi sang `const { default: jsQR } = await import("jsqr")` bên trong hàm dùng nó, theo đúng khuôn mẫu `html2canvas`/`jspdf` đã làm đúng trong `InvoiceViewer.tsx`. | Low |
| 5 | `server/src/main/java/com/quanlycuahang/erp/sales/service/OrderService.java` | 336-405 (`cancelOrder`) | Tính năng "hủy đơn hàng đã hoàn tất" (mới khôi phục, commit `6d407a2`) lặp qua từng dòng đơn hàng và gọi `inventoryRepository.findByProductIdAndBranchId` **riêng lẻ từng dòng** — N+1 SELECT. Không nhất quán với `createOrder()` (đã preload hàng loạt qua `findByBranchIdAndProductIdIn`). | Preload toàn bộ `Inventory` liên quan bằng 1 câu `findByBranchIdAndProductIdIn` trước vòng lặp, giữ nguyên `saveAndFlush` từng dòng (cần cho `@Version` optimistic lock). | Medium |
| 6 | `server/src/main/resources/db/migration/` (mới) | — | Thiếu index `debts(tenant_id, reference_type, reference_id)` — bị quét full scan (lọc theo tenant_id) bởi `OrderService.cancelOrder()`, `PurchaseOrderService.updateItemPrice()`, và job đối soát `ReconciliationService.checkOrphanedReferences()`. | Migration mới: `CREATE INDEX idx_debts_reference ON debts (tenant_id, reference_type, reference_id);` | Medium |
| 7 | `server/src/main/resources/db/migration/` (mới) | — | Thiếu index tương tự cho `inventory_transactions(reference_type, reference_id)` — mức độ thấp hơn vì chỉ dùng trong job đối soát định kỳ, không phải mỗi request. | `CREATE INDEX idx_inventory_tx_reference ON inventory_transactions (tenant_id, reference_type, reference_id);` khi quy mô lớn hơn. | Low |
| 8 | `server/src/main/resources/db/migration/` (mới) | — | Thiếu composite index `purchase_orders(tenant_id, branch_id, created_at DESC)` — trang danh sách phiếu nhập (`PurchaseOrderService.java:210`) chỉ có index đơn cột, phải sort riêng khi dữ liệu lớn dần. | `CREATE INDEX idx_purchase_orders_tenant_branch_created ON purchase_orders (tenant_id, branch_id, created_at DESC);` (tương tự cho `stock_takes`, mức độ thấp hơn vì tần suất thấp hơn). | Medium (purchase_orders), Low (stock_takes) |
| 9 | `client/package.json` | `vite`/`vitest` | `npm audit`: 5 lỗ hổng (3 moderate, 1 high, 1 critical) — TOÀN BỘ nằm trong công cụ build/test (`vite@5.4.21`, `esbuild`, `vitest@2.1.9`), KHÔNG nằm trong code chạy thật ở `dist/`. 1 lỗi trong đó đặc thù Windows dev server (path traversal/NTLM). | Nâng cấp `vite` 5→8, `vitest` 2→4 trong 1 PR riêng (breaking, cần chạy lại toàn bộ test sau khi nâng) — không khẩn cấp cho production vì chỉ ảnh hưởng máy dev. | Medium |

**Đã xác nhận KHÔNG có vấn đề (không phải bỏ sót, đã kiểm tra thật):** `html2canvas`/`jspdf` đã lazy-import đúng cách trong `InvoiceViewer.tsx`; toàn bộ danh sách lớn (POS, DataTable) đều phân trang server-side ≤60 dòng/trang nên không cần virtualization; không có ảnh/asset nặng không tối ưu; các Repository lớn (`StockTakeItemRepository`, `PurchaseOrderItemRepository`, `InventoryRepository`) đã dùng `JOIN FETCH` đúng từ Prompt #7, không còn N+1 ở đó.

---

## 6. Database issues

| # | Bảng/File | Chi tiết | Nguyên nhân | Cách sửa | Mức độ |
|---|---|---|---|---|---|
| 1 | Toàn bộ 32 bảng nghiệp vụ | Không có ràng buộc DB-level đảm bảo FK cùng tenant (vd `orders.customer_id` về lý thuyết trỏ được sang customer tenant khác nếu tầng ứng dụng có lỗ hổng) | Multi-tenant được thêm sau vào schema đơn-PK có sẵn (`V13`); composite FK `(tenant_id, id)` giờ cải tạo lại rất tốn kém | Thêm bước kiểm tra `CROSS_TENANT_REFERENCE` vào `ReconciliationService` đã có sẵn (job đối soát định kỳ), theo đúng khuôn mẫu `ORPHANED_REFERENCE` hiện có — chi phí thấp, không cần sửa schema | **High** (theo lịch sử dự án từng có 2 lỗi cách ly tenant nghiêm trọng) nhưng rẻ để vá |
| 2 | `orders`, `order_items`, `debts`, `debt_payments`, `cash_transactions`, `order_payments`, `purchase_order_items`, `purchase_orders`, `returns`, `return_items`, `inventory` | Không có CHECK constraint nào chặn số tiền âm (`amount`, `total_amount`, `unit_price`, `cost_price`...) — chỉ có `quantity > 0` và 1 trigger riêng cho `inventory.stock`. Đợt bổ sung CHECK constraint trước đó (`V24`) chỉ phủ cột dạng enum/trạng thái, bỏ sót nhóm "số tiền không âm". | Migration mới, dùng `NOT VALID` + `VALIDATE CONSTRAINT` riêng để tránh khóa bảng lâu (sau khi xác nhận không có dòng nào vi phạm — theo đúng tiền lệ V24 đã làm) | **High** cho `orders`/`order_items`/`debts` (dữ liệu tài chính lõi), Medium cho các bảng còn lại |
| 3 | `orders.idx_orders_tenant_id`, `audit_logs.idx_audit_logs_tenant_id`, `orders.idx_orders_created_branch_status` | Index đơn cột dư thừa, đã bị bao phủ hoàn toàn bởi index composite mới hơn (`idx_orders_tenant_created_branch_status` từ V19) | Sót lại khi thêm composite index sau này, không dọn index cũ | Migration mới: `DROP INDEX idx_orders_tenant_id; DROP INDEX idx_audit_logs_tenant_id; DROP INDEX idx_orders_created_branch_status;` | Low (chỉ tốn overhead ghi/dung lượng) |
| 4 | `shifts` | Thiếu composite index `(tenant_id, opened_by, status)` cho truy vấn nóng `findFirstByOpenedByIdAndStatusOrderByOpenedAtDesc` (mỗi lần thu ngân mở/xem ca) | Đợt bổ sung composite index trước chỉ phủ `orders`/`audit_logs`/`debts` | `CREATE INDEX idx_shifts_tenant_opened_by_status ON shifts (tenant_id, opened_by, status);` | Low (số dòng/thu ngân nhỏ tự nhiên) |
| 5 | `server/.../sales/repository/OrderRepository.java` | `findByBranchIdOrderByCreatedAtDesc` — dead code, không nơi nào gọi (đã thay bằng query `search()` gộp bộ lọc) | Sót lại từ trước khi có `search()` | Xóa method Java (không phải migration) | Low |
| 6 | Toàn bộ migration (V1→V35) | Chưa migration nào dùng `CREATE INDEX CONCURRENTLY` — an toàn với dữ liệu hiện tại (bảng nhỏ lúc tạo), nhưng Flyway chạy transactional mặc định nên KHÔNG THỂ dùng `CONCURRENTLY` nếu không cấu hình `executeInTransaction=false`. Rủi ro thật cho các bảng tăng trưởng nhanh nhất (`orders`/`order_items`/`inventory_transactions`/`audit_logs`) khi cần thêm index mới trong tương lai — sẽ khóa ghi (SHARE lock) toàn bảng trong lúc build index. | Chưa cấu hình sẵn khả năng `executeInTransaction=false` cho Flyway | Khi thêm index mới lên các bảng lớn ở tương lai: đánh dấu migration đó `-- flyway:executeInTransaction=false` + dùng `CREATE INDEX CONCURRENTLY`, hoặc lên lịch bảo trì | Medium (quy trình cho tương lai, không phải bug ở migration hiện có) |

**Đã xác nhận KHÔNG có vấn đề:** `git log --diff-filter=M` trên thư mục migration trả về rỗng — chưa từng có migration đã áp dụng bị sửa lại (kỷ luật append-only giữ nguyên). Không có `SELECT *` sai chỗ (1 chỗ duy nhất tìm được là native query map thẳng vào entity, hợp lệ). Toàn bộ cột khóa ngoại đều có index. Backup/restore (`scripts/backup.sh`/`restore.sh`/`restore-test.sh`) đã xác nhận dùng `pg_dump -Fc` đúng chuẩn, restore-test THẬT SỰ dựng container Postgres tạm để kiểm chứng (không chỉ mô tả suông), ghi log PASS/FAIL.

---

## 7. Docker issues

| # | File | Dòng | Nguyên nhân | Cách sửa | Mức độ |
|---|---|---|---|---|---|
| 1 | `.github/workflows/ci.yml` | 3-6 | `on.push` chỉ khai báo `branches: [main, master]`, **thiếu `tags:`**. GitHub Actions không bắn sự kiện `push` cho tag nếu filter chỉ có `branches` — nghĩa là `git push origin v1.2.0` **KHÔNG BAO GIỜ chạy workflow này**, dù job `docker-images` (dòng 244-245) đã viết sẵn logic tag semver (`type=semver,pattern={{version}}`). Toàn bộ đường build+push image theo tag semver hiện là dead code. | Thêm `tags: ['v*.*.*']` cạnh `branches: [main, master]` ở `on.push`. | Medium |
| 2 | `server/Dockerfile` | 28 | `ENTRYPOINT ["java", "-jar", "app.jar"]` không set JVM heap tường minh. JDK 21 có container-aware ergonomics mặc định (`MaxRAMPercentage=25%`), nhưng với `mem_limit: 1g` ở `docker-compose.prod.yml`, heap chỉ ~256MB cho cả Hikari pool + Hibernate + Jackson — hơi chật cho 1 container dành riêng 1GB không chia sẻ. | Thêm `-XX:MaxRAMPercentage=75.0` (hoặc `-Xmx700m`) vào `ENTRYPOINT`/`JAVA_TOOL_OPTIONS`. | Medium |
| 3 | `docker/docker-compose.prod.yml` | khối `ml-service` | Không có `mem_limit`/`deploy.resources` cho `ml-service` (Python/scikit-learn) — mọi service khác trong overlay prod đều có giới hạn, kể cả `ollama` (đã giới hạn 4GB rõ ràng vì lo ngại tương tự). 1 lỗi rò rỉ bộ nhớ ở service phụ này có thể chiếm hết RAM của VPS nhỏ, ảnh hưởng cả `postgres`/`server`. | Thêm `deploy.resources.limits.memory: 512m` cho `ml-service`, theo đúng khuôn mẫu `ollama`. | Medium |
| 4 | `docker/docker-compose.observability.yml` | 15-39 | `prometheus`/`grafana` không có giới hạn tài nguyên nào — mức ưu tiên thấp hơn vì overlay này tùy chọn, không bật mặc định. | Thêm `deploy.resources.limits.memory` cho cả 2 (vd `1g`/`512m`). | Low |
| 5 | `docker/docker-compose.observability.yml` | 28 | (Trùng với mục Security #1) `GF_SECURITY_ADMIN_PASSWORD` mặc định `admin` nếu thiếu biến môi trường — hiện đang thiếu thật trong `.env`. | Đặt `GRAFANA_ADMIN_PASSWORD` trong `.env` trước khi bật overlay. | Low |
| 6 | `ml-service/Dockerfile` | 1 | Single-stage build (không multi-stage) — không nghiêm trọng vì không có bước biên dịch nặng, nhưng có thể gọn hơn. | Tùy chọn: multi-stage với builder stage `pip install --user` rồi copy `site-packages`. | Low |

**Đã xác nhận KHÔNG có vấn đề:** Cả `server/Dockerfile` và `client/Dockerfile` đều multi-stage đúng chuẩn (build stage tách biệt runtime slim: JRE-alpine cho server, nginx-alpine cho client), non-root user (`erp`), `HEALTHCHECK` đầy đủ, `.dockerignore` chặn copy `node_modules`/`target`/`.git`. Toàn bộ service DB/Redis/Ollama/ml-service **không publish port ra host** (chỉ mạng nội bộ Docker). Named volume cho dữ liệu bền vững (`postgres-data`, `redis-data`...). `restart: unless-stopped`/`always` đầy đủ. `depends_on: condition: service_healthy` đúng chỗ cần. `x-logging` (10MB × 5 file) áp dụng cho mọi service, tránh log đầy ổ đĩa. Nginx: gzip bật, security header đầy đủ (`X-Frame-Options`, `X-Content-Type-Options`, CSP `frame-ancestors 'none'`), `client_max_body_size 5m` đủ dư so với giới hạn upload 2MB của backend, không có TLS trong file này — **có chủ đích**, giả định TLS terminate ở reverse proxy bên ngoài (đã ghi rõ trong comment + `README_DEPLOY.md`). `.env` không bị commit, không có file build/log/dump nào lọt vào git tracking, không có file quá khổ bất thường trong repo.

---

## 8. Code quality issues

| # | File | Dòng | Nguyên nhân | Cách sửa | Mức độ |
|---|---|---|---|---|---|
| 1 | `server/.../inventory/controller/InventoryController.java` | 92, 103-118 | Logic nghiệp vụ lọt vào Controller: tính giá trị tồn kho + nhãn trạng thái "Cận hạn/Dưới định mức/Đủ hàng" viết thẳng trong `/export` — **trùng lặp độc lập** với đúng công thức đã có ở `client/src/pages/inventory/InventoryPage.tsx:59-66` (`statusOf()`). Nếu sau này đổi ngưỡng 7 ngày hay đổi nhãn ở 1 chỗ mà quên chỗ kia, file Excel xuất ra sẽ lệch với màn hình. | Chuyển phần tính toán này vào `InventoryService`/`InventoryMapper`, trả sẵn field trong `InventoryResponse`, dùng chung cho cả endpoint hiển thị lẫn `/export`. | Medium |
| 2 | `server/.../sales/controller/OrderController.java` | 41-47, 140-145 | Tương tự: `STATUS_LABELS` (dịch trạng thái đơn) và `paymentLabel()` (thứ tự ưu tiên hiển thị thanh toán) là quyết định nghiệp vụ/hiển thị nằm thẳng trong Controller, trùng với `client/src/lib/orderStatus.ts`. | Chuyển vào `OrderService`/1 mapper riêng để dùng chung cho export và bất kỳ nơi nào khác sau này. | Low |
| 3 | `InventoryController.java`, `ReportController.java`, `OrderController.java` | `InventoryController.java:95-101`, `ReportController.java:259-266`, `OrderController.java:129-134` | 3 Controller lặp lại y hệt đoạn code bọc `byte[]` thành `ResponseEntity` file .xlsx (content-type + header `Content-Disposition`) — `ReportController` còn có sẵn hàm `excelFile()` riêng nhưng 2 Controller kia không tái sử dụng. | Thêm 1 hàm `ReportExcelExporter.toXlsxResponse(byte[], filename)` dùng chung cho cả 3. | Low |
| 4 | `InventoryController.java`, `OrderController.java` | `InventoryController.java:72`, `OrderController.java:103` | Endpoint `/export` cố định `PageRequest.of(0, 10_000)` — nếu dữ liệu thật vượt 10.000 dòng, file xuất ra bị cắt ngang mà không có cảnh báo nào cho người dùng. | Kiểm tra `page.getTotalElements() > 10_000` và báo lỗi/cảnh báo rõ ràng, hoặc nới giới hạn (RAM không phải vấn đề vì đã dùng `SXSSFWorkbook` streaming). | Low |
| 5 | `client/src/pages/settings/SettingsPage.tsx` | 67-544 (477 dòng) | 1 component ôm 4 mảng nghiệp vụ không liên quan (bán hàng/tiền tệ, hóa đơn/thanh toán, cửa hàng/chi nhánh, bảo mật) qua 1 state `tab` + 1 object `form` phẳng. | Tách `SalesSettingsTab`/`InvoiceSettingsTab`/`StoreSettingsTab`/`SecuritySettingsTab`, mỗi tab tự quản lý phần state của mình. | Medium |
| 6 | `client/src/pages/pos/PosPage.tsx` | 1-669 | Toàn bộ logic giỏ hàng (`addProduct`/`updatePrice`/`updateQuantity`/`removeLine`/`resetCart`/`lineDiscount`), 5 `useMutation`, phím tắt, và JSX cho cả lưới sản phẩm lẫn panel giỏ hàng dồn vào 1 file. | Tách `useCart()` hook (state + hàm sửa giỏ hàng) và tách `ProductGrid`/`CartPanel` thành component riêng (cũng giải quyết luôn vấn đề re-render ở mục Performance #2). | Medium |
| 7 | `client/src/lib/http/apiClient.ts` | 9 | `baseURL: "/api/v1"` — đường dẫn tương đối cố định, không đọc từ biến môi trường nào (không có `VITE_API_URL` ở đâu trong `client/src`). Đây là khác biệt so với giả định ban đầu trong yêu cầu audit (đề bài kỳ vọng `VITE_API_URL`), nhưng **không phải lỗi**: kiến trúc thật của dự án là frontend + backend cùng origin qua Nginx reverse proxy (xem `docker/nginx.conf`), nên không cần biến URL riêng và tránh được toàn bộ vấn đề CORS. | Không bắt buộc sửa nếu giữ mô hình same-origin qua Nginx. Nếu tương lai muốn tách domain API riêng (multi-region, CDN riêng...), thêm `baseURL: import.meta.env.VITE_API_URL ?? "/api/v1"`. | Low (ghi nhận khác biệt kiến trúc, không phải bug) |
| 8 | `client/package.json` | `eslint: ^8.57.0` | ESLint 8 đã vào giai đoạn bảo trì (maintenance mode), ESLint 9 (flat config) là chuẩn hiện tại. | Nâng cấp không khẩn cấp — cần chuyển `.eslintrc` sang flat config, kiểm tra lại tương thích `eslint-plugin-react-hooks`/`eslint-plugin-react-refresh`. | Low |

**Đã xác nhận KHÔNG có vấn đề:** Không có component trùng lặp hay không dùng tới (đã dọn ở Prompt #10). Không có prop-drilling quá 2-3 cấp. Toàn bộ `useEffect` khác đều có cleanup đúng (`removeEventListener`/abort), các chỗ tắt `exhaustive-deps` đều có comment giải thích lý do chính đáng. Routing: 100% route riêng tư đều bọc `RequireAuth` + `RequirePermission` theo từng quyền cụ thể (không phải role thô), route Super Admin (`/platform-admin`) có cơ chế gate riêng độc lập nhưng đúng đắn. Exception handling backend: `GlobalExceptionHandler` trả về format nhất quán, không lộ stack trace/exception message nội bộ ra response.

---

## 9. Files cần xóa

**Không tìm thấy file/dependency nào cần xóa trong đợt audit này** — dự án đã có 1 vòng dọn dead-code chuyên biệt (Prompt #10, `docs/PROJECT_STATE.md` mục tương ứng): xóa bảng `promotions` mồ côi, 1 component UI không dùng, 2 endpoint không FE nào gọi, và đã xác nhận lại qua agent lần này — không phát sinh thêm.

Duy nhất 1 mục code (không phải file) nên xóa:
- `server/src/main/java/com/quanlycuahang/erp/sales/repository/OrderRepository.java` — method `findByBranchIdOrderByCreatedAtDesc` không còn nơi nào gọi (đã thay bằng `search()`). Xóa method này, không xóa cả file.

**Đã xác nhận sạch (không cần dọn):** không có `node_modules`/`target`/`dist`/`.idea`/`.vscode` bị lỡ commit vào git; không có file `.dump`/log database nào bị commit (các bản backup thật trong `backups/` đã đúng chuẩn nằm ngoài git nhờ `.gitignore`); không có dependency npm nào khai báo trong `package.json` mà không dùng tới; file lớn nhất trong git là tài liệu PDF hệ thống (1.3MB, hợp lý).

---

## 10. Files cần sửa

Tổng hợp lại tất cả file có phát hiện cụ thể (chi tiết đầy đủ ở mục 4-8), xếp theo mức ưu tiên:

**Ưu tiên cao (High/Medium — nên làm trước khi/ngay sau deploy):**
1. [client/vite.config.ts](client/vite.config.ts) — thêm `manualChunks`
2. [server/.../sales/service/OrderService.java](server/src/main/java/com/quanlycuahang/erp/sales/service/OrderService.java) — sửa N+1 ở `cancelOrder()`
3. [server/.../inventory/controller/InventoryController.java](server/src/main/java/com/quanlycuahang/erp/inventory/controller/InventoryController.java) — chuyển logic tính trạng thái tồn kho ra Service
4. [server/src/main/resources/db/migration/](server/src/main/resources/db/migration/) — thêm migration mới: index `debts`/`purchase_orders`, CHECK constraint số tiền không âm
5. [.github/workflows/ci.yml](.github/workflows/ci.yml) — thêm `tags:` filter
6. [server/Dockerfile](server/Dockerfile) — thêm `MaxRAMPercentage`
7. [docker/docker-compose.prod.yml](docker/docker-compose.prod.yml) — thêm giới hạn RAM cho `ml-service`
8. [client/src/pages/pos/PosPage.tsx](client/src/pages/pos/PosPage.tsx) — sửa dependency `useEffect` phím tắt + tách component + `React.memo`

**Ưu tiên thấp (Low — làm khi rảnh, không chặn deploy):**
9. [client/src/pages/settings/SettingsPage.tsx](client/src/pages/settings/SettingsPage.tsx) — tách theo tab
10. [client/src/lib/qrImageDecoder.ts](client/src/lib/qrImageDecoder.ts) — lazy import `jsQR`
11. [server/.../sales/controller/OrderController.java](server/src/main/java/com/quanlycuahang/erp/sales/controller/OrderController.java) — chuyển `STATUS_LABELS`/`paymentLabel` ra Service
12. [.env](.env) — bổ sung `GRAFANA_ADMIN_PASSWORD` nếu định bật observability overlay
13. `docker/docker-compose.observability.yml` — thêm giới hạn RAM cho Prometheus/Grafana
14. `client/package.json` — kế hoạch nâng cấp `vite`/`vitest`/`eslint` (không khẩn cấp)

---

## 11. Code cần refactor

Không có refactor kiến trúc lớn nào bắt buộc trước deploy — codebase đã qua 1 vòng refactor god-class (`OrderService`) và đang ở trạng thái tốt. 2 refactor đáng làm (không khẩn cấp, cải thiện khả năng bảo trì):

1. **`PosPage.tsx` (669 dòng)** → tách `useCart()` hook + `ProductGrid`/`CartPanel` component riêng, bọc `React.memo`. Giải quyết đồng thời vấn đề performance (mục 5) lẫn khả năng bảo trì (mục 8).
2. **`SettingsPage.tsx` (477 dòng)** → tách theo tab thành 4 component riêng, mỗi component tự quản lý state của phần mình thay vì 1 object `form` phẳng dùng chung.
3. **Logic tính toán/nhãn trùng lặp giữa FE và BE** (trạng thái tồn kho, trạng thái đơn hàng, nhãn thanh toán) → gom về 1 nguồn duy nhất ở Service/Mapper backend, FE và file Excel xuất ra cùng đọc từ 1 field DTO thay vì tự tính lại — tránh lệch dữ liệu khi sửa 1 chỗ quên chỗ kia (đã xảy ra đúng kiểu lỗi này ở phiên làm việc vừa thêm tính năng xuất Excel).

---

## 12. Deployment instructions

Dự án đã có `README_DEPLOY.md` (16KB, hướng dẫn 12 bước + rollback + backup) khá đầy đủ từ trước — dưới đây là tóm tắt quy trình + các bước bổ sung riêng cho lần deploy này (sau khi áp dụng các fix ở mục 10, nếu chọn làm trước khi deploy):

1. **Backup trước khi deploy** (bắt buộc, đặc biệt nếu deploy có kèm migration DB):
   ```
   ./scripts/backup.sh
   ```
2. **Kiểm tra `.env` production** khớp đủ key với `.env.example` (mục 7 audit này đã xác nhận khớp, chỉ thiếu vài key optional có fallback an toàn — xem mục 4/7).
3. **Build & test toàn bộ trước khi build image:**
   ```
   cd client && npm run lint && npx tsc --noEmit && npm run build
   cd ../server && mvn clean verify   # bao gồm unit + integration test + JaCoCo coverage gate + Spotless
   ```
4. **Build Docker image:**
   ```
   cd docker && docker compose build server web
   ```
5. **Deploy:**
   ```
   docker compose up -d
   ```
6. **Xác nhận Flyway migrate sạch** (nếu có migration mới từ mục 10 — vd index/CHECK constraint):
   ```
   docker exec docker-postgres-1 psql -U <user> -d <db> -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"
   ```
7. **Health check tất cả container:**
   ```
   docker compose ps
   docker logs docker-server-1 --tail 100
   curl -f http://localhost:8080/actuator/health   # (nội bộ, KHÔNG public qua Nginx theo cấu hình hiện tại)
   ```
8. **Restore-test định kỳ** (đã có sẵn, nên chạy lại sau backup mới nhất):
   ```
   ./scripts/restore-test.sh
   ```
9. Nếu migration mới thuộc loại rủi ro (CHECK constraint trên bảng có dữ liệu thật) — làm theo đúng khuôn mẫu `V24` đã dùng: `NOT VALID` trước, `VALIDATE CONSTRAINT` riêng sau khi xác nhận không có dòng vi phạm, tránh khóa bảng lâu trong giờ cao điểm.

---

## 13. Final production checklist

**DATABASE:**
- [x] Cơ chế backup đã có và đã test restore thật (`scripts/backup.sh` + `restore-test.sh`)
- [ ] Backup mới nhất trước lần deploy này
- [x] Migration V1→V35 sạch, append-only, không có migration nào bị sửa sau khi áp dụng
- [ ] (Tùy chọn, không chặn deploy) Áp dụng migration mới: index `debts`/`purchase_orders`, CHECK constraint số tiền không âm (mục 6)
- [x] Connection pool (Hikari) cấu hình qua env, không hardcode

**BACKEND:**
- [x] `mvn clean verify` xanh (unit + integration test qua Testcontainers + JaCoCo coverage gate + Spotless) — *xác nhận qua `docs/PROJECT_STATE.md`, môi trường audit cục bộ hiện tại gặp lỗi hạ tầng Docker Desktop/Testcontainers không liên quan đến code (xem ghi chú cuối phần này)*
- [x] Security: JWT/CORS/CSRF/XSS/rate-limit/upload đều đã audit và xác nhận OK (mục 4)
- [x] Không secret nào bị commit vào git
- [ ] (Tùy chọn) Sửa N+1 ở `OrderService.cancelOrder()` trước khi deploy nếu tenant có nhiều đơn hàng lớn

**FRONTEND:**
- [x] `npm run lint` + `npx tsc --noEmit` + `npm run build` xanh
- [x] Không hardcode `localhost`/URL backend — dùng same-origin qua Nginx reverse proxy
- [ ] (Tùy chọn) Thêm `manualChunks` để giảm kích thước bundle chặn-render đầu tiên (mục 5, High)

**DOCKER:**
- [x] `docker compose build` thành công cho `server` + `web`
- [x] Container khởi động healthy (`docker compose ps`)
- [x] Không có port DB/Redis publish ra ngoài host
- [ ] (Tùy chọn) Thêm giới hạn RAM cho `ml-service` trong overlay prod

**SERVER:**
- [ ] Firewall: chỉ mở 80/443 (Nginx) ra ngoài, mọi service khác chỉ mạng nội bộ Docker (đã đúng theo cấu hình hiện tại — cần xác nhận ở tầng hạ tầng thật ngoài phạm vi source code)
- [ ] SSL/HTTPS: cấu hình ở reverse proxy bên ngoài container Nginx này (mô hình đã chọn — xem `README_DEPLOY.md`)
- [ ] Domain: cập nhật `CORS_ALLOWED_ORIGINS` trong `.env` production thành domain thật (không phải localhost)

---

### Ghi chú về giới hạn của audit này

- Agent Security/Docker/Database không có quyền truy cập mạng để tra cứu CVE mới nhất theo thời gian thực — mục "Spring Boot 3.3.5"/"vite 5→8" nên được xác nhận lại bằng công cụ tra CVE thật (`mvn versions:display-dependency-updates`, `npm audit`) ngay trước ngày deploy thật, không chỉ dựa vào audit hôm nay.
- Trong lúc audit, tôi thử chạy `mvn clean verify` đầy đủ (gồm integration test qua Testcontainers) trên máy audit hiện tại và gặp lỗi hạ tầng cục bộ (`NpipeSocketClientProviderStrategy: BadRequestException` — Docker Desktop/Testcontainers không tương thích npipe trên máy này tại thời điểm audit) — **không liên quan đến chất lượng code**, đã xác nhận riêng bằng: `mvn test` (chỉ unit, không cần Docker) xanh, `docker compose build` cho cả 2 image thành công (chứng minh code compile/package đúng), và theo `docs/PROJECT_STATE.md` lần chạy gần nhất (2026-07-13) `mvn verify` đầy đủ đã xanh 100%. Cần chạy lại `mvn clean verify` đầy đủ trên máy CI/deploy thật (không phải máy audit này) trước khi tin tưởng hoàn toàn vào coverage gate.
