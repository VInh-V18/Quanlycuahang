# Phase 1.1 — Ma trận phân quyền (RBAC)

## 6 vai trò (roles)

| Mã vai trò | Tên hiển thị | Mô tả |
|---|---|---|
| `owner` | Chủ cửa hàng | Toàn quyền, kể cả cấu hình hệ thống và phân quyền |
| `manager` | Quản lý | Vận hành toàn bộ nghiệp vụ chi nhánh, trừ cấu hình hệ thống cấp cao |
| `cashier` | Thu ngân | Bán hàng POS, thu tiền, mở/đóng ca — không sửa giá, không xóa dữ liệu |
| `sales_staff` | Nhân viên bán hàng | Bán hàng POS, quản lý khách hàng — không thao tác kho, không xem báo cáo tài chính |
| `warehouse_staff` | Nhân viên kho | Nhập/xuất/kiểm kê kho, quản lý nhà cung cấp — không bán hàng, không xem doanh thu |
| `accountant` | Kế toán | Xem báo cáo, quản lý công nợ, xuất Excel/PDF — không thao tác bán hàng/kho trực tiếp |

Ghi chú: bảng `permissions` seed sẵn 6 role này ở Phase 3 (V2__seed_data.sql); người dùng
thực tế có thể gán 1 user cho nhiều role (bảng `user_roles` many-to-many) nếu cửa hàng nhỏ
cần một người kiêm nhiều việc — quyền là hợp (union) của mọi role được gán.

## Ma trận resource:action

Ký hiệu: ✅ = có quyền · — = không có quyền.
Cột "Chuỗi authority" là giá trị lưu trong bảng `permissions.code`, dùng trực tiếp trong
`@PreAuthorize("hasAuthority('product:create')")` (Phase 6).

### Hệ thống & nhân sự

| Chuỗi authority | Mô tả | owner | manager | cashier | sales_staff | warehouse_staff | accountant |
|---|---|:-:|:-:|:-:|:-:|:-:|:-:|
| `employee:view` | Xem danh sách nhân viên | ✅ | ✅ | — | — | — | — |
| `employee:create` | Tạo nhân viên mới | ✅ | ✅ | — | — | — | — |
| `employee:update` | Sửa thông tin nhân viên | ✅ | ✅ | — | — | — | — |
| `employee:delete` | Xóa (vô hiệu hóa) nhân viên | ✅ | — | — | — | — | — |
| `employee:manage-permission` | Đổi vai trò/quyền của nhân viên | ✅ | — | — | — | — | — |
| `branch:view` | Xem danh sách chi nhánh | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `branch:manage` | Thêm/sửa chi nhánh | ✅ | — | — | — | — | — |
| `settings:view` | Xem cấu hình hệ thống | ✅ | ✅ | — | — | — | — |
| `settings:update` | Sửa cấu hình (làm tròn, VAT mặc định, hạn mức nợ...) | ✅ | — | — | — | — | — |
| `audit-log:view` | Xem nhật ký audit | ✅ | ✅ | — | — | — | — |

### Sản phẩm & danh mục

| Chuỗi authority | Mô tả | owner | manager | cashier | sales_staff | warehouse_staff | accountant |
|---|---|:-:|:-:|:-:|:-:|:-:|:-:|
| `product:view` | Xem sản phẩm | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `product:create` | Tạo sản phẩm | ✅ | ✅ | — | — | ✅ | — |
| `product:update` | Sửa sản phẩm (bao gồm sửa giá — audit riêng) | ✅ | ✅ | — | — | — | — |
| `product:delete` | Xóa (soft delete) sản phẩm | ✅ | ✅ | — | — | — | — |
| `category:view` | Xem danh mục | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `category:create` | Tạo danh mục | ✅ | ✅ | — | — | — | — |
| `category:update` | Sửa danh mục | ✅ | ✅ | — | — | — | — |
| `category:delete` | Xóa danh mục | ✅ | ✅ | — | — | — | — |

### Bán hàng (POS)

| Chuỗi authority | Mô tả | owner | manager | cashier | sales_staff | warehouse_staff | accountant |
|---|---|:-:|:-:|:-:|:-:|:-:|:-:|
| `order:view` | Xem đơn hàng | ✅ | ✅ | ✅ (đơn của mình/ca) | ✅ (đơn của mình) | — | ✅ |
| `order:create` | Tạo đơn (bán hàng) | ✅ | ✅ | ✅ | ✅ | — | — |
| `order:void` ⚠️ | Hủy đơn đã hoàn tất (trong ngày) — **mồ côi từ Prompt #10 (P3)**: endpoint duy nhất dùng quyền này (`POST /orders/{id}/cancel`) đã bị xoá (dead code, không FE nào gọi), permission vẫn còn trong DB seed (đã chạy, không sửa) nhưng không còn code nào kiểm tra | ✅ | ✅ | — | — | — | — |
| `order:park` | Treo/mở lại đơn | ✅ | ✅ | ✅ | ✅ | — | — |
| `return:view` | Xem phiếu trả hàng | ✅ | ✅ | ✅ | ✅ | — | ✅ |
| `return:create` | Tạo phiếu trả hàng | ✅ | ✅ | ✅ | ✅ | — | — |
| `promotion:view` | Xem khuyến mãi/voucher | ✅ | ✅ | ✅ | ✅ | — | ✅ |
| `promotion:manage` | Tạo/sửa khuyến mãi/voucher | ✅ | ✅ | — | — | — | — |

