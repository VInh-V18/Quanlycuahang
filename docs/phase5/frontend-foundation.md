# Phase 5 — Frontend Foundation

Phạm vi đúng F5.1–5.3 của master prompt: scaffold + hạ tầng + component nền — **không**
bao gồm màn hình nghiệp vụ đầy đủ (giỏ hàng POS thật, CRUD sản phẩm/đơn hàng...), các màn đó
thuộc phạm vi xây dựng tính năng Frontend theo từng module, ngoài 12 phase của master prompt.

## ⚠️ Bug thật phát hiện khi verify bằng trình duyệt thật (không phải giả định)

Test đăng nhập lần đầu qua Playwright + Chromium thật thất bại với lỗi mạng chung chung dù
mật khẩu đúng lẫn sai đều không phân biệt được — hoá ra do 2 nguyên nhân độc lập, cả hai chỉ lộ
ra khi test bằng trình duyệt thật (curl không bao giờ phát hiện được vì curl không áp dụng chính
sách cookie/CORS của trình duyệt):

1. **Cookie `Secure` chặn hoàn toàn refresh token ở dev local**: `AuthController` hard-code
   `.secure(true)` cho cookie `refreshToken`. Theo RFC 6265, cookie `Secure` chỉ được trình duyệt
   lưu khi kết nối là HTTPS — dev local chạy `http://localhost`, trình duyệt âm thầm từ chối lưu
   cookie, không có lỗi nào hiện ra, refresh token không bao giờ tồn tại. **Đã sửa**: thêm cấu
   hình `app.auth.refresh-cookie-secure` (mặc định `true`, override `false` ở
   `application-local.yml` kèm giải thích) — inject qua `@Value` vào `AuthController` thay vì
   hard-code.
2. **Origin `127.0.0.1` không khớp CORS whitelist `localhost`**: Backend cấu hình
   `allowed-origins: http://localhost:5173`, nhưng lần test đầu chạy trình duyệt trỏ vào
   `http://127.0.0.1:5173` (2 origin khác nhau dù cùng loopback) → mọi request bị CORS chặn,
   axios trả lỗi mạng chung chung. Đã sửa cách chạy trình duyệt test để trỏ đúng `localhost`;
   ghi chú lại đây để không lặp lại nhầm lẫn khi Phase 12 cấu hình domain thật.

Sau khi sửa, đăng nhập/đăng xuất/refresh/phân quyền đều hoạt động đúng qua trình duyệt thật (xem
bảng verify bên dưới).

## 5.1 Scaffold

- Vite 5 + React 18 + TypeScript 5 (ép version đúng Part C — `npm create vite` mặc định cài
  React 19/Vite 8/TS 6 mới nhất, phải hạ version thủ công trong `package.json`).
- Tailwind 3 + `tailwindcss-animate`, design tokens/CSS variables lấy nguyên từ
  `docs/phase4/design-tokens.md` (không chỉnh sửa).
- shadcn/ui: viết tay từng primitive (`button`, `input`, `label`, `card`, `dialog`,
  `dropdown-menu`, `select`, `popover`, `tabs`, `table`, `avatar`, `badge`, `skeleton`,
  `separator`, `toast`+`use-toast`+`toaster`, `form`, `calendar`) theo đúng mã nguồn chuẩn cộng
  đồng shadcn — **không dùng CLI `shadcn add`** vì sandbox không có quyền truy cập
  `ui.shadcn.com`/registry ngoài whitelist proxy.
- Router: `react-router-dom` 6, `createBrowserRouter` + `React.lazy`/`Suspense` cho mọi trang
  (xác nhận qua `vite build`: mỗi trang tách chunk riêng — `LoginPage`, `DashboardPage`,
  `ProductsPage`, `PosPage`).
- 3 layout: `MainLayout` (Sidebar 240px thu gọn 64px + Topbar 56px, đúng `layout.md`),
  `AuthLayout` (căn giữa, không sidebar/topbar), `PosLayout` (toàn màn hình, POSBar riêng, khung
  2 cột 60/40 đúng `pos-design.md` — nội dung bên trong khung là placeholder, chưa phải tính năng
  bán hàng thật).
- Dark/light: class `dark` trên `<html>`, đồng bộ 2 chiều với Redux `ui.theme` (persist).

## 5.2 Hạ tầng dữ liệu

- `apiClient` (axios, `baseURL: /api/v1`, `withCredentials: true`): interceptor request gắn
  `Authorization: Bearer`, interceptor response bắt `401` → gọi `/auth/refresh` (dùng cookie
  httpOnly), có hàng đợi (`pendingQueue`) để nhiều request 401 đồng thời chỉ refresh 1 lần, dùng
  `_retry` flag chống lặp vô hạn.
- `bootstrapSession()`: gọi `/auth/refresh` 1 lần lúc app khởi động để "đăng nhập ngầm" bằng
  cookie còn hạn — vì access token chỉ sống trong RAM (D3), nếu không có bước này, F5 giữa ca sẽ
  đá thu ngân về màn đăng nhập dù phiên vẫn còn hạn. Đã verify: reload trang không mất phiên.
