-- Xoa du lieu DEMO ma V2__seed_data.sql (va placeholder store_name cua V4__invoice_fields.sql) da
-- tao san: 20 don hang mau, 30 san pham/5 khach hang/3 NCC/5 danh muc/2 nhom khach mau. KHONG dung
-- den permissions/roles/role_permissions/users/branches/settings khac — nhung thu do la cau hinh
-- van hanh, khong phai du lieu demo.
--
-- Nguyen tac an toan: CHI xoa dong nao vua (a) khop CHINH XAC gia tri seed ban dau, VUA (b) chua tung
-- duoc dung qua nghiep vu that (khong co don hang/phieu nhap/kiem ke/cong no nao tham chieu toi) —
-- dam bao KHONG dung vao du lieu that da phat sinh tu luc dua he thong vao su dung. Vi migration nay
-- chay tren MOI lan cai dat (ke ca database hoan toan moi, ngay sau V2), no cung la co che dam bao
-- moi lan cai dat moi se KHONG con du lieu mau ngay tu dau.

-- === 1. 20 don hang mau (HD-000001..HD-000020) do V2 tu sinh bang PL/pgSQL — luon an toan xoa vi
-- la du lieu gia tao ngau nhien tu dau, thanh toan du 100% (khong co cong no lien quan).
CREATE TEMP TABLE _seed_order_ids AS
SELECT id FROM orders WHERE order_number IN (
    'HD-000001','HD-000002','HD-000003','HD-000004','HD-000005','HD-000006','HD-000007','HD-000008',
    'HD-000009','HD-000010','HD-000011','HD-000012','HD-000013','HD-000014','HD-000015','HD-000016',
    'HD-000017','HD-000018','HD-000019','HD-000020');

DELETE FROM inventory_transactions WHERE reference_type = 'order' AND reference_id IN (SELECT id FROM _seed_order_ids);
DELETE FROM debts WHERE reference_type = 'order' AND reference_id IN (SELECT id FROM _seed_order_ids);
DELETE FROM invoices WHERE order_id IN (SELECT id FROM _seed_order_ids);
DELETE FROM order_payments WHERE order_id IN (SELECT id FROM _seed_order_ids);
DELETE FROM return_items WHERE order_item_id IN (SELECT id FROM order_items WHERE order_id IN (SELECT id FROM _seed_order_ids));
DELETE FROM returns WHERE order_id IN (SELECT id FROM _seed_order_ids);
DELETE FROM voucher_usages WHERE order_id IN (SELECT id FROM _seed_order_ids);
DELETE FROM order_items WHERE order_id IN (SELECT id FROM _seed_order_ids);
DELETE FROM orders WHERE id IN (SELECT id FROM _seed_order_ids);

-- === 2. San pham seed (30 SKU dau tien) CHUA TUNG duoc dung qua nghiep vu that (chi con dung 1 giao
-- dich opening_balance luc seed, chua tung nhap/ban/kiem ke that lan nao) — xoa ca ton kho di kem.
CREATE TEMP TABLE _seed_product_skus AS
SELECT unnest(ARRAY[
    'SP-000001','SP-000002','SP-000003','SP-000004','SP-000005','SP-000006','SP-000007','SP-000008',
    'SP-000009','SP-000010','SP-000011','SP-000012','SP-000013','SP-000014','SP-000015','SP-000016',
    'SP-000017','SP-000018','SP-000019','SP-000020','SP-000021','SP-000022','SP-000023','SP-000024',
    'SP-000025','SP-000026','SP-000027','SP-000028','SP-000029','SP-000030']) AS sku;

-- Luu y: stock_take_items thuong tao dong cho CA catalog ngay luc tao phieu kiem ke (du chua cham
-- toi san pham do), nen CHI coi la "da dung that" khi co actual_qty (da thuc su nhap so dem) — dong
-- chua dem (actual_qty NULL) se duoc xoa cung san pham ben duoi, khong tinh la dau vet su dung that.
CREATE TEMP TABLE _removable_product_ids AS
SELECT p.id FROM products p
JOIN _seed_product_skus s ON s.sku = p.sku
WHERE NOT EXISTS (
        SELECT 1 FROM inventory_transactions it
        WHERE it.product_id = p.id AND it.reference_type <> 'opening_balance')
  AND NOT EXISTS (SELECT 1 FROM order_items oi WHERE oi.product_id = p.id)
  AND NOT EXISTS (SELECT 1 FROM purchase_order_items poi WHERE poi.product_id = p.id)
  AND NOT EXISTS (
        SELECT 1 FROM stock_take_items sti
        WHERE sti.product_id = p.id AND sti.actual_qty IS NOT NULL);

