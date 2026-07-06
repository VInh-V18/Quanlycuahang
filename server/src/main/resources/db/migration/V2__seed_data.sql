-- =====================================================================
-- V2__seed_data.sql
-- Du lieu seed cho moi truong local/demo (Phase 3.5)
-- Mat khau seed cho ca 3 user: "Password@123" (BCrypt strength 12)
-- =====================================================================

SELECT setseed(0.42);

-- =====================================================================
-- 1 chi nhanh
-- =====================================================================
INSERT INTO branches (name, address, phone) VALUES
    ('Chi nhanh Quan 1', '12 Nguyen Hue, Quan 1, TP.HCM', '0281234567');

-- Cau hinh mac dinh (global, branch_id = NULL)
INSERT INTO settings (branch_id, key, value) VALUES
    (NULL, 'allow_negative_stock', 'false'),
    (NULL, 'price_includes_vat_default', 'true'),
    (NULL, 'rounding_unit', '1000'),
    (NULL, 'debt_limit_default', '5000000'),
    (NULL, 'sku_prefix', 'SP-'),
    (NULL, 'order_number_prefix', 'HD-'),
    (NULL, 'invoice_number_prefix', 'INV-');

-- =====================================================================
-- 6 vai tro (dung ma tran Phase 1.1)
-- =====================================================================
INSERT INTO roles (code, display_name) VALUES
    ('owner', 'Chu cua hang'),
    ('manager', 'Quan ly'),
    ('cashier', 'Thu ngan'),
    ('sales_staff', 'Nhan vien ban hang'),
    ('warehouse_staff', 'Nhan vien kho'),
    ('accountant', 'Ke toan');

-- =====================================================================
-- 51 quyen (resource:action) — dung nguon docs/phase1/permission-matrix.md
-- =====================================================================
INSERT INTO permissions (code, description) VALUES
    ('employee:view', 'Xem danh sach nhan vien'),
    ('employee:create', 'Tao nhan vien moi'),
    ('employee:update', 'Sua thong tin nhan vien'),
    ('employee:delete', 'Xoa (vo hieu hoa) nhan vien'),
    ('employee:manage-permission', 'Doi vai tro/quyen cua nhan vien'),
    ('branch:view', 'Xem danh sach chi nhanh'),
    ('branch:manage', 'Them/sua chi nhanh'),
    ('settings:view', 'Xem cau hinh he thong'),
    ('settings:update', 'Sua cau hinh'),
    ('audit-log:view', 'Xem nhat ky audit'),
    ('product:view', 'Xem san pham'),
    ('product:create', 'Tao san pham'),
    ('product:update', 'Sua san pham'),
    ('product:delete', 'Xoa (soft delete) san pham'),
    ('category:view', 'Xem danh muc'),
    ('category:create', 'Tao danh muc'),
    ('category:update', 'Sua danh muc'),
    ('category:delete', 'Xoa danh muc'),
    ('order:view', 'Xem don hang'),
    ('order:create', 'Tao don (ban hang)'),
    ('order:void', 'Huy don da hoan tat'),
    ('order:park', 'Treo/mo lai don'),
    ('return:view', 'Xem phieu tra hang'),
    ('return:create', 'Tao phieu tra hang'),
    ('promotion:view', 'Xem khuyen mai/voucher'),
    ('promotion:manage', 'Tao/sua khuyen mai/voucher'),
    ('inventory:view', 'Xem ton kho, the kho'),
    ('purchase-order:view', 'Xem phieu nhap'),
    ('purchase-order:create', 'Tao phieu nhap kho'),
    ('purchase-order:return', 'Tao phieu tra hang NCC'),
    ('stock-take:view', 'Xem phieu kiem ke'),
    ('stock-take:create', 'Tao phieu kiem ke'),
    ('stock-take:approve', 'Duyet phieu can bang chenh lech'),
    ('customer:view', 'Xem khach hang'),
    ('customer:create', 'Tao khach hang'),
    ('customer:update', 'Sua khach hang'),
    ('supplier:view', 'Xem nha cung cap'),
    ('supplier:manage', 'Tao/sua nha cung cap'),
    ('debt:view', 'Xem cong no KH/NCC'),
    ('debt:collect-payment', 'Ghi nhan thanh toan cong no'),
    ('shift:open', 'Mo ca'),
    ('shift:close', 'Dong ca'),
    ('shift:view', 'Xem lich su ca'),
    ('cash-transaction:create', 'Ghi thu/chi tien mat ngoai don'),
    ('invoice:view', 'Xem/in hoa don'),
    ('invoice:send-email', 'Gui hoa don qua email'),
    ('report:revenue', 'Bao cao doanh thu'),
    ('report:gross-profit', 'Bao cao loi nhuan gop'),
    ('report:inventory-value', 'Bao cao gia tri ton kho'),
    ('report:employee-performance', 'Hieu suat nhan vien'),
    ('report:export', 'Xuat Excel/PDF');

