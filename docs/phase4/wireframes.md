# Phase 4.3 — Wireframe & mô tả UX (15 màn hình, POS xem riêng `pos-design.md`)

Mỗi màn: **Mục đích** → **Thành phần chính** → **Hành động chính** →
**Trạng thái** (theo chuẩn `ui-states.md`) → **Phím tắt** (nếu có).

## 1. Dashboard

- **Mục đích**: tổng quan nhanh tình hình kinh doanh khi đăng nhập.
- **Thành phần**: 4 KPI card (doanh thu hôm nay, số đơn, khách mới, sản
  phẩm sắp hết hàng); biểu đồ doanh thu 7 ngày (Recharts line chart); bảng
  top 5 sản phẩm bán chạy; danh sách cảnh báo tồn kho thấp.
- **Hành động chính**: chọn khoảng thời gian (hôm nay/tuần/tháng); click
  KPI card điều hướng sang trang chi tiết tương ứng (Báo cáo).
- **Trạng thái**: skeleton cho KPI card + chart khi loading; empty state
  cho biểu đồ nếu chưa có đơn nào (cửa hàng mới).
- **Phím tắt**: không có (trang xem, không thao tác tần suất cao).

## 2. Sản phẩm

- **Mục đích**: quản lý danh mục + danh sách sản phẩm.
- **Thành phần**: sidebar cây danh mục (trái) + `DataTable` sản phẩm
  (phải: ảnh, SKU, tên, danh mục, giá bán, tồn kho, trạng thái); ô tìm
  kiếm không dấu; nút "+ Thêm sản phẩm"; form modal/trang riêng (tên, danh
  mục, đơn vị, barcode, giá vốn/bán, VAT, ảnh, tồn tối thiểu).
- **Hành động chính**: tạo/sửa/xóa (soft delete) sản phẩm; tạo/sửa/xóa
  danh mục; xem lịch sử giá (`price_history`) từ trang chi tiết sản phẩm.
- **Trạng thái**: empty khi danh mục chưa có sản phẩm; error rõ ràng khi
  trùng SKU/barcode (`PRODUCT_DUPLICATE_SKU`); xác nhận (`ConfirmDialog`)
  trước khi xóa.
- **Phím tắt**: không bắt buộc.

## 3. Nhập kho

- **Mục đích**: tạo phiếu nhập hàng từ nhà cung cấp.
- **Thành phần**: chọn NCC (autocomplete); bảng dòng nhập (thêm sản phẩm,
  SL, đơn giá — tính thành tiền dòng tự động); tổng tiền phiếu; ghi chú
  công nợ (nếu mua thiếu, hiện input số tiền đã trả).
- **Hành động chính**: thêm/xóa dòng; xác nhận tạo phiếu (hiện xác nhận vì
  không sửa được sau khi tạo — chỉ tạo phiếu trả hàng NCC bù trừ).
- **Trạng thái**: disable nút xác nhận khi chưa có dòng nào; loading khi
  submit (tính giá vốn bình quân xảy ra ở Backend).
- **Phím tắt**: không bắt buộc.

## 4. Tồn kho (thẻ kho)

- **Mục đích**: xem tồn kho hiện tại theo chi nhánh + lịch sử biến động.
- **Thành phần**: `DataTable` tồn kho (sản phẩm, tồn, giá vốn, giá trị
  tồn = tồn × giá vốn); badge cảnh báo đỏ nếu dưới tồn tối thiểu; click
  vào 1 dòng mở "Thẻ kho" (timeline `InventoryTransaction`: loại giao
  dịch, số lượng +/-, thời gian, tham chiếu).
- **Hành động chính**: lọc theo chi nhánh/danh mục; xuất Excel (COULD,
  Phase 12); điều hướng sang "Kiểm kê" khi phát hiện chênh lệch nghi ngờ.
- **Trạng thái**: empty nếu chi nhánh chưa có tồn kho nào.

## 5. Kiểm kê

