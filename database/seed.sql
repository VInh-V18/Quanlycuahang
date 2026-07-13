-- ============================================================================
-- OPTIONAL demo/staging data — KHONG phai seed bat buoc cho production.
--
-- Du lieu BAT BUOC (Super Admin, roles, permissions, role_permissions) da duoc
-- Flyway seed san va tu dong chay 1 lan duy nhat khi server khoi dong lan dau
-- (xem server/src/main/resources/db/migration/V2__seed_data.sql va
-- V15__seed_platform_admin.sql) — KHONG chay lai script nay de tao cac du lieu
-- do, Flyway da dam bao dung nhat quan roi.
--
-- Script nay CHI tao 1 tenant demo ("Cua hang Demo") kem 1 chi nhanh, 1 tai
-- khoan chu cua hang, vai danh muc/san pham mau — de QA/demo/staging co du
-- lieu thao tac ma KHONG dung cham gi den du lieu tenant that dang chay.
--
-- An toan chay lai nhieu lan (idempotent): moi buoc deu kiem tra ton tai
-- truoc khi insert (INSERT ... WHERE NOT EXISTS / ON CONFLICT DO NOTHING).
--
-- Chay: docker compose -f docker/docker-compose.yml --env-file .env exec -T \
--         postgres psql -U "$DB_USERNAME" -d "$DB_NAME" -f /dev/stdin < database/seed.sql
-- ============================================================================

BEGIN;

-- 1) Tenant demo (bo qua neu da ton tai — nhan biet qua ten cua hang)
INSERT INTO tenants (name, is_active)
SELECT 'Cửa hàng Demo', true
WHERE NOT EXISTS (SELECT 1 FROM tenants WHERE name = 'Cửa hàng Demo');

-- 2) Chi nhanh dau tien cua tenant demo
INSERT INTO branches (tenant_id, name, is_active)
SELECT t.id, 'Chi nhánh chính', true
FROM tenants t
WHERE t.name = 'Cửa hàng Demo'
  AND NOT EXISTS (
    SELECT 1 FROM branches b WHERE b.tenant_id = t.id AND b.name = 'Chi nhánh chính'
  );

-- 3) Tai khoan chu cua hang demo (username: demo_owner)
-- Mat khau: Demo@12345 (BCrypt strength 12 — PHAI doi ngay sau lan dang nhap dau
-- tien qua trang Cai dat > Doi mat khau, day la du lieu demo/staging, khong
-- dung cho production that).
INSERT INTO users (tenant_id, username, password_hash, full_name, is_active)
SELECT t.id, 'demo_owner',
       '$2b$12$AFWkC/MrT4fr33Ko68UUW.6SD.muO/PQgWLauehVnu4SoCeDYpEVG',
       'Chủ cửa hàng Demo', true
FROM tenants t
WHERE t.name = 'Cửa hàng Demo'
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.username = 'demo_owner');

-- Gan vai tro "owner" (global role, dung chung moi tenant) cho tai khoan demo
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'demo_owner' AND r.code = 'owner'
ON CONFLICT DO NOTHING;

-- Gan chi nhanh cho tai khoan demo
INSERT INTO user_branches (user_id, branch_id)
SELECT u.id, b.id
FROM users u
JOIN branches b ON b.tenant_id = u.tenant_id AND b.name = 'Chi nhánh chính'
WHERE u.username = 'demo_owner'
ON CONFLICT DO NOTHING;

-- 4) Danh muc mau
INSERT INTO categories (tenant_id, name, display_order)
SELECT t.id, v.name, v.ord
FROM tenants t
CROSS JOIN (VALUES ('Đồ uống', 1), ('Bánh kẹo', 2), ('Rau củ', 3), ('Trái cây', 4)) AS v(name, ord)
WHERE t.name = 'Cửa hàng Demo'
  AND NOT EXISTS (
    SELECT 1 FROM categories c WHERE c.tenant_id = t.id AND c.name = v.name
  );

-- 5) San pham mau (SKU/barcode duy nhat theo tenant — an toan chay lai nho UNIQUE constraint)
INSERT INTO products (tenant_id, category_id, sku, name, unit, sell_price, vat_rate, min_stock)
SELECT t.id, c.id, v.sku, v.name, v.unit, v.price, 0, v.min_stock
FROM tenants t
JOIN categories c ON c.tenant_id = t.id AND c.name = 'Đồ uống'
CROSS JOIN (VALUES
    ('DEMO-SP001', 'Nước suối 500ml', 'Chai', 8000, 20),
    ('DEMO-SP002', 'Trà xanh không độ', 'Chai', 12000, 10)
) AS v(sku, name, unit, price, min_stock)
WHERE t.name = 'Cửa hàng Demo'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.tenant_id = t.id AND p.sku = v.sku
  );

COMMIT;

-- Sau khi chay xong: dang nhap bang demo_owner / Demo@12345 tren tenant "Cửa hàng Demo"
-- (Super Admin co the xem/quan ly tenant nay tai /platform-admin nhu moi tenant khac).