DELETE FROM stock_take_items WHERE product_id IN (SELECT id FROM _removable_product_ids);
DELETE FROM inventory_transactions WHERE product_id IN (SELECT id FROM _removable_product_ids);
DELETE FROM inventory_batches WHERE product_id IN (SELECT id FROM _removable_product_ids);
DELETE FROM inventory WHERE product_id IN (SELECT id FROM _removable_product_ids);
DELETE FROM product_units WHERE product_id IN (SELECT id FROM _removable_product_ids);
DELETE FROM price_history WHERE product_id IN (SELECT id FROM _removable_product_ids);
DELETE FROM products WHERE id IN (SELECT id FROM _removable_product_ids);

-- === 3. Danh muc seed khong con san pham nao (ke ca san pham that) tham chieu toi.
DELETE FROM categories
WHERE name IN ('Đồ uống', 'Bánh kẹo', 'Gia vị & Thực phẩm khô', 'Hóa mỹ phẩm', 'Đồ gia dụng')
  AND NOT EXISTS (SELECT 1 FROM products p WHERE p.category_id = categories.id);

-- === 4. Khach hang seed (dung ten+SDT goc de nhan dien) chua tung phat sinh don hang/cong no that.
DELETE FROM customers
WHERE (name, phone) IN (
    ('Nguyễn Thị Anh', '0912000001'), ('Trần Văn Bình', '0912000002'), ('Lê Thị Cúc', '0912000003'),
    ('Phạm Văn Đức', '0912000004'), ('Hoàng Thị Em', '0912000005'))
  AND NOT EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = customers.id)
  AND NOT EXISTS (SELECT 1 FROM debts d WHERE d.customer_id = customers.id);

-- === 5. Nhom khach hang seed khong con khach hang nao (ke ca khach that) tham chieu toi.
DELETE FROM customer_groups
WHERE name IN ('Khách lẻ', 'Khách thân thiết')
  AND NOT EXISTS (SELECT 1 FROM customers c WHERE c.customer_group_id = customer_groups.id);

-- === 6. Nha cung cap seed chua tung phat sinh phieu nhap/cong no that.
DELETE FROM suppliers
WHERE (name, phone) IN (
    ('Công ty TNHH Phân phối Thực phẩm An Gia', '0281112222'),
    ('Công ty CP Hàng tiêu dùng Minh Phát', '0281113333'),
    ('Công ty TNHH Thương mại Việt Hưng', '0281114444'))
  AND NOT EXISTS (SELECT 1 FROM purchase_orders po WHERE po.supplier_id = suppliers.id)
  AND NOT EXISTS (SELECT 1 FROM debts d WHERE d.supplier_id = suppliers.id);

-- === 7. Ten cua hang placeholder cua V4 — CHI xoa neu con y nguyen gia tri seed (nghia la chua ai
-- doi qua Cai dat), de khong ghi de ten that nguoi dung da tu cau hinh.
DELETE FROM settings
WHERE branch_id IS NULL AND key = 'store_name' AND value = 'Cua hang Quan Ly';

-- === 8. Chi giu 1 tai khoan quan tri (owner01, du 51 quyen) de giao cho chu cua hang tu dang nhap
-- va tu cau hinh moi thu — VO HIEU HOA (khong xoa cung) 2 tai khoan demo con lai (manager01,
-- cashier01): dung dung co che deactivate co san cua EmployeeService.delete() (setActive(false)),
-- an toan tuyet doi vi khong dung toi khoa ngoai (moi du lieu cu do 2 tai khoan nay tao/thao tac
-- truoc day - vd lam cashier cho 20 don mau da xoa o buoc 1 - van giu nguyen, chi mat kha nang dang
-- nhap). Chu cua hang tu tao lai nhan vien that qua man hinh Nhan vien sau khi dang nhap.
UPDATE users SET is_active = false WHERE username IN ('manager01', 'cashier01');

DROP TABLE _seed_order_ids;
DROP TABLE _seed_product_skus;
DROP TABLE _removable_product_ids;
