-- =====================================================================
-- V2__seed_data.sql
-- Dữ liệu seed cho môi trường local/demo (Phase 3.5)
-- Mật khẩu seed cho cả 3 user: "Password@123" (BCrypt strength 12)
-- =====================================================================

SELECT setseed(0.42);

-- =====================================================================
-- 1 chi nhánh
-- =====================================================================
INSERT INTO branches (name, address, phone) VALUES
    ('Chi nhánh Quận 1', '12 Nguyễn Huệ, Quận 1, TP.HCM', '0281234567');

-- Cấu hình mặc định (global, branch_id = NULL)
INSERT INTO settings (branch_id, key, value) VALUES
    (NULL, 'allow_negative_stock', 'false'),
    (NULL, 'price_includes_vat_default', 'true'),
    (NULL, 'rounding_unit', '1000'),
    (NULL, 'debt_limit_default', '5000000'),
    (NULL, 'sku_prefix', 'SP-'),
    (NULL, 'order_number_prefix', 'HD-'),
    (NULL, 'invoice_number_prefix', 'INV-');

-- =====================================================================
-- 6 vai trò (dùng ma trận Phase 1.1)
-- =====================================================================
INSERT INTO roles (code, display_name) VALUES
    ('owner', 'Chủ cửa hàng'),
    ('manager', 'Quản lý'),
    ('cashier', 'Thu ngân'),
    ('sales_staff', 'Nhân viên bán hàng'),
    ('warehouse_staff', 'Nhân viên kho'),
    ('accountant', 'Kế toán');

-- =====================================================================
-- 51 quyền (resource:action) — dùng nguồn docs/phase1/permission-matrix.md
-- =====================================================================
INSERT INTO permissions (code, description) VALUES
    ('employee:view', 'Xem danh sách nhân viên'),
    ('employee:create', 'Tạo nhân viên mới'),
    ('employee:update', 'Sửa thông tin nhân viên'),
    ('employee:delete', 'Xóa (vô hiệu hóa) nhân viên'),
    ('employee:manage-permission', 'Đổi vai trò/quyền của nhân viên'),
    ('branch:view', 'Xem danh sách chi nhánh'),
    ('branch:manage', 'Thêm/sửa chi nhánh'),
    ('settings:view', 'Xem cấu hình hệ thống'),
    ('settings:update', 'Sửa cấu hình'),
    ('audit-log:view', 'Xem nhật ký audit'),
    ('product:view', 'Xem sản phẩm'),
    ('product:create', 'Tạo sản phẩm'),
    ('product:update', 'Sửa sản phẩm'),
    ('product:delete', 'Xóa (soft delete) sản phẩm'),
    ('category:view', 'Xem danh mục'),
    ('category:create', 'Tạo danh mục'),
    ('category:update', 'Sửa danh mục'),
    ('category:delete', 'Xóa danh mục'),
    ('order:view', 'Xem đơn hàng'),
    ('order:create', 'Tạo đơn (bán hàng)'),
    ('order:void', 'Hủy đơn đã hoàn tất'),
    ('order:park', 'Treo/mở lại đơn'),
    ('return:view', 'Xem phiếu trả hàng'),
    ('return:create', 'Tạo phiếu trả hàng'),
    ('promotion:view', 'Xem khuyến mãi/voucher'),
    ('promotion:manage', 'Tạo/sửa khuyến mãi/voucher'),
    ('inventory:view', 'Xem tồn kho, thẻ kho'),
    ('purchase-order:view', 'Xem phiếu nhập'),
    ('purchase-order:create', 'Tạo phiếu nhập kho'),
    ('purchase-order:return', 'Tạo phiếu trả hàng NCC'),
    ('stock-take:view', 'Xem phiếu kiểm kê'),
    ('stock-take:create', 'Tạo phiếu kiểm kê'),
    ('stock-take:approve', 'Duyệt phiếu cân bằng chênh lệch'),
    ('customer:view', 'Xem khách hàng'),
    ('customer:create', 'Tạo khách hàng'),
    ('customer:update', 'Sửa khách hàng'),
    ('supplier:view', 'Xem nhà cung cấp'),
    ('supplier:manage', 'Tạo/sửa nhà cung cấp'),
    ('debt:view', 'Xem công nợ KH/NCC'),
    ('debt:collect-payment', 'Ghi nhận thanh toán công nợ'),
    ('shift:open', 'Mở ca'),
    ('shift:close', 'Đóng ca'),
    ('shift:view', 'Xem lịch sử ca'),
    ('cash-transaction:create', 'Ghi thu/chi tiền mặt ngoài đơn'),
    ('invoice:view', 'Xem/in hóa đơn'),
    ('invoice:send-email', 'Gửi hóa đơn qua email'),
    ('report:revenue', 'Báo cáo doanh thu'),
    ('report:gross-profit', 'Báo cáo lợi nhuận gộp'),
    ('report:inventory-value', 'Báo cáo giá trị tồn kho'),
    ('report:employee-performance', 'Hiệu suất nhân viên'),
    ('report:export', 'Xuất Excel/PDF');

