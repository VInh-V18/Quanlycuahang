-- Chan race condition tang used_count dong thoi (2 don dung cung 1 voucher gan het luot cung luc
-- co the deu qua duoc kiem tra used_count < max_usage roi cung ghi de nhau, vuot qua gioi han that
-- su cho phep) — phat hien khi rieng soat, cung 1 lop bug da tung sua cho ton kho (Inventory.version).
ALTER TABLE vouchers ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
