# Phase 1.2 — Đặc tả nghiệp vụ MUST (MVP)

Mỗi use case: Mô tả → Tiền điều kiện → Luồng chính (đánh số) → Luồng phụ →
Ngoại lệ → Quy tắc (đối chiếu B4/D-chuẩn) → Hậu điều kiện.

## UC-01 — Đăng nhập / Refresh token rotation / Đăng xuất

**Mô tả**: Nhân viên đăng nhập bằng username/password để nhận access token
(JWT, 15 phút, memory FE) và refresh token (httpOnly cookie `SameSite=Strict`,
7 ngày, rotation).

**Tiền điều kiện**: Tài khoản tồn tại, chưa bị khóa/xóa.

**Luồng chính**:
1. Người dùng nhập username/password.
2. Backend kiểm tra rate limit (5 lần/15 phút/IP — Bucket4j + Redis).
3. Backend xác thực bằng `BCryptPasswordEncoder` (strength 12).
4. Backend sinh access token (JWT, chứa userId + authorities) và refresh
   token (lưu Redis theo `tokenFamily`), set cookie httpOnly.
5. Frontend lưu access token trong memory (Redux) — không localStorage.
6. Access token hết hạn hoặc nhận 401 → Frontend tự gọi endpoint refresh
   (kèm cookie).
7. Backend kiểm tra refresh token trong Redis; nếu hợp lệ → sinh cặp token
   mới (rotation), vô hiệu hóa token cũ.
8. Refresh token bị dùng lại sau khi đã rotate → phát hiện reuse, thu hồi
   toàn bộ `tokenFamily`, buộc đăng nhập lại.
9. Đăng xuất → xóa refresh token khỏi Redis + xóa cookie.

**Luồng phụ**:
- 6a. Access token hết hạn giữa lúc thanh toán → giỏ hàng POS không mất
  (Redux + redux-persist), đăng nhập lại xong tiếp tục (Edge case B4.5).

**Ngoại lệ**:
- Sai mật khẩu → `AUTH_INVALID_CREDENTIALS` (401), không tiết lộ do sai
  username hay password.
- Vượt rate limit → `AUTH_RATE_LIMIT_EXCEEDED` (429).
- Refresh token hết hạn/không tồn tại → 401, buộc đăng nhập lại.
- Phát hiện reuse → ghi `AuditLog` (`AUTH_TOKEN_REUSE_DETECTED`).

**Quy tắc**: D3 (mật khẩu, JWT, rate limit).

**Hậu điều kiện**: Người dùng có phiên hợp lệ hoặc bị từ chối rõ ràng.

---

## UC-02 — Quản lý danh mục sản phẩm

**Mô tả**: Quản lý cây danh mục 2 cấp dùng để phân loại sản phẩm.

**Tiền điều kiện**: Có quyền `category:*`.

**Luồng chính**:
1. Xem danh sách danh mục dạng cây.
2. Tạo danh mục (chọn cha hoặc để trống = danh mục gốc).
3. Sửa tên/mô tả/thứ tự hiển thị.
4. Xóa danh mục.

**Luồng phụ**: Sắp xếp lại thứ tự hiển thị.

**Ngoại lệ**: Xóa danh mục còn sản phẩm/danh mục con tham chiếu →
`CATEGORY_HAS_PRODUCTS` (422), yêu cầu chuyển sản phẩm trước.

**Quy tắc**: Chỉ 2 cấp (self-referencing `parent_id`), không đệ quy sâu hơn.

**Hậu điều kiện**: Cây danh mục phản ánh đúng cấu trúc sản phẩm hiện có.

---

## UC-03 — Quản lý sản phẩm

**Mô tả**: CRUD sản phẩm: SKU (tự sinh theo mẫu cấu hình), barcode, đơn vị
tính, giá vốn/bán, VAT rate, ảnh, tồn tối thiểu.

**Tiền điều kiện**: Có quyền `product:*`.

**Luồng chính**:
1. Nhập thông tin sản phẩm (tên, danh mục, đơn vị, giá bán theo cấu hình
   gồm/chưa gồm VAT, VAT rate, barcode hoặc để hệ thống tự sinh, tồn tối
   thiểu).
2. Hệ thống tự sinh SKU theo mẫu cấu hình nếu không nhập tay.
3. Lưu sản phẩm.
4. Sửa giá bán → ghi vào `price_history` (giữ lịch sử thay đổi giá).
5. Xóa sản phẩm đã có giao dịch → soft delete (ẩn khỏi POS, giữ lịch sử).

**Luồng phụ**: Tìm sản phẩm không dấu (unaccent + pg_trgm — B3).

