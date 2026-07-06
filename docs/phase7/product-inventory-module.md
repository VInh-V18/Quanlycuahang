# Phase 7 — Module Sản phẩm & Kho

## ⚠️ Bug thật phát hiện qua test chạy thật (không phải giả định)

`BaseEntity.createdAt`/`updatedAt` ban đầu khai báo `OffsetDateTime` (theo đúng B3: "Java
OffsetDateTime/Instant"). Khi tạo sản phẩm đầu tiên ở Phase 7, request thất bại với
`500 INTERNAL_ERROR`. Log thực tế:

```
IllegalArgumentException: Cannot convert unsupported date type java.time.LocalDateTime
to java.time.OffsetDateTime; Supported types are [LocalDateTime, LocalDate, LocalTime,
Instant, Date, Long, long]
```

`Spring Data Commons` (`DefaultAuditableBeanWrapperFactory`, bản đi kèm Spring Boot 3.3.5)
không hỗ trợ `OffsetDateTime` làm kiểu đích cho `@CreatedDate`/`@LastModifiedDate` — chỉ
hỗ trợ `Instant` trong nhóm kiểu `java.time`. Đã sửa: `BaseEntity.createdAt`/`updatedAt`
đổi sang `Instant` (B3 cho phép cả hai); các cột không đi qua Spring Auditing (`Shift`,
`Invoice`, `DebtPayment`...) vẫn giữ `OffsetDateTime` bình thường vì đó là mapping JPA/
Hibernate thuần túy, không liên quan đến bug này.

## 7.1 Sản phẩm

- `CategoryService`: cây 2 cấp — validate `parent.getParent() == null` khi gán danh mục
  cha (chặn tạo cấp 3); xóa danh mục kiểm tra cả danh mục con lẫn sản phẩm tham chiếu.
- `ProductService`: SKU tự sinh theo `sku_prefix` (từ `SettingsService`) + số thứ tự, retry
  nếu trùng; lịch sử giá ghi **trực tiếp trong Service** khi `sellPrice` thay đổi — không
  dùng `@EntityListeners` như văn bản gốc đề xuất (JPA EntityListener không phải Spring
  bean, không thể inject Repository một cách an toàn/idiomatic; ghi trong Service đơn giản
  hơn, vẫn cùng transaction, đạt đúng mục tiêu "giữ lịch sử thay đổi giá").
- Xóa sản phẩm gọi thẳng `productRepository.deleteById()` — soft delete tự động qua
  `@SQLDelete` đã cấu hình ở Phase 3, không cần logic thêm.
- Tìm không dấu: `immutable_unaccent` + `ILIKE` (chứa) + `pg_trgm %` (gần đúng), native
  query có `countQuery` riêng cho phân trang.

## 7.2 Nhập kho

- `AverageCostService`: Java thuần, không `@Service`, để unit test không cần Spring
  context (Phase 11) — đúng công thức B4, `RoundingMode.HALF_UP`, scale 0 (khớp
  `NUMERIC(15,0)`).
- `PurchaseOrderService.create()`: 1 `@Transactional`, khóa `PESSIMISTIC_WRITE` trên dòng
  `Inventory` đang cập nhật (tránh race condition nhập đồng thời), tính lại giá vốn từng
  dòng, ghi `InventoryTransaction` (loại `purchase`), tự động tạo `Debt` (payable) nếu
  `paidAmount < tổng tiền`.

## 7.3 Tồn & kiểm kê

- `InventoryService`: tồn theo chi nhánh, cảnh báo dưới tồn tối thiểu
  (`stock <= product.minStock`), thẻ kho phân trang theo `product_id + created_at`
  (đúng index D4 đã tạo ở Phase 3).
- `StockTakeService`: tạo phiếu snapshot toàn bộ `Inventory` hiện có của chi nhánh vào
  `expectedQty`; nhập `actualQty` qua endpoint riêng; duyệt bắt buộc lý do cho mọi dòng có
  chênh lệch (`STOCK_TAKE_REASON_REQUIRED`) — validate 2 vòng lặp (kiểm tra toàn bộ trước,
  chỉ ghi khi tất cả hợp lệ, tránh ghi dở dang).

## Đã verify bằng ứng dụng chạy thật

| Kịch bản | Kết quả |
|---|---|
| Tìm "ca phe sua da" (không dấu) → khớp "Cà phê sữa đá đặc biệt" (có dấu thật) | ✅ đúng B3 |
| Tạo phiếu nhập 50 đơn vị giá 40.000, tồn cũ 98 @ 38.500 | Giá vốn mới = **39.007** (tính tay: 5.773.000/148 = 39006,756 → HALF_UP 39007) — khớp chính xác |
| `paidAmount` < tổng tiền phiếu nhập | Tự tạo `Debt` payable đúng số tiền còn thiếu |
| Kiểm kê: duyệt khi có dòng lệch nhưng chưa nhập lý do | 422 `STOCK_TAKE_REASON_REQUIRED` |
| Kiểm kê: duyệt sau khi bổ sung lý do | Tồn kho cập nhật đúng bằng `actualQty`, `InventoryTransaction` loại `stock_take` ghi đúng chênh lệch |

## Lệnh kiểm tra

```bash
cd server
mvn clean install
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
