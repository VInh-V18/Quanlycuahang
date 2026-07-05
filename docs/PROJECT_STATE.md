## PROJECT_STATE — sau Phase 6 — 2026-07-05

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
│       │   │   ├── sales/entity/         # ParkedOrder, Order, OrderItem, OrderPayment, Return, ReturnItem
│       │   │   ├── promotion/entity/     # Promotion, Voucher, VoucherUsage
│       │   │   └── operation/entity/     # Shift, CashTransaction, InvoiceTemplate, Invoice
│       │   └── resources/
│       │       ├── application*.yml, logback-spring.xml
│       │       └── db/migration/
│       │           ├── V1__init_schema.sql   (37 bang, trigger, index, extension)
│       │           └── V2__seed_data.sql     (1 CN, 6 role, 51 quyen, 3 user, 5 danh muc, 30 SP, 5 KH, 3 NCC, 20 don)
│       └── test/java/com/quanlycuahang/erp/product/ProductRepositoryIT.java
├── docker/                          # rỗng — cấu hình ở Phase 12
├── docs/
│   ├── conventions.md, PROJECT_STATE.md
│   ├── phase1/  (8 file — permission matrix, business specs, diagrams)
│   ├── phase2/architecture.md
│   ├── phase3/erd.md
│   └── phase4/  (design-tokens, layout, wireframes, pos-design, ui-states)
├── scripts/                         # rỗng
├── .env.example, .gitignore, README.md
```

### Database
- Bảng đã có: đủ 37 bảng (xem `docs/phase3/erd.md` mục 4)
- Migration Flyway mới nhất: `V2__seed_data.sql`
- Extension: `unaccent`, `pg_trgm`; function: `immutable_unaccent()`, `fn_check_inventory_stock()` + trigger

### API đã sinh
- Chưa có endpoint nghiệp vụ (Controller) — chỉ Actuator mặc định (`/actuator/health`, `/actuator/info`)

### FE đã sinh
- Chưa có code (thư mục `client/` để trống) — nhưng đã có đầy đủ thiết kế: design tokens + `tailwind.config.ts` sẵn dùng, layout, 15+1 wireframe, chuẩn UI states — khởi tạo code thật ở Phase 5

### Nợ kỹ thuật / dang dở
- Chưa có Dockerfile/docker-compose.yml — Phase 12
- Chưa có CI (GitHub Actions) — Phase 12
- `ProductRepositoryIT` dùng Testcontainers — viết đúng chuẩn nhưng **chưa chạy được trong sandbox này** (không có Docker daemon khả dụng); đã verify tương đương bằng PostgreSQL/Redis cài trực tiếp + `spring-boot:run` thật (xem trên) — cần chạy lại `mvn verify` trên máy/CI có Docker trước khi coi là đã pass CI
- `stock_transfers` (chuyển kho đa chi nhánh, COULD) chưa thiết kế
- Wireframe hiện là mô tả text + Mermaid box diagram (chưa phải hình ảnh/Figma) — đủ chi tiết để code Phase 5 nhưng không có mockup trực quan; có thể bổ sung sau nếu cần

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
