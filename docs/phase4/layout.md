# Phase 4.2 — Layout

## Layout chính (mọi trang, kể cả POS): Sidebar + Topbar

```mermaid
flowchart TD
    subgraph Topbar["Topbar (cao 56px, sticky top)"]
        direction LR
        T1["Chọn chi nhánh (dropdown)"] --- T2["Tìm kiếm nhanh (Ctrl+K)"] --- T3["Thông báo (chuông + badge số)"] --- T4["User menu (avatar, tên, đăng xuất)"]
    end

    subgraph Body["Vùng nội dung"]
        direction LR
        subgraph Sidebar["Sidebar (rộng 240px, thu gọn còn 64px)"]
            direction TB
            S1["Dashboard"]
            S2["Bán hàng (POS)"]
            S3["Sản phẩm"]
            S4["Kho (Nhập/Tồn/Kiểm kê)"]
            S5["Đơn hàng / Trả hàng"]
            S6["Khách hàng"]
            S7["Nhà cung cấp"]
            S8["Công nợ"]
            S9["Ca & két"]
            S10["Báo cáo"]
            S11["Nhân viên & phân quyền"]
            S12["Cài đặt"]
        end
        Content["Nội dung trang (padding 24px, max-width responsive)"]
    end

    Topbar --> Body
    Sidebar --> Content
```

**Hành vi**:
- Sidebar thu gọn (chỉ icon) trên màn hình < 1280px hoặc khi bấm nút
  toggle; trên mobile (< 768px) sidebar ẩn hoàn toàn, mở bằng hamburger
  menu overlay.
- Menu item chỉ hiển thị nếu user có ít nhất 1 quyền `view` trong nhóm đó
  (`<PermissionGate>` — Phase 5.3) — không hiển thị mục "Nhân viên & phân
  quyền" cho `cashier`/`sales_staff`/`warehouse_staff`.
- Route `/pos` dùng chung layout này (Sidebar + Topbar) như mọi trang khác
  trong hệ thống; vùng nội dung bên trong tự co gọn khoảng cách (xem mục
  dưới) để bù lại phần chiều rộng Sidebar chiếm dụng.

## Vùng nội dung POS (bên trong layout chính)

```mermaid
flowchart LR
    subgraph Main["Vùng chính (2 cột, thay thế padding trang mặc định để tối đa không gian)"]
        direction LR
        subgraph Left["Cột trái ~60%: Tìm & chọn sản phẩm"]
            direction TB
            L1["Ô tìm kiếm / quét barcode (auto-focus)"]
            L2["Lưới sản phẩm (ảnh, tên, giá) hoặc danh mục tab"]
        end
        subgraph Right["Cột phải ~40%: Giỏ hàng & thanh toán"]
            direction TB
            R1["Danh sách dòng giỏ hàng (SL, CK dòng, thành tiền)"]
            R2["Chọn khách hàng / voucher / CK đơn"]
            R3["Tổng hàng — CK — VAT — Tổng thanh toán (nổi bật, chữ lớn)"]
            R4["Nút: Treo đơn | Thanh toán (F9)"]
        end
    end
```

**Lý do 2 cột (không phải 3 cột hay xếp dọc)**: thu ngân cần nhìn đồng
thời sản phẩm đang chọn (trái) và tổng tiền cập nhật realtime (phải) mà
không cuộn trang — tối ưu cho thao tác nhanh, tay trái quét barcode/click
chuột, mắt theo dõi cột phải để đọc số tiền cho khách. Chi tiết đầy đủ tại
[`pos-design.md`](./pos-design.md).
