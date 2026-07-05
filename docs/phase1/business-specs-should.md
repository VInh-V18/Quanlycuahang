# Phase 1.3a — Đặc tả nghiệp vụ SHOULD

Cùng template: Mô tả → Tiền điều kiện → Luồng chính → Luồng phụ → Ngoại lệ →
Quy tắc → Hậu điều kiện.

## UC-11 — Khuyến mãi / Voucher

**Mô tả**: Tạo chương trình khuyến mãi (CK % hoặc số tiền, áp dụng theo sản
phẩm/danh mục/toàn đơn) và voucher (mã dùng 1 lần hoặc nhiều lần, có hạn
dùng, đơn tối thiểu, giới hạn lượt).

**Tiền điều kiện**: Có quyền `promotion:manage`.

**Luồng chính**:
1. Tạo `Promotion`/`Voucher` với điều kiện áp dụng (đối tượng, hạn dùng,
   đơn tối thiểu, giới hạn lượt).
2. Ở POS (UC-04 bước 3), hệ thống verify voucher qua API riêng trước khi
   cộng vào tổng đơn.
3. Khi đơn hoàn tất, ghi `voucher_usages` tăng lượt đã dùng.

**Ngoại lệ**: Voucher hết hạn/hết lượt/không đủ điều kiện đơn tối thiểu →
`VOUCHER_INVALID` (422), báo lỗi rõ ràng ngay ở bước áp dụng (B4 edge case
4).

**Quy tắc**: Lượt dùng kiểm tra tại thời điểm áp dụng VÀ tại thời điểm tạo
đơn (double-check để tránh 2 request đồng thời vượt giới hạn lượt).

**Hậu điều kiện**: `voucher_usages` phản ánh đúng số lượt đã dùng, không
vượt giới hạn cấu hình.

---

## UC-12 — Trả hàng (khách hàng)

**Mô tả**: Trả hàng theo hóa đơn gốc, tối đa = số lượng đã mua − đã trả.

**Tiền điều kiện**: Có quyền `return:create`; đơn gốc ở trạng thái
`completed`/`partially_returned`.

**Luồng chính**:
1. Tra cứu hóa đơn gốc theo mã/số HĐ.
2. Chọn dòng + số lượng trả (≤ số lượng đã mua − đã trả của dòng đó).
3. Backend tính tiền hoàn theo đơn giá thực trả (sau mọi chiết khấu đã
   phân bổ về dòng, lấy từ snapshot `OrderItem`).
4. Nhập lại kho với giá vốn tại thời điểm bán (`costPrice` snapshot trên
   `OrderItem`, không join sang `Product` lấy giá hiện tại).
5. Cập nhật trạng thái `Order` theo state machine
   (`completed → partially_returned → fully_returned`).
6. Hoàn tiền (tiền mặt/chuyển khoản) hoặc giảm công nợ nếu đơn gốc bán nợ.

**Ngoại lệ**: Trả vượt số lượng còn lại → `RETURN_QUANTITY_EXCEEDED` (422).

**Quy tắc**: Toàn bộ bước 3–6 nằm trong 1 `@Transactional` (`ReturnService`)
— B4.

**Hậu điều kiện**: Tồn kho, công nợ, trạng thái đơn cập nhật nhất quán.

---

## UC-13 — Hủy đơn hàng

**Mô tả**: Hủy đơn `completed` trong ngày, cần quyền Quản lý trở lên.

**Tiền điều kiện**: Đơn còn trong ngày tạo; user có `order:void`.

**Luồng chính**:
1. Kiểm tra `canTransition(completed, cancelled)` hợp lệ (chỉ trong ngày).
2. Hoàn kho: ghi `InventoryTransaction` (loại `cancel`) trả lại số lượng.
3. Hoàn công nợ (nếu đơn gốc bán nợ, giảm `Debt` tương ứng).
4. Hoàn tiền (nếu đã thu tiền mặt/chuyển khoản).

**Ngoại lệ**: Hủy đơn quá ngày tạo → `ORDER_CANCEL_WINDOW_EXPIRED` (422).

**Quy tắc**: Validate chuyển trạng thái tập trung trong `canTransition`
(B4) — không rải if-else khắp service. Bước 2–4 trong cùng 1
`@Transactional`.

**Hậu điều kiện**: Đơn `cancelled`, mọi ảnh hưởng (kho/nợ/tiền) hoàn tác
đúng và nhất quán.

---

## UC-14 — Công nợ khách hàng

**Mô tả**: Theo dõi công nợ phát sinh khi bán nợ (UC-04), ghi nhận thanh
toán dần, cảnh báo vượt hạn mức.

