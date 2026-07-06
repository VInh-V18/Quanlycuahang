-- =====================================================================
-- V1__init_schema.sql
-- Schema khoi tao cho ERP quan ly ban hang (Phase 3)
-- Quy uoc: snake_case, bang so nhieu, khoa ngoai <bang_so_it>_id (D1)
-- Tien te: NUMERIC(15,0) | phan tram: NUMERIC(5,2) | so luong: NUMERIC(12,3) (D5)
-- Timestamp: TIMESTAMPTZ, timezone Asia/Ho_Chi_Minh xu ly o tang Java (B3)
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- unaccent() mac dinh la STABLE (phu thuoc text search config theo search_path),
-- Postgres tu choi dung truc tiep trong index expression (yeu cau IMMUTABLE).
-- Wrapper nay ep dung 1 dictionary 'unaccent' co dinh — an toan de danh dau IMMUTABLE
-- vi dictionary nay khong thay doi luc runtime (B3).
CREATE OR REPLACE FUNCTION immutable_unaccent(text) RETURNS text AS $$
    SELECT unaccent('unaccent', $1)
$$ LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT;

-- =====================================================================
-- NHOM 1: HE THONG
-- =====================================================================

CREATE TABLE branches (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    phone VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_users_username UNIQUE (username)
);

CREATE TABLE roles (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_roles_code UNIQUE (code)
);

CREATE TABLE permissions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_permissions_code UNIQUE (code)
);