-- owner: toan bo 51 quyen
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.code = 'owner';

-- manager: tat ca tru 4 quyen chi owner (employee:delete, employee:manage-permission, branch:manage, settings:update)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'manager'
    AND p.code NOT IN ('employee:delete', 'employee:manage-permission', 'branch:manage', 'settings:update');

-- cashier
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'product:view', 'category:view',
    'order:view', 'order:create', 'order:park', 'return:view', 'return:create', 'promotion:view',
    'customer:view', 'customer:create', 'debt:collect-payment',
    'shift:open', 'shift:close', 'shift:view', 'cash-transaction:create', 'invoice:view', 'invoice:send-email'
)
WHERE r.code = 'cashier';

-- sales_staff
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'product:view', 'category:view',
    'order:view', 'order:create', 'order:park', 'return:view', 'return:create', 'promotion:view',
    'customer:view', 'customer:create', 'customer:update',
    'invoice:view', 'invoice:send-email'
)
WHERE r.code = 'sales_staff';

-- warehouse_staff
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'product:view', 'product:create', 'category:view',
    'inventory:view', 'purchase-order:view', 'purchase-order:create', 'purchase-order:return',
    'stock-take:view', 'stock-take:create',
    'supplier:view', 'supplier:manage',
    'report:inventory-value'
)
WHERE r.code = 'warehouse_staff';

-- accountant
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'product:view', 'category:view',
    'order:view', 'return:view', 'promotion:view',
    'inventory:view', 'purchase-order:view', 'stock-take:view',
    'customer:view', 'supplier:view', 'debt:view', 'debt:collect-payment',
    'shift:view',
    'report:revenue', 'report:gross-profit', 'report:inventory-value', 'report:employee-performance', 'report:export'
)
WHERE r.code = 'accountant';

-- =====================================================================
-- 3 user (mat khau: Password@123, BCrypt strength 12)
-- =====================================================================
INSERT INTO users (username, password_hash, full_name, phone) VALUES
    ('owner01', '$2b$12$xt1u.QNjFBv8dO6nyLCSsOS5DToHep7U5ZKoUXl8Cfaf2V6pJaPgG', 'Nguyen Van Chu', '0901000001'),
    ('manager01', '$2b$12$xt1u.QNjFBv8dO6nyLCSsOS5DToHep7U5ZKoUXl8Cfaf2V6pJaPgG', 'Tran Thi Quan Ly', '0901000002'),
    ('cashier01', '$2b$12$xt1u.QNjFBv8dO6nyLCSsOS5DToHep7U5ZKoUXl8Cfaf2V6pJaPgG', 'Le Van Thu Ngan', '0901000003');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON
    (u.username = 'owner01' AND r.code = 'owner') OR
    (u.username = 'manager01' AND r.code = 'manager') OR
    (u.username = 'cashier01' AND r.code = 'cashier');

INSERT INTO user_branches (user_id, branch_id)
SELECT u.id, b.id FROM users u CROSS JOIN branches b;

-- =====================================================================
-- 5 danh muc
-- =====================================================================
INSERT INTO categories (name, display_order) VALUES
    ('Do uong', 1),
    ('Banh keo', 2),
    ('Gia vi & Thuc pham kho', 3),
    ('Hoa my pham', 4),
    ('Do gia dung', 5);

