-- =====================================================================
-- V5__product_origin_and_batches.sql
-- FH-4: Xuat xu san pham (nhap khau trai cay) + lo/HSD theo tung dot nhap kho.
--
-- Batch la thong tin THAM KHAO cho canh bao can het han va hien thi "Lo gan nhat/HSD" o
-- Ton kho (docs mockup FruitHouse 07-ton-kho.png) - khong tru tru theo tung lo khi ban/kiem ke
-- (mockup 08-kiem-ke.png van kiem theo tong ton san pham, khong theo lo), nen inventory.stock
-- van la nguon su that duy nhat cho ton kho hien tai (giu nguyen tinh toan D5), inventory_batches
-- chi cong dan them (quantity nhap luc do, khong giam khi ban).
-- =====================================================================

ALTER TABLE products
    ADD COLUMN origin_country VARCHAR(100),
    ADD COLUMN origin_region VARCHAR(100);

CREATE TABLE inventory_batches (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products (id),
    branch_id BIGINT NOT NULL REFERENCES branches (id),
    purchase_order_item_id BIGINT REFERENCES purchase_order_items (id),
    batch_code VARCHAR(50) NOT NULL,
    expiry_date DATE,
    quantity NUMERIC(12, 3) NOT NULL,
    cost_price NUMERIC(15, 0) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_inventory_batches_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_inventory_batches_product_branch ON inventory_batches (product_id, branch_id);
CREATE INDEX idx_inventory_batches_expiry ON inventory_batches (expiry_date);