### Kho

| Chuỗi authority | Mô tả | owner | manager | cashier | sales_staff | warehouse_staff | accountant |
|---|---|:-:|:-:|:-:|:-:|:-:|:-:|
| `inventory:view` | Xem tồn kho, thẻ kho | ✅ | ✅ | — | — | ✅ | ✅ |
| `purchase-order:view` | Xem phiếu nhập | ✅ | ✅ | — | — | ✅ | ✅ |
| `purchase-order:create` | Tạo phiếu nhập kho | ✅ | ✅ | — | — | ✅ | — |
| `purchase-order:return` | Tạo phiếu trả hàng NCC | ✅ | ✅ | — | — | ✅ | — |
| `stock-take:view` | Xem phiếu kiểm kê | ✅ | ✅ | — | — | ✅ | ✅ |
| `stock-take:create` | Tạo phiếu kiểm kê | ✅ | ✅ | — | — | ✅ | — |
| `stock-take:approve` | Duyệt phiếu cân bằng chênh lệch | ✅ | ✅ | — | — | — | — |

### Đối tác (khách hàng / nhà cung cấp / công nợ)

| Chuỗi authority | Mô tả | owner | manager | cashier | sales_staff | warehouse_staff | accountant |
|---|---|:-:|:-:|:-:|:-:|:-:|:-:|
| `customer:view` | Xem khách hàng | ✅ | ✅ | ✅ | ✅ | — | ✅ |
| `customer:create` | Tạo khách hàng | ✅ | ✅ | ✅ | ✅ | — | — |
| `customer:update` | Sửa khách hàng | ✅ | ✅ | — | ✅ | — | — |
| `supplier:view` | Xem nhà cung cấp | ✅ | ✅ | — | — | ✅ | ✅ |
| `supplier:manage` | Tạo/sửa nhà cung cấp | ✅ | ✅ | — | — | ✅ | — |
| `debt:view` | Xem công nợ KH/NCC | ✅ | ✅ | — | — | — | ✅ |
| `debt:collect-payment` | Ghi nhận thanh toán công nợ | ✅ | ✅ | ✅ | — | — | ✅ |

### Vận hành (ca & két) và hóa đơn

| Chuỗi authority | Mô tả | owner | manager | cashier | sales_staff | warehouse_staff | accountant |
|---|---|:-:|:-:|:-:|:-:|:-:|:-:|
| `shift:open` | Mở ca | ✅ | ✅ | ✅ | — | — | — |
| `shift:close` | Đóng ca (đối chiếu tiền) | ✅ | ✅ | ✅ | — | — | — |
| `shift:view` | Xem lịch sử ca | ✅ | ✅ | ✅ (ca của mình) | — | — | ✅ |
| `cash-transaction:create` | Ghi thu/chi tiền mặt ngoài đơn | ✅ | ✅ | ✅ | — | — | — |
| `invoice:view` | Xem/in hóa đơn | ✅ | ✅ | ✅ | ✅ | — | ✅ |
| `invoice:send-email` | Gửi hóa đơn qua email | ✅ | ✅ | ✅ | ✅ | — | — |

### Báo cáo

| Chuỗi authority | Mô tả | owner | manager | cashier | sales_staff | warehouse_staff | accountant |
|---|---|:-:|:-:|:-:|:-:|:-:|:-:|
| `report:revenue` | Báo cáo doanh thu | ✅ | ✅ | — | — | — | ✅ |
| `report:gross-profit` | Báo cáo lợi nhuận gộp (nhạy cảm — ẩn giá vốn với cashier/sales) | ✅ | ✅ | — | — | — | ✅ |
| `report:inventory-value` | Báo cáo giá trị tồn kho | ✅ | ✅ | — | — | ✅ | ✅ |
| `report:employee-performance` | Hiệu suất nhân viên | ✅ | ✅ | — | — | — | ✅ |
| `report:export` | Xuất Excel/PDF | ✅ | ✅ | — | — | — | ✅ |

## Quy tắc áp dụng

1. Mỗi API endpoint nhạy cảm map với đúng 1 (hoặc nhiều) authority ở trên qua
   `@PreAuthorize("hasAuthority('...')")` — không kiểm tra role trực tiếp trong code
   (role chỉ là tập hợp authority, để thêm role mới không phải sửa code Controller/Service).
2. `owner` không được seed cứng "bypass-all" trong code; thay vào đó seed đủ toàn bộ
   authority cho role `owner` trong `role_permissions` — giữ nhất quán cơ chế kiểm tra quyền
   cho mọi role kể cả cao nhất, tránh nhánh code đặc biệt khó test.
3. `report:gross-profit` chứa `costPrice` — Controller trả DTO khác nhau tùy quyền
   (ẩn trường costPrice/margin nếu người gọi không có `report:gross-profit`), không đơn
   thuần ẩn ở Frontend.
4. Toàn bộ query đọc list (`*:view`) đều lọc theo `branchId` mà user hiện tại có quyền truy cập
   (bảng `user_branches` nếu 1 user chỉ thuộc 1 số chi nhánh) — chống IDOR chéo chi nhánh,
   xử lý cụ thể tại Phase 3 (thiết kế bảng) và Phase 6 (PermissionEvaluator).
5. Danh sách trên là nguồn seed duy nhất cho `permissions` + `role_permissions` ở Phase 3 —
   không thêm authority mới ngoài luồng nếu chưa cập nhật file này trước.
