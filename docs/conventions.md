# Quy ước kỹ thuật xuyên suốt dự án

Tài liệu này chép lại chuẩn D1–D5 từ Master Prompt v4.0, áp dụng cho toàn bộ
dự án. Mọi Phase phải nhất quán với các quy tắc dưới đây; thay đổi phải được
ghi chú lại và cập nhật vào tài liệu này.

## D1. Quy ước đặt tên

- **DB (PostgreSQL)**: `snake_case`, tên bảng số nhiều (`products`,
  `order_items`); khóa ngoại `<bảng_số_ít>_id`.
- **Java**: `camelCase` cho field/method, `PascalCase` cho
  class/interface/enum, `UPPER_SNAKE` cho hằng số; package gốc
  `com.quanlycuahang.erp.<module>` (ví dụ `com.quanlycuahang.erp.product`).
- **Entity ↔ cột DB**: field Java `camelCase` map sang cột `snake_case` qua
  `@Column(name = "...")` tường minh — KHÔNG dựa vào naming strategy tự động
  ngầm định, để tránh nhầm lẫn khi đọc migration.
- **TS (Frontend)**: `camelCase` biến/hàm, `PascalCase` component/type,
  `UPPER_SNAKE` hằng số.
- **API**: REST, danh từ số nhiều, kebab-case: `/api/v1/purchase-orders`.
- **Git**: Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`,
  `test:`); nhánh `main` / `develop` / `feature/<tên>` / `fix/<tên>`.

## D2. Chuẩn API

- Prefix: `/api/v1`.
- Response thống nhất, bọc bằng generic `ApiResponse<T>`:

```json
{ "success": true, "data": {}, "meta": { "page": 1, "limit": 20, "total": 154 } }
```

```json
{ "success": false, "error": { "code": "PRODUCT_OUT_OF_STOCK", "message": "Sản phẩm ABC chỉ còn 2 trong kho", "details": {} } }
```

- Mã lỗi nghiệp vụ `UPPER_SNAKE` ổn định, Frontend map code → message tiếng
  Việt; nhóm chuẩn: `AUTH_*`, `VALIDATION_*`, `PRODUCT_*`, `ORDER_*`,
  `INVENTORY_*`, `PAYMENT_*`, `PERMISSION_DENIED`, `NOT_FOUND`, `CONFLICT`,
  `INTERNAL_ERROR`.
- HTTP status:
  - `200/201/204` — thành công
  - `400` — validation (`MethodArgumentNotValidException`)
  - `401` — chưa đăng nhập
  - `403` — thiếu quyền
  - `404` — không tìm thấy
  - `409` — xung đột (oversell → `OptimisticLockException`; trùng mã →
    `DataIntegrityViolationException`)
  - `422` — vi phạm quy tắc nghiệp vụ (`BusinessRuleException`)
  - `429` — rate limit
  - `500` — lỗi hệ thống
- Phân trang chuẩn dùng Spring `Pageable`
  (`?page=0&size=20&sort=createdAt,desc`) — controller nhận `Pageable` trực
  tiếp, không tự parse tay; filter động qua JPA Specification:
  `?search=...&status=completed`.
- Idempotency cho POST tạo đơn: header `Idempotency-Key`, kiểm tra qua
  `HandlerInterceptor` trước khi vào Controller, lưu kết quả Redis TTL 24h.

## D3. Bảo mật (áp dụng mọi Phase)

- Mật khẩu: `BCryptPasswordEncoder` strength 12; tối thiểu 8 ký tự.
- JWT: access token giữ trong memory Frontend (KHÔNG localStorage); refresh
  token trong httpOnly cookie `SameSite=Strict`; ký bằng `jjwt`, rotation +
  phát hiện reuse (so `tokenFamily` lưu Redis) → thu hồi cả chuỗi.
- Spring Security filter chain: `JwtAuthenticationFilter` (đọc header
  `Authorization: Bearer`) → `SecurityContext` →
  `@PreAuthorize("hasAuthority('product:create')")` ở method
  Service/Controller thay cho middleware tự viết.
- Validate: `@Valid` + Jakarta Bean Validation ở MỌI DTO input; Controller
  không nhận Entity trực tiếp, luôn qua DTO.
- Chống: SQLi (JPA parameterized mặc định + `@Query` dùng named param, cấm
  nối chuỗi SQL), XSS (React escape phía Frontend; Backend không render
  HTML), CSRF (SameSite cookie; API JSON tắt CSRF mặc định của Spring Security
  kèm giải thích lý do), IDOR (mọi Repository query kèm điều kiện
  `branchId`/quyền sở hữu, không lấy theo ID trần).
- Rate limit: đăng nhập 5 lần/15 phút/IP (Bucket4j + Redis); API chung 100
  req/phút/user.
- Upload: whitelist MIME (jpg/png/webp), max 2MB
  (`spring.servlet.multipart.max-file-size`), đổi tên UUID, lưu ngoài
  webroot hoặc object storage, serve qua endpoint riêng không thực thi được.
- Audit log: đăng nhập/thất bại, sửa giá, xóa, hoàn tiền, hủy đơn, kết ca,
  đổi phân quyền — Entity `AuditLog` với cột `before`/`after` kiểu `JSONB`,
  ghi qua AOP `@Aspect` bọc quanh method nhạy cảm.
- Soft delete: Hibernate `@SQLDelete(sql = "UPDATE ... SET deleted_at =
  now() WHERE id = ?")` + `@Where(clause = "deleted_at IS NULL")` trên mọi
  Entity nghiệp vụ.

## D4. Hiệu năng

- POS: thêm hàng vào giỏ < 200ms; tìm sản phẩm < 300ms — index GIN cho tìm
  không dấu (unaccent + pg_trgm), unique index `sku`, `barcode`.
- Index bắt buộc: mọi FK; composite `orders(created_at, branch_id,
  status)`; `inventory_transactions(product_id, created_at)`; unique:
  `products.sku`, `products.barcode`, `users.username`.
- Phân trang mọi danh sách bằng `Pageable`; cấm `findAll()` không giới hạn
  trên bảng lớn.
- Tránh N+1: dùng `@EntityGraph` hoặc `JOIN FETCH` trong `@Query` khi load
  quan hệ; bật `spring.jpa.properties.hibernate.default_batch_fetch_size`
  làm lưới an toàn phụ.
- Báo cáo nặng: dùng `@Query(nativeQuery = true)` hoặc JPA Specification
  aggregate, không load hết Entity về JVM rồi tính bằng Stream; nếu vẫn
  chậm → bảng tổng hợp `daily_sales_summary` cập nhật bằng `@Scheduled`.
- `@Transactional` giữ ngắn gọn; không gọi HTTP ngoài hoặc gửi email bên
  trong transaction (tách qua event
  `@TransactionalEventListener(phase = AFTER_COMMIT)`).

## D5. Toàn vẹn dữ liệu

- Bán hàng = 1 `@Transactional`: `Order` + `OrderItem` (snapshot
  `productName`, `unitPrice`, `costPrice`) + trừ kho (`@Version`) +
  `InventoryTransaction` + `Invoice` + công nợ (nếu bán nợ) +
  `OrderPayment`.
- Tiền: `NUMERIC(15,0)` ↔ Java `BigDecimal`, cấm `double`/`float` ở mọi
  phép tính tiền; phần trăm `NUMERIC(5,2)`; số lượng `NUMERIC(12,3)` (hỗ
  trợ bán theo kg).
- Snapshot trên chứng từ: sửa master data không làm sai lịch sử (`OrderItem`
  không có quan hệ tính toán lại từ `Product` khi hiển thị hóa đơn cũ).

## Quyết định bổ sung riêng cho dự án này (Phase 0)

- **Không dùng Lombok** — viết đầy đủ getter/setter/constructor thủ công,
  ưu tiên tường minh và không phụ thuộc annotation processor.
- **Build tool**: Maven.
- **Format code Java**: Spotless (google-java-format), chạy tự động ở phase
  `verify` (`mvn clean install` sẽ fail nếu code chưa format đúng chuẩn —
  chạy `mvn spotless:apply` để tự động format).
- **Package gốc**: `com.quanlycuahang.erp`.
