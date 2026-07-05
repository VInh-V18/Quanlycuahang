## PROJECT_STATE — sau Phase 8 — 2026-07-05

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
├── client/                          # rỗng — khởi tạo ở Phase 5
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
│       │   │   ├── operation/{entity,repository}/    # Shift, CashTransaction, InvoiceTemplate, Invoice
│       │   │   └── common/sequence/NumberSequenceService.java   # SEQUENCE atomic cho order/invoice/sku
│       │   └── resources/
│       │       ├── application*.yml, logback-spring.xml
│       │       └── db/migration/
│       │           ├── V1__init_schema.sql        (37 bang, trigger, index, extension)
│       │           ├── V2__seed_data.sql          (1 CN, 6 role, 51 quyen, 3 user, 5 danh muc, 30 SP, 5 KH, 3 NCC, 20 don)
│       │           └── V3__number_sequences.sql   (order_number_seq, invoice_number_seq, sku_seq)
│       └── test/java/com/quanlycuahang/erp/
│           ├── product/ProductRepositoryIT.java
│           └── sales/pricing/OrderPricingServiceTest.java
├── docker/                          # rỗng — cấu hình ở Phase 12
├── docs/
│   ├── conventions.md, PROJECT_STATE.md
│   ├── phase1/  (8 file — permission matrix, business specs, diagrams)
│   ├── phase2/architecture.md
│   ├── phase3/erd.md
│   ├── phase4/  (design-tokens, layout, wireframes, pos-design, ui-states)
│   ├── phase6/backend-foundation.md
│   ├── phase7/product-inventory-module.md
│   └── phase8/pos-module.md
├── scripts/                         # rỗng
├── .env.example, .gitignore, README.md
```

### Database
- Bảng đã có: đủ 37 bảng (xem `docs/phase3/erd.md` mục 4)
- Migration Flyway mới nhất: `V3__number_sequences.sql`
- Extension: `unaccent`, `pg_trgm`; function: `immutable_unaccent()`, `fn_check_inventory_stock()` + trigger; sequence: `order_number_seq`, `invoice_number_seq`, `sku_seq`

### API đã sinh
- Auth: `POST /api/v1/auth/{login,refresh,logout}`, `POST /api/v1/auth/change-password`
- Sản phẩm/Kho: `/api/v1/products`, `/api/v1/categories`, `/api/v1/inventory`, `/api/v1/purchase-orders`, `/api/v1/stock-takes`
- Khách hàng/Voucher: `/api/v1/customers`
- Bán hàng POS: `POST /api/v1/orders` (header `Idempotency-Key`), `GET /api/v1/orders/{id}`, `POST /api/v1/orders/{id}/cancel`, `/api/v1/parked-orders` (list/park/resume), `POST /api/v1/returns`
- Upload: `POST /api/v1/uploads`, `GET /api/v1/uploads/{fileName}` (permitAll)
- Actuator mặc định (`/actuator/health`, `/actuator/info`)
- **Nợ**: chưa có `BranchController` (CRUD chi nhánh) — hiện chỉ có `BranchRepository`, dùng nội bộ trong `OrderService`/`SettingsService`; cần bổ sung nếu FE cần màn quản lý chi nhánh

### FE đã sinh
- Chưa có code (thư mục `client/` để trống) — nhưng đã có đầy đủ thiết kế: design tokens + `tailwind.config.ts` sẵn dùng, layout, 15+1 wireframe, chuẩn UI states — khởi tạo code thật ở Phase 5

### Nợ kỹ thuật / dang dở
- Chưa có Dockerfile/docker-compose.yml — Phase 12
- Chưa có CI (GitHub Actions) — Phase 12
- `ProductRepositoryIT` dùng Testcontainers — viết đúng chuẩn nhưng **chưa chạy được trong sandbox này** (không có Docker daemon khả dụng); đã verify tương đương bằng PostgreSQL/Redis cài trực tiếp + `spring-boot:run` thật (xem trên) — cần chạy lại `mvn verify` trên máy/CI có Docker trước khi coi là đã pass CI
- `stock_transfers` (chuyển kho đa chi nhánh, COULD) chưa thiết kế
- Wireframe hiện là mô tả text + Mermaid box diagram (chưa phải hình ảnh/Figma) — đủ chi tiết để code Phase 5 nhưng không có mockup trực quan; có thể bổ sung sau nếu cần
- Chưa có `BranchController` (CRUD chi nhánh qua API) — chỉ 1 chi nhánh seed sẵn, đủ cho Phase 8 test nhưng cần bổ sung trước khi FE cần màn quản lý đa chi nhánh
- Chưa seed `vouchers` mẫu trong `V2__seed_data.sql` (test Phase 8 tự thêm 1 voucher tạm qua SQL trực tiếp, không lưu vào migration) — nên bổ sung vào seed data chính thức ở phase sau nếu cần demo

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

### Kế tiếp
- Phase 5: Frontend Foundation (5.1 Vite+TS+Tailwind+shadcn, 5.2 axios+TanStack Query+Redux, 5.3 component nền DataTable/FormField/ConfirmDialog/Money/DateRangePicker/Toast/PermissionGate)
