# Phase 6 — Backend Foundation

## 6.1 Bootstrap
- `SecurityConfig`: filter chain STATELESS, CORS whitelist qua `app.cors.allowed-origins`,
  CSRF tắt (giải thích lý do trong Javadoc class) vì API JSON dùng Bearer token trong
  header, không dựa vào cookie session tự động đính kèm.
- `JwtAuthenticationEntryPoint`/`JwtAccessDeniedHandler`: trả đúng format D2 (`ApiResponse.error`)
  ngay ở tầng filter chain, trước khi vào `DispatcherServlet` — `GlobalExceptionHandler`
  (Phase 2) chỉ bắt được exception ném ra trong Controller/AOP proxy, không bắt được lỗi
  xảy ra ở filter chain.
- `ddl-auto: validate` (đã cấu hình Phase 0) — không đổi.

## 6.2 Auth hoàn chỉnh

**JWT**: `JwtService` dùng jjwt 0.12.6 (API xác nhận thật qua `javap`, không suy đoán từ
tài liệu cũ 0.11.x). Access token 15 phút chứa claim `authorities` (danh sách
`resource:action`) — không cần query lại DB mỗi request. Refresh token 7 ngày chứa claim
`jti` (id token) + `tokenFamily` (ổn định qua các lần rotate).

**Rotation + reuse detection**: `RefreshTokenService` lưu Redis key
`refresh:family:{tokenFamily}` → `jti` hợp lệ hiện tại (TTL = thời hạn refresh token).
Mỗi lần refresh: so `jti` trong token với giá trị Redis — khớp thì rotate (sinh cặp token
mới, cùng family, cập nhật Redis); lệch thì coi là tái sử dụng token cũ (bị đánh cắp),
thu hồi toàn bộ family (xóa key Redis) → mọi refresh token cùng family (kể cả bản mới
nhất) đều bị từ chối, buộc đăng nhập lại.

**Rate limit**: `RateLimitService` dùng Bucket4j 8.10.1 + `LettuceBasedProxyManager`
(API xác nhận thật qua `javap` — groupId `com.bucket4j` nhưng package Java là
`io.github.bucket4j`, dễ nhầm nếu đoán). Đăng nhập giới hạn 5 lần/15 phút/IP, tính cả
lần thành công lẫn thất bại (đúng ngữ nghĩa "5 lần đăng nhập", không chỉ đếm thất bại).

**Quyền**: `CustomUserDetailsService` nạp `authorities` = hợp của mọi permission từ mọi
role được gán (Set, không trùng lặp). `ResourceActionPermissionEvaluator` hỗ trợ
`hasPermission(id, 'resource', 'action')` cho trường hợp cần gắn quyền với 1 đối tượng cụ
thể; cách dùng chính vẫn là `hasAuthority('resource:action')` trực tiếp như D3 mô tả.

## 6.3 Validation, Upload, Audit, Settings

- `@ValidPhoneVN`: regex số di động VN sau chuẩn hóa đầu số 2018.
- `FileStorageService`/`FileUploadController`: whitelist MIME (jpg/png/webp), giới hạn
  2MB (`spring.servlet.multipart.max-file-size`), đổi tên UUID (chống đoán tên file/path
  traversal), lưu ngoài webroot (`app.upload.dir`), serve qua endpoint riêng
  (`GET /api/v1/uploads/{fileName}` — permitAll vì `<img>` tag không gửi kèm header
  Authorization), header `X-Content-Type-Options: nosniff` chống thực thi nhầm.
- `AuditAspect` (`@Around("@annotation(Audited)")`): bọc method Service đánh dấu
  `@Audited(action = "...")`, ghi `before` (tham số đầu vào) / `after` (kết quả trả về)
  dạng JSON vào `audit_logs`, tự lấy user hiện tại từ `SecurityContext`. Lỗi ghi audit
  không làm hỏng nghiệp vụ chính (bắt exception, chỉ log warn).
- `SettingsService`: đọc `settings` ưu tiên theo chi nhánh, fallback global
  (`branch_id IS NULL`), cache Redis TTL 10 phút (kể cả cache "không tồn tại" qua
  `NULL_MARKER` để tránh cache stampede khi key chưa từng seed).

## Đã verify bằng ứng dụng chạy thật (không chỉ compile)

Chạy `mvn spring-boot:run` với PostgreSQL 16 + Redis 7 local, test qua `curl`:

| Kịch bản | Kết quả |
|---|---|
| Đăng nhập đúng (`owner01`/`Password@123`) | 200, access token chứa đủ 51 quyền |
| Refresh token hợp lệ | 200, access + refresh token mới (rotate) |
| Dùng lại refresh token **cũ** (đã rotate) | 401 `AUTH_TOKEN_REUSE_DETECTED`, gia đình token bị thu hồi |
| Dùng refresh token **mới** sau khi gia đình đã bị thu hồi | 401 `AUTH_INVALID_REFRESH_TOKEN` (đúng — thu hồi cả chuỗi) |
| Sai mật khẩu | 401 `AUTH_INVALID_CREDENTIALS` |
| 6 lần đăng nhập liên tiếp cùng IP | 3 lần đầu qua, từ lần thứ 4 → 429 `AUTH_RATE_LIMIT_EXCEEDED` (đúng công thức: tổng 5 lượt đã dùng trước đó + lượt hiện tại) |
| Truy cập endpoint yêu cầu đăng nhập, không có token | 401 `AUTH_UNAUTHENTICATED` đúng format D2 |
| Upload file `.txt` giả `image/png` | 422 `UPLOAD_INVALID_MIME_TYPE` |
| Upload `.png` hợp lệ | 200, tên file đổi thành UUID |
| Serve lại ảnh vừa upload qua GET | 200, đúng `Content-Type: image/png`, không cần token |

## Lệnh kiểm tra

```bash
cd server
mvn clean install       # compile + Spotless + package
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