- TanStack Query 5 (`queryClient`, `staleTime` 30s) cho toàn bộ dữ liệu từ API; Redux Toolkit 2 +
  `redux-persist` chỉ cho `cart` và `ui` (`whitelist`), **`auth` KHÔNG persist** (D3 — access
  token không bao giờ chạm localStorage, chỉ tồn tại trong RAM qua Redux store thường).
  Permissions/username lấy từ giải mã payload JWT phía client (`decodeJwtPayload`) — chỉ phục vụ
  hiển thị UI, không dùng để xác thực (xác thực luôn ở Backend).
- `<RequireAuth>` (điều hướng `/login` nếu chưa có access token) và `<RequirePermission perm="...">`
  (chặn cả trang, hiện `ForbiddenPage` nếu thiếu quyền) — khác `<PermissionGate>` (ẩn/hiện từng
  phần tử UI, dùng trong Sidebar).
- Form: React Hook Form 7 + Zod 3 qua `@hookform/resolvers`.

## 5.3 Component nền

`DataTable`, `FormField`, `ConfirmDialog`, `Money`, `DateRangePicker`, `Toast`
(`use-toast`/`Toaster`), `PermissionGate` — đủ theo yêu cầu Gate. `DataTable` đọc đúng
`meta.page/limit/total` (Pageable response D2), hỗ trợ phân trang (gọi lại API), lọc (ô tìm kiếm
rời, nơi gọi tự quản lý debounce/state), sắp xếp (click header).

**Ghi chú kỹ thuật quan trọng**: 2 endpoint danh sách hiện có (`/products`, `/customers`) dùng
native `@Query` với `ORDER BY` cố định trong SQL (viết ở Phase 7) — Spring Data không thể an
toàn nối thêm `Pageable.getSort()` vào sau một `ORDER BY` đã có sẵn, và Sort theo tên thuộc tính
Java không tự ánh xạ sang tên cột snake_case trong native query. Vì vậy demo sắp xếp ở
`ProductsPage` thực hiện **phía client trên trang dữ liệu hiện tại** (đã ghi chú rõ trong code) —
phân trang và lọc vẫn gọi API thật. Đây là nợ kỹ thuật đã biết, không phải lỗi ẩn: khi xây dựng
màn danh sách thật ở các module sau, cần chuyển các endpoint cần sắp xếp động sang
`JpaSpecificationExecutor` (đã có sẵn interface, dùng `CriteriaBuilder.function()` gọi
`immutable_unaccent`) để `Pageable.getSort()` hoạt động đúng qua JPA Criteria.

## Đã verify bằng trình duyệt thật (Playwright + Chromium, không chỉ đọc code)

| Kịch bản | Kết quả |
|---|---|
| Chưa đăng nhập, vào `/` | Điều hướng đúng `/login` |
| Đăng nhập sai mật khẩu | Toast đỏ hiện đúng message Backend trả (`Sai ten dang nhap hoac mat khau`) |
| Đăng nhập đúng (`owner01`/`Password@123`) | Điều hướng `/`, cookie `refreshToken` được lưu (`httpOnly=true`, `secure=false` ở local, `sameSite=Strict`) |
| Reload trang sau khi đăng nhập | Vẫn ở `/` (silent refresh qua `bootstrapSession` hoạt động, không bị đá về `/login`) |
| Vào `/products` | `DataTable` tải đúng 10/31 sản phẩm thật từ API, hiện đúng "Trang 1/4 — tổng 31 bản ghi" |
| Gõ "coca" vào ô tìm kiếm | Lọc còn đúng 1 dòng "Coca-Cola lon 330ml" (gọi lại API thật) |
| Click header "Giá bán" 2 lần | Sắp xếp tăng dần rồi giảm dần đúng theo giá (client-side, xem ghi chú trên) |
| Bấm "Sau" | Chuyển đúng "Trang 2/4" |
| Vào `/pos` | Layout 2 cột + POSBar (nút thoát, tên thu ngân, đồng hồ realtime) hiện đúng, không lỗi |
| Bấm nút đổi giao diện | `<html>` nhận class `dark`, toàn bộ theme đổi đúng token (đã chụp ảnh so sánh) |
| Đăng xuất | Điều hướng `/login`, cookie `refreshToken` bị xoá hoàn toàn |
| Vào lại `/products` sau đăng xuất | Điều hướng đúng `/login` (`RequireAuth` hoạt động) |

## Lệnh kiểm tra

```bash
cd client
npm install
npm run dev        # http://localhost:5173 — cần trỏ đúng "localhost", không dùng 127.0.0.1 (CORS)
npm run build       # tsc -b && vite build — xác nhận type-check + code-splitting
npm test            # Vitest — 6 test (utils, jwt decode, Money) pass
npx eslint . --ext ts,tsx   # 0 error (3 warning react-refresh, thuộc pattern chuẩn shadcn)
```
