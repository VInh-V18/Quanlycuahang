# Phase 4.4 — Thiết kế POS (chi tiết nhất, quan trọng nhất)

## Mục đích màn hình
Thu ngân/nhân viên bán hàng xử lý toàn bộ 1 giao dịch bán hàng — từ chọn
sản phẩm đến in hóa đơn — trong thời gian ngắn nhất, sai sót thấp nhất, kể
cả khi thao tác hoàn toàn bằng bàn phím + máy quét barcode (không dùng
chuột).

## Yêu cầu nền tảng
- Độ phân giải tối thiểu hỗ trợ: **1024×768** (máy tính bán hàng phổ thông
  tại quầy) — layout không được vỡ ở kích thước này; ưu tiên **cảm ứng**
  (nút bấm tối thiểu 44×44px theo chuẩn touch target) vì nhiều cửa hàng
  dùng màn hình cảm ứng làm POS.
- **Scanner-friendly**: máy quét barcode hoạt động như bàn phím ảo (gửi
  chuỗi ký tự + Enter) — ô tìm kiếm sản phẩm phải **auto-focus** ngay khi
  vào màn hình POS và **tự động refocus** sau mỗi lần thêm sản phẩm vào
  giỏ hoặc đóng modal, để quét liên tục không cần click chuột lại.

## Cột trái — Tìm & chọn sản phẩm (~60% chiều rộng)

- Ô tìm kiếm luôn ở trên cùng, auto-focus, placeholder "Quét mã vạch hoặc
  gõ tên sản phẩm...". Gõ không dấu vẫn ra kết quả đúng (B3 — unaccent +
  pg_trgm, ví dụ gõ "ca phe" ra "Cà phê").
- Quét barcode khớp chính xác 1 sản phẩm → tự động thêm vào giỏ (số lượng
  +1 nếu đã có trong giỏ), không cần bước xác nhận thêm — tối ưu tốc độ.
- Gõ tay ra nhiều kết quả gần đúng → hiện lưới/danh sách, có thể dùng phím
  mũi tên + Enter để chọn (không bắt buộc dùng chuột).
- Tab danh mục ngang phía trên lưới sản phẩm để lọc nhanh bằng cảm ứng khi
  không dùng bàn phím/scanner (ví dụ nhân viên bán hàng dùng tablet).
- Mỗi ô sản phẩm hiển thị: ảnh (hoặc icon mặc định theo danh mục), tên,
  giá bán, badge đỏ nhỏ nếu tồn kho ≤ tồn tối thiểu (cảnh báo sắp hết).

## Cột phải — Giỏ hàng & thanh toán (~40% chiều rộng)

- Danh sách dòng: tên sản phẩm, đơn giá, số lượng (bấm +/- hoặc gõ trực
  tiếp), chiết khấu dòng (% hoặc số tiền, click để mở popover nhập), thành
  tiền dòng — cập nhật tổng ngay lập tức (không có nút "tính lại").
- Vùng chọn khách hàng (tìm theo SĐT/tên, tùy chọn — bỏ trống = khách lẻ),
  ô nhập mã voucher (verify ngay khi rời ô nhập, báo lỗi rõ ràng nếu
  không hợp lệ — B4 edge case 4), chiết khấu toàn đơn.
- Khối tổng tiền tách bạch rõ ràng, cỡ chữ lớn nhất màn hình (`text-3xl`,
  `tabular-nums`): Tổng hàng → Chiết khấu → VAT (nếu giá chưa gồm VAT) →
  **Tổng thanh toán** (đậm, nổi bật nhất) → Tiền khách đưa (nếu tiền mặt)
  → Tiền thừa (tính tự động).
- 2 nút hành động chính ở đáy, luôn cố định trong khung nhìn (không cuộn
  mất): **Treo đơn** (phụ, viền) và **Thanh toán** (chính, nền `primary`,
  chiếm phần lớn chiều rộng, phím tắt F9).

## Phím tắt bàn phím (bắt buộc, không dùng chuột vẫn thao tác được)

| Phím | Hành động |
|---|---|
| `F1` | Focus vào ô tìm kiếm sản phẩm |
| `F2` | Mở tìm khách hàng |
| `F3` | Mở nhập mã voucher |
| `F4` | Mở chiết khấu toàn đơn |
| `F5` | Treo đơn hiện tại |
| `F6` | Mở danh sách đơn đang treo |
| `F7` | Xóa dòng đang chọn trong giỏ |
| `F8` | Xóa toàn bộ giỏ hàng (có xác nhận `ConfirmDialog`) |
| `F9` | Mở màn hình thanh toán / xác nhận thanh toán |
| `Esc` | Đóng modal/popover đang mở, quay lại focus ô tìm kiếm |

## Trạng thái UI trong POS

- **Loading**: khi gọi API tạo đơn — khóa nút Thanh toán (disabled +
  spinner), không khóa toàn màn hình để tránh cảm giác "treo máy".
- **Empty**: giỏ hàng trống → hiện icon giỏ hàng mờ + text "Chưa có sản
  phẩm nào, quét mã vạch hoặc tìm kiếm để bắt đầu".
- **Error**: lỗi nghiệp vụ (hết hàng, voucher không hợp lệ, lệch giá) hiện
  `Toast` đỏ ở góc trên phải POSBar, không chặn thao tác tiếp theo, không
  làm mất dữ liệu giỏ hàng đã nhập.
- Mất kết nối mạng khi đang thao tác giỏ hàng → giỏ hàng vẫn giữ nguyên
  (Redux + redux-persist, B4 edge case 5); banner cảnh báo nhỏ "Mất kết
  nối, một số thao tác có thể chậm" ở đầu POSBar.

## Responsive & Dark mode

- Dưới 1024px chiều rộng: 2 cột chuyển thành tab chuyển đổi ("Sản phẩm" /
  "Giỏ hàng (n)") thay vì side-by-side, vẫn giữ nguyên phím tắt.
- Dark mode dùng đúng token Phase 4.1 — riêng khối "Tổng thanh toán" giữ
  độ tương phản cao ở cả 2 theme (test thực tế bằng công cụ contrast
  checker trước khi bàn giao Phase 5).