- **Mục đích**: đối chiếu tồn hệ thống với tồn thực tế đếm tay.
- **Thành phần**: bước 1 — tạo phiếu (chọn phạm vi sản phẩm/danh mục, hệ
  thống chốt `expectedQty`); bước 2 — bảng nhập `actualQty` cho từng sản
  phẩm (highlight đỏ/xanh dòng có chênh lệch); bước 3 — xem trước phiếu
  cân bằng, bắt buộc nhập lý do nếu có chênh lệch, nút "Duyệt".
- **Hành động chính**: nhập số đếm thực tế (hỗ trợ quét barcode để nhảy
  nhanh đến dòng sản phẩm); duyệt phiếu (chỉ Quản lý/Chủ cửa hàng).
- **Trạng thái**: cảnh báo xác nhận nếu duyệt phiếu còn dòng chưa nhập
  `actualQty`.

## 6. Đơn hàng

- **Mục đích**: xem danh sách + chi tiết đơn đã bán, hủy đơn trong ngày.
- **Thành phần**: `DataTable` (mã đơn, ngày giờ, khách hàng, thu ngân,
  tổng tiền, trạng thái badge màu theo state machine); bộ lọc khoảng ngày
  + chi nhánh + trạng thái + thu ngân; trang chi tiết đơn (đầy đủ dòng
  hàng, thanh toán, nút in lại hóa đơn, nút trả hàng, nút hủy đơn).
- **Hành động chính**: xem chi tiết; in lại hóa đơn; tạo phiếu trả hàng từ
  đơn; hủy đơn (chỉ Quản lý, chỉ trong ngày — disable nút + tooltip giải
  thích nếu quá hạn).
- **Trạng thái**: badge màu theo trạng thái (`draft` xám, `completed`
  xanh lá, `partially_returned`/`fully_returned` vàng/cam, `cancelled` đỏ
  gạch ngang).

## 7. Trả hàng

- **Mục đích**: lập phiếu trả hàng theo hóa đơn gốc.
- **Thành phần**: ô tra cứu số HĐ/mã đơn; bảng dòng hàng gốc (SL đã mua,
  đã trả, còn lại — chỉ cho nhập SL trả ≤ còn lại); tổng tiền hoàn tự động
  tính theo đơn giá thực trả (snapshot); chọn phương thức hoàn tiền.
- **Hành động chính**: xác nhận trả hàng (1 transaction: hoàn kho + hoàn
  tiền/công nợ + cập nhật trạng thái đơn).
- **Trạng thái**: error rõ ràng nếu nhập SL vượt còn lại
  (`RETURN_QUANTITY_EXCEEDED`).

## 8. Hóa đơn

- **Mục đích**: xem/in lại hóa đơn, tra cứu bằng mã.
- **Thành phần**: template in K80 mặc định (chọn K58/A4 tùy chọn) render
  bằng CSS `@media print`; nút "In" (`window.print()`), nút "Gửi email";
  QR tra cứu đơn (`qrcode.react`) từ payload Backend trả về.
- **Hành động chính**: in; gửi email; tra cứu công khai qua mã (trang
  riêng không cần đăng nhập, chỉ đọc).
- **Trạng thái**: loading khi tạo QR; error nếu gửi email thất bại (không
  chặn việc đã bán hàng thành công — email là hành động phụ sau commit).

## 9. Khách hàng

- **Mục đích**: quản lý thông tin khách hàng + xem lịch sử mua/công nợ.
- **Thành phần**: `DataTable` (tên, SĐT, nhóm khách, tổng chi tiêu, dư nợ);
  form tạo/sửa; tab trong trang chi tiết: "Lịch sử đơn hàng" / "Công nợ".
- **Hành động chính**: tạo/sửa khách hàng; xem lịch sử; điều hướng nhanh
  sang POS với khách đã chọn.

## 10. Nhà cung cấp

- **Mục đích**: quản lý NCC + lịch sử nhập hàng/công nợ phải trả.
- **Thành phần**: tương tự Khách hàng, tab "Lịch sử nhập hàng" / "Công nợ
  phải trả".
- **Hành động chính**: tạo/sửa NCC; xem lịch sử; điều hướng sang "Nhập
  kho" với NCC đã chọn.

## 11. Công nợ