-- owner: toàn bộ 51 quyền
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.code = 'owner';

-- manager: tất cả trừ 4 quyền chỉ owner (employee:delete, employee:manage-permission, branch:manage, settings:update)
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
-- 3 user (mật khẩu: Password@123, BCrypt strength 12)
-- =====================================================================
INSERT INTO users (username, password_hash, full_name, phone) VALUES
    ('owner01', '$2b$12$xt1u.QNjFBv8dO6nyLCSsOS5DToHep7U5ZKoUXl8Cfaf2V6pJaPgG', 'Nguyễn Văn Chủ', '0901000001'),
    ('manager01', '$2b$12$xt1u.QNjFBv8dO6nyLCSsOS5DToHep7U5ZKoUXl8Cfaf2V6pJaPgG', 'Trần Thị Quản Lý', '0901000002'),
    ('cashier01', '$2b$12$xt1u.QNjFBv8dO6nyLCSsOS5DToHep7U5ZKoUXl8Cfaf2V6pJaPgG', 'Lê Văn Thu Ngân', '0901000003');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON
    (u.username = 'owner01' AND r.code = 'owner') OR
    (u.username = 'manager01' AND r.code = 'manager') OR
    (u.username = 'cashier01' AND r.code = 'cashier');

INSERT INTO user_branches (user_id, branch_id)
SELECT u.id, b.id FROM users u CROSS JOIN branches b;

-- =====================================================================
-- 5 danh mục
-- =====================================================================
INSERT INTO categories (name, display_order) VALUES
    ('Đồ uống', 1),
    ('Bánh kẹo', 2),
    ('Gia vị & Thực phẩm khô', 3),
    ('Hóa mỹ phẩm', 4),
    ('Đồ gia dụng', 5);