-- =====================================================================
-- 30 san pham co barcode (6 san pham / danh muc)
-- =====================================================================
INSERT INTO products (category_id, sku, barcode, name, unit, sell_price, price_includes_vat, vat_rate, min_stock) VALUES
    ((SELECT id FROM categories WHERE name = 'Do uong'), 'SP-000001', '8934567000011', 'Ca phe hoa tan G7 hop 20 goi', 'Hop', 55000, TRUE, 10, 10),
    ((SELECT id FROM categories WHERE name = 'Do uong'), 'SP-000002', '8934567000028', 'Tra xanh khong do Lipton chai 450ml', 'Chai', 12000, TRUE, 10, 20),
    ((SELECT id FROM categories WHERE name = 'Do uong'), 'SP-000003', '8934567000035', 'Nuoc suoi Lavie chai 500ml', 'Chai', 6000, TRUE, 10, 30),
    ((SELECT id FROM categories WHERE name = 'Do uong'), 'SP-000004', '8934567000042', 'Coca-Cola lon 330ml', 'Lon', 10000, TRUE, 10, 30),
    ((SELECT id FROM categories WHERE name = 'Do uong'), 'SP-000005', '8934567000059', 'Sua tuoi Vinamilk hop 1 lit', 'Hop', 32000, TRUE, 5, 15),
    ((SELECT id FROM categories WHERE name = 'Do uong'), 'SP-000006', '8934567000066', 'Nuoc tang luc Red Bull lon 250ml', 'Lon', 11000, TRUE, 10, 20),
    ((SELECT id FROM categories WHERE name = 'Banh keo'), 'SP-000007', '8934567000073', 'Banh Oreo goi 137g', 'Goi', 22000, TRUE, 10, 15),
    ((SELECT id FROM categories WHERE name = 'Banh keo'), 'SP-000008', '8934567000080', 'Keo Alpenliebe hop 100 vien', 'Hop', 35000, TRUE, 10, 10),
    ((SELECT id FROM categories WHERE name = 'Banh keo'), 'SP-000009', '8934567000097', 'Snack Oishi goi 40g', 'Goi', 8000, TRUE, 10, 25),
    ((SELECT id FROM categories WHERE name = 'Banh keo'), 'SP-000010', '8934567000103', 'Banh quy Cosy hop 462g', 'Hop', 45000, TRUE, 10, 10),
    ((SELECT id FROM categories WHERE name = 'Banh keo'), 'SP-000011', '8934567000110', 'Chocopie Orion hop 12 cai', 'Hop', 68000, TRUE, 10, 8),
    ((SELECT id FROM categories WHERE name = 'Banh keo'), 'SP-000012', '8934567000127', 'Keo cao su Doublemint vi 10 thanh', 'Vi', 6000, TRUE, 10, 20),
    ((SELECT id FROM categories WHERE name = 'Gia vi & Thuc pham kho'), 'SP-000013', '8934567000134', 'Nuoc mam Nam Ngu chai 500ml', 'Chai', 35000, TRUE, 5, 12),
    ((SELECT id FROM categories WHERE name = 'Gia vi & Thuc pham kho'), 'SP-000014', '8934567000141', 'Duong cat trang Bien Hoa 1kg', 'Goi', 25000, TRUE, 5, 15),
    ((SELECT id FROM categories WHERE name = 'Gia vi & Thuc pham kho'), 'SP-000015', '8934567000158', 'Dau an Neptune chai 1 lit', 'Chai', 52000, TRUE, 5, 10),
    ((SELECT id FROM categories WHERE name = 'Gia vi & Thuc pham kho'), 'SP-000016', '8934567000165', 'Gao ST25 tui 5kg', 'Tui', 145000, TRUE, 0, 8),
    ((SELECT id FROM categories WHERE name = 'Gia vi & Thuc pham kho'), 'SP-000017', '8934567000172', 'Mi tom Hao Hao thung 30 goi', 'Thung', 120000, TRUE, 10, 6),
    ((SELECT id FROM categories WHERE name = 'Gia vi & Thuc pham kho'), 'SP-000018', '8934567000189', 'Muoi i-ot goi 500g', 'Goi', 8000, TRUE, 0, 20),
    ((SELECT id FROM categories WHERE name = 'Hoa my pham'), 'SP-000019', '8934567000196', 'Dau goi Clear chai 630ml', 'Chai', 98000, TRUE, 10, 10),
    ((SELECT id FROM categories WHERE name = 'Hoa my pham'), 'SP-000020', '8934567000202', 'Sua tam Dove chai 500ml', 'Chai', 89000, TRUE, 10, 10),
    ((SELECT id FROM categories WHERE name = 'Hoa my pham'), 'SP-000021', '8934567000219', 'Kem danh rang P/S ong 200g', 'Ong', 32000, TRUE, 10, 15),
    ((SELECT id FROM categories WHERE name = 'Hoa my pham'), 'SP-000022', '8934567000226', 'Nuoc rua chen Sunlight chai 750ml', 'Chai', 28000, TRUE, 10, 12),
    ((SELECT id FROM categories WHERE name = 'Hoa my pham'), 'SP-000023', '8934567000233', 'Bot giat Omo tui 3kg', 'Tui', 135000, TRUE, 10, 8),
    ((SELECT id FROM categories WHERE name = 'Hoa my pham'), 'SP-000024', '8934567000240', 'Khan giay Pulppy hop 3 goi', 'Hop', 42000, TRUE, 10, 10),
    ((SELECT id FROM categories WHERE name = 'Do gia dung'), 'SP-000025', '8934567000257', 'Pin Con Tho AA vi 4 vien', 'Vi', 25000, TRUE, 10, 10),
    ((SELECT id FROM categories WHERE name = 'Do gia dung'), 'SP-000026', '8934567000264', 'Bong den LED 9W', 'Cai', 35000, TRUE, 10, 15),
    ((SELECT id FROM categories WHERE name = 'Do gia dung'), 'SP-000027', '8934567000271', 'Tui rac tu huy 60x70cm cuon 20 cai', 'Cuon', 18000, TRUE, 10, 15),
    ((SELECT id FROM categories WHERE name = 'Do gia dung'), 'SP-000028', '8934567000288', 'Bat nhua Duy Tan bo 6 cai', 'Bo', 55000, TRUE, 10, 6),
    ((SELECT id FROM categories WHERE name = 'Do gia dung'), 'SP-000029', '8934567000295', 'Bat ve sinh da nang', 'Cai', 15000, TRUE, 10, 10),
    ((SELECT id FROM categories WHERE name = 'Do gia dung'), 'SP-000030', '8934567000301', 'Bang keo trong 5cm cuon', 'Cuon', 9000, TRUE, 10, 20);

