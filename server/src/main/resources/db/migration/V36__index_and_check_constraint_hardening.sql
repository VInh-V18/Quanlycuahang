-- Va them index/CHECK constraint con thieu, phat hien qua audit production readiness (2026-07-17)
-- — mirror dung phong cach V24 (dong nhieu cai tien schema nho lien quan vao 1 migration, da xac
-- nhan hien tai KHONG co dong nao vi pham truoc khi viet migration nay), khong tach nhieu file nho.

-- 1) Index con thieu cho cap (tenant_id, reference_type, reference_id) — bi
-- OrderService.cancelOrder()/PurchaseOrderService.updateItemPrice() (doc truc tiep tung request)
-- va ReconciliationService.checkOrphanedReferences() (job dinh ky) quet full scan, chi loc duoc
-- theo tenant_id qua Hibernate @Filter/index don cot co san.
CREATE INDEX idx_debts_reference ON debts (tenant_id, reference_type, reference_id);
CREATE INDEX idx_inventory_tx_reference ON inventory_transactions (tenant_id, reference_type, reference_id);

-- 2) Index composite con thieu cho danh sach phan trang theo chi nhanh + sap xep theo thoi gian —
-- chi co index don cot tenant_id/branch_id rieng le (V13/V1), phai sap xep rieng khi du lieu lon
-- dan, giong dung ly do da vá cho "orders"/"audit_logs" o V19/V27.
CREATE INDEX idx_purchase_orders_tenant_branch_created ON purchase_orders (tenant_id, branch_id, created_at DESC);
CREATE INDEX idx_stock_takes_tenant_branch_created ON stock_takes (tenant_id, branch_id, created_at DESC);
CREATE INDEX idx_shifts_tenant_opened_by_status ON shifts (tenant_id, opened_by, status);

-- 3) Index don cot du thua — da duoc bao phu HOAN TOAN boi index composite moi hon (cung dan cot
-- dau), chi con ton chi phi ghi/dung luong tren 2 bang ghi nhieu nhat he thong.
DROP INDEX idx_orders_tenant_id;
DROP INDEX idx_audit_logs_tenant_id;
DROP INDEX idx_orders_created_branch_status;

-- 4) CHECK constraint chan so tien am (chi ap dung cho cot da xac nhan qua code Java hien tai
-- KHONG BAO GIO am theo dung nghiep vu — rieng orders.rounding_adjustment CO THE am do lam tron
-- xuong, nen KHONG dua vao day). Cot thanh toan (order_payments/debt_payments/cash_transactions)
-- da co @Positive o tang DTO tu truoc, dung > 0 khop dung; con lai dung >= 0 (vd debts.amount ve 0
-- khi da tra het/huy don, khong bao gio am).
ALTER TABLE orders
    ADD CONSTRAINT chk_orders_subtotal_amount_nonneg CHECK (subtotal_amount >= 0),
    ADD CONSTRAINT chk_orders_discount_amount_nonneg CHECK (discount_amount >= 0),
    ADD CONSTRAINT chk_orders_vat_amount_nonneg CHECK (vat_amount >= 0),
    ADD CONSTRAINT chk_orders_total_amount_nonneg CHECK (total_amount >= 0);

ALTER TABLE order_items
    ADD CONSTRAINT chk_order_items_unit_price_nonneg CHECK (unit_price_snapshot >= 0),
    ADD CONSTRAINT chk_order_items_cost_price_nonneg CHECK (cost_price_snapshot >= 0),
    ADD CONSTRAINT chk_order_items_discount_amount_nonneg CHECK (discount_amount >= 0),
    ADD CONSTRAINT chk_order_items_vat_amount_nonneg CHECK (vat_amount >= 0),
    ADD CONSTRAINT chk_order_items_line_total_nonneg CHECK (line_total >= 0);

ALTER TABLE order_payments
    ADD CONSTRAINT chk_order_payments_amount_positive CHECK (amount > 0);

ALTER TABLE returns
    ADD CONSTRAINT chk_returns_total_refund_nonneg CHECK (total_refund >= 0);

ALTER TABLE return_items
    ADD CONSTRAINT chk_return_items_refund_amount_nonneg CHECK (refund_amount >= 0);

ALTER TABLE debts
    ADD CONSTRAINT chk_debts_amount_nonneg CHECK (amount >= 0),
    ADD CONSTRAINT chk_debts_original_amount_nonneg CHECK (original_amount >= 0);

ALTER TABLE debt_payments
    ADD CONSTRAINT chk_debt_payments_amount_positive CHECK (amount > 0);

ALTER TABLE cash_transactions
    ADD CONSTRAINT chk_cash_transactions_amount_positive CHECK (amount > 0);

ALTER TABLE purchase_orders
    ADD CONSTRAINT chk_purchase_orders_total_amount_nonneg CHECK (total_amount >= 0);

ALTER TABLE purchase_order_items
    ADD CONSTRAINT chk_purchase_order_items_unit_price_nonneg CHECK (unit_price >= 0);

ALTER TABLE inventory
    ADD CONSTRAINT chk_inventory_cost_price_nonneg CHECK (cost_price >= 0);
