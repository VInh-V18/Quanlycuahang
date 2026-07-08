# Phase 8 — Module Bán hàng POS (QUAN TRỌNG NHẤT)

## ⚠️ Bug thật phát hiện qua test tích hợp 2 luồng (không phải giả định)

Kịch bản gate bắt buộc: "2 luồng tranh 1 sản phẩm cuối → 1 thành công, 1 lỗi
`PRODUCT_OUT_OF_STOCK`". Chạy 2 request `POST /orders` đồng thời (cùng mua 1 sản phẩm còn
tồn kho = 1) qua ứng dụng thật (không phải unit test), kết quả ban đầu: 1 đơn thành công, 1
đơn lỗi — nhưng lỗi trả về là `409 CONFLICT` chung chung, **không phải** `PRODUCT_OUT_OF_STOCK`.

Log thực tế cho thấy nguyên nhân **không nằm ở tồn kho** (cơ chế `@Version` optimistic
locking hoạt động đúng) mà ở bước sinh `order_number` **trước đó**:

```
ERROR: duplicate key value violates unique constraint "uq_orders_order_number"
Detail: Key (order_number)=(HD-000022) already exists.
```

`generateOrderNumber()`/`generateInvoiceNumber()`/`generateSku()` dùng pattern
`orderRepository.count() + 1` rồi vòng lặp `existsBy...()` — không atomic: ở mức isolation
READ COMMITTED mặc định của PostgreSQL, 2 transaction đồng thời đều đọc `count()` giống nhau
và đều thấy `existsBy...()` là `false` (vì chưa bên nào commit), nên sinh trùng số. Transaction
thứ 2 vi phạm unique constraint ở bước `INSERT orders` — xảy ra **trước khi** kịp chạm tới
bước kiểm tra tồn kho/optimistic lock của `Inventory`, nên lỗi bị `DataIntegrityViolationException`
handler (chung, 409 `CONFLICT`) bắt trước, che mất lỗi `PRODUCT_OUT_OF_STOCK` đúng nghĩa.

Đây là lỗi thật, không phải lý thuyết: hệ thống nhắm tới nhiều quầy thu ngân bán đồng thời tại
1-5 chi nhánh, nên tranh chấp sinh số đơn/hoá đơn là tình huống vận hành bình thường, không phải
edge case hiếm.

**Đã sửa**: thay pattern `count()+existsBy()` bằng PostgreSQL `SEQUENCE` (atomic ở mức DB,
không phụ thuộc transaction isolation) — migration `V3__number_sequences.sql` tạo
`order_number_seq`, `invoice_number_seq`, `sku_seq`, khởi tạo bằng `setval()` dựa trên số lớn
nhất đang có trong dữ liệu hiện tại (an toàn khi áp lên DB đã có dữ liệu seed). Thêm
`NumberSequenceService` (constructor injection `JdbcTemplate`, gọi `SELECT nextval(?)`) dùng
chung cho `OrderService` (order/invoice number) và `ProductService` (SKU). Xoá các phương thức
`existsByOrderNumber`/`existsByInvoiceNumber` không còn dùng tới (giữ lại `existsBySku` vì
`ProductService` vẫn cần kiểm tra trùng khi người dùng tự nhập SKU thủ công).

Sau khi sửa, chạy lại đúng kịch bản gate: 1 luồng thành công (order number mới, duy nhất), luồng
kia nhận đúng `PRODUCT_OUT_OF_STOCK`; tồn kho cuối cùng chính xác bằng 0 (không âm, không bị mất
cập nhật).

## Gap thật phát hiện khi test luồng trả hàng

`GET /orders/{id}` (và response `createOrder`) trả `OrderItemResponse` **không có trường `id`**,
trong khi `POST /returns` bắt buộc `orderItemId` để xác định dòng hàng cần trả — FE không có
cách nào lấy được `orderItemId` từ API đơn hàng để gọi API trả hàng. Đã thêm trường `id` vào
`OrderItemResponse` và set ở cả 2 nơi tạo response (`createOrder`, `getById`).

## 8.1 Tính tiền (7 bước B4)

`OrderPricingService` — Java thuần (không `@Service`, không phụ thuộc Spring context, unit
test không cần khởi động `ApplicationContext`): thành tiền dòng → sau CK dòng → tổng hàng →
phân bổ CK đơn + voucher về từng dòng theo tỷ trọng (dòng cuối nhận phần dư để tổng luôn khớp
tuyệt đối, không lệch 1 đồng do sai số chia làm tròn) → tách/cộng VAT theo `price_includes_vat`
→ làm tròn theo `rounding_unit` (dư làm tròn cũng dồn vào tổng, không rải về dòng) → tiền thừa.

Đã verify bằng unit test (kịch bản tay: 3 sản phẩm, CK dòng, CK đơn, tiền thừa — khớp tính tay
từng đồng) **và** bằng đơn hàng thật qua API (3 sản phẩm, CK dòng 2000/0/1000, CK đơn 5000,
voucher 10%, tiền mặt) — kết quả API khớp chính xác với tính tay ở từng dòng và tổng.

