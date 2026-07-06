# Phase 4.5 — Trạng thái UI chuẩn (áp dụng mọi màn hình)

## Loading

- **Danh sách/bảng** (`DataTable` — Phase 5.3): skeleton rows (3–5 dòng mờ,
  animate-pulse) thay vì spinner toàn màn hình — giữ nguyên khung layout,
  tránh "nhảy" giao diện khi data về.
- **Form submit**: disable nút submit + spinner nhỏ trong nút (không thay
  đổi text nút để tránh layout shift), disable toàn bộ field trong form.
- **Trang lần đầu load** (route mới): spinner giữa màn hình chỉ khi thời
  gian tải > 300ms (tránh nháy loading cho request nhanh — dùng delay
  trong `TanStack Query` hoặc component wrapper).

## Empty

- Icon minh họa nhạt (dùng bộ icon nhất quán, vd Lucide) + tiêu đề ngắn +
  1 dòng mô tả + nút hành động chính nếu có (vd "Chưa có sản phẩm nào" +
  nút "Thêm sản phẩm đầu tiên").
- Phân biệt rõ 2 trường hợp: **chưa từng có dữ liệu** (empty state đầy đủ
  như trên) vs **lọc/tìm kiếm không ra kết quả** (message ngắn hơn: "Không
  tìm thấy kết quả phù hợp" + nút "Xóa bộ lọc").

## Error

- **Lỗi nghiệp vụ** (400/409/422 — mã `UPPER_SNAKE` từ Backend D2): hiện
  `Toast` đỏ với message tiếng Việt đã map từ code (FE giữ bảng map
  code → message, không hardcode message ở từng nơi gọi API).
- **Lỗi hệ thống** (500/timeout/mất mạng): `Toast` với message chung
  "Có lỗi xảy ra, vui lòng thử lại" + nút "Thử lại" nếu action có thể
  retry an toàn (idempotent).
- **Lỗi validate form**: hiện ngay dưới từng field (không dồn hết lên
  đầu form), border field chuyển `destructive`, giữ nguyên dữ liệu đã
  nhập ở field khác.
- Không bao giờ để trắng trang hoặc treo vô hạn khi lỗi — mọi lỗi phải có
  đường thoát rõ ràng (nút thử lại/quay lại).

## Dark mode

- Toggle ở User menu (Topbar), lưu lựa chọn vào `localStorage`
  (`theme: light | dark | system`), mặc định theo `prefers-color-scheme`
  của hệ điều hành nếu chưa từng chọn.
- Mọi màu dùng qua token CSS variable (Phase 4.1) — cấm hardcode hex trực
  tiếp trong component, để đổi theme không cần sửa từng nơi.
- Ảnh/logo cửa hàng (do người dùng tự upload) giữ nguyên không invert màu;
  chỉ áp dụng dark mode cho UI hệ thống (nút, nền, chữ, border).

## Responsive breakpoint (theo Tailwind mặc định)

| Breakpoint | Hành vi chính |
|---|---|
| `< 768px` (mobile) | Sidebar ẩn (hamburger overlay); bảng chuyển thành danh sách card xếp dọc; POS chuyển tab (Phase 4.4) |
| `768–1279px` (tablet) | Sidebar thu gọn còn icon; bảng giữ dạng bảng nhưng ẩn bớt cột phụ |
| `≥ 1280px` (desktop) | Đầy đủ sidebar mở rộng + bảng đầy đủ cột |

Riêng màn hình POS ưu tiên desktop/tablet ngang (≥ 1024×768) theo D4 — máy
tính tiền hiếm khi dùng ở độ phân giải mobile dọc.