**Luồng chính**:
1. Bán nợ (UC-04) → `Debt` tăng theo khách hàng.
2. Khách trả dần → ghi `debt_payments`.
3. Xem lịch sử đối chiếu (danh sách phát sinh + đã trả theo thời gian).
4. Cảnh báo khi số dư nợ mới vượt hạn mức cấu hình theo từng khách.

**Quy tắc**: Số dư nợ = Σ(nợ phát sinh) − Σ(đã trả), luôn tính lại được từ
lịch sử `debt_payments`, không lưu số dư "cache" không đối chiếu được.

**Hậu điều kiện**: Số dư nợ chính xác, có cảnh báo kịp thời trước khi bán
nợ vượt hạn mức.

---

## UC-15 — Công nợ nhà cung cấp

**Mô tả**: Tương tự UC-14 nhưng theo chiều phải trả NCC, phát sinh từ UC-05
khi nhập hàng chưa trả đủ tiền.

---

## UC-16 — Kiểm kê kho

**Mô tả**: Tạo phiếu kiểm (chốt tồn hệ thống tại thời điểm tạo) → đếm thực
tế → sinh phiếu cân bằng chênh lệch kèm lý do.

**Tiền điều kiện**: Có quyền `stock-take:create` (tạo) và
`stock-take:approve` (duyệt).

**Luồng chính**:
1. Tạo `StockTake`; hệ thống chốt snapshot tồn hiện tại theo từng sản
   phẩm vào `StockTakeItem.expectedQty`.
2. Nhân viên kho đếm thực tế, nhập `actualQty`.
3. Hệ thống tính chênh lệch (`actualQty − expectedQty`).
4. Duyệt phiếu → sinh `InventoryTransaction` (loại `stock_take`) điều
   chỉnh tăng/giảm, kèm lý do bắt buộc khi có chênh lệch.

**Ngoại lệ**: Duyệt phiếu có chênh lệch mà không nhập lý do →
`VALIDATION_STOCK_TAKE_REASON_REQUIRED` (400).

**Hậu điều kiện**: Tồn kho khớp số đếm thực tế, có ghi chú lý do để truy
vết sau này.

---

## UC-17 — Báo cáo lợi nhuận gộp

**Mô tả**: Lợi nhuận gộp = doanh thu − Σ(costPrice snapshot × số lượng) −
ảnh hưởng hoàn trả, theo khoảng ngày/chi nhánh.

**Quy tắc**: Dùng `costPrice` snapshot trên `OrderItem` tại thời điểm bán,
không lấy giá `Product` hiện tại (khớp B4/D5). Chỉ user có
`report:gross-profit` mới thấy trường này (Phase 1.1 — quy tắc 3).

---

## UC-18 — Treo đơn POS

**Mô tả**: Cho phép thu ngân tạm dừng đơn đang thao tác dở để phục vụ
khách khác, mở lại sau.

**Luồng chính**:
1. Lưu giỏ hàng hiện tại thành `ParkedOrder` (chưa trừ kho, chưa phải
   `Order` chính thức).
2. Mở lại → khôi phục giỏ hàng vào FE cart (Redux).

**Hậu điều kiện**: Không ảnh hưởng tồn kho/kế toán cho đến khi đơn được
thanh toán thật (UC-04).

---

## UC-19 — Quản lý ca & két tiền

**Mô tả**: Mở ca ghi tiền đầu ca; mọi giao dịch tiền mặt (bán, hoàn, chi)
gắn với ca; kết ca đối chiếu tiền hệ thống vs tiền đếm thực.

**Tiền điều kiện**: Có quyền `shift:open`/`shift:close`.

**Luồng chính**:
1. Thu ngân mở ca, nhập số tiền mặt đầu ca (`Shift.openingCash`).
2. Trong ca: mọi `OrderPayment` tiền mặt + `CashTransaction` (thu/chi
   khác) được gắn `shiftId`.
3. Kết ca: hệ thống tính tiền mặt lý thuyết = `openingCash + Σthu tiền mặt
   − Σchi tiền mặt`; thu ngân nhập tiền đếm thực tế; hệ thống ghi chênh
   lệch kèm ghi chú bắt buộc nếu có sai lệch.

**Ngoại lệ**: Kết ca khi còn đơn treo (`ParkedOrder`) chưa xử lý → cảnh báo
xác nhận trước khi đóng ca.

**Hậu điều kiện**: `Shift` ở trạng thái `closed`, có báo cáo đối chiếu tiền
mặt theo ca.