## 8.2 Tạo đơn (`OrderService.createOrder`)

- 1 `@Transactional` duy nhất: nạp Product + Inventory hiện tại (Backend là nguồn giá/VAT/giá
  vốn duy nhất, không tin FE), validate voucher, gọi `OrderPricingService`, so khớp
  `expectedTotalAmount` (lệch → `ORDER_PRICE_MISMATCH`), ghi `Order`/`OrderItem` (snapshot
  `productName`/`unitPrice`/`costPrice`/`vatRate` tại thời điểm bán)/`InventoryTransaction`/
  `OrderPayment`/`Invoice`, tạo `Debt` nếu bán nợ (yêu cầu có khách hàng), ghi nhận lượt dùng
  voucher.
- Chống oversell 2 lớp: (1) kiểm tra `stock >= qty` trước khi trừ (bỏ qua nếu
  `allow_negative_stock=true`); (2) `@Version` optimistic locking trên `Inventory`, gọi
  `saveAndFlush()` để buộc phát hiện xung đột NGAY trong transaction thay vì đợi tới commit —
  `OptimisticLockingFailureException` được `GlobalExceptionHandler` (có sẵn từ Phase 2) map
  thành `409 PRODUCT_OUT_OF_STOCK`.
- Idempotency: `IdempotencyInterceptor` (chỉ áp cho path `/api/v1/orders`) kiểm tra/khoá key
  trong Redis ở `preHandle()`; `OrderService` gọi `idempotencyService.complete()` sau khi tạo
  đơn thành công — request lặp lại cùng `Idempotency-Key` trả về đúng đơn đã tạo, không tạo
  đơn trùng.
- `cancelOrder()`: chỉ huỷ được đơn tạo cùng ngày (giờ Asia/Ho_Chi_Minh), theo state machine
  (`draft`/`completed` → `cancelled`), hoàn trả tồn kho, xoá nợ liên quan nếu có.

## 8.3 Đặt trước, trả hàng, thanh toán

- `ParkedOrderService`: giữ giỏ hàng dưới dạng `cartSnapshot` (chuỗi JSON, FE tự quyết định cấu
  trúc, Backend không diễn giải) — `park()`/`listByBranch()`/`resume()` (xoá bản ghi khi resume).
- `ReturnService.createReturn()`: tối đa = số lượng đã mua trừ đã trả (`RETURN_QUANTITY_EXCEEDED`
  nếu vượt); hoàn tiền theo đơn giá thực trả = `lineTotal / quantity` (đã qua mọi CK phân bổ, đúng
  B4 — không lấy lại giá gốc); nhập lại kho theo giá vốn snapshot trên `OrderItem` (không lấy giá
  vốn hiện tại của Product); nếu đơn gốc có `Debt` liên kết, giảm nợ trước, phần dư còn lại coi
  như hoàn tiền mặt/CK theo `refundMethod`; cập nhật trạng thái đơn (`partially_returned`/
  `fully_returned`) dựa trên tổng đã trả so với tổng đã mua của mọi dòng.
- `VietQrService`: build payload EMVCo (TLV) + CRC16-CCITT tự viết, đã verify CRC khớp test
  vector chuẩn ("123456789" → "29B1") trước khi dùng cho payload thật.

## Đã verify bằng ứng dụng chạy thật (không chỉ compile)

| Kịch bản | Kết quả |
|---|---|
| Đơn 3 sản phẩm + CK dòng + CK đơn + tiền mặt dư | Khớp tính tay từng đồng (subtotal/discount/VAT/rounding/total/change) |
| Lặp lại cùng `Idempotency-Key` | Trả về đúng đơn cũ (cùng `id`), không tạo đơn/trừ kho lần 2 |
| 2 luồng đồng thời tranh 1 sản phẩm tồn = 1 | 1 thành công, 1 lỗi `PRODUCT_OUT_OF_STOCK`; tồn kho cuối = 0 chính xác |
| 3 luồng đồng thời tranh sản phẩm tồn = 2 | 1 thành công (mất mát cơ hội bán do khoá lạc quan không tự retry — đúng thiết kế B4), 2 lỗi `PRODUCT_OUT_OF_STOCK` |
| Park giỏ hàng → resume | Trả đúng `cartSnapshot` đã lưu, xoá khỏi danh sách đặt trước sau khi resume |
| Trả hàng 1 phần (2/5) | `totalRefund` khớp đơn giá hiệu lực × số lượng; tồn kho +2; trạng thái đơn → `partially_returned` |
| Trả hàng vượt số lượng còn lại | 422 `RETURN_QUANTITY_EXCEEDED` |
| Huỷ đơn cùng ngày | Trạng thái → `cancelled`, tồn kho hoàn trả đúng; huỷ lần 2 → `ORDER_CANCEL_NOT_ALLOWED` |
| Đơn có voucher 10% + thanh toán `bank_transfer` | Giảm giá/VAT/làm tròn khớp tính tay; sinh `qrPayload` EMVCo hợp lệ; `used_count` voucher tăng đúng 1 |

## Lệnh kiểm tra

```bash
cd server
mvn clean install
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
