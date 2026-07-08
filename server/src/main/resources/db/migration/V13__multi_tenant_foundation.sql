-- Chuyen tu single-tenant (1 lan trien khai = 1 doanh nghiep) sang multi-tenant (1 he thong phuc vu
-- nhieu cua hang, kieu KiotViet). Them 1 tang Tenant o TREN Branch hien co:
--   Tenant (1 doanh nghiep) -> Branch (nhieu chi nhanh, khong doi so voi hien tai) -> moi thu con lai
-- roles/permissions/role_permissions GIU NGUYEN global (dinh nghia quyen dung chung moi tenant, chi
-- User moi gan tenant cu the). platform_admins la bang/luong dang nhap TACH BIET hoan toan users,
-- chi de tao/quan ly tenant (khong phai nguoi dung trong 1 cua hang cu the).

CREATE TABLE tenants (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE TABLE platform_admins (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_platform_admins_username UNIQUE (username)
);

-- Tenant #1 = dai dien toan bo du lieu that dang co (cua hang dang chay hien tai) - lay dung ten
-- cua hang da tu cau hinh qua Cai dat lam ten tenant, khong bia ten moi.
INSERT INTO tenants (name)
VALUES (COALESCE(NULLIF((SELECT value FROM settings WHERE branch_id IS NULL AND key = 'store_name'), ''), 'Cửa hàng 1'));

-- Them tenant_id (nullable truoc, backfill ve tenant #1, roi moi bat buoc + rang buoc khoa ngoai +
-- index) vao TOAN BO bang nghiep vu - dung 1 vong lap de tranh sai sot khi lap lai 32 lan bang tay.
DO $$
DECLARE
    tbl TEXT;
    business_tables TEXT[] := ARRAY[
        'audit_logs', 'branches', 'cash_transactions', 'categories', 'customer_groups', 'customers',
        'debt_payments', 'debts', 'inventory', 'inventory_batches', 'inventory_transactions',
        'invoice_templates', 'invoices', 'order_items', 'order_payments', 'orders', 'parked_orders',
        'price_history', 'product_units', 'products', 'promotions', 'purchase_order_items',
        'purchase_orders', 'return_items', 'returns', 'settings', 'shifts', 'stock_take_items',
        'stock_takes', 'suppliers', 'users', 'voucher_usages', 'vouchers'
    ];
BEGIN
    FOREACH tbl IN ARRAY business_tables LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN tenant_id BIGINT', tbl);
        EXECUTE format('UPDATE %I SET tenant_id = 1', tbl);
        EXECUTE format('ALTER TABLE %I ALTER COLUMN tenant_id SET NOT NULL', tbl);
        EXECUTE format(
            'ALTER TABLE %I ADD CONSTRAINT fk_%s_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)',
            tbl, tbl);
        EXECUTE format('CREATE INDEX idx_%s_tenant_id ON %I (tenant_id)', tbl, tbl);
    END LOOP;
END $$;

-- Sua lai cac rang buoc UNIQUE dang toan cuc thanh UNIQUE THEO TUNG TENANT - neu khong, tenant #2 se
-- khong the them khach hang trung SDT/san pham trung SKU-barcode/don hang so 1 voi bat ky tenant nao
-- khac tung co du lieu do, du 2 ben khong lien quan gi nhau.
ALTER TABLE customers DROP CONSTRAINT uq_customers_phone;
ALTER TABLE customers ADD CONSTRAINT uq_customers_tenant_phone UNIQUE (tenant_id, phone);

ALTER TABLE products DROP CONSTRAINT uq_products_sku;
ALTER TABLE products ADD CONSTRAINT uq_products_tenant_sku UNIQUE (tenant_id, sku);

ALTER TABLE products DROP CONSTRAINT uq_products_barcode;
ALTER TABLE products ADD CONSTRAINT uq_products_tenant_barcode UNIQUE (tenant_id, barcode);

ALTER TABLE orders DROP CONSTRAINT uq_orders_order_number;
ALTER TABLE orders ADD CONSTRAINT uq_orders_tenant_order_number UNIQUE (tenant_id, order_number);

ALTER TABLE invoices DROP CONSTRAINT uq_invoices_invoice_number;
ALTER TABLE invoices ADD CONSTRAINT uq_invoices_tenant_invoice_number UNIQUE (tenant_id, invoice_number);

ALTER TABLE vouchers DROP CONSTRAINT uq_vouchers_code;
ALTER TABLE vouchers ADD CONSTRAINT uq_vouchers_tenant_code UNIQUE (tenant_id, code);

ALTER TABLE settings DROP CONSTRAINT uq_settings_branch_key;
ALTER TABLE settings ADD CONSTRAINT uq_settings_tenant_branch_key UNIQUE (tenant_id, branch_id, key);

-- users.username va roles/permissions.code CO CHU DINH giu nguyen duy nhat TOAN HE THONG (khong
-- theo tenant) - dang nhap van chi bang username/password, he thong tu suy ra tenant tu tai khoan.