-- Bang join thuan tuy: khong Entity Java rieng, anh xa qua @ManyToMany + @JoinTable
CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL REFERENCES roles (id),
    permission_id BIGINT NOT NULL REFERENCES permissions (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users (id),
    role_id BIGINT NOT NULL REFERENCES roles (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE user_branches (
    user_id BIGINT NOT NULL REFERENCES users (id),
    branch_id BIGINT NOT NULL REFERENCES branches (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, branch_id)
);

CREATE TABLE settings (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    branch_id BIGINT REFERENCES branches (id),
    key VARCHAR(100) NOT NULL,
    value TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_settings_branch_key UNIQUE (branch_id, key)
);

CREATE TABLE audit_logs (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT REFERENCES users (id),
    branch_id BIGINT REFERENCES branches (id),
    action VARCHAR(100) NOT NULL,
    entity_name VARCHAR(100),
    entity_id BIGINT,
    before JSONB,
    after JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_branch_id ON audit_logs (branch_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);

-- =====================================================================
-- NHOM 2: SAN PHAM
-- =====================================================================

CREATE TABLE categories (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    parent_id BIGINT REFERENCES categories (id),
    name VARCHAR(255) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_categories_parent_id ON categories (parent_id);

CREATE TABLE products (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_id BIGINT REFERENCES categories (id),
    sku VARCHAR(50) NOT NULL,
    barcode VARCHAR(50),
    name VARCHAR(255) NOT NULL,
    unit VARCHAR(50) NOT NULL DEFAULT 'Cai',
    sell_price NUMERIC(15, 0) NOT NULL DEFAULT 0,
    price_includes_vat BOOLEAN NOT NULL DEFAULT TRUE,
    vat_rate NUMERIC(5, 2) NOT NULL DEFAULT 0,
    min_stock NUMERIC(12, 3) NOT NULL DEFAULT 0,
    image_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_products_sku UNIQUE (sku),
    CONSTRAINT uq_products_barcode UNIQUE (barcode),
    CONSTRAINT chk_products_vat_rate CHECK (vat_rate IN (0, 5, 8, 10))
);

CREATE INDEX idx_products_category_id ON products (category_id);
-- B3: tim khong dau + gan dung qua unaccent + pg_trgm (khong can cot name_normalized thu cong)
CREATE INDEX idx_products_name_trgm ON products USING GIN (immutable_unaccent(lower(name)) gin_trgm_ops);

CREATE TABLE product_units (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products (id),
    unit_name VARCHAR(50) NOT NULL,
    conversion_rate NUMERIC(12, 3) NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_product_units_product_id ON product_units (product_id);

CREATE TABLE price_history (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products (id),
    old_price NUMERIC(15, 0),
    new_price NUMERIC(15, 0) NOT NULL,
    changed_by BIGINT REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_price_history_product_id ON price_history (product_id);

-- =====================================================================
-- NHOM 5 (tao truoc vi Kho/Ban hang phu thuoc): DOI TAC
-- =====================================================================

CREATE TABLE customer_groups (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE TABLE customers (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_group_id BIGINT REFERENCES customer_groups (id),
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    address VARCHAR(500),
    debt_limit NUMERIC(15, 0) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_customers_phone UNIQUE (phone)
);

CREATE INDEX idx_customers_customer_group_id ON customers (customer_group_id);
CREATE INDEX idx_customers_name_trgm ON customers USING GIN (immutable_unaccent(lower(name)) gin_trgm_ops);

CREATE TABLE suppliers (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    address VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE TABLE debts (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id BIGINT REFERENCES customers (id),
    supplier_id BIGINT REFERENCES suppliers (id),
    direction VARCHAR(20) NOT NULL,
    amount NUMERIC(15, 0) NOT NULL,
    reference_type VARCHAR(30),
    reference_id BIGINT,
    note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_debts_direction CHECK (direction IN ('receivable', 'payable')),
    CONSTRAINT chk_debts_party CHECK (
        (direction = 'receivable' AND customer_id IS NOT NULL AND supplier_id IS NULL)
        OR (direction = 'payable' AND supplier_id IS NOT NULL AND customer_id IS NULL)
    )
);

CREATE INDEX idx_debts_customer_id ON debts (customer_id);
CREATE INDEX idx_debts_supplier_id ON debts (supplier_id);

CREATE TABLE debt_payments (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    debt_id BIGINT NOT NULL REFERENCES debts (id),
    amount NUMERIC(15, 0) NOT NULL,
    method VARCHAR(20),
    note VARCHAR(500),
    paid_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_debt_payments_debt_id ON debt_payments (debt_id);

-- =====================================================================
-- NHOM 3: KHO
-- =====================================================================

CREATE TABLE inventory (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products (id),
    branch_id BIGINT NOT NULL REFERENCES branches (id),
    stock NUMERIC(12, 3) NOT NULL DEFAULT 0,
    cost_price NUMERIC(15, 0) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_inventory_product_branch UNIQUE (product_id, branch_id)
);

CREATE INDEX idx_inventory_branch_id ON inventory (branch_id);

-- Xu ly mau thuan B4 (allow_negative_stock vs CHECK stock >= 0) — xem docs/phase3/erd.md.
-- Thay CHECK tinh bang TRIGGER doc cau hinh allow_negative_stock theo chi nhanh (hoac global).
CREATE OR REPLACE FUNCTION fn_check_inventory_stock() RETURNS TRIGGER AS $$
DECLARE
    v_allow_negative BOOLEAN;
BEGIN
    IF NEW.stock < 0 THEN
        SELECT COALESCE(
            (SELECT value::boolean FROM settings
                WHERE branch_id = NEW.branch_id AND key = 'allow_negative_stock' AND deleted_at IS NULL),
            (SELECT value::boolean FROM settings
                WHERE branch_id IS NULL AND key = 'allow_negative_stock' AND deleted_at IS NULL),
            FALSE
        ) INTO v_allow_negative;

        IF NOT v_allow_negative THEN
            RAISE EXCEPTION 'PRODUCT_OUT_OF_STOCK: khong du ton kho cho product_id=%, branch_id=%',
                NEW.product_id, NEW.branch_id
                USING ERRCODE = 'P0001';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_inventory_stock
    BEFORE INSERT OR UPDATE ON inventory
    FOR EACH ROW EXECUTE FUNCTION fn_check_inventory_stock();

CREATE TABLE inventory_transactions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products (id),
    branch_id BIGINT NOT NULL REFERENCES branches (id),
    type VARCHAR(30) NOT NULL,
    quantity NUMERIC(12, 3) NOT NULL,
    unit_cost NUMERIC(15, 0),
    reference_type VARCHAR(30),
    reference_id BIGINT,
    note VARCHAR(500),
    created_by BIGINT REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_inventory_tx_type CHECK (
        type IN ('purchase', 'sale', 'supplier_return', 'customer_return', 'stock_take', 'transfer', 'cancel')
    )
);

-- D4: composite bat buoc cho the kho
CREATE INDEX idx_inventory_tx_product_created ON inventory_transactions (product_id, created_at);
CREATE INDEX idx_inventory_tx_branch_id ON inventory_transactions (branch_id);

CREATE TABLE purchase_orders (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    supplier_id BIGINT NOT NULL REFERENCES suppliers (id),
    branch_id BIGINT NOT NULL REFERENCES branches (id),
    created_by BIGINT REFERENCES users (id),
    status VARCHAR(20) NOT NULL DEFAULT 'completed',
    total_amount NUMERIC(15, 0) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_purchase_orders_supplier_id ON purchase_orders (supplier_id);
CREATE INDEX idx_purchase_orders_branch_id ON purchase_orders (branch_id);

CREATE TABLE purchase_order_items (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    purchase_order_id BIGINT NOT NULL REFERENCES purchase_orders (id),
    product_id BIGINT NOT NULL REFERENCES products (id),
    quantity NUMERIC(12, 3) NOT NULL,
    unit_price NUMERIC(15, 0) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_purchase_order_items_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_purchase_order_items_po_id ON purchase_order_items (purchase_order_id);
CREATE INDEX idx_purchase_order_items_product_id ON purchase_order_items (product_id);

CREATE TABLE stock_takes (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    branch_id BIGINT NOT NULL REFERENCES branches (id),
    created_by BIGINT REFERENCES users (id),
    status VARCHAR(20) NOT NULL DEFAULT 'draft',
    approved_by BIGINT REFERENCES users (id),
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_stock_takes_status CHECK (status IN ('draft', 'approved'))
);

CREATE INDEX idx_stock_takes_branch_id ON stock_takes (branch_id);

CREATE TABLE stock_take_items (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    stock_take_id BIGINT NOT NULL REFERENCES stock_takes (id),
    product_id BIGINT NOT NULL REFERENCES products (id),
    expected_qty NUMERIC(12, 3) NOT NULL,
    actual_qty NUMERIC(12, 3),
    reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_stock_take_items_stock_take_id ON stock_take_items (stock_take_id);
CREATE INDEX idx_stock_take_items_product_id ON stock_take_items (product_id);

-- =====================================================================
-- NHOM 7 (tao truoc vi Ban hang phu thuoc shifts): VAN HANH (mot phan)
-- =====================================================================

CREATE TABLE shifts (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    branch_id BIGINT NOT NULL REFERENCES branches (id),
    opened_by BIGINT NOT NULL REFERENCES users (id),
    opening_cash NUMERIC(15, 0) NOT NULL DEFAULT 0,
    actual_cash NUMERIC(15, 0),
    discrepancy NUMERIC(15, 0),
    note VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'open',
    opened_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_shifts_status CHECK (status IN ('open', 'closed'))
);

CREATE INDEX idx_shifts_branch_id ON shifts (branch_id);

-- =====================================================================
-- NHOM 6: KHUYEN MAI (truoc Ban hang vi orders tham chieu voucher)
-- =====================================================================

CREATE TABLE promotions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    branch_id BIGINT REFERENCES branches (id),
    name VARCHAR(255) NOT NULL,
    discount_type VARCHAR(20) NOT NULL,
    discount_value NUMERIC(15, 2) NOT NULL,
    start_date TIMESTAMPTZ,
    end_date TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_promotions_discount_type CHECK (discount_type IN ('percentage', 'fixed_amount'))
);

CREATE TABLE vouchers (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    discount_type VARCHAR(20) NOT NULL,
    discount_value NUMERIC(15, 2) NOT NULL,
    min_order_amount NUMERIC(15, 0) NOT NULL DEFAULT 0,
    max_usage INT NOT NULL DEFAULT 1,
    used_count INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_vouchers_code UNIQUE (code),
    CONSTRAINT chk_vouchers_discount_type CHECK (discount_type IN ('percentage', 'fixed_amount'))
);

-- =====================================================================
-- NHOM 4: BAN HANG
-- =====================================================================

CREATE TABLE parked_orders (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    branch_id BIGINT NOT NULL REFERENCES branches (id),
    created_by BIGINT NOT NULL REFERENCES users (id),
    cart_snapshot JSONB NOT NULL,
    note VARCHAR(255),
    parked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_parked_orders_branch_id ON parked_orders (branch_id);

CREATE TABLE orders (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_number VARCHAR(30) NOT NULL,
    branch_id BIGINT NOT NULL REFERENCES branches (id),
    customer_id BIGINT REFERENCES customers (id),
    cashier_id BIGINT NOT NULL REFERENCES users (id),
    shift_id BIGINT REFERENCES shifts (id),
    voucher_id BIGINT REFERENCES vouchers (id),
    status VARCHAR(20) NOT NULL DEFAULT 'draft',
    subtotal_amount NUMERIC(15, 0) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(15, 0) NOT NULL DEFAULT 0,
    vat_amount NUMERIC(15, 0) NOT NULL DEFAULT 0,
    rounding_adjustment NUMERIC(15, 0) NOT NULL DEFAULT 0,
    total_amount NUMERIC(15, 0) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_orders_order_number UNIQUE (order_number),
    CONSTRAINT chk_orders_status CHECK (
        status IN ('draft', 'completed', 'partially_returned', 'fully_returned', 'cancelled')
    )
);

-- D4: composite bat buoc cho danh sach don + bao cao
CREATE INDEX idx_orders_created_branch_status ON orders (created_at, branch_id, status);
CREATE INDEX idx_orders_customer_id ON orders (customer_id);
CREATE INDEX idx_orders_cashier_id ON orders (cashier_id);
CREATE INDEX idx_orders_shift_id ON orders (shift_id);

CREATE TABLE order_items (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders (id),
    product_id BIGINT NOT NULL REFERENCES products (id),
    product_name_snapshot VARCHAR(255) NOT NULL,
    unit_price_snapshot NUMERIC(15, 0) NOT NULL,
    cost_price_snapshot NUMERIC(15, 0) NOT NULL,
    vat_rate_snapshot NUMERIC(5, 2) NOT NULL DEFAULT 0,
    quantity NUMERIC(12, 3) NOT NULL,
    discount_amount NUMERIC(15, 0) NOT NULL DEFAULT 0,
    vat_amount NUMERIC(15, 0) NOT NULL DEFAULT 0,
    line_total NUMERIC(15, 0) NOT NULL,
    returned_quantity NUMERIC(12, 3) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_product_id ON order_items (product_id);

CREATE TABLE order_payments (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders (id),
    method VARCHAR(20) NOT NULL,
    amount NUMERIC(15, 0) NOT NULL,
    qr_payload TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_order_payments_method CHECK (method IN ('cash', 'bank_transfer', 'card'))
);

CREATE INDEX idx_order_payments_order_id ON order_payments (order_id);

CREATE TABLE voucher_usages (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    voucher_id BIGINT NOT NULL REFERENCES vouchers (id),
    order_id BIGINT NOT NULL REFERENCES orders (id),
    used_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_voucher_usages_voucher_id ON voucher_usages (voucher_id);
CREATE INDEX idx_voucher_usages_order_id ON voucher_usages (order_id);

CREATE TABLE returns (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders (id),
    created_by BIGINT NOT NULL REFERENCES users (id),
    total_refund NUMERIC(15, 0) NOT NULL DEFAULT 0,
    refund_method VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_returns_order_id ON returns (order_id);

CREATE TABLE return_items (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    return_id BIGINT NOT NULL REFERENCES returns (id),
    order_item_id BIGINT NOT NULL REFERENCES order_items (id),
    quantity NUMERIC(12, 3) NOT NULL,
    refund_amount NUMERIC(15, 0) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_return_items_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_return_items_return_id ON return_items (return_id);
CREATE INDEX idx_return_items_order_item_id ON return_items (order_item_id);

-- =====================================================================
-- NHOM 7: VAN HANH (phan con lai — invoice phu thuoc orders)
-- =====================================================================

CREATE TABLE invoice_templates (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    branch_id BIGINT REFERENCES branches (id),
    paper_size VARCHAR(10) NOT NULL DEFAULT 'K80',
    template_config JSONB,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_invoice_templates_paper_size CHECK (paper_size IN ('K80', 'K58', 'A4'))
);

CREATE TABLE invoices (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders (id),
    invoice_number VARCHAR(30) NOT NULL,
    invoice_template_id BIGINT REFERENCES invoice_templates (id),
    qr_payload TEXT,
    lookup_code VARCHAR(20),
    issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_invoices_order_id UNIQUE (order_id),
    CONSTRAINT uq_invoices_invoice_number UNIQUE (invoice_number)
);

CREATE TABLE cash_transactions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    shift_id BIGINT NOT NULL REFERENCES shifts (id),
    type VARCHAR(20) NOT NULL,
    amount NUMERIC(15, 0) NOT NULL,
    note VARCHAR(500),
    created_by BIGINT REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_cash_transactions_type CHECK (type IN ('cash_in', 'cash_out'))
);

CREATE INDEX idx_cash_transactions_shift_id ON cash_transactions (shift_id);
