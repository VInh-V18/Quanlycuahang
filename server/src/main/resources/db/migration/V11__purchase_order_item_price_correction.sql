-- Cho sua gia nhap cua 1 dong phieu nhap cu (VD: nhap sai gia luc tao phieu) - gia von se duoc
-- tinh lai bang cach phat lai (replay) toan bo lich su giao dich kho cua san pham do (B4 mo rong).

-- Lien ket 1-1 tu inventory_transactions ve dung dong phieu nhap da sinh ra no, de xac dinh chinh
-- xac giao dich nao can thay gia luc tinh lai (truoc day chi biet reference_id = purchase_order_id,
-- khong phan biet duoc dong nao neu 1 phieu co nhieu san pham).
ALTER TABLE inventory_transactions ADD COLUMN purchase_order_item_id BIGINT REFERENCES purchase_order_items(id);

CREATE INDEX idx_inventory_transactions_purchase_order_item ON inventory_transactions(purchase_order_item_id);

-- Backfill du lieu cu: chi noi khi khop DUY NHAT 1 dong (tranh gan nham neu 1 phieu nhap co 2 dong
-- cung 1 san pham) - truong hop khong khop duy nhat se bi bo qua (con lai NULL), phieu do don gian
-- se chua sua gia duoc cho toi khi co du lieu ro rang hon.
WITH candidate AS (
    SELECT
        t.id AS txn_id,
        poi.id AS item_id,
        COUNT(*) OVER (PARTITION BY t.id) AS match_count
    FROM inventory_transactions t
    JOIN purchase_order_items poi
        ON poi.purchase_order_id = t.reference_id
       AND poi.product_id = t.product_id
    WHERE t.type = 'purchase' AND t.reference_type = 'purchase_order'
)
UPDATE inventory_transactions t
SET purchase_order_item_id = c.item_id
FROM candidate c
WHERE t.id = c.txn_id
  AND c.match_count = 1
  AND t.purchase_order_item_id IS NULL;

INSERT INTO permissions (code, description) VALUES
    ('purchase-order:update', 'Sửa giá nhập phiếu nhập kho cũ');

-- Chi Chu cua hang / Quan ly duoc sua (anh huong cong no NCC da ghi nhan) - thu kho van tao phieu
-- nhu cu nhung khong tu sua duoc phieu da tao.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code IN ('owner', 'manager') AND p.code = 'purchase-order:update';