- **Mục đích**: theo dõi & thu/trả công nợ 2 chiều (KH/NCC).
- **Thành phần**: 2 tab "Phải thu" (KH) / "Phải trả" (NCC); `DataTable`
  (tên, dư nợ hiện tại, hạn mức, % đã dùng hạn mức — progress bar cảnh
  báo đỏ nếu gần/vượt hạn mức); modal "Ghi nhận thanh toán" (số tiền,
  phương thức, ghi chú).
- **Hành động chính**: ghi nhận thanh toán từng phần; xem lịch sử đối
  chiếu đầy đủ (phát sinh + đã trả theo thời gian).
- **Trạng thái**: cảnh báo màu vàng/đỏ khi dư nợ gần/vượt hạn mức.

## 12. Nhân viên & phân quyền

- **Mục đích**: quản lý tài khoản nhân viên và gán vai trò.
- **Thành phần**: `DataTable` (tên, username, SĐT, vai trò badge, trạng
  thái active); form tạo/sửa (chọn 1+ vai trò từ 6 vai trò Phase 1.1);
  trang riêng "Ma trận quyền" (chỉ xem, tham chiếu — quyền gán theo vai
  trò có sẵn, không tạo vai trò tùy chỉnh ở MVP).
- **Hành động chính**: tạo nhân viên; đổi vai trò (chỉ Chủ cửa hàng —
  `employee:manage-permission`); vô hiệu hóa tài khoản (không xóa cứng).
- **Trạng thái**: chỉ hiển thị menu này nếu user có `employee:view`.

## 13. Ca & két

- **Mục đích**: mở/đóng ca, xem lịch sử ca và giao dịch tiền mặt.
- **Thành phần**: banner ca hiện tại (nếu đang mở: giờ mở, tiền đầu ca,
  tổng thu/chi tạm tính); nút "Mở ca"/"Đóng ca"; modal đóng ca (tiền lý
  thuyết vs tiền đếm thực tế, chênh lệch tự tính, bắt buộc ghi chú nếu
  chênh lệch ≠ 0); `DataTable` lịch sử ca đã đóng.
- **Hành động chính**: mở ca (nhập tiền đầu ca); đóng ca; ghi thu/chi tiền
  mặt ngoài đơn (modal riêng, lý do bắt buộc).

## 14. Báo cáo

- **Mục đích**: xem báo cáo doanh thu/lợi nhuận/tồn kho/công nợ/nhân viên.
- **Thành phần**: bộ lọc dùng chung (`ReportFilter`: khoảng ngày + chi
  nhánh); tab theo loại báo cáo (Doanh thu, Lợi nhuận gộp, Tồn kho, Công
  nợ, Hiệu suất nhân viên, Top khách hàng); biểu đồ (Recharts) + bảng chi
  tiết bên dưới; nút "Xuất Excel"/"Xuất PDF".
- **Hành động chính**: đổi bộ lọc (áp dụng cho toàn bộ tab đang xem); xuất
  file.
- **Trạng thái**: ẩn tab "Lợi nhuận gộp" nếu user không có quyền
  `report:gross-profit` (không chỉ ẩn ở UI — Backend cũng chặn ở DTO).

## 15. Cài đặt

- **Mục đích**: cấu hình tham số hệ thống (chỉ Chủ cửa hàng).
- **Thành phần**: các nhóm cấu hình dạng form: Làm tròn (đơn vị làm tròn
  100đ/500đ/1000đ), Thuế (giá mặc định gồm/chưa gồm VAT), Kho (bật/tắt
  `allow_negative_stock`), Công nợ (hạn mức mặc định), Hóa đơn (mẫu SKU,
  mẫu số hóa đơn, mẫu in mặc định K80/K58/A4), Chi nhánh (thêm/sửa chi
  nhánh — COULD).
- **Hành động chính**: lưu từng nhóm cấu hình độc lập (không phải 1 form
  khổng lồ); xem trước ảnh hưởng khi đổi làm tròn (ví dụ tính minh họa).
- **Trạng thái**: chỉ hiển thị menu này nếu user có `settings:view`; nút
  lưu disable nếu chưa thay đổi gì (tránh gọi API thừa).
