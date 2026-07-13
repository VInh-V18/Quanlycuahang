-- Chan dong ca (Shift.close) 2 lan dong thoi - khong co @Version truoc day, 2 request cung dong 1
-- ca co the cung doc status='open', request thu 2 am tham ghi de actualCash/discrepancy/note thay
-- vi bao loi ro rang - phat hien khi rieng soat, cung 1 lop bug da tung sua cho Inventory/Voucher.
ALTER TABLE shifts ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