-- =====================================================================
-- Ton kho dau ky: 100 don vi moi san pham tai chi nhanh, gia von = 70% gia ban
-- (ghi nhan qua inventory_transactions loai purchase, reference_type = opening_balance)
-- =====================================================================
INSERT INTO inventory (product_id, branch_id, stock, cost_price)
SELECT p.id, b.id, 100, ROUND(p.sell_price * 0.7)
FROM products p CROSS JOIN branches b;

INSERT INTO inventory_transactions (product_id, branch_id, type, quantity, unit_cost, reference_type, created_by)
SELECT p.id, b.id, 'purchase', 100, ROUND(p.sell_price * 0.7), 'opening_balance',
       (SELECT id FROM users WHERE username = 'owner01')
FROM products p CROSS JOIN branches b;

-- =====================================================================
-- 5 khach hang, 3 nha cung cap
-- =====================================================================
INSERT INTO customer_groups (name) VALUES ('Khach le'), ('Khach than thiet');

INSERT INTO customers (customer_group_id, name, phone, address, debt_limit) VALUES
    ((SELECT id FROM customer_groups WHERE name = 'Khach than thiet'), 'Nguyen Thi Anh', '0912000001', '10 Le Loi, Q1', 3000000),
    ((SELECT id FROM customer_groups WHERE name = 'Khach le'), 'Tran Van Binh', '0912000002', '22 Hai Ba Trung, Q1', 2000000),
    ((SELECT id FROM customer_groups WHERE name = 'Khach than thiet'), 'Le Thi Cuc', '0912000003', '5 Pasteur, Q3', 5000000),
    ((SELECT id FROM customer_groups WHERE name = 'Khach le'), 'Pham Van Duc', '0912000004', '18 Vo Van Tan, Q3', 2000000),
    ((SELECT id FROM customer_groups WHERE name = 'Khach le'), 'Hoang Thi Em', '0912000005', '30 Dien Bien Phu, Binh Thanh', 2000000);

INSERT INTO suppliers (name, phone, address) VALUES
    ('Cong ty TNHH Phan phoi Thuc pham An Gia', '0281112222', '100 Truong Chinh, Tan Binh'),
    ('Cong ty CP Hang tieu dung Minh Phat', '0281113333', '55 Cong Hoa, Tan Binh'),
    ('Cong ty TNHH Thuong mai Viet Hung', '0281114444', '8 Le Trong Tan, Tan Phu');

