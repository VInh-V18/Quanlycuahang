# Phase 9 — Module Hóa đơn

## 9.1 Backend — endpoint JSON hóa đơn đầy đủ

- `GET /api/v1/invoices/{id}` (quyền `invoice:view`) và `GET /api/v1/invoices/lookup/{lookupCode}`
  (công khai, `permitAll` — khách quét QR trên hóa đơn giấy không cần đăng nhập; an toàn IDOR vì
  `lookupCode` là UUID rút gọn ngẫu nhiên sinh lúc tạo đơn ở Phase 8, không đoán được từ id tuần
  tự và không cho liệt kê hóa đơn khác).
- `InvoiceDetailAssembler` (Java thuần, không Spring, cùng phong cách `OrderPricingService`
  Phase 8) lắp ráp `InvoiceDetailResponse` từ Entity đã tải sẵn — tách riêng khỏi
  `InvoiceDetailService` (Spring, chỉ lo truy vấn DB) để unit test không cần
  Testcontainers/ApplicationContext. Verify bằng **snapshot đúng đơn hàng thật đã kiểm chứng ở
  Phase 8** (HD-000021) — `InvoiceDetailAssemblerTest` dựng lại đúng dữ liệu đơn đó và assert từng
  đồng, đặc biệt bước gộp VAT theo thuế suất (5% và 10% tách riêng, tổng taxable+VAT khớp chính
  xác `totalAmount`, không lệch 1 đồng) — đúng Gate "endpoint JSON hóa đơn có test snapshot".
- Bổ sung dữ liệu còn thiếu để hóa đơn xem/in lại đúng từng đồng (migration
  `V4__invoice_fields.sql`): `orders.cash_received`/`orders.change_amount` (trước đây chỉ tính
  lúc tạo đơn, không lưu — không thể tái tạo khi xem hóa đơn cũ); `customers.email`; setting
  `store_name`/`store_tax_code` (tên thương hiệu chuỗi cửa hàng, khác với tên/địa chỉ/SĐT từng chi
  nhánh đã có sẵn ở bảng `branches`).
- `EInvoiceProvider` — interface Java + `NoOpEInvoiceProvider` (bean mặc định, chỉ log, không gọi
  API ngoài nào). **Chưa tích hợp thật** với Viettel S-Invoice/MISA/VNPT — các nhà cung cấp này
  đều cần hợp đồng thương mại/mã số thuế doanh nghiệp thật để đăng ký sandbox, không thể tích hợp
  và kiểm chứng thật trong phạm vi phiên làm việc này. Javadoc trên interface ghi rõ cách thay thế
  sau này (viết `@Component` implement interface, đăng ký đè `NoOpEInvoiceProvider`).

## 9.2 Gửi email hóa đơn

- `spring-boot-starter-mail` + `spring-boot-starter-thymeleaf` (template
  `templates/invoice-email.html`). `OrderService.createOrder()` publish `InvoiceCreatedEvent` sau
  khi lưu `Invoice`; `InvoiceEmailListener` lắng nghe qua
  `@TransactionalEventListener(phase = AFTER_COMMIT)` — chỉ gửi **sau khi** transaction bán hàng
  đã commit thành công (D4: không gọi IO ngoài trong `@Transactional`); nếu đơn bị rollback trước
  khi commit, event không bao giờ được xử lý nên không gửi nhầm email cho đơn không tồn tại.
- Mọi lỗi (SMTP không kết nối được, template lỗi...) đều bắt và log lại ở cả
  `InvoiceEmailListener` lẫn `InvoiceEmailService` — không bao giờ ném ngược lên, vì lúc này
  transaction bán hàng đã xong, không thể/không nên ảnh hưởng ngược lại đơn đã bán.
- **Đã verify bằng SMTP server thật** (dựng nhanh 1 server SMTP debug bằng `python3 -m smtpd`
  ở `localhost:1025`, cấu hình `MAIL_HOST`/`MAIL_PORT` trỏ vào) — tạo đơn cho khách hàng có email,
  xác nhận email HTML đến đúng ngay sau khi API trả response (tức là chạy sau commit, không chặn
  luồng bán hàng), nội dung khớp chính xác: tên cửa hàng/chi nhánh, số hóa đơn, số đơn, thu ngân,
  dòng hàng, VAT theo từng thuế suất, tổng thanh toán, tiền thừa, mã tra cứu — tiếng Việt có dấu
  hiển thị đúng (quoted-printable, không bị lỗi encoding).

## 9.3 Frontend — template in K80/A4 + tra cứu QR

- `InvoiceK80`/`InvoiceA4` (React, `docs/conventions.md` D-nào-đó không quy định layout in nên tự
  thiết kế theo khổ giấy thực tế: K80 = 80mm nhiệt POS phổ biến, font monospace nhỏ; A4 = hóa đơn
  trang trọng có chữ ký). `InvoicePrintPage` (route bảo vệ `/invoices/:id/print`, quyền
  `invoice:view`) chuyển đổi khổ giấy qua tab, tiêm `<style>{"@page {...}"}</style>` động theo khổ
  đang chọn trước khi gọi `window.print()` — tránh xung đột vì CSS `@page` không thể scope theo
  class, chỉ có 1 khổ được "kích hoạt" tại một thời điểm.
- `InvoiceLookupPage` (route công khai `/tra-cuu/:code`, **không** bọc `RequireAuth`) — trang
  khách quét QR trên hóa đơn giấy để xem/in lại, gọi endpoint `lookup/{lookupCode}` công khai.
- QR code dùng `qrcode.react` (`QRCodeSVG`) mã hóa URL tra cứu (`/tra-cuu/{lookupCode}`) — hiện cả
  trên bill in (để khách quét) lẫn trên chính trang tra cứu (để khách chia sẻ lại).
- Backend chỉ trả JSON, toàn bộ layout/format in dựng ở FE (B3) — không có endpoint render
  HTML/PDF phía Backend.

## Đã verify bằng ứng dụng chạy thật (Playwright + Chromium + SMTP debug server thật)

| Kịch bản | Kết quả |
|---|---|
| Tạo khách hàng có email, tạo đơn cho khách đó | Email hóa đơn tự động gửi tới SMTP debug server ngay sau khi API trả response (đúng sau-commit) |
| `GET /invoices/{id}` (có quyền) | JSON đầy đủ, VAT breakdown đúng (taxable 18.182 + VAT 1.818 = tổng 20.000, khớp `totalAmount`) |
| `GET /invoices/{id}` không token | 401 |
| `GET /invoices/lookup/{code}` không đăng nhập | 200, JSON đầy đủ giống hệt endpoint có quyền |
| Trang `/invoices/:id/print`, khổ K80 | Hiện đúng bill 80mm, đủ dòng hàng/VAT/tổng/tiền thừa/QR |
| Chuyển tab sang khổ A4 | Hiện đúng layout A4 trang trọng, có chữ ký, khớp cùng dữ liệu |
| Trang `/tra-cuu/{code}` (context trình duyệt mới, không cookie) | Truy cập được, hiện đúng hóa đơn, không cần đăng nhập |
| `/tra-cuu/{code-sai}` | Hiện thông báo lỗi tiếng Việt từ Backend, không crash trang |

## Lệnh kiểm tra

```bash
cd server
mvn clean install
mvn spring-boot:run -Dspring-boot.run.profiles=local
# SMTP debug (tuỳ chọn, xem code trong lịch sử phiên làm việc): python3 -m smtpd -n -c DebuggingServer localhost:1025

cd client
npm run build
npm run dev
```