-- =====================================================================
-- 30 sản phẩm có barcode (6 sản phẩm / danh mục)
-- =====================================================================
INSERT INTO products (category_id, sku, barcode, name, unit, sell_price, price_includes_vat, vat_rate, min_stock) VALUES
    ((SELECT id FROM categories WHERE name = 'Đồ uống'), 'SP-000001', '8934567000011', 'Cà phê hòa tan G7 hộp 20 gói', 'Hộp', 55000, TRUE, 10, 10),
    ((SELECT id FROM categories WHERE name = 'Đồ uống'), 'SP-000002', '8934567000028', 'Trà xanh không độ Lipton chai 450ml', 'Chai', 12000, TRUE, 10, 20),
    ((SELECT id FROM categories WHERE name = 'Đồ uống'), 'SP-000003', '8934567000035', 'Nước suối Lavie chai 500ml', 'Chai', 6000, TRUE, 10, 30),
    ((SELECT id FROM categories WHERE name = 'Đồ uống'), 'SP-000004', '8934567000042', 'Coca-Cola lon 330ml', 'Lon', 10000, TRUE, 10, 30),
    ((SELECT id FROM categories WHERE name = 'Đồ uống'), 'SP-000005', '8934567000059', 'Sữa tươi Vinamilk hộp 1 lít', 'Hộp', 32000, TRUE, 5, 15),
    ((SELECT id FROM categories WHERE name = 'Đồ uống'), 'SP-000006', '8934567000066', 'Nước tăng lực Red Bull lon 250ml', 'Lon', 11000, TRUE, 10, 20),
    ((SELECT id FROM categories WHERE name = 'Bánh kẹo'), 'SP-000007', '8934567000073', 'Bánh Oreo gói 137g', 'Gói', 22000, TRUE, 10, 15),
    ((SELECT id FROM categories WHERE name = 'Bánh kẹo'), 'SP-000008', '8934567000080', 'Kẹo Alpenliebe hộp 100 viên', 'Hộp', 35000, TRUE, 10, 10),
    ((SELECT id FROM categories WHERE name = 'Bánh kẹo'), 'SP-000009', '8934567000097', 'Snack Oishi gói 40g', 'Gói', 8000, TRUE, 10, 25),
    ((SELECT id FROM categories WHERE name = 'Bánh kẹo'), 'SP-000010', '8934567000103', 'Bánh quy Cosy hộp 462g', 'Hộp', 45000, TRUE, 10, 10),
    ((SELECT id FROM categories WHERE name = 'Bánh kẹo'), 'SP-000011', '8934567000110', 'Chocopie Orion hộp 12 cái', 'Hộp', 68000, TRUE, 10, 8),
    ((SELECT id FROM categories WHERE name = 'Bánh kẹo'), 'SP-000012', '8934567000127', 'Kẹo cao su Doublemint vỉ 10 thanh', 'Vỉ', 6000, TRUE, 10, 20),
    ((SELECT id FROM categories WHERE name = 'Gia vị & Thực phẩm khô'), 'SP-000013', '8934567000134', 'Nước mắm Nam Ngư chai 500ml', 'Chai', 35000, TRUE, 5, 12),
    ((SELECT id FROM categories WHERE name = 'Gia vị & Thực phẩm khô'), 'SP-000014', '8934567000141', 'Đường cát trắng Biên Hòa 1kg', 'Gói', 25000, TRUE, 5, 15),
    ((SELECT id FROM categories WHERE name = 'Gia vị & Thực phẩm khô'), 'SP-000015', '8934567000158', 'Dầu ăn Neptune chai 1 lít', 'Chai', 52000, TRUE, 5, 10),
    ((SELECT id FROM categories WHERE name = 'Gia vị & Thực phẩm khô'), 'SP-000016', '8934567000165', 'Gạo ST25 túi 5kg', 'Túi', 145000, TRUE, 0, 8),
    ((SELECT id FROM categories WHERE name = 'Gia vị & Thực phẩm khô'), 'SP-000017', '8934567000172', 'Mì tôm Hảo Hảo thùng 30 gói', 'Thùng', 120000, TRUE, 10, 6),
    ((SELECT id FROM categories WHERE name = 'Gia vị & Thực phẩm khô'), 'SP-000018', '8934567000189', 'Muối i-ốt gói 500g', 'Gói', 8000, TRUE, 0, 20),
    ((SELECT id FROM categories WHERE name = 'Hóa mỹ phẩm'), 'SP-000019', '8934567000196', 'Dầu gội Clear chai 630ml', 'Chai', 98000, TRUE, 10, 10),
    ((SELECT id FROM categories WHERE name = 'Hóa mỹ phẩm'), 'SP-000020', '8934567000202', 'Sữa tắm Dove chai 500ml', 'Chai', 89000, TRUE, 10, 10),
    ((SELECT id FROM categories WHERE name = 'Hóa mỹ phẩm'), 'SP-000021', '8934567000219', 'Kem đánh răng P/S ống 200g', 'Ống', 32000, TRUE, 10, 15),
    ((SELECT id FROM categories WHERE name = 'Hóa mỹ phẩm'), 'SP-000022', '8934567000226', 'Nước rửa chén Sunlight chai 750ml', 'Chai', 28000, TRUE, 10, 12),
    ((SELECT id FROM categories WHERE name = 'Hóa mỹ phẩm'), 'SP-000023', '8934567000233', 'Bột giặt Omo túi 3kg', 'Túi', 135000, TRUE, 10, 8),
    ((SELECT id FROM categories WHERE name = 'Hóa mỹ phẩm'), 'SP-000024', '8934567000240', 'Khăn giấy Pulppy hộp 3 gói', 'Hộp', 42000, TRUE, 10, 10),
    ((SELECT id FROM categories WHERE name = 'Đồ gia dụng'), 'SP-000025', '8934567000257', 'Pin Con Thỏ AA vỉ 4 viên', 'Vỉ', 25000, TRUE, 10, 10),
    ((SELECT id FROM categories WHERE name = 'Đồ gia dụng'), 'SP-000026', '8934567000264', 'Bóng đèn LED 9W', 'Cái', 35000, TRUE, 10, 15),
    ((SELECT id FROM categories WHERE name = 'Đồ gia dụng'), 'SP-000027', '8934567000271', 'Túi rác tự hủy 60x70cm cuộn 20 cái', 'Cuộn', 18000, TRUE, 10, 15),
    ((SELECT id FROM categories WHERE name = 'Đồ gia dụng'), 'SP-000028', '8934567000288', 'Bát nhựa Duy Tân bộ 6 cái', 'Bộ', 55000, TRUE, 10, 6),
    ((SELECT id FROM categories WHERE name = 'Đồ gia dụng'), 'SP-000029', '8934567000295', 'Bàn chải vệ sinh đa năng', 'Cái', 15000, TRUE, 10, 10),
    ((SELECT id FROM categories WHERE name = 'Đồ gia dụng'), 'SP-000030', '8934567000301', 'Băng keo trong 5cm cuộn', 'Cuộn', 9000, TRUE, 10, 20);

