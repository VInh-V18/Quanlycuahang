# Phase 10 — Báo cáo

## ⚠️ Bug thật phát hiện qua `curl` xác nhận actuator health (không phải giả định)

Sau khi thêm `spring-boot-starter-mail` ở Phase 9, `GET /actuator/health` bắt đầu trả
`{"status":"DOWN"}` dù toàn bộ hệ thống (DB, Redis, web layer) vẫn hoạt động bình thường — chỉ vì
không có SMTP server nào đang lắng nghe ở `localhost:1025` lúc đó. Spring Boot tự động đăng ký 1
`MailHealthIndicator` khi thấy `spring-boot-starter-mail` trên classpath, và indicator này **góp
phần vào status tổng hợp** của `/actuator/health` mặc định — nghĩa là SMTP gián đoạn tạm thời (rất
phổ biến, SMTP relay thường kém ổn định hơn chính ứng dụng) sẽ khiến readiness/liveness probe
(Kubernetes, load balancer...) đánh giá cả instance là "DOWN" và có thể loại bỏ/khởi động lại một
tiến trình POS đang bán hàng hoàn toàn bình thường — mâu thuẫn trực tiếp với nguyên tắc đã chốt ở
Phase 9 ("gửi email hóa đơn không bao giờ được ảnh hưởng ngược lại luồng bán hàng").

**Đã sửa**: `management.health.mail.enabled: false` trong `application.yml` — tắt hẳn đóng góp của
mail indicator vào health tổng hợp (không xóa tính năng gửi email, chỉ tách nó ra khỏi định nghĩa
"hệ thống có đang hoạt động"). Verify: dừng SMTP debug server hoàn toàn, gọi lại
`/actuator/health` → `{"status":"UP"}` đúng như kỳ vọng.

## 10.1 Backend — bao cao tong hop

Tất cả bên dưới nằm ở package mới `report` (dto/service/controller/excel), dùng
`@Query(nativeQuery = true)` GROUP BY/`date_trunc()` trực tiếp trên
`orders`/`order_items`/`returns`/`debts`/`inventory` — **chưa** cần bảng tổng hợp
`daily_sales_summary` (`@Scheduled`) vì quy mô dữ liệu mục tiêu (500–20.000 SKU, 50–500 đơn/ngày)
chạy trực tiếp đủ nhanh; để ngỏ nếu sau này cần tối ưu.

| Endpoint | Quyền | Nội dung |
|---|---|---|
| `GET /reports/revenue?groupBy=day\|week\|month\|branch\|cashier` | `report:revenue` | Doanh thu nhóm theo 1 trong 5 chiều, cùng 1 shape `{label, revenue, orderCount}` cho FE dùng chung |
| `GET /reports/gross-profit` | `report:gross-profit` | `revenue − Σ(costPriceSnapshot × SL gốc) − Σ(totalRefund trong kỳ)` đúng công thức B4 |
| `GET /reports/top-products` | `report:revenue` | Top SP theo SL bán, join `order_items`/`products` |
| `GET /reports/top-customers` | `report:revenue` | Top KH theo doanh thu |
| `GET /reports/employee-performance` | `report:employee-performance` | Số đơn/doanh thu/giá trị TB mỗi đơn theo thu ngân |
| `GET /reports/inventory-value?groupBy=branch\|category` | `report:inventory-value` | Σ(stock × costPrice) nhóm theo chi nhánh hoặc danh mục |
| `GET /reports/debt-aging?direction=receivable\|payable` | `debt:view` | Tổng công nợ còn dư theo 4 mức tuổi nợ chuẩn (0-30/31-60/61-90/>90 ngày, tính từ `created_at` đến hiện tại) |
| `GET /reports/{revenue,top-products,employee-performance,inventory-value}/export` | quyền tương ứng **và** `report:export` | Xuất `.xlsx` qua `ReportExcelExporter` (Apache POI) dùng chung 1 hàm export cho mọi loại bảng |

Quy ước ngày giờ: mọi bộ lọc `from`/`to` (LocalDate) quy đổi sang mốc đầu ngày Asia/Ho_Chi_Minh
trước khi so sánh với `created_at` (TIMESTAMPTZ lưu UTC) — nhóm theo ngày/tuần/tháng cũng
`date_trunc(..., created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')` để không lệch ngày so với giờ VN.

