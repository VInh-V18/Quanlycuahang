-- Bo sung CHECK constraint con thieu tren cot dang enum-like (da xac nhan du lieu hien tai khong
-- vi pham truoc khi viet migration nay) va index tren cac cot FK chua duoc danh index (Postgres
-- khong tu dong index cot FK, khac voi phia duoc tham chieu) - phat hien khi rieng soat schema.

ALTER TABLE purchase_orders
    ADD CONSTRAINT chk_purchase_orders_status CHECK (status IN ('completed'));

ALTER TABLE returns
    ADD CONSTRAINT chk_returns_refund_method
        CHECK (refund_method IS NULL OR refund_method IN ('cash', 'bank_transfer', 'card'));

ALTER TABLE debt_payments
    ADD CONSTRAINT chk_debt_payments_method
        CHECK (method IS NULL OR method IN ('cash', 'bank_transfer', 'card'));

ALTER TABLE promotions
    ADD CONSTRAINT chk_promotions_percentage_range
        CHECK (discount_type <> 'percentage' OR discount_value BETWEEN 0 AND 100);

ALTER TABLE vouchers
    ADD CONSTRAINT chk_vouchers_percentage_range
        CHECK (discount_type <> 'percentage' OR discount_value BETWEEN 0 AND 100);

ALTER TABLE vouchers
    ADD CONSTRAINT chk_vouchers_usage CHECK (used_count <= max_usage);

-- stock_take_items: khong duoc co 2 dong cung san pham trong 1 phieu kiem ke (logic duyet phieu
-- cong don theo product_id, dong trung se lam sai lech tinh toan chenh lech gap doi).
ALTER TABLE stock_take_items
    ADD CONSTRAINT uq_stock_take_items_take_product UNIQUE (stock_take_id, product_id);

-- product_units: khong duoc co 2 don vi tinh trung ten cho cung 1 san pham.
ALTER TABLE product_units
    ADD CONSTRAINT uq_product_units_product_unit UNIQUE (product_id, unit_name);

-- invoice_templates: chi duoc co toi da 1 mau mac dinh cho moi (tenant, branch).
CREATE UNIQUE INDEX uq_invoice_templates_default ON invoice_templates (tenant_id, branch_id)
    WHERE is_default AND deleted_at IS NULL;

-- Index cot FK con thieu (Postgres khong tu dong danh index ben tham chieu, khac PK ben duoc
-- tham chieu) - phuc vu bao cao "ai da lam gi" va khoa kiem tra khi UPDATE/DELETE bang cha.
CREATE INDEX idx_cash_transactions_created_by ON cash_transactions (created_by);
CREATE INDEX idx_debt_payments_created_by ON debt_payments (created_by);
CREATE INDEX idx_inventory_transactions_created_by ON inventory_transactions (created_by);
CREATE INDEX idx_parked_orders_created_by ON parked_orders (created_by);
CREATE INDEX idx_price_history_changed_by ON price_history (changed_by);
CREATE INDEX idx_purchase_orders_created_by ON purchase_orders (created_by);
CREATE INDEX idx_returns_created_by ON returns (created_by);
CREATE INDEX idx_stock_takes_created_by ON stock_takes (created_by);
CREATE INDEX idx_stock_takes_approved_by ON stock_takes (approved_by);
CREATE INDEX idx_invoice_templates_branch_id ON invoice_templates (branch_id);
CREATE INDEX idx_invoices_invoice_template_id ON invoices (invoice_template_id);
CREATE INDEX idx_orders_voucher_id ON orders (voucher_id);

-- inventory_batches.branch_id chi duoc index o vi tri thu 2 trong (product_id, branch_id) - query
-- "lo sap het han theo chi nhanh" (khong theo san pham cu the) khong tan dung duoc index hien co.
CREATE INDEX idx_inventory_batches_branch_id ON inventory_batches (branch_id);