**Ngoại lệ**: Trùng SKU/barcode → `PRODUCT_DUPLICATE_SKU` /
`PRODUCT_DUPLICATE_BARCODE` (409).

**Quy tắc**: Sửa giá bán sau khi đã có đơn cũ → đơn cũ giữ giá snapshot,
không hồi tố (B4 edge case 2). Giá vốn chỉ đổi khi nhập hàng (B4). Xóa sản
phẩm đã giao dịch chỉ soft delete (B4 edge case 3).

**Hậu điều kiện**: Sản phẩm sẵn sàng bán ở POS (nếu chưa xóa).

---

## UC-04 — Bán hàng POS (đầy đủ, quan trọng nhất)

**Mô tả**: Luồng bán hàng tại quầy: thêm hàng vào giỏ, áp chiết khấu/voucher,
thanh toán, in hóa đơn.

**Tiền điều kiện**: User có `order:create`; ca đã mở nếu module ca đã bật.

**Luồng chính** (đối chiếu đúng công thức tính tiền B4):
1. Quét barcode/tìm sản phẩm không dấu → thêm vào giỏ hàng (FE, Redux +
   redux-persist).
2. Sửa số lượng, áp chiết khấu dòng (% hoặc số tiền).
3. Áp chiết khấu đơn và/hoặc voucher — FE gọi API verify hiệu lực
   voucher/hạn mức đơn tối thiểu trước khi cộng vào tổng.
4. FE tính tổng theo đúng 7 bước B4: thành tiền dòng → sau CK dòng → tổng
   hàng → sau CK đơn/voucher (phân bổ ngược về dòng theo tỷ trọng) → VAT
   (bóc tách hoặc cộng thêm tùy cấu hình) → làm tròn → tổng thanh toán.
5. Chọn phương thức thanh toán (tiền mặt/chuyển khoản/thẻ); nhập tiền khách
   đưa nếu tiền mặt.
6. FE gửi request tạo đơn kèm header `Idempotency-Key` (UUID); Backend kiểm
   tra Redis — nếu đã xử lý, trả lại kết quả lần đầu, không tạo đơn thứ 2
   (Edge case B4.7).
7. Backend tính lại toàn bộ bằng `OrderPricingService` (độc lập FE) — lệch
   với số FE gửi → reject `ORDER_PRICE_MISMATCH` (422).
8. Backend trong 1 `@Transactional`: tạo `Order` + `OrderItem` (snapshot
   `productName`/`unitPrice`/`costPrice`) + trừ kho (`@Version`) + ghi
   `InventoryTransaction` (loại `sale`) + tạo `Invoice` + `OrderPayment`
   (+ ghi công nợ nếu bán nợ).
9. Trả dữ liệu hóa đơn JSON; FE render và in K80.

**Luồng phụ**:
- Treo đơn: lưu giỏ hàng hiện tại thành `ParkedOrder`, mở lại sau.
- Bán nợ: chọn khách hàng, cảnh báo nếu vượt hạn mức nợ cấu hình.

**Ngoại lệ**:
- Hết tồn, không cho bán âm → `PRODUCT_OUT_OF_STOCK` (409/422). Edge case
  B4.1: 2 request đồng thời tranh 1 đơn vị cuối → đúng 1 thành công, 1 lỗi.
- Voucher hết hạn/hết lượt/không đủ điều kiện → 422 rõ ràng ở bước áp dụng
  (B4 edge case 4).
- Mất mạng khi đang in → đơn đã lưu server, in lại được từ chi tiết đơn
  (B4 edge case 6).

**Quy tắc**: Toàn bộ phép tính nằm trong `OrderPricingService` thuần Java,
không phụ thuộc Spring context (B4). Chống oversell 2 lớp: `@Version` +
CHECK constraint DB.

**Hậu điều kiện**: Đơn `completed`, tồn kho giảm đúng, hóa đơn in/tra cứu
được.

---

## UC-05 — Nhập kho (Purchase Order)

**Mô tả**: Ghi nhận nhập hàng từ nhà cung cấp, tính lại giá vốn bình quân
gia quyền di động.

**Tiền điều kiện**: Có quyền `purchase-order:create`.

**Luồng chính**:
1. Chọn nhà cung cấp, thêm nhiều dòng sản phẩm (số lượng, đơn giá nhập).
2. Xác nhận phiếu nhập.
3. Backend trong 1 `@Transactional`, với mỗi dòng: tính
   `giá_vốn_mới = (tồn_hiện_tại × giá_vốn_cũ + SL_nhập × giá_nhập) /
   (tồn_hiện_tại + SL_nhập)`, cập nhật `Inventory.costPrice` + `stock`,
   ghi `InventoryTransaction` (loại `purchase`).
