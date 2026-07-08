-- =====================================================================
-- V4__invoice_fields.sql
-- Bo sung du lieu can cho hoa don day du (Phase 9): tien khach dua/thua luu tren Order
-- (truoc day chi tinh luc tao don, khong luu nen khong the xem lai dung tung dong khi
-- in lai/tra cuu hoa don cu); email khach hang (gui hoa don qua email); thong tin cua hang
-- hien thi tren hoa don (ten thuong hieu chuoi cua hang, khac voi tung chi nhanh o bang branches).
-- =====================================================================

ALTER TABLE orders
    ADD COLUMN cash_received NUMERIC(15, 0),
    ADD COLUMN change_amount NUMERIC(15, 0);

ALTER TABLE customers
    ADD COLUMN email VARCHAR(255);

INSERT INTO settings (branch_id, key, value) VALUES
    (NULL, 'store_name', 'Cua hang Quan Ly'),
    (NULL, 'store_tax_code', '');
