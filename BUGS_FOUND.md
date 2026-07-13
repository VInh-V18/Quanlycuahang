# BUGS_FOUND — phát sinh trong lúc viết test (Prompt #1, P0)

Theo quy định của roadmap (`docs/UPGRADE_ROADMAP.md`, phụ lục): lỗi phát hiện NGOÀI phạm vi
prompt đang làm thì ghi lại đây, KHÔNG tự ý sửa (trừ lỗi bảo mật thì vá ngay kèm test). Prompt #1
là "viết test cho code hiện có", nên các mục dưới đây là quan sát/rủi ro phát hiện được NHỜ viết
test, không phải lỗi tự nhảy vào sửa.

## Đã sửa NGAY (nằm trong phạm vi Prompt #1 — hạ tầng test, không phải business logic)

- `DebtPaymentRepository` chưa có phương thức truy vấn nào ngoài kế thừa `JpaRepository` — thêm
  `findByDebtIdIn(List<Long>)` để test đọc lại `DebtPayment` sau `recordPayment()`. Chỉ thêm 1
  finder method, không đổi hành vi Service nào.
- `client/src/pages/pos/PosPage.tsx`: hàm kẹp giá bán sửa tay (`Math.min(catalogPrice, Math.max(0,
  rawValue || 0))`) được tách thành `clampEditablePrice()` trong `client/src/lib/pos/pricing.ts` để
  test được độc lập — công thức giữ NGUYÊN, chỉ đổi vị trí (extract function), verify lại bằng
  `npx tsc -b --noEmit` (sạch) + toàn bộ 30 test Vitest vẫn xanh sau khi đổi.

## Rủi ro/nợ kỹ thuật phát hiện khi viết test (CHƯA sửa, để prompt sau)

1. **Danh sách service CHƯA có test tích hợp/đơn vị nào** (phát hiện qua báo cáo JaCoCo sau khi
   hoàn tất Prompt #1 — coverage dòng lệnh 0% ở các lớp này):
   `StockTakeService`, `InventoryService`, `ProductService`, `CategoryService`, `DashboardService`,
   `EmployeeService`, `AuthService`, `RoleService`, `ReportService`, `VietQrService`,
   `ParkedOrderService`, `TenantUserAdminService`, `TenantAdminService`, `PlatformAdminAuthService`,
   `PlatformAuditService`, `CustomerGroupService`, `SupplierService`, `CustomerService`,
   `VoucherService`, `InvoiceEmailService`, `BranchService`, `AuditLogService`. Đây là lý do
   coverage tầng service toàn bộ mới đạt ~37,8% dòng lệnh (xem PROJECT_STATE) — chưa đạt ngưỡng
   60% mục tiêu của roadmap. Đề xuất ưu tiên `AuthService`/`StockTakeService`/`InventoryService`
   trước (nghiệp vụ lõi + đã từng có bug thật ở các Phase trước) khi làm Prompt tiếp theo về test.

2. **`ShiftService.close()` không tự động rollback nếu tính `expectedCash` ném lỗi giữa chừng** —
   không phải bug (đã có `@Transactional` bọc đúng), chỉ là quan sát: `computeExpectedCash()` gọi
   4 query tuần tự không trong 1 lần round-trip, nên nếu 1 trong 4 bảng liên quan bị khoá lâu bởi
   giao dịch khác, `close()` có thể chờ lâu hơn cần thiết. Không ảnh hưởng tính đúng đắn, chỉ là cơ
   hội tối ưu hiệu năng (ngoài phạm vi P0 test coverage).

3. **`PurchaseOrderService.updateItemPrice()` chỉ điều chỉnh `debts[0]`** khi có nhiều hơn 1 khoản
   nợ ứng với cùng `reference_type='purchase_order'` + `reference_id` (dòng
   `Debt debt = debts.isEmpty() ? null : debts.get(0);`) — trên thực tế mỗi phiếu nhập chỉ tạo tối
   đa 1 `Debt` lúc `create()` nên hiện tại luôn đúng, nhưng đây là một giả định ngầm không được
   validate. Nếu sau này có luồng nào khác tạo thêm `Debt` thứ 2 cùng `reference_id` (hiện chưa có),
   logic này sẽ âm thầm bỏ qua khoản nợ thứ 2. Không sửa vì ngoài phạm vi Prompt #1 và hiện không
   có đường dẫn code nào tạo ra tình huống đó — ghi lại để cảnh giác khi thêm luồng nợ mới.

4. **`OrderServiceCreateOrderIT.concurrentCheckoutsOnLastUnitOfStockOnlyOneSucceeds()` log ra
   `HHH100501 StaleStateException` ở mức ERROR trong Hibernate** cho 4/5 luồng thua cuộc — đây là
   hành vi ĐÚNG và ĐÃ ĐƯỢC MONG ĐỢI (chính là cơ chế `@Version` optimistic lock đang hoạt động,
   test assert đúng 4 thất bại/1 thành công), không phải lỗi. Ghi lại vì nếu ai đó chỉ nhìn log CI
   thấy dòng `ERROR` sẽ tưởng nhầm là build có vấn đề — nên cân nhắc hạ level log riêng cho
   `org.hibernate.orm.jdbc.batch` trong `application-test.yml` ở prompt CI/CD sau (Prompt #9) để
   log CI sạch hơn, không phải sửa ngay bây giờ.

## Không tìm thấy lỗi bảo mật nào cần vá khẩn cấp trong phạm vi các service đã viết test lần này
(`OrderService`, `ReturnService`, `PurchaseOrderService`, `ShiftService`, `DebtService`) — tenant
isolation, IDOR (`requireShift`), pessimistic lock, và validate nghiệp vụ đều hoạt động đúng như
thiết kế khi test bằng dữ liệu/tình huống thật (không mock) qua Postgres/Redis thật.