4. Mua thiếu (chưa trả đủ NCC) → ghi `Debt` (công nợ phải trả) +
   `DebtPayment` nếu trả một phần.

**Luồng phụ**: Trả hàng NCC — giảm tồn, ghi `InventoryTransaction` (loại
`supplier_return`), giữ nguyên giá vốn hiện tại (không tính ngược).

**Ngoại lệ**: Số lượng/giá ≤ 0 → 400 validation.

**Quy tắc**: Giá vốn chỉ thay đổi khi nhập hàng, tính bằng `BigDecimal` +
`RoundingMode.HALF_UP`, scale cố định — cấm `double`/`float` (B4).

**Hậu điều kiện**: Tồn kho tăng đúng, giá vốn cập nhật chính xác từng đồng,
công nợ NCC (nếu có) được ghi nhận.

---

## UC-06 — Tồn kho, thẻ kho & cảnh báo tồn thấp

**Mô tả**: Xem tồn kho hiện tại theo chi nhánh, lịch sử biến động (thẻ
kho), cảnh báo dưới tồn tối thiểu.

**Tiền điều kiện**: Có quyền `inventory:view`.

**Luồng chính**:
1. Xem tồn kho theo chi nhánh (`Inventory`, khóa composite
   `productId+branchId`).
2. Xem thẻ kho: danh sách `InventoryTransaction` theo sản phẩm, phân
   trang, đủ mọi loại giao dịch (`purchase`, `sale`, `supplier_return`,
   `customer_return`, `stock_take`, `transfer`, `cancel`).
3. Định kỳ (`@Scheduled`) hoặc theo query, kiểm tra sản phẩm dưới tồn tối
   thiểu → cảnh báo.

**Quy tắc**: Tồn hiện tại luôn tính lại được từ lịch sử
`inventory_transactions` (B4) — không có nguồn dữ liệu tồn nào khác.

**Hậu điều kiện**: Người quản lý nắm được tình trạng tồn kho theo thời gian
thực.

---

## UC-07 — Quản lý khách hàng

**Mô tả**: CRUD khách hàng, nhóm khách hàng, xem lịch sử mua hàng/công nợ.

**Luồng chính**: Tạo/sửa khách hàng (tên, SĐT, địa chỉ, nhóm); xem lịch sử
đơn hàng và công nợ.

**Quy tắc**: SĐT hợp lệ định dạng Việt Nam (`@ValidPhoneVN` — Phase 6.3).

**Hậu điều kiện**: Khách hàng sẵn sàng dùng ở POS (tìm nhanh theo SĐT/tên).

---

## UC-08 — Quản lý nhà cung cấp

**Mô tả**: CRUD nhà cung cấp, xem lịch sử nhập hàng/công nợ phải trả.
Tương tự UC-07 nhưng chiều nhà cung cấp.

---

## UC-09 — Hóa đơn in K80

**Mô tả**: Backend trả dữ liệu hóa đơn JSON đầy đủ; Frontend render + in
bằng CSS `@media print`.

**Luồng chính**:
1. Sau khi tạo đơn thành công, Backend sinh `Invoice` (số HĐ theo mẫu cấu
   hình, ngày giờ, thu ngân, danh sách dòng, tổng/CK/VAT tách theo thuế
   suất, làm tròn, tổng thanh toán, phương thức, tiền khách đưa/thừa,
   payload QR VietQR nếu chuyển khoản, mã tra cứu).
2. Frontend gọi endpoint lấy dữ liệu hóa đơn theo id, render template K80,
   gọi `window.print()`.
3. In lại bất kỳ lúc nào từ màn hình chi tiết đơn (B4 edge case 6).

**Quy tắc**: Backend không sinh HTML/PDF, chỉ trả JSON (B3).

**Hậu điều kiện**: Hóa đơn giấy khớp 100% số liệu với đơn gốc.

---

## UC-10 — Báo cáo doanh thu ngày/tháng

**Mô tả**: Tổng hợp doanh thu theo ngày/tháng/chi nhánh/thu ngân.

**Luồng chính**: Chọn khoảng ngày + chi nhánh (`ReportFilter` dùng chung) →
Backend aggregate bằng native query (`GROUP BY`, `date_trunc()`) → trả tổng
doanh thu, số đơn, giá trị trung bình/đơn.

**Quy tắc**: Không load hết Entity về JVM rồi tính bằng Stream (D4) — dùng
SQL aggregate; nếu chậm → `daily_sales_summary` cập nhật bằng `@Scheduled`.

**Hậu điều kiện**: Số liệu khớp với đơn hàng `completed` trong khoảng lọc.