Trạng thái đơn tính vào doanh thu: `completed`, `partially_returned`, `fully_returned` — loại
`draft`/`cancelled`. Lợi nhuận gộp trừ COGS theo **số lượng bán gốc** (không trừ `returnedQuantity`)
và trừ riêng "ảnh hưởng hoàn trả" = tổng `totalRefund` phát sinh trong kỳ (theo ngày tạo phiếu trả,
không phải ngày bán gốc) — đúng 2 số hạng tách biệt như công thức B4 gốc, không gộp chung.

**Nợ kỹ thuật đã ghi nhận**: "Công nợ kèm tuổi nợ" hiện chỉ trả **tổng hợp theo mức tuổi nợ**
(không phải danh sách từng khách hàng/NCC kèm tuổi nợ riêng) vì module CRUD Công nợ (duyệt/xem
từng khoản) chưa được xây (`DebtController` chưa tồn tại) — nằm ngoài phạm vi "Báo cáo", thuộc về
module Công nợ riêng khi được yêu cầu.

## 10.2 Frontend — `ReportsPage`

- Biểu đồ cột doanh thu (Recharts 2.x, 1 chuỗi dữ liệu — không cần chú giải theo đúng nguyên tắc
  dataviz, chỉ 1 màu thương hiệu `hsl(217, 91%, 60%)` khớp token `--primary` đã dùng xuyên suốt
  app), đổi chiều nhóm qua `Select`, có tooltip hover.
- Thẻ tóm tắt lợi nhuận gộp (4 số liệu: doanh thu/giá vốn/ảnh hưởng hoàn trả/lợi nhuận gộp).
- Bảng `DataTable` (tái dùng nguyên component Phase 5) cho top sản phẩm/top khách hàng/hiệu suất
  nhân viên/giá trị tồn kho/công nợ theo tuổi nợ — chuyển đổi nhóm qua `Tabs`.
- Nút "Xuất Excel": endpoint export yêu cầu header `Authorization` nên không dùng được `<a href>`
  trực tiếp — tải qua `apiClient` (đã có interceptor gắn token) dạng `responseType: "blob"`, tạo
  Blob URL tạm để kích hoạt tải xuống trình duyệt.
- **Nợ kỹ thuật**: chưa có bộ lọc chi nhánh trên UI vì chưa có endpoint danh sách chi nhánh
  (`BranchController` — nợ đã ghi từ Phase 8); mọi báo cáo hiện mặc định tính trên toàn bộ
  (`branchId` không truyền).

## Đã verify bằng ứng dụng chạy thật (không chỉ đọc code)

| Kịch bản | Kết quả |
|---|---|
| Doanh thu theo chi nhánh + theo thu ngân | Tổng khớp nhau tuyệt đối (2.525.000đ, 26 đơn) và khớp `SELECT ... GROUP BY status` trực tiếp trên DB — đúng loại trừ 1 đơn `cancelled` |
| Lợi nhuận gộp | `2.525.000 − 1.775.200 − 18.533 = 731.267` — COGS và return impact đối chiếu khớp tuyệt đối truy vấn SQL tay |
| Giá trị tồn kho theo danh mục | Tổng 5 danh mục cộng lại khớp chính xác tổng theo chi nhánh (80.257.115đ) |
| Phân quyền | `cashier01` gọi `/reports/revenue` → `403 PERMISSION_DENIED` đúng ma trận quyền |
| Xuất Excel (`revenue`) | File `.xlsx` thật (Apache POI), mở bằng `openpyxl` xác nhận đúng header + dữ liệu, tổng cộng dồn khớp `2.525.000` |
| `ReportsPage` (Playwright + Chromium) | Biểu đồ/bảng/tab chuyển nhóm đều hoạt động đúng với dữ liệu thật; nút "Xuất Excel" tải file thật từ trình duyệt (không chỉ qua curl) |
| `/actuator/health` sau khi tắt mail indicator | `UP` dù không có SMTP server nào chạy |

## Lệnh kiểm tra

```bash
cd server
mvn clean install
mvn spring-boot:run -Dspring-boot.run.profiles=local

cd client
npm run build
npm run dev
```
