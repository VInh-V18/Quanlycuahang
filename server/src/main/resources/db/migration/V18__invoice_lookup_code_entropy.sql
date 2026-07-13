-- lookup_code truoc day chi lay 8 ky tu dau cua UUID (32 bit entropy) va KHONG co unique constraint
-- - endpoint GET /invoices/lookup/{code} la CONG KHAI (khong dang nhap), bao mat hoan toan dua vao
-- do kho doan cua ma nay; 32 bit ngay cang de bi do trung/vet can khi tong so hoa don TOAN HE THONG
-- (moi tenant dung CHUNG 1 khong gian ma, vi tra cuu cong khai khong biet tenant nao de loc theo
-- @Filter) tang dan theo thoi gian (phat hien khi rieng soat bao mat). Mo rong cot de chua du UUID
-- day du (128 bit, xem OrderService) va them unique index lam lop chan cuoi - cac ma 8 ky tu cu con
-- lai van hop le, khong can migrate lai (UNIQUE index cho phep nhieu NULL cho hoa don rat cu chua
-- co lookup_code).
ALTER TABLE invoices ALTER COLUMN lookup_code TYPE VARCHAR(40);
CREATE UNIQUE INDEX uq_invoices_lookup_code ON invoices (lookup_code);
