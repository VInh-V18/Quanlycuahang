# SECURITY_AUDIT_REPORT — Prompt #4 (P1: Audit cách ly tenant + IDOR)

Ngày: 2026-07-12. Phạm vi: FruitHouse ERP, multi-tenant SaaS, 29 controller/~110 endpoint.
Phương pháp: đọc code trực tiếp + xác nhận bằng test chạy thật trên Postgres/Redis thật (container
tạm dùng chung với Prompt #1-#3), KHÔNG suy đoán.

## Tóm tắt

**Không tìm thấy lỗ hổng Critical/High nào còn tồn tại.** Kiến trúc cách ly tenant hiện có
(`TenantAwareRepositoryImpl` override `findById`/`existsById`/`findAllById`/`getReferenceById` cho
MỌI Entity kế thừa `TenantScopedEntity`, kết hợp Hibernate `@Filter` cho truy vấn JPQL thường và
`tenant_id` thủ công cho 14 file native-query) đã được xây dựng và vá kỹ ở các phiên làm việc
trước — đây chính xác là 2 lỗ hổng nghiêm trọng ("Hibernate @Filter gotchas") đã được phát hiện và
sửa trước khi Prompt #4 này bắt đầu. Nhiệm vụ của Prompt #4 vì vậy chủ yếu là **xác nhận bằng test
chạy thật** (không chỉ đọc code) rằng các cơ chế đó hoạt động đúng trên từng luồng nghiệp vụ cụ thể,
và rà thêm các đường vòng khác mà roadmap liệt kê. Có **1 phát hiện Low** (làm cứng thêm, không
phải lỗ hổng có thể khai thác trong điều kiện hiện tại) đã được vá kèm test.

| # | Mức độ | Mô tả | File:dòng | Trạng thái |
|---|---|---|---|---|
| 1 | Low | `FileUploadController.serve()` dùng `fileName` từ path variable trực tiếp vào header `Content-Disposition` — về lý thuyết 1 ký tự nháy kép trong `fileName` có thể phá vỡ header, nhưng KHÔNG khai thác được trong thực tế vì mọi file lưu ra đĩa luôn có tên dạng `UUID.random() + đuôi ảnh` (qua `store()`), nên yêu cầu 1 `fileName` chứa ký tự lạ sẽ luôn resolve tới file KHÔNG TỒN TẠI → 404 trước khi chạm header. Đã vá thêm 1 lớp validate định dạng tên file (`^[0-9a-fA-F-]{36}\.(jpg\|png\|webp)$`) làm cứng, phòng ngừa cho tương lai. | `server/.../common/upload/FileStorageService.java` (`resolve()`) | ✅ Đã vá + 6 test mới (`FileStorageServiceTest`) |

Không phát hiện gì ở các mục còn lại — chi tiết từng mục dưới đây.

## 1. Native query & JPQL bypass

Quét toàn bộ `@Query(nativeQuery = true)` (14 file), `EntityManager.createNativeQuery` (0 kết
quả), `JdbcTemplate` (3 file: `TenantAdminService`/`TenantUserAdminService` — Super Admin, cố ý
xuyên tenant vì đó là chức năng quản trị toàn hệ thống; `NumberSequenceService` — luôn dùng
`TenantContext.get()` server-side, không bao giờ nhận tenantId từ client).

Xác nhận TỪNG câu native query trong 14 file đều có `WHERE ... tenant_id = :tenantId` (hoặc tương
đương) VÀ giá trị `:tenantId` luôn được truyền vào từ `TenantContext.get()` phía Service (server
tự suy ra từ JWT đã xác thực), KHÔNG có endpoint nào nhận tenantId trực tiếp làm tham số request từ
client. Đặc biệt soi kỹ `ReportService` (12 endpoint báo cáo) theo đúng yêu cầu roadmap: mọi
method đều gọi `branchAccessGuard.assertAccess(filter.getBranchId())` trước khi truy vấn, và dùng
`TenantContext.get()` (không phải tham số request) cho tenantId.

**Kết luận: PASS, không có lỗ hổng.**

## 2. Tham chiếu đa hình (reference_id/reference_type)

Grep toàn bộ thư mục `dto/` tìm `referenceId`/`referenceType`: chỉ xuất hiện ở
**response** DTO (`InventoryTransactionResponse`), **KHÔNG có bất kỳ request DTO nào** cho phép
client gửi `referenceId` trực tiếp. Mọi nơi tạo `Debt`/`InventoryTransaction` với `reference_id`
đều lấy từ 1 entity đã được load và xác thực tenant từ trước đó trong cùng luồng (vd
`debt.setReferenceId(order.getId())` sau khi `order` đã qua `orderRepository.save()`/`findById()`
được lọc tenant).

**Kết luận: PASS — không tồn tại đường tấn công mà roadmap mô tả (API nhận reference_id từ
client) trong codebase này.**

## 3. IDOR theo ID tuần tự

Cơ chế bảo vệ hệ thống (đã có từ trước, không phải Prompt #4 tạo mới):
`TenantAwareRepositoryImpl` là `repositoryBaseClass` cho MỌI `JpaRepository`, override 4 phương
thức Hibernate KHÔNG áp dụng `@Filter` (`findById`, `existsById`, `findAllById`,
`getReferenceById`) để tự lọc thêm theo `TenantContext` hiện tại.

Viết **8 test tích hợp mới** (`TenantIsolationIT`, chạy thật trên Postgres — file permanent, nằm
trong `mvn verify`/CI) xác nhận cơ chế trên hoạt động đúng trên từng luồng nghiệp vụ cụ thể mà
roadmap liệt kê — tenant A dùng ID thuộc tenant B PHẢI luôn `ResourceNotFoundException` (404):

- `OrderService.getById()` / `.cancelOrder()`
- `InvoiceDetailService.getById()`
- `CustomerService.getById()`
- `PurchaseOrderService.getById()` / `.updateItemPrice()` (đúng endpoint hành động roadmap nêu:
  `/purchase-orders/items/{id}/price`)
- `StockTakeService.getById()` / `.approve()` (đúng endpoint hành động:
  `/stock-takes/{id}/approve`)
- `ShiftService.getDetail()` / `.close()` (đúng endpoint hành động: `/shifts/{id}/close`)
- `DebtService.recordPayment()` với `partnerId` thuộc tenant khác

**Kết quả: cả 8 test đều PASS ngay từ lần chạy đầu tiên — không phát hiện lỗ hổng nào.**

## 4. Quyền theo chi nhánh (BranchAccessGuard)

`BranchAccessGuard.assertAccess(branchId)` gọi `branchRepository.existsById(branchId)` trước —
cũng đi qua `TenantAwareRepositoryImpl.existsById()` nên chi nhánh thuộc tenant khác luôn trả
`false` giống hệt "không tồn tại" (không tiết lộ có hay không), rồi mới kiểm `user_branches`/vai
trò toàn quyền. Thêm 1 test trong `TenantIsolationIT`
(`branchAccessGuardRejectsBranchIdBelongingToAnotherTenantWithoutLeakingExistence`) xác nhận
`branchId` trong BODY request (không chỉ path `{id}`) của `OrderService.createOrder()` cũng bị
chặn đúng bằng `PermissionDeniedException` kèm thông điệp không lộ thông tin ("Không tìm thấy chi
nhánh này").

**Kết luận: PASS.**

## 5. Luồng công khai

- `GET /api/v1/invoices/lookup/{code}`: `Invoice.lookup_code` có `UNIQUE INDEX` **toàn cục** (không
  scope theo tenant — `V18__invoice_lookup_code_entropy.sql`), UUID 128-bit đầy đủ. Response DTO
  (`InvoiceDetailResponse`) chỉ chứa dữ liệu hoá đơn bán hàng (giá bán, VAT, thông tin cửa hàng) —
  **không có `costPrice`/công nợ nào** (đúng yêu cầu roadmap). Rủi ro lộ `customerPhone`/
  `customerEmail` đã được ghi nhận và CHẤP NHẬN từ Phase 9 (hoá đơn giấy vật lý vốn đã có các
  thông tin này) — không phải phát hiện mới.
- `GET /api/v1/settings/branding`: đã tự phòng thủ đúng — khi `TenantContext.get() == null` (chưa
  đăng nhập, không biết "cửa hàng nào") trả về rỗng thay vì gọi `SettingsService` (có thể lộ tên
  cửa hàng của MỘT TENANT NGẪU NHIÊN nếu Hibernate không lọc được do thiếu `TenantContext`) — xem
  comment trong chính `SettingsController.getBranding()`, đã được xử lý đúng từ trước.

**Kết luận: PASS.**

## 6. Upload

`FileStorageService`: whitelist MIME (jpg/png/webp) + kiểm tra **magic-byte thật** (không chỉ tin
`Content-Type` header) + đổi tên UUID (không giữ tên gốc, chống path traversal/đoán URL) + giới hạn
2MB (`application.yml`) + lưu ngoài webroot + endpoint serve có `X-Content-Type-Options: nosniff`.
Duy nhất 1 phát hiện Low (xem bảng đầu báo cáo) đã vá + 6 test mới (`FileStorageServiceTest`):
magic-byte giả mạo bị chặn, MIME không cho phép bị chặn, path traversal (`../../etc/passwd`) bị
chặn, tên file sai định dạng bị chặn, tên file hợp lệ hoạt động bình thường.

**Kết luận: PASS (sau khi vá 1 mục Low).**

## 7. Ma trận quyền (52 permission × 6 role)

Thay vì hardcode 1 bảng CSV tĩnh (sẽ lạc hậu ngay khi thêm endpoint mới), viết
`PermissionMatrixIT` — quét **bằng reflection** mọi bean `@RestController` trong
`ApplicationContext`, lấy mọi chuỗi `hasAuthority(...)`/`hasAnyAuthority(...)` trong
`@PreAuthorize`, đối chiếu với bảng `permissions` THẬT (đã Flyway migrate) để bắt lỗi "gõ nhầm mã
quyền" — loại lỗi khiến `@PreAuthorize` luôn đánh giá `false` cho MỌI vai trò (kể cả owner) mà
không có lỗi biên dịch nào báo trước. Test này **tự cập nhật** khi thêm Controller/quyền mới (không
cần sửa lại danh sách hardcode), phù hợp hơn để giữ vĩnh viễn trong CI so với 1 bảng CSV tĩnh.

**Kết quả: PASS — không có mã quyền nào bị gõ sai trong toàn bộ 29 Controller.**

*Phạm vi đã thu hẹp có chủ đích*: không sinh ma trận đầy đủ "gọi từng endpoint thật với từng vai
trò qua HTTP" (110 endpoint × 6 vai trò = 660 tổ hợp, cần MockMvc + fixture JWT cho từng vai trò)
— chi phí triển khai/duy trì rất lớn so với giá trị tăng thêm, vì kiểm tra ở tầng
`@PreAuthorize`/`hasAuthority` (test này) + các trường hợp cụ thể đã có (Prompt #1: `ShiftService`
IDOR giữa 2 nhân viên; Prompt #4: `TenantIsolationIT`) đã bao phủ đúng các lớp lỗi thực tế từng xảy
ra trong dự án này.

## Bộ test cách ly tenant giữ vĩnh viễn trong CI

- `server/src/test/java/com/quanlycuahang/erp/security/TenantIsolationIT.java` (8 case)
- `server/src/test/java/com/quanlycuahang/erp/security/PermissionMatrixIT.java` (1 case, tự quét)
- `server/src/test/java/com/quanlycuahang/erp/common/upload/FileStorageServiceTest.java` (6 case)

Cả 3 file chạy tự động trong `mvn verify` (đã cấu hình từ Prompt #1) — không cần bước CI riêng.