-- =====================================================================
-- Tồn kho đầu kỳ: 100 đơn vị mỗi sản phẩm tại chi nhánh, giá vốn = 70% giá bán
-- (ghi nhận qua inventory_transactions loại purchase, reference_type = opening_balance)
-- =====================================================================
INSERT INTO inventory (product_id, branch_id, stock, cost_price)
SELECT p.id, b.id, 100, ROUND(p.sell_price * 0.7)
FROM products p CROSS JOIN branches b;

INSERT INTO inventory_transactions (product_id, branch_id, type, quantity, unit_cost, reference_type, created_by)
SELECT p.id, b.id, 'purchase', 100, ROUND(p.sell_price * 0.7), 'opening_balance',
       (SELECT id FROM users WHERE username = 'owner01')
FROM products p CROSS JOIN branches b;

-- =====================================================================
-- 5 khách hàng, 3 nhà cung cấp
-- =====================================================================
INSERT INTO customer_groups (name) VALUES ('Khách lẻ'), ('Khách thân thiết');

INSERT INTO customers (customer_group_id, name, phone, address, debt_limit) VALUES
    ((SELECT id FROM customer_groups WHERE name = 'Khách thân thiết'), 'Nguyễn Thị Anh', '0912000001', '10 Lê Lợi, Q1', 3000000),
    ((SELECT id FROM customer_groups WHERE name = 'Khách lẻ'), 'Trần Văn Bình', '0912000002', '22 Hai Bà Trưng, Q1', 2000000),
    ((SELECT id FROM customer_groups WHERE name = 'Khách thân thiết'), 'Lê Thị Cúc', '0912000003', '5 Pasteur, Q3', 5000000),
    ((SELECT id FROM customer_groups WHERE name = 'Khách lẻ'), 'Phạm Văn Đức', '0912000004', '18 Võ Văn Tần, Q3', 2000000),
    ((SELECT id FROM customer_groups WHERE name = 'Khách lẻ'), 'Hoàng Thị Em', '0912000005', '30 Điện Biên Phủ, Bình Thạnh', 2000000);

INSERT INTO suppliers (name, phone, address) VALUES
    ('Công ty TNHH Phân phối Thực phẩm An Gia', '0281112222', '100 Trường Chinh, Tân Bình'),
    ('Công ty CP Hàng tiêu dùng Minh Phát', '0281113333', '55 Cộng Hòa, Tân Bình'),
    ('Công ty TNHH Thương mại Việt Hưng', '0281114444', '8 Lê Trọng Tấn, Tân Phú');

-- =====================================================================
-- 20 đơn mẫu rải 30 ngày (sinh tự động bằng PL/pgSQL để đảm bảo tính tiền nhất quán)
-- Kịch bản đơn giản: không CK/voucher, giá đã gồm VAT, thanh toán tiền mặt đủ.
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
        v_num_items := 1 + floor(random() * 2)::int; -- 1 hoặc 2 dòng

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

            -- Trừ kho + ghi InventoryTransaction loại sale
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