-- =====================================================================
-- 20 don mau rai 30 ngay (sinh tu dong bang PL/pgSQL de dam bao tinh tien nhat quan)
-- Kich ban don gian: khong CK/voucher, gia da gom VAT, thanh toan tien mat du.
-- =====================================================================
DO $$
DECLARE
    v_branch_id BIGINT := (SELECT id FROM branches LIMIT 1);
    v_cashier_id BIGINT := (SELECT id FROM users WHERE username = 'cashier01');
    v_order_id BIGINT;
    v_product RECORD;
    v_qty NUMERIC(12,3);
    v_line_total NUMERIC(15,0);
    v_vat_amount NUMERIC(15,0);
    v_line_vat NUMERIC(15,0);
    v_order_total NUMERIC(15,0);
    v_order_vat NUMERIC(15,0);
    v_created_at TIMESTAMPTZ;
    v_num_items INT;
    i INT;
    j INT;
    v_product_id BIGINT;
    v_cost_price NUMERIC(15,0);
BEGIN
    FOR i IN 1..20 LOOP
        v_created_at := now() - ((30 - (i * 1.5))::int || ' days')::interval
                                - (random() * 8)::int * interval '1 hour';
        v_order_total := 0;
        v_order_vat := 0;
        v_num_items := 1 + floor(random() * 2)::int; -- 1 hoac 2 dong

        INSERT INTO orders (order_number, branch_id, cashier_id, status, created_at, updated_at)
        VALUES ('HD-' || lpad(i::text, 6, '0'), v_branch_id, v_cashier_id, 'completed', v_created_at, v_created_at)
        RETURNING id INTO v_order_id;

        FOR j IN 1..v_num_items LOOP
            SELECT id, sell_price, vat_rate INTO v_product
            FROM products
            ORDER BY random()
            LIMIT 1;

            v_qty := 1 + floor(random() * 3)::int; -- 1..3
            v_line_total := v_product.sell_price * v_qty;
            v_line_vat := ROUND(v_line_total * v_product.vat_rate / (100 + v_product.vat_rate));

            SELECT cost_price INTO v_cost_price FROM inventory
            WHERE product_id = v_product.id AND branch_id = v_branch_id;

            INSERT INTO order_items (
                order_id, product_id, product_name_snapshot, unit_price_snapshot,
                cost_price_snapshot, vat_rate_snapshot, quantity, discount_amount,
                vat_amount, line_total, created_at, updated_at
            )
            SELECT v_order_id, v_product.id, p.name, v_product.sell_price, v_cost_price,
                   v_product.vat_rate, v_qty, 0, v_line_vat, v_line_total, v_created_at, v_created_at
            FROM products p WHERE p.id = v_product.id;

            -- Tru kho + ghi InventoryTransaction loai sale
            UPDATE inventory SET stock = stock - v_qty, updated_at = v_created_at
            WHERE product_id = v_product.id AND branch_id = v_branch_id;

            INSERT INTO inventory_transactions (
                product_id, branch_id, type, quantity, unit_cost, reference_type,
                reference_id, created_by, created_at, updated_at
            ) VALUES (
                v_product.id, v_branch_id, 'sale', -v_qty, v_cost_price, 'order',
                v_order_id, v_cashier_id, v_created_at, v_created_at
            );

            v_order_total := v_order_total + v_line_total;
            v_order_vat := v_order_vat + v_line_vat;
        END LOOP;

        UPDATE orders SET
            subtotal_amount = v_order_total,
            discount_amount = 0,
            vat_amount = v_order_vat,
            rounding_adjustment = 0,
            total_amount = v_order_total
        WHERE id = v_order_id;

        INSERT INTO order_payments (order_id, method, amount, created_at, updated_at)
        VALUES (v_order_id, 'cash', v_order_total, v_created_at, v_created_at);

        INSERT INTO invoices (order_id, invoice_number, qr_payload, lookup_code, issued_at, created_at, updated_at)
        VALUES (
            v_order_id, 'INV-' || lpad(i::text, 6, '0'), NULL,
            upper(substr(md5(v_order_id::text), 1, 8)), v_created_at, v_created_at, v_created_at
        );
    END LOOP;
END $$;
